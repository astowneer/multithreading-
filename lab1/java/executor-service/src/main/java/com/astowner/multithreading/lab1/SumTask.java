package com.astowner.multithreading.lab1;

import java.util.concurrent.Callable;

/**
 * Sums one part of an array and <em>returns</em> the result.
 *
 * <p>Unlike {@link Runnable#run()}, {@link Callable#call()} has a return value and may throw a
 * checked exception. The executor captures both in a {@link java.util.concurrent.Future}.
 */
final class SumTask implements Callable<Long> {
  private final int[] arr;
  private final ChunkRange range;

  SumTask(int[] arr, ChunkRange range) {
    this.arr = arr;
    this.range = range;
  }

  @Override
  public Long call() {
    long localSum = 0;
    for (int i = range.start(); i < range.end(); i++) {
      localSum += arr[i];
    }
    return localSum;
  }
}
