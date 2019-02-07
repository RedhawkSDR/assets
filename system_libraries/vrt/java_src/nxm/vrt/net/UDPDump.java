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
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import nxm.vrt.lib.BasicVRAFile;
import nxm.vrt.lib.DataPacket;
import nxm.vrt.lib.Main;
import nxm.vrt.lib.VRAFile;
import nxm.vrt.lib.VRTConfig;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.net.NetUtilities.TransportProtocol;

import static nxm.vrt.lib.Utilities.sleep;
import static nxm.vrt.lib.Utilities.toHexDump;
import static nxm.vrt.net.NetUtilities.MAX_UDP_LEN;

/** <b>Command-Line Use Only:</b> A basic tool for debugging UDP/Multicast
 *  streams that also supports UDP/Unicast and TCP. <br>
 *  <br>
 *  Although it may seem a bit of a paradox to include TCP support within a
 *  command called 'udpdump' it nicely mirrors the Linux 'tcpdump' which
 *  optionally supports UDP. The big difference between the Linux command and
 *  this one is that the Linux command requires superuser (i.e. root) access
 *  in order to run (since it has the ability to interact with lower-level
 *  protocols) whereas this command simply requires user-level access.
 */
public class UDPDump extends Main {
  /** Supported translation formats. */
  private static enum Format { RAW, VITA49, VITA49_NO_DATA, VITA49_ALL };

  private static final String[] OPTIONS = { "-A", "-C", "-T=", "-i=", "-n=", "-p=",
                                            "-t", "-v", "-w=", "-x" };
  private static final String[] VERSION = { "UDPDump "+VRTConfig.LIBRARY_VERSION };
  private static final String[] USAGE   = {
      "Usage: udpdump [OPTION]... <host:port>",
      " or:   java -classpath <classpath> nxm.vrt.net.UDPDump [OPTION]... <host:port>",
      "",
      "",
      "-A                 Print each packet in ASCII",                            // from tcpdump
      "-C                 Print each packet in hex and ASCII",                    // from hexdump
      "-H                 Prints usage information and exits",
      "-i interface       Use the specified device interface",                    // from tcpdump
      "-n limit           Exit after receiving the specified number of packets",  // from tail, etc.
      "-p port            Port number (allows entry of just host in <host:port>)",// from ssh
      "-T format          Translate packets before output to using one of the following conversions:",
      "                       raw            - Raw packet data (default)",
      "                       vita49         - VITA-49.0 packets or VITA-49.1 frames in 'processing' form",
      "                                        waiting for initial context and then displaying context",
      "                                        and data packets as received (for the purposes of -n, the",
      "                                        initial context, which may be multiple packets, counts as",
      "                                        one packet)",
      "                       vita49-no-data - Same as 'vita49' but omits any data packets other than the",
      "                                        one that is associated with the initial context (for the",
      "                                        purposes of -n, any data packets received are not counted)",
      "                                        (note that this does not prevent inclusion of data packets",
      "                                        in the 'raw' or VRA output files)",
      "                       vita49-all     - VITA-49.0 packets or VITA-49.1 frames in 'all' form",
      "                                        where evey packet is displayed as received without any",
      "                                        waiting for initial context",
      "-t transport       Specifies the transport protocol to use:",
      "                       UDP            - UDP/Unicast *or* UDP/Multicast (default)",
      "                       TCP            - TCP using normal socket connection (same as TCP_CLIENT)",
      "                       TCP-CLIENT     - TCP using client socket connection",
      "                       TCP-SERVER     - TCP using server socket connection",
      "-V                 Prints version information and exits",
      "-v                 Output verbose output (automatically on if -A, -C, or -x are specified)",
      "-w file            Save a copy of the data to a file. The format of the output file is determined",
      "                   based on the file name extension given:",               // from tcpdump
      "                       .bin           - Raw octets as received",
      "                       .cap           - Not supported yet (reserved for PCAP output)",
      "                       .dmp           - Not supported yet (reserved for PCAP output)",
      "                       .out           - Raw octets as received",
      "                       .pcap          - Not supported yet (reserved for PCAP output)",
      "                       .txt           - Text output (combine with -A or -C or -x)",
      "                       .vra           - VITA-49 VRA archive format (this usage is NOT compatible",
      "                                        '-T raw')",
      "-x                 Print each packet in hex",                              // from tcpdump, hexdump
      "",
      "(--help, --verbose, and --version are aliases for -H, -v, and -V)",
      "",
      "Note:",
      "  Although it may seem a bit of a paradox to include TCP support within a command called 'udpdump'",
      "  it nicely mirrors the Linux 'tcpdump' which optionally supports UDP. The big difference between",
      "  the Linux command and this one is that the Linux command requires superuser (i.e. root) access",
      "  in order to run (since it has the ability to interact with lower-level protocols) whereas this",
      "  command simply requires user-level access.",
      "",
      "Examples:",
      "  UDP/Multicast:          udpdump -n 4 -C        237.80.1.1:27000",
      "  UDP/Unicast:            udpdump -n 4 -C        :27000",
      "  TCP (normal):           udpdump -n 4 -C -t TCP servername:9999",
      "",
      "  Format V49 Packets:     udpdump -n 100 -T vita49               237.80.1.1:27000",
      "  Write V49 Pkts to File: udpdump -n 100 -T vita49 -w myfile.vra 237.80.1.1:27000",
  };

