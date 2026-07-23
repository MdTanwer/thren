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

## 4. Joins

**Goal:** Make the **current** thread wait until **another** thread has finished (`run()` returned / thread terminated).

```java
worker.start();
worker.join();   // current thread pauses here until worker dies
// safe: worker's work is done
```

### Overloads

| Call | Meaning |
|------|---------|
| `t.join()` | Wait until `t` terminates (no time limit) |
| `t.join(ms)` | Wait at most ~ms; may return while `t` is still alive |
| `t.join(ms, nanos)` | Same idea; timing still not exact (OS-limited, like `sleep`) |

Like `sleep`, `join` can throw `InterruptedException` if the waiting thread is interrupted.

### How the JVM executes this

```
main                         worker
────                         ──────
worker.start()
worker.join()  ──────────►   running...
  (WAITING / TIMED_WAITING)
  paused, not using CPU      ... still working ...
                             run() ends → TERMINATED
  ◄───────────────────────── woken up
continues...
```

1. **`join()`** parks the caller until the target thread’s life ends.
2. It is **not** busy-waiting — efficient, like sleep.
3. **`join(timeout)`** can return early even if the other thread is still alive — check `t.isAlive()` if you care.
4. **Interrupt** on the waiting thread aborts `join` with `InterruptedException` (target may still be running).

### Why it matters

Without `join()`, `main` can finish (or use a result) **before** the worker is done. `join()` is the basic “wait for that task to complete.”

### Examples in this repo

```bash
cd 04-joins
javac *.java
java BasicJoin
java JoinWithTimeout
java JoinAndInterrupt
```

---

## 5. Thread Interference

**Goal:** Understand why shared data breaks when two threads touch it at once — even with “simple” code like `c++`.

### The naive `Counter`

```java
public void increment() { c++; }
public void decrement() { c--; }
```

Looks like one step. The JVM actually does **three** steps for `c++`:

1. **Read** current `c`
2. **Add** 1 to that value (in a register / local)
3. **Write** the result back to `c`

Same idea for `c--`.

### Lost update (Oracle’s interleaving)

Start with `c = 0`. Thread A increments, Thread B decrements at the “same time”:

```
A: read c (=0)
B: read c (=0)
A: compute 0+1 → 1
B: compute 0-1 → -1
A: write 1 into c
B: write -1 into c   ← A's update is overwritten / lost
```

Final `c` can be wrong. Next run might lose B’s update, or (by luck) look correct. That **non-determinism** is why these bugs are hard to catch.

### How the JVM / CPU makes this possible

```
Thread A CPU                    Thread B CPU
─────────                       ─────────
load c                          load c
add 1                           sub 1
store c                         store c
         ▲ steps can interleave in any order ▲
```

Both threads read a **stale** snapshot, then both write — last write wins; the other update disappears.

This is a classic **race condition** on shared mutable state. Fix comes later: **synchronization** (locks), or atomic types — Oracle’s next sections.

### Examples in this repo

```bash
cd 05-thread-interference
javac *.java
java InterferenceDemo   # run several times — often Actual ≠ 0
java VisibleRace        # slowed 3-step increment so loss is obvious
```

---

## 6. Memory Consistency Errors

**Goal:** Different threads can have **different views** of the “same” variable. Not only can updates interleave (interference) — a write in thread A might **not be visible** to thread B yet.

### Oracle’s example

```java
int counter = 0;
// Thread A:  counter++;
// Thread B:  System.out.println(counter);  // might print 0!
```

In one thread, you’d expect `1`. Across threads, **no guarantee** unless you create a **happens-before** relationship.

### Happens-before (the strategy)

**Happens-before** = a JVM guarantee: writes by statement X are visible to statement Y (and ordering is respected).

You don’t need CPU-cache theory — you need actions that **create** happens-before edges.

### Actions you already know

| Action | Guarantee |
|--------|-----------|
| **`Thread.start()`** | Everything the starter did *before* `start()` is visible **to** the new thread |
| **`Thread.join()`** | Everything the finished thread did is visible **to** the joiner *after* `join()` returns |
| **Synchronization** | Coming next (`synchronized`, locks) — also creates happens-before |
| **`volatile` writes/reads** | Write to volatile happens-before later read of that volatile |

```
main writes shared=7
main calls start()  ──happens-before──►  worker reads shared (sees 7)
worker writes shared=99
worker ends
main join() returns  ◄──happens-before──  main reads shared (sees 99)
```

