#!/usr/bin/env python
#
# AUTO-GENERATED CODE.  DO NOT MODIFY!
#
# Source: MSDD_Controller.spd.xml
from ossie.cf import CF
from ossie.cf import CF__POA
from ossie.utils import uuid

# require to fix framework base class issues when setting properties
from fix_cf import *

from ossie.device import Device
from ossie.device import AggregateDevice
from ossie.threadedcomponent import *
from ossie.properties import simple_property
from ossie.properties import simpleseq_property
from ossie.properties import struct_property
from ossie.properties import structseq_property

import Queue, copy, time, threading

class enums:
    # Enumerated values for msdd
    class msdd:
        # Enumerated values for msdd::clock_ref
        class clock_ref:
            INTERNAL = 0
            EXTERNAL = 10

    # Enumerated values for advanced
    class advanced:
        # Enumerated values for advanced::rcvr_mode
        class rcvr_mode:
            Not_allocatable = 0
            RX_Only = 8
    
        # Enumerated values for advanced::wb_ddc_mode
        class wb_ddc_mode:
            Not_allocatable = 0
            RX_DIGITIZER_CHANNELIZER_Only = 2
            RX_DIGITIZER_Only = 4
            RX_DIGITIZER_or_RX_DIGITIZER_CHANNELIZER = 6
    
        # Enumerated values for advanced::hw_ddc_mode
        class hw_ddc_mode:
            Not_allocatable = 0
            DDC_Only = 1
            RX_DIGITIZER_Only = 4
            RX_DIGITIZER_or_DDC = 5

    # Enumerated values for psd_configuration
    class psd_configuration:
        # Enumerated values for psd_configuration::window_type
        class window_type:
            HANNING = "HANNING"
            HAMMING = "HAMMING"
            BLACKMAN = "BLACKMAN"
            RECT = "RECT"
    
        # Enumerated values for psd_configuration::peak_mode
        class peak_mode:
            INST = "INST"
            PEAK = "PEAK"
            BOTH = "BOTH"

    # Enumerated values for time_of_day
    class time_of_day:
        # Enumerated values for time_of_day::mode
        class mode:
            SIMULATED = "SIM"
            ONE_PPS = "ONEPPS"
            IRIG = "IRIGB"
            NAV__IF_AVAILABLE_ = "NAV"
            TFN__IF_AVAILABLE_ = "TFN"
    
        # Enumerated values for time_of_day::reference_track
        class reference_track:
            OFF = 0
            CALIBRATE = 1
            TRACK = 2
    
        # Enumerated values for time_of_day::toy
        class toy:
            _24_Hour_offset_from_IRIGB = 0
            Direct_from_IRIGB = 1

    # Enumerated values for tuner_output_definition
    class tuner_output_definition:
        # Enumerated values for tuner_output::protocol
        class protocol:
            SDDS = "UDP_SDDS"
            SDDSX = "UDP_SDDSX"
            SDDSA = "UDP_SDDSA"
            RAW = "UDP_RAW"
            VITA49 = "UDP_VITA49"
    
        # Enumerated values for tuner_output::endianess
        class endianess:
            BIG_ENDIAN = 0
            LITTLE_ENDIAN = 1

    # Enumerated values for block_tuner_output_definition
    class block_tuner_output_definition:
        # Enumerated values for block_tuner_output::protocol
        class protocol:
            SDDS = "UDP_SDDS"
            SDDSX = "UDP_SDDSX"
            SDDSA = "UDP_SDDSA"
            RAW = "UDP_RAW"
            VITA49 = "UDP_VITA49"
    
        # Enumerated values for block_tuner_output::endianess
        class endianess:
            BIG_ENDIAN = 0
            LITTLE_ENDIAN = 1

    # Enumerated values for block_psd_output_definition
    class block_psd_output_definition:
        # Enumerated values for block_psd_output::protocol
        class protocol:
            SDDS = "UDP_SDDS"
            SDDSX = "UDP_SDDSX"
            SDDSA = "UDP_SDDSA"
            RAW = "UDP_RAW"
            VITA49 = "UDP_VITA49"
    
        # Enumerated values for block_psd_output::endianess
        class endianess:
            BIG_ENDIAN = 0
            LITTLE_ENDIAN = 1

