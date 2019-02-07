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
import nxm.vrt.lib.AbstractVRAFile;
import nxm.vrt.lib.BasicContextPacket;
import nxm.vrt.lib.BasicDataPacket;
import nxm.vrt.lib.BasicVRAFile;
import nxm.vrt.lib.BasicVRLFrame;
import nxm.vrt.lib.BasicVRTPacket;
import nxm.vrt.lib.HasFields;
import nxm.vrt.lib.Utilities;
import nxm.vrt.lib.VRAFile;
import nxm.vrt.lib.VRLFrame;
import nxm.vrt.lib.VRTConfig;
import nxm.vrt.net.VRTContextListener;
import nxm.vrt.net.VRTEventListener;
import nxm.vrttest.inc.JsonUtilitiesTest;
import nxm.vrttest.inc.Testable;
import nxm.vrttest.net.NetTestClient;
import nxm.vrttest.net.NetTestServer;
import nxm.vrttest.net.NetUtilitiesTest;
import nxm.vrttest.net.VRTEventAdaptorTest;
import nxm.vrttest.net.VRTEventListenerTest;
import nxm.vrttest.net.VRTEventTest;
import nxm.vrttest.net.VRTListenerSupportTest;

import static nxm.vrttest.inc.TestRunner.runTests;

/** Runs the test cases. */
public final class Main extends nxm.vrt.lib.Main {
  private static enum TestType { UnitTest, NetTestClient, NetTestServer };

  private static final String[] OPTIONS = { "" };
  private static final String[] VERSION = { "VRT Java Test Suite "+VRTConfig.LIBRARY_VERSION };
  private static final String[] USAGE   = {
      "This is the core of the VRT Java Test Suite Library and includes the",
      "following functions:",
      "",
      "UnitTest      - Runs the suite of unit tests.",
      "                  java -jar vrttest.jar UnitTest",
      "",
      "NetTestClient - Runs the client side of the networking tests.",
      "                  java -jar vrttest.jar NetTestClient [hostPort]",
      "",
      "                  hostPort - The host and port the server is running on.",
      "                             Note that the server must be started *prior*",
      "                             to running this test suite.",
      "",
      "NetTestServer - Runs the server side of the networking tests. Note that",
      "                this server side is used for both the Java and C++ tests.",
      "                  java -jar vrttest.jar NetTestServer [hostPort] [duration]",
      "",
      "                  hostPort - The (host and) port to run the server on.",
      "                             This will typically be in port-only form",
      "                             (e.g. ':8080'), but may optionally include",
      "                             a host name.",
      "                  duration - The duration (in seconds) for the server",
      "                             to remain active.",
      "",
      "The following environment variables also control the test operation, note",
      "that when running the networking tests that both client and server must",
      "have these set identically:",
      "  VRT_TEST_FIRST_MCAST     - Sets the UDP/Multicast address range to use.",
      "                             This will be an implied range of 8 addresses",
      "                             starting at the given address. Setting this",
      "                             to '' will disable any tests that require",
      "                             using UDP/Multicast (default is '').",
      "",
      "  VRT_TEST_FIRST_PORT      - Sets the port numbers to use as part of the",
      "                             unit/network tests. This will be an implied",
      "                             range of 10 ports starting at the given port",
      "                             port number. Setting this to 0 will disable",
      "                             any tests that require opening a port",
      "                             (default is 0).",
      "",
      "  VRT_TEST_QUICK           - Turns on/off 'quick test' mode which ",
      "                             abbreviates some of the test cases in order",
      "                             to speed up the testing process (default is",
      "                             on). Setting this to 'off' may cause the",
      "                             test suite to take 15+ minutes to execute.",
  };

  private final TestType testType;

  /** Creates a new instance. */
  public Main (String[] args) {
    super(VERSION, USAGE, OPTIONS, 1, 1, args);
    this.testType = Utilities.toEnum(TestType.class, parameters.get(0));
  }

  /** Runs the command. */
  public static void main (String ... args) {
    runMain(Main.class, args);
  }

  /** Runs all of the test cases. */
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  @Override
  protected void main () throws Exception {
    if (isHelpVersion()) {
      super.main();
      return;
    }

    List<Testable> testCases = new ArrayList<Testable>();

    switch (testType) {
      case UnitTest:
        // nxm.vrttest.inc.*
        testCases.add(new JsonUtilitiesTest());

//        // nxm.vrt.lib.*
//        testCases.add(new AbstractPacketFactoryTest());
//        testCases.add(new AsciiCharSequenceTest());
//        testCases.add(new VRAFileTest<AbstractVRAFile>(AbstractVRAFile.class));
//        testCases.add(new AsciiCharSequenceTest());
//        testCases.add(new VRAFileTest<BasicVRAFile>(BasicVRAFile.class));
//        testCases.add(new VRLFrameTest<BasicVRLFrame>(BasicVRLFrame.class));
//        testCases.add(new VRTPacketTest<BasicVRTPacket>(BasicVRTPacket.class));
//        testCases.add(new ContextPacketTest<BasicContextPacket>(BasicContextPacket.class));
//        testCases.add(new DataPacketTest<BasicDataPacket>(BasicDataPacket.class));
//        testCases.add(new EphemerisTest());
//        testCases.add(new GeolocationTest());
//        testCases.add(new HasFieldsTest<HasFields>(HasFields.class));
//        testCases.add(new LeapSecondsTest());
//        testCases.add(new MainTest<nxm.vrt.lib.Main>(nxm.vrt.lib.Main.class));
//        testCases.add(new PackUnpackTest());
//        testCases.add(new PacketIteratorTest());
//        testCases.add(new TimeStampTest());
//        testCases.add(new UtilitiesTest());
//        testCases.add(new VRTPacketTest.PayloadFormatTest());
//        testCases.add(new XmlUtilitiesTest());
//        testCases.add(new VRAFileTest<VRAFile>(VRAFile.class));
//        testCases.add(new VRLFrameTest<VRLFrame>(VRLFrame.class));
//        testCases.add(new VRTConfigTest());
//        testCases.add(new VRTMathTest());
//
//        // nxm.vrt.net.*
//        testCases.add(new NetUtilitiesTest());
//        testCases.add(new VRTEventAdaptorTest());
//        testCases.add(new VRTEventListenerTest<VRTContextListener>(VRTContextListener.class));
//        testCases.add(new VRTEventTest());
//        testCases.add(new VRTListenerSupportTest());
//        testCases.add(new VRTEventListenerTest<VRTEventListener>(VRTEventListener.class));
        break;
      case NetTestClient:
        testCases.add(new NetTestClient());
        break;
      case NetTestServer:
        (new NetTestServer()).run();
        break;
      default:
        throw new AssertionError("Unexpected test type "+testType);
    }

    if (testCases.isEmpty()) {
      System.exit(0);
    }
    else {
      boolean pass = runTests(testCases, true);
      System.exit((pass)? 0 : 1);
    }
  }
}
