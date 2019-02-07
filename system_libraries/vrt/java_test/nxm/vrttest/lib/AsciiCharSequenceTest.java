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
import java.util.Arrays;
import java.util.List;
import nxm.vrt.lib.AsciiCharSequence;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static nxm.vrttest.inc.TestRunner.assertArrayEquals;
import static nxm.vrttest.inc.TestRunner.assertEquals;
import static nxm.vrttest.inc.TestRunner.assertException;

/** Tests for the {@link AsciiCharSequence} class. */
public class AsciiCharSequenceTest implements Testable {
  private static final char SUPERSCRIPT_ONE   = '\u00B9';
  private static final char SUPERSCRIPT_TWO   = '\u00B2';
  private static final char SUPERSCRIPT_THREE = '\u00B3';

  @Override
  public Class<?> getTestedClass () {
    return AsciiCharSequence.class;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    tests.add(new TestSet("AsciiCharSequence(..)", this,  "testAsciiCharSequence"));
    tests.add(new TestSet("charAt(..)",            this, "+testAsciiCharSequence"));
    tests.add(new TestSet("contentEquals(..)",     this,  "testContentEquals"));
    tests.add(new TestSet("equals(..)",            this,  "testEquals"));
    tests.add(new TestSet("fromCharSequence(..)",  this,  "testFromCharSequence"));
    tests.add(new TestSet("getBytes()",            this,  "testGetBytes"));
    tests.add(new TestSet("hashCode(..)",          this,  "testHashCode"));
    tests.add(new TestSet("length(..)",            this, "+testAsciiCharSequence"));
    tests.add(new TestSet("subSequence(..)",       this,  "testSubSequence"));
    tests.add(new TestSet("toString()",            this, "+testAsciiCharSequence"));
    tests.add(new TestSet("unwrap(..)",            this, "+testGetBytes"));
    tests.add(new TestSet("wrap(..)",              this,  "testWrap"));

    return tests;
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }

  @Override
  public String toString () {
    return "Tests for AsciiCharSequence";
  }

  /** Test cases all of which should match "Hello World!" */
  private static final TestCase[] TEST_HELLO = {
    new TestCase(new byte[] { (byte)'H', (byte)'e', (byte)'l', (byte)'l', (byte)'o', (byte)' ',
                              (byte)'W', (byte)'o', (byte)'r', (byte)'l', (byte)'d', (byte)'!' },
                 0,
                 12,
                 false,
                 "Hello World!",
                 "Hello World!"),
    new TestCase(new byte[] { 0, 1, 2, 3,
                              (byte)'H', (byte)'e', (byte)'l', (byte)'l', (byte)'o', (byte)' ',
                              (byte)'W', (byte)'o', (byte)'r', (byte)'l', (byte)'d', (byte)'!',
                              1, 2, 3, 4, 5, 6, 7, 8 },
                 4,
                 12,
                 false,
                 "Hello World!",
                 null),
    new TestCase(new byte[] { 0, 1, 2, 3,
                              (byte)'H', (byte)'e', (byte)'l', (byte)'l', (byte)'o', (byte)' ',
                              (byte)'W', (byte)'o', (byte)'r', (byte)'l', (byte)'d', (byte)'!',
                              1, 2, 3, 4, 5, 6, 7, 8 },
                 4,
                 12,
                 true, // <-- null terminated, but not applicable
                 "Hello World!",
                 null),
    new TestCase(new byte[] { 0, 1, 2, 3,
                              (byte)'H', (byte)'e', (byte)'l', (byte)'l', (byte)'o', (byte)' ',
                              (byte)'W', (byte)'o', (byte)'r', (byte)'l', (byte)'d', (byte)'!',
                              0, 0, 0, 0, 5, 6, 7, 8 },
                 4,
                 16,
                 true,
                 "Hello World!",
                 null),
  };

