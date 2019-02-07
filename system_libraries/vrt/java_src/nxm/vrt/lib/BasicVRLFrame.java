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

package nxm.vrt.lib;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static nxm.vrt.lib.VRTMath.packInt;
import static nxm.vrt.lib.VRTMath.unpackInt;

/** Defines a VRL frame type. The most frequently used implementations of interface this is
 *  {@link BasicVRLFrame}. <br>
 *  <br>
 *  Note that the numeric representation (byte order) used by all VRL frames is
 *  {@link java.nio.ByteOrder#BIG_ENDIAN}.
 */
public final class BasicVRLFrame implements VRLFrame, PacketIterator.PacketContainer {
  private       byte[]  bbuf;
  private       int     boff;
  private final boolean readOnly;  // Is this instance read-only?
  private final boolean direct;    // Is this instance directly writing to external buffers?


  /** Creates a new frame that has no packets in it. */
  public BasicVRLFrame () {
    this.bbuf     = new byte[] { VRL_FAW_0, VRL_FAW_1, VRL_FAW_2, VRL_FAW_3,
                                 0,         0,         0,         3,
                                 NO_CRC_0,  NO_CRC_1,  NO_CRC_2,  NO_CRC_3 };
    this.boff     = 0;
    this.readOnly = false;
    this.direct   = false;
  }

  /** Creates a new frame that has the specified packets in it. This is the same as a call to
   *  the no-argument constructor called by a call to {@link #setVRTPackets}.
   *  @param packets The packets to include in the frame.
   */
  public BasicVRLFrame (VRTPacket ... packets) {
    this();
    setVRTPackets(packets);
  }

  /** Creates a new instance accessing the given data buffer. Note that when the buffer length
   *  is given, only the most minimal of error checking is done. Users should call
   *  {@link #isFrameValid()} to verify that the packet is valid. Invalid frames can result
   *  unpredictable behavior, but this is explicitly allowed (to the extent possible) so that
   *  applications creating packets can use this class even if the packet isn't yet "valid".<br>
   *  <br>
   *  <b>Warning: If <tt>direct=true</tt> and <tt>readOnly=false</tt> the instance will have
   *  direct access to the underlying buffers. The lengths specified here are considered to be
   *  the *initial* length, not the max length. As updates are made to the packet, the space used
   *  is permitted to grow up to the maximum lengths for a VRL frame. This may result in data
   *  corruption and/or an {@link ArrayIndexOutOfBoundsException} if such space is not available.</b>
   *  @param bbuf     The data buffer to use.
   *  @param boff     The byte offset into the bbuf to start reading/writing at.
   *  @param blen     The length of bbuf in bytes (-1=use size in header of frame).
   *  @param readOnly Should users of this instance be able to modify the underlying data buffer?
   *  @param direct   Should the methods in this class directly interact with the buffer or should
   *                  a copy of the data be made so that changes to the buffer don't affect this
   *                  instance?
   *  @throws NullPointerException If the buffer is null.
   *  @throws IllegalArgumentException If the offset is negative or greater than the
   *          length of the buffer given.
   */
  public BasicVRLFrame (byte[] bbuf, int boff, int blen, boolean readOnly, boolean direct) {
    if (bbuf == null) throw new NullPointerException("Given null buffer.");
    if ((boff < 0) || (boff > bbuf.length)) {
      throw new IllegalArgumentException("Illegal buffer offset given.");
    }

    if (direct) {
      this.bbuf     = bbuf;
      this.boff     = boff;
      this.readOnly = readOnly;
      this.direct   = true;
    }
    else {
      if (blen < 0) {
        blen = getFrameLength(bbuf, boff);
      }
      this.bbuf     = new byte[blen];
      this.readOnly = readOnly;
      this.direct   = false;
      System.arraycopy(bbuf, boff, this.bbuf, 0, blen);
    }
  }

