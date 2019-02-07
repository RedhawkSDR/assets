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

#include "VRTMathTest.h"
#include "BasicVRTPacket.h"
#include "TestRunner.h"
#include "TestUtilities.h"
#include "Utilities.h"

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;
using namespace Utilities;
using namespace VRTMath;

#define SPECIAL_FLOAT_COUNT  TestUtilities::SPECIAL_FLOAT_COUNT
#define SPECIAL_FLOAT_BITS   TestUtilities::SPECIAL_FLOAT_BITS
#define SPECIAL_FLOAT_VALS   TestUtilities::SPECIAL_FLOAT_VALS
#define SPECIAL_DOUBLE_COUNT TestUtilities::SPECIAL_DOUBLE_COUNT
#define SPECIAL_DOUBLE_BITS  TestUtilities::SPECIAL_DOUBLE_BITS
#define SPECIAL_DOUBLE_VALS  TestUtilities::SPECIAL_DOUBLE_VALS

string VRTMathTest::getTestedClass () {
  return "vrt::VRTMath";
}

vector<TestSet> VRTMathTest::init () {
  vector<TestSet> tests;

  // IEEE 754-2008 half-precision
  tests.push_back(TestSet("fromHalf",                   this,  "testFromHalf"));
  tests.push_back(TestSet("fromHalfInternal",           this,  "testFromHalfInternal"));
  tests.push_back(TestSet("toHalfInternal"  ,           this,  "testToHalfInternal"));
  tests.push_back(TestSet("toHalf"  ,                   this,  "testToHalf"));

  // IEEE 754-2008 single-/double-precision
  tests.push_back(TestSet("doubleToRawLongBits(..)",    this,  "testDoubleToRawLongBits"));
  tests.push_back(TestSet("floatToRawIntBits(..)",      this,  "testFloatToRawIntBits"));
  tests.push_back(TestSet("intBitsToFloat(..)",         this, "+testFloatToRawIntBits"));
  tests.push_back(TestSet("longBitsToDouble(..)",       this, "+testDoubleToRawLongBits"));

  // VRT Fixed-Point (64-bit)
  tests.push_back(TestSet("fromLong64",                 this, "+testFromDouble64"));
  tests.push_back(TestSet("fromInt64",                  this, "+testFromDouble64"));
  tests.push_back(TestSet("fromFloat64",                this, "+testFromDouble64"));
  tests.push_back(TestSet("fromDouble64",               this,  "testFromDouble64"));
  tests.push_back(TestSet("toLong64",                   this, "+testFromDouble64"));
  tests.push_back(TestSet("toInt64",                    this, "+testFromDouble64"));
  tests.push_back(TestSet("toFloat64",                  this, "+testFromDouble64"));
  tests.push_back(TestSet("toDouble64",                 this, "+testFromDouble64"));

  // VRT Fixed-Point (32-bit)
  tests.push_back(TestSet("fromLong32",                 this, "+testFromDouble32"));
  tests.push_back(TestSet("fromInt32",                  this, "+testFromDouble32"));
  tests.push_back(TestSet("fromFloat32",                this, "+testFromDouble32"));
  tests.push_back(TestSet("fromDouble32",               this,  "testFromDouble32"));
  tests.push_back(TestSet("toLong32",                   this, "+testFromDouble32"));
  tests.push_back(TestSet("toInt32",                    this, "+testFromDouble32"));
  tests.push_back(TestSet("toFloat32",                  this, "+testFromDouble32"));
  tests.push_back(TestSet("toDouble32",                 this, "+testFromDouble32"));

  // VRT Fixed-Point (16-bit)
  tests.push_back(TestSet("fromLong16",                 this, "+testFromDouble16"));
  tests.push_back(TestSet("fromInt16",                  this, "+testFromDouble16"));
  tests.push_back(TestSet("fromFloat16",                this, "+testFromDouble16"));
  tests.push_back(TestSet("fromDouble16",               this,  "testFromDouble16"));
  tests.push_back(TestSet("toLong16",                   this, "+testFromDouble16"));
  tests.push_back(TestSet("toInt16",                    this, "+testFromDouble16"));
  tests.push_back(TestSet("toFloat16",                  this, "+testFromDouble16"));
  tests.push_back(TestSet("toDouble16",                 this, "+testFromDouble16"));

  // VRT Floating-Point
  tests.push_back(TestSet("fromVRTFloat",               this,  "testVRTFloat"));
  tests.push_back(TestSet("fromVRTFloat32",             this, "+testVRTFloat"));
  tests.push_back(TestSet("fromVRTFloat64",             this, "+testVRTFloat"));
  tests.push_back(TestSet("toVRTFloat",                 this, "+testVRTFloat"));
  tests.push_back(TestSet("toVRTFloat32",               this, "+testVRTFloat"));
  tests.push_back(TestSet("toVRTFloat64",               this, "+testVRTFloat"));

  // Pack/Unpack (Integer/Floating-Point)
  tests.push_back(TestSet("packDouble",                 this,  "testPackDouble"));
  tests.push_back(TestSet("packFloat",                  this,  "testPackFloat"));
  tests.push_back(TestSet("packLong",                   this,  "testPackLong"));
  tests.push_back(TestSet("packInt",                    this,  "testPackInt"));
  tests.push_back(TestSet("packInt24",                  this,  "testPackInt24"));
  tests.push_back(TestSet("packShort",                  this,  "testPackShort"));
  tests.push_back(TestSet("packByte",                   this,  "testPackByte"));

  tests.push_back(TestSet("unpackDouble",               this,  "testUnpackDouble"));
  tests.push_back(TestSet("unpackFloat",                this,  "testUnpackFloat"));
  tests.push_back(TestSet("unpackLong",                 this,  "testUnpackLong"));
  tests.push_back(TestSet("unpackInt",                  this,  "testUnpackInt"));
  tests.push_back(TestSet("unpackInt24",                this,  "testUnpackInt24"));
  tests.push_back(TestSet("unpackShort",                this,  "testUnpackShort"));
  tests.push_back(TestSet("unpackByte",                 this,  "testUnpackByte"));

  // Pack/Unpack (Bits)
  tests.push_back(TestSet("packBits32",                 this, "+testPackBits64"));
  tests.push_back(TestSet("packBits64",                 this,  "testPackBits64"));
  tests.push_back(TestSet("unpackBits32",               this, "+testUnpackBits64"));
  tests.push_back(TestSet("unpackBits64",               this,  "testUnpackBits64"));

  // Pack/Unpack (Bytes)
  tests.push_back(TestSet("packBytes",                  this,  "testPackBytes"));
  tests.push_back(TestSet("unpackBytes",                this,  "testUnpackBytes"));

  // Pack/Unpack (Boolean)
  tests.push_back(TestSet("packBoolNull",               this,  "testPackBoolNull"));
  tests.push_back(TestSet("packBoolean",                this,  "testPackBoolean"));
  tests.push_back(TestSet("unpackBoolNull",             this,  "testUnpackBoolNull"));
  tests.push_back(TestSet("unpackBoolean",              this,  "testUnpackBoolean"));

  // Pack/Unpack (String)
  tests.push_back(TestSet("lengthUTF8",                 this, "+testPackAscii"));
  tests.push_back(TestSet("packAscii",                  this,  "testPackAscii"));
  tests.push_back(TestSet("packUTF8",                   this, "+testPackAscii"));
  tests.push_back(TestSet("unpackAscii",                this, "+testPackAscii"));
  tests.push_back(TestSet("unpackUTF8",                 this, "+testPackAscii"));

  // Pack/Unpack (Object)
  tests.push_back(TestSet("packInetAddr",               this, "+testUnpackInetAddr"));
  tests.push_back(TestSet("packMetadata",               this, "+testUnpackMetadata"));
  tests.push_back(TestSet("packRecord",                 this, "+testUnpackRecord"));
  tests.push_back(TestSet("packTimeStamp",              this, "+testUnpackTimeStamp"));
  tests.push_back(TestSet("packUUID",                   this, "+testUnpackUUID"));
  tests.push_back(TestSet("unpackInetAddr",             this,  "testUnpackInetAddr"));
  tests.push_back(TestSet("unpackMetadata",             this, "!testUnpackMetadata"));
  tests.push_back(TestSet("unpackRecord",               this, "!testUnpackRecord"));
  tests.push_back(TestSet("unpackTimeStamp",            this,  "testUnpackTimeStamp"));
  tests.push_back(TestSet("unpackUUID",                 this,  "testUnpackUUID"));

  return tests;
}

void VRTMathTest::done () {
  // nothing to do
}

