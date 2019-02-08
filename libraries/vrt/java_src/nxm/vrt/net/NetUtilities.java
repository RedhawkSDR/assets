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

package nxm.vrt.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import nxm.vrt.lib.Utilities;
import nxm.vrt.lib.VRTConfig;

/** Various utilities methods.
 *
 *  <h3>Socket Creation Options</h3>
 *  The socket creation methods take in a map of options used for initializing a
 *  socket. If the map is null, or if a given entry is null, the default value
 *  will be used. The map may include any of the <tt>java.net.StandardSocketOptions</tt>
 *  from Java 7 in "string form" for example:
 *  <pre>
 *    options.put(StandardSocketOptions.SO_SNDBUF.name(), bufSize);   // Java 7+
 *    options.put("SO_SNDBUF",                            bufSize);   // Java 6+
 *  </pre>
 *  It may also include the following options:
 *  <pre>
 *    CONNECT_TIMEOUT - The connection timeout for a TCP {@link Socket} in ms.
 *    LOCAL_PORT      - The local port to use for a TCP client connection (this
 *                      overrides the default which is to use any available
 *                      ephemeral port).
 *    RCVBUF_EAGER    - Override the normal o/s behavior and force the receive
 *                      buffer to be eagerly initialized when a UDP socket
 *                      (unicast/multicast) is opened. The default o/s behavior
 *                      on some systems (particularly Linux) is to do a lazy
 *                      initialization of the receive buffer for a UDP socket.
 *                      When an lazy initialization is done, the receive buffer
 *                      might not be initialized until (or possibly after) the
 *                      first receive call is made, resulting in packet loss
 *                      during the period between the open and the actual buffer
 *                      initialization. Setting this to true attempts to force
 *                      the o/s to initialize the receive buffer as part of the
 *                      open (or at least the open within this class); however,
 *                      there is no guarantee that the o/s will allocate it at
 *                      that time. Setting this to false will use the default
 *                      o/s behavior, which is generally faster. <i>(Note that
 *                      there is no SNDBUF_EAGER since there is no reason to
 *                      buffer output packets until the first one is sent.)</i>
 *    SERVER_BACKLOG  - The backlog value for a TCP {@link ServerSocket}
 *  </pre>
 *  Any options not applicable for the given socket type will be ignored. Note
 *  that <tt>IP_MULTICAST_IF</tt> is also ignored since this is explicitly set
 *  via the "device" input parameter. Additionally <tt>SO_SNDBUF</tt>,
 *  <tt>SO_RCVBUF</tt>, <tt>SO_REUSEADDR</tt>, and <tt>SO_TIMEOUT</tt> may be
 *  given defaults that differ from the O/S defaults where applicable (however
 *  these defaults can be overridden via the socket options provided).
 */
public final class NetUtilities {
  private NetUtilities() { } // prevent instantiation

  /** Describes a network protocol. */
  public static enum TransportProtocol {
    /** A TCP server-side socket (if sending) and a client-side socket (if
     *  receiving). Use {@link #TCP_SERVER} or {@link #TCP_CLIENT} to override
     *  the automatic socket type selection.
     */
    TCP,
    /** A TCP server-side socket (note that in this case "server" means that
     *  this side will wait for connections, not that it is responsible for
     *  sending the data).
     */
    TCP_SERVER,
    /** A TCP client-side socket (note that in this case "client" means that
     *  this side will make the connection to the "server," not that it is
     *  responsible for receiving the data).
     */
    TCP_CLIENT,
    /** A UDP socket. This may be a Unicast or Multicast (the exact type of
     *  which will be determined based on the IP address given).
     */
    UDP;
  };

  /** Maximum length of a UDP packet (including the UDP header) in octets (65,635).
   *  @see #MAX_IPv4_UDP_LEN
   */
  public static final int MAX_UDP_LEN = 65535;

  /** Maximum length of a UDP packet (including the UDP header) in octets when
   *  transmitted over IPv4 (65,471). Although the max packet length for UDP is
   *  65,635 bytes, IPv4 imposes a lower limit:
   *  <pre>
   *    Maximum IPv4 Length:        65,535 octets
   *    Maximum IPv4 Header:      -     60 octets  (20 octets is minimum)
   *    -----------------------------------------
   *    Maximim UDP in IPv4:        65,475 octets
   *  </pre>
   *  Accordingly this value should be thought of as the maximum for transmission
   *  size and {@link #MAX_UDP_LEN} should be used as the maximum receive size.
   *  @see #MAX_UDP_LEN
   */
  public static final int MAX_IPv4_UDP_LEN = 65475;

