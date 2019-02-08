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
import nxm.vrt.lib.BasicContextPacket;
import nxm.vrt.lib.ContextPacket.Ephemeris;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.TimeStamp.IntegerMode;
import nxm.vrt.lib.VRTConfig;
import nxm.vrt.lib.VRTConfig.VITAVersion;
import nxm.vrt.lib.VRTMath;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static nxm.vrttest.inc.TestRunner.assertEquals;
import static nxm.vrttest.lib.ContextPacketTest.ECEF_EPHEM_ADJ;
import static nxm.vrttest.lib.ContextPacketTest.getByteOffset;

/** Test cases for the {@link Ephemeris} class.  */
public class EphemerisTest implements Testable {

  @Override
  public Class<?> getTestedClass () {
    return BasicContextPacket.Ephemeris.class;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    tests.add(new TestSet("getAccelerationX",                 this, "+testSetAccelerationX"));
    tests.add(new TestSet("getAccelerationY",                 this, "+testSetAccelerationY"));
    tests.add(new TestSet("getAccelerationZ",                 this, "+testSetAccelerationZ"));
    tests.add(new TestSet("getAdjunct",                       this, "-"));
    tests.add(new TestSet("getAttitudeAlpha",                 this, "+testSetAttitudeAlpha"));
    tests.add(new TestSet("getAttitudeBeta",                  this, "+testSetAttitudeBeta"));
    tests.add(new TestSet("getAttitudephi",                   this, "+testSetAttitudePhi"));
    tests.add(new TestSet("getPositionX",                     this, "+testSetPositionX"));
    tests.add(new TestSet("getPositionY",                     this, "+testSetPositionY"));
    tests.add(new TestSet("getPositionZ",                     this, "+testSetPositionZ"));
    tests.add(new TestSet("getRotationalAccelerationAlpha",   this, "+testSetRotationalAccelerationAlpha"));
    tests.add(new TestSet("getRotationalAccelerationBeta",    this, "+testSetRotationalAccelerationBeta"));
    tests.add(new TestSet("getRotationalAccelerationPhi",     this, "+testSetRotationalAccelerationPhi"));
    tests.add(new TestSet("getRotationalVelocityAlpha",       this, "+testSetRotationalVelocityAlpha"));
    tests.add(new TestSet("getRotationalVelocityBeta",        this, "+testSetRotationalVelocityBeta"));
    tests.add(new TestSet("getRotationalVelocityPhi",         this, "+testSetRotationalVelocityPhi"));
    tests.add(new TestSet("getVelocityX",                     this, "+testSetVelocityX"));
    tests.add(new TestSet("getVelocityY",                     this, "+testSetVelocityY"));
    tests.add(new TestSet("getVelocityZ",                     this, "+testSetVelocityZ"));
    if (VRTConfig.VRT_VERSION == VITAVersion.V49b) {
      tests.add(new TestSet("setAccelerationX",               this, "testSetAccelerationX"));
      tests.add(new TestSet("setAccelerationY",               this, "testSetAccelerationY"));
      tests.add(new TestSet("setAccelerationZ",               this, "testSetAccelerationZ"));
    }
    else {
      tests.add(new TestSet("setAccelerationX",               this, "-testSetAccelerationX"));
      tests.add(new TestSet("setAccelerationY",               this, "-testSetAccelerationY"));
      tests.add(new TestSet("setAccelerationZ",               this, "-testSetAccelerationZ"));
    }
    tests.add(new TestSet("setAttitudeAlpha",                 this, "testSetAttitudeAlpha"));
    tests.add(new TestSet("setAttitudeBeta",                  this, "testSetAttitudeBeta"));
    tests.add(new TestSet("setAttitudePhi",                   this, "testSetAttitudePhi"));
    tests.add(new TestSet("setPositionX",                     this, "testSetPositionX"));
    tests.add(new TestSet("setPositionY",                     this, "testSetPositionY"));
    tests.add(new TestSet("setPositionZ",                     this, "testSetPositionZ"));
    if (VRTConfig.VRT_VERSION == VITAVersion.V49b) {
      tests.add(new TestSet("setRotationalAccelerationAlpha", this, "testSetRotationalAccelerationAlpha"));
      tests.add(new TestSet("setRotationalAccelerationBeta",  this, "testSetRotationalAccelerationBeta"));
      tests.add(new TestSet("setRotationalAccelerationPhi",   this, "testSetRotationalAccelerationPhi"));
      tests.add(new TestSet("setRotationalVelocityAlpha",     this, "testSetRotationalVelocityAlpha"));
      tests.add(new TestSet("setRotationalVelocityBeta",      this, "testSetRotationalVelocityBeta"));
      tests.add(new TestSet("setRotationalVelocityPhi",       this, "testSetRotationalVelocityPhi"));
    }
    else {
      tests.add(new TestSet("setRotationalAccelerationAlpha", this, "-testSetRotationalAccelerationAlpha"));
      tests.add(new TestSet("setRotationalAccelerationBeta",  this, "-testSetRotationalAccelerationBeta"));
      tests.add(new TestSet("setRotationalAccelerationPhi",   this, "-testSetRotationalAccelerationPhi"));
      tests.add(new TestSet("setRotationalVelocityAlpha",     this, "-testSetRotationalVelocityAlpha"));
      tests.add(new TestSet("setRotationalVelocityBeta",      this, "-testSetRotationalVelocityBeta"));
      tests.add(new TestSet("setRotationalVelocityPhi",       this, "-testSetRotationalVelocityPhi"));
    }
    tests.add(new TestSet("setTimeStamp",                     this, "testSetTimeStamp"));
    tests.add(new TestSet("setVelocityX",                     this, "testSetVelocityX"));
    tests.add(new TestSet("setVelocityY",                     this, "testSetVelocityY"));
    tests.add(new TestSet("setVelocityZ",                     this, "testSetVelocityZ"));

    return tests;
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }

