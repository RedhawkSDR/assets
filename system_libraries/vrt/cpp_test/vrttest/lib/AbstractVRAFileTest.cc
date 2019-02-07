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

#include "AbstractVRAFileTest.h"
#include "BasicContextPacket.h"
#include "BasicDataPacket.h"
#include "BasicDataPacket.h"
#include "HasFieldsTest.h"
#include "TestRunner.h"
#include "VRTConfig.h"
#include <errno.h>  // for errno
#include <stdio.h>  // for remove(..) / tmpnam(..) / et.al.
#include <stdlib.h> // for getenv(..)


using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

/** Creates a temp file using the given prefix and suffix. */
static string createTempFile (string prefix, string suffix) {
  char *tmpdir = getenv("TMPDIR");

  if (tmpdir != NULL) {
         if (tmpdir[0] == '/' ) prefix = string(tmpdir) + "/"  + prefix;
    else if (tmpdir[0] == '\\') prefix = string(tmpdir) + "\\" + prefix;
    else                        prefix = string(tmpdir) + "/"  + prefix;
  }
  else {
#if !defined(_WIN32)
    prefix = string("/tmp/")  + prefix;
#endif
  }

  for (int32_t i = 0; i < 100; i++) {
    string fname = Utilities::format("%s%d%s", prefix.c_str(), i, suffix.c_str());

    FILE *in = fopen(fname.c_str(), "rb");
    if (in != NULL) {
      if (fclose(in) != 0) {
        throw VRTException("Unable to close %s: %s", fname.c_str(), ERRNO_STR);
      }
      continue; // Duplicate name
    }

    FILE *out = fopen(fname.c_str(), "wb+");
    if (out != NULL) {
      if (fclose(out) != 0) {
        throw VRTException("Unable to close %s: %s", fname.c_str(), ERRNO_STR);
      }
      return fname;
    }
  }
  return ""; // FAILED
}


AbstractVRAFileTest::AbstractVRAFileTest (string testedClass) :
  testedClass(testedClass),
  fname1(""),
  fname2("")
{
  // done
}

AbstractVRAFile* AbstractVRAFileTest::openFile (string fname, FileMode mode, bool isSetSize,
                                                bool isSetCRC, bool isStrict) {
  if (testedClass == "vrt::BasicVRAFile") {
    return new BasicVRAFile(fname, mode, isSetSize, isSetCRC, isStrict);
  }
  else if (testedClass == "vrt::AbstractVRAFile") {
    // Can't instantiate abstract class, so use commonly-used sub-class
    return new BasicVRAFile(fname, mode, isSetSize, isSetCRC, isStrict);
  }
  else {
    throw VRTException("Unsupported test class "+getTestedClass());
  }
}

vector<BasicVRTPacket*> AbstractVRAFileTest::makeTestPacketVector () {
  PayloadFormat   pf    = PayloadFormat_INT32;
  TimeStamp       time0 = TimeStamp::getSystemTime();
  vector<int32_t> data0;  for (int i = 0; i < 128; i++) data0.push_back(0);
  vector<int32_t> data1;  for (int i = 0; i < 128; i++) data1.push_back(i);
  vector<int32_t> data2;  for (int i = 0; i < 512; i++) data2.push_back(~i);

  BasicDataPacket *p0 = new BasicDataPacket();
  p0->setPacketType(PacketType_Data);
  p0->setStreamID("42");
  p0->setClassID("AB-CD-EF:0123.4567");
  p0->setTimeStamp(time0);
  p0->setDataInt(pf, data0);

  BasicDataPacket *p1 = new BasicDataPacket(*p0);
  p1->setDataInt(pf, data1);
  p1->resetForResend(time0.addSeconds(1));

  BasicDataPacket *p2 = new BasicDataPacket(*p0);
  p2->setDataInt(pf, data2);
  p2->resetForResend(time0.addSeconds(2));

  BasicContextPacket *c0 = new BasicContextPacket();
  c0->setPacketType(PacketType_Context);
  c0->setStreamID("42");
  c0->setClassID("AB-CD-EF:9876.5432");
  c0->setTimeStamp(time0);
  c0->setDataPayloadFormat(pf);
  c0->setTemperature(27.0f);

  BasicContextPacket *c1 = new BasicContextPacket(*c0);
  c0->setTemperature(28.0f);
  c0->resetForResend(time0.addSeconds(2));

  vector<BasicVRTPacket*> packets;
  packets.push_back(p0);
  packets.push_back(c0);
  packets.push_back(p1);
  packets.push_back(p2);
  packets.push_back(c1);

  return packets;
}

