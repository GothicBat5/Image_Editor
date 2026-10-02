import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Screen extends JFrame
{
    private final FileManager fm = new FileManager();
    private final Navigation nav;
    // toolbar
    private final JButton backBtn = new JButton("Back");
    private final JButton forwardBtn = new JButton("Forward");
    private final JButton upBtn = new JButton("Up");
    private final JButton refreshBtn = new JButton("Refresh");
    private final JTextField pathField = new JTextField();
    private final JTextField filterField = new JTextField(12);
    private final JCheckBox hiddenBox = new JCheckBox("Hidden");
    // file list
    private final EntryTableModel tableModel = new EntryTableModel();
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<EntryTableModel> sorter = new TableRowSorter<>(tableModel);
    private final JLabel statusLabel = new JLabel(" ");
    // preview
    private final CardLayout cards = new CardLayout();
    private final JPanel previewCards = new JPanel(cards);
    private final JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
    private final JTextArea textArea = new JTextArea();
    private final JLabel noteLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel infoPanel = new JPanel(new GridBagLayout());
    private SwingWorker<Preview, Void> previewWorker;

    private final Map<String, Icon> iconCache = new java.util.HashMap<>();
    private record Preview(Map<String, String> info, BufferedImage image, String text, String note) {}

    public Screen(Path start)
    {
        super("My File Manager");
        nav = new Navigation(start);

        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        buildToolbar();
        buildCenter();
        wireActions();

        load(start);
        updateButtons();
    }

    //layout

    private void buildToolbar()
    {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        left.add(backBtn);
        left.add(forwardBtn);
        left.add(upBtn);
        left.add(refreshBtn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        right.add(new JLabel("Filter:"));
        right.add(filterField);
        right.add(hiddenBox);

        JPanel bar = new JPanel(new BorderLayout(4, 0));
        bar.add(left, BorderLayout.WEST);
        bar.add(pathField, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        bar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        add(bar, BorderLayout.NORTH);
    }

    private void buildCenter()
    {
        // file table
        table.setRowSorter(sorter);
        table.setRowHeight(24);
        table.setShowGrid(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        sorter.setComparator(0, Comparator.comparing((FileManager.Entry e) -> !e.directory())
                .thenComparing(e -> e.name().toLowerCase()));

        table.getColumnModel().getColumn(0).setCellRenderer(new NameRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(new TextRenderer(false, true));
        table.getColumnModel().getColumn(3).setCellRenderer(new TextRenderer(true, false));
        table.getColumnModel().getColumn(0).setPreferredWidth(280);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);

        // preview cards
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setMargin(new Insets(6, 6, 6, 6));
        noteLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        previewCards.add(noteLabel, "note");
        previewCards.add(imageLabel, "image");
        previewCards.add(new JScrollPane(textArea), "text");
        showNote("Select a file to preview it.");

        infoPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane infoScroll = new JScrollPane(infoPanel);
        infoScroll.setBorder(BorderFactory.createTitledBorder("Details"));

        JSplitPane rightSide = new JSplitPane(JSplitPane.VERTICAL_SPLIT, previewCards, infoScroll);
        rightSide.setResizeWeight(0.65);
        rightSide.setBorder(null);

        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(table), rightSide);
        main.setResizeWeight(0.62);
        main.setBorder(null);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        add(main, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void wireActions() // actions stuff 
    {
        backBtn.addActionListener(e -> {
            Path target = nav.peekBack();

            if (target != null && load(target)) 
            { 
                nav.goBack(); updateButtons(); 
            }
        });

        forwardBtn.addActionListener(e -> {
            Path target = nav.peekForward();

            if (target != null && load(target))
            { 
                nav.goForward(); 
                updateButtons(); 
            }
        });

        upBtn.addActionListener(e -> goUp());
        refreshBtn.addActionListener(e -> load(nav.current()));
        hiddenBox.addActionListener(e -> load(nav.current()));

        pathField.addActionListener(e -> {

            try { 

                navigateTo(Path.of(pathField.getText().trim())); 
            }
            catch (java.nio.file.InvalidPathException ex)
            {
                error("Invalid path", ex.getMessage());
                pathField.setText(nav.current().toString());
            }
        });

        filterField.getDocument().addDocumentListener(new DocumentListener()
        {
            public void insertUpdate(DocumentEvent e)  
            { 
                applyFilter(); 
            }

            public void removeUpdate(DocumentEvent e)  
            { 
                applyFilter(); 
            }

            public void changedUpdate(DocumentEvent e) 
            { 
                applyFilter(); 
            }
        });

        table.addMouseListener(new MouseAdapter()
        {
            @Override 
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2) openSelected();
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onSelectionChanged();
        });

        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open");
        table.getActionMap().put("open", new AbstractAction()
        {
            public void actionPerformed(java.awt.event.ActionEvent e) 
            { 
                openSelected(); 
            }
        });

        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "up");
        table.getActionMap().put("up", new AbstractAction()
        {
            public void actionPerformed(java.awt.event.ActionEvent e) 
            { 
                goUp(); 
            }
        });
    }

    private void goUp()
    {
        Path parent = nav.peekUp();
        if (parent != null) navigateTo(parent);
    }

    // Loads the folder first; only records history if it succeeded.
    private void navigateTo(Path target)
    {
        if (load(target))
        {
            nav.go(target);
            updateButtons();
        }
    }

    private boolean load(Path dir)
    {
        List<FileManager.Entry> entries;

        try
        {
            entries = fm.list(dir, hiddenBox.isSelected());
        }
        catch (IOException | SecurityException ex)
        {
            error("Cannot open folder", ex.toString());
            return false;
        }

        tableModel.setEntries(entries);
        pathField.setText(dir.toString());
        applyFilter();
        clearPreview();
        return true;
    }

    private void openSelected()
    {
        FileManager.Entry e = selectedEntry();
        if (e == null) return;

        if (e.directory())
        {
            navigateTo(e.path());
        }
        else
        {
            try 
            {
                fm.open(e.path()); 
            }
            catch (IOException ex) 
            { 
                error("Cannot open file", ex.getMessage()); 
            }
        }
    }

    private void applyFilter()
    {
        final String text = filterField.getText().trim().toLowerCase();

        if (text.isEmpty())
        {
            sorter.setRowFilter(null);
        }
        else
        {
            sorter.setRowFilter(new RowFilter<>()
            {
                @Override 
                public boolean include(Entry<? extends EntryTableModel, ? extends Integer> en)
                {
                    FileManager.Entry fe = (FileManager.Entry) en.getValue(0);
                    return fe.name().toLowerCase().contains(text);
                }
            });
        }

        updateStatus();
    }

    private void updateButtons()
    {
        backBtn.setEnabled(nav.canGoBack());
        forwardBtn.setEnabled(nav.canGoForward());
        upBtn.setEnabled(nav.canGoUp());
    }

    private void updateStatus()
    {
        int shown = table.getRowCount();
        int total = tableModel.getRowCount();
        statusLabel.setText(shown == total ? total + " items" : shown + " of " + total + " items");
    }

    private FileManager.Entry selectedEntry()
    {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return tableModel.getEntry(table.convertRowIndexToModel(row));
    }

    // preview

    private void onSelectionChanged()
    {
        FileManager.Entry e = selectedEntry();

        if (e == null) 
        { 
            clearPreview(); 
            return; 
        }
        showPreview(e);
    }

    private void clearPreview()
    {
        if (previewWorker != null) previewWorker.cancel(true);
        showNote("Select a file to preview it.");
        showInfo(Map.of());
    }

    private void showPreview(FileManager.Entry entry)
    {
        if (previewWorker != null) previewWorker.cancel(true);

        final Path path = entry.path();
        final int w = Math.max(200, previewCards.getWidth() - 24);
        final int h = Math.max(200, previewCards.getHeight() - 24);

        showNote("Loading...");

        previewWorker = new SwingWorker<>()
        {
            @Override 
            protected Preview doInBackground()
            {
                Map<String, String> info = fm.details(path);
                BufferedImage img = null;
                String text = null;
                String note = null;

                if (entry.directory())
                {
                    note = "Folder. Double-click to open.";
                }
                else if (fm.isImage(path))
                {
                    img = fm.loadScaledImage(path, w, h);
                    if (img == null) note = "This image format can't be previewed.";
                }
                else if (fm.isVideo(path))
                {
                    img = fm.videoThumbnail(path, w, h);
                    if (img == null)
                        note = "<html><center>No video thumbnail.<br>Install ffmpeg (on PATH) to see one.<br>"
                                + "Double-click to play in your default player.</center></html>";
                }
                else if (fm.isText(path))
                {
                    text = fm.readTextPreview(path, 6000);
                }
                else
                {
                    note = "No preview available for this file type.";
                }

                return new Preview(info, img, text, note);
            }

            @Override protected void done()
            {
                if (isCancelled()) return;

                try
                {
                    Preview p = get();
                    showInfo(p.info());

                    if (p.image() != null)
                    {
                        imageLabel.setIcon(new ImageIcon(p.image()));
                        cards.show(previewCards, "image");
                    }
                    else if (p.text() != null)
                    {
                        textArea.setText(p.text());
                        textArea.setCaretPosition(0);
                        cards.show(previewCards, "text");
                    }
                    else
                    {
                        showNote(p.note());
                    }
                }
                catch (Exception ignored) 
                { 
                    //cancelled or failed
                }
            }
        };

        previewWorker.execute();
    }

    private void showNote(String text)
    {
        noteLabel.setText(text);
        cards.show(previewCards, "note");
    }

    private void showInfo(Map<String, String> info)
    {
        infoPanel.removeAll();

        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(2, 0, 2, 12);
        gc.gridy = 0;

        for (Map.Entry<String, String> e : info.entrySet())
        {
            JLabel key = new JLabel(e.getKey() + ":");
            key.setFont(key.getFont().deriveFont(Font.BOLD));

            JLabel value = new JLabel(e.getValue());

            gc.gridx = 0; gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
            infoPanel.add(key, gc);

            gc.gridx = 1; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
            infoPanel.add(value, gc);

            gc.gridy++;
        }

        // filler pushes rows to the top
        gc.gridx = 0; gc.weighty = 1; gc.gridwidth = 2;
        infoPanel.add(Box.createVerticalGlue(), gc);

        infoPanel.revalidate();
        infoPanel.repaint();
    }

    private void error(String title, String message)
    {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }


    private static class EntryTableModel extends AbstractTableModel
    {
        private static final String[] COLS = {"Name", "Modified", "Type", "Size"};
        private List<FileManager.Entry> entries = new ArrayList<>();

        void setEntries(List<FileManager.Entry> list) 
        { 
            entries = list; fireTableDataChanged(); 
        }

        FileManager.Entry getEntry(int row) 
        { 
            return entries.get(row); 
        }

        @Override 
        public int getRowCount() 
        { 
            return entries.size(); 
        }
        
        @Override 
        public int getColumnCount() 
        { 
            return COLS.length; 
        }

        @Override 
        public String getColumnName(int c) 
        { 
            return COLS[c]; 
        }

        @Override public Class<?> getColumnClass(int c)
        {
            return switch (c)
            {
                case 0 -> FileManager.Entry.class;
                case 1, 3 -> Long.class;
                default -> String.class;
            };
        }

        @Override public Object getValueAt(int row, int col)
        {
            FileManager.Entry e = entries.get(row);
            return switch (col)
            {
                case 0 -> e;
                case 1 -> e.modified();
                case 2 -> e.type();
                default -> e.size();
            };
        }
    }

    // Name column: system icon + file name.
    private class NameRenderer extends DefaultTableCellRenderer
    {
        @Override 
        public java.awt.Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col)
        {
            FileManager.Entry e = (FileManager.Entry) value;
            super.getTableCellRendererComponent(t, e.name(), sel, focus, row, col);
            setIcon(iconFor(e));
            return this;
        }
    }

    //Size (right-aligned) and date columns.
    private static class TextRenderer extends DefaultTableCellRenderer
    {
        private final boolean size;
        private final boolean date;

        TextRenderer(boolean size, boolean date) 
        { 
            this.size = size; 
            this.date = date; 
        }

        @Override 
        protected void setValue(Object value)
        {
            long v = (Long) value;

            if (size) 
            { 
                setText(FileManager.formatSize(v)); 
                setHorizontalAlignment(RIGHT); 
            }
            
            else if (date) setText(FileManager.formatDate(v));
        }
    }

    private Icon iconFor(FileManager.Entry e)
    {
        // Cache by folder / extension so we don't ask the OS for every repaint.
        String key = e.directory() ? "<dir>" : fm.extensionOf(e.path());
        return iconCache.computeIfAbsent(key,
                k -> FileSystemView.getFileSystemView().getSystemIcon(e.path().toFile()));
    }
}
