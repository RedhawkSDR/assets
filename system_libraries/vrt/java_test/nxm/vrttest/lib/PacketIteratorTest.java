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
import nxm.vrt.lib.BasicVRLFrame;
import nxm.vrt.lib.PacketIterator;
import nxm.vrt.lib.VRTPacket;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static nxm.vrttest.inc.TestRunner.assertEquals;
import static nxm.vrttest.lib.VRAFileTest.makeTestPacketArray;

/** Tests for the {@link PacketIterator} class. This set of test utilizes the
 *  {@link PacketIterator} returned from the {@link BasicVRLFrame#iterator()}
 *  method.
 */
public class PacketIteratorTest implements Testable {
  @Override
  public Class<?> getTestedClass () {
    return PacketIterator.class;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    tests.add(new TestSet("hasMoreElements", this, "+testNext"));
    tests.add(new TestSet("hasNext",         this, "+testNext"));
    tests.add(new TestSet("next",            this,  "testNext"));
    tests.add(new TestSet("nextElement",     this, "+testNext"));
    tests.add(new TestSet("remove",          this, "+testNext"));
    tests.add(new TestSet("toString",        this,  "testToString"));

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

  public void testNext () {
    VRTPacket[]   packets = makeTestPacketArray();
    BasicVRLFrame frame1  = new BasicVRLFrame();
    BasicVRLFrame frame2  = new BasicVRLFrame();
    frame2.setVRTPackets(packets);

    PacketIterator pi1  = frame1.iterator();
    PacketIterator pi2a = frame2.iterator();
    PacketIterator pi2b = frame2.iterator();

    for (int i = 0; i < packets.length; i++) {
      assertEquals("Iterator 2a hasMoreElements()", true, pi2a.hasMoreElements());
      assertEquals("Iterator 2a hasNext()",         true, pi2a.hasNext());
      assertEquals("Iterator 2b hasMoreElements()", true, pi2b.hasMoreElements());
      assertEquals("Iterator 2b hasNext()",         true, pi2b.hasNext());

      assertEquals("Iterator 2a next()",        packets[i], pi2a.next());
      assertEquals("Iterator 2b nextElement()", packets[i], pi2b.nextElement());

      if (i == 2) {
        try {
          pi2a.remove();
          assertEquals("BasicVRLFrame does not support removal from iterator", true, false);
        }
        catch (Exception e) {
          assertEquals("BasicVRLFrame does not support removal from iterator", true, true);
        }
      }
    }

    assertEquals("Iterator 1 hasMoreElements()",  false, pi1.hasMoreElements());
    assertEquals("Iterator 1 hasNext()",          false, pi1.hasNext());
    assertEquals("Iterator 2a hasMoreElements()", false, pi2a.hasMoreElements());
    assertEquals("Iterator 2a hasNext()",         false, pi2a.hasNext());
    assertEquals("Iterator 2b hasMoreElements()", false, pi2b.hasMoreElements());
    assertEquals("Iterator 2b hasNext()",         false, pi2b.hasNext());
  }

  public void testToString () {
    BasicVRLFrame frame1 = new BasicVRLFrame();
    BasicVRLFrame frame2 = new BasicVRLFrame();
    frame2.setVRTPackets(makeTestPacketArray());

    PacketIterator pi1  = frame1.iterator();
    PacketIterator pi2  = frame2.iterator();
    String         exp1 = "PacketIterator for "+frame1;
    String         exp2 = "PacketIterator for "+frame2;

    assertEquals("Iterator 1 toString()", exp1, pi1.toString());
    assertEquals("Iterator 2 toString()", exp2, pi2.toString());
  }
}
