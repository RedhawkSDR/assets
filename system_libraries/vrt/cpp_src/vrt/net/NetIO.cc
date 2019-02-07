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

#include <vector>

#include "NetIO.h"
#include "BasicVRLFrame.h"

using namespace std;
using namespace vrt;

NetIO::QueuedPacketHandler::QueuedPacketHandler (NetIO *netIO) :
  netIO(netIO),
  stopIfClear(false)
{
  //done
}

void NetIO::QueuedPacketHandler::runThread () {
  while (!stopNow()) {
    try {
      vector<char> *p = netIO->popPacket();

      if (p != NULL) {
        netIO->handle(p, p->size());
      }
      else if (stopIfClear) {
        stop(false);
      }
      else {
        sleep(10);
      }
    }
    catch (VRTException e) {
      if (netIO->evtListener != NULL) {
        netIO->evtListener->errorOccurred(VRTEvent(*netIO), "Error while processing packet", e);
      }
    }
  }
}

void NetIO::QueuedPacketHandler::stopWhenClear () {
  stopIfClear = true;
}

static vector<char> BUFFER_PUSHED_VAL(0);
vector<char> *NetIO::BUFFER_PUSHED = &BUFFER_PUSHED_VAL;

NetIO::NetIO (bool isOutput, TransportProtocol transport,
              const string &host, int32_t port, const string &device,
              VRTEventListener *listener, const map<string,string> &options,
              int32_t bufLength) :
  isOutput(isOutput),
  evtListener(listener),
  ctxListener(NULL),
  event(NULL),
  transport(transport),
  hostAddr((isNull(host))? "0.0.0.0" : host),
  host(host),
  port(port),
  device(device),
  buffer(bufLength),
  datagramSocket(NULL),
  tcpServerSocket(NULL),
  tcpSockets((transport == TransportProtocol_UDP)? NULL : new vector<Socket*>()),
  tcpServerCheck(0),
  socketOptions(options),
  reconnectNow(false),
  reconnectDone(false),
  reconnectError(),
  frameCounter(0),
  packetCounters(),
  packetsReadIn(),
  packetsReadInLock(),
  lengthReadIn(0),
  queuedPacketHandler((isOutput)? NULL : new QueuedPacketHandler(this)),
  queueLimitPackets(1024),
  queueLimitOctets(8388608),
  doShutdown(0)
{
  try {
    ctxListener = checked_dynamic_cast<VRTContextListener*>(listener);
  }
  catch (VRTException e) {
    UNUSED_VARIABLE(e);
    ctxListener = NULL;
  }

  if (this->transport == TransportProtocol_TCP) {
    this->transport = (isOutput)? TransportProtocol_TCP_SERVER
                                : TransportProtocol_TCP_CLIENT;
  }

  event             = VRTEvent(this);
  queueLimitPackets = NetUtilities::getSocketOptionInt(socketOptions, "QUEUE_LIMIT_PACKETS", 1024);
  queueLimitOctets  = NetUtilities::getSocketOptionInt(socketOptions, "QUEUE_LIMIT_OCTETS",  8388608);

  openSockets();
}

NetIO::~NetIO () {
  stop(true);
  // Do not delete evtListener since it is owned by someone else
  // Do not delete ctxListener since it is owned by someone else
  safe_delete(datagramSocket);
  safe_delete(tcpServerSocket);
  safe_delete(tcpSockets);
  safe_delete(queuedPacketHandler);
}

string NetIO::toString () const {
  return Utilities::format("%s on %s:%d", getClassName().c_str(), host.c_str(), port);
}

void NetIO::start () {
  Threadable::start();
  if (queuedPacketHandler != NULL) {
    queuedPacketHandler->start();
  }
}

void NetIO::stop (bool wait) {
  shutdownSockets();

  if (!isOutput) {
    // Wait up to 10 seconds for low-level input buffers to clear out.
    for (int32_t i = 0; (i < 100) && !isDone(); i++) {
      sleep(100);
    }
  }

  Threadable::stop(wait);
}

void NetIO::shutdown () {
  closeSockets();
  if (queuedPacketHandler != NULL) {
    queuedPacketHandler->stopWhenClear();

    // Wait up to 10 seconds for queue to clear out
    for (int32_t i = 0; (i < 100) && !queuedPacketHandler->isDone(); i++) {
      sleep(100);
    }

    // Discard any remaining entries on the queue
    clearQueue();
    queuedPacketHandler->stop(true);
  }
}

void NetIO::openSockets () {
  SYNCHRONIZED(this);
  switch (transport) {
    case TransportProtocol_TCP_SERVER:
      tcpServerSocket = Socket::createTCPServerSocket(isOutput, host, port, device,
                                                      socketOptions, buffer.size());
      tcpServerCheck  = 0; // check immediately
      break;
    case TransportProtocol_TCP_CLIENT:
      tcpSockets->clear();
      tcpSockets->push_back(Socket::createTCPSocket(isOutput, host, port, device,
                                                    socketOptions, buffer.size()));
      break;
    case TransportProtocol_UDP:
      datagramSocket = Socket::createUDPSocket(isOutput, host, port, device,
                                               socketOptions, buffer.size());
      break;
    default:
      throw VRTException("Unknown transport type %d", (int)transport);
  }
}

