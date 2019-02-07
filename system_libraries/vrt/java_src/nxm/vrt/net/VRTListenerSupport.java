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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nxm.vrt.lib.DataPacket;
import nxm.vrt.lib.Utilities;
import nxm.vrt.lib.VRLFrame;
import nxm.vrt.lib.VRTPacket;

/** <b>Internal Use Only:</b> Provides listener support for {@link VRTEventListener}
 *  classes. <br>
 *  <br>
 *  The early (prototype) versions of this class supported filtering based on
 *  Class ID. This feature was found to be fairly useless since most filtering
 *  needs to be done Stream ID *not* Class ID. Accordingly when similar support
 *  was added to C++ this type of filtering was *not* supported. However, it was
 *  maintained in this class until the end of 2013 (to support existing uses),
 *  but was then deprecated in an effort to streamline this class and improve
 *  overall efficiency.
 */
public final class VRTListenerSupport {
  private final Object                source;
  private final VRTEvent              event;
  private       boolean               initialCtxSent;
  private final Set<VRTEventListener> errorListeners   = new LinkedHashSet<>(8);
  private final Set<VRTEventListener> recvPktListeners = new LinkedHashSet<>(8);
  private final Set<VRTEventListener> sentPktListeners = new LinkedHashSet<>(8);

  /** Creates a new instance.
   *  @param source The source to use for the default {@link VRTEvent}.
   */
  public VRTListenerSupport (Object source) {
    this.source         = source;
    this.event          = new VRTEvent(source);
    this.initialCtxSent = false;
  }

  /** Resets the listener to allow a re-send of the initial context. */
  public void reset () {
    initialCtxSent = false;
  }

  /** Shutdown, removing all registered listeners. */
  public void shutdown () {
    errorListeners.clear();
    recvPktListeners.clear();
    sentPktListeners.clear();
  }

  /** Fires a received packet message.
   *  @param p The packet received.
   */
  public void fireReceivedPacket (VRTPacket p) {
    if (p instanceof DataPacket) {
      fireReceivedDataPacket((DataPacket)p);
    }
    else {
      fireReceivedContextPacket(p);
    }
  }

  /** Fires a received data packet message.
   *  @param p The packet received.
   */
  public void fireReceivedDataPacket (DataPacket p) {
    recvPktListeners.forEach((l) -> l.receivedDataPacket(event, p.copy()));
  }

  /** Fires a received context packet message.
   *  @param p The packet received.
   */
  public void fireReceivedContextPacket (VRTPacket p) {
    recvPktListeners.forEach((l) -> l.receivedContextPacket(event, p.copy()));
  }

  /** Fires a received initial context message. Please see
   *  {@link VRTContextListener#receivedInitialContext} for details on the parameters passed in.
   *  @param errorMsg The error message.
   *  @param data     The data packet.
   *  @param ctx      The paired context packet.
   *  @param context  The full set of context packets.
   */
  public void fireReceivedInitialContext (String errorMsg, DataPacket data,
                                          VRTPacket ctx, Map<Integer,VRTPacket> context) {
    recvPktListeners.forEach((l) -> l.receivedInitialContext(event,
                                                             errorMsg,
                                                             (data == null)? null : data.copy(),
                                                             (ctx  == null)? null : ctx.copy(),
                                                             Utilities.deepCopy(context)));
    initialCtxSent = true;
  }

  /** <b>Internal Use Only:</b> Fires a set of sent packet messages for a frame. */
  void fireSentPackets (VRLFrame f) {
    if (sentPktListeners.isEmpty()) return; // common case
    for (VRTPacket p : f) {
      fireSentPacket(p);
    }
  }

  /** Fires a sent packet message.
   *  @param p The packet sent.
   */
  public void fireSentPacket (VRTPacket p) {
    sentPktListeners.forEach((l) -> l.sentPacket(event, p.copy()));
  }

