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
import nxm.vrt.lib.BasicDataPacket;
import nxm.vrt.lib.DataPacket;
import nxm.vrt.lib.HasFields;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.VRTConfig;
import nxm.vrt.lib.VRTMath;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.lib.VRTPacket.DataItemFormat;
import nxm.vrt.lib.VRTPacket.PacketType;
import nxm.vrt.lib.VRTPacket.PayloadFormat;
import nxm.vrt.lib.VRTPacket.RealComplexType;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static nxm.vrttest.inc.TestRunner.assertArrayEquals;
import static nxm.vrttest.inc.TestRunner.assertBufEquals;
import static nxm.vrttest.inc.TestRunner.assertEquals;
import static nxm.vrttest.inc.TestUtilities.newInstance;
import static nxm.vrttest.lib.HasFieldsTest.checkGetField;
import static nxm.vrttest.lib.HasFieldsTest.checkGetFieldName;

/** Test cases for the {@link BasicDataPacket} class. */
public class DataPacketTest<T extends DataPacket> implements Testable {
  private byte[]          input       = new byte[256]; // input test values (0x00 through 0xFF)
  private byte[]          outputSwap2 = new byte[256]; // same as input with bytes swapped by 2
  private byte[]          outputSwap4 = new byte[256]; // same as input with bytes swapped by 4
  private byte[]          outputSwap8 = new byte[256]; // same as input with bytes swapped by 8
  private PayloadFormat[] formats     = { VRTPacket.UINT1,
                        VRTPacket.INT4,   VRTPacket.UINT4,
                        VRTPacket.INT8,   VRTPacket.UINT8,
                        VRTPacket.INT16,  VRTPacket.UINT16,
                        VRTPacket.INT32,  VRTPacket.UINT32,
                        VRTPacket.INT64,  VRTPacket.UINT64 };

  private final Class<T> testedClass;

  /** Creates a new instance using the given implementation class. */
  public DataPacketTest (Class<T> testedClass) {
    this.testedClass = testedClass;
  }

  @Override
  public Class<?> getTestedClass () {
    return testedClass;
  }

