import java.util.concurrent.atomic.AtomicInteger;

/**
 * Oracle Atomic Variables: Plain vs Synchronized vs AtomicInteger counter.
 * Atomic get/set behave like volatile (happens-before); incrementAndGet is atomic RMW.
 */
public class AtomicCounter {
    private final AtomicInteger c = new AtomicInteger(0);

    public void increment() {
        c.incrementAndGet();
    }

    public void decrement() {
        c.decrementAndGet();
    }

    public int value() {
        return c.get();
    }

    /** Unsafe — same as Oracle's original Counter. */
    static class Plain {
        private int c = 0;

        void increment() {
            c++;
        }

        void decrement() {
            c--;
        }

        int value() {
            return c;
        }
    }

    /** Safe via synchronized methods. */
    static class Sync {
        private int c = 0;

        synchronized void increment() {
            c++;
        }

        synchronized void decrement() {
            c--;
        }

        synchronized int value() {
            return c;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int n = 100_000;

        Plain plain = new Plain();
        runBoth(plain::increment, plain::decrement, n);
        System.out.println("Plain (c++)           actual=" + plain.value() + " (expect 0 — often wrong)");

        Sync sync = new Sync();
        runBoth(sync::increment, sync::decrement, n);
        System.out.println("Synchronized          actual=" + sync.value() + " (expect 0)");

        AtomicCounter atomic = new AtomicCounter();
        runBoth(atomic::increment, atomic::decrement, n);
        System.out.println("AtomicInteger         actual=" + atomic.value() + " (expect 0)");
    }

    static void runBoth(Runnable inc, Runnable dec, int n) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < n; i++) {
                inc.run();
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < n; i++) {
                dec.run();
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
