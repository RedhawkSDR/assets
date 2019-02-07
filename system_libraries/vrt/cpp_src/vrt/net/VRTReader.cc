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

#include "VRTReader.h"
#include "AbstractPacketFactory.h"
#include <cstring>
#include <string>
#include <map>
#include <vector>
#include <unistd.h>


using namespace std;
using namespace vrt;

const int32_t VRTReader::DEFAULT_TIMEOUT   = 60;
const int32_t VRTReader::UNLIMITED_TIMEOUT = -1;
const int32_t VRTReader::LEGACY_MODE       = -2;
const int32_t VRTReader::FOUND_INITIAL     = -3;

// Leading '"%s", ' required to avoid warnings under clang (assumes use ONLY as
// argument to fireReceivedInitialContext(..)).
#define NO_CONTEXT_STREAM  "%s", VRTContextListener::NO_CONTEXT_STREAM.c_str()
#define NO_DATA_STREAM     "%s", VRTContextListener::NO_DATA_STREAM.c_str()

#define fireReceivedInitialContext(...) \
  if (ctxListener != NULL) { \
    ctxListener->receivedInitialContext(VRTEvent(this), Utilities::format(__VA_ARGS__), \
                                        initialData, initialCtx, initialContext); \
  } \
  startTimeMS     = FOUND_INITIAL; \
  initialData     = BasicVRTPacket::NULL_PACKET; \
  initialCtx      = BasicVRTPacket::NULL_PACKET; \
  idContext       = INT32_NULL; \
  initialContext.clear(); \
  requiredContext.clear();

/** The following definition should be used in place of 'fireReceivedInitialContext("")'
 *  to avoid a "zero-length gnu_printf format string" warning in GCC (seen in GCC 4.4.5).
 */
#define fireReceivedInitialContextDone() fireReceivedInitialContext("%s","")

/** Checks to see if all the required keys are present in a map. */
template <typename T>
static inline bool containsAll (map<int32_t,T> present, set<int32_t> required) {
  set<int32_t>::iterator it;
  for (it=required.begin(); it!=required.end(); it++) {
    if (present.count(*it) == 0) return false;
  }
  return true;
}

static inline string toStringSet (set<int32_t> myset) {
  set<int32_t>::iterator it;
  ostringstream str;

  str << "[ ";
  for (it=myset.begin(); it!=myset.end(); it++) {
    str << *it << " ";
  }
  str << "]";
  return str.str();
}

template <typename T>
static inline string toStringKeys (map<int32_t,T> mymap) {
  typename map<int32_t,T>::iterator it;
  ostringstream str;

  str << "[ ";
  for (it=mymap.begin(); it!=mymap.end(); it++) {
    str << (*it).first << " ";
  }
  str << "]";
  return str.str();
}

/** Converts old individual options into a new map of options. */
static map<string,string> &makeOptions (double timeout, int32_t recvBufSize,
                                       int32_t queueSize, bool readOnly) {
  UNUSED_VARIABLE(readOnly);
  static map<string,string> opts;

  if (!isNull(timeout    )) opts["INITIAL_TIMEOUT"    ] = Utilities::format("%d", (int)ceil(timeout));
  if (!isNull(queueSize  )) opts["QUEUE_LIMIT_PACKETS"] = Utilities::format("%d", queueSize);
  if (!isNull(recvBufSize)) opts["SO_RCVBUF"          ] = Utilities::format("%d", recvBufSize);

  return opts;
}

/** This is a dirty trick to work around the fact that we can't use delegating
 *  constructors yet (they aren't introduced until C++11).
 */
#define _VRTReader(transport,host,port,device,listener,options,noDelete) \
  NetIO(false, transport, host, port, device, listener, options, \
        (transport == TransportProtocol_UDP)? NetUtilities::MAX_UDP_LEN : BasicVRLFrame::HEADER_LENGTH), \
  deletePointers(!noDelete || (listener==NULL)) \
{ \
  int32_t _timeout = NetUtilities::getSocketOptionInt(options, "INITIAL_TIMEOUT", 60); \
  if (_timeout == -2 /*LEGACY_MODE*/) { \
    timeoutMS   = -2 /*LEGACY_MODE*/; \
    startTimeMS = -2 /*LEGACY_MODE*/; \
    initialData = BasicVRTPacket::NULL_PACKET; \
    initialCtx  = BasicVRTPacket::NULL_PACKET; \
    idContext   = INT32_NULL; \
  } \
  else { \
    timeoutMS   = (_timeout < 0)? ((int64_t)_timeout) : ((int64_t)(_timeout * 1000)); \
    startTimeMS = 0; \
    initialData = BasicVRTPacket::NULL_PACKET; \
    initialCtx  = BasicVRTPacket::NULL_PACKET; \
    idContext   = INT32_NULL; \
  } \
}

