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

#include "BasicVRTPacketTest.h"
#include "BasicContextPacket.h"
#include "BasicDataPacket.h"
#include "BasicVRTPacket.h"
#include "HasFieldsTest.h"
#include "TestRunner.h"
#include "VRTConfig.h"

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

/** Does the packet type have a stream identifier? */
inline static bool hasStreamIdentifier (PacketType t) {
  return (t != PacketType_UnidentifiedData) && (t != PacketType_UnidentifiedExtData);
}

string BasicVRTPacketTest::getTestedClass () {
  return "vrt::BasicVRTPacket";
}

vector<TestSet> BasicVRTPacketTest::init () {
  vector<TestSet> tests;

  tests.push_back(TestSet("copy(..)",                     this, "+testEquals"));
  tests.push_back(TestSet("equals(..)",                   this, "testEquals"));
  tests.push_back(TestSet("getClassID()",                 this, "testGetClassID"));
  tests.push_back(TestSet("getClassIdentifier()",         this, "+testGetClassID"));
  tests.push_back(TestSet("getClassIdentifierICC()",      this, "+testGetClassID"));
  tests.push_back(TestSet("getClassIdentifierOUI()",      this, "+testGetClassID"));
  tests.push_back(TestSet("getClassIdentifierPCC()",      this, "+testGetClassID"));
  tests.push_back(TestSet("getField(..)",                 this,  "testGetField"));
  tests.push_back(TestSet("getFieldByName(..)",           this, "+testGetField"));
  tests.push_back(TestSet("getFieldCount()",              this, "+testGetFieldName"));
  tests.push_back(TestSet("getFieldID(..)",               this, "+testGetFieldName"));
  tests.push_back(TestSet("getFieldName(..)",             this,  "testGetFieldName"));
  tests.push_back(TestSet("getFieldType(..)",             this, "+testGetFieldName"));
  tests.push_back(TestSet("getHeaderLength()",            this, "+testGetPacketLength"));
  tests.push_back(TestSet("getPacket()",                  this, "testGetPacket"));
  tests.push_back(TestSet("getPacketCount()",             this, "testGetPacketCount"));
  tests.push_back(TestSet("getPacketLength()",            this, "testGetPacketLength"));
  tests.push_back(TestSet("getPacketType()",              this, "testGetPacketType"));
  tests.push_back(TestSet("getPacketValid(..)",           this, "testGetPacketValid"));

  if (VRTConfig::getVRTVersion() == VRTConfig::VITAVersion_V49) {
    tests.push_back(TestSet("getPadBitCount(..) VRT_VERSION=V49",  this, "testGetPadBitCount"));
    tests.push_back(TestSet("getPadBitCount(..) VRT_VERSION=V49b", this, "-testGetPadBitCount"));
  }
  else {
    tests.push_back(TestSet("getPadBitCount(..) VRT_VERSION=V49",  this, "-testGetPadBitCount"));
    tests.push_back(TestSet("getPadBitCount(..) VRT_VERSION=V49b", this, "testGetPadBitCount"));
  }

  tests.push_back(TestSet("getPayload()",                 this, "+testGetPacket"));
  tests.push_back(TestSet("getPayloadLength()",           this, "+testGetPacketLength"));
  tests.push_back(TestSet("getStreamCode()",              this, "testGetStreamCode"));
  tests.push_back(TestSet("getStreamID()",                this, "testGetStreamID"));
  tests.push_back(TestSet("getStreamIdentifier()",        this, "+testGetStreamID"));
  tests.push_back(TestSet("getTimeStamp()",               this, "testGetTimeStamp"));
  tests.push_back(TestSet("getTrailerLength()",           this, "+testGetPacketLength"));
  tests.push_back(TestSet("headerEquals(..)",             this, "+testEquals"));
  tests.push_back(TestSet("isChangePacket()",             this, "testIsChangePacket"));
  tests.push_back(TestSet("isPacketValid(..)",            this, "+testGetPacketValid"));
  tests.push_back(TestSet("isTimeStampMode()",            this, "testIsTimeStampMode"));
  tests.push_back(TestSet("payloadEquals(..)",            this, "+testEquals"));
  tests.push_back(TestSet("readPacket(..)",               this, "testReadPacket"));
  tests.push_back(TestSet("readPayload(..)",              this, "+testReadPacket"));
  tests.push_back(TestSet("resetForResend(..)",           this, "testResetForResend"));
  tests.push_back(TestSet("setClassID(..)",               this, "+testGetClassID"));
  tests.push_back(TestSet("setClassIdentifier(..)",       this, "+testGetClassID"));
  tests.push_back(TestSet("setField(..)",                 this, "+testGetField"));
  tests.push_back(TestSet("setFieldByName(..)",           this, "+testGetField"));
  tests.push_back(TestSet("setPacketCount(..)",           this, "+testGetPacketCount"));
  tests.push_back(TestSet("setPacketType(..)",            this, "+testGetPacketType"));
  tests.push_back(TestSet("setPadBitCount(..)",           this, "+testGetPadBitCount"));
  tests.push_back(TestSet("setPayload(..)",               this, "+testGetPacket"));
  tests.push_back(TestSet("setPayloadLength(..)",         this, "+testGetPacketLength"));
  tests.push_back(TestSet("setStreamID(..)",              this, "+testGetStreamID"));
  tests.push_back(TestSet("setStreamIdentifier(..)",      this, "+testGetStreamID"));
  tests.push_back(TestSet("setTimeStamp(..)",             this, "+testGetTimeStamp"));
  tests.push_back(TestSet("setTimeStampMode(..)",         this, "+testIsTimeStampMode"));
  tests.push_back(TestSet("toString()",                   this, "testToString"));
  tests.push_back(TestSet("trailerEquals(..)",            this, "+testEquals"));
  tests.push_back(TestSet("writePayload(..)",             this, "testWritePayload"));
  tests.push_back(TestSet("isSpectrumMode()",             this, "testIsSpectrumMode"));

  return tests;
}

void BasicVRTPacketTest::done () {
  // nothing to do
}

void BasicVRTPacketTest::call (const string &func) {
  if (func == "testEquals"          ) testEquals();
  if (func == "testGetClassID"      ) testGetClassID();
  if (func == "testGetField"        ) testGetField();
  if (func == "testGetFieldName"    ) testGetFieldName();
  if (func == "testGetPacket"       ) testGetPacket();
  if (func == "testGetPacketCount"  ) testGetPacketCount();
  if (func == "testGetPacketLength" ) testGetPacketLength();
  if (func == "testGetPacketType"   ) testGetPacketType();
  if (func == "testGetPacketValid"  ) testGetPacketValid();
  if (func == "testGetPadBitCount"  ) testGetPadBitCount();
  if (func == "testGetStreamCode"   ) testGetStreamCode();
  if (func == "testGetStreamID"     ) testGetStreamID();
  if (func == "testGetTimeStamp"    ) testGetTimeStamp();
  if (func == "testIsChangePacket"  ) testIsChangePacket();
  if (func == "testIsTimeStampMode" ) testIsTimeStampMode();
  if (func == "testReadPacket"      ) testReadPacket();
  if (func == "testResetForResend"  ) testResetForResend();
  if (func == "testToString"        ) testToString();
  if (func == "testWritePayload"    ) testWritePayload();
  if (func == "testIsSpectrumMode"  ) testIsSpectrumMode();
}


