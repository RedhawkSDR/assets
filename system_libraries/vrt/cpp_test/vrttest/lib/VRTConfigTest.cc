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

#include "VRTConfigTest.h"
#include "TestRunner.h"
#include "VRTConfig.h"
#include <errno.h>

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;


string VRTConfigTest::getTestedClass () {
  return "vrt::VRTConfig";
}

vector<TestSet> VRTConfigTest::init () {
  vector<TestSet> tests;
  tests.push_back(TestSet("getLeapSecondsFile()",      this,  "testConstants"));
  tests.push_back(TestSet("getNoradLeapSecCounted()",  this, "+testConstants"));
  tests.push_back(TestSet("getLibraryVersion()",       this, "+testConstants"));
  tests.push_back(TestSet("getTestDevice()",           this, "+testConstants"));
  tests.push_back(TestSet("getTestFirstMCast()",       this, "+testConstants"));
  tests.push_back(TestSet("getTestFirstPort()",        this, "+testConstants"));
  tests.push_back(TestSet("getTestQuick()",            this, "+testConstants"));
  tests.push_back(TestSet("getTestServer()",           this, "+testConstants"));
  tests.push_back(TestSet("getTestServerTimeout()",    this, "+testConstants"));
  tests.push_back(TestSet("getStrict()",               this, "+testConstants"));
  tests.push_back(TestSet("getVRTVersion()",           this, "+testConstants"));
  return tests;
}

void VRTConfigTest::done () {
  // nothing to do
}

void VRTConfigTest::call (const string &func) {
  if (func == "testConstants") testConstants();
}

void VRTConfigTest::testConstants () {
  // ==== DSLT_SERVICE & DSLT_DEVICE =========================================
  // Not currently in C++

  // ==== LEAP_SECONDS_FILE ==================================================
  assertEquals("LEAP_SECONDS_FILE may be empty", true, (VRTConfig::getLeapSecondsFile() != "")
                                                    || (VRTConfig::getLeapSecondsFile() == ""));

  // ==== LIBRARY_VERSION ====================================================
  size_t dot1 = VRTConfig::getLibraryVersion().find('.');
  size_t dot2 = VRTConfig::getLibraryVersion().find('.', dot1+1);
  size_t dot3 = VRTConfig::getLibraryVersion().find('.', dot2+1);

  assertEquals("LIBRARY_VERSION is in x.x.x form (dot1 > 0)",      true, (dot1 > 0));
  assertEquals("LIBRARY_VERSION is in x.x.x form (dot2 > dot1+1)", true, (dot2 > dot1+1));
  assertEquals("LIBRARY_VERSION is in x.x.x form (dot1 != npos)",  true, (dot1 != string::npos));
  assertEquals("LIBRARY_VERSION is in x.x.x form (dot2 != npos)",  true, (dot2 != string::npos));
  assertEquals("LIBRARY_VERSION is in x.x.x form (dot3 == npos)",  true, (dot3 == string::npos));

  int num1 = atoi(VRTConfig::getLibraryVersion().substr(0,dot1).c_str());
  int num2 = atoi(VRTConfig::getLibraryVersion().substr(dot1+1,dot2-dot1).c_str());
  int num3 = atoi(VRTConfig::getLibraryVersion().substr(dot2+1).c_str());

  assertEquals("LIBRARY_VERSION is in x.x.x form (num1 >= 0)",     true, (num1 >= 0));
  assertEquals("LIBRARY_VERSION is in x.x.x form (num2 >= 0)",     true, (num2 >= 0));
  assertEquals("LIBRARY_VERSION is in x.x.x form (num3 >= 0)",     true, (num3 >= 0));

  // ==== NORAD_LEAP_SEC_COUNTED =============================================
  assertEquals("NORAD_LEAP_SEC_COUNTED is true/false", true, (VRTConfig::getNoradLeapSecCounted() == true)
                                                          || (VRTConfig::getNoradLeapSecCounted() == false));

  // ==== TEST_DELAY =========================================================
  assertEquals("TEST_DELAY is non-negative", true, (VRTConfig::getTestDelay() >= 0));

  // ==== TEST_DEVICE =========================================================
  assertEquals("TEST_DEVICE is not null", true, (VRTConfig::getTestDevice() != "")
                                             || (VRTConfig::getTestDevice() == ""));

  // ==== TEST_FIRST_MCAST ====================================================
  assertEquals("TEST_FIRST_MCAST is not null", true, (VRTConfig::getTestFirstMCast() != "")
                                                  || (VRTConfig::getTestFirstMCast() == ""));

  // ==== TEST_FIRST_PORT ====================================================
  assertEquals("TEST_FIRST_PORT is 0..N", true, (VRTConfig::getTestFirstPort() >= 0)
                                             || (VRTConfig::getTestFirstPort() <= 0xFFFF));

  // ==== TEST_QUICK =========================================================
  assertEquals("TEST_QUICK is true/false", true, (VRTConfig::getTestQuick() == true)
                                              || (VRTConfig::getTestQuick() == false));

  // ==== TEST_SERVER ==========================================================
  assertEquals("TEST_SERVER is not null", true, (VRTConfig::getTestServer() != "")
                                             || (VRTConfig::getTestServer() == ""));

  // ==== TEST_SERVER_TIMEOUT ================================================
  assertEquals("TEST_SERVER_TIMEOUT is 0..86400", true, (VRTConfig::getTestServerTimeout() >= 0)
                                                     || (VRTConfig::getTestServerTimeout() <= 86400));

  // ==== STRICT =============================================================
  assertEquals("STRICT is true/false", true, (VRTConfig::getStrict() == true)
                                          || (VRTConfig::getStrict() == false));

  // ==== VRT_VERSION ========================================================
  assertEquals("VRT_VERSION is V49/V49b", true, (VRTConfig::getVRTVersion() == VRTConfig::VITAVersion_V49)
                                             || (VRTConfig::getVRTVersion() == VRTConfig::VITAVersion_V49b));
}
