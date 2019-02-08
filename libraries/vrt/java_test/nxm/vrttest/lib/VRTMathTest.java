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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import nxm.vrt.lib.AsciiCharSequence;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.VRTConfig;
import nxm.vrt.lib.VRTMath;
import nxm.vrt.lib.VRTPacket.DataItemFormat;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.intBitsToFloat;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static nxm.vrt.lib.VRTMath.*;
import static nxm.vrttest.inc.TestRunner.assertArrayEquals;
import static nxm.vrttest.inc.TestRunner.assertBufEquals;
import static nxm.vrttest.inc.TestRunner.assertEquals;

/** Test cases for the {@link VRTMath} class. */
public class VRTMathTest implements Testable {

  @Override
  public Class<?> getTestedClass () {
    return VRTMath.class;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    // IEEE 754-2008 half-precision
    tests.add(new TestSet("fromHalf",                   this,  "testFromHalf"));
    tests.add(new TestSet("toHalf"  ,                   this,  "testToHalf"));

    // VRT Fixed-Point (64-bit)
    tests.add(new TestSet("fromLong64",                 this, "+testFromDouble64"));
    tests.add(new TestSet("fromInt64",                  this, "+testFromDouble64"));
    tests.add(new TestSet("fromFloat64",                this, "+testFromDouble64"));
    tests.add(new TestSet("fromDouble64",               this,  "testFromDouble64"));
    tests.add(new TestSet("toLong64",                   this, "+testFromDouble64"));
    tests.add(new TestSet("toInt64",                    this, "+testFromDouble64"));
    tests.add(new TestSet("toFloat64",                  this, "+testFromDouble64"));
    tests.add(new TestSet("toDouble64",                 this, "+testFromDouble64"));

    // VRT Fixed-Point (32-bit)
    tests.add(new TestSet("fromLong32",                 this, "+testFromDouble32"));
    tests.add(new TestSet("fromInt32",                  this, "+testFromDouble32"));
    tests.add(new TestSet("fromFloat32",                this, "+testFromDouble32"));
    tests.add(new TestSet("fromDouble32",               this,  "testFromDouble32"));
    tests.add(new TestSet("toLong32",                   this, "+testFromDouble32"));
    tests.add(new TestSet("toInt32",                    this, "+testFromDouble32"));
    tests.add(new TestSet("toFloat32",                  this, "+testFromDouble32"));
    tests.add(new TestSet("toDouble32",                 this, "+testFromDouble32"));

    // VRT Fixed-Point (16-bit)
    tests.add(new TestSet("fromLong16",                 this, "+testFromDouble16"));
    tests.add(new TestSet("fromInt16",                  this, "+testFromDouble16"));
    tests.add(new TestSet("fromFloat16",                this, "+testFromDouble16"));
    tests.add(new TestSet("fromDouble16",               this,  "testFromDouble16"));
    tests.add(new TestSet("toLong16",                   this, "+testFromDouble16"));
    tests.add(new TestSet("toInt16",                    this, "+testFromDouble16"));
    tests.add(new TestSet("toFloat16",                  this, "+testFromDouble16"));
    tests.add(new TestSet("toDouble16",                 this, "+testFromDouble16"));

    // VRT Floating-Point
    tests.add(new TestSet("fromVRTFloat",               this,  "testVRTFloat"));
    tests.add(new TestSet("fromVRTFloat32",             this, "+testVRTFloat"));
    tests.add(new TestSet("fromVRTFloat64",             this, "+testVRTFloat"));
    tests.add(new TestSet("toVRTFloat",                 this, "+testVRTFloat"));
    tests.add(new TestSet("toVRTFloat32",               this, "+testVRTFloat"));
    tests.add(new TestSet("toVRTFloat64",               this, "+testVRTFloat"));

    // Pack/Unpack (Integer/Floating-Point)
    tests.add(new TestSet("packDouble",                 this,  "testPackDouble"));
    tests.add(new TestSet("packFloat",                  this,  "testPackFloat"));
    tests.add(new TestSet("packLong",                   this,  "testPackLong"));
    tests.add(new TestSet("packInt",                    this,  "testPackInt"));
    tests.add(new TestSet("packInt24",                  this,  "testPackInt24"));
    tests.add(new TestSet("packShort",                  this,  "testPackShort"));
    tests.add(new TestSet("packByte",                   this,  "testPackByte"));

    tests.add(new TestSet("unpackDouble",               this,  "testUnpackDouble"));
    tests.add(new TestSet("unpackFloat",                this,  "testUnpackFloat"));
    tests.add(new TestSet("unpackLong",                 this,  "testUnpackLong"));
    tests.add(new TestSet("unpackInt",                  this,  "testUnpackInt"));
    tests.add(new TestSet("unpackInt24",                this,  "testUnpackInt24"));
    tests.add(new TestSet("unpackShort",                this,  "testUnpackShort"));
    tests.add(new TestSet("unpackByte",                 this,  "testUnpackByte"));

    // Pack/Unpack (Bits)
    tests.add(new TestSet("packBits32",                 this, "+testPackBits64"));
    tests.add(new TestSet("packBits64",                 this,  "testPackBits64"));
    tests.add(new TestSet("unpackBits32",               this, "+testUnpackBits64"));
    tests.add(new TestSet("unpackBits64",               this,  "testUnpackBits64"));

    // Pack/Unpack (Bytes)
    tests.add(new TestSet("packBytes",                  this,  "testPackBytes"));
    tests.add(new TestSet("unpackBytes",                this,  "testUnpackBytes"));

    // Pack/Unpack (Boolean)
    tests.add(new TestSet("packBoolNull",               this,  "testPackBoolNull"));
    tests.add(new TestSet("packBoolean",                this,  "testPackBoolean"));
    tests.add(new TestSet("unpackBoolNull",             this,  "testUnpackBoolNull"));
    tests.add(new TestSet("unpackBoolean",              this,  "testUnpackBoolean"));

    // Pack/Unpack (String)
    tests.add(new TestSet("lengthUTF8",                 this, "+testPackAscii"));
    tests.add(new TestSet("packAscii",                  this,  "testPackAscii"));
    tests.add(new TestSet("packUTF8",                   this, "+testPackAscii"));
    tests.add(new TestSet("unpackAscii",                this, "+testPackAscii"));
    tests.add(new TestSet("unpackUTF8",                 this, "+testPackAscii"));

    // Pack/Unpack (Object)
    tests.add(new TestSet("packInetAddr",               this, "+testUnpackInetAddr"));
    tests.add(new TestSet("packMetadata",               this, "+testUnpackMetadata"));
    tests.add(new TestSet("packRecord",                 this, "+testUnpackRecord"));
    tests.add(new TestSet("packTimeStamp",              this, "+testUnpackTimeStamp"));
    tests.add(new TestSet("packUUID",                   this, "+testUnpackUUID"));
    tests.add(new TestSet("unpackInetAddr",             this,  "testUnpackInetAddr"));
    tests.add(new TestSet("unpackMetadata",             this, "!testUnpackMetadata"));
    tests.add(new TestSet("unpackRecord",               this, "!testUnpackRecord"));
    tests.add(new TestSet("unpackTimeStamp",            this,  "testUnpackTimeStamp"));
    tests.add(new TestSet("unpackUUID",                 this,  "testUnpackUUID"));

    return tests;
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }

