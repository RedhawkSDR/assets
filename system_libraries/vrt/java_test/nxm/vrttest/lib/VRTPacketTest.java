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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nxm.vrt.lib.BasicContextPacket;
import nxm.vrt.lib.BasicDataPacket;
import nxm.vrt.lib.BasicVRTPacket;
import nxm.vrt.lib.HasFields;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.TimeStamp.FractionalMode;
import nxm.vrt.lib.TimeStamp.IntegerMode;
import nxm.vrt.lib.VRTConfig;
import nxm.vrt.lib.VRTMath;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.lib.VRTPacket.DataItemFormat;
import nxm.vrt.lib.VRTPacket.DataType;
import nxm.vrt.lib.VRTPacket.PacketType;
import nxm.vrt.lib.VRTPacket.PayloadFormat;
import nxm.vrt.lib.VRTPacket.RealComplexType;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static nxm.vrttest.inc.TestRunner.assertBufEquals;
import static nxm.vrttest.inc.TestRunner.assertEquals;
import static nxm.vrttest.inc.TestUtilities.newInstance;
import static nxm.vrttest.lib.HasFieldsTest.checkGetField;
import static nxm.vrttest.lib.HasFieldsTest.checkGetFieldName;

/** Test cases for {@link VRTPacket} */
public class VRTPacketTest<T extends VRTPacket> implements Testable {
  private final Class<T> testedClass;

  /** Creates a new instance using the given implementation class. */
  public VRTPacketTest (Class<T> testedClass) {
    this.testedClass = testedClass;
  }

  @Override
  public Class<?> getTestedClass () {
    return testedClass;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    tests.add(new TestSet("copy(..)",                     this, "+testEquals"));
    tests.add(new TestSet("equals(..)",                   this, "testEquals"));
    tests.add(new TestSet("getClassID()",                 this, "testGetClassID"));
    tests.add(new TestSet("getClassIdentifier()",         this, "+testGetClassID"));
    tests.add(new TestSet("getClassIdentifierICC()",      this, "+testGetClassID"));
    tests.add(new TestSet("getClassIdentifierOUI()",      this, "+testGetClassID"));
    tests.add(new TestSet("getClassIdentifierPCC()",      this, "+testGetClassID"));
    tests.add(new TestSet("getHeaderLength()",            this, "+testGetPacketLength"));
    tests.add(new TestSet("getPacket()",                  this, "testGetPacket"));
    tests.add(new TestSet("getPacketCount()",             this, "testGetPacketCount"));
    tests.add(new TestSet("getPacketLength()",            this, "testGetPacketLength"));
    tests.add(new TestSet("getPacketType()",              this, "testGetPacketType"));
    tests.add(new TestSet("getPacketValid(..)",           this, "testGetPacketValid"));

    if (VRTConfig.VRT_VERSION == VRTConfig.VITAVersion.V49) {
      tests.add(new TestSet("getPadBitCount(..) VRT_VERSION=V49",  this, "testGetPadBitCount"));
      tests.add(new TestSet("getPadBitCount(..) VRT_VERSION=V49b", this, "-testGetPadBitCount"));
    }
    else {
      tests.add(new TestSet("getPadBitCount(..) VRT_VERSION=V49",  this, "-testGetPadBitCount"));
      tests.add(new TestSet("getPadBitCount(..) VRT_VERSION=V49b", this, "testGetPadBitCount"));
    }

    tests.add(new TestSet("getPayload()",                 this, "+testGetPacket"));
    tests.add(new TestSet("getPayloadLength()",           this, "+testGetPacketLength"));
    tests.add(new TestSet("getStreamCode()",              this, "testGetStreamCode"));
    tests.add(new TestSet("getStreamID()",                this, "testGetStreamID"));
    tests.add(new TestSet("getStreamIdentifier()",        this, "+testGetStreamID"));
    tests.add(new TestSet("getTimeStamp()",               this, "testGetTimeStamp"));
    tests.add(new TestSet("getTrailerLength()",           this, "+testGetPacketLength"));
    tests.add(new TestSet("hashCode()",                   this, "testHashCode"));
    tests.add(new TestSet("headerEquals(..)",             this, "+testEquals"));
    tests.add(new TestSet("isChangePacket()",             this, "testIsChangePacket"));
    tests.add(new TestSet("isPacketValid(..)",            this, "+testGetPacketValid"));
    tests.add(new TestSet("isTimeStampMode()",            this, "testIsTimeStampMode"));
    tests.add(new TestSet("payloadEquals(..)",            this, "+testEquals"));
    tests.add(new TestSet("readPacket(..)",               this, "testReadPacket"));
    tests.add(new TestSet("readPacketFrom(..)",           this, "testReadPacketFrom"));
    tests.add(new TestSet("readPayload(..)",              this, "+testReadPacket"));
    tests.add(new TestSet("resetForResend(..)",           this, "testResetForResend"));
    tests.add(new TestSet("setClassID(..)",               this, "+testGetClassID"));
    tests.add(new TestSet("setClassIdentifier(..)",       this, "+testGetClassID"));
    tests.add(new TestSet("setPacketCount(..)",           this, "+testGetPacketCount"));
    tests.add(new TestSet("setPacketType(..)",            this, "+testGetPacketType"));
    tests.add(new TestSet("setPadBitCount(..)",           this, "+testGetPadBitCount"));
    tests.add(new TestSet("setPayload(..)",               this, "+testGetPacket"));
    tests.add(new TestSet("setPayloadLength(..)",         this, "+testGetPacketLength"));
    tests.add(new TestSet("setStreamID(..)",              this, "+testGetStreamID"));
    tests.add(new TestSet("setStreamIdentifier(..)",      this, "+testGetStreamID"));
    tests.add(new TestSet("setTimeStamp(..)",             this, "+testGetTimeStamp"));
    tests.add(new TestSet("setTimeStampMode(..)",         this, "+testIsTimeStampMode"));
    tests.add(new TestSet("toBasicVRTPacket()",           this, "testToBasicVRTPacket"));
    tests.add(new TestSet("toString()",                   this, "testToString"));
    tests.add(new TestSet("trailerEquals(..)",            this, "+testEquals"));
    tests.add(new TestSet("writePacketTo(..)",            this, "testWritePacketTo"));
    tests.add(new TestSet("writePayload(..)",             this, "testWritePayload"));

    if (HasFields.class.isAssignableFrom(testedClass)) {
      tests.add(new TestSet("getField(..)",                 this,  "testGetField"));
      tests.add(new TestSet("getFieldByName(..)",           this, "+testGetField"));
      tests.add(new TestSet("getFieldCount()",              this, "+testGetFieldName"));
      tests.add(new TestSet("getFieldID(..)",               this, "+testGetFieldName"));
      tests.add(new TestSet("getFieldName(..)",             this,  "testGetFieldName"));
      tests.add(new TestSet("getFieldType(..)",             this, "+testGetFieldName"));
      tests.add(new TestSet("setField(..)",                 this, "+testGetField"));
      tests.add(new TestSet("setFieldByName(..)",           this, "+testGetField"));
    }
    return tests;
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }



