
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Chat for one shipment at a time. The customer, the delivery person
 * assigned to the shipment, and the admin can all write in the same thread.
 *
 *   role "CUSTOMER"  sees and writes in their own shipments
 *   role "DELIVERY"  sees and writes in shipments assigned to them
 *   role "ADMIN"     sees every shipment
 *
 * The thread refreshes itself every 4 seconds. Messages are stored in the
 * chat_messages table.
 */
public class Chat extends JPanel {

    private static class Conv {
        int shipmentId;
        String tracking, receiver, status, customer, courier, last;
        Timestamp lastAt;
    }

    private static class Msg {
        int id, senderId;
        String name, role, text;
        Timestamp at;
    }

    private final int userId;
    private final String role;

    private final DefaultListModel<Conv> convModel = new DefaultListModel<>();
    private final JList<Conv> convList = new JList<>(convModel);
    private final JPanel thread = new JPanel();
    private final JScrollPane threadScroll;
    private final JLabel header = new JLabel("Choose a conversation");
    private final JLabel sub = new JLabel(" ");
    private final JTextField input = new JTextField();
    private final JButton send = new JButton("Send");
    private final JPanel rightCards = new JPanel(new CardLayout());
    private final Timer poll;

    private int openShipment = -1;
    private int lastMessageId = -1;
    private boolean loadingList = false;

