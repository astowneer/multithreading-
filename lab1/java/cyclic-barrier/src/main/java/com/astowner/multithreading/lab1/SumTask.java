package com.astowner.multithreading.lab1;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Sums one part of an array, stores the result, and waits at the barrier until every other part
 * is done. The last thread to arrive runs the barrier action, which adds the partial sums up.
 */
final class SumTask implements Runnable {
  private final int[] arr;
  private final ChunkRange range;
  private final PartialSums sums;
  private final int index;
  private final CyclicBarrier barrier;

  SumTask(int[] arr, ChunkRange range, PartialSums sums, int index, CyclicBarrier barrier) {
    this.arr = arr;
    this.range = range;
    this.sums = sums;
    this.index = index;
    this.barrier = barrier;
  }

  @Override
  public void run() {
    long localSum = 0;
    for (int i = range.start(); i < range.end(); i++) {
      localSum += arr[i];
    }
    sums.set(index, localSum);

    try {
      barrier.await();
    } catch (InterruptedException e) {
      // Restore the flag so the code that owns this thread can see it was interrupted.
      Thread.currentThread().interrupt();
    } catch (BrokenBarrierException e) {
      // Another party was interrupted or timed out, so this barrier will never trip.
      throw new IllegalStateException("barrier broken while waiting for the other parts", e);
    }
  }
}
