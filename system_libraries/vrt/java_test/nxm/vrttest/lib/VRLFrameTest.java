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

package nxm.vrttest.lib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import nxm.vrt.lib.BasicContextPacket;
import nxm.vrt.lib.BasicDataPacket;
import nxm.vrt.lib.BasicVRLFrame;
import nxm.vrt.lib.PacketIterator;
import nxm.vrt.lib.PacketIterator.PacketContainer;
import nxm.vrt.lib.VRLFrame;
import nxm.vrt.lib.VRTMath;
import nxm.vrt.lib.VRTPacket;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static nxm.vrttest.inc.TestRunner.assertBufEquals;
import static nxm.vrttest.inc.TestRunner.assertEquals;
import static nxm.vrttest.inc.TestUtilities.newInstance;
import static nxm.vrttest.lib.VRAFileTest.makeTestPacketArray;

/** Tests for the {@link VRLFrame} class. */
public class VRLFrameTest<T extends VRLFrame> implements Testable {
  private final Class<T> testedClass;

  /** Creates a new instance using the given implementation class. */
  public VRLFrameTest (Class<T> testedClass) {
    this.testedClass = testedClass;
  }

  @Override
  public Class<?> getTestedClass () {
    return testedClass;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    if (testedClass == VRLFrame.class) {
      tests.add(new TestSet("HEADER_LENGTH",         this,  "testConstants"));
      tests.add(new TestSet("MAX_FRAME_LENGTH",      this, "+testConstants"));
      tests.add(new TestSet("MAX_PAYLOAD_LENGTH",    this, "+testConstants"));
      tests.add(new TestSet("MIN_FRAME_LENGTH",      this, "+testConstants"));
      tests.add(new TestSet("NO_CRC",                this, "+testConstants"));
      tests.add(new TestSet("NO_CRC_0",              this, "+testConstants"));
      tests.add(new TestSet("NO_CRC_1",              this, "+testConstants"));
      tests.add(new TestSet("NO_CRC_2",              this, "+testConstants"));
      tests.add(new TestSet("NO_CRC_3",              this, "+testConstants"));
      tests.add(new TestSet("TRAILER_LENGTH",        this, "+testConstants"));
      tests.add(new TestSet("VRL_FAW",               this, "+testConstants"));
      tests.add(new TestSet("VRL_FAW_0",             this, "+testConstants"));
      tests.add(new TestSet("VRL_FAW_1",             this, "+testConstants"));
      tests.add(new TestSet("VRL_FAW_2",             this, "+testConstants"));
      tests.add(new TestSet("VRL_FAW_3",             this, "+testConstants"));
    }
    else {
      tests.add(new TestSet("copy()",                this,  "testCopy"));
      tests.add(new TestSet("equals(..)",            this, "+testCopy"));
      tests.add(new TestSet("getFrame()",            this,  "testGetFrame"));
      tests.add(new TestSet("getFrameCount()",       this,  "testGetFrameCount"));
      tests.add(new TestSet("getFrameLength()",      this, "+testGetFrame"));
      tests.add(new TestSet("getFrameValid(..)",     this, "+testIsFrameValid"));
      tests.add(new TestSet("getPacketCount()",      this, "+testGetFrame"));
      tests.add(new TestSet("getVRTPackets()",       this, "+testGetFrame"));
      tests.add(new TestSet("hashCode()",            this,  "testHashCode"));
      tests.add(new TestSet("iterator()",            this, "+testGetFrame"));
      if (testedClass == BasicVRLFrame.class) {
        tests.add(new TestSet("iteratorRW()",        this,  "testIteratorRW"));
      }
      tests.add(new TestSet("isCRCValid()",          this,  "testIsCRCValid"));
      tests.add(new TestSet("isFrameValid(..)",      this,  "testIsFrameValid"));
      if (testedClass == BasicVRLFrame.class) {
        tests.add(new TestSet("isVRL(..)",           this,  "testIsVRL"));
      }
      tests.add(new TestSet("readFrame(..)",         this,  "testReadFrame"));
      tests.add(new TestSet("readFrameFrom(..)",     this,  "testReadFrameFrom"));
      tests.add(new TestSet("setFrame(..)",          this,  "testSetFrame"));
      tests.add(new TestSet("setFrameCount(..)",     this, "+testGetFrameCount"));
      tests.add(new TestSet("setFrameLength(..)",    this, "+testGetFrame"));
      tests.add(new TestSet("setVRTPackets(..)",     this, "+testGetFrame"));
      tests.add(new TestSet("toString()",            this,  "testToString"));
      tests.add(new TestSet("updateCRC()",           this, "+testIsCRCValid"));
      tests.add(new TestSet("writeFrameTo(..)",      this,  "testWriteFrameTo"));
      if (PacketContainer.class.isAssignableFrom(testedClass)) {
        tests.add(new TestSet("getNextPacket(..)",   this,  "testGetNextPacket"));
        tests.add(new TestSet("hasNextPacket(..)",   this, "+testGetNextPacket"));
        tests.add(new TestSet("removeNextPacket(..)",this, "+testGetNextPacket"));
        tests.add(new TestSet("removePrevPacket(..)",this, "-")); // not supported
      }
    }

    return tests;
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }

