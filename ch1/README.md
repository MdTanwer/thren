# Chapter 1 — Java Thread & Runnable Notes

## 1. Lambda vs. Anonymous Class with Runnable

A lambda expression is an anonymous function passed to a constructor or method for subsequent execution.
Lambdas only work with functional interfaces — interfaces that declare exactly one abstract method (e.g., Runnable).
The lambda is definitely less verbose than the equivalent anonymous class.

💻 Example — Lambda & Anonymous Class
```java
// Lambda (concise)
Runnable r1 = () -> System.out.println("Hello from thread");

// Equivalent anonymous class (verbose)
Runnable r2 = new Runnable() {
    @Override
    public void run() {
        System.out.println("Hello from thread");
    }
};
```

## 2. Connecting Runnable to a Thread
Way A: Pass Runnable into Thread constructor
```java
Thread t = new Thread(r);               // Thread(Runnable runnable)
Thread t2 = new Thread(r, "Worker-1");  // with custom name
```
Way B: Extend Thread (when using no-arg constructor)

Thread() does not accept a Runnable argument.
Because Thread implements Runnable, you must extend Thread and override run().

💻 Example — Extending Thread
```java
class MyThread extends Thread {
   @Override
   public void run() {
      System.out.println("Hello from thread");
   }
}

MyThread mt = new MyThread();
// mt.start() would be called later
```

## 3. Thread State / Properties Stored in a Thread Object
A Thread object encapsulates the following state:

Name
Alive or Dead (isAlive())
Execution state (getState())
Priority (getPriority() / setPriority())
Daemon or Nondaemon status


## 4. Processors, Cores, and the OS Scheduler

When a computer has enough processors/cores, the OS assigns a separate thread to each so they execute simultaneously.
When there are not enough, threads wait their turns to use shared processors/cores.
Runtime.getRuntime().availableProcessors() returns the number available to the JVM.
The value can change during JVM execution.
It is never smaller than 1.



💻 Example
```java
int cores = Runtime.getRuntime().availableProcessors();
System.out.println("Processors available to JVM: " + cores);
```
OS Schedulers Mentioned

| OS / Version | Scheduler |
| --- | --- |
| Linux 2.6 – 2.6.23 | O(1) Scheduler |
| Linux 2.6.23+ | Completely Fair Scheduler (default) |
| Windows NT / XP / Vista / 7 | Multilevel feedback queue scheduler |

Schedulers often combine:
Preemptive scheduling — higher priority threads preempt (interrupt) lower priority threads.
Round-robin scheduling — equal priority threads get time slices and take turns.




## 5. Parallelism vs. Concurrency

Parallelism: “A condition that arises when at least two threads are executing simultaneously.”
Concurrency: “A condition that exists when at least two threads are making progress. [It is a] more generalized form of parallelism that can include time-slicing as a form of virtual parallelism.”
— Oracle Multithreading Guide


## 6. Thread Priority

getPriority() — returns current priority.
setPriority(int priority) — sets priority.
Range: Thread.MIN_PRIORITY to Thread.MAX_PRIORITY.
Default: Thread.NORMAL_PRIORITY.

💻 Example
```java
Thread t = new Thread(r);
System.out.println(t.getPriority());       // 5 (NORMAL)
t.setPriority(Thread.MIN_PRIORITY);         // 1
t.setPriority(Thread.MAX_PRIORITY);         // 10
```
⚠️ Portability Caution

Using setPriority() can impact portability.
Some OS schedulers delay lower priority threads until higher priority threads finish → leads to indefinite postponement / starvation.
Other schedulers do not delay them.
Do not rely on priority for program correctness.


## 7. Daemon Threads

A daemon thread acts as a helper to a nondaemon thread.
It dies automatically when the application’s last nondaemon thread dies, so the application can terminate.

💻 Example
```java
Runnable r = () -> {
    while (true) {
        System.out.println("Daemon running...");
    }
};

Thread t = new Thread(r);
t.setDaemon(true);   // default is false (nondaemon)
t.start();

// If main thread exits now, JVM terminates because t is daemon.
```

## 8. Starting a Thread (start())

start() tells the runtime to create the underlying OS thread and schedule it.
The run() method is invoked later by the new thread.
start() returns immediately — it does not wait for the thread to finish.
When execution leaves run(), the thread is destroyed.
The Thread object is no longer viable after its thread dies.

⚠️ Calling start() again on a thread that is running or has died throws IllegalThreadStateException.
💻 Example — IllegalThreadStateException
```java
Thread t = new Thread(() -> System.out.println("Run"));
t.start();
// t.start(); // RUNTIME EXCEPTION: IllegalThreadStateException
```

## 9. Complete Example — Thread Fundamentals (Listing 1-1)

