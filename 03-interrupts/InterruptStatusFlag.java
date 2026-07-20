/**
 * interrupted() vs isInterrupted():
 * - Thread.interrupted()     → checks CURRENT thread, CLEARS the flag
 * - thread.isInterrupted()   → checks THAT thread, does NOT clear the flag
 */
public class InterruptStatusFlag {

    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                // sleep threw → JVM cleared the interrupt flag by convention
                System.out.println("[worker] after InterruptedException, interrupted()="
                        + Thread.interrupted()); // usually false (already cleared)

                // re-set flag if you want callers higher up to see it
                Thread.currentThread().interrupt();
                System.out.println("[worker] after re-interrupt, isInterrupted()="
                        + Thread.currentThread().isInterrupted());
            }
        }, "worker");

        worker.start();
        Thread.sleep(200);

        System.out.println("[main] before interrupt, isInterrupted=" + worker.isInterrupted());
        worker.interrupt();
        System.out.println("[main] after interrupt,  isInterrupted=" + worker.isInterrupted());

        worker.join();
    }
}
