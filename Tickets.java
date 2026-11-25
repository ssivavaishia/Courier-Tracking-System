
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.List;

/**
 * Admin: every support ticket of every customer, including cancellation and
 * return requests ("[CANCEL REQUEST] tracking number"). The admin can mark a
 * ticket In Progress or Resolved. To act on a cancel request, open
 * Shipments and use Cancel Shipment.
 */
public class Tickets extends Grid {

    public Tickets() {

        super("Support Tickets", "Customer requests, including cancellation and return requests.",
              new String[]{"Ticket ID", "Customer", "Subject", "Status", "Created"}, 3);

        addAction(button("Mark In Progress", new Color(34, 47, 86), "IN PROGRESS"));
        addAction(button("Mark Resolved", Theme.RED, "RESOLVED"));
    }

    private JButton button(String text, Color bg, String status) {

        JButton b = new JButton(text);
        b.setBackground(bg);
        b.addActionListener(e -> mark(status));

        return b;
    }

    @Override
    protected List<Object[]> fetch() throws Exception {

        List<Object[]> rows = newRows();

        String sql =
            "SELECT t.ticket_id, u.full_name, t.subject, t.ticket_status, t.created_at "
            + "FROM support_tickets t JOIN users u ON u.user_id = t.customer_id "
            + "ORDER BY t.created_at DESC";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), date(rs.getTimestamp(5))});
            }
        }

        return rows;
    }

    private void mark(String status) {

        Object[] r = selected();

        if (r == null) {
            JOptionPane.showMessageDialog(this, "Please select a ticket first.");
            return;
        }

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                "UPDATE support_tickets SET ticket_status = ? WHERE ticket_id = ?")) {

            ps.setString(1, status);
            ps.setInt(2, (Integer) r[0]);
            ps.executeUpdate();

            reload();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not update the ticket: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}