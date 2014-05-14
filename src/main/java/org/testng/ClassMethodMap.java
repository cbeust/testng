package org.testng;

import org.testng.internal.RunInfo;
import org.testng.collections.Lists;
import org.testng.collections.Maps;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class maintains a map of <CODE><Class, List<ITestNGMethod>></CODE>.
 * It is used by TestWorkers to determine if the method they just ran
 * is the last of its class, in which case it's time to invoke all the
 * afterClass methods.
 *
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
public class ClassMethodMap {
  private Map<Object, List<ITestNGMethod>> m_classMap = Maps.newHashMap();
  // These two variables are used throughout the workers to keep track
  // of what beforeClass/afterClass methods have been invoked
  private Map<ITestClass, Set<Object>> m_beforeClassMethods = Maps.newHashMap();
  private Map<ITestClass, Set<Object>> m_afterClassMethods = Maps.newHashMap();

  public ClassMethodMap(List<ITestNGMethod> methods, RunInfo runInfo) {
    for (ITestNGMethod m : methods) {
      // Using RunInfo here is consistent with method selection in TestMethodWorker
      //See https://github.com/cbeust/testng/issues/440
      if (! runInfo.includeMethod(m, true)) continue;

      Object instance = m.getInstance();
      List<ITestNGMethod> l = m_classMap.get(instance);
      if (l == null) {
        l = Lists.newArrayList();
        m_classMap.put(instance, l);
      }
      l.add(m);
    }
  }

  /**
   * Remove the method from this map and returns true if it is the last
   * of its class.
   */
  public synchronized boolean removeAndCheckIfLast(ITestNGMethod m, Object instance) {
    List<ITestNGMethod> l = m_classMap.get(instance);
    if (l != null) {
      l.remove(m);
      // It's the last method of this class if all the methods remaining in the list belong to a
      // different class
      for (ITestNGMethod tm : l) {
        if (tm.getEnabled() && tm.getTestClass().equals(m.getTestClass())) return false;
      }
      return true;
    } else {
      throw new AssertionError("l should not be null");
    }
  }

  private Class<?> getMethodClass(ITestNGMethod m) {
    return m.getTestClass().getRealClass();
  }

  public Map<ITestClass, Set<Object>> getInvokedBeforeClassMethods() {
    return m_beforeClassMethods;
  }

  public Map<ITestClass, Set<Object>> getInvokedAfterClassMethods() {
    return m_afterClassMethods;
  }

  public void clear() {
    for(Set<Object> instances: m_beforeClassMethods.values()) {
      instances.clear();
      instances= null;
    }
    for(Set<Object> instances: m_afterClassMethods.values()) {
      instances.clear();
      instances= null;
    }
    m_beforeClassMethods.clear();
    m_afterClassMethods.clear();
  }
}
