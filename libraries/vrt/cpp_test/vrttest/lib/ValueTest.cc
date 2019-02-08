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

#include "ValueTest.h"
#include "TestRunner.h"
#include "TimeStamp.h"
#include "UUID.h"
#include "Value.h"
#include <errno.h>

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

string ValueTest::getTestedClass () {
  return "vrt::Value";
}

vector<TestSet> ValueTest::init () {
  vector<TestSet> tests;
  tests.push_back(TestSet("Value()",                        this, "testValue_void"));
  tests.push_back(TestSet("Value(int8_t)",                  this, "testValue_int8_t"));
  tests.push_back(TestSet("Value(int16_t)",                 this, "testValue_int16_t"));
  tests.push_back(TestSet("Value(int32_t)",                 this, "testValue_int32_t"));
  tests.push_back(TestSet("Value(int64_t)",                 this, "testValue_int64_t"));
  tests.push_back(TestSet("Value(float)",                   this, "testValue_float"));
  tests.push_back(TestSet("Value(double)",                  this, "testValue_double"));
  tests.push_back(TestSet("Value(bool)",                    this, "testValue_bool"));
  tests.push_back(TestSet("Value(boolNull)",                this, "testValue_boolNull"));
  tests.push_back(TestSet("Value(const string&)",           this, "+testValue_string"));
  tests.push_back(TestSet("Value(const wstring&)",          this, "+testValue_wstring"));
  tests.push_back(TestSet("Value(string*,   ...)",          this, "testValue_string"));
  tests.push_back(TestSet("Value(wstring*,  ...)",          this, "testValue_wstring"));
  tests.push_back(TestSet("Value(VRTObject*,...)",          this, "testValue_VRTObject"));
  tests.push_back(TestSet("Value(vector<T>*,...)",          this, "+"));
  tests.push_back(TestSet("as<T>()",                        this, "+"));
  tests.push_back(TestSet("at(..)",                         this, "+"));
  tests.push_back(TestSet("cast<T>()",                      this, "+"));
  tests.push_back(TestSet("equals(..)",                     this, "+"));
  tests.push_back(TestSet("getType()",                      this, "+"));
  tests.push_back(TestSet("isNullValue()",                  this, "+"));
  tests.push_back(TestSet("size()",                         this, "+"));
  tests.push_back(TestSet("toString()",                     this, "+"));

  return tests;
}

void ValueTest::done () {
  // nothing to do
}

void ValueTest::call (const string &func) {
  if (func == "testValue_void"            ) testValue_void();
  if (func == "testValue_int8_t"          ) testValue_int8_t();
  if (func == "testValue_int16_t"         ) testValue_int16_t();
  if (func == "testValue_int32_t"         ) testValue_int32_t();
  if (func == "testValue_int64_t"         ) testValue_int64_t();
  if (func == "testValue_float"           ) testValue_float();
  if (func == "testValue_double"          ) testValue_double();
  if (func == "testValue_bool"            ) testValue_bool();
  if (func == "testValue_boolNull"        ) testValue_boolNull();
  if (func == "testValue_string"          ) testValue_string();
  if (func == "testValue_wstring"         ) testValue_wstring();
  if (func == "testValue_VRTObject"       ) testValue_VRTObject();
}









/** Converts value type to string. */
inline static string typeToString (ValueType type) {
  ostringstream str;
  str << type;
  return str.str();
}