The isDaemon boolean is set to true if any command-line argument is passed (args.length != 0).
Thread.currentThread() obtains a reference to the currently executing thread.
isAlive() and getState() are queried both before and after starting.

💻 Full Source
```java
public class ThreadDemo
{
   public static void main(String[] args)
   {
      boolean isDaemon = args.length != 0;

      Runnable r = new Runnable()
                   {
                      @Override
                      public void run()
                      {
                         Thread thd = Thread.currentThread();
                         while (true)
                            System.out.printf("%s is %salive and in %s " +
                                              "state%n",
                                              thd.getName(),
                                              thd.isAlive() ? "" : "not ",
                                              thd.getState());
                      }
                   };

      Thread t1 = new Thread(r, "thd1");
      if (isDaemon)
         t1.setDaemon(true);

      System.out.printf("%s is %salive and in %s state%n",
                        t1.getName(),
                        t1.isAlive() ? "" : "not ",
                        t1.getState());

      Thread t2 = new Thread(r);
      t2.setName("thd2");
      if (isDaemon)
         t2.setDaemon(true);

      System.out.printf("%s is %salive and in %s state%n",
                        t2.getName(),
                        t2.isAlive() ? "" : "not ",
                        t2.getState());

      t1.start();
      t2.start();
   }
}
```
Behavior

No arguments: Both threads are nondaemon → they run indefinitely (prints forever).
With argument (e.g., java ThreadDemo x): Both threads are daemon → they die shortly after the main thread ends.


## 10. Advanced Thread Tasks

### A. Interrupting Threads
One thread can interrupt another. The mechanism consists of three methods:

| Method | Behavior |
| --- | --- |
| void interrupt() | Interrupts the thread. If blocked in sleep() or join(), clears interrupted status and throws InterruptedException. Otherwise, sets interrupted status. |
| static boolean interrupted() | Tests if current thread is interrupted → returns true, then clears the interrupted status. |
| boolean isInterrupted() | Tests if this thread is interrupted → returns true, does not clear status. |

💻 Example — Thread Interruption (Listing 1-2 concept)
```java
public class ThreadDemo {
    public static void main(String[] args) {
        Runnable r = () -> {
            int count = 0;
            // Thread.interrupted() checks AND clears the flag
            while (!Thread.interrupted()) {
                System.out.println(Thread.currentThread().getName() + ": " + count++);
            }
        };

        Thread t1 = new Thread(r);
        Thread t2 = new Thread(r);
        t1.start();
        t2.start();

        // Bad: busy loop to waste time (text explicitly says this is NOT ideal)
        while (Math.random() < 0.9999) { /* wait */ }

        t1.interrupt();
        t2.interrupt();
    }
}
```
Sample Output
```
Thread-1: 67
Thread-1: 68
Thread-0: 768
Thread-1: 69
...
```

### B. Joining Threads (join())
When a thread starts a worker thread for a long task, it can wait for the worker to die.
Three join() overloads

void join() — wait indefinitely.
void join(long millis) — wait at most millis milliseconds.
void join(long millis, int nanos) — wait millis + nanoseconds.

💻 Example — Computing Pi with join() (Listing 1-3)

Uses Machin’s 1700s formula:π/4 = 4·arctan(1/5) − arctan(1/239)
A worker thread computes π to 50,000 digits.
main calls join() and waits for the result.

```java
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ThreadDemo {
    private static final RoundingMode roundingMode = RoundingMode.HALF_UP;

    public static void main(String[] args) throws InterruptedException {
        BigDecimal[] result = new BigDecimal[1];

        // Lambda runnable
        Runnable r = () -> {
            BigDecimal pi = arctan(5, 50000).multiply(BigDecimal.valueOf(16))
                             .subtract(arctan(239, 50000).multiply(BigDecimal.valueOf(4)));
            result[0] = pi;
        };

        Thread t = new Thread(r);
        t.start();        // launch worker

        t.join();         // main thread waits until worker dies

        System.out.println(result[0]);
    }

    /*
     * Compute arctan(1/inverseX) to specified scale using power series:
     * arctan(x) = x - (x^3)/3 + (x^5)/5 - (x^7)/7 + ...
     */
    public static BigDecimal arctan(int inverseX, int scale) {
       BigDecimal result, numer, term;
       BigDecimal invX = BigDecimal.valueOf(inverseX);
       BigDecimal invX2 = BigDecimal.valueOf(inverseX * inverseX);

       numer = BigDecimal.ONE.divide(invX, scale, roundingMode);
       result = numer;
       int i = 1;
       do {
          numer = numer.divide(invX2, scale, roundingMode);
          int denom = 2 * i + 1;
          term = numer.divide(BigDecimal.valueOf(denom), scale, roundingMode);
          if ((i % 2) != 0)
             result = result.subtract(term);
          else
             result = result.add(term);
          i++;
       } while (term.compareTo(BigDecimal.ZERO) != 0);
       return result;
    }
}
```
Output prefix (from text)
```
3.141592653589793238462643383279502884197169399375105820974944592...
```

