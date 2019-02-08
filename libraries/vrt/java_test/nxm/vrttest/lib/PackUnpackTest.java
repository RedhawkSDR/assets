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
import nxm.vrt.lib.PackUnpack;
import nxm.vrt.lib.VRTMath;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.lib.VRTPacket.DataItemFormat;
import nxm.vrt.lib.VRTPacket.PayloadFormat;
import nxm.vrt.lib.VRTPacket.RealComplexType;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static java.lang.Math.max;
import static nxm.vrt.lib.PackUnpack.packAsByte;
import static nxm.vrt.lib.PackUnpack.packAsDouble;
import static nxm.vrt.lib.PackUnpack.packAsFloat;
import static nxm.vrt.lib.PackUnpack.packAsInt;
import static nxm.vrt.lib.PackUnpack.packAsLong;
import static nxm.vrt.lib.PackUnpack.packAsShort;
import static nxm.vrt.lib.PackUnpack.unpackAsByte;
import static nxm.vrt.lib.PackUnpack.unpackAsDouble;
import static nxm.vrt.lib.PackUnpack.unpackAsFloat;
import static nxm.vrt.lib.PackUnpack.unpackAsInt;
import static nxm.vrt.lib.PackUnpack.unpackAsLong;
import static nxm.vrt.lib.PackUnpack.unpackAsShort;
import static nxm.vrt.lib.VRTConfig.TEST_QUICK;
import static nxm.vrttest.inc.TestRunner.assertArrayEquals;
import static nxm.vrttest.inc.TestUtilities.SPECIAL_DOUBLE_BITS;
import static nxm.vrttest.inc.TestUtilities.SPECIAL_DOUBLE_VALS;
import static nxm.vrttest.inc.TestUtilities.SPECIAL_FLOAT_BITS;
import static nxm.vrttest.inc.TestUtilities.SPECIAL_FLOAT_VALS;

/** Tests for {@link PackUnpack}. This has a HUGE number of iterations since simply
 *  due to the high multiplicity inherent in the payload formats:
 *  <pre>
 *    dSize    =  1 to 64
 *    eSize    =  0 to  7 (or null)
 *    cSize    =  0 to 15 (or null)
 *    fSize    =  N to 64
 *    format   =  Int/UInt/Float32/Double64
 *    packing  =  Link/Processing
 *    offset   =  N
 *    arrayFmt =  byte/short/int/long/float/double
 *  </pre>
 *  In the end there are around 48 million permutations that need to be tested
 *  (170 million if you include the VRT floating point formats). This tries to
 *  do at least some testing on all of them. The integer formats are the most
 *  tested as are the direct byte/short/int/long/float/double payloads. <br>
 *  <br>
 *  When the full suite of tests are included (VRT_TEST_QUICK=false) this test
 *  set takes a long time to run at around ~20 billion tests. By default only a
 *  small subset is run (VRT_TEST_QUICK=true) with around 0.3 billion tests.
 */
public class PackUnpackTest implements Testable {

  @Override
  public Class<?> getTestedClass () {
    return PackUnpack.class;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();
    tests.add(new TestSet("packAsByte(..)",     this, "testPack"));
    tests.add(new TestSet("packAsDouble(..)",   this, "+testPack"));
    tests.add(new TestSet("packAsFloat(..)",    this, "+testPack"));
    tests.add(new TestSet("packAsInt(..)",      this, "+testPack"));
    tests.add(new TestSet("packAsLong(..)",     this, "+testPack"));
    tests.add(new TestSet("packAsShort(..)",    this, "+testPack"));

    tests.add(new TestSet("unpackAsByte(..)",   this, "testUnpack"));
    tests.add(new TestSet("unpackAsDouble(..)", this, "+testUnpack"));
    tests.add(new TestSet("unpackAsFloat(..)",  this, "+testUnpack"));
    tests.add(new TestSet("unpackAsInt(..)",    this, "+testUnpack"));
    tests.add(new TestSet("unpackAsLong(..)",   this, "+testUnpack"));
    tests.add(new TestSet("unpackAsShort(..)",  this, "+testUnpack"));

    return tests;
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }

  @Override
  public String toString () {
    return "Tests for PackUnpack";
  }

  /** Gets bits from an array.
   *  @param buf       The buffer to read from.
   *  @param bitOffset The bit offset into the buffer to start at.
   *  @param numBits   The number of bits to read in.
   *  @param sign      Should the output value be sign-extended?
   */
  private long getBits (byte[] buf, int bitOffset, int numBits, boolean sign) {
    long bits = VRTMath.unpackBits64(buf, bitOffset, numBits);
    if (sign) { // sign-extend
      int shift = 64 - numBits;
      bits = ((bits << shift) >> shift);
    }
    return bits;
  }

