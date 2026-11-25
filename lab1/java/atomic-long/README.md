# Lab 1 - Java - AtomicLong

Implementation of [Lab 1](../../README.md) in Java.

**Approach:** explicit `Thread` objects, one per part of the array, exactly like
[manual-threads](../manual-threads). The only difference is how the partial sums are combined:
instead of a `synchronized` accumulator, every thread adds its partial sum to a shared
`java.util.concurrent.atomic.AtomicLong`.

## How it works

```java
AtomicLong total = new AtomicLong();
// in each worker thread, after summing its part locally:
total.addAndGet(localSum);
// in main, after join():
return total.get();
```

`addAndGet` is **lock-free**. Internally it is a compare-and-set (CAS) loop, a single CPU
instruction (`lock xadd` on x86, `ldadd` on ARM64):

1. read the current value `old`;
2. ask the CPU to write `old + delta` **only if** the value is still `old`;
3. if another thread changed it in between, go back to 1.

No thread is ever put to sleep, and there is no monitor to acquire, so there is no lock overhead and
no risk of deadlock.

## Why it is correct

- **Atomicity**: the read-add-write happens as one indivisible step, so no update is lost. With a
  plain `long total` and `total += localSum`, two threads could read the same old value and one
  addition would disappear (a *race condition*).
- **Visibility**: `AtomicLong` has `volatile` semantics. A write by one thread is visible to every
  thread that reads it afterwards. `Thread.join()` additionally gives a *happens-before* edge from
  each worker to `main`.

## Trade-offs

- Here each thread touches the atomic **once**, so the choice of accumulator barely affects the
  time: almost all of it is spent in the local loop. The difference between `synchronized`,
  `AtomicLong` and `LongAdder` would only show if threads updated the total very often.
- Under heavy contention (many threads updating the same atomic in a tight loop), CAS retries
  pile up and the cache line holding the value bounces between cores. See
  [long-adder](../long-adder) for the fix.
- An anti-pattern to avoid: calling `total.addAndGet(arr[i])` **for every element**. It is
  correct but turns a fast local loop into billions of contended atomic operations, which is
  slower than the sequential sum.

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
  SumTask.java     Runnable: sums one part and adds it to the shared AtomicLong
  FillTask.java    Runnable: fills one part with random values
src/test/java/...  JUnit 5 tests
```
