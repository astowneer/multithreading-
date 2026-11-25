package com.astowner.multithreading.lab1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.ForkJoinPool;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class SumTaskTest {

  /** Threshold 1 splits all the way down to single elements: the deepest possible recursion. */
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 7, 1_000})
  void anyThresholdGivesTheSameSum(int threshold) {
    int[] arr = new int[10_007];
    long expected = 0;
    for (int i = 0; i < arr.length; i++) {
      arr[i] = i % 13;
      expected += i % 13;
    }

    try (ForkJoinPool pool = new ForkJoinPool(4)) {
      assertEquals(expected, pool.invoke(new SumTask(arr, 0, arr.length, threshold)));
    }
  }

  @Test
  void sumsOnlyItsOwnRange() {
    int[] arr = {1, 2, 3, 4, 5};

    try (ForkJoinPool pool = new ForkJoinPool(2)) {
      assertEquals(2 + 3 + 4, pool.invoke(new SumTask(arr, 1, 4, 1)));
    }
  }

  @Test
  void rejectsNonPositiveThreshold() {
    assertThrows(IllegalArgumentException.class, () -> new SumTask(new int[1], 0, 1, 0));
  }
}
