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

package nxm.vita.prim;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import nxm.sys.lib.Data;
import nxm.sys.lib.DataFile;
import nxm.sys.lib.FileName;
import nxm.sys.lib.Keywords;
import nxm.sys.lib.MidasException;
import nxm.sys.lib.Primitive;
import nxm.sys.lib.Table;
import nxm.sys.lib.Time;
import nxm.vrt.lib.ContextPacket;
import nxm.vrt.lib.ContextPacket.ContextAssocLists;
import nxm.vrt.lib.ContextPacket.Ephemeris;
import nxm.vrt.lib.DataPacket;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.VRLFrame;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.libm.EphemerisPacket;
import nxm.vrt.libm.EphemerisPacket.Point;

import static java.lang.Math.max;
import static nxm.vita.inc.VRTIO.blueFileFor;
import static nxm.vita.inc.VRTIO.readVRLFrame;
import static nxm.vita.inc.VRTIO.toTimeObject;
import static nxm.vrt.lib.TimeStamp.ONE_SEC;

/** Parent class for the auto-generated VRT-to-BLUE primitives. */
public class vrt2blue extends Primitive {
  private       DataFile                     inFile;           // Input data file (V49 packets)
  private       DataFile                     outFile;          // Current output file
  private       FileName                     outName;          // Output file name (or name template)
  private       long                         fileDurationPS;   // Approximate file duration in ps (-1=Inf)
  private       TimeStamp                    nextFileStart;    // First timestamp in file
  private       int                          fileCount;        // Number of files completed so far
  private       boolean                      foundInitialCtx;  // All initial context found?
  private       Integer                      pairedDataID;     // Stream ID for paired data stream
  private final LinkedList<VRTPacket>        pktQueue      = new LinkedList<>();       // Queue of packets to output
  private final Map<Integer,VRTPacket>       ctxPackets    = new LinkedHashMap<>(8);   // Status packets
  private final Map<Time,Table>              ephKeywords   = new LinkedHashMap<>(128); // Ephemeris keywords to write (tc=keys)
  private final Map<Time,Table>              ecefKeywords  = new LinkedHashMap<>(128); // Ephemeris keywords to write (tc=keys)
  private final Map<Double,Table>            tcKeywords    = new LinkedHashMap<>(128); // Timecode keywords to write (off=keys)

  @Override
  public int open () {
    double fileDuration = MA.getD("/DURATION", -1);

    inFile          = MA.getDataFile("IN");
    outFile         = null; // set later
    outName         = MA.getFileName("OUT");
    fileDurationPS  = (fileDuration < 0)? -1L : max(1L, (long)(fileDuration * ONE_SEC));
    nextFileStart   = null;  // not set yet
    fileCount       = 0;     // none so far
    foundInitialCtx = false; // not found
    pairedDataID    = null;  // not found

    pktQueue.clear();
    ctxPackets.clear();
    ephKeywords.clear();
    tcKeywords.clear();

    // OPEN INPUT FILE =============================================================================
    inFile.open();
    if (inFile.getType() != 1049) {
      throw new MidasException("Expected file with VRT packets (Type 1049) but got Type "+inFile.getType());
    }

    return NORMAL;
  }

