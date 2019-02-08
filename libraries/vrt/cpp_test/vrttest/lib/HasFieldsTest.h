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

#ifndef _HasFieldsTest_h
#define _HasFieldsTest_h

#include "HasFields.h"
#include "Testable.h"
#include "TestRunner.h"
#include "TestSet.h"
#include "Utilities.h"

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

namespace vrttest {
  /** Tests for the {@link HasFields} class. */
  class HasFieldsTest : public VRTObject, public virtual Testable {
    // Implement Testable
    public: virtual string getTestedClass ();
    public: virtual vector<TestSet> init ();
    public: virtual void done ();
    public: virtual void call (const string &func);

    // Test functions
    public: virtual void testGetFieldCount ();
    public: virtual void testGetField ();
    public: virtual void testGetField_idErrorA ();
    public: virtual void testGetField_idErrorB ();
    public: virtual void testGetField_errorAm1 ();
    public: virtual void testGetField_errorA0 ();
    public: virtual void testGetField_errorA1 ();
    public: virtual void testGetField_byNameErrorA ();
    public: virtual void testGetField_byNameErrorB ();
    public: virtual void testGetField_nameErrorAm1 ();
    public: virtual void testGetField_nameErrorA0 ();
    public: virtual void testGetField_nameErrorA1 ();
    public: virtual void testGetField_typeErrorAm1 ();
    public: virtual void testGetField_typeErrorA0 ();
    public: virtual void testGetField_typeErrorA1 ();
    public: virtual void testSetField ();
    public: virtual void testSetField_errorAm1 ();
    public: virtual void testSetField_errorA0 ();
    public: virtual void testSetField_errorA1 ();
    public: virtual void testSetField_byNameErrorA ();
    public: virtual void testSetField_byNameErrorB ();
  };

  /** Tests getFieldName/getFieldType/getFieldID.
   *  @param hf          The object to test.
   *  @param fieldOffset The field offset (from parent class).
   *  @param fieldNames  The field names.
   *  @param fieldTypes  The field types.
   *  @param numFields   The number of fields (i.e. length of fieldNames).
   */
  inline void checkGetFieldName (HasFields &hf, int32_t fieldOffset, const char **fieldNames,
                                 const ValueType *fieldTypes, int32_t numFields) {
    int32_t count = fieldOffset + numFields;

    assertEquals("getFieldCount()", count, hf.getFieldCount());
    for (int32_t i = 0; i < numFields; i++) {
      int32_t   fieldID    = i + fieldOffset;
      string    fieldIdent = Utilities::format("%d", fieldID);
      string    fieldName  = fieldNames[i];
      ValueType fieldType  = fieldTypes[i];

      assertEquals(string("getFieldName(")+fieldIdent+")", fieldName, hf.getFieldName(fieldID));
      assertEquals(string("getFieldType(")+fieldIdent+")", fieldType, hf.getFieldType(fieldID));
      assertEquals(string("getFieldID(")+fieldName+")",    fieldID,   hf.getFieldID(fieldName));
    }
  }

  /** Tests getField/getFieldByName/setField/setFieldByName with given method names.
   *  @param hf        The object to test.
   *  @param fieldName The field name (not quoted).
   *  @param T         The type.
   *  @param val1      The 1st value to set.
   *  @param val2      The 2nd value to set.
   *  @param getFunc   Name of the get function (not quoted).
   *  @param setFunc   Name of the set function (not quoted).
   */
#  define checkGetFieldAny11(hf, _fieldName, T, _val1, _val2, getFunc, setFunc, \
                             initVal1, initVal2, castA, castB) { \
    string     fieldName  = # _fieldName; \
    int32_t    fieldID    = hf.getFieldID(fieldName); \
    string     fieldIdent = Utilities::format("%d", fieldID); \
    Value     *val1       = initVal1; \
    Value     *val2       = initVal2; \
    T          exp1; \
    T          exp2; \
    Value     *actA; \
    Value     *actB; \
    \
    hf.setFunc(_val1); \
    exp1 = hf.getFunc(); \
    actA = hf.getField(fieldID); \
    actB = hf.getFieldByName(fieldName); \
    assertEquals(string("getField(")+fieldIdent+")",      exp1, castA); \
    assertEquals(string("getFieldByName(")+fieldName+")", exp1, castB); \
    delete actA; \
    delete actB; \
    \
    hf.setFunc(_val2); \
    exp2 = hf.getFunc(); \
    actA = hf.getField(fieldID); \
    actB = hf.getFieldByName(fieldName); \
    assertEquals(string("getField(")+fieldIdent+")",      exp2, castA); \
    assertEquals(string("getFieldByName(")+fieldName+")", exp2, castB); \
    delete actA; \
    delete actB; \
    \
    hf.setField(fieldID, val1); \
    actA = hf.getField(fieldID); \
    hf.setFieldByName(fieldName, val2); \
    actB = hf.getField(fieldID); \
    assertEquals(string("setField(")+fieldIdent+",val1)",      exp1, castA); \
    assertEquals(string("setFieldByName(")+fieldName+",val2)", exp2, castB); \
    delete actA; \
    delete actB; \
    \
    delete val1; \
    delete val2; \
  }

  /** Tests getField/getFieldByName/setField/setFieldByName with given method names.
   *  @param hf        The object to test.
   *  @param fieldName The field name (not quoted).
   *  @param T         The type.
   *  @param val1      The 1st value to set.
   *  @param val2      The 2nd value to set.
   *  @param getFunc   Name of the get function (not quoted).
   *  @param setFunc   Name of the set function (not quoted).
   */
