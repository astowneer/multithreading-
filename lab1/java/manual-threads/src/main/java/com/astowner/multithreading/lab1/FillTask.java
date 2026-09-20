package com.astowner.multithreading.lab1;

import java.util.concurrent.ThreadLocalRandom;

/** Fills one part of an array with random values in {@code [0, MAX_EXCLUSIVE)}. */
final class FillTask implements Runnable {
  static final int MAX_EXCLUSIVE = 100;

  private final int[] arr;
  private final ChunkRange range;

  FillTask(int[] arr, ChunkRange range) {
    this.arr = arr;
    this.range = range;
  }

  @Override
  public void run() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = range.start(); i < range.end(); i++) {
      arr[i] = random.nextInt(MAX_EXCLUSIVE);
    }
  }
}
