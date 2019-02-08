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
#include <primitive.h>
#include <cstdlib>
#include <map>
#include <xmtime.h>
#include <string>
#include "BasicVRTPacket.h"
#include "BasicContextPacket.h"
#include "BasicDataPacket.h"
#include "BasicVRLFrame.h"
#include "LeapSeconds.h"
#include "TimeStamp.h"
#include "VRTObject.h"

/** Provides a few X-Midas-like functions that support I/O of VRT packets similar to that used
 *  in the core X-Midas EASYIO package. Most of these functions are declared inline since they are
 *  either tiny or will be used only once or twice within the typical X-Midas primitive.
 */
#ifndef _vrtio_h
#define _vrtio_h

using namespace std;
using namespace vrt;

/** Delta between J1970 and J1950 epochs. */
static const int32_t J1970TOJ1950 = 631152000;

static int32_t NO_VRL_FRAMES = -1;

/** <b>Internal use only:</b> Special version of "grabx" used by m_vrt_grabx(..). */
static int32_t _M_VRT_GRABX (CPHEADER &HCB, char *tempBuffer, size_t len, bool optional=false) {
  int32_t off = 0;
  while ((off < len) && (!Mc->break_)) {
    int32_t numRead = 0;

    HCB.xfer_len = len - off;
    HCB.cons_len = len - off;
    m_grabx(HCB, &tempBuffer[off], numRead);

    if (numRead > 0) {
      off += numRead;
    }
    else if (numRead == 0) {
      // stalled on read (writes of packets should be all-at-once, clearly this is a case
      // where that did not happen, possibly a problem with pipe sizes or a pipe that has
      // lost synch with where packets start [in which case we are just spewing trash])
      cerr << "WARNING: Stalled on read VRT of packet (" << off << " of " << len << " bytes read)" << endl;
    }
    else if (optional && (off == 0)) {
      return 0; // EOF found on optional read
    }
    else {
      // pre-mature EOF
      throw VRTException("End-of-file detected before complete VRT packet was read.");
    }
  }

  if (off < len) {
    // pre-mature break
    throw VRTException("Mc->break_ set before complete VRT packet was read (%d of %d bytes read)", off, len);
  }
  return len;
}

/** This function reads a VRT packet from a file or pipe in a dataflow format.
 *  This is a small wrapper over the <tt>m_grabx(..)</tt> function in X-Midas. This
 *  function should not be mixed with other file read calls (e.g. <tt>m_grabx(..)</tt>)
 *  as it takes care to update the internal HCB values as appropriate to maintain
 *  synch between packets.
 *  @param HCB        Header control block for the file.
 *  @param frameLength The frame-length counter used internally to support the reading of
 *                     VRL frames. Omit this variable to disable reading of VRL frames,
 *                     otherwise initialize this to 0 when the file is opened and do not
 *                     modify it subsequently.
 *  @return The packet read in. The packet will be "null" (i.e. <tt>isNull(packet)</tt>
 *          will return true) if the end-of-file is reached (similar to <tt>m_grabx(..)</tt>
 *          returning -1) before a packet is seen.
 *  @throws VRTException If an incomplete or blatantly invalid packet is read in.
 */
