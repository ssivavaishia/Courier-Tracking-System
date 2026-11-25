
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

public class Status extends JPanel {

    private int adminId;

    private JTextField trackingField;
    private JTextField locationField;
    private JTextArea remarksArea;
    private JComboBox<String> statusBox;
    private JLabel currentStatusLabel;
    private JLabel shipmentIdLabel;

    private int selectedShipmentId = -1;

    private final Color plum = new Color(75, 35, 75);
    private final Color lightPurple = new Color(235, 220, 240);

    public Status(int adminId) {

        this.adminId = adminId;

        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 238, 248));
        setBorder(new EmptyBorder(25, 30, 25, 30));

        JLabel title = new JLabel("UPDATE SHIPMENT STATUS");
        title.setFont(new Font("Arial", Font.BOLD, 25));
        title.setForeground(plum);

        // SEARCH SECTION
        trackingField = new JTextField(22);

        JButton searchButton = new JButton("Search Shipment");
        styleButton(searchButton);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("Tracking Number:"));
        searchPanel.add(trackingField);
        searchPanel.add(searchButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(title, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        // UPDATE FORM
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 10, 12, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        shipmentIdLabel = new JLabel("Not selected");
        currentStatusLabel = new JLabel("Not selected");

        statusBox = new JComboBox<>(new String[]{
            "BOOKED",
            "PICKED UP",
            "IN TRANSIT",
            "OUT FOR DELIVERY",
            "DELIVERED",
            "CANCELLED"
        });

        locationField = new JTextField(25);

        remarksArea = new JTextArea(4, 25);
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);

        addField(form, gbc, 0, "Shipment ID:", shipmentIdLabel);
        addField(form, gbc, 1, "Current Status:", currentStatusLabel);
        addField(form, gbc, 2, "New Status:", statusBox);
        addField(form, gbc, 3, "Current Location:", locationField);
        addField(form, gbc, 4, "Remarks:", new JScrollPane(remarksArea));

        JButton updateButton = new JButton("Save Status Update");
        JButton clearButton = new JButton("Clear");

        styleButton(updateButton);
        styleButton(clearButton);

        JPanel buttonPanel = new JPanel(
            new FlowLayout(FlowLayout.CENTER, 20, 10)
        );
        buttonPanel.setOpaque(false);
        buttonPanel.add(updateButton);
        buttonPanel.add(clearButton);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(form, BorderLayout.CENTER);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        // ACTIONS
        searchButton.addActionListener(e -> searchShipment());
        trackingField.addActionListener(e -> searchShipment());
        updateButton.addActionListener(e -> updateStatus());
        clearButton.addActionListener(e -> clearForm());
    }

    private void addField(
        JPanel panel,
        GridBagConstraints gbc,
        int row,
        String label,
        JComponent component
    ) {

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;

        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setFont(new Font("Arial", Font.BOLD, 14));
        fieldLabel.setForeground(plum);

        panel.add(fieldLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;

        panel.add(component, gbc);
    }

    // SEARCH SHIPMENT
    private void searchShipment() {

        String trackingNumber = trackingField.getText().trim();

        if (trackingNumber.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, "Enter a tracking number."
            );
            return;
        }

        String sql =
            "SELECT shipment_id, delivery_status " +
            "FROM shipments WHERE tracking_number = ?";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, trackingNumber);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    selectedShipmentId = rs.getInt("shipment_id");

                    shipmentIdLabel.setText(
                        String.valueOf(selectedShipmentId)
                    );

                    currentStatusLabel.setText(
                        rs.getString("delivery_status")
                    );

                    statusBox.setSelectedItem(
                        rs.getString("delivery_status").toUpperCase()
                    );

                    JOptionPane.showMessageDialog(
                        this, "Shipment found successfully!"
                    );

                } else {

                    selectedShipmentId = -1;
                    shipmentIdLabel.setText("Not found");
                    currentStatusLabel.setText("Not found");

                    JOptionPane.showMessageDialog(
                        this, "No shipment found with this tracking number."
                    );
                }
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this, "Search failed: " + ex.getMessage()
            );
            ex.printStackTrace();
        }
    }

    // UPDATE SHIPMENT STATUS
    private void updateStatus() {

        if (selectedShipmentId == -1) {
            JOptionPane.showMessageDialog(
                this, "Search and select a shipment first."
            );
            return;
        }

        String newStatus = statusBox.getSelectedItem().toString();
        String location = locationField.getText().trim();
        String remarks = remarksArea.getText().trim();

        if (location.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, "Enter the current location."
            );
            return;
        }

        if (remarks.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, "Enter remarks for this update."
            );
            return;
        }

        Connection con = null;

        try {

            con = Db.getConnection();
            con.setAutoCommit(false);

            String updateSql =
                "UPDATE shipments SET delivery_status = ? " +
                "WHERE shipment_id = ?";

            try (PreparedStatement ps =
                     con.prepareStatement(updateSql)) {

                ps.setString(1, newStatus);
                ps.setInt(2, selectedShipmentId);
                ps.executeUpdate();
            }

            String logSql =
                "INSERT INTO tracking_logs " +
                "(shipment_id, status, location, remarks, updated_by) " +
                "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement ps =
                     con.prepareStatement(logSql)) {

                ps.setInt(1, selectedShipmentId);
                ps.setString(2, newStatus);
                ps.setString(3, location);
                ps.setString(4, remarks);

                if (adminId > 0) {
                    ps.setInt(5, adminId);
                } else {
                    ps.setNull(5, Types.INTEGER);
                }

                ps.executeUpdate();
            }

            con.commit();

            currentStatusLabel.setText(newStatus);

            JOptionPane.showMessageDialog(
                this, "Shipment status updated successfully!"
            );

        } catch (SQLException ex) {

            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }

            JOptionPane.showMessageDialog(
                this, "Update failed: " + ex.getMessage()
            );

            ex.printStackTrace();

        } finally {

            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void clearForm() {

        trackingField.setText("");
        locationField.setText("");
        remarksArea.setText("");

        shipmentIdLabel.setText("Not selected");
        currentStatusLabel.setText("Not selected");

        selectedShipmentId = -1;
        statusBox.setSelectedIndex(0);
    }

    private void styleButton(JButton button) {

        button.setBackground(plum);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        button.setBorder(
            new EmptyBorder(10, 18, 10, 18)
        );
    }
}