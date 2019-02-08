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
#include <sstream>
#include <primitive.h>
#include <cstdlib>
#include <map>
#include "VRTWriter.h"
#include "BasicContextPacket.h"
#include "BasicDataPacket.h"
#include "NetUtilities.h"
#include "PacketStream.h"
#include "PSUtilities.h"
#include "StandardDataPacket.h"
#include "vrtio.h"
#include <midas/xmvalue.hh>

#include <qmessages.h>

#include "qmessdef.hh"

using namespace std;
using namespace vrt;


/** Maximum transfer length in bytes. This is equal to:
 *  <pre>
 *    MAX_XFER = round((max packet length) - (UDP header length) - MAX_HEADER_LENGTH - MAX_TRAILER_LENGTH)
 *
 *    Where round(..) rounds to the next lowest multiple of 4-bytes or 8-bytes
 *  </pre>
 *  Although the max packet length for UDP is 65,635 bytes, IPv4 imposes a limit of 65,515 to
 *  65,471 bytes based on the number of "options" set in the IPv4 header.
 */
const int32_t MAX_XFER = (65471 - 8 - BasicVRTPacket::MAX_HEADER_LENGTH - BasicVRTPacket::MAX_TRAILER_LENGTH) & 0xFFF8;

/** Default transfer length in bytes. This is equal to MAX_XFER rounded to the nearest 1024 bytes. */
const int32_t DEF_XFER = MAX_XFER & 0xFC00;


static vector<char> EMPTY(4096);
static vector<char> dataBuffer(BasicVRTPacket::MAX_PACKET_LENGTH);
static BasicContextPacket*      readContextPacket (CPHEADER& pipe, int32_t streamID);
static int32_t readDataStreamID (CPHEADER& pipe);

static bool debug;

void mainroutine () {

  string                    keywords         = "";
  bool                      force1000        = (m_get_switch_def("FORCE1000", 0) > 0);

  map<int32_t,PacketStream*> pipeForStream;
  double                    cutPeriod        = m_get_dswitch_def("CUTPERIOD", 60);
  bool                      findDataStream   = false;
  bool                      fillMode         = (m_get_switch_def("FILL",        1) > 0);
                            debug            = (m_get_switch_def("DEBUG",       0) > 0);
  int_4                     debugPeriod      = m_get_switch_def ("DEBUGPERIOD" ,1000);

  if (m_get_pswitch("KEYWORDS" )) m_get_uswitch("KEYWORDS", keywords);

  string pipeName = m_apick(1);

  HEADER             hcb;
  CPHEADER           pipe(hcb,true);

  m_initialize(pipe);

  pipe.file_name = pipeName;
  if (pipe.file_name[0] == '_') {
    // Workaround for X-Midas DR 501655-5 (Pipe names are occasionally case-sensitive)
    m_uppercase(pipe.file_name);
  }

  m_open(pipe, HCBF_INPUT);
  if (force1000) m_force1000(pipe);
  int32_t transferLengthBytes = 4;
  int32_t transferLengthElem = transferLengthBytes / pipe.dbpe;

  // Special handling for Python users who may use xm.sourcevrt("...", ALLSTREAMS="_mypipe")
  // rather than xm.sourcevrt("...", "ALLSTREAMS=_mypipe")
  XMValue::KVList cmdSwitchList;

  m_get_command_switches(cmdSwitchList);

  string commandString = Mc->command_string.substr(0, Mc->command_length);
  for (XMValue::KVList::iterator it=cmdSwitchList.begin() ; it < cmdSwitchList.end(); it++) {
    string key = it->first;
    string val = it->second.as<string>();

    if ((key == "ALLSTREAMS") || (key == "DATASTREAM") || (key.find("STREAM") == 0)) {
      cout << "SOURCEVRT: Mapping /" << key << "=" << val << " switch to " << key << "=" << val << " parameter" << endl;
      commandString += key + "=" + val + ",";
    }
  }
  Mc->command_string = commandString;
  Mc->command_length = commandString.length();

  vector<string> args;
  for (int32_t i = 2; m_ppick(i) != 0; i++) {
    string arg = m_upick(i);
    args.push_back(arg);
  }

  uint64_t debugCount = 0;

  //------------------------------------------------------------------------------------------------
  m_sync();


// Read the TAG=VAL arguments from the command line
for (vector<string>::iterator it = args.begin(); it != args.end(); ++it) {
  string arg = *it;
  // The next 7 lines are based on headermod.for which also uses key=val parameters
//    string arg = m_upick(i);
  size_t iEq = arg.find("=");
  if (iEq == string::npos) {
    m_error("Invalid command line argument "+arg);
  }
  string key = arg.substr(0,iEq);
  string val = arg.substr(iEq+1);

  // Wait for the first context packet to arrive so we can determine the configuration.
  if (key == "ALLSTREAMS") {
    BasicContextPacket* cp = readContextPacket(pipe, ALLSTREAMS);
    PacketStream *ps = new PacketStream(val, ALLSTREAMS, force1000, false, cutPeriod, *cp, keywords, debug);
    pipeForStream[ALLSTREAMS] = ps;
    delete cp;
  }
  else if (key == "DATASTREAM") {
    int32_t streamID = readDataStreamID(pipe);
    BasicContextPacket* cp = readContextPacket(pipe, streamID);
    pipeForStream[streamID] = new PacketStream(val, streamID, force1000, false, cutPeriod, *cp, keywords, debug);
    delete cp;
  }
  else if (key.find("STREAM") == 0) {  // find == 0 indicates that it is at the start of the string
    int32_t streamID = atoll(key.substr(6).c_str());
    BasicContextPacket* cp = readContextPacket(pipe, streamID);
    pipeForStream[streamID] = new PacketStream(val, streamID, force1000, false, cutPeriod, *cp, keywords, debug);
    delete cp;
  }
  else {
    m_error("Invalid parameter "+key+"="+val);
  }
}
  //------------------------------------------------------------------------------------------------
  bool done = false;
  while (!Mc->break_ && !done) {
    // Receive keyword updates
    if (m_get_msg(MQH, MQD, 0, 0) != 0) {
      if (MQH_NM.name == MQH.name && MQD.NM.msg_name == "KEYWORDS" ) {
        int32_t streamID = MQD.NM.msg_info;
        XMValue keywords = XMValue::Read(MQD.NM.msg_data,XMValue::TABLE);

        if (pipeForStream.count(streamID) > 0) {
          pipeForStream[streamID]->updateState(keywords);
        }
      }
    }

    BasicVRTPacket *p = m_vrt_grabx(pipe);

    debugCount++;

    if (debug && ((debugCount % debugPeriod) == 0)) {
      cout<< "DEBUG: Current Context: ";
      for (map<int32_t,PacketStream*>::iterator it = pipeForStream.begin(); it != pipeForStream.end(); ++it) {
        cout<< "DEBUG:  Context for Stream "<< it->first << ": " << it->second->getState() << endl;
      }
    }

    if (p != NULL) {

      int32_t        streamID = p->getStreamIdentifier();

      if (pipeForStream.count(ALLSTREAMS) > 0 && pipeForStream[ALLSTREAMS]->isInitialized()) {
        done = pipeForStream[ALLSTREAMS]->writeToPipe(*p, false, fillMode);
      }

      if (findDataStream) {
        PacketType type = p->getPacketType();

        if ((type == PacketType_Data   ) || (type == PacketType_UnidentifiedData   ) ||
            (type == PacketType_ExtData) || (type == PacketType_UnidentifiedExtData)) {
          if (debug)
            cout << "DEBUG: Found data stream on STREAM" << streamID << endl;

          findDataStream = false;
          if (streamID != 0) {
            pipeForStream[streamID] = pipeForStream[0];
            pipeForStream.erase(0);
          }
        }
      }

      if (!findDataStream && (pipeForStream.count(streamID) > 0)) {
        if (pipeForStream[streamID]->isInitialized()) {
          done = pipeForStream[streamID]->writeToPipe(*p, false, fillMode);
        }
        else if (p->getPacketType() == PacketType_Context) {
          BasicContextPacket *cp = dynamic_cast<BasicContextPacket*>(p);
          pipeForStream[streamID]->initialize(*cp);
          if (debug)
            cout << "DEBUG: Found context packet for " << PacketStream::PacketStream::getStreamName(streamID) << ": " << cp << endl;
        }
        else {
          if (debug)
            cout << "DEBUG: Need to find context packet for " << PacketStream::getStreamName(streamID) << endl;
        }
      }
      delete p;
    }
  }

  if (debug) {
    cout<<"DONE PROCESSING"<<endl;
  }

  //------------------------------------------------------------------------------------------------
  for (map<int32_t,PacketStream*>::iterator entry = pipeForStream.begin(); entry != pipeForStream.end(); entry++) {
    entry->second->close();
    delete entry->second;
  }

  m_close(pipe);
}

