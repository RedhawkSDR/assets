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

#include "NetTestClient.h"
#include "BasicContextPacket.h"
#include "BasicDataPacket.h"
#include "StandardDataPacket.h"
#include "TestListener.h"
#include "TestRunner.h"
#include "UUID.h"
#include "VRTContextListener.h"
#include "VRTEvent.h"
#include "VRTReader.h"
#include "VRTWriter.h"
#include <errno.h>
#include <string>
#include <queue>

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

#ifndef HOST_NAME_MAX
# define HOST_NAME_MAX 255 // matches SUSv2 (Linux usually uses 64)
#endif

namespace vrttest {
  /** An implementation of {@link VRTContextListener} that stores the
   *  results of the previous calls.
   */
  class QueuedListener : public VRTContextListener {
    queue<TestListener*> _queue;

    public: virtual ~QueuedListener () {
      while (size() > 0) {
        TestListener *l = poll();
        safe_delete(l);
      }
    }

    /** Gets the number of messages in the queue. */
    public: int32_t size () const {
      SYNCHRONIZED(this);
      return _queue.size();
    }

    /** Polls the received messages. */
    public: TestListener* poll () {
      SYNCHRONIZED(this);
      if (size() == 0) return NULL;
      TestListener *l = _queue.front();
      _queue.pop();
      return l;
    }

    public: virtual string toString () const {
      SYNCHRONIZED(this);
      QueuedListener *self = const_cast<QueuedListener*>(this);

      string str = Utilities::format("QueuedListener with %d messages\n", size());
      queue<TestListener*> q;
      int32_t i = 0;
      while (size() > 0) {
        TestListener *l = self->poll();
        str += Utilities::format("%4d: %s\n", i++, l->toString().c_str());
        q.push(l);
      }
      self->_queue = q;
      return str;
    }

    public: virtual void receivedPacket (const VRTEvent &e, const BasicVRTPacket *p) {
      SYNCHRONIZED(this);
      TestListener *l = new TestListener();
      l->receivedPacket(e, p);
      _queue.push(l);
    }

    public: virtual void sentPacket (const VRTEvent &e, const BasicVRTPacket *p) {
      SYNCHRONIZED(this);
      TestListener *l = new TestListener();
      l->sentPacket(e, p);
      _queue.push(l);
    }

    public: virtual void errorOccurred (const VRTEvent &e, const string &msg, const VRTException &t) {
      SYNCHRONIZED(this);
      TestListener *l = new TestListener();
      l->errorOccurred(e, msg, t);
      _queue.push(l);
    }

    public: virtual void warningOccurred (const VRTEvent &e, const string &msg, const VRTException &t) {
      SYNCHRONIZED(this);
      TestListener *l = new TestListener();
      l->warningOccurred(e, msg, t);
      _queue.push(l);
    }

    public: virtual void receivedDataPacket (const VRTEvent &e, const BasicDataPacket *p) {
      SYNCHRONIZED(this);
      TestListener *l = new TestListener();
      l->receivedDataPacket(e, p);
      _queue.push(l);
    }

    public: virtual void receivedContextPacket (const VRTEvent &e, const BasicVRTPacket *p) {
      SYNCHRONIZED(this);
      TestListener *l = new TestListener();
      l->receivedContextPacket(e, p);
      _queue.push(l);
    }

    public: virtual void receivedInitialContext (const VRTEvent &e, const string &msg,
                                                 const BasicDataPacket &d, const BasicVRTPacket &c,
                                                 const map<int32_t,BasicVRTPacket> &m) {
      SYNCHRONIZED(this);
      TestListener *l = new TestListener();
      l->receivedInitialContext(e, msg, d, c, m);
      _queue.push(l);
    }
  };
} END_NAMESPACE

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

