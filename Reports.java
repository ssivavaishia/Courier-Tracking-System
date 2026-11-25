import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.io.*;

public class Reports extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> reportType;

    private final Color plum = new Color(75, 35, 75);

    public Reports() {

        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 238, 248));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("GENERATE REPORTS");
        title.setFont(new Font("Arial", Font.BOLD, 25));
        title.setForeground(plum);

        reportType = new JComboBox<>(new String[]{
            "Shipment Report",
            "Customer Report",
            "Delivery Personnel Report",
            "Payment Report"
        });

        JButton generateButton = new JButton("Generate Report");
        JButton exportButton = new JButton("Export CSV");

        styleButton(generateButton);
        styleButton(exportButton);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        top.add(new JLabel("Select Report:"));
        top.add(reportType);
        top.add(generateButton);
        top.add(exportButton);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(title, BorderLayout.NORTH);
        heading.add(top, BorderLayout.SOUTH);

        model = new DefaultTableModel();

        table = new JTable(model);
        table.setRowHeight(27);
        table.getTableHeader().setBackground(plum);
        table.getTableHeader().setForeground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(table);

        add(heading, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        generateButton.addActionListener(e -> generateReport());
        exportButton.addActionListener(e -> exportCSV());

        generateReport();
    }

    private void generateReport() {

        String selected = reportType.getSelectedItem().toString();

        String sql;

        switch (selected) {

            case "Shipment Report":
                sql =
                    "SELECT shipment_id, tracking_number, customer_id, " +
                    "receiver_name, receiver_address, delivery_status, " +
                    "booking_date, delivery_date FROM shipments";
                break;

            case "Customer Report":
                sql =
                    "SELECT user_id, full_name, username, email, phone " +
                    "FROM users WHERE UPPER(role) = 'CUSTOMER'";
                break;

            case "Delivery Personnel Report":
                sql =
                    "SELECT user_id, full_name, username, email, phone, " +
                    "account_status FROM users " +
                    "WHERE UPPER(role) = 'DELIVERY'";
                break;

            case "Payment Report":
                sql =
                    "SELECT payment_id, shipment_id, amount, " +
                    "payment_method, payment_status, payment_date " +
                    "FROM payments";
                break;

            default:
                return;
        }

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            model.setRowCount(0);
            model.setColumnCount(0);

            ResultSetMetaData meta = rs.getMetaData();

            int columns = meta.getColumnCount();

            for (int i = 1; i <= columns; i++) {
                model.addColumn(meta.getColumnLabel(i));
            }

            while (rs.next()) {

                Object[] row = new Object[columns];

                for (int i = 1; i <= columns; i++) {
                    row[i - 1] = rs.getObject(i);
                }

                model.addRow(row);
            }

            JOptionPane.showMessageDialog(
                this, "Report generated successfully!"
            );

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this, "Report error: " + ex.getMessage()
            );

            ex.printStackTrace();
        }
    }

    private void exportCSV() {

        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(
                this, "Generate a report before exporting."
            );
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(
            new File("SUDWAR_Report.csv")
        );

        if (chooser.showSaveDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        try (PrintWriter writer = new PrintWriter(
                new FileWriter(file))) {

            for (int col = 0; col < model.getColumnCount(); col++) {
                writer.print(csvValue(model.getColumnName(col)));

                if (col < model.getColumnCount() - 1) {
                    writer.print(",");
                }
            }

            writer.println();

            for (int row = 0; row < model.getRowCount(); row++) {

                for (int col = 0; col < model.getColumnCount(); col++) {

                    writer.print(
                        csvValue(model.getValueAt(row, col))
                    );

                    if (col < model.getColumnCount() - 1) {
                        writer.print(",");
                    }
                }

                writer.println();
            }

            JOptionPane.showMessageDialog(
                this, "Report exported successfully!"
            );

        } catch (IOException ex) {

            JOptionPane.showMessageDialog(
                this, "Export failed: " + ex.getMessage()
            );
            ex.printStackTrace();
        }
    }

    private String csvValue(Object value) {

        if (value == null) return "";

        String text = value.toString().replace("\"", "\"\"");

        return "\"" + text + "\"";
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