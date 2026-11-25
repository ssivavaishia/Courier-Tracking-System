
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Assign extends JPanel {

    private final Color plum = new Color(75, 35, 75);
    private final Color deepPlum = new Color(48, 20, 55);
    private final Color lightPurple = new Color(235, 220, 240);
    private final Color cream = new Color(250, 245, 235);

    private JTable shipmentTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> deliveryBox;

    private int[] deliveryIds = new int[0];

    public Assign() {

        setLayout(new BorderLayout(15, 15));
        setBackground(cream);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        // HEADER
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Assign Delivery Personnel");
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(plum);

        JLabel subtitle = new JLabel(
            "Assign unassigned shipments to delivery personnel"
        );
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitle.setForeground(deepPlum);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(title);
        heading.add(Box.createVerticalStrut(8));
        heading.add(subtitle);

        JButton refreshButton = new JButton("Refresh");
        styleButton(refreshButton);
        refreshButton.addActionListener(e -> loadShipments());

        header.add(heading, BorderLayout.WEST);
        header.add(refreshButton, BorderLayout.EAST);

        // SHIPMENT TABLE
        String[] columns = {
            "Shipment ID",
            "Tracking Number",
            "Receiver Name",
            "Receiver Address"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        shipmentTable = new JTable(tableModel);
        shipmentTable.setRowHeight(32);
        shipmentTable.setFont(new Font("Arial", Font.PLAIN, 13));
        shipmentTable.setSelectionBackground(lightPurple);
        shipmentTable.setSelectionForeground(deepPlum);
        shipmentTable.getTableHeader().setBackground(plum);
        shipmentTable.getTableHeader().setForeground(Color.WHITE);
        shipmentTable.getTableHeader().setFont(
            new Font("Arial", Font.BOLD, 13)
        );

        JScrollPane scrollPane = new JScrollPane(shipmentTable);
        scrollPane.setBorder(
            BorderFactory.createLineBorder(lightPurple, 2)
        );

        // ASSIGNMENT PANEL
        JPanel assignPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        assignPanel.setBackground(lightPurple);
        assignPanel.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(plum, 1),
                "Assign Selected Shipment"
            )
        );

        JLabel deliveryLabel = new JLabel("Delivery Personnel:");
        deliveryLabel.setFont(new Font("Arial", Font.BOLD, 14));
        deliveryLabel.setForeground(deepPlum);

        deliveryBox = new JComboBox<>();
        deliveryBox.setPreferredSize(new Dimension(220, 35));

        JButton assignButton = new JButton("Assign Shipment");
        styleButton(assignButton);
        assignButton.addActionListener(e -> assignShipment());

        assignPanel.add(deliveryLabel);
        assignPanel.add(deliveryBox);
        assignPanel.add(assignButton);

        // LAYOUT
        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(assignPanel, BorderLayout.SOUTH);

        loadDeliveryPersonnel();
        loadShipments();
    }

    private void styleButton(JButton button) {

        button.setBackground(plum);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(new EmptyBorder(12, 20, 12, 20));
        button.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
    }

    private void loadDeliveryPersonnel() {

        deliveryBox.removeAllItems();

        String sql =
            "SELECT user_id, full_name FROM users " +
            "WHERE UPPER(role) = 'DELIVERY' ORDER BY full_name";

        try (
            Connection con = Db.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            java.util.ArrayList<Integer> ids =
                new java.util.ArrayList<>();

            while (rs.next()) {

                ids.add(rs.getInt("user_id"));

                deliveryBox.addItem(
                    rs.getString("full_name")
                );
            }

            deliveryIds = new int[ids.size()];

            for (int i = 0; i < ids.size(); i++) {
                deliveryIds[i] = ids.get(i);
            }

            if (deliveryIds.length == 0) {
                deliveryBox.addItem("No delivery personnel found");
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Could not load delivery personnel:\n" + ex.getMessage()
            );
        }
    }

    private void loadShipments() {

        tableModel.setRowCount(0);

        String sql =
            "SELECT shipment_id, tracking_number, receiver_name, " +
            "receiver_address FROM shipments " +
            "WHERE delivery_person_id IS NULL " +
            "ORDER BY shipment_id";

        try (
            Connection con = Db.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                tableModel.addRow(new Object[] {
                    rs.getInt("shipment_id"),
                    rs.getString("tracking_number"),
                    rs.getString("receiver_name"),
                    rs.getString("receiver_address")
                });
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Could not load shipments:\n" + ex.getMessage()
            );
        }
    }

    private void assignShipment() {

        int selectedRow = shipmentTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a shipment first."
            );
            return;
        }

        int selectedDelivery = deliveryBox.getSelectedIndex();

        if (selectedDelivery < 0 ||
            selectedDelivery >= deliveryIds.length) {

            JOptionPane.showMessageDialog(
                this,
                "Please select a valid delivery person."
            );
            return;
        }

        int shipmentId = (int) tableModel.getValueAt(
            shipmentTable.convertRowIndexToModel(selectedRow), 0
        );

        int deliveryId = deliveryIds[selectedDelivery];

        String sql =
            "UPDATE shipments SET delivery_person_id = ? " +
            "WHERE shipment_id = ? AND delivery_person_id IS NULL";

        try (
            Connection con = Db.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, deliveryId);
            ps.setInt(2, shipmentId);

            int updated = ps.executeUpdate();

            if (updated > 0) {

                JOptionPane.showMessageDialog(
                    this,
                    "Shipment assigned successfully!"
                );

                loadShipments();

            } else {

                JOptionPane.showMessageDialog(
                    this,
                    "This shipment has already been assigned. Refresh and try again."
                );

                loadShipments();
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Assignment failed:\n" + ex.getMessage()
            );
        }
    }
}
