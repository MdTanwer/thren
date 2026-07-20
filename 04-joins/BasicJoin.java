// /**
//  * main waits for worker to finish via join(), then prints the result.
//  * Without join(), main might finish (or print) before worker is done.
//  */
// public class BasicJoin {

//     public static void main(String[] args) throws InterruptedException {
//         Thread worker = new Thread(() -> {
//             System.out.println("[worker] starting work");
//             try {
//                 Thread.sleep(1500);
//             } catch (InterruptedException e) {
//                 return;
//             }
//             System.out.println("[worker] done");
//         }, "worker");

//         worker.start();
//         System.out.println("[main] waiting for worker (join)...");
//         worker.join(); // main pauses until worker terminates
//         System.out.println("[main] worker finished — safe to continue");
//     }
// }


public class BasicJoin {


    static Runnable task1 = () -> {
        System.out.println("[task1] starting work");
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            return;
        }
        System.out.println("[task1] done");
    };

    static Runnable task2 = () -> {
        System.out.println("[task2] starting work");
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            return;
        }
        System.out.println("[task2] done");
    };
    public static void main(String[] args) throws InterruptedException {
        System.out.println("main starts on: " + Thread.currentThread().getName());
        System.out.println("hello form the main thread");

        Thread t1 = new Thread(task1, "worker-1");
        Thread t2 = new Thread(task2, "worker-2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("main continues on: " + Thread.currentThread().getName());
    }
}