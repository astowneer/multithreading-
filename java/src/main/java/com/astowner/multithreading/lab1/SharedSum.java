package com.astowner.multithreading.lab1;

public class SharedSum {
  private long sum = 0;

  public synchronized void add(long value) {
    this.sum += value;
  }

  public synchronized long getSum() {
    return this.sum;
  }
}