void VRTMathTest::call (const string &func) {
  if (func == "testDoubleToRawLongBits" ) testDoubleToRawLongBits();
  if (func == "testFloatToRawIntBits"   ) testFloatToRawIntBits();
  if (func == "testFromDouble64"        ) testFromDouble64();
  if (func == "testFromDouble32"        ) testFromDouble32();
  if (func == "testFromDouble16"        ) testFromDouble16();
  if (func == "testFromHalf"            ) testFromHalf();
  if (func == "testFromHalfInternal"    ) testFromHalfInternal();
  if (func == "testPackAscii"           ) testPackAscii();
  if (func == "testPackBits64"          ) testPackBits64();
  if (func == "testPackBoolNull"        ) testPackBoolNull();
  if (func == "testPackBoolean"         ) testPackBoolean();
  if (func == "testPackByte"            ) testPackByte();
  if (func == "testPackBytes"           ) testPackBytes();
  if (func == "testPackDouble"          ) testPackDouble();
  if (func == "testPackFloat"           ) testPackFloat();
  if (func == "testPackInt"             ) testPackInt();
  if (func == "testPackInt24"           ) testPackInt24();
  if (func == "testPackLong"            ) testPackLong();
  if (func == "testPackShort"           ) testPackShort();
  if (func == "testToHalf"              ) testToHalf();
  if (func == "testToHalfInternal"      ) testToHalfInternal();
  if (func == "testUnpackBits64"        ) testUnpackBits64();
  if (func == "testUnpackBoolNull"      ) testUnpackBoolNull();
  if (func == "testUnpackBoolean"       ) testUnpackBoolean();
  if (func == "testUnpackByte"          ) testUnpackByte();
  if (func == "testUnpackBytes"         ) testUnpackBytes();
  if (func == "testUnpackDouble"        ) testUnpackDouble();
  if (func == "testUnpackFloat"         ) testUnpackFloat();
  if (func == "testUnpackInetAddr"      ) testUnpackInetAddr();
  if (func == "testUnpackInt"           ) testUnpackInt();
  if (func == "testUnpackInt24"         ) testUnpackInt24();
  if (func == "testUnpackLong"          ) testUnpackLong();
  if (func == "testUnpackShort"         ) testUnpackShort();
  if (func == "testUnpackTimeStamp"     ) testUnpackTimeStamp();
  if (func == "testUnpackUUID"          ) testUnpackUUID();
  if (func == "testVRTFloat"            ) testVRTFloat();
}

/** Tests the fromVRTFloat(..) and toVRTFloat(..) functions.
 *  @param fmt    The format to use.
 *  @param dSize  The data size (2..64).
 *  @param bits1  The bits.
 *  @param num    Numerator of expected value.
 *  @param denom  Denominator of expected value.
 */
static void _testVRTFloat(DataItemFormat fmt, int32_t dSize, int64_t bits1, int64_t num, int64_t denom) {
  double  val   = ((double)num) / ((double)denom);
  int64_t bits2 = toVRTFloat(fmt, dSize, val); // 'bits1' does not need to match 'bits2'
  string  msg1  = Utilities::format("fromVRTFloat(..) with %" PRId64 "/%" PRId64, num, denom);
  string  msg2  = Utilities::format(  "toVRTFloat(..) with %" PRId64 "/%" PRId64, num, denom);

  assertEquals(msg1, val, fromVRTFloat(fmt, dSize, bits1));
  assertEquals(msg2, val, fromVRTFloat(fmt, dSize, bits2));

  int64_t bits64 = toVRTFloat64(DataItemFormat_isSigned(fmt), DataItemFormat_getExponentBits(fmt), dSize, val);
  assertEquals("toVRTFloat64(..)",   bits2, bits64);
  assertEquals("fromVRTFloat64(..)", val,   fromVRTFloat(fmt, dSize, bits64));

  if (dSize <= 32) {
    int32_t bits32 = toVRTFloat32(DataItemFormat_isSigned(fmt), DataItemFormat_getExponentBits(fmt), dSize, val);
    assertEquals("toVRTFloat32(..)",   (int32_t)bits2, bits32);
    assertEquals("fromVRTFloat32(..)", val,            fromVRTFloat(fmt, dSize, bits32));
  }
}

/** Tests the toVRTFloat(..) function.
 *  @param fmt    The format to use.
 *  @param dSize  The data size (2..64).
 *  @param num    Numerator of expected value.
 *  @param denom  Denominator of expected value.
 */
static void _testVRTFloat(DataItemFormat fmt, int32_t dSize, int64_t num, int64_t denom) {
  double  val   = ((double)num) / ((double)denom);
  int64_t bits2 = toVRTFloat(fmt, dSize, val);
  string  msg2  = Utilities::format(  "toVRTFloat(..) with %" PRId64 "/%" PRId64, num, denom);

  assertEquals(msg2, val, fromVRTFloat(fmt, dSize, bits2));
}

void VRTMathTest::testVRTFloat () {
  // ==== SPECIFIED TESTS (D-1) ==============================================
  // This set of tests taken directly from ANSI/VITA 49.0 Appendix D, Table D-1
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x1F, +7,8);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x1E, +7,16);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x1D, +7,32);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x1C, +7,64);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x10, +4,64); // Table D-1 has 1/32 but should be 4/64
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x08, +1,32);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x07, +1,8);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x06, +1,16);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x05, +1,32);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x04, +1,64);

  // ==== SPECIFIED TESTS (D-2) ==============================================
  // This set of tests taken directly from ANSI/VITA 49.0 Appendix D, Table D-2
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x0F, +3,4);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x0E, +3,8);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x0D, +3,16);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x0C, +3,32);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x07, +1,4);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x06, +1,8);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x05, +1,16);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x04, +1,32);

  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x1C, -1,32);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x1D, -1,16);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x1E, -1,8);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x1F, -1,4);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x10, -1,8);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x11, -1,4);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x12, -1,2);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, 0x13, -1,1);

  // ==== VRT1 ===============================================================
  _testVRTFloat(DataItemFormat_SignedVRT1, 5, 0x1F, -1,8);
  _testVRTFloat(DataItemFormat_SignedVRT1, 5, 0x11, -1,1);
  _testVRTFloat(DataItemFormat_SignedVRT1, 5, 0x10, -1,2);

  // ==== VRT2 ===============================================================
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x07, +1,8);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x0A, +2,16);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x11, +4,32);

  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x03, +0,8);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x02, +0,16);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x01, +0,32);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, 0x00, +0,64);

  _testVRTFloat(DataItemFormat_SignedVRT2, 5, +3,32);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, +3,16);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, +3,8);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, +3,4);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, -3,32);

  _testVRTFloat(DataItemFormat_SignedVRT2, 5, -1,32);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, -1,16);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, -1,8);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, -1,4);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, -1,2);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, +1,2);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, +1,4);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, +1,8);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, +1,16);
  _testVRTFloat(DataItemFormat_SignedVRT2, 5, +1,32);

  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, +7,8);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, +7,32);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, +7,64);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, +1,32);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, +1,16);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, +1,8);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, +1,4);
  _testVRTFloat(DataItemFormat_UnsignedVRT2, 5, +1,2);

  // ==== VRT3 ===============================================================
  _testVRTFloat(DataItemFormat_SignedVRT3, 32, +1,2);
  _testVRTFloat(DataItemFormat_SignedVRT3, 32, +1,32);
  _testVRTFloat(DataItemFormat_SignedVRT3, 32, +57,256);
  _testVRTFloat(DataItemFormat_SignedVRT3, 32, +1,256);
  _testVRTFloat(DataItemFormat_SignedVRT3, 32, -1,256);
  _testVRTFloat(DataItemFormat_SignedVRT3, 32, -57,256);
  _testVRTFloat(DataItemFormat_SignedVRT3, 32, -1,32);
  _testVRTFloat(DataItemFormat_SignedVRT3, 32, -1,2);

  _testVRTFloat(DataItemFormat_UnsignedVRT3, 32, +1,2);
  _testVRTFloat(DataItemFormat_UnsignedVRT3, 32, +1,32);
  _testVRTFloat(DataItemFormat_UnsignedVRT3, 32, +57,256);
  _testVRTFloat(DataItemFormat_UnsignedVRT3, 32, +1,256);

  // ==== VRT4 ===============================================================
  _testVRTFloat(DataItemFormat_SignedVRT4, 12, -7,128);
  _testVRTFloat(DataItemFormat_SignedVRT4, 12, -1,128);
  _testVRTFloat(DataItemFormat_SignedVRT4, 12, -15,256);
  _testVRTFloat(DataItemFormat_SignedVRT4, 12, -7,256);
  _testVRTFloat(DataItemFormat_SignedVRT4, 12, -7,256);
  _testVRTFloat(DataItemFormat_SignedVRT4, 12, -15,256);
  _testVRTFloat(DataItemFormat_SignedVRT4, 12, -1,128);
  _testVRTFloat(DataItemFormat_SignedVRT4, 12, -7,128);

  _testVRTFloat(DataItemFormat_UnsignedVRT4, 12, +1,128);
  _testVRTFloat(DataItemFormat_UnsignedVRT4, 12, +7,128);
  _testVRTFloat(DataItemFormat_UnsignedVRT4, 12, +7,256);
  _testVRTFloat(DataItemFormat_UnsignedVRT4, 12, +15,256);

  // ==== VRT5 ===============================================================
  _testVRTFloat(DataItemFormat_SignedVRT5, 16, +1,32);
  _testVRTFloat(DataItemFormat_SignedVRT5, 16, +104,256);
  _testVRTFloat(DataItemFormat_SignedVRT5, 16, +1,256);
  _testVRTFloat(DataItemFormat_SignedVRT5, 16, -1,256);
  _testVRTFloat(DataItemFormat_SignedVRT5, 16, -104,256);
  _testVRTFloat(DataItemFormat_SignedVRT5, 16, -1,32);

  _testVRTFloat(DataItemFormat_UnsignedVRT5, 16, +1,256);
  _testVRTFloat(DataItemFormat_UnsignedVRT5, 16, +104,256);

  // ==== VRT6 ===============================================================
  _testVRTFloat(DataItemFormat_SignedVRT6, 16, +104,256);
  _testVRTFloat(DataItemFormat_SignedVRT6, 16, -104,256);

  _testVRTFloat(DataItemFormat_SignedVRT6, 58, +1,2);
  _testVRTFloat(DataItemFormat_SignedVRT6, 58, +104,256);
  _testVRTFloat(DataItemFormat_SignedVRT6, 58, +1,8192);
  _testVRTFloat(DataItemFormat_SignedVRT6, 58, +1,__INT64_C(4294967296));
  _testVRTFloat(DataItemFormat_SignedVRT6, 58, -1,__INT64_C(4294967296));
  _testVRTFloat(DataItemFormat_SignedVRT6, 58, -1,8192);
  _testVRTFloat(DataItemFormat_SignedVRT6, 58, -104,256);
  _testVRTFloat(DataItemFormat_SignedVRT6, 58, -1,2);

  _testVRTFloat(DataItemFormat_UnsignedVRT6, 16, +104,256);

  _testVRTFloat(DataItemFormat_UnsignedVRT6, 58, +1,2);
  _testVRTFloat(DataItemFormat_UnsignedVRT6, 58, +104,256);
  _testVRTFloat(DataItemFormat_UnsignedVRT6, 58, +1,8192);
  _testVRTFloat(DataItemFormat_UnsignedVRT6, 58, +1,__INT64_C(4294967296));

  // ==== SPECIAL CASES ======================================================
  // Special cases that previously resulted in different values in Java vs C++
  // due to issues with UInt64 constants (see VRT-40 for details).
  assertEquals("fromVRTFloat(SignedVRT6, 7, 0x40)", -1.0842021724855044E-19,
               VRTMath::fromVRTFloat(DataItemFormat_SignedVRT6, 7, 0x40));
}