  @Override
  public List<TestSet> init () throws Exception {
    for (int i = 0x00, j = 0; i <= 0xFF; i++) {
      input[j++] = (byte)i;
    }
    for (int i = 0x00, j = 0; i <= 0xFF; i+=2) {
      outputSwap2[j++] = (byte)(i+1);
      outputSwap2[j++] = (byte)( i );
    }
    for (int i = 0x00, j = 0; i <= 0xFF; i+=4) {
      outputSwap4[j++] = (byte)(i+3);
      outputSwap4[j++] = (byte)(i+2);
      outputSwap4[j++] = (byte)(i+1);
      outputSwap4[j++] = (byte)( i );
    }
    for (int i = 0x00, j = 0; i <= 0xFF; i+=8) {
      outputSwap8[j++] = (byte)(i+7);
      outputSwap8[j++] = (byte)(i+6);
      outputSwap8[j++] = (byte)(i+5);
      outputSwap8[j++] = (byte)(i+4);
      outputSwap8[j++] = (byte)(i+3);
      outputSwap8[j++] = (byte)(i+2);
      outputSwap8[j++] = (byte)(i+1);
      outputSwap8[j++] = (byte)( i );
    }

    List<TestSet> tests = new ArrayList<TestSet>();

    tests.add(new TestSet("copy",                           this, "-"));
    tests.add(new TestSet("getAssocPacketCount",            this, "+testSetAssocPacketCount"));
    tests.add(new TestSet("getDataLength",                  this, "+testSetDataLength"));
    tests.add(new TestSet("getLostBytes",                   this, "+testGetLostSamples"));
    tests.add(new TestSet("getLostSamples",                 this,  "testGetLostSamples"));
    tests.add(new TestSet("getNextTimeStamp",               this,  "testGetNextTimeStamp"));
    tests.add(new TestSet("getPacketType",                  this, "+testSetPacketType"));
    tests.add(new TestSet("getPacketValid",                 this, "+")); //same as super method with only error message changed
    tests.add(new TestSet("getPayloadFormat",               this, "+testSetPayloadFormat"));
    tests.add(new TestSet("getScalarDataLength",            this, "+testSetScalarDataLength"));
    tests.add(new TestSet("isAutomaticGainControl",         this, "+testSetAutomaticGainControl"));
    tests.add(new TestSet("isBit11",                        this, "+testSetBit11"));
    tests.add(new TestSet("isBit10",                        this, "+testSetBit10"));
    tests.add(new TestSet("isBit9",                         this, "+testSetBit9"));
    tests.add(new TestSet("isBit8",                         this, "+testSetBit8"));
    tests.add(new TestSet("isCalibratedTimeStamp",          this, "+testSetCalibratedTimeStamp"));
    tests.add(new TestSet("isDataValid",                    this, "+testSetDataValid"));
    tests.add(new TestSet("isDiscontinuous",                this, "+testSetDiscontinuous"));
    tests.add(new TestSet("isDiscontinuious",               this, "+testSetDiscontinuous"));
    tests.add(new TestSet("isInvertedSpectrum",             this, "+testSetInvertedSpectrum"));
    tests.add(new TestSet("isOverRange",                    this, "+testSetOverRange"));
    tests.add(new TestSet("isReferenceLocked",              this, "+testSetReferenceLocked"));
    tests.add(new TestSet("isSignalDetected",               this, "+testSetSignalDetected"));

    tests.add(new TestSet("setAssocPacketCount",            this,  "testSetAssocPacketCount"));
    tests.add(new TestSet("setAutomaticGainControl",        this,  "testSetAutomaticGainControl"));
    tests.add(new TestSet("setBit11",                       this,  "testSetBit11"));
    tests.add(new TestSet("setBit10",                       this,  "testSetBit10"));
    tests.add(new TestSet("setBit9",                        this,  "testSetBit9"));
    tests.add(new TestSet("setBit8",                        this,  "testSetBit8"));
    tests.add(new TestSet("setCalibratedTimeStamp",         this,  "testSetCalibratedTimeStamp"));

    if (VRTConfig.VRT_VERSION == VRTConfig.VITAVersion.V49) {
      tests.add(new TestSet("setDataLength VRT_VERSION=V49",  this,  "testSetDataLength"));
      tests.add(new TestSet("setDataLength VRT_VERSION=V49b", this, "-testSetDataLength"));
    }
    else {
      tests.add(new TestSet("setDataLength VRT_VERSION=V49",  this, "-testSetDataLength"));
      tests.add(new TestSet("setDataLength VRT_VERSION=V49b", this,  "testSetDataLength"));
    }

    tests.add(new TestSet("setDataValid",                   this,  "testSetDataValid"));
    tests.add(new TestSet("setDiscontinuous",               this,  "testSetDiscontinuous"));
    tests.add(new TestSet("setDiscontinuious",              this, "+testSetDiscontinuious"));
    tests.add(new TestSet("setInvertedSpectrum",            this,  "testSetInvertedSpectrum"));
    tests.add(new TestSet("setOverRange",                   this,  "testSetOverRange"));
    tests.add(new TestSet("setPacketType",                  this,  "testSetPacketType"));
    tests.add(new TestSet("setPayloadFormat",               this,  "testSetPayloadFormat"));
    tests.add(new TestSet("setReferenceLocked",             this,  "testSetReferenceLocked"));
    tests.add(new TestSet("setScalarDataLength",            this,  "testSetScalarDataLength"));
    tests.add(new TestSet("setSignalDetected",              this,  "testSetSignalDetected"));

    tests.add(new TestSet("getData",                        this,  "testGetData"));
    tests.add(new TestSet("getDataByte",                    this,  "testGetDataByte"));
    tests.add(new TestSet("getDataDouble",                  this, "+testGetDataByte"));
    tests.add(new TestSet("getDataFloat",                   this, "+testGetDataByte"));
    tests.add(new TestSet("getDataInt",                     this, "+testGetDataByte"));
    tests.add(new TestSet("getDataLong",                    this, "+testGetDataByte"));
    tests.add(new TestSet("getDataShort",                   this, "+testGetDataByte"));

    tests.add(new TestSet("setData",                        this,  "testSetData"));
    tests.add(new TestSet("setDataByte",                    this,  "testSetDataByte"));
    tests.add(new TestSet("setDataDouble",                  this, "+testSetDataByte"));
    tests.add(new TestSet("setDataFloat",                   this, "+testSetDataByte"));
    tests.add(new TestSet("setDataInt",                     this, "+testSetDataByte"));
    tests.add(new TestSet("setDataLong",                    this, "+testSetDataByte"));
    tests.add(new TestSet("setDataShort",                   this, "+testSetDataByte"));

    if (HasFields.class.isAssignableFrom(testedClass)) {
      tests.add(new TestSet("getField(..)",                 this,  "testGetField"));
      tests.add(new TestSet("getFieldByName(..)",           this, "+testGetField"));
      tests.add(new TestSet("getFieldCount()",              this, "+testGetFieldName"));
      tests.add(new TestSet("getFieldID(..)",               this, "+testGetFieldName"));
      tests.add(new TestSet("getFieldName(..)",             this,  "testGetFieldName"));
      tests.add(new TestSet("getFieldType(..)",             this, "+testGetFieldName"));
      tests.add(new TestSet("setField(..)",                 this, "+testGetField"));
      tests.add(new TestSet("setFieldByName(..)",           this, "+testGetField"));
    }

    return tests;
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }

  @Override
  public String toString () {
    return "Tests for DataPacket";
  }

  public void testSetCalibratedTimeStamp () {
    DataPacket p = newInstance(testedClass);
    byte buffer[];
    p.setCalibratedTimeStamp(false);
    assertEquals("isCalibratedTimeStamp(false)",false,p.isCalibratedTimeStamp());
    buffer = p.getPacket();
    assertEquals("setCalibratedTimeStamp(false)",0x80000000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x80080000);

    p.setCalibratedTimeStamp(true);
    assertEquals("isCalibratedTimeStamp(true)",true,p.isCalibratedTimeStamp());
    buffer = p.getPacket();
    assertEquals("setCalibratedTimeStamp(true)",0x80080000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x80080000);
  }

  public void testSetDataValid () {
    DataPacket p = newInstance(testedClass);
    byte buffer[];
    p.setDataValid(false);
    assertEquals("isDataValid(false)",false,p.isDataValid());
    buffer = p.getPacket();
    assertEquals("setDataValid(false)",0x40000000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x40040000);

    p.setDataValid(true);
    assertEquals("isDataValid(true)",true,p.isDataValid());
    buffer = p.getPacket();
    assertEquals("setDataValid(true)",0x40040000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x40040000);
  }

