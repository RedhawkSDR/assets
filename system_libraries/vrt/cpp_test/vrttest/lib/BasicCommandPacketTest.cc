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

#include "BasicCommandPacketTest.h"
//#include "BasicDataPacket.h"
#include "BasicCommandPacket.h"
#include "HasFieldsTest.h"
#include "TestRunner.h"
#include "VRTConfig.h"

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

// any #defines? bitfield definitions? TODO

string BasicCommandPacketTest::getTestedClass () {
  return "vrt::BasicCommandPacket";
}

vector<TestSet> BasicCommandPacketTest::init () {
  vector<TestSet> tests;

  tests.push_back(TestSet("setPacketType",                this, "testSetPacketType"));
  tests.push_back(TestSet("resetForResend",               this, "testResetForResend"));

  tests.push_back(TestSet("setControlleeEnable",             this, "+testSetControlleeID"));
  tests.push_back(TestSet("setControlleeFormat",             this, "+testSetControlleeID"));
  tests.push_back(TestSet("setControllerEnable",             this, "+testSetControllerID"));
  tests.push_back(TestSet("setControllerFormat",             this, "+testSetControllerID"));
  tests.push_back(TestSet("setPartialChangePermitted",       this, "testSetPartialChangePermitted"));
  tests.push_back(TestSet("setWarningsPermitted",            this, "testSetWarningsPermitted"));
  tests.push_back(TestSet("setErrorsPermitted",              this, "testSetErrorsPermitted"));
  tests.push_back(TestSet("setAction",                       this, "testSetAction")); // setAction1Flag, setAction0Flag, setActionNoAction, setActionDryRun, setActionTakeAction
  tests.push_back(TestSet("setAction1Flag",                  this, "+testSetAction"));
  tests.push_back(TestSet("setAction0Flag",                  this, "+testSetAction"));
  tests.push_back(TestSet("setActionNoAction",               this, "+testSetAction"));
  tests.push_back(TestSet("setActionDryRun",                 this, "+testSetAction"));
  tests.push_back(TestSet("setActionTakeAction",             this, "+testSetAction"));
  tests.push_back(TestSet("setNotAckOnly",                   this, "testSetNotAckOnly"));
  tests.push_back(TestSet("setValidationAcknowledge",        this, "testSetValidationAcknowledge"));
  tests.push_back(TestSet("setRequestValidationAcknowledge", this, "testSetRequestValidationAcknowledge"));
  tests.push_back(TestSet("setExecutionAcknowledge",         this, "testSetExecutionAcknowledge"));
  tests.push_back(TestSet("setRequestExecutionAcknowledge",  this, "testSetRequestExecutionAcknowledge"));
  tests.push_back(TestSet("setQueryAcknowledge",             this, "testSetQueryAcknowledge"));
  tests.push_back(TestSet("setRequestQueryAcknowledge",      this, "testSetRequestQueryAcknowledge"));
  tests.push_back(TestSet("setWarningsGenerated",            this, "testSetWarningsGenerated"));
  tests.push_back(TestSet("setRequestWarningsGenerated",     this, "testSetRequestWarningsGenerated"));
  tests.push_back(TestSet("setErrorsGenerated",              this, "testSetErrorsGenerated"));
  tests.push_back(TestSet("setRequestErrorsGenerated",       this, "testSetRequestErrorsGenerated"));
  tests.push_back(TestSet("setTimestampControlMode",         this, "testSetTimestampControlMode")); // setTimestampControl2Bit, setTimestampControl1Bit, setTimestampControl0Bit, setTimestampControlMode
  tests.push_back(TestSet("setTimestampControl2Bit",         this, "+testSetTimestampControlMode"));
  tests.push_back(TestSet("setTimestampControl1Bit",         this, "+testSetTimestampControlMode"));
  tests.push_back(TestSet("setTimestampControl0Bit",         this, "+testSetTimestampControlMode"));
  tests.push_back(TestSet("setPartialAction",                this, "testSetPartialAction"));
  tests.push_back(TestSet("setActionExecuted",               this, "testSetActionExecuted"));

  tests.push_back(TestSet("setMessageID",                    this, "testSetMessageID"));
  tests.push_back(TestSet("setControlleeID",                 this, "testSetControlleeID")); // setControlleeIDNumber, setControlleeUUID
  tests.push_back(TestSet("setControlleeIDNumber",           this, "+testSetControlleeID"));
  tests.push_back(TestSet("setControlleeUUID",               this, "+testSetControlleeID"));
  tests.push_back(TestSet("setControllerID",                 this, "testSetControllerID")); // setControllerIDNumber, setControllerUUID
  tests.push_back(TestSet("setControllerIDNumber",           this, "+testSetControllerID"));
  tests.push_back(TestSet("setControllerUUID",               this, "+testSetControllerID"));

  tests.push_back(TestSet("setCancelMode",                   this, "testSetCancelMode"));

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

void BasicCommandPacketTest::done () {
  // nothing to do
}

void BasicCommandPacketTest::call (const string &func) {
  if (func == "testSetPacketType"                       ) testSetPacketType();
  if (func == "testResetForResend"                      ) testResetForResend();
// these methods are protected and cannot be tested in this way
//  if (func == "testSetControlleeEnable"                 ) testSetControlleeEnable();
//  if (func == "testSetControlleeFormat"                 ) testSetControlleeFormat();
//  if (func == "testSetControllerEnable"                 ) testSetControllerEnable();
//  if (func == "testSetControllerFormat"                 ) testSetControllerFormat();
  if (func == "testSetPartialChangePermitted"           ) testSetPartialChangePermitted();
  if (func == "testSetWarningsPermitted"                ) testSetWarningsPermitted();
  if (func == "testSetErrorsPermitted"                  ) testSetErrorsPermitted();
  if (func == "testSetAction"                           ) testSetAction(); // setAction1Flag, setAction0Flag, setActionNoAction, setActionDryRun, setActionTakeAction
  if (func == "testSetNotAckOnly"                       ) testSetNotAckOnly();
  if (func == "testSetValidationAcknowledge"            ) testSetValidationAcknowledge();
  if (func == "testSetRequestValidationAcknowledge"     ) testSetRequestValidationAcknowledge();
  if (func == "testSetExecutionAcknowledge"             ) testSetExecutionAcknowledge();
  if (func == "testSetRequestExecutionAcknowledge"      ) testSetRequestExecutionAcknowledge();
  if (func == "testSetQueryAcknowledge"                 ) testSetQueryAcknowledge();
  if (func == "testSetRequestQueryAcknowledge"          ) testSetRequestQueryAcknowledge();
  if (func == "testSetWarningsGenerated"                ) testSetWarningsGenerated();
  if (func == "testSetRequestWarningsGenerated"         ) testSetRequestWarningsGenerated();
  if (func == "testSetErrorsGenerated"                  ) testSetErrorsGenerated();
  if (func == "testSetRequestErrorsGenerated"           ) testSetRequestErrorsGenerated();
  if (func == "testSetTimestampControlMode"             ) testSetTimestampControlMode(); // setTimestampControl2Bit, setTimestampControl1Bit, setTimestampControl0Bit, setTimestampControlMode
  if (func == "testSetPartialAction"                    ) testSetPartialAction();
  if (func == "testSetActionExecuted"                   ) testSetActionExecuted();

  if (func == "testSetMessageID"                        ) testSetMessageID();
  if (func == "testSetControlleeID"                     ) testSetControlleeID(); // setControlleeIDNumber, setControlleeUUID
  if (func == "testSetControllerID"                     ) testSetControllerID(); // setControllerIDNumber, setControllerUUID

  if (func == "testSetCancelMode"                       ) testSetCancelMode();
}

void BasicCommandPacketTest::testResetForResend () {
  BasicCommandPacket p;

}

void BasicCommandPacketTest::testSetPacketType () {
  BasicCommandPacket p;
  char* buffer;
  buffer = (char*)p.getPacketPointer();
  assertEquals("getPacketType()", 6, (buffer[0] >> 4)); // Should default to Command

  p.setPacketType(PacketType_Command); // Setting to Command also valid
  buffer = (char*)p.getPacketPointer();
  assertEquals("setPacketType(Command)", 6, (buffer[0] >> 4));

}


// this is a protected method and cannot be tested in this way
/*
void BasicCommandPacketTest::testSetControlleeEnable () {

  const int32_t bit = 31;

  BasicCommandPacket p;
  p.setControlleeEnable(false);
  assertEquals("isControlleeEnable() false", false, p.isControlleeEnable());
  assertEquals("setControlleeEnable(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setControlleeEnable(true);
  assertEquals("isControlleeEnable() true", true, p.isControlleeEnable());
  assertEquals("setControlleeEnable(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}
*/

// this is a protected method and cannot be tested in this way
/*
void BasicCommandPacketTest::testSetControlleeFormat () {

  const int32_t bit = 30;

  BasicCommandPacket p;
  p.setControlleeEnable(true);
  assertEquals("isControlleeEnable() true", true, p.isControlleeEnable());
  p.setControlleeFormat(false);
  assertEquals("isControlleeFormat() false", false, p.getControlleeFormat());
  assertEquals("setControlleeFormat(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setControlleeFormat(true);
  assertEquals("isControlleeFormat() true", true, p.getControlleeFormat());
  assertEquals("setControlleeFormat(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}
*/

// this is a protected method and cannot be tested in this way
/*
void BasicCommandPacketTest::testSetControllerEnable () {

  const int32_t bit = 29;

  BasicCommandPacket p;
  p.setControllerEnable(false);
  assertEquals("isControllerEnable() false", false, p.isControllerEnable());
  assertEquals("setControllerEnable(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setControllerEnable(true);
  assertEquals("isControllerEnable() true", true, p.isControllerEnable());
  assertEquals("setControllerEnable(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}
*/

// this is a protected method and cannot be tested in this way
/*
void BasicCommandPacketTest::testSetControllerFormat () {

  const int32_t bit = 28;

  BasicCommandPacket p;
  p.setControllerEnable(true);
  assertEquals("isControllerEnable() true", true, p.isControllerEnable());
  p.setControllerFormat(false);
  assertEquals("isControllerFormat() false", false, p.getControllerFormat());
  assertEquals("setControllerFormat(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setControllerFormat(true);
  assertEquals("isControllerFormat() true", true, p.getControllerFormat());
  assertEquals("setControllerFormat(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}
*/

void BasicCommandPacketTest::testSetPartialChangePermitted () {

  const int32_t bit = 27;

  BasicCommandPacket p;
  p.setPartialChangePermitted(false);
  assertEquals("isPartialChangePermitted() false", false, p.isPartialChangePermitted());
  assertEquals("setPartialChangePermitted(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setPartialChangePermitted(true);
  assertEquals("isPartialChangePermitted() true", true, p.isPartialChangePermitted());
  assertEquals("setPartialChangePermitted(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}

void BasicCommandPacketTest::testSetWarningsPermitted () {

  const int32_t bit = 26;

  BasicCommandPacket p;
  p.setWarningsPermitted(false);
  assertEquals("isWarningsPermitted() false", false, p.isWarningsPermitted());
  assertEquals("setWarningsPermitted(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setWarningsPermitted(true);
  assertEquals("isWarningsPermitted() true", true, p.isWarningsPermitted());
  assertEquals("setWarningsPermitted(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}

void BasicCommandPacketTest::testSetErrorsPermitted () {

  const int32_t bit = 25;

  BasicCommandPacket p;
  p.setErrorsPermitted(false);
  assertEquals("isErrorsPermitted() false", false, p.isErrorsPermitted());
  assertEquals("setErrorsPermitted(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setErrorsPermitted(true);
  assertEquals("isErrorsPermitted() true", true, p.isErrorsPermitted());
  assertEquals("setErrorsPermitted(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}

void BasicCommandPacketTest::testSetAction () { // setAction1Flag, setAction0Flag, setActionNoAction, setActionDryRun, setActionTakeAction

  const int32_t mask     = 0x01800000; // bits 24..23
  const int8_t  val1     = 0;
  const int32_t val1bits = 0x00000000; // no action
  const int8_t  val2     = 1;
  const int32_t val2bits = 0x00800000; // dry run
  const int8_t  val3     = 2;
  const int32_t val3bits = 0x01000000; // take action

  BasicCommandPacket p;
  p.setAction(val1);
  assertEquals("getAction() val1", val1, p.getAction());
  assertEquals("setAction(val1) bits", val1bits, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & mask);
  assertEquals("getAction1Flag() val1", false, p.getAction1Flag());
  assertEquals("getAction0Flag() val1", false, p.getAction0Flag());
  assertEquals("isActionNoAction() val1",   true,  p.isActionNoAction());
  assertEquals("isActionDryRun() val1",     false, p.isActionDryRun());
  assertEquals("isActionTakeAction() val1", false, p.isActionTakeAction());

  p.setAction(val2);
  assertEquals("getAction() val2", val2, p.getAction());
  assertEquals("setAction(val2) bits", val2bits, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & mask);
  assertEquals("getAction1Flag() val2", false, p.getAction1Flag());
  assertEquals("getAction0Flag() val2", true,  p.getAction0Flag());
  assertEquals("isActionNoAction() val2",   false, p.isActionNoAction());
  assertEquals("isActionDryRun() val2",     true,  p.isActionDryRun());
  assertEquals("isActionTakeAction() val2", false, p.isActionTakeAction());

  p.setAction(val3);
  assertEquals("getAction() val3", val3, p.getAction());
  assertEquals("setAction(val3) bits", val3bits, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & mask);
  assertEquals("getAction1Flag() val3", true,  p.getAction1Flag());
  assertEquals("getAction0Flag() val3", false, p.getAction0Flag());
  assertEquals("isActionNoAction() val3",   false, p.isActionNoAction());
  assertEquals("isActionDryRun() val3",     false, p.isActionDryRun());
  assertEquals("isActionTakeAction() val3", true,  p.isActionTakeAction());

  p.setActionNoAction();
  assertEquals("getAction() val1", val1, p.getAction());
  assertEquals("setActionNoAction() bits", val1bits, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & mask);
  assertEquals("getAction1Flag() val1", false, p.getAction1Flag());
  assertEquals("getAction0Flag() val1", false, p.getAction0Flag());
  assertEquals("isActionNoAction() val1",   true,  p.isActionNoAction());
  assertEquals("isActionDryRun() val1",     false, p.isActionDryRun());
  assertEquals("isActionTakeAction() val1", false, p.isActionTakeAction());

  p.setActionDryRun();
  assertEquals("getAction() val2", val2, p.getAction());
  assertEquals("setActionDryRun() bits", val2bits, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & mask);
  assertEquals("getAction1Flag() val2", false, p.getAction1Flag());
  assertEquals("getAction0Flag() val2", true,  p.getAction0Flag());
  assertEquals("isActionNoAction() val2",   false, p.isActionNoAction());
  assertEquals("isActionDryRun() val2",     true,  p.isActionDryRun());
  assertEquals("isActionTakeAction() val2", false, p.isActionTakeAction());

  p.setActionTakeAction();
  assertEquals("getAction() val3", val3, p.getAction());
  assertEquals("setActionTakeAction() bits", val3bits, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & mask);
  assertEquals("getAction1Flag() val3", true,  p.getAction1Flag());
  assertEquals("getAction0Flag() val3", false, p.getAction0Flag());
  assertEquals("isActionNoAction() val3",   false, p.isActionNoAction());
  assertEquals("isActionDryRun() val3",     false, p.isActionDryRun());
  assertEquals("isActionTakeAction() val3", true,  p.isActionTakeAction());

  p.setAction1Flag(0);
  p.setAction0Flag(1);
  assertEquals("getAction() 01", val2, p.getAction());
  assertEquals("setActionXFlag(01) bits", val2bits, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & mask);
  assertEquals("getAction1Flag() 0", false, p.getAction1Flag());
  assertEquals("getAction0Flag() 1", true,  p.getAction0Flag());
  assertEquals("isActionNoAction() 01",   false, p.isActionNoAction());
  assertEquals("isActionDryRun() 01",     true,  p.isActionDryRun());
  assertEquals("isActionTakeAction() 01", false, p.isActionTakeAction());
}

void BasicCommandPacketTest::testSetNotAckOnly () {

  const int32_t bit = 22;

  BasicCommandPacket p;
  p.setNotAckOnly(false);
  assertEquals("getNotAckOnly() false", false, p.getNotAckOnly());
  assertEquals("setNotAckOnly(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setNotAckOnly(true);
  assertEquals("getNotAckOnly() true", true, p.getNotAckOnly());
  assertEquals("setNotAckOnly(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}

void BasicCommandPacketTest::testSetValidationAcknowledge () {

  const int32_t Xbit = 19;
  const int32_t Vbit = 20;

  BasicCommandPacket p;
  p.setValidationAcknowledge();
  assertEquals("isValidationAcknowledge() default/true", true, p.isValidationAcknowledge());
  assertEquals("isExecutionAcknowledge() default/false", false, p.isExecutionAcknowledge());
  assertEquals("setValidationAcknowledge(default)", 0x1<<Vbit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<Vbit);
  assertEquals("setValidationAcknowledge(default) Xbit", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<Xbit);

  p.setValidationAcknowledge(false);
  assertEquals("isValidationAcknowledge() false", false, p.isValidationAcknowledge());
  assertEquals("isExecutionAcknowledge() true", true, p.isExecutionAcknowledge());
  assertEquals("setValidationAcknowledge(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<Vbit);
  assertEquals("setValidationAcknowledge(false) Xbit", 0x1<<Xbit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<Xbit);

  p.setValidationAcknowledge(true);
  assertEquals("isValidationAcknowledge() true", true, p.isValidationAcknowledge());
  assertEquals("isExecutionAcknowledge() false", false, p.isExecutionAcknowledge());
  assertEquals("setValidationAcknowledge(true)", 0x1<<Vbit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<Vbit);
  assertEquals("setValidationAcknowledge(true) Xbit", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<Xbit);
}

void BasicCommandPacketTest::testSetRequestValidationAcknowledge () {

  const int32_t bit = 20;

  BasicCommandPacket p;
  p.setRequestValidationAcknowledge(false);
  assertEquals("getRequestValidationAcknowledge() false", false, p.getRequestValidationAcknowledge());
  assertEquals("setRequestValidationAcknowledge(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setRequestValidationAcknowledge(true);
  assertEquals("getRequestValidationAcknowledge() true", true, p.getRequestValidationAcknowledge());
  assertEquals("setRequestValidationAcknowledge(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}

void BasicCommandPacketTest::testSetExecutionAcknowledge () {

  const int32_t Xbit = 19;
  const int32_t Vbit = 20;

  BasicCommandPacket p;
  p.setExecutionAcknowledge();
  assertEquals("isExecutionAcknowledge() default/true", true, p.isExecutionAcknowledge());
  assertEquals("isValidationAcknowledge() default/false", false, p.isValidationAcknowledge());
  assertEquals("setExecutionAcknowledge(default)", 0x1<<Xbit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<Xbit);
  assertEquals("setExecutionAcknowledge(default) Vbit", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<Vbit);

  p.setExecutionAcknowledge(false);
  assertEquals("isExecutionAcknowledge() false", false, p.isExecutionAcknowledge());
  assertEquals("isValidationAcknowledge() true", true, p.isValidationAcknowledge());
  assertEquals("setExecutionAcknowledge(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<Xbit);
  assertEquals("setExecutionAcknowledge(false) Vbit", 0x1<<Vbit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<Vbit);

  p.setExecutionAcknowledge(true);
  assertEquals("isExecutionAcknowledge() true", true, p.isExecutionAcknowledge());
  assertEquals("isValidationAcknowledge() false", false, p.isValidationAcknowledge());
  assertEquals("setExecutionAcknowledge(true)", 0x1<<Xbit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<Xbit);
  assertEquals("setExecutionAcknowledge(true) Vbit", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<Vbit);
}

void BasicCommandPacketTest::testSetRequestExecutionAcknowledge () {

  const int32_t bit = 19;

  BasicCommandPacket p;
  p.setRequestExecutionAcknowledge(false);
  assertEquals("getRequestExecutionAcknowledge() false", false, p.getRequestExecutionAcknowledge());
  assertEquals("setRequestExecutionAcknowledge(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setRequestExecutionAcknowledge(true);
  assertEquals("getRequestExecutionAcknowledge() true", true, p.getRequestExecutionAcknowledge());
  assertEquals("setRequestExecutionAcknowledge(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}

void BasicCommandPacketTest::testSetQueryAcknowledge () {

  const int32_t bit = 18;

  BasicCommandPacket p;
  p.setQueryAcknowledge(false);
  assertEquals("isQueryAcknowledge() false", false, p.isQueryAcknowledge());
  assertEquals("setQueryAcknowledge(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setQueryAcknowledge(true);
  assertEquals("isQueryAcknowledge() true", true, p.isQueryAcknowledge());
  assertEquals("setQueryAcknowledge(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}

void BasicCommandPacketTest::testSetRequestQueryAcknowledge () {

  const int32_t bit = 18;

  BasicCommandPacket p;
  p.setRequestQueryAcknowledge(false);
  assertEquals("getRequestQueryAcknowledge() false", false, p.getRequestQueryAcknowledge());
  assertEquals("setRequestQueryAcknowledge(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setRequestQueryAcknowledge(true);
  assertEquals("getRequestQueryAcknowledge() true", true, p.getRequestQueryAcknowledge());
  assertEquals("setRequestQueryAcknowledge(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}

void BasicCommandPacketTest::testSetWarningsGenerated () {

  const int32_t bit = 17;

  BasicCommandPacket p;
  p.setWarningsGenerated(false);
  assertEquals("getWarningsGenerated() false", false, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setWarningsGenerated(true);
  assertEquals("getWarningsGenerated() true", true, p.getWarningsGenerated());
  assertEquals("setWarningsGenerated(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}

void BasicCommandPacketTest::testSetRequestWarningsGenerated () {

  const int32_t bit = 17;

  BasicCommandPacket p;
  p.setRequestWarningsGenerated(false);
  assertEquals("getRequestWarningsGenerated() false", false, p.getRequestWarningsGenerated());
  assertEquals("setRequestWarningsGenerated(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setRequestWarningsGenerated(true);
  assertEquals("getRequestWarningsGenerated() true", true, p.getRequestWarningsGenerated());
  assertEquals("setRequestWarningsGenerated(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}

void BasicCommandPacketTest::testSetErrorsGenerated () {

  const int32_t bit = 16;

  BasicCommandPacket p;
  p.setErrorsGenerated(false);
  assertEquals("getErrorsGenerated() false", false, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setErrorsGenerated(true);
  assertEquals("getErrorsGenerated() true", true, p.getErrorsGenerated());
  assertEquals("setErrorsGenerated(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}

void BasicCommandPacketTest::testSetRequestErrorsGenerated () {

  const int32_t bit = 16;

  BasicCommandPacket p;
  p.setRequestErrorsGenerated(false);
  assertEquals("getRequestErrorsGenerated() false", false, p.getRequestErrorsGenerated());
  assertEquals("setRequestErrorsGenerated(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setRequestErrorsGenerated(true);
  assertEquals("getRequestErrorsGenerated() true", true, p.getRequestErrorsGenerated());
  assertEquals("setRequestErrorsGenerated(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}

void BasicCommandPacketTest::testSetTimestampControlMode () { // setTimestampControl2Bit, setTimestampControl1Bit, setTimestampControl0Bit, setTimestampControlMode

  const int32_t mask     = 0x00007000; // bits 14..12
  const int8_t  val1     = 0;
  const int32_t val1bits = 0x00000000; // ignore timestamp
  const int8_t  val2     = 1;
  const int32_t val2bits = 0x00001000; // only specified
  const int8_t  val3     = 2;
  const int32_t val3bits = 0x00002000; // late and specified
  const int8_t  val4     = 3;
  const int32_t val4bits = 0x00003000; // early and specified
  const int8_t  val5     = 4;
  const int32_t val5bits = 0x00004000; // permitted early/late

  BasicCommandPacket p;
  p.setTimestampControlMode(val1);
  assertEquals("getTimestampControlMode() val1", val1, p.getTimestampControlMode());
  assertEquals("setTimestampControlMode(val1) bits", val1bits, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & mask);
  assertEquals("getTimestampControl2Bit() val1", false, p.getTimestampControl2Bit());
  assertEquals("getTimestampControl1Bit() val1", false, p.getTimestampControl1Bit());
  assertEquals("getTimestampControl0Bit() val1", false, p.getTimestampControl0Bit());

  p.setTimestampControlMode(val2);
  assertEquals("getTimestampControlMode() val2", val2, p.getTimestampControlMode());
  assertEquals("setTimestampControlMode(val2) bits", val2bits, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & mask);
  assertEquals("getTimestampControl2Bit() val2", false, p.getTimestampControl2Bit());
  assertEquals("getTimestampControl1Bit() val2", false, p.getTimestampControl1Bit());
  assertEquals("getTimestampControl0Bit() val2", true,  p.getTimestampControl0Bit());

  p.setTimestampControlMode(val3);
  assertEquals("getTimestampControlMode() val3", val3, p.getTimestampControlMode());
  assertEquals("setTimestampControlMode(val3) bits", val3bits, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & mask);
  assertEquals("getTimestampControl2Bit() val3", false, p.getTimestampControl2Bit());
  assertEquals("getTimestampControl1Bit() val3", true,  p.getTimestampControl1Bit());
  assertEquals("getTimestampControl0Bit() val3", false, p.getTimestampControl0Bit());

  p.setTimestampControlMode(val4);
  assertEquals("getTimestampControlMode() val4", val4, p.getTimestampControlMode());
  assertEquals("setTimestampControlMode(val4) bits", val4bits, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & mask);
  assertEquals("getTimestampControl2Bit() val4", false, p.getTimestampControl2Bit());
  assertEquals("getTimestampControl1Bit() val4", true,  p.getTimestampControl1Bit());
  assertEquals("getTimestampControl0Bit() val4", true,  p.getTimestampControl0Bit());

  p.setTimestampControlMode(val5);
  assertEquals("getTimestampControlMode() val5", val5, p.getTimestampControlMode());
  assertEquals("setTimestampControlMode(val5) bits", val5bits, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & mask);
  assertEquals("getTimestampControl2Bit() val5", true,  p.getTimestampControl2Bit());
  assertEquals("getTimestampControl1Bit() val5", false, p.getTimestampControl1Bit());
  assertEquals("getTimestampControl0Bit() val5", false, p.getTimestampControl0Bit());

  p.setTimestampControl2Bit(0);
  p.setTimestampControl1Bit(1);
  p.setTimestampControl0Bit(0);
  assertEquals("getTimestampControlMode() 010", val3, p.getTimestampControlMode());
  assertEquals("setTimestampControlXBit(010) bits", val3bits, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & mask);
  assertEquals("getTimestampControl2Bit() 0", false, p.getTimestampControl2Bit());
  assertEquals("getTimestampControl1Bit() 1", true, p.getTimestampControl1Bit());
  assertEquals("getTimestampControl0Bit() 0", false, p.getTimestampControl0Bit());

}

void BasicCommandPacketTest::testSetPartialAction () {

  const int32_t bit = 11;

  BasicCommandPacket p;
  p.setPartialAction(false);
  assertEquals("getPartialAction() false", false, p.getPartialAction());
  assertEquals("setPartialAction(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setPartialAction(true);
  assertEquals("getPartialAction() true", true, p.getPartialAction());
  assertEquals("setPartialAction(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}

void BasicCommandPacketTest::testSetActionExecuted () {

  const int32_t bit = 10;

  BasicCommandPacket p;
  p.setActionExecuted(false);
  assertEquals("getActionExecuted() false", false, p.getActionExecuted());
  assertEquals("setActionExecuted(false)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);

  p.setActionExecuted(true);
  assertEquals("getActionExecuted() true", true, p.getActionExecuted());
  assertEquals("setActionExecuted(true)", 0x1<<bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<bit);
}


void BasicCommandPacketTest::testSetMessageID () {

  const int32_t val1 = 0x12345678;
  const int32_t val2 = 0x87654321;

  BasicCommandPacket p;
  p.setMessageID(val1);
  assertEquals("getMessageID() val1", val1, p.getMessageID());
  assertEquals("setMessageID(val1)", val1, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()+4));

  p.setMessageID(val2);
  assertEquals("getMessageID() val2", val2, p.getMessageID());
  assertEquals("setMessageID(val2)", val2, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()+4));

}

void BasicCommandPacketTest::testSetControlleeID () { // setControlleeIDNumber, setControlleeUUID

  const int32_t CE_bit = 31;
  const int32_t IE_bit = 30;

  const int32_t val1 = 0x12345678;
  const int32_t val2 = 0x87654321;

  const uuid_t u3 = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
  const UUID val3 = UUID(u3);
  const uuid_t u4 = {'f','e','d','c','b','a','9','8','7','6','5','4','3','2','1','0'};
  const UUID val4 = UUID(u4);

  BasicCommandPacket p;
  int32_t len0 = p.getPacketLength();
  //Utilities::dumpBytes(p.getPacketPointer(), len0);

  p.setControlleeIDNumber(val1);
  int32_t len1 = p.getPacketLength();
  //Utilities::dumpBytes(p.getPacketPointer(), len1);
  assertEquals("setControlleeIDNumber(val1) pkt len", len0-12, len1);
  assertEquals("setControlleeIDNumber(val1) enable", 0x01<<CE_bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<CE_bit);
  assertEquals("setControlleeIDNumber(val1) format", 0x00<<IE_bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<IE_bit);
  assertEquals("isControlleeEnable() true", true, p.isControlleeEnable());
  assertEquals("getControlleeFormat() false", false, p.getControlleeFormat());
  assertEquals("getControlleeIDNumber() val1", val1, p.getControlleeIDNumber());
  assertEquals("setControlleeIDNumber(val1)", val1, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()+8));

  p.setControlleeIDNumber(val2);
  //Utilities::dumpBytes(p.getPacketPointer(), p.getPacketLength());
  assertEquals("getControlleeIDNumber() val2", val2, p.getControlleeIDNumber());
  assertEquals("setControlleeIDNumber(val2)", val2, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()+8));

  p.setControlleeUUID(val3);
  int32_t len2 = p.getPacketLength();
  //Utilities::dumpBytes(p.getPacketPointer(), len2);
  assertEquals("setControlleeUUID(val3) pkt len", len1+12, len2);
  assertEquals("setControlleeUUID(val3) enable", 0x01<<CE_bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<CE_bit);
  assertEquals("setControlleeUUID(val3) format", 0x01<<IE_bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<IE_bit);
  assertEquals("isControlleeEnable() true", true, p.isControlleeEnable());
  assertEquals("getControlleeFormat() true", true, p.getControlleeFormat());
  assertEquals("getControlleeUUID() val3", val3, p.getControlleeUUID());

  p.setControlleeUUID(val4);
  //Utilities::dumpBytes(p.getPacketPointer(), p.getPacketLength());
  assertEquals("getControlleeUUID() val4", val4, p.getControlleeUUID());

  // remove controllee ID
  p.setControlleeUUID(UUID());
  int32_t len3 = p.getPacketLength();
  //Utilities::dumpBytes(p.getPacketPointer(), len3);
  assertEquals("setControlleeIDNumber(val1) pkt len", len2-16, len3);
  assertEquals("setControlleeUUID(null) enable", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<CE_bit);
  assertEquals("isControlleeEnable() false", false, p.isControlleeEnable());
  assertEquals("getControlleeUUID() null", UUID(), p.getControlleeUUID());
}

void BasicCommandPacketTest::testSetControllerID () { // setControllerIDNumber, setControllerUUID

  const int32_t CR_bit = 29;
  const int32_t IR_bit = 28;

  const int32_t val1 = 0x12345678;
  const int32_t val2 = 0x87654321;

  const uuid_t u3 = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
  const UUID val3 = UUID(u3);
  const uuid_t u4 = {'f','e','d','c','b','a','9','8','7','6','5','4','3','2','1','0'};
  const UUID val4 = UUID(u4);

  BasicCommandPacket p;
  int32_t len0 = p.getPacketLength();
  //Utilities::dumpBytes(p.getPacketPointer(), len0);

  p.setControllerIDNumber(val1);
  int32_t len1 = p.getPacketLength();
  //Utilities::dumpBytes(p.getPacketPointer(), len1);
  assertEquals("setControllerIDNumber(val1) pkt len", len0-12, len1);
  assertEquals("setControllerIDNumber(val1) enable", 0x01<<CR_bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<CR_bit);
  assertEquals("setControllerIDNumber(val1) format", 0x00<<IR_bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<IR_bit);
  assertEquals("isControllerEnable() true", true, p.isControllerEnable());
  assertEquals("getControllerFormat() false", false, p.getControllerFormat());
  assertEquals("getControllerIDNumber() val1", val1, p.getControllerIDNumber());
  assertEquals("setControllerIDNumber(val1)", val1, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()+24));

  p.setControllerIDNumber(val2);
  //Utilities::dumpBytes(p.getPacketPointer(), p.getPacketLength());
  assertEquals("getControllerIDNumber() val2", val2, p.getControllerIDNumber());
  assertEquals("setControllerIDNumber(val2)", val2, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()+24));

  p.setControllerUUID(val3);
  int32_t len2 = p.getPacketLength();
  //Utilities::dumpBytes(p.getPacketPointer(), len2);
  assertEquals("setControllerIDNumber(val1) pkt len", len1+12, len2);
  assertEquals("setControllerUUID(val3) enable", 0x01<<CR_bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<CR_bit);
  assertEquals("setControllerUUID(val3) format", 0x01<<IR_bit, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<IR_bit);
  assertEquals("isControllerEnable() true", true, p.isControllerEnable());
  assertEquals("getControllerFormat() true", true, p.getControllerFormat());
  assertEquals("getControllerUUID() val3", val3, p.getControllerUUID());

  p.setControllerUUID(val4);
  //Utilities::dumpBytes(p.getPacketPointer(), p.getPacketLength());
  assertEquals("getControllerUUID() val4", val4, p.getControllerUUID());

  // remove controller ID
  p.setControllerUUID(UUID());
  int32_t len3 = p.getPacketLength();
  //Utilities::dumpBytes(p.getPacketPointer(), len3);
  assertEquals("setControllerIDNumber(val1) pkt len", len2-16, len3);
  assertEquals("setControllerUUID(null) enable", 0x0, VRTMath::unpackInt(p.getPacketPointer(), p.getHeaderLength()) & 0x1<<CR_bit);
  assertEquals("isControllerEnable() false", false, p.isControllerEnable());
  assertEquals("getControllerUUID() null", UUID(), p.getControllerUUID());
}

void BasicCommandPacketTest::testSetCancelMode () {

  const int32_t bit = 24;

  BasicCommandPacket p;
  int32_t len0 = p.getPacketLength();
  //Utilities::dumpBytes(p.getPacketPointer(), len0);

  p.setCancelMode(true);
  int32_t len1 = p.getPacketLength();
  //Utilities::dumpBytes(p.getPacketPointer(), len1);
  assertEquals("setCancelMode(true) pkt len", len0, len1);
  assertEquals("isCancelMode() true", _TRUE, p.isCancelMode());
  assertEquals("setCancelMode(true) bits", 0x01<<bit, VRTMath::unpackInt(p.getPacketPointer(), 0) & 0x1<<bit);

  p.setCancelMode(false);
  int32_t len2 = p.getPacketLength();
  //Utilities::dumpBytes(p.getPacketPointer(), len2);
  assertEquals("setCancelMode(false) pkt len", len1, len2);
  assertEquals("isCancelMode() false", _FALSE, p.isCancelMode());
  assertEquals("setCancelMode(false) bits", 0x00, VRTMath::unpackInt(p.getPacketPointer(), 0) & 0x1<<bit);
}

//////////////////////////////////////////////////////////////////////////////
// INTERNAL METHODS
//////////////////////////////////////////////////////////////////////////////

