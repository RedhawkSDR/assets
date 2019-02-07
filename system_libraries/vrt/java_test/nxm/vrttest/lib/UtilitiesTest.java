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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import nxm.vrt.lib.AsciiCharSequence;
import nxm.vrt.lib.BasicContextPacket;
import nxm.vrt.lib.BasicDataPacket;
import nxm.vrt.lib.HasFields;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.Utilities;
import nxm.vrt.lib.VRTPacket;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static nxm.vrttest.inc.TestRunner.assertEquals;
import static nxm.vrttest.inc.TestRunner.assertException;
import static nxm.vrttest.inc.TestRunner.assertInstanceOf;
import static nxm.vrttest.inc.TestRunner.assertSame;

/** Tests for the {@link Utilities} class. */
public class UtilitiesTest implements Testable {
  @Override
  public Class<?> getTestedClass () {
    return Utilities.class;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    tests.add(new TestSet("append(..)",                  this,  "testAppend"));
    tests.add(new TestSet("asHasFields(..)",             this,  "testAsHasFields"));
    tests.add(new TestSet("asMap(..)",                   this,  "testAsMap"));
    tests.add(new TestSet("castValue(..)",               this,  "testCastValue"));
    tests.add(new TestSet("deepCopy(..)",                this,  "testDeepCopy"));
    tests.add(new TestSet("equal(..)",                   this,  "testEqual"));
    tests.add(new TestSet("fromStringClassID(..)",       this, "+testToStringClassID"));
    tests.add(new TestSet("fromStringDeviceID(..)",      this, "+testToStringDeviceID"));
    tests.add(new TestSet("fromStringOUI(..)",           this, "+testToStringOUI"));
    tests.add(new TestSet("getFieldByName(..)",          this, "+testGetFieldID"));
    tests.add(new TestSet("getFieldID(..)",              this,  "testGetFieldID"));
    tests.add(new TestSet("isNumericType(..)",           this,  "testIsNumericType"));
    tests.add(new TestSet("isString(..)",                this,  "testIsString"));
    tests.add(new TestSet("newNamedThreadFactory(..)",   this,  "testNewNamedThreadFactory"));
    tests.add(new TestSet("newSingleThreadExecutor(..)", this,  "testNewSingleThreadExecutor"));
    tests.add(new TestSet("normalizeAngle180(..)",       this,  "testNormalizeAngle180"));
    tests.add(new TestSet("normalizeAngle360(..)",       this,  "testNormalizeAngle360"));
    tests.add(new TestSet("present(..)",                 this,  "testPresent"));
    tests.add(new TestSet("setFieldByName(..)",          this, "+testGetFieldID"));
    tests.add(new TestSet("sleep(..)",                   this, "-testSleep"));
    tests.add(new TestSet("sleepUntil(..)",              this, "-testSleepUntil"));
    tests.add(new TestSet("toBoolean(..)",               this,  "testToBoolean"));
    tests.add(new TestSet("toBooleanValue(..)",          this, "+testToBoolean"));
    tests.add(new TestSet("toEnum(..)",                  this,  "testToEnum"));
    tests.add(new TestSet("toStringClassID(..)",         this,  "testToStringClassID"));
    tests.add(new TestSet("toStringDeviceID(..)",        this,  "testToStringDeviceID"));
    tests.add(new TestSet("toStringOUI(..)",             this,  "testToStringOUI"));
    tests.add(new TestSet("toHexDump(..)",               this,  "testToHexDump"));
    tests.add(new TestSet("toHexString(..)",             this,  "testToHexString"));
    tests.add(new TestSet("toMap(..)",                   this, "+testAsMap"));
    tests.add(new TestSet("trim(..)",                    this, "+testTrimNA"));
    tests.add(new TestSet("trimNA(..)",                  this,  "testTrimNA"));

    return tests;
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }

  @Override
  public String toString () {
    return "Tests for Utilities";
  }

  public void testAppend () {
    final Object[] tests = {
      //input         output1                           output2
      null,          "initial text",                   "initial text",
      "My String",   "initial text prefix My String",  "initial text prefix My String suffix",
      (byte  )10,    "initial text prefix 10",         "initial text prefix 10 suffix",
      (short )20,    "initial text prefix 20",         "initial text prefix 20 suffix",
      30,            "initial text prefix 30",         "initial text prefix 30 suffix",
      40L,           "initial text prefix 40",         "initial text prefix 40 suffix",
      5.5f,          "initial text prefix 5.5",        "initial text prefix 5.5 suffix",
      6.5d,          "initial text prefix 6.5",        "initial text prefix 6.5 suffix",
      true,          "initial text prefix true",       "initial text prefix true suffix",
      false,         "initial text prefix false",      "initial text prefix false suffix",
    };

    for (int i = 0; i < tests.length; i+=3) {
      StringBuilder str1 = new StringBuilder("initial text");
      StringBuilder str2 = new StringBuilder("initial text");

      Utilities.append(str1," prefix ",tests[i]);
      Utilities.append(str2," prefix ",tests[i]," suffix");

      assertEquals("append(s,prefix,"+tests[i]+")",        tests[i+1], str1.toString());
      assertEquals("append(s,prefix,"+tests[i]+",suffix)", tests[i+2], str2.toString());
    }
  }

