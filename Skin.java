import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;

/**
 * SUDWAR design layer. Install once (Welcome does it) and EVERY screen of the
 * application follows the dark design:
 *
 *   - dark colours and one font family for all components, dialogs included
 *   - rounded buttons with hover feedback
 *   - tables: dark header, hover rows, coloured status pills, empty message
 *   - slim scroll bars, rounded fields
 *   - LIVE VALIDATION on every page: phone, PIN code, e-mail, password and
 *     number fields show a red border and a message while you type, exactly
 *     like the Register page
 */
public final class Skin {

    private static boolean installed = false;

    private Skin() { }

    public static synchronized void install() {

        if (installed) return;
        installed = true;

        Theme.install();

        UIManager.put("ButtonUI", "Skin$BtnUI");
        UIManager.put("ScrollBarUI", "Skin$BarUI");

        // Nothing is changed globally: Welcome, Login and Register keep their
        // own look. Only components inside a SUDWAR dashboard window (Shell),
        // and the dialogs it opens, are restyled when they appear.
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {

            if (!(event instanceof HierarchyEvent)) return;

            HierarchyEvent h = (HierarchyEvent) event;

            if ((h.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) return;

            Component c = h.getComponent();

            if (c.isShowing() && c instanceof JComponent) polish((JComponent) c);

        }, AWTEvent.HIERARCHY_EVENT_MASK);
    }

    /** True when the component lives in a SUDWAR dashboard window (or a dialog opened by one). */
    public static boolean inShell(Component c) {

        Window w = SwingUtilities.getWindowAncestor(c);

        while (w != null) {
            if (w instanceof Shell) return true;
            w = w.getOwner();
        }

        return false;
    }

    // ------------------------------------------------------------ polishing

    private static void polish(JComponent c) {

        if (Boolean.TRUE.equals(c.getClientProperty("sud.skin"))) return;
        if (!inShell(c)) return;

        c.putClientProperty("sud.skin", Boolean.TRUE);

        Font f = c.getFont();

        if (f != null && !f.getFamily().equals(Theme.family())
                && !f.getFamily().contains("Emoji")
                && !f.getFamily().contains("Symbol")
                && !f.getFamily().contains("Mono")) {
            c.setFont(Theme.font(f.getStyle(), Math.max(12, f.getSize())));
        }

        if (c instanceof JTable) {
            table((JTable) c);
            return;
        }

        boolean field = c instanceof JTextComponent || c instanceof JComboBox
                || c instanceof JList || c instanceof JSpinner;

        // background
        Color bg = c.getBackground();

        if (bg instanceof UIResource) {

            if (field) c.setBackground(Theme.PANEL2);
            else if (c instanceof AbstractButton && !(c instanceof JCheckBox)
                    && !(c instanceof JRadioButton)) c.setBackground(new Color(34, 47, 86));
            else if (!(c instanceof JLabel)) c.setBackground(Theme.PANEL);

        } else {
            c.setBackground(bgFor(c, bg));
        }

        // text colour
        Color fg = c.getForeground();

        c.setForeground(fg instanceof UIResource ? Theme.TXT : fgFor(fg));

        if (c instanceof JTextComponent) {
            ((JTextComponent) c).setCaretColor(Theme.TXT);
            ((JTextComponent) c).setSelectionColor(new Color(40, 80, 170));
            ((JTextComponent) c).setSelectedTextColor(Color.WHITE);
            if (c.getBorder() instanceof UIResource && !(c instanceof JTextArea)) {
                c.setBorder(new Field());
            }
        }

        if (c instanceof JList) {
            ((JList<?>) c).setSelectionBackground(new Color(40, 80, 170));
            ((JList<?>) c).setSelectionForeground(Color.WHITE);
        }

        borders(c);

        if (c instanceof JScrollPane) {
            ((JScrollPane) c).getViewport().setBackground(Theme.PANEL);
        }

        if (c instanceof JTextComponent) validation((JTextComponent) c);
    }

    private static void borders(JComponent c) {

        Border b = c.getBorder();

        if (b instanceof TitledBorder) {
            TitledBorder t = (TitledBorder) b;
            t.setTitleColor(Theme.DIM);
            t.setTitleFont(Theme.font(Font.BOLD, 12));
            t.setBorder(new LineBorder(Theme.EDGE, 1, true));
        } else if (b instanceof LineBorder && !(b instanceof UIResource)) {
            c.setBorder(new LineBorder(Theme.EDGE, ((LineBorder) b).getThickness()));
        } else if (b instanceof CompoundBorder) {
            CompoundBorder cb = (CompoundBorder) b;
            if (cb.getOutsideBorder() instanceof LineBorder) {
                c.setBorder(new CompoundBorder(
                    new LineBorder(Theme.EDGE, 1, true), cb.getInsideBorder()));
            }
        }
    }

