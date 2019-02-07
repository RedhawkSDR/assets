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

package nxm.vita.prim;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import nxm.sys.lib.DataFile;
import nxm.sys.lib.MidasException;
import nxm.sys.lib.Primitive;
import nxm.sys.lib.Table;
import nxm.vrt.lib.VRLFrame;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.net.NetUtilities;
import nxm.vrt.net.NetUtilities.TransportProtocol;
import nxm.vrt.net.VRTEvent;
import nxm.vrt.net.VRTEventListener;
import nxm.vrt.net.VRTWriter;

import static nxm.vita.inc.VRTIO.readVRLFrame;

/** The <tt>SINKVRT</tt> primitive. */
public class sinkvrt extends Primitive implements VRTEventListener {
  private DataFile  inFile;
  private VRTWriter packetWriter;

  @Override
  public int open () {
    // OPEN THE FILE
    inFile = MA.getDataFile("IN");
    inFile.open();

    // OPEN THE SOCKET
    String  hostPort  = MA.getS("HOSTPORT");
    String  device    = MA.getS("/DEVICE", null);
    boolean useFrames = MA.getState("/VRL", false);
    boolean useCRC    = MA.getState("/CRC", false);
    String  host      = NetUtilities.getHost(hostPort);
    Integer port      = NetUtilities.getPort(hostPort);
    Table   options   = new Table();

    options.put("VRL_FRAME", useFrames);
    options.put("VRL_CRC",   useCRC);

    try {
      packetWriter = new VRTWriter(TransportProtocol.UDP, host, port, device, this, options);
    }
    catch (IOException e) {
      throw new MidasException("Unable to open connection to "+hostPort, e);
    }

    return NORMAL;
  }

  @Override
  public int process () {
    AtomicInteger numRead = new AtomicInteger();
    VRLFrame      frame   = readVRLFrame(inFile, numRead);

    if (numRead.intValue() <  0) return FINISH;
    if (numRead.intValue() == 0) return NOOP;

    for (VRTPacket pkt : frame) {
      try {
        packetWriter.sendPacket(pkt);
      }
      catch (IOException e) {
        throw new MidasException("Unable to send packet", e);
      }
    }
    return NORMAL;
  }

  @Override
  public int close () {
    if (packetWriter != null) packetWriter.close();
    if (inFile       != null) inFile.close();
    return NORMAL;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Implement VRTEventListener
  //////////////////////////////////////////////////////////////////////////////////////////////////
  @Override
  public void receivedPacket (VRTEvent e, VRTPacket p) {
    // not applicable
  }

  @Override
  public void sentPacket (VRTEvent e, VRTPacket p) {
    // not that important
  }

  @Override
  public void errorOccurred (VRTEvent e, String msg, Throwable t) {
    if (t != null) M.printStackTrace("SINKVRT: "+msg, t);
    else           M.warning("SINKVRT: "+msg);
  }

  @Override
  public void warningOccurred (VRTEvent e, String msg, Throwable t) {
    if (t != null) M.printStackTrace("SINKVRT: "+msg, t);
    else           M.warning("SINKVRT: "+msg);
  }
}
