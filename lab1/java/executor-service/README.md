# Lab 1 - Java - ExecutorService

Implementation of [Lab 1](../../README.md) in Java.

**Approach:** a fixed **thread pool** from `java.util.concurrent.ExecutorService`. Each part of the
array becomes a `Callable<Long>` that *returns* its partial sum. The pool runs the tasks, and the
results come back as `Future<Long>`s that the main thread adds up. No thread is created by hand,
and no state is shared.

## How it works

```java
try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
  List<Future<Long>> partials = pool.invokeAll(tasks);   // submit all, wait for all
  long total = 0;
  for (Future<Long> f : partials) {
    total += f.get();                                    // already done, so no waiting
  }
  return total;
}                                                        // close(): shut the pool down
```

### The pieces

| Type | Role |
|---|---|
| `Executor` / `ExecutorService` | separates *what* to run (tasks) from *how* to run it (threads). |
| `Executors.newFixedThreadPool(n)` | `n` reusable threads plus an unbounded task queue. |
| `Callable<V>` | a task that returns `V` and may throw. `Runnable` cannot return a value. |
| `Future<V>` | a handle to a result that may not exist yet: `get()`, `isDone()`, `cancel()`. |
| `invokeAll` | submits a batch and waits until every task is finished. |
| `invokeAny` | returns the first task to succeed and cancels the rest. |

### Other factory methods in `Executors`

- `newFixedThreadPool(n)`: a constant number of threads, which suits CPU-bound work like this.
- `newCachedThreadPool()`: creates threads on demand and reuses idle ones. It suits many short
  I/O tasks, but has no upper bound on threads.
- `newSingleThreadExecutor()`: runs tasks one at a time, in submission order.
- `newScheduledThreadPool(n)`: runs tasks with a delay or periodically.
- `newWorkStealingPool()`: a `ForkJoinPool` (see [fork-join](../fork-join)).
- `newVirtualThreadPerTaskExecutor()`: see [virtual-threads](../virtual-threads).

## Why a pool instead of `new Thread`

- **Threads are reused.** Creating an OS thread costs a system call and about 1 MB of stack. A
  pool pays that once, which matters when there are many tasks.
- **Concurrency is bounded.** You choose how many threads run, whatever the number of tasks.
- **Results and errors are returned.** A `Callable` returns its result through the `Future`. If it
  throws, `get()` rethrows the error wrapped in an `ExecutionException`. With a raw `Thread`, an
  exception in `run()` is only printed, and the caller never sees it.
- **Lifecycle is managed.** `shutdown()`, `awaitTermination()` and `close()` stop the pool cleanly.

## Pitfalls

- **Always shut the pool down.** A fixed pool's threads are not daemon threads, so the JVM will not
  exit while the pool is alive. Here `try`-with-resources calls `close()`.
- **`Future.get()` blocks.** Calling it right after `submit()`, inside a loop, makes the tasks run
  one at a time. Submit everything first and collect afterwards; `invokeAll` does both.
- **The default queue is unbounded.** Submitting millions of tasks faster than they run can exhaust
  memory. Production code builds a `ThreadPoolExecutor` with a bounded queue and a rejection
  policy.
- **Pool size for CPU-bound work:** about `Runtime.getRuntime().availableProcessors()` threads.
  More threads only add context switches.

## Compare with manual-threads

Everything except the technique is copied from [manual-threads](../manual-threads): splitting into
balanced parts (`ChunkRange`), the random fill, the command line and the tests. To see only what
this approach changes:

```sh
diff -ru ../manual-threads/src src
```

## Requirements

- JDK 21 or newer. Maven is not needed: the included Maven Wrapper (`mvnw`) downloads it.

## Build and test

```sh
./mvnw verify
```

## Run

```sh
java -Xmx5g -jar target/lab1-array-sum-1.0-SNAPSHOT.jar [size] [threads]
```

| Argument  | Default         | Meaning                    |
|-----------|-----------------|----------------------------|
| `size`    | `1_000_000_000` | number of array elements   |
| `threads` | `4`             | size of the thread pool   |

The default array is `int[1_000_000_000]`, about 4 GB, so it needs `-Xmx5g` or more. For a quick
run use a smaller size, e.g. `... .jar 100000000 8`.

The exit code is `0` if the parallel and sequential sums match, `1` if they differ or the array
does not fit in memory, and `2` for invalid arguments.

## Layout

```
src/main/java/com/astowner/multithreading/lab1/
  Main.java        command-line entry point and timing
  ArraySum.java    sequential sum (loop), parallel sum (thread pool), parallel random fill
  ChunkRange.java  splits [0, size) into balanced parts (lengths differ by at most 1)
  SumTask.java     Callable<Long>: sums one part and returns it
  FillTask.java    Runnable: fills one part with random values
src/test/java/...  JUnit 5 tests
```
