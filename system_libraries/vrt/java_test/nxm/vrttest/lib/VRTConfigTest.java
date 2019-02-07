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
import nxm.vrt.lib.PacketFactory;
import nxm.vrt.lib.VRTConfig;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static nxm.vrttest.inc.TestRunner.assertEquals;
import static nxm.vrttest.inc.TestRunner.assertException;

/** Tests for the {@link VRTConfig} class. */
public class VRTConfigTest implements Testable {
  @Override
  public Class<?> getTestedClass () {
    return VRTConfig.class;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>(8);

    tests.add(new TestSet("DSLT_SERVICE",                this, "+testConstants"));
    tests.add(new TestSet("DSLT_DEVICE",                 this, "+testConstants"));
    tests.add(new TestSet("LEAP_SECONDS_FILE",           this,  "testConstants"));
    tests.add(new TestSet("LIBRARY_VERSION",             this, "+testConstants"));
    tests.add(new TestSet("NORAD_LEAP_SEC_COUNTED",      this, "+testConstants"));
    tests.add(new TestSet("TEST_DEVICE",                 this, "+testConstants"));
    tests.add(new TestSet("TEST_FIRST_MCAST",            this, "+testConstants"));
    tests.add(new TestSet("TEST_FIRST_PORT",             this, "+testConstants"));
    tests.add(new TestSet("TEST_QUICK",                  this, "+testConstants"));
    tests.add(new TestSet("TEST_SERVER",                 this, "+testConstants"));
    tests.add(new TestSet("TEST_SERVER_TIMEOUT",         this, "+testConstants"));
    tests.add(new TestSet("STRICT",                      this, "+testConstants"));
    tests.add(new TestSet("VRT_VERSION",                 this, "+testConstants"));
    tests.add(new TestSet("getPacket(byte[],..)",        this, "!"));
    tests.add(new TestSet("getPacket(..)",               this, "+AbstractPacketFactoryTest.testGetPacket"));
    tests.add(new TestSet("getPacketClass(..)",          this, "+AbstractPacketFactoryTest.testGetPacket"));
    tests.add(new TestSet("getPacketFactory()",          this,  "testGetPacketFactory"));
    tests.add(new TestSet("setPacketFactory(..)",        this, "+testGetPacketFactory"));
    tests.add(new TestSet("getPackets(byte[],..)",       this, "!"));
    tests.add(new TestSet("getPackets(InputStream)",     this, "!"));
    return tests;
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }

  @Override
  public String toString () {
    return "Tests for VRTConfig";
  }

