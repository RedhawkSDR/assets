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

#include "PacketStream.h"
#include <time.h>

using namespace std;

static const int64_t MAX_MISSING_SECONDS        = 0;

static const int32_t CLASSID_OUI                = 0x40000001;
static const int32_t CLASSID_INFO_CLASS_CODE    = 0x40000002;
static const int32_t CLASSID_PACKET_CLASS_CODE  = 0x40000003;

static const midasKeywordMapping defaultFieldMapping[] = {
    { "FrequencyRF", "VRF", false },
    { "FrequencyRF", "SBT_FREQ", false },
    { "FrequencyIF", "IF", false },
    { "SampleRate", "FS", false },
    { "SamplePeriod", "PERIOD", false },
    { "TimeStamp.MidasTime", "SBT_TIME", false },
    { "ChangePacket", "STATUSCHANGE", false },
    { "Bandwidth", "BW", false },
    { "StreamID", "VRT_STREAM_ID", false },
    { "ClassID", "VRT_CLASS_ID", false },
    { "GeolocationGPS.Manufacturer", "GPS_MAN_OUI", false },
    { "GeolocationGPS.Latitude", "GPS_LAT", false },
    { "GeolocationGPS.Longitude", "GPS_LON", false },
    { "GeolocationGPS.Altitude", "GPS_ALT", false },
    { "GeolocationGPS.TimeStamp.MidasTime", "GPS_TIME", false },
    { "EphemerisECEF.PositionX", "ECEF_POS_X", false },
    { "EphemerisECEF.PositionY", "ECEF_POS_Y", false },
    { "EphemerisECEF.PositionZ", "ECEF_POS_Z", false },
    { "EphemerisECEF.VelocityX", "ECEF_VEL_X", false },
    { "EphemerisECEF.VelocityY", "ECEF_VEL_Y", false },
    { "EphemerisECEF.VelocityZ", "ECEF_VEL_Z", false },
    { "EphemerisECEF.AccelerationX", "ECEF_ACC_X", false },
    { "EphemerisECEF.AccelerationY", "ECEF_ACC_Y", false },
    { "EphemerisECEF.AccelerationZ", "ECEF_ACC_Z", false },
    { "EphemerisECEF.TimeStamp.MidasTime", "ECEF_TIME", false },
    { "GeoSentences.Sentences", "GPS_ASCII", false },
    { "DeviceID", "MANUFACTURER_OUI", false },
    { "Gain1", "GAIN1", false },
    { "Gain2", "GAIN2", false },
    { "Gain", "TOTAL_GAIN", false },
    { "UserDefinedBits", "USER_DEFINED_BITS", false },
    { "DataValid", "DATA_VALID", false },
    { "SignalDetected", "DETECTED_SIGNAL", false },
};

static int32_t m_get_uqualifier2 (HCB &in, string qualifier, fstring<2> &value) {
  string fname   = in.file_name;
  string qualStr = "("+qualifier+"=";
  size_t index   = fname.find(qualStr);       if (index == string::npos) return m_get_uqualifier(in, qualifier, value);
  size_t start   = index + qualStr.length();
  size_t end     = fname.find(")", start);    if (index == string::npos) return m_get_uqualifier(in, qualifier, value);

  value = fname.substr(start, end);
  return end - start;
}

map<string,string> PacketStream::getDefaultFieldMap() {
  map<string,string> map;
  for (int i=0; i<sizeof(defaultFieldMapping)/sizeof(midasKeywordMapping); i++) {
    string midasKey = string(defaultFieldMapping[i].vrtField);
    string vrtField = string(defaultFieldMapping[i].midasKeyword);
    map[midasKey] = vrtField;
  }
  return map;
}

PacketStream::~PacketStream () {
  // done
}

PacketStream::PacketStream () :
  hcb(),
  pipe(hcb, true),
  currentState(),
  transferLengthBytes(-1),
  transferLengthElem(-1),
  defTransferLength(-1),
  streamID(INT32_NULL),
  packetMode(false),
  force1000(false),
  nextPacketTimeStamp(),
  lastCut(),
  counter(0),
  newContext(true),
  initialized(false),
  archivePeriod(-1),
  archiveCutNo(0),
  outFileName(""),
  dataBuffer(BasicVRTPacket::MAX_PACKET_LENGTH),
  keyBuffer(XMValue::TABLE),
  exactTopOfSecond(false),
  openOnNextDataPacket(false),
  debug(false),
  keywords(),
  numFiles(-1),
  fieldMapping(getDefaultFieldMap()),
  datastreamInitialized(false),
  writeActive(!exactTopOfSecond || archivePeriod < 0),
  archiveMissingSamples(0),
  totalMissingSamples(0)
{
  initKeywords();
  m_initialize(pipe);
  pipe.file_name = "";
}

