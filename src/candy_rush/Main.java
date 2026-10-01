package candy_rush;

import opening.OpeningScreen;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main
{
    public static void main(String[] args)
    {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception ignored)
        {
            // File manager:: Native OS opne ??
        }
        System.out.println("Program Started");
        SwingUtilities.invokeLater(OpeningScreen::new);
    }
}
