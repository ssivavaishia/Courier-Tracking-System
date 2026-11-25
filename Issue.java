
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Issue extends JPanel {

    private int customerId;
    private JTextField trackingField;
    private JComboBox<String> typeBox;
    private JTextArea descriptionArea;
    private DefaultTableModel model;

    private final Color plum = new Color(55, 20, 58);
    private final Color orchid = new Color(239, 225, 242);
    private final Color cinnamon = new Color(157, 83, 65);
    private final Color chocolate = new Color(83, 48, 42);

    public Issue(int customerId, String customerName) {

        this.customerId = customerId;

        setLayout(new BorderLayout(15, 15));
        setBackground(orchid);
        setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel heading = new JLabel("Delivery Exception");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 27));
        heading.setForeground(plum);

        JLabel subtitle = new JLabel(
            "Report and monitor problems with your shipments."
        );
        subtitle.setForeground(chocolate);

        JPanel headingPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        headingPanel.setOpaque(false);
        headingPanel.add(heading);
        headingPanel.add(subtitle);

        // Complaint form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Report a Problem"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        trackingField = new JTextField(20);

        typeBox = new JComboBox<>(new String[] {
            "Delayed Delivery",
            "Damaged Parcel",
            "Missing Parcel",
            "Incorrect Delivery",
            "Other"
        });

        descriptionArea = new JTextArea(4, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        addFormRow(form, gbc, 0, "Tracking Number:", trackingField);
        addFormRow(form, gbc, 1, "Exception Type:", typeBox);
        addFormRow(form, gbc, 2, "Description:",
            new JScrollPane(descriptionArea));

        JButton submitButton = new JButton("Submit Exception");
        submitButton.setBackground(cinnamon);
        submitButton.setForeground(Color.WHITE);
        submitButton.setFocusPainted(false);

        JButton clearButton = new JButton("Clear");
        clearButton.setBackground(plum);
        clearButton.setForeground(Color.WHITE);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(clearButton);
        buttonPanel.add(submitButton);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        form.add(buttonPanel, gbc);

        // Exception history
        String[] columns = {
            "Exception ID",
            "Tracking Number",
            "Exception Type",
            "Description",
            "Status",
            "Reported Date"
        };

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.getTableHeader().setBackground(plum);
        table.getTableHeader().setForeground(Color.WHITE);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(
            BorderFactory.createTitledBorder("Previous Reports")
        );

        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);
        center.add(form, BorderLayout.NORTH);
        center.add(tableScroll, BorderLayout.CENTER);

        add(headingPanel, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        submitButton.addActionListener(e -> submitException());

        clearButton.addActionListener(e -> {
            trackingField.setText("");
            descriptionArea.setText("");
            typeBox.setSelectedIndex(0);
        });

        loadExceptions();
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc,
                            int row, String label, Component component) {

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;

        JLabel title = new JLabel(label);
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(chocolate);
        panel.add(title, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(component, gbc);
    }

    private void submitException() {

        String tracking = trackingField.getText().trim();
        String description = descriptionArea.getText().trim();
        String type = (String) typeBox.getSelectedItem();

        if (tracking.isEmpty() || description.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Enter the tracking number and description."
            );
            return;
        }

        if (customerId <= 0) {
            JOptionPane.showMessageDialog(
                this, "Please log in again."
            );
            return;
        }

        String checkSql =
            "SELECT shipment_id FROM shipments " +
            "WHERE tracking_number = ? AND customer_id = ?";

        String insertSql =
            "INSERT INTO delivery_exceptions " +
            "(shipment_id, customer_id, exception_type, " +
            "description, exception_status) " +
            "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = Db.getConnection();
             PreparedStatement check = con.prepareStatement(checkSql)) {

            check.setString(1, tracking);
            check.setInt(2, customerId);

            try (ResultSet rs = check.executeQuery()) {

                if (!rs.next()) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Shipment not found. Check your tracking number."
                    );
                    return;
                }

                int shipmentId = rs.getInt("shipment_id");

                try (PreparedStatement insert =
                         con.prepareStatement(insertSql)) {

                    insert.setInt(1, shipmentId);
                    insert.setInt(2, customerId);
                    insert.setString(3, type);
                    insert.setString(4, description);
                    insert.setString(5, "Pending");

                    insert.executeUpdate();
                }
            }

            JOptionPane.showMessageDialog(
                this,
                "Your delivery exception has been submitted!"
            );

            trackingField.setText("");
            descriptionArea.setText("");

            loadExceptions();

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Database Error: " + ex.getMessage()
            );

            ex.printStackTrace();
        }
    }

    private void loadExceptions() {

        model.setRowCount(0);

        String sql =
            "SELECT d.exception_id, s.tracking_number, " +
            "d.exception_type, d.description, " +
            "d.exception_status, d.reported_date " +
            "FROM delivery_exceptions d " +
            "JOIN shipments s ON d.shipment_id = s.shipment_id " +
            "WHERE d.customer_id = ? " +
            "ORDER BY d.reported_date DESC";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Timestamp date = rs.getTimestamp("reported_date");

                    model.addRow(new Object[] {
                        rs.getInt("exception_id"),
                        rs.getString("tracking_number"),
                        rs.getString("exception_type"),
                        rs.getString("description"),
                        rs.getString("exception_status"),
                        date == null ? "N/A" : date.toString()
                    });
                }
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Unable to load exception history: " + ex.getMessage()
            );

            ex.printStackTrace();
        }
    }
}