const float FLOAT_NEGATIVE_INFINITY = -numeric_limits<float>::infinity();
const float FLOAT_POSITIVE_INFINITY = +numeric_limits<float>::infinity();

void VRTMathTest::testFromHalf () {
  // The following test cases based on the "Half precision examples" given on:
  //   Wikipedia. "Half-precision floating-point format."
  //   https://en.wikipedia.org/wiki/Half-precision_floating-point_format
  //   [Last Accessed 8 Aug 2013]
  // Note however, that the tests below include additional digits of precision
  // and include both positive and negative values.
  assertEquals("fromHalf(..)", FLOAT_NEGATIVE_INFINITY, fromHalf((int16_t)0xFC00)); // -Inf
  assertEquals("fromHalf(..)", -2.0f,                   fromHalf((int16_t)0xC000)); // -2.0
  assertEquals("fromHalf(..)", -1.0f,                   fromHalf((int16_t)0xBC00)); // -1.0
  assertEquals("fromHalf(..)", -6.1035156E-5f,          fromHalf((int16_t)0x8400)); // Smallest Negative Normalized
  assertEquals("fromHalf(..)", -6.0975550E-5f,          fromHalf((int16_t)0x83FF)); // Largest  Negative Denorm
  assertEquals("fromHalf(..)", -5.9604645E-8f,          fromHalf((int16_t)0x8001)); // Smallest Negative Denorm
  assertEquals("fromHalf(..)", -0.0f,                   fromHalf((int16_t)0x8000)); // -0.0
  assertEquals("fromHalf(..)", +0.0f,                   fromHalf((int16_t)0x0000)); // +0.0
  assertEquals("fromHalf(..)", +5.9604645E-8f,          fromHalf((int16_t)0x0001)); // Smallest Positive Denorm
  assertEquals("fromHalf(..)", +6.0975550E-5f,          fromHalf((int16_t)0x03FF)); // Largest  Positive Denorm
  assertEquals("fromHalf(..)", +6.1035156E-5f,          fromHalf((int16_t)0x0400)); // Smallest Positive Normalized
  assertEquals("fromHalf(..)", +1.0f,                   fromHalf((int16_t)0x3C00)); // +1.0
  assertEquals("fromHalf(..)", +2.0f,                   fromHalf((int16_t)0x4000)); // +2.0
  assertEquals("fromHalf(..)", FLOAT_POSITIVE_INFINITY, fromHalf((int16_t)0x7C00)); // +Inf

  assertEquals("fromHalf(..)", -0.33325195f,            fromHalf((int16_t)0xB555)); // -1/3
  assertEquals("fromHalf(..)", +0.33325195f,            fromHalf((int16_t)0x3555)); // +1/3

  // Additional tests for NaN
  assertEquals("fromHalf(..)", FLOAT_NAN,               fromHalf((int16_t)0x7C01)); // NaN ("Smallest"   Value)
  assertEquals("fromHalf(..)", FLOAT_NAN,               fromHalf((int16_t)0x7E00)); // NaN ("Normalized" Value)
  assertEquals("fromHalf(..)", FLOAT_NAN,               fromHalf((int16_t)0x7FFF)); // NaN ("Largest"    Value)
}

void VRTMathTest::testFromHalfInternal () {
  // The following test cases based on the "Half precision examples" given on:
  //   Wikipedia. "Half-precision floating-point format."
  //   https://en.wikipedia.org/wiki/Half-precision_floating-point_format
  //   [Last Accessed 8 Aug 2013]
  // Note however, that the tests below include additional digits of precision
  // and include both positive and negative values.
  assertEquals("_fromHalfInternal(..)", FLOAT_NEGATIVE_INFINITY, _fromHalfInternal((int16_t)0xFC00)); // -Inf
  assertEquals("_fromHalfInternal(..)", -2.0f,                   _fromHalfInternal((int16_t)0xC000)); // -2.0
  assertEquals("_fromHalfInternal(..)", -1.0f,                   _fromHalfInternal((int16_t)0xBC00)); // -1.0
  assertEquals("_fromHalfInternal(..)", -6.1035156E-5f,          _fromHalfInternal((int16_t)0x8400)); // Smallest Negative Normalized
  assertEquals("_fromHalfInternal(..)", -6.0975550E-5f,          _fromHalfInternal((int16_t)0x83FF)); // Largest  Negative Denorm
  assertEquals("_fromHalfInternal(..)", -5.9604645E-8f,          _fromHalfInternal((int16_t)0x8001)); // Smallest Negative Denorm
  assertEquals("_fromHalfInternal(..)", -0.0f,                   _fromHalfInternal((int16_t)0x8000)); // -0.0
  assertEquals("_fromHalfInternal(..)", +0.0f,                   _fromHalfInternal((int16_t)0x0000)); // +0.0
  assertEquals("_fromHalfInternal(..)", +5.9604645E-8f,          _fromHalfInternal((int16_t)0x0001)); // Smallest Positive Denorm
  assertEquals("_fromHalfInternal(..)", +6.0975550E-5f,          _fromHalfInternal((int16_t)0x03FF)); // Largest  Positive Denorm
  assertEquals("_fromHalfInternal(..)", +6.1035156E-5f,          _fromHalfInternal((int16_t)0x0400)); // Smallest Positive Normalized
  assertEquals("_fromHalfInternal(..)", +1.0f,                   _fromHalfInternal((int16_t)0x3C00)); // +1.0
  assertEquals("_fromHalfInternal(..)", +2.0f,                   _fromHalfInternal((int16_t)0x4000)); // +2.0
  assertEquals("_fromHalfInternal(..)", FLOAT_POSITIVE_INFINITY, _fromHalfInternal((int16_t)0x7C00)); // +Inf

  assertEquals("_fromHalfInternal(..)", -0.33325195f,            _fromHalfInternal((int16_t)0xB555)); // -1/3
  assertEquals("_fromHalfInternal(..)", +0.33325195f,            _fromHalfInternal((int16_t)0x3555)); // +1/3

  // Additional tests for NaN
  assertEquals("_fromHalfInternal(..)", FLOAT_NAN,               _fromHalfInternal((int16_t)0x7C01)); // NaN ("Smallest"   Value)
  assertEquals("_fromHalfInternal(..)", FLOAT_NAN,               _fromHalfInternal((int16_t)0x7E00)); // NaN ("Normalized" Value)
  assertEquals("_fromHalfInternal(..)", FLOAT_NAN,               _fromHalfInternal((int16_t)0x7FFF)); // NaN ("Largest"    Value)
}

