/**
 * Way 1: Pass a Runnable to Thread.
 * Preferred — task (Runnable) is separate from the worker (Thread).
 */
public class HelloRunnable implements Runnable {

    @Override
    public void run() {
        System.out.println("Hello from Runnable thread: " + Thread.currentThread().getName());
    }

    public static void main(String[] args) {
        System.out.println("Main starts on: " + Thread.currentThread().getName());

        Thread t = new Thread(new HelloRunnable(), "worker-1");
        t.start(); // schedules run() on a NEW thread — do NOT call run() yourself

        System.out.println("Main continues on: " + Thread.currentThread().getName());
    }
}
