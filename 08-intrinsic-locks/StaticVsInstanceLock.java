/**
 * Static synchronized uses the Class object's lock, not an instance lock.
 * Two instance sync methods block each other; static sync is a separate lock.
 */
public class StaticVsInstanceLock {

    static int staticCount = 0;
    int instanceCount = 0;

    public static synchronized void bumpStatic() {
        staticCount++;
    }

    public synchronized void bumpInstance() {
        instanceCount++;
    }

    public static void main(String[] args) throws InterruptedException {
        StaticVsInstanceLock a = new StaticVsInstanceLock();
        StaticVsInstanceLock b = new StaticVsInstanceLock();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10_000; i++) {
                a.bumpInstance();
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10_000; i++) {
                b.bumpInstance(); // different objects → different locks → parallel
            }
        });

        Thread t3 = new Thread(() -> {
            for (int i = 0; i < 10_000; i++) {
                StaticVsInstanceLock.bumpStatic(); // Class lock — shared by all instances
            }
        });

        t1.start();
        t2.start();
        t3.start();
        t1.join();
        t2.join();
        t3.join();

        System.out.println("a.instanceCount=" + a.instanceCount + " (expect 10000)");
        System.out.println("b.instanceCount=" + b.instanceCount + " (expect 10000)");
        System.out.println("staticCount=" + staticCount + " (expect 10000)");
    }
}
