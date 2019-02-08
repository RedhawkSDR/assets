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

#include "BasicDataPacketTest.h"
#include "BasicContextPacket.h"
#include "BasicDataPacket.h"
#include "HasFieldsTest.h"
#include "TestRunner.h"
#include "VRTConfig.h"

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

string BasicDataPacketTest::getTestedClass () {
  return "vrt::BasicDataPacket";
}

vector<TestSet> BasicDataPacketTest::init () {
  for (int32_t i = 0x00; i <= 0xFF; i++) {
    input.push_back((char)i);
  }
  for (int32_t i = 0x00; i <= 0xFF; i+=2) {
    outputSwap2.push_back((char)(i+1));
    outputSwap2.push_back((char)( i ));
  }
  for (int32_t i = 0x00; i <= 0xFF; i+=4) {
    outputSwap4.push_back((char)(i+3));
    outputSwap4.push_back((char)(i+2));
    outputSwap4.push_back((char)(i+1));
    outputSwap4.push_back((char)( i ));
  }
  for (int32_t i = 0x00; i <= 0xFF; i+=8) {
    outputSwap8.push_back((char)(i+7));
    outputSwap8.push_back((char)(i+6));
    outputSwap8.push_back((char)(i+5));
    outputSwap8.push_back((char)(i+4));
    outputSwap8.push_back((char)(i+3));
    outputSwap8.push_back((char)(i+2));
    outputSwap8.push_back((char)(i+1));
    outputSwap8.push_back((char)( i ));
  }
                                           formats.push_back(PayloadFormat_UINT1);
  formats.push_back(PayloadFormat_INT4);   formats.push_back(PayloadFormat_UINT4);
  formats.push_back(PayloadFormat_INT8);   formats.push_back(PayloadFormat_UINT8);
  formats.push_back(PayloadFormat_INT16);  formats.push_back(PayloadFormat_UINT16);
  formats.push_back(PayloadFormat_INT32);  formats.push_back(PayloadFormat_UINT32);
  formats.push_back(PayloadFormat_INT64);  formats.push_back(PayloadFormat_UINT64);


  vector<TestSet> tests;

  tests.push_back(TestSet("getAssocPacketCount",            this, "+testSetAssocPacketCount"));
  tests.push_back(TestSet("getDataLength",                  this, "+testSetDataLength"));
  tests.push_back(TestSet("getLostBytes",                   this, "+testGetLostSamples"));
  tests.push_back(TestSet("getLostSamples",                 this,  "testGetLostSamples"));
  tests.push_back(TestSet("getNextTimeStamp",               this,  "testGetNextTimeStamp"));
  tests.push_back(TestSet("getPacketType",                  this, "+testSetPacketType"));
  tests.push_back(TestSet("getPacketValid",                 this, "+")); //same as super method with only error message changed
  tests.push_back(TestSet("getPayloadFormat",               this, "+testSetPayloadFormat"));
  tests.push_back(TestSet("getScalarDataLength",            this, "+genericTest"));
  tests.push_back(TestSet("getScalarDataLength",            this, "+testSetScalarDataLength"));
  tests.push_back(TestSet("isAutomaticGainControl",         this, "+testSetAutomaticGainControl"));
  tests.push_back(TestSet("isBit11",                        this, "+testSetBit11"));
  tests.push_back(TestSet("isBit10",                        this, "+testSetBit10"));
  tests.push_back(TestSet("isBit9",                         this, "+testSetBit9"));
  tests.push_back(TestSet("isBit8",                         this, "+testSetBit8"));
  tests.push_back(TestSet("isCalibratedTimeStamp",          this, "+testSetCalibratedTimeStamp"));
  tests.push_back(TestSet("isDataValid",                    this, "+testSetDataValid"));
  tests.push_back(TestSet("isDiscontinuous",                this, "+testSetDiscontinuous"));
  tests.push_back(TestSet("isDiscontinuious",               this, "+testSetDiscontinuous"));
  tests.push_back(TestSet("isInvertedSpectrum",             this, "+testSetInvertedSpectrum"));
  tests.push_back(TestSet("isOverRange",                    this, "+testSetOverRange"));
  tests.push_back(TestSet("isReferenceLocked",              this, "+testSetReferenceLocked"));
  tests.push_back(TestSet("isSignalDetected",               this, "+testSetSignalDetected"));

  tests.push_back(TestSet("setAssocPacketCount",            this,  "testSetAssocPacketCount"));
  tests.push_back(TestSet("setAutomaticGainControl",        this,  "testSetAutomaticGainControl"));
  tests.push_back(TestSet("setBit11",                       this,  "testSetBit11"));
  tests.push_back(TestSet("setBit10",                       this,  "testSetBit10"));
  tests.push_back(TestSet("setBit9",                        this,  "testSetBit9"));
  tests.push_back(TestSet("setBit8",                        this,  "testSetBit8"));
  tests.push_back(TestSet("setCalibratedTimeStamp",         this,  "testSetCalibratedTimeStamp"));

  if (VRTConfig::getVRTVersion() == VRTConfig::VITAVersion_V49) {
    tests.push_back(TestSet("setDataLength VRT_VERSION=V49",  this,   "testSetDataLength"));
    tests.push_back(TestSet("setDataLength VRT_VERSION=V49b", this,  "-testSetDataLength"));
  }
  else {
    tests.push_back(TestSet("setDataLength VRT_VERSION=V49",  this,  "-testSetDataLength"));
    tests.push_back(TestSet("setDataLength VRT_VERSION=V49b", this,   "testSetDataLength"));
  }

  tests.push_back(TestSet("setDataValid",                   this,  "testSetDataValid"));
  tests.push_back(TestSet("setDiscontinuous",               this,  "testSetDiscontinuous"));
  tests.push_back(TestSet("setDiscontinuious",              this, "+testSetDiscontinuous"));
  tests.push_back(TestSet("setInvertedSpectrum",            this,  "testSetInvertedSpectrum"));
  tests.push_back(TestSet("setOverRange",                   this,  "testSetOverRange"));
  tests.push_back(TestSet("setPacketType",                  this,  "testSetPacketType"));
  tests.push_back(TestSet("setPayloadFormat",               this,  "testSetPayloadFormat"));
  tests.push_back(TestSet("setReferenceLocked",             this,  "testSetReferenceLocked"));
  tests.push_back(TestSet("setScalarDataLength",            this,  "testSetScalarDataLength"));
  tests.push_back(TestSet("setSignalDetected",              this,  "testSetSignalDetected"));

  tests.push_back(TestSet("getData",                        this,  "testGetData"));
  tests.push_back(TestSet("getDataByte",                    this,  "testGetDataByte"));
  tests.push_back(TestSet("getDataDouble",                  this, "+testGetDataByte"));
  tests.push_back(TestSet("getDataFloat",                   this, "+testGetDataByte"));
  tests.push_back(TestSet("getDataInt",                     this, "+testGetDataByte"));
  tests.push_back(TestSet("getDataLong",                    this, "+testGetDataByte"));
  tests.push_back(TestSet("getDataShort",                   this, "+testGetDataByte"));

  tests.push_back(TestSet("setData",                        this,  "testSetData"));
  tests.push_back(TestSet("setDataByte",                    this,  "testSetDataByte"));
  tests.push_back(TestSet("setDataDouble",                  this, "+testSetDataByte"));
  tests.push_back(TestSet("setDataFloat",                   this, "+testSetDataByte"));
  tests.push_back(TestSet("setDataInt",                     this, "+testSetDataByte"));
  tests.push_back(TestSet("setDataLong",                    this, "+testSetDataByte"));
  tests.push_back(TestSet("setDataShort",                   this, "+testSetDataByte"));

  tests.push_back(TestSet("getField(..)",                   this,  "testGetField"));
  tests.push_back(TestSet("getFieldByName(..)",             this, "+testGetField"));
  tests.push_back(TestSet("getFieldCount()",                this, "+testGetFieldName"));
  tests.push_back(TestSet("getFieldID(..)",                 this, "+testGetFieldName"));
  tests.push_back(TestSet("getFieldName(..)",               this,  "testGetFieldName"));
  tests.push_back(TestSet("getFieldType(..)",               this, "+testGetFieldName"));
  tests.push_back(TestSet("setField(..)",                   this, "+testGetField"));
  tests.push_back(TestSet("setFieldByName(..)",             this, "+testGetField"));

  return tests;
}