  public void testAsMap () {
    String[]  strings  = { "1", "2", "3", "4" };
    Integer[] integers = {  1,   2,   3,   4  };

    Map<String,Integer> asMapSI = Utilities.asMap(String.class, Integer.class,
                                                  "1", 1, "2", 2, "3", 3, "4", 4);
    Map<Integer,String> asMapIS = Utilities.asMap(Integer.class, String.class,
                                                   1, "1", 2, "2", 3, "3", 4, "4");

    Map<String,Integer> toMapSI = Utilities.toMap("1", 1, "2", 2, "3", 3, "4", 4);
    Map<Integer,String> toMapIS = Utilities.toMap( 1, "1", 2, "2", 3, "3", 4, "4");
    Map<Object,Object>  toMapOO = Utilities.toMap((Object)"1", (Object)1, "2", 2, "3", 3, "4", 4,
                                                   1, "1", 2, "2", 3, "3", 4, "4");

    assertEquals("asMap() -> size", integers.length,   asMapSI.size());
    assertEquals("asMap() -> size", integers.length,   asMapIS.size());
    assertEquals("toMap() -> size", integers.length,   toMapSI.size());
    assertEquals("toMap() -> size", integers.length,   toMapIS.size());
    assertEquals("toMap() -> size", integers.length*2, toMapOO.size());

    for (int i = 0; i < strings.length; i++) {
      assertEquals("asMap() -> at i="+i, integers[i], asMapSI.get(strings[i]));
      assertEquals("asMap() -> at i="+i, strings[i], asMapIS.get(integers[i]));
      assertEquals("toMap() -> at i="+i, integers[i], toMapSI.get(strings[i]));
      assertEquals("toMap() -> at i="+i, strings[i], toMapIS.get(integers[i]));

      assertEquals("toMap() -> at i="+i, integers[i], toMapOO.get(strings[i]));
      assertEquals("toMap() -> at i="+i, strings[i], toMapOO.get(integers[i]));
    }

    assertException("asMap(..) with bad types", "testAsMap_badTypes", ClassCastException.class);
    assertException("asMap(..) with bad count", "testAsMap_badCount", ArrayIndexOutOfBoundsException.class);
  }

  public void testAsMap_badTypes () {
    Map<String,Integer> m = Utilities.asMap(String.class, Integer.class,
                                            1, "1", 2, "2", 3, "3", 4, "4");
  }

  public void testAsMap_badCount () {
    Map<String,Integer> m = Utilities.asMap(String.class, Integer.class,
                                            "1", 1, "2", 2, "3", 3, "4"/*, 4*/);
  }

  public void testEqual () {
    String  a = "123";
    String  b = a;
    String  c = Integer.toString(123); // same as "123" but should not intern it
    String  x = null;
    Integer y = 123;
    Integer z = null;

    assertEquals("a & b", true,  Utilities.equal(a,b));
    assertEquals("a & c", true,  Utilities.equal(a,c));
    assertEquals("b & c", true,  Utilities.equal(b,c));

    assertEquals("a & x", false, Utilities.equal(a,x));
    assertEquals("a & y", false, Utilities.equal(a,y));
    assertEquals("a & z", false, Utilities.equal(a,z));

    assertEquals("x & y", false, Utilities.equal(x,y));
    assertEquals("x & z", true,  Utilities.equal(x,z));
    assertEquals("y & z", false, Utilities.equal(y,z));
  }

  public void testIsString () {
    String        a = "123";
    String        b = a;
    String        c = Integer.toString(123); // same as "123" but should not intern it
    StringBuilder x = new StringBuilder("123");
    StringBuilder y = null;
    CharSequence  z = "456";

    assertEquals("a & b", TRUE,  Utilities.isString(a,b));
    assertEquals("a & c", TRUE,  Utilities.isString(a,c));
    assertEquals("b & c", TRUE,  Utilities.isString(b,c));

    assertEquals("a & x", TRUE,  Utilities.isString(a,x));
    assertEquals("a & y", null,  Utilities.isString(a,y));
    assertEquals("a & z", FALSE, Utilities.isString(a,z));
  }

  public void testPresent () {
    //                       value                   -> isPresent
    final Object[] TESTS = { null                     , false,
                             ""                       , false,
                             "123"                    , true,
                             "456"                    , true,
                             new StringBuilder("123") , true };

    for (int i = 0; i < TESTS.length; i+=2) {
      CharSequence val = (CharSequence)TESTS[ i ];
      Boolean      exp = (Boolean     )TESTS[i+1];

      assertEquals(val, exp, Utilities.present(val));
    }
  }

  public void testToEnum () {
    for (TimeUnit tu : TimeUnit.values()) {
      String s1 = tu.toString();
      String s2 = tu.toString().toLowerCase();

      assertEquals("toEnum(" +tu+ ", TimeUnit.class)", tu, Utilities.toEnum(TimeUnit.class, tu));
      assertEquals("toEnum('"+s1+"', TimeUnit.class)", tu, Utilities.toEnum(TimeUnit.class, s1));
      assertEquals("toEnum('"+s2+"', TimeUnit.class)", tu, Utilities.toEnum(TimeUnit.class, s2));
    }
    assertEquals("toEnum(null, TimeUnit.class)", null, Utilities.toEnum(TimeUnit.class, null));
    assertEquals("toEnum('', TimeUnit.class)",   null, Utilities.toEnum(TimeUnit.class, ""));
  }

