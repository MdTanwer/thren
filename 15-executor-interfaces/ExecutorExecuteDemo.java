import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Executor.execute(r) replaces (new Thread(r)).start()
 * — but usually reuses a pooled worker instead of creating a new Thread each time.
 */
public class ExecutorExecuteDemo {

    public static void main(String[] args) throws InterruptedException {
        Executor e = Executors.newFixedThreadPool(2);

        for (int i = 1; i <= 4; i++) {
            final int taskId = i;
            // Old: (new Thread(r)).start();
            // New:
            e.execute(() -> System.out.println(
                    "task " + taskId + " on " + Thread.currentThread().getName()));
        }

        // Executor has no shutdown — cast or declare as ExecutorService in real code
        ((java.util.concurrent.ExecutorService) e).shutdown();
        Thread.sleep(200);
    }
}