NetTestClient::NetTestClient () {
  if (isNull(VRTConfig::getTestFirstMCast())) {
    throw VRTException("Can not run network tests without VRT_TEST_FIRST_MCAST set.");
  }
  if (VRTConfig::getTestFirstPort() == 0) {
    throw VRTException("Can not run network tests without VRT_TEST_FIRST_PORT set.");
  }
  if (VRTConfig::getTestServer() == "") {
    throw VRTException("Can not run network tests without VRT_TEST_SERVER set.");
  }

  device     = VRTConfig::getTestDevice();
  mcHost     = VRTConfig::getTestFirstMCast();
  mcPort     = 27000;
  localHost  = InetAddress::getLocalHost().toString();
  svrHost    = NetUtilities::getHost(VRTConfig::getTestServer());
  svrPort    = VRTConfig::getTestFirstPort();
  svrPort2   = VRTConfig::getTestFirstPort() + 1;
  localPort  = VRTConfig::getTestFirstPort() + 2;
  localPort2 = VRTConfig::getTestFirstPort() + 3;
}

string NetTestClient::getTestedClass () {
  return "Networking Tests";
}

vector<TestSet> NetTestClient::init () {
  vector<TestSet> tests;

  tests.push_back(TestSet("VRTReader for TCP Client",     this,  "testVRTReaderTCPClient"));
  tests.push_back(TestSet("VRTReader for TCP Server",     this,  "testVRTReaderTCPServer"));
  tests.push_back(TestSet("VRTReader for UDP/Multicast",  this,  "testVRTReaderUDPMulticast"));
  tests.push_back(TestSet("VRTReader for UDP/Unicast",    this,  "testVRTReaderUDPUnicast"));

  tests.push_back(TestSet("VRTWriter for TCP Client",     this,  "testVRTWriterTCPClient"));
  tests.push_back(TestSet("VRTWriter for TCP Server",     this,  "testVRTWriterTCPServer"));
  tests.push_back(TestSet("VRTWriter for UDP/Multicast",  this,  "testVRTWriterUDPMulticast"));
  tests.push_back(TestSet("VRTWriter for UDP/Unicast",    this,  "testVRTWriterUDPUnicast"));

  return tests;
}

void NetTestClient::done () {
  // nothing to do
}

void NetTestClient::call (const string &func) {
  if (func == "testVRTReaderTCPClient"   ) testVRTReaderTCPClient();
  if (func == "testVRTReaderTCPServer"   ) testVRTReaderTCPServer();
  if (func == "testVRTReaderUDPMulticast") testVRTReaderUDPMulticast();
  if (func == "testVRTReaderUDPUnicast"  ) testVRTReaderUDPUnicast();
  if (func == "testVRTWriterTCPClient"   ) testVRTWriterTCPClient();
  if (func == "testVRTWriterTCPServer"   ) testVRTWriterTCPServer();
  if (func == "testVRTWriterUDPMulticast") testVRTWriterUDPMulticast();
  if (func == "testVRTWriterUDPUnicast"  ) testVRTWriterUDPUnicast();
}

void NetTestClient::testVRTReaderUDPMulticast () {
  TransportProtocol transport = TransportProtocol_UDP;
  _testVRTReader(transport, mcHost, mcPort, mcHost, mcPort, 128, 0.01,  32,   false, false);
  _testVRTReader(transport, mcHost, mcPort, mcHost, mcPort, 256, 0.001, 1000, true,  false);
  _testVRTReader(transport, mcHost, mcPort, mcHost, mcPort, 512, 0.001, 100,  true,  true);
}

void NetTestClient::testVRTReaderUDPUnicast () {
  TransportProtocol transport = TransportProtocol_UDP;
  _testVRTReader(transport, "", localPort, localHost, localPort, 128, 0.01,  32,   false, false);
  _testVRTReader(transport, "", localPort, localHost, localPort, 256, 0.001, 1000, true,  false);
  _testVRTReader(transport, "", localPort, localHost, localPort, 512, 0.001, 100,  true,  true);
}

void NetTestClient::testVRTReaderTCPClient () {
  TransportProtocol transport = TransportProtocol_TCP_CLIENT;
  _testVRTReader(transport, svrHost, svrPort, "", svrPort, 128, 0.01,  32,   false, false);
  _testVRTReader(transport, svrHost, svrPort, "", svrPort, 256, 0.001, 1000, true,  false);
  _testVRTReader(transport, svrHost, svrPort, "", svrPort, 512, 0.001, 100,  true,  true);
}

