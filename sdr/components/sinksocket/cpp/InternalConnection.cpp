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

#include "InternalConnection.h"

PREPARE_LOGGING(InternalConnection)

/*
 * Initialize the stored connection type to
 * be empty so that an initial call to set the
 * connection will properly initialize the list
 * of servers or clients
 */
InternalConnection::InternalConnection() :
	clients(NULL),
	servers(NULL)
{
	LOG_TRACE(InternalConnection, __PRETTY_FUNCTION__);

	connectionInfo.connection_type = "";
}

/*
 * Given a Connection_struct, initialize the
 * list of servers or clients
 */
InternalConnection::InternalConnection(const Connection_struct &connection) :
	clients(NULL),
	servers(NULL)
{
	LOG_TRACE(InternalConnection, __PRETTY_FUNCTION__);

	connectionInfo.connection_type = "";

	setConnection(connection);
}

/*
 * Erase the byteSent counters and delete and
 * erase the bytesPerSec objects, client objects,
 * and/or server objects
 */
void InternalConnection::cleanUp()
{
	LOG_TRACE(InternalConnection, __PRETTY_FUNCTION__);

	// Erase all byte total mappings
	bytesSent.clear();

	// Delete and erase all statistics mappings
	for (portStatsMap::iterator i = bytesPerSec.begin(); i != bytesPerSec.end(); ++i) {
		delete i->second;
	}

	bytesPerSec.clear();

	byteSwaps.clear();

	// If the clients exist, delete and erase all client mappings,
	// and the map itself
	if (clients) {
		LOG_DEBUG(InternalConnection, "Deleting client maps");

		for (portClientMap::iterator i = clients->begin(); i != clients->end(); ++i) {
			delete i->second;
		}

		delete clients;
		clients = NULL;
	}

	// If the servers exist, delete and erase all server mappings,
	// and the map itself
	if (servers) {
		LOG_DEBUG(InternalConnection, "Deleting server map");

		for (portServerMap::iterator i = servers->begin(); i != servers->end(); ++i) {
			delete i->second;
		}

		delete servers;
		servers = NULL;
	}
}

/*
 * Given a port and IP address, create a client
 * object and initialize the relevant information
 * for that object, while returning the statistic
 * information
 */
ConnectionStat_struct InternalConnection::createClientConnection(const unsigned short &port, const std::string &ip)
{
	LOG_TRACE(InternalConnection, __PRETTY_FUNCTION__);
	LOG_INFO(InternalConnection, "Creating client connection to " << ip << ":" << port);

	client *newClient = NULL;

	// Populate the statistic struct with initial values appropriate
	// for a client
	ConnectionStat_struct statistic;
	statistic.bytes_per_second = 0;
	statistic.bytes_sent = 0;
	statistic.ip_address = ip;
	statistic.port = port;
	statistic.status = "startup";

	try {
		// Instantiate a client
		newClient = new client(port, ip);

		// Try to connect the client and save the status
		if (newClient->connect()) {
			statistic.status = "connected";
		} else {
			statistic.status = "not_connected";
		}

		// Make a new QuickStats pair, bytesSent pair, and clients pair
		bytesPerSec.insert(std::make_pair(port, new QuickStats));
		bytesSent.insert(std::make_pair(port, 0));
		clients->insert(std::make_pair(port, newClient));
	} catch(std::exception &e) {
		LOG_ERROR(InternalConnection, "Unable to create client connection to " << ip << ":" << port);

		if (newClient) {
			delete newClient;
		}

		statistic.status = "error";
	}

	return statistic;
}

/*
 * Given a port, create a server object and initialize
 * the relevant information for that object, while
 * returning the statistic information
 */