PacketStream::PacketStream (const string &in, int32_t streamID, bool force1000, bool packetMode, int32_t xfer, bool debug, map<string,string> fieldMapping) :
  hcb(),
  pipe(hcb, true),
  currentState(),
  transferLengthBytes(-1),
  transferLengthElem(-1),
  defTransferLength(xfer),
  streamID(streamID),
  packetMode(packetMode),
  force1000(force1000),
  nextPacketTimeStamp(),
  lastCut(),
  counter(0),
  newContext(true),
  initialized(false),
  archivePeriod(-1),
  archiveCutNo(0),
  outFileName(NULL),
  dataBuffer(BasicVRTPacket::MAX_PACKET_LENGTH),
  keyBuffer(XMValue::TABLE),
  exactTopOfSecond(false),
  openOnNextDataPacket(false),
  firstDataPacketReceived(false),
  keywords(""),
  numFiles(-1),
  debug(debug),
  fieldMapping(fieldMapping),
  datastreamInitialized(false),
  writeActive(!exactTopOfSecond || archivePeriod < 0),
  archiveMissingSamples(0),
  totalMissingSamples(0)

{
  initKeywords();
  openBlueToVrt(in);
}

PacketStream::PacketStream (const string &out, int32_t streamID, bool force1000, bool packetMode, double archivePeriod, const BasicContextPacket &ctx, string keywords, int32_t numFiles, bool debug, bool exactTopOfSecond, map<string,string> fieldMapping) :
  hcb(),
  pipe(hcb, true),
  currentState(),
  transferLengthBytes(-1),
  transferLengthElem(-1),
  defTransferLength(-1),
  streamID(streamID),
  packetMode(packetMode),
  force1000(force1000),
  nextPacketTimeStamp(ctx.getTimeStamp()),
  lastCut(ctx.getTimeStamp()),
  counter(0),
  newContext(true),
  initialized(false),
  archivePeriod(archivePeriod),
  archiveCutNo(0),
  outFileName(out),
  dataBuffer(BasicVRTPacket::MAX_PACKET_LENGTH),
  keyBuffer(XMValue::TABLE),
  exactTopOfSecond(exactTopOfSecond || archivePeriod == (int)archivePeriod),
  openOnNextDataPacket(false),
  firstDataPacketReceived(false),
  keywords(keywords),
  numFiles(numFiles),
  debug(debug),
  fieldMapping(fieldMapping),
  datastreamInitialized(false),
  writeActive(!this->exactTopOfSecond || archivePeriod < 0),
  archiveMissingSamples(0),
  totalMissingSamples(0)

{
  initialize(ctx);
  initKeywords();
}

PacketStream::PacketStream (const string &out, int32_t streamID, bool force1000, bool packetMode, double archivePeriod, string keywords, int32_t numFiles, bool debug, bool exactTopOfSecond, map<string,string> fieldMapping) :
  hcb(),
  pipe(hcb, true),
  currentState(),
  transferLengthBytes(-1),
  transferLengthElem(-1),
  defTransferLength(-1),
  streamID(streamID),
  packetMode(packetMode),
  force1000(force1000),
  nextPacketTimeStamp(),
  lastCut(),
  counter(0),
  newContext(true),
  initialized(false),
  archivePeriod(archivePeriod),
  archiveCutNo(0),
  outFileName(out),
  dataBuffer(BasicVRTPacket::MAX_PACKET_LENGTH),
  keyBuffer(XMValue::TABLE),
  exactTopOfSecond(exactTopOfSecond || archivePeriod == (int)archivePeriod),
  openOnNextDataPacket(false),
  firstDataPacketReceived(false),
  keywords(keywords),
  numFiles(numFiles),
  debug(debug),
  fieldMapping(fieldMapping),
  datastreamInitialized(false),
  writeActive(!this->exactTopOfSecond || archivePeriod < 0),
  archiveMissingSamples(0),
  totalMissingSamples(0)

{
  initKeywords();
}

void PacketStream::initKeywords() {
  for (map<string,string>::iterator entry = fieldMapping.begin(); entry != fieldMapping.end(); entry++) {
    vrtToBlueKeywordMapping[entry->first] = entry->second;
    blueToVrtKeywordMapping[entry->second] = entry->first;
  }
}