template <typename N>
static void testBoolValue (ValueType type, N expVal, Value *val, int32_t idx) {
  string typeStr = typeToString(type);
  if (idx >= 0) {
    char txt[32];
    snprintf(txt, 40, "%s[] (i=%d)", typeStr.c_str(), idx);
    typeStr = txt;
  }

  ostringstream str;
  str << expVal;

  int8_t   expInt8     = INT8_NULL;
  int16_t  expInt16    = INT16_NULL;
  int32_t  expInt32    = INT32_NULL;
  int64_t  expInt64    = INT64_NULL;
  float    expFloat    = FLOAT_NAN;
  double   expDouble   = DOUBLE_NAN;
  bool     expBool     = false;
  boolNull expBoolNull = _NULL;
  bool     expIsNull   = (isNull(expVal))? true   : false;
  string   expString   = (isNull(expVal))? "null" : str.str();

  if (!isNull(expVal) && ((type == ValueType_Bool) || (type == ValueType_BoolNull))) {
    bool ok = (type == ValueType_Bool)? (bool)expVal : ((int)expVal > 0);

    // The type casts below are required to avoid warnings under the Intel compiler
    expInt8     = (int8_t )((ok)? 1 : 0);
    expInt16    = (int16_t)((ok)? 1 : 0);
    expInt32    = (int32_t)((ok)? 1 : 0);
    expInt64    = (int64_t)((ok)? 1 : 0);
    expFloat    = (float  )((ok)? 1 : 0);
    expDouble   = (double )((ok)? 1 : 0);
    expBool     = (ok)?  true :  false;
    expBoolNull = (ok)? _TRUE : _FALSE;
  }

  assertEquals(typeStr+" val->getType()",       type,           val->getType());
  assertEquals(typeStr+" val->as<N>()",         expVal,         val->as<N       >());
  assertEquals(typeStr+" val->as<int8_t>()",    expInt8,        val->as<int8_t  >());
  assertEquals(typeStr+" val->as<int16_t>()",   expInt16,       val->as<int16_t >());
  assertEquals(typeStr+" val->as<int32_t>()",   expInt32,       val->as<int32_t >());
  assertEquals(typeStr+" val->as<int64_t>()",   expInt64,       val->as<int64_t >());
  assertEquals(typeStr+" val->as<float>()",     expFloat,       val->as<float   >());
  assertEquals(typeStr+" val->as<double>()",    expDouble,      val->as<double  >());
  assertEquals(typeStr+" val->as<bool>()",      expBool,        val->as<bool    >());
  assertEquals(typeStr+" val->as<boolNull>()",  expBoolNull,    val->as<boolNull>());
  assertEquals(typeStr+" val->as<string>()",    expString,      val->as<string  >());
  assertEquals(typeStr+" val->toString()",      expString,      val->toString());
  assertEquals(typeStr+" val->isNullValue()",   expIsNull,      val->isNullValue());
  assertEquals(typeStr+" val->size()",          Value::npos,    val->size());
}

template <typename T, typename N> // T=Comparison Type  /  N=Access Type
static void testObjectValue (ValueType type, N expVal, const string &expString,
                             bool expIsNull, bool copy, Value *val, int32_t idx) {
  string typeStr = typeToString(type);
  if (idx >= 0) {
    char txt[32];
    snprintf(txt, 40, "%s[] (i=%d)", typeStr.c_str(), idx);
    typeStr = txt;
  }

  int8_t   expInt8     = INT8_NULL;
  int16_t  expInt16    = INT16_NULL;
  int32_t  expInt32    = INT32_NULL;
  int64_t  expInt64    = INT64_NULL;
  float    expFloat    = FLOAT_NAN;
  double   expDouble   = DOUBLE_NAN;
  bool     expBool     = false;
  boolNull expBoolNull = _NULL;
  wstring  expWString; // Simply a wchar_t version of expString since we only use ASCII

  for (size_t i = 0; i < expString.size(); i++) {
    expWString += expString[i];
  }

  if (copy) {
    assertEquals(typeStr+" val->as<N>()",      *dynamic_cast<T>(expVal),
                                               *dynamic_cast<T>(val->as<N>()));
  }
  else {
    assertEquals(typeStr+" val->as<N>()",       dynamic_cast<T>(expVal),
                                                dynamic_cast<T>(val->as<N>()));
  }
  assertEquals(typeStr+" val->getType()",       type,           val->getType());
  assertEquals(typeStr+" val->as<int8_t>()",    expInt8,        val->as<int8_t  >());
  assertEquals(typeStr+" val->as<int16_t>()",   expInt16,       val->as<int16_t >());
  assertEquals(typeStr+" val->as<int32_t>()",   expInt32,       val->as<int32_t >());
  assertEquals(typeStr+" val->as<int64_t>()",   expInt64,       val->as<int64_t >());
  assertEquals(typeStr+" val->as<float>()",     expFloat,       val->as<float   >());
  assertEquals(typeStr+" val->as<double>()",    expDouble,      val->as<double  >());
  assertEquals(typeStr+" val->as<bool>()",      expBool,        val->as<bool    >());
  assertEquals(typeStr+" val->as<boolNull>()",  expBoolNull,    val->as<boolNull>());
  assertEquals(typeStr+" val->as<string>()",    expString,      val->as<string  >());
  assertEquals(typeStr+" val->as<wstring>()",   expWString,     val->as<wstring >());
  assertEquals(typeStr+" val->toString()",      expString,      val->toString());
  assertEquals(typeStr+" val->isNullValue()",   expIsNull,      val->isNullValue());
  assertEquals(typeStr+" val->size()",          Value::npos,    val->size());
}