  /** Creates an int array with a range of values. */
  private int[] range (int start, int end) {
    int[] vals = new int[end-start];

    for (int i = 0; i < vals.length; i++) {
      vals[i] = start + i;
    }
    return vals;
  }


  @SuppressWarnings("cast")
  public void testPack () {
    int[]   eSizeOptions = (!TEST_QUICK)? range(-1,8) : new int[] { -1, 0, 4, 7     };
    int[]   cSizeOptions = (!TEST_QUICK)? range(-1,8) : new int[] { -1, 0, 4, 7, 15 };
    int[]   dSizeOptions = (!TEST_QUICK)? range(1,65) : new int[] {  1,  4,  7,  8,
                                                                     9, 12, 15, 16,
                                                                    17, 24, 31, 32,
                                                                    33, 48, 63, 64 };

    // ==== INTEGER INPUT VALUES ===============================================
    byte[]   inByte    = new byte[  256  ];
    short[]  inShort   = new short[ 256*2];
    float[]  inFloat   = new float[ 256*3];
    int[]    inInt     = new int[   256*4];
    double[] inDouble  = new double[256*5];
    long[]   inLong    = new long[  256*8];
    int[]    inEvent   = new int[inLong.length];
    int[]    inChannel = new int[inLong.length];

    for (int j = 0; j < inLong.length;) {
      for (long i = 0x00; i <= 0xFF; i++,j++) {
        long bits = ((i ^ 0xFF) << 56)
                  | ((i ^ 0xFA) << 48)
                  | ((i ^ 0xF5) << 40)
                  | ((i ^ 0xAF) << 32)
                  | ((i ^ 0xAA) << 24)
                  | ((i ^ 0xA5) << 16)
                  | ((i ^ 0x55) <<  8)
                  | ((i       )      );
        if (j < inByte.length   ) inByte[j]    = (byte  )bits;
        if (j < inShort.length  ) inShort[j]   = (short )bits;
        if (j < inInt.length    ) inInt[j]     = (int   )bits;
        if (j < inLong.length   ) inLong[j]    = (long  )bits;
        if (j < inFloat.length  ) inFloat[j]   = (float )(bits &   0x00FFFFFF);
        if (j < inDouble.length ) inDouble[j]  = (double)(bits & 0xFFFFFFFFFFL);
        if (j < inEvent.length  ) inEvent[j]   = (int   )(bits &   0x0000007F); //  7-bit max
        if (j < inChannel.length) inChannel[j] = (int   )(bits &   0x00007FFF); // 15-bit max
      }
    }

    // ==== FLOAT/DOUBLE EXP VALUES ============================================
    int[]  fexp = new int[SPECIAL_FLOAT_BITS.length];
    long[] dexp = new long[SPECIAL_DOUBLE_BITS.length];

    for (int i = 0; i < fexp.length; i++) {
      fexp[i] = SPECIAL_FLOAT_BITS[i];
      dexp[i] = SPECIAL_DOUBLE_BITS[i];

      // Normalize the NaN values
      if ((fexp[i] >= 0x7F800001         ) && (fexp[i] <= 0x7FFFFFFF         )) fexp[i] = 0x7FC00000;
      if ((dexp[i] >= 0x7FF0000000000001L) && (dexp[i] <= 0x7FFFFFFFFFFFFFFFL)) dexp[i] = 0x7FF8000000000000L;
    }

    // ==== VRT INPUT VALUES ===================================================
    double[] inVRT = Arrays.copyOf(SPECIAL_DOUBLE_VALS, SPECIAL_DOUBLE_VALS.length+1024);

    double testVal = -2.0;
    for (int i = SPECIAL_DOUBLE_VALS.length; i < inVRT.length; i++) {
      inVRT[i] = testVal;
      testVal += 1.0 / 256.0;
    }

    // ==== TEST CASES =========================================================
    for (int offset = 0; offset <= 512; offset+=512) {
      for (int eSize : eSizeOptions) {
        int[] evt      = (eSize < 0)? null : inEvent;
        int[] expEvent = new int[inEvent.length];

        if (eSize > 0) {
          int mask = ~(-1 << eSize);
          for (int i = 0; i < expEvent.length; i++) {
            expEvent[i] = inEvent[i] & mask;
          }
        }

        for (int cSize : cSizeOptions) {
          int[] chan       = (cSize < 0)? null : inChannel;
          int[] expChannel = new int[inChannel.length];

          if (cSize > 0) {
            int mask = ~(-1 << cSize);
            for (int i = 0; i < expChannel.length; i++) {
              expChannel[i] = inChannel[i] & mask;
            }
          }

          // ---- INTEGER TESTS ------------------------------------------------
          for (int dSize : dSizeOptions) {
            int minFieldSize = max(0,eSize) + max(0,cSize) + dSize;

            for (int fSize = minFieldSize; fSize <= 64; fSize++) {
              if (TEST_QUICK && (fSize != dSize) && (fSize != minFieldSize)
                             && (Arrays.binarySearch(dSizeOptions, fSize) < 0)) {
                continue;
              }

              for (int sign = 0; sign <= 1; sign++) {
                for (int proc = 0; proc <= 1; proc++) {
                  DataItemFormat  format = (sign == 0)? DataItemFormat.UnsignedInt : DataItemFormat.SignedInt;
                  RealComplexType real   = RealComplexType.Real;
                  PayloadFormat   pf     = new PayloadFormat((proc!=0), real, format, false, max(0,eSize), max(0,cSize), fSize, dSize, 1, 1);
                  String          pfStr  = pf.toString();
                  int             maxLen = inLong.length * 64; // maximum possible buffer length

                  byte[] outByte   = new byte[offset+maxLen];
                  byte[] outShort  = new byte[offset+maxLen];
                  byte[] outInt    = new byte[offset+maxLen];
                  byte[] outLong   = new byte[offset+maxLen];
                  byte[] outFloat  = new byte[offset+maxLen];
                  byte[] outDouble = new byte[offset+maxLen];

                  packAsByte(  pf, outByte,   offset, inByte,   chan, evt, inByte.length  );
                  packAsShort( pf, outShort,  offset, inShort,  chan, evt, inShort.length );
                  packAsInt(   pf, outInt,    offset, inInt,    chan, evt, inInt.length   );
                  packAsLong(  pf, outLong,   offset, inLong,   chan, evt, inLong.length  );
                  packAsFloat( pf, outFloat,  offset, inFloat,  chan, evt, inFloat.length );
                  packAsDouble(pf, outDouble, offset, inDouble, chan, evt, inDouble.length);

                  long[] expByte   = new long[inByte.length  ];
                  long[] expShort  = new long[inShort.length ];
                  long[] expInt    = new long[inInt.length   ];
                  long[] expLong   = new long[inLong.length  ];
                  long[] expFloat  = new long[inFloat.length ];
                  long[] expDouble = new long[inDouble.length];

                  int  shift = 64 - dSize;
                  long mask  = (dSize == 64)? -1L : ~((-1L) << dSize);
                  for (int i = 0; i < expLong.length; i++) {
                    if (sign == 0) {
                      if (i < expByte.length  ) expByte[i]   = ((inLong[i] & mask &   0x000000FFL));
                      if (i < expShort.length ) expShort[i]  = ((inLong[i] & mask &   0x0000FFFFL));
                      if (i < expFloat.length ) expFloat[i]  = ((inLong[i] & mask &   0x00FFFFFFL));
                      if (i < expInt.length   ) expInt[i]    = ((inLong[i] & mask &   0xFFFFFFFFL));
                      if (i < expDouble.length) expDouble[i] = ((inLong[i] & mask & 0xFFFFFFFFFFL));
                      if (i < expLong.length  ) expLong[i]   = ((inLong[i] & mask                ));
                    }
                    else {
                      if (i < expByte.length  ) expByte[i]   = (((long)inByte[i]  ) << shift) >> shift;
                      if (i < expShort.length ) expShort[i]  = (((long)inShort[i] ) << shift) >> shift;
                      if (i < expInt.length   ) expInt[i]    = (((long)inInt[i]   ) << shift) >> shift;
                      if (i < expLong.length  ) expLong[i]   = (((long)inLong[i]  ) << shift) >> shift;
                      if (i < expFloat.length ) expFloat[i]  = (((inLong[i] & mask &   0x00FFFFFFL)) << shift) >> shift;
                      if (i < expDouble.length) expDouble[i] = (((inLong[i] & mask & 0xFFFFFFFFFFL)) << shift) >> shift;
                    }
                  }

                  long[] actByte    = new long[inByte.length  ];
                  long[] actShort   = new long[inShort.length ];
                  long[] actInt     = new long[inInt.length   ];
                  long[] actLong    = new long[inLong.length  ];
                  long[] actFloat   = new long[inFloat.length ];
                  long[] actDouble  = new long[inDouble.length];

                  int[]  evtByte    = new int[inByte.length  ];
                  int[]  evtShort   = new int[inShort.length ];
                  int[]  evtInt     = new int[inInt.length   ];
                  int[]  evtLong    = new int[inLong.length  ];
                  int[]  evtFloat   = new int[inFloat.length ];
                  int[]  evtDouble  = new int[inDouble.length];

                  int[]  chanByte   = new int[inByte.length  ];
                  int[]  chanShort  = new int[inShort.length ];
                  int[]  chanInt    = new int[inInt.length   ];
                  int[]  chanLong   = new int[inLong.length  ];
                  int[]  chanFloat  = new int[inFloat.length ];
                  int[]  chanDouble = new int[inDouble.length];

                  unpackAsLong(pf, outByte,   offset, actByte,   chanByte,   evtByte,   actByte.length  );
                  unpackAsLong(pf, outShort,  offset, actShort,  chanShort,  evtShort,  actShort.length );
                  unpackAsLong(pf, outInt,    offset, actInt,    chanInt,    evtInt,    actInt.length   );
                  unpackAsLong(pf, outLong,   offset, actLong,   chanLong,   evtLong,   actLong.length  );
                  unpackAsLong(pf, outFloat,  offset, actFloat,  chanFloat,  evtFloat,  actFloat.length );
                  unpackAsLong(pf, outDouble, offset, actDouble, chanDouble, evtDouble, actDouble.length);

                  assertArrayEquals("packAsByte(..) values for "+pfStr,   expByte,   0, expByte.length,   actByte,   0, actByte.length  );
                  assertArrayEquals("packAsShort(..) values for "+pfStr,  expShort,  0, expShort.length,  actShort,  0, actShort.length );
                  assertArrayEquals("packAsInt(..) values for "+pfStr,    expInt,    0, expInt.length,    actInt,    0, actInt.length   );
                  assertArrayEquals("packAsLong(..) values for "+pfStr,   expLong,   0, expLong.length,   actLong,   0, actLong.length  );
                  assertArrayEquals("packAsFloat(..) values for "+pfStr,  expFloat,  0, expFloat.length,  actFloat,  0, actFloat.length );
                  assertArrayEquals("packAsDouble(..) values for "+pfStr, expDouble, 0, expDouble.length, actDouble, 0, actDouble.length);

                  assertArrayEquals("packAsByte(..) channel tags for "+pfStr,   expChannel, 0, expByte.length,   chanByte,   0, chanByte.length  );
                  assertArrayEquals("packAsShort(..) channel tags for "+pfStr,  expChannel, 0, expShort.length,  chanShort,  0, chanShort.length );
                  assertArrayEquals("packAsInt(..) channel tags for "+pfStr,    expChannel, 0, expInt.length,    chanInt,    0, chanInt.length   );
                  assertArrayEquals("packAsLong(..) channel tags for "+pfStr,   expChannel, 0, expLong.length,   chanLong,   0, chanLong.length  );
                  assertArrayEquals("packAsFloat(..) channel tags for "+pfStr,  expChannel, 0, expFloat.length,  chanFloat,  0, chanFloat.length );
                  assertArrayEquals("packAsDouble(..) channel tags for "+pfStr, expChannel, 0, expDouble.length, chanDouble, 0, chanDouble.length);

                  assertArrayEquals("packAsByte(..) event tags for "+pfStr,   expEvent, 0, expByte.length,   evtByte,   0, evtByte.length  );
                  assertArrayEquals("packAsShort(..) event tags for "+pfStr,  expEvent, 0, expShort.length,  evtShort,  0, evtShort.length );
                  assertArrayEquals("packAsInt(..) event tags for "+pfStr,    expEvent, 0, expInt.length,    evtInt,    0, evtInt.length   );
                  assertArrayEquals("packAsLong(..) event tags for "+pfStr,   expEvent, 0, expLong.length,   evtLong,   0, evtLong.length  );
                  assertArrayEquals("packAsFloat(..) event tags for "+pfStr,  expEvent, 0, expFloat.length,  evtFloat,  0, evtFloat.length );
                  assertArrayEquals("packAsDouble(..) event tags for "+pfStr, expEvent, 0, expDouble.length, evtDouble, 0, evtDouble.length);
                }
              }
            }
          }

          // ---- IEEE-754 FLOAT TESTS -----------------------------------------
          int fminFieldSize = max(0,eSize) + max(0,cSize) + 32;
          for (int fSize = fminFieldSize; fSize <= 64; fSize++) {
            if (TEST_QUICK && (fSize != 32) && (fSize != fminFieldSize)
                           && (Arrays.binarySearch(dSizeOptions, fSize) < 0)) {
              continue;
            }

            for (int proc = 0; proc <= 1; proc++) {
              DataItemFormat  format = DataItemFormat.Float;
              RealComplexType real   = RealComplexType.Real;
              PayloadFormat   pf     = new PayloadFormat((proc!=0), real, format,                   false, max(0,eSize), max(0,cSize), fSize, 32, 1, 1);
              PayloadFormat   pfBits = new PayloadFormat((proc!=0), real, DataItemFormat.SignedInt, false, max(0,eSize), max(0,cSize), fSize, 32, 1, 1);
              String          pfStr  = pf.toString();

              byte[] out     = new byte[offset + SPECIAL_FLOAT_BITS.length*8];
              int[]  act     = new int[SPECIAL_FLOAT_BITS.length];
              int[]  actEvt  = new int[SPECIAL_FLOAT_BITS.length];
              int[]  actChan = new int[SPECIAL_FLOAT_BITS.length];

              packAsFloat(pf,     out, offset, SPECIAL_FLOAT_VALS, chan,    evt,    fexp.length);
              unpackAsInt(pfBits, out, offset, act,                actChan, actEvt, fexp.length);

              for (int i = 0; i < fexp.length; i++) {
                // Normalize the NaN values
                if ((act[i] >= 0x7F800001) && (act[i] <= 0x7FFFFFFF)) act[i] = 0x7FC00000;
              }

              assertArrayEquals("packAsFloat(..) values for "+pfStr,       fexp,       0, fexp.length, act,     0, fexp.length);
              assertArrayEquals("packAsFloat(..) event tags for "+pfStr,   expEvent,   0, fexp.length, actEvt,  0, fexp.length);
              assertArrayEquals("packAsFloat(..) channel tags for "+pfStr, expChannel, 0, fexp.length, actChan, 0, fexp.length);
            }
          }

          // ---- IEEE-754 DOUBLE TESTS ----------------------------------------
          int dminFieldSize = max(0,eSize) + max(0,cSize) + 64;
          for (int fSize = dminFieldSize; fSize <= 64; fSize++) {
            for (int proc = 0; proc <= 1; proc++) {
              DataItemFormat  format = DataItemFormat.Double;
              RealComplexType real   = RealComplexType.Real;
              PayloadFormat   pf     = new PayloadFormat((proc!=0), real, format,                   false, max(0,eSize), max(0,cSize), fSize, 64, 1, 1);
              PayloadFormat   pfBits = new PayloadFormat((proc!=0), real, DataItemFormat.SignedInt, false, max(0,eSize), max(0,cSize), fSize, 64, 1, 1);
              String          pfStr  = pf.toString();

              byte[] out     = new byte[offset + SPECIAL_DOUBLE_BITS.length*8];
              long[] act     = new long[SPECIAL_DOUBLE_BITS.length];
              int[]  actEvt  = new int[SPECIAL_DOUBLE_BITS.length];
              int[]  actChan = new int[SPECIAL_DOUBLE_BITS.length];

              packAsDouble(pf,     out, offset, SPECIAL_DOUBLE_VALS, chan,    evt,    dexp.length);
              unpackAsLong(pfBits, out, offset, act,                 actChan, actEvt, dexp.length);

              for (int i = 0; i < dexp.length; i++) {
                // Normalize the NaN values
                if ((act[i] >= 0x7FF0000000000001L) && (act[i] <= 0x7FFFFFFFFFFFFFFFL)) act[i] = 0x7FF8000000000000L;
              }

              assertArrayEquals("packAsDouble(..) values for "+pfStr,       dexp,       0, dexp.length, act,     0, dexp.length);
              assertArrayEquals("packAsDouble(..) event tags for "+pfStr,   expEvent,   0, dexp.length, actEvt,  0, dexp.length);
              assertArrayEquals("packAsDouble(..) channel tags for "+pfStr, expChannel, 0, dexp.length, actChan, 0, dexp.length);
            }
          }

          // ---- VRT FLOAT TESTS ----------------------------------------------
          for (DataItemFormat format : DataItemFormat.VALUES) {
            int eBits = format.getExponentBits();
            if (eBits < 0) continue; // Not VRT float type

            for (int dSize : dSizeOptions) {
              if (dSize < eBits+1) continue; // Not valid data size

              int minFieldSize = max(0,eSize) + max(0,cSize) + dSize;
              for (int fSize = minFieldSize; fSize <= 64; fSize++) {
                if (TEST_QUICK && (fSize != dSize) && (fSize != minFieldSize)
                               && (Arrays.binarySearch(dSizeOptions, fSize) < 0)) {
                  continue;
                }
                for (int proc = 0; proc <= 1; proc++) {
                  RealComplexType real  = RealComplexType.Real;
                  PayloadFormat   pf    = new PayloadFormat((proc!=0), real, format, false, max(0,eSize), max(0,cSize), fSize, dSize, 1, 1);
                  String          pfStr = pf.toString();

                  byte[]   out     = new byte[offset + inVRT.length*8];
                  long[]   expBits = new long[inVRT.length];
                  double[] expVals = new double[inVRT.length];
                  double[] actVals = new double[inVRT.length];
                  int[]    actEvt  = new int[inVRT.length];
                  int[]    actChan = new int[inVRT.length];

                  for (int i = 0; i < expBits.length; i++) {
                    expBits[i] = VRTMath.toVRTFloat(format, dSize, inVRT[i]);
                    expVals[i] = VRTMath.fromVRTFloat(format, dSize, expBits[i]);
                  }

                  packAsDouble(  pf, out, offset, inVRT,   chan,    evt,    inVRT.length);
                  unpackAsDouble(pf, out, offset, actVals, actChan, actEvt, inVRT.length);

                  assertArrayEquals("packAsDouble(..) values for "+pfStr,       expVals,    0, expVals.length,   actVals, 0, expVals.length);
                  assertArrayEquals("packAsDouble(..) channel tags for "+pfStr, expChannel, 0, expVals.length,   actChan, 0, expVals.length);
                  assertArrayEquals("packAsDouble(..) event tags for "+pfStr,   expEvent,   0, expVals.length,   actEvt,  0, expVals.length);
                }
              }
            }
          }
        }
      }
    }
  }

