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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import nxm.vrt.lib.AbstractVRAFile;
import nxm.vrt.lib.BasicContextPacket;
import nxm.vrt.lib.BasicDataPacket;
import nxm.vrt.lib.BasicVRAFile;
import nxm.vrt.lib.BasicVRAFile.FileMode;
import nxm.vrt.lib.PacketIterator;
import nxm.vrt.lib.PacketIterator.PacketContainer;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.VRAFile;
import nxm.vrt.lib.VRLFrame;
import nxm.vrt.lib.VRTMath;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.lib.VRTPacket.PacketType;
import nxm.vrt.lib.VRTPacket.PayloadFormat;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static nxm.vrttest.inc.TestRunner.assertBufEquals;
import static nxm.vrttest.inc.TestRunner.assertEquals;

/** Tests for the {@link VRAFile} class. */
public class VRAFileTest<T extends VRAFile> implements Testable {
  private final Class<T> testedClass;
  private       File     fname1;
  private       File     fname2;

  /** Creates a new instance using the given implementation class. */
  public VRAFileTest (Class<T> testedClass) {
    this.testedClass = testedClass;
  }

  @Override
  public Class<?> getTestedClass () {
    return testedClass;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    fname1 = File.createTempFile("VRAFileTest", VRAFile.FILE_NAME_EXT);
    fname2 = File.createTempFile("VRAFileTest", VRAFile.FILE_NAME_EXT);

    String ok = "-"; // Set to "-" to disable tests if not OK

    if (fname1.exists() && fname2.exists()) {
      ok = ""; // GOOD
      fname1.deleteOnExit();
      fname2.deleteOnExit();
    }

    if (testedClass == VRAFile.class) {
      tests.add(new TestSet("DEFAULT_VERSION",       this,    "testConstants"));
      tests.add(new TestSet("FILE_NAME_EXT",         this,   "+testConstants"));
      tests.add(new TestSet("HEADER_LENGTH",         this,   "+testConstants"));
      tests.add(new TestSet("MAX_VERSION_SUPPORTED", this,   "+testConstants"));
      tests.add(new TestSet("MIME_TYPE",             this,   "+testConstants"));
      tests.add(new TestSet("MIN_VERSION_SUPPORTED", this,   "+testConstants"));
      tests.add(new TestSet("TRAILER_LENGTH",        this,   "+testConstants"));
      tests.add(new TestSet("VRA_FAW",               this,   "+testConstants"));
      tests.add(new TestSet("VRA_FAW_0",             this,   "+testConstants"));
      tests.add(new TestSet("VRA_FAW_1",             this,   "+testConstants"));
      tests.add(new TestSet("VRA_FAW_2",             this,   "+testConstants"));
      tests.add(new TestSet("VRA_FAW_3",             this,   "+testConstants"));
    }
    else {
      tests.add(new TestSet("close()",               this,   "+testAppend"));
      tests.add(new TestSet("append(..)",            this, ok+"testAppend"));
      tests.add(new TestSet("equals(..)",            this,   "+testAppend"));
      tests.add(new TestSet("flush()",               this,   "+testAppend"));
      tests.add(new TestSet("getFileLength()",       this,   "+testAppend"));
      tests.add(new TestSet("getHeader()",           this,   "+testAppend"));
      tests.add(new TestSet("getURI(..)",            this, ok+"testGetURI"));
      tests.add(new TestSet("getVersion()",          this, ok+"testGetVersion"));
      tests.add(new TestSet("hashCode()",            this,   "+testAppend"));
      tests.add(new TestSet("iterator()",            this,   "+testAppend"));
      tests.add(new TestSet("isCRCValid()",          this,   "+testAppend"));
      tests.add(new TestSet("isFileValid(..)",       this,   "+testAppend"));
      tests.add(new TestSet("setVersion(..)",        this,   "+testGetVersion"));
      tests.add(new TestSet("toString()",            this, ok+"testToString"));
      tests.add(new TestSet("updateFileLength()",    this,   "+testAppend"));
      tests.add(new TestSet("updateCRC()",           this,   "+testAppend"));
      if (PacketContainer.class.isAssignableFrom(testedClass)) {
        tests.add(new TestSet("getNextPacket(..)",   this,  "testGetNextPacket"));
        tests.add(new TestSet("hasNextPacket(..)",   this, "+testGetNextPacket"));
        tests.add(new TestSet("removeNextPacket(..)",this, "+testGetNextPacket"));
      }
    }

    return tests;
  }

