import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity Feed: a chronological list of the latest events across the
 * courier system (every row written to tracking_logs). Refreshes itself
 * every 30 seconds while it is on screen.
 */
public class Feed extends JPanel {

    private static class Row {
        String tracking, status, location, remarks, by;
        Timestamp time;
    }

    private final boolean dark;
    private final int limit;

    private final JPanel list = new JPanel();
    private final JPanel emptyHolder = new JPanel(new BorderLayout());
    private final JPanel body = new JPanel(new CardLayout());
    private final Timer autoRefresh;
    private java.time.LocalDate day = null;

    public Feed(boolean dark, boolean withHeader, int limit) {

        this.dark = dark;
        this.limit = limit;

        setOpaque(false);
        setLayout(new BorderLayout(0, 10));

        if (withHeader) {

            JLabel title = new JLabel("Activity Feed");
            title.setFont(Theme.font(Font.BOLD, 26));
            title.setForeground(Theme.PLUM);

            JLabel sub = new JLabel(
                "Important events across the courier system, newest first.");
            sub.setFont(Theme.font(Font.PLAIN, 13));
            sub.setForeground(Theme.MUTED);

            JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
            titles.setOpaque(false);
            titles.add(title);
            titles.add(sub);

            JButton refresh = new JButton("Refresh");
            refresh.setBackground(Theme.RED);
            refresh.setForeground(Color.WHITE);
            refresh.setFont(Theme.font(Font.BOLD, 13));
            refresh.setFocusPainted(false);
            refresh.setOpaque(true);
            refresh.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
            refresh.addActionListener(e -> reload());

            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(titles, BorderLayout.WEST);
            top.add(refresh, BorderLayout.EAST);

            DayStrip strip = new DayStrip(true, true, null);
            strip.onChange(dd -> { day = dd; reload(); });

            JPanel north = new JPanel(new BorderLayout(0, 10));
            north.setOpaque(false);
            north.add(top, BorderLayout.NORTH);
            north.add(strip, BorderLayout.CENTER);

            add(north, BorderLayout.NORTH);
        }

        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        JPanel listHolder = new JPanel(new BorderLayout());
        listHolder.setOpaque(false);
        listHolder.add(list, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(listHolder,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        emptyHolder.setOpaque(false);

        body.setOpaque(false);
        body.add(scroll, "list");
        body.add(emptyHolder, "empty");

        add(body, BorderLayout.CENTER);

        autoRefresh = new Timer(30_000, e -> reload());
    }

    @Override
    public void addNotify() {
        super.addNotify();
        reload();
        autoRefresh.start();
    }

    @Override
    public void removeNotify() {
        autoRefresh.stop();
        super.removeNotify();
    }

    public void reload() {

        new SwingWorker<List<Row>, Void>() {

            @Override
            protected List<Row> doInBackground() throws Exception {

                String sql =
                    "SELECT s.tracking_number, tl.status, tl.location, "
                    + "tl.remarks, tl.update_time, u.full_name "
                    + "FROM tracking_logs tl "
                    + "JOIN shipments s ON tl.shipment_id = s.shipment_id "
                    + "LEFT JOIN users u ON tl.updated_by = u.user_id "
                    + (day == null ? "" : "WHERE DATE(tl.update_time) = ? ")
                    + "ORDER BY tl.update_time DESC LIMIT ?";

                List<Row> rows = new ArrayList<>();

                try (Connection con = Db.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {

                    int pi = 1;
                    if (day != null) ps.setDate(pi++, java.sql.Date.valueOf(day));
                    ps.setInt(pi, limit);

                    try (ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {

                            Row r = new Row();
                            r.tracking = rs.getString(1);
                            r.status = rs.getString(2);
                            r.location = rs.getString(3);
                            r.remarks = rs.getString(4);
                            r.time = rs.getTimestamp(5);
                            r.by = rs.getString(6);
                            rows.add(r);
                        }
                    }
                }

                return rows;
            }

            @Override
            protected void done() {

                try {
                    show(get());
                } catch (Exception ex) {
                    showMessage("Could not load activity",
                        "Check the database connection and try Refresh.");
                }
            }

        }.execute();
    }

    private void show(List<Row> rows) {

        list.removeAll();

        if (rows.isEmpty()) {
            showMessage("No activity yet",
                "Bookings, scans, status updates and deliveries will "
                + "appear here as they happen.");
            return;
        }

        for (Row r : rows) {
            list.add(new RowPanel(r));
        }

        list.revalidate();
        list.repaint();

        ((CardLayout) body.getLayout()).show(body, "list");
    }

    private void showMessage(String title, String hint) {

        emptyHolder.removeAll();
        emptyHolder.add(new Empty(dark, title, hint, null, null),
                BorderLayout.CENTER);
        emptyHolder.revalidate();
        emptyHolder.repaint();

        ((CardLayout) body.getLayout()).show(body, "empty");
    }

    /** One event: coloured timeline dot + text + "5 min ago". */
    private class RowPanel extends JPanel {

        private final Color accent;

        RowPanel(Row r) {

            accent = Colors.of(r.status);

            setOpaque(false);
            setLayout(new BorderLayout(10, 0));
            setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 6));

            Color main = dark ? Theme.TEXT_LIGHT : Theme.PLUM;
            Color dim = dark ? Theme.TEXT_DIM : Theme.MUTED;

            JLabel line1 = new JLabel(
                (r.tracking == null ? "-" : r.tracking) + "  ·  "
                + Colors.norm(r.status));
            line1.setFont(Theme.font(Font.BOLD, 13));
            line1.setForeground(main);

            StringBuilder sb = new StringBuilder();

            if (r.location != null && !r.location.trim().isEmpty()) {
                sb.append(r.location.trim());
            }
            if (r.by != null && !r.by.trim().isEmpty()) {
                if (sb.length() > 0) sb.append("  ·  ");
                sb.append(r.by.trim());
            }

            JLabel line2 = new JLabel(sb.length() == 0 ? " " : sb.toString());
            line2.setFont(Theme.font(Font.PLAIN, 12));
            line2.setForeground(dim);

            if (r.remarks != null && !r.remarks.trim().isEmpty()) {
                line2.setToolTipText(r.remarks);
            }

            JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
            text.setOpaque(false);
            text.add(line1);
            text.add(line2);

            JLabel when = new JLabel(Theme.ago(r.time));
            when.setFont(Theme.font(Font.PLAIN, 11));
            when.setForeground(dim);
            when.setVerticalAlignment(SwingConstants.TOP);

            JComponent dot = new JComponent() {
                @Override protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    Theme.smooth(g);
                    g.setColor(Theme.alpha(accent, 70));
                    g.fillOval(0, 4, 14, 14);
                    g.setColor(accent);
                    g.fillOval(3, 7, 8, 8);
                    g.dispose();
                }
            };
            dot.setPreferredSize(new Dimension(16, 22));

            JPanel dotHolder = new JPanel(new BorderLayout());
            dotHolder.setOpaque(false);
            dotHolder.add(dot, BorderLayout.NORTH);

            add(dotHolder, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
            add(when, BorderLayout.EAST);
        }

        @Override
        protected void paintComponent(Graphics g0) {

            Graphics2D g = (Graphics2D) g0.create();

            g.setColor(dark ? Theme.alpha(Color.WHITE, 18)
                            : new Color(0, 0, 0, 18));
            g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

            g.dispose();
            super.paintComponent(g0);
        }
    }
}