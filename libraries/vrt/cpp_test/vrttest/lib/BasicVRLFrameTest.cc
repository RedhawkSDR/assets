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

#include "AbstractVRAFileTest.h"
#include "BasicVRLFrameTest.h"
#include "BasicContextPacket.h"
#include "BasicDataPacket.h"
#include "BasicVRLFrame.h"
#include "BasicVRTPacket.h"
#include "HasFieldsTest.h"
#include "TestRunner.h"
#include "VRTConfig.h"

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

#define makeTestPacketVector   AbstractVRAFileTest::makeTestPacketVector
#define deleteTestPacketVector AbstractVRAFileTest::deleteTestPacketVector

string BasicVRLFrameTest::getTestedClass () {
  return "vrt::BasicVRLFrame";
}

vector<TestSet> BasicVRLFrameTest::init () {
  vector<TestSet> tests;

  tests.push_back(TestSet("HEADER_LENGTH",         this,  "testConstants"));
  tests.push_back(TestSet("MAX_FRAME_LENGTH",      this, "+testConstants"));
  tests.push_back(TestSet("MAX_PAYLOAD_LENGTH",    this, "+testConstants"));
  tests.push_back(TestSet("MIN_FRAME_LENGTH",      this, "+testConstants"));
  tests.push_back(TestSet("NO_CRC",                this, "+testConstants"));
  tests.push_back(TestSet("NO_CRC_0",              this, "+testConstants"));
  tests.push_back(TestSet("NO_CRC_1",              this, "+testConstants"));
  tests.push_back(TestSet("NO_CRC_2",              this, "+testConstants"));
  tests.push_back(TestSet("NO_CRC_3",              this, "+testConstants"));
  tests.push_back(TestSet("TRAILER_LENGTH",        this, "+testConstants"));
  tests.push_back(TestSet("VRL_FAW",               this, "+testConstants"));
  tests.push_back(TestSet("VRL_FAW_0",             this, "+testConstants"));
  tests.push_back(TestSet("VRL_FAW_1",             this, "+testConstants"));
  tests.push_back(TestSet("VRL_FAW_2",             this, "+testConstants"));
  tests.push_back(TestSet("VRL_FAW_3",             this, "+testConstants"));

  tests.push_back(TestSet("copy()",                this,  "testCopy"));
  tests.push_back(TestSet("equals(..)",            this, "+testCopy"));
  tests.push_back(TestSet("getFrameCount()",       this,  "testGetFrameCount"));
  tests.push_back(TestSet("getFrameLength()",      this,  "testGetFrame"));
  tests.push_back(TestSet("getFrameValid(..)",     this, "+testIsFrameValid"));
  tests.push_back(TestSet("getPacketCount()",      this, "+testGetFrame"));
  tests.push_back(TestSet("getVRTPackets()",       this, "+testGetFrame"));
  tests.push_back(TestSet("isCRCValid()",          this,  "testIsCRCValid"));
  tests.push_back(TestSet("isFrameValid(..)",      this,  "testIsFrameValid"));

  tests.push_back(TestSet("isVRL(..)",             this,  "testIsVRL"));

  tests.push_back(TestSet("setFrameCount(..)",     this, "+testGetFrameCount"));
  tests.push_back(TestSet("setFrameLength(..)",    this, "+testGetFrame"));
  tests.push_back(TestSet("setVRTPackets(..)",     this, "+testGetFrame"));
  tests.push_back(TestSet("toString()",            this,  "testToString"));
  tests.push_back(TestSet("updateCRC()",           this, "+testIsCRCValid"));

  return tests;
}

void BasicVRLFrameTest::done () {
  // nothing to do
}

void BasicVRLFrameTest::call (const string &func) {
  if (func == "testConstants"       ) testConstants();
  if (func == "testCopy"            ) testCopy();
  if (func == "testGetFrame"        ) testGetFrame();
  if (func == "testGetFrameCount"   ) testGetFrameCount();
  if (func == "testIsCRCValid"      ) testIsCRCValid();
  if (func == "testIsFrameValid"    ) testIsFrameValid();
  if (func == "testIsVRL"           ) testIsVRL();
  if (func == "testToString"        ) testToString();
}

