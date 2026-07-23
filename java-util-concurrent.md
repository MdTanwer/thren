# Package `java.util.concurrent` — In-Depth Guide

Utility classes for concurrent programming (since **Java 5**).  
Also covers related thread APIs in **`java.lang`**, **`.locks`**, **`.atomic`**, **`Flow`**, and **virtual threads / structured concurrency** (Java 21+).

**Runnable companions:**

| Class | Covers |
|-------|--------|
| `ConcurrentPackageTour` | Executors, queues, synchronizers, CHM, CompletableFuture |
| `VirtualThreadsDemo` | Virtual threads, `SubmissionPublisher` / Flow |

```bash
make compile
java -cp target ConcurrentPackageTour
java -cp target VirtualThreadsDemo
```

---

## Table of contents

1. [Why this package exists](#1-why-this-package-exists)
2. [Executors framework](#2-executors-framework)
3. [Futures and async results](#3-futures-and-async-results)
4. [CompletableFuture](#4-completablefuture)
5. [Fork/Join](#5-forkjoin)
6. [Queues](#6-queues)
7. [Synchronizers](#7-synchronizers)
8. [Concurrent collections](#8-concurrent-collections)
9. [ThreadLocalRandom](#9-threadlocalrandom)
10. [TimeUnit and timeouts](#10-timeunit-and-timeouts)
11. [Exceptions](#11-exceptions)
12. [Memory consistency (happens-before)](#12-memory-consistency-happens-before)
13. [Which tool should I use?](#13-which-tool-should-i-use)
14. [How this maps to our trail](#14-how-this-maps-to-our-trail)
15. [`java.lang` thread APIs (foundation)](#15-javalang-thread-apis-foundation)
16. [Virtual threads (Java 21+)](#16-virtual-threads-java-21)
17. [Structured concurrency & ScopedValue](#17-structured-concurrency--scopedvalue)
18. [Package `java.util.concurrent.locks`](#18-package-javautilconcurrentlocks)
19. [Package `java.util.concurrent.atomic`](#19-package-javautilconcurrentatomic)
20. [Reactive Streams: `Flow` & `SubmissionPublisher`](#20-reactive-streams-flow--submissionpublisher)
21. [Related: `VarHandle`](#21-related-varhandle)

---

## 1. Why this package exists

Low-level APIs (`Thread`, `synchronized`, `wait`/`notify`) work, but they are:

- Easy to get wrong (deadlock, lost wakeups, visibility bugs)
- Hard to scale (one big lock, too many threads)
- Verbose for common patterns (pools, producer/consumer, “wait for N tasks”)

`java.util.concurrent` gives **standardized building blocks** for those patterns.

### Four pillars

| Pillar | Problem it solves |
|--------|-------------------|
| **Executors** | Don’t create a thread per task; manage lifecycle and results |
| **Queues** | Safe hand-off between producers and consumers |
| **Synchronizers** | “Wait until condition / everyone arrives / N permits” |
| **Concurrent collections** | Shared maps/lists without wrapping everything in `synchronized` |

---

## 2. Executors framework

### 2.1 Mental model

```
YOU                          POOL
───                          ────
submit(task) ──► queue ──► worker thread runs task
                │
                └── Future ←── result / exception / cancel
```

You describe **what** to run (`Runnable` / `Callable`).  
The executor decides **which thread** runs it (new, pooled, or caller).

### 2.2 Interface hierarchy

```
Executor
  └── execute(Runnable)
        │
        └── ExecutorService
              ├── submit → Future
              ├── invokeAll / invokeAny
              └── shutdown / shutdownNow
                    │
                    └── ScheduledExecutorService
                          ├── schedule
                          ├── scheduleAtFixedRate
                          └── scheduleWithFixedDelay
```

**Declare variables as interface types**, not concrete classes:

```java
ExecutorService pool = Executors.newFixedThreadPool(4);
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
```

### 2.3 `Executor` — simplest form

```java
Executor e = Executors.newFixedThreadPool(2);

// Old style:
// new Thread(r).start();

// New style:
e.execute(() -> System.out.println("on " + Thread.currentThread().getName()));
```

`execute` may:

- Reuse a **pool worker**
- **Queue** the task until a worker is free
- Sometimes run on a new thread (depends on implementation)

It does **not** return a result.

### 2.4 `ExecutorService` — results + lifecycle

```java
ExecutorService pool = Executors.newFixedThreadPool(2);

// Runnable — no return value
Future<?> f1 = pool.submit(() -> System.out.println("hi"));

// Callable — returns a value
Future<Integer> f2 = pool.submit(() -> {
    Thread.sleep(100);
    return 42;
});

Integer answer = f2.get(); // blocks until done
System.out.println(answer); // 42

pool.shutdown();                 // no new tasks
pool.awaitTermination(5, TimeUnit.SECONDS);
```

| Method | Meaning |
|--------|---------|
| `submit(Runnable)` | Returns `Future<?>` (`get()` → `null`) |
| `submit(Callable<T>)` | Returns `Future<T>` with result |
| `invokeAll(collection)` | Run many Callables; wait for all |
| `invokeAny(collection)` | Return first successful result |
| `shutdown()` | Finish queued tasks; reject new ones |
| `shutdownNow()` | Try to cancel running; interrupt workers |

**Always shut down pools** in apps, or the JVM may not exit (non-daemon workers).

### 2.5 Factory methods (`Executors`)

| Factory | Behavior | Good for |
|---------|----------|----------|
| `newFixedThreadPool(n)` | Exactly `n` workers; extra tasks queue | Steady load, web-like request handling |
| `newCachedThreadPool()` | Creates threads as needed; reuses idle | Many short-lived tasks |
| `newSingleThreadExecutor()` | One worker; tasks run in order | Ordered side effects |
| `newScheduledThreadPool(n)` | Delayed / periodic | Timers, heartbeats |
| `newWorkStealingPool()` | ForkJoin-style pool | Parallel compute (Java 8+) |

**Graceful degradation (fixed pool):**  
If you spawn a new `Thread` per HTTP request, a traffic spike can exhaust memory.  
A fixed pool **queues** work — the app slows down but stays alive.

### 2.6 `ThreadPoolExecutor` — full control

When factories are not enough:

```java
ThreadPoolExecutor pool = new ThreadPoolExecutor(
    2,                          // corePoolSize
    4,                          // maximumPoolSize
    60L, TimeUnit.SECONDS,      // keepAlive for idle extra threads
    new ArrayBlockingQueue<>(100), // work queue
    Executors.defaultThreadFactory(),
    new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
);
```

**Rejection policies** (queue full + max threads busy):

| Policy | Behavior |
|--------|----------|
| `AbortPolicy` (default) | Throw `RejectedExecutionException` |
| `CallerRunsPolicy` | Run task on the **calling** thread (backpressure) |
| `DiscardPolicy` | Silently drop the new task |
| `DiscardOldestPolicy` | Drop oldest queued task, retry |

### 2.7 Scheduled execution

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

// Once after delay
scheduler.schedule(() -> System.out.println("later"), 500, TimeUnit.MILLISECONDS);

// Fixed rate: period measured from start to start
scheduler.scheduleAtFixedRate(() -> System.out.println("tick"),
        0, 200, TimeUnit.MILLISECONDS);

// Fixed delay: wait after each finish, then run again
scheduler.scheduleWithFixedDelay(() -> System.out.println("tock"),
        0, 200, TimeUnit.MILLISECONDS);

Thread.sleep(1000);
scheduler.shutdown();
```

| Method | Timing |
|--------|--------|
| `schedule` | Once after delay |
| `scheduleAtFixedRate` | Try to keep a steady period |
| `scheduleWithFixedDelay` | Delay **after** previous completion |

### 2.8 `CompletionService` — take results as they finish

```java
ExecutorService pool = Executors.newFixedThreadPool(4);
CompletionService<Integer> cs = new ExecutorCompletionService<>(pool);

for (int i = 1; i <= 4; i++) {
    final int n = i;
    cs.submit(() -> {
        Thread.sleep(100 * (5 - n)); // slower first tasks finish later
        return n * n;
    });
}

for (int i = 0; i < 4; i++) {
    Future<Integer> done = cs.take(); // blocks until NEXT completion
    System.out.println("finished: " + done.get());
}
pool.shutdown();
```

Useful when you want to process results **in completion order**, not submission order.

---

## 3. Futures and async results

### 3.1 `Callable` vs `Runnable`

```java
Runnable r = () -> System.out.println("no return");

Callable<String> c = () -> {
    if (Math.random() < 0) throw new Exception("boom");
    return "ok";
};
```

| | `Runnable` | `Callable<V>` |
|--|------------|---------------|
| Return | void | `V` |
| Checked exceptions | Cannot throw | May throw |
| Typical submit | `execute` / `submit` | `submit` |

### 3.2 `Future` API

```java
Future<Integer> f = pool.submit(() -> 10 + 32);

f.isDone();          // finished?
f.isCancelled();
f.cancel(true);      // true = interrupt if running
Integer v = f.get(); // wait forever
Integer v2 = f.get(1, TimeUnit.SECONDS); // wait with timeout
```

**What `get()` does:**

1. Waits until the task finishes  
2. Returns the value  
3. Or throws:
   - `CancellationException` — cancelled  
   - `ExecutionException` — task threw (cause inside)  
   - `TimeoutException` — timed out  
   - `InterruptedException` — waiting thread interrupted  

```java
try {
    return f.get();
} catch (ExecutionException e) {
    Throwable cause = e.getCause(); // real error from the task
    throw new RuntimeException(cause);
}
```

### 3.3 Happens-before with Future

Everything the task wrote **before finishing** is visible to the thread after a successful `Future.get()`.

```java
// Worker
shared = 99;
return shared;

// Main after get()
System.out.println(shared); // guaranteed to see 99 (and task's other writes)
```

### 3.4 `FutureTask`

Concrete class implementing `RunnableFuture` (both `Runnable` and `Future`):

```java
FutureTask<Integer> task = new FutureTask<>(() -> 7 * 6);
new Thread(task).start();
System.out.println(task.get()); // 42
```

---

## 4. CompletableFuture

A `Future` you can **complete manually**, and chain like a pipeline (`CompletionStage`).

### 4.1 Basic usage

```java
CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
    // runs on ForkJoinPool.commonPool() by default
    return "hello";
});

String upper = cf.thenApply(String::toUpperCase)  // transform
                 .thenApply(s -> s + "!")
                 .get();
System.out.println(upper); // HELLO!
```

### 4.2 Common chaining

| Method | Meaning |
|--------|---------|
| `thenApply(fn)` | Transform result (sync) |
| `thenAccept(consumer)` | Consume result (no return) |
| `thenRun(runnable)` | Run after completion |
| `thenCompose(fn)` | Flat-map another CompletionStage |
| `thenCombine(other, fn)` | Wait for two stages; combine |
| `exceptionally(fn)` | Recover from error |
| `handle((v, ex) -> …)` | Handle success or failure |
| `allOf(...)` | Wait for all |
| `anyOf(...)` | Wait for first |

```java
CompletableFuture<Integer> a = CompletableFuture.supplyAsync(() -> 2);
CompletableFuture<Integer> b = CompletableFuture.supplyAsync(() -> 3);

int sum = a.thenCombine(b, Integer::sum).get(); // 5

CompletableFuture.allOf(a, b).join(); // wait both
```

### 4.3 Explicit completion

```java
CompletableFuture<String> cf = new CompletableFuture<>();

new Thread(() -> {
    try {
        Thread.sleep(100);
        cf.complete("ready");
    } catch (Exception e) {
        cf.completeExceptionally(e);
    }
}).start();

System.out.println(cf.get()); // ready
```

---

## 5. Fork/Join

Designed for **recursive divide-and-conquer** on multi-core CPUs.

### 5.1 Work-stealing

```
Worker A queue: [task1, task2, task3]
Worker B idle  → steals task from A's queue
```

Idle workers steal from busy ones → better CPU utilization than a single shared queue for fine-grained tasks.

### 5.2 Pattern

```
if (work is small enough)
    compute directly
else
    split into two
    fork/join both
```

| Class | Returns value? |
|-------|----------------|
| `RecursiveAction` | No |
| `RecursiveTask<V>` | Yes |

### 5.3 Example — parallel sum (`RecursiveTask`)

```java
class SumTask extends RecursiveTask<Long> {
    static final int THRESHOLD = 10_000;
    final long[] data;
    final int start, end;

    SumTask(long[] data, int start, int end) {
        this.data = data; this.start = start; this.end = end;
    }

    protected Long compute() {
        if (end - start <= THRESHOLD) {
            long sum = 0;
            for (int i = start; i < end; i++) sum += data[i];
            return sum;
        }
        int mid = (start + end) >>> 1;
        SumTask left = new SumTask(data, start, mid);
        SumTask right = new SumTask(data, mid, end);
        left.fork();                    // async
        long rightResult = right.compute();
        long leftResult = left.join();  // wait
        return leftResult + rightResult;
    }
}

// use:
ForkJoinPool pool = new ForkJoinPool();
long sum = pool.invoke(new SumTask(array, 0, array.length));
```

### 5.4 Built into the JDK

- `Arrays.parallelSort(...)`
- Parallel streams: `list.parallelStream()...`

---

## 6. Queues

### 6.1 Why special queues?

Producer/consumer with `wait`/`notify` is easy to get wrong.  
Concurrent queues encode the protocol:

```java
queue.put(item);   // wait if full (blocking queues)
item = queue.take(); // wait if empty
```

**Happens-before:** writes before `put` are visible after `take` in another thread.

### 6.2 Operation styles on `BlockingQueue`

| Method | Full queue | Empty queue |
|--------|------------|-------------|
| `add` / `remove` / `element` | throws | throws |
| `offer` / `poll` / `peek` | false / null | null |
| `put` / `take` | **blocks** | **blocks** |
| `offer(x, time)` / `poll(time)` | timeout | timeout |

### 6.3 Implementations

#### `ArrayBlockingQueue` — bounded, array

```java
BlockingQueue<String> q = new ArrayBlockingQueue<>(2);
q.put("A");
q.put("B");
// q.put("C"); // would block until space frees
```

Good when you want **backpressure** (producer slows if consumer is behind).

#### `LinkedBlockingQueue` — optionally bounded

```java
BlockingQueue<Runnable> q = new LinkedBlockingQueue<>(); // Integer.MAX_VALUE capacity
// or: new LinkedBlockingQueue<>(1000);
```

Common as a pool work queue.

#### `SynchronousQueue` — capacity 0

```java
BlockingQueue<String> handoff = new SynchronousQueue<>();
// put() waits until another thread take()s — direct hand-off
```

Used by `Executors.newCachedThreadPool()` internally for task hand-off.

#### `PriorityBlockingQueue` — priority order

```java
BlockingQueue<Integer> pq = new PriorityBlockingQueue<>();
pq.offer(30);
pq.offer(10);
pq.offer(20);
System.out.println(pq.take()); // 10 (natural order)
```

#### `DelayQueue` — available only after delay

```java
class DelayedEvent implements Delayed {
    final long readyAtMs;
    final String name;
    DelayedEvent(String name, long delayMs) {
        this.name = name;
        this.readyAtMs = System.currentTimeMillis() + delayMs;
    }
    public long getDelay(TimeUnit unit) {
        return unit.convert(readyAtMs - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }
    public int compareTo(Delayed o) {
        return Long.compare(getDelay(TimeUnit.MILLISECONDS),
                            o.getDelay(TimeUnit.MILLISECONDS));
    }
}

DelayQueue<DelayedEvent> dq = new DelayQueue<>();
dq.put(new DelayedEvent("fire", 500));
DelayedEvent e = dq.take(); // waits until delay expires
```

#### `ConcurrentLinkedQueue` — non-blocking FIFO

```java
Queue<String> q = new ConcurrentLinkedQueue<>();
q.offer("x");
String v = q.poll(); // null if empty — never blocks
```

Use when you prefer non-blocking polls (e.g. event buffers) over blocking waits.

#### `TransferQueue` / `LinkedTransferQueue`

```java
TransferQueue<String> tq = new LinkedTransferQueue<>();
// transfer: wait until a consumer takes the element
tq.transfer("must-be-received");
```

Stronger than `put`: producer can wait for **actual consumption**.

#### `BlockingDeque` / `LinkedBlockingDeque`

Queue + stack style: add/remove from **both ends**, with blocking.

```java
BlockingDeque<String> dq = new LinkedBlockingDeque<>();
dq.putFirst("front");
dq.putLast("back");
System.out.println(dq.takeFirst()); // front
```

### 6.4 Mini producer–consumer

```java
BlockingQueue<String> q = new ArrayBlockingQueue<>(10);

Thread producer = new Thread(() -> {
    try {
        for (String m : List.of("a", "b", "c", "DONE")) {
            q.put(m);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});

Thread consumer = new Thread(() -> {
    try {
        String m;
        while (!(m = q.take()).equals("DONE")) {
            System.out.println("got " + m);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});

producer.start();
consumer.start();
```

---

## 7. Synchronizers

Special-purpose coordination tools. Prefer these over inventing wait/notify protocols.

### 7.1 `CountDownLatch` — wait for N events (one-shot)

```
workers each: do work → countDown()
main:         await() until count == 0
```

```java
int n = 3;
CountDownLatch latch = new CountDownLatch(n);

for (int i = 0; i < n; i++) {
    final int id = i;
    new Thread(() -> {
        System.out.println("worker " + id);
        latch.countDown();
    }).start();
}

latch.await(); // main waits here
System.out.println("all done");
```

| Trait | Detail |
|-------|--------|
| Reusable? | **No** — count only goes down |
| Typical use | “Start gate” or “wait for N services to init” |

Start-gate pattern (all workers wait, then start together):

```java
CountDownLatch start = new CountDownLatch(1);
CountDownLatch done = new CountDownLatch(workers);

for (...) {
    new Thread(() -> {
        start.await();   // wait for main's "go"
        // work...
        done.countDown();
    }).start();
}
start.countDown(); // go!
done.await();
```

### 7.2 `CyclicBarrier` — all threads rendezvous (reusable)

```
each thread: await()
when all N arrived → optional barrier action → all continue
```

```java
int parties = 3;
CyclicBarrier barrier = new CyclicBarrier(parties,
        () -> System.out.println("all arrived — barrier action"));

ExecutorService pool = Executors.newFixedThreadPool(parties);
for (int i = 0; i < parties; i++) {
    final int id = i;
    pool.submit(() -> {
        System.out.println(id + " waiting");
        barrier.await();
        System.out.println(id + " passed");
    });
}
```

| | `CountDownLatch` | `CyclicBarrier` |
|--|------------------|-----------------|
| Who waits | Often one waiter | All parties wait for each other |
| Reuse | No | **Yes** (`reset` / reuse) |
| Action | — | Optional runnable when all arrive |

If one party is interrupted / times out, barrier can **break** → `BrokenBarrierException`.

### 7.3 `Phaser` — flexible multi-phase barrier

Like a barrier that supports:

- Dynamic registration of parties  
- Multiple phases (phase 0, 1, 2, …)

```java
Phaser phaser = new Phaser(1); // register self (main)

for (int i = 0; i < 3; i++) {
    phaser.register();
    new Thread(() -> {
        System.out.println(Thread.currentThread().getName() + " phase " + phaser.getPhase());
        phaser.arriveAndAwaitAdvance(); // arrive + wait for others
        System.out.println(Thread.currentThread().getName() + " next");
        phaser.arriveAndDeregister();
    }).start();
}

phaser.arriveAndDeregister(); // main drops out
```

Use when party count changes over time, or you need repeated phases.

### 7.4 `Semaphore` — limit concurrent access

```
permits = N
acquire() → take a permit (block if 0)
release() → return a permit
```

```java
Semaphore sem = new Semaphore(2); // max 2 concurrent

Runnable task = () -> {
    try {
        sem.acquire();
        try {
            // critical limited section (e.g. max 2 DB connections)
            Thread.sleep(100);
        } finally {
            sem.release();
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
};
```

Also: `tryAcquire()`, `tryAcquire(timeout)`, fair vs non-fair constructor.

**Happens-before:** `release` happens-before successful `acquire` on the same semaphore.

### 7.5 `Exchanger` — two threads swap

```java
Exchanger<String> ex = new Exchanger<>();

new Thread(() -> {
    try {
        String got = ex.exchange("from-A");
        System.out.println("A got " + got);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}).start();

new Thread(() -> {
    try {
        String got = ex.exchange("from-B");
        System.out.println("B got " + got);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}).start();
// A got from-B ; B got from-A
```

Useful in pipeline designs (fill buffer / empty buffer swap).

### 7.6 Synchronizer cheat sheet

| Need | Tool |
|------|------|
| Wait until N tasks finish (once) | `CountDownLatch` |
| N threads must meet, then continue (repeatable) | `CyclicBarrier` |
| Dynamic parties / multiple phases | `Phaser` |
| At most N threads in a section | `Semaphore` |
| Two threads swap data | `Exchanger` |

---

## 8. Concurrent collections

### 8.1 Concurrent vs synchronized collections

```java
// Synchronized wrapper — ONE lock for whole map
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());

// Concurrent — designed for concurrent readers/writers
ConcurrentMap<String, Integer> cmap = new ConcurrentHashMap<>();
```

| | Synchronized map | `ConcurrentHashMap` |
|--|------------------|---------------------|
| Locking | Single lock | Fine-grained / concurrent algorithms |
| Scalability | Poor under many threads | Much better |
| Compound atomic ops | You lock manually | `putIfAbsent`, `replace`, `compute`, `merge` |

**Rule:** If many threads share a map, prefer `ConcurrentHashMap` over `Collections.synchronizedMap`.

### 8.2 `ConcurrentHashMap` / `ConcurrentMap`

```java
ConcurrentMap<String, Integer> scores = new ConcurrentHashMap<>();

scores.put("alice", 1);

// Atomic: add only if absent
scores.putIfAbsent("bob", 5);

// Atomic: replace only if still old value
scores.replace("bob", 5, 50);

// Atomic: remove only if value matches
scores.remove("bob", 50);

// Atomic update (Java 8+)
scores.merge("alice", 1, Integer::sum);
scores.compute("alice", (k, v) -> v == null ? 1 : v + 1);
```

**Never do this race:**

```java
// BAD on any shared map without sync
if (!map.containsKey(k)) {
    map.put(k, v); // another thread may have put in between
}
// GOOD:
map.putIfAbsent(k, v);
```

### 8.3 `ConcurrentSkipListMap` / `ConcurrentNavigableMap`

Concurrent sorted map (like `TreeMap`):

```java
ConcurrentNavigableMap<Integer, String> ages = new ConcurrentSkipListMap<>();
ages.put(18, "teen");
ages.put(25, "young");
ages.put(40, "mid");
ages.put(65, "senior");

ages.floorKey(30);    // 25  (≤ 30)
ages.ceilingKey(30);  // 40  (≥ 30)
ages.subMap(20, 50);  // {25=young, 40=mid}
ages.headMap(40);     // keys < 40
```

Also: `ConcurrentSkipListSet` for a concurrent sorted set.

### 8.4 `CopyOnWriteArrayList` / `CopyOnWriteArraySet`

Every mutation **copies** the underlying array.

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("a");
list.add("b");

// Readers never need external sync; iterators are snapshots
for (String s : list) {
    System.out.println(s);
}
```

| Prefer COW when | Avoid COW when |
|-----------------|----------------|
| Reads/traversals ≫ writes | Frequent adds/removes |
| Listeners, config lists | Large lists mutated often (copy cost) |

### 8.5 Weakly consistent iterators

Most concurrent collections’ iterators:

- Do **not** throw `ConcurrentModificationException`
- May run while other threads update
- Traverse elements that existed around construction; later changes **may or may not** appear

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("a", 1);
for (String k : map.keySet()) {
    map.put("b", 2); // allowed — no CME; "b" may or may not be seen in this loop
}
```

---

## 9. ThreadLocalRandom

For random numbers from **many threads** (or fork/join workers):

```java
int r = ThreadLocalRandom.current().nextInt(4, 77); // [4, 77)
long x = ThreadLocalRandom.current().nextLong();
double d = ThreadLocalRandom.current().nextDouble(0.0, 1.0);
```

| Approach | Concurrent use |
|----------|----------------|
| Shared `Random` / `Math.random()` | Contention (and `Random` needs care) |
| `ThreadLocalRandom` | Per-thread RNG — less contention |

**Not** for cryptography → use `SecureRandom`.

---

## 10. TimeUnit and timeouts

```java
TimeUnit.SECONDS.sleep(1);
lock.tryLock(200, TimeUnit.MILLISECONDS);
future.get(1, TimeUnit.SECONDS);
queue.poll(100, TimeUnit.MILLISECONDS);
```

Rules from the package docs:

| Rule | Meaning |
|------|---------|
| Timeout value | **Minimum** time to wait before reporting timeout |
| `≤ 0` | Do not wait |
| Detection lag | Timeout detected ASAP, but thread may resume later |
| “Forever” | Often `Long.MAX_VALUE` |

Common units: `NANOSECONDS`, `MICROSECONDS`, `MILLISECONDS`, `SECONDS`, `MINUTES`, `HOURS`, `DAYS`.

---

## 11. Exceptions

| Exception | When |
|-----------|------|
| `RejectedExecutionException` | Executor won’t accept a task (shutdown / saturated + AbortPolicy) |
| `ExecutionException` | Task failed; unwrap with `getCause()` after `Future.get()` |
| `CancellationException` | `Future` cancelled before completion |
| `TimeoutException` | Timed `get` / `await` / `tryLock` expired |
| `BrokenBarrierException` | Barrier entered broken state |
| `CompletionException` | Failure while completing a `CompletableFuture` stage |
| `InterruptedException` | Blocking wait interrupted (restore interrupt flag if you catch) |

```java
} catch (InterruptedException e) {
    Thread.currentThread().interrupt(); // restore status
}
```

---

## 12. Memory consistency (happens-before)

### 12.1 JLS basics (still apply)

| Edge | Guarantee |
|------|-----------|
| Program order in one thread | Earlier actions HB later ones |
| Unlock monitor → later lock same monitor | Visibility across threads |
| Write volatile → later read same volatile | Visibility |
| `Thread.start()` | Starter’s prior actions visible to new thread |
| `Thread.join()` returns | Finished thread’s actions visible to joiner |

### 12.2 Extra guarantees from this package

| Before | Happens-before | After (other thread) |
|--------|----------------|----------------------|
| Writes before putting into a concurrent collection | → | Access/remove that element |
| Submit `Runnable`/`Callable` to an Executor | → | Task execution begins |
| Actions of an async computation | → | Successful `Future.get()` |
| `Lock.unlock`, `Semaphore.release`, `CountDownLatch.countDown` | → | Matching acquire / await |
| Before `Exchanger.exchange` | → | After peer’s corresponding exchange |
| Before `CyclicBarrier`/`Phaser` await | → | Barrier action, then after await in others |

### 12.3 Practical meaning

If you pass data **only** through these APIs (queue put/take, future get, latch await, etc.), you usually get **visibility for free**.

You still need locks/atomics for **compound mutations** of your own shared fields that aren’t handed off through these tools.

---

## 13. Which tool should I use?

| I need… | Use |
|---------|-----|
| Run many tasks without managing threads | `ExecutorService` / `Executors.newFixedThreadPool` |
| Millions of blocking/IO tasks cheaply | **Virtual threads** / `newVirtualThreadPerTaskExecutor` |
| Fan-out then join related tasks as one unit | `StructuredTaskScope` (modern JDK) |
| Get a result from async work | `Future` or `CompletableFuture` |
| Chain async steps | `CompletableFuture` |
| Process results in finish order | `CompletionService` |
| Delayed / periodic jobs | `ScheduledExecutorService` |
| Recursive parallel algorithm | `ForkJoinPool` + `RecursiveTask`/`Action` |
| Producer → consumer | `BlockingQueue` (`ArrayBlockingQueue` if bounded) |
| Reactive stream with backpressure | `Flow` + `SubmissionPublisher` |
| Direct hand-off only | `SynchronousQueue` |
| Wait until N workers finish | `CountDownLatch` |
| All threads sync at a point | `CyclicBarrier` / `Phaser` |
| Limit concurrency to N | `Semaphore` |
| Two threads swap buffers | `Exchanger` |
| Shared map | `ConcurrentHashMap` |
| Shared sorted map | `ConcurrentSkipListMap` |
| Read-heavy list | `CopyOnWriteArrayList` |
| Random numbers from many threads | `ThreadLocalRandom` |
| Lock with try/timeout | `ReentrantLock` |
| Many readers, few writers | `ReentrantReadWriteLock` / `StampedLock` |
| Lock-free counter | `AtomicInteger` / `LongAdder` (high contention) |
| Per-thread context | `ThreadLocal` or `ScopedValue` (preferred with VTs) |

---

## 14. How this maps to our trail

| Folder / topic in this repo | Package piece |
|-----------------------------|---------------|
| `01`–`12` Oracle basics | `java.lang.Thread`, `Runnable`, sync, wait/notify |
| `14-lock-objects` | `ReentrantLock`, `Lock` |
| `15-executor-interfaces` | `Executor`, `ExecutorService`, `ScheduledExecutorService` |
| `16-thread-pools` | `Executors`, `ThreadPoolExecutor` ideas |
| `17-fork-join` | `ForkJoinPool`, `RecursiveAction`/`Task` |
| `18-concurrent-collections` | `BlockingQueue`, `ConcurrentHashMap`, `ConcurrentSkipListMap` |
| `19-atomic-variables` | `java.util.concurrent.atomic` |
| `20-thread-local-random` | `ThreadLocalRandom` |
| `21-concurrent-package-tour` | Latch, Barrier, Semaphore, CompletableFuture |
| `22-virtual-threads` | Virtual threads + modern APIs (added) |

---

## 15. `java.lang` thread APIs (foundation)

These live **outside** `java.util.concurrent` but every concurrency story starts here.

### Core types

| Type | Kind | Role |
|------|------|------|
| `Runnable` | interface | Work unit with `void run()` |
| `Thread` | class | Thread of execution; implements `Runnable` |
| `Thread.Builder` | interface | Fluent create platform/virtual threads (Java 21+) |
| `Thread.Builder.OfPlatform` | interface | Builder for OS/platform threads |
| `Thread.Builder.OfVirtual` | interface | Builder for virtual threads |
| `Thread.UncaughtExceptionHandler` | interface | Called when a thread dies from an uncaught exception |
| `Thread.State` | enum | `NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`, `TERMINATED` |
| `ThreadGroup` | class | Group threads (legacy; rarely needed in modern code) |
| `ThreadLocal<T>` | class | Per-thread variable storage |
| `InheritableThreadLocal<T>` | class | Child threads inherit parent’s value |
| `Process` / `ProcessBuilder` | class | OS processes (not JVM threads, but related concurrency) |
| `Object.wait` / `notify` / `notifyAll` | methods | Intrinsic monitor wait/notify |

### `Thread` essentials

```java
// Classic
Thread t = new Thread(() -> System.out.println("hi"), "worker");
t.start();          // schedules run() on new thread
t.join();           // wait for finish
t.interrupt();      // cooperative cancel signal
Thread.sleep(100);
Thread.currentThread();
t.getState();       // Thread.State
t.setDaemon(true);  // don't keep JVM alive (set BEFORE start)
```

Important methods: `start`, `run`, `join`, `interrupt`, `isInterrupted`, `interrupted`, `sleep`, `yield`, `setPriority`, `getId` / `threadId()`, `setUncaughtExceptionHandler`.

```java
Thread.setDefaultUncaughtExceptionHandler((th, ex) ->
    System.err.println(th.getName() + " crashed: " + ex));
```

### `ThreadLocal`

```java
static final ThreadLocal<String> USER = new ThreadLocal<>();

USER.set("alice");           // only this thread sees it
System.out.println(USER.get());
USER.remove();               // always clean up (esp. with pools!)
```

With **thread pools**, forgetting `remove()` leaks values across tasks. Prefer **`ScopedValue`** (below) with virtual threads when possible.

---

## 16. Virtual threads (Java 21+)

Sources: [Oracle Virtual Threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html), [Thread API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html).

**Virtual threads** are lightweight threads scheduled by the JVM (not 1:1 with OS threads). One JVM can run **millions**. Best for **blocking I/O-bound** work, not long CPU burns.

### Create & start

```java
// 1) One-liner
Thread.startVirtualThread(() -> System.out.println("VT " + Thread.currentThread()));

// 2) Builder
Thread vt = Thread.ofVirtual().name("worker-", 0).start(() -> {
    System.out.println(Thread.currentThread().isVirtual()); // true
});
vt.join();

// 3) Executor — new virtual thread PER task
try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<Integer> f = exec.submit(() -> 42);
    System.out.println(f.get());
} // AutoCloseable — shuts down

// Platform (classic) builder still available:
Thread.ofPlatform().name("heavy").start(() -> { /* CPU work */ });
```

### Facts

| Topic | Detail |
|-------|--------|
| Daemon | Virtual threads are daemon-like (don’t block JVM exit by themselves in the old sense — still join/await work you care about) |
| Priority | Fixed; cannot meaningfully change |
| Carrier | VT mounts on a small pool of platform **carrier** threads |
| Pinning | Holding a `synchronized` monitor during blocking can **pin** the carrier (costly); prefer `ReentrantLock` for long blocks |
| Pools | Usually **don’t** pool VTs — create one per task |

Demo in repo: `22-virtual-threads/VirtualThreadsDemo.java`.

---

## 17. Structured concurrency & ScopedValue

Available on modern JDKs (your environment: **Java 25** — both present). Treat as the preferred successor pattern to “fire many futures and hope.”

### `StructuredTaskScope`

Fork related subtasks; their lifetime is bound to a **scope** (try-with-resources).

```java
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;

try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Subtask<String> user  = scope.fork(() -> fetchUser());
    Subtask<String> order = scope.fork(() -> fetchOrder());

    scope.join();            // wait for both
    scope.throwIfFailed();   // fail fast if any failed

    return user.get() + " / " + order.get();
} // cancels leftover work on close
```

| Policy class | Behavior |
|--------------|----------|
| `ShutdownOnFailure` | If one fails → cancel others |
| `ShutdownOnSuccess` | If one succeeds → cancel others (race for first result) |

Interface: `StructuredTaskScope.Subtask` — state + `get()` after join.

### `ScopedValue` (`java.lang`)

Safer alternative to `ThreadLocal` for immutable context (request id, user):

```java
static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

ScopedValue.where(REQUEST_ID, "req-42").run(() -> {
    System.out.println(REQUEST_ID.get()); // visible in this dynamic scope
    // also visible to virtual threads forked in structured scopes (when bound)
});
```

---

## 18. Package `java.util.concurrent.locks`

Distinct from `synchronized` monitors — more flexible, more verbose.

### Interfaces & classes

| Type | Role |
|------|------|
| `Lock` | Explicit lock: `lock`, `unlock`, `tryLock`, `lockInterruptibly` |
| `Condition` | Wait-sets for a `Lock` (like wait/notify, but multiple conditions per lock) |
| `ReadWriteLock` | Pair: shared read lock + exclusive write lock |
| `ReentrantLock` | Main `Lock` impl (reentrant, optional fairness) |
| `ReentrantReadWriteLock` | Standard RW lock (+ nested `ReadLock` / `WriteLock`) |
| `StampedLock` | Optimistic read + read/write modes (advanced) |
| `LockSupport` | Low-level park/unpark for custom synchronizers |
| `AbstractQueuedSynchronizer` (AQS) | Framework for building locks/latches/semaphores |
| `AbstractQueuedLongSynchronizer` | AQS with `long` state |
| `AbstractOwnableSynchronizer` | Tracks exclusive owner thread |

### Examples

```java
Lock lock = new ReentrantLock();
lock.lock();
try {
    // critical section
} finally {
    lock.unlock();
}

if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
    try { /* ... */ } finally { lock.unlock(); }
}

// Condition (multiple wait-sets)
ReentrantLock rl = new ReentrantLock();
Condition notEmpty = rl.newCondition();
Condition notFull  = rl.newCondition();
// await / signal / signalAll instead of wait / notify
```

```java
ReadWriteLock rw = new ReentrantReadWriteLock();
rw.readLock().lock();
try { /* many readers OK */ }
finally { rw.readLock().unlock(); }

rw.writeLock().lock();
try { /* exclusive writer */ }
finally { rw.writeLock().unlock(); }
```

```java
StampedLock sl = new StampedLock();
long stamp = sl.tryOptimisticRead();
// read fields...
if (!sl.validate(stamp)) {
    stamp = sl.readLock();
    try { /* re-read under real read lock */ }
    finally { sl.unlockRead(stamp); }
}
```

---

## 19. Package `java.util.concurrent.atomic`

Lock-free updates on single variables / arrays / fields. `get`/`set` ≈ volatile; plus CAS and arithmetic.

### Classes

| Class | Role |
|-------|------|
| `AtomicBoolean` | Atomic `boolean` |
| `AtomicInteger` | Atomic `int` (+ increment, add, …) |
| `AtomicLong` | Atomic `long` |
| `AtomicReference<V>` | Atomic object reference |
| `AtomicIntegerArray` | Atomic `int[]` elements |
| `AtomicLongArray` | Atomic `long[]` elements |
| `AtomicReferenceArray<E>` | Atomic reference array |
| `AtomicIntegerFieldUpdater` | CAS on a `volatile int` field (reflection; legacy vs VarHandle) |
| `AtomicLongFieldUpdater` | Same for `volatile long` |
| `AtomicReferenceFieldUpdater` | Same for `volatile` reference field |
| `AtomicMarkableReference<V>` | Reference + boolean mark (e.g. logical delete) |
| `AtomicStampedReference<V>` | Reference + int stamp (version / ABA defense) |
| `LongAdder` | High-contention sum (better than `AtomicLong` under heavy updates) |
| `DoubleAdder` | Same for `double` |
| `LongAccumulator` | Custom long accumulate function |
| `DoubleAccumulator` | Custom double accumulate function |

### Examples

```java
AtomicInteger c = new AtomicInteger(0);
c.incrementAndGet();
c.compareAndSet(1, 10);

AtomicReference<String> ref = new AtomicReference<>("a");
ref.compareAndSet("a", "b");

LongAdder hits = new LongAdder();
hits.increment();          // many threads
long total = hits.sum();

AtomicStampedReference<Node> stamped = new AtomicStampedReference<>(node, 0);
int[] stampHolder = new int[1];
Node cur = stamped.get(stampHolder);
stamped.compareAndSet(cur, next, stampHolder[0], stampHolder[0] + 1);
```

---

## 20. Reactive Streams: `Flow` & `SubmissionPublisher`

Java 9+ (`java.util.concurrent.Flow`) — JDK’s Reactive Streams SPI with **backpressure**.

| Type | Role |
|------|------|
| `Flow.Publisher<T>` | Produces items to subscribers |
| `Flow.Subscriber<T>` | Consumes: `onSubscribe`, `onNext`, `onError`, `onComplete` |
| `Flow.Subscription` | `request(n)` / `cancel()` — backpressure control |
| `Flow.Processor<T,R>` | Both Subscriber and Publisher (transform stage) |
| `SubmissionPublisher<T>` | Ready-made `Publisher` implementation |

```java
try (SubmissionPublisher<String> pub = new SubmissionPublisher<>()) {
    pub.subscribe(new Flow.Subscriber<>() {
        private Flow.Subscription sub;
        public void onSubscribe(Flow.Subscription s) {
            sub = s;
            s.request(1); // ask for one item
        }
        public void onNext(String item) {
            System.out.println("got " + item);
            sub.request(1); // ask for next
        }
        public void onError(Throwable t) { t.printStackTrace(); }
        public void onComplete() { System.out.println("done"); }
    });
    pub.submit("A");
    pub.submit("B");
}
```

Use when you need async streams with flow control. For simple task results, prefer `CompletableFuture` / executors.

---

## 21. Related: `VarHandle`

`java.lang.invoke.VarHandle` (Java 9+) — low-level typed access to fields/arrays with explicit memory ordering (plain, opaque, acquire/release, volatile, CAS).

Powers many concurrent structures and largely replaces reflection-based `Atomic*FieldUpdater` for new code.

```java
// Conceptual — obtain via MethodHandles.lookup().findVarHandle(...)
// handle.compareAndSet(obj, expected, update);
// handle.setVolatile(obj, value);
```

You rarely need this unless writing custom concurrent data structures.

---

## Appendix — Full interface & class index

### `java.lang` (thread-related)

`Runnable`, `Thread`, `Thread.Builder`, `Thread.Builder.OfPlatform`, `Thread.Builder.OfVirtual`, `Thread.UncaughtExceptionHandler`, `Thread.State`, `ThreadGroup`, `ThreadLocal`, `InheritableThreadLocal`, `ScopedValue` (modern), `Object` wait/notify

### `java.util.concurrent` — interfaces

`BlockingDeque`, `BlockingQueue`, `Callable`, `CompletableFuture.AsynchronousCompletionTask`, `CompletionService`, `CompletionStage`, `ConcurrentMap`, `ConcurrentNavigableMap`, `Delayed`, `Executor`, `ExecutorService`, `Flow.Publisher`, `Flow.Subscriber`, `Flow.Subscription`, `Flow.Processor`, `ForkJoinPool.ForkJoinWorkerThreadFactory`, `ForkJoinPool.ManagedBlocker`, `Future`, `RejectedExecutionHandler`, `RunnableFuture`, `RunnableScheduledFuture`, `ScheduledExecutorService`, `ScheduledFuture`, `StructuredTaskScope.Subtask`, `ThreadFactory`, `TransferQueue`

### `java.util.concurrent` — key classes

`AbstractExecutorService`, `ArrayBlockingQueue`, `CompletableFuture`, `ConcurrentHashMap`, `ConcurrentLinkedDeque`, `ConcurrentLinkedQueue`, `ConcurrentSkipListMap`, `ConcurrentSkipListSet`, `CopyOnWriteArrayList`, `CopyOnWriteArraySet`, `CountDownLatch`, `CountedCompleter`, `CyclicBarrier`, `DelayQueue`, `Exchanger`, `ExecutorCompletionService`, `Executors`, `Flow`, `ForkJoinPool`, `ForkJoinTask`, `ForkJoinWorkerThread`, `FutureTask`, `LinkedBlockingDeque`, `LinkedBlockingQueue`, `LinkedTransferQueue`, `Phaser`, `PriorityBlockingQueue`, `RecursiveAction`, `RecursiveTask`, `ScheduledThreadPoolExecutor`, `Semaphore`, `StructuredTaskScope` (+ `ShutdownOnFailure` / `ShutdownOnSuccess`), `SubmissionPublisher`, `SynchronousQueue`, `ThreadLocalRandom`, `ThreadPoolExecutor` (+ rejection policies)

### `java.util.concurrent.locks`

`Lock`, `Condition`, `ReadWriteLock`, `ReentrantLock`, `ReentrantReadWriteLock`, `StampedLock`, `LockSupport`, `AbstractQueuedSynchronizer`, `AbstractQueuedLongSynchronizer`, `AbstractOwnableSynchronizer`

### `java.util.concurrent.atomic`

`AtomicBoolean`, `AtomicInteger`, `AtomicLong`, `AtomicReference`, `AtomicIntegerArray`, `AtomicLongArray`, `AtomicReferenceArray`, `AtomicIntegerFieldUpdater`, `AtomicLongFieldUpdater`, `AtomicReferenceFieldUpdater`, `AtomicMarkableReference`, `AtomicStampedReference`, `LongAdder`, `DoubleAdder`, `LongAccumulator`, `DoubleAccumulator`

### Enums / related

`TimeUnit`, `Thread.State`, `java.lang.invoke.VarHandle`

---

*Sources: [Oracle java.util.concurrent](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/package-summary.html), [locks](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/locks/package-summary.html), [atomic](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/atomic/package-summary.html), [Thread](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html), [Virtual Threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html), [Flow](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Flow.html).*

*Run demos with `make compile` then `java -cp target <ClassName>`.*
