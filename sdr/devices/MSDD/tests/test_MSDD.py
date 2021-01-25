#!/usr/bin/env python
#
# This file is protected by Copyright. Please refer to the COPYRIGHT file
# distributed with this source distribution.
#
# This file is part of REDHAWK rh.MSDD.
#
# REDHAWK rh.MSDD is free software: you can redistribute it and/or modify it under
# the terms of the GNU Lesser General Public License as published by the Free
# Software Foundation, either version 3 of the License, or (at your option) any
# later version.
#
# REDHAWK rh.MSDD is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
# details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.
#

import subprocess
import sys
import os
import ossie.utils.testing
from ossie.utils import sb, uuid
from ossie.cf import CF
from omniORB import any
from omniORB import CORBA
from ossie.utils.bulkio.bulkio_data_helpers import SDDSSink
from redhawk.frontendInterfaces import FRONTEND
from ossie.utils import uuid
from ossie import properties
import time
import re
import frontend
import traceback
from tod_assist import *

DEBUG_LEVEL = 3
IP_ADDRESS='192.168.11.2'
INTERFACE='em3'
TOD_MODE="ONEPPS"

def get_debug_level( debug ):
    _rlookup = { 'FATAL' :  0,
                 'ERROR' : 1,
                 'WARN' : 2,
                 'INFO' : 3,
                 'DEBUG' : 4,
                 'TRACE' : 5 }
    return _rlookup[debug.upper()]


def _generateRFInfoPkt(rf_freq=1e9,rf_bw=1e9,if_freq=0,spec_inverted=False,rf_flow_id="testflowID"):
        antenna_info = frontend.FRONTEND.AntennaInfo("antenna_name","antenna_type","antenna.size","description")
        freqRange= frontend.FRONTEND.FreqRange(0,1e12,[] )
        feed_info = frontend.FRONTEND.FeedInfo("feed_name", "polarization",freqRange)
        sensor_info = frontend.FRONTEND.SensorInfo("msn_name", "collector_name", "receiver_name",antenna_info,feed_info)
        delays = [];
        cap = frontend.FRONTEND.RFCapabilities(freqRange,freqRange);
        add_props = [];
        rf_info_pkt = frontend.FRONTEND.RFInfoPkt(rf_flow_id,rf_freq, rf_bw, if_freq, spec_inverted, sensor_info, delays, cap, add_props)

        return rf_info_pkt

def getAllocationParams( ip_address ):
        msdd_id=None
        try:
                from id_msdd import id_msdd
                msdd_id = id_msdd(ip_address)
        except:
                raise Exception("Unable to contact ip: " + ip_address)

	# wbddc and nbddc settings for fgpa loads
        if "vr1a" in msdd_id:
            alloc_params = { 'wbddc_tuner' : 0,
                             'wbddc_bw' : 20e6,
                             'wbddc_srate' : 25e6,
                             'wbddc_enable' : False,
                             'nbddc_tuner' : 1,
                             'nbddc_bw' : 2.5e6,
                             'nbddc_srate' : 3.125e6,
            }
        elif "32n512b40" in msdd_id:
            alloc_params = { 'wbddc_tuner' : 0,
                             'wbddc_bw' : 20e6,
                             'wbddc_srate' : 24.576e6,
                             'wbddc_enable' : False,
                             'nbddc_tuner' : 1,
                             'nbddc_bw' : 42e3,
                             'nbddc_srate' : 48e3,
            }
        elif "5n5b1300" in msdd_id:
            alloc_params = { 'wbddc_tuner' : 0,
                             'wbddc_bw' : 20e6,
                             'wbddc_srate' : 24.576e6,
                             'wbddc_enable' : False,
                             'nbddc_tuner' : 1,
                             'nbddc_bw' : 1.5e6,
                             'nbddc_srate' : 4.9125e6,
            }
        elif "1w0n0b0" in msdd_id:
            alloc_params = { 'wbddc_tuner' : 0,
                             'wbddc_bw' : 20e6,
                             'wbddc_srate' : 24.576e6,
                             'wbddc_enable' : True,
                             'nbddc_tuner' : None,
                             'nbddc_bw' : None,
                             'nbddc_srate' : None,
            }
        elif "2w8n64b320" in msdd_id:
            alloc_params = { 'wbddc_tuner' : 0,
                             'wbddc_bw' : 20e6,
                             'wbddc_srate' : 25e6,
                             'wbddc_enable' : True,
                             'nbddc_tuner' : 2,
                             'nbddc_bw' : 320e3,
                             'nbddc_srate' : 390.625e3,
            }
        else:
                raise Exception("Unknown FPGA load MSDD id: " + msdd_id)

        return alloc_params, msdd_id



