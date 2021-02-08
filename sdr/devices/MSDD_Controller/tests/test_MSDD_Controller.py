#!/usr/bin/env python

import ossie.utils.testing
from ossie.utils import sb

import subprocess
import sys
import ossie.utils.testing
from ossie.utils import sb
from ossie.cf import CF
from omniORB import any
from omniORB import CORBA
from ossie.utils.bulkio.bulkio_data_helpers import SDDSSink
from redhawk.frontendInterfaces import FRONTEND
from ossie.utils import uuid
from ossie import properties
import os
import time
import frontend
import traceback
import pprint
import re
from  tod_assist import *

DEBUG_LEVEL = 3
IP_ADDRESS='192.168.11.2'
INTERFACE='em3'
MSDD_INTERFACE=0
ALLOCATE_RCV="RCV:1"
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
            _alloc_params = { 'wbddc_tuner' : 0,
                             'wbddc_bw' : 20e6,
                             'wbddc_srate' : 25e6,
                             'wbddc_enable' : False,
                             'nbddc_tuner' : 1,
                             'nbddc_bw' : 2.5e6,
                             'nbddc_srate' : 3.125e6
            }
            alloc_params = { 'RCV:1' : _alloc_params }
        elif "32n512b40" in msdd_id:
            _alloc_params = { 'wbddc_tuner' : 0,
                             'wbddc_bw' : 20e6,
                             'wbddc_srate' : 24.576e6,
                             'wbddc_enable' : False,
                             'nbddc_tuner' : 1,
                             'nbddc_bw' : 42e3,
                             'nbddc_srate' : 48e3,
            }
            alloc_params = { 'RCV:1' : _alloc_params }
        elif "5n5b1300" in msdd_id:
            _alloc_params = { 'wbddc_tuner' : 0,
                             'wbddc_bw' : 20e6,
                             'wbddc_srate' : 24.576e6,
                             'wbddc_enable' : False,
                             'nbddc_tuner' : 1,
                             'nbddc_bw' : 1.5e6,
                             'nbddc_srate' : 4.9125e6,
            }
            alloc_params = { 'RCV:1' : _alloc_params }
        elif "1w0n0b0" in msdd_id:
            _alloc_params = { 'wbddc_tuner' : 0,
                             'wbddc_bw' : 20e6,
                             'wbddc_srate' : 24.576e6,
                             'wbddc_enable' : True,
                             'nbddc_tuner' : None,
                             'nbddc_bw' : None,
                             'nbddc_srate' : None,
            }
            alloc_params = { 'RCV:1' : _alloc_params }
        elif "2w8n64b320" in msdd_id:
            _alloc_params = { 'wbddc_tuner' : 0,
                             'wbddc_bw' : 20e6,
                             'wbddc_srate' : 25e6,
                             'wbddc_enable' : True,
                             'nbddc_tuner' : 1,
                             'nbddc_bw' : 320e3,
                             'nbddc_srate' : 390.625e3,
            }
            _alloc_params_2 = { 'wbddc_tuner' : 0,
                             'wbddc_bw' : 20e6,
                             'wbddc_srate' : 25e6,
                             'wbddc_enable' : True,
                             'nbddc_tuner' : None,
                             'nbddc_bw' : None,
                             'nbddc_srate' : None,
            }

            alloc_params = { 'RCV:1' : _alloc_params, 
                             'RCV:2' : _alloc_params_2 }
        else:
                raise Exception("Unknown FPGA load MSDD id: " + msdd_id)

        return alloc_params



def createSandboxDevice(dev, spdPath="../MSDD", impl='python'):
    from ossie.utils.sandbox.model import SandboxDevice
    _sb=sb.domainless._getSandbox()
    _profile=_sb.getSdrRoot().findProfile( spdPath+'/'+dev.softwareProfile)
    spd, scd, prf = _sb.getSdrRoot().readProfile(_profile)
    comp=SandboxDevice(_sb, 
                       _profile, 
                       spd, 
                       scd, 
                       prf, 
                       dev._get_identifier(), 
                       dev._get_label(), 
                       impl)
    if comp:
        comp.ref=dev
    return comp


