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

package nxm.vrttest.net;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nxm.vrt.lib.BasicContextPacket;
import nxm.vrt.lib.ContextPacket;
import nxm.vrt.lib.DataPacket;
import nxm.vrt.lib.StandardDataPacket;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.Utilities;
import nxm.vrt.lib.VRTConfig;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.lib.VRTPacket.PayloadFormat;
import nxm.vrt.net.NetUtilities;
import nxm.vrt.net.NetUtilities.TransportProtocol;
import nxm.vrt.net.VRTContextListener;
import nxm.vrt.net.VRTEvent;
import nxm.vrt.net.VRTReader;
import nxm.vrt.net.VRTWriter;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;
import nxm.vrttest.net.TestListener.Mode;

import static nxm.vrt.lib.Utilities.sleep;
import static nxm.vrt.lib.Utilities.sleepUntil;
import static nxm.vrttest.inc.TestRunner.assertArrayEquals;
import static nxm.vrttest.inc.TestRunner.assertEquals;
import static nxm.vrttest.inc.TestRunner.getTestSocketOptions;
import static nxm.vrttest.inc.TestRunner.sendServerRequest;

/** Runs the networking test cases. */
public final class NetTestClient implements Testable {
  private String device;     // The network device to use
  private String mcHost;     // The host to use for UDP/Multicast
  private int    mcPort;     // The host to use for UDP/Multicast
  private String localHost;  // The local host to use
  private int    localPort;  // The local port to use
  private int    localPort2; // The local port to use (alternate)
  private String svrHost;    // The server host to use
  private int    svrPort;    // The server port to use
  private int    svrPort2;   // The server port to use (alternate)
  private long   testDelay;  // The delay between tests in ms

  public NetTestClient () throws IOException {
    if (VRTConfig.TEST_FIRST_MCAST == null) {
      throw new RuntimeException("Can not run network tests without VRT_TEST_FIRST_MCAST set.");
    }
    if (VRTConfig.TEST_FIRST_PORT == 0) {
      throw new RuntimeException("Can not run network tests without VRT_TEST_FIRST_PORT set.");
    }
    if (VRTConfig.TEST_SERVER == null) {
      throw new RuntimeException("Can not run network tests without VRT_TEST_SERVER set.");
    }

    this.device     = VRTConfig.TEST_DEVICE;
    this.mcHost     = VRTConfig.TEST_FIRST_MCAST;
    this.mcPort     = 27000;
    this.localHost  = InetAddress.getLocalHost().getHostAddress();
    this.svrHost    = NetUtilities.getHost(VRTConfig.TEST_SERVER);
    this.svrPort    = VRTConfig.TEST_FIRST_PORT;
    this.svrPort2   = VRTConfig.TEST_FIRST_PORT + 1;
    this.localPort  = VRTConfig.TEST_FIRST_PORT + 2;
    this.localPort2 = VRTConfig.TEST_FIRST_PORT + 3;
    this.testDelay  = VRTConfig.TEST_DELAY;
  }

  @Override
  public Class<?> getTestedClass () {
    return null;
  }

  @Override
  public String toString () {
    return "Networking Tests";
  }

  @Override
  public List<TestSet> init () throws Exception {
    // ==== INIT TEST CASES ====================================================
    List<TestSet> tests = new ArrayList<TestSet>(8);

    tests.add(new TestSet("VRTReader for TCP Client",     this,  "testVRTReaderTCPClient"));
    tests.add(new TestSet("VRTReader for TCP Server",     this,  "testVRTReaderTCPServer"));
    tests.add(new TestSet("VRTReader for UDP/Multicast",  this,  "testVRTReaderUDPMulticast"));
    tests.add(new TestSet("VRTReader for UDP/Unicast",    this,  "testVRTReaderUDPUnicast"));

    tests.add(new TestSet("VRTWriter for TCP Client",     this,  "testVRTWriterTCPClient"));
    tests.add(new TestSet("VRTWriter for TCP Server",     this,  "testVRTWriterTCPServer"));
    tests.add(new TestSet("VRTWriter for UDP/Multicast",  this,  "testVRTWriterUDPMulticast"));
    tests.add(new TestSet("VRTWriter for UDP/Unicast",    this,  "testVRTWriterUDPUnicast"));

    return tests;
  }