  public void testSetAttitudeAlpha () {
    byte buffer[] = new byte[64];
    Ephemeris ep = new Ephemeris();
    ep.setAttitudeAlpha(1.5);
    ep.readBytes(buffer, 0);
    assertEquals("setAttitudeAlpha 1.5",0x00600000, VRTMath.unpackInt(buffer, 28));
    assertEquals("getAttitudeAlpha 1.5",1.5, ep.getAttitudeAlpha());

    ep.setAttitudeAlpha(1.0000002384185791015625);
    ep.readBytes(buffer, 0);
    assertEquals("setAttitudeAlpha 1.0000002384185791015625", 0x00400001,               VRTMath.unpackInt(buffer, 28));
    assertEquals("getAttitudeAlpha 1.0000002384185791015625", 1.0000002384185791015625, ep.getAttitudeAlpha());

    ep.setAttitudeAlpha(-0.0000002384185791015625);
    ep.readBytes(buffer, 0);
    assertEquals("setAttitudeAlpha -0.0000002384185791015625", 0xFFFFFFFF,                VRTMath.unpackInt(buffer, 28));
    assertEquals("getAttitudeAlpha -0.0000002384185791015625", -0.0000002384185791015625, ep.getAttitudeAlpha());

    ep.setAttitudeAlpha(0xFFFF + 0.0);
    ep.readBytes(buffer, 0);
    assertEquals("setAttitudeAlpha 7FFFFFFF error", 0x7FFFFFFF, VRTMath.unpackInt(buffer, 28));
    assertEquals("getAttitudeAlpha 7FFFFFFF error", null,       ep.getAttitudeAlpha());
  }