class DeviceTests(ossie.utils.testing.RHTestCase):
    # Path to the SPD file, relative to this file. This must be set in order to
    # launch the device.
    SPD_FILE = '../MSDD.spd.xml'

    # setUp is run before every function preceded by "test" is executed
    # tearDown is run after every function preceded by "test" is executed
    
    # self.comp is a device using the sandbox API
    # to create a data source, the package sb contains data sources like DataSource or FileSource
    # to create a data sink, there are sinks like DataSink and FileSink
    # to connect the component to get data from a file, process it, and write the output to a file, use the following syntax:
    #  src = sb.FileSource('myfile.dat')
    #  snk = sb.DataSink()
    #  src.connect(self.comp)
    #  self.comp.connect(snk)
    #  sb.start()
    #
    # components/sources/sinks need to be started. Individual components or elements can be started
    #  src.start()
    #  self.comp.start()
    #
    # every component/elements in the sandbox can be started
    #  sb.start()


    def setUp(self):
        # reset device
        subprocess.call(['./reset_msdd', IP_ADDRESS])

        try:
                self.alloc_params, self.msdd_id = getAllocationParams(IP_ADDRESS)
        except Exception, e:
                self.fail("Unable to identify MSDD, " + str(IP_ADDRESS) + " reason:" + str(e))

        # Launch the device, using the selected implementation
        configure = {
                'DEBUG_LEVEL': DEBUG_LEVEL,
                 "msdd_configuration" : {
                     "msdd_configuration::msdd_ip_address": IP_ADDRESS,
                     "msdd_configuration::msdd_port":"23"
                      }
                ,
                "advanced":{
                        "advanced::udp_timeout" : 0.10,
                        "advanced::enable_fft_channels" : True
                     },
                "msdd_time_of_day_configuration": {
                    "msdd_time_of_day_configuration::mode": TOD_MODE,
                    },
                "msdd_output_configuration": [{
                     "msdd_output_configuration::tuner_number": self.alloc_params['wbddc_tuner'],
                     "msdd_output_configuration::protocol":"UDP_SDDS",
                     "msdd_output_configuration::ip_address":"234.168.103.100",
                     "msdd_output_configuration::port":0,
                     "msdd_output_configuration::vlan":0,
                     "msdd_output_configuration::enabled":True,
                     "msdd_output_configuration::timestamp_offset":0,
                     "msdd_output_configuration::endianess":1,
                     "msdd_output_configuration::mfp_flush":63,
                     "msdd_output_configuration::vlan_enable": False                        
                    }]
                                    
                 }
        
        
        self.comp = sb.launch(self.spd_file, impl=self.impl,properties=configure)

    def tearDown(self):
        # Clean up all sandbox artifacts created during test
        sb.release()

    def testBasicBehavior(self):
        #######################################################################
        # Make sure start and stop can be called without throwing exceptions
        self.comp.start()
        self.comp.stop()

    def testSingleTunerAllocation(self):
        
        self.comp.start()
        
        sink = sb.DataSinkSDDS()
        
               
        alloc = self._generateAlloc(cf=110e6,sr=0,bw=0)
        allocationID = properties.props_to_dict(alloc)['FRONTEND::tuner_allocation']['FRONTEND::tuner_allocation::allocation_id']
        self.comp.connect(sink,usesPortName = "dataSDDS_out",connectionId=allocationID)
        sink.start()
        
        try:
            retval = self.comp.allocateCapacity(alloc)
        except Exception, e:
            print "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
            print "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
            print "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
            print str(e)
            self.assertFalse("Exception thrown on allocateCapactiy %s" % str(e))
        if not retval:
            self.assertFalse("Allocation Failed")
            
        time.sleep(1)
        
        #dataSink1._sink.sri
        #attachments1 = dataSink1._sink.attachments
        sri = sink.sri()
        self.assertEqual(sri.streamID, allocationID)
        #self.assertAlmostEqual(sri.xdelta, 1.0/2.5e6,5)
        #time.sleep(1)
        
        self.comp.deallocateCapacity(alloc)
        
    def testReportedBandwidth(self):
        self.comp.start()

        alloc = self._generateAlloc(cf=110e6,
                                    sr=self.alloc_params['wbddc_srate'],
                                    bw=self.alloc_params['wbddc_bw'])
        allocationID = properties.props_to_dict(alloc)['FRONTEND::tuner_allocation']['FRONTEND::tuner_allocation::allocation_id']

        try:
            retval = self.comp.allocateCapacity(alloc)
        except Exception, e:
            print str(e)
            self.fail("Exception thrown on allocateCapactiy %s" % str(e))

        if not retval:
            self.fail("Allocation Failed")

        tuner_status = self.comp.frontend_tuner_status[0]

        bw=tuner_status.bandwidth.queryValue()
        srate=tuner_status.sample_rate.queryValue()
        _avail_bw=tuner_status.available_bandwidth.queryValue()
        if "," in _avail_bw:
                avail_bw = [ float(x) for x in _avail_bw.split(',')]
                self.assertEqual( (bw in avail_bw), True, msg="Checking for correct available bandwidth")
                print srate, avail_bw, (srate in avail_bw)
                self.assertEquals( (srate not in avail_bw), True, msg="Checking sample rate not equal to available bandwidth")
        else:
                avail_bw=float(_avail_bw)
                self.assertAlmostEqual(bw,avail_bw, msg="Checking for correct available bandwidth")
                self.assertNotAlmostEquals(srate, avail_bw, msg="Checking sample rate not equal to available bandwidth")

        self.comp.deallocateCapacity(alloc)


    def _generateAlloc(self,tuner_type='RX_DIGITIZER', cf=100e6,sr=25e6,bw=20e6,rf_flow_id=''):
        
        value = {}
        value['ALLOC_ID'] = str(uuid.uuid4())
        value['TYPE'] = tuner_type
        value['BW_TOLERANCE'] = 100.0
        value['SR_TOLERANCE'] = 100.0
        value['RF_FLOW_ID'] = rf_flow_id
        value['GROUP_ID'] = ''
        value['CONTROL'] = True
        value['CF'] = cf
        value['SR'] = sr
        value['BW'] = bw
        
        #generate the allocation
        allocationPropDict = {'FRONTEND::tuner_allocation':{
                    'FRONTEND::tuner_allocation::tuner_type': value['TYPE'],
                    'FRONTEND::tuner_allocation::allocation_id': value['ALLOC_ID'],
                    'FRONTEND::tuner_allocation::center_frequency': float(value['CF']),
                    'FRONTEND::tuner_allocation::bandwidth': float(value['BW']),
                    'FRONTEND::tuner_allocation::bandwidth_tolerance': float(value['BW_TOLERANCE']),
                    'FRONTEND::tuner_allocation::sample_rate': float(value['SR']),
                    'FRONTEND::tuner_allocation::sample_rate_tolerance': float(value['SR_TOLERANCE']),
                    'FRONTEND::tuner_allocation::device_control': value['CONTROL'],
                    'FRONTEND::tuner_allocation::group_id': value['GROUP_ID'],
                    'FRONTEND::tuner_allocation::rf_flow_id': value['RF_FLOW_ID'],
                    }}
        return properties.props_from_dict(allocationPropDict)

