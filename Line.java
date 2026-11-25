import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Multi-series line chart with soft area fill, grid lines and a legend.
 * setData(labels, names, colors, series...) then it repaints.
 */
public class Line extends JComponent {

    private final boolean dark;
    private String[] labels = new String[0];
    private String[] names = new String[0];
    private Color[] colors = new Color[0];
    private double[][] series = new double[0][];

    public Line(boolean dark) {
        this.dark = dark;
        setOpaque(false);
        setPreferredSize(new Dimension(420, 230));
    }

    public void setData(String[] labels, String[] names, Color[] colors,
                        double[]... series) {

        this.labels = labels;
        this.names = names;
        this.colors = colors;
        this.series = series;

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {

        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);

        Color text = dark ? Theme.TEXT_DIM : new Color(110, 90, 115);
        Color grid = dark ? Theme.alpha(Color.WHITE, 22) : new Color(0, 0, 0, 22);

        int w = getWidth(), h = getHeight();
        int left = 34, right = 10, top = 30, bottom = 24;
        int cw = w - left - right, ch = h - top - bottom;

        if (labels.length < 2 || cw < 20 || ch < 20) {
            g.dispose();
            return;
        }

        double max = 1;
        for (double[] s : series) for (double v : s) max = Math.max(max, v);
        max = Math.ceil(max / 4.0) * 4.0;

        g.setFont(Theme.font(Font.PLAIN, 10));

        // grid + y labels
        for (int i = 0; i <= 4; i++) {
            int y = top + ch - ch * i / 4;
            g.setColor(grid);
            g.drawLine(left, y, left + cw, y);
            g.setColor(text);
            String t = String.valueOf((int) (max * i / 4));
            g.drawString(t, left - 6 - g.getFontMetrics().stringWidth(t), y + 4);
        }

        // x labels (about 7 of them)
        int step = Math.max(1, labels.length / 7);
        for (int i = 0; i < labels.length; i += step) {
            int x = left + cw * i / (labels.length - 1);
            g.setColor(text);
            String t = labels[i];
            g.drawString(t, x - g.getFontMetrics().stringWidth(t) / 2, h - 7);
        }

        // series
        for (int k = 0; k < series.length; k++) {

            double[] s = series[k];
            Color c = colors[k];

            Path2D line = new Path2D.Double();
            Path2D area = new Path2D.Double();

            for (int i = 0; i < s.length; i++) {

                double x = left + cw * (double) i / (s.length - 1);
                double y = top + ch - ch * (s[i] / max);

                if (i == 0) {
                    line.moveTo(x, y);
                    area.moveTo(x, top + ch);
                    area.lineTo(x, y);
                } else {
                    line.lineTo(x, y);
                    area.lineTo(x, y);
                }
            }

            area.lineTo(left + cw, top + ch);
            area.closePath();

            g.setPaint(new GradientPaint(0, top, Theme.alpha(c, 90),
                    0, top + ch, Theme.alpha(c, 0)));
            g.fill(area);

            g.setColor(c);
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g.draw(line);

            // dots
            for (int i = 0; i < s.length; i++) {
                if (s.length > 20 && i % step != 0) continue;
                double x = left + cw * (double) i / (s.length - 1);
                double y = top + ch - ch * (s[i] / max);
                g.setColor(dark ? Theme.NAVY2 : Color.WHITE);
                g.fillOval((int) x - 4, (int) y - 4, 8, 8);
                g.setColor(c);
                g.setStroke(new BasicStroke(2f));
                g.drawOval((int) x - 4, (int) y - 4, 8, 8);
            }
        }

        // legend
        int lx = left;
        g.setFont(Theme.font(Font.BOLD, 11));

        for (int k = 0; k < names.length; k++) {
            g.setColor(colors[k]);
            g.fillRoundRect(lx, 8, 14, 6, 6, 6);
            g.setColor(dark ? Theme.TEXT_LIGHT : Theme.PLUM);
            g.drawString(names[k], lx + 20, 15);
            lx += 20 + g.getFontMetrics().stringWidth(names[k]) + 20;
        }

        g.dispose();
    }
}