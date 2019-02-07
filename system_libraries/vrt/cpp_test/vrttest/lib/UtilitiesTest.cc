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

#include "UtilitiesTest.h"
#include "TestRunner.h"
#include "UUID.h"
#include "TestUtilities.h"
#include "Utilities.h"
#include <math.h>     // for fmod(..) [do not use tgmath.h in place of this]
#include <ctime>      // for gettimeofday(..) / clock_gettime(..)
#include <sys/time.h> // for gettimeofday(..)  [see man page]
#include <errno.h>    // for errno

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

#define SPECIAL_FLOAT_COUNT  TestUtilities::SPECIAL_FLOAT_COUNT
#define SPECIAL_FLOAT_BITS   TestUtilities::SPECIAL_FLOAT_BITS
#define SPECIAL_FLOAT_VALS   TestUtilities::SPECIAL_FLOAT_VALS
#define SPECIAL_DOUBLE_COUNT TestUtilities::SPECIAL_DOUBLE_COUNT
#define SPECIAL_DOUBLE_BITS  TestUtilities::SPECIAL_DOUBLE_BITS
#define SPECIAL_DOUBLE_VALS  TestUtilities::SPECIAL_DOUBLE_VALS

string UtilitiesTest::getTestedClass () {
  return "vrt::Utilities";
}

vector<TestSet> UtilitiesTest::init () {
  vector<TestSet> tests;
  tests.push_back(TestSet("append(..)",              this,  "testAppend"));
  tests.push_back(TestSet("binarySearch(..)",        this,  "testBinarySearch"));
  tests.push_back(TestSet("currentTimeMillis()",     this,  "testCurrentTimeMillis"));
  tests.push_back(TestSet("format(..)",              this,  "testFormat"));
  tests.push_back(TestSet("fromStringClassID(..)",   this, "+testToStringClassID"));
  tests.push_back(TestSet("fromStringDeviceID(..)",  this, "+testToStringDeviceID"));
  tests.push_back(TestSet("fromStringOUI(..)",       this, "+testToStringOUI"));
  tests.push_back(TestSet("getCurrentSystemTime(..)",this, "+testCurrentTimeMillis"));
  tests.push_back(TestSet("normalizeAngle180(..)",   this,  "testNormalizeAngle180"));
  tests.push_back(TestSet("normalizeAngle360(..)",   this,  "testNormalizeAngle360"));
  tests.push_back(TestSet("sleep(..)",               this, "-testSleep"));
  tests.push_back(TestSet("sleepUntil(..)",          this, "-testSleepUntil"));
  tests.push_back(TestSet("toBoolean(..)",           this,  "testToBoolean"));
  tests.push_back(TestSet("toHexString(..)",         this,  "testToHexString"));
  tests.push_back(TestSet("toLowerCase(..)",         this,  "testToLowerCase"));
  tests.push_back(TestSet("toStringClassID(..)",     this,  "testToStringClassID"));
  tests.push_back(TestSet("toStringDeviceID(..)",    this,  "testToStringDeviceID"));
  tests.push_back(TestSet("toStringOUI(..)",         this,  "testToStringOUI"));
  tests.push_back(TestSet("toUpperCase(..)",         this, "+testToLowerCase"));
  tests.push_back(TestSet("trim(..)",                this, "+testTrimNA"));
  tests.push_back(TestSet("trimNA(..)",              this,  "testTrimNA"));

  return tests;
}

void UtilitiesTest::done () {
  // nothing to do
}

void UtilitiesTest::call (const string &func) {
  if (func == "testAppend"             ) testAppend();
  if (func == "testBinarySearch"       ) testBinarySearch();
  if (func == "testCurrentTimeMillis"  ) testCurrentTimeMillis();
  if (func == "testFormat"             ) testFormat();
  if (func == "testNormalizeAngle180"  ) testNormalizeAngle180();
  if (func == "testNormalizeAngle360"  ) testNormalizeAngle360();
  if (func == "testToBoolean"          ) testToBoolean();
  if (func == "testToHexString"        ) testToHexString();
  if (func == "testToLowerCase"        ) testToLowerCase();
  if (func == "testToStringClassID"    ) testToStringClassID();
  if (func == "testToStringDeviceID"   ) testToStringDeviceID();
  if (func == "testToStringOUI"        ) testToStringOUI();
  if (func == "testTrimNA"             ) testTrimNA();
}

