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
import nxm.vrt.lib.ContextPacket.Geolocation;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.TimeStamp.IntegerMode;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static nxm.vrttest.inc.TestRunner.assertEquals;

/** Test cases for the Geolocation class. */
public class GeolocationTest implements Testable {


  @Override
  public Class<?> getTestedClass () {
    return BasicContextPacket.Geolocation.class;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    tests.add(new TestSet("getAltitude",          this, "+testSetAltitude"));
    tests.add(new TestSet("getHeadingAngle",      this, "+testSetHeadingAngle"));
    tests.add(new TestSet("getLatitude",          this, "+testSetLatitude"));
    tests.add(new TestSet("getLongitude",         this, "+testSetLongitude"));
    tests.add(new TestSet("getMagneticVariation", this, "+testSetMagneticVariation"));
    tests.add(new TestSet("getSpeedOverGround",   this, "+testSetSpeedOverGround"));
    tests.add(new TestSet("getTrackAngle",        this, "+testSetTrackAngle"));
    tests.add(new TestSet("setAltitude",          this, "testSetAltitude"));
    tests.add(new TestSet("setHeadingAngle",      this, "testSetHeadingAngle"));
    tests.add(new TestSet("setLatitude",          this, "testSetLatitude"));
    tests.add(new TestSet("setLongitude",         this, "testSetLongitude"));
    tests.add(new TestSet("setMagneticVariation", this, "testSetMagneticVariation"));
    tests.add(new TestSet("setSpeedOverGround",   this, "testSetSpeedOverGround"));
    tests.add(new TestSet("setTimeStamp",         this, "testSetTimeStamp"));
    tests.add(new TestSet("setTrackAngle",        this, "testSetTrackAngle"));


    return tests;
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }

  public void testSetAltitude () {
    byte buffer[] = new byte[64];
    Geolocation gl = new Geolocation();
    gl.setAltitude(0xABC + 0.5);
    gl.readBytes(buffer, 0);
    assertEquals("setAltitude 0xABC + 0.5",0x00015790,getWordAt(buffer,24));
    assertEquals("getAltitude 0xABC + 0.5",0xABC + 0.5, gl.getAltitude());

    gl.setAltitude(0xF0F + 0.875);
    gl.readBytes(buffer, 0);
    assertEquals("setAltitude 0xF0F + 0.875",0x0001E1FC,getWordAt(buffer,24));
    assertEquals("getAltitude 0xF0F + 0.875",0xF0F + 0.875, gl.getAltitude());

    gl.setAltitude(-0.5);
    gl.readBytes(buffer, 0);

    assertEquals("setAltitude -0.5",0xFFFFFFF0,getWordAt(buffer,24));
    assertEquals("getAltitude -0.5",-0.5, gl.getAltitude());
  }

  public void testSetHeadingAngle () {
    byte buffer[] = new byte[64];
    Geolocation gl = new Geolocation();
    gl.setTimeStamp(new TimeStamp(IntegerMode.GPS,0xBBBBBB,0));

    gl.setHeadingAngle(1.5);
    gl.readBytes(buffer, 0);
    assertEquals("setHeadingAngle 1.5",0x00600000,getWordAt(buffer,32));
    assertEquals("getHeadingAngle 1.5",1.5, gl.getHeadingAngle());

    gl.setHeadingAngle(1.0000002384185791015625);
    gl.readBytes(buffer, 0);
    assertEquals("setHeadingAngle 1.0000002384185791015625",0x00400001,getWordAt(buffer,32));
    assertEquals("getHeadingAngle 1.0000002384185791015625",1.0000002384185791015625, gl.getHeadingAngle());

    gl.setHeadingAngle(-0.0000002384185791015625);
    gl.readBytes(buffer, 0);
    assertEquals("setHeadingAngle -0.0000002384185791015625",0xFFFFFFFF,getWordAt(buffer,32));
    assertEquals("getHeadingAngle -0.0000002384185791015625",-0.0000002384185791015625, gl.getHeadingAngle());

    gl.setHeadingAngle(0xFFFF + 0.0);
    gl.readBytes(buffer, 0);
    assertEquals("setHeadingAngle 7FFFFFFF error",0x7FFFFFFF,getWordAt(buffer,32));
    assertEquals("getHeadingAngle 7FFFFFFF error",null, gl.getHeadingAngle());

  }

