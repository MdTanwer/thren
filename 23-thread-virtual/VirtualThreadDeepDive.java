import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Virtual threads in depth: scale, executor, blocking, vs platform cost intuition.
 * Pair with: 23-thread-virtual/virtual-threads.md
 */
public class VirtualThreadDeepDive {

    public static void main(String[] args) throws Exception {
        demoManyVirtualThreads();
        demoBlockingFriendly();
        demoVirtualExecutor();
        demoCurrentThreadIsVirtualNotCarrier();
        System.out.println("\nDone — see virtual-threads.md");
    }

    /** Millions possible in theory; here we start thousands of sleeping VTs cheaply. */
    static void demoManyVirtualThreads() throws InterruptedException {
        System.out.println("\n--- many virtual threads (sleep) ---");
        int n = 5_000;
        Thread[] threads = new Thread[n];
        AtomicInteger ran = new AtomicInteger();
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            threads[i] = Thread.ofVirtual().unstarted(() -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                ran.incrementAndGet();
            });
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        long ms = System.currentTimeMillis() - t0;
        System.out.println("started+joined " + n + " virtual threads, ran=" + ran.get() + " in ~" + ms + " ms");
        System.out.println("(same with platform threads would be far heavier)");
    }

    /** VTs shine when tasks block — carriers are reused. */
    static void demoBlockingFriendly() throws InterruptedException {
        System.out.println("\n--- blocking I/O style work on VTs ---");
        Thread t = Thread.ofVirtual().name("io-sim").start(() -> {
            System.out.println("before block: " + Thread.currentThread());
            try {
                Thread.sleep(100); // stands in for blocking I/O
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("after block:  " + Thread.currentThread()
                    + " still virtual=" + Thread.currentThread().isVirtual());
        });
        t.join();
    }

    static void demoVirtualExecutor() throws Exception {
        System.out.println("\n--- newVirtualThreadPerTaskExecutor ---");
        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> f1 = exec.submit(() -> {
                Thread.sleep(30);
                return "one@" + Thread.currentThread().isVirtual();
            });
            Future<String> f2 = exec.submit(() -> {
                Thread.sleep(30);
                return "two@" + Thread.currentThread().isVirtual();
            });
            System.out.println(f1.get());
            System.out.println(f2.get());
        }
    }

    static void demoCurrentThreadIsVirtualNotCarrier() throws InterruptedException {
        System.out.println("\n--- currentThread() is the virtual thread ---");
        Thread.ofVirtual().start(() -> {
            Thread self = Thread.currentThread();
            System.out.println("currentThread()=" + self);
            System.out.println("isVirtual()=" + self.isVirtual());
            System.out.println("toString shows carrier mount info in JDK impl, but identity is VT");
        }).join();
    }
}
