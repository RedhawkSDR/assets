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

import java.util.ArrayList;
import java.util.Collections;
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
import nxm.vrt.net.Threadable;
import nxm.vrt.net.VRTEvent;
import nxm.vrt.net.VRTEventListener;
import nxm.vrt.net.VRTReader;
import nxm.vrt.net.VRTWriter;

import static nxm.vrttest.inc.TestRunner.getTestSocketOptions;

/** A handler for one of the requests made to {@link NetTestServer}. */
public abstract class NetTestHandler extends Threadable implements VRTEventListener {
  protected final NetTestServer      server;          // The server
  protected final UUID               taskID;          // The UUID of the task
  protected final List<String>       errors;          // Any errors caught
  protected final List<String>       warnings;        // Any warnings caught
  protected final TransportProtocol  transport;       // The transport protocol to use
  protected final String             host;            // The host address to connect to
  protected final int                port;            // The host port to connect to
  protected final String             device;          // The local device to use
  protected final Map<String,Object> socketOptions;   // The socket options to use

  /** Creates a new instance.
   *  @param server The server this instance is associated with (not null).
   *  @param params The parameters to use.
   *  @param output The output content for the request.
   */
  @SuppressWarnings("LeakingThisInConstructor")
  protected NetTestHandler (NetTestServer server, Map<String,String> params,
                                                  Map<String,Object> output) {
    this.server   = server;
    this.taskID   = UUID.randomUUID();
    this.errors   = new ArrayList<String>();
    this.warnings = new ArrayList<String>();

    if (params == null) {
      this.transport       = null;
      this.host            = null;
      this.port            = 0;
      this.device          = null;
      this.socketOptions   = null;
    }
    else {
      String dev = params.get("IN-DEVICE");
      if (dev == null) dev = VRTConfig.TEST_DEVICE;

      this.transport       = getParamEnum(params, "PROTOCOL", TransportProtocol.class);
      this.host            = params.get("HOST");
      this.port            = getParamInt(params, "PORT", 0, 65535);
      this.device          = dev;
      this.socketOptions   = getTestSocketOptions(transport);
    }
    super.setUncaughtExceptionHandler(this);
  }

  /** Gets the task ID. */
  public final UUID getTaskID () {
    return taskID;
  }

  @Override
  public void start () {
    server.activeTasks.put(taskID, this);
    server.recentTasks.put(taskID, this);
    super.start();
  }

  @Override
  protected void shutdown () {
    server.activeTasks.remove(taskID);
  }

  @Override public void receivedPacket (VRTEvent e, VRTPacket p) { } // ignore
  @Override public void sentPacket (VRTEvent e, VRTPacket p) { } // ignore

  @Override
  public void errorOccurred (VRTEvent e, String msg, Throwable t) {
    if (msg == null) {
      msg = (t != null)? t.toString() : "Unknown error from "+e;
    }
    errors.add(msg);
    server.errorOccurred(e, msg, t);
  }

  @Override
  public void warningOccurred (VRTEvent e, String msg, Throwable t) {
    if (msg == null) {
      msg = (t != null)? t.toString() : "Unknown warning from "+e;
    }
    warnings.add(msg);
    server.warningOccurred(e, msg, t);
  }

  /** Gets the list of errors caught. */
  public List<String> getErrors () {
    return Collections.unmodifiableList(errors);
  }

  /** Gets the list of warnings caught. */
  public List<String> getWarnings () {
    return Collections.unmodifiableList(warnings);
  }


  //////////////////////////////////////////////////////////////////////////////
  // Support parameter gets
  //////////////////////////////////////////////////////////////////////////////
  /** Gets one of the required parameters as a string. */
  static String getParamStr (Map<String,String> params, String key) {
    String val = params.get(key);
    if ((val == null) || val.isEmpty()) {
      throw new RuntimeException("Missing "+key+" parameter with start request");
    }
    return val;
  }

  /** Gets one of the required parameters as a double. */
  static double getParamNum (Map<String,String> params, String key) {
    String val = getParamStr(params, key);
    try {
      return Double.parseDouble(val);
    }
    catch (NumberFormatException e) {
      throw new RuntimeException("Invalid "+key+" parameter, expected number but got '"+val+"'");
    }
  }

  /** Gets one of the required parameters as a double (min/max are inclusive). */
  static double getParamNum (Map<String,String> params, String key, double min, double max) {
    double val = getParamNum(params, key);
    if ((val < min) || (val > max)) {
      throw new RuntimeException("Invalid "+key+" parameter, expected number in "
                                +"range ["+min+","+max+"] but got "+val+"");
    }
    return val;
  }

