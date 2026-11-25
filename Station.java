import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Parcel Processing Station: scan (or type) a tracking number, pick what
 * happened to the parcel (picked up, arrived, dispatched, out for delivery,
 * delivered) and record it. Every action updates the shipment and writes a
 * tracking log, so it shows up in the Activity Feed and customer tracking.
 *
 * "Quick mode" records the chosen action as soon as a parcel is scanned,
 * for fast batch processing at a branch.
 */
public class Station extends JPanel {

    private static final String[][] ACTIONS = {
        {"Picked up from sender",   "PICKED UP"},
        {"Arrived at branch",       "IN TRANSIT"},
        {"Dispatched from branch",  "IN TRANSIT"},
        {"Out for delivery",        "OUT FOR DELIVERY"},
        {"Delivered",               "DELIVERED"}
    };

    private final int adminId;

    private final JTextField scanField = new JTextField();
    private final JComboBox<String> actionBox = new JComboBox<>();
    private final JComboBox<String> locationBox = new JComboBox<>();
    private final JTextField remarksField = new JTextField();
    private final JCheckBox quickMode = new JCheckBox(
        "Quick mode: record the chosen action as soon as a parcel is scanned");
    private final JLabel message = new JLabel(" ");
    private final JPanel cardHolder = new JPanel(new GridBagLayout());
    private final DefaultTableModel scans;

    private int currentId = -1;
    private String currentStatus = "";
    private String currentTracking = "";
    private Card currentCard = null;

    public Station(int adminId) {

        this.adminId = adminId;

        setBackground(Theme.PAPER);
        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(14, 16, 14, 16));

        // ---- heading
        JLabel title = new JLabel("Parcel Processing Station");
        title.setFont(Theme.font(Font.BOLD, 26));
        title.setForeground(Theme.PLUM);

        JLabel sub = new JLabel(
            "Record parcel arrivals, dispatches and status changes.");
        sub.setFont(Theme.font(Font.PLAIN, 13));
        sub.setForeground(Theme.MUTED);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(sub);

        // ---- form
        scanField.setFont(Theme.font(Font.BOLD, 20));
        scanField.setToolTipText("Scan a barcode or type a tracking number, then press Enter");

        JButton find = button("Find");
        find.addActionListener(e -> find());
        scanField.addActionListener(e -> find());

        JPanel scanRow = new JPanel(new BorderLayout(8, 0));
        scanRow.setOpaque(false);
        scanRow.add(scanField, BorderLayout.CENTER);
        scanRow.add(find, BorderLayout.EAST);

        for (String[] a : ACTIONS) actionBox.addItem(a[0]);
        actionBox.setFont(Theme.font(Font.PLAIN, 14));

        locationBox.setEditable(true);
        locationBox.setFont(Theme.font(Font.PLAIN, 14));
        loadBranches();

        remarksField.setFont(Theme.font(Font.PLAIN, 14));

        quickMode.setOpaque(false);
        quickMode.setFont(Theme.font(Font.PLAIN, 12));

        JButton recordBtn = button("Record");
        recordBtn.addActionListener(e -> record());

