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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import nxm.sys.lib.Data;
import nxm.sys.lib.DataFile;
import nxm.sys.lib.Keywords;
import nxm.sys.lib.MidasException;
import nxm.sys.lib.Primitive;
import nxm.sys.lib.Time;
import nxm.sys.lib.TimeLine;
import nxm.vita.inc.TimeLineUtil;
import nxm.vrt.lib.ContextPacket;
import nxm.vrt.lib.ContextPacket.ContextAssocLists;
import nxm.vrt.lib.ContextPacket.Ephemeris;
import nxm.vrt.lib.StandardDataPacket;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.lib.VRTPacket.PayloadFormat;
import nxm.vrt.libm.EphemerisPacket;
import nxm.vrt.libm.EphemerisPacket.FixType;
import nxm.vrt.libm.EphemerisPacket.Point;
import nxm.vrt.libm.TimestampAccuracyPacket;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static nxm.vita.inc.VRTIO.contextPacketFor;
import static nxm.vita.inc.VRTIO.setPacketData;
import static nxm.vita.inc.VRTIO.toTimeObject;
import static nxm.vita.inc.VRTIO.toTimeStamp;
import static nxm.vita.inc.VRTIO.writeVRTPacket;
import static nxm.vrt.lib.TimeStamp.ONE_SEC;

/** Parent class for the auto-generated BLUE-to-VRT primitives. */
public class blue2vrt extends Primitive {
  private static final long ONE_MS = 1_000_000_000L; // The value of 1 ms in picoseconds.

  private       DataFile                  inFile;           // Input data file
  private       DataFile                  outFile;          // Output file (V49 packets)
  private       int                       repeat;           // Number of file wraps (-1=Infinite)
  private       int                       gap;              // Number of 0's (x TL) between wraps
  private       boolean                   realTime;         // Throttle the data to approximate real-time
  private       double                    timeOffset;       // Time offset for adjusting timestamps
  private       long                      ctxIntervalPS;    // Context resend interval (in ps)
  private       Data                      inData;           // Buffer for incoming data
  private       Data                      zeroData;         // Buffer with 0's for gap (during file wrap)
  private       double                    sampleRate;       // Sample rate from input file
  private       PayloadFormat             payloadFormat;    // Payload format from input file
  private       boolean                   invertedSpectrum; // Is spectrum inverted?
  private       TimeStamp                 firstTimeStamp;   // First timestamp pushed out (for realTime=true)
  private       long                      firstTimeMS;      // First system time (for realTime=true)
  private       TimeStamp                 nextTimeStamp;    // Next expectad timestamp for data
  private       long                      timestampAccPS;   // Timestamp accuracy (ps) 0=NoPrecisionTime
  private final Map<Long,List<VRTPacket>> fileStatus    = new TreeMap<>();     // Status for file (offset=status)
  private final LinkedList<VRTPacket>     pktQueue      = new LinkedList<>();  // Queue of packets to send
  private final Map<Integer,VRTPacket>    ctxQueue      = new TreeMap<>();     // Most up-to-date context in queue
  private final int                       pktQueueLimit = 8;                   // Nominal packet queue length
  private final int                       ctxStreamID   = 0x1000;              // Stream identifier for context packet (per V49a)
  private final int                       ephStreamID   = ctxStreamID+0x100;   // Stream identifier for ephemeris packet
  private final int                       tapStreamID   = -ctxStreamID;        // Stream identifier for timestamp accuracy


