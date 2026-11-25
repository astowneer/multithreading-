package com.astowner.multithreading.lab1;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Sums one part of an array and adds the partial result to a shared {@link AtomicLong}.
 *
 * <p>{@code addAndGet} is a lock-free compare-and-set (CAS) loop: the CPU atomically replaces the
 * old value with {@code old + delta} only if nobody changed it in between, and retries otherwise.
 * No thread is ever blocked, unlike with {@code synchronized}.
 */
final class SumTask implements Runnable {
  private final int[] arr;
  private final ChunkRange range;
  private final AtomicLong total;

  SumTask(int[] arr, ChunkRange range, AtomicLong total) {
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
    total.addAndGet(localSum);
  }
}
