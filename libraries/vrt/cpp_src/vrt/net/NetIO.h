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

#ifndef _NetIO_h
#define _NetIO_h

#include "NetUtilities.h"
#include "Threadable.h"
#include "VRTContextListener.h"
#include "VRTObject.h"
#include <queue>

using namespace std;

/** <b>Internal Use Only:</b> Sends message to listener. */
#define listenerSupport_fireErrorOccurred(...) \
  if (evtListener != NULL) { evtListener->errorOccurred(event, Utilities::format(__VA_ARGS__), VRTException()); }
/** <b>Internal Use Only:</b> Sends message to listener. */
#define listenerSupport_fireErrorOccurredErr(msg,t) \
  if (evtListener != NULL) { evtListener->errorOccurred(event, msg, t); }
/** <b>Internal Use Only:</b> Sends message to listener. */
#define listenerSupport_fireErrorOccurredPkt(msg,t,p) \
  if (evtListener != NULL) { evtListener->errorOccurred(VRTEvent(this,p), msg, t); }
/** <b>Internal Use Only:</b> Sends message to listener. */
#define listenerSupport_fireWarningOccurred(...) \
  if (evtListener != NULL) { evtListener->warningOccurred(event, Utilities::format(__VA_ARGS__), VRTException()); }
/** <b>Internal Use Only:</b> Sends message to listener. */
#define listenerSupport_fireWarningOccurredErr(msg,t) \
  if (evtListener != NULL) { evtListener->warningOccurred(event, msg, t); }
/** <b>Internal Use Only:</b> Sends message to listener. */
#define listenerSupport_fireWarningOccurredPkt(msg,t,p) \
  if (evtListener != NULL) { evtListener->errorOccurred(VRTEvent(this,p), msg, t); }
/** <b>Internal Use Only:</b> Sends message to listener. */
#define listenerSupport_fireSentPacket(pkt) \
  if (evtListener != NULL) { evtListener->sentPacket(event, pkt); }
/** <b>Internal Use Only:</b> Sends message to listener. */
#define listenerSupport_fireSentPackets(frame) \
  if (evtListener != NULL) { \
    vector<BasicVRTPacket*> pkts = frame->getVRTPackets(); \
    for (size_t i = 0; i < pkts.size(); i++) { \
      evtListener->sentPacket(event, pkts[i]); \
    } \
  }
/** <b>Internal Use Only:</b> Sends message to listener. */
#define listenerSupport_fireReceivedPacket(pkt) \
  if (evtListener != NULL) { evtListener->receivedPacket(event, pkt); }
/** <b>Internal Use Only:</b> Sends message to listener. */
#define listenerSupport_fireReceivedDataPacket(pkt) \
       if (ctxListener != NULL) { ctxListener->receivedDataPacket(event, checked_dynamic_cast<BasicDataPacket*>(pkt)); } \
  else if (evtListener != NULL) { evtListener->receivedPacket(event, pkt); }
/** <b>Internal Use Only:</b> Sends message to listener. */
#define listenerSupport_fireReceivedContextPacket(pkt) \
       if (ctxListener != NULL) { ctxListener->receivedContextPacket(event, pkt); } \
  else if (evtListener != NULL) { evtListener->receivedPacket(event, pkt); }

namespace vrt {
  /** <b>Internal Use Only:</b> Provides the basic network I/O functions used by
   *  both {@link VRTReader} and {@link VRTWriter}. <br>
   *  <br>
   *  <b>Warning:</b> This class is public to support documentation and testing.
   *  Users should never access this class directly (even when trying to give a
   *  generic "parent" class) as it is subject to change without notice as is
   *  the fact that any sub-class extends this class.
   */
  class NetIO : public Threadable {
    /** <b>Internal Use Only:</b> Handles any packets on the queue. */
    class QueuedPacketHandler : public Threadable {
      private:          NetIO *netIO;
      private: volatile bool   stopIfClear;

