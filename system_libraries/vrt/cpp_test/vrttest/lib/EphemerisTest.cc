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

#include "BasicContextPacketTest.h"
#include "BasicDataPacket.h"
#include "BasicContextPacket.h"
#include "BasicDataPacket.h"
#include "EphemerisTest.h"
#include "HasFieldsTest.h"
#include "TestRunner.h"
#include "VRTConfig.h"

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

#define CHANGE_IND     0x80000000
#define REF_POINT      0x40000000
#define BANDWIDTH      0x20000000
#define IF_FREQ        0x10000000
#define RF_FREQ        0x08000000
#define RF_OFFSET      0x04000000
#define IF_OFFSET      0x02000000
#define REF_LEVEL      0x01000000
#define GAIN           0x00800000
#define OVER_RANGE     0x00400000
#define SAMPLE_RATE    0x00200000
#define TIME_ADJUST    0x00100000
#define TIME_CALIB     0x00080000
#define TEMPERATURE    0x00040000
#define DEVICE_ID      0x00020000
#define STATE_EVENT    0x00010000
#define DATA_FORMAT    0x00008000
#define GPS_EPHEM      0x00004000
#define INS_EPHEM      0x00002000
#define ECEF_EPHEM     0x00001000
#define REL_EPHEM      0x00000800
#define EPHEM_REF      0x00000400
#define GPS_ASCII      0x00000200
#define CONTEXT_ASOC   0x00000100
#define ECEF_EPHEM_ADJ 0x00000080
#define REL_EPHEM_ADJ  0x00000040

static int32_t getByteOffset (char*, int64_t field);

string EphemerisTest::getTestedClass () {
  return "vrt::Ephemeris";
}

vector<TestSet> EphemerisTest::init () {
  vector<TestSet> tests;

  tests.push_back(TestSet("getAdjustedTimeStamp",             this, "testGetAdjustedTimeStamp"));
  
  tests.push_back(TestSet("getAdjunct",                       this, "-"));
  tests.push_back(TestSet("getAttitudeAlpha",                 this, "+testSetAttitudeAlpha"));
  tests.push_back(TestSet("getAttitudeBeta",                  this, "+testSetAttitudeBeta"));
  tests.push_back(TestSet("getAttitudephi",                   this, "+testSetAttitudePhi"));
  tests.push_back(TestSet("getPositionX",                     this, "+testSetPositionX"));
  tests.push_back(TestSet("getPositionY",                     this, "+testSetPositionY"));
  tests.push_back(TestSet("getPositionZ",                     this, "+testSetPositionZ"));
  tests.push_back(TestSet("getVelocityX",                     this, "+testSetVelocityX"));
  tests.push_back(TestSet("getVelocityY",                     this, "+testSetVelocityY"));
  tests.push_back(TestSet("getVelocityZ",                     this, "+testSetVelocityZ"));
  tests.push_back(TestSet("setAttitudeAlpha",                 this, "testSetAttitudeAlpha"));
  tests.push_back(TestSet("setAttitudeBeta",                  this, "testSetAttitudeBeta"));
  tests.push_back(TestSet("setAttitudePhi",                   this, "testSetAttitudePhi"));
  tests.push_back(TestSet("setPositionX",                     this, "testSetPositionX"));
  tests.push_back(TestSet("setPositionY",                     this, "testSetPositionY"));
  tests.push_back(TestSet("setPositionZ",                     this, "testSetPositionZ"));
  tests.push_back(TestSet("setTimeStamp",                     this, "testSetTimeStamp"));
  tests.push_back(TestSet("setVelocityX",                     this, "testSetVelocityX"));
  tests.push_back(TestSet("setVelocityY",                     this, "testSetVelocityY"));
  tests.push_back(TestSet("setVelocityZ",                     this, "testSetVelocityZ"));

  return tests;
}