  @Override
  public String toString () {
    try {
      String err = getFrameValid(false);
      if (err != null) {
        return getClass().getName()+": <"+err+">";
      }
      return getClass().getName() + ": "
           + " FrameCount="+getFrameCount()
           + " FrameLength="+getFrameLength();
    }
    catch (Exception e) {
      return getClass().getName()+": <Invalid VRLFrame: "+e+">";
    }
  }

  @Override
  public boolean equals (Object o) {
    if (o == this) return true;
    if (!(o instanceof VRLFrame)) return false;
    VRLFrame f = (VRLFrame)o;

    byte[] buf;
    int    off;
    int    len = getFrameLength();
    if (f.getFrameLength() != len) return false; // quick check

    if (o instanceof BasicVRLFrame) {
      buf = ((BasicVRLFrame)f).bbuf;
      off = ((BasicVRLFrame)f).boff;
    }
    else {
      buf = f.getFrame();
      off = 0;
    }

    int end = boff + getFrameLength();
    for (int i=boff, j=off; i < end; i++,j++) {
      if (bbuf[i] != buf[j]) return false;
    }
    return true;
  }

  @Override
  public final int hashCode () {
    int hash = 0;
    int len  = Math.min(getFrameLength(), 32);

    int off = boff;
    for (int i = 0; i < len; i++) {
      hash ^= (bbuf[off++] << i);
    }
    return hash;
  }

  @Override
  public String getFrameValid (boolean strict) {
    return getFrameValid(strict, -1);
  }

  @Override
  public String getFrameValid (boolean strict, int length) {
    if (!isVRL(bbuf, boff)) {
      return "Invalid VRLFrame: Missing frame alignment word.";
    }
    if (bbuf.length-boff < getFrameLength()) {
      return "Invalid VRLFrame: Allocated buffer shorter than packet length.";
    }
    if (getFrameLength() < HEADER_LENGTH+TRAILER_LENGTH) {
      return "Invalid VRLFrame: Frame reports length of "+getFrameLength()+" "
           + "but minimum is "+(HEADER_LENGTH+TRAILER_LENGTH)+".";
    }
    if (!isCRCValid()) {
      return "Invalid VRLFrame: Failed CRC check.";
    }
    if ((length != -1) && (getFrameLength() != length)) {
      return "Invalid VRLFrame: Invalid frame length, frame reports "+getFrameLength()+" "
           + "octets, but working with "+length+" octets.";
    }

    if (strict) {
      try {
        PacketIterator pi = iterator();
        while (pi.hasMoreElements()) {
          pi.skipNext(); // <-- throws exception if invalid
        }
      }
      catch (Exception e) {
        return "Invalid VRLFrame: Length of packets in frame is not consistent "
             + "with length of frame.";
      }
    }
    return null;
  }

  @Override
  public boolean isFrameValid (int length) {
    return (getFrameValid(true,length) == null);
  }

  @Override
  public boolean isFrameValid () {
    return (getFrameValid(true,-1) == null);
  }

  @Override
  public boolean isCRCValid () {
    int off = boff + getFrameLength() - TRAILER_LENGTH;
    boolean noCRC = (bbuf[ off ] == NO_CRC_0)
                 && (bbuf[off+1] == NO_CRC_1)
                 && (bbuf[off+2] == NO_CRC_2)
                 && (bbuf[off+3] == NO_CRC_3);

    return noCRC || (unpackInt(bbuf, off) == computeCRC());
  }

  @Override
  public void updateCRC () {
    int off = boff + getFrameLength() - TRAILER_LENGTH;
    packInt(bbuf, off, computeCRC());
  }

  /** Clears the CRC by setting it to the NO_CRC value. */
  private void clearCRC () {
    int off = boff + getFrameLength() - TRAILER_LENGTH;
    bbuf[ off ] = NO_CRC_0;
    bbuf[off+1] = NO_CRC_1;
    bbuf[off+2] = NO_CRC_2;
    bbuf[off+3] = NO_CRC_3;
  }

