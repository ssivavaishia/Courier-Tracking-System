import javax.swing.*;

/**
 * Delivery window (SUDWAR design). Same pages as before, plus My Route
 * (street map) and Chat.
 */
public class DelHome extends Shell {

    private final int deliveryPersonId;
    private final String deliveryPersonName;

    public DelHome(int deliveryPersonId, String deliveryPersonName) {

        super("SUDWAR - Delivery Dashboard", deliveryPersonName, "Delivery partner",
            new String[][]{
                {"Dashboard", "home"},
                {"My Route", "route"},
                {"Assigned Shipments", "boxes"},
                {"Update Delivery Status", "edit"},
                {"Confirm Delivery", "check"},
                {"Delivery History", "clock"},
                {"Chat", "chat"},
                {"My Profile", "user"}
            });

        this.deliveryPersonId = deliveryPersonId;
        this.deliveryPersonName = deliveryPersonName;

        start("Dashboard");
    }

    @Override
    protected String subtitleFor(String page) {
        return page.equals("Dashboard")
            ? "Here's your route and deliveries for today." : page;
    }

    @Override
    protected JComponent page(String name) {

        switch (name) {
            case "Dashboard":              return new Home("DELIVERY", deliveryPersonId, deliveryPersonName, this::open);
            case "My Route":               return new MyRoute("DELIVERY", deliveryPersonId);
            case "Assigned Shipments":     return new Assigned(deliveryPersonId);
            case "Update Delivery Status": return new Update(deliveryPersonId);
            case "Confirm Delivery":       return new Confirm(deliveryPersonId);
            case "Delivery History":       return new History(deliveryPersonId);
            case "Chat":                   return new Chat(deliveryPersonId, "DELIVERY", deliveryPersonName);
            case "My Profile":             return new DelProfile(deliveryPersonId);
            default:
                return new Empty("Page not found", name + " is not available.");
        }
    }
}