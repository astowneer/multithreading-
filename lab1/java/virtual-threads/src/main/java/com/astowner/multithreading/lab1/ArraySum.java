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

  /** Sums the array using {@code threads} virtual threads, each handling one contiguous part. */
  public static long parallel(int[] arr, int threads) throws InterruptedException {
    Objects.requireNonNull(arr, "arr");
    SharedSum total = new SharedSum();
    List<SumTask> tasks = ChunkRange.split(arr.length, threads).stream()
        .map(range -> new SumTask(arr, range, total))
        .toList();

    runAll("sum", tasks);
    return total.getSum();
  }

  /**
   * Same sum, written with {@link Executors#newVirtualThreadPerTaskExecutor()}: every submitted
   * task gets a brand-new virtual thread, and each part returns its sum through a {@link Future}.
   * There is no pool to size, because virtual threads are cheap enough to create one per task.
   */
  public static long parallelWithExecutor(int[] arr, int threads) throws InterruptedException {
    Objects.requireNonNull(arr, "arr");
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<Long>> partials = ChunkRange.split(arr.length, threads).stream()
          .map(range -> executor.submit(() -> sumRange(arr, range)))
          .toList();

      long total = 0;
      for (Future<Long> partial : partials) {
        try {
          total += partial.get();
        } catch (ExecutionException e) {
          throw new IllegalStateException("a sum task failed", e.getCause());
        }
      }
      return total;
    }
  }

  private static long sumRange(int[] arr, ChunkRange range) {
    long sum = 0;
    for (int i = range.start(); i < range.end(); i++) {
      sum += arr[i];
    }
    return sum;
  }

  /**
   * Fills the array with random values in {@code [0, 100)} using {@code threads} virtual threads.
   */
  public static void fillRandomParallel(int[] arr, int threads) throws InterruptedException {
    Objects.requireNonNull(arr, "arr");
    List<FillTask> tasks = ChunkRange.split(arr.length, threads).stream()
        .map(range -> new FillTask(arr, range))
        .toList();

    runAll("fill", tasks);
  }

  /** Starts one virtual thread per task, then waits for all of them to finish. */
  static void runAll(String name, List<? extends Runnable> tasks)
      throws InterruptedException {
    // Thread.ofVirtual() (Java 21) builds virtual threads; Thread.ofPlatform() builds the classic
    // OS-backed ones that `new Thread(...)` creates. name(prefix, 0) numbers them sum-0, sum-1, ...
    Thread.Builder builder = Thread.ofVirtual().name(name + "-", 0);
    List<Thread> workers = new ArrayList<>(tasks.size());
    for (Runnable task : tasks) {
      workers.add(builder.unstarted(task));
    }

    workers.forEach(Thread::start);
    for (Thread worker : workers) {
      worker.join();
    }
  }
}
