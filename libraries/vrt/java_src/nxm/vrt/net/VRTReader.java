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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nxm.vrt.lib.BasicVRLFrame;
import nxm.vrt.lib.BasicVRTPacket;
import nxm.vrt.lib.ContextPacket;
import nxm.vrt.lib.ContextPacket.ContextAssocLists;
import nxm.vrt.lib.DataPacket;
import nxm.vrt.lib.VRLFrame;
import nxm.vrt.lib.VRTConfig;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.net.NetUtilities.TransportProtocol;

import static nxm.vrt.lib.BasicVRLFrame.isVRL;
import static nxm.vrt.lib.VRLFrame.NO_CRC_0;
import static nxm.vrt.lib.VRLFrame.NO_CRC_1;
import static nxm.vrt.lib.VRLFrame.NO_CRC_2;
import static nxm.vrt.lib.VRLFrame.NO_CRC_3;
import static nxm.vrt.lib.VRLFrame.VRL_FAW_0;
import static nxm.vrt.lib.VRLFrame.VRL_FAW_1;
import static nxm.vrt.lib.VRLFrame.VRL_FAW_2;
import static nxm.vrt.lib.VRLFrame.VRL_FAW_3;
import static nxm.vrt.net.NetUtilities.MAX_UDP_LEN;
import static nxm.vrt.net.NetUtilities.getSocketOptionInt;
import static nxm.vrt.net.VRTEventListener.NO_CONTEXT_STREAM;
import static nxm.vrt.net.VRTEventListener.NO_DATA_STREAM;

/** Reads VRT packets which may optionally be included in VRL frames. The choice
 *  between VRT packet and VRL frame is made automatically by looking at the
 *  first four bytes received and seeing if they match the {@link VRLFrame#VRL_FAW},
 *  if not a VRT packet is assumed.
 */
public final class VRTReader extends NetIO {
  /** The "timeout" value indicating the default wait of 60.0 sec.
   *  @deprecated Since Oct 2013 (not required by new constructor).
   */
  @Deprecated
  public static final int DEFAULT_TIMEOUT = 60;

  /** The "timeout" value indicating an unlimited wait.
   *  @deprecated Since Oct 2013 (not required by new constructor).
   */
  @Deprecated
  public static final int UNLIMITED_TIMEOUT = -1;

  /** The "timeout" value indicating that "legacy mode" should be used.
   *  @deprecated Since Oct 2013 (not required by new constructor).
   */
  @Deprecated
  public static final int LEGACY_MODE = -2;

  /** The start time indicator that the initial context has already been found. */
  private static final int FOUND_INITIAL = -3;

  private final    long                    timeoutMS;       // timeout in ms (-1=LegacyMode)
  private          long                    startTimeMS;     // start time in ms (0=not started)
  private          DataPacket              initialData;     // data packet marking paired stream
  private          VRTPacket               initialCtx;      // context packet marking paired stream
  private          Map<Integer,VRTPacket>  initialContext;  // all initial context received
  private          Set<Integer>            requiredContext; // all initial context required
  private          Integer                 idContext;       // ID for the paired context
  private          VRTWriter               rawEcho;         // Writer for echoing raw frames/packets

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
  public VRTReader (TransportProtocol transport, String host, int port, String device,
                    VRTEventListener listener, Map<String,?> options) throws IOException {
    super(false, transport, host, port, device, listener, options,
          (transport == TransportProtocol.UDP)? MAX_UDP_LEN : VRLFrame.HEADER_LENGTH);

    int timeout = getSocketOptionInt(options, "INITIAL_TIMEOUT", 60);
    if (timeout == LEGACY_MODE) {
      this.timeoutMS         = LEGACY_MODE;
      this.startTimeMS       = LEGACY_MODE;
      this.initialData       = null;
      this.initialCtx        = null;
      this.initialContext    = null;
      this.requiredContext   = null;
      this.idContext         = null;
      this.dataPacketCounter = -1;
    }
    else {
      this.timeoutMS         = (timeout < 0)? timeout : (timeout * 1000);
      this.startTimeMS       = 0;
      this.initialData       = null;
      this.initialCtx        = null;
      this.initialContext    = new LinkedHashMap<>(8);
      this.requiredContext   = new HashSet<>(8);
      this.idContext         = null;
      this.dataPacketCounter = -1;
    }
  }

  /** <b>Internal Use Only:</b> Enables "echo mode" where raw packets/frames are
   *  echoed out of a corresponding writer. This is here to support the server-side
   *  of some of the networking test cases, it is not for general use.
   *  @param out The writer to forward the echoed data to.
   */
  public void enableTestModeEcho (VRTWriter out) {
    testMode = 1;
    rawEcho  = out;
  }

  @Override
  public void reconnect (VRTEventListener warnings) {
    try {
      super.reconnect(warnings);
    }
    catch (Throwable t) {
      throw new RuntimeException("Error during reconnect", t);
    }
  }

