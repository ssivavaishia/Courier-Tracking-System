import javax.swing.*;

/**
 * Customer window (SUDWAR design). Same menu and same pages as before, plus
 * Parcel Map and Chat.
 */
public class CustHome extends Shell {

    private final int customerId;
    private final String customerName;

    public CustHome() {
        this(1, "Customer");
    }

    public CustHome(int customerId, String customerName) {

        super("SUDWAR - Customer Dashboard", customerName, "Customer",
            new String[][]{
                {"Dashboard", "home"},
                {"Book Shipment", "plus"},
                {"Track Shipment", "track"},
                {"My Shipments", "boxes"},
                {"Parcel Map", "map"},
                {"Chat", "chat"},
                {"Payment Details", "rupee"},
                {"Delivery Exception", "alert"},
                {"My Profile", "user"},
                {"Contact Support", "help"}
            });

        this.customerId = customerId;
        this.customerName = customerName;

        start("Dashboard");
    }

    @Override
    protected String subtitleFor(String page) {
        return page.equals("Dashboard")
            ? "Here's what's happening with your shipments today." : page;
    }

    @Override
    protected JComponent page(String name) {

        switch (name) {
            case "Dashboard":          return new Home("CUSTOMER", customerId, customerName, this::open);
            case "Book Shipment":      return new Book(customerId, customerName);
            case "Track Shipment":     return new Track(customerId, customerName);
            case "My Shipments":       return new Orders(customerId, customerName);
            case "Parcel Map":         return new MyRoute("CUSTOMER", customerId);
            case "Chat":               return new Chat(customerId, "CUSTOMER", customerName);
            case "Payment Details":    return new Pay(customerId, customerName);
            case "Delivery Exception": return new Issue(customerId, customerName);
            case "My Profile":         return new Profile(customerId, customerName);
            case "Contact Support":    return new Support(customerId, customerName);
            default:
                return new Empty("Page not found", name + " is not available.");
        }
    }
}