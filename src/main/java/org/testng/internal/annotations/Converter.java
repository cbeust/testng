package org.testng.internal.annotations;

import org.testng.collections.Lists;
import org.testng.internal.ClassHelper;
import org.testng.internal.Utils;

import java.util.List;
import java.util.StringTokenizer;

/**
 * Convert a string values into primitive types.
 *
 * Created on Dec 20, 2005
 * @author cbeust
 */
public class Converter {

  public Converter(){

  }

  public Converter(List<ClassLoader> classLoaders) {

  }
  public static boolean  getBoolean(String tagValue, boolean def) {
    boolean result = def;
    if (tagValue != null) {
      result = Boolean.valueOf(tagValue).booleanValue();
    }
    return result;
  }

  public static int getInt(String tagValue, int def) {
    int result = def;
    if (tagValue != null) {
      result = new Integer(tagValue).intValue();
    }
    return result;
  }

  public static String getString(String tagValue, String def) {
    String result = def;
    if (tagValue != null) {
      result = tagValue;
    }
    return result;
  }

  public static long getLong(String tagValue, long def) {
    long result = def;
    if (tagValue != null) {
      result = new Long(tagValue).longValue();
    }
    return result;
  }

  public static String[] getStringArray(String tagValue, String[] def) {
    String[] result = def;
    if (tagValue != null) {
      result = Utils.stringToArray(tagValue);
    }

    return result;
  }

  public static Class[] getClassArray(String tagValue, Class[] def) {
    Class[] result = def;
    List vResult = Lists.newArrayList();
    if (tagValue != null) {
      StringTokenizer st = new StringTokenizer(tagValue, " ,");
      while (st.hasMoreElements()) {
        String className = (String) st.nextElement();
        try {
          Class cls = Class.forName(className);
          vResult.add(cls);
        }
        catch (ClassNotFoundException e) {
          e.printStackTrace();
        }
      }
      result = (Class[]) vResult.toArray(new Class[vResult.size()]);
    }

    return result;
  }

  public static Class getClass(String namedParameter) {
    Class result = null;
    if (namedParameter != null) {
      result = ClassHelper.forName(namedParameter, null);
    }

    return result;
  }

}