inline BasicVRTPacket* m_vrt_grabx (CPHEADER &HCB, int32_t &frameLength=NO_VRL_FRAMES) {
  static vector<char> tempBuffer(BasicVRTPacket::MAX_PACKET_LENGTH);

  int32_t numRead = _M_VRT_GRABX(HCB, &tempBuffer[0], 4, true);
  if (numRead < 0) {
    return NULL; // EOF
  }

  // CHECK FOR VRL FRAME (START)
  if ((tempBuffer[0] == BasicVRLFrame::VRL_FAW_0) && (tempBuffer[1] == BasicVRLFrame::VRL_FAW_1) &&
      (tempBuffer[2] == BasicVRLFrame::VRL_FAW_2) && (tempBuffer[3] == BasicVRLFrame::VRL_FAW_3)) {
    if (frameLength != 0) {
      throw VRTException("Found a VRL frame in a VRT packet only stream.");
    }

    // Found a VRL frame
    _M_VRT_GRABX(HCB, &tempBuffer[4], 4);
    int32_t len = (VRTMath::unpackInt(tempBuffer, 4, BIG_ENDIAN) << 2) & 0x003FFFFF;

    if (len == 24) {
      // Empty VRL frame, skip trailer and try again. This should be exceedingly rare as it violates
      // the spirit, if not the letter, of the VITA-49.1 specification.
      cerr << "WARNING: Empty VRL frame found." << endl;
      _M_VRT_GRABX(HCB, &tempBuffer[8], 4);
      return m_vrt_grabx(HCB, frameLength);
    }

    // Read the first four of the first packet
    _M_VRT_GRABX(HCB, &tempBuffer[0], 4);
    frameLength = len - 8;
    }

  // READ PACKET
  int32_t len = ((int32_t)VRTMath::unpackShort(tempBuffer, 2, BIG_ENDIAN)) << 2;
  if (len < 4) {
    throw VRTException("Invalid VRT packet detected in VRL frame (length < 4 octets).");
  }
  _M_VRT_GRABX(HCB, &tempBuffer[4], len-4); // read 4 octets less since already read in

  // CHECK FOR VRL FRAME (END)
  if (frameLength > 0) {
    frameLength -= len;
    if (frameLength < 0) {
      // Total length of the packets in the frame exceeds the frame length.
      throw VRTException("Invalid VRT packet detected in VRL frame.");
  }

    if (frameLength == 4) {
      // discard the VRL frame trailer
      char trailer[] = { 0, 0, 0, 0 };
      _M_VRT_GRABX(HCB, &trailer[0], 4);
      frameLength = 0; // not in a frame now
    }
  }
  if ((tempBuffer[0] & 0xC0) == 0) {
//    return new BasicDataPacket(tempBuffer, 0, frameLength, false);
    return new BasicDataPacket(tempBuffer, 0, len, false);
  }
  else {
//    return new BasicContextPacket(tempBuffer, 0, frameLength, false);
    return new BasicContextPacket(tempBuffer, 0, len, false);
  }
}

/** This function adds (appends) a VRT packet to a file or pipe. This is a small
 *  wrapper over the standard <tt>m_filad(..)</tt> function in X-Midas. This
 *  function should not be mixed with other file write calls (e.g. <tt>m_filad(..)</tt>)
 *  as it takes care to update the internal HCB values as appropriate to maintain
 *  synch between packets.
 *  @param HCB        Header control block for the file.
 *  @param p          The packet to write.
 *  @throws VRTException If <tt>p</tt> is "null" or blatantly invalid.
 */
inline void m_vrt_filad (CPHEADER &HCB, const BasicVRTPacket &p) {
  if (isNull(p)) throw VRTException("Can not write null VRT packet to file.");
  BasicVRTPacket _p = *const_cast<BasicVRTPacket*>(&p);
  m_filad(HCB, _p.getPacketPointer(), _p.getPacketLength());
}

/** This function adds (appends) a VRL frame to a file or pipe. This is a small
 *  wrapper over the standard <tt>m_filad(..)</tt> function in X-Midas. This
 *  function should not be mixed with other file write calls (e.g. <tt>m_filad(..)</tt>)
 *  as it takes care to update the internal HCB values as appropriate to maintain
 *  synch between packets.
 *  @param HCB        Header control block for the file.
 *  @param f          The frame to write.
 *  @throws VRTException If <tt>f</tt> is "null" or blatantly invalid.
 */
inline void m_vrt_filad (CPHEADER &HCB, const BasicVRLFrame &f) {
  if (isNull(f)) throw VRTException("Can not write null VRL frame to file.");
  BasicVRLFrame _f = *const_cast<BasicVRLFrame*>(&f);
  m_filad(HCB, _f.getFramePointer(), _f.getFrameLength());
}