class _BaseMSDDControllerTest(ossie.utils.testing.RHTestCase):
    # Path to the SPD file, relative to this file. This must be set in order to
    # launch the device.
    SPD_FILE = '../MSDD_Controller.spd.xml'

    # setUp is run before every function preceded by "test" is executed
    # tearDown is run after every function preceded by "test" is executed
    
    # self.comp is a device using the sandbox API
    # to create a data source, the package sb contains sources like StreamSource or FileSource
    # to create a data sink, there are sinks like StreamSink and FileSink
    # to connect the component to get data from a file, process it, and write the output to a file, use the following syntax:
    #  src = sb.FileSource('myfile.dat')
    #  snk = sb.StreamSink()
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
                self.alloc_params = getAllocationParams(IP_ADDRESS)
        except Exception, e:
                self.fail("Unable to identify MSDD, " + str(IP_ADDRESS) + " reason:" + str(e))

        self.preConfigure()

        # Launch the device, using the selected implementation
        tuner_output_cfg=[]
        block_output_cfg=[]
        self.getOutputConfiguration(tuner_output_cfg, block_output_cfg )

        configure = {
            'DEBUG_LEVEL': DEBUG_LEVEL,
            "msdd" : {
                     "msdd::ip_address": IP_ADDRESS,
                     "msdd::port":"23",
                     "msdd::timeout" : 0.10,
                      },
            "time_of_day": {
                "time_of_day::mode": TOD_MODE,
            },
            "tuner_output": tuner_output_cfg,
            "block_tuner_output": block_output_cfg,
        }

        test_cfg_props=self.getProperties()
        configure.update(test_cfg_props)

        self.alloc1=None
        self.fei_dev=None
        self.alloc2=None

        self.preLaunch()

        self.comp = sb.launch(self.spd_file, impl=self.impl,properties=configure)
    
    def tearDown(self):

        try:
            if self.alloc2 and self.fei_dev:
                self.fei_dev.deallocateCapacity(self.alloc2)
        except:
            pass

        try:
            if self.alloc1 and self.fei_dev:
                self.fei_dev.deallocateCapacity(self.alloc1)
        except:
            pass

        # Clean up all sandbox artifacts created during test
        sb.release()


    def preConfigure(self):
        pass

    def preLaunch(self):
        pass

    def getProperties(self):
        return {}

    def getOutputConfiguration(self, tuner_output_cfg, block_output_cfg):
        for rcv_id, alloc_params in self.alloc_params.iteritems():
            tuner_output_cfg.append( {
                     "tuner_output::receiver_identifier": rcv_id,
                     "tuner_output::tuner_number": alloc_params['wbddc_tuner'],
                     "tuner_output::protocol":"UDP_SDDS",
                     "tuner_output::interface": MSDD_INTERFACE,
                     "tuner_output::ip_address":"234.168.103.100",
                     "tuner_output::port":0,
                     "tuner_output::vlan":0,
                     "tuner_output::enabled":True,
                     "tuner_output::timestamp_offset":0,
                     "tuner_output::endianess":1,
                     "tuner_output::mfp_flush":63,
                     "tuner_output::vlan_enable": False
            })               
            block_output_cfg.append({
                     "block_tuner_output::receiver_identifier": rcv_id,
                     "block_tuner_output::tuner_number_start": alloc_params['wbddc_tuner'],
                     "block_tuner_output::tuner_number_stop": alloc_params['wbddc_tuner']+2,
                     "block_tuner_output::protocol":"UDP_SDDS",
                     "block_tuner_output::interface": MSDD_INTERFACE,
                     "block_tuner_output::ip_address":"234.168.103.100",
                     "block_tuner_output::port":0,
                     "block_tuner_output::increment_port": True,
                     "block_tuner_output::vlan":0,
                     "block_tuner_output::enabled":True,
                     "block_tuner_output::timestamp_offset":0,
                     "block_tuner_output::endianess":1,
                     "block_tuner_output::mfp_flush":63,
                     "block_tuner_output::vlan_enable": False                        
            })
        

    def getDevices(self):
        """
        Add devices attribute to launched MSDD_Controller with proper
        sandbox wrappers.
        """
        if self.comp:
            try: 
                getattr(self.comp, 'devices')
            except:
                self.comp.devices=[]
                for d in self.comp.ref.devices:
                    self.comp.devices.append( createSandboxDevice(d) )

    def getAllocationContext(self, rcv_id='RCV:1'):
        self.getDevices()
        fei_dev=None
        alloc_params=None
        for d in self.comp.devices:
            if d.msdd_status.receiver_identifier == rcv_id:
                fei_dev=d
                alloc_params=self.alloc_params[rcv_id]

        if not fei_dev:
            self.fail("Unable to identify FRONTEND allocation device, receiver id: " + str(rcv_id))

        return fei_dev, alloc_params

