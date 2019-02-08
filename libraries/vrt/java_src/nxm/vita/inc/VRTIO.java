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

package nxm.vita.inc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import nxm.sys.inc.InternalUseOnly;
import nxm.sys.inc.MidasReference;
import nxm.sys.inc.Units;
import nxm.sys.lib.BaseFile;
import nxm.sys.lib.Convert;
import nxm.sys.lib.Data;
import nxm.sys.lib.DataFile;
import nxm.sys.lib.Keywords;
import nxm.sys.lib.MidasException;
import nxm.sys.lib.Shell;
import nxm.sys.lib.Time;
import nxm.vrt.lib.BasicContextPacket;
import nxm.vrt.lib.BasicVRLFrame;
import nxm.vrt.lib.BasicVRTPacket;
import nxm.vrt.lib.ContextPacket;
import nxm.vrt.lib.ContextPacket.GeoSentences;
import nxm.vrt.lib.DataPacket;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.VRLFrame;
import nxm.vrt.lib.VRTConfig;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.lib.VRTPacket.DataItemFormat;
import nxm.vrt.lib.VRTPacket.PayloadFormat;
import nxm.vrt.lib.VRTPacket.RealComplexType;

import static java.util.Objects.requireNonNull;
import static nxm.vrt.lib.TimeStamp.ONE_SEC;

/** Provides a few NeXtMidas-like functions that support I/O of VRT packets similar to that used
 *  in the core NeXtMidas I/O libraries.
 */
public final class VRTIO {
  /** Delta between J1970 and J1950 epochs. */
  public static final int J1970TOJ1950       = 631152000; // (7305 days) * (86,400 sec/day)


  private static byte osRep = 0; // Equal to Shell.getOSRep()

  private VRTIO () { } // prevent instantiation

  /** This function checks to see if a file or pipe is ready for reading a VRTPacket. This is
   *  a small wrapper over the {@link DataFile#isReady(double)} method in NeXtMidas.
   *  @param in The input file.
   *  @return true if ready for a call to {@link #readVRTPacket(DataFile)}, false if not yet ready.
   */
  public static boolean isReady (DataFile in) {
    double numElements = 4 / in.getBPE();
    return in.isReady(numElements);
  }

  /** This function reads a VRT packet from a file or pipe. This is a small wrapper over the
   *  {@link DataFile#read(byte[],int,int)} method in NeXtMidas. This method should not be mixed
   *  with other file read calls as it takes care to update the internal values as appropriate to
   *  maintain synch between packets. This method will block waiting for a packet to come in, if
   *  this is undesirable, the {@link #isReady(DataFile)} method can be used to see if a read can
   *  be done without waiting; note however that {@link #isReady(DataFile)} only checks to see if
   *  the start of a packet is available and this method may need to wait for the rest of the packet
   *  (packet writes should be nearly atomic, so this should rarely be an issue). <br>
   *  <br>
   *  <b>Users are encouraged to use the <tt>readVRLFrame(..)</tt> function in place of this one
   *  since it supports both VRT packets and VRL frames in the input stream.</b>
   *  @param in  The input file.
   *  @return The packet read in. The packet will be "null" if the end-of-file is reached
   *          (similar to <tt>read(..)</tt> returning -1) before a packet is seen.
   *  @throws RuntimeException If an incomplete or blatantly invalid packet is read in.
   */
  public static VRTPacket readVRTPacket (DataFile in) {
    return readVRTPacket(in, null);
  }

  /** This function reads a VRT packet from a file or pipe. This is identical to
   *  {@link #readVRTPacket(DataFile)}, except that it reports back the number of bytes read
   *  and/or the EOF code from {@link DataFile}. <br>
   *  <br>
   *  <b>Users are encouraged to use the <tt>readVRLFrame(..)</tt> function in place of this one
   *  since it supports both VRT packets and VRL frames in the input stream.</b>
   *  @param in      The input file.
   *  @param numRead The number of bytes read or the EOF code value. (OUTPUT)
   *  @return The packet read in. The packet will be "null" if the end-of-file is reached
   *          (similar to <tt>read(..)</tt> returning -1) before a packet is seen.
   *  @throws RuntimeException If an incomplete or blatantly invalid packet is read in.
   */
  public static VRTPacket readVRTPacket (DataFile in, AtomicInteger numRead) {
    try {
      byte[] firstFour = new byte[4];
      int num = in.read(firstFour, 0, 4);
      if (num < 0) {
        if (numRead != null) numRead.set(num);
        return null; // EOF
      }

      BasicVRTPacket p = new BasicVRTPacket();
      numRead.set(p.readPacketFrom(BaseFile_getInputStream(in), firstFour, 0));
      return VRTConfig.getPacket(p, false); // <-- resolves the exact packet class
    }
    catch (IOException e) {
      throw new MidasException("Error while reading VRT packet from "+in.getName(), e);
    }
  }

