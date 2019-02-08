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

package nxm.vrt.lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.binarySearch;

/** Class to deal with leap seconds.
 *  @see TimeStamp
 */
public final class LeapSeconds {
  /** Day In Month map. Maps the day-of-year to the month that day falls in.
   *  <pre>
   *    NON-LEAP-YEARS:
   *      (<i>day</i> &gt;= dim[0]) && (<i>day</i> &lt; dim[1])  --&gt; JAN
   *      (<i>day</i> &gt;= dim[8]) && (<i>day</i> &lt; dim[9])  --&gt; SEP
   *
   *    LEAP-YEARS:
   *      (<i>day</i> &gt;= dim[0+12]) && (<i>day</i> &lt; dim[1+12])  --&gt; JAN
   *      (<i>day</i> &gt;= dim[8+12]) && (<i>day</i> &lt; dim[9+12])  --&gt; SEP
   *  </pre>
   */
  static final int dim[] = { 0,31,59,90,120,151,181,212,243,273,304,334,
                             0,31,60,91,121,152,182,213,244,274,305,335 };

  private static enum Month { XXX, JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC };

  /** @deprecated This constant has moved to <tt>VRTIO</tt> in the accompanying Midas option tree. */
  @Deprecated
  public static final int J1970TOJ1950       = 631152000; // (7305 days) * (86,400 sec/day)

  /** @deprecated This constant has moved to the {@link TimeStamp} class. */
  @Deprecated
  public static final int GPS2UTC            = 315964811; // (3657 days) * (86,400 sec/day) + (~11 leap sec)

  /** Number of leap seconds between UTC and TAI on 1 JAN 1970, rounded to the nearest second.
   *  See {@link TimeStamp} for details.
   */
  static final int UTC2TAI_LS_1970    =         8; // ~= 8.000082

  /** Number of leap seconds between GPS and TAI on 6 JAN 1980.
   *  See {@link TimeStamp} for details.
   */
  static final int GPS2TAI_LS_1980    =        19;

  private static final double[] PRE_1972 = {
    // START DATE    CONSTANT  OFFSET      SCALE
    -3287 * 86400.0, 1.422818, 3.471552E8, 0.001296,
    -3075 * 86400.0, 1.372818, 3.471552E8, 0.001296,
    -2922 * 86400.0, 1.845858, 3.786912E8, 0.0011232,
    -2253 * 86400.0, 1.945858, 3.786912E8, 0.0011232,
    -2192 * 86400.0, 3.24013,  4.733856E8, 0.001296,
    -2101 * 86400.0, 3.34013,  4.733856E8, 0.001296,
    -1948 * 86400.0, 3.44013,  4.733856E8, 0.001296,
    -1826 * 86400.0, 3.54013,  4.733856E8, 0.001296,
    -1767 * 86400.0, 3.64013,  4.733856E8, 0.001296,
    -1645 * 86400.0, 3.74013,  4.733856E8, 0.001296,
    -1583 * 86400.0, 3.84013,  4.733856E8, 0.001296,
    -1461 * 86400.0, 4.31317,  5.049216E8, 0.002592,
     -700 * 86400.0, 4.21317,  5.049216E8, 0.002592,
  };

  private static final int PRE_1972_LENGTH = PRE_1972.length / 4;

