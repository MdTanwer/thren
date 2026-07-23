import java.util.concurrent.atomic.AtomicInteger;

/**
 * compareAndSet (CAS) — update only if value is still what we expect.
 * Heart of lock-free algorithms; used inside incrementAndGet.
 */
public class CompareAndSetDemo {

    public static void main(String[] args) {
        AtomicInteger n = new AtomicInteger(10);

        boolean ok = n.compareAndSet(10, 20); // if still 10 → set 20
        System.out.println("CAS 10→20: " + ok + ", value=" + n.get());

        ok = n.compareAndSet(10, 99); // expects 10, but is 20 → fail
        System.out.println("CAS 10→99: " + ok + ", value=" + n.get() + " (unchanged)");

        // Manual lock-free increment (what incrementAndGet does internally)
        int prev;
        do {
            prev = n.get();
        } while (!n.compareAndSet(prev, prev + 1));
        System.out.println("after CAS loop increment: " + n.get());
    }
}
