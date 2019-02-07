/* ===================== COPYRIGHT NOTICE =====================
 * This file is protected by Copyright. Please refer to the COPYRIGHT file
 * distributed with this source distribution.
 *
 * This file is part of REDHAWK.
 *
 * REDHAWK is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * REDHAWK is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * ============================================================
 */

package nxm.vrttest.lib;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import nxm.vrt.lib.HasFields;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static nxm.vrttest.inc.TestRunner.assertEquals;

/** Test cases for the {@link HasFields} class. */
public class HasFieldsTest<T extends HasFields> implements Testable {
  private final Class<T> testedClass;

  /** Creates a new instance using the given implementation class. */
  public HasFieldsTest (Class<T> testedClass) {
    this.testedClass = testedClass;
  }

  @Override
  public Class<?> getTestedClass () {
    return testedClass;
  }

  @Override
  public String toString () {
    return "Tests for "+testedClass;
  }

  @Override
  public List<TestSet> init () throws Exception {
    return Collections.emptyList(); // all functions are abstract
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }

  /** Tests getFieldName/getFieldType/getFieldID.
   *  @param hf          The object to test.
   *  @param fieldOffset The field offset (from parent class).
   *  @param fieldNames  The field names.
   *  @param fieldTypes  The field types.
   */
  public static void checkGetFieldName (HasFields hf, int fieldOffset, String[] fieldNames, Class<?>[] fieldTypes) {
    int count = fieldOffset + fieldNames.length;

    assertEquals("getFieldCount()", count, hf.getFieldCount());
    for (int i = 0; i < fieldNames.length; i++) {
      int      fieldID   = i + fieldOffset;
      String   fieldName = fieldNames[i];
      Class<?> fieldType = fieldTypes[i];

      assertEquals("getFieldName("+fieldID+")", fieldName, hf.getFieldName(fieldID));
      assertEquals("getFieldType("+fieldID+")", fieldType, hf.getFieldType(fieldID));
      assertEquals("getFieldID("+fieldName+")", fieldID,   hf.getFieldID(fieldName));
    }
  }

  /** Tests getField/getFieldByName/setField/setFieldByName with default method names.
   *  @param hf        The object to test.
   *  @param fieldName The field name.
   *  @param val1      The 1st value to set.
   *  @param val2      The 2nd value to set.
   */
  public static void checkGetField (HasFields hf, String fieldName, Object val1, Object val2) {
    checkGetField(hf, fieldName, val1, val2, null, null);
  }

  /** Tests getField/getFieldByName/setField/setFieldByName with given method names.
   *  @param hf        The object to test.
   *  @param fieldName The field name.
   *  @param val1      The 1st value to set.
   *  @param val2      The 2nd value to set.
   *  @param getName   Name of the set function.
   *  @param setName   Name of the set function.
   */
  public static void checkGetField (HasFields hf, String fieldName, Object val1, Object val2,
                                    String getName, String setName) {
    int      fieldID   = hf.getFieldID(fieldName);
    Class<?> fieldType = hf.getFieldType(fieldID);
    Method   get       = null;
    Method   set       = null;
    Object   exp1;
    Object   exp2;

    try {
      get = getGetMethod(hf, getName, fieldName);
      set = getSetMethod(hf, setName, fieldName, fieldType);

      set.invoke(hf, val1);
      exp1 = get.invoke(hf);
      assertEquals("getField("+fieldID+")",         exp1, hf.getField(fieldID));
      assertEquals("getFieldByName("+fieldName+")", exp1, hf.getFieldByName(fieldName));

      set.invoke(hf, val2);
      exp2 = get.invoke(hf);
      assertEquals("getField("+fieldID+")",         exp2, hf.getField(fieldID));
      assertEquals("getFieldByName("+fieldName+")", exp2, hf.getFieldByName(fieldName));

      hf.setField(fieldID, val1);
      assertEquals("setField("+fieldID+",val1)",         exp1, get.invoke(hf));

      hf.setFieldByName(fieldName, val2);
      assertEquals("setFieldByName("+fieldName+",val2)", exp2, get.invoke(hf));
    }
    catch (IllegalArgumentException e) {
      if (e.getMessage().toLowerCase().contains("argument type mismatch")) {
        String c1 = (val1 == null)? "null" : val1.getClass().getName();
        String c2 = (val2 == null)? "null" : val2.getClass().getName();

        if (c1.equals(c2)) {
          throw new RuntimeException("Could not access '"+set+"' when given value is "
                                    +"of type "+c1, e);
        }
        else {
          throw new RuntimeException("Could not access '"+set+"' when given value is "
                                    +"of type "+c1+" or "+c2, e);
        }
      }
      throw e;
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException("Could not access '"+get+"' or '"+set+"' in "+hf.getClass(), e);
    }
    catch (InvocationTargetException e) {
      Throwable t = e.getCause();
      if (t instanceof RuntimeException) throw (RuntimeException)t;
      throw new RuntimeException("Could not invoke '"+get+"' or '"+set+"' in "+hf.getClass(), t);
    }
  }

