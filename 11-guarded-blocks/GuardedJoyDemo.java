/**
 * Minimal guardedJoy: waiter parks until notifier sets joy and notifyAll().
 */
public class GuardedJoyDemo {

    private boolean joy = false;

    public synchronized void guardedJoy() {
        while (!joy) {
            try {
                System.out.println("[waiter] joy=false — wait()");
                wait();
            } catch (InterruptedException e) {
            }
        }
        System.out.println("[waiter] Joy and efficiency have been achieved!");
    }

    public synchronized void notifyJoy() {
        joy = true;
        System.out.println("[notifier] joy=true — notifyAll()");
        notifyAll();
    }

    public static void main(String[] args) throws InterruptedException {
        GuardedJoyDemo demo = new GuardedJoyDemo();

        Thread waiter = new Thread(demo::guardedJoy, "waiter");
        waiter.start();

        Thread.sleep(500);
        demo.notifyJoy();

        waiter.join();
    }
}
