package com.astowner.multithreading.lab1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

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
   * Computes all prefix sums of the array in parallel with {@link Arrays#parallelPrefix} and
   * returns the last one, which is the total. {@code prefix[i]} is
   * {@code arr[0] + ... + arr[i]}.
   *
   * <p>The prefixes are {@code long}s, so this allocates a {@code long[]} twice the size of the
   * input. It is a demonstration of a parallel <em>scan</em>, not the fastest way to get one sum.
   */
  public static long[] prefixSums(int[] arr, int threads) throws InterruptedException {
    Objects.requireNonNull(arr, "arr");
    // Both Arrays.parallelSetAll and Arrays.parallelPrefix fork their work into the pool of the
    // calling worker thread, so running them inside our own pool makes `threads` take effect.
    try (ForkJoinPool pool = new ForkJoinPool(threads)) {
      return pool.submit(() -> {
        long[] prefix = new long[arr.length];
        Arrays.parallelSetAll(prefix, i -> arr[i]);   // widen int -> long, in parallel
        Arrays.parallelPrefix(prefix, Long::sum);      // in-place inclusive scan
        return prefix;
      }).get();
    } catch (ExecutionException e) {
      throw new IllegalStateException("the parallel prefix failed", e.getCause());
    }
  }

  /** Sums the array as the last element of its parallel prefix sums. */
  public static long parallel(int[] arr, int threads) throws InterruptedException {
    long[] prefix = prefixSums(arr, threads);
    return prefix.length == 0 ? 0 : prefix[prefix.length - 1];
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
