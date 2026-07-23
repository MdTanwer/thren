/**
 * Overview: low-level Thread vs high-level ExecutorService (thread pool).
 * Full Executor details come in later Oracle sections.
 */
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;

public class HighLevelOverview {

    public static void main(String[] args) throws InterruptedException {
        AtomicInteger hits = new AtomicInteger(0);

        // High-level: pool manages threads — you submit tasks
        ExecutorService pool = Executors.newFixedThreadPool(4);

        for (int i = 0; i < 8; i++) {
            pool.submit(() -> {
                int n = ThreadLocalRandom.current().nextInt(100); // per-thread RNG
                hits.incrementAndGet(); // lock-free atomic
                System.out.println(Thread.currentThread().getName() + " rolled " + n);
            });
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("hits=" + hits.get() + " (expect 8)");
        System.out.println("Used: ExecutorService + AtomicInteger + ThreadLocalRandom");
    }
}