  @SuppressWarnings("cast")
  public void testUnpack () {
    // ==== TEST CASES =========================================================
    for (int offset = 0; offset <= 512; offset+=512) {
      // ---- INPUT VALUES -----------------------------------------------------
      byte[] buf = new byte[offset+256];
      for (int i = 0x00; i <= 0xFF; i++) {
        buf[offset+i] = (byte)i;
      }

      byte[] fbuf = new byte[offset+SPECIAL_FLOAT_BITS.length*4];
      byte[] dbuf = new byte[offset+SPECIAL_FLOAT_BITS.length*8];

      for (int i = 0; i < SPECIAL_FLOAT_BITS.length; i++) {
        VRTMath.packFloat( fbuf, offset+i*4, SPECIAL_FLOAT_VALS[i]);
        VRTMath.packDouble(dbuf, offset+i*8, SPECIAL_DOUBLE_VALS[i]);
      }

      // ---- INTEGER TESTS ----------------------------------------------------
      for (int dSize = 1; dSize <= 64; dSize++) {
        for (int fSize = dSize; fSize <= 64; fSize++) {
          for (int sign = 0; sign <= 1; sign++) {
            for (int proc = 0; proc <= 1; proc++) {
              DataItemFormat  format = (sign == 0)? DataItemFormat.UnsignedInt : DataItemFormat.SignedInt;
              RealComplexType real   = RealComplexType.Real;
              PayloadFormat   pf     = new PayloadFormat((proc!=0), real, format, false, 0, 0, fSize, dSize, 1, 1);
              String          pfStr  = pf.toString();
              int             count;

                   if (proc  ==  0) count = 256*8 / fSize;
              else if (fSize <= 32) count = 256*8 / 32 * (32 / fSize);
              else                  count = 256*8 / 64 * (64 / fSize);

              byte[]   expByte   = new byte[count];
              short[]  expShort  = new short[count];
              float[]  expFloat  = new float[count];
              int[]    expInt    = new int[count];
              double[] expDouble = new double[count];
              long[]   expLong   = new long[count];

              for (int i=0,bitOffset=0; i < count; i++,bitOffset+=fSize) {
                if (proc != 0) {
                  int avail = (fSize <= 32)? 32 - (bitOffset%32)
                                           : 64 - (bitOffset%64);
                  if (avail < fSize) {
                    bitOffset += avail;
                  }
                }
                long bits = getBits(buf, offset*8+bitOffset, dSize, (sign==1));

                expByte[i]   = (byte  )bits;
                expShort[i]  = (short )bits;
                expInt[i]    = (int   )bits;
                expLong[i]   = (long  )bits;
                expFloat[i]  = (float )bits;
                expDouble[i] = (double)bits;

                if ((sign == 0) && (bits < 0)) {
                  // Special handling for unsigned values
                  expFloat[i]  = (float )((bits & 0x7FFFFFFFFFFFFFFFL) + 9223372036854775808.0);
                  expDouble[i] = (double)((bits & 0x7FFFFFFFFFFFFFFFL) + 9223372036854775808.0);
                }
              }

              byte[]   actByte   = new byte[count];
              short[]  actShort  = new short[count];
              float[]  actFloat  = new float[count];
              int[]    actInt    = new int[count];
              double[] actDouble = new double[count];
              long[]   actLong   = new long[count];

              unpackAsByte(  pf, buf, offset, actByte,   null, null, count);
              unpackAsShort( pf, buf, offset, actShort,  null, null, count);
              unpackAsInt(   pf, buf, offset, actInt,    null, null, count);
              unpackAsLong(  pf, buf, offset, actLong,   null, null, count);
              unpackAsFloat( pf, buf, offset, actFloat,  null, null, count);
              unpackAsDouble(pf, buf, offset, actDouble, null, null, count);

              assertArrayEquals("unpackAsByte(..) for "+pfStr,   expByte,   0, count, actByte,   0, count);
              assertArrayEquals("unpackAsShort(..) for "+pfStr,  expShort,  0, count, actShort,  0, count);
              assertArrayEquals("unpackAsInt(..) for "+pfStr,    expInt,    0, count, actInt,    0, count);
              assertArrayEquals("unpackAsLong(..) for "+pfStr,   expLong,   0, count, actLong,   0, count);
              assertArrayEquals("unpackAsFloat(..) for "+pfStr,  expFloat,  0, count, actFloat,  0, count);
              assertArrayEquals("unpackAsDouble(..) for "+pfStr, expDouble, 0, count, actDouble, 0, count);
            }
          }
        }
      }

      // ---- UNSIGNED TESTS ---------------------------------------------------
      // Special test cases added for VRT-40 to make sure conversions of unsigned
      // 32/64-bit numbers work properly.
      if (true) {
        byte[]   bbuf      = { (byte)0x80, (byte)0x81, (byte)0x82, (byte)0x83,
                               (byte)0x84, (byte)0x85, (byte)0x86, (byte)0x87,
                               (byte)0x88, (byte)0x89, (byte)0x8A, (byte)0x8B,
                               (byte)0x8C, (byte)0x8D, (byte)0x8E, (byte)0x8F };
        long[]   expLong32   = { 2155971203L, 2223343239L, 2290715275L /* last is undefined */ };
        float[]  expFloat32  = { 2155971203f, 2223343239f, 2290715275f, 2358087311f };
        double[] expDouble32 = { 2155971203d, 2223343239d, 2290715275d, 2358087311d };
        float[]  expFloat64  = { 9259825810226120327f, 9838547192930733711f };
        double[] expDouble64 = { 9259825810226120327d, 9838547192930733711d };

        long[]   actLong32   = new long[3];
        float[]  actFloat32  = new float[4];
        double[] actDouble32 = new double[4];
        float[]  actFloat64  = new float[2];
        double[] actDouble64 = new double[2];

        unpackAsLong(  VRTPacket.UINT32, bbuf, 0, actLong32,   null, null, 3);
        unpackAsFloat( VRTPacket.UINT32, bbuf, 0, actFloat32,  null, null, 4);
        unpackAsDouble(VRTPacket.UINT32, bbuf, 0, actDouble32, null, null, 4);
        unpackAsFloat( VRTPacket.UINT64, bbuf, 0, actFloat64,  null, null, 2);
        unpackAsDouble(VRTPacket.UINT64, bbuf, 0, actDouble64, null, null, 2);

        assertArrayEquals("unpackAsLong(..) for UINT32",   expLong32,   0, 3, actLong32,   0, 3);
        assertArrayEquals("unpackAsFloat(..) for UINT32",  expFloat32,  0, 4, actFloat32,  0, 4);
        assertArrayEquals("unpackAsDouble(..) for UINT32", expDouble32, 0, 4, actDouble32, 0, 4);
        assertArrayEquals("unpackAsFloat(..) for UINT64",  expFloat64,  0, 2, actFloat64,  0, 2);
        assertArrayEquals("unpackAsDouble(..) for UINT64", expDouble64, 0, 2, actDouble64, 0, 2);
      }

      // ---- FLOAT TESTS (1 of 2) ---------------------------------------------
      // Test float with special values in FLOAT32 format
      if (true) {
        PayloadFormat pf  = VRTPacket.FLOAT32;
        float[]       act = new float[SPECIAL_FLOAT_BITS.length];

        unpackAsFloat(pf, fbuf, offset, act, null, null, SPECIAL_FLOAT_BITS.length);
        assertArrayEquals("unpackAsFloat(..) for "+pf, SPECIAL_FLOAT_VALS, 0, SPECIAL_FLOAT_VALS.length, act, 0, act.length);
      }

      // ---- DOUBLE TESTS -----------------------------------------------------
      // Test double with special values in DOUBLE64 format
      if (true) {
        PayloadFormat pf  = VRTPacket.DOUBLE64;
        double[]      act = new double[SPECIAL_FLOAT_BITS.length];

        unpackAsDouble(pf, dbuf, offset, act, null, null, SPECIAL_FLOAT_BITS.length);
        assertArrayEquals("unpackAsDouble(..) for "+pf, SPECIAL_DOUBLE_VALS, 0, SPECIAL_DOUBLE_VALS.length, act, 0, act.length);
      }

      // ---- FLOAT TESTS (2 of 2) ---------------------------------------------
      // Test float with various values and differing field size, presume that unpackAsInt(..)
      // works and base the expected float value on that.
      for (int fSize = 32; fSize <= 64; fSize++) {
        for (int proc = 0; proc <= 1; proc++) {
          RealComplexType real    = RealComplexType.Real;
          PayloadFormat   pfFloat = new PayloadFormat((proc!=0), real, DataItemFormat.Float,     false, 0, 0, fSize, 32, 1, 1);
          PayloadFormat   pfInt   = new PayloadFormat((proc!=0), real, DataItemFormat.SignedInt, false, 0, 0, fSize, 32, 1, 1);
          int             count;

               if (proc  ==  0) count = 256*8 / fSize;
          else if (fSize <= 32) count = 256*8 / 32 * (32 / fSize);
          else                  count = 256*8 / 64 * (64 / fSize);

          float[]  expFloat  = new float[count];
          float[]  actFloat  = new float[count];
          int[]    actInt    = new int[count];


          unpackAsFloat(pfFloat, buf, 0, actFloat, null, null, count);
          unpackAsInt(  pfInt,   buf, 0, actInt,   null, null, count);

          for (int i = 0; i < expFloat.length; i++) {
            expFloat[i] = Float.intBitsToFloat(actInt[i]);
          }

          assertArrayEquals("unpackAsFloat(..) for "+pfFloat, expFloat, 0, count, actFloat, 0, count);
        }
      }

      // ---- VRT FLOAT TESTS --------------------------------------------------
      for (DataItemFormat format : DataItemFormat.VALUES) {
        int eBits = format.getExponentBits();
        if (eBits < 0) continue; // Not VRT float type

        for (int dSize = 1+eBits; dSize <= 64; dSize++) {
          for (int fSize = dSize; fSize <= dSize; fSize++) {
            for (int proc = 0; proc <= 1; proc++) {
              RealComplexType real   = RealComplexType.Real;
              PayloadFormat   pf     = new PayloadFormat((proc!=0), real, format, false, 0, 0, fSize, dSize, 1, 1);
              String          pfStr  = pf.toString();
              int             count;

                   if (proc  ==  0) count = 256*8 / fSize;
              else if (fSize <= 32) count = 256*8 / 32 * (32 / fSize);
              else                  count = 256*8 / 64 * (64 / fSize);

              double[] exp = new double[count];
              double[] act = new double[count];

              for (int i=0,bitOffset=0; i < count; i++,bitOffset+=fSize) {
                if (proc != 0) {
                  int avail = (fSize <= 32)? 32 - (bitOffset%32)
                                           : 64 - (bitOffset%64);
                  if (avail < fSize) {
                    bitOffset += avail;
                  }
                }
                long bits = getBits(buf, offset*8+bitOffset, dSize, false);

                exp[i] = VRTMath.fromVRTFloat(format, dSize, bits);
              }

              unpackAsDouble(pf, buf, offset, act, null, null, count);
              assertArrayEquals("unpackAsDouble(..) for "+pfStr, exp, 0, count, act, 0, count);
            }
          }
        }
      }
    }
  }
}
