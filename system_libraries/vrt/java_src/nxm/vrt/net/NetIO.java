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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import nxm.vrt.net.NetUtilities.TCPSocket;
import nxm.vrt.net.NetUtilities.TransportProtocol;

import static nxm.vrt.net.NetUtilities.acceptTCPSocket;
import static nxm.vrt.net.NetUtilities.createTCPServerSocket;
import static nxm.vrt.net.NetUtilities.createTCPSocket;
import static nxm.vrt.net.NetUtilities.createUDPSocket;
import static nxm.vrt.net.NetUtilities.getHostAddress;
import static nxm.vrt.net.NetUtilities.getSocketOptionInt;

/** <b>Internal Use Only:</b> Provides the basic network I/O functions used by
 *  both {@link VRTReader} and {@link VRTWriter}. <br>
 *  <br>
 *  <b>Warning:</b> This class is public to support documentation and testing.
 *  Users should never access this class directly (even when trying to give a
 *  generic "parent" class) as it is subject to change without notice as is
 *  the fact that any sub-class extends this class.
 */
public abstract class NetIO extends Threadable {
  /** Indicates that a buffer was already handled. */
  protected static final byte[] BUFFER_PUSHED = new byte[0];

  final    boolean                 isOutput;            // Is this an output device?
  final    VRTListenerSupport      listenerSupport;     // Listener support class
  final    TransportProtocol       transport;           // Network transport protocol
  final    InetAddress             hostAddr;            // Network host address (host:port)
  final    String                  host;                // Network host
  final    int                     port;                // Network port
  final    String                  device;              // Name of network device
  final    byte[]                  buffer;              // Data buffer
  final    DatagramPacket          datagram;            // UDP datagram
           DatagramSocket          datagramSocket;      // UDP socket
           ServerSocket            tcpServerSocket;     // TCP server socket
  final    List<TCPSocket>         tcpSockets;          // TCP client socket(s)
           long                    tcpServerCheck;      // Next time to check for TCP server socket
  final    Map<String,?>           socketOptions;       // The socket options
  volatile boolean                 reconnectNow;        // Is a reconnect needed?
  volatile boolean                 reconnectDone;       // Is the reconnect done?
  volatile Throwable               reconnectError;      // Error detected on reconnect
  volatile int                     frameCounter;        // Frame counter
  volatile int                     dataPacketCounter;   // Data packet counter
  final    Map<Integer,Integer>    ctxPacketCounters;   // Context packet counters
  final    ArrayDeque<byte[]>      packetsReadIn;       // Queue of packets read in
           int                     lengthReadIn;        // Total length read in
  final    QueuedPacketHandler     queuedPacketHandler; // Handler for the queued packets
  final    int                     queueLimitPackets;   // Queued packet limit in packets
  final    int                     queueLimitOctets;    // Queued packet limit in octets
  volatile int                     doShutdown;          // Shutdown stage (1=Indicated, 2=InProgress)
           int                     testMode;            // Special test mode (0=Normal, 1=PacketEcho)


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
   *  @param isOutput  Is this connection for output (true) or input (false)?
   *  @param transport The transport being used.
   *  @param host      The host to connect to (null = wildcard address).
   *  @param port      The port number.
   *  @param device    The device to connect to (e.g. "eth0", "eth1.101") (null = use default).
   *  @param listener  The primary listener to use.
   *  @param options   Map of options to set (can be null).
   *  @param bufLength The buffer length to use.
   *  @throws IOException If an I/O execution occurs.
   */
  NetIO (boolean isOutput, TransportProtocol transport, String host, Integer port, String device,
         VRTEventListener listener, Map<String,?> options, int bufLength) throws IOException {
    if ((host   != null) && host.isEmpty()  ) host      = null;
    if ((device != null) && device.isEmpty()) device    = null;
    if (transport == TransportProtocol.TCP  ) transport = (isOutput)? TransportProtocol.TCP_SERVER
                                                                    : TransportProtocol.TCP_CLIENT;
    this.isOutput            = isOutput;
    this.listenerSupport     = new VRTListenerSupport(this);
    this.transport           = transport;
    this.hostAddr            = (host == null)? null : getHostAddress(host);
    this.host                = host;
    this.port                = port;
    this.device              = device;
    this.socketOptions       = options;
    this.ctxPacketCounters   = new TreeMap<>();
    this.frameCounter        = 0;
    this.buffer              = new byte[bufLength];
    this.queueLimitPackets   = getSocketOptionInt(options, "QUEUE_LIMIT_PACKETS", 1024);
    this.queueLimitOctets    = getSocketOptionInt(options, "QUEUE_LIMIT_OCTETS",  8388608);
    this.lengthReadIn        = 0;
    this.packetsReadIn       = (isOutput)? null : new ArrayDeque<>(queueLimitPackets);
    this.queuedPacketHandler = (isOutput)? null : new QueuedPacketHandler();
    this.doShutdown          = 0;

    if (transport == TransportProtocol.UDP) {
      this.datagram        = new DatagramPacket(buffer, 0, buffer.length);
      this.tcpSockets      = null;
    }
    else {
      this.datagram        = null;
      this.tcpSockets      = Collections.synchronizedList(new ArrayList<TCPSocket>(8));
    }
    openSockets();

    if (listener != null) {
      addErrorWarningListener(listener);
      if (isOutput) addSentPacketListener(listener);
      else          addReceivedPacketListener(listener);
    }
  }

