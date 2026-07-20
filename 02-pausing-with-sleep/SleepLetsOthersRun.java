/**
 * Shows sleep frees the CPU so another thread can run,
 * and that sleep time is approximate (not exact).
 */
public class SleepLetsOthersRun {

    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                System.out.println("  [worker] tick " + i);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "worker");

        long start = System.currentTimeMillis();
        worker.start();

        System.out.println("[main] sleeping 2 seconds — worker can use the CPU");
        Thread.sleep(2000);

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("[main] woke up after ~" + elapsed + " ms (requested 2000)");
        worker.join(); // wait for worker to finish (covered more later)
    }
}
