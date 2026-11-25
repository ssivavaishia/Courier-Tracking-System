import java.awt.Color;

/** One accent colour per shipment status, used everywhere in SUDWAR. */
public final class Colors {

    public static final Color BOOKED     = new Color(47, 128, 255);
    public static final Color PICKED_UP  = new Color(20, 184, 166);
    public static final Color IN_TRANSIT = new Color(245, 158, 11);
    public static final Color OUT        = new Color(236, 72, 153);
    public static final Color DELIVERED  = new Color(34, 197, 94);
    public static final Color CANCELLED  = new Color(239, 68, 68);
    public static final Color PENDING    = new Color(168, 85, 247);
    public static final Color OTHER      = new Color(150, 156, 175);

    private Colors() { }

    public static Color of(String status) {

        String s = norm(status);

        switch (s) {
            case "BOOKED":           return BOOKED;
            case "PENDING":          return PENDING;
            case "PICKED UP":        return PICKED_UP;
            case "IN TRANSIT":       return IN_TRANSIT;
            case "OUT FOR DELIVERY": return OUT;
            case "DELIVERED":
            case "PAID":
            case "COMPLETED":
            case "SUCCESS":
            case "ACTIVE":
            case "RESOLVED":
            case "CLOSED":           return DELIVERED;
            case "CANCELLED":
            case "FAILED":
            case "REJECTED":
            case "INACTIVE":
            case "REFUNDED":         return CANCELLED;
            case "OPEN":
            case "IN PROGRESS":
            case "UNPAID":
            case "ASSIGNED":         return BOOKED;
            default:                 return OTHER;
        }
    }

    /** How far along the journey a shipment is, from 0.0 to 1.0. */
    public static double progress(String status) {

        switch (norm(status)) {
            case "BOOKED":
            case "PENDING":          return 0.15;
            case "PICKED UP":        return 0.35;
            case "IN TRANSIT":       return 0.60;
            case "OUT FOR DELIVERY": return 0.85;
            case "DELIVERED":        return 1.0;
            default:                 return 0.0;
        }
    }

    /** Text colour that stays readable on top of a status colour. */
    public static Color inkOn(Color c) {
        double l = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
        return l > 0.55 ? new Color(30, 24, 10) : Color.WHITE;
    }

    public static String norm(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }
}