void BasicDataPacketTest::done () {
  // nothing to do
}

void BasicDataPacketTest::call (const string &func) {
  if (func == "testGetData"                ) testGetData();
  if (func == "testGetDataByte"            ) testGetDataByte();
  if (func == "testGetField"               ) testGetField();
  if (func == "testGetFieldName"           ) testGetFieldName();
  if (func == "testGetLostSamples"         ) testGetLostSamples();
  if (func == "testGetNextTimeStamp"       ) testGetNextTimeStamp();
  if (func == "testSetAssocPacketCount"    ) testSetAssocPacketCount();
  if (func == "testSetAutomaticGainControl") testSetAutomaticGainControl();
  if (func == "testSetBit11"               ) testSetBit11();
  if (func == "testSetBit10"               ) testSetBit10();
  if (func == "testSetBit9"                ) testSetBit9();
  if (func == "testSetBit8"                ) testSetBit8();
  if (func == "testSetCalibratedTimeStamp" ) testSetCalibratedTimeStamp();
  if (func == "testSetData"                ) testSetData();
  if (func == "testSetDataByte"            ) testSetDataByte();
  if (func == "testSetDataLength"          ) testSetDataLength();
  if (func == "testSetDataValid"           ) testSetDataValid();
  if (func == "testSetDiscontinuous"       ) testSetDiscontinuous();
  if (func == "testSetInvertedSpectrum"    ) testSetInvertedSpectrum();
  if (func == "testSetOverRange"           ) testSetOverRange();
  if (func == "testSetPacketType"          ) testSetPacketType();
  if (func == "testSetPayloadFormat"       ) testSetPayloadFormat();
  if (func == "testSetReferenceLocked"     ) testSetReferenceLocked();
  if (func == "testSetScalarDataLength"    ) testSetScalarDataLength();
  if (func == "testSetSignalDetected"      ) testSetSignalDetected();
}


void BasicDataPacketTest::testSetCalibratedTimeStamp () {
  BasicDataPacket p;
  char *buffer;
  p.setCalibratedTimeStamp(_FALSE);
  assertEquals("isCalibratedTimeStamp(false)",_FALSE,p.isCalibratedTimeStamp());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setCalibratedTimeStamp(false)",0x80000000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x80080000);

  p.setCalibratedTimeStamp(_TRUE);
  assertEquals("isCalibratedTimeStamp(true)",_TRUE,p.isCalibratedTimeStamp());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setCalibratedTimeStamp(true)",0x80080000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x80080000);
}

void BasicDataPacketTest::testSetDataValid () {
  BasicDataPacket p;
  char *buffer;
  p.setDataValid(_FALSE);
  assertEquals("isDataValid(false)",_FALSE,p.isDataValid());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setDataValid(false)",0x40000000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x40040000);

  p.setDataValid(_TRUE);
  assertEquals("isDataValid(true)",_TRUE,p.isDataValid());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setDataValid(true)",0x40040000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x40040000);
}

void BasicDataPacketTest::testSetReferenceLocked () {
  BasicDataPacket p;
  char *buffer;
  p.setReferenceLocked(_FALSE);
  assertEquals("isReferenceLocked(false)",_FALSE,p.isReferenceLocked());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setReferenceLocked(false)",0x20000000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x20020000);

  p.setReferenceLocked(_TRUE);
  assertEquals("isReferenceLocked(true)",_TRUE,p.isReferenceLocked());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setReferenceLocked(true)",0x20020000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x20020000);
}

void BasicDataPacketTest::testSetAutomaticGainControl () {
  BasicDataPacket p;
  char *buffer;
  p.setAutomaticGainControl(_FALSE);
  assertEquals("isAutomaticGainControl(false)",_FALSE,p.isAutomaticGainControl());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setAutomaticGainControl(false)",0x10000000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x10010000);

  p.setAutomaticGainControl(_TRUE);
  assertEquals("isAutomaticGainControl(true)",_TRUE,p.isAutomaticGainControl());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setAutomaticGainControl(true)",0x10010000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x10010000);
}

void BasicDataPacketTest::testSetSignalDetected () {
  BasicDataPacket p;
  char *buffer;
  p.setSignalDetected(_FALSE);
  assertEquals("isSignalDetected(false)",_FALSE,p.isSignalDetected());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setSignalDetected(false)",0x08000000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x08008000);

  p.setSignalDetected(_TRUE);
  assertEquals("isSignalDetected(true)",_TRUE,p.isSignalDetected());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setSignalDetected(true)",0x08008000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x08008000);
}

void BasicDataPacketTest::testSetInvertedSpectrum () {
  BasicDataPacket p;
  char *buffer;
  p.setInvertedSpectrum(_FALSE);
  assertEquals("isInvertedSpectrum(false)",_FALSE,p.isInvertedSpectrum());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setInvertedSpectrum(false)",0x04000000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x04004000);

  p.setInvertedSpectrum(_TRUE);
  assertEquals("isInvertedSpectrum(true)",_TRUE,p.isInvertedSpectrum());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setInvertedSpectrum(true)",0x04004000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x04004000);
}

