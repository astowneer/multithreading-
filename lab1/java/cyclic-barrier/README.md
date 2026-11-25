# Lab 1 - Java - CyclicBarrier

Implementation of [Lab 1](../../README.md) in Java.

**Approach:** one explicit thread per part. Each worker stores its partial sum and then waits at a
`java.util.concurrent.CyclicBarrier` for all the others. When the last worker arrives, the barrier
runs its **barrier action**, which adds the partial sums together, and then releases everyone.

## How it works

```java
PartialSums sums = new PartialSums(parts);
CyclicBarrier barrier = new CyclicBarrier(parts, sums::combine);   // action runs once per trip

// each worker:
sums.set(i, sumOfMyPart());
barrier.await();                     // wait here until all `parts` threads arrive

// main:
runAll(tasks);                       // start + join
return sums.total();
```

Timeline for 3 workers:

```
worker-0  ███████░░░░░░░░░┐
worker-1  ███████████████░┤ last to arrive: runs combine(), then all three are released
worker-2  ██████████░░░░░░┘
          compute   wait   barrier trips
```

## Why it is correct

- Each slot of `partials` is written by one worker, before it calls `await()`.
- The barrier guarantees that actions before `await()` in every party *happen-before* the barrier
  action, and the action *happens-before* every party returns from `await()`. So `combine()` sees
  all slots.
- `main` reads `total` after `join()`, which makes the action's write visible to it.

## `CountDownLatch` vs `CyclicBarrier`

| | `CountDownLatch` | `CyclicBarrier` |
|---|---|---|
| Who waits | other threads wait **for** the workers | the workers wait **for each other** |
| Count goes down by | `countDown()`, from any thread, any number of times | each party's `await()` |
| Reusable | no | **yes**, it resets after each trip ("cyclic") |
| Action when complete | none | optional barrier action |
| On failure | n/a | the barrier is *broken* for all waiters |

## Where a barrier really fits

A one-shot sum like this one only needs a single meeting point. Barriers shine in **iterative
algorithms that work in phases**, where every thread must finish step *k* before anyone starts
step *k + 1*:

- simulations: each step, every thread updates its region of a grid, then all wait, then the
  next step reads the neighbours' new values;
- iterative solvers (Jacobi, PageRank): the barrier action checks for convergence;
- parallel prefix sums and other multi-pass algorithms.

`CyclicBarrierTest.theSameBarrierIsReusedForSeveralRounds` shows three rounds on one barrier.

## Pitfalls

- **The party count is fixed.** If one fewer thread calls `await()` than the barrier expects,
  everyone waits forever. [phaser](../phaser) lets parties register and leave at runtime.
- **Breakage spreads.** If a waiting thread is interrupted or times out, the barrier becomes
  *broken*, and every other waiter gets `BrokenBarrierException`. Call `reset()` before reusing it.
- **The barrier action runs on a worker thread**, the last one to arrive. A slow action delays
  every party, and an exception thrown from it breaks the barrier.

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
  Main.java         command-line entry point and timing
  ArraySum.java     sequential sum, parallel sum (CyclicBarrier), parallel random fill
  ChunkRange.java   splits [0, size) into balanced parts (lengths differ by at most 1)
  SumTask.java      Runnable: sums one part, stores it, waits at the barrier
  PartialSums.java  slots for the partial sums; combine() is the barrier action
  FillTask.java     Runnable: fills one part with random values
src/test/java/...  JUnit 5 tests
```