void PacketStream::addStaticKeyword(XMValue xmv, string key) {
  XMValue::Type_t type = xmv.type();
  switch (type) {
  case XMValue::ASCII:
  case XMValue::INT_1:
  case XMValue::INT_U1:
  case XMValue::INT_2:
  case XMValue::INT_4:
  case XMValue::INT_8:
  case XMValue::REAL_4:
  case XMValue::REAL_8:
  {
    m_put_keydata(pipe,key,xmv.data(),xmv.databytes(),0,type);
    break;
  }
  case XMValue::LIST:
  {
    XMValue::List& l = xmv.as<XMValue::List>();
    int j=0;
    for (XMValue::List::iterator jj = l.begin(); jj != l.end(); ++jj) {
      stringstream l_key;
      l_key << key << j++;
      addStaticKeyword(*jj,l_key.str());
    }
    break;
  }
  case XMValue::KVLIST:
  {
    XMValue::KVList& kvl = xmv.as<XMValue::KVList>();
    if (key.size() > 0) {
      for (XMValue::KVList::iterator jj = kvl.begin(); jj != kvl.end(); ++jj) {
        string kvl_key = key + "." + jj->first;
        addStaticKeyword(jj->second,kvl_key);
      }
    }
    else {
      // SHOULD NEVER HAPPEN
    }
    break;
  }
  case XMValue::TABLE:
  {
    XMValue::Table& t = xmv.as<XMValue::Table>();
    for (XMValue::Table::iterator ii = t.begin(); ii != t.end(); ++ii) {
      string tbl_key = ii->first;
      if (key.size() > 0) {
        tbl_key = key + "." + tbl_key;
      }
      addStaticKeyword(ii->second,tbl_key);
    }
    break;
  }
  }
}
string PacketStream::generateFileName(const TimeStamp& timeStamp, string fileName) const{
  string str(fileName);

  while (true) {
    size_t iStart = str.find("${");
    if (iStart == string::npos) { break; }
    size_t iEnd = str.find("}");
    if (iEnd == string::npos) { throw VRTException("Archive Filename Pattern contains unmatched {}'s: %s", fileName.c_str()); }
    string key = str.substr(iStart+2,iEnd-iStart-2);
    transform(key.begin(), key.end(), key.begin(), ::toupper);

    if (key == "CUTNO") {
      int32_t c = archiveCutNo;
      ostringstream ss;
      ss << c;
      string cutNo = ss.str();
      str.replace(iStart,iEnd-iStart+1, cutNo);
    }
    else if (key == "DATE") {
      TimeStamp ts = timeStamp;
      string pattern = "%Y%m%d";
      string date = ts.toStringUTC(pattern);
      str.replace(iStart,iEnd-iStart+1,date);
    }
    else if (key == "TIME") {
      TimeStamp ts = timeStamp;
      string pattern = "%H%M%S";
      string time = ts.toStringUTC(pattern);
      str.replace(iStart,iEnd-iStart+1,time);
    }
    else if (key == "FREQ") {
      if (!isNull(currentState)) {
        uint64_t f = (uint64_t)currentState.getFrequencyRF();
        ostringstream ss;
        ss << f;
        string freq = ss.str();
        str.replace(iStart,iEnd-iStart+1,freq);
      }
      else {
        str.replace(iStart,iEnd-iStart+1,"UNKNOWN");
      }
    }
    else {
      throw VRTException("Archive Filename Pattern contains unknown identifier: %s", fileName.c_str());
    }
  }
  return str;
}