  public void testToBoolean () {
    final Object[] nullValues  = { null, ""                             ,    new StringBuilder("")  };
    final Object[] trueValues  = { "true",  "t", "1", "yes", "y", "on"  , 1, new StringBuilder("1"),
                                   "TRUE",  "T",      "YES", "Y", "ON"  };
    final Object[] falseValues = { "false", "f", "0", "no",  "n", "off" , 0, new StringBuilder("0"),
                                   "FALSE", "F",      "NO",  "N", "OFF" };

    for (Object v : nullValues) {
      assertEquals("toBoolean("+v+")", null, Utilities.toBoolean(v));
      assertEquals("toBooleanValue("+v+")", false, Utilities.toBooleanValue(v));
    }

    for (Object v : trueValues) {
      assertEquals("toBoolean("+v+")", true, Utilities.toBoolean(v));
      assertEquals("toBooleanValue("+v+")", true, Utilities.toBooleanValue(v));
    }

    for (Object v : falseValues) {
      assertEquals("toBoolean("+v+")", false, Utilities.toBoolean(v));
      assertEquals("toBooleanValue("+v+")", false, Utilities.toBooleanValue(v));
    }
  }

  public void testTrimNA () {
    final CharSequence EMPTY = "";
    final CharSequence NA1   = "N/A";
    final CharSequence NA2   = "n/a";
    final CharSequence NA3   = "NA";
    final CharSequence NA4   = "na";
    final CharSequence NA5   = new StringBuilder("N/A");
    final CharSequence NA6   = new StringBuilder("n/a");
    final CharSequence CAT   = "Cat";
    final CharSequence DOG   = new StringBuilder("Dog");

    // NORMAL TEST CASES (equals)
    //                             input             trim            trimNA
    final CharSequence[] tests = { "",               "",             "",
                                   "    ",           "",             "",
                                   "n/a",            "n/a",          "",
                                   "N/A",            "N/A",          "",
                                   "  n/a",          "n/a",          "",
                                   "  N/A",          "N/A",          "",
                                   "n/a  ",          "n/a",          "",
                                   "N/A  ",          "N/A",          "",
                                   " n/a ",          "n/a",          "",
                                   " N/A ",          "N/A",          "",
                                   "na",             "na",           "",
                                   "NA",             "NA",           "",
                                   "  na",           "na",           "",
                                   "  NA",           "NA",           "",
                                   "na  ",           "na",           "",
                                   "NA  ",           "NA",           "",
                                   " na ",           "na",           "",
                                   " NA ",           "NA",           "",
                                   "Cat",            "Cat",          "Cat",
                                   "  Cat",          "Cat",          "Cat",
                                   "Cat  ",          "Cat",          "Cat",
                                   " Cat ",          "Cat",          "Cat",
                                   "Hello World!",   "Hello World!", "Hello World!",
                                   "  Hello World!", "Hello World!", "Hello World!",
                                   "Hello World!  ", "Hello World!", "Hello World!",
                                   " Hello World! ", "Hello World!", "Hello World!",
                         new StringBuilder("Dog"),   "Dog",          "Dog",
                         new StringBuilder("  Dog"), "Dog",          "Dog",
                         new StringBuilder("Dog  "), "Dog",          "Dog",
                         new StringBuilder(" Dog "), "Dog",          "Dog",
    };

    // UNMODIFIED INPUT TEST CASES (itentical pointer)
    //                             input             trim            trimNA
    final CharSequence[] ident = { EMPTY,            EMPTY,          EMPTY,
                                   NA1,              NA1,            "",
                                   NA2,              NA2,            "",
                                   NA3,              NA3,            "",
                                   NA4,              NA4,            "",
                                   NA5,              NA5,            "",
                                   NA6,              NA6,            "",
                                   CAT,              CAT,            CAT,
                                   DOG,              DOG,            DOG,
    };

    for (int i = 0; i < tests.length; i+=3) {
      assertEquals("trim("  +tests[i]+")", tests[i+1], Utilities.trim(  tests[i]).toString());
      assertEquals("trimNA("+tests[i]+")", tests[i+2], Utilities.trimNA(tests[i]).toString());
    }

    for (int i = 0; i < ident.length; i+=3) {
      assertSame("trim("  +ident[i]+")", ident[i+1], Utilities.trim(  ident[i]));
      if (ident[i] != ident[i+2]) continue; // Skip N/A test
      assertSame("trimNA("+ident[i]+")", ident[i+2], Utilities.trimNA(ident[i]));
    }
  }