  public void testSetReferenceLocked () {
    DataPacket p = newInstance(testedClass);
    byte buffer[];
    p.setReferenceLocked(false);
    assertEquals("isReferenceLocked(false)",false,p.isReferenceLocked());
    buffer = p.getPacket();
    assertEquals("setReferenceLocked(false)",0x20000000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x20020000);

    p.setReferenceLocked(true);
    assertEquals("isReferenceLocked(true)",true,p.isReferenceLocked());
    buffer = p.getPacket();
    assertEquals("setReferenceLocked(true)",0x20020000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x20020000);
  }

  public void testSetAutomaticGainControl () {
    DataPacket p = newInstance(testedClass);
    byte buffer[];
    p.setAutomaticGainControl(false);
    assertEquals("isAutomaticGainControl(false)",false,p.isAutomaticGainControl());
    buffer = p.getPacket();
    assertEquals("setAutomaticGainControl(false)",0x10000000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x10010000);

    p.setAutomaticGainControl(true);
    assertEquals("isAutomaticGainControl(true)",true,p.isAutomaticGainControl());
    buffer = p.getPacket();
    assertEquals("setAutomaticGainControl(true)",0x10010000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x10010000);
  }

  public void testSetSignalDetected () {
    DataPacket p = newInstance(testedClass);
    byte buffer[];
    p.setSignalDetected(false);
    assertEquals("isSignalDetected(false)",false,p.isSignalDetected());
    buffer = p.getPacket();
    assertEquals("setSignalDetected(false)",0x08000000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x08008000);

    p.setSignalDetected(true);
    assertEquals("isSignalDetected(true)",true,p.isSignalDetected());
    buffer = p.getPacket();
    assertEquals("setSignalDetected(true)",0x08008000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x08008000);
  }

  public void testSetInvertedSpectrum () {
    DataPacket p = newInstance(testedClass);
    byte buffer[];
    p.setInvertedSpectrum(false);
    assertEquals("isInvertedSpectrum(false)",false,p.isInvertedSpectrum());
    buffer = p.getPacket();
    assertEquals("setInvertedSpectrum(false)",0x04000000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x04004000);

    p.setInvertedSpectrum(true);
    assertEquals("isInvertedSpectrum(true)",true,p.isInvertedSpectrum());
    buffer = p.getPacket();
    assertEquals("setInvertedSpectrum(true)",0x04004000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x04004000);
  }

  public void testSetOverRange () {
    DataPacket p = newInstance(testedClass);
    byte buffer[];
    p.setOverRange(false);
    assertEquals("isOverRange(false)",false,p.isOverRange());
    buffer = p.getPacket();
    assertEquals("setOverRange(false)",0x02000000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x02002000);

    p.setOverRange(true);
    assertEquals("isOverRange(true)",true,p.isOverRange());
    buffer = p.getPacket();
    assertEquals("setOverRange(true)",0x02002000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x02002000);
  }

  public void testSetDiscontinuous () {
    DataPacket p = newInstance(testedClass);
    byte buffer[];
    p.setDiscontinuous(false);
    assertEquals("isDiscontinuous(false)",false,p.isDiscontinuous());
    buffer = p.getPacket();
    assertEquals("setDiscontinuous(false)",0x01000000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x01001000);

    p.setDiscontinuous(true);
    assertEquals("isDiscontinuous(true)",true,p.isDiscontinuous());
    buffer = p.getPacket();
    assertEquals("setDiscontinuous(true)",0x01001000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x01001000);
  }

  public void testSetBit11 () {
    DataPacket p = newInstance(testedClass);
    byte buffer[];
    p.setBit11(false);
    assertEquals("isBit11(false)",false,p.isBit11());
    buffer = p.getPacket();
    assertEquals("setBit11(false)",0x00800000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x00800800);

    p.setBit11(true);
    assertEquals("isBit11(true)",true,p.isBit11());
    buffer = p.getPacket();
    assertEquals("setBit11(true)",0x00800800,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x00800800);
  }

  public void testSetBit10 () {
    DataPacket p = newInstance(testedClass);
    byte buffer[];
    p.setBit10(false);
    assertEquals("isBit10(false)",false,p.isBit10());
    buffer = p.getPacket();
    assertEquals("setBit10(false)",0x00400000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x00400400);

    p.setBit10(true);
    assertEquals("isBit10(true)",true,p.isBit10());
    buffer = p.getPacket();
    assertEquals("setBit10(true)",0x00400400,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x00400400);
  }

  public void testSetBit9 () {
    DataPacket p = newInstance(testedClass);
    byte buffer[];
    p.setBit9(false);
    assertEquals("isBit9(false)",false,p.isBit9());
    buffer = p.getPacket();
    assertEquals("setBit9(false)",0x00200000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x00200200);

    p.setBit9(true);
    assertEquals("isBit9(true)",true,p.isBit9());
    buffer = p.getPacket();
    assertEquals("setBit9(true)",0x00200200,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x00200200);
  }

