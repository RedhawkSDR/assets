/*
 * This file is protected by Copyright. Please refer to the COPYRIGHT file
 * distributed with this source distribution.
 *
 * This file is part of REDHAWK FmRdsSimulator.
 *
 * REDHAWK FmRdsSimulator is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * REDHAWK FmRdsSimulator is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
/*******************************************************************************************

    AUTO-GENERATED CODE. DO NOT MODIFY

    Source: None

*******************************************************************************************/

#include "RDC.h"

using namespace RDC_ns;

/******************************************
 *
 * Logging:
 *      To log, use the _portLog member (not available in the constructor)
 *
 *      For example,
 *          RH_DEBUG(_portLog, "this is a debug message");
 *
 ******************************************/

// ----------------------------------------------------------------------------------------
// CF_DeviceStatus_Out_i definition
// ----------------------------------------------------------------------------------------
CF_DeviceStatus_Out_i::CF_DeviceStatus_Out_i(std::string port_name, RDC_base *_parent) :
Port_Uses_base_impl(port_name)
{
    parent = static_cast<RDC_i *> (_parent);
    recConnectionsRefresh = false;
    recConnections.length(0);
}

CF_DeviceStatus_Out_i::~CF_DeviceStatus_Out_i()
{
}
void CF_DeviceStatus_Out_i::statusChanged(const CF::DeviceStatusType& status, const std::string __connection_id__)
{
    std::vector < std::pair < CF::DeviceStatus_var, std::string > >::iterator i;

    boost::mutex::scoped_lock lock(updatingPortsLock);   // don't want to process while command information is coming in

    __evaluateRequestBasedOnConnections(__connection_id__, false, false, false);
    if (this->active) {
        for (i = this->outConnections.begin(); i != this->outConnections.end(); ++i) {
            if (not __connection_id__.empty() and __connection_id__ != (*i).second)
                continue;
            try {
                ((*i).first)->statusChanged(status);
            } catch (...) {
                RH_ERROR(this->_portLog, "Call to statusChanged by CF_DeviceStatus_Out_i failed");
                throw;
            }
        }
    }

}

std::string CF_DeviceStatus_Out_i::getRepid() const
{
    return CF::DeviceStatus::_PD_repoId;
}

