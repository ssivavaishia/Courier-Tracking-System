import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.List;

/**
 * My Shipments: every shipment of the customer in a searchable, filterable
 * table with status pills and a Cancel Order button (allowed while the
 * shipment is still BOOKED or PENDING).
 */
public class Orders extends Grid {

    private final int customerId;

    public Orders(int customerId, String customerName) {

        super("My Shipments", "Search, filter and cancel your orders.",
              new String[]{"ID", "Tracking No.", "Receiver", "Destination", "Booked", "Status"}, 5);

        this.customerId = customerId;

        JButton cancel = new JButton("Cancel Order");
        cancel.setIcon(Ico.of("alert", 16, Color.WHITE));
        cancel.setBackground(Theme.RED);
        cancel.setToolTipText("Select a shipment first. Only BOOKED or PENDING orders can be cancelled.");
        cancel.addActionListener(e -> cancelSelected());
        addAction(cancel);
    }

    @Override
    protected List<Object[]> fetch() throws Exception {

        List<Object[]> rows = newRows();

        String sql =
            "SELECT shipment_id, tracking_number, receiver_name, receiver_address, "
            + "booking_date, delivery_status FROM shipments "
            + "WHERE customer_id = ? ORDER BY shipment_id DESC";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String addr = rs.getString(4) == null ? "" : rs.getString(4).replace('\n', ' ');
                    rows.add(new Object[]{
                        rs.getInt(1), rs.getString(2), rs.getString(3),
                        addr.length() > 42 ? addr.substring(0, 41) + "..." : addr,
                        date(rs.getTimestamp(5)), rs.getString(6)});
                }
            }
        }

        return rows;
    }

    private void cancelSelected() {

        Object[] r = selected();

        if (r == null) {
            JOptionPane.showMessageDialog(this, "Please select the shipment you want to cancel.");
            return;
        }

        String status = String.valueOf(r[5]).trim().toUpperCase();

        if (!status.equals("BOOKED") && !status.equals("PENDING")) {
            JOptionPane.showMessageDialog(this,
                status.equals("CANCELLED")
                    ? "This order is already cancelled."
                    : "This order is " + status + " and can no longer be cancelled.\nPlease contact support.",
                "Cannot Cancel", JOptionPane.WARNING_MESSAGE);
            return;
        }

        cancelOrder(this, customerId, (Integer) r[0], String.valueOf(r[1]));

        reload();
    }

    /**
     * Cancels one order for a customer (only while BOOKED or PENDING).
     * Asks for confirmation, updates the shipment and writes a tracking log in
     * one transaction. Returns true when the order was cancelled.
     */
    public static boolean cancelOrder(Component parent, int customerId,
                                      int shipmentId, String tracking) {

        int confirm = JOptionPane.showConfirmDialog(parent,
            "Cancel shipment " + tracking + "?\nThis cannot be undone.",
            "Confirm Cancellation", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return false;

        String sql =
            "UPDATE shipments SET delivery_status = 'CANCELLED' "
            + "WHERE shipment_id = ? AND customer_id = ? "
            + "AND UPPER(delivery_status) IN ('BOOKED', 'PENDING')";

        String logSql =
            "INSERT INTO tracking_logs (shipment_id, status, location, remarks, updated_by) "
            + "VALUES (?, 'CANCELLED', 'Customer request', "
            + "'Order cancelled by the customer', ?)";

        try (Connection con = Db.getConnection()) {

            con.setAutoCommit(false);

            boolean cancelled;

            try {

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, shipmentId);
                    ps.setInt(2, customerId);
                    cancelled = ps.executeUpdate() > 0;
                }

                if (cancelled) {
                    try (PreparedStatement ps = con.prepareStatement(logSql)) {
                        ps.setInt(1, shipmentId);
                        ps.setInt(2, customerId);
                        ps.executeUpdate();
                    }
                }

                con.commit();

            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }

            JOptionPane.showMessageDialog(parent, cancelled
                ? "Shipment " + tracking + " has been cancelled."
                : "This order can no longer be cancelled because its status just changed.");

            return cancelled;

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(parent,
                "Unable to cancel order: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            return false;
        }
    }
}
