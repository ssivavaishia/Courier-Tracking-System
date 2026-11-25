import javax.swing.*;
import java.time.LocalTime;

/**
 * A label whose text follows the time of day:
 * "Good morning, Asha!" ... and keeps itself up to date while visible.
 */
public class Greeting extends JLabel {

    private final String name;
    private final Timer timer;

    public Greeting(String name) {

        this.name = name;
        setText(text(name));

        timer = new Timer(60_000, e -> setText(text(this.name)));
        timer.setRepeats(true);
    }

    public static String text(String name) {

        int hour = LocalTime.now().getHour();

        String hello;

        if (hour >= 5 && hour < 12) {
            hello = "Good morning";
        } else if (hour >= 12 && hour < 17) {
            hello = "Good afternoon";
        } else if (hour >= 17 && hour < 21) {
            hello = "Good evening";
        } else {
            hello = "Good night";
        }

        return name == null || name.trim().isEmpty()
                ? hello + "!"
                : hello + ", " + name + "!";
    }

    @Override
    public void addNotify() {
        super.addNotify();
        setText(text(name));
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }
}