void BasicDataPacketTest::testSetOverRange () {
  BasicDataPacket p;
  char *buffer;
  p.setOverRange(_FALSE);
  assertEquals("isOverRange(false)",_FALSE,p.isOverRange());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setOverRange(false)",0x02000000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x02002000);

  p.setOverRange(_TRUE);
  assertEquals("isOverRange(true)",_TRUE,p.isOverRange());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setOverRange(true)",0x02002000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x02002000);
}

void BasicDataPacketTest::testSetDiscontinuous () {
  BasicDataPacket p;
  char *buffer;
  p.setDiscontinuous(_FALSE);
  assertEquals("isDiscontinuous(false)",_FALSE,p.isDiscontinuous());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setDiscontinuous(false)",0x01000000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x01001000);

  p.setDiscontinuous(_TRUE);
  assertEquals("isDiscontinuous(true)",_TRUE,p.isDiscontinuous());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setDiscontinuous(true)",0x01001000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x01001000);
}

void BasicDataPacketTest::testSetBit11 () {
  BasicDataPacket p;
  char *buffer;
  p.setBit11(_FALSE);
  assertEquals("isBit11(false)",_FALSE,p.isBit11());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setBit11(false)",0x00800000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x00800800);

  p.setBit11(_TRUE);
  assertEquals("isBit11(true)",_TRUE,p.isBit11());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setBit11(true)",0x00800800,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x00800800);
}

void BasicDataPacketTest::testSetBit10 () {
  BasicDataPacket p;
  char *buffer;
  p.setBit10(_FALSE);
  assertEquals("isBit10(false)",_FALSE,p.isBit10());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setBit10(false)",0x00400000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x00400400);

  p.setBit10(_TRUE);
  assertEquals("isBit10(true)",_TRUE,p.isBit10());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setBit10(true)",0x00400400,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x00400400);
}

void BasicDataPacketTest::testSetBit9 () {
  BasicDataPacket p;
  char *buffer;
  p.setBit9(_FALSE);
  assertEquals("isBit9(false)",_FALSE,p.isBit9());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setBit9(false)",0x00200000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x00200200);

  p.setBit9(_TRUE);
  assertEquals("isBit9(true)",_TRUE,p.isBit9());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setBit9(true)",0x00200200,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x00200200);
}

void BasicDataPacketTest::testSetBit8 () {
  BasicDataPacket p;
  char *buffer;
  p.setBit8(_FALSE);
  assertEquals("isBit8(false)",_FALSE,p.isBit8());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setBit8(false)",0x00100000,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x00100100);

  p.setBit8(_TRUE);
  assertEquals("isBit8(true)",_TRUE,p.isBit8());
  buffer = (char*)p.getPacketPointer();
  assertEquals("setBit8(true)",0x00100100,VRTMath::unpackInt(buffer,28,BIG_ENDIAN) & 0x00100100);
}

void BasicDataPacketTest::testSetDataLength () {
  BasicDataPacket p1;
  BasicDataPacket p2;
  p1.setPayloadFormat(PayloadFormat_INT4);
  p2.setPayloadFormat(PayloadFormat_INT4);

  for (int32_t i = 0; i < 128; i+=8) {
    vector<int8_t> pay(i);
    string  n   = Utilities::format("%d", i);
    int32_t pad = (32 - ((i * 4) % 32)) % 32;
    bool    ok  = (VRTConfig::getVRTVersion() == VRTConfig::VITAVersion_V49) || (pad == 0);

    try {
      p1.setDataByte(pay); // i*INT8 -> i*INT4 (calls setDataLength(..))
      assertEquals(string("setDataByte(..) for ")+n+"*INT4 throws exception", !ok, false);
    }
    catch (VRTException e) {
      UNUSED_VARIABLE(e);
      assertEquals(string("setDataByte(..) for ")+n+"*INT4 throws exception", !ok, true);
    }
    assertEquals(string("getDataLength() for ")+n+"*INT4",  i,   p1.getDataLength());
    assertEquals(string("getPadBitCount() for ")+n+"*INT4", pad, p1.getPadBitCount());

    try {
      p2.setDataLength(i);
      assertEquals(string("setDataLength(..) for ")+n+"*INT4 throws exception", !ok, false);
    }
    catch (VRTException e) {
      UNUSED_VARIABLE(e);
      assertEquals(string("setDataLength(..) for ")+n+"*INT4 throws exception", !ok, true);
    }
    assertEquals(string("getDataLength() for ")+n+"*INT4",  i,   p2.getDataLength());
    assertEquals(string("getPadBitCount() for ")+n+"*INT4", pad, p2.getPadBitCount());
  }
}

void BasicDataPacketTest::testSetPayloadFormat () {
  BasicDataPacket p;
  p.setPayloadFormat(PayloadFormat_INT4);
  assertEquals("getDataLength 12",PayloadFormat_INT4,p.getPayloadFormat());

  p.setPayloadFormat(PayloadFormat_UINT64);
  assertEquals("getDataLength 12",PayloadFormat_UINT64,p.getPayloadFormat());
  PayloadFormat pf(false,RealComplexType_Real,DataItemFormat_SignedInt,false,0,0,15,5,1,1);
  p.setPayloadFormat(pf);
  assertEquals("getDataLength 12",pf,p.getPayloadFormat());
}

void BasicDataPacketTest::testSetAssocPacketCount () {
  BasicDataPacket p;

  for (int32_t i = 0; i <= 127; i++) {
    p.setAssocPacketCount((int8_t)i);

    char *packet = (char*)p.getPacketPointer();
    char  expVal = (int8_t)(i | 0x80);
    assertEquals("setAssocPacketCount(..) -> trailer length", 4, p.getTrailerLength());
    assertEquals("setAssocPacketCount(..) -> value",     expVal, packet[p.getPacketLength()-1]);
    assertEquals("getAssocPacketCount()",             (int8_t)i, p.getAssocPacketCount());
  }

  if (true) {
    // NO TRAILER
    p.setAssocPacketCount(INT8_NULL);
    assertEquals("setAssocPacketCount(null) -> trailer length", 0, p.getTrailerLength());
    assertEquals("getAssocPacketCount()",               INT8_NULL, p.getAssocPacketCount());
  }

  if (true) {
    // YES TRAILER
    p.setAutomaticGainControl(_TRUE);
    p.setAssocPacketCount(INT8_NULL);
    char  *packet = (char*)p.getPacketPointer();
    assertEquals("setAssocPacketCount(null) -> trailer length", 4, p.getTrailerLength());
    assertEquals("setAssocPacketCount(null) -> value",  (char)0,   packet[p.getPacketLength()-1]);
    assertEquals("getAssocPacketCount()",               INT8_NULL, p.getAssocPacketCount());
  }
}

