package com.astowner.multithreading.lab1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class SumTaskTest {

  @Test
  void callSumsOnlyItsOwnRange() {
    int[] arr = {1, 2, 3, 4, 5};

    assertEquals(2 + 3 + 4, new SumTask(arr, new ChunkRange(1, 4)).call());
  }

  @Test
  void resultComesBackThroughTheFuture() throws InterruptedException, ExecutionException {
    int[] arr = {10, 20, 30};

    try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
      Future<Long> future = pool.submit(new SumTask(arr, new ChunkRange(0, 3)));

      assertEquals(60, future.get());
    }
  }
}