/** Reads a context packet off of the given stream. */
static int32_t readDataStreamID (CPHEADER& pipe) {
  int32_t count = 0;

  while (!Mc->break_) {
    BasicVRTPacket *p = m_vrt_grabx(pipe);
    if (p != NULL) {
      PacketType type = p->getPacketType();
      if ((type == PacketType_Data   ) || (type == PacketType_UnidentifiedData   ) ||
          (type == PacketType_ExtData) || (type == PacketType_UnidentifiedExtData)) {

        int32_t streamID = p->getStreamIdentifier();
        if (debug)
          cout << "DEBUG: Found data stream on STREAM" << streamID << endl;
        return streamID;
      }
      delete p;
    }
    else {
      cout<<"got null packet"<<endl;
    }

    if ((count % 10) == 0 && debug) {
      cout << "DEBUG: Need to find data stream before opening" << endl;
    }
    count++;
  }
  throw VRTException("Break encountered while waiting for a data packet");
}


/** Reads a context packet off of the given stream. */
static BasicContextPacket* readContextPacket (CPHEADER& pipe, int32_t streamID) {
  int32_t count = 0;

  while (!Mc->break_) {
    BasicVRTPacket *p = m_vrt_grabx(pipe);
    if (p == NULL                                                      ) { }
    else if (p->getPacketType() != PacketType_Context                   ) { delete p; }
    else if (!isNull(streamID) && (p->getStreamIdentifier() != streamID)) { delete p; }
    else {
      BasicContextPacket* cp = dynamic_cast<BasicContextPacket*>(p);
      if (debug)
        cout << "DEBUG: Found context packet for " << PacketStream::getStreamName(streamID) << ": " << *cp << endl;
      return cp;
    }

    if ((count % 10) == 0 && debug) {
      cout << "DEBUG: Need to find context packet for STREAM" << streamID << " before opening" << endl;
    }
    count++;
  }
  throw VRTException("Break encountered while waiting for a context packet on %s", PacketStream::getStreamName(streamID).c_str());
}
