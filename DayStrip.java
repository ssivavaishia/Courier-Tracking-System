import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Week strip date selector ("Jan 2023  < Wed 01 Thu 02 Fri 03 ... >").
 * Arrows move one week; clicking a day selects it. With allowAll a small
 * "All" button clears the selection (date == null).
 */
public class DayStrip extends JPanel {

    private final boolean dark;
    private final boolean allowAll;
    private LocalDate selected;
    private LocalDate weekStart;
    private Consumer<LocalDate> listener;

    public DayStrip(boolean dark, boolean allowAll, LocalDate initial) {

        this.dark = dark;
        this.allowAll = allowAll;
        this.selected = initial;

        LocalDate base = initial == null ? LocalDate.now() : initial;
        this.weekStart = base.minusDays(3);

        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));

        rebuild();
    }

    public LocalDate getDate() { return selected; }

    public void setDate(LocalDate d) {
        selected = d;
        if (d != null) weekStart = d.minusDays(3);
        rebuild();
    }

    public void onChange(Consumer<LocalDate> listener) {
        this.listener = listener;
    }

    private void pick(LocalDate d) {

        selected = d;
        rebuild();

        if (listener != null) listener.accept(d);
    }

    private void rebuild() {

        removeAll();

        Color text = dark ? Theme.TEXT_LIGHT : Theme.PLUM;
        Color dim = dark ? Theme.TEXT_DIM : Theme.MUTED;
        Color chip = dark ? Theme.NAVY3 : Color.WHITE;
        Color on = dark ? new Color(66, 133, 244) : Theme.PLUM;

        LocalDate ref = selected == null ? LocalDate.now() : selected;

        JLabel month = new JLabel(
            ref.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            + " " + ref.getYear());
        month.setFont(Theme.font(Font.BOLD, 14));
        month.setForeground(text);
        month.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        add(month);

        JButton prev = arrow("<", chip, text);
        prev.addActionListener(e -> { weekStart = weekStart.minusDays(7); rebuild(); });
        add(prev);

        LocalDate today = LocalDate.now();

        for (int i = 0; i < 7; i++) {

            final LocalDate d = weekStart.plusDays(i);
            boolean sel = d.equals(selected);

            JButton b = new JButton("<html><center><font size=2>"
                + d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                + "</font><br><b>" + String.format("%02d", d.getDayOfMonth())
                + "</b></center></html>");

            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setPreferredSize(new Dimension(50, 46));
            b.setBorder(BorderFactory.createLineBorder(
                d.equals(today) && !sel ? on : Theme.alpha(dim, 60)));
            b.setBackground(sel ? on : chip);
            b.setForeground(sel ? Color.WHITE : text);
            b.addActionListener(e -> pick(d));

            add(b);
        }

        JButton next = arrow(">", chip, text);
        next.addActionListener(e -> { weekStart = weekStart.plusDays(7); rebuild(); });
        add(next);

        if (allowAll) {

            JButton all = new JButton("All days");
            all.setFont(Theme.font(Font.BOLD, 12));
            all.setFocusPainted(false);
            all.setOpaque(true);
            all.setBackground(selected == null ? on : chip);
            all.setForeground(selected == null ? Color.WHITE : text);
            all.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
            all.addActionListener(e -> pick(null));
            add(all);
        }

        revalidate();
        repaint();
    }

    private JButton arrow(String t, Color bg, Color fg) {

        JButton b = new JButton(t);
        b.setFont(Theme.font(Font.BOLD, 14));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(32, 46));
        b.setBorder(BorderFactory.createEmptyBorder());

        return b;
    }
}