  /** The first lines that should appear in the file. This serves as a sanity check for the
   *  accuracy of the file.
   */
  private static final String[] FIRST_LINES = {
  // 0         1         2         3         4         5         6         7         8
  // 012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
    " 1961 JAN  1 =JD 2437300.5  TAI-UTC=   1.4228180 S + (MJD - 37300.) X 0.001296 S", //  0
    " 1961 AUG  1 =JD 2437512.5  TAI-UTC=   1.3728180 S + (MJD - 37300.) X 0.001296 S", //  1
    " 1962 JAN  1 =JD 2437665.5  TAI-UTC=   1.8458580 S + (MJD - 37665.) X 0.0011232S", //  2
    " 1963 NOV  1 =JD 2438334.5  TAI-UTC=   1.9458580 S + (MJD - 37665.) X 0.0011232S", //  3
    " 1964 JAN  1 =JD 2438395.5  TAI-UTC=   3.2401300 S + (MJD - 38761.) X 0.001296 S", //  4
    " 1964 APR  1 =JD 2438486.5  TAI-UTC=   3.3401300 S + (MJD - 38761.) X 0.001296 S", //  5
    " 1964 SEP  1 =JD 2438639.5  TAI-UTC=   3.4401300 S + (MJD - 38761.) X 0.001296 S", //  6
    " 1965 JAN  1 =JD 2438761.5  TAI-UTC=   3.5401300 S + (MJD - 38761.) X 0.001296 S", //  7
    " 1965 MAR  1 =JD 2438820.5  TAI-UTC=   3.6401300 S + (MJD - 38761.) X 0.001296 S", //  8
    " 1965 JUL  1 =JD 2438942.5  TAI-UTC=   3.7401300 S + (MJD - 38761.) X 0.001296 S", //  9
    " 1965 SEP  1 =JD 2439004.5  TAI-UTC=   3.8401300 S + (MJD - 38761.) X 0.001296 S", // 10
    " 1966 JAN  1 =JD 2439126.5  TAI-UTC=   4.3131700 S + (MJD - 39126.) X 0.002592 S", // 11
    " 1968 FEB  1 =JD 2439887.5  TAI-UTC=   4.2131700 S + (MJD - 39126.) X 0.002592 S", // 12
    " 1972 JAN  1 =JD 2441317.5  TAI-UTC=  10.0       S + (MJD - 41317.) X 0.0      S", // 13
    " 1972 JUL  1 =JD 2441499.5  TAI-UTC=  11.0       S + (MJD - 41317.) X 0.0      S", // 14
    " 1973 JAN  1 =JD 2441683.5  TAI-UTC=  12.0       S + (MJD - 41317.) X 0.0      S", // 15
    " 1974 JAN  1 =JD 2442048.5  TAI-UTC=  13.0       S + (MJD - 41317.) X 0.0      S", // 16
    " 1975 JAN  1 =JD 2442413.5  TAI-UTC=  14.0       S + (MJD - 41317.) X 0.0      S", // 17
    " 1976 JAN  1 =JD 2442778.5  TAI-UTC=  15.0       S + (MJD - 41317.) X 0.0      S", // 18
    " 1977 JAN  1 =JD 2443144.5  TAI-UTC=  16.0       S + (MJD - 41317.) X 0.0      S", // 19
    " 1978 JAN  1 =JD 2443509.5  TAI-UTC=  17.0       S + (MJD - 41317.) X 0.0      S", // 20
    " 1979 JAN  1 =JD 2443874.5  TAI-UTC=  18.0       S + (MJD - 41317.) X 0.0      S", // 21
    " 1980 JAN  1 =JD 2444239.5  TAI-UTC=  19.0       S + (MJD - 41317.) X 0.0      S", // 22
    " 1981 JUL  1 =JD 2444786.5  TAI-UTC=  20.0       S + (MJD - 41317.) X 0.0      S", // 23
    " 1982 JUL  1 =JD 2445151.5  TAI-UTC=  21.0       S + (MJD - 41317.) X 0.0      S", // 24
    " 1983 JUL  1 =JD 2445516.5  TAI-UTC=  22.0       S + (MJD - 41317.) X 0.0      S", // 25
    " 1985 JUL  1 =JD 2446247.5  TAI-UTC=  23.0       S + (MJD - 41317.) X 0.0      S", // 26
    " 1988 JAN  1 =JD 2447161.5  TAI-UTC=  24.0       S + (MJD - 41317.) X 0.0      S", // 27
    " 1990 JAN  1 =JD 2447892.5  TAI-UTC=  25.0       S + (MJD - 41317.) X 0.0      S", // 28
    " 1991 JAN  1 =JD 2448257.5  TAI-UTC=  26.0       S + (MJD - 41317.) X 0.0      S", // 29
    " 1992 JUL  1 =JD 2448804.5  TAI-UTC=  27.0       S + (MJD - 41317.) X 0.0      S", // 30
    " 1993 JUL  1 =JD 2449169.5  TAI-UTC=  28.0       S + (MJD - 41317.) X 0.0      S", // 31
    " 1994 JUL  1 =JD 2449534.5  TAI-UTC=  29.0       S + (MJD - 41317.) X 0.0      S", // 32
    " 1996 JAN  1 =JD 2450083.5  TAI-UTC=  30.0       S + (MJD - 41317.) X 0.0      S", // 33
    " 1997 JUL  1 =JD 2450630.5  TAI-UTC=  31.0       S + (MJD - 41317.) X 0.0      S", // 34
    " 1999 JAN  1 =JD 2451179.5  TAI-UTC=  32.0       S + (MJD - 41317.) X 0.0      S", // 35
    " 2006 JAN  1 =JD 2453736.5  TAI-UTC=  33.0       S + (MJD - 41317.) X 0.0      S", // 36
    " 2009 JAN  1 =JD 2454832.5  TAI-UTC=  34.0       S + (MJD - 41317.) X 0.0      S", // 37
    " 2012 JUL  1 =JD 2456109.5  TAI-UTC=  35.0       S + (MJD - 41317.) X 0.0      S", // 38
    " 2015 JUL  1 =JD 2457204.5  TAI-UTC=  36.0       S + (MJD - 41317.) X 0.0      S", // 39
  };

