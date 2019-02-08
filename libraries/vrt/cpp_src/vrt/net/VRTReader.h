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

#ifndef _VRTReader_h
#define _VRTReader_h

#include "BasicVRLFrame.h"
#include "BasicVRTPacket.h"
#include "BasicContextPacket.h"
#include "BasicDataPacket.h"
#include "NetIO.h"
#include "Threadable.h"
#include "VRTContextListener.h"
#include "VRTMath.h"
#include "ThreadSafeQueue.h"
#include <map>
#include <set>
#include <queue>

namespace vrt {
  using namespace std;

  /** Reads VRT packets which may optionally be included in VRL frames. The choice
   *  between VRT packet and VRL frame is made automatically by looking at the
   *  first four bytes received and seeing if they match the <tt>VRLFrame::VRL_FAW</tt>,
   *  if not a VRT packet is assumed.<br>
   *  <br>
   *  <b>Prior to Oct 2013 this class used <tt>multicast.h</tt>, which limited
   *  its applicability to only UDP/Multicast input. This was changed in Oct 2013
   *  to support UDP/Unicast and TCP, accordingly the inclusion of <tt>multicast.h</tt>
   *  was dropped.</b>
   */
  class VRTReader : public NetIO {

    public: __attribute__((deprecated)) __intelattr__((deprecated))
            static const int32_t DEFAULT_MAX_QUEUE_SIZE = 2500;

    /** The "timeout" value indicating the default wait of 60.0 sec.
     *  @deprecated Since Oct 2013 (not required by new constructor).
     */
    public: __attribute__((deprecated)) __intelattr__((deprecated))
            static const int32_t DEFAULT_TIMEOUT;

    /** The "timeout" value indicating an unlimited wait.
     *  @deprecated Since Oct 2013 (not required by new constructor).
     */
    public: __attribute__((deprecated)) __intelattr__((deprecated))
            static const int32_t UNLIMITED_TIMEOUT;

    /** The "timeout" value indicating that "legacy mode" should be used.
     *  @deprecated Since Oct 2013 (not required by new constructor).
     */
    protected: __attribute__((deprecated)) __intelattr__((deprecated))
               static const int32_t LEGACY_MODE;

    /** The start time indicator that the initial context has already been found. */
    protected: static const int32_t FOUND_INITIAL;

    private: int64_t                      timeoutMS;       // timeout in ms (-1=LegacyMode)
    private: int64_t                      startTimeMS;     // start time in ms (0=not started)
    private: BasicDataPacket              initialData;     // data packet marking paired stream
    private: BasicVRTPacket               initialCtx;      // context packet marking paired stream
    private: map<int32_t,BasicVRTPacket>  initialContext;  // all initial context received
    private: set<int32_t>                 requiredContext; // all initial context required
    private: int32_t                      idContext;       // ID for the paired context
    private: BasicVRLFrame                tempFrame;       // Temporary VRL frame
    private: bool                         deletePointers;  // Should VRTReader delete any pointers?

    /** Creates a new instance.
     *  @param port            The port number.
     *  @param device          The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
     *  @param recvBufSize     The SO_RCVBUF size in bytes.
     *  @param packetQueueSize The packet queue size in packets.
     *  @param readOnly        Are packets read-only?
     *  @throws VRTException If an I/O exception occurs.
     */
    public: __attribute__((deprecated)) __intelattr__((deprecated))
            VRTReader (int32_t port, const string &device,
                       int32_t recvBufSize=2097152,
                       int32_t packetQueueSize=2500,
                       bool readOnly=false);

    /** Creates a new instance.
     *  @param host            The host to connect to (null = wildcard address).
     *  @param port            The port number.
     *  @param device          The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
     *  @param recvBufSize     The SO_RCVBUF size in bytes.
     *  @param packetQueueSize The packet queue size in packets.
     *  @param readOnly        Are packets read-only?
     *  @throws VRTException If an I/O exception occurs.
     */
    public: __attribute__((deprecated)) __intelattr__((deprecated))
            VRTReader (const string &host, int32_t port, const string &device,
                       int32_t recvBufSize=2097152,
                       int32_t packetQueueSize=2500,
                       bool readOnly=false);

    /** Creates a new instance. <br>
     *  <br>
     *  <i>For historical reasons (i.e. to prevent incompatibilities with the Java version in VRT
     *  version 1.0 and earlier), the default timeout value (i.e. {@link #DEFAULT_TIMEOUT}) must
     *  be explicitly provided.</i>
     *  @param port            The port number.
     *  @param device          The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
     *  @param listener        The listener used to handle any errors/warnings and to receive any packets.
     *  @param timeout         The approximate timeout (in seconds) applied when checking for the initial
     *                         packets. If the complete initial context is not received before the first
     *                         packet received after this time has elapsed, it will be reported as an error
     *                         (via <tt>errorMsg</tt> in {@link VRTContextListener#receivedInitialContext}
     *                         if appropriate). Setting this to {@link #UNLIMITED_TIMEOUT} disables the
     *                         timeout. The default timeout value to use is {@link #DEFAULT_TIMEOUT}.
     *  @param recvBufSize     The SO_RCVBUF size in bytes.
     *  @param packetQueueSize The packet queue size in packets.
     *  @param readOnly        Are packets read-only?
     *  @param deletePointers  Delete pointers when done?
     *  @throws VRTException If an I/O exception occurs.
     */
    public: __attribute__((deprecated)) __intelattr__((deprecated))
            VRTReader (int32_t port, const string &device,
                       VRTContextListener *listener, double timeout,
                       int32_t recvBufSize=2097152,
                       int32_t packetQueueSize=2500,
                       bool readOnly=false, bool deletePointers=true);