  public void testToHexDump () {
    final byte[] input = new byte[256];
    for (int i = 0; i < input.length; i++) {
      input[i] = (byte)(i & 0xFF);
    }
    final String output1 =   "0:  \t00 01 02 03 04 05 06 07  08 09 0A 0B 0C 0D 0E 0F   |  ................\n"
                         +  "16:  \t10 11 12 13 14 15 16 17  18 19 1A 1B 1C 1D 1E 1F   |  ................\n"
                         +  "32:  \t20 21 22 23 24 25 26 27  28 29 2A 2B 2C 2D 2E 2F   |   !\"#$%&\'()*+,-./\n"
                         +  "48:  \t30 31 32 33 34 35 36 37  38 39 3A 3B 3C 3D 3E 3F   |  0123456789:;<=>?\n"
                         +  "64:  \t40 41 42 43 44 45 46 47  48 49 4A 4B 4C 4D 4E 4F   |  @ABCDEFGHIJKLMNO\n"
                         +  "80:  \t50 51 52 53 54 55 56 57  58 59 5A 5B 5C 5D 5E 5F   |  PQRSTUVWXYZ[\\]^_\n"
                         +  "96:  \t60 61 62 63 64 65 66 67  68 69 6A 6B 6C 6D 6E 6F   |  `abcdefghijklmno\n"
                         + "112:  \t70 71 72 73 74 75 76 77  78 79 7A 7B 7C 7D 7E 7F   |  pqrstuvwxyz{|}~.\n"
                         + "128:  \t80 81 82 83 84 85 86 87  88 89 8A 8B 8C 8D 8E 8F   |  ................\n"
                         + "144:  \t90 91 92 93 94 95 96 97  98 99 9A 9B 9C 9D 9E 9F   |  ................\n"
                         + "160:  \tA0 A1 A2 A3 A4 A5 A6 A7  A8 A9 AA AB AC AD AE AF   |  ................\n"
                         + "176:  \tB0 B1 B2 B3 B4 B5 B6 B7  B8 B9 BA BB BC BD BE BF   |  ................\n"
                         + "192:  \tC0 C1 C2 C3 C4 C5 C6 C7  C8 C9 CA CB CC CD CE CF   |  ................\n"
                         + "208:  \tD0 D1 D2 D3 D4 D5 D6 D7  D8 D9 DA DB DC DD DE DF   |  ................\n"
                         + "224:  \tE0 E1 E2 E3 E4 E5 E6 E7  E8 E9 EA EB EC ED EE EF   |  ................\n"
                         + "240:  \tF0 F1 F2 F3 F4 F5 F6 F7  F8 F9 FA FB FC FD FE FF   |  ................\n";
    final String output2 =   "0:  \t41 42 43 44 45 46 47 48  49 4A                     |  ABCDEFGHIJ      \n";
    final String output3 =   "0:  \t00 01 02 03 04 05 06 07  08 09 0A 0B 0C 0D 0E 0F \n"
                         +  "16:  \t10 11 12 13 14 15 16 17  18 19 1A 1B 1C 1D 1E 1F \n";
    final String output4 =  "0:  \t................................ !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNO\n"
                         + "80:  \tPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~.                                \n";
    final String output5 =        "41 42 43 44 45 46 47 48  49 4A                     |  ABCDEFGHIJ      \n";
    final String output6 =        "................................ !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNO\n"
                         +        "PQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~.                                \n";
    final String output7 = output5.replace('\n','#');
    final String output8 = output6.replace('\n','#');

    // 3-argument form
    assertEquals("toHexDump(input,0,256)", output1, Utilities.toHexDump(input,0,256).toString());
    assertEquals("toHexDump(input,65,10)", output2, Utilities.toHexDump(input,65,10).toString());

    // 5-argument form
    assertEquals("toHexDump(input,0,256,true,true)",  output1, Utilities.toHexDump(input,0,256,true,true).toString());
    assertEquals("toHexDump(input,65,10,true,true)",  output2, Utilities.toHexDump(input,65,10,true,true).toString());
    assertEquals("toHexDump(input,0,32,true,false)",  output3, Utilities.toHexDump(input,0,32,true,false).toString());
    assertEquals("toHexDump(input,0,128,false,true)", output4, Utilities.toHexDump(input,0,128,false,true).toString());

    // 7-argument form
    assertEquals("toHexDump(input,0,256,true,true,true,'\\n')",   output1, Utilities.toHexDump(input,0,256,true,true,true,"\n").toString());
    assertEquals("toHexDump(input,65,10,true,true,true,'\\n')",   output2, Utilities.toHexDump(input,65,10,true,true,true,"\n").toString());
    assertEquals("toHexDump(input,0,32,true,false,true,'\\n')",   output3, Utilities.toHexDump(input,0,32,true,false,true,"\n").toString());
    assertEquals("toHexDump(input,0,128,false,true,true,'\\n')",  output4, Utilities.toHexDump(input,0,128,false,true,true,"\n").toString());
    assertEquals("toHexDump(input,65,10,true,false,false,'\\n')", output5, Utilities.toHexDump(input,65,10,true,true,false,"\n").toString());
    assertEquals("toHexDump(input,0,128,false,true,false,'\\n')", output6, Utilities.toHexDump(input,0,128,false,true,false,"\n").toString());
    assertEquals("toHexDump(input,65,10,true,false,false,'#')",   output7, Utilities.toHexDump(input,65,10,true,true,false,"#").toString());
    assertEquals("toHexDump(input,0,128,false,true,false,'#')",   output8, Utilities.toHexDump(input,0,128,false,true,false,"#").toString());
  }