  /** Test cases all of which should match "" */
  private static final TestCase[] TEST_EMPTY = {
    new TestCase(new byte[] { }, 0, 0, false, "", ""),
    new TestCase(new byte[] { }, 0, 0, true,  "", ""),
    new TestCase(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, 0, 4, true, ""),
    new TestCase(new byte[] { 0, 1, 2, 3, 0, 5, 6, 7, 8, 9 }, 4, 4, true, ""),
    new TestCase(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, 0, 0, true, ""),
    new TestCase(new byte[] { 0, 1, 2, 3, 0, 5, 6, 7, 8, 9 }, 4, 0, true, ""),
    new TestCase(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, 0, 0, false, ""),
    new TestCase(new byte[] { 0, 1, 2, 3, 0, 5, 6, 7, 8, 9 }, 4, 0, false, ""),
  };

  private static final TestCase[] TESTS = {
    new TestCase(new byte[] { (byte)'1', (byte)0xB9,
                              (byte)'2', (byte)0xB2,
                              (byte)'3', (byte)0xB3, },
                 0,
                 6,
                 false,
                 "1?2?3?",
                 "1"+SUPERSCRIPT_ONE+"2"+SUPERSCRIPT_TWO+"3"+SUPERSCRIPT_THREE),
  };

  private static final List<TestCase> ALL_TESTS = new ArrayList<TestCase>();
  static {
    ALL_TESTS.addAll(Arrays.asList(TEST_HELLO));
    ALL_TESTS.addAll(Arrays.asList(TEST_EMPTY));
    ALL_TESTS.addAll(Arrays.asList(TESTS));
  }

  public void testAsciiCharSequence () {
    // 3-ARGUMENT FORMAT =======================================================
    for (TestCase tc : ALL_TESTS) {
      if (tc.nullTerm) continue;
      String            msg = "AsciiCharSequence(chars,off,len) for '"+tc.toString+"'";
      AsciiCharSequence acs = new AsciiCharSequence(tc.chars, tc.off, tc.len);

      assertEquals(msg+" length()",          tc.length,   acs.length());
      assertEquals(msg+" toString()",        tc.toString, acs.toString());
      assertEquals(msg+" contentEquals(..)", true,        acs.contentEquals(tc.toString));

      for (int i = 0; i < tc.length; i++) {
        assertEquals(msg+" charAt("+i+")", tc.toString.charAt(i), acs.charAt(i));
      }
    }

    // 4-ARGUMENT FORMAT =======================================================
    for (TestCase tc : ALL_TESTS) {
      String            msg = "AsciiCharSequence(chars,off,len,term) for '"+tc.toString+"'";
      AsciiCharSequence acs = new AsciiCharSequence(tc.chars, tc.off, tc.len, tc.nullTerm);

      assertEquals(msg+" length()",          tc.length,   acs.length());
      assertEquals(msg+" toString()",        tc.toString, acs.toString());
      assertEquals(msg+" contentEquals(..)", true,        acs.contentEquals(tc.toString));

      for (int i = 0; i < tc.length; i++) {
        assertEquals(msg+" charAt("+i+")", tc.toString.charAt(i), acs.charAt(i));
      }
    }
  }

  public void testWrap () {
    for (TestCase tc : ALL_TESTS) {
      if (tc.nullTerm) continue;
      String            msg = "wrap(chars,off,len) for '"+tc.toString+"'";
      AsciiCharSequence acs = AsciiCharSequence.wrap(tc.chars, tc.off, tc.len, false);

      assertEquals(msg+" length()",          tc.length,   acs.length());
      assertEquals(msg+" toString()",        tc.toString, acs.toString());
      assertEquals(msg+" contentEquals(..)", true,        acs.contentEquals(tc.toString));

      for (int i = 0; i < tc.length; i++) {
        assertEquals(msg+" charAt("+i+")", tc.toString.charAt(i), acs.charAt(i));
      }
    }

    assertException("wrap(chars,-1,3)", "testWrap_badOffset1", ArrayIndexOutOfBoundsException.class);
    assertException("wrap(chars,42,3)", "testWrap_badOffset2", ArrayIndexOutOfBoundsException.class);
    assertException("wrap(chars,1,-1)", "testWrap_badLength1", ArrayIndexOutOfBoundsException.class);
    assertException("wrap(chars,1,4)",  "testWrap_badLength2", ArrayIndexOutOfBoundsException.class);
  }