class MsddDeviceTests(ossie.utils.testing.RHTestCase):
    # Path to the SPD file, relative to this file. This must be set in order to
    # launch the device.
    SPD_FILE = '../MSDD.spd.xml'

    def setUp(self):
        # reset device
        subprocess.call(['./reset_msdd', IP_ADDRESS])

        try:
                self.alloc_params, self.msdd_id = getAllocationParams(IP_ADDRESS)
        except Exception, e:
                self.fail("Unable to identify MSDD, " + str(IP_ADDRESS) + " reason:" + str(e))


        self.comp=sb.launch(self.spd_file,
                      properties={
                "DEBUG_LEVEL": DEBUG_LEVEL,
                "msdd_configuration" : { "msdd_configuration::msdd_ip_address" :  IP_ADDRESS },
                "msdd_time_of_day_configuration": {
                    "msdd_time_of_day_configuration::mode": TOD_MODE,
                    },
                "msdd_output_configuration": [
                        {
                             "msdd_output_configuration::tuner_number": self.alloc_params['wbddc_tuner'],
                             "msdd_output_configuration::protocol":"UDP_SDDS",
                             "msdd_output_configuration::ip_address":"234.168.103.100",
                             "msdd_output_configuration::port":0,
                             "msdd_output_configuration::vlan":0,
                             "msdd_output_configuration::enabled": self.alloc_params['wbddc_enable'],
                             "msdd_output_configuration::timestamp_offset":0,
                             "msdd_output_configuration::endianess":1,
                             "msdd_output_configuration::mfp_flush":63,
                             "msdd_output_configuration::vlan_enable": False                        
                             },
                        {
                             "msdd_output_configuration::tuner_number": self.alloc_params['nbddc_tuner'],
                             "msdd_output_configuration::protocol":"UDP_SDDS",
                             "msdd_output_configuration::ip_address":"234.168.103.100",
                             "msdd_output_configuration::port":1,
                             "msdd_output_configuration::vlan":0,
                             "msdd_output_configuration::enabled":True,
                             "msdd_output_configuration::timestamp_offset":0,
                             "msdd_output_configuration::endianess":1,
                             "msdd_output_configuration::mfp_flush":63,
                             "msdd_output_configuration::vlan_enable": False
                             }]
                }

                      )


    def tearDown(self):
        # Clean up all sandbox artifacts created during test
        sb.release()


    def testWideBandDDC_SampleRateAllocation(self):
       alloc=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                            center_frequency=100e6, 
                                            sample_rate=self.alloc_params['wbddc_srate'],
                                            sample_rate_tolerance=100.0)
       ret=self.comp.allocateCapacity( alloc)
       self.assertTrue(ret)
       self.comp.deallocateCapacity( alloc)

    def testWideBandDDC_BandwidthAllocation(self):
       alloc=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                            center_frequency=100e6, 
                                            bandwidth=self.alloc_params['wbddc_bw'],
                                            bandwidth_tolerance=100.0)
       ret=self.comp.allocateCapacity( alloc)
       self.assertTrue(ret)
       self.comp.deallocateCapacity( alloc)

    def testNarrowBandDDC_BandwidthAllocation(self):

       if self.alloc_params['nbddc_srate'] is None : return

       wb_alloc=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                            center_frequency=100e6)
       ret=self.comp.allocateCapacity( wb_alloc)
       self.assertTrue(ret)

       alloc=frontend.createTunerAllocation(tuner_type="DDC", 
                                            center_frequency=100e6,
                                            bandwidth=self.alloc_params['nbddc_bw'],
                                            bandwidth_tolerance=100.0)
       ret=self.comp.allocateCapacity( alloc)
       self.assertTrue(ret)
       self.comp.deallocateCapacity( alloc)

       self.comp.deallocateCapacity(wb_alloc)

    def testNarrowBandDDC_SampleRateAllocation(self):

       if self.alloc_params['nbddc_srate'] is None : return

       wb_alloc=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                            center_frequency=100e6)
       ret=self.comp.allocateCapacity( wb_alloc)
       self.assertTrue(ret)

       alloc=frontend.createTunerAllocation(tuner_type="DDC", 
                                            center_frequency=100e6,
                                            sample_rate=self.alloc_params['nbddc_srate'],
                                            sample_rate_tolerance=100.0)
       ret=self.comp.allocateCapacity( alloc)
       self.assertTrue(ret)
       self.comp.deallocateCapacity( alloc)

       self.comp.deallocateCapacity(wb_alloc)


    def testNarrowBandDDC_BadAllocation(self):
       if self.alloc_params['nbddc_srate'] is None : return

       wb_alloc=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                            center_frequency=100e6)
       ret=self.comp.allocateCapacity( wb_alloc)
       self.assertTrue(ret)

       alloc=frontend.createTunerAllocation(tuner_type="DDC", 
                                            center_frequency=100e6,
                                            sample_rate=20,
                                            sample_rate_tolerance=100.0)
       ret=self.comp.allocateCapacity( alloc)
       self.assertFalse(ret)

       self.comp.deallocateCapacity(wb_alloc)

    def testNarrowBandDDC_BadAllocation2(self):
       if self.alloc_params['nbddc_srate'] is None : return

       wb_alloc=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                            center_frequency=100e6)
       ret=self.comp.allocateCapacity( wb_alloc)
       self.assertTrue(ret)

       alloc=frontend.createTunerAllocation(tuner_type="DDC", 
                                            center_frequency=100e6,
                                            bandwidth=20,
                                            bandwidth_tolerance=100.0)
       ret=self.comp.allocateCapacity( alloc)
       self.assertFalse(ret)

       self.comp.deallocateCapacity(wb_alloc)



    def testNarrowBandDDC_DontCareAllocation(self):
       if self.alloc_params['nbddc_srate'] is None : return

       wb_alloc=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                            center_frequency=100e6)
       ret=self.comp.allocateCapacity( wb_alloc)
       self.assertTrue(ret)

       alloc=frontend.createTunerAllocation(tuner_type="DDC", 
                                            center_frequency=100e6)
       ret=self.comp.allocateCapacity( alloc)
       self.assertTrue(ret)
       self.comp.deallocateCapacity( alloc)

       self.comp.deallocateCapacity(wb_alloc)