void VRTMathTest::testToHalf () {
  // The following test cases based on the "Half precision examples" given on:
  //   Wikipedia. "Half-precision floating-point format."
  //   https://en.wikipedia.org/wiki/Half-precision_floating-point_format
  //   [Last Accessed 8 Aug 2013]
  // Note however, that the tests below include additional digits of precision
  // and include both positive and negative values.
  assertEquals("toHalf(..)", (int16_t)0xFC00, toHalf(FLOAT_NEGATIVE_INFINITY)); // -Inf
  assertEquals("toHalf(..)", (int16_t)0xC000, toHalf(-2.0f                  )); // -2.0
  assertEquals("toHalf(..)", (int16_t)0xBC00, toHalf(-1.0f                  )); // -1.0
  assertEquals("toHalf(..)", (int16_t)0x8400, toHalf(-6.1035156E-5f         )); // Smallest Negative Normalized
  assertEquals("toHalf(..)", (int16_t)0x83FF, toHalf(-6.0975550E-5f         )); // Largest  Negative Denorm
  assertEquals("toHalf(..)", (int16_t)0x8001, toHalf(-5.9604645E-8f         )); // Smallest Negative Denorm
  assertEquals("toHalf(..)", (int16_t)0x8000, toHalf(-0.0f                  )); // -0.0
  assertEquals("toHalf(..)", (int16_t)0x0000, toHalf(+0.0f                  )); // +0.0
  assertEquals("toHalf(..)", (int16_t)0x0001, toHalf(+5.9604645E-8f         )); // Smallest Positive Denorm
  assertEquals("toHalf(..)", (int16_t)0x03FF, toHalf(+6.0975550E-5f         )); // Largest  Positive Denorm
  assertEquals("toHalf(..)", (int16_t)0x0400, toHalf(+6.1035156E-5f         )); // Smallest Positive Normalized
  assertEquals("toHalf(..)", (int16_t)0x3C00, toHalf(+1.0f                  )); // +1.0
  assertEquals("toHalf(..)", (int16_t)0x4000, toHalf(+2.0f                  )); // +2.0
  assertEquals("toHalf(..)", (int16_t)0x7C00, toHalf(FLOAT_POSITIVE_INFINITY)); // +Inf

  assertEquals("toHalf(..)", (int16_t)0xB555, toHalf(-0.33325195f           )); // -1/3
  assertEquals("toHalf(..)", (int16_t)0x3555, toHalf(+0.33325195f           )); // +1/3

  // Additional tests for NaN
  int16_t h = toHalf(FLOAT_NAN);
  assertEquals("fromHalf(..)", true, (h >= (int16_t)0x7C01)); // 0x7C01 = NaN ("Smallest"   Value)
                                                              // 0x7FFF = NaN ("Largest"    Value)
}

void VRTMathTest::testToHalfInternal () {
  // The following test cases based on the "Half precision examples" given on:
  //   Wikipedia. "Half-precision floating-point format."
  //   https://en.wikipedia.org/wiki/Half-precision_floating-point_format
  //   [Last Accessed 8 Aug 2013]
  // Note however, that the tests below include additional digits of precision
  // and include both positive and negative values.
  assertEquals("_toHalfInternal(..)", (int16_t)0xFC00, _toHalfInternal(FLOAT_NEGATIVE_INFINITY)); // -Inf
  assertEquals("_toHalfInternal(..)", (int16_t)0xC000, _toHalfInternal(-2.0f                  )); // -2.0
  assertEquals("_toHalfInternal(..)", (int16_t)0xBC00, _toHalfInternal(-1.0f                  )); // -1.0
  assertEquals("_toHalfInternal(..)", (int16_t)0x8400, _toHalfInternal(-6.1035156E-5f         )); // Smallest Negative Normalized
  assertEquals("_toHalfInternal(..)", (int16_t)0x83FF, _toHalfInternal(-6.0975550E-5f         )); // Largest  Negative Denorm
  assertEquals("_toHalfInternal(..)", (int16_t)0x8001, _toHalfInternal(-5.9604645E-8f         )); // Smallest Negative Denorm
  assertEquals("_toHalfInternal(..)", (int16_t)0x8000, _toHalfInternal(-0.0f                  )); // -0.0
  assertEquals("_toHalfInternal(..)", (int16_t)0x0000, _toHalfInternal(+0.0f                  )); // +0.0
  assertEquals("_toHalfInternal(..)", (int16_t)0x0001, _toHalfInternal(+5.9604645E-8f         )); // Smallest Positive Denorm
  assertEquals("_toHalfInternal(..)", (int16_t)0x03FF, _toHalfInternal(+6.0975550E-5f         )); // Largest  Positive Denorm
  assertEquals("_toHalfInternal(..)", (int16_t)0x0400, _toHalfInternal(+6.1035156E-5f         )); // Smallest Positive Normalized
  assertEquals("_toHalfInternal(..)", (int16_t)0x3C00, _toHalfInternal(+1.0f                  )); // +1.0
  assertEquals("_toHalfInternal(..)", (int16_t)0x4000, _toHalfInternal(+2.0f                  )); // +2.0
  assertEquals("_toHalfInternal(..)", (int16_t)0x7C00, _toHalfInternal(FLOAT_POSITIVE_INFINITY)); // +Inf

  assertEquals("_toHalfInternal(..)", (int16_t)0xB555, _toHalfInternal(-0.33325195f           )); // -1/3
  assertEquals("_toHalfInternal(..)", (int16_t)0x3555, _toHalfInternal(+0.33325195f           )); // +1/3

  // Additional tests for NaN
  //  The algorithm provided by Jeroen van der Zijp suffers from a known issue
  //  where some NaN values can become Inf. As such we carefully check to make
  //  sure all NaN values are converted properly in our modified algorithm.
  for (int64_t bits = 0x7F800001; bits <= 0x7FFFFFFF; bits++) {
    // This is ~8million checks, but they execute in <0.1sec so do all of them...
    int32_t b = (int32_t)bits;
    float   f = *((float*)(void*)&b);
    int16_t h = _toHalfInternal(f);
    assertEquals("_toHalfInternal(..)", true, (h >= (int16_t)0x7C01)); // 0x7C01 = NaN ("Smallest"   Value)
                                                                       // 0x7FFF = NaN ("Largest"    Value)
  }
}

void VRTMathTest::testDoubleToRawLongBits () {
  for (size_t i = 0; i < SPECIAL_DOUBLE_COUNT; i++) {
    string num = Utilities::format("%f", SPECIAL_DOUBLE_VALS[i]);

    assertEquals(string("doubleToRawLongBits(..) for")+num, SPECIAL_DOUBLE_BITS[i], doubleToRawLongBits(SPECIAL_DOUBLE_VALS[i]));
    assertEquals(string("longBitsToDouble(..) for")+num,    SPECIAL_DOUBLE_VALS[i], longBitsToDouble(SPECIAL_DOUBLE_BITS[i]));
  }
}

void VRTMathTest::testFloatToRawIntBits () {
  for (size_t i = 0; i < SPECIAL_FLOAT_COUNT; i++) {
    string num = Utilities::format("%f", (double)SPECIAL_FLOAT_VALS[i]);

    assertEquals(string("floatToRawIntBits(..) for")+num, SPECIAL_FLOAT_BITS[i], floatToRawIntBits(SPECIAL_FLOAT_VALS[i]));
    assertEquals(string("intBitsToFloat(..) for")+num,    SPECIAL_FLOAT_VALS[i], intBitsToFloat(SPECIAL_FLOAT_BITS[i]));
  }
}

void VRTMathTest::testFromDouble64 () {
  for (int32_t radixPoint = 1; radixPoint <= 52; radixPoint++) {
    int64_t max = (numeric_limits<int64_t>::max() >> radixPoint);
    int64_t min = (numeric_limits<int64_t>::min() >> radixPoint);

    // Integer values
    for (int32_t val = -128; val <= 128; val++) {
      int64_t bits;

      if (val > max) {
        bits = __INT64_C(0x7FFFFFFFFFFFFFFF); // over range
      }
      else if (val < min) {
        bits = __INT64_C(0x8000000000000000); // below range
      }
      else {
        bits = (((int64_t)val) << radixPoint);
        assertEquals("toLong16(..)",   (int64_t)val, toLong64(  radixPoint, bits));
        assertEquals("toInt16(..)",            val, toInt64(   radixPoint, bits));
        assertEquals("toFloat16(..)",  (float )val, toFloat64( radixPoint, bits));
        assertEquals("toDouble16(..)", (double)val, toDouble64(radixPoint, bits));
      }

      assertEquals("fromLong16(..)",   bits, fromLong64(  radixPoint, (int64_t)val));
      assertEquals("fromInt16(..)",    bits, fromInt64(   radixPoint,         val));
      assertEquals("fromFloat16(..)",  bits, fromFloat64( radixPoint, (float )val));
      assertEquals("fromDouble16(..)", bits, fromDouble64(radixPoint, (double)val));
    }

    // Float values
    for (int32_t i = 0; i <= 2000; i++) {
      double  val = -1000 + (i * 0.5);
      int64_t bits;

      if (val > max) {
        bits = __INT64_C(0x7FFFFFFFFFFFFFFF); // over range
      }
      else if (val < min) {
        bits = __INT64_C(0x8000000000000000); // below range
      }
      else {
        bits = (((int64_t)(val*2)) << (radixPoint-1));
        assertEquals("toLong64(..)",   (int64_t)val, toLong64(  radixPoint, bits));
        assertEquals("toInt64(..)",    (int    )val, toInt64(   radixPoint, bits));
        assertEquals("toFloat64(..)",  (float  )val, toFloat64( radixPoint, bits));
        assertEquals("toDouble64(..)",          val, toDouble64(radixPoint, bits));
      }

      assertEquals("fromFloat64(..)",  bits, fromFloat64( radixPoint, (float)val));
      assertEquals("fromDouble64(..)", bits, fromDouble64(radixPoint,        val));
    }
  }
}

