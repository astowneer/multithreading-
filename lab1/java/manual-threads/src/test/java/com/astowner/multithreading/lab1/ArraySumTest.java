package com.astowner.multithreading.lab1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ArraySumTest {

  /** Not divisible by 2, 3, 4, 7 or 16, so every thread count leaves a remainder. */
  private static final int UNEVEN_SIZE = 1_000_003;

  @Test
  void sequentialSumsKnownValues() {
    assertEquals(6, ArraySum.sequential(new int[] {1, 2, 3}));
    assertEquals(0, ArraySum.sequential(new int[0]));
  }

  @Test
  void sumDoesNotOverflowInt() {
    int[] arr = new int[4];
    Arrays.fill(arr, Integer.MAX_VALUE);

    assertEquals(4L * Integer.MAX_VALUE, ArraySum.sequential(arr));
    assertEquals(4L * Integer.MAX_VALUE, sumParallel(arr, 3));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 7, 16})
  void parallelSumMatchesClosedFormForUnevenSize(int threads) throws InterruptedException {
    int[] arr = new int[UNEVEN_SIZE];
    for (int i = 0; i < arr.length; i++) {
      arr[i] = i % 100;
    }
    long expected = 0;
    for (int i = 0; i < arr.length; i++) {
      expected += i % 100;
    }

    assertEquals(expected, ArraySum.parallel(arr, threads));
    assertEquals(expected, ArraySum.sequential(arr));
  }

  @Test
  void parallelSumWorksWithMoreThreadsThanElements() {
    assertEquals(6, sumParallel(new int[] {1, 2, 3}, 8));
    assertEquals(0, sumParallel(new int[0], 4));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3, 4, 7})
  void fillTouchesEveryElementIncludingRemainder(int threads) throws InterruptedException {
    int[] arr = new int[UNEVEN_SIZE];
    Arrays.fill(arr, -1);

    ArraySum.fillRandomParallel(arr, threads);

    assertTrue(
        Arrays.stream(arr).allMatch(v -> v >= 0 && v < FillTask.MAX_EXCLUSIVE),
        "an element was left unfilled or out of range");
  }

  private static long sumParallel(int[] arr, int threads) {
    try {
      return ArraySum.parallel(arr, threads);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError("interrupted", e);
    }
  }
}
