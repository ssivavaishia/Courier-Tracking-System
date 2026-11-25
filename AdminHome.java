import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Admin window (SUDWAR design). Each menu entry opens its own class through
 * Nav.adminPage(...). Home is the Logistics Intelligence Centre.
 * Ctrl+K opens the quick command search from anywhere.
 */
public class AdminHome extends Shell {

    private final int adminId;
    private final Layout layout;
    private String current = Nav.HOME;

    public AdminHome() {
        this(0, "Administrator");
    }

    public AdminHome(int adminId, String adminName) {

        super("SUDWAR - Admin", adminName, "Administrator", new String[][]{
            {Nav.HOME, "home"}, {Nav.FEED, "clock"}, {Nav.ROUTES, "route"},
            {Nav.INSIGHTS, "chart"}, {Nav.MESSAGES, "chat"}, {Nav.STATION, "boxes"},
            {Nav.SHIPMENTS, "truck"}, {Nav.ASSIGN, "pin"}, {Nav.STATUS, "edit"},
            {Nav.USERS, "user"}, {Nav.COURIERS, "check"}, {Nav.BRANCHES, "map"},
            {Nav.PAYMENTS, "rupee"}, {Nav.REPORTS, "chart"}
        });

        this.adminId = adminId;
        this.layout = new Layout(adminId);

        KeyStroke ctrlK = KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK);
        KeyStroke metaK = KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.META_DOWN_MASK);

        for (KeyStroke ks : new KeyStroke[]{ctrlK, metaK}) {
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, "sud-search");
        }

        getRootPane().getActionMap().put("sud-search", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                openSearch();
            }
        });

        start(Nav.HOME);
    }

    @Override
    protected String subtitleFor(String page) {
        return page.equals(Nav.HOME)
            ? "Here's what's happening across the courier network today." : page;
    }

    @Override
    protected JComponent topExtras() {

        JButton search = new JButton("Search   Ctrl+K");
        search.setIcon(Ico.of("track", 18, Theme.TXT));
        search.setIconTextGap(10);
        search.setBackground(Theme.PANEL);
        search.setForeground(Theme.TXT);
        search.setFont(Theme.font(Font.BOLD, 13));
        search.setFocusPainted(false);
        search.setBorder(new EmptyBorder(11, 16, 11, 18));
        search.addActionListener(e -> openSearch());

        JButton customize = new JButton("Customize");
        customize.setBackground(Theme.RED);
        customize.setForeground(Color.WHITE);
        customize.setFont(Theme.font(Font.BOLD, 13));
        customize.setFocusPainted(false);
        customize.setBorder(new EmptyBorder(11, 18, 11, 18));
        customize.addActionListener(e ->
            layout.showDialog(this, () -> { if (Nav.HOME.equals(current)) open(Nav.HOME); }));

        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        p.setOpaque(false);
        p.add(search);
        p.add(customize);

        return p;
    }

    @Override
    protected JComponent page(String name) {

        current = name;

        return Nav.adminPage(name, adminId, layout, this::open);
    }

    private void openSearch() {

        new Search(this, Nav.adminPages(), this::open).setVisible(true);
    }
}