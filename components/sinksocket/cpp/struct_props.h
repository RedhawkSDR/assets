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

#ifndef STRUCTPROPS_H
#define STRUCTPROPS_H

/*******************************************************************************************

    AUTO-GENERATED CODE. DO NOT MODIFY

*******************************************************************************************/

#include <ossie/CorbaUtils.h>
#include <CF/cf.h>
#include <ossie/PropertyMap.h>

struct Connection_struct {
    Connection_struct ()
    {
        connection_type = "server";
        ip_address = "";
        byte_swap.push_back(0);
        ports.push_back(32191);
    };

    static std::string getId() {
        return std::string("Connection");
    };

    std::string connection_type;
    std::string ip_address;
    std::vector<unsigned short> byte_swap;
    std::vector<unsigned short> ports;
};

inline bool operator>>= (const CORBA::Any& a, Connection_struct& s) {
    CF::Properties* temp;
    if (!(a >>= temp)) return false;
    const redhawk::PropertyMap& props = redhawk::PropertyMap::cast(*temp);
    if (props.contains("Connection::connection_type")) {
        if (!(props["Connection::connection_type"] >>= s.connection_type)) return false;
    }
    if (props.contains("Connection::ip_address")) {
        if (!(props["Connection::ip_address"] >>= s.ip_address)) return false;
    }
    if (props.contains("Connection::byte_swap")) {
        if (!(props["Connection::byte_swap"] >>= s.byte_swap)) return false;
    }
    if (props.contains("Connection::ports")) {
        if (!(props["Connection::ports"] >>= s.ports)) return false;
    }
    return true;
}

inline void operator<<= (CORBA::Any& a, const Connection_struct& s) {
    redhawk::PropertyMap props;
 
    props["Connection::connection_type"] = s.connection_type;
 
    props["Connection::ip_address"] = s.ip_address;
 
    props["Connection::byte_swap"] = s.byte_swap;
 
    props["Connection::ports"] = s.ports;
    a <<= props;
}

inline bool operator== (const Connection_struct& s1, const Connection_struct& s2) {
    if (s1.connection_type!=s2.connection_type)
        return false;
    if (s1.ip_address!=s2.ip_address)
        return false;
    if (s1.byte_swap!=s2.byte_swap)
        return false;
    if (s1.ports!=s2.ports)
        return false;
    return true;
}

inline bool operator!= (const Connection_struct& s1, const Connection_struct& s2) {
    return !(s1==s2);
}

struct ConnectionStat_struct {
    ConnectionStat_struct ()
    {
    };

    static std::string getId() {
        return std::string("ConnectionStat");
    };

    std::string ip_address;
    unsigned short port;
    std::string status;
    float bytes_per_second;
    double bytes_sent;
};

inline bool operator>>= (const CORBA::Any& a, ConnectionStat_struct& s) {
    CF::Properties* temp;
    if (!(a >>= temp)) return false;
    const redhawk::PropertyMap& props = redhawk::PropertyMap::cast(*temp);
    if (props.contains("ConnectionStat::ip_address")) {
        if (!(props["ConnectionStat::ip_address"] >>= s.ip_address)) return false;
    }
    if (props.contains("ConnectionStat::port")) {
        if (!(props["ConnectionStat::port"] >>= s.port)) return false;
    }
    if (props.contains("ConnectionStat::status")) {
        if (!(props["ConnectionStat::status"] >>= s.status)) return false;
    }
    if (props.contains("ConnectionStat::bytes_per_second")) {
        if (!(props["ConnectionStat::bytes_per_second"] >>= s.bytes_per_second)) return false;
    }
    if (props.contains("ConnectionStat::bytes_sent")) {
        if (!(props["ConnectionStat::bytes_sent"] >>= s.bytes_sent)) return false;
    }
    return true;
}

inline void operator<<= (CORBA::Any& a, const ConnectionStat_struct& s) {
    redhawk::PropertyMap props;
 
    props["ConnectionStat::ip_address"] = s.ip_address;
 
    props["ConnectionStat::port"] = s.port;
 
    props["ConnectionStat::status"] = s.status;
 
    props["ConnectionStat::bytes_per_second"] = s.bytes_per_second;
 
    props["ConnectionStat::bytes_sent"] = s.bytes_sent;
    a <<= props;
}

inline bool operator== (const ConnectionStat_struct& s1, const ConnectionStat_struct& s2) {
    if (s1.ip_address!=s2.ip_address)
        return false;
    if (s1.port!=s2.port)
        return false;
    if (s1.status!=s2.status)
        return false;
    if (s1.bytes_per_second!=s2.bytes_per_second)
        return false;
    if (s1.bytes_sent!=s2.bytes_sent)
        return false;
    return true;
}

inline bool operator!= (const ConnectionStat_struct& s1, const ConnectionStat_struct& s2) {
    return !(s1==s2);
}

#endif // STRUCTPROPS_H
