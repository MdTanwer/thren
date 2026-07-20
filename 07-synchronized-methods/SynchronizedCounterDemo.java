/**
 * Same stress test as InterferenceDemo, but with synchronized methods.
 * Expected final value is always 0.
 */
public class SynchronizedCounterDemo {

    private static final int N = 100_000;

    public static void main(String[] args) throws InterruptedException {
        SynchronizedCounter counter = new SynchronizedCounter();

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
    }
}