  @Override
  public void done () throws Exception {
    if (fname1.exists()) fname1.delete();
    if (fname2.exists()) fname2.delete();
    fname1 = null;
    fname2 = null;
  }

  @Override
  public String toString () {
    return "Tests for "+getTestedClass();
  }

  /** Opens a file using an instance of the tested class. */
  private VRAFile openFile (File fname, FileMode mode) {
    return openFile(fname, mode, false, false, false);
  }

  /** Opens a file using an instance of the tested class. */
  private VRAFile openFile (File fname, FileMode mode, boolean isSetSize, boolean isSetCRC, boolean isStrict) {
    if (testedClass == BasicVRAFile.class) {
      return new BasicVRAFile(fname, mode, isSetSize, isSetCRC, isStrict);
    }
    else if (testedClass == AbstractVRAFile.class) {
      // Can't instantiate abstract class, so use commonly-used sub-class
      return new BasicVRAFile(fname, mode, isSetSize, isSetCRC, isStrict);
    }
    else {
      throw new AssertionError("Unsupported test class "+testedClass);
    }
  }

  /** <b>Internal Use Only:</b> Creates a set of test packets to use. */
  public static List<VRTPacket> makeTestPacketList () { // Used by VRAFileTest and VRLFrameTest
    PayloadFormat pf    = VRTPacket.INT32;
    TimeStamp     time0 = TimeStamp.getSystemTime();
    int[]         data0 = new int[128];
    int[]         data1 = new int[128];  for (int i = 0; i < 128; i++) data1[i] = i;
    int[]         data2 = new int[512];  for (int i = 0; i < 512; i++) data2[i] = ~i;

    BasicDataPacket p0 = new BasicDataPacket();
    p0.setPacketType(PacketType.Data);
    p0.setStreamID("42");
    p0.setClassID("AB-CD-EF:0123.4567");
    p0.setTimeStamp(time0);
    p0.setDataInt(pf, data0);

    BasicDataPacket p1 = p0.copy();
    p1.setDataInt(pf, data1);
    p1.resetForResend(time0.addSeconds(1));

    BasicDataPacket p2 = p0.copy();
    p2.setDataInt(pf, data2);
    p2.resetForResend(time0.addSeconds(2));

    BasicContextPacket c0 = new BasicContextPacket();
    c0.setPacketType(PacketType.Context);
    c0.setStreamID("42");
    c0.setClassID("AB-CD-EF:9876.5432");
    c0.setTimeStamp(time0);
    c0.setDataPayloadFormat(pf);
    c0.setTemperature(27.0f);

    BasicContextPacket c1 = c0.copy();
    c0.setTemperature(28.0f);
    c0.resetForResend(time0.addSeconds(2));

    List<VRTPacket> packets = new ArrayList<VRTPacket>();
    packets.add(p0);
    packets.add(c0);
    packets.add(p1);
    packets.add(p2);
    packets.add(c1);

    return packets;
  }

  /** <b>Internal Use Only:</b> Creates a set of test packets to use. */
  public static VRTPacket[] makeTestPacketArray () { // Used by VRAFileTest and VRLFrameTest
    List<VRTPacket> packets = makeTestPacketList();
    return packets.toArray(new VRTPacket[packets.size()]);
  }