void AbstractVRAFileTest::deleteTestPacketVector (vector<BasicVRTPacket*> &v) {
  for (size_t i = 0; i < v.size(); i++) {
    delete v[i];
    v[i] = NULL;
  }
  v.resize(0);
}


string AbstractVRAFileTest::getTestedClass () {
  return testedClass;
}

vector<TestSet> AbstractVRAFileTest::init () {
  vector<TestSet> tests;

  fname1 = createTempFile("VRAFileTest", AbstractVRAFile::FILE_NAME_EXT);
  fname2 = createTempFile("VRAFileTest", AbstractVRAFile::FILE_NAME_EXT);

  string ok = "-"; // Set to "-" to disable tests if not OK

  if (!isNull(fname1) && !isNull(fname2)) {
    ok = ""; // GOOD
  }

  if (testedClass == "vrt::AbstractVRAFile") {
    tests.push_back(TestSet("DEFAULT_VERSION",       this,    "testConstants"));
    tests.push_back(TestSet("FILE_NAME_EXT",         this,   "+testConstants"));
    tests.push_back(TestSet("HEADER_LENGTH",         this,   "+testConstants"));
    tests.push_back(TestSet("MAX_VERSION_SUPPORTED", this,   "+testConstants"));
    tests.push_back(TestSet("MIME_TYPE",             this,   "+testConstants"));
    tests.push_back(TestSet("MIN_VERSION_SUPPORTED", this,   "+testConstants"));
    tests.push_back(TestSet("TRAILER_LENGTH",        this,   "+testConstants"));
    tests.push_back(TestSet("VRA_FAW",               this,   "+testConstants"));
    tests.push_back(TestSet("VRA_FAW_0",             this,   "+testConstants"));
    tests.push_back(TestSet("VRA_FAW_1",             this,   "+testConstants"));
    tests.push_back(TestSet("VRA_FAW_2",             this,   "+testConstants"));
    tests.push_back(TestSet("VRA_FAW_3",             this,   "+testConstants"));
  }
  if (true) {
    tests.push_back(TestSet("append(..)",            this, ok+"testAppend"));
    tests.push_back(TestSet("begin()",               this,   "+testGetThisPacket"));
    tests.push_back(TestSet("close()",               this,   "+testAppend"));
    tests.push_back(TestSet("end()",                 this,   "+testGetThisPacket"));
    tests.push_back(TestSet("equals(..)",            this,   "+testAppend"));
    tests.push_back(TestSet("flush()",               this,   "+testAppend"));
    tests.push_back(TestSet("getFileLength()",       this,   "+testAppend"));
    tests.push_back(TestSet("getHeader()",           this,   "+testAppend"));
    tests.push_back(TestSet("getThisPacket(..)",     this, ok+"testGetThisPacket"));
    tests.push_back(TestSet("getURI(..)",            this, ok+"testGetURI"));
    tests.push_back(TestSet("getVersion()",          this, ok+"testGetVersion"));
    tests.push_back(TestSet("gotoNextPacket(..)",    this,   "+testGetThisPacket"));
    tests.push_back(TestSet("hashCode()",            this,   "+testAppend"));
    tests.push_back(TestSet("iterator()",            this,   "+testAppend"));
    tests.push_back(TestSet("isCRCValid()",          this,   "+testAppend"));
    tests.push_back(TestSet("isFileValid(..)",       this,   "+testAppend"));
    tests.push_back(TestSet("setVersion(..)",        this,   "+testGetVersion"));
    tests.push_back(TestSet("toString()",            this, ok+"testToString"));
    tests.push_back(TestSet("updateFileLength()",    this,   "+testAppend"));
    tests.push_back(TestSet("updateCRC()",           this,   "+testAppend"));
  }

  return tests;
}

void AbstractVRAFileTest::done () {
  if (!isNull(fname1)) {
    if (remove(fname1.c_str()) != 0) {
      throw VRTException("Error deleting %s: %s", fname1.c_str(), ERRNO_STR);
    }
  }
  if (!isNull(fname2)) {
    if (remove(fname2.c_str()) != 0) {
      throw VRTException("Error deleting %s: %s", fname2.c_str(), ERRNO_STR);
    }
  }
  fname1 = "";
  fname2 = "";
}

void AbstractVRAFileTest::call (const string &func) {
  if (func == "testConstants"    ) testConstants();
  if (func == "testAppend"       ) testAppend();
  if (func == "testGetThisPacket") testGetThisPacket();
  if (func == "testGetURI"       ) testGetURI();
  if (func == "testGetVersion"   ) testGetVersion();
  if (func == "testToString"     ) testToString();
}

