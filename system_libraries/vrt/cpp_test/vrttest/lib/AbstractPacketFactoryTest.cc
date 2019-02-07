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

#include "AbstractPacketFactoryTest.h"
#include "TestRunner.h"
#include "AbstractPacketFactory.h"

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

string AbstractPacketFactoryTest::getTestedClass () {
  return "vrt::AbstractPacketFactory";
}

vector<TestSet> AbstractPacketFactoryTest::init () {
  vector<TestSet> tests;
  tests.push_back(TestSet("getPacket(..)", this, "testGetPacket"));

  return tests;
}

void AbstractPacketFactoryTest::done () {
  // nothing to do
}

void AbstractPacketFactoryTest::call (const string &func) {
  if (func == "testGetPacket") testGetPacket();
}

#define addPacketTest(type,classID,superClass) \
  testTypes.push_back(type); \
  testClassID.push_back(classID); \
  testClassName.push_back(superClass);

void AbstractPacketFactoryTest::testGetPacket () {
  AbstractPacketFactory fact;

  vector<PacketType> testTypes;
  vector<string>     testClassID;
  vector<string>     testClassName;

  addPacketTest(PacketType_UnidentifiedData,     "FF-FF-FF:1234.ABCD",  "vrt::BasicDataPacket");
  addPacketTest(PacketType_Data,                 "FF-FF-FF:1234.ABCD",  "vrt::BasicDataPacket");
  addPacketTest(PacketType_UnidentifiedExtData,  "FF-FF-FF:1234.ABCD",  "vrt::BasicDataPacket");
  addPacketTest(PacketType_ExtData,              "FF-FF-FF:1234.ABCD",  "vrt::BasicDataPacket");
  addPacketTest(PacketType_Context,              "FF-FF-FF:1234.ABCD",  "vrt::BasicContextPacket");
  addPacketTest(PacketType_ExtContext,           "FF-FF-FF:1234.ABCD",  "vrt::BasicVRTPacket");
  addPacketTest(PacketType_Data,                 "FF-FF-FA:0000.0000",  "vrt::StandardDataPacket"); //  1-bit
  addPacketTest(PacketType_ExtData,              "FF-FF-FA:0000.0000",  "vrt::StandardDataPacket"); //  1-bit
  addPacketTest(PacketType_Data,                 "FF-FF-FA:0001.0000",  "vrt::StandardDataPacket"); //  4-bit
  addPacketTest(PacketType_ExtData,              "FF-FF-FA:0001.0000",  "vrt::StandardDataPacket"); //  4-bit
  addPacketTest(PacketType_Data,                 "FF-FF-FA:0002.0000",  "vrt::StandardDataPacket"); //  8-bit
  addPacketTest(PacketType_ExtData,              "FF-FF-FA:0002.0000",  "vrt::StandardDataPacket"); //  8-bit
  addPacketTest(PacketType_Data,                 "FF-FF-FA:0003.0000",  "vrt::StandardDataPacket"); // 16-bit
  addPacketTest(PacketType_ExtData,              "FF-FF-FA:0003.0000",  "vrt::StandardDataPacket"); // 16-bit
  addPacketTest(PacketType_Data,                 "FF-FF-FA:0004.0000",  "vrt::StandardDataPacket"); // 32-bit
  addPacketTest(PacketType_ExtData,              "FF-FF-FA:0004.0000",  "vrt::StandardDataPacket"); // 32-bit
  addPacketTest(PacketType_Data,                 "FF-FF-FA:0005.0000",  "vrt::StandardDataPacket"); // 64-bit
  addPacketTest(PacketType_ExtData,              "FF-FF-FA:0005.0000",  "vrt::StandardDataPacket"); // 64-bit

  for (size_t i = 0; i < testTypes.size(); i++) {
    PacketType     type  = testTypes[i];
    string         idStr = testClassID[i];
    int64_t        idNum = Utilities::fromStringClassID(idStr);
    string         clazz = testClassName[i];
    BasicVRTPacket pkt;

    pkt.setPacketType(type);
    if ((type != PacketType_UnidentifiedData) && (type != PacketType_UnidentifiedExtData)) {
      pkt.setClassID(idStr);
    }
    pkt.setPayloadLength(0);

    BasicVRTPacket *actObj1  = fact.getPacket(type, idNum);
    BasicVRTPacket *actObj2  = fact.getPacket(&pkt);

    assertEquals("getPacket(type,id)",     clazz, actObj1->getClassName());
    assertEquals("getPacket(pkt) type",    clazz, actObj2->getClassName());
    assertEquals("getPacket(pkt) value",   pkt,  *actObj2);

    delete actObj1;
    delete actObj2;
  }
}
