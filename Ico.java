import java.awt.*;
import java.awt.geom.Path2D;
import javax.swing.Icon;

/** Small vector icons (no image files, no special fonts needed). */
public final class Ico {

    private Ico() { }

    public static Icon of(String name, int size, Color color) {

        return new Icon() {

            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }

            @Override
            public void paintIcon(Component c, Graphics g0, int x, int y) {

                Graphics2D g = (Graphics2D) g0.create();
                Theme.smooth(g);
                g.translate(x, y);
                g.scale(size / 24.0, size / 24.0);
                g.setColor(color);
                g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));

                draw(g, name);

                g.dispose();
            }
        };
    }

    private static void draw(Graphics2D g, String n) {

        switch (n) {

            case "home":
                g.drawPolyline(new int[]{3, 12, 21}, new int[]{11, 3, 11}, 3);
                g.drawPolyline(new int[]{5, 5, 19, 19}, new int[]{10, 20, 20, 10}, 4);
                g.drawLine(10, 20, 10, 14); g.drawLine(14, 20, 14, 14);
                g.drawLine(10, 14, 14, 14);
                break;

            case "plus":
                g.drawRoundRect(3, 3, 18, 18, 6, 6);
                g.drawLine(12, 8, 12, 16); g.drawLine(8, 12, 16, 12);
                break;

            case "track":
                g.drawOval(4, 3, 16, 16);
                g.fillOval(10, 9, 4, 4);
                g.drawLine(12, 19, 12, 22);
                break;

            case "boxes":
                g.drawPolygon(new int[]{12, 21, 21, 12, 3, 3},
                              new int[]{3, 7.5 > 0 ? 8 : 8, 17, 21, 17, 8}, 6);
                g.drawPolyline(new int[]{3, 12, 21}, new int[]{8, 13, 8}, 3);
                g.drawLine(12, 13, 12, 21);
                break;

            case "rupee":
                g.drawOval(2, 2, 20, 20);
                g.drawLine(8, 8, 16, 8); g.drawLine(8, 12, 16, 12);
                g.drawLine(9, 12, 15, 18);
                break;

            case "alert":
                g.drawPolygon(new int[]{12, 22, 2}, new int[]{3, 20, 20}, 3);
                g.drawLine(12, 9, 12, 14); g.fillOval(11, 16, 2, 2);
                break;

            case "user":
                g.drawOval(8, 3, 8, 8);
                g.drawArc(4, 13, 16, 14, 0, 180);
                break;

            case "help":
                g.drawOval(2, 2, 20, 20);
                g.setFont(Theme.font(Font.BOLD, 15));
                g.drawString("?", 9, 17);
                break;

            case "map":
                g.drawPolyline(new int[]{3, 9, 15, 21, 21, 15, 9, 3, 3},
                               new int[]{6, 4, 6, 4, 18, 20, 18, 20, 6}, 9);
                g.drawLine(9, 4, 9, 18); g.drawLine(15, 6, 15, 20);
                break;

            case "chat":
                g.drawRoundRect(3, 4, 18, 13, 6, 6);
                g.drawPolyline(new int[]{8, 7, 12}, new int[]{17, 21, 17}, 3);
                g.drawLine(8, 9, 16, 9); g.drawLine(8, 13, 13, 13);
                break;

            case "truck":
                g.drawRect(2, 6, 12, 10);
                g.drawPolyline(new int[]{14, 19, 22, 22, 14}, new int[]{9, 9, 12, 16, 16}, 5);
                g.drawOval(5, 15, 5, 5); g.drawOval(16, 15, 5, 5);
                break;

            case "check":
                g.drawOval(2, 2, 20, 20);
                g.drawPolyline(new int[]{7, 11, 17}, new int[]{12, 16, 8}, 3);
                break;

            case "route":
                g.drawOval(3, 15, 6, 6); g.drawOval(15, 3, 6, 6);
                Path2D p = new Path2D.Double();
                p.moveTo(6, 15); p.curveTo(6, 8, 18, 16, 18, 9);
                g.draw(p);
                break;

            case "logout":
                g.drawPolyline(new int[]{10, 4, 4, 10}, new int[]{4, 4, 20, 20}, 4);
                g.drawLine(10, 12, 21, 12);
                g.drawPolyline(new int[]{17, 21, 17}, new int[]{8, 12, 16}, 3);
                break;

            case "clock":
                g.drawOval(2, 2, 20, 20);
                g.drawPolyline(new int[]{12, 12, 17}, new int[]{6, 12, 14}, 3);
                break;

            case "edit":
                g.drawPolygon(new int[]{4, 16, 20, 8}, new int[]{20, 8, 12, 24}, 4);
                g.drawLine(14, 6, 18, 10);
                break;

            case "chart":
                g.drawLine(4, 20, 20, 20);
                g.drawRect(5, 12, 3, 8); g.drawRect(10, 6, 3, 14); g.drawRect(15, 10, 3, 10);
                break;

            case "bolt":
                g.drawPolygon(new int[]{13, 5, 11, 10, 19, 12}, new int[]{2, 13, 13, 22, 10, 10}, 6);
                break;

            case "pin":
                g.drawOval(7, 3, 10, 10);
                g.drawPolyline(new int[]{8, 12, 16}, new int[]{12, 21, 12}, 3);
                break;

            case "menu":
                g.drawLine(4, 7, 20, 7); g.drawLine(4, 12, 20, 12);
                g.drawLine(4, 17, 20, 17);
                break;

            default:
                g.drawOval(4, 4, 16, 16);
        }
    }
}