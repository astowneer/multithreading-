# Lab 1 - Java - CompletableFuture

Implementation of [Lab 1](../../README.md) in Java.

**Approach:** asynchronous pipelines with `java.util.concurrent.CompletableFuture`. Each part of the
array is started with `supplyAsync` on a fixed pool. The partial results are then combined into a
single future with `thenCombine`, without blocking, and the main thread blocks once, at the end,
for the total.

## How it works

```java
List<CompletableFuture<Long>> partials = ranges.stream()
    .map(r -> CompletableFuture.supplyAsync(new SumTask(arr, r), pool))  // start all parts
    .toList();

CompletableFuture<Long> total = partials.stream()
    .reduce((a, b) -> a.thenCombine(b, Long::sum))   // describe how to combine them
    .orElseThrow();

return total.get();                                  // the only blocking call
```

For 4 parts this builds a small graph that runs on its own as results arrive:

```
p0 ─┐
    ├─ + ─┐
p1 ─┘     ├─ + ─┐
p2 ───────┘     ├─ + ─→ total
p3 ─────────────┘
```

## `Future` vs `CompletableFuture`

A plain `Future` (see [executor-service](../executor-service)) can only be polled or waited on
with `get()`. A `CompletableFuture` can also be **composed**: you describe what happens next, and
it runs as soon as the input is ready.

| Method | Meaning |
|---|---|
| `supplyAsync(supplier, executor)` | start a task that returns a value |
| `runAsync(runnable, executor)` | start a task without a result |
| `thenApply(f)` | transform the result: `map` |
| `thenCompose(f)` | chain another async step: `flatMap` |
| `thenCombine(other, f)` | wait for two futures and merge their results |
| `allOf(...)` / `anyOf(...)` | wait for all / the first of many futures |
| `exceptionally(f)` / `handle(f)` | recover from or inspect an error |
| `orTimeout(...)` / `completeOnTimeout(...)` | fail or use a default after a timeout |
| `join()` / `get()` | block for the result; `join` throws unchecked `CompletionException` |

An equivalent version with `allOf`:

```java
CompletableFuture.allOf(partials.toArray(CompletableFuture[]::new)).join();
long total = partials.stream().mapToLong(CompletableFuture::join).sum();
```

## Pitfalls

- **Default executor.** Without the second argument, `supplyAsync` runs on
  `ForkJoinPool.commonPool()`. Its size is fixed (CPU count minus 1), and it is shared by the
  whole JVM, including parallel streams. Blocking or long tasks there slow everything else down.
  This project passes its own pool.
- **Errors.** If one part throws, every dependent stage fails with a `CompletionException` that
  wraps the original exception. `CompletableFutureErrorTest` shows this, together with recovery
  via `exceptionally`.
- **`thenX` vs `thenXAsync`.** The non-async variants run the callback on whichever thread
  completed the previous stage, or on the caller if it was already done. The `Async` variants
  submit the callback to an executor.
- **Where it shines.** CPU-bound sums gain nothing over [executor-service](../executor-service).
  `CompletableFuture` is most useful for I/O pipelines, such as calling several services in
  parallel and combining their responses.

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
| `threads` | `4`             | size of the thread pool   |

The default array is `int[1_000_000_000]`, about 4 GB, so it needs `-Xmx5g` or more. For a quick
run use a smaller size, e.g. `... .jar 100000000 8`.

The exit code is `0` if the parallel and sequential sums match, `1` if they differ or the array
does not fit in memory, and `2` for invalid arguments.

## Layout

```
src/main/java/com/astowner/multithreading/lab1/
  Main.java        command-line entry point and timing
  ArraySum.java    sequential sum, parallel sum (CompletableFuture pipeline), parallel random fill
  ChunkRange.java  splits [0, size) into balanced parts (lengths differ by at most 1)
  SumTask.java     Supplier<Long>: sums one part and returns it
  FillTask.java    Runnable: fills one part with random values
src/test/java/...  JUnit 5 tests
```