void AbstractVRAFileTest::testConstants () {
  int32_t expFAW = ((AbstractVRAFile::VRA_FAW_0 & 0xFF) << 24)
                 | ((AbstractVRAFile::VRA_FAW_1 & 0xFF) << 16)
                 | ((AbstractVRAFile::VRA_FAW_2 & 0xFF) <<  8)
                 | ((AbstractVRAFile::VRA_FAW_3 & 0xFF)      );

  // SHOULD BE CONSTANT
  assertEquals("FILE_NAME_EXT",         ".vra",                             AbstractVRAFile::FILE_NAME_EXT);
  assertEquals("MIME_TYPE",             "application/x-vita-radio-archive", AbstractVRAFile::MIME_TYPE);
  assertEquals("HEADER_LENGTH",         20,                                 AbstractVRAFile::HEADER_LENGTH);
  assertEquals("TRAILER_LENGTH",        0,                                  AbstractVRAFile::TRAILER_LENGTH);
  assertEquals("VRA_FAW",               expFAW,                             AbstractVRAFile::VRA_FAW);
  assertEquals("VRA_FAW_0",             (char)'V',                          AbstractVRAFile::VRA_FAW_0);
  assertEquals("VRA_FAW_1",             (char)'R',                          AbstractVRAFile::VRA_FAW_1);
  assertEquals("VRA_FAW_2",             (char)'A',                          AbstractVRAFile::VRA_FAW_2);
  assertEquals("VRA_FAW_3",             (char)'F',                          AbstractVRAFile::VRA_FAW_3);

  // MAY CHANGE IN FUTURE
  assertEquals("MAX_VERSION_SUPPORTED", (int8_t)1, AbstractVRAFile::MAX_VERSION_SUPPORTED);
  assertEquals("MIN_VERSION_SUPPORTED", (int8_t)1, AbstractVRAFile::MIN_VERSION_SUPPORTED);
  assertEquals("DEFAULT_VERSION",       (int8_t)1, AbstractVRAFile::DEFAULT_VERSION);

  // SANITY CHECKS
  assertEquals("DEFAULT_VERSION >= MIN_VERSION_SUPPORTED", true, AbstractVRAFile::DEFAULT_VERSION >= AbstractVRAFile::MIN_VERSION_SUPPORTED);
  assertEquals("DEFAULT_VERSION <= MAX_VERSION_SUPPORTED", true, AbstractVRAFile::DEFAULT_VERSION <= AbstractVRAFile::MAX_VERSION_SUPPORTED);
}