  @Override
  public String toString () {
    return "Tests for "+getTestedClass();
  }

  public void testConstants () throws Exception {
    int expFAW = ((VRLFrame.VRL_FAW_0 & 0xFF) << 24)
               | ((VRLFrame.VRL_FAW_1 & 0xFF) << 16)
               | ((VRLFrame.VRL_FAW_2 & 0xFF) <<  8)
               | ((VRLFrame.VRL_FAW_3 & 0xFF)      );
    int expCRC = ((VRLFrame.NO_CRC_0  & 0xFF) << 24)
               | ((VRLFrame.NO_CRC_1  & 0xFF) << 16)
               | ((VRLFrame.NO_CRC_2  & 0xFF) <<  8)
               | ((VRLFrame.NO_CRC_3  & 0xFF)      );

    assertEquals("HEADER_LENGTH",         8,                  VRLFrame.HEADER_LENGTH);
    assertEquals("TRAILER_LENGTH",        4,                  VRLFrame.TRAILER_LENGTH);
    assertEquals("MAX_FRAME_LENGTH",      0x000FFFFF*4,       VRLFrame.MAX_FRAME_LENGTH);
    assertEquals("MIN_FRAME_LENGTH",      8+4,                VRLFrame.MIN_FRAME_LENGTH);
    assertEquals("MAX_PAYLOAD_LENGTH",    (0x000FFFFF*4)-8-4, VRLFrame.MAX_PAYLOAD_LENGTH);
    assertEquals("VRL_FAW",               expFAW,             VRLFrame.VRL_FAW);
    assertEquals("VRL_FAW_0",             (byte)'V',          VRLFrame.VRL_FAW_0);
    assertEquals("VRL_FAW_1",             (byte)'R',          VRLFrame.VRL_FAW_1);
    assertEquals("VRL_FAW_2",             (byte)'L',          VRLFrame.VRL_FAW_2);
    assertEquals("VRL_FAW_3",             (byte)'P',          VRLFrame.VRL_FAW_3);
    assertEquals("VRL_FAW",               expCRC,             VRLFrame.NO_CRC);
    assertEquals("VRL_FAW_0",             (byte)'V',          VRLFrame.NO_CRC_0);
    assertEquals("VRL_FAW_1",             (byte)'E',          VRLFrame.NO_CRC_1);
    assertEquals("VRL_FAW_2",             (byte)'N',          VRLFrame.NO_CRC_2);
    assertEquals("VRL_FAW_3",             (byte)'D',          VRLFrame.NO_CRC_3);
  }

