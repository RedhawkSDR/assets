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
#include <queue>
#include <pthread.h>
#include <errno.h>
#include <unistd.h>
#include "BasicContextPacket.h"
#include "BasicDataPacket.h"
#include "NetUtilities.h"
#include "PacketStream.h"
#include "PSUtilities.h"
#include "VRTReader.h"
#include "vrtio.h"
#include "string.h"
#include <qmessages.h>
#include <midas/xmvalue.hh>
#include "qmessdef.hh"

using namespace std;
using namespace vrt;


static          pthread_mutex_t            threadLock;
static          pthread_mutex_t            queueLock;
static          pthread_t                  readThread;
static volatile bool                       stopThread       = false;
static          bool                       useThreads;
static          queue<BasicVRTPacket*>     packetQueue;
static          VRTReader                 *packetReader     = NULL;
static          map<int32_t,PacketStream*> pipeForStream;
static          VRTContextListener        *contextListener  = NULL;
static          bool                       opening          = false;
static          bool                       findDataStream   = false;
static          bool                       packetMode       = false;
static          bool                       fillMode         = false;
static          bool                       force1000        = false;
static          bool                       done             = false;
static          bool                       synced           = false;
static          bool                       debug            = false;

#define POP_COUNT    8     // number of packets to pop off at a time
#define READ_COUNT   8     // number of packets to read off at a time
#define QUEUE_LIMIT  1024  // number of packets to permitted in the queue
#define READ_TIMEOUT 5.0f  // number of seconds to use for the read timeout
// Context Listener maintains state of PacketStreams
// main thread adds packet streams as they are necessary from input using method
// context listener adds by receiving packets

static void receivedDataPacket (const VRTEvent &e, const BasicDataPacket *d) {
  int32_t streamID = d->getStreamIdentifier();
  if (findDataStream) {
    cout<<"FOUND DATASTREAM: STREAM"<<streamID<<endl;
    findDataStream = false;
    if (streamID != DATASTREAM) {
      pipeForStream[streamID] = pipeForStream[DATASTREAM];
      pipeForStream[streamID]->setStreamID(streamID);
      pipeForStream.erase(DATASTREAM);
    }
  }
  else if (!findDataStream && (pipeForStream.count(streamID) > 0)) {
    if ((packetMode || pipeForStream[streamID]->isInitialized()) && synced) {
      done = done || pipeForStream[streamID]->writeToPipe(*d, packetMode, fillMode);
    }
  }
  if (pipeForStream.count(ALLSTREAMS) > 0) {
    PacketStream *ps = pipeForStream[ALLSTREAMS];
    if (ps->isInitialized() && synced) {
      done = done || ps->writeToPipe(*d, packetMode, fillMode);
    }
  }
}

static void receivedContextPacket (const VRTEvent &e, const BasicVRTPacket *c) {
  int32_t streamID = c->getStreamIdentifier();
  if (pipeForStream.count(ALLSTREAMS) > 0) {
    PacketStream *ps = pipeForStream[ALLSTREAMS];
    if (ps->isInitialized() && synced) {
      done = done || ps->writeToPipe(*c, packetMode, fillMode);
    }
    else {
      cout<<"INITIALIZING STREAM ALLSTREAMS"<<endl;
      ps->initialize(*c);
      if (synced) {
        done = done || ps->writeToPipe(*c, packetMode, fillMode);
      }
    }
  }
  if (!findDataStream && (pipeForStream.count(streamID) > 0)) {
    PacketStream *ps = pipeForStream[streamID];
    if ((packetMode || ps->isInitialized()) && synced) {
      done = done || ps->writeToPipe(*c, packetMode, fillMode);
    }
    else if (!isNull(*c) && !ps->isInitialized()) {
      cout<<"INITIALIZING STREAM "<<streamID<<endl;
      ps->initialize(*c);
      if (synced) {
        done = done || ps->writeToPipe(*c, packetMode, fillMode);
      }
    }
  }
}

static void receivedInitialContext (const VRTEvent &e, const string &errorMsg,
                                    const BasicDataPacket &data, const BasicVRTPacket &ctx,
                                    const map<int32_t, BasicVRTPacket> &context) {
}

static void receivedPacket (const VRTEvent &e, const BasicVRTPacket *p) {
}