  /** The length of a UPD packet header in octets (8). */
  public static final int UDP_HEADER_LENGTH = 8;

  /** <b>Internal Use Only:</b> Fudge factor to use when sizing buffers that
   *  work with UDP datagrams to account for size of datagram header, plus the
   *  IP header, etc.
   */
  public static final int EXTRA_BUFFER_SIZE = 80;

  /** Enable work-around for Java Bug 4701650 on Windows? */
  private static final boolean WINDOWS_BUG_FIX
                       = System.getProperty("os.name","").startsWith("Windows");

  /** Default SO_TIMEOUT in ms. We intentionally default to a non-zero value
   *  so that we never get stuck in a blocking call.
   */
  private static final Integer DEF_SO_TIMEOUT = 1000;

  /** Default SO_REUSEADDR. We intentionally set this to true so the Java
   *  and C++ versions will match (Java tends to default SO_REUSEADDR to true
   *  despite the O/S setting being false as has been observed with Java 7 on
   *  RHEL 6.1). Additionally, we've always defaulted this to true with
   *  UDP/Multicast since that is almost always the correct choice.
   */
  private static final Boolean DEF_SO_REUSEADDR = true;


  /** Gets the device name applicable when given a NIC and VLAN.
   *  @param nic  The NIC to use  ("" if n/a).
   *  @param vlan The VLAN to use (-1 if n/a).
   *  @return The applicable device identifier (e.g. "eth1", "eth0.123").
   *  @throws IllegalArgumentException if a VLAN is specified without a NIC
   */
  public static String getNetworkDeviceName (String nic, int vlan) {
    if (vlan < 0) return nic;

    if ((nic == null) || nic.isEmpty()) {
      throw new IllegalArgumentException("Can not specify VLAN of "+vlan+" without specifying a NIC");
    }
    return nic + "." +vlan;
  }

  /** Gets the host name from a host:port.
   *  @param hostPort The host:port in "host:port", "host" or ":port" syntax.
   *  @return The applicable host name or null if not specified.
   *  @throws UnknownHostException If the host name is invalid.
   */
  public static InetAddress getHostAddress (String hostPort) throws UnknownHostException {
    String host = getHost(hostPort);
    return (host == null)? null : InetAddress.getByName(host);
  }

  /** Gets the host name from a host:port.
   *  @param hostPort The host:port in "host:port", "host" or ":port" syntax.
   *  @return The applicable host name or null if not specified.
   */
  public static String getHost (String hostPort) {
    if (hostPort.startsWith("[")) {
      // IPv6 address
      int i = hostPort.indexOf(']');
      return hostPort.substring(0,i+1);
    }
    int i = hostPort.lastIndexOf(':');
    if (i <  0) return hostPort; // no port, just a host name
    if (i == 0) return null;     // no host, just a port number
    return hostPort.substring(0,i);
  }

  /** Gets the port number from a host:port.
   *  @param hostPort The host:port in "host:port", "host" or ":port" syntax.
   *  @return The applicable port number or null if not specified.
   */
  public static Integer getPort (String hostPort) {
    if (hostPort.startsWith("[")) {
      // IPv6 address
      int i = hostPort.indexOf("]:");
      if (i < 0) return null; // no port, just a host name
      return Integer.valueOf(hostPort.substring(i+2));
    }
    int i = hostPort.lastIndexOf(':');
    if (i <  0) return null; // no port, just a host name
    return Integer.valueOf(hostPort.substring(i+1));
  }

  /** Converts host+port to a socket address. */
  private static InetSocketAddress toAddress (String host, int port) {
    return (host == null)? new InetSocketAddress(port)
                         : new InetSocketAddress(host,port);
  }

  /** Converts device name to a device object. */
  private static NetworkInterface toInterface (String device) throws IOException {
    return (device == null)? null : NetworkInterface.getByName(device);
  }

  /** Converts device name to an address. */
  private static InetAddress toInterfaceAddr (String device) throws IOException {
    NetworkInterface dev = toInterface(device);
    if (dev == null) return null;

    Enumeration<InetAddress> allAddresses = dev.getInetAddresses();
    InetAddress              firstAddress = null;

    while (allAddresses.hasMoreElements()) {
      InetAddress _addr = allAddresses.nextElement();
      if (firstAddress == null) {
        firstAddress = _addr;
      }
      if ((_addr instanceof Inet6Address) == VRTConfig.PREFER_IPV6_ADDRESSES) {
        return _addr;
      }
    }
    if (firstAddress != null) {
      return firstAddress;
    }
    throw new IOException("Could not find address for "+device);
  }

