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

#include "HasFieldsTest.h"
#include "TestRunner.h"
#include "TimeStampTest.h"
#include "TimeStamp.h"

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

const int32_t GPS_JUL1985LS =  173059200; //1985-07-01:00:00:00 GPS time
const int32_t UTC_JUL1985LS =  489024015; //1985-07-01:00:00:00 UTC time
const int32_t GPS_DEC2005LS =  820108800; //2006-01-01:00:00:00 GPS time
const int32_t UTC_DEC2005LS = 1136073625; //2006-01-01:00:00:00 UTC time

string TimeStampTest::getTestedClass () {
  return "vrt::TimeStamp";
}

vector<TestSet> TimeStampTest::init () {
  vector<TestSet> tests;
  tests.push_back(TestSet("TimeStamp(..)",            this,  "testTimeStamp"));
  tests.push_back(TestSet("addPicoSeconds(..)",       this,  "testAddPicoSeconds"));
  tests.push_back(TestSet("addSeconds(..)",           this,  "testAddSeconds"));
  tests.push_back(TestSet("addTime(..)",              this,  "testAddTime"));
  tests.push_back(TestSet("compareTo(..)",            this,  "testCompareTo"));
  tests.push_back(TestSet("equals(..)",               this,  "testEquals"));
  tests.push_back(TestSet("forNoradTime(..)",         this, "-"));  //function is deprecated
  tests.push_back(TestSet("forTime(..)",              this,  "testForTime"));
  tests.push_back(TestSet("forTimeIRIG(..)",          this,  "testForTimeIRIG"));
  tests.push_back(TestSet("forTimeMidas(..)",         this,  "testForTimeMidas"));
  tests.push_back(TestSet("forTimeNORAD(..)",         this, "+testForTimeIRIG"));
  tests.push_back(TestSet("forTimePOSIX(..)",         this,  "testForTimePOSIX"));
  tests.push_back(TestSet("forTimePTP(..)",           this,  "testForTimePTP"));
  tests.push_back(TestSet("getEpoch()",               this,  "testGetEpoch"));
  tests.push_back(TestSet("getField(..)",             this,  "testGetField"));
  tests.push_back(TestSet("getFieldByName(..)",       this, "+testGetField"));
  tests.push_back(TestSet("getFieldCount()",          this, "+testGetFieldName"));
  tests.push_back(TestSet("getFieldID(..)",           this, "+testGetFieldName"));
  tests.push_back(TestSet("getFieldName(..)",         this,  "testGetFieldName"));
  tests.push_back(TestSet("getFieldType(..)",         this, "+testGetFieldName"));
  tests.push_back(TestSet("getFractionalMode()",      this, "+testTimeStamp"));
  tests.push_back(TestSet("getFractionalSeconds()",   this,  "testGetFractionalSeconds"));
  tests.push_back(TestSet("getGPSSeconds()",          this,  "testGetGPSSeconds"));
  tests.push_back(TestSet("getIntegerMode()",         this, "+testTimeStamp"));
  tests.push_back(TestSet("getLeapSecondRef()",       this, "+testTimeStamp"));
  tests.push_back(TestSet("getMidasSeconds()",        this, "testGetMidasSeconds"));
  tests.push_back(TestSet("getMidasTime()",           this, "+testGetMidasTime"));
  tests.push_back(TestSet("getNORADSeconds()",        this,  "testGetNORADSeconds"));
  tests.push_back(TestSet("getPOSIXSeconds()",        this,  "testGetPOSIXSeconds"));
  tests.push_back(TestSet("getPicoSeconds()",         this,  "testGetPicoSeconds"));
  tests.push_back(TestSet("getSecondsGPS()",          this, "+testGetGPSSeconds"));
  tests.push_back(TestSet("getSecondsUTC()",          this, "+testGetUTCSeconds"));
  tests.push_back(TestSet("getSecondsNORAD()",        this, "+testGetNORADSeconds"));
  tests.push_back(TestSet("getSecondsPOSIX()",        this, "+testGetPOSIXSeconds"));
  tests.push_back(TestSet("getSystemTime()",          this,  "testGetSystemTime"));
  tests.push_back(TestSet("getTimeStampFractional()", this, "+testTimeStamp"));
  tests.push_back(TestSet("getTimeStampInteger()",    this, "+testTimeStamp"));
  tests.push_back(TestSet("getUTCSeconds()",          this,  "testGetUTCSeconds"));
  tests.push_back(TestSet("parseTime(..)",            this,  "testParseTime"));
  tests.push_back(TestSet("setField(..)",             this,  "testSetField"));
  tests.push_back(TestSet("setFieldByName(..)",       this, "+testSetField"));
  tests.push_back(TestSet("toGPS()",                  this,  "testToGPS"));
  tests.push_back(TestSet("toString()",               this, "+"));
  tests.push_back(TestSet("toStringUTC()",            this,  "testToStringUTC"));
  tests.push_back(TestSet("toStringGPS()",            this,  "testToStringGPS"));
  tests.push_back(TestSet("toUTC()",                  this,  "testToUTC"));
  tests.push_back(TestSet("operator<(..)",            this, "+testCompareTo"));
  tests.push_back(TestSet("operator<=(..)",           this, "+testCompareTo"));
  tests.push_back(TestSet("operator==(..)",           this, "+testCompareTo"));
  tests.push_back(TestSet("operator>=(..)",           this, "+testCompareTo"));
  tests.push_back(TestSet("operator>(..)",            this, "+testCompareTo"));

  return tests;
}

void TimeStampTest::done () {
  // nothing to do
}

