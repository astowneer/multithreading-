package com.astowner.multithreading.lab1;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe accumulator that worker threads add their partial sums to, guarded by an explicit
 * {@link ReentrantLock} instead of the {@code synchronized} keyword.
 *
 * <p>The rule for every explicit lock: call {@code lock()} right before {@code try}, and
 * {@code unlock()} in {@code finally}, so the lock is released even if the guarded code throws.
 */
final class SharedSum {
  private final Lock lock = new ReentrantLock();
  private long sum;

  void add(long value) {
    lock.lock();
    try {
      sum += value;
    } finally {
      lock.unlock();
    }
  }

  long getSum() {
    lock.lock();
    try {
      return sum;
    } finally {
      lock.unlock();
    }
  }
}
