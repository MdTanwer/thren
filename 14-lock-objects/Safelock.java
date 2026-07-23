import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Oracle Safelock — avoid deadlock with tryLock().
 * If both locks aren't acquired, release what you got and back out.
 *
 * Runs for a few seconds then exits (Oracle's version loops forever).
 */
public class Safelock {

    static class Friend {
        private final String name;
        private final Lock lock = new ReentrantLock();

        public Friend(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public boolean impendingBow(Friend bower) {
            boolean myLock = false;
            boolean yourLock = false;
            try {
                myLock = lock.tryLock();
                yourLock = bower.lock.tryLock();
            } finally {
                if (!(myLock && yourLock)) {
                    if (myLock) {
                        lock.unlock();
                    }
                    if (yourLock) {
                        bower.lock.unlock();
                    }
                }
            }
            return myLock && yourLock;
        }

        public void bow(Friend bower) {
            if (impendingBow(bower)) {
                try {
                    System.out.format("%s: %s has bowed to me!%n", this.name, bower.getName());
                    bower.bowBack(this);
                } finally {
                    lock.unlock();
                    bower.lock.unlock();
                }
            } else {
                System.out.format("%s: %s started to bow to me, but saw that I was already bowing to him.%n",
                        this.name, bower.getName());
            }
        }

        public void bowBack(Friend bower) {
            System.out.format("%s: %s has bowed back to me!%n", this.name, bower.getName());
        }
    }

    static class BowLoop implements Runnable {
        private final Friend bower;
        private final Friend bowee;
        private final long stopAt;

        public BowLoop(Friend bower, Friend bowee, long stopAt) {
            this.bower = bower;
            this.bowee = bowee;
            this.stopAt = stopAt;
        }

        @Override
        public void run() {
            Random random = new Random();
            while (System.currentTimeMillis() < stopAt) {
                try {
                    Thread.sleep(random.nextInt(10));
                } catch (InterruptedException e) {
                }
                bowee.bow(bower);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Friend alphonse = new Friend("Alphonse");
        Friend gaston = new Friend("Gaston");
        long stopAt = System.currentTimeMillis() + 2000;

        Thread t1 = new Thread(new BowLoop(alphonse, gaston, stopAt));
        Thread t2 = new Thread(new BowLoop(gaston, alphonse, stopAt));
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("--- finished without deadlock ---");
    }
}
