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

  private static void findSumMultithreading(int numberOfThreads, Thread[] threads, SharedSum sharedSum, int[] arr) {
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
    Thread[] fillThreads = new Thread[numberOfThreads];

    for (int i = 0; i < numberOfThreads; i++) {
      fillThreads[i] = new Thread(new FillTask(arr, ranges.get(i)));
    }

    for (Thread t : fillThreads) {
      t.start();
    }

    for (Thread t : fillThreads) {
      try {
        t.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private static Thread[] createSumThreads(int numberOfThreads, int[] arr, SharedSum sharedSum) {
    List<ChunkRange> ranges = ChunkRange.split(arr.length, numberOfThreads);
    Thread[] threads = new Thread[numberOfThreads];

    for (int i = 0; i < numberOfThreads; i++) {
      threads[i] = new Thread(new SumTask(arr, ranges.get(i), sharedSum));
    }

    return threads;
  }

  public static void main(String[] args) {
    int dimension = 1000000000;
    int numberOfThreads = 4;

    int[] arr = new int[dimension];

    SharedSum sharedSum = new SharedSum();

    populateArr(numberOfThreads, arr);

    Thread[] threads = createSumThreads(numberOfThreads, arr, sharedSum);
    findSumMultithreading(numberOfThreads, threads, sharedSum, arr);

    findSumNoThreads(arr);
  }
}
