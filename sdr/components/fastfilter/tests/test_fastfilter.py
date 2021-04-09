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
import bulkio
import copy

from filter_test_helpers import *

class ComponentTests(ossie.utils.testing.RHTestCase, ImpulseResponseMixIn):
    """Test for all component implementations in fastfilter"""

    SPD_FILE = os.environ.get('SPD_FILE', '../fastfilter.spd.xml')

    def setUp(self):
        """Set up the unit test - this is run before every method that starts with test
        """
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
        self.sampleRate = 1.0e6
        self.output=[]
        self.ts = []
 
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
        sb.stop()
        

    def setupComponent(self):
        #######################################################################
        # Launch the component with the default execparams
        self.comp = sb.launch(self.spd_file,impl = self.impl)

    def testBadCfg1(self):
        """Set with multiple filterProp settings simultaniously and verify we get an error
        """
        prop1 =  self.makeFilterProps()
        prop2 = self.makeCxCoefProps()
        try:
            self.comp.configure([prop1,prop2])
        except CF.PropertySet.InvalidConfiguration:
            return
        raise RunTimeError("No error raised in testBadCfg1")

    def testBadCfg2(self):
        """Set with multiple filterProp settings simultaniously and verify we get an error
        """
        prop1 =  self.makeFilterProps()
        prop2 = self.makeRealCoefProps()
        try:
            self.comp.configure([prop1,prop2])
        except CF.PropertySet.InvalidConfiguration:
            return
        raise RunTimeError("No error raised in testBadCfg1") 

    def testBadCfg3(self):
        """Set with multiple filterProp settings simultaniously and verify we get an error
        """
        prop1 =  self.makeCxCoefProps()
        prop2 = self.makeRealCoefProps()
        try:
            self.comp.configure([prop1,prop2])
        except CF.PropertySet.InvalidConfiguration:
            return
        raise RunTimeError("No error raised in testBadCfg1")

    def testDefaultConfiguration(self):
        """ Test that default configuration is a working allpass filter
        """
        dataPoints = 1024
        data = range(dataPoints)
        
        self.src.push(data,complexData=False, EOS=False,streamID="someSRI")
        count = 0
        while True:
            newData, newTSts = self.sink.getData(tstamps=True)
            if newData:
                count = 0
                self.output.extend(newData)
            elif count==200:
                break
            time.sleep(.01)
            count+=1
     
        # Very vague but there's padding in the beginning
        # Just want to verify it doesn't fail miserably
        self.assertTrue(len(self.output) > dataPoints/2)
 
    def testEOS(self):
        """ Test EOS
        """
        dataPoints = 1024
        data = range(dataPoints)
        
        self.src.push(data,complexData=False,sampleRate=self.sampleRate, EOS=False,streamID="someSRI")
        count = 0
        while True:
            newData = self.sink.getData()
            if newData:
                count = 0
                self.output.extend(newData)
            elif count==200:
                break
            time.sleep(.01)
            count+=1

        self.assertFalse(self.sink.eos())
        self.src.push([],complexData=False, sampleRate=self.sampleRate, EOS=True,streamID="someSRI")
        time.sleep(.1)
        self.assertTrue(self.sink.eos())

    def testReal(self):
        """ Real Filter real data
        """
        self.runSinusoideTest()

    def testRealSmallPackets(self):
        """ Runs SinusoideTest with Small Packets of real data
        """
        self.runSinusoideTest(samplesPerPush=100)

    def testRealBigPackets(self):
        """ Runs SinusioideTest with big packets of real data 
        """
        self.runSinusoideTest(samplesPerPush = 4000, numSamples = 12000)

    def testCxFilt(self):
        """Runs SinusoideTest with complex data and a filter
        """
        self.runSinusoideTest(realData = False, realFilter = False)

    def testCxSmallPackets(self):
        """ Runs SinusoideTest with Small packets of complex data
        """
        self.runSinusoideTest(realData = False, realFilter = False, samplesPerPush = 100)

    def testCxBigPackets(self):
        """ Runs SinusoideTest with Big packets of complex data
        """
        self.runSinusoideTest(realData = False, realFilter = False, samplesPerPush = 4000, numSamples = 12000)

    def testCxRealFilt(self):
        """Runs SinusoideTest with real filter complex data
        """
        self.runSinusoideTest(realData = False)

    def testRealCxFilt(self):
        """Runs SinusoideTest with complex filter real data
        """
        self.runSinusoideTest(realData = False)

    def testNoFilterReal(self):
        """Runs SinusoideTest without a filter and real data
        """
        self.runSinusoideTest(filter=[1])

    def testNoFilterCx(self):
        """Runs SinusoideTest without a filter and complex data
        """
        self.runSinusoideTest(realData = False, realFilter = False, filter = [1])

    def runSinusoideTest(self, realData = True, realFilter = True, samplesPerPush = None, numSamples = 2052, filter = None):
        """ Consolidate logic from other tests into one method
            Create data within the passband of the filter so the steady state output from the filter
            Should be identical to the input
        """
        if filter is None:
                filter = getSink(.2,513)
        self.comp.fftsize = 1024

        if realFilter:
                self.comp.realFilterCoefficients = filter
        else:
                self.comp.complexFilterCoefficients = filter
        dataA = getSin(.05, numSamples)
        dataB = getSin(.0123, numSamples, phase0 = .054)

        inDataReal = [x+y for x,y in zip(dataA,dataB)]

        if not realData:
                inDataL = muxZeros(inDataReal)
        else:
                inDataL = inDataReal
        if samplesPerPush:
                if realData:
                        elementsPerPush = samplesPerPush
                else:
                        elementsPerPush = samplesPerPush*2
                inData = []
                startI = 0
                while True:
                        stopI = startI + elementsPerPush
                        thisData = inDataL[startI:stopI]
                        if thisData:
                                inData.append(thisData)
                                startI = stopI
                        else:
                                break
        else:
                inData = [inDataL]
        self.main(inData, dataCx = not realData)
        self.validateSRIPushing()
        self.validateTC(not realData)

        startTC = self.ts[0][1]
        startSeconds = startTC.twsec + startTC.tfsec
        sampleOffset = -startSeconds * self.sampleRate
        sampleOffsetInt = int(round(sampleOffset))
        assert(abs(sampleOffsetInt - sampleOffset) < 1.0e-3 )
        self.assertEqual((len(filter)-1)/2.0, sampleOffsetInt)
        realOutput = realData and realFilter
        self.assertEqual(self.outputCmplx, not realOutput)
        if realOutput:
                output = self.output
        else:
                re,im = demux(self.output)
                output = re
                self.assertTrue(all([abs(x)<0.1 for x in im]))
        outDataSS = output[int(sampleOffsetInt):]
        self.assertTrue(all([abs(x-y)<.1 for x,y in zip(outDataSS, inDataReal)]))

    def testRealManualImpulse(self):
        """use manual configuration (real taps) and ensure that the impulse response matches the response
        """
        filter = [random.random() for _ in xrange(513)]
        self.comp.fftSize = 1024
        self.comp.realFilterCoefficients = filter
        self.doImpulseResponse(1e6,filter)    

    def testCxManualImpulse(self):
        """use manual configuration (complex taps) and ensure that the impulse response matches the response
        """
        filter = [complex(random.random(), random.random()) for _ in xrange(513)]
        self.comp.fftSize = 1024
        self.comp.complexFilterCoefficients = filter
        self.doImpulseResponse(1e6,filter) 

    def testRealCorrelation(self):
        """Put the filter into correlation mode and ensure that it correlates
        """
        filter = [random.random() for _ in xrange(513)]
        self.comp.correlationMode=True
        self.comp.fftSize = 1024
        self.comp.realFilterCoefficients = filter
        data = [random.random() for _ in range(int(self.comp.fftSize)/2)]
        outExpected = scipyCorl(filter,data)
        data.extend([0]*(self.comp.fftSize))

        self.main([data],False)
        self.validateSRIPushing()
        self.cmpList(outExpected,self.output[:len(outExpected)])


    def testCxCorrelation(self):
        """Put the filter into correlation mode and ensure that it correlates with cx data and coeficients
        """
        filter = [complex(random.random(),random.random()) for _ in xrange(513)]
        self.comp.correlationMode=True
        self.comp.fftSize = 1024
        self.comp.complexFilterCoefficients = filter
        data = [random.random() for _ in range(int(self.comp.fftSize))]
        dataCx = toCx(data)
        outExpected =  scipyCorl(filter,dataCx)
        data.extend([0]*(int(2*self.comp.fftSize)))
        self.main([data],True)
        self.validateSRIPushing()
        self.cmpList(outExpected,self.output[:len(outExpected)])         

    def testCxRealCorrelation(self):
        """real filter complex data for correlation
        """
        filter = [random.random() for _ in xrange(513)]
        self.comp.correlationMode=True
        self.comp.fftSize = 1024
        self.comp.realFilterCoefficients = filter
        data = [random.random() for _ in range(int(self.comp.fftSize))]
        dataCx = toCx(data)
        outExpected =  scipyCorl(filter,dataCx)
        data.extend([0]*(self.comp.fftSize))
        self.main([data],True)
        self.validateSRIPushing()
        self.cmpList(outExpected,self.output[:len(outExpected)])

    def testRealCxCorrelationWithReconnecting(self):
        """complex filter real data
        """
        filter = [complex(random.random(),random.random()) for _ in xrange(513)]
        self.comp.correlationMode=True
        self.comp.fftSize = 1024
        self.comp.complexFilterCoefficients = filter
        
        data = [random.random() for _ in range(int(self.comp.fftSize)/2)]
        outExpected = scipyCorl(filter,data)
        data.extend([0]*(self.comp.fftSize))
        self.comp.disconnect(self.sink)
        self.comp.connect(self.sink)
        self.main([data],False)
        self.validateSRIPushing()
        self.cmpList(outExpected,self.output[:len(outExpected)])
    
    def makeCxCoefProps(self):
        return ossie.cf.CF.DataType(id='complexFilterCoefficients', value=CORBA.Any(CORBA.TypeCode("IDL:CF/complexFloatSeq:1.0"), []))

    def makeRealCoefProps(self):
        return ossie.cf.CF.DataType(id='realFilterCoefficients', value=CORBA.Any(CORBA.TypeCode("IDL:omg.org/CORBA/FloatSeq:1.0"), []))
   
    def validateSRIPushing(self, sampleRate=None, streamID='test_stream'):
        if sampleRate is None:
                sampleRate = self.sampleRate
        self.assertEqual(self.sink.sri().streamID, streamID, "Component not pushing streamID properly")
        # Account for rounding error
        calcSR = 1/self.sink.sri().xdelta
        diffSR = abs(calcSR-sampleRate)
        tolerance = 1
        self.assertTrue(diffSR < tolerance, "Component not pushing samplerate properly")

        #Activate bypassmode and check if output is the same as input
    def testBypassMode(self):
        ff=sb.launch('../fastfilter.spd.xml')
        sg=sb.launch('../../SigGen/SigGen.spd.xml')
        
        sg.shape='square'
        ff.bypassMode = True
        ffOut=sb.DataSink()
        sgOut=sb.DataSink()

        ff.connect(ffOut)
        sg.connect(sgOut,usesPortName='dataFloat_out')
        sg.connect(ff,usesPortName='dataFloat_out')

        sb.start()
        ff_data=ffOut.getData(1000)
        sg_data=sgOut.getData(1000)
        ut=unittest.TestCase('run')

        ut.assertEqual(ff_data,sg_data,'Data was not equal')
        ffOut.reset()
        sgOut.reset()
        time.sleep(1)
        sb.stop()

        #Turn off bypassmode and check if filtering occurred
    def testDeactivateBypassMode(self):
        ff=sb.launch('../fastfilter.spd.xml')
        sg=sb.launch('../../SigGen/SigGen.spd.xml')

        sg.shape='square'
        ff.bypassMode = False
        ffOut=sb.DataSink()
        sgOut=sb.DataSink()

        ff.connect(ffOut)
        sg.connect(sgOut,usesPortName='dataFloat_out')
        sg.connect(ff,usesPortName='dataFloat_out')

        sb.start()
        ff_data=ffOut.getData(1000)
        sg_data=sgOut.getData(1000)
        ut=unittest.TestCase('run')
        ut.assertNotEqual(ff_data,sg_data,'Data was equal')
        ffOut.reset()
        sgOut.reset()
        time.sleep(1)
        sb.stop()
    
    def validateTC(self, isComplex = False):
        """Make sure there are no gaps in time codes for a given sample rate
        """
        last = self.ts[0]
        xdelta = 1.0/self.sampleRate
        for ts in self.ts[1:]:
                tDelta = ts[1] - last[1]
                numSamps = ts[0] - last[0]
                if isComplex:
                        numSamps/=2
                expectedTDelta = numSamps*xdelta
                self.assertTrue(abs(expectedTDelta - tDelta) < .5*xdelta)

    def main(self, inData, dataCx=False, sampleRate=None, eos=False,streamID='test_stream'):    
        if sampleRate is None:
                sampleRate = self.sampleRate
        ts = bulkio.timestamp.create(0,0)
        count=0
        lastPktIndex = len(inData)-1
        for i, data in enumerate(inData):
            #just to mix things up I'm going to push through in two stages
            #to ensure the filter is working properly with its state
            EOS = eos and i == lastPktIndex
            self.src.push(data,complexData=dataCx, sampleRate=sampleRate, EOS=EOS,streamID=streamID, ts = ts)
            ts = copy.copy(ts)
            ts += len(data)/(1 + dataCx) / self.sampleRate
        dataLen = None
        while True:
            thisDataLen = len(self.sink._sink.data)
            newData = thisDataLen != dataLen
            dataLen = thisDataLen
            if newData:
                count = 0
            elif count==200:
                break
            time.sleep(.01)
            count+=1
        self.output, self.ts = self.sink.getData(tstamps = True)

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
