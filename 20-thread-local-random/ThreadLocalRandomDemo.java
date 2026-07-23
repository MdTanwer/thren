import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ThreadLocalRandom — per-thread RNG for concurrent use (JDK 7+).
 * Avoids contention on a shared Random / Math.random().
 */
public class ThreadLocalRandomDemo {

    public static void main(String[] args) throws InterruptedException {
        // Oracle's example: bound range [4, 77)
        int r = ThreadLocalRandom.current().nextInt(4, 77);
        System.out.println("Oracle style nextInt(4, 77) → " + r);

        int tasks = 8;
        int perTask = 200_000;

        long sharedMs = timeWithSharedRandom(tasks, perTask);
        long tlrMs = timeWithThreadLocalRandom(tasks, perTask);

        System.out.println("shared Random  (~" + sharedMs + " ms) — threads contend on one RNG");
        System.out.println("ThreadLocalRandom (~" + tlrMs + " ms) — each thread has its own");
    }

    static long timeWithSharedRandom(int tasks, int perTask) throws InterruptedException {
        Random shared = new Random();
        AtomicLong sink = new AtomicLong();
        ExecutorService pool = Executors.newFixedThreadPool(tasks);
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < tasks; i++) {
            pool.submit(() -> {
                long sum = 0;
                for (int j = 0; j < perTask; j++) {
                    synchronized (shared) { // Random is not safely concurrent without sync
                        sum += shared.nextInt(100);
                    }
                }
                sink.addAndGet(sum);
            });
        }
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        return System.currentTimeMillis() - t0;
    }

    static long timeWithThreadLocalRandom(int tasks, int perTask) throws InterruptedException {
        AtomicLong sink = new AtomicLong();
        ExecutorService pool = Executors.newFixedThreadPool(tasks);
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < tasks; i++) {
            pool.submit(() -> {
                long sum = 0;
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                for (int j = 0; j < perTask; j++) {
                    sum += rnd.nextInt(100);
                }
                sink.addAndGet(sum);
            });
        }
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        return System.currentTimeMillis() - t0;
    }
}
