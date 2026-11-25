import javax.swing.*;
import java.awt.*;

/** A dark rounded card with a thin border: the building block of every page. */
public class Surface extends JPanel {

    private Color fill = Theme.PANEL;
    private final int arc;

    public Surface() {
        this(18);
    }

    public Surface(int arc) {

        this.arc = arc;

        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
    }

    public Surface fill(Color c) {
        this.fill = c;
        repaint();
        return this;
    }

    @Override
    protected void paintComponent(Graphics g0) {

        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);

        int w = getWidth(), h = getHeight();

        g.setColor(fill);
        g.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

        g.setColor(Theme.EDGE);
        g.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

        g.dispose();
        super.paintComponent(g0);
    }
}