  public void testWrap_badOffset1 () {
    byte[] chars = { 0, (byte)'A', (byte)'B', (byte)'C' };
    AsciiCharSequence.wrap(chars, -1, 3, false);
  }

  public void testWrap_badOffset2 () {
    byte[] chars = { 0, (byte)'A', (byte)'B', (byte)'C' };
    AsciiCharSequence.wrap(chars, 42, 3, false);
  }

  public void testWrap_badLength1 () {
    byte[] chars = { 0, (byte)'A', (byte)'B', (byte)'C' };
    AsciiCharSequence.wrap(chars, 1, -1, false);
  }

  public void testWrap_badLength2 () {
    byte[] chars = { 0, (byte)'A', (byte)'B', (byte)'C' };
    AsciiCharSequence.wrap(chars, 1, 4, false);
  }

  public void testFromCharSequence () {
    for (TestCase tc : ALL_TESTS) {
      if (tc.nullTerm) continue;
      if (tc.fromCS == null) continue;
      AsciiCharSequence exp  = new AsciiCharSequence(tc.chars, tc.off, tc.len);
      AsciiCharSequence act1 = AsciiCharSequence.fromCharSequence(tc.fromCS);
      AsciiCharSequence act2 = AsciiCharSequence.fromCharSequence(tc.fromCS,(byte)'?');

      assertEquals("fromCharSequence(seq) for '"+tc.toString+"'",     exp, act1);
      assertEquals("fromCharSequence(seq,'?') for '"+tc.toString+"'", exp, act2);
    }

    String str = "1"+SUPERSCRIPT_ONE+"2"+SUPERSCRIPT_TWO+"3"+SUPERSCRIPT_THREE;
    assertException("fromCharSequence(seq,null) for '"+str+"'", "testFromCharSequence_notASCII",
                                                                IllegalArgumentException.class);
  }

  public void testFromCharSequence_notASCII () {
    String str = "1"+SUPERSCRIPT_ONE+"2"+SUPERSCRIPT_TWO+"3"+SUPERSCRIPT_THREE;
    AsciiCharSequence.fromCharSequence(str,null);
  }

  @SuppressWarnings("all") // hide .equals(null) warnings
  public void testEquals () {
    for (TestCase a : TEST_HELLO) {
      AsciiCharSequence _a = AsciiCharSequence.wrap(a.chars, a.off, a.len, a.nullTerm);

      for (TestCase b : TEST_HELLO) {
        AsciiCharSequence _b = AsciiCharSequence.wrap(b.chars, b.off, b.len, b.nullTerm);

        assertEquals("'"+_a+"' & '"+_b+"'", true, _a.equals(_b));
      }
    }

    for (TestCase a : TEST_EMPTY) {
      AsciiCharSequence _a = AsciiCharSequence.wrap(a.chars, a.off, a.len, a.nullTerm);

      for (TestCase b : TEST_EMPTY) {
        AsciiCharSequence _b = AsciiCharSequence.wrap(b.chars, b.off, b.len, b.nullTerm);

        assertEquals("'"+_a+"' & '"+_b+"'", true, _a.equals(_b));
      }
    }

    for (TestCase a : TEST_EMPTY) {
      AsciiCharSequence _a = AsciiCharSequence.wrap(a.chars, a.off, a.len, a.nullTerm);

      for (TestCase b : TEST_HELLO) {
        AsciiCharSequence _b = AsciiCharSequence.wrap(b.chars, b.off, b.len, b.nullTerm);

        assertEquals("'"+_a+"' & '"+_b+"'", false, _a.equals(_b));
        assertEquals("'"+_a+"' & null",     false, _a.equals(null));
        assertEquals("'"+_b+"' & null",     false, _b.equals(null));
      }
    }
  }