  public void testConstants () throws Exception {
    int expFAW = ((VRAFile.VRA_FAW_0 & 0xFF) << 24)
               | ((VRAFile.VRA_FAW_1 & 0xFF) << 16)
               | ((VRAFile.VRA_FAW_2 & 0xFF) <<  8)
               | ((VRAFile.VRA_FAW_3 & 0xFF)      );

    // SHOULD BE CONSTANT
    assertEquals("FILE_NAME_EXT",         ".vra",                             VRAFile.FILE_NAME_EXT);
    assertEquals("MIME_TYPE",             "application/x-vita-radio-archive", VRAFile.MIME_TYPE);
    assertEquals("HEADER_LENGTH",         20,                                 VRAFile.HEADER_LENGTH);
    assertEquals("TRAILER_LENGTH",        0,                                  VRAFile.TRAILER_LENGTH);
    assertEquals("VRA_FAW",               expFAW,                             VRAFile.VRA_FAW);
    assertEquals("VRA_FAW_0",             (byte)'V',                          VRAFile.VRA_FAW_0);
    assertEquals("VRA_FAW_1",             (byte)'R',                          VRAFile.VRA_FAW_1);
    assertEquals("VRA_FAW_2",             (byte)'A',                          VRAFile.VRA_FAW_2);
    assertEquals("VRA_FAW_3",             (byte)'F',                          VRAFile.VRA_FAW_3);

    // MAY CHANGE IN FUTURE
    assertEquals("MAX_VERSION_SUPPORTED", 1, VRAFile.MAX_VERSION_SUPPORTED);
    assertEquals("MIN_VERSION_SUPPORTED", 1, VRAFile.MIN_VERSION_SUPPORTED);
    assertEquals("DEFAULT_VERSION",       1, VRAFile.DEFAULT_VERSION);

    // SANITY CHECKS
    assertEquals("DEFAULT_VERSION >= MIN_VERSION_SUPPORTED", true, VRAFile.DEFAULT_VERSION >= VRAFile.MIN_VERSION_SUPPORTED);
    assertEquals("DEFAULT_VERSION <= MAX_VERSION_SUPPORTED", true, VRAFile.DEFAULT_VERSION <= VRAFile.MAX_VERSION_SUPPORTED);
  }

