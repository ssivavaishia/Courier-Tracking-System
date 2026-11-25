import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Real street map (OpenStreetMap tiles) with pan, zoom and status-coloured
 * pins for every open delivery. Addresses are turned into coordinates with
 * Geo (and saved in shipments.dest_lat / dest_lng so each address is looked
 * up only once).
 *
 * Drag to pan, mouse wheel or +/- to zoom, "Fit" to show all pins.
 * If there is no internet the map falls back to a schematic city view.
 *
 * Filters: new MapView()                      every open delivery (admin)
 *          new MapView("COURIER", userId)     one delivery person's stops
 *          new MapView("CUSTOMER", userId)    one customer's parcels
 */
public class MapView extends JPanel {

    public static class Stop {
        public int id, personId, order;
        public String tracking, receiver, address, status, person;
        public double lat = Double.NaN, lng = Double.NaN;
        double x, y;                       // schematic fallback position

        public boolean located() {
            return !Double.isNaN(lat) && !Double.isNaN(lng);
        }
    }

    private static final Color[] PALETTE = {
        new Color(66, 133, 244), new Color(245, 166, 35),
        new Color(52, 168, 83),  new Color(171, 71, 188),
        new Color(0, 172, 193),  new Color(255, 107, 129),
        new Color(160, 196, 60), new Color(255, 138, 76)
    };

    private final String filterMode;
    private final int filterId;

    private final List<Stop> stops = new ArrayList<>();
    private final List<Runnable> loadListeners = new ArrayList<>();
    private final Timer anim;
    private final Timer refresh;

    private boolean showLegend = true;
    private boolean dark = true;
    private int phase = 0;
    private String error = null;

    // map view
    private double centerLat = 20.59, centerLng = 78.96;
    private int zoom = 4;
    private boolean userMoved = false;
    private boolean geocodingDone = false;
    private int toLocate = 0, located = 0;
    private double hubLat = Double.NaN, hubLng = Double.NaN;
    private int dragX, dragY;

    private final Rectangle zoomIn = new Rectangle();
    private final Rectangle zoomOut = new Rectangle();
    private final Rectangle fitBox = new Rectangle();

    public MapView() {
        this("ALL", 0);
    }