  public void testSetAttitudeBeta () {
    byte buffer[] = new byte[64];
    Ephemeris ep = new Ephemeris();
    ep.setAttitudeBeta(1.5);
    ep.readBytes(buffer, 0);
    assertEquals("setAttitudeBeta 1.5", 0x00600000, VRTMath.unpackInt(buffer, 32));
    assertEquals("getAttitudeBeta 1.5", 1.5,        ep.getAttitudeBeta());

    ep.setAttitudeBeta(1.0000002384185791015625);
    ep.readBytes(buffer, 0);
    assertEquals("setAttitudeBeta 1.0000002384185791015625", 0x00400001,               VRTMath.unpackInt(buffer, 32));
    assertEquals("getAttitudeBeta 1.0000002384185791015625", 1.0000002384185791015625, ep.getAttitudeBeta());

    ep.setAttitudeBeta(-0.0000002384185791015625);
    ep.readBytes(buffer, 0);
    assertEquals("setAttitudeBeta -0.0000002384185791015625", 0xFFFFFFFF,                VRTMath.unpackInt(buffer, 32));
    assertEquals("getAttitudeBeta -0.0000002384185791015625", -0.0000002384185791015625, ep.getAttitudeBeta());

    ep.setAttitudeBeta(0xFFFF + 0.0);
    ep.readBytes(buffer, 0);
    assertEquals("setAttitudeBeta 7FFFFFFF error", 0x7FFFFFFF, VRTMath.unpackInt(buffer, 32));
    assertEquals("getAttitudeBeta 7FFFFFFF error", null,       ep.getAttitudeBeta());
  }

  public void testSetAttitudePhi () {
    byte buffer[] = new byte[64];
    BasicContextPacket bc = new BasicContextPacket();
    Ephemeris ep = new Ephemeris();
    ep.setAttitudePhi(1.5);

    ep.readBytes(buffer, 0);
    assertEquals("setAttitudePhi 1.5", 0x00600000, VRTMath.unpackInt(buffer, 36));
    assertEquals("getAttitudePhi 1.5", 1.5,        ep.getAttitudePhi());
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();

    ep.setAttitudePhi(1.0000002384185791015625);
    ep.readBytes(buffer, 0);
    assertEquals("setAttitudePhi 1.0000002384185791015625", 0x00400001,               VRTMath.unpackInt(buffer, 36));
    assertEquals("getAttitudePhi 1.0000002384185791015625", 1.0000002384185791015625, ep.getAttitudePhi());
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();

    ep.setAttitudePhi(-0.0000002384185791015625);
    ep.readBytes(buffer, 0);
    assertEquals("setAttitudePhi -0.0000002384185791015625", 0xFFFFFFFF,                VRTMath.unpackInt(buffer, 36));
    assertEquals("getAttitudePhi -0.0000002384185791015625", -0.0000002384185791015625, ep.getAttitudePhi());
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();

    ep.setAttitudePhi(0xFFFF + 0.0);
    ep.readBytes(buffer, 0);
    assertEquals("setAttitudePhi 7FFFFFFF error", 0x7FFFFFFF, VRTMath.unpackInt(buffer, 36));
    assertEquals("getAttitudePhi 7FFFFFFF error", null,       ep.getAttitudePhi());
  }

  public void testSetAccelerationX () {
    byte buffer[];
    BasicContextPacket bc = new BasicContextPacket();
    Ephemeris ep = new Ephemeris();
    ep.setAccelerationY(128 - 0.500000059604644775390625);
    bc.setEphemerisReference(0xaaaaaaaa);
    ep.setAccelerationX(1.5);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setAccelerationX 1.5", 0x01800000, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+16));
    assertEquals("getAccelerationX 1.5", 1.5,        ep.getAccelerationX());

    ep.setAccelerationX(0.000000059604644775390625);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setAccelerationX 0.000000059604644775390625", 0x00000001,                 VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+16));
    assertEquals("getAccelerationX 0.000000059604644775390625", 0.000000059604644775390625, ep.getAccelerationX());

    ep.setAccelerationX(-0.000000059604644775390625);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setAccelerationX -0.000000059604644775390625", 0xFFFFFFFF,                  VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+16));
    assertEquals("setAccelerationX -0.000000059604644775390625", -0.000000059604644775390625, ep.getAccelerationX());

    ep.setAccelerationX(128.0);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setAccelerationX 7FFFFFFF error", 0x7FFFFFFF, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+16));
    assertEquals("setAccelerationX 7FFFFFFF error", null,       ep.getAccelerationX());
  }

