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

#ifndef _VRTWriter_h
#define _VRTWriter_h

#include "BasicVRLFrame.h"
#include "BasicVRTPacket.h"
#include "NetIO.h"
#include "VRTMath.h"
#include "VRTEventListener.h"
#include <map>
#include <stdarg.h>

namespace vrt {
  using namespace std;

  /** Writes VRT packets which may optionally be included in VRL frames. The choice
   *  between un-framed VRT packets and using VRL frames must be made at the time
   *  the class is instantiated. If frames are used, there is also an option to
   *  use CRC protection for the frames. <br>
   *  <br>
   *  This implements <tt>Threadable</tt> to support use of TCP is used in "server
   *  mode" (i.e. opening a TCP server socket to accept connections). When in this
   *  mode the thread is used to support the acceptance of "client" connections.
   *  When a packet is sent any connected "clients" will receive a copy of the
   *  packet (if none are connected the packet is just discarded).<br>
   *  <br>
   *  <b>Prior to Oct 2013 this class used <tt>multicast.h</tt>, which limited
   *  its applicability to only UDP/Multicast input. This was changed in Oct 2013
   *  to support UDP/Unicast and TCP, accordingly the inclusion of <tt>multicast.h</tt>
   *  was dropped.</b>
   */
  class VRTWriter : public NetIO {
    private: BasicVRTPacket *packet;           // Output packet to use (null if n/a)
    private: BasicVRLFrame  *frame;            // Output frame to use (null if n/a)
    private: bool            crc;              // Include CRC with output frame?
    private: int             frameBreak;       // The VRL frame break (in octets)

    /** Creates a new instance.
     *  @param host      The host to connect to (null = wildcard address).
     *  @param port      The port number.
     *  @param device    The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
     *  @param useFrames Should packets be sent embedded in VRL frames?
     *  @param useCRC    Should frames provide CRC protection? (This parameter only applies if frames
     *                   are in use.)
     *  @param ttl       Time to live for the multicast packet (this should usually be set to 1).
     *  @throws VRTException If an I/O exception occurs.
     */
    public: __attribute__((deprecated)) __intelattr__((deprecated))
            VRTWriter (const string &host, int32_t port, const string &device,
                       bool useFrames, bool useCRC, uint8_t ttl=1);

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
    public: VRTWriter (TransportProtocol transport, const string &host, int32_t port,
                       const string &device, VRTEventListener *listener,
                       map<string,string> options);

    /** Destroys the current instance, closing the socket if required. */
    public: ~VRTWriter ();

    /** Updates the counter in the given packet. */
    private: void updateCounter (BasicVRTPacket *p);

    /** Updates the counter in the given packet (passed via pointer). */
    private: void updateCounter (void *p);

    /** <b>Deprecated:</b> Use {@link #sendPacket}. */
    public: __attribute__((deprecated)) __intelattr__((deprecated))
            inline void transmit (const BasicVRTPacket &p) {
      sendPacket(p);
    }

    /** <b>Deprecated:</b> Use {@link #sendPacket}. */
    public: __attribute__((deprecated)) __intelattr__((deprecated))
            inline void send (const BasicVRTPacket &p) {
      sendPacket(p);
    }

    /** Sends a packet in a logical frame or frames (where applicable). The actual
     *  transmitted packet will have the packet count updated as necessary, however
     *  the packet object passed in will not be altered. This method does not block
     *  until the packet has been sent. To see the actual packet sent and
     *  verification of sending, the listener can be set when this class is created.
     *  @param p The packet to send.
     *  @throws VRTException If an I/O exception occurs.
     */
    public: inline void sendPacket (const BasicVRTPacket &p) {
      sendPacket(&p);
    }

    /** Sends a packet in a logical frame or frames (where applicable). The actual
     *  transmitted packet will have the packet count updated as necessary, however
     *  the packet object passed in will not be altered. This method does not block
     *  until the packet has been sent. To see the actual packet sent and
     *  verification of sending, the listener can be set when this class is created.
     *  @param p The packet to send.
     *  @throws VRTException If an I/O exception occurs.
     */
    public: void sendPacket (const BasicVRTPacket *p);

