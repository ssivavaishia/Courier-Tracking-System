import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * A distinctive visual card for one shipment: status-coloured accent bar,
 * animated progress ring, tracking number, receiver and a status chip.
 * Call setStatus(...) to animate a status change.
 */
public class Card extends JPanel {

    private final boolean dark;
    private final JLabel trackingLabel = new JLabel();
    private final JLabel receiverLabel = new JLabel();
    private final JLabel addressLabel = new JLabel();
    private final Chip chip = new Chip();
    private final Ring ring = new Ring(54);

    private Color accent = Colors.OTHER;
    private boolean hover = false;
    private Runnable clickAction;

    public Card(boolean dark, String tracking, String receiver,
                String address, String status) {

        this.dark = dark;

        setOpaque(false);
        setLayout(new BorderLayout(12, 0));
        setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 14));
        setPreferredSize(new Dimension(300, 108));

        Color main = dark ? Theme.TEXT_LIGHT : Theme.PLUM;
        Color dim = dark ? Theme.TEXT_DIM : Theme.MUTED;

        trackingLabel.setFont(Theme.font(Font.BOLD, 14));
        trackingLabel.setForeground(main);

        receiverLabel.setFont(Theme.font(Font.PLAIN, 12));
        receiverLabel.setForeground(main);

        addressLabel.setFont(Theme.font(Font.PLAIN, 11));
        addressLabel.setForeground(dim);

        ring.setDark(dark);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        chip.setAlignmentX(Component.LEFT_ALIGNMENT);
        trackingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        receiverLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        addressLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        text.add(trackingLabel);
        text.add(Box.createVerticalStrut(2));
        text.add(receiverLabel);
        text.add(addressLabel);
        text.add(Box.createVerticalStrut(6));
        text.add(chip);

        JPanel ringHolder = new JPanel(new GridBagLayout());
        ringHolder.setOpaque(false);
        ringHolder.add(ring);

        add(ringHolder, BorderLayout.WEST);
        add(text, BorderLayout.CENTER);

        trackingLabel.setText(tracking == null ? "-" : tracking);
        receiverLabel.setText(receiver == null ? "" : receiver);
        addressLabel.setText(shorten(address, 34));

        applyStatus(status, false);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                hover = true;
                setCursor(Cursor.getPredefinedCursor(
                        clickAction == null ? Cursor.DEFAULT_CURSOR
                                            : Cursor.HAND_CURSOR));
                repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (clickAction != null) clickAction.run();
            }
        });
    }

    /** Change the status; the ring animates to the new progress. */
    public void setStatus(String status) {
        applyStatus(status, true);
    }

    public void onClick(Runnable action) {
        this.clickAction = action;
    }

    private void applyStatus(String status, boolean animate) {

        accent = Colors.of(status);

        ring.setColor(accent);
        ring.setProgress(Colors.progress(status), animate);

        chip.setStatus(status, accent);

        repaint();
    }

    private static String shorten(String s, int max) {

        if (s == null) return "";

        s = s.replace('\n', ' ').trim();

        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    @Override
    protected void paintComponent(Graphics g0) {

        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);

        int w = getWidth() - 1;
        int h = getHeight() - 1;

        Shape shape = new RoundRectangle2D.Double(0, 0, w, h, 18, 18);

        Color bg = dark
                ? (hover ? Theme.NAVY3 : Theme.NAVY2)
                : (hover ? new Color(252, 248, 254) : Color.WHITE);

        g.setColor(bg);
        g.fill(shape);

        // status-coloured accent bar on the left
        Shape oldClip = g.getClip();
        g.setClip(shape);
        g.setColor(accent);
        g.fillRect(0, 0, 6, getHeight());
        g.setClip(oldClip);

        g.setColor(hover ? accent
                : dark ? Theme.alpha(Color.WHITE, 30)
                       : Theme.LINE);
        g.setStroke(new BasicStroke(hover ? 1.6f : 1f));
        g.draw(shape);

        g.dispose();
        super.paintComponent(g0);
    }

    /** Small rounded status label. */
    private static class Chip extends JLabel {

        private Color color = Colors.OTHER;

        Chip() {
            setFont(Theme.font(Font.BOLD, 10));
            setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
            setOpaque(false);
        }

        void setStatus(String status, Color color) {

            this.color = color;

            String s = Colors.norm(status);
            setText(s.isEmpty() ? "UNKNOWN" : s);
            setForeground(color);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {

            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);

            g.setColor(Theme.alpha(color, 36));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                    getHeight(), getHeight());

            g.dispose();
            super.paintComponent(g0);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
    }
}