  /** Gets one of the required parameters as an int. */
  static int getParamInt (Map<String,String> params, String key) {
    return (int)getParamNum(params, key);
  }

  /** Gets one of the required parameters as an int (min/max are inclusive). */
  static int getParamInt (Map<String,String> params, String key, int min, int max) {
    return (int)getParamNum(params, key, min, max);
  }

  /** Gets one of the required parameters as a string. */
  static boolean getParamBool (Map<String,String> params, String key) {
    String val = getParamStr(params, key);
    try {
      return Utilities.toBooleanValue(val);
    }
    catch (Exception e) {
      throw new RuntimeException("Invalid "+key+" parameter: "+e.getMessage());
    }
  }

  /** Gets one of the required parameters as a string. */
  static <T extends Enum<T>> T getParamEnum (Map<String,String> params, String key, Class<T> type) {
    String val = getParamStr(params, key);
    try {
      return Utilities.toEnum(type, val);
    }
    catch (Exception e) {
      throw new RuntimeException("Invalid "+key+" parameter: "+e.getMessage());
    }
  }

  /** Gets one of the required parameters as a PayloadFormat. */
  static PayloadFormat getParamFmt (Map<String,String> params, String key, boolean bitNibble) {
    String val = getParamStr(params, key);
    String pf  = val.toUpperCase();

    if (pf.equals("INT8"    )) return VRTPacket.INT8;
    if (pf.equals("INT16"   )) return VRTPacket.INT16;
    if (pf.equals("INT32"   )) return VRTPacket.INT32;
    if (pf.equals("INT64"   )) return VRTPacket.INT64;
    if (pf.equals("UINT8"   )) return VRTPacket.UINT8;
    if (pf.equals("UINT16"  )) return VRTPacket.UINT16;
    if (pf.equals("UINT32"  )) return VRTPacket.UINT32;
    if (pf.equals("UINT64"  )) return VRTPacket.UINT64;
    if (pf.equals("FLOAT32" )) return VRTPacket.FLOAT32;
    if (pf.equals("DOUBLE64")) return VRTPacket.DOUBLE64;


    if (bitNibble) {
      // Only permit sub-Int8 values if requested as they are more difficult to deal with
      if (pf.equals("INT4"  )) return VRTPacket.INT4;
      if (pf.equals("UINT1" )) return VRTPacket.UINT1;
      if (pf.equals("UINT4" )) return VRTPacket.UINT4;
    }
    else if (pf.equals("INT4") || pf.equals("UINT1") || pf.equals("UINT4")) {
      throw new RuntimeException("Invalid "+key+" parameter, expected 8-bit payload "
                                +"format or larger but got '"+val+"'");
    }

    throw new RuntimeException("Invalid "+key+" parameter, expected payload format "
                              +"(e.g. Int8,Int16,Float32,etc.) but got '"+val+"'");
  }

  /** A handler for a request to send a "ramp" of data.
   *  <pre>
   *    Configuration Parameters
   *    ========================================================================
   *    PROTOCOL           - Protocol to use (see {@link TransportProtocol})
   *    HOST               - The host address to connect to (ignored for
   *                         PROTOCOL=TCP and PROTOCOL=TCP_SERVER, but required
   *                         for all others)
   *    PORT               - The host port to connect to
   *    DEVICE             - The network device to use (optional)
   *    VRL-FRAME          - Use VRL frames (T/F)?
   *    VRL-CRC            - Use CRC with VRL frames (T/F)? (optional if VRL-FRAME=F)
   *    CONTEXT-INTERVAL   - The context packet interval in number of data packets
   *                         (e.g. 10 = send 1 context packet for every 10 data
   *                         packets)
   *    DATA-PACKET-COUNT  - Total number of data packets to send
   *    DATA-PACKET-DELTA  - Duration between data packets sent (in seconds) note
   *                         that this will directly drive the output time stamps
   *                         (rounded to the nearest picosecond) but the actual
   *                         rate may varry, particularly if a very small interval
   *                         is selected
   *    DATA-PAYLOAD-FMT   - Data packet payload format (e.g. "Int8","Int16",etc.)
   *    DATA-ELEMENTS      - Number of elements per data packet
   *
   *    Output Data
   *    ========================================================================
   *    Data Stream:
   *      This will output the given number of StandardDataPacket instances.
   *      The data for the packet will be the values N to 'DATA-ELEMENTS'+N
   *      where N is equal to the value of the 4-bit packet count (i.e. 0 to
   *      15). The data packets will use StreamID=100 and the trailer bits
   *      will be set as follows:
   *        CalibratedTimeStamp   - false
   *        DataValid             - true
   *        ReferenceLocked       - false
   *        AutomaticGainControl  - false
   *        SignalDetected        - true
   *        InvertedSpectrum      - false
   *
   *    Paired Context Stream:
   *      This will output a BasicContextPacket at the given interval with
   *      StreamID=100 and the following context fields set:
   *        ChangePacket          - true (first packet sent) or false (all others)
   *        DataPayloadFormat     - Set to match data (see DATA-PAYLOAD-FMT)
   *        ReferencePoint        - 100
   *        SampleRate            - Sample rate (set via SamplePeriod)
   *        SamplePeriod          - Sample period computed using:
   *                                   SamplePeriod = DATA-PACKET-DELTA / DATA-PACKET-COUNT
   *        CalibratedTimeStamp   - false
   *        DataValid             - true
   *        ReferenceLocked       - false
   *        AutomaticGainControl  - false
   *        SignalDetected        - true
   *        InvertedSpectrum      - false
   *  </pre>
   */
  static class NetTestRamp extends NetTestHandler {
    private final DataPacket[]       dataPackets;     // The data packets to send
    private final ContextPacket      contextPacket;   // The context packet to send
    private final int                contextInterval; // The context packet interval (packets)
    private final long               dataInterval;    // The data packet interval (picoseconds)
    private final int                packetMax;       // The max packet count


