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
#
# AUTO-GENERATED CODE.  DO NOT MODIFY!
#
# Source: MSDD.spd.xml
from ossie.cf import CF
from ossie.cf import CF__POA
from ossie.utils import uuid

from frontend import FrontendTunerDevice
from frontend import digital_tuner_delegation
from frontend import rfinfo_delegation
from ossie.threadedcomponent import *
from ossie.properties import simple_property
from ossie.properties import simpleseq_property
from ossie.properties import struct_property
from ossie.properties import structseq_property
from ossie.properties import struct_to_props
from omniORB import any as _any

import Queue, copy, time, threading
from ossie.resource import usesport, providesport
import bulkio
import frontend
from frontend import FRONTEND
BOOLEAN_VALUE_HERE=False

class MSDD_base(CF__POA.Device, FrontendTunerDevice, digital_tuner_delegation, rfinfo_delegation, ThreadedComponent):
        # These values can be altered in the __init__ of your derived class

        PAUSE = 0.0125 # The amount of time to sleep if process return NOOP
        TIMEOUT = 5.0 # The amount of time to wait for the process thread to die when stop() is called
        DEFAULT_QUEUE_SIZE = 100 # The number of BulkIO packets that can be in the queue before pushPacket will block

        def __init__(self, devmgr, uuid, label, softwareProfile, compositeDevice, execparams):
            FrontendTunerDevice.__init__(self, devmgr, uuid, label, softwareProfile, compositeDevice, execparams)
            ThreadedComponent.__init__(self)

            self.listeners={}
            # self.auto_start is deprecated and is only kept for API compatibility
            # with 1.7.X and 1.8.0 devices.  This variable may be removed
            # in future releases
            self.auto_start = False
            # Instantiate the default implementations for all ports on this device
            self.port_RFInfo_in = frontend.InRFInfoPort("RFInfo_in", self)
            self.port_DigitalTuner_in = frontend.InDigitalTunerPort("DigitalTuner_in", self)
            self.port_dataSDDS_out = bulkio.OutSDDSPort("dataSDDS_out")
            self.port_dataVITA49_out = bulkio.OutVITA49Port("dataVITA49_out")
            self.port_dataSDDS_out_PSD = bulkio.OutSDDSPort("dataSDDS_out_PSD")
            self.port_dataSDDS_out_SPC = bulkio.OutSDDSPort("dataSDDS_out_SPC")
            self.port_dataVITA49_out_PSD = bulkio.OutVITA49Port("dataVITA49_out_PSD")
            self.addPropertyChangeListener('connectionTable',self.updated_connectionTable)
            self.device_kind = "FRONTEND::TUNER"
            self.frontend_listener_allocation = frontend.fe_types.frontend_listener_allocation()
            self.frontend_tuner_allocation = frontend.fe_types.frontend_tuner_allocation()

        def start(self):
            FrontendTunerDevice.start(self)
            ThreadedComponent.startThread(self, pause=self.PAUSE)

        def stop(self):
            FrontendTunerDevice.stop(self)
            if not ThreadedComponent.stopThread(self, self.TIMEOUT):
                raise CF.Resource.StopError(CF.CF_NOTSET, "Processing thread did not die")

        def updated_connectionTable(self, id, oldval, newval):
            self.port_dataSDDS_out.updateConnectionFilter(newval)
            self.port_dataVITA49_out.updateConnectionFilter(newval)
            self.port_dataSDDS_out_PSD.updateConnectionFilter(newval)
            self.port_dataSDDS_out_SPC.updateConnectionFilter(newval)
            self.port_dataVITA49_out_PSD.updateConnectionFilter(newval)

        def releaseObject(self):
            try:
                self.stop()
            except Exception:
                self._log.exception("Error stopping")
            FrontendTunerDevice.releaseObject(self)

        ######################################################################
        # PORTS
        # 
        # DO NOT ADD NEW PORTS HERE.  You can add ports in your derived class, in the SCD xml file, 
        # or via the IDE.

        port_RFInfo_in = providesport(name="RFInfo_in",
                                      repid="IDL:FRONTEND/RFInfo:1.0",
                                      type_="data")

        port_DigitalTuner_in = providesport(name="DigitalTuner_in",
                                            repid="IDL:FRONTEND/DigitalTuner:1.0",
                                            type_="control")

        port_dataSDDS_out = usesport(name="dataSDDS_out",
                                     repid="IDL:BULKIO/dataSDDS:1.0",
                                     type_="data")

        port_dataVITA49_out = usesport(name="dataVITA49_out",
                                       repid="IDL:BULKIO/dataVITA49:1.0",
                                       type_="control")

        port_dataSDDS_out_PSD = usesport(name="dataSDDS_out_PSD",
                                         repid="IDL:BULKIO/dataSDDS:1.0",
                                         type_="control")

        port_dataSDDS_out_SPC = usesport(name="dataSDDS_out_SPC",
                                         repid="IDL:BULKIO/dataSDDS:1.0",
                                         type_="control")

        port_dataVITA49_out_PSD = usesport(name="dataVITA49_out_PSD",
                                           repid="IDL:BULKIO/dataVITA49:1.0",
                                           type_="control")

        ######################################################################
        # PROPERTIES
        # 
        # DO NOT ADD NEW PROPERTIES HERE.  You can add properties in your derived class, in the PRF xml file
        # or by using the IDE.
        frontend_group_id = simple_property(id_="frontend_group_id",
                                            type_="string",
                                            defvalue="",
                                            mode="readwrite",
                                            action="external",
                                            kinds=("property",))


        clock_ref = simple_property(id_="clock_ref",
                                    name="clock_ref",
                                    type_="short",
                                    defvalue=10,
                                    mode="readwrite",
                                    action="external",
                                    kinds=("property",))


        class advanced_struct(object):
            enable_msdd_advanced_debugging_tools = simple_property(
                                                                   id_="advanced::enable_msdd_advanced_debugging_tools",
                                                                   name="enable_msdd_advanced_debugging_tools",
                                                                   type_="boolean",
                                                                   defvalue=False
                                                                   )
        
            allow_internal_allocations = simple_property(
                                                         id_="advanced::allow_internal_allocations",
                                                         name="allow_internal_allocations",
                                                         type_="boolean",
                                                         defvalue=True
                                                         )
        
            udp_timeout = simple_property(
                                          id_="advanced::udp_timeout",
                                          name="udp_timeout",
                                          type_="double",
                                          defvalue=1.0
                                          )
        
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
                                          defvalue=7
                                          )
        
            sw_ddc_mode = simple_property(
                                          id_="advanced::sw_ddc_mode",
                                          name="sw_ddc_mode",
                                          type_="short",
                                          defvalue=1
                                          )
        
            psd_mode = simple_property(
                                       id_="advanced::psd_mode",
                                       name="psd_mode",
                                       type_="short",
                                       defvalue=0
                                       )
        
            spc_mode = simple_property(
                                       id_="advanced::spc_mode",
                                       name="spc_mode",
                                       type_="short",
                                       defvalue=0
                                       )
        
            minimum_connected_nic_rate = simple_property(
                                                         id_="minimum_connected_nic_rate",
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
                d["enable_msdd_advanced_debugging_tools"] = self.enable_msdd_advanced_debugging_tools
                d["allow_internal_allocations"] = self.allow_internal_allocations
                d["udp_timeout"] = self.udp_timeout
                d["rcvr_mode"] = self.rcvr_mode
                d["wb_ddc_mode"] = self.wb_ddc_mode
                d["hw_ddc_mode"] = self.hw_ddc_mode
                d["sw_ddc_mode"] = self.sw_ddc_mode
                d["psd_mode"] = self.psd_mode
                d["spc_mode"] = self.spc_mode
                d["minimum_connected_nic_rate"] = self.minimum_connected_nic_rate
                return str(d)
        
            @classmethod
            def getId(cls):
                return "advanced"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("enable_msdd_advanced_debugging_tools",self.enable_msdd_advanced_debugging_tools),("allow_internal_allocations",self.allow_internal_allocations),("udp_timeout",self.udp_timeout),("rcvr_mode",self.rcvr_mode),("wb_ddc_mode",self.wb_ddc_mode),("hw_ddc_mode",self.hw_ddc_mode),("sw_ddc_mode",self.sw_ddc_mode),("psd_mode",self.psd_mode),("spc_mode",self.spc_mode),("minimum_connected_nic_rate",self.minimum_connected_nic_rate)]

        advanced = struct_property(id_="advanced",
                                   structdef=advanced_struct,
                                   configurationkind=("property",),
                                   mode="readwrite")


        class msdd_configuration_struct(object):
            msdd_ip_address = simple_property(
                                              id_="msdd_configuration::msdd_ip_address",
                                              name="msdd_ip_address",
                                              type_="string",
                                              defvalue="127.0.0.1"
                                              )
        
            msdd_port = simple_property(
                                        id_="msdd_configuration::msdd_port",
                                        name="msdd_port",
                                        type_="string",
                                        defvalue="23"
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
                d["msdd_ip_address"] = self.msdd_ip_address
                d["msdd_port"] = self.msdd_port
                return str(d)
        
            @classmethod
            def getId(cls):
                return "msdd_configuration"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("msdd_ip_address",self.msdd_ip_address),("msdd_port",self.msdd_port)]

        msdd_configuration = struct_property(id_="msdd_configuration",
                                             structdef=msdd_configuration_struct,
                                             configurationkind=("property",),
                                             mode="readwrite")


        class msdd_time_of_day_configuration_struct(object):
            mode = simple_property(
                                   id_="msdd_time_of_day_configuration::mode",
                                   name="mode",
                                   type_="string",
                                   defvalue="SIM"
                                   )
        
            reference_adjust = simple_property(
                                               id_="msdd_time_of_day_configuration::reference_adjust",
                                               name="reference_adjust",
                                               type_="short",
                                               defvalue=0
                                               )
        
            reference_track = simple_property(
                                              id_="msdd_time_of_day_configuration::reference_track",
                                              name="reference_track",
                                              type_="short",
                                              defvalue=0
                                              )
        
            toy = simple_property(
                                  id_="msdd_time_of_day_configuration::toy",
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
                return "msdd_time_of_day_configuration"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("mode",self.mode),("reference_adjust",self.reference_adjust),("reference_track",self.reference_track),("toy",self.toy)]

        msdd_time_of_day_configuration = struct_property(id_="msdd_time_of_day_configuration",
                                                         structdef=msdd_time_of_day_configuration_struct,
                                                         configurationkind=("property",),
                                                         mode="readwrite")


        class msdd_psd_configuration_struct(object):
            fft_size = simple_property(
                                       id_="msdd_psd_configuration::fft_size",
                                       name="fft_size",
                                       type_="double",
                                       defvalue=32768.0
                                       )
        
            time_average = simple_property(
                                           id_="msdd_psd_configuration::time_average",
                                           name="time_average",
                                           type_="double",
                                           defvalue=0.10000000000000001
                                           )
        
            time_between_ffts = simple_property(
                                                id_="msdd_psd_configuration::time_between_ffts",
                                                name="time_between_ffts",
                                                type_="double",
                                                defvalue=0.0
                                                )
        
            output_bin_size = simple_property(
                                              id_="msdd_psd_configuration::output_bin_size",
                                              name="output_bin_size",
                                              type_="double",
                                              defvalue=0.0
                                              )
        
            window_type = simple_property(
                                          id_="msdd_psd_configuration::window_type",
                                          name="window_type",
                                          type_="string",
                                          defvalue="HAMMING"
                                          )
        
            peak_mode = simple_property(
                                        id_="msdd_psd_configuration::peak_mode",
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
                return "msdd_psd_configuration"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("fft_size",self.fft_size),("time_average",self.time_average),("time_between_ffts",self.time_between_ffts),("output_bin_size",self.output_bin_size),("window_type",self.window_type),("peak_mode",self.peak_mode)]

        msdd_psd_configuration = struct_property(id_="msdd_psd_configuration",
                                                 structdef=msdd_psd_configuration_struct,
                                                 configurationkind=("property",),
                                                 mode="readwrite",
                                                 description="""Currently this is global across all fft modules.This may be changed in future releases.""")


        class msdd_spc_configuration_struct(object):
            fft_size = simple_property(
                                       id_="msdd_spc_configuration::fft_size",
                                       name="fft_size",
                                       type_="short",
                                       defvalue=8192
                                       )
        
            num_fft_averages = simple_property(
                                               id_="msdd_spc_configuration::num_fft_averages",
                                               name="num_fft_averages",
                                               type_="short",
                                               defvalue=0
                                               )
        
            time_between_ffts = simple_property(
                                                id_="msdd_spc_configuration::time_between_ffts",
                                                name="time_between_ffts",
                                                type_="double",
                                                defvalue=200.0
                                                )
        
            output_bin_size = simple_property(
                                              id_="msdd_spc_configuration::output_bin_size",
                                              name="output_bin_size",
                                              type_="short",
                                              defvalue=4096
                                              )
        
            window_type = simple_property(
                                          id_="msdd_spc_configuration::window_type",
                                          name="window_type",
                                          type_="string",
                                          defvalue="HAMMING"
                                          )
        
            peak_mode = simple_property(
                                        id_="msdd_spc_configuration::peak_mode",
                                        name="peak_mode",
                                        type_="string",
                                        defvalue="INST"
                                        )
        
            start_frequency = simple_property(
                                              id_="msdd_spc_configuration::start_frequency",
                                              name="start_frequency",
                                              type_="double",
                                              defvalue=100000000.0
                                              )
        
            stop_frequency = simple_property(
                                             id_="msdd_spc_configuration::stop_frequency",
                                             name="stop_frequency",
                                             type_="double",
                                             defvalue=1000000000.0
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
                d["num_fft_averages"] = self.num_fft_averages
                d["time_between_ffts"] = self.time_between_ffts
                d["output_bin_size"] = self.output_bin_size
                d["window_type"] = self.window_type
                d["peak_mode"] = self.peak_mode
                d["start_frequency"] = self.start_frequency
                d["stop_frequency"] = self.stop_frequency
                return str(d)
        
            @classmethod
            def getId(cls):
                return "msdd_spc_configuration"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("fft_size",self.fft_size),("num_fft_averages",self.num_fft_averages),("time_between_ffts",self.time_between_ffts),("output_bin_size",self.output_bin_size),("window_type",self.window_type),("peak_mode",self.peak_mode),("start_frequency",self.start_frequency),("stop_frequency",self.stop_frequency)]

        msdd_spc_configuration = struct_property(id_="msdd_spc_configuration",
                                                 structdef=msdd_spc_configuration_struct,
                                                 configurationkind=("property",),
                                                 mode="readwrite",
                                                 description="""This is for configuration of the spectral scanning.""")


        class msdd_status_struct(object):
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
                return str(d)
        
            @classmethod
            def getId(cls):
                return "msdd_status"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("connected",self.connected),("ip_address",self.ip_address),("port",self.port),("model",self.model),("serial",self.serial),("software_part_number",self.software_part_number),("rf_board_type",self.rf_board_type),("fpga_type",self.fpga_type),("dsp_type",self.dsp_type),("minimum_frequency_hz",self.minimum_frequency_hz),("maximum_frequency_hz",self.maximum_frequency_hz),("dsp_reference_frequency_hz",self.dsp_reference_frequency_hz),("adc_clock_frequency_hz",self.adc_clock_frequency_hz),("num_if_ports",self.num_if_ports),("num_eth_ports",self.num_eth_ports),("cpu_type",self.cpu_type),("cpu_rate",self.cpu_rate),("cpu_load",self.cpu_load),("pps_termination",self.pps_termination),("pps_voltage",self.pps_voltage),("number_wb_ddc_channels",self.number_wb_ddc_channels),("number_nb_ddc_channels",self.number_nb_ddc_channels),("filename_app",self.filename_app),("filename_fpga",self.filename_fpga),("filename_batch",self.filename_batch),("filename_boot",self.filename_boot),("filename_loader",self.filename_loader),("filename_config",self.filename_config),("tod_module",self.tod_module),("tod_available_module",self.tod_available_module),("tod_meter_list",self.tod_meter_list),("tod_tod_reference_adjust",self.tod_tod_reference_adjust),("tod_track_mode_state",self.tod_track_mode_state),("tod_bit_state",self.tod_bit_state),("tod_toy",self.tod_toy)]

        msdd_status = struct_property(id_="msdd_status",
                                      structdef=msdd_status_struct,
                                      configurationkind=("property",),
                                      mode="readonly")


        class msdd_advanced_debugging_tools_struct(object):
            command = simple_property(
                                      id_="msdd_advanced_debugging_tools::command",
                                      name="command",
                                      type_="string")
        
            response = simple_property(
                                       id_="msdd_advanced_debugging_tools::response",
                                       name="response",
                                       type_="string")
        
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
                d["command"] = self.command
                d["response"] = self.response
                return str(d)
        
            @classmethod
            def getId(cls):
                return "msdd_advanced_debugging_tools"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("command",self.command),("response",self.response)]

        msdd_advanced_debugging_tools = struct_property(id_="msdd_advanced_debugging_tools",
                                                        structdef=msdd_advanced_debugging_tools_struct,
                                                        configurationkind=("property",),
                                                        mode="readwrite")


        class msdd_gain_configuration_struct(object):
            rcvr_gain = simple_property(
                                        id_="msdd_gain_configuration::rcvr_gain",
                                        name="rcvr_gain",
                                        type_="float",
                                        defvalue=0.0
                                        )
        
            wb_ddc_gain = simple_property(
                                          id_="msdd_gain_configuration::wb_ddc_gain",
                                          name="wb_ddc_gain",
                                          type_="float",
                                          defvalue=0.0
                                          )
        
            hw_ddc_gain = simple_property(
                                          id_="msdd_gain_configuration::hw_ddc_gain",
                                          name="hw_ddc_gain",
                                          type_="float",
                                          defvalue=0.0
                                          )
        
            sw_ddc_gain = simple_property(
                                          id_="msdd_gain_configuration::sw_ddc_gain",
                                          name="sw_ddc_gain",
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
                d["sw_ddc_gain"] = self.sw_ddc_gain
                return str(d)
        
            @classmethod
            def getId(cls):
                return "msdd_gain_configuration"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("rcvr_gain",self.rcvr_gain),("wb_ddc_gain",self.wb_ddc_gain),("hw_ddc_gain",self.hw_ddc_gain),("sw_ddc_gain",self.sw_ddc_gain)]

        msdd_gain_configuration = struct_property(id_="msdd_gain_configuration",
                                                  structdef=msdd_gain_configuration_struct,
                                                  configurationkind=("property",),
                                                  mode="readwrite")


        class msdd_output_configuration_struct_struct(object):
            tuner_number = simple_property(
                                           id_="msdd_output_configuration::tuner_number",
                                           name="tuner_number",
                                           type_="short",
                                           defvalue=0
                                           )
        
            protocol = simple_property(
                                       id_="msdd_output_configuration::protocol",
                                       name="protocol",
                                       type_="string",
                                       defvalue="UDP_SDDS"
                                       )
        
            ip_address = simple_property(
                                         id_="msdd_output_configuration::ip_address",
                                         name="ip_address",
                                         type_="string",
                                         defvalue=""
                                         )
        
            port = simple_property(
                                   id_="msdd_output_configuration::port",
                                   name="port",
                                   type_="short",
                                   defvalue=8800
                                   )
        
            vlan = simple_property(
                                   id_="msdd_output_configuration::vlan",
                                   name="vlan",
                                   type_="short",
                                   defvalue=-1
                                   )
        
            enabled = simple_property(
                                      id_="msdd_output_configuration::enabled",
                                      name="enabled",
                                      type_="boolean",
                                      defvalue=True
                                      )
        
            timestamp_offset = simple_property(
                                               id_="msdd_output_configuration::timestamp_offset",
                                               name="timestamp_offset",
                                               type_="short",
                                               defvalue=0
                                               )
        
            endianess = simple_property(
                                        id_="msdd_output_configuration::endianess",
                                        name="endianess",
                                        type_="short",
                                        defvalue=1
                                        )
        
            mfp_flush = simple_property(
                                        id_="msdd_output_configuration::mfp_flush",
                                        name="mfp_flush",
                                        type_="long",
                                        defvalue=63
                                        )
        
            vlan_enable = simple_property(
                                          id_="msdd_output_configuration::vlan_enable",
                                          name="vlan_enable",
                                          type_="boolean",
                                          defvalue=True
                                          )
        
            def __init__(self, tuner_number=0, protocol="UDP_SDDS", ip_address="", port=8800, vlan=-1, enabled=True, timestamp_offset=0, endianess=1, mfp_flush=63, vlan_enable=True):
                self.tuner_number = tuner_number
                self.protocol = protocol
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
                d["tuner_number"] = self.tuner_number
                d["protocol"] = self.protocol
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
                return "msdd_output_configuration_struct"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("tuner_number",self.tuner_number),("protocol",self.protocol),("ip_address",self.ip_address),("port",self.port),("vlan",self.vlan),("enabled",self.enabled),("timestamp_offset",self.timestamp_offset),("endianess",self.endianess),("mfp_flush",self.mfp_flush),("vlan_enable",self.vlan_enable)]

        msdd_output_configuration = structseq_property(id_="msdd_output_configuration",
                                                       structdef=msdd_output_configuration_struct_struct,
                                                       defvalue=[],
                                                       configurationkind=("property",),
                                                       mode="readwrite")


        class msdd_block_output_configuration_struct_struct(object):
            tuner_number_start = simple_property(
                                                 id_="msdd_block_output_configuration::tuner_number_start",
                                                 name="tuner_number_start",
                                                 type_="short",
                                                 defvalue=0
                                                 )
        
            tuner_number_stop = simple_property(
                                                id_="msdd_block_output_configuration::tuner_number_stop",
                                                name="tuner_number_stop",
                                                type_="short",
                                                defvalue=0
                                                )
        
            protocol = simple_property(
                                       id_="msdd_block_output_configuration::protocol",
                                       name="protocol",
                                       type_="string",
                                       defvalue="UDP_SDDS"
                                       )
        
            ip_address = simple_property(
                                         id_="msdd_block_output_configuration::ip_address",
                                         name="ip_address",
                                         type_="string",
                                         defvalue=""
                                         )
        
            increment_ip_address = simple_property(
                                                   id_="msdd_block_output_configuration::increment_ip_address",
                                                   name="increment_ip_address",
                                                   type_="boolean",
                                                   defvalue=False
                                                   )
        
            port = simple_property(
                                   id_="msdd_block_output_configuration::port",
                                   name="port",
                                   type_="short",
                                   defvalue=8800
                                   )
        
            increment_port = simple_property(
                                             id_="msdd_block_output_configuration::increment_port",
                                             name="increment_port",
                                             type_="boolean",
                                             defvalue=True
                                             )
        
            vlan = simple_property(
                                   id_="msdd_block_output_configuration::vlan",
                                   name="vlan",
                                   type_="short",
                                   defvalue=-1
                                   )
        
            increment_vlan = simple_property(
                                             id_="msdd_block_output_configuration::increment_vlan",
                                             name="increment_vlan",
                                             type_="boolean",
                                             defvalue=False
                                             )
        
            enabled = simple_property(
                                      id_="msdd_block_output_configuration::enabled",
                                      name="enabled",
                                      type_="boolean",
                                      defvalue=True
                                      )
        
            timestamp_offset = simple_property(
                                               id_="msdd_block_output_configuration::timestamp_offset",
                                               name="timestamp_offset",
                                               type_="short",
                                               defvalue=0
                                               )
        
            endianess = simple_property(
                                        id_="msdd_block_output_configuration::endianess",
                                        name="endianess",
                                        type_="short",
                                        defvalue=1
                                        )
        
            mfp_flush = simple_property(
                                        id_="msdd_block_output_configuration:mfp_flush",
                                        name="mfp_flush",
                                        type_="long",
                                        defvalue=63
                                        )
        
            vlan_enable = simple_property(
                                          id_="msdd_block_output_configuration:vlan_enable",
                                          name="vlan_enable",
                                          type_="boolean",
                                          defvalue=True
                                          )
        
            def __init__(self, tuner_number_start=0, tuner_number_stop=0, protocol="UDP_SDDS", ip_address="", increment_ip_address=False, port=8800, increment_port=True, vlan=-1, increment_vlan=False, enabled=True, timestamp_offset=0, endianess=1, mfp_flush=63, vlan_enable=True):
                self.tuner_number_start = tuner_number_start
                self.tuner_number_stop = tuner_number_stop
                self.protocol = protocol
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
                d["tuner_number_start"] = self.tuner_number_start
                d["tuner_number_stop"] = self.tuner_number_stop
                d["protocol"] = self.protocol
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
                return "msdd_block_output_configuration_struct"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return [("tuner_number_start",self.tuner_number_start),("tuner_number_stop",self.tuner_number_stop),("protocol",self.protocol),("ip_address",self.ip_address),("increment_ip_address",self.increment_ip_address),("port",self.port),("increment_port",self.increment_port),("vlan",self.vlan),("increment_vlan",self.increment_vlan),("enabled",self.enabled),("timestamp_offset",self.timestamp_offset),("endianess",self.endianess),("mfp_flush",self.mfp_flush),("vlan_enable",self.vlan_enable)]

        msdd_block_output_configuration = structseq_property(id_="msdd_block_output_configuration",
                                                             structdef=msdd_block_output_configuration_struct_struct,
                                                             defvalue=[],
                                                             configurationkind=("property",),
                                                             mode="readwrite")


        class frontend_tuner_status_struct_struct(frontend.default_frontend_tuner_status_struct_struct):
            available_bandwidth = simple_property(
                                                  id_="FRONTEND::tuner_status::available_bandwidth",
                                                  name="available_bandwidth",
                                                  type_="string")
        
            available_frequency = simple_property(
                                                  id_="FRONTEND::tuner_status::available_frequency",
                                                  name="available_frequency",
                                                  type_="string")
        
            available_gain = simple_property(
                                             id_="FRONTEND::tuner_status::available_gain",
                                             name="available_gain",
                                             type_="string")
        
            available_sample_rate = simple_property(
                                                    id_="FRONTEND::tuner_status::available_sample_rate",
                                                    name="available_sample_rate",
                                                    type_="string")
        
            complex = simple_property(
                                      id_="FRONTEND::tuner_status::complex",
                                      name="complex",
                                      type_="boolean")
        
            decimation = simple_property(
                                         id_="FRONTEND::tuner_status::decimation",
                                         name="decimation",
                                         type_="long")
        
            gain = simple_property(
                                   id_="FRONTEND::tuner_status::gain",
                                   name="gain",
                                   type_="double")
        
            output_format = simple_property(
                                            id_="FRONTEND::tuner_status::output_format",
                                            name="output_format",
                                            type_="string")
        
            output_multicast = simple_property(
                                               id_="FRONTEND::tuner_status::output_multicast",
                                               name="output_multicast",
                                               type_="string")
        
            output_port = simple_property(
                                          id_="FRONTEND::tuner_status::output_port",
                                          name="output_port",
                                          type_="long")
        
            output_vlan = simple_property(
                                          id_="FRONTEND::tuner_status::output_vlan",
                                          name="output_vlan",
                                          type_="long")
        
            tuner_number = simple_property(
                                           id_="FRONTEND::tuner_status::tuner_number",
                                           name="tuner_number",
                                           type_="short")
        
            available_decimation = simple_property(
                                                   id_="FRONTEND::tuner_status::available_decimation",
                                                   name="available_decimation",
                                                   type_="string",
                                                   defvalue=""
                                                   )
        
            msdd_channel_type = simple_property(
                                                id_="FRONTEND::tuner_status::msdd_channel_type",
                                                name="msdd_channel_type",
                                                type_="string")
        
            msdd_installation_name_csv = simple_property(
                                                         id_="FRONTEND::tuner_status::msdd_installation_name_csv",
                                                         name="msdd_installation_name_csv",
                                                         type_="string")
        
            msdd_registration_name_csv = simple_property(
                                                         id_="FRONTEND::tuner_status::msdd_registration_name_csv",
                                                         name="msdd_registration_name_csv",
                                                         type_="string")
        
            bits_per_sample = simple_property(
                                              id_="FRONTEND::tuner_status::bits_per_sample",
                                              name="bits_per_sample",
                                              type_="short")
        
            adc_meter_values = simple_property(
                                               id_="FRONTEND::tuner_status::adc_meter_values",
                                               name="adc_meter_values",
                                               type_="string")
        
            rcvr_gain = simple_property(
                                        id_="FRONTEND::tuner_status::rcvr_gain",
                                        name="rcvr_gain",
                                        type_="float")
        
            ddc_gain = simple_property(
                                       id_="FRONTEND::tuner_status::ddc_gain",
                                       name="ddc_gain",
                                       type_="float")
        
            allocated = simple_property(
                                        id_="FRONTEND::tuner_status::allocated",
                                        name="allocated",
                                        type_="boolean")
        
            input_sample_rate = simple_property(
                                                id_="FRONTEND::tuner_status::input_sample_rate",
                                                name="input_sample_rate",
                                                type_="double")
        
            output_channel = simple_property(
                                             id_="FRONTEND::tuner_status::output_channel",
                                             name="output_channel",
                                             type_="string")
        
            output_enabled = simple_property(
                                             id_="FRONTEND::tuner_status::output_enabled",
                                             name="output_enabled",
                                             type_="boolean")
        
            output_protocol = simple_property(
                                              id_="FRONTEND::tuner_status::output_protocol",
                                              name="output_protocol",
                                              type_="string")
        
            output_vlan_enabled = simple_property(
                                                  id_="FRONTEND::tuner_status::output_vlan_enabled",
                                                  name="output_vlan_enabled",
                                                  type_="boolean")
        
            output_vlan_tci = simple_property(
                                              id_="FRONTEND::tuner_status::output_vlan_tci",
                                              name="output_vlan_tci",
                                              type_="string")
        
            output_flow = simple_property(
                                          id_="FRONTEND::tuner_status::output_flow",
                                          name="output_flow",
                                          type_="string")
        
            output_timestamp_offset = simple_property(
                                                      id_="FRONTEND::tuner_status::output_timestamp_offset",
                                                      name="output_timestamp_offset",
                                                      type_="string")
        
            output_endianess = simple_property(
                                               id_="FRONTEND::tuner_status::output_endianess",
                                               name="output_endianess",
                                               type_="short",
                                               defvalue=1
                                               )
        
            output_mfp_flush = simple_property(
                                               id_="FRONTEND::tuner_status::output_mfp_flush",
                                               name="output_mfp_flush",
                                               type_="long")
        
            psd_fft_size = simple_property(
                                           id_="FRONTEND::tuner_status::psd_fft_size",
                                           name="psd_fft_size",
                                           type_="double")
        
            psd_averages = simple_property(
                                           id_="FRONTEND::tuner_status::psd_averages",
                                           name="psd_averages",
                                           type_="double")
        
            psd_time_between_ffts = simple_property(
                                                    id_="FRONTEND::tuner_status::psd_time_between_ffts",
                                                    name="psd_time_between_ffts",
                                                    type_="double")
        
            psd_output_bin_size = simple_property(
                                                  id_="FRONTEND::tuner_status::psd_output_bin_size",
                                                  name="psd_output_bin_size",
                                                  type_="double")
        
            psd_window_type = simple_property(
                                              id_="FRONTEND::tuner_status::psd_window_type",
                                              name="psd_window_type",
                                              type_="string")
        
            psd_peak_mode = simple_property(
                                            id_="FRONTEND::tuner_status::psd_peak_mode",
                                            name="psd_peak_mode",
                                            type_="string")
        
            spc_fft_size = simple_property(
                                           id_="FRONTEND::tuner_status::spc_fft_size",
                                           name="spc_fft_size",
                                           type_="short")
        
            spc_averages = simple_property(
                                           id_="FRONTEND::tuner_status::spc_averages",
                                           name="spc_averages",
                                           type_="short")
        
            spc_time_between_ffts = simple_property(
                                                    id_="FRONTEND::tuner_status::spc_time_between_ffts",
                                                    name="spc_time_between_ffts",
                                                    type_="double")
        
            spc_output_bin_size = simple_property(
                                                  id_="FRONTEND::tuner_status::spc_output_bin_size",
                                                  name="spc_output_bin_size",
                                                  type_="short")
        
            spc_window_type = simple_property(
                                              id_="FRONTEND::tuner_status::spc_window_type",
                                              name="spc_window_type",
                                              type_="string")
        
            spc_peak_mode = simple_property(
                                            id_="FRONTEND::tuner_status::spc_peak_mode",
                                            name="spc_peak_mode",
                                            type_="string")
        
            spc_start_frequency = simple_property(
                                                  id_="FRONTEND::tuner_status::spc_start_frequency",
                                                  name="spc_start_frequency",
                                                  type_="double")
        
            available_tuner_type = simple_property(
                                                   id_="FRONTEND::tuner_status::available_tuner_type",
                                                   name="available_tuner_type",
                                                   type_="string")
        
            spc_stop_frequency = simple_property(
                                                 id_="FRONTEND::tuner_status::spc_stop_frequency",
                                                 name="spc_stop_frequency",
                                                 type_="double")
        
            def __init__(self, allocation_id_csv="", available_bandwidth="", available_frequency="", available_gain="", available_sample_rate="", bandwidth=0.0, center_frequency=0.0, complex=False, decimation=0, enabled=False, gain=0.0, group_id="", output_format="", output_multicast="", output_port=0, output_vlan=0, rf_flow_id="", sample_rate=0.0, tuner_number=0, tuner_type="", available_decimation="", msdd_channel_type="", msdd_installation_name_csv="", msdd_registration_name_csv="", bits_per_sample=0, adc_meter_values="", rcvr_gain=0.0, ddc_gain=0.0, allocated=False, input_sample_rate=0.0, output_channel="", output_enabled=False, output_protocol="", output_vlan_enabled=False, output_vlan_tci="", output_flow="", output_timestamp_offset="", output_endianess=1, output_mfp_flush=0, psd_fft_size=0.0, psd_averages=0.0, psd_time_between_ffts=0.0, psd_output_bin_size=0.0, psd_window_type="", psd_peak_mode="", spc_fft_size=0, spc_averages=0, spc_time_between_ffts=0.0, spc_output_bin_size=0, spc_window_type="", spc_peak_mode="", spc_start_frequency=0.0, available_tuner_type="", spc_stop_frequency=0.0):
                frontend.default_frontend_tuner_status_struct_struct.__init__(self, allocation_id_csv=allocation_id_csv, bandwidth=bandwidth, center_frequency=center_frequency, enabled=enabled, group_id=group_id, rf_flow_id=rf_flow_id, sample_rate=sample_rate, tuner_type=tuner_type)
                self.available_bandwidth = available_bandwidth
                self.available_frequency = available_frequency
                self.available_gain = available_gain
                self.available_sample_rate = available_sample_rate
                self.complex = complex
                self.decimation = decimation
                self.gain = gain
                self.output_format = output_format
                self.output_multicast = output_multicast
                self.output_port = output_port
                self.output_vlan = output_vlan
                self.tuner_number = tuner_number
                self.available_decimation = available_decimation
                self.msdd_channel_type = msdd_channel_type
                self.msdd_installation_name_csv = msdd_installation_name_csv
                self.msdd_registration_name_csv = msdd_registration_name_csv
                self.bits_per_sample = bits_per_sample
                self.adc_meter_values = adc_meter_values
                self.rcvr_gain = rcvr_gain
                self.ddc_gain = ddc_gain
                self.allocated = allocated
                self.input_sample_rate = input_sample_rate
                self.output_channel = output_channel
                self.output_enabled = output_enabled
                self.output_protocol = output_protocol
                self.output_vlan_enabled = output_vlan_enabled
                self.output_vlan_tci = output_vlan_tci
                self.output_flow = output_flow
                self.output_timestamp_offset = output_timestamp_offset
                self.output_endianess = output_endianess
                self.output_mfp_flush = output_mfp_flush
                self.psd_fft_size = psd_fft_size
                self.psd_averages = psd_averages
                self.psd_time_between_ffts = psd_time_between_ffts
                self.psd_output_bin_size = psd_output_bin_size
                self.psd_window_type = psd_window_type
                self.psd_peak_mode = psd_peak_mode
                self.spc_fft_size = spc_fft_size
                self.spc_averages = spc_averages
                self.spc_time_between_ffts = spc_time_between_ffts
                self.spc_output_bin_size = spc_output_bin_size
                self.spc_window_type = spc_window_type
                self.spc_peak_mode = spc_peak_mode
                self.spc_start_frequency = spc_start_frequency
                self.available_tuner_type = available_tuner_type
                self.spc_stop_frequency = spc_stop_frequency
        
            def __str__(self):
                """Return a string representation of this structure"""
                d = {}
                d["allocation_id_csv"] = self.allocation_id_csv
                d["available_bandwidth"] = self.available_bandwidth
                d["available_frequency"] = self.available_frequency
                d["available_gain"] = self.available_gain
                d["available_sample_rate"] = self.available_sample_rate
                d["bandwidth"] = self.bandwidth
                d["center_frequency"] = self.center_frequency
                d["complex"] = self.complex
                d["decimation"] = self.decimation
                d["enabled"] = self.enabled
                d["gain"] = self.gain
                d["group_id"] = self.group_id
                d["output_format"] = self.output_format
                d["output_multicast"] = self.output_multicast
                d["output_port"] = self.output_port
                d["output_vlan"] = self.output_vlan
                d["rf_flow_id"] = self.rf_flow_id
                d["sample_rate"] = self.sample_rate
                d["tuner_number"] = self.tuner_number
                d["tuner_type"] = self.tuner_type
                d["available_decimation"] = self.available_decimation
                d["msdd_channel_type"] = self.msdd_channel_type
                d["msdd_installation_name_csv"] = self.msdd_installation_name_csv
                d["msdd_registration_name_csv"] = self.msdd_registration_name_csv
                d["bits_per_sample"] = self.bits_per_sample
                d["adc_meter_values"] = self.adc_meter_values
                d["rcvr_gain"] = self.rcvr_gain
                d["ddc_gain"] = self.ddc_gain
                d["allocated"] = self.allocated
                d["input_sample_rate"] = self.input_sample_rate
                d["output_channel"] = self.output_channel
                d["output_enabled"] = self.output_enabled
                d["output_protocol"] = self.output_protocol
                d["output_vlan_enabled"] = self.output_vlan_enabled
                d["output_vlan_tci"] = self.output_vlan_tci
                d["output_flow"] = self.output_flow
                d["output_timestamp_offset"] = self.output_timestamp_offset
                d["output_endianess"] = self.output_endianess
                d["output_mfp_flush"] = self.output_mfp_flush
                d["psd_fft_size"] = self.psd_fft_size
                d["psd_averages"] = self.psd_averages
                d["psd_time_between_ffts"] = self.psd_time_between_ffts
                d["psd_output_bin_size"] = self.psd_output_bin_size
                d["psd_window_type"] = self.psd_window_type
                d["psd_peak_mode"] = self.psd_peak_mode
                d["spc_fft_size"] = self.spc_fft_size
                d["spc_averages"] = self.spc_averages
                d["spc_time_between_ffts"] = self.spc_time_between_ffts
                d["spc_output_bin_size"] = self.spc_output_bin_size
                d["spc_window_type"] = self.spc_window_type
                d["spc_peak_mode"] = self.spc_peak_mode
                d["spc_start_frequency"] = self.spc_start_frequency
                d["available_tuner_type"] = self.available_tuner_type
                d["spc_stop_frequency"] = self.spc_stop_frequency
                return str(d)
        
            @classmethod
            def getId(cls):
                return "FRONTEND::tuner_status_struct"
        
            @classmethod
            def isStruct(cls):
                return True
        
            def getMembers(self):
                return frontend.default_frontend_tuner_status_struct_struct.getMembers(self) + [("available_bandwidth",self.available_bandwidth),("available_frequency",self.available_frequency),("available_gain",self.available_gain),("available_sample_rate",self.available_sample_rate),("complex",self.complex),("decimation",self.decimation),("gain",self.gain),("output_format",self.output_format),("output_multicast",self.output_multicast),("output_port",self.output_port),("output_vlan",self.output_vlan),("tuner_number",self.tuner_number),("available_decimation",self.available_decimation),("msdd_channel_type",self.msdd_channel_type),("msdd_installation_name_csv",self.msdd_installation_name_csv),("msdd_registration_name_csv",self.msdd_registration_name_csv),("bits_per_sample",self.bits_per_sample),("adc_meter_values",self.adc_meter_values),("rcvr_gain",self.rcvr_gain),("ddc_gain",self.ddc_gain),("allocated",self.allocated),("input_sample_rate",self.input_sample_rate),("output_channel",self.output_channel),("output_enabled",self.output_enabled),("output_protocol",self.output_protocol),("output_vlan_enabled",self.output_vlan_enabled),("output_vlan_tci",self.output_vlan_tci),("output_flow",self.output_flow),("output_timestamp_offset",self.output_timestamp_offset),("output_endianess",self.output_endianess),("output_mfp_flush",self.output_mfp_flush),("psd_fft_size",self.psd_fft_size),("psd_averages",self.psd_averages),("psd_time_between_ffts",self.psd_time_between_ffts),("psd_output_bin_size",self.psd_output_bin_size),("psd_window_type",self.psd_window_type),("psd_peak_mode",self.psd_peak_mode),("spc_fft_size",self.spc_fft_size),("spc_averages",self.spc_averages),("spc_time_between_ffts",self.spc_time_between_ffts),("spc_output_bin_size",self.spc_output_bin_size),("spc_window_type",self.spc_window_type),("spc_peak_mode",self.spc_peak_mode),("spc_start_frequency",self.spc_start_frequency),("available_tuner_type",self.available_tuner_type),("spc_stop_frequency",self.spc_stop_frequency)]

        connectionTable = structseq_property(id_="connectionTable",
                                             structdef=bulkio.connection_descriptor_struct,
                                             defvalue=[],
                                             configurationkind=("property",),
                                             mode="readonly")



        # Rebind tuner status property with custom struct definition
        frontend_tuner_status = FrontendTunerDevice.frontend_tuner_status.rebind()
        frontend_tuner_status.structdef = frontend_tuner_status_struct_struct

        def frontendTunerStatusChanged(self,oldValue, newValue):
            pass

        def getTunerStatus(self,allocation_id):
            tuner_id = self.getTunerMapping(allocation_id)
            if tuner_id < 0:
                raise FRONTEND.FrontendException(("ERROR: ID: " + str(allocation_id) + " IS NOT ASSOCIATED WITH ANY TUNER!"))
            #return struct_to_props(self.frontend_tuner_status[tuner_id])
            #return [CF.DataType(id=self.frontend_tuner_status[tuner_id].getId(),value=self.frontend_tuner_status[tuner_id]._toAny())]
            _props = self.query([CF.DataType(id='FRONTEND::tuner_status',value=_any.to_any(None))])
            return _props[0].value._v[tuner_id]._v


        def assignListener(self,listen_alloc_id, allocation_id):
            # find control allocation_id
            existing_alloc_id = allocation_id
            if self.listeners.has_key(existing_alloc_id):
                existing_alloc_id = self.listeners[existing_alloc_id]
            self.listeners[listen_alloc_id] = existing_alloc_id

            old_table = self.connectionTable
            new_entries = []
            for entry in self.connectionTable:
                if entry.connection_id == existing_alloc_id:
                    tmp = bulkio.connection_descriptor_struct()
                    tmp.connection_id = listen_alloc_id
                    tmp.stream_id = entry.stream_id
                    tmp.port_name = entry.port_name
                    new_entries.append(tmp)

            for new_entry in new_entries:
                foundEntry = False
                for entry in self.connectionTable:
                    if entry.connection_id == new_entry.connection_id and \
                       entry.stream_id == entry.stream_id and \
                       entry.port_name == entry.port_name:
                        foundEntry = True
                        break

                if not foundEntry:
                    self.connectionTable.append(new_entry)

            self.connectionTableChanged(old_table, self.connectionTable)

        def connectionTableChanged(self, oldValue, newValue):
            self.port_dataSDDS_out.updateConnectionFilter(newValue)
            self.port_dataVITA49_out.updateConnectionFilter(newValue)
            self.port_dataSDDS_out_PSD.updateConnectionFilter(newValue)
            self.port_dataSDDS_out_SPC.updateConnectionFilter(newValue)
            self.port_dataVITA49_out_PSD.updateConnectionFilter(newValue)

        def removeListener(self,listen_alloc_id):
            if self.listeners.has_key(listen_alloc_id):
                del self.listeners[listen_alloc_id]

            old_table = self.connectionTable
            for entry in list(self.connectionTable):
                if entry.connection_id == listen_alloc_id:
                    self.connectionTable.remove(entry)

            # Check to see if port "port_dataSDDS_out" has a connection for this listener
            tmp = self.port_dataSDDS_out._get_connections()
            for i in range(len(self.port_dataSDDS_out._get_connections())):
                connection_id = tmp[i].connectionId
                if connection_id == listen_alloc_id:
                    self.port_dataSDDS_out.disconnectPort(connection_id)
            # Check to see if port "port_dataVITA49_out" has a connection for this listener
            tmp = self.port_dataVITA49_out._get_connections()
            for i in range(len(self.port_dataVITA49_out._get_connections())):
                connection_id = tmp[i].connectionId
                if connection_id == listen_alloc_id:
                    self.port_dataVITA49_out.disconnectPort(connection_id)
            # Check to see if port "port_dataSDDS_out_PSD" has a connection for this listener
            tmp = self.port_dataSDDS_out_PSD._get_connections()
            for i in range(len(self.port_dataSDDS_out_PSD._get_connections())):
                connection_id = tmp[i].connectionId
                if connection_id == listen_alloc_id:
                    self.port_dataSDDS_out_PSD.disconnectPort(connection_id)
            # Check to see if port "port_dataSDDS_out_SPC" has a connection for this listener
            tmp = self.port_dataSDDS_out_SPC._get_connections()
            for i in range(len(self.port_dataSDDS_out_SPC._get_connections())):
                connection_id = tmp[i].connectionId
                if connection_id == listen_alloc_id:
                    self.port_dataSDDS_out_SPC.disconnectPort(connection_id)
            # Check to see if port "port_dataVITA49_out_PSD" has a connection for this listener
            tmp = self.port_dataVITA49_out_PSD._get_connections()
            for i in range(len(self.port_dataVITA49_out_PSD._get_connections())):
                connection_id = tmp[i].connectionId
                if connection_id == listen_alloc_id:
                    self.port_dataVITA49_out_PSD.disconnectPort(connection_id)
            self.connectionTableChanged(old_table, self.connectionTable)


        def removeAllocationIdRouting(self,tuner_id):
            allocation_id = self.getControlAllocationId(tuner_id)
            old_table = self.connectionTable
            for entry in list(self.connectionTable):
                if entry.connection_id == allocation_id:
                    self.connectionTable.remove(entry)

            for key,value in self.listeners.items():
                if (value == allocation_id):
                    for entry in list(self.connectionTable):
                        if entry.connection_id == key:
                            self.connectionTable.remove(entry)

            self.connectionTableChanged(old_table, self.connectionTable)

        def removeStreamIdRouting(self,stream_id, allocation_id):
            old_table = self.connectionTable
            for entry in list(self.connectionTable):
                if allocation_id == "":
                    if entry.stream_id == stream_id:
                        self.connectionTable.remove(entry)
                else:
                    if entry.stream_id == stream_id and entry.connection_id == allocation_id:
                        self.connectionTable.remove(entry)

            for key,value in self.listeners.items():
                if (value == allocation_id):
                    for entry in list(self.connectionTable):
                        if entry.connection_id == key and entry.stream_id == stream_id:
                            self.connectionTable.remove(entry)

            self.connectionTableChanged(old_table, self.connectionTable)

        def matchAllocationIdToStreamId(self,allocation_id, stream_id, port_name):
            if port_name != "":
                for entry in list(self.connectionTable):
                    if entry.port_name != port_name:
                        continue
                    if entry.stream_id != stream_id:
                        continue
                    if entry.connection_id != allocation_id:
                        continue
                    # all three match. This is a repeat
                    return

                old_table = self.connectionTable;
                tmp = bulkio.connection_descriptor_struct()
                tmp.connection_id = allocation_id
                tmp.port_name = port_name
                tmp.stream_id = stream_id
                self.connectionTable.append(tmp)
                self.connectionTableChanged(old_table, self.connectionTable)
                return

            old_table = self.connectionTable;
            tmp = bulkio.connection_descriptor_struct()
            tmp.connection_id = allocation_id
            tmp.port_name = "port_dataSDDS_out"
            tmp.stream_id = stream_id
            self.connectionTable.append(tmp)
            tmp.connection_id = allocation_id
            tmp.port_name = "port_dataVITA49_out"
            tmp.stream_id = stream_id
            self.connectionTable.append(tmp)
            tmp.connection_id = allocation_id
            tmp.port_name = "port_dataSDDS_out_PSD"
            tmp.stream_id = stream_id
            self.connectionTable.append(tmp)
            tmp.connection_id = allocation_id
            tmp.port_name = "port_dataSDDS_out_SPC"
            tmp.stream_id = stream_id
            self.connectionTable.append(tmp)
            tmp.connection_id = allocation_id
            tmp.port_name = "port_dataVITA49_out_PSD"
            tmp.stream_id = stream_id
            self.connectionTable.append(tmp)
            self.connectionTableChanged(old_table, self.connectionTable)

