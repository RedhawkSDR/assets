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

#ifndef _TestListener_h
#define _TestListener_h

#include "VRTContextListener.h"

using namespace std;
using namespace vrt;

namespace vrttest {
  /** The function last called. */
  enum TestListener_Mode { Mode_none, Mode_receivedPacket, Mode_sentPacket, Mode_errorOccurred,
                           Mode_warningOccurred, Mode_receivedDataPacket,
                           Mode_receivedContextPacket, Mode_receivedInitialContext };

  /** A dummy implementation of {@link VRTContextListener} that stores the
   *  results of the last call.
   */
  class TestListener : public VRTContextListener {
    public: TestListener_Mode           mode;
    public: VRTEvent                    event;
    public: string                      message;
    public: BasicVRTPacket              packet;
    public: BasicDataPacket             data;
    public: BasicVRTPacket              ctx;
    public: map<int32_t,BasicVRTPacket> context;
    public: VRTException                error;

    public: virtual string toString () const;

    /** Resets all values to null. */
    public: inline void reset () {
      mode    = Mode_none;
      event   = VRTEvent();
      message = "";
      packet  = BasicVRTPacket();
      data    = BasicDataPacket();
      ctx     = BasicVRTPacket();
      context = map<int32_t,BasicVRTPacket>();
      error   = VRTException();
    }

    public: virtual void receivedPacket (const VRTEvent &e, const BasicVRTPacket *p) {
      reset();
      mode   = Mode_receivedPacket;
      event  = e;
      packet = *p;
    }

    public: virtual void sentPacket (const VRTEvent &e, const BasicVRTPacket *p) {
      reset();
      mode   = Mode_sentPacket;
      event  = e;
      packet = *p;
    }

    public: virtual void errorOccurred (const VRTEvent &e, const string &msg, const VRTException &t) {
      reset();
      mode    = Mode_errorOccurred;
      event   = e;
      message = msg;
      error   = t;
    }

    public: virtual void warningOccurred (const VRTEvent &e, const string &msg, const VRTException &t) {
      reset();
      mode    = Mode_warningOccurred;
      event   = e;
      message = msg;
      error   = t;
    }

    public: virtual void receivedDataPacket (const VRTEvent &e, const BasicDataPacket *p) {
      reset();
      mode   = Mode_receivedDataPacket;
      event  = e;
      packet = *p;
    }

    public: virtual void receivedContextPacket (const VRTEvent &e, const BasicVRTPacket *p) {
      reset();
      mode   = Mode_receivedContextPacket;
      event  = e;
      packet = *p;
    }

    public: virtual void receivedInitialContext (const VRTEvent &e, const string &msg,
                                                 const BasicDataPacket &d, const BasicVRTPacket &c,
                                                 const map<int32_t,BasicVRTPacket> &m) {
      reset();
      mode    = Mode_receivedInitialContext;
      event   = e;
      message = msg;
      data    = d;
      ctx     = c;
      context = m;
    }
  };
} END_NAMESPACE
#endif /* _TestListener_h */
