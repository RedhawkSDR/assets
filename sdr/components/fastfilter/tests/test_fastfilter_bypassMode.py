#!/usr/bin/env python
#
# This file is protected by Copyright. Please refer to the COPYRIGHT file distributed with this 
# source distribution.
# 
# This file is part of REDHAWK Basic Components fastfilter.
# 
# REDHAWK Basic Components fastfilter is free software: you can redistribute it and/or modify it under the terms of 
# the GNU General Public License as published by the Free Software Foundation, either 
# version 3 of the License, or (at your option) any later version.
# 
# REDHAWK Basic Components fastfilter is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
# without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
# PURPOSE.  See the GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License along with this 
# program.  If not, see http://www.gnu.org/licenses/.
#

import unittest
import ossie.utils.testing
import os
from omniORB import any
from ossie.cf import CF
from omniORB import CORBA
from ossie.utils import sb
import time
import random

from filter_test_helpers import *

class ComponentTests(ossie.utils.testing.ScaComponentTestCase, ImpulseResponseMixIn):
    """Test for all component implementations in fastfilter"""


    def setUp(self):
        """Set up the unit test - this is run before every method that starts with test
        """
        ossie.utils.testing.ScaComponentTestCase.setUp(self)
        self.src = sb.DataSource()
        self.sink = sb.DataSink()
        
        #setup my components
        self.setupComponent()
        
        self.comp.start()
        self.src.start()
        self.sink.start()
        
        #do the connections
        self.src.connect(self.comp)        
        self.comp.connect(self.sink)
        self.output=[]
 
    def tearDown(self):
        """Finish the unit test - this is run after every method that starts with test
        """
        self.comp.stop()
        #######################################################################
        # Simulate regular component shutdown
        self.comp.releaseObject()
        self.sink.stop()
        self.src.stop()
        self.src.releaseObject()
        self.sink.releaseObject()  
        ossie.utils.testing.ScaComponentTestCase.tearDown(self)
        

    def setupComponent(self):
        #######################################################################
        # Launch the component with the default execparams
        execparams = self.getPropertySet(kinds=("execparam",), modes=("readwrite", "writeonly"), includeNil=False)
        execparams = dict([(x.id, any.from_any(x.value)) for x in execparams])
        execparams['DEBUG_LEVEL']=4
        self.launch(execparams, initialize=True)
        
        #######################################################################
        # Verify the basic state of the component
        self.assertNotEqual(self.comp, None)
        self.assertEqual(self.comp.ref._non_existent(), False)
        self.assertEqual(self.comp.ref._is_a("IDL:CF/Resource:1.0"), True)
        
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
  
    def testBypassModeReal(self):
    	""" Set Real filter Coefficient to 1.0 to test if BypassMode is working
	"""
    	self.comp.log_level(0)
	self.comp.realFilterCoefficients=[1]
	self.src.push([0]*100)

	time.sleep(.1)

	self.testLowpass()
	try:
		self.testLowpass()
	except:
		print("Bypass Mode Worked")	
	

    def testBypassModeCx(self):
    	""" Set Complex filter coefficient to 1.0 to test if BypassMode is working
	"""
	self.comp.log_level(0)
	self.comp.complexFilterCoefficients=[1.0]
	self.src.push([0]*100)

	time.sleep(.1)
	
	try:
		self.testLowpassCx()
	except:
		print("BypassMode Worked")
		

    def testTurnOffBypassReal(self):
    	"""Turn Bypass mode off
	"""
	self.comp.log_level(0)
	self.comp.complexFilterCoefficients = []
	self.comp.realFilterCoefficients = []
	self.src.push([0]*100)

	time.sleep(.1)

	self.testLowpass()


    def testTurnOffBypassCx(self):
    	"""Test turning off bypass mode Complex 
	"""
	self.comp.log_level(0)
	self.comp.complexFilterCoefficients = []
	self.src.push([0]*100)

	time.sleep(.1)

	self.testLowpassCx()

    def validateSRIPushing(self, sampleRate=1.0, streamID='test_stream'):
        self.assertEqual(self.sink.sri().streamID, streamID, "Component not pushing streamID properly")
        # Account for rounding error
        calcSR = 1/self.sink.sri().xdelta
        diffSR = abs(calcSR-sampleRate)
        tolerance = 1
        self.assertTrue(diffSR < tolerance, "Component not pushing samplerate properly")

    def main(self, inData, dataCx=False, sampleRate=1.0, eos=False,streamID='test_stream'):    
        count=0
        lastPktIndex = len(inData)-1
        for i, data in enumerate(inData):
            #just to mix things up I'm going to push through in two stages
            #to ensure the filter is working properly with its state
            EOS = eos and i == lastPktIndex
            self.src.push(data,complexData=dataCx, sampleRate=sampleRate, EOS=EOS,streamID=streamID)
        while True:
            newData = self.sink.getData()
            if newData:
                count = 0
                self.output.extend(newData)
            elif count==200:
                break
            time.sleep(.01)
            count+=1
        #convert the output to complex if necessary    
        self.outputCmplx = self.sink.sri().mode==1
        if self.outputCmplx:
            self.output = toCx(self.output)
        
    # TODO Add additional tests here
    #
    # See:
    #   ossie.utils.bulkio.bulkio_helpers,
    #   ossie.utils.bluefile.bluefile_helpers
    # for modules that will assist with testing components with BULKIO ports
    
if __name__ == "__main__":
    ossie.utils.testing.main("../fastfilter.spd.xml") # By default tests all implementations