  /** Converts device name to an address. */
  private static NetworkInterface toInterface (InetAddress addr) throws IOException {
    if (addr == null) return null;

    Enumeration<NetworkInterface> devices = NetworkInterface.getNetworkInterfaces();

    while (devices.hasMoreElements()) {
      NetworkInterface         dev   = devices.nextElement();
      Enumeration<InetAddress> addrs = dev.getInetAddresses();

      while (addrs.hasMoreElements()) {
        InetAddress a = addrs.nextElement();
        if (a.equals(addr)) return dev;
      }
    }
    throw new IOException("Could not find interface for "+addr);
  }

// ONLY EXPERIMENTAL... WILL PROBABLY DELETE
//import java.io.File;
//import java.io.FileInputStream;
//import nxm.vrt.lib.AsciiCharSequence;
//  /** <b>Internal Use Only:</b> Gets the FIN timeout value (in sec) applicable
//   *  for a TCP connection on this host -- if unknown returns a default value
//   *  of 120.
//   */
//  public static int getFinTimeout () {
//    try {
//      if (true) {
//        // This works on most versions of Linux...
//        File f = new File("/proc/sys/net/ipv4/tcp_fin_timeout");
//        if (f.exists()) {
//          InputStream in   = new FileInputStream(f);
//          byte[]      data = new byte[80];
//          int         len  = in.read(data);
//          String      val  = AsciiCharSequence.wrap(data,0,len,true).toString().trim();
//System.out.println(">>> FIN Timeout = '"+val+"'");
//          return Integer.valueOf(val);
//        }
//      }
//    }
//    catch (Exception e) {
//      e.printStackTrace();
//      // ignore and return the default value (below)
//    }
//    return 120;
//  }

  /** <b>Internal Use Only:</b> Gets the MTU applicable for a given connection. */
  static int getMTU (InetAddress addr) {
    try {
      NetworkInterface dev = toInterface(addr);
      return (dev == null)? 1500 : dev.getMTU();
    }
    catch (Exception e) {
      return 1500; // Ethernet default
    }
  }

  /** <b>Internal Use Only:</b> Gets the MTU applicable for a given connection. */
  static int getMTU (DatagramSocket socket) {
    try {
      if (socket instanceof MulticastSocket) {
        MulticastSocket  msocket = (MulticastSocket)socket;
        NetworkInterface dev     = msocket.getNetworkInterface();
        if (dev == null) return 1500; // IP default

        if (dev.getName() == null) {
          // This is a work-around for Java Bug 9007874 which results in a
          // SIGSEGV within the Java VM when getMTU() is called.
          return 1500;
        }

        return dev.getMTU();
      }
      else {
        return 1500; // IP default
      }
    }
    catch (Exception e) {
      return 1500; // IP default
    }
  }

  /** Gets a set of default socket options that can be added to. This allows a
   *  easy way of getting "default" options that may be slightly more optimal
   *  than the ones provided by the o/s. <br>
   *  <br>
   *  <b>WARNING: The "default" options provided by this function may change
   *  between releases and may vary from platform to platform within the same
   *  release.</b>
   *  @param optimize  Select more aggressive optimizations that assume high-rate
   *                   VITA-49 traffic (true) or defer to o/s defaults where the
   *                   benefit to "general" traffic is unclear (false). Setting
   *                   this to true may enable options that optimize performance
   *                   for streaming-data at the expense of slower connect/open
   *                   times and/or may be inappropriate for non-packetized data.
   *  @param transport Specific transport to use for any transport-specific
   *                   optimizations (can be null). <i>(This value is only
   *                   considered with respect to optimizations that require
   *                   differentiation between transport options.)</i>
   *  @return The set of options to use (never null).
   */
  public static Map<String,Object> getDefaultSocketOptions (boolean optimize,
                                                            TransportProtocol transport) {
    Map<String,Object> options = new LinkedHashMap<String,Object>(16);

    if (optimize) {
      options.put("RCVBUF_EAGER", true);
      options.put("TCP_NODELAY",  true);
      options.put("VRL_CRC",      false);
    }
    else {
      // Use o/s default for RCVBUF_EAGER
      // Use o/s default for TCP_NODELAY
      options.put("VRL_CRC",      false);
    }

    return options;
  }