void BasicVRLFrameTest::testConstants () {
  int32_t expFAW = ((BasicVRLFrame::VRL_FAW_0 & 0xFF) << 24)
                 | ((BasicVRLFrame::VRL_FAW_1 & 0xFF) << 16)
                 | ((BasicVRLFrame::VRL_FAW_2 & 0xFF) <<  8)
                 | ((BasicVRLFrame::VRL_FAW_3 & 0xFF)      );
  int32_t expCRC = ((BasicVRLFrame::NO_CRC_0  & 0xFF) << 24)
                 | ((BasicVRLFrame::NO_CRC_1  & 0xFF) << 16)
                 | ((BasicVRLFrame::NO_CRC_2  & 0xFF) <<  8)
                 | ((BasicVRLFrame::NO_CRC_3  & 0xFF)      );

  assertEquals("HEADER_LENGTH",         8,                  BasicVRLFrame::HEADER_LENGTH);
  assertEquals("TRAILER_LENGTH",        4,                  BasicVRLFrame::TRAILER_LENGTH);
  assertEquals("MAX_FRAME_LENGTH",      0x000FFFFF*4,       BasicVRLFrame::MAX_FRAME_LENGTH);
  assertEquals("MIN_FRAME_LENGTH",      8+4,                BasicVRLFrame::MIN_FRAME_LENGTH);
  assertEquals("MAX_PAYLOAD_LENGTH",    (0x000FFFFF*4)-8-4, BasicVRLFrame::MAX_PAYLOAD_LENGTH);
  assertEquals("VRL_FAW",               expFAW,             BasicVRLFrame::VRL_FAW);
  assertEquals("VRL_FAW_0",             (int8_t)'V',        BasicVRLFrame::VRL_FAW_0);
  assertEquals("VRL_FAW_1",             (int8_t)'R',        BasicVRLFrame::VRL_FAW_1);
  assertEquals("VRL_FAW_2",             (int8_t)'L',        BasicVRLFrame::VRL_FAW_2);
  assertEquals("VRL_FAW_3",             (int8_t)'P',        BasicVRLFrame::VRL_FAW_3);
  assertEquals("VRL_FAW",               expCRC,             BasicVRLFrame::NO_CRC);
  assertEquals("VRL_FAW_0",             (int8_t)'V',        BasicVRLFrame::NO_CRC_0);
  assertEquals("VRL_FAW_1",             (int8_t)'E',        BasicVRLFrame::NO_CRC_1);
  assertEquals("VRL_FAW_2",             (int8_t)'N',        BasicVRLFrame::NO_CRC_2);
  assertEquals("VRL_FAW_3",             (int8_t)'D',        BasicVRLFrame::NO_CRC_3);
}

void BasicVRLFrameTest::testCopy () {
  vector<BasicVRTPacket*> packetVector = makeTestPacketVector();
  BasicVRLFrame frame1;
  BasicVRLFrame frame2;
  frame2.setVRTPackets(packetVector);

  BasicVRLFrame f1 = frame1.copy();
  BasicVRLFrame f2 = frame2.copy();

  assertEquals("Frame 1 == F1", true, frame1 == f1);
  assertEquals("Frame 2 == F2", true, frame2 == f2);

  assertEquals("Frame 1 != F2", true, frame1 != f2);
  assertEquals("Frame 2 != F1", true, frame2 != f1);

  deleteTestPacketVector(packetVector);
}