class DeviceTests(_BaseMSDDControllerTest):

    def testBasicBehavior(self):
        #######################################################################
        # Make sure start and stop can be called without throwing exceptions
        self.comp.start()
        self.comp.stop()

    def testSingleTunerAllocation(self):
        
        self.comp.start()

        self.fei_dev, alloc_params = self.getAllocationContext(ALLOCATE_RCV)
        
        sink = sb.DataSinkSDDS()
               
        alloc = self._generateAlloc(cf=110e6,sr=0,bw=0)
        allocationID = properties.props_to_dict(alloc)['FRONTEND::tuner_allocation']['FRONTEND::tuner_allocation::allocation_id']
        self.fei_dev.connect(sink,usesPortName = "dataSDDS_out",connectionId=allocationID)
        sink.start()
        
        try:
            retval = self.fei_dev.allocateCapacity(alloc)
        except Exception, e:
            print "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
            print "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
            print "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
            print str(e)
            self.assertFalse("Exception thrown on allocateCapactiy %s" % str(e))
        if not retval:
            self.assertFalse("Allocation Failed")
            
        time.sleep(1)
        sri = sink.sri()
        self.assertEqual(sri.streamID, allocationID)
        self.alloc1=alloc

        
    def testReportedBandwidth(self):
        self.comp.start()

        self.fei_dev, alloc_params = self.getAllocationContext(ALLOCATE_RCV)

        alloc = self._generateAlloc(cf=110e6,
                                    sr=alloc_params['wbddc_srate'],
                                    bw=alloc_params['wbddc_bw'])
        allocationID = properties.props_to_dict(alloc)['FRONTEND::tuner_allocation']['FRONTEND::tuner_allocation::allocation_id']

        try:
            retval = self.fei_dev.allocateCapacity(alloc)
        except Exception, e:
            print str(e)
            self.fail("Exception thrown on allocateCapactiy %s" % str(e))

        if not retval:
            self.fail("Allocation Failed")

        tuner_status = self.fei_dev.frontend_tuner_status[0]
        self.alloc1=alloc

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



