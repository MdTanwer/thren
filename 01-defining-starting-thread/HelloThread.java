// /**
//  * Way 2: Subclass Thread and override run().
//  * Simpler for tiny demos, but your class cannot extend anything else.
//  */
// public class HelloThread extends Thread {

//     public HelloThread(String name) {
//         super(name);
//     }

//     @Override
//     public void run() {
//         System.out.println("Hello from Thread subclass: " + Thread.currentThread().getName());
//     }

//     public static void main(String[] args) {
//         System.out.println("Main starts on: " + Thread.currentThread().getName());

//         HelloThread t = new HelloThread("worker-2");
//         t.start();

//         System.out.println("Main continues on: " + Thread.currentThread().getName());
//     }
// }

public class HelloThread   extends Thread  {

    public HelloThread(String name){
        super(name);
    }

    @Override
    public void run(){
        System.out.println("hello form the thread: " + Thread.currentThread().getName());
    }
    public static void main(String[] args) {
        System.out.println("hello form the main thread");
        HelloThread t = new HelloThread("worker-1");
        t.start();

        System.out.println("main continues on: " + Thread.currentThread().getName());
    }
}