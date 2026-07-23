# `java.lang.Thread` & Virtual Threads — In-Depth Guide

Module: **java.base** · Package: **java.lang** · Class: **`Thread`**

```
Object
  └── Thread  implements Runnable
        └── ForkJoinWorkerThread  (subclass)
```

**Since:** 1.0 · Virtual threads / `Thread.Builder`: **Java 21+**

**Run demos:**

```bash
make compile
java -cp target ThreadApiDemo
java -cp target VirtualThreadDeepDive
```

Official docs: [Thread (Java SE)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html) · [Virtual Threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)

---

## Table of contents

1. [What is a Thread?](#1-what-is-a-thread)
2. [Platform threads vs virtual threads](#2-platform-threads-vs-virtual-threads)
3. [Creating and starting threads](#3-creating-and-starting-threads)
4. [Thread.Builder API](#4-threadbuilder-api)
5. [Lifecycle and Thread.State](#5-lifecycle-and-threadstate)
6. [Identity: name, threadId, toString](#6-identity-name-threadid-tostring)
7. [Daemon threads and JVM shutdown](#7-daemon-threads-and-jvm-shutdown)
8. [Priority](#8-priority)
9. [Sleep, yield, join, onSpinWait](#9-sleep-yield-join-onspinwait)
10. [Interrupts](#10-interrupts)
11. [ThreadLocal and inheritance](#11-threadlocal-and-inheritance)
12. [UncaughtExceptionHandler](#12-uncaughtexceptionhandler)
13. [Carrier threads, pinning, scheduling props](#13-carrier-threads-pinning-scheduling-props)
14. [Deprecated / dangerous APIs](#14-deprecated--dangerous-apis)
15. [Method & field quick reference](#15-method--field-quick-reference)
16. [When to use which](#16-when-to-use-which)

---

## 1. What is a Thread?

A **thread** is a thread of execution in a program. The JVM allows many threads to run **concurrently**.

- Creating a `Thread` object is **not** enough — you must **`start()`** it.
- `start()` schedules the thread to execute its **`run()`** method.
- The new thread runs **concurrently** with the thread that called `start()`.

A thread **terminates** when:

1. `run()` completes normally, or  
2. `run()` throws, and the **uncaught exception handler** finishes.

Then use **`join()`** to wait for termination from another thread.

```java
Thread t = Thread.ofVirtual().start(() -> System.out.println("running"));
t.join(); // wait until t finishes
```

`Thread` implements **`Runnable`**. Prefer passing a `Runnable` / lambda over subclassing `Thread` (Oracle concurrency tutorial style). Subclass exists mainly for customization; known subclass: **`ForkJoinWorkerThread`**.

---

## 2. Platform threads vs virtual threads

### Platform threads

| Trait | Detail |
|-------|--------|
| Mapping | Typically **1:1** with OS/kernel threads |
| Resources | Large stack + OS-managed resources |
| Best for | Any task, including **CPU-heavy** work |
| Limit | Scarce — thousands may already hurt |
| Default name | Auto: `"Thread-" + n` |
| Daemon | Configurable (`setDaemon`) |
| Priority | Configurable (`MIN`…`MAX`) |
| Thread group | Yes |

Constructors like `new Thread(runnable)` create **platform** threads.

### Virtual threads (Java 21+)

| Trait | Detail |
|-------|--------|
| Mapping | **User-mode**; scheduled by the **Java runtime** |
| Resources | Very cheap — JVM may host **millions** |
| Best for | Tasks that **block** a lot (I/O, `sleep`, waits) |
| Not for | Long **CPU-intensive** loops (use platform / pools) |
| Default name | **Empty string** until you set one |
| Daemon | **Always** daemon (cannot make non-daemon) |
| Priority | Always **`NORM_PRIORITY`** (changes ignored) |
| `currentThread()` | Always returns the **virtual** `Thread`, not the carrier |

### How virtual threads run (carriers)

```
millions of virtual threads
        │
        ▼
 small pool of platform "carrier" threads (≈ CPU count)
```

When a virtual thread **blocks** (I/O, park, etc.), the runtime can **unmount** it and let the carrier run another virtual thread. Your code only sees the virtual `Thread` via `currentThread()`.

---

## 3. Creating and starting threads

### Classic constructors → always platform

```java
new Thread(runnable).start();
new Thread(runnable, "worker").start();
```

Equivalent builder form:

```java
Thread.ofPlatform().name("worker").start(runnable);
```

### Preferred modern APIs

```java
// Virtual — start immediately
Thread vt = Thread.ofVirtual().start(runnable);
Thread vt2 = Thread.startVirtualThread(runnable); // same as ofVirtual().start

// Platform — start immediately
Thread pt = Thread.ofPlatform().daemon().start(runnable);

// Unstarted — call start() later
Thread t = Thread.ofPlatform().name("duke").unstarted(runnable);
t.start();
```

**Rules:**

- `start()` at most **once** — restart after terminate → `IllegalThreadStateException`
- Do **not** call `run()` yourself if you want a new thread (that runs on the current thread)
- For virtual threads, invoking `run()` directly does **nothing** (per API)

---

## 4. Thread.Builder API

Nested types:

| Type | Role |
|------|------|
| `Thread.Builder` | Common builder |
| `Thread.Builder.OfPlatform` | Platform threads + factory |
| `Thread.Builder.OfVirtual` | Virtual threads + factory |

### Examples (from Oracle docs)

```java
Runnable runnable = () -> System.out.println(Thread.currentThread());

// Daemon platform thread, started
Thread thread = Thread.ofPlatform().daemon().start(runnable);

// Named, unstarted platform thread
Thread duke = Thread.ofPlatform().name("duke").unstarted(runnable);
duke.start();

// ThreadFactory: worker-0, worker-1, ...
ThreadFactory factory =
    Thread.ofPlatform().daemon().name("worker-", 0).factory();

// Virtual thread started
Thread v = Thread.ofVirtual().start(runnable);

// Factory for virtual threads
ThreadFactory vFactory = Thread.ofVirtual().factory();
```

### Inheritance of thread-locals

At creation, a child normally inherits **inheritable** thread-locals (and context class loader) from the parent.

```java
// Builder: opt out
Thread.ofVirtual()
      .inheritInheritableThreadLocals(false)
      .start(runnable);

// 5-arg constructor also controls inheritInheritableThreadLocals
```

**Security note:** Creating a **platform** thread captures caller context (Inherited AccessControlContext). Creating a **virtual** thread does **not** — VTs have no permissions for privileged actions in that model.

---

## 5. Lifecycle and Thread.State

```java
Thread.State state = t.getState();
```

| State | Meaning |
|-------|---------|
| `NEW` | Created, not started |
| `RUNNABLE` | Executing or ready (may wait for CPU) |
| `BLOCKED` | Waiting to enter a `synchronized` monitor |
| `WAITING` | Waiting forever (`Object.wait`, `join`, `LockSupport.park`, …) |
| `TIMED_WAITING` | Waiting with timeout (`sleep`, timed `join`/`wait`) |
| `TERMINATED` | Finished |

`getState()` is for **monitoring**, not for synchronization control.

```
NEW ──start()──► RUNNABLE ◄──► BLOCKED / WAITING / TIMED_WAITING
                      │
                      └──► TERMINATED
```

---

## 6. Identity: name, threadId, toString

```java
long id = t.threadId();   // unique, stable (prefer over deprecated getId())
String name = t.getName();
t.setName("orders-worker");

boolean virtual = t.isVirtual();
```

| | Platform | Virtual |
|--|----------|---------|
| Default name | `"Thread-n"` | `""` (empty) |
| `threadId()` | Unique long | Unique long |
| `toString()` | Often id, name, priority, group | Includes id / name / virtual info |

---

## 7. Daemon threads and JVM shutdown

The JVM shutdown sequence begins when **all started non-daemon** platform threads have terminated.

```java
t.setDaemon(true);  // BEFORE start()
```

| | Platform | Virtual |
|--|----------|---------|
| Daemon default | Inherits from parent | **Always daemon** |
| `setDaemon(false)` on VT | — | **IllegalArgumentException** |

Unstarted non-daemon threads do **not** delay shutdown.

**Practical tip:** Even with VTs (always daemon), **`join()`** / await executor shutdown if you need work to finish before main exits.

---

## 8. Priority

```java
Thread.MIN_PRIORITY  // 1
Thread.NORM_PRIORITY // 5
Thread.MAX_PRIORITY  // 10

t.setPriority(Thread.MAX_PRIORITY); // platform only effectively
```

- Platform: clamped by thread group max priority  
- Virtual: **always `NORM_PRIORITY`**; `setPriority` ignores the new value  

---

## 9. Sleep, yield, join, onSpinWait

### `sleep`

```java
Thread.sleep(100);
Thread.sleep(100, 500_000);      // ms + nanos
Thread.sleep(Duration.ofMillis(100)); // Java 19+
```

Does **not** release monitor locks you hold. Throws `InterruptedException` and clears interrupt status.

### `yield`

Hint to the scheduler — may be ignored. Rarely needed in production.

### `join`

```java
t.join();                          // wait forever
t.join(1000);                      // wait up to 1s
t.join(Duration.ofSeconds(1));     // Java 19+; returns true if terminated
```

Do **not** use `wait`/`notify` on `Thread` instances yourself (platform `join` uses that internally).

### `onSpinWait` (Java 9+)

Hint inside a busy-wait loop:

```java
while (!ready) {
    Thread.onSpinWait();
}
```

---

## 10. Interrupts

Cooperative cancellation — does **not** kill the thread.

```java
t.interrupt();

// In the worker:
if (Thread.interrupted()) { // clears flag
    return;
}
// or
if (Thread.currentThread().isInterrupted()) { // does not clear
    return;
}
```

If blocked in `sleep` / `join` / `wait` → throws `InterruptedException` and clears status.  
Also interacts with interruptible NIO channels / selectors.

---

## 11. ThreadLocal and inheritance

```java
static final ThreadLocal<String> USER = ThreadLocal.withInitial(() -> "anon");
static final InheritableThreadLocal<String> REQ =
        new InheritableThreadLocal<>();

USER.set("alice");
REQ.set("req-1");

Thread.ofVirtual().start(() -> {
    // USER: not inherited (plain ThreadLocal)
    // REQ: inherited if inheritInheritableThreadLocals true (default)
    System.out.println("REQ=" + REQ.get());
});
```

With **thread pools**, always `USER.remove()` in `finally` — otherwise values leak to the next task on the same worker.

Context class loader is a special inheritable thread-local-like mechanism (`get`/`setContextClassLoader`).

---

## 12. UncaughtExceptionHandler

If `run()` throws and nothing catches it:

1. Thread’s own handler (if set)  
2. Else `ThreadGroup.uncaughtException`  
3. Else default handler  

```java
Thread.setDefaultUncaughtExceptionHandler((thread, ex) ->
    System.err.println(thread + " => " + ex));

Thread.ofVirtual()
      .uncaughtExceptionHandler((t, e) -> System.err.println("VT fail: " + e))
      .start(() -> { throw new RuntimeException("boom"); });
```

Nested interface: **`Thread.UncaughtExceptionHandler`**.

---

## 13. Carrier threads, pinning, scheduling props

### Carrier

Virtual thread ↔ mounts on a **platform carrier**. Blocking can unmount so another VT runs.

### Pinning

While a virtual thread holds a **`synchronized`** monitor and blocks, it may **pin** the carrier (carrier stuck with that VT). Prefer **`ReentrantLock`** for locks held across blocking calls when using VTs heavily.

### JDK scheduler system properties

| Property | Meaning |
|----------|---------|
| `jdk.virtualThreadScheduler.parallelism` | Platform threads for scheduling VTs (default ≈ available processors) |
| `jdk.virtualThreadScheduler.maxPoolSize` | Max platform threads for scheduler (default **256**) |

```bash
java -Djdk.virtualThreadScheduler.parallelism=8 -cp target VirtualThreadDeepDive
```

### Enumeration caveats

`activeCount()`, `enumerate(...)`, `getAllStackTraces()` deal with **platform** threads — **virtual threads are excluded** from those estimates/maps.

`getThreadGroup()` for a VT returns the special virtual-threads group.

---

## 14. Deprecated / dangerous APIs

| Method | Status | Why |
|--------|--------|-----|
| `stop()` | Deprecated for removal | Unlocks monitors abruptly → corrupted state |
| `suspend()` / `resume()` | Deprecated for removal | Deadlock-prone |
| `countStackFrames()` | Deprecated for removal | Ill-defined |
| `checkAccess()` | Deprecated for removal | Security Manager going away |
| `getId()` | Deprecated | Prefer final **`threadId()`** |

Use **interrupt + flag** for cooperative shutdown instead of `stop`.

---

## 15. Method & field quick reference

### Fields

`MIN_PRIORITY`, `NORM_PRIORITY`, `MAX_PRIORITY`

### Nested types

`Thread.Builder`, `Thread.Builder.OfPlatform`, `Thread.Builder.OfVirtual`, `Thread.State`, `Thread.UncaughtExceptionHandler`

### Create / start

`ofPlatform()`, `ofVirtual()`, `startVirtualThread(Runnable)`, constructors (platform), `start()`, `run()`

### Query / identity

`currentThread()`, `isVirtual()`, `threadId()`, `getName()` / `setName()`, `getState()`, `isAlive()`, `toString()`, `isDaemon()` / `setDaemon()`, `getPriority()` / `setPriority()`, `getThreadGroup()`

### Coordination

`join()`, `join(long)`, `join(long,int)`, `join(Duration)`, `sleep(...)`, `yield()`, `onSpinWait()`, `interrupt()`, `interrupted()`, `isInterrupted()`, `holdsLock(Object)`

### Diagnostics

`getStackTrace()`, `getAllStackTraces()` (platform only), `dumpStack()`, `activeCount()`, `enumerate(Thread[])`

### Class loading / errors

`getContextClassLoader()`, `setContextClassLoader()`, `setUncaughtExceptionHandler()`, `getUncaughtExceptionHandler()`, `setDefaultUncaughtExceptionHandler()`, `getDefaultUncaughtExceptionHandler()`

---

## 16. When to use which

| Situation | Choice |
|-----------|--------|
| Blocking I/O, many concurrent requests | **Virtual threads** (`ofVirtual` / `newVirtualThreadPerTaskExecutor`) |
| Heavy CPU (encoding, crypto, number crunching) | **Platform threads** or fixed **thread pool** sized ≈ cores |
| One-off background task | `ofVirtual().start` or platform daemon |
| Need thread to keep JVM alive | **Non-daemon platform** thread (VTs cannot) |
| Custom naming + factories for pools | `Thread.Builder` → `factory()` |
| Shared per-thread context | `ThreadLocal` (clean up!) or **`ScopedValue`** with modern structured concurrency |

### Scale intuition

```
10_000 blocking tasks:
  platform threads → often too expensive / thrash
  virtual threads  → natural fit (each task one VT)
```

---

## See also in this repo

| Path | Content |
|------|---------|
| `23-thread-virtual/ThreadApiDemo.java` | Builder, states, daemon, interrupt, join |
| `23-thread-virtual/VirtualThreadDeepDive.java` | VT vs platform, many VTs, executor, carrier awareness |
| `22-virtual-threads/VirtualThreadsDemo.java` | Short VT + Flow tour |
| `java-util-concurrent.md` §15–17 | Package-level map including StructuredTaskScope |

---

*Based on the Oracle `java.lang.Thread` specification (platform vs virtual, Builder, lifecycle, and method contracts).*