  public void testSetBit8 () {
    DataPacket p = newInstance(testedClass);
    byte buffer[];
    p.setBit8(false);
    assertEquals("isBit8(false)",false,p.isBit8());
    buffer = p.getPacket();
    assertEquals("setBit8(false)",0x00100000,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x00100100);

    p.setBit8(true);
    assertEquals("isBit8(true)",true,p.isBit8());
    buffer = p.getPacket();
    assertEquals("setBit8(true)",0x00100100,VRTMath.unpackInt(buffer,28,BIG_ENDIAN) & 0x00100100);
  }

  public void testSetDataLength () {
    BasicDataPacket p1 = new BasicDataPacket();
    BasicDataPacket p2 = new BasicDataPacket();
    p1.setPayloadFormat(VRTPacket.INT4);
    p2.setPayloadFormat(VRTPacket.INT4);

    for (int i = 0; i < 128; i+=8) {
      byte[]  pay = new byte[i];
      String  n   = Integer.toString(i);
      int     pad = (32 - ((i * 4) % 32)) % 32;
      boolean ok  = (VRTConfig.VRT_VERSION == VRTConfig.VITAVersion.V49b) || (pad == 0);

      try {
        p1.setDataByte(pay); // i*INT8 -> i*INT4 (calls setDataLength(..))
        assertEquals("setDataByte(..) for "+n+"*INT4 throws exception", !ok, false);
      }
      catch (RuntimeException e) {
        assertEquals("setDataByte(..) for "+n+"*INT4 throws exception", !ok, true);
      }
      assertEquals("getDataLength() for "+n+"*INT4",  i,   p1.getDataLength());
      assertEquals("getPadBitCount() for "+n+"*INT4", pad, p1.getPadBitCount());

      try {
        p2.setDataLength(i);
        assertEquals("setDataLength(..) for "+n+"*INT4 throws exception", !ok, false);
      }
      catch (RuntimeException e) {
        assertEquals("setDataLength(..) for "+n+"*INT4 throws exception", !ok, true);
      }
      assertEquals("getDataLength() for "+n+"*INT4",  i,   p2.getDataLength());
      assertEquals("getPadBitCount() for "+n+"*INT4", pad, p2.getPadBitCount());
    }
  }

  public void testSetPayloadFormat () {
    BasicDataPacket p = new BasicDataPacket();
    p.setPayloadFormat(VRTPacket.INT4);
    assertEquals("getPayloadFormat",VRTPacket.INT4,p.getPayloadFormat());

    p.setPayloadFormat(VRTPacket.UINT64);
    assertEquals("getPayloadFormat",VRTPacket.UINT64,p.getPayloadFormat());
    PayloadFormat pf = new PayloadFormat(false,RealComplexType.Real,DataItemFormat.SignedInt,false,0,0,15,5,1,1);
    p.setPayloadFormat(pf);
    assertEquals("getPayloadFormat",pf,p.getPayloadFormat());
  }

  public void testSetAssocPacketCount () {
    DataPacket p = newInstance(testedClass);

    for (int i = 0; i <= 127; i++) {
      p.setAssocPacketCount((byte)i);

      byte[] packet = p.getPacket();
      byte   expVal = (byte)(i | 0x80);
      assertEquals("setAssocPacketCount("+i+") -> trailer length", 4, p.getTrailerLength());
      assertEquals("setAssocPacketCount("+i+") -> value", expVal,  packet[packet.length-1]);
      assertEquals("getAssocPacketCount()",               (byte)i, p.getAssocPacketCount());
    }

    if (true) {
      // NO TRAILER
      p.setAssocPacketCount(null);
      assertEquals("setAssocPacketCount(null) -> trailer length", 0, p.getTrailerLength());
      assertEquals("getAssocPacketCount()",                    null, p.getAssocPacketCount());
    }

    if (true) {
      // YES TRAILER
      p.setAutomaticGainControl(TRUE);
      p.setAssocPacketCount(null);
      byte[] packet = p.getPacket();
      assertEquals("setAssocPacketCount(null) -> trailer length", 4, p.getTrailerLength());
      assertEquals("setAssocPacketCount(null) -> value", (byte)0, packet[packet.length-1]);
      assertEquals("getAssocPacketCount()",                 null, p.getAssocPacketCount());
    }
  }

  public void testGetLostSamples () {
    DataPacket    p1 = newInstance(testedClass);
    DataPacket    p2 = newInstance(testedClass);
    TimeStamp     ts = TimeStamp.Y2K_GPS;
    PayloadFormat pf = VRTPacket.INT32;

    p1.setTimeStamp(ts);
    p2.setTimeStamp(ts);

    p1.setPayloadFormat(pf);

    for (double sampleRate = 1e3; sampleRate <= 1e9; sampleRate*=10) {
      if (true) {
        assertEquals("getLostSamples(..)", 0, p1.getLostSamples(ts, sampleRate));
        assertEquals("getLostBytes(..)",   0, p1.getLostBytes(  ts, sampleRate));
        assertEquals("getLostBytes(..)",   0, p2.getLostBytes(  ts, sampleRate, pf));
      }
      for (int lostSamples = 1; lostSamples <= 32768; lostSamples*=2) {
        int lostBytes = (pf.getDataItemSize() / 8) * lostSamples;

        // Previously this was computed as:
        //   dt = -1 * lostSamples * (1/sampleRate)
        // However that caused rounding errors in C++ on 32-bit platforms (e.g.
        // sampleRate=1e6 and lostSamples=1 yielded dt=-999999 not dt=-1000000).
        double    dt = -lostSamples / sampleRate;
        TimeStamp t  = ts.addPicoSeconds((long)(dt * TimeStamp.ONE_SEC));

        assertEquals("getLostSamples(..)", lostSamples, p1.getLostSamples(t, sampleRate));
        assertEquals("getLostBytes(..)",   lostBytes,   p1.getLostBytes(  t, sampleRate));
        assertEquals("getLostBytes(..)",   lostBytes,   p2.getLostBytes(  t, sampleRate, pf));
      }
    }
  }