  @Override
  public void done () throws Exception {

  }

  public void testVRTReaderUDPMulticast () throws Exception {
    TransportProtocol transport = TransportProtocol.UDP;
    _testVRTReader(transport, mcHost, mcPort, mcHost, mcPort, 128, 0.01,  32,   false, false);
    _testVRTReader(transport, mcHost, mcPort, mcHost, mcPort, 256, 0.001, 1000, true,  false);
    _testVRTReader(transport, mcHost, mcPort, mcHost, mcPort, 512, 0.001, 100,  true,  true);
  }

  public void testVRTReaderUDPUnicast () throws Exception {
    TransportProtocol transport = TransportProtocol.UDP;
    _testVRTReader(transport, null, localPort, localHost, localPort, 128, 0.01,  32,   false, false);
    _testVRTReader(transport, null, localPort, localHost, localPort, 256, 0.001, 1000, true,  false);
    _testVRTReader(transport, null, localPort, localHost, localPort, 512, 0.001, 100,  true,  true);
  }

  public void testVRTReaderTCPClient () throws Exception {
    TransportProtocol transport = TransportProtocol.TCP_CLIENT;
    _testVRTReader(transport, svrHost, svrPort, null, svrPort, 128, 0.01,  32,   false, false);
    _testVRTReader(transport, svrHost, svrPort, null, svrPort, 256, 0.001, 1000, true,  false);
    _testVRTReader(transport, svrHost, svrPort, null, svrPort, 512, 0.001, 100,  true,  true);
  }

  public void testVRTReaderTCPServer () throws Exception {
    TransportProtocol transport = TransportProtocol.TCP_SERVER;
    _testVRTReader(transport, null, localPort, localHost, localPort, 128, 0.01,  32,   false, false);
    _testVRTReader(transport, null, localPort, localHost, localPort, 256, 0.001, 1000, true,  false);
    _testVRTReader(transport, null, localPort, localHost, localPort, 512, 0.001, 100,  true,  true);
  }

  public void testVRTWriterUDPMulticast () throws Exception {
    TransportProtocol transport = TransportProtocol.UDP;
    _testVRTWriter(transport, mcHost, mcPort, mcHost, mcPort, 128, 0.01,  32,   false, false);
    _testVRTWriter(transport, mcHost, mcPort, mcHost, mcPort, 256, 0.001, 1000, true,  false);
    _testVRTWriter(transport, mcHost, mcPort, mcHost, mcPort, 512, 0.001, 100,  true,  true);
  }

  public void testVRTWriterUDPUnicast () throws Exception {
    TransportProtocol transport = TransportProtocol.UDP;
    _testVRTWriter(transport, svrHost, svrPort, svrHost, svrPort, 128, 0.01,  32,   false, false);
    _testVRTWriter(transport, svrHost, svrPort, svrHost, svrPort, 256, 0.001, 1000, true,  false);
    _testVRTWriter(transport, svrHost, svrPort, svrHost, svrPort, 512, 0.001, 100,  true,  true);
  }

  public void testVRTWriterTCPClient () throws Exception {
    TransportProtocol transport = TransportProtocol.TCP_CLIENT;
    _testVRTWriter(transport, svrHost, svrPort, null, svrPort, 128, 0.01,  32,   false, false);
    _testVRTWriter(transport, svrHost, svrPort, null, svrPort, 256, 0.001, 1000, true,  false);
    _testVRTWriter(transport, svrHost, svrPort, null, svrPort, 512, 0.001, 100,  true,  true);
  }

  public void testVRTWriterTCPServer () throws Exception {
    TransportProtocol transport = TransportProtocol.TCP_SERVER;
    _testVRTWriter(transport, null, localPort, localHost, localPort, 128, 0.01,  32,   false, false);
    _testVRTWriter(transport, null, localPort, localHost, localPort, 256, 0.001, 1000, true,  false);
    _testVRTWriter(transport, null, localPort, localHost, localPort, 512, 0.001, 100,  true,  true);
  }

