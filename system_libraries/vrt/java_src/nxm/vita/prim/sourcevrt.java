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
import nxm.sys.lib.DataFile;
import nxm.sys.lib.MidasException;
import nxm.sys.lib.Primitive;
import nxm.sys.lib.Table;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.net.NetUtilities;
import nxm.vrt.net.NetUtilities.TransportProtocol;
import nxm.vrt.net.VRTEvent;
import nxm.vrt.net.VRTEventListener;
import nxm.vrt.net.VRTReader;

import static nxm.vita.inc.VRTIO.writeVRTPacket;

/** The <tt>SOURCEVRT</tt> primitive. */
public class sourcevrt extends Primitive implements VRTEventListener {
  private          VRTReader packetReader;
  private          DataFile  outFile;
  private volatile boolean   running;

  @Override
  public int open () {
    // OPEN THE FILE
    outFile = MA.getDataFile("OUT");
    outFile.open(DataFile.OUTPUT);
    outFile.setType(1049);

    // OPEN THE SOCKET
    String  hostPort  = MA.getS("HOSTPORT");
    String  device    = MA.getS("/DEVICE", null);
    String  host      = NetUtilities.getHost(hostPort);
    Integer port      = NetUtilities.getPort(hostPort);
    Table   options   = new Table();

    try {
      packetReader = new VRTReader(TransportProtocol.UDP, host, port, device, this, options);
      packetReader.start();
    }
    catch (IOException e) {
      throw new MidasException("Unable to open connection to "+hostPort, e);
    }
    return NORMAL;
  }

  @Override
  public int process () {
    running = true;
    // processing handled in other threads
    return SLEEP;
  }

  @Override
  public int close () {
    running = false;
    if (packetReader != null) packetReader.close();
    if (outFile      != null) outFile.close();

    return NORMAL;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Implement VRTEventListener
  //////////////////////////////////////////////////////////////////////////////////////////////////
  @Override
  public void receivedPacket (VRTEvent e, VRTPacket p) {
    if (running) {
      writeVRTPacket(outFile, p);
    }
  }

  @Override
  public void sentPacket (VRTEvent e, VRTPacket p) {
    // not applicable
  }

  @Override
  public void errorOccurred (VRTEvent e, String msg, Throwable t) {
    if (t != null) M.printStackTrace("SOURCEVRT: "+msg, t);
    else           M.warning("SOURCEVRT: "+msg);
  }

  @Override
  public void warningOccurred (VRTEvent e, String msg, Throwable t) {
    if (t != null) M.printStackTrace("SOURCEVRT: "+msg, t);
    else           M.warning("SOURCEVRT: "+msg);
  }
}
