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
import java.util.Iterator;
import java.util.Map;
import nxm.vrt.lib.BasicVRLFrame;
import nxm.vrt.lib.BasicVRTPacket;
import nxm.vrt.lib.DataPacket;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.net.NetUtilities.TransportProtocol;

import static nxm.vrt.lib.VRLFrame.MAX_FRAME_LENGTH;
import static nxm.vrt.lib.VRLFrame.NO_CRC_0;
import static nxm.vrt.lib.VRLFrame.NO_CRC_1;
import static nxm.vrt.lib.VRLFrame.NO_CRC_2;
import static nxm.vrt.lib.VRLFrame.NO_CRC_3;
import static nxm.vrt.lib.VRLFrame.VRL_FAW_0;
import static nxm.vrt.lib.VRLFrame.VRL_FAW_1;
import static nxm.vrt.lib.VRLFrame.VRL_FAW_2;
import static nxm.vrt.lib.VRLFrame.VRL_FAW_3;
import static nxm.vrt.lib.VRTPacket.MAX_PACKET_LENGTH;
import static nxm.vrt.net.NetUtilities.getMTU;
import static nxm.vrt.net.NetUtilities.getSocketOptionBool;
import static nxm.vrt.net.NetUtilities.getSocketOptionInt;

/** Writes VRT packets which may optionally be included in VRL frames. The choice
 *  between un-framed VRT packets and using VRL frames must be made at the time
 *  the class is instantiated. If frames are used, there is also an option to
 *  use CRC protection for the frames. <br>
 *  <br>
 *  This implements {@link Threadable} to support use of TCP is used in "server
 *  mode" (i.e. opening a TCP server socket to accept connections). When in this
 *  mode the thread is used to support the acceptance of "client" connections.
 *  When a packet is sent any connected "clients" will receive a copy of the
 *  packet (if none are connected the packet is just discarded).
 */
public final class VRTWriter extends NetIO {
  private final BasicVRTPacket       packet;           // Output packet to use (null if n/a)
  private final BasicVRLFrame        frame;            // Output frame to use (null if n/a)
  private final boolean              crc;              // Include CRC with output frame?
  private final int                  frameBreak;       // The VRL frame break (in octets)

  /** Creates a new instance. The options provided to control the socket and
   *  connection may include any of the "Socket Creation Options" listed
   *  at the top of {@link NetUtilities} plus the following:
   *  <pre>
   *    VRL_FRAME  - Use VRL frames when sending VRT packets.  [Default = false]
   *    VRL_CRC    - Include CRC with VRL frames sent.         [Default = false]
   *    VRL_BREAK  - The number of octets before splitting a multi-packet
   *                 send into multiple frames. This can also take one of
   *                 the following special values:
   *                   0 = Send all packets individually
   *                  -1 = Use default
   *                  -2 = Use default based on protocol   (~60 KiB for UDP)
   *                  -3 = Use default based on MTU        (usually ~1,500 octets)
   *  </pre>
   *  It is suggested that users start with the default set of options provided
   *  by {@link NetUtilities#getDefaultSocketOptions}. Starting with an empty
   *  map (or simply passing in null) will use the o/s defaults, which may not
   *  be ideal for use with VITA-49 streams.
   *  @param transport The transport to use.
   *  @param host      The host to connect to (null = wildcard address).
   *  @param port      The port number (null = any port).
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
   *  @throws IOException If an I/O exception occurs.
   */
  public VRTWriter (TransportProtocol transport, String host, Integer port, String device,
                    VRTEventListener listener, Map<String,?> options) throws IOException {
    super(true, transport, host, port, device, listener, options,
          (getSocketOptionBool(options, "VRL_FRAME", false))? MAX_FRAME_LENGTH : MAX_PACKET_LENGTH);

    if (getSocketOptionBool(options, "VRL_FRAME", false)) {
      // Need to init the header and trailer of the frame so it is valid
      buffer[ 0] = VRL_FAW_0;  buffer[ 1] = VRL_FAW_1;  buffer[ 2] = VRL_FAW_2;  buffer[ 3] = VRL_FAW_3;
      buffer[ 4] = 0;          buffer[ 5] = 0;          buffer[ 6] = 0;          buffer[ 7] = 3;
      buffer[ 8] = NO_CRC_0;   buffer[ 9] = NO_CRC_1;   buffer[10] = NO_CRC_2;   buffer[11] = NO_CRC_3;

      this.packet = null;
      this.frame  = new BasicVRLFrame(buffer, 0, buffer.length, false, true);
      this.crc    = getSocketOptionBool(options, "VRL_CRC", false);
    }
    else {
      this.packet = new BasicVRTPacket(buffer, 0, buffer.length, false, true);
      this.frame  = null;
      this.crc    = false; // n/a for packet
    }

    int vrlBreak = getSocketOptionInt(options, "VRL_BREAK", -1);
    int mtu;
    switch (transport) {
      case TCP_SERVER: mtu = getMTU(tcpServerSocket.getInetAddress());                break;
      case TCP_CLIENT: mtu = getMTU(tcpSockets.get(0).getSocket().getLocalAddress()); break;
      case UDP:        mtu = getMTU(datagramSocket);                                  break;
      default:         throw new IllegalArgumentException("Unknown transport type "+transport);
    }

    switch (vrlBreak) {
      default: this.frameBreak = vrlBreak;  break;  // user-specified
      case  0: this.frameBreak = 0;         break;  // special mode
      case -1: this.frameBreak = 61440;     break;  // 60 KiB
      case -2: this.frameBreak = mtu - 128; break;  // MTU - 128 for headers
    }
  }

