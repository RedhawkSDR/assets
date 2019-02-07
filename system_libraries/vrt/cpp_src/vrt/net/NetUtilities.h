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

#ifndef _NetUtilities_h
#define _NetUtilities_h

#include "InetAddress.h"
#include "Utilities.h"
#include "VRTContextListener.h"
#include "VRTObject.h"
#include <stdlib.h>      // for atoi(..)
#include <net/if.h>      // for ifreq

using namespace std;

namespace vrt {
  /** Describes a network protocol. */
  enum TransportProtocol {
    /** A TCP server-side socket (if sending) and a client-side socket (if
     *  receiving). Use {@link #TCP_SERVER} or {@link #TCP_CLIENT} to override
     *  the automatic socket type selection.
     */
    TransportProtocol_TCP,
    /** A TCP server-side socket (note that in this case "server" means that
     *  this side will wait for connections, not that it is responsible for
     *  sending the data).
     */
    TransportProtocol_TCP_SERVER,
    /** A TCP client-side socket (note that in this case "client" means that
     *  this side will make the connection to the "server," not that it is
     *  responsible for receiving the data).
     */
    TransportProtocol_TCP_CLIENT,
    /** A UDP socket. This may be a Unicast or Multicast (the exact type of
     *  which will be determined based on the IP address given).
     */
    TransportProtocol_UDP
  };

  /** Various utilities methods.
   *
   *  <h3>Socket Creation Options</h3>
   *  The socket creation methods take in a map of options used for initializing a
   *  socket. If the map is null, or if a given entry is null, the default value
   *  will be used. The map may include any of the basic SO_* socket options for
   *  example:
   *  <pre>
   *    string bufSize = ...;
   *    options["SO_SNDBUF"] = bufSize;
   *  </pre>
   *  It may also include the following options:
   *  <pre>
   *    CONNECT_TIMEOUT - The connection timeout for a TCP Socket in ms.
   *    LOCAL_PORT      - The local port to use for a TCP client connection (this
   *                      overrides the default which is to use any available
   *                      ephemeral port).
   *    RCVBUF_EAGER    - Override the normal o/s behavior and force the receive
   *                      buffer to be eagerly initialized when a UDP socket
   *                      (unicast/multicast) is opened. The default o/s behavior
   *                      on some systems (particularly Linux) is to do a lazy
   *                      initialization of the receive buffer for a UDP socket.
   *                      When an lazy initialization is done, the receive buffer
   *                      might not be initialized until (or possibly after) the
   *                      first receive call is made, resulting in packet loss
   *                      during the period between the open and the actual buffer
   *                      initialization. Setting this to true attempts to force
   *                      the o/s to initialize the receive buffer as part of the
   *                      open (or at least the open within this class); however,
   *                      there is no guarantee that the o/s will allocate it at
   *                      that time. Setting this to false will use the default
   *                      o/s behavior, which is generally faster. <i>(Note that
   *                      there is no SNDBUF_EAGER since there is no reason to
   *                      buffer output packets until the first one is sent.)</i>
   *    SERVER_BACKLOG  - The backlog value for a TCP server socket.
   *  </pre>
   *  Any options not applicable for the given socket type will be ignored. Note
   *  that <tt>IP_MULTICAST_IF</tt> is also ignored since this is explicitly set
   *  via the "device" input parameter. Additionally <tt>SO_SNDBUF</tt>,
   *  <tt>SO_RCVBUF</tt>, <tt>SO_REUSEADDR</tt>, and <tt>SO_TIMEOUT</tt> may be
   *  given defaults that differ from the O/S defaults where applicable (however
   *  these defaults can be overridden via the socket options provided).
   */
  namespace NetUtilities {
    /** Maximum length of a UDP packet (including the UDP header) in octets (65,535).
     *  @see #MAX_IPv4_UDP_LEN
     */
    const int32_t MAX_UDP_LEN = 65535;