void PacketStream::openArchiveFile(const TimeStamp& timeStamp) {
    openOnNextDataPacket = false;
    if (writeActive) {
  //  cout<<"OPENING!!!"<<endl;
  //  cout<<"EXACT TOP OF SECOND: "<<exactTopOfSecond<<" CUTPERIOD: "<<archivePeriod<<endl;
      // GENERATE NEW FILENAME AND OPEN NEW FILE
      string fileName = generateFileName(timeStamp, outFileName);
      openVrtToBlue(fileName, timeStamp);

      // WRITE KEYWORDS
      addStaticKeyword(keyBuffer);

      // FOR DEBUG PURPOSES
      string key = "TIMESTAMP_INTEGER";
      int32_t tsi = timeStamp.getTimeStampInteger();
      m_put_keydata(pipe,key,(void*)&tsi,4,0,XMValue::INT_4);
      key = "TIMESTAMP_FRACTIONAL";
      int64_t tsf = timeStamp.getTimeStampFractional();
      m_put_keydata(pipe,key,(void*)&tsf,8,0,XMValue::INT_8);


      if (keywords.size() > 0) {
        XMValue v;
        if (m_vrfind(keywords, v)) {
          addStaticKeyword(v);
        }
      }

      addDynamicKeywords();

      archiveCutNo++;

      if (debug) {
        ostringstream oss;
        oss<<"CUTTING NEW FILE FOR ";
        if (streamID == ALLSTREAMS)
          oss<<"ALLSTREAMS ";
        else
          oss<<"STREAM "<<streamID;
        oss<<" '"<<fileName<<"' at time "<<timeStamp;
        oss<<" TOTAL MISSING SAMPLES: "<<totalMissingSamples;
        cout<<oss.str()<<endl;
      }
    }
    lastCut = timeStamp;

    int64_t samplesToAdd = currentState.getSampleRate() * archivePeriod;

    if (timeStamp.getFractionalMode() == FractionalMode_SampleCount) {
      double sr = currentState.getSampleRate();
      if (exactTopOfSecond) {
        uint64_t a_sr = archivePeriod * sr;
        uint64_t tsf = lastCut.getTimeStampFractional() + sr * (lastCut.getTimeStampInteger() - firstCut.getTimeStampInteger());
        nextCut = lastCut.addSamples(a_sr - (tsf % a_sr), sr);
      }
      else {
        nextCut = lastCut.addSamples((uint64_t)(archivePeriod * sr), sr);
      }
    }
    else {
      if (exactTopOfSecond) {
        uint64_t tsf = (int64_t)lastCut.getTimeStampFractional() - (int64_t)firstCut.getTimeStampFractional() + TimeStamp::ONE_SEC * (lastCut.getTimeStampInteger() - firstCut.getTimeStampInteger());
        uint64_t a_sr = floor(archivePeriod * TimeStamp::ONE_SEC + 0.5);
        uint64_t add = a_sr - (tsf % a_sr);
        double sr = currentState.getSampleRate();
        double oneSample = 1 / sr * TimeStamp::ONE_SEC;
        if (add < oneSample)
            add = add + a_sr;
//        cout<<"TSF: "<<tsf<<" A_SR: "<<a_sr<<" ADD: "<<add<<endl;
        nextCut = lastCut.addPicoSeconds(add);
      }
      else {
        nextCut = lastCut.addPicoSeconds(floor(TimeStamp::ONE_SEC * archivePeriod + 0.5));
      }
    }
}

void PacketStream::closeArchiveFile() {
  // CLOSE FILE
  close();

  openOnNextDataPacket = true;
}

void PacketStream::addDynamicKeywords() {
  for (map<string,string>::iterator entry = fieldMapping.begin(); entry != fieldMapping.end(); entry++) {
    string midasKey = entry->first;
    string vrtField = entry->second;
    try {
      Value * f = currentState.getFieldByName(vrtField);
      if (!f->isNullValue())
        m_put_key_vrtvalue(pipe, midasKey, *f);
      delete f;
    }
    catch (VRTException e) {
      cout<<"EXCEPTION: "<<e<<endl;
    }
  }
}

