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

#include "InetAddressTest.h"
#include "TestRunner.h"
#include "InetAddress.h"
#include <errno.h>
#include <netinet/in.h>
#include <VRTConfig.h>

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

#if (defined(__APPLE__) && defined(__MACH__))
// See notes in InetAddress.cc for details...
# ifndef s6_addr16
#  define s6_addr16 __u6_addr.__u6_addr16
# endif
# ifndef s6_addr32
#  define s6_addr32 __u6_addr.__u6_addr32
# endif
#endif

string InetAddressTest::getTestedClass () {
  return "vrt::InetAddress";
}

vector<TestSet> InetAddressTest::init () {
  vector<TestSet> tests;
  tests.push_back(TestSet("equals(..)",           this,  "testEquals"));
  tests.push_back(TestSet("getField(..)",         this, "+testGetHostAddress"));
  tests.push_back(TestSet("getFieldCount()",      this,  "testGetFieldCount"));
  tests.push_back(TestSet("getFieldName()",       this,  "testGetFieldName"));
  tests.push_back(TestSet("getFieldType()",       this,  "testGetFieldType"));
  tests.push_back(TestSet("getHostAddress()",     this,  "testGetHostAddress"));
  tests.push_back(TestSet("getLocalHost()",       this, "-testGetLocalHost"));
  tests.push_back(TestSet("getLoopbackAddress()", this,  "testGetLoopbackAddress"));
  tests.push_back(TestSet("isMulticastAddress()", this,  "testIsMulticastAddress"));
  tests.push_back(TestSet("isNullValue()",        this, "+testGetHostAddress"));
  tests.push_back(TestSet("isIPv4()",             this, "+testGetHostAddress"));
  tests.push_back(TestSet("setField(..)",         this, "+testGetHostAddress"));
  tests.push_back(TestSet("setHostAddress(..)",   this, "+testGetHostAddress"));
  tests.push_back(TestSet("toIPv4()",             this,  "testToIPv4"));
  tests.push_back(TestSet("toIPv6()",             this, "+testToIPv4"));
  tests.push_back(TestSet("toString()",           this, "+testGetHostAddress"));

  return tests;
}

void InetAddressTest::done () {
  // nothing to do
}

void InetAddressTest::call (const string &func) {
  if (func == "testEquals"            ) testEquals();
  if (func == "testGetFieldCount"     ) testGetFieldCount();
  if (func == "testGetFieldName"      ) testGetFieldName();
  if (func == "testGetFieldType"      ) testGetFieldType();
  if (func == "testGetHostAddress"    ) testGetHostAddress();
  if (func == "testGetLocalHost"      ) testGetLocalHost();
  if (func == "testGetLoopbackAddress") testGetLoopbackAddress();
  if (func == "testIsMulticastAddress") testIsMulticastAddress();
  if (func == "testToIPv4"            ) testToIPv4();
}

void InetAddressTest::testEquals () {
  VRTObject      junk;
  vector<string> addresses;
  vector<char  > matches;

  addresses.push_back("0.0.0.0"           );  matches.push_back('A');
  addresses.push_back("::FFFF:0.0.0.0"    );  matches.push_back('A');
  addresses.push_back("::0"               );  matches.push_back('B');
  addresses.push_back("127.0.0.1"         );  matches.push_back('C');
  addresses.push_back("::FFFF:127.0.0.1"  );  matches.push_back('C');
  addresses.push_back("10.10.255.1"       );  matches.push_back('D');
  addresses.push_back("::FFFF:10.10.255.1");  matches.push_back('D');
  addresses.push_back("::FFFF:0A0A:FF01"  );  matches.push_back('D');

  for (size_t i = 0; i < addresses.size(); i++) {
    InetAddress a(addresses[i]);

    assertEquals(string("a.equals(a) for ")+addresses[i],    true,  a.equals(a));
    assertEquals(string("a.equals(junk) for ")+addresses[i], false, a.equals(junk));
    assertEquals(string("junk.equals(a) for ")+addresses[i], false, junk.equals(a));

    for (size_t j = 0; j < addresses.size(); j++) {
      InetAddress b(addresses[j]);
      bool exp = (matches[i] == matches[j]);
      bool act = a.equals(b);

      assertEquals(string("a.equals(b) for a=")+addresses[i]+string(" and b=")+addresses[j], exp, act);
    }
  }
}