  public void _testVRTReader (TransportProtocol transport, String host, int port,
                              String svrHost, int svrPort,
                              int elements, double timeDelta, int totDataPkts,
                              boolean vrlFrame, boolean vrlCRC) throws Exception {
    VRTReader in = null;

    try {
      // ==== INITIALIZATION ===================================================
      QueuedListener     listener     = new QueuedListener();
      Map<String,Object> options      = getTestSocketOptions(transport);
      int                ctxInterval  = 10;
      int                totCtxPkts   = (int)Math.ceil(totDataPkts / (double)ctxInterval);
      int                totPkts      = totDataPkts + totCtxPkts;
      Map<String,Object> request      = new LinkedHashMap<String,Object>(16);
      TransportProtocol  svrTransport;
      boolean            connectFirst;

      switch (transport) {
        case TCP_CLIENT: svrTransport = TransportProtocol.TCP_SERVER; connectFirst = false; break;
        case TCP_SERVER: svrTransport = TransportProtocol.TCP_CLIENT; connectFirst = true;  break;
        default:         svrTransport = transport;                    connectFirst = true;  break;
      }

      request.put("TYPE",               "RAMP");
      request.put("PROTOCOL",           svrTransport);
      request.put("HOST",               svrHost);
      request.put("PORT",               svrPort);
      request.put("VRL-FRAME",          vrlFrame);
      request.put("VRL-CRC",            vrlCRC);
      request.put("CONTEXT-INTERVAL",   ctxInterval);
      request.put("DATA-PACKET-COUNT",  totDataPkts);
      request.put("DATA-PACKET-DELTA",  timeDelta);
      request.put("DATA-PAYLOAD-FMT",   "Int8");
      request.put("DATA-ELEMENTS",      elements);

      // ==== CONNECT AND WAIT FOR COMPLETION ==================================
      if (connectFirst) {
        in = new VRTReader(transport, host, port, device, listener, options);
        in.start();
      }

      UUID taskID = sendStartRequest(request, 10000);

      if (in == null) { // same as !connectFirst
        in = new VRTReader(transport, host, port, device, listener, options);
        in.start();
      }

      boolean ok = waitForRequest(taskID, listener, totPkts, 10000, 1000);
      in.close();
//System.out.println("=========================================================");
//System.out.println(listener);
//System.out.println("=========================================================");

      // ==== CHECK RESULTS ====================================================
      assertEquals("Messages sent successfully",          true,    ok);
      assertEquals("Correct number of messages received", totPkts, listener.size());

      TestListener  evt;
      int           count        = 0;
      int           dataPktCount = 0;
      int           ctxPktCount  = 0;
      PayloadFormat pf           = null;

      while ((evt = listener.poll()) != null) {
        String msg = "Event "+count+" ";

        if (count == 0) { // First should be receivedInitialContext(..)
          // On the initial the packet count should be 0 as this should be the
          // first of each type received. Since the data should come after the
          // context, do NOT increment the data counter since that packet should
          // repeat as the next data packet received.
          assertEquals(msg+"type",             Mode.receivedInitialContext, evt.mode);
          assertEquals(msg+"event.source",     in,                          evt.event.getSource());
          assertEquals(msg+"message",          null,                        evt.message);
          assertEquals(msg+"data.StreamID",    100,                         evt.data.getStreamIdentifier());
          assertEquals(msg+"data.PacketCount", dataPktCount % 16,           evt.data.getPacketCount());
          assertEquals(msg+"ctx.StreamID",     100,                         evt.ctx.getStreamIdentifier());
          assertEquals(msg+"ctx.PacketCount",  ctxPktCount % 16,            evt.ctx.getPacketCount());
          assertEquals(msg+"context.size",     1,                           evt.context.size());
          assertEquals(msg+"context[100]",     evt.ctx,                     evt.context.get(100));
          pf = ((ContextPacket)evt.ctx).getDataPayloadFormat();
          ctxPktCount++;
        }
        else if ((count % (ctxInterval+1)) == 0) {
          assertEquals(msg+"type",               Mode.receivedContextPacket, evt.mode);
          assertEquals(msg+"event.source",       in,                         evt.event.getSource());
          assertEquals(msg+"packet.StreamID",    100,                        evt.packet.getStreamIdentifier());
          assertEquals(msg+"packet.PacketCount", ctxPktCount % 16,           evt.packet.getPacketCount());
          ctxPktCount++;
        }
        else {
          assertEquals(msg+"type",               Mode.receivedDataPacket, evt.mode);
          assertEquals(msg+"event.source",       in,                      evt.event.getSource());
          assertEquals(msg+"packet.StreamID",    100,                     evt.packet.getStreamIdentifier());
          assertEquals(msg+"packet.PacketCount", dataPktCount % 16,       evt.packet.getPacketCount());

          byte[] exp = new byte[elements];
          byte[] act = ((DataPacket)evt.packet).getDataByte(pf);

          for (int idx=0,val=(dataPktCount % 16); idx < elements; idx++,val++) {
            exp[idx] = (byte)val;
          }
          assertArrayEquals(msg+"packet.DataByte", exp, act);
          dataPktCount++;
        }
        count++;
      }
    }
    finally {
      if (in != null) in.close();
      Utilities.sleep(testDelay);
    }
  }

