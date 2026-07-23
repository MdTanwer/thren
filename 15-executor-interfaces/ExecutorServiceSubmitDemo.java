import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ExecutorService.submit → Future for result / status / cancel.
 */
public class ExecutorServiceSubmitDemo {

    public static void main(String[] args) throws Exception {
        ExecutorService service = Executors.newFixedThreadPool(2);

        // Runnable — no return value (Future.get() returns null)
        Future<?> runnableFuture = service.submit(() ->
                System.out.println("Runnable on " + Thread.currentThread().getName()));

        // Callable — returns a value
        Callable<Integer> sumTask = () -> {
            int sum = 0;
            for (int i = 1; i <= 100; i++) {
                sum += i;
            }
            return sum;
        };
        Future<Integer> callableFuture = service.submit(sumTask);

        runnableFuture.get(); // wait until done
        System.out.println("Callable result (1..100 sum): " + callableFuture.get());
        System.out.println("callableFuture.isDone()=" + callableFuture.isDone());

        service.shutdown(); // stop accepting new tasks; finish queued ones
    }
}
