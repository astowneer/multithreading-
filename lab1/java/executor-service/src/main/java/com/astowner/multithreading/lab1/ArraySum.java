package com.astowner.multithreading.lab1;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
   * Sums the array on a fixed pool of {@code threads} threads. The array is split into
   * {@code threads} parts, each part is submitted as a {@link SumTask}, and the partial results
   * are collected from their {@link Future}s.
   */
  public static long parallel(int[] arr, int threads) throws InterruptedException {
    Objects.requireNonNull(arr, "arr");
    List<SumTask> tasks = ChunkRange.split(arr.length, threads).stream()
        .map(range -> new SumTask(arr, range))
        .toList();

    // ExecutorService is AutoCloseable since Java 19: close() waits for running tasks and then
    // shuts the pool down. Before that you had to call shutdown() and awaitTermination() yourself.
    try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
      // invokeAll submits every task and blocks until all of them are done.
      List<Future<Long>> partials = pool.invokeAll(tasks);

      long total = 0;
      for (Future<Long> partial : partials) {
        total += getUnchecked(partial);
      }
      return total;
    }
  }

  /**
   * Returns the result of a completed future. If the task threw, {@code get()} wraps the error in
   * an {@link ExecutionException}; it is unwrapped and rethrown unchecked here.
   */
  private static long getUnchecked(Future<Long> future) throws InterruptedException {
    try {
      return future.get();
    } catch (ExecutionException e) {
      throw new IllegalStateException("a sum task failed", e.getCause());
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