  /** Provides a description of the instance useful for debugging.
   *  @return A debugging string.
   */
  @Override
  public String toString () {
    if (tcpServerSocket != null) {     // TCP_SERVER
      return getClass().getSimpleName()+" on "+NetUtilities.toString(tcpServerSocket)
           + " connected to "+tcpSockets;
    }
    else if (tcpSockets != null) {     // TCP_CLIENT
      TCPSocket tcpSocket;
      synchronized (tcpSockets) {
        tcpSocket = (tcpSockets.isEmpty())? null : tcpSockets.get(0);
      }
      return getClass().getSimpleName()+" on "+NetUtilities.toString(tcpSocket);
    }
    else if (datagramSocket != null) { // UDP
      if (isOutput && !hostAddr.isMulticastAddress()) {
        return getClass().getSimpleName()+" on "+NetUtilities.toString(datagramSocket)
             + " writing to "+host+":"+port;
      }
      else {
        return getClass().getSimpleName()+" on "+NetUtilities.toString(datagramSocket);
      }
    }
    else {                             // ERROR
      return getClass().getSimpleName()+" on ???";
    }
  }

  /** <b>Internal Use Only:</b> Gets a reference to the socket being used.
   *  @return The socket being used.
   *  @deprecated Since Oct 2013 (only applicable to UDP).
   */
  @Deprecated
  public DatagramSocket getSocket () {
    return datagramSocket;
  }

  @Override
  public void start () {
    super.start();
    if (queuedPacketHandler != null) {
      queuedPacketHandler.start();
    }
  }

  @Override
  public void stop (boolean wait) {
    shutdownSockets();

    if (!isOutput) {
      // Wait up to 10 seconds for low-level input buffers to clear out.
      for (int i = 0; (i < 100) && !isDone(); i++) {
        sleep(100);
      }
    }
    super.stop(wait);
  }

  @Override
  protected void shutdown () {
    closeSockets();
    if (queuedPacketHandler != null) {
      queuedPacketHandler.stopWhenClear();

      // Wait up to 10 seconds for queue to clear out
      for (int i = 0; (i < 100) && !queuedPacketHandler.isDone(); i++) {
        sleep(100);
      }

      // Discard any remaining entries on the queue
      clearQueue();
      queuedPacketHandler.stop(true);
    }
    listenerSupport.shutdown();
  }

  /** Closes the sockets.
   *  @throws IOException If an i/o error occurs.
   */
  protected final void openSockets () throws IOException {
    switch (transport) {
      case TCP_SERVER:
        tcpServerSocket = createTCPServerSocket(isOutput, host, port, device,
                                                socketOptions, buffer.length);
        tcpServerCheck  = 0; // check immediately
        break;
      case TCP_CLIENT:
        tcpSockets.clear();
        tcpSockets.add(createTCPSocket(isOutput, host, port, device,
                                       socketOptions, buffer.length));
        break;
      case UDP:
        datagramSocket = createUDPSocket(isOutput, host, port, device,
                                         socketOptions, buffer.length);
        break;
      default:
        throw new IllegalArgumentException("Unknown transport type "+transport);
    }
  }