void BasicVRTPacketTest::testEquals () {
  char            data18[8] = { 1,2,3,4,5,6,7,8 };
  char            data81[8] = { 8,7,6,5,4,3,2,1 };
  BasicDataPacket givenPkt;  // Initial packet
  BasicDataPacket cpyEqual;  // Copy of packet
  BasicVRTPacket  bufEqual;  // Copy of packet buffer
  BasicDataPacket allEqual;  // All fields equal
  BasicDataPacket hdrEqual;  // Header  is equal
  BasicDataPacket payEqual;  // Payload is equal
  BasicDataPacket trlEqual;  // Trailer is equal

  TimeStamp ts2000 = TimeStamp::parseTime("2000-01-01T00:00:00Z", IntegerMode_GPS);
  TimeStamp ts2013 = TimeStamp::parseTime("2013-01-01T00:00:00Z", IntegerMode_GPS);

  givenPkt.setTimeStamp(ts2013);
  allEqual.setTimeStamp(ts2013);
  hdrEqual.setTimeStamp(ts2013);
  payEqual.setTimeStamp(ts2000);
  trlEqual.setTimeStamp(ts2000);

  givenPkt.setData(PayloadFormat_INT8, data18, 8);
  allEqual.setData(PayloadFormat_INT8, data18, 8);
  hdrEqual.setData(PayloadFormat_INT8, data81, 8);
  payEqual.setData(PayloadFormat_INT8, data18, 8);
  trlEqual.setData(PayloadFormat_INT8, data81, 8);

  givenPkt.setCalibratedTimeStamp(_TRUE );
  allEqual.setCalibratedTimeStamp(_TRUE );
  hdrEqual.setCalibratedTimeStamp(_FALSE);
  payEqual.setCalibratedTimeStamp(_FALSE);
  trlEqual.setCalibratedTimeStamp(_TRUE );

  cpyEqual = givenPkt;                  // <-- in C++ uses copy-constructor (implicit)
  bufEqual = BasicVRTPacket(givenPkt);  // <-- in C++ uses copy-constructor (explicit)

  assertEquals("equals(..) for givenPkt", true , givenPkt.equals(givenPkt));
  assertEquals("equals(..) for cpyEqual", true , givenPkt.equals(cpyEqual));
  assertEquals("equals(..) for bufEqual", true , givenPkt.equals(bufEqual));
  assertEquals("equals(..) for allEqual", true , givenPkt.equals(allEqual));
  assertEquals("equals(..) for hdrEqual", false, givenPkt.equals(hdrEqual));
  assertEquals("equals(..) for payEqual", false, givenPkt.equals(payEqual));
  assertEquals("equals(..) for trlEqual", false, givenPkt.equals(trlEqual));

  assertEquals("headerEquals(..) for givenPkt", true , givenPkt.headerEquals(givenPkt));
  assertEquals("headerEquals(..) for cpyEqual", true , givenPkt.headerEquals(cpyEqual));
  assertEquals("headerEquals(..) for bufEqual", true , givenPkt.headerEquals(bufEqual));
  assertEquals("headerEquals(..) for allEqual", true , givenPkt.headerEquals(allEqual));
  assertEquals("headerEquals(..) for hdrEqual", true , givenPkt.headerEquals(hdrEqual));
  assertEquals("headerEquals(..) for payEqual", false, givenPkt.headerEquals(payEqual));
  assertEquals("headerEquals(..) for trlEqual", false, givenPkt.headerEquals(trlEqual));

  assertEquals("payloadEquals(..) for givenPkt", true , givenPkt.payloadEquals(givenPkt));
  assertEquals("payloadEquals(..) for cpyEqual", true , givenPkt.payloadEquals(cpyEqual));
  assertEquals("payloadEquals(..) for bufEqual", true , givenPkt.payloadEquals(bufEqual));
  assertEquals("payloadEquals(..) for allEqual", true , givenPkt.payloadEquals(allEqual));
  assertEquals("payloadEquals(..) for hdrEqual", false, givenPkt.payloadEquals(hdrEqual));
  assertEquals("payloadEquals(..) for payEqual", true , givenPkt.payloadEquals(payEqual));
  assertEquals("payloadEquals(..) for trlEqual", false, givenPkt.payloadEquals(trlEqual));

  assertEquals("trailerEquals(..) for givenPkt", true , givenPkt.trailerEquals(givenPkt));
  assertEquals("trailerEquals(..) for cpyEqual", true , givenPkt.trailerEquals(cpyEqual));
  assertEquals("trailerEquals(..) for bufEqual", true , givenPkt.trailerEquals(bufEqual));
  assertEquals("trailerEquals(..) for allEqual", true , givenPkt.trailerEquals(allEqual));
  assertEquals("trailerEquals(..) for hdrEqual", false, givenPkt.trailerEquals(hdrEqual));
  assertEquals("trailerEquals(..) for payEqual", false, givenPkt.trailerEquals(payEqual));
  assertEquals("trailerEquals(..) for trlEqual", true , givenPkt.trailerEquals(trlEqual));

  // ==== TRAILER EXACT -VS- INEXACT =========================================
  char b0[8] = { 0x04, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00 };
  char b1[8] = { 0x04, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00 };
  char b2[8] = { 0x04, 0x00, 0x00, 0x02, 0x00, 0x00, 0x0F, 0x00 }; // indicators set but not enabled
  char b3[4] = { 0x00, 0x00, 0x00, 0x01                         }; // no payload

  BasicVRTPacket p0(b0, 8);  // Initial packet
  BasicVRTPacket p1(b1, 8);  // Trailer with exact equality
  BasicVRTPacket p2(b2, 8);  // Trailer with inexact equality
  BasicVRTPacket p3(b3, 4);  // Trailer with inexact equality

  assertEquals("trailerEquals(p1)",       true,  p0.trailerEquals(p1      ));
  assertEquals("trailerEquals(p2)",       false, p0.trailerEquals(p2      ));
  assertEquals("trailerEquals(p3)",       false, p0.trailerEquals(p3      ));
  assertEquals("trailerEquals(p1,false)", true,  p0.trailerEquals(p1,false));
  assertEquals("trailerEquals(p2,false)", true,  p0.trailerEquals(p2,false));
  assertEquals("trailerEquals(p3,false)", true,  p0.trailerEquals(p3,false));
  assertEquals("trailerEquals(p1,true)",  true,  p0.trailerEquals(p1,true ));
  assertEquals("trailerEquals(p2,true)",  false, p0.trailerEquals(p2,true ));
  assertEquals("trailerEquals(p3,true)",  false, p0.trailerEquals(p3,true ));

  // ==== NULL PACKETS =======================================================
  // See bug VRT-25 for details on why we need to check this and why we need
  // the BasicDataPacket and BasicContextPacket checks included.
  BasicVRTPacket     nullPkt  = BasicVRTPacket::NULL_PACKET;
  BasicDataPacket    nullData = BasicVRTPacket::NULL_PACKET;
  BasicContextPacket nullCtx  = BasicVRTPacket::NULL_PACKET;
  assertEquals("equals(..) for null packet",             false, givenPkt.equals(BasicVRTPacket::NULL_PACKET));
  assertEquals("equals(..) for null BasicVRTPacket",     true,  nullPkt.equals(BasicVRTPacket::NULL_PACKET));
  assertEquals("equals(..) for null BasicDataPacket",    true,  nullData.equals(BasicVRTPacket::NULL_PACKET));
  assertEquals("equals(..) for null BasicContextPacket", true,  nullCtx.equals(BasicVRTPacket::NULL_PACKET));
}