template <typename T>
static void _testAppend (T nullVal, T value) {
  ostringstream msg1;  msg1  << "append(s,prefix,"<<nullVal<<")";
  ostringstream msg2;  msg2  << "append(s,prefix,"<<nullVal<<",suffix)";
  ostringstream msg3;  msg3  << "append(s,prefix,"<<value<<")";
  ostringstream msg4;  msg4  << "append(s,prefix,"<<value<<",suffix)";

  ostringstream exp1;  exp1 << "initial text ";
  ostringstream exp2;  exp2 << "initial text ";
  ostringstream exp3;  exp3 << "initial text prefix " << value;
  ostringstream exp4;  exp4 << "initial text prefix " << value << " suffix";

  ostringstream act1;  act1 << "initial text ";
  ostringstream act2;  act2 << "initial text ";
  ostringstream act3;  act3 << "initial text ";
  ostringstream act4;  act4 << "initial text ";

  Utilities::append(act1, "prefix ", nullVal);
  Utilities::append(act2, "prefix ", nullVal, " suffix");
  Utilities::append(act3, "prefix ", value);
  Utilities::append(act4, "prefix ", value, " suffix");


  assertEquals(msg1.str(), exp1.str(), act1.str());
  assertEquals(msg2.str(), exp2.str(), act2.str());
  assertEquals(msg3.str(), exp3.str(), act3.str());
  assertEquals(msg4.str(), exp4.str(), act4.str());
}

void UtilitiesTest::testAppend () {
  UUID a; // <-- null by default
  UUID b; b.setUUID("091e6a58-5379-4686-bd2e-60427bdd6c5e");

  _testAppend("",         "My String");
  _testAppend(string(""), string("My String"));
  _testAppend(INT8_NULL,  ((int8_t )10));
  _testAppend(INT16_NULL, ((int16_t)20));
  _testAppend(INT32_NULL, ((int32_t)30));
  _testAppend(INT64_NULL, ((int64_t)40));
  _testAppend(FLOAT_NAN,  ((float  )5.5));
  _testAppend(DOUBLE_NAN, ((double )6.5));
  _testAppend(_NULL,      _TRUE        );
  _testAppend(a,          b            );

  // bool is special since it always appends
  ostringstream act1;  act1 << "initial text ";
  ostringstream act2;  act2 << "initial text ";
  Utilities::append(act1, "prefix ", true);
  Utilities::append(act2, "prefix ", false, " suffix");

  assertEquals("append(s,prefix,true)"       , "initial text prefix 1",        act1.str());
  assertEquals("append(s,prefix,true,suffix)", "initial text prefix 0 suffix", act2.str());
}

void UtilitiesTest::testBinarySearch () {
  vector<char> vowels;
  vowels.push_back('a');
  vowels.push_back('e');
  vowels.push_back('i');
  vowels.push_back('o');
  vowels.push_back('u');

  assertEquals("binarySearch(vowels,'a')", 0, Utilities::binarySearch(vowels,'a'));
  assertEquals("binarySearch(vowels,'e')", 1, Utilities::binarySearch(vowels,'e'));
  assertEquals("binarySearch(vowels,'i')", 2, Utilities::binarySearch(vowels,'i'));
  assertEquals("binarySearch(vowels,'o')", 3, Utilities::binarySearch(vowels,'o'));
  assertEquals("binarySearch(vowels,'u')", 4, Utilities::binarySearch(vowels,'u'));

  assertEquals("binarySearch(vowels,'b')", -2, Utilities::binarySearch(vowels,'b'));
  assertEquals("binarySearch(vowels,'h')", -3, Utilities::binarySearch(vowels,'h'));
  assertEquals("binarySearch(vowels,'z')", -5, Utilities::binarySearch(vowels,'z'));
}

