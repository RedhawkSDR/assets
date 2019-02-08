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

/**************************************************************************

    This is the component code. This file contains the child class where
    custom functionality can be added to the component. Custom
    functionality to the base class can be extended here. Access to
    the ports can also be done from this class

**************************************************************************/

#include "sinksocket.h"
#include "vectorswap.h"
#include <sstream>

// Because the vector of internal connections must store pointers to avoid
// reconnecting whenever the vector is resized, this operator must be
// defined to search the vector.  In C++, move semantics could be used
// and the pointers would be unnecessary
bool operator==(const InternalConnection *lhs, const Connection_struct &rhs)
{
	return (*lhs) == rhs;
}

PREPARE_LOGGING(sinksocket_i)

sinksocket_i::sinksocket_i(const char *uuid, const char *label) :
    sinksocket_base(uuid, label)
{
	bytesPerSecTemp = 0;
	bytes_per_sec = 0;
	onlyByteSwaps = false;
	performByteSwap = false;
	totalBytesTemp = 0;
	total_bytes = 0;
}

sinksocket_i::~sinksocket_i()
{
	boost::recursive_mutex::scoped_lock lock(socketsLock_);

	for (std::vector<InternalConnection *>::iterator i = internalConnections.begin(); i != internalConnections.end(); ++i) {
		delete *i;
	}
}

void sinksocket_i::constructor()
{
    /***********************************************************************************
     This is the RH constructor. All properties are properly initialized before this function is called
    ***********************************************************************************/
	ConnectionsChanged(NULL,&Connections); // apply initial property configuration
	addPropertyChangeListener("Connections", this, &sinksocket_i::ConnectionsChanged);
}

template<typename T, typename U>
void sinksocket_i::createByteSwappedVector(const std::vector<T, U> &original, unsigned short byteSwap) {
	unsigned int numSwap = byteSwap;
	size_t dataSize = sizeof(T);

	// If 1 is requested, use the word size associated with the data
	if (numSwap == 1) {
		numSwap = dataSize;
	}

	size_t numBytes = original.size() * dataSize;
	size_t oldLeftoverSize = leftovers[typeid(T).name()][byteSwap].size();
	size_t totalSize = numBytes + oldLeftoverSize;
	size_t newLeftoverSize;

	// Create the vector to hold the swapped data
	std::vector<char> newData;

	// Make sure to send an exact multiple of numSwap if it's greater than 1
	if (numSwap > 1) {
		newLeftoverSize = totalSize % numSwap;

		if (numSwap != dataSize) {
			LOG_WARN(sinksocket_i, "Data size of " << dataSize << " is not equal to byte swap size  of " << numSwap <<".");
		}
	} else {
		newLeftoverSize = 0;
	}

	//Don't have to deal with leftover data.  This should be the typical case
	if (newLeftoverSize == 0 && oldLeftoverSize == 0) {
		if (numSwap > 1) {
			newData.resize(numBytes);
			vectorSwap(reinterpret_cast<const char *>(original.data()), newData, numSwap);
			byteSwapped[typeid(T).name()][byteSwap] = newData;
		}
	}
	else
	{
		LOG_WARN(sinksocket_i, "Byte swapping and packet sizes are not compatible.  Swapping bytes over adjacent packets");

		newData.reserve(totalSize - newLeftoverSize);
		newData.insert(newData.begin(), leftovers[typeid(T).name()][byteSwap].begin(), leftovers[typeid(T).name()][byteSwap].end());
		newData.insert(newData.begin() + oldLeftoverSize, reinterpret_cast<const char *>(original.data()), reinterpret_cast<const char *>(original.data()) + numBytes - newLeftoverSize);

		if (numSwap > 1) {
			vectorSwap(newData, numSwap);
		}

		byteSwapped[typeid(T).name()][byteSwap] = newData;
		leftovers[typeid(T).name()][byteSwap].clear();

		// If we have new leftovers, populate it now
		if (newLeftoverSize != 0) {
			leftovers[typeid(T).name()][byteSwap].insert(leftovers[typeid(T).name()][byteSwap].begin(), reinterpret_cast<const char *>(original.data()) + numBytes - newLeftoverSize, reinterpret_cast<const char *>(original.data()) + numBytes);
		}
	}
}