template <typename N>
static void testNumericValue (ValueType type, N num, Value *val, int32_t idx) {
  string typeStr = typeToString(type);
  if (idx >= 0) {
    char txt[32];
    snprintf(txt, 40, "%s[] (i=%d)", typeStr.c_str(), idx);
    typeStr = txt;
  }

  if (isNull(num)) {
    assertEquals(typeStr+" val->getType()",       type,          val->getType());
    assertEquals(typeStr+" val->as<N>()",         num,           val->as<N       >());
    assertEquals(typeStr+" val->as<int8_t>()",    INT8_NULL,     val->as<int8_t  >());
    assertEquals(typeStr+" val->as<int16_t>()",   INT16_NULL,    val->as<int16_t >());
    assertEquals(typeStr+" val->asint32_t<>()",   INT32_NULL,    val->as<int32_t >());
    assertEquals(typeStr+" val->as<int64_t>()",   INT64_NULL,    val->as<int64_t >());
    assertEquals(typeStr+" val->as<float>()",     FLOAT_NAN,     val->as<float   >());
    assertEquals(typeStr+" val->as<double>()",    DOUBLE_NAN,    val->as<double  >());
    assertEquals(typeStr+" val->as<bool>()",      false,         val->as<bool    >());
    assertEquals(typeStr+" val->as<boolNull>()",  _NULL,         val->as<boolNull>());
    assertEquals(typeStr+" val->as<string>()",    "null",        val->as<string  >());
    assertEquals(typeStr+" val->toString()",      "null",        val->toString());
    assertEquals(typeStr+" val->isNullValue()",   true,          val->isNullValue());
    assertEquals(typeStr+" val->size()",          Value::npos,   val->size());
  }
  else {
    ostringstream str;
    str << num;

    // Need to disable Intel compiler warning 1572 (floating-point equality and
    // inequality comparisons are unreliable) since it will otherwise flag the
    // expBool check even though it is a correct.
    _Intel_Pragma("warning push")
    _Intel_Pragma("warning disable 1572")
    bool     expBool     = (num != 0);
    boolNull expBoolNull = ((expBool)? _TRUE : _FALSE);
    string   expString   = str.str();
    _Intel_Pragma("warning pop")

    assertEquals(typeStr+" val->getType()",       type,          val->getType());
    assertEquals(typeStr+" val->as<N>()",                  num,  val->as<N       >());
    assertEquals(typeStr+" val->as<int8_t>()",    (int8_t )num,  val->as<int8_t  >());
    assertEquals(typeStr+" val->as<int16_t>()",   (int16_t)num,  val->as<int16_t >());
    assertEquals(typeStr+" val->asint32_t<>()",   (int32_t)num,  val->as<int32_t >());
    assertEquals(typeStr+" val->as<int64_t>()",   (int64_t)num,  val->as<int64_t >());
    assertEquals(typeStr+" val->as<float>()",     (float  )num,  val->as<float   >());
    assertEquals(typeStr+" val->as<double>()",    (double )num,  val->as<double  >());
    assertEquals(typeStr+" val->as<bool>()",      expBool,       val->as<bool    >());
    assertEquals(typeStr+" val->as<boolNull>()",  expBoolNull,   val->as<boolNull>());
    assertEquals(typeStr+" val->as<string>()",    expString,     val->as<string  >());
    assertEquals(typeStr+" val->toString()",      expString,     val->toString());
    assertEquals(typeStr+" val->isNullValue()",   false,         val->isNullValue());
    assertEquals(typeStr+" val->size()",          Value::npos,   val->size());
  }
}


