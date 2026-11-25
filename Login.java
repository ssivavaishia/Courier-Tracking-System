import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.sql.*;
import java.util.Arrays;

public class Login extends JFrame {

    static {
        Skin.install();
    }


    private JTextField txtEmail;
    private JPasswordField txtPassword;

    private JCheckBox chkRemember;
    private JCheckBox chkShowPassword;

    private JLabel emailError;
    private JLabel passwordError;

    private InputShell emailShell;
    private InputShell passwordShell;

    private char passwordEchoChar;

    private final Color forest = new Color(43, 83, 65);
    private final Color darkForest = new Color(32, 65, 49);
    private final Color cream = new Color(249, 246, 237);
    private final Color caramel = new Color(178, 126, 77);
    private final Color brown = new Color(65, 54, 45);
    private final Color muted = new Color(125, 113, 101);
    private final Color red = new Color(190, 55, 55);

    private boolean emailTouched = false;
    private boolean passwordTouched = false;

    public Login() {

        setTitle("SUDWAR - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(900, 650));

        BackgroundPanel background =
                new BackgroundPanel("/resources/login.png");

        background.setLayout(new GridBagLayout());

        JPanel card = new RoundedCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(cream);
        card.setBorder(new EmptyBorder(38, 45, 38, 45));
        card.setPreferredSize(new Dimension(470, 650));

        // BRAND SECTION

        JPanel brandPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 12, 0));

        brandPanel.setOpaque(false);
        brandPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Reserved blank logo space: 64 x 64 pixels

        JPanel logoSpace = new JPanel();
        logoSpace.setPreferredSize(new Dimension(64, 64));
        logoSpace.setMinimumSize(new Dimension(64, 64));
        logoSpace.setMaximumSize(new Dimension(64, 64));
        logoSpace.setOpaque(false);

