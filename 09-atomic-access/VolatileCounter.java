/**
 * volatile read/write is atomic AND establishes happens-before.
 * Reader is guaranteed to see the latest value (and prior writes).
 */
public class VolatileCounter {

    private volatile int count = 0;

    public void increment() {
        count++; // read-modify-write is NOT atomic — still a race if two threads call this!
    }

    public int getCount() {
        return count; // volatile read — always sees latest published value
    }

    public static void main(String[] args) throws InterruptedException {
        VolatileCounter counter = new VolatileCounter();

        Thread writer = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment();
            }
        }, "writer");

        Thread reader = new Thread(() -> {
            int last = 0;
            for (int i = 0; i < 1000; i++) {
                int now = counter.getCount();
                if (now != last) {
                    last = now;
                }
            }
            System.out.println("[reader] final count seen: " + counter.getCount());
        }, "reader");

        writer.start();
        reader.start();
        writer.join();
        reader.join();

        // volatile does NOT make c++ atomic — final value may be < 1000 under contention
        System.out.println("[main] actual count: " + counter.getCount() + " (may be < 1000)");
        System.out.println("volatile fixes visibility, not lost updates on c++");
    }
}