    public Chat(int userId, String role, String name) {

        this.userId = userId;
        this.role = role == null ? "CUSTOMER" : role.toUpperCase();

        setBackground(Theme.PAPER);
        setLayout(new BorderLayout(14, 0));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // ---- left: conversations
        JLabel title = new JLabel("Messages");
        title.setFont(Theme.font(Font.BOLD, 24));
        title.setForeground(Theme.PLUM);

        JLabel hint = new JLabel(roleHint());
        hint.setFont(Theme.font(Font.PLAIN, 12));
        hint.setForeground(Theme.MUTED);

        JPanel leftTop = new JPanel(new GridLayout(2, 1, 0, 2));
        leftTop.setOpaque(false);
        leftTop.add(title);
        leftTop.add(hint);

        convList.setCellRenderer(new ConvRenderer());
        convList.setFixedCellHeight(66);
        convList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        convList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !loadingList) {
                Conv c = convList.getSelectedValue();
                if (c != null) openConversation(c);
            }
        });

        JScrollPane convScroll = new JScrollPane(convList);
        convScroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));

        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(310, 100));
        left.add(leftTop, BorderLayout.NORTH);
        left.add(convScroll, BorderLayout.CENTER);

        // ---- right: thread
        header.setFont(Theme.font(Font.BOLD, 17));
        header.setForeground(Theme.PLUM);

        sub.setFont(Theme.font(Font.PLAIN, 12));
        sub.setForeground(Theme.MUTED);

        JPanel head = new JPanel(new GridLayout(2, 1, 0, 2));
        head.setBackground(Theme.PANEL);
        head.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        head.add(header);
        head.add(sub);

        thread.setLayout(new BoxLayout(thread, BoxLayout.Y_AXIS));
        thread.setBackground(new Color(246, 249, 243));
        thread.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JPanel threadHolder = new JPanel(new BorderLayout());
        threadHolder.setBackground(new Color(246, 249, 243));
        threadHolder.add(thread, BorderLayout.NORTH);

        threadScroll = new JScrollPane(threadHolder,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        threadScroll.setBorder(null);
        threadScroll.getVerticalScrollBar().setUnitIncrement(16);

        input.setFont(Theme.font(Font.PLAIN, 14));
        input.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(185, 200, 182)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        input.addActionListener(e -> sendMessage());

        send.setBackground(Theme.RED);
        send.setForeground(Color.WHITE);
        send.setFont(Theme.font(Font.BOLD, 13));
        send.setFocusPainted(false);
        send.setOpaque(true);
        send.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        send.addActionListener(e -> sendMessage());

        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(Theme.PANEL);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        bar.add(input, BorderLayout.CENTER);
        bar.add(send, BorderLayout.EAST);

        JPanel chatPane = new JPanel(new BorderLayout());
        chatPane.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        chatPane.add(head, BorderLayout.NORTH);
        chatPane.add(threadScroll, BorderLayout.CENTER);
        chatPane.add(bar, BorderLayout.SOUTH);

        JPanel pick = new JPanel(new BorderLayout());
        pick.setOpaque(false);
        pick.add(new Empty(true, "Pick a conversation",
            "Choose a shipment on the left to read and send messages.",
            null, null), BorderLayout.CENTER);

        rightCards.setOpaque(false);
        rightCards.add(pick, "pick");
        rightCards.add(chatPane, "chat");

        add(left, BorderLayout.WEST);
        add(rightCards, BorderLayout.CENTER);

        poll = new Timer(4000, e -> {
            loadConversations(false);
            if (openShipment > 0) loadMessages(false);
        });
    }

    private String roleHint() {
        switch (role) {
            case "DELIVERY": return "Chat with customers about your deliveries.";
            case "ADMIN":    return "All shipment conversations.";
            default:         return "Ask your courier or SUDWAR about a parcel.";
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        loadConversations(true);
        poll.start();
    }

    @Override
    public void removeNotify() {
        poll.stop();
        super.removeNotify();
    }

    // ----------------------------------------------------- conversations

    private String roleFilter() {
        switch (role) {
            case "CUSTOMER": return "WHERE s.customer_id = ? ";
            case "DELIVERY": return "WHERE s.delivery_person_id = ? ";
            default:         return "";
        }
    }

    private void loadConversations(boolean selectFirst) {

        new SwingWorker<List<Conv>, Void>() {

            @Override
            protected List<Conv> doInBackground() throws Exception {

                String sql =
                    "SELECT s.shipment_id, s.tracking_number, s.receiver_name, "
                    + "s.delivery_status, c.full_name, d.full_name, "
                    + "(SELECT m.message FROM chat_messages m "
                    + "  WHERE m.shipment_id = s.shipment_id "
                    + "  ORDER BY m.message_id DESC LIMIT 1), "
                    + "(SELECT MAX(m.sent_at) FROM chat_messages m "
                    + "  WHERE m.shipment_id = s.shipment_id) AS last_at "
                    + "FROM shipments s "
                    + "JOIN users c ON c.user_id = s.customer_id "
                    + "LEFT JOIN users d ON d.user_id = s.delivery_person_id "
                    + roleFilter()
                    + "ORDER BY last_at DESC, s.shipment_id DESC LIMIT 100";

                List<Conv> out = new ArrayList<>();

                try (Connection con = Db.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {

                    if (!role.equals("ADMIN")) ps.setInt(1, userId);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Conv c = new Conv();
                            c.shipmentId = rs.getInt(1);
                            c.tracking = rs.getString(2);
                            c.receiver = rs.getString(3);
                            c.status = rs.getString(4);
                            c.customer = rs.getString(5);
                            c.courier = rs.getString(6);
                            c.last = rs.getString(7);
                            c.lastAt = rs.getTimestamp(8);
                            out.add(c);
                        }
                    }
                }

                return out;
            }

            @Override
            protected void done() {

                try {

                    List<Conv> list = get();

                    loadingList = true;

                    convModel.clear();
                    int keep = -1;

                    for (int i = 0; i < list.size(); i++) {
                        convModel.addElement(list.get(i));
                        if (list.get(i).shipmentId == openShipment) keep = i;
                    }

                    if (keep >= 0) convList.setSelectedIndex(keep);

                    loadingList = false;

                    if (list.isEmpty()) {
                        showEmptyList();
                    } else if (selectFirst && openShipment < 0) {
                        convList.setSelectedIndex(0);
                    }

                } catch (Exception ex) {
                    loadingList = false;
                    header.setText("Could not load messages");
                    sub.setText("Is the chat_messages table created? "
                                + "(see sudwar_schema.sql)");
                    ((CardLayout) rightCards.getLayout()).show(rightCards, "chat");
                }
            }

        }.execute();
    }

    private void showEmptyList() {

        rightCards.removeAll();

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(new Empty(true, "No shipments to chat about",
            role.equals("CUSTOMER")
                ? "Book a shipment first. You can then message your courier here."
                : "Conversations appear once shipments are assigned.",
            null, null), BorderLayout.CENTER);

        rightCards.add(p, "pick");
        ((CardLayout) rightCards.getLayout()).show(rightCards, "pick");
        rightCards.revalidate();
        rightCards.repaint();
    }

    private void openConversation(Conv c) {

        openShipment = c.shipmentId;
        lastMessageId = -1;

        header.setText("Shipment " + c.tracking);
        sub.setText("Customer: " + c.customer + "     Courier: "
                + (c.courier == null ? "not assigned yet" : c.courier)
                + "     Status: " + Colors.norm(c.status));

        ((CardLayout) rightCards.getLayout()).show(rightCards, "chat");

        loadMessages(true);

        SwingUtilities.invokeLater(input::requestFocusInWindow);
    }

    // ------------------------------------------------------------ messages

    private void loadMessages(boolean scrollDown) {

        final int shipment = openShipment;

        new SwingWorker<List<Msg>, Void>() {

            @Override
            protected List<Msg> doInBackground() throws Exception {

                String sql =
                    "SELECT m.message_id, m.sender_id, u.full_name, "
                    + "UPPER(u.role), m.message, m.sent_at "
                    + "FROM chat_messages m "
                    + "JOIN users u ON u.user_id = m.sender_id "
                    + "JOIN shipments s ON s.shipment_id = m.shipment_id "
                    + "WHERE m.shipment_id = ? "
                    + (role.equals("CUSTOMER") ? "AND s.customer_id = ? "
                     : role.equals("DELIVERY") ? "AND s.delivery_person_id = ? "
                     : "")
                    + "ORDER BY m.message_id";

                List<Msg> out = new ArrayList<>();

                try (Connection con = Db.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setInt(1, shipment);
                    if (!role.equals("ADMIN")) ps.setInt(2, userId);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Msg m = new Msg();
                            m.id = rs.getInt(1);
                            m.senderId = rs.getInt(2);
                            m.name = rs.getString(3);
                            m.role = rs.getString(4);
                            m.text = rs.getString(5);
                            m.at = rs.getTimestamp(6);
                            out.add(m);
                        }
                    }
                }

                return out;
            }

            @Override
            protected void done() {

                if (shipment != openShipment) return;

                try {

                    List<Msg> msgs = get();

                    int newest = msgs.isEmpty() ? 0 : msgs.get(msgs.size() - 1).id;

                    if (newest == lastMessageId && !scrollDown) return;

                    lastMessageId = newest;

                    thread.removeAll();

                    if (msgs.isEmpty()) {
                        JLabel none = new JLabel(
                            "No messages yet. Say hello below.");
                        none.setForeground(Theme.MUTED);
                        none.setFont(Theme.font(Font.ITALIC, 13));
                        none.setAlignmentX(Component.CENTER_ALIGNMENT);
                        thread.add(none);
                    }

                    for (Msg m : msgs) {
                        thread.add(new Bubble(m, m.senderId == userId));
                        thread.add(Box.createVerticalStrut(8));
                    }

                    thread.revalidate();
                    thread.repaint();

                    SwingUtilities.invokeLater(() -> {
                        JScrollBar bar = threadScroll.getVerticalScrollBar();
                        bar.setValue(bar.getMaximum());
                    });

                } catch (Exception ex) {
                    // keep what is shown; the next poll will try again
                }
            }

        }.execute();
    }

    private void sendMessage() {

        String text = input.getText().trim();

        if (text.isEmpty() || openShipment < 0) return;

        if (text.length() > 1000) {
            JOptionPane.showMessageDialog(this,
                "Please keep messages under 1000 characters.");
            return;
        }

        final int shipment = openShipment;

        input.setText("");

        new SwingWorker<Boolean, Void>() {

            @Override
            protected Boolean doInBackground() throws Exception {

                // the WHERE clause makes sure the sender belongs to the shipment
                String sql =
                    "INSERT INTO chat_messages (shipment_id, sender_id, message) "
                    + "SELECT s.shipment_id, ?, ? FROM shipments s "
                    + "WHERE s.shipment_id = ? "
                    + (role.equals("CUSTOMER") ? "AND s.customer_id = ?"
                     : role.equals("DELIVERY") ? "AND s.delivery_person_id = ?"
                     : "");

                try (Connection con = Db.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setInt(1, userId);
                    ps.setString(2, text);
                    ps.setInt(3, shipment);
                    if (!role.equals("ADMIN")) ps.setInt(4, userId);

                    return ps.executeUpdate() == 1;
                }
            }

            @Override
            protected void done() {

                try {
                    if (!get()) {
                        JOptionPane.showMessageDialog(Chat.this,
                            "This message could not be sent.");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(Chat.this,
                        "Could not send: " + ex.getMessage());
                }

                loadMessages(true);
                loadConversations(false);
            }

        }.execute();
    }

    // ------------------------------------------------------------ widgets

    private static String esc(String s) {

        if (s == null) return "";

        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\n", "<br>");
    }

    /** One chat bubble: mine on the right (plum), others on the left. */
    private static class Bubble extends JPanel {

        Bubble(Msg m, boolean mine) {

            setOpaque(false);
            setLayout(new FlowLayout(mine ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            String who = mine ? "You"
                : m.name + (m.role.equals("DELIVERY") ? " (courier)"
                          : m.role.equals("ADMIN") ? " (SUDWAR)" : "");

            JLabel label = new JLabel("<html><div style='width:250px'>"
                + "<b><font size=2>" + esc(who) + "</font></b><br>"
                + esc(m.text) + "<br><font size=1 color='"
                + (mine ? "#E8D8EE" : "#8A7A90") + "'>"
                + (m.at == null ? "" : Theme.ago(m.at)) + "</font></div></html>");

            label.setFont(Theme.font(Font.PLAIN, 13));
            label.setForeground(Theme.TXT);
            label.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

            JPanel bubble = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    Theme.smooth(g);
                    g.setColor(mine ? Theme.RED : Theme.PANEL2);
                    g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                    if (!mine) {
                        g.setColor(Theme.LINE);
                        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                    }
                    g.dispose();
                }
            };

            bubble.setOpaque(false);
            bubble.add(label, BorderLayout.CENTER);

            add(bubble);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    private static class ConvRenderer implements ListCellRenderer<Conv> {

        @Override
        public Component getListCellRendererComponent(JList<? extends Conv> l,
                Conv c, int index, boolean selected, boolean focus) {

            JPanel p = new JPanel(new BorderLayout(10, 0));
            p.setBackground(selected ? Theme.LILAC : Theme.PANEL);
            p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

            Color accent = Colors.of(c.status);

            JComponent dot = new JComponent() {
                @Override protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    Theme.smooth(g);
                    g.setColor(accent);
                    g.fillOval(0, 4, 12, 12);
                    g.dispose();
                }
            };
            dot.setPreferredSize(new Dimension(14, 20));

            JLabel top = new JLabel(c.tracking + "  ·  " + (c.receiver == null ? "" : c.receiver));
            top.setFont(Theme.font(Font.BOLD, 13));
            top.setForeground(Theme.PLUM);

            String preview = c.last == null ? "No messages yet" : c.last.replace('\n', ' ');
            if (preview.length() > 38) preview = preview.substring(0, 37) + "…";

            JLabel bottom = new JLabel(preview);
            bottom.setFont(Theme.font(c.last == null ? Font.ITALIC : Font.PLAIN, 12));
            bottom.setForeground(Theme.MUTED);

            JLabel when = new JLabel(c.lastAt == null ? "" : Theme.ago(c.lastAt));
            when.setFont(Theme.font(Font.PLAIN, 10));
            when.setForeground(Theme.MUTED);
            when.setVerticalAlignment(SwingConstants.TOP);

            JPanel text = new JPanel(new GridLayout(2, 1, 0, 3));
            text.setOpaque(false);
            text.add(top);
            text.add(bottom);

            JPanel dotHolder = new JPanel(new BorderLayout());
            dotHolder.setOpaque(false);
            dotHolder.add(dot, BorderLayout.NORTH);

            p.add(dotHolder, BorderLayout.WEST);
            p.add(text, BorderLayout.CENTER);
            p.add(when, BorderLayout.EAST);

            return p;
        }
    }
}