  /** Tests the fromVRTFloat(..) and toVRTFloat(..) functions.
   *  @param fmt    The format to use.
   *  @param dSize  The data size (2..64).
   *  @param bits1  The bits.
   *  @param num    Numerator of expected value.
   *  @param denom  Denominator of expected value.
   */
  private static void _testVRTFloat (DataItemFormat fmt, int dSize, long bits1, long num, long denom) {
    double val   = ((double)num) / ((double)denom);
    long   bits2 = toVRTFloat(fmt, dSize, val); // 'bits1' does not need to match 'bits2'
    String msg1  = "fromVRTFloat(..) for "+fmt+" with "+num+"/"+denom+" (0x"+Long.toHexString(bits1)+")";
    String msg2  =   "toVRTFloat(..) for "+fmt+" with "+num+"/"+denom+" (0x"+Long.toHexString(bits2)+")";

    assertEquals(msg1, val, fromVRTFloat(fmt, dSize, bits1));
    assertEquals(msg2, val, fromVRTFloat(fmt, dSize, bits2));

    long bits64 = toVRTFloat64(fmt.isSigned(), fmt.getExponentBits(), dSize, val);
    assertEquals("toVRTFloat64(..)",   bits2, bits64);
    assertEquals("fromVRTFloat64(..)", val,   fromVRTFloat(fmt, dSize, bits64));

    if (dSize <= 32) {
      int bits32 = toVRTFloat32(fmt.isSigned(), fmt.getExponentBits(), dSize, val);
      assertEquals("toVRTFloat32(..)",   (int)bits2, bits32);
      assertEquals("fromVRTFloat32(..)", val,        fromVRTFloat(fmt, dSize, bits32));
    }
  }

  /** Tests the toVRTFloat(..) function.
   *  @param fmt    The format to use.
   *  @param dSize  The data size (2..64).
   *  @param num    Numerator of expected value.
   *  @param denom  Denominator of expected value.
   */
  private static void _testVRTFloat(DataItemFormat fmt, int dSize, long num, long denom) {
    double val   = ((double)num) / ((double)denom);
    long   bits2 = toVRTFloat(fmt, dSize, val);
    String msg2  = "toVRTFloat(..) for "+fmt+" with "+num+"/"+denom+" (0x"+Long.toHexString(bits2)+")";

    assertEquals(msg2, val, fromVRTFloat(fmt, dSize, bits2));
  }

