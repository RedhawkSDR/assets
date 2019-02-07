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

/** A UDP/Multicast reader that is designed to translate packets from one form
 *  to another.
 *  @deprecated Since Oct 2013 (just use {@link MulticastReader} directly, followed
 *              by a call to {@link #start()}).
 */
@Deprecated
@SuppressWarnings("deprecation")
public final class TranslatingReader extends MulticastReader {
  /** Creates a new instance.
   *  @param trans     Packet translator to use.
   *  @param host      The host to connect to (null = wildcard address).
   *  @param port      The port number.
   *  @param device    The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
   *  @param warnings  The listener to handle any warnings. This is only used for handling warnings
   *                   during startup, and is discarded immediately following. If this is null, any
   *                   warnings will print to the console.
   *  @param lbuf      The buffer length to use.
   *  @throws IOException If an I/O exception occurs.
   */
  public TranslatingReader (PacketHandler trans, String host, int port, String device,
                            VRTEventListener warnings, int lbuf) throws IOException {
    super(host, port, device, warnings, null, lbuf, trans);
    start();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // NESTED CLASSES
  //////////////////////////////////////////////////////////////////////////////////////////////////
  /** Handles the translation of a raw packet stream into a set of
   *  {@link nxm.vrt.lib.VRTPacket VRTPackets}.
   *  @deprecated Since Oct 2013 (just use {@link MulticastReader.PacketTranslator} directly).
   */
  @Deprecated
  public interface PacketHandler extends PacketTranslator { }
}
