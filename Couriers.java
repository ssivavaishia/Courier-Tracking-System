import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * Admin screen: add new delivery personnel and see/deactivate existing ones.
 * Creates rows in the existing "users" table with role = 'DELIVERY', so the
 * person can log in through Login (email + password) and is routed to
 * DelHome automatically. Appears in Assign too.
 */
public class Couriers extends JPanel {

    private JTextField txtFullName, txtUsername, txtEmail, txtPhone;
    private JPasswordField txtPassword;
    private JCheckBox chkShow;
    private JTable table;
    private DefaultTableModel model;

    private final Color plum = new Color(75, 35, 75);
    private final Color bg = new Color(245, 238, 248);
    private final char defaultEcho;

    public Couriers() {

        setLayout(new BorderLayout(15, 15));
        setBackground(bg);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("DELIVERY PERSONNEL");
        title.setFont(new Font("Arial", Font.BOLD, 25));
        title.setForeground(plum);

        // ---------- ADD FORM ----------
        txtFullName = new JTextField(18);
        txtUsername = new JTextField(18);
        txtEmail = new JTextField(18);
        txtPhone = new JTextField(18);
        txtPassword = new JPasswordField(18);
        defaultEcho = txtPassword.getEchoChar();

        chkShow = new JCheckBox("Show");
        chkShow.setOpaque(false);
        chkShow.addActionListener(e ->
            txtPassword.setEchoChar(chkShow.isSelected() ? (char) 0 : defaultEcho));

        JButton addButton = new JButton("Add Delivery Person");
        styleButton(addButton);
        addButton.addActionListener(e -> addPerson());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("New delivery person"),
            new EmptyBorder(8, 10, 8, 10)));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 8, 5, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, g, 0, 0, "Full Name", txtFullName);
        addRow(form, g, 0, 2, "Username", txtUsername);
        addRow(form, g, 1, 0, "Email", txtEmail);
        addRow(form, g, 1, 2, "Phone", txtPhone);

        JPanel pwd = new JPanel(new BorderLayout(5, 0));
        pwd.setOpaque(false);
        pwd.add(txtPassword, BorderLayout.CENTER);
        pwd.add(chkShow, BorderLayout.EAST);
        addRow(form, g, 2, 0, "Temp Password", pwd);

        g.gridx = 3; g.gridy = 2; g.anchor = GridBagConstraints.EAST;
        g.fill = GridBagConstraints.NONE;
        form.add(addButton, g);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);

        // ---------- TABLE ----------
        model = new DefaultTableModel(new String[]{
            "User ID", "Full Name", "Username", "Email",
            "Phone", "Account Status", "Active Shipments"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        table.getTableHeader().setBackground(plum);
        table.getTableHeader().setForeground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Current delivery personnel"));

        JButton toggle = new JButton("Activate / Deactivate");
        JButton refresh = new JButton("Refresh");
        styleButton(toggle);
        styleButton(refresh);
        toggle.addActionListener(e -> toggleStatus());
        refresh.addActionListener(e -> loadPersonnel());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttons.setOpaque(false);
        buttons.add(toggle);
        buttons.add(refresh);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        loadPersonnel();
    }

    // ---------- ADD ----------
    private void addPerson() {

        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        char[] pw = txtPassword.getPassword();

        try {
            String password = new String(pw);

            String error = validate(fullName, username, email, phone, password);
            if (error != null) {
                JOptionPane.showMessageDialog(this, error,
                    "Check the form", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Same insert shape as Register, but role = DELIVERY
            String sql = "INSERT INTO users "
                + "(full_name, username, email, phone, password_hash, role) "
                + "VALUES (?, ?, ?, ?, ?, 'DELIVERY')";

            try (Connection con = Db.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, fullName);
                ps.setString(2, username);
                ps.setString(3, email);
                ps.setString(4, phone);
                ps.setString(5, Pass.hashPassword(password));
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this,
                fullName + " was added.\nThey can log in with their email "
                + "and the temporary password you set.",
                "Delivery person added", JOptionPane.INFORMATION_MESSAGE);

            clearForm();
            loadPersonnel();

        } catch (SQLIntegrityConstraintViolationException ex) {
            JOptionPane.showMessageDialog(this,
                "Username, email or phone already exists.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Unable to add delivery person: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            java.util.Arrays.fill(pw, '\0');
        }
    }

    // Same rules as Register
    private String validate(String name, String user, String email,
                            String phone, String pass) {
        if (name.isEmpty() || user.isEmpty())
            return "Full name and username are required.";
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            return "Enter a valid email address.";
        if (!phone.matches("[6-9][0-9]{9}"))
            return "Enter a valid 10-digit phone number.";
        if (!pass.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,20}$"))
            return "Password needs 8-20 characters with uppercase, lowercase, "
                 + "a number and a special character.";
        return null;
    }

    // ---------- LIST ----------
    private void loadPersonnel() {

        model.setRowCount(0);

        String sql =
            "SELECT u.user_id, u.full_name, u.username, u.email, u.phone, "
            + "u.account_status, "
            + "(SELECT COUNT(*) FROM shipments s "
            + "  WHERE s.delivery_person_id = u.user_id "
            + "  AND UPPER(s.delivery_status) NOT IN ('DELIVERED','CANCELLED')) "
            + "AS active_count "
            + "FROM users u WHERE UPPER(u.role) = 'DELIVERY' ORDER BY u.full_name";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("user_id"), rs.getString("full_name"),
                    rs.getString("username"), rs.getString("email"),
                    rs.getString("phone"), rs.getString("account_status"),
                    rs.getInt("active_count")});
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Unable to load delivery personnel: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ---------- ACTIVATE / DEACTIVATE ----------
    private void toggleStatus() {

        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a delivery person.");
            return;
        }

        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        String current = String.valueOf(model.getValueAt(row, 5));
        int active = Integer.parseInt(model.getValueAt(row, 6).toString());
        String next = "ACTIVE".equalsIgnoreCase(current) ? "INACTIVE" : "ACTIVE";

        String msg = "Change status to " + next + "?";
        if (next.equals("INACTIVE") && active > 0) {
            msg += "\n\nThey still have " + active + " active shipment(s). "
                 + "Reassign them first.";
        }

        if (JOptionPane.showConfirmDialog(this, msg, "Confirm",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        String sql = "UPDATE users SET account_status = ? "
                   + "WHERE user_id = ? AND UPPER(role) = 'DELIVERY'";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, next);
            ps.setInt(2, id);
            ps.executeUpdate();
            loadPersonnel();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Unable to update status: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ---------- HELPERS ----------
    private void addRow(JPanel p, GridBagConstraints g, int row, int col,
                        String label, JComponent field) {
        g.gridy = row; g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = col; g.anchor = GridBagConstraints.WEST;
        p.add(new JLabel(label + ":"), g);
        g.gridx = col + 1;
        p.add(field, g);
    }

    private void clearForm() {
        txtFullName.setText(""); txtUsername.setText("");
        txtEmail.setText(""); txtPhone.setText(""); txtPassword.setText("");
        chkShow.setSelected(false); txtPassword.setEchoChar(defaultEcho);
    }

    private void styleButton(JButton b) {
        b.setBackground(plum);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Arial", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(10, 18, 10, 18));
    }
}