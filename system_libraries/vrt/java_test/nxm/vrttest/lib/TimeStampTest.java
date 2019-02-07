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

package nxm.vrttest.lib;

import java.util.ArrayList;
import java.util.List;
import nxm.vrt.lib.LeapSeconds;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.TimeStamp.FractionalMode;
import nxm.vrt.lib.TimeStamp.IntegerMode;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static nxm.vrttest.inc.TestRunner.assertEquals;
import static nxm.vrttest.lib.HasFieldsTest.checkGetFieldName;

/** Tests for {@link TimeStamp}. */
public class TimeStampTest implements Testable {
  private static final int GPS_JUL1985LS =  173059200; //1985-07-01:00:00:00 GPS time
  private static final int UTC_JUL1985LS =  489024015; //1985-07-01:00:00:00 UTC time
  private static final int GPS_DEC2005LS =  820108800; //2006-01-01:00:00:00 GPS time
  private static final int UTC_DEC2005LS = 1136073625; //2006-01-01:00:00:00 UTC time

  @Override
  public Class<?> getTestedClass () {
    return TimeStamp.class;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();
    tests.add(new TestSet("TimeStamp(..)",            this,  "testTimeStamp"));
    tests.add(new TestSet("addPicoSeconds(..)",       this,  "testAddPicoSeconds"));
    tests.add(new TestSet("addSeconds(..)",           this,  "testAddSeconds"));
    tests.add(new TestSet("addTime(..)",              this,  "testAddTime"));
    tests.add(new TestSet("compareTo(..)",            this,  "testCompareTo"));
    tests.add(new TestSet("equals(..)",               this,  "testEquals"));
    tests.add(new TestSet("forNoradTime(..)",         this, "-"));  //function is deprecated
    tests.add(new TestSet("forTime(..)",              this,  "testForTime"));
    tests.add(new TestSet("forTimeIRIG(..)",          this,  "testForTimeIRIG"));
    tests.add(new TestSet("forTimeMidas(..)",         this,  "testForTimeMidas"));
    tests.add(new TestSet("forTimeNORAD(..)",         this, "+testForTimeIRIG"));
    tests.add(new TestSet("forTimePOSIX(..)",         this,  "testForTimePOSIX"));
    tests.add(new TestSet("forTimePTP(..)",           this,  "testForTimePTP"));
    tests.add(new TestSet("getEpoch()",               this,  "testGetEpoch"));
    tests.add(new TestSet("getField(..)",             this,  "testGetField"));
    tests.add(new TestSet("getFieldByName(..)",       this, "+testGetField"));
    tests.add(new TestSet("getFieldCount()",          this, "+testGetFieldName"));
    tests.add(new TestSet("getFieldID(..)",           this, "+testGetFieldName"));
    tests.add(new TestSet("getFieldName(..)",         this,  "testGetFieldName"));
    tests.add(new TestSet("getFieldType(..)",         this, "+testGetFieldName"));
    tests.add(new TestSet("getFractionalMode()",      this, "+testTimeStamp"));
    tests.add(new TestSet("getFractionalSeconds()",   this,  "testGetFractionalSeconds"));
    tests.add(new TestSet("getSecondsGPS()",          this, "+testGetGPSSeconds"));
    tests.add(new TestSet("getGPSSeconds()",          this,  "testGetGPSSeconds"));
    tests.add(new TestSet("getIntegerMode()",         this, "+testTimeStamp"));
    tests.add(new TestSet("getLeapSecondRef()",       this, "+testTimeStamp"));
    tests.add(new TestSet("getMidasSeconds()",        this,  "testGetMidasSeconds"));
    tests.add(new TestSet("getMidasTime()",           this, "+testGetMidasTime"));
    tests.add(new TestSet("getNORADSeconds()",        this,  "testGetNORADSeconds"));
    tests.add(new TestSet("getPOSIXSeconds()",        this,  "testGetPOSIXSeconds"));
    tests.add(new TestSet("getPicoSeconds()",         this,  "testGetPicoSeconds"));
    tests.add(new TestSet("getSecondsNORAD()",        this, "+testGetNORADSeconds"));
    tests.add(new TestSet("getSecondsPOSIX()",        this, "+testGetPOSIXSeconds"));
    tests.add(new TestSet("getSecondsUTC()",          this, "+testGetUTCSeconds"));
    tests.add(new TestSet("getSystemTime()",          this,  "testGetSystemTime"));
    tests.add(new TestSet("getTimeStampFractional()", this, "+testTimeStamp"));
    tests.add(new TestSet("getTimeStampInteger()",    this,  "testTimeStamp"));
    tests.add(new TestSet("getUTCSeconds()",          this,  "testGetUTCSeconds"));
    tests.add(new TestSet("hashCode()",               this,  "testHashCode"));
    tests.add(new TestSet("parseTime(..)",            this,  "testParseTime"));
    tests.add(new TestSet("setField(..)",             this,  "testSetField"));
    tests.add(new TestSet("setFieldByName(..)",       this, "+testSetField"));
    tests.add(new TestSet("toGPS()",                  this,  "testToGPS"));
    tests.add(new TestSet("toString()",               this, "+"));
    tests.add(new TestSet("toStringGPS()",            this,  "testToStringGPS"));
    tests.add(new TestSet("toStringUTC()",            this,  "testToStringUTC"));
    tests.add(new TestSet("toUTC()",                  this,  "testToUTC"));

    tests.add(new TestSet("XXX()",                    this,  "testXXX"));
    return tests;
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }

