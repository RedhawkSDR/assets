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

#include "VRTWriter.h"
#include <string>
#include <unistd.h>

using namespace std;
using namespace vrt;

/** Quick inline conversion to pointer, used in macro expansion. */
inline       BasicVRTPacket* asPointer (      BasicVRTPacket *p) { return  p; }
inline       BasicVRTPacket* asPointer (      BasicVRTPacket &p) { return &p; }
inline const BasicVRTPacket* asPointer (const BasicVRTPacket *p) { return  p; }
inline const BasicVRTPacket* asPointer (const BasicVRTPacket &p) { return &p; }

/** Applies function to all packets */
#define forAllPacketsPtr(func) \
  /* never null */func(const_cast<BasicVRTPacket*>(p0)); \
  /* never null */func(const_cast<BasicVRTPacket*>(p1)); \
  if (p2 != NULL) func(const_cast<BasicVRTPacket*>(p2)); \
  if (p3 != NULL) func(const_cast<BasicVRTPacket*>(p3)); \
  if (p4 != NULL) func(const_cast<BasicVRTPacket*>(p4)); \
  if (p5 != NULL) func(const_cast<BasicVRTPacket*>(p5)); \
  if (p6 != NULL) func(const_cast<BasicVRTPacket*>(p6)); \
  if (p7 != NULL) func(const_cast<BasicVRTPacket*>(p7)); \
  if (p8 != NULL) func(const_cast<BasicVRTPacket*>(p8)); \
  if (p9 != NULL) func(const_cast<BasicVRTPacket*>(p9)); \
  if (pA != NULL) func(const_cast<BasicVRTPacket*>(pA)); \
  if (pB != NULL) func(const_cast<BasicVRTPacket*>(pB)); \
  if (pC != NULL) func(const_cast<BasicVRTPacket*>(pC)); \
  if (pD != NULL) func(const_cast<BasicVRTPacket*>(pD)); \
  if (pE != NULL) func(const_cast<BasicVRTPacket*>(pE)); \
  if (pF != NULL) func(const_cast<BasicVRTPacket*>(pF));

#define forAllPacketsRef(func) \
  /* never null */ func(const_cast<BasicVRTPacket*>(&p0)); \
  /* never null */ func(const_cast<BasicVRTPacket*>(&p1)); \
  if (!isNull(p2)) func(const_cast<BasicVRTPacket*>(&p2)); \
  if (!isNull(p3)) func(const_cast<BasicVRTPacket*>(&p3)); \
  if (!isNull(p4)) func(const_cast<BasicVRTPacket*>(&p4)); \
  if (!isNull(p5)) func(const_cast<BasicVRTPacket*>(&p5)); \
  if (!isNull(p6)) func(const_cast<BasicVRTPacket*>(&p6)); \
  if (!isNull(p7)) func(const_cast<BasicVRTPacket*>(&p7)); \
  if (!isNull(p8)) func(const_cast<BasicVRTPacket*>(&p8)); \
  if (!isNull(p9)) func(const_cast<BasicVRTPacket*>(&p9)); \
  if (!isNull(pA)) func(const_cast<BasicVRTPacket*>(&pA)); \
  if (!isNull(pB)) func(const_cast<BasicVRTPacket*>(&pB)); \
  if (!isNull(pC)) func(const_cast<BasicVRTPacket*>(&pC)); \
  if (!isNull(pD)) func(const_cast<BasicVRTPacket*>(&pD)); \
  if (!isNull(pE)) func(const_cast<BasicVRTPacket*>(&pE)); \
  if (!isNull(pF)) func(const_cast<BasicVRTPacket*>(&pF));

/** Converts old individual options into a new map of options. */
static map<string,string> &makeOptions (bool useFrames, bool useCRC, uint8_t ttl) {
  static map<string,string> opts;

  opts["VRL_FRAME"       ] = (useFrames)? "true" : "false";
  opts["VRL_CRC"         ] = (useCRC   )? "true" : "false";
  opts["IP_MULTICAST_TTL"] = Utilities::format("%d", ttl);

  return opts;
}

/** This is a dirty trick to work around the fact that we can't use delegating
 *  constructors yet (they aren't introduced until C++11).
 */
#define _VRTWriter(transport,host,port,device,listener,options) \
  NetIO(true, transport, host, port, device, listener, options, 0) \
{ \
  if (NetUtilities::getSocketOptionBool(options, "VRL_FRAME", false)) { \
    this->packet = NULL; \
    this->frame  = new BasicVRLFrame(); \
    this->crc    = NetUtilities::getSocketOptionBool(options, "VRL_CRC", false); \
  } \
  else { \
    this->packet = new BasicVRTPacket(); \
    this->frame  = NULL; \
    this->crc    = false; /* n/a for packet */ \
  } \
  \
  int32_t vrlBreak = NetUtilities::getSocketOptionInt(options, "VRL_BREAK", -1); \
  int32_t mtu; \
  switch (transport) { \
    case TransportProtocol_TCP_SERVER: mtu = tcpServerSocket->getMTU();   break; \
    case TransportProtocol_TCP_CLIENT: mtu = tcpSockets->at(0)->getMTU(); break; \
    case TransportProtocol_UDP:        mtu = datagramSocket->getMTU();    break; \
    default:                           throw VRTException("Unknown transport type %d",transport); \
  } \
  \
  switch (vrlBreak) { \
    default: this->frameBreak = vrlBreak;  break;  /* user-specified        */ \
    case  0: this->frameBreak = 0;         break;  /* special mode          */ \
    case -1: this->frameBreak = 61440;     break;  /* 60 KiB                */ \
    case -2: this->frameBreak = mtu - 128; break;  /* MTU - 128 for headers */ \
  } \
}

