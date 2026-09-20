# Lab 1 - Parallel array sum (Java)

Parallelization of the array element sum: the array is split into contiguous parts, each part is
summed by its own thread, and the partial sums are added into a shared total. The result is
compared with a sequential sum and the timings of both are printed.

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
  SharedSum.java   synchronized accumulator for the partial sums
src/test/java/...  JUnit 5 tests
```
