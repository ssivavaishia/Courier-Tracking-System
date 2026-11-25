import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Shipments extends JPanel {

    private final Color plum = new Color(75, 35, 75);
    private final Color deepPlum = new Color(48, 20, 55);
    private final Color lightPurple = new Color(235, 220, 240);
    private final Color cream = new Color(250, 245, 235);

    private JTable shipmentTable;
    private DefaultTableModel model;
    private JTextField searchField;
    private JComboBox<String> statusBox;

    public Shipments() {

        setLayout(new BorderLayout(15, 15));
        setBackground(cream);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        // HEADER
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Manage Shipments");
        title.setFont(new Font("Arial", Font.BOLD, 27));
        title.setForeground(plum);

        JLabel subtitle = new JLabel(
            "View, search and manage all courier shipments"
        );
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitle.setForeground(deepPlum);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(title);
        heading.add(Box.createVerticalStrut(8));
        heading.add(subtitle);

        header.add(heading, BorderLayout.WEST);

        // SEARCH AND FILTER
        JPanel filterPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 12, 12)
        );
        filterPanel.setBackground(lightPurple);

        searchField = new JTextField(18);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));

        statusBox = new JComboBox<>();
        statusBox.addItem("All Statuses");

        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh");

        styleButton(searchButton);
        styleButton(refreshButton);

        searchButton.addActionListener(e -> loadShipments());
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            statusBox.setSelectedIndex(0);
            loadStatuses();
            loadShipments();
        });

        filterPanel.add(new JLabel("Tracking Number:"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("Delivery Status:"));
        filterPanel.add(statusBox);
        filterPanel.add(searchButton);
        filterPanel.add(refreshButton);

        // TABLE
        String[] columns = {
            "Shipment ID",
            "Tracking Number",
            "Customer",
            "Receiver",
            "Delivery Status",
            "Booking Date"
        };

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        shipmentTable = new JTable(model);
        shipmentTable.setRowHeight(32);
        shipmentTable.setFont(new Font("Arial", Font.PLAIN, 13));
        shipmentTable.setSelectionBackground(lightPurple);
        shipmentTable.setSelectionForeground(deepPlum);
        shipmentTable.setAutoCreateRowSorter(true);

        shipmentTable.getTableHeader().setBackground(plum);
        shipmentTable.getTableHeader().setForeground(Color.WHITE);
        shipmentTable.getTableHeader().setFont(
            new Font("Arial", Font.BOLD, 13)
        );

        JScrollPane scrollPane = new JScrollPane(shipmentTable);
        scrollPane.setBorder(
            BorderFactory.createLineBorder(lightPurple, 2)
        );

        // ACTION BUTTONS
        JPanel actionPanel = new JPanel(
            new FlowLayout(FlowLayout.CENTER, 18, 12)
        );
        actionPanel.setBackground(cream);

        JButton viewButton = new JButton("View Details");
        JButton cancelButton = new JButton("Cancel Shipment");

        styleButton(viewButton);
        styleButton(cancelButton);

        viewButton.addActionListener(e -> viewDetails());
        cancelButton.addActionListener(e -> cancelShipment());

        actionPanel.add(viewButton);
        actionPanel.add(cancelButton);

        // MAIN LAYOUT
        JPanel topPanel = new JPanel(new BorderLayout(10, 15));
        topPanel.setOpaque(false);
        topPanel.add(header, BorderLayout.NORTH);
        topPanel.add(filterPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);

        loadStatuses();
        loadShipments();
    }

    private void styleButton(JButton button) {

        button.setBackground(plum);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(new EmptyBorder(11, 18, 11, 18));
        button.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
    }

    // LOAD AVAILABLE DELIVERY STATUSES
    private void loadStatuses() {

        String selected = statusBox.getSelectedItem() == null
            ? "All Statuses"
            : statusBox.getSelectedItem().toString();

        statusBox.removeAllItems();
        statusBox.addItem("All Statuses");

        String sql =
            "SELECT DISTINCT delivery_status FROM shipments " +
            "WHERE delivery_status IS NOT NULL ORDER BY delivery_status";

        try (
            Connection con = Db.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                statusBox.addItem(rs.getString("delivery_status"));
            }

            statusBox.setSelectedItem(selected);

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Could not load statuses:\n" + ex.getMessage()
            );
        }
    }

    // LOAD SHIPMENTS
    private void loadShipments() {

        model.setRowCount(0);

        String search = searchField.getText().trim();
        String status = (String) statusBox.getSelectedItem();

        StringBuilder sql = new StringBuilder(
            "SELECT s.shipment_id, s.tracking_number, " +
            "u.full_name AS customer_name, s.receiver_name, " +
            "s.delivery_status, s.booking_date " +
            "FROM shipments s " +
            "LEFT JOIN users u ON s.customer_id = u.user_id " +
            "WHERE s.tracking_number LIKE ? "
        );

        if (status != null && !status.equals("All Statuses")) {
            sql.append("AND s.delivery_status = ? ");
        }

        sql.append("ORDER BY s.shipment_id DESC");

        try (
            Connection con = Db.getConnection();
            PreparedStatement ps = con.prepareStatement(sql.toString())
        ) {

            ps.setString(1, "%" + search + "%");

            if (status != null && !status.equals("All Statuses")) {
                ps.setString(2, status);
            }

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    model.addRow(new Object[] {
                        rs.getInt("shipment_id"),
                        rs.getString("tracking_number"),
                        rs.getString("customer_name"),
                        rs.getString("receiver_name"),
                        rs.getString("delivery_status"),
                        rs.getTimestamp("booking_date")
                    });
                }
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Could not load shipments:\n" + ex.getMessage()
            );
        }
    }

    // VIEW FULL SHIPMENT DETAILS
    private void viewDetails() {

        int selectedRow = shipmentTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                this, "Please select a shipment."
            );
            return;
        }

        int row = shipmentTable.convertRowIndexToModel(selectedRow);
        int shipmentId = (int) model.getValueAt(row, 0);

        String sql = "SELECT * FROM shipments WHERE shipment_id = ?";

        try (
            Connection con = Db.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, shipmentId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    String details =
                        "Shipment ID: " + rs.getInt("shipment_id") +
                        "\nTracking Number: " + rs.getString("tracking_number") +
                        "\nCustomer ID: " + rs.getInt("customer_id") +
                        "\nDelivery Person ID: " + rs.getObject("delivery_person_id") +
                        "\nSender Address: " + rs.getString("sender_address") +
                        "\nReceiver Name: " + rs.getString("receiver_name") +
                        "\nReceiver Phone: " + rs.getString("receiver_phone") +
                        "\nReceiver Address: " + rs.getString("receiver_address") +
                        "\nParcel Description: " + rs.getString("parcel_description") +
                        "\nBooking Date: " + rs.getTimestamp("booking_date") +
                        "\nPickup Date: " + rs.getDate("pickup_date") +
                        "\nDelivery Status: " + rs.getString("delivery_status") +
                        "\nDelivery Date: " + rs.getTimestamp("delivery_date");

                    JTextArea area = new JTextArea(details, 15, 42);
                    area.setEditable(false);
                    area.setFont(new Font("Arial", Font.PLAIN, 14));
                    area.setLineWrap(true);
                    area.setWrapStyleWord(true);

                    JOptionPane.showMessageDialog(
                        this,
                        new JScrollPane(area),
                        "Shipment Details",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Could not view shipment:\n" + ex.getMessage()
            );
        }
    }

    // CANCEL SHIPMENT WITHOUT DELETING ITS RECORD
    private void cancelShipment() {

        int selectedRow = shipmentTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                this, "Please select a shipment first."
            );
            return;
        }

        int row = shipmentTable.convertRowIndexToModel(selectedRow);
        int shipmentId = (int) model.getValueAt(row, 0);

        String currentStatus =
            String.valueOf(model.getValueAt(row, 4));

        if (currentStatus.equalsIgnoreCase("DELIVERED") ||
            currentStatus.equalsIgnoreCase("CANCELLED")) {

            JOptionPane.showMessageDialog(
                this,
                "This shipment cannot be cancelled."
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to cancel this shipment?",
            "Confirm Cancellation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String sql =
            "UPDATE shipments SET delivery_status = 'CANCELLED' " +
            "WHERE shipment_id = ? AND UPPER(delivery_status) " +
            "NOT IN ('DELIVERED', 'CANCELLED')";

        try (
            Connection con = Db.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, shipmentId);

            if (ps.executeUpdate() > 0) {

                JOptionPane.showMessageDialog(
                    this,
                    "Shipment cancelled successfully."
                );

                loadStatuses();
                loadShipments();

            } else {

                JOptionPane.showMessageDialog(
                    this,
                    "Shipment status changed. Please refresh."
                );
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Cancellation failed:\n" + ex.getMessage()
            );
        }
    }
}