void sinksocket_i::ConnectionsChanged(const std::vector<Connection_struct> *oldValue, const std::vector<Connection_struct> *newValue)
{
	// First, clear out any server IP addresses and make sure the byte
	// swap and port lists are the same size
	std::vector<Connection_struct> cleanList;

	for (std::vector<Connection_struct>::const_iterator i = newValue->begin(); i != newValue->end(); ++i) {
		Connection_struct cleaned = *i;

		// Adjust the byte swap size to match the number of ports
		if (cleaned.ports.size() != cleaned.byte_swap.size()) {
			LOG_WARN(sinksocket_i, "Port list and Byte Swap list differ in size, resizing");

			cleaned.byte_swap.resize(cleaned.ports.size(), 0);
		}

		// Remove the IP address for a server connection
		if (cleaned.connection_type == "server" && cleaned.ip_address != "") {
			LOG_WARN(sinksocket_i, "IP Address specified for server connection, removing");

			cleaned.ip_address = "";
		}

		cleanList.push_back(cleaned);
	}

	// Now coalesce any servers or clients with duplicate information
	std::vector<Connection_struct> duplicateFree;

	for (std::vector<Connection_struct>::const_iterator i = cleanList.begin(); i != cleanList.end(); ++i) {
		bool found = false;
		std::vector<Connection_struct>::iterator j;

		// Check if the duplicate list already contains an entry with
		// a matching connection type and IP
		for (j = duplicateFree.begin(); j != duplicateFree.end(); ++j) {
			if (i->connection_type == j->connection_type && i->ip_address == j->ip_address) {
				found = true;
				break;
			}
		}

		// Augment the existing entry to contain the new data
		if (found) {
			Connection_struct combined;

			combined.connection_type = j->connection_type;
			combined.ip_address = j->ip_address;

			// Vectors used for combining and preserving the order
			// of the ports and byte swaps lists
			std::vector<unsigned short> newPorts;
			std::vector<unsigned short> oldPorts;
			std::vector<unsigned short> newByteSwaps;
			std::vector<unsigned short> oldByteSwaps;

			oldPorts.insert(oldPorts.end(), i->ports.begin(), i->ports.end());
			oldPorts.insert(oldPorts.end(), j->ports.begin(), j->ports.end());
			oldByteSwaps.insert(oldByteSwaps.end(), i->byte_swap.begin(), i->byte_swap.end());
			oldByteSwaps.insert(oldByteSwaps.end(), j->byte_swap.begin(), j->byte_swap.end());

			while (oldPorts.size() != 0) {
				int counter = 0;
				unsigned int minSoFar = 65536;
				std::vector<unsigned short>::iterator minPort;
				std::vector<unsigned short>::iterator correspondingByteSwap;

				// Search for the minimum port in the current list and
				// make note of its position and the position of its
				// corresponding byte_swap value
				for (std::vector<unsigned short>::iterator i = oldPorts.begin(); i != oldPorts.end(); ++i, ++counter) {
					if (*i < minSoFar) {
						minSoFar = *i;
						minPort = i;
						correspondingByteSwap = oldByteSwaps.begin() + counter;
					}
				}

				// Add the port and byte swap values to the new vectors
				newPorts.insert(newPorts.end(), *minPort);
				newByteSwaps.insert(newByteSwaps.end(), *correspondingByteSwap);

				// Remove the port and byte swap values from the old vectors
				oldPorts.erase(minPort);
				oldByteSwaps.erase(correspondingByteSwap);
			}

			// Set the entry's values
			combined.ports = newPorts;
			combined.byte_swap = newByteSwaps;

			*j = combined;
		} else {
			// A matching entry wasn't found, add it to the back of the list
			duplicateFree.push_back(*i);
		}
	}

	// Set the property to match the clean and duplicate free version
	Connections = duplicateFree;

	boost::recursive_mutex::scoped_lock lock(socketsLock_);

	// Reinitialize the onlyByteSwaps and performByteSwap members
	// and then set them appropriately
	onlyByteSwaps = true;
	performByteSwap = false;

	// Keep a list of stats to populate the ConnectionStats property
	std::vector<ConnectionStat_struct> stats;
	std::vector<ConnectionStat_struct> returned;

	// Add and update the current connections
	for (std::vector<Connection_struct>::const_iterator i = duplicateFree.begin(); i != duplicateFree.end(); ++i) {
		std::vector<InternalConnection *>::iterator found = find(internalConnections.begin(), internalConnections.end(), *i);

		// This is a brand new connection
		if (found == internalConnections.end()) {
			LOG_DEBUG(sinksocket_i, "Adding new internal connection");
			internalConnections.push_back(new InternalConnection());

			returned = internalConnections.back()->setConnection(*i);
		} else {
			LOG_DEBUG(sinksocket_i, "Updating existing internal connection");
			// A connection with this same connection type and IP exists
			// so only update the relevant information to preserve
			// existing connections
			returned = (*found)->setConnection(*i);
		}

		stats.insert(stats.end(), returned.begin(), returned.end());

		// Set the onlyByteSwaps flag if necessary
		if (onlyByteSwaps) {
			for (std::vector<unsigned short>::const_iterator j = i->byte_swap.begin(); j != i->byte_swap.end(); ++j) {
				if (not (onlyByteSwaps &= (*j != 0))) {
					break;
				}
			}
		}

		// Set the performByteSwap flag if necessary
		if (not performByteSwap) {
			for (std::vector<unsigned short>::const_iterator j = i->byte_swap.begin(); j != i->byte_swap.end(); ++j) {
				if ((performByteSwap |= (*j != 0))) {
					break;
				}
			}
		}
	}

	ConnectionStats = stats;

	// Remove from the current connections
	if (oldValue != NULL){
		for (std::vector<Connection_struct>::const_iterator i = oldValue->begin(); i != oldValue->end(); ++i) {
			// If the value exists in the old property but not in the duplicate
			// free version, it has been removed
			if (find(duplicateFree.begin(), duplicateFree.end(), *i) == duplicateFree.end()) {
				std::vector<InternalConnection *>::iterator found = find(internalConnections.begin(), internalConnections.end(), *i);

				// If found, delete and remove the entry
				if (found != internalConnections.end()) {
					delete *found;
					internalConnections.erase(found);
				} else {
					LOG_ERROR(sinksocket_i, "Unable to find connection data for removal");
				}
			}
		}
	}
}

