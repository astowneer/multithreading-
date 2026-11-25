# Lab 1 - Java - parallel prefix (scan)

Implementation of [Lab 1](../../README.md) in Java.

**Approach:** `java.util.Arrays.parallelPrefix`, a parallel **scan**. It turns the array into its
running totals, `prefix[i] = arr[0] + ... + arr[i]`, so the total of the whole array is the last
element. `Arrays.parallelSetAll` first copies the `int`s into a `long[]` in parallel, so that the
running totals cannot overflow.

> **Memory:** the `long[]` takes 8 bytes per element, twice the size of the input. With the
> default 1 billion elements that is 4 GB + 8 GB, so run it with a smaller size:
> `java -jar target/lab1-array-sum-1.0-SNAPSHOT.jar 100000000 8`.

## How it works

```java
long[] prefix = new long[arr.length];
Arrays.parallelSetAll(prefix, i -> arr[i]);   // [3, 1, 4, 1, 5]
Arrays.parallelPrefix(prefix, Long::sum);      // [3, 4, 8, 9, 14]
return prefix[prefix.length - 1];              // 14
```

Both calls run inside a dedicated `ForkJoinPool(threads)`, the same trick as in
[parallel-streams](../parallel-streams).

## How can a running total be parallel at all?

Every `prefix[i]` depends on every element before it, so this looks sequential. The parallel scan
works in **two passes** over a tree of chunks, the classic up-sweep/down-sweep scheme:

```
input:          3  1  4  1 | 5  9  2  6        two chunks, one per worker

pass 1 (up):    each chunk is scanned locally, in parallel
                3  4  8  9 | 5 14 16 22        chunk totals: 9 and 22

combine:        prefix of the chunk totals: 0 for the first chunk, 9 for the second

pass 2 (down):  each chunk adds its offset, in parallel
                3  4  8  9 |14 23 25 31
```

The JDK's version, `ArrayPrefixHelpers`, uses fork/join `CountedCompleter`s to do this
recursively. It needs about twice the work of a sequential scan, but the work runs on all cores.

The operator must be **associative**: `(a + b) + c == a + (b + c)`. Otherwise the chunked result
would differ from the sequential one. `Long::sum`, `Math::max` and `Math::min` are fine.
Subtraction is not.

## Where scans are useful

A total alone is cheaper with [parallel-streams](../parallel-streams) or [fork-join](../fork-join).
A scan is the building block when you need **all** the running totals:

- cumulative statistics, such as a running balance or a histogram turned into a CDF;
- *stream compaction*: computing where each kept element goes in the output;
- radix sort, polynomial evaluation, and many GPU algorithms.

## Related `Arrays` parallel helpers

| Method | Does |
|---|---|
| `Arrays.parallelPrefix(a, op)` | in-place inclusive scan |
| `Arrays.parallelSetAll(a, i -> ...)` | fill each element from its index, in parallel |
| `Arrays.parallelSort(a)` | parallel merge sort, on the common pool |

All three run on `ForkJoinPool.commonPool()` unless they are started from another pool's worker.

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
  ArraySum.java    sequential sum, parallel prefix sums, sum = last prefix, parallel fill
  ChunkRange.java  splits [0, size) into balanced parts, used by the fill
  FillTask.java    Runnable: fills one part with random values
src/test/java/...  JUnit 5 tests
```