ConnectionStat_struct InternalConnection::createServerConnection(const unsigned short &port)
{
	LOG_TRACE(InternalConnection, __PRETTY_FUNCTION__);
	LOG_INFO(InternalConnection, "Creating server listening on port " << port);

	server *newServer = NULL;

	// Populate the statistic struct with initial values appropriate
	// for a server
	ConnectionStat_struct statistic;
	statistic.bytes_per_second = 0;
	statistic.bytes_sent = 0;
	statistic.ip_address = "";
	statistic.port = port;
	statistic.status = "startup";

	try {
		// Instantiate a server
		newServer = new server(port);

		// Check if the server has a connection and save the status
		if (newServer->is_connected()) {
			statistic.status = "connected";
		} else {
			statistic.status = "not_connected";
		}

		// Make a new QuickStats pair, bytesSent pair, and servers pair
		bytesPerSec.insert(std::make_pair(port, new QuickStats));
		bytesSent.insert(std::make_pair(port, 0));
		servers->insert(std::make_pair(port, newServer));
	} catch(std::exception &e) {
		LOG_ERROR(InternalConnection, "Unable to create server listening on port " << port);

		if (newServer) {
			delete newServer;
		}

		statistic.status = "error";
	}

	return statistic;
}

std::vector<unsigned short> InternalConnection::getByteSwaps() const
{
	return connectionInfo.byte_swap;
}

/*
 * A custom equals operator for comparing an Internal
 * Connection to a Connection_struct, which only
 * checks for equality of the connection type and
 * IP address
 */
bool InternalConnection::operator==(const Connection_struct &connection) const
{
	return (connectionInfo == connection);
}

/*
 * Given a Connection, iterate over all of the ports
 * and create client connections with the specified
 * port, IP address, and byte swap value, while
 * returning the statistic information for each
 * created connection
 */
std::vector<ConnectionStat_struct> InternalConnection::populateClientMap(const Connection_struct &connection)
{
	LOG_TRACE(InternalConnection, __PRETTY_FUNCTION__);

	std::vector<ConnectionStat_struct> statistics;

	for (std::vector<unsigned short>::const_iterator i = connection.ports.begin(); i != connection.ports.end(); ++i) {
		statistics.push_back(createClientConnection(*i, connection.ip_address));
	}

	return statistics;
}

/*
 * Given a Connection, iterate over all of the ports
 * and create server connections with the specified
 * port and byte swap value, while returning the
 * statistic information for each created connection
 */
std::vector<ConnectionStat_struct> InternalConnection::populateServerMap(const Connection_struct &connection)
{
	LOG_TRACE(InternalConnection, __PRETTY_FUNCTION__);

	std::vector<ConnectionStat_struct> statistics;

	for (std::vector<unsigned short>::const_iterator i = connection.ports.begin(); i != connection.ports.end(); ++i) {
		statistics.push_back(createServerConnection(*i));
	}

	return statistics;
}

/*
 * Given a Connection, determine which type of
 * connection (client/server) to create or manage
 * an existing connection
 */
