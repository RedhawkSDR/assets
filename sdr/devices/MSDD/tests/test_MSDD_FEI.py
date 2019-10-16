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

import os, sys, copy
script_dir = os.path.dirname(os.path.abspath(__file__))
lib_dir = os.path.join(script_dir, 'fei_base')
sys.path.append(lib_dir)
import frontend_tuner_unit_test_base as fe
sdr_devices = os.path.join(os.environ.get('SDRROOT','/var/redhawk/sdr'),'dev/devices')
dut_config = {}

''' TODO:
  1)  set DUT to the key of one of the device configurations defined in the
      device_config dictionary below.
  2)  override spd, parent_dir, namespace, name, execparams, configure,
      or properties if necessary.
  3)  If 'custom', modify configuration to have accurate capabilities for the
      device under test.
  4)  Set DEBUG_LEVEL, DUT_*, or MCAST_* if desired and applicable.
  5)  Advanced: Override functions with device specific behavior.
'''

#******* MODIFY CONFIG BELOW **********#
#DUT = 'MSDD|3000|s100|vr1a'
DUT = "MSDD|3000|s98"

# Optional:
DEBUG_LEVEL = 4    # typical values include 0, 1, 2, 3, 4 and 5
DUT_INDEX = None
#DUT_IP_ADDR = "192.168.11.97"
DUT_IP_ADDR = "192.168.12.2"
DUT_PORT = None
DUT_IFACE = None
MCAST_GROUP = "234.168.103.100"
MCAST_PORT = None
MCAST_VLAN = None
MCAST_IFACE = None

# Your custom device
dut_config['custom'] = {
    # parent_dir/namespace/name are only used when spd = None
    'spd'         : None,       # path to SPD file for asset.
    'parent_dir'  : None,       # path to parent directory
    'namespace'   : None,       # leave as None if asset is not namespaced
    'name'        : 'myDevice', # exact project name, without namespace
    'impl_id'     : None,       # None for all (or only), 'cpp', 'python', 'java', etc.
    'execparams'  : {},         # {'prop_name': value}
    'configure'   : {},         # {'prop_name': value}
    'properties'  : {},         # {'prop_name': value}
    'capabilities': [           # entry for each tuner; single entry if all tuners are the same
        {                       # include entry for each tuner type (i.e. RX, TX, RX_DIGITIZER, etc.)
            'RX_DIGITIZER': {   # include ranges for CF, BW, SR, and GAIN
                'COMPLEX' : True,
                # To specify a range from 0 to 10 with a gap from 3 to 5: [0, 3, 5, 10]
                'CF'      : [100e6, 500e6], # enter center frequency range in Hz
                'BW'      : [0.0, 1e3],     # enter bandwidth range in Hz
                'SR'      : [0.0, 1e3],     # enter sample rate range in Hz or sps
                'GAIN'    : [-5.0, 10.0]    # enter gain range in dB
            }
        }
    ]
}

