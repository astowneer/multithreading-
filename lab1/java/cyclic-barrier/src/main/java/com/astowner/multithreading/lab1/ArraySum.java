package com.astowner.multithreading.lab1;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CyclicBarrier;

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
   * workers meet at a {@link CyclicBarrier}; its barrier action, run once by the last thread to
   * arrive, adds the partial sums together.
   */
  public static long parallel(int[] arr, int threads) throws InterruptedException {
    Objects.requireNonNull(arr, "arr");
    List<ChunkRange> ranges = ChunkRange.split(arr.length, threads);
    PartialSums sums = new PartialSums(ranges.size());
    CyclicBarrier barrier = new CyclicBarrier(ranges.size(), sums::combine);

    List<SumTask> tasks = new ArrayList<>(ranges.size());
    for (int i = 0; i < ranges.size(); i++) {
      tasks.add(new SumTask(arr, ranges.get(i), sums, i, barrier));
    }

    runAll("sum", tasks);
    // join() inside runAll makes the total written by the barrier action visible here.
    return sums.total();
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