void TimeStampTest::call (const string &func) {
  if (func == "testTimeStamp"           ) testTimeStamp();
  if (func == "testAddPicoSeconds"      ) testAddPicoSeconds();
  if (func == "testAddSeconds"          ) testAddSeconds();
  if (func == "testAddTime"             ) testAddTime();
  if (func == "testCompareTo"           ) testCompareTo();
  if (func == "testEquals"              ) testEquals();
  if (func == "testForTime"             ) testForTime();
  if (func == "testForTimeIRIG"         ) testForTimeIRIG();
  if (func == "testForTimeMidas"        ) testForTimeMidas();
  if (func == "testForTimePOSIX"        ) testForTimePOSIX();
  if (func == "testForTimePTP"          ) testForTimePTP();
  if (func == "testGetEpoch"            ) testGetEpoch();
  if (func == "testGetField"            ) testGetField();
  if (func == "testGetFieldName"        ) testGetFieldName();
  if (func == "testGetFractionalSeconds") testGetFractionalSeconds();
  if (func == "testGetGPSSeconds"       ) testGetGPSSeconds();
  if (func == "testGetMidasSeconds"     ) testGetMidasSeconds();
  if (func == "testGetNORADSeconds"     ) testGetNORADSeconds();
  if (func == "testGetPOSIXSeconds"     ) testGetPOSIXSeconds();
  if (func == "testGetPicoSeconds"      ) testGetPicoSeconds();
  if (func == "testGetSystemTime"       ) testGetSystemTime();
  if (func == "testGetUTCSeconds"       ) testGetUTCSeconds();
  if (func == "testParseTime"           ) testParseTime();
  if (func == "testSetField"            ) testSetField();
  if (func == "testToStringUTC"         ) testToStringUTC();
  if (func == "testToStringGPS"         ) testToStringGPS();
  if (func == "testToUTC"               ) testToUTC();
  if (func == "testToGPS"               ) testToGPS();
}


void TimeStampTest::testTimeStamp () {
  TimeStamp t1(IntegerMode_UTC,0,0);
  TimeStamp t2(IntegerMode_GPS,0,0);
  TimeStamp t3(IntegerMode_GPS,GPS_JUL1985LS,123456789);

  assertEquals("UTC 0 tsi",          0,                       t1.getTimeStampInteger());
  assertEquals("UTC 0 tsf",          __INT64_C(0),            t1.getTimeStampFractional());
  assertEquals("UTC 0 tsimode",      IntegerMode_UTC,         t1.getIntegerMode());
  assertEquals("UTC 0 tsfmode",      FractionalMode_RealTime, t1.getFractionalMode());

  assertEquals("GPS 0 tsi",          0,                       t2.getTimeStampInteger());
  assertEquals("GPS 0 tsf",          __INT64_C(0),            t2.getTimeStampFractional());
  assertEquals("GPS 0 tsimode",      IntegerMode_GPS,         t2.getIntegerMode());
  assertEquals("GPS 0 tsfmode",      FractionalMode_RealTime, t2.getFractionalMode());

  assertEquals("GPS 1985LS tsi",     GPS_JUL1985LS,           t3.getTimeStampInteger());
  assertEquals("GPS 1985LS tsf",     __INT64_C(123456789),    t3.getTimeStampFractional());
  assertEquals("GPS 1985LS tsimode", IntegerMode_GPS,         t3.getIntegerMode());
  assertEquals("GPS 1985LS tsfmode", FractionalMode_RealTime, t3.getFractionalMode());
}

void TimeStampTest::testAddPicoSeconds () {
  // wrap around a second in the positive direction
  TimeStamp t1(IntegerMode_UTC,0,0);
  TimeStamp t2(IntegerMode_GPS,0,0);

  for (int32_t i = 0; i < 65536; i++) {
    int64_t expSec = (__INT64_C(1234567890) * i) / __INT64_C(1000000000000);
    int64_t expPS  = (__INT64_C(1234567890) * i) % __INT64_C(1000000000000);

    assertEquals("UTC whole",      expSec, t1.getUTCSeconds());
    assertEquals("GPS whole",      expSec, t2.getGPSSeconds());
    assertEquals("UTC fractional", expPS,  t1.getPicoSeconds());
    assertEquals("GPS fractional", expPS,  t2.getPicoSeconds());
    t1 = t1.addPicoSeconds(1234567890);
    t2 = t2.addPicoSeconds(1234567890);
  }

  // wrap around negative
  t1 = TimeStamp(IntegerMode_UTC,1,0);
  t2 = TimeStamp(IntegerMode_GPS,1,0);
  t1 = t1.addPicoSeconds(-1);
  t2 = t2.addPicoSeconds(-1);
  assertEquals("UTC negative wrap fractional", __INT64_C(999999999999), t1.getPicoSeconds());
  assertEquals("GPS negative wrap fractional", __INT64_C(999999999999), t2.getPicoSeconds());
  assertEquals("UTC negative wrap whole",                 0,  t1.getSecondsUTC());
  assertEquals("GPS negative wrap whole",                 0,  t2.getSecondsGPS());

  // negative maximum wrap
  t1 = TimeStamp(IntegerMode_UTC,0,0);
  t2 = TimeStamp(IntegerMode_GPS,0,0);
  t1 = t1.addPicoSeconds(-1);
  t2 = t2.addPicoSeconds(-1);
  assertEquals("UTC max negative", __INT64_C(999999999999), t1.getPicoSeconds());
  assertEquals("GPS max negative", __INT64_C(999999999999), t2.getPicoSeconds());
}

void TimeStampTest::testAddSeconds () {
  TimeStamp t1(IntegerMode_UTC,0,0);
  TimeStamp t2(IntegerMode_GPS,0,0);

  t1 = t1.addSeconds(1234567890);                                 //t=1234567890
  t2 = t2.addSeconds(1234567890);
  assertEquals("UTC t=1234567890",1234567890,t1.getSecondsUTC());
  assertEquals("GPS t=1234567890",1234567890,t2.getSecondsGPS());

  t1 = t1.addSeconds(-234567890);                                 //t=1000000000
  t2 = t2.addSeconds(-234567890);
  assertEquals("UTC t=1000000000",1000000000,t1.getSecondsUTC());
  assertEquals("GPS t=1000000000",1000000000,t2.getSecondsGPS());

  t1 = t1.addSeconds(-1000000001);                                //t=-1
  t2 = t2.addSeconds(-1000000001);
  assertEquals("UTC t=-1",-1,t1.getSecondsUTC());
  assertEquals("GPS t=-1",-1,t2.getSecondsGPS());

  t1 = t1.addSeconds(2);                                          //t=1
  t2 = t2.addSeconds(2);
  assertEquals("UTC t=1",1,t1.getSecondsUTC());
  assertEquals("GPS t=1",1,t2.getSecondsGPS());

  t1 = t1.addSeconds(0x7FFFFFFE);                                 //max size
  t2 = t2.addSeconds(0x7FFFFFFE);
  assertEquals("UTC t=max",0x7FFFFFFF,t1.getSecondsUTC());
  assertEquals("GPS t=max",0x7FFFFFFF,t2.getSecondsGPS());

  t1 = t1.addSeconds(1);                                          //overflow to -max
  t2 = t2.addSeconds(1);
  assertEquals("UTC t=-max",0x80000000,t1.getSecondsUTC());
  assertEquals("GPS t=-max",0x80000000,t2.getSecondsGPS());

  t1 = t1.addSeconds(0x80000000);                                 //overflow back to 0
  t2 = t2.addSeconds(0x80000000);
  assertEquals("UTC t=-max",0,t1.getSecondsUTC());
  assertEquals("GPS t=-max",0,t2.getSecondsGPS());
}