      public: QueuedPacketHandler (NetIO *netIO);

      public: virtual void runThread ();

      /** Indicates that the thread should stop once the queue is clear. */
      public: void stopWhenClear ();
    };
    friend class QueuedPacketHandler;

    /** Indicates that a buffer was already handled. */
    protected: static vector<char> *BUFFER_PUSHED;

    protected: bool                    isOutput;            // Is this an output device?
    protected: VRTEventListener       *evtListener;         // The listener to send to (NULL if n/a)
    protected: VRTContextListener     *ctxListener;         // The listener to send to (NULL if n/a)
    protected: VRTEvent                event;               // The event object to use
    protected: TransportProtocol       transport;           // Network transport protocol
    protected: InetAddress             hostAddr;            // Network host address (host:port)
    protected: string                  host;                // Network host
    protected: int32_t                 port;                // Network port
    protected: string                  device;              // Name of network device
    protected: vector<char>            buffer;              // Data buffer
    protected: Socket                 *datagramSocket;      // UDP socket
    protected: Socket                 *tcpServerSocket;     // TCP server socket
    protected: vector<Socket*>        *tcpSockets;          // TCP client socket(s)
    protected: int64_t                 tcpServerCheck;      // Next time to check for TCP server socket
    protected: map<string,string>      socketOptions;       // The socket options
    protected: bool                    reconnectNow;        // Is a reconnect needed?
    protected: bool                    reconnectDone;       // Is the reconnect done?
    protected: VRTException            reconnectError;      // Error detected on reconnect
    protected: int32_t                 frameCounter;        // Frame counter
    protected: map<int64_t,int32_t>    packetCounters;      // Packet counters
    protected: queue<vector<char>*>    packetsReadIn;       // Queue of packets read in
    protected: VRTObject               packetsReadInLock;   // Lock object for packetsReadIn
    protected: int32_t                 lengthReadIn;        // Total length read in
    protected: QueuedPacketHandler    *queuedPacketHandler; // Handler for the queued packets
    protected: int32_t                 queueLimitPackets;   // Queued packet limit in packets
    protected: int32_t                 queueLimitOctets;    // Queued packet limit in octets
    protected: int32_t                 doShutdown;          // Shutdown stage (1=Indicated, 2=InProgress)

    /** Creates a new instance. The options provided to control the socket and
     *  connection may include any of the "Socket Creation Options" listed
     *  at the top of <tt>NetUtilities</tt> plus the following:
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
     *  @param isOutput  Is this connection for output (true) or input (false)?
     *  @param transport The transport being used.
     *  @param host      The host to connect to (null = wildcard address).
     *  @param port      The port number.
     *  @param device    The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
     *  @param listener  The primary listener to use.
     *  @param options   Map of options to set (can be null).
     *  @param bufLength The buffer length to use.
     *  @throws VRTException If an I/O execution occurs.
     */
    public: NetIO (bool isOutput, TransportProtocol transport,
                   const string &host, int32_t port, const string &device,
                   VRTEventListener *listener, const map<string,string> &options,
                   int32_t bufLength);

    /** Destructor. */
    public: virtual ~NetIO ();

    /** Provides a description of the instance useful for debugging.
     *  @return A debugging string.
     */
    public: virtual string toString () const;


    public:    virtual void start ();
    public:    virtual void stop (bool wait=true);
    protected: virtual void shutdown ();

    /** Closes the sockets. */
    protected: void openSockets ();

    /** Checks the current sockets, reconnecting as needed. Also connects to any
     *  TCP clients.
     *  @return true if processing can continue, false otherwise.
     */
    protected: bool checkSockets ();

    /** Stops any future read/write on the sockets, but does not close them. This
     *  permits i/o to continue until the underlying buffers are empty.
     */
    protected: void shutdownSockets ();

    /** Closes the sockets. */
    protected: void closeSockets ();

    /** Shutdown only the client sockets. */
    protected: void shutdownClientSockets ();