void BasicVRTPacketTest::testGetClassID () {
  BasicVRTPacket p1;
  BasicVRTPacket p2;
  BasicVRTPacket p3;
  BasicVRTPacket p4;
  BasicVRTPacket p5;
  p1.setStreamIdentifier(42);
  p2.setStreamIdentifier(42);
  p3.setStreamIdentifier(42);
  p4.setStreamIdentifier(42);
  p5.setStreamIdentifier(42);
  p1.setClassID("AB-CD-EF:1234.5678");
  p2.setClassIdentifier(__INT64_C(0xABCDEF12345678));
  p3.setClassIdentifier(0xABCDEF, (int16_t)0x1234, (int16_t)0x5678);
  p4.setClassID("");
  p5.setClassIdentifier(INT64_NULL);

  char  exp[8]  = { (char)0x00, (char)0xAB, (char)0xCD, (char)0xEF,
                    (char)0x12, (char)0x34, (char)0x56, (char)0x78 };
  char* act1 = (char*)p1.getPacketPointer();
  char* act2 = (char*)p2.getPacketPointer();
  char* act3 = (char*)p3.getPacketPointer();

  assertBufEquals("setClassID(String)",                  exp, 8, &act1[8], 8);
  assertBufEquals("setClassIdentifier(long)",            exp, 8, &act2[8], 8);
  assertBufEquals("setClassIdentifier(int,short,short)", exp, 8, &act3[8], 8);

  assertEquals("p1.getClassID()",            "AB-CD-EF:1234.5678",        p1.getClassID());
  assertEquals("p1.getClassIdentifier()",    __INT64_C(0xABCDEF12345678), p1.getClassIdentifier());
  assertEquals("p1.getClassIdentifierOUI()",        0xABCDEF,             p1.getClassIdentifierOUI());
  assertEquals("p1.getClassIdentifierICC()", (int16_t)0x1234,             p1.getClassIdentifierICC());
  assertEquals("p1.getClassIdentifierPCC()", (int16_t)0x5678,             p1.getClassIdentifierPCC());

  assertEquals("p2.getClassID()",            "AB-CD-EF:1234.5678",        p2.getClassID());
  assertEquals("p2.getClassIdentifier()",    __INT64_C(0xABCDEF12345678), p2.getClassIdentifier());
  assertEquals("p2.getClassIdentifierOUI()",        0xABCDEF,             p2.getClassIdentifierOUI());
  assertEquals("p2.getClassIdentifierICC()", (int16_t)0x1234,             p2.getClassIdentifierICC());
  assertEquals("p2.getClassIdentifierPCC()", (int16_t)0x5678,             p2.getClassIdentifierPCC());

  assertEquals("p3.getClassID()",            "AB-CD-EF:1234.5678",        p3.getClassID());
  assertEquals("p3.getClassIdentifier()",    __INT64_C(0xABCDEF12345678), p3.getClassIdentifier());
  assertEquals("p3.getClassIdentifierOUI()",        0xABCDEF,             p3.getClassIdentifierOUI());
  assertEquals("p3.getClassIdentifierICC()", (int16_t)0x1234,             p3.getClassIdentifierICC());
  assertEquals("p3.getClassIdentifierPCC()", (int16_t)0x5678,             p3.getClassIdentifierPCC());

  assertEquals("p4.getClassID()",            "",         p4.getClassID());
  assertEquals("p4.getClassIdentifier()",    INT64_NULL, p4.getClassIdentifier());
  assertEquals("p4.getClassIdentifierOUI()", INT32_NULL, p4.getClassIdentifierOUI());
  assertEquals("p4.getClassIdentifierICC()", INT16_NULL, p4.getClassIdentifierICC());
  assertEquals("p4.getClassIdentifierPCC()", INT16_NULL, p4.getClassIdentifierPCC());

  assertEquals("p5.getClassID()",            "",         p5.getClassID());
  assertEquals("p5.getClassIdentifier()",    INT64_NULL, p5.getClassIdentifier());
  assertEquals("p5.getClassIdentifierOUI()", INT32_NULL, p5.getClassIdentifierOUI());
  assertEquals("p5.getClassIdentifierICC()", INT16_NULL, p5.getClassIdentifierICC());
  assertEquals("p5.getClassIdentifierPCC()", INT16_NULL, p5.getClassIdentifierPCC());

  // ==== Test Case Added for VRT-30 ===========================================
  vector<char> buff(72);
  buff[0]=0x41;
  buff[1]=0xe0;
  buff[2]=0x00;
  buff[3]=0x12;
  for (int i=4;i<9;++i) buff[i]=0x00;
  buff[9]=0x7;
  buff[10]=0xeb;
  buff[11]=0x8c;
  buff[12]=0;
  buff[13]=0;
  buff[14]=0;
  buff[15]=0x61;
  buff[16]=0xa1;
  buff[17]=0x93;
  buff[18]=0x68;
  buff[19]=0x60;
  buff[20]=0x1c;
  buff[21]=0xe0;
  buff[22]=0x80;
  for (int i=23;i<34;++i) buff[i]=0;
  buff[34]=98;
  buff[35]=96;
  buff[36]=80;
  for (int i=37;i<48;++i) buff[i]=0;
  buff[48]=0xfc;
  buff[49]=0xc0;
  for (int i=50;i<58;++i) buff[i]=0;
  buff[58]=0x17;
  buff[59]=0xd7;
  buff[60]=0x84;
  buff[61]=0;
  buff[62]=0;
  buff[63]=0;
  buff[64]=0xa0;
  buff[65]=0;
  buff[66]=0x3;
  buff[67]=0xcf;
  buff[68]=0;
  buff[69]=0;
  buff[70]=0;
  buff[71]=0;

  BasicContextPacket *context = new BasicContextPacket(buff,false);
  assertEquals("context.getClassID()",            "",         context->getClassID());
  assertEquals("context.getClassIdentifier()",    INT64_NULL, context->getClassIdentifier());
  assertEquals("context.getClassIdentifierOUI()", INT32_NULL, context->getClassIdentifierOUI());
  assertEquals("context.getClassIdentifierICC()", INT16_NULL, context->getClassIdentifierICC());
  assertEquals("context.getClassIdentifierPCC()", INT16_NULL, context->getClassIdentifierPCC());
  delete context;
}

void BasicVRTPacketTest::testGetPacket () {
  char buf[12] = { 0x04,0x00,0x00,0x03, 0x12,0x34,0x56,0x78, 0x00,0x00,0x00,0x00 };

  BasicVRTPacket p(buf, 12);

  vector<char> pkt = p.getPacket();
  vector<char> pay = p.getPayload();

  assertBufEquals("p.getPacketPointer()",  &buf[0], 12, p.getPacketPointer(),  p.getPacketLength());
  assertBufEquals("p.getPayloadPointer()", &buf[4],  4, p.getPayloadPointer(), p.getPayloadLength());
  assertBufEquals("p.getPacket()",         &buf[0], 12, &pkt[0],               pkt.size());
  assertBufEquals("p.getPayload()",        &buf[4],  4, &pay[0],               pay.size());
}

void BasicVRTPacketTest::testGetPacketCount () {
  BasicVRTPacket p;
  p.setPacketCount(0x4); // just some non-zero number

  for (int32_t i = 0x0; i <= 0xF; i++) {
    p.setPacketCount(i);

    char* buf = (char*)p.getPacketPointer();
    assertEquals("setPacketCount(..)", i, buf[1] & 0x0F);
    assertEquals("getPacketCount()",   i, p.getPacketCount());
  }
}

void BasicVRTPacketTest::testGetPacketLength () {
  char hdr0[BasicVRTPacket::MAX_HEADER_LENGTH];
  char hdr1[BasicVRTPacket::MAX_HEADER_LENGTH];
  char data1[4] = { 0x12, 0x34, 0x56, 0x78 };
  char data2[8] = { 0x12, 0x34, 0x56, 0x78, 0x0A, 0x0B, 0x0C, 0x0D };

  memset(hdr0, 0, BasicVRTPacket::MAX_HEADER_LENGTH);
  memset(hdr1, 0, BasicVRTPacket::MAX_HEADER_LENGTH);

  hdr0[3] = 0x01;
  hdr1[0] = 0x10;
  hdr1[3] = 0x02;

  // ==== NO STREAM IDENTIFIER ===============================================
  BasicVRTPacket p0(hdr0, BasicVRTPacket::MAX_HEADER_LENGTH);

  assertEquals("p.getHeaderLength()",  4, p0.getHeaderLength());
  assertEquals("p.getPacketLength()",  4, p0.getPacketLength());
  p0.setClassID("AB-CD-EF:1234.5678");
  assertEquals("p.getHeaderLength()", 12, p0.getHeaderLength());
  assertEquals("p.getPacketLength()", 12, p0.getPacketLength());
  p0.setTimeStamp(TimeStamp::getSystemTime());
  assertEquals("p.getHeaderLength()", 24, p0.getHeaderLength());
  assertEquals("p.getPacketLength()", 24, p0.getPacketLength());

  p0.setClassID("");
  assertEquals("p.getHeaderLength()", 16, p0.getHeaderLength());
  assertEquals("p.getPacketLength()", 16, p0.getPacketLength());
  p0.setTimeStamp(TimeStamp());
  assertEquals("p.getHeaderLength()",  4, p0.getHeaderLength());
  assertEquals("p.getPacketLength()",  4, p0.getPacketLength());

  // ==== WITH STREAM IDENTIFIER =============================================
  BasicVRTPacket p1(hdr1, BasicVRTPacket::MAX_HEADER_LENGTH);
  p1.setStreamIdentifier(123);

  assertEquals("p.getHeaderLength()",  8, p1.getHeaderLength());
  assertEquals("p.getPacketLength()",  8, p1.getPacketLength());
  p1.setClassID("AB-CD-EF:1234.5678");
  assertEquals("p.getHeaderLength()", 16, p1.getHeaderLength());
  assertEquals("p.getPacketLength()", 16, p1.getPacketLength());
  p1.setTimeStamp(TimeStamp::getSystemTime());
  assertEquals("p.getHeaderLength()", 28, p1.getHeaderLength());
  assertEquals("p.getPacketLength()", 28, p1.getPacketLength());

  p1.setClassID("");
  assertEquals("p.getHeaderLength()", 20, p1.getHeaderLength());
  assertEquals("p.getPacketLength()", 20, p1.getPacketLength());
  p1.setTimeStamp(TimeStamp());
  assertEquals("p.getHeaderLength()",  8, p1.getHeaderLength());
  assertEquals("p.getPacketLength()",  8, p1.getPacketLength());

  // ==== PAYLOAD/TRAILER LENGTH =============================================
  BasicDataPacket p2(hdr1, BasicVRTPacket::MAX_HEADER_LENGTH);
  p2.setStreamIdentifier(123);
  p2.setClassID("AB-CD-EF:1234.5678");
  p2.setTimeStamp(TimeStamp::getSystemTime());

  assertEquals("p.getPacketLength()",  28, p2.getPacketLength());
  assertEquals("p.getHeaderLength()",  28, p2.getHeaderLength());
  assertEquals("p.getPayloadLength()",  0, p2.getPayloadLength());
  assertEquals("p.getTrailerLength()",  0, p2.getTrailerLength());

  p2.setPayload(data1, 4);
  assertEquals("p.getPacketLength()",  28+4, p2.getPacketLength());
  assertEquals("p.getHeaderLength()",  28,   p2.getHeaderLength());
  assertEquals("p.getPayloadLength()",    4, p2.getPayloadLength());
  assertEquals("p.getTrailerLength()",  0,   p2.getTrailerLength());


  p2.setCalibratedTimeStamp(_TRUE);
  assertEquals("p.getPacketLength()",  28+4+4, p2.getPacketLength());
  assertEquals("p.getHeaderLength()",  28,     p2.getHeaderLength());
  assertEquals("p.getPayloadLength()",    4,   p2.getPayloadLength());
  assertEquals("p.getTrailerLength()",  4,     p2.getTrailerLength());

  p2.setPayload(data2, 8);
  assertEquals("p.getPacketLength()",  28+8+4, p2.getPacketLength());
  assertEquals("p.getHeaderLength()",  28,     p2.getHeaderLength());
  assertEquals("p.getPayloadLength()",    8,   p2.getPayloadLength());
  assertEquals("p.getTrailerLength()",  4,     p2.getTrailerLength());

  p2.setPayloadLength(8+4);
  assertEquals("p.getPacketLength()",  28+8+4+4, p2.getPacketLength());
  assertEquals("p.getHeaderLength()",  28,       p2.getHeaderLength());
  assertEquals("p.getPayloadLength()",    8+4,   p2.getPayloadLength());
  assertEquals("p.getTrailerLength()",  4,       p2.getTrailerLength());

  char expData[12] = { 0x12, 0x34, 0x56, 0x78, 0x0A, 0x0B, 0x0C, 0x0D, 0,0,0,0 };
  assertBufEquals("p.setPayloadLength(..)", expData, 12, p2.getPayloadPointer(), p2.getPayloadLength());
}

