import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a text address into latitude / longitude using the free
 * OpenStreetMap Nominatim service. Results are cached in memory, requests
 * are limited to one per 1.1 seconds (Nominatim's usage policy), and every
 * call blocks, so call it from a background thread, never from the UI.
 *
 * Needs an internet connection. Returns null when the address cannot be found.
 */
public final class Geo {

    /** Nominatim asks every app to identify itself. Put your own contact here. */
    public static final String AGENT = "SUDWAR-Courier-App/1.0 (admin@sudwar.com)";

    private static final Map<String, double[]> FOUND = new ConcurrentHashMap<>();
    private static final Set<String> MISSED = ConcurrentHashMap.newKeySet();
    private static final Pattern LATLON = Pattern.compile(
        "\"lat\"\\s*:\\s*\"(-?[0-9.]+)\".*?\"lon\"\\s*:\\s*\"(-?[0-9.]+)\"",
        Pattern.DOTALL);

    private static long lastCall = 0;

    /** Set to true when the network could not be reached at all. */
    public static volatile boolean offline = false;

    private Geo() { }

    public static synchronized double[] locate(String address) {

        if (address == null || address.trim().isEmpty()) return null;

        String key = address.trim().toLowerCase();

        double[] cached = FOUND.get(key);
        if (cached != null) return cached;
        if (MISSED.contains(key)) return null;

        double[] r = query(address);

        // informal street names often fail: try just the last parts (area, city)
        if (r == null && !offline) {
            String shorter = tail(address);
            if (shorter != null) r = query(shorter);
        }

        if (r == null) {
            if (!offline) MISSED.add(key);
        } else {
            FOUND.put(key, r);
        }

        return r;
    }

    private static String tail(String address) {

        String[] parts = address.replace('\n', ',').split(",");
        java.util.List<String> keep = new java.util.ArrayList<>();

        for (String p : parts) {
            if (!p.trim().isEmpty()) keep.add(p.trim());
        }

        if (keep.size() <= 2) return null;

        int from = Math.max(0, keep.size() - 3);

        return String.join(", ", keep.subList(from, keep.size()));
    }

    private static double[] query(String q) {

        try {

            long wait = 1100 - (System.currentTimeMillis() - lastCall);

            if (wait > 0) Thread.sleep(wait);

            lastCall = System.currentTimeMillis();

            URL url = new URL(
                "https://nominatim.openstreetmap.org/search?format=json&limit=1&q="
                + URLEncoder.encode(q, "UTF-8"));

            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestProperty("User-Agent", AGENT);
            c.setConnectTimeout(6000);
            c.setReadTimeout(8000);

            if (c.getResponseCode() != 200) return null;

            StringBuilder sb = new StringBuilder();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    c.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = in.readLine()) != null) sb.append(line);
            }

            offline = false;

            Matcher m = LATLON.matcher(sb);

            if (m.find()) {
                return new double[]{
                    Double.parseDouble(m.group(1)),
                    Double.parseDouble(m.group(2))};
            }

            return null;

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception ex) {
            offline = true;
            return null;
        }
    }
}