        message.setFont(Theme.font(Font.BOLD, 13));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LINE),
            new EmptyBorder(14, 16, 14, 16)));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 4, 5, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;

        int y = 0;
        y = row(form, g, y, "Tracking number", scanRow);
        y = row(form, g, y, "What happened", actionBox);
        y = row(form, g, y, "Branch / location", locationBox);
        y = row(form, g, y, "Remarks (optional)", remarksField);

        g.gridy = y++; g.gridx = 0;
        form.add(quickMode, g);

        g.gridy = y++;
        g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.EAST;
        form.add(recordBtn, g);

        g.gridy = y;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        form.add(message, g);

        // ---- parcel card area
        cardHolder.setOpaque(false);
        showWaiting();

        JPanel top = new JPanel(new GridLayout(1, 2, 16, 0));
        top.setOpaque(false);
        top.add(form);
        top.add(cardHolder);

        // ---- recent scans
        scans = new DefaultTableModel(new String[]{
            "Time", "Tracking number", "Action", "Location", "New status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(scans);
        table.setRowHeight(26);
        table.setFont(Theme.font(Font.PLAIN, 13));
        table.getTableHeader().setBackground(Theme.RED);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(Theme.font(Font.BOLD, 13));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Recorded in this session"));
        scroll.setPreferredSize(new Dimension(100, 190));

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(top, BorderLayout.CENTER);
        center.add(scroll, BorderLayout.SOUTH);

        add(titles, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        SwingUtilities.invokeLater(scanField::requestFocusInWindow);
    }

    // ---------------------------------------------------------------- UI

    private int row(JPanel p, GridBagConstraints g, int y,
                    String label, JComponent field) {

        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.PLUM);

        g.gridx = 0; g.gridy = y++;
        p.add(l, g);

        g.gridy = y++;
        p.add(field, g);

        return y;
    }

    private JButton button(String text) {

        JButton b = new JButton(text);
        b.setBackground(Theme.RED);
        b.setForeground(Color.WHITE);
        b.setFont(Theme.font(Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(10, 22, 10, 22));

        return b;
    }

    private void showWaiting() {

        cardHolder.removeAll();
        cardHolder.add(new Empty(true, "Ready to scan",
            "Scan a parcel or type its tracking number. Its card appears "
            + "here.", null, null));
        cardHolder.revalidate();
        cardHolder.repaint();

        currentCard = null;
    }

    private void say(String text, boolean ok) {
        message.setForeground(ok ? Colors.DELIVERED.darker() : Colors.CANCELLED);
        message.setText(text);
    }

    // ---------------------------------------------------------------- data

    private void loadBranches() {

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT branch_name FROM branches ORDER BY branch_name");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) locationBox.addItem(rs.getString(1));

        } catch (SQLException ex) {
            // branches are optional; the box stays editable
            ex.printStackTrace();
        }
    }

    private void find() {

        String tracking = scanField.getText().trim();

        if (tracking.isEmpty()) {
            say("Enter or scan a tracking number first.", false);
            return;
        }

        String sql =
            "SELECT shipment_id, tracking_number, receiver_name, "
            + "receiver_address, delivery_status "
            + "FROM shipments WHERE tracking_number = ?";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, tracking);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    currentId = -1;
                    showWaiting();
                    say("No shipment found for \"" + tracking + "\".", false);
                    scanField.selectAll();
                    return;
                }

                currentId = rs.getInt(1);
                currentTracking = rs.getString(2);
                currentStatus = rs.getString(5);

                currentCard = new Card(true, currentTracking, rs.getString(3),
                        rs.getString(4), currentStatus);
                currentCard.setPreferredSize(new Dimension(340, 112));

                cardHolder.removeAll();
                cardHolder.add(currentCard);
                cardHolder.revalidate();
                cardHolder.repaint();
            }

        } catch (SQLException ex) {
            say("Database error: " + ex.getMessage(), false);
            ex.printStackTrace();
            return;
        }

        say("Parcel found. Choose what happened and press Record.", true);

        if (quickMode.isSelected()) record();
    }

    private void record() {

        if (currentId < 0) {
            say("Find a parcel first.", false);
            return;
        }

        String st = Colors.norm(currentStatus);

        if (st.equals("DELIVERED") || st.equals("CANCELLED")) {
            say("This parcel is already " + st.toLowerCase()
                + " and cannot be changed.", false);
            return;
        }

        int idx = actionBox.getSelectedIndex();
        String actionLabel = ACTIONS[idx][0];
        String newStatus = ACTIONS[idx][1];

        Object loc = locationBox.getEditor().getItem();
        String location = loc == null ? "" : loc.toString().trim();
        if (location.isEmpty()) location = "Parcel station";

        String extra = remarksField.getText().trim();
        String remarks = actionLabel + (extra.isEmpty() ? "" : " - " + extra);

        String updateSql = newStatus.equals("DELIVERED")
            ? "UPDATE shipments SET delivery_status = ?, delivery_date = NOW() "
              + "WHERE shipment_id = ? AND UPPER(delivery_status) "
              + "NOT IN ('DELIVERED','CANCELLED')"
            : "UPDATE shipments SET delivery_status = ? "
              + "WHERE shipment_id = ? AND UPPER(delivery_status) "
              + "NOT IN ('DELIVERED','CANCELLED')";

        String logSql =
            "INSERT INTO tracking_logs "
            + "(shipment_id, status, location, remarks, updated_by) "
            + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = Db.getConnection()) {

            con.setAutoCommit(false);

            try {

                try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, currentId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                            "The parcel was changed by someone else. "
                            + "Scan it again.");
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(logSql)) {
                    ps.setInt(1, currentId);
                    ps.setString(2, newStatus);
                    ps.setString(3, location);
                    ps.setString(4, remarks);

                    if (adminId > 0) ps.setInt(5, adminId);
                    else ps.setNull(5, Types.INTEGER);

                    ps.executeUpdate();
                }

                con.commit();

            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException ex) {
            say(ex.getMessage(), false);
            ex.printStackTrace();
            return;
        }

        currentStatus = newStatus;

        if (currentCard != null) currentCard.setStatus(newStatus);

        scans.insertRow(0, new Object[]{
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            currentTracking, actionLabel, location, newStatus});

        say("Recorded: " + currentTracking + " - " + actionLabel + ".", true);

        if (newStatus.equals("DELIVERED")) Party.burst(this);

        scanField.setText("");
        remarksField.setText("");
        scanField.requestFocusInWindow();
    }
}