  public void testSetAccelerationY () {
    byte buffer[];
    BasicContextPacket bc = new BasicContextPacket();
    Ephemeris ep = new Ephemeris();
    ep.setAccelerationX(128 - 0.500000059604644775390625);
    bc.setEphemerisReference(0xaaaaaaaa);
    ep.setAccelerationY(1.5);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setAccelerationY 1.5", 0x01800000, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+20));
    assertEquals("getAccelerationY 1.5", 1.5,        ep.getAccelerationY());

    ep.setAccelerationY(0.000000059604644775390625);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setAccelerationY 0.000000059604644775390625", 0x00000001,                 VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+20));
    assertEquals("getAccelerationY 0.000000059604644775390625", 0.000000059604644775390625, ep.getAccelerationY());

    ep.setAccelerationY(-0.000000059604644775390625);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setAccelerationY -0.000000059604644775390625", 0xFFFFFFFF,                  VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+20));
    assertEquals("getAccelerationY -0.000000059604644775390625", -0.000000059604644775390625, ep.getAccelerationY());

    ep.setAccelerationY(128.0);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setAccelerationY 7FFFFFFF error", 0x7FFFFFFF, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+20));
    assertEquals("getAccelerationY 7FFFFFFF error", null,       ep.getAccelerationY());
  }

  public void testSetAccelerationZ () {
    byte buffer[];
    BasicContextPacket bc = new BasicContextPacket();
    Ephemeris ep = new Ephemeris();
    ep.setAccelerationX(128 - 0.500000059604644775390625);
    bc.setEphemerisReference(0xaaaaaaaa);
    ep.setAccelerationZ(1.5);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setAccelerationZ 1.5", 0x01800000, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+24));
    assertEquals("getAccelerationZ 1.5", 1.5,        ep.getAccelerationZ());

    ep.setAccelerationZ(0.000000059604644775390625);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setAccelerationZ 0.000000059604644775390625", 0x00000001,                 VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+24));
    assertEquals("getAccelerationZ 0.000000059604644775390625", 0.000000059604644775390625, ep.getAccelerationZ());

    ep.setAccelerationZ(-0.000000059604644775390625);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setAccelerationZ -0.000000059604644775390625", 0xFFFFFFFF,                  VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+24));
    assertEquals("getAccelerationZ -0.000000059604644775390625", -0.000000059604644775390625, ep.getAccelerationZ());

    ep.setAccelerationZ(128.0);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setAccelerationZ 7FFFFFFF error", 0x7FFFFFFF, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+24));
    assertEquals("getAccelerationZ 7FFFFFFF error", null,       ep.getAccelerationZ());
  }

  public void testSetRotationalVelocityAlpha () {
    byte buffer[];
    BasicContextPacket bc = new BasicContextPacket();
    Ephemeris ep = new Ephemeris();
    ep.setAccelerationX(128 - 0.500000059604644775390625);
    bc.setEphemerisReference(0xaaaaaaaa);
    ep.setRotationalVelocityAlpha(1.5);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalVelocityAlpha 1.5", 0x00018000, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+4));
    assertEquals("getRotationalVelocityAlpha 1.5", 1.5,        ep.getRotationalVelocityAlpha());

    ep.setRotationalVelocityAlpha(0.0000152587890625);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalVelocityAlpha 0.0000152587890625", 0x00000001,         VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+4));
    assertEquals("getRotationalVelocityAlpha 0.0000152587890625", 0.0000152587890625, ep.getRotationalVelocityAlpha());

    ep.setRotationalVelocityAlpha(-0.0000152587890625);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalVelocityAlpha -0.0000152587890625", 0xFFFFFFFF,          VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+4));
    assertEquals("getRotationalVelocityAlpha -0.0000152587890625", -0.0000152587890625, ep.getRotationalVelocityAlpha());

    ep.setRotationalVelocityAlpha(0x8000 + 0.0);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalVelocityAlpha 7FFFFFFF error", 0x7FFFFFFF, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+4));
    assertEquals("getRotationalVelocityAlpha 7FFFFFFF error", null,       ep.getAccelerationZ());
  }

