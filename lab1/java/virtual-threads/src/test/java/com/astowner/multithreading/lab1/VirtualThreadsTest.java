package com.astowner.multithreading.lab1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VirtualThreadsTest {

  @ParameterizedTest
  @ValueSource(ints = {1, 3, 7, 1_000})
  void executorVersionMatchesSequential(int threads) throws InterruptedException {
    int[] arr = IntStream.range(0, 100_003).map(i -> i % 100).toArray();

    assertEquals(ArraySum.sequential(arr), ArraySum.parallelWithExecutor(arr, threads));
  }

  @Test
  void runAllStartsEveryTaskOnANamedVirtualThread() throws InterruptedException {
    Set<String> names = ConcurrentHashMap.newKeySet();
    AtomicInteger platformThreads = new AtomicInteger();
    Runnable record = () -> {
      names.add(Thread.currentThread().getName());
      if (!Thread.currentThread().isVirtual()) {
        platformThreads.incrementAndGet();
      }
    };

    ArraySum.runAll("probe", List.of(record, record, record));

    assertEquals(0, platformThreads.get(), "a task ran on a platform thread");
    assertEquals(Set.of("probe-0", "probe-1", "probe-2"), names);
  }

  /**
   * Ten thousand threads that each sleep for 100 ms. With platform threads this needs ten
   * thousand OS threads; virtual threads unmount from their carrier while sleeping, so a handful
   * of carriers run all of them and the whole batch takes about 100 ms, not 10,000 x 100 ms.
   */
  @Test
  void tenThousandBlockingTasksFinishQuickly() {
    AtomicInteger done = new AtomicInteger();
    long start = System.nanoTime();

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < 10_000; i++) {
        executor.submit(() -> {
          Thread.sleep(Duration.ofMillis(100));
          return done.incrementAndGet();
        });
      }
    }

    Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
    assertEquals(10_000, done.get());
    assertTrue(elapsed.compareTo(Duration.ofSeconds(10)) < 0, "took " + elapsed);
  }
}
