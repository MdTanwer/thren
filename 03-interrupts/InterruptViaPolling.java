/**
 * Worker does CPU work with no sleep — must poll Thread.interrupted().
 * interrupted() returns true AND clears the flag.
 */
public class InterruptViaPolling implements Runnable {

    @Override
    public void run() {
        long n = 0;
        while (true) {
            // pretend "heavy work"
            n++;
            if (n % 50_000_000L == 0) {
                System.out.println("[worker] still crunching, n=" + n);
            }

            if (Thread.interrupted()) {
                // flag was set; interrupted() cleared it
                System.out.println("[worker] saw interrupt — stopping");
                return;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(new InterruptViaPolling(), "worker");
        worker.start();

        Thread.sleep(300);
        System.out.println("[main] interrupt status before: " + worker.isInterrupted());
        worker.interrupt();
        System.out.println("[main] interrupt status after interrupt(): " + worker.isInterrupted());

        worker.join();
        System.out.println("[main] done");
    }
}