// Need to disable Intel compiler warning 280 (selector expression is constant)
// since it will otherwise flag on the switch statement with TransportProtocol_UDP.
_Intel_Pragma("warning push")
_Intel_Pragma("warning disable 280")
VRTWriter::VRTWriter (const string &host, int32_t port, const string &device,
                      bool useFrames, bool useCRC, uint8_t ttl) :
  _VRTWriter(TransportProtocol_UDP, host, port, device, NULL,
             makeOptions(useFrames, useCRC, ttl))
_Intel_Pragma("warning pop")

VRTWriter::VRTWriter (TransportProtocol transport, const string &host, int32_t port,
                      const string &device, VRTEventListener *listener,
                      map<string,string> options) :
  _VRTWriter(transport, host, port, device, listener, options)


VRTWriter::~VRTWriter () {
  close();
  safe_delete(frame);
  safe_delete(packet);
}

void VRTWriter::updateCounter (BasicVRTPacket *p) {
  int64_t code = p->getStreamCode();

  int32_t count;
  if (packetCounters.count(code) == 0) {
    count = 0;
  }
  else {
    count = packetCounters[code] + 1;
  }
  packetCounters[code] = count;
  p->setPacketCount(count % 16);
}

void VRTWriter::updateCounter (void *p) {
  int64_t code = BasicVRTPacket::getStreamCode(p);

  int32_t count;
  if (packetCounters.count(code) == 0) {
    count = 0;
  }
  else {
    count = packetCounters[code];
  }
  count++;
  packetCounters[code] = count;
  BasicVRTPacket::setPacketCount(p, count % 16);
}

void VRTWriter::sendPacket (const BasicVRTPacket *p) {
  if (frame == NULL) _sendVRTPacket(p);
  else               _sendVRLFrame(p);
}

void VRTWriter::sendPackets (const vector<BasicVRTPacket*> &packets) {
  if (packets.size() == 0) {
    // nothing to send
  }
  else if (frame == NULL) {
    // Send individual packets
    for (size_t i = 0; i < packets.size(); i++) {
      _sendVRTPacket(packets[i]);
    }
  }
  else if (frameBreak == 0) {
    // Send packets (1 per-frame)
    for (size_t i = 0; i < packets.size(); i++) {
      _sendVRLFrame(packets[i]);
    }
  }
  else {
    // Send packets (1+ per-frame)
    _sendVRLFrame(packets);
  }
}

void VRTWriter::sendPackets (const vector<BasicVRTPacket> &packets) {
  if (packets.size() == 0) {
    // nothing to send
  }
  else if (frame == NULL) {
    // Send individual packets
    for (size_t i = 0; i < packets.size(); i++) {
      _sendVRTPacket(&packets[i]);
    }
  }
  else if (frameBreak == 0) {
    // Send packets (1 per-frame)
    for (size_t i = 0; i < packets.size(); i++) {
      _sendVRLFrame(&packets[i]);
    }
  }
  else {
    // Send packets (1+ per-frame)
    // Need to convert to pointers first
    vector<BasicVRTPacket*> _packets;
    for (size_t i = 0; i < packets.size(); i++) {
      _packets.push_back(const_cast<BasicVRTPacket*>(&packets[i]));
    }
    _sendVRLFrame(_packets);
  }
}

void VRTWriter::sendPackets (const BasicVRTPacket *p0, const BasicVRTPacket *p1,
                             const BasicVRTPacket *p2, const BasicVRTPacket *p3,
                             const BasicVRTPacket *p4, const BasicVRTPacket *p5,
                             const BasicVRTPacket *p6, const BasicVRTPacket *p7,
                             const BasicVRTPacket *p8, const BasicVRTPacket *p9,
                             const BasicVRTPacket *pA, const BasicVRTPacket *pB,
                             const BasicVRTPacket *pC, const BasicVRTPacket *pD,
                             const BasicVRTPacket *pE, const BasicVRTPacket *pF) {
  if (frame == NULL) {
    // Send individual packets
    forAllPacketsPtr(_sendVRTPacket);
  }
  else if (frameBreak == 0) {
    // Send packets (1 per-frame)
    forAllPacketsPtr(_sendVRLFrame);
  }
  else {
    vector<BasicVRTPacket*> packets;
    forAllPacketsPtr(packets.push_back);
    _sendVRLFrame(packets);
  }
}