void BasicVRLFrameTest::testGetFrame () {
  vector<BasicVRTPacket*> packetVector = makeTestPacketVector();
  BasicVRLFrame frame1;
  BasicVRLFrame frame2;
  BasicVRLFrame frame3;
  BasicVRLFrame frame4;

  // ==== CHECK INSERTION ====================================================
  int32_t lenForAll = 29495; // should be larger than all packets in array combined
  int32_t lenFor3   = BasicVRLFrame::HEADER_LENGTH
                    + packetVector[0]->getPacketLength()
                    + packetVector[1]->getPacketLength()
                    + packetVector[2]->getPacketLength()
                    + packetVector[3]->getPacketLength() / 2 // <-- doesn't fit
                    + BasicVRLFrame::TRAILER_LENGTH;

  int32_t count1 = 0;
  int32_t count2 = (int32_t)packetVector.size();
                   frame2.setVRTPackets(packetVector);            // <-- all packets into frame
  int32_t count3 = frame3.setVRTPackets(lenForAll, packetVector); // <-- all packets fit
  int32_t count4 = frame4.setVRTPackets(lenFor3,   packetVector); // <-- only 3 packets fit

  assertEquals("Frame 1 Insert Count", 0,                            count1);
  assertEquals("Frame 2 Insert Count", (int32_t)packetVector.size(), count2);
  assertEquals("Frame 3 Insert Count", (int32_t)packetVector.size(), count3);
  assertEquals("Frame 4 Insert Count", 3,                            count4);
  assertEquals("Frame 1 Packet Count", count1,                       frame1.getPacketCount());
  assertEquals("Frame 2 Packet Count", count2,                       frame2.getPacketCount());
  assertEquals("Frame 3 Packet Count", count3,                       frame3.getPacketCount());
  assertEquals("Frame 4 Packet Count", count4,                       frame4.getPacketCount());

  // ==== CHECK FRAME CONTENT ================================================
  void *buf1 = frame1.getFramePointer();
  void *buf2 = frame2.getFramePointer();
  void *buf3 = frame3.getFramePointer();
  void *buf4 = frame4.getFramePointer();

  // Since the FrameCount is 0, the second word will be equal to the FrameLength/4
  assertEquals("Frame 1 Header 1", BasicVRLFrame::VRL_FAW,    VRTMath::unpackInt(buf1, 0));
  assertEquals("Frame 2 Header 1", BasicVRLFrame::VRL_FAW,    VRTMath::unpackInt(buf2, 0));
  assertEquals("Frame 3 Header 1", BasicVRLFrame::VRL_FAW,    VRTMath::unpackInt(buf3, 0));
  assertEquals("Frame 4 Header 1", BasicVRLFrame::VRL_FAW,    VRTMath::unpackInt(buf4, 0));
  assertEquals("Frame 1 Header 2", frame1.getFrameLength()/4, VRTMath::unpackInt(buf1, 4));
  assertEquals("Frame 2 Header 2", frame2.getFrameLength()/4, VRTMath::unpackInt(buf2, 4));
  assertEquals("Frame 3 Header 2", frame3.getFrameLength()/4, VRTMath::unpackInt(buf3, 4));
  assertEquals("Frame 4 Header 2", frame4.getFrameLength()/4, VRTMath::unpackInt(buf4, 4));

  vector<BasicVRTPacket*> pkts1 = frame1.getVRTPackets();
  vector<BasicVRTPacket*> pkts2 = frame2.getVRTPackets();
  vector<BasicVRTPacket*> pkts3 = frame3.getVRTPackets();
  vector<BasicVRTPacket*> pkts4 = frame4.getVRTPackets();

  int32_t off1 = BasicVRLFrame::HEADER_LENGTH;
  int32_t off2 = BasicVRLFrame::HEADER_LENGTH;
  int32_t off3 = BasicVRLFrame::HEADER_LENGTH;
  int32_t off4 = BasicVRLFrame::HEADER_LENGTH;
  for (int32_t i = 0; i < (int32_t)packetVector.size(); i++) {
    int32_t len = packetVector[i]->getPacketLength();
    if (i < count1) {
      assertBufEquals("Packet in Frame 1 (buf)",      buf1, off1, len, packetVector[i]->getPacketPointer(), 0, len);
      assertEquals(   "Packet in Frame 1 (array)",    *packetVector[i], *pkts1[i]);
      off1 += len;
    }
    if (i < count2) {
      assertBufEquals("Packet in Frame 2 (buf)", buf2, off2, len, packetVector[i]->getPacketPointer(), 0, len);
      assertEquals(   "Packet in Frame 2 (array)",    *packetVector[i], *pkts2[i]);
      off2 += len;
    }
    if (i < count3) {
      assertBufEquals("Packet in Frame 3 (buf)", buf3, off3, len, packetVector[i]->getPacketPointer(), 0, len);
      assertEquals(   "Packet in Frame 3 (array)",    *packetVector[i], *pkts3[i]);
      off3 += len;
    }
    if (i < count4) {
      assertBufEquals("Packet in Frame 4 (buf)", buf4, off4, len, packetVector[i]->getPacketPointer(), 0, len);
      assertEquals(   "Packet in Frame 4 (array)",    *packetVector[i], *pkts4[i]);
      off4 += len;
    }
  }
  off1 += BasicVRLFrame::TRAILER_LENGTH;
  off2 += BasicVRLFrame::TRAILER_LENGTH;
  off3 += BasicVRLFrame::TRAILER_LENGTH;
  off4 += BasicVRLFrame::TRAILER_LENGTH;

  assertEquals("Packet in Frame 1 (array len)",    count1,                (int32_t)pkts1.size());
  assertEquals("Packet in Frame 2 (array len)",    count2,                (int32_t)pkts2.size());
  assertEquals("Packet in Frame 3 (array len)",    count3,                (int32_t)pkts3.size());
  assertEquals("Packet in Frame 4 (array len)",    count4,                (int32_t)pkts4.size());
  assertEquals("Frame 1 Length",                   off1,                  frame1.getFrameLength());
  assertEquals("Frame 2 Length",                   off2,                  frame2.getFrameLength());
  assertEquals("Frame 3 Length",                   off3,                  frame3.getFrameLength());
  assertEquals("Frame 4 Length",                   off4,                  frame4.getFrameLength());
  assertEquals("Frame 1 Trailer",                  BasicVRLFrame::NO_CRC, VRTMath::unpackInt(buf1, frame1.getFrameLength()-4));
  assertEquals("Frame 2 Trailer",                  BasicVRLFrame::NO_CRC, VRTMath::unpackInt(buf2, frame2.getFrameLength()-4));
  assertEquals("Frame 3 Trailer",                  BasicVRLFrame::NO_CRC, VRTMath::unpackInt(buf3, frame3.getFrameLength()-4));
  assertEquals("Frame 4 Trailer",                  BasicVRLFrame::NO_CRC, VRTMath::unpackInt(buf4, frame4.getFrameLength()-4));

  // ==== CHECK STATIC LENGTH CHECK ==========================================
  assertEquals("getFrameLength()", frame1.getFrameLength(), BasicVRLFrame::getFrameLength(buf1,0));
  assertEquals("getFrameLength()", frame2.getFrameLength(), BasicVRLFrame::getFrameLength(buf2,0));
  assertEquals("getFrameLength()", frame3.getFrameLength(), BasicVRLFrame::getFrameLength(buf3,0));
  assertEquals("getFrameLength()", frame4.getFrameLength(), BasicVRLFrame::getFrameLength(buf4,0));

  for (int32_t i = 0; i < (int32_t)packetVector.size(); i++) {
    if (i < count1) delete pkts1[i];
    if (i < count2) delete pkts2[i];
    if (i < count3) delete pkts3[i];
    if (i < count4) delete pkts4[i];
  }
  deleteTestPacketVector(packetVector);
}

