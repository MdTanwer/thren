# Java Concurrency — Learning Notes

Notes from the [Oracle Concurrency Tutorial](https://docs.oracle.com/javase/tutorial/essential/concurrency/).

---

## 1. Defining and Starting a Thread

**Goal:** Put code on a *new* thread of execution (not just another method call).

### Two ways to give a thread its work

| Approach | How | When to use |
|----------|-----|-------------|
| **Runnable** | Class implements `Runnable`, pass it to `new Thread(runnable)` | **Preferred** — task can extend another class; works with executors later |
| **Subclass Thread** | Class extends `Thread`, override `run()` | Tiny demos only — Java has single inheritance, so your class is locked to `Thread` |

Both start the same way: call **`start()`**, never treat `run()` as “start a thread.”

```java
// Preferred
new Thread(new HelloRunnable()).start();

// Also valid, less flexible
new HelloThread().start();
```

### What `start()` vs `run()` does

- `thread.start()` → JVM creates a **new** thread, then that thread calls `run()`.
- `thread.run()` → just a normal method call on the **current** thread (usually `main`). No concurrency.

### How the JVM executes this (mental model)

```
main thread                          new worker thread
─────────────                        ─────────────────
new Thread(runnable)  ──► Thread object exists (NEW state)
start()               ──► JVM registers thread with scheduler (RUNNABLE)
                          OS/JVM picks it up → calls run()
main keeps going...       work inside run() runs in parallel
                          run() returns → thread dies (TERMINATED)
```

1. **`new Thread(...)`** — creates a `Thread` object in memory. Code for the task is attached (`Runnable` or overridden `run`). The thread is **not** running yet.
2. **`start()`** — asks the JVM to create a real native/OS thread (or reuse a platform thread) and schedule it. Returns immediately to the caller.
3. **Scheduler** — when the new thread gets CPU time, the JVM invokes `run()` on that thread.
4. **`run()` finishes** — the thread ends. Calling `start()` again on the same object throws `IllegalThreadStateException`.

Oracle focuses on **Runnable + Thread** because later APIs (`ExecutorService`, thread pools) take `Runnable`/`Callable` — the task is separate from *who* runs it.

### Examples in this repo

```bash
cd 01-defining-starting-thread
javac *.java
java HelloRunnable
java HelloThread
java StartVsRun
```

---

## 2. Pausing Execution with Sleep

**Goal:** Temporarily suspend the *current* thread so other threads (or apps) can use the CPU, or to pace work over time.

### API

```java
Thread.sleep(4000);        // milliseconds
Thread.sleep(4000, 500_000); // ms + nanoseconds (still not exact)
```

- Affects **only the thread that calls it** (usually `main` or a worker).
- Time is a **request**, not a promise — OS timer resolution and scheduling can make it wake a bit late (or early in rare cases).
- Can end early if another thread **interrupts** this one → throws `InterruptedException` (covered in Interrupts).

### Oracle example idea

Print a line, wait ~4 seconds, repeat — pacing, not busy-waiting:

```java
Thread.sleep(4000);
System.out.println(importantInfo[i]);
```

`main` declares `throws InterruptedException` because `sleep` can throw it. This tiny app has no interrupter, so it lets the exception propagate out of `main`.

### How the JVM / OS executes this

```
thread calls sleep(N)
        │
        ▼
 JVM asks OS: park this thread for ~N ms
        │
        ▼
 thread state → TIMED_WAITING  (not using CPU)
        │
        ├── other threads / apps get CPU time
        │
        ▼
 timer expires (or interrupt arrives)
        │
        ▼
 thread → RUNNABLE again → scheduler may resume it
```

1. **`Thread.sleep(ms)`** — current thread stops running Java bytecode and yields the CPU.
2. **Not a busy loop** — it does *not* spin checking the clock; the OS parks the thread (efficient).
3. **Wakeup** — after ~ms, or sooner on interrupt. Then the thread competes again for CPU like any other runnable thread.
4. **Precision** — limited by OS timers; never write code that assumes “exactly 4000 ms.”

### Examples in this repo

```bash
cd 02-pausing-with-sleep
javac *.java
java SleepMessages          # ~16s total (4 pauses × 4s)
java SleepLetsOthersRun     # main sleeps; worker keeps ticking
```

---

## 3. Interrupts

**Goal:** One thread signals another: “stop what you’re doing and do something else” (usually: exit `run()`).

**Important:** `interrupt()` only sets a flag / wakes waiters. The **target thread must cooperate** — ignore the signal and it keeps running.

### How you send an interrupt

```java
worker.interrupt();  // called on the Thread object of the other thread
```

### How a thread supports interruption (two cases)

**A) It’s blocked in `sleep` / `join` / `wait` / etc.** — those throw `InterruptedException`:

```java
try {
    Thread.sleep(4000);
} catch (InterruptedException e) {
    return; // stop the thread (common pattern)
}
```

**B) It’s doing long CPU work** — no exception is thrown automatically; poll:

```java
if (Thread.interrupted()) {
    return; // or: throw new InterruptedException();
}
```

### Interrupt status flag

| Method | Whose flag? | Clears flag? |
|--------|-------------|--------------|
| `t.interrupt()` | sets `t`'s flag | — |
| `Thread.interrupted()` | **current** thread | **Yes** |
| `t.isInterrupted()` | thread `t` | **No** |

By convention, methods that throw `InterruptedException` also **clear** the flag when they throw. Code that catches and rethrows (or wants callers to see the interrupt) often does:

```java
Thread.currentThread().interrupt(); // restore the flag
```

### How the JVM executes this

```
main                         worker
────                         ──────
worker.interrupt()
  │
  ├─ set interrupt status = true
  └─ if worker is in sleep/join/wait:
       wake it → throw InterruptedException (flag cleared)

                             if running normal code:
                               keeps going until it polls
                               Thread.interrupted() / isInterrupted()
```

Interrupts are **cooperative**, not forced kill (unlike older `Thread.stop()`, which is unsafe/deprecated).

### Examples in this repo

```bash
cd 03-interrupts
javac *.java
java InterruptViaSleep      # interrupt during sleep → catch & exit
java InterruptViaPolling    # interrupt during CPU loop → poll & exit
java InterruptStatusFlag    # interrupted() vs isInterrupted()
```

---

## Topics (more coming)

<!-- Next Oracle sections go here -->