void TimeStampTest::testAddTime () {
  TimeStamp t1(IntegerMode_UTC,0,0);
  TimeStamp t2(IntegerMode_GPS,0,0);

  t1 = t1.addTime(1234567890, 1234567890);
  t2 = t2.addTime(1234567890, 1234567890);
  assertEquals("UTC 1234567890 seconds",     __INT64_C(1234567890),  t1.getUTCSeconds());
  assertEquals("GPS 1234567890 seconds",     __INT64_C(1234567890),  t2.getGPSSeconds());
  assertEquals("UTC 1234567890 picoseconds", __UINT64_C(1234567890), t1.getPicoSeconds());
  assertEquals("GPS 1234567890 picoseconds", __UINT64_C(1234567890), t2.getPicoSeconds());

  t1 = t1.addTime(-234567891, -234567891);
  t2 = t2.addTime(-234567891, -234567891);

  assertEquals("UTC 1000000000 seconds",     __INT64_C(999999999),  t1.getUTCSeconds());
  assertEquals("GPS 1000000000 seconds",     __INT64_C(999999999),  t2.getGPSSeconds());
  assertEquals("UTC 1000000000 picoseconds", __UINT64_C(999999999), t1.getPicoSeconds());
  assertEquals("GPS 1000000000 picoseconds", __UINT64_C(999999999), t2.getPicoSeconds());
}

void TimeStampTest::testCompareTo () {
  TimeStamp u1(IntegerMode_UTC,0,0);
  TimeStamp g1(IntegerMode_GPS,0,0);
  TimeStamp u2(IntegerMode_UTC,0,0);
  TimeStamp g2(IntegerMode_GPS,0,0);

  assertEquals("UTC == UTC     ", 0, (int)u1.compareTo(u2));
  assertEquals("UTC == UTC (<) ", false, u1 <  u2);
  assertEquals("UTC == UTC (<=)", true,  u1 <= u2);
  assertEquals("UTC == UTC (==)", true,  u1 == u2);
  assertEquals("UTC == UTC (>=)", true,  u1 >= u2);
  assertEquals("UTC == UTC (>) ", false, u1 >  u2);
  u1 = u1.addSeconds(1);
  assertEquals("UTC > UTC     ", 1, (int)u1.compareTo(u2));
  assertEquals("UTC > UTC (<) ", false, u1 <  u2);
  assertEquals("UTC > UTC (<=)", false, u1 <= u2);
  assertEquals("UTC > UTC (==)", false, u1 == u2);
  assertEquals("UTC > UTC (>=)", true,  u1 >= u2);
  assertEquals("UTC > UTC (>) ", true,  u1 >  u2);
  u2 = u1.addSeconds(2);
  assertEquals("UTC < UTC     ", -1, (int)u1.compareTo(u2));
  assertEquals("UTC < UTC (<) ", true,  u1 <  u2);
  assertEquals("UTC < UTC (<=)", true,  u1 <= u2);
  assertEquals("UTC < UTC (==)", false, u1 == u2);
  assertEquals("UTC < UTC (>=)", false, u1 >= u2);
  assertEquals("UTC < UTC (>) ", false, u1 >  u2);

  assertEquals("GPS == GPS     ", 0, (int)g1.compareTo(g2));
  assertEquals("GPS == GPS (<) ", false, g1 <  g2);
  assertEquals("GPS == GPS (<=)", true,  g1 <= g2);
  assertEquals("GPS == GPS (==)", true,  g1 == g2);
  assertEquals("GPS == GPS (>=)", true,  g1 >= g2);
  assertEquals("GPS == GPS (>) ", false, g1 >  g2);
  g1 = g1.addSeconds(1);
  assertEquals("GPS > GPS     ", 1, (int)g1.compareTo(g2));
  assertEquals("GPS > GPS (<) ", false, g1 <  g2);
  assertEquals("GPS > GPS (<=)", false, g1 <= g2);
  assertEquals("GPS > GPS (==)", false, g1 == g2);
  assertEquals("GPS > GPS (>=)", true,  g1 >= g2);
  assertEquals("GPS > GPS (>) ", true,  g1 >  g2);
  g2 = g2.addSeconds(2);
  assertEquals("GPS < GPS     ", -1, (int)g1.compareTo(g2));
  assertEquals("GPS < GPS (<) ", true,  g1 <  g2);
  assertEquals("GPS < GPS (<=)", true,  g1 <= g2);
  assertEquals("GPS < GPS (==)", false, g1 == g2);
  assertEquals("GPS < GPS (>=)", false, g1 >= g2);
  assertEquals("GPS < GPS (>) ", false, g1 >  g2);

  u1 = u1.addSeconds(315964811);
  assertEquals("UTC == GPS     ", 0, (int)u1.compareTo(g1));
  assertEquals("UTC == GPS (<) ", false, u1 <  g1);
  assertEquals("UTC == GPS (<=)", true,  u1 <= g1);
  assertEquals("UTC == GPS (==)", true,  u1 == g1);
  assertEquals("UTC == GPS (>=)", true,  u1 >= g1);
  assertEquals("UTC == GPS (>) ", false, u1 >  g1);

  assertEquals("UTC < GPS     ", -1, (int)u1.compareTo(g2));
  assertEquals("UTC < GPS (<) ", true,  u1 <  g2);
  assertEquals("UTC < GPS (<=)", true,  u1 <= g2);
  assertEquals("UTC < GPS (==)", false, u1 == g2);
  assertEquals("UTC < GPS (>=)", false, u1 >= g2);
  assertEquals("UTC < GPS (>) ", false, u1 >  g2);

  u1 = u1.addSeconds(1);
  assertEquals("UTC > GPS     ", 1, (int)u1.compareTo(g1));
  assertEquals("UTC > GPS (<) ", false, u1 <  g1);
  assertEquals("UTC > GPS (<=)", false, u1 <= g1);
  assertEquals("UTC > GPS (==)", false, u1 == g1);
  assertEquals("UTC > GPS (>=)", true,  u1 >= g1);
  assertEquals("UTC > GPS (>) ", true,  u1 >  g1);

  assertEquals("GPS == UTC     ", 0, (int)g1.addSeconds(1).compareTo(u1));
  assertEquals("GPS == UTC (<) ", false, g1.addSeconds(1) <  u1);
  assertEquals("GPS == UTC (<=)", true,  g1.addSeconds(1) <= u1);
  assertEquals("GPS == UTC (==)", true,  g1.addSeconds(1) == u1);
  assertEquals("GPS == UTC (>=)", true,  g1.addSeconds(1) >= u1);
  assertEquals("GPS == UTC (>) ", false, g1.addSeconds(1) >  u1);

  assertEquals("GPS > UTC     ", 1, (int)g1.addSeconds(2).compareTo(u1));
  assertEquals("GPS > UTC (<) ", false, g1.addSeconds(2) <  u1);
  assertEquals("GPS > UTC (<=)", false, g1.addSeconds(2) <= u1);
  assertEquals("GPS > UTC (==)", false, g1.addSeconds(2) == u1);
  assertEquals("GPS > UTC (>=)", true,  g1.addSeconds(2) >= u1);
  assertEquals("GPS > UTC (>) ", true,  g1.addSeconds(2) >  u1);

  assertEquals("GPS < UTC     ", -1, (int)g1.compareTo(u1));
  assertEquals("GPS < UTC (<) ", true,  g1 <  u1);
  assertEquals("GPS < UTC (<=)", true,  g1 <= u1);
  assertEquals("GPS < UTC (==)", false, g1 == u1);
  assertEquals("GPS < UTC (>=)", false, g1 >= u1);
  assertEquals("GPS < UTC (>) ", false, g1 >  u1);
}

