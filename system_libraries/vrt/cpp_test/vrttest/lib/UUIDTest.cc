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

#include "UUIDTest.h"
#include "TestRunner.h"
#include "UUID.h"
#include <errno.h>

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

static const uuid_t buf1 = { 0x09, 0x1e, 0x6a, 0x58, 0x53, 0x79, 0x46, 0x86, 0xbd, 0x2e, 0x60, 0x42, 0x7b, 0xdd, 0x6c, 0x5e };
static const uuid_t buf2 = { 0x61, 0xe0, 0x9d, 0x02, 0x6d, 0x6b, 0x46, 0x34, 0x9e, 0x73, 0xf2, 0xc8, 0xb5, 0xf8, 0x13, 0xe0 };
static const string str1 = "091e6a58-5379-4686-bd2e-60427bdd6c5e";
static const string str2 = "61e09d02-6d6b-4634-9e73-f2c8b5f813e0";

string UUIDTest::getTestedClass () {
  return "vrt::UUID";
}

vector<TestSet> UUIDTest::init () {
  vector<TestSet> tests;
  tests.push_back(TestSet("equals(..)",      this, "testEquals"));
  tests.push_back(TestSet("getField(..)",    this, "testGetField"));
  tests.push_back(TestSet("getFieldCount()", this, "testGetFieldCount"));
  tests.push_back(TestSet("getFieldName()",  this, "testGetFieldName"));
  tests.push_back(TestSet("getFieldType()",  this, "testGetFieldType"));
  tests.push_back(TestSet("getUUID(..)",     this, "testGetUUID"));
  tests.push_back(TestSet("getValue(..)",    this, "testGetValue"));
  tests.push_back(TestSet("isNullValue()",   this, "testIsNullValue"));
#if INTERNAL_UUID_LIB != 1
  tests.push_back(TestSet("randomUUID()",    this, "testRandomUUID"));
#else
  tests.push_back(TestSet("randomUUID()",    this, "-testRandomUUID"));
#endif
  tests.push_back(TestSet("setField(..)",    this, "testSetField"));
  tests.push_back(TestSet("setUUID(..)",     this, "testSetUUID"));
  tests.push_back(TestSet("setValue(..)",    this, "testSetValue"));
  tests.push_back(TestSet("toString()",      this, "testToString"));

  return tests;
}

void UUIDTest::done () {
  // nothing to do
}

void UUIDTest::call (const string &func) {
  if (func == "testEquals"        ) testEquals();
  if (func == "testGetField"      ) testGetField();
  if (func == "testgGetFieldCount") testGetFieldCount();
  if (func == "testgGetFieldName" ) testGetFieldName();
  if (func == "testgGetFieldType" ) testGetFieldType();
  if (func == "testGetUUID"       ) testGetUUID();
  if (func == "testGetValue"      ) testGetValue();
  if (func == "testIsNullValue"   ) testIsNullValue();
  if (func == "testRandomUUID"    ) testRandomUUID();
  if (func == "testSetField"      ) testSetField();
  if (func == "testSetUUID"       ) testSetUUID();
  if (func == "testSetValue"      ) testSetValue();
  if (func == "testToString"      ) testToString();
}

void UUIDTest::testEquals () {
  UUID u1(buf1);
  UUID u2(buf2);
  UUID u3(buf1); // same as u1
  VRTObject o1;

  assertEquals("u1.equals(u1)", true,  u1.equals(u1));
  assertEquals("u1.equals(u2)", false, u1.equals(u2));
  assertEquals("u1.equals(u3)", true,  u1.equals(u3));
  assertEquals("u1.equals(o1)", false, u1.equals(o1));
}

void UUIDTest::testGetField () {
  UUID u1(buf1);
  UUID u2(buf2);

  Value *v1 = u1.getField(0);
  Value *v2 = u2.getField(0);

  assertEquals("u1.getField(0)", u1.getUUID(), v1->toString());
  assertEquals("u2.getField(0)", u2.getUUID(), v2->toString());

  delete v1;
  delete v2;
}