  /** This function reads a VRL frame from a file or pipe. This is a small wrapper over the
   *  {@link DataFile#read(byte[],int,int)} method in NeXtMidas. This method should not be mixed
   *  with other file read calls as it takes care to update the internal values as appropriate to
   *  maintain synch between packets. This method will block waiting for a frame to come in, if
   *  this is undesirable, the {@link #isReady(DataFile)} method can be used to see if a read can
   *  be done without waiting; note however that {@link #isReady(DataFile)} only checks to see if
   *  the start of a frame is available and this method may need to wait for the rest of the frame
   *  (frame writes should be nearly atomic, so this should rarely be an issue).
   *  @param in  The input file.
   *  @return The frame read in. The frame will be "null" if the end-of-file is reached
   *          (similar to <tt>read(..)</tt> returning -1) before a frame is seen.
   *  @throws RuntimeException If an incomplete or blatantly invalid packet is read in.
   */
  public static VRLFrame readVRLFrame (DataFile in) {
    return readVRLFrame(in, null);
  }

  /** This function reads a VRL frame from a file or pipe. This is identical to
   *  {@link #readVRTPacket(DataFile)}, except that it reports back the number of bytes read
   *  and/or the EOF code from {@link DataFile}.
   *  @param in      The input file.
   *  @param numRead The number of bytes read or the EOF code value. (OUTPUT)
   *  @return The frame read in. The frame will be "null" if the end-of-file is reached
   *          (similar to <tt>read(..)</tt> returning -1) before a frame is seen.
   *  @throws RuntimeException If an incomplete or blatantly invalid packet is read in.
   */
  public static VRLFrame readVRLFrame (DataFile in, AtomicInteger numRead) {
    try {
      byte[] firstFour = new byte[4];
      int num = in.read(firstFour, 0, 4);
      if (num < 0) {
        if (numRead != null) numRead.set(num);
        return null; // EOF
      }

      BasicVRLFrame f = new BasicVRLFrame();
      numRead.set(f.readFrameFrom(BaseFile_getInputStream(in), firstFour, 0));
      return f;
    }
    catch (IOException e) {
      throw new MidasException("Error while reading VRT packet from "+in.getName(), e);
    }
  }

  /** This function writes a VRT packet to a file or pipe. This is a small wrapper over the
   *  {@link DataFile#write(byte[],int,int)} method in NeXtMidas. This method should not be mixed
   *  with other file write calls as it takes care to update the internal values as appropriate to
   *  maintain synch between packets.
   *  @param out  The output file.
   *  @param p    The packet to write.
   *  @return The number of bytes written.
   *  @throws RuntimeException If a blatantly invalid packet is passed in.
   */
  public static int writeVRTPacket (DataFile out, VRTPacket p) {
//    try {
//      // OutputStream os = out.getOutputStream();       // See DR xxxx
//      OutputStream os = BaseFile_getOutputStream(out);
//      p.writePacketTo(os);
//      return p.getPacketLength();
//    }
//    catch (IOException e) {
//      throw new MidasException("Error while writing VRT packet to "+out.getName(), e);
//    }

      byte[] buf = p.getPacket();
      out.write(buf, 0, buf.length);
      return buf.length;
  }

  /** This function writes a VRL frame to a file or pipe. This is a small wrapper over the
   *  {@link DataFile#write(byte[],int,int)} method in NeXtMidas. This method should not be mixed
   *  with other file write calls as it takes care to update the internal values as appropriate to
   *  maintain synch between packets.
   *  @param out  The output file.
   *  @param f    The frame to write.
   *  @return The number of bytes written.
   *  @throws RuntimeException If a blatantly invalid packet is passed in.
   */
  public static int writeVRLFrame (DataFile out, VRLFrame f) {
    try {
      // OutputStream os = out.getOutputStream();       // See DR xxxx
      OutputStream os = BaseFile_getOutputStream(out);
      f.writeFrameTo(os);
      return f.getFrameLength();
    }
    catch (IOException e) {
      throw new MidasException("Error while writing VRL frame to "+out.getName(), e);
    }
  }

  /** Converts a TimeStamp to a UTC Time object.
   *  @param ts The time stamp.
   *  @return The Time object or null if not supported.
   */
  public static Time toTimeObject (TimeStamp ts) {
    if (ts == null) return null;

    if ((ts.getEpoch() == TimeStamp.GPS_EPOCH) || (ts.getEpoch() == TimeStamp.UTC_EPOCH)) {
      if (ts.getFractionalMode() == TimeStamp.FractionalMode.None) {
        double wsec = ts.getUTCSeconds() + J1970TOJ1950;
        double fsec = 0.0;
        return new Time(wsec, fsec);
      }
      if (ts.getFractionalMode() == TimeStamp.FractionalMode.RealTime) {
        double wsec = ts.getUTCSeconds() + J1970TOJ1950;
        double fsec = ts.getPicoSeconds() / ((double)TimeStamp.ONE_SEC);
        return new Time(wsec, fsec);
      }
    }
    return null; // unsupported
  }

  /** Converts a Midas time object in UTC to a TimeStamp.
   *  @param t The Midas time in UTC.
   *  @return The TimeStamp object.
   */
  public static TimeStamp toTimeStamp (Time t) {
    if (t == null) return null;

    long sec = (long)(t.getWSec() - J1970TOJ1950);
    long ps  = (long)(t.getFSec() * TimeStamp.ONE_SEC);

    return new TimeStamp(TimeStamp.IntegerMode.UTC, sec, ps);
  }