class RFInfoTest(ossie.utils.testing.RHTestCase):
    # Path to the SPD file, relative to this file. This must be set in order to
    # launch the device.
    SPD_FILE = '../MSDD.spd.xml'

    def setUp(self):
        # reset device
        subprocess.call(['./reset_msdd', IP_ADDRESS])

        try:
                self.alloc_params, self.msdd_id = getAllocationParams(IP_ADDRESS)
        except Exception, e:
                self.fail("Unable to identify MSDD, " + str(IP_ADDRESS) + " reason:" + str(e))

        self.alloc1=None
        self.alloc2=None
        self.comp=sb.launch(self.spd_file,
                            properties={ 
                "DEBUG_LEVEL": DEBUG_LEVEL, 
                "msdd_configuration" : { "msdd_configuration::msdd_ip_address" : IP_ADDRESS },
                "msdd_output_configuration": [{
                     "msdd_output_configuration::tuner_number": self.alloc_params['wbddc_tuner'],
                     "msdd_output_configuration::protocol":"UDP_SDDS",
                     "msdd_output_configuration::ip_address":"239.1.1.1",
                     "msdd_output_configuration::port":29000,
                     "msdd_output_configuration::vlan":0,
                     "msdd_output_configuration::enabled": self.alloc_params['wbddc_enable'],
                     "msdd_output_configuration::timestamp_offset":0,
                     "msdd_output_configuration::endianess":1,
                     "msdd_output_configuration::mfp_flush":63,
                     "msdd_output_configuration::vlan_enable": False                        
                     },
                   {
                     "msdd_output_configuration::tuner_number": self.alloc_params['nbddc_tuner'],
                     "msdd_output_configuration::protocol":"UDP_SDDS",
                     "msdd_output_configuration::ip_address":"239.1.1.2",
                     "msdd_output_configuration::port":29001,
                     "msdd_output_configuration::vlan":0,
                     "msdd_output_configuration::enabled":True,
                     "msdd_output_configuration::timestamp_offset":0,
                     "msdd_output_configuration::endianess":1,
                     "msdd_output_configuration::mfp_flush":63,
                     "msdd_output_configuration::vlan_enable": False                        
                     }
                                              ]
                }
              )
    
    def tearDown(self):
        try:
           if self.alloc1:
              self.comp.deallocateCapacity(alloc1)
	except:
		pass
        try:
           if self.alloc2:
              self.comp.deallocateCapacity(alloc2)
	except:
		pass
        # Clean up all sandbox artifacts created during test
        sb.release()

    def testRFFlowID(self): 
        
        #create rf_info uses port and connect to MSDD
        out_rf_info_port=frontend.OutRFInfoPort("out_control")
        in_rf_info_port=self.comp.getPort("RFInfo_in")
        out_rf_info_port.connectPort(in_rf_info_port,"test_rf_flow")

	# get params for allocations
	bw=self.alloc_params['wbddc_bw']
	sr=self.alloc_params['wbddc_srate']

        # allocation params
        flow_id = "ca-710-flow"
        cf=100e6

        # set rf flow id
        out_rf_info_port._set_rf_flow_id(flow_id)

        # check rf_flow_id was set
        n=len(self.comp.frontend_tuner_status)
        expected=[flow_id]*n
        actual=[ x["FRONTEND::tuner_status::rf_flow_id"] for x in self.comp.frontend_tuner_status ]
        self.assertEqual(expected,actual, "Mismatch of RF Flow Ids for tuners")

        # allocation for sample rate and rf_flow_id
        alloc1=frontend.createTunerAllocation(center_frequency=cf, sample_rate=sr, rf_flow_id=flow_id)
        ret=self.comp.allocateCapacity( alloc1)
        alloc1_aid =  alloc1["FRONTEND::tuner_allocation"]["FRONTEND::tuner_allocation::allocation_id"]
        self.assertEqual(True,ret, "Allocation failed using rf_flow_id")

        alloc2_aid = alloc1_aid
        if self.alloc_params['nbddc_srate'] is not None :
            # dual channel we need to provide specific rate so the correct DDC is selected that matches the same rf_flow_id
            if '2w' in self.msdd_id:
                # allocation for center freq and rf_flow_id
               alloc2=frontend.createTunerAllocation(center_frequency=cf,  
                                                     sample_rate=self.alloc_params['nbddc_srate'],
                                                     sample_rate_tolerance=1.0,
                                                     rf_flow_id=flow_id)
            else:
               # allocation for center freq and rf_flow_id
               alloc2=frontend.createTunerAllocation(center_frequency=cf,  rf_flow_id=flow_id)
            ret=self.comp.allocateCapacity( alloc2)
            alloc2_aid =  alloc2["FRONTEND::tuner_allocation"]["FRONTEND::tuner_allocation::allocation_id"]
            self.assertEqual(True,ret, "Allocation failed using rf_flow_id again ")

        # valid rf_flow_id was probagated downstream
        sink=sb.StreamSink()
        sdds_in=sb.launch('rh.SourceSDDS',  properties={'interface': INTERFACE,
                                                        'advanced_optimizations': {
                                                         'advanced_optimizations::sdds_pkts_per_bulkio_push' : 5
                                                        },
                                                        'attachment_override' : {
                                                         'attachment_override:endianness': "1234"
                                                         }

                                                })
        sb.start()
        self.comp.connect(sdds_in, connectionId=alloc2_aid, usesPortName='dataSDDS_out')
        sdds_in.connect(sink, usesPortName="dataShortOut")

        kws=None
        try:
            sink_data=sink.read()
            kws=properties.props_to_dict(sink_data.sri.keywords)
        except:
            pass
        self.assertEqual( kws["FRONTEND::RF_FLOW_ID"] , flow_id, "Missing RF_FLOW_ID from keyword list")


    def testRFFlowIDFailure(self): 
        
        #create rf_info uses port and connect to MSDD
        out_rf_info_port=frontend.OutRFInfoPort("out_control")
        in_rf_info_port=self.comp.getPort("RFInfo_in")
        out_rf_info_port.connectPort(in_rf_info_port,"test_rf_flow")

	# get params for allocations
	bw=self.alloc_params['wbddc_bw']
	sr=self.alloc_params['wbddc_srate']

        # allocation params
        flow_id = "ca-710-flow"
        cf=100e6

        # set rf flow id
        out_rf_info_port._set_rf_flow_id(flow_id)

        # check rf_flow_id was set
        n=len(self.comp.frontend_tuner_status)
        expected=[flow_id]*n
        actual=[ x["FRONTEND::tuner_status::rf_flow_id"] for x in self.comp.frontend_tuner_status ]
        self.assertEqual(expected,actual, "Mismatch of RF Flow Ids for tuners")

        # allocation for sample rate and rf_flow_id
        alloc1=frontend.createTunerAllocation(center_frequency=cf, sample_rate=sr, rf_flow_id=flow_id)
        ret=self.comp.allocateCapacity( alloc1)
        alloc1_aid =  alloc1["FRONTEND::tuner_allocation"]["FRONTEND::tuner_allocation::allocation_id"]
        self.assertEqual(True,ret, "Allocation failed using rf_flow_id")

        if len(self.comp.frontend_tuner_status) > 1:
                # allocation for center freq and rf_flow_id
                alloc2=frontend.createTunerAllocation(center_frequency=cf,  rf_flow_id="noworkie")
                ret=self.comp.allocateCapacity( alloc2)
                self.assertEqual(False,ret, "Allocation should have failed for unknown rf_flow_id ")

    def testRFInfoPkt(self):

        #create rf_info uses port and connect to MSDD
        out_rf_info_port=frontend.OutRFInfoPort("out_control")
        in_rf_info_port=self.comp.getPort("RFInfo_in")
        out_rf_info_port.connectPort(in_rf_info_port,"test_rf_flow")

	bw=self.alloc_params['wbddc_bw']
	sr=self.alloc_params['wbddc_srate']
        # allocation params
        flow_id = "ca-710-flow-2"
        cf=100e6

        # send info pkto
        pkt=_generateRFInfoPkt(cf,bw,rf_flow_id=flow_id)
        out_rf_info_port._set_rfinfo_pkt(pkt)

        # check rf_flow_id was set
        n=len(self.comp.frontend_tuner_status)
        expected=[flow_id]*n
        actual=[ x["FRONTEND::tuner_status::rf_flow_id"] for x in self.comp.frontend_tuner_status ]
        self.assertEqual(expected,actual, "Mismatch of RF Flow Ids for tuners")

        # allocation for sample rate and rf_flow_id
        alloc1=frontend.createTunerAllocation(center_frequency=cf, sample_rate=sr, rf_flow_id=flow_id)
        ret=self.comp.allocateCapacity( alloc1)
        alloc1_aid =  alloc1["FRONTEND::tuner_allocation"]["FRONTEND::tuner_allocation::allocation_id"]
        self.assertEqual(True,ret, "Allocation failed using rf_flow_id")

        alloc2_aid = alloc1_aid
        if self.alloc_params['nbddc_srate'] is not None :
            # dual channel we need to provide specific rate so the correct DDC is selected that matches the same rf_flow_id
            if '2w' in self.msdd_id:
                # allocation for center freq and rf_flow_id
               alloc2=frontend.createTunerAllocation(center_frequency=cf,  
                                                     sample_rate=self.alloc_params['nbddc_srate'],
                                                     sample_rate_tolerance=1.0,
                                                     rf_flow_id=flow_id)
            else:
                # allocation for center freq and rf_flow_id
                alloc2=frontend.createTunerAllocation(center_frequency=cf,  rf_flow_id=flow_id)
            ret=self.comp.allocateCapacity( alloc2)
            alloc2_aid =  alloc2["FRONTEND::tuner_allocation"]["FRONTEND::tuner_allocation::allocation_id"]
            self.assertEqual(True,ret, "Allocation failed using rf_flow_id again ")

        # valid rf_flow_id was probagated downstream
        sink=sb.StreamSink()
        sdds_in=sb.launch('rh.SourceSDDS',  properties={'interface':INTERFACE})
        sb.start()
        self.comp.connect(sdds_in, connectionId=alloc2_aid, usesPortName='dataSDDS_out')
        sdds_in.connect(sink, usesPortName="dataShortOut")

        kws=None
        try:
            sink_data=sink.read()
            kws=properties.props_to_dict(sink_data.sri.keywords)
        except:
            pass
        self.assertEqual( kws["FRONTEND::RF_FLOW_ID"] , flow_id, "Missing RF_FLOW_ID from keyword list")