    /** Closes only the client sockets. */
    protected: void closeClientSockets ();

    /** Requests a reconnect and waits for it to be done. */
    protected: virtual void reconnect (const VRTEventListener &warnings);

    protected: virtual inline void runThread () {
      if (isOutput) runOutputThread();
      else          runInputThread();
    }

    /** <b>Internal Use Only:</b> Gets the number of active connections. This will
     *  be:
     *  <pre>
     *    -1  = UDP        (UDP is connectionless)
     *     0  = TCP Server (no active clients)
     *     N  = TCP Server (N  active clients)
     *     1  = TCP Client
     *  </pre>
     *  @return The number of active connections.
     */
    public: inline int32_t getActiveConnectionCount () {
      return (tcpSockets != NULL)? (int32_t)tcpSockets->size() : -1;
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////
    // Functions to support output
    //////////////////////////////////////////////////////////////////////////////////////////////////
    /** Version of {@link #runThread()} for an output connection. */
    protected: virtual void runOutputThread ();

    /** Sends the UDP datagram *or* TCP buffer of data.
     *  @param len The number of octets in the buffer to send.
     */
    protected: inline void sendBuffer (int32_t len) {
      sendBuffer(&buffer[0], len);
    }

    /** Sends the UDP datagram *or* TCP buffer of data.
     *  @param buf The pointer to the data to send.
     *  @param len The number of octets in the buffer to send.
     */
    protected: void sendBuffer (const void *buf, int32_t len);

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // Functions to support input
    //////////////////////////////////////////////////////////////////////////////////////////////////
    /** Version of {@link #runThread()} for an input connection. */
    protected: virtual void runInputThread ();

    /** Adds a set of packets to the processing queue. This makes sure that all
     *  are pushed together at the same time.
     */
    protected: void pushPackets (vector<char>* first, vector<vector<char>*> *rest);

    /** Adds a packet to the processing queue. */
    protected: inline void pushPacket (vector<char> *p) {
      pushPackets(p, NULL);
    }

    /** Pops a packet off of the queue. */
    protected: vector<char>* popPacket ();

    /** Pops a packet off of the queue. */
    protected: void clearQueue ();

    /** Handles the packet read in.
     *  @param buffer The data read in.
     *  @param length The number of bytes read in. <i>[This is here for backwards-compatibility,
     *                it is always called with a value equal to <tt>buffer.length</tt>.]</i>
     */
    protected: virtual void handle (vector<char> *buffer, int32_t length);

    /** Reads a single buffer from the input stream. This method can not be used
     *  directly when the reader is running as a thread.
     *  @param autoPush Automatically push received packet onto queue if doing so
     *                  would be more optimal than just returning it. (This is a
     *                  "suggestion" in that the function is free to disregard the
     *                  value of this and return the received buffer.)
     *  @return The buffer received or null if none read in. If <tt>autoPush=true</tt>
     *          and a buffer is added to the queue this must return
     *          {@link #BUFFER_PUSHED}.
     *  @throws VRTException If an exception occurs.
     */
    protected: vector<char>* receiveBuffer (bool autoPush);

    /** Receive a packet via UDP. */
    protected: virtual vector<char>* receiveBufferUDP (bool autoPush);

    /** Receive a packet via UDP. */
    protected: virtual vector<char>* receiveBufferTCP (bool autoPush);

    /** Read from TCP socket with special handling for socket errors.
     *  @param buf      The buffer to read data into.
     *  @param off      The offset into the buffer.
     *  @param len      The number of octets to read.
     *  @param required Is this a mandatory read?
     *  @return Output value from the socket read (number of octets read or EOF
     *          flag) *or* -999 if the read failed and/or the read had a required
     *          length and EOF was reached first.
     */
    protected: int32_t _tcpSocketRead (void *buf, int32_t off, int32_t len, bool required);
  };
} END_NAMESPACE
#endif /* _NetIO_h */