  /** Converts a Midas time in UTC to a TimeStamp.
   *  @param t The Midas time in UTC.
   *  @return The TimeStamp object.
   */
  public static TimeStamp toTimeStamp (double t) {
    return toTimeStamp(new Time(t));
  }

  /** Gets a VRT format specifier matching a given BLUE format specifier.
   *  @param mode      The Midas mode. Must be scalar ('S' or '1') or complex ('C' or '2'), other
   *                   modes are not supported.
   *  @param type      The Midas type. Must be double ('D'), float ('F'), long ('X'), int ('L'),
   *                   short ('I'), byte ('B'), nibble ('N') or bit ('P'). (Nibble and bit are
   *                   assumed to be big-endian notation within the byte.)
   *  @param frameSize The frame size (must be less than 65536, though any size where the total
   *                   number of octets per frame exceeds 32768 is strongly discouraged).
   *  @return The VRT format specifier.
   *  @throws IllegalArgumentException If the format specifier is not convertible to a VRT format.
   */
  public static PayloadFormat toVrtFormat (char mode, char type, int frameSize) {
    PayloadFormat pf = new PayloadFormat();

    switch (mode) {
      case 'S': pf.setRealComplexType(RealComplexType.Real            ); break;
      case 'C': pf.setRealComplexType(RealComplexType.ComplexCartesian); break;
      case '1': pf.setRealComplexType(RealComplexType.Real            ); break; // Assume 'S'
      case '2': pf.setRealComplexType(RealComplexType.ComplexCartesian); break; // Assume 'C'
      default:  throw new IllegalArgumentException("Unsupported format "+mode+type);
    }

    int dSize;
    switch (type) {
      case 'D': pf.setDataItemFormat(DataItemFormat.Double     ); dSize = 64; break;
      case 'F': pf.setDataItemFormat(DataItemFormat.Float      ); dSize = 32; break;
      case 'X': pf.setDataItemFormat(DataItemFormat.SignedInt  ); dSize = 64; break;
      case 'L': pf.setDataItemFormat(DataItemFormat.SignedInt  ); dSize = 32; break;
      case 'I': pf.setDataItemFormat(DataItemFormat.SignedInt  ); dSize = 16; break;
      case 'B': pf.setDataItemFormat(DataItemFormat.SignedInt  ); dSize =  8; break;
      case 'N': pf.setDataItemFormat(DataItemFormat.SignedInt  ); dSize =  4; break;
      case 'P': pf.setDataItemFormat(DataItemFormat.UnsignedInt); dSize =  1; break;
      default:  throw new IllegalArgumentException("Unsupported format "+mode+type);
    }
    pf.setDataItemSize(dSize);
    pf.setItemPackingFieldSize(dSize);

    if (frameSize <= 0) frameSize = 1; // <-- Type 1000 files may use FS=0
    pf.setVectorSize(frameSize);
    return pf;
  }

  /** Gets a BLUE format specifier matching a given VRT format specifier.
   *  @param pf The payload format.
   *  @return The applicable BLUE format specifier giving mode and type.
   *  @throws IllegalArgumentException If the format specifier is not convertible to a BLUE format.
   */
  public static String toBlueFormat (PayloadFormat pf) {
    char mode;
    char type;

    if (pf.getRepeatCount() != 1) {
      throw new IllegalArgumentException("Unsupported payload format "+pf);
    }

    switch (pf.getRealComplexType()) {
      case Real:             mode = 'S'; break;
      case ComplexCartesian: mode = 'C'; break;
      default: throw new IllegalArgumentException("Unsupported payload format "+pf);
    }

    int dSize = pf.getDataItemSize();
    int pSize = pf.getItemPackingFieldSize();

    if (pSize != dSize) {
      // may choose to support this in the future
      throw new IllegalArgumentException("Unsupported payload format "+pf);
    }

    switch (pf.getDataItemFormat()) {
      case Double:         type = 'D'; break;
      case Float:          type = 'F'; break;
      case SignedInt:
        if (dSize ==  1) { type = 'P'; break; } // Allow 1-bit signed and unsigned as SP
        if (dSize ==  4) { type = 'N'; break; }
        if (dSize ==  8) { type = 'B'; break; }
        if (dSize == 16) { type = 'I'; break; }
        if (dSize == 32) { type = 'L'; break; }
        if (dSize == 64) { type = 'X'; break; }
        throw new IllegalArgumentException("Unsupported payload format "+pf);
      case UnsignedInt:
        if (dSize ==  1) { type = 'P'; break; } // Allow 1-bit signed and unsigned as SP
        throw new IllegalArgumentException("Unsupported payload format "+pf);
      default: throw new IllegalArgumentException("Unsupported payload format "+pf);
    }
    return ""+mode+type;
  }