template <typename N>
static void testNumeric (ValueType vecType, const vector<N> &vec) {
  ValueType type       = (ValueType)(-vecType);
  string    vecTypeStr = typeToString(vecType);

  for (size_t i = 0; i < vec.size(); i++) {
    Value val(vec[i]);
    testNumericValue(type, vec[i], &val, -1);
  }

  for (size_t len = 0; len < vec.size(); len++) {
    vector<N> testVec(vec.begin(), vec.begin()+len);
    ostringstream str;

    str << "[ ";
    for (size_t i = 0; i < len; i++) {
      if (i > 0) str << ", ";
      if (isNull(vec[i])) {
        str << "null";
      }
      else {
        str << vec[i];
      }
    }
    str << " ]";

    Value  val(&testVec);
    string expString = (len == 0)? "null" : str.str();

    assertEquals(vecTypeStr+" val.getType()",       vecType,       val.getType());
    assertEquals(vecTypeStr+" val.toString()",      expString,     val.toString());
    assertEquals(vecTypeStr+" val.isNullValue()",   (len == 0),    val.isNullValue());
    assertEquals(vecTypeStr+" val.size()",          len,           val.size());

    for (size_t i = 0; i < len; i++) {
      Value *v = val.at(i);
      testNumericValue(type, vec[i], v, (int32_t)i);
      delete v;
    }
  }
}





void ValueTest::testValue_void () {
  Value val;

  assertEquals("val.isNullValue()", true,   val.isNullValue());
  assertEquals("val.toString()",    "null", val.toString());
}

void ValueTest::testValue_int8_t () {
  ValueType       type = (ValueType)-ValueType_Int8;
  int32_t         bits = 8;
  vector<int8_t>  vec;

  vec.push_back(INT8_NULL);
  for (int64_t num = -16; num <= +16; num++) {
    vec.push_back((int8_t)num);
  }
  for (int32_t bit = 0; bit < bits; bit++) {
    int64_t num = ((int64_t)0x1) << bit;
    vec.push_back((int8_t)num);
  }
  testNumeric(type, vec);
}

void ValueTest::testValue_int16_t () {
  ValueType       type = (ValueType)-ValueType_Int16;
  int32_t         bits = 16;
  vector<int16_t> vec;

  vec.push_back(INT16_NULL);
  for (int64_t num = -16; num <= +16; num++) {
    vec.push_back((int16_t)num);
  }
  for (int32_t bit = 0; bit < bits; bit++) {
    int64_t num = ((int64_t)0x1) << bit;
    vec.push_back((int16_t)num);
  }
  testNumeric(type, vec);
}

void ValueTest::testValue_int32_t () {
  ValueType       type = (ValueType)-ValueType_Int32;
  int32_t         bits = 32;
  vector<int32_t> vec;

  vec.push_back(INT32_NULL);
  for (int64_t num = -16; num <= +16; num++) {
    vec.push_back((int32_t)num);
  }
  for (int32_t bit = 0; bit < bits; bit++) {
    int64_t num = ((int64_t)0x1) << bit;
    vec.push_back((int32_t)num);
  }
  testNumeric(type, vec);
}

void ValueTest::testValue_int64_t () {
  ValueType       type = (ValueType)-ValueType_Int64;
  int32_t         bits = 64;
  vector<int64_t> vec;

  vec.push_back(INT64_NULL);
  for (int64_t num = -16; num <= +16; num++) {
    vec.push_back((int64_t)num);
  }
  for (int32_t bit = 0; bit < bits; bit++) {
    int64_t num = ((int64_t)0x1) << bit;
    vec.push_back((int64_t)num);
  }
  testNumeric(type, vec);
}

void ValueTest::testValue_float () {
  ValueType       type = (ValueType)-ValueType_Float;
  vector<float>   vec;

  vec.push_back(FLOAT_NAN);
  for (int64_t num = -16; num <= +16; num++) {
    vec.push_back((float)num);
  }
  vec.push_back(+1e-9f);
  vec.push_back(-1e-9f);
  vec.push_back(+1e+9f);
  vec.push_back(-1e+9f);

  testNumeric(type, vec);
}

void ValueTest::testValue_double () {
  ValueType       type = (ValueType)-ValueType_Double;
  vector<double>  vec;

  vec.push_back(DOUBLE_NAN);
  for (int64_t num = -16; num <= +16; num++) {
    vec.push_back((double)num);
  }
  vec.push_back(+1e-9);
  vec.push_back(-1e-9);
  vec.push_back(+1e+9);
  vec.push_back(-1e+9);

  testNumeric(type, vec);
}

