#!/usr/bin/env python
#
# This file is protected by Copyright. Please refer to the COPYRIGHT file
# distributed with this source distribution.
#
# This file is part of REDHAWK.
#
# REDHAWK is free software: you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by the
# Free Software Foundation, either version 3 of the License, or (at your
# option) any later version.
#
# REDHAWK is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.

import os, sys, copy, traceback
script_dir = os.path.dirname(os.path.abspath(__file__))
lib_dir = os.path.join(script_dir, 'fei_base')
sys.path.append(lib_dir)
import frontend_tuner_unit_test_base as fe
sdr_devices = os.path.join(os.environ.get('SDRROOT','/var/redhawk/sdr'),'dev/devices')

DEBUG_LEVEL = 3    # typical values include 0, 1, 2, 3, 4 and 5

def get_debug_level( debug ):
    _rlookup = { 'FATAL' :  0,
                 'ERROR' : 1,
                 'WARN' : 2,
                 'INFO' : 3,
                 'DEBUG' : 4,
                 'TRACE' : 5 }
    return _rlookup[debug.upper()]


########################################
# BEGIN Custom Override Functions      #
########################################

# If special logic must be used when generating tuner requests, modify the
# function below. and add 'tuner_gen' key to the dut_config struct with the
# function as the associated value. Add as many custom functions as needed for
# all target devices in dut_config.
def customGenerateTunerRequest(idx=0):
    #Pick a random set for CF,BW,SR and return
    value = {}
    value['ALLOC_ID'] = str(uuid.uuid4())
    value['TYPE'] = 'RX_DIGITIZER'
    value['BW_TOLERANCE'] = 100.0
    value['SR_TOLERANCE'] = 100.0
    value['RF_FLOW_ID'] = ''
    value['GROUP_ID'] = ''
    value['CONTROL'] = True

    value['CF'] = getValueInRange(DEVICE_INFO['capabilities'][idx]['RX_DIGITIZER']['CF'])
    sr_subrange = DEVICE_INFO['capabilities'][idx]['RX_DIGITIZER']['SR']
    if 'sr_limit' in DEVICE_INFO and DEVICE_INFO['sr_limit'] > 0:
        sr_subrange = getSubranges([0,DEVICE_INFO['sr_limit']], DEVICE_INFO['capabilities'][idx]['RX_DIGITIZER']['SR'])
    value['SR'] = getValueInRange(sr_subrange)
    # Usable BW is typically equal to SR if complex samples, otherwise half of SR
    BW_MULT = 1.0 if DEVICE_INFO['capabilities'][idx]['RX_DIGITIZER']['COMPLEX'] else 0.5
    value['BW'] = 0.8*value['SR']*BW_MULT # Try 80% of SR
    if isValueInRange(value['BW'], DEVICE_INFO['capabilities'][idx]['RX_DIGITIZER']['BW']):
        # success! all done, return value
        return value

    # Can't use 80% of SR as BW, try to find a BW value within SR tolerances
    bw_min = value['SR']*BW_MULT
    bw_max = value['SR']*(1.0+(value['SR_TOLERANCE']/100.0))*BW_MULT
    tolerance_range = [bw_min,bw_max]
    bw_subrange = getSubranges(tolerance_range, DEVICE_INFO['capabilities'][idx]['RX_DIGITIZER']['BW'])
    if len(bw_subrange) > 0:
        # success! get BW value and return
        value['BW'] = getValueInRange(bw_subrange)
        return value

    # last resort
    value['BW'] = getValueInRange(DEVICE_INFO['capabilities'][idx]['RX_DIGITIZER']['BW'])
    return value
#dut_config['custom']['tuner_gen']=customGenerateTunerRequest # TODO - uncomment this line to use custom
                                                              #        function for custom DUT

########################################
#   END Custom Override Functions      #
########################################

class FrontendTunerTests(fe.FrontendTunerTests):
    
    def __init__(self,*args,**kwargs):
        import ossie.utils.testing
        super(FrontendTunerTests,self).__init__(*args,**kwargs)

    # Use functions below to add pre-/post-launch commands if your device has special startup requirements
    @classmethod
    def devicePreLaunch(cls):
        pass
    # Use functions below to add pre-/post-release commands if your device has special shutdown requirements
    @classmethod
    def devicePreRelease(self):
        pass
    @classmethod
    def devicePostRelease(self):
        pass


if __name__ == '__main__':
    import argparse
    print sys.argv
    parser = argparse.ArgumentParser(
        description='''
Runs FEI test suite against a MSDD device. Use optional arguments to setup MSDD, all other command line arguments
are passed to nosetest
i.e. test_MSDD_Controller_FEI.py  -h --ip=192.168.1.96 --iface=em2 --debug=ERROR  -v --nocapture test_MSDD_Controller_FEI.py:FrontendTunerTests -m  testFRONTEND_1
''')
    parser.add_argument('--ip', default="192.168.1.2", help="ip address of msdd")
    parser.add_argument('--port', default='23', help="port number for msdd")
    parser.add_argument('--device', default=None, help="provide msdd device string when running tests, default is to run id_msdd")
    parser.add_argument('--rcv-id', default="RCV:1", help="Receiver module to allocate against")
    parser.add_argument('--output_ip', default=None, help="ip/multicast starting address for output channels")
    parser.add_argument('--output_port', default=None, help="starting port number for output channels, default starts at 0")
    parser.add_argument('--output_vlan', default=None, help="vlan id to assign to output channels, default is 0")
    parser.add_argument('--psd_ip', default=None, help="ip/multicast address for psd output, if enabled in msdd configuration")
    parser.add_argument('--debug', default='info', help="debug level, fatal, error, warn, info, debug, trace" )

    args, remaining_args = parser.parse_known_args()
    DEBUG_LEVEL=get_debug_level(args.debug)


    msdd_id=None
    if args.device is not None:
        msdd_id = args.device
    else:
        try:
            from id_msdd import id_msdd
            msdd_id = id_msdd(args.ip)
        except:
            raise Exception("Unable to contact ip: " + args.ip)

    import dut
    dut_config=dut.get_config( msdd_id,
                                  DUT_IP_ADDR=args.ip,
                                  DUT_PORT=args.port,
                                  DUT_RCV_ID=args.rcv_id,
                                  OUTPUT_ADDR=args.output_ip,
                                  OUTPUT_PORT=args.output_port,
                                  OUTPUT_VLAN=args.output_vlan,
                                  PSD_ADDR=args.psd_ip)
    if dut_config is None:
        raise Exception("No configuration for msdd id: " + str(msdd_id))
    
    print "Running FEI test suite against:  msdd_id:", msdd_id, " ip ", args.ip
    sys.argv[1:]=remaining_args
    fe.set_debug_level(DEBUG_LEVEL)
    fe.set_device_info(dut_config)

    # run using nose
    import nose
    nose.main(defaultTest=__name__)