### Interference vs consistency (short)

| Problem | Symptom |
|---------|---------|
| **Interference** | Two threads update together → lost updates (`c++` race) |
| **Memory consistency** | One thread wrote, another never sees it (or sees it out of order) |

Both are fixed by establishing happens-before (sync, volatile, atomics, join, etc.).

### Examples in this repo

```bash
cd 06-memory-consistency
javac *.java
java HappensBeforeStartJoin   # start/join make shared visible — no volatile needed
java VisibilityFixedVolatile  # volatile ready publishes data=42
java VisibilityBug            # may hang or “get lucky” — no happens-before
```

---

## 7. Synchronized Methods

**Goal:** Fix **interference** (lost updates) and **memory consistency** (stale reads) by guarding shared object state with a **lock**.

### Oracle’s pattern

```java
public class SynchronizedCounter {
    private int c = 0;

    public synchronized void increment() { c++; }
    public synchronized void decrement() { c--; }
    public synchronized int value()       { return c; }
}
```

Add `synchronized` to the method declaration. Each instance has **one lock** (the object itself).

### Two effects (Oracle)

**1. Mutual exclusion (no interleaving on this object)**  
Only **one** thread at a time can run **any** `synchronized` method on the **same object**. Others **block** until the lock is released.

```
Thread A: increment()  ── acquires lock on counter
Thread B: decrement()  ── BLOCKED (waits for A)
Thread A: exits method ── releases lock
Thread B: runs
```

**2. Happens-before (visibility)**  
When a `synchronized` method **exits**, its writes are visible to the **next** thread that enters **any** `synchronized` method on that **same** object.

Unlock happens-before the next lock on that monitor.

### How the JVM executes this

```
synchronized void increment() { c++; }

  bytecode equivalent:
    monitorenter   // acquire lock on "this"
    ... c++ ...
    monitorexit    // release lock (also on exception paths)
```

- Lock is tied to the **object** (`this` for instance methods).
- All `synchronized` methods on **one** object share **one** lock.
- Two **different** `SynchronizedCounter` instances → two locks → no blocking between them.

### Rules & warnings

| Topic | Detail |
|-------|--------|
| Constructors | **Cannot** be `synchronized` (syntax error) |
| Safe strategy | If an object is shared, read/write its fields only via `synchronized` methods |
| `final` fields | After construction, can be read without sync (immutable) |
| **this leak** | Don’t publish `this` in constructor (e.g. `instances.add(this)`) — other threads may see half-built object |
| Liveness | Sync fixes correctness but can cause **deadlock** / blocking later |

### Examples in this repo

```bash
cd 07-synchronized-methods
javac *.java
java SynchronizedCounterDemo   # always 0 (vs InterferenceDemo)
java SynchronizedBlocking      # waiter blocks until slow thread releases lock
```

---

## 8. Intrinsic Locks and Synchronization

**Goal:** Understand the **lock** behind `synchronized` — what it is, where it lives, and finer control with `synchronized` blocks.

### Intrinsic lock (monitor)

Every Java object has a hidden **intrinsic lock** (monitor). Synchronization uses it for:

1. **Exclusive access** — only one thread “owns” the lock at a time; others block.
2. **Happens-before** — **release** of the lock happens-before the next **acquire** of the same lock.

```
Thread A: acquire lock → read/write fields → release lock
Thread B:                    (blocked)              acquire → sees A's writes
```

### Locks in synchronized methods

| Kind | Lock used |
|------|-----------|
| Instance `synchronized void m()` | **`this`** (that object) |
| `static synchronized void m()` | **`Class` object** for that class (separate from any instance lock) |

Lock is released when the method returns — **even on exception**.

### Synchronized statements (blocks)

Pick **which object** provides the lock:

```java
public void addName(String name) {
    synchronized (this) {
        lastName = name;
        nameCount++;
    }
    nameList.add(name); // outside lock — less blocking
}
```

Use when you need:

- **Smaller critical section** (don’t hold lock while calling other objects’ methods).
- **Fine-grained locks** — different fields, different lock objects.

### MsLunch — fine-grained locking

```java
private Object lock1 = new Object();
private Object lock2 = new Object();

void inc1() { synchronized (lock1) { c1++; } }
void inc2() { synchronized (lock2) { c2++; } }
```

`inc1` and `inc2` can run **in parallel** because they use **different** locks.  
**Only safe if `c1` and `c2` are truly independent.**

### Reentrant synchronization

