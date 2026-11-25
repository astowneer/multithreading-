package com.astowner.multithreading.lab1;

import java.util.concurrent.atomic.LongAdder;

/**
 * Sums one part of an array and adds the partial result to a shared {@link LongAdder}.
 *
 * <p>A {@code LongAdder} keeps several internal cells. Threads that collide on one cell are moved
 * to different cells, so under contention they stop fighting over a single memory location. The
 * cells are added together only when {@link LongAdder#sum()} is called.
 */
final class SumTask implements Runnable {
  private final int[] arr;
  private final ChunkRange range;
  private final LongAdder total;

  SumTask(int[] arr, ChunkRange range, LongAdder total) {
    this.arr = arr;
    this.range = range;
    this.total = total;
  }

  @Override
  public void run() {
    long localSum = 0;
    for (int i = range.start(); i < range.end(); i++) {
      localSum += arr[i];
    }
    total.add(localSum);
  }
}
