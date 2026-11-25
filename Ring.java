import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

/**
 * Circular progress indicator. setProgress(...) animates smoothly from the
 * old value to the new one, which is how a shipment status change is shown.
 */
public class Ring extends JComponent {

    private double shown = 0;
    private double target = 0;
    private Color color = Colors.OTHER;
    private boolean dark = true;
    private boolean showText = true;
    private final Timer anim;

    public Ring(int size) {

        setPreferredSize(new Dimension(size, size));
        setOpaque(false);

        anim = new Timer(16, e -> {

            double diff = target - shown;

            if (Math.abs(diff) < 0.002) {
                shown = target;
                ((Timer) e.getSource()).stop();
            } else {
                shown += diff * 0.12;
            }

            repaint();
        });
    }

    public void setDark(boolean dark) {
        this.dark = dark;
        repaint();
    }

    public void setShowText(boolean showText) {
        this.showText = showText;
        repaint();
    }

    public void setColor(Color color) {
        this.color = color;
        repaint();
    }

    public void setProgress(double p, boolean animate) {

        target = Math.max(0, Math.min(1, p));

        if (animate && isDisplayable()) {
            anim.start();
        } else {
            shown = target;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {

        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);

        int s = Math.min(getWidth(), getHeight());
        float stroke = Math.max(4f, s / 9f);
        double pad = stroke / 2 + 1;
        double d = s - pad * 2;

        g.setStroke(new BasicStroke(stroke));

        g.setColor(dark ? Theme.alpha(Color.WHITE, 40)
                        : new Color(0, 0, 0, 28));
        g.draw(new Ellipse2D.Double(pad, pad, d, d));

        g.setColor(color);
        g.setStroke(new BasicStroke(stroke,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(pad, pad, d, d, 90,
                -360 * shown, Arc2D.OPEN));

        if (showText) {

            String t = Math.round(shown * 100) + "%";

            g.setFont(Theme.font(Font.BOLD, Math.max(9, s / 4)));
            g.setColor(dark ? Theme.TEXT_LIGHT : Theme.PLUM);

            FontMetrics fm = g.getFontMetrics();

            g.drawString(t,
                    (s - fm.stringWidth(t)) / 2f,
                    (s + fm.getAscent() - fm.getDescent()) / 2f);
        }

        g.dispose();
    }

    @Override
    public void removeNotify() {
        anim.stop();
        super.removeNotify();
    }
}