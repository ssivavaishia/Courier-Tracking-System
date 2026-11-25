import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Date picker: a text field (yyyy-MM-dd) with a calendar icon.
 * Click the icon (or press the Down arrow) to open a calendar popup.
 * It IS a JTextField, so existing code that calls getText()/setText()
 * keeps working. Use getDate()/setDate() for typed access.
 */
public class DatePick extends JTextField {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private LocalDate min = null;
    private LocalDate max = null;
    private Consumer<LocalDate> listener;

    public DatePick() {
        this(null);
    }

    public DatePick(LocalDate initial) {

        super(10);

        setFont(Theme.font(Font.PLAIN, 14));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(185, 200, 182)),
            BorderFactory.createEmptyBorder(6, 8, 6, 34)));
        setToolTipText("yyyy-MM-dd  (click the calendar icon to pick)");

        if (initial != null) setText(initial.format(FMT));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getX() > getWidth() - 34) openPopup();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) openPopup();
            }
        });
    }

    public LocalDate getDate() {

        try {
            return LocalDate.parse(getText().trim(), FMT);
        } catch (Exception ex) {
            return null;
        }
    }

    public void setDate(LocalDate d) {

        setText(d == null ? "" : d.format(FMT));

        if (listener != null) listener.accept(d);
    }

    public void setMinDate(LocalDate d) { this.min = d; }

    public void setMaxDate(LocalDate d) { this.max = d; }

    /** Called when the user picks a date in the popup. */
    public void onPick(Consumer<LocalDate> listener) {
        this.listener = listener;
    }

    private void openPopup() {

        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createLineBorder(Theme.PURPLE));

        LocalDate start = getDate();

        menu.add(new Calendar(start == null ? LocalDate.now() : start, menu));
        menu.show(this, 0, getHeight());
    }

    @Override
    protected void paintComponent(Graphics g0) {

        super.paintComponent(g0);

        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);

        int x = getWidth() - 26, y = (getHeight() - 16) / 2;

        g.setColor(Theme.PLUM);
        g.setStroke(new BasicStroke(1.6f));
        g.drawRoundRect(x, y + 2, 16, 14, 3, 3);
        g.fillRect(x, y + 2, 16, 4);
        g.drawLine(x + 4, y, x + 4, y + 4);
        g.drawLine(x + 12, y, x + 12, y + 4);

        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                g.fillRect(x + 3 + c * 4, y + 9 + r * 3, 2, 2);
            }
        }

        g.dispose();
    }

    // ------------------------------------------------------------ calendar

    private class Calendar extends JPanel {

        private YearMonth shown;
        private final LocalDate selected;
        private final JPopupMenu owner;
        private final JLabel title = new JLabel("", SwingConstants.CENTER);
        private final JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));

        Calendar(LocalDate start, JPopupMenu owner) {

            this.owner = owner;
            this.shown = YearMonth.from(start);
            this.selected = getDate();

            setLayout(new BorderLayout(0, 6));
            setBackground(Theme.PANEL);
            setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 10));

            JButton prev = nav("<");
            JButton next = nav(">");

            prev.addActionListener(e -> { shown = shown.minusMonths(1); fill(); });
            next.addActionListener(e -> { shown = shown.plusMonths(1); fill(); });

            title.setFont(Theme.font(Font.BOLD, 14));
            title.setForeground(Theme.PLUM);

            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);
            head.add(prev, BorderLayout.WEST);
            head.add(title, BorderLayout.CENTER);
            head.add(next, BorderLayout.EAST);

            grid.setOpaque(false);

            JButton today = link("Today");
            today.addActionListener(e -> choose(LocalDate.now()));

            JButton clear = link("Clear");
            clear.addActionListener(e -> {
                setText("");
                owner.setVisible(false);
                if (listener != null) listener.accept(null);
            });

            JPanel foot = new JPanel(new BorderLayout());
            foot.setOpaque(false);
            foot.add(today, BorderLayout.WEST);
            foot.add(clear, BorderLayout.EAST);

            add(head, BorderLayout.NORTH);
            add(grid, BorderLayout.CENTER);
            add(foot, BorderLayout.SOUTH);

            setPreferredSize(new Dimension(272, 250));

            fill();
        }

        private void fill() {

            title.setText(shown.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    + " " + shown.getYear());

            grid.removeAll();

            // week starts on Sunday
            DayOfWeek[] order = {
                DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY};

            for (DayOfWeek d : order) {
                JLabel l = new JLabel(
                    d.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).substring(0, 2),
                    SwingConstants.CENTER);
                l.setFont(Theme.font(Font.BOLD, 11));
                l.setForeground(Theme.PURPLE);
                grid.add(l);
            }

            LocalDate first = shown.atDay(1);
            int offset = first.getDayOfWeek().getValue() % 7;
            LocalDate cell = first.minusDays(offset);
            LocalDate today = LocalDate.now();

            for (int i = 0; i < 42; i++) {

                final LocalDate day = cell.plusDays(i);

                JButton b = new JButton(String.valueOf(day.getDayOfMonth()));
                b.setFont(Theme.font(Font.PLAIN, 12));
                b.setFocusPainted(false);
                b.setOpaque(true);
                b.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

                boolean inMonth = YearMonth.from(day).equals(shown);
                boolean isSel = day.equals(selected);
                boolean disabled = (min != null && day.isBefore(min))
                                || (max != null && day.isAfter(max));

                if (isSel) {
                    b.setBackground(Theme.RED);
                    b.setForeground(Color.WHITE);
                } else if (day.equals(today)) {
                    b.setBackground(Theme.LILAC);
                    b.setForeground(Theme.PLUM);
                    b.setFont(Theme.font(Font.BOLD, 12));
                } else {
                    b.setBackground(Theme.PANEL);
                    b.setForeground(inMonth ? Theme.PLUM
                                            : new Color(175, 165, 180));
                }

                if (disabled) {
                    b.setEnabled(false);
                    b.setForeground(new Color(200, 195, 205));
                } else {
                    b.addActionListener(e -> choose(day));
                }

                grid.add(b);
            }

            grid.revalidate();
            grid.repaint();
        }

        private void choose(LocalDate d) {

            if ((min != null && d.isBefore(min)) || (max != null && d.isAfter(max))) {
                return;
            }

            setText(d.format(FMT));
            owner.setVisible(false);

            if (listener != null) listener.accept(d);
        }

        private JButton nav(String t) {

            JButton b = new JButton(t);
            b.setFont(Theme.font(Font.BOLD, 14));
            b.setForeground(Theme.PLUM);
            b.setBackground(Theme.LILAC);
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 12));

            return b;
        }

        private JButton link(String t) {

            JButton b = new JButton(t);
            b.setFont(Theme.font(Font.BOLD, 12));
            b.setForeground(Theme.PURPLE);
            b.setBackground(Theme.PANEL);
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

            return b;
        }
    }
}