inline int_4 m_put_key_vrtvalue (HEADER& hdr, string key, Value& value)
{
  value.getType();
  switch (value.getType()) {
  case ValueType_Int8:
  {
    int8_t val = (int8_t)value;
    return m_put_keydata(hdr, key, (void*)&val ,1, 0, 'B');
    break;
  }
  case ValueType_Int16:
  {
    int16_t val = (int16_t)value;
    return m_put_keydata(hdr, key, (void*)&val ,2, 0, 'I');
    break;
  }
  case ValueType_Int32:
  {
    int32_t val = (int32_t)value;
    return m_put_keydata(hdr, key, (void*)&val ,4, 0, 'L');
    break;
  }
  case ValueType_Int64:
  {
    int64_t val = (int64_t)value;
    return m_put_keydata(hdr, key, (void*)&val ,8, 0, 'X');
    break;
  }
  case ValueType_Float:
  {
    float val = (float)value;
    return m_put_keydata(hdr, key, (void*)&val ,4, 0, 'F');
    break;
  }
  case ValueType_Double:
  {
    double val = (double)value;
    return m_put_keydata(hdr, key, (void*)&val ,8, 0, 'D');
    break;
  }
  case ValueType_Bool:
  {
    int8_t val = ((bool)value) ? 1 : 0;
    return m_put_keydata(hdr, key, (void*)&val ,1, 0, 'B');
    break;
  }
  case ValueType_BoolNull:
  {
    boolNull val = ((boolNull)value);
    switch (val) {
    case _TRUE:
      return m_put_keydata(hdr, key, (void*)&val ,1, 0, 'B');
      break;
    case _FALSE:
      return m_put_keydata(hdr, key, (void*)&val ,1, 0, 'B');
      break;
    case _NULL:
      return m_put_keyword(hdr, key, "NULL");
      break;
    }
    break;
  }
  case ValueType_String:
  case ValueType_WString:
  case ValueType_VRTObject:
  default:
  {
    return m_put_keyword(hdr, key, value.toString());
    break;
  }
  }
  throw VRTException("This should never happen");
}

/** Converts a TimeStamp to a UTC Time object.
 *  @param ts The time stamp.
 *  @param sr The sample rate (defaults to -1).  If no sample rate is given, and FractionalMode is
 *            SampleCount, zero will be returned for fsec.
 *  @return The Time object or a time of 0 if not supported.
 */
inline XMTime toTimeObject (const TimeStamp &ts, double sr = DOUBLE_NAN) {
  if (isNull(ts)) return XMTime(0.0);

  if (isnan(sr)) sr = ts.getSampleRate();

  if ((ts.getEpoch() == TimeStamp::GPS_EPOCH) || (ts.getEpoch() == TimeStamp::UTC_EPOCH)) {
    switch (ts.getFractionalMode()) {
    case FractionalMode_None:
    case FractionalMode_FreeRunningCount:
    {
      double wsec = ts.getPOSIXSeconds() + J1970TOJ1950;
      double fsec = 0.0;
      return XMTime(wsec, fsec);
    }
    case FractionalMode_RealTime:
    {
      double wsec = ts.getPOSIXSeconds() + J1970TOJ1950;
      double fsec = ts.getPicoSeconds() / ((double)TimeStamp::ONE_SEC);
      return XMTime(wsec, fsec);
    }
    case FractionalMode_SampleCount:
    {
      double wsec = ts.getPOSIXSeconds() + J1970TOJ1950;
      double fsec = 0.0;
      if (!isnan(sr))
        fsec = ts.getPicoSeconds(sr) / ((double)TimeStamp::ONE_SEC);
      return XMTime(wsec, fsec);
    }
    }
  }
  m_warning("NULL EPOCH, returning current time.");
  return XMTime(); // unsupported
}

/** Converts a Midas time object in UTC to a TimeStamp.
 *  @param t The Midas time in UTC.
 *  @return The TimeStamp object.
 */
inline TimeStamp toTimeStamp (const XMTime &t) {
  int64_t sec = (int64_t)(t.integralSeconds() - J1970TOJ1950);
  int64_t ps  = (int64_t)(t.fractionalSeconds() * TimeStamp::ONE_SEC);

  return TimeStamp(IntegerMode_UTC, sec, ps);
}

/** Converts a Midas time in UTC to a TimeStamp.
 *  @param t The Midas time in UTC.
 *  @return The TimeStamp object.
 */
