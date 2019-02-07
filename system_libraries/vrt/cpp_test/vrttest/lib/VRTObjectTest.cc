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

#include "VRTObjectTest.h"
#include "TestRunner.h"
#include "UUID.h"
#include <errno.h>

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

/** Checks the exceptions getMessage(), toString(), and what() functions. */
static void checkException (const string &msg, const string &err, VRTException &e,
                            string prefix="vrt::VRTException: ") {
  const char *what = e.what();
  string     _what = (what == NULL)? string("<null>") : string(what);

  assertEquals(msg + " (message)",         err, e.getMessage());
  assertEquals(msg + " (toString)", prefix+err, e.toString());
  assertEquals(msg + " (what)",     prefix+err, _what);
}

string VRTObjectTest::getTestedClass () {
  return "vrt::VRTObject";
}

vector<TestSet> VRTObjectTest::init () {
  vector<TestSet> tests;
  tests.push_back(TestSet("equals(..)",     this, "testEquals"));
  tests.push_back(TestSet("getClass()",     this, "+testGetClassName"));
  tests.push_back(TestSet("getClassName()", this, "testGetClassName"));
  tests.push_back(TestSet("isNullValue()",  this, "testIsNullValue"));
  tests.push_back(TestSet("toString()",     this, "testToString"));

  return tests;
}

void VRTObjectTest::done () {
  // nothing to do
}

void VRTObjectTest::call (const string &func) {
  if (func == "testEquals"      ) testEquals();
  if (func == "testGetClassName") testGetClassName();
  if (func == "testIsNullValue" ) testIsNullValue();
  if (func == "testToString"    ) testToString();
}

void VRTObjectTest::testToString () {
  VRTObject    a1;
  VRTException e1("Test Error");

  assertEquals("a1.toString()", "vrt::VRTObject@",   a1.toString().substr(0,15));
  assertEquals("e1.toString()", "vrt::VRTException", e1.toString().substr(0,17));
}

void VRTObjectTest::testEquals () {
  VRTObject    a1;
  VRTException e1("Test Error");

  assertEquals("a1.equals(a1)", true,  a1.equals(a1));
  assertEquals("a1.equals(e1)", false, a1.equals(e1));
  assertEquals("e1.equals(a1)", false, e1.equals(a1));
}

void VRTObjectTest::testGetClassName () {
  VRTObject    a1;
  VRTException e1("Test Error");

  assertEquals("a1.getClassName()", "vrt::VRTObject",    a1.getClassName());
  assertEquals("e1.getClassName()", "vrt::VRTException", e1.getClassName());
}

void VRTObjectTest::testIsNullValue () {
  VRTObject    a1;
  VRTException e1("Test Error");
  VRTException e2;

  assertEquals("a1.isNullValue()", false, a1.isNullValue()); // top-level default
  assertEquals("e1.isNullValue()", false, e1.isNullValue());
  assertEquals("e2.isNullValue()", true,  e2.isNullValue());
}






string VRTExceptionTest::getTestedClass () {
  return "vrt::VRTException";
}

vector<TestSet> VRTExceptionTest::init () {
  vector<TestSet> tests;
  tests.push_back(TestSet("VRTExceptionTest(errno)",   this, "testVRTException_errnum"));
  tests.push_back(TestSet("VRTExceptionTest(string)",  this, "testVRTException_string"));
  tests.push_back(TestSet("VRTExceptionTest(fmt,...)", this, "testVRTException_format"));

  tests.push_back(TestSet("equals(..)",                this, "+testToString"));
  tests.push_back(TestSet("getMessage()",              this, "+testVRTException_errnum"));
  tests.push_back(TestSet("getStackTrace()",           this, "-")); // SKIP: platform-specific
  tests.push_back(TestSet("printStackTrace()",         this, "-")); // SKIP: requires stderr
  tests.push_back(TestSet("toString()",                this,  "testToString"));
  tests.push_back(TestSet("what()",                    this, "+")); // tested in various places

  return tests;
}

void VRTExceptionTest::done () {
  // nothing to do
}

void VRTExceptionTest::call (const string &func) {
  if (func == "testVRTException_errnum"              ) testVRTException_errnum();
  if (func == "testVRTException_errnum_throwEACCES"  ) testVRTException_errnum_throwEACCES();
  if (func == "testVRTException_errnum_throw9999"    ) testVRTException_errnum_throw9999();
  if (func == "testVRTException_string"              ) testVRTException_string();
  if (func == "testVRTException_string_throwTestMsg1") testVRTException_string_throwTestMsg1();
  if (func == "testVRTException_string_throwTestMsg2") testVRTException_string_throwTestMsg2();
  if (func == "testVRTException_format"              ) testVRTException_format();
  if (func == "testVRTException_format_throwTestMsg1") testVRTException_format_throwTestMsg1();
  if (func == "testVRTException_format_throwTestMsg2") testVRTException_format_throwTestMsg2();
  if (func == "testToString"                         ) testToString();
}