  public void _testVRTWriter (TransportProtocol transport, String host, int port,
                              String svrHost, int svrPort,
                              int elements, double timeDelta, int totDataPkts,
                              boolean vrlFrame, boolean vrlCRC) throws Exception {
    VRTReader in  = null;
    VRTWriter out = null;

    try {
      // ==== INITIALIZATION ===================================================
      QueuedListener     outListener  = new QueuedListener();
      QueuedListener     inListener   = new QueuedListener();
      long               timeDeltaPS  = (long)(timeDelta * TimeStamp.ONE_SEC);
      Map<String,Object> outOptions   = getTestSocketOptions(transport);
      Map<String,Object> inOptions    = getTestSocketOptions(TransportProtocol.UDP);
      int                ctxInterval  = 10;
      int                totCtxPkts   = (int)Math.ceil(totDataPkts / (double)ctxInterval);
      int                totPkts      = totDataPkts + totCtxPkts;
      Map<String,Object> request      = new LinkedHashMap<String,Object>(16);
      PayloadFormat      pf           = VRTPacket.INT8;
      byte[]             data         = { 0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7,
                                          0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF };
      TransportProtocol  svrTransport;
      boolean            connectFirst;

      switch (transport) {
        case TCP_CLIENT: svrTransport = TransportProtocol.TCP_SERVER; connectFirst = false; break;
        case TCP_SERVER: svrTransport = TransportProtocol.TCP_CLIENT; connectFirst = true;  break;
        default:         svrTransport = transport;                    connectFirst = false; break;
      }

      request.put("TYPE",               "ECHO");
      request.put("PROTOCOL",           "UDP");
      request.put("HOST",               localHost);
      request.put("PORT",               localPort2);
      request.put("IN-PROTOCOL",        svrTransport);
      request.put("IN-HOST",            svrHost);
      request.put("IN-PORT",            svrPort);

      // ==== CONNECT AND WAIT FOR COMPLETION ==================================
      if (connectFirst) {
        out = new VRTWriter(transport, host, port, device, outListener, outOptions);
        out.start();
      }

      UUID taskID = sendStartRequest(request, 10000);

      if (out == null) { // or !connectFirst
        out = new VRTWriter(transport, host, port, device, outListener, outOptions);
        out.start();
      }
      in = new VRTReader(TransportProtocol.UDP, localHost, localPort2, device, inListener, inOptions);
      in.start();

      TimeStamp ts = TimeStamp.getSystemTime();
      for (int i = 0; i < totDataPkts; i++) {
        ContextPacket ctx = new BasicContextPacket();
        DataPacket    dat = new StandardDataPacket();
        ctx.setStreamIdentifier(100);
        ctx.setTimeStamp(ts);
        ctx.setDataPayloadFormat(pf);
        dat.setStreamIdentifier(100);
        dat.setTimeStamp(ts);
        dat.setPayloadFormat(pf);
        dat.setPayload(data);

        if ((i % ctxInterval) == 0) {
          out.sendPackets(ctx, dat);
        }
        else {
          out.sendPacket(dat);
        }

        ts = ts.addPicoSeconds(timeDeltaPS);
        sleepUntil(ts);
      }
      out.close();

      waitForRequest(taskID, inListener, totPkts, 10000, 1000);
      in.close();

//System.out.println("=========================================================");
//System.out.println(outListener);
//System.out.println("=========================================================");
//System.out.println(inListener);
//System.out.println("=========================================================");

      // ==== CHECK RESULTS ====================================================
      assertEquals("Correct number of messages transmitted", totPkts, outListener.size());
      assertEquals("Correct number of messages received",    totPkts, inListener.size());

      TestListener  outEvt;
      TestListener  inEvt;
      int           count        = 0;
      int           dataPktCount = 0;
      int           ctxPktCount  = 0;

      while ((outEvt = outListener.poll()) != null) {
        inEvt = inListener.poll();
        String msg = "Event "+count+" ";

        if (count == 0) { // First should be receivedInitialContext(..)
          // On the initial the packet count should be 0 as this should be the
          // first of each type received. Since the data should come after the
          // context, do NOT increment the data counter since that packet should
          // repeat as the next data packet received.
          assertEquals(msg+"(OUT) type",               Mode.sentPacket,             outEvt.mode);
          assertEquals(msg+"(OUT) event.source",       out,                         outEvt.event.getSource());
          assertEquals(msg+"(OUT) packet.StreamID",    100,                         outEvt.packet.getStreamIdentifier());
          assertEquals(msg+"(OUT) packet.PacketCount", ctxPktCount % 16,            outEvt.packet.getPacketCount());

          assertEquals(msg+"(IN) type",                Mode.receivedInitialContext, inEvt.mode);
          assertEquals(msg+"(IN) event.source",        in,                          inEvt.event.getSource());
          assertEquals(msg+"(IN) message",             null,                        inEvt.message);
          assertEquals(msg+"(IN) data.StreamID",       100,                         inEvt.data.getStreamIdentifier());
          assertEquals(msg+"(IN) data.PacketCount",    dataPktCount % 16,           inEvt.data.getPacketCount());
          assertEquals(msg+"(IN) ctx.StreamID",        100,                         inEvt.ctx.getStreamIdentifier());
          assertEquals(msg+"(IN) ctx.PacketCount",     ctxPktCount % 16,            inEvt.ctx.getPacketCount());
          assertEquals(msg+"(IN) context.size",        1,                           inEvt.context.size());
          assertEquals(msg+"(IN) context[100]",        inEvt.ctx,                   inEvt.context.get(100));

          assertEquals(msg+"(I/O) input = output",     outEvt.packet,               inEvt.ctx);
          ctxPktCount++;
        }
        else if ((count % (ctxInterval+1)) == 0) {
          assertEquals(msg+"(OUT) type",               Mode.sentPacket,             outEvt.mode);
          assertEquals(msg+"(OUT) event.source",       out,                         outEvt.event.getSource());
          assertEquals(msg+"(OUT) packet.StreamID",    100,                         outEvt.packet.getStreamIdentifier());
          assertEquals(msg+"(OUT) packet.PacketCount", ctxPktCount % 16,            outEvt.packet.getPacketCount());

          assertEquals(msg+"(IN) type",                Mode.receivedContextPacket,  inEvt.mode);
          assertEquals(msg+"(IN) event.source",        in,                          inEvt.event.getSource());
          assertEquals(msg+"(IN) packet.StreamID",     100,                         inEvt.packet.getStreamIdentifier());
          assertEquals(msg+"(IN) packet.PacketCount",  ctxPktCount % 16,            inEvt.packet.getPacketCount());

          assertEquals(msg+"(I/O) input = output",     outEvt.packet,               inEvt.packet);
          ctxPktCount++;
        }
        else {
          assertEquals(msg+"(OUT) type",               Mode.sentPacket,             outEvt.mode);
          assertEquals(msg+"(OUT) event.source",       out,                         outEvt.event.getSource());
          assertEquals(msg+"(OUT) packet.StreamID",    100,                         outEvt.packet.getStreamIdentifier());
          assertEquals(msg+"(OUT) packet.PacketCount", dataPktCount % 16,           outEvt.packet.getPacketCount());

          assertEquals(msg+"(IN) type",                Mode.receivedDataPacket,     inEvt.mode);
          assertEquals(msg+"(IN) event.source",        in,                          inEvt.event.getSource());
          assertEquals(msg+"(IN) packet.StreamID",     100,                         inEvt.packet.getStreamIdentifier());
          assertEquals(msg+"(IN) packet.PacketCount",  dataPktCount % 16,           inEvt.packet.getPacketCount());

          assertEquals(msg+"(I/O) input = output",     outEvt.packet,               inEvt.packet);
          dataPktCount++;
        }
        count++;
      }
    }
    finally {
      if (out != null) out.close();
      if (in  != null) in.close();
      Utilities.sleep(testDelay);
    }
  }