  public void testVRTFloat () {
    // ==== SPECIFIED TESTS (D-1) ==============================================
    // This set of tests taken directly from ANSI/VITA 49.0 Appendix D, Table D-1
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x1F, +7,8);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x1E, +7,16);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x1D, +7,32);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x1C, +7,64);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x10, +4,64); // Table D-1 has 1/32 but should be 4/64
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x08, +1,32);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x07, +1,8);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x06, +1,16);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x05, +1,32);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x04, +1,64);

    // ==== SPECIFIED TESTS (D-2) ==============================================
    // This set of tests taken directly from ANSI/VITA 49.0 Appendix D, Table D-2
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x0F, +3,4);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x0E, +3,8);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x0D, +3,16);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x0C, +3,32);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x07, +1,4);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x06, +1,8);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x05, +1,16);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x04, +1,32);

    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x1C, -1,32);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x1D, -1,16);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x1E, -1,8);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x1F, -1,4);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x10, -1,8);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x11, -1,4);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x12, -1,2);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, 0x13, -1,1);

    // ==== VRT1 ===============================================================
    _testVRTFloat(DataItemFormat.SignedVRT1, 5, 0x1F, -1,8);
    _testVRTFloat(DataItemFormat.SignedVRT1, 5, 0x11, -1,1);
    _testVRTFloat(DataItemFormat.SignedVRT1, 5, 0x10, -1,2);

    // ==== VRT2 ===============================================================
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x07, +1,8);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x0A, +2,16);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x11, +4,32);

    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x03, +0,8);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x02, +0,16);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x01, +0,32);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, 0x00, +0,64);

    _testVRTFloat(DataItemFormat.SignedVRT2, 5, +3,32);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, +3,16);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, +3,8);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, +3,4);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, -3,32);

    _testVRTFloat(DataItemFormat.SignedVRT2, 5, -1,32);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, -1,16);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, -1,8);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, -1,4);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, -1,2);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, +1,2);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, +1,4);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, +1,8);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, +1,16);
    _testVRTFloat(DataItemFormat.SignedVRT2, 5, +1,32);

    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, +7,8);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, +7,32);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, +7,64);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, +1,32);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, +1,16);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, +1,8);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, +1,4);
    _testVRTFloat(DataItemFormat.UnsignedVRT2, 5, +1,2);

    // ==== VRT3 ===============================================================
    _testVRTFloat(DataItemFormat.SignedVRT3, 32, +1,2);
    _testVRTFloat(DataItemFormat.SignedVRT3, 32, +1,32);
    _testVRTFloat(DataItemFormat.SignedVRT3, 32, +57,256);
    _testVRTFloat(DataItemFormat.SignedVRT3, 32, +1,256);
    _testVRTFloat(DataItemFormat.SignedVRT3, 32, -1,256);
    _testVRTFloat(DataItemFormat.SignedVRT3, 32, -57,256);
    _testVRTFloat(DataItemFormat.SignedVRT3, 32, -1,32);
    _testVRTFloat(DataItemFormat.SignedVRT3, 32, -1,2);

    _testVRTFloat(DataItemFormat.UnsignedVRT3, 32, +1,2);
    _testVRTFloat(DataItemFormat.UnsignedVRT3, 32, +1,32);
    _testVRTFloat(DataItemFormat.UnsignedVRT3, 32, +57,256);
    _testVRTFloat(DataItemFormat.UnsignedVRT3, 32, +1,256);

    // ==== VRT4 ===============================================================
    _testVRTFloat(DataItemFormat.SignedVRT4, 12, -7,128);
    _testVRTFloat(DataItemFormat.SignedVRT4, 12, -1,128);
    _testVRTFloat(DataItemFormat.SignedVRT4, 12, -15,256);
    _testVRTFloat(DataItemFormat.SignedVRT4, 12, -7,256);
    _testVRTFloat(DataItemFormat.SignedVRT4, 12, -7,256);
    _testVRTFloat(DataItemFormat.SignedVRT4, 12, -15,256);
    _testVRTFloat(DataItemFormat.SignedVRT4, 12, -1,128);
    _testVRTFloat(DataItemFormat.SignedVRT4, 12, -7,128);

    _testVRTFloat(DataItemFormat.UnsignedVRT4, 12, +1,128);
    _testVRTFloat(DataItemFormat.UnsignedVRT4, 12, +7,128);
    _testVRTFloat(DataItemFormat.UnsignedVRT4, 12, +7,256);
    _testVRTFloat(DataItemFormat.UnsignedVRT4, 12, +15,256);

    // ==== VRT5 ===============================================================
    _testVRTFloat(DataItemFormat.SignedVRT5, 16, +1,32);
    _testVRTFloat(DataItemFormat.SignedVRT5, 16, +104,256);
    _testVRTFloat(DataItemFormat.SignedVRT5, 16, +1,256);
    _testVRTFloat(DataItemFormat.SignedVRT5, 16, -1,256);
    _testVRTFloat(DataItemFormat.SignedVRT5, 16, -104,256);
    _testVRTFloat(DataItemFormat.SignedVRT5, 16, -1,32);

    _testVRTFloat(DataItemFormat.UnsignedVRT5, 16, +1,256);
    _testVRTFloat(DataItemFormat.UnsignedVRT5, 16, +104,256);

    // ==== VRT6 ===============================================================
    _testVRTFloat(DataItemFormat.SignedVRT6, 16, +104,256);
    _testVRTFloat(DataItemFormat.SignedVRT6, 16, -104,256);

    _testVRTFloat(DataItemFormat.SignedVRT6, 58, +1,2);
    _testVRTFloat(DataItemFormat.SignedVRT6, 58, +104,256);
    _testVRTFloat(DataItemFormat.SignedVRT6, 58, +1,8192);
    _testVRTFloat(DataItemFormat.SignedVRT6, 58, +1,4294967296L);
    _testVRTFloat(DataItemFormat.SignedVRT6, 58, -1,4294967296L);
    _testVRTFloat(DataItemFormat.SignedVRT6, 58, -1,8192);
    _testVRTFloat(DataItemFormat.SignedVRT6, 58, -104,256);
    _testVRTFloat(DataItemFormat.SignedVRT6, 58, -1,2);

    _testVRTFloat(DataItemFormat.UnsignedVRT6, 16, +104,256);

    _testVRTFloat(DataItemFormat.UnsignedVRT6, 58, +1,2);
    _testVRTFloat(DataItemFormat.UnsignedVRT6, 58, +104,256);
    _testVRTFloat(DataItemFormat.UnsignedVRT6, 58, +1,8192);
    _testVRTFloat(DataItemFormat.UnsignedVRT6, 58, +1,4294967296L);

    // ==== SPECIAL CASES ======================================================
    // Special cases that previously resulted in different values in Java vs C++
    // due to issues with UInt64 constants (see VRT-40 for details).
    assertEquals("fromVRTFloat(SignedVRT6, 7, 0x40)", -1.0842021724855044E-19,
                 VRTMath.fromVRTFloat(DataItemFormat.SignedVRT6, 7, 0x40));
  }

  public void testFromHalf () {
    // The following test cases based on the "Half precision examples" given on:
    //   Wikipedia. "Half-precision floating-point format."
    //   https://en.wikipedia.org/wiki/Half-precision_floating-point_format
    //   [Last Accessed 8 Aug 2013]
    // Note however, that the tests below include additional digits of precision
    // and include both positive and negative values.
    assertEquals("fromHalf(..)", Float.NEGATIVE_INFINITY, fromHalf((short)0xFC00)); // -Inf
    assertEquals("fromHalf(..)", -2.0f,                   fromHalf((short)0xC000)); // -2.0
    assertEquals("fromHalf(..)", -1.0f,                   fromHalf((short)0xBC00)); // -1.0
    assertEquals("fromHalf(..)", -6.1035156E-5f,          fromHalf((short)0x8400)); // Smallest Negative Normalized
    assertEquals("fromHalf(..)", -6.0975550E-5f,          fromHalf((short)0x83FF)); // Largest  Negative Denorm
    assertEquals("fromHalf(..)", -5.9604645E-8f,          fromHalf((short)0x8001)); // Smallest Negative Denorm
    assertEquals("fromHalf(..)", -0.0f,                   fromHalf((short)0x8000)); // -0.0
    assertEquals("fromHalf(..)", +0.0f,                   fromHalf((short)0x0000)); // +0.0
    assertEquals("fromHalf(..)", +5.9604645E-8f,          fromHalf((short)0x0001)); // Smallest Positive Denorm
    assertEquals("fromHalf(..)", +6.0975550E-5f,          fromHalf((short)0x03FF)); // Largest  Positive Denorm
    assertEquals("fromHalf(..)", +6.1035156E-5f,          fromHalf((short)0x0400)); // Smallest Positive Normalized
    assertEquals("fromHalf(..)", +1.0f,                   fromHalf((short)0x3C00)); // +1.0
    assertEquals("fromHalf(..)", +2.0f,                   fromHalf((short)0x4000)); // +2.0
    assertEquals("fromHalf(..)", Float.POSITIVE_INFINITY, fromHalf((short)0x7C00)); // +Inf

    assertEquals("fromHalf(..)", -0.33325195f,            fromHalf((short)0xB555)); // -1/3
    assertEquals("fromHalf(..)", +0.33325195f,            fromHalf((short)0x3555)); // +1/3

    // Additional tests for NaN
    assertEquals("fromHalf(..)", Float.NaN,               fromHalf((short)0x7C01)); // NaN ("Smallest"   Value)
    assertEquals("fromHalf(..)", Float.NaN,               fromHalf((short)0x7E00)); // NaN ("Normalized" Value)
    assertEquals("fromHalf(..)", Float.NaN,               fromHalf((short)0x7FFF)); // NaN ("Largest"    Value)
  }


  public void testToHalf () {
    // The following test cases based on the "Half precision examples" given on:
    //   Wikipedia. "Half-precision floating-point format."
    //   https://en.wikipedia.org/wiki/Half-precision_floating-point_format
    //   [Last Accessed 8 Aug 2013]
    // Note however, that the tests below include additional digits of precision
    // and include both positive and negative values.
    assertEquals("toHalf(..)", (short)0xFC00, toHalf(Float.NEGATIVE_INFINITY)); // -Inf
    assertEquals("toHalf(..)", (short)0xC000, toHalf(-2.0f                  )); // -2.0
    assertEquals("toHalf(..)", (short)0xBC00, toHalf(-1.0f                  )); // -1.0
    assertEquals("toHalf(..)", (short)0x8400, toHalf(-6.1035156E-5f         )); // Smallest Negative Normalized
    assertEquals("toHalf(..)", (short)0x83FF, toHalf(-6.0975550E-5f         )); // Largest  Negative Denorm
    assertEquals("toHalf(..)", (short)0x8001, toHalf(-5.9604645E-8f         )); // Smallest Negative Denorm
    assertEquals("toHalf(..)", (short)0x8000, toHalf(-0.0f                  )); // -0.0
    assertEquals("toHalf(..)", (short)0x0000, toHalf(+0.0f                  )); // +0.0
    assertEquals("toHalf(..)", (short)0x0001, toHalf(+5.9604645E-8f         )); // Smallest Positive Denorm
    assertEquals("toHalf(..)", (short)0x03FF, toHalf(+6.0975550E-5f         )); // Largest  Positive Denorm
    assertEquals("toHalf(..)", (short)0x0400, toHalf(+6.1035156E-5f         )); // Smallest Positive Normalized
    assertEquals("toHalf(..)", (short)0x3C00, toHalf(+1.0f                  )); // +1.0
    assertEquals("toHalf(..)", (short)0x4000, toHalf(+2.0f                  )); // +2.0
    assertEquals("toHalf(..)", (short)0x7C00, toHalf(Float.POSITIVE_INFINITY)); // +Inf

    assertEquals("toHalf(..)", (short)0xB555, toHalf(-0.33325195f           )); // -1/3
    assertEquals("toHalf(..)", (short)0x3555, toHalf(+0.33325195f           )); // +1/3

    // Additional tests for NaN
    //  The algorithm provided by Jeroen van der Zijp suffers from a known issue
    //  where some NaN values can become Inf. As such we carefully check to make
    //  sure all NaN values are converted properly in our modified algorithm.
    for (long bits = 0x7F800001; bits <= 0x7FFFFFFF; bits++) {
      // This is ~8million checks, but they execute in <0.1sec so do all of them...
      float f = Float.intBitsToFloat((int)bits);
      short h = toHalf(f);
      assertEquals("fromHalf(..)", true, (h >= (short)0x7C01)   // 0x7C01 = NaN ("Smallest"   Value)
                                      && (h <= (short)0x7FFF)); // 0x7FFF = NaN ("Largest"    Value)
    }
  }

  public void testFromDouble64 () {
    for (int radixPoint = 1; radixPoint <= 52; radixPoint++) {
      long max = (Long.MAX_VALUE >> radixPoint);
      long min = (Long.MIN_VALUE >> radixPoint);

      // Integer values
      for (int val = -128; val <= 128; val++) {
        long bits;

        if (val > max) {
          bits = 0x7FFFFFFFFFFFFFFFL; // over range
        }
        else if (val < min) {
          bits = 0x8000000000000000L; // below range
        }
        else {
          bits = (((long)val) << radixPoint);
          assertEquals("toLong16(..)",   (long  )val, toLong64(  radixPoint, bits));
          assertEquals("toInt16(..)",            val, toInt64(   radixPoint, bits));
          assertEquals("toFloat16(..)",  (float )val, toFloat64( radixPoint, bits));
          assertEquals("toDouble16(..)", (double)val, toDouble64(radixPoint, bits));
        }

        assertEquals("fromLong16(..)",   bits, fromLong64(  radixPoint, (long  )val));
        assertEquals("fromInt16(..)",    bits, fromInt64(   radixPoint,         val));
        assertEquals("fromFloat16(..)",  bits, fromFloat64( radixPoint, (float )val));
        assertEquals("fromDouble16(..)", bits, fromDouble64(radixPoint, (double)val));
      }

      // Float values
      for (int i = 0; i <= 2000; i++) {
        double val = -1000 + (i * 0.5);
        long   bits;

        if (val > max) {
          bits = 0x7FFFFFFFFFFFFFFFL; // over range
        }
        else if (val < min) {
          bits = 0x8000000000000000L; // below range
        }
        else {
          bits = (((long)(val*2)) << (radixPoint-1));
          assertEquals("toLong64(..)",   (long )val, toLong64(  radixPoint, bits));
          assertEquals("toInt64(..)",    (int  )val, toInt64(   radixPoint, bits));
          assertEquals("toFloat64(..)",  (float)val, toFloat64( radixPoint, bits));
          assertEquals("toDouble64(..)",        val, toDouble64(radixPoint, bits));
        }

        assertEquals("fromFloat64(..)",  bits, fromFloat64( radixPoint, (float)val));
        assertEquals("fromDouble64(..)", bits, fromDouble64(radixPoint,        val));
      }
    }
  }

  public void testFromDouble32 () {
    for (int radixPoint = 1; radixPoint <= 28; radixPoint++) {
      int max = (Integer.MAX_VALUE >> radixPoint);
      int min = (Integer.MIN_VALUE >> radixPoint);

      // Integer values
      for (int val = -128; val <= 128; val++) {
        int bits;

        if (val > max) {
          bits = 0x7FFFFFFF; // over range
        }
        else if (val < min) {
          bits = 0x80000000; // below range
        }
        else {
          bits = (val << radixPoint);
          assertEquals("toLong32(..)",   (long  )val, toLong32(  radixPoint, bits));
          assertEquals("toInt32(..)",            val, toInt32(   radixPoint, bits));
          assertEquals("toFloat32(..)",  (float )val, toFloat32( radixPoint, bits));
          assertEquals("toDouble32(..)", (double)val, toDouble32(radixPoint, bits));
        }

        assertEquals("fromLong32(..)",   bits, fromLong32(  radixPoint, (long  )val));
        assertEquals("fromInt32(..)",    bits, fromInt32(   radixPoint,         val));
        assertEquals("fromFloat32(..)",  bits, fromFloat32( radixPoint, (float )val));
        assertEquals("fromDouble32(..)", bits, fromDouble32(radixPoint, (double)val));
      }

      // Float values
      for (int i = 0; i <= 2000; i++) {
        double val = -1000 + (i * 0.5);
        int    bits;

        if (val > max) {
          bits = 0x7FFFFFFF; // over range
        }
        else if (val < min) {
          bits = 0x80000000; // below range
        }
        else {
          bits = (((int)(val*2)) << (radixPoint-1));
          assertEquals("toLong32(..)",   (long )val, toLong32(  radixPoint, bits));
          assertEquals("toInt32(..)",    (int  )val, toInt32(   radixPoint, bits));
          assertEquals("toFloat32(..)",  (float)val, toFloat32( radixPoint, bits));
          assertEquals("toDouble32(..)",        val, toDouble32(radixPoint, bits));
        }

        assertEquals("fromFloat32(..)",  bits, fromFloat32( radixPoint, (float)val));
        assertEquals("fromDouble32(..)", bits, fromDouble32(radixPoint,        val));
      }
    }
  }

  public void testFromDouble16 () {
    for (int radixPoint = 1; radixPoint <= 12; radixPoint++) {
      int max = (Short.MAX_VALUE >> radixPoint);
      int min = (Short.MIN_VALUE >> radixPoint);

      // Integer values
      for (int val = -128; val <= 128; val++) {
        short bits;

        if (val > max) {
          bits = (short)0x7FFF; // over range
        }
        else if (val < min) {
          bits = (short)0x8000; // below range
        }
        else {
          bits = (short)(val << radixPoint);
          assertEquals("toLong16(..)",   (long  )val, toLong16(  radixPoint, bits));
          assertEquals("toInt16(..)",            val, toInt16(   radixPoint, bits));
          assertEquals("toFloat16(..)",  (float )val, toFloat16( radixPoint, bits));
          assertEquals("toDouble16(..)", (double)val, toDouble16(radixPoint, bits));
        }

        assertEquals("fromLong16(..)",   bits, fromLong16(  radixPoint, (long  )val));
        assertEquals("fromInt16(..)",    bits, fromInt16(   radixPoint,         val));
        assertEquals("fromFloat16(..)",  bits, fromFloat16( radixPoint, (float )val));
        assertEquals("fromDouble16(..)", bits, fromDouble16(radixPoint, (double)val));
      }

      // Float values
      for (int i = 0; i <= 2000; i++) {
        double val = -1000 + (i * 0.5);
        short bits;

        if (val > max) {
          bits = (short)0x7FFF; // over range
        }
        else if (val < min) {
          bits = (short)0x8000; // below range
        }
        else {
          bits = (short)(((int)(val*2)) << (radixPoint-1));
          assertEquals("toLong16(..)",   (long )val, toLong16(  radixPoint, bits));
          assertEquals("toInt16(..)",    (int  )val, toInt16(   radixPoint, bits));
          assertEquals("toFloat16(..)",  (float)val, toFloat16( radixPoint, bits));
          assertEquals("toDouble16(..)",        val, toDouble16(radixPoint, bits));
        }

        assertEquals("fromFloat16(..)",  bits, fromFloat16( radixPoint, (float)val));
        assertEquals("fromDouble16(..)", bits, fromDouble16(radixPoint,        val));
      }
    }
  }

  /** Trivial unpackBits(..) function that is easier to demonstrate accurate.
   *  @param buf       The buffer to read from.
   *  @param bitOffset The bit offset into the buffer to start at.
   *  @param numBits   The number of bits to read in.
   */
  private static long getBits (byte[] buf, int bitOffset, int numBits) {
    long val = 0;
    for (int i = 0; i < numBits; i++,bitOffset++) {
      int off = bitOffset / 8;
      int bit = bitOffset % 8;

      switch (bit) {
        case 0: val = (val << 1) | ((buf[off] >> 7) & 0x1); break;
        case 1: val = (val << 1) | ((buf[off] >> 6) & 0x1); break;
        case 2: val = (val << 1) | ((buf[off] >> 5) & 0x1); break;
        case 3: val = (val << 1) | ((buf[off] >> 4) & 0x1); break;
        case 4: val = (val << 1) | ((buf[off] >> 3) & 0x1); break;
        case 5: val = (val << 1) | ((buf[off] >> 2) & 0x1); break;
        case 6: val = (val << 1) | ((buf[off] >> 1) & 0x1); break;
        case 7: val = (val << 1) | ((buf[off]     ) & 0x1); break;
      }
    }
    return val;
  }

  public void testUnpackBits64 () {
    byte[] buf = { (byte)0x01, (byte)0x23, (byte)0x45, (byte)0x67,
                   (byte)0x89, (byte)0xAB, (byte)0xCD, (byte)0xEF,
                   (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33,
                   (byte)0x44, (byte)0x55, (byte)0x66, (byte)0x77,
                   (byte)0x88, (byte)0x99, (byte)0xAA, (byte)0xBB,
                   (byte)0xCC, (byte)0xDD, (byte)0xEE, (byte)0xFF };

    int totalBits = buf.length * 8;

    for (int bitOffset = 0; bitOffset < totalBits; bitOffset+=5) {
      for (int numBits = 1; (numBits <= 64) && (numBits < (totalBits - bitOffset)); numBits++) {
        long exp64 = getBits(buf, bitOffset, numBits);
        long act64 = VRTMath.unpackBits64(buf, bitOffset, numBits);

        assertEquals("unpackBits64(..)", exp64, act64);

        if (numBits <= 32) {
          int exp32 = (int)exp64;
          int act32 = VRTMath.unpackBits32(buf, bitOffset, numBits);
          assertEquals("unpackBits32(..)", exp32, act32);
        }
      }
    }
  }

  public void testPackBits64 () {
    long[] bits = { 0x0000000000000000L, 0x0123456789ABCDEFL,
                    0x0011223344556677L, 0x8899AABBCCDDEEFFL };
    int totalBits = bits.length * 64;

    for (int bitOffset = 0; bitOffset < totalBits; bitOffset+=5) {
      for (int numBits = 1; (numBits <= 64) && (numBits < (totalBits - bitOffset)); numBits++) {
        for (long value : bits) {
          long   mask  = (numBits == 64)? -1 : ~(-1L << numBits);
          long   exp64 = value & mask;

          byte[] buf64 = new byte[totalBits/8];
          byte[] buf32 = new byte[totalBits/8];

          VRTMath.packBits64(buf64, bitOffset, numBits, value);
          assertEquals("packBits64(..)", exp64, VRTMath.unpackBits64(buf64, bitOffset, numBits));

          if (numBits <= 32) {
            VRTMath.packBits32(buf32, bitOffset, numBits, (int)value);
            assertArrayEquals("packBits32(..)", buf64, buf32);
          }
        }
      }
    }
  }

  public void testPackBytes () {
    byte[] data = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };
    byte[] exp  = { 0x0A, 0x0B, 0x01, 0x02, 0x03, 0x04, 0x0C, 0x0D };
    byte[] act  = { 0x0A, 0x0B, 0x11, 0x22, 0x33, 0x44, 0x0C, 0x0D };

    packBytes(act, 2, data, 1, 4);
    assertBufEquals("packBytes(..)", exp, act);
  }

  public void testUnpackBytes () {
    byte[] data = { 0x0A, 0x0B, 0x01, 0x02, 0x03, 0x04, 0x0C, 0x0D };
    byte[] exp  = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };
    byte[] act  = { 0x00, 0x11, 0x22, 0x33, 0x44, 0x05, 0x06, 0x07 };

    unpackBytes(data, 2, act, 1, 4);
    assertBufEquals("unpackBytes(..)", exp, act);
  }

  public void testUnpackByte () {
    byte[] data = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };

    for (int i = 0; i < 8; i++) {
      assertEquals("unpackByte(..)", data[i], unpackByte(data, i));
    }
  }

  public void testPackByte () {
    byte[] data = new byte[8];

    for (int i = 0; i < 8; i++) {
      packByte(data, i, (byte)i);
    }
    for (int i = 0; i < 8; i++) {
      assertEquals("packByte(..)", (byte)i, data[i]);
    }
  }

  public void testUnpackShort () {
    byte[] data = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };

    assertEquals("unpackShort(..)", (short)0x0001, unpackShort(data, 0));
    assertEquals("unpackShort(..)", (short)0x0001, unpackShort(data, 0, BIG_ENDIAN));
    assertEquals("unpackShort(..)", (short)0x0100, unpackShort(data, 0, LITTLE_ENDIAN));

    assertEquals("unpackShort(..)", (short)0x0203, unpackShort(data, 2));
    assertEquals("unpackShort(..)", (short)0x0203, unpackShort(data, 2, BIG_ENDIAN));
    assertEquals("unpackShort(..)", (short)0x0302, unpackShort(data, 2, LITTLE_ENDIAN));

    assertEquals("unpackShort(..)", (short)0x0405, unpackShort(data, 4));
    assertEquals("unpackShort(..)", (short)0x0405, unpackShort(data, 4, BIG_ENDIAN));
    assertEquals("unpackShort(..)", (short)0x0504, unpackShort(data, 4, LITTLE_ENDIAN));

    assertEquals("unpackShort(..)", (short)0x0607, unpackShort(data, 6));
    assertEquals("unpackShort(..)", (short)0x0607, unpackShort(data, 6, BIG_ENDIAN));
    assertEquals("unpackShort(..)", (short)0x0706, unpackShort(data, 6, LITTLE_ENDIAN));
  }

  public void testPackShort () {
    byte[] data  = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };
    byte[] data1 = new byte[8];
    byte[] data2 = new byte[8];
    byte[] data3 = new byte[8];

    packShort(data1, 0, (short)0x0001);
    packShort(data2, 0, (short)0x0001, BIG_ENDIAN);
    packShort(data3, 0, (short)0x0100, LITTLE_ENDIAN);

    packShort(data1, 2, (short)0x0203);
    packShort(data2, 2, (short)0x0203, BIG_ENDIAN);
    packShort(data3, 2, (short)0x0302, LITTLE_ENDIAN);

    packShort(data1, 4, (short)0x0405);
    packShort(data2, 4, (short)0x0405, BIG_ENDIAN);
    packShort(data3, 4, (short)0x0504, LITTLE_ENDIAN);

    packShort(data1, 6, (short)0x0607);
    packShort(data2, 6, (short)0x0607, BIG_ENDIAN);
    packShort(data3, 6, (short)0x0706, LITTLE_ENDIAN);

    assertBufEquals("packShort(..)", data, data1);
    assertBufEquals("packShort(..)", data, data2);
    assertBufEquals("packShort(..)", data, data3);
  }

  public void testUnpackInt24 () {
    byte[] data = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };

    assertEquals("unpackInt24(..)", 0x000102, unpackInt24(data, 0));
    assertEquals("unpackInt24(..)", 0x000102, unpackInt24(data, 0, BIG_ENDIAN));
    assertEquals("unpackInt24(..)", 0x020100, unpackInt24(data, 0, LITTLE_ENDIAN));

    assertEquals("unpackInt24(..)", 0x030405, unpackInt24(data, 3));
    assertEquals("unpackInt24(..)", 0x030405, unpackInt24(data, 3, BIG_ENDIAN));
    assertEquals("unpackInt24(..)", 0x050403, unpackInt24(data, 3, LITTLE_ENDIAN));

    assertEquals("unpackInt24(..)", 0x060708, unpackInt24(data, 6));
    assertEquals("unpackInt24(..)", 0x060708, unpackInt24(data, 6, BIG_ENDIAN));
    assertEquals("unpackInt24(..)", 0x080706, unpackInt24(data, 6, LITTLE_ENDIAN));
  }

  public void testPackInt24 () {
    byte[] data  = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };
    byte[] data1 = new byte[9];
    byte[] data2 = new byte[9];
    byte[] data3 = new byte[9];

    packInt24(data1, 0, 0x000102);
    packInt24(data2, 0, 0x000102, BIG_ENDIAN);
    packInt24(data3, 0, 0x020100, LITTLE_ENDIAN);

    packInt24(data1, 3, 0x030405);
    packInt24(data2, 3, 0x030405, BIG_ENDIAN);
    packInt24(data3, 3, 0x050403, LITTLE_ENDIAN);

    packInt24(data1, 6, 0x060708);
    packInt24(data2, 6, 0x060708, BIG_ENDIAN);
    packInt24(data3, 6, 0x080706, LITTLE_ENDIAN);

    assertBufEquals("packInt24(..)", data, data1);
    assertBufEquals("packInt24(..)", data, data2);
    assertBufEquals("packInt24(..)", data, data3);
  }

  public void testUnpackInt () {
    byte[] data = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };

    assertEquals("unpackInt(..)", 0x00010203, unpackInt(data, 0));
    assertEquals("unpackInt(..)", 0x00010203, unpackInt(data, 0, BIG_ENDIAN));
    assertEquals("unpackInt(..)", 0x03020100, unpackInt(data, 0, LITTLE_ENDIAN));

    assertEquals("unpackInt(..)", 0x04050607, unpackInt(data, 4));
    assertEquals("unpackInt(..)", 0x04050607, unpackInt(data, 4, BIG_ENDIAN));
    assertEquals("unpackInt(..)", 0x07060504, unpackInt(data, 4, LITTLE_ENDIAN));
  }

  public void testPackInt () {
    byte[] data  = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };
    byte[] data1 = new byte[8];
    byte[] data2 = new byte[8];
    byte[] data3 = new byte[8];

    packInt(data1, 0, 0x00010203);
    packInt(data2, 0, 0x00010203, BIG_ENDIAN);
    packInt(data3, 0, 0x03020100, LITTLE_ENDIAN);

    packInt(data1, 4, 0x04050607);
    packInt(data2, 4, 0x04050607, BIG_ENDIAN);
    packInt(data3, 4, 0x07060504, LITTLE_ENDIAN);

    assertBufEquals("packInt(..)", data, data1);
    assertBufEquals("packInt(..)", data, data2);
    assertBufEquals("packInt(..)", data, data3);
  }

  public void testUnpackLong () {
    byte[] data = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                    0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };

    assertEquals("unpackLong(..)", 0x0001020304050607L, unpackLong(data, 0));
    assertEquals("unpackLong(..)", 0x0001020304050607L, unpackLong(data, 0, BIG_ENDIAN));
    assertEquals("unpackLong(..)", 0x0706050403020100L, unpackLong(data, 0, LITTLE_ENDIAN));

    assertEquals("unpackLong(..)", 0x08090A0B0C0D0E0FL, unpackLong(data, 8));
    assertEquals("unpackLong(..)", 0x08090A0B0C0D0E0FL, unpackLong(data, 8, BIG_ENDIAN));
    assertEquals("unpackLong(..)", 0x0F0E0D0C0B0A0908L, unpackLong(data, 8, LITTLE_ENDIAN));
  }

  public void testPackLong () {
    byte[] data  = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                     0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };
    byte[] data1 = new byte[16];
    byte[] data2 = new byte[16];
    byte[] data3 = new byte[16];

    packLong(data1, 0, 0x0001020304050607L);
    packLong(data2, 0, 0x0001020304050607L, BIG_ENDIAN);
    packLong(data3, 0, 0x0706050403020100L, LITTLE_ENDIAN);

    packLong(data1, 8, 0x08090A0B0C0D0E0FL);
    packLong(data2, 8, 0x08090A0B0C0D0E0FL, BIG_ENDIAN);
    packLong(data3, 8, 0x0F0E0D0C0B0A0908L, LITTLE_ENDIAN);

    assertBufEquals("packLong(..)", data, data1);
    assertBufEquals("packLong(..)", data, data2);
    assertBufEquals("packLong(..)", data, data3);
  }

  public void testUnpackFloat () {
    byte[] data = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };

    assertEquals("unpackFloat(..)", intBitsToFloat(0x00010203), unpackFloat(data, 0));
    assertEquals("unpackFloat(..)", intBitsToFloat(0x00010203), unpackFloat(data, 0, BIG_ENDIAN));
    assertEquals("unpackFloat(..)", intBitsToFloat(0x03020100), unpackFloat(data, 0, LITTLE_ENDIAN));

    assertEquals("unpackFloat(..)", intBitsToFloat(0x04050607), unpackFloat(data, 4));
    assertEquals("unpackFloat(..)", intBitsToFloat(0x04050607), unpackFloat(data, 4, BIG_ENDIAN));
    assertEquals("unpackFloat(..)", intBitsToFloat(0x07060504), unpackFloat(data, 4, LITTLE_ENDIAN));
  }

  public void testPackFloat () {
    byte[] data  = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };
    byte[] data1 = new byte[8];
    byte[] data2 = new byte[8];
    byte[] data3 = new byte[8];

    packFloat(data1, 0, intBitsToFloat(0x00010203));
    packFloat(data2, 0, intBitsToFloat(0x00010203), BIG_ENDIAN);
    packFloat(data3, 0, intBitsToFloat(0x03020100), LITTLE_ENDIAN);

    packFloat(data1, 4, intBitsToFloat(0x04050607));
    packFloat(data2, 4, intBitsToFloat(0x04050607), BIG_ENDIAN);
    packFloat(data3, 4, intBitsToFloat(0x07060504), LITTLE_ENDIAN);

    assertBufEquals("packFloat(..)", data, data1);
    assertBufEquals("packFloat(..)", data, data2);
    assertBufEquals("packFloat(..)", data, data3);
  }

  public void testUnpackDouble () {
    byte[] data = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                    0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };

    assertEquals("unpackDouble(..)", longBitsToDouble(0x0001020304050607L), unpackDouble(data, 0));
    assertEquals("unpackDouble(..)", longBitsToDouble(0x0001020304050607L), unpackDouble(data, 0, BIG_ENDIAN));
    assertEquals("unpackDouble(..)", longBitsToDouble(0x0706050403020100L), unpackDouble(data, 0, LITTLE_ENDIAN));

    assertEquals("unpackDouble(..)", longBitsToDouble(0x08090A0B0C0D0E0FL), unpackDouble(data, 8));
    assertEquals("unpackDouble(..)", longBitsToDouble(0x08090A0B0C0D0E0FL), unpackDouble(data, 8, BIG_ENDIAN));
    assertEquals("unpackDouble(..)", longBitsToDouble(0x0F0E0D0C0B0A0908L), unpackDouble(data, 8, LITTLE_ENDIAN));
  }

  public void testPackDouble () {
    byte[] data  = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                     0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };
    byte[] data1 = new byte[16];
    byte[] data2 = new byte[16];
    byte[] data3 = new byte[16];

    packDouble(data1, 0, longBitsToDouble(0x0001020304050607L));
    packDouble(data2, 0, longBitsToDouble(0x0001020304050607L), BIG_ENDIAN);
    packDouble(data3, 0, longBitsToDouble(0x0706050403020100L), LITTLE_ENDIAN);

    packDouble(data1, 8, longBitsToDouble(0x08090A0B0C0D0E0FL));
    packDouble(data2, 8, longBitsToDouble(0x08090A0B0C0D0E0FL), BIG_ENDIAN);
    packDouble(data3, 8, longBitsToDouble(0x0F0E0D0C0B0A0908L), LITTLE_ENDIAN);

    assertBufEquals("packDouble(..)", data, data1);
    assertBufEquals("packDouble(..)", data, data2);
    assertBufEquals("packDouble(..)", data, data3);
  }

  public void testUnpackBoolean () {
    byte[] data = { -2, -1, 0, +1, +2 };

    if (VRTConfig.STRICT) {
      try {
        unpackBoolean(data, 0);
        assertEquals("unpackBoolean(..) for -2 causes exception", true, false);
      }
      catch (Exception e) {
        assertEquals("unpackBoolean(..) for -2 causes exception", true, true);
      }
      assertEquals("unpackBoolean(..)", false, unpackBoolean(data, 1));
      assertEquals("unpackBoolean(..)", false, unpackBoolean(data, 2));
      assertEquals("unpackBoolean(..)", true,  unpackBoolean(data, 3));
      try {
        unpackBoolean(data, 4);
        assertEquals("unpackBoolean(..) for +2 causes exception", true, false);
      }
      catch (Exception e) {
        assertEquals("unpackBoolean(..) for +2 causes exception", true, true);
      }
    }
    else {
      assertEquals("unpackBoolean(..)", false, unpackBoolean(data, 0));
      assertEquals("unpackBoolean(..)", false, unpackBoolean(data, 1));
      assertEquals("unpackBoolean(..)", false, unpackBoolean(data, 2));
      assertEquals("unpackBoolean(..)", true,  unpackBoolean(data, 3));
      assertEquals("unpackBoolean(..)", true,  unpackBoolean(data, 4));
    }
  }

  public void testPackBoolean () {
    byte[] exp  = { -1, +1, -1, +1 };
    byte[] act  = new byte[4];

    packBoolean(act, 0, false);
    packBoolean(act, 1, true);
    packBoolean(act, 2, false);
    packBoolean(act, 3, true);

    assertBufEquals("packBoolean(..)", exp, act);
  }

  public void testUnpackBoolNull () {
    byte[] data = { -2, -1, 0, +1, +2 };

    if (VRTConfig.STRICT) {
      try {
        unpackBoolNull(data, 0);
        assertEquals("unpackBoolNull(..) for -2 causes exception", true, false);
      }
      catch (Exception e) {
        assertEquals("unpackBoolNull(..) for -2 causes exception", true, true);
      }
      assertEquals("unpackBoolNull(..)", false, unpackBoolNull(data, 1));
      assertEquals("unpackBoolNull(..)", null,  unpackBoolNull(data, 2));
      assertEquals("unpackBoolNull(..)", true,  unpackBoolNull(data, 3));
      try {
        unpackBoolNull(data, 4);
        assertEquals("unpackBoolNull(..) for +2 causes exception", true, false);
      }
      catch (Exception e) {
        assertEquals("unpackBoolNull(..) for +2 causes exception", true, true);
      }
    }
    else {
      assertEquals("unpackBoolNull(..)", false, unpackBoolNull(data, 0));
      assertEquals("unpackBoolNull(..)", false, unpackBoolNull(data, 1));
      assertEquals("unpackBoolNull(..)", null,  unpackBoolNull(data, 2));
      assertEquals("unpackBoolNull(..)", true,  unpackBoolNull(data, 3));
      assertEquals("unpackBoolNull(..)", true,  unpackBoolNull(data, 4));
    }
  }

  public void testPackBoolNull () {
    byte[] exp  = { -1, 0, +1 };
    byte[] act  = new byte[3];

    packBoolNull(act, 0, false);
    packBoolNull(act, 1, null);
    packBoolNull(act, 2, true);

    assertBufEquals("packBoolNull(..)", exp, act);
  }

  private void testText (CharSequence txt, byte[] ascii, byte[] utf8) {
    String _txtASCII = new String(ascii);
    String _txtUTF8  = txt.toString();

    for (int offset = 0; offset <= 4; offset++) {
      byte[] expASCII = new byte[ascii.length + 8];
      byte[] actASCII = new byte[ascii.length + 8];
      byte[] expUTF8  = new byte[utf8.length  + 8];
      byte[] actUTF8  = new byte[utf8.length  + 8];

      Arrays.fill(expASCII, 0, offset,          (byte)-1);
      Arrays.fill(actASCII, 0, actASCII.length, (byte)-1);
      Arrays.fill(expUTF8,  0, offset,          (byte)-1);
      Arrays.fill(actUTF8,  0, actUTF8.length,  (byte)-1);

      System.arraycopy(ascii, 0, expASCII, offset, ascii.length);
      System.arraycopy(utf8,  0, expUTF8,  offset, utf8.length);

      int lenASCII = packAscii(actASCII, offset, txt, actASCII.length-offset);
      int lenUTF8  = packUTF8( actUTF8,  offset, txt, actUTF8.length-offset);

      CharSequence txtASCII = unpackAscii(actASCII, offset, actASCII.length-offset);
      CharSequence txtUTF8  = unpackUTF8( actUTF8,  offset, actUTF8.length-offset);

      assertEquals(   "packAscii(..)  for '"+txt+"'", ascii.length, lenASCII);
      assertEquals(   "packUTF8(..)   for '"+txt+"'", utf8.length,  lenUTF8 );
      assertEquals(   "lengthUTF8(..) for '"+txt+"'", utf8.length,  lengthUTF8(txt));
      assertBufEquals("packAscii(..)  for '"+txt+"'", expASCII,     actASCII);
      assertBufEquals("packUTF8(..)   for '"+txt+"'", actUTF8,      actUTF8 );
      assertEquals( "unpackAscii(..)  for '"+txt+"'", _txtASCII,    txtASCII.toString());
      assertEquals( "unpackUTF8(..)   for '"+txt+"'", _txtUTF8,     txtUTF8.toString());
    }
  }

  public void testPackAscii () {
    // ASCII STRINGS:
    String helloWorld = "Hello World!";
    byte[] bytes      = helloWorld.getBytes();
    testText(helloWorld,                                     bytes, bytes);
    testText(AsciiCharSequence.fromCharSequence(helloWorld), bytes, bytes);

    // UNICODE STRINGS:
    //  U+0000B2 = SUPERSCRIPT_TWO                     (2-octet)
    //  U+0003BC = GREEK SMALL LETTER MU               (2-octet)
    //  U+002212 = MINUS SIGN (Mathematical Operators) (3-octet)
    //  U+01D49E = MATHEMATICAL SCRIPT CAPITAL C       (4-octet)
    byte[] ascii_1 = { (byte)'m', (byte)'/', (byte)'s', (byte)'?' };
    byte[] utf8_1  = { (byte)'m',  (byte)'/',  (byte)'s',  (byte)0xC2, (byte)0xB2 };
    testText("m/s\u00B2", ascii_1, utf8_1);  // m/s^2

    byte[] ascii_2 = { (byte)'?', (byte)'s', (byte)'e', (byte)'c' };
    byte[] utf8_2  = { (byte)0xCE, (byte)0xBC, (byte)'s',  (byte)'e',  (byte)'c' };
    testText("\u03BCsec", ascii_2, utf8_2);  // usec

    byte[] ascii_3 = { (byte)'?', (byte)'4', (byte)'2' };
    byte[] utf8_3  = { (byte)0xE2, (byte)0x88, (byte)0x92, (byte)'4',  (byte)'2' };
    testText("\u221242", ascii_3, utf8_3);  // -42

    String txt_4   = (new StringBuilder()).appendCodePoint(0x01D49E).toString();
    byte[] ascii_4 = { (byte)'?' };
    byte[] utf8_4  = { (byte)0xF0, (byte)0x9D, (byte)0x92, (byte)0x9E };
    testText(txt_4, ascii_4, utf8_4);
  }

  public void testUnpackUUID () {
    UUID   uuid1 = UUID.fromString("091e6a58-5379-4686-bd2e-60427bdd6c5e");
    UUID   uuid2 = UUID.fromString("61e09d02-6d6b-4634-9e73-f2c8b5f813e0");
    byte[] exp1  = { 0, 0, 0, 0,
                     (byte)0x09, (byte)0x1e, (byte)0x6a, (byte)0x58,
                     (byte)0x53, (byte)0x79, (byte)0x46, (byte)0x86,
                     (byte)0xbd, (byte)0x2e, (byte)0x60, (byte)0x42,
                     (byte)0x7b, (byte)0xdd, (byte)0x6c, (byte)0x5e,
                     0, 0, 0, 0 };
    byte[] exp2  = { (byte)0x61, (byte)0xe0, (byte)0x9d, (byte)0x02,
                     (byte)0x6d, (byte)0x6b, (byte)0x46, (byte)0x34,
                     (byte)0x9e, (byte)0x73, (byte)0xf2, (byte)0xc8,
                     (byte)0xb5, (byte)0xf8, (byte)0x13, (byte)0xe0 };
    byte[] act1  = new byte[24];
    byte[] act2  = new byte[16];

    assertEquals("unpackUUID(..)", uuid1, unpackUUID(exp1, 4));
    assertEquals("unpackUUID(..)", uuid2, unpackUUID(exp2, 0));

    packUUID(act1, 4, uuid1);
    packUUID(act2, 0, uuid2);

    assertBufEquals("packUUID(..)", exp1, act1);
    assertBufEquals("packUUID(..)", exp2, act2);
  }

  public void testUnpackTimeStamp () {
    TimeStamp[] timeStamps = {
      TimeStamp.Y2K_GPS,
      TimeStamp.parseTime("2013-02-14T01:02:03.456", TimeStamp.UTC_EPOCH),
      TimeStamp.parseTime("2013-02-14T01:02:03.456", TimeStamp.GPS_EPOCH)
    };

    for (TimeStamp ts : timeStamps) {
      byte[] expG = new byte[20];
      byte[] expU = new byte[20];
      byte[] actG = new byte[20];
      byte[] actU = new byte[20];

      packInt(expG, 2, ts.getSecondsGPS());
      packInt(expU, 2, ts.getSecondsUTC());
      packLong(expG, 6, ts.getPicoSeconds());
      packLong(expU, 6, ts.getPicoSeconds());

      assertEquals("unpackTimeStamp(..)", ts.toGPS(), unpackTimeStamp(expG, 2, TimeStamp.GPS_EPOCH));
      assertEquals("unpackTimeStamp(..)", ts.toUTC(), unpackTimeStamp(expU, 2, TimeStamp.UTC_EPOCH));

      packTimeStamp(actG, 2, ts, TimeStamp.GPS_EPOCH);
      packTimeStamp(actU, 2, ts, TimeStamp.UTC_EPOCH);

      assertBufEquals("packTimeStamp(..)", expG, actG);
      assertBufEquals("packTimeStamp(..)", expU, actU);
    }
  }

  public void testUnpackInetAddr () {
    try {
      String[] addresses = {
        "0.0.0.0",
        "::FFFF:0.0.0.0",
        "::0",
        "127.0.0.1",
        "::FFFF:127.0.0.1",
        "10.10.255.1",
        "::FFFF:10.10.255.1",
        "::FFFF:0A0A:FF01"
      };

      for (String addrStr : addresses) {
        InetAddress inetAddr = InetAddress.getByName(addrStr);
        byte[]      addr     = inetAddr.getAddress();

        if (addr.length == 4) {
          // Convert IPv4 to IPv6
          byte[] ipv6 = new byte[16];
          ipv6[10] = (byte)0xFF;
          ipv6[11] = (byte)0xFF;
          System.arraycopy(addr, 0, ipv6, 12, 4);
          addr = ipv6;
        }

        byte[] exp = new byte[20];
        byte[] act = new byte[20];
        System.arraycopy(addr, 0, exp, 2, 16);

        assertEquals("unpackInetAddr(..)", inetAddr, unpackInetAddr(exp, 2));

        packInetAddr(act, 2, inetAddr);
        assertBufEquals("packInetAddr(..)", exp, act);
      }
    }
    catch (UnknownHostException e) {
      throw new RuntimeException("Error in host resolution", e);
    }
  }
}
