import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Navigation hub. Knows which class belongs to which admin page and how to
 * move between the Welcome / Login windows.
 */
public final class Nav {

    // the page names used by the menu, the search box and the Centre
    public static final String HOME      = "Home";
    public static final String FEED      = "Activity Feed";
    public static final String ROUTES    = "Routes";
    public static final String INSIGHTS  = "Insights";
    public static final String MESSAGES  = "Messages";
    public static final String STATION   = "Parcel Station";
    public static final String SHIPMENTS = "Shipments";
    public static final String ASSIGN    = "Assign Couriers";
    public static final String STATUS    = "Update Status";
    public static final String USERS     = "Users";
    public static final String COURIERS  = "Delivery Personnel";
    public static final String BRANCHES  = "Branches";
    public static final String PAYMENTS  = "Payments";
    public static final String REPORTS   = "Reports";

    /** Page names grouped for the sidebar: {section, page, page, ...}. */
    public static final String[][] ADMIN_MENU = {
        {"OVERVIEW",   HOME, FEED, ROUTES, INSIGHTS, MESSAGES},
        {"OPERATIONS", STATION, SHIPMENTS, ASSIGN, STATUS},
        {"PEOPLE",     USERS, COURIERS},
        {"BUSINESS",   BRANCHES, PAYMENTS, REPORTS}
    };

    private Nav() { }

    /** Every admin page name (used by the quick search). */
    public static List<String> adminPages() {

        java.util.ArrayList<String> out = new java.util.ArrayList<>();

        for (String[] section : ADMIN_MENU) {
            out.addAll(Arrays.asList(section).subList(1, section.length));
        }

        return Collections.unmodifiableList(out);
    }

    /** Build the screen (class) that belongs to an admin page name. */
    public static JComponent adminPage(String name, int adminId,
                                       Layout layout, Consumer<String> go) {

        switch (name) {

            case HOME:      return new Centre(layout, go);
            case FEED:      return padded(new Feed(true, true, 100));
            case ROUTES:    return new Routes();
            case INSIGHTS:  return new Insights();
            case MESSAGES:  return new Chat(adminId, "ADMIN", "Admin");
            case STATION:   return new Station(adminId);
            case SHIPMENTS: return new Shipments();
            case ASSIGN:    return new Assign();
            case STATUS:    return new Status(adminId);
            case USERS:     return new Users();
            case COURIERS:  return new Couriers();
            case BRANCHES:  return new Branches();
            case PAYMENTS:  return new Payments();
            case REPORTS:   return new Reports();

            default:
                return new Empty("Page not found",
                    "\"" + name + "\" is not available.");
        }
    }

    private static JComponent padded(JComponent inner) {

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.PAPER);
        p.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        p.add(inner, BorderLayout.CENTER);

        return p;
    }

    // ----------------------------------------------------- window movement

    public static void toWelcome(Window from) {

        if (from != null) from.dispose();

        SwingUtilities.invokeLater(() -> new Welcome());
    }
}