# rh.MSDD
dut_config['MSDD'] = {
    'spd'         : '../MSDD.spd.xml',
    'parent_dir'  : None,
    'namespace'   : 'rh',
    'name'        : 'MSDD',
    'impl_id'     : 'python',
    'num_output_configs' : 2,
    'max_output_enabled' : 2,
    'execparams'  : {},
    'configure'   : {
        'advanced' : {
            'advanced::enable_secondary_tuners' : True,
            'advanced::enable_fft_channels' : False
        },

        'msdd_configuration': {
            'msdd_configuration::msdd_ip_address': DUT_IP_ADDR or '192.168.100.250',
            'msdd_configuration::msdd_port':       '%s'%(DUT_PORT) if DUT_PORT else '23'
        },
        'msdd_output_configuration': [
            {
                'msdd_output_configuration::tuner_number'    : 0,
                'msdd_output_configuration::protocol'        : 'UDP_SDDS',
                'msdd_output_configuration::ip_address'      : MCAST_GROUP or '234.0.0.100',
                'msdd_output_configuration::port'            : MCAST_PORT or 0,
                'msdd_output_configuration::vlan'            : MCAST_VLAN or 0,
                'msdd_output_configuration::enabled'         : True,
                'msdd_output_configuration::timestamp_offset': 0,
                'msdd_output_configuration::endianess'       : 1,
                'msdd_output_configuration::mfp_flush'       : 63,
                'msdd_output_configuration::vlan_enable'     : False                        
            } # more added below
        ],

        'msdd_psd_output_configuration': [
            {
                'msdd_psd_output_configuration::fft_channel_start'    : 0,
                'msdd_psd_output_configuration::fft_channel_stop'    : 0,
                'msdd_psd_output_configuration::protocol'        : 'UDP_SDDS',
                'msdd_psd_output_configuration::ip_address'      : MCAST_GROUP or '233.0.0.100',
                'msdd_psd_output_configuration::port'            : MCAST_PORT or 0,
                'msdd_psd_output_configuration::vlan'            : MCAST_VLAN or 0,
                'msdd_psd_output_configuration::enabled'         : False,
                'msdd_psd_output_configuration::timestamp_offset': 0,
                'msdd_psd_output_configuration::endianess'       : 1,
                'msdd_psd_output_configuration::mfp_flush'       : 63,
                'msdd_psd_output_configuration::vlan_enable'     : False
            } # more added below
        ]

    },
    'properties'  : {},
    
    # The below capabilities are based on an MSDD-6000 with a freq range from 30 - 6000 MHz. It also
    # assumes a 100 MSPS ADC clock which creates a max SR of 25 MSPS. For MSDD 3000 models the max
    # frequency would need to be adjusted to 3000 MHz. For models that have the 98 MSPS clock
    # (s98 FPGA loads) the max sample rate should be set to 24.576e6. 
    'capabilities': [
        {
            'RX_DIGITIZER': {
                'COMPLEX' : True,
                'CF'      : [30e6, 6e9],
                'BW'      : [20e6, 20e6],
                'SR'      : [25e6, 25e6],
                'GAIN'    : [-48.0, 12.0]
            },
            'RX_DIGITIZER_CHANNELIZER': {
                'COMPLEX' : True,
                'CF'      : [30e6, 6e9],
                'BW'      : [20e6, 20e6],
                'SR'      : [25e6, 25e6],
            },
            'DDC': {
                'COMPLEX' : True,
                'CF'      : [30e6, 6e9],
                'BW'      : [80e3, 80e3],
                'SR'      : [100e3, 100e3],
                'GAIN'    : [-48.0, 12.0],
                'NUMDDCs' : 16,
            }
        }
    ]
}

# rh.MSDD 6000
dut_config['MSDD|6000'] = dut_config['MSDD']

# rh.MSDD 6000 s100
dut_config['MSDD|6000|s100'] = dut_config['MSDD']

# rh.MSDD 6000 s98
dut_config['MSDD|6000|s98'] = copy.deepcopy(dut_config['MSDD|6000'])
dut_config['MSDD|6000|s98']['capabilities'] = [
        {
            'RX_DIGITIZER': {
                'COMPLEX' : True,
                'CF'      : [30e6, 6e9],
                'BW'      : [20e6, 20e6],
                'SR'      : [24.576e6, 24.576e6],
                'GAIN'    : [-48.0, 12.0]
            },
            'RX_DIGITIZER_CHANNELIZER': {
                'COMPLEX' : True,
                'CF'      : [30e6, 6e9],
                'BW'      : [20e6, 20e6],
                'SR'      : [24.576e6, 24.576e6],
            },
            'DDC': {
                'COMPLEX' : True,
                'CF'      : [30e6, 6e9],
                'BW'      : [80e3, 80e3],
                'SR'      : [100e3, 100e3],
                'GAIN'    : [-48.0, 12.0],
                'NUMDDCs' : 16,
            }
        }
    ]

# rh.MSDD 3000
dut_config['MSDD|3000'] = copy.deepcopy(dut_config['MSDD'])
dut_config['MSDD|3000']['configure'].update({
        'advanced' : {
            'advanced::enable_secondary_tuners' : False,
            'advanced::enable_fft_channels' : False
        }
       } )

dut_config['MSDD|3000']['capabilities'] = [
        {
            'RX_DIGITIZER': {
                'COMPLEX' : True,
                'CF'      : [30e6, 3e9],
                'BW'      : [20e6, 20e6],
                'SR'      : [25e6, 25e6],
                'GAIN'    : [-48.0, 12.0]
            },
            'RX_DIGITIZER_CHANNELIZER': {
                'COMPLEX' : True,
                'CF'      : [30e6, 3e9],
                'BW'      : [20e6, 20e6],
                'SR'      : [25e6, 25e6],
            },
            'DDC': {
                'COMPLEX' : True,
                'CF'      : [30e6, 3e9],
                'BW'      : [80e3, 80e3],
                'SR'      : [100e3, 100e3],
                'GAIN'    : [-48.0, 12.0],
                'NUMDDCs' : 16,
            }
        }
    ]