void UUIDTest::testGetFieldCount () {
  UUID u1(buf1);
  UUID u2(buf2);

  assertEquals("u1.getFieldCount()", 1, u1.getFieldCount());
  assertEquals("u2.getFieldCount()", 1, u2.getFieldCount());
}

void UUIDTest::testGetFieldName () {
  UUID u1(buf1);
  UUID u2(buf2);

  assertEquals("u1.getFieldName(0)", "UUID", u1.getFieldName(0));
  assertEquals("u2.getFieldName(0)", "UUID", u2.getFieldName(0));
}

void UUIDTest::testGetFieldType () {
  UUID u1(buf1);
  UUID u2(buf2);

  assertEquals("u1.getFieldType(0)", ValueType_String, u1.getFieldType(0));
  assertEquals("u2.getFieldType(0)", ValueType_String, u2.getFieldType(0));
}

void UUIDTest::testGetUUID () {
  UUID u1(buf1);
  UUID u2(buf2);

  assertEquals("u1.getUUID()", str1, u1.getUUID());
  assertEquals("u2.getUUID()", str2, u2.getUUID());
}

void UUIDTest::testGetValue () {
  UUID u1(buf1);
  UUID u2(buf2);
  char v1[16]; u1.getValue(v1);
  char v2[16]; u2.getValue(v2);

  assertBufEquals("u1.getValue()", (char*)(void*)buf1, 0, 16, v1, 0, 16);
  assertBufEquals("u2.getValue()", (char*)(void*)buf2, 0, 16, v2, 0, 16);
}

void UUIDTest::testIsNullValue () {
  UUID u1(buf1);
  UUID u2(buf2);
  UUID u3;

  assertEquals("u1.isNullValue()", false, u1.isNullValue());
  assertEquals("u2.isNullValue()", false, u2.isNullValue());
  assertEquals("u3.isNullValue()", true,  u3.isNullValue());
}

#if INTERNAL_UUID_LIB != 1
void UUIDTest::testRandomUUID () {
  UUID u1 = UUID::randomUUID();
  UUID u2 = UUID::randomUUID();

  // Simply tests that u1 and u2 got values and that they differ
  assertEquals("u1.isNullValue()", false, u1.isNullValue());
  assertEquals("u2.isNullValue()", false, u2.isNullValue());
  assertEquals("u1.equals(u2)",    false, u1.equals(u2));
}
#else
void UUIDTest::testRandomUUID () {
  // not valid
}
#endif

void UUIDTest::testSetField () {
  UUID u1(buf1);  // <-- no change
  UUID u2(buf2);  // <-- changes
  UUID u3;        // <-- set from null

  Value val = Value(str1);

  u1.setField(0,&val);
  u2.setField(0,&val);
  u3.setField(0,&val);

  assertEquals("u1.setField(0,&val)", str1, u1.getUUID());
  assertEquals("u2.setField(0,&val)", str1, u2.getUUID());
  assertEquals("u3.setField(0,&val)", str1, u3.getUUID());
}

void UUIDTest::testSetUUID () {
  UUID u1(buf1);  // <-- no change
  UUID u2(buf2);  // <-- changes
  UUID u3;        // <-- set from null

  u1.setUUID(str1);
  u2.setUUID(str1);
  u3.setUUID(str1);

  assertEquals("u1.setUUID(str1)", str1, u1.getUUID());
  assertEquals("u2.setUUID(str1)", str1, u2.getUUID());
  assertEquals("u3.setUUID(str1)", str1, u3.getUUID());
}

void UUIDTest::testSetValue () {
  UUID u1(buf1);  // <-- no change
  UUID u2(buf2);  // <-- changes
  UUID u3;        // <-- set from null

  u1.setValue(buf1);
  u2.setValue(buf1);
  u3.setValue(buf1);

  assertEquals("u1.setValue(buf1)", str1, u1.getUUID());
  assertEquals("u2.setValue(buf1)", str1, u2.getUUID());
  assertEquals("u3.setValue(buf1)", str1, u3.getUUID());
}

void UUIDTest::testToString () {
  UUID u1(buf1);
  UUID u2(buf2);

  assertEquals("u1.toString()", u1.getUUID(), u1.toString());
  assertEquals("u2.toString()", u2.getUUID(), u2.toString());
}
