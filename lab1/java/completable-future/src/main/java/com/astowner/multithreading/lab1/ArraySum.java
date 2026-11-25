package com.astowner.multithreading.lab1;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Sequential and multithreaded operations on {@code int[]} arrays. */
public final class ArraySum {

  private ArraySum() {}

  /** Sums the array on the calling thread. */
  public static long sequential(int[] arr) {
    Objects.requireNonNull(arr, "arr");
    long sum = 0;
    for (int value : arr) {
      sum += value;
    }
    return sum;
  }

  /**
   * Sums the array with {@link CompletableFuture}s on a pool of {@code threads} threads. Each part
   * is started with {@code supplyAsync}, and the partial results are combined pairwise with
   * {@code thenCombine} into one future that completes with the total.
   */
  public static long parallel(int[] arr, int threads) throws InterruptedException {
    Objects.requireNonNull(arr, "arr");

    // Without an explicit executor, supplyAsync uses ForkJoinPool.commonPool(), whose size is
    // fixed by the JVM. Passing our own pool makes the `threads` argument mean something.
    try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
      List<CompletableFuture<Long>> partials = ChunkRange.split(arr.length, threads).stream()
          .map(range -> CompletableFuture.supplyAsync(new SumTask(arr, range), pool))
          .toList();

      // Build a pipeline: ((p0 + p1) + p2) + ... Each thenCombine runs as soon as both of its
      // inputs are ready. Nothing blocks until get() below.
      CompletableFuture<Long> total = partials.stream()
          .reduce((left, right) -> left.thenCombine(right, Long::sum))
          .orElseThrow(); // split() always returns at least one part

      try {
        return total.get();
      } catch (ExecutionException e) {
        throw new IllegalStateException("a sum task failed", e.getCause());
      }
    }
  }

  /** Fills the array with random values in {@code [0, 100)} using {@code threads} threads. */
  public static void fillRandomParallel(int[] arr, int threads) throws InterruptedException {
    Objects.requireNonNull(arr, "arr");
    List<FillTask> tasks = ChunkRange.split(arr.length, threads).stream()
        .map(range -> new FillTask(arr, range))
        .toList();

    runAll("fill", tasks);
  }

  /** Starts one thread per task, then waits for all of them to finish. */
  private static void runAll(String name, List<? extends Runnable> tasks)
      throws InterruptedException {
    List<Thread> workers = new ArrayList<>(tasks.size());
    for (int i = 0; i < tasks.size(); i++) {
      workers.add(new Thread(tasks.get(i), name + "-" + i));
    }

    workers.forEach(Thread::start);
    for (Thread worker : workers) {
      worker.join();
    }
  }
}