  /** Updates the counter in the given packet. */
  private void updateCounter (VRTPacket p) {
    if (p instanceof DataPacket) {
      p.setPacketCount(dataPacketCounter % 16);
      dataPacketCounter++;
    }
    else {
      Integer streamID = p.getStreamIdentifier();
      Integer count    = ctxPacketCounters.getOrDefault(streamID, 0);
      p.setPacketCount(count % 16);
      ctxPacketCounters.put(streamID, count+1);
    }
  }

  /** Sends a packet in a logical frame or frames (where applicable). The actual
   *  transmitted packet will have the packet count updated as necessary, however
   *  the packet object passed in will not be altered. This method does not block
   *  until the packet has been sent. To see the actual packet sent and
   *  verification of sending, call <tt>addSentPacketListener(..)</tt> to add
   *  the appropriate listener.
   *  @param p The packet to send.
   *  @throws IOException If an I/O exception occurs.
   */
  @SuppressWarnings("deprecation")
  public void sendPacket (VRTPacket p) throws IOException {
    if (frame == null) _sendVRTPacket(p);
    else               _sendVRLFrame(p);
  }

  /** Sends a packet in a logical frame or frames (where applicable). This is
   *  identical to calling <tt>sendPacket(p)</tt> and is more efficient than
   *  calling {@link #sendPackets(VRTPacket...)} with a single packet.
   *  @param packet The packets to send.
   *  @throws IOException If an I/O exception occurs.
   */
  public void sendPackets (VRTPacket packet) throws IOException {
    sendPacket(packet);
  }

  /** Sends a set of packets in a logical frame or frames (where applicable).
   *  The actual transmitted packet will have the packet count updated as
   *  necessary, however the packet object passed in will not be altered. This
   *  method does not block until the packet has been sent. To see the actual
   *  packet sent and verification of sending, call
   *  <tt>addSentPacketListener(..)</tt> to add the appropriate listener.
   *  @param packets The packets to send.
   *  @throws IOException If an I/O exception occurs.
   */
  @SuppressWarnings("deprecation")
  public void sendPackets (VRTPacket ... packets) throws IOException {
    if (packets.length == 0) {
      // nothing to send
    }
    else if (frame == null) {
      // Send individual packets
      for (VRTPacket p : packets) {
        _sendVRTPacket(p);
      }
    }
    else if (frameBreak == 0) {
      // Send packets (1 per-frame)
      for (VRTPacket p : packets) {
        _sendVRLFrame(p);
      }
    }
    else {
      // Send packets (1+ per-frame)
      _sendVRLFrame(packets);
    }
  }

  /** Sends the packet (no VRL frame). */
  private void _sendVRTPacket (VRTPacket p) throws IOException {
    // SANITY CHECKS
    if (p == null) throw new NullPointerException("Can not send null.");
    String err = p.getPacketValid(true);
    if (err != null) throw new IllegalArgumentException(err);

    // FIT PACKET INTO DATAGRAM
    p.readPacket(buffer);

    // UPDATE COUNTERS (our copy only)
    updateCounter(packet);

    // SEND THE PACKET
    sendBuffer(packet.getPacketLength(), "VRTPacket", packet);

    // REPORT SENT PACKET
    listenerSupport.fireSentPacket(packet);
  }

