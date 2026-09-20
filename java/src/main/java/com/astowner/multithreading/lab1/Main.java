package com.astowner.multithreading.lab1;

/** Fills an array, sums it with threads and without, and compares the results. */
public final class Main {

  private static final int DEFAULT_SIZE = 1_000_000_000;
  private static final int DEFAULT_THREADS = 4;

  private static final int EXIT_OK = 0;
  private static final int EXIT_FAILURE = 1;
  private static final int EXIT_USAGE = 2;

  private static final String USAGE = "Usage: java -jar lab1-array-sum.jar [size] [threads]";

  private Main() {}

  public static void main(String[] args) throws InterruptedException {
    int exitCode = run(args);
    if (exitCode != EXIT_OK) {
      System.exit(exitCode);
    }
  }

  private static int run(String[] args) throws InterruptedException {
    int size;
    int threads;
    try {
      if (args.length > 2) {
        throw new IllegalArgumentException("Too many arguments");
      }
      size = args.length > 0 ? parsePositive(args[0], "size") : DEFAULT_SIZE;
      threads = args.length > 1 ? parsePositive(args[1], "threads") : DEFAULT_THREADS;
    } catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
      System.err.println(USAGE);
      return EXIT_USAGE;
    }

    int[] arr = new int[size];
    ArraySum.fillRandomParallel(arr, threads);

    long start = System.nanoTime();
    long parallelSum = ArraySum.parallel(arr, threads);
    System.out.println("Multithreaded time (seconds): " + secondsSince(start));
    System.out.println("SUM multithreaded: " + parallelSum);

    start = System.nanoTime();
    long sequentialSum = ArraySum.sequential(arr);
    System.out.println("No threads time (seconds): " + secondsSince(start));
    System.out.println("SUM no threads: " + sequentialSum);

    boolean match = parallelSum == sequentialSum;
    System.out.println("Results match: " + match);
    return match ? EXIT_OK : EXIT_FAILURE;
  }

  private static int parsePositive(String text, String name) {
    try {
      int value = Integer.parseInt(text);
      if (value >= 1) {
        return value;
      }
    } catch (NumberFormatException e) {
      // fall through to the shared error below
    }
    throw new IllegalArgumentException(name + " must be a positive integer, was '" + text + "'");
  }

  private static double secondsSince(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000_000.0;
  }
}
