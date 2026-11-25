package com.astowner.multithreading.lab1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SumTaskTest {

  @Test
  void sumsOnlyItsOwnRange() {
    int[] arr = {1, 2, 3, 4, 5};
    SumTask task = new SumTask(arr, new ChunkRange(1, 4));

    task.run();

    assertEquals(2 + 3 + 4, task.result());
  }

  @Test
  void resultOfAnotherThreadIsVisibleAfterJoin() throws InterruptedException {
    int[] arr = {10, 20, 30};
    SumTask task = new SumTask(arr, new ChunkRange(0, 3));

    Thread worker = new Thread(task);
    worker.start();
    worker.join(); // the happens-before edge that makes the plain field safe to read

    assertEquals(60, task.result());
  }
}
