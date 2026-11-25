
import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Route Overview: all open delivery destinations on the map, with the
 * assigned delivery person for each, plus a side list of every route.
 */
public class Routes extends JPanel {

    private final MapView map = new MapView();
    private final JPanel side = new JPanel();
    private final JPanel emptyHolder = new JPanel(new BorderLayout());
    private final JPanel body = new JPanel(new CardLayout());

    public Routes() {

        setBackground(Theme.PAPER);
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel("Route Overview");
        title.setFont(Theme.font(Font.BOLD, 26));
        title.setForeground(Theme.PLUM);

        JLabel sub = new JLabel(
            "Open deliveries and who is assigned to them. "
            + "Hover a pin for details. (Schematic map.)");
        sub.setFont(Theme.font(Font.PLAIN, 13));
        sub.setForeground(Theme.MUTED);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(sub);

        map.setShowLegend(false);

        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));

        JPanel sideHolder = new JPanel(new BorderLayout());
        sideHolder.setOpaque(false);
        sideHolder.add(side, BorderLayout.NORTH);

        JScrollPane sideScroll = new JScrollPane(sideHolder,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.setBorder(null);
        sideScroll.setOpaque(false);
        sideScroll.getViewport().setOpaque(false);
        sideScroll.setPreferredSize(new Dimension(320, 100));
        sideScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel mapRow = new JPanel(new BorderLayout(14, 0));
        mapRow.setOpaque(false);
        mapRow.add(map, BorderLayout.CENTER);
        mapRow.add(sideScroll, BorderLayout.EAST);

        emptyHolder.setOpaque(false);
        emptyHolder.add(new Empty(true, "No open deliveries",
            "When customers book shipments they appear here, grouped by "
            + "the courier you assign.", null, null), BorderLayout.CENTER);

        body.setOpaque(false);
        body.add(mapRow, "map");
        body.add(emptyHolder, "empty");

        add(titles, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        map.addLoadListener(this::rebuildSide);
    }

    private void rebuildSide() {

        List<MapView.Stop> stops = map.stops();

        ((CardLayout) body.getLayout()).show(body, stops.isEmpty() ? "empty" : "map");

        side.removeAll();

        Map<Integer, String> people = new LinkedHashMap<>();
        int unassigned = 0;

        for (MapView.Stop s : stops) {
            if (s.personId > 0) people.putIfAbsent(s.personId, s.person);
            else unassigned++;
        }

        for (Map.Entry<Integer, String> e : people.entrySet()) {

            List<MapView.Stop> route = map.route(e.getKey());
            Color c = MapView.colorFor(e.getKey());

            StringBuilder sb = new StringBuilder("<html>");

            for (MapView.Stop s : route) {
                sb.append(s.order).append(". ").append(s.tracking)
                  .append(" - ").append(s.receiver == null ? "" : s.receiver)
                  .append("<br>");
            }

            sb.append("</html>");

            side.add(routeCard(e.getValue() == null ? "Courier" : e.getValue(),
                    route.size() + (route.size() == 1 ? " stop" : " stops"),
                    sb.toString(), c));
            side.add(Box.createVerticalStrut(10));
        }

        if (unassigned > 0) {
            side.add(routeCard("Not assigned yet",
                    unassigned + (unassigned == 1 ? " shipment" : " shipments"),
                    "<html>Open <b>Assign Couriers</b> to give these a "
                    + "delivery person.</html>", Colors.OTHER));
        }

        side.revalidate();
        side.repaint();
    }

    private JPanel routeCard(String name, String count, String detail, Color c) {

        JPanel p = new JPanel(new BorderLayout(10, 4)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.smooth(g);
                g.setColor(Theme.PANEL2);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g.setColor(c);
                g.fillRoundRect(0, 0, 6, getHeight() - 1, 6, 6);
                g.setColor(Theme.LINE);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g.dispose();
            }
        };

        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 12));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel n = new JLabel(name);
        n.setFont(Theme.font(Font.BOLD, 14));
        n.setForeground(Theme.PLUM);

        JLabel k = new JLabel(count);
        k.setFont(Theme.font(Font.BOLD, 12));
        k.setForeground(c.darker());

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(n, BorderLayout.WEST);
        head.add(k, BorderLayout.EAST);

        JLabel d = new JLabel(detail);
        d.setFont(Theme.font(Font.PLAIN, 12));
        d.setForeground(Theme.MUTED);

        p.add(head, BorderLayout.NORTH);
        p.add(d, BorderLayout.CENTER);

        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                p.getPreferredSize().height + 400));

        return p;
    }
}