package com.astowner.multithreading.lab1;

import java.util.concurrent.CountDownLatch;

/**
 * Sums one part of an array, stores the result in its own slot of a shared array, and then counts
 * down the latch, telling the waiting thread that one more part is done.
 */
final class SumTask implements Runnable {
  private final int[] arr;
  private final ChunkRange range;
  private final long[] partials;
  private final int index;
  private final CountDownLatch done;

  SumTask(int[] arr, ChunkRange range, long[] partials, int index, CountDownLatch done) {
    this.arr = arr;
    this.range = range;
    this.partials = partials;
    this.index = index;
    this.done = done;
  }

  @Override
  public void run() {
    try {
      long localSum = 0;
      for (int i = range.start(); i < range.end(); i++) {
        localSum += arr[i];
      }
      // Written before countDown(), so it is visible to whoever returns from await().
      partials[index] = localSum;
    } finally {
      // In finally: if the loop threw, the waiting thread must still be released.
      done.countDown();
    }
  }
}
