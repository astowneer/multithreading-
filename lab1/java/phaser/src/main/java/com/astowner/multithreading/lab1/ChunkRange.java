package com.astowner.multithreading.lab1;

import java.util.ArrayList;
import java.util.List;

/** A half-open index range {@code [start, end)} of an array. */
public record ChunkRange(int start, int end) {

  public ChunkRange {
    if (start < 0 || end < start) {
      throw new IllegalArgumentException("Invalid range [" + start + ", " + end + ")");
    }
  }

  public int length() {
    return end - start;
  }

  /**
   * Splits {@code [0, size)} into {@code parts} contiguous ranges that together cover every index
   * exactly once. Range lengths differ by at most one: the first {@code size % parts} ranges get
   * one extra element. If {@code parts > size}, the trailing ranges are empty.
   */
  public static List<ChunkRange> split(int size, int parts) {
    if (size < 0) {
      throw new IllegalArgumentException("size must be >= 0, was " + size);
    }
    if (parts < 1) {
      throw new IllegalArgumentException("parts must be >= 1, was " + parts);
    }

    int base = size / parts;
    int remainder = size % parts;

    List<ChunkRange> ranges = new ArrayList<>(parts);
    int start = 0;
    for (int i = 0; i < parts; i++) {
      int end = start + base + (i < remainder ? 1 : 0);
      ranges.add(new ChunkRange(start, end));
      start = end;
    }
    return List.copyOf(ranges);
  }
}
