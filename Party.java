
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Milestone celebration: a short burst of confetti over the whole window.
 * Usage: Party.burst(this);   (any component inside the window)
 */
public final class Party {

    private Party() { }

    public static void burst(Component from) {

        JRootPane root = SwingUtilities.getRootPane(from);

        if (root == null) return;

        Component old = root.getGlassPane();

        Overlay overlay = new Overlay(root.getWidth(), root.getHeight());

        root.setGlassPane(overlay);
        overlay.setVisible(true);

        Timer timer = new Timer(16, null);

        timer.addActionListener(e -> {

            overlay.step();
            overlay.repaint();

            if (overlay.finished()) {
                timer.stop();
                overlay.setVisible(false);
                root.setGlassPane(old);
                old.setVisible(false);
            }
        });

        timer.start();
    }

    private static class Piece {
        double x, y, vx, vy, rot, vr;
        float size;
        Color color;
    }

    private static class Overlay extends JComponent {

        private final List<Piece> pieces = new ArrayList<>();
        private final int h;
        private int frames = 0;

        Overlay(int w, int h) {

            this.h = h;

            setOpaque(false);

            Random r = new Random();

            Color[] palette = {
                Colors.BOOKED, Colors.PICKED_UP, Colors.IN_TRANSIT,
                Colors.OUT, Colors.DELIVERED, new Color(255, 107, 129)
            };

            for (int i = 0; i < 110; i++) {

                Piece p = new Piece();

                p.x = w / 2.0 + r.nextGaussian() * 40;
                p.y = h * 0.35;

                double angle = Math.toRadians(250 + r.nextInt(40) * 1.0
                        + r.nextGaussian() * 18);
                double speed = 6 + r.nextDouble() * 9;

                p.vx = Math.cos(angle) * speed;
                p.vy = Math.sin(angle) * speed;
                p.rot = r.nextDouble() * Math.PI;
                p.vr = (r.nextDouble() - 0.5) * 0.4;
                p.size = 6 + r.nextFloat() * 6;
                p.color = palette[r.nextInt(palette.length)];

                pieces.add(p);
            }
        }

        // lets mouse clicks pass through to the window underneath
        @Override
        public boolean contains(int x, int y) {
            return false;
        }

        void step() {

            frames++;

            for (Piece p : pieces) {
                p.vy += 0.32;
                p.vx *= 0.99;
                p.x += p.vx;
                p.y += p.vy;
                p.rot += p.vr;
            }
        }

        boolean finished() {

            if (frames > 200) return true;

            for (Piece p : pieces) {
                if (p.y < h + 20) return false;
            }

            return true;
        }

        @Override
        protected void paintComponent(Graphics g0) {

            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);

            for (Piece p : pieces) {

                AffineTransform old = g.getTransform();

                g.translate(p.x, p.y);
                g.rotate(p.rot);
                g.setColor(p.color);
                g.fill(new Rectangle2D.Float(-p.size / 2, -p.size / 4,
                        p.size, p.size / 2));

                g.setTransform(old);
            }

            g.dispose();
        }
    }
}