  /** Computes the CRC for the frame, but does not insert it into the frame. */
  private int computeCRC () {
    // References:
    //   [1] VITA 49.1
    //   [2] Warren, Henry S. Jr. "Hacker's Delight." Addison-Wesley, 2002. (ISBN 0-201-91465-4)

    final int COEFICIENTS = 0xEDB88320; // Corresponds to CRC32 polynomial
    int crc = 0;
    int end = boff + getFrameLength() - TRAILER_LENGTH;

    // This is based on the code given in Appendix A of [1], but it has
    // been extensively optimized. Note that to make the (COEFICIENTS * (0 or 1)) trick work
    // we need to compute the CRC in bit-reversed order and then flip the bits at the end.
    for (int off = boff; off < end; off++) {
      int val = bbuf[off];
      crc = (crc >>> 1) ^ (COEFICIENTS * ((crc ^ (val >> 7)) & 1));
      crc = (crc >>> 1) ^ (COEFICIENTS * ((crc ^ (val >> 6)) & 1));
      crc = (crc >>> 1) ^ (COEFICIENTS * ((crc ^ (val >> 5)) & 1));
      crc = (crc >>> 1) ^ (COEFICIENTS * ((crc ^ (val >> 4)) & 1));
      crc = (crc >>> 1) ^ (COEFICIENTS * ((crc ^ (val >> 3)) & 1));
      crc = (crc >>> 1) ^ (COEFICIENTS * ((crc ^ (val >> 2)) & 1));
      crc = (crc >>> 1) ^ (COEFICIENTS * ((crc ^ (val >> 1)) & 1));
      crc = (crc >>> 1) ^ (COEFICIENTS * ((crc ^ (val     )) & 1));
    }

    // Flip CRC bits as noted above, we are using the bit-shifting algorithm from Section 7-1 in
    // [2] rather than a loop-based solution in Appendix A of [1] which is considerably slower.
    crc = ((crc & 0x55555555) <<  1) | ((crc & 0xAAAAAAAA) >>>  1);  // abcdefgh -> badcfehg (per byte)
    crc = ((crc & 0x33333333) <<  2) | ((crc & 0xCCCCCCCC) >>>  2);  // badcfehg -> dcbahgfe (per byte)
    crc = ((crc & 0x0F0F0F0F) <<  4) | ((crc & 0xF0F0F0F0) >>>  4);  // dcbahgfe -> hgfedcba (per byte)
    crc = ((crc & 0x00FF00FF) <<  8) | ((crc & 0xFF00FF00) >>>  8);  // 0123 -> 1032
    crc = ((crc & 0x0000FFFF) << 16) | ((crc & 0xFFFF0000) >>> 16);  // 1032 -> 3210

    return crc;
  }

