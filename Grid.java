
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base for every table page: heading, action buttons, coloured status
 * filter pills with counts, search box, status drop-down, sortable styled
 * table and a row counter. A subclass only supplies the columns and fetch().
 */
public abstract class Grid extends JPanel {

    protected final DefaultTableModel model;
    protected final JTable table;

    private final int statusCol;
    private final JTextField search = new JTextField(16);
    private final JComboBox<String> filter = new JComboBox<>();
    private final JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JLabel count = new JLabel(" ");
    private final TableRowSorter<DefaultTableModel> sorter;
    private boolean updating = false;

    protected Grid(String title, String subtitle, String[] cols, int statusCol) {

        this.statusCol = statusCol;

        setOpaque(false);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        table.setAutoCreateRowSorter(false);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // heading
        JLabel t = new JLabel(title);
        t.setFont(Theme.font(Font.BOLD, 22));
        t.setForeground(Color.WHITE);

        JLabel s = new JLabel(subtitle);
        s.setFont(Theme.font(Font.PLAIN, 13));
        s.setForeground(Theme.DIM);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(t);
        titles.add(s);

        actions.setOpaque(false);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(titles, BorderLayout.WEST);
        head.add(actions, BorderLayout.EAST);

        // tools: chips + search + filter + refresh
        chips.setOpaque(false);

        search.setToolTipText("Search in every column");
        search.putClientProperty("sud.skin", Boolean.TRUE);
        search.setBackground(Theme.PANEL2);
        search.setForeground(Color.WHITE);
        search.setCaretColor(Color.WHITE);
        search.setFont(Theme.font(Font.PLAIN, 13));
        search.setBorder(BorderFactory.createCompoundBorder(
            new Skin.Field(), BorderFactory.createEmptyBorder(0, 4, 0, 4)));
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });

        filter.addItem("All statuses");
        filter.addActionListener(e -> { if (!updating) applyFilter(); });
        filter.setFont(Theme.font(Font.PLAIN, 13));

        JButton refresh = new JButton("Refresh");
        refresh.setIcon(Ico.of("route", 16, Color.WHITE));
        refresh.setBackground(new Color(34, 47, 86));
        refresh.setForeground(Color.WHITE);
        refresh.setFont(Theme.font(Font.BOLD, 12));
        refresh.setFocusPainted(false);
        refresh.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        refresh.addActionListener(e -> reload());

        JLabel sl = new JLabel("Search");
        sl.setForeground(Theme.DIM);
        sl.setFont(Theme.font(Font.BOLD, 12));

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.setOpaque(false);
        tools.add(sl);
        tools.add(search);
        tools.add(filter);
        tools.add(refresh);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(chips, BorderLayout.WEST);
        bar.add(tools, BorderLayout.EAST);

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(head, BorderLayout.NORTH);
        north.add(bar, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.EDGE));

        count.setFont(Theme.font(Font.PLAIN, 12));
        count.setForeground(Theme.DIM);

        add(north, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(count, BorderLayout.SOUTH);
    }

    /** Rows for the table (called on a background thread). */
    protected abstract List<Object[]> fetch() throws Exception;

    /** Text shown next to the row counter, e.g. totals. */
    protected String summary(List<Object[]> rows) {
        return "";
    }

    protected void addAction(JButton b) {
        b.setFont(Theme.font(Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        actions.add(b);
    }

    protected JButton primary(String text) {
        JButton b = new JButton(text);
        b.setBackground(Theme.RED);
        return b;
    }

    /** Values of the selected row, or null. */
    protected Object[] selected() {

        int r = table.getSelectedRow();

        if (r < 0) return null;

        int m = table.convertRowIndexToModel(r);
        Object[] v = new Object[model.getColumnCount()];

        for (int i = 0; i < v.length; i++) v[i] = model.getValueAt(m, i);

        return v;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        reload();
    }

    public void reload() {

        new SwingWorker<List<Object[]>, Void>() {

            @Override
            protected List<Object[]> doInBackground() throws Exception {
                return fetch();
            }

            @Override
            protected void done() {

                try {
                    show(get());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    model.setRowCount(0);
                    count.setText("Could not load data: " + ex.getCause());
                }
            }

        }.execute();
    }

    private void show(List<Object[]> rows) {

        model.setRowCount(0);

        Map<String, Integer> byStatus = new LinkedHashMap<>();

        for (Object[] r : rows) {
            model.addRow(r);
            if (statusCol >= 0 && r[statusCol] != null) {
                byStatus.merge(r[statusCol].toString().trim().toUpperCase(), 1, Integer::sum);
            }
        }

        // status pills with counts; click one to filter
        chips.removeAll();

        updating = true;
        Object keep = filter.getSelectedItem();
        filter.removeAllItems();
        filter.addItem("All statuses");

        for (Map.Entry<String, Integer> e : byStatus.entrySet()) {

            filter.addItem(e.getKey());

            JComponent pill = Kit.pill(e.getKey() + "  " + e.getValue(), Colors.of(e.getKey()));
            pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            final String key = e.getKey();
            pill.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent ev) {
                    filter.setSelectedItem(filter.getSelectedItem().equals(key) ? "All statuses" : key);
                }
            });
            chips.add(pill);
        }

        if (keep != null) filter.setSelectedItem(keep);
        if (filter.getSelectedIndex() < 0) filter.setSelectedIndex(0);
        updating = false;

        chips.revalidate();
        chips.repaint();

        applyFilter();

        String extra = summary(rows);
        count.setText(rows.size() + (rows.size() == 1 ? " record" : " records")
                + (extra.isEmpty() ? "" : "     " + extra));
    }

    private void applyFilter() {

        final String q = search.getText().trim().toLowerCase();
        final Object f = filter.getSelectedItem();
        final String st = f == null || "All statuses".equals(f) ? null : f.toString();

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> en) {

                if (st != null && statusCol >= 0) {
                    Object v = en.getValue(statusCol);
                    if (v == null || !v.toString().trim().equalsIgnoreCase(st)) return false;
                }

                if (q.isEmpty()) return true;

                for (int i = 0; i < en.getValueCount(); i++) {
                    Object v = en.getValue(i);
                    if (v != null && v.toString().toLowerCase().contains(q)) return true;
                }

                return false;
            }
        });
    }

    protected static String date(java.sql.Timestamp t) {

        return t == null ? "" : t.toLocalDateTime().toLocalDate()
            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    protected static List<Object[]> newRows() {
        return new ArrayList<>();
    }
}