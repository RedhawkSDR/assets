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

DEBUG_LEVEL = 3 # 1=ERROR, 2=WARN, 3=INFO, 4=DEBUG, 5=TRACE

import unittest
import ossie.utils.testing
import os
from omniORB import any
from ossie.utils import sb

import struct
import time
import traceback

from NetworkSource import NetworkSource

STRING_MAP = {'octet':'B',
              'char':'b',
              'short':'h',
              'ushort':'H',
              'long':'i',
              'ulong':'I',
              'float':'f',
              'double':'d'}

def toStr(data,dataType):
    """
    Pack data in to a string
    """
    return struct.pack("%s%s"%(len(data), STRING_MAP[dataType]),*data)

def flip(dataStr,numBytes):
    """
    Given data packed into a string, reverse bytes for a given 
    word length and returned the byte-flipped string
    """
    out = ""

    for i in xrange(len(dataStr)/numBytes):
        l = list(dataStr[numBytes*i:numBytes*(i+1)])
        l.reverse()
        out += (''.join(l))

    return out              

class ComponentTests(ossie.utils.testing.ScaComponentTestCase):
    """
    Test for all component implementations in sinksocket
    """
    PORT = 8645
    OCTET_DATA = [range(256)*100]
    CHAR_DATA = [range(-128,128)*100]
    U_SHORT_DATA = [range(i*16384,(i+1)*16384) for i in xrange(4)]
    SHORT_DATA = [range(i*16384,(i+1)*16384) for i in xrange(-2,2)]
    U_LONG_DATA = [range(i*2**30,(i+1)*2**30, 500000) for i in xrange(0,4)]
    LONG_DATA = [range(i*2**30,(i+1)*2**30, 500000) for i in xrange(-2,2)]
    FLOAT_DATA = DOUBLE_DATA = [[float(x) for x in range(i*4096,(i+1)*4096)] for i in xrange(16)]
    
    def testScaBasicBehavior(self):
        #######################################################################
        # Validate that query returns all expected parameters
        # Query of '[]' should return the following set of properties
        expectedProps = []
        expectedProps.extend(self.getPropertySet(kinds=("configure", "execparam"), modes=("readwrite", "readonly"), includeNil=True))
        expectedProps.extend(self.getPropertySet(kinds=("allocate",), action="external", includeNil=True))
        props = self.comp.query([])
        props = dict((x.id, any.from_any(x.value)) for x in props)
        # Query may return more than expected, but not less
        for expectedProp in expectedProps:
            self.assertEquals(props.has_key(expectedProp.id), True)
        
        #######################################################################
        # Verify that all expected ports are available
        for port in self.scd.get_componentfeatures().get_ports().get_uses():
            port_obj = self.comp.getPort(str(port.get_usesname()))
            self.assertNotEqual(port_obj, None)
            self.assertEqual(port_obj._non_existent(), False)
            self.assertEqual(port_obj._is_a("IDL:CF/Port:1.0"),  True)
            
        for port in self.scd.get_componentfeatures().get_ports().get_provides():
            port_obj = self.comp.getPort(str(port.get_providesname()))
            self.assertNotEqual(port_obj, None)
            self.assertEqual(port_obj._non_existent(), False)
            self.assertEqual(port_obj._is_a(port.get_repid()),  True)
            
        #######################################################################
        # Make sure start and stop can be called without throwing exceptions
        self.comp.start()
        self.comp.stop()

    #test a bunch of stuff - vary the start order for client.  Vary which one is the client.  Vary the data ports as well
    def testA(self):
        self.runTest(clientFirst=True, client = 'sinksocket', dataPackets=self.OCTET_DATA,portType='octet')
    def testB(self):
        self.runTest(clientFirst=False, client = 'sinksocket', dataPackets=self.OCTET_DATA,portType='octet')
    def testC(self):
        self.runTest(clientFirst=True, client = 'sourcesocket', dataPackets=self.OCTET_DATA,portType='octet')
    def testD(self):
        self.runTest(clientFirst=False, client = 'sourcesocket', dataPackets=self.OCTET_DATA,portType='octet')

    def testAChar(self):
        self.runTest(clientFirst=True, client = 'sinksocket', dataPackets=self.CHAR_DATA,portType='char')
    def testBChar(self):
        self.runTest(clientFirst=False, client = 'sinksocket', dataPackets=self.CHAR_DATA,portType='char')
    def testCChar(self):
        self.runTest(clientFirst=True, client = 'sourcesocket', dataPackets=self.CHAR_DATA,portType='char')
    def testDChar(self):
        self.runTest(clientFirst=False, client = 'sourcesocket', dataPackets=self.CHAR_DATA,portType='char')

    def testAUShort(self):
        self.runTest(clientFirst=True, client = 'sinksocket', dataPackets=self.U_SHORT_DATA,portType='ushort')
    def testBUShort(self):
        self.runTest(clientFirst=False, client = 'sinksocket', dataPackets=self.U_SHORT_DATA,portType='ushort')
    def testCUShort(self):
        self.runTest(clientFirst=True, client = 'sourcesocket', dataPackets=self.U_SHORT_DATA,portType='ushort')
    def testDUShort(self):
        self.runTest(clientFirst=False, client = 'sourcesocket', dataPackets=self.U_SHORT_DATA,portType='ushort')

    def testAShort(self):
        self.runTest(clientFirst=True, client = 'sinksocket', dataPackets=self.SHORT_DATA,portType='short')
    def testBShort(self):
        self.runTest(clientFirst=False, client = 'sinksocket', dataPackets=self.SHORT_DATA,portType='short')
    def testCShort(self):
        self.runTest(clientFirst=True, client = 'sourcesocket', dataPackets=self.SHORT_DATA,portType='short')
    def testDShort(self):
        self.runTest(clientFirst=False, client = 'sourcesocket', dataPackets=self.SHORT_DATA,portType='short')

    def testAULong(self):
        self.runTest(clientFirst=True, client = 'sinksocket', dataPackets=self.U_LONG_DATA,portType='ulong')
    def testBULong(self):
        self.runTest(clientFirst=False, client = 'sinksocket', dataPackets=self.U_LONG_DATA,portType='ulong')
    def testCULong(self):
        self.runTest(clientFirst=True, client = 'sourcesocket', dataPackets=self.U_LONG_DATA,portType='ulong')
    def testDULong(self):
        self.runTest(clientFirst=False, client = 'sourcesocket', dataPackets=self.U_LONG_DATA,portType='ulong')

    def testALong(self):
        self.runTest(clientFirst=True, client = 'sinksocket', dataPackets=self.LONG_DATA,portType='long')
    def testBLong(self):
        self.runTest(clientFirst=False, client = 'sinksocket', dataPackets=self.LONG_DATA,portType='long')
    def testCLong(self):
        self.runTest(clientFirst=True, client = 'sourcesocket', dataPackets=self.LONG_DATA,portType='long')
    def testDLong(self):
        self.runTest(clientFirst=False, client = 'sourcesocket', dataPackets=self.LONG_DATA,portType='long')

    def testAFloat(self):
        self.runTest(clientFirst=True, client = 'sinksocket', dataPackets=self.FLOAT_DATA,portType='float')
    def testBFloat(self):
        self.runTest(clientFirst=False, client = 'sinksocket', dataPackets=self.FLOAT_DATA,portType='float')
    def testCFloat(self):
        self.runTest(clientFirst=True, client = 'sourcesocket', dataPackets=self.FLOAT_DATA,portType='float')
    def testDFloat(self):
        self.runTest(clientFirst=False, client = 'sourcesocket', dataPackets=self.FLOAT_DATA,portType='float')

    def testADouble(self):
        self.runTest(clientFirst=True, client = 'sinksocket', dataPackets=self.DOUBLE_DATA,portType='double')
    def testBDouble(self):
        self.runTest(clientFirst=False, client = 'sinksocket', dataPackets=self.DOUBLE_DATA,portType='double')
    def testCDouble(self):
        self.runTest(clientFirst=True, client = 'sourcesocket', dataPackets=self.DOUBLE_DATA,portType='double')
    def testDDouble(self):
        self.runTest(clientFirst=False, client = 'sourcesocket', dataPackets=self.DOUBLE_DATA,portType='double')

    #test a bunch of stuff.  Vary which one is the client.  Vary the data ports as well
    def testAMultiConnection(self):
        self.runMultiConnectionTest(client = 'sinksocket', dataPackets=self.OCTET_DATA,portType='octet')
    
    def testBMultiConnection(self):
        self.runMultiConnectionTest(client = 'sourcesocket', dataPackets=self.OCTET_DATA,portType='octet')

    def testAMultiConnectionChar(self):
        self.runMultiConnectionTest(client = 'sinksocket', dataPackets=self.CHAR_DATA,portType='char')
    def testBMultiConnectionChar(self):
        self.runMultiConnectionTest(client = 'sourcesocket', dataPackets=self.CHAR_DATA,portType='char')

    def testAMultiConnectionUShort(self):
        self.runMultiConnectionTest(client = 'sinksocket', dataPackets=self.U_SHORT_DATA,portType='ushort')
    def testBMultiConnectionUShort(self):
        self.runMultiConnectionTest(client = 'sourcesocket', dataPackets=self.U_SHORT_DATA,portType='ushort')

    def testAMultiConnectionShort(self):
        self.runMultiConnectionTest(client = 'sinksocket', dataPackets=self.SHORT_DATA,portType='short')
    def testBMultiConnectionShort(self):
        self.runMultiConnectionTest(client = 'sourcesocket', dataPackets=self.SHORT_DATA,portType='short')

    def testAMultiConnectionULong(self):
        self.runMultiConnectionTest(client = 'sinksocket', dataPackets=self.U_LONG_DATA,portType='ulong')
    def testBMultiConnectionULong(self):
        self.runMultiConnectionTest(client = 'sourcesocket', dataPackets=self.U_LONG_DATA,portType='ulong')

    def testAMultiConnectionLong(self):
        self.runMultiConnectionTest(client = 'sinksocket', dataPackets=self.LONG_DATA,portType='long')
    def testBMultiConnectionLong(self):
        self.runMultiConnectionTest(client = 'sourcesocket', dataPackets=self.LONG_DATA,portType='long')

    def testAMultiConnectionFloat(self):
        self.runMultiConnectionTest(client = 'sinksocket', dataPackets=self.FLOAT_DATA,portType='float')
    def testBMultiConnectionFloat(self):
        self.runMultiConnectionTest(client = 'sourcesocket', dataPackets=self.FLOAT_DATA,portType='float')

    def testAMultiConnectionDouble(self):
        self.runMultiConnectionTest(client = 'sinksocket', dataPackets=self.DOUBLE_DATA,portType='double')
    def testBMultiConnectionDouble(self):
        self.runMultiConnectionTest(client = 'sourcesocket', dataPackets=self.DOUBLE_DATA,portType='double')

    #Test sending a bunch of little packets
    def testLITTLE_PACKETS(self):
        print "testLITTLE_PACKETS"
        self.runTest(clientFirst=True, client = 'sinksocket', dataPackets=[range(200) for _ in xrange(50000)], maxBytes=256*256, minBytes=0)

    def testLITTLE_PACKETS_2(self):
        print "testLITTLE_PACKETS_2"
        self.runTest(clientFirst=True, client = 'sourcesocket', dataPackets=[range(200) for _ in xrange(50000)], maxBytes=256*256, minBytes=0)     

    #Test sending two BIG PACKETS
    def testBIG_PACKETS(self):
        print "testBIG_PACKETS"
        self.runTest(clientFirst=True, client = 'sinksocket', dataPackets=[range(200)*25000 for _ in xrange(2)])
    def testBIG_PACKETS_2(self):
        print "testBIG_PACKETS_2"
        self.runTest(clientFirst=True, client = 'sourcesocket', dataPackets=[range(200)*25000 for _ in xrange(2)])

    #A bunch of tests for byte swapping
    #start with octet port and using various number of bytes for swapping
    #flip the bits and show they are equal for the output
    def testByteSwap1(self):
        TYPE= 'octet'
        SWAP = 2
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s = toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f = flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap2(self):
        TYPE= 'octet'
        SWAP = 2
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap3(self):
        TYPE= 'octet'
        SWAP = 4
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap4(self):
        TYPE= 'octet'
        SWAP = 4
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap5(self):
        TYPE= 'octet'
        SWAP = 8
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap6(self):
        TYPE= 'octet'
        SWAP=8
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)


    def testByteSwap7(self):
        TYPE= 'octet'
        SWAP=10
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap8(self):
        TYPE= 'octet'
        SWAP = 10
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)

    #here are some tests where we swap both ends -
    #verify the output is equal
    def testByteSwap9(self):
        TYPE= 'octet'
        SWAP = 10
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=SWAP, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        self.assertEqual(s[:len(so)],so)

    def testByteSwap10(self):
        TYPE= 'octet'
        SWAP = 8
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=SWAP, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        self.assertEqual(s[:len(so)],so)

    def testByteSwap11(self):
        TYPE= 'octet'
        SWAP = 4
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=SWAP, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        self.assertEqual(s[:len(so)],so)
        
    def testByteSwap12(self):
        TYPE= 'octet'
        SWAP = 2
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=SWAP, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        self.assertEqual(s[:len(so)],so)
        
    #here are some tests where we use the swapping defined as the default for each port by settign SWAP to 1
    #2 bytes for short & ushort
    def testByteSwap13(self):
        TYPE= 'short'
        SWAP = 1
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,2)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap14(self):
        TYPE= 'short'
        SWAP = 1
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,2)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap15(self):
        TYPE= 'ushort'
        SWAP = 1
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,2)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap16(self):
        TYPE= 'ushort'
        SWAP = 1
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,2)
        self.assertEqual(s[:len(f)],f)
    
    #4 bytes for long & ulong, & float
    def testByteSwap17(self):
        TYPE= 'long'
        SWAP = 1
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,4)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap18(self):
        TYPE= 'long'
        SWAP = 1
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,4)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap19(self):
        TYPE= 'ulong'
        SWAP = 1
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,4)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap20(self):
        TYPE= 'ulong'
        SWAP = 1
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,4)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap21(self):
        TYPE= 'float'
        SWAP = 1
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,4)
        self.assertEqual(s[:len(f)],f)
    
    def testByteSwap22(self):
        TYPE= 'float'
        SWAP = 1
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,4)
        self.assertEqual(s[:len(f)],f)
    
    #8 bytes for double    
    def testByteSwap23(self):
        TYPE= 'double'
        SWAP = 1
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,8)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap24(self):
        TYPE= 'double'
        SWAP = 1
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,100)*10], byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,8)
        self.assertEqual(s[:len(f)],f)
    
    #Do a test with all the different type of ports using a non-standard byte swapping
    def testByteSwap25(self):
        TYPE= 'short'
        SWAP = 5
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,99)]*9, byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)
    
    def testByteSwap26(self):
        TYPE= 'short'
        SWAP = 5
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,99)]*9, byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)
    
    def testByteSwap27(self):
        TYPE= 'ushort'
        SWAP = 5
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,99)]*9, byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap28(self):
        TYPE= 'ushort'
        SWAP = 5
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,99)]*9, byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)
    
    def testByteSwap29(self):
        TYPE= 'long'
        SWAP = 5
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,99)]*9, byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap30(self):
        TYPE= 'long'
        SWAP = 5
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,99)]*9, byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap31(self):
        TYPE= 'ulong'
        SWAP = 5
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,99)]*9, byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap32(self):
        TYPE= 'ulong'
        SWAP = 5
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,99)]*9, byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap33(self):
        TYPE= 'float'
        SWAP = 5
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,99)]*9, byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap34(self):
        TYPE= 'float'
        SWAP = 5
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,99)]*9, byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)
    
     
    def testByteSwap35(self):
        TYPE= 'double'
        SWAP = 5
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,99)]*9, byteSwapSrc=None, byteSwapSink=SWAP,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)

    def testByteSwap36(self):
        TYPE= 'double'
        SWAP = 5
        self.runTest(client = 'sourcesocket', dataPackets=[range(0,99)]*9, byteSwapSrc=SWAP, byteSwapSink=None,minBytes=1,portType=TYPE)
        s= toStr(self.input,TYPE)
        so = toStr(self.output,TYPE)
        f= flip(so,SWAP)
        self.assertEqual(s[:len(f)],f)
    
    def runTest(self, clientFirst=True, client = 'sinksocket',dataPackets=[],maxBytes=None,minBytes=None, portType='octet',byteSwapSrc=None, byteSwapSink=None):
        self.startTest(client, portType)
        
        if maxBytes!=None:
            self.sourceSocket.setMax_bytes(maxBytes)
        if minBytes!=None:
            self.sourceSocket.setMin_bytes(minBytes)
        
        if clientFirst:
            self.configureClient(byteSwapSrc, byteSwapSink)
            self.configureServer(byteSwapSrc, byteSwapSink)
        else:
            self.configureServer(byteSwapSrc, byteSwapSink)
            self.configureClient(byteSwapSrc, byteSwapSink)
        
        self.src.start()
        self.sink.start()
        self.sourceSocket.start()
        self.sinkSocket.start()
        
        self.input = []
        self.output = []

        time.sleep(.1)

        for packet in dataPackets:
            self.input.extend(packet)
            self.src.push(packet, False, "test stream", 1.0)

            newdata = self.sink.getData()

            if newdata:
                if portType == 'octet':
                    self.output.extend([ord(x) for x in newdata])
                else:
                    self.output.extend(newdata)

        time.sleep(.25)

        noData = 0

        while True:
            newdata = self.sink.getData()

            if newdata:
                noData=0

                if portType == 'octet':
                    self.output.extend([ord(x) for x in newdata])
                else:
                    self.output.extend(newdata)
            else:
                noData += 1

                if noData == 200:
                    break

                time.sleep(.01)

        print "self.sourceSocket.bytes_per_sec", self.sourceSocket.bytes_per_sec
        print "self.sinkSocket.bytes_per_sec", self.sinkSocket.bytes_per_sec
