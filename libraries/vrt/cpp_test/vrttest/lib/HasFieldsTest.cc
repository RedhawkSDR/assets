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

#include "HasFieldsTest.h"
#include "TestRunner.h"
#include "UUID.h"
#include <errno.h>

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

static HasFields   a;
static UUID        b;
static HasFields *_a;
static HasFields *_b;

string HasFieldsTest::getTestedClass () {
  return "vrt::HasFields";
}

vector<TestSet> HasFieldsTest::init () {
  b.setUUID("091e6a58-5379-4686-bd2e-60427bdd6c5e");
  _a = &a;
  _b = &b;

  vector<TestSet> tests;
  tests.push_back(TestSet("getFieldCount(..)",    this, "testGetFieldCount"));
  tests.push_back(TestSet("getField(..)",         this, "testGetField"));
  tests.push_back(TestSet("getFieldByName(..)",   this, "+testGetField"));
  tests.push_back(TestSet("getFieldID(..)",       this, "+testGetField"));
  tests.push_back(TestSet("getFieldName(..)",     this, "+testGetField"));
  tests.push_back(TestSet("getFieldType(..)",     this, "+testGetField"));
  tests.push_back(TestSet("setField(..)",         this, "testSetField"));
  tests.push_back(TestSet("setFieldByName(..)",   this, "+testSetField"));

  return tests;
}

void HasFieldsTest::done () {
  // nothing to do
}

void HasFieldsTest::call (const string &func) {
  if (func == "testGetFieldCount"        ) testGetFieldCount();
  if (func == "testGetField"             ) testGetField();
  if (func == "testGetField_byNameErrorA") testGetField_byNameErrorA();
  if (func == "testGetField_byNameErrorB") testGetField_byNameErrorB();
  if (func == "testGetField_idErrorA"    ) testGetField_idErrorA();
  if (func == "testGetField_idErrorB"    ) testGetField_idErrorB();
  if (func == "testGetField_errorAm1"    ) testGetField_errorAm1();
  if (func == "testGetField_errorA0"     ) testGetField_errorA0();
  if (func == "testGetField_errorA1"     ) testGetField_errorA1();
  if (func == "testGetField_nameErrorAm1") testGetField_nameErrorAm1();
  if (func == "testGetField_nameErrorA0" ) testGetField_nameErrorA0();
  if (func == "testGetField_nameErrorA1" ) testGetField_nameErrorA1();
  if (func == "testGetField_typeErrorAm1") testGetField_typeErrorAm1();
  if (func == "testGetField_typeErrorA0" ) testGetField_typeErrorA0();
  if (func == "testGetField_typeErrorA1" ) testGetField_typeErrorA1();
  if (func == "testSetField"             ) testSetField();
  if (func == "testSetField_errorAm1"    ) testGetField_errorAm1();
  if (func == "testSetField_errorA0"     ) testGetField_errorA0();
  if (func == "testSetField_errorA1"     ) testGetField_errorA1();
  if (func == "testSetField_byNameErrorA") testSetField_byNameErrorA();
  if (func == "testSetField_byNameErrorB") testSetField_byNameErrorB();
}

void HasFieldsTest::testGetFieldCount () {
  assertEquals("_a.getFieldCount()", 0,                 _a->getFieldCount());
  assertEquals("_b.getFieldCount()", b.getFieldCount(), _b->getFieldCount());
}