  public void testSetRotationalVelocityBeta () {
    byte buffer[];
    BasicContextPacket bc = new BasicContextPacket();
    Ephemeris ep = new Ephemeris();
    ep.setAccelerationX(128 - 0.500000059604644775390625);
    bc.setEphemerisReference(0xaaaaaaaa);
    ep.setRotationalVelocityBeta(1.5);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalVelocityBeta 1.5", 0x00018000, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+8));
    assertEquals("getRotationalVelocityBeta 1.5", 1.5,        ep.getRotationalVelocityBeta());

    ep.setRotationalVelocityBeta(0.0000152587890625);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalVelocityBeta 0.0000152587890625", 0x00000001,         VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+8));
    assertEquals("getRotationalVelocityBeta 0.0000152587890625", 0.0000152587890625, ep.getRotationalVelocityBeta());

    ep.setRotationalVelocityBeta(-0.0000152587890625);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalVelocityBeta -0.0000152587890625", 0xFFFFFFFF,          VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+8));
    assertEquals("getRotationalVelocityBeta -0.0000152587890625", -0.0000152587890625, ep.getRotationalVelocityBeta());

    ep.setRotationalVelocityBeta(0x8000 + 0.0);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalVelocityBeta 7FFFFFFF error", 0x7FFFFFFF, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+8));
    assertEquals("getRotationalVelocityBeta 7FFFFFFF error", null,       ep.getRotationalVelocityBeta());
  }

  public void testSetRotationalVelocityPhi () {
    byte buffer[];
    BasicContextPacket bc = new BasicContextPacket();
    Ephemeris ep = new Ephemeris();
    ep.setAccelerationX(128 - 0.500000059604644775390625);
    bc.setEphemerisReference(0xaaaaaaaa);
    ep.setRotationalVelocityPhi(1.5);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalVelocityPhi 1.5", 0x00018000, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+12));
    assertEquals("getRotationalVelocityPhi 1.5", 1.5,        ep.getRotationalVelocityPhi());

    ep.setRotationalVelocityPhi(0.0000152587890625);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalVelocityPhi 0.0000152587890625", 0x00000001,         VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+12));
    assertEquals("getRotationalVelocityPhi 0.0000152587890625", 0.0000152587890625, ep.getRotationalVelocityPhi());

    ep.setRotationalVelocityPhi(-0.0000152587890625);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalVelocityPhi -0.0000152587890625", 0xFFFFFFFF,          VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+12));
    assertEquals("getRotationalVelocityPhi -0.0000152587890625", -0.0000152587890625, ep.getRotationalVelocityPhi());

    ep.setRotationalVelocityPhi(0x8000 + 0.0);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalVelocityPhi 7FFFFFFF error", 0x7FFFFFFF, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+12));
    assertEquals("getRotationalVelocityPhi 7FFFFFFF error", null,       ep.getRotationalVelocityPhi());
  }

  public void testSetRotationalAccelerationAlpha () {
    byte buffer[];
    BasicContextPacket bc = new BasicContextPacket();
    Ephemeris ep = new Ephemeris();
    ep.setAccelerationX(128 - 0.500000059604644775390625);
    bc.setEphemerisReference(0xaaaaaaaa);
    ep.setRotationalAccelerationAlpha(1.5);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalAccelerationAlpha 1.5", 0x00C00000, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+28));
    assertEquals("getRotationalAccelerationAlpha 1.5", 1.5,        ep.getRotationalAccelerationAlpha());

    ep.setRotationalAccelerationAlpha(0.00000011920928955078125);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalAccelerationAlpha 0.00000011920928955078125", 0x00000001,                VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+28));
    assertEquals("getRotationalAccelerationAlpha 0.00000011920928955078125", 0.00000011920928955078125, ep.getRotationalAccelerationAlpha());

    ep.setRotationalAccelerationAlpha(-0.00000011920928955078125);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalAccelerationAlpha -0.00000011920928955078125", 0xFFFFFFFF,                 VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+28));
    assertEquals("getRotationalAccelerationAlpha -0.00000011920928955078125", -0.00000011920928955078125, ep.getRotationalAccelerationAlpha());

    ep.setRotationalAccelerationAlpha(0x800 + 0.0);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalAccelerationAlpha 7FFFFFFF error", 0x7FFFFFFF, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+28));
    assertEquals("getRotationalAccelerationAlpha 7FFFFFFF error", null,       ep.getRotationalAccelerationAlpha());
  }

