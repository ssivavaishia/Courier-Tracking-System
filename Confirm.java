import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

public class Confirm extends JPanel {

    private int deliveryPersonId;
    private JTextField trackingField;
    private JTextField locationField;
    private JTextField remarksField;

    public Confirm(int deliveryPersonId) {

        this.deliveryPersonId = deliveryPersonId;

        setLayout(new BorderLayout());
        setBackground(new Color(245, 238, 248));
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel("Confirm Delivery");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(new Color(75, 35, 85));

        JPanel form = new JPanel(new GridLayout(4, 2, 15, 20));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(30, 30, 30, 30));

        trackingField = new JTextField();
        locationField = new JTextField();
        remarksField = new JTextField();

        form.add(new JLabel("Tracking Number:"));
        form.add(trackingField);

        form.add(new JLabel("Delivery Location:"));
        form.add(locationField);

        form.add(new JLabel("Delivery Remarks:"));
        form.add(remarksField);

        JButton confirmButton = new JButton("Confirm Delivery");
        JButton resetButton = new JButton("Reset");

        styleButton(confirmButton);
        styleButton(resetButton);

        form.add(confirmButton);
        form.add(resetButton);

        add(title, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);

        confirmButton.addActionListener(e -> confirmDelivery());

        resetButton.addActionListener(e -> {
            trackingField.setText("");
            locationField.setText("");
            remarksField.setText("");
        });
    }

    private void confirmDelivery() {

        String tracking = trackingField.getText().trim();
        String location = locationField.getText().trim();
        String remarks = remarksField.getText().trim();

        if (tracking.isEmpty() || location.isEmpty() || remarks.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Please fill in all fields."
            );
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure this shipment has been delivered?",
            "Confirm Delivery",
            JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        String checkSql =
            "SELECT shipment_id, delivery_status FROM shipments " +
            "WHERE tracking_number = ? AND delivery_person_id = ?";

        String updateSql =
            "UPDATE shipments SET delivery_status = 'DELIVERED', " +
            "delivery_date = CURRENT_TIMESTAMP " +
            "WHERE shipment_id = ? AND delivery_person_id = ?";

        String logSql =
            "INSERT INTO tracking_logs " +
            "(shipment_id, status, location, remarks, updated_by) " +
            "VALUES (?, 'DELIVERED', ?, ?, ?)";

        try (Connection con = Db.getConnection()) {

            con.setAutoCommit(false);

            try {

                int shipmentId;

                try (PreparedStatement check = con.prepareStatement(checkSql)) {

                    check.setString(1, tracking);
                    check.setInt(2, deliveryPersonId);

                    try (ResultSet rs = check.executeQuery()) {

                        if (!rs.next()) {
                            throw new SQLException(
                                "Shipment not found or not assigned to you."
                            );
                        }

                        shipmentId = rs.getInt("shipment_id");

                        if ("DELIVERED".equalsIgnoreCase(
                                rs.getString("delivery_status"))) {
                            throw new SQLException(
                                "This shipment is already delivered."
                            );
                        }

                        if ("CANCELLED".equalsIgnoreCase(
                                rs.getString("delivery_status"))) {
                            throw new SQLException(
                                "Cancelled shipments cannot be delivered."
                            );
                        }
                    }
                }

                try (PreparedStatement update = con.prepareStatement(updateSql)) {
                    update.setInt(1, shipmentId);
                    update.setInt(2, deliveryPersonId);

                    if (update.executeUpdate() != 1) {
                        throw new SQLException("Delivery confirmation failed.");
                    }
                }

                try (PreparedStatement log = con.prepareStatement(logSql)) {
                    log.setInt(1, shipmentId);
                    log.setString(2, location);
                    log.setString(3, remarks);
                    log.setInt(4, deliveryPersonId);
                    log.executeUpdate();
                }

                con.commit();

                Party.burst(this);

                JOptionPane.showMessageDialog(
                    this,
                    "Delivery confirmed successfully!"
                );

                trackingField.setText("");
                locationField.setText("");
                remarksField.setText("");

            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Error confirming delivery:\n" + ex.getMessage()
            );
        }
    }

    private void styleButton(JButton button) {
        button.setBackground(new Color(100, 45, 110));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
    }
}