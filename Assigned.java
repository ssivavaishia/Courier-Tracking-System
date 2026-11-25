import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

public class Assigned extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;
    private int deliveryPersonId;

    public Assigned(int deliveryPersonId) {

        this.deliveryPersonId = deliveryPersonId;

        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 238, 248));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("My Assigned Shipments");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(75, 35, 85));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);
        topPanel.add(title, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setOpaque(false);

        searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh");

        searchPanel.add(new JLabel("Tracking Number:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);

        topPanel.add(searchPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        model = new DefaultTableModel(
            new String[]{
                "Shipment ID",
                "Tracking Number",
                "Receiver Name",
                "Receiver Phone",
                "Delivery Address",
                "Status",
                "Booking Date"
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

        searchButton.addActionListener(e ->
            loadShipments(searchField.getText().trim())
        );

        refreshButton.addActionListener(e -> {
            searchField.setText("");
            loadShipments("");
        });

        loadShipments("");
    }

    private void loadShipments(String search) {

        model.setRowCount(0);

        String sql =
            "SELECT shipment_id, tracking_number, receiver_name, " +
            "receiver_phone, receiver_address, delivery_status, booking_date " +
            "FROM shipments " +
            "WHERE delivery_person_id = ? " +
            "AND tracking_number LIKE ? " +
            "ORDER BY booking_date DESC";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, deliveryPersonId);
            ps.setString(2, "%" + search + "%");

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt("shipment_id"),
                        rs.getString("tracking_number"),
                        rs.getString("receiver_name"),
                        rs.getString("receiver_phone"),
                        rs.getString("receiver_address"),
                        rs.getString("delivery_status"),
                        rs.getString("booking_date")
                    });
                }
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Unable to load assigned shipments:\n" + ex.getMessage()
            );
        }
    }
}