    /** Maximum length of a UDP packet (including the UDP header) in octets when
     *  transmitted over IPv4 (65,471). Although the max packet length for UDP is
     *  65,635 bytes, IPv4 imposes a lower limit:
     *  <pre>
     *    Maximum IPv4 Length:        65,535 octets
     *    Maximum IPv4 Header:      -     60 octets  (20 octets is minimum)
     *    -----------------------------------------
     *    Maximim UDP in IPv4:        65,475 octets
     *  </pre>
     *  Accordingly this value should be thought of as the maximum for transmission
     *  size and {@link #MAX_UDP_LEN} should be used as the maximum receive size.
     *  @see #MAX_UDP_LEN
     */
    const int32_t MAX_IPv4_UDP_LEN = 65475;

    /**  The length of a UPD packet header in octets (8). */
    const int32_t UDP_HEADER_LENGTH = 8;

    /** <b>Internal Use Only:</b> Fudge factor to use when sizing buffers that
     *  work with UDP datagrams to account for size of datagram header, plus the
     *  IP header, etc.
     */
    const int32_t EXTRA_BUFFER_SIZE = 80;

    /** Gets the device name applicable when given a NIC and VLAN.
     *  @param nic  The NIC to use  ("" if n/a).
     *  @param vlan The VLAN to use (-1 if n/a).
     *  @return The applicable device identifier (e.g. "eth1", "eth0.123").
     *  @throws VRTException if a VLAN is specified without a NIC
     */
    string getNetworkDeviceName (const string &nic, int32_t vlan);

    /** Gets the host name from a host:port.
     *  @param hostPort The host:port in "host:port", "host" or ":port" syntax.
     *  @return The applicable host name or null if not specified.
     */
    InetAddress getHostAddress (const string &hostPort);

    /** Gets the host name from a host:port.
     *  @param hostPort The host:port in "host:port", "host" or ":port" syntax.
     *  @return The applicable host name or null if not specified.
     */
    string getHost (const string &hostPort);

    /** Gets the port number from a host:port.
     *  @param hostPort The host:port in "host:port", "host" or ":port" syntax.
     *  @return The applicable port number or null if not specified.
     */
    int16_t getPort (const string &hostPort);

    /** <b>Internal Use Only:</b> Converts device name to a device object. */
    ifreq toInterface (int sockfd, const string &device, bool isMCast=false);

    /** Gets a set of default socket options that can be added to. This allows a
     *  easy way of getting "default" options that may be slightly more optimal
     *  than the ones provided by the o/s. <br>
     *  <br>
     *  <b>WARNING: The "default" options provided by this function may change
     *  between releases and may vary from platform to platform within the same
     *  release.</b>
     *  @param optimize  Select more aggressive optimizations that assume high-rate
     *                   VITA-49 traffic (true) or defer to o/s defaults where the
     *                   benefit to "general" traffic is unclear (false). Setting
     *                   this to true may enable options that optimize performance
     *                   for streaming-data at the expense of slower connect/open
     *                   times and/or may be inappropriate for non-packetized data.
     *  @param transport Specific transport to use for any transport-specific
     *                   optimizations (can be null). <i>(This value is only
     *                   considered with respect to optimizations that require
     *                   differentiation between transport options.)</i>
     *  @return The set of options to use (never null).
     */
    map<string,string> getDefaultSocketOptions (bool optimize, TransportProtocol transport);

    /** <b>Internal Use Only:</b> Gets the response from an HTTP GET call. */
    void doHttpGet (const string &url, map<string,string> &header, string &data);

    /** <b>Internal Use Only:</b> Is a socket option present? */
    inline bool hasOpt (map<string,string> &opts, const string &key) {
      return (opts.count(key) > 0);
    }

    /** <b>Internal Use Only:</b> Gets a socket option as an int. */
    inline int getSocketOptionInt (map<string,string> &opts, const string &key, int32_t def=INT32_NULL) {
      if (!hasOpt(opts,key)) return def;
      return atoi(opts[key].c_str());
    }

    /** <b>Internal Use Only:</b> Gets a socket option as a boolean (0/1). */
    inline int getSocketOptionBool (map<string,string> &opts, const string &key, bool def=false) {
      if (!hasOpt(opts,key)) return def;
      return Utilities::toBooleanValue(opts[key]);
    }
  } END_NAMESPACE