  /** Creates a BLUE file based on the given context information. This is essentially the reverse
   *  of {@link #contextPacketFor}. Note that this does NOT set DataRep. The data from a VRT packet
   *  is always big-endian (IEEE), but depending on how it is processed and written to the file,
   *  it may be converted to little-endian (EEEI) before writing.
   *  @param ref          The {@link MidasReference} to use.
   *  @param fname        The file name to use.
   *  @param ts           The time stamp for the file header.
   *  @param pairedCtxID  The Stream ID of the paired context packet.
   *  @param ctxPackets   The set of context packets (map of StreamID=Packet).
   *  @return The new BLUE file (not open yet).
   */
  public static DataFile blueFileFor (MidasReference ref, Object fname, TimeStamp ts,
                                      int pairedCtxID, Map<Integer,VRTPacket> ctxPackets) {
    DataFile  out = new DataFile(ref, fname);
    Keywords  kw  = out.getKeywordsObject();
    VRTPacket pkt = ctxPackets.get(pairedCtxID);

    out.setType(1000);
    out.setStart(0.0);
    out.setUnits(Units.TIME_S);

    if (ts         != null) out.setTime(toTimeObject(ts));

    if (pkt instanceof ContextPacket) {
      ContextPacket      ctx        = (ContextPacket)pkt;
      PayloadFormat      pf         = ctx.getDataPayloadFormat();
      Double             sampleRate = ctx.getSampleRate();
      Double             freqIF     = ctx.getFrequencyIF();
      Double             freqRF     = ctx.getFrequencyRF();
      Double             offsetRF   = ctx.getFrequencyOffsetRF();
      Double             bw         = ctx.getBandwidth();
      Long               pathDelay  = ctx.getTimeStampAdjustment();
      Boolean            inverted   = ctx.isInvertedSpectrum();
      Map<String,Object> keys       = parseKW(ctx.getGeoSentences());

      if (offsetRF   == null) offsetRF = 0.0;

      if (pf         != null) out.setFormat(toBlueFormat(pf));
      if (sampleRate != null) out.setDelta(1.0 / sampleRate);

      if (bw         != null) keys.put("BANDWIDTH",           new Data(bw));
      if (pathDelay  != null) keys.put("PATHDELAY",           new Data(((double)pathDelay) / ONE_SEC));
      if (sampleRate != null) keys.put("FS",                  new Data(sampleRate));
      if (freqRF     != null) keys.put("VRF",                 new Data(freqRF));
      if (freqRF     != null) keys.put("SBT",                 new Data(freqRF + offsetRF));
      if (freqIF     != null) keys.put("FNOM",                new Data(freqIF + offsetRF));
      if (inverted   != null) keys.put("SNAP_SPECTRAL_SENSE", (inverted)? "INVERTED" : "NORMAL");

      keys.put("USE_SV_KEYWORDS", new Data(1));
      kw.putAll(keys);
    }
    else {
      throw new MidasException("Unsupported context packet type: "+pkt);
    }
    return out;
  }

  /** Creates a VRT context packet that matches a BLUE file. This only supports the basic Type
   *  1000 and Type 2000 BLUE files with scalar and complex data types and small frame size.
   *  @param in The input file.
   *  @return The corresponding context packet.
   *  @throws IllegalArgumentException If it if not possible to create a VRT context packet for the
   *                                   given input file/pipe.
   */
  public static ContextPacket contextPacketFor (DataFile in) {
    return contextPacketFor(in, true);
  }

