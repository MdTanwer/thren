/**
 * Memory visibility bug: writer sets ready=true, reader may NEVER see it
 * (or see stale data) without happens-before (volatile / sync / join / ...).
 *
 * On some machines this hangs; on others it "accidentally" works.
 * Use -Dfail=true mentally: without volatile, there is NO guarantee.
 */
public class VisibilityBug {

    // NOT volatile — no happens-before between writer store and reader load
    static boolean ready = false;
    static int data = 0;

    public static void main(String[] args) throws InterruptedException {
        Thread reader = new Thread(() -> {
            while (!ready) {
                // spin — may keep reading a cached ready==false forever
            }
            System.out.println("[reader] saw ready=true, data=" + data);
            // data might still be 0 even if ready looked true (reordering),
            // without a proper happens-before edge
        }, "reader");

        reader.start();

        Thread.sleep(100); // give reader time to start spinning

        data = 42;
        ready = true;
        System.out.println("[writer] set data=42, ready=true");

        reader.join(2000);
        if (reader.isAlive()) {
            System.out.println("[main] reader still stuck after 2s — classic visibility hang");
            System.out.println("        (JVM/CPU never made writer's writes visible)");
            System.exit(0); // stop the spinning thread for this demo
        }
    }
}