void EphemerisTest::call (const string &func) {
  if (func == "testSetAttitudeAlpha"                    ) testSetAttitudeAlpha();
  if (func == "testSetAttitudeBeta"                     ) testSetAttitudeBeta();
  if (func == "testSetAttitudePhi"                      ) testSetAttitudePhi();
  if (func == "testsetPositionX"                        ) testSetPositionX();
  if (func == "testsetPositionY"                        ) testSetPositionY();
  if (func == "testsetPositionZ"                        ) testSetPositionZ();
  if (func == "testsetTimeStamp"                        ) testSetTimeStamp();
  if (func == "testsetVelocityX"                        ) testSetVelocityX();
  if (func == "testsetVelocityY"                        ) testSetVelocityY();
  if (func == "testsetVelocityZ"                        ) testSetVelocityZ();
}

void EphemerisTest::done () {
  // nothing to do
}

void EphemerisTest::testSetAttitudeAlpha () {
  char buffer[64];
  Ephemeris ep;
  ep.setAttitudeAlpha(1.5);
  ep.readBytes(buffer);
  assertEquals("setAttitudeAlpha 1.5",0x00600000, VRTMath::unpackInt(buffer, 28));
  assertEquals("getAttitudeAlpha 1.5",1.5, ep.getAttitudeAlpha());

  ep.setAttitudeAlpha(1.0000002384185791015625);
  ep.readBytes(buffer);
  assertEquals("setAttitudeAlpha 1.0000002384185791015625", 0x00400001,               VRTMath::unpackInt(buffer, 28));
  assertEquals("getAttitudeAlpha 1.0000002384185791015625", 1.0000002384185791015625, ep.getAttitudeAlpha());

  ep.setAttitudeAlpha(-0.0000002384185791015625);
  ep.readBytes(buffer);
  assertEquals("setAttitudeAlpha -0.0000002384185791015625", 0xFFFFFFFF,                VRTMath::unpackInt(buffer, 28));
  assertEquals("getAttitudeAlpha -0.0000002384185791015625", -0.0000002384185791015625, ep.getAttitudeAlpha());

  ep.setAttitudeAlpha(0xFFFF + 0.0);
  ep.readBytes(buffer);
  assertEquals("setAttitudeAlpha 0x7FFFFFFF error", 0x7FFFFFFF, VRTMath::unpackInt(buffer, 28));
}

void EphemerisTest::testSetAttitudeBeta () {
  char buffer[64];
  Ephemeris ep;
  ep.setAttitudeBeta(1.5);
  ep.readBytes(buffer);
  assertEquals("setAttitudeBeta 1.5",0x00600000, VRTMath::unpackInt(buffer, 32));
  assertEquals("getAttitudeBeta 1.5",1.5, ep.getAttitudeBeta());

  ep.setAttitudeBeta(1.0000002384185791015625);
  ep.readBytes(buffer);
  assertEquals("setAttitudeBeta 1.0000002384185791015625", 0x00400001,               VRTMath::unpackInt(buffer, 32));
  assertEquals("getAttitudeBeta 1.0000002384185791015625", 1.0000002384185791015625, ep.getAttitudeBeta());

  ep.setAttitudeBeta(-0.0000002384185791015625);
  ep.readBytes(buffer);
  assertEquals("setAttitudeBeta -0.0000002384185791015625", 0xFFFFFFFF,                VRTMath::unpackInt(buffer, 32));
  assertEquals("getAttitudeBeta -0.0000002384185791015625", -0.0000002384185791015625, ep.getAttitudeBeta());

  ep.setAttitudeBeta(0xFFFF + 0.0);
  ep.readBytes(buffer);
  assertEquals("setAttitudeBeta 0x7FFFFFFF error", 0x7FFFFFFF, VRTMath::unpackInt(buffer, 32));
}

void EphemerisTest::testSetAttitudePhi () {
  char buffer[64];
  Ephemeris ep;
  ep.setAttitudePhi(1.5);
  ep.readBytes(buffer);
  assertEquals("setAttitudePhi 1.5",0x00600000, VRTMath::unpackInt(buffer, 36));
  assertEquals("getAttitudePhi 1.5",1.5, ep.getAttitudePhi());

  ep.setAttitudePhi(1.0000002384185791015625);
  ep.readBytes(buffer);
  assertEquals("setAttitudePhi 1.0000002384185791015625", 0x00400001,               VRTMath::unpackInt(buffer, 36));
  assertEquals("getAttitudePhi 1.0000002384185791015625", 1.0000002384185791015625, ep.getAttitudePhi());

  ep.setAttitudePhi(-0.0000002384185791015625);
  ep.readBytes(buffer);
  assertEquals("setAttitudePhi -0.0000002384185791015625", 0xFFFFFFFF,                VRTMath::unpackInt(buffer, 36));
  assertEquals("getAttitudePhi -0.0000002384185791015625", -0.0000002384185791015625, ep.getAttitudePhi());

  ep.setAttitudePhi(0xFFFF + 0.0);
  ep.readBytes(buffer);
  assertEquals("setAttitudePhi 0x7FFFFFFF error", 0x7FFFFFFF, VRTMath::unpackInt(buffer, 36));
}

