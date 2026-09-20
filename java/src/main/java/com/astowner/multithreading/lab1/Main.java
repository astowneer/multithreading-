package com.astowner.multithreading.lab1;

/** Fills an array, sums it with threads and without, and prints how long each took. */
public final class Main {

  private Main() {}

  public static void main(String[] args) {
    int dimension = 1_000_000_000;
    int numberOfThreads = 4;

    int[] arr = new int[dimension];
    ArraySum.fillRandomParallel(arr, numberOfThreads);

    long start = System.nanoTime();
    long parallelSum = ArraySum.parallel(arr, numberOfThreads);
    System.out.println("Multithreaded time (seconds): " + secondsSince(start));
    System.out.println("SUM multithreaded: " + parallelSum);

    start = System.nanoTime();
    long sequentialSum = ArraySum.sequential(arr);
    System.out.println("No threads time (seconds): " + secondsSince(start));
    System.out.println("SUM no threads: " + sequentialSum);
  }

  private static double secondsSince(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000_000.0;
  }
}