void BasicDataPacketTest::testGetLostSamples () {
  BasicDataPacket p1;
  BasicDataPacket p2;
  TimeStamp       ts = TimeStamp::Y2K_GPS;
  PayloadFormat   pf = PayloadFormat_INT32;

  p1.setTimeStamp(ts);
  p2.setTimeStamp(ts);

  p1.setPayloadFormat(pf);

  for (double sampleRate = 1e3; sampleRate <= 1e9; sampleRate*=10) {
    if (true) {
      assertEquals("getLostSamples(..)", 0, p1.getLostSamples(ts, sampleRate));
      assertEquals("getLostBytes(..)",   0, p1.getLostBytes(  ts, sampleRate));
      assertEquals("getLostBytes(..)",   0, p2.getLostBytes(  ts, sampleRate, pf));
    }
    for (int32_t lostSamples = 1; lostSamples <= 32768; lostSamples*=2) {
      int32_t lostBytes = (pf.getDataItemSize() / 8) * lostSamples;

      // Previously this was computed as:
      //   dt = -1 * lostSamples * (1/sampleRate)
      // However that caused rounding errors in C++ on 32-bit platforms (e.g.
      // sampleRate=1e6 and lostSamples=1 yielded dt=-999999 not dt=-1000000).
      double    dt = -lostSamples / sampleRate;
      double    ps = rint(dt * TimeStamp::ONE_SEC);
      TimeStamp t  = ts.addPicoSeconds((int64_t)ps);
      assertEquals("getLostSamples(..)", lostSamples, p1.getLostSamples(t, sampleRate));
      assertEquals("getLostBytes(..)",   lostBytes,   p1.getLostBytes(  t, sampleRate));
      assertEquals("getLostBytes(..)",   lostBytes,   p2.getLostBytes(  t, sampleRate, pf));
    }
  }
}

void BasicDataPacketTest::testGetNextTimeStamp () {
  BasicDataPacket p1;
  BasicDataPacket p2;

  for (int32_t numSamples = 0; numSamples <= 65536; numSamples+=128) {
    vector<char> data(numSamples * 2);
    double       sampleRate = 1e6;                                                     // Hz
    int64_t      durationPS = (int64_t)(numSamples / sampleRate * TimeStamp::ONE_SEC); // picoseconds
    TimeStamp    thisTime   = TimeStamp::Y2K_GPS;
    TimeStamp    nextTime   = thisTime.addPicoSeconds(durationPS);

    p1.setTimeStamp(thisTime);
    p2.setTimeStamp(thisTime);
    p1.setPayload(data);
    p2.setPayload(data);

    p1.setPayloadFormat(PayloadFormat_INT16);
    assertEquals("getNextTimeStamp(sr)",    nextTime, p1.getNextTimeStamp(sampleRate));
    assertEquals("getNextTimeStamp(sr,pf)", nextTime, p2.getNextTimeStamp(sampleRate, PayloadFormat_INT16));
  }
}

