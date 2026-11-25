import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Friendly "nothing here yet" screen: a small illustration, a title,
 * a helpful instruction and an optional action button.
 */
public class Empty extends JPanel {

    public Empty(String title, String hint) {
        this(true, title, hint, null, null);
    }

    public Empty(boolean dark, String title, String hint,
                 String buttonText, Runnable action) {

        setOpaque(false);
        setLayout(new GridBagLayout());

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        Art art = new Art(dark);
        art.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel t = new JLabel(title);
        t.setFont(Theme.font(Font.BOLD, 17));
        t.setForeground(dark ? Theme.TEXT_LIGHT : Theme.PLUM);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel h = new JLabel("<html><div style='text-align:center;width:260px'>"
                + hint + "</div></html>");
        h.setFont(Theme.font(Font.PLAIN, 13));
        h.setForeground(dark ? Theme.TEXT_DIM : Theme.MUTED);
        h.setAlignmentX(Component.CENTER_ALIGNMENT);

        Mascot truck = new Mascot(dark ? new Color(66, 133, 244) : Theme.PURPLE);
        truck.setPreferredSize(new Dimension(190, 44));
        truck.setMaximumSize(new Dimension(190, 44));
        truck.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(art);
        box.add(Box.createVerticalStrut(8));
        box.add(t);
        box.add(Box.createVerticalStrut(6));
        box.add(h);
        box.add(Box.createVerticalStrut(10));
        box.add(truck);

        if (buttonText != null && action != null) {

            JButton b = new JButton(buttonText);
            b.setBackground(dark ? new Color(66, 133, 244) : Theme.PLUM);
            b.setForeground(Color.WHITE);
            b.setFont(Theme.font(Font.BOLD, 13));
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.addActionListener(e -> action.run());

            box.add(Box.createVerticalStrut(10));
            box.add(b);
        }

        add(box);
    }

    /** Open parcel illustration. */
    private static class Art extends JComponent {

        private final boolean dark;

        Art(boolean dark) {
            this.dark = dark;
            setPreferredSize(new Dimension(150, 96));
            setMaximumSize(new Dimension(150, 96));
        }

        @Override
        protected void paintComponent(Graphics g0) {

            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);

            Color line = dark ? Theme.TEXT_DIM : Theme.PURPLE;
            Color fill = dark ? Theme.NAVY3 : Theme.LILAC;

            int cx = getWidth() / 2;

            // dashed halo
            g.setColor(Theme.alpha(line, 60));
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 6f}, 0f));
            g.drawOval(cx - 52, 4, 104, 88);

            // box body
            Path2D body = new Path2D.Double();
            body.moveTo(cx - 32, 40);
            body.lineTo(cx + 32, 40);
            body.lineTo(cx + 28, 76);
            body.lineTo(cx - 28, 76);
            body.closePath();

            g.setColor(fill);
            g.fill(body);
            g.setColor(line);
            g.setStroke(new BasicStroke(2f));
            g.draw(body);

            // open flaps
            g.drawLine(cx - 32, 40, cx - 44, 28);
            g.drawLine(cx + 32, 40, cx + 44, 28);
            g.drawLine(cx - 44, 28, cx - 20, 28);
            g.drawLine(cx + 44, 28, cx + 20, 28);

            // sparkles
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            sparkle(g, cx, 14, 5);
            sparkle(g, cx - 50, 52, 3);
            sparkle(g, cx + 50, 60, 3);

            g.dispose();
        }

        private void sparkle(Graphics2D g, int x, int y, int r) {
            g.drawLine(x - r, y, x + r, y);
            g.drawLine(x, y - r, x, y + r);
        }
    }
}