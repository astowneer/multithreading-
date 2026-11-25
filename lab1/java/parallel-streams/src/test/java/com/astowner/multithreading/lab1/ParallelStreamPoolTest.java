package com.astowner.multithreading.lab1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ParallelStreamPoolTest {

  @Test
  void commonPoolVersionGivesTheSameSum() {
    int[] arr = IntStream.range(0, 1_000_003).map(i -> i % 100).toArray();

    assertEquals(ArraySum.sequential(arr), ArraySum.parallelOnCommonPool(arr));
  }

  @Test
  void streamStartedInsideAPoolRunsOnlyOnThatPool()
      throws InterruptedException, ExecutionException {
    Set<ForkJoinPool> poolsUsed = ConcurrentHashMap.newKeySet();

    try (ForkJoinPool pool = new ForkJoinPool(3)) {
      pool.submit(() -> IntStream.range(0, 100_000).parallel()
              .peek(i -> poolsUsed.add(currentPool()))
              .sum())
          .get();

      assertEquals(Set.of(pool), poolsUsed, "some elements ran outside the dedicated pool");
    }
  }

  @Test
  void streamStartedOutsideAPoolUsesTheCommonPool() {
    Set<ForkJoinPool> poolsUsed = ConcurrentHashMap.newKeySet();

    IntStream.range(0, 100_000).parallel().peek(i -> poolsUsed.add(currentPool())).sum();

    // The calling thread also takes part, and it belongs to no pool, so NO_POOL may appear.
    poolsUsed.remove(NO_POOL);
    assertTrue(poolsUsed.isEmpty() || poolsUsed.equals(Set.of(ForkJoinPool.commonPool())));
  }

  private static final ForkJoinPool NO_POOL = new ForkJoinPool(1);

  private static ForkJoinPool currentPool() {
    return Thread.currentThread() instanceof ForkJoinWorkerThread worker ? worker.getPool() : NO_POOL;
  }
}
