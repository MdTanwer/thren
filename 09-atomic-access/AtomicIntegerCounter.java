import java.util.concurrent.atomic.AtomicInteger;

/**
 * AtomicInteger.incrementAndGet() is a true atomic read-modify-write.
 * No lost updates — more efficient than synchronized for simple counters.
 */
public class AtomicIntegerCounter {

    private final AtomicInteger count = new AtomicInteger(0);

    public void increment() {
        count.incrementAndGet(); // atomic — cannot interleave like c++
    }

    public int getCount() {
        return count.get();
    }

    public static void main(String[] args) throws InterruptedException {
        AtomicIntegerCounter counter = new AtomicIntegerCounter();
        int n = 100_000;

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < n; i++) {
                counter.increment();
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < n; i++) {
                counter.increment();
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("Expected: " + (2 * n));
        System.out.println("Actual:   " + counter.getCount());
    }
}