  @Override
  protected void handle (byte[] buffer, int length) throws IOException {
    if (startTimeMS == 0) {
      // start the clock on the first datagram received
      startTimeMS = System.currentTimeMillis();
    }

    if (rawEcho != null) {
      rawEcho.sendRawBuffer(buffer, length);
    }
    else if (isVRL(buffer, 0)) {
      handle(new BasicVRLFrame(buffer, 0, length, false, true));
    }
    else {
      handle(VRTConfig.getPacket(buffer, 0, length, false, true));
    }
  }

  /** Handles a VRLFrame. */
  private void handle (VRLFrame frame) throws IOException {
    String err = frame.getFrameValid(false);

    if (err != null) {
      listenerSupport.fireErrorOccurred(err, null);
      return;
    }
    int count    = frame.getFrameCount();
    int next     = (count+1) & 0xFFF;
    int expected = frameCounter;
    frameCounter = next;

    if ((expected != count) && (expected >= 0)) {
      listenerSupport.fireErrorOccurred("Missed frames "+expected+" (inclusive) to "+count+" (exclusive).", null);
    }

    for (VRTPacket packet : frame) {
      handle(packet);
    }
  }

  /** Handles a VRTPacket. */
  private void handle (VRTPacket packet) throws IOException {
    String err = packet.getPacketValid(false);

    if (err != null) {
      listenerSupport.fireErrorOccurred(err, null);
      return;
    }

    int count     = packet.getPacketCount();
    int next      = (count+1) & 0xF;
    int expected;

    if (packet instanceof DataPacket) {
      expected = dataPacketCounter;
      dataPacketCounter = next;
    }
    else {
      Integer streamID = packet.getStreamIdentifier();
      expected = ctxPacketCounters.getOrDefault(streamID, -1);
      ctxPacketCounters.put(streamID, next);
    }

    if ((count != expected) && (expected != -1)) {
      listenerSupport.fireErrorOccurred("Missed packets "+expected+" (inclusive) to "+count+" (exclusive).", null);
    }

    // SEE IF WE ARE USING LEGACY MODE ========================================
    if (startTimeMS == LEGACY_MODE) {
      listenerSupport.fireReceivedPacket(packet);
      return;
    }

    // SEE IF WE ALREADY FOUND INITIAL CONTEXT ================================
    if (startTimeMS == FOUND_INITIAL) {
      if (packet instanceof DataPacket) {
        listenerSupport.fireReceivedDataPacket((DataPacket)packet);
      }
      else {
        listenerSupport.fireReceivedContextPacket(packet);
      }
      return;
    }

    // STILL NEED MORE CONTEXT (OR INITIAL DATA PACKET) =======================
    long    now     = System.currentTimeMillis();
    boolean timeout = (timeoutMS > 0) && (startTimeMS+timeoutMS <= now);

    // ---- If this is a DataPacket, handle it as such ------------------------
    if (packet instanceof DataPacket) {
      initialData = (DataPacket)packet;
      idContext   = initialData.getStreamIdentifier();

      if (idContext == null) {
        fireReceivedInitialContext(null); // unidentified stream (i.e. done)
        // No initial context -> this can be our first data packet.
        listenerSupport.fireReceivedDataPacket((DataPacket)packet);
      }
      else if (timeout) {
        fireReceivedInitialContext(NO_CONTEXT_STREAM);
        // No initial context -> this can be our first data packet.
        listenerSupport.fireReceivedDataPacket((DataPacket)packet);
      }
      else if (initialCtx == null) {
        // We can now associate the initial context if already received. Prior
        // versions did not do this which presented a situation where the full
        // context was received prior to any data but we don't recognize that
        // until a later context packet comes in. This wasn't a big deal with
        // UDP/Multicast since we were connecting asynchronously. But with
        // UDP/Unicast or TCP is it highly likely the sender will give us any
        // context information first.
        initialCtx = initialContext.get(idContext); // <-- may be null
        requiredContext.add(idContext);

        if (checkInitialContextDone(timeout)) {
          // Initial context is already complete, this can be our first data packet.
          listenerSupport.fireReceivedDataPacket((DataPacket)packet);
        }
      }
      return;
    }

    // ---- Found a context packet --------------------------------------------
    int id = packet.getStreamIdentifier();
    initialContext.put(id, packet);

    // ---- Is this a non-ContextPacket primary stream (rare)? ----------------
    if ((idContext != null) && (id == idContext) && !(packet instanceof ContextPacket)) {
      initialCtx = packet;
      if (initialContext.size() == 1) {
        fireReceivedInitialContext(null);
      }
      else {
        fireReceivedInitialContext("Context packets do not follow stream ID rules (found streams "
                                 + initialContext.keySet() + " but expected only " + id + ").");
      }
      return;
    }

    // ---- For any ContextPackets, check assoc. lists first ------------------
    if (packet instanceof ContextPacket) {
      ContextPacket     ctx   = (ContextPacket)packet;
      ContextAssocLists assoc = ctx.getContextAssocLists();

      if ((idContext != null) && (id == idContext)) {
        // it is the primary, set it and mark as required
        initialCtx = ctx;
        requiredContext.add(id);
      }
      if (assoc != null) {
        for (int val : assoc.getSourceContext()) requiredContext.add(val);
        for (int val : assoc.getSystemContext()) requiredContext.add(val);
      }
    }

    // ---- Check to see if done ----------------------------------------------
    checkInitialContextDone(timeout);
  }

