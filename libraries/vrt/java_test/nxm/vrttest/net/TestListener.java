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

import java.util.Map;
import nxm.vrt.lib.DataPacket;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.net.VRTContextListener;
import nxm.vrt.net.VRTEvent;

/** A dummy implementation of {@link VRTContextListener} that stores the
 *  results of the last call.
 */
final class TestListener implements VRTContextListener {
  /** The function last called. */
  public static enum Mode { none, receivedPacket, sentPacket, errorOccurred,
                            warningOccurred, receivedDataPacket,
                            receivedContextPacket, receivedInitialContext };

  Mode                    mode;
  VRTEvent                event;
  String                  message;
  VRTPacket               packet;
  DataPacket              data;
  VRTPacket               ctx;
  Map<Integer, VRTPacket> context;
  Throwable               error;

  /** Creates a new instance with all values (re-)set to defaults.*/
  TestListener () {
    reset();
  }

  @Override
  public String toString () {
    String msg = (message == null)? "<null>" : "'"+message+"'";
    String err = (error   == null)? "<null>" : "'"+error+"'";

    switch (mode) {
      case none:                   return "No Message Received";
      case receivedPacket:         return "receivedPacket("+event+", "+packet+")";
      case sentPacket:             return "sentPacket("+event+", "+packet+")";
      case errorOccurred:          return "errorOccurred("+event+", "+msg+", "+err+")";
      case warningOccurred:        return "warningOccurred("+event+", "+msg+", "+err+")";
      case receivedDataPacket:     return "receivedDataPacket("+event+", "+packet+")";
      case receivedContextPacket:  return "receivedContextPacket("+event+", "+packet+")";
      case receivedInitialContext: return "receivedInitialContext("+event+", "+msg+", "+data+", "+ctx+", "+context+")";
      default: throw new AssertionError("Unknown mode "+mode);
    }
  }

  /** Resets all values to null. */
  void reset () {
    mode    = Mode.none;
    event   = null;
    message = null;
    packet  = null;
    data    = null;
    ctx     = null;
    context = null;
    error   = null;
  }

  @Override
  public void receivedPacket (VRTEvent e, VRTPacket p) {
    reset();
    mode   = Mode.receivedPacket;
    event  = e;
    packet = p;
  }

  @Override
  public void sentPacket (VRTEvent e, VRTPacket p) {
    reset();
    mode   = Mode.sentPacket;
    event  = e;
    packet = p;
  }

  @Override
  public void errorOccurred (VRTEvent e, String msg, Throwable t) {
    reset();
    mode    = Mode.errorOccurred;
    event   = e;
    message = msg;
    error   = t;
  }

  @Override
  public void warningOccurred (VRTEvent e, String msg, Throwable t) {
    reset();
    mode    = Mode.warningOccurred;
    event   = e;
    message = msg;
    error   = t;
  }

  @Override
  public void receivedDataPacket (VRTEvent e, DataPacket p) {
    reset();
    mode   = Mode.receivedDataPacket;
    event  = e;
    packet = p;
  }

  @Override
  public void receivedContextPacket (VRTEvent e, VRTPacket p) {
    reset();
    mode   = Mode.receivedContextPacket;
    event  = e;
    packet = p;
  }

  @Override
  public void receivedInitialContext (VRTEvent e, String msg, DataPacket d,
                                      VRTPacket c, Map<Integer,VRTPacket> map) {
    reset();
    mode    = Mode.receivedInitialContext;
    event   = e;
    message = msg;
    data    = d;
    ctx     = c;
    context = map;
  }
}