  public void testSetRotationalAccelerationBeta () {
    byte buffer[];
    BasicContextPacket bc = new BasicContextPacket();
    Ephemeris ep = new Ephemeris();
    ep.setAccelerationX(128 - 0.500000059604644775390625);
    bc.setEphemerisReference(0xaaaaaaaa);
    ep.setRotationalAccelerationBeta(1.5);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalAccelerationBeta 1.5", 0x00C00000, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+32));
    assertEquals("getRotationalAccelerationBeta 1.5", 1.5,        ep.getRotationalAccelerationBeta());

    ep.setRotationalAccelerationBeta(0.00000011920928955078125);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalAccelerationBeta 0.00000011920928955078125", 0x00000001,                VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+32));
    assertEquals("getRotationalAccelerationBeta 0.00000011920928955078125", 0.00000011920928955078125, ep.getRotationalAccelerationBeta());

    ep.setRotationalAccelerationBeta(-0.00000011920928955078125);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalAccelerationBeta -0.00000011920928955078125", 0xFFFFFFFF,                 VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+32));
    assertEquals("getRotationalAccelerationBeta -0.00000011920928955078125", -0.00000011920928955078125, ep.getRotationalAccelerationBeta());

    ep.setRotationalAccelerationBeta(0x800 + 0.0);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalAccelerationBeta 7FFFFFFF error", 0x7FFFFFFF, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+32));
    assertEquals("getRotationalAccelerationBeta 7FFFFFFF error", null,       ep.getRotationalAccelerationBeta());
  }

  public void testSetRotationalAccelerationPhi () {
    byte buffer[];
    BasicContextPacket bc = new BasicContextPacket();
    Ephemeris ep = new Ephemeris();
    ep.setAccelerationX(128 - 0.500000059604644775390625);
    bc.setEphemerisReference(0xaaaaaaaa);
    ep.setRotationalAccelerationPhi(1.5);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalAccelerationPhi 1.5", 0x00C00000, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+36));
    assertEquals("getRotationalAccelerationPhi 1.5", 1.5,        ep.getRotationalAccelerationPhi());

    ep.setRotationalAccelerationPhi(0.00000011920928955078125);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalAccelerationPhi 0.00000011920928955078125", 0x00000001,                VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+36));
    assertEquals("getRotationalAccelerationPhi 0.00000011920928955078125", 0.00000011920928955078125, ep.getRotationalAccelerationPhi());

    ep.setRotationalAccelerationPhi(-0.00000011920928955078125);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalAccelerationPhi -0.00000011920928955078125", 0xFFFFFFFF,                 VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+36));
    assertEquals("getRotationalAccelerationPhi -0.00000011920928955078125",- 0.00000011920928955078125, ep.getRotationalAccelerationPhi());

    ep.setRotationalAccelerationPhi(0x800 + 0.0);
    bc.setEphemerisECEF(ep);
    buffer = bc.getPayload();
    assertEquals("setRotationalAccelerationPhi 7FFFFFFF error", 0x7FFFFFFF, VRTMath.unpackInt(buffer, getByteOffset(buffer,ECEF_EPHEM_ADJ)+36));
    assertEquals("getRotationalAccelerationPhi 7FFFFFFF error", null,       ep.getRotationalAccelerationPhi());
  }