class MSDD_Controller_base(CF__POA.AggregatePlainDevice, Device, AggregateDevice, ThreadedComponent):
        # These values can be altered in the __init__ of your derived class

        PAUSE = 0.0125 # The amount of time to sleep if process return NOOP
        TIMEOUT = 5.0 # The amount of time to wait for the process thread to die when stop() is called
        DEFAULT_QUEUE_SIZE = 100 # The number of BulkIO packets that can be in the queue before pushPacket will block

        def __init__(self, devmgr, uuid, label, softwareProfile, compositeDevice, execparams):
            Device.__init__(self, devmgr, uuid, label, softwareProfile, compositeDevice, execparams)
            AggregateDevice.__init__(self)
            ThreadedComponent.__init__(self)

            # self.auto_start is deprecated and is only kept for API compatibility
            # with 1.7.X and 1.8.0 devices.  This variable may be removed
            # in future releases
            self.auto_start = False
            # Instantiate the default implementations for all ports on this device

        def start(self):
            Device.start(self)
            ThreadedComponent.startThread(self, pause=self.PAUSE)

        def stop(self):
            Device.stop(self)
            if not ThreadedComponent.stopThread(self, self.TIMEOUT):
                raise CF.Resource.StopError(CF.CF_NOTSET, "Processing thread did not die")

        def releaseObject(self):
            try:
                self.stop()
            except Exception:
                self._baseLog.exception("Error stopping")
            Device.releaseObject(self)

        ######################################################################
        # PORTS
        # 
        # DO NOT ADD NEW PORTS HERE.  You can add ports in your derived class, in the SCD xml file, 
        # or via the IDE.

        ######################################################################
        # PROPERTIES
        # 
        # DO NOT ADD NEW PROPERTIES HERE.  You can add properties in your derived class, in the PRF xml file
        # or by using the IDE.
        device_kind = simple_property(id_="DCE:cdc5ee18-7ceb-4ae6-bf4c-31f983179b4d",
                                      name="device_kind",
                                      type_="string",
                                      defvalue="MSDD Controller",
                                      mode="readonly",
                                      action="eq",
                                      kinds=("allocation",),
                                      description="""This specifies the device kind""")


        device_model = simple_property(id_="DCE:0f99b2e4-9903-4631-9846-ff349d18ecfb",
                                       name="device_model",
                                       type_="string",
                                       defvalue="MSDD X000/6XX Controller",
                                       mode="readonly",
                                       action="eq",
                                       kinds=("allocation",),
                                       description=""" This specifies the specific device""")


        class Msdd(object):
            ip_address = simple_property(
                                         id_="msdd::ip_address",
                                         
                                         name="ip_address",
                                         type_="string",
                                         defvalue="127.0.0.1"
                                         )
        
            port = simple_property(
                                   id_="msdd::port",
                                   
                                   name="port",
                                   type_="string",
                                   defvalue="23"
                                   )
        
            timeout = simple_property(
                                      id_="msdd::timeout",
                                      
                                      name="timeout",
                                      type_="double",
                                      defvalue=0.2
                                      )
        
            clock_ref = simple_property(
                                        id_="msdd::clock_ref",
                                        
                                        name="clock_ref",
                                        type_="short",
                                        defvalue=10
                                        )
        
            def __init__(self, **kw):
                """Construct an initialized instance of this struct definition"""
                for classattr in type(self).__dict__.itervalues():
                    if isinstance(classattr, (simple_property, simpleseq_property)):
                        classattr.initialize(self)
                for k,v in kw.items():
                    setattr(self,k,v)
        
            def __str__(self):
                """Return a string representation of this structure"""
                d = {}
                d["ip_address"] = self.ip_address
                d["port"] = self.port
                d["timeout"] = self.timeout
                d["clock_ref"] = self.clock_ref
                return str(d)
        
            @classmethod
            def getId(cls):
                return "msdd"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("ip_address",self.ip_address),("port",self.port),("timeout",self.timeout),("clock_ref",self.clock_ref)]

        msdd = struct_property(id_="msdd",
                               structdef=Msdd,
                               configurationkind=("property",),
                               mode="readonly")


        class Advanced(object):
            rcvr_mode = simple_property(
                                        id_="advanced::rcvr_mode",
                                        
                                        name="rcvr_mode",
                                        type_="short",
                                        defvalue=0
                                        )
        
            wb_ddc_mode = simple_property(
                                          id_="advanced::wb_ddc_mode",
                                          
                                          name="wb_ddc_mode",
                                          type_="short",
                                          defvalue=6
                                          )
        
            hw_ddc_mode = simple_property(
                                          id_="advanced::hw_ddc_mode",
                                          
                                          name="hw_ddc_mode",
                                          type_="short",
                                          defvalue=5
                                          )
        
            enable_inline_swddc = simple_property(
                                                  id_="advanced::enable_inline_swddc",
                                                  
                                                  name="enable_inline_swddc",
                                                  type_="boolean",
                                                  defvalue=False
                                                  )
        
            enable_fft_channels = simple_property(
                                                  id_="advanced::enable_fft_channels",
                                                  
                                                  name="enable_fft_channels",
                                                  type_="boolean",
                                                  defvalue=False
                                                  )
        
            max_cpu_load = simple_property(
                                           id_="advanced::max_cpu_load",
                                           
                                           name="max_cpu_load",
                                           type_="float",
                                           defvalue=95.0
                                           )
        
            max_nic_percentage = simple_property(
                                                 id_="advanced::max_nic_percentage",
                                                 
                                                 name="max_nic_percentage",
                                                 type_="float",
                                                 defvalue=90.0
                                                 )
        
            minimum_connected_nic_rate = simple_property(
                                                         id_="advanced::minimum_connected_nic_rate",
                                                         
                                                         name="minimum_connected_nic_rate",
                                                         type_="float",
                                                         defvalue=1000.0
                                                         )
        
            def __init__(self, **kw):
                """Construct an initialized instance of this struct definition"""
                for classattr in type(self).__dict__.itervalues():
                    if isinstance(classattr, (simple_property, simpleseq_property)):
                        classattr.initialize(self)
                for k,v in kw.items():
                    setattr(self,k,v)
        
            def __str__(self):
                """Return a string representation of this structure"""
                d = {}
                d["rcvr_mode"] = self.rcvr_mode
                d["wb_ddc_mode"] = self.wb_ddc_mode
                d["hw_ddc_mode"] = self.hw_ddc_mode
                d["enable_inline_swddc"] = self.enable_inline_swddc
                d["enable_fft_channels"] = self.enable_fft_channels
                d["max_cpu_load"] = self.max_cpu_load
                d["max_nic_percentage"] = self.max_nic_percentage
                d["minimum_connected_nic_rate"] = self.minimum_connected_nic_rate
                return str(d)
        
            @classmethod
            def getId(cls):
                return "advanced"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("rcvr_mode",self.rcvr_mode),("wb_ddc_mode",self.wb_ddc_mode),("hw_ddc_mode",self.hw_ddc_mode),("enable_inline_swddc",self.enable_inline_swddc),("enable_fft_channels",self.enable_fft_channels),("max_cpu_load",self.max_cpu_load),("max_nic_percentage",self.max_nic_percentage),("minimum_connected_nic_rate",self.minimum_connected_nic_rate)]

        advanced = struct_property(id_="advanced",
                                   name="advanced",
                                   structdef=Advanced,
                                   configurationkind=("property",),
                                   mode="readonly")


        class PsdConfiguration(object):
            fft_size = simple_property(
                                       id_="psd_configuration::fft_size",
                                       
                                       name="fft_size",
                                       type_="double",
                                       defvalue=32768.0
                                       )
        
            time_average = simple_property(
                                           id_="psd_configuration::time_average",
                                           
                                           name="time_average",
                                           type_="double",
                                           defvalue=0.1
                                           )
        
            time_between_ffts = simple_property(
                                                id_="psd_configuration::time_between_ffts",
                                                
                                                name="time_between_ffts",
                                                type_="double",
                                                defvalue=0.0
                                                )
        
            output_bin_size = simple_property(
                                              id_="psd_configuration::output_bin_size",
                                              
                                              name="output_bin_size",
                                              type_="double",
                                              defvalue=0.0
                                              )
        
            window_type = simple_property(
                                          id_="psd_configuration::window_type",
                                          
                                          name="window_type",
                                          type_="string",
                                          defvalue="HAMMING"
                                          )
        
            peak_mode = simple_property(
                                        id_="psd_configuration::peak_mode",
                                        
                                        name="peak_mode",
                                        type_="string",
                                        defvalue="INST"
                                        )
        
            def __init__(self, **kw):
                """Construct an initialized instance of this struct definition"""
                for classattr in type(self).__dict__.itervalues():
                    if isinstance(classattr, (simple_property, simpleseq_property)):
                        classattr.initialize(self)
                for k,v in kw.items():
                    setattr(self,k,v)
        
            def __str__(self):
                """Return a string representation of this structure"""
                d = {}
                d["fft_size"] = self.fft_size
                d["time_average"] = self.time_average
                d["time_between_ffts"] = self.time_between_ffts
                d["output_bin_size"] = self.output_bin_size
                d["window_type"] = self.window_type
                d["peak_mode"] = self.peak_mode
                return str(d)
        
            @classmethod
            def getId(cls):
                return "psd_configuration"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("fft_size",self.fft_size),("time_average",self.time_average),("time_between_ffts",self.time_between_ffts),("output_bin_size",self.output_bin_size),("window_type",self.window_type),("peak_mode",self.peak_mode)]

        psd_configuration = struct_property(id_="psd_configuration",
                                            structdef=PsdConfiguration,
                                            configurationkind=("property",),
                                            mode="readwrite",
                                            description="""This is global across all FFT channels. Changes to this property are applied to the next time a FFT channel is used.""")


        class GainConfiguration(object):
            rcvr_gain = simple_property(
                                        id_="gain_configuration::rcvr_gain",
                                        
                                        name="rcvr_gain",
                                        type_="float",
                                        defvalue=0.0
                                        )
        
            wb_ddc_gain = simple_property(
                                          id_="gain_configuration::wb_ddc_gain",
                                          
                                          name="wb_ddc_gain",
                                          type_="float",
                                          defvalue=0.0
                                          )
        
            hw_ddc_gain = simple_property(
                                          id_="gain_configuration::hw_ddc_gain",
                                          
                                          name="hw_ddc_gain",
                                          type_="float",
                                          defvalue=0.0
                                          )
        
            def __init__(self, **kw):
                """Construct an initialized instance of this struct definition"""
                for classattr in type(self).__dict__.itervalues():
                    if isinstance(classattr, (simple_property, simpleseq_property)):
                        classattr.initialize(self)
                for k,v in kw.items():
                    setattr(self,k,v)
        
            def __str__(self):
                """Return a string representation of this structure"""
                d = {}
                d["rcvr_gain"] = self.rcvr_gain
                d["wb_ddc_gain"] = self.wb_ddc_gain
                d["hw_ddc_gain"] = self.hw_ddc_gain
                return str(d)
        
            @classmethod
            def getId(cls):
                return "gain_configuration"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("rcvr_gain",self.rcvr_gain),("wb_ddc_gain",self.wb_ddc_gain),("hw_ddc_gain",self.hw_ddc_gain)]

        gain_configuration = struct_property(id_="gain_configuration",
                                             structdef=GainConfiguration,
                                             configurationkind=("property",),
                                             mode="readwrite",
                                             description="""Applies the gain settings to the appropriate module.  Changes to these setting will affect the next time tuner channel is allocated.""")


        class MsddStatus(object):
            connected = simple_property(
                                        id_="msdd_status::connected",
                                        
                                        name="connected",
                                        type_="boolean")
        
            ip_address = simple_property(
                                         id_="msdd_status::ip_address",
                                         
                                         name="ip_address",
                                         type_="string")
        
            port = simple_property(
                                   id_="msdd_status::port",
                                   
                                   name="port",
                                   type_="string")
        
            control_host = simple_property(
                                           id_="msdd_status::control_host",
                                           
                                           name="control_host",
                                           type_="string")
        
            control_host_port = simple_property(
                                                id_="msdd_status::control_host_port",
                                                
                                                name="control_host_port",
                                                type_="string")
        
            model = simple_property(
                                    id_="msdd_status::model",
                                    
                                    name="model",
                                    type_="string")
        
            serial = simple_property(
                                     id_="msdd_status::serial",
                                     
                                     name="serial",
                                     type_="string")
        
            software_part_number = simple_property(
                                                   id_="msdd_status::software_part_number",
                                                   
                                                   name="software_part_number",
                                                   type_="string")
        
            rf_board_type = simple_property(
                                            id_="msdd_status::rf_board_type",
                                            
                                            name="rf_board_type",
                                            type_="string")
        
            fpga_type = simple_property(
                                        id_="msdd_status::fpga_type",
                                        
                                        name="fpga_type",
                                        type_="string")
        
            dsp_type = simple_property(
                                       id_="msdd_status::dsp_type",
                                       
                                       name="dsp_type",
                                       type_="string")
        
            minimum_frequency_hz = simple_property(
                                                   id_="msdd_status::minimum_frequency_hz",
                                                   
                                                   name="minimum_frequency_hz",
                                                   type_="string")
        
            maximum_frequency_hz = simple_property(
                                                   id_="msdd_status::maximum_frequency_hz",
                                                   
                                                   name="maximum_frequency_hz",
                                                   type_="string")
        
            dsp_reference_frequency_hz = simple_property(
                                                         id_="msdd_status::dsp_reference_frequency_hz",
                                                         
                                                         name="dsp_reference_frequency_hz",
                                                         type_="string")
        
            adc_clock_frequency_hz = simple_property(
                                                     id_="msdd_status::adc_clock_frequency_hz",
                                                     
                                                     name="adc_clock_frequency_hz",
                                                     type_="string")
        
            num_if_ports = simple_property(
                                           id_="msdd_status::num_if_ports",
                                           
                                           name="num_if_ports",
                                           type_="string")
        
            num_eth_ports = simple_property(
                                            id_="msdd_status::num_eth_ports",
                                            
                                            name="num_eth_ports",
                                            type_="string")
        
            cpu_type = simple_property(
                                       id_="msdd_status::cpu_type",
                                       
                                       name="cpu_type",
                                       type_="string")
        
            cpu_rate = simple_property(
                                       id_="msdd_status::cpu_rate",
                                       
                                       name="cpu_rate",
                                       type_="string")
        
            cpu_load = simple_property(
                                       id_="msdd_status::cpu_load",
                                       
                                       name="cpu_load",
                                       type_="string")
        
            pps_termination = simple_property(
                                              id_="msdd_status::pps_termination",
                                              
                                              name="pps_termination",
                                              type_="string")
        
            pps_voltage = simple_property(
                                          id_="msdd_status::pps_voltage",
                                          
                                          name="pps_voltage",
                                          type_="string")
        
            number_wb_ddc_channels = simple_property(
                                                     id_="msdd_status::number_wb_ddc_channels",
                                                     
                                                     name="number_wb_ddc_channels",
                                                     type_="string")
        
            number_nb_ddc_channels = simple_property(
                                                     id_="msdd_status::number_nb_ddc_channels",
                                                     
                                                     name="number_nb_ddc_channels",
                                                     type_="string")
        
            filename_app = simple_property(
                                           id_="msdd_status::filename_app",
                                           
                                           name="filename_app",
                                           type_="string")
        
            filename_fpga = simple_property(
                                            id_="msdd_status::filename_fpga",
                                            
                                            name="filename_fpga",
                                            type_="string")
        
            filename_batch = simple_property(
                                             id_="msdd_status::filename_batch",
                                             
                                             name="filename_batch",
                                             type_="string")
        
            filename_boot = simple_property(
                                            id_="msdd_status::filename_boot",
                                            
                                            name="filename_boot",
                                            type_="string")
        
            filename_loader = simple_property(
                                              id_="msdd_status::filename_loader",
                                              
                                              name="filename_loader",
                                              type_="string")
        
            filename_config = simple_property(
                                              id_="msdd_status::filename_config",
                                              
                                              name="filename_config",
                                              type_="string")
        
            tod_module = simple_property(
                                         id_="msdd_status::tod_module",
                                         
                                         name="tod_module",
                                         type_="string")
        
            tod_available_module = simple_property(
                                                   id_="msdd_status::tod_available_module",
                                                   
                                                   name="tod_available_module",
                                                   type_="string")
        
            tod_meter_list = simple_property(
                                             id_="msdd_status::tod_meter_list",
                                             
                                             name="tod_meter_list",
                                             type_="string")
        
            tod_tod_reference_adjust = simple_property(
                                                       id_="msdd_status::tod_reference_adjust",
                                                       
                                                       name="tod_tod_reference_adjust",
                                                       type_="string")
        
            tod_track_mode_state = simple_property(
                                                   id_="msdd_status::tod_track_mode_state",
                                                   
                                                   name="tod_track_mode_state",
                                                   type_="string")
        
            tod_bit_state = simple_property(
                                            id_="msdd_status::tod_bit_state",
                                            
                                            name="tod_bit_state",
                                            type_="string")
        
            tod_toy = simple_property(
                                      id_="msdd_status::tod_toy",
                                      
                                      name="tod_toy",
                                      type_="string")
        
            tod_host_delta = simple_property(
                                             id_="msdd_status::tod_host_delta",
                                             
                                             name="tod_host_delta",
                                             type_="double")
        
            ntp_running = simple_property(
                                          id_="msdd_status::ntp_running",
                                          
                                          name="ntp_running",
                                          type_="boolean")
        
            def __init__(self, **kw):
                """Construct an initialized instance of this struct definition"""
                for classattr in type(self).__dict__.itervalues():
                    if isinstance(classattr, (simple_property, simpleseq_property)):
                        classattr.initialize(self)
                for k,v in kw.items():
                    setattr(self,k,v)
        
            def __str__(self):
                """Return a string representation of this structure"""
                d = {}
                d["connected"] = self.connected
                d["ip_address"] = self.ip_address
                d["port"] = self.port
                d["control_host"] = self.control_host
                d["control_host_port"] = self.control_host_port
                d["model"] = self.model
                d["serial"] = self.serial
                d["software_part_number"] = self.software_part_number
                d["rf_board_type"] = self.rf_board_type
                d["fpga_type"] = self.fpga_type
                d["dsp_type"] = self.dsp_type
                d["minimum_frequency_hz"] = self.minimum_frequency_hz
                d["maximum_frequency_hz"] = self.maximum_frequency_hz
                d["dsp_reference_frequency_hz"] = self.dsp_reference_frequency_hz
                d["adc_clock_frequency_hz"] = self.adc_clock_frequency_hz
                d["num_if_ports"] = self.num_if_ports
                d["num_eth_ports"] = self.num_eth_ports
                d["cpu_type"] = self.cpu_type
                d["cpu_rate"] = self.cpu_rate
                d["cpu_load"] = self.cpu_load
                d["pps_termination"] = self.pps_termination
                d["pps_voltage"] = self.pps_voltage
                d["number_wb_ddc_channels"] = self.number_wb_ddc_channels
                d["number_nb_ddc_channels"] = self.number_nb_ddc_channels
                d["filename_app"] = self.filename_app
                d["filename_fpga"] = self.filename_fpga
                d["filename_batch"] = self.filename_batch
                d["filename_boot"] = self.filename_boot
                d["filename_loader"] = self.filename_loader
                d["filename_config"] = self.filename_config
                d["tod_module"] = self.tod_module
                d["tod_available_module"] = self.tod_available_module
                d["tod_meter_list"] = self.tod_meter_list
                d["tod_tod_reference_adjust"] = self.tod_tod_reference_adjust
                d["tod_track_mode_state"] = self.tod_track_mode_state
                d["tod_bit_state"] = self.tod_bit_state
                d["tod_toy"] = self.tod_toy
                d["tod_host_delta"] = self.tod_host_delta
                d["ntp_running"] = self.ntp_running
                return str(d)
        
            @classmethod
            def getId(cls):
                return "msdd_status"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("connected",self.connected),("ip_address",self.ip_address),("port",self.port),("control_host",self.control_host),("control_host_port",self.control_host_port),("model",self.model),("serial",self.serial),("software_part_number",self.software_part_number),("rf_board_type",self.rf_board_type),("fpga_type",self.fpga_type),("dsp_type",self.dsp_type),("minimum_frequency_hz",self.minimum_frequency_hz),("maximum_frequency_hz",self.maximum_frequency_hz),("dsp_reference_frequency_hz",self.dsp_reference_frequency_hz),("adc_clock_frequency_hz",self.adc_clock_frequency_hz),("num_if_ports",self.num_if_ports),("num_eth_ports",self.num_eth_ports),("cpu_type",self.cpu_type),("cpu_rate",self.cpu_rate),("cpu_load",self.cpu_load),("pps_termination",self.pps_termination),("pps_voltage",self.pps_voltage),("number_wb_ddc_channels",self.number_wb_ddc_channels),("number_nb_ddc_channels",self.number_nb_ddc_channels),("filename_app",self.filename_app),("filename_fpga",self.filename_fpga),("filename_batch",self.filename_batch),("filename_boot",self.filename_boot),("filename_loader",self.filename_loader),("filename_config",self.filename_config),("tod_module",self.tod_module),("tod_available_module",self.tod_available_module),("tod_meter_list",self.tod_meter_list),("tod_tod_reference_adjust",self.tod_tod_reference_adjust),("tod_track_mode_state",self.tod_track_mode_state),("tod_bit_state",self.tod_bit_state),("tod_toy",self.tod_toy),("tod_host_delta",self.tod_host_delta),("ntp_running",self.ntp_running)]

        msdd_status = struct_property(id_="msdd_status",
                                      name="msdd_status",
                                      structdef=MsddStatus,
                                      configurationkind=("property",),
                                      mode="readonly")


        class TimeOfDay(object):
            mode = simple_property(
                                   id_="time_of_day::mode",
                                   
                                   name="mode",
                                   type_="string",
                                   defvalue="SIM"
                                   )
        
            reference_adjust = simple_property(
                                               id_="time_of_day::reference_adjust",
                                               
                                               name="reference_adjust",
                                               type_="short",
                                               defvalue=0
                                               )
        
            reference_track = simple_property(
                                              id_="time_of_day::reference_track",
                                              
                                              name="reference_track",
                                              type_="short",
                                              defvalue=0
                                              )
        
            toy = simple_property(
                                  id_="time_of_day::toy",
                                  
                                  name="toy",
                                  type_="short",
                                  defvalue=0
                                  )
        
            def __init__(self, **kw):
                """Construct an initialized instance of this struct definition"""
                for classattr in type(self).__dict__.itervalues():
                    if isinstance(classattr, (simple_property, simpleseq_property)):
                        classattr.initialize(self)
                for k,v in kw.items():
                    setattr(self,k,v)
        
            def __str__(self):
                """Return a string representation of this structure"""
                d = {}
                d["mode"] = self.mode
                d["reference_adjust"] = self.reference_adjust
                d["reference_track"] = self.reference_track
                d["toy"] = self.toy
                return str(d)
        
            @classmethod
            def getId(cls):
                return "time_of_day"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("mode",self.mode),("reference_adjust",self.reference_adjust),("reference_track",self.reference_track),("toy",self.toy)]

        time_of_day = struct_property(id_="time_of_day",
                                      name="time_of_day",
                                      structdef=TimeOfDay,
                                      configurationkind=("property",),
                                      mode="readonly")


        class TunerOutputDefinition(object):
            receiver_identifier = simple_property(
                                                  id_="tuner_output::receiver_identifier",
                                                  
                                                  name="receiver_identifier",
                                                  type_="string",
                                                  defvalue="RCV:1"
                                                  )
        
            tuner_number = simple_property(
                                           id_="tuner_output::tuner_number",
                                           
                                           name="tuner_number",
                                           type_="short",
                                           defvalue=0
                                           )
        
            protocol = simple_property(
                                       id_="tuner_output::protocol",
                                       
                                       name="protocol",
                                       type_="string",
                                       defvalue="UDP_SDDS"
                                       )
        
            interface = simple_property(
                                        id_="tuner_output::interface",
                                        
                                        name="interface",
                                        type_="short",
                                        defvalue=0
                                        )
        
            ip_address = simple_property(
                                         id_="tuner_output::ip_address",
                                         
                                         name="ip_address",
                                         type_="string",
                                         defvalue=""
                                         )
        
            port = simple_property(
                                   id_="tuner_output::port",
                                   
                                   name="port",
                                   type_="short",
                                   defvalue=8800
                                   )
        
            vlan = simple_property(
                                   id_="tuner_output::vlan",
                                   
                                   name="vlan",
                                   type_="short",
                                   defvalue=-1
                                   )
        
            enabled = simple_property(
                                      id_="tuner_output::enabled",
                                      
                                      name="enabled",
                                      type_="boolean",
                                      defvalue=True
                                      )
        
            timestamp_offset = simple_property(
                                               id_="tuner_output::timestamp_offset",
                                               
                                               name="timestamp_offset",
                                               type_="short",
                                               defvalue=0
                                               )
        
            endianess = simple_property(
                                        id_="tuner_output::endianess",
                                        
                                        name="endianess",
                                        type_="short",
                                        defvalue=1
                                        )
        
            mfp_flush = simple_property(
                                        id_="tuner_output::mfp_flush",
                                        
                                        name="mfp_flush",
                                        type_="long",
                                        defvalue=63
                                        )
        
            vlan_enable = simple_property(
                                          id_="tuner_output::vlan_enable",
                                          
                                          name="vlan_enable",
                                          type_="boolean",
                                          defvalue=True
                                          )
        
            def __init__(self, receiver_identifier="RCV:1", tuner_number=0, protocol="UDP_SDDS", interface=0, ip_address="", port=8800, vlan=-1, enabled=True, timestamp_offset=0, endianess=1, mfp_flush=63, vlan_enable=True):
                self.receiver_identifier = receiver_identifier
                self.tuner_number = tuner_number
                self.protocol = protocol
                self.interface = interface
                self.ip_address = ip_address
                self.port = port
                self.vlan = vlan
                self.enabled = enabled
                self.timestamp_offset = timestamp_offset
                self.endianess = endianess
                self.mfp_flush = mfp_flush
                self.vlan_enable = vlan_enable
        
            def __str__(self):
                """Return a string representation of this structure"""
                d = {}
                d["receiver_identifier"] = self.receiver_identifier
                d["tuner_number"] = self.tuner_number
                d["protocol"] = self.protocol
                d["interface"] = self.interface
                d["ip_address"] = self.ip_address
                d["port"] = self.port
                d["vlan"] = self.vlan
                d["enabled"] = self.enabled
                d["timestamp_offset"] = self.timestamp_offset
                d["endianess"] = self.endianess
                d["mfp_flush"] = self.mfp_flush
                d["vlan_enable"] = self.vlan_enable
                return str(d)
        
            @classmethod
            def getId(cls):
                return "tuner_output_definition"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("receiver_identifier",self.receiver_identifier),("tuner_number",self.tuner_number),("protocol",self.protocol),("interface",self.interface),("ip_address",self.ip_address),("port",self.port),("vlan",self.vlan),("enabled",self.enabled),("timestamp_offset",self.timestamp_offset),("endianess",self.endianess),("mfp_flush",self.mfp_flush),("vlan_enable",self.vlan_enable)]

        tuner_output = structseq_property(id_="tuner_output",
                                          name="tuner_output",
                                          structdef=TunerOutputDefinition,
                                          defvalue=[],
                                          configurationkind=("property",),
                                          mode="readonly")


        class BlockTunerOutputDefinition(object):
            receiver_identifier = simple_property(
                                                  id_="block_tuner_output::receiver_identifier",
                                                  
                                                  name="receiver_identifier",
                                                  type_="string",
                                                  defvalue="RCV:1"
                                                  )
        
            tuner_number_start = simple_property(
                                                 id_="block_tuner_output::tuner_number_start",
                                                 
                                                 name="tuner_number_start",
                                                 type_="short",
                                                 defvalue=0
                                                 )
        
            tuner_number_stop = simple_property(
                                                id_="block_tuner_output::tuner_number_stop",
                                                
                                                name="tuner_number_stop",
                                                type_="short",
                                                defvalue=0
                                                )
        
            protocol = simple_property(
                                       id_="block_tuner_output::protocol",
                                       
                                       name="protocol",
                                       type_="string",
                                       defvalue="UDP_SDDS"
                                       )
        
            interface = simple_property(
                                        id_="block_tuner_output::interface",
                                        
                                        name="interface",
                                        type_="short",
                                        defvalue=0
                                        )
        
            ip_address = simple_property(
                                         id_="block_tuner_output::ip_address",
                                         
                                         name="ip_address",
                                         type_="string",
                                         defvalue=""
                                         )
        
            increment_ip_address = simple_property(
                                                   id_="block_tuner_output::increment_ip_address",
                                                   
                                                   name="increment_ip_address",
                                                   type_="boolean",
                                                   defvalue=False
                                                   )
        
            port = simple_property(
                                   id_="block_tuner_output::port",
                                   
                                   name="port",
                                   type_="short",
                                   defvalue=8800
                                   )
        
            increment_port = simple_property(
                                             id_="block_tuner_output::increment_port",
                                             
                                             name="increment_port",
                                             type_="boolean",
                                             defvalue=True
                                             )
        
            vlan = simple_property(
                                   id_="block_tuner_output::vlan",
                                   
                                   name="vlan",
                                   type_="short",
                                   defvalue=-1
                                   )
        
            increment_vlan = simple_property(
                                             id_="block_tuner_output::increment_vlan",
                                             
                                             name="increment_vlan",
                                             type_="boolean",
                                             defvalue=False
                                             )
        
            enabled = simple_property(
                                      id_="block_tuner_output::enabled",
                                      
                                      name="enabled",
                                      type_="boolean",
                                      defvalue=True
                                      )
        
            timestamp_offset = simple_property(
                                               id_="block_tuner_output::timestamp_offset",
                                               
                                               name="timestamp_offset",
                                               type_="short",
                                               defvalue=0
                                               )
        
            endianess = simple_property(
                                        id_="block_tuner_output::endianess",
                                        
                                        name="endianess",
                                        type_="short",
                                        defvalue=1
                                        )
        
            mfp_flush = simple_property(
                                        id_="block_tuner_output::mfp_flush",
                                        
                                        name="mfp_flush",
                                        type_="long",
                                        defvalue=63
                                        )
        
            vlan_enable = simple_property(
                                          id_="block_tuner_output::vlan_enable",
                                          
                                          name="vlan_enable",
                                          type_="boolean",
                                          defvalue=True
                                          )
        
            def __init__(self, receiver_identifier="RCV:1", tuner_number_start=0, tuner_number_stop=0, protocol="UDP_SDDS", interface=0, ip_address="", increment_ip_address=False, port=8800, increment_port=True, vlan=-1, increment_vlan=False, enabled=True, timestamp_offset=0, endianess=1, mfp_flush=63, vlan_enable=True):
                self.receiver_identifier = receiver_identifier
                self.tuner_number_start = tuner_number_start
                self.tuner_number_stop = tuner_number_stop
                self.protocol = protocol
                self.interface = interface
                self.ip_address = ip_address
                self.increment_ip_address = increment_ip_address
                self.port = port
                self.increment_port = increment_port
                self.vlan = vlan
                self.increment_vlan = increment_vlan
                self.enabled = enabled
                self.timestamp_offset = timestamp_offset
                self.endianess = endianess
                self.mfp_flush = mfp_flush
                self.vlan_enable = vlan_enable
        
            def __str__(self):
                """Return a string representation of this structure"""
                d = {}
                d["receiver_identifier"] = self.receiver_identifier
                d["tuner_number_start"] = self.tuner_number_start
                d["tuner_number_stop"] = self.tuner_number_stop
                d["protocol"] = self.protocol
                d["interface"] = self.interface
                d["ip_address"] = self.ip_address
                d["increment_ip_address"] = self.increment_ip_address
                d["port"] = self.port
                d["increment_port"] = self.increment_port
                d["vlan"] = self.vlan
                d["increment_vlan"] = self.increment_vlan
                d["enabled"] = self.enabled
                d["timestamp_offset"] = self.timestamp_offset
                d["endianess"] = self.endianess
                d["mfp_flush"] = self.mfp_flush
                d["vlan_enable"] = self.vlan_enable
                return str(d)
        
            @classmethod
            def getId(cls):
                return "block_tuner_output_definition"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("receiver_identifier",self.receiver_identifier),("tuner_number_start",self.tuner_number_start),("tuner_number_stop",self.tuner_number_stop),("protocol",self.protocol),("interface",self.interface),("ip_address",self.ip_address),("increment_ip_address",self.increment_ip_address),("port",self.port),("increment_port",self.increment_port),("vlan",self.vlan),("increment_vlan",self.increment_vlan),("enabled",self.enabled),("timestamp_offset",self.timestamp_offset),("endianess",self.endianess),("mfp_flush",self.mfp_flush),("vlan_enable",self.vlan_enable)]

        block_tuner_output = structseq_property(id_="block_tuner_output",
                                                structdef=BlockTunerOutputDefinition,
                                                defvalue=[],
                                                configurationkind=("property",),
                                                mode="readonly")


        class BlockPsdOutputDefinition(object):
            receiver_identifier = simple_property(
                                                  id_="block_psd_output::receiver_identifier",
                                                  
                                                  name="receiver_identifier",
                                                  type_="string",
                                                  defvalue="RCV:1"
                                                  )
        
            fft_channel_start = simple_property(
                                                id_="block_psd_output::fft_channel_start",
                                                
                                                name="fft_channel_start",
                                                type_="short",
                                                defvalue=0
                                                )
        
            fft_channel_stop = simple_property(
                                               id_="block_psd_output::fft_channel_stop",
                                               
                                               name="fft_channel_stop",
                                               type_="short",
                                               defvalue=0
                                               )
        
            protocol = simple_property(
                                       id_="block_psd_output::protocol",
                                       
                                       name="protocol",
                                       type_="string",
                                       defvalue="UDP_SDDS"
                                       )
        
            interface = simple_property(
                                        id_="block_psd_output::interface",
                                        
                                        name="interface",
                                        type_="short",
                                        defvalue=0
                                        )
        
            ip_address = simple_property(
                                         id_="block_psd_output::ip_address",
                                         
                                         name="ip_address",
                                         type_="string",
                                         defvalue=""
                                         )
        
            increment_ip_address = simple_property(
                                                   id_="block_psd_output::increment_ip_address",
                                                   
                                                   name="increment_ip_address",
                                                   type_="boolean",
                                                   defvalue=False
                                                   )
        
            port = simple_property(
                                   id_="block_psd_output::port",
                                   
                                   name="port",
                                   type_="short",
                                   defvalue=8800
                                   )
        
            increment_port = simple_property(
                                             id_="block_psd_output::increment_port",
                                             
                                             name="increment_port",
                                             type_="boolean",
                                             defvalue=True
                                             )
        
            vlan = simple_property(
                                   id_="block_psd_output::vlan",
                                   
                                   name="vlan",
                                   type_="short",
                                   defvalue=-1
                                   )
        
            increment_vlan = simple_property(
                                             id_="block_psd_output::increment_vlan",
                                             
                                             name="increment_vlan",
                                             type_="boolean",
                                             defvalue=False
                                             )
        
            enabled = simple_property(
                                      id_="block_psd_output::enabled",
                                      
                                      name="enabled",
                                      type_="boolean",
                                      defvalue=False
                                      )
        
            timestamp_offset = simple_property(
                                               id_="block_psd_output::timestamp_offset",
                                               
                                               name="timestamp_offset",
                                               type_="short",
                                               defvalue=0
                                               )
        
            endianess = simple_property(
                                        id_="block_psd_output::endianess",
                                        
                                        name="endianess",
                                        type_="short",
                                        defvalue=1
                                        )
        
            mfp_flush = simple_property(
                                        id_="block_psd_output::mfp_flush",
                                        
                                        name="mfp_flush",
                                        type_="long",
                                        defvalue=63
                                        )
        
            vlan_enable = simple_property(
                                          id_="block_psd_output::vlan_enable",
                                          
                                          name="vlan_enable",
                                          type_="boolean",
                                          defvalue=True
                                          )
        
            def __init__(self, receiver_identifier="RCV:1", fft_channel_start=0, fft_channel_stop=0, protocol="UDP_SDDS", interface=0, ip_address="", increment_ip_address=False, port=8800, increment_port=True, vlan=-1, increment_vlan=False, enabled=False, timestamp_offset=0, endianess=1, mfp_flush=63, vlan_enable=True):
                self.receiver_identifier = receiver_identifier
                self.fft_channel_start = fft_channel_start
                self.fft_channel_stop = fft_channel_stop
                self.protocol = protocol
                self.interface = interface
                self.ip_address = ip_address
                self.increment_ip_address = increment_ip_address
                self.port = port
                self.increment_port = increment_port
                self.vlan = vlan
                self.increment_vlan = increment_vlan
                self.enabled = enabled
                self.timestamp_offset = timestamp_offset
                self.endianess = endianess
                self.mfp_flush = mfp_flush
                self.vlan_enable = vlan_enable
        
            def __str__(self):
                """Return a string representation of this structure"""
                d = {}
                d["receiver_identifier"] = self.receiver_identifier
                d["fft_channel_start"] = self.fft_channel_start
                d["fft_channel_stop"] = self.fft_channel_stop
                d["protocol"] = self.protocol
                d["interface"] = self.interface
                d["ip_address"] = self.ip_address
                d["increment_ip_address"] = self.increment_ip_address
                d["port"] = self.port
                d["increment_port"] = self.increment_port
                d["vlan"] = self.vlan
                d["increment_vlan"] = self.increment_vlan
                d["enabled"] = self.enabled
                d["timestamp_offset"] = self.timestamp_offset
                d["endianess"] = self.endianess
                d["mfp_flush"] = self.mfp_flush
                d["vlan_enable"] = self.vlan_enable
                return str(d)
        
            @classmethod
            def getId(cls):
                return "block_psd_output_definition"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("receiver_identifier",self.receiver_identifier),("fft_channel_start",self.fft_channel_start),("fft_channel_stop",self.fft_channel_stop),("protocol",self.protocol),("interface",self.interface),("ip_address",self.ip_address),("increment_ip_address",self.increment_ip_address),("port",self.port),("increment_port",self.increment_port),("vlan",self.vlan),("increment_vlan",self.increment_vlan),("enabled",self.enabled),("timestamp_offset",self.timestamp_offset),("endianess",self.endianess),("mfp_flush",self.mfp_flush),("vlan_enable",self.vlan_enable)]

        block_psd_output = structseq_property(id_="block_psd_output",
                                              structdef=BlockPsdOutputDefinition,
                                              defvalue=[],
                                              configurationkind=("property",),
                                              mode="readonly",
                                              description="""Define the output configuration for each FFT channel.""")