void TimeStampTest::testEquals () {
  TimeStamp u1(IntegerMode_UTC,1000000000,0);
  TimeStamp g1(IntegerMode_GPS,1000000000,0);
  TimeStamp u2(IntegerMode_UTC,1000000000,0);
  TimeStamp g2(IntegerMode_GPS,1000000000,0);

  assertEquals("UTC = UTC",true,u1.equals(u2));
  assertEquals("GPS = GPS",true,g1.equals(g2));
  assertEquals("UTC != GPS",false,u1.equals(g2));
  assertEquals("GPS != UTC",false,g1.equals(u2));
  u2 = u2.addSeconds(1);
  g2 = g2.addSeconds(1);
  assertEquals("UTC != UTC",false,u1.equals(u2));
  assertEquals("GPS != GPS",false,g1.equals(g2));
}

void TimeStampTest::testForTime () {
  TimeStamp t1 = TimeStamp::forTime(1985,7,1,0,0,0,0,0,IntegerMode_UTC);
  TimeStamp t2 = TimeStamp::forTime(1985,7,1,0,0,0,0,0,IntegerMode_GPS);
  assertEquals("Basic UTC",UTC_JUL1985LS,t1.getSecondsUTC());
  assertEquals("Basic GPS",GPS_JUL1985LS,t2.getSecondsGPS());

  t1 = TimeStamp::forTime(1985,6,30,23,59,59,0,0,IntegerMode_UTC);
  t2 = TimeStamp::forTime(1985,6,30,23,59,59,0,0,IntegerMode_GPS);
  assertEquals("UTC Leap Second",UTC_JUL1985LS-2,t1.getSecondsUTC());
  assertEquals("GPS Leap Second",GPS_JUL1985LS-1,t2.getSecondsGPS());

  t1 = TimeStamp::forTime(1985,6,30,23,59,61,0,0,IntegerMode_UTC);
  t2 = TimeStamp::forTime(1985,6,30,23,59,61,0,0,IntegerMode_GPS);
  assertEquals("UTC Wrap Seconds",UTC_JUL1985LS+1,t1.getSecondsUTC());
  assertEquals("GPS Wrap Seconds",GPS_JUL1985LS+1,t2.getSecondsGPS());

  t1 = TimeStamp::forTime(1985,6,30,23,60,61,0,0,IntegerMode_UTC);
  t2 = TimeStamp::forTime(1985,6,30,23,60,61,0,0,IntegerMode_GPS);
  assertEquals("UTC Wrap Minutes and Seconds",UTC_JUL1985LS+61,t1.getSecondsUTC());
  assertEquals("GPS Wrap Minutes and Seconds",GPS_JUL1985LS+61,t2.getSecondsGPS());

  t1 = TimeStamp::forTime(1985,6,30,27,60,61,0,0,IntegerMode_UTC);
  t2 = TimeStamp::forTime(1985,6,30,27,60,61,0,0,IntegerMode_GPS);
  assertEquals("UTC Wrap Hours Minutes and Seconds",UTC_JUL1985LS+14461,t1.getSecondsUTC());
  assertEquals("GPS Wrap Hours Minutes and Seconds",GPS_JUL1985LS+14461,t2.getSecondsGPS());

  t1 = TimeStamp::forTime(1985,6,40,27,60,61,0,0,IntegerMode_UTC);
  t2 = TimeStamp::forTime(1985,6,40,27,60,61,0,0,IntegerMode_GPS);
  assertEquals("UTC Wrap Days Hours Minutes and Seconds",UTC_JUL1985LS+878461,t1.getSecondsUTC());
  assertEquals("GPS Wrap Days Hours Minutes and Seconds",GPS_JUL1985LS+878461,t2.getSecondsGPS());
}

