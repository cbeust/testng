package org.testng.xml;

import org.testng.TestNGException;
import org.testng.collections.Lists;
import org.testng.collections.Maps;
import org.testng.collections.Objects;
import org.testng.internal.ClassHelper;
import org.testng.reporters.XMLStringBuffer;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This class describes the tag <class> in testng.xml.
 *
 * @author <a href="mailto:cedric@beust.com">Cedric Beust</a>
 */
public class XmlClass implements Serializable, Cloneable {

  private static final long serialVersionUID = 8885360896966149897L;
  private List<XmlInclude> m_includedMethods = Lists.newArrayList();
  private List<String> m_excludedMethods = Lists.newArrayList();
  private String m_name = null;
  private Class m_class = null;
  /** The index of this class in the <test> tag */
  private int m_index;
  /** True if the classes need to be loaded */
  private boolean m_loadClasses = true;
  private Map<String, String> m_parameters = Maps.newHashMap();
  private XmlTest m_xmlTest;

  public XmlClass() {
    init("", null, 0, false /* load classes */);
  }

  public XmlClass(String name) {
    init(name, null, 0);
  }

  public XmlClass(String name, boolean loadClasses) {
    init(name, null, 0, loadClasses);
  }

  public XmlClass(Class cls) {
    init(cls.getName(), cls, 0, true);
  }

  public XmlClass(Class cls, boolean loadClasses) {
    init(cls.getName(), cls, 0, loadClasses);
  }

  public XmlClass(String className, int index) {
    init(className, null, index, true /* load classes */);
  }

  public XmlClass(String className, int index, boolean loadClasses) {
    init(className, null, index, loadClasses);
  }

  private void init(String className, Class cls, int index) {
    init(className, cls, index, true /* load classes */);
  }

  private void init(String className, Class cls, int index,
      boolean resolveClass) {
    m_name = className;
    m_class = cls;
    m_index = index;

    if (null == m_class && resolveClass) {
      loadClass();
    }
  }

  private void loadClass() {
    m_class = ClassHelper.forName(m_name, null);

    if (null == m_class) {
      throw new TestNGException("Cannot find class in classpath: " + m_name);
    }
  }

  /**
   * @return Returns the className.
   */
  public Class getSupportClass() {
    if (m_class == null) loadClass();
    return m_class;
  }

  /**
   * @param className The className to set.
   */
  public void setClass(Class className) {
    m_class = className;
  }

  /**
   * @return Returns the excludedMethods.
   */
  public List<String> getExcludedMethods() {
    return m_excludedMethods;
  }

  /**
   * @param excludedMethods The excludedMethods to set.
   */
  public void setExcludedMethods(List<String> excludedMethods) {
    m_excludedMethods = excludedMethods;
  }

  /**
   * @return Returns the includedMethods.
   */
  public List<XmlInclude> getIncludedMethods() {
    return m_includedMethods;
  }

  /**
   * @param includedMethods The includedMethods to set.
   */
  public void setIncludedMethods(List<XmlInclude> includedMethods) {
    m_includedMethods = includedMethods;
  }

  /**
   * @return Returns the name.
   */
  public String getName() {
    return m_name;
  }

  /**
   * @param name The name to set.
   */
  public void setName(String name) {
    m_name = name;
  }

  /**
   * @return true if the classes need to be loaded.
   */
  public boolean loadClasses() {
    return m_loadClasses;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(getClass())
        .add("class", m_name)
        .toString();
  }

  public String toXml(String indent) {
    XMLStringBuffer xsb = new XMLStringBuffer(indent);
    Properties prop = new Properties();
    prop.setProperty("name", getName());

    boolean hasMethods = !m_includedMethods.isEmpty() || !m_excludedMethods.isEmpty();
    boolean hasParameters = !m_parameters.isEmpty();
    if (hasParameters || hasMethods) {
      xsb.push("class", prop);
      XmlUtils.dumpParameters(xsb, m_parameters);

      if (hasMethods) {
        xsb.push("methods");
  
        for (XmlInclude m : getIncludedMethods()) {
          xsb.getStringBuffer().append(m.toXml(indent + "    "));
        }
  
        for (String m: getExcludedMethods()) {
          Properties p= new Properties();
          p.setProperty("name", m);
          xsb.addEmptyElement("exclude", p);
        }
  
        xsb.pop("methods");
      }

      xsb.pop("class");
    }
    else {
      xsb.addEmptyElement("class", prop);
    }

    return xsb.toXML();

  }