class MsddDeviceTests(_BaseMSDDControllerTest):

    def testWideBandDDC_SampleRateAllocation(self):
        self.fei_dev, alloc_params = self.getAllocationContext(ALLOCATE_RCV)

        alloc=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                             center_frequency=100e6, 
                                             sample_rate=alloc_params['wbddc_srate'],
                                             sample_rate_tolerance=100.0)
        ret=self.fei_dev.allocateCapacity( alloc)
        self.assertTrue(ret)
        self.alloc1=alloc

    def testWideBandDDC_BandwidthAllocation(self):
        self.fei_dev, alloc_params = self.getAllocationContext(ALLOCATE_RCV)
        alloc=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                             center_frequency=100e6, 
                                             bandwidth=alloc_params['wbddc_bw'],
                                             bandwidth_tolerance=100.0)
        ret=self.fei_dev.allocateCapacity( alloc)
        self.assertTrue(ret)
        self.alloc1=alloc

    def testNarrowBandDDC_BandwidthAllocation(self):
        self.fei_dev, alloc_params = self.getAllocationContext(ALLOCATE_RCV)

        if alloc_params['nbddc_srate'] is None : return

        wb_alloc=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                               center_frequency=100e6)
        ret=self.fei_dev.allocateCapacity( wb_alloc)
        self.assertTrue(ret)
        self.alloc1=wb_alloc

        alloc=frontend.createTunerAllocation(tuner_type="DDC", 
                                             center_frequency=100e6,
                                             bandwidth=alloc_params['nbddc_bw'],
                                             bandwidth_tolerance=100.0)
        ret=self.fei_dev.allocateCapacity(alloc)
        self.assertTrue(ret)
        self.alloc2=alloc

    def testNarrowBandDDC_SampleRateAllocation(self):

        self.fei_dev, alloc_params = self.getAllocationContext(ALLOCATE_RCV)

        if alloc_params['nbddc_srate'] is None : return

        wb_alloc=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                                center_frequency=100e6)
        ret=self.fei_dev.allocateCapacity( wb_alloc)
        self.assertTrue(ret)
        self.alloc1=wb_alloc

        alloc=frontend.createTunerAllocation(tuner_type="DDC", 
                                             center_frequency=100e6,
                                             sample_rate=alloc_params['nbddc_srate'],
                                             sample_rate_tolerance=100.0)
        ret=self.fei_dev.allocateCapacity( alloc)
        self.assertTrue(ret)
        self.alloc2=alloc

    def testNarrowBandDDC_BadAllocation(self):

        self.fei_dev, alloc_params = self.getAllocationContext(ALLOCATE_RCV)

        if alloc_params['nbddc_srate'] is None : return

        wb_alloc=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                               center_frequency=100e6)
        ret=self.fei_dev.allocateCapacity( wb_alloc)
        self.assertTrue(ret)
        self.alloc1=wb_alloc

        alloc=frontend.createTunerAllocation(tuner_type="DDC", 
                                             center_frequency=100e6,
                                             sample_rate=20,
                                             sample_rate_tolerance=100.0)
        ret=self.fei_dev.allocateCapacity( alloc)
        self.assertFalse(ret)


    def testNarrowBandDDC_BadAllocation2(self):

        self.fei_dev, alloc_params = self.getAllocationContext(ALLOCATE_RCV)
        
        if alloc_params['nbddc_srate'] is None : return

        wb_alloc=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                                center_frequency=100e6)
        ret=self.fei_dev.allocateCapacity( wb_alloc)
        self.assertTrue(ret)
        self.alloc1=wb_alloc

        alloc=frontend.createTunerAllocation(tuner_type="DDC", 
                                             center_frequency=100e6,
                                             bandwidth=20,
                                             bandwidth_tolerance=100.0)
        ret=self.fei_dev.allocateCapacity( alloc)
        self.assertFalse(ret)


    def testNarrowBandDDC_DontCareAllocation(self):

        self.fei_dev, alloc_params = self.getAllocationContext(ALLOCATE_RCV)

        if alloc_params['nbddc_srate'] is None : return

        wb_alloc=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                                center_frequency=100e6)
        ret=self.fei_dev.allocateCapacity( wb_alloc)
        self.assertTrue(ret)
        self.alloc1=wb_alloc

        alloc=frontend.createTunerAllocation(tuner_type="DDC", 
                                             center_frequency=100e6)
        ret=self.fei_dev.allocateCapacity( alloc)
        self.assertTrue(ret)
        self.alloc2=alloc


