/**
 * long/double reads and writes are NOT guaranteed atomic unless volatile.
 * On 32-bit JVMs this was classic; on 64-bit it's rarer but the rule still applies per spec.
 *
 * Demo: writer sets long to all-ones, reader may briefly see torn (half-updated) value
 * without volatile. With volatile, read is always atomic.
 */
public class LongTornRead {

    static long plainLong = 0L;
    static volatile long volatileLong = 0L;

    static final long ALL_ONES = 0xFFFFFFFFFFFFFFFFL;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== plain long (may see torn reads on some platforms) ===");
        runTest(false);

        System.out.println("\n=== volatile long (atomic read/write guaranteed) ===");
        runTest(true);
    }

    static void runTest(boolean useVolatile) throws InterruptedException {
        java.util.concurrent.atomic.AtomicBoolean sawBad = new java.util.concurrent.atomic.AtomicBoolean(false);

        Thread writer = new Thread(() -> {
            for (int i = 0; i < 1_000_000; i++) {
                if (useVolatile) {
                    volatileLong = 0L;
                    volatileLong = ALL_ONES;
                } else {
                    plainLong = 0L;
                    plainLong = ALL_ONES;
                }
            }
        }, "writer");

        Thread reader = new Thread(() -> {
            for (int i = 0; i < 1_000_000; i++) {
                long v = useVolatile ? volatileLong : plainLong;
                if (v != 0L && v != ALL_ONES) {
                    sawBad.set(true);
                    System.out.println("[reader] torn value: 0x" + Long.toHexString(v));
                    return;
                }
            }
        }, "reader");

        writer.start();
        reader.start();
        writer.join();
        reader.join();

        if (!sawBad.get()) {
            System.out.println("[reader] no torn values observed this run (may still happen without volatile)");
        }
    }
}
