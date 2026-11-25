package com.astowner.multithreading.lab1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Phaser;
import org.junit.jupiter.api.Test;

class PhaserTest {

  @Test
  void registeredPartiesAreCountedAndLeaveOnDeregister() {
    Phaser phaser = new Phaser(1);
    phaser.register();
    phaser.register();
    assertEquals(3, phaser.getRegisteredParties());

    phaser.arriveAndDeregister();
    assertEquals(2, phaser.getRegisteredParties());
    assertEquals(0, phaser.getPhase(), "phase 0 still waits for two more arrivals");
  }

  /**
   * A Phaser used like a reusable barrier with a phase counter. onAdvance runs between phases,
   * like a CyclicBarrier action, and returning true from it terminates the phaser.
   */
  @Test
  void workersMoveThroughPhasesTogetherUntilOnAdvanceStopsThem() throws InterruptedException {
    int workers = 3;
    int lastPhase = 2;
    List<String> log = new CopyOnWriteArrayList<>();
    Phaser phaser = new Phaser(workers) {
      @Override
      protected boolean onAdvance(int phase, int registeredParties) {
        log.add("end of phase " + phase);
        return phase == lastPhase; // true = terminate
      }
    };

    List<Thread> threads = new ArrayList<>();
    for (int w = 0; w < workers; w++) {
      threads.add(new Thread(() -> {
        while (!phaser.isTerminated()) {
          log.add("work in phase " + phaser.getPhase());
          phaser.arriveAndAwaitAdvance();
        }
      }));
    }
    threads.forEach(Thread::start);
    for (Thread t : threads) {
      t.join();
    }

    List<String> expected = new ArrayList<>();
    for (int phase = 0; phase <= lastPhase; phase++) {
      for (int w = 0; w < workers; w++) {
        expected.add("work in phase " + phase);
      }
      expected.add("end of phase " + phase);
    }
    assertEquals(expected, log);
    assertTrue(phaser.isTerminated());
  }
}