  public void testGetNextTimeStamp () {
    DataPacket p1 = newInstance(testedClass);
    DataPacket p2 = newInstance(testedClass);

    for (int numSamples = 0; numSamples <= 65536; numSamples+=128) {
      byte[]    data       = new byte[numSamples * 2];
      double    sampleRate = 1e6;                                                 // Hz
      long      durationPS = (long)(numSamples / sampleRate * TimeStamp.ONE_SEC); // picoseconds
      TimeStamp thisTime   = TimeStamp.Y2K_GPS;
      TimeStamp nextTime   = thisTime.addPicoSeconds(durationPS);

      p1.setTimeStamp(thisTime);
      p2.setTimeStamp(thisTime);
      p1.setPayload(data);
      p2.setPayload(data);

      p1.setPayloadFormat(VRTPacket.INT16);
      assertEquals("getNextTimeStamp(sr)",    nextTime, p1.getNextTimeStamp(sampleRate));
      assertEquals("getNextTimeStamp(sr,pf)", nextTime, p2.getNextTimeStamp(sampleRate, VRTPacket.INT16));
    }
  }

  @SuppressWarnings("cast")
  public void testGetDataByte () {
    for (int pe = 0; pe <= 1; pe++) {
      for (int idx = 0; idx < formats.length; idx++) {
        PayloadFormat pf = formats[idx].clone();       pf.setProcessingEfficient(pe == 1);
        DataPacket    p1 = newInstance(testedClass);
        DataPacket    p2 = newInstance(testedClass);   p2.setPayloadFormat(pf);

        p1.setPayload(input);
        p2.setPayload(input);

        byte[]   actByte1   = p1.getDataByte(  pf);
        short[]  actShort1  = p1.getDataShort( pf);
        int[]    actInt1    = p1.getDataInt(   pf);
        long[]   actLong1   = p1.getDataLong(  pf);
        float[]  actFloat1  = p1.getDataFloat( pf);
        double[] actDouble1 = p1.getDataDouble(pf);

        switch (pf.getDataType()) {
          case UInt1:
            break;
          case Int4:
            break;
          case Int8:
            for (int i = 0x00, j = 0; i <= 0xFF; i++,j++) {
              byte val = VRTMath.unpackByte(input, i);
              assertEquals("getDataByte(pf)   for "+pf, (byte   )val, actByte1[j]);
              assertEquals("getDataShort(pf)  for "+pf, (short  )val, actShort1[j]);
              assertEquals("getDataInt(pf)    for "+pf, (int    )val, actInt1[j]);
              assertEquals("getDataLong(pf)   for "+pf, (long   )val, actLong1[j]);
              assertEquals("getDataFloat(pf)  for "+pf, (float  )val, actFloat1[j]);
              assertEquals("getDataDouble(pf) for "+pf, (double )val, actDouble1[j]);
            }
            break;
          case Int16:
            for (int i = 0x00, j = 0; i <= 0xFF; i+=2,j++) {
              short val = VRTMath.unpackShort(input, i);
              assertEquals("getDataByte(pf)   for "+pf, (byte   )val, actByte1[j]);
              assertEquals("getDataShort(pf)  for "+pf, (short  )val, actShort1[j]);
              assertEquals("getDataInt(pf)    for "+pf, (int    )val, actInt1[j]);
              assertEquals("getDataLong(pf)   for "+pf, (long   )val, actLong1[j]);
              assertEquals("getDataFloat(pf)  for "+pf, (float  )val, actFloat1[j]);
              assertEquals("getDataDouble(pf) for "+pf, (double )val, actDouble1[j]);
            }
            break;
          case Int32:
            for (int i = 0x00, j = 0; i <= 0xFF; i+=4,j++) {
              int val = VRTMath.unpackInt(input, i);
              assertEquals("getDataByte(pf)   for "+pf, (byte   )val, actByte1[j]);
              assertEquals("getDataShort(pf)  for "+pf, (short  )val, actShort1[j]);
              assertEquals("getDataInt(pf)    for "+pf, (int    )val, actInt1[j]);
              assertEquals("getDataLong(pf)   for "+pf, (long   )val, actLong1[j]);
              assertEquals("getDataFloat(pf)  for "+pf, (float  )val, actFloat1[j]);
              assertEquals("getDataDouble(pf) for "+pf, (double )val, actDouble1[j]);
            }
            break;
          case Int64:
            for (int i = 0x00, j = 0; i <= 0xFF; i+=8,j++) {
              long val = VRTMath.unpackLong(input, i);
              assertEquals("getDataByte(pf)   for "+pf, (byte   )val, actByte1[j]);
              assertEquals("getDataShort(pf)  for "+pf, (short  )val, actShort1[j]);
              assertEquals("getDataInt(pf)    for "+pf, (int    )val, actInt1[j]);
              assertEquals("getDataLong(pf)   for "+pf, (long   )val, actLong1[j]);
              assertEquals("getDataFloat(pf)  for "+pf, (float  )val, actFloat1[j]);
              assertEquals("getDataDouble(pf) for "+pf, (double )val, actDouble1[j]);
            }
            break;
          case Float:
            for (int i = 0x00, j = 0; i <= 0xFF; i+=4,j++) {
              float val = VRTMath.unpackFloat(input, i);
              assertEquals("getDataByte(pf)   for "+pf, (byte   )val, actByte1[j]);
              assertEquals("getDataShort(pf)  for "+pf, (short  )val, actShort1[j]);
              assertEquals("getDataInt(pf)    for "+pf, (int    )val, actInt1[j]);
              assertEquals("getDataLong(pf)   for "+pf, (long   )val, actLong1[j]);
              assertEquals("getDataFloat(pf)  for "+pf, (float  )val, actFloat1[j]);
              assertEquals("getDataDouble(pf) for "+pf, (double )val, actDouble1[j]);
            }
            break;
          case Double:
            for (int i = 0x00, j = 0; i <= 0xFF; i+=8,j++) {
              double val = VRTMath.unpackDouble(input, i);
              assertEquals("getDataByte(pf)   for "+pf, (byte   )val, actByte1[j]);
              assertEquals("getDataShort(pf)  for "+pf, (short  )val, actShort1[j]);
              assertEquals("getDataInt(pf)    for "+pf, (int    )val, actInt1[j]);
              assertEquals("getDataLong(pf)   for "+pf, (long   )val, actLong1[j]);
              assertEquals("getDataFloat(pf)  for "+pf, (float  )val, actFloat1[j]);
              assertEquals("getDataDouble(pf) for "+pf, (double )val, actDouble1[j]);
            }
            break;
          case UInt4:
            break;
          case UInt8:
            break;
          case UInt16:
            break;
          case UInt32:
            break;
          case UInt64:
            break;
          default:
            throw new AssertionError("Unexpected payload format: "+pf);
        }

        byte[]   actByte2   = p2.getDataByte();
        short[]  actShort2  = p2.getDataShort();
        int[]    actInt2    = p2.getDataInt();
        long[]   actLong2   = p2.getDataLong();
        float[]  actFloat2  = p2.getDataFloat();
        double[] actDouble2 = p2.getDataDouble();

        assertArrayEquals("getDataByte()   for "+pf, actByte1,   0, actByte1.length,   actByte2,   0, actByte2.length);
        assertArrayEquals("getDataShort()  for "+pf, actShort1,  0, actShort1.length,  actShort2,  0, actShort2.length);
        assertArrayEquals("getDataInt()    for "+pf, actInt1,    0, actInt1.length,    actInt2,    0, actInt2.length);
        assertArrayEquals("getDataLong()   for "+pf, actLong1,   0, actLong1.length,   actLong2,   0, actLong2.length);
        assertArrayEquals("getDataFloat()  for "+pf, actFloat1,  0, actFloat1.length,  actFloat2,  0, actFloat2.length);
        assertArrayEquals("getDataDouble() for "+pf, actDouble1, 0, actDouble1.length, actDouble2, 0, actDouble2.length);
      }
    }
  }