##        
        print "self.sinkSocket.total_bytes", self.sinkSocket.total_bytes
        print "self.sourceSocket.total_bytes",  self.sourceSocket.total_bytes
##        
        print "len(self.input)", len(self.input), "len(self.output)", len(self.output)
        
        self.assertTrue(len(self.output)> 0)
        self.assertTrue(len(self.input)-len(self.output)< self.sourceSocket.max_bytes)
        self.assertTrue(len(self.input)>=len(self.output))

        if byteSwapSrc == byteSwapSink:
            self.assertEquals(self.input[:len(self.output)],self.output)

    def runMultiConnectionTest(self, client = 'sinksocket',dataPackets=[],maxBytes=None,minBytes=None, portType='octet',byteSwapSrc=None, byteSwapSink=None):
        self.startTests(client, portType)
        
        if maxBytes!=None:
            self.sourceSocket.setMax_bytes(maxBytes)
            self.sourceSocket2.setMax_bytes(maxBytes)
        if minBytes!=None:
            self.sourceSocket.setMin_bytes(minBytes)
            self.sourceSocket2.setMin_bytes(minBytes)
        
        self.configureClients(byteSwapSrc, byteSwapSink)
        self.configureServers(byteSwapSrc, byteSwapSink)
        
        self.src.start()
        self.sink.start()
        self.sink2.start()
        self.sourceSocket.start()
        self.sourceSocket2.start()
        self.sinkSocket.start()
        
        self.input = []
        self.output1 = []
        self.output2 = []

        time.sleep(.1)

        for packet in dataPackets:
            self.input.extend(packet)
            self.src.push(packet, False, "test stream", 1.0)

            newdata1 = self.sink.getData()

            if newdata1:
                if portType == 'octet':
                    self.output1.extend([ord(x) for x in newdata1])
                else:
                    self.output1.extend(newdata1)

            newdata2 = self.sink2.getData()

            if newdata2:
                if portType == 'octet':
                    self.output2.extent([ord(x) for x in newdata2])
                else:
                    self.output2.extend(newdata2)

        time.sleep(.25)

        noData = 0

        while True:
            newdata1 = self.sink.getData()
            newdata2 = self.sink2.getData()

            if not newdata1 and not newdata2:
                noData += 1

                if noData == 200:
                    break

                time.sleep(.01)
            else:
                if newdata1:
                    noData=0

                    if portType == 'octet':
                        self.output1.extend([ord(x) for x in newdata1])
                    else:
                        self.output1.extend(newdata1)

                if newdata2:
                    noData=0

                    if portType == 'octet':
                        self.output2.extend([ord(x) for x in newdata2])
                    else:
                        self.output2.extend(newdata2)

        print "self.sourceSocket.bytes_per_sec", self.sourceSocket.bytes_per_sec
        print "self.sourceSocket2.bytes_per_sec", self.sourceSocket2.bytes_per_sec
        print "self.sinkSocket.bytes_per_sec", self.sinkSocket.bytes_per_sec