class OnePpsTests(ossie.utils.testing.RHTestCase):
    # Path to the SPD file, relative to this file. This must be set in order to launch the device.
    SPD_FILE = '../MSDD.spd.xml'

    def setUp(self):
        # reset device
        subprocess.call(['./reset_msdd', IP_ADDRESS ])

        self.temp_files = []

        properties = {
                "msdd_configuration" : {
                     "msdd_configuration::msdd_ip_address": IP_ADDRESS,
                     "msdd_configuration::msdd_port":"23"
                      },
                "msdd_time_of_day_configuration": {
                    "msdd_time_of_day_configuration::mode": TOD_MODE,
                    },
                "msdd_output_configuration": [{
                     "msdd_output_configuration::tuner_number":0,
                     "msdd_output_configuration::protocol":"UDP_SDDS",
                     "msdd_output_configuration::ip_address":"234.168.103.100",
                     "msdd_output_configuration::port":0,
                     "msdd_output_configuration::vlan":0,
                     "msdd_output_configuration::enabled":True,
                     "msdd_output_configuration::timestamp_offset":0,
                     "msdd_output_configuration::endianess":1,
                     "msdd_output_configuration::mfp_flush":63,
                     "msdd_output_configuration::vlan_enable": False                        
                    }],
                'LOGGING_CONFIG_URI': 'file://' + os.getcwd() + '/OnePpsTests.log.cfg',
                 }
        self.comp = sb.launch(self.spd_file, impl=self.impl, properties=properties)
    
    def tearDown(self):
        for fname in self.temp_files:
            try:
                os.remove(fname)
            except:
                pass
        sb.release()

    def _testSetTimeOfDayManual(self):
        """
        This is intended to run under 2 scenarios.
        scenario 1:  disconnect 1PPS cable, then run the test.
        scenario 2:  while the test is running, disconnect the 1PPS cable.
        To run the test, either rename it without the leading `_`, or list it on the command line.
        This test does not need to run every time the test suite is run, but would
        be useful, for example, with new hardware.
        """
        fname_log = 'OnePpsTests.log'
        self.temp_files.append(fname_log)

        self.comp.start()

        msdd_time_of_day_configuration = self.comp.msdd_time_of_day_configuration.queryValue()
        mode = msdd_time_of_day_configuration['msdd_time_of_day_configuration::mode']
        self.assertEqual(mode, 'ONEPPS', msg='tod_module.mode must be ONEPPS for this test.')

        # for scenario 2, unplug 1PPS cable here.
        raw_input('press ENTER to continue: ')

        log_contents = ''
        try:
            log_contents = open(fname_log).read()
        except:
            pass
        self.assertTrue(len(log_contents) > 0, msg='log file for test was not written')
        msg = 'did not see log WARNING about missed 1PPS pulse'
        condition = 'WARNING' in log_contents and 'Missed 1PPS Pulse' in log_contents
        self.assertTrue(condition, msg=msg)

        self.comp.stop()

    def testSetTimeOfDay(self):
        fname_log = 'OnePpsTests.log'
        self.temp_files.append(fname_log)
        self.comp.start()
        tod_host_delta_orig = self.comp.msdd_status.tod_host_delta.queryValue()

        msdd_time_of_day_configuration = self.comp.msdd_time_of_day_configuration.queryValue()
        mode = msdd_time_of_day_configuration['msdd_time_of_day_configuration::mode']
        self.assertEqual(mode, 'ONEPPS', msg='tod_module.mode must be ONEPPS for this test.')

        deltas = []
        num_reads = 30
        for _ in xrange(num_reads):
            host_time = get_seconds_from_start_of_year()
            tod_time = get_tod(IP_ADDRESS)
            if not tod_time: continue
            deltas.append(host_time - tod_time)
            time.sleep(1)
        delta_avg = sum(deltas) / len(deltas)

        ntp_accuracy = self._get_ntp_accuracy()
        self.assertTrue(ntp_accuracy >= 0, msg='failed to get a number for ntp accuracy')

        msg = 'abs(host_time - tod_time) exceeds NTP estimated accuracy'
        self.assertTrue(abs(delta_avg) < ntp_accuracy, msg=msg)

        # Check that msdd_status.tod_host_delta was written.
        tod_host_delta = self.comp.msdd_status.tod_host_delta.queryValue()
        msg = 'msdd_status.tod_host_delta not written over {0} seconds'.format(num_reads)
        self.assertFalse(tod_host_delta == tod_host_delta_orig, msg=msg)

        # Check that msdd_status.tod_host_delta contains a reasonable value.
        msg = 'msdd_status::tod_host_delta value is larger than expected'
        self.assertTrue(tod_host_delta < delta_avg + 0.5 and
                        tod_host_delta > delta_avg - 0.5, msg=msg)

        log_contents, has_warning = self._check_log(fname_log)
        self.assertTrue(len(log_contents) > 0, msg='log file for test was not written')
        # Check for time synchronization errors, at ERROR or WARNING.
        msg = 'Time synchronization error(s) occurred.'
        self.assertFalse(has_warning, msg=msg)

        self.comp.stop()

    def _check_log(self, fname_log):
        log_contents = ''
        try:
            log_contents = open(fname_log).read()
        except:
            pass
        has_warning = False
        for line in log_contents.split('\n'):
            if 'WARNING' in line and 'time' in line.lower():
                has_warning = True
            if 'ERROR' in line and 'time' in line.lower():
                has_warning = True
        return log_contents, has_warning

    def _run_ntpstat(self):
        stdout = None
        returncode = None
        proc = subprocess.Popen(['ntpstat'], stdout=subprocess.PIPE)
        stdout = proc.communicate()[0]
        returncode = proc.wait()
        return stdout, returncode

    def _parse_ntpstat(self, stdout):
        """Return stratum as int or None; accuracy as string or None.
        ref:  https://github.com/darkhelmet/ntpstat/blob/master/ntpstat.c """
        stratum = None
        accuracy = None
        for line in stdout.split('\n'):
            words = line.split()
            if line.startswith('synchronised to NTP server') and words[-2] == 'stratum':
                try:
                    stratum = int(words[-1])
                except:
                    pass
            if 'time correct to within' in line and words[-1] == 'ms':
                try:
                    accuracy = float(words[-2])
                except:
                    pass
        return stratum, accuracy

    def _get_ntp_accuracy(self):
        stdout, returncode = self._run_ntpstat()
        if returncode in [1, 2]:
            return None
        stratum, accuracy = self._parse_ntpstat(stdout)
        return accuracy