void BasicVRTPacketTest::testGetPacketType () {
  BasicVRTPacket p;

  for (int32_t i = 0; i <= 5; i++) {
    PacketType t = (PacketType)i;
    p.setPacketType(t);
    char*   pkt = (char*)p.getPacketPointer();
    int32_t exp = i;
    int32_t act = (pkt[0] >> 4) & 0x0F;

    assertEquals("setPacketType(..)", exp, act);
    assertEquals("getPacketType()",   t,   p.getPacketType());
  }
}

/** Checks packet validity for testGetPacketValid(). */
static void checkPacketValid (BasicVRTPacket &p, bool isValid, bool isSemiValid) {
  int32_t len = p.getPacketLength();

  assertEquals("getPacketValid(false)",      isSemiValid, (p.getPacketValid(false        ) == ""));
  assertEquals("getPacketValid(true )",      isValid,     (p.getPacketValid(true         ) == ""));
  assertEquals("isPacketValid()",            isValid,      p.isPacketValid()                     );

  assertEquals("getPacketValid(false,len)", isSemiValid, (p.getPacketValid(false,len) == ""));
  assertEquals("getPacketValid(true, len)", isValid,     (p.getPacketValid(true ,len) == ""));
  assertEquals("getPacketValid(false,len)", isSemiValid, (p.getPacketValid(false,len) == ""));
  assertEquals("getPacketValid(true, len)", isValid,     (p.getPacketValid(true ,len) == ""));
  assertEquals("isPacketValid(len)",        isValid,      p.isPacketValid(len)              );

  // Try sizes too large
  for (int32_t i=1,n=len+1; i < 16; i++,n++) {
    assertEquals("getPacketValid(false,n)", false, (p.getPacketValid(false,n) == ""));
    assertEquals("getPacketValid(true, n)", false, (p.getPacketValid(true ,n) == ""));
    assertEquals("getPacketValid(false,n)", false, (p.getPacketValid(false,n) == ""));
    assertEquals("getPacketValid(true, n)", false, (p.getPacketValid(true ,n) == ""));
    assertEquals("isPacketValid(n)",        false,  p.isPacketValid(n)              );
  }

  // Try sizes too small
  for (int32_t i=1,n=len-1; (i < 16) && (n >= 0); i++,n--) {
    assertEquals("getPacketValid(false,n)", false, (p.getPacketValid(false,n) == ""));
    assertEquals("getPacketValid(true, n)", false, (p.getPacketValid(true ,n) == ""));
    assertEquals("getPacketValid(false,n)", false, (p.getPacketValid(false,n) == ""));
    assertEquals("getPacketValid(true, n)", false, (p.getPacketValid(true ,n) == ""));
    assertEquals("isPacketValid(n)",        false,  p.isPacketValid(n)              );
  }
}

void BasicVRTPacketTest::testGetPacketValid () {
  BasicVRTPacket p;
  checkPacketValid(p, true, true);

  char hdr0[BasicVRTPacket::MAX_HEADER_LENGTH];
  char hdr1[BasicVRTPacket::MAX_HEADER_LENGTH];
  char hdr3[4]  = { 0x03, 0x00, 0x00, 0x01 }; // <-- semi-valid (reserved bits set)
  char hdr4[4]  = { 0x03, 0x60, 0x00, 0x01 }; // <-- invalid    (missing time stamp)

  memset(hdr0, 0, BasicVRTPacket::MAX_HEADER_LENGTH);
  memset(hdr1, 0, BasicVRTPacket::MAX_HEADER_LENGTH);

  hdr0[3] = 0x01;
  hdr1[0] = 0x10;
  hdr1[3] = 0x02;

  BasicVRTPacket p0(hdr0, BasicVRTPacket::MAX_HEADER_LENGTH);
  BasicVRTPacket p1(hdr1, BasicVRTPacket::MAX_HEADER_LENGTH);
  BasicVRTPacket p3(hdr3, 4);
  BasicVRTPacket p4(hdr4, 4);

  checkPacketValid(p0, true,  true );
  checkPacketValid(p1, true,  true );
  checkPacketValid(p3, false, true );
  checkPacketValid(p4, false, false);
}

void BasicVRTPacketTest::testGetPadBitCount () {
  BasicVRTPacket p;
  p.setStreamIdentifier(123);
  p.setClassID("AB-CD-EF:1234.5678");

  // ==== BASIC TESTS ========================================================
  // These are not valid under strict V49 (only under V49b), so expect an error
  // if trying to set it
  if (VRTConfig::getVRTVersion() == VRTConfig::VITAVersion_V49) {
    for (int32_t i = 0; i < 32; i++) {
      try {
        p.setPadBitCount(i, 1);
        assertEquals("setPadBitCount(..) fails under V49", true, false);
      }
      catch (VRTException e) {
        UNUSED_VARIABLE(e);
        assertEquals("setPadBitCount(..) fails under V49", true, true);
      }

      // Now force it in so we can try reading it
      char* pkt = (char*)p.getPacketPointer();
      pkt[8] = (char)(i << 3);
      BasicVRTPacket p1(pkt, p.getPacketLength());
      assertEquals("getPadBitCount()", i, p1.getPadBitCount());
    }
  }
  else {
    for (int32_t i = 0; i < 32; i++) {
      p.setPadBitCount(i, 1);

      char*   packet = (char*)p.getPacketPointer();
      int32_t act    = (packet[8] >> 3) & 0x1F;

      assertEquals("setPadBitCount(..)", i, act);
      assertEquals("getPadBitCount()",   i, p.getPadBitCount());
    }
  }

  // ==== MAKE SURE CHANGES TO CLASS ID DON'T DELETE IT ======================
  // Manually set PadBitCount so that it works under strict V49 mode too
  char* pkt = (char*)p.getPacketPointer();
  pkt[8] = (char)(4 << 3); // <-- 4 pad bits
  BasicVRTPacket p2(pkt, p.getPacketLength());

  p2.setClassID("12-34-56:ABCD.1234");
  assertEquals("getPadBitCount() after setClassID(..)", 4, p2.getPadBitCount());

  // ==== SPECIAL CASE: bits=0 ===============================================
  p.setPadBitCount(0, 24);
  char* packet = (char*)p.getPacketPointer();
  assertEquals("setPadBitCount(..) when bits=0", (char)0, packet[8]);
  assertEquals("getPadBitCount()   when bits=0",       0, p.getPadBitCount());

  // ==== SPECIAL CASE: bits=<implicit> ======================================
  p.setPadBitCount(4, 24);

  char*   packet2 = (char*)p.getPacketPointer();
  int32_t padBits = (packet2[8] >> 3) & 0x1F;

  assertEquals("setPadBitCount(..) when bits=<implicit>", 0, padBits);
  assertEquals("getPadBitCount()   when bits=<implicit>", 0, p.getPadBitCount());
}

void BasicVRTPacketTest::testGetStreamCode () {
  BasicVRTPacket p;

  for (int32_t j = 0; j <= 5; j++) {
    PacketType t = (PacketType)j;
    p.setPacketType(t);

    if (hasStreamIdentifier(t)) {
      for (int64_t i = 0; i < __INT64_C(0xFFFFFFFF); i+= __INT64_C(0x11111111)) {
        p.setStreamIdentifier((int)i);

        int64_t exp = (((int64_t)j) << 60) | i;
        int64_t act = p.getStreamCode();

        assertEquals("getStreamCode()", exp, act);
      }
    }
    else {
      // If no stream identifier use 0x00000000 in place of it
      int64_t exp = (((int64_t)j) << 60);
      int64_t act = p.getStreamCode();

      assertEquals("getStreamCode()", exp, act);
    }
  }
}