void NetTestClient::testVRTReaderTCPServer () {
  TransportProtocol transport = TransportProtocol_TCP_SERVER;
  _testVRTReader(transport, "", localPort, localHost, localPort, 128, 0.01,  32,   false, false);
  _testVRTReader(transport, "", localPort, localHost, localPort, 256, 0.001, 1000, true,  false);
  _testVRTReader(transport, "", localPort, localHost, localPort, 512, 0.001, 100,  true,  true);
}

void NetTestClient::testVRTWriterUDPMulticast () {
  TransportProtocol transport = TransportProtocol_UDP;
  _testVRTWriter(transport, mcHost, mcPort, mcHost, mcPort, 128, 0.01,  32,   false, false);
  _testVRTWriter(transport, mcHost, mcPort, mcHost, mcPort, 256, 0.001, 1000, true,  false);
  _testVRTWriter(transport, mcHost, mcPort, mcHost, mcPort, 512, 0.001, 100,  true,  true);
}

void NetTestClient::testVRTWriterUDPUnicast () {
  TransportProtocol transport = TransportProtocol_UDP;
  _testVRTWriter(transport, svrHost, svrPort, svrHost, svrPort, 128, 0.01,  32,   false, false);
  _testVRTWriter(transport, svrHost, svrPort, svrHost, svrPort, 256, 0.001, 1000, true,  false);
  _testVRTWriter(transport, svrHost, svrPort, svrHost, svrPort, 512, 0.001, 100,  true,  true);
}

void NetTestClient::testVRTWriterTCPClient () {
  TransportProtocol transport = TransportProtocol_TCP_CLIENT;
  _testVRTWriter(transport, svrHost, svrPort, "", svrPort, 128, 0.01,  32,   false, false);
  _testVRTWriter(transport, svrHost, svrPort, "", svrPort, 256, 0.001, 1000, true,  false);
  _testVRTWriter(transport, svrHost, svrPort, "", svrPort, 512, 0.001, 100,  true,  true);
}

void NetTestClient::testVRTWriterTCPServer () {
  TransportProtocol transport = TransportProtocol_TCP_SERVER;
  _testVRTWriter(transport, "", localPort, localHost, localPort, 128, 0.01,  32,   false, false);
  _testVRTWriter(transport, "", localPort, localHost, localPort, 256, 0.001, 1000, true,  false);
  _testVRTWriter(transport, "", localPort, localHost, localPort, 512, 0.001, 100,  true,  true);
}

