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
import time
import frontend

DEBUG_LEVEL = 4
IP_ADDRESS="192.168.103.250"

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
        # Launch the device, using the selected implementation
        configure = {
                 "msdd_configuration" : {
                     "msdd_configuration::msdd_ip_address":IP_ADDRESS,
                     "msdd_configuration::msdd_port":"23"
                      }
                 ,
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
                    }]
                                    
                 }
        
        
        self.comp = sb.launch(self.spd_file, impl=self.impl,configure=configure,execparams={'DEBUG_LEVEL': DEBUG_LEVEL})
    
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

        self.comp=sb.launch('rh.MSDD', 
                      properties={ 
                "DEBUG_LEVEL": DEBUG_LEVEL, 
                "msdd_configuration" : { "msdd_configuration::msdd_ip_address" :  IP_ADDRESS },
                "advanced":{
                                  "advanced::allow_internal_allocations" : False 
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
                             }]
                }

                      )


        alloc_params = {}
        if "vr1a" in self.comp.msdd_status.filename_fpga:
            self.alloc_params = { 'wbddc_bw' : 20e6,
                             'wbddc_srate' : 25e6,
                             'nbddc_bw' : 3.125e6,
                             'nbddc_srate' : 1.526e6,
            }
        else:
            self.alloc_params = { 'wbddc_bw' : 20e6,
                             'wbddc_srate' : 24.576e6,
                             'nbddc_bw' : 1.5e6,
                             'nbddc_srate' : 4.9125e6,
            }
    
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


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--ip', default="192.168.103.250")
    parser.add_argument('--debug', default=3)
    parser.add_argument('unittest_args', nargs='*')

    try:
        args = parser.parse_args()
        IP_ADDRESS=args.ip
        DEBUG_LEVEL=args.debug
    except:
        pass
    sys.argv[1:] = args.unittest_args
    ossie.utils.testing.main() # By default tests all implementations