  @Override
  public int process () {
    boolean eof  = false;
    boolean any  = false;

    // ==== READ INPUT DATA/CONTEXT AND ADD TO QUEUE ===============================================
    if (inFile.isReady(4)) {
      AtomicInteger numRead  = new AtomicInteger();
      VRLFrame      vrlFrame = readVRLFrame(inFile, numRead);

      if (numRead.intValue() < 0) {
        eof = true;
      }
      else if (numRead.intValue() == 0) {
        // none to read yet
      }
      else {
        // Handle the various packets read in
        for (VRTPacket pkt : vrlFrame) {
          //System.out.println("\n\n"+pkt);

          Integer streamID = pkt.getStreamIdentifier();
          if (streamID == null) {
            streamID = 0;
          }

          // ---- Check to see if we have enough context to start ----------------------------------
          if (!foundInitialCtx) {
            if (pkt instanceof DataPacket) {
              pairedDataID = streamID;
            }
            else {
              ctxPackets.put(streamID, pkt);
            }

            if (pairedDataID != null) {
              // Have paired data and (possibly) more context, see if we now have everything...
              VRTPacket paired = ctxPackets.get(pairedDataID);

              if (paired instanceof ContextPacket) {
                ContextPacket     ctx = (ContextPacket)paired;
                ContextAssocLists cal = ctx.getContextAssocLists();

                if (cal == null) {
                  // nothing else -> done looking
                  foundInitialCtx = true;
                }
                else {
                  boolean all = true;
                  for (int sid : cal.getSystemContext()      ) all = all && ctxPackets.containsKey(sid);
                  for (int sid : cal.getSourceContext()      ) all = all && ctxPackets.containsKey(sid);
                  for (int sid : cal.getVectorComponent()    ) all = all && ctxPackets.containsKey(sid);
                  for (int sid : cal.getAsynchronousChannel()) all = all && ctxPackets.containsKey(sid);
                  foundInitialCtx = all;
                }
              }
              else if (paired != null) {
                // This is the only option...
                foundInitialCtx = true;
              }

              if (foundInitialCtx) {
                // Order by timestamp and push into queue
                Map<TimeStamp,VRTPacket> map = new TreeMap<>();
                ctxPackets.values().forEach((p) -> map.put(p.getTimeStamp(), p));
                pktQueue.addAll(map.values());
              }
            }
          }

          // ---- Add packet to the queue ----------------------------------------------------------
          if (!foundInitialCtx) {
            // cant start yet
          }
          else if (pkt instanceof DataPacket) {
            pktQueue.add(pkt);
          }
          else {
            ctxPackets.put(streamID, pkt);
            pktQueue.add(pkt);
          }
        }
      }
    }

    // ==== PROCESS PACKETS ALREADY ON QUEUE =======================================================
    while (!pktQueue.isEmpty()) {
      VRTPacket pkt = pktQueue.pop();
      TimeStamp ts  = pkt.getTimeStamp();

      //System.out.println("\n\n"+pkt);
      if ((outFile == null) || ((nextFileStart != null) && (ts.compareTo(nextFileStart) >= 0))) {
        openOutFile(pkt); // start a new file
      }

      if (pkt instanceof DataPacket) {
        // Handle the time code (TC) keywords
        Table  keys   = new Table();
        int    n      = tcKeywords.size()+1;
        double sample = outFile.seek();

        keys.put("TCSAMPLE_"+n, sample);
        keys.put("TC_WHOLE_"+n, ts.getMidasSeconds());
        keys.put("TC_FRAC_"+n,  ts.getFractionalSeconds());

        tcKeywords.put(sample, keys);

        // Write the data
        byte[] buf = pkt.getPayload();
        outFile.write(buf, 0, buf.length);
      }
      else if (pkt instanceof ContextPacket) {
        ContextPacket ctx  = (ContextPacket)pkt;
        Ephemeris     eph  = ctx.getEphemerisECEF();

        if (eph != null) {
          Time  tc   = toTimeObject(eph.getTimeStamp());
          Table keys = new Table();
          int   n    = ecefKeywords.size()+1;

          keys.put("ETIM"+n, new Data(tc.getSec()));
          if (eph.getPositionX() != null) {
            keys.put("POSX"+n, new Data(eph.getPositionX()));
            keys.put("POSY"+n, new Data(eph.getPositionY()));
            keys.put("POSZ"+n, new Data(eph.getPositionZ()));
          }
          if (eph.getVelocityX() != null) {
            keys.put("VELX"+n, new Data(eph.getVelocityX()));
            keys.put("VELY"+n, new Data(eph.getVelocityY()));
            keys.put("VELZ"+n, new Data(eph.getVelocityZ()));
          }
          ecefKeywords.put(tc, keys);
        }
      }
      else if (pkt instanceof EphemerisPacket) {
        EphemerisPacket eph = (EphemerisPacket)pkt;
        TimeStamp       ft  = eph.getFixTime();

        for (Point p : eph.getAllPoints()) {
          Table keys = new Table();
          Time  tc   = toTimeObject(ft);
          int   n    = ephKeywords.size()+1;

          keys.put("ETIM"+n, new Data(tc.getSec()));
          if (eph.getPosFixType() != null) {
            keys.put("POSX"+n, new Data(p.getPositionX()));
            keys.put("POSY"+n, new Data(p.getPositionY()));
            keys.put("POSZ"+n, new Data(p.getPositionZ()));
          }
          if (eph.getVelFixType() != null) {
            keys.put("VELX"+n, new Data(p.getVelocityX()));
            keys.put("VELY"+n, new Data(p.getVelocityY()));
            keys.put("VELZ"+n, new Data(p.getVelocityZ()));
          }
          if (eph.getAccFixType() != null) {
            keys.put("ACCX"+n, new Data((double)p.getAccelerationX()));
            keys.put("ACCY"+n, new Data((double)p.getAccelerationY()));
            keys.put("ACCZ"+n, new Data((double)p.getAccelerationZ()));
          }
          ephKeywords.put(tc, keys);
          ft = ft.addPicoSeconds(eph.getFixDelta());
        }
      }
      else {
        // ignore it
      }
      any = true;
    }

    // ==== DONE ===================================================================================
    if (any) {
      return NORMAL;
    }
    else if (eof) {
      return FINISH;
    }
    else {
      return NOOP;
    }
  }