void AbstractVRAFileTest::testAppend () {
  // ==== CREATE TEST DATA ===================================================
  vector<BasicVRTPacket*> packets = AbstractVRAFileTest::makeTestPacketVector();

  // ==== DECLARE FILES ======================================================
  AbstractVRAFile *out1 = openFile(fname1, FileMode_Write);
  AbstractVRAFile *out2 = openFile(fname2, FileMode_Write, true, true, true);
  AbstractVRAFile *in1  = NULL;
  AbstractVRAFile *in1b = NULL;
  AbstractVRAFile *in2  = NULL;

  // ==== WRITE OUT FILES ==================================================
  // ---- Check Writing Packets --------------------------------------------
  int64_t len = AbstractVRAFile::HEADER_LENGTH;
  assertEquals("File 1 initial length", len, out1->getFileLength());
  assertEquals("File 2 initial length", len, out2->getFileLength());
  for (size_t i = 0; i < packets.size(); i++) {
    out1->append(*packets[i]);
    out2->append(*packets[i]);
    len += packets[i]->getPacketLength();

    assertEquals("File 1 length after writing packet", len, out1->getFileLength());
    assertEquals("File 2 length after writing packet", len, out2->getFileLength());

    out1->close();
    delete out1;
    out1 = openFile(fname1, FileMode_ReadWrite);
    assertEquals("File 1 length after ReadWrite open", len, out1->getFileLength());
  }

  // ---- Check Flush/Update -----------------------------------------------
  out1->flush();
  out2->flush();
  assertEquals("File 1 length after flush",  len,  out1->getFileLength());
  assertEquals("File 2 length after flush",  len,  out2->getFileLength());
  assertEquals("File 1 CRC after flush",     true, out1->isCRCValid());
  assertEquals("File 2 CRC after flush",     true, out2->isCRCValid());
  out1->updateCRC();
  assertEquals("File 1 CRC after update",    true, out1->isCRCValid());
  assertEquals("File 2 CRC after update",    true, out2->isCRCValid());
  out1->updateFileLength();
  assertEquals("File 1 length after update", len,  out1->getFileLength());
  assertEquals("File 2 length after update", len,  out2->getFileLength());

  // ---- Check Output Header ----------------------------------------------
  void*   header1    = out1->getHeaderPointer();
  void*   header2    = out2->getHeaderPointer();
  int32_t expVerWord = ((int32_t)AbstractVRAFile::DEFAULT_VERSION) << 24;
  //int32_t expCRC     = 0;

  assertEquals("Header 1 / Word 0",   AbstractVRAFile::VRA_FAW, VRTMath::unpackInt( header1, 0));
  assertEquals("Header 2 / Word 0",   AbstractVRAFile::VRA_FAW, VRTMath::unpackInt( header2, 0));
  assertEquals("Header 1 / Word 1",   expVerWord,               VRTMath::unpackInt( header1, 4));
  assertEquals("Header 2 / Word 1",   expVerWord,               VRTMath::unpackInt( header2, 4));
  assertEquals("Header 1 / Word 2-3", __INT64_C(0),             VRTMath::unpackLong(header1, 8));
  assertEquals("Header 2 / Word 2-3", len,                      VRTMath::unpackLong(header2, 8));
  assertEquals("Header 1 / Word 4",   BasicVRLFrame::NO_CRC,    VRTMath::unpackInt( header1,16));
  //assertEquals("Header 2 / Word 4",   expCRC,                   VRTMath::unpackInt( header2,16));

  out1->close();
  out2->close();

  // ==== READ IN FILES ====================================================
  in1  = openFile(fname1, FileMode_Read);
  in1b = openFile(fname1, FileMode_Read);
  in2  = openFile(fname2, FileMode_Read);

  // ---- Check Input Header -----------------------------------------------
  assertBufEquals("Input 1 Header",     header1, AbstractVRAFile::HEADER_LENGTH, in1->getHeaderPointer(), AbstractVRAFile::HEADER_LENGTH);
  assertBufEquals("Input 2 Header",     header2, AbstractVRAFile::HEADER_LENGTH, in2->getHeaderPointer(), AbstractVRAFile::HEADER_LENGTH);
  assertEquals(   "Input 1 Length",     len,     in1->getFileLength());
  assertEquals(   "Input 2 Length",     len,     in2->getFileLength());
  assertEquals(   "Input 1 CRC Valid",  true,    in1->isCRCValid());
  assertEquals(   "Input 2 CRC Valid",  true,    in2->isCRCValid());
  assertEquals(   "Input 1 File Valid", true,    in1->isFileValid());
  assertEquals(   "Input 2 File Valid", true,    in2->isFileValid());

  // ---- Check Input Packets ----------------------------------------------
  size_t i = 0;
  for (ConstPacketIterator pi = in1->begin(); pi != in1->end(); ++pi) {
    BasicVRTPacket *p = *pi;
    assertEquals("Input 1 Packets", *packets[i], *p);
    delete p;
    i++;
  }

  // ---- Check Input Equality ---------------------------------------------
  assertEquals("Input 1 == Input 1",  true,  in1->equals(*in1 ));
  assertEquals("Input 2 == Input 2",  true,  in2->equals(*in2 ));
  assertEquals("Input 1 != Input 2",  true, !in1->equals(*in2 )); // different CRC/len in header
  assertEquals("Input 1 == Input 1b", true,  in1->equals(*in1b)); // same underlying file


  if (out1 != NULL) { out1->close(); delete out1; }
  if (out2 != NULL) { out2->close(); delete out2; }
  if (in1  != NULL) {  in1->close(); delete in1;  }
  if (in1b != NULL) { in1b->close(); delete in1b; }
  if (in2  != NULL) {  in2->close(); delete in2;  }
  AbstractVRAFileTest::deleteTestPacketVector(packets);
}

