package test.thread.parallelization.issue1773;

import org.testng.Reporter;
import org.testng.annotations.Test;

public class PriorityTestSample2 {

  @Test(priority = 1)
  public void FirstTest() {
    log();
  }

  @Test(priority = 2)
  public void SecondTest() {
    log();
  }

  @Test(priority = 3)
  public void ThridTest() {
    log();
  }

  private void log() {
    Reporter.log(Long.toString(Thread.currentThread().getId()));
  }

}
