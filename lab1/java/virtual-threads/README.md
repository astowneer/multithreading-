# Lab 1 - Java - virtual threads

Implementation of [Lab 1](../../README.md) in Java.

**Approach:** the same design as [manual-threads](../manual-threads), with one thread per part and
a synchronized total, but the threads are **virtual threads** (Project Loom, final in Java 21).
`ArraySum.parallelWithExecutor` shows the second common style, an
`Executors.newVirtualThreadPerTaskExecutor()`. `main` times both.

## How it works

```java
// style 1: a thread builder, the drop-in replacement for `new Thread(task, name)`
Thread.Builder builder = Thread.ofVirtual().name("sum-", 0);   // sum-0, sum-1, ...
Thread t = builder.unstarted(task);
t.start();
t.join();

// style 2: one new virtual thread per submitted task, no pool to size
try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
  Future<Long> f = ex.submit(() -> sumRange(arr, range));
}
```

The diff against manual-threads is a few lines in `runAll`. That is the point of virtual threads:
the same `Thread` API and the same blocking code style, at a much lower cost per thread.

## Platform threads vs virtual threads

| | Platform thread | Virtual thread |
|---|---|---|
| Backed by | one OS thread, for its whole life | a *carrier* (platform) thread, only while it runs |
| Stack | ~1 MB reserved up front | small, grows on the heap as needed |
| Cost to create | a system call, ~microseconds | an object allocation, ~nanoseconds |
| How many fit | thousands | millions |
| Scheduled by | the OS | the JVM, on a `ForkJoinPool` of ~CPU-count carriers |
| Pool them? | yes | **no**: create one per task |

When a virtual thread blocks on I/O, `sleep`, a lock or a queue, the JVM **unmounts** it from its
carrier, so the carrier can run another virtual thread. Its stack is copied back to a carrier
when it can continue. `tenThousandBlockingTasksFinishQuickly` in the tests shows this: 10,000
tasks that each sleep for 100 ms finish in about 100 ms in total.

## Do they make this lab faster? No

Summing an array is **CPU-bound**. It never blocks, so there is nothing to unmount, and the
virtual threads end up running on the same ~CPU-count carriers as a fixed platform pool. Expect
the same time as [manual-threads](../manual-threads) or [executor-service](../executor-service).

Virtual threads raise **throughput for blocking work**, such as a server handling 100,000
concurrent requests that mostly wait on databases and HTTP calls. They do not make a computation
faster. For CPU-bound work, the number of cores is the limit, and fork/join or parallel streams
remain the tools.

## Pitfalls

- **Pinning.** Up to JDK 23, a virtual thread that blocks *inside* a `synchronized` block cannot
  unmount: it *pins* its carrier. JDK 24 removed this for `synchronized` (JEP 491). Here the
  synchronized section only adds two numbers and never blocks, so it does not matter. In
  blocking code on JDK 21, prefer `ReentrantLock`.
- **Don't pool virtual threads.** To limit concurrency, for example to at most 10 database calls
  at once, use a `Semaphore`, not a fixed pool of virtual threads.
- **Watch `ThreadLocal`.** With millions of threads, per-thread caches multiply. *Scoped values*
  are the intended replacement.
- **Structured concurrency** (`StructuredTaskScope`) is the next step: it treats a group of
  virtual threads as one unit that fails and is cancelled together. It is still a preview API, so
  this project does not use it.

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
| `threads` | `4`             | number of parts, one virtual thread each   |

The default array is `int[1_000_000_000]`, about 4 GB, so it needs `-Xmx5g` or more. For a quick
run use a smaller size, e.g. `... .jar 100000000 8`.

The exit code is `0` if the parallel and sequential sums match, `1` if they differ or the array
does not fit in memory, and `2` for invalid arguments.

## Layout

```
src/main/java/com/astowner/multithreading/lab1/
  Main.java        command-line entry point and timing
  ArraySum.java    sequential sum, parallel sum on virtual threads (two variants), parallel fill
  ChunkRange.java  splits [0, size) into balanced parts (lengths differ by at most 1)
  SumTask.java     Runnable: sums one part into the shared total
  FillTask.java    Runnable: fills one part with random values
  SharedSum.java   synchronized accumulator for the partial sums
src/test/java/...  JUnit 5 tests
```