void VRTMathTest::testFromDouble32 () {
  for (int32_t radixPoint = 1; radixPoint <= 28; radixPoint++) {
    int32_t max = (numeric_limits<int32_t>::max() >> radixPoint);
    int32_t min = (numeric_limits<int32_t>::min() >> radixPoint);

    // Integer values
    for (int32_t val = -128; val <= 128; val++) {
      int32_t bits;

      if (val > max) {
        bits = 0x7FFFFFFF; // over range
      }
      else if (val < min) {
        bits = 0x80000000; // below range
      }
      else {
        bits = (val << radixPoint);
        assertEquals("toLong32(..)",   (int64_t)val, toLong32(  radixPoint, bits));
        assertEquals("toInt32(..)",             val, toInt32(   radixPoint, bits));
        assertEquals("toFloat32(..)",  (float  )val, toFloat32( radixPoint, bits));
        assertEquals("toDouble32(..)", (double )val, toDouble32(radixPoint, bits));
      }

      assertEquals("fromLong32(..)",   bits, fromLong32(  radixPoint, (int64_t)val));
      assertEquals("fromInt32(..)",    bits, fromInt32(   radixPoint,          val));
      assertEquals("fromFloat32(..)",  bits, fromFloat32( radixPoint, (float  )val));
      assertEquals("fromDouble32(..)", bits, fromDouble32(radixPoint, (double )val));
    }

    // Float values
    for (int32_t i = 0; i <= 2000; i++) {
      double  val = -1000 + (i * 0.5);
      int32_t bits;

      if (val > max) {
        bits = 0x7FFFFFFF; // over range
      }
      else if (val < min) {
        bits = 0x80000000; // below range
      }
      else {
        bits = (((int32_t)(val*2)) << (radixPoint-1));
        assertEquals("toLong32(..)",   (int64_t)val, toLong32(  radixPoint, bits));
        assertEquals("toInt32(..)",    (int32_t)val, toInt32(   radixPoint, bits));
        assertEquals("toFloat32(..)",  (float  )val, toFloat32( radixPoint, bits));
        assertEquals("toDouble32(..)",          val, toDouble32(radixPoint, bits));
      }

      assertEquals("fromFloat32(..)",  bits, fromFloat32( radixPoint, (float)val));
      assertEquals("fromDouble32(..)", bits, fromDouble32(radixPoint,        val));
    }
  }
}

void VRTMathTest::testFromDouble16 () {
  for (int32_t radixPoint = 1; radixPoint <= 12; radixPoint++) {
    int32_t max = (numeric_limits<int16_t>::max() >> radixPoint);
    int32_t min = (numeric_limits<int16_t>::min() >> radixPoint);

    // Integer values
    for (int32_t val = -128; val <= 128; val++) {
      int16_t bits;

      if (val > max) {
        bits = (int16_t)0x7FFF; // over range
      }
      else if (val < min) {
        bits = (int16_t)0x8000; // below range
      }
      else {
        bits = (int16_t)(val << radixPoint);
        assertEquals("toLong16(..)",   (int64_t)val, toLong16(  radixPoint, bits));
        assertEquals("toInt16(..)",             val, toInt16(   radixPoint, bits));
        assertEquals("toFloat16(..)",  (float  )val, toFloat16( radixPoint, bits));
        assertEquals("toDouble16(..)", (double )val, toDouble16(radixPoint, bits));
      }

      assertEquals("fromLong16(..)",   bits, fromLong16(  radixPoint, (int64_t)val));
      assertEquals("fromInt16(..)",    bits, fromInt16(   radixPoint,          val));
      assertEquals("fromFloat16(..)",  bits, fromFloat16( radixPoint, (float  )val));
      assertEquals("fromDouble16(..)", bits, fromDouble16(radixPoint, (double )val));
    }

    // Float values
    for (int32_t i = 0; i <= 2000; i++) {
      double  val = -1000 + (i * 0.5);
      int16_t bits;

      if (val > max) {
        bits = (int16_t)0x7FFF; // over range
      }
      else if (val < min) {
        bits = (int16_t)0x8000; // below range
      }
      else {
        bits = (int16_t)(((int32_t)(val*2)) << (radixPoint-1));
        assertEquals("toLong16(..)",   (int64_t)val, toLong16(  radixPoint, bits));
        assertEquals("toInt16(..)",    (int32_t)val, toInt16(   radixPoint, bits));
        assertEquals("toFloat16(..)",  (float  )val, toFloat16( radixPoint, bits));
        assertEquals("toDouble16(..)",          val, toDouble16(radixPoint, bits));
      }

      assertEquals("fromFloat16(..)",  bits, fromFloat16( radixPoint, (float)val));
      assertEquals("fromDouble16(..)", bits, fromDouble16(radixPoint,        val));
    }
  }
}

/** Trivial unpackBits(..) function that is easier to demonstrate accurate.
 *  @param ptr       The buffer to read from.
 *  @param bitOffset The bit offset into the buffer to start at.
 *  @param numBits   The number of bits to read in.
 */
static int64_t getBits (void *ptr, int64_t bitOffset, int64_t numBits) {
  char *buf = (char*)ptr;
  int64_t val = 0;
  for (int32_t i = 0; i < numBits; i++,bitOffset++) {
    int32_t off = (int32_t)(bitOffset / 8);
    int32_t bit = (int32_t)(bitOffset % 8);

    switch (bit) {
      case 0: val = (val << 1) | ((buf[off] >> 7) & 0x1); break;
      case 1: val = (val << 1) | ((buf[off] >> 6) & 0x1); break;
      case 2: val = (val << 1) | ((buf[off] >> 5) & 0x1); break;
      case 3: val = (val << 1) | ((buf[off] >> 4) & 0x1); break;
      case 4: val = (val << 1) | ((buf[off] >> 3) & 0x1); break;
      case 5: val = (val << 1) | ((buf[off] >> 2) & 0x1); break;
      case 6: val = (val << 1) | ((buf[off] >> 1) & 0x1); break;
      case 7: val = (val << 1) | ((buf[off]     ) & 0x1); break;
    }
  }
  return val;
}

void VRTMathTest::testUnpackBits64 () {
  char buf[24] = { (char)0x01, (char)0x23, (char)0x45, (char)0x67,
                   (char)0x89, (char)0xAB, (char)0xCD, (char)0xEF,
                   (char)0x00, (char)0x11, (char)0x22, (char)0x33,
                   (char)0x44, (char)0x55, (char)0x66, (char)0x77,
                   (char)0x88, (char)0x99, (char)0xAA, (char)0xBB,
                   (char)0xCC, (char)0xDD, (char)0xEE, (char)0xFF };

  int32_t totalBits = 24 * 8;

  for (int32_t bitOffset = 0; bitOffset < totalBits; bitOffset+=5) {
    for (int32_t numBits = 1; (numBits <= 64) && (numBits < (totalBits - bitOffset)); numBits++) {
      int64_t exp64 = getBits(buf, bitOffset, numBits);
      int64_t act64 = VRTMath::unpackBits64(buf, bitOffset, numBits);

      assertEquals("unpackBits64(..)", exp64, act64);

      if (numBits <= 32) {
        int32_t exp32 = (int)exp64;
        int32_t act32 = VRTMath::unpackBits32(buf, bitOffset, numBits);
        assertEquals("unpackBits32(..)", exp32, act32);
      }
    }
  }
}

void VRTMathTest::testPackBits64 () {
  int64_t bits[4] = { __INT64_C(0x0000000000000000), __INT64_C(0x0123456789ABCDEF),
                      __INT64_C(0x0011223344556677), (int64_t)__INT64_C(0x8899AABBCCDDEEFFU) };
  int32_t totalBits = 4 * 64;

  for (int32_t bitOffset = 0; bitOffset < totalBits; bitOffset+=5) {
    for (int32_t numBits = 1; (numBits <= 64) && (numBits < (totalBits - bitOffset)); numBits++) {
      for (size_t i = 0; i < 4; i++) {
        int64_t value = bits[i];
        int64_t mask  = (numBits == 64)? -1 : ~(__INT64_C(-1) << numBits);
        int64_t exp64 = value & mask;

        char buf64[totalBits/8]; memset(buf64, 0, totalBits/8);
        char buf32[totalBits/8]; memset(buf32, 0, totalBits/8);

        VRTMath::packBits64(buf64, bitOffset, numBits, value);
        assertEquals("packBits64(..)", exp64, VRTMath::unpackBits64(buf64, bitOffset, numBits));

        if (numBits <= 32) {
          VRTMath::packBits32(buf32, bitOffset, numBits, (int32_t)value);
          assertArrayEquals("packBits32(..)", buf64, totalBits/8, buf32, totalBits/8);
        }
      }
    }
  }
}