  public void testCopy () throws Exception {
    VRLFrame frame1 = newInstance(testedClass);
    VRLFrame frame2 = newInstance(testedClass);
    frame2.setVRTPackets(makeTestPacketArray());

    VRLFrame f1 = frame1.copy();
    VRLFrame f2 = frame2.copy();

    assertEquals("Frame 1 == F1", frame1, f1);
    assertEquals("Frame 2 == F2", frame2, f2);

    assertEquals("Frame 1 != F2", false, frame1.equals(f2));
    assertEquals("Frame 2 != F1", false, frame2.equals(f1));
  }

  public void testGetFrame () throws Exception {
    VRTPacket[] packetArray = makeTestPacketArray();
    VRLFrame frame1 = newInstance(testedClass);
    VRLFrame frame2 = newInstance(testedClass);
    VRLFrame frame3 = newInstance(testedClass);
    VRLFrame frame4 = newInstance(testedClass);

    // ==== CHECK INSERTION ====================================================
    int lenForAll = 29495; // should be larger than all packets in array combined
    int lenFor3   = VRLFrame.HEADER_LENGTH
                  + packetArray[0].getPacketLength()
                  + packetArray[1].getPacketLength()
                  + packetArray[2].getPacketLength()
                  + packetArray[3].getPacketLength() / 2 // <-- doesn't fit
                  + VRLFrame.TRAILER_LENGTH;

    int count1 = 0;
    int count2 = packetArray.length;
                 frame2.setVRTPackets(packetArray);            // <-- all packets into frame
    int count3 = frame3.setVRTPackets(lenForAll, packetArray); // <-- all packets fit
    int count4 = frame4.setVRTPackets(lenFor3,   packetArray); // <-- only 3 packets fit

    assertEquals("Frame 1 Insert Count", 0,                  count1);
    assertEquals("Frame 2 Insert Count", packetArray.length, count2);
    assertEquals("Frame 3 Insert Count", packetArray.length, count3);
    assertEquals("Frame 4 Insert Count", 3,                  count4);
    assertEquals("Frame 1 Packet Count", count1,             frame1.getPacketCount());
    assertEquals("Frame 2 Packet Count", count2,             frame2.getPacketCount());
    assertEquals("Frame 3 Packet Count", count3,             frame3.getPacketCount());
    assertEquals("Frame 4 Packet Count", count4,             frame4.getPacketCount());

    // ==== CHECK FRAME CONTENT ================================================
    byte[] buf1 = frame1.getFrame();
    byte[] buf2 = frame2.getFrame();
    byte[] buf3 = frame3.getFrame();
    byte[] buf4 = frame4.getFrame();

    // Since the FrameCount is 0, the second word will be equal to the FrameLength/4
    assertEquals("Frame 1 Header 1", VRLFrame.VRL_FAW,          VRTMath.unpackInt(buf1, 0));
    assertEquals("Frame 2 Header 1", VRLFrame.VRL_FAW,          VRTMath.unpackInt(buf2, 0));
    assertEquals("Frame 3 Header 1", VRLFrame.VRL_FAW,          VRTMath.unpackInt(buf3, 0));
    assertEquals("Frame 4 Header 1", VRLFrame.VRL_FAW,          VRTMath.unpackInt(buf4, 0));
    assertEquals("Frame 1 Header 2", frame1.getFrameLength()/4, VRTMath.unpackInt(buf1, 4));
    assertEquals("Frame 2 Header 2", frame2.getFrameLength()/4, VRTMath.unpackInt(buf2, 4));
    assertEquals("Frame 3 Header 2", frame3.getFrameLength()/4, VRTMath.unpackInt(buf3, 4));
    assertEquals("Frame 4 Header 2", frame4.getFrameLength()/4, VRTMath.unpackInt(buf4, 4));

    VRTPacket[] pkts1 = frame1.getVRTPackets();
    VRTPacket[] pkts2 = frame2.getVRTPackets();
    VRTPacket[] pkts3 = frame3.getVRTPackets();
    VRTPacket[] pkts4 = frame4.getVRTPackets();

    Iterator<VRTPacket> iter1 = frame1.iterator();
    Iterator<VRTPacket> iter2 = frame2.iterator();
    Iterator<VRTPacket> iter3 = frame3.iterator();
    Iterator<VRTPacket> iter4 = frame4.iterator();

    int off1 = VRLFrame.HEADER_LENGTH;
    int off2 = VRLFrame.HEADER_LENGTH;
    int off3 = VRLFrame.HEADER_LENGTH;
    int off4 = VRLFrame.HEADER_LENGTH;
    for (int i = 0; i < packetArray.length; i++) {
      int len = packetArray[i].getPacketLength();
      if (i < count1) {
        assertBufEquals("Packet in Frame 1 (buf)",      buf1, off1, len, packetArray[i].getPacket(), 0, len);
        assertEquals(   "Packet in Frame 1 (array)",    packetArray[i], pkts1[i]);
        assertEquals(   "Packet in Frame 1 (iterator)", true,           iter1.hasNext());
        assertEquals(   "Packet in Frame 1 (iterator)", packetArray[i], iter1.next());
        off1 += len;
      }
      if (i < count2) {
        assertBufEquals("Packet in Frame 2 (buf)", buf2, off2, len, packetArray[i].getPacket(), 0, len);
        assertEquals(   "Packet in Frame 2 (array)",    packetArray[i], pkts2[i]);
        assertEquals(   "Packet in Frame 2 (iterator)", true,           iter2.hasNext());
        assertEquals(   "Packet in Frame 2 (iterator)", packetArray[i], iter2.next());
        off2 += len;
      }
      if (i < count3) {
        assertBufEquals("Packet in Frame 3 (buf)", buf3, off3, len, packetArray[i].getPacket(), 0, len);
        assertEquals(   "Packet in Frame 3 (array)",    packetArray[i], pkts3[i]);
        assertEquals(   "Packet in Frame 3 (iterator)", true,           iter3.hasNext());
        assertEquals(   "Packet in Frame 3 (iterator)", packetArray[i], iter3.next());
        off3 += len;
      }
      if (i < count4) {
        assertBufEquals("Packet in Frame 4 (buf)", buf4, off4, len, packetArray[i].getPacket(), 0, len);
        assertEquals(   "Packet in Frame 4 (array)",    packetArray[i], pkts4[i]);
        assertEquals(   "Packet in Frame 4 (iterator)", true,           iter4.hasNext());
        assertEquals(   "Packet in Frame 4 (iterator)", packetArray[i], iter4.next());
        off4 += len;
      }
    }
    off1 += VRLFrame.TRAILER_LENGTH;
    off2 += VRLFrame.TRAILER_LENGTH;
    off3 += VRLFrame.TRAILER_LENGTH;
    off4 += VRLFrame.TRAILER_LENGTH;

    assertEquals("Packet in Frame 1 (array len)",    count1,          pkts1.length);
    assertEquals("Packet in Frame 2 (array len)",    count2,          pkts2.length);
    assertEquals("Packet in Frame 3 (array len)",    count3,          pkts3.length);
    assertEquals("Packet in Frame 4 (array len)",    count4,          pkts4.length);
    assertEquals("Packet in Frame 1 (iterator len)", false,           iter1.hasNext());
    assertEquals("Packet in Frame 2 (iterator len)", false,           iter2.hasNext());
    assertEquals("Packet in Frame 3 (iterator len)", false,           iter3.hasNext());
    assertEquals("Packet in Frame 4 (iterator len)", false,           iter4.hasNext());
    assertEquals("Frame 1 Length",                   off1,            buf1.length);
    assertEquals("Frame 2 Length",                   off2,            buf2.length);
    assertEquals("Frame 3 Length",                   off3,            buf3.length);
    assertEquals("Frame 4 Length",                   off4,            buf4.length);
    assertEquals("Frame 1 Trailer",                  VRLFrame.NO_CRC, VRTMath.unpackInt(buf1, buf1.length-4));
    assertEquals("Frame 2 Trailer",                  VRLFrame.NO_CRC, VRTMath.unpackInt(buf2, buf2.length-4));
    assertEquals("Frame 3 Trailer",                  VRLFrame.NO_CRC, VRTMath.unpackInt(buf3, buf3.length-4));
    assertEquals("Frame 4 Trailer",                  VRLFrame.NO_CRC, VRTMath.unpackInt(buf4, buf4.length-4));

    // ==== CHECK STATIC LENGTH CHECK ==========================================
    assertEquals("getFrameLength()", frame1.getFrameLength(), BasicVRLFrame.getFrameLength(buf1,0));
    assertEquals("getFrameLength()", frame2.getFrameLength(), BasicVRLFrame.getFrameLength(buf2,0));
    assertEquals("getFrameLength()", frame3.getFrameLength(), BasicVRLFrame.getFrameLength(buf3,0));
    assertEquals("getFrameLength()", frame4.getFrameLength(), BasicVRLFrame.getFrameLength(buf4,0));
  }