std::vector<ConnectionStat_struct> InternalConnection::setConnection(const Connection_struct &connection)
{
	LOG_TRACE(InternalConnection, __PRETTY_FUNCTION__);

	// Make a vector of Connection Statistics to return
	std::vector<ConnectionStat_struct> statistics;

	// Guard against an invalid connection type
	if (connection.connection_type != "client" && connection.connection_type != "server") {
		LOG_ERROR(InternalConnection, "Attempted to set connection type to \"" << connection.connection_type << "\"");

		return statistics;
	}

	// If the connection type has changed, everything needs to be
	// deleted and created from scratch.  Otherwise, only update
	// the parts that have changed
	if (connectionInfo.connection_type != connection.connection_type ) {
		LOG_INFO(InternalConnection, "Connection type has changed, deleting old connections");

		cleanUp();

		if (connection.connection_type == "client") {
			LOG_DEBUG(InternalConnection, "Creating client map");

			clients = new portClientMap();

			statistics = populateClientMap(connection);

			// Save the connection information for later
			connectionInfo = connection;
		} else {
			LOG_DEBUG(InternalConnection, "Creating server map");

			servers = new portServerMap();

			statistics = populateServerMap(connection);

			// Save the connection information for later
			connectionInfo = connection;
			connectionInfo.ip_address = "";
		}
	} else {
		if (connection.connection_type == "client") {
			// If the IP address has changed, all of the connections need
			// to be restarted
			if (connectionInfo.ip_address != connection.ip_address) {
				cleanUp();

				clients = new portClientMap();

				statistics = populateClientMap(connection);
			}
			// If the ports have changed, some connections may stay the
			// same
			else if (connectionInfo.ports != connection.ports) {
				// Keep track of which byte swap value to use
				int counter = 0;

				// Check for added ports
				for (std::vector<unsigned short>::const_iterator i = connection.ports.begin(); i != connection.ports.end(); ++i, ++counter) {
					if (find(connectionInfo.ports.begin(), connectionInfo.ports.end(), *i) == connectionInfo.ports.end()) {
						statistics.push_back(createClientConnection(*i, connection.ip_address));
					}
				}

				// Check for removed ports
				for (std::vector<unsigned short>::const_iterator i = connectionInfo.ports.begin(); i != connectionInfo.ports.end(); ++i) {
					if (find(connection.ports.begin(), connection.ports.end(), *i) == connection.ports.end()) {
						delete bytesPerSec.at(*i);
						bytesPerSec.erase(*i);
						bytesSent.erase(*i);
						byteSwaps.erase(*i);
						delete clients->at(*i);
						clients->erase(*i);
					}
				}
			}

			// Save the connection information for later
			connectionInfo = connection;
		} else {
			// If the ports have changed, some connections may stay the
			// same
			if (connectionInfo.ports != connection.ports) {
				// Keep track of which byte swap value to use
				int counter = 0;

				// Check for added ports
				for (std::vector<unsigned short>::const_iterator i = connection.ports.begin(); i != connection.ports.end(); ++i, ++counter) {
					if (find(connectionInfo.ports.begin(), connectionInfo.ports.end(), *i) == connectionInfo.ports.end()) {
						statistics.push_back(createServerConnection(*i));
					}
				}

				// Check for removed ports
				for (std::vector<unsigned short>::const_iterator i = connectionInfo.ports.begin(); i != connectionInfo.ports.end(); ++i) {
					if (find(connection.ports.begin(), connection.ports.end(), *i) == connection.ports.end()) {
						delete bytesPerSec.at(*i);
						bytesPerSec.erase(*i);
						bytesSent.erase(*i);
						delete servers->at(*i);
						servers->erase(*i);
					}
				}
			}

			// Save the connection information for later
			connectionInfo = connection;
			connectionInfo.ip_address = "";
		}
	}

	// Re-build the byte swap map
	int counter = 0;

	byteSwaps.clear();

	// Catch all for byte swaps changed
	for (std::vector<unsigned short>::const_iterator i = connection.ports.begin(); i != connection.ports.end(); ++i, ++counter) {
		byteSwaps[*i] = connection.byte_swap[counter];
	}

	return statistics;
}

std::vector<ConnectionStat_struct> InternalConnection::writeByteSwap(std::map<unsigned short, std::vector<char> > &dataMap)
{
	LOG_TRACE(InternalConnection, __PRETTY_FUNCTION__);

	// Make a vector of Connection Statistics to return
	std::vector<ConnectionStat_struct> statistics;

	if (connectionInfo.connection_type == "client" && clients) {
		statistics.reserve(clients->size());

		for (portClientMap::iterator i = clients->begin(); i != clients->end(); ++i) {
			ConnectionStat_struct statistic;

			statistic.ip_address = connectionInfo.ip_address;
			statistic.port = i->first;

			if (i->second->connect_if_necessary()) {
				statistic.status = "connected";

				i->second->write(dataMap[byteSwaps[i->first]]);

				size_t pktSize = dataMap[byteSwaps[i->first]].size();

				statistic.bytes_per_second = bytesPerSec[i->first]->newPacket(pktSize);
				statistic.bytes_sent = (bytesSent[i->first] += pktSize);
			} else {
				statistic.status = "not_connected";
				statistic.bytes_per_second = bytesPerSec[i->first]->newPacket(0);
			}

			statistics.push_back(statistic);
		}
	} else if (connectionInfo.connection_type == "server" && servers) {
		statistics.reserve(servers->size());

		for (portServerMap::iterator i = servers->begin(); i != servers->end(); ++i) {
			ConnectionStat_struct statistic;

			statistic.ip_address = "";
			statistic.port = i->first;

			if (i->second->is_connected()) {
				statistic.status = "connected";

				i->second->write(dataMap[byteSwaps[i->first]]);

				size_t pktSize = dataMap[byteSwaps[i->first]].size();

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

InternalConnection::~InternalConnection()
{
	LOG_TRACE(InternalConnection, __PRETTY_FUNCTION__);

	cleanUp();
}

