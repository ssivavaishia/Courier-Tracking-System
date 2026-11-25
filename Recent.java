import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Compact bottom strip with the most recently booked shipments as cards.
 * Clicking a card calls the supplied action (e.g. open Shipments).
 */
public class Recent extends JPanel {

    private final Runnable onOpen;
    private final JPanel strip = new JPanel();
    private final Timer refresh;

    public Recent(Runnable onOpen) {

        this.onOpen = onOpen;

        setOpaque(false);
        setLayout(new BorderLayout(0, 6));

        JLabel title = new JLabel("RECENT SHIPMENTS");
        title.setFont(Theme.font(Font.BOLD, 11));
        title.setForeground(Theme.TEXT_DIM);
        title.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

        strip.setOpaque(false);
        strip.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 0));

        JScrollPane scroll = new JScrollPane(strip,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getHorizontalScrollBar().setUnitIncrement(24);

        add(title, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        refresh = new Timer(45_000, e -> reload());
    }

    @Override
    public void addNotify() {
        super.addNotify();
        reload();
        refresh.start();
    }

    @Override
    public void removeNotify() {
        refresh.stop();
        super.removeNotify();
    }

    public void reload() {

        new SwingWorker<List<String[]>, Void>() {

            @Override
            protected List<String[]> doInBackground() throws Exception {

                String sql =
                    "SELECT tracking_number, receiver_name, "
                    + "receiver_address, delivery_status "
                    + "FROM shipments ORDER BY shipment_id DESC LIMIT 10";

                List<String[]> rows = new ArrayList<>();

                try (Connection con = Db.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        rows.add(new String[]{
                            rs.getString(1), rs.getString(2),
                            rs.getString(3), rs.getString(4)});
                    }
                }

                return rows;
            }

            @Override
            protected void done() {

                strip.removeAll();

                try {

                    List<String[]> rows = get();

                    if (rows.isEmpty()) {
                        strip.add(emptyNote(
                            "No shipments yet. Bookings made by customers "
                            + "will appear here."));
                    }

                    for (String[] r : rows) {

                        Card c = new Card(true, r[0], r[1], r[2], r[3]);
                        c.setPreferredSize(new Dimension(290, 100));
                        c.onClick(onOpen);

                        strip.add(c);
                    }

                } catch (Exception ex) {
                    strip.add(emptyNote("Could not load recent shipments."));
                }

                strip.revalidate();
                strip.repaint();
            }

        }.execute();
    }

    private JComponent emptyNote(String text) {

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 14));
        p.setOpaque(false);

        Mascot truck = new Mascot(new Color(66, 133, 244));

        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.PLAIN, 13));
        l.setForeground(Theme.TEXT_DIM);

        p.add(truck);
        p.add(l);

        return p;
    }
}