  /** Creates a new datagram socket. This function is a convenience function
   *  that creates the applicable {@link InetSocketAddress} and
   *  {@link NetworkInterface} (as applicable) before calling
   *  {@link #createSocket(InetSocketAddress,NetworkInterface,VRTEventListener,int)}.
   *  @param host      The host to connect to (null = wildcard address).
   *  @param port      The port number.
   *  @param device    The device to connect to (e.g. "eth0", "eth1.101")
   *                   (null = use default).
   *  @param warnings  The listener to handle any warnings. This is only used
   *                   for handling warnings during startup, and is discarded
   *                   immediately following. If this is null, any warnings
   *                   will print to the console.
   *  @param bufLength The buffer length in bytes.
   *  @return The socket.
   *  @throws IOException If an I/O error occurs.
   *  @deprecated Since Oct 2013 (only reliably supports UDP/Multicast sockets)
   *              Replaced by: {@link #createUDPSocket}.
   */
  @Deprecated
  public static DatagramSocket createSocket (String host, int port, String device,
                                             VRTEventListener warnings, int bufLength)
                                            throws IOException {
    return createSocket(toAddress(host,port), toInterface(device), warnings, bufLength);
  }

  /** Creates a new datagram socket. The applicable datagram socket is initialized
   *  with the following parameters:
   *  <pre>
   *    SO_RCVBUF    = 8 MiB    (ask for 8 MiB, but will probably get closer to 128 KiB)
   *    SO_SNDBUF    = 8 MiB    (ask for 8 MiB, but will probably get closer to 128 KiB)
   *    SO_REUSEADDR = true
   *    SO_TIMEOUT   = 10000 ms
   *  </pre>
   *  The type of datagram socket is based on the input address. If the input
   *  address is a Multicast address, {@link MulticastSocket} is used, otherwise
   *  {@link DatagramSocket} is used (the determination of "Multicast" is done
   *  using <tt>address.getAddress().isMulticastAddress()</tt>).
   *  @param address   The address to connect to.
   *  @param device    The network interface to use (null to use default).
   *  @param warnings  The listener to handle any warnings. This is only used
   *                   for handling warnings during startup, and is discarded
   *                   immediately following. If this is null, any warnings
   *                   will print to the console.
   *  @param bufLength The buffer length in bytes.
   *  @return The socket.
   *  @throws IOException If an I/O error occurs.
   *  @deprecated Since Oct 2013 (only reliably supports UDP/Multicast sockets,
   *              also the documented SO_* settings don't match the actual
   *              implementation) Replaced by: {@link #createUDPSocket}.
   */
  @Deprecated
  public static DatagramSocket createSocket (InetSocketAddress address, NetworkInterface device,
                                             VRTEventListener warnings, int bufLength)
                                            throws IOException {
    DatagramSocket  socket;
    MulticastSocket msocket;

    boolean multicast = (address.getAddress() != null) && address.getAddress().isMulticastAddress();

    if (multicast) {
      // Java bugs 6347853 and 6218155 (which are actually references to Linux bugs) can cause a
      // BindException on some systems if -Djava.net.preferIPv4Stack=true is not set due to a bug
      // in the IPv6 protocol stack.
      try {
        if (WINDOWS_BUG_FIX) {
          socket = msocket = new MulticastSocket(address.getPort());
        }
        else {
          socket = msocket = new MulticastSocket(address);
        }
      }
      catch (BindException e) {
        if (System.getProperty("java.net.preferIPv4Stack") == null) {
          throw new IOException(""+e.getMessage()+" (some Linux versions have a bug in their IPv6 "
                  + "protocol stack and require -Djava.net.preferIPv4Stack=true to be set to avoid it)", e);
        }
        else {
          throw e;
        }
      }

      if (device != null) {
        msocket.setNetworkInterface(device);
        msocket.setReuseAddress(true);
        msocket.joinGroup(address, device);
      }
      else {
        msocket.setReuseAddress(true);
        msocket.joinGroup(address.getAddress());
      }
    }
    else {
      socket = new DatagramSocket(address);
    }

    socket.setReceiveBufferSize(128*1024*1024); // <-- Ask for 128MiB, but will probably get less than this
    socket.setSendBufferSize(128*1024*1024);    // <-- Ask for 128MiB, but will probably get less than this
    socket.setReuseAddress(true);
    socket.setSoTimeout(10000);

    return socket;
  }

