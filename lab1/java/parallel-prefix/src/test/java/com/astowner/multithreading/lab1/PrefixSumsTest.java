package com.astowner.multithreading.lab1;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PrefixSumsTest {

  @Test
  void everyElementIsTheSumOfAllElementsUpToIt() throws InterruptedException {
    assertArrayEquals(
        new long[] {3, 4, 8, 9, 14},
        ArraySum.prefixSums(new int[] {3, 1, 4, 1, 5}, 2));
  }

  @Test
  void emptyArrayHasNoPrefixes() throws InterruptedException {
    assertEquals(0, ArraySum.prefixSums(new int[0], 2).length);
  }

  /** Large enough that parallelPrefix really splits the work instead of running sequentially. */
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 4, 8})
  void parallelPrefixMatchesASequentialScan(int threads) throws InterruptedException {
    int[] arr = IntStream.range(0, 200_003).map(i -> i % 7).toArray();
    long[] expected = new long[arr.length];
    long running = 0;
    for (int i = 0; i < arr.length; i++) {
      running += arr[i];
      expected[i] = running;
    }

    assertArrayEquals(expected, ArraySum.prefixSums(arr, threads));
  }
}
