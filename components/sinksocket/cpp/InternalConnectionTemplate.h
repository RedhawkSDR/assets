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

#ifndef INTERNALCONNECTIONTEMPLATE_H_
#define INTERNALCONNECTIONTEMPLATE_H_

#include "InternalConnection.h"

template <typename T, typename U>
std::vector<ConnectionStat_struct> InternalConnection::write(std::vector<T, U> &data)
{
	LOG_TRACE(InternalConnection, __PRETTY_FUNCTION__);

	// Make a vector of Connection Statistics to return
	std::vector<ConnectionStat_struct> statistics;

	if (connectionInfo.connection_type == "client" && clients) {
		for (portClientMap::iterator i = clients->begin(); i != clients->end(); ++i) {
			ConnectionStat_struct statistic;

			statistic.ip_address = connectionInfo.ip_address;
			statistic.port = i->first;

			if (i->second->connect_if_necessary()) {
				statistic.status = "connected";

				i->second->write(data);

				size_t pktSize = data.size() * sizeof(T);

				statistic.bytes_per_second = bytesPerSec[i->first]->newPacket(pktSize);
				statistic.bytes_sent = (bytesSent[i->first] += pktSize);
			} else {
				statistic.status = "not_connected";
				statistic.bytes_per_second = bytesPerSec[i->first]->newPacket(0);
			}

			statistics.push_back(statistic);
		}
	} else if (connectionInfo.connection_type == "server" && servers) {

		for (portServerMap::iterator i = servers->begin(); i != servers->end(); ++i) {
			ConnectionStat_struct statistic;

			statistic.ip_address = "";
			statistic.port = i->first;

			if (i->second->is_connected()) {
				statistic.status = "connected";

				i->second->write(data);

				size_t pktSize = data.size() * sizeof(T);

				statistic.bytes_per_second = bytesPerSec[i->first]->newPacket(pktSize);
				statistic.bytes_sent = (bytesSent[i->first] += pktSize);
			} else {
				statistic.status = "not_connected";
				statistic.bytes_per_second = bytesPerSec[i->first]->newPacket(0);
			}

			statistics.push_back(statistic);
		}
	} else {
		LOG_ERROR(InternalConnection, "Invalid conditions for writing data");
	}

	return statistics;
}

#endif /* INTERNALCONNECTIONTEMPLATE_H_ */