  public void testAppend () throws Exception {
    // ==== CREATE TEST DATA ===================================================
    List<VRTPacket> packets = makeTestPacketList();

    // ==== DECLARE FILES ======================================================
    VRAFile out1 = openFile(fname1, FileMode.Write);
    VRAFile out2 = openFile(fname2, FileMode.Write, true, true, true);
    VRAFile in1  = null;
    VRAFile in1b = null;
    VRAFile in2  = null;

    try {
      // ==== WRITE OUT FILES ==================================================
      // ---- Check Writing Packets --------------------------------------------
      long len = VRAFile.HEADER_LENGTH;
      assertEquals("File 1 initial length", len, out1.getFileLength());
      assertEquals("File 2 initial length", len, out2.getFileLength());
      for (int i = 0; i < packets.size(); i++) {
        out1.append(packets.get(i));
        out2.append(packets.get(i));
        len += packets.get(i).getPacketLength();

        assertEquals("File 1 length after writing packet", len, out1.getFileLength());
        assertEquals("File 2 length after writing packet", len, out2.getFileLength());

        out1.close();
        out1 = openFile(fname1, FileMode.ReadWrite);
        assertEquals("File 1 length after ReadWrite open", len, out1.getFileLength());
      }

      // ---- Check Flush/Update -----------------------------------------------
      out1.flush();
      out2.flush();
      assertEquals("File 1 length after flush",  len,  out1.getFileLength());
      assertEquals("File 2 length after flush",  len,  out2.getFileLength());
      assertEquals("File 1 CRC after flush",     true, out1.isCRCValid());
      assertEquals("File 2 CRC after flush",     true, out2.isCRCValid());
      out1.updateCRC();
      assertEquals("File 1 CRC after update",    true, out1.isCRCValid());
      assertEquals("File 2 CRC after update",    true, out2.isCRCValid());
      out1.updateFileLength();
      assertEquals("File 1 length after update", len,  out1.getFileLength());
      assertEquals("File 2 length after update", len,  out2.getFileLength());

      // ---- Check Output Header ----------------------------------------------
      byte[] header1    = out1.getHeader();
      byte[] header2    = out2.getHeader();
      int    expVerWord = ((int)VRAFile.DEFAULT_VERSION) << 24;
      //int    expCRC     = 0;

      assertEquals("Header 1 / Length",   VRAFile.HEADER_LENGTH, header1.length);
      assertEquals("Header 2 / Length",   VRAFile.HEADER_LENGTH, header2.length);
      assertEquals("Header 1 / Word 0",   VRAFile.VRA_FAW,       VRTMath.unpackInt( header1, 0));
      assertEquals("Header 2 / Word 0",   VRAFile.VRA_FAW,       VRTMath.unpackInt( header2, 0));
      assertEquals("Header 1 / Word 1",   expVerWord,            VRTMath.unpackInt( header1, 4));
      assertEquals("Header 2 / Word 1",   expVerWord,            VRTMath.unpackInt( header2, 4));
      assertEquals("Header 1 / Word 2-3", 0L,                    VRTMath.unpackLong(header1, 8));
      assertEquals("Header 2 / Word 2-3", len,                   VRTMath.unpackLong(header2, 8));
      assertEquals("Header 1 / Word 4",   VRLFrame.NO_CRC,       VRTMath.unpackInt( header1,16));
      //assertEquals("Header 2 / Word 4",   expCRC,                VRTMath.unpackInt( header2,16));

      out1.close();
      out2.close();

      // ==== READ IN FILES ====================================================
      in1  = openFile(fname1, FileMode.Read);
      in1b = openFile(fname1, FileMode.Read);
      in2  = openFile(fname2, FileMode.Read);

      // ---- Check Input Header -----------------------------------------------
      assertBufEquals("Input 1 Header",     header1, in1.getHeader());
      assertBufEquals("Input 2 Header",     header2, in2.getHeader());
      assertEquals(   "Input 1 Length",     len,     in1.getFileLength());
      assertEquals(   "Input 2 Length",     len,     in2.getFileLength());
      assertEquals(   "Input 1 CRC Valid",  true,    in1.isCRCValid());
      assertEquals(   "Input 2 CRC Valid",  true,    in2.isCRCValid());
      assertEquals(   "Input 1 File Valid", true,    in1.isFileValid());
      assertEquals(   "Input 2 File Valid", true,    in2.isFileValid());

      // ---- Check Input Packets ----------------------------------------------
      int i = 0;
      for (VRTPacket p : in1) { // implicit call to .iterator()
        assertEquals("Input 1 Packets", packets.get(i), p);
        i++;
      }

      i = 0;
      for (VRTPacket p : in2) { // explicit call to .iterator()
        assertEquals("Input 2 Packets", packets.get(i), p);
        i++;
      }

      // ---- Check Input Equality ---------------------------------------------
      assertEquals("Input 1 == Input 1",  true,  in1.equals(in1 ));
      assertEquals("Input 2 == Input 2",  true,  in2.equals(in2 ));
      assertEquals("Input 1 != Input 2",  true, !in1.equals(in2 )); // different CRC/len in header
      assertEquals("Input 1 == Input 1b", true,  in1.equals(in1b)); // same underlying file

      assertEquals("Input 1 Hash == Input 1b Hash", in1.hashCode(), in1b.hashCode());
    }
    finally {
      if (out1 != null) out1.close();
      if (out2 != null) out2.close();
      if (in1  != null)  in1.close();
      if (in2  != null)  in2.close();
      if (in1b != null) in1b.close();
    }
  }

  public void testGetURI () throws Exception {
    VRAFile file1 = openFile(fname1, FileMode.Write);
    VRAFile file2 = openFile(fname2, FileMode.Write);

    try {
      assertEquals("File 1 URI", fname1.toURI(), file1.getURI());
      assertEquals("File 2 URI", fname2.toURI(), file2.getURI());
    }
    finally {
      file1.close();
      file2.close();
    }
  }