  /** Creates a new UDP datagram socket.
   *  @param isOutput  Is this socket being created for output (true) or
   *                   input (false)?
   *  @param host      The host to connect to (null = wildcard address).
   *  @param port      The port number.
   *  @param device    The device to connect to (e.g. "eth0", "eth1.101")
   *                   (null = use default).
   *  @param options   Map of options to set (can be null). See top of class
   *                   for details.
   *  @param bufLength The buffer length in bytes.
   *  @return The socket.
   *  @throws IOException If an I/O error occurs.
   */
  public static DatagramSocket createUDPSocket (boolean isOutput, String host, int port,
                                                String device, Map<String,?> options,
                                                int bufLength) throws IOException {
    InetSocketAddress address   = toAddress(host,port);
    boolean           multicast = (address.getAddress() != null) && address.getAddress().isMulticastAddress();
    DatagramSocket    socket;

    if (multicast) {
      MulticastSocket msocket;
      try {
        if (isOutput) {
          socket = msocket = new MulticastSocket();
        }
        else if (WINDOWS_BUG_FIX) {
          // work-around for Java Bug 4701650 on Windows
          socket = msocket = new MulticastSocket(address.getPort());
        }
        else {
          socket = msocket = new MulticastSocket(address);
        }
      }
      catch (BindException e) {
        // Java bugs 6347853 and 6218155 (which are actually references to Linux bugs) can cause a
        // BindException on some systems if -Djava.net.preferIPv4Stack=true is not set due to a bug
        // in the IPv6 protocol stack.
        if (System.getProperty("java.net.preferIPv4Stack") == null) {
          throw new IOException(""+e.getMessage()+" (some Linux versions have a bug in their IPv6 "
                  + "protocol stack and require -Djava.net.preferIPv4Stack=true to be set to avoid it)", e);
        }
        else {
          throw e;
        }
      }

      if (device != null) {
        msocket.setNetworkInterface(toInterface(device));
      }

      if (isOutput) {
        // no join required
      }
      else if (device != null) {
        msocket.joinGroup(address, toInterface(device));
      }
      else {
        msocket.joinGroup(address.getAddress());
      }
    }
    else if (isOutput) {
      InetAddress devAddress = toInterfaceAddr(device);
      if (devAddress == null) devAddress = InetAddress.getLocalHost();
      socket = new DatagramSocket(new InetSocketAddress(devAddress, 0)); // 0=select any port
    }
    else {
      InetAddress devAddress = toInterfaceAddr(device);
      if (devAddress == null) devAddress = InetAddress.getLocalHost();
      socket = new DatagramSocket(new InetSocketAddress(devAddress, port));
    }

    if (isOutput) initSocket(socket, options, bufLength+EXTRA_BUFFER_SIZE, null);
    else          initSocket(socket, options, null, bufLength+EXTRA_BUFFER_SIZE);

    if (!isOutput && getSocketOptionBool(options, "RCVBUF_EAGER", false)) {
      // Attempt to force "eager" initialization of the receive buffer (see
      // description for RCVBUF_EAGER at top of class) via an early call to
      // receive (which will frequently time out). Note that we have observed
      // that (at least in Java) a timeout that is too small (e.g. 1 ms) can
      // let the o/s return on a time-out prior to allocating the buffer.
      int timeout = socket.getSoTimeout();
      socket.setSoTimeout(100);
      try {
        byte[]         buf = new byte[MAX_UDP_LEN];
        DatagramPacket pkt = new DatagramPacket(buf, 0, MAX_UDP_LEN);
        socket.receive(pkt);
      }
      catch (SocketTimeoutException e) {
        // ignore
      }
      socket.setSoTimeout(timeout);
    }
    return socket;
  }

  /** Creates a new TCP Server socket.
   *  @param isOutput  Is this socket being created for output (true) or
   *                   input (false)?
   *  @param host      Should be null (included for consistency with other methods).
   *  @param port      The port number.
   *  @param device    The device to connect to (e.g. "eth0", "eth1.101")
   *                   (null = use default).
   *  @param options   Map of options to set (can be null). See top of class
   *                   for details.
   *  @param bufLength The buffer length in bytes.
   *  @return The socket.
   *  @throws IOException If an I/O error occurs.
   */
  public static ServerSocket createTCPServerSocket (boolean isOutput, String host, int port,
                                                    String device, Map<String,?> options,
                                                    int bufLength) throws IOException {
    int               backlog   = getSocketOptionInt(options, "SERVER_BACKLOG", -1);
    InetSocketAddress localAddr = new InetSocketAddress(toInterfaceAddr(device), port);

    ServerSocket socket = initSocket(new ServerSocket(), options);
    socket.bind(localAddr, backlog);
    return socket;
  }

