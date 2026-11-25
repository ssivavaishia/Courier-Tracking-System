import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.List;

/**
 * Contact Support: a ticket form with live validation messages (like the
 * Register page) and the list of the customer's tickets.
 */
public class Support extends JPanel {

    private final int customerId;

    private final JTextField subject = new JTextField();
    private final JTextArea message = new JTextArea(6, 20);
    private final JLabel subjectErr = err();
    private final JLabel messageErr = err();
    private final JLabel info = new JLabel(" ");
    private final JLabel counter = new JLabel("0 / 500");
    private final Grid tickets;

    public Support(int customerId, String customerName) {

        this.customerId = customerId;

        setOpaque(false);
        setLayout(new BorderLayout(14, 0));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        tickets = new Grid("My Tickets", "Track the answers to your requests.",
                new String[]{"Ticket ID", "Subject", "Status", "Created"}, 2) {
            @Override
            protected List<Object[]> fetch() throws Exception {

                List<Object[]> rows = newRows();

                try (Connection con = Db.getConnection();
                     PreparedStatement ps = con.prepareStatement(
                        "SELECT ticket_id, subject, ticket_status, created_at "
                        + "FROM support_tickets WHERE customer_id = ? "
                        + "ORDER BY created_at DESC")) {

                    ps.setInt(1, Support.this.customerId);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            rows.add(new Object[]{rs.getInt(1), rs.getString(2),
                                rs.getString(3), date(rs.getTimestamp(4))});
                        }
                    }
                }

                return rows;
            }
        };

        add(form(customerName), BorderLayout.WEST);
        add(tickets, BorderLayout.CENTER);
    }

    private static JLabel err() {
        JLabel l = new JLabel(" ");
        l.setFont(Theme.font(Font.BOLD, 11));
        l.setForeground(new Color(255, 110, 110));
        return l;
    }

    private JComponent form(String name) {

        Surface s = new Surface(18);
        s.setLayout(new GridBagLayout());
        s.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        s.setPreferredSize(new Dimension(420, 100));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(2, 0, 2, 0);
        g.anchor = GridBagConstraints.NORTHWEST;

        JLabel t = Kit.title("Submit a Request", "help", Colors.BOOKED);
        JLabel hi = new JLabel("Hello " + name + "! How can we help you?");
        hi.setFont(Theme.font(Font.PLAIN, 13));
        hi.setForeground(Theme.DIM);

        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setFont(Theme.font(Font.PLAIN, 14));
        message.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JScrollPane ms = new JScrollPane(message);
        ms.setBorder(new Skin.Field());
        ms.setPreferredSize(new Dimension(100, 150));

        counter.setFont(Theme.font(Font.PLAIN, 11));
        counter.setForeground(Theme.DIM);

        JButton submit = new JButton("Submit Request");
        submit.setBackground(Theme.RED);
        submit.setForeground(Color.WHITE);
        submit.setFont(Theme.font(Font.BOLD, 13));
        submit.setFocusPainted(false);
        submit.setBorder(BorderFactory.createEmptyBorder(11, 22, 11, 22));
        submit.addActionListener(e -> submit());

        JButton reset = new JButton("Reset");
        reset.setBackground(new Color(34, 47, 86));
        reset.setForeground(Color.WHITE);
        reset.setFont(Theme.font(Font.BOLD, 13));
        reset.setFocusPainted(false);
        reset.setBorder(BorderFactory.createEmptyBorder(11, 22, 11, 22));
        reset.addActionListener(e -> {
            subject.setText("");
            message.setText("");
            info.setText(" ");
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btns.setOpaque(false);
        btns.add(submit);
        btns.add(reset);

        info.setFont(Theme.font(Font.BOLD, 12));

        subject.getDocument().addDocumentListener(live(() -> checkSubject()));
        message.getDocument().addDocumentListener(live(() -> checkMessage()));

        int y = 0;
        g.gridy = y++; s.add(t, g);
        g.gridy = y++; s.add(hi, g);
        g.gridy = y++; g.insets = new Insets(16, 0, 4, 0); s.add(label("Subject"), g);
        g.gridy = y++; g.insets = new Insets(0, 0, 0, 0); s.add(subject, g);
        g.gridy = y++; s.add(subjectErr, g);
        g.gridy = y++; g.insets = new Insets(8, 0, 4, 0); s.add(label("Your message"), g);
        g.gridy = y++; g.insets = new Insets(0, 0, 0, 0); g.weighty = 1; g.fill = GridBagConstraints.BOTH;
        s.add(ms, g);
        g.weighty = 0; g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = y++; s.add(counter, g);
        g.gridy = y++; s.add(messageErr, g);
        g.gridy = y++; g.insets = new Insets(10, 0, 4, 0); s.add(btns, g);
        g.gridy = y;   g.insets = new Insets(4, 0, 0, 0); s.add(info, g);

        return s;
    }

    private static JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TXT);
        return l;
    }

    private static javax.swing.event.DocumentListener live(Runnable r) {
        return new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
        };
    }

    private boolean checkSubject() {

        String t = subject.getText().trim();
        String m = t.isEmpty() ? "Subject is required."
                 : t.length() < 5 ? "Subject must be at least 5 characters."
                 : t.length() > 100 ? "Subject must be 100 characters or fewer." : null;

        subject.putClientProperty("sud.error", m);
        subject.setToolTipText(m);
        subjectErr.setText(m == null ? " " : m);
        subject.repaint();

        return m == null;
    }

    private boolean checkMessage() {

        String t = message.getText().trim();
        counter.setText(message.getText().length() + " / 500");

        String m = t.isEmpty() ? "Message is required."
                 : t.length() < 10 ? "Message must be at least 10 characters."
                 : t.length() > 500 ? "Message must be 500 characters or fewer." : null;

        messageErr.setText(m == null ? " " : m);

        return m == null;
    }

    private void submit() {

        boolean ok = checkSubject();
        ok &= checkMessage();

        if (!ok) {
            info.setForeground(new Color(255, 110, 110));
            info.setText("Please fix the highlighted fields.");
            return;
        }

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                "INSERT INTO support_tickets (customer_id, subject, message) VALUES (?, ?, ?)")) {

            ps.setInt(1, customerId);
            ps.setString(2, subject.getText().trim());
            ps.setString(3, message.getText().trim());
            ps.executeUpdate();

            subject.setText("");
            message.setText("");
            subjectErr.setText(" ");
            messageErr.setText(" ");
            info.setForeground(Colors.DELIVERED);
            info.setText("Your request was submitted. We will get back to you soon.");

            tickets.reload();

        } catch (SQLException ex) {
            info.setForeground(new Color(255, 110, 110));
            info.setText("Could not submit: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}