void BasicVRTPacketTest::testGetStreamID () {
  BasicVRTPacket p1;
  BasicVRTPacket p2;

  for (int32_t j = 0; j <= 5; j++) {
    PacketType t = (PacketType)j;
    p1.setPacketType(t);
    p2.setPacketType(t);

    if (hasStreamIdentifier(t)) {
      for (int64_t i = 0; i < __INT64_C(0xFFFFFFFF); i+= __INT64_C(0x11111111)) {
        int32_t exp = (int32_t)j;
        char    str[8]; snprintf(str, 8, "%d", exp);
        char    sid[4]; VRTMath::packInt(sid, 0, exp, BIG_ENDIAN);

        p1.setStreamID(str);
        p2.setStreamIdentifier(exp);

        char* pkt1 = (char*)p1.getPacketPointer();
        char* pkt2 = (char*)p2.getPacketPointer();

        assertBufEquals("setStreamID(..)",         sid, 4, &pkt1[4], 4);
        assertBufEquals("setStreamIdentifier(..)", sid, 4, &pkt2[4], 4);

        assertEquals("getStreamID()",         str, p1.getStreamID());
        assertEquals("getStreamIdentifier()", exp, p1.getStreamIdentifier());
      }
    }
    else {
      try {
        p1.setStreamID("123");
        assertEquals("setStreamID(..) fails when none permitted", true, false);
      }
      catch (VRTException e) {
        UNUSED_VARIABLE(e);
        assertEquals("setStreamID(..) fails when none permitted", true, true);
      }
      try {
        p1.setStreamIdentifier(123);
        assertEquals("setStreamIdentifier(..) fails when none permitted", true, false);
      }
      catch (VRTException e) {
        UNUSED_VARIABLE(e);
        assertEquals("setStreamIdentifier(..) fails when none permitted", true, true);
      }

      assertEquals("getStreamIdentifier()", "",         p1.getStreamID());
      assertEquals("getStreamIdentifier()", INT32_NULL, p1.getStreamIdentifier());
    }
  }
}

/** Utility time stamp tester used by testGetTimeStamp(..). */
static void testTimeStamp (BasicVRTPacket &p, IntegerMode tsiMode, FractionalMode tsfMode,
                           int64_t tsi, int64_t tsf) {
  TimeStamp useTS;
  TimeStamp expTS;

  if (isNull(tsi) && isNull(tsf)) {
    expTS = TimeStamp::NO_TIME_STAMP;
  }
  else {
    useTS = TimeStamp(tsiMode, tsfMode, (uint32_t)tsi, tsf);
    expTS = useTS;
  }

  p.setTimeStamp(useTS);

  char*   pkt        = (char*)p.getPacketPointer();
  int32_t expTSIMode = (int)tsiMode;
  int32_t expTSFMode = (int)tsfMode;
  int32_t actTSIMode = (pkt[1] >> 6) & 0x03;
  int32_t actTSFMode = (pkt[1] >> 4) & 0x03;
  int32_t expTSI     = (isNull(tsi))? INT32_NULL : (int32_t)tsi;
  int64_t expTSF     = tsf;
  int32_t actTSI     = INT32_NULL;
  int64_t actTSF     = INT64_NULL;

  int32_t off = 4;
  if (p.getStreamIdentifier() != INT32_NULL) off+=4;
  if (p.getClassIdentifier()  != INT64_NULL) off+=8;

  if (actTSIMode != 0) {
    actTSI = VRTMath::unpackInt(pkt, off, BIG_ENDIAN);
    off+=4;
  }
  if (actTSFMode != 0) {
    actTSF = VRTMath::unpackLong(pkt, off, BIG_ENDIAN);
  }

  assertEquals("setTimeStamp(..) TSI Mode",  expTSIMode, actTSIMode);
  assertEquals("setTimeStamp(..) TSF Mode",  expTSFMode, actTSFMode);
  assertEquals("setTimeStamp(..) TSI Value", expTSI,     actTSI);
  assertEquals("setTimeStamp(..) TSF Value", expTSF,     actTSF);
  assertEquals("getTimeStamp()",             expTS,      p.getTimeStamp());
}

void BasicVRTPacketTest::testGetTimeStamp () {
  const int64_t y2k_utc = TimeStamp::Y2K_GPS.getUTCSeconds();
  const int64_t y2k_gps = TimeStamp::Y2K_GPS.getGPSSeconds();

  BasicVRTPacket p;

  testTimeStamp(p, IntegerMode_None,  FractionalMode_None, INT64_NULL,        INT64_NULL);
  testTimeStamp(p, IntegerMode_UTC,   FractionalMode_None, y2k_utc,           INT64_NULL);
  testTimeStamp(p, IntegerMode_GPS,   FractionalMode_None, y2k_gps,           INT64_NULL);
  testTimeStamp(p, IntegerMode_Other, FractionalMode_None, __INT64_C(123456), INT64_NULL);

  testTimeStamp(p, IntegerMode_None,  FractionalMode_SampleCount, INT64_NULL,        __INT64_C(1));
  testTimeStamp(p, IntegerMode_UTC,   FractionalMode_SampleCount, y2k_utc,           __INT64_C(2));
  testTimeStamp(p, IntegerMode_GPS,   FractionalMode_SampleCount, y2k_gps,           __INT64_C(3));
  testTimeStamp(p, IntegerMode_Other, FractionalMode_SampleCount, __INT64_C(123456), __INT64_C(4));

  testTimeStamp(p, IntegerMode_None,  FractionalMode_RealTime, INT64_NULL,        __INT64_C(1));
  testTimeStamp(p, IntegerMode_UTC,   FractionalMode_RealTime, y2k_utc,           __INT64_C(2));
  testTimeStamp(p, IntegerMode_GPS,   FractionalMode_RealTime, y2k_gps,           __INT64_C(3));
  testTimeStamp(p, IntegerMode_Other, FractionalMode_RealTime, __INT64_C(123456), __INT64_C(4));

  testTimeStamp(p, IntegerMode_None,  FractionalMode_FreeRunningCount, INT64_NULL,        __INT64_C(1));
  testTimeStamp(p, IntegerMode_UTC,   FractionalMode_FreeRunningCount, y2k_utc,           __INT64_C(2));
  testTimeStamp(p, IntegerMode_GPS,   FractionalMode_FreeRunningCount, y2k_gps,           __INT64_C(3));
  testTimeStamp(p, IntegerMode_Other, FractionalMode_FreeRunningCount, __INT64_C(123456), __INT64_C(4));
}

void BasicVRTPacketTest::testIsChangePacket () {
  // Default implementation is simply to return true
  BasicVRTPacket p;
  assertEquals("isChangePacket()", true, p.isChangePacket());
}

void BasicVRTPacketTest::testIsSpectrumMode () {
  const size_t BIT = 24;
  BasicVRTPacket p;
  p.setPacketType(PacketType_Context);
  assertEquals("isSpectrumMode() null (default context)", _NULL, p.isSpectrumMode());
  assertEquals("setPacketType(context) default spectral bit", 0x0, VRTMath::unpackInt(p.getPacketPointer(), 0) & (0x1<<BIT));
  p.setPacketType(PacketType_Data);
  assertEquals("isSpectrumMode() false (default data)", _FALSE, p.isSpectrumMode());
  assertEquals("setPacketType(data) default spectral bit", (int32_t) 0x0, VRTMath::unpackInt(p.getPacketPointer(), 0) & (0x1<<BIT));
  p.setSpectrumMode(true);
  assertEquals("isSpectrumMode() true (data)", _TRUE, p.isSpectrumMode());
  assertEquals("setSpectrumMode(true) bit (data)", 0x1<<BIT, VRTMath::unpackInt(p.getPacketPointer(), 0) & (0x1<<BIT));
  p.setSpectrumMode(false);
  assertEquals("isSpectrumMode() false (data)", _FALSE, p.isSpectrumMode());
  assertEquals("setSpectrumMode(false) bit (data)", 0x0, VRTMath::unpackInt(p.getPacketPointer(), 0) & (0x1<<BIT));
}

void BasicVRTPacketTest::testIsTimeStampMode () {
  // Default implementation is simply to return true
  BasicVRTPacket p0;
  BasicVRTPacket p1;
  BasicVRTPacket p2;

  p0.setPacketType(PacketType_Data);
  p1.setPacketType(PacketType_Context);
  p2.setPacketType(PacketType_Context);

  try {
    p0.setTimeStampMode(true);
    assertEquals("setTimeStampMode(..) fails in data packet", true, false);
  }
  catch (VRTException e) {
    UNUSED_VARIABLE(e);
    assertEquals("setTimeStampMode(..) fails in data packet", true, true);
  }

  p1.setTimeStampMode(false);
  p2.setTimeStampMode(true);

  char* pkt1 = (char*)p1.getPacketPointer();
  char* pkt2 = (char*)p2.getPacketPointer();

  assertEquals("setTimeStampMode(false)", 0x00, (pkt1[0] & 0x01));
  assertEquals("setTimeStampMode(true)",  0x01, (pkt2[0] & 0x01));

  assertEquals("isTimeStampMode()", _NULL,  p0.isTimeStampMode());
  assertEquals("isTimeStampMode()", _FALSE, p1.isTimeStampMode());
  assertEquals("isTimeStampMode()", _TRUE,  p2.isTimeStampMode());
}