  private final    TransportProtocol transport;       // Transport protocol to use
  private final    String            device;          // Network device to use
  private final    String            host;            // Host to connect to
  private final    int               port;            // Port to connect to
  private final    Map<String,?>     socketOptions;   // Any socket options to use
  private final    Format            format;          // Translation format
  private final    long              limit;           // Number of packets to collect (-1=unlimited)
  private volatile long              count;           // Number of packets received
  private final    boolean           ascii;           // Show ASCII output?
  private final    boolean           hex;             // Show hex output?
  private final    boolean           verbose;         // Show verbose output?
  private final    PrintStream       rawOut;          // Output stream for raw output
  private final    PrintStream       txtOut;          // Output stream for txt output
  private final    VRAFile           vraOut;          // Output stream for VRA output

  /** <b>Internal Use Only:</b> Creates a new instance.
   *  @param args The command-line arguments.
   *  @throws IOException If there is an error while opening any files/connections.
   */
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public UDPDump (String[] args) throws IOException {
    super(VERSION, USAGE, OPTIONS, 1, 1, args);
    String  hostPort = (isHelpVersion())? "127.0.0.1:0" : parameters.get(0);
    String  _host    = NetUtilities.getHost(hostPort);
    Integer _port    = NetUtilities.getPort(hostPort);

    if (isOptionSet("-p")) {
      if (_port != null) {
        System.err.println("WARNING: Given port in "+hostPort+" overridden by -p option");
      }
      _port = getNumericOption("-p", 0);
    }
    if (_port == null) {
      throw new Main.InvalidCommandLineException("No port number specified");
    }

    boolean forceVerbose = isOptionSet("-v") || isOptionSet("--verbose");
    String  fname        = getOption("-w", "");

    if (fname.isEmpty()) {
      this.rawOut = null;
      this.txtOut = null;
      this.vraOut = null;
      forceVerbose = true;
    }
    else if (fname.endsWith(".cap") || fname.endsWith(".dmp") || fname.endsWith(".pcap")) {
      throw new Main.InvalidCommandLineException("PCAP output is not supported at this time, "
                                                +"given '"+fname+"'.");
    }
    else if (fname.endsWith(".txt")) {
      this.rawOut = null;
      this.txtOut = new PrintStream(fname);
      this.vraOut = null;
    }
    else if (!fname.endsWith(".bin") && !fname.endsWith(".out")) {
      this.rawOut = new PrintStream(fname);
      this.txtOut = null;
      this.vraOut = null;
    }
    else if (fname.endsWith(".vra")) {
      this.rawOut = null;
      this.txtOut = null;
      this.vraOut = new BasicVRAFile(fname, BasicVRAFile.FileMode.Write);
    }
    else {
      // For backwards-compatibility just issue a warning for now. In future
      // releases this may change to an error.
      System.err.println("WARNING: Unknown file format associated with '"+fname+"', "
                        +"assuming raw binary output.");
      this.rawOut = new PrintStream(fname);
      this.txtOut = null;
      this.vraOut = null;
    }

    this.transport     = getEnumOption("-t", TransportProtocol.UDP);
    this.device        = getOption("-i");
    this.host          = _host;
    this.port          = _port;
    this.format        = getEnumOption("-T", Format.RAW);
    this.socketOptions = new LinkedHashMap<String,Object>(8);
    this.limit         = getNumericOption("-n", -1L);
    this.ascii         = isOptionSet("-A") || isOptionSet("-C");
    this.hex           = isOptionSet("-x") || isOptionSet("-C");
    this.verbose       = ascii || hex || forceVerbose;
  }

  /** Main method for the class.
   *  @param args The command line arguments.
   */
  public static void main (String ... args) {
    runMain(UDPDump.class, args);
  }

  @Override
  @SuppressWarnings("deprecation")
  protected void main () throws Exception {
    if (isHelpVersion()) {
      super.main();
      return;
    }

    if (format == Format.RAW) {
      MulticastReader pktReader = new PacketReader(host, port, device);
      pktReader.start();
      waitUntilComplete();
      pktReader.close();
    }
    else {
      VRTEventListener listener;
      switch (format) {
        case VITA49:         listener = new V49Listener(true ); break;
        case VITA49_NO_DATA: listener = new V49Listener(false); break;
        case VITA49_ALL:     listener = new RawV49Listener();   break;
        default:             throw new AssertionError("Unsupported format "+format);
      }
      VRTReader vrtReader = new VRTReader(transport, host, port, device, listener, socketOptions);
      vrtReader.start();
      waitUntilComplete();
      vrtReader.close();
    }

    if (rawOut != null) rawOut.close();
    if (vraOut != null) vraOut.close();
  }

  /** Waits until the the capture is complete. */
  private void waitUntilComplete () {
    while ((limit == -1) || (count < limit)) {
      sleep(100);
    }
  }

