#!/usr/bin/env python3
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
import pprint

#                     DUT_IFACE = None,
#                     OUTPUT_IFACE = None,

def  get_config( msdd_id,
                     spd_path='../MSDD.spd.xml',
                     DUT_IP_ADDR='192.168.1.2',
                     DUT_PORT='23',
                     OUTPUT_ADDR = "234.0.0.100",
                     OUTPUT_PORT = None,
                     OUTPUT_VLAN = None,
                     OUTPUT_IFACE=0,
                     PSD_ADDR="233.0.0.100"
                 ) :

    dut_config = {}

    # rh.MSDD
    dut_config['MSDD'] = {
        'spd'         :  spd_path,
        'namespace'   : 'rh',
        'name'        : 'MSDD',
        'impl_id'     : 'python',
        'receiver_identifier' : 'RCV:1',   # default receiver path to test
        'receiver_modules' : ['RCV:1' ],   # identifiers for each rcv->wbbc flows
        'additional_tuner_output' : [1] ,  # total number of additional tuner output configs
        'execparams'  : { },
        'configure'   : {
            'advanced' : {
                'advanced::enable_fft_channels' : False
            },

            'msdd': {
                'msdd::ip_address': DUT_IP_ADDR or '192.168.100.250',
                'msdd::port':     DUT_PORT,
                'msdd::timeout':     0.10
            },
            'tuner_output': [
                {   # initial wbddc config for all radio configurations
                    'tuner_output::tuner_number'    : 0,
                    'tuner_output::protocol'        : 'UDP_SDDS',
                    'tuner_output::ip_address'      : OUTPUT_ADDR or '234.0.0.100',
                    'tuner_output::port'            : OUTPUT_PORT or 0,
                    'tuner_output::vlan'            : OUTPUT_VLAN or 0,
                    'tuner_output::interface'       : OUTPUT_IFACE or 0,
                    'tuner_output::enabled'         : True,
                    'tuner_output::timestamp_offset': 0,
                    'tuner_output::endianess'       : 1,
                    'tuner_output::mfp_flush'       : 63,
                    'tuner_output::vlan_enable'     : False
                } # more added below
            ],

            'block_psd_output': [
                {
                    'block_psd_output::fft_channel_start'    : 0,
                    'block_psd_output::fft_channel_stop'    : 0,
                    'block_psd_output::protocol'        : 'UDP_SDDS',
                    'block_psd_output::ip_address'      : PSD_ADDR or '233.0.0.100',
                    'block_psd_output::port'            : OUTPUT_PORT or 0,
                    'block_psd_output::vlan'            : OUTPUT_VLAN or 0,
                    'block_psd_output::interface'       : OUTPUT_IFACE or 0,
                    'block_psd_output::enabled'         : False,
                    'block_psd_output::timestamp_offset': 0,
                    'block_psd_output::endianess'       : 1,
                    'block_psd_output::mfp_flush'       : 63,
                    'block_psd_output::vlan_enable'     : False
                } # more added below
            ]

        },
        'properties'  : {},

        # The below capabilities are based on an MSDD-6000 with a freq range from 30 - 6000 MHz. It also
        # assumes a 100 MSPS ADC clock which creates a max SR of 25 MSPS. For MSDD 3000 models the max
        # frequency would need to be adjusted to 3000 MHz. For models that have the 98 MSPS clock
        # (s98 FPGA loads) the max sample rate should be set to 24.576e6.
        'capabilities': {
            'RCV:1' : [
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
    }

    #
    # For tests of Channelizers/DDC pairs, the  output for WBDDC will be disabled
    # for those specific tests because of network utilization issues. ( See
    # fei_base/frontend_tuner_unit_test_base.py test decorators)
    # You can disable the output here but this will cause failures with
    # some frontend unit tests.
    #dut_config['MSDD|3000']['configure']['tuner_output'][0].update(
    #    { 'tuner_output::enabled': False }
    #)
    # to change a specific dictionary key
    # dut_config['MSDD|3000']['configure'].update({
    #         'advanced' : {
    #             'advanced::enable_fft_channels' : False
    #         }
    #        } )

    # to over write dictionary key
    # dut_config['MSDD|3000']['capabilties'] = [
    # {
    #     'RX_DIGITIZER': {
    #         'COMPLEX' : True,
    #         'CF'      : [30e6, 6e9],
    #         'BW'      : [20e6, 20e6],
    #         'SR'      : [24.576e6, 24.576e6],
    #         'GAIN'    : [-48.0, 12.0]
    #     }
    #     ]

    # rh.MSDD 6000
    dut_config['MSDD|6000'] = copy.deepcopy(dut_config['MSDD'])
    dut_config['MSDD|6000']['additional_tuner_outputs'] = [16]

    # rh.MSDD 6000 s100
    dut_config['MSDD|6000|s100'] = copy.deepcopy(dut_config['MSDD|6000'])

    # rh.MSDD 6000 s98
    dut_config['MSDD|6000|s98'] = copy.deepcopy(dut_config['MSDD|6000'])
    dut_config['MSDD|6000|s98']['capabilities']['RCV:1'] = [
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


    # rh.MSDD 6000 ex98 HS
    dut_config['MSDD|6000|ex98|32n512b40'] = copy.deepcopy(dut_config['MSDD|6000'])
    dut_config['MSDD|6000|ex98|32n512b40']['capabilities']['RCV:1'] = [
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
                    'BW'      : [42e3, 42e3],
                    'SR'      : [48e3, 48e3],
                    'GAIN'    : [-48.0, 12.0],
                    'NUMDDCs' : 16,
                }
            }
        ]


    # rh.MSDD 3000
    dut_config['MSDD|3000'] = copy.deepcopy(dut_config['MSDD'])

    dut_config['MSDD|3000']['capabilities']['RCV:1'] = [
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

    # rh 3000EX
    #MSDD|3000EX|ex98|1w0n0b0|1w0n0b0
    dut_config['MSDD|3000EX|ex98|1w0n0b0'] = copy.deepcopy(dut_config['MSDD|3000'])
    dut_config['MSDD|3000EX|ex98|1w0n0b0']['additional_tuner_outputs'] = []
    dut_config['MSDD|3000EX|ex98|1w0n0b0']['capabilities']['RCV:1'] = [
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
                'DDC': None
            }
        ]


    # rh.MSDD 3000 s98
    dut_config['MSDD|3000|s98|1w5n5b1300'] = copy.deepcopy(dut_config['MSDD|3000'])
    dut_config['MSDD|3000|s98|1w5n5b1300']['additional_tuner_outputs'] = [5]
    dut_config['MSDD|3000|s98|1w5n5b1300']['capabilities']['RCV:1'] = [
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
    dut_config['MSDD|3000|s100|1w1nvr1a'] = copy.deepcopy(dut_config['MSDD|3000'])
    dut_config['MSDD|3000|s100|1w1nvr1a']['additional_tuner_outputs'] = [1]
    dut_config['MSDD|3000|s100|1w1nvr1a']['capabilities']['RCV:1'] = [
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
                    'BW'      : [ 1.25e6, 1.25e6 ],
                    'SR'      : [ 1.5625e6, 1.5625e6 ],
                    'GAIN'    : [-48.0, 12.0],
                    'NUMDDCs' : 1,
                    'RESTRICT' : True
                }
            },
            {
                'DDC': {
                    'COMPLEX' : True,
                    'CF'      : [30e6, 3e9],
                    'BW'      : [5e6, 2.5e6, 1.25e6, 320.0e3],
                    'SR'      : [6.25e6, 3.125e6, 1.5625e6, 390.625e3 ],
                    'GAIN'    : [-48.0, 12.0],
                    'NUMDDCs' : 16,
                    'RESTRICT' : True
                }
            }
        ]


    # rh.MSDD 3000 s100 1w8n64b320
    dut_config['MSDD|3000|s100|1w8n64b320'] = copy.deepcopy(dut_config['MSDD|3000'])
    dut_config['MSDD|3000|s100|1w8n64b320']['additional_tuner_outputs'] = [8]
    dut_config['MSDD|3000|s100|1w8n64b320']['capabilities']['RCV:1'] = [
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
                    'BW'      : [320e3,320e3],
                    'SR'      : [390.625e3,390.625e3],
                    'GAIN'    : [-48.0, 12.0],
                    'NUMDDCs' : 5,
                }
            }
        ]



    # rh.MSDD 660
    dut_config['MSDD|0660D'] = copy.deepcopy(dut_config['MSDD'])
    dut_config['MSDD|0660D']['execparams']  = {'receiver_identifier': 'RCV:1' }
    dut_config['MSDD|0660D']['receiver_modules']= [ 'RCV:1', 'RCV:2']
    dut_config['MSDD|0660D']['additional_tuner_outputs'] = [ 1, 1]
    dut_config['MSDD|0660D']['configure']['tuner_output']=[
                {
                    'tuner_output::tuner_number'    : 0,
                    'tuner_output::protocol'        : 'UDP_SDDS',
                    'tuner_output::ip_address'      : OUTPUT_ADDR or '234.0.0.100',
                    'tuner_output::port'            : OUTPUT_PORT or 0,
                    'tuner_output::vlan'            : OUTPUT_VLAN or 0,
                    'tuner_output::interface'       : OUTPUT_IFACE or 0,
                    'tuner_output::enabled'         : True,
                    'tuner_output::timestamp_offset': 0,
                    'tuner_output::endianess'       : 1,
                    'tuner_output::mfp_flush'       : 63,
                    'tuner_output::vlan_enable'     : False
                },
                {
                    'tuner_output::tuner_number'    : 0,
                    'tuner_output::protocol'        : 'UDP_SDDS',
                    'tuner_output::ip_address'      : OUTPUT_ADDR or '234.0.0.101',
                    'tuner_output::port'            : OUTPUT_PORT or 0,
                    'tuner_output::vlan'            : OUTPUT_VLAN or 0,
                    'tuner_output::interface'       : OUTPUT_IFACE or 0,
                    'tuner_output::enabled'         : True,
                    'tuner_output::timestamp_offset': 0,
                    'tuner_output::endianess'       : 1,
                    'tuner_output::mfp_flush'       : 63,
                    'tuner_output::vlan_enable'     : False
                }
        ]


    dut_config['MSDD|0660D']['capabilities']['RCV:1'] = [
            {
                'RX_DIGITIZER': {
                    'COMPLEX' : True,
                    'CF'      : [30e6, 6e9],
                    'BW'      : [20e6, 20e6],
                    'SR'      : [25e6,25e6],
                    'GAIN'    : [-45.0, 15.0]
                },
                'RX_DIGITIZER_CHANNELIZER': {
                    'COMPLEX' : True,
                    'CF'      : [30e6, 6e9],
                    'BW'      : [20e6, 20e6],
                    'SR'      : [25e6,25e6],
                },
                'DDC': None,
            }
    ]
    dut_config['MSDD|0660D']['capabilities']['RCV:2'] = [
            {
                'RX_DIGITIZER': {
                    'COMPLEX' : True,
                    'CF'      : [30e6, 6e9],
                    'BW'      : [20e6, 20e6],
                    'SR'      : [25e6,25e6],
                    'GAIN'    : [-45.0, 15.0]
                },
                'RX_DIGITIZER_CHANNELIZER': {
                    'COMPLEX' : True,
                    'CF'      : [30e6, 6e9],
                    'BW'      : [20e6, 20e6],
                    'SR'      : [25e6,25e6],
                },
                'DDC': None,
            }
        ]

    # rh.MSDD 0660 d100
    dut_config['MSDD|0660D|d100'] = copy.deepcopy(dut_config['MSDD|0660D'])

    dut_config['MSDD|0660D|d100|2w8n64b320'] = copy.deepcopy(dut_config['MSDD|0660D'])
    dut_config['MSDD|0660D|d100|2w8n64b320']['additional_tuner_outputs'] = [ 4, 0 ]
    dut_config['MSDD|0660D|d100|2w8n64b320']['capabilities']['RCV:1'] = [
            {   # wideband ddc 0
                'RX_DIGITIZER': {
                    'COMPLEX' : True,
                    'CF'      : [30e6, 6e9],
                    'BW'      : [20e6, 20e6],
                    'SR'      : [25e6,25e6],
                    'GAIN'    : [-45.0, 15.0]

                },
                'RX_DIGITIZER_CHANNELIZER': {
                    'COMPLEX' : True,
                    'CF'      : [30e6, 6e9],
                    'BW'      : [20e6, 20e6],
                    'SR'      : [25e6,25e6],
                },
                'DDC': {
                    'COMPLEX' : True,
                    'CF'      : [30e6, 6e9],
                    'BW'      : [320e3, 320e3],
                    'SR'      : [390.625e3, 390.625e3],
                    'GAIN'    : [-17.0, 31.0 ],
                    'RESTRICT' : True,
                    'NUMDDCs' : 4,
                }
            }
    ]
    dut_config['MSDD|0660D|d100|2w8n64b320']['capabilities']['RCV:2'] = [
            { # wideband ddc 1
                'RX_DIGITIZER': {
                    'COMPLEX' : True,
                    'CF'      : [30e6, 6e9],
                    'BW'      : [20e6, 20e6],
                    'SR'      : [25e6,25e6],
                    'GAIN'    : [-45.0, 15.0]
                },
                'RX_DIGITIZER_CHANNELIZER': {
                    'COMPLEX' : True,
                    'CF'      : [30e6, 6e9],
                    'BW'      : [20e6, 20e6],
                    'SR'      : [25e6,25e6],
                },
                'DDC': None,
            }
    ]




    # # rh.MSDD_RX_Device
    # dut_config['MSDD|Dreamin'] = {
    #     'spd'         : None,
    #     'parent_dir'  : None,
    #     'namespace'   : 'rh',
    #     'name'        : 'MSDD_RX_Device_v2',
    #     'impl_id'     : 'DCE:3f0ac7cb-1601-456d-97fa-2dcc5be684ee',
    #     'execparams'  : {},
    #     'configure'   : {
    #         'NetworkConfiguration': {
    #              'status_destination_address'               : OUTPUT_ADDR or '234.0.0.100',
    #              'master_destination_address'               : OUTPUT_ADDR or '234.0.0.100',
    #              'DCE:d91f918d-5cdf-44d4-a2e1-6f4e5f3128bf' : OUTPUT_PORT or 8887,
    #              'master_destination_vlan'                  : OUTPUT_VLAN or 0,
    #              'msdd_address'                             : DUT_IP_ADDR or '192.168.100.250',
    #              'msdd_interface'                           : DUT_IFACE or 'em2',
    #              'msdd_port'                                : DUT_PORT or 8267
    #          }
    #     },
    #     'properties'  : {},
    #     'capabilities': [
    #         {
    #             'RX_DIGITIZER': {
    #                 'COMPLEX' : True,
    #                 'CF'      : [30e6, 3e9],
    #                 'BW'      : [20e6, 20e6],
    #                 'SR'      : [25e6, 25e6],
    #                 'GAIN'    : [-60.0, 0.0]
    #             }
    #         }
    #     ]
    # }

    if msdd_id in list(dut_config.keys()):
        dut_cfg = copy.deepcopy(dut_config[msdd_id])
        configure_output_channels(dut_cfg, OUTPUT_ADDR, OUTPUT_PORT, OUTPUT_VLAN, OUTPUT_IFACE )

        return dut_cfg
    else:
        return None