A thread can acquire a lock it **already owns** (same thread enters nested `synchronized` on the same monitor). Without this, `outer()` calling `inner()` would **deadlock itself**.

```
outer()  → acquire lock on this
  inner() → re-enter same lock (same thread) ✓
  release
release
```

### Examples in this repo

```bash
cd 08-intrinsic-locks
javac *.java
java AddNameExample
java MsLunch
java ReentrantDemo
java StaticVsInstanceLock
```

---

## 9. Atomic Access

**Goal:** Know which operations are **atomic** (all-or-nothing, no interleaving mid-action) and when that is — and isn’t — enough for thread safety.

### What “atomic” means

An atomic action **completes fully** or not at all. No other thread sees a “half-done” effect.

**Not atomic:** `c++` (read → add → write — three steps, can interleave).

### What IS atomic (Java memory model)

| Operation | Atomic? |
|-----------|---------|
| Read/write **reference** | Yes |
| Read/write **int, short, byte, char, boolean, float** | Yes |
| Read/write **long, double** (plain) | **No** (can be torn on some platforms) |
| Read/write **any field declared `volatile`** | Yes (including long/double) |

Atomic actions **cannot interleave** with each other on the same variable — no thread interference on that single read/write.

### But atomic ≠ fully thread-safe

Two separate issues:

| Problem | Does atomic read/write alone fix it? |
|---------|--------------------------------------|
| **Interference** on `c++` | No — compound action, not one atomic op |
| **Memory consistency** | Partially — **`volatile`** also creates happens-before |

**`volatile` guarantees:**

- Write to volatile **happens-before** later read of that volatile.
- Reader sees the latest volatile value **and** side effects that happened before that write.

**`volatile` does NOT make `c++` safe** — still read-modify-write.

For true atomic increment, use **`java.util.concurrent.atomic`** (`AtomicInteger`, etc.) — covered more in High-Level Concurrency Objects.

### volatile vs synchronized vs atomic

| Tool | Typical use |
|------|-------------|
| `volatile` | One writer / flag / publish state; visibility |
| `synchronized` | Guard compound logic + visibility |
| `AtomicInteger` etc. | Lock-free atomic read-modify-write (often faster for simple counters) |

Atomics are faster than sync for simple ops but need more care — compound check-then-act still needs design (e.g. `compareAndSet`).

### Examples in this repo

```bash
cd 09-atomic-access
javac *.java
java AtomicIntegerCounter   # always correct count
java VolatileCounter        # volatile ≠ atomic increment
java VolatileNotEnough      # check-then-act race despite volatile
java LongTornRead           # long needs volatile for atomic access (spec rule)
```

---

## 10. Deadlock

**Goal:** Two or more threads **blocked forever**, each waiting for a lock the other holds.

### Oracle’s Alphonse & Gaston story

Each `Friend` has `synchronized bow()` and `bowBack()`. Rule: stay bowed until friend bows back.

```
Thread 1: alphonse.bow(gaston)   → locks Alphonse
          calls gaston.bowBack() → needs Gaston lock

Thread 2: gaston.bow(alphonse)   → locks Gaston
          calls alphonse.bowBack() → needs Alphonse lock

Thread 1 holds Alphonse, wants Gaston
Thread 2 holds Gaston, wants Alphonse
→ circular wait → DEADLOCK (neither can proceed)
```

### Four conditions (Coffman) — all must hold for deadlock

1. **Mutual exclusion** — resource (lock) held by one thread at a time
2. **Hold and wait** — thread holds a lock while waiting for another
3. **No preemption** — locks aren’t forcibly taken away
4. **Circular wait** — A waits for B, B waits for A (cycle in the wait graph)

Break **any one** → avoid deadlock. Common fix: **lock ordering** — always acquire locks in the same global order.

### How the JVM executes this

```
alphonse.bow(gaston):
  monitorenter alphonse     ✓ acquired
  gaston.bowBack(this):
    monitorenter gaston     ✗ BLOCKED (Thread 2 owns gaston)

gaston.bow(alphonse):
  monitorenter gaston       ✓ acquired
  alphonse.bowBack(this):
    monitorenter alphonse   ✗ BLOCKED (Thread 1 owns alphonse)
```

Both threads stuck in `TIMED_WAITING` / `BLOCKED` on the other’s monitor — forever unless interrupted.

### Fix in this repo (`DeadlockFixed`)

Always lock **both** friends in name order (`Alphonse` before `Gaston`). No cycle possible.