  public void testEquals () {
    byte[]          data18   = { 1,2,3,4,5,6,7,8 };
    byte[]          data81   = { 8,7,6,5,4,3,2,1 };
    BasicDataPacket givenPkt = new BasicDataPacket();  // Initial packet
    BasicDataPacket cpyEqual;                          // Copy of packet
    BasicVRTPacket  bufEqual;                          // Copy of packet buffer
    BasicDataPacket allEqual = new BasicDataPacket();  // All fields equal
    BasicDataPacket hdrEqual = new BasicDataPacket();  // Header  is equal
    BasicDataPacket payEqual = new BasicDataPacket();  // Payload is equal
    BasicDataPacket trlEqual = new BasicDataPacket();  // Trailer is equal

    TimeStamp ts2000 = TimeStamp.parseTime("2000-01-01T00:00:00Z", TimeStamp.IntegerMode.GPS);
    TimeStamp ts2013 = TimeStamp.parseTime("2013-01-01T00:00:00Z", TimeStamp.IntegerMode.GPS);

    givenPkt.setTimeStamp(ts2013);
    allEqual.setTimeStamp(ts2013);
    hdrEqual.setTimeStamp(ts2013);
    payEqual.setTimeStamp(ts2000);
    trlEqual.setTimeStamp(ts2000);

    givenPkt.setData(VRTPacket.INT8, data18, 0, data18.length);
    allEqual.setData(VRTPacket.INT8, data18, 0, data18.length);
    hdrEqual.setData(VRTPacket.INT8, data81, 0, data81.length);
    payEqual.setData(VRTPacket.INT8, data18, 0, data18.length);
    trlEqual.setData(VRTPacket.INT8, data81, 0, data81.length);

    givenPkt.setCalibratedTimeStamp(true );
    allEqual.setCalibratedTimeStamp(true );
    hdrEqual.setCalibratedTimeStamp(false);
    payEqual.setCalibratedTimeStamp(false);
    trlEqual.setCalibratedTimeStamp(true );

    cpyEqual = givenPkt.copy();
    bufEqual = new BasicVRTPacket(givenPkt, true);

    assertEquals("equals(..) for givenPkt", true , givenPkt.equals(givenPkt));
    assertEquals("equals(..) for cpyEqual", true , givenPkt.equals(cpyEqual));
    assertEquals("equals(..) for bufEqual", true , givenPkt.equals(bufEqual));
    assertEquals("equals(..) for allEqual", true , givenPkt.equals(allEqual));
    assertEquals("equals(..) for hdrEqual", false, givenPkt.equals(hdrEqual));
    assertEquals("equals(..) for payEqual", false, givenPkt.equals(payEqual));
    assertEquals("equals(..) for trlEqual", false, givenPkt.equals(trlEqual));

    assertEquals("headerEquals(..) for givenPkt", true , givenPkt.headerEquals(givenPkt));
    assertEquals("headerEquals(..) for cpyEqual", true , givenPkt.headerEquals(cpyEqual));
    assertEquals("headerEquals(..) for bufEqual", true , givenPkt.headerEquals(bufEqual));
    assertEquals("headerEquals(..) for allEqual", true , givenPkt.headerEquals(allEqual));
    assertEquals("headerEquals(..) for hdrEqual", true , givenPkt.headerEquals(hdrEqual));
    assertEquals("headerEquals(..) for payEqual", false, givenPkt.headerEquals(payEqual));
    assertEquals("headerEquals(..) for trlEqual", false, givenPkt.headerEquals(trlEqual));

    assertEquals("payloadEquals(..) for givenPkt", true , givenPkt.payloadEquals(givenPkt));
    assertEquals("payloadEquals(..) for cpyEqual", true , givenPkt.payloadEquals(cpyEqual));
    assertEquals("payloadEquals(..) for bufEqual", true , givenPkt.payloadEquals(bufEqual));
    assertEquals("payloadEquals(..) for allEqual", true , givenPkt.payloadEquals(allEqual));
    assertEquals("payloadEquals(..) for hdrEqual", false, givenPkt.payloadEquals(hdrEqual));
    assertEquals("payloadEquals(..) for payEqual", true , givenPkt.payloadEquals(payEqual));
    assertEquals("payloadEquals(..) for trlEqual", false, givenPkt.payloadEquals(trlEqual));

    assertEquals("trailerEquals(..) for givenPkt", true , givenPkt.trailerEquals(givenPkt));
    assertEquals("trailerEquals(..) for cpyEqual", true , givenPkt.trailerEquals(cpyEqual));
    assertEquals("trailerEquals(..) for bufEqual", true , givenPkt.trailerEquals(bufEqual));
    assertEquals("trailerEquals(..) for allEqual", true , givenPkt.trailerEquals(allEqual));
    assertEquals("trailerEquals(..) for hdrEqual", false, givenPkt.trailerEquals(hdrEqual));
    assertEquals("trailerEquals(..) for payEqual", false, givenPkt.trailerEquals(payEqual));
    assertEquals("trailerEquals(..) for trlEqual", true , givenPkt.trailerEquals(trlEqual));

    // ==== TRAILER EXACT -VS- INEXACT =========================================
    byte[] b0 = { 0x04, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00 };
    byte[] b1 = { 0x04, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00 };
    byte[] b2 = { 0x04, 0x00, 0x00, 0x02, 0x00, 0x00, 0x0F, 0x00 }; // indicators set but not enabled
    byte[] b3 = { 0x00, 0x00, 0x00, 0x01                         }; // no payload

    BasicVRTPacket p0 = new BasicVRTPacket(b0, 0, 8, true, true);  // Initial packet
    BasicVRTPacket p1 = new BasicVRTPacket(b1, 0, 8, true, true);  // Trailer with exact equality
    BasicVRTPacket p2 = new BasicVRTPacket(b2, 0, 8, true, true);  // Trailer with inexact equality
    BasicVRTPacket p3 = new BasicVRTPacket(b3, 0, 4, true, true);  // Trailer with inexact equality

    assertEquals("trailerEquals(p1)",       true,  p0.trailerEquals(p1      ));
    assertEquals("trailerEquals(p2)",       false, p0.trailerEquals(p2      ));
    assertEquals("trailerEquals(p3)",       false, p0.trailerEquals(p3      ));
    assertEquals("trailerEquals(p1,false)", true,  p0.trailerEquals(p1,false));
    assertEquals("trailerEquals(p2,false)", true,  p0.trailerEquals(p2,false));
    assertEquals("trailerEquals(p3,false)", true,  p0.trailerEquals(p3,false));
    assertEquals("trailerEquals(p1,true)",  true,  p0.trailerEquals(p1,true ));
    assertEquals("trailerEquals(p2,true)",  false, p0.trailerEquals(p2,true ));
    assertEquals("trailerEquals(p3,true)",  false, p0.trailerEquals(p3,true ));

    // ==== STORAGE METHODS ====================================================
    // Java Only: Make sure there isn't anything particularly special about
    // storing everything as a single buffer vs three buffers.
    byte[] buf = { 0x04,0x00,0x00,0x03, 0x12,0x34,0x56,0x78, 0x00,0x00,0x00,0x00 };
    byte[] hdr = Arrays.copyOfRange(buf, 0,  4);
    byte[] pay = Arrays.copyOfRange(buf, 4,  8);
    byte[] tlr = Arrays.copyOfRange(buf, 8, 12);

    BasicVRTPacket pA = new BasicVRTPacket(buf, 0, 12, true, true);
    BasicVRTPacket pB = new BasicVRTPacket(buf, 0,  4,
                                           buf, 4,  4,
                                           buf, 8,  4, true, true);
    BasicVRTPacket pC = new BasicVRTPacket(hdr, 0,  4,
                                           pay, 0,  4,
                                           tlr, 0,  4, true, true);
    assertEquals("pA.equals(pB)", true,  pA.equals(pB));
    assertEquals("pA.equals(pC)", true,  pA.equals(pC));
  }