  /** Have we found all of the initial context? If so send it out. */
  private boolean checkInitialContextDone (boolean timeout) {
    boolean foundCtx = (initialCtx != null);
    boolean sameSize = initialContext.size() == requiredContext.size();
    boolean foundAll = initialContext.keySet().containsAll(requiredContext);

    if (foundCtx && foundAll) {
      // found all required context
      if (sameSize) {
        fireReceivedInitialContext(null);
        return true;
      }
      else {
        fireReceivedInitialContext("Context packets do not follow stream ID rules (found streams "
                                 + initialContext.keySet() + " but expected " + requiredContext + ").");
        return true;
      }
    }

    if (timeout && foundCtx) {
      // timeout before finding all required context
      if (sameSize) {
        fireReceivedInitialContext("Context packets do not follow stream ID rules (found streams "
                                 + initialContext.keySet() + " but expected " + requiredContext + ").");
        return true;
      }
      else {
        fireReceivedInitialContext("Timeout before all required context could be found (found streams "
                                  + initialContext.keySet() + " but expected " + requiredContext + ").");
        return true;
      }
    }

    if (timeout) {
      // timeout before finding primary context
      if (initialData == null) {
        fireReceivedInitialContext(NO_DATA_STREAM);
        return true;
      }
      else {
        fireReceivedInitialContext("Could not find IF Context for stream ID "
                                   + initialData.getStreamID() + ".");
        return true;
      }
    }
    return false;
  }

  @Override
  protected byte[] receiveBufferUDP (boolean autoPush) throws IOException {
    // UDP is fairly easy since everything must be within a single datagram
    datagram.setLength(buffer.length); // reset length since receive changes it
    datagramSocket.receive(datagram);

    int numRead = datagram.getLength();
    if (numRead <= 0) return null;

    if (autoPush && (numRead > VRLFrame.MIN_FRAME_LENGTH) && isVRL(buffer,0)) {
      // Rather than creating a new buffer now and then creating another later for
      // each packet in a VRL frame, we can slice the frame up into packets now
      // and reduce the number of memory copies. However, if this fails, just
      // fall through and push the whole thing onto the queue.
      try {
        List<byte[]> packets = BasicVRLFrame.getVRTPackets(buffer, 0, numRead);
        if (packets != null) {
          // Push "empty" frame first so we can still check the frame counters
          byte[] buf = new byte[] { VRL_FAW_0, VRL_FAW_1, VRL_FAW_2, VRL_FAW_3,
                                    0,         0,         0,         3,
                                    NO_CRC_0,  NO_CRC_1,  NO_CRC_2,  NO_CRC_3 };
          buf[ 4] = buffer[4];                  // <-- Copy counter
          buf[ 5] = (byte)(buffer[5] & 0xF0);   // <-- Copy counter
          pushPackets(buf, packets);
          return BUFFER_PUSHED;
        }
      }
      catch (Exception e) {
        // Ignore this for now
      }
    }

    return Arrays.copyOf(buffer, numRead);
  }

  @Override
  protected byte[] receiveBufferTCP (boolean autoPush) throws IOException {
    // TCP is more difficult since there is no underlying framing structure so
    // we need to read some data, figure out the size, and then read the rest.
    // If anything fails mid-way-through we are in trouble and throwing an
    // exception is about the only thing we can do.
    int numRead = _tcpSocketRead(buffer, 0, 4, false);
    if (numRead <= 0) return null;

    boolean isVRL   = isVRL(buffer, 0);
    int     totRead = numRead;
    int     toRead;
    int     len;

    if (isVRL) {
      toRead  = VRLFrame.HEADER_LENGTH - totRead;
      numRead = _tcpSocketRead(buffer, totRead, toRead, true);
      if (numRead < 0) return null;
      totRead += numRead;
      len = BasicVRLFrame.getFrameLength(buffer, 0);
    }
    else {
      len = BasicVRTPacket.getPacketLength(buffer, 0);
    }

    byte[] buf = Arrays.copyOf(buffer, len);

    toRead  = len - totRead;
    numRead = _tcpSocketRead(buf, totRead, toRead, true);

    return (numRead < 0)? null : buf;
  }

  /** Variant of listenerSupport.fireReceivedInitialContext(..) that resets fields when done. */
  private void fireReceivedInitialContext (String errorMsg) {
    listenerSupport.fireReceivedInitialContext(errorMsg, initialData, initialCtx, initialContext);
    startTimeMS     = FOUND_INITIAL;
    initialData     = null;
    initialCtx      = null;
    idContext       = null;
    initialContext  = null;
    requiredContext = null;
  }
}
