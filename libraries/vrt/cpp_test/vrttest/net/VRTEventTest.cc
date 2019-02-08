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

#include "VRTEventTest.h"
#include "BasicContextPacket.h"
#include "BasicDataPacket.h"
#include "TestRunner.h"
#include "UUID.h"
#include "VRTEvent.h"
#include <errno.h>
#include <string>

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

string VRTEventTest::getTestedClass () {
  return "vrt::VRTEvent";
}

vector<TestSet> VRTEventTest::init () {
  vector<TestSet> tests;

  tests.push_back(TestSet("equals(..)",       this,  "testEquals"));
  tests.push_back(TestSet("getPacket()",      this,  "testGetPacket"));
  tests.push_back(TestSet("getSource()",      this, "+testGetPacket"));
  tests.push_back(TestSet("isNullValue()",    this, "+testGetPacket"));
  tests.push_back(TestSet("toString()",       this, "+testGetPacket"));

  return tests;
}

void VRTEventTest::done () {
  // nothing to do
}

void VRTEventTest::call (const string &func) {
  if (func == "testEquals"   ) testEquals();
  if (func == "testGetPacket") testGetPacket();
}

void VRTEventTest::testEquals () {
  BasicVRTPacket testPkt1;  testPkt1.setTimeStamp(TimeStamp::Y2K_GPS);
  BasicVRTPacket testPkt2;  testPkt2.setTimeStamp(TimeStamp::getSystemTime());
  VRTEvent       origEvt(this, testPkt1);
  VRTEvent       diffEvt(this, testPkt2);
  VRTEvent       copyEvt = origEvt;

  assertEquals("equals(..) for self", true,  origEvt.equals(origEvt));
  assertEquals("equals(..) for diff", false, origEvt.equals(diffEvt));
  assertEquals("equals(..) for copy", true,  origEvt.equals(copyEvt));
}

static void _testGetPacket (VRTObject *src, BasicVRTPacket *pkt) {
  VRTEvent       evt1;
  VRTEvent       evt2;
  string         expMsg1;
  string         expMsg2;
  BasicVRTPacket expPkt;

  if (src == NULL) {
    evt1    = VRTEvent();
    evt2    = VRTEvent(src);
    expMsg1 = "";
    expMsg2 = "<null>";
    expPkt  = BasicVRTPacket::NULL_PACKET;
  }
  else if (pkt == NULL) {
    evt1    = VRTEvent(*src);
    evt2    = VRTEvent( src);
    expMsg1 = src->toString();
    expMsg2 = src->toString();
    expPkt  = BasicVRTPacket::NULL_PACKET;
  }
  else {
    evt1    = VRTEvent(*src,*pkt);
    evt2    = VRTEvent( src,*pkt);
    expMsg1 = src->toString();
    expMsg2 = src->toString();
    expPkt  = *pkt;
  }

  assertEquals("evt1.getSource()",   NULL,            evt1.getSource());
  assertEquals("evt1.getPacket()",   expPkt,          evt1.getPacket());
  assertEquals("evt1.isNullValue()", isNull(expMsg1), evt1.isNullValue());
  assertEquals("evt1.toString()",    true,            (evt1.toString().find(expMsg1) != string::npos));

  assertEquals("evt2.getSource()",   src,             evt2.getSource());
  assertEquals("evt2.getPacket()",   expPkt,          evt2.getPacket());
  assertEquals("evt2.isNullValue()", isNull(expMsg2), evt2.isNullValue());
  assertEquals("evt2.toString()",    true,            (evt2.toString().find(expMsg2) != string::npos));
}

void VRTEventTest::testGetPacket () {
  // The Java version of this test relies on the core functionality being tested
  // by the tests for the superclass (VRTEvent in Java extends EventObject) and
  // is a much more simplified test as a result.
  UUID               src1;  src1.setUUID("5f10526d-8483-491f-83c1-e0669db0f2e4");
  UUID               src2;  src2.setUUID("964e1899-b5d9-4eb4-9881-27edb3a8bdf9");
  UUID               src3;  src3.setUUID("fcfa3dfc-5914-4c05-b676-8410f42b19a7");
  BasicVRTPacket     pkt1;
  BasicContextPacket pkt2;  pkt2.setTimeStamp(TimeStamp::Y2K_GPS);
  BasicDataPacket    pkt3;  pkt3.setTimeStamp(TimeStamp::getSystemTime());

  _testGetPacket(NULL,  NULL );
  _testGetPacket(&src1, NULL );
  _testGetPacket(&src1, &pkt1);

  _testGetPacket(NULL,  NULL );
  _testGetPacket(&src2, NULL );
  _testGetPacket(&src2, &pkt2);

  _testGetPacket(NULL,  NULL );
  _testGetPacket(&src3, NULL );
  _testGetPacket(&src3, &pkt3);
}
