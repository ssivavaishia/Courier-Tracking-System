import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Quick command search (Ctrl+K): one box to jump to any dashboard page or
 * to find customers, delivery personnel and shipments.
 */
public class Search extends JDialog {

    private static class Result {
        String kind, title, sub, target, detail;
        Result(String kind, String title, String sub, String target, String detail) {
            this.kind = kind;
            this.title = title;
            this.sub = sub;
            this.target = target;
            this.detail = detail;
        }
    }

    private final DefaultListModel<Result> model = new DefaultListModel<>();
    private final JList<Result> list = new JList<>(model);
    private final JTextField field = new JTextField();
    private final List<String> pages;
    private final Consumer<String> onPick;
    private final Timer debounce;
    private int ticket = 0;

    public Search(Window owner, List<String> pages, Consumer<String> onPick) {

        super(owner);

        this.pages = pages;
        this.onPick = onPick;

        setUndecorated(true);
        setSize(660, 440);

        if (owner != null) {
            setLocation(owner.getX() + (owner.getWidth() - 660) / 2,
                        owner.getY() + 110);
        } else {
            setLocationRelativeTo(null);
        }

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(Theme.NAVY);
        root.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.NAVY3, 2),
            BorderFactory.createEmptyBorder(14, 14, 10, 14)));

        field.setFont(Theme.font(Font.PLAIN, 18));
        field.setBackground(Theme.NAVY2);
        field.setForeground(Theme.TEXT_LIGHT);
        field.setCaretColor(Theme.TEXT_LIGHT);
        field.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel hint = new JLabel(
            "Search customers, shipments, delivery personnel or pages    "
            + "↑↓ move   Enter open   Esc close");
        hint.setFont(Theme.font(Font.PLAIN, 11));
        hint.setForeground(Theme.TEXT_DIM);

        list.setBackground(Theme.NAVY);
        list.setFixedCellHeight(46);
        list.setCellRenderer(new Renderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int i = list.locationToIndex(e.getPoint());
                if (i >= 0) { list.setSelectedIndex(i); pick(); }
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.NAVY);

        root.add(field, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(hint, BorderLayout.SOUTH);

        setContentPane(root);

        // keys
        bind("UP", () -> move(-1));
        bind("DOWN", () -> move(1));
        bind("ENTER", this::pick);
        bind("ESCAPE", this::dispose);

        debounce = new Timer(220, e -> search(field.getText().trim()));
        debounce.setRepeats(false);

        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { debounce.restart(); }
            public void removeUpdate(DocumentEvent e) { debounce.restart(); }
            public void changedUpdate(DocumentEvent e) { debounce.restart(); }
        });

        addWindowListener(new WindowAdapter() {
            @Override public void windowDeactivated(WindowEvent e) { dispose(); }
        });

        search("");
    }

    private void bind(String key, Runnable action) {

        field.getInputMap(JComponent.WHEN_FOCUSED)
             .put(KeyStroke.getKeyStroke(key), "sud-" + key);

        field.getActionMap().put("sud-" + key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) field.requestFocusInWindow();
    }

    private void move(int delta) {

        int n = model.size();
        if (n == 0) return;

        int i = Math.max(0, Math.min(n - 1, list.getSelectedIndex() + delta));

        list.setSelectedIndex(i);
        list.ensureIndexIsVisible(i);
    }

    private void pick() {

        Result r = list.getSelectedValue();

        if (r == null) return;

        Window owner = getOwner();

        dispose();

        SwingUtilities.invokeLater(() -> {

            onPick.accept(r.target);

            if (r.detail != null) {
                JOptionPane.showMessageDialog(owner, r.detail,
                    r.kind + " details", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    // -------------------------------------------------------------- search

    private void search(String q) {

        final int mine = ++ticket;

        List<Result> out = new ArrayList<>();

        for (String p : pages) {
            if (q.isEmpty() || p.toLowerCase().contains(q.toLowerCase())) {
                out.add(new Result("Page", p, "Open this page", p, null));
            }
        }

        show(out);

        if (q.length() < 2) return;

        new SwingWorker<List<Result>, Void>() {

            @Override
            protected List<Result> doInBackground() throws Exception {

                List<Result> found = new ArrayList<>();
                String like = "%" + q + "%";

                try (Connection con = Db.getConnection()) {

                    people(con, found, like, "CUSTOMER", "Customer", "Users");
                    people(con, found, like, "DELIVERY", "Courier",
                           "Delivery Personnel");

                    String sql =
                        "SELECT s.tracking_number, s.receiver_name, "
                        + "s.delivery_status, s.receiver_address, u.full_name "
                        + "FROM shipments s LEFT JOIN users u "
                        + "ON s.delivery_person_id = u.user_id "
                        + "WHERE s.tracking_number LIKE ? "
                        + "OR s.receiver_name LIKE ? OR s.receiver_phone LIKE ? "
                        + "ORDER BY s.shipment_id DESC LIMIT 6";

                    try (PreparedStatement ps = con.prepareStatement(sql)) {

                        ps.setString(1, like);
                        ps.setString(2, like);
                        ps.setString(3, like);

                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                found.add(new Result("Shipment", rs.getString(1),
                                    rs.getString(2) + "  ·  " + rs.getString(3),
                                    "Shipments",
                                    "Tracking number: " + rs.getString(1)
                                    + "\nReceiver: " + rs.getString(2)
                                    + "\nAddress: " + rs.getString(4)
                                    + "\nStatus: " + rs.getString(3)
                                    + "\nCourier: " + (rs.getString(5) == null
                                        ? "Not assigned yet" : rs.getString(5))));
                            }
                        }
                    }
                }

                return found;
            }

            @Override
            protected void done() {

                if (mine != ticket) return;   // a newer search is running

                try {
                    List<Result> all = new ArrayList<>(out);
                    all.addAll(get());
                    show(all);
                } catch (Exception ex) {
                    // keep the page results that are already shown
                }
            }

        }.execute();
    }

    private void people(Connection con, List<Result> found, String like,
                        String role, String kind, String target)
            throws SQLException {

        String sql =
            "SELECT full_name, email, phone, username, account_status "
            + "FROM users WHERE UPPER(role) = ? AND (full_name LIKE ? "
            + "OR email LIKE ? OR phone LIKE ? OR username LIKE ?) "
            + "ORDER BY full_name LIMIT 5";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, role);
            for (int i = 2; i <= 5; i++) ps.setString(i, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    found.add(new Result(kind, rs.getString(1), rs.getString(2),
                        target,
                        "Name: " + rs.getString(1)
                        + "\nUsername: " + rs.getString(4)
                        + "\nEmail: " + rs.getString(2)
                        + "\nPhone: " + rs.getString(3)
                        + "\nAccount: " + rs.getString(5)));
                }
            }
        }
    }

    private void show(List<Result> results) {

        model.clear();

        for (Result r : results) model.addElement(r);

        if (!results.isEmpty()) list.setSelectedIndex(0);
    }

    // ------------------------------------------------------------ renderer

    private static class Renderer implements ListCellRenderer<Result> {

        @Override
        public Component getListCellRendererComponent(JList<? extends Result> l,
                Result r, int index, boolean selected, boolean focus) {

            JPanel p = new JPanel(new BorderLayout(12, 0));
            p.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            p.setBackground(selected ? Theme.NAVY3 : Theme.NAVY);

            Color tag = r.kind.equals("Page") ? Colors.OTHER
                      : r.kind.equals("Customer") ? Colors.PICKED_UP
                      : r.kind.equals("Courier") ? Colors.OUT
                      : Colors.IN_TRANSIT;

            JLabel kind = new JLabel(r.kind.toUpperCase());
            kind.setFont(Theme.font(Font.BOLD, 10));
            kind.setForeground(tag);
            kind.setPreferredSize(new Dimension(72, 20));

            JLabel title = new JLabel(r.title == null ? "" : r.title);
            title.setFont(Theme.font(Font.BOLD, 14));
            title.setForeground(Theme.TEXT_LIGHT);

            JLabel sub = new JLabel(r.sub == null ? " " : r.sub);
            sub.setFont(Theme.font(Font.PLAIN, 12));
            sub.setForeground(Theme.TEXT_DIM);

            JPanel text = new JPanel(new GridLayout(2, 1));
            text.setOpaque(false);
            text.add(title);
            text.add(sub);

            p.add(kind, BorderLayout.WEST);
            p.add(text, BorderLayout.CENTER);

            return p;
        }
    }
}