void BasicVRTPacketTest::testReadPacket () {
  char buf[12] = { 0x04,0x00,0x00,0x03, 0x12,0x34,0x56,0x78, 0x00,0x00,0x00,0x00 };

  BasicVRTPacket p(buf, 12);

  char*   packet  = (char*)p.getPacketPointer();
  char*   payload = (char*)p.getPayloadPointer();
  char*   buffer[1024];
  int32_t nread;

  // ==== READ FULLY =========================================================
  nread = p.readPacket(buffer, 0, 1024);
  assertBufEquals("p.readPacket(..)", packet, p.getPacketLength(), buffer, nread);

  nread = p.readPayload(buffer, 0, 1024);
  assertBufEquals("p.readPayload(..)", payload, p.getPayloadLength(), buffer, nread);


  // ==== READ PARTIAL =======================================================
  nread = p.readPacket(&buffer[128],3,7);
  assertBufEquals("p.readPacket(..)", &packet[3], 7, &buffer[128], nread);

  nread = p.readPayload(&buffer[512],2,8);
  assertBufEquals("p.readPayload(..)", &payload[2], 2, &buffer[512], nread);
}

void BasicVRTPacketTest::testResetForResend () {
  // Default implementation is simply to set time stamp and then return false
  BasicVRTPacket p;
  TimeStamp      ts = TimeStamp::getSystemTime();

  bool act = p.resetForResend(ts);

  assertEquals("resetForResend() return value",  false, act);
  assertEquals("resetForResend() new TimeStamp", ts,    p.getTimeStamp());
}

void BasicVRTPacketTest::testToString () {
  BasicVRTPacket p1;
  BasicVRTPacket p2 = BasicVRTPacket::NULL_PACKET;
  assertEquals("toString() is empty", false, (p1.toString() == ""));
  assertEquals("toString() for null", true,  (p2.toString().find("<null>") != string::npos));
}

void BasicVRTPacketTest::testWritePayload () {
  char buf[12] = { 0x04,0x00,0x00,0x03, 0x12,0x34,0x56,0x78, 0x00,0x00,0x00,0x00 };
  char exp[12] = { 0x04,0x00,0x00,0x03, 0x12,0x01,0x02,0x78, 0x00,0x00,0x00,0x00 };
  char dat[ 4] = { 0x00,0x01,0x02,0x03 };

  BasicVRTPacket p(buf, 12);

  p.writePayload(&dat[1], 1, 2);

  assertBufEquals("p.writePayload(..)", exp, 12, p.getPacketPointer(), p.getPacketLength());
}

static const int32_t   FIELD_OFFSET   = 0;
static const int32_t   NUM_FIELDS     = 3;
static const char*     FIELD_NAMES[3] = { "StreamID",       "ClassID",        "TimeStamp" };
static const ValueType FIELD_TYPES[3] = { ValueType_String, ValueType_String, ValueType_VRTObject };

void BasicVRTPacketTest::testGetFieldName () {
  BasicVRTPacket hf;
  checkGetFieldName(hf, FIELD_OFFSET, FIELD_NAMES, FIELD_TYPES, NUM_FIELDS);
}

void BasicVRTPacketTest::testGetField () {
  BasicVRTPacket hf;
  TimeStamp      ts1 = TimeStamp::Y2K_GPS;
  TimeStamp      ts2 = TimeStamp::getSystemTime();
  checkGetFieldDef5(hf, StreamID,  string,    "123",                "42");
  checkGetFieldDef5(hf, ClassID,   string,    "AB-CD-EF:1234.5678", "12-34-56:ABCD.1234");
  checkGetFieldObj5(hf, TimeStamp, TimeStamp, ts1,                  ts2);
}



static const size_t   DataType_COUNT      = 13;
static const DataType DataType_VALUES[13] = {
                  DataType_Int4,  DataType_Int8,  DataType_Int16,  DataType_Int32,  DataType_Int64,
  DataType_UInt1, DataType_UInt4, DataType_UInt8, DataType_UInt16, DataType_UInt32, DataType_UInt64,
  DataType_Float, DataType_Double
};

string PayloadFormatTest::getTestedClass () {
  return "vrt::PayloadFormat";
}

vector<TestSet> PayloadFormatTest::init () {
  vector<TestSet> tests;

  tests.push_back(TestSet("equals(..)",                   this,  "testEquals"));
  tests.push_back(TestSet("getBits()",                    this,  "testGetBits"));
  tests.push_back(TestSet("getChannelTagSize()",          this,  "testGetChannelTagSize"));
  tests.push_back(TestSet("getDataItemFormat()",          this,  "testGetDataItemFormat"));
  tests.push_back(TestSet("getDataItemSize()",            this,  "testGetDataItemSize"));
  tests.push_back(TestSet("getDataItemFracSize()",        this,  "testGetDataItemFracSize"));
  tests.push_back(TestSet("getDataType()",                this,  "testGetDataType"));
  tests.push_back(TestSet("getEventTagSize()",            this,  "testGetEventTagSize"));
  tests.push_back(TestSet("getField(..)",                 this,  "testGetField"));
  tests.push_back(TestSet("getFieldByName(..)",           this, "+testGetField"));
  tests.push_back(TestSet("getFieldCount()",              this, "+testGetFieldName"));
  tests.push_back(TestSet("getFieldID(..)",               this, "+testGetFieldName"));
  tests.push_back(TestSet("getFieldName(..)",             this,  "testGetFieldName"));
  tests.push_back(TestSet("getFieldType(..)",             this, "+testGetFieldName"));
  tests.push_back(TestSet("getItemPackingFieldSize()",    this,  "testGetItemPackingFieldSize"));
  tests.push_back(TestSet("getRealComplexType()",         this,  "testGetRealComplexType"));
  tests.push_back(TestSet("getRepeatCount()",             this,  "testGetRepeatCount"));
  tests.push_back(TestSet("getValid()",                   this,  "testGetValid"));
  tests.push_back(TestSet("getVectorSize()",              this,  "testGetVectorSize"));
  tests.push_back(TestSet("isComplex()",                  this, "+testGetRealComplexType"));
  tests.push_back(TestSet("isProcessingEfficient()",      this,  "testIsProcessingEfficient"));
  tests.push_back(TestSet("isRepeating()",                this,  "testIsRepeating"));
  tests.push_back(TestSet("isSigned()",                   this,  "testIsSigned"));
  tests.push_back(TestSet("isValid()",                    this, "+testGetValid"));
  tests.push_back(TestSet("setBits(..)",                  this, "+testGetBits"));
  tests.push_back(TestSet("setChannelTagSize(..)",        this, "+testGetChannelTagSize"));
  tests.push_back(TestSet("setDataItemFormat(..)",        this, "+testGetDataItemFormat"));
  tests.push_back(TestSet("setDataItemSize(..)",          this, "+testGetDataItemSize"));
  tests.push_back(TestSet("setDataItemFracSize(..)",      this, "+testGetDataItemFracSize"));
  tests.push_back(TestSet("setDataType(..)",              this, "+testGetDataType"));
  tests.push_back(TestSet("setEventTagSize(..)",          this, "+testGetEventTagSize"));
  tests.push_back(TestSet("setField(..)",                 this, "+testGetField"));
  tests.push_back(TestSet("setFieldByName(..)",           this, "+testGetField"));
  tests.push_back(TestSet("setItemPackingFieldSize(..)",  this, "+testGetItemPackingFieldSize"));
  tests.push_back(TestSet("setProcessingEfficient(..)",   this, "+testIsProcessingEfficient"));
  tests.push_back(TestSet("setRealComplexType(..)",       this, "+testGetRealComplexType"));
  tests.push_back(TestSet("setRepeatCount(..)",           this, "+testGetRepeatCount"));
  tests.push_back(TestSet("setRepeating(..)",             this, "+testIsRepeating"));
  tests.push_back(TestSet("setVectorSize(..)",            this, "+testGetVectorSize"));
  tests.push_back(TestSet("toString()",                   this,  "testToString"));

  return tests;
}

void PayloadFormatTest::done () {
  // nothing to do
}