  private static LeapSeconds defaultInstance = null;

  private final long[] startDatePOSIX;
  private final long[] startDateUTC;
  private final int[]  leapSeconds;
  private final long[] yearStartPOSIX;
  private final long[] yearStartUTC;

  /** Creates a new instance. */
  private LeapSeconds (long[] wsec, int[] ls) {
    this.startDatePOSIX = new long[wsec.length];
    this.startDateUTC   = new long[wsec.length];
    this.leapSeconds    = ls;
    this.yearStartPOSIX = new long[2106-1972]; // other stuff breaks in 2106, so don't go beyond there.
    this.yearStartUTC   = new long[2106-1972]; // other stuff breaks in 2106, so don't go beyond there.

    for (int i = 0; i < wsec.length; i++) {
      startDatePOSIX[i] = (wsec[i]);
      startDateUTC[i]   = (startDatePOSIX[i] + leapSeconds[i]);
    }

    int  year  = 1972;
    long posix = 365 * 2 * 86400L; // start in 1972
    for (int i = 0; i < yearStartUTC.length; i++) {
      yearStartPOSIX[i] = posix;
      yearStartUTC[i]   = posix + getLeapSecondsPOSIX(posix);

      posix += (isLeapYear(year))? (366 * 86400L)
                                 : (365 * 86400L);
      year  += 1;
    }
  }

  /** Gets the default instance. This will return the instance previously set with
   *  <tt>setDefaultInstance(..)</tt>; if no default instance has yet been set, the
   *  {@link VRTConfig#LEAP_SECONDS_FILE} will be used as the default.
   *  @return An using the give file.
   */
  public static synchronized LeapSeconds getDefaultInstance () {
    if (defaultInstance == null) {
      try {
        InputStream in = LeapSeconds.class.getClassLoader().getResourceAsStream(VRTConfig.LEAP_SECONDS_FILE);
        defaultInstance = getInstance(in);
      }
      catch (IOException e) {
        throw new RuntimeException("Unable to initialize default instance.", e);
      }
    }
    return defaultInstance;
  }

  /** Sets the default instance to use.
   *  @param def The default instance to use.
   */
  public static synchronized void setDefaultInstance (LeapSeconds def) {
    defaultInstance = def;
  }

  /** Sets the default instance to use.
   *  @param in Input stream to read from.
   *  @throws IOException if there is an error while reading and parsing the file.
   */
  public static void setDefaultInstance (InputStream in) throws IOException {
    setDefaultInstance(getInstance(in));
  }

  /** Sets the default instance to use.
   *  @param in Input stream to read from.
   *  @throws IOException if there is an error while reading and parsing the file.
   */
  public static void setDefaultInstance (BufferedReader in) throws IOException {
    setDefaultInstance(getInstance(in));
  }

  /** Gets an instance with the given tai-utc.dat file. On the first call to this method the
   *  file will be opened and read. Subsequent calls may either re-open the file or return the an
   *  existing open copy.
   *  @param in The input stream for the file to read in.
   *  @return An using the give file.
   *  @throws IOException If there is an error reading from the input stream.
   */
  public static LeapSeconds getInstance (InputStream in) throws IOException {
    return getInstance(new BufferedReader(new InputStreamReader(in), 65536));
  }