VRTReader::VRTReader (TransportProtocol transport, const string &host, int32_t port,
                      const string &device, VRTEventListener *listener,
                      map<string,string> options, bool noDelete) :
  _VRTReader(transport,host,port,device,listener,options,noDelete)

VRTReader::VRTReader (int32_t port, const string &device,
                      int32_t recvBufSize, int32_t packetQueueSize, bool readOnly) :
  _VRTReader(TransportProtocol_UDP, "", port, device, NULL,
             makeOptions(DOUBLE_NAN,recvBufSize,packetQueueSize,readOnly),
             true)

VRTReader::VRTReader (const string &host, int32_t port, const string &device,
                      int32_t recvBufSize, int32_t packetQueueSize, bool readOnly) :
  _VRTReader(TransportProtocol_UDP, host, port, device, NULL,
             makeOptions(DOUBLE_NAN,recvBufSize,packetQueueSize,readOnly),
             true)

VRTReader::VRTReader (int32_t port, const string &device,
                      VRTContextListener *listener, double timeout,
                      int32_t recvBufSize, int32_t packetQueueSize,
                      bool readOnly, bool deletePointers) :
  _VRTReader(TransportProtocol_UDP, "", port, device, listener,
             makeOptions(timeout,recvBufSize,packetQueueSize,readOnly),
             deletePointers)

VRTReader::VRTReader (const string &host, int32_t port, const string &device,
                      VRTContextListener *listener, double timeout,
                      int32_t recvBufSize, int32_t packetQueueSize,
                      bool readOnly, bool deletePointers) :
  _VRTReader(TransportProtocol_UDP, host, port, device, listener,
             makeOptions(timeout,recvBufSize,packetQueueSize,readOnly),
             deletePointers)

VRTReader::~VRTReader () {
  close();
}

void VRTReader::handle (vector<char> *buffer, int32_t length) {
  if (startTimeMS == 0) {
    // start the clock on the first datagram received
    startTimeMS = Utilities::currentTimeMillis();
  }

  try {
    if (BasicVRLFrame::isVRL(*buffer, 0)) {
      tempFrame.swap(buffer);
      handle(tempFrame);
    }
    else {
      handle(VRTConfig::getPacketSwap(*buffer, 0, length));
    }
    delete buffer;
  }
  catch (VRTException e) {
    delete buffer;
    throw e;
  }
}

void VRTReader::handle (BasicVRLFrame &frame) {
  string err = frame.getFrameValid(false);

  if (!isNull(err)) {
    listenerSupport_fireErrorOccurred("%s", err.c_str());
    return;
  }
  int32_t count    = frame.getFrameCount();
  int32_t next     = (count+1) & 0xFFF;
  int32_t expected = frameCounter; frameCounter = next;

  if ((expected != count) && (expected >= 0)) {
    listenerSupport_fireErrorOccurred("Missed frames %d (inclusive) to %d (exclusive).", expected, count);
  }

  vector<BasicVRTPacket*> packets = frame.getVRTPackets();
  for (size_t i = 0; i < packets.size(); i++) {
    handle(packets[i]);
  }
}

void VRTReader::handle (BasicVRTPacket *packet) {
  bool pktSent = false;
  try {
    _handle(packet, pktSent);
    if (!pktSent || deletePointers) delete packet;
  }
  catch (VRTException e) {
    if (!pktSent || deletePointers) delete packet;
    throw e;
  }
}