void InetAddressTest::testGetFieldCount () {
  InetAddress addr1("0.0.0.0");
  InetAddress addr2("127.0.0.1");

  assertEquals("addr1.getFieldCount()", 1, addr1.getFieldCount());
  assertEquals("addr2.getFieldCount()", 1, addr2.getFieldCount());
}

void InetAddressTest::testGetFieldName () {
  InetAddress addr1("0.0.0.0");
  InetAddress addr2("127.0.0.1");

  assertEquals("addr1.getFieldName(0)", "HostAddress", addr1.getFieldName(0));
  assertEquals("addr2.getFieldName(0)", "HostAddress", addr2.getFieldName(0));
}

void InetAddressTest::testGetFieldType () {
  InetAddress addr1("0.0.0.0");
  InetAddress addr2("127.0.0.1");

  assertEquals("addr1.getFieldType(0)", ValueType_String, addr1.getFieldType(0));
  assertEquals("addr2.getFieldType(0)", ValueType_String, addr2.getFieldType(0));
}

/** Tests for host address.
 *  @param given  The given address.
 *  @param exp    The expected output (normalized) or expected error message in
 *                the form "<msg>".
 *  @param isNull Is this a null (e.g. "0.0.0.0") value?
 *  @param isIPv4 Is this an IPv4 address?
 */
static void checkHostAddress (const string &given, const string &exp, bool isNull, bool isIPv4) {
  string input = string("'") + given + string("'");

  if (exp[0] == '<') {
    string errMsg = exp.substr(1,exp.size()-2);

    try {
      InetAddress addr(given);
      assertEquals(string("InetAddress(..) for ")+input+" caught VRTException", errMsg, "<null>");
    }
    catch (VRTException e) {
      assertEquals(string("InetAddress(..) for ")+input+" caught VRTException", errMsg, e.getMessage());
    }

    try {
      InetAddress addr2;
      addr2.setHostAddress(given);
      assertEquals(string("setHostAddress(..) for ")+input+" caught VRTException", errMsg, "<null>");
    }
    catch (VRTException e) {
      assertEquals(string("setHostAddress(..) for ")+input+" caught VRTException", errMsg, e.getMessage());
    }

    try {
      InetAddress addr3;
      Value v = Value(given);
      addr3.setField(0, &v);
      assertEquals(string("setField(0,..) for ")+input+" caught VRTException", errMsg, "<null>");
    }
    catch (VRTException e) {
      assertEquals(string("setField(0,..) for ")+input+" caught VRTException", errMsg, e.getMessage());
    }
  }
  else {
    InetAddress addr(given);

    Value *val = addr.getField(0);

    assertEquals(string("getHostAddress() for ")+input, exp,    addr.getHostAddress());
    assertEquals(string("toString() for ")+input,       exp,    addr.toString());
    assertEquals(string("getField(0) for ")+input,      exp,    val->toString());
    assertEquals(string("isNullValue() for ")+input,    isNull, addr.isNullValue());
    assertEquals(string("isIPv4() for ")+input,         isIPv4, addr.isIPv4());

    delete val;

    InetAddress addr2;
    addr2.setHostAddress(given);
    assertEquals(string("setHostAddress(..) for ")+input, exp, addr2.getHostAddress());

    InetAddress addr3;
    Value v = Value(given);
    addr3.setField(0, &v);
    assertEquals(string("setField(0,..) for ")+input, exp, addr2.getHostAddress());
  }
}

