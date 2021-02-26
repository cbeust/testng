package org.testng.reporters;

import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.collections.Maps;
import org.testng.collections.Sets;
import org.testng.internal.IResultListener2;
import org.testng.internal.Utils;
import org.testng.util.TimeUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Queue;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Pattern;

/**
 * A JUnit XML report generator (replacing the original JUnitXMLReporter that was based on XML
 * APIs).
 *
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
public class JUnitXMLReporter implements IResultListener2 {
  private static final Pattern ENTITY = Pattern.compile("&[a-zA-Z]+;.*");
  private static final Pattern LESS = Pattern.compile("<");
  private static final Pattern GREATER = Pattern.compile(">");
  private static final Pattern SINGLE_QUOTE = Pattern.compile("'");
  private static final Pattern QUOTE = Pattern.compile("\"");
  private static final Map<String, Pattern> ATTR_ESCAPES = Maps.newHashMap();

  static {
    ATTR_ESCAPES.put("&lt;", LESS);
    ATTR_ESCAPES.put("&gt;", GREATER);
    ATTR_ESCAPES.put("&apos;", SINGLE_QUOTE);
    ATTR_ESCAPES.put("&quot;", QUOTE);
  }

  private int m_numFailed = 0;
  private Queue<ITestResult> m_allTests = new ConcurrentLinkedDeque<>();
  private Queue<ITestResult> m_configIssues = new ConcurrentLinkedDeque<>();
  private Map<String, String> m_fileNameMap = Maps.newHashMap();
  private int m_fileNameIncrementer = 0;

  @Override
  public void onTestStart(ITestResult result) {}

  @Override
  public void beforeConfiguration(ITestResult tr) {}

  /** Invoked each time a test succeeds. */
  @Override
  public void onTestSuccess(ITestResult tr) {
    m_allTests.add(tr);
  }

  @Override
  public void onTestFailedButWithinSuccessPercentage(ITestResult tr) {
    m_allTests.add(tr);
  }

  /** Invoked each time a test fails. */
  @Override
  public void onTestFailure(ITestResult tr) {
    m_allTests.add(tr);
    m_numFailed++;
  }

  /** Invoked each time a test is skipped. */
  @Override
  public void onTestSkipped(ITestResult tr) {
    m_allTests.add(tr);
  }

  /** Invoked after the test class is instantiated and before any configuration method is called. */
  @Override
  public void onStart(ITestContext context) {}

  /** Invoked after all the tests have run and all their Configuration methods have been called. */
  @Override
  public void onFinish(ITestContext context) {
    generateReport(context);
    resetAll();
  }

  /** @see org.testng.IConfigurationListener#onConfigurationFailure(org.testng.ITestResult) */
  @Override
  public void onConfigurationFailure(ITestResult itr) {
    m_configIssues.add(itr);
  }

  /** @see org.testng.IConfigurationListener#onConfigurationSkip(org.testng.ITestResult) */
  @Override
  public void onConfigurationSkip(ITestResult itr) {
    m_configIssues.add(itr);
  }

  /** @see org.testng.IConfigurationListener#onConfigurationSuccess(org.testng.ITestResult) */
  @Override
  public void onConfigurationSuccess(ITestResult itr) {}

  /**
   * generate the XML report given what we know from all the test results
   *
   * @param context The test context
   */
  protected void generateReport(ITestContext context) {

    XMLStringBuffer document = new XMLStringBuffer();
    document.addComment("Generated by " + getClass().getName());

    Properties attrs = new Properties();
    attrs.setProperty(XMLConstants.ATTR_ERRORS, "0");
    attrs.setProperty(XMLConstants.ATTR_FAILURES, Integer.toString(m_numFailed));
    attrs.setProperty(
        XMLConstants.ATTR_IGNORED, Integer.toString(context.getExcludedMethods().size()));
    try {
      attrs.setProperty(XMLConstants.ATTR_HOSTNAME, InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException e) {
      // ignore
    }
    Set<String> packages = getPackages(context);
    if (packages.size() > 0) {
      attrs.setProperty(XMLConstants.ATTR_NAME, context.getCurrentXmlTest().getName());
      //        attrs.setProperty(XMLConstants.ATTR_PACKAGE, packages.iterator().next());
    }

    attrs.setProperty(XMLConstants.ATTR_TESTS, Integer.toString(m_allTests.size()));
    attrs.setProperty(
        XMLConstants.ATTR_TIME,
        Double.toString(
            (context.getEndDate().getTime() - context.getStartDate().getTime()) / 1000.0));

    attrs.setProperty(XMLConstants.ATTR_TIMESTAMP, formattedTime());

    document.push(XMLConstants.TESTSUITE, attrs);

    createElementFromTestResults(document, m_configIssues);
    createElementFromTestResults(document, m_allTests);
    createElementFromIgnoredTests(document, context);

    document.pop();
    Utils.writeUtf8File(
        context.getOutputDirectory(), generateFileName(context) + ".xml", document.toXML());
  }

  static String formattedTime() {
    return TimeUtils.formatTimeInLocalOrSpecifiedTimeZone(
        System.currentTimeMillis(), XMLReporterConfig.FMT_DEFAULT);
  }

  private synchronized void createElementFromTestResults(
      XMLStringBuffer document, Collection<ITestResult> results) {
    for (ITestResult tr : results) {
      createElement(document, tr);
    }
  }

  private synchronized void createElementFromIgnoredTests(
      XMLStringBuffer doc, ITestContext context) {
    Collection<ITestNGMethod> methods = context.getExcludedMethods();
    for (ITestNGMethod method : methods) {
      Properties properties = getPropertiesFor(method, 0);
      doc.push(XMLConstants.TESTCASE, properties);
      doc.addEmptyElement(XMLConstants.ATTR_IGNORED);
      doc.pop();
    }
  }

  private Properties getPropertiesFor(ITestNGMethod method, long elapsedTimeMillis) {
    Properties attrs = new Properties();
    String name = Utils.detailedMethodName(method, false);
    attrs.setProperty(XMLConstants.ATTR_NAME, name);
    attrs.setProperty(XMLConstants.ATTR_CLASSNAME, method.getRealClass().getName());
    attrs.setProperty(XMLConstants.ATTR_TIME, Double.toString(((double) elapsedTimeMillis) / 1000));
    return attrs;
  }

  private Set<String> getPackages(ITestContext context) {
    Set<String> result = Sets.newHashSet();
    for (ITestNGMethod m : context.getAllTestMethods()) {
      Package pkg = m.getRealClass().getPackage();
      if (pkg != null) {
        result.add(pkg.getName());
      }
    }
    return result;
  }

  private void createElement(XMLStringBuffer doc, ITestResult tr) {

    long elapsedTimeMillis = tr.getEndMillis() - tr.getStartMillis();
    Properties attrs = getPropertiesFor(tr.getMethod(), elapsedTimeMillis);
    if (tr.getMethod().isTest()) {
      attrs.setProperty(XMLConstants.ATTR_NAME, tr.getName());
    }

    if ((ITestResult.FAILURE == tr.getStatus()) || (ITestResult.SKIP == tr.getStatus())) {
      doc.push(XMLConstants.TESTCASE, attrs);

      if (ITestResult.FAILURE == tr.getStatus()) {
        createFailureElement(doc, tr);
      } else if (ITestResult.SKIP == tr.getStatus()) {
        createSkipElement(doc);
      }

      doc.pop();
    } else {
      doc.addEmptyElement(XMLConstants.TESTCASE, attrs);
    }
  }

  private void createFailureElement(XMLStringBuffer doc, ITestResult tr) {
    Properties attrs = new Properties();
    Throwable t = tr.getThrowable();
    if (t != null) {
      attrs.setProperty(XMLConstants.ATTR_TYPE, t.getClass().getName());
      String message = t.getMessage();
      if ((message != null) && (message.length() > 0)) {
        attrs.setProperty(XMLConstants.ATTR_MESSAGE, encodeAttr(message)); // ENCODE
      }
      doc.push(XMLConstants.FAILURE, attrs);
      doc.addCDATA(Utils.shortStackTrace(t, false));
      doc.pop();
    } else {
      doc.addEmptyElement(XMLConstants.FAILURE); // THIS IS AN ERROR
    }
  }

  private void createSkipElement(XMLStringBuffer doc) {
    doc.addEmptyElement(XMLConstants.SKIPPED);
  }

  private String encodeAttr(String attr) {
    String result = replaceAmpersand(attr, ENTITY);
    for (Map.Entry<String, Pattern> e : ATTR_ESCAPES.entrySet()) {
      result = e.getValue().matcher(result).replaceAll(e.getKey());
    }

    return result;
  }

  private String replaceAmpersand(String str, Pattern pattern) {
    int start = 0;
    int idx = str.indexOf('&', start);
    if (idx == -1) {
      return str;
    }
    StringBuilder result = new StringBuilder();
    while (idx != -1) {
      result.append(str.substring(start, idx));
      if (pattern.matcher(str.substring(idx)).matches()) {
        // do nothing it is an entity;
        result.append("&");
      } else {
        result.append("&amp;");
      }
      start = idx + 1;
      idx = str.indexOf('&', start);
    }
    result.append(str.substring(start));

    return result.toString();
  }

  /** Reset all member variables for next test. */
  private void resetAll() {
    m_allTests = new ConcurrentLinkedDeque<>();
    m_configIssues = new ConcurrentLinkedDeque<>();
    m_numFailed = 0;
  }

  /**
   * This method guarantees unique file names for reports.<br>
   * Also, this will guarantee that the old reports are overwritten when tests are run again.
   *
   * @param context test context
   * @return unique name for the file associated with this test context.
   */
  private String generateFileName(ITestContext context) {
    String fileName;
    String keyToSearch = context.getSuite().getName() + context.getName();
    if (m_fileNameMap.get(keyToSearch) == null) {
      fileName = context.getName();
    } else {
      fileName = context.getName() + m_fileNameIncrementer++;
    }

    m_fileNameMap.put(keyToSearch, fileName);
    return fileName;
  }
}
