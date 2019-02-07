#!/usr/bin/env python
#
# This file is protected by Copyright. Please refer to the COPYRIGHT file distributed with this 
# source distribution.
# 
# This file is part of REDHAWK Basic Components sinksocket.
# 
# REDHAWK Basic Components sinksocket is free software: you can redistribute it and/or modify it under the terms of 
# the GNU Lesser General Public License as published by the Free Software Foundation, either 
# version 3 of the License, or (at your option) any later version.
# 
# REDHAWK Basic Components sinksocket is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
# without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
# PURPOSE.  See the GNU Lesser General Public License for more details.
# 
# You should have received a copy of the GNU Lesser General Public License along with this 
# program.  If not, see http://www.gnu.org/licenses/.
#
try:
    from bulkio.bulkioInterfaces import BULKIO as _BULKIO
    from bulkio.bulkioInterfaces import BULKIO__POA as _BULKIO__POA
except:
    # Handle case where bulkioInterface may not be installed
    pass

import ossie.utils.bulkio.bulkio_helpers as _bulkio_helpers
import ossie.utils.bulkio.bulkio_data_helpers as _bulkio_data_helpers
import logging as _logging

import Queue as _Queue
import select
import socket
import struct
import threading as _threading
import time as _time

from ossie.utils.sb import io_helpers

log = _logging.getLogger(__name__)