  @Override
  public String toString () {
    return "Tests for TimeStamp";
  }

  public void testAddTime () {
    TimeStamp t1 = new TimeStamp(IntegerMode.UTC,0,0);
    TimeStamp t2 = new TimeStamp(IntegerMode.GPS,0,0);

    t1 = t1.addTime(1234567890, 1234567890);
    t2 = t2.addTime(1234567890, 1234567890);
    assertEquals("UTC 1234567890 seconds",     1234567890L, t1.getUTCSeconds());
    assertEquals("GPS 1234567890 seconds",     1234567890L, t2.getGPSSeconds());
    assertEquals("UTC 1234567890 picoseconds", 1234567890L, t1.getPicoSeconds());
    assertEquals("GPS 1234567890 picoseconds", 1234567890L, t2.getPicoSeconds());

    t1 = t1.addTime(-234567891, -234567891);
    t2 = t2.addTime(-234567891, -234567891);

    assertEquals("UTC 1000000000 seconds",     999999999L, t1.getSecondsUTC());
    assertEquals("GPS 1000000000 seconds",     999999999L, t2.getSecondsGPS());
    assertEquals("UTC 1000000000 picoseconds", 999999999L, t1.getPicoSeconds());
    assertEquals("GPS 1000000000 picoseconds", 999999999L, t2.getPicoSeconds());
  }

  public void testCompareTo () {
    TimeStamp u1 = new TimeStamp(IntegerMode.UTC,0,0);
    TimeStamp g1 = new TimeStamp(IntegerMode.GPS,0,0);
    TimeStamp u2 = new TimeStamp(IntegerMode.UTC,0,0);
    TimeStamp g2 = new TimeStamp(IntegerMode.GPS,0,0);

    assertEquals("UTC == UTC",0,u1.compareTo(u2));                      //u1=0, u2=0
    u1 = u1.addSeconds(1);
    assertEquals("UTC > UTC",1,u1.compareTo(u2));                       //u1=1, u2=0
    u2 = u1.addSeconds(2);
    assertEquals("UTC < UTC",-1,u1.compareTo(u2));                      //u1=1, u2=2

    assertEquals("GPS == GPS",0,g1.compareTo(g2));                      //g1=0, g2=0
    g1 = g1.addSeconds(1);
    assertEquals("GPS > GPS",1,g1.compareTo(g2));                       //g1=1, g2=0
    g2 = g2.addSeconds(2);
    assertEquals("GPS < GPS",-1,g1.compareTo(g2));                      //g1=1, g2=2

    u1 = u1.addSeconds(315964811);
    assertEquals("UTC == GPS",0,u1.compareTo(g1));                      //u1=(1)  g1=1
    assertEquals("UTC < GPS",-1,u1.compareTo(g2));                      //u1=(1), g2=2
    u1 = u1.addSeconds(1);
    assertEquals("UTC > GPS",1,u1.compareTo(g1));                       //u1=(2), g1=1

    assertEquals("GPS == UTC",0,g1.addSeconds(1).compareTo(u1));        //2 == 2
    assertEquals("GPS > UTC",1,g1.addSeconds(2).compareTo(u1));         //3 > 2
    assertEquals("GPS < UTC",-1,g1.compareTo(u1));                      // 1 < 2
  }

  public void testAddPicoSeconds () {
    // wrap around a second in the positive direction
    TimeStamp t1 = new TimeStamp(IntegerMode.UTC,0,0);
    TimeStamp t2 = new TimeStamp(IntegerMode.GPS,0,0);

    for (int i = 0; i < 65536; i++) {
      long expSec = (1234567890L * i) / 1000000000000L;
      long expPS  = (1234567890L * i) % 1000000000000L;

      assertEquals("UTC whole i="+i,      expSec, t1.getUTCSeconds());
      assertEquals("GPS whole i="+i,      expSec, t2.getGPSSeconds());
      assertEquals("UTC fractional i="+i, expPS,  t1.getPicoSeconds());
      assertEquals("GPS fractional i="+i, expPS,  t2.getPicoSeconds());
      t1 = t1.addPicoSeconds(1234567890);
      t2 = t2.addPicoSeconds(1234567890);
    }

    // wrap around negative
    t1 = new TimeStamp(IntegerMode.UTC,1,0);
    t2 = new TimeStamp(IntegerMode.GPS,1,0);
    t1 = t1.addPicoSeconds(-1);
    t2 = t2.addPicoSeconds(-1);
    assertEquals("UTC negative wrap fractional", 999999999999L, t1.getPicoSeconds());
    assertEquals("GPS negative wrap fractional", 999999999999L, t2.getPicoSeconds());
    assertEquals("UTC negative wrap whole",                 0,  t1.getSecondsUTC());
    assertEquals("GPS negative wrap whole",                 0,  t2.getSecondsGPS());

    // negative maximum wrap
    t1 = new TimeStamp(IntegerMode.UTC,0,0);
    t2 = new TimeStamp(IntegerMode.GPS,0,0);
    t1 = t1.addPicoSeconds(-1);
    t2 = t2.addPicoSeconds(-1);
    assertEquals("UTC max negative", 999999999999L, t1.getPicoSeconds());
    assertEquals("GPS max negative", 999999999999L, t2.getPicoSeconds());
  }


