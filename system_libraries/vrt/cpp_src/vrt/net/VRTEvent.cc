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

#include "VRTEvent.h"

using namespace std;
using namespace vrt;

#define setSource(src) { \
       if (src == NULL) sourceStr = "<null>"; \
  else if (isNull(src)) sourceStr = (src)->getClassName() + ": " + "<null>"; \
  else                  sourceStr = (src)->getClassName() + ": " + (src)->toString(); \
}

VRTEvent::VRTEvent (const VRTObject &source) :
  source(NULL),
  sourceStr("???"),
  packet(BasicVRTPacket::NULL_PACKET)
{
  setSource(&source);
}

VRTEvent::VRTEvent (VRTObject *source) :
  source(source),
  sourceStr("???"),
  packet(BasicVRTPacket::NULL_PACKET)
{
  setSource(source);
}

VRTEvent::VRTEvent (const VRTObject &source, const BasicVRTPacket &packet) :
  source(NULL),
  sourceStr("???"),
  packet(packet)
{
  setSource(&source);
}

VRTEvent::VRTEvent (const VRTObject &source, const BasicVRTPacket *_packet) :
  source(NULL),
  sourceStr("???"),
  packet(BasicVRTPacket::NULL_PACKET)
{
  setSource(&source);
  if (_packet != NULL) packet = *_packet;
}

VRTEvent::VRTEvent (VRTObject *source, const BasicVRTPacket &packet) :
  source(source),
  sourceStr("???"),
  packet(packet)
{
  setSource(source);
}

VRTEvent::VRTEvent (VRTObject *source, const BasicVRTPacket *_packet) :
  source(source),
  sourceStr("???"),
  packet(BasicVRTPacket::NULL_PACKET)
{
  setSource(source);
  if (_packet != NULL) packet = *_packet;
}

VRTEvent::VRTEvent (const VRTEvent &e) :
  VRTObject(e), // <-- Used to avoid warnings under GCC with -Wextra turned on
  source(e.source),
  sourceStr(e.sourceStr),
  packet(e.packet)
{
  // done
}

VRTEvent::VRTEvent () :
  source(NULL),
  sourceStr(""),
  packet(BasicVRTPacket::NULL_PACKET)
{
  // done
}

string VRTEvent::toString () const {
  return getClassName() + " [source=" + sourceStr + "]";
}

bool VRTEvent::equals (const VRTEvent &o) const {
  if (isNullValue() && o.isNullValue()) return true;

  return (source    == o.source)
      && (sourceStr == o.sourceStr)
      && (packet    == o.packet);
}
