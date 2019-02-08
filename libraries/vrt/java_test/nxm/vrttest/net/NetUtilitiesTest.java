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

import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import nxm.vrt.lib.VRTConfig;
import nxm.vrt.net.NetUtilities;
import nxm.vrt.net.NetUtilities.TCPSocket;
import nxm.vrt.net.NetUtilities.TransportProtocol;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static nxm.vrttest.inc.TestRunner.assertEquals;

/** Tests for the {@link NetUtilities} class. */
public class NetUtilitiesTest implements Testable {

  @Override
  public Class<?> getTestedClass () {
    return NetUtilities.class;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    String ok = (VRTConfig.TEST_FIRST_PORT > 0)? "" : "-"; // OK to open ports

    tests.add(new TestSet("MAX_IPv4_UDP_LEN",            this,    "testConstants"));
    tests.add(new TestSet("MAX_UDP_LEN",                 this,   "+testConstants"));
    tests.add(new TestSet("UDP_HEADER_LENGTH",           this,   "+testConstants"));

    tests.add(new TestSet("acceptTCPSocket(..)",         this,   "+testCreateTCPServerSocket"));
    tests.add(new TestSet("createSocket(..)",            this,   "+testCreateUDPSocket"));
    tests.add(new TestSet("createTCPServerSocket(..)",   this,   "+testCreateTCPSocket"));
    tests.add(new TestSet("createTCPSocket(..)",         this, ok+"testCreateTCPSocket"));
    tests.add(new TestSet("createUDPSocket(..)",         this, ok+"testCreateUDPSocket"));
    tests.add(new TestSet("getDefaultSocketOptions(..)", this,    "testGetDefaultSocketOptions"));
    tests.add(new TestSet("getHost(..)",                 this,    "testGetHost"));
    tests.add(new TestSet("getHostAddress(..)",          this,    "testGetHostAddress"));
    tests.add(new TestSet("getNetworkDeviceName(..)",    this,    "testGetNetworkDeviceName"));
    tests.add(new TestSet("getPort(..)",                 this,   "+testGetHost"));
    tests.add(new TestSet("toString(..)",                this,   "+")); // various tests

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

  public void testConstants () {
    assertEquals("MAX_IPv4_UDP_LEN",  65475, NetUtilities.MAX_IPv4_UDP_LEN);
    assertEquals("MAX_UDP_LEN",       65535, NetUtilities.MAX_UDP_LEN);
    assertEquals("UDP_HEADER_LENGTH",     8, NetUtilities.UDP_HEADER_LENGTH);
  }

  public void testGetNetworkDeviceName () {
    assertEquals("getNetworkDeviceName('eth1',-1)", "eth1",
     NetUtilities.getNetworkDeviceName("eth1",-1));

    assertEquals("getNetworkDeviceName('eth0',12)", "eth0.12",
     NetUtilities.getNetworkDeviceName("eth0",12));

    assertEquals("getNetworkDeviceName('',-1)", "",
     NetUtilities.getNetworkDeviceName("",-1));

    assertEquals("getNetworkDeviceName(null,-1)", null,
     NetUtilities.getNetworkDeviceName(null,-1));

    try {
      NetUtilities.getNetworkDeviceName("",12);
      assertEquals("getNetworkDeviceName('',12) -> error", true, false);
    }
    catch (Exception e) {
      assertEquals("getNetworkDeviceName('',12) -> error", true, true);
    }
    try {
      NetUtilities.getNetworkDeviceName(null,12);
      assertEquals("getNetworkDeviceName(null,12) -> error", true, false);
    }
    catch (Exception e) {
      assertEquals("getNetworkDeviceName(null,12) -> error", true, true);
    }
  }

  public void testGetHostAddress () throws Exception {
    // The tests here are limited to localhost since that is the only one
    // we are guaranteed to be able to get.
    InetAddress addr1 = NetUtilities.getHostAddress("localhost");
    InetAddress addr2 = NetUtilities.getHostAddress("localhost:9999");

    InetAddress addr3 = NetUtilities.getHostAddress("127.0.0.1");
    InetAddress addr4 = NetUtilities.getHostAddress("127.0.0.1:9999");

    InetAddress addr5 = NetUtilities.getHostAddress("[::1]");
    InetAddress addr6 = NetUtilities.getHostAddress("[::1]:9999");

    InetAddress addr7 = NetUtilities.getHostAddress(":9999");

    InetAddress exp4  = InetAddress.getByName("127.0.0.1"); // IPv4
    InetAddress exp6  = InetAddress.getByName("[::1]");     // IPv6

    if (addr1 instanceof Inet6Address) { // in the off-chance we get an IPv6 back
      assertEquals("getHostAddress(localhost)",      exp6, addr1);
      assertEquals("getHostAddress(localhost:9999)", exp6, addr2);
    }
    else {
      assertEquals("getHostAddress(localhost)",      exp4, addr1);
      assertEquals("getHostAddress(localhost:9999)", exp4, addr2);
    }
    assertEquals("getHostAddress(127.0.0.1)",        exp4, addr3);
    assertEquals("getHostAddress(127.0.0.1:9999)",   exp4, addr4);
    assertEquals("getHostAddress([::1])",            exp6, addr5);
    assertEquals("getHostAddress([::1]:9999)",       exp6, addr6);
    assertEquals("getHostAddress(:9999)",            null, addr7);
  }

  /** Tests host/port access. */
  private void _testHostPort (String host) {
    short  port     = (short)9999;
    String portOnly = ":" + port;
    String hostPort = host + portOnly;

    assertEquals("getHost("+host    +")", host, NetUtilities.getHost(host));
    assertEquals("getPort("+host    +")", null, NetUtilities.getPort(host));
    assertEquals("getHost(:"+port   +")", null, NetUtilities.getHost(portOnly));
    assertEquals("getPort(:"+port   +")", port, NetUtilities.getPort(portOnly));
    assertEquals("getHost("+hostPort+")", host, NetUtilities.getHost(hostPort));
    assertEquals("getPort("+hostPort+")", port, NetUtilities.getPort(hostPort));
  }

  public void testGetHost () {
    _testHostPort("localhost");
    _testHostPort("127.0.0.1");
    _testHostPort("[::1]");
    _testHostPort("example.com");      // Reserved name under RFC 2606
    _testHostPort("example.net");      // Reserved name under RFC 2606
    _testHostPort("example.org");      // Reserved name under RFC 2606
    _testHostPort("www.example.com");  // Reserved name under RFC 2606
    _testHostPort("www.example.net");  // Reserved name under RFC 2606
    _testHostPort("www.example.org");  // Reserved name under RFC 2606
  }

  public void testGetDefaultSocketOptions () {
    // The tests here are fairly basic since the only guarantee is that the output
    // is not null.
    for (TransportProtocol t : TransportProtocol.values()) {
      Map<String,Object> options1 = NetUtilities.getDefaultSocketOptions(false,t);
      Map<String,Object> options2 = NetUtilities.getDefaultSocketOptions(true,t);

      assertEquals("getDefaultSocketOptions("+t+",false)", true, (options1 != null));
      assertEquals("getDefaultSocketOptions("+t+",true)",  true, (options2 != null));
    }
  }

  /** Tests the to-string for a socket. */
  private void _testToString (String protocol, String host, Integer port, String dev,
                              boolean closed, String act) throws Exception {
    if (closed) {
      assertEquals("toString(..) -> shows closed", true, act.toLowerCase().contains("closed"));
      return;
    }

    assertEquals("toString(..) -> has protocol name",   true, act.contains(protocol));

    if (host != null) {
      InetAddress addr = InetAddress.getByName(host);
      assertEquals("toString(..) -> has host",   true, act.contains(addr.getHostName())
                                                    || act.contains(addr.getHostAddress()));
    }

    if (port != null) {
      assertEquals("toString(..) -> has port",   true, act.contains(":"+port));
    }

    if (dev != null) {
      assertEquals("toString(..) -> has device", true, act.contains(dev));
    }
  }

  /** Tests socket creation. */
  @SuppressWarnings("deprecation")
  private void _testCreateUDPSocket (String host, NetworkInterface device, int bufLength, boolean mcast) throws Exception {
    // The tests here are very brief and could use more checks in the future (they
    // have not been added thus far since it gets tricky to do them in a portable
    // manner).
    int           port     = VRTConfig.TEST_FIRST_PORT;
    String        dev      = (device == null)? null : device.getName();
    Map<String,?> options  = new LinkedHashMap<String,Object>();
    String        protocol = (mcast)? "UDP/Multicast" : "UDP";

    // ---- New createUDPSocket(..) function -----------------------------------
    if (mcast) {
      DatagramSocket out1 = NetUtilities.createUDPSocket(true,  host, port, dev, options, bufLength);
      DatagramSocket in1  = NetUtilities.createUDPSocket(false, host, port, dev, options, bufLength);

      assertEquals("createUDPSocket(..) -> Multicast", true,    (out1 instanceof MulticastSocket));
      assertEquals("createUDPSocket(..) -> Multicast", true,    (in1 instanceof MulticastSocket));

      _testToString(protocol, null, null, dev, false, NetUtilities.toString(out1));
      out1.close();
      _testToString(protocol, null, null, dev, true, NetUtilities.toString(out1));

      _testToString(protocol, null, null, dev, false, NetUtilities.toString(in1));
      in1.close();
      _testToString(protocol, null, null, dev, true, NetUtilities.toString(in1));
    }
    else {
      DatagramSocket out1 = NetUtilities.createUDPSocket(true,  host, port, dev, options, bufLength);
      DatagramSocket in1  = NetUtilities.createUDPSocket(false, null, port, dev, options, bufLength);

      assertEquals("createUDPSocket(..) -> Multicast", false,   (out1 instanceof MulticastSocket));
      assertEquals("createUDPSocket(..) -> Multicast", false,   (in1 instanceof MulticastSocket));
      assertEquals("createUDPSocket(..) -> address",   port,    in1.getLocalPort());

      _testToString(protocol, null, null, dev, false, NetUtilities.toString(out1));
      out1.close();
      _testToString(protocol, null, null, dev, true, NetUtilities.toString(out1));

      _testToString(protocol, null, port, dev, false, NetUtilities.toString(in1));
      in1.close();
      _testToString(protocol, null, port, dev, true, NetUtilities.toString(in1));
    }

    // ---- Legacy createSocket(..) function -----------------------------------
    if ((host != null) && host.startsWith("[")) {
      // The legacy functions aren't entirely friendly with IPv6
    }
    else {
      // Just verify there are no exceptions when opening the sockets
      InetSocketAddress address = (host == null)? new InetSocketAddress(port)
                                                : new InetSocketAddress(host,port);

      DatagramSocket sock1 = NetUtilities.createSocket(host, port, dev, null, bufLength);
      assertEquals("createSocket(..) -> address", address, sock1.getLocalSocketAddress());
      sock1.close();

      DatagramSocket sock2 = NetUtilities.createSocket(address, device, null, bufLength);
      assertEquals("createSocket(..) -> address", address, sock2.getLocalSocketAddress());
      sock2.close();
    }
  }

  public void testCreateUDPSocket () throws Exception {
    _testCreateUDPSocket("localhost", null, 1024, false);
    _testCreateUDPSocket("127.0.0.1", null, 2048, false);
    _testCreateUDPSocket("[::1]",     null, 4096, false);
    _testCreateUDPSocket(null,        null, 8192, false);

    Enumeration<NetworkInterface> allDevices = NetworkInterface.getNetworkInterfaces();
    while (allDevices.hasMoreElements()) {
      NetworkInterface device = allDevices.nextElement();
      _testCreateUDPSocket("localhost", device, 1024, false);
      _testCreateUDPSocket("127.0.0.1", device, 2048, false);
      _testCreateUDPSocket("[::1]",     device, 4096, false);
      _testCreateUDPSocket(null,        device, 8192, false);

      if (!VRTConfig.TEST_FIRST_MCAST.isEmpty() && device.supportsMulticast()) {
        _testCreateUDPSocket(VRTConfig.TEST_FIRST_MCAST, device, 4096, true);
      }
    }
  }

  public void testCreateTCPSocket () throws Exception {
    // The tests here are very brief and could use more checks in the future (they
    // have not been added thus far since it gets tricky to do them in a portable
    // manner).
    String             host      = "localhost";
    int                port1     = VRTConfig.TEST_FIRST_PORT;
    int                port2     = VRTConfig.TEST_FIRST_PORT + 1;
    String             dev       = null;
    Map<String,Object> options   = new LinkedHashMap<String,Object>();
    String             protocol  = "TCP";
    int                bufLength = 4096;

    options.put("SO_TIMEOUT", 10000); // 10 seconds

    ServerSocket out1 = NetUtilities.createTCPServerSocket(true,  host, port1, dev, options, bufLength);
    ServerSocket in2  = NetUtilities.createTCPServerSocket(false, host, port2, dev, options, bufLength);
    DoAccept     acc1 = new DoAccept(out1, options);
    DoAccept     acc2 = new DoAccept(in2, options);

    new Thread(acc1).start();
    new Thread(acc2).start();

    TCPSocket    in1  = NetUtilities.createTCPSocket(false, host, port1, dev, options, bufLength);
    TCPSocket    out2 = NetUtilities.createTCPSocket(true,  host, port2, dev, options, bufLength);

    assertEquals("acceptTCPSocket(..) -> any errors", null, acc1.error);
    assertEquals("acceptTCPSocket(..) -> any errors", null, acc2.error);

    _testToString(protocol, null, port1, dev, false, NetUtilities.toString(out1)); out1.close();
    _testToString(protocol, null, port1, dev, true,  NetUtilities.toString(out1));
    _testToString(protocol, host, port1, dev, false, in1.toString());              in1.close();
    _testToString(protocol, host, port1, dev, true,  in1.toString());
    _testToString(protocol, host, port2, dev, false, out2.toString());             out2.close();
    _testToString(protocol, host, port2, dev, true,  out2.toString());
    _testToString(protocol, null, port2, dev, false, NetUtilities.toString(in2));  in2.close();
    _testToString(protocol, null, port2, dev, true,  NetUtilities.toString(in2));
  }

  /** Class that will do an accept on a separate thread. */
  private static class DoAccept implements Runnable {
    final    Map<String,?> options;
    final    ServerSocket  serverSocket;
    volatile TCPSocket     clientSocket;
    volatile Throwable     error;

    /** Creates a new instance. */
    DoAccept (ServerSocket s, Map<String,?> o) {
      options      = o;
      serverSocket = s;
      clientSocket = null;
      error        = null;
    }

    @Override
    public void run () {
      try {
        clientSocket = NetUtilities.acceptTCPSocket(serverSocket, options);
      }
      catch (Throwable t) {
        error = t;
      }
    }
  }
}