  public void testGetClassID () {
    VRTPacket p1 = newInstance(testedClass);
    VRTPacket p2 = newInstance(testedClass);
    VRTPacket p3 = newInstance(testedClass);
    VRTPacket p4 = newInstance(testedClass);
    VRTPacket p5 = newInstance(testedClass);
    p1.setStreamIdentifier(42);
    p2.setStreamIdentifier(42);
    p3.setStreamIdentifier(42);
    p4.setStreamIdentifier(42);
    p5.setStreamIdentifier(42);
    p1.setClassID("AB-CD-EF:1234.5678");
    p2.setClassIdentifier(0xABCDEF12345678L);
    p3.setClassIdentifier(0xABCDEF, (short)0x1234, (short)0x5678);
    p4.setClassID(null);
    p5.setClassIdentifier(null);

    byte[] exp  = { (byte)0x00, (byte)0xAB, (byte)0xCD, (byte)0xEF,
                    (byte)0x12, (byte)0x34, (byte)0x56, (byte)0x78 };
    byte[] act1 = p1.getPacket();
    byte[] act2 = p2.getPacket();
    byte[] act3 = p3.getPacket();

    assertBufEquals("setClassID(String)",                  exp, 0, exp.length, act1, 8, exp.length);
    assertBufEquals("setClassIdentifier(long)",            exp, 0, exp.length, act2, 8, exp.length);
    assertBufEquals("setClassIdentifier(int,short,short)", exp, 0, exp.length, act3, 8, exp.length);

    assertEquals("p1.getClassID()",            "AB-CD-EF:1234.5678", p1.getClassID());
    assertEquals("p1.getClassIdentifier()",    0xABCDEF12345678L,    p1.getClassIdentifier());
    assertEquals("p1.getClassIdentifierOUI()",          0xABCDEF,    p1.getClassIdentifierOUI());
    assertEquals("p1.getClassIdentifierICC()", (short  )0x1234,      p1.getClassIdentifierICC());
    assertEquals("p1.getClassIdentifierPCC()", (short  )0x5678,      p1.getClassIdentifierPCC());

    assertEquals("p2.getClassID()",            "AB-CD-EF:1234.5678", p2.getClassID());
    assertEquals("p2.getClassIdentifier()",    0xABCDEF12345678L,    p2.getClassIdentifier());
    assertEquals("p2.getClassIdentifierOUI()",          0xABCDEF,    p2.getClassIdentifierOUI());
    assertEquals("p2.getClassIdentifierICC()", (short  )0x1234,      p2.getClassIdentifierICC());
    assertEquals("p2.getClassIdentifierPCC()", (short  )0x5678,      p2.getClassIdentifierPCC());

    assertEquals("p3.getClassID()",            "AB-CD-EF:1234.5678", p3.getClassID());
    assertEquals("p3.getClassIdentifier()",    0xABCDEF12345678L,    p3.getClassIdentifier());
    assertEquals("p3.getClassIdentifierOUI()",          0xABCDEF,    p3.getClassIdentifierOUI());
    assertEquals("p3.getClassIdentifierICC()", (short  )0x1234,      p3.getClassIdentifierICC());
    assertEquals("p3.getClassIdentifierPCC()", (short  )0x5678,      p3.getClassIdentifierPCC());

    assertEquals("p4.getClassID()",            null, p4.getClassID());
    assertEquals("p4.getClassIdentifier()",    null, p4.getClassIdentifier());
    assertEquals("p4.getClassIdentifierOUI()", null, p4.getClassIdentifierOUI());
    assertEquals("p4.getClassIdentifierICC()", null, p4.getClassIdentifierICC());
    assertEquals("p4.getClassIdentifierPCC()", null, p4.getClassIdentifierPCC());

    assertEquals("p5.getClassID()",            null, p5.getClassID());
    assertEquals("p5.getClassIdentifier()",    null, p5.getClassIdentifier());
    assertEquals("p5.getClassIdentifierOUI()", null, p5.getClassIdentifierOUI());
    assertEquals("p5.getClassIdentifierICC()", null, p5.getClassIdentifierICC());
    assertEquals("p5.getClassIdentifierPCC()", null, p5.getClassIdentifierPCC());


    // ==== Test Case Added for VRT-30 =========================================
    byte[] buff = new byte[72];
    buff[0]=(byte)0x41;
    buff[1]=(byte)0xe0;
    buff[2]=(byte)0x00;
    buff[3]=(byte)0x12;
    for (int i=4;i<9;++i) buff[i]=(byte)0x00;
    buff[9]=(byte)0x7;
    buff[10]=(byte)0xeb;
    buff[11]=(byte)0x8c;
    buff[12]=(byte)0;
    buff[13]=(byte)0;
    buff[14]=(byte)0;
    buff[15]=(byte)0x61;
    buff[16]=(byte)0xa1;
    buff[17]=(byte)0x93;
    buff[18]=(byte)0x68;
    buff[19]=(byte)0x60;
    buff[20]=(byte)0x1c;
    buff[21]=(byte)0xe0;
    buff[22]=(byte)0x80;
    for (int i=23;i<34;++i) buff[i]=(byte)0;
    buff[34]=(byte)98;
    buff[35]=(byte)96;
    buff[36]=(byte)80;
    for (int i=37;i<48;++i) buff[i]=(byte)0;
    buff[48]=(byte)0xfc;
    buff[49]=(byte)0xc0;
    for (int i=50;i<58;++i) buff[i]=(byte)0;
    buff[58]=(byte)0x17;
    buff[59]=(byte)0xd7;
    buff[60]=(byte)0x84;
    buff[61]=(byte)0;
    buff[62]=(byte)0;
    buff[63]=(byte)0;
    buff[64]=(byte)0xa0;
    buff[65]=(byte)0;
    buff[66]=(byte)0x3;
    buff[67]=(byte)0xcf;
    buff[68]=(byte)0;
    buff[69]=(byte)0;
    buff[70]=(byte)0;
    buff[71]=(byte)0;

    BasicContextPacket context = new BasicContextPacket(buff, 0, buff.length, false, true);
    assertEquals("context.getClassID()",            null, context.getClassID());
    assertEquals("context.getClassIdentifier()",    null, context.getClassIdentifier());
    assertEquals("context.getClassIdentifierOUI()", null, context.getClassIdentifierOUI());
    assertEquals("context.getClassIdentifierICC()", null, context.getClassIdentifierICC());
    assertEquals("context.getClassIdentifierPCC()", null, context.getClassIdentifierPCC());
  }

  public void testGetPacket () {
    byte[] buf = { 0x04,0x00,0x00,0x03, 0x12,0x34,0x56,0x78, 0x00,0x00,0x00,0x00 };
    byte[] hdr = Arrays.copyOfRange(buf, 0,  4);
    byte[] pay = Arrays.copyOfRange(buf, 4,  8);
    byte[] tlr = Arrays.copyOfRange(buf, 8, 12);
    byte[] act;

    BasicVRTPacket pA = new BasicVRTPacket(buf, 0, 12, true, true);
    BasicVRTPacket pB = new BasicVRTPacket(buf, 0,  4,
                                           buf, 4,  4,
                                           buf, 8,  4, true, true);
    BasicVRTPacket pC = new BasicVRTPacket(hdr, 0,  4,
                                           pay, 0,  4,
                                           tlr, 0,  4, true, true);

    act = pA.getPacket(); assertBufEquals("pA.getPacket()", buf, 0, buf.length, act, 0, act.length);
    act = pB.getPacket(); assertBufEquals("pB.getPacket()", buf, 0, buf.length, act, 0, act.length);
    act = pC.getPacket(); assertBufEquals("pC.getPacket()", buf, 0, buf.length, act, 0, act.length);

    act = pA.getPayload(); assertBufEquals("pA.getPayload()", pay, 0, pay.length, act, 0, act.length);
    act = pB.getPayload(); assertBufEquals("pB.getPayload()", pay, 0, pay.length, act, 0, act.length);
    act = pC.getPayload(); assertBufEquals("pC.getPayload()", pay, 0, pay.length, act, 0, act.length);
  }

  public void testGetPacketCount () {
    VRTPacket p = newInstance(testedClass);
    p.setPacketCount(0x4); // just some non-zero number

    for (int i = 0x0; i <= 0xF; i++) {
      p.setPacketCount(i);

      byte[] buf = p.getPacket();
      assertEquals("setPacketCount(..)", i, buf[1] & 0x0F);
      assertEquals("getPacketCount()",   i, p.getPacketCount());
    }
  }

  public void testGetPacketLength () {
    byte[] hdr0  = new byte[VRTPacket.MAX_HEADER_LENGTH];
    byte[] hdr1  = new byte[VRTPacket.MAX_HEADER_LENGTH];
    byte[] data1 = { 0x12, 0x34, 0x56, 0x78 };
    byte[] data2 = { 0x12, 0x34, 0x56, 0x78, 0x0A, 0x0B, 0x0C, 0x0D };

    hdr0[3] = 0x01;
    hdr1[0] = 0x10;
    hdr1[3] = 0x02;

    // ==== NO STREAM IDENTIFIER ===============================================
    VRTPacket p0 = new BasicVRTPacket(hdr0, 0, hdr0.length, false, false);

    assertEquals("p.getHeaderLength()",  4, p0.getHeaderLength());
    assertEquals("p.getPacketLength()",  4, p0.getPacketLength());
    p0.setClassID("AB-CD-EF:1234.5678");
    assertEquals("p.getHeaderLength()", 12, p0.getHeaderLength());
    assertEquals("p.getPacketLength()", 12, p0.getPacketLength());
    p0.setTimeStamp(TimeStamp.getSystemTime());
    assertEquals("p.getHeaderLength()", 24, p0.getHeaderLength());
    assertEquals("p.getPacketLength()", 24, p0.getPacketLength());

    p0.setClassID(null);
    assertEquals("p.getHeaderLength()", 16, p0.getHeaderLength());
    assertEquals("p.getPacketLength()", 16, p0.getPacketLength());
    p0.setTimeStamp(null);
    assertEquals("p.getHeaderLength()",  4, p0.getHeaderLength());
    assertEquals("p.getPacketLength()",  4, p0.getPacketLength());

    // ==== WITH STREAM IDENTIFIER =============================================
    VRTPacket p1 = new BasicVRTPacket(hdr1, 0, hdr1.length, false, false);
    p1.setStreamIdentifier(123);

    assertEquals("p.getHeaderLength()",  8, p1.getHeaderLength());
    assertEquals("p.getPacketLength()",  8, p1.getPacketLength());
    p1.setClassID("AB-CD-EF:1234.5678");
    assertEquals("p.getHeaderLength()", 16, p1.getHeaderLength());
    assertEquals("p.getPacketLength()", 16, p1.getPacketLength());
    p1.setTimeStamp(TimeStamp.getSystemTime());
    assertEquals("p.getHeaderLength()", 28, p1.getHeaderLength());
    assertEquals("p.getPacketLength()", 28, p1.getPacketLength());

    p1.setClassID(null);
    assertEquals("p.getHeaderLength()", 20, p1.getHeaderLength());
    assertEquals("p.getPacketLength()", 20, p1.getPacketLength());
    p1.setTimeStamp(null);
    assertEquals("p.getHeaderLength()",  8, p1.getHeaderLength());
    assertEquals("p.getPacketLength()",  8, p1.getPacketLength());

    // ==== PAYLOAD/TRAILER LENGTH =============================================
    BasicDataPacket p2 = new BasicDataPacket(hdr1, 0, hdr1.length, false, false);
    p2.setStreamIdentifier(123);
    p2.setClassID("AB-CD-EF:1234.5678");
    p2.setTimeStamp(TimeStamp.getSystemTime());

    assertEquals("p.getPacketLength()",  28, p2.getPacketLength());
    assertEquals("p.getHeaderLength()",  28, p2.getHeaderLength());
    assertEquals("p.getPayloadLength()",  0, p2.getPayloadLength());
    assertEquals("p.getTrailerLength()",  0, p2.getTrailerLength());

    p2.setPayload(data1);
    assertEquals("p.getPacketLength()",  28+data1.length, p2.getPacketLength());
    assertEquals("p.getHeaderLength()",  28,              p2.getHeaderLength());
    assertEquals("p.getPayloadLength()",    data1.length, p2.getPayloadLength());
    assertEquals("p.getTrailerLength()",  0,              p2.getTrailerLength());


    p2.setCalibratedTimeStamp(true);
    assertEquals("p.getPacketLength()",  28+data1.length+4, p2.getPacketLength());
    assertEquals("p.getHeaderLength()",  28,                p2.getHeaderLength());
    assertEquals("p.getPayloadLength()",    data1.length,   p2.getPayloadLength());
    assertEquals("p.getTrailerLength()",  4,                p2.getTrailerLength());

    p2.setPayload(data2);
    assertEquals("p.getPacketLength()",  28+data2.length+4, p2.getPacketLength());
    assertEquals("p.getHeaderLength()",  28,                p2.getHeaderLength());
    assertEquals("p.getPayloadLength()",    data2.length,   p2.getPayloadLength());
    assertEquals("p.getTrailerLength()",  4,                p2.getTrailerLength());

    p2.setPayloadLength(data2.length+4);
    assertEquals("p.getPacketLength()",  28+data2.length+4+4, p2.getPacketLength());
    assertEquals("p.getHeaderLength()",  28,                  p2.getHeaderLength());
    assertEquals("p.getPayloadLength()",    data2.length+4,   p2.getPayloadLength());
    assertEquals("p.getTrailerLength()",  4,                  p2.getTrailerLength());

    byte[] expData = Arrays.copyOf(data2, data2.length+4);
    byte[] actData = p2.getPayload();
    assertBufEquals("p.setPayloadLength(..)", expData, 0, expData.length, actData, 0, actData.length);
  }

