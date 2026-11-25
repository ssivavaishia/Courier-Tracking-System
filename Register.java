import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.*;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

public class Register extends JFrame {

    private JTextField txtFullName, txtUsername;
    private JTextField txtEmail, txtPhone;
    private JPasswordField txtPassword, txtConfirmPassword;

    private JLabel emailError, phoneError;
    private JLabel passwordError, confirmError;

    private JCheckBox chkShowPassword;

    private final Map<JComponent, Border> normalBorders =
        new IdentityHashMap<>();

    private char passwordEchoChar;

    private final Color ORCHID = new Color(218, 190, 224);
    private final Color PLUM = new Color(65, 30, 70, 225);
    private final Color CINNAMON = new Color(165, 91, 65);
    private final Color LIGHT_TEXT = new Color(255, 247, 238);
    private final Color SOFT_LILAC = new Color(239, 222, 244);
    private final Color ERROR_RED = new Color(255, 100, 100);

    public Register() {

        setTitle("SUDWAR - Registration");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(900, 650));

        BackgroundPanel background =
            new BackgroundPanel("/resources/registration.png");

        background.setLayout(new GridBagLayout());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);
        panel.setBackground(PLUM);
        panel.setBorder(new EmptyBorder(30, 45, 30, 45));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 12, 7, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel(
            "CREATE ACCOUNT", SwingConstants.CENTER
        );

        heading.setFont(new Font("Serif", Font.BOLD, 34));
        heading.setForeground(LIGHT_TEXT);

        JLabel subtitle = new JLabel(
            "Join SUDWAR and manage your shipments",
            SwingConstants.CENTER
        );

        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
        subtitle.setForeground(SOFT_LILAC);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(heading, gbc);

        gbc.gridy = 1;
        panel.add(subtitle, gbc);

        gbc.gridwidth = 1;

        txtFullName = new JTextField(20);
        txtUsername = new JTextField(20);
        txtEmail = new JTextField(20);
        txtPhone = new JTextField(20);
        txtPassword = new JPasswordField(20);
        txtConfirmPassword = new JPasswordField(20);

        passwordEchoChar = txtPassword.getEchoChar();

        emailError = createErrorLabel();
        phoneError = createErrorLabel();
        passwordError = createErrorLabel();
        confirmError = createErrorLabel();

        addField(panel, gbc, "Full Name", txtFullName, 2);
        addField(panel, gbc, "Username", txtUsername, 3);

        addValidatedField(
            panel, gbc, "Email Address",
            txtEmail, emailError, 4
        );

        addValidatedField(
            panel, gbc, "Phone Number",
            txtPhone, phoneError, 5
        );

        addPasswordField(panel, gbc, 6);

        addValidatedField(
            panel, gbc, "Confirm Password",
            txtConfirmPassword, confirmError, 7
        );

        JButton btnRegister = new JButton("REGISTER");
        JButton btnReset = new JButton("RESET");
        JButton btnPrevious = new JButton("PREVIOUS");
        JButton btnLogin = new JButton("GO TO LOGIN");

        styleButton(btnRegister);
        styleButton(btnReset);
        styleButton(btnPrevious);
        styleButton(btnLogin);

        JPanel mainButtons = new JPanel(
            new FlowLayout(FlowLayout.CENTER, 12, 5)
        );

        mainButtons.setOpaque(false);
        mainButtons.add(btnRegister);
        mainButtons.add(btnReset);

        JPanel navigationButtons = new JPanel(
            new FlowLayout(FlowLayout.CENTER, 12, 5)
        );

        navigationButtons.setOpaque(false);
        navigationButtons.add(btnPrevious);
        navigationButtons.add(btnLogin);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        panel.add(mainButtons, gbc);

        gbc.gridy = 9;
        panel.add(navigationButtons, gbc);

        JPanel card = new JPanel(new BorderLayout()) {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );

                g2.setColor(PLUM);

                g2.fillRoundRect(
                    0, 0, getWidth() - 1,
                    getHeight() - 1, 35, 35
                );

                g2.setColor(new Color(220, 177, 160));
                g2.setStroke(new BasicStroke(1.5f));

                g2.drawRoundRect(
                    1, 1, getWidth() - 3,
                    getHeight() - 3, 35, 35
                );

                g2.dispose();
                super.paintComponent(g);
            }
        };

        card.setOpaque(false);
        card.add(panel, BorderLayout.CENTER);

        background.add(card);
        setContentPane(background);

        addValidationListeners();

        btnRegister.addActionListener(e -> registerCustomer());

        btnReset.addActionListener(e -> resetFields());

        btnLogin.addActionListener(e -> {
            new Login().setVisible(true);
            dispose();
        });

        btnPrevious.addActionListener(e -> {
            new Welcome();
            dispose();
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JLabel createErrorLabel() {

        JLabel label = new JLabel(" ");
        label.setForeground(ERROR_RED);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));

        return label;
    }

    private void prepareField(JComponent field) {

        field.setFont(new Font("SansSerif", Font.PLAIN, 15));
        field.setPreferredSize(new Dimension(260, 36));
        field.setBackground(new Color(250, 245, 250));
        field.setForeground(new Color(55, 30, 55));

        normalBorders.put(field, field.getBorder());
    }

    private void addField(
            JPanel panel, GridBagConstraints gbc,
            String label, JComponent field, int row) {

        gbc.gridy = row;
        gbc.gridx = 0;

        JLabel lbl = new JLabel(label + ":");
        lbl.setForeground(LIGHT_TEXT);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 15));

        panel.add(lbl, gbc);

        prepareField(field);

        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private void addValidatedField(
            JPanel panel, GridBagConstraints gbc,
            String label, JComponent field,
            JLabel errorLabel, int row) {

        gbc.gridy = row;
        gbc.gridx = 0;

        JLabel lbl = new JLabel(label + ":");
        lbl.setForeground(LIGHT_TEXT);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 15));

        panel.add(lbl, gbc);

        prepareField(field);

        JPanel fieldPanel = new JPanel(new BorderLayout(0, 2));
        fieldPanel.setOpaque(false);

        fieldPanel.add(field, BorderLayout.CENTER);
        fieldPanel.add(errorLabel, BorderLayout.SOUTH);

        gbc.gridx = 1;
        panel.add(fieldPanel, gbc);
    }

    private void addPasswordField(
            JPanel panel, GridBagConstraints gbc, int row) {

        gbc.gridy = row;
        gbc.gridx = 0;

        JLabel lbl = new JLabel("Password:");
        lbl.setForeground(LIGHT_TEXT);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 15));

        panel.add(lbl, gbc);

        prepareField(txtPassword);

        chkShowPassword = new JCheckBox("Show Passwords");
        chkShowPassword.setOpaque(false);
        chkShowPassword.setForeground(LIGHT_TEXT);
        chkShowPassword.setFont(
            new Font("SansSerif", Font.PLAIN, 12)
        );

        chkShowPassword.addActionListener(e -> {

            if (chkShowPassword.isSelected()) {
                txtPassword.setEchoChar((char) 0);
                txtConfirmPassword.setEchoChar((char) 0);
            } else {
                txtPassword.setEchoChar(passwordEchoChar);
                txtConfirmPassword.setEchoChar(passwordEchoChar);
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        topPanel.setOpaque(false);

        topPanel.add(txtPassword, BorderLayout.CENTER);
        topPanel.add(chkShowPassword, BorderLayout.EAST);

        JPanel fieldPanel = new JPanel(new BorderLayout(0, 2));
        fieldPanel.setOpaque(false);

        fieldPanel.add(topPanel, BorderLayout.CENTER);
        fieldPanel.add(passwordError, BorderLayout.SOUTH);

        gbc.gridx = 1;
        panel.add(fieldPanel, gbc);
    }

    private void styleButton(JButton button) {

        button.setBackground(CINNAMON);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(
            new EmptyBorder(12, 22, 12, 22)
        );

        button.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
    }

    private void addValidationListeners() {

        addDocumentListener(txtEmail, this::validateEmail);
        addDocumentListener(txtPhone, this::validatePhone);
        addDocumentListener(txtPassword, this::validatePassword);
        addDocumentListener(
            txtConfirmPassword, this::validateConfirmPassword
        );
    }

    private void addDocumentListener(
            JTextField field, Runnable validation) {

        field.getDocument().addDocumentListener(
            new DocumentListener() {

                public void insertUpdate(DocumentEvent e) {
                    validation.run();
                }

                public void removeUpdate(DocumentEvent e) {
                    validation.run();
                }

                public void changedUpdate(DocumentEvent e) {
                    validation.run();
                }
            }
        );
    }

    private void setFieldError(
            JComponent field, JLabel label, String message) {

        if (message.isEmpty()) {

            field.setBorder(normalBorders.get(field));
            label.setText(" ");

        } else {

            field.setBorder(
                BorderFactory.createLineBorder(ERROR_RED, 2)
            );

            label.setText(message);
        }

        field.repaint();
    }

    private String getEmailError() {

        String email = txtEmail.getText().trim();

        if (email.isEmpty()) {
            return "Email address is required.";
        }

        if (!email.matches(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {

            return "Enter a valid email address.";
        }

        return "";
    }

    private String getPhoneError() {

        String phone = txtPhone.getText().trim();

        if (phone.isEmpty()) {
            return "Phone number is required.";
        }

        if (!phone.matches("[6-9][0-9]{9}")) {
            return "Enter a valid 10-digit phone number.";
        }

        return "";
    }

    private String getPasswordError() {

        String password = new String(txtPassword.getPassword());

        if (password.isEmpty()) {
            return "Password is required.";
        }

        if (!password.matches(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)"
            + "(?=.*[@#$%^&+=!]).{8,20}$")) {

            return "Use 8-20 characters, uppercase, lowercase, "
                + "number and special character.";
        }

        return "";
    }

    private String getConfirmPasswordError() {

        String password = new String(txtPassword.getPassword());
        String confirm = new String(
            txtConfirmPassword.getPassword()
        );

        if (confirm.isEmpty()) {
            return "Please confirm your password.";
        }

        if (!password.equals(confirm)) {
            return "Passwords do not match.";
        }

        return "";
    }

    private void validateEmail() {
        setFieldError(txtEmail, emailError, getEmailError());
    }

    private void validatePhone() {
        setFieldError(txtPhone, phoneError, getPhoneError());
    }

    private void validatePassword() {

        setFieldError(
            txtPassword, passwordError, getPasswordError()
        );

        validateConfirmPassword();
    }

    private void validateConfirmPassword() {

        setFieldError(
            txtConfirmPassword,
            confirmError,
            getConfirmPasswordError()
        );
    }

    private boolean validateRegistrationFields() {

        validateEmail();
        validatePhone();
        validatePassword();
        validateConfirmPassword();

        boolean valid =
            getEmailError().isEmpty()
            && getPhoneError().isEmpty()
            && getPasswordError().isEmpty()
            && getConfirmPasswordError().isEmpty();

        if (!valid) {
            return false;
        }

        if (txtFullName.getText().trim().isEmpty()
                || txtUsername.getText().trim().isEmpty()) {

            JOptionPane.showMessageDialog(
                this, "Please fill in all fields."
            );

            return false;
        }

        return true;
    }

    private void registerCustomer() {

        if (!validateRegistrationFields()) {
            return;
        }

        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();

        char[] passwordChars = txtPassword.getPassword();
        char[] confirmChars = txtConfirmPassword.getPassword();

        try {

            String password = new String(passwordChars);
            String confirmPassword = new String(confirmChars);

            if (!password.equals(confirmPassword)) {
                validateConfirmPassword();
                return;
            }

            String hashedPassword =
                Pass.hashPassword(password);

            String sql = "INSERT INTO users "
                + "(full_name, username, email, phone, "
                + "password_hash, role) "
                + "VALUES (?, ?, ?, ?, ?, 'CUSTOMER')";

            try (Connection con = Db.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, fullName);
                ps.setString(2, username);
                ps.setString(3, email);
                ps.setString(4, phone);
                ps.setString(5, hashedPassword);

                int result = ps.executeUpdate();

                if (result > 0) {

                    JOptionPane.showMessageDialog(
                        this,
                        "Registration Successful!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                    );

                    resetFields();
                    new Login().setVisible(true);
                    dispose();
                }
            }

        } catch (SQLIntegrityConstraintViolationException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Username, email or phone already exists."
            );

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Database error: " + ex.getMessage()
            );

        } finally {

            Arrays.fill(passwordChars, '\0');
            Arrays.fill(confirmChars, '\0');
        }
    }

    private void resetFields() {

        txtFullName.setText("");
        txtUsername.setText("");
        txtEmail.setText("");
        txtPhone.setText("");
        txtPassword.setText("");
        txtConfirmPassword.setText("");

        chkShowPassword.setSelected(false);

        txtPassword.setEchoChar(passwordEchoChar);
        txtConfirmPassword.setEchoChar(passwordEchoChar);

        setFieldError(txtEmail, emailError, "");
        setFieldError(txtPhone, phoneError, "");
        setFieldError(txtPassword, passwordError, "");
        setFieldError(txtConfirmPassword, confirmError, "");
    }

    class BackgroundPanel extends JPanel {

        private Image image;

        BackgroundPanel(String path) {

            java.net.URL url = getClass().getResource(path);

            if (url != null) {
                image = new ImageIcon(url).getImage();
            } else {
                System.err.println("Image not found: " + path);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            if (image != null) {

                g.drawImage(
                    image, 0, 0,
                    getWidth(), getHeight(), this
                );

            } else {

                g.setColor(ORCHID);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }
}