# rh.MSDD 3000 s100
dut_config['MSDD|3000|s100'] = dut_config['MSDD|3000']

# rh.MSDD 3000 s98
dut_config['MSDD|3000|s98'] = copy.deepcopy(dut_config['MSDD|3000'])
dut_config['MSDD|3000|s98']['num_output_configs'] = 6
dut_config['MSDD|3000|s98']['max_output_enabled'] = 6
dut_config['MSDD|3000|s98']['capabilities'] = [
        {
            'RX_DIGITIZER': {
                'COMPLEX' : True,
                'CF'      : [30e6, 3e9],
                'BW'      : [20e6, 20e6],
                'SR'      : [24.576e6, 24.576e6],
                'GAIN'    : [-48.0, 12.0]
            },
            'RX_DIGITIZER_CHANNELIZER': {
                'COMPLEX' : True,
                'CF'      : [30e6, 3e9],
                'BW'      : [20e6, 20e6],
                'SR'      : [24.576e6, 24.576e6],
            },
            'DDC': {
                'COMPLEX' : True,
                'CF'      : [30e6, 3e9],
                'BW'      : [1.3e6,1.3e6],
                'SR'      : [4.9152e6, 4.9152e6],
                'GAIN'    : [-48.0, 12.0],
                'NUMDDCs' : 5,
            }
        }
    ]


# rh.MSDD 3000 s100 vr1a fpga load
dut_config['MSDD|3000|s100|vr1a'] = copy.deepcopy(dut_config['MSDD|3000'])
dut_config['MSDD|3000|s100|vr1a']['num_output_configs'] = 17
dut_config['MSDD|3000|s100|vr1a']['max_output_enabled'] = 17
dut_config['MSDD|3000|s100|vr1a']['capabilities'] = [
        {
            'RX_DIGITIZER': {
                'COMPLEX' : True,
                'CF'      : [30e6, 3e9],
                'BW'      : [20e6, 20e6],
                'SR'      : [25e6,25e6],
                'GAIN'    : [-48.0, 12.0]
            },
            'RX_DIGITIZER_CHANNELIZER': {
                'COMPLEX' : True,
                'CF'      : [30e6, 3e9],
                'BW'      : [20e6, 20e6],
                'SR'      : [25e6,25e6],
            },
            'DDC': {
                'COMPLEX' : True,
                'CF'      : [30e6, 3e9],
                'BW'      : [.781250e6, .781250e6],                  # fix these to single instance so secondary DDC can be allocated
                'SR'      :  [.781250e6,.781250e6],                 # fix these to single instance so secondary DDC can be allocated
                'GAIN'    : [-48.0, 12.0],
                'NUMDDCs' : 1,
                'RESTRICT' : True
            }
        },
        {
            'DDC': {
                'COMPLEX' : True,
                'CF'      : [30e6, 3e9],
                'BW'      : [1250000.0, 781250.0, 390625.0, 195312.5, 97656.25, 48828.125],
                'SR'      : [1250000.0, 781250.0, 390625.0, 195312.5, 97656.25, 48828.125],
                'GAIN'    : [-48.0, 12.0],
                'NUMDDCs' : 16,
                'RESTRICT' : True
            }
        }
    ]


# rh.MSDD_RX_Device
dut_config['MSDD|Dreamin'] = {
    'spd'         : None,
    'parent_dir'  : None,
    'namespace'   : 'rh',
    'name'        : 'MSDD_RX_Device_v2',
    'impl_id'     : 'DCE:3f0ac7cb-1601-456d-97fa-2dcc5be684ee',
    'execparams'  : {},
    'configure'   : {
        'NetworkConfiguration': {
             'status_destination_address'               : MCAST_GROUP or '234.0.0.100',
             'master_destination_address'               : MCAST_GROUP or '234.0.0.100',
             'DCE:d91f918d-5cdf-44d4-a2e1-6f4e5f3128bf' : MCAST_PORT or 8887,
             'master_destination_vlan'                  : MCAST_VLAN or 0,
             'msdd_address'                             : DUT_IP_ADDR or '192.168.100.250',
             'msdd_interface'                           : DUT_IFACE or 'em2',
             'msdd_port'                                : DUT_PORT or 8267
         }
    },
    'properties'  : {},
    'capabilities': [
        {
            'RX_DIGITIZER': {
                'COMPLEX' : True,
                'CF'      : [30e6, 3e9],
                'BW'      : [20e6, 20e6],
                'SR'      : [25e6, 25e6],
                'GAIN'    : [-60.0, 0.0]
            }
        }
    ]
}