  public void testGetPacketType () {
    VRTPacket p = newInstance(testedClass);

    for (PacketType t : PacketType.VALUES) {
      if (t.isSupported()) {
        p.setPacketType(t);
        byte[] pkt = p.getPacket();
        int    exp = t.ordinal();
        int    act = (pkt[0] >> 4) & 0x0F;

        assertEquals("setPacketType(..)", exp, act);
        assertEquals("getPacketType()",   t,   p.getPacketType());
      }
    }
  }

  /** Checks packet validity for testGetPacketValid(). */
  private void checkPacketValid (VRTPacket p, boolean isValid, boolean isSemiValid) {
    int len = p.getPacketLength();

    assertEquals("getPacketValid(false) for"+p,      isSemiValid, (p.getPacketValid(false        ) == null));
    assertEquals("getPacketValid(true ) for"+p,      isValid,     (p.getPacketValid(true         ) == null));
    assertEquals("isPacketValid() for"+p,            isValid,      p.isPacketValid()                       );

    assertEquals("getPacketValid(false,"+len+") for"+p, isSemiValid, (p.getPacketValid(false,len) == null));
    assertEquals("getPacketValid(true, "+len+") for"+p, isValid,     (p.getPacketValid(true ,len) == null));
    assertEquals("getPacketValid(false,"+len+") for"+p, isSemiValid, (p.getPacketValid(false,len) == null));
    assertEquals("getPacketValid(true, "+len+") for"+p, isValid,     (p.getPacketValid(true ,len) == null));
    assertEquals("isPacketValid("+len+") for"+p,        isValid,      p.isPacketValid(len)                );

    // Try sizes too large
    for (int i=1,n=len+1; i < 16; i++,n++) {
      assertEquals("getPacketValid(false,"+n+") for"+p, false, (p.getPacketValid(false,n) == null));
      assertEquals("getPacketValid(true, "+n+") for"+p, false, (p.getPacketValid(true ,n) == null));
      assertEquals("getPacketValid(false,"+n+") for"+p, false, (p.getPacketValid(false,n) == null));
      assertEquals("getPacketValid(true, "+n+") for"+p, false, (p.getPacketValid(true ,n) == null));
      assertEquals("isPacketValid("+n+") for"+p,        false,  p.isPacketValid(n)                );
    }

    // Try sizes too small
    for (int i=1,n=len-1; (i < 16) && (n >= 0); i++,n--) {
      assertEquals("getPacketValid(false,"+n+") for"+p, false, (p.getPacketValid(false,n) == null));
      assertEquals("getPacketValid(true, "+n+") for"+p, false, (p.getPacketValid(true ,n) == null));
      assertEquals("getPacketValid(false,"+n+") for"+p, false, (p.getPacketValid(false,n) == null));
      assertEquals("getPacketValid(true, "+n+") for"+p, false, (p.getPacketValid(true ,n) == null));
      assertEquals("isPacketValid("+n+") for"+p,        false,  p.isPacketValid(n)                );
    }
  }

  public void testGetPacketValid () {
    VRTPacket p = newInstance(testedClass);
    checkPacketValid(p, true, true);

    byte[] hdr0  = new byte[VRTPacket.MAX_HEADER_LENGTH];
    byte[] hdr1  = new byte[VRTPacket.MAX_HEADER_LENGTH];
    byte[] hdr3  = { 0x03, 0x00, 0x00, 0x01 }; // <-- semi-valid (reserved bits set)
    byte[] hdr4  = { 0x03, 0x60, 0x00, 0x01 }; // <-- invalid    (missing time stamp)

    hdr0[3] = 0x01;
    hdr1[0] = 0x10;
    hdr1[3] = 0x02;

    VRTPacket p0 = new BasicVRTPacket(hdr0, 0, hdr0.length, false, false);
    VRTPacket p1 = new BasicVRTPacket(hdr1, 0, hdr1.length, false, false);
    VRTPacket p3 = new BasicVRTPacket(hdr3, 0, hdr3.length, true,  true );
    VRTPacket p4 = new BasicVRTPacket(hdr4, 0, hdr4.length, true,  true );

    checkPacketValid(p0, true,  true );
    checkPacketValid(p1, true,  true );
    checkPacketValid(p3, false, true );
    checkPacketValid(p4, false, false);
  }

  public void testGetPadBitCount () {
    VRTPacket p = newInstance(testedClass);
    p.setStreamIdentifier(123);
    p.setClassID("AB-CD-EF:1234.5678");

    // ==== BASIC TESTS ========================================================
    // These are not valid under strict V49 (only under V49b), so expect an error
    // if trying to set it
    if (VRTConfig.VRT_VERSION == VRTConfig.VITAVersion.V49) {
      for (int i = 0; i < 32; i++) {
        try {
          p.setPadBitCount(i, 1);
          assertEquals("setPadBitCount(..) fails under V49", true, false);
        }
        catch (RuntimeException e) {
          assertEquals("setPadBitCount(..) fails under V49", true, true);
        }

        // Now force it in so we can try reading it
        byte[] pkt = p.getPacket();
        pkt[8] = (byte)(i << 3);
        BasicVRTPacket p1 = new BasicVRTPacket(pkt, 0, pkt.length, true, true);
        assertEquals("getPadBitCount()", i, p1.getPadBitCount());
      }
    }
    else {
      for (int i = 0; i < 32; i++) {
        p.setPadBitCount(i, 1);

        byte[] packet = p.getPacket();
        int    act    = (packet[8] >> 3) & 0x1F;

        assertEquals("setPadBitCount(..)", i, act);
        assertEquals("getPadBitCount()",   i, p.getPadBitCount());
      }
    }

    // ==== MAKE SURE CHANGES TO CLASS ID DON'T DELETE IT ======================
    // Manually set PadBitCount so that it works under strict V49 mode too
    byte[] pkt = p.getPacket();
    pkt[8] = (byte)(4 << 3); // <-- 4 pad bits
    BasicVRTPacket p2 = new BasicVRTPacket(pkt, 0, pkt.length, false, true);

    p2.setClassID("12-34-56:ABCD.1234");
    assertEquals("getPadBitCount() after setClassID(..)", 4, p2.getPadBitCount());

    // ==== SPECIAL CASE: bits=0 ===============================================
    p.setPadBitCount(0, 24);
    byte[] packet = p.getPacket();
    assertEquals("setPadBitCount(..) when bits=0", 0, packet[8]);
    assertEquals("getPadBitCount()   when bits=0", 0, p.getPadBitCount());

    // ==== SPECIAL CASE: bits=<implicit> ======================================
    p.setPadBitCount(4, 24);

    byte[] packet2 = p.getPacket();
    int    padBits = (packet2[8] >> 3) & 0x1F;

    assertEquals("setPadBitCount(..) when bits=<implicit>", 0, padBits);
    assertEquals("getPadBitCount()   when bits=<implicit>", 0, p.getPadBitCount());
  }

