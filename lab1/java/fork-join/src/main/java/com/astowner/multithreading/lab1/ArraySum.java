package com.astowner.multithreading.lab1;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;

/** Sequential and multithreaded operations on {@code int[]} arrays. */
public final class ArraySum {

  /** Below this many elements, splitting further costs more than it saves. */
  static final int MIN_THRESHOLD = 10_000;

  /**
   * Splitting into roughly this many leaf tasks per thread gives work stealing enough pieces to
   * balance the load when some threads are slower than others.
   */
  private static final int TASKS_PER_THREAD = 8;

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
   * Sums the array with fork/join on a {@link ForkJoinPool} of {@code threads} workers. Instead of
   * splitting once into {@code threads} parts, the array is halved recursively until a part is
   * no longer than the threshold.
   */
  public static long parallel(int[] arr, int threads) throws InterruptedException {
    Objects.requireNonNull(arr, "arr");
    if (threads < 1) {
      throw new IllegalArgumentException("threads must be >= 1, was " + threads);
    }
    int threshold = Math.max(MIN_THRESHOLD, arr.length / (threads * TASKS_PER_THREAD));

    // A dedicated pool with exactly `threads` workers. ForkJoinPool.commonPool() would also work,
    // but its size is set by the JVM and it is shared with parallel streams.
    try (ForkJoinPool pool = new ForkJoinPool(threads)) {
      return pool.invoke(new SumTask(arr, 0, arr.length, threshold));
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
