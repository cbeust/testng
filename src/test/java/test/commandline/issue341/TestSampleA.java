package test.commandline.issue341;

import org.testng.Reporter;
import org.testng.annotations.Test;

public class TestSampleA {

  @Test
  public void a() {
    Reporter.log(Long.toString(Thread.currentThread().getId()));
  }
}