  /** Gets an instance with the given tai-utc.dat file. On the first call to this method the
   *  file will be opened and read. Subsequent calls may either re-open the file or return the an
   *  existing open copy.
   *  @param in The buffered reader for the file to read in.
   *  @return An using the give file.
   *  @throws IOException If there is an error reading from the input stream.
   */
  public static LeapSeconds getInstance (BufferedReader in) throws IOException {
    List<String> lines = new ArrayList<String>(FIRST_LINES.length+4);
    String line;

    while ((line = in.readLine()) != null) {
      lines.add(line);
    }

    if (lines.size() < FIRST_LINES.length) {
      throw new IOException("TAI to UTC mapping file appears to be out of date or invalid.");
    }

    long[] sd = new long[lines.size() - PRE_1972_LENGTH];
    int[]  ls = new int [lines.size() - PRE_1972_LENGTH];

    for (int i = 0; i < lines.size(); i++) {
      line = lines.get(i);

      if ((i < FIRST_LINES.length) && !line.equals(FIRST_LINES[i])) {
        throw new IllegalArgumentException("TAI to UTC mapping file appears invalid: On line "+(i+1)
                                         + " got '"+line+"' but expected '"+FIRST_LINES[i]+"'.");
      }

      if (i >= PRE_1972_LENGTH) {
        int year = parseInt(line.substring(1, 5));
        int mon  = Month.valueOf(line.substring(6, 9)).ordinal();
        int day  = parseInt(line.substring(10, 12).trim());

        sd[i-PRE_1972_LENGTH] = ymdToPosixDay(year, mon, day) * 86400L; // POSIX date
        ls[i-PRE_1972_LENGTH] = parseInt(line.substring(38, 40)) - UTC2TAI_LS_1970;
      }
    }
    return new LeapSeconds(sd, ls);
  }

  /** Calculate and return the difference between TAI (International Atomic Time) and UTC
   *  (Universal Coordinated Time) for the given time. Generally, this will be the number
   *  of leap seconds in effect at that time. Between 1961/01/01 and 1972/01/01 this was a
   *  sliding value with fractional offset; since 1972/01/01 this is an integral number of
   *  seconds. <br>
   *  <br>
   *  The functionality of this method matches the <tt>m_get_leap_seconds(..)</tt> method
   *  in X-Midas's <tt>leap_seconds.cc</tt> class, but uses an optimized algorithm for the
   *  most common cases.
   *  @param wsec The number of whole seconds in Midas time referenced to UTC.
   *  @param fsec The number of fractional seconds (ignored after 1972/01/01).
   *  @return The difference between TAI and UTC (i.e. TAI - UTC) in seconds.
   *  @throws IllegalArgumentException If the date is before 1961/01/01.
   */
  public double getLeapSeconds (double wsec, double fsec) {
    int posix = (int)(wsec - J1970TOJ1950);

    if (posix >= startDatePOSIX[0]) {
      return getLeapSecondsPOSIX(posix);
    }
    if (posix == 0) {
      return 0.0; // common special-case where time is zero
    }

    for (int i = (PRE_1972_LENGTH*4)-4; i >= 0; i-=4) {
      if (PRE_1972[i] > wsec) continue;
      //     CONSTANT      + (wsec + fsec - OFFSET       ) * (SCALE         / SecondsPerDay)
      return PRE_1972[i+1] + (wsec + fsec - PRE_1972[i+2]) * (PRE_1972[i+3] / 86400.0      ) - 8.000082;
    }

    // This should only hit for dates before 1961
    throw new IllegalArgumentException("Can not convert "+wsec+"+"+fsec+" TAI to UTC, "
                                     + "dates before 1960/01/01 are not supported.");
  }

  /** Gets the number of leap seconds elapsed prior to the given time.
   *  @param utc 1-second time tics since 1970/01/01.
   *  @return Leap seconds elapsed during prior to the given time.
   *  @throws IllegalArgumentException if the input time is before 1972/01/01. (Between 1970 and 1972,
   *                                   the number of leap seconds is not-integral.)
   */
  public int getLeapSecondsUTC (long utc) {
    if (utc > 0xFFFFFFFFL) throw new IllegalArgumentException("Input time ("+utc+" POSIX) exceeds max value.");

    if (utc == 0) { // special case (date not yet initialized, use 1970-01-01T00:00:00)
      return 0;
    }
    if (utc >= startDateUTC[startDateUTC.length-1]) { // most common case (after last entry)
      return leapSeconds[startDateUTC.length-1];
    }
    if (utc >= startDateUTC[0]) {
      int i = binarySearch(startDateUTC, utc);
      return (i >= 0)? leapSeconds[i] : leapSeconds[-i - 2];
    }
    throw new IllegalArgumentException("Input time ("+utc+" UTC) is before 1972/01/01.");
  }

