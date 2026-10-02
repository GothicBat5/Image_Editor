import javax.swing.*;
import java.nio.file.Path;

public class Main
{
    public static void main(String[] args)
    {
        // Use the OS look and feel: it resembles a native file explorer.
        try { 

            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); 
        }
        catch (Exception ignored) 
        { 
            //defualt 
        }

        Path start = Path.of(System.getProperty("user.home"));

        SwingUtilities.invokeLater(() -> new Screen(start).setVisible(true));
    }
}
