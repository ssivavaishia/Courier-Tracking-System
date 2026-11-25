import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performance Insights: delivery completion and workload distribution.
 * Deliberately informative, not competitive: couriers are listed
 * alphabetically, there is no ranking, and every bar uses the same colour.
 */
public class Insights extends JPanel {

    private static class Data {
        int delivered, active, cancelled;
        Map<String, Integer> byStatus = new HashMap<>();
        List<String> names = new ArrayList<>();
        List<Integer> activeLoad = new ArrayList<>();
        List<Integer> doneMonth = new ArrayList<>();
        int[] days = new int[7];
        String[] dayLabels = new String[7];
    }

    private final JPanel body = new JPanel(new CardLayout());
    private final JPanel content = new JPanel(new GridBagLayout());
    private final JPanel emptyHolder = new JPanel(new BorderLayout());

    public Insights() {

        setBackground(Theme.PAPER);
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel("Performance Insights");
        title.setFont(Theme.font(Font.BOLD, 26));
        title.setForeground(Theme.PLUM);

        JLabel sub = new JLabel(
            "Delivery completion and workload, for planning. "
            + "Figures cover the last 30 days; couriers are listed "
            + "alphabetically, not ranked.");
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
        refresh.addActionListener(e -> load());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titles, BorderLayout.WEST);
        top.add(refresh, BorderLayout.EAST);

        content.setOpaque(false);
        emptyHolder.setOpaque(false);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        body.setOpaque(false);
        body.add(scroll, "data");
        body.add(emptyHolder, "empty");

        add(top, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        load();
    }

    private void load() {

        new SwingWorker<Data, Void>() {

            @Override
            protected Data doInBackground() throws Exception {

                Data d = new Data();

                try (Connection con = Db.getConnection()) {

                    String s1 =
                        "SELECT UPPER(delivery_status), COUNT(*) "
                        + "FROM shipments WHERE booking_date >= "
                        + "DATE_SUB(CURDATE(), INTERVAL 30 DAY) "
                        + "GROUP BY UPPER(delivery_status)";

                    try (PreparedStatement ps = con.prepareStatement(s1);
                         ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {
                            String st = rs.getString(1);
                            int n = rs.getInt(2);
                            d.byStatus.put(st, n);
                            if ("DELIVERED".equals(st)) d.delivered += n;
                            else if ("CANCELLED".equals(st)) d.cancelled += n;
                            else d.active += n;
                        }
                    }

                    String s2 =
                        "SELECT u.full_name, "
                        + "COALESCE(SUM(UPPER(s.delivery_status) NOT IN "
                        + "  ('DELIVERED','CANCELLED')), 0), "
                        + "COALESCE(SUM(UPPER(s.delivery_status) = 'DELIVERED' "
                        + "  AND s.delivery_date >= "
                        + "  DATE_SUB(CURDATE(), INTERVAL 30 DAY)), 0) "
                        + "FROM users u LEFT JOIN shipments s "
                        + "ON s.delivery_person_id = u.user_id "
                        + "WHERE UPPER(u.role) = 'DELIVERY' "
                        + "AND u.account_status = 'ACTIVE' "
                        + "GROUP BY u.user_id, u.full_name "
                        + "ORDER BY u.full_name";

                    try (PreparedStatement ps = con.prepareStatement(s2);
                         ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {
                            d.names.add(rs.getString(1));
                            d.activeLoad.add(rs.getInt(2));
                            d.doneMonth.add(rs.getInt(3));
                        }
                    }

                    LocalDate today = LocalDate.now();
                    Map<LocalDate, Integer> perDay = new HashMap<>();

                    String s3 =
                        "SELECT DATE(delivery_date), COUNT(*) FROM shipments "
                        + "WHERE UPPER(delivery_status) = 'DELIVERED' "
                        + "AND delivery_date >= "
                        + "DATE_SUB(CURDATE(), INTERVAL 6 DAY) "
                        + "GROUP BY DATE(delivery_date)";

                    try (PreparedStatement ps = con.prepareStatement(s3);
                         ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {
                            java.sql.Date day = rs.getDate(1);
                            if (day != null) {
                                perDay.put(day.toLocalDate(), rs.getInt(2));
                            }
                        }
                    }

                    for (int i = 0; i < 7; i++) {
                        LocalDate day = today.minusDays(6 - i);
                        d.days[i] = perDay.getOrDefault(day, 0);
                        d.dayLabels[i] = day.getDayOfWeek().toString()
                                .substring(0, 3);
                    }
                }

                return d;
            }

            @Override
            protected void done() {

                try {
                    show(get());
                } catch (Exception ex) {
                    showEmpty("Could not load insights",
                        "Check the database connection and press Refresh.");
                }
            }

        }.execute();
    }