void TimeStampTest::testForTimeIRIG () {
  // Note that we specifically use 1 Feb of the current year to avoid strangeness
  // with the end-of-year/start-of-year logic (which we should eventually test too).
  string       now  = TimeStamp::getSystemTime().toStringUTC();
  int32_t      year = atoi(now.substr(0,4).c_str());
  TimeStamp    feb1 = TimeStamp::forTime(year, 2, 1, 0, 0, 0, 0, 0, TimeStamp::UTC_EPOCH);
  LeapSeconds *ls   = LeapSeconds::getDefaultInstance();


  for (int32_t day = 1; day <= 28; day++) {
    for (int32_t hour = 0; hour < 24; hour+=4) {
      for (int32_t min = 0; min < 60; min+=5) {
        for (int32_t sec = 0; sec < 60; sec+=15) {
          int32_t delta = ((day-1) * 86400)
                        + (hour    *  3600)
                        + (min     *    60)
                        + (sec            );       // Delta from 1 Feb
          int32_t doy   = 31 + day;                // Day of year
          int32_t soy   = delta + (31 * 86400);    // Sec of year

          TimeStamp exp1 = feb1.addSeconds(delta);
          TimeStamp exp2 = exp1.addPicoSeconds(__INT64_C(420000000000));

          // ==== IRIG =======================================================
          TimeStamp act1 = TimeStamp::forTimeIRIG(sec, min, hour, doy);
          TimeStamp act2 = TimeStamp::forTimeIRIG(sec, min, hour, doy, 42);
          assertEquals("forTimeIRIG(sec,min,hour,day)", exp1, act1);
          assertEquals("forTimeIRIG(sec,min,hour,day)", exp2, act2);

          // ==== NORAD ======================================================
          TimeStamp act1n = TimeStamp::forTimeNORAD(soy, __INT64_C(           0));
          TimeStamp act2n = TimeStamp::forTimeNORAD(soy, __INT64_C(420000000000));
          TimeStamp act3n = TimeStamp::forTimeNORAD(soy, __INT64_C(           0),   -1, ls);
          TimeStamp act4n = TimeStamp::forTimeNORAD(soy, __INT64_C(420000000000),   -1, ls);
          TimeStamp act5n = TimeStamp::forTimeNORAD(soy, __INT64_C(           0), year, ls);
          TimeStamp act6n = TimeStamp::forTimeNORAD(soy, __INT64_C(420000000000), year, ls);
          assertEquals("forTimeNORAD(sec,ps)",         exp1, act1n);
          assertEquals("forTimeNORAD(sec,ps)",         exp2, act2n);
          assertEquals("forTimeNORAD(sec,ps,-1,ls)",   exp1, act3n);
          assertEquals("forTimeNORAD(sec,ps,-1,ls)",   exp2, act4n);
          assertEquals("forTimeNORAD(sec,ps,year,ls)", exp1, act5n);
          assertEquals("forTimeNORAD(sec,ps,year,ls)", exp2, act6n);
        }
      }
    }
  }
}

void TimeStampTest::testForTimeMidas () {
  TimeStamp t1 = TimeStamp::forTimeMidas(TimeStamp::MIDAS2POSIX,0);
  assertEquals("Minimum Time",0,t1.getSecondsUTC());

  //Account for the 15 leap seconds between 1950 and 1985
  t1 = TimeStamp::forTimeMidas(TimeStamp::MIDAS2POSIX+UTC_JUL1985LS-15,0);
  assertEquals("UTC 1985-07-01:00:00:00 Numeric", UTC_JUL1985LS, t1.getSecondsUTC());

  t1 = TimeStamp::forTimeMidas(TimeStamp::MIDAS2POSIX+UTC_JUL1985LS-15,0);
  assertEquals("UTC 1985-07-01:00:00:00 Numeric", UTC_JUL1985LS, t1.getSecondsUTC());

  //Account for the 25 leap seconds between 1950 and 2006
  t1 = TimeStamp::forTimeMidas(TimeStamp::MIDAS2POSIX+UTC_DEC2005LS-25,0);
  assertEquals("UTC 2005-01-01:00:00:00 Numeric", UTC_DEC2005LS, t1.getSecondsUTC());
  //TimeStamp t1 = TimeStamp::forTimeMidas(GPS_JUL1985LS, GPS_JUL1985LS);
}

void TimeStampTest::testForTimePOSIX () {
  assertEquals("Zero Case",0,TimeStamp::forTimePOSIX(0,0).getSecondsUTC());
  assertEquals("Before UTC 1985 LS",UTC_JUL1985LS-2,TimeStamp::forTimePOSIX(UTC_JUL1985LS-16,0).getSecondsUTC());
  assertEquals("After UTC 1985 LS",UTC_JUL1985LS,TimeStamp::forTimePOSIX(UTC_JUL1985LS-15,0).getSecondsUTC());
}

void TimeStampTest::testForTimePTP () {
  assertEquals("Zero Case",0,TimeStamp::forTimePTP(315964819,0).getSecondsGPS());
  assertEquals("Before GPS 1985 LS",GPS_JUL1985LS-1,TimeStamp::forTimePTP(315964819+GPS_JUL1985LS-1,0).getSecondsGPS());
  assertEquals("After GPS 1985 LS",GPS_JUL1985LS+1,TimeStamp::forTimePTP(315964819+GPS_JUL1985LS+1,0).getSecondsGPS());
}

void TimeStampTest::testGetEpoch () {
  TimeStamp t1(IntegerMode_UTC,UTC_JUL1985LS,12345);
  TimeStamp t2(IntegerMode_GPS,0,67890);

  assertEquals("UTC Before conversion",IntegerMode_UTC,t1.getEpoch());
  assertEquals("GPS Before conversion",IntegerMode_GPS,t2.getEpoch());
  t1 = t1.toGPS();
  t2 = t2.toUTC();
  assertEquals("UTC After conversion",IntegerMode_GPS,t1.getEpoch());
  assertEquals("GPS After conversion",IntegerMode_UTC,t2.getEpoch());
}

void TimeStampTest::testGetFractionalSeconds () {
  TimeStamp t1(IntegerMode_UTC,UTC_JUL1985LS,1234567890);
  assertEquals("Average Case",0.00123456789,t1.getFractionalSeconds());
  assertEquals("0",0.0,TimeStamp(IntegerMode_GPS,0,0).getFractionalSeconds());
  for (int32_t i = 0; i < 100; i++) {
    t1 = t1.addPicoSeconds(1000000000);
  }
  assertEquals("Large Case",.10123456789,t1.getFractionalSeconds());
}

void TimeStampTest::testGetGPSSeconds () {
  TimeStamp t1(IntegerMode_UTC,UTC_JUL1985LS,0);

  assertEquals("GPS 1980 Start", 0,                TimeStamp::forTime(1980,1,6,0,0,0,0,0,IntegerMode_UTC).getSecondsGPS()&0xFFFFFFFF);
  assertEquals("UTC 1985LS",     GPS_JUL1985LS+4,  t1.getSecondsGPS()&0xFFFFFFFF);
  assertEquals("UTC 2006LS",     GPS_DEC2005LS+14, TimeStamp(IntegerMode_UTC,UTC_DEC2005LS,0).getSecondsGPS()&0xFFFFFFFF);
}

