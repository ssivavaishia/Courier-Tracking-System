import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Dashboard customisation: lets an admin hide or re-order the stat cards
 * and panels of the Intelligence Centre. The choice is remembered per user
 * (java.util.prefs), so it survives restarts.
 */
public class Layout {

    public static class Item {
        public final String id, title, group;
        Item(String id, String title, String group) {
            this.id = id;
            this.title = title;
            this.group = group;
        }
    }

    public static final Item[] ITEMS = {
        new Item("total",      "Total shipments",    "stats"),
        new Item("transit",    "In transit",         "stats"),
        new Item("ofd",        "Out for delivery",   "stats"),
        new Item("done",       "Delivered today",    "stats"),
        new Item("unassigned", "Waiting for courier","stats"),
        new Item("feed",       "Activity panel (right side)", "panel"),
        new Item("recent",     "Recent shipments strip (bottom)", "panel"),
        new Item("mascot",     "Mascot truck",       "panel")
    };

    private final Preferences prefs;
    private final List<String> order = new ArrayList<>();
    private final Set<String> hidden = new HashSet<>();

    public Layout(int userId) {

        prefs = Preferences.userRoot().node("sudwar/dashboard/" + userId);
        load();
    }

    private void load() {

        order.clear();
        hidden.clear();

        String saved = prefs.get("order", "");

        if (!saved.isEmpty()) {
            for (String id : saved.split(",")) {
                if (find(id) != null && !order.contains(id)) order.add(id);
            }
        }

        // any item missing from the saved list goes to the end
        for (Item it : ITEMS) {
            if (!order.contains(it.id)) order.add(it.id);
        }

        String hide = prefs.get("hidden", "");

        if (!hide.isEmpty()) {
            hidden.addAll(Arrays.asList(hide.split(",")));
        }
    }

    private void save() {

        prefs.put("order", String.join(",", order));
        prefs.put("hidden", String.join(",", hidden));
    }

    private static Item find(String id) {

        for (Item it : ITEMS) {
            if (it.id.equals(id)) return it;
        }

        return null;
    }

    public boolean visible(String id) {
        return !hidden.contains(id);
    }

    /** Ids of one group ("stats" or "panel") in the saved order. */
    public List<String> order(String group) {

        List<String> out = new ArrayList<>();

        for (String id : order) {
            Item it = find(id);
            if (it != null && it.group.equals(group)) out.add(id);
        }

        return out;
    }

    // ---------------------------------------------------------------- UI

    public void showDialog(Component parent, Runnable onChange) {

        Window owner = SwingUtilities.getWindowAncestor(parent);

        JDialog dlg = new JDialog(owner, "Customize dashboard",
                Dialog.ModalityType.APPLICATION_MODAL);

        DefaultListModel<String> model = new DefaultListModel<>();
        for (String id : order) model.addElement(id);

        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(34);
        list.setFont(Theme.font(Font.PLAIN, 14));
        list.setCellRenderer((l, id, index, sel, focus) -> {

            Item it = find(id);

            JLabel lab = new JLabel(
                (visible(id) ? "  ☑   " : "  ☐   ")
                + (it == null ? id : it.title)
                + (it != null && it.group.equals("panel") ? "   (panel)" : ""));

            lab.setOpaque(true);
            lab.setFont(Theme.font(Font.PLAIN, 14));
            lab.setForeground(visible(id) ? Theme.PLUM : Color.GRAY);
            lab.setBackground(sel ? Theme.LILAC : Color.WHITE);

            return lab;
        });

        JButton up = button("Move up");
        JButton down = button("Move down");
        JButton toggle = button("Show / Hide");
        JButton reset = button("Reset");
        JButton close = button("Done");

        up.addActionListener(e -> {
            int i = list.getSelectedIndex();
            if (i > 0) {
                String id = model.remove(i);
                model.add(i - 1, id);
                list.setSelectedIndex(i - 1);
                apply(model, onChange);
            }
        });

        down.addActionListener(e -> {
            int i = list.getSelectedIndex();
            if (i >= 0 && i < model.size() - 1) {
                String id = model.remove(i);
                model.add(i + 1, id);
                list.setSelectedIndex(i + 1);
                apply(model, onChange);
            }
        });

        Runnable doToggle = () -> {
            int i = list.getSelectedIndex();
            if (i >= 0) {
                String id = model.get(i);
                if (!hidden.remove(id)) hidden.add(id);
                list.repaint();
                apply(model, onChange);
            }
        };

        toggle.addActionListener(e -> doToggle.run());

        reset.addActionListener(e -> {
            model.clear();
            for (Item it : ITEMS) model.addElement(it.id);
            hidden.clear();
            apply(model, onChange);
            list.repaint();
        });

        close.addActionListener(e -> dlg.dispose());

        JLabel tip = new JLabel(
            "<html>Choose what appears on your dashboard and in what order."
            + "<br>Changes are saved automatically.</html>");
        tip.setFont(Theme.font(Font.PLAIN, 12));
        tip.setForeground(Theme.MUTED);
        tip.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 8));
        buttons.setOpaque(false);
        buttons.add(up);
        buttons.add(down);
        buttons.add(toggle);
        buttons.add(reset);
        buttons.add(close);

        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(false);
        side.add(buttons, BorderLayout.NORTH);

        JPanel root = new JPanel(new BorderLayout(14, 0));
        root.setBackground(Theme.PAPER);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        root.add(tip, BorderLayout.NORTH);
        root.add(new JScrollPane(list), BorderLayout.CENTER);
        root.add(side, BorderLayout.EAST);

        dlg.setContentPane(root);
        dlg.setSize(520, 440);
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }

    private void apply(DefaultListModel<String> model, Runnable onChange) {

        order.clear();

        for (int i = 0; i < model.size(); i++) order.add(model.get(i));

        save();

        if (onChange != null) onChange.run();
    }

    private static JButton button(String text) {

        JButton b = new JButton(text);
        b.setBackground(Theme.RED);
        b.setForeground(Color.WHITE);
        b.setFont(Theme.font(Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));

        return b;
    }
}