void PayloadFormatTest::call (const string &func) {
  if (func == "testEquals"                 ) testEquals();
  if (func == "testGetBits"                ) testGetBits();
  if (func == "testGetChannelTagSize"      ) testGetChannelTagSize();
  if (func == "testGetDataItemFormat"      ) testGetDataItemFormat();
  if (func == "testGetDataItemSize"        ) testGetDataItemSize();
  if (func == "testGetDataItemFracSize"    ) testGetDataItemFracSize();
  if (func == "testGetDataType"            ) testGetDataType();
  if (func == "testGetEventTagSize"        ) testGetEventTagSize();
  if (func == "testGetField"               ) testGetField();
  if (func == "testGetFieldName"           ) testGetFieldName();
  if (func == "testGetItemPackingFieldSize") testGetItemPackingFieldSize();
  if (func == "testGetRealComplexType"     ) testGetRealComplexType();
  if (func == "testGetRepeatCount"         ) testGetRepeatCount();
  if (func == "testGetValid"               ) testGetValid();
  if (func == "testGetVectorSize"          ) testGetVectorSize();
  if (func == "testIsProcessingEfficient"  ) testIsProcessingEfficient();
  if (func == "testIsRepeating"            ) testIsRepeating();
  if (func == "testIsSigned"               ) testIsSigned();
  if (func == "testToString"               ) testToString();
}

void PayloadFormatTest::testEquals () {
  PayloadFormat  pf1(DataType_Int32);
  PayloadFormat  pf2(DataType_Double);
  PayloadFormat  pf3(DataType_Double);
  BasicVRTPacket obj;

  assertEquals("Int32  + Double -> equals(..)", false, pf1.equals(pf2)); // Diff values
  assertEquals("Int32  + Object -> equals(..)", false, pf1.equals(obj)); // Diff class type
  assertEquals("Int32  + Int32  -> equals(..)", true,  pf1.equals(pf1)); // Same object
  assertEquals("Double + Double -> equals(..)", true,  pf2.equals(pf3)); // Diff object




}

void PayloadFormatTest::testGetBits () {
  PayloadFormat pf1(DataType_Int32);
  PayloadFormat pf2(false, RealComplexType_ComplexCartesian,
                    DataItemFormat_UnsignedInt, true, 4, 8, 32,
                    12, 16, 256);
  PayloadFormat pf3(DataType_Int32);
  int64_t       bits1 = __INT64_C(0x000007DF00000000);
  int64_t       bits2 = __INT64_C(0xB0C807CB000F00FF);

  assertEquals("Int32  -> getBits()", bits1, pf1.getBits());
  assertEquals("Double -> getBits()", bits2, pf2.getBits());

  pf3.setBits(bits2);
  assertEquals("setBits(..)", pf2, pf3);
}

void PayloadFormatTest::testGetChannelTagSize () {
  PayloadFormat pf(DataType_Int32);

  assertEquals("getChannelTagSize()", 0, pf.getChannelTagSize());

  int64_t mask = __INT64_C(0x000F000000000000);
  for (int32_t i = 15; i >= 0; i--) {
    string  msg     = Utilities::format("setChannelTagSize(%d)", i);
    int64_t expBits = ((int64_t)i) << 48;

    pf.setChannelTagSize(i);
    assertEquals(msg, i,       pf.getChannelTagSize());
    assertEquals(msg, expBits, pf.getBits() & mask);
  }
}

void PayloadFormatTest::testGetEventTagSize () {
  PayloadFormat pf(DataType_Int32);

  assertEquals("getEventTagSize()", 0, pf.getEventTagSize());

  int64_t mask = __INT64_C(0x0070000000000000);
  for (int32_t i = 7; i >= 0; i--) {
    string  msg     = Utilities::format("setEventTagSize(%d)", i);
    int64_t expBits = ((int64_t)i) << 52;

    pf.setEventTagSize(i);
    assertEquals(msg, i,       pf.getEventTagSize());
    assertEquals(msg, expBits, pf.getBits() & mask);
  }
}

void PayloadFormatTest::testGetDataItemSize () {
  PayloadFormat pf(DataType_Int32);

  assertEquals("getDataItemSize()", 32, pf.getDataItemSize());

  int64_t mask = __INT64_C(0x0000003F00000000);
  for (int32_t i = 1; i <= 64; i++) {
    string  msg     = Utilities::format("setDataItemSize(%d)", i);
    int64_t expBits = ((int64_t)i-1) << 32;

    pf.setDataItemSize(i);
    assertEquals(msg, i,       pf.getDataItemSize());
    assertEquals(msg, expBits, pf.getBits() & mask);
  }
}

void PayloadFormatTest::testGetDataItemFracSize () {

  PayloadFormat pfNorm(DataItemFormat_SignedInt, 32);
  assertEquals("getDataItemFracSize() for SignedInt32", 0, pfNorm.getDataItemFracSize());


  PayloadFormat pfNN(DataItemFormat_SignedIntNN, 32);
  assertEquals("getDataItemFracSize() for SignedIntNN32", 0, pfNN.getDataItemFracSize());

  int64_t mask = __INT64_C(0x0000F00000000000);
  for (int32_t i = 0; i <= 15; i++) {
    string  msg     = Utilities::format("setDataItemFracSize(%d)", i);
    int64_t expBits = ((int64_t)i) << 44;

    pfNN.setDataItemFracSize(i);
    assertEquals(msg, i,       pfNN.getDataItemFracSize());
    assertEquals(msg, expBits, pfNN.getBits() & mask);
  }
}

void PayloadFormatTest::testGetItemPackingFieldSize () {
  PayloadFormat pf(DataType_UInt1);

  assertEquals("getItemPackingFieldSize()", 1, pf.getItemPackingFieldSize());

  int64_t mask = __INT64_C(0x00000FC000000000);
  for (int32_t i = 1; i <= 64; i++) {
    string  msg     = Utilities::format("setItemPackingFieldSize(%d)", i);
    int64_t expBits = ((int64_t)i-1) << 38;

    pf.setItemPackingFieldSize(i);
    assertEquals(msg, i,       pf.getItemPackingFieldSize());
    assertEquals(msg, expBits, pf.getBits() & mask);
  }
}

void PayloadFormatTest::testGetRepeatCount ()  {
  PayloadFormat pf(DataType_Int32);

  assertEquals("getRepeatCount()", 1, pf.getRepeatCount());

  int64_t mask = __INT64_C(0x00000000FFFF0000);
  for (int32_t i = 1; i <= 65536; i*=2) {
    string  msg     = Utilities::format("setRepeatCount(%d)", i);
    int64_t expBits = ((int64_t)i-1) << 16;

    pf.setRepeatCount(i);
    assertEquals(msg, i,       pf.getRepeatCount());
    assertEquals(msg, expBits, pf.getBits() & mask);
  }
}

void PayloadFormatTest::testGetVectorSize ()  {
  PayloadFormat pf(DataType_Int32);

  assertEquals("getVectorSize()", 1, pf.getVectorSize());

  int64_t mask = __INT64_C(0x000000000000FFFF);
  for (int32_t i = 1; i <= 65536; i*=2) {
    string  msg     = Utilities::format("setVectorSize(%d)", i);
    int64_t expBits = ((int64_t)i-1);

    pf.setVectorSize(i);
    assertEquals(msg, i,       pf.getVectorSize());
    assertEquals(msg, expBits, pf.getBits() & mask);
  }
}

void PayloadFormatTest::testGetDataItemFormat () {
  PayloadFormat pf(DataType_Int32);

  assertEquals("getDataItemFormat()", DataItemFormat_SignedInt, pf.getDataItemFormat());

  int64_t mask = __INT64_C(0x1F00000000000000);
  for (int32_t i = 0; i <= 31; i++) {
    DataItemFormat f   = (DataItemFormat)i;
    string         msg = "setDataItemFormat(..)";

    if (((i & 0x0F) <= (int32_t)DataItemFormat_SignedVRT6)
            || (f == DataItemFormat_Float) || (f == DataItemFormat_Double)) {
      int64_t expBits = ((int64_t)i) << 56;

      pf.setDataItemFormat(f);
      assertEquals(msg, f,       pf.getDataItemFormat());
      assertEquals(msg, expBits, pf.getBits() & mask);
    }
    else {
      try {
        pf.setDataItemFormat(f);
        assertEquals(msg+" fails", true, false);
      }
      catch (VRTException e) {
        UNUSED_VARIABLE(e);
        assertEquals(msg+" fails", true, true);
      }
    }
  }
}

void PayloadFormatTest::testGetRealComplexType () {
  PayloadFormat pf(DataType_Int32);

  assertEquals("getRealComplexType()", RealComplexType_Real, pf.getRealComplexType());
  assertEquals("isComplex()",          false,                pf.isComplex());

  int64_t mask = __INT64_C(0x6000000000000000);
  for (int32_t i = 0; i <= 3; i++) {
    RealComplexType t   = (RealComplexType)i;
    string          msg = "setRealComplexType(..)";

    if (i <= 3) {
      int64_t expBits  = ((int64_t)t) << 61;
      bool    expCmplx = (t != RealComplexType_Real);

      pf.setRealComplexType(t);
      assertEquals(msg, t,        pf.getRealComplexType());
      assertEquals(msg, expBits,  pf.getBits() & mask);
      assertEquals(msg, expCmplx, pf.isComplex());
    }
    else {
      try {
        pf.setRealComplexType(t);
        assertEquals(msg+" fails", true, false);
      }
      catch (VRTException e) {
        UNUSED_VARIABLE(e);
        assertEquals(msg+" fails", true, true);
      }
    }
  }
}

