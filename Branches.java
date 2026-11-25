
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Branches extends JPanel {

    private JTable branchTable;
    private DefaultTableModel model;
    private JTextField searchField;

    private final Color plum = new Color(75, 35, 75);
    private final Color lightPurple = new Color(235, 220, 240);

    public Branches() {

        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 238, 248));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // TITLE
        JLabel title = new JLabel("MANAGE BRANCHES");
        title.setFont(new Font("Arial", Font.BOLD, 25));
        title.setForeground(plum);

        // SEARCH
        searchField = new JTextField(20);

        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh");

        styleButton(searchButton);
        styleButton(refreshButton);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("Search Branch:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(title, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        // TABLE
        model = new DefaultTableModel(
            new String[]{
                "Branch ID", "Branch Name", "Address", "Phone"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        branchTable = new JTable(model);
        branchTable.setRowHeight(28);
        branchTable.setFont(new Font("Arial", Font.PLAIN, 14));
        branchTable.getTableHeader().setFont(
            new Font("Arial", Font.BOLD, 14)
        );
        branchTable.getTableHeader().setBackground(plum);
        branchTable.getTableHeader().setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(branchTable);

        // BUTTONS
        JButton addButton = new JButton("Add Branch");
        JButton editButton = new JButton("Edit Branch");
        JButton deleteButton = new JButton("Delete Branch");

        styleButton(addButton);
        styleButton(editButton);
        styleButton(deleteButton);

        JPanel buttonPanel = new JPanel(
            new FlowLayout(FlowLayout.CENTER, 15, 10)
        );
        buttonPanel.setOpaque(false);

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // ACTIONS
        searchButton.addActionListener(e -> loadBranches());
        searchField.addActionListener(e -> loadBranches());

        refreshButton.addActionListener(e -> {
            searchField.setText("");
            loadBranches();
        });

        addButton.addActionListener(e -> addBranch());
        editButton.addActionListener(e -> editBranch());
        deleteButton.addActionListener(e -> deleteBranch());

        loadBranches();
    }

    // LOAD BRANCHES
    private void loadBranches() {

        model.setRowCount(0);

        String search = searchField.getText().trim();

        String sql =
            "SELECT branch_id, branch_name, address, phone " +
            "FROM branches " +
            "WHERE branch_name LIKE ? OR address LIKE ? " +
            "ORDER BY branch_id";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String pattern = "%" + search + "%";

            ps.setString(1, pattern);
            ps.setString(2, pattern);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    model.addRow(new Object[]{
                        rs.getInt("branch_id"),
                        rs.getString("branch_name"),
                        rs.getString("address"),
                        rs.getString("phone")
                    });
                }
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this, "Unable to load branches: " + ex.getMessage()
            );
            ex.printStackTrace();
        }
    }

    // ADD BRANCH
    private void addBranch() {

        JTextField nameField = new JTextField();
        JTextField addressField = new JTextField();
        JTextField phoneField = new JTextField();

        JPanel panel = createForm(
            nameField, addressField, phoneField
        );

        int result = JOptionPane.showConfirmDialog(
            this, panel, "Add New Branch",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();

        if (!validateBranch(name, address, phone)) return;

        String sql =
            "INSERT INTO branches (branch_name, address, phone) " +
            "VALUES (?, ?, ?)";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, address);
            ps.setString(3, phone);

            ps.executeUpdate();

            JOptionPane.showMessageDialog(
                this, "Branch added successfully!"
            );

            loadBranches();

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this, "Unable to add branch: " + ex.getMessage()
            );
            ex.printStackTrace();
        }
    }

    // EDIT BRANCH
    private void editBranch() {

        int row = branchTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(
                this, "Please select a branch."
            );
            return;
        }

        int branchId = Integer.parseInt(
            model.getValueAt(row, 0).toString()
        );

        JTextField nameField = new JTextField(
            model.getValueAt(row, 1).toString()
        );

        JTextField addressField = new JTextField(
            model.getValueAt(row, 2).toString()
        );

        JTextField phoneField = new JTextField(
            model.getValueAt(row, 3).toString()
        );

        JPanel panel = createForm(
            nameField, addressField, phoneField
        );

        int result = JOptionPane.showConfirmDialog(
            this, panel, "Edit Branch",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();

        if (!validateBranch(name, address, phone)) return;

        String sql =
            "UPDATE branches SET branch_name = ?, " +
            "address = ?, phone = ? WHERE branch_id = ?";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, address);
            ps.setString(3, phone);
            ps.setInt(4, branchId);

            ps.executeUpdate();

            JOptionPane.showMessageDialog(
                this, "Branch updated successfully!"
            );

            loadBranches();

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this, "Unable to update branch: " + ex.getMessage()
            );
            ex.printStackTrace();
        }
    }

    // DELETE BRANCH
    private void deleteBranch() {

        int row = branchTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(
                this, "Please select a branch."
            );
            return;
        }

        int branchId = Integer.parseInt(
            model.getValueAt(row, 0).toString()
        );

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this branch?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        String checkSql =
            "SELECT COUNT(*) FROM shipments " +
            "WHERE source_branch_id = ? " +
            "OR destination_branch_id = ?";

        try (Connection con = Db.getConnection()) {

            try (PreparedStatement check =
                     con.prepareStatement(checkSql)) {

                check.setInt(1, branchId);
                check.setInt(2, branchId);

                try (ResultSet rs = check.executeQuery()) {

                    if (rs.next() && rs.getInt(1) > 0) {

                        JOptionPane.showMessageDialog(
                            this,
                            "This branch is linked to shipments. " +
                            "It cannot be deleted."
                        );
                        return;
                    }
                }
            }

            String deleteSql =
                "DELETE FROM branches WHERE branch_id = ?";

            try (PreparedStatement ps =
                     con.prepareStatement(deleteSql)) {

                ps.setInt(1, branchId);
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(
                this, "Branch deleted successfully!"
            );

            loadBranches();

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this, "Unable to delete branch: " + ex.getMessage()
            );
            ex.printStackTrace();
        }
    }

    // FORM DESIGN
    private JPanel createForm(
        JTextField name,
        JTextField address,
        JTextField phone
    ) {

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 12));

        panel.add(new JLabel("Branch Name:"));
        panel.add(name);

        panel.add(new JLabel("Address:"));
        panel.add(address);

        panel.add(new JLabel("Phone:"));
        panel.add(phone);

        return panel;
    }

    // VALIDATION
    private boolean validateBranch(
        String name,
        String address,
        String phone
    ) {

        if (name.isEmpty() || address.isEmpty() || phone.isEmpty()) {

            JOptionPane.showMessageDialog(
                this, "Please fill in all branch details."
            );
            return false;
        }

        if (!phone.matches("\\d{10,15}")) {

            JOptionPane.showMessageDialog(
                this, "Enter a valid phone number (10-15 digits)."
            );
            return false;
        }

        return true;
    }

    // BUTTON STYLE
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