  @Override
  public int close () {
    closeInFile();
    closeOutFile();
    return NORMAL;
  }

  /** Close the current input file (if any). */
  private synchronized void closeInFile () {
    if (inFile == null) return; // done

    inFile.close();
    inFile = null;
  }

  /** Close the current output file (if any). */
  private synchronized void closeOutFile () {
    if (outFile == null) return; // done
    Keywords kw = outFile.getKeywordsObject();

    if (!ephKeywords.isEmpty()) {
      // Use preferred (more accurate) ECEF ephemeris values
      ephKeywords.values().forEach((keys) -> kw.addAll(keys));
    }
    else if (!ecefKeywords.isEmpty()) {
      // Use alternative (less accurate) ECEF ephemeris values
      ecefKeywords.values().forEach((keys) -> kw.addAll(keys));
    }
    else {
      M.warning("No ECEF ephemeris found in VRT stream");
    }

    // Add the time code (TC) keywords
    tcKeywords.values().forEach((keys) -> kw.addAll(keys));

    outFile.close();
    // TODO: This needs to be fixed...
    //ephKeywords.clear();
    //ecefKeywords.clear();
    tcKeywords.clear();

    if (getMessageHandler() != null) {
      Table tbl = new Table();
      tbl.put("FILENAME", outFile.getName());
      MQ.put("RECVFILE", 0, tbl, getMessageHandler(), this);
    }
    if (verbose) {
      M.info("Received file: "+outFile.getName());
    }
    outFile = null;
  }

  /** Opens a new output file. */
  private synchronized void openOutFile (VRTPacket first) {
    closeOutFile(); // make sure the old one is closed

    TimeStamp ts = first.getTimeStamp();
    Time      t  = toTimeObject(ts);
    String    fn = outName.toString().replace("%n", Integer.toString(fileCount))
                                     .replace("%t", t.toFileName());

    outFile = blueFileFor(this, fn, ts, pairedDataID, ctxPackets);
    outFile.setDataRep("IEEE"); // <-- always writing data as big-endian (IEEE) in this primitive
    outFile.open(DataFile.OUTPUT);
    fileCount++;

    if (fileDurationPS > 0) {
      nextFileStart = ts.addPicoSeconds(fileDurationPS);
    }
  }
}
