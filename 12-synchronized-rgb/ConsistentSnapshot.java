/**
 * Fix: hold the same lock across both reads so the snapshot is consistent.
 */
public class ConsistentSnapshot {

    public static void main(String[] args) throws InterruptedException {
        SynchronizedRGB color = new SynchronizedRGB(0, 0, 0, "Pitch Black");

        Thread changer = new Thread(() -> {
            for (int i = 0; i < 100_000; i++) {
                color.set(255, 255, 255, "Pure White");
                color.set(0, 0, 0, "Pitch Black");
            }
        }, "changer");

        Thread reader = new Thread(() -> {
            int mismatches = 0;
            for (int i = 0; i < 100_000; i++) {
                int rgb;
                String name;
                synchronized (color) {  // bind Statement 1 + 2 together
                    rgb = color.getRGB();
                    name = color.getName();
                }
                boolean black = (rgb == 0) && "Pitch Black".equals(name);
                boolean white = (rgb == 0xFFFFFF) && "Pure White".equals(name);
                if (!black && !white) {
                    mismatches++;
                }
            }
            System.out.println("Mismatches with synchronized block: " + mismatches + " (expect 0)");
        }, "reader");

        changer.start();
        reader.start();
        changer.join();
        reader.join();
    }
}