  public void testToHexString () {
    Object[] tests = {
      0x0000000000000000L, "00", "0000", "000000", "00000000", "0000000000", "000000000000", "00000000000000", "0000000000000000",
      0x000000000000000FL, "0F", "000F", "00000F", "0000000F", "000000000F", "00000000000F", "0000000000000F", "000000000000000F",
      0x00000000000000F0L, "F0", "00F0", "0000F0", "000000F0", "00000000F0", "0000000000F0", "000000000000F0", "00000000000000F0",
      0x0000000000000F00L, "00", "0F00", "000F00", "00000F00", "0000000F00", "000000000F00", "00000000000F00", "0000000000000F00",
      0x000000000000F000L, "00", "F000", "00F000", "0000F000", "000000F000", "00000000F000", "0000000000F000", "000000000000F000",
      0x00000000000F0001L, "01", "0001", "0F0001", "000F0001", "00000F0001", "0000000F0001", "000000000F0001", "00000000000F0001",
      0x0000000000F00010L, "10", "0010", "F00010", "00F00010", "0000F00010", "000000F00010", "00000000F00010", "0000000000F00010",
      0x000000000F000100L, "00", "0100", "000100", "0F000100", "000F000100", "00000F000100", "0000000F000100", "000000000F000100",
      0x00000000F0001000L, "00", "1000", "001000", "F0001000", "00F0001000", "0000F0001000", "000000F0001000", "00000000F0001000",
      0x0000000F0001000AL, "0A", "000A", "01000A", "0001000A", "0F0001000A", "000F0001000A", "00000F0001000A", "0000000F0001000A",
      0x000000F0001000A0L, "A0", "00A0", "1000A0", "001000A0", "F0001000A0", "00F0001000A0", "0000F0001000A0", "000000F0001000A0",
      0x00000F0001000A00L, "00", "0A00", "000A00", "01000A00", "0001000A00", "0F0001000A00", "000F0001000A00", "00000F0001000A00",
      0x0000F0001000A000L, "00", "A000", "00A000", "1000A000", "001000A000", "F0001000A000", "00F0001000A000", "0000F0001000A000",
      0x000F0001000A0002L, "02", "0002", "0A0002", "000A0002", "01000A0002", "0001000A0002", "0F0001000A0002", "000F0001000A0002",
      0x00F0001000A00020L, "20", "0020", "A00020", "00A00020", "1000A00020", "001000A00020", "F0001000A00020", "00F0001000A00020",
      0x0F0001000A000200L, "00", "0200", "000200", "0A000200", "000A000200", "01000A000200", "0001000A000200", "0F0001000A000200",
      0xF0001000A0002000L, "00", "2000", "002000", "A0002000", "00A0002000", "1000A0002000", "001000A0002000", "F0001000A0002000",
    };

    for (int i = 0; i < tests.length; i+=9) {
      long lval = (Long)tests[i];
      int  ival = (int)lval;
      assertEquals("toHexString("+ival+", 1)", tests[i+1], Utilities.toHexString(ival,1));
      assertEquals("toHexString("+ival+", 2)", tests[i+2], Utilities.toHexString(ival,2));
      assertEquals("toHexString("+ival+", 3)", tests[i+3], Utilities.toHexString(ival,3));
      assertEquals("toHexString("+ival+", 4)", tests[i+4], Utilities.toHexString(ival,4));

      assertEquals("toHexString("+lval+"L, 1)", tests[i+1], Utilities.toHexString(lval,1));
      assertEquals("toHexString("+lval+"L, 2)", tests[i+2], Utilities.toHexString(lval,2));
      assertEquals("toHexString("+lval+"L, 3)", tests[i+3], Utilities.toHexString(lval,3));
      assertEquals("toHexString("+lval+"L, 4)", tests[i+4], Utilities.toHexString(lval,4));
      assertEquals("toHexString("+lval+"L, 5)", tests[i+5], Utilities.toHexString(lval,5));
      assertEquals("toHexString("+lval+"L, 6)", tests[i+6], Utilities.toHexString(lval,6));
      assertEquals("toHexString("+lval+"L, 7)", tests[i+7], Utilities.toHexString(lval,7));
      assertEquals("toHexString("+lval+"L, 8)", tests[i+8], Utilities.toHexString(lval,8));

      assertEquals("toHexString("+ival+")",  tests[i+4], Utilities.toHexString(ival));
      assertEquals("toHexString("+lval+"L)", tests[i+8], Utilities.toHexString(lval));
    }
  }

  public void testToStringClassID () {
    //                        Class ID             Class ID             OUI       ICC            PCC
    final Object[] tests = { "00-00-00:0000.0000", 0x0000000000000000L, 0x000000, (short)0x0000, (short)0x0000,
                             "12-34-56:9876.5432", 0x0012345698765432L, 0x123456, (short)0x9876, (short)0x5432,
                             "AB-CD-EF:1234.ABCD", 0x00ABCDEF1234ABCDL, 0xABCDEF, (short)0x1234, (short)0xABCD };

    for (int i = 0; i < tests.length; i+=5) {
      assertEquals("toStringClassID(id)",          tests[ i ], Utilities.toStringClassID((Long)tests[i+1]));
      assertEquals("toStringClassID(oui,icc,pcc)", tests[ i ], Utilities.toStringClassID((Integer)tests[i+2],(Short)tests[i+3],(Short)tests[i+4]));
      assertEquals("fromStringClassID(..)",        tests[i+1], Utilities.fromStringClassID((String)tests[i]));
    }
  }

  public void testToStringDeviceID () {
    //                        Device ID       Device ID            OUI       Device
    final Object[] tests = { "00-00-00:0000", 0x0000000000000000L, 0x000000, (short)0x0000,
                             "12-34-56:ABCD", 0x001234560000ABCDL, 0x123456, (short)0xABCD,
                             "AB-CD-EF:1234", 0x00ABCDEF00001234L, 0xABCDEF, (short)0x1234 };

    for (int i = 0; i < tests.length; i+=4) {
      assertEquals("toStringDeviceID(id)",      tests[ i ], Utilities.toStringDeviceID((Long)tests[i+1]));
      assertEquals("toStringDeviceID(oui,dev)", tests[ i ], Utilities.toStringDeviceID((Integer)tests[i+2],(Short)tests[i+3]));
      assertEquals("fromStringDeviceID(..)",    tests[i+1], Utilities.fromStringDeviceID((String)tests[i]));
    }
  }

  public void testToStringOUI () {
    //                        OUI        OUI
    final Object[] tests = { "00-00-00", 0x00000000,
                             "12-34-56", 0x00123456,
                             "AB-CD-EF", 0x00ABCDEF };

    for (int i = 0; i < tests.length; i+=2) {
      assertEquals("toStringOUI(..)",   tests[ i ], Utilities.toStringOUI((Integer)tests[i+1]));
      assertEquals("fromStringOUI(..)", tests[i+1], Utilities.fromStringOUI((String)tests[i]));
    }
  }