void VRTMathTest::testPackBytes () {
  char data[8] = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };
  char exp[8]  = { 0x0A, 0x0B, 0x01, 0x02, 0x03, 0x04, 0x0C, 0x0D };
  vector<char> act(8);
  act[0] = 0x0A; act[1] = 0x0B; act[2] = 0x11; act[3] = 0x22;
  act[4] = 0x33; act[5] = 0x44; act[6] = 0x0C; act[7] = 0x0D;

  packBytes(act, 2, &data[1], 4);
  assertBufEquals("packBytes(..)", exp, 8, &act[0], 8);
}

void VRTMathTest::testUnpackBytes () {
  vector<char> data(8);
  data[0] = 0x0A; data[1] = 0x0B; data[2] = 0x01; data[3] = 0x02;
  data[4] = 0x03; data[5] = 0x04; data[6] = 0x0C; data[7] = 0x0D;

  char exp[8]  = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };
  char act[8]  = { 0x00, 0x11, 0x22, 0x33, 0x44, 0x05, 0x06, 0x07 };

  unpackBytes(data, 2, &act[1], 4);
  assertBufEquals("unpackBytes(..)", exp, 8, act, 8);
}

void VRTMathTest::testUnpackByte () {
  char data[8] = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };

  for (int32_t i = 0; i < 8; i++) {
    assertEquals("unpackByte(..)", data[i], unpackByte(data, i));
  }
}

void VRTMathTest::testPackByte () {
  char data[8];

  for (int32_t i = 0; i < 8; i++) {
    packByte(data, i, (char)i);
  }
  for (int32_t i = 0; i < 8; i++) {
    assertEquals("packByte(..)", (char)i, data[i]);
  }
}

void VRTMathTest::testUnpackShort () {
  char data[8] = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };

  assertEquals("unpackShort(..)", (int16_t)0x0001, unpackShort(data, 0));
  assertEquals("unpackShort(..)", (int16_t)0x0001, unpackShort(data, 0, BIG_ENDIAN));
  assertEquals("unpackShort(..)", (int16_t)0x0100, unpackShort(data, 0, LITTLE_ENDIAN));

  assertEquals("unpackShort(..)", (int16_t)0x0203, unpackShort(data, 2));
  assertEquals("unpackShort(..)", (int16_t)0x0203, unpackShort(data, 2, BIG_ENDIAN));
  assertEquals("unpackShort(..)", (int16_t)0x0302, unpackShort(data, 2, LITTLE_ENDIAN));

  assertEquals("unpackShort(..)", (int16_t)0x0405, unpackShort(data, 4));
  assertEquals("unpackShort(..)", (int16_t)0x0405, unpackShort(data, 4, BIG_ENDIAN));
  assertEquals("unpackShort(..)", (int16_t)0x0504, unpackShort(data, 4, LITTLE_ENDIAN));

  assertEquals("unpackShort(..)", (int16_t)0x0607, unpackShort(data, 6));
  assertEquals("unpackShort(..)", (int16_t)0x0607, unpackShort(data, 6, BIG_ENDIAN));
  assertEquals("unpackShort(..)", (int16_t)0x0706, unpackShort(data, 6, LITTLE_ENDIAN));
}

void VRTMathTest::testPackShort () {
  char data[8]  = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };
  char data1[8];
  char data2[8];
  char data3[8];

  packShort(data1, 0, (int16_t)0x0001);
  packShort(data2, 0, (int16_t)0x0001, BIG_ENDIAN);
  packShort(data3, 0, (int16_t)0x0100, LITTLE_ENDIAN);

  packShort(data1, 2, (int16_t)0x0203);
  packShort(data2, 2, (int16_t)0x0203, BIG_ENDIAN);
  packShort(data3, 2, (int16_t)0x0302, LITTLE_ENDIAN);

  packShort(data1, 4, (int16_t)0x0405);
  packShort(data2, 4, (int16_t)0x0405, BIG_ENDIAN);
  packShort(data3, 4, (int16_t)0x0504, LITTLE_ENDIAN);

  packShort(data1, 6, (int16_t)0x0607);
  packShort(data2, 6, (int16_t)0x0607, BIG_ENDIAN);
  packShort(data3, 6, (int16_t)0x0706, LITTLE_ENDIAN);

  assertBufEquals("packShort(..)", data, 8, data1, 8);
  assertBufEquals("packShort(..)", data, 8, data2, 8);
  assertBufEquals("packShort(..)", data, 8, data3, 8);
}

void VRTMathTest::testUnpackInt24 () {
  char data[9] = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };

  assertEquals("unpackInt24(..)", 0x000102, unpackInt24(data, 0));
  assertEquals("unpackInt24(..)", 0x000102, unpackInt24(data, 0, BIG_ENDIAN));
  assertEquals("unpackInt24(..)", 0x020100, unpackInt24(data, 0, LITTLE_ENDIAN));

  assertEquals("unpackInt24(..)", 0x030405, unpackInt24(data, 3));
  assertEquals("unpackInt24(..)", 0x030405, unpackInt24(data, 3, BIG_ENDIAN));
  assertEquals("unpackInt24(..)", 0x050403, unpackInt24(data, 3, LITTLE_ENDIAN));

  assertEquals("unpackInt24(..)", 0x060708, unpackInt24(data, 6));
  assertEquals("unpackInt24(..)", 0x060708, unpackInt24(data, 6, BIG_ENDIAN));
  assertEquals("unpackInt24(..)", 0x080706, unpackInt24(data, 6, LITTLE_ENDIAN));
}

void VRTMathTest::testPackInt24 () {
  char data[9]  = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };
  char data1[9];
  char data2[9];
  char data3[9];

  packInt24(data1, 0, 0x000102);
  packInt24(data2, 0, 0x000102, BIG_ENDIAN);
  packInt24(data3, 0, 0x020100, LITTLE_ENDIAN);

  packInt24(data1, 3, 0x030405);
  packInt24(data2, 3, 0x030405, BIG_ENDIAN);
  packInt24(data3, 3, 0x050403, LITTLE_ENDIAN);

  packInt24(data1, 6, 0x060708);
  packInt24(data2, 6, 0x060708, BIG_ENDIAN);
  packInt24(data3, 6, 0x080706, LITTLE_ENDIAN);

  assertBufEquals("packInt24(..)", data, 9, data1, 9);
  assertBufEquals("packInt24(..)", data, 9, data2, 9);
  assertBufEquals("packInt24(..)", data, 9, data3, 9);
}

void VRTMathTest::testUnpackInt () {
  char data[8] = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };

  assertEquals("unpackInt(..)", 0x00010203, unpackInt(data, 0));
  assertEquals("unpackInt(..)", 0x00010203, unpackInt(data, 0, BIG_ENDIAN));
  assertEquals("unpackInt(..)", 0x03020100, unpackInt(data, 0, LITTLE_ENDIAN));

  assertEquals("unpackInt(..)", 0x04050607, unpackInt(data, 4));
  assertEquals("unpackInt(..)", 0x04050607, unpackInt(data, 4, BIG_ENDIAN));
  assertEquals("unpackInt(..)", 0x07060504, unpackInt(data, 4, LITTLE_ENDIAN));
}

void VRTMathTest::testPackInt () {
  char data[8]  = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };
  char data1[8];
  char data2[8];
  char data3[8];

  packInt(data1, 0, 0x00010203);
  packInt(data2, 0, 0x00010203, BIG_ENDIAN);
  packInt(data3, 0, 0x03020100, LITTLE_ENDIAN);

  packInt(data1, 4, 0x04050607);
  packInt(data2, 4, 0x04050607, BIG_ENDIAN);
  packInt(data3, 4, 0x07060504, LITTLE_ENDIAN);

  assertBufEquals("packInt(..)", data, 8, data1, 8);
  assertBufEquals("packInt(..)", data, 8, data2, 8);
  assertBufEquals("packInt(..)", data, 8, data3, 8);
}

void VRTMathTest::testUnpackLong () {
  char data[16] = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                    0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };

  assertEquals("unpackLong(..)", __INT64_C(0x0001020304050607), unpackLong(data, 0));
  assertEquals("unpackLong(..)", __INT64_C(0x0001020304050607), unpackLong(data, 0, BIG_ENDIAN));
  assertEquals("unpackLong(..)", __INT64_C(0x0706050403020100), unpackLong(data, 0, LITTLE_ENDIAN));

  assertEquals("unpackLong(..)", __INT64_C(0x08090A0B0C0D0E0F), unpackLong(data, 8));
  assertEquals("unpackLong(..)", __INT64_C(0x08090A0B0C0D0E0F), unpackLong(data, 8, BIG_ENDIAN));
  assertEquals("unpackLong(..)", __INT64_C(0x0F0E0D0C0B0A0908), unpackLong(data, 8, LITTLE_ENDIAN));
}