void BasicVRLFrameTest::testToString () {
  vector<BasicVRTPacket*> packetVector = makeTestPacketVector();
  BasicVRLFrame frame1;
  BasicVRLFrame frame2;
  frame2.setVRTPackets(packetVector);

  // Output string is free-form, so just make sure call works and doesn't return null
  assertEquals("Frame 1 toString() != null", true, !isNull(frame1.toString()));
  assertEquals("Frame 2 toString() != null", true, !isNull(frame2.toString()));

  deleteTestPacketVector(packetVector);
}

void BasicVRLFrameTest::testIsCRCValid () {
  vector<BasicVRTPacket*> packetVector = makeTestPacketVector();

  // The test cases 'example1' and 'example2' come directly from Table A-1 in
  // the ANSI/VITA-49.1 specification. Note that the payloads aren't actually
  // valid VRT packets.
  int32_t example1[4] = { 0x56524C50, 0x00000004,                    0x00000000,  (int32_t)__INT64_C(0xB94CD673) };
  int32_t example2[4] = { 0x56524C50, 0x00000004, (int32_t)__INT64_C(0xFFFFFFFF), (int32_t)__INT64_C(0x7E480B08) };
  vector<char> buf1(4 * 4);
  vector<char> buf2(4 * 4);

  for (int32_t i=0,j=0; i < 4; i++,j+=4) {
    VRTMath::packInt(buf1, j, example1[i]);
  }
  for (int32_t i=0,j=0; i < 4; i++,j+=4) {
    VRTMath::packInt(buf2, j, example2[i]);
  }

  BasicVRLFrame frame1(buf1); // ANSI/VITA-49.1 'Example 1'
  BasicVRLFrame frame2(buf2); // ANSI/VITA-49.1 'Example 2'
  BasicVRLFrame frame3;       // Empty frame     (may have 'VEND')
  BasicVRLFrame frame4;       // Non-Empty frame (may have 'VEND')
  BasicVRLFrame frame5;       // Empty frame     (not 'VEND')
  BasicVRLFrame frame6;       // Non-Empty frame (not 'VEND')
  frame4.setVRTPackets(packetVector);
  frame6.setVRTPackets(packetVector);

  frame5.updateCRC();
  frame6.updateCRC();

  assertEquals("Frame 1 CRC valid", true, frame1.isCRCValid());
  assertEquals("Frame 2 CRC valid", true, frame2.isCRCValid());
  assertEquals("Frame 3 CRC valid", true, frame3.isCRCValid());
  assertEquals("Frame 4 CRC valid", true, frame4.isCRCValid());
  assertEquals("Frame 5 CRC valid", true, frame5.isCRCValid());
  assertEquals("Frame 6 CRC valid", true, frame6.isCRCValid());

  int32_t crc5 = VRTMath::unpackInt(frame5.getFramePointer(), frame5.getFrameLength()-4);
  int32_t crc6 = VRTMath::unpackInt(frame6.getFramePointer(), frame6.getFrameLength()-4);

  assertEquals("Frame 5 CRC not 'VEND'", true, (crc5 != BasicVRLFrame::NO_CRC));
  assertEquals("Frame 6 CRC not 'VEND'", true, (crc6 != BasicVRLFrame::NO_CRC));

  deleteTestPacketVector(packetVector);
}

