package com.astowner.multithreading.lab1;

import java.util.concurrent.RecursiveTask;

/**
 * Sums {@code arr[start, end)} by divide and conquer. A range longer than {@code threshold} is
 * split in half: the left half is forked as a new task, and the right half is computed by the
 * current thread. Ranges at or below the threshold are summed with a plain loop.
 *
 * <p>Idle worker threads of the {@link java.util.concurrent.ForkJoinPool} steal forked tasks from
 * busy ones, so the work spreads over all workers even when some halves finish earlier.
 */
final class SumTask extends RecursiveTask<Long> {
  private final int[] arr;
  private final int start;
  private final int end;
  private final int threshold;

  SumTask(int[] arr, int start, int end, int threshold) {
    if (threshold < 1) {
      throw new IllegalArgumentException("threshold must be >= 1, was " + threshold);
    }
    this.arr = arr;
    this.start = start;
    this.end = end;
    this.threshold = threshold;
  }

  @Override
  protected Long compute() {
    if (end - start <= threshold) {
      long sum = 0;
      for (int i = start; i < end; i++) {
        sum += arr[i];
      }
      return sum;
    }

    int mid = start + (end - start) / 2; // not (start + end) / 2, which can overflow int
    SumTask left = new SumTask(arr, start, mid, threshold);
    SumTask right = new SumTask(arr, mid, end, threshold);

    left.fork();                      // queue the left half; an idle worker may steal it
    long rightSum = right.compute();  // do the right half on this thread, no new task needed
    long leftSum = left.join();       // wait for the left half, or run it here if not stolen
    return leftSum + rightSum;
  }
}