### C. Sleeping (Thread.sleep())
Sleeping is preferable to busy loops because it does not waste processor cycles.
Two static methods

static void sleep(long millis)
static void sleep(long millis, int nanos)


Throws IllegalArgumentException if arguments are out of range.
Throws InterruptedException if interrupted while sleeping.
The interrupted status is cleared when InterruptedException is thrown.
Actual sleep time is subject to system timer precision and accuracy.

💻 Example — Thread Sleep (Listing 1-4)
```java
public class ThreadDemo
{
   public static void main(String[] args)
   {
      Runnable r = new Runnable()
                   {
                      @Override
                      public void run()
                      {
                         String name = Thread.currentThread().getName();
                         int count = 0;
                         while (!Thread.interrupted())
                            System.out.println(name + ": " + count++);
                      }
                   };

      Thread thdA = new Thread(r);
      Thread thdB = new Thread(r);
      thdA.start();
      thdB.start();

      try {
         Thread.sleep(2000);   // Sleep for ~2 seconds
      }
      catch (InterruptedException ie) {
         // ignore for this demo
      }

      thdA.interrupt();
      thdB.interrupt();
   }
}
```

Because the sleep time is approximate, output line counts vary slightly between runs.


## 11. Exercise Solutions (Q&A Format)

| # | Question | Answer |
| --- | --- | --- |
| 1 | Define thread. | An independent path of execution through an application’s code. |
| 2 | Define runnable. | An object whose code is encapsulated to be executed by a thread; the code lives in run(). |
| 3 | What do Thread and Runnable accomplish? | Thread provides a consistent interface to the OS threading architecture. Runnable supplies the code to be executed (the run() method). |
| 4 | Two ways to create a Runnable? | 1) Lambda or anonymous class implementing Runnable directly. 2) Extend Thread (since Thread implements Runnable). |
| 5 | Two ways to connect runnable to Thread? | 1) Pass Runnable to Thread constructor. 2) Extend Thread and override run() (for the no-arg constructor). |
| 6 | Five kinds of Thread state? | Name, alive/dead, execution state, priority, daemon/nondaemon. |
| 7 | True/False: Default thread name starts with "Thd-" prefix. | False. It starts with Thread- (e.g., Thread-0). |
| 8 | How to give a thread a nondefault name? | Pass it to the constructor new Thread(r, "name") or call setName("name"). |
| 9 | How to determine if a thread is alive or dead? | Call isAlive(). |
| 10 | Identify Thread.State enum constants. | NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED. |
| 11 | How to obtain current thread execution state? | Call getState() on the Thread object. |
| 12 | Define priority. | Thread relative importance used by the OS scheduler. |
| 13 | How can setPriority() impact portability? | Different schedulers handle priority differently; some starve low-priority threads indefinitely, others do not. |
| 14 | Range of values for setPriority()? | Thread.MIN_PRIORITY (1) to Thread.MAX_PRIORITY (10). |
| 15 | True/False: Daemon thread dies automatically when last nondaemon dies. | True. |
| 16 | What does start() do if thread is running or has died? | Throws IllegalThreadStateException. |
| 17 | How to stop an unending application on Windows? | Press Ctrl+C. |
| 18 | Methods forming Thread’s interruption mechanism? | interrupt(), interrupted(), isInterrupted(). |
| 19 | True/False: isInterrupted() clears interrupted status. | False. interrupted() clears it; isInterrupted() does not. |
| 20 | What does a thread do when interrupted? | If blocked in sleep()/join(), it throws InterruptedException and clears status. Otherwise, its interrupted status is set. |
| 21 | Define a busy loop. | A loop of statements designed to waste time / spin until some condition becomes true. |
| 22 | Thread methods that let a thread wait for another to die? | join(), join(long millis), join(long millis, int nanos). |
| 23 | Thread methods that let a thread sleep? | sleep(long millis), sleep(long millis, int nanos). |



## 12. Exercise Application — IntSleep
Requirement: Create a background thread that repeatedly outputs Hello, then sleeps for 100 ms. After sleeping for 2 seconds, main interrupts the background thread, which should break out of the loop after outputting interrupted.
💻 Full Solution
```java
public class IntSleep {
    public static void main(String[] args) throws InterruptedException {
        Runnable r = () -> {
            try {
                while (true) {
                    System.out.println("Hello");
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                System.out.println("interrupted");
            }
        };

        Thread t = new Thread(r);
        t.start();

        // Main thread sleeps for 2 seconds
        Thread.sleep(2000);

        // Interrupt background thread
        t.interrupt();
    }
}
```