  public void testAddSeconds () {
    TimeStamp t1 = new TimeStamp(IntegerMode.UTC,0,0);
    TimeStamp t2 = new TimeStamp(IntegerMode.GPS,0,0);

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

  public void testEquals () {
    TimeStamp u1 = new TimeStamp(IntegerMode.UTC,1000000000,0);
    TimeStamp g1 = new TimeStamp(IntegerMode.GPS,1000000000,0);
    TimeStamp u2 = new TimeStamp(IntegerMode.UTC,1000000000,0);
    TimeStamp g2 = new TimeStamp(IntegerMode.GPS,1000000000,0);

    assertEquals("UTC = UTC",true,u1.equals(u2));
    assertEquals("GPS = GPS",true,g1.equals(g2));
    assertEquals("UTC != GPS",false,u1.equals(g2));
    assertEquals("GPS != UTC",false,g1.equals(u2));
    u2 = u2.addSeconds(1);
    g2 = g2.addSeconds(1);
    assertEquals("UTC != UTC",false,u1.equals(u2));
    assertEquals("GPS != GPS",false,g1.equals(g2));
  }

  public void testForTime () {
    TimeStamp t1 = TimeStamp.forTime(1985,7,1,0,0,0,0,0,IntegerMode.UTC);
    TimeStamp t2 = TimeStamp.forTime(1985,7,1,0,0,0,0,0,IntegerMode.GPS);
    assertEquals("Basic UTC",UTC_JUL1985LS,t1.getSecondsUTC());
    assertEquals("Basic GPS",GPS_JUL1985LS,t2.getSecondsGPS());

    t1 = TimeStamp.forTime(1985,6,30,23,59,59,0,0,IntegerMode.UTC);
    t2 = TimeStamp.forTime(1985,6,30,23,59,59,0,0,IntegerMode.GPS);
    assertEquals("UTC Leap Second",UTC_JUL1985LS-2,t1.getSecondsUTC());
    assertEquals("GPS Leap Second",GPS_JUL1985LS-1,t2.getSecondsGPS());

    t1 = TimeStamp.forTime(1985,6,30,23,59,61,0,0,IntegerMode.UTC);
    t2 = TimeStamp.forTime(1985,6,30,23,59,61,0,0,IntegerMode.GPS);
    assertEquals("UTC Wrap Seconds",UTC_JUL1985LS+1,t1.getSecondsUTC());
    assertEquals("GPS Wrap Seconds",GPS_JUL1985LS+1,t2.getSecondsGPS());

    t1 = TimeStamp.forTime(1985,6,30,23,60,61,0,0,IntegerMode.UTC);
    t2 = TimeStamp.forTime(1985,6,30,23,60,61,0,0,IntegerMode.GPS);
    assertEquals("UTC Wrap Minutes and Seconds",UTC_JUL1985LS+61,t1.getSecondsUTC());
    assertEquals("GPS Wrap Minutes and Seconds",GPS_JUL1985LS+61,t2.getSecondsGPS());

    t1 = TimeStamp.forTime(1985,6,30,27,60,61,0,0,IntegerMode.UTC);
    t2 = TimeStamp.forTime(1985,6,30,27,60,61,0,0,IntegerMode.GPS);
    assertEquals("UTC Wrap Hours Minutes and Seconds",UTC_JUL1985LS+14461,t1.getSecondsUTC());
    assertEquals("GPS Wrap Hours Minutes and Seconds",GPS_JUL1985LS+14461,t2.getSecondsGPS());

    t1 = TimeStamp.forTime(1985,6,40,27,60,61,0,0,IntegerMode.UTC);
    t2 = TimeStamp.forTime(1985,6,40,27,60,61,0,0,IntegerMode.GPS);
    assertEquals("UTC Wrap Days Hours Minutes and Seconds",UTC_JUL1985LS+878461,t1.getSecondsUTC());
    assertEquals("GPS Wrap Days Hours Minutes and Seconds",GPS_JUL1985LS+878461,t2.getSecondsGPS());
  }

  public void testForTimeIRIG () {
    // Note that we specifically use 1 Feb of the current year to avoid strangeness
    // with the end-of-year/start-of-year logic (which we should eventually test too).
    String      now  = TimeStamp.getSystemTime().toStringUTC();
    int         year = Integer.parseInt(now.substring(0,4));
    TimeStamp   feb1 = TimeStamp.forTime(year, 2, 1, 0, 0, 0, 0, 0, TimeStamp.UTC_EPOCH);
    LeapSeconds ls   = LeapSeconds.getDefaultInstance();

    for (int day = 1; day <= 28; day++) {
      for (int hour = 0; hour < 24; hour+=4) {
        for (int min = 0; min < 60; min+=5) {
          for (int sec = 0; sec < 60; sec+=15) {
            int delta = ((day-1) * 86400)
                      + (hour    *  3600)
                      + (min     *    60)
                      + (sec            );       // Delta from 1 Feb
            int doy   = 31 + day;                // Day of year
            int soy   = delta + (31 * 86400);    // Sec of year

            TimeStamp exp1 = feb1.addSeconds(delta);
            TimeStamp exp2 = exp1.addPicoSeconds(420000000000L);

            // ==== IRIG =======================================================
            TimeStamp act1 = TimeStamp.forTimeIRIG(sec, min, hour, doy);
            TimeStamp act2 = TimeStamp.forTimeIRIG(sec, min, hour, doy, 42);
            assertEquals("forTimeIRIG(sec,min,hour,day)", exp1, act1);
            assertEquals("forTimeIRIG(sec,min,hour,day)", exp2, act2);

            // ==== NORAD ======================================================
            TimeStamp act1n = TimeStamp.forTimeNORAD(soy,            0L);
            TimeStamp act2n = TimeStamp.forTimeNORAD(soy, 420000000000L);
            TimeStamp act3n = TimeStamp.forTimeNORAD(soy,            0L,   -1, ls);
            TimeStamp act4n = TimeStamp.forTimeNORAD(soy, 420000000000L,   -1, ls);
            TimeStamp act5n = TimeStamp.forTimeNORAD(soy,            0L, year, ls);
            TimeStamp act6n = TimeStamp.forTimeNORAD(soy, 420000000000L, year, ls);
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

  public void testForTimeMidas () {
    TimeStamp t1 = TimeStamp.forTimeMidas(TimeStamp.MIDAS2POSIX,0);
    assertEquals("Minimum Time",0,t1.getSecondsUTC());

    //Account for the 15 leap seconds between 1950 and 1985
    t1 = TimeStamp.forTimeMidas(TimeStamp.MIDAS2POSIX+UTC_JUL1985LS-15,0);
    assertEquals("UTC 1985-07-01:00:00:00 Numeric", UTC_JUL1985LS, t1.getSecondsUTC());

    t1 = TimeStamp.forTimeMidas(TimeStamp.MIDAS2POSIX+UTC_JUL1985LS-15,0);
    assertEquals("UTC 1985-07-01:00:00:00 Numeric", UTC_JUL1985LS, t1.getSecondsUTC());

    //Account for the 25 leap seconds between 1950 and 2006
    t1 = TimeStamp.forTimeMidas(TimeStamp.MIDAS2POSIX+UTC_DEC2005LS-25,0);
    assertEquals("UTC 2005-01-01:00:00:00 Numeric", UTC_DEC2005LS, t1.getSecondsUTC());
    //TimeStamp t1 = TimeStamp.forTimeMidas(GPS_JUL1985LS, GPS_JUL1985LS);
  }

  public void testForTimePOSIX () {
    assertEquals("Zero Case",0,TimeStamp.forTimePOSIX(0,0).getSecondsUTC());
    assertEquals("Before UTC 1985 LS",UTC_JUL1985LS-2,TimeStamp.forTimePOSIX(UTC_JUL1985LS-16,0).getSecondsUTC());
    assertEquals("After UTC 1985 LS",UTC_JUL1985LS,TimeStamp.forTimePOSIX(UTC_JUL1985LS-15,0).getSecondsUTC());
  }

  public void testForTimePTP () {
    assertEquals("Zero Case",0,TimeStamp.forTimePTP(315964819,0).getSecondsGPS());
    assertEquals("Before GPS 1985 LS",GPS_JUL1985LS-1,TimeStamp.forTimePTP(315964819+GPS_JUL1985LS-1,0).getSecondsGPS());
    assertEquals("After GPS 1985 LS",GPS_JUL1985LS+1,TimeStamp.forTimePTP(315964819+GPS_JUL1985LS+1,0).getSecondsGPS());
  }

  public void testGetEpoch () {
    TimeStamp t1 = new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS,12345);
    TimeStamp t2 = new TimeStamp(IntegerMode.GPS,0,67890);

    assertEquals("UTC Before conversion",IntegerMode.UTC,t1.getEpoch());
    assertEquals("GPS Before conversion",IntegerMode.GPS,t2.getEpoch());
    t1 = t1.toGPS();
    t2 = t2.toUTC();
    assertEquals("UTC After conversion",IntegerMode.GPS,t1.getEpoch());
    assertEquals("GPS After conversion",IntegerMode.UTC,t2.getEpoch());
  }

  public void testGetFractionalSeconds () {
    TimeStamp t1 = new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS,1234567890);
    assertEquals("Average Case",0.00123456789,t1.getFractionalSeconds());
    assertEquals("0",0,new TimeStamp(IntegerMode.GPS,0,0).getFractionalSeconds());
    for (int i = 0; i < 100; i++) {
      t1 = t1.addPicoSeconds(1000000000);
    }
    assertEquals("Large Case",.10123456789,t1.getFractionalSeconds());
  }

  public void testGetGPSSeconds () {
    TimeStamp t1 = new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS,0);

    assertEquals("GPS 1980 Start", 0,                TimeStamp.forTime(1980,1,6,0,0,0,0,0,IntegerMode.UTC).getSecondsGPS()&0xFFFFFFFF);
    assertEquals("UTC 1985LS",     GPS_JUL1985LS+4,  t1.getSecondsGPS()&0xFFFFFFFF);
    assertEquals("UTC 2006LS",     GPS_DEC2005LS+14, new TimeStamp(IntegerMode.UTC,UTC_DEC2005LS,0).getSecondsGPS()&0xFFFFFFFF);
  }
  public void testGetMidasSeconds () {
    assertEquals("UTC 0",       631152000,                  new TimeStamp(IntegerMode.UTC,0,0).getMidasSeconds());
    assertEquals("1985 LS",     631152000+UTC_JUL1985LS-15, new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS,0).getMidasSeconds());
    assertEquals("1985 LS - 1", 631152000+UTC_JUL1985LS-15, new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS-1,0).getMidasSeconds());
    assertEquals("2005 LS",     631152000+UTC_DEC2005LS-25, new TimeStamp(IntegerMode.UTC,UTC_DEC2005LS,0).getMidasSeconds());
  }
  public void testGetNORADSeconds () {
    assertEquals("UTC 0",                            0, new TimeStamp(IntegerMode.UTC,UTC_DEC2005LS,0).getSecondsNORAD()&0xFFFFFFFF);
    assertEquals("UTC normal case",            1234567, new TimeStamp(IntegerMode.UTC,UTC_DEC2005LS+1234567,0).getSecondsNORAD()&0xFFFFFFFF);
    assertEquals("1985 LS - 1 (not counted)", 15638400, new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS-1,0).getSecondsNORAD(false)&0xFFFFFFFF);
    assertEquals("1985 LS - 1 (counted)",     15638400, new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS-1,0).getSecondsNORAD(true)&0xFFFFFFFF);
    assertEquals("1985 LS (not counted)",     15638400, new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS,0).getSecondsNORAD(false)&0xFFFFFFFF);
    assertEquals("1985 LS (counted)",         15638401, new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS,0).getSecondsNORAD(true)&0xFFFFFFFF);
    assertEquals("1985 LS + 1 (not counted)", 15638401, new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS+1,0).getSecondsNORAD(false)&0xFFFFFFFF);
    assertEquals("1985 LS + 1 (counted)",     15638402, new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS+1,0).getSecondsNORAD(true)&0xFFFFFFFF);
  }

  public void testGetPOSIXSeconds () {
    assertEquals("UTC 0",                     0, new TimeStamp(IntegerMode.UTC,0,0).getSecondsPOSIX()&0xFFFFFFFF);
    //14 seconds difference before the LS
    assertEquals("1985 LS - 2",UTC_JUL1985LS-16, new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS-2,0).getSecondsPOSIX()&0xFFFFFFFF);
    assertEquals("1985 LS - 1",UTC_JUL1985LS-15, new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS-1,0).getSecondsPOSIX()&0xFFFFFFFF);
    //15 seconds difference after the LS
    assertEquals("1985 LS",    UTC_JUL1985LS-15, new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS,0).getSecondsPOSIX()&0xFFFFFFFF);
    assertEquals("1985 LS + 1",UTC_JUL1985LS-14, new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS+1,0).getSecondsPOSIX()&0xFFFFFFFF);
  }

  public void testGetPicoSeconds () {
    TimeStamp t1 = new TimeStamp(IntegerMode.UTC,0,0);
    assertEquals("UTC 0",0,t1.getPicoSeconds());
    for (int i = 1; i < 1500;i++) {
      t1 = t1.addPicoSeconds(1234567890);
      long expSec = (1234567890L * i) / 1000000000000L;
      long expPS  = (1234567890L * i) % 1000000000000L;
      assertEquals("t = "+ i, expSec, t1.getUTCSeconds());
      assertEquals("t = "+ i, expPS,  t1.getPicoSeconds());
    }
  }

  public void testGetUTCSeconds () {
    assertEquals("GPS 0",      315964811,        new TimeStamp(IntegerMode.GPS,0,0).getSecondsUTC()&0xFFFFFFFF);
    assertEquals("GPS 1985LS", UTC_JUL1985LS-4,  new TimeStamp(IntegerMode.GPS,GPS_JUL1985LS,0).getSecondsUTC()&0xFFFFFFFF);
    assertEquals("GPS 2005LS", UTC_DEC2005LS-14, new TimeStamp(IntegerMode.GPS,GPS_DEC2005LS,0).getSecondsUTC()&0xFFFFFFFF);
    assertEquals("UTC 0",      0,                new TimeStamp(IntegerMode.UTC,0,0).getSecondsUTC()&0xFFFFFFFF);
    assertEquals("UTC 1985LS", UTC_JUL1985LS,    new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS,0).getSecondsUTC()&0xFFFFFFFF);
    assertEquals("UTC 2005LS", UTC_DEC2005LS,    new TimeStamp(IntegerMode.UTC,UTC_DEC2005LS,0).getSecondsUTC()&0xFFFFFFFF);
  }

  public void testGetSystemTime () {
    // The basic test is done in ms since that is what the system clock uses,
    // however allow for a 100ms tolerance since there is some non-negligible
    // code between the call to get the system time in getSystemTime() and the
    // one below.
    TimeStamp t1  = TimeStamp.getSystemTime();
    long      exp = System.currentTimeMillis();
    long      act = (t1.getPOSIXSeconds() * 1000)
                  + (t1.getPicoSeconds() / 1000000000);
    assertEquals("System Time Test", exp, act, 100);
  }

  public void testHashCode () {
    TimeStamp t1 = new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS,12345);
    TimeStamp t2 = new TimeStamp(IntegerMode.GPS,GPS_JUL1985LS,67890);
    assertEquals("UTC 1985",new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS,12345).hashCode(),t1.hashCode());
    assertEquals("GPS 1985",new TimeStamp(IntegerMode.GPS,GPS_JUL1985LS,67890).hashCode(),t2.hashCode());
  }

  public void testParseTime () {
    assertEquals("GPS 0",           0,               TimeStamp.parseTime("1980-01-06T00:00:00.000000000000Z", IntegerMode.GPS).getGPSSeconds());
    assertEquals("GPS 1985 LS - 1", GPS_JUL1985LS-1, TimeStamp.parseTime("1985-06-30T23:59:59.000000000000Z", IntegerMode.GPS).getGPSSeconds());
    assertEquals("GPS 1985 LS",     GPS_JUL1985LS,   TimeStamp.parseTime("1985-07-01T00:00:00.000000000000Z", IntegerMode.GPS).getGPSSeconds());
    assertEquals("GPS 1985 LS + 1", GPS_JUL1985LS+1, TimeStamp.parseTime("1985-07-01T00:00:01.000000000000Z", IntegerMode.GPS).getGPSSeconds());

    assertEquals("UTC 0",           0,               TimeStamp.parseTime("1970-01-01T00:00:00.000000000000Z", IntegerMode.UTC).getUTCSeconds());
    assertEquals("UTC 1985 LS - 2", UTC_JUL1985LS-2, TimeStamp.parseTime("1985-06-30T23:59:59.000000000000Z", IntegerMode.UTC).getUTCSeconds());
    assertEquals("UTC 1985 LS - 1", UTC_JUL1985LS-1, TimeStamp.parseTime("1985-06-30T23:59:60.000000000000Z", IntegerMode.UTC).getUTCSeconds());
    assertEquals("UTC 1985 LS",     UTC_JUL1985LS,   TimeStamp.parseTime("1985-07-01T00:00:00.000000000000Z", IntegerMode.UTC).getUTCSeconds());
    assertEquals("UTC 1985 LS + 1", UTC_JUL1985LS+1, TimeStamp.parseTime("1985-07-01T00:00:01.000000000000Z", IntegerMode.UTC).getUTCSeconds());

    assertEquals("GPS Picoseconds", 123456789000L,   TimeStamp.parseTime("1985-07-01T00:00:01.123456789000Z", IntegerMode.GPS).getPicoSeconds());
    assertEquals("UTC Picoseconds", 123456789000L,   TimeStamp.parseTime("1985-07-01T00:00:01.123456789000Z", IntegerMode.UTC).getPicoSeconds());
  }

  public void testTimeStamp () {
    TimeStamp t1 = new TimeStamp(IntegerMode.UTC,0,0);
    TimeStamp t2 = new TimeStamp(IntegerMode.GPS,0,0);
    TimeStamp t3 = new TimeStamp(IntegerMode.GPS,GPS_JUL1985LS,123456789);

    assertEquals("UTC 0 tsi",          0,                       t1.getTimeStampInteger());
    assertEquals("UTC 0 tsf",          0,                       t1.getTimeStampFractional());
    assertEquals("UTC 0 tsimode",      IntegerMode.UTC,         t1.getIntegerMode());
    assertEquals("UTC 0 tsfmode",      FractionalMode.RealTime, t1.getFractionalMode());

    assertEquals("GPS 0 tsi",          0,                       t2.getTimeStampInteger());
    assertEquals("GPS 0 tsf",          0,                       t2.getTimeStampFractional());
    assertEquals("GPS 0 tsimode",      IntegerMode.GPS,         t2.getIntegerMode());
    assertEquals("GPS 0 tsfmode",      FractionalMode.RealTime, t2.getFractionalMode());

    assertEquals("GPS 1985LS tsi",     GPS_JUL1985LS,           t3.getTimeStampInteger());
    assertEquals("GPS 1985LS tsf",     123456789,               t3.getTimeStampFractional());
    assertEquals("GPS 1985LS tsimode", IntegerMode.GPS,         t3.getIntegerMode());
    assertEquals("GPS 1985LS tsfmode", FractionalMode.RealTime, t3.getFractionalMode());
  }


  public void testToUTC () {
    TimeStamp t1 = new TimeStamp(IntegerMode.GPS,0,0);
    t1 = t1.toUTC();
    assertEquals("0:(GPS2UTC)",315964811,t1.getSecondsUTC());

    t1 = new TimeStamp(IntegerMode.GPS,GPS_JUL1985LS,0);
    t1 = t1.toUTC();

    //at this point, UTC is 4 seconds "behind" GPS in terms of displayed time
    assertEquals("1985-07-01:00:00:00 numeric",UTC_JUL1985LS-4,t1.getSecondsUTC());

    //at this point, UTC is 4 seconds "behind" GPS, so including the :60 leap second the clock should read :57
    assertEquals("1985-07-01:00:00:00 string","1985-06-30T23:59:57.000000000000Z",t1.toStringUTC());
    TimeStamp t2 = new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS,0);
    t2 = t2.toUTC();
    assertEquals("UTC to UTC",UTC_JUL1985LS,t2.getSecondsUTC());

    t1 = new TimeStamp(IntegerMode.UTC,1234567890,0);
    t1 = t1.toGPS();
    t1 = t1.toUTC();
    assertEquals("Coversion to GPS and back to UTC",1234567890,t1.getSecondsUTC());
  }

  public void testToGPS () {
    TimeStamp t1 = new TimeStamp(IntegerMode.UTC,315964811,0);
    t1.toGPS();
    assertEquals("(GPS2UTC):0",0,t1.getSecondsGPS());

    t1 = new TimeStamp(IntegerMode.GPS,1234567890,0);
    t1 = t1.toUTC();
    t1 = t1.toGPS();
    assertEquals("Coversion to UTC and back to GPS",1234567890,t1.getSecondsGPS());

    TimeStamp t2 = new TimeStamp(IntegerMode.GPS,GPS_JUL1985LS,0);
    t2 = t2.toGPS();
    assertEquals("GPS to GPS",GPS_JUL1985LS,t2.getSecondsGPS());

    t1 = new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS,0);
    t2 = new TimeStamp(IntegerMode.UTC,UTC_DEC2005LS,0);
    t1 = t1.toGPS();
    t2 = t2.toGPS();

    //after 1985 LS, GPS displayed time is 4 seconds ahead of UTC
    assertEquals("1985-07-01:00:00:00 numeric",GPS_JUL1985LS+4,t1.getSecondsGPS());
    assertEquals("1985-07-01:00:00:00 string","1985-07-01T00:00:04.000000000000Z",t1.toStringGPS());

    //after 2005/2006 LS, GPS displayed time is 14 seconds ahead of UTC
    assertEquals("2006-01-01:00:00:00 numeric",GPS_DEC2005LS+14,t2.getSecondsGPS());
    assertEquals("2006-01-01:00:00:00 string","2006-01-01T00:00:14.000000000000Z",t2.toStringGPS());
  }

  public void testToStringUTC () {
    TimeStamp t1 = new TimeStamp(IntegerMode.UTC,0,0);
    assertEquals("Output Test t=11","1970-01-01T00:00:00.000000000000Z",t1.toStringUTC());

    t1 = new TimeStamp(IntegerMode.UTC,UTC_JUL1985LS-11,0);            //June 1985 leap second
    TimeStamp t2 = new TimeStamp(IntegerMode.UTC,UTC_DEC2005LS-11,0);
    assertEquals("Constructor test",UTC_JUL1985LS-11,t1.getUTCSeconds());

    for (int i = 50; i <= 60; i++) {
      String exp1 = String.format("1985-06-30T23:59:%d.000000000000Z", i);
      String exp2 = String.format("2005-12-31T23:59:%d.000000000000Z", i);
      assertEquals("Output Test t="+i,exp1,t1.toStringUTC());
      assertEquals("Output Test t="+i,exp2,t2.toStringUTC());
      t1 = t1.addSeconds(1);
      t2 = t2.addSeconds(1);
    }
    assertEquals("Output Test t=11","1985-07-01T00:00:00.000000000000Z",t1.toStringUTC());
    assertEquals("Output Test t=11","2006-01-01T00:00:00.000000000000Z",t2.toStringUTC());

    //test picoseconds across the 1985 leap second
    t1 = t1.addSeconds(-1);
    for (int i = 0; i < 1500; i++) {
      long   ps  = (1234567890L * i);
      String exp;

      if (ps < 1000000000000L) {
        exp = String.format("1985-06-30T23:59:60.%012dZ", ps % 1000000000000L);
      }
      else {
        exp = String.format("1985-07-01T00:00:00.%012dZ", ps % 1000000000000L);
      }

      assertEquals("Output Test i="+i,exp, t1.toStringUTC());
      t1 = t1.addPicoSeconds(1234567890);
    }
  }

  public void testToStringGPS () {
    TimeStamp t1 = new TimeStamp(IntegerMode.GPS,0,0);
    assertEquals("Constructor test",0,t1.getGPSSeconds());
    assertEquals("Constructor test","1980-01-06T00:00:00.000000000000Z",t1.toStringGPS());

    t1 = new TimeStamp(IntegerMode.GPS,GPS_JUL1985LS-10,0);            //June 1985 leap second
    TimeStamp t2 = new TimeStamp(IntegerMode.GPS,GPS_DEC2005LS-10,0);

    for (int i = 50; i <= 59; i++) {
      String exp1 = String.format("1985-06-30T23:59:%d.000000000000Z", i);
      String exp2 = String.format("2005-12-31T23:59:%d.000000000000Z", i);
      assertEquals("Output Test 1985 t="+i,exp1,t1.toStringGPS());
      assertEquals("Output Test 2005 t="+i,exp2,t2.toStringGPS());
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
    for (int i = 0; i < 1500; i++) {
      long   ps  = (1234567890L * i);
      String exp;

      if (ps < 1000000000000L) {
        exp = String.format("1985-07-01T00:00:00.%012dZ", ps % 1000000000000L);
      }
      else {
        exp = String.format("1985-07-01T00:00:01.%012dZ", ps % 1000000000000L);
      }

      assertEquals("Output Test i="+i,exp, t1.toStringGPS());
      t1 = t1.addPicoSeconds(1234567890);
    }
  }

  private final int        FIELD_OFFSET = 0;
  private final String[]   FIELD_NAMES  = {
    "String",       "StringUTC",         "StringGPS",        "Epoch",
    "IntegerMode",  "FractionalMode",    "UTCSeconds",       "GPSSeconds",
    "NORADSeconds", "POSIXSeconds",      "MidasTime",        "MidasSeconds",
    "PicoSeconds",  "FractionalSeconds", "TimeStampInteger", "TimeStampFractional"
  };
  private final Class<?>[] FIELD_TYPES  = {
    String.class,      String.class,         String.class, IntegerMode.class,
    IntegerMode.class, FractionalMode.class, Long.TYPE,    Long.TYPE,
    Long.TYPE,         Long.TYPE,            Double.class, Double.class,
    Long.class,        Double.class,         Long.class,   Long.class
  };

  public void testGetFieldName () {
    TimeStamp ts = TimeStamp.getSystemTime();
    checkGetFieldName(ts, FIELD_OFFSET, FIELD_NAMES, FIELD_TYPES);
  }

  public void testGetField () {
    TimeStamp ts = TimeStamp.getSystemTime();
    assertEquals("getField( 0)", ts.toString(),               ts.getField( 0));
    assertEquals("getField( 1)", ts.toStringUTC(),            ts.getField( 1));
    assertEquals("getField( 2)", ts.toStringGPS(),            ts.getField( 2));
    assertEquals("getField( 3)", ts.getEpoch(),               ts.getField( 3));
    assertEquals("getField( 4)", ts.getIntegerMode(),         ts.getField( 4));
    assertEquals("getField( 5)", ts.getFractionalMode(),      ts.getField( 5));
    assertEquals("getField( 6)", ts.getUTCSeconds(),          ts.getField( 6));
    assertEquals("getField( 7)", ts.getGPSSeconds(),          ts.getField( 7));
    assertEquals("getField( 8)", ts.getNORADSeconds(),        ts.getField( 8));
    assertEquals("getField( 9)", ts.getPOSIXSeconds(),        ts.getField( 9));
    assertEquals("getField(10)", ts.getMidasTime(),           ts.getField(10));
    assertEquals("getField(11)", ts.getMidasSeconds(),        ts.getField(11));
    assertEquals("getField(12)", ts.getPicoSeconds(),         ts.getField(12));
    assertEquals("getField(13)", ts.getFractionalSeconds(),   ts.getField(13));
    assertEquals("getField(14)", ts.getTimeStampInteger(),    ts.getField(14));
    assertEquals("getField(15)", ts.getTimeStampFractional(), ts.getField(15));

    for (int i = 0; i < FIELD_NAMES.length; i++) {
      int    id   = i + FIELD_OFFSET;
      String name = FIELD_NAMES[i];
      assertEquals("getFieldByName("+name+")", ts.getField(id), ts.getFieldByName(name));
    }
  }

  public void testSetField () {
    // TimeStamp fields are read-only so make sure the set methods fail
    TimeStamp ts = TimeStamp.getSystemTime();

    try {
      ts.setField(6, 123);
      assertEquals("setField(..) is unsupported", true, false);
    }
    catch (UnsupportedOperationException e) {
      assertEquals("setField(..) is unsupported", true, true);
    }

    try {
      ts.setFieldByName("UTCSeconds", 123);
      assertEquals("setFieldByName(..) is unsupported", true, false);
    }
    catch (UnsupportedOperationException e) {
      assertEquals("setFieldByName(..) is unsupported", true, true);
    }
  }

  public void testXXX () {
    TimeStamp t1 = TimeStamp.forTime(2015,6,30,23,59,59,0,0,IntegerMode.UTC);
    TimeStamp t2 = t1.toGPS();
    System.out.println("============================================================");
    System.out.println("UTC   = "+t1.getUTCSeconds()+" = "+t1.toStringUTC());
    System.out.println("GPS   = "+t1.getGPSSeconds()+" = "+t1.toStringGPS());
    System.out.println("POSIX = "+t1.getPOSIXSeconds());
    System.out.println("============================================================");
    
    t1 = TimeStamp.forTime(2000,1,01,00,00,00,0,0,IntegerMode.UTC);
    System.out.println("============================================================");
    System.out.println("UTC   = "+t1.getUTCSeconds()+" = "+t1.toStringUTC());
    System.out.println("GPS   = "+t1.getGPSSeconds()+" = "+t1.toStringGPS());
    System.out.println("POSIX = "+t1.getPOSIXSeconds());
    System.out.println("============================================================");
    
    t1 = TimeStamp.forTime(2015,1,01,00,00,00,0,0,IntegerMode.UTC);
    System.out.println("============================================================");
    System.out.println("UTC   = "+t1.getUTCSeconds()+" = "+t1.toStringUTC());
    System.out.println("GPS   = "+t1.getGPSSeconds()+" = "+t1.toStringGPS());
    System.out.println("POSIX = "+t1.getPOSIXSeconds());
    System.out.println("============================================================");
  }
}