void PacketStream::openVrtToBlue (const string &out, const TimeStamp& ts) {
  m_initialize(pipe);
  pipe.file_name = out;
  if (pipe.file_name[0] == '_') {
    // Workaround for X-Midas DR 501655-5 (Pipe names are occasionally case-sensitive)
    m_uppercase(pipe.file_name);
  }
  
  if (packetMode) {
    pipe.type     = 1000;
    pipe.format   = "SB";
    pipe.data_rep = "IEEE";
    m_open(pipe, HCBF_OUTPUT);
  }
  else if (isNull(currentState)) {
    m_get_uqualifier2(pipe, "FC", pipe.format); // without this the FC= qualifier will be ignored
    // regardless of what host is, use big endian
    pipe.data_rep = "IEEE";
    pipe.format   = "SB";
    m_open(pipe, HCBF_OUTPUT);
//    reformat = ' ';//pipe.format[1];
  }
  else {
    double        sr = currentState.getSampleRate();
    PayloadFormat pf = currentState.getDataPayloadFormat();
//    TimeStamp     ts = currentState.getTimeStamp(); // MSM: This will have to change to reflect the last cut time, not the context packet

    if (isNull(sr)) throw VRTException("Missing SampleRate in "+currentState.toString());
    if (isNull(pf)) throw VRTException("Missing DataPayloadFormat in "+currentState.toString());
    if (isNull(ts)) throw VRTException("Missing TimeStamp in "+currentState.toString());

    int32_t fs = (force1000)? 0 : pf.getVectorSize();

    XMTime  t  = toTimeObject(ts, currentState.getSampleRate());

    if (fs == 1) fs = 0;
    m_put_epoch(pipe, t.integralSeconds(), t.fractionalSeconds(), true);

    // regardless of what host is, use big endian
    pipe.data_rep = "IEEE";

    pipe.format   = toBlueFormat(pf);
//    reformat      = pipe.format[1];

    pipe.subsize  = fs;
    pipe.size     = 0;
    pipe.type     = 1000;
    pipe.xunits   = 1; // TIME_S
    pipe.xstart   = 0.0;
    pipe.xdelta   = 1.0 / sr; // Hz -> sec
    
    if (fs > 1) {
      // Type 2000 file, set secondary axis
      double freq = currentState.getFrequencyIF();
      double bw   = currentState.getBandwidth();

      if (!isNull(freq) && !isNull(bw)) {
        pipe.type     = 2000;
        pipe.yunits   = 1; // TIME_S
        pipe.ystart   = 0.0;
        pipe.ydelta   = 1.0 / sr; // Hz -> sec
        pipe.xunits   = 3; // FREQUENCY_HZ
        pipe.xstart   = freq - bw/2;
        pipe.xdelta   = bw / fs;
      }
    }
//    nextPacketTimeStamp = ts;
    m_get_uqualifier2(pipe, "FC", pipe.format); // without this the FC= qualifier will be ignored
    m_open(pipe, HCBF_OUTPUT);

    // Set back to big endian since midas doesn't let us set it before opening file
    pipe.data_rep = "IEEE";
    m_update_header(pipe);

  }

}

void PacketStream::openBlueToVrt (const string &in) {
  m_initialize(pipe);
  pipe.file_name = in;
  if (pipe.file_name[0] == '_') {
    // Workaround for X-Midas DR 501655-5 (Pipe names are occasionally case-sensitive)
    m_uppercase(pipe.file_name);
  }
  m_open(pipe, HCBF_INPUT);
  if (force1000) m_force1000(pipe);
  
//  context       = contextPacketFor(pipe);
//  nextTimeStamp = currentState.getTimeStamp();

  if (packetMode) {
    transferLengthBytes = 4;
  }
  else if ((pipe.type / 1000) == 2) {
    int32_t frameSize = (int32_t)pipe.dbpe; // FrameSize in bytes
    transferLengthBytes = (defTransferLength / frameSize) * frameSize;
    if (transferLengthBytes <= 0) {
      throw VRTException("Transfer length of %d bytes is less than one frame in given input pipe", defTransferLength);
    }
  }
  else {
    transferLengthBytes = defTransferLength;
  }
  transferLengthElem = transferLengthBytes / pipe.dbpe;
}

void PacketStream::close () {
  string key = "MISSING_SAMPLES";
  m_put_keydata(pipe,key,(void*)&archiveMissingSamples,4,0,XMValue::INT_4);
  archiveMissingSamples = 0;

  m_close(pipe);
}

string PacketStream::toString () const {
  ostringstream str;
  str << "PacketStream:";
  str << " pipe="          << pipe.file_name;
  str << " currentState="       << currentState;
  if (isNull(streamID)) str << " stream=ALL";
  else                  str << " stream=" << streamID;
  str << " packetMode="    << packetMode;
  str << " nextTimeStamp=" << nextPacketTimeStamp;
  return str.str();
}

bool PacketStream::isNullValue () const {
  return (pipe.file_name == "");
}

int64_t PacketStream::isTimeToCut(const TimeStamp& ts) const {
  if (archivePeriod < 0) return 0;
  try{
    int32_t ntsi = ts.getTimeStampInteger();
    int64_t ntsf = ts.getTimeStampFractional();
    int32_t ltsi = nextCut.getTimeStampInteger();
    int64_t ltsf = nextCut.getTimeStampFractional();

//    cout<<"TS: "<<ts<<" NC: "<<nextCut<<endl;

    if ((ntsi > ltsi) || (ntsi == ltsi && ntsf > ltsf)) {
      int32_t timeDeltaSec = ntsi - ltsi;
      int64_t r;
      if (ts.getFractionalMode() == FractionalMode_SampleCount) {
        r = (ntsf - ltsf) + (int64_t)(timeDeltaSec * currentState.getSampleRate());
      }
      else {
        int64_t timeDeltaPS = ntsf - ltsf + (timeDeltaSec * TimeStamp::ONE_SEC);
        r = ceil(timeDeltaPS * currentState.getSampleRate() / TimeStamp::ONE_SEC);
      }
      return r > 0 ? r : 0;
    }
    else {
      return 0;
    }
  }
  catch (...){
    std::cout<< "caught..."<<std::endl;
    return false;
  }
  return false;
}