void UtilitiesTest::testCurrentTimeMillis () {
  // The basic test is done in us since that is what the system clock uses,
  // however allow for a 100ms tolerance since there is some non-negligible
  // code between the call to get the system time in getSystemTime() and the
  // one below.
  int64_t ms   = Utilities::currentTimeMillis();
  int64_t sec;
  int64_t ps;
  Utilities::getCurrentSystemTime(sec, ps);

  struct timeval tv;
  if (gettimeofday(&tv, NULL) != 0) {
    throw VRTException(errno);
  }

  // Need to cast to int64_t on next line since some 32-bit systems will have
  // a smaller type declaration that will cause the math to overflow.
  int64_t exp  = (((int64_t)tv.tv_sec) * 1000000) + ((int64_t)tv.tv_usec);
  int64_t act1 = (ms  * 1000);
  int64_t act2 = (sec * 1000000) + (ps / 1000000);

  assertEquals("currentTimeMillis()",      exp, act1, __INT64_C(100000));
  assertEquals("getCurrentSystemTime(..)", exp, act2, __INT64_C(100000));
}

void UtilitiesTest::testFormat () {
  assertEquals("Test #1", "Hello World!",   Utilities::format("Hello World!"));
  assertEquals("Test #2", "Hello World!",   Utilities::format("Hello %s!", "World"));
  assertEquals("Test #3", "3, 2, 1, Go!",   Utilities::format("%d, %d, %d, %s!", 3, 2, 1, "Go"));
}

void UtilitiesTest::testToBoolean () {
  vector<string> nullValues;
  vector<string> trueValues;
  vector<string> falseValues;

  nullValues.push_back( ""     );
  trueValues.push_back( "true" );  trueValues.push_back( "TRUE" );
  trueValues.push_back( "t"    );  trueValues.push_back( "T"    );
  trueValues.push_back( "1"    );
  trueValues.push_back( "yes"  );  trueValues.push_back( "YES"  );
  trueValues.push_back( "y"    );  trueValues.push_back( "Y"    );
  trueValues.push_back( "on"   );  trueValues.push_back( "ON"   );
  falseValues.push_back("false");  falseValues.push_back("FALSE");
  falseValues.push_back("f"    );  falseValues.push_back("F"    );
  falseValues.push_back("0"    );
  falseValues.push_back("no"   );  falseValues.push_back("NO"  );
  falseValues.push_back("n"    );  falseValues.push_back("N"    );
  falseValues.push_back("off"  );  falseValues.push_back("off"  );

  for (size_t i = 0; i < nullValues.size(); i++) {
    string v = nullValues[i];
    assertEquals("toBoolean("+v+")",      _NULL, Utilities::toBoolean(v));
    assertEquals("toBooleanValue("+v+")", false, Utilities::toBooleanValue(v));
  }

  for (size_t i = 0; i < trueValues.size(); i++) {
    string v = trueValues[i];
    assertEquals("toBoolean("+v+")",      _TRUE, Utilities::toBoolean(v));
    assertEquals("toBooleanValue("+v+")", true,  Utilities::toBooleanValue(v));
  }

  for (size_t i = 0; i < falseValues.size(); i++) {
    string v = falseValues[i];
    assertEquals("toBoolean("+v+")",      _FALSE, Utilities::toBoolean(v));
    assertEquals("toBooleanValue("+v+")", false,  Utilities::toBooleanValue(v));
  }
}