  /** Creates a VRT context packet that matches a BLUE file. This only supports the basic Type
   *  1000 and Type 2000 BLUE files with scalar and complex data types and small frame size.
   *  @param strict Strictly follow the JICD 4.2 ICD. If false, allow some minor flexibility.
   *  @param in     The input file.
   *  @return The corresponding context packet.
   *  @throws IllegalArgumentException If it if not possible to create a VRT context packet for the
   *                                   given input file/pipe.
   */
  @InternalUseOnly
  public static ContextPacket contextPacketFor (DataFile in, boolean strict) {
    BasicContextPacket ctx               = new BasicContextPacket();
    Keywords           kw                = in.getKeywordsObject();
    int                frameSize         = in.getFrameSize();
    PayloadFormat      pf                = toVrtFormat((char)in.getFormatMode(), (char)in.getFormatType(), frameSize);
    Time               time              = in.getTime();
    TimeStamp          ts                = (time.isZero())? TimeStamp.getSystemTime() : toTimeStamp(time);
    double             sampleRate        = 1.0 / in.getDelta(); // sec -> Hz
    double             freqIF            = (pf.isComplex())? 0.0        : sampleRate/4;
    double             defBW             = (pf.isComplex())? sampleRate : sampleRate/2;
    double             loCorrection      = getOptKwdD(kw, strict, "LO_CORRECTION",        1.0);
    double             pathDelay         = getReqKwdD(kw, strict, "PATHDELAY",            0.0);
    double             vrf               = getReqKwdD(kw, strict, "VRF",                  null) * loCorrection;
    double             sbt               = getReqKwdD(kw, strict, "SBT",                  vrf);
    double             fnom              = getReqKwdD(kw, strict, "FNOM",                 Double.NaN); // redundant
    double             bw                = getOptKwdD(kw, strict, "BANDWIDTH",            defBW);
    String             spectralSense     = getOptKwdS(kw, strict, "SNAP_SPECTRAL_SENSE",  "NORMAL");
    Double             antLat            = getOptKwdD(kw, strict, "ANT_LAT",              null);
    Double             antLon            = getOptKwdD(kw, strict, "ANT_LON",              null);
    Double             antAlt            = getOptKwdD(kw, strict, "ANT_ALT",              null);
    String             antenna           = getOptKwdS(kw, strict, "ANTENNA",              "NONE");
    String             classification    = getOptKwdS(kw, strict, "CLASSIFICATION",       null);
    String             desiredInterp     = getReqKwdS(kw, strict, "DESIREDINTERPOLATION", "LINEAR");
    String             feed              = getOptKwdS(kw, strict, "FEED",                 null);
    Double             feedLat           = getReqKwdD(kw, strict, "FEED_LAT",             null);
    Double             feedLon           = getReqKwdD(kw, strict, "FEED_LON",             null);
    Double             feedAlt           = getReqKwdD(kw, strict, "FEED_ALT",             null);
    String             mission           = getReqKwdS(kw, strict, "MISSION",              null);
    String             path              = getReqKwdS(kw, strict, "PATH",                 "NONE");
    Integer            sunspot           = getOptKwdL(kw, strict, "SUNSPOT",              null);
    Double             systemToaSigma    = getOptKwdD(kw, strict, "SYSTEM_TOA_SIGMA",     null);
    Double             systemFoaSigma    = getOptKwdD(kw, strict, "SYSTEM_FOA_SIGMA",     null);
    Double             systemToaBias     = getOthKwdD(kw, strict, "SYSTEM_TOA_BIAS",      null);
    Double             systemFoaBias     = getOthKwdD(kw, strict, "SYSTEM_FOA_BIAS",      null);
    Double             toaSigma          = getOptKwdD(kw, strict, "TOA_SIGMA",            null);
    Double             foaSigma          = getOptKwdD(kw, strict, "FOA_SIGMA",            null);
    Double             toaBias           = getOthKwdD(kw, strict, "TOA_BIAS",             null);
    Double             foaBias           = getOthKwdD(kw, strict, "FOA_BIAS",             null);
    String             comment           = getOthKwdS(kw, strict, "COMMENT",              in.getComment());
    String             tag               = getOthKwdS(kw, strict, "TAG",                  in.getTag());
    String             receiver          = getOthKwdS(kw, strict, "RECEIVER",             null);
    double             freqRF            = (vrf == sbt)? sbt   : vrf;
    Double             offsetRF          = (vrf == sbt)? null  : sbt - vrf;
    boolean            invertedSpectrum;   // set below

    if ((antLat != null) || (antLon != null) || (antAlt != null)) {
      requireNonNull(antLat, "Missing keyword: ANT_LAT");
      requireNonNull(antLon, "Missing keyword: ANT_LON");
      requireNonNull(antAlt, "Missing keyword: ANT_ALT");
    }
    else {
      requireNonNull(antenna, "Missing keyword: ANTENNA");
    }

    if ((sunspot != null) && (sunspot < 0)) {
      sunspot = null; // sunspotNum=-1 is same as omitted
    }

    if ((systemToaSigma != null) || (systemFoaSigma != null)) {
      requireNonNull(systemToaSigma, "Missing keyword: SYSTEM_TOA_SIGMA");
      requireNonNull(systemFoaSigma, "Missing keyword: SYSTEM_FOA_SIGMA");
    }

    if ((toaSigma != null) || (foaSigma != null)) {
      requireNonNull(toaSigma, "Missing keyword: TOA_SIGMA");
      requireNonNull(foaSigma, "Missing keyword: FOA_SIGMA");
    }

    // Update invertedSpectrum...
    switch (spectralSense) {
      case "NORMAL":   invertedSpectrum = false;  break;
      case "INVERTED": invertedSpectrum = true;   break;
      default:         throw new MidasException("Invalid keyword: SNAP_SPECTRAL_SENSE="+spectralSense);
    }

    // Changes for Type 2000...
    if (frameSize > 1) {
      // This is TBD...
      //// Get parameters for secondary axis
      //double bw = frameSize * in.getXDelta();
      //double xs = in.getXStart();
      //ctx.setFrequencyIF(xs + bw/2);
      //ctx.setBandwidth(bw);
    }

    // To work arround the lack of some important fields in the V49.0 context, we set the information
    // in the GeoSentences in NEMA-0183 format.
    GeoSentences  gs  = new GeoSentences();
    StringBuilder str = new StringBuilder(4096);
    appendKW(str, "ANT_LAT"             , antLat);
    appendKW(str, "ANT_LAT"             , antLat);
    appendKW(str, "ANT_LON"             , antLon);
    appendKW(str, "ANT_ALT"             , antAlt);
    appendKW(str, "ANTENNA"             , antenna);
    appendKW(str, "CLASSIFICATION"      , classification);
    appendKW(str, "DESIREDINTERPOLATION", desiredInterp);
    appendKW(str, "FEED"                , feed);
    appendKW(str, "FEED_LAT"            , feedLat);
    appendKW(str, "FEED_LON"            , feedLon);
    appendKW(str, "FEED_ALT"            , feedAlt);
    appendKW(str, "MISSION"             , mission);
    appendKW(str, "PATH"                , path);
    appendKW(str, "SUNSPOT"             , sunspot);
    appendKW(str, "SYSTEM_TOA_SIGMA"    , systemToaSigma);
    appendKW(str, "SYSTEM_FOA_SIGMA"    , systemFoaSigma);
    appendKW(str, "SYSTEM_TOA_BIAS"     , systemToaBias);
    appendKW(str, "SYSTEM_FOA_BIAS"     , systemFoaBias);
    appendKW(str, "TOA_SIGMA"           , toaSigma);
    appendKW(str, "FOA_SIGMA"           , foaSigma);
    appendKW(str, "TOA_BIAS"            , toaBias);
    appendKW(str, "FOA_BIAS"            , foaBias);
    appendKW(str, "COMMENT"             , comment);
    appendKW(str, "TAG"                 , tag);
    appendKW(str, "RECEIVER"            , receiver);

    if ((toaSigma != null) || (foaSigma != null)) {
      appendKW(str, "POS_COVAR_MTX11", getReqKwdD(kw, strict, "POS_COVAR_MTX11", null));
      appendKW(str, "POS_COVAR_MTX12", getReqKwdD(kw, strict, "POS_COVAR_MTX12", null));
      appendKW(str, "POS_COVAR_MTX13", getReqKwdD(kw, strict, "POS_COVAR_MTX13", null));
      appendKW(str, "POS_COVAR_MTX14", getOptKwdD(kw, strict, "POS_COVAR_MTX14", Double.NaN));
      appendKW(str, "POS_COVAR_MTX15", getOptKwdD(kw, strict, "POS_COVAR_MTX15", Double.NaN));
      appendKW(str, "POS_COVAR_MTX16", getOptKwdD(kw, strict, "POS_COVAR_MTX16", Double.NaN));

      appendKW(str, "POS_COVAR_MTX22", getReqKwdD(kw, strict, "POS_COVAR_MTX22", null));
      appendKW(str, "POS_COVAR_MTX23", getReqKwdD(kw, strict, "POS_COVAR_MTX23", null));
      appendKW(str, "POS_COVAR_MTX24", getOptKwdD(kw, strict, "POS_COVAR_MTX24", Double.NaN));
      appendKW(str, "POS_COVAR_MTX25", getOptKwdD(kw, strict, "POS_COVAR_MTX25", Double.NaN));
      appendKW(str, "POS_COVAR_MTX26", getOptKwdD(kw, strict, "POS_COVAR_MTX26", Double.NaN));

      appendKW(str, "POS_COVAR_MTX33", getReqKwdD(kw, strict, "POS_COVAR_MTX33", null));
      appendKW(str, "POS_COVAR_MTX34", getOptKwdD(kw, strict, "POS_COVAR_MTX34", Double.NaN));
      appendKW(str, "POS_COVAR_MTX35", getOptKwdD(kw, strict, "POS_COVAR_MTX35", Double.NaN));
      appendKW(str, "POS_COVAR_MTX36", getOptKwdD(kw, strict, "POS_COVAR_MTX36", Double.NaN));

      appendKW(str, "VEL_COVAR_MTX11", getReqKwdD(kw, strict, "VEL_COVAR_MTX11", null));
      appendKW(str, "VEL_COVAR_MTX12", getReqKwdD(kw, strict, "VEL_COVAR_MTX12", null));
      appendKW(str, "VEL_COVAR_MTX13", getReqKwdD(kw, strict, "VEL_COVAR_MTX13", null));

      appendKW(str, "VEL_COVAR_MTX22", getReqKwdD(kw, strict, "VEL_COVAR_MTX22", null));
      appendKW(str, "VEL_COVAR_MTX23", getReqKwdD(kw, strict, "VEL_COVAR_MTX23", null));

      appendKW(str, "VEL_COVAR_MTX33", getReqKwdD(kw, strict, "VEL_COVAR_MTX33", null));
    }
    gs.setManufacturerID("FF-FF-FF"); // not set
    gs.setSentences(str);

    // Set everything (in order)...
    ctx.setClassID("FF-FF-FA:2011.0003"); // from V49a
    ctx.setTimeStamp(ts);
    ctx.setChangePacket(true);
    ctx.setBandwidth(bw);
    ctx.setFrequencyIF(freqIF);
    ctx.setFrequencyRF(freqRF);
    ctx.setFrequencyOffsetRF(offsetRF);
    ctx.setSampleRate(sampleRate);
    ctx.setTimeStampAdjustment((long)(pathDelay * ONE_SEC));
    ctx.setInvertedSpectrum(invertedSpectrum);
    ctx.setDataPayloadFormat(pf);
    ctx.setGeoSentences(gs);

    return ctx;
  }