void VRTExceptionTest::testToString () {
  // Special tests added for VRT-26 regarding issues with copy constructor and equals
  VRTException nullException;
  VRTException origException("Test Exception");
  VRTException copyException = origException;
  VRTException diffException("Another Test Exception");

  assertEquals("toString()", "<null>",                            nullException.toString());
  assertEquals("toString()", "vrt::VRTException: Test Exception", origException.toString());
  assertEquals("toString()", "vrt::VRTException: Test Exception", copyException.toString());

  assertEquals("equals(..) for null",  false, origException.equals(nullException));
  assertEquals("equals(..) for self",  true,  origException.equals(origException));
  assertEquals("equals(..) for copy",  true,  origException.equals(copyException));
  assertEquals("equals(..) for other", false, origException.equals(diffException));
}

void VRTExceptionTest::testVRTException_errnum () {
  // Note: The following messages match GCC 4.4.5 on RHEL 6.1 and may differ on
  // other platforms, change as required.
  VRTException eEACCES(EACCES);
  VRTException e9999(9999);

  checkException("VRTException(EACCES)", "Permission denied",  eEACCES);

#if (defined(__APPLE__) && defined(__MACH__))
  checkException("VRTException(9999)",   "Unknown error: 9999", e9999);
#else
  checkException("VRTException(9999)",   "Unknown error 9999", e9999);
#endif

  assertException("throw VRTException(EACCES)", "testVRTException_errnum_throwEACCES", "vrt::VRTException: Permission denied");

#if (defined(__APPLE__) && defined(__MACH__))
  assertException("throw VRTException(9999)",   "testVRTException_errnum_throw9999",   "vrt::VRTException: Unknown error: 9999");
#else
  assertException("throw VRTException(9999)",   "testVRTException_errnum_throw9999",   "vrt::VRTException: Unknown error 9999");
#endif
}

void VRTExceptionTest::testVRTException_errnum_throwEACCES () {
  throw VRTException(EACCES);
}

void VRTExceptionTest::testVRTException_errnum_throw9999() {
  throw VRTException(9999);
}

void VRTExceptionTest::testVRTException_string () {
  VRTException e1("Test Error Message #1");
  VRTException e2("Test Error Message #2");

  checkException("VRTException('Test Error Message #1')", "Test Error Message #1", e1);
  checkException("VRTException('Test Error Message #2')", "Test Error Message #2", e2);

  assertException("throw VRTException('Test Error Message #1')", "testVRTException_string_throwTestMsg1", "vrt::VRTException: Test Error Message #1");
  assertException("throw VRTException('Test Error Message #2')", "testVRTException_string_throwTestMsg2", "vrt::VRTException: Test Error Message #2");
}

void VRTExceptionTest::testVRTException_string_throwTestMsg1 () {
  throw VRTException("Test Error Message #1");
}

void VRTExceptionTest::testVRTException_string_throwTestMsg2() {
  throw VRTException("Test Error Message #2");
}

void VRTExceptionTest::testVRTException_format () {
  VRTException e1("Test Error Message #%d", 1);
  VRTException e2("Test Error Message #%d", 2);

  checkException("VRTException('Test Error Message #1')", "Test Error Message #1", e1);
  checkException("VRTException('Test Error Message #2')", "Test Error Message #2", e2);

  assertException("throw VRTException('Test Error Message #1')", "testVRTException_format_throwTestMsg1", "vrt::VRTException: Test Error Message #1");
  assertException("throw VRTException('Test Error Message #2')", "testVRTException_format_throwTestMsg2", "vrt::VRTException: Test Error Message #2");
}

void VRTExceptionTest::testVRTException_format_throwTestMsg1 () {
  throw VRTException("Test Error Message #%d", 1);
}

void VRTExceptionTest::testVRTException_format_throwTestMsg2() {
  throw VRTException("Test Error Message #%d", 2);
}





string ClassCastExceptionTest::getTestedClass () {
  return "vrt::ClassCastException";
}

vector<TestSet> ClassCastExceptionTest::init () {
  vector<TestSet> tests;
  tests.push_back(TestSet("ClassCastException(..)", this,  "testClassCastException"));
  tests.push_back(TestSet("getMessage()",           this, "+testClassCastException"));
  tests.push_back(TestSet("toString()",             this, "+testClassCastException"));
  tests.push_back(TestSet("what()",                 this, "+testClassCastException"));

  return tests;
}

void ClassCastExceptionTest::done () {
  // nothing to do
}

void ClassCastExceptionTest::call (const string &func) {
  if (func == "testClassCastException") testClassCastException();
}

void ClassCastExceptionTest::testClassCastException () {
  VRTObject        obj;
  UUID             uuid;    uuid.setUUID("091e6a58-5379-4686-bd2e-60427bdd6c5e");
  const VRTObject& objref = obj;

  try {
    UUID *x = checked_dynamic_cast<UUID*>(&obj);
    assertEquals("Caught exception", true,  false);
    assertEquals("Cast to NULL",     false, (x == NULL));
  }
  catch (ClassCastException e) {
    checkException("Casting to UUID", "vrt::VRTObject* can not be cast to vrt::UUID*", e, "vrt::ClassCastException: ");
    assertEquals("Caught exception", true, true);
  }

  try {
    const UUID& x = checked_dynamic_cast<const UUID&>(objref);
    assertEquals("Caught exception", true,  false);
    assertEquals("Cast to NULL",     false, isNull(x)); // <-- this will never hit, but prevents warnings
  }
  catch (ClassCastException e) {
    checkException("Casting to UUID", "vrt::VRTObject can not be cast to vrt::UUID", e, "vrt::ClassCastException: ");
    assertEquals("Caught exception", true, true);
  }
}
