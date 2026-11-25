package com.astowner.multithreading.lab1;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

  /** Fills the array with random values in {@code [0, 100)} using {@code threads} virtual threads. */
  public static void fillRandomParallel(int[] arr, int threads) throws InterruptedException {
    Objects.requireNonNull(arr, "arr");
    List<FillTask> tasks = ChunkRange.split(arr.length, threads).stream()
        .map(range -> new FillTask(arr, range))
        .toList();

    runAll("fill", tasks);
  }

  /** Starts one virtual thread per task, then waits for all of them to finish. */
  private static void runAll(String name, List<? extends Runnable> tasks)
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