  /** <b>Internal Use Only:</b> Class to hold a BSD-style socket with accompanying
   *  information. <br>
   *  <br>
   *  Note that this class does NOT have a copy constructor. This is because
   *  the destructor for the class calls {@link #close()} and because permitting
   *  copies may cause consistency problem in the socket interactions.
   */
  class Socket : public VRTObject {
    private: TransportProtocol  transport;  // The socket type
    private: int                sock;       // The POSIX socket (-1 if not open, -2 if null)
    private: int                mtu;        // The socket MTU (-N if n/a)
    private: struct sockaddr_in addr;       // The socket address
    private: int                soTimeout;  // Socket timeout in ms (SO_TIMEOUT in Java)
    private: bool               shutdownRW; // Stops read/write

    /** Creates a new instance. */
    private: Socket (TransportProtocol transport);

    /** Creates a new instance. */
    private: Socket (TransportProtocol transport, const string &host, int32_t port) ;

    /** Destructor. */
    public: ~Socket ();

    public: virtual string toString () const;

    public: virtual inline bool isNullValue () const {
      return (sock == -2);
    }

    /** What type of socket is this? */
    public: inline TransportProtocol getTransportProtocol () const {
      return transport;
    }

    /** Gets the host address. */
    private: InetAddress getInetAddress () const {
      return (isNullValue())? InetAddress() : InetAddress(addr.sin_addr);
    }

    /** Gets the host port number. */
    private: int32_t getPort () const {
      return (isNullValue())? INT32_NULL : ntohs(addr.sin_port);
    }

    /** <b>Internal Use Only:</b> Gets the MTU applicable for the socket. */
    public: inline int32_t getMTU () const {
      return (mtu <= 0)? 1500 : mtu; // 1500 = IP default
    }

    /** Is this a UDP/Multicast socket? */
    private: bool isMulticast () const {
      return !isNullValue() && getInetAddress().isMulticastAddress();
    }

    /** Is the socket open? */
    public: inline bool isOpen () const {
      return (sock > 0);
    }

    /** Stops the socket's i/o but does not close it. This will stop the low-level
     *  socket from receiving any more data, but will still permit reads until the
     *  low-level buffers are empty. Following a call to this, a call to read for
     *  which there is no data will return EOF rather than 0.
     */
    public: void shutdownSocket ();

    /** Closes the socket. This function is safe to call multiple times. */
    public: void close ();

    /** Is the socket ready for blocking i/o? If SO_TIMEOUT is set, this will
     *  wait up to the specified time for it to become available and return
     *  true if available and false if time expires first. For sockets where
     *  SO_TIMEOUT is 0 (wait indefinitely) this will always return true.
     */
    private: bool isSocketReady ();

    /** Reads data from the socket. If this is a UDP socket, this will receive
     *  a complete datagram (up to the given length) and will return the length.
     *  If this is a TCP client socket this will attempt to read in the specified
     *  number of octets.
     *  @param buf The buffer to receive the data. (OUTPUT)
     *  @param len The length of the buffer.
     *  @return The number of octets received. Returns 0 on a timeout and -1
     *          on EOF.
     *  @throws VRTException If this is a TCP_SERVER socket or if an i/o error
     *          occurs.
     */
    public: int32_t read (void *buf, int32_t len);

    /** Reads data from the socket. If this is a UDP socket, this will receive
     *  a complete datagram (up to the given length) and will return the length.
     *  If this is a TCP client socket this will attempt to read in the specified
     *  number of octets.
     *  @param buf The buffer to receive the data. (OUTPUT)
     *  @param len The length of the buffer.
     *  @return The number of octets received. Returns 0 on a timeout and -1
     *          on EOF.
     *  @throws VRTException If this is a TCP_SERVER socket or if an i/o error
     *          occurs.
     */
    public: inline int32_t read (void *buf, size_t len) {
      return read(buf, (int32_t)len);
    }

