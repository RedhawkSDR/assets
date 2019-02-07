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
import nxm.vrt.lib.ContextPacket;
import nxm.vrt.lib.ContextPacket.ContextAssocLists;
import nxm.vrt.lib.ContextPacket.Ephemeris;
import nxm.vrt.lib.ContextPacket.GeoSentences;
import nxm.vrt.lib.ContextPacket.Geolocation;
import nxm.vrt.lib.HasFields;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.TimeStamp.IntegerMode;
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
import static nxm.vrttest.inc.TestRunner.assertEquals;
import static nxm.vrttest.inc.TestUtilities.newInstance;
import static nxm.vrttest.lib.HasFieldsTest.checkGetField;
import static nxm.vrttest.lib.HasFieldsTest.checkGetFieldName;

/** Tests for the {@link ContextPacket} class. */
public class ContextPacketTest<T extends ContextPacket> implements Testable {
  static final int CHANGE_IND     = 0x80000000;
  static final int REF_POINT      = 0x40000000;
  static final int BANDWIDTH      = 0x20000000;
  static final int IF_FREQ        = 0x10000000;
  static final int RF_FREQ        = 0x08000000;
  static final int RF_OFFSET      = 0x04000000;
  static final int IF_OFFSET      = 0x02000000;
  static final int REF_LEVEL      = 0x01000000;
  static final int GAIN           = 0x00800000;
  static final int OVER_RANGE     = 0x00400000;
  static final int SAMPLE_RATE    = 0x00200000;
  static final int TIME_ADJUST    = 0x00100000;
  static final int TIME_CALIB     = 0x00080000;
  static final int TEMPERATURE    = 0x00040000;
  static final int DEVICE_ID      = 0x00020000;
  static final int STATE_EVENT    = 0x00010000;
  static final int DATA_FORMAT    = 0x00008000;
  static final int GPS_EPHEM      = 0x00004000;
  static final int INS_EPHEM      = 0x00002000;
  static final int ECEF_EPHEM     = 0x00001000;
  static final int REL_EPHEM      = 0x00000800;
  static final int EPHEM_REF      = 0x00000400;
  static final int GPS_ASCII      = 0x00000200;
  static final int CONTEXT_ASOC   = 0x00000100;
  static final int ECEF_EPHEM_ADJ = 0x00000080;
  static final int REL_EPHEM_ADJ  = 0x00000040;


  private final Class<T> testedClass;

  /** Creates a new instance using the given implementation class. */
  public ContextPacketTest (Class<T> testedClass) {
    this.testedClass = testedClass;
  }

  @Override
  public Class<?> getTestedClass () {
    return testedClass;
  }