  public void testNormalizeAngle360 () {
    for (double i = 0; i < 720.0; i+=0.25) {
      double a   = i;
      double b   = i - 360.0;
      double c   = i + 360.0;
      double exp = i % 360.0;
      assertEquals("normalizeAngle360("+a+")", exp, Utilities.normalizeAngle360(a));
      assertEquals("normalizeAngle360("+b+")", exp, Utilities.normalizeAngle360(b));
      assertEquals("normalizeAngle360("+c+")", exp, Utilities.normalizeAngle360(c));

      assertEquals("normalizeAngle360((Double)"+a+")", exp, Utilities.normalizeAngle360((Double)a));
      assertEquals("normalizeAngle360((Double)"+b+")", exp, Utilities.normalizeAngle360((Double)b));
      assertEquals("normalizeAngle360((Double)"+c+")", exp, Utilities.normalizeAngle360((Double)c));
    }
    assertEquals("normalizeAngle360(null)", null, Utilities.normalizeAngle360(null));
  }

  public void testNormalizeAngle180 () {
    for (double i = 0; i < 720.0; i+=0.25) {
      double a   = i;
      double b   = i - 360.0;
      double c   = i + 360.0;
      double j   = Utilities.normalizeAngle360(i);
      double exp = (j < 180.0)? j : j-360.0;
      assertEquals("normalizeAngle180("+a+")", exp, Utilities.normalizeAngle180(a));
      assertEquals("normalizeAngle180("+b+")", exp, Utilities.normalizeAngle180(b));
      assertEquals("normalizeAngle180("+c+")", exp, Utilities.normalizeAngle180(c));

      assertEquals("normalizeAngle180((Double)"+a+")", exp, Utilities.normalizeAngle180((Double)a));
      assertEquals("normalizeAngle180((Double)"+b+")", exp, Utilities.normalizeAngle180((Double)b));
      assertEquals("normalizeAngle180((Double)"+c+")", exp, Utilities.normalizeAngle180((Double)c));
    }
    assertEquals("normalizeAngle180(null)", null, Utilities.normalizeAngle180(null));
  }

  public void testGetFieldID () {
    try {
      // ==== BASIC TESTS ======================================================
      // Simply make sure our getFieldID(..) matches up with getFieldName(..)
      // and that our getField(..) matches up with getFieldByName(..).
      final HasFields[] tests = {
        Utilities.asHasFields(InetAddress.getByName("localhost")),
        Utilities.asHasFields(UUID.randomUUID()),
      };

      for (HasFields hf : tests) {
        for (int id = 0; id < hf.getFieldCount(); id++) {
          String name  = hf.getFieldName(id);
          int    expID = id;
          int    actID = Utilities.getFieldID(hf, name);

          assertEquals("getFieldID(hf, '"+name+"')", expID, actID);

          Object expVal = hf.getField(id);
          Object actVal = Utilities.getFieldByName(hf, name);

          assertEquals("getFieldByName(hf, '"+name+"')", expVal, actVal);
        }
      }

      // ==== SPECIFIC TESTS ===================================================
      // Tests some of the sets
      HasFields hf;

      hf = Utilities.asHasFields(InetAddress.getByName("localhost"));
      Utilities.setFieldByName(hf, "HostAddress", "10.0.0.1");
      assertEquals("setFieldByName(HostAddress)",  "10.0.0.1", Utilities.getFieldByName(hf, "HostAddress"));

      UUID uuid = UUID.randomUUID();
      hf = Utilities.asHasFields(UUID.randomUUID());
      Utilities.setFieldByName(hf, "UUID", uuid);
      assertEquals("setFieldByName(UUID)",  uuid.toString(), Utilities.getFieldByName(hf, "UUID"));


      // ==== SPECIAL CASES ====================================================
      // Tests some of the special handling for UUID and InetAddress


    }
    catch (UnknownHostException e) {
      throw new RuntimeException("Error creating test value", e);
    }
  }

  public void testAsHasFields () {
    // ==== InetAddress TESTS ==================================================
    try {
      InetAddress                             addr = InetAddress.getByName("localhost");
      Utilities.HasFieldsWrapper<InetAddress> act  = Utilities.asHasFields(addr);

      assertEquals("InetAddress getFieldCount()", 1,               act.getFieldCount());
      assertEquals("InetAddress getFieldName(0)", "HostAddress",   act.getFieldName(0));
      assertEquals("InetAddress getField(0)",     "127.0.0.1",     act.getField(0));

      act.setField(0, "10.0.0.1");
      assertEquals("InetAddress setField(0)",     "10.0.0.1",      act.getField(0));
    }
    catch (UnknownHostException e) {
      throw new RuntimeException("Error creating test value", e);
    }

    // ==== UUID TESTS =========================================================
    for (int i = 0; i < 16; i++) {
      UUID                             uuid = UUID.randomUUID();
      UUID                             val  = UUID.randomUUID();
      Utilities.HasFieldsWrapper<UUID> act  = Utilities.asHasFields(uuid);

      assertEquals("UUID getFieldCount()", 1,               act.getFieldCount());
      assertEquals("UUID getFieldName(0)", "UUID",          act.getFieldName(0));
      assertEquals("UUID getField(0)",     uuid.toString(), act.getField(0));

      act.setField(0, val.toString());
      assertEquals("UUID setField(0)",     val.toString(),  act.getField(0));
    }
  }

