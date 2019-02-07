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

#include "LeapSecondsTest.h"
#include "LeapSeconds.h"
#include "TestRunner.h"

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

const int32_t UTC_JUL1985LS =  489024015; //1985-07-01:00:00:00 UTC time
const int32_t UTC_DEC2005LS = 1136073625; //2006-01-01:00:00:00 UTC time
const int32_t J1970TOJ1950  =  631152000; // (7305 days) * (86,400 sec/day);

string LeapSecondsTest::getTestedClass () {
  return "vrt::LeapSeconds";
}

vector<TestSet> LeapSecondsTest::init () {
  vector<TestSet> tests;
  tests.push_back(TestSet("getDefaultInstance()",    this, "testGetDefaultInstance"));
  tests.push_back(TestSet("getInstance()",           this, "-getInstance"));
  tests.push_back(TestSet("getLeapSeconds(..)",      this, "testGetLeapSeconds"));
  tests.push_back(TestSet("getLeapSecondsPOSIX(..)", this, "testGetLeapSecondsPOSIX"));
  tests.push_back(TestSet("getLeapSecondsUTC(..)",   this, "testGetLeapSecondsUTC"));
  tests.push_back(TestSet("setDefaultInstance(..)",  this, "-testSetDefaultInstance"));
  tests.push_back(TestSet("isLeapYear(..)",          this,  "testIsLeapYear"));
  tests.push_back(TestSet("toStringGPS(..)",         this,  "testToStringGPS"));
  return tests;
}

void LeapSecondsTest::done () {
  // nothing to do
}

void LeapSecondsTest::call (const string &func) {
  if (func == "testGetDefaultInstance" ) testGetDefaultInstance();
  if (func == "testGetLeapSeconds"     ) testGetLeapSeconds();
  if (func == "testGetLeapSecondsUTC"  ) testGetLeapSecondsUTC();
  if (func == "testGetLeapSecondsPOSIX") testGetLeapSecondsPOSIX();
  if (func == "testIsLeapYear"         ) testIsLeapYear();
  if (func == "testToStringGPS"        ) testToStringGPS();
}

void LeapSecondsTest::testGetDefaultInstance () {
  LeapSeconds *ls = LeapSeconds::getDefaultInstance();
  assertEquals("Got a default instance", true, !isNull(ls));
}

void LeapSecondsTest::testGetLeapSeconds () {
  LeapSeconds *ls = LeapSeconds::getDefaultInstance();
  assertEquals("1970",      0.0, ls->getLeapSeconds(J1970TOJ1950,0));
  assertEquals("1985 - 1", 14.0, ls->getLeapSeconds(J1970TOJ1950+UTC_JUL1985LS - 16,0));
  assertEquals("1985",     15.0, ls->getLeapSeconds(J1970TOJ1950+UTC_JUL1985LS - 15,0));
  assertEquals("1985 - 1", 24.0, ls->getLeapSeconds(J1970TOJ1950+UTC_DEC2005LS - 26,0));
  assertEquals("2005",     25.0, ls->getLeapSeconds(J1970TOJ1950+UTC_DEC2005LS - 25,0));
}

void LeapSecondsTest::testGetLeapSecondsUTC () {
  LeapSeconds *ls = LeapSeconds::getDefaultInstance();
  assertEquals("UTC 0",     0, ls->getLeapSecondsUTC(0));
  assertEquals("UTC 1985", 15, ls->getLeapSecondsUTC(UTC_JUL1985LS));
  assertEquals("UTC 2005", 25, ls->getLeapSecondsUTC(UTC_DEC2005LS));
}

void LeapSecondsTest::testGetLeapSecondsPOSIX () {
  LeapSeconds *ls = LeapSeconds::getDefaultInstance();
  assertEquals("POSIX 0",         0, ls->getLeapSecondsPOSIX(0));
  assertEquals("POSIX 1985 - 1", 14, ls->getLeapSecondsPOSIX(UTC_JUL1985LS-16));
  assertEquals("POSIX 1985",     15, ls->getLeapSecondsPOSIX(UTC_JUL1985LS-15));
  assertEquals("POSIX 2005 - 1", 24, ls->getLeapSecondsPOSIX(UTC_DEC2005LS-26));
  assertEquals("POSIX 2005",     25, ls->getLeapSecondsPOSIX(UTC_DEC2005LS-25));
}