#  define checkGetFieldDef7(hf, fieldName, T, val1, val2, getFunc, setFunc) \
    checkGetFieldAny11(hf, fieldName, T, val1, val2, getFunc, setFunc, \
                       new Value(val1), new Value(val2), actA->as<T>(), actB->as<T>())

  /** Tests getField/getFieldByName/setField/setFieldByName with normal methods.
   *  @param hf        The object to test.
   *  @param fieldName The field name (not quoted).
   *  @param T         The type.
   *  @param val1      The 1st value to set.
   *  @param val2      The 2nd value to set.
   */
#  define checkGetFieldDef5(hf, fieldName, T, val1, val2) \
    checkGetFieldDef7(hf, fieldName, T, val1, val2, get ## fieldName, set ## fieldName)

  /** Tests getField/getFieldByName/setField/setFieldByName with boolean methods.
   *  @param hf        The object to test.
   *  @param fieldName The field name (not quoted).
   *  @param T         The type.
   *  @param val1      The 1st value to set.
   *  @param val2      The 2nd value to set.
   */
#  define checkGetFieldBool(hf, fieldName, T, val1, val2) \
    checkGetFieldDef7(hf, fieldName, T, val1, val2, is ## fieldName, set ## fieldName)

  /** Tests getField/getFieldByName/setField/setFieldByName with enum methods.
   *  @param hf        The object to test.
   *  @param fieldName The field name (not quoted).
   *  @param T         The type.
   *  @param val1      The 1st value to set.
   *  @param val2      The 2nd value to set.
   */
#if (defined(__APPLE__) && defined(__MACH__))
#  define checkGetFieldEnum(hf, fieldName, T, val1, val2) \
    checkGetFieldAny11(hf, fieldName, int64_t, val1, val2, get ## fieldName, set ## fieldName, \
                       new Value((int64_t)val1), new Value((int64_t)val2), \
                       (int64_t)actA->as<int64_t>(), (int64_t)actB->as<int64_t>())
#else
#  define checkGetFieldEnum(hf, fieldName, T, val1, val2) \
    checkGetFieldAny11(hf, fieldName, T, val1, val2, get ## fieldName, set ## fieldName, \
                       new Value((int64_t)val1), new Value((int64_t)val2), \
                       (T)actA->as<int64_t>(), (T)actB->as<int64_t>())
#endif

  /** Tests getField/getFieldByName/setField/setFieldByName with given method names.
   *  @param hf        The object to test.
   *  @param fieldName The field name (not quoted).
   *  @param T         The type.
   *  @param val1      The 1st value to set.
   *  @param val2      The 2nd value to set.
   *  @param getFunc   Name of the get function (not quoted).
   *  @param setFunc   Name of the set function (not quoted).
   */
#  define checkGetFieldObj7(hf, fieldName, T, val1, val2, getFunc, setFunc) \
    checkGetFieldAny11(hf, fieldName, T, val1, val2, getFunc, setFunc, \
                       new Value(&val1), new Value(&val2), *actA->cast<T*>(), *actB->cast<T*>())

  /** Tests getField/getFieldByName/setField/setFieldByName with object methods.
   *  @param hf        The object to test.
   *  @param fieldName The field name (not quoted).
   *  @param T         The type.
   *  @param val1      The 1st value to set.
   *  @param val2      The 2nd value to set.
   */
#  define checkGetFieldObj5(hf, fieldName, T, val1, val2) \
    checkGetFieldObj7(hf, fieldName, T, val1, val2, get ## fieldName, set ## fieldName)

} END_NAMESPACE
#endif /* _HasFieldsTest_h */
