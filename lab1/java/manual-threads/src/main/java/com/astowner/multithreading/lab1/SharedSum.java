package com.astowner.multithreading.lab1;

/** Thread-safe accumulator that worker threads add their partial sums to. */
final class SharedSum {
  private long sum;

  synchronized void add(long value) {
    sum += value;
  }

  synchronized long getSum() {
    return sum;
  }
}