##        
        print "self.sinkSocket.total_bytes", self.sinkSocket.total_bytes
        print "self.source1Socket.total_bytes",  self.sourceSocket.total_bytes
        print "self.source1Socket2.total_bytes",  self.sourceSocket2.total_bytes
##        
        print "len(self.input)", len(self.input), "len(self.output1)", len(self.output1), "len(self.output2)", len(self.output2)
        
        self.assertTrue(len(self.output1)> 0)
        self.assertTrue(len(self.output2)> 0)
        self.assertTrue(len(self.input)-len(self.output1)< self.sourceSocket.max_bytes)
        self.assertTrue(len(self.input)-len(self.output2)< self.sourceSocket2.max_bytes)
        self.assertTrue(len(self.input)>=len(self.output1))
        self.assertTrue(len(self.input)>=len(self.output2))

        if byteSwapSrc == byteSwapSink:
            self.assertEquals(self.input[:len(self.output1)],self.output1)
            self.assertEquals(self.input[:len(self.output2)],self.output2)
    
    def configureClient(self, byteSwapSrc, byteSwapSink):
        if self.client == self.sinkSocket:
            if byteSwapSink != None:
                self.client.Connections = [{'connection_type' : 'client', 'ip_address' : '127.0.0.1', 'ports' : [self.PORT], 'byte_swap' : [byteSwapSink]}]
                self.assertTrue(self.client.Connections[0].byte_swap[0] == byteSwapSink)
            else:
                self.client.Connections = [{'connection_type' : 'client', 'ip_address' : '127.0.0.1', 'ports' : [self.PORT], 'byte_swap' : [0]}]

            self.assertTrue(self.client.Connections[0].connection_type == 'client')
            self.assertTrue(self.client.Connections[0].ports[0] == self.PORT)
        else:
            self.client.setConnection_type('client')
            self.client.setIp_address("127.0.0.1")
            self.client.setPort(self.PORT)
            self.assertTrue(self.client.connection_type == 'client')
            self.assertTrue(self.client.port == self.PORT)

            if byteSwapSrc != None:
                self.client.setByte_swap(byteSwapSrc)
                self.assertTrue(self.client.byte_swap == byteSwapSrc)

    def configureClients(self, byteSwapSrc, byteSwapSink):
        if self.clients[0] == self.sinkSocket:
            if byteSwapSink != None:
                self.clients[0].Connections = [{'connection_type' : 'client', 'ip_address' : '127.0.0.1', 'ports' : [self.PORT, self.PORT+1], 'byte_swap' : [byteSwapSink, byteSwapSink]}]
                self.assertTrue(self.clients[0].Connections[0].byte_swap[0] == byteSwapSink)
                self.assertTrue(self.clients[0].Connections[0].byte_swap[1] == byteSwapSink)
            else:
                self.clients[0].Connections = [{'connection_type' : 'client', 'ip_address' : '127.0.0.1', 'ports' : [self.PORT, self.PORT+1], 'byte_swap' : [0, 0]}]

            self.assertTrue(self.clients[0].Connections[0].connection_type == 'client')
            self.assertTrue(self.clients[0].Connections[0].ports[0] == self.PORT)
            self.assertTrue(self.clients[0].Connections[0].ports[1] == self.PORT+1)
        else:
            self.clients[0].setConnection_type('client')
            self.clients[0].setIp_address("127.0.0.1")
            self.clients[0].setPort(self.PORT)
            self.assertTrue(self.clients[0].connection_type == 'client')
            self.assertTrue(self.clients[0].port == self.PORT)

            self.clients[1].setConnection_type('client')
            self.clients[1].setIp_address("127.0.0.1")
            self.clients[1].setPort(self.PORT+1)
            self.assertTrue(self.clients[1].connection_type == 'client')
            self.assertTrue(self.clients[1].port == self.PORT+1)

            if byteSwapSrc != None:
                self.clients[0].setByte_swap(byteSwapSrc)
                self.assertTrue(self.clients[0].byte_swap == byteSwapSrc)
                self.clients[1].setByte_swap(byteSwapSrc)
                self.assertTrue(self.clients[1].byte_swap == byteSwapSrc)
        
    def configureServer(self, byteSwapSrc, byteSwapSink):
        if self.server == self.sinkSocket:
            if byteSwapSink != None:
                self.server.Connections = [{'connection_type' : 'server', 'ports' : [self.PORT], 'byte_swap' : [byteSwapSink]}]
                self.assertTrue(self.server.Connections[0].byte_swap[0] == byteSwapSink)
            else:
                self.server.Connections = [{'connection_type' : 'server', 'ports' : [self.PORT], 'byte_swap' : [0]}]

            self.assertTrue(self.server.Connections[0].connection_type == 'server')
            self.assertTrue(self.server.Connections[0].ports[0] == self.PORT)
        else:
            self.server.setConnection_type('server')
            self.server.setPort(self.PORT)
            self.assertTrue(self.server.connection_type=='server')
            self.assertTrue(self.server.port==self.PORT)

            if byteSwapSrc != None:
                self.server.setByte_swap(byteSwapSrc)
                self.assertTrue(self.server.byte_swap == byteSwapSrc)

    def configureServers(self, byteSwapSrc, byteSwapSink):
        if self.servers[0] == self.sinkSocket:
            if byteSwapSink != None:
                self.servers[0].Connections = [{'connection_type' : 'server', 'ports' : [self.PORT, self.PORT+1], 'byte_swap' : [byteSwapSink, byteSwapSink]}]
                self.assertTrue(self.servers[0].Connections[0].byte_swap[0] == byteSwapSink)
                self.assertTrue(self.servers[0].Connections[0].byte_swap[1] == byteSwapSink)
            else:
                self.servers[0].Connections = [{'connection_type' : 'server', 'ports' : [self.PORT, self.PORT+1], 'byte_swap' : [0, 0]}]

            self.assertTrue(self.servers[0].Connections[0].connection_type == 'server')
            self.assertTrue(self.servers[0].Connections[0].ports[0] == self.PORT)
            self.assertTrue(self.servers[0].Connections[0].ports[1] == self.PORT+1)
        else:
            self.servers[0].setConnection_type('server')
            self.servers[0].setPort(self.PORT)
            self.assertTrue(self.servers[0].connection_type=='server')
            self.assertTrue(self.servers[0].port==self.PORT)

            self.servers[1].setConnection_type('server')
            self.servers[1].setPort(self.PORT+1)
            self.assertTrue(self.servers[1].connection_type=='server')
            self.assertTrue(self.servers[1].port==self.PORT+1)

            if byteSwapSrc != None:
                self.servers[0].setByte_swap(byteSwapSrc)
                self.assertTrue(self.servers[0].byte_swap == byteSwapSrc)
                self.servers[1].setByte_swap(byteSwapSrc)
                self.assertTrue(self.servers[1].byte_swap == byteSwapSrc)

    def setUp(self):
        ossie.utils.testing.ScaComponentTestCase.setUp(self)

        #######################################################################
        # Launch the component with the default execparams
        execparams = self.getPropertySet(kinds=("execparam",), modes=("readwrite", "writeonly"), includeNil=False)
        execparams = dict([(x.id, any.from_any(x.value)) for x in execparams])
        execparams['DEBUG_LEVEL'] = DEBUG_LEVEL
        self.launch(execparams)
        
        #######################################################################
        # Verify the basic state of the component
        self.assertNotEqual(self.comp, None)
        self.assertEqual(self.comp.ref._non_existent(), False)
        self.assertEqual(self.comp.ref._is_a("IDL:CF/Resource:1.0"), True)

        # Load resources for majority of tests
        self.src = sb.DataSource()
        self.sink = sb.DataSink()
        self.sinkSocket = self.comp
        self.sourceSocket = NetworkSource()

        self.client = None
        self.clients = None
        self.server = None
        self.servers = None
        self.sink2 = None
        self.sourceSocket2 = None

    def startTest(self, client='sinksocket', portType='octet'):
        if client == 'sinksocket':
            self.client = self.sinkSocket
            self.server = self.sourceSocket
        else:
            self.server = self.sinkSocket
            self.client = self.sourceSocket
        
        sinkSocketName = 'data%s_in'%portType.capitalize()
        self.src.connect(self.sinkSocket, sinkSocketName)
        self.sourceSocket.connect(self.sink, None, '%sOut'%portType)

    def startTests(self, client='sinksocket', portType='octet'):
        self.sink2 = sb.DataSink()
        self.sourceSocket.setPort(self.PORT)
        self.sourceSocket2 = NetworkSource()
        self.sourceSocket2.setPort(self.PORT+1)
        
        if client == 'sinksocket':
            self.clients = [self.sinkSocket]
            self.servers = [self.sourceSocket, self.sourceSocket2]
        else:
            self.servers = [self.sinkSocket]
            self.clients = [self.sourceSocket, self.sourceSocket2]
        
        sinkSocketName = 'data%s_in'%portType.capitalize()
        self.src.connect(self.sinkSocket, sinkSocketName)
        self.sourceSocket.connect(self.sink, None, '%sOut'%portType)
        self.sourceSocket2.connect(self.sink2, None, '%sOut'%portType)

    def tearDown(self):
        ossie.utils.testing.ScaComponentTestCase.tearDown(self)

        if self.src != None:
            self.src.stop()
            self.src.releaseObject()
            self.src = None

        if self.sink != None:
            self.sink.stop()
            self.sink.releaseObject()
            self.sink = None

        if self.sink2 != None:
            self.sink2.stop()
            self.sink2.releaseObject()
            self.sink2 = None

        if self.sourceSocket != None:
            self.sourceSocket.stop()
            self.sourceSocket.releaseObject()
            self.sourceSocket = None

        if self.sourceSocket2 != None:
            self.sourceSocket2.stop()
            self.sourceSocket2.releaseObject()
            self.sourceSocket2 = None

        if self.sinkSocket != None:
            self.sinkSocket.stop()
            self.sinkSocket.releaseObject()
            self.sinkSocket = None

        self.client = None
        self.clients = None
        self.server = None
        self.servers = None
    
if __name__ == "__main__":
    ossie.utils.testing.main("../sinksocket.spd.xml") # By default tests all implementations