  /** Gets the number of leap seconds elapsed prior to the given time.
   *  @param posix Number of days elapsed since 1970/01/01 multiplied by 86400, plus the number of
   *               seconds in the current day.
   *  @return Leap seconds elapsed during prior to the given time.
   *  @throws IllegalArgumentException if the input time is before 1972/01/01. (Between 1970 and 1972,
   *                                   the number of leap seconds is non-integral.)
   */
  public int getLeapSecondsPOSIX (long posix) {
    if (posix > 0xFFFFFFFFL) throw new IllegalArgumentException("Input time ("+posix+" POSIX) exceeds max value.");

    if (posix == 0) { // special case (date not yet initialized, use 1970-01-01T00:00:00)
      return 0;
    }
    if (posix > startDatePOSIX[startDatePOSIX.length-1]) { // most common case (after last entry)
      return leapSeconds[startDatePOSIX.length-1];
    }
    if (posix >= startDatePOSIX[0]) {
      int i = binarySearch(startDatePOSIX, posix);
      return (i >= 0)? leapSeconds[i] : leapSeconds[-i - 2];
    }
    throw new IllegalArgumentException("Input time ("+posix+" POSIX) is before 1972/01/01.");
  }

  /** <b>Internal use only:</b> Is the identified UTC time equal to an inserted leap second?
   *  That is, is the time equal to the 86401th second of the day (23:59:60)?
   *  @param utc 1-second time tics since 1970/01/01.
   *  @return true if the time is 23:59:60, false otherwise.
   *  @throws VRTException if the input time is before 1972/01/01.
   */
  boolean isLeapSecond (long utc) {
    if (utc > 0xFFFFFFFFL) throw new IllegalArgumentException("Input time ("+utc+" POSIX) exceeds max value.");

    if ((utc == 0) || (utc >= startDateUTC[startDateUTC.length-1])) {
      // the two most common cases, and they are both false
      return false;
    }
    if (utc >= startDateUTC[0]) {
      int i = binarySearch(startDateUTC, utc+1);
      return (i >= 0);
    }
    throw new IllegalArgumentException("Input time ("+utc+" UTC) is before 1972/01/01.");
  }

  /** Converts UTC time to a year number (1970..N). */
  int getYear (long utc) {
    if (utc == 0) return 1970;  // special case (date not yet initialized, use 1970-01-01T00:00:00)
    if (utc < yearStartUTC[0]) throw new IllegalArgumentException("Year look-up with leap seconds not valid before 1972");
    if (utc > 0xFFFFFFFFL ) throw new IllegalArgumentException("Input time ("+utc+" UTC) exceeds max value.");

    int i = binarySearch(yearStartUTC, utc);
    return (i >= 0)? 1972+i : 1972+(-i)-2;
  }

  /** Gets month number based on UTC year (1970..N) and UTC time (from 1970). In
   *  this form 'year' is technically redundant (since it can be computed from
   *  'utc'), but it is here to skip that computation.
   */
  private int getMonth (int year, long utc) {
    if (utc == 0) return 1; // special case (date not yet initialized, use 1970-01-01T00:00:00)

    for (int mon = 2; mon <= 12; mon++) {
      if (utc < getStartOfMonth(year,mon)) return mon-1;
    }
    return 12;
  }

  /** Gets UTC start-of-month based on year (1970..N) and month (1..12). */
  private long getStartOfMonth (int year, int mon) {
    long monthStartPOSIX = ymdToPosixDay(year, mon, 1)*86400L;
    long monthStartUTC   = monthStartPOSIX + getLeapSecondsPOSIX(monthStartPOSIX);
    return monthStartUTC;
  }

  /** Gets the start of the UTC year in seconds-since-1970. */
  long getStartOfYearUTC (int year) {
    if (year < 1972) throw new IllegalArgumentException("Year look-up with leap seconds not valid before 1972");
    if (year > 2106) throw new IllegalArgumentException("Year look-up with leap seconds not valid after 2106");
    return yearStartUTC[year - 1972];
  }

  /** Gets the start of the POSIX year in seconds-since-1970. */
  long getStartOfYearPOSIX (int year) {
    if (year < 1972) throw new IllegalArgumentException("Year look-up with leap seconds not valid before 1972");
    if (year > 2106) throw new IllegalArgumentException("Year look-up with leap seconds not valid after 2106");
    return yearStartPOSIX[year - 1972];
  }

