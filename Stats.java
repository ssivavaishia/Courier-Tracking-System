import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Floating statistic chips shown over the map. Each chip has the accent
 * colour of its status; the numbers count up smoothly when they change.
 * Which chips appear (and their order) comes from Layout.
 */
public class Stats extends JPanel {

    private final Layout layout;
    private final Map<String, Chip> chips = new LinkedHashMap<>();
    private final Timer refresh;

    public Stats(Layout layout) {

        this.layout = layout;

        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));

        chips.put("total",      new Chip("TOTAL SHIPMENTS",    new Color(120, 130, 170)));
        chips.put("transit",    new Chip("IN TRANSIT",         Colors.IN_TRANSIT));
        chips.put("ofd",        new Chip("OUT FOR DELIVERY",   Colors.OUT));
        chips.put("done",       new Chip("DELIVERED TODAY",    Colors.DELIVERED));
        chips.put("unassigned", new Chip("NEEDS A COURIER", Colors.BOOKED));

        applyLayout();

        refresh = new Timer(45_000, e -> reload());
    }

    /** Re-add the chips the user wants, in the order they chose. */
    public void applyLayout() {

        removeAll();

        for (String id : layout.order("stats")) {
            if (layout.visible(id) && chips.containsKey(id)) {
                add(chips.get(id));
            }
        }

        revalidate();
        repaint();
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

        new SwingWorker<int[], Void>() {

            @Override
            protected int[] doInBackground() throws Exception {

                String sql =
                    "SELECT COUNT(*), "
                    + "SUM(UPPER(delivery_status) = 'IN TRANSIT'), "
                    + "SUM(UPPER(delivery_status) = 'OUT FOR DELIVERY'), "
                    + "SUM(UPPER(delivery_status) = 'DELIVERED' "
                    + "    AND DATE(delivery_date) = CURDATE()), "
                    + "SUM(UPPER(delivery_status) NOT IN "
                    + "    ('DELIVERED','CANCELLED') "
                    + "    AND delivery_person_id IS NULL) "
                    + "FROM shipments";

                try (Connection con = Db.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        return new int[]{
                            rs.getInt(1), rs.getInt(2), rs.getInt(3),
                            rs.getInt(4), rs.getInt(5)};
                    }
                }

                return new int[5];
            }

            @Override
            protected void done() {

                try {
                    int[] v = get();
                    chips.get("total").setValue(v[0]);
                    chips.get("transit").setValue(v[1]);
                    chips.get("ofd").setValue(v[2]);
                    chips.get("done").setValue(v[3]);
                    chips.get("unassigned").setValue(v[4]);
                } catch (Exception ex) {
                    for (Chip c : chips.values()) c.setValue(0);
                }
            }

        }.execute();
    }

    /** One translucent chip with a count-up number. */
    private static class Chip extends JComponent {

        private final String title;
        private final Color accent;
        private double shown = 0;
        private int target = 0;
        private final Timer anim;

        Chip(String title, Color accent) {

            this.title = title;
            this.accent = accent;

            setPreferredSize(new Dimension(136, 66));
            setOpaque(false);

            anim = new Timer(20, e -> {

                double diff = target - shown;

                if (Math.abs(diff) < 0.5) {
                    shown = target;
                    ((Timer) e.getSource()).stop();
                } else {
                    shown += diff * 0.18;
                }

                repaint();
            });
        }

        void setValue(int v) {
            target = v;
            anim.start();
        }

        @Override
        public void removeNotify() {
            anim.stop();
            super.removeNotify();
        }

        @Override
        protected void paintComponent(Graphics g0) {

            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);

            int w = getWidth() - 1, h = getHeight() - 1;

            g.setColor(Theme.alpha(Theme.NAVY2, 225));
            g.fillRoundRect(0, 0, w, h, 16, 16);

            g.setColor(Theme.alpha(accent, 150));
            g.setStroke(new BasicStroke(1.2f));
            g.drawRoundRect(0, 0, w, h, 16, 16);

            g.setColor(accent);
            g.fillRoundRect(14, h - 7, 28, 4, 4, 4);

            g.setFont(Theme.font(Font.BOLD, 9));
            g.setColor(Theme.TEXT_DIM);
            g.drawString(title, 14, 19);

            g.setFont(Theme.font(Font.BOLD, 26));
            g.setColor(Theme.TEXT_LIGHT);
            g.drawString(String.valueOf(Math.round(shown)), 14, 47);

            g.dispose();
        }
    }
}