void InetAddressTest::testGetHostAddress () {
  checkHostAddress("0.0.0.0",            "0.0.0.0",                                 true,  true );
  checkHostAddress("::0",                "0000:0000:0000:0000:0000:0000:0000:0000", true,  false);
  checkHostAddress("::FFFF:0.0.0.0",     "0.0.0.0",                                 true,  true ); // IPv4 in IPv6
  checkHostAddress("127.0.0.1",          "127.0.0.1",                               false, true );
  checkHostAddress("::FFFF:127.0.0.1",   "127.0.0.1",                               false, true ); // IPv4 in IPv6
  checkHostAddress("10.10.255.1",        "10.10.255.1",                             false, true );
  checkHostAddress("::FFFF:10.10.255.1", "10.10.255.1",                             false, true ); // IPv4 in IPv6
  checkHostAddress("::FFFF:0A0A:FF01",   "10.10.255.1",                             false, true ); // IPv4 in IPv6

  // ERROR CASES
  checkHostAddress("127.0.0.1024",       "<Invalid IPv4 HostAddress given '127.0.0.1024'>",    false, true ); // Bad IPv4
  checkHostAddress("::FFFF:0A0AFF01",    "<Invalid IPv6 HostAddress given '::FFFF:0A0AFF01'>", false, false); // Bad IPv6
  checkHostAddress("",                   "<Invalid HostAddress given empty/null string>",      false, true ); // Bad IPv4/IPv6
}

void InetAddressTest::testGetLocalHost () {
  // No good way to test this without duplicating the code in InetAddress, so we
  // just verify that it isn't the same as the loop-back address. Note that this
  // test will fail in the rare case that the build of InetAddress produces a
  // a warning about gethostname(..) being unsupported.
  InetAddress bad4("127.0.0.1");
  InetAddress bad6("::1");
  InetAddress act  = InetAddress::getLocalHost();

  assertEquals("getLocalHost() is 127.0.0.1", false, bad4 == act);
  assertEquals("getLocalHost() is ::1",       false, bad6 == act);
}

void InetAddressTest::testGetLoopbackAddress () {
  InetAddress exp;
  InetAddress act = InetAddress::getLoopbackAddress();

  if (VRTConfig::getPreferIPv6Addresses()) {
    exp = InetAddress("::1");
  }
  else {
    exp = InetAddress("127.0.0.1");
  }
  assertEquals("getLoopbackAddress()", exp, act);
}

/** Checks toIPv4() and toIPv4() for an IPv4 address with address given as string,
 *  and char values.
 */
static void checkToIPv4 (const string &given, uint8_t a, uint8_t b, uint8_t c, uint8_t d) {
  string      input = string("'") + given + string("'");
  InetAddress addr(given);

  struct in_addr  ipv4;
  struct in6_addr ipv6;

  uint32_t ip = ((((uint32_t)a) << 24) & 0xFF000000)
              | ((((uint32_t)b) << 16) & 0x00FF0000)
              | ((((uint32_t)c) <<  8) & 0x0000FF00)
              | ((((uint32_t)d)      ) & 0x000000FF);


  ipv4.s_addr       = htonl(ip);
  ipv6.s6_addr16[0] = 0x0000;
  ipv6.s6_addr16[1] = 0x0000;
  ipv6.s6_addr16[2] = 0x0000;
  ipv6.s6_addr16[3] = 0x0000;
  ipv6.s6_addr16[4] = 0x0000;
  ipv6.s6_addr16[5] = 0xFFFF;
  ipv6.s6_addr32[3] = ipv4.s_addr;

  assertEquals(string("toIPv4() for ")+input, ipv4.s_addr,       addr.toIPv4().s_addr);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[0], addr.toIPv6().s6_addr16[0]);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[1], addr.toIPv6().s6_addr16[1]);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[2], addr.toIPv6().s6_addr16[2]);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[3], addr.toIPv6().s6_addr16[3]);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[4], addr.toIPv6().s6_addr16[4]);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[5], addr.toIPv6().s6_addr16[5]);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[6], addr.toIPv6().s6_addr16[6]);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[7], addr.toIPv6().s6_addr16[7]);
}

/** Checks toIPv4() and toIPv4() for an IPv6 address with address given as string,
 *  and short values.
 */