int64_t PacketStream::getMissingSamples(TimeStamp& ts) {
  int64_t missingSamples;
  int64_t timeDeltaSec = (int64_t)ts.getSecondsUTC()  - (int64_t)nextPacketTimeStamp.getSecondsUTC();
  if (ts.getFractionalMode() == FractionalMode_SampleCount) {
    missingSamples = ((int64_t)ts.getTimeStampFractional() - (int64_t)nextPacketTimeStamp.getTimeStampFractional()) + (int64_t)((int64_t)timeDeltaSec * currentState.getSampleRate());
  }
  else {
    int64_t timeDeltaPS    = (int64_t)ts.getTimeStampFractional() - (int64_t)nextPacketTimeStamp.getTimeStampFractional() + (int64_t)(timeDeltaSec * TimeStamp::ONE_SEC);
    missingSamples = (int64_t)(timeDeltaPS * currentState.getSampleRate() / TimeStamp::ONE_SEC);
  }
//  if (missingSamples != 0) {
//    cout<<"GOT MISSING SAMPLES: "<<missingSamples<<endl;
//  }
  return missingSamples;
}

bool PacketStream::writeToPipe(PayloadFormat pf, TimeStamp ts, char* data, int32_t nelem, bool missing) {
  int64_t afterCut = 0;
  bool cut = (afterCut = isTimeToCut(nextPacketTimeStamp)) > 0;
  if (afterCut < nelem)
    if (writeActive) {
      if (missing) {
        archiveMissingSamples += (nelem - afterCut);
      }
//      cout<<"DOING INITIAL WRITE"<<endl;
      // Set to machine data rep so that m_filad won't swap bytes
      pipe.data_rep = Mc->machine;
//      cout<<"0NELEM: "<<(nelem-afterCut)<<endl;
      m_filad(pipe, (void*)&data[0], nelem - afterCut);
      pipe.data_rep = "IEEE";
    }
  if (cut) {
    if (writeActive){
      if (numFiles > 0 && archiveCutNo > (numFiles-1)) {
        return true;
      }
//      cout<<"CUTCLOSARCH"<<endl;
      closeArchiveFile();
      if (missing) {
        archiveMissingSamples += afterCut;
      }
    }
    //    cout<<"ADDING SAMPLES"<<endl;
    ts = ts.addSamples(nelem - afterCut,currentState.getSampleRate());
    if (!writeActive) {
//      cout<<"SETTING WRITEACTIVE TRUE"<<endl;
      firstCut = ts;
      writeActive = true;
    }
//    cout<<"CUTTING NEW FILE IN MIDDLE, THEN WRITING "<<(nelem - afterCut)<<" len: "<<nelem<<" afterCut: "<<afterCut<<endl;

    openArchiveFile(ts);
//    cout<<"DONE OPEN ARCH"<<endl;

//    cout<<"NEW TIMESTAMP INTEGER: "<<ts.getTimeStampInteger()<<endl;
    // Set to machine data rep so that m_filad won't swap bytes
//    cout<<"WRITE FILAD"<<endl;
    pipe.data_rep = Mc->machine;
//    cout<<"1NELEM: "<<(afterCut)<<endl;
    m_filad(pipe,(void*)&data[nelem*(pf.isComplex() ? 2 : 1)*pf.getDataItemSize()/8], afterCut);
    pipe.data_rep = "IEEE";
//    cout<<"WROTE FILAD"<<endl;
  }
  return false;
}

