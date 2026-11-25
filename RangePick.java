
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;

/**
 * Date-range pill ("01/10/2026 - 31/10/2026"). Click it to choose a preset
 * (this month, last 7 days...) or two dates with the calendar pickers.
 */
public class RangePick extends JComponent {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private LocalDate from, to;
    private BiConsumer<LocalDate, LocalDate> listener;

    public RangePick() {

        LocalDate now = LocalDate.now();
        from = now.withDayOfMonth(1);
        to = now.withDayOfMonth(now.lengthOfMonth());

        setPreferredSize(new Dimension(290, 46));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { choose(); }
        });
    }

    public LocalDate from() { return from; }

    public LocalDate to() { return to; }

    public void onChange(BiConsumer<LocalDate, LocalDate> l) {
        this.listener = l;
    }

    public void set(LocalDate f, LocalDate t) {
        from = f;
        to = t;
        repaint();
        if (listener != null) listener.accept(f, t);
    }

    @Override
    protected void paintComponent(Graphics g0) {

        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);

        int w = getWidth() - 1, h = getHeight() - 1;

        g.setColor(Theme.PANEL);
        g.fillRoundRect(0, 0, w, h, 16, 16);
        g.setColor(Theme.EDGE);
        g.drawRoundRect(0, 0, w, h, 16, 16);

        Ico.of("clock", 22, Theme.TXT).paintIcon(this, g, 14, (h - 22) / 2);

        g.setFont(Theme.font(Font.PLAIN, 14));
        g.setColor(Color.WHITE);
        g.drawString(from.format(FMT) + "  -  " + to.format(FMT), 48, h / 2 + 5);

        g.setColor(Theme.DIM);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(new int[]{w - 28, w - 22, w - 16}, new int[]{h / 2 - 3, h / 2 + 3, h / 2 - 3}, 3);

        g.dispose();
    }

    private void choose() {

        Window owner = SwingUtilities.getWindowAncestor(this);

        JDialog dlg = new JDialog(owner, "Select date range", Dialog.ModalityType.APPLICATION_MODAL);

        DatePick f = new DatePick(from);
        DatePick t = new DatePick(to);

        LocalDate now = LocalDate.now();

        JButton[] presets = {
            preset("This month", dlg, now.withDayOfMonth(1), now.withDayOfMonth(now.lengthOfMonth())),
            preset("Last 7 days", dlg, now.minusDays(6), now),
            preset("Last 30 days", dlg, now.minusDays(29), now),
            preset("This year", dlg, now.withDayOfYear(1), now.withDayOfYear(now.lengthOfYear()))
        };

        JButton apply = new JButton("Apply");
        apply.setBackground(Theme.RED);
        apply.setForeground(Color.WHITE);
        apply.setFont(Theme.font(Font.BOLD, 13));
        apply.setFocusPainted(false);
        apply.setBorder(BorderFactory.createEmptyBorder(10, 26, 10, 26));
        apply.addActionListener(e -> {

            LocalDate a = f.getDate(), b = t.getDate();

            if (a == null || b == null) {
                JOptionPane.showMessageDialog(dlg, "Please choose both dates (yyyy-MM-dd).");
                return;
            }

            if (b.isBefore(a)) {
                JOptionPane.showMessageDialog(dlg, "The end date is before the start date.");
                return;
            }

            dlg.dispose();
            set(a, b);
        });

        JPanel presetRow = new JPanel(new GridLayout(2, 2, 8, 8));
        presetRow.setOpaque(false);
        for (JButton b : presets) presetRow.add(b);

        JPanel dates = new JPanel(new GridLayout(2, 2, 8, 8));
        dates.setOpaque(false);
        dates.add(label("From"));
        dates.add(label("To"));
        dates.add(f);
        dates.add(t);

        JPanel box = new JPanel(new BorderLayout(0, 14));
        box.setBackground(Theme.PANEL);
        box.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        box.add(presetRow, BorderLayout.NORTH);
        box.add(dates, BorderLayout.CENTER);

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        foot.setOpaque(false);
        foot.add(apply);
        box.add(foot, BorderLayout.SOUTH);

        dlg.setContentPane(box);
        dlg.pack();
        dlg.setSize(Math.max(dlg.getWidth(), 420), dlg.getHeight() + 40);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.DIM);
        return l;
    }

    private JButton preset(String text, JDialog dlg, LocalDate a, LocalDate b) {

        JButton p = new JButton(text);
        p.setBackground(new Color(34, 47, 86));
        p.setForeground(Color.WHITE);
        p.setFont(Theme.font(Font.BOLD, 12));
        p.setFocusPainted(false);
        p.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        p.addActionListener(e -> { dlg.dispose(); set(a, b); });

        return p;
    }
}