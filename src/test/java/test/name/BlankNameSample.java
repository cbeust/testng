package test.name;

import org.testng.Assert;
import org.testng.ITest;
import org.testng.annotations.Test;

public class BlankNameSample implements ITest {

  @Test
  public void test() {
    Assert.assertTrue(true);
  }

  @Override
  public String getTestName() {
    return "";
  }

}