  public void testConstants () {
    // ==== DSLT_SERVICE & DSLT_DEVICE =========================================
    // Not currently used, simply make sure they exist.
    assertEquals("DSLT_SERVICE may be null",  true, (VRTConfig.DSLT_SERVICE == null)
                                                 || (VRTConfig.DSLT_SERVICE != null));
    assertEquals("DSLT_DEVICE may be null",   true, (VRTConfig.DSLT_DEVICE == null)
                                                 || (VRTConfig.DSLT_DEVICE != null));

    // ==== LEAP_SECONDS_FILE ==================================================
    assertEquals("LEAP_SECONDS_FILE is not null",  true, (VRTConfig.LEAP_SECONDS_FILE != null));
    assertEquals("LEAP_SECONDS_FILE is not empty", true, (VRTConfig.LEAP_SECONDS_FILE.length() > 0));

    // ==== LIBRARY_VERSION ====================================================
    int dot1 = VRTConfig.LIBRARY_VERSION.indexOf('.');
    int dot2 = VRTConfig.LIBRARY_VERSION.indexOf('.', dot1+1);
    int dot3 = VRTConfig.LIBRARY_VERSION.indexOf('.', dot2+1);

    assertEquals("LIBRARY_VERSION is in x.x.x form (dot1 > 0)",      true, (dot1 > 0));
    assertEquals("LIBRARY_VERSION is in x.x.x form (dot2 > dot1+1)", true, (dot2 > dot1+1));
    assertEquals("LIBRARY_VERSION is in x.x.x form (dot3 < 0)",      true, (dot3 < 0));

    int num1 = Integer.parseInt(VRTConfig.LIBRARY_VERSION.substring(0,dot1));
    int num2 = Integer.parseInt(VRTConfig.LIBRARY_VERSION.substring(dot1+1,dot2));
    int num3 = Integer.parseInt(VRTConfig.LIBRARY_VERSION.substring(dot2+1));

    assertEquals("LIBRARY_VERSION is in x.x.x form (num1 >= 0)",     true, (num1 >= 0));
    assertEquals("LIBRARY_VERSION is in x.x.x form (num2 >= 0)",     true, (num2 >= 0));
    assertEquals("LIBRARY_VERSION is in x.x.x form (num3 >= 0)",     true, (num3 >= 0));

    // ==== NORAD_LEAP_SEC_COUNTED =============================================
    assertEquals("NORAD_LEAP_SEC_COUNTED is true/false", true, (VRTConfig.NORAD_LEAP_SEC_COUNTED == true)
                                                            || (VRTConfig.NORAD_LEAP_SEC_COUNTED == false));

    // ==== TEST_DELAY =========================================================
    assertEquals("TEST_DELAY is non-negative", true, (VRTConfig.TEST_DELAY >= 0));

    // ==== TEST_DEVICE =========================================================
    assertEquals("TEST_DEVICE is not null", true, (VRTConfig.TEST_DEVICE != null));

    // ==== TEST_FIRST_MCAST ====================================================
    assertEquals("TEST_FIRST_MCAST is not null", true, (VRTConfig.TEST_FIRST_MCAST != null));

    // ==== TEST_FIRST_PORT ====================================================
    assertEquals("TEST_FIRST_PORT is 0..N", true, (VRTConfig.TEST_FIRST_PORT >= 0)
                                               || (VRTConfig.TEST_FIRST_PORT <= 0xFFFF));

    // ==== TEST_QUICK =========================================================
    assertEquals("TEST_QUICK is true/false", true, (VRTConfig.TEST_QUICK == true)
                                                || (VRTConfig.TEST_QUICK == false));

    // ==== TEST_SERVER ==========================================================
    assertEquals("TEST_SERVER is not null", true, (VRTConfig.TEST_SERVER != null));

    // ==== TEST_SERVER_TIMEOUT ================================================
    assertEquals("TEST_SERVER_TIMEOUT is 0..86400", true, (VRTConfig.TEST_SERVER_TIMEOUT >= 0)
                                                       || (VRTConfig.TEST_SERVER_TIMEOUT <= 86400));

    // ==== STRICT =============================================================
    assertEquals("STRICT is true/false", true, (VRTConfig.STRICT == true)
                                            || (VRTConfig.STRICT == false));

    // ==== VRT_VERSION ========================================================
    assertEquals("VRT_VERSION is V49/V49b", true, (VRTConfig.VRT_VERSION == VRTConfig.VITAVersion.V49)
                                               || (VRTConfig.VRT_VERSION == VRTConfig.VITAVersion.V49b));
  }

  public void testGetPacketFactory () {
    PacketFactory origPF = VRTConfig.getPacketFactory();
    PacketFactory testPF = new nxm.vrt.libm.PacketFactory();
    assertEquals("getPacketFactory() is not null", true, (origPF != null));

    try {
      VRTConfig.setPacketFactory(testPF);
      assertEquals("setPacketFactory(..)", testPF, VRTConfig.getPacketFactory());
    }
    finally {
      VRTConfig.setPacketFactory(origPF);
    }
    assertEquals("setPacketFactory(..)", origPF, VRTConfig.getPacketFactory());

    assertException("setPacketFactory(null)", "testSetPacketFactory_null", NullPointerException.class);
  }

  public void testSetPacketFactory_null () {
    VRTConfig.setPacketFactory(null);
  }
}
