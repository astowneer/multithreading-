package com.astowner.multithreading.lab1;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;

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
   * Sums the array using {@code threads} worker threads, each handling one contiguous part. The
   * partial sums are added into a {@link LongAdder}, which spreads contended updates over several
   * cells.
   */
  public static long parallel(int[] arr, int threads) throws InterruptedException {
    Objects.requireNonNull(arr, "arr");
    LongAdder total = new LongAdder();
    List<SumTask> tasks = ChunkRange.split(arr.length, threads).stream()
        .map(range -> new SumTask(arr, range, total))
        .toList();

    runAll("sum", tasks);
    // sum() is only exact when no thread is still adding. That holds here: runAll joined them all.
    return total.sum();
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