inline TimeStamp toTimeStamp (double t) {
  return toTimeStamp(XMTime(t));
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
 *  @throws VRTException If the format specifier is not convertible to a VRT format.
 */
inline PayloadFormat toVrtFormat (char mode, char type, int32_t frameSize) {
  PayloadFormat pf;

  switch (mode) {
    case 'S': pf.setRealComplexType(RealComplexType_Real            ); break;
    case 'C': pf.setRealComplexType(RealComplexType_ComplexCartesian); break;
    case '1': pf.setRealComplexType(RealComplexType_Real            ); break; // Assume 'S'
    case '2': pf.setRealComplexType(RealComplexType_ComplexCartesian); break; // Assume 'C'
    default:  throw VRTException("Unsupported format %c%c",mode,type);
  }

  int dSize;
  switch (type) {
    case 'D': pf.setDataItemFormat(DataItemFormat_Double     ); dSize = 64; break;
    case 'F': pf.setDataItemFormat(DataItemFormat_Float      ); dSize = 32; break;
    case 'X': pf.setDataItemFormat(DataItemFormat_SignedInt  ); dSize = 64; break;
    case 'L': pf.setDataItemFormat(DataItemFormat_SignedInt  ); dSize = 32; break;
    case 'I': pf.setDataItemFormat(DataItemFormat_SignedInt  ); dSize = 16; break;
    case 'B': pf.setDataItemFormat(DataItemFormat_SignedInt  ); dSize =  8; break;
    case 'N': pf.setDataItemFormat(DataItemFormat_SignedInt  ); dSize =  4; break;
    case 'P': pf.setDataItemFormat(DataItemFormat_UnsignedInt); dSize =  1; break;
    default:  throw VRTException("Unsupported format %c%c",mode,type);
  }
  pf.setDataItemSize(dSize);
  pf.setItemPackingFieldSize(dSize);

  if (frameSize <= 0) frameSize = 1; // <-- Type 1000 files may use FS=0
  pf.setVectorSize(frameSize);
  return pf;
}

inline char toBlueMode(const PayloadFormat &pf) {
  if (pf.getRepeatCount() != 1) {
    throw VRTException("Unsupported payload format "+pf.toString());
  }

  switch (pf.getRealComplexType()) {
    case RealComplexType_Real:             return 'S';
    case RealComplexType_ComplexCartesian: return 'C';
    default: throw VRTException("Unsupported payload format "+pf.toString());
  }
}

inline char toBlueType(const PayloadFormat &pf) {
  int dSize = pf.getDataItemSize();
  int pSize = pf.getItemPackingFieldSize();

  if (pSize != dSize) {
    // may choose to support this in the future
    throw VRTException("Unsupported payload format "+pf.toString());
  }

  switch (pf.getDataItemFormat()) {
    case DataItemFormat_Double:         return 'D';
    case DataItemFormat_Float:          return 'F';
    case DataItemFormat_SignedInt:
      if (dSize ==  1) { return 'P'; } // Allow 1-bit signed and unsigned as SP
      if (dSize ==  4) { return 'N'; }
      if (dSize ==  8) { return 'B'; }
      if (dSize == 16) { return 'I'; }
      if (dSize == 32) { return 'L'; }
      if (dSize == 64) { return 'X'; }
      throw VRTException("Unsupported payload format "+pf.toString());
    case DataItemFormat_UnsignedInt:
      if (dSize ==  1) { return 'P'; } // Allow 1-bit signed and unsigned as SP
      throw VRTException("Unsupported payload format "+pf.toString());
    default: throw VRTException("Unsupported payload format "+pf.toString());
  }
}

/** Gets a BLUE format specifier matching a given VRT format specifier.
 *  @param pf The payload format.
 *  @return The applicable BLUE format specifier giving mode and type.
 *  @throws VRTException If the format specifier is not convertible to a BLUE format.
 */
inline string toBlueFormat (const PayloadFormat &pf) {
  if (pf.getDataType() == -1) {
    throw VRTException("Unsupported Payload Format.");
  }

  char str[3];
  str[0] = toBlueMode(pf);
  str[1] = toBlueType(pf);
  str[2] = 0;
  return str;
}

/** Creates a VRT context packet that matches a BLUE file. This only supports the basic Type
 *  1000 and Type 2000 BLUE files with scalar and complex data types and small frame size.
 *  @param in The input file.
 *  @return The corresponding context packet.
 *  @throws VRTException If it if not possible to create a VRT context packet for the
 *                       given input file/pipe.
 */
inline BasicContextPacket contextPacketFor (const CPHEADER &in) {
  int32_t       fs = in.subsize;
  PayloadFormat pf = toVrtFormat(in.format[0], in.format[1], fs);
  TimeStamp     ts = toTimeStamp(XMTime(in));
  double        xd = (in.type < 2000)? in.xdelta : in.ydelta;

  BasicContextPacket context;
  context.setDataPayloadFormat(pf);
  context.setSampleRate(1.0 / xd); // sec -> Hz
  context.setTimeStamp(ts);
  context.setChangePacket(true);

  if (fs > 1) {
    // Get parameters for secondary axis
    double bw = fs * in.xdelta;
    double xs = in.xstart;
    context.setFrequencyIF(xs + bw/2);
    context.setBandwidth(bw);
  }
  return context;
}
#if 0 // commenting out because it does not work yet
#define _GET_KEYVALUES(type) \
  mbuf = (sizeof(type) * value->size()); \
  { \
  buf = malloc(mbuf); \
  if (buf == NULL) { \
    throw VRTException("Out Of Memory"); \
  } \
  type *_buf = (type*)buf; \
  for (size_t i = 0; i < value->size(); i++) { \
    Value *v = value->at(i); \
    _buf[i] = (type)*v; \
    delete v; \
  } \
}

/** A variant of <tt>m_put_keydata</tt> that takes the keyword value from the
 *  given {@link #Value}.
 *  @param hcb    (I/O) HCB for the file.
 *  @param key    (I/O) Keyword name (OUT when MK_Typetag is in mode).
 *  @param value  (IN)  Value to set.
 *  @param mode   (IN)  Access mode (see <tt>m_put_keydata</tt> for list of values).
 *  @return Number of octets written (0 or negative on error).
 */
inline int32_t m_put_keyvalue (CPHEADER &hcb, string &key, const Value *value, int32_t mode) {
  char      bbuf[8];
  char     *buf  = NULL;
  int32_t   mbuf = 0;
  char      type;

  switch ((int32_t)value->getType()) {
    case ValueType_Int8       : type = 'B';  mbuf =  1;  *((int8_t *)bbuf) = (int8_t )*value;  break;
    case ValueType_Int16      : type = 'I';  mbuf =  2;  *((int16_t*)bbuf) = (int16_t)*value;  break;
    case ValueType_Int32      : type = 'L';  mbuf =  4;  *((int32_t*)bbuf) = (int32_t)*value;  break;
    case ValueType_Int64      : type = 'X';  mbuf =  8;  *((int64_t*)bbuf) = (int64_t)*value;  break;
    case ValueType_Float      : type = 'F';  mbuf =  4;  *((float  *)bbuf) = (float  )*value;  break;
    case ValueType_Double     : type = 'D';  mbuf =  8;  *((double *)bbuf) = (double )*value;  break;
    case ValueType_Bool       : type = 'B';  mbuf =  1;  *((int8_t *)bbuf) = (int8_t )*value;  break;
    case ValueType_BoolNull   : type = 'B';  mbuf =  1;  *((int8_t *)bbuf) = (int8_t )*value;  break;
    case ValueType_String     : type = 'A';  mbuf =  0;  break;
    case ValueType_WString    : type = 'A';  mbuf =  0;  break;
    case ValueType_VRTObject  : type = 'A';  mbuf =  0;  break;
    case -ValueType_Int8      : type = 'B';  _GET_KEYVALUES(int8_t );  break;
    case -ValueType_Int16     : type = 'I';  _GET_KEYVALUES(int16_t);  break;
    case -ValueType_Int32     : type = 'L';  _GET_KEYVALUES(int32_t);  break;
    case -ValueType_Int64     : type = 'X';  _GET_KEYVALUES(int64_t);  break;
    case -ValueType_Float     : type = 'F';  _GET_KEYVALUES(float  );  break;
    case -ValueType_Double    : type = 'D';  _GET_KEYVALUES(double );  break;
    case -ValueType_Bool      : type = 'B';  _GET_KEYVALUES(int8_t );  break;
    case -ValueType_BoolNull  : type = 'B';  _GET_KEYVALUES(int8_t );  break;
    case -ValueType_String    : type = 'A';  mbuf =  0;  break;
    case -ValueType_WString   : type = 'A';  mbuf =  0;  break;
    case -ValueType_VRTObject : type = 'A';  mbuf =  0;  break;
    default                   : type = 'A';  mbuf =  0;  break;
  }

  if (mbuf > 0) {
    if (buf == NULL) {
      return m_put_keydata(hcb, key, bbuf, mbuf, mode, type);
    }
    else {
      int32_t nput = m_put_keydata(hcb, key, buf, mbuf, mode, type);
      free(buf);
      return nput;
    }
  }
  else {
    string  str = value->toString();
    return m_put_keydata(hcb, key, str.c_str(), str.length(), mode, type);
  }
}
#undef _GET_KEYVALUES
#endif /* 0 */

#endif /* _vrtio_h */