mcast_start_addr = MCAST_GROUP or '234.0.0.100'
mcast_octets = [int(x) for x in mcast_start_addr.split('.')]
nconfigs=dut_config[DUT]['num_output_configs']
nenabled=dut_config[DUT]['max_output_enabled']
print dut_config[DUT]['num_output_configs'], dut_config[DUT]['max_output_enabled']

for i in xrange(1,nconfigs+1):
    oenable=False
    if i <= nenabled: oenable=True
    mcast_octets[-1] += 1
    dut_config[DUT]['configure']['msdd_output_configuration'].append(
            {
                'msdd_output_configuration::tuner_number'    : i,
                'msdd_output_configuration::protocol'        : 'UDP_SDDS',
                'msdd_output_configuration::ip_address'      : '.'.join(str(x) for x in mcast_octets),
                'msdd_output_configuration::port'            : MCAST_PORT or 0,
                'msdd_output_configuration::vlan'            : MCAST_VLAN or 0,
                'msdd_output_configuration::enabled'         : oenable,
                'msdd_output_configuration::timestamp_offset': 0,
                'msdd_output_configuration::endianess'       : 1,
                'msdd_output_configuration::mfp_flush'       : 63,
                'msdd_output_configuration::vlan_enable'     : False
            }
    )


########################################
#    END Pre-defined Configurations    #
########################################

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

# Find SPD if not specified
if not dut_config[DUT]['spd']:
    if dut_config[DUT]['parent_dir']:
        ns = ''
        if dut_config[DUT]['namespace']:
            ns = dut_config[DUT]['namespace']+'.'
        dut_config[DUT]['spd'] = os.path.join(dut_config[DUT]['parent_dir'], ns+dut_config[DUT]['name'], dut_config[DUT]['name']+'.spd.xml')
    else: # Assume it must be in SDR
        dut_config[DUT]['spd'] = os.path.join(sdr_devices, dut_config[DUT]['namespace'] or '', dut_config[DUT]['name'], dut_config[DUT]['name']+'.spd.xml')

# TODO - update to use `properties` instead of `configure` and `execparam`

class FrontendTunerTests(fe.FrontendTunerTests):
    
    def __init__(self,*args,**kwargs):
        import ossie.utils.testing
        super(FrontendTunerTests,self).__init__(*args,**kwargs)
        fe.set_debug_level(DEBUG_LEVEL)
        fe.set_device_info(dut_config[DUT])

    # Use functions below to add pre-/post-launch commands if your device has special startup requirements
    @classmethod
    def devicePreLaunch(cls):
        pass

    @classmethod
    def devicePostLaunch(cls):
        if 'USRP' == DUT[:4] and len(dut_config[DUT]['capabilities'])==0:
            device_channels = cls._query( ('device_channels',) )['device_channels']
            if DEBUG_LEVEL >= 4:
                from pprint import pprint as pp
                print 'device channel: '
                pp(device_channels)

            for chan in device_channels:
                if chan['device_channels::tuner_type'] != 'RX_DIGITIZER':
                    continue
                chan_capabilities = {
                    chan['device_channels::tuner_type']: {
                        'COMPLEX' : True,
                        'CF'      : [chan['device_channels::freq_min'], chan['device_channels::freq_max']],
                        'BW'      : [chan['device_channels::bandwidth_min'], chan['device_channels::bandwidth_max']],
                        'SR'      : [chan['device_channels::rate_min'], chan['device_channels::rate_max']],
                        'GAIN'    : [chan['device_channels::gain_min'], chan['device_channels::gain_max']]
                    }
                }
                dut_config[DUT]['capabilities'].append(chan_capabilities)

    # Use functions below to add pre-/post-release commands if your device has special shutdown requirements
    @classmethod
    def devicePreRelease(self):
        pass
    @classmethod
    def devicePostRelease(self):
        pass


if __name__ == '__main__':
    fe.set_debug_level(DEBUG_LEVEL)
    fe.set_device_info(dut_config[DUT])
    
    # run using nose
    import nose
    nose.main(defaultTest=__name__)