void BasicVRLFrameTest::testIsFrameValid () {
  vector<BasicVRTPacket*> packetVector = makeTestPacketVector();

  // ==== TEST WITH VALID FRAMES =============================================
  BasicVRLFrame frame1;
  BasicVRLFrame frame2;
  frame2.setVRTPackets(packetVector);
  int32_t len2 = frame2.getFrameLength();

  assertEquals("isFrameValid() for frame1",            true,   frame1.isFrameValid());
  assertEquals("isFrameValid() for frame2",            true,   frame2.isFrameValid());
  assertEquals("isFrameValid(len2) for frame1",        false,  frame1.isFrameValid(len2));
  assertEquals("isFrameValid(len2) for frame2",        true,   frame2.isFrameValid(len2));
  assertEquals("getFrameValid(false) for frame1",      true,  (frame1.getFrameValid(false)      == ""));
  assertEquals("getFrameValid(false) for frame2",      true,  (frame2.getFrameValid(false)      == ""));
  assertEquals("getFrameValid(true) for frame1",       true,  (frame1.getFrameValid(true)       == ""));
  assertEquals("getFrameValid(true) for frame2",       true,  (frame2.getFrameValid(true)       == ""));
  assertEquals("getFrameValid(false,len2) for frame1", false, (frame1.getFrameValid(false,len2) == ""));
  assertEquals("getFrameValid(false,len2) for frame2", true,  (frame2.getFrameValid(false,len2) == ""));
  assertEquals("getFrameValid(true,len2) for frame1",  false, (frame1.getFrameValid(true,len2)  == ""));
  assertEquals("getFrameValid(true,len2) for frame2",  true,  (frame2.getFrameValid(true,len2)  == ""));

  // ==== TEST WITH INVALID FRAMES ===========================================
  // Here frame3 will fail all checks, while frame4 will only fail strict checks.
  char invalidFrame3[16] = { BasicVRLFrame::VRL_FAW_0, BasicVRLFrame::VRL_FAW_1, BasicVRLFrame::VRL_FAW_2, BasicVRLFrame::VRL_FAW_3,
                             0,                        0,                        0,                        0, // <- Invalid frame size
                             0,                        0,                        0,                        1,
                             BasicVRLFrame::NO_CRC_0,  BasicVRLFrame::NO_CRC_1,  BasicVRLFrame::NO_CRC_2,  BasicVRLFrame::NO_CRC_3 };
  char invalidFrame4[16] = { BasicVRLFrame::VRL_FAW_0, BasicVRLFrame::VRL_FAW_1, BasicVRLFrame::VRL_FAW_2, BasicVRLFrame::VRL_FAW_3,
                             0,                        0,                        0,                        4,
                             0,                        0,                        0,                        0, // <- Invalid packet size
                             BasicVRLFrame::NO_CRC_0,  BasicVRLFrame::NO_CRC_1,  BasicVRLFrame::NO_CRC_2,  BasicVRLFrame::NO_CRC_3 };
  BasicVRLFrame frame3(invalidFrame3, 16);
  BasicVRLFrame frame4(invalidFrame4, 16);

  assertEquals("isFrameValid() for frame3",            false,  frame3.isFrameValid());
  assertEquals("isFrameValid() for frame4",            false,  frame4.isFrameValid());
  assertEquals("getFrameValid(false) for frame3",      false, (frame3.getFrameValid(false) == ""));
  assertEquals("getFrameValid(false) for frame4",      true,  (frame4.getFrameValid(false) == ""));
  assertEquals("getFrameValid(true) for frame3",       false, (frame3.getFrameValid(true)  == ""));
  assertEquals("getFrameValid(true) for frame4",       false, (frame4.getFrameValid(true)  == ""));

  deleteTestPacketVector(packetVector);
}