    /** Creates a new instance.
     *  @param server The server this instance is associated with (not null).
     *  @param params The parameters to use. (not null).
     *  @param output The output content for the request (not null).
     */
    protected NetTestRamp (NetTestServer server, Map<String,String> params,
                                                 Map<String,Object> output) {
      super(server, params, output);
      socketOptions.put("VRL_FRAME", getParamBool(params, "VRL-FRAME"));
      socketOptions.put("VRL_CRC",   getParamBool(params, "VRL-CRC"));

      double            dataInt  = getParamNum( params, "DATA-PACKET-DELTA", 1e-12, 60);
      PayloadFormat     pf       = getParamFmt( params, "DATA-PAYLOAD-FMT",  true);
      int               elem     = getParamInt( params, "DATA-ELEMENTS",     0, 1048575);

      this.dataPackets     = new DataPacket[16];
      this.contextPacket   = new BasicContextPacket();
      this.contextInterval = getParamInt(params, "CONTEXT-INTERVAL",  1, 1024);
      this.packetMax       = getParamInt(params, "DATA-PACKET-COUNT", 0, 1048576);
      this.dataInterval    = (long)(dataInt * TimeStamp.ONE_SEC);

      // ==== INIT CONTEXT PACKET ==============================================
      contextPacket.setStreamIdentifier(100);
      contextPacket.setClassID("FF-FF-FA:2011.0003");
      contextPacket.setTimeStamp(TimeStamp.Y2K_GPS); // dummy value to reserve space

      contextPacket.setDataPayloadFormat(pf);
      contextPacket.setReferencePointIdentifier(100);
      contextPacket.setSamplePeriod(dataInt / elem);

      contextPacket.setCalibratedTimeStamp(false);
      contextPacket.setDataValid(true);
      contextPacket.setReferenceLocked(false);
      contextPacket.setAutomaticGainControl(false);
      contextPacket.setSignalDetected(true);
      contextPacket.setInvertedSpectrum(false);

      // ==== INIT DATA PACKETS ================================================
      // Also set the data in the packets since we will simply be repeating them
      // over and over again rather than recreating them.
      for (int i = 0; i < 16; i++) {
        StandardDataPacket p = new StandardDataPacket();
        p.setStreamIdentifier(100);
        p.setPayloadFormat(pf);
        p.setTimeStamp(TimeStamp.Y2K_GPS); // dummy value to reserve space

        p.setCalibratedTimeStamp(false);
        p.setDataValid(true);
        p.setReferenceLocked(false);
        p.setAutomaticGainControl(false);
        p.setSignalDetected(true);
        p.setInvertedSpectrum(false);

        int[] values = new int[elem];
        for (int j = 0; j < elem; j++) {
          values[j] = i + j;
        }
        p.setDataInt(values);

        dataPackets[i] = p;
      }
    }

