package com.astowner.multithreading.lab1;

import java.util.concurrent.Phaser;

/**
 * Sums one part of an array, stores the result in its own slot, then arrives at the phaser and
 * deregisters: this worker takes no part in later phases.
 */
final class SumTask implements Runnable {
  private final int[] arr;
  private final ChunkRange range;
  private final long[] partials;
  private final int index;
  private final Phaser phaser;

  SumTask(int[] arr, ChunkRange range, long[] partials, int index, Phaser phaser) {
    this.arr = arr;
    this.range = range;
    this.partials = partials;
    this.index = index;
    this.phaser = phaser;
  }

  @Override
  public void run() {
    try {
      long localSum = 0;
      for (int i = range.start(); i < range.end(); i++) {
        localSum += arr[i];
      }
      partials[index] = localSum;
    } finally {
      // Does not wait for the others. It only signals "done" and leaves the phaser.
      phaser.arriveAndDeregister();
    }
  }
}