  @SuppressWarnings("cast")
  public void testSetDataByte () {
    byte []  inByte   = new byte[256];
    short[]  inShort  = new short[256];
    int[]    inInt    = new int[256];
    long[]   inLong   = new long[256];
    float[]  inFloat  = new float[256];
    double[] inDouble = new double[256];

    for (int i = 0; i < 256; i++) {
      inByte[i]   = (byte   )(i & 0xF);
      inShort[i]  = (short  )(i & 0xF);
      inInt[i]    = (int    )(i & 0xF);
      inLong[i]   = (long   )(i & 0xF);
      inFloat[i]  = (float  )(i & 0xF);
      inDouble[i] = (double )(i & 0xF);
    }

    for (int pe = 0; pe <= 1; pe++) {
      for (int idx = 0; idx < formats.length; idx++) {
        if (formats[idx].getDataItemSize() < 8) continue;
        PayloadFormat pf = formats[idx].clone();      pf.setProcessingEfficient(pe == 1);
        DataPacket    p1 = newInstance(testedClass);
        DataPacket    p2 = newInstance(testedClass);  p2.setPayloadFormat(pf);

        p1.setDataByte(pf, inByte);
        p2.setDataByte(inByte);
        assertArrayEquals("setDataByte(pf) for "+pf, inByte, p1.getDataByte(pf));
        assertArrayEquals("setDataByte()   for "+pf, inByte, p2.getDataByte(pf));

        p1.setDataShort(pf, inShort);
        p2.setDataShort(inShort);
        assertArrayEquals("setDataShort(pf) for "+pf, inShort, p1.getDataShort(pf));
        assertArrayEquals("setDataShort()   for "+pf, inShort, p2.getDataShort(pf));

        p1.setDataInt(pf, inInt);
        p2.setDataInt(inInt);
        assertArrayEquals("setDataInt(pf) for "+pf, inInt, p1.getDataInt(pf));
        assertArrayEquals("setDataInt()   for "+pf, inInt, p2.getDataInt(pf));

        p1.setDataLong(pf, inLong);
        p2.setDataLong(inLong);
        assertArrayEquals("setDataLong(pf) for "+pf, inLong, p1.getDataLong(pf));
        assertArrayEquals("setDataLong()   for "+pf, inLong, p2.getDataLong(pf));

        p1.setDataFloat(pf, inFloat);
        p2.setDataFloat(inFloat);
        assertArrayEquals("setDataFloat(pf) for "+pf, inFloat, p1.getDataFloat(pf));
        assertArrayEquals("setDataFloat()   for "+pf, inFloat, p2.getDataFloat(pf));

        p1.setDataDouble(pf, inDouble);
        p2.setDataDouble(inDouble);
        assertArrayEquals("setDataDouble(pf) for "+pf, inDouble, p1.getDataDouble(pf));
        assertArrayEquals("setDataDouble()   for "+pf, inDouble, p2.getDataDouble(pf));
      }
    }
  }

