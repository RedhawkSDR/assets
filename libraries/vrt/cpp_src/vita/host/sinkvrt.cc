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

/**  Default transfer length in bytes. This is equal to MAX_XFER rounded to the nearest 1024 bytes. */
const int32_t DEF_XFER = MAX_XFER & 0xFC00;


static vector<char> EMPTY(4096);
static vector<char> dataBuffer(BasicVRTPacket::MAX_PACKET_LENGTH);
static bool writeToPipe (PacketStream &ps, const BasicVRTPacket &p, bool packetMode, bool fillMode);

void mainroutine () {
  string                    prefix           = "PFX_";
  string                    host             = "";
  int32_t                   port             = -1; // -1=Invalid
  string                    device           = "eth0";
  string                    nic              = "eth0";
  int32_t                   vlan             = -1; // -1=No VLAN
  bool                      force1000        = (m_get_switch_def("FORCE1000", 0) > 0);
  bool                      packetMode       = (m_get_switch_def("PACKET",    0) > 0);
  bool                      useFrames        = (m_get_switch_def("VRL",       0) > 0);
  bool                      useCRC           = (m_get_switch_def("CRC",       0) > 0);
  map<int32_t,PacketStream*> pipeForStream;
  int32_t                   xfer             = m_get_switch_def("TL", DEF_XFER);
  int32_t                   contextFrequency = m_get_switch_def("CONTEXTFREQ", 16);
  int32_t                   modeValue        = MODE_NOCHANGE;
  int32_t                   modeWidget       = m_mwinit("MODE", modeValue, MODES);
  VRTWriter*                packetWriter     = NULL;

  if ((xfer < 8) || (xfer > MAX_XFER) || ((xfer&0x7) != 0)) {
    // technically it could be a multiple of 4, if only ever using data sizes <=32-bits
    throw VRTException("Illegal value /TL=%d (must be between 8 and %d and a multiple of 8)",xfer,MAX_XFER);
  }
  if (contextFrequency < 1) {
    throw VRTException("Illegal value /CONTEXTFREQ=%d",contextFrequency);
  }

  if (m_get_pswitch("DEVICE")) m_get_uswitch("DEVICE", device);  m_lowercase(device);
  if (m_get_pswitch("PFX"   )) m_get_uswitch("PFX",    prefix);

  string hostPort = m_apick(1);
  if (hostPort.length() > 0) {
    size_t iColon = hostPort.rfind(":");
    if (iColon == string::npos) {
      m_error("Invalid host:port given on command line");
    }
    host = hostPort.substr(0,iColon);
    port = atoi(hostPort.substr(iColon+1).c_str());
  }
  else {
    if (m_get_pswitch("NIC"  )) m_get_uswitch("NIC",   nic);  m_lowercase(nic);
    if (m_get_pswitch("MCAST")) m_get_uswitch("MCAST", host);
    vlan   = m_get_switch_def("VLAN", -1);
    port   = m_get_switch_def("PORT", -1);
    device = NetUtilities::getNetworkDeviceName(nic, vlan);
  }

  if (host.length() > 0) {
    cout << "INFO: Opening connection to "<<host<<":"<<port<<" using "<<device<<endl;
    packetWriter = new VRTWriter(host, port, device, useFrames, useCRC);
  }
  else {
    cout << "INFO: No host specified, monitoring widget for host information" << endl;
  }

  // Special handling for Python users who may use xm.sinkvrt("...", ALLSTREAMS="_mypipe")
  // rather than xm.sinkvrt("...", "ALLSTREAMS=_mypipe")
  XMValue::KVList cmdSwitchList;

  m_get_command_switches(cmdSwitchList);

  string commandString = Mc->command_string.substr(0, Mc->command_length);
  for (XMValue::KVList::iterator it=cmdSwitchList.begin() ; it < cmdSwitchList.end(); it++) {
    string key = it->first;
    string val = it->second.as<string>();

    if ((key == "ALLSTREAMS") || (key == "DATASTREAM") || (key.find("STREAM") == 0)) {
      cout << "SINKVRT: Mapping /" << key << "=" << val << " switch to " << key << "=" << val << " parameter" << endl;
      commandString += key + "=" + val + ",";
    }
  }
  Mc->command_string = commandString;
  Mc->command_length = commandString.length();


  // Read the TAG=VAL arguments from the command line
  for (int32_t i = 2; m_ppick(i) != 0; i++) {
    // The next 7 lines are based on headermod.for which also uses key=val parameters
    string arg = m_upick(i);
    size_t iEq = arg.find("=");
    if (iEq == string::npos) {
      m_error("Invalid command line argument "+arg);
    }
    string key = arg.substr(0,iEq);
    string val = arg.substr(iEq+1);

    if (key == "ALLSTREAMS") {
      pipeForStream[ALLSTREAMS] = new PacketStream(val, ALLSTREAMS, force1000, packetMode, xfer);
    }
    else if (key.find("STREAM") == 0) {  // find == 0 indicates that it is at the start of the string
      int32_t streamID = atoll(key.substr(6).c_str());
      pipeForStream[streamID] = new PacketStream(val, streamID, force1000, packetMode, xfer);
    }
    else {
      m_error("Invalid parameter "+key+"="+val);
    }
  }

  //------------------------------------------------------------------------------------------------
  m_sync();

  //------------------------------------------------------------------------------------------------
  bool done = false;
  while (!Mc->break_ && !done) {
    // Receive keyword updates
    if (m_get_msg(MQH, MQD, 0, 0) != 0) {
      if (MQH_NM.name == MQH.name && MQD.NM.msg_name == "KEYWORDS" ) {
        cout<<"GOT NM msg_name="<<MQD.NM.msg_name<<", msg_data="<<MQD.NM.msg_data<<", types="<<MQH.types<<endl;
        int32_t streamID = MQD.NM.msg_info;
        XMValue keywords = XMValue::Read(MQD.NM.msg_data,XMValue::TABLE);

        if (pipeForStream.count(streamID) > 0) {
          cout<<"UPDATING STATE WITH\n"<<keywords<<endl;
          pipeForStream[streamID]->updateState(keywords);
        }
      }
    }

    if (m_mwget(modeWidget, modeValue)) {
      if (packetWriter != NULL) {
        // close old connection
        packetWriter->close();
        delete packetWriter;
        packetWriter = NULL;
      }
      if (modeValue == MODE_START) {
        host         = getResult(prefix+"MCAST", host);
        port         = getResult(prefix+"PORT",  port);
        nic          = getResult(prefix+"NIC",   nic );  m_lowercase(nic);
        vlan         = getResult(prefix+"VLAN",  vlan);
        device       = NetUtilities::getNetworkDeviceName(nic, vlan);

        cout << "INFO: Opening connection to "<<host<<":"<<port<<" using "<<nic<<" (VLAN="<<vlan<<")" << endl;
        packetWriter = new VRTWriter(host, port, device, useFrames, useCRC);
      }
      modeValue = MODE_NOCHANGE;
      m_mwput(modeWidget, modeValue);
    }

    bool anyRead = false;
    for (map<int32_t,PacketStream*>::iterator entry = pipeForStream.begin(); entry != pipeForStream.end(); entry++) {
      anyRead = anyRead || entry->second->readFromPipe(packetMode, contextFrequency, packetWriter);
      //END
    }
    if (!anyRead) {
      m_pause(0.001);
    }
  }

  //------------------------------------------------------------------------------------------------
  for (map<int32_t,PacketStream*>::iterator entry = pipeForStream.begin(); entry != pipeForStream.end(); entry++) {
    entry->second->close();
    delete entry->second;
  }
  if (packetWriter != NULL) {
    packetWriter->close();
    delete packetWriter;
    packetWriter = NULL;
  }
}