  /** Sends a start request and waits for it to complete.
   *  @param request   The request parameters.
   *  @param wait      The total wait time in ms.
   *  @return The UUID of the task.
   */
  private UUID sendStartRequest (Map<String,?> request, int wait) {
    // ==== SEND START REQUEST =================================================
    Map<?,?> response = sendServerRequest("/start", request);
    Object   status   = response.get("STATUS");
    Object   message  = response.get("MESSAGE");
    Object   task     = response.get("TASK");
    UUID     uuid;

    if (status == null) {
      throw new RuntimeException("Invalid server response null to start request");
    }
    else if (status.equals("OK")) {
      // ok
    }
    else {
      throw new RuntimeException("Failed start request: "+status+" - "+message);
    }

    if (task == null) {
      throw new RuntimeException("Invalid task ID "+task+" from start request");
    }
    try {
      uuid = UUID.fromString(task.toString());
    }
    catch (Exception e) {
      throw new RuntimeException("Invalid task ID "+task+" from start request");
    }

    // ==== INITIAL WAIT FOR STARTUP ===========================================
    sleep(1000);

    // ==== WAIT FOR STARTUP ===================================================
    long               endWait  = System.currentTimeMillis() + wait;
    Map<String,Object> queryReq = new LinkedHashMap<String,Object>(4);
    queryReq.put("TASK", uuid.toString());

    while (System.currentTimeMillis() < endWait) {
      Map<?,?> queryResp     = sendServerRequest("/query", queryReq);
      Object   queryStatus   = queryResp.get("STATUS");
      Object   queryMessage  = queryResp.get("MESSAGE");
      Object   queryResult   = queryResp.get("RESULT");

      if (queryStatus.equals("OK")) {
        if (queryResult == null) {
          throw new RuntimeException("Invalid server response to query request RESULT=null");
        }
        else if (queryResult.equals("PENDING")) {
          sleep(1000); // keep waiting (time in ms)
        }
        else if (queryResult.equals("RUNNING")) {
          return uuid; // expected status
        }
        else if (queryResult.equals("COMPLETED")) {
          return uuid; // rare case where task (e.g. "RAMP") completes before getting here
        }
        else {
          throw new RuntimeException("Invalid server response to query request RESULT="+queryResult);
        }
      }
      else {
        throw new RuntimeException("Failed query request: "+queryStatus+" - "+queryMessage);
      }
    }
    throw new RuntimeException("Server task failed to start within given wait time");
  }