  /** Checks the current sockets, reconnecting as needed. Also connects to any
   *  TCP clients.
   *  @return true if processing can continue, false otherwise.
   */
  protected final boolean checkSockets () {
    // ==== RECONNECT, IF REQUESTED ============================================
    if (reconnectNow && !reconnectDone) {
      synchronized (this) {
        try {
          closeSockets();
          openSockets();
          reconnectDone = true;
        }
        catch (SocketTimeoutException e) {
          return false; // this is normal, but we can't continue
        }
        catch (Throwable t) {
          listenerSupport.fireWarningOccurred("Error during socket reconnect", t);
          reconnectError = t;
          reconnectDone  = true;
          return false; // this is NOT normal, and we can't continue
        }
      }
    }

    // ==== CONNECT TO TCP CLIENT(S) IF REQUIRED ===============================
    if ((tcpServerSocket != null) && (System.currentTimeMillis() >= tcpServerCheck)
                                  && (doShutdown == 0)) {
      try {
        TCPSocket s = acceptTCPSocket(tcpServerSocket, socketOptions);
        if (s != null) {
          tcpSockets.add(s);
          tcpServerCheck = 0; // check again next time
        }
        else {
          tcpServerCheck = System.currentTimeMillis() + 1000; // wait before next check
        }
      }
      catch (IOException e) {
        listenerSupport.fireWarningOccurred("Error connecting to TCP client", e);
        // this is NOT normal, but we can continue on using existing sockets
      }
    }
    return true;
  }

  /** Stops any future read/write on the sockets, but does not close them. This
   *  permits i/o to continue until the underlying buffers are empty.
   */
  protected final void shutdownSockets () {
    if (doShutdown == 0) doShutdown = 1;
  }

  /** Closes the sockets. */
  protected final void closeSockets () {
    if (datagramSocket != null) {
      datagramSocket.close();
    }

    if (tcpSockets != null) {
      closeClientSockets();
    }

    if (tcpServerSocket != null) {
      try {
        tcpServerSocket.close();
      }
      catch (Exception e) {
        listenerSupport.fireErrorOccurred("Error while trying to close TCP server socket", e);
      }
    }
  }

  /** Shutdown only the client sockets. */
  protected void shutdownClientSockets () {
    synchronized (tcpSockets) {
      for (TCPSocket s : tcpSockets) {
        try {
          s.shutdownSocket();
        }
        catch (Exception e) {
          listenerSupport.fireErrorOccurred("Error while trying to shutdown TCP client socket for "+s, e);
        }
      }
    }
  }

  /** Closes only the client sockets. */
  protected final void closeClientSockets () {
    synchronized (tcpSockets) {
      for (TCPSocket s : tcpSockets) {
        try {
          s.close();
        }
        catch (Exception e) {
          listenerSupport.fireErrorOccurred("Error while trying to close TCP client socket for "+s, e);
        }
      }
      tcpSockets.clear();
    }
  }

  /** Requests a reconnect and waits for it to be done.
   *  @param warnings The listener to receive any warnings. (Not currently used.)
   *  @throws Throwable Any error that may come up.
   */
  protected void reconnect (VRTEventListener warnings) throws Throwable {
    // Only do the parts we absolutely need within a synchronized block since
    // the actual work is going to be done on another thread.
    synchronized (this) {
      if (reconnectNow) {
        throw new RuntimeException("Already trying to reconnect");
      }
      reconnectError    = null;
      reconnectNow      = true;
      reconnectDone     = false;
    }

    while (!stopNow()) {
      synchronized (this) {
        if (reconnectDone) {
          Throwable err = reconnectError;

          reconnectError    = null;
          reconnectNow      = false;
          reconnectDone     = false;

          if (err != null) throw err;    // done (error)
          else             return;       // done (success)
        }
      }
      sleep(100);
    }
  }

