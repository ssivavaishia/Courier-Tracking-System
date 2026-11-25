
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Track Shipment: type a tracking number to see the shipment summary, a
 * five-step progress bar and the full event timeline.
 */
public class Track extends JPanel {

    private static final String[] STEPS = {"Booked", "Picked up", "In transit", "Out for delivery", "Delivered"};
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");

    private static class Log {
        String status, location, remarks;
        Timestamp at;
    }

    private static class Result {
        String tracking, receiver, status;
        Timestamp delivered;
        List<Log> logs = new ArrayList<>();
    }

    private final int customerId;
    private final JTextField input = new JTextField();
    private final JLabel error = new JLabel(" ");
    private final JPanel body = new JPanel(new BorderLayout());

    public Track(int customerId, String customerName) {

        this.customerId = customerId;

        setOpaque(false);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JLabel title = new JLabel("Track Shipment");
        title.setFont(Theme.font(Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Enter your tracking number to follow the parcel.");
        sub.setFont(Theme.font(Font.PLAIN, 13));
        sub.setForeground(Theme.DIM);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(sub);

        input.setFont(Theme.font(Font.BOLD, 16));
        input.setToolTipText("Tracking number");
        input.addActionListener(e -> search());

        JButton go = new JButton("Track");
        go.setIcon(Ico.of("track", 18, Color.WHITE));
        go.setBackground(Theme.RED);
        go.setForeground(Color.WHITE);
        go.setFont(Theme.font(Font.BOLD, 14));
        go.setFocusPainted(false);
        go.setBorder(BorderFactory.createEmptyBorder(11, 24, 11, 24));
        go.addActionListener(e -> search());

        error.setFont(Theme.font(Font.BOLD, 12));
        error.setForeground(new Color(255, 110, 110));

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(input, BorderLayout.CENTER);
        row.add(go, BorderLayout.EAST);

        JPanel bar = new JPanel(new BorderLayout(0, 4));
        bar.setOpaque(false);
        bar.add(row, BorderLayout.CENTER);
        bar.add(error, BorderLayout.SOUTH);

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(titles, BorderLayout.NORTH);
        north.add(bar, BorderLayout.CENTER);

        body.setOpaque(false);
        body.add(new Empty(true, "Where is my parcel?",
            "Type a tracking number above. You will see every step it has been through.",
            null, null), BorderLayout.CENTER);

        add(north, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    private void search() {

        final String t = input.getText().trim();

        if (t.isEmpty()) {
            error.setText("Please enter a tracking number.");
            input.putClientProperty("sud.error", "Required");
            input.repaint();
            return;
        }

        error.setText(" ");
        input.putClientProperty("sud.error", null);
        input.repaint();

        new SwingWorker<Result, Void>() {

            @Override
            protected Result doInBackground() throws Exception {

                String sql =
                    "SELECT s.tracking_number, s.receiver_name, s.delivery_status, "
                    + "s.delivery_date, tl.status, tl.location, tl.remarks, tl.update_time "
                    + "FROM shipments s LEFT JOIN tracking_logs tl "
                    + "ON s.shipment_id = tl.shipment_id "
                    + "WHERE s.tracking_number = ? AND s.customer_id = ? "
                    + "ORDER BY tl.update_time DESC";

                Result r = null;

                try (Connection con = Db.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setString(1, t);
                    ps.setInt(2, customerId);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {

                            if (r == null) {
                                r = new Result();
                                r.tracking = rs.getString(1);
                                r.receiver = rs.getString(2);
                                r.status = rs.getString(3);
                                r.delivered = rs.getTimestamp(4);
                            }

                            if (rs.getString(5) != null) {
                                Log l = new Log();
                                l.status = rs.getString(5);
                                l.location = rs.getString(6);
                                l.remarks = rs.getString(7);
                                l.at = rs.getTimestamp(8);
                                r.logs.add(l);
                            }
                        }
                    }
                }

                return r;
            }

            @Override
            protected void done() {

                try {
                    Result r = get();

                    if (r == null) {
                        error.setText("No shipment found with this tracking number on your account.");
                        input.putClientProperty("sud.error", "Not found");
                        input.repaint();
                        show(new Empty(true, "No shipment found",
                            "Check the tracking number and try again.", null, null));
                    } else {
                        show(result(r));
                    }

                } catch (Exception ex) {
                    error.setText("Could not search: " + ex.getCause());
                }
            }

        }.execute();
    }

    private void show(JComponent c) {
        body.removeAll();
        body.add(c, BorderLayout.CENTER);
        body.revalidate();
        body.repaint();
    }

    // -------------------------------------------------------------- result

    private JComponent result(Result r) {

        Surface summary = new Surface(18);
        summary.setLayout(new BorderLayout(0, 10));
        summary.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel no = new JLabel(r.tracking);
        no.setFont(Theme.font(Font.BOLD, 24));
        no.setForeground(Color.WHITE);

        JLabel to = new JLabel("Receiver: " + r.receiver
                + (r.delivered == null ? "" : "     Delivered: "
                   + r.delivered.toLocalDateTime().format(STAMP)));
        to.setFont(Theme.font(Font.PLAIN, 13));
        to.setForeground(Theme.DIM);

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 4));
        left.setOpaque(false);
        left.add(no);
        left.add(to);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(left, BorderLayout.WEST);
        top.add(Kit.pill(r.status, Colors.of(r.status)), BorderLayout.EAST);

        summary.add(top, BorderLayout.NORTH);
        summary.add(new Stepper(r.status), BorderLayout.CENTER);

        // timeline
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        if (r.logs.isEmpty()) {
            JLabel none = new JLabel("No tracking events recorded yet.");
            none.setForeground(Theme.DIM);
            none.setFont(Theme.font(Font.ITALIC, 13));
            list.add(none);
        }

        for (int i = 0; i < r.logs.size(); i++) {
            list.add(event(r.logs.get(i), i == r.logs.size() - 1));
        }

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(list, BorderLayout.NORTH);

        JScrollPane sc = new JScrollPane(holder,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setBorder(null);
        sc.setOpaque(false);
        sc.getViewport().setOpaque(false);

        Surface timeline = new Surface(18);
        timeline.setLayout(new BorderLayout(0, 10));
        timeline.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 14));
        timeline.add(Kit.title("Shipment Timeline", "clock", Colors.IN_TRANSIT), BorderLayout.NORTH);
        timeline.add(sc, BorderLayout.CENTER);

        JPanel all = new JPanel(new BorderLayout(0, 12));
        all.setOpaque(false);
        all.add(summary, BorderLayout.NORTH);
        all.add(timeline, BorderLayout.CENTER);

        return all;
    }