    private static float[] hsb(Color c) {
        return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
    }

    private static double lum(Color c) {
        return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
    }

    private static boolean purple(float h, float s) {
        return s > 0.10f && h >= 0.68f && h <= 0.95f;
    }

    /** Light backgrounds become dark panels, old purple buttons become red. */
    static Color bgFor(JComponent c, Color col) {

        if (col == null) return null;

        float[] h = hsb(col);
        double l = lum(col);
        boolean input = c instanceof JTextComponent || c instanceof JComboBox
                || c instanceof JList || c instanceof JSpinner;
        int a = col.getAlpha();

        Color out = col;

        if (c instanceof AbstractButton && !(c instanceof JCheckBox)
                && !(c instanceof JRadioButton)) {

            if (l > 0.60 && h[1] < 0.25f) out = new Color(34, 47, 86);
            else if (purple(h[0], h[1]) || (l < 0.22 && h[1] > 0.2f)) out = Theme.RED;

        } else if (l > 0.60 && h[1] < 0.30f) {
            out = input ? Theme.PANEL2 : Theme.PANEL;
        } else if (purple(h[0], h[1])) {
            out = l < 0.5 ? Theme.NAVY3 : Theme.BLUE;
        }

        return a == 255 ? out : new Color(out.getRed(), out.getGreen(), out.getBlue(), a);
    }

    static Color fgFor(Color col) {

        if (col == null) return null;

        float[] h = hsb(col);

        if (lum(col) < 0.55 || purple(h[0], h[1])) {
            return h[1] > 0.5f && !purple(h[0], h[1]) ? col : Theme.TXT;
        }

        return col;
    }

    // ------------------------------------------------------------ validation

    private static String labelOf(JTextComponent f) {

        StringBuilder sb = new StringBuilder();

        if (f.getName() != null) sb.append(f.getName()).append(' ');
        if (f.getToolTipText() != null) sb.append(f.getToolTipText()).append(' ');

        Container p = f.getParent();
        int hops = 0;
        Component child = f;

        while (p != null && hops < 3) {

            Component[] kids = p.getComponents();
            int idx = -1;

            for (int i = 0; i < kids.length; i++) if (kids[i] == child) idx = i;

            for (int i = idx - 1; i >= 0 && i >= idx - 3; i--) {
                if (kids[i] instanceof JLabel && ((JLabel) kids[i]).getText() != null) {
                    sb.append(((JLabel) kids[i]).getText()).append(' ');
                    return sb.toString().toLowerCase();
                }
            }

            child = p;
            p = p.getParent();
            hops++;
        }

        return sb.toString().toLowerCase();
    }

    private static void validation(JTextComponent f) {

        if (f instanceof DatePick || !f.isEditable()) return;
        if (f instanceof JTextArea || f instanceof JEditorPane) return;

        Window w = SwingUtilities.getWindowAncestor(f);

        // Register and Login already have their own messages
        if (w instanceof Register || w instanceof Login) return;

        final String hint = labelOf(f);

        final int kind;

        if (f instanceof JPasswordField) {
            kind = hint.contains("password") && !hint.contains("confirm")
                    && !hint.contains("current") && !hint.contains("old") ? 4 : 0;
        } else if (hint.contains("phone") || hint.contains("mobile")
                || hint.contains("contact no") || hint.contains("contact number")) {
            kind = 1;
        } else if (hint.matches(".*\\b(pin|pincode|postal|zip)\\b.*")
                || hint.contains("pin code")) {
            kind = 2;
        } else if (hint.contains("mail")) {
            kind = 3;
        } else if (hint.matches(".*\\b(weight|amount|charges?|price|quantity)\\b.*")) {
            kind = 5;
        } else {
            kind = 0;
        }

        if (kind == 0) return;

        f.setBorder(new Err(f.getBorder()));

        Runnable check = () -> {

            String t = f instanceof JPasswordField
                    ? new String(((JPasswordField) f).getPassword()) : f.getText().trim();

            String msg = null;

            if (!t.isEmpty()) {
                switch (kind) {
                    case 1: if (!t.matches("[6-9][0-9]{9}"))
                                msg = "Enter a valid 10-digit phone number."; break;
                    case 2: if (!t.matches("[1-9][0-9]{5}"))
                                msg = "Enter a valid 6-digit PIN code."; break;
                    case 3: if (!t.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
                                msg = "Enter a valid email address."; break;
                    case 4: if (!t.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,20}$"))
                                msg = "Use 8-20 characters, upper, lower, number, symbol."; break;
                    default: if (!t.matches("^[0-9]+(\\.[0-9]+)?$"))
                                msg = "Enter a valid number."; break;
                }
            }

            f.putClientProperty("sud.error", msg);
            f.setToolTipText(msg);
            f.repaint();
        };

        f.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { check.run(); }
            public void removeUpdate(DocumentEvent e) { check.run(); }
            public void changedUpdate(DocumentEvent e) { check.run(); }
        });