  @Override
  public int open () {
    double  timestampAcc = MA.getD("/TSACCURACY",  0);    // in seconds
    double  ctxInterval  = MA.getD("/CTXINTERVAL", 1);    // in seconds (V49a requires 10.0 or less)
    boolean strict       = MA.getState("/STRICT", false); // strictly follow JICD 4.2 ICD?

    inFile           = MA.getDataFile("IN");
    outFile          = MA.getDataFile("OUT");
    xfer             = MA.getL("/TL",     0); // (0=Default, -N=Bytes, +N=Elements)
    repeat           = MA.getL("/REPEAT", 0);
    gap              = MA.getL("/GAP",    0);
    realTime         = MA.getState("/RT");
    nextTimeStamp    = null;
    timeOffset       = 0;    // set below
    inData           = null; // set below
    zeroData         = null; // set below
    sampleRate       = 0;    // set below
    payloadFormat    = null; // set below
    invertedSpectrum = false;// set below
    firstTimeStamp   = null; // set at first send
    firstTimeMS      = 0;    // set at first send
    timestampAccPS   = (long)((timestampAcc <= 0)? timestampAcc : max(1, timestampAcc * ONE_SEC));
    ctxIntervalPS    = (long)((ctxInterval  <= 0)? ctxInterval  : max(1, ctxInterval  * ONE_SEC));

    pktQueue.clear();
    ctxQueue.clear();
    fileStatus.clear();

    // ==== OPEN OUTPUT FILE =======================================================================
    outFile.open(DataFile.OUTPUT);
    outFile.setType(1049);
    outFile.setFormat("SB");
    outFile.setDataRep("IEEE");
    outFile.setComment("Raw VRT packets");
    outFile.flush();

    // ==== OPEN INPUT DATA FILE ===================================================================
    inFile.open(DataFile.INPUT);

    // Special transfer length handling based on file type...
    switch (inFile.getTypeCodeClass()) {
      case 1:
        if (xfer == 0) {
          xfer = -1024; // Reasonable default that doesn't require "jumbo frames" (override below)
        }
        if (xfer < 0) {
          xfer = (int)(-xfer / inFile.getBPE());
        }
        inData = inFile.getDataBuffer(xfer);
        break;

      case 2:
        if (xfer == 0) {
          if (inFile.getBPE() <= 32768) {
            xfer = 1;     // Send entire frame if it is small enough (~63 KiB limit for use over UDP)
          }
          else {
            xfer = -8196; // Frame needs to be split up, this is a reasonable default (override below)
          }
        }
        if (xfer < 0) {
          int _xfer = (int)(-xfer / inFile.getBPE()); // <-- can be 0 (0=split between N frames)
          if (_xfer > 0) {
            xfer     = _xfer;
            inData   = inFile.getDataBuffer(xfer);
            zeroData = inFile.getDataBuffer(xfer);
          }
          else {
            inData   = inFile.getDataBuffer(1);
            zeroData = inFile.getDataBuffer(1);
          }
        }
        break;

      default:
        throw new MidasException("Can not convert Type "+inFile.getType()+" BLUE file to VRT data "
                                +"packets");
    }

    // Generate mid-packet context information...
    Keywords kw = inFile.getKeywordsObject();

    // TimeLine fix...
    TimeLine tl = TimeLineUtil.fromBlueKeywords(kw, inFile.getDelta(), inFile.dbpe);

    if (tl != null) {
      try {
        Field timeLine = DataFile.class.getDeclaredField("timeLine");
        timeLine.setAccessible(true);
        timeLine.set(inFile, tl);
      }
      catch (SecurityException|IllegalArgumentException|ReflectiveOperationException e) {
        throw new MidasException("Unable to access fields in TimeLine", e);
      }
    }

    // Get timestamp accuracy...
    if (timestampAccPS < 0) {
      timestampAccPS = (long)max(1, inFile.getTimeLineTolerance()*ONE_SEC);
    }

    // Compute timeOffset...
    Time now = Time.currentTime();
    Time t0  = inFile.getTimeAtCurrent();   // time @ 0.0

    if (t0.isZero()) {
      timeOffset = now.getSec();            // shift t=0 to start at current time
    }
    else {
      timeOffset = now.diff(t0);            // shift t=T to start at current time
    }

    // Generate mid-packet context information...
    ContextPacket           ctx0 = contextPacketFor(inFile, strict); // Packet at offset=0
    TimestampAccuracyPacket tap  = new TimestampAccuracyPacket();
    ContextAssocLists       cal  = new ContextAssocLists();

    tap.setTimeStamp(ctx0.getTimeStamp());
    tap.setStreamIdentifier(tapStreamID);
    tap.setTimestampAccuracy(timestampAccPS);
    cal.setSystemContext(tap);
    ctx0.setStreamIdentifier(ctxStreamID);
    ctx0.setCalibratedTimeStamp(timestampAccPS != 0);
    ctx0.setContextAssocLists(cal);

    sampleRate    = ctx0.getSampleRate();
    payloadFormat = ctx0.getDataPayloadFormat();

    fileStatus.put(0L, new ArrayList<>(4));
    fileStatus.get(0L).add(ctx0);
    fileStatus.get(0L).add(tap);

    if (kw.getL("USE_SV_KEYWORDS", 0) > 0) {
      // Read in ECEF ephemeris information, include special ECEF ephemeris packet if available,
      // otherwise just use the ECEF field in the context.
      EphemerisPacket pkt    = new EphemerisPacket();
      List<Point>     points = new ArrayList<>(16);
      Double          lastTS = null;
      Double          etim;
      Ephemeris       save   = null;
      Integer         rate   = kw.getL("NSV_DATA_RATE", null);

      for (int i = 1; (etim = kw.getD("ETIM"+i, null)) != null; i++) {
        TimeStamp     ts     = toTimeStamp(etim);
        double        offset = inFile.getIndexAt(etim);
        long          off    = (long)offset;
        ContextPacket ctx    = (offset == 0)? ctx0 : ctx0.copy();
        Ephemeris     eph    = new Ephemeris();
        Point         p      = new Point();

        // For nearly-stationary ECEF values, the velocity will drop to 0.0 when packed into
        // the ephemeris field.
        eph.setTimeStamp(ts);
        eph.setPositionX(kw.getD("POSX"+i, null));
        eph.setPositionY(kw.getD("POSY"+i, null));
        eph.setPositionZ(kw.getD("POSZ"+i, null));
        eph.setVelocityX(kw.getD("VELX"+i, null));
        eph.setVelocityY(kw.getD("VELY"+i, null));
        eph.setVelocityZ(kw.getD("VELZ"+i, null));

        // For nearly-stationary ECEF values, the acceleration will drop to 0.0 when converted
        // to a float.
        p.setPositionX(kw.getD("POSX"+i, null));
        p.setPositionY(kw.getD("POSY"+i, null));
        p.setPositionZ(kw.getD("POSZ"+i, null));
        p.setVelocityX(kw.getD("VELX"+i, null));
        p.setVelocityY(kw.getD("VELY"+i, null));
        p.setVelocityZ(kw.getD("VELZ"+i, null));
        p.setAccelerationX(toFloat(kw.getD("ACCX"+i, null)));
        p.setAccelerationY(toFloat(kw.getD("ACCY"+i, null)));
        p.setAccelerationZ(toFloat(kw.getD("ACCZ"+i, null)));

        ctx.setEphemerisECEF(eph);

        if (ctx == ctx0) {
          // Already updated ctx0
          save = null;
        }
        else {
          ctx.setTimeStamp(ts);

          if (Double.isNaN(offset)) {
            // prior to ctx0, add it before ctx0, but save it so we can use it in ctx0 if there
            // is not one better
            off  = -1;
            save = eph;
          }
          if (!fileStatus.containsKey(off)) {
            fileStatus.put(off, new ArrayList<>(4));
          }
          fileStatus.get(off).add(ctx);
        }

        if (pkt == null) {
          // ignore
        }
        else if (points.isEmpty()) {
          // Init other parts of the packet
          if (kw.getD("POSX"+i, null) != null) pkt.setPosFixType(FixType.ACTUAL);
          if (kw.getD("VELX"+i, null) != null) pkt.setVelFixType(FixType.ACTUAL);
          if (kw.getD("ACCX"+i, null) != null) pkt.setAccFixType(FixType.ACTUAL);
          pkt.setAttFixType(null);
          pkt.setFixTime(ts);

          if (rate != null) {
            pkt.setFixDelta(toRoundSecToPS(1 / rate));
          }
          else {
            pkt.setFixDelta(0); // default (will recompute when next point is read)
          }
        }
        else if ((rate == null) && (points.size() == 1)) {
          // compute rate from delta between first two points
          long deltaPS = toRoundSecToPS(etim - lastTS);
          pkt.setFixDelta(deltaPS);
        }
        else {
          long deltaPS = toRoundSecToPS(etim - lastTS);
          if (deltaPS != pkt.getFixDelta()) {
            pkt = null; // ephemeris is not evenly spaced, can't send ECEF ephemeris packet
          }
        }
        points.add(p);
        lastTS = etim;
      }

      if (save != null) {
        // Put the saved ephemeris (last prior to first data value in packet) in the first packet
        ctx0.setEphemerisECEF(save);
      }

      if (pkt != null) {
        // Have ECEF Ephemeris Packet, but need to update all context packets to point to it.
        pkt.setStreamIdentifier(ephStreamID);
        pkt.setTimeStamp(ctx0.getTimeStamp());
        pkt.setAllPoints(points.toArray(new Point[points.size()]));
        fileStatus.get(0L).add(pkt);
        cal.setSourceContext(pkt);

        fileStatus.forEach((k,v) -> {
          v.forEach((p) -> {
            if (p instanceof ContextPacket) {
              ((ContextPacket)p).setContextAssocLists(cal);
            }
          });
        });
      }
    }

    // ==== PRE-POPULATE THE QUEUE =================================================================
    if (fileStatus.containsKey(-1L)) {
      // Any packets that come prior to the first data element should be added to the queue now
      fileStatus.get(-1L).forEach((p) -> addContextPacketToQueue(p));
    }

    return NORMAL;
  }