class IPPInterfaceTest(ossie.utils.testing.RHTestCase):
    # Path to the SPD file, relative to this file. This must be set in order to
    # launch the device.
    SPD_FILE = '../MSDD.spd.xml'

    def setUp(self):

        # reset device
        subprocess.call(['./reset_msdd', IP_ADDRESS])

        try:
            self.alloc_params, self.msdd_id = getAllocationParams(IP_ADDRESS)
        except Exception, e:
                self.fail("Unable to identify MSDD, " + str(IP_ADDRESS) + " reason:" + str(e))

	import sys
	sys.path.append('../python')
	import msddcontroller
	msdd=msddcontroller.MSDDRadio(IP_ADDRESS,23)
        omsg="Does not report"
        dual=False
        try:
            dual=re.split('[_-]',msdd.console.filename_fpga)[1].startswith('d')
        except:
            traceback.print_exc()
            pass
	if '170314' in msdd.console.filename_app or dual:
                omsg= "reports "
        msg="*** Application firmware: " + \
                msdd.console.filename_app + \
                " Output Module: "+omsg+ " interface numbers ***"
        print>>sys.stderr, "\n",msg


        self.alloc1=None
        self.alloc2=None
        configure = {
                "DEBUG_LEVEL": DEBUG_LEVEL, 
                "msdd_configuration" : { 
                    "msdd_configuration::msdd_ip_address" : IP_ADDRESS,
                     "msdd_configuration::msdd_port":"23"
                },
                "advanced":{
                        "advanced::udp_timeout" : 0.10,
                     },
                "msdd_output_configuration": [{
                     "msdd_output_configuration::tuner_number": self.alloc_params['wbddc_tuner'],
                     "msdd_output_configuration::protocol":"UDP_SDDS",
                     "msdd_output_configuration::ip_address":"239.1.1.1",
                     "msdd_output_configuration::port":29000,
                     "msdd_output_configuration::vlan":0,
                     "msdd_output_configuration::enabled":True,
                     "msdd_output_configuration::timestamp_offset":0,
                     "msdd_output_configuration::endianess":1,
                     "msdd_output_configuration::mfp_flush":63,
                     "msdd_output_configuration::vlan_enable": False                        
                     },
                                      {
                     "msdd_output_configuration::tuner_number": self.alloc_params['nbddc_tuner'],
                     "msdd_output_configuration::protocol":"UDP_SDDS",
                     "msdd_output_configuration::ip_address":"239.1.1.2",
                     "msdd_output_configuration::port":29001,
                     "msdd_output_configuration::vlan":0,
                     "msdd_output_configuration::enabled":True,
                     "msdd_output_configuration::timestamp_offset":0,
                     "msdd_output_configuration::endianess":1,
                     "msdd_output_configuration::mfp_flush":63,
                     "msdd_output_configuration::vlan_enable": False                        
                     }]
                }
    
        self.comp = sb.launch(self.spd_file, impl=self.impl,properties=configure)

    
    def tearDown(self):
        try:
           if self.alloc1:
              self.comp.deallocateCapacity(alloc1)
	except:
		pass
        try:
           if self.alloc2:
              self.comp.deallocateCapacity(alloc2)
	except:
		pass
        # Clean up all sandbox artifacts created during test
        sb.release()


    def testTunerStatus(self):

       self.alloc1=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                            center_frequency=100e6, 
                                            sample_rate=self.alloc_params['wbddc_srate'],
                                            sample_rate_tolerance=100.0)
       ret=self.comp.allocateCapacity(self.alloc1)
       self.assertTrue(ret)
       alloc1_aid =  self.alloc1["FRONTEND::tuner_allocation"]["FRONTEND::tuner_allocation::allocation_id"]

       expected=True
       actual=self.comp.frontend_tuner_status[0].enabled
       self.assertEqual(expected,actual, "Tuner status enabled failed")

       expected=True
       actual=self.comp.frontend_tuner_status[0].output_enabled
       self.assertEqual(expected,actual, "Tuner status output enabled failed")

       sink=sb.StreamSink()
       sdds_in=sb.launch('rh.SourceSDDS',  properties={'interface': INTERFACE})
       sb.start()
       self.comp.connect(sdds_in, connectionId=alloc1_aid, usesPortName='dataSDDS_out')
       sdds_in.connect(sink, usesPortName="dataShortOut")

       sink_data=None
       try:
               sink_data=sink.read(timeout=1.0)
       except:
            pass
       self.assertNotEqual( None, sink_data, "MSDD did not produce data for allocation")
       self.assertNotEqual( 0, len(sink_data.data), "MSDD did not produce data for allocation")


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--ip', default="192.168.11.2", help="ip address of msdd")
    parser.add_argument('--iface', default="em3", help="local host interface for reading data from msdd")
    parser.add_argument('--tod', default="ONEPPS", help="set TOD mode: ONEPPS, SIM")
    parser.add_argument('--debug', default='info', help="debug level, fatal, error, warn, info, debug, trace" )

    try:
        args, remaining_args = parser.parse_known_args()
        IP_ADDRESS=args.ip
        DEBUG_LEVEL=get_debug_level(args.debug)
        INTERFACE=args.iface
        TOD_MODE=args.tod
    except SystemExit:
        raise SystemExit
    except:
        traceback.print_exc()
        pass

    sys.argv[1:] = remaining_args
    ossie.utils.testing.main() # By default tests all implementations