void VRTWriter::sendPackets (const BasicVRTPacket &p0, const BasicVRTPacket &p1,
                             const BasicVRTPacket &p2, const BasicVRTPacket &p3,
                             const BasicVRTPacket &p4, const BasicVRTPacket &p5,
                             const BasicVRTPacket &p6, const BasicVRTPacket &p7,
                             const BasicVRTPacket &p8, const BasicVRTPacket &p9,
                             const BasicVRTPacket &pA, const BasicVRTPacket &pB,
                             const BasicVRTPacket &pC, const BasicVRTPacket &pD,
                             const BasicVRTPacket &pE, const BasicVRTPacket &pF) {
  if (frame == NULL) {
    // Send individual packets
    forAllPacketsRef(_sendVRTPacket);
  }
  else if (frameBreak == 0) {
    // Send packets (1 per-frame)
    forAllPacketsRef(_sendVRLFrame);
  }
  else {
    vector<BasicVRTPacket*> packets;
    forAllPacketsRef(packets.push_back);
    _sendVRLFrame(packets);
  }
}

void VRTWriter::_sendVRTPacket (const BasicVRTPacket *p) {
  // SANITY CHECKS
  if (p == NULL) throw VRTException("Can not send null.");
  string err = p->getPacketValid(true);
  if (!isNull(err)) throw VRTException(err);

  // FIT PACKET INTO DATAGRAM
  *packet = *p;

  // UPDATE COUNTERS (our copy only)
  updateCounter(packet);

  // SEND THE PACKET
  sendBuffer(packet->getPacketPointer(), packet->getPacketLength(), "VRTPacket", packet);

  // REPORT SENT PACKET
  listenerSupport_fireSentPacket(packet);
}

void VRTWriter::_sendVRLFrame (const vector<BasicVRTPacket*> &packets) {
  // FIT PACKETS INTO FRAME/DATAGRAM
  int32_t count = frame->setVRTPackets(frameBreak, packets);
  if (count < 1) {
    frame->setVRTPackets(BasicVRLFrame::MAX_FRAME_LENGTH, packets[0]);
    count = 1;
  }

  // UPDATE COUNTERS (our copy only) AND CRC (if applicable)
  vector<void*> packetsInFrame = frame->getVRTPacketsRW();
  for (size_t i = 0; i < packetsInFrame.size(); i++) {
    updateCounter(packetsInFrame[i]);
  }
  frameCounter++;
  frame->setFrameCount(frameCounter & 0xFFF);
  if (crc) frame->updateCRC();

  // SEND THE FRAME
  sendBuffer(frame->getFramePointer(), frame->getFrameLength(), "VRLFrame", count, packets);

  // REPORT SENT PACKETS
  listenerSupport_fireSentPackets(frame);

  // SEND ANY PACKETS THAT DID NOT FIT IN THIS FRAME
  int32_t needToSend = (int32_t)(packets.size()-count);
  if (needToSend == 1) {
    _sendVRLFrame(packets[count]);
  }
  else if (needToSend > 1) {
    vector<BasicVRTPacket*> next(packets.begin()+count, packets.end());
    _sendVRLFrame(next);
  }
}

void VRTWriter::_sendVRLFrame (const BasicVRTPacket *p) {
  // FIT PACKETS INTO FRAME/DATAGRAM
  frame->setVRTPackets(BasicVRLFrame::MAX_FRAME_LENGTH, packet);

  // UPDATE COUNTERS (our copy only) AND CRC (if applicable)
  vector<void*> packetsInFrame = frame->getVRTPacketsRW();
  updateCounter(packetsInFrame[0]);
  frameCounter++;
  frame->setFrameCount(frameCounter & 0xFFF);
  if (crc) frame->updateCRC();

  // SEND THE FRAME
  sendBuffer(frame->getFramePointer(), frame->getFrameLength(), "VRLFrame", packet);

  // REPORT SENT PACKETS
  listenerSupport_fireSentPacket(p);
}

void VRTWriter::sendBuffer (const void *buf, int32_t len, const string &type,
                            const BasicVRTPacket *packet) {
  try {
    NetIO::sendBuffer(buf, len);
  }
  catch (VRTException e) {
    string msg = string("Unable to send ") + type + " to " + hostAddr.toString();
    listenerSupport_fireErrorOccurredPkt(msg, e, packet);
  }
}

void VRTWriter::sendBuffer (const void *buf, int32_t len, const string &type,
                            int32_t count, const vector<BasicVRTPacket*> &packets) {
  try {
    NetIO::sendBuffer(buf, len);
  }
  catch (VRTException e) {
    string msg = string("Unable to send ") + type + " to " + hostAddr.toString();
    for (int32_t i = 0; i < count; i++) {
      listenerSupport_fireErrorOccurredPkt(msg, e, packets[i]);
    }
  }
}
