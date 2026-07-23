import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * BlockingQueue — put blocks when full; take blocks when empty.
 * Natural fit for producer/consumer (no wait/notify boilerplate).
 */
public class BlockingQueueDemo {

    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(2); // capacity 2

        Thread producer = new Thread(() -> {
            try {
                for (String msg : new String[]{"A", "B", "C", "DONE"}) {
                    System.out.println("[producer] putting " + msg);
                    queue.put(msg); // blocks if queue is full
                    System.out.println("[producer] put OK, size=" + queue.size());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            try {
                String msg;
                while (!(msg = queue.take()).equals("DONE")) { // blocks if empty
                    System.out.println("[consumer] got " + msg);
                    Thread.sleep(200); // slower than producer → queue fills
                }
                System.out.println("[consumer] got DONE");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
    }
}