  /** Creates a new TCP (Client) socket.
   *  @param isOutput  Is this socket being created for output (true) or
   *                   input (false)?
   *  @param host      The host to connect to (null = wildcard address).
   *  @param port      The port number.
   *  @param device    The device to connect to (e.g. "eth0", "eth1.101")
   *                   (null = use default).
   *  @param options   Map of options to set (can be null). See top of class
   *                   for details.
   *  @param bufLength The buffer length in bytes.
   *  @return The socket.
   *  @throws IOException If an I/O error occurs.
   */
  public static TCPSocket createTCPSocket (boolean isOutput,  String host, int port,
                                           String device, Map<String,?> options,
                                           int bufLength) throws IOException {
    int               timeout   = getSocketOptionInt(options, "CONNECT_TIMEOUT", 10000);
    int               localPort = getSocketOptionInt(options, "LOCAL_PORT",          0);
    InetSocketAddress address   = toAddress(host,port);
    InetSocketAddress localAddr = new InetSocketAddress(toInterfaceAddr(device), localPort);

    Socket s = initSocket(new Socket(), options);
    s.bind(localAddr);
    s.connect(address, timeout);
    return new TCPSocket(s);
  }

  /** Initializes a UDP socket. */
  private static DatagramSocket initSocket (DatagramSocket s, Map<String,?> opts,
                                            Integer recvBuf, Integer sendBuf) throws IOException {
    // Not supported: IP_MULTICAST_IF
    if (s instanceof MulticastSocket) {
      MulticastSocket m = (MulticastSocket)s;

      if (hasOpt(opts, "IP_MULTICAST_LOOP")) m.setLoopbackMode(getSocketOptionBool(opts, "IP_MULTICAST_LOOP"));
      if (hasOpt(opts, "IP_MULTICAST_TTL" )) m.setTimeToLive(getSocketOptionInt(opts, "IP_MULTICAST_TTL"));
    }

    if (hasOpt(opts, "IP_TOS"      )) s.setTrafficClass(getSocketOptionInt(opts, "IP_TOS"));
    if (hasOpt(opts, "SO_BROADCAST")) s.setBroadcast(getSocketOptionBool(opts, "SO_BROADCAST"));

    if (hasOpt(opts, "SO_RCVBUF")) {
      s.setReceiveBufferSize(getSocketOptionInt(opts, "SO_RCVBUF"));
    }
    else if (recvBuf != null) {
      s.setReceiveBufferSize(recvBuf);
    }

    if (hasOpt(opts, "SO_SNDBUF")) {
      s.setSendBufferSize(getSocketOptionInt(opts, "SO_SNDBUF"));
    }
    else if (sendBuf != null) {
      s.setSendBufferSize(sendBuf);
    }

    if (hasOpt(opts, "SO_REUSEADDR")) {
      s.setReuseAddress(getSocketOptionBool(opts, "SO_REUSEADDR"));
    }
    else if (DEF_SO_REUSEADDR != null) {
      s.setReuseAddress(DEF_SO_REUSEADDR);
    }

    if (hasOpt(opts, "SO_TIMEOUT")) {
      s.setSoTimeout(getSocketOptionInt(opts, "SO_TIMEOUT"));
    }
    else if (DEF_SO_TIMEOUT != null) {
      s.setSoTimeout(DEF_SO_TIMEOUT);
    }

    return s;
  }

  /** Initializes a TCP server socket. */
  private static ServerSocket initSocket (ServerSocket s, Map<String,?> opts) throws IOException {
    if (hasOpt(opts, "SO_RCVBUF"   )) s.setReceiveBufferSize(getSocketOptionInt(opts, "SO_RCVBUF"));

    if (hasOpt(opts, "SO_REUSEADDR")) {
      s.setReuseAddress(getSocketOptionBool(opts, "SO_REUSEADDR"));
    }
    else if (DEF_SO_REUSEADDR != null) {
      s.setReuseAddress(DEF_SO_REUSEADDR);
    }

    if (hasOpt(opts, "SO_TIMEOUT")) {
      s.setSoTimeout(getSocketOptionInt(opts, "SO_TIMEOUT"));
    }
    else if (DEF_SO_TIMEOUT != null) {
      s.setSoTimeout(DEF_SO_TIMEOUT);
    }
    return s;
  }

