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
   * Sums the array with a parallel stream on the JVM-wide {@link ForkJoinPool#commonPool()}. This
   * is the one-liner you would normally write; its parallelism is chosen by the JVM.
   */
  public static long parallelOnCommonPool(int[] arr) {
    Objects.requireNonNull(arr, "arr");
    // asLongStream() widens every int to long, so the sum cannot overflow int.
    return Arrays.stream(arr).parallel().asLongStream().sum();
  }

  /**
   * Sums the array with a parallel stream that runs on a dedicated pool of {@code threads}
   * workers, so the parallelism can be chosen.
   *
   * <p>A parallel stream splits its work into fork/join tasks. When the terminal operation starts
   * inside a {@link ForkJoinPool} worker, those tasks are forked into that worker's pool instead
   * of the common pool. This is how the JDK is implemented, but not part of the
   * {@code Stream} specification.
   */
  public static long parallel(int[] arr, int threads) throws InterruptedException {
    Objects.requireNonNull(arr, "arr");
    try (ForkJoinPool pool = new ForkJoinPool(threads)) {
      return pool.submit(() -> Arrays.stream(arr).parallel().asLongStream().sum()).get();
    } catch (ExecutionException e) {
      throw new IllegalStateException("the parallel stream failed", e.getCause());
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
