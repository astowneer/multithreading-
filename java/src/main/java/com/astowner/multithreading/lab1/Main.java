package com.astowner.multithreading.lab1;

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

  private static void populateArr(int numberOfThreads, int chunkSize, int[] arr) {
    FillThread[] fillThreads = new FillThread[numberOfThreads];

    for (int i = 0; i < numberOfThreads; i++) {
      int startIndex = i * chunkSize;
      int endIndex = (i == numberOfThreads - 1) ? arr.length : startIndex + chunkSize;
      fillThreads[i] =new FillThread(startIndex, endIndex, arr);
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

  private static SumThread[] createSumThreads(int numberOfThreads, int chunkSize, int dimension, int[] arr,
      SharedSum sharedSum) {
    SumThread[] threads = new SumThread[numberOfThreads];

    for (int i = 0; i < numberOfThreads; i++) {
      int startIndex = i * chunkSize;
      int endIndex = (i == numberOfThreads - 1) ? dimension : startIndex + chunkSize;
      threads[i] = new SumThread(startIndex, endIndex, arr, sharedSum);
    }

    return threads;
  }

  public static void main(String[] args) {
    int dimension = 1000000000;
    int numberOfThreads = 4;

    int[] arr = new int[dimension];

    SharedSum sharedSum = new SharedSum();

    int chunkSize = dimension / numberOfThreads;

    populateArr(numberOfThreads, chunkSize, arr);

    SumThread[] threads = createSumThreads(numberOfThreads, chunkSize, dimension, arr, sharedSum);
    findSumMultithreading(numberOfThreads, threads, sharedSum, arr);

    findSumNoThreads(arr);
  }
}