  /** Sends a start request and waits for it to complete.
   *  @param uuid      The UUID of the task.
   *  @param listener  The listener being used.
   *  @param count     The number of expected messages.
   *  @param wait      The total wait time in ms.
   *  @param waitStop  Extra time to wait following last expected packet before
   *                   sending a stop request.
   */
  private boolean waitForRequest (UUID uuid, QueuedListener listener,
                                  int count, int wait, int waitStop) {

    // ==== WAIT FOR TASK TO COMPLETE ==========================================
    long               endWait  = System.currentTimeMillis() + wait;
    Map<String,Object> queryReq = new LinkedHashMap<String,Object>(4);
    queryReq.put("TASK", uuid.toString());

    while (System.currentTimeMillis() < endWait) {
      Map<?,?> queryResp     = sendServerRequest("/query", queryReq);
      Object   queryStatus   = queryResp.get("STATUS");
      Object   queryMessage  = queryResp.get("MESSAGE");
      Object   queryResult   = queryResp.get("RESULT");

      if (queryStatus == null) {
        throw new RuntimeException("Invalid server response "+queryStatus+" to query request");
      }
      if (queryStatus.equals("OK")) {
        if (queryResult == null) {
          throw new RuntimeException("Invalid server response to query request RESULT=null");
        }
        else if (queryResult.equals("RUNNING")) {
          if (listener.size() >= count) {
            sleep(500); // wait for completion
            waitStop -= 500;
            if (waitStop <= 0) break;
          }
          else {
            sleep(1000); // keep waiting (time in ms)
          }
        }
        else if (queryResult.equals("COMPLETED")) {
          return true;
        }
        else if (queryResult.equals("FAILED") || queryResult.equals("ABORTED")) {
          return false;
        }
        else if (queryResult.equals("PENDING")) {
          sleep(1000); // wait for task to start
        }
        else {
          throw new RuntimeException("Invalid server response to query request RESULT="+queryResult);
        }
      }
      else if (queryStatus.equals("NOT-FOUND")) {
        return true; // Assume complete and dropped from server listing
      }
      else {
        throw new RuntimeException("Failed query request: "+queryStatus+" - "+queryMessage);
      }
    }

    // ==== TELL SERVER TO STOP ================================================
    sendServerRequest("/stop", queryReq);
    return true;
  }


