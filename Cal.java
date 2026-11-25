
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;

/** Month calendar widget: today in red, clickable days, month arrows. */
public class Cal extends JComponent {

    private YearMonth ym = YearMonth.now();
    private LocalDate selected = null;
    private Consumer<LocalDate> listener;

    private final Rectangle prev = new Rectangle();
    private final Rectangle next = new Rectangle();

    public Cal() {

        setPreferredSize(new Dimension(250, 230));
        setOpaque(false);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {

                if (prev.contains(e.getPoint())) { ym = ym.minusMonths(1); repaint(); return; }
                if (next.contains(e.getPoint())) { ym = ym.plusMonths(1); repaint(); return; }

                LocalDate d = dayAt(e.getX(), e.getY());

                if (d != null) {
                    selected = d;
                    repaint();
                    if (listener != null) listener.accept(d);
                }
            }
        });
    }

    public void onPick(Consumer<LocalDate> listener) {
        this.listener = listener;
    }

    private LocalDate dayAt(int x, int y) {

        int w = getWidth(), top = 64, cw = w / 7, rh = Math.max(24, (getHeight() - top) / 6);

        if (y < top) return null;

        int col = Math.min(6, x / Math.max(1, cw));
        int row = (y - top) / rh;

        if (row < 0 || row > 5) return null;

        int offset = ym.atDay(1).getDayOfWeek().getValue() % 7;
        int dayNo = row * 7 + col - offset + 1;

        if (dayNo < 1 || dayNo > ym.lengthOfMonth()) return null;

        return ym.atDay(dayNo);
    }

    @Override
    protected void paintComponent(Graphics g0) {

        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);

        int w = getWidth(), h = getHeight();

        // header
        g.setFont(Theme.font(Font.BOLD, 14));
        g.setColor(Color.WHITE);
        String title = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + ym.getYear();
        g.drawString(title, (w - g.getFontMetrics().stringWidth(title)) / 2, 18);

        prev.setBounds(0, 0, 36, 28);
        next.setBounds(w - 36, 0, 36, 28);

        g.setColor(Theme.DIM);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(new int[]{20, 14, 20}, new int[]{8, 14, 20}, 3);
        g.drawPolyline(new int[]{w - 20, w - 14, w - 20}, new int[]{8, 14, 20}, 3);

        // weekdays
        String[] wd = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
        int cw = w / 7;
        g.setFont(Theme.font(Font.PLAIN, 12));
        g.setColor(Theme.TXT);

        for (int i = 0; i < 7; i++) {
            g.drawString(wd[i], i * cw + (cw - g.getFontMetrics().stringWidth(wd[i])) / 2, 52);
        }

        // days
        int top = 64, rh = Math.max(24, (h - top) / 6);
        int offset = ym.atDay(1).getDayOfWeek().getValue() % 7;
        LocalDate today = LocalDate.now();

        for (int d = 1; d <= ym.lengthOfMonth(); d++) {

            int idx = offset + d - 1;
            int cx = (idx % 7) * cw + cw / 2;
            int cy = top + (idx / 7) * rh + rh / 2;

            LocalDate date = ym.atDay(d);
            String t = String.valueOf(d);

            if (date.equals(today)) {
                g.setColor(Theme.RED);
                g.fillOval(cx - 14, cy - 14, 28, 28);
            } else if (date.equals(selected)) {
                g.setColor(Theme.BLUE);
                g.setStroke(new BasicStroke(2f));
                g.drawOval(cx - 14, cy - 14, 28, 28);
            }

            g.setColor(Color.WHITE);
            g.drawString(t, cx - g.getFontMetrics().stringWidth(t) / 2, cy + 5);
        }

        g.dispose();
    }
}