  @Override
  public int process () {
    boolean eof  = false;
    boolean any  = false;

    // ==== PROCESS PACKETS ALREADY ON QUEUE =======================================================
    if (!pktQueue.isEmpty()) {
      VRTPacket first = pktQueue.peek();

      if (firstTimeStamp == null) {
        firstTimeStamp = first.getTimeStamp();
        firstTimeMS    = System.currentTimeMillis();
      }
      long      deltaTimeMS = System.currentTimeMillis() - firstTimeMS;
      TimeStamp now         = firstTimeStamp.addPicoSeconds(deltaTimeMS * ONE_MS);

      while ((first != null) && (!realTime || (first.getTimeStamp().compareTo(now) <= 0))) {
        VRTPacket pkt = pktQueue.pop();

        //System.out.println("\n\n"+pkt);
        writeVRTPacket(outFile, pkt);

        any   = true;
        first = pktQueue.peek();
      }
    }

    // ==== READ INPUT DATA/CONTEXT AND ADD TO QUEUE ===============================================
    if (pktQueue.size() >= pktQueueLimit) {
      return NORMAL; // queue is already full
    }

    // ---- Read Data ------------------------------------------------------------------------------
    if (inFile.isOpen()) {
      // Determine where the next context packet will be (so we can read up to it, or send it
      // if we are there)...
      List<VRTPacket> context = null;
      int             toRead  = inData.getSize();          // number of elements to read
      long            first   = (long)inFile.getOffset();  // starting offset in file
      long            last    = first + toRead;            // nominal ending offset in file

      for (Map.Entry<Long,List<VRTPacket>> e : fileStatus.entrySet()) {
        long offset = e.getKey();
        if (offset < first) {
          // ignore old stuff
        }
        else if (offset == first) {
          // time to send an update
          context = e.getValue();
        }
        else if (offset <= last) {
          // On this pass only read up to the next context (which will be sent next time)
          toRead = (int)(offset - first);
        }
        else {
          // past end of what we will read now
          break;
        }
      }

      // Update the timestamp...
      Time      t  = inFile.getTimeAtCurrent();
      TimeStamp ts = toTimeStamp(t.addSec(timeOffset));

      // Read in the data...
      int numRead = inFile.read(inData, toRead);

      if (numRead == 0) {
        // Nothing to process
      }
      else if (numRead > 0) {
        // Process data
        if (context != null) {
          context.forEach((p) -> addContextPacketToQueue(ts,p));
        }
        nextTimeStamp = addDataPacketToQueue(ts, inData, numRead);
        any = true;
      }
      else {
        // Got EOF
        if (repeat != 0) {
          // Add a gap between the wrap
          for (int i = 0; i < gap; i++) {
            nextTimeStamp = addDataPacketToQueue(ts, zeroData, zeroData.getSize());
            any = true;
          }

          // Wrap the file
          if (repeat > 0) {
            repeat--;
          }
          inFile.seek(0.0);

          // Recompute timeOffset
          Time now = toTimeObject(nextTimeStamp);
          Time t0  = inFile.getTimeAtCurrent();   // time @ 0.0

          if (t0.isZero()) {
            timeOffset = now.getSec();            // shift t=0 to start at current time
          }
          else {
            timeOffset = now.diff(t0);            // shift t=T to start at current time
          }
        }
        else {
          eof = true;

          // Add any post EOF context information (often extra ECEF ephemeris information)
          fileStatus.forEach((k,v) -> {
            if (k >= inFile.seek()) {
              v.forEach((p) -> addContextPacketToQueue(p));
              inFile.seek(k + 1);
            }
          });
          fileStatus.clear(); // prevent any duplicate additions
        }
      }
    }

    // ==== DONE ===================================================================================
    if (any) {
      return NORMAL;
    }
    else if (eof && pktQueue.isEmpty()) {
      return FINISH;
    }
    else {
      return NOOP;
    }
  }

