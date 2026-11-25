import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;

/**
 * Donut chart with a legend. The ring sweeps in when the data changes.
 */
public class Donut extends JComponent {

    private final boolean dark;
    private String[] labels = new String[0];
    private double[] values = new double[0];
    private Color[] colors = new Color[0];
    private String centerCaption = "total";
    private String suffix = "";
    private double sweep = 1;
    private final Timer anim;

    public Donut(boolean dark) {

        this.dark = dark;

        setOpaque(false);
        setPreferredSize(new Dimension(360, 190));

        anim = new Timer(16, e -> {
            sweep += 0.05;
            if (sweep >= 1) { sweep = 1; ((Timer) e.getSource()).stop(); }
            repaint();
        });
    }

    public void setSuffix(String suffix) { this.suffix = suffix; }

    /** Sets the chart data. */
    public void setData(String caption, String[] labels, double[] values,
                        Color[] colors) {

        this.centerCaption = caption;
        this.labels = labels;
        this.values = values;
        this.colors = colors;

        sweep = 0;

        if (isDisplayable()) anim.start(); else sweep = 1;

        repaint();
    }

    @Override
    public void removeNotify() {
        anim.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g0) {

        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);

        double total = 0;
        for (double v : values) total += v;

        int h = getHeight();
        int d = Math.min(h - 10, 170);
        int x0 = 8, y0 = (h - d) / 2;
        float stroke = d / 6f;

        g.setStroke(new BasicStroke(stroke));
        g.setColor(dark ? Theme.alpha(Color.WHITE, 30) : new Color(0, 0, 0, 25));
        g.drawOval(x0 + (int) (stroke / 2), y0 + (int) (stroke / 2),
                (int) (d - stroke), (int) (d - stroke));

        if (total > 0) {

            double angle = 90;

            for (int i = 0; i < values.length; i++) {

                if (values[i] <= 0) continue;

                double a = -360.0 * values[i] / total * sweep;

                g.setColor(colors[i]);
                g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Double(x0 + stroke / 2, y0 + stroke / 2,
                        d - stroke, d - stroke, angle, a, Arc2D.OPEN));

                angle += a;
            }
        }

        // centre text
        g.setColor(dark ? Theme.TEXT_LIGHT : Theme.PLUM);
        g.setFont(Theme.font(Font.BOLD, 22));
        String t = String.valueOf(Math.round(total * sweep));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(t, x0 + (d - fm.stringWidth(t)) / 2, y0 + d / 2 + 4);

        g.setFont(Theme.font(Font.PLAIN, 10));
        g.setColor(dark ? Theme.TEXT_DIM : new Color(110, 90, 115));
        fm = g.getFontMetrics();
        g.drawString(centerCaption,
                x0 + (d - fm.stringWidth(centerCaption)) / 2, y0 + d / 2 + 20);

        // legend
        int lx = x0 + d + 22, ly = Math.max(14, h / 2 - values.length * 11);

        g.setFont(Theme.font(Font.PLAIN, 12));

        for (int i = 0; i < values.length; i++) {

            g.setColor(colors[i]);
            g.fillRoundRect(lx, ly + i * 22 - 9, 10, 10, 4, 4);

            g.setColor(dark ? Theme.TEXT_LIGHT : new Color(70, 50, 75));

            String v = values[i] == Math.rint(values[i])
                ? String.valueOf((long) values[i])
                : String.format("%.1f", values[i]);

            g.drawString(labels[i] + "  " + v + suffix, lx + 18, ly + i * 22);
        }

        g.dispose();
    }
}