    private JComponent event(Log l, boolean last) {

        Color c = Colors.of(l.status);

        JComponent dot = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.smooth(g);
                if (!last) {
                    g.setColor(Theme.EDGE);
                    g.fillRect(getWidth() / 2 - 1, 22, 2, getHeight() - 20);
                }
                g.setColor(Theme.alpha(c, 70));
                g.fillOval(getWidth() / 2 - 11, 4, 22, 22);
                g.setColor(c);
                g.fillOval(getWidth() / 2 - 6, 9, 12, 12);
                g.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(34, 60));

        JLabel st = new JLabel(Colors.norm(l.status));
        st.setFont(Theme.font(Font.BOLD, 14));
        st.setForeground(Color.WHITE);

        String where = (l.location == null ? "" : l.location)
                + (l.remarks == null || l.remarks.isEmpty() ? "" : "  -  " + l.remarks);

        JLabel loc = new JLabel(where.isEmpty() ? " " : where);
        loc.setFont(Theme.font(Font.PLAIN, 12));
        loc.setForeground(Theme.TXT);

        JLabel when = new JLabel(l.at == null ? " " : l.at.toLocalDateTime().format(STAMP));
        when.setFont(Theme.font(Font.PLAIN, 11));
        when.setForeground(Theme.DIM);

        JPanel text = new JPanel(new GridLayout(3, 1, 0, 2));
        text.setOpaque(false);
        text.add(st);
        text.add(loc);
        text.add(when);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        row.add(dot, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);

        return row;
    }

    /** Five-step progress bar. */
    private static class Stepper extends JComponent {

        private final int step;
        private final boolean cancelled;
        private final Color color;

        Stepper(String status) {

            String s = Colors.norm(status);

            cancelled = s.equals("CANCELLED");
            color = Colors.of(status);

            switch (s) {
                case "PICKED UP":        step = 1; break;
                case "IN TRANSIT":       step = 2; break;
                case "OUT FOR DELIVERY": step = 3; break;
                case "DELIVERED":        step = 4; break;
                default:                 step = 0;
            }

            setPreferredSize(new Dimension(500, 96));
        }

        @Override
        protected void paintComponent(Graphics g0) {

            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);

            int w = getWidth(), pad = 60, y = 30;
            int gap = (w - 2 * pad) / (STEPS.length - 1);

            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            for (int i = 0; i < STEPS.length - 1; i++) {
                boolean done = !cancelled && i < step;
                g.setColor(done ? color : Theme.EDGE);
                g.drawLine(pad + i * gap + 14, y, pad + (i + 1) * gap - 14, y);
            }

            g.setFont(Theme.font(Font.BOLD, 12));

            for (int i = 0; i < STEPS.length; i++) {

                int cx = pad + i * gap;
                boolean done = !cancelled && i <= step;

                g.setColor(done ? color : Theme.PANEL2);
                g.fillOval(cx - 14, y - 14, 28, 28);
                g.setColor(done ? color : Theme.EDGE);
                g.setStroke(new BasicStroke(2f));
                g.drawOval(cx - 14, y - 14, 28, 28);

                if (done) {
                    g.setColor(Colors.inkOn(color));
                    g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawPolyline(new int[]{cx - 6, cx - 1, cx + 7}, new int[]{y, y + 5, y - 5}, 3);
                }

                g.setColor(done ? Color.WHITE : Theme.DIM);
                int tw = g.getFontMetrics().stringWidth(STEPS[i]);
                g.drawString(STEPS[i], cx - tw / 2, y + 40);
            }

            if (cancelled) {
                g.setColor(Colors.CANCELLED);
                g.setFont(Theme.font(Font.BOLD, 13));
                g.drawString("This shipment was cancelled.", pad, y + 70);
            }

            g.dispose();
        }
    }
}