static void checkToIPv6 (const string &given, uint16_t a, uint16_t b, uint16_t c, uint16_t d,
                                              uint16_t e, uint16_t f, uint16_t g, uint16_t h) {
  string      input = string("'") + given + string("'");
  InetAddress addr(given);

  struct in_addr  ipv4;
  struct in6_addr ipv6;

  ipv4.s_addr       = 0x00000000;
  ipv6.s6_addr16[0] = a;
  ipv6.s6_addr16[1] = b;
  ipv6.s6_addr16[2] = c;
  ipv6.s6_addr16[3] = d;
  ipv6.s6_addr16[4] = e;
  ipv6.s6_addr16[5] = f;
  ipv6.s6_addr16[6] = g;
  ipv6.s6_addr16[7] = h;

  assertEquals(string("toIPv4() for ")+input, ipv4.s_addr,       addr.toIPv4().s_addr);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[0], addr.toIPv6().s6_addr16[0]);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[1], addr.toIPv6().s6_addr16[1]);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[2], addr.toIPv6().s6_addr16[2]);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[3], addr.toIPv6().s6_addr16[3]);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[4], addr.toIPv6().s6_addr16[4]);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[5], addr.toIPv6().s6_addr16[5]);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[6], addr.toIPv6().s6_addr16[6]);
  assertEquals(string("toIPv6() for ")+input, ipv6.s6_addr16[7], addr.toIPv6().s6_addr16[7]);
}

void InetAddressTest::testToIPv4 () {
  checkToIPv4("0.0.0.0",            0,0,0,0);
  checkToIPv4("::FFFF:0.0.0.0",     0,0,0,0);
  checkToIPv4("127.0.0.1",          127,0,0,1);
  checkToIPv4("::FFFF:127.0.0.1",   127,0,0,1);
  checkToIPv4("10.10.255.1",        10,10,255,1);
  checkToIPv4("::FFFF:10.10.255.1", 10,10,255,1);
  checkToIPv4("::FFFF:0A0A:FF01",   10,10,255,1);

  checkToIPv6("::0",                0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000);
}

/** Checks to see if a given address is a multicast address. */
static void checkMCast (const string &addr, bool exp) {
  InetAddress inet(addr);
  assertEquals(string("isMulticastAddress() for ")+addr, exp, inet.isMulticastAddress());
}

void InetAddressTest::testIsMulticastAddress () {
  // Well-Known IPv4 Multicast Addresses
  checkMCast("224.0.0.0",  true); // Base Address (reserved)
  checkMCast("224.0.0.22", true); // IGMP
  checkMCast("224.0.1.1",  true); // NTP

  // Well-Known IPv6 Multicast Addresses
  checkMCast("ff02::c",   true); // SSDP
  checkMCast("ff02::101", true); // NTP

  // Check IPv4 Range
  for (int32_t a = 0; a <= 255; a++) {
    string firstOctet = Utilities::format("%d", a);

    checkMCast(firstOctet+".0.0.0",       (a >= 224) && (a <= 239));
    checkMCast(firstOctet+".1.1.1",       (a >= 224) && (a <= 239));
    checkMCast(firstOctet+".11.11.11",    (a >= 224) && (a <= 239));
    checkMCast(firstOctet+".111.111.111", (a >= 224) && (a <= 239));
    checkMCast(firstOctet+".255.255.255", (a >= 224) && (a <= 239));
  }

  // Check IPv6 Range
  for (int32_t i = 0; i <= 0xFF; i++) {
    string firstOctet = Utilities::toHexString(i,2);

    checkMCast(firstOctet+"02::0000", (i == 0xFF));
    checkMCast(firstOctet+"02::3333", (i == 0xFF));
    checkMCast(firstOctet+"02::6666", (i == 0xFF));
    checkMCast(firstOctet+"02::9999", (i == 0xFF));
    checkMCast(firstOctet+"02::CCCC", (i == 0xFF));
    checkMCast(firstOctet+"02::FFFF", (i == 0xFF));
  }
}
