package org.testng.reporters;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.collections.Lists;
import org.testng.collections.Maps;
import org.testng.collections.Sets;
import org.testng.internal.LiteWeightTestNGMethod;
import org.testng.internal.MethodHelper;
import org.testng.internal.RuntimeBehavior;
import org.testng.internal.Systematiser;
import org.testng.internal.Utils;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This reporter is responsible for creating testng-failed.xml
 *
 * @author <a href="mailto:cedric@beust.com">Cedric Beust</a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class FailedReporter implements IReporter {
  public static final String TESTNG_FAILED_XML = "testng-failed.xml";

  private XmlSuite m_xmlSuite;

  public FailedReporter() {}

  public FailedReporter(XmlSuite xmlSuite) {
    m_xmlSuite = xmlSuite;
  }

  @Override
  public void generateReport(
      List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
    for (ISuite suite : suites) {
      generateFailureSuite(suite.getXmlSuite(), suite, outputDirectory);
    }
  }

  protected void generateFailureSuite(XmlSuite xmlSuite, ISuite suite, String outputDir) {
    XmlSuite failedSuite = xmlSuite.shallowCopy();
    failedSuite.setName("Failed suite [" + xmlSuite.getName() + "]");
    m_xmlSuite = failedSuite;

    Map<String, ISuiteResult> results = suite.getResults();

    for (Map.Entry<String, ISuiteResult> entry : results.entrySet()) {
      ISuiteResult suiteResult = entry.getValue();
      ITestContext testContext = suiteResult.getTestContext();

      generateXmlTest(
          testContext.getCurrentXmlTest(),
          testContext,
          testContext.getFailedTests().getAllResults(),
          testContext.getSkippedTests().getAllResults());
    }

    if (null != failedSuite.getTests() && failedSuite.getTests().size() > 0) {
      Utils.writeUtf8File(outputDir, TESTNG_FAILED_XML, failedSuite.toXml());
      Utils.writeUtf8File(suite.getOutputDirectory(), TESTNG_FAILED_XML, failedSuite.toXml());
    }
  }

  private void generateXmlTest(
      XmlTest xmlTest,
      ITestContext context,
      Set<ITestResult> failedTests,
      Set<ITestResult> skippedTests) {
    // Note:  we can have skipped tests and no failed tests
    // if a method depends on nonexistent groups
    if (skippedTests.size() > 0 || failedTests.size() > 0) {
      Set<ITestNGMethod> methodsToReRun = Sets.newHashSet();

      // Get the transitive closure of all the failed methods and the methods
      // they depend on
      Set<ITestResult> allTests = Sets.newHashSet();
      allTests.addAll(failedTests);
      allTests.addAll(skippedTests);
      ITestNGMethod[] allTestMethods = context.getAllTestMethods();
      for (ITestResult failedTest : allTests) {
        ITestNGMethod current = failedTest.getMethod();
        if (!current.isTest()) { // Don't count configuration methods
          continue;
        }
        methodsToReRun.add(current);
        List<ITestNGMethod> methodsDependedUpon =
            MethodHelper.getMethodsDependedUpon(current, allTestMethods, Systematiser.getComparator());

        for (ITestNGMethod m : methodsDependedUpon) {
          if (m.isTest()) {
            methodsToReRun.add(m);
          }
        }
      }

      //
      // Now we have all the right methods.  Go through the list of
      // all the methods that were run and only pick those that are
      // in the methodToReRun map.  Since the methods are already
      // sorted, we don't need to sort them again.
      //
      List<ITestNGMethod> result = Lists.newArrayList();
      Set<ITestNGMethod> relevantConfigs = Sets.newHashSet();
      for (ITestNGMethod m : allTestMethods) {
        if (RuntimeBehavior.isMemoryFriendlyMode()) {
          //We are doing this because the `m` would not be of type
          // LiteWeightTestNGMethod and hence the hashCode() and equals()
          // computation would be different.
          m = new LiteWeightTestNGMethod(m);
        }
        if (methodsToReRun.contains(m)) {
          result.add(m);
          getAllApplicableConfigs(relevantConfigs, m.getTestClass());
        }
      }
      result.addAll(relevantConfigs);
      createXmlTest(context, result, xmlTest);
    }
  }

  private static void getAllApplicableConfigs(Set<ITestNGMethod> configs, ITestClass iTestClass) {
    configs.addAll(Arrays.asList(iTestClass.getBeforeSuiteMethods()));
    configs.addAll(Arrays.asList(iTestClass.getAfterSuiteMethods()));
    configs.addAll(Arrays.asList(iTestClass.getBeforeTestConfigurationMethods()));
    configs.addAll(Arrays.asList(iTestClass.getAfterTestConfigurationMethods()));
    configs.addAll(Arrays.asList(iTestClass.getBeforeTestMethods()));
    configs.addAll(Arrays.asList(iTestClass.getAfterTestMethods()));
    configs.addAll(Arrays.asList(iTestClass.getBeforeClassMethods()));
    configs.addAll(Arrays.asList(iTestClass.getAfterClassMethods()));
  }

  /** Generate testng-failed.xml */
  private void createXmlTest(
      ITestContext context, List<ITestNGMethod> methods, XmlTest srcXmlTest) {
    XmlTest xmlTest = new XmlTest(m_xmlSuite);
    xmlTest.setName(context.getName() + "(failed)");
    xmlTest.setScript(srcXmlTest.getScript());
    xmlTest.setIncludedGroups(srcXmlTest.getIncludedGroups());
    xmlTest.setExcludedGroups(srcXmlTest.getExcludedGroups());
    xmlTest.setParallel(srcXmlTest.getParallel());
    xmlTest.setParameters(srcXmlTest.getLocalParameters());
    xmlTest.setJUnit(srcXmlTest.isJUnit());
    List<XmlClass> xmlClasses = createXmlClasses(methods, srcXmlTest);
    xmlTest.setXmlClasses(xmlClasses);
  }

  /**
   * @param methods The methods we want to represent
   * @param srcXmlTest The {@link XmlTest} object that represents the source.
   * @return A list of XmlClass objects (each representing a <class> tag) based on the parameter
   *     methods
   */
  private List<XmlClass> createXmlClasses(List<ITestNGMethod> methods, XmlTest srcXmlTest) {
    List<XmlClass> result = Lists.newArrayList();
    Map<Class<?>, Set<ITestNGMethod>> methodsMap = Maps.newHashMap();

    for (ITestNGMethod m : methods) {
      Object instances = m.getInstance();
      Class<?> clazz = instances == null ? m.getRealClass() : instances.getClass();
      Set<ITestNGMethod> methodList = methodsMap.computeIfAbsent(clazz, k -> Sets.newHashSet());
      methodList.add(m);
    }

    // Store parameters per XmlClass

    Map<String,  Map<String,  String>> classParameters = Maps.newHashMap();
    for (XmlClass c : srcXmlTest.getClasses()) {
      classParameters.put(c.getName(),c.getLocalParameters());
    }

    int index = 0;
    for (Map.Entry<Class<?>, Set<ITestNGMethod>> entry : methodsMap.entrySet()) {
      Class<?> clazz = entry.getKey();
      Set<ITestNGMethod> methodList = entry.getValue();
      // @author Borojevic
      // Need to check all the methods, not just @Test ones.
      XmlClass xmlClass = new XmlClass(clazz.getName(), index++, false /* don't load classes */);
      List<XmlInclude> methodNames = Lists.newArrayList(methodList.size());
      int ind = 0;
      for (ITestNGMethod m : methodList) {

        XmlInclude methodName =
            new XmlInclude(m.getMethodName(), m.getFailedInvocationNumbers(), ind++);
        methodName.setParameters(findMethodLocalParameters(srcXmlTest, m));
        methodName.setConstructorOrMethod(m.getConstructorOrMethod());
        methodNames.add(methodName);
      }

      List<XmlInclude> groupedMethodNames = new ArrayList<>();
      methodNames.stream()
              .collect(Collectors.groupingBy(XmlInclude::getConstructorOrMethod))
              .forEach((method, methodNameList) -> {
                XmlInclude tmpMethodName = methodNameList.get(0);
                if (methodNameList.size() > 1) {
                  methodNameList.forEach(methodName -> {
                    tmpMethodName.addInvocationNumbers(methodName.getInvocationNumbers());
                  });
                }
                groupedMethodNames.add(tmpMethodName);
              });

      xmlClass.setIncludedMethods(groupedMethodNames);
      xmlClass.setParameters(classParameters.getOrDefault(xmlClass.getName(), classParameters
              .values()
              .stream()
              .flatMap(map -> map.entrySet().stream())
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2)-> v2))));
      result.add(xmlClass);
    }

    return result;
  }

  /**
   * Get local parameters of one include method from origin test xml.
   *
   * @param srcXmlTest The {@link XmlTest} object that represents the source.
   * @param method the method we want to find its parameters
   * @return local parameters belong to one test method.
   */
  private static Map<String, String> findMethodLocalParameters(
      XmlTest srcXmlTest, ITestNGMethod method) {
    Class<?> clazz = method.getRealClass();

    for (XmlClass c : srcXmlTest.getClasses()) {
      if (clazz == c.getSupportClass()) {
        for (XmlInclude xmlInclude : c.getIncludedMethods()) {
          if (xmlInclude.getName().equals(method.getMethodName())) {
            return xmlInclude.getLocalParameters();
          }
        }
      }
    }

    return Collections.emptyMap();
  }
}
