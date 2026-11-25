# Lab 1 - Java - LongAdder

Implementation of [Lab 1](../../README.md) in Java.

**Approach:** explicit `Thread` objects, one per part of the array, exactly like
[manual-threads](../manual-threads). The partial sums are combined in a
`java.util.concurrent.atomic.LongAdder`, the JDK's accumulator built for many writers.

## How it works

```java
LongAdder total = new LongAdder();
// in each worker thread:
total.add(localSum);
// in main, after join():
return total.sum();
```

An [AtomicLong](../atomic-long) is one memory location. When many threads update it at the same
time, their CAS operations keep failing and retrying, and the cache line holding the value moves
from core to core on every update (*cache-line ping-pong*).

`LongAdder` avoids that by **striping**:

- It starts with a single `base` field, which costs the same as an `AtomicLong` while there is no
  contention.
- When a CAS on `base` fails, it creates an array of `Cell`s, padded with `@Contended` so that
  each sits in its own cache line, and each thread hashes to one cell.
- If two threads still collide on a cell, one is re-hashed to another cell and the array grows,
  up to the number of CPUs.
- `sum()` adds `base` and all the cells together.

So writes are cheap and scale with the number of cores, while reads are more expensive (*O(cells)*)
and are not a consistent snapshot while writes are still happening.

## When to use which

| | `synchronized` / lock | `AtomicLong` | `LongAdder` |
|---|---|---|---|
| Blocking | yes | no (CAS) | no (CAS) |
| Writes under contention | slow | degrades | scales |
| Exact read at any moment | yes | yes | no, only once writers stop |
| Supports `compareAndSet` | n/a | yes | no |
| Memory | one field | one field | up to one cell per CPU |

`LongAdder` fits counters and statistics with many writers that are read rarely (request
counters, metrics). `AtomicLong` fits values that you also read or compare often, such as IDs and
sequence numbers.

In this lab every thread writes **once**, so the three variants run at the same speed. The
difference becomes visible only if the threads update the total very often.

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
  SumTask.java     Runnable: sums one part and adds it to the shared LongAdder
  FillTask.java    Runnable: fills one part with random values
src/test/java/...  JUnit 5 tests
```