void VRTMathTest::testPackLong () {
  char data[16] = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                    0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };
  char data1[16];
  char data2[16];
  char data3[16];

  packLong(data1, 0, __INT64_C(0x0001020304050607));
  packLong(data2, 0, __INT64_C(0x0001020304050607), BIG_ENDIAN);
  packLong(data3, 0, __INT64_C(0x0706050403020100), LITTLE_ENDIAN);

  packLong(data1, 8, __INT64_C(0x08090A0B0C0D0E0F));
  packLong(data2, 8, __INT64_C(0x08090A0B0C0D0E0F), BIG_ENDIAN);
  packLong(data3, 8, __INT64_C(0x0F0E0D0C0B0A0908), LITTLE_ENDIAN);

  assertBufEquals("packLong(..)", data, 16, data1, 16);
  assertBufEquals("packLong(..)", data, 16, data2, 16);
  assertBufEquals("packLong(..)", data, 16, data3, 16);
}

void VRTMathTest::testUnpackFloat () {
  char data[8] = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };

  assertEquals("unpackFloat(..)", intBitsToFloat(0x00010203), unpackFloat(data, 0));
  assertEquals("unpackFloat(..)", intBitsToFloat(0x00010203), unpackFloat(data, 0, BIG_ENDIAN));
  assertEquals("unpackFloat(..)", intBitsToFloat(0x03020100), unpackFloat(data, 0, LITTLE_ENDIAN));

  assertEquals("unpackFloat(..)", intBitsToFloat(0x04050607), unpackFloat(data, 4));
  assertEquals("unpackFloat(..)", intBitsToFloat(0x04050607), unpackFloat(data, 4, BIG_ENDIAN));
  assertEquals("unpackFloat(..)", intBitsToFloat(0x07060504), unpackFloat(data, 4, LITTLE_ENDIAN));
}

void VRTMathTest::testPackFloat () {
  char data[8]  = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };
  char data1[8];
  char data2[8];
  char data3[8];

  packFloat(data1, 0, intBitsToFloat(0x00010203));
  packFloat(data2, 0, intBitsToFloat(0x00010203), BIG_ENDIAN);
  packFloat(data3, 0, intBitsToFloat(0x03020100), LITTLE_ENDIAN);

  packFloat(data1, 4, intBitsToFloat(0x04050607));
  packFloat(data2, 4, intBitsToFloat(0x04050607), BIG_ENDIAN);
  packFloat(data3, 4, intBitsToFloat(0x07060504), LITTLE_ENDIAN);

  assertBufEquals("packFloat(..)", data, 8, data1, 8);
  assertBufEquals("packFloat(..)", data, 8, data2, 8);
  assertBufEquals("packFloat(..)", data, 8, data3, 8);
}

void VRTMathTest::testUnpackDouble () {
  char data[16] = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                    0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };

  assertEquals("unpackDouble(..)", longBitsToDouble(__INT64_C(0x0001020304050607)), unpackDouble(data, 0));
  assertEquals("unpackDouble(..)", longBitsToDouble(__INT64_C(0x0001020304050607)), unpackDouble(data, 0, BIG_ENDIAN));
  assertEquals("unpackDouble(..)", longBitsToDouble(__INT64_C(0x0706050403020100)), unpackDouble(data, 0, LITTLE_ENDIAN));

  assertEquals("unpackDouble(..)", longBitsToDouble(__INT64_C(0x08090A0B0C0D0E0F)), unpackDouble(data, 8));
  assertEquals("unpackDouble(..)", longBitsToDouble(__INT64_C(0x08090A0B0C0D0E0F)), unpackDouble(data, 8, BIG_ENDIAN));
  assertEquals("unpackDouble(..)", longBitsToDouble(__INT64_C(0x0F0E0D0C0B0A0908)), unpackDouble(data, 8, LITTLE_ENDIAN));
}

void VRTMathTest::testPackDouble () {
  char data[16] = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                    0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };
  char data1[16];
  char data2[16];
  char data3[16];

  packDouble(data1, 0, longBitsToDouble(__INT64_C(0x0001020304050607)));
  packDouble(data2, 0, longBitsToDouble(__INT64_C(0x0001020304050607)), BIG_ENDIAN);
  packDouble(data3, 0, longBitsToDouble(__INT64_C(0x0706050403020100)), LITTLE_ENDIAN);

  packDouble(data1, 8, longBitsToDouble(__INT64_C(0x08090A0B0C0D0E0F)));
  packDouble(data2, 8, longBitsToDouble(__INT64_C(0x08090A0B0C0D0E0F)), BIG_ENDIAN);
  packDouble(data3, 8, longBitsToDouble(__INT64_C(0x0F0E0D0C0B0A0908)), LITTLE_ENDIAN);

  assertBufEquals("packDouble(..)", data, 16, data1, 16);
  assertBufEquals("packDouble(..)", data, 16, data2, 16);
  assertBufEquals("packDouble(..)", data, 16, data3, 16);
}

void VRTMathTest::testUnpackBoolean () {
  char data[5] = { -2, -1, 0, +1, +2 };

  if (VRTConfig::getStrict()) {
    try {
      unpackBoolean(data, 0);
      assertEquals("unpackBoolean(..) for -2 causes exception", true, false);
    }
    catch (VRTException e) {
      UNUSED_VARIABLE(e);
      assertEquals("unpackBoolean(..) for -2 causes exception", true, true);
    }
    assertEquals("unpackBoolean(..)", false, unpackBoolean(data, 1));
    assertEquals("unpackBoolean(..)", false, unpackBoolean(data, 2));
    assertEquals("unpackBoolean(..)", true,  unpackBoolean(data, 3));
    try {
      unpackBoolean(data, 4);
      assertEquals("unpackBoolean(..) for +2 causes exception", true, false);
    }
    catch (VRTException e) {
      UNUSED_VARIABLE(e);
      assertEquals("unpackBoolean(..) for +2 causes exception", true, true);
    }
  }
  else {
    assertEquals("unpackBoolean(..)", false, unpackBoolean(data, 0));
    assertEquals("unpackBoolean(..)", false, unpackBoolean(data, 1));
    assertEquals("unpackBoolean(..)", false, unpackBoolean(data, 2));
    assertEquals("unpackBoolean(..)", true,  unpackBoolean(data, 3));
    assertEquals("unpackBoolean(..)", true,  unpackBoolean(data, 4));
  }
}

void VRTMathTest::testPackBoolean () {
  char exp[4] = { -1, +1, -1, +1 };
  char act[4];

  packBoolean(act, 0, false);
  packBoolean(act, 1, true);
  packBoolean(act, 2, false);
  packBoolean(act, 3, true);

  assertBufEquals("packBoolean(..)", exp, 4, act, 4);
}

void VRTMathTest::testUnpackBoolNull () {
  char data[5] = { -2, -1, 0, +1, +2 };

  if (VRTConfig::getStrict()) {
    try {
      unpackBoolNull(data, 0);
      assertEquals("unpackBoolNull(..) for -2 causes exception", true, false);
    }
    catch (VRTException e) {
      UNUSED_VARIABLE(e);
      assertEquals("unpackBoolNull(..) for -2 causes exception", true, true);
    }
    assertEquals("unpackBoolNull(..)", _FALSE, unpackBoolNull(data, 1));
    assertEquals("unpackBoolNull(..)", _NULL,  unpackBoolNull(data, 2));
    assertEquals("unpackBoolNull(..)", _TRUE,  unpackBoolNull(data, 3));
    try {
      unpackBoolNull(data, 4);
      assertEquals("unpackBoolNull(..) for +2 causes exception", true, false);
    }
    catch (VRTException e) {
      UNUSED_VARIABLE(e);
      assertEquals("unpackBoolNull(..) for +2 causes exception", true, true);
    }
  }
  else {
    assertEquals("unpackBoolNull(..)", _FALSE, unpackBoolNull(data, 0));
    assertEquals("unpackBoolNull(..)", _FALSE, unpackBoolNull(data, 1));
    assertEquals("unpackBoolNull(..)", _NULL,  unpackBoolNull(data, 2));
    assertEquals("unpackBoolNull(..)", _TRUE,  unpackBoolNull(data, 3));
    assertEquals("unpackBoolNull(..)", _TRUE,  unpackBoolNull(data, 4));
  }
}

void VRTMathTest::testPackBoolNull () {
  char exp[3] = { -1, 0, +1 };
  char act[3];

  packBoolNull(act, 0, _FALSE);
  packBoolNull(act, 1, _NULL);
  packBoolNull(act, 2, _TRUE);

  assertBufEquals("packBoolNull(..)", exp, 3, act, 3);
}

