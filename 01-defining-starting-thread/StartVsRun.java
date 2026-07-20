/**
 * Shows why start() matters: run() alone stays on the calling thread.
 */
public class StartVsRun implements Runnable {

    @Override
    public void run() {
        System.out.println("  run() executing on: " + Thread.currentThread().getName());
    }

    public static void main(String[] args) {
        StartVsRun task = new StartVsRun();
        Thread t = new Thread(task, "worker");

        System.out.println("1) Calling run() directly (WRONG for concurrency):");
        t.run(); // still main thread — no new OS/JVM thread

        System.out.println("2) Calling start() (CORRECT):");
        t.start(); // new thread; JVM will call run() there
    }
}
