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
 
#ifndef SINKSOCKET_IMPL_H
#define SINKSOCKET_IMPL_H

#include "sinksocket_base.h"
#include "BoostClient.h"
#include "BoostServer.h"
#include "InternalConnection.h"
#include "quickstats.h"

#include <vector>

class sinksocket_i;

class sinksocket_i : public sinksocket_base
{
	ENABLE_LOGGING
public:
	sinksocket_i(const char *uuid, const char *label);
    void constructor();
	~sinksocket_i();
	int serviceFunction();
	template<typename T>
	int serviceFunctionT(T* inputPort);
private:
	template<typename T, typename U>
	void createByteSwappedVector(const std::vector<T, U> &original, unsigned short byteSwap);

	template<typename T, typename U>
	void sendData(std::vector<T, U>& outData);

	template<typename T, typename U>
	void newData(std::vector<T, U>& newData);

	float bytesPerSecTemp;
	std::map<std::string, std::map<unsigned short, std::vector<char> > > byteSwapped;
	std::vector<InternalConnection *> internalConnections;
	std::map<std::string, std::map<unsigned short, std::vector<char> > > leftovers;
	bool onlyByteSwaps;
	bool performByteSwap;
	boost::recursive_mutex socketsLock_;
	double totalBytesTemp;

	//Property Change Listener
	void ConnectionsChanged(const std::vector<Connection_struct> *oldValue, const std::vector<Connection_struct> *newValue);
};

#endif