  public void testToString () throws Exception {
    VRLFrame frame1 = newInstance(testedClass);
    VRLFrame frame2 = newInstance(testedClass);
    frame2.setVRTPackets(makeTestPacketArray());

    // Output string is free-form, so just make sure call works and doesn't return null
    assertEquals("Frame 1 toString() != null", true, (frame1.toString() != null));
    assertEquals("Frame 2 toString() != null", true, (frame2.toString() != null));
  }

  public void testGetNextPacket () throws Exception {
    VRLFrame frame1 = newInstance(testedClass);
    VRLFrame frame2 = newInstance(testedClass);
    frame2.setVRTPackets(makeTestPacketArray());

    PacketContainer     pc1 = (PacketContainer)frame1;
    PacketContainer     pc2 = (PacketContainer)frame2;
    PacketIterator      pi1 = (PacketIterator)frame1.iterator();
    PacketIterator      pi2 = (PacketIterator)frame2.iterator();
    Iterator<VRTPacket> it1 = frame1.iterator();
    Iterator<VRTPacket> it2 = frame2.iterator();

    while (it1.hasNext() && pc1.hasNextPacket(pi1)) {
      assertEquals("Frame 1 iterator pkt = PacketIterator pkt", it1.next(), pc1.getNextPacket(pi1,false));
    }
    while (it2.hasNext() && pc2.hasNextPacket(pi2)) {
      assertEquals("Frame 2 iterator pkt = PacketIterator pkt", it2.next(), pc2.getNextPacket(pi2,false));
    }

    assertEquals("Frame 1 Iterator lengths", it1.hasNext(), pc1.hasNextPacket(pi1));
    assertEquals("Frame 2 Iterator lengths", it2.hasNext(), pc2.hasNextPacket(pi2));

    if (testedClass == BasicVRLFrame.class) {
      PacketIterator piRW = (PacketIterator)frame2.iterator();
      try {
        pc2.removePrevPacket(piRW);
        assertEquals("BasicVRLFrame does not support removal from iterator", true, false);
      }
      catch (Exception e) {
        assertEquals("BasicVRLFrame does not support removal from iterator", true, true);
      }
    }
  }

