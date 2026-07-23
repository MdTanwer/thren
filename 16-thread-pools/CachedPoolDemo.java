import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Cached pool: creates threads as needed for short tasks; reuses idle ones;
 * unused threads may be discarded after ~60s.
 */
public class CachedPoolDemo {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService pool = Executors.newCachedThreadPool();

        for (int i = 1; i <= 5; i++) {
            final int id = i;
            pool.submit(() -> {
                System.out.println("short task " + id + " on " + Thread.currentThread().getName());
            });
        }

        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
        System.out.println("(cached pool expands for bursts of short-lived work)");
    }
}