  @Override
  public String toString () {
    return "Tests for ContextPacket";
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    tests.add(new TestSet("getAdjustedTimeStamp",         this, "testGetAdjustedTimestamp"));
    tests.add(new TestSet("getBandOffsetIF",              this, "+testSetBandOffsetIF"));
    tests.add(new TestSet("getBandwidth",                 this, "+testSetBandwidth"));
    tests.add(new TestSet("getContextAssocLists",         this, "+testSetContextAssocLists"));
    tests.add(new TestSet("getDataPayloadFormat",         this, "+testSetDataPayloadFormat"));
    tests.add(new TestSet("getDeviceID",                  this, "+testSetDeviceID"));
    tests.add(new TestSet("getDeviceIdentifier",          this, "+testSetDeviceID"));
    tests.add(new TestSet("getEphemerisECEF",             this, "+testSetEphemerisECEF"));
    tests.add(new TestSet("getEphemerisReference",        this, "+testSetEphemerisReference"));
    tests.add(new TestSet("getEphemerisRelative",         this, "+testSetEphemerisECEF"));
    tests.add(new TestSet("getFrequencyIF",               this, "+testSetFrequencyIF"));
    tests.add(new TestSet("getFrequencyOffsetRF",         this, "+testSetFrequencyOffsetRF"));
    tests.add(new TestSet("getFrequencyRF",               this, "+testSetFrequencyRF"));
    tests.add(new TestSet("getGain",                      this, "+testSetGain"));
    tests.add(new TestSet("getGain1",                     this, "+testSetGain"));
    tests.add(new TestSet("getGain2",                     this, "+testSetGain"));
    tests.add(new TestSet("getGeolocationGPS",            this, "+testSetGeolocation"));
    tests.add(new TestSet("getGeolocationINS",            this, "+testSetGeolocation"));
    tests.add(new TestSet("getGeoSentences",              this, "+testSetGeoSentences"));
    tests.add(new TestSet("getOverRangeCount",            this, "+testSetOverRangeCount"));
    tests.add(new TestSet("getReferenceLevel",            this, "+testSetReferenceLevel"));
    tests.add(new TestSet("getReferencePointIdentifier",  this, "+testSetReferencePointIdentifier"));
    tests.add(new TestSet("getSampleRate",                this, "+testSetSampleRate"));
    tests.add(new TestSet("getTimeStampAdjustment",       this, "+testSetTimeStampAdjustment"));
    tests.add(new TestSet("getTimeStampCalibration",      this, "+testSetTimeStampCalibration"));
    tests.add(new TestSet("getTemperature",               this, "+testSetTemperature"));
    tests.add(new TestSet("getUserDefinedBits",           this, "+testSetUserDefinedBits"));
    tests.add(new TestSet("isAutomaticGainControl",       this, "+testSetAutomaticGainControl"));
    tests.add(new TestSet("isCalibratedTimestamp",        this, "+testSetCalibratedTimestamp"));
    tests.add(new TestSet("isChangePacket",               this, "+testSetChangePacket"));
    tests.add(new TestSet("isDataValid",                  this, "+testSetDataValid"));
    tests.add(new TestSet("isDiscontinuous",              this, "+testSetDiscontinuous"));
    tests.add(new TestSet("isDiscontinuious",             this, "+testSetDiscontinuous"));
    tests.add(new TestSet("isInvertedSpectrum",           this, "+testSetInvertedSpectrum"));
    tests.add(new TestSet("isOverRange",                  this, "+testSetOverRange"));
    tests.add(new TestSet("isReferenceLocked",            this, "+testSetReferenceLocked"));
    tests.add(new TestSet("isSignalDetected",             this, "+testSetSignalDetected"));
    tests.add(new TestSet("setAutomaticGainControl",      this, "testSetAutomaticGainControl"));
    tests.add(new TestSet("setBandOffsetIF",              this, "testSetBandOffsetIF"));
    tests.add(new TestSet("setBandwidth",                 this, "testSetBandwidth"));
    tests.add(new TestSet("setCalibratedTimestamp",       this, "testSetCalibratedTimestamp"));
    tests.add(new TestSet("setChangePacket",              this, "testSetChangePacket"));
    tests.add(new TestSet("setContextAssocLists",         this, "testSetContextAssocLists"));
    tests.add(new TestSet("setDataPayloadFormat",         this, "testSetDataPayloadFormat"));
    tests.add(new TestSet("setDataValid",                 this, "testSetDataValid"));
    tests.add(new TestSet("setDeviceID",                  this, "testSetDeviceID"));
    tests.add(new TestSet("setDeviceIdentifier",          this, "+testSetDeviceID"));
    tests.add(new TestSet("setDiscontinuous",             this, "testSetDiscontinuous"));
    tests.add(new TestSet("setDiscontinuious",            this, "+testSetDiscontinuous"));
    tests.add(new TestSet("setEphemerisECEF",             this, "testSetEphemerisECEF"));
    tests.add(new TestSet("setEphemerisReference",        this, "testSetEphemerisReference"));
    tests.add(new TestSet("setEphemerisRelative",         this, "+testSetEphemerisECEF"));
    tests.add(new TestSet("setGain",                      this, "testSetGain"));
    tests.add(new TestSet("setGain1",                     this, "+testSetGain"));
    tests.add(new TestSet("setGain2",                     this, "+testSetGain"));
    tests.add(new TestSet("setGeolocationGPS",            this, "testSetGeolocation"));
    tests.add(new TestSet("setGeolocationINS",            this, "+testSetGeolocation"));
    tests.add(new TestSet("setGeoSentences",              this, "testSetGeoSentences"));
    tests.add(new TestSet("setFrequencyIF",               this, "testSetFrequencyIF"));
    tests.add(new TestSet("setFrequencyOffsetRF",         this, "testSetFrequencyOffsetRF"));
    tests.add(new TestSet("setFrequencyRF",               this, "testSetFrequencyRF"));
    tests.add(new TestSet("setInvertedSpectrum",          this, "testSetInvertedSpectrum"));
    tests.add(new TestSet("setOverRange",                 this, "testSetOverRange"));
    tests.add(new TestSet("setOverRangeCount",            this, "testSetOverRangeCount"));
    tests.add(new TestSet("setPacketType",                this, "testSetPacketType"));
    tests.add(new TestSet("setReferenceLevel",            this, "testSetReferenceLevel"));
    tests.add(new TestSet("setReferenceLocked",           this, "testSetReferenceLocked"));
    tests.add(new TestSet("setReferencePointIdentifier",  this, "testSetReferencePointIdentifier"));
    tests.add(new TestSet("setSampleRate",                this, "testSetSampleRate"));
    tests.add(new TestSet("setSignalDetected",            this, "testSetSignalDetected"));
    tests.add(new TestSet("setTimeStampAdjustment",       this, "testSetTimeStampAdjustment"));
    tests.add(new TestSet("setTimeStampCalibration",      this, "testSetTimeStampCalibration"));
    tests.add(new TestSet("setTemperature",               this, "testSetTemperature"));
    tests.add(new TestSet("setUserDefinedBits",           this, "testSetUserDefinedBits"));
    tests.add(new TestSet("resetForResend",               this, "testResetForResend"));

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

  public void testSetChangePacket () {
    ContextPacket p = newInstance(testedClass);
    byte[] buffer;
    p.setChangePacket(false);
    buffer = p.getPayload();
    assertEquals("getChangePacket(false)", false,      p.isChangePacket());
    assertEquals("setChangePacket(false)", 0x00000000, VRTMath.unpackInt(buffer, 0, BIG_ENDIAN) & 0x8000000);

    p.setChangePacket(true);
    buffer = p.getPayload();
    assertEquals("getChangePacket(true)", true,       p.isChangePacket());
    assertEquals("setChangePacket(true)", 0x80000000, VRTMath.unpackInt(buffer, 0, BIG_ENDIAN) & 0x80000000);
  }

  public void testSetCalibratedTimestamp () {
    ContextPacket p = newInstance(testedClass);

    p.setCalibratedTimeStamp(true);
    assertEquals("isDataValid()",                true,       p.isCalibratedTimeStamp());
    assertEquals("setCalibratedTimestamp(true)", 0x80080000, getOneWordFieldValue(p, STATE_EVENT) & 0x80080000);

    p.setCalibratedTimeStamp(false);
    assertEquals("isDataValid()",                 false,      p.isCalibratedTimeStamp());
    assertEquals("setCalibratedTimestamp(false)", 0x80000000, getOneWordFieldValue(p, STATE_EVENT) & 0x80080000);
  }

  public void testSetDataValid () {
    ContextPacket p = newInstance(testedClass);

    p.setDataValid(true);
    assertEquals("isDataValid()",      true,       p.isDataValid());
    assertEquals("setDataValid(true)", 0x40040000, getOneWordFieldValue(p, STATE_EVENT) & 0x40040000);

    p.setDataValid(false);
    assertEquals("isDataValid()",       false,      p.isDataValid());
    assertEquals("setDataValid(false)", 0x40000000, getOneWordFieldValue(p, STATE_EVENT) & 0x40040000);
  }

  public void testSetReferenceLocked () {
    ContextPacket p = newInstance(testedClass);

    p.setReferenceLocked(true);
    assertEquals("isReferenceLocked()",      true,      p.isReferenceLocked());
    assertEquals("setReferenceLocked(true)", 0x20020000, getOneWordFieldValue(p, STATE_EVENT) & 0x20020000);

    p.setReferenceLocked(false);
    assertEquals("isReferenceLocked()",       false,      p.isReferenceLocked());
    assertEquals("setReferenceLocked(false)", 0x20000000, getOneWordFieldValue(p, STATE_EVENT) & 0x20020000);
  }

  public void testSetAutomaticGainControl () {
    ContextPacket p = newInstance(testedClass);

    p.setAutomaticGainControl(true);
    assertEquals("isAutomaticGainControl()",      true,       p.isAutomaticGainControl());
    assertEquals("setAutomaticGainControl(true)", 0x10010000, getOneWordFieldValue(p, STATE_EVENT) & 0x10010000);

    p.setAutomaticGainControl(false);
    assertEquals("isAutomaticGainControl()",       false,      p.isAutomaticGainControl());
    assertEquals("setAutomaticGainControl(false)", 0x10000000, getOneWordFieldValue(p, STATE_EVENT) & 0x10010000);
  }

  public void testSetSignalDetected () {
    ContextPacket p = newInstance(testedClass);

    p.setSignalDetected(true);
    assertEquals("isSignalDetected(true)",  true,       p.isSignalDetected());
    assertEquals("setSignalDetected(true)", 0x08008000, getOneWordFieldValue(p, STATE_EVENT) & 0x08008000);

    p.setSignalDetected(false);
    assertEquals("isSignalDetected(true)",   false,      p.isSignalDetected());
    assertEquals("setSignalDetected(false)", 0x08000000, getOneWordFieldValue(p, STATE_EVENT) & 0x08008000);
  }

  public void testSetInvertedSpectrum () {
    ContextPacket p = newInstance(testedClass);

    p.setInvertedSpectrum(true);
    assertEquals("isInvertedSpectrum()",      true,       p.isInvertedSpectrum());
    assertEquals("setInvertedSpectrum(true)", 0x04004000, getOneWordFieldValue(p, STATE_EVENT) & 0x04004000);

    p.setInvertedSpectrum(false);
    assertEquals("isInvertedSpectrum()",       false,      p.isInvertedSpectrum());
    assertEquals("setInvertedSpectrum(false)", 0x04000000, getOneWordFieldValue(p, STATE_EVENT) & 0x04004000);
  }

  public void testSetOverRange () {
    ContextPacket p = newInstance(testedClass);

    p.setOverRange(true);
    assertEquals("isOverRange()",      true,       p.isOverRange());
    assertEquals("setOverRange(true)", 0x02002000, getOneWordFieldValue(p, STATE_EVENT) & 0x02002000);

    p.setOverRange(false);
    assertEquals("isOverRange()",       false,      p.isOverRange());
    assertEquals("setOverRange(false)", 0x02000000, getOneWordFieldValue(p, STATE_EVENT) & 0x02002000);
  }

  public void testSetDiscontinuous () {
    ContextPacket p = newInstance(testedClass);

    p.setDiscontinuous(true);
    assertEquals("isDiscontinuous()",      true,       p.isDiscontinuous());
    assertEquals("setDiscontinuous(true)", 0x01001000, getOneWordFieldValue(p, STATE_EVENT) & 0x01001000);

    p.setDiscontinuous(false);
    assertEquals("isDiscontinuous()",       false,      p.isDiscontinuous());
    assertEquals("setDiscontinuous(false)", 0x01000000, getOneWordFieldValue(p, STATE_EVENT) & 0x01001000);

  }

  public void testResetForResend () {
    ContextPacket p = newInstance(testedClass);

    p.setOverRange(true);
    p.setDiscontinuous(true);
    p.setCalibratedTimeStamp(true);
    p.setReferenceLocked(true);

    assertEquals("resetForResend() before", 0xA30A3000, getOneWordFieldValue(p, STATE_EVENT) & 0xA30A3000);

    p.resetForResend(new TimeStamp(IntegerMode.GPS,0,0));
    assertEquals("resetForResend() after", 0xA00A0000, getOneWordFieldValue(p, STATE_EVENT) & 0xA30A3000);
  }


  public void testSetReferencePointIdentifier () {
    ContextPacket p = newInstance(testedClass);

    p.setReferencePointIdentifier(0);
    assertEquals("getReferencePointIdentifier(0)", 0, p.getReferencePointIdentifier());
    assertEquals("setReferencePointIdentifier(0)", 0, getOneWordFieldValue(p, REF_POINT));

    p.setReferencePointIdentifier(0x12345678);
    assertEquals("getReferencePointIdentifier(0x12345678)", 0x12345678, p.getReferencePointIdentifier());
    assertEquals("setReferencePointIdentifier(0x12345678)", 0x12345678, getOneWordFieldValue(p, REF_POINT));

    p.setReferencePointIdentifier(0xFFFFFFFF);
    assertEquals("getReferencePointIdentifier(0xFFFFFFFF)", 0xFFFFFFFF, p.getReferencePointIdentifier());
    assertEquals("setReferencePointIdentifier(0xFFFFFFFF)", 0xFFFFFFFF, getOneWordFieldValue(p, REF_POINT));

  }

  public void testSetBandwidth () {
    ContextPacket p = newInstance(testedClass);

    p.setBandwidth(1.0);
    assertEquals("getBandwidth(0)", 1.0,                 p.getBandwidth(), 0.00000095);
    assertEquals("setBandwidth(0)", 0x0000000000100000L, getTwoWordFieldValue(p, BANDWIDTH));

    p.setBandwidth(0.00000095);
    assertEquals("getBandwidth(0.00000095)", 0.00000095,          p.getBandwidth(), 0.00000095);
    assertEquals("setBandwidth(0.00000095)", 0x0000000000000001L, getTwoWordFieldValue(p, BANDWIDTH));

    p.setBandwidth(15.5);
    assertEquals("getBandwidth(15.5)", 15.5,                p.getBandwidth(), 0.00000095);
    //last 5 bytes are after the radixpoint
    assertEquals("setBandwidth(15.5)", 0x0000000000F80000L, getTwoWordFieldValue(p, BANDWIDTH));
  }

  public void testSetFrequencyIF () {
    ContextPacket p = newInstance(testedClass);

    p.setFrequencyIF(1.0);
    assertEquals("getFrequencyIF(1)", 1.0, p.getFrequencyIF(), 0.00000095);
    assertEquals("setFrequencyIF(1)", 0x0000000000100000L, getTwoWordFieldValue(p, IF_FREQ));

    p.setFrequencyIF(-0.00000095);
    assertEquals("getFrequencyIF(-0.00000095)", -0.00000095,         p.getFrequencyIF(), 0.00000095);
    assertEquals("setFrequencyIF(-0.00000095)", 0xFFFFFFFFFFFFFFFFL, getTwoWordFieldValue(p, IF_FREQ));

    p.setFrequencyIF(0.00000095);
    assertEquals("getFrequencyIF(0.00000095)", 0.00000095,          p.getFrequencyIF(), 0.00000095);
    assertEquals("setFrequencyIF(0.00000095)", 0x0000000000000001L, getTwoWordFieldValue(p, IF_FREQ));
  }

  public void testSetFrequencyRF () {
    ContextPacket p = newInstance(testedClass);

    p.setFrequencyRF(1.0);
    assertEquals("getFrequencyRF(1)", 1.0,                 p.getFrequencyRF(), 0.00000095);
    assertEquals("setFrequencyRF(1)", 0x0000000000100000L, getTwoWordFieldValue(p, RF_FREQ));

    p.setFrequencyRF(-0.00000095);
    assertEquals("getFrequencyRF(-0.00000095)", -0.00000095,         p.getFrequencyRF(), 0.00000095);
    assertEquals("setFrequencyRF(-0.00000095)", 0xFFFFFFFFFFFFFFFFL, getTwoWordFieldValue(p, RF_FREQ));

    p.setFrequencyRF(0.00000095);
    assertEquals("getFrequencyRF(0.00000095)", 0.00000095,          p.getFrequencyRF(), 0.00000095);
    assertEquals("setFrequencyRF(0.00000095)", 0x0000000000000001L, getTwoWordFieldValue(p, RF_FREQ));
  }

  public void testSetBandOffsetIF () {
    ContextPacket p = newInstance(testedClass);

    p.setBandOffsetIF(1.0);
    assertEquals("getBandOffsetIF(1)", 1.0,                 p.getBandOffsetIF(), 0.00000095);
    assertEquals("setBandOffsetIF(1)", 0x0000000000100000L, getTwoWordFieldValue(p, IF_OFFSET));

    p.setBandOffsetIF(-0.00000095);
    assertEquals("getBandOffsetIF(-0.00000095)", -0.00000095,         p.getBandOffsetIF(), 0.00000095);
    assertEquals("setBandOffsetIF(-0.00000095)", 0xFFFFFFFFFFFFFFFFL, getTwoWordFieldValue(p, IF_OFFSET));

    p.setBandOffsetIF(0.00000095);
    assertEquals("getBandOffsetIF(0.00000095)", 0.00000095,          p.getBandOffsetIF(), 0.00000095);
    assertEquals("setBandOffsetIF(0.00000095)", 0x0000000000000001L, getTwoWordFieldValue(p, IF_OFFSET));
  }

  public void testSetFrequencyOffsetRF () {
    ContextPacket p = newInstance(testedClass);

    p.setFrequencyOffsetRF(1.0);
    assertEquals("getFrequencyOffsetRF(1)", 1.0,                 p.getFrequencyOffsetRF(), 0.00000095);
    assertEquals("setFrequencyOffsetRF(1)", 0x0000000000100000L, getTwoWordFieldValue(p, RF_OFFSET));

    p.setFrequencyOffsetRF(-0.00000095);
    assertEquals("getFrequencyOffsetRF(-0.00000095)", -0.00000095,         p.getFrequencyOffsetRF(), 0.00000095);
    assertEquals("setFrequencyOffsetRF(-0.00000095)", 0xFFFFFFFFFFFFFFFFL, getTwoWordFieldValue(p, RF_OFFSET));

    p.setFrequencyOffsetRF(0.00000095);
    assertEquals("getFrequencyOffsetRF(0.00000095)", 0.00000095,          p.getFrequencyOffsetRF(), 0.00000095);
    assertEquals("setFrequencyOffsetRF(0.00000095)", 0x0000000000000001L, getTwoWordFieldValue(p, RF_OFFSET));
  }

  public void testSetReferenceLevel () {
    ContextPacket p = newInstance(testedClass);

    p.setReferenceLevel((float)1.0);
    assertEquals("getReferenceLevel(1)", 1.0,        p.getReferenceLevel(), 0.001);
    assertEquals("setReferenceLevel(1)", 0x00000080, getOneWordFieldValue(p, REF_LEVEL));

    p.setReferenceLevel((float)-1.0);
    assertEquals("getReferenceLevel(-1)", -1.0,       p.getReferenceLevel(), 0.001);
    assertEquals("setReferenceLevel(-1)", 0x0000FF80, getOneWordFieldValue(p, REF_LEVEL));

    p.setReferenceLevel((float)-0.0078125);
    assertEquals("getReferenceLevel(-0.0078125)",  -0.0078125, p.getReferenceLevel(),0.001);
    assertEquals("setReferenceLevel(-0.0078125)",  0x0000FFFF, getOneWordFieldValue(p, REF_LEVEL));

    p.setReferenceLevel((float)0.0078125);
    assertEquals("getReferenceLevel(0.0078125)", 0.0078125,  p.getReferenceLevel(),0.001);
    assertEquals("setReferenceLevel(0.0078125)", 0x00000001, getOneWordFieldValue(p, REF_LEVEL));
  }

  public void testSetGain () {
    ContextPacket p = newInstance(testedClass);

    p.setGain(1.0f, 1.0f);
    assertEquals("getGain1(1.0)", 1.0,        p.getGain1(), 0.001);
    assertEquals("getGain2(1.0)", 1.0,        p.getGain2(), 0.001);
    assertEquals("getGain2(1.0)", 2.0,        p.getGain(),  0.001);
    assertEquals("setGain(-1)",   0x00800080, getOneWordFieldValue(p, GAIN));

    p.setGain(-1.0f, -1.0f);
    assertEquals("getGain1(-1.0)", -1.0,       p.getGain1(), 0.001);
    assertEquals("getGain2(-1.0)", -1.0,       p.getGain2(), 0.001);
    assertEquals("getGain(-1.0)",  -2.0,       p.getGain(),  0.001);
    assertEquals("setGain(-1)",    0xFF80FF80, getOneWordFieldValue(p, GAIN));

    p.setGain(0.0078125f, 0.0078125f);
    assertEquals("getGain1(0.0078125)", 0.0078125,   p.getGain1(), 0.001);
    assertEquals("getGain2(0.0078125)", 0.0078125,   p.getGain2(), 0.001);
    assertEquals("getGain(0.0078125)",  0.0078125*2, p.getGain(),  0.001);
    assertEquals("setGain(0.0078125)",  0x00010001,  getOneWordFieldValue(p, GAIN));

    p.setGain(-0.0078125f, -0.0078125f);
    assertEquals("getGain1(-0.0078125)", -0.0078125,   p.getGain1(), 0.001);
    assertEquals("getGain2(-0.0078125)", -0.0078125,   p.getGain2(), 0.001);
    assertEquals("getGain(-0.0078125)",  -0.0078125*2, p.getGain(),  0.001);
    assertEquals("setGain(-0.0078125)",  0xFFFFFFFF,   getOneWordFieldValue(p, GAIN));

    // Make sure gain values are in correct place (VRT-39) [Gain 1 in lower-order
    // bits and Gain 2 in the higher-order bits.]
    p.setGain(1.0f, 0.0078125f);
    assertEquals("getGain1()",  1.0,        p.getGain1(), 0.001);
    assertEquals("getGain2()",  0.0078125,  p.getGain2(), 0.001);
    assertEquals("getGain()",   1.0078125,  p.getGain(),  0.001);
    assertEquals("setGain(..)", 0x00010080, getOneWordFieldValue(p, GAIN));

    // Make sure setting individual gain values works
    p.setGain(-1.0f, -1.0f);
    p.setGain(1.0f);
    assertEquals("getGain1()",  1.0,        p.getGain1(), 0.001);
    assertEquals("getGain2()",  0.0,        p.getGain2(), 0.001);
    assertEquals("getGain()",   1.0,        p.getGain(),  0.001);
    assertEquals("setGain(..)", 0x00000080, getOneWordFieldValue(p, GAIN));

    p.setGain(-1.0f, -1.0f);
    p.setGain1(1.0f);
    assertEquals("getGain1()",  1.0,        p.getGain1(), 0.001);
    assertEquals("getGain2()", -1.0,        p.getGain2(), 0.001);
    assertEquals("getGain()",   0.0,        p.getGain(),  0.001);
    assertEquals("setGain(..)", 0xFF800080, getOneWordFieldValue(p, GAIN));

    p.setGain(-1.0f, -1.0f);
    p.setGain2(1.0f);
    assertEquals("getGain1()", -1.0,        p.getGain1(), 0.001);
    assertEquals("getGain2()",  1.0,        p.getGain2(), 0.001);
    assertEquals("getGain()",   0.0,        p.getGain(),  0.001);
    assertEquals("setGain(..)", 0x0080FF80, getOneWordFieldValue(p, GAIN));
  }

  public void testSetOverRangeCount () {
    ContextPacket p = newInstance(testedClass);

    p.setOverRangeCount(0L);
    assertEquals("getOverRangeCount(0)",0,p.getOverRangeCount());
    assertEquals("setOverRangeCount(0)",0,getOneWordFieldValue(p, OVER_RANGE));

    p.setOverRangeCount(0xFEDCBA98L);
    assertEquals("getOverRangeCount()",            0xFEDCBA98L, p.getOverRangeCount());
    assertEquals("setOverRangeCount(0xFEDCBA98L)", 0xFEDCBA98,  getOneWordFieldValue(p, OVER_RANGE));
  }

  public void testSetSampleRate () {
    ContextPacket p = newInstance(testedClass);

    p.setSampleRate(1.0);
    assertEquals("getSampleRate()",  1.0,                 p.getSampleRate(),0.00000095);
    assertEquals("setSampleRate(0)", 0x0000000000100000L, getTwoWordFieldValue(p, SAMPLE_RATE));

    p.setSampleRate(0.00000095);
    assertEquals("getSampleRate()",           0.00000095,          p.getSampleRate(),0.00000095);
    assertEquals("setSampleRate(0.00000095)", 0x0000000000000001L, getTwoWordFieldValue(p, SAMPLE_RATE));

    p.setSampleRate(15.5);
    assertEquals("getSampleRate()",     15.5,                p.getSampleRate(),0.00000095);
    //last 5 bytes are after the radixpoint
    assertEquals("setSampleRate(15.5)", 0x0000000000F80000L, getTwoWordFieldValue(p, SAMPLE_RATE));
  }

  public void testSetTimeStampAdjustment () {
    ContextPacket p = newInstance(testedClass);

    p.setTimeStampAdjustment(0L);
    assertEquals("getTimeStampAdjustment()",  0, p.getTimeStampAdjustment());
    assertEquals("setTimeStampAdjustment(0)", 0, getTwoWordFieldValue(p, TIME_ADJUST));

    p.setTimeStampAdjustment(0xFEDCBA9876543210L);
    assertEquals("getTimeStampAdjustment()",            0xFEDCBA9876543210L, p.getTimeStampAdjustment());
    assertEquals("setTimeStampAdjustment(0xFEDCBA98L)", 0xFEDCBA9876543210L, getTwoWordFieldValue(p, TIME_ADJUST));
  }

  public void testSetTimeStampCalibration () {
    ContextPacket p = newInstance(testedClass);

    p.setTimeStampCalibration(0);
    assertEquals("getTimeStampCalibration()",  0, p.getTimeStampCalibration());
    assertEquals("setTimeStampCalibration(0)", 0, getOneWordFieldValue(p, TIME_CALIB));

    p.setTimeStampCalibration(0xFEDCBA98);
    assertEquals("getTimeStampCalibration()",           0xFEDCBA98, p.getTimeStampCalibration());
    assertEquals("setTimeStampCalibration(0xFEDCBA98)", 0xFEDCBA98, getOneWordFieldValue(p, TIME_CALIB));
  }

  public void testSetTemperature () {
    ContextPacket p = newInstance(testedClass);

    p.setTemperature(0f);
    assertEquals("getTemperature()",          0f, p.getTemperature());
    assertEquals("setTemperature(0)", 0x00000000, getOneWordFieldValue(p, TEMPERATURE));

    p.setTemperature(1f);
    assertEquals("getTemperature()",          1f, p.getTemperature());
    assertEquals("setTemperature(1)", 0x00000040, getOneWordFieldValue(p, TEMPERATURE));

    p.setTemperature(0.015625f);
    assertEquals("getTemperature()",          0.015625f, p.getTemperature());
    assertEquals("setTemperature(0.015625)", 0x00000001, getOneWordFieldValue(p, TEMPERATURE));

    p.setTemperature(-1f);
    assertEquals("getTemperature()",          -1f, p.getTemperature(), 0.02);
    assertEquals("setTemperature(-1)", 0x0000FFC0, getOneWordFieldValue(p, TEMPERATURE));

    p.setTemperature(-0.015625f);
    assertEquals("getTemperature(-0.015625)", -0.015625f, p.getTemperature(), 0.02);
    assertEquals("setTemperature(-0.015625)", 0x0000FFFF, getOneWordFieldValue(p, TEMPERATURE));
  }

  public void testSetDeviceID () {
    ContextPacket p = newInstance(testedClass);

    p.setDeviceID("01-23-45:FEDC");
    assertEquals("getDeviceID(12345:FEDC)", "01-23-45:FEDC",      p.getDeviceID());
    assertEquals("setDeviceID(12345:1)",     0x000123450000FEDCL, getTwoWordFieldValue(p, DEVICE_ID));
  }

  public void testSetUserDefinedBits () {
    ContextPacket p = newInstance(testedClass);

    p.setUserDefinedBits((byte)0);
    assertEquals("getUserDefinedBits()", (byte)0, p.getUserDefinedBits());

    p.setUserDefinedBits((byte)0xAA);
    assertEquals("getUserDefinedBits()", (byte)0xAA, p.getUserDefinedBits());
    p.setDataPayloadFormat(null);
  }

  public void testSetDataPayloadFormat () {
    ContextPacket p = newInstance(testedClass);

    PayloadFormat pf = new PayloadFormat(false,RealComplexType.Real,DataItemFormat.SignedInt,true,5,15,64,30,0xF0F0,0x0F0F);
    p.setDataPayloadFormat(pf);
    assertEquals("setDataPayloadFormat(test 1)", 0x80DF0FDDF0EF0F0EL, getTwoWordFieldValue(p, DATA_FORMAT));

    pf = new PayloadFormat(true,RealComplexType.ComplexPolar,DataItemFormat.UnsignedVRT6,true,4,10,63,12,1,0x10000);
    p.setDataPayloadFormat(pf);
    assertEquals("setDataPayloadFormat(test 2)", 0x56CA0F8B0000FFFFL, getTwoWordFieldValue(p, DATA_FORMAT));
  }

  public void testSetGeolocation () {
    ContextPacket p = newInstance(testedClass);

    Geolocation gl = new Geolocation();
    gl.setTimeStamp(new TimeStamp(IntegerMode.GPS,0xFFBACD11,0xD23456789AL));
    gl.setLatitude(40.875);
    gl.setLongitude(0.0000002384185791015625);
    gl.setAltitude(-1.5);
    gl.setSpeedOverGround((double)0x7FFF + 0.0000152587890625);
    gl.setHeadingAngle(0x1FF + 0.75);
    gl.setTrackAngle(1.0);
    gl.setMagneticVariation(-0.875);

    p.setGeolocationGPS(gl);
    byte[] buffer = p.getPayload();
    assertEquals("setGeolocation GPS 1.01-02", 0x0A000000FFBACD11L, VRTMath.unpackLong(buffer,getByteOffset(buffer,GPS_EPHEM)+ 4, BIG_ENDIAN));
    assertEquals("setGeolocation GPS 1.03-04", 0x000000D23456789AL, VRTMath.unpackLong(buffer,getByteOffset(buffer,GPS_EPHEM)+12, BIG_ENDIAN));
    assertEquals("setGeolocation GPS 1.05-06", 0x0A38000000000001L, VRTMath.unpackLong(buffer,getByteOffset(buffer,GPS_EPHEM)+20, BIG_ENDIAN));
    assertEquals("setGeolocation GPS 1.07-08", 0xFFFFFFD07FFF0001L, VRTMath.unpackLong(buffer,getByteOffset(buffer,GPS_EPHEM)+28, BIG_ENDIAN));
    assertEquals("setGeolocation GPS 1.09-10", 0x7FF0000000400000L, VRTMath.unpackLong(buffer,getByteOffset(buffer,GPS_EPHEM)+36, BIG_ENDIAN));
    assertEquals("setGeolocation GPS 1.11"   ,         0xFFC80000,  VRTMath.unpackInt(buffer, getByteOffset(buffer,GPS_EPHEM)+44, BIG_ENDIAN));
    assertEquals("getGeolocation GPS 1",                        gl, p.getGeolocationGPS());

    p.setGeolocationINS(gl);
    buffer = p.getPayload();
    assertEquals("setGeolocation INS 1.01-02", 0x0A000000FFBACD11L, VRTMath.unpackLong(buffer,getByteOffset(buffer,INS_EPHEM)+ 4, BIG_ENDIAN));
    assertEquals("setGeolocation INS 1.03-04", 0x000000D23456789AL, VRTMath.unpackLong(buffer,getByteOffset(buffer,INS_EPHEM)+12, BIG_ENDIAN));
    assertEquals("setGeolocation INS 1.05-06", 0x0A38000000000001L, VRTMath.unpackLong(buffer,getByteOffset(buffer,INS_EPHEM)+20, BIG_ENDIAN));
    assertEquals("setGeolocation INS 1.07-08", 0xFFFFFFD07FFF0001L, VRTMath.unpackLong(buffer,getByteOffset(buffer,INS_EPHEM)+28, BIG_ENDIAN));
    assertEquals("setGeolocation INS 1.09-10", 0x7FF0000000400000L, VRTMath.unpackLong(buffer,getByteOffset(buffer,INS_EPHEM)+36, BIG_ENDIAN));
    assertEquals("setGeolocation INS 1.11"   ,         0xFFC80000,  VRTMath.unpackInt(buffer, getByteOffset(buffer,INS_EPHEM)+44, BIG_ENDIAN));
    assertEquals("getGeolocation INS 1",                       gl,  p.getGeolocationGPS());
  }

  public void testSetEphemerisECEF () {
    ContextPacket p = newInstance(testedClass);

    Ephemeris eph = new Ephemeris();
    eph.setTimeStamp(new TimeStamp(IntegerMode.GPS,0xFFBACD11,0xD23456789AL));
    eph.setPositionX(0.5);
    eph.setPositionY(0.03125);
    eph.setPositionZ(-0.5);
    eph.setAttitudeAlpha(0x1FF + 0.75);
    eph.setAttitudeBeta(1.0);
    eph.setAttitudePhi(-2.0);
    eph.setVelocityX(1.0);
    eph.setVelocityY(-0.5);
    eph.setVelocityZ(0x7FFE + 0.0);

    p.setEphemerisECEF(eph);
    byte[] buffer = p.getPayload();
    assertEquals("setEphemerisECEF 1.01-02", 0x0A000000FFBACD11L, VRTMath.unpackLong(buffer,getByteOffset(buffer,ECEF_EPHEM)+ 4, BIG_ENDIAN));
    assertEquals("setEphemerisECEF 1.03-04", 0x000000D23456789AL, VRTMath.unpackLong(buffer,getByteOffset(buffer,ECEF_EPHEM)+12, BIG_ENDIAN));
    assertEquals("setEphemerisECEF 1.05-06", 0x0000001000000001L, VRTMath.unpackLong(buffer,getByteOffset(buffer,ECEF_EPHEM)+20, BIG_ENDIAN));
    assertEquals("setEphemerisECEF 1.07-08", 0xFFFFFFF07FF00000L, VRTMath.unpackLong(buffer,getByteOffset(buffer,ECEF_EPHEM)+28, BIG_ENDIAN));
    assertEquals("setEphemerisECEF 1.09-10", 0x00400000FF800000L, VRTMath.unpackLong(buffer,getByteOffset(buffer,ECEF_EPHEM)+36, BIG_ENDIAN));
    assertEquals("setEphemerisECEF 1.11-12", 0x00010000FFFF8000L, VRTMath.unpackLong(buffer,getByteOffset(buffer,ECEF_EPHEM)+44, BIG_ENDIAN));
    assertEquals("setEphemerisECEF 1.13"   ,         0x7FFE0000,  VRTMath.unpackInt(buffer,getByteOffset(buffer,ECEF_EPHEM)+52, BIG_ENDIAN));
    assertEquals("getEphemerisECEF 1",                      eph,  p.getEphemerisECEF());

    p.setEphemerisRelative(eph);
    buffer = p.getPayload();
    assertEquals("setEphemerisRelative 1.01-02", 0x0A000000FFBACD11L, VRTMath.unpackLong(buffer,getByteOffset(buffer,REL_EPHEM)+ 4, BIG_ENDIAN));
    assertEquals("setEphemerisRelative 1.03-04", 0x000000D23456789AL, VRTMath.unpackLong(buffer,getByteOffset(buffer,REL_EPHEM)+12, BIG_ENDIAN));
    assertEquals("setEphemerisRelative 1.05-06", 0x0000001000000001L, VRTMath.unpackLong(buffer,getByteOffset(buffer,REL_EPHEM)+20, BIG_ENDIAN));
    assertEquals("setEphemerisRelative 1.07-08", 0xFFFFFFF07FF00000L, VRTMath.unpackLong(buffer,getByteOffset(buffer,REL_EPHEM)+28, BIG_ENDIAN));
    assertEquals("setEphemerisRelative 1.09-10", 0x00400000FF800000L, VRTMath.unpackLong(buffer,getByteOffset(buffer,REL_EPHEM)+36, BIG_ENDIAN));
    assertEquals("setEphemerisRelative 1.11-12", 0x00010000FFFF8000L, VRTMath.unpackLong(buffer,getByteOffset(buffer,REL_EPHEM)+44, BIG_ENDIAN));
    assertEquals("setEphemerisRelative 1.13"   ,         0x7FFE0000,  VRTMath.unpackInt(buffer,getByteOffset(buffer,REL_EPHEM)+52, BIG_ENDIAN));
    assertEquals("setEphemerisRelative 1",                       eph, p.getEphemerisRelative());
  }

  public void testSetEphemerisReference () {
    ContextPacket p = newInstance(testedClass);

    p.setEphemerisReference(0);
    assertEquals("getEphemerisReference()",  0, p.getEphemerisReference());
    assertEquals("setEphemerisReference(0)", 0, getOneWordFieldValue(p, EPHEM_REF));

    p.setEphemerisReference(0x7EDCBA98);
    assertEquals("getEphemerisReference()",           0x7EDCBA98, p.getEphemerisReference());
    assertEquals("setEphemerisReference(0x7EDCBA98)", 0x7EDCBA98, getOneWordFieldValue(p, EPHEM_REF));
  }

  public void testSetGeoSentences () {
    ContextPacket p = newInstance(testedClass);

    GeoSentences gs = new GeoSentences();
    gs.setSentences("test string value");
    gs.setManufacturerIdentifier(0xFFFFFF);

    p.setGeoSentences(gs);
    byte[] buffer = p.getPayload();
    int    off    = getByteOffset(buffer,GPS_ASCII)+12;
    String exp    = "test string value\0\0";
    String act    = new String(buffer, off, exp.length());

    assertEquals("setGeoSentences", exp, act);
    assertEquals("getGeoSentences", gs,  p.getGeoSentences());
  }

  public void testSetContextAssocLists () {
    ContextPacket p = newInstance(testedClass);

    int[] hdr = { 0x00040004, 0x00148002 };
    int[] src = { 0,1,2,3 };
    int[] sys = { 0xABCDEF01, 0x12345678, 0xFFFFFFFF, 0x0C0A0D0A };
    int[] vec = { 5,4,3,2,1,0,6,7,8,9,10,11,12,14,15,16,17,18,19,20 };
    int[] asy = { 0xEFEFEFEF, 0xACACACAC };
    int[] tag = { 0x01010101, 0xCACACACA };

    ContextAssocLists cal = new ContextAssocLists();
    cal.setSourceContext(src);
    cal.setSystemContext(sys);
    cal.setVectorComponent(vec);
    cal.setAsynchronousChannel(asy, tag);
    cal.setAsynchronousChannelTagsPresent(true);
    p.setContextAssocLists(cal);

    byte[] buffer = p.getPayload();
    buffer[3] = (byte)(buffer[3] | 0x80);
    int off = getByteOffset(buffer,CONTEXT_ASOC)+4;

    for (int i = 0; i < hdr.length; i++,off+=4) assertEquals("setContextAssocLists(..) hdr", hdr[i], VRTMath.unpackInt(buffer, off, BIG_ENDIAN));
    for (int i = 0; i < src.length; i++,off+=4) assertEquals("setContextAssocLists(..) src", src[i], VRTMath.unpackInt(buffer, off, BIG_ENDIAN));
    for (int i = 0; i < sys.length; i++,off+=4) assertEquals("setContextAssocLists(..) sys", sys[i], VRTMath.unpackInt(buffer, off, BIG_ENDIAN));
    for (int i = 0; i < vec.length; i++,off+=4) assertEquals("setContextAssocLists(..) vec", vec[i], VRTMath.unpackInt(buffer, off, BIG_ENDIAN));
    for (int i = 0; i < asy.length; i++,off+=4) assertEquals("setContextAssocLists(..) asy", asy[i], VRTMath.unpackInt(buffer, off, BIG_ENDIAN));
    for (int i = 0; i < tag.length; i++,off+=4) assertEquals("setContextAssocLists(..) tag", tag[i], VRTMath.unpackInt(buffer, off, BIG_ENDIAN));
  }

  public void testSetPacketType () {
    ContextPacket p = newInstance(testedClass);
    byte[] buffer;
    buffer = p.getPacket();
    assertEquals("getPacketType()", 4, (buffer[0] >> 4)); // Should default to Context

    p.setPacketType(PacketType.Context); // Setting to Context also valid
    buffer = p.getPacket();
    assertEquals("setPacketType(Context)", 4, (buffer[0] >> 4));
  }

  public void testGetAdjustedTimestamp () {
    ContextPacket p  = newInstance(testedClass);
    TimeStamp     ts = new TimeStamp(IntegerMode.UTC,0x12345678,0);
    TimeStamp     t  = new TimeStamp(IntegerMode.UTC,0,0);

    p.setTimeStampAdjustment(0xFFFFFFFFFL);
    p.setTimeStamp(ts);
    assertEquals("getAdjustedTimestamp", ts.addPicoSeconds(0xFFFFFFFFFL), p.getAdjustedTimeStamp());

    p.setTimeStampAdjustment(0xABCDEF00L);
    assertEquals("getAdjustedTimestamp", ts.addPicoSeconds(0xABCDEF00L), p.getAdjustedTimeStamp());

    p.setTimeStampAdjustment(0L);
    p.setTimeStamp(t);
    assertEquals("getAdjustedTimestamp", t, p.getAdjustedTimeStamp());
  }


  //////////////////////////////////////////////////////////////////////////////
  // INTERNAL METHODS
  //////////////////////////////////////////////////////////////////////////////

  /** Reads a 32-bit word from the packet's payload for the given field. */
  private static int getOneWordFieldValue (ContextPacket p, long field) {
    byte[] buffer = p.getPayload();
    return VRTMath.unpackInt(buffer, getByteOffset(buffer,field)+4, BIG_ENDIAN);
  }

  /** Reads a 64-bit double-word from the packet's payload for the given field. */
  private static long getTwoWordFieldValue (ContextPacket p, long field) {
    byte[] buffer = p.getPayload();
    return VRTMath.unpackLong(buffer, getByteOffset(buffer,field)+4, BIG_ENDIAN);
  }

  /** Computes the offset for the given field. */
  static int getByteOffset (byte[] buffer, long field) {
    int indicator = VRTMath.unpackInt(buffer, 0, BIG_ENDIAN);
    if ((field & indicator) == 0) {      //the field is not set
      return -1;
    }
    int i;
    int ret = 0;
    int off;
    for (i = 0; (field & 1) == 0; i++) {
      field = field >> 1;
    }
    for (int j = 1; i+j < 32;j++){
      if (((indicator >> i+j) & 1) == 1) {
        switch(1 << i+j) {
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
            off = getByteOffset(buffer,GPS_ASCII);
            ret += 4*(2 + VRTMath.unpackInt(buffer, off+4, BIG_ENDIAN));     //2 + X words
            break;
          case CONTEXT_ASOC:
            off = getByteOffset(buffer,CONTEXT_ASOC);
            ret += 8;
            ret += 4*((buffer[off+4] << 8) + buffer[off+5]); //source list size
            ret += 4*((buffer[off+6] << 8) + buffer[off+7]); //system list size
            ret += 4*((buffer[off+8] << 8) + buffer[off+9]); //vector-component list size
            ret += (((buffer[off+10] & 0x80) == 0) ? 4 : 8)*(((buffer[off+10] & 0x7F) << 8) + buffer[off+11]& 0xFF);
            break;
        }
      }
    }
    return ret;
  }


  private static final int        FIELD_OFFSET = 3;
  private static final String[]   FIELD_NAMES  = {
    "ChangePacket",       "ReferencePointIdentifier", "Bandwidth",           "FrequencyIF",
    "FrequencyRF",        "FrequencyOffsetRF",        "BandOffsetIF",        "ReferenceLevel",
    "Gain",               "Gain1",                    "Gain2",               "OverRangeCount",
    "SampleRate",         "SamplePeriod",             "TimeStampAdjustment", "TimeStampCalibration",
    "Temperature",        "DeviceID",                 "CalibratedTimeStamp", "DataValid",
    "ReferenceLocked",    "AGC",                      "SignalDetected",      "InvertedSpectrum",
    "OverRange",          "Discontinuous",            "UserDefinedBits",     "DataPayloadFormat",
    "GeolocationGPS",     "GeolocationINS",           "EphemerisECEF",       "EphemerisRelative",
    "EphemerisReference", "GeoSentences",             "ContextAssocLists"
  };
  private static final Class<?>[] FIELD_TYPES  = {
    Boolean.TYPE,      Integer.class,      Double.class,            Double.class,
    Double.class,      Double.class,       Double.class,            Float.class,
    Float.class,       Float.class,        Float.class,             Long.class,
    Double.class,      Double.class,       Long.class,              Integer.class,
    Float.class,       String.class,       Boolean.class,           Boolean.class,
    Boolean.class,     Boolean.class,      Boolean.class,           Boolean.class,
    Boolean.class,     Boolean.class,      Byte.class,              PayloadFormat.class,
    Geolocation.class, Geolocation.class,  Ephemeris.class,         Ephemeris.class,
    Integer.class,     GeoSentences.class, ContextAssocLists.class
  };

  public void testGetFieldName () {
    HasFields hf = (HasFields)newInstance(testedClass);
    checkGetFieldName(hf, FIELD_OFFSET, FIELD_NAMES, FIELD_TYPES);
  }

  public void testGetField () {
    HasFields         hf   = (HasFields)newInstance(testedClass);
    TimeStamp         ts1  = TimeStamp.Y2K_GPS;
    TimeStamp         ts2  = TimeStamp.getSystemTime();
    Geolocation       g1   = new Geolocation();
    Geolocation       g2   = new Geolocation();
    Ephemeris         e1   = new Ephemeris();
    Ephemeris         e2   = new Ephemeris();
    GeoSentences      gs1  = new GeoSentences();
    GeoSentences      gs2  = new GeoSentences();
    ContextAssocLists cal1 = new ContextAssocLists();
    ContextAssocLists cal2 = new ContextAssocLists();

    g1.setLatitude(11.1); g1.setLongitude(22.2); g1.setAltitude(1234.0);
    g2.setLatitude( 1.0); g2.setLongitude( 2.0); g2.setAltitude(5432.1);

    e1.setPositionX(1e9); e1.setPositionY(2e9); e1.setPositionZ(3e9);
    e2.setPositionX(1.0); e2.setPositionY(2.0); e2.setPositionZ(3.0);

    gs1.setSentences("Hello World!");
    gs2.setSentences("Nothing to see here!");

    cal1.setSourceContext(1, 2, 3, 4); cal1.setSystemContext(10, 20, 30);
    cal2.setSourceContext(4, 3, 2, 1); cal1.setSystemContext(11, 22, 33);

    checkGetField(hf, "ChangePacket",              true,                false);
    checkGetField(hf, "ReferencePointIdentifier",  1234,                4321);
    checkGetField(hf, "Bandwidth",                 1e6,                 32e3);
    checkGetField(hf, "FrequencyIF",               0.0,                 1e3);
    checkGetField(hf, "FrequencyRF",               1e9,                 100e6);
    checkGetField(hf, "FrequencyOffsetRF",         1e9,                 32e3);
    checkGetField(hf, "BandOffsetIF",              1e3,                 1e6);
    checkGetField(hf, "ReferenceLevel",            100f,                10.2f);
    checkGetField(hf, "Gain",                      3f,                  2.5f);
    checkGetField(hf, "Gain1",                     3f,                  0.5f);
    checkGetField(hf, "Gain2",                     3f,                  1.5f);
    checkGetField(hf, "OverRangeCount",            0L,                  1234L);
    checkGetField(hf, "SampleRate",                1e+8,                1e+9);
    checkGetField(hf, "SamplePeriod",              1e-8,                1e-9);
    checkGetField(hf, "TimeStampAdjustment",       0L,                  1234567890L);
    checkGetField(hf, "TimeStampCalibration",      ts1.getSecondsGPS(), ts2.getSecondsGPS());
    checkGetField(hf, "Temperature",               32.0f,               27.7f);
    checkGetField(hf, "DeviceID",                  "12-34-56:ABCD",     "AB-CD-EF:1234");
    checkGetField(hf, "CalibratedTimeStamp",       TRUE,                FALSE);
    checkGetField(hf, "DataValid",                 TRUE,                FALSE);
    checkGetField(hf, "ReferenceLocked",           TRUE,                FALSE);
    checkGetField(hf, "AGC",                       TRUE,                FALSE, "isAutomaticGainControl", "setAutomaticGainControl");
    checkGetField(hf, "SignalDetected",            TRUE,                FALSE);
    checkGetField(hf, "InvertedSpectrum",          TRUE,                FALSE);
    checkGetField(hf, "OverRange",                 TRUE,                FALSE);
    checkGetField(hf, "Discontinuous",             TRUE,                FALSE);
    checkGetField(hf, "UserDefinedBits",           (byte)7,             (byte)42);
    checkGetField(hf, "DataPayloadFormat",         VRTPacket.INT32,     VRTPacket.DOUBLE64);
    checkGetField(hf, "GeolocationGPS",            g1,                  g2);
    checkGetField(hf, "GeolocationINS",            g1,                  g2);
    checkGetField(hf, "EphemerisECEF",             e1,                  e2);
    checkGetField(hf, "EphemerisRelative",         e1,                  e2);
    checkGetField(hf, "EphemerisReference",        1234,                4321);
    checkGetField(hf, "GeoSentences",              gs1,                 gs2);
    checkGetField(hf, "ContextAssocLists",         cal1,                cal2);
  }
}
