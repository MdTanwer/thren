/**
 * Oracle's point: atomic visibility ≠ full thread safety.
 * Two volatile writes from different threads — each read is atomic,
 * but compound logic (check-then-act) still needs sync or atomics.
 */
public class VolatileNotEnough {

    private volatile int balance = 100;

    /** NOT safe: two threads can both pass the check and withdraw. */
    public void withdrawUnsafe(int amount) {
        if (balance >= amount) {
            try {
                Thread.sleep(1); // widen the race window for demo
            } catch (InterruptedException e) {
                return;
            }
            balance -= amount;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        VolatileNotEnough account = new VolatileNotEnough();

        Runnable withdraw60 = () -> account.withdrawUnsafe(60);

        Thread t1 = new Thread(withdraw60, "t1");
        Thread t2 = new Thread(withdraw60, "t2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("Started balance: 100, two threads each try to withdraw 60");
        System.out.println("If only one succeeds (serialized): balance = 40");
        System.out.println("If both pass check (race):         balance = -20");
        System.out.println("Actual balance:   " + account.balance);
    }
}
