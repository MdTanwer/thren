/**
 * Oracle's point: start() and join() already create happens-before.
 *
 * - Everything main did BEFORE start() is visible TO the new thread.
 * - Everything the worker did is visible to main AFTER join() returns.
 */
public class HappensBeforeStartJoin {

    static int shared = 0; // no volatile — visibility comes from start/join rules

    public static void main(String[] args) throws InterruptedException {
        shared = 7; // write in main BEFORE start

        Thread worker = new Thread(() -> {
            // Guaranteed to see writes that happened-before start()
            System.out.println("[worker] shared at start = " + shared + " (expect 7)");
            shared = 99;
        }, "worker");

        worker.start(); // start creates happens-before into the new thread
        worker.join();  // join creates happens-before back to main

        // Guaranteed to see worker's writes after successful join
        System.out.println("[main] shared after join = " + shared + " (expect 99)");
    }
}
