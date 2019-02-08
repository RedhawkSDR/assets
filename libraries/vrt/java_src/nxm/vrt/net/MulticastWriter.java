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

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import nxm.vrt.net.NetUtilities.TransportProtocol;

import static nxm.vrt.net.NetUtilities.MAX_UDP_LEN;

/** Writes UDP/Multicast datagrams. <br>
 *  <br>
 *  <b>Warning: Since Oct 2013 the use of {@link MulticastWriter} as a parent
 *  class to {@link VRTWriter} is deprecated since {@link VRTWriter} now
 *  supports both UDP/Unicast and TCP. Additionally, the <tt>&lt;T&gt;</tt>
 *  parameter on the class is deprecated.</b>
 */
public class MulticastWriter<T> extends NetIO {
  /** @deprecated Since 2013. Replaced by {@link NetUtilities#EXTRA_BUFFER_SIZE}. */
  @Deprecated
  public static final int PACKET_GAP = NetUtilities.EXTRA_BUFFER_SIZE;

  /** Creates a new instance.
   *  @param host      The host to connect to (null = wildcard address).
   *  @param port      The port number.
   *  @param device    The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
   *  @param listener  The primary listener *and* the listener used to handle
   *                   any warnings during startup (if null, any startup warnings
   *                   will print to the console).
   *  @param lbuf      The buffer length to use.
   *  @throws IOException If an I/O exception occurs.
   *  @deprecated Since Oct 2013. Replaced by:
   *              <tt>MulticastWriter(host, port, device, listener, options, lbuf)</tt>
   */
  @Deprecated
  protected MulticastWriter (String host, int port, String device, VRTEventListener listener, int lbuf) throws IOException {
    super(true, TransportProtocol.UDP, host, port, device, listener, null, lbuf);
  }

  /** Creates a new instance. The options provided to control the socket and
   *  connection may include any of the "Socket Creation Options" listed
   *  at the top of {@link NetUtilities}. <br>
   *  <br>
   *  It is suggested that users start with the default set of options provided
   *  by {@link NetUtilities#getDefaultSocketOptions}. Starting with an empty
   *  map (or simply passing in null) will use the o/s defaults, which may not
   *  be ideal for use with VITA-49 streams.
   *  @param host      The host to connect to (null = wildcard address).
   *  @param port      The port number.
   *  @param device    The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
   *  @param listener  The primary listener *and* the listener used to handle
   *                   any warnings during startup (if null, any startup warnings
   *                   will print to the console).
   *  @param options   Map of options to set (can be null).
   *  @param lbuf      The buffer length to use.
   *  @throws IOException If an I/O exception occurs.
   */
  protected MulticastWriter (String host, int port, String device, VRTEventListener listener,
                             Map<String,?> options, int lbuf) throws IOException {
    super(true, TransportProtocol.UDP, host, port, device, listener, options, lbuf);
  }

  /** <b>Internal Use Only:</b> Only used by VRTWriter, will be removed once this
   *  is no longer listed as a parent class for VRTWriter.
   */
  MulticastWriter (boolean isOutput, TransportProtocol transport,
                   String host, int port, String device, VRTEventListener listener,
                   Map<String,?> options, int lbuf) throws IOException {
    super(isOutput, transport, host, port, device, listener, options, lbuf);
  }

  /** Sends a raw byte buffer out over the socket.
   *  @param packet The packet to send.
   *  @throws IOException If an I/O exception occurs.
   */
  public void sendPacket (byte[] packet) throws IOException {
    System.arraycopy(packet, 0, buffer, 0, packet.length);
    sendBuffer(packet.length);
  }

  /** Sends a UDP datagram packet.
   *  @param packet The packet to send.
   *  @throws IOException If an I/O exception occurs.
   *  @deprecated Since Oct 2013 (replaced by {@link #sendPacket(byte[])})
   */
  @Deprecated
  public void sendPacket (T packet) throws IOException {
    if (packet instanceof byte[]) {
      sendPacket((byte[])packet);
    }
    else {
      throw new UnsupportedOperationException("sendPacket(T) is not supported");
    }
  }

  /** Sends multiple UDP datagram packets.
   *  @param packets The packets to send.
   *  @throws IOException If an I/O exception occurs.
   *  @deprecated Since Oct 2013 (replaced by {@link #sendPacket(byte[])})
   */
  @Deprecated
  @SuppressWarnings("unchecked")
  public void sendPackets (final T ... packets) throws IOException {
    for (T p : packets) {
      sendPacket(p);
    }
  }

  /** @deprecated Since Oct 2013 (replaced by <tt>sendBuffer(..)</tt>. */
  @Deprecated
  @SuppressWarnings("unchecked")
  protected void sendDatagram (int len, T ... packets) {
    try {
      super.sendBuffer(len);
    }
    catch (Exception e) {
      listenerSupport.fireErrorOccurred("Unable to send "+Arrays.toString(packets), e);
    }
  }

  /** Creates a new instance that writes a <tt>byte[]</tt> holding the packet data.
   *  @param hostPort  The host:port to connect to.
   *  @param device    The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
   *  @param warnings  The listener to handle any warnings. This is only used for handling warnings
   *                   during startup, and is discarded immediately following. If this is null, any
   *                   warnings will print to the console.
   *  @throws IOException If an I/O exception occurs.
   *  @deprecated Since Oct 2013 (replaced by {@link MulticastWriter} which is no longer abstract)
   */
  @Deprecated
  public static MulticastWriter<byte[]> getBinaryMulticastWriter (String hostPort, String device,
                                                                  VRTEventListener warnings) throws IOException {
    return getBinaryMulticastWriter(NetUtilities.getHost(hostPort), NetUtilities.getPort(hostPort), device, warnings);
  }

  /** Creates a new instance that writes a <tt>byte[]</tt> holding the packet data.
   *  @param host      The host to connect to (null = wildcard address).
   *  @param port      The port number.
   *  @param device    The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
   *  @param warnings  The listener to handle any warnings. This is only used for handling warnings
   *                   during startup, and is discarded immediately following. If this is null, any
   *                   warnings will print to the console.
   *  @throws IOException If an I/O exception occurs.
   *  @deprecated Since Oct 2013 (replaced by {@link MulticastWriter} which is no longer abstract)
   */
  @Deprecated
  public static MulticastWriter<byte[]> getBinaryMulticastWriter (String host, int port, String device,
                                                                  VRTEventListener warnings) throws IOException {
    return new MulticastWriter<byte[]>(host, port, device, warnings, MAX_UDP_LEN);
  }
}