class RFInfoTest(_BaseMSDDControllerTest):

    def getOutputConfiguration(self, tuner_output_cfg, block_output_cfg):
        for rcv_id, alloc_params in self.alloc_params.iteritems():
            tuner_output_cfg.append( {
                     "tuner_output::receiver_identifier": rcv_id,
                     "tuner_output::tuner_number": alloc_params['wbddc_tuner'],
                     "tuner_output::protocol":"UDP_SDDS",
                     "tuner_output::interface": MSDD_INTERFACE,
                     "tuner_output::ip_address": "239.1.1.1",
                     "tuner_output::port":29000,
                     "tuner_output::vlan":0,
                     "tuner_output::enabled": alloc_params['wbddc_enable'],
                     "tuner_output::timestamp_offset":0,
                     "tuner_output::endianess":1,
                     "tuner_output::mfp_flush":63,
                     "tuner_output::vlan_enable": False
            })               
            tuner_output_cfg.append( {
                     "tuner_output::receiver_identifier": rcv_id,
                     "tuner_output::tuner_number": alloc_params['nbddc_tuner'],
                     "tuner_output::protocol":"UDP_SDDS",
                     "tuner_output::interface": MSDD_INTERFACE,
                     "tuner_output::ip_address":"239.1.1.2",
                     "tuner_output::port":29001,
                     "tuner_output::vlan":0,
                     "tuner_output::enabled":True,
                     "tuner_output::timestamp_offset":0,
                     "tuner_output::endianess":1,
                     "tuner_output::mfp_flush":63,
                     "tuner_output::vlan_enable": False
             })

    def testRFFlowID(self): 

        self.fei_dev, alloc_params = self.getAllocationContext(ALLOCATE_RCV)

        #create rf_info uses port and connect to MSDD
        out_rf_info_port=frontend.OutRFInfoPort("out_control")
        in_rf_info_port=self.fei_dev.getPort("RFInfo_in")
        out_rf_info_port.connectPort(in_rf_info_port,"test_rf_flow")

	# get params for allocations
	bw=alloc_params['wbddc_bw']
	sr=alloc_params['wbddc_srate']

        # allocation params
        flow_id = "ca-710-flow"
        cf=100e6

        # set rf flow id
        out_rf_info_port._set_rf_flow_id(flow_id)

        # check rf_flow_id was set
        n=len(self.fei_dev.frontend_tuner_status)
        expected=[flow_id]*n
        actual=[ x["FRONTEND::tuner_status::rf_flow_id"] for x in self.fei_dev.frontend_tuner_status ]
        self.assertEqual(expected,actual, "Mismatch of RF Flow Ids for tuners")

        # allocation for sample rate and rf_flow_id
        alloc1=frontend.createTunerAllocation(center_frequency=cf, sample_rate=sr, rf_flow_id=flow_id)
        ret=self.fei_dev.allocateCapacity(alloc1)
        self.alloc1=alloc1
        alloc1_aid =  alloc1["FRONTEND::tuner_allocation"]["FRONTEND::tuner_allocation::allocation_id"]
        self.assertEqual(True,ret, "Allocation failed using rf_flow_id")

        alloc2_aid = alloc1_aid
        if alloc_params['nbddc_srate'] is not None :
               # allocation for center freq and rf_flow_id
               alloc2=frontend.createTunerAllocation(center_frequency=cf,  rf_flow_id=flow_id)
               ret=self.fei_dev.allocateCapacity( alloc2)
               alloc2_aid =  alloc2["FRONTEND::tuner_allocation"]["FRONTEND::tuner_allocation::allocation_id"]
               self.assertEqual(True,ret, "Allocation failed using rf_flow_id again ")
               self.alloc2=alloc2

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
        self.fei_dev.connect(sdds_in, connectionId=alloc2_aid, usesPortName='dataSDDS_out')
        sdds_in.connect(sink, usesPortName="dataShortOut")

        kws=None
        try:
            sink_data=sink.read()
            kws=properties.props_to_dict(sink_data.sri.keywords)
        except:
            pass
        self.assertEqual( kws["FRONTEND::RF_FLOW_ID"] , flow_id, "Missing RF_FLOW_ID from keyword list")


    def testRFFlowIDFailure(self): 

        self.fei_dev, alloc_params = self.getAllocationContext(ALLOCATE_RCV)
        
        #create rf_info uses port and connect to MSDD
        out_rf_info_port=frontend.OutRFInfoPort("out_control")
        in_rf_info_port=self.fei_dev.getPort("RFInfo_in")
        out_rf_info_port.connectPort(in_rf_info_port,"test_rf_flow")

	# get params for allocations
	bw=alloc_params['wbddc_bw']
	sr=alloc_params['wbddc_srate']

        # allocation params
        flow_id = "ca-710-flow"
        cf=100e6

        # set rf flow id
        out_rf_info_port._set_rf_flow_id(flow_id)

        # check rf_flow_id was set
        n=len(self.fei_dev.frontend_tuner_status)
        expected=[flow_id]*n
        actual=[ x["FRONTEND::tuner_status::rf_flow_id"] for x in self.fei_dev.frontend_tuner_status ]
        self.assertEqual(expected,actual, "Mismatch of RF Flow Ids for tuners")

        # allocation for sample rate and rf_flow_id
        alloc1=frontend.createTunerAllocation(center_frequency=cf, sample_rate=sr, rf_flow_id=flow_id)
        ret=self.fei_dev.allocateCapacity(alloc1)
        alloc1_aid =  alloc1["FRONTEND::tuner_allocation"]["FRONTEND::tuner_allocation::allocation_id"]
        self.assertEqual(True,ret, "Allocation failed using rf_flow_id")
        self.alloc1=alloc1

        if len(self.fei_dev.frontend_tuner_status) > 1:
                # allocation for center freq and rf_flow_id
                alloc2=frontend.createTunerAllocation(center_frequency=cf,  rf_flow_id="noworkie")
                ret=self.fei_dev.allocateCapacity( alloc2)
                self.assertEqual(False,ret, "Allocation should have failed for unknown rf_flow_id ")
                self.alloc2=alloc2



    def testRFInfoPkt(self):

        self.fei_dev, alloc_params = self.getAllocationContext(ALLOCATE_RCV)

        #create rf_info uses port and connect to MSDD
        out_rf_info_port=frontend.OutRFInfoPort("out_control")
        in_rf_info_port=self.fei_dev.getPort("RFInfo_in")
        out_rf_info_port.connectPort(in_rf_info_port,"test_rf_flow")

	bw=alloc_params['wbddc_bw']
	sr=alloc_params['wbddc_srate']
        # allocation params
        flow_id = "ca-710-flow-2"
        cf=100e6

        # send info pkto
        pkt=_generateRFInfoPkt(cf,bw,rf_flow_id=flow_id)
        out_rf_info_port._set_rfinfo_pkt(pkt)

        # check rf_flow_id was set
        n=len(self.fei_dev.frontend_tuner_status)
        expected=[flow_id]*n
        actual=[ x["FRONTEND::tuner_status::rf_flow_id"] for x in self.fei_dev.frontend_tuner_status ]
        self.assertEqual(expected,actual, "Mismatch of RF Flow Ids for tuners")

        # allocation for sample rate and rf_flow_id
        alloc1=frontend.createTunerAllocation(center_frequency=cf, sample_rate=sr, rf_flow_id=flow_id)
        ret=self.fei_dev.allocateCapacity( alloc1)
        alloc1_aid =  alloc1["FRONTEND::tuner_allocation"]["FRONTEND::tuner_allocation::allocation_id"]
        self.assertEqual(True,ret, "Allocation failed using rf_flow_id")
        self.alloc1=alloc1

        alloc2_aid = alloc1_aid
        if alloc_params['nbddc_srate'] is not None :
                # allocation for center freq and rf_flow_id
                alloc2=frontend.createTunerAllocation(center_frequency=cf,  rf_flow_id=flow_id)
                ret=self.fei_dev.allocateCapacity( alloc2)
                alloc2_aid =  alloc2["FRONTEND::tuner_allocation"]["FRONTEND::tuner_allocation::allocation_id"]
                self.assertEqual(True,ret, "Allocation failed using rf_flow_id again ")
                self.alloc2=alloc2

        # valid rf_flow_id was probagated downstream
        sink=sb.StreamSink()
        sdds_in=sb.launch('rh.SourceSDDS',  properties={'interface':INTERFACE})
        sb.start()
        self.fei_dev.connect(sdds_in, connectionId=alloc2_aid, usesPortName='dataSDDS_out')
        sdds_in.connect(sink, usesPortName="dataShortOut")

        kws=None
        try:
            sink_data=sink.read()
            kws=properties.props_to_dict(sink_data.sri.keywords)
        except:
            pass
        self.assertEqual( kws["FRONTEND::RF_FLOW_ID"] , flow_id, "Missing RF_FLOW_ID from keyword list")