void ValueTest::testValue_bool () {
  ValueType type    = ValueType_Bool;
  ValueType vecType = (ValueType)-type;

  vector<bool> vec;
  vec.push_back(true);
  vec.push_back(false);

  Value t(true);
  Value f(false);
  Value v(&vec);

  Value *_t = v.at(0);
  Value *_f = v.at(1);

  assertEquals("Bool[] v.getType()",       vecType,       v.getType());
  assertEquals("Bool[] v.isNullValue()",   false,         v.isNullValue());
  assertEquals("Bool[] v.size()",          vec.size(),    v.size());

  testBoolValue(type, true,  &t, -1);
  testBoolValue(type, false, &f, -1);
  testBoolValue(type, true,  _t,  0);
  testBoolValue(type, false, _f,  1);

  delete _t;
  delete _f;
}

void ValueTest::testValue_boolNull () {
  ValueType type    = ValueType_BoolNull;
  ValueType vecType = (ValueType)-type;

  vector<boolNull> vec;
  vec.push_back(_TRUE);
  vec.push_back(_FALSE);
  vec.push_back(_NULL);

  Value t(_TRUE);
  Value f(_FALSE);
  Value n(_NULL);
  Value v(&vec);

  Value *_t = v.at(0);
  Value *_f = v.at(1);
  Value *_n = v.at(2);

  assertEquals("BoolNull[] v.getType()",       vecType,       v.getType());
  assertEquals("BoolNull[] v.isNullValue()",   false,         v.isNullValue());
  assertEquals("BoolNull[] v.size()",          vec.size(),    v.size());

  testBoolValue(type, _TRUE,  &t, -1);
  testBoolValue(type, _FALSE, &f, -1);
  testBoolValue(type, _NULL,  &n, -1);
  testBoolValue(type, _TRUE,  _t,  0);
  testBoolValue(type, _FALSE, _f,  1);
  testBoolValue(type, _NULL,  _n,  2);

  delete _t;
  delete _f;
  delete _n;
}

void ValueTest::testValue_string () {
  ValueType type    = ValueType_String;
  ValueType vecType = (ValueType)-type;

  string a = "";
  string b = "Hello World!";
  string c = "ABC123";

  vector<string*> vec;
  vec.push_back(&a);
  vec.push_back(&b);
  vec.push_back(&c);

  Value valA1(a); Value valA2(&a);
  Value valB1(b); Value valB2(&b);
  Value valC1(c); Value valC2(&c);
  Value v(&vec);

  Value *_a = v.at(0);
  Value *_b = v.at(1);
  Value *_c = v.at(2);

  assertEquals("String[] v.getType()",       vecType,       v.getType());
  assertEquals("String[] v.isNullValue()",   false,         v.isNullValue());
  assertEquals("String[] v.size()",          vec.size(),    v.size());

  testObjectValue<string*>(type, &a, "null", true,  true,  &valA1, -1);
  testObjectValue<string*>(type, &b, b,      false, true,  &valB1, -1);
  testObjectValue<string*>(type, &c, c,      false, true,  &valC1, -1);
  testObjectValue<string*>(type, &a, "null", true,  false, &valA2, -1);
  testObjectValue<string*>(type, &b, b,      false, false, &valB2, -1);
  testObjectValue<string*>(type, &c, c,      false, false, &valC2, -1);
  testObjectValue<string*>(type, &a, "null", true,  false, _a,      0);
  testObjectValue<string*>(type, &b, b,      false, false, _b,      1);
  testObjectValue<string*>(type, &c, c,      false, false, _c,      2);

  delete _a;
  delete _b;
  delete _c;
}

void ValueTest::testValue_wstring () {
  ValueType type    = ValueType_WString;
  ValueType vecType = (ValueType)-type;

  string strA = "";
  string strB = "Hello World!";
  string strC = "ABC123";
  wstring a;
  wstring b;
  wstring c;

  for (size_t i = 0; i < strA.length(); i++) a.push_back((wchar_t)strA[i]);
  for (size_t i = 0; i < strB.length(); i++) b.push_back((wchar_t)strB[i]);
  for (size_t i = 0; i < strC.length(); i++) c.push_back((wchar_t)strC[i]);

  vector<wstring*> vec;
  vec.push_back(&a);
  vec.push_back(&b);
  vec.push_back(&c);

  Value valA1(a); Value valA2(&a);
  Value valB1(b); Value valB2(&b);
  Value valC1(c); Value valC2(&c);
  Value v(&vec);

  Value *_a = v.at(0);
  Value *_b = v.at(1);
  Value *_c = v.at(2);

  assertEquals("WString[] v.getType()",       vecType,       v.getType());
  assertEquals("WString[] v.isNullValue()",   false,         v.isNullValue());
  assertEquals("WString[] v.size()",          vec.size(),    v.size());

  testObjectValue<wstring*>(type, &a, "null", true,  true,  &valA1, -1);
  testObjectValue<wstring*>(type, &b, strB,   false, true,  &valB1, -1);
  testObjectValue<wstring*>(type, &c, strC,   false, true,  &valC1, -1);
  testObjectValue<wstring*>(type, &a, "null", true,  false, &valA2, -1);
  testObjectValue<wstring*>(type, &b, strB,   false, false, &valB2, -1);
  testObjectValue<wstring*>(type, &c, strC,   false, false, &valC2, -1);
  testObjectValue<wstring*>(type, &a, "null", true,  false, _a,      0);
  testObjectValue<wstring*>(type, &b, strB,   false, false, _b,      1);
  testObjectValue<wstring*>(type, &c, strC,   false, false, _c,      2);

  delete _a;
  delete _b;
  delete _c;
}