    /** Sends a set of packets in a logical frame or frames (where applicable).
     *  The actual transmitted packet will have the packet count updated as
     *  necessary, however the packet object passed in will not be altered. This
     *  method does not block until the packet has been sent. To see the actual
     *  packet sent and verification of sending, the listener can be set when
     *  this class is created.
     *  @param packets The packets to send.
     *  @throws VRTException If an I/O exception occurs.
     */
    public: void sendPackets (const vector<BasicVRTPacket*> &packets);

    /** Sends a set of packets in a logical frame or frames (where applicable).
     *  The actual transmitted packet will have the packet count updated as
     *  necessary, however the packet object passed in will not be altered. This
     *  method does not block until the packet has been sent. To see the actual
     *  packet sent and verification of sending, the listener can be set when
     *  this class is created.
     *  @param packets The packets to send.
     *  @throws VRTException If an I/O exception occurs.
     */
    public: void sendPackets (const vector<BasicVRTPacket> &packets);

    /** Sends a set of packets via the socket. <br>
     *  <br>
     *  Prior to Dec 2013, <tt>sendPackets</tt> was a vararg macro, which prevented
     *  us from overloading the method signature, hence <tt>sendAllPackets</tt>
     *  was used even though it was inconsistent with other uses. Since the
     *  re-working of the various <tt>sendAllPackets(..)</tt> functions, the use
     *  of the <tt>sendAllPackets</tt> name is no longer required.
     *  @deprecated Since Dec 2013, use <tt>sendPackets(..)</tt>.
     */
    public: inline void sendAllPackets (const vector<BasicVRTPacket> &packets) {
      sendPackets(packets);
    }

    /** Sends a set of packets in a logical frame or frames (where applicable).
     *  The actual transmitted packet will have the packet count updated as
     *  necessary, however the packet object passed in will not be altered. This
     *  method does not block until the packet has been sent. To see the actual
     *  packet sent and verification of sending, the listener can be set when
     *  this class is created. <br>
     *  <br>
     *  <b>Note that none of the supplied arguments may be null (only the default
     *  arguments at the end may be null).</b> <br>
     *  <br>
     *  Prior to Dec 2013, <tt>sendPackets</tt> was a vararg macro, however this
     *  caused issues if the vararg parameters weren't exactly pointers to a
     *  <tt>BasicVRTPacket</tt> (i.e. bad things would happen if one was a
     *  pointer to a <tt>BasicDataPacket</tt>). Accordingly this was changed,
     *  but does limit the number of packets to 16, which should be sufficient
     *  for most uses.
     *  @param p0 Packet to send #1.
     *  @param p1 Packet to send #2.
     *  @param p2 Packet to send #3.
     *  @param p3 Packet to send #4.
     *  @param p4 Packet to send #5.
     *  @param p5 Packet to send #6.
     *  @param p6 Packet to send #7.
     *  @param p7 Packet to send #8.
     *  @param p8 Packet to send #9.
     *  @param p9 Packet to send #10.
     *  @param pA Packet to send #11.
     *  @param pB Packet to send #12.
     *  @param pC Packet to send #13.
     *  @param pD Packet to send #14.
     *  @param pE Packet to send #15.
     *  @param pF Packet to send #16.
     *  @throws VRTException If an I/O exception occurs.
     */
    public: void sendPackets (const BasicVRTPacket *p0,
                              const BasicVRTPacket *p1,
                              const BasicVRTPacket *p2=NULL,
                              const BasicVRTPacket *p3=NULL,
                              const BasicVRTPacket *p4=NULL,
                              const BasicVRTPacket *p5=NULL,
                              const BasicVRTPacket *p6=NULL,
                              const BasicVRTPacket *p7=NULL,
                              const BasicVRTPacket *p8=NULL,
                              const BasicVRTPacket *p9=NULL,
                              const BasicVRTPacket *pA=NULL,
                              const BasicVRTPacket *pB=NULL,
                              const BasicVRTPacket *pC=NULL,
                              const BasicVRTPacket *pD=NULL,
                              const BasicVRTPacket *pE=NULL,
                              const BasicVRTPacket *pF=NULL);

