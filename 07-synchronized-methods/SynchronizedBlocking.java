/**
 * Shows effect 1: only one synchronized method runs on this object at a time.
 * Thread A holds the lock inside increment(); B blocks until A finishes.
 */
class SlowSynchronizedCounter {
    private int c = 0;

    public synchronized void increment() {
        try {
            Thread.sleep(1000); // simulates work while holding the lock
        } catch (InterruptedException e) {
            return;
        }
        c++;
    }

    public synchronized int value() {
        return c;
    }
}

public class SynchronizedBlocking {

    public static void main(String[] args) throws InterruptedException {
        SlowSynchronizedCounter counter = new SlowSynchronizedCounter();

        Thread slow = new Thread(() -> {
            System.out.println("[slow] entering synchronized increment (holds lock ~1s)");
            counter.increment();
            System.out.println("[slow] done");
        }, "slow");

        Thread waiter = new Thread(() -> {
            System.out.println("[waiter] calling increment — blocks until slow releases lock");
            counter.increment();
            System.out.println("[waiter] got lock, value=" + counter.value());
        }, "waiter");

        slow.start();
        Thread.sleep(50);
        waiter.start();

        slow.join();
        waiter.join();
    }
}