int sinksocket_i::serviceFunction()
{
	  int ret = 0;

	  ret += serviceFunctionT(dataOctet_in);
	  ret += serviceFunctionT(dataChar_in);
	  ret += serviceFunctionT(dataShort_in);
	  ret += serviceFunctionT(dataUshort_in);
	  ret += serviceFunctionT(dataLong_in);
	  ret += serviceFunctionT(dataUlong_in);
	  ret += serviceFunctionT(dataFloat_in);
	  ret += serviceFunctionT(dataDouble_in);

	  if (ret > 1)
	  {
		  LOG_WARN(sinksocket_i, "More than one data port received data");
	  	  return NORMAL;
	  }

	  return ret;
}

template<typename T>
int sinksocket_i::serviceFunctionT(T* inputPort)
{
	LOG_TRACE(sinksocket_i, __PRETTY_FUNCTION__);
	typename T::dataTransfer *packet = inputPort->getPacket(0.0);

	if (not packet) {
		return NOOP;
	}

	if (packet->inputQueueFlushed) {
		LOG_WARN(sinksocket_i, "Input Queue Flushed");
	}

	boost::recursive_mutex::scoped_lock lock(socketsLock_);

	// Keep a list of stats to populate the ConnectionStats property
	std::vector<ConnectionStat_struct> stats;
	std::vector<ConnectionStat_struct> returned;

	// Avoid unnecessary processing and allocation if no byte swaps
	// are being performed
	if (performByteSwap) {
		// Use the data type as the key into the byteSwapped and
		// leftovers member maps
		std::string byteSwapKey = typeid(packet->dataBuffer[0]).name();

		// This copy isn't necessary if all of the connections require
		// byte swaps
		if (not onlyByteSwaps) {
			byteSwapped[byteSwapKey][0] = std::vector<char>(reinterpret_cast<char *>(packet->dataBuffer.data()),
																				reinterpret_cast<char *>(packet->dataBuffer.data()) + packet->dataBuffer.size() * sizeof(packet->dataBuffer[0]));
		}

		// Iterate through the internal connections, building the byte
		// swapped vectors as necessary.  This should prevent multiple
		// byte swaps for the same byte swap values from being performed
		// in the same service function call
		for (std::vector<InternalConnection *>::iterator i = internalConnections.begin(); i != internalConnections.end(); ++i) {
			std::vector<unsigned short> byteSwaps = (*i)->getByteSwaps();

			for (std::vector<unsigned short>::iterator j = byteSwaps.begin(); j != byteSwaps.end(); ++j) {
				if (*j != 0) {
					if (byteSwapped[byteSwapKey].find(*j) == byteSwapped[byteSwapKey].end()) {
						createByteSwappedVector(packet->dataBuffer, *j);
					}
				}
			}

			returned = (*i)->writeByteSwap(byteSwapped[byteSwapKey]);

			stats.insert(stats.end(), returned.begin(), returned.end());
		}

		byteSwapped[byteSwapKey].clear();
	} else {
		// Iterate through the internal connections and write the data buffer
		for (std::vector<InternalConnection *>::iterator i = internalConnections.begin(); i != internalConnections.end(); ++i) {
			returned = (*i)->write(packet->dataBuffer);

			stats.insert(stats.end(), returned.begin(), returned.end());
		}
	}

	// Update the properties
	bytesPerSecTemp = 0;
	totalBytesTemp = 0;

	for (std::vector<ConnectionStat_struct>::const_iterator i = stats.begin(); i != stats.end(); ++i) {
		bytesPerSecTemp += i->bytes_per_second;
		totalBytesTemp += i->bytes_sent;
	}

	bytes_per_sec = bytesPerSecTemp;
	ConnectionStats = stats;
	total_bytes = totalBytesTemp;

	if (packet) {
		delete packet;
	}

	return NORMAL;
}