  /** Sends the packet(s) in a VRL frame.
   *  @param packets The packets to send.
   *  @throws IOException If an I/O exception occurs.
   */
  private void _sendVRLFrame (VRTPacket ... packets) throws IOException {
    // FIT PACKETS INTO FRAME/DATAGRAM
    int count = frame.setVRTPackets(frameBreak, packets);
    if (count < 1) {
      frame.setVRTPackets(MAX_FRAME_LENGTH, packets[0]);
      count = 1;
    }

    // UPDATE COUNTERS (our copy only) AND CRC (if applicable)
    Iterator<VRTPacket> packetsInFrame = frame.iteratorRW();
    while (packetsInFrame.hasNext()) {
      updateCounter(packetsInFrame.next());
    }
    frame.setFrameCount(frameCounter & 0xFFF);
    frameCounter++;
    if (crc) frame.updateCRC();

    // SEND THE FRAME
    sendBuffer(frame.getFrameLength(), "VRLFrame", count, packets);

    // REPORT SENT PACKETS
    listenerSupport.fireSentPackets(frame);

    // SEND ANY PACKETS THAT DID NOT FIT IN THIS FRAME
    int needToSend = packets.length-count;
    if (needToSend == 1) {
      _sendVRLFrame(packets[count]);
    }
    else if (needToSend > 1) {
      VRTPacket[] next = Arrays.copyOfRange(packets, count, needToSend);
      _sendVRLFrame(next);
    }
  }

  /** <b>Internal Use Only:</b> Sends a raw buffer out. This is only called by
   *  {@link VRTReader} when the two are linked in "echo mode" as is required
   *  to support some of the networking tests.
   *  @param buf The data to send.
   *  @param len The length of the data to send.
   */
  @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch"})
  void sendRawBuffer (byte[] buf, int len) {
    try {
      System.arraycopy(buf, 0, buffer, 0, len);
      super.sendBuffer(len);
    }
    catch (Exception e) {
      listenerSupport.fireErrorOccurred("Unable to send raw buffer to "+hostAddr, e);
    }
  }

  /** Sends the UDP datagram *or* TCP buffer of data. */
  @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch"})
  private void sendBuffer (int len, String type, VRTPacket packet) {
    try {
      super.sendBuffer(len);
    }
    catch (Exception e) {
      listenerSupport.fireErrorOccurred("Unable to send "+type+" to "+hostAddr, e, packet);
    }
  }

  /** Sends the UDP datagram *or* TCP buffer of data. */
  @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch"})
  private void sendBuffer (int len, String type, int count, VRTPacket[] packets) {
    try {
      super.sendBuffer(len);
    }
    catch (Exception e) {
      for (int i = 0; i < count; i++) {
        listenerSupport.fireErrorOccurred("Unable to send "+type+" to "+hostAddr, e, packets[i]);
      }
    }
  }

  /** <b>Internal Use Only:</b> Pipes the output from the specified reader and
   *  dumps it out from this writer. Typical usage is to create a XYZ to VRT
   *  translation service:
   *  <pre>
   *    XYZReader r = XYZReader.startReader(..);
   *    VRTWriter w = VRTWriter.startWriter(..);
   *
   *    r.addErrorWarningListener(this);     // assumes this implements VRTEventListener
   *    w.addErrorWarningListener(this);     // assumes this implements VRTEventListener
   *
   *    w.pipeFrom(r);
   *
   *    // wait until done
   *
   *    r.close();
   *    w.close();
   *  </pre>
   *  @param r The reader to read from.
   */
  @SuppressWarnings("deprecation")
  public void pipeFrom (MulticastReader r) {
    Translator t = new Translator();
    r.addErrorWarningListener(t);
    r.addReceivedPacketListener(t);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Nested classes
  //////////////////////////////////////////////////////////////////////////////////////////////////
  /** <b>Internal Use Only:</b> Provides translation support for VRT packets. */
  private class Translator implements VRTEventListener {
    @Override
    public void receivedPacket (VRTEvent e, VRTPacket p) {
      try {
        sendPacket(p);
      }
      catch (IOException ex) {
        listenerSupport.fireErrorOccurred("Error sending "+p, ex);
      }
    }
  }
}
