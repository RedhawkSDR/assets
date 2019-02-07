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

#include "Main.h"
#include "TestRunner.h"

#include "AbstractPacketFactoryTest.h"
#include "AbstractVRAFileTest.h"
#include "BasicAcknowledgePacketTest.h"
#include "BasicCommandPacketTest.h"
#include "BasicContextPacketTest.h"
#include "BasicControlPacketTest.h"
#include "BasicDataPacketTest.h"
#include "BasicQueryAcknowledgePacketTest.h"
#include "BasicVRLFrameTest.h"
#include "BasicVRTPacketTest.h"
#include "EphemerisTest.h"
#include "HasFieldsTest.h"
#include "InetAddressTest.h"
#include "LeapSecondsTest.h"
#include "NetTestClient.h"
#include "NetUtilitiesTest.h"
#include "PackUnpackTest.h"
#include "TimeStampTest.h"
#include "UUIDTest.h"
#include "UtilitiesTest.h"
#include "VRTConfig.h"
#include "VRTConfigTest.h"
#include "VRTEventListenerTest.h"
#include "VRTEventTest.h"
#include "VRTMathTest.h"
#include "VRTObjectTest.h"
#include "ValueTest.h"

using namespace std;
using namespace vrt;
using namespace vrttest;

// Usage text, last line must be "EOF"
static const char* USAGE[] = {
    "This is the core of the VRT Java Test Suite Library and includes the",
    "following functions:",
    "",
    "UnitTest      - Runs the suite of unit tests.",
    "                  java -jar vrttest.jar UnitTest",
    "",
    "NetTestClient - Runs the client side of the networking tests.",
    "                  java -jar vrttest.jar NetTestClient [hostPort]",
    "",
    "                  hostPort - The host and port the server is running on.",
    "                             Note that the server must be started *prior*",
    "                             to running this test suite.",
    "",
    "NetTestServer - Runs the server side of the networking tests. Note that",
    "                this server side is used for both the Java and C++ tests.",
    "                  java -jar vrttest.jar NetTestServer [hostPort] [duration]",
    "",
    "                  hostPort - The (host and) port to run the server on.",
    "                             This will typically be in port-only form",
    "                             (e.g. ':8080'), but may optionally include",
    "                             a host name.",
    "                  duration - The duration (in seconds) for the server",
    "                             to remain active.",
    "",
    "The following environment variables also control the test operation, note",
    "that when running the networking tests that both client and server must",
    "have these set identically:",
    "  VRT_TEST_FIRST_MCAST     - Sets the UDP/Multicast address range to use.",
    "                             This will be an implied range of 8 addresses",
    "                             starting at the given address. Setting this",
    "                             to '' will disable any tests that require",
    "                             using UDP/Multicast (default is '').",
    "",
    "  VRT_TEST_FIRST_PORT      - Sets the port numbers to use as part of the",
    "                             unit/network tests. This will be an implied",
    "                             range of 10 ports starting at the given port",
    "                             port number. Setting this to 0 will disable",
    "                             any tests that require opening a port",
    "                             (default is 0).",
    "",
    "  VRT_TEST_QUICK           - Turns on/off 'quick test' mode which ",
    "                             abbreviates some of the test cases in order",
    "                             to speed up the testing process (default is",
    "                             on). Setting this to 'off' may cause the",
    "                             test suite to take 15+ minutes to execute.",
    "EOF"
};

int main (int argc, char* argv[]) {
  VRTConfig::libraryInit();
  vector<Testable*> testCases;
  try {
    string testType = (argc == 2)? argv[1] : "";
    string err      = "";

    if (argc != 2) {
      err = "Incorrect number of parameters";
    }

    if (!isNull(err)) {
      // ERROR CONDITION
    }
    else if (testType == "UnitTest") {
      // nxm.vrt.lib.*
      testCases.push_back(new AbstractPacketFactoryTest());
      testCases.push_back(new AbstractVRAFileTest("vrt::AbstractVRAFile"));
      testCases.push_back(new BasicAcknowledgePacketTest());
      testCases.push_back(new BasicCommandPacketTest());
      testCases.push_back(new BasicContextPacketTest());
      testCases.push_back(new BasicControlPacketTest());
      testCases.push_back(new BasicDataPacketTest());
      testCases.push_back(new BasicQueryAcknowledgePacketTest());
      testCases.push_back(new AbstractVRAFileTest("vrt::BasicVRAFile"));
      testCases.push_back(new BasicVRLFrameTest());
      testCases.push_back(new BasicVRTPacketTest());
      testCases.push_back(new ClassCastExceptionTest());
      testCases.push_back(new EphemerisTest());
      testCases.push_back(new HasFieldsTest());
      testCases.push_back(new InetAddressTest());
      testCases.push_back(new LeapSecondsTest());
      testCases.push_back(new PackUnpackTest());
      testCases.push_back(new PayloadFormatTest());
      testCases.push_back(new TimeStampTest());
      testCases.push_back(new UUIDTest());
      testCases.push_back(new UtilitiesTest());
      testCases.push_back(new VRTConfigTest());
      testCases.push_back(new VRTObjectTest());
      testCases.push_back(new VRTExceptionTest());
      testCases.push_back(new ValueTest());
      testCases.push_back(new VRTMathTest());

      // nxm.vrt.net.*
      testCases.push_back(new VRTEventListenerTest("vrt::VRTContextListener"));
      testCases.push_back(new NetUtilitiesTest());
      testCases.push_back(new VRTEventListenerTest("vrt::VRTEventListener"));
      testCases.push_back(new VRTEventTest());
    }
    else if (testType == "NetTestClient") {
      testCases.push_back(new NetTestClient());
    }
    else if (testType == "NetTestServer") {
      err = "Test type NetTestServer only supported in the Java version (which "
            "supports both Java and C++ clients).";
    }
    else {
      err = string("Invalid test type, expected one of UnitTest, NetTestClient, "
                   "or NetTestServer; but given '")+testType+"'.";
    }

    if (!isNull(err)) {
      cout << "ERROR: " << err << endl;
      cout << endl;
      cout << "Usage:" << endl;
      for (size_t i = 0; string(USAGE[i]) != string("EOF"); i++) {
        cout << USAGE[i] << endl;
      }
      return -1;
    }
    else if (testCases.size() == 0) {
      return 0;
    }
    else {
      bool pass = TestRunner::runTests(testCases, true);

      while (!testCases.empty()) {
        Testable *tc = testCases.back();
        testCases.pop_back();
        checked_dynamic_cast<VRTObject*>(tc)->_delete();
      }

      return (pass)? 0 : 1;
    }
  }
  catch (VRTException e) {
    while (!testCases.empty()) {
      Testable *tc = testCases.back();
      testCases.pop_back();
      checked_dynamic_cast<VRTObject*>(tc)->_delete();
    }
    e.printStackTrace();
    return -1;
  }
  catch (exception e) {
    while (!testCases.empty()) {
      Testable *tc = testCases.back();
      testCases.pop_back();
      checked_dynamic_cast<VRTObject*>(tc)->_delete();
    }
    cout << "ERROR: Caught exception: " << e.what() << endl;
    return -1;
  }
}
