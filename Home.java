import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Dashboard home for customers and delivery partners (same layout, the SQL
 * and wording follow the role):
 *
 *   6 statistic cards  |  order trend graph  |  status donut
 *   live street map    |  calendar           |  recent + active shipment tables
 *   quick action tiles |  date-range filter  |  cancel button for customers
 */
public class Home extends JPanel implements Native {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static class Data {
        int total, delivered, transit, pending, cancelled;
        double[] orders, delivs;
        String[] labels;
        List<String[]> recent = new ArrayList<>();
        List<String[]> active = new ArrayList<>();
    }

    /** Scroll area that stretches to fill the window when there is room. */
    private static class Page extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport
                && ((JViewport) getParent()).getHeight() > getPreferredSize().height;
        }
    }

    private final boolean customer;
    private final int id;
    private final Consumer<String> nav;
    private final String col;

    private final RangePick range = new RangePick();
    private final Kit.Stat sTotal, sDelivered, sTransit, sPending, sCancelled;
    private final Ring ring = new Ring(80);
    private final JLabel ringSub = new JLabel(" ");
    private final Line line = new Line(true);
    private final Donut donut = new Donut(true);
    private final Cal cal = new Cal();
    private final MiniTable recent;
    private final MiniTable active;
    private final Timer refresh;

    public Home(String role, int id, String name, Consumer<String> nav) {

        this.customer = "CUSTOMER".equalsIgnoreCase(role);
        this.id = id;
        this.nav = nav;
        this.col = customer ? "customer_id" : "delivery_person_id";

        setOpaque(false);
        setLayout(new BorderLayout());

        sTotal = new Kit.Stat(customer ? "Total Orders<br>Placed" : "Total<br>Assigned", "boxes",
                Colors.BOOKED, () -> nav.accept(customer ? "My Shipments" : "Assigned Shipments"));
        sDelivered = new Kit.Stat("Delivered", "check", Colors.DELIVERED, null);
        sTransit = new Kit.Stat("In Transit", "truck", Colors.IN_TRANSIT, null);
        sPending = new Kit.Stat("Pending", "clock", Colors.PENDING, null);
        sCancelled = new Kit.Stat("Cancelled", "alert", Colors.CANCELLED, null);

        recent = new MiniTable(new String[]{"Tracking No.", "Date", "Status"}, 2,
                this::recentAction, "No shipments in this period.");
        active = new MiniTable(new String[]{"Tracking No.", "Destination", "Pickup Date", "Status"}, 3,
                customer ? this::activeAction : null, "No active shipments right now.");

        Page page = new Page();
        page.setOpaque(false);
        page.setLayout(new GridBagLayout());
        page.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 2));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.gridx = 0; g.gridy = 0; g.weightx = 1; g.weighty = 0;
        g.insets = new Insets(0, 0, 12, 0);

        // ---- row 0: range
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel hint = new JLabel(customer ? "Your shipments in the selected period"
                                          : "Your deliveries in the selected period");
        hint.setFont(Theme.font(Font.PLAIN, 13));
        hint.setForeground(Theme.DIM);
        top.add(hint, BorderLayout.WEST);
        top.add(range, BorderLayout.EAST);
        page.add(top, g);

        // ---- row 1: stat cards
        JPanel stats = new JPanel(new GridLayout(1, 6, 14, 0));
        stats.setOpaque(false);
        stats.add(sTotal);
        stats.add(sDelivered);
        stats.add(sTransit);
        stats.add(sPending);
        stats.add(sCancelled);
        stats.add(Kit.ringStat("Success<br>Rate", ring, ringSub));
        g.gridy = 1;
        page.add(stats, g);

        // ---- row 2: charts, map, calendar
        JPanel charts = new JPanel(new GridBagLayout());
        charts.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;

        c.gridx = 0; c.weightx = 1.25; c.insets = new Insets(0, 0, 0, 14);
        charts.add(card("Order Trend", "chart", Colors.BOOKED, line, null), c);

        c.gridx = 1; c.weightx = 1.0;
        charts.add(card("Shipment Status", "boxes", Colors.BOOKED, donut, null), c);

        MapView map = new MapView(customer ? "CUSTOMER" : "COURIER", id);
        map.setShowLegend(false);

        c.gridx = 2; c.weightx = 1.3;
        charts.add(card(customer ? "Live Tracking Map" : "My Route Map", "pin", Colors.BOOKED, map, null), c);

        c.gridx = 3; c.weightx = 0.85; c.insets = new Insets(0, 0, 0, 0);
        charts.add(card("Calendar", "clock", Colors.BOOKED, cal, null), c);

        g.gridy = 2; g.weighty = 0.55;
        page.add(charts, g);

        // ---- row 3: tables and quick actions
        JPanel tables = new JPanel(new GridBagLayout());
        tables.setOpaque(false);

        c.gridx = 0; c.weightx = 1.3; c.insets = new Insets(0, 0, 0, 14);
        tables.add(card(customer ? "Recent Shipments" : "Recent Assignments", "boxes",
                Colors.IN_TRANSIT, recent, viewAll(customer ? "My Shipments" : "Assigned Shipments")), c);

        c.gridx = 1; c.weightx = 1.25;
        tables.add(card("Active Shipments", "truck", Colors.BOOKED, active,
                viewAll(customer ? "Track Shipment" : "Assigned Shipments")), c);

        c.gridx = 2; c.weightx = 0.85; c.insets = new Insets(0, 0, 0, 0);
        tables.add(card("Quick Actions", "bolt", Colors.IN_TRANSIT, quick(), null), c);

        g.gridy = 3; g.weighty = 0.45; g.insets = new Insets(0, 0, 0, 0);
        page.add(tables, g);

        JScrollPane scroll = new JScrollPane(page,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(20);

        add(scroll, BorderLayout.CENTER);

        range.onChange((a, b) -> load());
        cal.onPick(d -> range.set(d, d));

        refresh = new Timer(30_000, e -> load());
    }

    @Override
    public void addNotify() {
        super.addNotify();
        load();
        refresh.start();
    }

    @Override
    public void removeNotify() {
        refresh.stop();
        super.removeNotify();
    }

    // ------------------------------------------------------------- building

    private JComponent card(String title, String icon, Color iconColor,
                            JComponent body, JComponent right) {

        Surface s = new Surface(18);
        s.setLayout(new BorderLayout(0, 8));
        s.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        s.setPreferredSize(new Dimension(200, 250));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Kit.title(title, icon, iconColor), BorderLayout.WEST);
        if (right != null) head.add(right, BorderLayout.EAST);

        s.add(head, BorderLayout.NORTH);
        s.add(body, BorderLayout.CENTER);

        return s;
    }

    private JComponent viewAll(String target) {

        JButton b = new JButton("View All  >");
        b.setFont(Theme.font(Font.PLAIN, 12));
        b.setForeground(Theme.TXT);
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
        b.setFocusPainted(false);
        b.addActionListener(e -> nav.accept(target));

        return b;
    }

    private JComponent quick() {

        JPanel p = new JPanel(new GridLayout(2, 2, 12, 12));
        p.setOpaque(false);

        Color red1 = new Color(222, 38, 52), red2 = new Color(120, 14, 26);
        Color blue1 = new Color(36, 72, 170), blue2 = new Color(18, 32, 84);
        Color pur1 = new Color(112, 48, 210), pur2 = new Color(52, 22, 110);

        if (customer) {
            p.add(Kit.tile("Book\nShipment", "boxes", red1, red2, () -> nav.accept("Book Shipment")));
            p.add(Kit.tile("Track\nShipment", "pin", blue1, blue2, () -> nav.accept("Track Shipment")));
            p.add(Kit.tile("Contact\nSupport", "help", pur1, pur2, () -> nav.accept("Contact Support")));
            p.add(Kit.tile("My\nProfile", "user", blue1, blue2, () -> nav.accept("My Profile")));
        } else {
            p.add(Kit.tile("Assigned\nShipments", "boxes", red1, red2, () -> nav.accept("Assigned Shipments")));
            p.add(Kit.tile("Update\nStatus", "edit", blue1, blue2, () -> nav.accept("Update Delivery Status")));
            p.add(Kit.tile("Confirm\nDelivery", "check", pur1, pur2, () -> nav.accept("Confirm Delivery")));
            p.add(Kit.tile("Messages", "chat", blue1, blue2, () -> nav.accept("Chat")));
        }

        return p;
    }

    /** Action cell of the Recent table: Track (and Cancel for customers). */
    private JComponent recentAction(String[] row) {

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);

        JButton go = Kit.ghost(customer ? "Track" : "Update", new Color(140, 110, 255));
        go.addActionListener(e -> nav.accept(customer ? "Track Shipment" : "Update Delivery Status"));
        p.add(go);

        if (customer && row.length > 4 && "1".equals(row[4])) {

            JButton cancel = Kit.ghost("Cancel", Colors.CANCELLED);
            cancel.setToolTipText("Cancel this order");
            cancel.addActionListener(e -> {
                if (Orders.cancelOrder(this, id, Integer.parseInt(row[3]), row[0])) load();
            });
            p.add(cancel);
        }

        return p;
    }

    /** Action cell of the Active table: Cancel while the order is still BOOKED / PENDING. */
    private JComponent activeAction(String[] row) {

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);

        if (row.length > 5 && "1".equals(row[5])) {

            JButton cancel = Kit.ghost("Cancel order", Colors.CANCELLED);
            cancel.setToolTipText("Cancel this order");
            cancel.addActionListener(e -> {
                if (Orders.cancelOrder(this, id, Integer.parseInt(row[4]), row[0])) load();
            });
            p.add(cancel);

        } else {

            JLabel none = new JLabel("-");
            none.setForeground(Theme.DIM);
            p.add(none);
        }

        return p;
    }

    // ----------------------------------------------------------------- data

    private void load() {

        final LocalDate from = range.from(), to = range.to();

        new SwingWorker<Data, Void>() {

            @Override
            protected Data doInBackground() throws Exception {

                Data d = new Data();

                LocalDate f = from;
                long days = ChronoUnit.DAYS.between(f, to) + 1;

                if (days > 62) { f = to.minusDays(61); days = 62; }

                int bucket = days > 31 ? 7 : 1;
                int n = Math.max(2, (int) Math.ceil(days / (double) bucket));

                d.orders = new double[n];
                d.delivs = new double[n];
                d.labels = new String[n];

                for (int i = 0; i < n; i++) {
                    LocalDate x = f.plusDays((long) i * bucket);
                    d.labels[i] = bucket == 1 ? x.getDayOfMonth() + "/" + x.getMonthValue()
                                              : "W" + (i + 1);
                }

                try (Connection con = Db.getConnection()) {

                    String between = "DATE(booking_date) BETWEEN ? AND ?";

                    // counts by status
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT UPPER(delivery_status), COUNT(*) FROM shipments WHERE "
                            + col + " = ? AND " + between + " GROUP BY UPPER(delivery_status)")) {

                        ps.setInt(1, id);
                        ps.setDate(2, java.sql.Date.valueOf(from));
                        ps.setDate(3, java.sql.Date.valueOf(to));

                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String st = rs.getString(1);
                                int k = rs.getInt(2);
                                d.total += k;
                                if (st.equals("DELIVERED")) d.delivered += k;
                                else if (st.equals("CANCELLED")) d.cancelled += k;
                                else if (st.equals("BOOKED") || st.equals("PENDING")) d.pending += k;
                                else d.transit += k;
                            }
                        }
                    }

                    // trend
                    fill(con, "SELECT DATE(booking_date), COUNT(*) FROM shipments WHERE "
                        + col + " = ? AND " + between + " GROUP BY DATE(booking_date)",
                        f, to, d.orders, bucket);

                    fill(con, "SELECT DATE(delivery_date), COUNT(*) FROM shipments WHERE "
                        + col + " = ? AND UPPER(delivery_status) = 'DELIVERED' "
                        + "AND DATE(delivery_date) BETWEEN ? AND ? GROUP BY DATE(delivery_date)",
                        f, to, d.delivs, bucket);

                    // recent
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT shipment_id, tracking_number, booking_date, delivery_status "
                            + "FROM shipments WHERE " + col + " = ? AND " + between
                            + " ORDER BY shipment_id DESC LIMIT 6")) {

                        ps.setInt(1, id);
                        ps.setDate(2, java.sql.Date.valueOf(from));
                        ps.setDate(3, java.sql.Date.valueOf(to));

                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String st = rs.getString(4);
                                Timestamp ts = rs.getTimestamp(3);
                                String u = st == null ? "" : st.trim().toUpperCase();
                                d.recent.add(new String[]{
                                    rs.getString(2),
                                    ts == null ? "" : ts.toLocalDateTime().toLocalDate().format(FMT),
                                    st, String.valueOf(rs.getInt(1)),
                                    u.equals("BOOKED") || u.equals("PENDING") ? "1" : "0"});
                            }
                        }
                    }

                    // active
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT tracking_number, receiver_address, pickup_date, delivery_status, shipment_id "
                            + "FROM shipments WHERE " + col + " = ? AND UPPER(delivery_status) "
                            + "NOT IN ('DELIVERED','CANCELLED') ORDER BY shipment_id DESC LIMIT 5")) {

                        ps.setInt(1, id);

                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                java.sql.Date pd = rs.getDate(3);
                                String st = rs.getString(4);
                                String u = st == null ? "" : st.trim().toUpperCase();
                                d.active.add(new String[]{
                                    rs.getString(1), city(rs.getString(2)),
                                    pd == null ? "-" : pd.toLocalDate().format(FMT),
                                    st, String.valueOf(rs.getInt(5)),
                                    u.equals("BOOKED") || u.equals("PENDING") ? "1" : "0"});
                            }
                        }
                    }
                }

                return d;
            }

            @Override
            protected void done() {

                try {
                    show(get());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    recent.rows(new ArrayList<String[]>());
                    active.rows(new ArrayList<String[]>());
                }
            }

        }.execute();
    }

    private void fill(Connection con, String sql, LocalDate f, LocalDate to,
                      double[] target, int bucket) throws SQLException {

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.setDate(2, java.sql.Date.valueOf(f));
            ps.setDate(3, java.sql.Date.valueOf(to));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date day = rs.getDate(1);
                    if (day == null) continue;
                    int idx = (int) (ChronoUnit.DAYS.between(f, day.toLocalDate()) / bucket);
                    if (idx >= 0 && idx < target.length) target[idx] += rs.getInt(2);
                }
            }
        }
    }

    private static String city(String address) {

        if (address == null) return "-";

        String[] p = address.replace('\n', ',').split(",");
        String last = p[p.length - 1].trim();

        if (last.isEmpty() && p.length > 1) last = p[p.length - 2].trim();

        return last.length() > 18 ? last.substring(0, 17) + "..." : last;
    }

    private void show(Data d) {

        sTotal.setValue(String.valueOf(d.total));
        sDelivered.setValue(String.valueOf(d.delivered));
        sTransit.setValue(String.valueOf(d.transit));
        sPending.setValue(String.valueOf(d.pending));
        sCancelled.setValue(String.valueOf(d.cancelled));

        double rate = d.total == 0 ? 0 : (double) d.delivered / d.total;
        ring.setColor(Colors.DELIVERED);
        ring.setProgress(rate, true);
        ringSub.setText(d.delivered + " of " + d.total + " delivered");

        line.setData(d.labels, new String[]{"Orders", "Delivered"},
                new Color[]{new Color(255, 70, 80), Theme.BLUE}, d.orders, d.delivs);

        donut.setData("Total",
                new String[]{"Delivered", "In Transit", "Pending", "Cancelled"},
                new double[]{d.delivered, d.transit, d.pending, d.cancelled},
                new Color[]{Colors.DELIVERED, Colors.IN_TRANSIT, Colors.PENDING, Colors.CANCELLED});

        recent.rows(d.recent);
        active.rows(d.active);
    }
}