package com.astowner.multithreading.lab1;

/**
 * Sums one part of an array and keeps the result in its own field. No state is shared between
 * workers, so no lock or atomic is needed.
 *
 * <p>The field is a plain {@code long}, not {@code volatile}. It is still safe to read after
 * {@link Thread#join()}: everything a thread does happens-before another thread returns from
 * {@code join()} on it.
 */
final class SumTask implements Runnable {
  private final int[] arr;
  private final ChunkRange range;
  private long result;

  SumTask(int[] arr, ChunkRange range) {
    this.arr = arr;
    this.range = range;
  }

  @Override
  public void run() {
    long localSum = 0;
    for (int i = range.start(); i < range.end(); i++) {
      localSum += arr[i];
    }
    result = localSum;
  }

  /** The sum of this part. Only valid after the thread that ran this task was joined. */
  long result() {
    return result;
  }
}