bool PacketStream::writeToPipe(const BasicDataPacket& dp, bool fillMode) {
  TimeStamp ts = dp.getTimeStamp();

  if (isNull(ts)) {
    m_warning("No TimeStamp in "+dp.toString());
    return false;
  }

  if (openOnNextDataPacket){
    cout<<"OPENING ON ENXT"<<endl;
    openArchiveFile(ts);
  }

  double sr = currentState.getSampleRate();

  if (!datastreamInitialized) {
    writeActive = !exactTopOfSecond || archivePeriod < 0;
//    cout<<"ETOS: "<<exactTopOfSecond<<" WRITE ACTIVE: "<<writeActive<<" ARCH PER: "<<archivePeriod<<endl;
//    cout<<"WRITEACTIVE: "<<writeActive<<endl;
    // SET NEXT CUT
    if (archivePeriod > 0) {
      if (exactTopOfSecond) {
//        cout<<"ETOS!!!"<<endl;
        if (ts.getFractionalMode() == FractionalMode_SampleCount) {
          uint64_t a_sr = sr;
//          cout<<"SR: "<<sr<<endl;
          nextCut = ts.addSamples(a_sr - ts.getTimeStampFractional(), sr);
//          cout<<"CURRENT TIME IS " << ts.toString() << ", SETTING NEXT CUT TO " << nextCut.toString() << endl;
        }
        else {
          uint64_t a_sr = TimeStamp::ONE_SEC;
          nextCut = ts.addPicoSeconds(a_sr - ts.getTimeStampFractional());
//          cout<<"CURRENT TIME IS " << ts.toString() << ", SETTING NEXT CUT TO " << nextCut.toString() << endl;
        }
      }
      else {
        nextCut = ts.addSamples(archivePeriod * sr, sr);
//        cout<<"CURRENT TIME IS " << ts.toString() << ", SETTING NEXT CUT TO " << nextCut.toString() << endl;
      }
//      cout<<"SET NEXTCUT TO "<<nextCut<<endl;
      firstCut = nextCut;
    }

    // SET NEXT PACKET TIMESTAMP SO WE DON'T THINK SAMPLES ARE MISSING
    nextPacketTimeStamp = ts;
    datastreamInitialized = true;
  }

  // GET NUMBER OF MISSING SAMPLES
//  cout<<"GETTING MISSING"<<endl;
  int64_t missingSamples = getMissingSamples(ts);
  PayloadFormat   pf = currentState.getDataPayloadFormat();

  if (missingSamples == 0) {
    // PERFECT!
  }
  else if (missingSamples > 0) {
    totalMissingSamples += missingSamples;
    if (fillMode) {
      if (missingSamples < (int64_t)sr) {
        TimeStamp ts1 = nextPacketTimeStamp;
        cout<<"FILLING IN "<<missingSamples<<" SAMPLES at time " << ts1.toString()<<" ON FILE: "<<pipe.file_name<<endl;
        int32_t bytesPerSample = (pf.isComplex() ? 2 : 1) * pf.getDataItemSize()/8;
        int32_t missingBytes = missingSamples * bytesPerSample;
        int32_t missingSamplesPerEMPTY = EMPTY.size() / bytesPerSample;

        while (missingBytes > EMPTY.size()) {
//          cout<<"WRITING " <<(EMPTY.size() / bytesPerSample)<<" EMPTY SAMPLES AT TIME "<<ts1.toString()<<endl;
          nextPacketTimeStamp = ts1.addSamples(missingSamplesPerEMPTY, sr);
          if (writeToPipe(pf, ts1, (char*)&EMPTY[0], missingSamplesPerEMPTY, true)){
            close();
            return true;
          }
          ts1 = nextPacketTimeStamp;
          missingBytes -= EMPTY.size();
        }
//        cout<<"WRITING " <<(missingBytes / bytesPerSample)<<" EMPTY SAMPLES AT TIME "<<ts1.toString()<<endl;
        nextPacketTimeStamp = ts1.addSamples(missingBytes / bytesPerSample, sr);
        if (writeToPipe(pf, ts1, (char*)&EMPTY[0], missingBytes / bytesPerSample, true)){
          close();
          return true;
        }
        ts1 = nextPacketTimeStamp;
        nextPacketTimeStamp = ts;
//        cout<<"nextPacketTimeStamp IS " <<nextPacketTimeStamp.toString()<<endl;
//        cout<<"ts1 IS " <<ts1.toString()<<endl;
      }
      else if (archivePeriod > 0) {
        cout<<"TOO MANY MISSING SAMPLES TO BACK-FILL, STARTING NEW ARCHIVE FILE"<<endl;

        closeArchiveFile();
        openArchiveFile(ts);
        // TOO MANY SAMPLES TO FILL
      }
      else {
        cout<<"TOO MANY MISSING SAMPLES TO BACK-FILL"<<endl;
      }
    }
  }
  else if (missingSamples < 0) {
    m_warning("Expected to receive packet with TimeStamp="+nextPacketTimeStamp.toString()+" but got TimeStamp="
             +ts.toString()+" dropping packet. This error is usually caused by an improperly-reported "
             +"SampleRate.");
    close();
    return true;
  }

  int64_t samp = dp.getDataLength(pf);
  nextPacketTimeStamp = ts.addSamples(samp,sr);

  // UPDATE STATE
  updateState(dp);

  int32_t len = dp.getDataLength(pf);

  dp.getData(pf, dataBuffer, 0, false);
//  cout<<"WRITING TO PIPE"<<endl;
  if (writeToPipe(pf, ts, (char*) &dataBuffer[0], len)){
    close();
    return true;
  }
//  cout<<"DONE WITH PIPE"<<endl;
  return false;
}

  /**  Writes data from the packet to the pipe. */
