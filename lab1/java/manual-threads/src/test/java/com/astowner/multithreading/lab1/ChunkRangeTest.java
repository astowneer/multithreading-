package com.astowner.multithreading.lab1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ChunkRangeTest {

  @ParameterizedTest
  @CsvSource({
      "0, 1", "0, 4", "1, 4", "3, 4", "10, 3", "10, 8", "100, 7", "100000003, 4", "1000000000, 7"
  })
  void splitCoversEveryIndexOnceAndIsBalanced(int size, int parts) {
    List<ChunkRange> ranges = ChunkRange.split(size, parts);

    assertEquals(parts, ranges.size());
    assertEquals(0, ranges.get(0).start());
    assertEquals(size, ranges.get(parts - 1).end());

    int shortest = Integer.MAX_VALUE;
    int longest = Integer.MIN_VALUE;
    for (int i = 0; i < parts; i++) {
      if (i > 0) {
        assertEquals(ranges.get(i - 1).end(), ranges.get(i).start(), "gap or overlap at part " + i);
      }
      shortest = Math.min(shortest, ranges.get(i).length());
      longest = Math.max(longest, ranges.get(i).length());
    }
    assertTrue(longest - shortest <= 1, "lengths differ by more than one");
  }

  @Test
  void splitGivesExtraElementsToFirstParts() {
    assertEquals(
        List.of(new ChunkRange(0, 4), new ChunkRange(4, 7), new ChunkRange(7, 10)),
        ChunkRange.split(10, 3));
  }

  @Test
  void splitWithMorePartsThanElementsLeavesTrailingRangesEmpty() {
    List<ChunkRange> ranges = ChunkRange.split(2, 4);

    assertEquals(1, ranges.get(0).length());
    assertEquals(1, ranges.get(1).length());
    assertEquals(0, ranges.get(2).length());
    assertEquals(0, ranges.get(3).length());
  }

  @Test
  void splitRejectsInvalidArguments() {
    assertThrows(IllegalArgumentException.class, () -> ChunkRange.split(-1, 2));
    assertThrows(IllegalArgumentException.class, () -> ChunkRange.split(10, 0));
  }

  @Test
  void constructorRejectsInvalidRange() {
    assertThrows(IllegalArgumentException.class, () -> new ChunkRange(5, 3));
    assertThrows(IllegalArgumentException.class, () -> new ChunkRange(-1, 3));
  }
}