  /** Fires an error message.
   *  @param msg The error message.
   *  @param t   Any applicable exception caught (can be null).
   */
  public final void fireErrorOccurred (String msg, Throwable t) {
    fireErrorOccurred(event, msg, t);
  }
  /** Fires an error message.
   *  @param msg The error message.
   *  @param t   Any applicable exception caught (can be null).
   *  @param p   The packet that triggered the error (null if not applicable).
   */
  public final void fireErrorOccurred (String msg, Throwable t, VRTPacket p) {
    fireErrorOccurred(new VRTEvent(source, p), msg, t);
  }
  /** Fires an error message.
   *  @param src The event source.
   *  @param msg The error message.
   *  @param t   Any applicable exception caught (can be null).
   */
  public void fireErrorOccurred (VRTEvent src, String msg, Throwable t) {
    errorListeners.forEach((l) -> l.errorOccurred(src, msg, t));
  }

  /** Fires a warning message.
   *  @param msg The error message.
   *  @param t   Any applicable exception caught (can be null).
   */
  public final void fireWarningOccurred (String msg, Throwable t) {
    fireWarningOccurred(event, msg, t);
  }
  /** Fires a warning message.
   *  @param msg The error message.
   *  @param t   Any applicable exception caught (can be null).
   *  @param p   The packet that triggered the error (null if not applicable).
   */
  public final void fireWarningOccurred (String msg, Throwable t, VRTPacket p) {
    fireWarningOccurred(new VRTEvent(source, p), msg, t);
  }
  /** Fires a warning message.
   *  @param src The event source.
   *  @param msg The error message.
   *  @param t   Any applicable exception caught (can be null).
   */
  public void fireWarningOccurred (VRTEvent src, String msg, Throwable t) {
    errorListeners.forEach((l) -> l.warningOccurred(src, msg, t));
  }

  /** Adds a received packet listener.
   *  @param l The listener.
   *  @throws NullPointerException if the listener is null.
   */
  public void addReceivedPacketListener (VRTEventListener l) {
    recvPktListeners.add(l);
  }

  /** Removes a received packet listener.
   *  @param l The listener.
   *  @throws NullPointerException if the listener is null.
   */
  public void removeReceivedPacketListener (VRTEventListener l) {
    recvPktListeners.remove(l);
  }

  /** Removes all received packet listeners. */
  public void removeAllReceivedPacketListeners () {
    recvPktListeners.clear();
  }

  /** Gets all received packet listeners.
   *  @return The listeners (not null).
   */
  public List<VRTEventListener> getAllReceivedPacketListeners () {
    return new ArrayList<VRTEventListener>(recvPktListeners);
  }

  /** Adds a sent packet listener.
   *  @param l The listener.
   *  @throws NullPointerException if the listener is null.
   */
  public void addSentPacketListener (VRTEventListener l) {
    sentPktListeners.add(l);
  }

  /** Removes a sent packet listener.
   *  @param l The listener.
   *  @throws NullPointerException if the listener is null.
   */
  public void removeSentPacketListener (VRTEventListener l) {
    sentPktListeners.remove(l);
  }

  /** Removes all sent packet listeners. */
  public void removeAllSentPacketListeners () {
    sentPktListeners.clear();
  }

  /** Gets all sent packet listeners.
   *  @return The listeners (not null).
   */
  public List<VRTEventListener> getAllSentPacketListeners () {
    return new ArrayList<VRTEventListener>(sentPktListeners);
  }

  /** Adds an error/warning listener.
   *  @param l The listener.
   *  @throws NullPointerException if the listener is null.
   */
  public void addErrorWarningListener (VRTEventListener l) {
    errorListeners.add(l);
  }

  /**  Removes all error/warning listeners. */
  public void removeAllErrorWarningListeners () {
    errorListeners.clear();
  }

  /** Removes an error/warning listener.
   *  @param l The listener.
   *  @throws NullPointerException if the listener is null.
   */
  public void removeErrorWarningListener (VRTEventListener l) {
    errorListeners.remove(l);
  }

  /** Gets all error/warning listeners.
   *  @return The listeners (not null).
   */
  public List<VRTEventListener> getAllErrorWarningListeners () {
    return new ArrayList<VRTEventListener>(errorListeners);
  }
}