  public void testGetData () {
    for (int pe = 0; pe <= 1; pe++) {
      for (int idx = 0; idx < formats.length; idx++) {
        PayloadFormat pf = formats[idx].clone();      pf.setProcessingEfficient(pe == 1);
        DataPacket    p1 = newInstance(testedClass);                            p1.setPayload(input);
        DataPacket    p2 = newInstance(testedClass);  p2.setPayloadFormat(pf);  p2.setPayload(input);

        byte[] out1 = new byte[256];    p1.getData(pf, out1, 0);
        byte[] outA = new byte[256];    p2.getData(    outA, 0);

        if (pf.getDataItemSize() <= 8) {
          assertBufEquals("getData(pf,array) for "+pf, input, 0, 256, out1, 0, 256);
        }
        else if (pf.getDataItemSize() == 16) {
          assertBufEquals("getData(pf,array) for "+pf, outputSwap2, 0, 256, out1, 0, 256);
        }
        else if (pf.getDataItemSize() == 32) {
          assertBufEquals("getData(pf,array) for "+pf, outputSwap4, 0, 256, out1, 0, 256);
        }
        else if (pf.getDataItemSize() == 64) {
          assertBufEquals("getData(pf,array) for "+pf, outputSwap8, 0, 256, out1, 0, 256);
        }
        else {
          throw new AssertionError("Unexpected format "+pf);
        }

        assertArrayEquals("getData(array) for "+pf, out1, outA);
      }
    }
  }

  public void testSetData () {
    for (int pe = 0; pe <= 1; pe++) {
      for (int idx = 0; idx < formats.length; idx++) {
        PayloadFormat pf = formats[idx].clone();      pf.setProcessingEfficient(pe == 1);
        DataPacket    p1 = newInstance(testedClass);
        DataPacket    pA = newInstance(testedClass);  pA.setPayloadFormat(pf);

        p1.setData(pf, input, 0, 256);
        pA.setData(    input, 0, 256);

        if (pf.getDataItemSize() <= 8) {
          assertArrayEquals("setData(pf,array,off,len) for "+pf, input, p1.getPayload());
        }
        else if (pf.getDataItemSize() == 16) {
          assertArrayEquals("setData(pf,array,off,len) for "+pf, outputSwap2, p1.getPayload());
        }
        else if (pf.getDataItemSize() == 32) {
          assertArrayEquals("setData(pf,array,off,len) for "+pf, outputSwap4, p1.getPayload());
        }
        else if (pf.getDataItemSize() == 64) {
          assertArrayEquals("setData(pf,array,off,len) for "+pf, outputSwap8, p1.getPayload());
        }
        else {
          throw new AssertionError("Unexpected format "+pf);
        }
        assertArrayEquals("setData(array,off,len) for "+pf, p1.getPayload(), pA.getPayload());
      }
    }
  }

