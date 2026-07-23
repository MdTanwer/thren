/**
 * Contrast: unbounded new Thread per request vs fixed pool.
 * Under burst load, unlimited threads can exhaust the system;
 * a fixed pool queues work and keeps running.
 */
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PoolVsNewThread {

    public static void main(String[] args) throws InterruptedException {
        int requests = 20;
        AtomicInteger done = new AtomicInteger(0);

        long t0 = System.currentTimeMillis();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        for (int i = 0; i < requests; i++) {
            pool.submit(() -> {
                busyWork();
                done.incrementAndGet();
            });
        }
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        long poolMs = System.currentTimeMillis() - t0;

        System.out.println("fixed pool(4): finished " + done.get() + " tasks in ~" + poolMs + " ms");
        System.out.println("Oracle point: web server with unbounded new Thread(req) can collapse;");
        System.out.println("              fixed pool degrades gracefully (queue + limited workers).");
    }

    static void busyWork() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