  public void testIteratorRW () throws Exception {
    BasicVRLFrame frame1 = new BasicVRLFrame();
    BasicVRLFrame frame2 = new BasicVRLFrame();
    frame2.setVRTPackets(makeTestPacketArray());

    Iterator<VRTPacket> it1 = frame1.iterator();
    Iterator<VRTPacket> rw1 = frame1.iteratorRW();
    Iterator<VRTPacket> it2 = frame1.iterator();
    Iterator<VRTPacket> rw2 = frame1.iteratorRW();

    while (it1.hasNext() && rw1.hasNext()) {
      assertEquals("Frame 1 ReadOnly pkt = ReadWrite pkt", it1.next(), rw1.next());
    }
    while (it2.hasNext() && rw2.hasNext()) {
      assertEquals("Frame 2 ReadOnly pkt = ReadWrite pkt", it2.next(), rw2.next());
    }

    assertEquals("Frame 1 Iterator lengths", it1.hasNext(), rw1.hasNext());
    assertEquals("Frame 2 Iterator lengths", it2.hasNext(), rw2.hasNext());
  }

  public void testHashCode () throws Exception {
    VRLFrame frame1 = newInstance(testedClass);
    VRLFrame frame2 = newInstance(testedClass);
    frame2.setVRTPackets(makeTestPacketArray());

    // Output string is free-form, so just make sure call works and doesn't return null
    assertEquals("Frame 1 hashCode()", hash(frame1), frame1.hashCode());
    assertEquals("Frame 2 hashCode()", hash(frame2), frame2.hashCode());
  }

