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

#include "BasicAcknowledgePacketTest.h"
//#include "BasicDataPacket.h"
#include "BasicAcknowledgePacket.h"
#include "HasFieldsTest.h"
#include "TestRunner.h"
#include "VRTConfig.h"

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

// any #defines? bitfield definitions? TODO

// CIF7 mapping, etc
#define _BELIEF              0x00080000
#define _BELIEF_MASK         0x000000FF
#define _PROBABILITY         0x00100000
#define _PROBABILITY_MASK    0x0000FFFF
#define _THIRD_DERIVATIVE    0x00200000
#define _SECOND_DERIVATIVE   0x00400000
#define _FIRST_DERIVATIVE    0x00800000
#define _ACCURACY            0x01000000
#define _PRECISION           0x02000000
#define _MIN_VALUE           0x04000000
#define _MAX_VALUE           0x08000000
#define _STANDARD_DEVIATION  0x10000000
#define _MEDIAN_VALUE        0x20000000
#define _AVERAGE_VALUE       0x40000000
#define _CURRENT_VALUE       0x80000000

string BasicAcknowledgePacketTest::getTestedClass () {
  return "vrt::BasicAcknowledgePacket";
}

vector<TestSet> BasicAcknowledgePacketTest::init () {
  vector<TestSet> tests;

  tests.push_back(TestSet("setPacketType",                this, "testSetPacketType"));
  tests.push_back(TestSet("resetForResend",               this, "testResetForResend"));
  tests.push_back(TestSet("getWarning",                   this, "testGetWarning"));
  tests.push_back(TestSet("getError",                     this, "testGetError"));
  tests.push_back(TestSet("getWarningError",              this, "testGetWarningError"));
  tests.push_back(TestSet("getWarnings",                  this, "testGetWarnings"));
  tests.push_back(TestSet("getErrors",                    this, "testGetErrors"));
  tests.push_back(TestSet("getWarningsErrors",            this, "testGetWarningsErrors"));
  tests.push_back(TestSet("addWarning",                   this, "testAddWarning"));
  tests.push_back(TestSet("addError",                     this, "testAddError"));
  tests.push_back(TestSet("removeWarning",                this, "testRemoveWarning"));
  tests.push_back(TestSet("removeError",                  this, "testRemoveError"));
  tests.push_back(TestSet("nullWarning",                  this, "testNullWarning"));
  tests.push_back(TestSet("nullError",                    this, "testNullError"));
  tests.push_back(TestSet("freeFormWarning",              this, "testFreeFormWarning"));
  tests.push_back(TestSet("freeFormError",                this, "testFreeFormError"));

  tests.push_back(TestSet("getField(..)",                 this,  "testGetField"));
  tests.push_back(TestSet("getFieldByName(..)",           this, "+testGetField"));
  tests.push_back(TestSet("getFieldCount()",              this, "+testGetFieldName"));
  tests.push_back(TestSet("getFieldID(..)",               this, "+testGetFieldName"));
  tests.push_back(TestSet("getFieldName(..)",             this,  "testGetFieldName"));
  tests.push_back(TestSet("getFieldType(..)",             this, "+testGetFieldName"));
  tests.push_back(TestSet("setField(..)",                 this, "+testGetField"));
  tests.push_back(TestSet("setFieldByName(..)",           this, "+testGetField"));

  return tests;
}

void BasicAcknowledgePacketTest::done () {
  // nothing to do
}

void BasicAcknowledgePacketTest::call (const string &func) {
  if (func == "testSetPacketType"                       ) testSetPacketType();
  if (func == "testResetForResend"                      ) testResetForResend();
  if (func == "testGetWarning"                          ) testGetWarning();
  if (func == "testGetError"                            ) testGetError();
  if (func == "testGetWarningError"                     ) testGetWarningError();
  if (func == "testGetWarnings"                         ) testGetWarnings();
  if (func == "testGetErrors"                           ) testGetErrors();
  if (func == "testGetWarningsErrors"                   ) testGetWarningsErrors();
  if (func == "testAddWarning"                          ) testAddWarning();
  if (func == "testAddError"                            ) testAddError();
  if (func == "testRemoveWarning"                       ) testRemoveWarning();
  if (func == "testRemoveError"                         ) testRemoveError();
  if (func == "testNullWarning"                         ) testNullWarning();
  if (func == "testNullError"                           ) testNullError();
  if (func == "testFreeFormWarning"                     ) testFreeFormWarning();
  if (func == "testFreeFormError"                       ) testFreeFormError();
}

void BasicAcknowledgePacketTest::testResetForResend () {
  BasicAcknowledgePacket p;

}

void BasicAcknowledgePacketTest::testSetPacketType () {
  BasicAcknowledgePacket p;
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getPacketType()", 6, (buffer[0] >> 4)); // Should default to Command

  p.setPacketType(PacketType_Command); // Setting to Command also valid
  buffer = (char*)p.getPacketPointer();
  assertEquals("setPacketType(Command)", 6, (buffer[0] >> 4));

}

