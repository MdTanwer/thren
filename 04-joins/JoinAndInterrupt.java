/**
 * join also responds to interrupts — same idea as sleep.
 */
public class JoinAndInterrupt {

    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                System.out.println("[worker] interrupted during sleep");
            }
        }, "worker");

        Thread mainWaiter = Thread.currentThread();

        // another thread interrupts main while main is blocked in join
        Thread interrupter = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            System.out.println("[interrupter] interrupting main during join");
            mainWaiter.interrupt();
        }, "interrupter");

        worker.start();
        interrupter.start();

        try {
            System.out.println("[main] joining worker...");
            worker.join();
            System.out.println("[main] join completed normally");
        } catch (InterruptedException e) {
            System.out.println("[main] join aborted by interrupt");
            worker.interrupt(); // ask worker to stop too
            worker.join();
        }
    }
}
