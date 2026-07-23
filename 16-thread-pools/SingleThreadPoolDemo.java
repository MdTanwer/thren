import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Single-thread executor: tasks run one at a time, in order (no parallel).
 */
public class SingleThreadPoolDemo {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService pool = Executors.newSingleThreadExecutor();

        for (int i = 1; i <= 4; i++) {
            final int id = i;
            pool.submit(() ->
                    System.out.println("task " + id + " on " + Thread.currentThread().getName()));
        }

        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
        System.out.println("(all tasks used the same single worker — serialized)");
    }
}