  /** Hash function described on docs for VRLFrame. */
  private int hash (VRLFrame f) {
    int    hash = 0;
    int    len  = Math.min(f.getFrameLength(), 32);
    byte[] buf  = new byte[len];

    f.readFrame(buf, 0, 0, len);
    for (int i = 0; i < len; i++) {
     hash ^= (buf[i] << i);
    }
    return hash;
  }


  public void testIsCRCValid () {
    // The test cases 'example1' and 'example2' come directly from Table A-1 in
    // the ANSI/VITA-49.1 specification. Note that the payloads aren't actually
    // valid VRT packets.
    int[]  example1 = { 0x56524C50, 0x00000004, 0x00000000, 0xB94CD673 };
    int[]  example2 = { 0x56524C50, 0x00000004, 0xFFFFFFFF, 0x7E480B08 };
    byte[] buf1  = new byte[example1.length * 4];
    byte[] buf2  = new byte[example1.length * 4];

    for (int i=0,j=0; i < example1.length; i++,j+=4) {
      VRTMath.packInt(buf1, j, example1[i]);
    }
    for (int i=0,j=0; i < example2.length; i++,j+=4) {
      VRTMath.packInt(buf2, j, example2[i]);
    }

    VRLFrame frame1 = newInstance(testedClass); // ANSI/VITA-49.1 'Example 1'
    VRLFrame frame2 = newInstance(testedClass); // ANSI/VITA-49.1 'Example 2'
    VRLFrame frame3 = newInstance(testedClass); // Empty frame     (may have 'VEND')
    VRLFrame frame4 = newInstance(testedClass); // Non-Empty frame (may have 'VEND')
    VRLFrame frame5 = newInstance(testedClass); // Empty frame     (not 'VEND')
    VRLFrame frame6 = newInstance(testedClass); // Non-Empty frame (not 'VEND')
    frame1.setFrame(buf1);
    frame2.setFrame(buf2);
    frame4.setVRTPackets(makeTestPacketArray());
    frame6.setVRTPackets(makeTestPacketArray());

    frame5.updateCRC();
    frame6.updateCRC();

    assertEquals("Frame 1 CRC valid", true, frame1.isCRCValid());
    assertEquals("Frame 2 CRC valid", true, frame2.isCRCValid());
    assertEquals("Frame 3 CRC valid", true, frame3.isCRCValid());
    assertEquals("Frame 4 CRC valid", true, frame4.isCRCValid());
    assertEquals("Frame 5 CRC valid", true, frame5.isCRCValid());
    assertEquals("Frame 6 CRC valid", true, frame6.isCRCValid());

    int crc5 = VRTMath.unpackInt(frame5.getFrame(), frame5.getFrameLength()-4);
    int crc6 = VRTMath.unpackInt(frame6.getFrame(), frame6.getFrameLength()-4);

    assertEquals("Frame 5 CRC not 'VEND'", true, (crc5 != VRLFrame.NO_CRC));
    assertEquals("Frame 6 CRC not 'VEND'", true, (crc6 != VRLFrame.NO_CRC));
  }

