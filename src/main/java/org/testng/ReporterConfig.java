package org.testng;

import org.testng.collections.Lists;
import org.testng.internal.ClassHelper;
import org.testng.internal.PropertyUtils;
import org.testng.internal.Utils;

import java.util.List;

/**
 * Stores the information regarding the configuration of a pluggable report listener. Used also
 * in conjunction with the &lt;reporter&gt; sub-element of the Ant task
 *
 * NOTE: this class needs to be public. It's used by TestNG Ant task
 *
 * @author Cosmin Marginean, Apr 12, 2007
 */
public class ReporterConfig {

  /**
   * The class name of the reporter listener
   */
  private String m_className;

  /**
   * The properties of the reporter listener
   */
  private List<Property> m_properties = Lists.newArrayList();

  public void addProperty(Property property) {
    m_properties.add(property);
  }

  public List<Property> getProperties() {
    return m_properties;
  }

  public String getClassName() {
    return m_className;
  }

  public void setClassName(String className) {
    this.m_className = className;
  }

  public String serialize() {
    StringBuffer sb = new StringBuffer();
    sb.append(m_className);
    if (!m_properties.isEmpty()) {
      sb.append(":");

      for (int i = 0; i < m_properties.size(); i++) {
        ReporterConfig.Property property = m_properties.get(i);
        sb.append(property.getName());
        sb.append("=");
        sb.append(property.getValue());
        if (i < m_properties.size() - 1) {
          sb.append(",");
        }
      }
    }
    return sb.toString();
  }

  public static ReporterConfig deserialize(String inputString) {
    ReporterConfig reporterConfig = null;
    if (!Utils.isStringEmpty(inputString)) {
      reporterConfig = new ReporterConfig();
      int clsNameEndIndex = inputString.indexOf(":");
      if (clsNameEndIndex == -1) {
        reporterConfig.setClassName(inputString);
      } else {
        reporterConfig.setClassName(inputString.substring(0, clsNameEndIndex));
        String propString = inputString.substring(clsNameEndIndex + 1, inputString.length());
        String[] props = propString.split(",");
        if ((props != null) && (props.length > 0)) {
          for (String prop : props) {
            String[] propNameAndVal = prop.split("=");
            if ((propNameAndVal != null) && (propNameAndVal.length == 2)) {
              Property property = new Property();
              property.setName(propNameAndVal[0]);
              property.setValue(propNameAndVal[1]);
              reporterConfig.addProperty(property);
            }
          }
        }
      }

    }
    return reporterConfig;
  }

  /**
   * Creates a reporter based on the current configuration
   */
  public Object newReporterInstance() {
    Object result = null;
    Class reporterClass = ClassHelper.forName(m_className, null);
    if (reporterClass != null) {
      result = ClassHelper.newInstance(reporterClass);
      for (ReporterConfig.Property property : m_properties) {
        PropertyUtils.setProperty(result, property.getName(), property.getValue());
      }
    }
    return result;
  }

  public static class Property {
    private String name;
    private String value;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("\nClass = " + m_className);
    for (Property prop : m_properties) {
      buf.append("\n\t " + prop.getName() + "=" + prop.getValue());
    }
    return buf.toString();
  }
}