void NetTestClient::_testVRTReader (TransportProtocol transport, const string &host, int32_t port,
                                    const string &svrHost, int32_t svrPort,
                                    int32_t elements, double timeDelta, int32_t totDataPkts,
                                    bool vrlFrame, bool vrlCRC) {
  VRTReader    *in  = NULL;
  TestListener *evt = NULL;

  try {
    // ==== INITIALIZATION =====================================================
    QueuedListener     listener;
    map<string,string> options      = getTestSocketOptions(transport);
    int32_t            ctxInterval  = 10;
    int32_t            totCtxPkts   = (int32_t)ceil(totDataPkts / (double)ctxInterval);
    int32_t            totPkts      = totDataPkts + totCtxPkts;
    map<string,string> request;
    string             svrTransport;
    bool               connectFirst;

    switch (transport) {
      case TransportProtocol_TCP_CLIENT: svrTransport = "TCP_SERVER"; connectFirst = false; break;
      case TransportProtocol_TCP_SERVER: svrTransport = "TCP_CLIENT"; connectFirst = true;  break;
      default:                           svrTransport = "UDP";        connectFirst = true;  break;
    }

    request["TYPE"             ] = "RAMP";
    request["PROTOCOL"         ] = svrTransport;
    request["HOST"             ] = svrHost;
    request["PORT"             ] = Utilities::format("%d",svrPort);
    request["VRL-FRAME"        ] = (vrlFrame)? "true" : "false";
    request["VRL-CRC"          ] = (vrlCRC  )? "true" : "false";
    request["CONTEXT-INTERVAL" ] = Utilities::format("%d",ctxInterval);
    request["DATA-PACKET-COUNT"] = Utilities::format("%d",totDataPkts);
    request["DATA-PACKET-DELTA"] = Utilities::format("%e",timeDelta);
    request["DATA-PAYLOAD-FMT" ] = "Int8";
    request["DATA-ELEMENTS"    ] = Utilities::format("%d",elements);


    // ==== CONNECT AND WAIT FOR COMPLETION ==================================
    if (connectFirst) {
      in = new VRTReader(transport, host, port, device, &listener, options);
      in->start();
    }

    UUID taskID = sendStartRequest(request, 10000);

    if (!connectFirst) {
      in = new VRTReader(transport, host, port, device, &listener, options);
      in->start();
    }

    bool ok = waitForRequest(taskID, listener, totPkts, 10000, 1000);

    in->close();
//cout << "=========================================================" << endl;
//cout << listener << endl;
//cout << "=========================================================" << endl;

    // ==== CHECK RESULTS ====================================================
    assertEquals("Messages sent successfully",          true,    ok);
    assertEquals("Correct number of messages received", totPkts, listener.size());

    int32_t       count        = 0;
    int32_t       dataPktCount = 0;
    int32_t       ctxPktCount  = 0;
    PayloadFormat pf;

    while ((evt = listener.poll()) != NULL) {
      string msg = Utilities::format("Event %d ", count);

      if (count == 0) { // First should be receivedInitialContext(..)
        // On the initial the packet count should be 0 as this should be the
        // first of each type received. Since the data should come after the
        // context, do NOT increment the data counter since that packet should
        // repeat as the next data packet received.
        assertEquals(msg+"type",             Mode_receivedInitialContext, evt->mode);
        assertEquals(msg+"event.source",     in,                          evt->event.getSource());
        assertEquals(msg+"message",          "",                          evt->message);
        assertEquals(msg+"data.StreamID",    100,                         evt->data.getStreamIdentifier());
        assertEquals(msg+"data.PacketCount", dataPktCount % 16,           evt->data.getPacketCount());
        assertEquals(msg+"ctx.StreamID",     100,                         evt->ctx.getStreamIdentifier());
        assertEquals(msg+"ctx.PacketCount",  ctxPktCount % 16,            evt->ctx.getPacketCount());
        assertEquals(msg+"context.size",     (size_t)1,                   evt->context.size());
        assertEquals(msg+"context[100]",     evt->ctx,                    evt->context[100]);
        BasicContextPacket _ctx = evt->ctx;
        pf = _ctx.getDataPayloadFormat();
        ctxPktCount++;
      }
      else if ((count % (ctxInterval+1)) == 0) {
        assertEquals(msg+"type",               Mode_receivedContextPacket, evt->mode);
        assertEquals(msg+"event.source",       in,                         evt->event.getSource());
        assertEquals(msg+"packet.StreamID",    100,                        evt->packet.getStreamIdentifier());
        assertEquals(msg+"packet.PacketCount", ctxPktCount % 16,           evt->packet.getPacketCount());
        ctxPktCount++;
      }
      else {
        assertEquals(msg+"type",               Mode_receivedDataPacket, evt->mode);
        assertEquals(msg+"event.source",       in,                      evt->event.getSource());
        assertEquals(msg+"packet.StreamID",    100,                     evt->packet.getStreamIdentifier());
        assertEquals(msg+"packet.PacketCount", dataPktCount % 16,       evt->packet.getPacketCount());

        BasicDataPacket _data = evt->packet;

        vector<int8_t> exp(elements);
        vector<int8_t> act = _data.getDataByte(pf);

        for (int32_t idx=0,val=(dataPktCount % 16); idx < elements; idx++,val++) {
          exp[idx] = (char)val;
        }
        assertArrayEquals(msg+"packet.DataByte", exp, act);
        dataPktCount++;
      }
      count++;
      safe_delete(evt);
    }
    safe_delete(in);
  }
  catch (VRTException e) {
    safe_delete(in);
    safe_delete(evt);
    throw e;
  }
}


