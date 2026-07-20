/**
 * Oracle SynchronizedCounter — synchronized methods on the same object
 * share one lock (the object itself).
 */
public class SynchronizedCounter {
    private int c = 0;

    public synchronized void increment() {
        c++;
    }

    public synchronized void decrement() {
        c--;
    }

    public synchronized int value() {
        return c;
    }
}
