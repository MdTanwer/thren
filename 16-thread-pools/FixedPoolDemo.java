import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Fixed pool: always N worker threads. Extra tasks wait in an internal queue.
 * Graceful under load — won't spawn unlimited threads.
 */
public class FixedPoolDemo {

    public static void main(String[] args) throws InterruptedException {
        int workers = 3;
        ExecutorService pool = Executors.newFixedThreadPool(workers);

        System.out.println("Submitting 6 tasks to pool of " + workers + " workers");
        for (int i = 1; i <= 6; i++) {
            final int id = i;
            pool.submit(() -> {
                System.out.println("task " + id + " START on " + Thread.currentThread().getName());
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("task " + id + " END   on " + Thread.currentThread().getName());
            });
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }
}