void TimeStampTest::testGetMidasSeconds () {
  assertEquals("UTC 0",       (double)(631152000),                  TimeStamp(IntegerMode_UTC,0,0).getMidasSeconds());
  assertEquals("1985 LS",     (double)(631152000+UTC_JUL1985LS-15), TimeStamp(IntegerMode_UTC,UTC_JUL1985LS,0).getMidasSeconds());
  assertEquals("1985 LS - 1", (double)(631152000+UTC_JUL1985LS-15), TimeStamp(IntegerMode_UTC,UTC_JUL1985LS-1,0).getMidasSeconds());
  assertEquals("2005 LS",     (double)(631152000+UTC_DEC2005LS-25), TimeStamp(IntegerMode_UTC,UTC_DEC2005LS,0).getMidasSeconds());
}

void TimeStampTest::testGetNORADSeconds () {
  assertEquals("UTC 0",                            0, TimeStamp(IntegerMode_UTC,UTC_DEC2005LS,0).getSecondsNORAD()&0xFFFFFFFF);
  assertEquals("UTC normal case",            1234567, TimeStamp(IntegerMode_UTC,UTC_DEC2005LS+1234567,0).getSecondsNORAD()&0xFFFFFFFF);
  assertEquals("1985 LS - 1 (not counted)", 15638400, TimeStamp(IntegerMode_UTC,UTC_JUL1985LS-1,0).getSecondsNORAD(_FALSE)&0xFFFFFFFF);
  assertEquals("1985 LS - 1 (counted)",     15638400, TimeStamp(IntegerMode_UTC,UTC_JUL1985LS-1,0).getSecondsNORAD(_TRUE)&0xFFFFFFFF);
  assertEquals("1985 LS (not counted)",     15638400, TimeStamp(IntegerMode_UTC,UTC_JUL1985LS,0).getSecondsNORAD(_FALSE)&0xFFFFFFFF);
  assertEquals("1985 LS (counted)",         15638401, TimeStamp(IntegerMode_UTC,UTC_JUL1985LS,0).getSecondsNORAD(_TRUE)&0xFFFFFFFF);
  assertEquals("1985 LS + 1 (not counted)", 15638401, TimeStamp(IntegerMode_UTC,UTC_JUL1985LS+1,0).getSecondsNORAD(_FALSE)&0xFFFFFFFF);
  assertEquals("1985 LS + 1 (counted)",     15638402, TimeStamp(IntegerMode_UTC,UTC_JUL1985LS+1,0).getSecondsNORAD(_TRUE)&0xFFFFFFFF);
}

void TimeStampTest::testGetPOSIXSeconds () {
  assertEquals("UTC 0",                     0, TimeStamp(IntegerMode_UTC,0,0).getSecondsPOSIX()&0xFFFFFFFF);
  //14 seconds difference before the LS
  assertEquals("1985 LS - 2",UTC_JUL1985LS-16, TimeStamp(IntegerMode_UTC,UTC_JUL1985LS-2,0).getSecondsPOSIX()&0xFFFFFFFF);
  assertEquals("1985 LS - 1",UTC_JUL1985LS-15, TimeStamp(IntegerMode_UTC,UTC_JUL1985LS-1,0).getSecondsPOSIX()&0xFFFFFFFF);
  //15 seconds difference after the LS
  assertEquals("1985 LS",    UTC_JUL1985LS-15, TimeStamp(IntegerMode_UTC,UTC_JUL1985LS,0).getSecondsPOSIX()&0xFFFFFFFF);
  assertEquals("1985 LS + 1",UTC_JUL1985LS-14, TimeStamp(IntegerMode_UTC,UTC_JUL1985LS+1,0).getSecondsPOSIX()&0xFFFFFFFF);
}

void TimeStampTest::testGetPicoSeconds () {
  TimeStamp t1(IntegerMode_UTC,0,0);
  assertEquals("UTC 0", __INT64_C(0), t1.getPicoSeconds());
  for (int64_t i = 1; i < 1500; i++) {
    t1 = t1.addPicoSeconds(1234567890);
    int64_t expSec = (__INT64_C(1234567890) * i) / __INT64_C(1000000000000);
    int64_t expPS  = (__INT64_C(1234567890) * i) % __INT64_C(1000000000000);
    assertEquals("getUTCSeconds()",  expSec, t1.getUTCSeconds());
    assertEquals("getPicoSeconds()", expPS,  t1.getPicoSeconds());
  }
}

void TimeStampTest::testGetSystemTime () {
  // The basic test is done in us since that is what the system clock uses,
  // however allow for a 100ms tolerance since there is some non-negligible
  // code between the call to get the system time in getSystemTime() and the
  // one below.
  int64_t   exp = Utilities::currentTimeMillis();
  TimeStamp ts  = TimeStamp::getSystemTime();
  int64_t   act = (ts.getPOSIXSeconds() * 1000)
                + (ts.getPicoSeconds() / __INT64_C(1000000000));
  assertEquals("System Time Test", exp, act, __INT64_C(100));
}

void TimeStampTest::testGetUTCSeconds () {
  assertEquals("GPS 0",      315964811,        TimeStamp(IntegerMode_GPS,0,0).getSecondsUTC()&0xFFFFFFFF);
  assertEquals("GPS 1985LS", UTC_JUL1985LS-4,  TimeStamp(IntegerMode_GPS,GPS_JUL1985LS,0).getSecondsUTC()&0xFFFFFFFF);
  assertEquals("GPS 2005LS", UTC_DEC2005LS-14, TimeStamp(IntegerMode_GPS,GPS_DEC2005LS,0).getSecondsUTC()&0xFFFFFFFF);
  assertEquals("UTC 0",      0,                TimeStamp(IntegerMode_UTC,0,0).getSecondsUTC()&0xFFFFFFFF);
  assertEquals("UTC 1985LS", UTC_JUL1985LS,    TimeStamp(IntegerMode_UTC,UTC_JUL1985LS,0).getSecondsUTC()&0xFFFFFFFF);
  assertEquals("UTC 2005LS", UTC_DEC2005LS,    TimeStamp(IntegerMode_UTC,UTC_DEC2005LS,0).getSecondsUTC()&0xFFFFFFFF);
}