    public MapView(String filterMode, int filterId) {

        this.filterMode = filterMode;
        this.filterId = filterId;

        setBackground(Theme.NAVY);
        setOpaque(true);
        setPreferredSize(new Dimension(600, 400));

        ToolTipManager.sharedInstance().registerComponent(this);

        MouseAdapter mouse = new MouseAdapter() {

            @Override public void mousePressed(MouseEvent e) {
                dragX = e.getX();
                dragY = e.getY();
            }

            @Override public void mouseDragged(MouseEvent e) {

                if (!geoMode()) return;

                double cx = wx(centerLng, zoom) - (e.getX() - dragX);
                double cy = wy(centerLat, zoom) - (e.getY() - dragY);

                centerLng = lonOf(cx, zoom);
                centerLat = Math.max(-84, Math.min(84, latOf(cy, zoom)));

                dragX = e.getX();
                dragY = e.getY();
                userMoved = true;

                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                repaint();
            }

            @Override public void mouseReleased(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
            }

            @Override public void mouseClicked(MouseEvent e) {

                if (zoomIn.contains(e.getPoint())) { zoomBy(1, getWidth() / 2, getHeight() / 2); return; }
                if (zoomOut.contains(e.getPoint())) { zoomBy(-1, getWidth() / 2, getHeight() / 2); return; }
                if (fitBox.contains(e.getPoint())) { userMoved = false; fit(); return; }

                if (e.getClickCount() == 2) zoomBy(1, e.getX(), e.getY());
            }

            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                zoomBy(e.getWheelRotation() < 0 ? 1 : -1, e.getX(), e.getY());
            }

            @Override public void mouseMoved(MouseEvent e) {

                Stop s = stopAt(e.getX(), e.getY());

                setToolTipText(s == null ? null
                    : "<html><b>" + s.tracking + "</b><br>" + s.receiver
                      + "<br>" + s.status + "<br>"
                      + (s.personId > 0 ? "Courier: " + s.person
                                        : "Not assigned yet")
                      + "</html>");

                boolean hand = s != null || zoomIn.contains(e.getPoint())
                        || zoomOut.contains(e.getPoint())
                        || fitBox.contains(e.getPoint());

                setCursor(Cursor.getPredefinedCursor(
                    hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };

        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);

        anim = new Timer(50, e -> { phase = (phase + 1) % 1000; repaint(); });
        refresh = new Timer(45_000, e -> reload());
    }

    public void setShowLegend(boolean show) { this.showLegend = show; repaint(); }

    /** Dark tint over the street map (used on the dark dashboard). */
    public void setDark(boolean dark) { this.dark = dark; repaint(); }

    public List<Stop> stops() { return stops; }

    public void addLoadListener(Runnable r) { loadListeners.add(r); }

    public static Color colorFor(int personId) {
        return personId <= 0 ? Colors.OTHER : PALETTE[personId % PALETTE.length];
    }

    @Override
    public void addNotify() {
        super.addNotify();
        reload();
        anim.start();
        refresh.start();
    }

    @Override
    public void removeNotify() {
        anim.stop();
        refresh.stop();
        super.removeNotify();
    }

    // ------------------------------------------------------------ map maths

    private static double wx(double lng, int z) {
        return (lng + 180.0) / 360.0 * (1L << z) * 256.0;
    }

    private static double wy(double lat, int z) {
        double r = Math.toRadians(lat);
        return (1 - Math.log(Math.tan(r) + 1 / Math.cos(r)) / Math.PI)
                / 2 * (1L << z) * 256.0;
    }

    private static double lonOf(double x, int z) {
        return x / ((1L << z) * 256.0) * 360.0 - 180.0;
    }

    private static double latOf(double y, int z) {
        double n = Math.PI - 2 * Math.PI * y / ((1L << z) * 256.0);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    private Point toScreen(double lat, double lng) {
        return new Point(
            (int) Math.round(getWidth() / 2.0 + wx(lng, zoom) - wx(centerLng, zoom)),
            (int) Math.round(getHeight() / 2.0 + wy(lat, zoom) - wy(centerLat, zoom)));
    }

    private void zoomBy(int delta, int mx, int my) {

        if (!geoMode()) return;

        int z2 = Math.max(2, Math.min(18, zoom + delta));

        if (z2 == zoom) return;

        double cx = wx(centerLng, zoom), cy = wy(centerLat, zoom);
        double lng0 = lonOf(cx + (mx - getWidth() / 2.0), zoom);
        double lat0 = latOf(cy + (my - getHeight() / 2.0), zoom);

        double cx2 = wx(lng0, z2) - (mx - getWidth() / 2.0);
        double cy2 = wy(lat0, z2) - (my - getHeight() / 2.0);

        zoom = z2;
        centerLng = lonOf(cx2, z2);
        centerLat = Math.max(-84, Math.min(84, latOf(cy2, z2)));
        userMoved = true;

        repaint();
    }

    /** Zoom and centre so every located pin is visible. */
    public void fit() {

        double minLa = 999, maxLa = -999, minLn = 999, maxLn = -999;
        int n = 0;

        for (Stop s : stops) {
            if (!s.located()) continue;
            minLa = Math.min(minLa, s.lat); maxLa = Math.max(maxLa, s.lat);
            minLn = Math.min(minLn, s.lng); maxLn = Math.max(maxLn, s.lng);
            n++;
        }

        if (!Double.isNaN(hubLat)) {
            minLa = Math.min(minLa, hubLat); maxLa = Math.max(maxLa, hubLat);
            minLn = Math.min(minLn, hubLng); maxLn = Math.max(maxLn, hubLng);
            n++;
        }

        if (n == 0 || getWidth() < 50) return;

        centerLat = (minLa + maxLa) / 2;
        centerLng = (minLn + maxLn) / 2;

        int z = 16;

        for (; z > 3; z--) {
            double w = Math.abs(wx(maxLn, z) - wx(minLn, z));
            double h = Math.abs(wy(minLa, z) - wy(maxLa, z));
            if (w < getWidth() - 160 && h < getHeight() - 140) break;
        }

        zoom = z;
        repaint();
    }

    /** True when the real map is shown (false = offline schematic view). */
    private boolean geoMode() {

        if (!geocodingDone) return true;

        for (Stop s : stops) if (s.located()) return true;

        return !Double.isNaN(hubLat);
    }

    // ----------------------------------------------------------------- data

    public void reload() {

        Tiles.fail = 0;       // retry tiles if the network came back

        new SwingWorker<List<Stop>, Void>() {

            @Override
            protected List<Stop> doInBackground() throws Exception {

                String where = "UPPER(s.delivery_status) NOT IN ('DELIVERED','CANCELLED')";

                if (filterMode.equals("COURIER"))  where += " AND s.delivery_person_id = ?";
                if (filterMode.equals("CUSTOMER")) where += " AND s.customer_id = ?";

                String sql =
                    "SELECT s.shipment_id, s.tracking_number, s.receiver_name, "
                    + "s.receiver_address, s.delivery_status, "
                    + "s.delivery_person_id, u.full_name, s.dest_lat, s.dest_lng "
                    + "FROM shipments s "
                    + "LEFT JOIN users u ON s.delivery_person_id = u.user_id "
                    + "WHERE " + where + " ORDER BY s.shipment_id LIMIT 300";

                List<Stop> out = new ArrayList<>();

                try (Connection con = Db.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {

                    if (!filterMode.equals("ALL")) ps.setInt(1, filterId);

                    try (ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {

                            Stop s = new Stop();
                            s.id = rs.getInt(1);
                            s.tracking = rs.getString(2);
                            s.receiver = rs.getString(3);
                            s.address = rs.getString(4);
                            s.status = rs.getString(5);
                            s.personId = rs.getInt(6);
                            s.person = rs.getString(7);

                            double la = rs.getDouble(8);
                            if (!rs.wasNull()) s.lat = la;
                            double ln = rs.getDouble(9);
                            if (!rs.wasNull()) s.lng = ln;

                            Random r = new Random(
                                String.valueOf(s.address).toLowerCase().hashCode());
                            s.x = 0.07 + 0.86 * r.nextDouble();
                            s.y = 0.14 + 0.74 * r.nextDouble();

                            out.add(s);
                        }
                    }
                }

                return out;
            }

            @Override
            protected void done() {

                try {
                    stops.clear();
                    stops.addAll(get());
                    error = null;
                } catch (Exception ex) {
                    error = "Could not load route data (is the database up to date?)";
                }

                buildRoutes();
                repaint();

                for (Runnable r : loadListeners) r.run();

                startGeocoding();
            }

        }.execute();
    }

    /** Looks up coordinates for stops that do not have them yet. */
    private void startGeocoding() {

        List<Stop> todo = new ArrayList<>();

        for (Stop s : stops) if (!s.located()) todo.add(s);

        toLocate = todo.size();
        located = 0;

        final boolean needHub = Double.isNaN(hubLat);

        if (todo.isEmpty() && !needHub) {
            geocodingDone = true;
            if (!userMoved) fit();
            repaint();
            return;
        }

        geocodingDone = false;

        new SwingWorker<Void, Stop>() {

            @Override
            protected Void doInBackground() {

                int budget = 25;       // addresses per refresh

                if (needHub) {
                    try (Connection con = Db.getConnection();
                         PreparedStatement ps = con.prepareStatement(
                            "SELECT address FROM branches "
                            + "ORDER BY branch_id LIMIT 1");
                         ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {
                            double[] h = Geo.locate(rs.getString(1));
                            if (h != null) { hubLat = h[0]; hubLng = h[1]; }
                        }

                    } catch (SQLException ex) {
                        // the hub is optional
                    }
                }

                for (Stop s : todo) {

                    if (budget-- <= 0 || Geo.offline) break;

                    double[] p = Geo.locate(s.address);

                    if (p == null) continue;

                    s.lat = p[0];
                    s.lng = p[1];

                    try (Connection con = Db.getConnection();
                         PreparedStatement ps = con.prepareStatement(
                            "UPDATE shipments SET dest_lat = ?, dest_lng = ? "
                            + "WHERE shipment_id = ?")) {

                        ps.setDouble(1, p[0]);
                        ps.setDouble(2, p[1]);
                        ps.setInt(3, s.id);
                        ps.executeUpdate();

                    } catch (SQLException ex) {
                        // coordinates still work for this session
                    }

                    publish(s);
                }

                return null;
            }

            @Override
            protected void process(List<Stop> chunk) {

                located += chunk.size();

                buildRoutes();

                if (!userMoved) fit();

                repaint();
            }

            @Override
            protected void done() {

                geocodingDone = true;

                buildRoutes();

                if (!userMoved) fit();

                repaint();

                for (Runnable r : loadListeners) r.run();
            }

        }.execute();
    }

    /** Number each person's stops by nearest-neighbour order. */
    private void buildRoutes() {

        boolean geo = false;
        for (Stop s : stops) if (s.located()) { geo = true; break; }

        Map<Integer, List<Stop>> byPerson = new LinkedHashMap<>();

        for (Stop s : stops) {
            s.order = 0;
            if (s.personId > 0 && (!geo || s.located())) {
                byPerson.computeIfAbsent(s.personId, k -> new ArrayList<>()).add(s);
            }
        }

        for (List<Stop> group : byPerson.values()) {

            List<Stop> left = new ArrayList<>(group);
            Stop cur = left.remove(0);
            cur.order = 1;
            int n = 2;

            while (!left.isEmpty()) {

                Stop best = left.get(0);
                double bd = Double.MAX_VALUE;

                for (Stop s : left) {
                    double d = geo
                        ? Math.hypot((s.lat - cur.lat), (s.lng - cur.lng)
                                * Math.cos(Math.toRadians(cur.lat)))
                        : Math.hypot(s.x - cur.x, s.y - cur.y);
                    if (d < bd) { bd = d; best = s; }
                }

                best.order = n++;
                left.remove(best);
                cur = best;
            }
        }
    }

    /** Stops of one person in route order. */
    public List<Stop> route(int personId) {

        List<Stop> r = new ArrayList<>();

        for (Stop s : stops) if (s.personId == personId && s.order > 0) r.add(s);

        r.sort(Comparator.comparingInt(a -> a.order));

        return r;
    }

    // ------------------------------------------------------------- painting

    private Point pos(Stop s) {

        if (geoMode()) {
            return s.located() ? toScreen(s.lat, s.lng) : null;
        }

        int m = 26;

        return new Point(
            (int) (m + s.x * (getWidth() - 2 * m)),
            (int) (m + s.y * (getHeight() - 2 * m)));
    }

    private Point hub() {

        if (geoMode()) {
            return Double.isNaN(hubLat) ? null : toScreen(hubLat, hubLng);
        }

        return new Point(getWidth() / 2, (int) (getHeight() * 0.52));
    }

    private Stop stopAt(int mx, int my) {

        for (Stop s : stops) {
            Point p = pos(s);
            if (p != null && p.distance(mx, my) <= 13) return s;
        }

        return null;
    }

    @Override
    protected void paintComponent(Graphics g0) {

        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);

        int w = getWidth(), h = getHeight();

        g.setColor(Theme.NAVY);
        g.fillRect(0, 0, w, h);

        boolean geo = geoMode();

        if (geo) {
            drawTiles(g, w, h);
        } else {
            drawCity(g, w, h);
        }

        drawRoutes(g);
        drawHub(g);
        drawPins(g);

        if (showLegend) drawLegend(g, w, h);

        if (geo) drawControls(g, w);

        drawStatus(g, w, h, geo);

        g.dispose();
    }

    private void drawTiles(Graphics2D g, int w, int h) {

        // faint grid shows through until tiles arrive
        g.setColor(Theme.alpha(Color.WHITE, 10));
        for (int x = 0; x < w; x += 64) g.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += 64) g.drawLine(0, y, w, y);

        double cx = wx(centerLng, zoom), cy = wy(centerLat, zoom);
        double left = cx - w / 2.0, top = cy - h / 2.0;

        int n = 1 << zoom;

        int tx0 = (int) Math.floor(left / 256), tx1 = (int) Math.floor((left + w) / 256);
        int ty0 = (int) Math.floor(top / 256),  ty1 = (int) Math.floor((top + h) / 256);

        for (int tx = tx0; tx <= tx1; tx++) {
            for (int ty = ty0; ty <= ty1; ty++) {

                if (ty < 0 || ty >= n) continue;

                int wrapped = ((tx % n) + n) % n;

                BufferedImage img = Tiles.get(zoom, wrapped, ty, this::repaint);

                if (img != null) {
                    g.drawImage(img,
                        (int) Math.round(tx * 256 - left),
                        (int) Math.round(ty * 256 - top), null);
                }
            }
        }

        if (dark) {
            g.setColor(new Color(10, 14, 34, 120));
            g.fillRect(0, 0, w, h);
        }
    }

    private void drawCity(Graphics2D g, int w, int h) {

        Path2D river = new Path2D.Double();
        river.moveTo(0, h * 0.78);
        river.curveTo(w * 0.25, h * 0.60, w * 0.45, h * 0.95, w * 0.7, h * 0.80);
        river.curveTo(w * 0.85, h * 0.72, w * 0.95, h * 0.80, w, h * 0.74);
        g.setColor(new Color(40, 90, 170, 60));
        g.setStroke(new BasicStroke(22f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(river);

        g.setStroke(new BasicStroke(1f));
        g.setColor(Theme.alpha(Color.WHITE, 12));
        for (int x = 0; x < w; x += 46) g.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += 46) g.drawLine(0, y, w, y);

        g.setStroke(new BasicStroke(3f));
        g.setColor(Theme.alpha(Color.WHITE, 26));
        double[] fx = {0.2, 0.5, 0.78};
        double[] fy = {0.28, 0.52, 0.72};
        for (double f : fx) g.drawLine((int) (w * f), 0, (int) (w * f), h);
        for (double f : fy) g.drawLine(0, (int) (h * f), w, (int) (h * f));
    }

    private void drawRoutes(Graphics2D g) {

        Map<Integer, List<Stop>> groups = new LinkedHashMap<>();

        for (Stop s : stops) {
            if (s.personId > 0 && s.order > 0) {
                groups.computeIfAbsent(s.personId, k -> new ArrayList<>()).add(s);
            }
        }

        for (Map.Entry<Integer, List<Stop>> e : groups.entrySet()) {

            List<Stop> r = e.getValue();
            r.sort(Comparator.comparingInt(a -> a.order));

            g.setColor(Theme.alpha(colorFor(e.getKey()), 190));
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND, 1f, new float[]{7f, 7f}, -phase * 0.6f));

            Point prev = hub();

            for (Stop s : r) {
                Point p = pos(s);
                if (p == null) continue;
                if (prev != null) g.drawLine(prev.x, prev.y, p.x, p.y);
                prev = p;
            }
        }
    }