  /** Prints out a line of text, also prints to output file. */
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  protected void printTxt (CharSequence str) {
    if (verbose) {
      System.out.println(str);
    }
    if (txtOut != null) {
      txtOut.println(str);
    }
  }


  /** Prints out an error message, also prints to output file. */
  @SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToThreadDumpStack"})
  protected void printErr (CharSequence str, Throwable t) {
    System.err.println(str);
    if (txtOut != null) {
      txtOut.println(str);
    }
    if (t != null) {
      t.printStackTrace();
      if (txtOut != null) {
        t.printStackTrace(txtOut);
      }
    }
  }

  /** Prints out a received buffer of data, also prints to output file. */
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  protected void printBuf (byte[] buf, int off, int len) {
    if (len == -1) len = buf.length;

    if (hex || ascii) {
      CharSequence hexDump = toHexDump(buf, 0, len, hex, ascii);

      System.out.println(hexDump);
      if (txtOut != null) {
        txtOut.println(hexDump);
      }
    }
    if (rawOut != null) {
      rawOut.write(buf, off, len);
    }
  }

  /** Prints out a received VRT packet, also prints to output file. */
  @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
  protected void printPkt (String type, VRTPacket p, boolean show) {
    if (show) {
      if (verbose) {
        printTxt("Received "+type+": "+p);
      }
      if (hex || ascii) {
        CharSequence hexDump = toHexDump(p.getPacket(), 0, p.getPacketLength(), hex, ascii);

        System.out.println(hexDump);
        if (txtOut != null) {
          txtOut.println(hexDump);
        }
      }
    }
    if (vraOut != null) {
      vraOut.append(p);
    }
    if (rawOut != null) {
      try {
        p.writePacketTo(rawOut);
      }
      catch (IOException e) {
        printErr("ERROR: Unable to write out packet", e);
      }
    }
  }

  /** The basic Multicast reader. */
  private final class PacketReader extends MulticastReader {
    /** Creates a new instance. */
    PacketReader (String host, int port, String device) throws IOException {
      super(host, port, device, null, null, MAX_UDP_LEN);
    }

    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    protected void handle (byte[] buffer, int length) throws IOException {
      if (count >= limit) return; // ignore any received during shutdown

      printTxt("Got a packet of length "+length);
      printBuf(buffer, 0, length);

      count++;
    }
  }

  /** Basic VITA-49 listener that does not implement VRTContextListener and prints to screen. */
  private class RawV49Listener implements VRTEventListener {
    private final boolean showData;

    RawV49Listener () {
      this(true);
    }

    protected RawV49Listener (boolean showData) {
      this.showData = showData;
    }

    /** Handler for a received packet. */
    private void receivedPkt (String type, VRTEvent e, VRTPacket p, boolean show) {
      if (count >= limit) return; // ignore any received during shutdown
      printPkt(type, p, show);
      if (show) count++;
    }

    /** Handler for a received error/warning. */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private void receivedErr (String type, VRTEvent e, String msg, Throwable t) {
      if (count >= limit) return; // ignore any received during shutdown
      printErr(type+" Condition Detected: "+msg, t);
      printTxt("");
    }

    @Override public synchronized void errorOccurred         (VRTEvent e, String msg, Throwable t) { receivedErr("Error", e, msg, t); }
    @Override public synchronized void warningOccurred       (VRTEvent e, String msg, Throwable t) { receivedErr("Warning", e, msg, t); }
    @Override public synchronized void sentPacket            (VRTEvent e, VRTPacket p) { /* not used */ }
    @Override public synchronized void receivedPacket        (VRTEvent e, VRTPacket p) { receivedPkt("Packet",  e, p, true); }
              public synchronized void receivedContextPacket (VRTEvent e, VRTPacket p) { receivedPkt("Context", e, p, true); }
              public synchronized void receivedDataPacket    (VRTEvent e,DataPacket p) { receivedPkt("Data", e, p, showData); }

    public synchronized void receivedInitialContext (VRTEvent e, String errorMsg, DataPacket data,
                                                     VRTPacket ctx, Map<Integer,VRTPacket> context) {
      if (count >= limit) return; // ignore any received during shutdown

      printTxt("Received Initial Context:");
      printTxt("  Status          : " + ((errorMsg == null)? "OK" : errorMsg));
      printTxt("  Associated Data : " + data);
      printTxt("  Primary Context : " + ctx);

      if (context.isEmpty()) {
        printTxt("  Full Context : <none>");
      }
      else {
        boolean first = true;
        for (Integer key : context.keySet()) {
          if (first) printTxt("  Full Context    : "+key+"\t = "+context.get(key));
          else       printTxt("                    "+key+"\t = "+context.get(key));
          first = false;
        }
      }
      printTxt("");

      if (vraOut != null) {
        for (VRTPacket p : context.values()) {
          vraOut.append(p);
        }
      }
      count++;
    }
  }

  /** Extension on the basic VITA-49 listener that implements VRTContextListener. */
  private final class V49Listener extends RawV49Listener implements VRTEventListener {
    V49Listener (boolean showData) {
      super(showData);
    }
  }
}