void PayloadFormatTest::testGetDataType () {
  PayloadFormat pf1(DataType_Int32);

  for (size_t i = 0; i < DataType_COUNT; i++) {
    DataType dt = DataType_VALUES[i];
    if (true) {
      PayloadFormat pf2(dt);

      pf1.setDataType(dt);
      assertEquals("getDataType()",   (int)dt, (int)pf2.getDataType());
      assertEquals("setDataType(..)", pf2, pf1);
    }
//    else {
//      try {
//        pf1.setDataType(dt);
//        assertEquals("PayloadFormat(..) fails", true, false);
//      }
//      catch (VRTException e) {
//        UNUSED_VARIABLE(e);
//        assertEquals("PayloadFormat(..) fails", true, true);
//      }
//      try {
//        PayloadFormat(dt);
//        assertEquals("setDataType(..) fails", true, false);
//      }
//      catch (VRTException e) {
//        UNUSED_VARIABLE(e);
//        assertEquals("setDataType(..) fails", true, true);
//      }
//    }
  }
}

/** Used by testGetValid() to check invalid combinations. */
static void testInvalidType (DataItemFormat format, int32_t dSize, int32_t fSize) {
  string msg = Utilities::format("Attempting to create PayloadFormat with format=%s "
                                 "dSize=%d fSize=%d fails", "f", dSize, fSize);
  try {
    PayloadFormat(true, RealComplexType_Real, format, false, 0, 0, fSize, dSize, 1, 1);
    assertEquals(msg, true, false);
  }
  catch (VRTException e) {
    UNUSED_VARIABLE(e);
    assertEquals(msg, true, true);
  }
}

void PayloadFormatTest::testGetValid () {
  // ==== CHECK VALID TYPES ================================================
  for (size_t i = 0; i < DataType_COUNT; i++) {
    DataType dt = DataType_VALUES[i];
    if (true) {
      PayloadFormat pf(dt);
      assertEquals("getValid()", "",   pf.getValid());
      assertEquals("isValid()",  true, pf.isValid());
    }
  }

  // ==== CHECK INVALID TYPES ==============================================
  testInvalidType(DataItemFormat_SignedInt,    64, 32);  // fSize  <  dSize
  testInvalidType(DataItemFormat_Float,        16, 64);  // Float  != 32-bits
  testInvalidType(DataItemFormat_Double,       16, 64);  // Double != 64-bits
  testInvalidType(DataItemFormat_SignedVRT1,    1, 64);  // VRT1   <   2-bits
  testInvalidType(DataItemFormat_SignedVRT2,    2, 64);  // VRT2   <   3-bits
  testInvalidType(DataItemFormat_SignedVRT3,    3, 64);  // VRT3   <   4-bits
  testInvalidType(DataItemFormat_SignedVRT4,    4, 64);  // VRT4   <   5-bits
  testInvalidType(DataItemFormat_SignedVRT5,    5, 64);  // VRT5   <   6-bits
  testInvalidType(DataItemFormat_SignedVRT6,    6, 64);  // VRT6   <   7-bits
  testInvalidType(DataItemFormat_UnsignedVRT1,  1, 64);  // VRT1   <   2-bits
  testInvalidType(DataItemFormat_UnsignedVRT2,  2, 64);  // VRT2   <   3-bits
  testInvalidType(DataItemFormat_UnsignedVRT3,  3, 64);  // VRT3   <   4-bits
  testInvalidType(DataItemFormat_UnsignedVRT4,  4, 64);  // VRT4   <   5-bits
  testInvalidType(DataItemFormat_UnsignedVRT5,  5, 64);  // VRT5   <   6-bits
  testInvalidType(DataItemFormat_UnsignedVRT6,  6, 64);  // VRT6   <   7-bits
}

void PayloadFormatTest::testIsProcessingEfficient () {
  // Note that the bit in the underlying field is LinkEfficient=0/1 which
  // is the reverse of ProcessingEfficient
  PayloadFormat pf(DataType_Int32);

  assertEquals("isProcessingEfficient()", true, pf.isProcessingEfficient());

  pf.setProcessingEfficient(true);
  assertEquals("setProcessingEfficient(true)", true,                          pf.isProcessingEfficient());
  assertEquals("setProcessingEfficient(true)", __INT64_C(0x0000000000000000), pf.getBits() & __INT64_C(0x8000000000000000));

  pf.setProcessingEfficient(false);
  assertEquals("setProcessingEfficient(false)", false,                         pf.isProcessingEfficient());
  assertEquals("setProcessingEfficient(false)", __INT64_C(0x8000000000000000), pf.getBits() & __INT64_C(0x8000000000000000));
}

void PayloadFormatTest::testIsRepeating () {
  PayloadFormat pf(DataType_Int32);

  assertEquals("isRepeating()", false,  pf.isRepeating());

  pf.setRepeating(true);
  assertEquals("setRepeating(true)", true,                          pf.isRepeating());
  assertEquals("setRepeating(true)", __INT64_C(0x0080000000000000), pf.getBits() & __INT64_C(0x0080000000000000));

  pf.setRepeating(false);
  assertEquals("setRepeating(false)", false,                         pf.isRepeating());
  assertEquals("setRepeating(false)", __INT64_C(0x0000000000000000), pf.getBits() & __INT64_C(0x0080000000000000));
}

void PayloadFormatTest::testIsSigned () {
  PayloadFormat pf1(DataType_Int32);
  PayloadFormat pf2(DataType_UInt8);

  assertEquals("Int32 -> isSigned()", true,  pf1.isSigned());
  assertEquals("UInt8 -> isSigned()", false, pf2.isSigned());
}

void PayloadFormatTest::testToString () {
  PayloadFormat pf(DataType_Int32);
  assertEquals("toString() is null",  false, isNull(pf.toString()));
}


static const int32_t   PF_FIELD_OFFSET    = 0;
static const int32_t   PF_NUM_FIELDS      = 12;
static const char*     PF_FIELD_NAMES[12] = {
  "ProcessingEfficient", "RealComplexType", "DataItemFormat",       "Repeating",
  "EventTagSize",        "ChannelTagSize",  "DataItemFracSize",     "ItemPackingFieldSize",
  "DataItemSize",        "RepeatCount",     "VectorSize",           "DataType"
};
static const ValueType PF_FIELD_TYPES[12] = {
  ValueType_Bool,  ValueType_Int8,  ValueType_Int8,  ValueType_Bool,
  ValueType_Int32, ValueType_Int32, ValueType_Int32, ValueType_Int32,
  ValueType_Int32, ValueType_Int32, ValueType_Int32, ValueType_Int64
};

void PayloadFormatTest::testGetFieldName () {
  PayloadFormat hf(DataType_Int32);
  checkGetFieldName(hf, PF_FIELD_OFFSET, PF_FIELD_NAMES, PF_FIELD_TYPES, PF_NUM_FIELDS);
}

void PayloadFormatTest::testGetField () {
  PayloadFormat hf(DataType_Int32);
  checkGetFieldBool(hf, ProcessingEfficient,  bool,            true,                       false);
  checkGetFieldEnum(hf, RealComplexType,      RealComplexType, RealComplexType_Real,       RealComplexType_ComplexCartesian);
  checkGetFieldEnum(hf, DataItemFormat,       DataItemFormat,  DataItemFormat_SignedInt,   DataItemFormat_UnsignedInt);
  checkGetFieldEnum(hf, DataItemFormat,       DataItemFormat,  DataItemFormat_SignedIntNN, DataItemFormat_UnsignedIntNN);
  checkGetFieldBool(hf, Repeating,            bool,            true,                       false);
  checkGetFieldDef5(hf, EventTagSize,         int32_t,         0,                          4);
  checkGetFieldDef5(hf, ChannelTagSize,       int32_t,         0,                          4);
  checkGetFieldDef5(hf, DataItemFracSize,     int32_t,         7,                          15);
  checkGetFieldDef5(hf, ItemPackingFieldSize, int32_t,         32,                         64);
  checkGetFieldDef5(hf, DataItemSize,         int32_t,         32,                         64);
  checkGetFieldDef5(hf, RepeatCount,          int32_t,         1,                          32768);
  checkGetFieldDef5(hf, VectorSize,           int32_t,         1,                          32768);
  checkGetFieldEnum(hf, DataType,             DataType,        DataType_Int16,             DataType_Int64);
}
