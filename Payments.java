import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Payments extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;
    private JComboBox<String> statusFilter;

    private final Color plum = new Color(75, 35, 75);

    public Payments() {

        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 238, 248));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("MANAGE PAYMENTS");
        title.setFont(new Font("Arial", Font.BOLD, 25));
        title.setForeground(plum);

        searchField = new JTextField(18);

        statusFilter = new JComboBox<>(
            new String[]{"ALL", "PENDING", "COMPLETED", "FAILED"}
        );

        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh");
        styleButton(searchButton);
        styleButton(refreshButton);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        top.add(new JLabel("Search Tracking Number:"));
        top.add(searchField);
        top.add(searchButton);
        top.add(new JLabel("Payment Status:"));
        top.add(statusFilter);
        top.add(refreshButton);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(title, BorderLayout.NORTH);
        heading.add(top, BorderLayout.SOUTH);

        model = new DefaultTableModel(
            new String[]{
                "Payment ID", "Shipment ID", "Tracking Number",
                "Customer", "Amount", "Method", "Status", "Date"
            }, 0
        ) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(28);
        table.getTableHeader().setBackground(plum);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(
            new Font("Arial", Font.BOLD, 13)
        );

        JScrollPane scroll = new JScrollPane(table);

        JButton viewButton = new JButton("View Details");
        styleButton(viewButton);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.add(viewButton);

        add(heading, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> loadPayments());
        searchField.addActionListener(e -> loadPayments());
        statusFilter.addActionListener(e -> loadPayments());

        refreshButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedItem("ALL");
            loadPayments();
        });

        viewButton.addActionListener(e -> viewDetails());

        loadPayments();
    }

    private void loadPayments() {

        model.setRowCount(0);

        String search = searchField.getText().trim();
        String status = statusFilter.getSelectedItem().toString();

        String sql =
            "SELECT p.payment_id, p.shipment_id, " +
            "s.tracking_number, u.full_name, p.amount, " +
            "p.payment_method, p.payment_status, p.payment_date " +
            "FROM payments p " +
            "JOIN shipments s ON p.shipment_id = s.shipment_id " +
            "LEFT JOIN users u ON s.customer_id = u.user_id " +
            "WHERE s.tracking_number LIKE ?";

        if (!status.equals("ALL")) {
            sql += " AND UPPER(p.payment_status) = ?";
        }

        sql += " ORDER BY p.payment_date DESC";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + search + "%");

            if (!status.equals("ALL")) {
                ps.setString(2, status);
            }

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    model.addRow(new Object[]{
                        rs.getInt("payment_id"),
                        rs.getInt("shipment_id"),
                        rs.getString("tracking_number"),
                        rs.getString("full_name"),
                        rs.getBigDecimal("amount"),
                        rs.getString("payment_method"),
                        rs.getString("payment_status"),
                        rs.getTimestamp("payment_date")
                    });
                }
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this, "Unable to load payments: " + ex.getMessage()
            );
            ex.printStackTrace();
        }
    }

    private void viewDetails() {

        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(
                this, "Please select a payment."
            );
            return;
        }

        StringBuilder details = new StringBuilder();

        for (int i = 0; i < model.getColumnCount(); i++) {
            details.append(model.getColumnName(i))
                   .append(": ")
                   .append(model.getValueAt(row, i))
                   .append("\n");
        }

        JOptionPane.showMessageDialog(
            this,
            details.toString(),
            "Payment Details",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void styleButton(JButton button) {

        button.setBackground(plum);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 13));
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