void HasFieldsTest::testGetField () {
  assertException("_a.getField(-1)", "testGetField_errorAm1", "vrt::VRTException: Unknown field ID #-1");
  assertException("_a.getField(0)",  "testGetField_errorA0",  "vrt::VRTException: Unknown field ID #0");
  assertException("_a.getField(1)",  "testGetField_errorA1",  "vrt::VRTException: Unknown field ID #1");

  assertException("_a.getFieldByName('bogus')", "testGetField_byNameErrorA");
  assertException("_b.getFieldByName('bogus')", "testGetField_byNameErrorB");
  assertException("_a.getFieldID('bogus')",     "testGetField_idErrorA");
  assertException("_b.getFieldID('bogus')",     "testGetField_idErrorB");

  assertException("_a.getFieldName(-1)", "testGetField_nameErrorAm1", "vrt::VRTException: Unknown field ID #-1");
  assertException("_a.getFieldName(0)",  "testGetField_nameErrorA0",  "vrt::VRTException: Unknown field ID #0");
  assertException("_a.getFieldName(1)",  "testGetField_nameErrorA1",  "vrt::VRTException: Unknown field ID #1");

  assertException("_a.getFieldType(-1)", "testGetField_typeErrorAm1", "vrt::VRTException: Unknown field ID #-1");
  assertException("_a.getFieldType(0)",  "testGetField_typeErrorA0",  "vrt::VRTException: Unknown field ID #0");
  assertException("_a.getFieldType(1)",  "testGetField_typeErrorA1",  "vrt::VRTException: Unknown field ID #1");

  for (int32_t i = 0; i < b.getFieldCount(); i++) {
    Value *exp  = b.getField(i);
    Value *act1 = _b->getField(i);
    Value *act2 = _b->getFieldByName(b.getFieldName(i));

    try {
      assertEquals("_b.getField(..)",        exp,               act1);
      assertEquals("_b.getFieldByName(..)",  exp,               act2);
      assertEquals("_b.getFieldID(..)",      i,                 _b->getFieldID(b.getFieldName(i)));
      assertEquals("_b.getFieldName(i)",     b.getFieldName(i), _b->getFieldName(i));
      assertEquals("_b.getFieldType(i)",     b.getFieldType(i), _b->getFieldType(i));
    }
    catch (VRTException e) {
      delete exp;
      delete act1;
      delete act2;
      throw e;
    }
    delete exp;
    delete act1;
    delete act2;
  }
}

void HasFieldsTest::testSetField () {
  assertException("_a.setField(-1,1)", "testSetField_errorAm1", "vrt::VRTException: Unknown field ID #-1");
  assertException("_a.setField(0,1)",  "testSetField_errorA0",  "vrt::VRTException: Unknown field ID #0");
  assertException("_a.setField(1,1)",  "testSetField_errorA1",  "vrt::VRTException: Unknown field ID #1");

  assertException("_a.setFieldByName('bogus',1)", "testSetField_byNameErrorA");
  assertException("_b.setFieldByName('bogus',1)", "testSetField_byNameErrorB");
}

// The 'safe_delete(..)' is present below to make sure any erroneous object creation
// doesn't result in a memory leak.
void HasFieldsTest::testGetField_errorAm1     () { Value *v = _a->getField(-1); safe_delete(v); }
void HasFieldsTest::testGetField_errorA0      () { Value *v = _a->getField( 0); safe_delete(v); }
void HasFieldsTest::testGetField_errorA1      () { Value *v = _a->getField( 1); safe_delete(v); }
void HasFieldsTest::testGetField_byNameErrorA () { Value *v = _a->getFieldByName("bogus"); safe_delete(v); }
void HasFieldsTest::testGetField_byNameErrorB () { Value *v = _b->getFieldByName("bogus"); safe_delete(v); }
void HasFieldsTest::testGetField_idErrorA     () { _a->getFieldID("bogus"); }
void HasFieldsTest::testGetField_idErrorB     () { _b->getFieldID("bogus"); }
void HasFieldsTest::testGetField_nameErrorAm1 () { _a->getFieldName(-1); }
void HasFieldsTest::testGetField_nameErrorA0  () { _a->getFieldName( 0); }
void HasFieldsTest::testGetField_nameErrorA1  () { _a->getFieldName( 1); }
void HasFieldsTest::testGetField_typeErrorAm1 () { _a->getFieldType(-1); }
void HasFieldsTest::testGetField_typeErrorA0  () { _a->getFieldType( 0); }
void HasFieldsTest::testGetField_typeErrorA1  () { _a->getFieldType( 1); }

void HasFieldsTest::testSetField_errorAm1     () { _a->setField(-1, Value(1)); }
void HasFieldsTest::testSetField_errorA0      () { _a->setField( 0, Value(1)); }
void HasFieldsTest::testSetField_errorA1      () { _a->setField( 1, Value(1)); }
void HasFieldsTest::testSetField_byNameErrorA () { _a->setFieldByName("bogus", Value(1)); }
void HasFieldsTest::testSetField_byNameErrorB () { _b->setFieldByName("bogus", Value(1)); }
