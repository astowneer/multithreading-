# Lab 1 - Java - parallel streams

Implementation of [Lab 1](../../README.md) in Java.

**Approach:** the Stream API with `.parallel()`. The whole parallel sum is one line, and the JDK
does the splitting, scheduling and combining. Underneath it is [fork/join](../fork-join): the
stream's `Spliterator` splits the array recursively, and `ForkJoinPool` workers sum the pieces.

## How it works

```java
// the idiomatic version: runs on ForkJoinPool.commonPool()
Arrays.stream(arr).parallel().asLongStream().sum();

// with a chosen number of threads: start the stream from inside your own pool
try (ForkJoinPool pool = new ForkJoinPool(threads)) {
  return pool.submit(() -> Arrays.stream(arr).parallel().asLongStream().sum()).get();
}
```

- `Arrays.stream(int[])` gives an `IntStream` over the array, without boxing.
- `.parallel()` only sets a flag. Nothing runs until the terminal operation (`sum()`).
- `.asLongStream()` widens each `int` to `long`. `IntStream.sum()` returns an `int` and would
  overflow on a billion elements.
- `sum()` is a **reduction**. Each piece is summed separately, then the partial sums are added
  together. It is the same map-reduce shape as [partial-sums](../partial-sums), but written for you.

`main` exposes both `ArraySum.parallelOnCommonPool(arr)` and `ArraySum.parallel(arr, threads)`.

## Controlling the number of threads

A parallel stream has no parameter for the number of threads. It uses:

1. **the pool of the current thread**, if the terminal operation runs inside a `ForkJoinPool`
   worker. This project uses that to honour `threads`. It is JDK behaviour that is widely relied
   on, not part of the specification. `ParallelStreamPoolTest` checks it;
2. otherwise **`ForkJoinPool.commonPool()`**, sized `availableProcessors() - 1`. The calling
   thread also does work, so all cores are used. The size can be changed for the whole JVM with
   `-Djava.util.concurrent.ForkJoinPool.common.parallelism=N`.

## When parallel streams pay off

The rough **N × Q** rule: parallelism helps when *number of elements × cost per element* is
large, about ≥ 10,000. Summing a billion ints qualifies. Summing a list of 100 does not.

The source must also **split well**:

| Splits well | Splits badly |
|---|---|
| arrays, `ArrayList`, `IntStream.range` | `LinkedList`, `Stream.iterate`, I/O lines |

## Pitfalls

- **Side effects in lambdas are data races:**
  ```java
  long[] total = {0};
  Arrays.stream(arr).parallel().forEach(v -> total[0] += v);   // WRONG: lost updates
  ```
  Use a reduction (`sum`, `reduce`, `collect`) instead.
- **`reduce` needs an associative operation and a real identity.** `reduce(0, Integer::sum)` is
  fine; `reduce(10, ...)` adds 10 once per piece.
- **Shared common pool.** A slow or blocking task in one parallel stream slows every other
  parallel stream and `CompletableFuture` in the JVM.
- **Order.** `forEach` on a parallel stream runs in no particular order; `forEachOrdered` keeps
  the order at a cost. Operations like `limit`, `findFirst` and `sorted` are expensive in
  parallel.

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
  ArraySum.java    sequential sum, parallel stream sum (common or dedicated pool), parallel fill
  ChunkRange.java  splits [0, size) into balanced parts, used by the fill
  FillTask.java    Runnable: fills one part with random values
src/test/java/...  JUnit 5 tests
```
