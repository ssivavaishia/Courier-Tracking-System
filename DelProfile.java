import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

public class DelProfile extends JPanel {

    private int deliveryPersonId;

    private JTextField nameField;
    private JTextField usernameField;
    private JTextField emailField;
    private JTextField phoneField;

    private final Color plum = new Color(75, 35, 85);

    public DelProfile(int deliveryPersonId) {

        this.deliveryPersonId = deliveryPersonId;

        setLayout(new BorderLayout());
        setBackground(new Color(245, 238, 248));
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel("My Profile");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(plum);

        JPanel form = new JPanel(new GridLayout(4, 2, 15, 20));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(35, 35, 35, 35));

        nameField = new JTextField();
        usernameField = new JTextField();
        emailField = new JTextField();
        phoneField = new JTextField();

        form.add(new JLabel("Full Name:"));
        form.add(nameField);

        form.add(new JLabel("Username:"));
        form.add(usernameField);

        form.add(new JLabel("Email:"));
        form.add(emailField);

        form.add(new JLabel("Phone:"));
        form.add(phoneField);

        emailField.setEditable(false);

        JButton saveButton = new JButton("Save Changes");
        JButton resetButton = new JButton("Reset");

        styleButton(saveButton);
        styleButton(resetButton);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(saveButton);
        buttonPanel.add(resetButton);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 20));
        centerPanel.setBackground(new Color(245, 238, 248));
        centerPanel.add(form, BorderLayout.CENTER);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(title, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        loadProfile();

        saveButton.addActionListener(e -> updateProfile());
        resetButton.addActionListener(e -> loadProfile());
    }

    private void loadProfile() {

        String sql =
            "SELECT full_name, username, email, phone " +
            "FROM users WHERE user_id = ? " +
            "AND UPPER(role) = 'DELIVERY'";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, deliveryPersonId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    nameField.setText(rs.getString("full_name"));
                    usernameField.setText(rs.getString("username"));
                    emailField.setText(rs.getString("email"));
                    phoneField.setText(rs.getString("phone"));
                }
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Unable to load profile:\n" + ex.getMessage()
            );
        }
    }

    private void updateProfile() {

        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String phone = phoneField.getText().trim();

        if (name.isEmpty() || username.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Please fill in all fields."
            );
            return;
        }

        if (!phone.matches("\\d{10}")) {
            JOptionPane.showMessageDialog(
                this,
                "Enter a valid 10-digit phone number."
            );
            return;
        }

        String sql =
            "UPDATE users SET full_name = ?, username = ?, phone = ? " +
            "WHERE user_id = ? AND UPPER(role) = 'DELIVERY'";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, username);
            ps.setString(3, phone);
            ps.setInt(4, deliveryPersonId);

            int result = ps.executeUpdate();

            if (result > 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "Profile updated successfully!"
                );
                loadProfile();
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Profile could not be updated."
                );
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Error updating profile:\n" + ex.getMessage()
            );
        }
    }

    private void styleButton(JButton button) {
        button.setBackground(plum);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
    }
}