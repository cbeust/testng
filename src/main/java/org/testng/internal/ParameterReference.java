package org.testng.internal;

import java.util.concurrent.*;
import java.lang.ref.*;

public class ParameterReference {
  private static ConcurrentHashMap<String, String> parameterSummaries = new ConcurrentHashMap<String, String>();
  private static int OMIT_THRESHOLD = 4096;
  private static int MAX_NOTE_LENGTH = 30;

  private Object strongReference = null;
  private SoftReference softReference;
  private String summary;
  private boolean tmp = false;

  protected ParameterReference(Object parameter)
  {
    try {
      if (parameter instanceof TestResult) {
        summary = ((TestResult) parameter).toStringSafe();
      } else {
        summary = Utils.toString(parameter);
      }
    } catch (Exception ignored) {
      strongReference = parameter;
    }
    softReference = new SoftReference(parameter);
    if (summary == null) {
      return;
    }
    int length = summary.length();
    if (length > OMIT_THRESHOLD) {
      StringBuilder sb = new StringBuilder(OMIT_THRESHOLD + MAX_NOTE_LENGTH);
      sb.append(summary, 0, OMIT_THRESHOLD);
      sb.append(" ");
      sb.append(length - OMIT_THRESHOLD);
      sb.append(" more bytes omitted");
      summary = sb.toString();
    }
    String other = parameterSummaries.putIfAbsent(summary, summary);
    if (other != null) {
      summary = other;
    }
  }

  public String toString()
  {
    Object reference = softReference.get();
    if (reference != null) {
      return reference.toString();
    } else {
      return summary;
    }
  }

  protected static ParameterReference[] convert(Object ... parameters)
  {
    if (parameters == null) {
      return null;
    }
    ParameterReference[] result = new ParameterReference[parameters.length];
    for (int idx = 0, size = parameters.length; idx < size; ++idx) {
      result[idx] = new ParameterReference(parameters[idx]);
    }
    return result;
  }
}