def configure_output_channels( dut_config, OUTPUT_ADDR, OUTPUT_PORT, OUTPUT_VLAN, OUTPUT_IFACE ):
    mcast_start_addr = OUTPUT_ADDR or '234.0.0.100'
    mcast_octets = [int(x) for x in mcast_start_addr.split('.')]
    if len(dut_config['receiver_modules']) > 1:
        mcast_octets[-1] += 1
    # create number of configs for each wbddc 
    rcv_idx=0
    for ncfgs in dut_config['additional_tuner_outputs']:
        for i in range(1, ncfgs+1):
            mcast_octets[-1] += 1
            dut_config['configure']['tuner_output'].append(
                {
                    'tuner_output::tuner_number'    : i,
                    'tuner_output::protocol'        : 'UDP_SDDS',
                    'tuner_output::ip_address'      : '.'.join(str(x) for x in mcast_octets),
                    'tuner_output::port'            : OUTPUT_PORT or 0,
                    'tuner_output::vlan'            : OUTPUT_VLAN or 0,
                    'tuner_output::interface'       : OUTPUT_IFACE or 0,
                    'tuner_output::enabled'         : True,
                    'tuner_output::timestamp_offset': 0,
                    'tuner_output::endianess'       : 1,
                    'tuner_output::mfp_flush'       : 63,
                    'tuner_output::vlan_enable'     : False
                }
        )