bool NetIO::checkSockets () {
  // ==== RECONNECT, IF REQUESTED ============================================
  if (reconnectNow && !reconnectDone) {
    START_SYNCHRONIZED(0,this);
      try {
        closeSockets();
        openSockets();
        reconnectDone = true;
      }
      catch (VRTException t) {
        listenerSupport_fireWarningOccurredErr("Error during socket reconnect", t);
        reconnectError = t;
        reconnectDone  = true;
        return false; // this is NOT normal, and we can't continue
      }
    END_SYNCHRONIZED(0);
  }

  // ==== CONNECT TO TCP CLIENT(S) IF REQUIRED ===============================
  if ((tcpServerSocket != NULL) && (Utilities::currentTimeMillis() >= tcpServerCheck)
                                && (doShutdown == 0)) {
    try {
      Socket *s = tcpServerSocket->accept(socketOptions);
      if (s != NULL) {
        START_SYNCHRONIZED(1,this);
          tcpSockets->push_back(s);
        END_SYNCHRONIZED(1);
        tcpServerCheck = 0; // check again next time
      }
      else {
        tcpServerCheck = Utilities::currentTimeMillis() + 1000; // wait before next check
      }
    }
    catch (VRTException e) {
      listenerSupport_fireWarningOccurredErr("Error connecting to TCP client", e);
      // this is NOT normal, but we can continue on using existing sockets
    }
  }
  return true;
}

void NetIO::shutdownSockets () {
  if (doShutdown == 0) doShutdown = 1;
}

void NetIO::closeSockets () {
  if (datagramSocket != NULL) {
    try {
      datagramSocket->close();
    }
    catch (VRTException e) {
      listenerSupport_fireErrorOccurredErr("Error while trying to close UDP socket", e);
    }
  }

  if (tcpSockets != NULL) {
    closeClientSockets();
  }

  if (tcpServerSocket != NULL) {
    try {
      tcpServerSocket->close();
    }
    catch (VRTException e) {
      listenerSupport_fireErrorOccurredErr("Error while trying to close TCP server socket", e);
    }
  }
}

void NetIO::shutdownClientSockets () {
  SYNCHRONIZED(this);
  for (size_t i = 0; i < tcpSockets->size(); i++) {
    Socket *s = tcpSockets->at(i);
    try {
      s->shutdownSocket();
    }
    catch (VRTException e) {
      listenerSupport_fireErrorOccurredErr("Error while trying to shutdown TCP client socket", e);
    }
  }
}

void NetIO::closeClientSockets () {
  SYNCHRONIZED(this);
  for (size_t i = 0; i < tcpSockets->size(); i++) {
    Socket *s = tcpSockets->at(i);
    try {
      s->close();
      safe_delete(s);
    }
    catch (VRTException e) {
      listenerSupport_fireErrorOccurredErr("Error while trying to close TCP client socket", e);
    }
  }
  tcpSockets->clear();
}

void NetIO::reconnect (const VRTEventListener &warnings) {
  UNUSED_VARIABLE(warnings);
  // Only do the parts we absolutely need within a synchronized block since
  // the actual work is going to be done on another thread.
  START_SYNCHRONIZED(0,this);
    if (reconnectNow) {
      throw VRTException("Already trying to reconnect");
    }
    reconnectError    = VRTException();
    reconnectNow      = true;
    reconnectDone     = false;
  END_SYNCHRONIZED(0);

  while (!stopNow()) {
    START_SYNCHRONIZED(1,this);
      if (reconnectDone) {
        VRTException err = reconnectError;

        reconnectError    = VRTException();
        reconnectNow      = false;
        reconnectDone     = false;

        if (!isNull(err)) throw err;  // done (success)
        else              return;     // done (error)
      }
    END_SYNCHRONIZED(1);
    sleep(100);
  }
}