  public void testSetPositionX () {
    byte buffer[] = new byte[64];
    Ephemeris ep = new Ephemeris();

    ep.setPositionX(0xABC + 0.5);
    ep.readBytes(buffer, 0);
    assertEquals("setPositionX 0xABC + 0.5", 0x00015790,   VRTMath.unpackInt(buffer, 16));
    assertEquals("getPositionX 0xABC + 0.5", 0xABC + 0.5, ep.getPositionX());

    ep.setPositionX(0xF0F + 0.875);
    ep.readBytes(buffer, 0);
    assertEquals("setPositionX 0xF0F + 0.875", 0x0001E1FC,    VRTMath.unpackInt(buffer, 16));
    assertEquals("getPositionX 0xF0F + 0.875", 0xF0F + 0.875, ep.getPositionX());

    ep.setPositionX(-0.5);
    ep.readBytes(buffer, 0);

    assertEquals("setPositionX -0.5", 0xFFFFFFF0, VRTMath.unpackInt(buffer, 16));
    assertEquals("getPositionX -0.5", -0.5,       ep.getPositionX());
  }
  public void testSetPositionY () {
    byte buffer[] = new byte[64];
    Ephemeris ep = new Ephemeris();

    ep.setPositionY(0xABC + 0.5);
    ep.readBytes(buffer, 0);
    assertEquals("setPositionY 0xABC + 0.5", 0x00015790,  VRTMath.unpackInt(buffer, 20));
    assertEquals("getPositionY 0xABC + 0.5", 0xABC + 0.5, ep.getPositionY());

    ep.setPositionY(0xF0F + 0.875);
    ep.readBytes(buffer, 0);
    assertEquals("setPositionY 0xF0F + 0.875", 0x0001E1FC,    VRTMath.unpackInt(buffer, 20));
    assertEquals("getPositionY 0xF0F + 0.875", 0xF0F + 0.875, ep.getPositionY());

    ep.setPositionY(-0.5);
    ep.readBytes(buffer, 0);

    assertEquals("setPositionY -0.5", 0xFFFFFFF0, VRTMath.unpackInt(buffer, 20));
    assertEquals("getPositionY -0.5", -0.5,       ep.getPositionY());
  }
  public void testSetPositionZ () {
    byte buffer[] = new byte[64];
    Ephemeris ep = new Ephemeris();

    ep.setPositionZ(0xABC + 0.5);
    ep.readBytes(buffer, 0);
    assertEquals("setPositionZ 0xABC + 0.5", 0x00015790,  VRTMath.unpackInt(buffer, 24));
    assertEquals("getPositionZ 0xABC + 0.5", 0xABC + 0.5, ep.getPositionZ());

    ep.setPositionZ(0xF0F + 0.875);
    ep.readBytes(buffer, 0);
    assertEquals("setPositionZ 0xF0F + 0.875", 0x0001E1FC,    VRTMath.unpackInt(buffer, 24));
    assertEquals("getPositionZ 0xF0F + 0.875", 0xF0F + 0.875, ep.getPositionZ());

    ep.setPositionZ(-0.5);
    ep.readBytes(buffer, 0);

    assertEquals("setPositionZ -0.5", 0xFFFFFFF0, VRTMath.unpackInt(buffer, 24));
    assertEquals("getPositionZ -0.5", -0.5,       ep.getPositionZ());
  }

  public void testSetVelocityX () {
    byte buffer[] = new byte[64];
    Ephemeris ep = new Ephemeris();

    ep.setVelocityX(1.5);
    ep.readBytes(buffer, 0);
    assertEquals("setVelocityX 1.5", 0x00018000, VRTMath.unpackInt(buffer, 40));
    assertEquals("getVelocityX 1.5", 1.5,        ep.getVelocityX());

    ep.setVelocityX(1.0000152587890625);
    ep.readBytes(buffer, 0);
    assertEquals("setVelocityX 1.0000152587890625", 0x00010001,         VRTMath.unpackInt(buffer, 40));
    assertEquals("getVelocityX 1.0000152587890625", 1.0000152587890625, ep.getVelocityX());

    ep.setVelocityX(-0.0000152587890625);
    ep.readBytes(buffer, 0);
    assertEquals("setVelocityX -0.0000152587890625", 0xFFFFFFFF,          VRTMath.unpackInt(buffer, 40));
    assertEquals("getVelocityX -0.0000152587890625", -0.0000152587890625, ep.getVelocityX());

    ep.setVelocityX(0xFFFF + 0.0);
    ep.readBytes(buffer, 0);
    assertEquals("setVelocityX 7FFFFFFF error", 0x7FFFFFFF, VRTMath.unpackInt(buffer, 40));
    assertEquals("getVelocityX 7FFFFFFF error", null,       ep.getVelocityX());
  }