  /** Appends a JICD 4.2 keyword to the string buffer in NEMA-0183 format.
   *  @param str The buffer to append to.
   *  @param key The key.
   *  @param val The value.
   */
  private static StringBuilder appendKW (StringBuilder str, String key, Number val) {
    if ((val != null) && !Double.isNaN(val.doubleValue())) {
      appendKW(str, key, val.toString());
    }
    return str;
  }

  /** Appends a JICD 4.2 keyword to the string buffer in NEMA-0183 format.
   *  @param str The buffer to append to.
   *  @param key The key.
   *  @param val The value.
   */
  private static void appendKW (StringBuilder str, String key, String val) {
    if (val == null) return; // nothing to do
    StringBuilder s = new StringBuilder(80);

    s.append("$PJICDKW,").append(key).append(",");

    for (int i = 0; i < val.length(); i++) {
      char c = val.charAt(i);

           if ((c >= '0') && (c <= '9')) s.append(c);
      else if ((c >= 'A') && (c <= 'Z')) s.append(c);
      else if ((c >= 'a') && (c <= 'z')) s.append(c);
      else if ((c == '-') || (c == '.')) s.append(c);
      else                               s.append('^').append(toHex(c));
    }

    int cksum = 0;
    for (int i = 1; i < s.length(); i++) { // start at 1 to skip the '$'
      cksum = 0x7F & (cksum ^ s.charAt(i));
    }

    s.append('*').append(toHex(cksum));

    if (s.length() > 80) { // 80 is maximum sentence length
      throw new MidasException("Keyword exceeds maximum length: "+key+"="+val);
    }
    str.append(s).append("\r\n");
  }