  public void testIsNumericType () {
    final Class<?>[] numericTypes = {
      Byte.TYPE,  Short.TYPE,  Integer.TYPE,  Long.TYPE,  Double.TYPE,
      Byte.class, Short.class, Integer.class, Long.class, Double.class,
      java.math.BigDecimal.class, java.math.BigInteger.class,
      java.util.concurrent.atomic.AtomicInteger.class,
      java.util.concurrent.atomic.AtomicLong.class,
      Number.class
    };
    final Class<?>[] otherTypes = {
      String.class, Object.class, List.class
    };

    for (Class<?> c : numericTypes) {
      assertEquals("isNumericType("+c+")", true, Utilities.isNumericType(c));
    }
    for (Class<?> c : otherTypes) {
      assertEquals("isNumericType("+c+")", false, Utilities.isNumericType(c));
    }
  }

  public void testCastValue () {
    // ==== TEST NUMERIC INPUTS ================================================
    for (int i = 0; i <= 6; i++) {
      Number value;
      switch (i) {
        case 0: value = null;     break;
        case 1: value = (byte)3;  break;
        case 2: value = (short)3; break;
        case 3: value = 3;        break;
        case 4: value = 3L;       break;
        case 5: value = 3.14f;    break;
        case 6: value = 3.14d;    break;
        default: throw new AssertionError("Bad i="+i);
      }
      String msg = (value == null)? "castValue(..) of null to "
                                  : "castValue(..) of "+value.getClass()+" to ";

      for (int j = 0; j <= 12; j++) {
        Class<?> clazz;
        Object   expVal;
        switch (j) {
          case  0: clazz = Byte.TYPE;     expVal = (value == null)? (byte )0 : value.byteValue();   break;
          case  1: clazz = Short.TYPE;    expVal = (value == null)? (short)0 : value.shortValue();  break;
          case  2: clazz = Integer.TYPE;  expVal = (value == null)? 0        : value.intValue();    break;
          case  3: clazz = Long.TYPE;     expVal = (value == null)? 0L       : value.longValue();   break;
          case  4: clazz = Float.TYPE;    expVal = (value == null)? 0f       : value.floatValue();  break;
          case  5: clazz = Double.TYPE;   expVal = (value == null)? 0d       : value.doubleValue(); break;
          case  6: clazz = Byte.class;    expVal = (value == null)? null     : value.byteValue();   break;
          case  7: clazz = Short.class;   expVal = (value == null)? null     : value.shortValue();  break;
          case  8: clazz = Integer.class; expVal = (value == null)? null     : value.intValue();    break;
          case  9: clazz = Long.class;    expVal = (value == null)? null     : value.longValue();   break;
          case 10: clazz = Float.class;   expVal = (value == null)? null     : value.floatValue();  break;
          case 11: clazz = Double.class;  expVal = (value == null)? null     : value.doubleValue(); break;
          case 12: clazz = String.class;  expVal = (value == null)? null     : value.toString();    break;
          default: throw new AssertionError("Bad j="+j);
        }
        assertEquals(msg+clazz, expVal,   Utilities.castValue(clazz, value));
      }
    }

    // ==== TEST STRING OUTPUTS ================================================
    //                 INPUT                                       OUTPUT
    Object[] tests = { null,                                       null,
                       new StringBuilder("Hello World!"),          "Hello World!",
                       42,                                         "42",
                       AsciiCharSequence.fromCharSequence("ABC"),  "ABC",
                     };
    for (int i = 0; i < tests.length; i+=2) {
      Object value = tests[i];
      String msg   = (value == null)? "castValue(..) of null to "
                                    : "castValue(..) of "+value.getClass()+" to ";
      String expStr   = String.valueOf(tests[i+1]);
      Object actStr   = Utilities.castValue(String.class,            value);
      Object actCSeq  = Utilities.castValue(CharSequence.class,      value);
      Object actAscii = Utilities.castValue(AsciiCharSequence.class, value);

      assertEquals(msg+"String",            expStr, String.valueOf(actStr));
      assertEquals(msg+"CharSequence",      expStr, String.valueOf(actCSeq));
      assertEquals(msg+"AsciiCharSequence", expStr, String.valueOf(actAscii));

      if (value != null) {
        assertInstanceOf(msg+"String",            String.class,            actStr);
        assertInstanceOf(msg+"CharSequence",      CharSequence.class,      actCSeq);
        assertInstanceOf(msg+"AsciiCharSequence", AsciiCharSequence.class, actAscii);
      }
    }

    // ==== TEST BOOLEAN OUTPUTS ===============================================
    //                     INPUT      Boolean.TYPE    Boolean.class
    Object[] moreTests = { null,      false,          null,
                           true,      true,           true,
                           false,     false,          false,
                           1,         true,           true,
                           0.0,       false,          null,
                           3.14,      true,           true,
                           -3.14,     true,           false,
                         };
    for (int i = 0; i < moreTests.length; i+=3) {
      Object value = moreTests[i];
      String msg   = (value == null)? "castValue(..) of null to "
                                    : "castValue(..) of "+value.getClass()+" to ";
      assertEquals(msg+"boolean",  moreTests[i+1], Utilities.castValue(Boolean.TYPE,  value));
      assertEquals(msg+"Boolean",  moreTests[i+2], Utilities.castValue(Boolean.class, value));
    }

    // ==== TEST PRIMITIVE OUTPUTS ===============================================
    //                     INPUT      type            null value,   value
    Object[] primTests = { (byte )42, Byte.TYPE,      (byte )0,     (byte )42,
                           (short)42, Short.TYPE,     (short)0,     (short)42,
                           42,        Integer.TYPE,   0,            42,
                           42L,       Long.TYPE,      0L,           42L,
                           42f,       Float.TYPE,     0f,           42f,
                           42d,       Double.TYPE,    0d,           42d,
                           false,     Boolean.TYPE,   false,        false,
                           true,      Boolean.TYPE,   false,        true,
                           'A',       Character.TYPE, '\0',         'A',
                         };
    for (int i = 0; i < primTests.length; i+=4) {
      Object   value = primTests[i];
      Class<?> type  = (Class<?>)primTests[i+1];

      assertEquals("castValue(..) of null to "+type,                  primTests[i+2], Utilities.castValue(type, null));
      assertEquals("castValue(..) of "+value.getClass()+" to "+type,  primTests[i+3], Utilities.castValue(type, value));
    }
  }

