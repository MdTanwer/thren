import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ScheduledExecutorService — delay and periodic tasks.
 */
public class ScheduledExecutorDemo {

    public static void main(String[] args) throws InterruptedException {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger ticks = new AtomicInteger(0);

        // Run once after 300ms delay
        scheduler.schedule(
                () -> System.out.println("delayed task at " + System.currentTimeMillis()),
                300, TimeUnit.MILLISECONDS);

        // Fixed rate: every 200ms
        scheduler.scheduleAtFixedRate(
                () -> System.out.println("tick " + ticks.incrementAndGet()),
                0, 200, TimeUnit.MILLISECONDS);

        Thread.sleep(900);
        scheduler.shutdown();
        scheduler.awaitTermination(1, TimeUnit.SECONDS);
        System.out.println("total ticks=" + ticks.get());
    }
}
