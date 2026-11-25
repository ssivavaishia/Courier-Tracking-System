import javax.swing.*;
import java.awt.*;
import java.time.YearMonth;
import java.text.DecimalFormat;

/**
 * Online payment window shown when the customer books with "Online Payment":
 * UPI, Card or Net Banking, each with live validation. This is a DEMO
 * gateway: no money is charged, a successful form simply marks the payment as paid.
 */
public final class PayDialog {

    private PayDialog() { }

    /** Returns "Online Payment - UPI / Card / NetBanking", or null when the customer pays later. */
    public static String collect(Component parent, double amount, String tracking) {

        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dlg = new JDialog(owner, "Online Payment", Dialog.ModalityType.APPLICATION_MODAL);

        final String[] result = {null};

        DecimalFormat money = new DecimalFormat("#,##0.00");

        // ---- UPI
        JTextField upi = new JTextField();
        JLabel upiErr = err();
        JPanel upiTab = form(
            "UPI ID", upi, upiErr, "Example: name@okbank");

        // ---- Card
        JTextField cardName = new JTextField();
        JTextField cardNo = new JTextField();
        JTextField expiry = new JTextField();
        JTextField cvv = new JTextField();
        JLabel cardErr = err();

        JPanel cardTab = new JPanel(new GridLayout(0, 1, 0, 6));
        cardTab.setOpaque(false);
        cardTab.add(label("Name on card"));
        cardTab.add(cardName);
        cardTab.add(label("Card number (16 digits)"));
        cardTab.add(cardNo);

        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
        row.setOpaque(false);
        JPanel a = new JPanel(new GridLayout(0, 1, 0, 6));
        a.setOpaque(false);
        a.add(label("Expiry (MM/YY)"));
        a.add(expiry);
        JPanel b = new JPanel(new GridLayout(0, 1, 0, 6));
        b.setOpaque(false);
        b.add(label("CVV (3 digits)"));
        b.add(cvv);
        row.add(a);
        row.add(b);
        cardTab.add(row);
        cardTab.add(cardErr);

        // ---- Net banking
        JComboBox<String> bank = new JComboBox<>(new String[]{
            "State Bank of India", "HDFC Bank", "ICICI Bank", "Axis Bank",
            "Kotak Mahindra Bank", "Canara Bank", "Bank of Baroda", "Punjab National Bank"});
        JPanel bankTab = new JPanel(new GridLayout(0, 1, 0, 6));
        bankTab.setOpaque(false);
        bankTab.add(label("Choose your bank"));
        bankTab.add(bank);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("UPI", wrap(upiTab));
        tabs.addTab("Card", wrap(cardTab));
        tabs.addTab("Net Banking", wrap(bankTab));

        // ---- header and buttons
        JLabel head = new JLabel("Pay \u20B9 " + money.format(amount));
        head.setFont(Theme.font(Font.BOLD, 22));
        head.setForeground(Theme.TXT);

        JLabel sub = new JLabel("Shipment " + tracking + "   (demo payment, no real money is charged)");
        sub.setFont(Theme.font(Font.PLAIN, 12));
        sub.setForeground(Theme.DIM);

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 2));
        top.setOpaque(false);
        top.add(head);
        top.add(sub);

        JButton pay = new JButton("Pay \u20B9 " + money.format(amount));
        pay.setBackground(Theme.RED);
        pay.setForeground(Color.WHITE);
        pay.setFont(Theme.font(Font.BOLD, 14));
        pay.setFocusPainted(false);
        pay.setBorder(BorderFactory.createEmptyBorder(11, 26, 11, 26));

        JButton later = new JButton("Pay later");
        later.setBackground(new Color(34, 47, 86));
        later.setForeground(Color.WHITE);
        later.setFont(Theme.font(Font.BOLD, 13));
        later.setFocusPainted(false);
        later.setBorder(BorderFactory.createEmptyBorder(11, 22, 11, 22));
        later.addActionListener(e -> dlg.dispose());

        pay.addActionListener(e -> {

            String error = null;
            String method = null;

            switch (tabs.getSelectedIndex()) {

                case 0:
                    if (!upi.getText().trim().matches("^[a-zA-Z0-9._-]{2,}@[a-zA-Z]{2,}$")) {
                        upiErr.setText("Enter a valid UPI ID, for example name@okbank.");
                        upi.putClientProperty("sud.error", "Invalid UPI ID");
                        upi.repaint();
                        return;
                    }
                    method = "Online Payment - UPI";
                    break;

                case 1:
                    error = checkCard(cardName.getText(), cardNo.getText(),
                                      expiry.getText(), cvv.getText());
                    if (error != null) {
                        cardErr.setText(error);
                        return;
                    }
                    method = "Online Payment - Card";
                    break;

                default:
                    method = "Online Payment - NetBanking";
            }

            result[0] = method;
            dlg.dispose();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(later);
        buttons.add(pay);

        JPanel box = new JPanel(new BorderLayout(0, 14));
        box.setBackground(Theme.PANEL);
        box.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));
        box.add(top, BorderLayout.NORTH);
        box.add(tabs, BorderLayout.CENTER);
        box.add(buttons, BorderLayout.SOUTH);

        dlg.setContentPane(box);
        dlg.setSize(470, 430);
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);

        return result[0];
    }

    private static String checkCard(String name, String no, String exp, String cvv) {

        if (name.trim().length() < 3) return "Enter the name on the card.";

        if (!no.replace(" ", "").matches("[0-9]{16}")) return "Card number must be 16 digits.";

        if (!exp.trim().matches("(0[1-9]|1[0-2])/[0-9]{2}")) return "Expiry must look like 08/29.";

        int mm = Integer.parseInt(exp.trim().substring(0, 2));
        int yy = 2000 + Integer.parseInt(exp.trim().substring(3, 5));

        if (YearMonth.of(yy, mm).isBefore(YearMonth.now())) return "This card has expired.";

        if (!cvv.trim().matches("[0-9]{3}")) return "CVV must be 3 digits.";

        return null;
    }

    private static JLabel err() {
        JLabel l = new JLabel(" ");
        l.setFont(Theme.font(Font.BOLD, 11));
        l.setForeground(new Color(255, 110, 110));
        return l;
    }

    private static JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TXT);
        return l;
    }

    private static JPanel form(String title, JTextField f, JLabel err, String hint) {

        JLabel h = new JLabel(hint);
        h.setFont(Theme.font(Font.PLAIN, 11));
        h.setForeground(Theme.DIM);

        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.setOpaque(false);
        p.add(label(title));
        p.add(f);
        p.add(h);
        p.add(err);

        return p;
    }

    private static JComponent wrap(JComponent c) {

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(14, 8, 8, 8));
        p.add(c, BorderLayout.NORTH);

        return p;
    }
}