  @SuppressWarnings("all") // hide .equals(null) warnings
  public void testContentEquals () {
    for (TestCase a : TEST_HELLO) {

      AsciiCharSequence _a = AsciiCharSequence.wrap(a.chars, a.off, a.len, a.nullTerm);

      for (TestCase b : TEST_HELLO) {
        AsciiCharSequence _b = AsciiCharSequence.wrap(b.chars, b.off, b.len, b.nullTerm);

        assertEquals("'"+_a+"' & '"+_b+"'",       true, _a.contentEquals(_b));
        assertEquals("'"+_a+"' & <String>",       true, _a.contentEquals(b.toString));
        assertEquals("'"+_a+"' & <CharSequence>", true, _a.contentEquals(new StringBuilder(b.toString)));
      }
    }

    for (TestCase a : TEST_EMPTY) {
      AsciiCharSequence _a = AsciiCharSequence.wrap(a.chars, a.off, a.len, a.nullTerm);

      for (TestCase b : TEST_HELLO) {
        AsciiCharSequence _b = AsciiCharSequence.wrap(b.chars, b.off, b.len, b.nullTerm);

        assertEquals("'"+_a+"' & '"+_b+"'",       false, _a.contentEquals(_b));
        assertEquals("'"+_a+"' & <String>",       false, _a.contentEquals(b.toString));
        assertEquals("'"+_a+"' & <CharSequence>", false, _a.contentEquals(new StringBuilder(b.toString)));
        assertEquals("'"+_a+"' & null",           false, _a.contentEquals(null));
      }
    }
  }

  public void testHashCode () {
    for (TestCase a : TEST_HELLO) {
      AsciiCharSequence _a = AsciiCharSequence.wrap(a.chars, a.off, a.len, a.nullTerm);

      for (TestCase b : TEST_HELLO) {
        AsciiCharSequence _b = AsciiCharSequence.wrap(b.chars, b.off, b.len, b.nullTerm);

        assertEquals("Matching values have same hash code", _a.hashCode(), _b.hashCode());
        assertEquals("Non-empty string has non-zero hash",  true,          (_a.hashCode() != 0));
      }
    }

    for (TestCase a : TEST_EMPTY) {
      AsciiCharSequence _a = AsciiCharSequence.wrap(a.chars, a.off, a.len, a.nullTerm);

      assertEquals("Empty string has hash of zero", 0, _a.hashCode());
    }
  }

  public void testSubSequence () {
    final String str = "Hello World!";

    for (TestCase a : TEST_HELLO) {
      AsciiCharSequence _a = AsciiCharSequence.wrap(a.chars, a.off, a.len, a.nullTerm);

      for (int start = 0; start <= _a.length(); start++) {
        for (int end = start; end <= str.length(); end++) {
          String       exp = str.substring(start, end);
          CharSequence act = _a.subSequence(start, end);

          assertEquals("subSequence("+start+", "+end+")", true, exp.contentEquals(act));
        }
      }
    }
  }

  public void testGetBytes () {
    for (TestCase tc : ALL_TESTS) {
      if (tc.nullTerm) continue;
      String            msg = "AsciiCharSequence(chars,off,len) for '"+tc.toString+"'";
      AsciiCharSequence acs = new AsciiCharSequence(tc.chars, tc.off, tc.len);
      byte[]            exp = new byte[tc.length];

      System.arraycopy(tc.chars, tc.off, exp, 0, tc.length);
      for (int i = 0; i < exp.length; i++) {
        if (exp[i] < 0) exp[i] = (byte)'?';
      }

      assertArrayEquals(msg+" getBytes()", exp, acs.getBytes());
      assertArrayEquals(msg+" unwrap()",   exp, acs.unwrap());
    }
  }


  //////////////////////////////////////////////////////////////////////////////
  // NESTED CLASSES
  //////////////////////////////////////////////////////////////////////////////
  /** Describes a test case. */
  private static final class TestCase {
    final byte[]       chars;
    final int          off;
    final int          len;
    final int          length;
    final boolean      nullTerm;
    final String       toString;
    final CharSequence fromCS;

    /** Creates a new instance where fromCS=null. */
    public TestCase (byte[] chars, int off, int len, boolean nullTerm,
                     String toString) {
      this(chars, off, len, nullTerm, toString, null);
    }

    /** Creates a new instance. */
    public TestCase (byte[] chars, int off, int len, boolean nullTerm,
                     String toString, CharSequence fromCS) {
      this.chars    = chars;
      this.off      = off;
      this.len      = len;
      this.length   = toString.length();
      this.nullTerm = nullTerm;
      this.toString = toString;
      this.fromCS   = fromCS;
    }
  }

}