  public void testGetNextPacket () throws Exception {
    VRAFile file1 = openFile(fname1, FileMode.Write);
    VRAFile file2 = openFile(fname2, FileMode.Write);

    try {
      List<VRTPacket> packets = makeTestPacketList();
      for (int i = 0; i < packets.size(); i++) {
        file1.append(packets.get(i));
      }

      PacketContainer     pc1 = (PacketContainer)file1;
      PacketContainer     pc2 = (PacketContainer)file2;
      PacketIterator      pi1 = (PacketIterator)file1.iterator();
      PacketIterator      pi2 = (PacketIterator)file2.iterator();
      Iterator<VRTPacket> it1 = file1.iterator();
      Iterator<VRTPacket> it2 = file2.iterator();

      while (it1.hasNext() && pc1.hasNextPacket(pi1)) {
        assertEquals("File 1 iterator pkt = PacketIterator pkt", it1.next(), pc1.getNextPacket(pi1,false));
      }
      while (it2.hasNext() && pc2.hasNextPacket(pi2)) {
        assertEquals("File 2 iterator pkt = PacketIterator pkt", it2.next(), pc2.getNextPacket(pi2,false));
      }

      assertEquals("File 1 Iterator lengths", it1.hasNext(), pc1.hasNextPacket(pi1));
      assertEquals("File 2 Iterator lengths", it2.hasNext(), pc2.hasNextPacket(pi2));

      if (testedClass == BasicVRAFile.class) {
        PacketIterator piRW = (PacketIterator)file2.iterator();
        try {
          pc2.removePrevPacket(piRW);
          assertEquals("BasicVRAFile does not support removal from iterator", true, false);
        }
        catch (Exception e) {
          assertEquals("BasicVRAFile does not support removal from iterator", true, true);
        }
      }
    }
    finally {
      file1.close();
      file2.close();
    }
  }

  public void testGetVersion () throws Exception {
    VRAFile file1 = openFile(fname1, FileMode.Write);
    VRAFile file2 = openFile(fname2, FileMode.Write);

    try {
      assertEquals("File 1 default version", VRAFile.DEFAULT_VERSION, file1.getVersion());
      assertEquals("File 2 default version", VRAFile.DEFAULT_VERSION, file2.getVersion());

      // SET FILE 1 TO VALID VERSIONS
      for (int i = VRAFile.MIN_VERSION_SUPPORTED; i <= VRAFile.MAX_VERSION_SUPPORTED; i++) {
        file1.setVersion(i);
        assertEquals("File 1 setVersion(..)", i, file1.getVersion());
      }

      // SET FILE 2 TO INVALID VERSIONS (MAKE SURE IT DOESN"T CHANGE)
      for (int i = 0; i < VRAFile.MIN_VERSION_SUPPORTED; i++) {
        try {
          file2.setVersion(i);
          assertEquals("File 2 setVersion(..) with invalid version", false, true);
        }
        catch (Exception e) {
          assertEquals("File 2 setVersion(..) with invalid version", true, true);
        }
      }
      for (int i = VRAFile.MAX_VERSION_SUPPORTED; i < VRAFile.MAX_VERSION_SUPPORTED+10; i++) {
        try {
          file2.setVersion(i);
          assertEquals("File 2 setVersion(..) with invalid version", false, true);
        }
        catch (Exception e) {
          assertEquals("File 2 setVersion(..) with invalid version", true, true);
        }
      }
      assertEquals("File 2 version unmodified", VRAFile.DEFAULT_VERSION, file2.getVersion());
    }
    finally {
      file1.close();
      file2.close();
    }
  }

  public void testToString () throws Exception {
    VRAFile file1 = openFile(fname1, FileMode.Write);
    VRAFile file2 = openFile(fname2, FileMode.Write);

    for (VRTPacket p : makeTestPacketList()) {
      file2.append(p);
    }

    try {
      // Output string is free-form, so just make sure call works and doesn't return null
      assertEquals("File 1 toString() != null", true, (file1.toString() != null));
      assertEquals("File 2 toString() != null", true, (file2.toString() != null));
    }
    finally {
      file1.close();
      file2.close();
    }
  }
}
