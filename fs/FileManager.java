import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

//core file logic: 
public class FileManager
{
    //One row in the file list
    public record Entry(Path path, String name, boolean directory, long size, long modified, String type) {}

    private static final Set<String> IMAGE = Set.of("png", "jpg", "jpeg", "gif", "bmp", "wbmp");
    private static final Set<String> VIDEO = Set.of("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg");
    private static final Set<String> AUDIO = Set.of("mp3", "wav", "flac", "ogg", "aac", "m4a");
    private static final Set<String> TEXT = Set.of("txt", "md", "java", "py", "js", "ts", "html", "css", "xml",
            "json", "csv", "log", "ini", "yml", "yaml", "c", "cpp", "h", "cs", "sh", "bat", "sql", "properties", "gradle");

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public List<Entry> list(Path dir, boolean showHidden) throws IOException
    {
        List<Entry> result = new ArrayList<>();

        try (Stream<Path> stream = Files.list(dir))
        {
            for (Path p : (Iterable<Path>) stream::iterator)
            {
                try
                {
                    if (!showHidden && Files.isHidden(p)) continue;
                    result.add(toEntry(p));
                }
                catch (IOException | SecurityException ignored) 
                { 
                    /* skip unreadable items */ 
                }
            }
        }

        result.sort(Comparator.comparing((Entry e) -> !e.directory()).thenComparing(e -> e.name().toLowerCase()));return result;
    }

    private Entry toEntry(Path p) throws IOException
    {
        BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class);
        boolean dir = a.isDirectory();
        return new Entry(p, nameOf(p), dir, dir ? -1 : a.size(), a.lastModifiedTime().toMillis(), typeOf(p, dir));
    }


    public Map<String, String> details(Path p)
    {
        Map<String, String> m = new LinkedHashMap<>();

        try
        {
            BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class);

            m.put("Name", nameOf(p));
            m.put("Type", typeOf(p, a.isDirectory()));

            if (a.isDirectory())
            {
                try (Stream<Path> s = Files.list(p)) 
                { 
                    m.put("Items", String.valueOf(s.count())); 
                }
                catch (IOException e) 
                { 
                    m.put("Items", "unreadable"); 
                }
            }
            else
            {
                m.put("Size", formatSize(a.size()) + String.format(" (%,d bytes)", a.size()));
            }

            if (isImage(p))
            {
                Dimension d = imageSize(p);
                if (d != null) m.put("Dimensions", d.width + " x " + d.height + " px");
            }

            Path parent = p.getParent();
            m.put("Location", parent == null ? "" : parent.toString());
            m.put("Created", formatDate(a.creationTime()));
            m.put("Modified", formatDate(a.lastModifiedTime()));
            m.put("Accessed", formatDate(a.lastAccessTime()));
            m.put("Permissions", (Files.isReadable(p) ? "Read " : "") + (Files.isWritable(p) ? "Write " : "")
                    + (Files.isExecutable(p) ? "Execute" : ""));
        }
        catch (IOException e)
        {
            m.put("Error", String.valueOf(e.getMessage()));
        }

        return m;
    }

    //Previews

    //Loads an image scaled down to fit maxW x maxH Returns null if unsupported.
    public BufferedImage loadScaledImage(Path p, int maxW, int maxH)
    {
        try
        {
            BufferedImage img = ImageIO.read(p.toFile());
            return img == null ? null : scaleToFit(img, maxW, maxH);
        }
        catch (IOException | RuntimeException e)
        {
            return null;
        }
    }

    public BufferedImage videoThumbnail(Path video, int maxW, int maxH)
    {
        Path tmp = null;

        try
        {
            tmp = Files.createTempFile("fm_thumb_", ".png");

            Process process = new ProcessBuilder("ffmpeg", "-y", "-loglevel", "error",
                    "-ss", "1", "-i", video.toString(), "-frames:v", "1", tmp.toString())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();

            if (!process.waitFor(15, TimeUnit.SECONDS))
            {
                process.destroyForcibly();
                return null;
            }

            BufferedImage img = ImageIO.read(tmp.toFile());
            return img == null ? null : scaleToFit(img, maxW, maxH);
        }
        catch (IOException e)
        {
            return null; // ffmpeg not installed or file unreadable
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return null;
        }
        finally
        {
            if (tmp != null) 
                try { 
            
                    Files.deleteIfExists(tmp); 
                } 
        catch (IOException ignored) 
        {

        }
        }
    }

    public String readTextPreview(Path p, int maxChars)
    {
        try
        {
            byte[] bytes = Files.readAllBytes(p);
            int len = Math.min(bytes.length, maxChars);
            String s = new String(bytes, 0, len, StandardCharsets.UTF_8);
            return bytes.length > maxChars ? s + "\n\n... (truncated)" : s;
        }
        catch (IOException | OutOfMemoryError e)
        {
            return "Cannot read file: " + e.getMessage();
        }
    }

    public void open(Path p) throws IOException
    {
        if (!Desktop.isDesktopSupported()) throw new IOException("Opening files is not supported on this system.");
        Desktop.getDesktop().open(p.toFile());
    }


    private BufferedImage scaleToFit(BufferedImage img, int maxW, int maxH)
    {
        double scale = Math.min(1.0, Math.min((double) maxW / img.getWidth(), (double) maxH / img.getHeight()));
        if (scale >= 1.0) return img;

        int w = Math.max(1, (int) (img.getWidth() * scale));
        int h = Math.max(1, (int) (img.getHeight() * scale));

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /** Reads image dimensions without decoding the whole image. */
    private Dimension imageSize(Path p)
    {
        try (ImageInputStream in = ImageIO.createImageInputStream(p.toFile()))
        {
            if (in == null) return null;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) return null;

            ImageReader reader = readers.next();
            try
            {
                reader.setInput(in);
                return new Dimension(reader.getWidth(0), reader.getHeight(0));
            }
            finally { reader.dispose(); }
        }
        catch (IOException | RuntimeException e)
        {
            return null;
        }
    }

    public String typeOf(Path p, boolean directory)
    {
        if (directory) return "Folder";

        String ext = extensionOf(p);
        if (ext.isEmpty()) return "File";

        String label = ext.toUpperCase();
        if (IMAGE.contains(ext)) return label + " image";
        if (VIDEO.contains(ext)) return label + " video";
        if (AUDIO.contains(ext)) return label + " audio";
        if (TEXT.contains(ext)) return label + " text file";
        return label + " file";
    }

    public boolean isImage(Path p) 
    { 
        return IMAGE.contains(extensionOf(p)); 
    }

    public boolean isVideo(Path p) 
    { 
        return VIDEO.contains(extensionOf(p)); 
    }

    public boolean isText(Path p) 
    { 
        return TEXT.contains(extensionOf(p)); 
    }

    public String extensionOf(Path p)
    {
        String n = nameOf(p);
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(dot + 1).toLowerCase() : "";
    }

    public String nameOf(Path p)
    {
        Path n = p.getFileName();
        return n == null ? p.toString() : n.toString();
    }

    public static String formatSize(long bytes)
    {
        if (bytes < 0) return "";
        if (bytes < 1024) return bytes + " B";

        String[] units = {"KB", "MB", "GB", "TB"};
        double v = bytes;
        int i = -1;

        do { 
            v /= 1024; i++; 
        } 
        while (v >= 1024 && i < units.length - 1);
        
        return String.format("%.1f %s", v, units[i]);
    }

    public static String formatDate(long millis)
    {
        return DATE_FMT.format(Instant.ofEpochMilli(millis));
    }

    public static String formatDate(FileTime t)
    {
        return DATE_FMT.format(t.toInstant());
    }
}