void TimeStampTest::testParseTime () {
  assertEquals("GPS 0",           __INT64_C(0),               TimeStamp::parseTime("1980-01-06T00:00:00.000000000000Z", IntegerMode_GPS).getGPSSeconds());
  assertEquals("GPS 1985 LS - 1", (int64_t)(GPS_JUL1985LS-1), TimeStamp::parseTime("1985-06-30T23:59:59.000000000000Z", IntegerMode_GPS).getGPSSeconds());
  assertEquals("GPS 1985 LS",     (int64_t)(GPS_JUL1985LS),   TimeStamp::parseTime("1985-07-01T00:00:00.000000000000Z", IntegerMode_GPS).getGPSSeconds());
  assertEquals("GPS 1985 LS + 1", (int64_t)(GPS_JUL1985LS+1), TimeStamp::parseTime("1985-07-01T00:00:01.000000000000Z", IntegerMode_GPS).getGPSSeconds());

  assertEquals("UTC 0",           __INT64_C(0),               TimeStamp::parseTime("1970-01-01T00:00:00.000000000000Z", IntegerMode_UTC).getUTCSeconds());
  assertEquals("UTC 1985 LS - 2", (int64_t)(UTC_JUL1985LS-2), TimeStamp::parseTime("1985-06-30T23:59:59.000000000000Z", IntegerMode_UTC).getUTCSeconds());
  assertEquals("UTC 1985 LS - 1", (int64_t)(UTC_JUL1985LS-1), TimeStamp::parseTime("1985-06-30T23:59:60.000000000000Z", IntegerMode_UTC).getUTCSeconds());
  assertEquals("UTC 1985 LS",     (int64_t)(UTC_JUL1985LS),   TimeStamp::parseTime("1985-07-01T00:00:00.000000000000Z", IntegerMode_UTC).getUTCSeconds());
  assertEquals("UTC 1985 LS + 1", (int64_t)(UTC_JUL1985LS+1), TimeStamp::parseTime("1985-07-01T00:00:01.000000000000Z", IntegerMode_UTC).getUTCSeconds());

  assertEquals("GPS Picoseconds", __INT64_C(123456789000),    TimeStamp::parseTime("1985-07-01T00:00:01.123456789000Z", IntegerMode_GPS).getPicoSeconds());
  assertEquals("UTC Picoseconds", __INT64_C(123456789000),    TimeStamp::parseTime("1985-07-01T00:00:01.123456789000Z", IntegerMode_UTC).getPicoSeconds());
}

void TimeStampTest::testToStringUTC () {
  TimeStamp t1(IntegerMode_UTC,0,0);
  assertEquals("Output Test t=11","1970-01-01T00:00:00.000000000000Z",t1.toStringUTC());

  t1 = TimeStamp(IntegerMode_UTC,UTC_JUL1985LS-11,0);            //June 1985 leap second
  TimeStamp t2(IntegerMode_UTC,UTC_DEC2005LS-11,0);
  assertEquals("Constructor test", (int64_t)(UTC_JUL1985LS-11),t1.getUTCSeconds());

  for (int32_t i = 50; i <= 60; i++) {
    string exp1 = Utilities::format("1985-06-30T23:59:%d.000000000000Z", i);
    string exp2 = Utilities::format("2005-12-31T23:59:%d.000000000000Z", i);
    assertEquals("Output Test",exp1,t1.toStringUTC());
    assertEquals("Output Test",exp2,t2.toStringUTC());
    t1 = t1.addSeconds(1);
    t2 = t2.addSeconds(1);
  }
  assertEquals("Output Test t=11","1985-07-01T00:00:00.000000000000Z",t1.toStringUTC());
  assertEquals("Output Test t=11","2006-01-01T00:00:00.000000000000Z",t2.toStringUTC());

  //test picoseconds across the 1985 leap second
  t1 = t1.addSeconds(-1);
  for (int32_t i = 0; i < 1500; i++) {
    int64_t ps  = (__INT64_C(1234567890) * i);
    string exp;

    if (ps < __INT64_C(1000000000000)) {
      exp = Utilities::format("1985-06-30T23:59:60.%012" PRId64 "Z", ps % __INT64_C(1000000000000));
    }
    else {
      exp = Utilities::format("1985-07-01T00:00:00.%012" PRId64 "Z", ps % __INT64_C(1000000000000));
    }

    assertEquals("Output Test",exp, t1.toStringUTC());
    t1 = t1.addPicoSeconds(1234567890);
  }
}

void TimeStampTest::testToStringGPS () {
  TimeStamp t1(IntegerMode_GPS,0,0);
  assertEquals("Constructor test", __INT64_C(0), t1.getGPSSeconds());
  assertEquals("Constructor test","1980-01-06T00:00:00.000000000000Z",t1.toStringGPS());

  t1 = TimeStamp(IntegerMode_GPS,GPS_JUL1985LS-10,0);            //June 1985 leap second
  TimeStamp t2(IntegerMode_GPS,GPS_DEC2005LS-10,0);

  for (int32_t i = 50; i <= 59; i++) {
    string exp1 = Utilities::format("1985-06-30T23:59:%d.000000000000Z", i);
    string exp2 = Utilities::format("2005-12-31T23:59:%d.000000000000Z", i);
    assertEquals("Output Test 1985",exp1,t1.toStringGPS());
    assertEquals("Output Test 2005",exp2,t2.toStringGPS());
    t1 = t1.addSeconds(1);
    t2 = t2.addSeconds(1);
  }
  assertEquals("Output Test 1985 t=10","1985-07-01T00:00:00.000000000000Z",t1.toStringGPS());
  assertEquals("Output Test 2005 t=11","2006-01-01T00:00:00.000000000000Z",t2.toStringGPS());
  t1 = t1.addSeconds(1);
  t2 = t2.addSeconds(1);
  assertEquals("Output Test 1985 t=11","1985-07-01T00:00:01.000000000000Z",t1.toStringGPS());
  assertEquals("Output Test 2005 t=11","2006-01-01T00:00:01.000000000000Z",t2.toStringGPS());

  t1 = t1.addSeconds(-1);
  for (int32_t i = 0; i < 1500; i++) {
    int64_t ps  = (__INT64_C(1234567890) * i);
    string  exp;

    if (ps < __INT64_C(1000000000000)) {
      exp = Utilities::format("1985-07-01T00:00:00.%012" PRId64 "Z", ps % __INT64_C(1000000000000));
    }
    else {
      exp = Utilities::format("1985-07-01T00:00:01.%012" PRId64 "Z", ps % __INT64_C(1000000000000));
    }

    assertEquals("Output Test",exp, t1.toStringGPS());
    t1 = t1.addPicoSeconds(1234567890);
  }
}