//////////////////////////////////////////////////////////////////////////////////////////////////
// Functions to support output
//////////////////////////////////////////////////////////////////////////////////////////////////
void NetIO::runOutputThread () {
  int32_t waitForPort = 500;
  if (tcpServerSocket != NULL) {
    while (!stopNow()) {
      if (!checkSockets()) {
        sleep(waitForPort);
        waitForPort = min(10000, waitForPort*2);
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

void NetIO::sendBuffer (const void *buf, int32_t len) {
  if (doShutdown > 0) { // handle shutdown in proper thread
    return;
  }

  if (datagramSocket != NULL) {
    datagramSocket->write(buf, len);
  }
  else {
    SYNCHRONIZED(this);
    for (int32_t i = tcpSockets->size()-1; i >= 0; i--) {
      Socket *s = tcpSockets->at(i);
      int32_t numWritten = s->write(buf, len);
      if (numWritten < 0) {
        s->close();
        tcpSockets->erase(tcpSockets->begin() + i);
      }
    }
  }
}


//////////////////////////////////////////////////////////////////////////////////////////////////
// Functions to support input
//////////////////////////////////////////////////////////////////////////////////////////////////
void NetIO::runInputThread () {
  int32_t waitForPort = 500;

  while (!stopNow()) {
    if (!checkSockets()) {
      sleep(waitForPort);
      waitForPort = min(10000, waitForPort*2);
      continue;
    }

    try {
      // RECEIVE THE PACKET AND ADD IT TO THE QUEUE
      vector<char> *pkt = receiveBuffer(true);

      if (pkt == BUFFER_PUSHED) {
        // already pushed
      }
      else if (pkt != NULL) {
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
    catch (VRTException e) {
      listenerSupport_fireErrorOccurredErr("Error while reading socket", e);
    }
  }
}

void NetIO::pushPackets (vector<char>* first, vector<vector<char>*> *rest) {
  SYNCHRONIZED(packetsReadInLock);
  if (((int32_t)packetsReadIn.size() > queueLimitPackets) && (lengthReadIn > queueLimitOctets)) {
    listenerSupport_fireWarningOccurredErr("Incoming packet queue full",
                    VRTException("Incoming packet queue full dropping %d "
                                 " packets totaling %d octets",
                                 packetsReadIn.size(), lengthReadIn));
    while (!packetsReadIn.empty()) {
      packetsReadIn.pop();
    }
    lengthReadIn = 0;
  }

  if (first != NULL) {
    packetsReadIn.push(first);
    lengthReadIn += first->size();
  }
  if (rest != NULL) {
    for (size_t i = 0; i < rest->size(); i++) {
      packetsReadIn.push(rest->at(i));
      lengthReadIn += rest->at(i)->size();
    }
  }
}

vector<char>* NetIO::popPacket () {
  SYNCHRONIZED(packetsReadInLock);
  vector<char> *p = NULL;
  if (!packetsReadIn.empty()) {
    p = packetsReadIn.front();
    packetsReadIn.pop();
    lengthReadIn -= p->size();
  }
  return p;
}

void NetIO::clearQueue () {
  SYNCHRONIZED(packetsReadInLock);
  while (!packetsReadIn.empty()) {
    packetsReadIn.pop();
  }
  lengthReadIn = 0;
}

void NetIO::handle (vector<char> *buffer, int32_t length) {
  // does nothing
  UNUSED_VARIABLE(buffer);
  UNUSED_VARIABLE(length);
}

vector<char>* NetIO::receiveBuffer (bool autoPush) {
  if (doShutdown > 0) { // handle shutdown in proper thread
    if (datagramSocket != NULL) {
      if (doShutdown == 1) {
        datagramSocket->close();
        doShutdown = 2;
      }
      return NULL; // nothing left to read
    }
    if (tcpSockets != NULL) {
      if (doShutdown == 1) {
        shutdownClientSockets();
        doShutdown = 2;
      }
      // fall through to below so we can read any remaining buffered data
    }
  }

  if (datagramSocket != NULL) return receiveBufferUDP(autoPush);
  if (tcpSockets->size() > 0) return receiveBufferTCP(autoPush);
  return NULL; // Using TCP but no current connection
}

vector<char>* NetIO::receiveBufferUDP (bool autoPush) {
  UNUSED_VARIABLE(autoPush);
  // UDP is fairly easy since everything must be within a single datagram
  int32_t numRead = datagramSocket->read(&buffer[0], buffer.size());
  if (numRead <= 0) return NULL;
  return new vector<char>(buffer.begin(), buffer.begin()+numRead);
}

vector<char>* NetIO::receiveBufferTCP (bool autoPush) {
  UNUSED_VARIABLE(autoPush);
  throw VRTException(getClassName()+" can not read from TCP");
}

int32_t NetIO::_tcpSocketRead (void *ptr, int32_t off, int32_t len, bool required) {
  SYNCHRONIZED(this);
  if (tcpSockets->empty()) return 0;

  char *buf = (char*)ptr;
  Socket  *tcpSocket = tcpSockets->at(0);
  int32_t  totRead   = 0;

  while (!stopNow() && !reconnectNow) {
    try {
      int32_t numRead = tcpSocket->read(&buf[off], len);
      if (numRead == len) {
        return numRead + totRead; // All is good
      }
      else if (numRead < 0) {
        // Hit EOF (or similar) condition
        if (!required) return numRead;
        listenerSupport_fireErrorOccurred("Insufficient data read from TCP socket on %s "
                                          "reconnecting", tcpSocket->toString().c_str());
        tcpSocket->close();
        tcpSockets->erase(tcpSockets->begin());
        return numRead;
      }
      else if (numRead == 0) {
        if (!required) return 0; // socket timeout
      }
      else {
        // Need to read more
        off     += numRead;
        len     -= numRead;
        totRead += numRead;
        required = true;
      }
    }
    catch (VRTException e) {
      listenerSupport_fireErrorOccurredErr(string("Error while reading from TCP socket on ")
                                           + tcpSocket->toString() + " reconnecting", e);
      tcpSocket->close();
      tcpSockets->erase(tcpSockets->begin());
      return -999;
    }
  }
  return -999;
}