static void sentPacket (const VRTEvent &e, const BasicVRTPacket *p) {
}

static void errorOccurred (const VRTEvent &e, const string &msg, const VRTException &t) {
  m_warning(msg);
}

static void warningOccurred (const VRTEvent &e, const string &msg, const VRTException &t) {
  m_warning(msg);
}


void mainroutine () {
  string                    prefix           = "PFX_";
  string                    host             = "";
  int32_t                   port             = -1; // -1=Invalid
  string                    keywords         = "";
  string                    device           = "eth0";
  string                    nic              = "eth0";
  int32_t                   vlan             = -1; // -1=No VLAN
  bool                      exactTopOfSecond = (m_get_switch_def("TOPOFSECOND",0) > 0);
                            fillMode         = (m_get_switch_def("FILL",      1) > 0);
                            force1000        = (m_get_switch_def("FORCE1000", 0) > 0);
                            packetMode       = (m_get_switch_def("PACKET",    0) > 0);
  int32_t                   modeValue        = MODE_NOCHANGE;
  int32_t                   modeWidget       = m_mwinit("MODE", modeValue, MODES);
  int32_t                   recvBufSize      = m_get_switch_def("RECVBUF", 32*1024*1024);
  int32_t                   packetQueueSize  = m_get_switch_def("PACKETBUF", 1024);
  char                     *error            = NULL;
  double                    cutPeriod        = m_get_dswitch_def("CUTPERIOD", -1);
                            debug            = (m_get_switch_def("DEBUG",       0) > 0);
  int_4                     debugPeriod      = 10*m_get_switch_def ("DEBUGPERIOD" ,1);
  int_4                     numFiles         = m_get_switch_def("NUMFILES",-1);

  if (m_get_pswitch("DEVICE" ))   m_get_uswitch("DEVICE",   device);
  if (m_get_pswitch("PFX"    ))   m_get_uswitch("PFX",      prefix);
  if (m_get_pswitch("KEYWORDS" )) m_get_uswitch("KEYWORDS", keywords);

  string hostPort = m_apick(1);
  if (hostPort.length() > 0) {
    size_t iColon = hostPort.rfind(":");
    if (iColon == string::npos) {
      // assume we just got a port
      host = "";
      try {
        port = atoi(hostPort.c_str());
      }
      catch(std::exception const & e)
      {
        m_error("Error parsing port " + hostPort);
      }
    } else {
      host = hostPort.substr(0,iColon);
      try {
        port = atoi(hostPort.substr(iColon + 1).c_str());
      }
      catch(std::exception const & e)
      {
        m_error("Error parsing port " + hostPort.substr(iColon + 1));
      }
    }
  }
  else {
    if (m_get_pswitch("NIC"  )) m_get_uswitch("NIC",   nic);  m_lowercase(nic);
    if (m_get_pswitch("MCAST")) m_get_uswitch("MCAST", host);
    vlan   = m_get_switch_def("VLAN", -1);
    port   = m_get_switch_def("PORT", -1);
    device = NetUtilities::getNetworkDeviceName(nic, vlan);
  }

  // DONE GETTING SETUP

  contextListener = new VRTContextListener(&receivedDataPacket,&receivedContextPacket,&receivedInitialContext,&errorOccurred,&warningOccurred);

  packetReader = new VRTReader(host, port, device, contextListener, 10000, recvBufSize, packetQueueSize, false, true);
  cout<<"LISTENING ON "<<hostPort<<endl;
  packetReader->start();

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

  // Read the TAG=VAL arguments from the command line
  for (int32_t i = 2; m_ppick(i) != 0; i++) {
    string arg = m_upick(i);
    size_t iEq = arg.find("=");
    if (iEq == string::npos) {
      m_error("Invalid command line argument "+arg);
    }
    string key = arg.substr(0,iEq);
    string val = arg.substr(iEq+1);

    bool all = (key == "ALLSTREAMS");
    bool data = (key == "DATASTREAM");

    if (all || data || key.find("STREAM") == 0) {
      int32_t streamID = (all) ? ALLSTREAMS : (data) ? DATASTREAM : atoll(key.substr(6).c_str());
      opening = true;
      findDataStream = data;
      if (debug) {
        cout<<"DEBUG: CREATING NEW PACKET STREAM FOR "<<((all) ? "ALLSTREAMS" : ((data) ? "DATASTREAM" : key.substr(6).c_str()))<<endl;
      }
      pipeForStream[streamID] = new PacketStream(val, streamID, force1000, packetMode, cutPeriod, keywords, numFiles, debug, exactTopOfSecond);
    }
    else {
      m_error("Invalid parameter "+key+"="+val);
    }
  }

  int count = 0;
  while (!(packetReader == NULL) && !Mc->break_ && opening) {
    m_pause(0.1); // no input source, just pause briefly before re-checking the widget
    count++;
    bool done = true;
    for (map<int32_t,PacketStream*>::iterator entry = pipeForStream.begin(); entry != pipeForStream.end(); entry++) {
      if (!entry->second->isInitialized()) {
        ostringstream oss;
        if (entry->first == ALLSTREAMS)
          oss <<"ALLSTREAMS";
        else if (entry->first == DATASTREAM)
          oss <<"DATASTREAM";
        else
          oss << entry->first;
        if (count%10==0)
          m_warning("SOURCEVRT STREAM " + oss.str() + " has not been initialized.");
        done = false;
      }
    }
    opening = !done;
  }

  //------------------------------------------------------------------------------------------------
  m_sync();
  //------------------------------------------------------------------------------------------------

  synced = true;
  count = 0;

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

    if (m_mwget(modeWidget, modeValue)) {
      if (modeValue == MODE_START) {
        host         = getResult(prefix+"MCAST", host);
        port         = getResult(prefix+"PORT",  port);
        nic          = getResult(prefix+"NIC",   nic );  m_lowercase(nic);
        vlan         = getResult(prefix+"VLAN",  vlan);
        device       = NetUtilities::getNetworkDeviceName(nic, vlan);

        cout << "INFO: Opening connection to "<<host<<":"<<port<<" using "<<nic<<" (VLAN="<<vlan<<")" << endl;

        for (map<int32_t,PacketStream*>::iterator entry = pipeForStream.begin(); entry != pipeForStream.end(); entry++) {
          entry->second->reset();
        }

        if (packetReader != NULL) {
          packetReader->stop(true);
          delete packetReader;
          packetReader = NULL;
        }
        packetReader = new VRTReader(host, port, device, contextListener, 10000, true, recvBufSize);
        packetReader->start();
      }
      
      if (modeValue == MODE_STOP) {
        packetReader = NULL;
        
        for (map<int32_t,PacketStream*>::iterator entry = pipeForStream.begin(); entry != pipeForStream.end(); entry++) {
          entry->second->reset();
        }
      }
      modeValue = MODE_NOCHANGE;
      m_mwput(modeWidget, modeValue);
    }

    if (opening) {
      count++;
      bool done = true;
      for (map<int32_t,PacketStream*>::iterator entry = pipeForStream.begin(); entry != pipeForStream.end(); entry++) {
        if (!entry->second->isInitialized()) {
          ostringstream oss;
          if (entry->first == ALLSTREAMS)
            oss <<"ALLSTREAMS";
          else if (entry->first == DATASTREAM)
            oss <<"DATASTREAM";
          else
            oss << entry->first;
          if (count%10==0)
            m_warning("STREAM " + oss.str() + " has not been initialized.");
          done = false;
        }
      }
      opening = !done;
    }

    if (count % debugPeriod == 0 && debug) {
      if (packetReader != NULL) {
        cout<<"CURRENT PACKET QUEUE SIZE: "<<packetReader->getQueueSize()<<", MAX SIZE: "<<packetReader->getMaxQueueSize()<<endl;
      }
    }
    m_pause(0.1); // no input source, just pause briefly before re-checking the widget
    count++;
  }

  if (packetReader != NULL) {
    packetReader->stop(true);
  }
  
  //------------------------------------------------------------------------------------------------
  for (map<int32_t,PacketStream*>::iterator entry = pipeForStream.begin(); entry != pipeForStream.end(); entry++) {
    entry->second->close();
    delete entry->second;
  }

  delete packetReader;
  packetReader = NULL;

  if (debug) {
    cout<<"DONE PROCESSING"<<endl;
  }
}