  /** Parses any JICD 4.2 keywords out of the {@link GeoSentences}.
   *  @param gs The sentences.
   *  @return The parsed keywords.
   */
  @InternalUseOnly
  public static Map<String,Object> parseKW (GeoSentences gs) {
    Map<String,Object> map = new LinkedHashMap<>(32);

    if (gs == null) return map; // nothing there

    String str = gs.getSentences().toString();
    while (str.length() > 0) {
      if (str.startsWith("$PJICDKW,")) {
        int i = str.indexOf(',',   9 );
        int j = str.indexOf('*',  i+1);
        int k = str.indexOf('\n', j+1);

        if ((i < 0) || (j < 0)) {
          // invalid
        }
        else {
          String key = str.substring(9,i);
          String val = str.substring(i+1,j);

          for (int n = 0; n < val.length(); n++) {
            if (n == '^') {
              val = val.substring(0,n)
                  + (char)fromHex(val.substring(n+1,n+3))
                  + val.substring(n+3);
            }
          }
          map.put(key, Convert.s2o(val, null));
        }
        str = (i < 0)? "" : str.substring(i+1);
      }
      else if (str.startsWith("$")) {
        // Not our key
        int i = str.indexOf('\n');
        str = (i < 0)? "" : str.substring(i+1);
      }
      else {
        // Junk
        str = str.substring(1);
      }
    }
    return map;
  }

  /** Converts to a hex value for NEMA-0183. */
  private static String toHex (int val) {
    String hex = Integer.toHexString(0xFF & val).toUpperCase();
    return (hex.length() == 2)? hex : "0"+hex;
  }

  /** Converts from a hex value for NEMA-0183. */
  private static int fromHex (String str) {
    return Integer.valueOf(str, 16);
  }

  /** Gets a required keyword.
   *  @param kw     The keywords.
   *  @param strict Strict checking?
   *  @param key    The key.
   *  @param def    The default value (used if strict=false).
   *  @return The keyword value.
   *  @throws NullPointerException If strict and the key is missing.
   */
  private static Double getReqKwdD (Keywords kw, boolean strict, String key, Double def) {
    Double val = kw.getD(key, (strict)? null : def);
    return requireNonNull(val, "Missing keyword: "+key);
  }

  /** Gets a required keyword.
   *  @param kw     The keywords.
   *  @param strict Strict checking?
   *  @param key    The key.
   *  @param def    The default value (used if strict=false).
   *  @return The keyword value.
   *  @throws NullPointerException If strict and the key is missing.
   */
  private static Integer getReqKwdL (Keywords kw, boolean strict, String key, Integer def) {
    Integer val = kw.getL(key, (strict)? null : def);
    return requireNonNull(val, "Missing keyword: "+key);
  }

  /** Gets a required keyword.
   *  @param kw     The keywords.
   *  @param strict Strict checking?
   *  @param key    The key.
   *  @param def    The default value (used if strict=false).
   *  @return The keyword value.
   *  @throws NullPointerException If strict and the key is missing.
   */
  private static String getReqKwdS (Keywords kw, boolean strict, String key, String def) {
    String val = kw.getS(key, (strict)? null : def);
    return requireNonNull(val, "Missing keyword: "+key);
  }

  /** Gets an optional keyword.
   *  @param kw     The keywords.
   *  @param strict Strict checking?
   *  @param key    The key.
   *  @param def    The default value (used if strict=false).
   *  @return The keyword value.
   */
  private static Double getOptKwdD (Keywords kw, boolean strict, String key, Double def) {
    return kw.getD(key, def);
  }

  /** Gets an optional keyword.
   *  @param kw     The keywords.
   *  @param strict Strict checking?
   *  @param key    The key.
   *  @param def    The default value (used if strict=false).
   *  @return The keyword value.
   */
  private static Integer getOptKwdL (Keywords kw, boolean strict, String key, Integer def) {
    return kw.getL(key, def);
  }

  /** Gets an optional keyword.
   *  @param kw     The keywords.
   *  @param strict Strict checking?
   *  @param key    The key.
   *  @param def    The default value (used if strict=false).
   *  @return The keyword value.
   */
  private static String getOptKwdS (Keywords kw, boolean strict, String key, String def) {
    return kw.getS(key, def);
  }

  /** Gets an optional keyword that is not in the JICD 4.2 specification.
   *  @param kw     The keywords.
   *  @param strict Strict checking?
   *  @param key    The key.
   *  @param def    The default value (used if strict=false).
   *  @return The keyword value (always null if strict=true).
   */
  private static Double getOthKwdD (Keywords kw, boolean strict, String key, Double def) {
    return (strict)? null : kw.getD(key, def);
  }

