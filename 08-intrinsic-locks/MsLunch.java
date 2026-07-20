/**
 * Oracle MsLunch — two separate locks so inc1 and inc2 can run in parallel.
 * Only safe because c1 and c2 are never used together.
 */
public class MsLunch {
    private long c1 = 0;
    private long c2 = 0;
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();

    public void inc1() {
        synchronized (lock1) {
            c1++;
        }
    }

    public void inc2() {
        synchronized (lock2) {
            c2++;
        }
    }

    public long getC1() {
        synchronized (lock1) {
            return c1;
        }
    }

    public long getC2() {
        synchronized (lock2) {
            return c2;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        MsLunch lunch = new MsLunch();
        int n = 50_000;

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < n; i++) {
                lunch.inc1();
            }
        }, "inc1");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < n; i++) {
                lunch.inc2();
            }
        }, "inc2");

        long start = System.currentTimeMillis();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("c1=" + lunch.getC1() + " c2=" + lunch.getC2() + " (both " + n + ")");
        System.out.println("Finished in ~" + elapsed + " ms — inc1 and inc2 ran concurrently");
    }
}