void EphemerisTest::testSetPositionX () {
  char buffer[64];
  Ephemeris ep;

  ep.setPositionX(0xABC + 0.5);
  ep.readBytes(buffer);
  assertEquals("setPositionX 0xABC + 0.5", 0x00015790,   VRTMath::unpackInt(buffer, 16));
  assertEquals("getPositionX 0xABC + 0.5", 0xABC + 0.5, ep.getPositionX());

  ep.setPositionX(0xF0F + 0.875);
  ep.readBytes(buffer);
  assertEquals("setPositionX 0xF0F + 0.875", 0x0001E1FC,    VRTMath::unpackInt(buffer, 16));
  assertEquals("getPositionX 0xF0F + 0.875", 0xF0F + 0.875, ep.getPositionX());

  ep.setPositionX(-0.5);
  ep.readBytes(buffer);

  assertEquals("setPositionX -0.5", 0xFFFFFFF0, VRTMath::unpackInt(buffer, 16));
  assertEquals("getPositionX -0.5", -0.5,       ep.getPositionX());
}

void EphemerisTest::testSetPositionY () {
  char buffer[64];
  Ephemeris ep;

  ep.setPositionY(0xABC + 0.5);
  ep.readBytes(buffer);
  assertEquals("setPositionY 0xABC + 0.5", 0x00015790,  VRTMath::unpackInt(buffer, 20));
  assertEquals("getPositionY 0xABC + 0.5", 0xABC + 0.5, ep.getPositionY());

  ep.setPositionY(0xF0F + 0.875);
  ep.readBytes(buffer);
  assertEquals("setPositionY 0xF0F + 0.875", 0x0001E1FC,    VRTMath::unpackInt(buffer, 20));
  assertEquals("getPositionY 0xF0F + 0.875", 0xF0F + 0.875, ep.getPositionY());

  ep.setPositionY(-0.5);
  ep.readBytes(buffer);

  assertEquals("setPositionY -0.5", 0xFFFFFFF0, VRTMath::unpackInt(buffer, 20));
  assertEquals("getPositionY -0.5", -0.5,       ep.getPositionY());
}

void EphemerisTest::testSetPositionZ () {
  char buffer[64];
  Ephemeris ep;

  ep.setPositionZ(0xABC + 0.5);
  ep.readBytes(buffer);
  assertEquals("setPositionZ 0xABC + 0.5", 0x00015790,  VRTMath::unpackInt(buffer, 24));
  assertEquals("getPositionZ 0xABC + 0.5", 0xABC + 0.5, ep.getPositionZ());

  ep.setPositionZ(0xF0F + 0.875);
  ep.readBytes(buffer);
  assertEquals("setPositionZ 0xF0F + 0.875", 0x0001E1FC,    VRTMath::unpackInt(buffer, 24));
  assertEquals("getPositionZ 0xF0F + 0.875", 0xF0F + 0.875, ep.getPositionZ());

  ep.setPositionZ(-0.5);
  ep.readBytes(buffer);

  assertEquals("setPositionZ -0.5", 0xFFFFFFF0, VRTMath::unpackInt(buffer, 24));
  assertEquals("getPositionZ -0.5", -0.5,       ep.getPositionZ());
}

