import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * SUDWAR Logistics Intelligence Centre (the admin home screen):
 *
 *   - a large central map of every open delivery
 *   - shipment statistics floating over the map
 *   - an activity panel on the right
 *   - a compact strip of recent shipments at the bottom
 *   - different accent colours for different shipment statuses
 *
 * The left navigation panel lives in AdminHome. Which cards and panels are
 * shown is controlled by Layout (Customize button).
 */
public class Centre extends JPanel implements Native {

    private final Layout layout;
    private final Stats stats;
    private final Mascot mascot;
    private final JPanel feedPanel;
    private final JPanel recentPanel;

    public Centre(Layout layout, Consumer<String> nav) {

        this.layout = layout;

        setBackground(Theme.NAVY);
        setOpaque(true);
        setLayout(new BorderLayout());

        // ---- header
        JLabel title = new JLabel("LOGISTICS INTELLIGENCE CENTRE");
        title.setFont(Theme.font(Font.BOLD, 13));
        title.setForeground(Theme.TEXT_LIGHT);

        JLabel live = new JLabel("●  LIVE  ·  refreshes every 45 seconds");
        live.setFont(Theme.font(Font.PLAIN, 11));
        live.setForeground(Colors.DELIVERED);

        JButton addCourier = new JButton("+  Add delivery person");
        addCourier.setBackground(Theme.RED);
        addCourier.setForeground(Color.WHITE);
        addCourier.setFont(Theme.font(Font.BOLD, 12));
        addCourier.setFocusPainted(false);
        addCourier.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        addCourier.setToolTipText("Register a new delivery person");
        addCourier.addActionListener(e -> nav.accept(Nav.COURIERS));

        JButton station = new JButton("Parcel station");
        station.setBackground(Theme.NAVY3);
        station.setForeground(Color.WHITE);
        station.setFont(Theme.font(Font.BOLD, 12));
        station.setFocusPainted(false);
        station.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        station.addActionListener(e -> nav.accept(Nav.STATION));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(live);
        right.add(station);
        right.add(addCourier);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 12));
        header.add(title, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        // ---- map with floating overlays
        MapView map = new MapView();
        stats = new Stats(layout);
        mascot = new Mascot(new Color(66, 133, 244));

        JPanel mapArea = new JPanel(null) {
            @Override
            public void doLayout() {

                map.setBounds(0, 0, getWidth(), getHeight());

                Dimension sp = stats.getPreferredSize();
                stats.setBounds(14, 14, Math.max(0, getWidth() - 28),
                        Math.max(sp.height, 70));

                Dimension mp = mascot.getPreferredSize();
                mascot.setBounds(14, getHeight() - mp.height - 8,
                        mp.width, mp.height);
            }
        };
        mapArea.setBackground(Theme.NAVY);

        // first added = painted on top
        mapArea.add(stats);
        mapArea.add(mascot);
        mapArea.add(map);

        // ---- right: activity
        JLabel feedTitle = new JLabel("ACTIVITY");
        feedTitle.setFont(Theme.font(Font.BOLD, 11));
        feedTitle.setForeground(Theme.TEXT_DIM);

        JButton more = new JButton("See all");
        more.setFont(Theme.font(Font.BOLD, 11));
        more.setForeground(Theme.TEXT_LIGHT);
        more.setBackground(Theme.NAVY3);
        more.setFocusPainted(false);
        more.setOpaque(true);
        more.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        more.addActionListener(e -> nav.accept("Activity Feed"));

        JPanel feedHead = new JPanel(new BorderLayout());
        feedHead.setOpaque(false);
        feedHead.add(feedTitle, BorderLayout.WEST);
        feedHead.add(more, BorderLayout.EAST);

        feedPanel = new JPanel(new BorderLayout(0, 8));
        feedPanel.setBackground(Theme.NAVY2);
        feedPanel.setBorder(BorderFactory.createEmptyBorder(14, 14, 10, 10));
        feedPanel.setPreferredSize(new Dimension(340, 100));
        feedPanel.add(feedHead, BorderLayout.NORTH);
        feedPanel.add(new Feed(true, false, 40), BorderLayout.CENTER);

        // ---- bottom: recent shipments
        recentPanel = new JPanel(new BorderLayout());
        recentPanel.setBackground(Theme.NAVY2);
        recentPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 12, 14));
        recentPanel.setPreferredSize(new Dimension(100, 168));
        recentPanel.add(new Recent(() -> nav.accept("Shipments")),
                BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(mapArea, BorderLayout.CENTER);
        add(feedPanel, BorderLayout.EAST);
        add(recentPanel, BorderLayout.SOUTH);

        applyLayout();
    }

    /** Called at start and whenever the user changes Customize options. */
    public void applyLayout() {

        stats.applyLayout();

        feedPanel.setVisible(layout.visible("feed"));
        recentPanel.setVisible(layout.visible("recent"));
        mascot.setVisible(layout.visible("mascot"));

        revalidate();
        repaint();
    }
}