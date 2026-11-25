package com.astowner.multithreading.lab1;

import java.util.function.Supplier;

/**
 * Sums one part of an array and returns the result. It is a {@link Supplier} because
 * {@link java.util.concurrent.CompletableFuture#supplyAsync} takes one.
 */
final class SumTask implements Supplier<Long> {
  private final int[] arr;
  private final ChunkRange range;

  SumTask(int[] arr, ChunkRange range) {
    this.arr = arr;
    this.range = range;
  }

  @Override
  public Long get() {
    long localSum = 0;
    for (int i = range.start(); i < range.end(); i++) {
      localSum += arr[i];
    }
    return localSum;
  }
}