void BasicDataPacketTest::testGetDataByte () {
  for (int32_t pe = 0; pe <= 1; pe++) {
    for (size_t idx = 0; idx < formats.size(); idx++) {
      PayloadFormat   pf = formats[idx];   pf.setProcessingEfficient(pe == 1);
      BasicDataPacket p1;
      BasicDataPacket p2;                  p2.setPayloadFormat(pf);

      p1.setPayload(input);
      p2.setPayload(input);

      vector<int8_t>  actByte1   = p1.getDataByte(  pf);
      vector<int16_t> actShort1  = p1.getDataShort( pf);
      vector<int32_t> actInt1    = p1.getDataInt(   pf);
      vector<int64_t> actLong1   = p1.getDataLong(  pf);
      vector<float>   actFloat1  = p1.getDataFloat( pf);
      vector<double>  actDouble1 = p1.getDataDouble(pf);
      int32_t         len        = p1.getScalarDataLength(pf);

      switch (pf.getDataType()) {
        case DataType_UInt1:
          break;
        case DataType_Int4:
          break;
        case DataType_Int8:
          for (int32_t i = 0x00, j = 0; i <= 0xFF; i++,j++) {
            int8_t val = VRTMath::unpackByte(input, i);
            assertEquals(string("getDataByte(pf)   for ")+pf.toString(), (int8_t )val, actByte1[j]);
            assertEquals(string("getDataShort(pf)  for ")+pf.toString(), (int16_t)val, actShort1[j]);
            assertEquals(string("getDataInt(pf)    for ")+pf.toString(), (int32_t)val, actInt1[j]);
            assertEquals(string("getDataLong(pf)   for ")+pf.toString(), (int64_t)val, actLong1[j]);
            assertEquals(string("getDataFloat(pf)  for ")+pf.toString(), (float  )val, actFloat1[j]);
            assertEquals(string("getDataDouble(pf) for ")+pf.toString(), (double )val, actDouble1[j]);
          }
          break;
        case DataType_Int16:
          for (int32_t i = 0x00, j = 0; i <= 0xFF; i+=2,j++) {
            int16_t val = VRTMath::unpackShort(input, i);
            assertEquals(string("getDataByte(pf)   for ")+pf.toString(), (int8_t )val, actByte1[j]);
            assertEquals(string("getDataShort(pf)  for ")+pf.toString(), (int16_t)val, actShort1[j]);
            assertEquals(string("getDataInt(pf)    for ")+pf.toString(), (int32_t)val, actInt1[j]);
            assertEquals(string("getDataLong(pf)   for ")+pf.toString(), (int64_t)val, actLong1[j]);
            assertEquals(string("getDataFloat(pf)  for ")+pf.toString(), (float  )val, actFloat1[j]);
            assertEquals(string("getDataDouble(pf) for ")+pf.toString(), (double )val, actDouble1[j]);
          }
          break;
        case DataType_Int32:
          for (int32_t i = 0x00, j = 0; i <= 0xFF; i+=4,j++) {
            int32_t val = VRTMath::unpackInt(input, i);
            assertEquals(string("getDataByte(pf)   for ")+pf.toString(), (int8_t )val, actByte1[j]);
            assertEquals(string("getDataShort(pf)  for ")+pf.toString(), (int16_t)val, actShort1[j]);
            assertEquals(string("getDataInt(pf)    for ")+pf.toString(), (int32_t)val, actInt1[j]);
            assertEquals(string("getDataLong(pf)   for ")+pf.toString(), (int64_t)val, actLong1[j]);
            assertEquals(string("getDataFloat(pf)  for ")+pf.toString(), (float  )val, actFloat1[j]);
            assertEquals(string("getDataDouble(pf) for ")+pf.toString(), (double )val, actDouble1[j]);
          }
          break;
        case DataType_Int64:
          for (int32_t i = 0x00, j = 0; i <= 0xFF; i+=8,j++) {
            int64_t val = VRTMath::unpackLong(input, i);
            assertEquals(string("getDataByte(pf)   for ")+pf.toString(), (int8_t )val, actByte1[j]);
            assertEquals(string("getDataShort(pf)  for ")+pf.toString(), (int16_t)val, actShort1[j]);
            assertEquals(string("getDataInt(pf)    for ")+pf.toString(), (int32_t)val, actInt1[j]);
            assertEquals(string("getDataLong(pf)   for ")+pf.toString(), (int64_t)val, actLong1[j]);
            assertEquals(string("getDataFloat(pf)  for ")+pf.toString(), (float  )val, actFloat1[j]);
            assertEquals(string("getDataDouble(pf) for ")+pf.toString(), (double )val, actDouble1[j]);
          }
          break;
        case DataType_Float:
          for (int32_t i = 0x00, j = 0; i <= 0xFF; i+=4,j++) {
            float val = VRTMath::unpackFloat(input, i);
            assertEquals(string("getDataByte(pf)   for ")+pf.toString(), (int8_t )val, actByte1[j]);
            assertEquals(string("getDataShort(pf)  for ")+pf.toString(), (int16_t)val, actShort1[j]);
            assertEquals(string("getDataInt(pf)    for ")+pf.toString(), (int32_t)val, actInt1[j]);
            assertEquals(string("getDataLong(pf)   for ")+pf.toString(), (int64_t)val, actLong1[j]);
            assertEquals(string("getDataFloat(pf)  for ")+pf.toString(), (float  )val, actFloat1[j]);
            assertEquals(string("getDataDouble(pf) for ")+pf.toString(), (double )val, actDouble1[j]);
          }
          break;
        case DataType_Double:
          for (int32_t i = 0x00, j = 0; i <= 0xFF; i+=8,j++) {
            double val = VRTMath::unpackDouble(input, i);
            assertEquals(string("getDataByte(pf)   for ")+pf.toString(), (int8_t )val, actByte1[j]);
            assertEquals(string("getDataShort(pf)  for ")+pf.toString(), (int16_t)val, actShort1[j]);
            assertEquals(string("getDataInt(pf)    for ")+pf.toString(), (int32_t)val, actInt1[j]);
            assertEquals(string("getDataLong(pf)   for ")+pf.toString(), (int64_t)val, actLong1[j]);
            assertEquals(string("getDataFloat(pf)  for ")+pf.toString(), (float  )val, actFloat1[j]);
            assertEquals(string("getDataDouble(pf) for ")+pf.toString(), (double )val, actDouble1[j]);
          }
          break;
        case DataType_UInt4:
          break;
        case DataType_UInt8:
          break;
        case DataType_UInt16:
          break;
        case DataType_UInt32:
          break;
        case DataType_UInt64:
          break;
        default:
          throw VRTException(string("Unexpected payload format: ")+pf.toString());
      }

      vector<int8_t>  actByte2   = p2.getDataByte();
      vector<int16_t> actShort2  = p2.getDataShort();
      vector<int32_t> actInt2    = p2.getDataInt();
      vector<int64_t> actLong2   = p2.getDataLong();
      vector<float>   actFloat2  = p2.getDataFloat();
      vector<double>  actDouble2 = p2.getDataDouble();

      assertArrayEquals(string("getDataByte()   for ")+pf.toString(), actByte1,   actByte2);
      assertArrayEquals(string("getDataShort()  for ")+pf.toString(), actShort1,  actShort2);
      assertArrayEquals(string("getDataInt()    for ")+pf.toString(), actInt1,    actInt2);
      assertArrayEquals(string("getDataLong()   for ")+pf.toString(), actLong1,   actLong2);
      assertArrayEquals(string("getDataFloat()  for ")+pf.toString(), actFloat1,  actFloat2);
      assertArrayEquals(string("getDataDouble() for ")+pf.toString(), actDouble1, actDouble2);

      vector<int8_t>  actByte3(len);    p1.getDataByte(pf, &actByte3[0]);
      vector<int16_t> actShort3(len);   p1.getDataShort(pf, &actShort3[0]);
      vector<int32_t> actInt3(len);     p1.getDataInt(pf, &actInt3[0]);
      vector<int64_t> actLong3(len);    p1.getDataLong(pf, &actLong3[0]);
      vector<float>   actFloat3(len);   p1.getDataFloat(pf, &actFloat3[0]);
      vector<double>  actDouble3(len);  p1.getDataDouble(pf, &actDouble3[0]);

      assertArrayEquals(string("getDataByte(pf,ptr)   for ")+pf.toString(), actByte1,   actByte3);
      assertArrayEquals(string("getDataShort(pf,ptr)  for ")+pf.toString(), actShort1,  actShort3);
      assertArrayEquals(string("getDataInt(pf,ptr)    for ")+pf.toString(), actInt1,    actInt3);
      assertArrayEquals(string("getDataLong(pf,ptr)   for ")+pf.toString(), actLong1,   actLong3);
      assertArrayEquals(string("getDataFloat(pf,ptr)  for ")+pf.toString(), actFloat1,  actFloat3);
      assertArrayEquals(string("getDataDouble(pf,ptr) for ")+pf.toString(), actDouble1, actDouble3);

      vector<int8_t>  actByte4(len);    p2.getDataByte(&actByte4[0]);
      vector<int16_t> actShort4(len);   p2.getDataShort(&actShort4[0]);
      vector<int32_t> actInt4(len);     p2.getDataInt(&actInt4[0]);
      vector<int64_t> actLong4(len);    p2.getDataLong(&actLong4[0]);
      vector<float>   actFloat4(len);   p2.getDataFloat(&actFloat4[0]);
      vector<double>  actDouble4(len);  p2.getDataDouble(&actDouble4[0]);

      assertArrayEquals(string("getDataByte(ptr)   for ")+pf.toString(), actByte1,   actByte4);
      assertArrayEquals(string("getDataShort(ptr)  for ")+pf.toString(), actShort1,  actShort4);
      assertArrayEquals(string("getDataInt(ptr)    for ")+pf.toString(), actInt1,    actInt4);
      assertArrayEquals(string("getDataLong(ptr)   for ")+pf.toString(), actLong1,   actLong4);
      assertArrayEquals(string("getDataFloat(ptr)  for ")+pf.toString(), actFloat1,  actFloat4);
      assertArrayEquals(string("getDataDouble(ptr) for ")+pf.toString(), actDouble1, actDouble4);
    }
  }
}