void AbstractVRAFileTest::testGetThisPacket () {
  AbstractVRAFile *file1 = openFile(fname1, FileMode_Write);
  AbstractVRAFile *file2 = openFile(fname2, FileMode_Write);
  vector<BasicVRTPacket*> packets = AbstractVRAFileTest::makeTestPacketVector();

  for (size_t i = 0; i < packets.size(); i++) {
    file1->append(*packets[i]);
  }

  for (ConstPacketIterator pi = file1->begin(); pi != file1->end(); ++pi) {
    BasicVRTPacket *p   = *pi;
    BasicVRTPacket *pkt = file1->getThisPacket(pi,false);
    BasicVRTPacket *nil = file1->getThisPacket(pi,true);
    assertEquals("Input 1 Packets", *p,          *pkt);
    assertEquals("Input 1 Packets", (void*)NULL, (void*)nil);
    delete p;
    delete pkt;

    ConstPacketIterator piA = pi;    ++piA;
    ConstPacketIterator piB = pi;    piB++;
    ConstPacketIterator piC = pi;    file1->gotoNextPacket(piC);

    assertEquals("Input 1 Prefix ++ / Postfix ++",         piA, piB);
    assertEquals("Input 1 Prefix ++ / gotoNextPacket(..)", piA, piC);
  }

  assertEquals("Input 2 Empty", file2->begin(), file2->end());

  file1->close();
  file2->close();

  delete file1;
  delete file2;
  AbstractVRAFileTest::deleteTestPacketVector(packets);
}

void AbstractVRAFileTest::testGetURI () {
  AbstractVRAFile *file1 = openFile(fname1, FileMode_Write);
  AbstractVRAFile *file2 = openFile(fname2, FileMode_Write);

  string uri1 = file1->getURI();
  string uri2 = file2->getURI();

  // The set of checks we can do here in C++ are limited since a lot is system-specific
  // so just check the very basics.
  assertEquals("File 1 URI (starts with 'file:')", (size_t)0,     uri1.find("file:"));
  assertEquals("File 1 URI (ends with '.vra')",    uri1.size()-4, uri1.find(".vra" ));
  assertEquals("File 2 URI (starts with 'file:')", (size_t)0,     uri2.find("file:"));
  assertEquals("File 2 URI (ends with '.vra')",    uri2.size()-4, uri2.find(".vra" ));

  file1->close();
  file2->close();

  delete file1;
  delete file2;
}

void AbstractVRAFileTest::testGetVersion () {
  AbstractVRAFile *file1 = openFile(fname1, FileMode_Write);
  AbstractVRAFile *file2 = openFile(fname2, FileMode_Write);

  assertEquals("File 1 default version", (int32_t)AbstractVRAFile::DEFAULT_VERSION, file1->getVersion());
  assertEquals("File 2 default version", (int32_t)AbstractVRAFile::DEFAULT_VERSION, file2->getVersion());

  // SET FILE 1 TO VALID VERSIONS
  for (int32_t i = AbstractVRAFile::MIN_VERSION_SUPPORTED; i <= AbstractVRAFile::MAX_VERSION_SUPPORTED; i++) {
    file1->setVersion(i);
    assertEquals("File 1 setVersion(..)", i, file1->getVersion());
  }

  // SET FILE 2 TO INVALID VERSIONS (MAKE SURE IT DOESN"T CHANGE)
  for (int32_t i = 0; i < AbstractVRAFile::MIN_VERSION_SUPPORTED; i++) {
    try {
      file2->setVersion(i);
      assertEquals("File 2 setVersion(..) with invalid version", false, true);
    }
    catch (VRTException e) {
      UNUSED_VARIABLE(e);
      assertEquals("File 2 setVersion(..) with invalid version", true, true);
    }
  }
  for (int32_t i = AbstractVRAFile::MAX_VERSION_SUPPORTED; i < AbstractVRAFile::MAX_VERSION_SUPPORTED+10; i++) {
    try {
      file2->setVersion(i);
      assertEquals("File 2 setVersion(..) with invalid version", false, true);
    }
    catch (VRTException e) {
      UNUSED_VARIABLE(e);
      assertEquals("File 2 setVersion(..) with invalid version", true, true);
    }
  }
  assertEquals("File 2 version unmodified", (int32_t)AbstractVRAFile::DEFAULT_VERSION, file2->getVersion());

  file1->close();
  file2->close();

  delete file1;
  delete file2;
}

void AbstractVRAFileTest::testToString () {
  AbstractVRAFile *file1 = openFile(fname1, FileMode_Write);
  AbstractVRAFile *file2 = openFile(fname2, FileMode_Write);

  vector<BasicVRTPacket*> packets = AbstractVRAFileTest::makeTestPacketVector();
  for (size_t i = 0; i < packets.size(); i++) {
    file2->append(*packets[i]);
  }

  // Output string is free-form, so just make sure call works and doesn't return null
  assertEquals("File 1 toString() != null", true, !isNull(file1->toString()));
  assertEquals("File 2 toString() != null", true, !isNull(file2->toString()));

  file1->close();
  file2->close();

  delete file1;
  delete file2;
  AbstractVRAFileTest::deleteTestPacketVector(packets);
}