    /** Creates a new instance. <br>
     *  <br>
     *  <i>For historical reasons (i.e. to prevent incompatibilities with the Java version in VRT
     *  version 1.0 and earlier), the default timeout value (i.e. {@link #DEFAULT_TIMEOUT}) must
     *  be explicitly provided.</i>
     *  @param host            The host to connect to (null = wildcard address).
     *  @param port            The port number.
     *  @param device          The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
     *  @param listener        The listener used to handle any errors/warnings and to receive any packets.
     *  @param timeout         The approximate timeout (in seconds) applied when checking for the initial
     *                         packets. If the complete initial context is not received before the first
     *                         packet received after this time has elapsed, it will be reported as an error
     *                         (via <tt>errorMsg</tt> in {@link VRTContextListener#receivedInitialContext}
     *                         if appropriate). Setting this to {@link #UNLIMITED_TIMEOUT} disables the
     *                         timeout. The default timeout value to use is {@link #DEFAULT_TIMEOUT}.
     *  @param recvBufSize     The SO_RCVBUF size in bytes.
     *  @param packetQueueSize The packet queue size in packets.
     *  @param readOnly        Are packets read-only?
     *  @param deletePointers  Delete pointers when done?
     *  @throws VRTException If an I/O exception occurs.
     */
    public: __attribute__((deprecated)) __intelattr__((deprecated))
            VRTReader (const string &host, int32_t port, const string &device,
                       VRTContextListener *listener, double timeout,
                       int32_t recvBufSize=2097152,
                       int32_t packetQueueSize=2500,
                       bool readOnly=false, bool deletePointers=true);

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
     *  @param transport     The transport to use.
     *  @param host          The host to connect to (null = wildcard address).
     *  @param port          The port number (null = any port).
     *  @param device        The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
     *  @param listener      The listener used to handle any errors/warnings and to
     *                       receive any packets.
     *  @param options       Map of options to set (can be null).
     *  @param noDelete      Should {@link VRTReader} skip the normal <tt>delete</tt>
     *                       calls following the sending of a packet to the listener
     *                       (true) or should {@link VRTReader} make sure the packets
     *                       are deleted (false)? Note that if <tt>noDelete=true</tt>
     *                       the listener assumes all responsibility for deleting any
     *                       packets sent to its <tt>receivedPacket(..)</tt>,
     *                       <tt>receivedDataPacket(..)</tt>, or
     *                       <tt>receivedContextPacket(..)</tt> methods (note that
     *                       <tt>receivedInitialContext(..)</tt> is NOT included
     *                       in this list since the packets in that call are passed
     *                       by reference).
     *                       all packet objects as soon as the call to (true) or will
     *  @throws VRTException If an I/O exception occurs.
     */
    public: VRTReader (TransportProtocol transport, const string &host, int32_t port,
                       const string &device, VRTEventListener *listener,
                       map<string,string> options, bool noDelete=false);

    /** Destroys the current instance, closing the socket if required. */
    public: ~VRTReader ();


    /** Requests a reconnect and waits for it to be done. */
    public: inline virtual void reconnect (const VRTEventListener &warnings) {
      NetIO::reconnect(warnings);
    }

    protected: virtual void handle (vector<char> *buffer, int32_t length);

    /** Handles a VRLFrame. */
    private: void handle (BasicVRLFrame &frame);

    /** Handles a VRTPacket (deleting it as required). */
    private: void handle (BasicVRTPacket *packet);

    /** Handles a VRTPacket (but does not delete it). Sets pktSent=true if the
     *  packet was sent out so we know to delete if pktSent==false even if
     *  deletePackets==false.
     */
    private: void _handle (BasicVRTPacket *packet, bool &pktSent);

    /** Have we found all of the initial context? If so send it out. */
    private: bool checkInitialContextDone (bool timeout);

    protected: virtual vector<char>* receiveBufferUDP (bool autoPush);
    protected: virtual vector<char>* receiveBufferTCP (bool autoPush);

////
////
////
////
////
////
////
////
////
////
////    public: size_t getQueueSize() {
////      return packetQueue.getSize();
////    }
////
////    public: size_t getMaxQueueSize() {
////      return packetQueue.getMaxSize();
////    }
////
////    /** <b>Deprecated:</b> Use {@link #receivePackets}. */
////    public: inline __attribute__((deprecated)) __intelattr__((deprecated)) BasicVRTPacket* receive (float timeout) {
////      vector<BasicVRTPacket*> packets = doReceivePackets(timeout);
////      if (packets.size() == 0) {
////        return packets[0];
////      }
////      else {
////        cout << "ERROR: Deprecated VRTReader::receive(..) method received "<<packets.size()<<" packets"<<endl;
////        return new BasicVRTPacket(vector<char>(0), true);
////      }
////    }
////
////    /** Receives packets off of the socket.
////     *  @param timeout The socket timeout in seconds.
////     *  @param count   The number of packets requested. Note that this is only a suggestion,
////     *                 if reading a stream with VRL frames the number of packets read will
////     *                 depend on the number includes in the frame as this will never read
////     *                 a partial VRL frame.
////     *  @return The packets received. If a VRT packet is received (not in a VRL frame) this will
////     *          have a length of one (i.e. the packet), if a VRL frame is received this will
////     *          contain all of VRT packets enclosed in it (in rare cases this could be a length
////     *          of zero). In the event of a timeout a zero-length vector will be returned.
////     */
////    public: vector<BasicVRTPacket*> receivePackets (float timeout, size_t count=1);
  };
} END_NAMESPACE
#endif /* _VRTReader_h */