  public void testIsFrameValid () {
    // ==== TEST WITH VALID FRAMES =============================================
    VRLFrame frame1 = newInstance(testedClass);
    VRLFrame frame2 = newInstance(testedClass);
    frame2.setVRTPackets(makeTestPacketArray());
    int len2 = frame2.getFrameLength();

    assertEquals("isFrameValid() for frame1",            true,   frame1.isFrameValid());
    assertEquals("isFrameValid() for frame2",            true,   frame2.isFrameValid());
    assertEquals("isFrameValid(len2) for frame1",        false,  frame1.isFrameValid(len2));
    assertEquals("isFrameValid(len2) for frame2",        true,   frame2.isFrameValid(len2));
    assertEquals("getFrameValid(false) for frame1",      true,  (frame1.getFrameValid(false)      == null));
    assertEquals("getFrameValid(false) for frame2",      true,  (frame2.getFrameValid(false)      == null));
    assertEquals("getFrameValid(true) for frame1",       true,  (frame1.getFrameValid(true)       == null));
    assertEquals("getFrameValid(true) for frame2",       true,  (frame2.getFrameValid(true)       == null));
    assertEquals("getFrameValid(false,len2) for frame1", false, (frame1.getFrameValid(false,len2) == null));
    assertEquals("getFrameValid(false,len2) for frame2", true,  (frame2.getFrameValid(false,len2) == null));
    assertEquals("getFrameValid(true,len2) for frame1",  false, (frame1.getFrameValid(true,len2)  == null));
    assertEquals("getFrameValid(true,len2) for frame2",  true,  (frame2.getFrameValid(true,len2)  == null));

    // ==== TEST WITH INVALID FRAMES ===========================================
    // Here frame3 will fail all checks, while frame4 will only fail strict checks.
    VRLFrame frame3 = newInstance(testedClass);
    VRLFrame frame4 = newInstance(testedClass);
    byte[] invalidFrame3 = { VRLFrame.VRL_FAW_0, VRLFrame.VRL_FAW_1, VRLFrame.VRL_FAW_2, VRLFrame.VRL_FAW_3,
                             0,                  0,                  0,                  0, // <- Invalid frame size
                             0,                  0,                  0,                  1,
                             VRLFrame.NO_CRC_0,  VRLFrame.NO_CRC_1,  VRLFrame.NO_CRC_2,  VRLFrame.NO_CRC_3 };
    byte[] invalidFrame4 = { VRLFrame.VRL_FAW_0, VRLFrame.VRL_FAW_1, VRLFrame.VRL_FAW_2, VRLFrame.VRL_FAW_3,
                             0,                  0,                  0,                  4,
                             0,                  0,                  0,                  0, // <- Invalid packet size
                             VRLFrame.NO_CRC_0,  VRLFrame.NO_CRC_1,  VRLFrame.NO_CRC_2,  VRLFrame.NO_CRC_3 };
    frame3.setFrame(invalidFrame3);
    frame4.setFrame(invalidFrame4);

    assertEquals("isFrameValid() for frame3",            false,  frame3.isFrameValid());
    assertEquals("isFrameValid() for frame4",            false,  frame4.isFrameValid());
    assertEquals("getFrameValid(false) for frame3",      false, (frame3.getFrameValid(false) == null));
    assertEquals("getFrameValid(false) for frame4",      true,  (frame4.getFrameValid(false) == null));
    assertEquals("getFrameValid(true) for frame3",       false, (frame3.getFrameValid(true)  == null));
    assertEquals("getFrameValid(true) for frame4",       false, (frame4.getFrameValid(true)  == null));
  }

  public void testIsVRL () {
    VRLFrame           frame1  = newInstance(testedClass);
    VRLFrame           frame2  = newInstance(testedClass);
    BasicDataPacket    packet1 = new BasicDataPacket();
    BasicContextPacket packet2 = new BasicContextPacket();
    frame2.setVRTPackets(makeTestPacketArray());

    assertEquals("isFrameValid() for frame1",  true,  BasicVRLFrame.isVRL(frame1.getFrame(), 0));
    assertEquals("isFrameValid() for frame2",  true,  BasicVRLFrame.isVRL(frame2.getFrame(), 0));
    assertEquals("isFrameValid() for packet1", false, BasicVRLFrame.isVRL(packet1.getPacket(), 0));
    assertEquals("isFrameValid() for packet2", false, BasicVRLFrame.isVRL(packet2.getPacket(), 0));
  }