  /** Converts UTC time to the UTC time at 1 Jan of the given year. */
  long getYiS (long utc) {
    return yearStartUTC[getYear(utc)-1972];
  }

  /** <b>Internal Use Only:</b> Converts a UTC time to Year-Month-Day Hour:Min:Sec.
   *  @param seconds     Whole seconds.
   *  @param picoseconds Picoseconds (-1 to ignore).
   *  @return Year-Month-Day Hour:Min:Sec.
   */
  String toStringUTC (long seconds, long picoseconds) {
    if (isLeapSecond(seconds)) {
      int year  = getYear(seconds);
      int month = getMonth(year,seconds);
      int sec   = (int)(seconds - getStartOfMonth(year,month));
      int day   = ((sec-1) / 86400) + 1;
      return toString(year, month, day, 23, 59, 60, picoseconds);
    }
    else {
      int year  = getYear(seconds);
      int month = getMonth(year,seconds);
      int sec   = (int)(seconds - getStartOfMonth(year,month));
      int day   = (sec / 86400) + 1;      sec = sec - (day-1) * 86400;
      int hour  = sec / 3600;             sec = sec - hour    * 3600;
      int min   = sec / 60;               sec = sec - min     * 60;
      return toString(year, month, day, hour, min, sec, picoseconds);
    }
  }

  /** <b>Internal Use Only:</b> Converts a GPS time to Year-Month-Day Hour:Min:Sec.
   *  @param seconds     Whole seconds.
   *  @param picoseconds Picoseconds (-1 to ignore).
   *  @return Year-Month-Day Hour:Min:Sec.
   */
  public static String toStringGPS (long seconds, long picoseconds) {
    // This function was re-written in Dec 2013 due to it having shown up as a
    // performance hot-spot. The new version is branch-free (except for the one
    // branch in toString(..), uses only integer math, and minimizes the number
    // of function calls used.
    int sec   = (int)(seconds % 86400);
    int hour  = sec / 3600; sec = sec - hour * 3600;
    int min   = sec / 60;   sec = sec - min  * 60;

    // This takes advantage of there being 1461 days in every 4-year period. Unlike
    // the more intuitive version of this equation, this needs to use a value for
    // d that is offset differently to account for 0=6 Jan 1980 (not 1 Jan 1980)
    // and to account for the first year being a leap-year.
    int days  = (int)(seconds / 86400);
    int d     = days + 4;
    int year  = 1980 + (d / 1461) * 4 + ((d % 1461) / 365);  // year number
    int soy   = ((year - 1977) / 4) + ((year - 1980) * 365); // start-of-year where 0=1 Jan 1980
    int doy   = d - soy + 2;

    // Offset the day-of-year such that 0=1 Mar and Jan & Feb are the last months
    // (or 13 & 14). This way 29 Feb will be at the end (if present) and we can
    // make use of the fact that between March and January every 5-month interval
    // has 153 days. Note that these computations only work with strict integer
    // math where any fractional artifacts of the division operations are discarded.
    //
    // Note that the "(bool)? 1 : 0" construct is a special one in many compilers
    // (including Java HotSpot) since it can strip the branch out if the boolean
    // value is already stored in the register as a 1 or 0.
    //
    // The mask + ddd construct used below is the branch-free form of:
    //   int ddd = (doy < cutOff)? doy + 305 : doy - cutOff;
    int cutOff     = 60 + ((isLeapYear(year))? 1 : 0);
    int mask       = (doy - cutOff) >> 31;
    int ddd        = doy + (305 & mask) + (-cutOff & ~mask);
    int m          = (5*ddd + 2) / 153;      // Month number if counting from March
    int monthStart = (153*m + 2) / 5;        // DoY for month start where 0=1 March
    int dayOfMon   = ddd - monthStart + 1;   // Normal day-of-month
    int monOfYear  = ((m+2) % 12) + 1;       // Normal month-of-year (1=Jan)

    return toString(year, monOfYear, dayOfMon, hour, min, sec, picoseconds);
  }