    private void showEmpty(String title, String hint) {

        emptyHolder.removeAll();
        emptyHolder.add(new Empty(true, title, hint, null, null),
                BorderLayout.CENTER);
        emptyHolder.revalidate();
        emptyHolder.repaint();

        ((CardLayout) body.getLayout()).show(body, "empty");
    }

    private void show(Data d) {

        int total = d.delivered + d.active + d.cancelled;

        if (total == 0 && d.names.isEmpty()) {
            showEmpty("Nothing to measure yet",
                "Insights appear once shipments are booked and couriers "
                + "are added.");
            return;
        }

        content.removeAll();

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 0, 14, 14);
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 0;

        // ---- completion
        int open = d.delivered + d.active;

        Ring ring = new Ring(120);
        ring.setColor(Colors.DELIVERED);

        JLabel big = new JLabel(open == 0 ? "No open orders"
                : d.delivered + " of " + open + " completed");
        big.setFont(Theme.font(Font.BOLD, 16));
        big.setForeground(Theme.PLUM);

        JLabel small = new JLabel("<html>" + d.active + " still in progress"
                + "<br>" + d.cancelled + " cancelled by customers</html>");
        small.setFont(Theme.font(Font.PLAIN, 13));
        small.setForeground(Theme.MUTED);

        JPanel txt = new JPanel(new GridLayout(2, 1, 0, 6));
        txt.setOpaque(false);
        txt.add(big);
        txt.add(small);

        JPanel completion = new JPanel(new BorderLayout(18, 0));
        completion.setOpaque(false);
        completion.add(ring, BorderLayout.WEST);
        completion.add(txt, BorderLayout.CENTER);

        JPanel stackBox = new JPanel(new BorderLayout(0, 8));
        stackBox.setOpaque(false);
        stackBox.add(completion, BorderLayout.NORTH);
        stackBox.add(new Stack(d.byStatus), BorderLayout.CENTER);

        g.gridx = 0; g.gridy = 0; g.weightx = 0.5;
        content.add(card("Completion", stackBox), g);

        // ---- last 7 days
        g.gridx = 1; g.insets = new Insets(0, 0, 14, 0);
        content.add(card("Deliveries, last 7 days",
                new Columns(d.days, d.dayLabels)), g);

        // ---- workload
        g.gridx = 0; g.gridy = 1; g.gridwidth = 2; g.weightx = 1; g.weighty = 1;
        g.insets = new Insets(0, 0, 0, 0);

        JComponent work;

        if (d.names.isEmpty()) {
            work = new Empty(true, "No delivery personnel yet",
                "Add couriers from the Delivery Personnel page.", null, null);
        } else {
            work = new Bars(d.names, d.activeLoad, d.doneMonth);
        }

        content.add(card("Workload by courier (open shipments)", work), g);

        content.revalidate();
        content.repaint();

        ((CardLayout) body.getLayout()).show(body, "data");