        f.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { check.run(); }
        });

        check.run();
    }

    // --------------------------------------------------------------- tables

    private static void table(JTable t) {

        t.setRowHeight(Math.max(t.getRowHeight(), 42));
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFont(Theme.font(Font.PLAIN, 13));
        t.setForeground(Theme.TXT);
        t.setBackground(Theme.PANEL);
        t.setSelectionBackground(new Color(34, 60, 120));
        t.setSelectionForeground(Color.WHITE);
        t.setFillsViewportHeight(true);
        t.setUI(new EmptyAwareUI());

        final Cells cells = new Cells();

        t.setDefaultRenderer(Object.class, cells);
        t.setDefaultRenderer(Number.class, cells);

        MouseAdapter hover = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int r = t.rowAtPoint(e.getPoint());
                if (r != cells.hover) { cells.hover = r; t.repaint(); }
            }
            @Override public void mouseExited(MouseEvent e) {
                cells.hover = -1;
                t.repaint();
            }
        };
        t.addMouseMotionListener(hover);
        t.addMouseListener(hover);

        JTableHeader th = t.getTableHeader();

        if (th != null) {
            th.setDefaultRenderer(new Head());
            th.setPreferredSize(new Dimension(100, 44));
            th.setReorderingAllowed(false);
            th.setOpaque(true);
            th.setBackground(new Color(11, 17, 38));
        }

        JScrollPane sp = (JScrollPane) SwingUtilities
                .getAncestorOfClass(JScrollPane.class, t);

        if (sp != null) {
            sp.setBorder(new LineBorder(Theme.EDGE, 1, true));
            sp.getViewport().setBackground(Theme.PANEL);
        }
    }

    private static class Head implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {

            JLabel l = new JLabel(v == null ? "" : v.toString());
            l.setOpaque(true);
            l.setBackground(new Color(11, 17, 38));
            l.setForeground(Theme.DIM);
            l.setFont(Theme.font(Font.BOLD, 12));
            l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.EDGE),
                BorderFactory.createEmptyBorder(0, 14, 0, 14)));

            return l;
        }
    }

    private static class Cells extends DefaultTableCellRenderer {

        int hover = -1;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {

            Color bg = sel ? new Color(34, 60, 120)
                     : row == hover ? new Color(26, 38, 74)
                     : row % 2 == 0 ? Theme.PANEL : new Color(17, 25, 52);

            String text = v == null ? "" : v.toString();
            String name = t.getColumnName(col).toLowerCase();

            if (name.contains("status") && !text.trim().isEmpty()) {
                return new Pill(text, bg);
            }

            JLabel l = (JLabel) super.getTableCellRendererComponent(
                    t, v, sel, false, row, col);

            l.setOpaque(true);
            l.setBackground(bg);
            l.setForeground(Theme.TXT);
            l.setFont(Theme.font(name.contains("tracking") || name.contains("name")
                    ? Font.BOLD : Font.PLAIN, 13));
            l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(26, 37, 70)),
                BorderFactory.createEmptyBorder(0, 14, 0, 14)));

            boolean numeric = v instanceof Number
                    || text.matches("^[₹$]?\\s?[0-9][0-9,]*(\\.[0-9]+)?$");

            l.setHorizontalAlignment(numeric && !name.contains("id")
                    && !name.contains("phone") && !name.contains("pin")
                    ? SwingConstants.RIGHT : SwingConstants.LEFT);

            if (name.equals("id") || name.endsWith(" id") || name.endsWith("_id")) {
                l.setForeground(Theme.DIM);
            }

            return l;
        }
    }

    /** Solid coloured status pill, like the reference design. */
    private static class Pill extends JComponent {

        private final String text;
        private final Color row;

        Pill(String text, Color row) {
            this.text = text.trim();
            this.row = row;
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g0) {

            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);

            g.setColor(row);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(26, 37, 70));
            g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

            Color c = Colors.of(text);

            g.setFont(Theme.font(Font.BOLD, 11));

            String shown = text.length() > 0
                ? text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase() : "";
            int pw = g.getFontMetrics().stringWidth(shown) + 26, ph = 24;
            int x = 12, y = (getHeight() - ph) / 2;

            g.setColor(c);
            g.fillRoundRect(x, y, pw, ph, ph, ph);

            g.setColor(Colors.inkOn(c));
            g.drawString(shown, x + 13,
                y + (ph + g.getFontMetrics().getAscent()
                        - g.getFontMetrics().getDescent()) / 2);

            g.dispose();
        }
    }

    private static class EmptyAwareUI extends BasicTableUI {

        @Override
        public void paint(Graphics g0, JComponent c) {

            super.paint(g0, c);

            JTable t = (JTable) c;

            if (t.getRowCount() > 0) return;

            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);

            Rectangle r = t.getVisibleRect();
            int cx = r.x + r.width / 2, cy = r.y + Math.min(r.height / 2, 120);

            g.setColor(Theme.alpha(Theme.DIM, 160));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(cx - 22, cy - 38, 44, 34, 8, 8);
            g.drawLine(cx - 22, cy - 26, cx + 22, cy - 26);
            g.drawLine(cx - 6, cy - 38, cx - 6, cy - 26);

            g.setFont(Theme.font(Font.BOLD, 14));
            g.setColor(Theme.TXT);
            String a = "Nothing here yet";
            g.drawString(a, cx - g.getFontMetrics().stringWidth(a) / 2, cy + 10);

            g.setFont(Theme.font(Font.PLAIN, 12));
            g.setColor(Theme.DIM);
            String b = "Records will appear in this table as they are created.";
            g.drawString(b, cx - g.getFontMetrics().stringWidth(b) / 2, cy + 30);

            g.dispose();
        }
    }

    // -------------------------------------------------------------- buttons

    public static class BtnUI extends BasicButtonUI {

        public static ComponentUI createUI(JComponent c) {
            return new BtnUI();
        }

        @Override
        public void update(Graphics g0, JComponent c) {

            if (!inShell(c)) {
                super.update(g0, c);
                return;
            }

            if (c.isOpaque()) c.setOpaque(false);

            AbstractButton b = (AbstractButton) c;

            if (b.isContentAreaFilled()) {

                Graphics2D g = (Graphics2D) g0.create();
                Theme.smooth(g);

                Color bg = b.getBackground();

                if (!b.isEnabled()) bg = Theme.alpha(bg, 110);
                else if (b.getModel().isPressed()) bg = bg.darker();

                int arc = Math.min(16, c.getHeight());

                g.setColor(bg);
                g.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), arc, arc);

                g.dispose();
            }

            paint(g0, c);
        }
    }

    public static class BarUI extends BasicScrollBarUI {

        public static ComponentUI createUI(JComponent c) {
            return new BarUI();
        }

        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(110, 122, 156);
            trackColor = new Color(0, 0, 0, 0);
        }

        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }

        private JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return new Dimension(10, 10);
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }

        @Override
        protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {

            if (r.isEmpty()) return;

            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);
            g.setColor(thumbColor);
            g.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g.dispose();
        }
    }

    // ---------------------------------------------------------- field borders

    /** Default rounded field border (a UIResource, so code can override it). */
    public static class Field extends AbstractBorder implements UIResource {

        @Override
        public void paintBorder(Component c, Graphics g0, int x, int y, int w, int h) {

            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);

            g.setColor(c.hasFocus() ? Theme.BLUE : Theme.EDGE);
            g.setStroke(new BasicStroke(c.hasFocus() ? 1.8f : 1.2f));
            g.drawRoundRect(x, y, w - 1, h - 1, 12, 12);

            g.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(8, 12, 8, 12);
        }
    }

    /** Wraps any field border and draws the red validation message. */
    public static class Err extends AbstractBorder {

        private final Border inner;

        Err(Border inner) {
            this.inner = inner;
        }

        @Override
        public void paintBorder(Component c, Graphics g0, int x, int y, int w, int h) {

            if (inner != null) inner.paintBorder(c, g0, x, y, w, h);

            Object e = ((JComponent) c).getClientProperty("sud.error");

            if (e == null) return;

            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);

            g.setColor(new Color(255, 90, 90));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(x, y, w - 1, h - 1, 12, 12);

            g.setFont(Theme.font(Font.BOLD, 10));

            String msg = e.toString();
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(msg);

            if (tw + 12 < w) {
                g.setColor(Theme.PANEL2);
                g.fillRect(x + w - tw - 14, y + 1, tw + 10, 13);
                g.setColor(new Color(255, 110, 110));
                g.drawString(msg, x + w - tw - 9, y + 11);
            }

            g.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return inner == null ? new Insets(2, 2, 2, 2) : inner.getBorderInsets(c);
        }
    }
}