class NetworkSource(io_helpers._SourceBase):
    def __init__(self,
                 bytesPerPush = 512000):

        self.threadExited = None

        io_helpers._SourceBase.__init__(self,
                                        bytesPerPush=bytesPerPush,
                                        dataFormat=None)

        # Properties
        self.byte_swap       = 0
        self.bytes_per_sec   = 0
        self.connection_type = "server"
        self.ip_address      = ""
        self.max_bytes       = 16384
        self.min_bytes       = 16384
        self.port            = 32191
        self.total_bytes     = 0

        # Internal Members
        self._buffer       = ""
        self._dataQueue    = _Queue.Queue()
        self._dataSocket   = None
        self._leftovers    = {}
        self._multSize     = 8
        self._runThread    = None
        self._serverSocket = None
        self._startTime    = 0.0

        self.ref = self

        self._openSocket()

    def _closeSockets(self):
        """
        Close the data and/or server sockets
        """
        if self._dataSocket:
            self._dataSocket.shutdown(socket.SHUT_RDWR)
            self._dataSocket.close()
            self._dataSocket = None

        if self._serverSocket:
            self._serverSocket.shutdown(socket.SHUT_RDWR)
            self._serverSocket.close()
            self._serverSocket = None

    def _createArraySrcInst(self, srcPortType):
        if srcPortType != "_BULKIO__POA.dataXML":
            return _bulkio_data_helpers.ArraySource(eval(srcPortType))
        else:
            return _bulkio_data_helpers.XmlArraySource(eval(srcPortType))

    def __del__(self):
        self._closeSockets()

    def _flip(self,
             dataStr,
             numBytes):
        """
        Given data packed into a string, reverse bytes 
        for a given word length and return the 
        byte-flipped string
        """
        out = ""
    
        for i in xrange(len(dataStr)/numBytes):
            l = list(dataStr[numBytes*i:numBytes*(i+1)])
            l.reverse()
            out += (''.join(l))

        return out

    def _gcd(self,
             a,
             b):
        if (b == 0):
            return a

        return self._gcd(b, a % b)

    def _lcm(self,
             a,
             b):
        return a * b / (self._gcd(a, b))

    def _openSocket(self):
        """
        Open the data and/or server sockets
        based on the current properties
        """
        log.info("Connection Type: " + str(self.connection_type))
        log.info("IP Address: " + self.ip_address)
        log.info("Port: " + str(self.port))
        if self.connection_type == "server":
            self._dataSocket = None
            self._serverSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self._serverSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

            try:
                self._serverSocket.bind(("localhost", self.port))
            except Exception, e:
                log.error("Unable to bind socket: " + str(e))
                return

            self._serverSocket.listen(1)
        elif self.connection_type == "client":
            self._dataSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self._dataSocket.connect((self.ip_address, self.port))
            self._serverSocket = None
        else:
            log.error("Invalid connection type: " + self.connection_type)
            self._dataSocket = None
            self._serverSocket = None

    def _pushData(self,
                  connection,
                  dataString,
                  numBytes,
                  currentSampleTime):

        # Gather information about the connection
        arraySrcInst   = connection['arraySrcInst']
        bytesPerSample = 1
        srcPortType    = connection['srcPortType']

        for i in self.supportedPorts.values():
            if i['portType'] == srcPortType:
                bytesPerSample = i['bytesPerSample']
                break

        outputSize = numBytes / bytesPerSample

        # Set the byte swap to use
        if self.byte_swap == 1:
            byte_swap = bytesPerSample
        else:
            byte_swap = self.byte_swap
        
        # Perform the byte swap
        if byte_swap > 1:
            if byte_swap != bytesPerSample:
                log.warn("Data size " + str(bytesPerSample) + " is not equal to byte swap size " + str(byte_swap))

            output = self._flip(dataString[:numBytes], byte_swap)
        else:
            output = dataString[:numBytes]

        # Convert from a string to a list of
        # the type expected for the port
        outputList = self._stringToList(output, bytesPerSample, srcPortType)

        formattedOutput = _bulkio_helpers.formatData(outputList, BULKIOtype=eval(srcPortType))

        EOS = False

        if len(formattedOutput) == 0:
            EOS = True

        T = _BULKIO.PrecisionUTCTime(_BULKIO.TCM_CPU,
                                     _BULKIO.TCS_VALID,
                                     0.0,
                                     int(currentSampleTime),
                                     currentSampleTime - int(currentSampleTime))

        if srcPortType != "_BULKIO__POA.dataXML":
            _bulkio_data_helpers.ArraySource.pushPacket(arraySrcInst,
                                                        data     = formattedOutput,
                                                        T        = T,
                                                        EOS      = EOS,
                                                        streamID = 'testing')
        else:
            _bulkio_data_helpers.XmlArraySource.pushPacket(arraySrcInst,
                                                           data     = formattedOutput,
                                                           EOS      = EOS,
                                                           streamID = 'testing')

    def _pushThread(self):
        """
        The thread function for collecting
        data from the socket and pushing it
        to the ports
        """
        self.threadExited = False

        currentSampleTime = self._startTime

        while not self._exitThread:
            # Open the socket(s) if necessary
            if self._dataSocket == None:
                if self.connection_type == "server":
                    if self._serverSocket == None:
                        self._openSocket()
                    
                    log.debug("Waiting for client connection")
                    
                    ready = select.select([self._serverSocket], [], [], 1.0)

                    if not ready[0]:
                        log.info("No connections pending")
                        _time.sleep(0.1)
                        continue

                    (self._dataSocket, clientAddress) = self._serverSocket.accept()

                    log.debug("Got client connection: " + str(clientAddress))
                else:
                    self._openSocket()

                self._dataSocket.setblocking(0)

                _time.sleep(0.1)
                continue

            if len(self._connections.values()) == 0:
                log.warn("No connections to NetworkSource")
                _time.sleep(1.0)
                continue

            # Send packets of size max_bytes
            if len(self._buffer) >= self.max_bytes:
                numLoops = len(self._buffer) / self.max_bytes

                for i in range(0, numLoops):
                    for connection in self._connections.values():
                        self._pushData(connection, self._buffer[i * self.max_bytes : ], self.max_bytes, currentSampleTime)

                self._buffer = self._buffer[numLoops * self.max_bytes:]

            self._retrieveData()

            # Check if the thread was shut down
            # during retrieval before continuing
            if self._exitThread == True:
                break

            if len(self._buffer) != 0 and len(self._buffer) >= self.min_bytes:
                numLeft = len(self._buffer) % self._multSize
                pushBytes = len(self._buffer) - numLeft

                for connection in self._connections.values():
                    self._pushData(connection, self._buffer, pushBytes, currentSampleTime)

                self._buffer = self._buffer[len(self._buffer)-numLeft :]

        # Indicate that the thread has finished
        self.threadExited = True

    def releaseObject(self):
        self._closeSockets()

        io_helpers._SourceBase.releaseObject(self)

    def _retrieveData(self):
        """
        Use select to poll for data on the
        socket
        """
        ready = select.select([self._dataSocket], [], [], 1.0)

        if not ready[0]:
            log.info("Data not available")
            return None

        self._buffer = self._buffer + self._dataSocket.recv(1024)

    def setByte_swap(self,
                     byte_swap):
        """
        When this property changes, update the
        transfer length
        """
        if byte_swap != self.byte_swap:
            self.byte_swap = byte_swap
            self._updateTransferLength()

    def setConnection_type(self,
                           connection_type):
        """
        When this property changes, close the
        socket so it can be reopened with the
        new values
        """
        if connection_type != self.connection_type and (connection_type == "server" or connection_type == "client"):
            self.connection_type = connection_type
            self._closeSockets()

    def setIp_address(self,
                      ip_address):
        """
        When this property changes, close the
        socket so it can be reopened with the
        new values
        """
        if ip_address != self.ip_address:
            self.ip_address = ip_address
            self._closeSockets()

    def setMax_bytes(self,
                     max_bytes):
        """
        When this property changes, update the
        transfer length
        """
        if max_bytes != self.max_bytes:
            self.max_bytes = max_bytes
            self._updateTransferLength()

    def setMin_bytes(self,
                     min_bytes):
        """
        When this property changes, update the
        transfer length
        """
        if min_bytes != self.min_bytes:
            self.min_bytes = min_bytes
            self._updateTransferLength()

    def setPort(self,
                port):
        """
        When this property changes, close the
        socket so it can be reopened with the
        new values
        """
        if port != self.port:
            self.port = port
            self._closeSockets()

    def start(self):
        self._exitThread = False
        
        if self._runThread == None:
            self._runThread = _threading.Thread(target=self._pushThread)
            self._runThread.setDaemon(True)
            self._runThread.start()
        elif not self._runThread.isAlive():
            self._runThread = _threading.Thread(target=self._pushThread)
            self._runThread.setDaemon(True)
            self._runThread.start()

    def stop(self):
        self._exitThread = True

        if self.threadExited != None:
            timeout_count = 10

            while not self.threadExited:
                _time.sleep(0.1)
                timeout_count -= 1

                if timeout_count < 0:
                    raise AssertionError, self.className + ":stop() failed to exit thread"

    def _stringToList(self,
                      string,
                      bytesPerSample,
                      srcPortType):
        """
        Given a string, use the output port type to
        create a list representing the data
        """
        length = len(string) / bytesPerSample
        remLength = length * bytesPerSample

        if srcPortType == '_BULKIO__POA.dataChar':
            listData = struct.unpack(str(length) + 'b', string)
        elif srcPortType == '_BULKIO__POA.dataOctet':
            listData = struct.unpack(str(length) + 'B', string)
        elif srcPortType == '_BULKIO__POA.dataShort':
            listData = struct.unpack(str(length) + 'h', string[:remLength])
        elif srcPortType == '_BULKIO__POA.dataUshort':
            listData = struct.unpack(str(length) + 'H', string[:remLength])
        elif srcPortType == '_BULKIO__POA.dataLong':
            listData = struct.unpack(str(length) + 'i', string[:remLength])
        elif srcPortType == '_BULKIO__POA.dataUlong':
            listData = struct.unpack(str(length) + 'I', string[:remLength])
        elif srcPortType == '_BULKIO__POA.dataFloat':
            listData = struct.unpack(str(length) + 'f', string[:remLength])
        elif srcPortType == '_BULKIO__POA.dataLongLong':
            listData = struct.unpack(str(length) + 'q', string[:remLength])
        elif srcPortType == '_BULKIO__POA.dataUlongLong':
            listData = struct.unpack(str(length) + 'Q', string[:remLength])
        elif srcPortType == '_BULKIO__POA.dataDouble':
            listData = struct.unpack(str(length) + 'd', string[:remLength])
        elif srcPortType == '_BULKIO__POA.dataString':          
            pass
        elif srcPortType == '_BULKIO__POA.dataXml':
            pass
        elif srcPortType == '_BULKIO__POA.dataFile':
            pass
        else:
            log.error("Invalid data type")
            listData = None

        return listData

    def _updateTransferLength(self):
        """
        Ensure that the buffer is a multiple
        of the largest possible sample size
        """
        if self.byte_swap > 1:
            self._multSize = self._lcm(8, self.byte_swap)
        else:
            self._multSize = 8

        if self.max_bytes < self._multSize:
            self.max_bytes = self._multSize

        if self.min_bytes < 1:
            self.min_bytes = 1

        self.max_bytes = self.max_bytes - self.max_bytes % self._multSize

        if self.min_bytes > self.max_bytes:
            self.min_bytes = self.max_bytes
        else:
            self.min_bytes = (self.min_bytes + self._multSize - 1) - ((self.min_bytes + self._multSize - 1) % self._multSize)