void UtilitiesTest::testTrimNA () {
  vector<string> tests;

  // NORMAL TEST CASES (equals)
  //             input                              trim                             trimNA
  tests.push_back("");               tests.push_back("");             tests.push_back("");
  tests.push_back("    ");           tests.push_back("");             tests.push_back("");
  tests.push_back("n/a");            tests.push_back("n/a");          tests.push_back("");
  tests.push_back("N/A");            tests.push_back("N/A");          tests.push_back("");
  tests.push_back("  n/a");          tests.push_back("n/a");          tests.push_back("");
  tests.push_back("  N/A");          tests.push_back("N/A");          tests.push_back("");
  tests.push_back("n/a  ");          tests.push_back("n/a");          tests.push_back("");
  tests.push_back("N/A  ");          tests.push_back("N/A");          tests.push_back("");
  tests.push_back(" n/a ");          tests.push_back("n/a");          tests.push_back("");
  tests.push_back(" N/A ");          tests.push_back("N/A");          tests.push_back("");
  tests.push_back("na");             tests.push_back("na");           tests.push_back("");
  tests.push_back("NA");             tests.push_back("NA");           tests.push_back("");
  tests.push_back("  na");           tests.push_back("na");           tests.push_back("");
  tests.push_back("  NA");           tests.push_back("NA");           tests.push_back("");
  tests.push_back("na  ");           tests.push_back("na");           tests.push_back("");
  tests.push_back("NA  ");           tests.push_back("NA");           tests.push_back("");
  tests.push_back(" na ");           tests.push_back("na");           tests.push_back("");
  tests.push_back(" NA ");           tests.push_back("NA");           tests.push_back("");
  tests.push_back("Cat");            tests.push_back("Cat");          tests.push_back("Cat");
  tests.push_back("  Cat");          tests.push_back("Cat");          tests.push_back("Cat");
  tests.push_back("Cat  ");          tests.push_back("Cat");          tests.push_back("Cat");
  tests.push_back(" Cat ");          tests.push_back("Cat");          tests.push_back("Cat");
  tests.push_back("Hello World!");   tests.push_back("Hello World!"); tests.push_back("Hello World!");
  tests.push_back("  Hello World!"); tests.push_back("Hello World!"); tests.push_back("Hello World!");
  tests.push_back("Hello World!  "); tests.push_back("Hello World!"); tests.push_back("Hello World!");
  tests.push_back(" Hello World! "); tests.push_back("Hello World!"); tests.push_back("Hello World!");

  for (size_t i = 0; i < tests.size(); i+=3) {
    assertEquals("trim("  +tests[i]+")", tests[i+1], Utilities::trim(  tests[i]));
    assertEquals("trimNA("+tests[i]+")", tests[i+2], Utilities::trimNA(tests[i]));
  }

}


