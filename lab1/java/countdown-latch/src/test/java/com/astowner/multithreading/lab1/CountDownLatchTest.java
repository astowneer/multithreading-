package com.astowner.multithreading.lab1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class CountDownLatchTest {

  @Test
  void sumTaskStoresItsPartialAndCountsDown() throws InterruptedException {
    int[] arr = {1, 2, 3, 4, 5};
    long[] partials = new long[2];
    CountDownLatch done = new CountDownLatch(2);

    new Thread(new SumTask(arr, new ChunkRange(0, 2), partials, 0, done)).start();
    new Thread(new SumTask(arr, new ChunkRange(2, 5), partials, 1, done)).start();

    assertTrue(done.await(5, TimeUnit.SECONDS), "workers did not finish");
    assertEquals(3, partials[0]);
    assertEquals(12, partials[1]);
  }

  @Test
  void awaitWithTimeoutGivesUpWhenTheCountIsNotReached() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(2);
    latch.countDown(); // only one of two

    assertFalse(latch.await(50, TimeUnit.MILLISECONDS));
    assertEquals(1, latch.getCount());
  }

  /**
   * The other classic use: a start gate. Every worker waits on the same latch, and one
   * countDown() releases them all at once, which is handy for starting a fair race or benchmark.
   */
  @Test
  void aLatchOfOneWorksAsAStartGate() throws InterruptedException {
    CountDownLatch startGate = new CountDownLatch(1);
    CountDownLatch finished = new CountDownLatch(3);

    for (int i = 0; i < 3; i++) {
      new Thread(() -> {
        try {
          startGate.await();
          finished.countDown();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }).start();
    }

    assertFalse(finished.await(50, TimeUnit.MILLISECONDS), "a worker ran before the gate opened");
    startGate.countDown();
    assertTrue(finished.await(5, TimeUnit.SECONDS));
  }
}