  /** Initializes a TCP socket. */
  private static Socket initSocket (Socket s, Map<String,?> opts) throws IOException {
    if (hasOpt(opts, "IP_TOS"      )) s.setTrafficClass(getSocketOptionInt(opts, "IP_TOS"));
    if (hasOpt(opts, "SO_KEEPALIVE")) s.setKeepAlive(getSocketOptionBool(opts, "SO_KEEPALIVE"));
    if (hasOpt(opts, "TCP_NODELAY" )) s.setTcpNoDelay(getSocketOptionBool(opts, "TCP_NODELAY"));
    if (hasOpt(opts, "SO_SNDBUF"   )) s.setSendBufferSize(getSocketOptionInt(opts, "SO_SNDBUF"));
    if (hasOpt(opts, "SO_RCVBUF"   )) s.setReceiveBufferSize(getSocketOptionInt(opts, "SO_RCVBUF"));

    if (hasOpt(opts, "SO_REUSEADDR")) {
      s.setReuseAddress(getSocketOptionBool(opts, "SO_REUSEADDR"));
    }
    else if (DEF_SO_REUSEADDR != null) {
      s.setReuseAddress(DEF_SO_REUSEADDR);
    }

    if (hasOpt(opts, "SO_TIMEOUT")) {
      s.setSoTimeout(getSocketOptionInt(opts, "SO_TIMEOUT"));
    }
    else if (DEF_SO_TIMEOUT != null) {
      s.setSoTimeout(DEF_SO_TIMEOUT);
    }

    if (hasOpt(opts, "SO_LINGER")) {
      int linger = getSocketOptionInt(opts, "SO_LINGER");
      s.setSoLinger((linger >= 0), linger);
    }
    return s;
  }

  /** Obtains and initializes a client socket connection.
   *  @param server  The server socket.
   *  @param options The options to initialize the socket with.
   *  @return The initialized socket or null if a {@link SocketTimeoutException}
   *          occurs before a connection is made.
   *  @throws IOException If any exceptions (other than a {@link SocketTimeoutException})
   *                      occur.
   */
  public static TCPSocket acceptTCPSocket (ServerSocket server, Map<String,?> options) throws IOException {
    try {
      Socket s = initSocket(server.accept(), options);
      return new TCPSocket(s);
    }
    catch (SocketTimeoutException e) {
      return null;
    }
  }

  /** <b>Internal Use Only:</b> Converts InetAddress/port to string builder. */
  private static StringBuilder toStringBuilder (InetAddress addr, int port) {
    StringBuilder str  = new StringBuilder(48);
    if (addr == null) {
      // do nothing
    }
    else if (addr instanceof Inet6Address) {
      str.append('[').append(addr.getHostAddress()).append(']');
    }
    else {
      str.append(addr.getHostAddress());
    }
    str.append(':');
    str.append(port);
    return str;
  }

  /** Converts an interface address to a user-friendly string. */
  private static String toStringIF (InetAddress addr) {
    if ((addr == null) || addr.isAnyLocalAddress()) {
      return "default interface";
    }

    try {
      return toInterface(addr).getName();
    }
    catch (Exception e) {
      return "unknown interface";
    }
  }

  /** Converts a socket definition to a user-friendly string. Note that the
   *  syntax of the output value may change in future releases.
   *  @param socket The socket.
   *  @return A free-form description of the socket in its current state.
   */
  public static String toString (ServerSocket socket) {
    if (socket == null   ) return null;
    if (socket.isClosed()) return "closed TCP server socket";

    StringBuilder str   = toStringBuilder(null, socket.getLocalPort());
    SocketAddress saddr = socket.getLocalSocketAddress();
    InetAddress   addr  = null;

    if (saddr instanceof InetSocketAddress) {
      addr = ((InetSocketAddress)saddr).getAddress();
    }
    str.append(" (TCP Server) using ").append(toStringIF(addr));
    return str.toString();
  }

  /** Converts a socket definition to a user-friendly string. Note that the
   *  syntax of the output value may change in future releases.
   *  @param socket The socket.
   *  @return A free-form description of the socket in its current state.
   */
  public static String toString (TCPSocket socket) {
    return (socket == null)? null : socket.toString();
  }

  /** Converts a socket definition to a user-friendly string. Note that the
   *  syntax of the output value may change in future releases.
   *  @param socket The socket.
   *  @return A free-form description of the socket in its current state.
   */
  public static String toString (Socket socket) {
    if (socket == null   ) return null;
    if (socket.isClosed()) return "closed TCP server socket";

    StringBuilder str  = toStringBuilder(socket.getInetAddress(), socket.getPort());
    InetAddress   addr = socket.getLocalAddress();

    str.append(" (TCP) using ").append(toStringIF(addr));
    return str.toString();
  }