class OnePpsTests(_BaseMSDDControllerTest):

    def preConfigure(self):
        self.temp_files=[]

    def getOutputConfiguration(self, tuner_output_cfg, block_output_cfg):
        for rcv_id, alloc_params in self.alloc_params.iteritems():
            tuner_output_cfg.append( {
                     "tuner_output::receiver_identifier": rcv_id,
                     "tuner_output::tuner_number": alloc_params['wbddc_tuner'],
                     "tuner_output::protocol":"UDP_SDDS",
                     "tuner_output::interface": MSDD_INTERFACE,
                     "tuner_output::ip_address": "239.1.1.1",
                     "tuner_output::port":29000,
                     "tuner_output::vlan":0,
                     "tuner_output::enabled": alloc_params['wbddc_enable'],
                     "tuner_output::timestamp_offset":0,
                     "tuner_output::endianess":1,
                     "tuner_output::mfp_flush":63,
                     "tuner_output::vlan_enable": False
            })               

    def getProperties(self):
        return {
            'LOGGING_CONFIG_URI': 'file://' + os.getcwd() + '/OnePpsTests.log.cfg'
        }
    
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

        msdd_time_of_day = self.comp.time_of_day.queryValue()
        mode = msdd_time_of_day['time_of_day::mode']
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

        msdd_time_of_day = self.comp.time_of_day.queryValue()
        mode = msdd_time_of_day['time_of_day::mode']
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