  /** Gets the "get" method. */
  private static Method getGetMethod (HasFields hf, String getName, String fieldName) {
    if (getName != null) {
      try {
        return hf.getClass().getMethod(getName);
      }
      catch (NoSuchMethodException e) {
        throw new RuntimeException("Could not find "+getName+"() in "+hf.getClass(), e);
      }
    }

    // If name isn't specified, try getFoo() and isFoo()
    try {
      return hf.getClass().getMethod("get"+fieldName);
    }
    catch (NoSuchMethodException e) {
      try {
        return hf.getClass().getMethod("is"+fieldName);
      }
      catch (NoSuchMethodException ex) {
        throw new RuntimeException("Could not find get"+fieldName+"() or is"+fieldName+"() in "+hf.getClass(), e);
      }
    }
  }

  /** Gets the "set" method. */
  private static Method getSetMethod (HasFields hf, String setName, String fieldName, Class<?> type) {
    if (setName == null) setName = "set"+fieldName;

    try {
      return hf.getClass().getMethod(setName, type);
    }
    catch (NoSuchMethodException e) {
      // Try swapping TYPE and Class (e.g. "int" vs "Integer") or
      // class vs interface (e.g. "String" vs "CharSequence")
      Class<?> t = null;
      try {
             if (type == Byte.TYPE      ) t = Byte.class;
        else if (type == Short.TYPE     ) t = Short.class;
        else if (type == Integer.TYPE   ) t = Integer.class;
        else if (type == Long.TYPE      ) t = Long.class;
        else if (type == Float.TYPE     ) t = Float.class;
        else if (type == Double.TYPE    ) t = Double.class;
        else if (type == Boolean.TYPE   ) t = Boolean.class;
        else if (type == Character.TYPE ) t = Character.class;
        else if (type == Byte.class     ) t = Byte.TYPE;
        else if (type == Short.class    ) t = Short.TYPE;
        else if (type == Integer.class  ) t = Integer.TYPE;
        else if (type == Long.class     ) t = Long.TYPE;
        else if (type == Float.class    ) t = Float.TYPE;
        else if (type == Double.class   ) t = Double.TYPE;
        else if (type == Boolean.class  ) t = Boolean.TYPE;
        else if (type == Character.class) t = Character.TYPE;
        else if (type == String.class   ) t = CharSequence.class;

        if (t == null) {
          throw new RuntimeException("Could not find "+setName+"("+type.getName()+") "
                                    +"in "+hf.getClass(), e);
        }
        return hf.getClass().getMethod(setName, t);
      }
      catch (NoSuchMethodException ex) {
        throw new RuntimeException("Could not find "+setName+"("+type+") or "
                                  +setName+"("+t.getName()+") in "+hf.getClass(), e);
      }
    }
  }
}
