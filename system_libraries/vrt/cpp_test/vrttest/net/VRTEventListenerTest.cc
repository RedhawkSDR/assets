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

#include "VRTEventListenerTest.h"
#include "BasicContextPacket.h"
#include "BasicDataPacket.h"
#include "TestListener.h"
#include "TestRunner.h"
#include "VRTEvent.h"
#include <errno.h>
#include <string>

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;


VRTEventListenerTest::VRTEventListenerTest (string testedClass) :
  testedClass(testedClass)
{
  // done
}

string VRTEventListenerTest::getTestedClass () {
  return testedClass;
}

vector<TestSet> VRTEventListenerTest::init () {
  vector<TestSet> tests;

  if (true) {
    tests.push_back(TestSet("receivedPacket(..)",         this,  "testReceivedPacket"));
    tests.push_back(TestSet("sentPacket(..)",             this, "+testReceivedPacket"));
    tests.push_back(TestSet("errorOccurred(..)",          this, "+testReceivedPacket"));
    tests.push_back(TestSet("warningOccurred(..)",        this, "+testReceivedPacket"));
  }
  if (getTestedClass() == "vrt::VRTContextListener") {
    tests.push_back(TestSet("receivedDataPacket(..)",     this, "+testReceivedPacket"));
    tests.push_back(TestSet("receivedContextPacket(..)",  this, "+testReceivedPacket"));
    tests.push_back(TestSet("receivedInitialContext(..)", this, "+testReceivedPacket"));
  }

  return tests;
}

void VRTEventListenerTest::done () {
  // nothing to do
}

void VRTEventListenerTest::call (const string &func) {
  if (func == "testReceivedPacket") testReceivedPacket();
}

void VRTEventListenerTest::testReceivedPacket () {
  TestListener                listener;
  VRTEvent                    event(this);
  BasicDataPacket             data;
  BasicContextPacket          ctx;
  string                      message = "Hello World!";
  VRTException                error("Test Exception");
  map<int32_t,BasicVRTPacket> context;

  data.setTimeStamp(TimeStamp::getSystemTime());
  ctx.setTimeStamp(TimeStamp::Y2K_GPS);

  listener.receivedPacket(event, &data);
  assertEquals("receivedPacket(..)", Mode_receivedPacket, listener.mode);
  assertEquals("receivedPacket(..)", event,               listener.event);
  assertEquals("receivedPacket(..)", &data,               &listener.packet);

  listener.sentPacket(event, &data);
  assertEquals("sentPacket(..)", Mode_sentPacket, listener.mode);
  assertEquals("sentPacket(..)", event,           listener.event);
  assertEquals("sentPacket(..)", &data,           &listener.packet);

  listener.errorOccurred(event, message, error);
  assertEquals("errorOccurred(..)", Mode_errorOccurred, listener.mode);
  assertEquals("errorOccurred(..)", event,              listener.event);
  assertEquals("errorOccurred(..)", message,            listener.message);
  assertEquals("errorOccurred(..)", error,              listener.error);

  listener.warningOccurred(event, message, error);
  assertEquals("warningOccurred(..)", Mode_warningOccurred,   listener.mode);
  assertEquals("warningOccurred(..)", event,                  listener.event);
  assertEquals("warningOccurred(..)", message,                listener.message);
  assertEquals("warningOccurred(..)", error,                  listener.error);

  if (getTestedClass() == "vrt::VRTContextListener") {
    listener.receivedDataPacket(event, &data);
    assertEquals("receivedDataPacket(..)", Mode_receivedDataPacket, listener.mode);
    assertEquals("receivedDataPacket(..)", event,                   listener.event);
    assertEquals("receivedDataPacket(..)", &data,                   &listener.packet);

    listener.receivedContextPacket(event, &ctx);
    assertEquals("receivedContextPacket(..)", Mode_receivedContextPacket, listener.mode);
    assertEquals("receivedContextPacket(..)", event,                      listener.event);
    assertEquals("receivedContextPacket(..)", &ctx,                       &listener.packet);

    listener.receivedInitialContext(event, message, data, ctx, context);
    assertEquals("receivedInitialContext(..)", Mode_receivedInitialContext, listener.mode);
    assertEquals("receivedInitialContext(..)", event,                       listener.event);
    assertEquals("receivedInitialContext(..)", message,                     listener.message);
    assertEquals("receivedInitialContext(..)", data,                        listener.data);
    assertEquals("receivedInitialContext(..)", &ctx,                        &listener.ctx);
    assertEquals("receivedInitialContext(..)", context.size(),              listener.context.size());
  }

}
