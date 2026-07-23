/**
 * Race: getRGB() then getName() without holding the lock across both.
 * Another thread can set() in between → RGB and name don't match.
 */
public class InconsistentSnapshot {

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
                int rgb = color.getRGB();       // Statement 1
                String name = color.getName();  // Statement 2 — gap between them!
                boolean black = (rgb == 0) && "Pitch Black".equals(name);
                boolean white = (rgb == 0xFFFFFF) && "Pure White".equals(name);
                if (!black && !white) {
                    mismatches++;
                    if (mismatches <= 3) {
                        System.out.printf("Mismatch: rgb=0x%06X name=%s%n", rgb, name);
                    }
                }
            }
            System.out.println("Mismatches (inconsistent RGB+name): " + mismatches);
        }, "reader");

        changer.start();
        reader.start();
        changer.join();
        reader.join();
    }
}