void VRTReader::_handle (BasicVRTPacket *packet, bool &pktSent) {
  string err = packet->getPacketValid(false);

  if (!isNull(err)) {
    listenerSupport_fireErrorOccurred("%s", err.c_str());
    return;
  }
  int64_t code     = packet->getStreamCode();
  int32_t count    = packet->getPacketCount();
  int32_t expected = (packetCounters.count(code) == 0)? count : packetCounters[code];
  int32_t next     = (count+1) & 0xF;

  packetCounters[code] = next;

  if (count != expected) {
    listenerSupport_fireErrorOccurred("Missed packets %d (inclusive) to %d (exclusive).", expected, count);
  }

  // SEE IF WE ARE USING LEGACY MODE ========================================
  if (startTimeMS == -2 /*LEGACY_MODE*/) {
    pktSent = true;
    listenerSupport_fireReceivedPacket(packet);
    return;
  }

  // SEE IF WE ALREADY FOUND INITIAL CONTEXT ================================
  if (startTimeMS == FOUND_INITIAL) {
    if (packet->isData()) {
      pktSent = true;
      listenerSupport_fireReceivedDataPacket(packet);
    }
    else {
      pktSent = true;
      listenerSupport_fireReceivedContextPacket(packet);
    }
    return;
  }

  // STILL NEED MORE CONTEXT (OR INITIAL DATA PACKET) =======================
  int64_t now     = Utilities::currentTimeMillis();
  bool    timeout = (timeoutMS > 0) && (startTimeMS+timeoutMS <= now);

  // ---- If this is a DataPacket, handle it as such ------------------------
  if (packet->isData()) {
    initialData = BasicDataPacket(*packet);
    idContext   = initialData.getStreamIdentifier();

    if (isNull(idContext)) {
      fireReceivedInitialContextDone(); // unidentified stream (i.e. done)
      // No initial context -> this can be our first data packet.
      pktSent = true;
      listenerSupport_fireReceivedDataPacket(packet);
    }
    else if (timeout) {
      fireReceivedInitialContext(NO_CONTEXT_STREAM);
      // No initial context -> this can be our first data packet.
      pktSent = true;
      listenerSupport_fireReceivedDataPacket(packet);
    }
    else if (isNull(initialCtx)) {
      // We can now associate the initial context if already received. Prior
      // versions did not do this which presented a situation where the full
      // context was received prior to any data but we don't recognize that
      // until a later context packet comes in. This wasn't a big deal with
      // UDP/Multicast since we were connecting asynchronously. But with
      // UDP/Unicast or TCP is it highly likely the sender will give us any
      // context information first.
      if (initialContext.count(idContext)) {
        initialCtx = initialContext[idContext];
      }
      requiredContext.insert(idContext);

      if (checkInitialContextDone(timeout)) {
        // Initial context is already complete, this can be our first data packet.
        pktSent = true;
        listenerSupport_fireReceivedDataPacket(packet);
      }
    }
    return;
  }

  // ---- Found a context packet --------------------------------------------
  int32_t id = packet->getStreamIdentifier();
  initialContext[id] = *checked_dynamic_cast<BasicContextPacket*>(packet);

  // ---- Is this a non-ContextPacket primary stream (rare)? ----------------
  if (!isNull(idContext) && (id == idContext) && (packet->getPacketType() != PacketType_Context)) {
    initialCtx = BasicVRTPacket(*packet);
    if (initialContext.size() == 1) {
      fireReceivedInitialContextDone();
    }
    else {
      fireReceivedInitialContext("Context packets do not follow stream ID rules (found streams %s "
                                 "but expected only %s).", toStringKeys(initialContext).c_str(),
                                  toStringSet(requiredContext).c_str());
    }
    return;
  }

  // ---- For any ContextPackets, check assoc. lists first ------------------
  if (packet->getPacketType() == PacketType_Context) {
    BasicContextPacket *ctx   = checked_dynamic_cast<BasicContextPacket*>(packet);
    ContextAssocLists   assoc = ctx->getContextAssocLists();

    if (!isNull(idContext) && (id == idContext)) {
      // it is the primary, set it and mark as required
      initialCtx = BasicVRTPacket(*ctx);
      requiredContext.insert(id);
    }
    if (!isNull(assoc)) {
      vector<int32_t> src = assoc.getSourceContext();
      vector<int32_t> sys = assoc.getSystemContext();

      for (size_t i = 0; i < src.size(); i++) requiredContext.insert(src[i]);
      for (size_t i = 0; i < sys.size(); i++) requiredContext.insert(sys[i]);
    }
  }

  // ---- Check to see if done ----------------------------------------------
  checkInitialContextDone(timeout);
}