class IPPInterfaceTest(_BaseMSDDControllerTest):

    def preConfigure(self):
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

    def getOutputConfiguration(self, tuner_output_cfg, block_output_cfg):
        for rcv_id, alloc_params in self.alloc_params.iteritems():
            tuner_output_cfg.append( {
                     "tuner_output::receiver_identifier": rcv_id,
                     "tuner_output::tuner_number": alloc_params['wbddc_tuner'],
                     "tuner_output::protocol":"UDP_SDDS",
                     "tuner_output::interface": MSDD_INTERFACE,
                     "tuner_output::ip_address": "239.1.1.1",
                     "tuner_output::port":29000,
                     "tuner_output::vlan":0,
                     "tuner_output::enabled": True,
                     "tuner_output::timestamp_offset":0,
                     "tuner_output::endianess":1,
                     "tuner_output::mfp_flush":63,
                     "tuner_output::vlan_enable": False
            })               
            tuner_output_cfg.append( {
                     "tuner_output::receiver_identifier": rcv_id,
                     "tuner_output::tuner_number": alloc_params['nbddc_tuner'],
                     "tuner_output::protocol":"UDP_SDDS",
                     "tuner_output::interface": MSDD_INTERFACE,
                     "tuner_output::ip_address":"239.1.1.2",
                     "tuner_output::port":29001,
                     "tuner_output::vlan":0,
                     "tuner_output::enabled":True,
                     "tuner_output::timestamp_offset":0,
                     "tuner_output::endianess":1,
                     "tuner_output::mfp_flush":63,
                     "tuner_output::vlan_enable": False
             })

    def testTunerStatus(self):
       self.fei_dev, alloc_params = self.getAllocationContext(ALLOCATE_RCV)

       self.alloc1=frontend.createTunerAllocation(tuner_type="RX_DIGITIZER_CHANNELIZER", 
                                            center_frequency=100e6, 
                                            sample_rate=alloc_params['wbddc_srate'],
                                            sample_rate_tolerance=100.0)
       ret=self.fei_dev.allocateCapacity(self.alloc1)
       self.assertTrue(ret) 
       alloc1_aid = self.alloc1["FRONTEND::tuner_allocation"]["FRONTEND::tuner_allocation::allocation_id"]

       expected=True
       actual=self.fei_dev.frontend_tuner_status[0].enabled
       self.assertEqual(expected,actual, "Tuner status enabled failed")

       expected=True
       actual=self.fei_dev.frontend_tuner_status[0].output_enabled
       self.assertEqual(expected,actual, "Tuner status output enabled failed")

       sink=sb.StreamSink()
       sdds_in=sb.launch('rh.SourceSDDS',
                         properties={'interface': INTERFACE}) 
       sb.start()
       self.fei_dev.connect(sdds_in, connectionId=alloc1_aid,
                            usesPortName='dataSDDS_out') 
       sdds_in.connect(sink,usesPortName="dataShortOut")

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
    parser.add_argument('--alloc-rcv', default="RCV:1", help="Receiver module to allocate against")
    parser.add_argument('--msdd-iface', default=0, help="MSDD radio interface for output (zero based, for eth1 or single interface)", type=int)
    parser.add_argument('--tod', default="ONEPPS", help="set TOD mode: ONEPPS, SIM")
    parser.add_argument('--debug', default='info', help="debug level, fatal, error, warn, info, debug, trace" )

    try:
        args, remaining_args = parser.parse_known_args()
        IP_ADDRESS=args.ip
        DEBUG_LEVEL=get_debug_level(args.debug)
        INTERFACE=args.iface
        MSDD_INTERFACE=args.msdd_iface
        ALLOCATE_RCV=args.alloc_rcv
        TOD_MODE=args.tod
    except SystemExit:
        raise SystemExit
    except:
        traceback.print_exc()
        pass

    sys.argv[1:] = remaining_args
    ossie.utils.testing.main() # By default tests all implementations
