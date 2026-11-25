
import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.List;

/** Payment Details: every payment of the customer with totals of paid and pending money. */
public class Pay extends Grid {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private final int customerId;

    public Pay(int customerId, String customerName) {

        super("Payment Details", "Your payments for every shipment.",
              new String[]{"ID", "Tracking No.", "Amount", "Method", "Status", "Date"}, 4);

        this.customerId = customerId;
    }

    @Override
    protected List<Object[]> fetch() throws Exception {

        List<Object[]> rows = newRows();

        String sql =
            "SELECT p.payment_id, s.tracking_number, p.amount, p.payment_method, "
            + "p.payment_status, p.payment_date FROM payments p "
            + "JOIN shipments s ON p.shipment_id = s.shipment_id "
            + "WHERE s.customer_id = ? ORDER BY p.payment_date DESC";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal a = rs.getBigDecimal(3);
                    rows.add(new Object[]{
                        rs.getInt(1), rs.getString(2),
                        "\u20B9 " + MONEY.format(a == null ? BigDecimal.ZERO : a),
                        rs.getString(4), rs.getString(5), date(rs.getTimestamp(6))});
                }
            }
        }

        return rows;
    }

    @Override
    protected String summary(List<Object[]> rows) {

        double paid = 0, pending = 0;

        for (Object[] r : rows) {

            double v = Double.parseDouble(r[2].toString().replace("\u20B9", "")
                    .replace(",", "").trim());
            String st = String.valueOf(r[4]).trim().toUpperCase();

            if (st.equals("PAID") || st.equals("COMPLETED") || st.equals("SUCCESS")) paid += v;
            else if (!st.equals("FAILED") && !st.equals("REFUNDED")) pending += v;
        }

        return "Paid: \u20B9 " + MONEY.format(paid) + "     Pending: \u20B9 " + MONEY.format(pending);
    }
}