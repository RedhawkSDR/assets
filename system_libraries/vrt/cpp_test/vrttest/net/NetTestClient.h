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

#ifndef _NetTestClient_h
#define _NetTestClient_h

#include "NetUtilities.h"
#include "Testable.h"
#include "TestRunner.h"
#include "TestSet.h"
#include "Utilities.h"

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

namespace vrttest {
  class QueuedListener;

  /** Runs the networking test cases. */
  class NetTestClient : public VRTObject, public virtual Testable {
    private: string  device;     // The network device to use
    private: string  mcHost;     // The host to use for UDP/Multicast
    private: int32_t mcPort;     // The host to use for UDP/Multicast
    private: string  localHost;  // The local host to use
    private: int32_t localPort;  // The local port to use
    private: int32_t localPort2; // The local port to use (alternate)
    private: string  svrHost;    // The server host to use
    private: int32_t svrPort;    // The server port to use
    private: int32_t svrPort2;   // The server port to use (alternate)


    public: NetTestClient ();

    // Implement Testable
    public: virtual string getTestedClass ();
    public: virtual vector<TestSet> init ();
    public: virtual void done ();
    public: virtual void call (const string &func);

    // Test functions
    public: virtual void testVRTReaderTCPClient ();
    public: virtual void testVRTReaderTCPServer ();
    public: virtual void testVRTReaderUDPMulticast ();
    public: virtual void testVRTReaderUDPUnicast ();
    public: virtual void testVRTWriterTCPClient();
    public: virtual void testVRTWriterTCPServer();
    public: virtual void testVRTWriterUDPMulticast();
    public: virtual void testVRTWriterUDPUnicast();

    // Other functions
    /** Sends a start request and waits for it to complete.
     *  @param request   The request parameters.
     *  @param wait      The total wait time in ms.
     *  @return The UUID of the task.
     */
    private: UUID sendStartRequest (map<string,string> request, int32_t wait);

    /** Sends a start request and waits for it to complete.
     *  @param uuid      The UUID of the task.
     *  @param listener  The listener being used.
     *  @param count     The number of expected messages.
     *  @param wait      The total wait time in ms.
     *  @param waitStop  Extra time to wait following last expected packet before
     *                   sending a stop request.
     */
    private: bool waitForRequest (const UUID &uuid, QueuedListener &listener,
                                  int32_t count, int32_t wait, int32_t waitStop);


    private: void _testVRTReader (TransportProtocol transport, const string &host, int32_t port,
                                  const string &svrHost, int32_t svrPort,
                                  int32_t elements, double timeDelta, int32_t totDataPkts,
                                  bool vrlFrame, bool vrlCRC);


    private: void _testVRTWriter (TransportProtocol transport, const string &host, int32_t port,
                                  const string &svrHost, int32_t svrPort,
                                  int32_t elements, double timeDelta, int32_t totDataPkts,
                                  bool vrlFrame, bool vrlCRC);
  };
} END_NAMESPACE
#endif /* _NetTestClient_h */