  public void testSetLatitude () {
    byte buffer[] = new byte[64];
    Geolocation gl = new Geolocation();

    gl.setLatitude(1.5);
    gl.readBytes(buffer, 0);
    assertEquals("setLatitude 1.5",0x00600000,getWordAt(buffer,16));
    assertEquals("getLatitude 1.5",1.5, gl.getLatitude());

    gl.setLatitude(1.0000002384185791015625);
    gl.readBytes(buffer, 0);
    assertEquals("setLatitude 1.0000002384185791015625",0x00400001,getWordAt(buffer,16));
    assertEquals("getLatitude 1.0000002384185791015625",1.0000002384185791015625, gl.getLatitude());

    gl.setLatitude(-0.0000002384185791015625);
    gl.readBytes(buffer, 0);
    assertEquals("setLatitude -0.0000002384185791015625",0xFFFFFFFF,getWordAt(buffer,16));
    assertEquals("getLatitude -0.0000002384185791015625",-0.0000002384185791015625, gl.getLatitude());

    gl.setLatitude(0xFFFF + 0.0);
    gl.readBytes(buffer, 0);
    assertEquals("setLatitude 7FFFFFFF error",0x7FFFFFFF,getWordAt(buffer,16));
    assertEquals("getLatitude 7FFFFFFF error",null, gl.getLatitude());

  }

  public void testSetLongitude () {
    byte buffer[] = new byte[64];
    Geolocation gl = new Geolocation();

    gl.setLongitude(1.5);
    gl.readBytes(buffer, 0);
    assertEquals("setLongitude 1.5",0x00600000,getWordAt(buffer,20));
    assertEquals("getLongitude 1.5",1.5, gl.getLongitude());

    gl.setLongitude(1.0000002384185791015625);
    gl.readBytes(buffer, 0);
    assertEquals("setLongitude 1.0000002384185791015625",0x00400001,getWordAt(buffer,20));
    assertEquals("getLongitude 1.0000002384185791015625",1.0000002384185791015625, gl.getLongitude());

    gl.setLongitude(-0.0000002384185791015625);
    gl.readBytes(buffer, 0);
    assertEquals("setLongitude -0.0000002384185791015625",0xFFFFFFFF,getWordAt(buffer,20));
    assertEquals("getLongitude -0.0000002384185791015625",-0.0000002384185791015625, gl.getLongitude());

    gl.setLongitude(0xFFFF + 0.0);
    gl.readBytes(buffer, 0);
    assertEquals("setLongitude 7FFFFFFF error",0x7FFFFFFF,getWordAt(buffer,20));
    assertEquals("getLongitude 7FFFFFFF error",null, gl.getLongitude());

  }

  public void testSetMagneticVariation () {
    byte buffer[] = new byte[64];
    Geolocation gl = new Geolocation();

    gl.setMagneticVariation(1.5);
    gl.readBytes(buffer, 0);
    assertEquals("setMagneticVariation 1.5",0x00600000,getWordAt(buffer,40));
    assertEquals("getMagneticVariation 1.5",1.5, gl.getMagneticVariation());

    gl.setMagneticVariation(1.0000002384185791015625);
    gl.readBytes(buffer, 0);
    assertEquals("setMagneticVariation 1.0000002384185791015625",0x00400001,getWordAt(buffer,40));
    assertEquals("getMagneticVariation 1.0000002384185791015625",1.0000002384185791015625, gl.getMagneticVariation());

    gl.setMagneticVariation(-0.0000002384185791015625);
    gl.readBytes(buffer, 0);
    assertEquals("setMagneticVariation -0.0000002384185791015625",0xFFFFFFFF,getWordAt(buffer,40));
    assertEquals("getMagneticVariation -0.0000002384185791015625",-0.0000002384185791015625, gl.getMagneticVariation());

    gl.setMagneticVariation(0xFFFF + 0.0);
    gl.readBytes(buffer, 0);
    assertEquals("setMagneticVariation 7FFFFFFF error",0x7FFFFFFF,getWordAt(buffer,40));
    assertEquals("getMagneticVariation 7FFFFFFF error",null, gl.getMagneticVariation());
  }