  @Override
  public int close () {
    if (inFile  != null) inFile.close();
    if (outFile != null) outFile.close();

    inFile  = null;
    outFile = null;
    pktQueue.clear();
    ctxQueue.clear();

    return NORMAL;
  }

  /** Converts Double to Float. */
  private static Float toFloat (Double val) {
    return (val == null)? null : val.floatValue();
  }

  /** Converts seconds to picoseconds rounding at milliseconds.
   *  @param sec The number of seconds.
   *  @return The number of picoseconds.
   */
  private static long toRoundSecToPS (double sec) {
    long ms = round(sec * 1000);
    return ms * ONE_MS;
  }

  /** Does a "resetForResend(..)" but with special handling for ephemeris information.
   *  @param p  The packet.
   *  @param ts The new timestamp.
   *  @return The updated packet.
   */
  private VRTPacket resetForResend (VRTPacket p, TimeStamp ts) {
    if (p.getStreamIdentifier() == ctxStreamID) {
      ContextPacket pkt = (ContextPacket)p;
      Ephemeris     eph = pkt.getEphemerisECEF();
      if (eph != null) {
        // Update ephemeris time (assume it matches packet)
        TimeStamp t = offsetTimeStamp(eph.getTimeStamp(), pkt, ts);
        eph.setTimeStamp(t);
        pkt.setEphemerisECEF(eph);
        pkt.setChangePacket(true);
      }
    }
    if (p.getStreamIdentifier() == ephStreamID) {
      // Update fix time (assume it doesn't match packet)
      EphemerisPacket pkt = (EphemerisPacket)p;
      TimeStamp       t   = offsetTimeStamp(pkt.getFixTime(), pkt, ts);
      pkt.setFixTime(t);
    }
    p.resetForResend(ts);
    return p;
  }