void EphemerisTest::testSetVelocityX () {
  char buffer[64];
  Ephemeris ep;

  ep.setVelocityX(1.5);
  ep.readBytes(buffer);
  assertEquals("setVelocityX 1.5", 0x00018000, VRTMath::unpackInt(buffer, 40));
  assertEquals("getVelocityX 1.5", 1.5,        ep.getVelocityX());

  ep.setVelocityX(1.0000152587890625);
  ep.readBytes(buffer);
  assertEquals("setVelocityX 1.0000152587890625", 0x00010001,         VRTMath::unpackInt(buffer, 40));
  assertEquals("getVelocityX 1.0000152587890625", 1.0000152587890625, ep.getVelocityX());

  ep.setVelocityX(-0.0000152587890625);
  ep.readBytes(buffer);
  assertEquals("setVelocityX -0.0000152587890625", 0xFFFFFFFF,          VRTMath::unpackInt(buffer, 40));
  assertEquals("getVelocityX -0.0000152587890625", -0.0000152587890625, ep.getVelocityX());

  ep.setVelocityX(0xFFFF + 0.0);
  ep.readBytes(buffer);
  assertEquals("setVelocityX 7FFFFFFF error", 0x7FFFFFFF, VRTMath::unpackInt(buffer, 40));
  assertEquals("getVelocityX 7FFFFFFF error", DOUBLE_NAN, ep.getVelocityX());
}

void EphemerisTest::testSetVelocityY () {
  char buffer[64];
  Ephemeris ep;

  ep.setVelocityY(1.5);
  ep.readBytes(buffer);
  assertEquals("setVelocityY 1.5", 0x00018000, VRTMath::unpackInt(buffer, 44));
  assertEquals("getVelocityY 1.5", 1.5,        ep.getVelocityY());

  ep.setVelocityY(1.0000152587890625);
  ep.readBytes(buffer);
  assertEquals("setVelocityY 1.0000152587890625", 0x00010001,         VRTMath::unpackInt(buffer, 44));
  assertEquals("getVelocityY 1.0000152587890625", 1.0000152587890625, ep.getVelocityY());

  ep.setVelocityY(-0.0000152587890625);
  ep.readBytes(buffer);
  assertEquals("setVelocityY -0.0000152587890625", 0xFFFFFFFF,          VRTMath::unpackInt(buffer, 44));
  assertEquals("getVelocityY -0.0000152587890625", -0.0000152587890625, ep.getVelocityY());

  ep.setVelocityY(0xFFFF + 0.0);
  ep.readBytes(buffer);
  assertEquals("setVelocityY 7FFFFFFF error", 0x7FFFFFFF, VRTMath::unpackInt(buffer, 44));
  assertEquals("getVelocityY 7FFFFFFF error", DOUBLE_NAN, ep.getVelocityY());
}

void EphemerisTest::testSetVelocityZ () {
  char buffer[64];
  Ephemeris ep;

  ep.setVelocityZ(1.5);
  ep.readBytes(buffer);
  assertEquals("setVelocityZ 1.5", 0x00018000, VRTMath::unpackInt(buffer, 48));
  assertEquals("getVelocityZ 1.5", 1.5,        ep.getVelocityZ());

  ep.setVelocityZ(1.0000152587890625);
  ep.readBytes(buffer);
  assertEquals("setVelocityZ 1.0000152587890625", 0x00010001,         VRTMath::unpackInt(buffer, 48));
  assertEquals("getVelocityZ 1.0000152587890625", 1.0000152587890625, ep.getVelocityZ());

  ep.setVelocityZ(-0.0000152587890625);
  ep.readBytes(buffer);
  assertEquals("setVelocityZ -0.0000152587890625", 0xFFFFFFFF,          VRTMath::unpackInt(buffer, 48));
  assertEquals("getVelocityZ -0.0000152587890625", -0.0000152587890625, ep.getVelocityZ());

  ep.setVelocityZ(0xFFFF + 0.0);
  ep.readBytes(buffer);
  assertEquals("setVelocityZ 7FFFFFFF error", 0x7FFFFFFF, VRTMath::unpackInt(buffer, 48));
  assertEquals("getVelocityZ 7FFFFFFF error", DOUBLE_NAN, ep.getVelocityZ());
}

