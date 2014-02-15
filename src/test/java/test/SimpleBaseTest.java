package test;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.collections.Lists;
import org.testng.internal.Utils;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.io.File;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

public class SimpleBaseTest implements Serializable{
  /**
	 * 
	 */
	private static final long serialVersionUID = 445256363660810698L;
	
// System property specifying where the resources (e.g. xml files) can be found
  private static final String TEST_RESOURCES_DIR = "test.resources.dir";

  public static TestNG create() {
    TestNG result = new TestNG();
    result.setUseDefaultListeners(false);
    result.setVerbose(0);
    return result;
  }

  public static TestNG create(Class<?> testClass) {
    TestNG result = create();
    result.setTestClasses(new Class[] { testClass});
    return result;
  }

  protected TestNG create(Class<?>... testClasses) {
    TestNG result = create();
    result.setTestClasses(testClasses);
    return result;
  }

  protected XmlSuite createXmlSuite(String name) {
    XmlSuite result = new XmlSuite();
    result.setName(name);
    return result;
  }

  protected XmlTest createXmlTest(XmlSuite suite, String name, String... classes) {
    XmlTest result = new XmlTest(suite);
    int index = 0;
    result.setName(name);
    for (String c : classes) {
      XmlClass xc = new XmlClass(c, index++, true /* load classes */);
      result.getXmlClasses().add(xc);
    }

    return result;
  }

  protected void addMethods(XmlClass cls, String... methods) {
    int index = 0;
    for (String m : methods) {
      XmlInclude include = new XmlInclude(m, index++);
      cls.getIncludedMethods().add(include);
    }
  }

  protected String getPathToResource(String fileName) {
    String result = System.getProperty(TEST_RESOURCES_DIR);
    if (result == null) {
      Utils.log("SimpleBaseTest", 2,  "Warning: System property " + TEST_RESOURCES_DIR
          + " was not defined.");
      return "target/test-classes/" + fileName;
    }
    else {
      return result + File.separatorChar + fileName;
    }
  }

  protected void verifyPassedTests(TestListenerAdapter tla, String... methodNames) {
    Iterator<ITestResult> it = tla.getPassedTests().iterator();
    Assert.assertEquals(tla.getPassedTests().size(), methodNames.length);

    int i = 0;
    while (it.hasNext()) {
      Assert.assertEquals(it.next().getName(), methodNames[i++]);
    }
  }

  /**
   * Compare a list of ITestResult with a list of String method names,
   */
  public static void assertTestResultsEqual(List<ITestResult> results, List<String> methods) {
    List<String> resultMethods = Lists.newArrayList();
    for (ITestResult r : results) {
      resultMethods.add(r.getMethod().getMethodName());
    }
    Assert.assertEquals(resultMethods, methods);
  }

}