  /** Offsets a fix time relative to the packet offset.
   *  @param old The old "fix" time.
   *  @param pkt The old packet (with time).
   *  @param ts  The new packet time.
   *  @return The new "fix" time.
   */
  private static TimeStamp offsetTimeStamp (TimeStamp old, VRTPacket pkt, TimeStamp ts) {
    TimeStamp t    = pkt.getTimeStamp();
    long      dSec = old.getGPSSeconds()  - t.getGPSSeconds();
    long      dPS  = old.getPicoSeconds() - t.getPicoSeconds();
    return ts.addTime(dSec, dPS);
  }

  /** Adds a context packet to the queue adjusting its timestamp as needed.
   *  @param p The packet to add.
   */
  private void addContextPacketToQueue (VRTPacket p) {
    Time      t  = toTimeObject(p.getTimeStamp());
    TimeStamp ts = toTimeStamp(t.addSec(timeOffset));
    addContextPacketToQueue(ts, p);
  }

  /** Adds a context packet to the queue adjusting its timestamp as needed.
   *  @param ts The timestamp to use.
   *  @param p  The packet to add.
   */
  private void addContextPacketToQueue (TimeStamp ts, VRTPacket p) {
    resetForResend(p, ts);
    pktQueue.add(p);
    ctxQueue.put(p.getStreamIdentifier(), p);
  }

