import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.sql.Timestamp;

/**
 * Shared look and feel for SUDWAR: colours, fonts, small helpers and the
 * global "hover feedback" for every button in the application.
 */
public final class Theme {

    // SUDWAR dark palette (from the design reference). The older constant
    // names are kept so every class picks the new colours automatically.
    public static final Color BG     = new Color(8, 12, 27);       // window background
    public static final Color PANEL  = new Color(14, 21, 44);      // cards and panels
    public static final Color PANEL2 = new Color(21, 31, 62);      // inputs, raised areas
    public static final Color EDGE   = new Color(34, 47, 86);      // borders
    public static final Color TXT    = new Color(236, 241, 255);   // main text
    public static final Color DIM    = new Color(139, 152, 186);   // secondary text
    public static final Color RED    = new Color(222, 38, 52);     // primary accent
    public static final Color BLUE   = new Color(47, 128, 255);

    public static final Color PLUM   = TXT;
    public static final Color PURPLE = BLUE;
    public static final Color LILAC  = new Color(30, 46, 92);      // selection tint
    public static final Color PAPER  = BG;
    public static final Color SIDE   = new Color(10, 15, 32);
    public static final Color LIME   = new Color(182, 242, 60);
    public static final Color MUTED  = DIM;
    public static final Color LINE   = EDGE;

    // Navy palette (Logistics Intelligence Centre)
    public static final Color NAVY   = new Color(9, 13, 28);
    public static final Color NAVY2  = new Color(14, 21, 44);
    public static final Color NAVY3  = new Color(26, 37, 72);
    public static final Color TEXT_LIGHT = new Color(236, 241, 255);
    public static final Color TEXT_DIM   = new Color(139, 152, 186);

    private static boolean installed = false;

    private Theme() { }

    private static Font regular, bold;
    private static boolean fontsTried = false;

    /**
     * Uses Poppins when its files are present in the project at
     * resources/fonts/Poppins-Regular.ttf and Poppins-Bold.ttf,
     * otherwise Segoe UI.
     */
    public static synchronized Font font(int style, int size) {

        if (!fontsTried) {
            fontsTried = true;
            regular = load("/resources/fonts/Poppins-Regular.ttf");
            bold = load("/resources/fonts/Poppins-Bold.ttf");
        }

        Font base = ((style & Font.BOLD) != 0 && bold != null) ? bold : regular;

        if (base != null) {
            return base.deriveFont(style & ~Font.BOLD | (bold != null ? 0 : style & Font.BOLD), (float) size);
        }

        return new Font("Segoe UI", style, size);
    }

    public static String family() {
        return font(Font.PLAIN, 12).getFamily();
    }

    private static Font load(String path) {

        try (java.io.InputStream in = Theme.class.getResourceAsStream(path)) {
            if (in == null) return null;
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (Exception ex) {
            return null;
        }
    }

    public static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    /** "5 min ago", "2 h ago", "3 d ago". */
    public static String ago(Timestamp t) {
        if (t == null) return "";
        long s = (System.currentTimeMillis() - t.getTime()) / 1000;
        if (s < 60) return "just now";
        if (s < 3600) return (s / 60) + " min ago";
        if (s < 86400) return (s / 3600) + " h ago";
        return (s / 86400) + " d ago";
    }

    /**
     * Custom cursor / hover feedback: every JButton in the app gets a
     * hand cursor and a slightly lighter (or darker) background while the
     * mouse is over it. Safe to call more than once.
     */
    public static synchronized void install() {

        if (installed) return;
        installed = true;

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {

            if (!(event instanceof MouseEvent)) return;

            MouseEvent m = (MouseEvent) event;

            if (!(m.getSource() instanceof JButton)) return;

            JButton b = (JButton) m.getSource();

            if (m.getID() == MouseEvent.MOUSE_ENTERED && b.isEnabled()) {

                if (b.getClientProperty("sud.bg") == null
                        && b.getBackground() != null) {

                    Color original = b.getBackground();
                    Color hover = shift(original);

                    b.putClientProperty("sud.bg", original);
                    b.putClientProperty("sud.hover", hover);
                    b.setBackground(hover);
                }

                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            } else if (m.getID() == MouseEvent.MOUSE_EXITED) {

                Color original = (Color) b.getClientProperty("sud.bg");
                Object hover = b.getClientProperty("sud.hover");

                if (original != null) {
                    if (b.getBackground().equals(hover)) {
                        b.setBackground(original);
                    }
                    b.putClientProperty("sud.bg", null);
                    b.putClientProperty("sud.hover", null);
                }
            }

        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    private static Color shift(Color c) {

        float[] hsb = Color.RGBtoHSB(
                c.getRed(), c.getGreen(), c.getBlue(), null);

        float br = hsb[2] < 0.6f
                ? Math.min(1f, hsb[2] + 0.12f)
                : Math.max(0f, hsb[2] - 0.08f);

        return Color.getHSBColor(hsb[0], hsb[1], br);
    }
}