  public void testDeepCopy () {
    Map<String,VRTPacket>  map1 = new LinkedHashMap<String,VRTPacket>();
    Map<Integer,VRTPacket> map2 = new LinkedHashMap<Integer,VRTPacket>();
    VRTPacket              pkt0 = new BasicDataPacket();
    VRTPacket              pkt1 = new BasicDataPacket();
    VRTPacket              pkt2 = new BasicContextPacket();

    pkt0.setTimeStamp(TimeStamp.Y2K_GPS);
    pkt1.setTimeStamp(TimeStamp.getSystemTime());
    pkt2.setTimeStamp(TimeStamp.getSystemTime());

    map1.put("0", pkt0);   map2.put(0, pkt0);
    map1.put("1", pkt1);   map2.put(1, pkt1);
    map1.put("2", pkt2);   map2.put(2, pkt2);

    Map<String,VRTPacket>  copy1 = Utilities.deepCopy(map1);
    Map<Integer,VRTPacket> copy2 = Utilities.deepCopy(map2);

    assertEquals("deepCopy(..) for map1, size", map1.size(), copy1.size());
    assertEquals("deepCopy(..) for map2, size", map1.size(), copy2.size());

    for (int i = 0; i < map1.size(); i++) {
      String j = Integer.toString(i);

      assertEquals("deepCopy(..) for map1, key="+i+", equal", map1.get(j), copy1.get(j));
      assertEquals("deepCopy(..) for map2, key="+i+", equal", map2.get(i), copy2.get(i));

      assertEquals("deepCopy(..) for map1, key="+i+", copy",  true,  map1.get(j) != copy1.get(j));
      assertEquals("deepCopy(..) for map2, key="+i+", copy",  true,  map2.get(i) != copy2.get(i));
    }
  }

  public void testNewSingleThreadExecutor () {
    // 1-ARGUMENT FORM =========================================================
    Utilities.AdvancedExecutorService aes = Utilities.newSingleThreadExecutor(4);
    ThreadPoolExecutor                tpe = aes.getThreadPoolExecutor();

    assertEquals("CorePoolSize",    1,                         tpe.getCorePoolSize());
    assertEquals("MaximumPoolSize", 1,                         tpe.getMaximumPoolSize());
    assertEquals("Queue type",      LinkedBlockingQueue.class, tpe.getQueue().getClass());

    LinkedBlockingQueue<Runnable> queue = (LinkedBlockingQueue<Runnable>)tpe.getQueue();

    assertEquals("Queue size",              0, queue.size());
    assertEquals("Queue remainingCapacity", 4, queue.remainingCapacity());

    assertException("newSingleThreadExecutor(0)",  "testNewSingleThreadExecutor_bad_value_0",  IllegalArgumentException.class);
    assertException("newSingleThreadExecutor(-1)", "testNewSingleThreadExecutor_bad_value_m1", IllegalArgumentException.class);

    assertEquals("Pending tasks", 0, aes.shutdownNow().size());

    // 2-ARGUMENT FORM =========================================================
    String                            name = "NewSingleThreadExecutorTestThread";
    ThreadFactory                     fact = Utilities.newNamedThreadFactory(name);
    Utilities.AdvancedExecutorService aes2 = Utilities.newSingleThreadExecutor(4,fact);

    for (int i = 0; i < 8; i++) {
      try {
        String       v = "Test #" + i;
        TestRunnable r = new TestRunnable();
        Future<?>    f = aes2.submit(r,v);

        assertSame("Thread #"+i+" result",     v, f.get());
        assertEquals("Thread #"+i+" executed", 1, r.counter);
      }
      catch (ExecutionException e) {
        throw new RuntimeException("Error while running thread", e);
      }
      catch (InterruptedException e) {
        throw new RuntimeException("Error while running thread", e);
      }
    }

    assertEquals("Pending tasks", 0, aes2.shutdownNow().size());
  }

  public void testNewSingleThreadExecutor_bad_value_0 () {
    Utilities.newSingleThreadExecutor(0);
  }

  public void testNewSingleThreadExecutor_bad_value_m1 () {
    Utilities.newSingleThreadExecutor(-1);
  }


  public void testNewNamedThreadFactory () {
    String        name  = "NewNamedThreadFactoryTestThread";
    ThreadFactory fact  = Utilities.newNamedThreadFactory(name);


    for (int i = 0; i < 8; i++) {
      TestRunnable r = new TestRunnable();
      Thread       t = fact.newThread(r);

      try {
        t.start();
        t.join();
      }
      catch (InterruptedException e) {
        throw new RuntimeException("Error while running thread", e);
      }

      assertEquals("Thread #"+i+" name",     name+"-"+i, t.getName());
      assertEquals("Thread #"+i+" executed", 1,          r.counter);
    }
  }


  //////////////////////////////////////////////////////////////////////////////
  // NESTED CLASSES
  //////////////////////////////////////////////////////////////////////////////

  /** Sample {@link Runnable} that increments a counter when run. */
  private static class TestRunnable implements Runnable {
    int counter = 0;
    @Override
    public void run () {
      counter++;
    }
  }
}
