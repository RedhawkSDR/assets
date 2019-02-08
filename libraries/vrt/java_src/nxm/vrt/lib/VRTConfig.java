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

package nxm.vrt.lib;

import java.io.IOException;
import java.io.InputStream;
import nxm.vrt.lib.VRTPacket.PacketType;

import static nxm.vrt.lib.BasicVRLFrame.isVRL;
import static nxm.vrt.lib.Utilities.toBooleanValue;

/** Global settings for the VRT libraries. Most of these can be controlled from
 *  the <tt>java</tt> command line with the following properties:
 *  <pre>
 *    vrt.dslt_service=[host:port]          - <b>Internal Use Only:</b> The host:port for the
 *                                            default Data Source Lookup Table (DSLT) service.
 *                                            The default is 237.88.29.0:1049.
 *
 *    vrt.dslt_device=[device]              - <b>Internal Use Only:</b> The device (e.g. "eth0")
 *                                            to use for the default Data Source Lookup Table
 *                                            (DSLT) service. If unspecified the the default
 *                                            (null) device is used.
 *
 *    vrt.ignore_env_var=[true/false]       - Ignore environment variables and only check the
 *                                            properties on the java command line (default is
 *                                            false). <i>(This environment variable capability
 *                                            was added Jan 2012, setting this to true matches
 *                                            the previous ver.)</i>
 *
 *    vrt.leap_seconds=[file]               - The name of the leap seconds file to use (must be a
 *                                            path relative to the CLASSPATH). The default is
 *                                            "nxm/vrt/dat/tai-utc.dat". <i>(Prior to Jan 2012
 *                                            this defaulted to "nxm/vrt/dat/tai_utc_data.txt".)</i>
 *
 *    vrt.norad_ls_counted=[true/false]     - Turns on/off the default mode for counting mid-year
 *                                            leap seconds in NORAD time (default is on).
 *
 *    vrt.packet_factory=[factory]          - Sets the packet factory. The [factory] must be the
 *                                            fully-qualified class name and the class must have
 *                                            a no-argument constructor. (This can be altered at
 *                                            run time.)
 *
 *    vrt.packet_factory=[f0]:[f1]:...:[fN] - Sets the packet factory. Each entry must be the
 *                                            fully-qualified name of a packet factory. The zero'th
 *                                            entry the list must have a no-argument constructor.
 *                                            Subsequent entries must have a one-argument
 *                                            constructor that takes in a parent PacketFactory as
 *                                            its argument. (This can be altered at run time.)
 *
 *    vrt.prefer_ipv6_addresses=[true/false]- Turns on preferences to use IPv6 over IPv4 (e.g.
 *                                            "localhost" becomes "::1" vs "127.0.0.1"), the
 *                                            default is to prefer IPv4 (false). This setting is
 *                                            exclusive to C++ since Java users must set the
 *                                            "java.net.preferIPv6Addresses" and/or
 *                                            "java.net.preferIPv4Stack" as these are handled
 *                                            at the Java VM level (in Java).
 *
 *    vrt.strict=[true/false]               - Turns on/off strict checking of input packets
 *                                            (default is off).
 *
 *    vrt.test_delay=[ms]                   - <b>Testing Use Only:</b> Sets the delay between sets of
 *                                            networking tests to permit socket close-out. Leave blank
 *                                            (default) or set to 0 to skip any waits.
 *
 *    vrt.test_device=[dev]                 - <b>Testing Use Only:</b> Sets the local network device
 *                                            to use while testing (e.g. "eth0"). Leave blank (default)
 *                                            to let o/s select the device.
 *
 *    vrt.test_first_mcast=[addr]           - <b>Testing Use Only:</b> Sets the UDP/Multicast address
 *                                            range to use as part of the network tests. This
 *                                            will be an implied range of 8 addresses starting
 *                                            at the given address. Setting this to "" will
 *                                            disable any tests that require using UDP/Multicast
 *                                            (default is "").
 *
 *    vrt.test_first_port=[port]            - <b>Testing Use Only:</b> Sets the port numbers to use
 *                                            as part of the network tests. This will be an
 *                                            implied range of 10 ports starting at the given
 *                                            port number. Setting this to 0 will disable any
 *                                            tests that require opening a port (default is 0).
 *
 *    vrt.test_quick=[true/false]           - <b>Testing Use Only:</b> Turns on/off "quick test" mode
 *                                            which abbreviates some of the test cases in order
 *                                            to speed up the testing process (default is on).
 *                                            In "quick test" mode the tests generally take less
 *                                            than a minute to completel; when not in "quick
 *                                            test" mode the tests can take 15+ minutes.
 *
 *    vrt.test_server=[host:port]           - Sets the host:port to run the server on. This should
 *                                            *not* specify "localhost" as the local-host interface
 *                                            (e.g. "lo0") is generally incompatible with some of
 *                                            the test protocols.
 *
 *    vrt.test_server_timeout=[sec]         - Sets the test server's timeout in seconds. This is
 *                                            present to ensure that the server is shutdown when
 *                                            done, even if the client-side terminates abnormally.
 *
 *    vrt.version=[V49/V49b]                - Sets the protocol version to use. Valid values are:
 *                                              "V49"  = VITA 49.0
 *                                              "V49b" = VITA 49.0b
 *  </pre>
 *  If <tt>vrt.ignore_env_var</tt> is <b>not</b> set to true on the Java command
 *  line, this will look at the environment variables for any properties not found.
 *  This allows compatibility with the C++ library for mixed Java/C++ environments.
 *  Note that the environment variables use all-uppercase with the "VRT_" prefix
 *  rather than "vrt." (e.g. "VRT_VERSION" for "vrt.version"). <br>
 *  <br>
 *  Except where explicitly stated above, these values are constants that <b>can
 *  not</b> be altered after the program is started. <br>
 *  <br>
 *  Any values marked "Testing Use Only" are used to support the internal unit
 *  tests and networking tests. They do not affect normal use of the library and
 *  may change without notice in future releases.
 */