        ring.setProgress(open == 0 ? 0 : (double) d.delivered / open, true);
    }

    private JPanel card(String title, JComponent inner) {

        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(Theme.PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LINE),
            BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel t = new JLabel(title);
        t.setFont(Theme.font(Font.BOLD, 15));
        t.setForeground(Theme.PLUM);

        p.add(t, BorderLayout.NORTH);
        p.add(inner, BorderLayout.CENTER);

        return p;
    }

    // ------------------------------------------------------------- charts

    /** One horizontal bar split by status, with a legend. */
    private static class Stack extends JComponent {

        private final Map<String, Integer> data;
        private static final String[] ORDER = {
            "BOOKED", "PENDING", "PICKED UP", "IN TRANSIT",
            "OUT FOR DELIVERY", "DELIVERED", "CANCELLED"};

        Stack(Map<String, Integer> data) {
            this.data = data;
            setPreferredSize(new Dimension(300, 80));
        }

        @Override
        protected void paintComponent(Graphics g0) {

            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);

            int total = 0;
            for (int v : data.values()) total += v;

            int w = getWidth(), x = 0;

            g.setColor(new Color(255, 255, 255, 26));
            g.fillRoundRect(0, 4, w, 16, 16, 16);

            if (total > 0) {

                Shape clip = new java.awt.geom.RoundRectangle2D.Double(
                        0, 4, w, 16, 16, 16);
                g.setClip(clip);

                for (String st : ORDER) {
                    Integer n = data.get(st);
                    if (n == null || n == 0) continue;
                    int sw = (int) Math.round((double) n / total * w);
                    g.setColor(Colors.of(st));
                    g.fillRect(x, 4, sw, 16);
                    x += sw;
                }

                g.setClip(null);
            }

            // legend
            g.setFont(Theme.font(Font.PLAIN, 11));
            int lx = 0, ly = 42;

            for (String st : ORDER) {
                Integer n = data.get(st);
                if (n == null || n == 0) continue;

                String label = st.charAt(0) + st.substring(1).toLowerCase()
                        + " " + n;
                int tw = g.getFontMetrics().stringWidth(label) + 26;

                if (lx + tw > w) { lx = 0; ly += 18; }

                g.setColor(Colors.of(st));
                g.fillOval(lx, ly - 9, 9, 9);
                g.setColor(Theme.MUTED);
                g.drawString(label, lx + 14, ly);

                lx += tw;
            }

            g.dispose();
        }
    }

    /** Seven small columns: deliveries per day. */
    private static class Columns extends JComponent {

        private final int[] vals;
        private final String[] labels;

        Columns(int[] vals, String[] labels) {
            this.vals = vals;
            this.labels = labels;
            setPreferredSize(new Dimension(300, 170));
        }

        @Override
        protected void paintComponent(Graphics g0) {

            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);

            int max = 1;
            for (int v : vals) max = Math.max(max, v);

            int w = getWidth(), h = getHeight();
            int slot = w / vals.length;
            int bw = Math.min(34, slot - 12);
            int base = h - 22, top = 22;

            g.setFont(Theme.font(Font.PLAIN, 11));

            for (int i = 0; i < vals.length; i++) {

                int bh = (int) ((base - top) * (vals[i] / (double) max));
                int x = i * slot + (slot - bw) / 2;

                g.setColor(new Color(255, 255, 255, 26));
                g.fillRoundRect(x, top, bw, base - top, 10, 10);

                g.setColor(Colors.DELIVERED);
                g.fillRoundRect(x, base - bh, bw, Math.max(bh, vals[i] > 0 ? 4 : 0),
                        10, 10);

                g.setColor(Theme.MUTED);
                String n = String.valueOf(vals[i]);
                g.drawString(n, x + (bw - g.getFontMetrics().stringWidth(n)) / 2,
                        base - bh - 5 < top - 4 ? top - 4 : base - bh - 5);

                String l = labels[i];
                g.drawString(l, x + (bw - g.getFontMetrics().stringWidth(l)) / 2,
                        h - 5);
            }

            g.dispose();
        }
    }

    /** One neutral bar per courier (alphabetical, never ranked). */
    private static class Bars extends JComponent {

        private final List<String> names;
        private final List<Integer> load, done;

        Bars(List<String> names, List<Integer> load, List<Integer> done) {
            this.names = names;
            this.load = load;
            this.done = done;
            setPreferredSize(new Dimension(400, names.size() * 36 + 8));
        }

        @Override
        protected void paintComponent(Graphics g0) {

            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);

            int max = 1;
            for (int v : load) max = Math.max(max, v);

            int w = getWidth();
            int nameW = 170, textW = 190;
            int barX = nameW + 10, barW = Math.max(60, w - nameW - textW - 20);

            g.setFont(Theme.font(Font.PLAIN, 13));

            for (int i = 0; i < names.size(); i++) {

                int y = i * 36 + 4;

                g.setColor(Theme.PLUM);
                String n = names.get(i);
                if (n != null && n.length() > 22) n = n.substring(0, 21) + "…";
                g.drawString(n == null ? "" : n, 0, y + 17);

                g.setColor(new Color(0, 0, 0, 18));
                g.fillRoundRect(barX, y + 6, barW, 14, 14, 14);

                int fw = (int) (barW * (load.get(i) / (double) max));
                g.setColor(Theme.PURPLE);
                g.fillRoundRect(barX, y + 6, Math.max(fw, load.get(i) > 0 ? 8 : 0),
                        14, 14, 14);

                g.setColor(Theme.MUTED);
                g.drawString(load.get(i) + " open  ·  " + done.get(i)
                        + " delivered (30 d)", barX + barW + 12, y + 17);
            }

            g.dispose();
        }
    }
}