        JLabel brand = new JLabel("SUDWAR");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 34));
        brand.setForeground(forest);

        brandPanel.add(logoSpace);
        brandPanel.add(brand);

        JLabel brandLine = new JLabel("COURIER MANAGEMENT");
        brandLine.setFont(new Font("Segoe UI", Font.BOLD, 10));
        brandLine.setForeground(caramel);
        brandLine.setAlignmentX(Component.CENTER_ALIGNMENT);

        // HEADING AND SUBTITLE

        JLabel heading = new JLabel("Welcome Back");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 27));
        heading.setForeground(brown);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "Sign in to continue your journey");

        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(muted);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(brandPanel);
        card.add(Box.createVerticalStrut(3));
        card.add(brandLine);
        card.add(Box.createVerticalStrut(25));
        card.add(heading);
        card.add(Box.createVerticalStrut(7));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(30));

        // EMAIL FIELD

        addFieldLabel(card, "Email Address");

        txtEmail = new JTextField();
        emailShell = createInputShell("✉", txtEmail);

        emailError = createErrorLabel();

        card.add(emailShell);
        card.add(emailError);
        card.add(Box.createVerticalStrut(18));

        // PASSWORD FIELD

        addFieldLabel(card, "Password");

        txtPassword = new JPasswordField();
        passwordEchoChar = txtPassword.getEchoChar();

        passwordShell = createInputShell("●", txtPassword);

        passwordError = createErrorLabel();

        card.add(passwordShell);
        card.add(passwordError);
        card.add(Box.createVerticalStrut(12));

        // REMEMBER ME AND SHOW PASSWORD

        JPanel options = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 0));

        options.setOpaque(false);
        options.setAlignmentX(Component.LEFT_ALIGNMENT);

        chkRemember = new JCheckBox("Remember me");
        chkRemember.setOpaque(false);
        chkRemember.setForeground(brown);
        chkRemember.setFont(
                new Font("Segoe UI", Font.PLAIN, 12));

        chkShowPassword = new JCheckBox("Show password");
        chkShowPassword.setOpaque(false);
        chkShowPassword.setForeground(brown);
        chkShowPassword.setFont(
                new Font("Segoe UI", Font.PLAIN, 12));

        options.add(chkRemember);
        options.add(chkShowPassword);

        card.add(options);
        card.add(Box.createVerticalStrut(22));

        // LOGIN BUTTON

        JButton btnLogin = new JButton("SIGN IN");
        styleButton(btnLogin, forest, Color.WHITE);
        card.add(btnLogin);

        card.add(Box.createVerticalStrut(8));

        // FORGOT PASSWORD

        JButton btnForgot = new JButton("Forgot Password?");
        styleTextButton(btnForgot);
        card.add(btnForgot);

        card.add(Box.createVerticalStrut(12));

        JLabel accountLabel =
                new JLabel("Don't have an account?");

        accountLabel.setFont(
                new Font("Segoe UI", Font.PLAIN, 13));

        accountLabel.setForeground(muted);
        accountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(accountLabel);
        card.add(Box.createVerticalStrut(8));

        // SIGN UP BUTTON

        JButton btnSignup = new JButton("CREATE ACCOUNT");
        styleButton(btnSignup, caramel, Color.WHITE);

        card.add(btnSignup);

        background.add(card);
        setContentPane(background);

        // EMAIL VALIDATION WHILE TYPING

        txtEmail.getDocument().addDocumentListener(
                new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                emailTouched = true;
                validateEmail();
            }

            public void removeUpdate(DocumentEvent e) {
                if (emailTouched) validateEmail();
            }

            public void changedUpdate(DocumentEvent e) {
                validateEmail();
            }
        });

        // PASSWORD VALIDATION WHILE TYPING

        txtPassword.getDocument().addDocumentListener(
                new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                passwordTouched = true;
                validatePasswordField();
            }

            public void removeUpdate(DocumentEvent e) {
                if (passwordTouched) validatePasswordField();
            }

            public void changedUpdate(DocumentEvent e) {
                validatePasswordField();
            }
        });

        // FOCUS EFFECTS

        addFocusEffects(txtEmail, emailShell);
        addFocusEffects(txtPassword, passwordShell);

        // SHOW / HIDE PASSWORD

        chkShowPassword.addActionListener(e -> {

            if (chkShowPassword.isSelected()) {
                txtPassword.setEchoChar((char) 0);
            } else {
                txtPassword.setEchoChar(passwordEchoChar);
            }
        });

        // BUTTON ACTIONS

        btnLogin.addActionListener(e -> loginUser());

        btnSignup.addActionListener(e -> {
            new Register().setVisible(true);
            dispose();
        });

        btnForgot.addActionListener(e ->
                JOptionPane.showMessageDialog(
                        this,
                        "Please contact the administrator "
                        + "for password assistance.",
                        "Password Assistance",
                        JOptionPane.INFORMATION_MESSAGE
                )
        );

        setLocationRelativeTo(null);
    }

    // FIELD LABEL

    private void addFieldLabel(JPanel panel, String text) {

        JLabel label = new JLabel(text);

        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(brown);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(8));
    }

    // ERROR LABEL

    private JLabel createErrorLabel() {

        JLabel label = new JLabel(" ");

        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(red);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    // INPUT SHELL

    private InputShell createInputShell(
            String symbol, JTextField field) {

        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setForeground(brown);
        field.setCaretColor(forest);
        field.setOpaque(false);
        field.setBorder(new EmptyBorder(10, 5, 10, 10));

        InputShell shell = new InputShell(symbol, field);

        shell.setAlignmentX(Component.LEFT_ALIGNMENT);
        shell.setMaximumSize(new Dimension(380, 50));
        shell.setPreferredSize(new Dimension(380, 50));

        return shell;
    }

    // FOCUS EFFECTS

    private void addFocusEffects(
            JTextField field, InputShell shell) {

        field.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {
                shell.setFocused(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                shell.setFocused(false);
            }
        });
    }

    // BUTTON STYLING

    private void styleButton(
            JButton button, Color background, Color foreground) {

        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(background);
        button.setForeground(foreground);

        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(380, 46));
        button.setPreferredSize(new Dimension(380, 46));

        button.setBorder(
                new EmptyBorder(10, 20, 10, 20));

        button.addMouseListener(
                new java.awt.event.MouseAdapter() {

            @Override
            public void mouseEntered(
                    java.awt.event.MouseEvent e) {

                if (background.equals(caramel)) {
                    button.setBackground(new Color(148, 99, 57));
                } else {
                    button.setBackground(darkForest);
                }
            }

            @Override
            public void mouseExited(
                    java.awt.event.MouseEvent e) {

                button.setBackground(background);
            }
        });
    }

    // TEXT BUTTON STYLING

    private void styleTextButton(JButton button) {

        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(forest);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.setAlignmentX(Component.CENTER_ALIGNMENT);
    }

    // EMAIL VALIDATION

    private boolean validateEmail() {

        String email = txtEmail.getText().trim();

        if (email.isEmpty()) {
            emailError.setText("Email address is required.");
            emailShell.setError(true);
            return false;
        }

        String emailPattern =
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

        if (!email.matches(emailPattern)) {
            emailError.setText(
                    "Please enter a valid email address.");

            emailShell.setError(true);
            return false;
        }

        emailError.setText(" ");
        emailShell.setError(false);

        return true;
    }

    // PASSWORD VALIDATION

    private boolean validatePasswordField() {

        char[] passwordChars = txtPassword.getPassword();

        boolean empty = passwordChars.length == 0;

        Arrays.fill(passwordChars, '\0');

        if (empty) {
            passwordError.setText("Password is required.");
            passwordShell.setError(true);
            return false;
        }

        passwordError.setText(" ");
        passwordShell.setError(false);

        return true;
    }

    // VALIDATE LOGIN FIELDS

    private boolean validateLoginFields() {

        emailTouched = true;
        passwordTouched = true;

        boolean emailValid = validateEmail();
        boolean passwordValid = validatePasswordField();

        return emailValid && passwordValid;
    }

    // LOGIN FUNCTION

    private void loginUser() {

        if (!validateLoginFields()) {
            return;
        }

        String email = txtEmail.getText().trim();

        char[] passwordChars = txtPassword.getPassword();
        String password = new String(passwordChars);

        String sql =
                "SELECT user_id, full_name, password_hash, role "
                + "FROM users "
                + "WHERE email = ? AND account_status = 'ACTIVE'";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    String storedHash =
                            rs.getString("password_hash");

                    if (Pass.verifyPassword(
                            password, storedHash)) {

                        String role = rs.getString("role");
                        int userId = rs.getInt("user_id");
                        String fullName = rs.getString("full_name");

                        JOptionPane.showMessageDialog(
                                this,
                                "Login Successful! Welcome " + fullName
                        );

                        openDashboard(role, userId, fullName);

                    } else {

                        JOptionPane.showMessageDialog(
                                this,
                                "Incorrect email or password.",
                                "Login Failed",
                                JOptionPane.WARNING_MESSAGE
                        );
                    }

                } else {

                    JOptionPane.showMessageDialog(
                            this,
                            "Incorrect email or password, "
                            + "or the account is inactive.",
                            "Login Failed",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Database error: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );

        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }

    // DASHBOARD NAVIGATION

    private void openDashboard(
            String role, int userId, String fullName) {

        if (role == null) {

            JOptionPane.showMessageDialog(
                    this, "User role is missing.");

            return;
        }

        switch (role.toUpperCase()) {

            case "CUSTOMER":
                new CustHome(
                        userId, fullName).setVisible(true);
                break;

            case "ADMIN":
                new AdminHome(
                        userId, fullName).setVisible(true);
                break;

            case "DELIVERY":
                new DelHome(
                        userId, fullName).setVisible(true);
                break;

            default:

                JOptionPane.showMessageDialog(
                        this, "Invalid user role.");

                return;
        }

        dispose();
    }

    // ROUNDED INPUT FIELD

    class InputShell extends JPanel {

        private boolean focused = false;
        private boolean error = false;

        InputShell(String symbol, JTextField field) {

            setLayout(new BorderLayout(10, 0));
            setOpaque(false);

            JLabel icon = new JLabel(symbol);

            icon.setFont(
                    new Font("Segoe UI Symbol", Font.PLAIN, 17));

            icon.setForeground(forest);
            icon.setBorder(new EmptyBorder(0, 14, 0, 0));

            add(icon, BorderLayout.WEST);
            add(field, BorderLayout.CENTER);
        }

        void setFocused(boolean value) {
            focused = value;
            repaint();
        }

        void setError(boolean value) {
            error = value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Color.WHITE);

            g2.fillRoundRect(
                    0, 0, getWidth() - 1,
                    getHeight() - 1, 18, 18);

            Color borderColor =
                    new Color(220, 213, 199);

            if (error) {
                borderColor = red;
            } else if (focused) {
                borderColor = forest;
            }

            g2.setColor(borderColor);

            g2.setStroke(new BasicStroke(
                    focused || error ? 2f : 1f));

            g2.drawRoundRect(
                    1, 1, getWidth() - 3,
                    getHeight() - 3, 18, 18);

            g2.dispose();
        }
    }

    // ROUNDED LOGIN CARD

    class RoundedCard extends JPanel {

        RoundedCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(
                    cream.getRed(),
                    cream.getGreen(),
                    cream.getBlue(),
                    245));

            g2.fillRoundRect(
                    0, 0, getWidth(),
                    getHeight(), 32, 32);

            g2.setColor(new Color(
                    caramel.getRed(),
                    caramel.getGreen(),
                    caramel.getBlue(),
                    120));

            g2.setStroke(new BasicStroke(1.2f));

            g2.drawRoundRect(
                    1, 1, getWidth() - 3,
                    getHeight() - 3, 32, 32);

            g2.dispose();
        }
    }

    // BACKGROUND IMAGE

    class BackgroundPanel extends JPanel {

        private Image image;

        BackgroundPanel(String path) {

            java.net.URL url = getClass().getResource(path);

            if (url != null) {
                image = new ImageIcon(url).getImage();
            } else {
                System.err.println("Image not found: " + path);
                setBackground(cream);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            if (image != null) {

                g.drawImage(
                        image, 0, 0,
                        getWidth(), getHeight(), this);
            }
        }
    }

    // MAIN METHOD

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() ->
                new Login().setVisible(true));
    }
}
