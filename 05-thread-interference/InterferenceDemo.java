/**
 * Two threads: one increments N times, one decrements N times.
 * Expected final value = 0. With interference, you often get a wrong number.
 *
 * Run several times — the bug is timing-dependent (may occasionally look "correct").
 */
public class InterferenceDemo {

    private static final int N = 100_000;

    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();

        Thread inc = new Thread(() -> {
            for (int i = 0; i < N; i++) {
                counter.increment();
            }
        }, "inc");

        Thread dec = new Thread(() -> {
            for (int i = 0; i < N; i++) {
                counter.decrement();
            }
        }, "dec");

        inc.start();
        dec.start();
        inc.join();
        dec.join();

        System.out.println("Expected: 0");
        System.out.println("Actual:   " + counter.value());
        System.out.println(counter.value() == 0
                ? "(got lucky this run — try again)"
                : "(interference lost some updates)");
    }
}
