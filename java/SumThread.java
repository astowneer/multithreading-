package lab1.java;

public class SumThread extends Thread {
  private final int startIndex;
  private final int endIndex;
  private final int[] arr;
  private final SharedSum sharedSum;

  public SumThread(int startIndex, int endIndex, int[] arr, SharedSum sharedSum) {
    this.startIndex = startIndex;
    this.endIndex = endIndex;
    this.arr = arr;
    this.sharedSum = sharedSum;
  }

  @Override
  public void run() {
    long localSum = 0;
    System.out.println("=".repeat(100));

    for (int i = startIndex; i < endIndex; i++) {
      localSum += this.arr[i];
    }

    System.out.println("SUM Threads " + localSum);

    this.sharedSum.add(localSum);
  }
}