### Examples in this repo

```bash
cd 10-deadlock
javac *.java
timeout 3 java Deadlock      # hangs — classic deadlock (use Ctrl+C or timeout)
java DeadlockFixed           # completes cleanly
```

---

## 11. Guarded Blocks

**Goal:** Coordinate threads efficiently — wait for a condition instead of busy-spinning.

### Bad vs good guard

```java
// BAD — burns CPU
while (!joy) {}

// GOOD — park until notified
public synchronized void guardedJoy() {
    while (!joy) {
        wait();  // releases lock, sleeps until notify/notifyAll
    }
    // proceed
}
```

### Rules (Oracle)

1. Call **`wait()`** only while holding the object’s intrinsic lock (`synchronized`).
2. Always `wait()` inside a **`while`** that re-checks the condition (spurious wakeups / wrong event).
3. Other thread sets the condition, then **`notifyAll()`** (or `notify()`).
4. After `wait()` returns, the waiter **re-acquires** the lock before continuing.

### How the JVM executes wait / notifyAll

```
Thread A (waiter)                    Thread B (notifier)
─────────────────                    ───────────────────
synchronized(d) {
  while (!joy) {
    wait() ──► release lock
               state WAITING
                                     synchronized(d) {
                                       joy = true
                                       notifyAll() ──► wake A
                                     }  // release lock
    ◄── re-acquire lock
  }
  use joy...
}
```

| Method | Effect |
|--------|--------|
| `wait()` | Release lock + park until notified (or interrupted) |
| `notifyAll()` | Wake **all** threads waiting on this monitor |
| `notify()` | Wake **one** arbitrary waiter (rare; use when many identical workers) |

### Producer–Consumer (`Drop`)

Shared object with one slot:

| State | Meaning |
|-------|---------|
| `empty == true` | Consumer must `wait`; producer may `put` |
| `empty == false` | Producer must `wait`; consumer may `take` |

```
Producer put → empty=false → notifyAll
Consumer take → empty=true  → notifyAll
```

They alternate — never overwrite unread data, never take empty.

### Examples in this repo

```bash
cd 11-guarded-blocks
javac *.java
java GuardedJoyDemo
java ProducerConsumerExample
```

---

## 12. A Synchronized Class Example (`SynchronizedRGB`)

**Goal:** Even a fully synchronized mutable class can look **inconsistent** if a client does **multiple** reads without holding the lock across them.

### What the class does

Color = `red`, `green`, `blue` + `name`. Mutators/accessors use `synchronized` so a single `set` / `getRGB` / `getName` / `invert` is atomic.

### The trap

```java
int myColorInt = color.getRGB();      // Statement 1 — releases lock after return
String myColorName = color.getName(); // Statement 2 — another thread may set() in between
```

Each call is synchronized **by itself**, but **together** they are not one atomic snapshot.

```
Reader: getRGB() → 0x000000 ("black" bits)
Changer: set(255,255,255, "Pure White")
Reader: getName() → "Pure White"   ← RGB and name don't match!
```

### Fix — bind the statements

```java
synchronized (color) {
    int myColorInt = color.getRGB();
    String myColorName = color.getName();
}
```

Same lock for both reads → `set` cannot sneak in. Works because synchronized methods are **reentrant** on `this`.

### Lesson

| Level | Safe? |
|-------|-------|
| One synchronized method call | Yes — consistent for that call |
| Several calls without outer sync | No — can mix old RGB + new name |
| Client holds lock across related reads | Yes — consistent compound snapshot |

Better long-term fix (next Oracle section): make the class **immutable** so no `set` can change mid-read.

### Examples in this repo

```bash
cd 12-synchronized-rgb
javac *.java
java InconsistentSnapshot   # often many mismatches
java ConsistentSnapshot     # mismatches = 0
```

---

## 13. High Level Concurrency Objects (overview)

**Goal:** Move from hand-rolled `Thread` / `synchronized` / `wait` to **`java.util.concurrent`** building blocks designed for multi-core apps.

### Low-level vs high-level

| Low-level (so far) | High-level (Java 5+) |
|--------------------|----------------------|
| `new Thread(...).start()` | **Executors** — submit tasks to a pool |
| `synchronized` / intrinsic lock | **Lock objects** — `ReentrantLock`, tryLock, fairness |
| Manual sync around lists/maps | **Concurrent collections** — `ConcurrentHashMap`, etc. |
| `volatile` / careful sync for counters | **Atomic variables** — `AtomicInteger`, CAS |
| Shared `Random` (contention) | **ThreadLocalRandom** (JDK 7+) |