  @Override
  public final void runThread () {
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
  public final int getActiveConnectionCount () {
    return (tcpSockets != null)? tcpSockets.size() : -1;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Functions to support output
  //////////////////////////////////////////////////////////////////////////////////////////////////
  /** Version of {@link #runThread()} for an output connection. */
  protected void runOutputThread () {
    int waitForPort = 500;
    if (tcpServerSocket != null) {
      while (!stopNow()) {
        if (!checkSockets()) {
          sleep(waitForPort);
          waitForPort = Math.min(10000, waitForPort*2);
          continue;
        }
        waitForPort = 500; // reset
      }
    }
    else {
      // This is here for compatibility with the TCP Server version, though it
      // doesn't do anything useful. Note that if we don't have this and the
      // user calls start() it will exit immediately closing the sockets and
      // then leave the user with a useless connection.
      while (!stopNow()) {
        sleep(1000);
      }
    }
  }

  /** Sends the UDP datagram *or* TCP buffer of data.
   *  @param len The number of octets in the buffer to send.
   *  @throws IOException If there is an i/o exception while sending the data.
   */
  protected final void sendBuffer (int len) throws IOException {
    if (doShutdown > 0) { // handle shutdown in proper thread
      return;
    }

    try {
      if (datagramSocket != null) {
        datagram.setAddress(hostAddr);
        datagram.setPort(port);
        datagram.setLength(len);
        datagramSocket.send(datagram);
      }
      else {
        synchronized (tcpSockets) {
          for (int i = tcpSockets.size()-1; i >= 0; i--) {
            TCPSocket s = tcpSockets.get(i);
            int numWritten = s.write(buffer, 0, len);
            if (numWritten < 0) {
              s.close();
              tcpSockets.remove(i);
            }
          }
        }
      }
    }
    catch (IOException e) {
      if (e.getMessage().equals("Invalid argument") && (System.getProperty("java.net.preferIPv4Stack") == null)) {
        throw new IOException(""+e.getMessage()+" (some Linux versions have a bug in their IPv6 "
                + "protocol stack and require -Djava.net.preferIPv4Stack=true to be set to avoid it)", e);
      }
      throw e;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Functions to support input
  //////////////////////////////////////////////////////////////////////////////////////////////////
  /** Version of {@link #runThread()} for an input connection. */
  protected void runInputThread () {
    int     waitForPort = 500;
    boolean canAutoPush = (testMode == 0);

    while (!stopNow()) {
      if (!checkSockets()) {
        sleep(waitForPort);
        waitForPort = Math.min(10000, waitForPort*2);
        continue;
      }

      try {
        // RECEIVE THE PACKET AND ADD IT TO THE QUEUE
        byte[] pkt = receiveBuffer(canAutoPush);

        if (pkt == BUFFER_PUSHED) {
          // already pushed
        }
        else if (pkt != null) {
          pushPacket(pkt);
        }
        else if (doShutdown > 0) {
          break; // in shutdown: no packets = EOF
        }
        else {
          // timeout
        }
        waitForPort = 500; // reset
      }
      catch (SocketTimeoutException e) {
        // ignore
      }
      catch (PortUnreachableException e) {
        listenerSupport.fireErrorOccurred("Error while reading socket", e);
        sleep(waitForPort);
        waitForPort = Math.min(10000, waitForPort*2);
      }
      catch (IOException e) {
        listenerSupport.fireErrorOccurred("Error while reading socket", e);
      }
      catch (RuntimeException e) {
        listenerSupport.fireErrorOccurred("Error while reading socket", e);
        throw e;
      }
    }
  }

  /** Adds a set of packets to the processing queue. This makes sure that all are
   *  pushed together at the same time. The separation between 'first' and 'rest'
   *  allows for some internal optimizations.
   *  @param first The first packet to push (can be null).
   *  @param rest  Any additional packets to push (can be null).
   */
  protected final void pushPackets (byte[] first, List<byte[]> rest) {
    synchronized (packetsReadIn) {
      if ((packetsReadIn.size() > queueLimitPackets) && (lengthReadIn > queueLimitOctets)) {
        listenerSupport.fireWarningOccurred("Incoming packet queue full",
               new QueueFullException(this, "Incoming packet queue full dropping "
                                          + packetsReadIn.size() + " packets totaling "+lengthReadIn
                                          + " octets", packetsReadIn.size()));
        packetsReadIn.clear();
        lengthReadIn = 0;
      }

      if (first != null) {
        packetsReadIn.add(first);
        lengthReadIn += first.length;
      }
      if (rest != null) {
        for (byte[] p : rest) {
          packetsReadIn.add(p);
          lengthReadIn += p.length;
        }
      }
    }
  }

  /** Adds a packet to the processing queue.
   *  @param p The packet to add to the queue.
   */
  protected final void pushPacket (byte[] p) {
    pushPackets(p, null);
  }

  /** Pops a packet off of the queue.
   *  @return The top packet on the queue (null if there are no packets available).
   */
  protected final byte[] popPacket () {
    byte[] p;
    synchronized (packetsReadIn) {
      p = packetsReadIn.poll();
      if (p != null) {
        lengthReadIn -= p.length;
      }
    }
    return p;
  }

  /** Clears the queue. */
  protected final void clearQueue () {
    synchronized (packetsReadIn) {
      packetsReadIn.clear();
      lengthReadIn = 0;
    }
  }

  /** Handles the packet read in. The default implementation does nothing with the assumption that
   *  subclasses will override as appropriate.
   *  @param buffer The data read in.
   *  @param length The number of bytes read in. <i>[This is here for backwards-compatibility,
   *                it is always called with a value equal to <tt>buffer.length</tt>.]</i>
   *  @throws IOException If there is an i/o exception while processing the packet.
   */
  protected void handle (byte[] buffer, int length) throws IOException {
    // does nothing
  }

  /** Reads a single buffer from the input stream. This method can not be used
   *  directly when the reader is running as a thread.
   *  @param autoPush Automatically push received packet onto queue if doing so
   *                  would be more optimal than just returning it. (This is a
   *                  "suggestion" in that the function is free to disregard the
   *                  value of this and return the received buffer.)
   *  @return The buffer received or null if none read in. If <tt>autoPush=true</tt>
   *          and a buffer is added to the queue this must return
   *          {@link #BUFFER_PUSHED}.
   *  @throws IOException If an exception occurs, common exceptions include
   *                      {@link SocketTimeoutException} and
   *                      {@link PortUnreachableException}.
   */
  protected final byte[] receiveBuffer (boolean autoPush) throws IOException {
    if (doShutdown > 0) { // handle shutdown in proper thread
      if (datagramSocket != null) {
        if (doShutdown == 1) {
          datagramSocket.close();
          doShutdown = 2;
        }
        return null; // nothing left to read
      }
      if (tcpSockets != null) {
        if (doShutdown == 1) {
          shutdownClientSockets();
          doShutdown = 2;
        }
        // fall through to below so we can read any remaining buffered data
      }
    }

    if (datagramSocket != null) return receiveBufferUDP(autoPush);
    if (!tcpSockets.isEmpty())  return receiveBufferTCP(autoPush);
    return null; // Using TCP but no current connection
  }

  /** Receive a packet via UDP. The 'auto-push' feature allows a received packet to be directly
   *  pushed into the receive queue (if autoPush=true); this may permit some optimizations that
   *  eliminate a buffer-copy.
   *  @param autoPush Can packets be auto-pushed if doing so is more efficient?
   *  @return The buffer received. This will be null if no data is received *or* if autoPush=true
   *          and the packet was already pushed for processing.
   *  @throws IOException If there is an i/o exception while reading the buffer.
   */
  protected byte[] receiveBufferUDP (boolean autoPush) throws IOException {
    // UDP is fairly easy since everything must be within a single datagram
    datagram.setLength(buffer.length); // reset length since receive changes it
    datagramSocket.receive(datagram);

    return Arrays.copyOf(buffer, datagram.getLength());
  }

  /** Receive a packet via TCP. The 'auto-push' feature allows a received packet to be directly
   *  pushed into the receive queue (if autoPush=true); this may permit some optimizations that
   *  eliminate a buffer-copy.
   *  @param autoPush Can packets be auto-pushed if doing so is more efficient?
   *  @return The buffer received. This will be null if no data is received *or* if autoPush=true
   *          and the packet was already pushed for processing.
   *  @throws IOException If there is an i/o exception while reading the buffer.
   *  @throws UnsupportedOperationException If TCP is unsupported. (TCP support requires knowledge
   *          of the packet/frame protocol used in order to split the incoming data into packets,
   *          absent this, the default implementation simply throws this exception.)
   */
  protected byte[] receiveBufferTCP (boolean autoPush) throws IOException {
    throw new UnsupportedOperationException(getClass().getName()+" can not read from TCP");
  }

  /** Read from TCP socket with special handling for socket errors.
   *  @param buf      The buffer to read data into.
   *  @param off      The offset into the buffer.
   *  @param len      The number of octets to read.
   *  @param required Is this a mandatory read?
   *  @return Output value from the socket read (number of octets read or EOF
   *          flag) *or* -999 if the read failed and/or the read had a required
   *          length and EOF was reached first.
   *  @throws IOException If there is an i/o exception while sending the buffer.
   */
  protected final int _tcpSocketRead (byte[] buf, int off, int len,
                                      boolean required) throws IOException {
    synchronized (tcpSockets) {
      TCPSocket tcpSocket = tcpSockets.get(0);
      int       totRead   = 0;

      while (!stopNow() && !reconnectNow) {
        try {
          int numRead = tcpSocket.read(buf, off, len);
          if (numRead == len) {
            return numRead + totRead; // All is good
          }
          else if (numRead < 0) {
            // Hit EOF (or similar) condition
            if (!required) return numRead;
            listenerSupport.fireErrorOccurred("Insufficient data read from TCP socket on "
                                             +tcpSocket+" reconnecting", null);
            tcpSocket.close();
            tcpSockets.remove(tcpSocket);
            return numRead;
          }
          else {
            // Need to read more
            off     += numRead;
            len     -= numRead;
            totRead += numRead;
            required = true;
          }
        }
        catch (SocketTimeoutException e) {
          if (!required) return 0;
        }
        catch (Throwable e) {
          listenerSupport.fireErrorOccurred("Error while reading from TCP socket on "
                                           +tcpSocket+" reconnecting", e);
          tcpSocket.close();
          tcpSockets.remove(tcpSocket);
          return -999;
        }
      }
      return -999;
    }
  }





  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Event Handling
  //////////////////////////////////////////////////////////////////////////////////////////////////
  /** Adds an error/warning listener. Duplicate additions are ignored.
   *  @param l The listener.
   *  @throws NullPointerException if the listener is null.
   */
  public final void addErrorWarningListener (VRTEventListener l) {
    listenerSupport.addErrorWarningListener(l);
  }
  /** Removes all error/warning listeners. */
  public final void removeAllErrorWarningListeners () {
    listenerSupport.removeAllErrorWarningListeners();
  }
  /** Removes an error/warning listener.
   *  @param l The listener.
   *  @throws NullPointerException if the listener is null.
   */
  public final void removeErrorWarningListener (VRTEventListener l) {
    listenerSupport.removeErrorWarningListener(l);
  }
  /** Gets all error/warning listeners.
   *  @return The listeners.
   */
  public final List<VRTEventListener> getAllErrorWarningListeners () {
    return listenerSupport.getAllErrorWarningListeners();
  }

  /** Adds a sent packet listener. Duplicate additions are ignored.
   *  @param l The listener.
   *  @throws NullPointerException if the listener is null.
   */
  public final void addSentPacketListener (VRTEventListener l) {
    listenerSupport.addSentPacketListener(l);
  }
  /** Removes a sent packet listener.
   *  @param l The listener.
   *  @throws NullPointerException if the listener is null.
   */
  public final void removeSentPacketListener (VRTEventListener l) {
    listenerSupport.removeSentPacketListener(l);
  }
  /** Removes all listeners. */
  public final void removeAllSentPacketListeners () {
    listenerSupport.removeAllSentPacketListeners();
  }
  /** Gets all active listeners.
   *  @return The listeners.
   */
  public final List<VRTEventListener> getAllSentPacketListeners () {
    return listenerSupport.getAllSentPacketListeners();
  }

  /** Adds a received packet listener. Duplicate additions are ignored.
   *  @param l The listener.
   *  @throws NullPointerException if the listener is null.
   */
  public final void addReceivedPacketListener (VRTEventListener l) {
    listenerSupport.addReceivedPacketListener(l);
  }
  /** Removes a received packet listener.
   *  @param l The listener.
   *  @throws NullPointerException if the listener is null.
   */
  public final void removeReceivedPacketListener (VRTEventListener l) {
    listenerSupport.removeReceivedPacketListener(l);
  }
  /** Removes all listeners. */
  public final void removeAllReceivedPacketListeners () {
    listenerSupport.removeAllReceivedPacketListeners();
  }
  /** Gets all active listeners.
   *  @return The listeners.
   */
  public final List<VRTEventListener> getAllReceivedPacketListeners () {
    return listenerSupport.getAllReceivedPacketListeners();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Packet Handler
  //////////////////////////////////////////////////////////////////////////////////////////////////
  /** Handles any packets on the queue. */
  class QueuedPacketHandler extends Threadable {
    private volatile boolean stopIfClear = false;

    /** Indicates that the thread should stop once the queue is clear. */
    void stopWhenClear () {
      stopIfClear = true;
    }

    @Override
    public void runThread () {
      try {
        while (!stopNow()) {
          byte[] p = popPacket();

          if (p != null) {
            try {
              handle(p, p.length);
            }
            catch (Exception e) {
              listenerSupport.fireErrorOccurred("Error while processing packet", e);
            }
          }
          else if (stopIfClear) {
            stop(false);
          }
          else {
            sleep(10);
          }
        }
      }
      catch (Exception e) {
        listenerSupport.fireErrorOccurred("Error while processing packets, aborting", e);
      }
    }
  }
}
