import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Small reusable widgets of the dark design: stat cards, pills, tiles, outline buttons. */
public final class Kit {

    private Kit() { }

    /** Coloured statistic card (icon, label, big number). */
    public static class Stat extends JPanel {

        private final Color accent;
        private final JLabel value = new JLabel("0");

        Stat(String label, String icon, Color accent, Runnable click) {

            this.accent = accent;

            setOpaque(false);
            setLayout(new BorderLayout(14, 0));
            setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 12));
            setPreferredSize(new Dimension(150, 104));

            JLabel ic = new JLabel(Ico.of(icon, 30, accent.brighter())) {
                @Override protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    Theme.smooth(g);
                    g.setColor(Theme.alpha(accent, 55));
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                    g.dispose();
                    super.paintComponent(g0);
                }
            };
            ic.setHorizontalAlignment(SwingConstants.CENTER);
            ic.setPreferredSize(new Dimension(54, 54));

            JLabel l = new JLabel("<html>" + label + "</html>");
            l.setFont(Theme.font(Font.PLAIN, 13));
            l.setForeground(Theme.TXT);

            value.setFont(Theme.font(Font.BOLD, 30));
            value.setForeground(Color.WHITE);

            JPanel t = new JPanel(new BorderLayout());
            t.setOpaque(false);
            t.add(l, BorderLayout.NORTH);
            t.add(value, BorderLayout.CENTER);

            add(ic, BorderLayout.WEST);
            add(t, BorderLayout.CENTER);

            if (click != null) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { click.run(); }
                });
            }
        }

        public void setValue(String v) {
            value.setText(v);
        }

        @Override
        protected void paintComponent(Graphics g0) {

            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);

            int w = getWidth() - 1, h = getHeight() - 1;

            g.setPaint(new GradientPaint(0, 0, Theme.alpha(accent, 90),
                    w, h, Theme.PANEL));
            g.fillRoundRect(0, 0, w, h, 18, 18);

            g.setColor(Theme.alpha(accent, 150));
            g.drawRoundRect(0, 0, w, h, 18, 18);

            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** "Success rate" card with a progress ring. */
    public static JComponent ringStat(String label, Ring ring, JLabel percent) {

        JPanel p = new JPanel(new BorderLayout(14, 0)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.smooth(g);
                g.setColor(Theme.PANEL);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g.setColor(Theme.alpha(Colors.DELIVERED, 140));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 12));

        JLabel l = new JLabel("<html>" + label + "</html>");
        l.setFont(Theme.font(Font.BOLD, 16));
        l.setForeground(Color.WHITE);

        percent.setFont(Theme.font(Font.PLAIN, 12));
        percent.setForeground(Theme.DIM);

        JPanel t = new JPanel(new GridLayout(2, 1));
        t.setOpaque(false);
        t.add(l);
        t.add(percent);

        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(ring);

        p.add(holder, BorderLayout.WEST);
        p.add(t, BorderLayout.CENTER);

        return p;
    }

    /** Panel heading: coloured icon + white title. */
    public static JLabel title(String text, String icon, Color iconColor) {

        JLabel l = new JLabel(text, Ico.of(icon, 22, iconColor), SwingConstants.LEFT);
        l.setIconTextGap(10);
        l.setFont(Theme.font(Font.BOLD, 17));
        l.setForeground(Color.WHITE);

        return l;
    }

    /** Solid status pill. */
    public static JComponent pill(String text, Color c) {

        String shown = text == null || text.isEmpty() ? ""
            : text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();

        JLabel l = new JLabel(shown) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.smooth(g);
                g.setColor(c);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g.dispose();
                super.paintComponent(g0);
            }
        };
        l.setFont(Theme.font(Font.BOLD, 11));
        l.setForeground(Colors.inkOn(c));
        l.setBorder(BorderFactory.createEmptyBorder(3, 13, 3, 13));
        l.setOpaque(false);

        return l;
    }

    private static class Outline implements Border {

        private final Color c;

        Outline(Color c) { this.c = c; }

        @Override
        public void paintBorder(Component comp, Graphics g0, int x, int y, int w, int h) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);
            g.setColor(c);
            g.drawRoundRect(x, y, w - 1, h - 1, 14, 14);
            g.dispose();
        }

        @Override public Insets getBorderInsets(Component c) { return new Insets(5, 14, 5, 14); }
        @Override public boolean isBorderOpaque() { return false; }
    }

    /** Small outlined button ("Track", "Cancel"). */
    public static JButton ghost(String text, Color c) {

        JButton b = new JButton(text);
        b.setFont(Theme.font(Font.BOLD, 11));
        b.setForeground(c.brighter());
        b.setBackground(Theme.alpha(c, 40));
        b.setFocusPainted(false);
        b.setBorder(new Outline(c));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        return b;
    }

    /** Big gradient quick-action tile. */
    public static JComponent tile(String text, String icon, Color top, Color bottom, Runnable click) {

        JComponent t = new JComponent() {

            private boolean hover = false;

            {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                    @Override public void mouseClicked(MouseEvent e) { click.run(); }
                });
            }

            @Override protected void paintComponent(Graphics g0) {

                Graphics2D g = (Graphics2D) g0.create();
                Theme.smooth(g);

                int w = getWidth() - 1, h = getHeight() - 1;

                g.setPaint(new GradientPaint(0, 0, hover ? top.brighter() : top,
                        0, h, bottom));
                g.fillRoundRect(0, 0, w, h, 18, 18);

                g.setColor(Theme.alpha(Color.WHITE, hover ? 110 : 55));
                g.drawRoundRect(0, 0, w, h, 18, 18);

                Ico.of(icon, 30, Color.WHITE).paintIcon(this, g, (w - 30) / 2, h / 2 - 34);

                g.setFont(Theme.font(Font.BOLD, 13));
                g.setColor(Color.WHITE);

                String[] lines = text.split("\n");
                int y = h / 2 + 20;

                for (String ln : lines) {
                    g.drawString(ln, (w - g.getFontMetrics().stringWidth(ln)) / 2, y);
                    y += 17;
                }

                g.dispose();
            }
        };

        t.setPreferredSize(new Dimension(130, 96));

        return t;
    }
}