bool PacketStream::writeToPipe (const BasicVRTPacket& p, bool packetMode, bool fillMode) {
    PacketType type = p.getPacketType();

    if (packetMode) {
      m_vrt_filad(pipe, p);
      return false;
    }
    else if ((type == PacketType_Data   ) || (type == PacketType_UnidentifiedData   ) ||
             (type == PacketType_ExtData) || (type == PacketType_UnidentifiedExtData)) {
      const BasicDataPacket& dp = checked_dynamic_cast<const BasicDataPacket&>(p);

      try {
        bool done = updateState(dp);
        if (done) return true;
      } catch (VRTException e) {
        m_warning(e.getMessage());
        return true;
      }

      if (writeToPipe(dp,fillMode)) {
//	cout<<"RETURNING TRUE"<<endl;
        return true;
      }
    }
    else if (type == PacketType_Context) {
      const BasicContextPacket& cp = checked_dynamic_cast<const BasicContextPacket&>(p);
      try {
        bool done = updateState(cp);
        if (done) return true;
      } catch (VRTException e) {
        m_warning(e.getMessage());
        return true;
      }
    }
    return false;
  }

bool PacketStream::readFromPipe(bool packetMode, int32_t contextFrequency, VRTWriter* packetWriter) {
    int32_t avail;
    m_hcbfunc(pipe, HCBF_AVAIL, &avail);
    if (avail < transferLengthElem) {
      return false; // not ready for reading
    }

    // IMPORT KEYWORDS FROM BLUE FILE (NOT IMPLEMENTED YET)
//    XMValue keywords = XMValue::DeserializeFromBlueKW(pipe);
//
//    XMValue::KVList& kvl = xmv.as<XMValue::KVList>();
//    for (XMValue::KVList::iterator it = kvl.begin(); it != kvl.end(); ++it) {
//      midasKeywordMapping mapping = blueToVrtKeywordMapping[it->first];
//      string vrtField = string(mapping.midasKeyword);
//      Field f(it->second);
//      p.setField(vrtField,f);
//    }
    // Update keyBuffer with bluefile keywords - this probably should be done at init for pipes, since keywrods are static

    // IMPORT KEYWORDS FROM MIDAS MESSAGES (NOT IMPLEMENTED YET)

    if (packetWriter == NULL) {
      // no destination, just discard
    }
    else if (packetMode) {
      BasicVRTPacket *p = NULL;//m_vrt_grabx(pipe);
      if (p == NULL) {
        return false;
      }
      else {
        packetWriter->sendPacket(p);
        return true;
      }
    }
    else {
      TimeStamp          ts = nextPacketTimeStamp;
      PayloadFormat      pf = context.getDataPayloadFormat();
      StandardDataPacket p(pf);

      int32_t numRead;
      pipe.xfer_len = (int32_t)transferLengthElem; // <-- should probably do something better for fractions
      pipe.cons_len = -1;
      m_grabx(pipe, &dataBuffer[0], numRead);
      if (numRead <= 0) {
        return false;
      }
      else {
        p.setPacketType(PacketType_Data);
        p.setTimeStamp(ts);
        p.setStreamIdentifier(streamID);
        p.setData(pf, dataBuffer, 0, (int32_t)(numRead*pipe.dbpe));

        if (newContext || (counter == 0)) {
          // INCLUDE REGULAR RE-SEND OF CONTEXT PACKET
          if (!newContext) context.resetForResend(ts);
          else               context.setTimeStamp(ts);

          packetWriter->sendPackets(&context, &p);
          newContext = false;
          counter    = 0;
        }
        else {
          // JUST SEND DATA PACKET
          packetWriter->sendPacket(&p);
          counter = (counter + 1) % contextFrequency;
        }

        double dt = context.getSampleRate() * numRead; // seconds
        nextPacketTimeStamp = ts.addPicoSeconds((int64_t)floor(dt / TimeStamp::ONE_SEC + 0.5));
        return true;
      }
    }
    return false;
  }
