import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Compact dark table used on the dashboards: header row, thin separators,
 * coloured status pills and an optional action cell (Track / Cancel buttons).
 */
public class MiniTable extends JPanel {

    /** Builds the action cell for one row. */
    public interface Action {
        JComponent make(String[] row);
    }

    private final String[] heads;
    private final int statusCol;
    private final Action action;
    private final String emptyText;

    public MiniTable(String[] heads, int statusCol, Action action, String emptyText) {

        this.heads = heads;
        this.statusCol = statusCol;
        this.action = action;
        this.emptyText = emptyText;

        setOpaque(false);
        setLayout(new GridBagLayout());

        rows(new java.util.ArrayList<String[]>());
    }

    public void rows(List<String[]> rows) {

        removeAll();

        int cols = heads.length + (action != null ? 1 : 0);

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.weighty = 0;
        g.insets = new Insets(0, 0, 0, 0);

        // header
        for (int c = 0; c < cols; c++) {

            JLabel h = new JLabel(c < heads.length ? heads[c] : "Action");
            h.setFont(Theme.font(Font.BOLD, 12));
            h.setForeground(Theme.DIM);
            h.setOpaque(true);
            h.setBackground(new Color(11, 17, 38));
            h.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 8));

            g.gridx = c;
            g.weightx = c == 0 ? 1.3 : 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.anchor = GridBagConstraints.WEST;
            add(h, g);
        }

        if (rows.isEmpty()) {

            JLabel none = new JLabel(emptyText, SwingConstants.CENTER);
            none.setFont(Theme.font(Font.ITALIC, 13));
            none.setForeground(Theme.DIM);
            none.setBorder(BorderFactory.createEmptyBorder(26, 0, 26, 0));

            g.gridy = 1; g.gridx = 0; g.gridwidth = cols; g.weighty = 1;
            g.fill = GridBagConstraints.BOTH;
            add(none, g);

            revalidate();
            repaint();
            return;
        }

        int y = 1;

        for (String[] r : rows) {

            for (int c = 0; c < cols; c++) {

                JComponent cell;
                boolean small = false;

                if (c < heads.length) {

                    String v = c < r.length && r[c] != null ? r[c] : "";

                    if (c == statusCol) {
                        cell = Kit.pill(v, Colors.of(v));
                        small = true;
                    } else {
                        JLabel l = new JLabel(v);
                        l.setFont(Theme.font(c == 0 ? Font.BOLD : Font.PLAIN, 13));
                        l.setForeground(Color.WHITE);
                        cell = l;
                    }

                } else {
                    cell = action.make(r);
                    small = true;
                }

                JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                holder.setOpaque(false);
                holder.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 8));
                holder.add(cell);

                g.gridy = y; g.gridx = c; g.gridwidth = 1;
                g.weightx = c == 0 ? 1.3 : 1;
                g.fill = GridBagConstraints.HORIZONTAL;
                add(holder, g);
            }

            JPanel line = new JPanel();
            line.setBackground(new Color(26, 37, 70));
            line.setPreferredSize(new Dimension(10, 1));

            g.gridy = y + 1; g.gridx = 0; g.gridwidth = cols; g.weightx = 1;
            add(line, g);

            y += 2;
        }

        // filler so rows stay at the top
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        g.gridy = y; g.gridx = 0; g.gridwidth = cols; g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        add(filler, g);

        revalidate();
        repaint();
    }
}