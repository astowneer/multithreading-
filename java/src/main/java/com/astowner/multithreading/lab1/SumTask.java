package com.astowner.multithreading.lab1;

/** Sums one part of an array and adds the partial result to a shared total. */
final class SumTask implements Runnable {
  private final int[] arr;
  private final ChunkRange range;
  private final SharedSum total;

  SumTask(int[] arr, ChunkRange range, SharedSum total) {
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
