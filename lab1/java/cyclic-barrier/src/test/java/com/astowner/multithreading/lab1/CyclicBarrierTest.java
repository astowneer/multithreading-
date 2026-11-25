package com.astowner.multithreading.lab1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class CyclicBarrierTest {

  @Test
  void totalIsUnavailableIfTheBarrierNeverTripped() {
    assertThrows(IllegalStateException.class, () -> new PartialSums(2).total());
  }

  /**
   * "Cyclic" means the barrier resets after it trips. Three workers go through three rounds; the
   * barrier action runs once per round, and no worker starts round r+1 before all finished r.
   */
  @Test
  void theSameBarrierIsReusedForSeveralRounds() throws InterruptedException {
    int workers = 3;
    int rounds = 3;
    List<String> log = new CopyOnWriteArrayList<>();
    CyclicBarrier barrier = new CyclicBarrier(workers, () -> log.add("round done"));

    List<Thread> threads = new ArrayList<>();
    for (int w = 0; w < workers; w++) {
      threads.add(new Thread(() -> {
        try {
          for (int r = 0; r < rounds; r++) {
            log.add("work " + r);
            barrier.await();
          }
        } catch (InterruptedException | BrokenBarrierException e) {
          throw new IllegalStateException(e);
        }
      }));
    }
    threads.forEach(Thread::start);
    for (Thread t : threads) {
      t.join();
    }

    List<String> expected = new ArrayList<>();
    for (int r = 0; r < rounds; r++) {
      for (int w = 0; w < workers; w++) {
        expected.add("work " + r);
      }
      expected.add("round done");
    }
    assertEquals(expected, log);
  }

  @Test
  void aTimeoutBreaksTheBarrierForEveryone() {
    CyclicBarrier barrier = new CyclicBarrier(2); // the second party never comes

    assertThrows(TimeoutException.class, () -> barrier.await(50, TimeUnit.MILLISECONDS));
    assertTrue(barrier.isBroken());
    assertThrows(BrokenBarrierException.class, barrier::await);
  }
}