  public void testSetSpeedOverGround () {
    byte buffer[] = new byte[64];
    Geolocation gl = new Geolocation();

    gl.setSpeedOverGround(1.5);
    gl.readBytes(buffer, 0);
    assertEquals("setSpeedOverGround 1.5",0x00018000,getWordAt(buffer,28));
    assertEquals("getSpeedOverGround 1.5",1.5, gl.getSpeedOverGround());

    gl.setSpeedOverGround(1.0000152587890625);
    gl.readBytes(buffer, 0);
    assertEquals("setSpeedOverGround 1.0000152587890625",0x00010001,getWordAt(buffer,28));
    assertEquals("getSpeedOverGround 1.0000152587890625",1.0000152587890625, gl.getSpeedOverGround());

    gl.setSpeedOverGround(-0.0000152587890625);
    gl.readBytes(buffer, 0);
    assertEquals("setSpeedOverGround -0.0000152587890625",0xFFFFFFFF,getWordAt(buffer,28));
    assertEquals("getSpeedOverGround -0.0000152587890625",-0.0000152587890625, gl.getSpeedOverGround());

    gl.setSpeedOverGround(0xFFFF + 0.0);
    gl.readBytes(buffer, 0);
    assertEquals("setSpeedOverGround 7FFFFFFF error",0x7FFFFFFF,getWordAt(buffer,28));
    assertEquals("getSpeedOverGround 7FFFFFFF error",null, gl.getSpeedOverGround());
  }

  public void testSetTimeStamp () {
    byte buffer[] = new byte[64];
    Geolocation gl = new Geolocation();
    gl.setTimeStamp(new TimeStamp(IntegerMode.UTC,0xFEDCBA,0x123456789AL));
    gl.readBytes(buffer, 0);

    assertEquals("setTimeStamp TSI/TSF",0x06000000,getWordAt(buffer,0));
    assertEquals("setTimeStamp Integer",0x00FEDCBA,getWordAt(buffer,4));
    assertEquals("setTimeStamp Fractional high",0x12,getWordAt(buffer,8));
    assertEquals("setTimeStamp Fractional low",0x3456789A,getWordAt(buffer,12));
  }


  public void testSetTrackAngle () {
    byte buffer[] = new byte[64];
    Geolocation gl = new Geolocation();

    gl.setTrackAngle(1.5);
    gl.readBytes(buffer, 0);
    assertEquals("setTrackAngle 1.5",0x00600000,getWordAt(buffer,36));
    assertEquals("getTrackAngle 1.5",1.5, gl.getTrackAngle());

    gl.setTrackAngle(1.0000002384185791015625);
    gl.readBytes(buffer, 0);
    assertEquals("setTrackAngle 1.0000002384185791015625",0x00400001,getWordAt(buffer,36));
    assertEquals("getTrackAngle 1.0000002384185791015625",1.0000002384185791015625, gl.getTrackAngle());

    gl.setTrackAngle(-0.0000002384185791015625);
    gl.readBytes(buffer, 0);
    assertEquals("setTrackAngle -0.0000002384185791015625",0xFFFFFFFF,getWordAt(buffer,36));
    assertEquals("getTrackAngle -0.0000002384185791015625",-0.0000002384185791015625, gl.getTrackAngle());

    gl.setTrackAngle(0xFFFF + 0.0);
    gl.readBytes(buffer, 0);
    assertEquals("setTrackAngle 7FFFFFFF error",0x7FFFFFFF,getWordAt(buffer,36));
    assertEquals("getTrackAngle 7FFFFFFF error",null, gl.getTrackAngle());
  }


  private int getWordAt(byte [] buffer, int position) {
    int val = 0;
    for(int i = 0; i < 4; i++) {
      val = (val << 8) + (buffer[position+i] & 0xFF);
    }
    return val;
  }
}
