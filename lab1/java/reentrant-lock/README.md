# Lab 1 - Java - ReentrantLock

Implementation of [Lab 1](../../README.md) in Java.

**Approach:** explicit `Thread` objects, one per part of the array, exactly like
[manual-threads](../manual-threads). The shared total is protected by an explicit
`java.util.concurrent.locks.ReentrantLock` instead of the `synchronized` keyword.

## How it works

```java
private final Lock lock = new ReentrantLock();

void add(long value) {
  lock.lock();          // outside try: if lock() itself fails, there is nothing to unlock
  try {
    sum += value;       // critical section: only one thread at a time
  } finally {
    lock.unlock();      // always released, even if the critical section throws
  }
}
```

`synchronized` and `ReentrantLock` give the same two guarantees:

- **Mutual exclusion**: only one thread runs the critical section at a time.
- **Visibility**: everything written before `unlock()` is visible to the next thread after its
  `lock()` (a *happens-before* edge), just like leaving and entering a `synchronized` block.

Both are *reentrant*: a thread that already holds the lock can take it again without deadlocking
itself.

## What `ReentrantLock` adds over `synchronized`

| Feature | `synchronized` | `ReentrantLock` |
|---|---|---|
| Released automatically | yes, at the end of the block | no, you must `unlock()` in `finally` |
| Try without blocking | no | `tryLock()` |
| Give up after a timeout | no | `tryLock(1, SECONDS)` |
| Interruptible while waiting | no | `lockInterruptibly()` |
| Fair ordering of waiters | no | `new ReentrantLock(true)` |
| Several wait conditions | one (`wait`/`notify`) | many, via `newCondition()` |
| Inspect state | no | `isLocked()`, `getQueueLength()`, ... |

Related locks in the same package:

- `ReentrantReadWriteLock`: many readers **or** one writer, useful when reads dominate.
- `StampedLock`: adds *optimistic reads*. You read without locking and then check that no write
  happened in between.

## Trade-offs

- For a critical section this short, `synchronized` is simpler and just as fast. Use
  `ReentrantLock` when you need one of the features in the table.
- Forgetting `unlock()`, or calling it outside `finally`, leaves the lock held forever, and every
  other thread then blocks on it. The compiler does not catch this.
- Each thread takes the lock once, so the lock costs nothing measurable here. Locking inside the
  per-element loop would make it slower than the sequential sum.

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
  SumTask.java     Runnable: sums one part into the shared total
  FillTask.java    Runnable: fills one part with random values
  SharedSum.java   accumulator for the partial sums, guarded by a ReentrantLock
src/test/java/...  JUnit 5 tests
```
