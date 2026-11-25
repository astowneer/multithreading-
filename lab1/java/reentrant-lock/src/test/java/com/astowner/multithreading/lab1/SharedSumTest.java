package com.astowner.multithreading.lab1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SharedSumTest {

  @Test
  void concurrentAddsAreNotLost() throws InterruptedException {
    // 8 threads x 100_000 adds each. Without the lock, `sum += value` is read-add-write, so two
    // threads can read the same old value and one of the additions disappears.
    SharedSum total = new SharedSum();
    int threads = 8;
    int addsPerThread = 100_000;

    List<Thread> workers = new ArrayList<>();
    for (int t = 0; t < threads; t++) {
      workers.add(new Thread(() -> {
        for (int i = 0; i < addsPerThread; i++) {
          total.add(1);
        }
      }));
    }
    workers.forEach(Thread::start);
    for (Thread worker : workers) {
      worker.join();
    }

    assertEquals((long) threads * addsPerThread, total.getSum());
  }
}