template <typename T>
static void _testText (T txt, char *ascii, int32_t ascii_length,
                              char *utf8,  int32_t utf8_length,
                              const string &_txtASCII,
                              const wstring &_txtUTF8) {
  int32_t actASCII_length = ascii_length + 8;
  int32_t actUTF8_length  = utf8_length + 8;

  for (int32_t offset = 0; offset <= 4; offset++) {
    char expASCII[actASCII_length];
    char actASCII[actASCII_length];
    char expUTF8[actUTF8_length];
    char actUTF8[actUTF8_length];

    memset(expASCII,  0, actASCII_length);
    memset(expASCII, -1, offset);
    memset(actASCII, -1, actASCII_length);
    memset(expUTF8,   0, actUTF8_length);
    memset(expUTF8,  -1, offset);
    memset(actUTF8,  -1, actUTF8_length);

    memcpy(&expASCII[offset], ascii, ascii_length);
    memcpy(&expUTF8[offset],  utf8,  utf8_length);

    int32_t lenASCII = packAscii(actASCII, offset, txt, actASCII_length-offset);
    int32_t lenUTF8  = packUTF8( actUTF8,  offset, txt, actUTF8_length-offset);

    string  txtASCII = unpackAscii(actASCII, offset, actASCII_length-offset);
    wstring txtUTF8  = unpackUTF8( actUTF8,  offset, actUTF8_length-offset);

    assertEquals(   "packAscii(..)",  ascii_length, lenASCII);
    assertEquals(   "packUTF8(..)",   utf8_length,  lenUTF8 );
    assertEquals(   "lengthUTF8(..)", utf8_length,  lengthUTF8(txt));
    assertBufEquals("packAscii(..)",  expASCII, actASCII_length, actASCII, actASCII_length);
    assertBufEquals("packUTF8(..)",   actUTF8,  actUTF8_length,  actUTF8,  actUTF8_length);
    assertEquals( "unpackAscii(..)",  _txtASCII,    txtASCII);
    assertEquals( "unpackUTF8(..)",   _txtUTF8,     txtUTF8);
  }
}

static void testText (const string &txt, char *ascii, size_t ascii_length,
                                         char *utf8,  size_t utf8_length) {
  wchar_t _txt[utf8_length+1];
  for (size_t i = 0; i < ascii_length; i++) {
    _txt[i] = ascii[i];
  }
  _txt[utf8_length] = 0;

  _testText(txt, ascii, (int32_t)ascii_length, utf8, (int32_t)utf8_length, txt, _txt);
}

static void testText (wstring txt, char *ascii, size_t ascii_length,
                                   char *utf8,  size_t utf8_length) {
  char _txt[ascii_length+1];
  memcpy(&_txt[0], ascii, ascii_length);
  _txt[ascii_length] = '\0';

  _testText(txt, ascii, (int32_t)ascii_length, utf8, (int32_t)utf8_length, _txt, txt);
}

void VRTMathTest::testPackAscii () {
  // ASCII STRINGS:
  string helloWorld = "Hello World!";
  testText(helloWorld,      &helloWorld[0], helloWorld.size(), &helloWorld[0], helloWorld.size());
  testText(L"Hello World!", &helloWorld[0], helloWorld.size(), &helloWorld[0], helloWorld.size());

  // UNICODE STRINGS:
  //  U+0000B2 = SUPERSCRIPT_TWO                     (2-octet)
  //  U+0003BC = GREEK SMALL LETTER MU               (2-octet)
  //  U+002212 = MINUS SIGN (Mathematical Operators) (3-octet)
  //  U+01D49E = MATHEMATICAL SCRIPT CAPITAL C       (4-octet)
  char ascii_1[4] = { 'm', '/', 's', '?' };
  char utf8_1[5]  = { 'm', '/',  's',  (char)0xC2, (char)0xB2 };
  testText(L"m/s\u00B2", ascii_1, 4, utf8_1, 5);  // m/s^2

  char ascii_2[4] = { '?', 's', 'e', 'c' };
  char utf8_2[5]  = { (char)0xCE, (char)0xBC, 's',  'e',  'c' };
  testText(L"\u03BCsec", ascii_2, 4, utf8_2, 5);  // usec

  char ascii_3[3] = { '?', '4', '2' };
  char utf8_3[5]  = { (char)0xE2, (char)0x88, (char)0x92, '4',  '2' };
  testText(L"\u221242", ascii_3, 3, utf8_3, 5);  // -42

  wchar_t txt_4[2]   = { 0x01D49E, 0 };
  char    ascii_4[2] = { '?' };
  char    utf8_4[4]  = { (char)0xF0, (char)0x9D, (char)0x92, (char)0x9E };
  testText(txt_4, ascii_4, 1, utf8_4, 4);
}

void VRTMathTest::testUnpackUUID () {
  UUID uuid1; uuid1.setUUID("091e6a58-5379-4686-bd2e-60427bdd6c5e");
  UUID uuid2; uuid2.setUUID("61e09d02-6d6b-4634-9e73-f2c8b5f813e0");
  char exp1[24] = { 0, 0, 0, 0,
                    (char)0x09, (char)0x1e, (char)0x6a, (char)0x58,
                    (char)0x53, (char)0x79, (char)0x46, (char)0x86,
                    (char)0xbd, (char)0x2e, (char)0x60, (char)0x42,
                    (char)0x7b, (char)0xdd, (char)0x6c, (char)0x5e,
                    0, 0, 0, 0 };
  char exp2[16] = { (char)0x61, (char)0xe0, (char)0x9d, (char)0x02,
                    (char)0x6d, (char)0x6b, (char)0x46, (char)0x34,
                    (char)0x9e, (char)0x73, (char)0xf2, (char)0xc8,
                    (char)0xb5, (char)0xf8, (char)0x13, (char)0xe0 };
  char act1[24]; memset(act1, 0, 24);
  char act2[16]; memset(act1, 0, 16);

  assertEquals("unpackUUID(..)", uuid1, unpackUUID(exp1, 4));
  assertEquals("unpackUUID(..)", uuid2, unpackUUID(exp2, 0));

  packUUID(act1, 4, uuid1);
  packUUID(act2, 0, uuid2);

  assertBufEquals("packUUID(..)", exp1, 24, act1, 24);
  assertBufEquals("packUUID(..)", exp2, 16, act2, 16);
}

void VRTMathTest::testUnpackTimeStamp () {
  vector<TimeStamp> timeStamps;
  timeStamps.push_back(TimeStamp::Y2K_GPS);
  timeStamps.push_back(TimeStamp::parseTime("2013-02-14T01:02:03.456", TimeStamp::UTC_EPOCH));
  timeStamps.push_back(TimeStamp::parseTime("2013-02-14T01:02:03.456", TimeStamp::GPS_EPOCH));

  for (size_t i = 0; i < timeStamps.size(); i++) {
    TimeStamp ts = timeStamps[i];
    char expG[20]; memset(expG, 0, 20);
    char expU[20]; memset(expU, 0, 20);
    char actG[20]; memset(actG, 0, 20);
    char actU[20]; memset(actU, 0, 20);

    packInt(expG, 2, ts.getSecondsGPS());
    packInt(expU, 2, ts.getSecondsUTC());
    packLong(expG, 6, ts.getPicoSeconds());
    packLong(expU, 6, ts.getPicoSeconds());

    assertEquals("unpackTimeStamp(..)", ts.toGPS(), unpackTimeStamp(expG, 2, TimeStamp::GPS_EPOCH));
    assertEquals("unpackTimeStamp(..)", ts.toUTC(), unpackTimeStamp(expU, 2, TimeStamp::UTC_EPOCH));

    packTimeStamp(actG, 2, ts, TimeStamp::GPS_EPOCH);
    packTimeStamp(actU, 2, ts, TimeStamp::UTC_EPOCH);

    assertBufEquals("packTimeStamp(..)", expG, 20, actG, 20);
    assertBufEquals("packTimeStamp(..)", expU, 20, actU, 20);
  }
}

void VRTMathTest::testUnpackInetAddr () {
  vector<string> addresses;
  addresses.push_back("0.0.0.0");
  addresses.push_back("::FFFF:0.0.0.0");
  addresses.push_back("::0");
  addresses.push_back("127.0.0.1");
  addresses.push_back("::FFFF:127.0.0.1");
  addresses.push_back("10.10.255.1");
  addresses.push_back("::FFFF:10.10.255.1");
  addresses.push_back("::FFFF:0A0A:FF01");

  for (size_t i = 0; i < addresses.size(); i++) {
    InetAddress     inetAddr(addresses[i]);
    struct in6_addr ipv6 = inetAddr.toIPv6();
    char            exp[20]; memset(exp, 0, 20);
    char            act[20]; memset(act, 0, 20);

    memcpy(&exp[2], &ipv6, 16);

    assertEquals("unpackInetAddr(..)", inetAddr, unpackInetAddr(exp, 2));

    packInetAddr(act, 2, inetAddr);
    assertBufEquals("packInetAddr(..)", exp, 20, act, 20);
  }
}
