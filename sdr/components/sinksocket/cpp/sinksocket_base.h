/*
 * This file is protected by Copyright. Please refer to the COPYRIGHT file distributed with this 
 * source distribution.
 * 
 * This file is part of REDHAWK Basic Components sinksocket.
 * 
 * REDHAWK Basic Components sinksocket is free software: you can redistribute it and/or modify it under the terms of 
 * the GNU Lesser General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * REDHAWK Basic Components sinksocket is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
 * PURPOSE.  See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this 
 * program.  If not, see http://www.gnu.org/licenses/.
 */

#ifndef SINKSOCKET_BASE_IMPL_BASE_H
#define SINKSOCKET_BASE_IMPL_BASE_H

#include <boost/thread.hpp>
#include <ossie/Component.h>
#include <ossie/ThreadedComponent.h>

#include <bulkio/bulkio.h>
#include "struct_props.h"

class sinksocket_base : public Component, protected ThreadedComponent
{
    public:
        sinksocket_base(const char *uuid, const char *label);
        ~sinksocket_base();

        void start() throw (CF::Resource::StartError, CORBA::SystemException);

        void stop() throw (CF::Resource::StopError, CORBA::SystemException);

        void releaseObject() throw (CF::LifeCycle::ReleaseError, CORBA::SystemException);

        void loadProperties();

    protected:
        // Member variables exposed as properties
        /// Property: total_bytes
        double total_bytes;
        /// Property: bytes_per_sec
        float bytes_per_sec;
        /// Property: Connections
        std::vector<Connection_struct> Connections;
        /// Property: ConnectionStats
        std::vector<ConnectionStat_struct> ConnectionStats;

        // Ports
        /// Port: dataOctet_in
        bulkio::InOctetPort *dataOctet_in;
        /// Port: dataChar_in
        bulkio::InCharPort *dataChar_in;
        /// Port: dataShort_in
        bulkio::InShortPort *dataShort_in;
        /// Port: dataUshort_in
        bulkio::InUShortPort *dataUshort_in;
        /// Port: dataLong_in
        bulkio::InLongPort *dataLong_in;
        /// Port: dataUlong_in
        bulkio::InULongPort *dataUlong_in;
        /// Port: dataFloat_in
        bulkio::InFloatPort *dataFloat_in;
        /// Port: dataDouble_in
        bulkio::InDoublePort *dataDouble_in;

    private:
};
#endif // SINKSOCKET_BASE_IMPL_BASE_H