  public static String listToString(List<Integer> invocationNumbers) {
    StringBuilder result = new StringBuilder();
    int i = 0;
    for (Integer n : invocationNumbers) {
      if (i++ > 0) {
        result.append(" ");
      }
      result.append(n);
    }
    return result.toString();
  }

  /**
   * Clone an XmlClass by copying all its components.
   */
  @Override
  public Object clone() {
    XmlClass result = new XmlClass(getName(), getIndex(), loadClasses());
    result.setExcludedMethods(getExcludedMethods());
    result.setIncludedMethods(getIncludedMethods());

    return result;
  }

  /**
   * Note that this attribute does not come from the XML file, it's calculated
   * internally and represents the order in which this class was found in its
   * &lt;test&gt; tag.  It's used to calculate the ordering of the classes
   * when preserve-order is true.
   */
  public int getIndex() {
    return m_index;
  }

  public void setIndex(int index) {
    m_index = index;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((m_class == null) ? 0 : m_class.hashCode());
    result = prime * result + (m_loadClasses ? 1 : 0);
    result = prime * result
        + ((m_excludedMethods == null) ? 0 : m_excludedMethods.hashCode());
    result = prime * result
        + ((m_includedMethods == null) ? 0 : m_includedMethods.hashCode());
    result = prime * result + m_index;
    result = prime * result + ((m_name == null) ? 0 : m_name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null)
      return XmlSuite.f();
    if (getClass() != obj.getClass())
      return XmlSuite.f();
    XmlClass other = (XmlClass) obj;
    if (m_class == null) {

      if (other.m_class != null) {
        return false;
      }
    } else if (m_class != null && other.m_class != null && !m_class.equals(other.m_class)) {
      return false;
    } else if (other.m_loadClasses != m_loadClasses) {
      return false;
    } else if (!m_excludedMethods.equals(other.m_excludedMethods)) {
      return false;
    }
    if (m_includedMethods == null) {
      if (other.m_includedMethods != null)
        return XmlSuite.f();
    } else if (!m_includedMethods.equals(other.m_includedMethods))
      return XmlSuite.f();
//    if (m_index != other.m_index)
//      return XmlSuite.f();
    if (m_name == null) {
      if (other.m_name != null)
        return XmlSuite.f();
    } else if (!m_name.equals(other.m_name))
      return XmlSuite.f();

    return true;
  }

  public void setParameters(Map<String, String> parameters) {
    m_parameters.clear();
    m_parameters.putAll(parameters);
  }

  /**
   * @return The parameters defined in this test tag and the tags above it.
   */
  public Map<String, String> getAllParameters() {
    Map<String, String> result = Maps.newHashMap();
    Map<String, String> parameters = m_xmlTest.getLocalParameters();
    for (Map.Entry<String, String> parameter : parameters.entrySet()) {
      result.put(parameter.getKey(), parameter.getValue());
    }
    for (String key : m_parameters.keySet()) {
      result.put(key, m_parameters.get(key));
    }
    return result;
  }

  /**
   * @return The parameters defined in this tag, and only this test tag. To retrieve
   * the inherited parameters as well, call {@code getAllParameters()}.
   */
  public Map<String, String> getLocalParameters() {
    return m_parameters;
  }

  /**
   * @deprecated Use {@code getLocalParameters()} or {@code getAllParameters()}
   */
  @Deprecated
  public Map<String, String> getParameters() {
    return getAllParameters();
  }

  public void setXmlTest(XmlTest test) {
    m_xmlTest = test;
  }
}
