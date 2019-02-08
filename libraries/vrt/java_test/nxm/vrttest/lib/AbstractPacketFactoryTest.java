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
import nxm.vrt.lib.AbstractPacketFactory;
import nxm.vrt.lib.BasicVRTPacket;
import nxm.vrt.lib.ContextPacket;
import nxm.vrt.lib.DataPacket;
import nxm.vrt.lib.PacketFactory;
import nxm.vrt.lib.StandardDataPacket;
import nxm.vrt.lib.Utilities;
import nxm.vrt.lib.VRTConfig;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.lib.VRTPacket.PacketType;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static nxm.vrttest.inc.TestRunner.assertAssignableFrom;
import static nxm.vrttest.inc.TestRunner.assertEquals;
import static nxm.vrttest.inc.TestRunner.assertInstanceOf;

/** Tests for the {@link AbstractPacketFactory} class. */
public class AbstractPacketFactoryTest implements Testable {
  @Override
  public Class<?> getTestedClass () {
    return AbstractPacketFactory.class;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    tests.add(new TestSet("getPacket(..)",      this, "testGetPacket"));
    tests.add(new TestSet("getPacketClass(..)", this, "+testGetPacket"));

    return tests;
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }

  @Override
  public String toString () {
    return "Tests for AbstractPacketFactory";
  }

  public void testGetPacket () {
    PacketFactory         origPF = VRTConfig.getPacketFactory();
    AbstractPacketFactory fact   = new AbstractPacketFactory();

    final Object[] tests = {
      // Type                          Class ID               Super Class
      PacketType.UnidentifiedData,     "FF-FF-FF:1234.ABCD",  DataPacket.class,
      PacketType.Data,                 "FF-FF-FF:1234.ABCD",  DataPacket.class,
      PacketType.UnidentifiedExtData,  "FF-FF-FF:1234.ABCD",  DataPacket.class,
      PacketType.ExtData,              "FF-FF-FF:1234.ABCD",  DataPacket.class,
      PacketType.Context,              "FF-FF-FF:1234.ABCD",  ContextPacket.class,
      PacketType.ExtContext,           "FF-FF-FF:1234.ABCD",  VRTPacket.class,
      PacketType.Data,                 "FF-FF-FA:0000.0000",  StandardDataPacket.class, //  1-bit
      PacketType.ExtData,              "FF-FF-FA:0000.0000",  StandardDataPacket.class, //  1-bit
      PacketType.Data,                 "FF-FF-FA:0001.0000",  StandardDataPacket.class, //  4-bit
      PacketType.ExtData,              "FF-FF-FA:0001.0000",  StandardDataPacket.class, //  4-bit
      PacketType.Data,                 "FF-FF-FA:0002.0000",  StandardDataPacket.class, //  8-bit
      PacketType.ExtData,              "FF-FF-FA:0002.0000",  StandardDataPacket.class, //  8-bit
      PacketType.Data,                 "FF-FF-FA:0003.0000",  StandardDataPacket.class, // 16-bit
      PacketType.ExtData,              "FF-FF-FA:0003.0000",  StandardDataPacket.class, // 16-bit
      PacketType.Data,                 "FF-FF-FA:0004.0000",  StandardDataPacket.class, // 32-bit
      PacketType.ExtData,              "FF-FF-FA:0004.0000",  StandardDataPacket.class, // 32-bit
      PacketType.Data,                 "FF-FF-FA:0005.0000",  StandardDataPacket.class, // 64-bit
      PacketType.ExtData,              "FF-FF-FA:0005.0000",  StandardDataPacket.class, // 64-bit
    };

    VRTConfig.setPacketFactory(fact);
    try {
      for (int i = 0; i < tests.length; i+=3) {
        PacketType type  = (PacketType)tests[i];
        String     idStr = (String)tests[i+1];
        Long       idNum = Utilities.fromStringClassID(idStr);
        Class<?>   clazz = (Class<?>)tests[i+2];
        VRTPacket  pkt   = new BasicVRTPacket();

        pkt.setPacketType(type);
        if ((type != PacketType.UnidentifiedData) && (type != PacketType.UnidentifiedExtData)) {
          pkt.setClassID(idStr);
        }
        pkt.setPayloadLength(0);

        Class<?>   actClass = fact.getPacketClass(type, idNum);
        VRTPacket  actObj1  = fact.getPacket(type, idNum);
        VRTPacket  actObj2  = fact.getPacket(pkt, true );
        VRTPacket  actObj3  = fact.getPacket(pkt, false);

        assertAssignableFrom("getPacketClass("+type+", "+idStr+")", clazz, actClass);
        assertInstanceOf("getPacket("+type+", "+idStr+") type",     clazz, actObj1);
        assertInstanceOf("getPacket("+pkt+", true) type",           clazz, actObj2);
        assertInstanceOf("getPacket("+pkt+", false) type",          clazz, actObj3);
        assertEquals("getPacket("+pkt+", true) value",              pkt,   actObj2);
        assertEquals("getPacket("+pkt+", false) value",             pkt,   actObj3);

        assertEquals("VRTConfig.getPacketClass("+type+", "+idStr+")", actClass, VRTConfig.getPacketClass(type, idNum));
        assertEquals("VRTConfig.getPacket("+type+", "+idStr+")",      actObj1,  VRTConfig.getPacket(type, idNum));
        assertEquals("VRTConfig.getPacket("+pkt+", true) value",      actObj2,  VRTConfig.getPacket(pkt, true ));
        assertEquals("VRTConfig.getPacket("+pkt+", false) value",     actObj3,  VRTConfig.getPacket(pkt, false));
      }
    }
    finally {
      VRTConfig.setPacketFactory(origPF);
    }
  }

}
