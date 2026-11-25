import java.sql.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class Book extends JPanel {

    // SUDWAR theme
    private final Color plum = new Color(75, 35, 75);
    private final Color deepPlum = new Color(55, 20, 58);
    private final Color orchid = new Color(239, 225, 242);
    private final Color chocolate = new Color(83, 48, 42);
    private final Color white = Color.WHITE;
    private final Color textDark = new Color(55, 42, 58);

    private int customerId;
    private String customerName;

    private int currentStep = 1;

    private JPanel stepContent;
    private JPanel progressPanel;

    private JButton backButton;
    private JButton nextButton;
    private JTextField senderNameField;
    private JTextField senderPhoneField;
    private JTextField senderEmailField;
    private JTextField pickupAddressField;
    private JTextField pickupPinField;
 // Receiver details
    private JTextField receiverNameField;
    private JTextField receiverPhoneField;
    private JTextField receiverEmailField;
    private JTextArea receiverAddressArea;
    private JTextField receiverPinField;

    // Package details
    private JComboBox<String> packageCategory;
    private JTextArea packageDescription;
    private JTextField weightField;
    private JTextField lengthField;
    private JTextField widthField;
    private JTextField heightField;
    private JCheckBox fragileCheckBox;

    // Delivery and payment
    private JComboBox<String> deliveryType;
    private JComboBox<String> paymentMethod;
    private JTextField pickupDateField;

    // Booking calculation
    private double estimatedCharges = 0.0;
    private JTextArea reviewArea;

    public Book(int customerId, String customerName) {

        this.customerId = customerId;
        this.customerName = customerName;

        setLayout(new BorderLayout());
        setBackground(orchid);

        createBookingPage();
    }

    private void createBookingPage() {

        // Main heading
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(
                heading, BoxLayout.Y_AXIS));
        heading.setBackground(white);
        heading.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("BOOK A SHIPMENT");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(plum);

        JLabel subtitle = new JLabel(
                "Complete the steps to arrange your delivery.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(textDark);

        heading.add(title);
        heading.add(Box.createVerticalStrut(6));
        heading.add(subtitle);

        add(heading, BorderLayout.NORTH);

        // Main centre area
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(orchid);
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        progressPanel = new JPanel();
        progressPanel.setBackground(orchid);

        stepContent = new JPanel(new BorderLayout());
        stepContent.setBackground(white);
        stepContent.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 205, 225)),
                new EmptyBorder(20, 20, 20, 20)));

        mainPanel.add(progressPanel, BorderLayout.NORTH);
        mainPanel.add(stepContent, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        // Bottom navigation
        JPanel navigation = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 15, 12));
        navigation.setBackground(white);

        backButton = new JButton("Back");
        nextButton = new JButton("Next");

        styleButton(backButton, chocolate);
        styleButton(nextButton, plum);

        backButton.addActionListener(e -> previousStep());
        nextButton.addActionListener(e -> nextStep());

        navigation.add(backButton);
        navigation.add(nextButton);

        add(navigation, BorderLayout.SOUTH);

        showCurrentStep();
    }

    private void styleButton(JButton button, Color colour) {

        button.setBackground(colour);
        button.setForeground(white);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 25, 10, 25));
        button.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    private void showCurrentStep() {

        String[] steps = {
            "Sender Details",
            "Receiver Details",
            "Package Details",
            "Delivery & Payment",
            "Review & Confirm"
        };

        // Progress indicator
        progressPanel.removeAll();
        progressPanel.setLayout(new GridLayout(1, 5, 8, 8));

        for (int i = 0; i < steps.length; i++) {

            JPanel item = new JPanel(new BorderLayout());
            item.setBackground(
                i + 1 == currentStep ? plum : white
            );

            JLabel label = new JLabel(
                (i + 1) + ". " + steps[i],
                SwingConstants.CENTER
            );

            label.setFont(new Font(
                "SansSerif", Font.BOLD, 11
            ));

            label.setForeground(
                i + 1 == currentStep ? white : textDark
            );

            item.add(label, BorderLayout.CENTER);
            item.setBorder(new EmptyBorder(12, 5, 12, 5));

            progressPanel.add(item);
        }

        // Current step content
        stepContent.removeAll();

        JLabel heading = new JLabel(steps[currentStep - 1]);
        heading.setFont(new Font(
            "SansSerif", Font.BOLD, 22
        ));
        heading.setForeground(plum);

        JLabel description = new JLabel(
            "Step " + currentStep + " of 5"
        );
        description.setForeground(textDark);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(white);

        top.add(heading);
        top.add(Box.createVerticalStrut(8));
        top.add(description);

        stepContent.add(top, BorderLayout.NORTH);
        if (currentStep == 1) {
            showSenderDetails();
        } else if (currentStep == 2) {
            showReceiverDetails();
        } else if (currentStep == 3) {
            showPackageDetails();
        } else if (currentStep == 4) {
            showDeliveryPayment();
        } else if (currentStep == 5) {
            showBookingReview();
        }


        backButton.setEnabled(currentStep > 1);

        nextButton.setText(
            currentStep == 5 ? "Review Booking" : "Next"
        );

        progressPanel.revalidate();
        progressPanel.repaint();

        stepContent.revalidate();
        stepContent.repaint();
    }



    private void nextStep() {
    	if(!validateCurrentStep()) {
    		return;
    	}
        if (currentStep < 5) {
            currentStep++;
            showCurrentStep();
        }
    }

    private void previousStep() {
        if (currentStep > 1) {
            currentStep--;
            showCurrentStep();
        }
    }
    private void showSenderDetails() {

        JPanel form = new JPanel(new GridLayout(0, 2, 15, 15));
        form.setBackground(white);

        senderNameField = new JTextField();
        senderPhoneField = new JTextField();
        senderEmailField = new JTextField();
        pickupAddressField = new JTextField();
        pickupPinField = new JTextField();

        senderNameField.setEditable(false);
        senderPhoneField.setEditable(false);
        senderEmailField.setEditable(false);

        addFormField(form, "Customer Name", senderNameField);
        addFormField(form, "Phone Number", senderPhoneField);
        addFormField(form, "Email Address", senderEmailField);
        addFormField(form, "Pickup Address", pickupAddressField);
        addFormField(form, "Pickup PIN Code", pickupPinField);

        JButton profileButton = new JButton("Use Profile Details");
        styleButton(profileButton, chocolate);

        profileButton.addActionListener(e -> loadSenderDetails());

        JPanel bottom = new JPanel(
            new FlowLayout(FlowLayout.LEFT)
        );
        bottom.setBackground(white);
        bottom.add(profileButton);

        JPanel container = new JPanel(new BorderLayout(10, 20));
        container.setBackground(white);
        container.add(form, BorderLayout.CENTER);
        container.add(bottom, BorderLayout.SOUTH);

        stepContent.add(container, BorderLayout.CENTER);

        loadSenderDetails();
    }

    private void addFormField(JPanel panel, String label, Component field) {

        JLabel heading = new JLabel(label);
        heading.setFont(new Font("SansSerif", Font.BOLD, 13));
        heading.setForeground(textDark);

        if (field instanceof JComponent) {
            ((JComponent) field).setPreferredSize(
                new Dimension(200, 38)
            );
        }

        panel.add(heading);
        panel.add(field);
    }
    private void loadSenderDetails() {

        String sql =
            "SELECT full_name, phone, email FROM users WHERE user_id = ?";

        try (java.sql.Connection con = Db.getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (java.sql.ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    senderNameField.setText(rs.getString("full_name"));
                    senderPhoneField.setText(rs.getString("phone"));
                    senderEmailField.setText(rs.getString("email"));
                } else {
                    JOptionPane.showMessageDialog(
                        this, "Customer profile not found."
                    );
                }
            }

        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Unable to load customer details: " + ex.getMessage()
            );
        }
    }
    private void showReceiverDetails() {

        JPanel form = new JPanel(new GridLayout(0, 2, 15, 15));
        form.setBackground(white);

        receiverNameField = new JTextField();
        receiverPhoneField = new JTextField();
        receiverEmailField = new JTextField();
        receiverAddressArea = new JTextArea(3, 20);
        receiverPinField = new JTextField();

        receiverAddressArea.setLineWrap(true);
        receiverAddressArea.setWrapStyleWord(true);

        addFormField(form, "Receiver Name", receiverNameField);
        addFormField(form, "Phone Number", receiverPhoneField);
        addFormField(form, "Email Address", receiverEmailField);
        addFormField(form, "Delivery Address",
                new JScrollPane(receiverAddressArea));
        addFormField(form, "Delivery PIN Code", receiverPinField);

        stepContent.add(form, BorderLayout.CENTER);
    }
    private void showPackageDetails() {

        JPanel form = new JPanel(new GridLayout(0, 2, 15, 15));
        form.setBackground(white);

        packageCategory = new JComboBox<>(new String[]{
            "Document",
            "Parcel",
            "Electronics",
            "Other"
        });

        packageDescription = new JTextArea(3, 20);
        packageDescription.setLineWrap(true);
        packageDescription.setWrapStyleWord(true);

        weightField = new JTextField();
        lengthField = new JTextField();
        widthField = new JTextField();
        heightField = new JTextField();

        fragileCheckBox = new JCheckBox("Handle as fragile");
        fragileCheckBox.setBackground(white);

        addFormField(form, "Package Category", packageCategory);
        addFormField(form, "Description",
                new JScrollPane(packageDescription));
        addFormField(form, "Weight (kg)", weightField);
        addFormField(form, "Length (cm)", lengthField);
        addFormField(form, "Width (cm)", widthField);
        addFormField(form, "Height (cm)", heightField);
        addFormField(form, "Special Handling", fragileCheckBox);

        stepContent.add(form, BorderLayout.CENTER);
    }
    private void showDeliveryPayment() {

        JPanel form = new JPanel(new GridLayout(0, 2, 15, 15));
        form.setBackground(white);

        deliveryType = new JComboBox<>(new String[]{
            "Standard Delivery",
            "Express Delivery"
        });

        paymentMethod = new JComboBox<>(new String[]{
            "Cash on Delivery",
            "Online Payment"
        });

        pickupDateField = new DatePick();
        ((DatePick) pickupDateField).setMinDate(java.time.LocalDate.now());

        addFormField(form, "Delivery Type", deliveryType);
        addFormField(form, "Payment Method", paymentMethod);
        addFormField(form, "Preferred Pickup Date (YYYY-MM-DD)",
                pickupDateField);

        JButton calculateButton =
                new JButton("Calculate Charges");

        styleButton(calculateButton, chocolate);

        calculateButton.addActionListener(e -> calculateCharges());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setBackground(white);
        bottom.add(calculateButton);

        JPanel container = new JPanel(new BorderLayout(10, 15));
        container.setBackground(white);
        container.add(form, BorderLayout.CENTER);
        container.add(bottom, BorderLayout.SOUTH);

        stepContent.add(container, BorderLayout.CENTER);
    }
    private void calculateCharges() {

        try {
            double weight = Double.parseDouble(
                    weightField.getText().trim());

            if (!Double.isFinite(weight) || weight <= 0) {
                throw new NumberFormatException();
            }

            double baseCharge = 50.0;
            double weightCharge = weight * 20.0;

            double deliveryCharge =
                deliveryType.getSelectedIndex() == 1 ? 50.0 : 0.0;

            estimatedCharges =
                baseCharge + weightCharge + deliveryCharge;

            JOptionPane.showMessageDialog(
                this,
                String.format(
                    "Estimated Charges: ₹%.2f",
                    estimatedCharges
                ),
                "Charge Calculation",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (NumberFormatException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Please enter a valid package weight."
            );
        }
    }
    private void showBookingReview() {

        reviewArea = new JTextArea();
        reviewArea.setEditable(false);
        reviewArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        reviewArea.setLineWrap(true);
        reviewArea.setWrapStyleWord(true);
        reviewArea.setBackground(white);

        String summary =
            " SUDWAR BOOKING SUMMARY\n\n" +

            "SENDER DETAILS\n" +
            "Name: " + senderNameField.getText() + "\n" +
            "Phone: " + senderPhoneField.getText() + "\n" +
            "Pickup Address: " + pickupAddressField.getText() + "\n\n" +

            "RECEIVER DETAILS\n" +
            "Name: " + receiverNameField.getText() + "\n" +
            "Phone: " + receiverPhoneField.getText() + "\n" +
            "Address: " + receiverAddressArea.getText() + "\n" +
            "PIN Code: " + receiverPinField.getText() + "\n\n" +

            "PACKAGE DETAILS\n" +
            "Category: " + packageCategory.getSelectedItem() + "\n" +
            "Description: " + packageDescription.getText() + "\n" +
            "Weight: " + weightField.getText() + " kg\n" +
            "Fragile: " + (fragileCheckBox.isSelected() ? "Yes" : "No") + "\n\n" +

            "DELIVERY AND PAYMENT\n" +
            "Delivery: " + deliveryType.getSelectedItem() + "\n" +
            "Payment: " + paymentMethod.getSelectedItem() + "\n" +
            "Pickup Date: " + pickupDateField.getText() + "\n\n" +

            "Estimated Charges: ₹" +
            String.format("%.2f", estimatedCharges);

        reviewArea.setText(summary);
        reviewArea.setCaretPosition(0);

        stepContent.add(new JScrollPane(reviewArea), BorderLayout.CENTER);

        JButton confirmButton = new JButton("Confirm Booking");
        styleButton(confirmButton, plum);

        confirmButton.addActionListener(e -> confirmBooking());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(white);
        bottom.add(confirmButton);

        stepContent.add(bottom, BorderLayout.SOUTH);
    }
    private void confirmBooking() {

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to confirm this booking?",
            "Confirm Shipment",
            JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        String trackingNumber =
            "ST" + System.currentTimeMillis();

        String sql = "INSERT INTO shipments "
            + "(tracking_number, customer_id, sender_address, "
            + "receiver_name, receiver_phone, receiver_address, "
            + "parcel_description, pickup_date, delivery_status) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'BOOKED')";

        String description =
            packageCategory.getSelectedItem().toString()
            + " - " + packageDescription.getText().trim()
            + " - Weight: " + weightField.getText().trim() + " kg"
            + " - Dimensions: " + lengthField.getText().trim()
            + "x" + widthField.getText().trim()
            + "x" + heightField.getText().trim()
            + " cm"
            + " - Fragile: " + fragileCheckBox.isSelected();

        try (Connection con = Db.getConnection()) {

            con.setAutoCommit(false);

            try {

                int shipmentId;

                try (PreparedStatement ps =
                    con.prepareStatement(
                        sql,
                        java.sql.Statement.RETURN_GENERATED_KEYS
                    )) {

                    ps.setString(1, trackingNumber);
                    ps.setInt(2, customerId);
                    ps.setString(3, pickupAddressField.getText().trim());
                    ps.setString(4, receiverNameField.getText().trim());
                    ps.setString(5, receiverPhoneField.getText().trim());
                    ps.setString(6, receiverAddressArea.getText().trim());
                    ps.setString(7, description);
                    ps.setDate(
                        8,
                        java.sql.Date.valueOf(
                            java.time.LocalDate.parse(
                                pickupDateField.getText().trim()
                            )
                        )
                    );

                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                "Could not generate shipment ID."
                            );
                        }

                        shipmentId = rs.getInt(1);
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO tracking_logs "
                    + "(shipment_id, status, location, remarks, updated_by) "
                    + "VALUES (?, 'BOOKED', ?, ?, ?)")) {

                    ps.setInt(1, shipmentId);
                    ps.setString(2, pickupAddressField.getText().trim());
                    ps.setString(3, "Shipment booked successfully.");
                    ps.setInt(4, customerId);

                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO payments "
                    + "(shipment_id, amount, payment_method, payment_status) "
                    + "VALUES (?, ?, ?, 'PENDING')")) {

                    ps.setInt(1, shipmentId);
                    ps.setBigDecimal(
                        2,
                        java.math.BigDecimal.valueOf(estimatedCharges)
                    );
                    ps.setString(
                        3,
                        paymentMethod.getSelectedItem().toString()
                    );

                    ps.executeUpdate();
                }

                con.commit();

                JOptionPane.showMessageDialog(
                    this,
                    "Booking successful!\n\n"
                    + "Tracking Number: " + trackingNumber
                    + "\nShipment ID: " + shipmentId
                    + "\nAmount: ₹" + estimatedCharges
                    + "\nPayment Status: PENDING",
                    "Shipment Confirmed",
                    JOptionPane.INFORMATION_MESSAGE
                );

            } catch (Exception ex) {
                con.rollback();
                throw ex;
            }

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                this,
                "Booking failed: " + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );

            ex.printStackTrace();
        }
    }

    private boolean validateCurrentStep() {

        if (currentStep == 1) {

            if (pickupAddressField.getText().trim().isEmpty()
                    || !pickupPinField.getText().trim()
                        .matches("\\d{6}")) {

                JOptionPane.showMessageDialog(
                    this,
                    "Enter your pickup address and valid 6-digit PIN code."
                );
                return false;
            }
        }

        if (currentStep == 2) {

            if (receiverNameField.getText().trim().isEmpty()
                    || !receiverPhoneField.getText().trim()
                        .matches("\\d{10}")
                    || receiverAddressArea.getText().trim().isEmpty()
                    || !receiverPinField.getText().trim()
                        .matches("\\d{6}")) {

                JOptionPane.showMessageDialog(
                    this,
                    "Enter the receiver's name, valid 10-digit phone, " +
                    "complete address and 6-digit PIN code."
                );
                return false;
            }
        }

        if (currentStep == 3) {

            try {
                double weight = Double.parseDouble(
                        weightField.getText().trim());

                if (!Double.isFinite(weight) || weight <= 0) {
                    throw new NumberFormatException();
                }

                if (packageDescription.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(
                        this, "Enter the package description."
                    );
                    return false;
                }

            } catch (NumberFormatException ex) {

                JOptionPane.showMessageDialog(
                    this, "Enter a valid positive package weight."
                );
                return false;
            }
        }

        if (currentStep == 4) {

            try {
                java.time.LocalDate date =
                    java.time.LocalDate.parse(
                        pickupDateField.getText().trim()
                    );

                if (date.isBefore(java.time.LocalDate.now())) {
                    JOptionPane.showMessageDialog(
                        this, "Pickup date cannot be in the past."
                    );
                    return false;
                }

            } catch (java.time.format.DateTimeParseException ex) {

                JOptionPane.showMessageDialog(
                    this, "Enter pickup date as YYYY-MM-DD."
                );
                return false;
            }

            if (estimatedCharges <= 0) {
                JOptionPane.showMessageDialog(
                    this, "Please calculate the charges before continuing."
                );
                return false;
            }
        }

        return true;
    }


}