void BasicAcknowledgePacketTest::testGetWarning () {
  BasicAcknowledgePacket p;
  int32_t len0 = p.getPayloadLength();
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() false", false, p.getWarningsGenerated());
  assertEquals("getWarningsGenerated() false bits", 0x0, buffer[29] & 0x2);

  // Set warnings
  p.setWarningsGenerated(true);
  int32_t len1 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len1);
  assertEquals("addWIF0() pkt len", len0+4, len1);
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() true", true, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(true) bits", 0x2, buffer[29] & 0x2);

  // Set SR out-of-range
  p.setWarning(SAMPLE_RATE, PARAM_OUT_OF_RANGE);
  int32_t len2 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len2);
  assertEquals("setWarning(sr,out-of-range) pkt len", len1+4, len2);
  buffer = (char*)p.getPacketPointer();
  assertEquals("setWarning(sr,out-of-range) WIF0[0]", 0x00, buffer[68]);
  assertEquals("setWarning(sr,out-of-range) WIF0[1]", 0x20, buffer[69]);
  assertEquals("setWarning(sr,out-of-range) WIF0[2]", 0x00, buffer[70]);
  assertEquals("setWarning(sr,out-of-range) WIF0[3]", 0x00, buffer[71]);
  assertEquals("setWarning(sr,out-of-range) SR[0]",   0x10, buffer[72]);
  assertEquals("setWarning(sr,out-of-range) SR[1]",   0x00, buffer[73]);
  assertEquals("setWarning(sr,out-of-range) SR[2]",   0x00, buffer[74]);
  assertEquals("setWarning(sr,out-of-range) SR[3]",   0x00, buffer[75]);
  assertEquals("setWarning(sr,out-of-range) WIF0 bits", 0x00200000, VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("setWarning(sr,out-of-range) SR bits",   0x10000000, VRTMath::unpackInt(p.getPayloadPointer(), 4));
  assertEquals("getWarning(sr) out-of-range", PARAM_OUT_OF_RANGE, p.getWarning(SAMPLE_RATE));
  assertEquals("getWarning(sr) out-of-range bits", 0x10 << 24, p.getWarning(SAMPLE_RATE));

  // CIF7 tests

  // Add CIF7
  p.addCIF7();
  int32_t len3 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len3);
  assertEquals("addWIF7() pkt len", len2+4, len3);
  assertEquals("addWIF7() WIF0 bits", 0x00200080, VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("addWIF7() WIF7 bits", _CURRENT_VALUE, VRTMath::unpackInt(p.getPayloadPointer(), 4));
  assertEquals("addWIF7() SR bits",   0x10000000, VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getWarning(sr,CUR) out-of-range", PARAM_OUT_OF_RANGE, p.getWarning(SAMPLE_RATE, CURRENT_VALUE));

  // Add MIN
  p.setCIF7Attribute(MIN_VALUE, true);
  int32_t len4 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len4);
  assertEquals("addCIF7Attribute(MIN) pkt len", len3+4, len4);
  assertEquals("addCIF7Attribute(MIN) WIF0 bits",             0x00200080,         VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("addCIF7Attribute(MIN) WIF7 bits CUR",         _CURRENT_VALUE,     VRTMath::unpackInt(p.getPayloadPointer(), 4) & _CURRENT_VALUE);
  assertEquals("addCIF7Attribute(MIN) WIF7 bits MIN",         _MIN_VALUE,         VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MIN_VALUE);
  assertEquals("addCIF7Attribute(MIN) SR CUR bits",           PARAM_OUT_OF_RANGE, VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getWarning(sr,CUR) out-of-range",             PARAM_OUT_OF_RANGE, p.getWarning(SAMPLE_RATE, CURRENT_VALUE));
  assertEquals("addCIF7Attribute(MIN) SR MIN bits",           0x0,                VRTMath::unpackInt(p.getPayloadPointer(), 12));
  assertEquals("getWarning(sr,MIN) out-of-range",             0x0,                p.getWarning(SAMPLE_RATE, MIN_VALUE));

  // Add MAX
  p.setCIF7Attribute(MAX_VALUE, true);
  int32_t len5 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len5);
  assertEquals("addCIF7Attribute(MAX) pkt len", len4+4, len5);
  assertEquals("addCIF7Attribute(MAX) WIF0 bits",             0x00200080,         VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("addCIF7Attribute(MAX) WIF7 bits CUR",         _CURRENT_VALUE,     VRTMath::unpackInt(p.getPayloadPointer(), 4) & _CURRENT_VALUE);
  assertEquals("addCIF7Attribute(MAX) WIF7 bits MIN",         _MIN_VALUE,         VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MIN_VALUE);
  assertEquals("addCIF7Attribute(MAX) WIF7 bits MAX",         _MAX_VALUE,         VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MAX_VALUE);
  assertEquals("addCIF7Attribute(MAX) SR CUR bits",           PARAM_OUT_OF_RANGE, VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getWarning(sr,CUR) out-of-range",             PARAM_OUT_OF_RANGE, p.getWarning(SAMPLE_RATE, CURRENT_VALUE));
  assertEquals("addCIF7Attribute(MAX) SR MAX bits",           0x0,                VRTMath::unpackInt(p.getPayloadPointer(), 12));
  assertEquals("getWarning(sr,MAX) out-of-range",             0x0,                p.getWarning(SAMPLE_RATE, MAX_VALUE));
  assertEquals("addCIF7Attribute(MAX) SR MIN bits",           0x0,                VRTMath::unpackInt(p.getPayloadPointer(), 16));
  assertEquals("getWarning(sr,MIN) out-of-range",             0x0,                p.getWarning(SAMPLE_RATE, MIN_VALUE));

  // Set MIN
  p.setWarning(SAMPLE_RATE, HARDWARE_DEVICE_FAILURE, MIN_VALUE);
  int32_t len6 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len6);
  assertEquals("setWarning(SR,hw-failure,MIN) pkt len", len5, len6);
  assertEquals("setWarning(SR,hw-failure,MIN) WIF0 bits",     0x00200080,              VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("setWarning(SR,hw-failure,MIN) WIF7 bits CUR", _CURRENT_VALUE,          VRTMath::unpackInt(p.getPayloadPointer(), 4) & _CURRENT_VALUE);
  assertEquals("setWarning(SR,hw-failure,MIN) WIF7 bits MIN", _MIN_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MIN_VALUE);
  assertEquals("setWarning(SR,hw-failure,MIN) WIF7 bits MAX", _MAX_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MAX_VALUE);
  assertEquals("setWarning(SR,hw-failure,MIN) SR CUR bits",   PARAM_OUT_OF_RANGE,      VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getWarning(sr,CUR) out-of-range",             PARAM_OUT_OF_RANGE,      p.getWarning(SAMPLE_RATE, CURRENT_VALUE));
  assertEquals("setWarning(SR,hw-failure,MIN) SR MAX bits",   0x0,                     VRTMath::unpackInt(p.getPayloadPointer(), 12));
  assertEquals("getWarning(sr,MAX) out-of-range",             0x0,                     p.getWarning(SAMPLE_RATE, MAX_VALUE));
  assertEquals("setWarning(SR,hw-failure,MIN) SR MIN bits",   HARDWARE_DEVICE_FAILURE, VRTMath::unpackInt(p.getPayloadPointer(), 16));
  assertEquals("getWarning(sr,MIN) out-of-range",             HARDWARE_DEVICE_FAILURE, p.getWarning(SAMPLE_RATE, MIN_VALUE));

  // Set MAX
  p.setWarning(SAMPLE_RATE, TIMESTAMP_PROBLEM, MAX_VALUE);
  int32_t len7 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len7);
  assertEquals("setWarning(SR,ts-prob,MAX) pkt len", len6, len7);
  assertEquals("setWarning(SR,ts-prob,MAX) WIF0 bits",        0x00200080,              VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("setWarning(SR,ts-prob,MAX) WIF7 bits CUR",    _CURRENT_VALUE,          VRTMath::unpackInt(p.getPayloadPointer(), 4) & _CURRENT_VALUE);
  assertEquals("setWarning(SR,ts-prob,MAX) WIF7 bits MIN",    _MIN_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MIN_VALUE);
  assertEquals("setWarning(SR,ts-prob,MAX) WIF7 bits MAX",    _MAX_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MAX_VALUE);
  assertEquals("setWarning(SR,ts-prob,MAX) SR CUR bits",      PARAM_OUT_OF_RANGE,      VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getWarning(sr,CUR) out-of-range",             PARAM_OUT_OF_RANGE,      p.getWarning(SAMPLE_RATE, CURRENT_VALUE));
  assertEquals("setWarning(SR,ts-prob,MAX) SR MAX bits",      TIMESTAMP_PROBLEM,       VRTMath::unpackInt(p.getPayloadPointer(), 12));
  assertEquals("getWarning(sr,MAX) out-of-range",             TIMESTAMP_PROBLEM,       p.getWarning(SAMPLE_RATE, MAX_VALUE));
  assertEquals("setWarning(SR,ts-prob,MAX) SR MIN bits",      HARDWARE_DEVICE_FAILURE, VRTMath::unpackInt(p.getPayloadPointer(), 16));
  assertEquals("getWarning(sr,MIN) out-of-range",             HARDWARE_DEVICE_FAILURE, p.getWarning(SAMPLE_RATE, MIN_VALUE));

  // Add and set Belief
  p.setCIF7Attribute(BELIEF, true);
  p.setWarning(SAMPLE_RATE, WEF_USER_DEFINED_10, BELIEF);
  int32_t len8 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len8);
  assertEquals("setWarning(SR,user-10,BELIEF) pkt len", len7+4, len8);
  assertEquals("setWarning(SR,user-10,BELIEF) WIF0 bits",        0x00200080,              VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("setWarning(SR,user-10,BELIEF) WIF7 bits CUR",    _CURRENT_VALUE,          VRTMath::unpackInt(p.getPayloadPointer(), 4) & _CURRENT_VALUE);
  assertEquals("setWarning(SR,user-10,BELIEF) WIF7 bits MIN",    _MIN_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MIN_VALUE);
  assertEquals("setWarning(SR,user-10,BELIEF) WIF7 bits MAX",    _MAX_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MAX_VALUE);
  assertEquals("setWarning(SR,user-10,BELIEF) WIF7 bits BELIEF", _BELIEF,                 VRTMath::unpackInt(p.getPayloadPointer(), 4) & _BELIEF);
  assertEquals("setWarning(SR,user-10,BELIEF) SR CUR bits",      PARAM_OUT_OF_RANGE,      VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getWarning(sr,CUR) out-of-range",                PARAM_OUT_OF_RANGE,      p.getWarning(SAMPLE_RATE, CURRENT_VALUE));
  assertEquals("setWarning(SR,user-10,BELIEF) SR MAX bits",      TIMESTAMP_PROBLEM,       VRTMath::unpackInt(p.getPayloadPointer(), 12));
  assertEquals("getWarning(sr,MAX) out-of-range",                TIMESTAMP_PROBLEM,       p.getWarning(SAMPLE_RATE, MAX_VALUE));
  assertEquals("setWarning(SR,user-10,BELIEF) SR MIN bits",      HARDWARE_DEVICE_FAILURE, VRTMath::unpackInt(p.getPayloadPointer(), 16));
  assertEquals("getWarning(sr,MIN) out-of-range",                HARDWARE_DEVICE_FAILURE, p.getWarning(SAMPLE_RATE, MIN_VALUE));
  assertEquals("setWarning(SR,user-10,BELIEF) SR BELIEF bits",   WEF_USER_DEFINED_10,     VRTMath::unpackInt(p.getPayloadPointer(), 20));
  assertEquals("getWarning(sr,MIN) out-of-range",                WEF_USER_DEFINED_10,     p.getWarning(SAMPLE_RATE, BELIEF));

  // Unset CUR
  p.setCIF7Attribute(CURRENT_VALUE, false);
  int32_t len9 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len9);
  assertEquals("setCIF7Attribute(CUR,false) pkt len", len8-4, len9);
  assertEquals("setCIF7Attribute(CUR,false) WIF0 bits",        0x00200080,              VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("setCIF7Attribute(CUR,false) WIF7 bits CUR",    0x0,                     VRTMath::unpackInt(p.getPayloadPointer(), 4) & _CURRENT_VALUE);
  assertEquals("setCIF7Attribute(CUR,false) WIF7 bits MIN",    _MIN_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MIN_VALUE);
  assertEquals("setCIF7Attribute(CUR,false) WIF7 bits MAX",    _MAX_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MAX_VALUE);
  assertEquals("setCIF7Attribute(CUR,false) WIF7 bits BELIEF", _BELIEF,                 VRTMath::unpackInt(p.getPayloadPointer(), 4) & _BELIEF);
  assertEquals("getWarning(sr,CUR) out-of-range",              WEF_NULL,                p.getWarning(SAMPLE_RATE, CURRENT_VALUE));
  assertEquals("setCIF7Attribute(CUR,false) SR MAX bits",      TIMESTAMP_PROBLEM,       VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getWarning(sr,MAX) out-of-range",              TIMESTAMP_PROBLEM,       p.getWarning(SAMPLE_RATE, MAX_VALUE));
  assertEquals("setCIF7Attribute(CUR,false) SR MIN bits",      HARDWARE_DEVICE_FAILURE, VRTMath::unpackInt(p.getPayloadPointer(), 12));
  assertEquals("getWarning(sr,MIN) out-of-range",              HARDWARE_DEVICE_FAILURE, p.getWarning(SAMPLE_RATE, MIN_VALUE));
  assertEquals("setCIF7Attribute(CUR,false) SR BELIEF bits",   WEF_USER_DEFINED_10,     VRTMath::unpackInt(p.getPayloadPointer(), 16));
  assertEquals("getWarning(sr,MIN) out-of-range",              WEF_USER_DEFINED_10,     p.getWarning(SAMPLE_RATE, BELIEF));

  // Add BW
  p.setWarning(BANDWIDTH, PARAM_OUT_OF_RANGE, MAX_VALUE);
  int32_t len10 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len10);
  assertEquals("setWarning(BW,oor,MAX) pkt len", len9+12, len10);
  assertEquals("setWarning(BW,oor,MAX) WIF0 bits",        0x20200080,              VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("setWarning(BW,oor,MAX) WIF7 bits MIN",    _MIN_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MIN_VALUE);
  assertEquals("setWarning(BW,oor,MAX) WIF7 bits MAX",    _MAX_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MAX_VALUE);
  assertEquals("setWarning(BW,oor,MAX) WIF7 bits BELIEF", _BELIEF,                 VRTMath::unpackInt(p.getPayloadPointer(), 4) & _BELIEF);

  assertEquals("setWarning(BW,oor,MAX) BW MAX bits",      PARAM_OUT_OF_RANGE,      VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getWarning(BW,MAX) out-of-range",         PARAM_OUT_OF_RANGE,      p.getWarning(BANDWIDTH, MAX_VALUE));
  assertEquals("setWarning(BW,oor,MAX) BW MIN bits",      0x0,                     VRTMath::unpackInt(p.getPayloadPointer(), 12));
  assertEquals("getWarning(BW,MIN) out-of-range",         0x0,                     p.getWarning(BANDWIDTH, MIN_VALUE));
  assertEquals("setWarning(BW,oor,MAX) BW BELIEF bits",   0x0,                     VRTMath::unpackInt(p.getPayloadPointer(), 16));
  assertEquals("getWarning(BW,MIN) out-of-range",         0x0,                     p.getWarning(BANDWIDTH, BELIEF));

  assertEquals("setWarning(BW,oor,MAX) SR MAX bits",      TIMESTAMP_PROBLEM,       VRTMath::unpackInt(p.getPayloadPointer(), 20));
  assertEquals("getWarning(sr,MAX) out-of-range",         TIMESTAMP_PROBLEM,       p.getWarning(SAMPLE_RATE, MAX_VALUE));
  assertEquals("setWarning(BW,oor,MAX) SR MIN bits",      HARDWARE_DEVICE_FAILURE, VRTMath::unpackInt(p.getPayloadPointer(), 24));
  assertEquals("getWarning(sr,MIN) out-of-range",         HARDWARE_DEVICE_FAILURE, p.getWarning(SAMPLE_RATE, MIN_VALUE));
  assertEquals("setWarning(BW,oor,MAX) SR BELIEF bits",   WEF_USER_DEFINED_10,     VRTMath::unpackInt(p.getPayloadPointer(), 28));
  assertEquals("getWarning(sr,MIN) out-of-range",         WEF_USER_DEFINED_10,     p.getWarning(SAMPLE_RATE, BELIEF));

}

void BasicAcknowledgePacketTest::testGetError () {
  BasicAcknowledgePacket p;
  int32_t len0 = p.getPayloadLength();
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getErrorsGenerated() false", false, p.getErrorsGenerated());
  assertEquals("getErrorsGenerated() false bits", 0x0, buffer[29] & 0x1);

  // Set warnings
  p.setErrorsGenerated(true);
  int32_t len1 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len1);
  assertEquals("addEIF0() pkt len", len0+4, len1);
  buffer = (char*)p.getPacketPointer();
  assertEquals("getErrorsGenerated() true", true, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(true) bits", 0x1, buffer[29] & 0x1);

  // Set SR out-of-range
  p.setError(SAMPLE_RATE, PARAM_OUT_OF_RANGE);
  int32_t len2 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len2);
  assertEquals("setError(sr,out-of-range) pkt len", len1+4, len2);
  buffer = (char*)p.getPacketPointer();
  assertEquals("setError(sr,out-of-range) EIF0[0]", 0x00, buffer[68]);
  assertEquals("setError(sr,out-of-range) EIF0[1]", 0x20, buffer[69]);
  assertEquals("setError(sr,out-of-range) EIF0[2]", 0x00, buffer[70]);
  assertEquals("setError(sr,out-of-range) EIF0[3]", 0x00, buffer[71]);
  assertEquals("setError(sr,out-of-range) SR[0]",   0x10, buffer[72]);
  assertEquals("setError(sr,out-of-range) SR[1]",   0x00, buffer[73]);
  assertEquals("setError(sr,out-of-range) SR[2]",   0x00, buffer[74]);
  assertEquals("setError(sr,out-of-range) SR[3]",   0x00, buffer[75]);
  assertEquals("setError(sr,out-of-range) EIF0 bits", 0x00200000, VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("setError(sr,out-of-range) SR bits",   0x10000000, VRTMath::unpackInt(p.getPayloadPointer(), 4));
  assertEquals("getError(sr) out-of-range", PARAM_OUT_OF_RANGE, p.getError(SAMPLE_RATE));
  assertEquals("getError(sr) out-of-range bits", 0x10 << 24, p.getError(SAMPLE_RATE));

  // CIF7 tests

  // Add CIF7
  p.addCIF7(true,true);
  int32_t len3 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len3);
  assertEquals("addEIF7() pkt len", len2+4, len3);
  assertEquals("addEIF7() EIF0 bits", 0x00200080, VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("addEIF7() EIF7 bits", _CURRENT_VALUE, VRTMath::unpackInt(p.getPayloadPointer(), 4));
  assertEquals("addEIF7() SR bits",   0x10000000, VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getError(sr,CUR) out-of-range", PARAM_OUT_OF_RANGE, p.getError(SAMPLE_RATE, CURRENT_VALUE));

  // Add MIN
  p.setCIF7Attribute(MIN_VALUE, true, true);
  int32_t len4 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len4);
  assertEquals("addCIF7Attribute(MIN) pkt len", len3+4, len4);
  assertEquals("addCIF7Attribute(MIN) EIF0 bits",             0x00200080,         VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("addCIF7Attribute(MIN) EIF7 bits CUR",         _CURRENT_VALUE,     VRTMath::unpackInt(p.getPayloadPointer(), 4) & _CURRENT_VALUE);
  assertEquals("addCIF7Attribute(MIN) EIF7 bits MIN",         _MIN_VALUE,         VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MIN_VALUE);
  assertEquals("addCIF7Attribute(MIN) SR CUR bits",           PARAM_OUT_OF_RANGE, VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getError(sr,CUR) out-of-range",             PARAM_OUT_OF_RANGE, p.getError(SAMPLE_RATE, CURRENT_VALUE));
  assertEquals("addCIF7Attribute(MIN) SR MIN bits",           0x0,                VRTMath::unpackInt(p.getPayloadPointer(), 12));
  assertEquals("getError(sr,MIN) out-of-range",             0x0,                p.getError(SAMPLE_RATE, MIN_VALUE));

  // Add MAX
  p.setCIF7Attribute(MAX_VALUE, true, true);
  int32_t len5 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len5);
  assertEquals("addCIF7Attribute(MAX) pkt len", len4+4, len5);
  assertEquals("addCIF7Attribute(MAX) EIF0 bits",             0x00200080,         VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("addCIF7Attribute(MAX) EIF7 bits CUR",         _CURRENT_VALUE,     VRTMath::unpackInt(p.getPayloadPointer(), 4) & _CURRENT_VALUE);
  assertEquals("addCIF7Attribute(MAX) EIF7 bits MIN",         _MIN_VALUE,         VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MIN_VALUE);
  assertEquals("addCIF7Attribute(MAX) EIF7 bits MAX",         _MAX_VALUE,         VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MAX_VALUE);
  assertEquals("addCIF7Attribute(MAX) SR CUR bits",           PARAM_OUT_OF_RANGE, VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getError(sr,CUR) out-of-range",             PARAM_OUT_OF_RANGE, p.getError(SAMPLE_RATE, CURRENT_VALUE));
  assertEquals("addCIF7Attribute(MAX) SR MAX bits",           0x0,                VRTMath::unpackInt(p.getPayloadPointer(), 12));
  assertEquals("getError(sr,MAX) out-of-range",             0x0,                p.getError(SAMPLE_RATE, MAX_VALUE));
  assertEquals("addCIF7Attribute(MAX) SR MIN bits",           0x0,                VRTMath::unpackInt(p.getPayloadPointer(), 16));
  assertEquals("getError(sr,MIN) out-of-range",             0x0,                p.getError(SAMPLE_RATE, MIN_VALUE));

  // Set MIN
  p.setError(SAMPLE_RATE, HARDWARE_DEVICE_FAILURE, MIN_VALUE);
  int32_t len6 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len6);
  assertEquals("setError(SR,hw-failure,MIN) pkt len", len5, len6);
  assertEquals("setError(SR,hw-failure,MIN) EIF0 bits",     0x00200080,              VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("setError(SR,hw-failure,MIN) EIF7 bits CUR", _CURRENT_VALUE,          VRTMath::unpackInt(p.getPayloadPointer(), 4) & _CURRENT_VALUE);
  assertEquals("setError(SR,hw-failure,MIN) EIF7 bits MIN", _MIN_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MIN_VALUE);
  assertEquals("setError(SR,hw-failure,MIN) EIF7 bits MAX", _MAX_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MAX_VALUE);
  assertEquals("setError(SR,hw-failure,MIN) SR CUR bits",   PARAM_OUT_OF_RANGE,      VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getError(sr,CUR) out-of-range",             PARAM_OUT_OF_RANGE,      p.getError(SAMPLE_RATE, CURRENT_VALUE));
  assertEquals("setError(SR,hw-failure,MIN) SR MAX bits",   0x0,                     VRTMath::unpackInt(p.getPayloadPointer(), 12));
  assertEquals("getError(sr,MAX) out-of-range",             0x0,                     p.getError(SAMPLE_RATE, MAX_VALUE));
  assertEquals("setError(SR,hw-failure,MIN) SR MIN bits",   HARDWARE_DEVICE_FAILURE, VRTMath::unpackInt(p.getPayloadPointer(), 16));
  assertEquals("getError(sr,MIN) out-of-range",             HARDWARE_DEVICE_FAILURE, p.getError(SAMPLE_RATE, MIN_VALUE));

  // Set MAX
  p.setError(SAMPLE_RATE, TIMESTAMP_PROBLEM, MAX_VALUE);
  int32_t len7 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len7);
  assertEquals("setError(SR,ts-prob,MAX) pkt len", len6, len7);
  assertEquals("setError(SR,ts-prob,MAX) EIF0 bits",        0x00200080,              VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("setError(SR,ts-prob,MAX) EIF7 bits CUR",    _CURRENT_VALUE,          VRTMath::unpackInt(p.getPayloadPointer(), 4) & _CURRENT_VALUE);
  assertEquals("setError(SR,ts-prob,MAX) EIF7 bits MIN",    _MIN_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MIN_VALUE);
  assertEquals("setError(SR,ts-prob,MAX) EIF7 bits MAX",    _MAX_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MAX_VALUE);
  assertEquals("setError(SR,ts-prob,MAX) SR CUR bits",      PARAM_OUT_OF_RANGE,      VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getError(sr,CUR) out-of-range",             PARAM_OUT_OF_RANGE,      p.getError(SAMPLE_RATE, CURRENT_VALUE));
  assertEquals("setError(SR,ts-prob,MAX) SR MAX bits",      TIMESTAMP_PROBLEM,       VRTMath::unpackInt(p.getPayloadPointer(), 12));
  assertEquals("getError(sr,MAX) out-of-range",             TIMESTAMP_PROBLEM,       p.getError(SAMPLE_RATE, MAX_VALUE));
  assertEquals("setError(SR,ts-prob,MAX) SR MIN bits",      HARDWARE_DEVICE_FAILURE, VRTMath::unpackInt(p.getPayloadPointer(), 16));
  assertEquals("getError(sr,MIN) out-of-range",             HARDWARE_DEVICE_FAILURE, p.getError(SAMPLE_RATE, MIN_VALUE));

  // Add and set Belief
  p.setCIF7Attribute(BELIEF, true, true);
  p.setError(SAMPLE_RATE, WEF_USER_DEFINED_10, BELIEF);
  int32_t len8 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len8);
  assertEquals("setError(SR,user-10,BELIEF) pkt len", len7+4, len8);
  assertEquals("setError(SR,user-10,BELIEF) EIF0 bits",        0x00200080,              VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("setError(SR,user-10,BELIEF) EIF7 bits CUR",    _CURRENT_VALUE,          VRTMath::unpackInt(p.getPayloadPointer(), 4) & _CURRENT_VALUE);
  assertEquals("setError(SR,user-10,BELIEF) EIF7 bits MIN",    _MIN_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MIN_VALUE);
  assertEquals("setError(SR,user-10,BELIEF) EIF7 bits MAX",    _MAX_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MAX_VALUE);
  assertEquals("setError(SR,user-10,BELIEF) EIF7 bits BELIEF", _BELIEF,                 VRTMath::unpackInt(p.getPayloadPointer(), 4) & _BELIEF);
  assertEquals("setError(SR,user-10,BELIEF) SR CUR bits",      PARAM_OUT_OF_RANGE,      VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getError(sr,CUR) out-of-range",                PARAM_OUT_OF_RANGE,      p.getError(SAMPLE_RATE, CURRENT_VALUE));
  assertEquals("setError(SR,user-10,BELIEF) SR MAX bits",      TIMESTAMP_PROBLEM,       VRTMath::unpackInt(p.getPayloadPointer(), 12));
  assertEquals("getError(sr,MAX) out-of-range",                TIMESTAMP_PROBLEM,       p.getError(SAMPLE_RATE, MAX_VALUE));
  assertEquals("setError(SR,user-10,BELIEF) SR MIN bits",      HARDWARE_DEVICE_FAILURE, VRTMath::unpackInt(p.getPayloadPointer(), 16));
  assertEquals("getError(sr,MIN) out-of-range",                HARDWARE_DEVICE_FAILURE, p.getError(SAMPLE_RATE, MIN_VALUE));
  assertEquals("setError(SR,user-10,BELIEF) SR BELIEF bits",   WEF_USER_DEFINED_10,     VRTMath::unpackInt(p.getPayloadPointer(), 20));
  assertEquals("getError(sr,MIN) out-of-range",                WEF_USER_DEFINED_10,     p.getError(SAMPLE_RATE, BELIEF));

  // Unset CUR
  p.setCIF7Attribute(CURRENT_VALUE, false, true);
  int32_t len9 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len9);
  assertEquals("setCIF7Attribute(CUR,false) pkt len", len8-4, len9);
  assertEquals("setCIF7Attribute(CUR,false) EIF0 bits",        0x00200080,              VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("setCIF7Attribute(CUR,false) EIF7 bits CUR",    0x0,                     VRTMath::unpackInt(p.getPayloadPointer(), 4) & _CURRENT_VALUE);
  assertEquals("setCIF7Attribute(CUR,false) EIF7 bits MIN",    _MIN_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MIN_VALUE);
  assertEquals("setCIF7Attribute(CUR,false) EIF7 bits MAX",    _MAX_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MAX_VALUE);
  assertEquals("setCIF7Attribute(CUR,false) EIF7 bits BELIEF", _BELIEF,                 VRTMath::unpackInt(p.getPayloadPointer(), 4) & _BELIEF);
  assertEquals("getError(sr,CUR) out-of-range",              WEF_NULL,                p.getError(SAMPLE_RATE, CURRENT_VALUE));
  assertEquals("setCIF7Attribute(CUR,false) SR MAX bits",      TIMESTAMP_PROBLEM,       VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getError(sr,MAX) out-of-range",              TIMESTAMP_PROBLEM,       p.getError(SAMPLE_RATE, MAX_VALUE));
  assertEquals("setCIF7Attribute(CUR,false) SR MIN bits",      HARDWARE_DEVICE_FAILURE, VRTMath::unpackInt(p.getPayloadPointer(), 12));
  assertEquals("getError(sr,MIN) out-of-range",              HARDWARE_DEVICE_FAILURE, p.getError(SAMPLE_RATE, MIN_VALUE));
  assertEquals("setCIF7Attribute(CUR,false) SR BELIEF bits",   WEF_USER_DEFINED_10,     VRTMath::unpackInt(p.getPayloadPointer(), 16));
  assertEquals("getError(sr,MIN) out-of-range",              WEF_USER_DEFINED_10,     p.getError(SAMPLE_RATE, BELIEF));

  // Add BW
  p.setError(BANDWIDTH, PARAM_OUT_OF_RANGE, MAX_VALUE);
  int32_t len10 = p.getPayloadLength();
  //Utilities::dumpBytes(p.getPayloadPointer(), len10);
  assertEquals("setError(BW,oor,MAX) pkt len", len9+12, len10);
  assertEquals("setError(BW,oor,MAX) EIF0 bits",        0x20200080,              VRTMath::unpackInt(p.getPayloadPointer(), 0));
  assertEquals("setError(BW,oor,MAX) EIF7 bits MIN",    _MIN_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MIN_VALUE);
  assertEquals("setError(BW,oor,MAX) EIF7 bits MAX",    _MAX_VALUE,              VRTMath::unpackInt(p.getPayloadPointer(), 4) & _MAX_VALUE);
  assertEquals("setError(BW,oor,MAX) EIF7 bits BELIEF", _BELIEF,                 VRTMath::unpackInt(p.getPayloadPointer(), 4) & _BELIEF);

  assertEquals("setError(BW,oor,MAX) BW MAX bits",      PARAM_OUT_OF_RANGE,      VRTMath::unpackInt(p.getPayloadPointer(), 8));
  assertEquals("getError(BW,MAX) out-of-range",         PARAM_OUT_OF_RANGE,      p.getError(BANDWIDTH, MAX_VALUE));
  assertEquals("setError(BW,oor,MAX) BW MIN bits",      0x0,                     VRTMath::unpackInt(p.getPayloadPointer(), 12));
  assertEquals("getError(BW,MIN) out-of-range",         0x0,                     p.getError(BANDWIDTH, MIN_VALUE));
  assertEquals("setError(BW,oor,MAX) BW BELIEF bits",   0x0,                     VRTMath::unpackInt(p.getPayloadPointer(), 16));
  assertEquals("getError(BW,MIN) out-of-range",         0x0,                     p.getError(BANDWIDTH, BELIEF));

  assertEquals("setError(BW,oor,MAX) SR MAX bits",      TIMESTAMP_PROBLEM,       VRTMath::unpackInt(p.getPayloadPointer(), 20));
  assertEquals("getError(sr,MAX) out-of-range",         TIMESTAMP_PROBLEM,       p.getError(SAMPLE_RATE, MAX_VALUE));
  assertEquals("setError(BW,oor,MAX) SR MIN bits",      HARDWARE_DEVICE_FAILURE, VRTMath::unpackInt(p.getPayloadPointer(), 24));
  assertEquals("getError(sr,MIN) out-of-range",         HARDWARE_DEVICE_FAILURE, p.getError(SAMPLE_RATE, MIN_VALUE));
  assertEquals("setError(BW,oor,MAX) SR BELIEF bits",   WEF_USER_DEFINED_10,     VRTMath::unpackInt(p.getPayloadPointer(), 28));
  assertEquals("getError(sr,MIN) out-of-range",         WEF_USER_DEFINED_10,     p.getError(SAMPLE_RATE, BELIEF));
}

void BasicAcknowledgePacketTest::testGetWarningError () {
  BasicAcknowledgePacket p;
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() false", false, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(false)", 0x0, buffer[29] & 0x2);
  assertEquals("getErrorsGenerated() false", false, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(false)", 0x0, buffer[29] & 0x1);
  p.setWarningsGenerated(true);
  p.setErrorsGenerated(true);
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() true", true, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(true)", 0x2, buffer[29] & 0x2);
  assertEquals("getErrorsGenerated() true", true, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(true)", 0x1, buffer[29] & 0x1);
  p.setWarning(IndicatorFields::SAMPLE_RATE, PARAM_OUT_OF_RANGE);
  p.setError(IndicatorFields::BANDWIDTH, HARDWARE_DEVICE_FAILURE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("setWarning(sr,out-of-range) EIF0[0]", 0x00, buffer[68]);
  assertEquals("setWarning(sr,out-of-range) EIF0[1]", 0x20, buffer[69]);
  assertEquals("setWarning(sr,out-of-range) EIF0[2]", 0x00, buffer[70]);
  assertEquals("setWarning(sr,out-of-range) EIF0[3]", 0x00, buffer[71]);
  assertEquals("setError(bw,hw-failure) EIF0[0]", 0x20, buffer[72]);
  assertEquals("setError(bw,hw-failure) EIF0[1]", 0x00, buffer[73]);
  assertEquals("setError(bw,hw-failure) EIF0[2]", 0x00, buffer[74]);
  assertEquals("setError(bw,hw-failure) EIF0[3]", 0x00, buffer[75]);
  assertEquals("setWarning(sr,out-of-range) SR[0]",   0x10, buffer[76]);
  assertEquals("setWarning(sr,out-of-range) SR[1]",   0x00, buffer[77]);
  assertEquals("setWarning(sr,out-of-range) SR[2]",   0x00, buffer[78]);
  assertEquals("setWarning(sr,out-of-range) SR[3]",   0x00, buffer[79]);
  assertEquals("setError(bw,hw-failure) SR[0]",   0x40, buffer[80]);
  assertEquals("setError(bw,hw-failure) SR[1]",   0x00, buffer[81]);
  assertEquals("setError(bw,hw-failure) SR[2]",   0x00, buffer[82]);
  assertEquals("setError(bw,hw-failure) SR[3]",   0x00, buffer[83]);
  assertEquals("getWarning(sr) out-of-range", PARAM_OUT_OF_RANGE,
                                              p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("getWarning(sr) out-of-range bits", 0x10 << 24, p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("getError(bw) hw-failure", HARDWARE_DEVICE_FAILURE, 
                                          p.getError(IndicatorFields::BANDWIDTH));
  assertEquals("getError(bw) hw-failure bits", 0x40 << 24, p.getError(IndicatorFields::BANDWIDTH));
}

void BasicAcknowledgePacketTest::testGetWarnings () {
  BasicAcknowledgePacket p;
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() false", false, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(false)", 0x0, buffer[29] & 0x2);
  p.setWarningsGenerated(true);
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() true", true, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(true)", 0x2, buffer[29] & 0x2);
  p.setWarning(IndicatorFields::SAMPLE_RATE, PARAM_OUT_OF_RANGE);
  p.setWarning(IndicatorFields::BANDWIDTH, PARAM_OUT_OF_RANGE |
                                           HARDWARE_DEVICE_FAILURE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("setWarning(sr+bw) EIF0[0]", 0x20, buffer[68]);
  assertEquals("setWarning(sr+bw) EIF0[1]", 0x20, buffer[69]);
  assertEquals("setWarning(sr+bw) EIF0[2]", 0x00, buffer[70]);
  assertEquals("setWarning(sr+bw) EIF0[3]", 0x00, buffer[71]);
  assertEquals("setWarning(bw) two-warnings BW[0]",   0x50, buffer[72]);
  assertEquals("setWarning(bw) two-warnings BW[1]",   0x00, buffer[73]);
  assertEquals("setWarning(bw) two-warnings BW[2]",   0x00, buffer[74]);
  assertEquals("setWarning(bw) two-warnings BW[3]",   0x00, buffer[75]);
  assertEquals("setWarning(sr) out-of-range SR[0]",   0x10, buffer[76]);
  assertEquals("setWarning(sr) out-of-range SR[1]",   0x00, buffer[77]);
  assertEquals("setWarning(sr) out-of-range SR[2]",   0x00, buffer[78]);
  assertEquals("setWarning(sr) out-of-range SR[3]",   0x00, buffer[79]);
  assertEquals("getWarning(bw) two-warnings", PARAM_OUT_OF_RANGE |
                                              HARDWARE_DEVICE_FAILURE,
                                              p.getWarning(IndicatorFields::BANDWIDTH));
  assertEquals("getWarning(bw) two-warnings bits", 0x50 << 24, p.getWarning(IndicatorFields::BANDWIDTH));
  assertEquals("getWarning(sr) out-of-range", PARAM_OUT_OF_RANGE,
                                              p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("getWarning(sr) out-of-range bits", 0x10 << 24, p.getWarning(IndicatorFields::SAMPLE_RATE));
  std::vector<WarningErrorField_t> warnings = p.getWarnings();
  assertEquals("getWarnings().size()", (size_t) 2, warnings.size());
  assertEquals("getWarnings() bw field", IndicatorFields::BANDWIDTH, warnings[0].field);
  assertEquals("getWarnings() bw responseField", PARAM_OUT_OF_RANGE |
                                                 HARDWARE_DEVICE_FAILURE,
                                                 warnings[0].responseField);
  assertEquals("getWarnings() bw responseField bits", 0x50 << 24, warnings[0].responseField);
  assertEquals("getWarnings() sr field", IndicatorFields::SAMPLE_RATE, warnings[1].field);
  assertEquals("getWarnings() sr responseField", PARAM_OUT_OF_RANGE,
                                                 warnings[1].responseField);
  assertEquals("getWarnings() sr responseField bits", 0x10 << 24, warnings[1].responseField);
}

void BasicAcknowledgePacketTest::testGetErrors () {
  BasicAcknowledgePacket p;
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getErrorsGenerated() false", false, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(false)", 0x0, buffer[29] & 0x1);
  p.setErrorsGenerated(true);
  buffer = (char*)p.getPacketPointer();
  assertEquals("getErrorsGenerated() true", true, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(true)", 0x1, buffer[29] & 0x1);
  p.setError(IndicatorFields::SAMPLE_RATE, PARAM_OUT_OF_RANGE);
  p.setError(IndicatorFields::BANDWIDTH, PARAM_OUT_OF_RANGE |
                                           HARDWARE_DEVICE_FAILURE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("setError(sr+bw) EIF0[0]", 0x20, buffer[68]);
  assertEquals("setError(sr+bw) EIF0[1]", 0x20, buffer[69]);
  assertEquals("setError(sr+bw) EIF0[2]", 0x00, buffer[70]);
  assertEquals("setError(sr+bw) EIF0[3]", 0x00, buffer[71]);
  assertEquals("setError(bw) two-errors BW[0]",   0x50, buffer[72]);
  assertEquals("setError(bw) two-errors BW[1]",   0x00, buffer[73]);
  assertEquals("setError(bw) two-errors BW[2]",   0x00, buffer[74]);
  assertEquals("setError(bw) two-errors BW[3]",   0x00, buffer[75]);
  assertEquals("setError(sr) out-of-range SR[0]",   0x10, buffer[76]);
  assertEquals("setError(sr) out-of-range SR[1]",   0x00, buffer[77]);
  assertEquals("setError(sr) out-of-range SR[2]",   0x00, buffer[78]);
  assertEquals("setError(sr) out-of-range SR[3]",   0x00, buffer[79]);
  assertEquals("getError(bw) two-errors", PARAM_OUT_OF_RANGE |
                                          HARDWARE_DEVICE_FAILURE,
                                          p.getError(IndicatorFields::BANDWIDTH));
  assertEquals("getError(bw) two-errors bits", 0x50 << 24, p.getError(IndicatorFields::BANDWIDTH));
  assertEquals("getError(sr) out-of-range", PARAM_OUT_OF_RANGE,
                                            p.getError(IndicatorFields::SAMPLE_RATE));
  assertEquals("getError(sr) out-of-range bits", 0x10 << 24, p.getError(IndicatorFields::SAMPLE_RATE));
  std::vector<WarningErrorField_t> errors = p.getErrors();
  assertEquals("getErrors().size()", (size_t) 2, errors.size());
  assertEquals("getErrors() bw field", IndicatorFields::BANDWIDTH, errors[0].field);
  assertEquals("getErrors() bw responseField", PARAM_OUT_OF_RANGE |
                                               HARDWARE_DEVICE_FAILURE,
                                               errors[0].responseField);
  assertEquals("getErrors() bw responseField bits", 0x50 << 24, errors[0].responseField);
  assertEquals("getErrors() sr field", IndicatorFields::SAMPLE_RATE, errors[1].field);
  assertEquals("getErrors() sr responseField", PARAM_OUT_OF_RANGE,
                                               errors[1].responseField);
  assertEquals("getErrors() sr responseField bits", 0x10 << 24, errors[1].responseField);
}

void BasicAcknowledgePacketTest::testGetWarningsErrors () {
  BasicAcknowledgePacket p;
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() false", false, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(false)", 0x0, buffer[29] & 0x2);
  assertEquals("getErrorsGenerated() false", false, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(false)", 0x0, buffer[29] & 0x1);
  p.setWarningsGenerated(true);
  p.setErrorsGenerated(true);
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() true", true, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(true)", 0x2, buffer[29] & 0x2);
  assertEquals("getErrorsGenerated() true", true, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(true)", 0x1, buffer[29] & 0x1);
  p.setWarning(IndicatorFields::SAMPLE_RATE, PARAM_OUT_OF_RANGE);
  p.setWarning(IndicatorFields::BANDWIDTH, HARDWARE_DEVICE_FAILURE);
  p.setError(IndicatorFields::SAMPLE_RATE, HARDWARE_DEVICE_FAILURE);
  p.setError(IndicatorFields::BANDWIDTH, PARAM_OUT_OF_RANGE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("setWarning(sr+bw) WIF0[0]", 0x20, buffer[68]);
  assertEquals("setWarning(sr+bw) WIF0[1]", 0x20, buffer[69]);
  assertEquals("setWarning(sr+bw) WIF0[2]", 0x00, buffer[70]);
  assertEquals("setWarning(sr+bw) WIF0[3]", 0x00, buffer[71]);
  assertEquals("setError(sr+bw) EIF0[0]", 0x20, buffer[72]);
  assertEquals("setError(sr+bw) EIF0[1]", 0x20, buffer[73]);
  assertEquals("setError(sr+bw) EIF0[2]", 0x00, buffer[74]);
  assertEquals("setError(sr+bw) EIF0[3]", 0x00, buffer[75]);
  assertEquals("setWarning(bw) hw-failure BW[0]",   0x40, buffer[76]);
  assertEquals("setWarning(bw) hw-failure BW[1]",   0x00, buffer[77]);
  assertEquals("setWarning(bw) hw-failure BW[2]",   0x00, buffer[78]);
  assertEquals("setWarning(bw) hw-failure BW[3]",   0x00, buffer[79]);
  assertEquals("setWarning(sr) out-of-range SR[0]",   0x10, buffer[80]);
  assertEquals("setWarning(sr) out-of-range SR[1]",   0x00, buffer[81]);
  assertEquals("setWarning(sr) out-of-range SR[2]",   0x00, buffer[82]);
  assertEquals("setWarning(sr) out-of-range SR[3]",   0x00, buffer[83]);
  assertEquals("setError(bw) out-of-range BW[0]",   0x10, buffer[84]);
  assertEquals("setError(bw) out-of-range BW[1]",   0x00, buffer[85]);
  assertEquals("setError(bw) out-of-range BW[2]",   0x00, buffer[86]);
  assertEquals("setError(bw) out-of-range BW[3]",   0x00, buffer[87]);
  assertEquals("setError(sr) hw-failure SR[0]",   0x40, buffer[88]);
  assertEquals("setError(sr) hw-failure SR[1]",   0x00, buffer[89]);
  assertEquals("setError(sr) hw-failure SR[2]",   0x00, buffer[90]);
  assertEquals("setError(sr) hw-failure SR[3]",   0x00, buffer[91]);
  assertEquals("getWarning(bw) hw-failure", HARDWARE_DEVICE_FAILURE,
                                            p.getWarning(IndicatorFields::BANDWIDTH));
  assertEquals("getWarning(sr) out-of-range", PARAM_OUT_OF_RANGE,
                                              p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("getError(bw) out-of-range", PARAM_OUT_OF_RANGE,
                                            p.getError(IndicatorFields::BANDWIDTH));
  assertEquals("getError(sr) hw-failure", HARDWARE_DEVICE_FAILURE,
                                          p.getError(IndicatorFields::SAMPLE_RATE));
  assertEquals("getWarning(bw) hw-failure bits", 0x40 << 24, p.getWarning(IndicatorFields::BANDWIDTH));
  assertEquals("getWarning(sr) out-of-range bits", 0x10 << 24, p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("getError(bw) out-of-range bits", 0x10 << 24, p.getError(IndicatorFields::BANDWIDTH));
  assertEquals("getError(sr) hw-failure bits", 0x40 << 24, p.getError(IndicatorFields::SAMPLE_RATE));
  std::vector<WarningErrorField_t> warnings = p.getWarnings();
  assertEquals("getWarnings().size()", (size_t) 2, warnings.size());
  assertEquals("getWarnings() bw field", IndicatorFields::BANDWIDTH, warnings[0].field);
  assertEquals("getWarnings() bw responseField", HARDWARE_DEVICE_FAILURE,
                                                 warnings[0].responseField);
  assertEquals("getWarnings() bw responseField bits", 0x40 << 24, warnings[0].responseField);
  assertEquals("getWarnings() sr field", IndicatorFields::SAMPLE_RATE, warnings[1].field);
  assertEquals("getWarnings() sr responseField", PARAM_OUT_OF_RANGE,
                                                 warnings[1].responseField);
  assertEquals("getWarnings() sr responseField bits", 0x10 << 24, warnings[1].responseField);
  std::vector<WarningErrorField_t> errors = p.getErrors();
  assertEquals("getErrors().size()", (size_t) 2, errors.size());
  assertEquals("getErrors() bw field", IndicatorFields::BANDWIDTH, errors[0].field);
  assertEquals("getErrors() bw responseField", PARAM_OUT_OF_RANGE,
                                               errors[0].responseField);
  assertEquals("getErrors() bw responseField bits", 0x10 << 24, errors[0].responseField);
  assertEquals("getErrors() sr field", IndicatorFields::SAMPLE_RATE, errors[1].field);
  assertEquals("getErrors() sr responseField", HARDWARE_DEVICE_FAILURE,
                                               errors[1].responseField);
  assertEquals("getErrors() sr responseField bits", 0x40 << 24, errors[1].responseField);
}

void BasicAcknowledgePacketTest::testAddWarning () {
  BasicAcknowledgePacket p;
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() false", false, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(false)", 0x0, buffer[29] & 0x2);
  p.setWarningsGenerated(true);
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() true", true, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(true)", 0x2, buffer[29] & 0x2);
  p.addWarning(IndicatorFields::SAMPLE_RATE, PARAM_OUT_OF_RANGE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("addWarning(sr,out-of-range) WIF0[0]", 0x00, buffer[68]);
  assertEquals("addWarning(sr,out-of-range) WIF0[1]", 0x20, buffer[69]);
  assertEquals("addWarning(sr,out-of-range) WIF0[2]", 0x00, buffer[70]);
  assertEquals("addWarning(sr,out-of-range) WIF0[3]", 0x00, buffer[71]);
  assertEquals("addWarning(sr,out-of-range) SR[0]",   0x10, buffer[72]);
  assertEquals("addWarning(sr,out-of-range) SR[1]",   0x00, buffer[73]);
  assertEquals("addWarning(sr,out-of-range) SR[2]",   0x00, buffer[74]);
  assertEquals("addWarning(sr,out-of-range) SR[3]",   0x00, buffer[75]);
  assertEquals("getWarning(sr) out-of-range", PARAM_OUT_OF_RANGE,
                                              p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("getWarning(sr) out-of-range bits", 0x10 << 24, p.getWarning(IndicatorFields::SAMPLE_RATE));
  p.addWarning(IndicatorFields::SAMPLE_RATE, HARDWARE_DEVICE_FAILURE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("addWarning(sr,two-warnings) WIF0[0]", 0x00, buffer[68]);
  assertEquals("addWarning(sr,two-warnings) WIF0[1]", 0x20, buffer[69]);
  assertEquals("addWarning(sr,two-warnings) WIF0[2]", 0x00, buffer[70]);
  assertEquals("addWarning(sr,two-warnings) WIF0[3]", 0x00, buffer[71]);
  assertEquals("addWarning(sr,two-warnings) SR[0]",   0x50, buffer[72]);
  assertEquals("addWarning(sr,two-warnings) SR[1]",   0x00, buffer[73]);
  assertEquals("addWarning(sr,two-warnings) SR[2]",   0x00, buffer[74]);
  assertEquals("addWarning(sr,two-warnings) SR[3]",   0x00, buffer[75]);
  assertEquals("getWarning(sr) two-warnings", PARAM_OUT_OF_RANGE |
                                              HARDWARE_DEVICE_FAILURE,
                                              p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("getWarning(sr) two-warnings bits", 0x50 << 24, p.getWarning(IndicatorFields::SAMPLE_RATE));
  p.addWarning(IndicatorFields::SAMPLE_RATE, 0x0);
  buffer = (char*)p.getPacketPointer();
  assertEquals("addWarning(sr,0x0) WIF0[0]", 0x00, buffer[68]);
  assertEquals("addWarning(sr,0x0) WIF0[1]", 0x20, buffer[69]);
  assertEquals("addWarning(sr,0x0) WIF0[2]", 0x00, buffer[70]);
  assertEquals("addWarning(sr,0x0) WIF0[3]", 0x00, buffer[71]);
  assertEquals("addWarning(sr,0x0) SR[0]",   0x50, buffer[72]);
  assertEquals("addWarning(sr,0x0) SR[1]",   0x00, buffer[73]);
  assertEquals("addWarning(sr,0x0) SR[2]",   0x00, buffer[74]);
  assertEquals("addWarning(sr,0x0) SR[3]",   0x00, buffer[75]);
  assertEquals("getWarning(sr) add0x0", PARAM_OUT_OF_RANGE |
                                        HARDWARE_DEVICE_FAILURE,
                                        p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("getWarning(sr) add0x0 bits", 0x50 << 24, p.getWarning(IndicatorFields::SAMPLE_RATE));
  p.addWarning(IndicatorFields::SAMPLE_RATE, WEF_NULL);
  buffer = (char*)p.getPacketPointer();
  assertEquals("addWarning(sr,null) WIF0[0]", 0x00, buffer[68]);
  assertEquals("addWarning(sr,null) WIF0[1]", 0x20, buffer[69]);
  assertEquals("addWarning(sr,null) WIF0[2]", 0x00, buffer[70]);
  assertEquals("addWarning(sr,null) WIF0[3]", 0x00, buffer[71]);
  assertEquals("addWarning(sr,null) SR[0]",   0x50, buffer[72]);
  assertEquals("addWarning(sr,null) SR[1]",   0x00, buffer[73]);
  assertEquals("addWarning(sr,null) SR[2]",   0x00, buffer[74]);
  assertEquals("addWarning(sr,null) SR[3]",   0x00, buffer[75]);
  assertEquals("getWarning(sr) add-null", PARAM_OUT_OF_RANGE |
                                          HARDWARE_DEVICE_FAILURE,
                                          p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("getWarning(sr) add-null bits", 0x50 << 24, p.getWarning(IndicatorFields::SAMPLE_RATE));
  p.addWarning(IndicatorFields::SAMPLE_RATE, WEF_USER_DEFINED_10 |
                                             TIMESTAMP_PROBLEM);
  buffer = (char*)p.getPacketPointer();
  assertEquals("addWarning(sr,multiple) WIF0[0]", 0x00, buffer[68]);
  assertEquals("addWarning(sr,multiple) WIF0[1]", 0x20, buffer[69]);
  assertEquals("addWarning(sr,multiple) WIF0[2]", 0x00, buffer[70]);
  assertEquals("addWarning(sr,multiple) WIF0[3]", 0x00, buffer[71]);
  assertEquals("addWarning(sr,multiple) SR[0]",   0x52, buffer[72]);
  assertEquals("addWarning(sr,multiple) SR[1]",   0x00, buffer[73]);
  assertEquals("addWarning(sr,multiple) SR[2]",   0x04, buffer[74]);
  assertEquals("addWarning(sr,multiple) SR[3]",   0x00, buffer[75]);
  assertEquals("getWarning(sr) add-multiple", WEF_USER_DEFINED_10 |
                                              TIMESTAMP_PROBLEM |
                                              PARAM_OUT_OF_RANGE |
                                              HARDWARE_DEVICE_FAILURE,
                                              p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("getWarning(sr) add-multiple bits", (0x52<<24)+(0x04<<8),
                                                   p.getWarning(IndicatorFields::SAMPLE_RATE));
}

void BasicAcknowledgePacketTest::testRemoveWarning () {
  //assertEquals("what is int32 null?", INT32_NULL, 0x80000000);
  BasicAcknowledgePacket p;
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() false", false, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(false)", 0x0, buffer[29] & 0x2);
  p.setWarningsGenerated(true);
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() true", true, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(true)", 0x2, buffer[29] & 0x2);
  // remove non-existing from field with no warnings
  p.removeWarning(IndicatorFields::SAMPLE_RATE, PARAM_OUT_OF_RANGE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("removeWarning(sr,null) WIF0[0]", 0x00, buffer[68]);
  assertEquals("removeWarning(sr,null) WIF0[1]", 0x00, buffer[69]);
  assertEquals("removeWarning(sr,null) WIF0[2]", 0x00, buffer[70]);
  assertEquals("removeWarning(sr,null) WIF0[3]", 0x00, buffer[71]);
  assertEquals("getWarning(sr) null", WEF_NULL,
                                      p.getWarning(IndicatorFields::SAMPLE_RATE));
  // set some warnings that we can test removing
  p.setWarning(IndicatorFields::SAMPLE_RATE, WEF_USER_DEFINED_10 |
                                             TIMESTAMP_PROBLEM |
                                             PARAM_OUT_OF_RANGE |
                                             HARDWARE_DEVICE_FAILURE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("setWarning(sr,multiple) WIF0[0]", 0x00, buffer[68]);
  assertEquals("setWarning(sr,multiple) WIF0[1]", 0x20, buffer[69]);
  assertEquals("setWarning(sr,multiple) WIF0[2]", 0x00, buffer[70]);
  assertEquals("setWarning(sr,multiple) WIF0[3]", 0x00, buffer[71]);
  assertEquals("setWarning(sr,multiple) SR[0]",   0x52, buffer[72]);
  assertEquals("setWarning(sr,multiple) SR[1]",   0x00, buffer[73]);
  assertEquals("setWarning(sr,multiple) SR[2]",   0x04, buffer[74]);
  assertEquals("setWarning(sr,multiple) SR[3]",   0x00, buffer[75]);
  assertEquals("setWarning(sr) add-multiple", WEF_USER_DEFINED_10 |
                                              TIMESTAMP_PROBLEM |
                                              PARAM_OUT_OF_RANGE |
                                              HARDWARE_DEVICE_FAILURE,
                                              p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("setWarning(sr) add-multiple bits", (0x52<<24)+(0x04<<8),
                                                   p.getWarning(IndicatorFields::SAMPLE_RATE));
  // remove non-existing and null or 0x0 from field that has warnings
  p.removeWarning(IndicatorFields::SAMPLE_RATE, FIELD_NOT_EXECUTED);
  p.removeWarning(IndicatorFields::SAMPLE_RATE, WEF_NULL);
  buffer = (char*)p.getPacketPointer();
  assertEquals("removeWarning(sr,N/A) WIF0[0]", 0x00, buffer[68]);
  assertEquals("removeWarning(sr,N/A) WIF0[1]", 0x20, buffer[69]);
  assertEquals("removeWarning(sr,N/A) WIF0[2]", 0x00, buffer[70]);
  assertEquals("removeWarning(sr,N/A) WIF0[3]", 0x00, buffer[71]);
  assertEquals("removeWarning(sr,N/A) SR[0]",   0x52, buffer[72]);
  assertEquals("removeWarning(sr,N/A) SR[1]",   0x00, buffer[73]);
  assertEquals("removeWarning(sr,N/A) SR[2]",   0x04, buffer[74]);
  assertEquals("removeWarning(sr,N/A) SR[3]",   0x00, buffer[75]);
  assertEquals("getWarning(sr) remove-N/A", WEF_USER_DEFINED_10 |
                                            TIMESTAMP_PROBLEM |
                                            PARAM_OUT_OF_RANGE |
                                            HARDWARE_DEVICE_FAILURE,
                                            p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("getWarning(sr) remove-N/A bits", (0x52<<24)+(0x04<<8),
                                                 p.getWarning(IndicatorFields::SAMPLE_RATE));
  // remove one existing
  p.removeWarning(IndicatorFields::SAMPLE_RATE, HARDWARE_DEVICE_FAILURE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("removeWarning(sr,hw-failure) WIF0[0]", 0x00, buffer[68]);
  assertEquals("removeWarning(sr,hw-failure) WIF0[1]", 0x20, buffer[69]);
  assertEquals("removeWarning(sr,hw-failure) WIF0[2]", 0x00, buffer[70]);
  assertEquals("removeWarning(sr,hw-failure) WIF0[3]", 0x00, buffer[71]);
  assertEquals("removeWarning(sr,hw-failure) SR[0]",   0x12, buffer[72]);
  assertEquals("removeWarning(sr,hw-failure) SR[1]",   0x00, buffer[73]);
  assertEquals("removeWarning(sr,hw-failure) SR[2]",   0x04, buffer[74]);
  assertEquals("removeWarning(sr,hw-failure) SR[3]",   0x00, buffer[75]);
  assertEquals("getWarning(sr) remove-hw-failure", WEF_USER_DEFINED_10 |
                                                   TIMESTAMP_PROBLEM |
                                                   PARAM_OUT_OF_RANGE,
                                                   p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("getWarning(sr) remove-hw-failure bits", (0x12<<24)+(0x04<<8),
                                                        p.getWarning(IndicatorFields::SAMPLE_RATE));
  // remove multiple existing
  p.removeWarning(IndicatorFields::SAMPLE_RATE, TIMESTAMP_PROBLEM |
                                                PARAM_OUT_OF_RANGE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("removeWarning(sr,multiple) WIF0[0]", 0x00, buffer[68]);
  assertEquals("removeWarning(sr,multiple) WIF0[1]", 0x20, buffer[69]);
  assertEquals("removeWarning(sr,multiple) WIF0[2]", 0x00, buffer[70]);
  assertEquals("removeWarning(sr,multiple) WIF0[3]", 0x00, buffer[71]);
  assertEquals("removeWarning(sr,multiple) SR[0]",   0x00, buffer[72]);
  assertEquals("removeWarning(sr,multiple) SR[1]",   0x00, buffer[73]);
  assertEquals("removeWarning(sr,multiple) SR[2]",   0x04, buffer[74]);
  assertEquals("removeWarning(sr,multiple) SR[3]",   0x00, buffer[75]);
  assertEquals("getWarning(sr) remove-multiple", WEF_USER_DEFINED_10,
                                                 p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("getWarning(sr) remove-multiple bits", 0x04 << 8,
                                                      p.getWarning(IndicatorFields::SAMPLE_RATE));
  // remove all existing
  p.removeWarning(IndicatorFields::SAMPLE_RATE, WEF_USER_DEFINED_10);
  buffer = (char*)p.getPacketPointer();
  assertEquals("removeWarning(sr,all) WIF0[0]", 0x00, buffer[68]);
  assertEquals("removeWarning(sr,all) WIF0[1]", 0x00, buffer[69]);
  assertEquals("removeWarning(sr,all) WIF0[2]", 0x00, buffer[70]);
  assertEquals("removeWarning(sr,all) WIF0[3]", 0x00, buffer[71]);
  assertEquals("getWarning(sr) remove-all", WEF_NO_WARNING_ERROR,
                                            p.getWarning(IndicatorFields::SAMPLE_RATE));
}

void BasicAcknowledgePacketTest::testAddError () {
  BasicAcknowledgePacket p;
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getErrorsGenerated() false", false, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(false)", 0x0, buffer[29] & 0x1);
  p.setErrorsGenerated(true);
  buffer = (char*)p.getPacketPointer();
  assertEquals("getErrorsGenerated() true", true, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(true)", 0x1, buffer[29] & 0x1);
  p.addError(IndicatorFields::SAMPLE_RATE, PARAM_OUT_OF_RANGE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("addError(sr,out-of-range) WIF0[0]", 0x00, buffer[68]);
  assertEquals("addError(sr,out-of-range) WIF0[1]", 0x20, buffer[69]);
  assertEquals("addError(sr,out-of-range) WIF0[2]", 0x00, buffer[70]);
  assertEquals("addError(sr,out-of-range) WIF0[3]", 0x00, buffer[71]);
  assertEquals("addError(sr,out-of-range) SR[0]",   0x10, buffer[72]);
  assertEquals("addError(sr,out-of-range) SR[1]",   0x00, buffer[73]);
  assertEquals("addError(sr,out-of-range) SR[2]",   0x00, buffer[74]);
  assertEquals("addError(sr,out-of-range) SR[3]",   0x00, buffer[75]);
  assertEquals("getError(sr) out-of-range", PARAM_OUT_OF_RANGE,
                                            p.getError(IndicatorFields::SAMPLE_RATE));
  assertEquals("getError(sr) out-of-range bits", 0x10 << 24, p.getError(IndicatorFields::SAMPLE_RATE));
  p.addError(IndicatorFields::SAMPLE_RATE, HARDWARE_DEVICE_FAILURE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("addError(sr,two-errors) WIF0[0]", 0x00, buffer[68]);
  assertEquals("addError(sr,two-errors) WIF0[1]", 0x20, buffer[69]);
  assertEquals("addError(sr,two-errors) WIF0[2]", 0x00, buffer[70]);
  assertEquals("addError(sr,two-errors) WIF0[3]", 0x00, buffer[71]);
  assertEquals("addError(sr,two-errors) SR[0]",   0x50, buffer[72]);
  assertEquals("addError(sr,two-errors) SR[1]",   0x00, buffer[73]);
  assertEquals("addError(sr,two-errors) SR[2]",   0x00, buffer[74]);
  assertEquals("addError(sr,two-errors) SR[3]",   0x00, buffer[75]);
  assertEquals("getError(sr) two-errors", PARAM_OUT_OF_RANGE |
                                            HARDWARE_DEVICE_FAILURE,
                                            p.getError(IndicatorFields::SAMPLE_RATE));
  assertEquals("getError(sr) two-errors bits", 0x50 << 24, p.getError(IndicatorFields::SAMPLE_RATE));
  p.addError(IndicatorFields::SAMPLE_RATE, 0x0);
  buffer = (char*)p.getPacketPointer();
  assertEquals("addError(sr,0x0) WIF0[0]", 0x00, buffer[68]);
  assertEquals("addError(sr,0x0) WIF0[1]", 0x20, buffer[69]);
  assertEquals("addError(sr,0x0) WIF0[2]", 0x00, buffer[70]);
  assertEquals("addError(sr,0x0) WIF0[3]", 0x00, buffer[71]);
  assertEquals("addError(sr,0x0) SR[0]",   0x50, buffer[72]);
  assertEquals("addError(sr,0x0) SR[1]",   0x00, buffer[73]);
  assertEquals("addError(sr,0x0) SR[2]",   0x00, buffer[74]);
  assertEquals("addError(sr,0x0) SR[3]",   0x00, buffer[75]);
  assertEquals("getError(sr) add0x0", PARAM_OUT_OF_RANGE |
                                      HARDWARE_DEVICE_FAILURE,
                                      p.getError(IndicatorFields::SAMPLE_RATE));
  assertEquals("getError(sr) add0x0 bits", 0x50 << 24, p.getError(IndicatorFields::SAMPLE_RATE));
  p.addError(IndicatorFields::SAMPLE_RATE, WEF_NULL);
  buffer = (char*)p.getPacketPointer();
  assertEquals("addError(sr,null) WIF0[0]", 0x00, buffer[68]);
  assertEquals("addError(sr,null) WIF0[1]", 0x20, buffer[69]);
  assertEquals("addError(sr,null) WIF0[2]", 0x00, buffer[70]);
  assertEquals("addError(sr,null) WIF0[3]", 0x00, buffer[71]);
  assertEquals("addError(sr,null) SR[0]",   0x50, buffer[72]);
  assertEquals("addError(sr,null) SR[1]",   0x00, buffer[73]);
  assertEquals("addError(sr,null) SR[2]",   0x00, buffer[74]);
  assertEquals("addError(sr,null) SR[3]",   0x00, buffer[75]);
  assertEquals("getError(sr) add-null", PARAM_OUT_OF_RANGE |
                                        HARDWARE_DEVICE_FAILURE,
                                        p.getError(IndicatorFields::SAMPLE_RATE));
  assertEquals("getError(sr) add-null bits", 0x50 << 24, p.getError(IndicatorFields::SAMPLE_RATE));
  p.addError(IndicatorFields::SAMPLE_RATE, WEF_USER_DEFINED_10 |
                                           TIMESTAMP_PROBLEM);
  buffer = (char*)p.getPacketPointer();
  assertEquals("addError(sr,multiple) WIF0[0]", 0x00, buffer[68]);
  assertEquals("addError(sr,multiple) WIF0[1]", 0x20, buffer[69]);
  assertEquals("addError(sr,multiple) WIF0[2]", 0x00, buffer[70]);
  assertEquals("addError(sr,multiple) WIF0[3]", 0x00, buffer[71]);
  assertEquals("addError(sr,multiple) SR[0]",   0x52, buffer[72]);
  assertEquals("addError(sr,multiple) SR[1]",   0x00, buffer[73]);
  assertEquals("addError(sr,multiple) SR[2]",   0x04, buffer[74]);
  assertEquals("addError(sr,multiple) SR[3]",   0x00, buffer[75]);
  assertEquals("getError(sr) add-multiple", WEF_USER_DEFINED_10 |
                                            TIMESTAMP_PROBLEM |
                                            PARAM_OUT_OF_RANGE |
                                            HARDWARE_DEVICE_FAILURE,
                                            p.getError(IndicatorFields::SAMPLE_RATE));
  assertEquals("getError(sr) add-multiple bits", (0x52<<24)+(0x04<<8),
                                                 p.getError(IndicatorFields::SAMPLE_RATE));
}

void BasicAcknowledgePacketTest::testRemoveError () {
  //assertEquals("what is int32 null?", INT32_NULL, 0x80000000);
  BasicAcknowledgePacket p;
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getErrorsGenerated() false", false, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(false)", 0x0, buffer[29] & 0x1);
  p.setErrorsGenerated(true);
  buffer = (char*)p.getPacketPointer();
  assertEquals("getErrorsGenerated() true", true, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(true)", 0x1, buffer[29] & 0x1);
  // remove non-existing from field with no errors
  p.removeError(IndicatorFields::SAMPLE_RATE, PARAM_OUT_OF_RANGE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("removeError(sr,null) WIF0[0]", 0x00, buffer[68]);
  assertEquals("removeError(sr,null) WIF0[1]", 0x00, buffer[69]);
  assertEquals("removeError(sr,null) WIF0[2]", 0x00, buffer[70]);
  assertEquals("removeError(sr,null) WIF0[3]", 0x00, buffer[71]);
  assertEquals("getError(sr) null", WEF_NULL,
                                    p.getError(IndicatorFields::SAMPLE_RATE));
  // set some errors that we can test removing
  p.setError(IndicatorFields::SAMPLE_RATE, WEF_USER_DEFINED_10 |
                                           TIMESTAMP_PROBLEM |
                                           PARAM_OUT_OF_RANGE |
                                           HARDWARE_DEVICE_FAILURE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("setError(sr,multiple) WIF0[0]", 0x00, buffer[68]);
  assertEquals("setError(sr,multiple) WIF0[1]", 0x20, buffer[69]);
  assertEquals("setError(sr,multiple) WIF0[2]", 0x00, buffer[70]);
  assertEquals("setError(sr,multiple) WIF0[3]", 0x00, buffer[71]);
  assertEquals("setError(sr,multiple) SR[0]",   0x52, buffer[72]);
  assertEquals("setError(sr,multiple) SR[1]",   0x00, buffer[73]);
  assertEquals("setError(sr,multiple) SR[2]",   0x04, buffer[74]);
  assertEquals("setError(sr,multiple) SR[3]",   0x00, buffer[75]);
  assertEquals("setError(sr) add-multiple", WEF_USER_DEFINED_10 |
                                            TIMESTAMP_PROBLEM |
                                            PARAM_OUT_OF_RANGE |
                                            HARDWARE_DEVICE_FAILURE,
                                            p.getError(IndicatorFields::SAMPLE_RATE));
  assertEquals("setError(sr) add-multiple bits", (0x52<<24)+(0x04<<8),
                                                 p.getError(IndicatorFields::SAMPLE_RATE));
  // remove non-existing and null or 0x0 from field that has errors
  p.removeError(IndicatorFields::SAMPLE_RATE, FIELD_NOT_EXECUTED);
  p.removeError(IndicatorFields::SAMPLE_RATE, WEF_NULL);
  buffer = (char*)p.getPacketPointer();
  assertEquals("removeError(sr,N/A) WIF0[0]", 0x00, buffer[68]);
  assertEquals("removeError(sr,N/A) WIF0[1]", 0x20, buffer[69]);
  assertEquals("removeError(sr,N/A) WIF0[2]", 0x00, buffer[70]);
  assertEquals("removeError(sr,N/A) WIF0[3]", 0x00, buffer[71]);
  assertEquals("removeError(sr,N/A) SR[0]",   0x52, buffer[72]);
  assertEquals("removeError(sr,N/A) SR[1]",   0x00, buffer[73]);
  assertEquals("removeError(sr,N/A) SR[2]",   0x04, buffer[74]);
  assertEquals("removeError(sr,N/A) SR[3]",   0x00, buffer[75]);
  assertEquals("getError(sr) remove-N/A", WEF_USER_DEFINED_10 |
                                          TIMESTAMP_PROBLEM |
                                          PARAM_OUT_OF_RANGE |
                                          HARDWARE_DEVICE_FAILURE,
                                          p.getError(IndicatorFields::SAMPLE_RATE));
  assertEquals("getError(sr) remove-N/A bits", (0x52<<24)+(0x04<<8),
                                               p.getError(IndicatorFields::SAMPLE_RATE));
  // remove one existing
  p.removeError(IndicatorFields::SAMPLE_RATE, HARDWARE_DEVICE_FAILURE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("removeError(sr,hw-failure) WIF0[0]", 0x00, buffer[68]);
  assertEquals("removeError(sr,hw-failure) WIF0[1]", 0x20, buffer[69]);
  assertEquals("removeError(sr,hw-failure) WIF0[2]", 0x00, buffer[70]);
  assertEquals("removeError(sr,hw-failure) WIF0[3]", 0x00, buffer[71]);
  assertEquals("removeError(sr,hw-failure) SR[0]",   0x12, buffer[72]);
  assertEquals("removeError(sr,hw-failure) SR[1]",   0x00, buffer[73]);
  assertEquals("removeError(sr,hw-failure) SR[2]",   0x04, buffer[74]);
  assertEquals("removeError(sr,hw-failure) SR[3]",   0x00, buffer[75]);
  assertEquals("getError(sr) remove-hw-failure", WEF_USER_DEFINED_10 |
                                                 TIMESTAMP_PROBLEM |
                                                 PARAM_OUT_OF_RANGE,
                                                 p.getError(IndicatorFields::SAMPLE_RATE));
  assertEquals("getError(sr) remove-hw-failure bits", (0x12<<24)+(0x04<<8),
                                                      p.getError(IndicatorFields::SAMPLE_RATE));
  // remove multiple existing
  p.removeError(IndicatorFields::SAMPLE_RATE, TIMESTAMP_PROBLEM |
                                              PARAM_OUT_OF_RANGE);
  buffer = (char*)p.getPacketPointer();
  assertEquals("removeError(sr,multiple) WIF0[0]", 0x00, buffer[68]);
  assertEquals("removeError(sr,multiple) WIF0[1]", 0x20, buffer[69]);
  assertEquals("removeError(sr,multiple) WIF0[2]", 0x00, buffer[70]);
  assertEquals("removeError(sr,multiple) WIF0[3]", 0x00, buffer[71]);
  assertEquals("removeError(sr,multiple) SR[0]",   0x00, buffer[72]);
  assertEquals("removeError(sr,multiple) SR[1]",   0x00, buffer[73]);
  assertEquals("removeError(sr,multiple) SR[2]",   0x04, buffer[74]);
  assertEquals("removeError(sr,multiple) SR[3]",   0x00, buffer[75]);
  assertEquals("getError(sr) remove-multiple", WEF_USER_DEFINED_10,
                                               p.getError(IndicatorFields::SAMPLE_RATE));
  assertEquals("getError(sr) remove-multiple bits", 0x04 << 8,
                                                    p.getError(IndicatorFields::SAMPLE_RATE));
  // remove all existing
  p.removeError(IndicatorFields::SAMPLE_RATE, WEF_USER_DEFINED_10);
  buffer = (char*)p.getPacketPointer();
  assertEquals("removeError(sr,all) WIF0[0]", 0x00, buffer[68]);
  assertEquals("removeError(sr,all) WIF0[1]", 0x00, buffer[69]);
  assertEquals("removeError(sr,all) WIF0[2]", 0x00, buffer[70]);
  assertEquals("removeError(sr,all) WIF0[3]", 0x00, buffer[71]);
  assertEquals("getError(sr) remove-all", WEF_NO_WARNING_ERROR,
                                          p.getError(IndicatorFields::SAMPLE_RATE));
}

// Test that FIELD_NOT_EXECUTED works, which happens to be the same value as INT32_NULL
void BasicAcknowledgePacketTest::testNullWarning () {
  assertEquals("FIELD_NOT_EXECUTED==INT32_NULL", FIELD_NOT_EXECUTED, INT32_NULL);
  assertEquals("INT32_NULL==0x80000000",         INT32_NULL,                            0x80000000);
  assertEquals("WEF_NO_WARNING_ERROR==WEF_NULL", WEF_NO_WARNING_ERROR,
                                                 WEF_NULL);
  assertEquals("WEF_NULL==0x00000000",           WEF_NULL,           0x00000000);
  BasicAcknowledgePacket p;
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() false", false, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(false)", 0x0, buffer[29] & 0x2);
  p.setWarningsGenerated(true);
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() true", true, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(true)", 0x2, buffer[29] & 0x2);
  // set FIELD_NOT_EXECUTED
  p.setWarning(IndicatorFields::SAMPLE_RATE, FIELD_NOT_EXECUTED);
  buffer = (char*)p.getPacketPointer();
  assertEquals("setWarning(sr,not-exe) WIF0[0]", 0x00, buffer[68]);
  assertEquals("setWarning(sr,not-exe) WIF0[1]", 0x20, buffer[69]);
  assertEquals("setWarning(sr,not-exe) WIF0[2]", 0x00, buffer[70]);
  assertEquals("setWarning(sr,not-exe) WIF0[3]", 0x00, buffer[71]);
  assertEquals("setWarning(sr,not-exe) SR[0]",   (char) 0x80, buffer[72]);
  assertEquals("setWarning(sr,not-exe) SR[1]",   0x00, buffer[73]);
  assertEquals("setWarning(sr,not-exe) SR[2]",   0x00, buffer[74]);
  assertEquals("setWarning(sr,not-exe) SR[3]",   0x00, buffer[75]);
  assertEquals("setWarning(sr) not-exe", FIELD_NOT_EXECUTED,
                                         p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("setWarning(sr) not-exe bits", (int32_t) (0x80 << 24),
                                              p.getWarning(IndicatorFields::SAMPLE_RATE));
  // set WEF_NULL
  p.setWarning(IndicatorFields::SAMPLE_RATE, WEF_NULL);
  buffer = (char*)p.getPacketPointer();
  assertEquals("setWarning(sr,null) WIF0[0]", 0x00, buffer[68]);
  assertEquals("setWarning(sr,null) WIF0[1]", 0x00, buffer[69]);
  assertEquals("setWarning(sr,null) WIF0[2]", 0x00, buffer[70]);
  assertEquals("setWarning(sr,null) WIF0[3]", 0x00, buffer[71]);
  assertEquals("getWarning(sr) null", WEF_NULL,
                                      p.getWarning(IndicatorFields::SAMPLE_RATE));
  // add FIELD_NOT_EXECUTED
  p.addWarning(IndicatorFields::SAMPLE_RATE, FIELD_NOT_EXECUTED);
  buffer = (char*)p.getPacketPointer();
  assertEquals("addWarning(sr,not-exe) WIF0[0]", 0x00, buffer[68]);
  assertEquals("addWarning(sr,not-exe) WIF0[1]", 0x20, buffer[69]);
  assertEquals("addWarning(sr,not-exe) WIF0[2]", 0x00, buffer[70]);
  assertEquals("addWarning(sr,not-exe) WIF0[3]", 0x00, buffer[71]);
  assertEquals("addWarning(sr,not-exe) SR[0]",   (char) 0x80, buffer[72]);
  assertEquals("addWarning(sr,not-exe) SR[1]",   0x00, buffer[73]);
  assertEquals("addWarning(sr,not-exe) SR[2]",   0x00, buffer[74]);
  assertEquals("addWarning(sr,not-exe) SR[3]",   0x00, buffer[75]);
  assertEquals("getWarning(sr) not-exe", FIELD_NOT_EXECUTED,
                                         p.getWarning(IndicatorFields::SAMPLE_RATE));
  assertEquals("getWarning(sr) not-exe bits", (int32_t) (0x80 << 24),
                                              p.getWarning(IndicatorFields::SAMPLE_RATE));
  // remove FIELD_NOT_EXECUTED
  p.removeWarning(IndicatorFields::SAMPLE_RATE, FIELD_NOT_EXECUTED);
  buffer = (char*)p.getPacketPointer();
  assertEquals("removeWarning(sr,not-exe) WIF0[0]", 0x00, buffer[68]);
  assertEquals("removeWarning(sr,not-exe) WIF0[1]", 0x00, buffer[69]);
  assertEquals("removeWarning(sr,not-exe) WIF0[2]", 0x00, buffer[70]);
  assertEquals("removeWarning(sr,not-exe) WIF0[3]", 0x00, buffer[71]);
  assertEquals("getWarning(sr) remove not-exe", WEF_NULL,
                                                p.getWarning(IndicatorFields::SAMPLE_RATE));
}

// Test that FIELD_NOT_EXECUTED works, which happens to be the same value as INT32_NULL
void BasicAcknowledgePacketTest::testNullError () {
  assertEquals("FIELD_NOT_EXECUTED==INT32_NULL", FIELD_NOT_EXECUTED, INT32_NULL);
  assertEquals("INT32_NULL==0x80000000",         INT32_NULL,                            0x80000000);
  assertEquals("WEF_NO_WARNING_ERROR==WEF_NULL", WEF_NO_WARNING_ERROR,
                                                 WEF_NULL);
  assertEquals("WEF_NULL==0x00000000",           WEF_NULL,           0x00000000);
  BasicAcknowledgePacket p;
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getErrorsGenerated() false", false, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(false)", 0x0, buffer[29] & 0x1);
  p.setErrorsGenerated(true);
  buffer = (char*)p.getPacketPointer();
  assertEquals("getErrorsGenerated() true", true, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(true)", 0x1, buffer[29] & 0x1);
  // set FIELD_NOT_EXECUTED
  p.setError(IndicatorFields::SAMPLE_RATE, FIELD_NOT_EXECUTED);
  buffer = (char*)p.getPacketPointer();
  assertEquals("setError(sr,not-exe) WIF0[0]", 0x00, buffer[68]);
  assertEquals("setError(sr,not-exe) WIF0[1]", 0x20, buffer[69]);
  assertEquals("setError(sr,not-exe) WIF0[2]", 0x00, buffer[70]);
  assertEquals("setError(sr,not-exe) WIF0[3]", 0x00, buffer[71]);
  assertEquals("setError(sr,not-exe) SR[0]",   (char) 0x80, buffer[72]);
  assertEquals("setError(sr,not-exe) SR[1]",   0x00, buffer[73]);
  assertEquals("setError(sr,not-exe) SR[2]",   0x00, buffer[74]);
  assertEquals("setError(sr,not-exe) SR[3]",   0x00, buffer[75]);
  assertEquals("setError(sr) not-exe", FIELD_NOT_EXECUTED,
                                       p.getError(IndicatorFields::SAMPLE_RATE));
  assertEquals("setError(sr) not-exe bits", (int32_t) (0x80 << 24),
                                            p.getError(IndicatorFields::SAMPLE_RATE));
  // set WEF_NULL
  p.setError(IndicatorFields::SAMPLE_RATE, WEF_NULL);
  buffer = (char*)p.getPacketPointer();
  assertEquals("setError(sr,null) WIF0[0]", 0x00, buffer[68]);
  assertEquals("setError(sr,null) WIF0[1]", 0x00, buffer[69]);
  assertEquals("setError(sr,null) WIF0[2]", 0x00, buffer[70]);
  assertEquals("setError(sr,null) WIF0[3]", 0x00, buffer[71]);
  assertEquals("getError(sr) null", WEF_NULL,
                                    p.getError(IndicatorFields::SAMPLE_RATE));
  // add FIELD_NOT_EXECUTED
  p.addError(IndicatorFields::SAMPLE_RATE, FIELD_NOT_EXECUTED);
  buffer = (char*)p.getPacketPointer();
  assertEquals("addError(sr,not-exe) WIF0[0]", 0x00, buffer[68]);
  assertEquals("addError(sr,not-exe) WIF0[1]", 0x20, buffer[69]);
  assertEquals("addError(sr,not-exe) WIF0[2]", 0x00, buffer[70]);
  assertEquals("addError(sr,not-exe) WIF0[3]", 0x00, buffer[71]);
  assertEquals("addError(sr,not-exe) SR[0]",   (char) 0x80, buffer[72]);
  assertEquals("addError(sr,not-exe) SR[1]",   0x00, buffer[73]);
  assertEquals("addError(sr,not-exe) SR[2]",   0x00, buffer[74]);
  assertEquals("addError(sr,not-exe) SR[3]",   0x00, buffer[75]);
  assertEquals("getError(sr) not-exe", FIELD_NOT_EXECUTED,
                                       p.getError(IndicatorFields::SAMPLE_RATE));
  assertEquals("getError(sr) not-exe bits", (int32_t) (0x80 << 24),
                                            p.getError(IndicatorFields::SAMPLE_RATE));
  // remove FIELD_NOT_EXECUTED
  p.removeError(IndicatorFields::SAMPLE_RATE, FIELD_NOT_EXECUTED);
  buffer = (char*)p.getPacketPointer();
  assertEquals("removeError(sr,not-exe) WIF0[0]", 0x00, buffer[68]);
  assertEquals("removeError(sr,not-exe) WIF0[1]", 0x00, buffer[69]);
  assertEquals("removeError(sr,not-exe) WIF0[2]", 0x00, buffer[70]);
  assertEquals("removeError(sr,not-exe) WIF0[3]", 0x00, buffer[71]);
  assertEquals("getError(sr) remove not-exe", WEF_NULL,
                                              p.getError(IndicatorFields::SAMPLE_RATE));
}

void BasicAcknowledgePacketTest::testFreeFormWarning() {
  const string msg1_in("TEST ERROR MESSAGE");   // two bytes less than 5 full 4-byte words
  const string msg2_in("TEST ERROR MESSAGE!!"); // exactly 5 full 4-byte words
  BasicAcknowledgePacket p;
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() false", false, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(false)", 0x0, buffer[29] & 0x2);
  assertEquals("hasFreeFormMessage() false", false, p.hasFreeFormMessage());
  int16_t pLen = (buffer[2]<<8) + buffer[3];
  assertEquals("packet size no free-form msg", (int16_t) 17, pLen);
  assertEquals("getFreeFormMessage() empty true", true, p.getFreeFormMessage().empty());
  assertEquals("getFreeFormMessage() size 0", (size_t) 0, p.getFreeFormMessage().size());
  assertEquals("getFreeFormMessage() text size 0", (size_t) 0, p.getFreeFormMessage().text().size());
  assertEquals("getFreeFormMessage() empty str", "", p.getFreeFormMessage().text());
  p.setWarningsGenerated(true);
  buffer = (char*)p.getPacketPointer();
  assertEquals("getWarningsGenerated() true", true, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(true)", 0x2, buffer[29] & 0x2);
  assertEquals("hasFreeFormMessage() false", false, p.hasFreeFormMessage());
  pLen = (buffer[2]<<8) + buffer[3];
  assertEquals("packet size no free-form msg", (int16_t) 18, pLen);
  assertEquals("getFreeFormMessage() empty true", true, p.getFreeFormMessage().empty());
  assertEquals("getFreeFormMessage() size 0", (size_t) 0, p.getFreeFormMessage().size());
  assertEquals("getFreeFormMessage() text size 0", (size_t) 0, p.getFreeFormMessage().text().size());
  assertEquals("getFreeFormMessage() empty str", "", p.getFreeFormMessage().text());
  p.setFreeFormMessage(FreeFormMessage_t(msg1_in));
  buffer = (char*)p.getPacketPointer();
  assertEquals("hasFreeFormMessage()1 true", true, p.hasFreeFormMessage());
  pLen = (buffer[2]<<8) + buffer[3];
  assertEquals("packet size w/ 18 byte free-form text msg1", (int16_t) 23, pLen);
  assertEquals("getFreeFormMessage()1 empty false", false, p.getFreeFormMessage().empty());
  assertEquals("getFreeFormMessage()1 size 20", (size_t) 20, p.getFreeFormMessage().size());
  assertEquals("getFreeFormMessage()1 text size 18", (size_t) 18, p.getFreeFormMessage().text().size());
  assertEquals("getFreeFormMessage()1 text", msg1_in, p.getFreeFormMessage().text());
  for (size_t i = 0; i < msg1_in.size(); i++) {
    assertEquals("setFreeFormMessage()1 characters", msg1_in[i], buffer[72+i]);
  }
  assertEquals("setFreeFormMessage()1 null-termination", '\0', buffer[72+msg1_in.size()]);
  p.setFreeFormMessage(msg2_in);
  buffer = (char*)p.getPacketPointer();
  assertEquals("hasFreeFormMessage()2 true", true, p.hasFreeFormMessage());
  pLen = (buffer[2]<<8) + buffer[3];
  assertEquals("packet size w/ 20 byte free-form text msg2", (int16_t) 24, pLen);
  assertEquals("getFreeFormMessage()2 empty false", false, p.getFreeFormMessage().empty());
  assertEquals("getFreeFormMessage()2 size 24", (size_t) 24, p.getFreeFormMessage().size());
  assertEquals("getFreeFormMessage()2 text size 20", (size_t) 20, p.getFreeFormMessage().text().size());
  assertEquals("getFreeFormMessage()2 text", msg2_in, p.getFreeFormMessage().text());
  for (size_t i = 0; i < msg2_in.size(); i++) {
    assertEquals("setFreeFormMessage()2 characters", msg2_in[i], buffer[72+i]);
  }
  assertEquals("setFreeFormMessage()2 null-termination", '\0', buffer[72+msg2_in.size()]);
  p.setFreeFormMessage(FreeFormMessage_t(&msg2_in[0], msg2_in.size()));
  buffer = (char*)p.getPacketPointer();
  assertEquals("hasFreeFormMessage()2b true", true, p.hasFreeFormMessage());
  pLen = (buffer[2]<<8) + buffer[3];
  assertEquals("packet size w/ 20 byte free-form byte msg2b", (int16_t) 23, pLen);
  assertEquals("getFreeFormMessage()2b empty false", false, p.getFreeFormMessage().empty());
  assertEquals("getFreeFormMessage()2b size 20", (size_t) 20, p.getFreeFormMessage().size());
  assertEquals("getFreeFormMessage()2b text size 20", (size_t) 20, p.getFreeFormMessage().text().size());
  assertEquals("getFreeFormMessage()2b text", msg2_in, p.getFreeFormMessage().text());
  for (size_t i = 0; i < msg2_in.size(); i++) {
    assertEquals("setFreeFormMessage()2b bytes", msg2_in[i], buffer[72+i]);
  }
  p.setFreeFormMessage(std::string(""));
  buffer = (char*)p.getPacketPointer();
  assertEquals("hasFreeFormMessage()3 false", false, p.hasFreeFormMessage());
  pLen = (buffer[2]<<8) + buffer[3];
  assertEquals("packet size w/ no free-form msg3", (int16_t) 18, pLen);
  assertEquals("getFreeFormMessage()3 empty true", true, p.getFreeFormMessage().empty());
  assertEquals("getFreeFormMessage()3 size 0", (size_t) 0, p.getFreeFormMessage().size());
  assertEquals("getFreeFormMessage()3 text size 0", (size_t) 0, p.getFreeFormMessage().text().size());
  assertEquals("getFreeFormMessage()3 empty str", "", p.getFreeFormMessage().text());
  // TODO - what happens if unset warning flag w/ free-form message in place? exception? remove free-form?
}
void BasicAcknowledgePacketTest::testFreeFormError() {
  const string msg1_in("TEST ERROR MESSAGE");   // two bytes less than 5 full 4-byte words
  const string msg2_in("TEST ERROR MESSAGE!!"); // exactly 5 full 4-byte words
  BasicAcknowledgePacket p;
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getErrorsGenerated() false", false, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(false)", 0x0, buffer[29] & 0x1);
  assertEquals("hasFreeFormMessage() false", false, p.hasFreeFormMessage());
  int16_t pLen = (buffer[2]<<8) + buffer[3];
  assertEquals("packet size no free-form msg", (int16_t) 17, pLen);
  assertEquals("getFreeFormMessage() empty true", true, p.getFreeFormMessage().empty());
  assertEquals("getFreeFormMessage() size 0", (size_t) 0, p.getFreeFormMessage().size());
  assertEquals("getFreeFormMessage() text size 0", (size_t) 0, p.getFreeFormMessage().text().size());
  assertEquals("getFreeFormMessage() empty str", "", p.getFreeFormMessage().text());
  p.setErrorsGenerated(true);
  buffer = (char*)p.getPacketPointer();
  assertEquals("getErrorsGenerated() true", true, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(true)", 0x1, buffer[29] & 0x1);
  assertEquals("hasFreeFormMessage() false", false, p.hasFreeFormMessage());
  pLen = (buffer[2]<<8) + buffer[3];
  assertEquals("packet size no free-form msg", (int16_t) 18, pLen);
  assertEquals("getFreeFormMessage() empty true", true, p.getFreeFormMessage().empty());
  assertEquals("getFreeFormMessage() size 0", (size_t) 0, p.getFreeFormMessage().size());
  assertEquals("getFreeFormMessage() text size 0", (size_t) 0, p.getFreeFormMessage().text().size());
  assertEquals("getFreeFormMessage() empty str", "", p.getFreeFormMessage().text());
  p.setFreeFormMessage(FreeFormMessage_t(msg1_in));
  buffer = (char*)p.getPacketPointer();
  assertEquals("hasFreeFormMessage()1 true", true, p.hasFreeFormMessage());
  pLen = (buffer[2]<<8) + buffer[3];
  assertEquals("packet size w/ 18 byte free-form text msg1", (int16_t) 23, pLen);
  assertEquals("getFreeFormMessage()1 empty false", false, p.getFreeFormMessage().empty());
  assertEquals("getFreeFormMessage()1 size 20", (size_t) 20, p.getFreeFormMessage().size());
  assertEquals("getFreeFormMessage()1 text size 18", (size_t) 18, p.getFreeFormMessage().text().size());
  assertEquals("getFreeFormMessage()1 text", msg1_in, p.getFreeFormMessage().text());
  for (size_t i = 0; i < msg1_in.size(); i++) {
    assertEquals("setFreeFormMessage()1 characters", msg1_in[i], buffer[72+i]);
  }
  assertEquals("setFreeFormMessage()1 null-termination", '\0', buffer[72+msg1_in.size()]);
  p.setFreeFormMessage(msg2_in);
  buffer = (char*)p.getPacketPointer();
  assertEquals("hasFreeFormMessage()2 true", true, p.hasFreeFormMessage());
  pLen = (buffer[2]<<8) + buffer[3];
  assertEquals("packet size w/ 20 byte free-form text msg2", (int16_t) 24, pLen);
  assertEquals("getFreeFormMessage()2 empty false", false, p.getFreeFormMessage().empty());
  assertEquals("getFreeFormMessage()2 size 24", (size_t) 24, p.getFreeFormMessage().size());
  assertEquals("getFreeFormMessage()2 text size 20", (size_t) 20, p.getFreeFormMessage().text().size());
  assertEquals("getFreeFormMessage()2 text", msg2_in, p.getFreeFormMessage().text());
  for (size_t i = 0; i < msg2_in.size(); i++) {
    assertEquals("setFreeFormMessage()2 characters", msg2_in[i], buffer[72+i]);
  }
  assertEquals("setFreeFormMessage()2 null-termination", '\0', buffer[72+msg2_in.size()]);
  p.setFreeFormMessage(FreeFormMessage_t(&msg2_in[0], msg2_in.size()));
  buffer = (char*)p.getPacketPointer();
  assertEquals("hasFreeFormMessage()2b true", true, p.hasFreeFormMessage());
  pLen = (buffer[2]<<8) + buffer[3];
  assertEquals("packet size w/ 20 byte free-form byte msg2b", (int16_t) 23, pLen);
  assertEquals("getFreeFormMessage()2b empty false", false, p.getFreeFormMessage().empty());
  assertEquals("getFreeFormMessage()2b size 20", (size_t) 20, p.getFreeFormMessage().size());
  assertEquals("getFreeFormMessage()2b text size 20", (size_t) 20, p.getFreeFormMessage().text().size());
  assertEquals("getFreeFormMessage()2b text", msg2_in, p.getFreeFormMessage().text());
  for (size_t i = 0; i < msg2_in.size(); i++) {
    assertEquals("setFreeFormMessage()2b bytes", msg2_in[i], buffer[72+i]);
  }
  p.setFreeFormMessage(std::string(""));
  buffer = (char*)p.getPacketPointer();
  assertEquals("hasFreeFormMessage()3 false", false, p.hasFreeFormMessage());
  pLen = (buffer[2]<<8) + buffer[3];
  assertEquals("packet size w/ no free-form msg3", (int16_t) 18, pLen);
  assertEquals("getFreeFormMessage()3 empty true", true, p.getFreeFormMessage().empty());
  assertEquals("getFreeFormMessage()3 size 0", (size_t) 0, p.getFreeFormMessage().size());
  assertEquals("getFreeFormMessage()3 text size 0", (size_t) 0, p.getFreeFormMessage().text().size());
  assertEquals("getFreeFormMessage()3 empty str", "", p.getFreeFormMessage().text());
}

// TODO - add tests for additional/multiple CIFs

//////////////////////////////////////////////////////////////////////////////
// INTERNAL METHODS
//////////////////////////////////////////////////////////////////////////////