void ValueTest::testValue_VRTObject () {
  ValueType type    = ValueType_VRTObject;
  ValueType vecType = (ValueType)-type;

  UUID      a; // <-- null by default
  UUID      b; b.setUUID("091e6a58-5379-4686-bd2e-60427bdd6c5e");
  TimeStamp c = TimeStamp::getSystemTime();

  vector<VRTObject*> vec;
  vec.push_back(&a);
  vec.push_back(&b);
  vec.push_back(&c);

  Value valA(&a);
  Value valB(&b);
  Value valC(&c);
  Value v(&vec);

  Value *_a = v.at(0);
  Value *_b = v.at(1);
  Value *_c = v.at(2);

  assertEquals("String[] v.getType()",       vecType,       v.getType());
  assertEquals("String[] v.isNullValue()",   false,         v.isNullValue());
  assertEquals("String[] v.size()",          vec.size(),    v.size());

  testObjectValue<VRTObject*>(type, dynamic_cast<VRTObject*>(&a), "null",       true,  false, &valA, -1);
  testObjectValue<VRTObject*>(type, dynamic_cast<VRTObject*>(&b), b.toString(), false, false, &valB, -1);
  testObjectValue<VRTObject*>(type, dynamic_cast<VRTObject*>(&c), c.toString(), false, false, &valC, -1);
  testObjectValue<VRTObject*>(type, dynamic_cast<VRTObject*>(&a), "null",       true,  false, _a,     0);
  testObjectValue<VRTObject*>(type, dynamic_cast<VRTObject*>(&b), b.toString(), false, false, _b,     1);
  testObjectValue<VRTObject*>(type, dynamic_cast<VRTObject*>(&c), c.toString(), false, false, _c,     2);

  testObjectValue<VRTObject*>(type, dynamic_cast<HasFields*>(&a), "null",       true,  false, &valA, -1);
  testObjectValue<VRTObject*>(type, dynamic_cast<HasFields*>(&b), b.toString(), false, false, &valB, -1);
  testObjectValue<VRTObject*>(type, dynamic_cast<HasFields*>(&c), c.toString(), false, false, &valC, -1);
  testObjectValue<VRTObject*>(type, dynamic_cast<HasFields*>(&a), "null",       true,  false, _a,     0);
  testObjectValue<VRTObject*>(type, dynamic_cast<HasFields*>(&b), b.toString(), false, false, _b,     1);
  testObjectValue<VRTObject*>(type, dynamic_cast<HasFields*>(&c), c.toString(), false, false, _c,     2);

  // Tests for .cast<T>()
  assertEquals("a->cast<VRTObject*>()", dynamic_cast<VRTObject*>(&a), valA.cast<VRTObject*>());
  assertEquals("b->cast<VRTObject*>()", dynamic_cast<VRTObject*>(&b), valB.cast<VRTObject*>());
  assertEquals("c->cast<VRTObject*>()", dynamic_cast<VRTObject*>(&c), valC.cast<VRTObject*>());
  assertEquals("a->cast<HasFields*>()", dynamic_cast<HasFields*>(&a), valA.cast<HasFields*>());
  assertEquals("b->cast<HasFields*>()", dynamic_cast<HasFields*>(&b), valB.cast<HasFields*>());
  assertEquals("c->cast<HasFields*>()", dynamic_cast<HasFields*>(&c), valC.cast<HasFields*>());

  delete _a;
  delete _b;
  delete _c;
}

