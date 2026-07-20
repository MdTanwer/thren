/**
 * join(timeout): wait at most N ms — may return while the other thread still runs.
 */
public class JoinWithTimeout {

    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(3000);
                System.out.println("[worker] finished long job");
            } catch (InterruptedException e) {
                System.out.println("[worker] interrupted");
            }
        }, "worker");

        worker.start();

        System.out.println("[main] join(1000) — wait at most 1 second");
        worker.join(1000);

        System.out.println("[main] woke from join; worker still alive? " + worker.isAlive());
        // wait the rest so the process doesn't exit early
        worker.join();
        System.out.println("[main] now worker is done; alive? " + worker.isAlive());
    }
}