void LeapSecondsTest::testIsLeapYear () {
  assertEquals("isLeapYear(1970)", false, LeapSeconds::isLeapYear(1970));
  assertEquals("isLeapYear(1971)", false, LeapSeconds::isLeapYear(1971));
  assertEquals("isLeapYear(1972)", true,  LeapSeconds::isLeapYear(1972));
  assertEquals("isLeapYear(1973)", false, LeapSeconds::isLeapYear(1973));
  assertEquals("isLeapYear(1974)", false, LeapSeconds::isLeapYear(1974));
  assertEquals("isLeapYear(1975)", false, LeapSeconds::isLeapYear(1975));
  assertEquals("isLeapYear(1976)", true,  LeapSeconds::isLeapYear(1976));
  assertEquals("isLeapYear(1977)", false, LeapSeconds::isLeapYear(1977));
  assertEquals("isLeapYear(1978)", false, LeapSeconds::isLeapYear(1978));
  assertEquals("isLeapYear(1979)", false, LeapSeconds::isLeapYear(1979));
  assertEquals("isLeapYear(1980)", true,  LeapSeconds::isLeapYear(1980));
  assertEquals("isLeapYear(1981)", false, LeapSeconds::isLeapYear(1981));
  assertEquals("isLeapYear(1982)", false, LeapSeconds::isLeapYear(1982));
  assertEquals("isLeapYear(1983)", false, LeapSeconds::isLeapYear(1983));
  assertEquals("isLeapYear(1984)", true,  LeapSeconds::isLeapYear(1984));
  assertEquals("isLeapYear(1985)", false, LeapSeconds::isLeapYear(1985));
  assertEquals("isLeapYear(1986)", false, LeapSeconds::isLeapYear(1986));
  assertEquals("isLeapYear(1987)", false, LeapSeconds::isLeapYear(1987));
  assertEquals("isLeapYear(1988)", true,  LeapSeconds::isLeapYear(1988));
  assertEquals("isLeapYear(1989)", false, LeapSeconds::isLeapYear(1989));
  assertEquals("isLeapYear(1990)", false, LeapSeconds::isLeapYear(1990));
  assertEquals("isLeapYear(1991)", false, LeapSeconds::isLeapYear(1991));
  assertEquals("isLeapYear(1992)", true,  LeapSeconds::isLeapYear(1992));
  assertEquals("isLeapYear(1993)", false, LeapSeconds::isLeapYear(1993));
  assertEquals("isLeapYear(1994)", false, LeapSeconds::isLeapYear(1994));
  assertEquals("isLeapYear(1995)", false, LeapSeconds::isLeapYear(1995));
  assertEquals("isLeapYear(1996)", true,  LeapSeconds::isLeapYear(1996));
  assertEquals("isLeapYear(1997)", false, LeapSeconds::isLeapYear(1997));
  assertEquals("isLeapYear(1998)", false, LeapSeconds::isLeapYear(1998));
  assertEquals("isLeapYear(1999)", false, LeapSeconds::isLeapYear(1999));
  assertEquals("isLeapYear(2000)", true,  LeapSeconds::isLeapYear(2000));
  assertEquals("isLeapYear(2001)", false, LeapSeconds::isLeapYear(2001));
  assertEquals("isLeapYear(2002)", false, LeapSeconds::isLeapYear(2002));
  assertEquals("isLeapYear(2003)", false, LeapSeconds::isLeapYear(2003));
  assertEquals("isLeapYear(2004)", true,  LeapSeconds::isLeapYear(2004));
  assertEquals("isLeapYear(2005)", false, LeapSeconds::isLeapYear(2005));
  assertEquals("isLeapYear(2006)", false, LeapSeconds::isLeapYear(2006));
  assertEquals("isLeapYear(2007)", false, LeapSeconds::isLeapYear(2007));
  assertEquals("isLeapYear(2008)", true,  LeapSeconds::isLeapYear(2008));
  assertEquals("isLeapYear(2009)", false, LeapSeconds::isLeapYear(2009));
}

void LeapSecondsTest::testToStringGPS () {
  // Added in Dec 2013 to accompany major re-write of toStringGPS(..)
  int32_t year = 1980;
  int32_t mon  = 1;
  int32_t day  = 6;
  int32_t hour = 1;
  int32_t min  = 2;
  int32_t sec  = 3;

  int64_t seconds = (hour * 3600) + (min * 60) + sec;
  while (year < 2100) {
    string exp1 = Utilities::format("%04d-%02d-%02dT%02d:%02d:%02dZ",              year,mon,day, hour,min,sec);
    string exp2 = Utilities::format("%04d-%02d-%02dT%02d:%02d:%02d.012345678901Z", year,mon,day, hour,min,sec);
    string act1 = LeapSeconds::toStringGPS(seconds, __INT64_C(-1));
    string act2 = LeapSeconds::toStringGPS(seconds, __INT64_C(12345678901));

    assertEquals("toStringGPS(..)", exp1, act1);
    assertEquals("toStringGPS(..)", exp2, act2);

    seconds += 86400;
    int32_t feb = (LeapSeconds::isLeapYear(year))? 29 : 28;
         if ((mon ==  1) && (day == 31)) { mon++; day=1; }
    else if ((mon ==  2) && (day ==feb)) { mon++; day=1; }
    else if ((mon ==  3) && (day == 31)) { mon++; day=1; }
    else if ((mon ==  4) && (day == 30)) { mon++; day=1; }
    else if ((mon ==  5) && (day == 31)) { mon++; day=1; }
    else if ((mon ==  6) && (day == 30)) { mon++; day=1; }
    else if ((mon ==  7) && (day == 31)) { mon++; day=1; }
    else if ((mon ==  8) && (day == 31)) { mon++; day=1; }
    else if ((mon ==  9) && (day == 30)) { mon++; day=1; }
    else if ((mon == 10) && (day == 31)) { mon++; day=1; }
    else if ((mon == 11) && (day == 30)) { mon++; day=1; }
    else if ((mon == 12) && (day == 31)) { mon=1; day=1; year++; }
    else                                 {        day++; }
  }
}