void NetTestClient::_testVRTWriter (TransportProtocol transport, const string &host, int32_t port,
                                    const string &svrHost, int32_t svrPort,
                                    int32_t elements, double timeDelta, int32_t totDataPkts,
                                    bool vrlFrame, bool vrlCRC) {
  VRTReader      *in     = NULL;
  VRTWriter      *out    = NULL;
  TestListener   *inEvt  = NULL;
  TestListener   *outEvt = NULL;
  QueuedListener  outListener;
  QueuedListener  inListener;

  try {
    // ==== INITIALIZATION =====================================================
    int64_t            timeDeltaPS  = (int64_t)(timeDelta * TimeStamp::ONE_SEC);
    map<string,string> outOptions   = getTestSocketOptions(transport);
    map<string,string> inOptions    = getTestSocketOptions(transport);
    int32_t            ctxInterval  = 10;
    int32_t            totCtxPkts   = (int32_t)ceil(totDataPkts / (double)ctxInterval);
    int32_t            totPkts      = totDataPkts + totCtxPkts;
    map<string,string> request;
    PayloadFormat      pf           = PayloadFormat_INT8;
    char               data[16]     = { 0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7,
                                        0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF };
    string             svrTransport;
    bool               connectFirst;

    switch (transport) {
      case TransportProtocol_TCP_CLIENT: svrTransport = "TCP_SERVER"; connectFirst = false; break;
      case TransportProtocol_TCP_SERVER: svrTransport = "TCP_CLIENT"; connectFirst = true;  break;
      default:                           svrTransport = "UDP";        connectFirst = false; break;
    }

    request["TYPE"             ] = "ECHO";
    request["PROTOCOL"         ] = "UDP";
    request["HOST"             ] = localHost;
    request["PORT"             ] = Utilities::format("%d",localPort2);
    request["IN-PROTOCOL"      ] = svrTransport;
    request["IN-HOST"          ] = svrHost;
    request["IN-PORT"          ] = Utilities::format("%d",svrPort);


    // ==== CONNECT AND WAIT FOR COMPLETION ==================================
    if (connectFirst) {
      out = new VRTWriter(transport, host, port, device, &outListener, outOptions);
      out->start();
    }

    UUID taskID = sendStartRequest(request, 10000);

    if (!connectFirst) {
      out = new VRTWriter(transport, host, port, device, &outListener, outOptions);
      out->start();
    }
    in = new VRTReader(TransportProtocol_UDP, localHost, localPort2, device, &inListener, inOptions);
    in->start();

    TimeStamp ts = TimeStamp::getSystemTime();
    for (int32_t i = 0; i < totDataPkts; i++) {
      BasicContextPacket ctx;
      StandardDataPacket dat;
      ctx.setStreamIdentifier(100);
      ctx.setTimeStamp(ts);
      ctx.setDataPayloadFormat(pf);
      dat.setStreamIdentifier(100);
      dat.setTimeStamp(ts);
      dat.setPayloadFormat(pf);
      dat.setPayload(data, 16);

      if ((i % ctxInterval) == 0) {
        out->sendPackets(ctx, dat);
      }
      else {
        out->sendPacket(dat);
      }

      ts = ts.addPicoSeconds(timeDeltaPS);
      Utilities::sleepUntil(ts);
    }
    out->close();

    waitForRequest(taskID, inListener, totPkts, 10000, 1000);
    in->close();
//cout << "=========================================================" << endl;
//cout << outListener << endl;
//cout << "=========================================================" << endl;
//cout << inListener << endl;
//cout << "=========================================================" << endl;

    // ==== CHECK RESULTS ====================================================
    assertEquals("Correct number of messages transmitted", totPkts, outListener.size());
    assertEquals("Correct number of messages received",    totPkts, inListener.size());

    int32_t count        = 0;
    int32_t dataPktCount = 0;
    int32_t ctxPktCount  = 0;

    while ((outEvt = outListener.poll()) != NULL) {
      inEvt = inListener.poll();
      string msg = Utilities::format("Event %d ", count);

      if (count == 0) { // First should be receivedInitialContext(..)
        // On the initial the packet count should be 0 as this should be the
        // first of each type received. Since the data should come after the
        // context, do NOT increment the data counter since that packet should
        // repeat as the next data packet received.
        assertEquals(msg+"(OUT) type",               Mode_sentPacket,             outEvt->mode);
        assertEquals(msg+"(OUT) event.source",       out,                         outEvt->event.getSource());
        assertEquals(msg+"(OUT) packet.StreamID",    100,                         outEvt->packet.getStreamIdentifier());
        assertEquals(msg+"(OUT) packet.PacketCount", ctxPktCount % 16,            outEvt->packet.getPacketCount());

        assertEquals(msg+"(IN) type",                Mode_receivedInitialContext, inEvt->mode);
        assertEquals(msg+"(IN) event.source",        in,                          inEvt->event.getSource());
        assertEquals(msg+"(IN) message",             "",                          inEvt->message);
        assertEquals(msg+"(IN) data.StreamID",       100,                         inEvt->data.getStreamIdentifier());
        assertEquals(msg+"(IN) data.PacketCount",    dataPktCount % 16,           inEvt->data.getPacketCount());
        assertEquals(msg+"(IN) ctx.StreamID",        100,                         inEvt->ctx.getStreamIdentifier());
        assertEquals(msg+"(IN) ctx.PacketCount",     ctxPktCount % 16,            inEvt->ctx.getPacketCount());
        assertEquals(msg+"(IN) context.size",        (size_t)1,                   inEvt->context.size());
        assertEquals(msg+"(IN) context[100]",        inEvt->ctx,                  inEvt->context[100]);

        assertEquals(msg+"(I/O) input = output",     outEvt->packet,              inEvt->ctx);
        ctxPktCount++;
      }
      else if ((count % (ctxInterval+1)) == 0) {
        assertEquals(msg+"(OUT) type",               Mode_sentPacket,             outEvt->mode);
        assertEquals(msg+"(OUT) event.source",       out,                         outEvt->event.getSource());
        assertEquals(msg+"(OUT) packet.StreamID",    100,                         outEvt->packet.getStreamIdentifier());
        assertEquals(msg+"(OUT) packet.PacketCount", ctxPktCount % 16,            outEvt->packet.getPacketCount());

        assertEquals(msg+"(IN) type",                Mode_receivedContextPacket,  inEvt->mode);
        assertEquals(msg+"(IN) event.source",        in,                          inEvt->event.getSource());
        assertEquals(msg+"(IN) packet.StreamID",     100,                         inEvt->packet.getStreamIdentifier());
        assertEquals(msg+"(IN) packet.PacketCount",  ctxPktCount % 16,            inEvt->packet.getPacketCount());

        assertEquals(msg+"(I/O) input = output",     outEvt->packet,              inEvt->packet);
        ctxPktCount++;
      }
      else {
        assertEquals(msg+"(OUT) type",               Mode_sentPacket,             outEvt->mode);
        assertEquals(msg+"(OUT) event.source",       out,                         outEvt->event.getSource());
        assertEquals(msg+"(OUT) packet.StreamID",    100,                         outEvt->packet.getStreamIdentifier());
        assertEquals(msg+"(OUT) packet.PacketCount", dataPktCount % 16,           outEvt->packet.getPacketCount());

        assertEquals(msg+"(IN) type",                Mode_receivedDataPacket,     inEvt->mode);
        assertEquals(msg+"(IN) event.source",        in,                          inEvt->event.getSource());
        assertEquals(msg+"(IN) packet.StreamID",     100,                         inEvt->packet.getStreamIdentifier());
        assertEquals(msg+"(IN) packet.PacketCount",  dataPktCount % 16,           inEvt->packet.getPacketCount());

        assertEquals(msg+"(I/O) input = output",     outEvt->packet,              inEvt->packet);
        dataPktCount++;
      }
      count++;
      safe_delete(outEvt);
      safe_delete(inEvt);
    }

    safe_delete(out);
    safe_delete(in);
  }
  catch (VRTException e) {
    safe_delete(out);
    safe_delete(in);
    safe_delete(outEvt);
    safe_delete(inEvt);
    throw e;
  }
}

