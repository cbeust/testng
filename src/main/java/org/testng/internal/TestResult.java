package org.testng.internal;

import org.testng.IAttributes;
import org.testng.IClass;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.collections.Objects;

import java.util.List;
import java.util.Set;


/**
 * This class represents the result of a test.
 *
 * @author Cedric Beust, May 2, 2004
 */
public class TestResult implements ITestResult {

  private static final long serialVersionUID = 6273017418233324556L;
  private IClass m_testClass = null;
  private ITestNGMethod m_method = null;
  private int m_status = -1;
  private Throwable m_throwable = null;
  private long m_startMillis = 0;
  private long m_endMillis = 0;
  private String m_name = null;
  private String m_host;
  transient private Object[] m_parameters = {};
  transient private Object m_instance;
  private String m_instanceName;
  private ITestContext m_context;

  public TestResult() {

  }

  public TestResult(IClass testClass,
      Object instance,
      ITestNGMethod method,
      Throwable throwable,
      long start,
      long end,
      ITestContext context)
  {
    init(testClass, instance, method, throwable, start, end, context);
  }

  /**
   *
   * @param testClass
   * @param instance
   * @param method
   * @param throwable
   * @param start
   * @param end
   */
  public void init (IClass testClass,
      Object instance,
      ITestNGMethod method,
      Throwable throwable,
      long start,
      long end,
      ITestContext context)
  {
    m_testClass = testClass;
    m_throwable = throwable;
    m_instanceName = m_testClass.getName();
    if (null == m_throwable) {
      m_status = ITestResult.SUCCESS;
    }
    m_startMillis = start;
    m_endMillis = end;
    m_method = method;
    m_context = context;

    m_instance = instance;

    // Calculate the name: either the method name, ITest#getTestName or
    // toString() if it's been overridden.
    if (m_instance == null) {
      m_name = m_method.getMethodName();
    } else {
      if (m_instance instanceof ITest) {
        m_name = ((ITest) m_instance).getTestName();
      }
      else {
        String string = m_instance.toString();
        // Only display toString() if it's been overridden by the user
        m_name = getMethod().getMethodName();
        try {
          if (!Object.class.getMethod("toString")
              .equals(m_instance.getClass().getMethod("toString"))) {
            m_instanceName = string.startsWith("class ")
                ? string.substring("class ".length())
                : string;
            m_name = m_name + " on " + m_instanceName;
          }
        }
        catch(NoSuchMethodException ignore) {
          // ignore
        }
      }
    }
  }

  private static void ppp(String s) {
    System.out.println("[TestResult] " + s);
  }

  @Override
  public void setEndMillis(long millis) {
    m_endMillis = millis;
  }

  /**
   * If this result's related instance implements ITest, returns its test name,
   * otherwise returns null.
   */
  @Override
  public String getTestName() {
    if (m_instance instanceof ITest) {
      return ((ITest) m_instance).getTestName();
    }
    return null;
  }

  @Override
  public String getName() {
    return m_name;
  }

  /**
   * @return Returns the method.
   */
  @Override
  public ITestNGMethod getMethod() {
    return m_method;
  }

  /**
   * @param method The method to set.
   */
  public void setMethod(ITestNGMethod method) {
    m_method = method;
  }

  /**
   * @return Returns the status.
   */
  @Override
  public int getStatus() {
    return m_status;
  }

  /**
   * @param status The status to set.
   */
  @Override
  public void setStatus(int status) {
    m_status = status;
  }

  @Override
  public boolean isSuccess() {
    return ITestResult.SUCCESS == m_status;
  }

  /**
   * @return Returns the testClass.
   */
  @Override
  public IClass getTestClass() {
    return m_testClass;
  }

  /**
   * @param testClass The testClass to set.
   */
  public void setTestClass(IClass testClass) {
    m_testClass = testClass;
  }

  /**
   * @return Returns the throwable.
   */
  @Override
  public Throwable getThrowable() {
    return m_throwable;
  }

  /**
   * @param throwable The throwable to set.
   */
  @Override
  public void setThrowable(Throwable throwable) {
    m_throwable = throwable;
  }

  /**
   * @return Returns the endMillis.
   */
  @Override
  public long getEndMillis() {
    return m_endMillis;
  }

  /**
   * @return Returns the startMillis.
   */
  @Override
  public long getStartMillis() {
    return m_startMillis;
  }

//  public List<String> getOutput() {
//    return m_output;
//  }

  @Override
  public String toString() {
    return toString(null);
  }

  protected String toStringSafe() {
    return toString("UNKNOWN");
  }

  private String toString(String defaultStatus) {
      List<String> output = Reporter.getOutput(this);
      String result = Objects.toStringHelper(getClass())
      .omitNulls()
      .omitEmptyStrings()
      .add("name", getName())
      .add("status", toString(m_status, defaultStatus))
      .add("method", m_method)
      .add("output", output != null && output.size() > 0 ? output.get(0) : null)
      .toString();

      return result;
  }

  private String toString(int status, String defaultResult) {
    switch(status) {
      case SUCCESS: return "SUCCESS";
      case FAILURE: return "FAILURE";
      case SKIP: return "SKIP";
      case SUCCESS_PERCENTAGE_FAILURE: return "SUCCESS WITHIN PERCENTAGE";
      case STARTED: return "STARTED";
      default:
        if (defaultResult == null) {
          throw new RuntimeException();
        } else {
          return defaultResult;
        }
    }
  }

  @Override
  public String getHost() {
    return m_host;
  }

  public void setHost(String host) {
    m_host = host;
  }

  @Override
  public Object[] getParameters() {
    return m_parameters;
  }

  @Override
  public void setParameters(Object[] parameters) {
    m_parameters = ParameterReference.convert(parameters);
  }

  @Override
  public Object getInstance() {
    return m_instance;
  }

  private IAttributes m_attributes = new Attributes();

  @Override
  public Object getAttribute(String name) {
    return m_attributes.getAttribute(name);
  }

  @Override
  public void setAttribute(String name, Object value) {
    m_attributes.setAttribute(name, value);
  }

  @Override
  public Set<String> getAttributeNames() {
    return m_attributes.getAttributeNames();
  }

  @Override
  public Object removeAttribute(String name) {
    return m_attributes.removeAttribute(name);
  }
  
  @Override
  public ITestContext getTestContext() {
	  return m_context;
  }
  
  public void setContext(ITestContext context) {
	  m_context = context;
  }

  @Override
  public int compareTo(ITestResult comparison) {
	  if( getStartMillis() > comparison.getStartMillis() ) {
		  return 1;
	  } else if( getStartMillis() < comparison.getStartMillis()) {
		  return -1;
	  } else {
		  return 0;
	  }
  }

  @Override
  public String getInstanceName() {
    return m_instanceName;
  }
}