  public void testGetFrameCount () {
    VRLFrame frame1 = newInstance(testedClass);
    VRLFrame frame2 = newInstance(testedClass);
    frame2.setVRTPackets(makeTestPacketArray());

    assertEquals("Frame 1 count", 0, frame1.getFrameCount());
    assertEquals("Frame 2 count", 0, frame2.getFrameCount());

    frame1.setFrameCount(0x42);
    frame2.setFrameCount(0x00000876);

    assertEquals("Frame 1 count",  0x42,       frame1.getFrameCount());
    assertEquals("Frame 2 count",  0x00000876, frame2.getFrameCount());

    assertEquals("Frame 1 header", 0x04200000, VRTMath.unpackInt(frame1.getFrame(),4) & 0xFFF00000);
    assertEquals("Frame 2 header", 0x87600000, VRTMath.unpackInt(frame2.getFrame(),4) & 0xFFF00000);
  }

  public void testReadFrame () throws Exception {
    VRLFrame frame1 = newInstance(testedClass);
    VRLFrame frame2 = newInstance(testedClass);
    frame2.setVRTPackets(makeTestPacketArray());

    byte[] buf1 = new byte[1024];
    byte[] buf2 = new byte[1024];

    int count1 = frame1.readFrame(buf1);
    int count2 = frame2.readFrame(buf2, 128, 64, 256);

    assertEquals(   "Frame 1 count", frame1.getFrameLength(), count1);
    assertEquals(   "Frame 2 count", 256,                     count2);
    assertBufEquals("Frame 1 data",  frame1.getFrame(),  0, count1, buf1,   0, count1);
    assertBufEquals("Frame 2 data",  frame2.getFrame(), 64, count2, buf2, 128, count2);
  }

  public void testSetFrame () throws Exception {
    VRLFrame frame1 = newInstance(testedClass);
    VRLFrame frame2 = newInstance(testedClass);
    frame2.setVRTPackets(makeTestPacketArray());

    VRLFrame f1 = newInstance(testedClass);
    VRLFrame f2 = newInstance(testedClass);
    f1.setFrame(frame1.getFrame(), 0, frame1.getFrameLength());
    f2.setFrame(frame2.getFrame());

    assertEquals("Frame 1", frame1, f1);
    assertEquals("Frame 2", frame2, f2);
  }

  public void testReadFrameFrom () throws Exception {
    VRLFrame frame1 = newInstance(testedClass);
    VRLFrame frame2 = newInstance(testedClass);
    frame2.setVRTPackets(makeTestPacketArray());


    ByteArrayInputStream in1 = new ByteArrayInputStream(frame1.getFrame());
    ByteArrayInputStream in2 = new ByteArrayInputStream(frame2.getFrame());

    VRLFrame f1 = newInstance(testedClass);
    VRLFrame f2 = newInstance(testedClass);
    f1.readFrameFrom(in1);
    f2.readFrameFrom(in2);

    assertEquals("Frame 1", frame1, f1);
    assertEquals("Frame 2", frame2, f2);
  }

  public void testWriteFrameTo () throws Exception {
    ByteArrayOutputStream out1 = new ByteArrayOutputStream(4096);
    ByteArrayOutputStream out2 = new ByteArrayOutputStream(4096);

    VRLFrame frame1 = newInstance(testedClass);
    VRLFrame frame2 = newInstance(testedClass);
    frame2.setVRTPackets(makeTestPacketArray());

    frame1.writeFrameTo(out1);
    frame2.writeFrameTo(out2);

    assertBufEquals("Frame 1", frame1.getFrame(), out1.toByteArray());
    assertBufEquals("Frame 2", frame2.getFrame(), out2.toByteArray());
  }
}
