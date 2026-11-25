
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class Profile extends JPanel {

    private int customerId;

    private JTextField nameField;
    private JTextField usernameField;
    private JTextField emailField;
    private JTextField phoneField;

    private JButton editButton;
    private JButton saveButton;
    private JButton resetButton;

    private String originalName;
    private String originalEmail;
    private String originalPhone;

    private final Color plum = new Color(55, 20, 58);
    private final Color orchid = new Color(239, 225, 242);
    private final Color cinnamon = new Color(157, 83, 65);
    private final Color chocolate = new Color(83, 48, 42);

    public Profile(int customerId, String customerName) {

        this.customerId = customerId;

        setLayout(new BorderLayout(15, 15));
        setBackground(orchid);
        setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 35));

        JLabel heading = new JLabel("My Profile");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 28));
        heading.setForeground(plum);

        JLabel subtitle = new JLabel(
            "View and manage your personal information."
        );
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(chocolate);

        JPanel headingPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        headingPanel.setOpaque(false);
        headingPanel.add(heading);
        headingPanel.add(subtitle);

        JPanel profilePanel = new JPanel(new GridBagLayout());
        profilePanel.setBackground(Color.WHITE);
        profilePanel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 205, 220)),
                BorderFactory.createEmptyBorder(25, 35, 25, 35)
            )
        );

        nameField = new JTextField(22);
        usernameField = new JTextField(22);
        emailField = new JTextField(22);
        phoneField = new JTextField(22);

        usernameField.setEditable(false);

        addField(profilePanel, 0, "Full Name:", nameField);
        addField(profilePanel, 1, "Username:", usernameField);
        addField(profilePanel, 2, "Email Address:", emailField);
        addField(profilePanel, 3, "Phone Number:", phoneField);

        editButton = createButton("Edit Profile");
        saveButton = createButton("Save Changes");
        resetButton = createButton("Reset");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setOpaque(false);

        buttonPanel.add(editButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(resetButton);

        JPanel centerPanel = new JPanel(new BorderLayout(15, 20));
        centerPanel.setOpaque(false);
        centerPanel.add(profilePanel, BorderLayout.CENTER);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(headingPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        setEditing(false);

        editButton.addActionListener(e -> setEditing(true));
        saveButton.addActionListener(e -> updateProfile());
        resetButton.addActionListener(e -> resetFields());

        loadProfile();
    }

    private void addField(JPanel panel, int row,
                          String label, JTextField field) {

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 10, 12, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;

        JLabel title = new JLabel(label);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(chocolate);

        panel.add(title, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;

        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(260, 35));

        panel.add(field, gbc);
    }

    private JButton createButton(String text) {

        JButton button = new JButton(text);
        button.setBackground(cinnamon);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);

        return button;
    }

    private void setEditing(boolean editing) {

        nameField.setEditable(editing);
        emailField.setEditable(editing);
        phoneField.setEditable(editing);

        saveButton.setEnabled(editing);
        resetButton.setEnabled(editing);
        editButton.setEnabled(!editing);
    }

    private void loadProfile() {

        String sql =
            "SELECT full_name, username, email, phone " +
            "FROM users WHERE user_id = ? AND role = 'CUSTOMER'";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    nameField.setText(rs.getString("full_name"));
                    usernameField.setText(rs.getString("username"));
                    emailField.setText(rs.getString("email"));
                    phoneField.setText(rs.getString("phone"));

                    saveOriginalValues();

                } else {

                    JOptionPane.showMessageDialog(
                        this,
                        "Customer profile not found."
                    );
                }
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Unable to load profile: " + ex.getMessage()
            );

            ex.printStackTrace();
        }
    }

    private void saveOriginalValues() {

        originalName = nameField.getText();
        originalEmail = emailField.getText();
        originalPhone = phoneField.getText();
    }

    private void resetFields() {

        nameField.setText(originalName);
        emailField.setText(originalEmail);
        phoneField.setText(originalPhone);

        setEditing(false);
    }

    private void updateProfile() {

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {

            JOptionPane.showMessageDialog(
                this,
                "Please fill in all fields."
            );
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

            JOptionPane.showMessageDialog(
                this,
                "Enter a valid email address."
            );
            return;
        }

        if (!phone.matches("[0-9]{10}")) {

            JOptionPane.showMessageDialog(
                this,
                "Enter a valid 10-digit phone number."
            );
            return;
        }

        String sql =
            "UPDATE users SET full_name = ?, email = ?, phone = ? " +
            "WHERE user_id = ? AND role = 'CUSTOMER'";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setInt(4, customerId);

            int updated = ps.executeUpdate();

            if (updated > 0) {

                saveOriginalValues();
                setEditing(false);

                JOptionPane.showMessageDialog(
                    this,
                    "Profile updated successfully!"
                );

            } else {

                JOptionPane.showMessageDialog(
                    this,
                    "Profile could not be updated."
                );
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Database Error: " + ex.getMessage()
            );

            ex.printStackTrace();
        }
    }
}