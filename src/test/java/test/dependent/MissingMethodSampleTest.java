package test.dependent;

import java.io.ByteArrayInputStream;
import org.testng.TestNG;
import org.testng.annotations.Test;
import org.testng.xml.Parser;

public class MissingMethodSampleTest {

  public static void main(String[] args) throws Exception {
    TestNG tng = new TestNG();
    String xml =
        "<suite name=\"dgf\" verbose=\"10\"><test name=\"dgf\"><classes>"
            + "<class name=\"test.dependent.MissingMethodSampleTest\"/>"
            + "</classes></test></suite>";
    System.out.println(xml);
    ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());
    tng.setXmlSuites(new Parser(is).parseToList());
    tng.run();
  }

  @Test(dependsOnMethods = "missingMethod", ignoreMissingDependencies = true)
  public void explicitlyIgnoreMissingMethod() {
  }

  @Test(dependsOnMethods = "missingMethod", alwaysRun = true)
  public void alwaysRunDespiteMissingMethod() {
  }
}