  public void testGetStreamCode () {
    VRTPacket p = newInstance(testedClass);

    for (PacketType t : PacketType.VALUES) {
      if (t.isSupported()) {
        p.setPacketType(t);

        if (t.hasStreamIdentifier()) {
          for (long i = 0; i < 0xFFFFFFFFL; i+= 0x11111111L) {
            p.setStreamIdentifier((int)i);

            long exp = (((long)t.ordinal()) << 60) | i;
            long act = p.getStreamCode();

            assertEquals("getStreamCode()", exp, act);
          }
        }
        else {
          // If no stream identifier use 0x00000000 in place of it
          long exp = (((long)t.ordinal()) << 60);
          long act = p.getStreamCode();

          assertEquals("getStreamCode()", exp, act);
        }
      }
    }
  }

  public void testGetStreamID () {
    VRTPacket p1 = newInstance(testedClass);
    VRTPacket p2 = newInstance(testedClass);

    for (PacketType t : PacketType.VALUES) {
      if (t.isSupported()) {
        p1.setPacketType(t);
        p2.setPacketType(t);

        if (t.hasStreamIdentifier()) {
          for (long i = 0; i < 0xFFFFFFFFL; i+= 0x11111111L) {
            int    exp = (int)i;
            String str = Integer.toString(exp);
            byte[] sid = new byte[4];
            VRTMath.packInt(sid, 0, exp, BIG_ENDIAN);

            p1.setStreamID(str);
            p2.setStreamIdentifier(exp);

            byte[] pkt1 = p1.getPacket();
            byte[] pkt2 = p2.getPacket();

            assertBufEquals("setStreamID(..)",         sid, 0, 4, pkt1, 4, 4);
            assertBufEquals("setStreamIdentifier(..)", sid, 0, 4, pkt2, 4, 4);

            assertEquals("getStreamID()",         str, p1.getStreamID());
            assertEquals("getStreamIdentifier()", exp, p1.getStreamIdentifier());
          }
        }
        else {
          try {
            p1.setStreamID("123");
            assertEquals("setStreamID(..) fails when none permitted", true, false);
          }
          catch (RuntimeException e) {
            assertEquals("setStreamID(..) fails when none permitted", true, true);
          }
          try {
            p1.setStreamIdentifier(123);
            assertEquals("setStreamIdentifier(..) fails when none permitted", true, false);
          }
          catch (RuntimeException e) {
            assertEquals("setStreamIdentifier(..) fails when none permitted", true, true);
          }

          assertEquals("getStreamIdentifier()", null, p1.getStreamID());
          assertEquals("getStreamIdentifier()", null, p1.getStreamIdentifier());
        }
      }
    }
  }

  /** Utility time stamp tester used by testGetTimeStamp(..). */
  private void testTimeStamp (VRTPacket p, IntegerMode tsiMode, FractionalMode tsfMode,
                                           Long tsi, Long tsf) {
    TimeStamp useTS;
    TimeStamp expTS;

    if ((tsi == null) && (tsf == null)) {
      useTS = null;
      expTS = TimeStamp.NO_TIME_STAMP;
    }
    else {
      useTS = new TimeStamp(tsiMode, tsfMode, tsi, tsf);
      expTS = useTS;
    }

    p.setTimeStamp(useTS);

    byte[]  pkt        = p.getPacket();
    int     expTSIMode = tsiMode.ordinal();
    int     expTSFMode = tsfMode.ordinal();
    int     actTSIMode = (pkt[1] >> 6) & 0x03;
    int     actTSFMode = (pkt[1] >> 4) & 0x03;
    Integer expTSI     = (tsi == null)? null : tsi.intValue();
    Long    expTSF     = tsf;
    Integer actTSI     = null;
    Long    actTSF     = null;

    int off = 4;
    if (p.getStreamIdentifier() != null) off+=4;
    if (p.getClassIdentifier()  != null) off+=8;

    if (actTSIMode != 0) {
      actTSI = VRTMath.unpackInt(pkt, off, BIG_ENDIAN);
      off+=4;
    }
    if (actTSFMode != 0) {
      actTSF = VRTMath.unpackLong(pkt, off, BIG_ENDIAN);
    }

    assertEquals("setTimeStamp(..) TSI Mode "+useTS,  expTSIMode, actTSIMode);
    assertEquals("setTimeStamp(..) TSF Mode "+useTS,  expTSFMode, actTSFMode);
    assertEquals("setTimeStamp(..) TSI Value "+useTS, expTSI,     actTSI);
    assertEquals("setTimeStamp(..) TSF Value "+useTS, expTSF,     actTSF);
    assertEquals("getTimeStamp() for "+useTS,         expTS,      p.getTimeStamp());
  }

  public void testGetTimeStamp () {
    final long y2k_utc = TimeStamp.Y2K_GPS.getUTCSeconds();
    final long y2k_gps = TimeStamp.Y2K_GPS.getGPSSeconds();

    VRTPacket p = newInstance(testedClass);

    testTimeStamp(p, IntegerMode.None,  FractionalMode.None, null,    null);
    testTimeStamp(p, IntegerMode.UTC,   FractionalMode.None, y2k_utc, null);
    testTimeStamp(p, IntegerMode.GPS,   FractionalMode.None, y2k_gps, null);
    testTimeStamp(p, IntegerMode.Other, FractionalMode.None, 123456L, null);

    testTimeStamp(p, IntegerMode.None,  FractionalMode.SampleCount, null,    1L);
    testTimeStamp(p, IntegerMode.UTC,   FractionalMode.SampleCount, y2k_utc, 2L);
    testTimeStamp(p, IntegerMode.GPS,   FractionalMode.SampleCount, y2k_gps, 3L);
    testTimeStamp(p, IntegerMode.Other, FractionalMode.SampleCount, 123456L, 4L);

    testTimeStamp(p, IntegerMode.None,  FractionalMode.RealTime, null,    1L);
    testTimeStamp(p, IntegerMode.UTC,   FractionalMode.RealTime, y2k_utc, 2L);
    testTimeStamp(p, IntegerMode.GPS,   FractionalMode.RealTime, y2k_gps, 3L);
    testTimeStamp(p, IntegerMode.Other, FractionalMode.RealTime, 123456L, 4L);

    testTimeStamp(p, IntegerMode.None,  FractionalMode.FreeRunningCount, null,    1L);
    testTimeStamp(p, IntegerMode.UTC,   FractionalMode.FreeRunningCount, y2k_utc, 2L);
    testTimeStamp(p, IntegerMode.GPS,   FractionalMode.FreeRunningCount, y2k_gps, 3L);
    testTimeStamp(p, IntegerMode.Other, FractionalMode.FreeRunningCount, 123456L, 4L);
  }


  public void testHashCode () {
    byte[] data = { 0x12, 0x34, 0x56, 0x78, 0x0A, 0x0B, 0x0C, 0x0D };

    VRTPacket p = newInstance(testedClass);
    assertEquals("hashCode()", _hashCode(p), p.hashCode());

    p.setStreamIdentifier(123);
    assertEquals("hashCode()", _hashCode(p), p.hashCode());

    p.setClassID("AB-CD-EF:1234.5678");
    assertEquals("hashCode()", _hashCode(p), p.hashCode());

    p.setTimeStamp(TimeStamp.getSystemTime());
    assertEquals("hashCode()", _hashCode(p), p.hashCode());

    p.setPayload(data);
    assertEquals("hashCode()", _hashCode(p), p.hashCode());
  }

  /** Direct copy-paste from javadocs for VRTPacket.hashCode(). */
  private int _hashCode (VRTPacket p) {
    int    hash = 0;
    int    len  = Math.min(p.getPacketLength(), 32);
    byte[] buf  = new byte[len];

    p.readPacket(buf, 0, 0, len);
    for (int i = 0; i < len; i++) {
      hash ^= (buf[i] << i);
    }
    return hash;
  }

  public void testIsChangePacket () {
    // Default implementation is simply to return true
    VRTPacket p = newInstance(testedClass);
    assertEquals("isChangePacket()", true, p.isChangePacket());
  }

  public void testIsTimeStampMode () {
    // Default implementation is simply to return true
    VRTPacket p0 = newInstance(testedClass);
    VRTPacket p1 = newInstance(testedClass);
    VRTPacket p2 = newInstance(testedClass);

    p0.setPacketType(PacketType.Data);
    p1.setPacketType(PacketType.Context);
    p2.setPacketType(PacketType.Context);

    try {
      p0.setTimeStampMode(true);
      assertEquals("setTimeStampMode(..) fails in data packet", true, false);
    }
    catch (Exception e) {
      assertEquals("setTimeStampMode(..) fails in data packet", true, true);
    }

    p1.setTimeStampMode(false);
    p2.setTimeStampMode(true);

    byte[] pkt1 = p1.getPacket();
    byte[] pkt2 = p2.getPacket();

    assertEquals("setTimeStampMode(false)", 0x00, (pkt1[0] & 0x01));
    assertEquals("setTimeStampMode(true)",  0x01, (pkt2[0] & 0x01));

    assertEquals("isTimeStampMode()", null,  p0.isTimeStampMode());
    assertEquals("isTimeStampMode()", false, p1.isTimeStampMode());
    assertEquals("isTimeStampMode()", true,  p2.isTimeStampMode());
  }

