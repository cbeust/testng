package test.retryAnalyzer.github1600;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class Github1600Analyzer implements IRetryAnalyzer {

  public static final String NO = "NO";
  static final String RETRY = "RETRY";
  static final String YES = "YES";
  private static final int MAX_RETRY_COUNT = 10;
  private static int retryCount = 0;

  @Override
  public boolean retry(ITestResult iTestResult) {
    String attribute = (String) iTestResult.getAttribute(RETRY);
    if (NO.equalsIgnoreCase(attribute)) {
      return false;
    } else if (YES.equalsIgnoreCase(attribute) || retryCount < MAX_RETRY_COUNT) {
      retryCount++;
      return true;
    }
    return false;
  }
}
