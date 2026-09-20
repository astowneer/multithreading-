package com.astowner.multithreading.lab1;

import java.util.List;

public class Main {

  private static void findSumNoThreads(int[] arr) {
    long sum = 0;

    long startTime = System.nanoTime();

    for (int i = 0; i < arr.length; i++) {
      sum += arr[i];
    }

    long endTime = System.nanoTime();

    double elapsedSeconds = (endTime - startTime) / 1_000_000_000.0;
    System.out.println("No threads time (seconds): " + elapsedSeconds);
    System.out.println("SUM no threads: " + sum);
  }

  private static void findSumMultithreading(int numberOfThreads, SumThread[] threads, SharedSum sharedSum, int[] arr) {
    long startTime = System.nanoTime();
    for (int i = 0; i < numberOfThreads; i++) {
      threads[i].start();
    }

    for (int i = 0; i < numberOfThreads; i++) {
      try {
        threads[i].join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    long endTime = System.nanoTime();
    double elapsedSeconds = (endTime - startTime) / 1_000_000_000.0;
    System.out.println("Multithreaded time (seconds): " + elapsedSeconds);
    System.out.println("SUM multithreaded: " + sharedSum.getSum());
  }

  private static void populateArr(int numberOfThreads, int[] arr) {
    List<ChunkRange> ranges = ChunkRange.split(arr.length, numberOfThreads);
    FillThread[] fillThreads = new FillThread[numberOfThreads];

    for (int i = 0; i < numberOfThreads; i++) {
      ChunkRange range = ranges.get(i);
      fillThreads[i] = new FillThread(range.start(), range.end(), arr);
    }

    for (FillThread t : fillThreads) {
      t.start();
    }

    for (FillThread t : fillThreads) {
      try {
        t.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private static SumThread[] createSumThreads(int numberOfThreads, int[] arr, SharedSum sharedSum) {
    List<ChunkRange> ranges = ChunkRange.split(arr.length, numberOfThreads);
    SumThread[] threads = new SumThread[numberOfThreads];

    for (int i = 0; i < numberOfThreads; i++) {
      ChunkRange range = ranges.get(i);
      threads[i] = new SumThread(range.start(), range.end(), arr, sharedSum);
    }

    return threads;
  }

  public static void main(String[] args) {
    int dimension = 1000000000;
    int numberOfThreads = 4;

    int[] arr = new int[dimension];

    SharedSum sharedSum = new SharedSum();

    populateArr(numberOfThreads, arr);

    SumThread[] threads = createSumThreads(numberOfThreads, arr, sharedSum);
    findSumMultithreading(numberOfThreads, threads, sharedSum, arr);

    findSumNoThreads(arr);
  }
}