  @Override
  public VRLFrame copy () {
    return new BasicVRLFrame(bbuf, boff, getFrameLength(), false, false);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // PacketContainer Functions
  //////////////////////////////////////////////////////////////////////////////////////////////////
  /** Gets an iterator over all of the VRT packets contained in the VRL frame. Unlike the normal
   *  {@link #iterator()} method, the packets are not marked read-only and are not resolved beyond
   *  their {@link BasicVRTPacket} implementation. This is an extremely dangerous situation since
   *  <b>any change that alters the length of the packet will corrupt the VRL frame and the other
   *  packets contained within</b>. This function is here to support the last-minute updates to
   *  fixed fields (such as the packet counter) that are required immediately before transmission.
   *  @return An iterator that will iterate over all the packets in the frame.
   */
  public PacketIterator iteratorRW () {
    return new PacketIterator(this, HEADER_LENGTH, true, false, true);
  }

  @Override
  public PacketIterator iterator () {
    return new PacketIterator(this, HEADER_LENGTH, true, true, true);
  }

  @Override
  public boolean hasNextPacket (PacketIterator pi) {
    return (pi.offset >= HEADER_LENGTH) && (pi.offset < getFrameLength() - TRAILER_LENGTH);
  }

  @Override
  public VRTPacket getNextPacket (PacketIterator pi, boolean skip) {
    if (!hasNextPacket(pi)) {
      throw new NoSuchElementException("No such element in "+pi);
    }
    int off = (int)pi.offset + boff;

    // ==== READ PACKET HEADER =================================================
    int len = BasicVRTPacket.getPacketLength(bbuf, off);

    if ((off+len > getFrameLength()) || (len < 4)) {
      // Invalid Packet
      throw new RuntimeException("Error reading from "+this+" at "+pi.offset);
    }

    // ==== READ PACKET ========================================================
    VRTPacket packet = null;

    if (!skip) {
      packet = new BasicVRTPacket(bbuf, off, -1, pi.readOnly, pi.direct);
      packet = (pi.resolve)? VRTConfig.getPacket(packet,false) : packet;
    }

    // ==== UPDATE OFFSET COUNTER ==============================================
    pi.offset += len;
    return packet;
  }

  @Override
  public byte[] getNextPacketBuffer (PacketIterator pi) {
    if (!hasNextPacket(pi)) {
      throw new NoSuchElementException("No such element in "+pi);
    }
    int off = (int)pi.offset + boff;

    // ==== READ PACKET HEADER =================================================
    int len = BasicVRTPacket.getPacketLength(bbuf, off);

    if ((off+len > getFrameLength()) || (len < 4)) {
      // Invalid Packet
      throw new RuntimeException("Error reading from "+this+" at "+pi.offset);
    }

    // ==== READ PACKET ========================================================
    byte[] buffer = new byte[len];
    System.arraycopy(bbuf, off, buffer, off, len);

    // ==== UPDATE OFFSET COUNTER ==============================================
    pi.offset += len;
    return buffer;
  }

  @Override
  public void removePrevPacket (PacketIterator pi) {
    throw new UnsupportedOperationException("remove() not supported");
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Get / Set
  //////////////////////////////////////////////////////////////////////////////////////////////////
  @Override
  public int getPacketCount () {
    PacketIterator pi = iterator();
    int count = 0;
    while (pi.hasMoreElements()) {
      count++;
      pi.skipNext(); // <-- throws exception if invalid
    }
    return count;
  }

  /** <b>Internal Use Only:</b> Gets the packets in the frame in their raw form.
   *  @param buf The buffer containing the frame.
   *  @param off The offset for the start of the frame.
   *  @param len The length of the frame.
   *  @return The byte arrays representing the content for the packets in the frame.
   */
  public static List<byte[]> getVRTPackets (byte[] buf, int off, int len) {
    try {
      BasicVRLFrame  frame = new BasicVRLFrame(buf, off, len, false, true);
      PacketIterator iter  = new PacketIterator(frame, HEADER_LENGTH, false, false, false);
      List<byte[]>   list  = new ArrayList<byte[]>(8);

      while (iter.hasNext()) {
        list.add(iter.nextBuffer());
      }
      return list;
    }
    catch (Exception e) {
      return null;
    }
  }

  @Override
  public VRTPacket[] getVRTPackets () {
    ArrayList<VRTPacket> list = Collections.list(iterator());
    return list.toArray(new VRTPacket[list.size()]);
  }

  /** Sets the frame content to be a single VRT packet.
   *  @param packet The packet.
   *  @throws IllegalArgumentException If the packet is invalid.
   */
  public final void setVRTPackets (VRTPacket packet) {
    int maxFrameLength = (direct)? bbuf.length-boff : MAX_FRAME_LENGTH;
    setVRTPackets(false, maxFrameLength, packet);
  }

  /** Sets the frame content to be a single VRT packet. Of the (one) packet given,
   *  this method will try to put as many as possible into the frame that will
   *  fit given the specified maximum frame length. If the packet doesn't fit,
   *  it will not be inserted.
   *  @param maxFrameLength The maximum frame length in octets (inclusive of
   *                        header and trailer).
   *  @param packet The packet.
   *  @return The number of packets inserted into the frame (will be 1).
   *  @throws IllegalArgumentException If <tt>maxFrameLength</tt> is invalid
   *                                   or if the packet is invalid.
   */
  public int setVRTPackets (int maxFrameLength, VRTPacket packet) {
    if ((maxFrameLength < MIN_FRAME_LENGTH) || (maxFrameLength > MAX_FRAME_LENGTH)) {
      throw new IllegalArgumentException("Illegal max frame length given ("+maxFrameLength+")");
    }
    return setVRTPackets(true, maxFrameLength, packet);
  }

  @Override
  public final void setVRTPackets (VRTPacket ... packets) {
    int maxFrameLength = (direct)? bbuf.length-boff : MAX_FRAME_LENGTH;
    setVRTPackets(false, maxFrameLength, packets);
  }

  @Override
  public int setVRTPackets (int maxFrameLength, VRTPacket ... packets) {
    if ((maxFrameLength < MIN_FRAME_LENGTH) || (maxFrameLength > MAX_FRAME_LENGTH)) {
      throw new IllegalArgumentException("Illegal max frame length given ("+maxFrameLength+")");
    }
    return setVRTPackets(true, maxFrameLength, packets);
  }

  private int setVRTPackets (boolean fit, int maxFrameLength, VRTPacket p) {
    if (readOnly) throw new UnsupportedOperationException("Frame is read only");

    int    len = HEADER_LENGTH + TRAILER_LENGTH;
    String err = p.getPacketValid(true);
    if (err != null) throw new IllegalArgumentException(err);

    int plen   = p.getPacketLength();
    int length = len + plen;
    if (length < maxFrameLength) {
      len = length;
    }
    else if (fit) {
      return 0;
    }
    else if (maxFrameLength == MAX_FRAME_LENGTH) {
      throw new ArrayIndexOutOfBoundsException("Total packet length exceeds MAX_FRAME_LENGTH");
    }
    else {
      throw new ArrayIndexOutOfBoundsException("Total packet length exceeds buffer length");
    }
    setFrameLength(len);

    p.readPacket(bbuf, boff+HEADER_LENGTH, 0, plen);
    clearCRC();
    return 1;
  }

  private int setVRTPackets (boolean fit, int maxFrameLength, VRTPacket ... packets) {
    if (readOnly           ) throw new UnsupportedOperationException("Frame is read only");
    if (packets.length == 0) return 0;
    if (packets.length == 1) return setVRTPackets(fit, maxFrameLength, packets[0]);

    int count = 0;
    int len   = HEADER_LENGTH + TRAILER_LENGTH;

    for (VRTPacket p : packets) {
      String err = p.getPacketValid(true);
      if (err != null) throw new IllegalArgumentException(err);

      int length = len + p.getPacketLength();

      if (length < maxFrameLength) {
        len = length;
        count++;
      }
      else if (fit) {
        break;
      }
      else if (maxFrameLength == MAX_FRAME_LENGTH) {
        throw new ArrayIndexOutOfBoundsException("Total packet length exceeds MAX_FRAME_LENGTH");
      }
      else {
        throw new ArrayIndexOutOfBoundsException("Total packet length exceeds buffer length");
      }
    }
    setFrameLength(len);

    int off = boff + HEADER_LENGTH;
    for (int i = 0; i < count; i++) {
      VRTPacket p = packets[i];
      int plen = p.getPacketLength();
      p.readPacket(bbuf, off, 0, plen);
      off += plen;
    }
    clearCRC();
    return count;
  }

  @Override
  public int getFrameCount () {
    return (unpackInt(bbuf, boff+4) >> 20) & 0x00000FFF;
  }

  @Override
  public void setFrameCount (int count) {
    if (readOnly) throw new UnsupportedOperationException("Frame is read only");
    if ((count < 0) || (count > 0x00000FFF)) {
      throw new IllegalArgumentException("Invalid frame count "+count);
    }

    int val = unpackInt(bbuf, boff+4);
    val = (count << 20) | (val & 0x000FFFFF);
    packInt(bbuf, boff+4, val);
    clearCRC();
  }

  @Override
  public int getFrameLength () {
    return getFrameLength(bbuf, boff);
  }

  /** <b>Internal Use Only:</b> Get VRL frame length using a buffer input.
   *  @param buf The buffer containing the frame.
   *  @param off The offset for the start of the frame.
   */
  public static int getFrameLength (byte[] buf, int off) {
    return (0x000FFFFF & unpackInt(buf, off+4)) << 2; // <<2 to convert words to bytes
  }

  /** <b>Internal Use Only:</b> Does the given buffer contain a VRL frame?
   *  @param buf The buffer to check. Note that the buffer can not be null and
   *             must have at least 4 octets following the offset given.
   *  @param off The offset into the buffer.
   *  @return true if it is a VRL frame, false otherwise.
   */
  public static boolean isVRL (byte[] buf, int off) {
    return (buf[ off ] == VRL_FAW_0) && (buf[off+1] == VRL_FAW_1)
        && (buf[off+2] == VRL_FAW_2) && (buf[off+3] == VRL_FAW_3);
  }

  @Override
  public void setFrameLength (int length) {
    if (readOnly) throw new UnsupportedOperationException("Frame is read only");
    if (((length & 0x3) != 0) || (length < MIN_FRAME_LENGTH) || (length > MAX_FRAME_LENGTH)) {
      throw new IllegalArgumentException("Invalid frame length "+length);
    }
    _setBufferLength(length, false);

    int val = unpackInt(bbuf, boff+4);
    val = (val & 0xFFF00000) | ((length>>2) & 0x000FFFFF); // >>2 to convert bytes to words
    packInt(bbuf, boff+4, val);
    clearCRC();
  }

  private void _setBufferLength (int len, boolean shrink) {
    if (bbuf.length-boff < len) {
      if (direct) {
        throw new ArrayIndexOutOfBoundsException("Frame contents exceeds buffer length");
      }
      else {
        byte[] buf = new byte[len];
        System.arraycopy(bbuf, boff, buf, 0, bbuf.length-boff);
        bbuf = buf;
        boff = 0;
      }
    }
    else if (shrink && !direct && (bbuf.length-boff > (len+4096))) {
      int length = (len + 0x000FFF) & 0xFFF000; // round up to nearest to 4 KiB.
      byte[] buf = new byte[length];
      System.arraycopy(bbuf, boff, buf, 0, bbuf.length-boff);
      bbuf = buf;
      boff = 0;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Read / Write
  //////////////////////////////////////////////////////////////////////////////////////////////////
  @Override
  public void writeFrameTo (OutputStream out) throws IOException {
    out.write(bbuf, boff, getFrameLength());
  }

  @Override
  public int readFrameFrom (InputStream in) throws IOException {
    return readFrameFrom(in, null, 0);
  }

  /** <b>Internal use only:</b> This allows the first four bytes (and it must be exactly 4) to be
   *  passed in pre-read without creating an additional PushbackInputStream. This is usually done
   *  in code used to check for the presence of the VRL FAW before reading in.
   *  @param in   The input stream.
   *  @param hdr  The content for the header (null if n/a).
   *  @param hoff The offset into the 'hdr' buffer (0 if n/a).
   *  @return The length of the frame if reading in a VRL frame and the length of the underlying
   *          packet if reading in a VRT packet without a frame.
   *  @throws IOException If there is an i/o exception during reading.
   */
  public int readFrameFrom (InputStream in, byte[] hdr, int hoff) throws IOException {
    if (readOnly) throw new UnsupportedOperationException("Frame is read only");
    if (hdr != null) {
      System.arraycopy(hdr, hoff, bbuf, boff, 4);
    }
    else {
      _read(in, bbuf, boff, 4);
    }

    if (isVRL(bbuf, boff)) {
      _read(in, bbuf, boff+4, HEADER_LENGTH-4);
      int vrlLen = getFrameLength();

      _setBufferLength(vrlLen, true);
      _read(in, bbuf, boff+HEADER_LENGTH, vrlLen-HEADER_LENGTH);
      return vrlLen;
    }
    else {
      int vrtLen = ((0xFF & bbuf[boff+2]) << 10) | ((0xFF & bbuf[boff+3]) << 2);
      int vrlLen = HEADER_LENGTH + vrtLen + TRAILER_LENGTH;

      _setBufferLength(vrlLen, true);

      // Create the VRT header, VRL header and trailer
      bbuf[boff+11] = bbuf[boff+3]; // move VRT packet bytes to correct location
      bbuf[boff+10] = bbuf[boff+2]; // move VRT packet bytes to correct location
      bbuf[boff+ 9] = bbuf[boff+1]; // move VRT packet bytes to correct location
      bbuf[boff+ 8] = bbuf[ boff ]; // move VRT packet bytes to correct location
      bbuf[boff+ 7] = 0;            // set via setFrameLength(..)
      bbuf[boff+ 6] = 0;            // set via setFrameLength(..)
      bbuf[boff+ 5] = 0;            // set via setFrameLength(..)
      bbuf[boff+ 4] = 0;            // set via setFrameLength(..)
      bbuf[boff+ 3] = VRL_FAW_3;    // set correct FAW chars
      bbuf[boff+ 2] = VRL_FAW_2;    // set correct FAW chars
      bbuf[boff+ 1] = VRL_FAW_1;    // set correct FAW chars
      bbuf[ boff  ] = VRL_FAW_0;    // set correct FAW chars
      setFrameLength(vrlLen);       // sets the frame length and then sets the CRC to "VEND"

      // Read in the rest of the VRT packet
      _read(in, bbuf, boff+4+HEADER_LENGTH, vrtLen-4);
      return vrtLen;
    }
  }

  /** Calls InputStream.read(..) until the entire length requested is read in. If the EOF comes
   *  before it should, an EOFException is thrown.
   */
  static byte[] _read (InputStream in, byte[] buf, int off, int len) throws IOException {
    if (buf == null) buf = new byte[off+len];

    int numRead = 0;
    while (numRead < len) {
      int count = in.read(buf, off, len);

      if (count < 0) {
        throw new EOFException("Premature end of VRL frame when reading from "+in);
      }
      numRead += count;
      off     += count;
    }
    return buf;
  }

  @Override
  public byte[] getFrame () {
    int len = getFrameLength();
    byte[] buf = new byte[len];
    readFrame(buf, 0, 0, len);
    return buf;
  }

  @Override
  public int readFrame (byte[] buf) {
    int len = Math.min(buf.length, getFrameLength());
    System.arraycopy(bbuf, boff, buf, 0, len);
    return readFrame(buf, 0, 0, len);
  }

  @Override
  public int readFrame (byte[] buf, int off, int poff, int len) {
    int flen = getFrameLength();
    if (poff >= flen) return -1;

    if (len > flen) len = flen;
    System.arraycopy(bbuf, boff+poff, buf, off, len);
    return len;
  }

  @Override
  public void setFrame (byte[] buf) {
    setFrame(buf, 0, buf.length);
  }

  @Override
  public void setFrame (byte[] buf, int off, int len) {
    setFrameLength(len);
    System.arraycopy(buf, off, bbuf, boff, len);
  }
}
