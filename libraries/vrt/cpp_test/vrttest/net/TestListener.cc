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

#include "TestListener.h"

using namespace std;
using namespace vrt;
using namespace vrttest;

string TestListener::toString () const {
  string evt = (isNull(event  ))? "<null>" : event.toString();
  string msg = (isNull(message))? "<null>" : string("'")+message+"'";
  string err = (isNull(error  ))? "<null>" : string("'")+error.toString()+"'";

  switch (mode) {
    case Mode_none:                   return string("No Message Received");
    case Mode_receivedPacket:         return string("receivedPacket(")+evt+", "+packet.toString()+")";
    case Mode_sentPacket:             return string("sentPacket(")+evt+", "+packet.toString()+")";
    case Mode_errorOccurred:          return string("errorOccurred(")+evt+", "+msg+", "+err+")";
    case Mode_warningOccurred:        return string("warningOccurred(")+evt+", "+msg+", "+err+")";
    case Mode_receivedDataPacket:     return string("receivedDataPacket(")+evt+", "+packet.toString()+")";
    case Mode_receivedContextPacket:  return string("receivedContextPacket(")+evt+", "+packet.toString()+")";
    case Mode_receivedInitialContext: return string("receivedInitialContext(")+evt+", "+msg+", "+data.toString()+", "+ctx.toString()+", ...)";
    default: throw VRTException("Unknown mode %d ", (int)mode);
  }
}
