import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Shows Lock advantages over synchronized: tryLock and lockInterruptibly.
 */
public class TryLockDemo {

    private final Lock lock = new ReentrantLock();

    public void holdLockFor(long ms) {
        lock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " holds lock");
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
            System.out.println(Thread.currentThread().getName() + " released lock");
        }
    }

    public void tryAcquire() {
        if (lock.tryLock()) {
            try {
                System.out.println(Thread.currentThread().getName() + " got lock via tryLock");
            } finally {
                lock.unlock();
            }
        } else {
            System.out.println(Thread.currentThread().getName() + " tryLock failed — backed out (no wait)");
        }
    }

    public void tryAcquireWithTimeout() throws InterruptedException {
        if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
            try {
                System.out.println(Thread.currentThread().getName() + " got lock after waiting ≤100ms");
            } finally {
                lock.unlock();
            }
        } else {
            System.out.println(Thread.currentThread().getName() + " timed out — backed out");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        TryLockDemo demo = new TryLockDemo();

        Thread holder = new Thread(() -> demo.holdLockFor(500), "holder");
        holder.start();
        Thread.sleep(50);

        Thread t1 = new Thread(demo::tryAcquire, "tryLock-now");
        t1.start();
        t1.join();

        Thread t2 = new Thread(() -> {
            try {
                demo.tryAcquireWithTimeout();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "tryLock-timeout");
        t2.start();
        t2.join();
        holder.join();
    }
}
