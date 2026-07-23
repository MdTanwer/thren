import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tour of key java.util.concurrent building blocks (one file).
 * Pair with: java-util-concurrent.md
 *
 *   make compile && java -cp target ConcurrentPackageTour
 */
public class ConcurrentPackageTour {

    public static void main(String[] args) throws Exception {
        demoExecutorAndFuture();
        demoBlockingQueue();
        demoConcurrentHashMap();
        demoCountDownLatch();
        demoCyclicBarrier();
        demoSemaphore();
        demoCompletableFuture();
        System.out.println("\n=== tour complete — see java-util-concurrent.md ===");
    }

    /** Executors + Callable + Future */
    static void demoExecutorAndFuture() throws Exception {
        System.out.println("\n--- Executor / Future ---");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<Integer> f = pool.submit(() -> {
            Thread.sleep(50);
            return 41 + 1;
        });
        System.out.println("Callable result=" + f.get());
        pool.shutdown();
    }

    /** BlockingQueue producer/consumer hand-off */
    static void demoBlockingQueue() throws Exception {
        System.out.println("\n--- BlockingQueue ---");
        BlockingQueue<String> q = new ArrayBlockingQueue<>(1);
        Thread producer = new Thread(() -> {
            try {
                q.put("hello");
                System.out.println("put done");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread consumer = new Thread(() -> {
            try {
                System.out.println("took=" + q.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
    }

    /** ConcurrentMap atomic update */
    static void demoConcurrentHashMap() throws Exception {
        System.out.println("\n--- ConcurrentHashMap ---");
        ConcurrentMap<String, Integer> map = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 4; i++) {
            pool.submit(() -> {
                for (int j = 0; j < 1000; j++) {
                    map.merge("hits", 1, Integer::sum);
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("hits=" + map.get("hits") + " (expect 4000)");
    }

    /** Wait until N events finish */
    static void demoCountDownLatch() throws Exception {
        System.out.println("\n--- CountDownLatch ---");
        int workers = 3;
        CountDownLatch done = new CountDownLatch(workers);
        for (int i = 0; i < workers; i++) {
            final int id = i;
            new Thread(() -> {
                System.out.println("worker " + id + " finished");
                done.countDown();
            }).start();
        }
        done.await();
        System.out.println("main: all workers done");
    }

    /** All threads meet, then continue together */
    static void demoCyclicBarrier() throws Exception {
        System.out.println("\n--- CyclicBarrier ---");
        int parties = 3;
        CyclicBarrier barrier = new CyclicBarrier(parties,
                () -> System.out.println("barrier action: all arrived"));
        ExecutorService pool = Executors.newFixedThreadPool(parties);
        for (int i = 0; i < parties; i++) {
            final int id = i;
            pool.submit(() -> {
                try {
                    System.out.println("thread " + id + " waiting");
                    barrier.await();
                    System.out.println("thread " + id + " passed");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }

    /** Limit concurrent access to N permits */
    static void demoSemaphore() throws Exception {
        System.out.println("\n--- Semaphore ---");
        Semaphore sem = new Semaphore(2); // at most 2 at a time
        AtomicInteger inside = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            final int id = i;
            pool.submit(() -> {
                try {
                    sem.acquire();
                    int n = inside.incrementAndGet();
                    System.out.println("id=" + id + " inside=" + n);
                    Thread.sleep(30);
                    inside.decrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    sem.release();
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }

    /** CompletableFuture composition */
    static void demoCompletableFuture() throws Exception {
        System.out.println("\n--- CompletableFuture ---");
        String result = CompletableFuture
                .supplyAsync(() -> "con")
                .thenApply(s -> s + "current")
                .thenApply(String::toUpperCase)
                .get();
        System.out.println("composed=" + result);
    }
}