    @Override
    protected void runThread () throws Exception {
      VRTWriter out       = null;
      TimeStamp ts        = TimeStamp.getSystemTime();
      long      firstTime = System.currentTimeMillis();  // send time for first packet


      try {
        out = new VRTWriter(transport, host, port, device, this, socketOptions);
        out.start();
        long endWait = System.currentTimeMillis() + 300000; // 5 minutes!!!
        while ((out.getActiveConnectionCount() == 0) && !stopNow()) {
          long now = System.currentTimeMillis();

          if (now > endWait) {
            throw new RuntimeException("No connection made after 5 minutes, aborting");
          }
          sleepUntil(now+5000);
        }

        for (int packetNum = 0; (packetNum < packetMax) && !stopNow(); packetNum++) {
          long sendTime = firstTime + ((dataInterval * packetNum) / 1000000000L);

          sleepUntil(sendTime);
          if (stopNow()) break;

          DataPacket data = dataPackets[packetNum % 16];
          data.resetForResend(ts);

          if ((packetNum % contextInterval) == 0) {
            contextPacket.resetForResend(ts);
            contextPacket.setChangePacket(packetNum == 0);
//System.out.println(">>> sending context = "+contextPacket);
//System.out.println(">>>       + data    = "+data);
            out.sendPackets(contextPacket, data);
          }
          else {
//System.out.println(">>> sending data    = "+data);
            out.sendPacket(data);
          }
          ts = ts.addPicoSeconds(dataInterval);
        }
      }
      finally {
//System.out.println(">>> stopping ramp");
        if (out != null) out.close();
      }
    }
  }

  /** A handler for a request to echo back any packets received.
   *  <pre>
   *    Configuration Parameters for Input to Server
   *    ========================================================================
   *    IN-PROTOCOL         - Protocol to use (see {@link TransportProtocol})
   *    IN-HOST             - The host address to connect to (ignored for
   *                          PROTOCOL=TCP and PROTOCOL=TCP_SERVER, but required
   *                          for all others)
   *    IN-PORT             - The host port to connect to
   *    IN-DEVICE           - The network device to use (optional)
   *
   *    Configuration Parameters for Output from Server
   *    ========================================================================
   *    PROTOCOL           - Protocol to use (see {@link TransportProtocol})
   *    HOST               - The host address to connect to (ignored for
   *                         PROTOCOL=TCP and PROTOCOL=TCP_SERVER, but required
   *                         for all others)
   *    PORT               - The host port to connect to
   *    DEVICE             - The network device to use (optional)
   *
   *    <i>The output parameters do NOT have an "OUT-" prefix since they match
   *    the other data-generation tasks like {@link NetTestRamp}.</i>
   *  </pre>
   */
  static class NetTestEcho extends NetTestHandler {
    private final TransportProtocol  inTransport;       // The transport protocol to use
    private final String             inHost;            // The host address to connect to
    private final int                inPort;            // The host port to connect to
    private final String             inDevice;          // The local device to use
    private final Map<String,Object> inSocketOptions;   // The socket options to use

    /** Creates a new instance.
     *  @param server The server this instance is associated with (not null).
     *  @param params The parameters to use. (not null).
     *  @param output The output content (not null).
     */
    protected NetTestEcho (NetTestServer server, Map<String,String> params,
                                                 Map<String,Object> output) {
      super(server, params, output);
      String dev = params.get("IN-DEVICE");
      if (dev == null) dev = VRTConfig.TEST_DEVICE;

      this.inTransport       = getParamEnum(params, "IN-PROTOCOL", TransportProtocol.class);
      this.inHost            = params.get("IN-HOST");
      this.inPort            = getParamInt( params, "IN-PORT", 0, 65535);
      this.inDevice          = dev;
      this.inSocketOptions   = NetUtilities.getDefaultSocketOptions(true, inTransport);
    }

    @Override
    protected void runThread () throws Exception {
      VRTReader in  = null;
      VRTWriter out = null;

      try {
        in  = new VRTReader(inTransport, inHost, inPort, inDevice, this, inSocketOptions);
        out = new VRTWriter(  transport,   host,   port,   device, this,   socketOptions);

        in.enableTestModeEcho(out);

        out.start();
        long endWait = System.currentTimeMillis() + 300000; // 5 minutes!!!
        while ((out.getActiveConnectionCount() == 0) && !stopNow()) {
          long now = System.currentTimeMillis();

          if (now > endWait) {
            throw new RuntimeException("No connection made after 5 minutes, aborting");
          }
          sleepUntil(now+5000);
        }

        in.start();

        sleep(3600000); // wait an hour, or until stopped
      }
      finally {
        if (in  != null) in.close();
        if (out != null) out.close();
      }
    }
  }
}