void EphemerisTest::testSetTimeStamp () {
  char buffer[64];
  Ephemeris ep;

  ep.setTimeStamp(TimeStamp(IntegerMode_UTC,0xFEDCBA,__INT64_C(0x123456789A)));
  ep.readBytes(buffer);
  assertEquals("setTimeStamp TSI/TSF",         0x06000000, VRTMath::unpackInt(buffer, 0));
  assertEquals("setTimeStamp Integer",         0x00FEDCBA, VRTMath::unpackInt(buffer, 4));
  assertEquals("setTimeStamp Fractional high", 0x00000012, VRTMath::unpackInt(buffer, 8));
  assertEquals("setTimeStamp Fractional low",  0x3456789A, VRTMath::unpackInt(buffer, 12));
}



static int32_t getByteOffset (char* input, int64_t field) {
  int32_t indicator = VRTMath::unpackInt(input, 0);
  if ((field & indicator) == 0) {      //the field is not set
    return -1;
  }
  int32_t i;
  int32_t ret = 0;
  int32_t off;
  for (i = 0; (field & 1) == 0; i++) {
    field = field >> 1;
  }
  for (int32_t j = 1; i+j < 32;j++){
    if (((indicator >> (i+j)) & 1) == 1) {
      switch(1 << (i+j)) {
        case REF_POINT:   // FALLTHROUGH
        case REF_LEVEL:   // FALLTHROUGH
        case GAIN:        // FALLTHROUGH
        case OVER_RANGE:  // FALLTHROUGH
        case TIME_CALIB:  // FALLTHROUGH
        case TEMPERATURE: // FALLTHROUGH
        case STATE_EVENT: // FALLTHROUGH
        case EPHEM_REF:
          ret += 4;       // these are all 1-word fields
          break;
        case BANDWIDTH:   // FALLTHROUGH
        case IF_FREQ:     // FALLTHROUGH
        case RF_FREQ:     // FALLTHROUGH
        case IF_OFFSET:   // FALLTHROUGH
        case RF_OFFSET:   // FALLTHROUGH
        case SAMPLE_RATE: // FALLTHROUGH
        case TIME_ADJUST: // FALLTHROUGH
        case DEVICE_ID:   // FALLTHROUGH
        case DATA_FORMAT:
          ret += 8;       // these are all 2-word fields
          break;
        case GPS_EPHEM:   // FALLTHROUGH
        case INS_EPHEM:
          ret += 44;      // these are all 11-word fields
          break;
        case ECEF_EPHEM:  // FALLTHROUGH
        case REL_EPHEM:
          ret += 52;      // these are all 13-word field
          break;
        case GPS_ASCII:
          off = getByteOffset(input,GPS_ASCII);
          ret += 4*(2 + VRTMath::unpackInt(input, off+4));     //2 + X words
          break;
        case CONTEXT_ASOC:
          off = getByteOffset(input,CONTEXT_ASOC);
          ret += 8;
          ret += 4*((((int32_t)input[off+4]) << 8) + input[off+5]); //source list size
          ret += 4*((((int32_t)input[off+6]) << 8) + input[off+7]); //system list size
          ret += 4*((((int32_t)input[off+8]) << 8) + input[off+9]); //vector-component list size
          ret += (((((int32_t)input[off+10]) & 0x80) == 0) ? 4 : 8)*(((((int32_t)input[off+10]) & 0x7F) << 8) + (input[off+11] & 0xFF));
          break;
      }
    }
  }
  return ret;
}

//static void printHexDump(void* buffer, int32_t length) {
//  uint8_t* temp = (uint8_t*)buffer;
//  uint32_t len = length;
//  uint32_t digits = 0;
//  while(len) {
//    len /= 10;
//    digits++;
//  }
//  for(int i = 0; i < length; i++)
//  {  
//    if(i % 16 == 0)
//      printf("%0*d: ",digits,i);
//    printf("%02X ",*temp);
//    if(i % 8 == 7)
//      cout << " ";
//    if(i % 16 == 15) {
//      cout << " |  ";
//      char* t = (char*)(temp - 15*sizeof(uint8_t));
//      char out;
//      for(uint8_t i = 0; i < 16; i++)
//      {
//        out = *t;
//        out = ((out < 0x20) || (out >= 0x7F))? '.' : out;
//        cout << out;
//        t = t + sizeof(char);
//      }
//      cout << endl;
//    }
//    temp = temp + sizeof(uint8_t);
//  }
//}