#define addHexTest(a,b,c,d,e,f,g,h,i) \
  tests0.push_back(__INT64_C(a)); \
  tests.push_back( # a); \
  tests.push_back(b); \
  tests.push_back(c); \
  tests.push_back(d); \
  tests.push_back(e); \
  tests.push_back(f); \
  tests.push_back(g); \
  tests.push_back(h); \
  tests.push_back(i);

void UtilitiesTest::testToHexString () {
  vector<int64_t> tests0;
  vector<string>  tests;

  addHexTest(0x0000000000000000, "00", "0000", "000000", "00000000", "0000000000", "000000000000", "00000000000000", "0000000000000000");
  addHexTest(0x000000000000000F, "0F", "000F", "00000F", "0000000F", "000000000F", "00000000000F", "0000000000000F", "000000000000000F");
  addHexTest(0x00000000000000F0, "F0", "00F0", "0000F0", "000000F0", "00000000F0", "0000000000F0", "000000000000F0", "00000000000000F0");
  addHexTest(0x0000000000000F00, "00", "0F00", "000F00", "00000F00", "0000000F00", "000000000F00", "00000000000F00", "0000000000000F00");
  addHexTest(0x000000000000F000, "00", "F000", "00F000", "0000F000", "000000F000", "00000000F000", "0000000000F000", "000000000000F000");
  addHexTest(0x00000000000F0001, "01", "0001", "0F0001", "000F0001", "00000F0001", "0000000F0001", "000000000F0001", "00000000000F0001");
  addHexTest(0x0000000000F00010, "10", "0010", "F00010", "00F00010", "0000F00010", "000000F00010", "00000000F00010", "0000000000F00010");
  addHexTest(0x000000000F000100, "00", "0100", "000100", "0F000100", "000F000100", "00000F000100", "0000000F000100", "000000000F000100");
  addHexTest(0x00000000F0001000, "00", "1000", "001000", "F0001000", "00F0001000", "0000F0001000", "000000F0001000", "00000000F0001000");
  addHexTest(0x0000000F0001000A, "0A", "000A", "01000A", "0001000A", "0F0001000A", "000F0001000A", "00000F0001000A", "0000000F0001000A");
  addHexTest(0x000000F0001000A0, "A0", "00A0", "1000A0", "001000A0", "F0001000A0", "00F0001000A0", "0000F0001000A0", "000000F0001000A0");
  addHexTest(0x00000F0001000A00, "00", "0A00", "000A00", "01000A00", "0001000A00", "0F0001000A00", "000F0001000A00", "00000F0001000A00");
  addHexTest(0x0000F0001000A000, "00", "A000", "00A000", "1000A000", "001000A000", "F0001000A000", "00F0001000A000", "0000F0001000A000");
  addHexTest(0x000F0001000A0002, "02", "0002", "0A0002", "000A0002", "01000A0002", "0001000A0002", "0F0001000A0002", "000F0001000A0002");
  addHexTest(0x00F0001000A00020, "20", "0020", "A00020", "00A00020", "1000A00020", "001000A00020", "F0001000A00020", "00F0001000A00020");
  addHexTest(0x0F0001000A000200, "00", "0200", "000200", "0A000200", "000A000200", "01000A000200", "0001000A000200", "0F0001000A000200");
  addHexTest(0xF0001000A0002000, "00", "2000", "002000", "A0002000", "00A0002000", "1000A0002000", "001000A0002000", "F0001000A0002000");

  for (size_t i = 0; i < tests.size(); i+=9) {
    int64_t lval = (int64_t)tests0[i/9];
    int32_t ival = (int32_t)(lval & __INT64_C(0xFFFFFFFF));
    string  str  = tests[i];

    assertEquals("toHexString("+str+", 1)", tests[i+1], Utilities::toHexString(ival,1));
    assertEquals("toHexString("+str+", 2)", tests[i+2], Utilities::toHexString(ival,2));
    assertEquals("toHexString("+str+", 3)", tests[i+3], Utilities::toHexString(ival,3));
    assertEquals("toHexString("+str+", 4)", tests[i+4], Utilities::toHexString(ival,4));

    assertEquals("toHexString("+str+"L, 1)", tests[i+1], Utilities::toHexString(lval,1));
    assertEquals("toHexString("+str+"L, 2)", tests[i+2], Utilities::toHexString(lval,2));
    assertEquals("toHexString("+str+"L, 3)", tests[i+3], Utilities::toHexString(lval,3));
    assertEquals("toHexString("+str+"L, 4)", tests[i+4], Utilities::toHexString(lval,4));
    assertEquals("toHexString("+str+"L, 5)", tests[i+5], Utilities::toHexString(lval,5));
    assertEquals("toHexString("+str+"L, 6)", tests[i+6], Utilities::toHexString(lval,6));
    assertEquals("toHexString("+str+"L, 7)", tests[i+7], Utilities::toHexString(lval,7));
    assertEquals("toHexString("+str+"L, 8)", tests[i+8], Utilities::toHexString(lval,8));

    assertEquals("toHexString("+str+")",  tests[i+4], Utilities::toHexString(ival));
    assertEquals("toHexString("+str+"L)", tests[i+8], Utilities::toHexString(lval));
  }
}

static void _testToLowerCase (const string &in, const string &upper, const string &lower) {
  assertEquals(string("toUpperCase() for '")+in+"'", upper, Utilities::toUpperCase(in));
  assertEquals(string("toLowerCase() for '")+in+"'", lower, Utilities::toLowerCase(in));
}

void UtilitiesTest::testToLowerCase () {
  _testToLowerCase("ABC", "ABC", "abc");  // UPPER-CASE
  _testToLowerCase("abc", "ABC", "abc");  // LOWER-CASE
  _testToLowerCase("Abc", "ABC", "abc");  // TITLE-CASE
  _testToLowerCase("aBc", "ABC", "abc");  // MIXED-CASE
  _testToLowerCase("123", "123", "123");  // NO CHANGE (numeric)
  _testToLowerCase("!@#", "!@#", "!@#");  // NO CHANGE (symbols)
  _testToLowerCase(" \t", " \t", " \t");  // NO CHANGE (white space)
  _testToLowerCase("",    "",    ""   );  // NO CHANGE (empty string)

  _testToLowerCase("Hello World!", "HELLO WORLD!", "hello world!");
}

#define addClassIDTest(_idStr,_idNum,_ouiNum,_iccNum,_pccNum) \
  idStr.push_back(_idStr); \
  idNum.push_back(__INT64_C(_idNum));  \
  ouiNum.push_back(_ouiNum); \
  iccNum.push_back(_iccNum);  \
  pccNum.push_back(_pccNum);


void UtilitiesTest::testToStringClassID () {
  vector<string>  idStr;
  vector<int64_t> idNum;
  vector<int32_t> ouiNum;
  vector<int16_t> iccNum;
  vector<int16_t> pccNum;

  addClassIDTest("00-00-00:0000.0000", 0x0000000000000000, 0x000000, 0x0000, 0x0000);
  addClassIDTest("12-34-56:9876.5432", 0x0012345698765432, 0x123456, 0x9876, 0x5432);
  addClassIDTest("AB-CD-EF:1234.ABCD", 0x00ABCDEF1234ABCD, 0xABCDEF, 0x1234, 0xABCD);

  for (size_t i = 0; i < idStr.size(); i++) {
    assertEquals("toStringClassID(id)",          idStr[i], Utilities::toStringClassID(idNum[i]));
    assertEquals("toStringClassID(oui,icc,pcc)", idStr[i], Utilities::toStringClassID(ouiNum[i],iccNum[i],pccNum[i]));
    assertEquals("fromStringClassID(..)",        idNum[i], Utilities::fromStringClassID(idStr[i]));
  }
}

#define addDeviceIDTest(_idStr,_idNum,_ouiNum,_devNum) \
  idStr.push_back(_idStr); \
  idNum.push_back(__INT64_C(_idNum));  \
  ouiNum.push_back(_ouiNum); \
  devNum.push_back(_devNum);

void UtilitiesTest::testToStringDeviceID () {
  vector<string>  idStr;
  vector<int64_t> idNum;
  vector<int32_t> ouiNum;
  vector<int16_t> devNum;

  addDeviceIDTest("00-00-00:0000", 0x0000000000000000, 0x000000, 0x0000);
  addDeviceIDTest("12-34-56:ABCD", 0x001234560000ABCD, 0x123456, 0xABCD);
  addDeviceIDTest("AB-CD-EF:1234", 0x00ABCDEF00001234, 0xABCDEF, 0x1234);

  for (size_t i = 0; i < idStr.size(); i++) {
    assertEquals("toStringDeviceID(id)",      idStr[i], Utilities::toStringDeviceID(idNum[i]));
    assertEquals("toStringDeviceID(oui,dev)", idStr[i], Utilities::toStringDeviceID(ouiNum[i],devNum[i]));
    assertEquals("fromStringDeviceID(..)",    idNum[i], Utilities::fromStringDeviceID(idStr[i]));
  }
}

void UtilitiesTest::testToStringOUI () {
  vector<string>  ouiStr;
  vector<int32_t> ouiNum;

  ouiStr.push_back("00-00-00"); ouiNum.push_back(0x00000000);
  ouiStr.push_back("12-34-56"); ouiNum.push_back(0x00123456);
  ouiStr.push_back("AB-CD-EF"); ouiNum.push_back(0x00ABCDEF);

  for (size_t i = 0; i < ouiStr.size(); i++) {
    assertEquals("toStringOUI(..)",   ouiStr[i], Utilities::toStringOUI(ouiNum[i]));
    assertEquals("fromStringOUI(..)", ouiNum[i], Utilities::fromStringOUI(ouiStr[i]));
  }
}

void UtilitiesTest::testNormalizeAngle360 () {
  for (double i = 0; i < 720.0; i+=0.25) {
    double a   = i;
    double b   = i - 360.0;
    double c   = i + 360.0;
    double exp = fmod(i, 360.0);
    assertEquals(Utilities::format("normalizeAngle360(%g)",a), exp, Utilities::normalizeAngle360(a));
    assertEquals(Utilities::format("normalizeAngle360(%g)",b), exp, Utilities::normalizeAngle360(b));
    assertEquals(Utilities::format("normalizeAngle360(%g)",c), exp, Utilities::normalizeAngle360(c));
  }
  assertEquals("normalizeAngle360(null)", true, isNull(Utilities::normalizeAngle360(DOUBLE_NAN)));
}

void UtilitiesTest::testNormalizeAngle180 () {
  for (double i = 0; i < 720.0; i+=0.25) {
    double a   = i;
    double b   = i - 360.0;
    double c   = i + 360.0;
    double j   = Utilities::normalizeAngle360(i);
    double exp = (j < 180.0)? j : j-360.0;
    assertEquals(Utilities::format("normalizeAngle180(%g)",a), exp, Utilities::normalizeAngle180(a));
    assertEquals(Utilities::format("normalizeAngle180(%g)",b), exp, Utilities::normalizeAngle180(b));
    assertEquals(Utilities::format("normalizeAngle180(%g)",c), exp, Utilities::normalizeAngle180(c));
  }
  assertEquals("normalizeAngle180(null)", true, isNull(Utilities::normalizeAngle180(DOUBLE_NAN)));
}
