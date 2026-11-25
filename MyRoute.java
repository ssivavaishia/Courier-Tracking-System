
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Map page for one person:
 *   role "DELIVERY"  -> "My Route": the delivery person's own stops, in order
 *   role "CUSTOMER"  -> "Parcel Map": where the customer's parcels are going
 * Uses the real street map (MapView) plus a card list of the stops.
 */
public class MyRoute extends JPanel {

    private final boolean courier;
    private final int userId;
    private final MapView map;
    private final JPanel list = new JPanel();
    private final JPanel body = new JPanel(new CardLayout());

    public MyRoute(String role, int userId) {

        this.courier = "DELIVERY".equalsIgnoreCase(role);
        this.userId = userId;

        map = new MapView(courier ? "COURIER" : "CUSTOMER", userId);
        map.setDark(true);
        map.setShowLegend(false);

        setBackground(Theme.PAPER);
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel(courier ? "My Route" : "Parcel Map");
        title.setFont(Theme.font(Font.BOLD, 26));
        title.setForeground(Theme.PLUM);

        JLabel sub = new JLabel(courier
            ? "Your open deliveries in suggested order. Drag to move, scroll to zoom."
            : "Where your open parcels are heading. Drag to move, scroll to zoom.");
        sub.setFont(Theme.font(Font.PLAIN, 13));
        sub.setForeground(Theme.MUTED);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(sub);

        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(list, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(holder,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(330, 100));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.add(map, BorderLayout.CENTER);
        row.add(scroll, BorderLayout.EAST);

        JPanel empty = new JPanel(new BorderLayout());
        empty.setOpaque(false);
        empty.add(new Empty(true,
            courier ? "No stops on your route" : "No open parcels",
            courier ? "When the admin assigns shipments to you, they appear here "
                      + "with a map."
                    : "Book a shipment and its destination will appear on the map.",
            null, null), BorderLayout.CENTER);

        body.setOpaque(false);
        body.add(row, "map");
        body.add(empty, "empty");

        add(titles, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        map.addLoadListener(this::rebuild);
    }

    private void rebuild() {

        List<MapView.Stop> stops = map.stops();

        ((CardLayout) body.getLayout()).show(body, stops.isEmpty() ? "empty" : "map");

        list.removeAll();

        List<MapView.Stop> ordered = courier ? map.route(userId) : stops;

        // stops without a number (not located yet) go after the numbered ones
        if (courier) {
            ordered = new java.util.ArrayList<>(ordered);
            for (MapView.Stop s : stops) if (!ordered.contains(s)) ordered.add(s);
        }

        int n = 1;

        for (MapView.Stop s : ordered) {

            Card c = new Card(true,
                (courier && s.order > 0 ? s.order + ".  " : (n++) + ".  ") + s.tracking,
                s.receiver, s.address, s.status);

            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 108));
            c.setAlignmentX(Component.LEFT_ALIGNMENT);

            list.add(c);
            list.add(Box.createVerticalStrut(10));
        }

        list.revalidate();
        list.repaint();
    }
}