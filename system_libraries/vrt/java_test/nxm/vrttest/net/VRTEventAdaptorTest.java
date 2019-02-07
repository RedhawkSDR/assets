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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import nxm.vrt.lib.BasicDataPacket;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.net.VRTEvent;
import nxm.vrt.net.VRTEventAdaptor;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static nxm.vrttest.inc.TestRunner.assertEquals;

/** Tests for the {@link VRTEventAdaptor} class. */
public class VRTEventAdaptorTest implements Testable {

  @Override
  public Class<?> getTestedClass () {
    return VRTEventAdaptor.class;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    tests.add(new TestSet("receivedPacket(..)",         this,  "testReceivedPacket"));
    tests.add(new TestSet("sentPacket(..)",             this, "+testReceivedPacket"));
    tests.add(new TestSet("errorOccurred(..)",          this, "+testReceivedPacket"));
    tests.add(new TestSet("warningOccurred(..)",        this, "+testReceivedPacket"));

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

  /** Checks the output and then resets the streams. */
  private void checkOutput (String msg, ByteArrayOutputStream out,
                                        ByteArrayOutputStream err,
                                        boolean anyOut, boolean anyErr) {
    if (anyOut) {
      assertEquals(msg+" -> got out", true, out.size() > 0);
    }
    else {
      assertEquals(msg+" -> no out", true, out.size() == 0);
    }


    if (anyErr) {
      assertEquals(msg+" -> got err", true, err.size() > 0);
    }
    else {
      assertEquals(msg+" -> no err", true, err.size() == 0);
    }

    out.reset();
    err.reset();
  }

  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public void testReceivedPacket () throws Exception {
    VRTEvent    evt  = new VRTEvent(this);
    VRTPacket   pkt  = new BasicDataPacket();
    String      msg  = "Hello World!";
    Throwable   t    = new Exception();
    PrintStream _out = System.out;
    PrintStream _err = System.err;

    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
      ByteArrayOutputStream err = new ByteArrayOutputStream(4096);

      System.setOut(new PrintStream(out));
      System.setErr(new PrintStream(err));

      VRTEventAdaptor adaptor1 = new VRTEventAdaptor();
      VRTEventAdaptor adaptor2 = new VRTEventAdaptor(false);
      VRTEventAdaptor adaptor3 = new VRTEventAdaptor(true);


      adaptor1.receivedPacket(evt, pkt);
      checkOutput("receivedPacket(..)", out, err, false, false);
      adaptor2.receivedPacket(evt, pkt);
      checkOutput("receivedPacket(..)", out, err, false, false);
      adaptor3.receivedPacket(evt, pkt);
      checkOutput("receivedPacket(..)", out, err, true, false);

      adaptor1.sentPacket(evt, pkt);
      checkOutput("sentPacket(..)", out, err, false, false);
      adaptor2.sentPacket(evt, pkt);
      checkOutput("sentPacket(..)", out, err, false, false);
      adaptor3.sentPacket(evt, pkt);
      checkOutput("sentPacket(..)", out, err, true, false);

      adaptor1.errorOccurred(evt, msg, t);
      checkOutput("errorOccurred(..)", out, err, false, true);
      adaptor2.errorOccurred(evt, msg, t);
      checkOutput("errorOccurred(..)", out, err, false, true);
      adaptor3.errorOccurred(evt, msg, t);
      checkOutput("errorOccurred(..)", out, err, false, true);

      adaptor1.warningOccurred(evt, msg, t);
      checkOutput("warningOccurred(..)", out, err, false, true);
      adaptor2.warningOccurred(evt, msg, t);
      checkOutput("warningOccurred(..)", out, err, false, true);
      adaptor3.warningOccurred(evt, msg, t);
      checkOutput("warningOccurred(..)", out, err, false, true);
    }
    finally {
      System.setOut(_out);
      System.setErr(_err);
    }
  }
}