bool VRTReader::checkInitialContextDone (bool timeout) {
  bool foundCtx = !isNull(initialCtx);
  bool sameSize = initialContext.size() == requiredContext.size();
  bool foundAll = containsAll(initialContext, requiredContext);

  if (foundCtx && foundAll) {
    // found all required context
    if (sameSize) {
      fireReceivedInitialContextDone();
      return true;
    }
    else {
      fireReceivedInitialContext("Context packets do not follow stream ID rules (found streams %s "
                                 "but expected %s).", toStringKeys(initialContext).c_str(),
                                  toStringSet(requiredContext).c_str());
      return true;
    }
  }

  if (timeout && foundCtx) {
    // timeout before finding all required context
    if (sameSize) {
      fireReceivedInitialContext("Context packets do not follow stream ID rules (found streams %s "
                                 "but expected %s).", toStringKeys(initialContext).c_str(),
                                  toStringSet(requiredContext).c_str());
      return true;
    }
    else {
      fireReceivedInitialContext("Timeout before all required context could be found (found streams %s "
                                 "but expected %s).", toStringKeys(initialContext).c_str(),
                                  toStringSet(requiredContext).c_str());
      return true;
    }
  }

  if (timeout) {
    // timeout before finding primary context
    if (isNull(initialData)) {
      fireReceivedInitialContext(NO_DATA_STREAM);
      return true;
    }
    else {
      fireReceivedInitialContext("Could not find IF Context for stream ID %s.",
                                 initialData.getStreamID().c_str());
      return true;
    }
  }
  return false;
}

vector<char>* VRTReader::receiveBufferUDP (bool autoPush) {
  int32_t numRead = datagramSocket->read(&buffer[0], buffer.size());
  if (numRead <= 0) return NULL;

  if (autoPush && (numRead > BasicVRLFrame::MIN_FRAME_LENGTH)
               && BasicVRLFrame::isVRL(buffer,0)) {
    // Rather than creating a new buffer now and then creating another later for
    // each packet in a VRL frame, we can slice the frame up into packets now
    // and reduce the number of memory copies. However, if this fails, just
    // fall through and push the whole thing onto the queue.
    vector<vector<char>*> *packets = NULL;
    try {
      packets = BasicVRLFrame::getVRTPackets(buffer, numRead);
      if (packets != NULL) {
        // Push "empty" frame first so we can still check the frame counters
        vector<char> *buf = new vector<char>(12);
        (*buf)[ 0] = BasicVRLFrame::VRL_FAW_0;
        (*buf)[ 1] = BasicVRLFrame::VRL_FAW_1;
        (*buf)[ 2] = BasicVRLFrame::VRL_FAW_2;
        (*buf)[ 3] = BasicVRLFrame::VRL_FAW_3;
        (*buf)[ 4] = (char)(buffer[4]);        // <-- Copy counter
        (*buf)[ 5] = (char)(buffer[5] & 0xF0); // <-- Copy counter
        (*buf)[ 6] = 0;                        // <-- Set size to 12 octets
        (*buf)[ 7] = 3;                        // <-- Set size to 12 octets
        (*buf)[ 8] = BasicVRLFrame::NO_CRC_0;
        (*buf)[ 9] = BasicVRLFrame::NO_CRC_1;
        (*buf)[10] = BasicVRLFrame::NO_CRC_2;
        (*buf)[11] = BasicVRLFrame::NO_CRC_3;
        pushPackets(buf, packets);
        safe_delete(packets);
        return BUFFER_PUSHED;
      }
    }
    catch (VRTException e) {
      // Ignore this for now
      UNUSED_VARIABLE(e);
      safe_delete(packets);
    }
  }

  return new vector<char>(buffer.begin(), buffer.begin()+numRead);
}

vector<char>* VRTReader::receiveBufferTCP (bool autoPush) {
  UNUSED_VARIABLE(autoPush);

  // TCP is more difficult since there is no underlying framing structure so
  // we need to read some data, figure out the size, and then read the rest.
  // If anything fails mid-way-through we are in trouble and throwing an
  // exception is about the only thing we can do.
  int32_t numRead = _tcpSocketRead(&buffer[0], 0, 4, false);
  if (numRead <= 0) return NULL;

  bool    isVRL   = BasicVRLFrame::isVRL(buffer, 0);
  int32_t totRead = numRead;
  int32_t toRead;
  int32_t len;

  if (isVRL) {
    toRead  = BasicVRLFrame::HEADER_LENGTH - totRead;
    numRead = _tcpSocketRead(&buffer[0], totRead, toRead, true);
    if (numRead < 0) return NULL;
    totRead += numRead;
    len = BasicVRLFrame::getFrameLength(buffer, 0);
  }
  else {
    len = BasicVRTPacket::getPacketLength(buffer, 0);
  }

  vector<char> *buf = new vector<char>(len);
  char         *ptr = &(*buf)[0];
  memcpy(ptr, &buffer[0], totRead);

  toRead  = len - totRead;
  numRead = _tcpSocketRead(ptr, totRead, toRead, true);

  if (numRead < 0) {
    safe_delete(buf);
    return NULL;
  }
  return buf;
}
