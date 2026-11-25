import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Miniature animated delivery truck that drives back and forth.
 * Meant for empty corners of dashboards. Stops animating when hidden.
 */
public class Mascot extends JComponent {

    private final Color body;
    private final Timer timer;
    private double x = 0;
    private int dir = 1;
    private double wheel = 0;
    private double tick = 0;

    public Mascot(Color body) {

        this.body = body;

        setOpaque(false);
        setPreferredSize(new Dimension(150, 44));
        setMinimumSize(new Dimension(150, 44));
        setToolTipText("SUDWAR delivery truck");

        timer = new Timer(33, e -> {

            double max = Math.max(0, getWidth() - 62);

            x += dir * 1.2;

            if (x >= max) { x = max; dir = -1; }
            if (x <= 0)   { x = 0;   dir = 1; }

            wheel += 0.35;
            tick += 0.2;

            repaint();
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g0) {

        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);

        double bob = Math.sin(tick) * 1.2;
        int baseY = getHeight() - 34;

        AffineTransform at = new AffineTransform();
        at.translate(x + (dir < 0 ? 58 : 0), baseY + bob);
        at.scale(dir, 1);
        g.transform(at);

        // exhaust puffs
        for (int i = 0; i < 3; i++) {
            int a = 90 - i * 28;
            g.setColor(new Color(160, 160, 175, Math.max(10, a)));
            g.fill(new Ellipse2D.Double(-4 - i * 7, 14 - i * 3, 5 + i, 5 + i));
        }

        // cargo box
        g.setColor(body);
        g.fill(new RoundRectangle2D.Double(0, 0, 36, 24, 6, 6));
        g.setColor(Color.WHITE);
        g.setFont(Theme.font(Font.BOLD, 8));
        g.drawString("SUD", 7, 15);

        // cab
        g.setColor(body.darker());
        g.fill(new RoundRectangle2D.Double(37, 7, 20, 17, 6, 6));
        g.setColor(new Color(200, 230, 255));
        g.fill(new RoundRectangle2D.Double(44, 10, 10, 7, 3, 3));

        // wheels
        drawWheel(g, 10, 25);
        drawWheel(g, 46, 25);

        g.dispose();
    }

    private void drawWheel(Graphics2D g, double cx, double cy) {

        g.setColor(new Color(40, 40, 50));
        g.fill(new Ellipse2D.Double(cx - 6, cy - 6, 12, 12));

        g.setColor(new Color(190, 190, 200));
        g.fill(new Ellipse2D.Double(cx - 2.5, cy - 2.5, 5, 5));

        g.setStroke(new BasicStroke(1.2f));
        g.setColor(new Color(120, 120, 135));
        g.drawLine((int) (cx + Math.cos(wheel) * 5),
                   (int) (cy + Math.sin(wheel) * 5),
                   (int) (cx - Math.cos(wheel) * 5),
                   (int) (cy - Math.sin(wheel) * 5));
    }
}