/** Gets a field from a response value. */
static string getField (Value *val, const string &name) {
  HasFields *hf = val->as<HasFields*>();
  if (hf == NULL) return "";
  Value *v = hf->getFieldByName(name);
  if (v == NULL) return "";
  string str = v->toString();
  safe_delete(v);
  return str;
}

UUID NetTestClient::sendStartRequest (map<string,string> request, int32_t wait) {
  Value *response  = NULL;
  Value *queryResp = NULL;
  try {
    // ==== SEND START REQUEST =================================================
           response = sendServerRequest("/start", request);
    string status   = getField(response, "STATUS");
    string message  = getField(response, "MESSAGE");
    string task     = getField(response, "TASK");
    UUID   uuid;
    safe_delete(response);

    if (isNull(status)) {
      throw VRTException("Invalid server response null to start request");
    }
    else if (status == "OK") {
      // ok
    }
    else {
      throw VRTException(string("Failed start request: ")+status+" - "+message);
    }

    if (isNull(task)) {
      throw VRTException(string("Invalid task ID ")+task+" from start request");
    }
    try {
      uuid.setUUID(task);
    }
    catch (VRTException e) {
      UNUSED_VARIABLE(e);
      throw VRTException(string("Invalid task ID ")+task+" from start request");
    }

    // ==== INITIAL WAIT FOR STARTUP ===========================================
    Utilities::sleep(1000);

    // ==== WAIT FOR STARTUP ===================================================
    int64_t            endWait  = Utilities::currentTimeMillis() + wait;
    map<string,string> queryReq;
    queryReq["TASK"] = uuid.toString();

    while (Utilities::currentTimeMillis() < endWait) {
             queryResp    = sendServerRequest("/query", queryReq);
      string queryStatus  = getField(queryResp, "STATUS");
      string queryMessage = getField(queryResp, "MESSAGE");
      string queryResult  = getField(queryResp, "RESULT");
      safe_delete(queryResp);

      if (queryStatus == "OK") {
        if (isNull(queryResult)) {
          throw VRTException("Invalid server response to query request RESULT=null");
        }
        else if (queryResult == "PENDING") {
          Utilities::sleep(1000); // keep waiting (time in ms)
        }
        else if (queryResult == "RUNNING") {
          return uuid; // expected status
        }
        else if (queryResult == "COMPLETED") {
          return uuid; // rare case where task (e.g. "RAMP") completes before getting here
        }
        else {
          throw VRTException(string("Invalid server response to query request RESULT=")+queryResult);
        }
      }
      else {
        throw VRTException(string("Failed query request: ")+queryStatus+" - "+queryMessage);
      }
    }
    throw VRTException("Server task failed to start within given wait time");
  }
  catch (VRTException e) {
    safe_delete(response);
    safe_delete(queryResp);
    throw e;
  }
}