void BasicVRLFrameTest::testIsVRL () {
  vector<BasicVRTPacket*> packetVector = makeTestPacketVector();
  BasicVRLFrame           frame1;
  BasicVRLFrame           frame2;
  BasicDataPacket    packet1;
  BasicContextPacket packet2;
  frame2.setVRTPackets(packetVector);

  assertEquals("isFrameValid() for frame1",  true,  BasicVRLFrame::isVRL(frame1.getFramePointer(), 0));
  assertEquals("isFrameValid() for frame2",  true,  BasicVRLFrame::isVRL(frame2.getFramePointer(), 0));
  assertEquals("isFrameValid() for packet1", false, BasicVRLFrame::isVRL(packet1.getPacketPointer(), 0));
  assertEquals("isFrameValid() for packet2", false, BasicVRLFrame::isVRL(packet2.getPacketPointer(), 0));

  deleteTestPacketVector(packetVector);
}

void BasicVRLFrameTest::testGetFrameCount () {
  vector<BasicVRTPacket*> packetVector = makeTestPacketVector();
  BasicVRLFrame frame1;
  BasicVRLFrame frame2;
  frame2.setVRTPackets(packetVector);

  assertEquals("Frame 1 count", 0, frame1.getFrameCount());
  assertEquals("Frame 2 count", 0, frame2.getFrameCount());

  frame1.setFrameCount(0x42);
  frame2.setFrameCount(0x00000876);

  assertEquals("Frame 1 count",  0x42,       frame1.getFrameCount());
  assertEquals("Frame 2 count",  0x00000876, frame2.getFrameCount());

  assertEquals("Frame 1 header", 0x04200000, VRTMath::unpackInt(frame1.getFramePointer(),4) & 0xFFF00000);
  assertEquals("Frame 2 header", 0x87600000, VRTMath::unpackInt(frame2.getFramePointer(),4) & 0xFFF00000);

  deleteTestPacketVector(packetVector);
}
