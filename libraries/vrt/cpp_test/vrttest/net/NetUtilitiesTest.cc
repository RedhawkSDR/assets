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

#include "NetUtilitiesTest.h"
#include "TestRunner.h"
#include "UUID.h"
#include <errno.h>

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

string NetUtilitiesTest::getTestedClass () {
  return "vrt::NetUtilities";
}

vector<TestSet> NetUtilitiesTest::init () {
  vector<TestSet> tests;

  tests.push_back(TestSet("MAX_IPv4_UDP_LEN",         this,    "testConstants"));
  tests.push_back(TestSet("MAX_UDP_LEN",              this,   "+testConstants"));
  tests.push_back(TestSet("UDP_HEADER_LENGTH",        this,   "+testConstants"));

  tests.push_back(TestSet("getHost(..)",              this,    "testGetHost"));
  tests.push_back(TestSet("getHostAddress(..)",       this,    "testGetHostAddress"));
  tests.push_back(TestSet("getNetworkDeviceName(..)", this,    "testGetNetworkDeviceName"));
  tests.push_back(TestSet("getPort(..)",              this,   "+testGetHost"));

  return tests;
}

void NetUtilitiesTest::done () {
  // nothing to do
}

void NetUtilitiesTest::call (const string &func) {
  if (func == "testConstants"           ) testConstants();
  if (func == "testGetHost"             ) testGetHost();
  if (func == "testGetHostAddress"      ) testGetHostAddress();
  if (func == "testGetNetworkDeviceName") testGetNetworkDeviceName();
}

void NetUtilitiesTest::testConstants () {
  assertEquals("MAX_IPv4_UDP_LEN",  65475, NetUtilities::MAX_IPv4_UDP_LEN);
  assertEquals("MAX_UDP_LEN",       65535, NetUtilities::MAX_UDP_LEN);
  assertEquals("UDP_HEADER_LENGTH",     8, NetUtilities::UDP_HEADER_LENGTH);
}

void NetUtilitiesTest:: testGetNetworkDeviceName () {
  assertEquals("getNetworkDeviceName('eth1',-1)", "eth1",
   NetUtilities::getNetworkDeviceName("eth1",-1));

  assertEquals("getNetworkDeviceName('eth0',12)", "eth0.12",
   NetUtilities::getNetworkDeviceName("eth0",12));

  assertEquals("getNetworkDeviceName('',-1)", "",
   NetUtilities::getNetworkDeviceName("",-1));

  try {
    NetUtilities::getNetworkDeviceName("",12);
    assertEquals("getNetworkDeviceName('',12) -> error", true, false);
  }
  catch (VRTException e) {
    UNUSED_VARIABLE(e);
    assertEquals("getNetworkDeviceName('',12) -> error", true, true);
  }
}

void NetUtilitiesTest::testGetHostAddress () {
  // The tests here are limited to localhost since that is the only one
  // we are guaranteed to be able to get.
  InetAddress addr1 = NetUtilities::getHostAddress("localhost");
  InetAddress addr2 = NetUtilities::getHostAddress("localhost:9999");

  InetAddress addr3 = NetUtilities::getHostAddress("127.0.0.1");
  InetAddress addr4 = NetUtilities::getHostAddress("127.0.0.1:9999");

  InetAddress addr5 = NetUtilities::getHostAddress("[::1]");
  InetAddress addr6 = NetUtilities::getHostAddress("[::1]:9999");

  InetAddress addr7 = NetUtilities::getHostAddress(":9999");

  InetAddress exp4  = addr3; // IPv4
  InetAddress exp6  = addr6; // IPv6

  if (!addr1.isIPv4()) { // in the off-chance we get an IPv6 back
    assertEquals("getHostAddress(localhost)",      exp6, addr1);
    assertEquals("getHostAddress(localhost:9999)", exp6, addr2);
  }
  else {
    assertEquals("getHostAddress(localhost)",      exp4, addr1);
    assertEquals("getHostAddress(localhost:9999)", exp4, addr2);
  }
  assertEquals("getHostAddress(127.0.0.1)",        exp4, addr3);
  assertEquals("getHostAddress(127.0.0.1:9999)",   exp4, addr4);
  assertEquals("getHostAddress([::1])",            exp6, addr5);
  assertEquals("getHostAddress([::1]:9999)",       exp6, addr6);
  assertEquals("getHostAddress(:9999)",            true, isNull(addr7));
}

/** Tests host/port access. */
static void _testHostPort (const string &host) {
  int16_t port     = (int16_t)9999;
  string  portOnly = Utilities::format(":%d", (int32_t)port);
  string  hostPort = host + portOnly;

  assertEquals(string("getHost(")+host    +")", host,       NetUtilities::getHost(host));
  assertEquals(string("getPort(")+host    +")", INT16_NULL, NetUtilities::getPort(host));
  assertEquals(string("getHost(")+portOnly+")", "",         NetUtilities::getHost(portOnly));
  assertEquals(string("getPort(")+portOnly+")", port,       NetUtilities::getPort(portOnly));
  assertEquals(string("getHost(")+hostPort+")", host,       NetUtilities::getHost(hostPort));
  assertEquals(string("getPort(")+hostPort+")", port,       NetUtilities::getPort(hostPort));
}

void NetUtilitiesTest::testGetHost () {
  _testHostPort("localhost");
  _testHostPort("127.0.0.1");
  _testHostPort("[::1]");
  _testHostPort("example.com");      // Reserved name under RFC 2606
  _testHostPort("example.net");      // Reserved name under RFC 2606
  _testHostPort("example.org");      // Reserved name under RFC 2606
  _testHostPort("www.example.com");  // Reserved name under RFC 2606
  _testHostPort("www.example.net");  // Reserved name under RFC 2606
  _testHostPort("www.example.org");  // Reserved name under RFC 2606
}