  //////////////////////////////////////////////////////////////////////////////
  // NESTED CLASSES
  //////////////////////////////////////////////////////////////////////////////
  /** An implementation of {@link VRTContextListener} that stores the
   *  results of the previous calls.
   */
  private final class QueuedListener implements VRTContextListener {
    private final LinkedList<TestListener> queue = new LinkedList<TestListener>();

    /** Gets the number of messages in the queue. */
    public synchronized int size () {
      return queue.size();
    }

    /** Polls the received messages. */
    public synchronized TestListener poll () {
      return queue.poll();
    }

    /** Peeks at the received messages. */
    public synchronized TestListener peek () {
      return queue.peek();
    }

    @Override
    public synchronized String toString () {
      Formatter f = new Formatter();
      f.format("QueuedListener with %d messages%n", queue.size());
      int i = 0;
      for (TestListener l : queue) {
        f.format("%4d: %s%n", i++, l);
      }
      return f.toString();
    }

    @Override
    public synchronized void receivedPacket (VRTEvent e, VRTPacket p) {
      TestListener l = new TestListener();
      l.receivedPacket(e, p.copy());
      queue.add(l);
    }

    @Override
    public synchronized void sentPacket (VRTEvent e, VRTPacket p) {
      TestListener l = new TestListener();
      l.sentPacket(e, p.copy());
      queue.add(l);
    }

    @Override
    public synchronized void errorOccurred (VRTEvent e, String msg, Throwable t) {
      TestListener l = new TestListener();
      l.errorOccurred(e, msg, t);
      queue.add(l);
    }

    @Override
    public synchronized void warningOccurred (VRTEvent e, String msg, Throwable t) {
      TestListener l = new TestListener();
      l.warningOccurred(e, msg, t);
      queue.add(l);
    }

    @Override
    public synchronized void receivedDataPacket (VRTEvent e, DataPacket p) {
      TestListener l = new TestListener();
      l.receivedDataPacket(e, p.copy());
      queue.add(l);
    }

    @Override
    public synchronized void receivedContextPacket (VRTEvent e, VRTPacket p) {
      TestListener l = new TestListener();
      l.receivedContextPacket(e, p);
      queue.add(l);
    }

    @Override
    public synchronized void receivedInitialContext (VRTEvent e, String msg, DataPacket d,
                                                     VRTPacket c, Map<Integer,VRTPacket> m) {
      TestListener l = new TestListener();
      Map<Integer,VRTPacket> _m = new LinkedHashMap<Integer,VRTPacket>(m.size());
      for (Map.Entry<Integer,VRTPacket> entry : m.entrySet()) {
        _m.put(entry.getKey(), entry.getValue().copy());
      }
      l.receivedInitialContext(e, msg, d.copy(), c.copy(), _m);
      queue.add(l);
    }
  }
}
