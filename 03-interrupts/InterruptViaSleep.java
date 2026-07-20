/**
 * Worker supports interruption: sleep throws InterruptedException → exit run().
 * Main interrupts the worker after ~2.5 seconds.
 */
public class InterruptViaSleep implements Runnable {

    private static final String[] MESSAGES = {
        "Mares eat oats",
        "Does eat oats",
        "Little lambs eat ivy",
        "A kid will eat ivy too"
    };

    @Override
    public void run() {
        for (int i = 0; i < MESSAGES.length; i++) {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                // Interrupted during sleep: stop — common pattern
                System.out.println("[worker] interrupted — stopping");
                return;
            }
            System.out.println("[worker] " + MESSAGES[i]);
        }
        System.out.println("[worker] finished all messages");
    }

    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(new InterruptViaSleep(), "worker");
        worker.start();

        Thread.sleep(2500); // let worker sleep once, then interrupt mid-wait
        System.out.println("[main] calling worker.interrupt()");
        worker.interrupt();

        worker.join();
        System.out.println("[main] done");
    }
}
