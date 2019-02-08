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
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.util.Map;
import nxm.vrt.net.NetUtilities.TransportProtocol;

import static nxm.vrt.net.NetUtilities.MAX_UDP_LEN;

/** Reads UDP/Multicast datagrams. <br>
 *  <br>
 *  <b>Warning: Since Oct 2013 the use of {@link MulticastReader} as a parent
 *  class to {@link VRTReader} is deprecated since {@link VRTReader} now
 *  supports both UDP/Unicast and TCP.</b>
 */
public class MulticastReader extends NetIO {
  /** @deprecated Since 2011. Replaced by configurable socket properties. */
  @Deprecated
  public static final int QUEUE_LIMIT = 1024;

  /** @deprecated Since 2013. Replaced by {@link NetUtilities#EXTRA_BUFFER_SIZE}. */
  @Deprecated
  public static final int PACKET_GAP          = NetUtilities.EXTRA_BUFFER_SIZE;

  /** @deprecated Since 2013. Replaced by configurable socket property. */
  @Deprecated
  public static final int QUEUE_LIMIT_PACKETS = 1024;

  /** @deprecated Since 2013. Replaced by configurable socket property. */
  @Deprecated
  public static final int QUEUE_LIMIT_OCTETS  = 8388608; // 8,388,608 = 8 MiB

  protected final PacketTranslator packetTranslator;

  /** Creates a new instance.
   *  @param host       The host to connect to (null = wildcard address).
   *  @param port       The port number.
   *  @param device     The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
   *  @param listener   The primary listener *and* the listener used to handle
   *                    any warnings during startup (if null, any startup warnings
   *                    will print to the console).
   *  @throws IOException If an I/O execution occurs.
   *  @deprecated Since Oct 2013. Replaced by:
   *              <tt>MulticastReader(host, port, device, listener, options, lbuf)</tt>
   */
  @Deprecated
  public MulticastReader (String host, int port, String device,
                          VRTEventListener listener) throws IOException {
    this(host, port, device, listener, null, -1);
  }

  /** Creates a new instance.
   *  @param host       The host to connect to (null = wildcard address).
   *  @param port       The port number.
   *  @param device     The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
   *  @param listener   The primary listener *and* the listener used to handle
   *                    any warnings during startup (if null, any startup warnings
   *                    will print to the console).
   *  @param lbuf       The buffer length to use.
   *  @throws IOException If an I/O execution occurs.
   *  @deprecated Since Oct 2013. Replaced by:
   *              <tt>MulticastReader(host, port, device, listener, options)</tt>
   */
  @Deprecated
  protected MulticastReader (String host, int port, String device,
                             VRTEventListener listener, int lbuf) throws IOException {
    this(host, port, device, listener, null, lbuf);
  }

  /** Creates a new instance. The options provided to control the socket and
   *  connection may include any of the "Socket Creation Options" listed
   *  at the top of {@link NetUtilities} plus the following:
   *  <pre>
   *    INITIAL_TIMEOUT     - The approximate timeout (in seconds) applied when
   *                          checking for the initial packets. If the complete
   *                          initial context is not received before the first
   *                          packet received after this time has elapsed, it
   *                          will be reported as an error if appropriate). This
   *                          can also take one of the following special values:
   *                            -1 = Unlimited wait time.
   *                            -2 = Use "legacy mode" (pre 2011 version of library)
   *    QUEUE_LIMIT_PACKETS - Maximum receive queue size in number of packets
   *                          [Default = 1024]
   *    QUEUE_LIMIT_OCTETS  - Maximum receive queue size in octets (total of all
   *                          packets in queue) [Default = 8 MiB]
   *  </pre>
   *  It is suggested that users start with the default set of options provided
   *  by {@link NetUtilities#getDefaultSocketOptions}. Starting with an empty
   *  map (or simply passing in null) will use the o/s defaults, which may not
   *  be ideal for use with VITA-49 streams.
   *  @param host      The host to connect to (null = wildcard address).
   *  @param port      The port number.
   *  @param device    The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
   *  @param listener  The primary listener *and* the listener used to handle any warnings during
   *                   startup (if null, any startup warnings will print to the console). After
   *                   startup, this will be registered the same as calling:
   *                   <pre>
   *                     if (listener != null) {
   *                       addErrorWarningListener(listener);
   *                       addSentPacketListener(listener,null);
   *                     }
   *                   </pre>
   *  @param options   Map of options to set (can be null).
   *  @param lbuf      The buffer length to use.
   *  @throws IOException If an I/O execution occurs.
   */
  public MulticastReader (String host, int port, String device, VRTEventListener listener,
                          Map<String,?> options, int lbuf) throws IOException {
    this(host, port, device, listener, options, lbuf, null);
  }

  /** Creates a new instance. This is identical to the "normal" constructor
   *  but with the addition of a {@link PacketTranslator} that can be used for
   *  translating a received buffer into set of VRT packets.
   *  @param host      The host to connect to (null = wildcard address).
   *  @param port      The port number.
   *  @param device    The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
   *  @param listener  The primary listener *and* the listener used to handle any warnings during
   *                   startup (if null, any startup warnings will print to the console).
   *  @param options   Map of options to set (can be null).
   *  @param lbuf      The buffer length to use.
   *  @param trans     The packet translator to use.
   *  @throws IOException If an I/O execution occurs.
   */
  public MulticastReader (String host, int port, String device, VRTEventListener listener,
                          Map<String,?> options, int lbuf, PacketTranslator trans) throws IOException {
    super(false, TransportProtocol.UDP, host, port, device, listener, options,
          (lbuf < 0)? MAX_UDP_LEN : lbuf);
    packetTranslator = trans;
  }

  /** <b>Internal Use Only:</b> Only used by VRTWriter, will be removed once this
   *  is no longer listed as a parent class for VRTWriter.
   */
  MulticastReader (boolean isOutput, TransportProtocol transport,
                   String host, int port, String device, VRTEventListener listener,
                   Map<String,?> options, int lbuf) throws IOException {
    super(isOutput, transport, host, port, device, listener, options,
          (lbuf < 0)? MAX_UDP_LEN : lbuf);
    packetTranslator = null;
  }

  /** Reads a single packet/frame/etc from the input stream. This method can not
   *  be used when the reader is running as a thread.
   *  @return The packet received.
   *  @throws IOException If an exception occurs, common exceptions include
   *                      {@link SocketTimeoutException} and {@link PortUnreachableException}.
   */
  public byte[] receivePacket () throws IOException {
    return receiveBuffer(false);
  }

  @Override
  protected void handle (byte[] buffer, int length) throws IOException {
    if (packetTranslator != null) {
      try {
        packetTranslator.translate(listenerSupport, buffer, length);
      }
      catch (Exception e) {
        throw new IOException("Error translating packet of type ", e);
      }
      catch (Error t) {
        throw t;
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // NESTED CLASSES
  //////////////////////////////////////////////////////////////////////////////////////////////////
  /** Handles the translation of a raw packet stream into a set of
   *  {@link nxm.vrt.lib.VRTPacket VRTPackets}.
   */
  public static interface PacketTranslator {
    /** Translates a raw packet to a {@link nxm.vrt.lib.VRTPacket}.
     *  @param vls    Listener support object, used to pass along errors/warnings.
     *  @param buffer The raw input data/packet.
     *  @param length The length of the input data/packet.
     *  @throws Exception If there is any exception in processing (this is optional since the
     *          handler can directly pass it to the <tt>vls</tt>.
     */
    public void translate (VRTListenerSupport vls, byte[] buffer, int length) throws Exception;
  }
}