  public void testReadPacket () {
    byte[] buf = { 0x04,0x00,0x00,0x03, 0x12,0x34,0x56,0x78, 0x00,0x00,0x00,0x00 };

    BasicVRTPacket pA = new BasicVRTPacket(buf, 0, 12, true, true);
    BasicVRTPacket pB = new BasicVRTPacket(buf, 0,  4,
                                           buf, 4,  4,
                                           buf, 8,  4, true, true);
    byte[] packetA  = pA.getPacket();
    byte[] packetB  = pB.getPacket();
    byte[] payloadA = pA.getPayload();
    byte[] payloadB = pB.getPayload();
    byte[] buffer   = new byte[1024];
    int    nread;

    // ==== READ FULLY =========================================================
    nread = pA.readPacket(buffer);
    assertBufEquals("pA.readPacket(..)", packetA, 0, packetA.length, buffer, 0, nread);
    nread = pB.readPacket(buffer);
    assertBufEquals("pB.readPacket(..)", packetB, 0, packetB.length, buffer, 0, nread);

    nread = pA.readPayload(buffer);
    assertBufEquals("pA.readPayload(..)", payloadA, 0, payloadA.length, buffer, 0, nread);
    nread = pB.readPayload(buffer);
    assertBufEquals("pB.readPayload(..)", payloadB, 0, payloadB.length, buffer, 0, nread);


    // ==== READ PARTIAL =======================================================
    nread = pA.readPacket(buffer,128,3,7);
    assertBufEquals("pA.readPacket(..)", packetA, 3, 7, buffer, 128, nread);
    nread = pB.readPacket(buffer,128,3,7);
    assertBufEquals("pB.readPacket(..)", packetB, 3, 7, buffer, 128, nread);

    nread = pA.readPayload(buffer,512,2,8);
    assertBufEquals("pA.readPayload(..)", payloadA, 2, 2, buffer, 512, nread);
    nread = pB.readPayload(buffer,512,2,8);
    assertBufEquals("pB.readPayload(..)", payloadB, 2, 2, buffer, 512, nread);
  }

  public void testReadPacketFrom () throws IOException {
    byte[] buf = { 0x04,0x00,0x00,0x03, 0x12,0x34,0x56,0x78, 0x00,0x00,0x00,0x00 };

    BasicVRTPacket exp = new BasicVRTPacket(buf, 0, 12, false, false);

    byte[] buffer = { 0,0,0,1, 0,0,0,0, 0,0,0,0, 0,0,0,0, // <-- note 1 set for size
                      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,
                      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,
                      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,
                      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0 };

    BasicVRTPacket actA = new BasicVRTPacket(buffer, 0, 0, false, false);
    BasicVRTPacket actB = new BasicVRTPacket(buffer, 0, 0,
                                             buffer, 0, 0,
                                             buffer, 0, 0, false, false);
    BasicVRTPacket actC = new BasicVRTPacket(buffer, 0, 0, false, false);
    BasicVRTPacket actD = new BasicVRTPacket(buffer, 0, 0,
                                             buffer, 0, 0,
                                             buffer, 0, 0, false, false);

    ByteArrayInputStream inA = new ByteArrayInputStream(buf);
    ByteArrayInputStream inB = new ByteArrayInputStream(buf);
    ByteArrayInputStream inC = new ByteArrayInputStream(buf, 4, buf.length-4);
    ByteArrayInputStream inD = new ByteArrayInputStream(buf, 4, buf.length-4);

    actA.readPacketFrom(inA);
    actB.readPacketFrom(inB);
    actC.readPacketFrom(inC, buf, 0);
    actD.readPacketFrom(inD, buf, 0);

    assertEquals("pA.readPacketFrom(..)", exp, actA);
    assertEquals("pB.readPacketFrom(..)", exp, actB);
    assertEquals("pC.readPacketFrom(..)", exp, actC);
    assertEquals("pD.readPacketFrom(..)", exp, actD);
  }

  public void testResetForResend () {
    // Default implementation is simply to set time stamp and then return false
    VRTPacket p  = newInstance(testedClass);
    TimeStamp ts = TimeStamp.getSystemTime();

    boolean act = p.resetForResend(ts);

    assertEquals("resetForResend() return value",  false, act);
    assertEquals("resetForResend() new TimeStamp", ts,    p.getTimeStamp());
  }

  public void testToBasicVRTPacket () {
    // WARNING:
    //   The tests in here are terribly incomplete since they only check the
    //   "happy path" whereby any instance of BasicVRTPacket is simply returned
    //   as-is. It does NOT check the case where a non-BasicVRTPacket if passed
    //   in.
    VRTPacket      p0 = newInstance(testedClass);
    BasicVRTPacket p1 = BasicVRTPacket.toBasicVRTPacket(p0);

    assertEquals("toBasicVRTPacket(..)", p0, p1);

    BasicDataPacket d0 = new BasicDataPacket();
    BasicVRTPacket  d1 = BasicVRTPacket.toBasicVRTPacket(d0);

    assertEquals("toBasicVRTPacket(..)", d0, d1);
  }

  public void testToString () {
    VRTPacket p = newInstance(testedClass);

    assertEquals("toString() is null",  false, (p.toString() == null));
    assertEquals("toString() is empty", false, (p.toString().isEmpty()));
  }

  public void testWritePacketTo () throws IOException {
    byte[] buf = { 0x04,0x00,0x00,0x03, 0x12,0x34,0x56,0x78, 0x00,0x00,0x00,0x00 };

    BasicVRTPacket pA = new BasicVRTPacket(buf, 0, 12, false, false);
    BasicVRTPacket pB = new BasicVRTPacket(buf, 0,  4,
                                           buf, 4,  4,
                                           buf, 8,  4, false, false);

    ByteArrayOutputStream outA = new ByteArrayOutputStream();
    ByteArrayOutputStream outB = new ByteArrayOutputStream();

    pA.writePacketTo(outA);
    pB.writePacketTo(outB);

    assertBufEquals("pA.writePacketTo(..)", buf, 0, buf.length, outA.toByteArray(), 0, outA.size());
    assertBufEquals("pB.writePacketTo(..)", buf, 0, buf.length, outB.toByteArray(), 0, outB.size());
  }

  public void testWritePayload () {
    byte[] buf = { 0x04,0x00,0x00,0x03, 0x12,0x34,0x56,0x78, 0x00,0x00,0x00,0x00 };
    byte[] exp = { 0x04,0x00,0x00,0x03, 0x12,0x01,0x02,0x78, 0x00,0x00,0x00,0x00 };
    byte[] dat = { 0x00,0x01,0x02,0x03 };

    BasicVRTPacket pA = new BasicVRTPacket(buf, 0, 12, false, false);
    BasicVRTPacket pB = new BasicVRTPacket(buf, 0,  4,
                                           buf, 4,  4,
                                           buf, 8,  4, false, false);

    pA.writePayload(dat, 1, 1, 2);
    pB.writePayload(dat, 1, 1, 2);

    byte[] packetA  = pA.getPacket();
    byte[] packetB  = pB.getPacket();

    assertBufEquals("pA.writePayload(..)", exp, 0, exp.length, packetA, 0, packetA.length);
    assertBufEquals("pB.writePayload(..)", exp, 0, exp.length, packetB, 0, packetB.length);
  }


  private static final int        FIELD_OFFSET = 0;
  private static final String[]   FIELD_NAMES  = { "StreamID",   "ClassID",    "TimeStamp" };
  private static final Class<?>[] FIELD_TYPES  = { String.class, String.class, TimeStamp.class };

  public void testGetFieldName () {
    HasFields hf = (HasFields)newInstance(testedClass);
    checkGetFieldName(hf, FIELD_OFFSET, FIELD_NAMES, FIELD_TYPES);
  }

  public void testGetField () {
    HasFields hf  = (HasFields)newInstance(testedClass);
    TimeStamp ts1 = TimeStamp.Y2K_GPS;
    TimeStamp ts2 = TimeStamp.getSystemTime();
    checkGetField(hf, "StreamID",  "123",                "42");
    checkGetField(hf, "ClassID",   "AB-CD-EF:1234.5678", "12-34-56:ABCD.1234");
    checkGetField(hf, "TimeStamp", ts1,                  ts2);
  }



  public static class PayloadFormatTest implements Testable {
    @Override
    public Class<?> getTestedClass () {
      return PayloadFormat.class;
    }