    /** Writes data to the socket. If this is a UDP socket, this will transmit
     *  a complete datagram. If this is a TCP client socket this will write the
     *  data to the stream.
     *  @param buf The buffer to receive the data. (INPUT)
     *  @param len The length of the buffer.
     *  @return The number of octets sent. Returns 0 on a timeout and -1 on EOF.
     *  @throws VRTException If this is a TCP_SERVER socket or if an i/o error
     *          occurs.
     */
    public: int32_t write (const void *buf, int32_t len);

    /** Writes data to the socket. If this is a UDP socket, this will transmit
     *  a complete datagram. If this is a TCP client socket this will write the
     *  data to the stream.
     *  @param buf The buffer to receive the data. (INPUT)
     *  @param len The length of the buffer.
     *  @return The number of octets sent. Returns 0 on a timeout and -1 on EOF.
     *  @throws VRTException If this is a TCP_SERVER socket or if an i/o error
     *          occurs.
     */
    public: int32_t write (const void *buf, size_t len) {
      return write(buf, (int32_t)len);
    }

    /** Accepts a new TCP client connection.
     *  @param options   Map of options to set (can be null). See top of
     *                   <tt>NetUtilities</tt> for details.
     *  @return The new client socket or null if no connection is available
     *          within the time set by SO_TIMEOUT.
     *  @throws VRTException If this is not a TCP_SERVER socket or if an i/o
     *          error occurs.
     */
    public: Socket* accept (map<string,string> &options) __attribute__((warn_unused_result));

    /** Creates a new UDP datagram socket.
     *  @param isOutput  Is this socket being created for output (true) or
     *                   input (false)?
     *  @param host      The host to connect to (null = wildcard address).
     *  @param port      The port number.
     *  @param device    The device to connect to (e.g. "eth0", "eth1.101")
     *                   (null = use default).
     *  @param options   Map of options to set (can be null). See top of
     *                   <tt>NetUtilities</tt> for details.
     *  @param bufLength The buffer length in bytes.
     *  @return The socket.
     *  @throws VRTException If an I/O error occurs.
     */
    public: static Socket* createUDPSocket (bool isOutput, const string &host, int32_t port,
                                            const string &device, map<string,string> &options,
                                            int32_t bufLength) __attribute__((warn_unused_result));

    /** Creates a new TCP Server socket.
     *  @param isOutput  Is this socket being created for output (true) or
     *                   input (false)?
     *  @param host      The host to connect to (null = wildcard address).
     *  @param port      The port number.
     *  @param device    The device to connect to (e.g. "eth0", "eth1.101")
     *                   (null = use default).
     *  @param options   Map of options to set (can be null). See top of
     *                   <tt>NetUtilities</tt> for details.
     *  @param bufLength The buffer length in bytes.
     *  @return The socket.
     *  @throws VRTException If an I/O error occurs.
     */
    public: static Socket* createTCPServerSocket (bool isOutput, const string &host, int32_t port,
                                                  const string &device, map<string,string> &options,
                                                  int32_t bufLength) __attribute__((warn_unused_result));

    /** Creates a new TCP (Client) socket.
     *  @param isOutput  Is this socket being created for output (true) or
     *                   input (false)?
     *  @param host      The host to connect to (null = wildcard address).
     *  @param port      The port number.
     *  @param device    The device to connect to (e.g. "eth0", "eth1.101")
     *                   (null = use default).
     *  @param options   Map of options to set (can be null). See top of
     *                   <tt>NetUtilities</tt> for details.
     *  @param bufLength The buffer length in bytes.
     *  @return The socket.
     *  @throws VRTException If an I/O error occurs.
     */
    public: static Socket* createTCPSocket (bool isOutput, const string &host, int32_t port,
                                            const string &device, map<string,string> &options,
                                            int32_t bufLength) __attribute__((warn_unused_result));

    /** Initializes the socket. */
    private: Socket* initSocket (map<string,string> &opts, int32_t recvBuf=INT32_NULL,
                                 int32_t sendBuf=INT32_NULL);
  };
} END_NAMESPACE
#endif /* _NetUtilities_h */
