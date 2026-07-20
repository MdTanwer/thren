/**
 * Reentrant sync: same thread acquires the same lock twice (outer + inner).
 * Without reentrancy, the inner call would deadlock on itself.
 */
public class ReentrantDemo {

    public synchronized void outer() {
        System.out.println("[outer] owns lock, calling inner()");
        inner(); // same thread, same lock (this) — allowed
        System.out.println("[outer] inner returned");
    }

    public synchronized void inner() {
        System.out.println("[inner] same thread re-entered synchronized method");
    }

    public static void main(String[] args) {
        ReentrantDemo demo = new ReentrantDemo();
        demo.outer();
    }
}