  /** Writes a set of picoseconds to a string in the form ".00000000000". */
  private static char[] writePicoseconds (char[] str, int off, long psec) {
    str[off   ] = '.';
    str[off+ 1] = (char)('0' + ((psec / 100000000000L) % 10));
    str[off+ 2] = (char)('0' + ((psec /  10000000000L) % 10));
    str[off+ 3] = (char)('0' + ((psec /   1000000000L) % 10));
    str[off+ 4] = (char)('0' + ((psec /    100000000L) % 10));
    str[off+ 5] = (char)('0' + ((psec /     10000000L) % 10));
    str[off+ 6] = (char)('0' + ((psec /      1000000L) % 10));
    str[off+ 7] = (char)('0' + ((psec /       100000L) % 10));
    str[off+ 8] = (char)('0' + ((psec /        10000L) % 10));
    str[off+ 9] = (char)('0' + ((psec /         1000L) % 10));
    str[off+10] = (char)('0' + ((psec /          100L) % 10));
    str[off+11] = (char)('0' + ((psec /           10L) % 10));
    str[off+12] = (char)('0' + ((psec                ) % 10));
    return str;
  }

  /** Converts a set of picoseconds to a string in the form "0.00000000000". */
  static String toPicosecondString (long psec) {
    char[] str = new char[14];
    str[0] = '0';
    return new String(writePicoseconds(str, 1, psec));
  }

  /** Converts date/time to string. (psec=-1L to ignore) */
  private static String toString (int year, int month, int day,
                                  int hour, int min, int sec, long psec) {
    char[] str;
    if (psec >= 0) {
      str = new char[33];
      writePicoseconds(str, 19, psec);
      str[32] = 'Z';
    }
    else {
      str = new char[20];
      str[19] = 'Z';
    }

    str[ 0] = (char)('0' + ((year  / 1000)     ));
    str[ 1] = (char)('0' + ((year  /  100) % 10));
    str[ 2] = (char)('0' + ((year  /   10) % 10));
    str[ 3] = (char)('0' + ((year        ) % 10));
    str[ 4] = '-';
    str[ 5] = (char)('0' + ((month /   10)     ));
    str[ 6] = (char)('0' + ((month       ) % 10));
    str[ 7] = '-';
    str[ 8] = (char)('0' + ((day   /   10)     ));
    str[ 9] = (char)('0' + ((day         ) % 10));
    str[10] = 'T';
    str[11] = (char)('0' + ((hour  /   10)     ));
    str[12] = (char)('0' + ((hour        ) % 10));
    str[13] = ':';
    str[14] = (char)('0' + ((min   /   10)     ));
    str[15] = (char)('0' + ((min         ) % 10));
    str[16] = ':';
    str[17] = (char)('0' + ((sec   /   10)     ));
    str[18] = (char)('0' + ((sec         ) % 10));

    return new String(str);
  }

  /** <b>Internal Use Only:</b> Calculate the number of days (since 6-Jan-1980)
   *  for the supplied date.
   *  @param year  The year (e.g. 2002).
   *  @param month The month (1 = Jan)
   *  @param day   The day.
   *  @return The Modified Julian Day.
   */
  static int ymdToGpsDay (int year, int month, int day) {
    // Identical to the POSIX version except using 11450 rather than 7793 to account for the
    // additional 3657 days between 1 Jan 1970 and 6 Jan 1980.
    return isLeapYear(year)? ((year-1950)*365 + (year+3)/4 - 11450 + (day + dim[month-1+12] - 1))
                           : ((year-1950)*365 + (year+3)/4 - 11450 + (day + dim[month-1+ 0] - 1));
  }

  /** <b>Internal Use Only:</b> Calculate the number of days (since 1-Jan-1970)
   *  for the supplied date.
   *  @param year  The year (e.g. 2002).
   *  @param month The month (1 = Jan)
   *  @param day   The day.
   *  @return The Modified Julian Day.
   */
  static int ymdToPosixDay (int year, int month, int day) {
    return isLeapYear(year)? ((year-1950)*365 + (year+3)/4 - 7793 + (day + dim[month-1+12] - 1))
                           : ((year-1950)*365 + (year+3)/4 - 7793 + (day + dim[month-1+ 0] - 1));
  }

  /** <b>Internal Use Only:</b> Indicates if the given year is a leap year. The
   *  code for this method was copied from RFC 3339.
   *  <pre>
   *    Source: Klyne, et.al. "RFC 3339 / Date and Time on the Internet: Timestamps."
   *            ITEF, July 2002. http://tools.ietf.org/html/rfc3339 (Retrieved 2007-06-15)
   *  </pre>
   *  @param year The year number.
   *  @return true if it is a leap year, false otherwise.
   */
  public static boolean isLeapYear (int year) {
    return (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0));
  }
}