  /** Adds a data packet to the queue.
   *  @param ts      The timestamp to use.
   *  @param buf     The data for the packet.
   *  @param numRead The number of valid data elements.
   */
  private TimeStamp addDataPacketToQueue (TimeStamp ts, Data buf, int numRead) {
    int totalCount = numRead * buf.getSPA() * buf.getAPE();
    int offset     = 0;

    ctxQueue.values().forEach((p) -> {
      TimeStamp next = p.getTimeStamp().addPicoSeconds(ctxIntervalPS);

      if (next.compareTo(ts) < 0) {
        addContextPacketToQueue(ts, p);
      }
    });

    if (xfer > 0) {
      // Send a single packet
      return _addDataPacketToQueue(ts, buf, offset, totalCount);
    }
    else {
      // Send a set of packets
      int       eachCount  = min(2, -xfer / buf.bps);
      double    dt         = totalCount / sampleRate; // seconds
      TimeStamp nextTS     = ts.addPicoSeconds((long)(dt * ONE_SEC));

      if (payloadFormat.isComplex()) {
        eachCount = (eachCount / 2) * 2; // <-- rounds down to the nearest multiple of 2
      }

      while (totalCount > 0) {
        int count = min(totalCount, eachCount);
        _addDataPacketToQueue(ts, buf, offset, count);
        totalCount -= count;
        offset     += count;
      }
      return nextTS;
    }
  }

  /** Adds a data packet to the queue. This should only be called by addDataPacketToQueue(..).
   *  @param ts      The timestamp to use.
   *  @param buf     The data for the packet.
   *  @param offset  First value to write (in number of scalar values).
   *  @param count   Number of scalar values to write.
   */
  private TimeStamp _addDataPacketToQueue (TimeStamp ts, Data buf, int offset, int count) {
    // Send a single packet
    StandardDataPacket pkt = new StandardDataPacket(payloadFormat);

    pkt.setStreamIdentifier(ctxStreamID); // data and paired context use same ID (per V49)
    pkt.setTimeStamp(ts);
    pkt.setPayloadFormat(payloadFormat);
    pkt.setCalibratedTimeStamp(timestampAccPS != 0);
    pkt.setInvertedSpectrum(invertedSpectrum);
    setPacketData(pkt, payloadFormat, buf, offset, count);
    pktQueue.add(pkt);
    return pkt.getNextTimeStamp(sampleRate, payloadFormat);
  }
}