    @Override
    public List<TestSet> init () throws Exception {
      List<TestSet> tests = new ArrayList<TestSet>();

      tests.add(new TestSet("equals(..)",                   this,  "testEquals"));
      tests.add(new TestSet("getBits()",                    this,  "testGetBits"));
      tests.add(new TestSet("getChannelTagSize()",          this,  "testGetChannelTagSize"));
      tests.add(new TestSet("getDataItemFormat()",          this,  "testGetDataItemFormat"));
      tests.add(new TestSet("getDataItemSize()",            this,  "testGetDataItemSize"));
      tests.add(new TestSet("getDataType()",                this,  "testGetDataType"));
      tests.add(new TestSet("getEventTagSize()",            this,  "testGetEventTagSize"));
      tests.add(new TestSet("getField(..)",                 this,  "testGetField"));
      tests.add(new TestSet("getFieldByName(..)",           this, "+testGetField"));
      tests.add(new TestSet("getFieldCount()",              this, "+testGetFieldName"));
      tests.add(new TestSet("getFieldID(..)",               this, "+testGetFieldName"));
      tests.add(new TestSet("getFieldName(..)",             this,  "testGetFieldName"));
      tests.add(new TestSet("getFieldType(..)",             this, "+testGetFieldName"));
      tests.add(new TestSet("getItemPackingFieldSize()",    this,  "testGetItemPackingFieldSize"));
      tests.add(new TestSet("getRealComplexType()",         this,  "testGetRealComplexType"));
      tests.add(new TestSet("getRepeatCount()",             this,  "testGetRepeatCount"));
      tests.add(new TestSet("getValid()",                   this,  "testGetValid"));
      tests.add(new TestSet("getVectorSize()",              this,  "testGetVectorSize"));
      tests.add(new TestSet("hashCode()",                   this,  "testHashCode"));
      tests.add(new TestSet("isComplex()",                  this, "+testGetRealComplexType"));
      tests.add(new TestSet("isProcessingEfficient()",      this,  "testIsProcessingEfficient"));
      tests.add(new TestSet("isRepeating()",                this,  "testIsRepeating"));
      tests.add(new TestSet("isSigned()",                   this,  "testIsSigned"));
      tests.add(new TestSet("isValid()",                    this, "+testGetValid"));
      tests.add(new TestSet("setBits(..)",                  this, "+testGetBits"));
      tests.add(new TestSet("setChannelTagSize(..)",        this, "+testGetChannelTagSize"));
      tests.add(new TestSet("setDataItemFormat(..)",        this, "+testGetDataItemFormat"));
      tests.add(new TestSet("setDataItemSize(..)",          this, "+testGetDataItemSize"));
      tests.add(new TestSet("setDataType(..)",              this, "+testGetDataType"));
      tests.add(new TestSet("setEventTagSize(..)",          this, "+testGetEventTagSize"));
      tests.add(new TestSet("setField(..)",                 this, "+testGetField"));
      tests.add(new TestSet("setFieldByName(..)",           this, "+testGetField"));
      tests.add(new TestSet("setItemPackingFieldSize(..)",  this, "+testGetItemPackingFieldSize"));
      tests.add(new TestSet("setProcessingEfficient(..)",   this, "+testIsProcessingEfficient"));
      tests.add(new TestSet("setRealComplexType(..)",       this, "+testGetRealComplexType"));
      tests.add(new TestSet("setRepeatCount(..)",           this, "+testGetRepeatCount"));
      tests.add(new TestSet("setRepeating(..)",             this, "+testIsRepeating"));
      tests.add(new TestSet("setVectorSize(..)",            this, "+testGetVectorSize"));
      tests.add(new TestSet("toString()",                   this,  "testToString"));

      return tests;
    }

    @Override
    public void done () throws Exception {
      // nothing to do
    }

    public void testEquals () {
      PayloadFormat pf1 = new PayloadFormat(DataType.Int32);
      PayloadFormat pf2 = new PayloadFormat(DataType.Double);
      PayloadFormat pf3 = new PayloadFormat(DataType.Double);
      Object        obj = "Hello World!";

      assertEquals("Int32  + Double -> equals(..)", false, pf1.equals(pf2)); // Diff values
      assertEquals("Int32  + Object -> equals(..)", false, pf1.equals(obj)); // Diff class type
      assertEquals("Int32  + Int32  -> equals(..)", true,  pf1.equals(pf1)); // Same object
      assertEquals("Double + Double -> equals(..)", true,  pf2.equals(pf3)); // Diff object
    }

    public void testGetBits () {
      PayloadFormat pf1   = new PayloadFormat(DataType.Int32);
      long          bits1 = 0x000007DF00000000L;
      PayloadFormat pf2   = new PayloadFormat(false, RealComplexType.ComplexCartesian,
                                              DataItemFormat.UnsignedInt, true, 4, 8, 32,
                                              12, 16, 256);
      long          bits2 = 0xB0C807CB000F00FFL;
      PayloadFormat pf3   = new PayloadFormat(DataType.Int32);

      assertEquals("Int32  -> getBits()", bits1, pf1.getBits());
      assertEquals("Double -> getBits()", bits2, pf2.getBits());

      pf3.setBits(bits2);
      assertEquals("setBits(..)", pf2, pf3);
    }

    public void testGetChannelTagSize () {
      PayloadFormat pf = new PayloadFormat(DataType.Int32);

      assertEquals("getChannelTagSize()", 0, pf.getChannelTagSize());

      long mask = 0x000F000000000000L;
      for (int i = 15; i >= 0; i--) {
        String msg     = "setChannelTagSize("+i+")";
        long   expBits = ((long)i) << 48;

        pf.setChannelTagSize(i);
        assertEquals(msg, i,       pf.getChannelTagSize());
        assertEquals(msg, expBits, pf.getBits() & mask);
      }
    }

    public void testGetEventTagSize () {
      PayloadFormat pf = new PayloadFormat(DataType.Int32);

      assertEquals("getEventTagSize()", 0, pf.getEventTagSize());

      long mask = 0x0070000000000000L;
      for (int i = 7; i >= 0; i--) {
        String msg     = "setEventTagSize("+i+")";
        long   expBits = ((long)i) << 52;

        pf.setEventTagSize(i);
        assertEquals(msg, i,       pf.getEventTagSize());
        assertEquals(msg, expBits, pf.getBits() & mask);
      }
    }

    public void testGetDataItemSize () {
      PayloadFormat pf = new PayloadFormat(DataType.Int32);

      assertEquals("getDataItemSize()", 32, pf.getDataItemSize());

      long mask = 0x0000003F00000000L;
      for (int i = 1; i <= 64; i++) {
        String msg     = "setDataItemSize("+i+")";
        long   expBits = ((long)i-1) << 32;

        pf.setDataItemSize(i);
        assertEquals(msg, i,       pf.getDataItemSize());
        assertEquals(msg, expBits, pf.getBits() & mask);
      }
    }

    public void testGetItemPackingFieldSize () {
      PayloadFormat pf = new PayloadFormat(DataType.UInt1);

      assertEquals("getItemPackingFieldSize()", 1, pf.getItemPackingFieldSize());

      long mask = 0x00000FC000000000L;
      for (int i = 1; i <= 64; i++) {
        String msg     = "setItemPackingFieldSize("+i+")";
        long   expBits = ((long)i-1) << 38;

        pf.setItemPackingFieldSize(i);
        assertEquals(msg, i,       pf.getItemPackingFieldSize());
        assertEquals(msg, expBits, pf.getBits() & mask);
      }
    }

    public void testGetRepeatCount ()  {
      PayloadFormat pf = new PayloadFormat(DataType.Int32);

      assertEquals("getRepeatCount()", 1, pf.getRepeatCount());

      long mask = 0x00000000FFFF0000L;
      for (int i = 1; i <= 65536; i*=2) {
        String msg     = "setRepeatCount("+i+")";
        long   expBits = ((long)i-1) << 16;

        pf.setRepeatCount(i);
        assertEquals(msg, i,       pf.getRepeatCount());
        assertEquals(msg, expBits, pf.getBits() & mask);
      }
    }

    public void testGetVectorSize ()  {
      PayloadFormat pf = new PayloadFormat(DataType.Int32);

      assertEquals("getVectorSize()", 1, pf.getVectorSize());

      long mask = 0x000000000000FFFFL;
      for (int i = 1; i <= 65536; i*=2) {
        String msg     = "setVectorSize("+i+")";
        long   expBits = ((long)i-1);

        pf.setVectorSize(i);
        assertEquals(msg, i,       pf.getVectorSize());
        assertEquals(msg, expBits, pf.getBits() & mask);
      }
    }

    public void testGetDataItemFormat () {
      PayloadFormat pf = new PayloadFormat(DataType.Int32);

      assertEquals("getDataItemFormat()", DataItemFormat.SignedInt, pf.getDataItemFormat());

      long mask = 0x1F00000000000000L;
      for (DataItemFormat f : DataItemFormat.VALUES) {
        String msg = "setDataItemFormat("+f+")";
        if (f.isSupported()) {
          long expBits = ((long)f.ordinal()) << 56;

          pf.setDataItemFormat(f);
          assertEquals(msg, f,       pf.getDataItemFormat());
          assertEquals(msg, expBits, pf.getBits() & mask);
        }
        else {
          try {
            pf.setDataItemFormat(f);
            assertEquals(msg+" fails", true, false);
          }
          catch (IllegalArgumentException e) {
            assertEquals(msg+" fails", true, true);
          }
        }
      }
    }

