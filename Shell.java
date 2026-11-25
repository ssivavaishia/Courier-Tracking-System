import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The common SUDWAR window (customer, delivery and admin): dark sidebar with
 * the logo, icon menu, mascot truck, a top bar with greeting, and a page
 * area. Subclasses only say which menu entries exist and which page belongs
 * to each entry.
 */
public abstract class Shell extends JFrame {

    private final String userName;
    private final String roleLabel;
    private final String[][] menu;

    private final JPanel content = new JPanel(new BorderLayout());
    private final JLabel subtitle = new JLabel(" ");
    private final Map<String, JButton> items = new LinkedHashMap<>();
    private final JComponent sidebar;
    private boolean menuVisible = true;

    protected Shell(String title, String userName, String roleLabel,
                    String[][] menu) {

        this.userName = userName == null ? "" : userName;
        this.roleLabel = roleLabel;
        this.menu = menu;

        Skin.install();

        setTitle(title);
        setSize(1366, 800);
        setMinimumSize(new Dimension(1100, 680));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);

        sidebar = buildSidebar();

        root.add(sidebar, BorderLayout.WEST);
        root.add(buildMain(), BorderLayout.CENTER);

        setContentPane(root);
    }

    protected abstract JComponent page(String name);

    /** Text under the greeting for a page. */
    protected String subtitleFor(String page) {
        return page;
    }

    /** Optional buttons at the right end of the top bar. */
    protected JComponent topExtras() {
        return null;
    }

    public void start(String first) {

        open(first);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void open(String name) {

        content.removeAll();

        JComponent p;

        try {
            p = page(name);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            p = new Empty("This page could not be opened", String.valueOf(ex.getMessage()));
        }

        if (!(p instanceof Native)) {

            // older pages sit inside a card that fills the whole area
            Surface card = new Surface(18);
            card.setLayout(new BorderLayout());
            card.setBorder(new EmptyBorder(8, 8, 8, 8));
            card.add(p, BorderLayout.CENTER);
            p = card;
        }

        content.add(p, BorderLayout.CENTER);
        subtitle.setText(subtitleFor(name));

        for (Map.Entry<String, JButton> e : items.entrySet()) {

            boolean on = e.getKey().equals(name);
            JButton b = e.getValue();

            b.setBackground(on ? Theme.RED : Theme.SIDE);
            b.setForeground(Color.WHITE);
            b.setFont(Theme.font(on ? Font.BOLD : Font.PLAIN, 14));
            b.setIcon(Ico.of(iconOf(e.getKey()), 21,
                    on ? Color.WHITE : new Color(190, 200, 225)));
        }

        content.revalidate();
        content.repaint();
    }

    private String iconOf(String name) {

        for (String[] m : menu) if (m[0].equals(name)) return m[1];

        return "home";
    }

    // ------------------------------------------------------------- sidebar

    private JComponent buildSidebar() {

        JPanel side = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setColor(Theme.SIDE);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.EDGE);
                g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                g.dispose();
            }
        };
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(262, 100));

        side.add(brand(), BorderLayout.NORTH);
        side.add(menuList(), BorderLayout.CENTER);
        side.add(footer(), BorderLayout.SOUTH);

        return side;
    }

    private JComponent brand() {

        JComponent logo = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {

                Graphics2D g = (Graphics2D) g0.create();
                Theme.smooth(g);

                int cx = getWidth() / 2 + 18, cy = 28;

                // speed lines
                g.setColor(Theme.RED);
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(cx - 70, cy - 4, cx - 34, cy - 4);
                g.drawLine(cx - 58, cy + 6, cx - 34, cy + 6);

                // cube
                Path2D top = new Path2D.Double();
                top.moveTo(cx, cy - 22); top.lineTo(cx + 22, cy - 11);
                top.lineTo(cx, cy); top.lineTo(cx - 22, cy - 11); top.closePath();
                g.setColor(new Color(255, 80, 80));
                g.fill(top);

                Path2D left = new Path2D.Double();
                left.moveTo(cx - 22, cy - 11); left.lineTo(cx, cy);
                left.lineTo(cx, cy + 26); left.lineTo(cx - 22, cy + 14); left.closePath();
                g.setColor(Theme.RED);
                g.fill(left);

                Path2D right = new Path2D.Double();
                right.moveTo(cx + 22, cy - 11); right.lineTo(cx, cy);
                right.lineTo(cx, cy + 26); right.lineTo(cx + 22, cy + 14); right.closePath();
                g.setColor(new Color(170, 20, 36));
                g.fill(right);

                g.dispose();
            }
        };
        logo.setPreferredSize(new Dimension(100, 62));

        JLabel name = new JLabel("SUDWAR", SwingConstants.CENTER);
        name.setFont(Theme.font(Font.BOLD, 30));
        name.setForeground(Color.WHITE);

        JLabel tag = new JLabel("Track  •  Ship  •  Deliver", SwingConstants.CENTER);
        tag.setFont(Theme.font(Font.PLAIN, 12));
        tag.setForeground(Theme.TXT);

        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(22, 10, 18, 10));
        p.add(logo, BorderLayout.NORTH);
        p.add(name, BorderLayout.CENTER);
        p.add(tag, BorderLayout.SOUTH);

        return p;
    }

    private JComponent menuList() {

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(6, 14, 6, 14));

        for (String[] m : menu) {

            JButton b = item(m[0]);
            b.addActionListener(e -> open(m[0]));
            items.put(m[0], b);

            list.add(b);
            list.add(Box.createVerticalStrut(5));
        }

        JPanel line = new JPanel();
        line.setBackground(Theme.EDGE);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setPreferredSize(new Dimension(10, 1));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);

        list.add(Box.createVerticalStrut(6));
        list.add(line);
        list.add(Box.createVerticalStrut(10));

        JButton out = item("Logout");
        out.setIcon(Ico.of("logout", 21, new Color(190, 200, 225)));
        out.addActionListener(e -> logout());
        list.add(out);

        JScrollPane sc = new JScrollPane(list,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setBorder(null);
        sc.setOpaque(false);
        sc.getViewport().setOpaque(false);

        return sc;
    }

    private JButton item(String text) {

        JButton b = new JButton(text);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setIconTextGap(16);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(12, 16, 12, 12));
        b.setBackground(Theme.SIDE);
        b.setForeground(Color.WHITE);
        b.setFont(Theme.font(Font.PLAIN, 14));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        return b;
    }

    /** Mascot truck, slogan and the red swoosh at the bottom of the sidebar. */
    private JComponent footer() {

        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {

                Graphics2D g = (Graphics2D) g0.create();
                Theme.smooth(g);

                int w = getWidth(), h = getHeight();

                g.setPaint(new GradientPaint(0, h * 0.45f, new Color(222, 38, 52, 0),
                        0, h, new Color(222, 38, 52, 120)));
                g.fillRect(0, 0, w - 1, h);

                Path2D sw = new Path2D.Double();
                sw.moveTo(0, h - 34);
                sw.curveTo(w * 0.35, h - 10, w * 0.65, h - 70, w, h - 26);
                sw.lineTo(w, h); sw.lineTo(0, h); sw.closePath();
                g.setColor(new Color(150, 16, 30));
                g.fill(sw);

                g.dispose();
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(100, 170));
        p.setBorder(new EmptyBorder(8, 14, 10, 14));

        Mascot truck = new Mascot(Theme.RED);
        truck.setPreferredSize(new Dimension(200, 52));

        JLabel slogan = new JLabel("<html><i>Delivering<br>&nbsp;&nbsp;&nbsp;Happiness...</i></html>");
        slogan.setFont(Theme.font(Font.PLAIN, 16));
        slogan.setForeground(Color.WHITE);

        p.add(truck, BorderLayout.NORTH);
        p.add(slogan, BorderLayout.CENTER);

        return p;
    }

    // ------------------------------------------------------------ main area

    private JComponent buildMain() {

        JComponent avatar = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.smooth(g);
                g.setColor(new Color(120, 20, 34));
                g.fillOval(1, 1, getWidth() - 3, getHeight() - 3);
                g.setColor(Theme.RED);
                g.setStroke(new BasicStroke(2f));
                g.drawOval(1, 1, getWidth() - 3, getHeight() - 3);
                Ico.of("user", 26, Color.WHITE).paintIcon(this, g,
                    (getWidth() - 26) / 2, (getHeight() - 26) / 2);
                g.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(58, 58));

        JLabel hello = new Greeting(userName);
        hello.setFont(Theme.font(Font.BOLD, 24));
        hello.setForeground(Color.WHITE);

        subtitle.setFont(Theme.font(Font.PLAIN, 14));
        subtitle.setForeground(Theme.TXT);

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(hello);
        text.add(subtitle);

        JButton burger = new JButton(Ico.of("menu", 22, Theme.TXT));
        burger.setBackground(Theme.PANEL);
        burger.setFocusPainted(false);
        burger.setBorder(new EmptyBorder(9, 10, 9, 10));
        burger.setToolTipText("Show / hide menu");
        burger.addActionListener(e -> {
            menuVisible = !menuVisible;
            sidebar.setVisible(menuVisible);
            revalidate();
        });

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);
        left.add(burger);
        left.add(avatar);
        left.add(text);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(14, 18, 10, 22));
        bar.add(left, BorderLayout.WEST);

        JComponent extras = topExtras();
        if (extras != null) bar.add(extras, BorderLayout.EAST);

        content.setOpaque(false);
        content.setBorder(new EmptyBorder(0, 16, 16, 16));

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BG);
        main.add(bar, BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);

        return main;
    }

    protected void logout() {

        int c = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);

        if (c == JOptionPane.YES_OPTION) Nav.toWelcome(this);
    }
}