  public void testSetScalarDataLength () {
    DataPacket p1 = newInstance(testedClass);   p1.setPayload(input);
    DataPacket p2 = newInstance(testedClass);   p2.setPayload(input);
    DataPacket p3 = newInstance(testedClass);   p3.setPayload(input);
    DataPacket p4 = newInstance(testedClass);   p4.setPayload(input);
    DataPacket p5 = newInstance(testedClass);   p5.setPayload(input);
    DataPacket p6 = newInstance(testedClass);   p6.setPayload(input);

    int numWords = 256 / 4; // num 32-bit words in payload

    for (int size = 1; size <= 64; size++) {
      for (int cx = 0; cx <= 2; cx++) {
        RealComplexType type;
        int             complexMult;

        switch (cx) {
          case 0: type = RealComplexType.Real;             complexMult = 1; break;
          case 1: type = RealComplexType.ComplexCartesian; complexMult = 2; break;
          case 2: type = RealComplexType.ComplexPolar;     complexMult = 2; break;
          default: throw new AssertionError("This should be impossible");
        }

        for (int fmt = 0; fmt <= 4; fmt++) {
          if ((fmt == 2) && (size != 32)) break;
          if ((fmt == 3) && (size != 64)) break;

          DataItemFormat format;
          switch (fmt) {
            case 0: format = DataItemFormat.SignedInt;   break;
            case 1: format = DataItemFormat.UnsignedInt; break;
            case 2: format = DataItemFormat.Float;       break;
            case 3: format = DataItemFormat.Double;      break;
            default: throw new AssertionError("This should be impossible");
          }

          PayloadFormat pf = new PayloadFormat(type, format, size);

          for (int pe = 0; pe <= 1; pe++) {
            pf.setProcessingEfficient(pe == 1);
            p2.setPayloadFormat(pf);
            p4.setPayloadFormat(pf);
            p6.setPayloadFormat(pf);

            int scalarLen;
            if (pe == 0) {         // processing efficient
              scalarLen = (numWords * 32) / size;
            }
            else if (size <= 32) { // 32-bit sizing
              int elemPerWord = 32 / size;
              scalarLen = numWords * elemPerWord;
            }
            else {                 // 64-bit sizing (1 element per 2 32-bit words)
              scalarLen = numWords / 2;
            }
            int dataLen = scalarLen / complexMult;

            assertEquals("getDataLength(pf)         for "+pf, dataLen,   p1.getDataLength(pf));
            assertEquals("getDataLength()           for "+pf, dataLen,   p2.getDataLength());
            assertEquals("getScalarDataLength(pf)   for "+pf, scalarLen, p1.getScalarDataLength(pf));
            assertEquals("getScalarDataLength()     for "+pf, scalarLen, p2.getScalarDataLength());

            if ((VRTConfig.VRT_VERSION == VRTConfig.VITAVersion.V49) && ((scalarLen % 2) == 1)) {
              // skip next part as it uses PadBitCount
            }
            else {
              p3.setDataLength(pf, dataLen);
              p4.setDataLength(dataLen);
              p5.setScalarDataLength(pf, scalarLen);
              p6.setScalarDataLength(scalarLen);
              assertEquals("setDataLength(pf,n)       for "+pf, dataLen,   p3.getDataLength(pf));
              assertEquals("setDataLength(n)          for "+pf, dataLen,   p4.getDataLength());
              assertEquals("setScalarDataLength(pf,n) for "+pf, scalarLen, p5.getScalarDataLength(pf));
              assertEquals("setScalarDataLength(n)    for "+pf, scalarLen, p6.getScalarDataLength());
            }
          }
        }
      }
    }
  }

  public void testSetPacketType () {
    DataPacket p = newInstance(testedClass);

    p.setPacketType(PacketType.UnidentifiedData);
    assertEquals("setPacketType(UnidentifiedData)", PacketType.UnidentifiedData, p.getPacketType());

    p.setPacketType(PacketType.Data);
    assertEquals("setPacketType(Data)", PacketType.Data, p.getPacketType());

    p.setPacketType(PacketType.UnidentifiedExtData);
    assertEquals("setPacketType(UnidentifiedExtData)", PacketType.UnidentifiedExtData, p.getPacketType());

    p.setPacketType(PacketType.ExtData);
    assertEquals("setPacketType(ExtData)", PacketType.ExtData, p.getPacketType());

    try {
      p.setPacketType(PacketType.Context);
      assertEquals("setPacketType(Context) fails", true, false);
    }
    catch (Exception e) {
      assertEquals("setPacketType(Context) fails", true, true);
    }

    try {
      p.setPacketType(PacketType.ExtContext);
      assertEquals("setPacketType(ExtContext) fails", true, false);
    }
    catch (Exception e) {
      assertEquals("setPacketType(ExtContext) fails", true, true);
    }
  }

  private static final int        FIELD_OFFSET = 3;
  private static final String[]   FIELD_NAMES  = {
    "CalibratedTimeStamp", "DataValid",        "ReferenceLocked", "AGC",
    "SignalDetected",      "InvertedSpectrum", "OverRange",       "Discontinuous",
    "Bit11",               "Bit10",            "Bit9",            "Bit8",
    "AssocPacketCount",    "PayloadFormat"
  };
  private static final Class<?>[] FIELD_TYPES  = {
    Boolean.class, Boolean.class, Boolean.class, Boolean.class,
    Boolean.class, Boolean.class, Boolean.class, Boolean.class,
    Boolean.class, Boolean.class, Boolean.class, Boolean.class,
    Byte.class,    PayloadFormat.class
  };

  public void testGetFieldName () {
    HasFields hf = (HasFields)newInstance(testedClass);
    checkGetFieldName(hf, FIELD_OFFSET, FIELD_NAMES, FIELD_TYPES);
  }

  public void testGetField () {
    HasFields hf = (HasFields)newInstance(testedClass);
    checkGetField(hf, "CalibratedTimeStamp",  TRUE, FALSE);
    checkGetField(hf, "DataValid",            TRUE, FALSE);
    checkGetField(hf, "ReferenceLocked",      TRUE, FALSE);
    checkGetField(hf, "AGC",                  TRUE, FALSE, "isAutomaticGainControl", "setAutomaticGainControl");
    checkGetField(hf, "SignalDetected",       TRUE, FALSE);
    checkGetField(hf, "InvertedSpectrum",     TRUE, FALSE);
    checkGetField(hf, "OverRange",            TRUE, FALSE);
    checkGetField(hf, "Discontinuous",        TRUE, FALSE);
    checkGetField(hf, "Bit11",                TRUE, FALSE);
    checkGetField(hf, "Bit10",                TRUE, FALSE);
    checkGetField(hf, "Bit9",                 TRUE, FALSE);
    checkGetField(hf, "Bit8",                 TRUE, FALSE);
    checkGetField(hf, "AssocPacketCount", (byte)7,         (byte)42);
    checkGetField(hf, "PayloadFormat",    VRTPacket.INT32, VRTPacket.DOUBLE64);
  }
}