void BasicDataPacketTest::testSetDataByte () {
  vector<int8_t > inByte(256);
  vector<int16_t> inShort(256);
  vector<int32_t> inInt(256);
  vector<int64_t> inLong(256);
  vector<float  > inFloat(256);
  vector<double > inDouble(256);

  for (int32_t i = 0; i < 256; i++) {
    inByte[i]   = (int8_t )(i & 0xF);
    inShort[i]  = (int16_t)(i & 0xF);
    inInt[i]    = (int32_t)(i & 0xF);
    inLong[i]   = (int64_t)(i & 0xF);
    inFloat[i]  = (float  )(i & 0xF);
    inDouble[i] = (double )(i & 0xF);
  }

  for (int32_t pe = 0; pe <= 1; pe++) {
    for (size_t idx = 0; idx < formats.size(); idx++) {
      if (formats[idx].getDataItemSize() < 8) continue;
      PayloadFormat   pf = formats[idx];   pf.setProcessingEfficient(pe == 1);
      BasicDataPacket p1;
      BasicDataPacket p2;                  p2.setPayloadFormat(pf);
      BasicDataPacket p3;
      BasicDataPacket p4;                  p4.setPayloadFormat(pf);

      p1.setDataByte(pf, inByte);
      p2.setDataByte(    inByte);
      p3.setDataByte(pf, &inByte[0], inByte.size());
      p4.setDataByte(    &inByte[0], inByte.size());
      assertArrayEquals(string("setDataByte(pf,vec) for ")+pf.toString(), inByte, p1.getDataByte(pf));
      assertArrayEquals(string("setDataByte(vec)    for ")+pf.toString(), inByte, p2.getDataByte(pf));
      assertArrayEquals(string("setDataByte(pf,p,l) for ")+pf.toString(), inByte, p3.getDataByte(pf));
      assertArrayEquals(string("setDataByte(p,l)    for ")+pf.toString(), inByte, p4.getDataByte(pf));

      p1.setDataShort(pf, inShort);
      p2.setDataShort(    inShort);
      p3.setDataShort(pf, &inShort[0], inShort.size());
      p4.setDataShort(    &inShort[0], inShort.size());
      assertArrayEquals(string("setDataShort(pf,vec) for ")+pf.toString(), inShort, p1.getDataShort(pf));
      assertArrayEquals(string("setDataShort(vec)    for ")+pf.toString(), inShort, p2.getDataShort(pf));
      assertArrayEquals(string("setDataShort(pf,p,l) for ")+pf.toString(), inShort, p3.getDataShort(pf));
      assertArrayEquals(string("setDataShort(p,l)    for ")+pf.toString(), inShort, p4.getDataShort(pf));

      p1.setDataInt(pf, inInt);
      p2.setDataInt(    inInt);
      p3.setDataInt(pf, &inInt[0], inInt.size());
      p4.setDataInt(    &inInt[0], inInt.size());
      assertArrayEquals(string("setDataInt(pf,vec) for ")+pf.toString(), inInt, p1.getDataInt(pf));
      assertArrayEquals(string("setDataInt(vec)    for ")+pf.toString(), inInt, p2.getDataInt(pf));
      assertArrayEquals(string("setDataInt(pf,p,l) for ")+pf.toString(), inInt, p3.getDataInt(pf));
      assertArrayEquals(string("setDataInt(p,l)    for ")+pf.toString(), inInt, p4.getDataInt(pf));

      p1.setDataLong(pf, inLong);
      p2.setDataLong(    inLong);
      p3.setDataLong(pf, &inLong[0], inLong.size());
      p4.setDataLong(    &inLong[0], inLong.size());
      assertArrayEquals(string("setDataLong(pf,vec) for ")+pf.toString(), inLong, p1.getDataLong(pf));
      assertArrayEquals(string("setDataLong(vec)    for ")+pf.toString(), inLong, p2.getDataLong(pf));
      assertArrayEquals(string("setDataLong(pf,p,l) for ")+pf.toString(), inLong, p3.getDataLong(pf));
      assertArrayEquals(string("setDataLong(p,l)    for ")+pf.toString(), inLong, p4.getDataLong(pf));

      p1.setDataFloat(pf, inFloat);
      p2.setDataFloat(    inFloat);
      p3.setDataFloat(pf, &inFloat[0], inFloat.size());
      p4.setDataFloat(    &inFloat[0], inFloat.size());
      assertArrayEquals(string("setDataFloat(pf,vec) for ")+pf.toString(), inFloat, p1.getDataFloat(pf));
      assertArrayEquals(string("setDataFloat(vec)    for ")+pf.toString(), inFloat, p2.getDataFloat(pf));
      assertArrayEquals(string("setDataFloat(pf,p,l) for ")+pf.toString(), inFloat, p3.getDataFloat(pf));
      assertArrayEquals(string("setDataFloat(p,l)    for ")+pf.toString(), inFloat, p4.getDataFloat(pf));

      p1.setDataDouble(pf, inDouble);
      p2.setDataDouble(    inDouble);
      p3.setDataDouble(pf, &inDouble[0], inDouble.size());
      p4.setDataDouble(    &inDouble[0], inDouble.size());
      assertArrayEquals(string("setDataDouble(pf,vec) for ")+pf.toString(), inDouble, p1.getDataDouble(pf));
      assertArrayEquals(string("setDataDouble(vec)    for ")+pf.toString(), inDouble, p2.getDataDouble(pf));
      assertArrayEquals(string("setDataDouble(pf,p,l) for ")+pf.toString(), inDouble, p3.getDataDouble(pf));
      assertArrayEquals(string("setDataDouble(p,l)    for ")+pf.toString(), inDouble, p4.getDataDouble(pf));
    }
  }
}

