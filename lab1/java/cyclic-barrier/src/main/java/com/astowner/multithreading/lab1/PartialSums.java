package com.astowner.multithreading.lab1;

/**
 * One slot per worker for its partial sum, plus the total. {@link #combine()} is used as the
 * barrier action: the barrier runs it exactly once, after every worker has stored its slot.
 */
final class PartialSums {
  private final long[] partials;
  private long total;
  private boolean combined;

  PartialSums(int parts) {
    this.partials = new long[parts];
  }

  /** Called by worker {@code index} before it reaches the barrier. */
  void set(int index, long value) {
    partials[index] = value;
  }

  /** The barrier action. Runs on the last thread to arrive, before any thread is released. */
  void combine() {
    long sum = 0;
    for (long partial : partials) {
      sum += partial;
    }
    total = sum;
    combined = true;
  }

  /** The total. Only valid after the workers were joined. */
  long total() {
    if (!combined) {
      throw new IllegalStateException("the barrier never tripped, so the total was not computed");
    }
    return total;
  }
}