  public void testSetVelocityY () {
    byte buffer[] = new byte[64];
    Ephemeris ep = new Ephemeris();

    ep.setVelocityY(1.5);
    ep.readBytes(buffer, 0);
    assertEquals("setVelocityY 1.5", 0x00018000, VRTMath.unpackInt(buffer, 44));
    assertEquals("getVelocityY 1.5", 1.5,        ep.getVelocityY());

    ep.setVelocityY(1.0000152587890625);
    ep.readBytes(buffer, 0);
    assertEquals("setVelocityY 1.0000152587890625", 0x00010001,         VRTMath.unpackInt(buffer, 44));
    assertEquals("getVelocityY 1.0000152587890625", 1.0000152587890625, ep.getVelocityY());

    ep.setVelocityY(-0.0000152587890625);
    ep.readBytes(buffer, 0);
    assertEquals("setVelocityY -0.0000152587890625", 0xFFFFFFFF,          VRTMath.unpackInt(buffer, 44));
    assertEquals("getVelocityY -0.0000152587890625", -0.0000152587890625, ep.getVelocityY());

    ep.setVelocityY(0xFFFF + 0.0);
    ep.readBytes(buffer, 0);
    assertEquals("setVelocityY 7FFFFFFF error", 0x7FFFFFFF, VRTMath.unpackInt(buffer, 44));
    assertEquals("getVelocityY 7FFFFFFF error", null,       ep.getVelocityY());
  }

  public void testSetVelocityZ () {
    byte buffer[] = new byte[64];
    Ephemeris ep = new Ephemeris();

    ep.setVelocityZ(1.5);
    ep.readBytes(buffer, 0);
    assertEquals("setVelocityZ 1.5", 0x00018000, VRTMath.unpackInt(buffer, 48));
    assertEquals("getVelocityZ 1.5", 1.5,        ep.getVelocityZ());

    ep.setVelocityZ(1.0000152587890625);
    ep.readBytes(buffer, 0);
    assertEquals("setVelocityZ 1.0000152587890625", 0x00010001,         VRTMath.unpackInt(buffer, 48));
    assertEquals("getVelocityZ 1.0000152587890625", 1.0000152587890625, ep.getVelocityZ());

    ep.setVelocityZ(-0.0000152587890625);
    ep.readBytes(buffer, 0);
    assertEquals("setVelocityZ -0.0000152587890625", 0xFFFFFFFF,          VRTMath.unpackInt(buffer, 48));
    assertEquals("getVelocityZ -0.0000152587890625", -0.0000152587890625, ep.getVelocityZ());

    ep.setVelocityZ(0xFFFF + 0.0);
    ep.readBytes(buffer, 0);
    assertEquals("setVelocityZ 7FFFFFFF error", 0x7FFFFFFF, VRTMath.unpackInt(buffer, 48));
    assertEquals("getVelocityZ 7FFFFFFF error", null,       ep.getVelocityZ());
  }

  public void testSetTimeStamp () {
    byte buffer[] = new byte[64];
    Ephemeris ep = new Ephemeris();

    ep.setTimeStamp(new TimeStamp(IntegerMode.UTC,0xFEDCBA,0x123456789AL));
    ep.readBytes(buffer, 0);
    assertEquals("setTimeStamp TSI/TSF",         0x06000000, VRTMath.unpackInt(buffer, 0));
    assertEquals("setTimeStamp Integer",         0x00FEDCBA, VRTMath.unpackInt(buffer, 4));
    assertEquals("setTimeStamp Fractional high", 0x00000012, VRTMath.unpackInt(buffer, 8));
    assertEquals("setTimeStamp Fractional low",  0x3456789A, VRTMath.unpackInt(buffer, 12));
  }
}