void BasicDataPacketTest::testGetData () {
  for (int32_t pe = 0; pe <= 1; pe++) {
    for (size_t idx = 0; idx < formats.size(); idx++) {
      PayloadFormat   pf = formats[idx];             pf.setProcessingEfficient(pe == 1);
      BasicDataPacket p1;                            p1.setPayload(input);
      BasicDataPacket p2;  p2.setPayloadFormat(pf);  p2.setPayload(input);

      vector<char> out1(256);    p1.getData(pf, &out1[0], false);
      vector<char> out2(256);    p1.getData(pf, &out2[0], true );
      vector<char> out3(256);    p1.getData(pf, out3, 0,  false);
      vector<char> out4(256);    p1.getData(pf, out4, 0,  true );
      vector<char> outA(256);    p2.getData(    &outA[0], false);
      vector<char> outB(256);    p2.getData(    &outB[0], true );
      vector<char> outC(256);    p2.getData(    outC, 0,  false);
      vector<char> outD(256);    p2.getData(    outD, 0,  true );

      if (pf.getDataItemSize() <= 8) {
        assertBufEquals(string("getData(pf,array,true) for ")+pf.toString(), &input[0], 256, &out2[0], 256);
      }
      else if (pf.getDataItemSize() == 16) {
        assertBufEquals(string("getData(pf,array,true) for ")+pf.toString(), &outputSwap2[0], 256, &out2[0], 256);
      }
      else if (pf.getDataItemSize() == 32) {
        assertBufEquals(string("getData(pf,array,true) for ")+pf.toString(), &outputSwap4[0], 256, &out2[0], 256);
      }
      else if (pf.getDataItemSize() == 64) {
        assertBufEquals(string("getData(pf,array,true) for ")+pf.toString(), &outputSwap8[0], 256, &out2[0], 256);
      }
      else {
        throw VRTException(string("Unexpected format ")+pf.toString());
      }

      assertBufEquals(string("getData(pf,array,false)   for ")+pf.toString(), &input[0], 256, &out1[0], 256);
      assertBufEquals(string("getData(pf,vec,off,false) for ")+pf.toString(), &out1[0],  256, &out3[0], 256);
      assertBufEquals(string("getData(pf,vec,off,true)  for ")+pf.toString(), &out2[0],  256, &out4[0], 256);

      assertBufEquals(string("getData(array,false)   for ")+pf.toString(), &out1[0], 256, &outA[0], 256);
      assertBufEquals(string("getData(array,true)    for ")+pf.toString(), &out2[0], 256, &outB[0], 256);
      assertBufEquals(string("getData(vec,off,false) for ")+pf.toString(), &out3[0], 256, &outC[0], 256);
      assertBufEquals(string("getData(vec,off,true)  for ")+pf.toString(), &out4[0], 256, &outD[0], 256);
    }
  }
}

void BasicDataPacketTest::testSetData () {
  for (int32_t pe = 0; pe <= 1; pe++) {
    for (size_t idx = 0; idx < formats.size(); idx++) {
      PayloadFormat   pf = formats[idx];             pf.setProcessingEfficient(pe == 1);
      BasicDataPacket p1;                            p1.setData(pf, &input[0], 256, false);
      BasicDataPacket p2;                            p2.setData(pf, &input[0], 256, true );
      BasicDataPacket p3;                            p3.setData(pf, input,  0, 256, false);
      BasicDataPacket p4;                            p4.setData(pf, input,  0, 256, true );
      BasicDataPacket pA;  pA.setPayloadFormat(pf);  pA.setData(pf, &input[0], 256, false);
      BasicDataPacket pB;  pB.setPayloadFormat(pf);  pB.setData(pf, &input[0], 256, true );
      BasicDataPacket pC;  pC.setPayloadFormat(pf);  pC.setData(pf, input,  0, 256, false);
      BasicDataPacket pD;  pD.setPayloadFormat(pf);  pD.setData(pf, input,  0, 256, true );

      if (pf.getDataItemSize() <= 8) {
        assertBufEquals(string("setData(pf,array,len,true) for ")+pf.toString(), &input[0], 256, p2.getPayloadPointer(), p2.getPayloadLength());
      }
      else if (pf.getDataItemSize() == 16) {
        assertBufEquals(string("setData(pf,array,len,true) for ")+pf.toString(), &outputSwap2[0], 256, p2.getPayloadPointer(), p2.getPayloadLength());
      }
      else if (pf.getDataItemSize() == 32) {
        assertBufEquals(string("setData(pf,array,len,true) for ")+pf.toString(), &outputSwap4[0], 256, p2.getPayloadPointer(), p2.getPayloadLength());
      }
      else if (pf.getDataItemSize() == 64) {
        assertBufEquals(string("setData(pf,array,len,true) for ")+pf.toString(), &outputSwap8[0], 256, p2.getPayloadPointer(), p2.getPayloadLength());
      }
      else {
        throw VRTException(string("Unexpected format ")+pf.toString());
      }

      assertBufEquals(string("setData(pf,array,len,false)   for ")+pf.toString(), &input[0], 256, p1.getPayloadPointer(), p1.getPayloadLength());
      assertBufEquals(string("setData(pf,vec,off,len,false) for ")+pf.toString(), p1.getPayloadPointer(), p1.getPayloadLength(), p3.getPayloadPointer(), p3.getPayloadLength());
      assertBufEquals(string("setData(pf,vec,off,len,true)  for ")+pf.toString(), p2.getPayloadPointer(), p2.getPayloadLength(), p4.getPayloadPointer(), p4.getPayloadLength());

      assertBufEquals(string("setData(pf,array,len,false)   for ")+pf.toString(), p1.getPayloadPointer(), p1.getPayloadLength(), pA.getPayloadPointer(), pA.getPayloadLength());
      assertBufEquals(string("setData(pf,array,len,true)    for ")+pf.toString(), p2.getPayloadPointer(), p2.getPayloadLength(), pB.getPayloadPointer(), pB.getPayloadLength());
      assertBufEquals(string("setData(pf,vec,off,len,false) for ")+pf.toString(), p3.getPayloadPointer(), p3.getPayloadLength(), pC.getPayloadPointer(), pC.getPayloadLength());
      assertBufEquals(string("setData(pf,vec,off,len,true)  for ")+pf.toString(), p4.getPayloadPointer(), p4.getPayloadLength(), pD.getPayloadPointer(), pD.getPayloadLength());
    }
  }
}

