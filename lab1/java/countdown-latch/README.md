# Lab 1 - Java - CountDownLatch

Implementation of [Lab 1](../../README.md) in Java.

**Approach:** explicit threads, one per part, but `main` does **not** `join()` them. Instead, it
waits on a `java.util.concurrent.CountDownLatch` initialised to the number of parts. Each worker
stores its partial sum in its own slot of a `long[]` and calls `countDown()`. When the count
reaches zero, `await()` returns and `main` adds up the slots.

## How it works

```java
long[] partials = new long[parts];
CountDownLatch done = new CountDownLatch(parts);

// each worker:
try {
  partials[i] = sumOfMyPart();
} finally {
  done.countDown();               // always, even if the sum threw
}

// main:
done.await();                     // blocks until the count is 0
long total = Arrays.stream(partials).sum();
```

## Why it is correct

- **No races on `partials`:** each slot is written by exactly one worker.
- **Visibility:** the `CountDownLatch` contract says actions before `countDown()`
  *happen-before* actions after the matching `await()` returns. So `main` sees every slot,
  although `long[]` has no `volatile` elements. This is the same guarantee `join()` gives in
  [partial-sums](../partial-sums), but it comes from the latch.

## `join()` vs `CountDownLatch`

| | `Thread.join()` | `CountDownLatch` |
|---|---|---|
| Waits for | a specific thread to **terminate** | a count of **events** to happen |
| Works with pools | no: pool threads never terminate | yes |
| Several waiters | each joins each thread | any number of threads can `await()` |
| Timeout | `join(millis)` | `await(timeout, unit)` |
| Reusable | n/a | **no**: once at zero it stays open |

A latch is the natural way to wait for N tasks on an `ExecutorService` when you have no
`Future`s, or for N events that are not "a thread finished", such as "N services reported
ready".

The second classic use, a **start gate**, is a `CountDownLatch(1)` that all workers `await()` and
the main thread opens with a single `countDown()`. `CountDownLatchTest` shows both uses.

## Pitfalls

- **`countDown()` belongs in `finally`.** If a worker throws before counting down, `main` waits
  forever. `await(timeout, unit)` is a safety net.
- **A latch reports that work finished, not that it succeeded.** If a worker throws, its slot
  stays `0` and the sum is silently wrong. A `Future` would deliver the exception to the caller
  (see [executor-service](../executor-service)).
- **One-shot.** It cannot be reset. For a reusable barrier, see [cyclic-barrier](../cyclic-barrier)
  or [phaser](../phaser).

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
  ArraySum.java    sequential sum, parallel sum (waits on a CountDownLatch), parallel random fill
  ChunkRange.java  splits [0, size) into balanced parts (lengths differ by at most 1)
  SumTask.java     Runnable: sums one part, stores it in its slot, counts the latch down
  FillTask.java    Runnable: fills one part with random values
src/test/java/...  JUnit 5 tests
```
