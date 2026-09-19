package lab1.java;

import java.util.concurrent.ThreadLocalRandom;

public class FillThread extends Thread {
  private final int startIndex;
  private final int endIndex;
  private final int[] arr;

  public FillThread(int startIndex, int endIndex, int[] arr) {
    this.startIndex = startIndex;
    this.endIndex = endIndex;
    this.arr = arr;
  }

  @Override
  public void run() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = startIndex; i < endIndex; i++) {
      arr[i] = random.nextInt(100);
    }
  }
}