  /** Gets an optional keyword that is not in the JICD 4.2 specification.
   *  @param kw     The keywords.
   *  @param strict Strict checking?
   *  @param key    The key.
   *  @param def    The default value (used if strict=false).
   *  @return The keyword value (always null if strict=true).
   */
  private static Integer getOthKwdL (Keywords kw, boolean strict, String key, Integer def) {
    return (strict)? null : kw.getL(key, def);
  }

  /** Gets an optional keyword that is not in the JICD 4.2 specification.
   *  @param kw     The keywords.
   *  @param strict Strict checking?
   *  @param key    The key.
   *  @param def    The default value (used if strict=false).
   *  @return The keyword value (always null if strict=true).
   */
  private static String getOthKwdS (Keywords kw, boolean strict, String key, String def) {
    return (strict)? null : kw.getS(key, def);
  }

  /** Sets the data in a VRT packet given a data buffer.
   *  @param pkt    The packet to set the data in.
   *  @param pf     The payload format (must match a Midas format).
   *  @param buf    The data buffer.
   *  @param off    The offset into the data buffer (IN SCALAR ELEMENTS).
   *  @param count  The number of elements to convert (IN SCALAR ELEMENTS).
   */
  public static void setPacketData (DataPacket pkt, PayloadFormat pf, Data buf, int off, int count) {
    int    boff   = (off   * buf.bps) + buf.boff;
    int    octets = (count * buf.bps);
    byte[] temp   = new byte[octets];
    byte   type;

    if (pf == null) {
      pf = pkt.getPayloadFormat();
    }

    switch (pf.getDataType()) {
      case Int4:   type = 'N'; break;
      case Int8:   type = 'B'; break;
      case Int16:  type = 'I'; break;
      case Int32:  type = 'L'; break;
      case Int64:  type = 'X'; break;
      case Float:  type = 'F'; break;
      case Double: type = 'D'; break;
      default:     throw new IllegalArgumentException("Unsupported format: "+pf);
    }

    if (osRep == 0) {
      osRep = (byte)Shell.getCurrent().getOSRep().charAt(0);
    }

    Convert.type(buf.buf, boff, buf.getFormatType(), temp, 0, type, count);
    Convert.rep(temp, 0, type, count, buf.getRep(), osRep); // <-- this is almost always a NOOP

    pkt.setDataLength(pf, count);
    pkt.setData(pf, temp, off, octets);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // FUNCTIONS THAT BELONG ELSEWHERE
  //////////////////////////////////////////////////////////////////////////////////////////////////

  /** Gets an InputStream for this file. This is a wrapper on the basic
      {@link BaseFile#read(byte[],int,int)} function and works no different than calling it. As
      such, users should expect similar performance, such as only reading from the "data section"
      for some files. The {@link IOResource#getInputStream()} is available for situations where
      these restrictions may be unacceptable. <br>
      <br>
      <b>Note that the object returned from this function call is NOT thread-safe.</b> As an
      optimization, multiple calls to this function MAY return a pointer to the same InputStream
      object.
      @return An InputStream for this file.
   */
  private static InputStream BaseFile_getInputStream (BaseFile bf) {
    InputStream inputStream = null;

    if (inputStream == null) {
      final BaseFile self = bf;

      inputStream = new InputStream() {
        final byte[] tempBuf = new byte[1];

        @Override
        public String toString () {
          return "InputStream for "+self;
        }

        @Override
        public int available ()  {
          return (int)Math.min(Integer.MAX_VALUE, self.avail());
        }

        @Override
        public int read () {
          int count = read(tempBuf, 0, 1);
          return (count < 0)? count : tempBuf[0];
        }

        @Override
        public int read (byte[] buf, int off, int len) {
          return self.read(buf, off, len);
        }

        @Override
        public void close() {
          self.close();
        }
      };
    }
    return inputStream;
  }

  /** Gets an OutputStream for this file. This is a wrapper on the basic
      {@link BaseFile#write(byte[],int,int)} function and works no different than calling it. As
      such, users should expect similar performance, such as only writing to the "data section"
      for some files. The {@link IOResource#getOutputStream()} is available for situations where
      these restrictions may be unacceptable. <br>
      <br>
      <b>Note that the object returned from this function call is NOT thread-safe.</b> As an
      optimization, multiple calls to this function MAY return a pointer to the same OutputStream
      object.
      @return An OutputStream for this file.
   */
  private static OutputStream BaseFile_getOutputStream (BaseFile bf) {
    OutputStream outputStream = null;

    if (outputStream == null) {
      final BaseFile self = bf;

      outputStream = new OutputStream() {
        final byte[] tempBuf = new byte[1];

        @Override
        public String toString () {
          return "OutputStream for "+self;
        }

        @Override
        public void write (int b) {
          tempBuf[0] = (byte)b;
          self.write(tempBuf, 0, 1);
        }

        @Override
        public void write (byte[] buf, int off, int len) {
          self.write(buf, off, len);
        }

        @Override
        public void close() {
          self.close();
        }
      };
    }
    return outputStream;
  }
}
