import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class History extends JPanel {

    private int deliveryPersonId;
    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;
    private JComboBox<String> statusFilter;

    public History(int deliveryPersonId) {

        this.deliveryPersonId = deliveryPersonId;

        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 238, 248));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Delivery History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(new Color(75, 35, 85));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);
        topPanel.add(title, BorderLayout.NORTH);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setOpaque(false);

        searchField = new JTextField(15);

        statusFilter = new JComboBox<>(new String[]{
            "ALL", "DELIVERED", "IN TRANSIT",
            "OUT FOR DELIVERY", "PICKED UP", "CANCELLED"
        });

        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh");

        filterPanel.add(new JLabel("Tracking Number:"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("Status:"));
        filterPanel.add(statusFilter);
        filterPanel.add(searchButton);
        filterPanel.add(refreshButton);

        topPanel.add(filterPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        model = new DefaultTableModel(
            new String[]{
                "Shipment ID",
                "Tracking Number",
                "Receiver Name",
                "Receiver Phone",
                "Delivery Address",
                "Status",
                "Booking Date",
                "Delivery Date"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(
            new Font("Segoe UI", Font.BOLD, 13)
        );

        add(new JScrollPane(table), BorderLayout.CENTER);

        searchButton.addActionListener(e -> loadHistory());

        refreshButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedItem("ALL");
            loadHistory();
        });

        statusFilter.addActionListener(e -> loadHistory());

        loadHistory();
    }

    private void loadHistory() {

        model.setRowCount(0);

        String search = searchField.getText().trim();
        String status = statusFilter.getSelectedItem().toString();

        String sql =
            "SELECT shipment_id, tracking_number, receiver_name, " +
            "receiver_phone, receiver_address, delivery_status, " +
            "booking_date, delivery_date " +
            "FROM shipments " +
            "WHERE delivery_person_id = ? " +
            "AND tracking_number LIKE ? " +
            "AND (? = 'ALL' OR UPPER(delivery_status) = ?) " +
            "ORDER BY booking_date DESC";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, deliveryPersonId);
            ps.setString(2, "%" + search + "%");
            ps.setString(3, status);
            ps.setString(4, status);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt("shipment_id"),
                        rs.getString("tracking_number"),
                        rs.getString("receiver_name"),
                        rs.getString("receiver_phone"),
                        rs.getString("receiver_address"),
                        rs.getString("delivery_status"),
                        rs.getString("booking_date"),
                        rs.getString("delivery_date")
                    });
                }
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Unable to load delivery history:\n" + ex.getMessage()
            );
        }
    }
}