Low-level APIs still work for small demos. Large apps need pools, richer locks, and lock-free/shared structures.

### What’s coming (Oracle’s map)

1. **Lock objects** — flexible locking idioms beyond `synchronized`
2. **Executors** — launch/manage threads; thread pools for scale
3. **Concurrent collections** — less boilerplate sync for shared data
4. **Atomic variables** — minimize sync; avoid consistency errors
5. **ThreadLocalRandom** — fast per-thread random numbers

Package home: `java.util.concurrent` (+ concurrent Collections types).

### Taste in this repo

```bash
cd 13-high-level-overview
javac *.java
java HighLevelOverview
```

Shows `ExecutorService` + `AtomicInteger` + `ThreadLocalRandom` together. Deep dives follow as you paste each Oracle subsection.

---

## 14. Lock Objects

**Goal:** Use `java.util.concurrent.locks.Lock` (usually `ReentrantLock`) when you need to **back out** of acquiring a lock — something `synchronized` cannot do.

### Intrinsic lock vs `Lock`

| | `synchronized` | `Lock` (`ReentrantLock`) |
|--|----------------|--------------------------|
| Acquire | Enter block/method | `lock.lock()` |
| Release | Automatic on exit | **Must** `unlock()` in `finally` |
| Wait if busy | Always blocks | Can **`tryLock()`** / timeout / interrupt |
| Conditions | `wait` / `notify` | `Condition` objects |

Same rule: only **one** thread owns a given lock at a time. Reentrant: same thread can re-enter.

### Biggest advantage — back out

```java
if (lock.tryLock()) {           // or tryLock(timeout, unit)
    try { /* critical */ }
    finally { lock.unlock(); }
} else {
    // lock busy — do something else (no deadlock wait)
}

lock.lockInterruptibly();       // give up if interrupted while waiting
```

### Safelock — deadlock fix for Alphonse & Gaston

Old deadlock: each thread held **one** friend’s lock and waited for the other.

New approach: `tryLock()` on **both** locks. If either fails → unlock what you got → skip the bow (no circular wait).

```
impendingBow:
  myLock   = this.lock.tryLock()
  yourLock = other.lock.tryLock()
  if not both → unlock any you hold → return false
  else return true → bow → unlock both in finally
```

### Examples in this repo

```bash
cd 14-lock-objects
javac *.java
java TryLockDemo    # tryLock fails immediately / with timeout
java Safelock       # bows for ~2s, never deadlocks
```

---

## 15. Executor Interfaces

**Goal:** Launch tasks through an **executor** instead of `new Thread(...).start()`. Declare variables as interface types (`Executor`, `ExecutorService`, `ScheduledExecutorService`).

### The hierarchy

```
Executor
  └── execute(Runnable)
        │
        └── ExecutorService
              ├── submit(Runnable|Callable) → Future
              ├── invokeAll / invokeAny
              └── shutdown / shutdownNow
                    │
                    └── ScheduledExecutorService
                          ├── schedule(delay)
                          ├── scheduleAtFixedRate
                          └── scheduleWithFixedDelay
```

### `Executor`

```java
// Old
(new Thread(r)).start();

// New — drop-in style
e.execute(r);
```

`execute` may: run on a **new** thread, reuse a **pool** worker, or **queue** the task. Spec does not require “always new thread.”

### `ExecutorService`

| Feature | Meaning |
|---------|---------|
| `submit(Runnable)` | Returns `Future<?>` (no result) |
| `submit(Callable<T>)` | Returns `Future<T>` — get result later |
| `Future.get()` | Wait for result / rethrow task exception |
| `shutdown()` | No new tasks; finish queued |
| `shutdownNow()` | Try to cancel running; interrupt |

Tasks should handle **interrupts** so shutdown can stop them.

### `ScheduledExecutorService`

| Method | Behavior |
|--------|----------|
| `schedule(..., delay)` | Once, after delay |
| `scheduleAtFixedRate` | Repeat at fixed period from start |
| `scheduleWithFixedDelay` | Wait *delay* after each finish, then again |

### Examples in this repo

```bash
cd 15-executor-interfaces
javac *.java
java ExecutorExecuteDemo
java ExecutorServiceSubmitDemo
java ScheduledExecutorDemo
```

---

## Topics (more coming)

<!-- Next: Thread Pools, ... -->
