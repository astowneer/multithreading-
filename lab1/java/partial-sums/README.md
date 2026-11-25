# Lab 1 - Java - partial sums (no shared state)

Implementation of [Lab 1](../../README.md) in Java.

**Approach:** explicit `Thread` objects, one per part of the array, like
[manual-threads](../manual-threads), but **with no shared mutable state at all**. Each task stores
its own partial sum in a field, and after `join()` the main thread adds the partial sums together.
There is no `synchronized`, lock or atomic.

## How it works

```java
List<SumTask> tasks = ...;      // one task per part, each with its own `result` field
runAll("sum", tasks);           // start every thread, then join() every thread
long total = 0;
for (SumTask task : tasks) {
  total += task.result();       // plain reads in main, after join
}
```

## Why it is correct without synchronization

A data race needs two threads accessing the same variable, at least one of them writing, with no
ordering between them. Here:

- Each `result` field is written by exactly **one** worker thread.
- It is read only by `main`, and only **after** `join()` on that worker.

The Java Memory Model says *all actions in a thread happen-before another thread successfully
returns from `join()` on it*. So `main` is guaranteed to see the final value of `result`, even
though the field is not `volatile`.

Without the `join()`, for example if `main` polled `result` in a loop, the read would be a data
race. `main` could see a stale `0` forever, because the JIT may hoist the read out of the loop.

## Why this is the preferred design

This is the **map-reduce** shape that the higher-level APIs use internally: each part is computed
independently (map), and the partial results are combined once at the end (reduce).
[executor-service](../executor-service), [fork-join](../fork-join) and
[parallel-streams](../parallel-streams) all work this way.

- No contention: threads never wait for each other.
- Nothing to get wrong with locks: there is no lock to forget or deadlock on.
- Deterministic combination order, which matters for floating-point sums.

The general rule: **don't share mutable state; if you must, synchronize it**. Avoiding sharing is
better than synchronizing well.

## A subtle performance trap: false sharing

A tempting variant stores the partial sums in one `long[] partials` and has worker `i` write
`partials[i] += arr[j]` **inside the loop**. It is correct, but neighbouring elements share a
64-byte cache line, so the cores keep invalidating each other's caches and the loop slows down a
lot. Accumulating in a local variable and writing the field once, as here, avoids it.

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
| `threads` | `4`             | number of worker threads   |

The default array is `int[1_000_000_000]`, about 4 GB, so it needs `-Xmx5g` or more. For a quick
run use a smaller size, e.g. `... .jar 100000000 8`.

The exit code is `0` if the parallel and sequential sums match, `1` if they differ or the array
does not fit in memory, and `2` for invalid arguments.

## Layout

```
src/main/java/com/astowner/multithreading/lab1/
  Main.java        command-line entry point and timing
  ArraySum.java    sequential sum, parallel sum, parallel random fill
  ChunkRange.java  splits [0, size) into balanced parts (lengths differ by at most 1)
  SumTask.java     Runnable: sums one part and keeps the result in its own field
  FillTask.java    Runnable: fills one part with random values
src/test/java/...  JUnit 5 tests
```