    private void drawHub(Graphics2D g) {

        Point p = hub();

        if (p == null) return;

        double pulse = (Math.sin(phase * 0.12) + 1) / 2;

        g.setColor(Theme.alpha(Colors.IN_TRANSIT, (int) (70 - pulse * 50)));
        double rr = 18 + pulse * 14;
        g.fill(new Ellipse2D.Double(p.x - rr, p.y - rr, rr * 2, rr * 2));

        g.setColor(Colors.IN_TRANSIT);
        g.fill(new Ellipse2D.Double(p.x - 10, p.y - 10, 20, 20));

        g.setColor(Color.WHITE);
        g.setFont(Theme.font(Font.BOLD, 9));
        g.drawString("HUB", p.x - 9, p.y + 3);
    }

    private void drawPins(Graphics2D g) {

        g.setFont(Theme.font(Font.BOLD, 10));

        for (Stop s : stops) {

            Point p = pos(s);

            if (p == null) continue;

            Color c = colorFor(s.personId);

            g.setColor(Theme.alpha(c, 70));
            g.fill(new Ellipse2D.Double(p.x - 14, p.y - 14, 28, 28));

            if (s.personId > 0 && s.order > 0) {
                g.setColor(c);
                g.fill(new Ellipse2D.Double(p.x - 10, p.y - 10, 20, 20));
                g.setColor(Color.WHITE);
                String t = String.valueOf(s.order);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(t, p.x - fm.stringWidth(t) / 2, p.y + 4);
            } else {
                g.setColor(Colors.of(s.status));
                g.fill(new Ellipse2D.Double(p.x - 8, p.y - 8, 16, 16));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(1.4f));
                g.draw(new Ellipse2D.Double(p.x - 8, p.y - 8, 16, 16));
            }
        }
    }

    private void drawLegend(Graphics2D g, int w, int h) {

        Map<Integer, String> people = new LinkedHashMap<>();

        for (Stop s : stops) {
            if (s.personId > 0) people.putIfAbsent(s.personId, s.person);
        }

        if (people.isEmpty()) return;

        int rows = Math.min(people.size(), 6);
        int bw = 170, bh = 14 + rows * 20;
        int x = w - bw - 14, y = h - bh - 32;

        g.setColor(Theme.alpha(Theme.NAVY2, 220));
        g.fillRoundRect(x, y, bw, bh, 14, 14);

        g.setFont(Theme.font(Font.PLAIN, 12));

        int i = 0;

        for (Map.Entry<Integer, String> e : people.entrySet()) {

            if (i >= rows) break;

            int ry = y + 12 + i * 20;

            g.setColor(colorFor(e.getKey()));
            g.fillOval(x + 12, ry, 10, 10);

            g.setColor(Theme.TEXT_LIGHT);
            String n = e.getValue() == null ? "Courier" : e.getValue();
            if (n.length() > 18) n = n.substring(0, 17) + "…";
            g.drawString(n, x + 30, ry + 10);

            i++;
        }
    }

    private void drawControls(Graphics2D g, int w) {

        int x = w - 50;

        zoomIn.setBounds(x, 14, 36, 30);
        zoomOut.setBounds(x, 48, 36, 30);
        fitBox.setBounds(x, 82, 36, 30);

        String[] labels = {"+", "-", "Fit"};
        Rectangle[] boxes = {zoomIn, zoomOut, fitBox};

        for (int i = 0; i < 3; i++) {

            Rectangle b = boxes[i];

            g.setColor(Theme.alpha(Theme.NAVY2, 235));
            g.fillRoundRect(b.x, b.y, b.width, b.height, 10, 10);

            g.setColor(Theme.TEXT_LIGHT);
            g.setFont(Theme.font(Font.BOLD, i == 2 ? 11 : 16));

            FontMetrics fm = g.getFontMetrics();

            g.drawString(labels[i],
                b.x + (b.width - fm.stringWidth(labels[i])) / 2,
                b.y + (b.height + fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    private void drawStatus(Graphics2D g, int w, int h, boolean geo) {

        g.setFont(Theme.font(Font.PLAIN, 10));

        String right = geo ? "© OpenStreetMap contributors" : "Schematic view (offline)";

        g.setColor(geo ? new Color(255, 255, 255, 200) : Theme.alpha(Theme.TEXT_DIM, 200));

        if (geo) {
            int tw = g.getFontMetrics().stringWidth(right);
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRect(w - tw - 12, h - 18, tw + 12, 18);
            g.setColor(new Color(255, 255, 255, 220));
            g.drawString(right, w - tw - 6, h - 6);
        } else {
            g.drawString(right, w - 130, h - 10);
        }

        String left = null;

        if (error != null) {
            left = error;
        } else if (!geocodingDone && toLocate > 0) {
            left = "Locating addresses… " + located + " of " + toLocate;
        } else if (geo && Geo.offline && Tiles.ok == 0) {
            left = "No internet: map tiles could not be loaded";
        } else if (geocodingDone && stops.size() > 0) {
            int missing = 0;
            for (Stop s : stops) if (!s.located()) missing++;
            if (missing > 0 && geo) left = missing + " address(es) could not be found on the map";
        }

        if (left != null) {
            g.setFont(Theme.font(Font.BOLD, 11));
            int tw = g.getFontMetrics().stringWidth(left);
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRoundRect(10, h - 30, tw + 16, 22, 10, 10);
            g.setColor(Color.WHITE);
            g.drawString(left, 18, h - 15);
        }
    }

    // ----------------------------------------------------------- tile cache

    private static final class Tiles {

        private static final int MAX = 500;

        private static final Map<String, BufferedImage> CACHE =
            new LinkedHashMap<String, BufferedImage>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<String, BufferedImage> eldest) {
                    return size() > MAX;
                }
            };

        private static final Set<String> PENDING = new HashSet<>();

        private static final ExecutorService POOL =
            Executors.newFixedThreadPool(3, r -> {
                Thread t = new Thread(r, "sudwar-tiles");
                t.setDaemon(true);
                return t;
            });

        static volatile int ok = 0;
        static volatile int fail = 0;

        static BufferedImage get(int z, int x, int y, Runnable repaint) {

            final String key = z + "/" + x + "/" + y;

            synchronized (CACHE) {

                BufferedImage img = CACHE.get(key);

                if (img != null) return img;
                if (PENDING.contains(key)) return null;
                if (fail >= 3 && ok == 0) return null;   // offline: stop trying

                PENDING.add(key);
            }

            POOL.execute(() -> {

                BufferedImage img = null;

                try {
                    HttpURLConnection c = (HttpURLConnection)
                        new URL("https://tile.openstreetmap.org/" + key + ".png")
                            .openConnection();
                    c.setRequestProperty("User-Agent", Geo.AGENT);
                    c.setConnectTimeout(5000);
                    c.setReadTimeout(6000);

                    if (c.getResponseCode() == 200) {
                        try (java.io.InputStream in = c.getInputStream()) {
                            img = ImageIO.read(in);
                        }
                    }
                } catch (Exception ex) {
                    img = null;
                }

                synchronized (CACHE) {
                    PENDING.remove(key);
                    if (img != null) { CACHE.put(key, img); ok++; }
                    else { fail++; }
                }

                if (img != null) SwingUtilities.invokeLater(repaint);
            });

            return null;
        }
    }
}