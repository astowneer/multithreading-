# Lab 1 - Java - fork/join

Implementation of [Lab 1](../../README.md) in Java.

**Approach:** the **fork/join framework** (`java.util.concurrent.ForkJoinPool` with a
`RecursiveTask<Long>`). Instead of cutting the array once into `threads` parts, the task halves its
range recursively. A range at or below a threshold is summed with a loop. The pool's workers
balance the load among themselves by **work stealing**.

## How it works

```java
protected Long compute() {
  if (end - start <= threshold) {
    return loopSum(start, end);          // base case: small enough
  }
  int mid = start + (end - start) / 2;
  SumTask left  = new SumTask(arr, start, mid, threshold);
  SumTask right = new SumTask(arr, mid,   end, threshold);
  left.fork();                           // push left onto this worker's deque
  long r = right.compute();              // do right ourselves
  long l = left.join();                  // get left: stolen by someone, or run it now
  return l + r;
}
```

The recursion forms a tree of tasks:

```
                [0, n)
              /        \
       [0, n/2)        [n/2, n)
       /     \          /     \
     ...     ...      ...     ...     <- leaves: at most `threshold` elements, plain loop
```

## Work stealing

Every worker thread has its own **double-ended queue (deque)** of tasks.

- A worker pushes the tasks it forks onto the **head** of its own deque and takes its next task
  from there too (LIFO). That keeps recently touched, cache-warm data on the same core.
- An idle worker **steals** from the **tail** of another worker's deque (FIFO). The tail holds the
  oldest and therefore biggest subtasks, so one steal brings a lot of work.

This keeps all cores busy even when parts take different amounts of time, for example because
another program uses a core. With a fixed split into `threads` parts, as in
[manual-threads](../manual-threads), the whole sum waits for the slowest part.

## Choosing the threshold

- Too **small**: millions of tiny tasks. Creating and scheduling them costs more than adding a
  few numbers.
- Too **large**: too few tasks to balance the load, so you are back to a fixed split.
- This project uses `max(10_000, n / (threads * 8))`, about 8 leaves per worker. The JDK's own
  rule of thumb is that each leaf should do 100 to 10,000 basic steps.

## Idioms and pitfalls

- **`fork()` one half, `compute()` the other.** Forking both halves and then joining both wastes
  the current thread, which just waits. `invokeAll(left, right)` is an equivalent shortcut.
- **Join in reverse order of forking.** Forking a, then b, then joining a before b can make a
  worker wait on a task that is still at the bottom of its own deque.
- **No blocking I/O in fork/join tasks.** The pool has a fixed number of workers; a blocked worker
  is a lost core. Use `ManagedBlocker`, or a different executor, for blocking work.
- **`RecursiveTask<V>` vs `RecursiveAction`.** A `RecursiveTask` returns a value. A
  `RecursiveAction` returns nothing, which suits in-place work like a parallel fill or sort.
  `CountedCompleter` is a lower-level variant that the JDK uses for parallel streams.
- `ForkJoinPool.commonPool()` is the JVM-wide shared pool. Parallel streams,
  `Arrays.parallelSort` and `CompletableFuture` all use it by default. This project creates a
  dedicated pool so that `threads` controls the parallelism.

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
| `threads` | `4`             | parallelism of the ForkJoinPool   |

The default array is `int[1_000_000_000]`, about 4 GB, so it needs `-Xmx5g` or more. For a quick
run use a smaller size, e.g. `... .jar 100000000 8`.

The exit code is `0` if the parallel and sequential sums match, `1` if they differ or the array
does not fit in memory, and `2` for invalid arguments.

## Layout

```
src/main/java/com/astowner/multithreading/lab1/
  Main.java        command-line entry point and timing
  ArraySum.java    sequential sum, parallel sum (ForkJoinPool), parallel random fill
  ChunkRange.java  splits [0, size) into balanced parts, used by the fill
  SumTask.java     RecursiveTask<Long>: halves its range until it is small, then loops
  FillTask.java    Runnable: fills one part with random values
src/test/java/...  JUnit 5 tests
```
