# Lab 1 - Java - Phaser

Implementation of [Lab 1](../../README.md) in Java.

**Approach:** one explicit thread per part, coordinated by a `java.util.concurrent.Phaser`, the
most flexible synchronizer in the JDK. `main` is a party of the phaser, and each worker is
**registered at runtime** before it starts. A worker stores its partial sum and calls
`arriveAndDeregister()`. `main` calls `arriveAndAwaitAdvance()`, which returns once every party has
arrived, that is, once phase 0 is over. `main` then adds up the slots.

## How it works

```java
Phaser phaser = new Phaser(1);          // party #1: main
for (each part i) {
  phaser.register();                    // one more party, before the thread starts
  start(worker i);
}
phaser.arriveAndAwaitAdvance();         // main arrives, waits for the rest

// each worker:
try {
  partials[i] = sumOfMyPart();
} finally {
  phaser.arriveAndDeregister();         // "done", without waiting, and leave
}
```

Here the Phaser does the job of a `CountDownLatch` whose count is not known up front. Registering
before `start()` matters: if a worker registered itself inside `run()`, `main` could arrive while
it was the only registered party and pass straight through.

## The Phaser vocabulary

| Method | Meaning |
|---|---|
| `register()` / `bulkRegister(n)` | add parties **at runtime** |
| `arrive()` | "I'm done with this phase", without waiting |
| `arriveAndAwaitAdvance()` | arrive and wait for everyone, like `CyclicBarrier.await()` |
| `arriveAndDeregister()` | arrive and leave; later phases wait for one fewer party |
| `awaitAdvance(phase)` | wait for a phase to end without being a party |
| `onAdvance(phase, parties)` | override: runs between phases; return `true` to terminate |
| `getPhase()` | the current phase number, starting at 0 and growing by 1 each advance |

Like the latch and the barrier, the Phaser gives *happens-before*: actions before an arrival are
visible to every thread after the phase advances.

## The three synchronizers compared

| | `CountDownLatch` | `CyclicBarrier` | `Phaser` |
|---|---|---|---|
| Number of parties | fixed | fixed | **dynamic** |
| Reusable | no | yes | yes, with numbered phases |
| Arrive without waiting | yes, `countDown()` | no | yes, `arrive()` |
| Action between phases | no | barrier action | `onAdvance()` |
| Can terminate itself | no | no | yes, `onAdvance` returns `true` |
| Tiering for many parties | no | no | yes, child phasers |

`PhaserTest` shows three workers going through three phases in lock-step, with `onAdvance`
terminating the phaser after the last one.

## Pitfalls

- **Register before starting the thread**, as explained above.
- **A party that never arrives** blocks the phase forever. That is why `arriveAndDeregister()` is
  in `finally`.
- **At most 65,535 parties per phaser.** For more, build a tree of phasers with
  `new Phaser(parent)`.
- **A Phaser is overkill for this lab.** The plain sum only needs a latch or `join()`. A Phaser
  pays off when tasks join and leave a multi-phase computation while it runs.

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
  ArraySum.java    sequential sum, parallel sum (waits on a Phaser), parallel random fill
  ChunkRange.java  splits [0, size) into balanced parts (lengths differ by at most 1)
  SumTask.java     Runnable: sums one part, stores it, arrives and deregisters
  FillTask.java    Runnable: fills one part with random values
src/test/java/...  JUnit 5 tests
```
