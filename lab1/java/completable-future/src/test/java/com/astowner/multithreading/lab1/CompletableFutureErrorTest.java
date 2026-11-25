package com.astowner.multithreading.lab1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

/** Shows how a failure in one stage travels through a thenCombine pipeline. */
class CompletableFutureErrorTest {

  @Test
  void failureInOnePartFailsTheCombinedFuture() {
    CompletableFuture<Long> ok = CompletableFuture.supplyAsync(() -> 1L);
    CompletableFuture<Long> broken = CompletableFuture.supplyAsync(() -> {
      throw new ArithmeticException("boom");
    });

    CompletableFuture<Long> total = ok.thenCombine(broken, Long::sum);

    CompletionException e = assertThrows(CompletionException.class, total::join);
    assertInstanceOf(ArithmeticException.class, e.getCause());
  }

  @Test
  void exceptionallyCanRecoverWithAFallback() {
    CompletableFuture<Long> broken = CompletableFuture.supplyAsync(() -> {
      throw new ArithmeticException("boom");
    });

    assertEquals(-1L, broken.exceptionally(error -> -1L).join());
  }
}