  /** Converts a socket definition to a user-friendly string. Note that the
   *  syntax of the output value may change in future releases.
   *  @param socket The socket.
   *  @return A free-form description of the socket in its current state.
   */
  public static String toString (DatagramSocket socket) {
    if (socket == null   ) return null;
    if (socket.isClosed()) return "closed UDP socket";

    StringBuilder str = toStringBuilder(socket.getLocalAddress(), socket.getLocalPort());
    if (socket instanceof MulticastSocket) {
      MulticastSocket msocket = (MulticastSocket)socket;

      str.append(" (UDP/Multicast)");
      try {
        NetworkInterface dev = msocket.getNetworkInterface();
        if (dev == null) str.append(" using default interface");
        else             str.append(" using ").append(dev.getName());
      }
      catch (Exception e) {
        str.append(" using unknown interface");
      }
    }
    else {
      InetAddress addr = socket.getLocalAddress();
      str.append(" (UDP) using ").append(toStringIF(addr));
    }
    return str.toString();
  }


  /** <b>Internal Use Only:</b> Is a socket option present? */
  private static boolean hasOpt (Map<?,?> options, String key) { // version with shorter name
    return (options != null) && options.containsKey(key);
  }

  /** <b>Internal Use Only:</b> Gets a socket option as a boolean. */
  static boolean getSocketOptionBool (Map<?,?> options, String key) {
    return Utilities.toBooleanValue(options.get(key));
  }

  /** <b>Internal Use Only:</b> Gets a socket option as an int. */
  static int getSocketOptionInt (Map<?,?> options, String key) {
    Object val = options.get(key);
    return (val instanceof Number)? ((Number)val).intValue()
                                  : Integer.parseInt(val.toString());
  }

  /** <b>Internal Use Only:</b> Gets a socket option as a boolean. */
  static boolean getSocketOptionBool (Map<?,?> options, String key, boolean def) {
    return (hasOpt(options, key))? getSocketOptionBool(options,key) : def;
  }

  /** <b>Internal Use Only:</b> Gets a socket option as an int. */
  static int getSocketOptionInt (Map<?,?> options, String key, int def) {
    return (hasOpt(options, key))? getSocketOptionInt(options,key) : def;
  }


  /** <b>Internal Use Only:</b> A TCP socket container with open in/out streams. */
  public static final class TCPSocket implements Closeable {
    private final Socket       socket;     // The underlying socket
    private final InputStream  in;         // The socket input stream
    private final OutputStream out;        // The socket output stream

    TCPSocket (Socket s) throws IOException {
      this.socket = s;
      this.in     = s.getInputStream();
      this.out    = s.getOutputStream();
    }

    @Override
    public String toString () {
      return NetUtilities.toString(socket);
    }

    @Override
    public synchronized void close () throws IOException {
      socket.close();
    }

    /** Stops the socket's i/o but does not close it. This will stop the low-level
     *  socket from receiving any more data, but will still permit reads until the
     *  low-level buffers are empty. Following a call to this, a call to read for
     *  which there is no data will return EOF rather than 0.
     *  @throws IOException If there is an error while during socket shutdown.
     */
    public void shutdownSocket () throws IOException {
      socket.shutdownInput();
      socket.shutdownOutput();
    }

    /** Gets the socket. */
    public Socket getSocket () {
      return socket;
    }

    /** Reads from the input stream.
     *  @param buf The buffer to read into.
     *  @param off The offset into the buffer.
     *  @param len The maximum number of octets to read.
     *  @return The number of octets read in (-1 if EOF, -2 if socket closed, -3
     *          if socket input closed).
     *  @throws IOException If there is an error while during read.
     */
    public int read (byte[] buf, int off, int len) throws IOException {
      if (socket.isClosed()       ) return -2;
      if (socket.isInputShutdown()) return -3;
      return in.read(buf, off, len);
    }

    /** Writes to the output stream.
     *  @param buf The buffer to write from.
     *  @param off The offset into the buffer.
     *  @param len The maximum number of octets to write.
     *  @return The number of octets written out (-2 if socket closed, -3
     *          if socket input closed).
     *  @throws IOException If there is an error while during write.
     */
    public int write (byte[] buf, int off, int len) throws IOException {
      if (socket.isClosed()        ) return -2;
      if (socket.isOutputShutdown()) return -3;
      out.write(buf, off, len);
      return len;
    }

  }
}