void BasicDataPacketTest::testSetScalarDataLength () {
  BasicDataPacket p1;   p1.setPayload(input);
  BasicDataPacket p2;   p2.setPayload(input);
  BasicDataPacket p3;   p3.setPayload(input);
  BasicDataPacket p4;   p4.setPayload(input);
  BasicDataPacket p5;   p5.setPayload(input);
  BasicDataPacket p6;   p6.setPayload(input);

  int32_t numWords = 256 / 4; // num 32-bit words in payload

  for (int32_t size = 1; size <= 64; size++) {
    for (int32_t cx = 0; cx <= 2; cx++) {
      RealComplexType type;
      int32_t         complexMult;

      switch (cx) {
        case 0: type = RealComplexType_Real;             complexMult = 1; break;
        case 1: type = RealComplexType_ComplexCartesian; complexMult = 2; break;
        case 2: type = RealComplexType_ComplexPolar;     complexMult = 2; break;
        default: throw VRTException("This should be impossible");
      }

      for (int32_t fmt = 0; fmt <= 4; fmt++) {
        if ((fmt == 2) && (size != 32)) break;
        if ((fmt == 3) && (size != 64)) break;

        DataItemFormat format;
        switch (fmt) {
          case 0: format = DataItemFormat_SignedInt;   break;
          case 1: format = DataItemFormat_UnsignedInt; break;
          case 2: format = DataItemFormat_Float;       break;
          case 3: format = DataItemFormat_Double;      break;
          default: throw VRTException("This should be impossible");
        }

        PayloadFormat pf(type, format, size);

        for (int32_t pe = 0; pe <= 1; pe++) {
          pf.setProcessingEfficient(pe == 1);
          p2.setPayloadFormat(pf);
          p4.setPayloadFormat(pf);
          p6.setPayloadFormat(pf);

          int32_t scalarLen;
          if (pe == 0) {         // processing efficient
            scalarLen = (numWords * 32) / size;
          }
          else if (size <= 32) { // 32-bit sizing
            int32_t elemPerWord = 32 / size;
            scalarLen = numWords * elemPerWord;
          }
          else {                 // 64-bit sizing (1 element per 2 32-bit words)
            scalarLen = numWords / 2;
          }
          int32_t dataLen = scalarLen / complexMult;

          assertEquals(string("getDataLength(pf)         for ")+pf.toString(), dataLen,   p1.getDataLength(pf));
          assertEquals(string("getDataLength()           for ")+pf.toString(), dataLen,   p2.getDataLength());
          assertEquals(string("getScalarDataLength(pf)   for ")+pf.toString(), scalarLen, p1.getScalarDataLength(pf));
          assertEquals(string("getScalarDataLength()     for ")+pf.toString(), scalarLen, p2.getScalarDataLength());

          if ((VRTConfig::getVRTVersion() == VRTConfig::VITAVersion_V49) && ((scalarLen % 2) == 1)) {
            // skip next part as it uses PadBitCount
          }
          else {
            p3.setDataLength(pf, dataLen);
            p4.setDataLength(dataLen);
            p5.setScalarDataLength(pf, scalarLen);
            p6.setScalarDataLength(scalarLen);
            assertEquals(string("setDataLength(pf,n)       for ")+pf.toString(), dataLen,   p3.getDataLength(pf));
            assertEquals(string("setDataLength(n)          for ")+pf.toString(), dataLen,   p4.getDataLength());
            assertEquals(string("setScalarDataLength(pf,n) for ")+pf.toString(), scalarLen, p5.getScalarDataLength(pf));
            assertEquals(string("setScalarDataLength(n)    for ")+pf.toString(), scalarLen, p6.getScalarDataLength());
          }
        }
      }
    }
  }
}

void BasicDataPacketTest::testSetPacketType () {
  BasicDataPacket p;

  p.setPacketType(PacketType_UnidentifiedData);
  assertEquals("setPacketType(UnidentifiedData)", PacketType_UnidentifiedData, p.getPacketType());

  p.setPacketType(PacketType_Data);
  assertEquals("setPacketType(Data)", PacketType_Data, p.getPacketType());

  p.setPacketType(PacketType_UnidentifiedExtData);
  assertEquals("setPacketType(UnidentifiedExtData)", PacketType_UnidentifiedExtData, p.getPacketType());

  p.setPacketType(PacketType_ExtData);
  assertEquals("setPacketType(ExtData)", PacketType_ExtData, p.getPacketType());

  try {
    p.setPacketType(PacketType_Context);
    assertEquals("setPacketType(Context) fails", true, false);
  }
  catch (VRTException e) {
    UNUSED_VARIABLE(e);
    assertEquals("setPacketType(Context) fails", true, true);
  }

  try {
    p.setPacketType(PacketType_ExtContext);
    assertEquals("setPacketType(ExtContext) fails", true, false);
  }
  catch (VRTException e) {
    UNUSED_VARIABLE(e);
    assertEquals("setPacketType(ExtContext) fails", true, true);
  }
}

static const int32_t   FIELD_OFFSET    = 3;
static const int32_t   NUM_FIELDS      = 14;
static const char*     FIELD_NAMES[14] = {
    "CalibratedTimeStamp", "DataValid",        "ReferenceLocked", "AGC",
    "SignalDetected",      "InvertedSpectrum", "OverRange",       "Discontinuous",
    "Bit11",               "Bit10",            "Bit9",            "Bit8",
    "AssocPacketCount",    "PayloadFormat"
  };
static const ValueType FIELD_TYPES[14] = {
    ValueType_BoolNull, ValueType_BoolNull, ValueType_BoolNull, ValueType_BoolNull,
    ValueType_BoolNull, ValueType_BoolNull, ValueType_BoolNull, ValueType_BoolNull,
    ValueType_BoolNull, ValueType_BoolNull, ValueType_BoolNull, ValueType_BoolNull,
    ValueType_Int8,     ValueType_VRTObject
  };

void BasicDataPacketTest::testGetFieldName () {
  BasicDataPacket hf;
  checkGetFieldName(hf, FIELD_OFFSET, FIELD_NAMES, FIELD_TYPES, NUM_FIELDS);
}

void BasicDataPacketTest::testGetField () {
  BasicDataPacket hf;
  PayloadFormat   pf1 = PayloadFormat_INT32;
  PayloadFormat   pf2 = PayloadFormat_DOUBLE64;
  checkGetFieldBool(hf, CalibratedTimeStamp,  boolNull, _TRUE, _FALSE);
  checkGetFieldBool(hf, DataValid,            boolNull, _TRUE, _FALSE);
  checkGetFieldBool(hf, ReferenceLocked,      boolNull, _TRUE, _FALSE);
  checkGetFieldDef7(hf, AGC,                  boolNull, _TRUE, _FALSE, isAutomaticGainControl, setAutomaticGainControl);
  checkGetFieldBool(hf, SignalDetected,       boolNull, _TRUE, _FALSE);
  checkGetFieldBool(hf, InvertedSpectrum,     boolNull, _TRUE, _FALSE);
  checkGetFieldBool(hf, OverRange,            boolNull, _TRUE, _FALSE);
  checkGetFieldBool(hf, Discontinuous,        boolNull, _TRUE, _FALSE);
  checkGetFieldBool(hf, Bit11,                boolNull, _TRUE, _FALSE);
  checkGetFieldBool(hf, Bit10,                boolNull, _TRUE, _FALSE);
  checkGetFieldBool(hf, Bit9,                 boolNull, _TRUE, _FALSE);
  checkGetFieldBool(hf, Bit8,                 boolNull, _TRUE, _FALSE);
  checkGetFieldDef5(hf, AssocPacketCount,     int8_t,        (int8_t)7, (int8_t)42);
  checkGetFieldObj5(hf, PayloadFormat,        PayloadFormat, pf1,       pf2);
}