public final class VRTConfig {
  private VRTConfig () { } // prevent instantiation

  /** Looks up a "system" property returning a default if not found. */
  private static String getProperty (String key, String def) {
    // ==== JAVA PROPERTY ======================================================
    String val = System.getProperty(key);
    if (val != null) return val;

    // ==== ENVIRONMENT VARIABLE ===============================================
    try {
      boolean ignore = toBooleanValue(System.getProperty("vrt.ignore_env_var","false"));
      if (!ignore) {
        String var = key.toUpperCase().replace('.', '_'); // change "vrt.version" to "VRT_VERSION"
        String env = System.getenv(var);
        if (env != null) return env;
      }
    }
    catch (Exception e) {
      // ignore
    }

    // ==== OTHER ==============================================================
    // In the future this may include special handling for applets, etc.

    // ==== USE DEFAULT ========================================================
    return def;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  //  Version
  //////////////////////////////////////////////////////////////////////////////////////////////////
  /** Supported protocol versions. (<tt>V49</tt> = VITA 49.0, <tt>V49b</tt> = VITA 49.0b) */
  public static enum VITAVersion { V49, V49b };

  /** The version number for the library, always {@value} with this version. */
  public static final String LIBRARY_VERSION = "0.9.0";

  /** Gets the protocol version to use (<tt>vrt.version</tt>). */
  public static final VITAVersion VRT_VERSION = VITAVersion.valueOf(getProperty("vrt.version", "V49"));

  //////////////////////////////////////////////////////////////////////////////////////////////////
  //  Testing
  //////////////////////////////////////////////////////////////////////////////////////////////////
  /** The delay between sets of networking tests (<tt>VRT_TEST_DELAY</tt>). */
  public static final long TEST_DELAY = Long.parseLong(getProperty("vrt.test_delay","0"));

  /** The local network device to use (<tt>VRT_TEST_DEVICE</tt>). */
  public static final String TEST_DEVICE = getProperty("vrt.test_device","");

  /** Gives the first (of 8) multicast addresses to use for testing purposes (""=disable) (<tt>vrt.test_first_mcast</tt>). */
  public static final String TEST_FIRST_MCAST = getProperty("vrt.test_first_mcast","");

  /** Gives the first (of 10) port to use for testing purposes (0=disable) (<tt>vrt.test_first_port</tt>). */
  public static final int TEST_FIRST_PORT = Integer.parseInt(getProperty("vrt.test_first_port","0"));

  /** Enable quicker unit tests (<tt>vrt.test_quick</tt>). */
  public static final boolean TEST_QUICK = toBooleanValue(getProperty("vrt.test_quick","true"));

  /** The test server to use (<tt>vrt.test_server</tt>). */
  public static final String TEST_SERVER = getProperty("vrt.test_server","");

  /** The test server timeout in seconds (<tt>vrt.test_server_timeout</tt>). */
  public static final int TEST_SERVER_TIMEOUT = Integer.parseInt(getProperty("vrt.test_server_timeout","3600"));

  //////////////////////////////////////////////////////////////////////////////////////////////////
  //  Miscellaneous
  //////////////////////////////////////////////////////////////////////////////////////////////////
  /** Enable strict checks (<tt>vrt.strict</tt>). */
  public static final boolean STRICT = toBooleanValue(getProperty("vrt.strict","false"));

  /** Prefer IPv6 over IPv4. This is set indirectly via the Java VM
   *  <tt>java.net.preferIPv4Stack</tt> and <tt>java.net.preferIPv6Addresses</tt>
   *  settings.
   */
  public static final boolean PREFER_IPV6_ADDRESSES;
  static {
    boolean preferIPv4Stack     = toBooleanValue(getProperty("java.net.preferIPv4Stack",    "true"));
    boolean preferIPv6Addresses = toBooleanValue(getProperty("java.net.preferIPv6Addresses","false"));
    PREFER_IPV6_ADDRESSES = preferIPv6Addresses && !preferIPv4Stack;
  }

  /** Default leap-seconds file (<tt>vrt.leap_seconds</tt>). */
  public static final String LEAP_SECONDS_FILE = getProperty("vrt.leap_seconds","nxm/vrt/dat/tai-utc.dat");

  /** Are leap seconds counted by default in NORAD time? */
  public static final boolean NORAD_LEAP_SEC_COUNTED = toBooleanValue(getProperty("vrt.norad_ls_counted","true"));

  /** Default DSLT service host:port (<tt>vrt.dslt_service</tt>). */
  public static final String DSLT_SERVICE = getProperty("vrt.dslt_service","237.88.29.0:1049");

  /** Default DSLT service device to use (<tt>vrt.dslt_device</tt>). */
  public static final String DSLT_DEVICE = getProperty("vrt.dslt_device",null);

  //////////////////////////////////////////////////////////////////////////////////////////////////
  //  PacketFactory and VRTPacket creation
  //////////////////////////////////////////////////////////////////////////////////////////////////
  private static PacketFactory packetFactory = null;

  static {
    // Unlike C++ there is no option to just do a "new nxm.vrt.libm.PacketFactory()" since this
    // class is loaded when building PacketGen and that creates a circular dependancy with
    // nxm/vrt/libm/PacketFactory.java being an output from PacketGen just prior to building the
    // library
    String prop = getProperty("vrt.packet_factory","nxm.vrt.libm.PacketFactory");
    try {
      if (prop.indexOf(':') > 0) {
        String[] props = prop.split(":");

        packetFactory = (PacketFactory)Class.forName(props[0]).newInstance();
        for (int i = 1; i < props.length; i++) {
          Class<?> c = Class.forName(props[i]);
          @SuppressWarnings("StaticNonFinalUsedInInitialization")
          Object   pf = c.getConstructor(PacketFactory.class).newInstance(packetFactory);
          packetFactory = (PacketFactory)pf;
        }
      }
      else if (prop.length() > 0) {
        packetFactory = (PacketFactory)Class.forName(prop).newInstance();
      }
    }
    catch (Exception e) {
      throw new Error("Invalid property set vrt.packet_factory="+prop, e);
    }
  }

  /** Gets the current packet factory.
   *  @return The current packet factory (not null).
   */
  public static PacketFactory getPacketFactory () {
    return packetFactory;
  }

  /** Sets the current packet factory.
   *  @param pf The current packet factory (not null).
   */
  public static void setPacketFactory (PacketFactory pf) {
    if (pf == null) {
      throw new NullPointerException("Can not set packet factory to null");
    }
    packetFactory = pf;
  }

  /** Internal method, gets applicable packet type.
   *  @param type  The packet type (VRT allows one Data/ExtData and one Context/ExtContext per class).
   *  @param id    The class ID of the packet.
   *  @return The applicable implementation class, or null if none found.
   */
  public static Class<? extends VRTPacket> getPacketClass (PacketType type, Long id) {
    return packetFactory.getPacketClass(type, id);
  }

  /** Gets a specific packet from the factory when given a generic packet.
   *  @param p    The existing (generic) packet.
   *  @param copy Must the data be copied? If true, the resulting packet must copy the data from
   *              <tt>p</tt> in such a way that no changes to the returned packet will alter
   *              <tt>p</tt>. If false, a "fast" initialization is used, meaning that the resulting
   *              packet may or may not be a copy of the given data, the implementation will use the
   *              fastest creation method available.
   *  @return The applicable packet. This will never return null, the result of an unknown packet
   *          may result in a new {@link BasicVRTPacket} or may simply returning <tt>p</tt> (if
   *          <tt>copy</tt> is false).
   *  @throws NullPointerException If <tt>p</tt> is null.
   */
  public static VRTPacket getPacket (VRTPacket p, boolean copy) {
    return packetFactory.getPacket(p, copy);
  }

  /** Gets a specific packet from the factory when given a generic packet.
   *  @param type The packet type (VRT allows one Data/ExtData and one Context/ExtContext per class).
   *  @param id   The class ID of the packet (null if not specified).
   *  @return The applicable packet. This will never return null, the result of an unknown packet
   *          may result in a new {@link BasicVRTPacket}.
   */
  public static VRTPacket getPacket (PacketType type, Long id) {
    return packetFactory.getPacket(type, id);
  }

  /** Creates a new instance based on the given data buffer. Note that when the buffer lengths
   *  are given, only the most minimal of error checking is done. Users should call
   *  {@link VRTPacket#isPacketValid()} to verify that the packet is valid. Invalid packets can
   *  result in unpredictable behavior, but this is explicitly allowed (to the extent possible) so
   *  that applications creating packets can use this class even if the packet isn't yet "valid".<br>
   *  <br>
   *  This function will also detect if a VRL frame was given and return null if that happens.
   *  The {@link #getPackets} method can be used in cases where VRL frames are expected. <br>
   *  <br>
   *  <b>Warning: Use of <tt>direct=true</tt> and <tt>readOnly=false</tt> are dangerous, see
   *  {@link BasicVRTPacket#BasicVRTPacket(byte[],int,int,byte[],int,int,byte[],int,int,boolean,boolean)}
   *  for details.</b>
   *  @param bbuf     The data buffer to use.
   *  @param boff     The byte offset into the bbuf to start reading/writing at.
   *  @param blen     The length of bbuf in bytes (-1=use size in header of packet).
   *  @param readOnly Should users of this instance be able to modify the underlying data buffer?
   *  @param direct   Can the methods in this class directly interact with the buffer or must
   *                  a copy of the data be made so that changes to the buffer don't affect this
   *                  instance?
   *  @return The applicable type of packet.
   *  @throws NullPointerException If the buffer is null.
   *  @throws IllegalArgumentException If the offset is negative or greater than the
   *          length of the buffer given. Also thrown if the buffer length is -1 and a copy
   *          can not be made because the sizes in the header of the packet are invalid.
   */
  public static VRTPacket getPacket (byte[] bbuf, int boff, int blen, boolean readOnly, boolean direct) {
    if (isVRL(bbuf,boff)) {
      return null;
    }
    else {
      BasicVRTPacket p = new BasicVRTPacket(bbuf, boff, blen, readOnly, direct);
      return getPacket(p, false);
    }
  }

  /** Creates a new instance based on the given data buffer. Note that when the buffer lengths
   *  are given, only the most minimal of error checking is done. Users should call
   *  {@link VRTPacket#isPacketValid()} to verify that the packet is valid. Invalid packets can
   *  result in unpredictable behavior, but this is explicitly allowed (to the extent possible) so
   *  that applications creating packets can use this class even if the packet isn't yet "valid".<br>
   *  <br>
   *  This function will also detect if a VRL frame was given and return null if that happens.
   *  The {@link #getPackets} method can be used in cases where VRL frames are expected. <br>
   *  <br>
   *  This function is identical to calling <tt>getPacket(bbuf,boff,blen,false,false)</tt>.
   *  @param bbuf     The data buffer to use.
   *  @param boff     The byte offset into the bbuf to start reading/writing at.
   *  @param blen     The length of bbuf in bytes (-1=use size in header of packet).
   *  @return The applicable type of packet.
   *  @throws NullPointerException If the buffer is null.
   *  @throws IllegalArgumentException If the offset is negative or greater than the
   *          length of the buffer given. Also thrown if the buffer length is -1 and a copy
   *          can not be made because the sizes in the header of the packet are invalid.
   */
  public static VRTPacket getPacket (byte[] bbuf, int boff, int blen) {
    return getPacket(bbuf,boff,blen,false,false);
  }

  /** Creates new instances based on the given data buffer. Users should call
   *  {@link VRTPacket#isPacketValid()} to verify that the packet is valid. Invalid packets can
   *  result in unpredictable behavior, but this is explicitly allowed (to the extent possible) so
   *  that applications creating packets can use this class even if the packet isn't yet "valid".<br>
   *  <br>
   *  This function will also detect if a VRL frame was given, in which case it will return the
   *  packets contained in the frame.
   *  @param bbuf     The data buffer to use.
   *  @param boff     The byte offset into the bbuf to start reading/writing at.
   *  @param blen     The length of bbuf in bytes (-1=use size in header of packet).
   *  @return The packets obtained. For a VRT packet this will be an array of length 1, for a VRL
   *          frame this will be an array of length N (and could be 0).
   *  @throws NullPointerException If the buffer is null.
   *  @throws IllegalArgumentException If the offset is negative or greater than the
   *          length of the buffer given. Also thrown if the buffer length is -1 and a copy
   *          can not be made because the sizes in the header of the packet are invalid.
   */
  public static VRTPacket[] getPackets (byte[] bbuf, int boff, int blen) {
    if (isVRL(bbuf,boff)) {
      BasicVRLFrame f = new BasicVRLFrame(bbuf, boff, blen, true, true);
      return f.getVRTPackets();
    }
    else {
      VRTPacket p = getPacket(bbuf, boff, blen, false, false);
      return new VRTPacket[] { p };
    }
  }

  /** Creates new instances based on the given input stream. The implementation of this function is
   *  such that the behavior is identical to doing (but may include optimizations that are outside
   *  of what is shown here):
   *  <pre>
   *    public static VRTPacket[] getPackets (InputStream in) throws IOException {
   *      VRLFrame f = ...;
   *      f.readFrameFrom(in);
   *      return f.getVRTPackets();
   *    }
   *  </pre>
   *  @param in The input stream.
   *  @return The packets obtained. For a VRT packet this will be an array of length 1, for a VRL
   *          frame this will be an array of length N (and could be 0).
   *  @throws NullPointerException If the buffer is null.
   *  @throws IOException If there is an error reading the stream. In the event of an I/O error, the
   *                      state of the input stream is indeterminate.
   */
  public static VRTPacket[] getPackets (InputStream in) throws IOException {
    BasicVRLFrame f = new BasicVRLFrame();
    f.readFrameFrom(in);
    return f.getVRTPackets();
  }
}
