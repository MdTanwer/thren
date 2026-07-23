import java.util.concurrent.*;
import java.util.concurrent.Flow;

/**
 * Modern thread APIs (Java 21+ / 25): virtual threads, Flow.
 * StructuredTaskScope APIs evolve by JDK — see java-util-concurrent.md §17.
 *
 *   make compile && java -cp target VirtualThreadsDemo
 */
public class VirtualThreadsDemo {

    public static void main(String[] args) throws Exception {
        demoVirtualThread();
        demoVirtualThreadExecutor();
        demoSubmissionPublisher();
        System.out.println("\nSee java-util-concurrent.md sections 15–21 for full API map.");
    }

    static void demoVirtualThread() throws InterruptedException {
        System.out.println("\n--- Thread.ofVirtual / startVirtualThread ---");
        Thread t = Thread.ofVirtual().name("vt-demo").start(() -> {
            Thread self = Thread.currentThread();
            System.out.println("name=" + self.getName()
                    + " virtual=" + self.isVirtual()
                    + " " + self);
        });
        t.join();

        Thread.startVirtualThread(() ->
                System.out.println("startVirtualThread ok, virtual="
                        + Thread.currentThread().isVirtual()));
        Thread.sleep(50);
    }

    static void demoVirtualThreadExecutor() throws Exception {
        System.out.println("\n--- Executors.newVirtualThreadPerTaskExecutor ---");
        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Integer> f = exec.submit(() -> {
                System.out.println("task on " + Thread.currentThread());
                return 21 * 2;
            });
            System.out.println("result=" + f.get());
        }
    }

    static void demoSubmissionPublisher() throws InterruptedException {
        System.out.println("\n--- Flow.SubmissionPublisher ---");
        try (SubmissionPublisher<String> pub = new SubmissionPublisher<>()) {
            CountDownLatch done = new CountDownLatch(1);
            pub.subscribe(new Flow.Subscriber<>() {
                private Flow.Subscription sub;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    sub = subscription;
                    sub.request(1);
                }

                @Override
                public void onNext(String item) {
                    System.out.println("subscriber got: " + item);
                    sub.request(1);
                }

                @Override
                public void onError(Throwable throwable) {
                    throwable.printStackTrace();
                    done.countDown();
                }

                @Override
                public void onComplete() {
                    System.out.println("subscriber complete");
                    done.countDown();
                }
            });
            pub.submit("hello");
            pub.submit("flow");
            pub.close();
            done.await(2, TimeUnit.SECONDS);
        }
    }
}
