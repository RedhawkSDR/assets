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

package nxm.vrttest.net;

import java.util.ArrayList;
import java.util.List;
import nxm.vrt.lib.BasicContextPacket;
import nxm.vrt.lib.BasicDataPacket;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.net.VRTEvent;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static nxm.vrttest.inc.TestRunner.assertEquals;

/** Tests for the {@link VRTEvent} class. */
public class VRTEventTest implements Testable {

  @Override
  public Class<?> getTestedClass () {
    return VRTEvent.class;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    tests.add(new TestSet("getPacket()", this,  "testGetPacket"));
    tests.add(new TestSet("getSource()", this, "+testGetPacket"));

    return tests;
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }

  @Override
  public String toString () {
    return "Tests for "+getTestedClass();
  }

  public void testGetPacket () {
    String    src1 = "ONE";
    String    src2 = "TWO";
    String    src3 = "THREE";
    VRTPacket pkt1 = null;
    VRTPacket pkt2 = new BasicContextPacket();
    VRTPacket pkt3 = new BasicDataPacket();

    pkt2.setTimeStamp(TimeStamp.Y2K_GPS);
    pkt3.setTimeStamp(TimeStamp.getSystemTime());

    VRTEvent evt1 = new VRTEvent(src1);
    VRTEvent evt2 = new VRTEvent(src2, pkt2);
    VRTEvent evt3 = new VRTEvent(src3, pkt3);

    assertEquals("getSource()", src1, evt1.getSource());
    assertEquals("getSource()", src2, evt2.getSource());
    assertEquals("getSource()", src3, evt3.getSource());

    assertEquals("getPacket()", pkt1, evt1.getPacket());
    assertEquals("getPacket()", pkt2, evt2.getPacket());
    assertEquals("getPacket()", pkt3, evt3.getPacket());
  }
}