    public void testGetRealComplexType () {
      PayloadFormat pf = new PayloadFormat(DataType.Int32);

      assertEquals("getRealComplexType()", RealComplexType.Real, pf.getRealComplexType());
      assertEquals("isComplex()",          false,                pf.isComplex());

      long mask = 0x6000000000000000L;
      for (RealComplexType t : RealComplexType.VALUES) {
        String msg = "setRealComplexType("+t+")";

        if (t.isSupported()) {
          long    expBits  = ((long)t.ordinal()) << 61;
          boolean expCmplx = (t != RealComplexType.Real);

          pf.setRealComplexType(t);
          assertEquals(msg, t,        pf.getRealComplexType());
          assertEquals(msg, expBits,  pf.getBits() & mask);
          assertEquals(msg, expCmplx, pf.isComplex());
        }
        else {
          try {
            pf.setRealComplexType(t);
            assertEquals(msg+" fails", true, false);
          }
          catch (IllegalArgumentException e) {
            assertEquals(msg+" fails", true, true);
          }
        }
      }
    }

    @SuppressWarnings("unused")
    public void testGetDataType () {
      PayloadFormat pf1 = new PayloadFormat(DataType.Int32);

      for (DataType dt : DataType.VALUES) {
        if (dt.isSupported()) {
          PayloadFormat pf2 = new PayloadFormat(dt);

          pf1.setDataType(dt);
          assertEquals("getDataType()",   dt,  pf2.getDataType());
          assertEquals("setDataType(..)", pf2, pf1);
        }
        else {
          try {
            pf1.setDataType(dt);
            assertEquals("PayloadFormat(..) fails", true, false);
          }
          catch (IllegalArgumentException e) {
            assertEquals("PayloadFormat(..) fails", true, true);
          }
          try {
            PayloadFormat pf2 = new PayloadFormat(dt);
            assertEquals("setDataType(..) fails", true, false);
          }
          catch (IllegalArgumentException e) {
            assertEquals("setDataType(..) fails", true, true);
          }
        }
      }
    }

    /** Used by testGetValid() to check invalid combinations. */
    @SuppressWarnings("unused")
    private void testInvalidType (DataItemFormat format, int dSize, int fSize) {
      String msg = "Attempting to create PayloadFormat with format="+format+" "
                 + "dSize="+dSize+" fSize="+fSize+" fails";
      try {
        PayloadFormat pf = new PayloadFormat(true, RealComplexType.Real, format, false, 0, 0, fSize, dSize, 1, 1);
        assertEquals(msg, true, false);
      }
      catch (Exception e) {
        assertEquals(msg, true, true);
      }
    }

    public void testGetValid () {
      // ==== CHECK VALID TYPES ================================================
      for (DataType dt : DataType.VALUES) {
        if (dt.isSupported()) {
          PayloadFormat pf = new PayloadFormat(dt);
          assertEquals("getValid()", null, pf.getValid());
          assertEquals("isValid()",  true, pf.isValid());
        }
      }

      // ==== CHECK INVALID TYPES ==============================================
      testInvalidType(DataItemFormat.SignedInt,    64, 32);  // fSize  <  dSize
      testInvalidType(DataItemFormat.Float,        16, 64);  // Float  != 32-bits
      testInvalidType(DataItemFormat.Double,       16, 64);  // Double != 64-bits
      testInvalidType(DataItemFormat.SignedVRT1,    1, 64);  // VRT1   <   2-bits
      testInvalidType(DataItemFormat.SignedVRT2,    2, 64);  // VRT2   <   3-bits
      testInvalidType(DataItemFormat.SignedVRT3,    3, 64);  // VRT3   <   4-bits
      testInvalidType(DataItemFormat.SignedVRT4,    4, 64);  // VRT4   <   5-bits
      testInvalidType(DataItemFormat.SignedVRT5,    5, 64);  // VRT5   <   6-bits
      testInvalidType(DataItemFormat.SignedVRT6,    6, 64);  // VRT6   <   7-bits
      testInvalidType(DataItemFormat.UnsignedVRT1,  1, 64);  // VRT1   <   2-bits
      testInvalidType(DataItemFormat.UnsignedVRT2,  2, 64);  // VRT2   <   3-bits
      testInvalidType(DataItemFormat.UnsignedVRT3,  3, 64);  // VRT3   <   4-bits
      testInvalidType(DataItemFormat.UnsignedVRT4,  4, 64);  // VRT4   <   5-bits
      testInvalidType(DataItemFormat.UnsignedVRT5,  5, 64);  // VRT5   <   6-bits
      testInvalidType(DataItemFormat.UnsignedVRT6,  6, 64);  // VRT6   <   7-bits
    }

    public void testHashCode () {
      PayloadFormat pf1   = new PayloadFormat(DataType.Int32);
      PayloadFormat pf2   = new PayloadFormat(DataType.Double);
      int           hash1 = Long.valueOf(pf1.getBits()).hashCode();
      int           hash2 = Long.valueOf(pf2.getBits()).hashCode();

      assertEquals("Int32  -> hashCode()", hash1, pf1.hashCode());
      assertEquals("Double -> hashCode()", hash2, pf2.hashCode());
    }

    public void testIsProcessingEfficient () {
      // Note that the bit in the underlying field is LinkEfficient=0/1 which
      // is the reverse of ProcessingEfficient
      PayloadFormat pf = new PayloadFormat(DataType.Int32);

      assertEquals("isProcessingEfficient()", true, pf.isProcessingEfficient());

      pf.setProcessingEfficient(true);
      assertEquals("setProcessingEfficient(true)", true,                pf.isProcessingEfficient());
      assertEquals("setProcessingEfficient(true)", 0x0000000000000000L, pf.getBits() & 0x8000000000000000L);

      pf.setProcessingEfficient(false);
      assertEquals("setProcessingEfficient(false)", false,               pf.isProcessingEfficient());
      assertEquals("setProcessingEfficient(false)", 0x8000000000000000L, pf.getBits() & 0x8000000000000000L);
    }

    public void testIsRepeating () {
      PayloadFormat pf = new PayloadFormat(DataType.Int32);

      assertEquals("isRepeating()", false,  pf.isRepeating());

      pf.setRepeating(true);
      assertEquals("setRepeating(true)", true,                pf.isRepeating());
      assertEquals("setRepeating(true)", 0x0080000000000000L, pf.getBits() & 0x0080000000000000L);

      pf.setRepeating(false);
      assertEquals("setRepeating(false)", false,               pf.isRepeating());
      assertEquals("setRepeating(false)", 0x0000000000000000L, pf.getBits() & 0x0080000000000000L);
    }

    public void testIsSigned () {
      PayloadFormat pf1 = new PayloadFormat(DataType.Int32);
      PayloadFormat pf2 = new PayloadFormat(DataType.UInt8);

      assertEquals("Int32 -> isSigned()", true,  pf1.isSigned());
      assertEquals("UInt8 -> isSigned()", false, pf2.isSigned());
    }

    public void testToString () {
      HasFields pf = new PayloadFormat(DataType.Int32);
      assertEquals("toString() is null",  false, (pf.toString() == null));
      assertEquals("toString() is empty", false, (pf.toString().isEmpty()));
    }


    private final int        FIELD_OFFSET = 0;
    private final String[]   FIELD_NAMES  = {
      "ProcessingEfficient", "RealComplexType", "DataItemFormat",       "Repeating",
      "EventTagSize",        "ChannelTagSize",  "ItemPackingFieldSize", "DataItemSize",
      "RepeatCount",         "VectorSize",      "DataType"
    };
    private final Class<?>[] FIELD_TYPES  = {
      Boolean.TYPE, RealComplexType.class, DataItemFormat.class, Boolean.TYPE,
      Integer.TYPE, Integer.TYPE,          Integer.TYPE,         Integer.TYPE,
      Integer.TYPE, Integer.TYPE,          DataType.class
    };

    public void testGetFieldName () {
      HasFields hf = new PayloadFormat(DataType.Int32);
      checkGetFieldName(hf, FIELD_OFFSET, FIELD_NAMES, FIELD_TYPES);
    }

    public void testGetField () {
      HasFields hf = new PayloadFormat(DataType.Int32);
      checkGetField(hf, "ProcessingEfficient",  true,                     false);
      checkGetField(hf, "RealComplexType",      RealComplexType.Real,     RealComplexType.ComplexCartesian);
      checkGetField(hf, "DataItemFormat",       DataItemFormat.SignedInt, DataItemFormat.UnsignedInt);
      checkGetField(hf, "Repeating",            true,                     false);
      checkGetField(hf, "EventTagSize",         0,                        4);
      checkGetField(hf, "ChannelTagSize",       0,                        4);
      checkGetField(hf, "ItemPackingFieldSize", 32,                       64);
      checkGetField(hf, "DataItemSize",         32,                       64);
      checkGetField(hf, "RepeatCount",          1,                        32768);
      checkGetField(hf, "VectorSize",           1,                        32768);
      checkGetField(hf, "DataType",             DataType.Int16,           DataType.Int64);
    }
  }
}