void TimeStampTest::testToUTC () {
  TimeStamp t1(IntegerMode_GPS,0,0);
  t1 = t1.toUTC();
  assertEquals("0:(GPS2UTC)",315964811,t1.getSecondsUTC());

  t1 = TimeStamp(IntegerMode_GPS,GPS_JUL1985LS,0);
  t1 = t1.toUTC();

  //at this point, UTC is 4 seconds "behind" GPS in terms of displayed time
  assertEquals("1985-07-01:00:00:00 numeric",UTC_JUL1985LS-4,t1.getSecondsUTC());

  //at this point, UTC is 4 seconds "behind" GPS, so including the :60 leap second the clock should read :57
  assertEquals("1985-07-01:00:00:00 string","1985-06-30T23:59:57.000000000000Z",t1.toStringUTC());
  TimeStamp t2(IntegerMode_UTC,UTC_JUL1985LS,0);
  t2 = t2.toUTC();
  assertEquals("UTC to UTC",UTC_JUL1985LS,t2.getSecondsUTC());

  t1 = TimeStamp(IntegerMode_UTC,1234567890,0);
  t1 = t1.toGPS();
  t1 = t1.toUTC();
  assertEquals("Coversion to GPS and back to UTC",1234567890,t1.getSecondsUTC());
}

void TimeStampTest::testToGPS () {
  TimeStamp t1(IntegerMode_UTC,315964811,0);
  t1.toGPS();
  assertEquals("(GPS2UTC):0",0,t1.getSecondsGPS());

  t1 = TimeStamp(IntegerMode_GPS,1234567890,0);
  t1 = t1.toUTC();
  t1 = t1.toGPS();
  assertEquals("Coversion to UTC and back to GPS",1234567890,t1.getSecondsGPS());

  TimeStamp t2 (IntegerMode_GPS,GPS_JUL1985LS,0);
  t2 = t2.toGPS();
  assertEquals("GPS to GPS",GPS_JUL1985LS,t2.getSecondsGPS());

  t1 = TimeStamp(IntegerMode_UTC,UTC_JUL1985LS,0);
  t2 = TimeStamp(IntegerMode_UTC,UTC_DEC2005LS,0);
  t1 = t1.toGPS();
  t2 = t2.toGPS();

  //after 1985 LS, GPS displayed time is 4 seconds ahead of UTC
  assertEquals("1985-07-01:00:00:00 numeric",GPS_JUL1985LS+4,t1.getSecondsGPS());
  assertEquals("1985-07-01:00:00:00 string","1985-07-01T00:00:04.000000000000Z",t1.toStringGPS());

  //after 2005/2006 LS, GPS displayed time is 14 seconds ahead of UTC
  assertEquals("2006-01-01:00:00:00 numeric",GPS_DEC2005LS+14,t2.getSecondsGPS());
  assertEquals("2006-01-01:00:00:00 string","2006-01-01T00:00:14.000000000000Z",t2.toStringGPS());
}



static const int32_t   FIELD_OFFSET    = 0;
static const int32_t   FIELD_COUNT     = 16;
static const char*     FIELD_NAMES[16] = {
  "String",       "StringUTC",         "StringGPS",        "Epoch",
  "IntegerMode",  "FractionalMode",    "UTCSeconds",       "GPSSeconds",
  "NORADSeconds", "POSIXSeconds",      "MidasTime",        "MidasSeconds",
  "PicoSeconds",  "FractionalSeconds", "TimeStampInteger", "TimeStampFractional"
};
static const ValueType FIELD_TYPES[16] = {
  ValueType_String, ValueType_String, ValueType_String, ValueType_Int8,
  ValueType_Int8,   ValueType_Int8,   ValueType_Int64,  ValueType_Int64,
  ValueType_Int64,  ValueType_Int64,  ValueType_Double, ValueType_Double,
  ValueType_Int64,  ValueType_Double, ValueType_Int64,  ValueType_Int64
};

void TimeStampTest::testGetFieldName () {
  TimeStamp ts = TimeStamp::getSystemTime();
  checkGetFieldName(ts, FIELD_OFFSET, FIELD_NAMES, FIELD_TYPES, FIELD_COUNT);
}

#define checkGetField(id, T, exp) { \
  string name = FIELD_NAMES[id]; \
  Value *val  = ts.getField(id); \
  Value *act  = ts.getFieldByName(name); \
  assertEquals(       "getField("          #id    ")", exp, val->as<T>()); \
  assertEquals(string("getFieldByName(") + name + ")", exp, act->as<T>()); \
  delete val; \
  delete act; \
}

void TimeStampTest::testGetField () {
  TimeStamp ts = TimeStamp::getSystemTime();
  checkGetField( 0, string,  ts.toString());
  checkGetField( 1, string,  ts.toStringUTC());
  checkGetField( 2, string,  ts.toStringGPS());
  checkGetField( 3, int8_t,  (int8_t)ts.getEpoch());
  checkGetField( 4, int8_t,  (int8_t)ts.getIntegerMode());
  checkGetField( 5, int8_t,  (int8_t)ts.getFractionalMode());
  checkGetField( 6, int64_t, ts.getUTCSeconds());
  checkGetField( 7, int64_t, ts.getGPSSeconds());
  checkGetField( 8, int64_t, ts.getNORADSeconds());
  checkGetField( 9, int64_t, ts.getPOSIXSeconds());
  checkGetField(10, double,  ts.getMidasTime());
  checkGetField(11, double,  ts.getMidasSeconds());
  checkGetField(12, int64_t, (int64_t)ts.getPicoSeconds());
  checkGetField(13, double,  ts.getFractionalSeconds());
  checkGetField(14, int64_t, (int64_t)ts.getTimeStampInteger());
  checkGetField(15, int64_t, (int64_t)ts.getTimeStampFractional());
}

void TimeStampTest::testSetField () {
  // TimeStamp fields are read-only so make sure the set methods fail
  TimeStamp ts = TimeStamp::getSystemTime();

  try {
    ts.setField(6, Value(123));
    assertEquals("setField(..) is unsupported", true, false);
  }
  catch (VRTException e) {
    UNUSED_VARIABLE(e);
    assertEquals("setField(..) is unsupported", true, true);
  }

  try {
    ts.setFieldByName("UTCSeconds", Value(123));
    assertEquals("setFieldByName(..) is unsupported", true, false);
  }
  catch (VRTException e) {
    UNUSED_VARIABLE(e);
    assertEquals("setFieldByName(..) is unsupported", true, true);
  }
}
