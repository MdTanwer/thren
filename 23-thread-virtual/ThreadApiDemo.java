import java.time.Duration;
import java.util.concurrent.ThreadFactory;

/**
 * Thread API essentials: Builder, State, join, interrupt, daemon, handlers.
 * Pair with: 23-thread-virtual/virtual-threads.md
 */
public class ThreadApiDemo {

    public static void main(String[] args) throws Exception {
        demoPlatformBuilder();
        demoVirtualBuilder();
        demoStatesAndJoin();
        demoInterruptDuringSleep();
        demoUncaughtHandler();
        demoThreadFactory();
        System.out.println("\nDone — see virtual-threads.md");
    }

    static void demoPlatformBuilder() throws InterruptedException {
        System.out.println("\n--- Platform Thread.Builder ---");
        Thread t = Thread.ofPlatform()
                .name("duke")
                .daemon(true)
                .start(() -> System.out.println(
                        "platform name=" + Thread.currentThread().getName()
                                + " daemon=" + Thread.currentThread().isDaemon()
                                + " virtual=" + Thread.currentThread().isVirtual()
                                + " id=" + Thread.currentThread().threadId()));
        t.join();
    }

    static void demoVirtualBuilder() throws InterruptedException {
        System.out.println("\n--- Virtual Thread.Builder ---");
        Thread t = Thread.ofVirtual()
                .name("vt-1")
                .start(() -> System.out.println(
                        "virtual name=" + Thread.currentThread().getName()
                                + " daemon=" + Thread.currentThread().isDaemon()
                                + " virtual=" + Thread.currentThread().isVirtual()
                                + " priority=" + Thread.currentThread().getPriority()));
        t.join();

        Thread quick = Thread.startVirtualThread(() ->
                System.out.println("startVirtualThread ok"));
        quick.join();
    }

    static void demoStatesAndJoin() throws InterruptedException {
        System.out.println("\n--- State + join(Duration) ---");
        Thread t = Thread.ofVirtual().unstarted(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        System.out.println("before start: " + t.getState()); // NEW
        t.start();
        System.out.println("after start:  " + t.getState()); // RUNNABLE or TIMED_WAITING
        boolean terminated = t.join(Duration.ofSeconds(2));
        System.out.println("join returned terminated=" + terminated + " state=" + t.getState());
    }

    static void demoInterruptDuringSleep() throws InterruptedException {
        System.out.println("\n--- interrupt during sleep ---");
        Thread t = Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(10_000);
                System.out.println("should not print");
            } catch (InterruptedException e) {
                System.out.println("woke with InterruptedException (cooperative cancel)");
            }
        });
        Thread.sleep(50);
        t.interrupt();
        t.join();
    }

    static void demoUncaughtHandler() throws InterruptedException {
        System.out.println("\n--- UncaughtExceptionHandler ---");
        Thread t = Thread.ofVirtual()
                .name("boom")
                .uncaughtExceptionHandler((th, ex) ->
                        System.out.println("handler: " + th.getName() + " -> " + ex.getMessage()))
                .start(() -> {
                    throw new RuntimeException("boom");
                });
        t.join();
    }

    static void demoThreadFactory() throws InterruptedException {
        System.out.println("\n--- ThreadFactory from builder ---");
        ThreadFactory factory = Thread.ofVirtual().name("worker-", 0).factory();
        Thread a = factory.newThread(() -> System.out.println("A on " + Thread.currentThread().getName()));
        Thread b = factory.newThread(() -> System.out.println("B on " + Thread.currentThread().getName()));
        a.start();
        b.start();
        a.join();
        b.join();
    }
}