bool NetTestClient::waitForRequest (const UUID &uuid, QueuedListener &listener,
                                    int32_t count, int32_t wait, int32_t waitStop) {
  Value *queryResp = NULL;
  Value *stopResp  = NULL;

  try {
    // ==== WAIT FOR TASK TO COMPLETE ==========================================
    int64_t            endWait  = Utilities::currentTimeMillis() + wait;
    map<string,string> queryReq;
    queryReq["TASK"] = uuid.toString();

    while (Utilities::currentTimeMillis() < endWait) {
             queryResp     = sendServerRequest("/query", queryReq);
      string queryStatus   = getField(queryResp, "STATUS");
      string queryMessage  = getField(queryResp, "MESSAGE");
      string queryResult   = getField(queryResp, "RESULT");
      safe_delete(queryResp);

      if (isNull(queryStatus)) {
        throw VRTException("Invalid server response null to query request");
      }
      if (queryStatus == "OK") {
        if (isNull(queryResult)) {
          throw VRTException("Invalid server response to query request RESULT=null");
        }
        else if (queryResult == "RUNNING") {
          if (listener.size() >= count) {
            Utilities::sleep(500); // wait for completion
            waitStop -= 500;
            if (waitStop <= 0) break;
          }
          else {
            Utilities::sleep(1000); // keep waiting (time in ms)
          }
        }
        else if (queryResult == "COMPLETED") {
          return true;
        }
        else if ((queryResult == "FAILED") || (queryResult == "ABORTED")) {
          return false;
        }
        else if (queryResult == "PENDING") {
          Utilities::sleep(1000); // wait for task to start
        }
        else {
          throw VRTException(string("Invalid server response to query request RESULT=")+queryResult);
        }
      }
      else if ((queryStatus == "NOT-FOUND")) {
        return true; // Assume complete and dropped from server listing
      }
      else {
        throw VRTException(string("Failed query request: ")+queryStatus+" - "+queryMessage);
      }
    }

    // ==== TELL SERVER TO STOP ================================================
    stopResp = sendServerRequest("/stop", queryReq);
    safe_delete(stopResp);
    return true;
  }
  catch (VRTException e) {
    safe_delete(queryResp);
    safe_delete(stopResp);
    throw e;
  }
}
