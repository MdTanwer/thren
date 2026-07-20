/**
 * Same as VisibilityBug, but volatile creates happens-before:
 * write to volatile ready  happens-before  later read of ready.
 * So reader is guaranteed to see ready==true AND the earlier data=42.
 */
public class VisibilityFixedVolatile {

    static volatile boolean ready = false;
    static int data = 0; // protected by happens-before through volatile ready

    public static void main(String[] args) throws InterruptedException {
        Thread reader = new Thread(() -> {
            while (!ready) {
                // spin
            }
            System.out.println("[reader] saw ready=true, data=" + data + " (should be 42)");
        }, "reader");

        reader.start();
        Thread.sleep(100);

        data = 42;       // this write happens-before the volatile write below
        ready = true;    // volatile write — publishes prior writes to the reader
        System.out.println("[writer] set data=42, ready=true");

        reader.join();
    }
}