    /** Sends a set of packets in a logical frame or frames (where applicable).
     *  The actual transmitted packet will have the packet count updated as
     *  necessary, however the packet object passed in will not be altered. This
     *  method does not block until the packet has been sent. To see the actual
     *  packet sent and verification of sending, the listener can be set when
     *  this class is created. <br>
     *  <br>
     *  <b>Note that none of the supplied arguments may be null (only the default
     *  arguments at the end may be null).</b>
     *  @param p0 Packet to send #1.
     *  @param p1 Packet to send #2.
     *  @param p2 Packet to send #3.
     *  @param p3 Packet to send #4.
     *  @param p4 Packet to send #5.
     *  @param p5 Packet to send #6.
     *  @param p6 Packet to send #7.
     *  @param p7 Packet to send #8.
     *  @param p8 Packet to send #9.
     *  @param p9 Packet to send #10.
     *  @param pA Packet to send #11.
     *  @param pB Packet to send #12.
     *  @param pC Packet to send #13.
     *  @param pD Packet to send #14.
     *  @param pE Packet to send #15.
     *  @param pF Packet to send #16.
     *  @throws VRTException If an I/O exception occurs.
     */
    public: void sendPackets (const BasicVRTPacket &p0,
                              const BasicVRTPacket &p1,
                              const BasicVRTPacket &p2=BasicVRTPacket::NULL_PACKET,
                              const BasicVRTPacket &p3=BasicVRTPacket::NULL_PACKET,
                              const BasicVRTPacket &p4=BasicVRTPacket::NULL_PACKET,
                              const BasicVRTPacket &p5=BasicVRTPacket::NULL_PACKET,
                              const BasicVRTPacket &p6=BasicVRTPacket::NULL_PACKET,
                              const BasicVRTPacket &p7=BasicVRTPacket::NULL_PACKET,
                              const BasicVRTPacket &p8=BasicVRTPacket::NULL_PACKET,
                              const BasicVRTPacket &p9=BasicVRTPacket::NULL_PACKET,
                              const BasicVRTPacket &pA=BasicVRTPacket::NULL_PACKET,
                              const BasicVRTPacket &pB=BasicVRTPacket::NULL_PACKET,
                              const BasicVRTPacket &pC=BasicVRTPacket::NULL_PACKET,
                              const BasicVRTPacket &pD=BasicVRTPacket::NULL_PACKET,
                              const BasicVRTPacket &pE=BasicVRTPacket::NULL_PACKET,
                              const BasicVRTPacket &pF=BasicVRTPacket::NULL_PACKET);

    /** Sends a packet in a logical frame or frames (where applicable). This is
     *  identical to calling <tt>sendPacket(p)</tt>.
     *  @param p The packet to send.
     *  @throws VRTException If an I/O exception occurs.
     */
    public: inline void sendPackets (const BasicVRTPacket *p) {
      sendPacket(p);
    }

    /** Sends a packet in a logical frame or frames (where applicable). This is
     *  identical to calling <tt>sendPacket(p)</tt>.
     *  @param p The packet to send.
     *  @throws VRTException If an I/O exception occurs.
     */
    public: inline void sendPackets (const BasicVRTPacket &p) {
      sendPacket(p);
    }

    /** Sends the packet (no VRL frame). */
    private: void _sendVRTPacket (const BasicVRTPacket *p);

    /** Sends the packet(s) in a VRL frame. */
    private: void _sendVRLFrame (const vector<BasicVRTPacket*> &packets);

    /** Sends the packet in a VRL frame (fast version for common case of single packet). */
    private: void _sendVRLFrame (const BasicVRTPacket *p);

    /** Sends the UDP datagram *or* TCP buffer of data. */
    private: void sendBuffer (const void *buf, int32_t len, const string &type,
                              const BasicVRTPacket *packet);

    /** Sends the UDP datagram *or* TCP buffer of data. */
    private: void sendBuffer (const void *buf, int32_t len, const string &type,
                              int32_t count, const vector<BasicVRTPacket*> &packets);
  };
} END_NAMESPACE
#endif /* _VRTWriter_h */
