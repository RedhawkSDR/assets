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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import nxm.vrt.lib.BasicContextPacket;
import nxm.vrt.lib.BasicDataPacket;
import nxm.vrt.lib.ContextPacket;
import nxm.vrt.lib.DataPacket;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.net.VRTContextListener;
import nxm.vrt.net.VRTEvent;
import nxm.vrt.net.VRTEventListener;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;
import nxm.vrttest.net.TestListener.Mode;

import static nxm.vrttest.inc.TestRunner.assertEquals;

/** Tests for the {@link VRTEventListener} and {@link VRTContextListener} classes. */
public class VRTEventListenerTest<T extends VRTEventListener> implements Testable {
  private final Class<T> testedClass;

  /** Creates a new instance using the given implementation class. */
  public VRTEventListenerTest (Class<T> testedClass) {
    this.testedClass = testedClass;
  }

  @Override
  public Class<?> getTestedClass () {
    return testedClass;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    if (true) {
      tests.add(new TestSet("receivedPacket(..)",         this,  "testReceivedPacket"));
      tests.add(new TestSet("sentPacket(..)",             this, "+testReceivedPacket"));
      tests.add(new TestSet("errorOccurred(..)",          this, "+testReceivedPacket"));
      tests.add(new TestSet("warningOccurred(..)",        this, "+testReceivedPacket"));
    }
    if (getTestedClass() == VRTContextListener.class) {
      tests.add(new TestSet("receivedDataPacket(..)",     this, "+testReceivedPacket"));
      tests.add(new TestSet("receivedContextPacket(..)",  this, "+testReceivedPacket"));
      tests.add(new TestSet("receivedInitialContext(..)", this, "+testReceivedPacket"));
    }

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

  public void testReceivedPacket () {
    TestListener           listener = new TestListener();
    VRTEvent               event    = new VRTEvent(this);
    DataPacket             data     = new BasicDataPacket();
    ContextPacket          ctx      = new BasicContextPacket();
    String                 message  = "Hello World!";
    Throwable              error    = new RuntimeException("Test Exception");
    Map<Integer,VRTPacket> context  = new LinkedHashMap<Integer,VRTPacket>();

    data.setTimeStamp(TimeStamp.getSystemTime());
    ctx.setTimeStamp(TimeStamp.Y2K_GPS);

    listener.receivedPacket(event, data);
    assertEquals("receivedPacket(..)", Mode.receivedPacket, listener.mode);
    assertEquals("receivedPacket(..)", event,               listener.event);
    assertEquals("receivedPacket(..)", data,                listener.packet);

    listener.sentPacket(event, data);
    assertEquals("sentPacket(..)", Mode.sentPacket, listener.mode);
    assertEquals("sentPacket(..)", event,           listener.event);
    assertEquals("sentPacket(..)", data,            listener.packet);

    listener.errorOccurred(event, message, error);
    assertEquals("errorOccurred(..)", Mode.errorOccurred, listener.mode);
    assertEquals("errorOccurred(..)", event,              listener.event);
    assertEquals("errorOccurred(..)", message,            listener.message);
    assertEquals("errorOccurred(..)", error,              listener.error);

    listener.warningOccurred(event, message, error);
    assertEquals("warningOccurred(..)", Mode.warningOccurred,   listener.mode);
    assertEquals("warningOccurred(..)", event,                  listener.event);
    assertEquals("warningOccurred(..)", message,                listener.message);
    assertEquals("warningOccurred(..)", error,                  listener.error);

    if (getTestedClass() == VRTContextListener.class) {
      listener.receivedDataPacket(event, data);
      assertEquals("receivedDataPacket(..)", Mode.receivedDataPacket, listener.mode);
      assertEquals("receivedDataPacket(..)", event,                   listener.event);
      assertEquals("receivedDataPacket(..)", data,                    listener.packet);

      listener.receivedContextPacket(event, ctx);
      assertEquals("receivedContextPacket(..)", Mode.receivedContextPacket, listener.mode);
      assertEquals("receivedContextPacket(..)", event,                      listener.event);
      assertEquals("receivedContextPacket(..)", ctx,                        listener.packet);

      listener.receivedInitialContext(event, message, data, ctx, context);
      assertEquals("receivedInitialContext(..)", Mode.receivedInitialContext, listener.mode);
      assertEquals("receivedInitialContext(..)", event,                       listener.event);
      assertEquals("receivedInitialContext(..)", message,                     listener.message);
      assertEquals("receivedInitialContext(..)", data,                        listener.data);
      assertEquals("receivedInitialContext(..)", ctx,                         listener.ctx);
      assertEquals("receivedInitialContext(..)", context,                     listener.context);
    }
  }
}
