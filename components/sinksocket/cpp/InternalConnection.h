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

#ifndef INTERNALCONNECTION_H_
#define INTERNALCONNECTION_H_

#include "BoostClient.h"
#include "BoostServer.h"
#include "quickstats.h"
#include "struct_props.h"


typedef std::map<unsigned short, double> portBytesMap;
typedef std::map<unsigned short, unsigned short> portByteSwapMap;
typedef std::map<unsigned short, client *> portClientMap;
typedef std::map<unsigned short, server *> portServerMap;
typedef std::map<unsigned short, QuickStats *> portStatsMap;

/*
 * This class manages server or client connections
 * based on a Connection_struct, returning
 * ConnectionStat_struct(s) to notify the owner of
 * an object of this type's current status
 */
class InternalConnection {
	ENABLE_LOGGING
public:
	InternalConnection();
	InternalConnection(const Connection_struct &connection);
	virtual ~InternalConnection();

private:
	/* Make the copy constructor private so that it
	 * can't be called by anyone else.  The internal
	 * pointers require move semantics as opposed to
	 * copy semantics
	 */
	InternalConnection(const InternalConnection &copy);

public:
	std::vector<unsigned short> getByteSwaps() const;

	bool operator==(const Connection_struct &connection) const;
	std::vector<ConnectionStat_struct> setConnection(const Connection_struct &connection);

	template <typename T, typename U>
	std::vector<ConnectionStat_struct> write(std::vector<T, U> &data);

	std::vector<ConnectionStat_struct> writeByteSwap(std::map<unsigned short, std::vector<char> > &dataMap);

private:
	void cleanUp();
	ConnectionStat_struct createClientConnection(const unsigned short &port, const std::string &ip);
	ConnectionStat_struct createServerConnection(const unsigned short &port);
	std::vector<ConnectionStat_struct> populateClientMap(const Connection_struct &connection);
	std::vector<ConnectionStat_struct> populateServerMap(const Connection_struct &connection);

private:
	portStatsMap bytesPerSec;
	portByteSwapMap byteSwaps;
	portBytesMap bytesSent;
	portClientMap *clients;
	Connection_struct connectionInfo;
	portServerMap *servers;
};

#include "InternalConnectionTemplate.h"

#endif /* INTERNALCONNECTION_H_ */
