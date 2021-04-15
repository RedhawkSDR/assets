#!/usr/bin/env python
#
# This file is protected by Copyright. Please refer to the COPYRIGHT file
# distributed with this source distribution.
#
# This file is part of REDHAWK
#
# REDHAWK is free software: you can redistribute it and/or modify it under
# the terms of the GNU Lesser General Public License as published by the Free
# Software Foundation, either version 3 of the License, or (at your option) any
# later version.
#
# REDHAWK is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
# details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.
#
#
#
import copy, math, sys, os
import inspect 
import logging
import subprocess
import traceback
import time as _time
import threading
from datetime import datetime, timedelta
from pprint import pprint as pp
from omniORB import CORBA, any

from MSDD_base import * 
from ossie.cf import ExtendedCF
from bulkio.bulkioInterfaces import BULKIO 
from ossie.device import start_device
from frontend.fe_types import *
from frontend.tuner_device import validateRequestVsRFInfo
from ossie import properties
from time_helpers import *

# MSDD helper files/packages
from msddcontroller import InvalidValue
from msddcontroller import CommandException
from msddcontroller import ConnectionFailure
from msddcontroller import TransmitFailure
from msddcontroller import EchoFailure
from msddcontroller import MSDDRadio
from msddcontroller import create_csv



class BusyException(Exception):
    pass

class AllocationFailure(Exception):
    pass

class MsddException(Exception):
    pass


def increment_ip(ip_string):
    ip_octets = str(ip_string).split(".")
    if len(ip_octets) != 4:
        raise Exception("INVALID IP")
    ip_octets[3] = int(ip_octets[3])
    ip_octets[2] = int(ip_octets[2])
    ip_octets[1] = int(ip_octets[1])
    ip_octets[0] = int(ip_octets[0])

    ip_octets[3] += 1
    if ip_octets[3] > 255:
        ip_octets[3] = 0
        ip_octets[2] += 1
    if ip_octets[2] > 255:
        ip_octets[2] = 0
        ip_octets[1] += 1
    if ip_octets[1] > 255:
        ip_octets[1] = 0
        ip_octets[0] += 1
    if ip_octets[0] > 255:
        raise Exception("CAN NOT INCREMENT ANY FURTHER")
    return str(ip_octets[0]) + '.' + str(ip_octets[1]) + '.' + str(ip_octets[2]) + '.' + str(ip_octets[3])


class output_config(object):
    def __init__(self, tuner_channel_number, enabled, interface, ip_address, port, protocol,vlan_enabled, vlan, timestamp_offset,endianess,mfp_flush):
        self.tuner_channel_number = tuner_channel_number
        self.enabled = enabled
        self.interface = interface
        self.ip_address = ip_address
        self.port = port
        self.protocol = protocol
        self.timestamp_offset = timestamp_offset
        self.vlan = vlan
        self.vlan_enabled = vlan_enabled
        self.endianess = endianess
        self.mfp_flush = mfp_flush
        self.samples_per_frame=512
        self.data_width=16

    def __str__(self):
        return "ch={0} (enb:{1},eth:{2}) ip:port={3}:{4} vlan{5} proto={6} endian={7} mfp={8}".format(self.tuner_channel_number, 
                                                                                                      self.enabled, 
                                                                                                      self.interface,
                                                                                                      self.ip_address,
                                                                                                      self.port, 
                                                                                                      self.vlan, 
                                                                                                      self.protocol, 
                                                                                                      self.endianess,
                                                                                                      self.mfp_flush)
class MSDD_i(MSDD_base):
    FE_TYPE_RECEIVER="RX"
    FE_TYPE_RXDIG="RX_DIGITIZER"
    FE_TYPE_RXDIGCHAN="RX_DIGITIZER_CHANNELIZER"
    FE_TYPE_DDC="DDC"
    FE_TYPE_PSD="PSD"
    FE_DDC_BW=int(1)
    FE_RXDIGCHAN_BW=int(2)
    FE_RXDIG_BW=int(4)
    FE_RX_BW=int(8)
    FE_PSD_BW=int(16)
    VITA49CONTEXTPACKETINTERVAL = 5.0 # 5 Second Interval for sending VITA49 Context Packets


    def _format_msg(self, *args, **kwds):
        """
        Produce a string from the set of arguments and keywords. The following key words
        will control if the MSDD: <ip:port> RCV:x info is added to the resulting string
        
        kwds contains:
           id_prepend - < MSDD id string>, Formatted string
           id_append - Formatted string, <MSDD id string>
           no_id: do not add <MSDD id string>

        """
        add_id=True
        if 'no_id' in kwds:
            add_id=False
        if add_id:
            if 'id_prepend' in kwds:
                return "MSDD({0}), ".format(self.msddReceiverInfo())+ args[0].format(*args[1:])
            if 'id_append' in kwds:
                return args[0].format(*args[1:])+ ", MSDD({0})".format(self.msddReceiverInfo())
        return args[0].format(*args[1:])

    def format_msg(self, *args, **kwds):
        kwds['no_id']=True
        return self._format_msg( *args, **kwds )

    def format_msg_with_radio(self, *args, **kwds):
        if not 'id_append' in kwds:
            kwds['id_append']=True
        return self._format_msg( *args, **kwds )

    def _log_wrapper(self, func,*args, **kwds):
        if not 'id_append' in kwds:
            kwds['id_append']=True
        msg=self._format_msg(*args,**kwds)
        _stack=inspect.stack()
        func("({0}:{1}) - {2}".format(os.path.basename(_stack[2][1]),
                                           _stack[2][2],
                                           msg))

    def fatal_msg(self,*args, **kwds):
        self._log_wrapper(self._baseLog.fatal, *args, **kwds)

    def error_msg(self,*args, **kwds):
        self._log_wrapper(self._baseLog.error, *args, **kwds)

    def warn_msg(self,*args, **kwds):
        self._log_wrapper(self._baseLog.warn, *args, **kwds)

    def info_msg(self,*args, **kwds):
        self._log_wrapper(self._baseLog.info, *args, **kwds)

    def debug_msg(self,*args, **kwds):
        self._log_wrapper(self._baseLog.debug, *args, **kwds)

    def trace_msg(self,*args, **kwds):
        self._log_wrapper(self._baseLog.trace, *args, **kwds)

    def exception_msg(self,*args, **kwds):
        self._log_wrapper(self._baseLog.exception, *args, **kwds)

    def getter_with_default(self,_get_,default_value=None):
        try:
            ret = _get_()
            return ret
        except:
            if default_value != None:
                return default_value
            raise
    
    def extend_tuner_status_struct(self, struct_mod):
        """
        Extend the tuner_status_struct to include extra dynamic properties that reference the
        MSDDRadio's rx_channels, allocation and tuner types
        """
        s = struct_mod
        s.tuner_types=[]
        s.rx_object = None
        s.allocation_id_control = ""
        s.allocation_id_listeners = []
        s.digital_output_info=None
        s.digital_output=False
        s.fft_output_info=None        
        s.fft_output=False        
        return s
    
    def create_allocation_id_csv(self, control_str, listener_list):
        """
        Create allocation id list for controlling and listener allocations
        """
        aid_csv = control_str
        for i in range(0,len(listener_list)):
            aid_csv+=("," + str(listener_list[i]))
        return aid_csv    

    def constructor(self):
        _stime_ctor=time.time()
        self.msdd_lock=threading.Lock()
        self.MSDD = None
        self.msdd_console = None
        self.device_rf_flow = ""
        self.device_rf_info_pkt=None
        self.dig_tuner_base_nums= []
        self.tuner_output_configuration={}
        self.psd_output_configuration={}
        self.msdd_status=MSDD_base.msdd_status_struct()
        self.frontend_tuner_status = []
        self.tuner_allocation_ids = []
        self.rx_channel_tuner_status={}
        self.receiver_identifier=None            # set in connect_to_msdd, allow for filtering of rx_channels with multi-channel radios
        self._enableTimeChecks=False             # process method is auto started.. disable time variance checks until time of day module is configured
        self._nextTimeCheck=None

        # determine data output protocol from tuner_output properties required for to configure the timing source
        self.output_protocol=self.determine_output_protocol()
        self.info_msg("Protocol for output streams will be {0}", self.output_protocol.upper())


        # connect to msdd and check network status
        if not self.connect_to_msdd():
            # connection failure raise initialization error
            raise CF.LifeCycle.InitializeError( self.format_msg_with_radio("Failure to establish connection."))
        
        # apply reference clock settings on radio
        try:
            self.process_clock_ref()
        except:
            traceback.print_exc()
            raise CF.LifeCycle.InitializeError( self.format_msg_with_radio("Failure to process clock reference value {0} ", 
                                                                self.msdd.clock_ref))
        # update msdd status properties
        _stime=time.time()        
        self.update_msdd_status()
        _etime=time.time()-_stime
        self.debug_msg("Initialized MSDD status properties, {:.7f}",_etime )
        
        # create tuner status struct using MSDDRadio's rx_channels
        _stime=time.time()        
        self.update_tuner_status()
        _etime=time.time()-_stime
        self.debug_msg("Created tuner status entries, {:.7f}",_etime )  

        _stime=time.time()        
        self.create_output_configuration()
        _etime=time.time()-_stime
        self.debug_msg("Determined output configuration for tuners and fft channels, {:.7f}",_etime )  

        # assigns output configurations to status structure
        self.update_tuner_status()
        self.debug_msg("Assigned output configuration to tuner status structure")                

        _stime=time.time()                
        for tuner_num in range(0,len(self.frontend_tuner_status)):
            try:
                self.frontend_tuner_status[tuner_num].rx_object.setEnable(False)
            except:
                self.warn_msg("Disabling tuner failed Tuner {0}", tuner_num)
        _etime=time.time()-_stime
        self.info_msg("Disabled all tuner hardware and output, {:.7f}",_etime )
        
        self.addPropertyChangeListener("advanced",self.advanced_changed)

        # initialize time of day module with TimeOfDay class
        self._time_of_day=TimeOfDay(self.MSDD.get_timeofday_module(),
                                  self,       
                                  self.enable_time_checks, 
                                  self.host_delta_cb, 
                                  self.ntp_status_cb)
        self._time_of_day.initialize(self.time_of_day, self.output_protocol, enable_thread=True)

        # Set connection timeout
        if self.MSDD:
            self.MSDD.connection.set_timeout(self.msdd.timeout)
            
        _etime=time.time()-_stime_ctor
        self.info_msg("Completed MSDD initialization {:.7f}",_etime )            

    def identifyModel(self):
        return "{0} ({1})".format(self.device_address,self.device_model)

    def msddReceiverInfo(self):
        return "{0}:{1}/{2}".format(self.msdd.ip_address, self.msdd.port, self.receiver_identifier)

    def updateUsageState(self):
        """
        This is called automatically after allocateCapacity or deallocateCapacity are called.
        Your implementation should determine the current state of the device:
           self._usageState = CF.Device.IDLE   # not in use
           self._usageState = CF.Device.ACTIVE # in use, with capacity remaining for allocation
           self._usageState = CF.Device.BUSY   # in use, with no capacity remaining for allocation, or MSDD radio is in a busy state
        """
        try:
            state =  FrontendTunerDevice.updateUsageState(self)
            self.debug_msg("updateUsageStat device state: {0}",state)
            if state != CF.Device.BUSY:
                try:
                    # check cpu usage of radio and network utilization
                    self.debug_msg("updateUsageStat checking cpu load")
                    if not self.checkReceiverCpuLoad(self.advanced.max_cpu_load):
                        state=CF.Device.BUSY

                    self.trace_msg("updateUsageState after CPU load,  usage state: {0}",state)
                except:
                    pass
        finally:
            pass
        
        return state

    
    def checkReceiverCpuLoad(self, max_cpu_load):
        """
        Get the current cpu load on radio and check if limit is within specified load maximum
        """
        try:
            self.msdd_lock.acquire()
            load=0.0
            if self.msdd_console:
                try:
                    load=self.msdd_console.cpu_load

                    self.info_msg('Check CPU Load, actual load {0} max {1}',load, max_cpu_load)
                    if load > max_cpu_load:
                        self.warn_msg('CPU exceeds maximum load setting {0}',max_cpu_load)
                        return False
                    return True
                except Exception as e:
                    self.warn_msg("Unable to get CPU load, reason {}",e)
                    return False
            else:
                return True
        finally:
            self.msdd_lock.release()
            pass

    def calcModuleBitRate(self, module_info,srate=None):
        pkt_bit_rate=0
        inf=0
        if module_info:
            inf = module_info.interface
            if not srate:
                srate=module_info.input_sample_rate # get input rate into the output module
            spf=module_info.samples_per_frame                # radio report scalars
            pkt_data_len = module_info.packet_data_len       # total length of packet in bytes
            data_width =module_info.data_width               # sample length in bits of scalar
            proto_name=module_info.protocol                  # output protocol 
            proto_extra_bits=0                            # default for raw
            pkt_rate =  srate/spf
            if "SDDS" in proto_name:
                pkt_header=56 # SDDS header length (56 bytes)
            if "VITA49" in proto_name:
                pkt_header = 28  # IF data packet header in bits (7 words (32bits) )
                # resolve, need to add in context packets, initial testing shows no context packets..                    
            pkt_bit_rate = pkt_rate*pkt_data_len*(data_width) + pkt_rate*pkt_header*8
            self.debug_msg("out module bitrate interface {} module id {} "\
                          "srate {} sample-per-frame {} data_width {} pkt_rate (srate/spf) {}",
                          inf,
                          module_info.channel_number,
                          srate, 
                          spf, 
                          data_width,
                          pkt_rate)
        return pkt_bit_rate, inf


    def checkNetworkOutput(self, additional_rate=None, out_module=None ):
        """
        check all enabled output modules if they are within the recommend rate for the radio's network interface.
        if the out_module parameter is specified then include that module's expected bit rate

        additional_rate = include this specific sample rate for the out_module parameter
        out_module = include the output module's bit rate if the module is disable
        """
        try:
            self.msdd_lock.acquire()
            # hold calculated totals for all the network interfaces on the radio
            netstat=[]
            netstat_fail=False
            enabled_modules=0
            if additional_rate is None:
                additional_rate=0.0

            if self.MSDD:
                try:
                    # seed netstat like list for each defined network interface
                    for  n in self.MSDD.network_modules:
                        try:
                            idx=self.MSDD.network_modules.index(n)
                            netstat.append( { 'interface' : idx,
                                              'total' : 0.0,
                                              'bit_rate' : n.object.bit_rate*1e6
                                          })
                        except:
                            if logging.NOTSET < self._baseLog.level < logging.INFO:
                                traceback.print_exc()

                    self.debug_msg('checkReceiverNetworkOutput receiver interfaces {0}', netstat)

                    #
                    # Determine network output rate based on tuners with
                    # digital output.
                    #
                    for tuner in self.frontend_tuner_status:
                        pkt_bit_rate=0
                        inf=0
                        if tuner.rx_object.output_object and \
                            tuner.rx_object.output_object == out_module:
                            pkt_bit_rate, inf = self.calcModuleBitRate(tuner.digital_output_info,additional_rate)
                            enabled_modules+=1

                        if tuner.digital_output:
                            pkt_bit_rate, inf = self.calcModuleBitRate(tuner.digital_output_info)
                            enabled_modules+=1
                            
                        cid="--missing--"
                        if tuner.digital_output_info:
                            cid=tuner.digital_output_info.channel_number
                        
                        self.debug_msg("Check network output: " \
                                       "interface {} out module {} pkt_bit_rate {} total netstat {} ",
                                       inf,
                                       cid,
                                       pkt_bit_rate, 
                                       netstat[inf])
                        
                        netstat[inf]['total'] += pkt_bit_rate

                        # Add in FFT output for channel if enabled
                        if tuner.fft_output and tuner.fft_output_info:
                            pkt_bit_rate, inf = self.calcModuleBitRate(tuner.fft_output_info)
                            netstat[inf]['total'] += pkt_bit_rate


                    # check all netstats
                    for n in netstat:
                        max_allowable_bit_rate=n['bit_rate'] * (self.advanced.max_nic_percentage/100.0)
                        self.info_msg("MSDD interface rate check, interface {} enabled modules {}  "\
                                           "total rate {} (bps) max allowable rate {} (bps) exceeds: {}",
                                           n['interface'],
                                           enabled_modules,
                                           n['total'],
                                           max_allowable_bit_rate,
                                           n['total'] > max_allowable_bit_rate)

                        if n['total'] > max_allowable_bit_rate:
                            self.error_msg("Network rate exceeded, interface {0} "\
                                           "total rate {1} (bps)  max allowable rate {2} (bps)",
                                           n['interface'], 
                                           n['total'],
                                           max_allowable_bit_rate)
                            return False
                except Exception as e:
                    traceback.print_exc()
                    if logging.NOTSET < self._baseLog.level < logging.INFO:
                        traceback.print_exc()
                    self.error_msg("Unable to determine network utilization, reason {} ",e)
                    netstat_fail=True

            # check if io exception occured during netstat operation
            if netstat_fail:
                return False

        finally:
            self.msdd_lock.release()

        return True
    

    def disconnect_from_msdd(self):
        if self.MSDD != None:

            self.info_msg('Disconnecting from radio')

            #Need to tear down old devices here if needed
            for t in range(0,len(self.frontend_tuner_status)):
                try:
                    if self.frontend_tuner_status[t].enabled:
                        self.frontend_tuner_status[t].enabled = False
                        self.purgeListeners(t)
                except IndexError:
                    self.warn_msg('Disconnect failed, Missing tuner {0}',t)
                    break

        # reset context to device
        self.MSDD=None
        if self._time_of_day:
            self._time_of_day.tod_module=None
        self.update_msdd_status()
        self.update_tuner_status()
    
    def connect_to_msdd(self):
        if self.MSDD == None:
            self.msdd_console=None
            try:
                self.info_msg("Connecting to radio")
                self.MSDD = MSDDRadio(self.msdd.ip_address,
                                      self.msdd.port,
                                      udp_timeout=min(0.2,self.msdd.timeout),
                                      enable_fft_channels=self.advanced.enable_fft_channels,
                                      radio_debug=False)

                
                rate_failure = None
                for net_mods in self.MSDD.network_modules:
                    connected_rate = net_mods.object.bit_rate
                    if connected_rate < self.advanced.minimum_connected_nic_rate:
                        rate_failure = connected_rate

                if rate_failure:
                        self.warn_msg("Connection rate of {0} " \
                                      " Mbps which does not meet minimum rate check, {1} Mbps",
                                      rate_failure,
                                      self.advanced.minimum_connected_nic_rate, id_prepend=True)
                        raise Exception(self.format_msg_with_radio("Connection rate check failure, minimum rate {0}",
                                                                   self.advanced.minimum_connected_nic_rate,
                                                                   id_prepend=True))

                self.msdd_console = self.MSDD.console
                # assign first channel as receiver identifier
                try:
                    # if receiver id is not provided then default to first receiver channels RCV obj
                    if self.msdd.receiver_id == "" or not self.msdd.receiver_id:
                        self.receiver_identifier=self.MSDD.rx_channels[0].msdd_channel_id()
                    else:
                        self.receiver_identifier = self.msdd.receiver_id
                    self.msdd_status.receiver_identifier=self.receiver_identifier
                except:
                    pass

            except:
                if logging.NOTSET < self._baseLog.level < logging.INFO:
                    traceback.print_exc()
                self.exception_msg("Unable to connect to MSDD, IP:PORT, {0}:{1} ",self.msdd.ip_address,self.msdd.port)
                self.disconnect_from_msdd()
                return False

        return True    

    def enable_time_checks(self, time_was_set=False):
        """
        Callback provided to TimeHelper to signal that initial time synchronization has completed
        """
        self._enableTimeChecks=time_was_set
        # check if we should be tracking time variances
        if self.time_of_day.tracking_interval > 0.0 :
            self._nextTimeCheck=time.time()+self.time_of_day.tracking_interval

    def host_delta_cb(self, new_delta):
        """
        Callback provided to TimeHelper to record the new delta between host time and the radio
        """
        try:
            self.trace_msg('host_delta_cb delta {0}',new_delta)
            self.msdd_status.tod_host_delta = new_delta
        except:
            traceback.print_exc()

    def ntp_status_cb(self, is_running):
        """
        Callback provided to TimeHelper to determine if NTP service is running
        """
        self.msdd_status.ntp_running = is_running


    def determine_output_protocol(self, default_proto='sdds'):
        """
        Determine output module protocol from configuration settings
        """
        proto=default_proto
        for cfg in self.block_tuner_output:
            if 'vita' in str(cfg.protocol).lower():
                proto='vita49'
        for cfg in self.tuner_output:
            if 'vita' in str(cfg.protocol).lower():
                proto='vita49'
        return proto


    def process_clock_ref(self):
        """
        Process external clock reference settings, this happens once during device startup
        """
        if self.MSDD == None:
            return
        try:
            idn=self.MSDD.msdd_id 
            self.info_msg('Setting external reference clock to {} for msdd id {} ',self.msdd.clock_ref, idn)
            
            # for the 660D dual channel receiver, use the board module to determine/configure reference clock
            if '0660D' in idn[0]:
                for board_num in range(0,len(self.MSDD.board_modules)):
                    self.MSDD.board_modules[board_num].object.setExternalRef(self.msdd.clock_ref)
            else:
                # all others, if a RX channel is analog the set it's reference clock
                for rx_chan in self.MSDD.rx_channels:
                    if rx_chan.is_analog():
                        rx_chan.analog_rx_object.object.setExternalRef(self.msdd.clock_ref)
        except Exception as e:
            self.error_msg('Exception occurred on MSDD {} ' \
                          ' when trying to configure external reference clock to {}, reason {} ',
                           self.msddReceiverInfo(),
                           self.msdd.clock_ref,
                           e,
                           no_id=True)

    def update_rf_flow_id(self, rf_flow_id):
        """
        Called when RF Flow ID is passed to this device, update all the tuner_status objects
        """
        self.info_msg('Setting rf_flow_id to {0}',rf_flow_id)
	self.device_rf_flow = rf_flow_id
	for tuner_num in range(0,len(self.frontend_tuner_status)):
	    self.frontend_tuner_status[tuner_num].rf_flow_id = self.device_rf_flow


    def update_msdd_status(self):
        """
        Update the MSDD status structure. This only happens once during device startup
        """
        if self.MSDD == None:
            self.msdd_status=MSDD_base.msdd_status_struct() #Reset to defaults
            return
        try:
            self.device_model=str(self.msdd_console.model)
            self.device_address = self.msdd_console.address
            self.msdd_status.connected = True
            self.msdd_status.ip_address = str(self.msdd.ip_address)
            self.msdd_status.port = str(self.msdd.port)
            self.msdd_status.control_host = str(self.msdd_console.ip_address)
            self.msdd_status.control_host_port = str(self.msdd_console.ip_port)
            self.msdd_status.model = str(self.msdd_console.model)
            self.msdd_status.serial = str(self.msdd_console.serial)
            self.msdd_status.software_part_number = str(self.msdd_console.sw_part_number)
            self.msdd_status.rf_board_type = str(self.msdd_console.rf_board_type)
            self.msdd_status.fpga_type = str(self.msdd_console.fpga_type)
            self.msdd_status.dsp_type = str(self.msdd_console.dsp_type)
            self.msdd_status.minimum_frequency_hz = str(self.msdd_console.minFreq_Hz)
            self.msdd_status.maximum_frequency_hz = str(self.msdd_console.maxFreq_Hz)
            self.msdd_status.dsp_reference_frequency_hz = str(self.msdd_console.dspRef_Hz)
            self.msdd_status.adc_clock_frequency_hz = str(self.msdd_console.adcClk_Hz)
            self.msdd_status.num_if_ports = str(self.msdd_console.num_if_ports)
            self.msdd_status.num_eth_ports = str(self.msdd_console.num_eth_ports)
            self.msdd_status.cpu_type = str(self.msdd_console.cpu_type)
            self.msdd_status.cpu_rate = str(self.msdd_console.cpu_rate)
            self.msdd_status.cpu_load = str(self.msdd_console.cpu_load)
            self.msdd_status.pps_termination = str(self.msdd_console.pps_termination)
            self.msdd_status.pps_voltage = str(self.msdd_console.pps_voltage)
            self.msdd_status.number_wb_ddc_channels = str(self.msdd_console.num_wb_channels)
            self.msdd_status.number_nb_ddc_channels = str(self.msdd_console.num_nb_channels)
            self.msdd_status.filename_app = str(self.msdd_console.filename_app)
            self.msdd_status.filename_fpga = str(self.msdd_console.filename_fpga)
            self.msdd_status.filename_batch = str(self.msdd_console.filename_batch)
            self.msdd_status.filename_boot = str(self.msdd_console.filename_boot)
            self.msdd_status.filename_loader = str(self.msdd_console.filename_loader)
            self.msdd_status.filename_config = str(self.msdd_console.filename_config)
            self.msdd_status.filename_cal = str(self.msdd_console.filename_cal)
            tod = self.MSDD.get_timeofday_module()
            if tod:
                self.msdd_status.tod_module = tod.mode_readable
                self.msdd_status.tod_available_module = tod.available_modes
                self.msdd_status.tod_meter_list = tod.meter_readable
                self.msdd_status.tod_tod_reference_adjust = str(tod.ref_adjust)
                self.msdd_status.tod_track_mode_state = str(tod.ref_track)
                self.msdd_status.tod_bit_state = tod.getBITStr(None)
                self.msdd_status.tod_toy = str(tod.toy)

            self.trace_msg("Completed update msdd_status")
        except:
            if logging.NOTSET < self._baseLog.level < logging.INFO:
                traceback.print_exc()
            self.error_msg("Unable to update msdd_status property")
    

    def getTunerTypeList(self,type_mode):
        """
        Parameter type_mode is a bitmap to a list of frontend tuner types that a MSDD tuner channel can
        be allocated against (see advanced::xxx_mode properites). This function expands the bitmap a
        a list frontend tuner types that are matched against during an allocation.
        """
        tuner_lst = []
        if (type_mode & self.FE_RX_BW) == self.FE_RX_BW:
            tuner_lst.append(self.FE_TYPE_RECEIVER)
        if (type_mode & self.FE_PSD_BW) == self.FE_PSD_BW:
            tuner_lst.append(self.FE_TYPE_PSD)
        if (type_mode & self.FE_DDC_BW) == self.FE_DDC_BW:
            tuner_lst.append(self.FE_TYPE_DDC)
        if (type_mode & self.FE_RXDIG_BW) == self.FE_RXDIG_BW:
            tuner_lst.append(self.FE_TYPE_RXDIG)
        if (type_mode & self.FE_RXDIGCHAN_BW) == self.FE_RXDIGCHAN_BW:
            tuner_lst.append(self.FE_TYPE_RXDIGCHAN)
        return tuner_lst


    def _initialize_tuners(self):
        """
        Create a list of frontend_tuner_status objects from list of MSDDRadio rx_channel objects
        assigned to the MSDD based on the receiver_identifier property.
        """
        self.dig_tuner_base_nums = []
        fe_tuner_num=0
        
        #
        # loop through all MSDDRadio's rx_channels
        #
        for tuner_num in range(0,len(self.MSDD.rx_channels)):            

            rx_chan = self.MSDD.rx_channels[tuner_num]

            #
            # filter tuners based on the receiver_identifier. The get_root_method is used to
            # determine which RCV:x channel is feeding the rx_chan object.
            # 
            if self.receiver_identifier:
                root_rx_chan = self.MSDD.get_root_parent(rx_chan)
                if self.receiver_identifier != root_rx_chan.msdd_channel_id():
                    self.debug_msg("Skipping tuner {0}", rx_chan.msdd_channel_id())
                    
                    continue
            else:
                self.error_msg('Configuration error, msdd::receiver_id was not configured')
                raise CF.LifeCycle.InitializeError(self.format_msg_with_radio('Configuration error,' \
                                                                           ' msdd::receiver_id is missing'))

            self.debug_msg("Adding tuner {0} for receiver {1}",
                           rx_chan.msdd_channel_id(),
                           self.receiver_identifier )
                                


            #Determine frontend tuner type using advanced property settings
            tuner_types=[]
            msdd_type_string = self.MSDD.rx_channels[tuner_num].get_msdd_type_string()
            if msdd_type_string == MSDDRadio.MSDDRXTYPE_FFT:
                continue
            elif msdd_type_string == MSDDRadio.MSDDRXTYPE_ANALOG_RX:
                tuner_types = self.getTunerTypeList(self.advanced.rcvr_mode)
            elif msdd_type_string == MSDDRadio.MSDDRXTYPE_DIGITAL_RX:
                self.dig_tuner_base_nums.append(tuner_num)
                tuner_types = self.getTunerTypeList(self.advanced.wb_ddc_mode)
            elif msdd_type_string == MSDDRadio.MSDDRXTYPE_HW_DDC:
                tuner_types = self.getTunerTypeList(self.advanced.hw_ddc_mode)
            elif msdd_type_string == MSDDRadio.MSDDRXTYPE_SW_DDC:
                continue
            elif msdd_type_string == MSDDRadio.MSDDRXTYPE_SPC:
                continue
            else:
                self.error_msg("UNKOWN tuner type {0}", msdd_type_string)

            # skip tuners that have no tuner types
            if len(tuner_types) == 0 : continue

            # create a default tuner type with the first entry in the list
            tuner_type = ""
            if len(tuner_types) > 0:
                    tuner_type = tuner_types[0]

            #
            # Create frontend tuner status structure for this rx channel
            # 
            tuner_struct = MSDD_base.frontend_tuner_status_struct_struct(tuner_type=tuner_type,
                                                          available_tuner_type = create_csv(tuner_types),
                                                          msdd_registration_name_csv= self.MSDD.rx_channels[tuner_num].get_registration_name_csv(),
                                                          msdd_installation_name_csv= self.MSDD.rx_channels[tuner_num].get_install_name_csv(),
                                                          tuner_number=fe_tuner_num,
                                                          msdd_channel_type = msdd_type_string)

            #
            # Add in custom attributes to assist with looking up info
            # 
            tuner_struct = self.extend_tuner_status_struct(tuner_struct)                          
            tuner_struct.tuner_types = tuner_types
            tuner_struct.rx_object = self.MSDD.rx_channels[tuner_num]
            self.debug_msg(" Adding tuner: {0} " \
                           " type: {1}:{2} " \
                           " msdd channel flow: {3}",
                           tuner_num,
                           tuner_type,
                           ",".join(tuner_types),
                           tuner_struct.rx_object.get_digital_signal_path())

            #
            # Update mapping from rx_channel to frontend tuner id
            # 
            self.rx_channel_tuner_status[tuner_struct.rx_object]=fe_tuner_num
            self.frontend_tuner_status.append(tuner_struct)
            fe_tuner_num += 1

    def update_tuner_status(self, tuner_range=[]):
        try:
            self.allocation_id_mapping_lock.acquire()
            self._update_tuner_status(tuner_range)
        finally:
            self.allocation_id_mapping_lock.release()

    def _update_tuner_status(self, tuner_range=[]):
        if self.MSDD == None:
            self.frontend_tuner_status = []
            return True

        #
        # Make sure status structure is initialized
        #
        if len(self.frontend_tuner_status) <= 0:
            _stime=time.time()
            self._initialize_tuners()
            _etime=time.time()-_stime
            self.debug_msg("Initialized tuner status structure {:.7f}",_etime)

        # Update list base on provide set of tuner or all of them
        if len(tuner_range) <= 0:
            tuner_range = range(0,len(self.frontend_tuner_status))
            self.trace_msg("No tuner range provided, updating all {0} tuners",len(tuner_range))

        for tuner_num in tuner_range:
            _stime=time.time()
            try:
                data_port = True
                fft_port = False
                #for just analog tuners
                status = self.frontend_tuner_status[tuner_num].rx_object.getStatus()
                if self.frontend_tuner_status[tuner_num].rx_object.is_analog() and not self.frontend_tuner_status[tuner_num].rx_object.is_digital():
                    self.trace_msg("update_tuner_status update ANALOG tuner settings for tuner {0}",
                                   tuner_num)
                    self.frontend_tuner_status[tuner_num].center_frequency = self.convert_if_to_rf(status.center_frequency)
                    self.frontend_tuner_status[tuner_num].available_frequency = status.available_frequency
                    self.frontend_tuner_status[tuner_num].gain = status.gain
                    self.frontend_tuner_status[tuner_num].available_gain = status.available_gain
                    self.frontend_tuner_status[tuner_num].attenuation = status.attenuation
                    self.frontend_tuner_status[tuner_num].available_attenuation = status.available_attenuation
                    self.frontend_tuner_status[tuner_num].adc_meter_values = status.adc_meter_values;
                    self.frontend_tuner_status[tuner_num].bandwidth = status.bandwidthd
                    self.frontend_tuner_status[tuner_num].available_bandwidth = status.available_bandwidth
                    self.frontend_tuner_status[tuner_num].rcvr_gain = status.gain
                #for just digital tuners
                elif self.frontend_tuner_status[tuner_num].rx_object.is_digital() and not self.frontend_tuner_status[tuner_num].rx_object.is_analog():
                    self.trace_msg("update_tuner_status update DIGITAL tuner settings for tuner {0}",
                                   tuner_num)
                    self.frontend_tuner_status[tuner_num].enabled = status.enabled
                    self.frontend_tuner_status[tuner_num].decimation = status.decimation
                    self.frontend_tuner_status[tuner_num].available_decimation = status.available_decimation
                    self.frontend_tuner_status[tuner_num].attenuation = status.attenuation
                    self.frontend_tuner_status[tuner_num].available_attenuation = status.available_attenuation
                    self.frontend_tuner_status[tuner_num].gain = status.gain
                    self.frontend_tuner_status[tuner_num].available_gain = status.available_gain
                    self.frontend_tuner_status[tuner_num].input_sample_rate = status.input_sample_rate
                    self.frontend_tuner_status[tuner_num].sample_rate = status.sample_rate
                    self.frontend_tuner_status[tuner_num].available_sample_rate = status.available_sample_rate
                    self.frontend_tuner_status[tuner_num].bandwidth = status.bandwidth
                    self.frontend_tuner_status[tuner_num].available_bandwidth = status.available_bandwidth
                    self.frontend_tuner_status[tuner_num].center_frequency = self.convert_if_to_rf(status.center_frequency)
                    self.frontend_tuner_status[tuner_num].available_frequency = status.available_frequency
                    self.frontend_tuner_status[tuner_num].ddc_gain = status.gain
                    self.frontend_tuner_status[tuner_num].complex = True
                #if the tuner is both analog and digital (usually rx_digitizer mode)
                elif self.frontend_tuner_status[tuner_num].rx_object.is_digital() and  self.frontend_tuner_status[tuner_num].rx_object.is_analog():
                    self.trace_msg("update_tuner_status update ANALOG/DIGITAL tuner settings for tuner {0}",
                                   tuner_num,)
                    self.frontend_tuner_status[tuner_num].enabled = status.enabled
                    self.frontend_tuner_status[tuner_num].decimation = status.decimation
                    self.frontend_tuner_status[tuner_num].available_decimation = status.available_decimation
                    self.frontend_tuner_status[tuner_num].attenuation = status.attenuation
                    self.frontend_tuner_status[tuner_num].available_attenuation = status.available_attenuation
                    self.frontend_tuner_status[tuner_num].gain = status.gain
                    self.frontend_tuner_status[tuner_num].available_gain = status.available_gain
                    self.frontend_tuner_status[tuner_num].input_sample_rate = status.input_sample_rate
                    self.frontend_tuner_status[tuner_num].sample_rate = status.sample_rate
                    self.frontend_tuner_status[tuner_num].available_sample_rate = status.available_sample_rate
                    self.frontend_tuner_status[tuner_num].bandwidth = status.bandwidth
                    self.frontend_tuner_status[tuner_num].available_bandwidth = status.available_bandwidth
                    self.frontend_tuner_status[tuner_num].center_frequency = self.convert_if_to_rf(status.center_frequency)
                    self.frontend_tuner_status[tuner_num].available_frequency = status.available_frequency
                    self.frontend_tuner_status[tuner_num].ddc_gain = status.gain
                    self.frontend_tuner_status[tuner_num].complex = True
                #if the tuner has streaming output
                if self.frontend_tuner_status[tuner_num].rx_object.has_streaming_output():
                    status = self.frontend_tuner_status[tuner_num].rx_object.getOutputStatus()
                    self.trace_msg("update_tuner_status update STREAMING context for tuner {0}", tuner_num)
                    if status:
                        self.frontend_tuner_status[tuner_num].output_format = "CI"
                        self.frontend_tuner_status[tuner_num].output_multicast = status.output_multicast
                        self.frontend_tuner_status[tuner_num].output_port = status.output_port
                        self.frontend_tuner_status[tuner_num].output_enabled = status.output_enabled
                        self.frontend_tuner_status[tuner_num].output_protocol = status.output_protocol
                        self.frontend_tuner_status[tuner_num].output_vlan_enabled = status.output_vlan_enabled
                        self.frontend_tuner_status[tuner_num].output_vlan = status.output_vlan
                        self.frontend_tuner_status[tuner_num].output_flow = status.output_flow
                        self.frontend_tuner_status[tuner_num].output_timestamp_offset = str(status.output_timestamp_offset)
                        self.frontend_tuner_status[tuner_num].output_channel = str(status.output_channel)
                        self.frontend_tuner_status[tuner_num].output_endianess = status.output_endianess
                        self.frontend_tuner_status[tuner_num].output_mfp_flush = status.output_mfp_flush
                if self.frontend_tuner_status[tuner_num].rx_object.hasFFTChannel():
                    status = self.frontend_tuner_status[tuner_num].rx_object.getFFTStatus()
                    if status:
                        fft_port = True
                        self.frontend_tuner_status[tuner_num].psd_fft_size = status.fftSize
                        self.frontend_tuner_status[tuner_num].psd_averages = status.num_averages
                        self.frontend_tuner_status[tuner_num].psd_time_between_ffts = status.time_between_fft_ms
                        self.frontend_tuner_status[tuner_num].psd_output_bin_size = status.outputBins
                        self.frontend_tuner_status[tuner_num].psd_window_type = status.window_type_str
                        self.frontend_tuner_status[tuner_num].psd_peak_mode = status.peak_mode_str
                self.SRIchanged(tuner_num, data_port, fft_port )
                self.trace_msg("update_tuner_status, completed tuner status update for tuner {0}",
                               tuner_num)
            except Exception, e:
                self.error_msg("Error updating Tuner status {0}, exception {1}", tuner_num,e)
            _etime=time.time()-_stime
            self.trace_msg("Updated tuner {} status, {:.7f}",tuner_num, _etime)            


    def create_output_configuration(self):
        """
        Create tuner output configuration objects from the tuner_output, and block_tuner_output
        device properties. Then for each tuner definition in the frontend_tuner_status list
        assign a configuration to the tuner and set the radio's output module for the rx_channel
        assigned to the frontend_tuner_status struct

        Create output configuration for all block_psd_output defintions and then assign those
        configuration to any created FFT channels
        """

        self.debug_msg("create_output_configuration")
        if self.MSDD == None:
            return False

        self.tuner_output_configuration={}
        
        self.debug_msg("Setting up output configuration "\
                       "from {0} block_tuner_configurations",
                       len(self.block_tuner_output))

        #
        # Process output block configuration first
        #
        for block_oc_num in range(0,len(self.block_tuner_output)):
            min_num = self.block_tuner_output[block_oc_num].tuner_number_start
            max_num = self.block_tuner_output[block_oc_num].tuner_number_stop
            if max_num < min_num:
                max_num = min_num
            
            ip_address=self.block_tuner_output[block_oc_num].ip_address
            port=self.block_tuner_output[block_oc_num].port
            vlan_enabled=(self.block_tuner_output[block_oc_num].vlan > 0 and self.block_tuner_output[block_oc_num].vlan_enable)
            vlan=max(0,self.block_tuner_output[block_oc_num].vlan)
            
            for oc_num in range(min_num,max_num+1):
                self.tuner_output_configuration[oc_num] = output_config(tuner_channel_number=oc_num,
                                                                        enabled=self.block_tuner_output[block_oc_num].enabled,
                                                                        interface=self.block_tuner_output[block_oc_num].interface,
                                                                        ip_address= str(ip_address).strip(),
                                                                        port=port,
                                                                        vlan_enabled=vlan_enabled,
                                                                        vlan= vlan,
                                                                        protocol=self.block_tuner_output[block_oc_num].protocol,
                                                                        endianess = self.block_tuner_output[block_oc_num].endianess,
                                                                        timestamp_offset=self.block_tuner_output[block_oc_num].timestamp_offset,
                                                                        mfp_flush=self.block_tuner_output[block_oc_num].mfp_flush,
                )
                if self.block_tuner_output[block_oc_num].increment_ip_address:
                    try:
                        ip_address = increment_ip(ip_address)
                    except:
                        self.error_msg("Block tuner configuration error, "\
                                       "output port address range error ip address {0} "\
                                       "block configuration {1}",
                                       ip_address,
                                       block_oc_num)
                        continue

                if self.block_tuner_output[block_oc_num].increment_port:
                    port +=1
                if self.block_tuner_output[block_oc_num].increment_vlan and vlan >= 0:
                    vlan +=1

        #
        # Process individual tuner_outputs, these will override any block definitions
        #
        self.debug_msg("Setting up tuner output configuration "\
                       "from {0} tuner_output configs",
                       len(self.tuner_output))

        for oc_num in range(0,len(self.tuner_output)):
                tuner_num = self.tuner_output[oc_num].tuner_number
                vlan_enabled=(self.tuner_output[oc_num].vlan > 0 and self.tuner_output[oc_num].vlan_enable)
                vlan=max(0,self.tuner_output[oc_num].vlan)
                self.tuner_output_configuration[tuner_num] = output_config(tuner_channel_number=tuner_num,
                                                                           enabled=self.tuner_output[oc_num].enabled,
                                                                           interface=self.tuner_output[oc_num].interface,
                                                                           ip_address=str(self.tuner_output[oc_num].ip_address).strip(),
                                                                           port=self.tuner_output[oc_num].port,
                                                                           vlan_enabled=vlan_enabled,
                                                                           vlan=vlan,
                                                                           protocol=self.tuner_output[oc_num].protocol,
                                                                           endianess = self.tuner_output[oc_num].endianess,
                                                                           timestamp_offset=self.tuner_output[oc_num].timestamp_offset,
                                                                           mfp_flush=self.tuner_output[oc_num].mfp_flush)
    

        self.debug_msg("Configuring MSDD output modules, Tuners {0} Output Configs {1}",
                       len(self.frontend_tuner_status),
                       len(self.tuner_output_configuration))

        for tuner_num in range(0,len(self.frontend_tuner_status)):
            try:
                _rx_object = self.frontend_tuner_status[tuner_num].rx_object
                try:
                    # disable the output module for the rx_channel
                    _rx_object.set_output_enable(False)
                except Exception, e:
                    self.error_msg("Unable to disable output for Tuner {}, reason {}",tuner_num, e, no_id=True)
                    raise e

                if not _rx_object.has_streaming_output():
                    continue

                _output_mod = _rx_object.output_object.object
                
                # if there is no configuration provided then skip that tuner
                if not self.tuner_output_configuration.has_key(tuner_num):
                    continue

                if self.tuner_output_configuration[tuner_num].protocol=="UDP_VITA49":                
                    _output_mod.configureVita49(False,
                                                self.tuner_output_configuration[tuner_num].interface,
                                                self.tuner_output_configuration[tuner_num].ip_address,
                                                self.tuner_output_configuration[tuner_num].port,
                                                self.tuner_output_configuration[tuner_num].vlan,
                                                self.tuner_output_configuration[tuner_num].vlan_enabled,
                                                self.tuner_output_configuration[tuner_num].endianess,
                                                self.tuner_output_configuration[tuner_num].timestamp_offset,
                                                self.tuner_output_configuration[tuner_num].mfp_flush,
                                                self.VITA49CONTEXTPACKETINTERVAL)
                else:
                    _output_mod.configureSDDS(False,
                                              self.tuner_output_configuration[tuner_num].interface,
                                              self.tuner_output_configuration[tuner_num].ip_address,
                                              self.tuner_output_configuration[tuner_num].port,
                                              self.tuner_output_configuration[tuner_num].protocol,
                                              self.tuner_output_configuration[tuner_num].vlan,
                                              self.tuner_output_configuration[tuner_num].vlan_enabled,
                                              self.tuner_output_configuration[tuner_num].endianess,
                                              self.tuner_output_configuration[tuner_num].timestamp_offset,
                                              self.tuner_output_configuration[tuner_num].mfp_flush)

                # prefetch module info
                self.frontend_tuner_status[tuner_num].digital_output_info=_output_mod.getInfo()
                self.frontend_tuner_status[tuner_num].digital_output=False

                success=True
                enable = self.tuner_output_configuration[tuner_num].enabled
                self.debug_msg("Tuner {0} Output Configuration: "\
                              "enabled {1} protocol {2} iface(radio) {3} ip-addr/port/vlan {4}/{5}/{6}",
                              tuner_num,
                              enable,
                              self.tuner_output_configuration[tuner_num].protocol,
                              self.tuner_output_configuration[tuner_num].interface,
                              self.tuner_output_configuration[tuner_num].ip_address,
                              self.tuner_output_configuration[tuner_num].port,
                              self.tuner_output_configuration[tuner_num].vlan)

                if not success:
                    self.error_msg("Configurating tuner radio output for tuner: {0}" \
                                   " configuration values: {1} ",
                                   tuner_num, 
                                   self.tuner_output_configuration[tuner_num])
            except Exception, e:
                self.error_msg("ERROR UPDATING OUTPUT CONFIGURATION FOR TUNER: {} reason {} ", tuner_num, e, no_id=True)


        #
        # Process PSD output configuration and assign to FFT Channels
        #

        self.psd_output_configuration={}
        self.debug_msg("Setting up block psd output configuration "\
                       "from {0} block_psd_output configs",
                       len(self.block_psd_output))

        # psd only supports block configuration
        for block_oc_num in range(0,len(self.block_psd_output)):
            min_num = self.block_psd_output[block_oc_num].fft_channel_start
            max_num = self.block_psd_output[block_oc_num].fft_channel_stop
            if max_num < min_num:
                max_num = min_num

            ip_address=self.block_psd_output[block_oc_num].ip_address
            port=self.block_psd_output[block_oc_num].port
            vlan_enabled=(self.block_psd_output[block_oc_num].vlan > 0 and self.block_psd_output[block_oc_num].vlan_enable)
            vlan=max(0,self.block_psd_output[block_oc_num].vlan)

            for oc_num in range(min_num,max_num+1):
                self.psd_output_configuration[oc_num] = output_config(tuner_channel_number=oc_num,
                                                enabled=self.block_psd_output[block_oc_num].enabled,
                                                interface=self.block_psd_output[block_oc_num].interface,
                                                ip_address= str(ip_address).strip(),
                                                port=port,
                                                vlan_enabled=vlan_enabled,
                                                vlan= vlan,
                                                protocol=self.block_psd_output[block_oc_num].protocol,
                                                endianess = self.block_psd_output[block_oc_num].endianess,
                                                timestamp_offset=self.block_psd_output[block_oc_num].timestamp_offset,
                                                mfp_flush=self.block_psd_output[block_oc_num].mfp_flush,
                                                )
                if self.block_psd_output[block_oc_num].increment_ip_address:
                    try:
                        ip_address = increment_ip(ip_address)
                    except:
                        self.error_msg("Unable to increment ip address, block psd output {0} "\
                                       "last valid ip_address : {0}",block_oc_num, ip_address)
                        continue
                if self.block_psd_output[block_oc_num].increment_port:
                    port +=1
                if self.block_psd_output[block_oc_num].increment_vlan and vlan >= 0:
                    vlan +=1

        self.debug_msg("Configuring {0} FFT channels using {1} psd output configuration." \
                       ,len(self.MSDD.fft_channels),len(self.psd_output_configuration))

        for fidx in range(0,len(self.MSDD.fft_channels)):
            fft_channel=self.MSDD.fft_channels[fidx]
            try:
                fft_chan_num=fft_channel.channel_number()
                fft_channel.setEnable(False)
                if not self.psd_output_configuration.has_key(fft_chan_num): continue

                _output_mod=fft_channel.output.object
                output_cfg=self.psd_output_configuration[fft_chan_num]
                
                if output_cfg.protocol=="UDP_VITA49":                
                    _output_mod.configureVita49(False,
                                                output_cfg.interface,
                                                output_cfg.ip_address,
                                                output_cfg.port,
                                                output_cfg.vlan,
                                                output_cfg.vlan_enabled,
                                                output_cfg.endianess,
                                                output_cfg.timestamp_offset,
                                                output_cfg.mfp_flush,
                                                self.VITA49CONTEXTPACKETINTERVAL)
                else:
                    _output_mod.configureSDDS(False,
                                              output_cfg.interface,
                                              output_cfg.ip_address,
                                              output_cfg.port,
                                              output_cfg.protocol,
                                              output_cfg.vlan,
                                              output_cfg.vlan_enabled,
                                              output_cfg.endianess,
                                              output_cfg.timestamp_offset,
                                              output_cfg.mfp_flush)

                self.debug_msg("FFT Output Channel {0} " \
                               " protocol {1}" \
                               " iface(radio) {2}" \
                               " ip-addr/port/vlan {3}/{4}/{5}",
                               fft_chan_num,
                               self.psd_output_configuration[fft_chan_num].protocol,
                               self.psd_output_configuration[fft_chan_num].interface,
                               self.psd_output_configuration[fft_chan_num].ip_address,
                               self.psd_output_configuration[fft_chan_num].port,
                               self.psd_output_configuration[fft_chan_num].vlan)

                if not success:
                    self.error_msg("Error configuring output for FFT Channel {0}, MSDD {1}", fft_channel.channel_id(),
                                    self.msddReceiverInfo())
                    

            except:
                if logging.NOTSET < self._baseLog.level < logging.INFO:
                    traceback.print_exc()
                self.error_msg("Unable to disable output for FFT Channel: {0}", fft_channel.channel_id())


    def set_default_gain_value_for_tuner(self,tuner_num):
        valid_gain=0.0
        try:
            msdd_type_string = self.frontend_tuner_status[tuner_num].rx_object.get_msdd_type_string()
            if msdd_type_string == MSDDRadio.MSDDRXTYPE_ANALOG_RX:
                valid_gain = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.get_valid_gain(self.gain_configuration.rcvr_gain)
                self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.setGain(valid_gain)
            elif msdd_type_string == MSDDRadio.MSDDRXTYPE_DIGITAL_RX:
                try:
                    valid_gain = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.get_valid_gain(self.gain_configuration.rcvr_gain)
                    self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.setGain(valid_gain)
                except Exception, e:
                    self.warn_msg('Could not set gain {} for analog tuner tuner {}, reason {}',valid_gain,tuner_num, e, no_id=True)

                valid_gain = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_gain(self.gain_configuration.wb_ddc_gain)
                self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setGain(valid_gain)
            elif msdd_type_string == MSDDRadio.MSDDRXTYPE_HW_DDC:
                valid_gain = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_gain(self.gain_configuration.hw_ddc_gain)
                self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setGain(valid_gain)
        except Exception, e :
            self.warn_msg('Could not set gain of {} for tuner {}, reason {}',valid_gain,tuner_num, e, no_id=True)

            
    def advanced_changed(self, propid,oldval, newval):
        try:
            self.allocation_id_mapping_lock.acquire()

            self.advanced = newval

            # for each tuner, update tuner types from advanced property settings. Change
            # will only affect new allocations and not existing allocations
            for tuner_num in range(0,len(self.frontend_tuner_status)):

                tuner_types=[]
                if self.MSDD.rx_channels[tuner_num].is_analog_only(): #Receiver
                    tuner_types = self.getTunerTypeList(self.advanced.rcvr_mode)
                elif self.frontend_tuner_status[tuner_num].rx_object.is_digital_only() and self.frontend_tuner_status[tuner_num].rx_object.is_hardware_based(): # hw_ddc
                    tuner_types = self.getTunerTypeList(self.advanced.hw_ddc_mode)
                elif self.frontend_tuner_status[tuner_num].rx_object.is_analog_and_digital():  #wb_ddc
                    tuner_types = self.getTunerTypeList(self.advanced.wb_ddc_mode)
                else:
                    raise ValueError(self.format_msg_with_radio("Unknown tuner type for MSDD channel {0}",
                                                     self.MSDD.rx_channels[tuner_num].msdd_channel_id()))
                self.frontend_tuner_status[tuner_num].tuner_types = tuner_types
        finally:
            self.allocation_id_mapping_lock.release()
                
    def process(self):
        #
        # if time_helper has completed initial check and 
        # configuration is enable time variance tracking
        #
        if self._enableTimeChecks and self.MSDD:

            #
            # check if time variance check is enabled
            #
            if self.time_of_day.tracking_interval > 0:
                
                #
                # is the next time check value set..
                #
                if self._nextTimeCheck:
                    if time.time() > self._nextTimeCheck:
                        try:
                            # let time helper module use the radio
                            self.msdd_lock.acquire()
                            if self._time_of_day:
                                self._time_of_day.monitor_time_variances()
                        except Exception as e:
                            self.warn_msg('Exception occurred when performing time variance monitoring:\n{0}\n',
                                          e)
                        finally:
                            self.msdd_lock.release()
                        self._nextTimeCheck=time.time()+self.time_of_day.tracking_interval
                else:
                    # we are tracking but interval was not set
                    self._nextTimeCheck=time.time()+self.time_of_day.tracking_interval

        return NOOP   

    def range_check(self, req_cf, req_bw, req_bw_tol, req_sr, req_sr_tol , cur_cf, cur_bw, cur_sr):
        """
        Perform range check for listener allocations that were requested based on a bandwidth or sample rate
        """
        ret_val = True
        if req_bw != None:
            if (req_cf-req_bw/2) < (cur_cf-cur_bw/2) or  (req_cf+req_bw/2) > (cur_cf+cur_bw/2):
                ret_val = False
            if cur_bw < req_bw or cur_bw > req_bw*(1+req_bw_tol/100.0):
                ret_val = False
        
        if req_sr != None:
            if (req_cf-req_sr/2) < (cur_cf-cur_sr/2) or  (req_cf+req_sr/2) > (cur_cf+cur_sr/2):
                ret_val = False
            if cur_sr < req_sr or cur_sr > req_sr*(1+req_sr_tol/100.0):
                ret_val = False
        
        return ret_val    
    
    def getTunerAllocations(self,tuner_num):
        ctl = []
        if len(self.frontend_tuner_status[tuner_num].allocation_id_control) > 0:
            ctl.append(self.frontend_tuner_status[tuner_num].allocation_id_control)
        return self.frontend_tuner_status[tuner_num].allocation_id_listeners + ctl

    def SRIchanged(self, tunerNum, data_port = True, fft_port = False):
        """
        Perform any SRI changes for any tuner changes or allocations.  By default the data_port is enabled
        since we hope someone is listening to our data.  The FFT port is disabled since these tend to
        over subscribed the DSP chip on the radio
        """
        if not self.frontend_tuner_status[tunerNum].allocated or not self.frontend_tuner_status[tunerNum].enabled:
            return
        
        if data_port:
            #tuner is active, so generate timestamps and header info
            H = BULKIO.StreamSRI(hversion=1, #header version
                                 xstart=0.0, #no offset provided
                                 xdelta=1/self.frontend_tuner_status[tunerNum].sample_rate,
                                 xunits=1, #for time data
                                 subsize=0, #1-d data
                                 ystart=0.0, #n/a
                                 ydelta=0.0, #n/a
                                 yunits=0, #n/a
                                 mode=1, #means complex
                                 blocking=False,
                                 streamID=None, # To be filled in below before sending
                                 keywords=[])
            keywordDict = {'COL_RF':self.frontend_tuner_status[tunerNum].center_frequency, 
                           'CHAN_RF':self.frontend_tuner_status[tunerNum].center_frequency,
                           'FRONTEND::BANDWIDTH':self.frontend_tuner_status[tunerNum].bandwidth,
                           'FRONTEND::RF_FLOW_ID':self.frontend_tuner_status[tunerNum].rf_flow_id,
                           'dataRef':1234,
                           'DATA_REF_STR':"1234",
                           'BULKIO_SRI_PRIORITY':True,
                           'FRONTEND::DEVICE_ID':self._id}
    
            for key, val in keywordDict.items():
                H.keywords.append(CF.DataType(id=key, value=any.to_any(val)))
            
            #
            # use system time for timestamp...
            #
            ts = get_utc_time_pair()
            T = BULKIO.PrecisionUTCTime(tcmode=0,
                                        tcstatus=0,
                                        toff=0,
                                        twsec=ts[0],
                                        tfsec=ts[1])
            alloc_id=self.getControlAllocationId(tunerNum)
            if alloc_id:
                H.streamID = alloc_id
                self.port_dataSDDS_out.pushSRI(H, T)
                self.port_dataVITA49_out.pushSRI(H, T)
                self.info_msg("Pushing SRI on alloc_id: {0} with contents: {1}",
                               alloc_id,str(H))
        
        if fft_port:
            num_bins = self.frontend_tuner_status[tunerNum].psd_output_bin_size
            if num_bins <= 0:
                num_bins = self.frontend_tuner_status[tunerNum].psd_fft_size      
            binFreq = self.frontend_tuner_status[tunerNum].sample_rate / num_bins;
            xs = self.frontend_tuner_status[tunerNum].center_frequency - (((num_bins / 2)*(binFreq)) + (binFreq / 2))

            #tuner is active, so generate timestamp and header info
            H = BULKIO.StreamSRI(hversion=1, #header version
                                 xstart=xs, #no offset provided
                                 xdelta=binFreq,
                                 xunits=3, #for time data
                                 subsize=long(num_bins), #1-d data
                                 ystart=0.0, #n/a
                                 ydelta=0.001, #n/a
                                 yunits=1, #n/a
                                 mode=0, #means real
                                 blocking=False,
                                 streamID=None, # To be filled in below before sending
                                 keywords=[])
            keywordDict = {'COL_RF':self.frontend_tuner_status[tunerNum].center_frequency, 
                           'CHAN_RF':self.frontend_tuner_status[tunerNum].center_frequency,
                           'FRONTEND::BANDWIDTH':self.frontend_tuner_status[tunerNum].bandwidth,
                           'FRONTEND::RF_FLOW_ID':self.frontend_tuner_status[tunerNum].rf_flow_id,
                           'dataRef':1234,
                           'DATA_REF_STR':"1234",
                           'BULKIO_SRI_PRIORITY':True}
    
            for key, val in keywordDict.items():
                H.keywords.append(CF.DataType(id=key, value=any.to_any(val)))
            
            #use system time for timestamp...
            ts = get_utc_time_pair()
            T = BULKIO.PrecisionUTCTime(tcmode=0,
                                        tcstatus=0,
                                        toff=0,
                                        twsec=ts[0],
                                        tfsec=ts[1])
            for alloc_id in self.getTunerAllocations(tunerNum):
                H.streamID = alloc_id
                self.port_dataSDDS_out_PSD.pushSRI(H, T)
                self.port_dataVITA49_out_PSD.pushSRI(H, T)
                self.debug_msg("Pushing FFT SRI for alloc_id {0} and SRI {1}",
                               alloc_id,
                               str(H))

    def sendAttach(self, alloc_id, tuner_num, data_port = True, fft_port = False, protocol=None ):
        """
        Send StreamDefintion objects to attached ports based on on allocation ID. The data_port is defaulted
        to true since we hope the are consumers of our data.   The fft_port is False because of the extra
        load to the radio's DSP chip.
        """
        self.debug_msg("Sending Attach() for Allocation ID: {0}", alloc_id)
        
        port_enabled=self.frontend_tuner_status[tuner_num].output_enabled
        if protocol == None:
            protocol=self.frontend_tuner_status[tuner_num].output_protocol
            if protocol == "": protocol = None
    
        if data_port:
            if port_enabled:
                if protocol is None or 'sdds' in protocol.lower():
                    sdef = BULKIO.SDDSStreamDefinition(id = alloc_id,
                                                       dataFormat = BULKIO.SDDS_CI,
                                                       multicastAddress = self.frontend_tuner_status[tuner_num].output_multicast,
                                                       vlan = int(self.frontend_tuner_status[tuner_num].output_vlan),
                                                       port = int(self.frontend_tuner_status[tuner_num].output_port),
                                                       sampleRate = int(self.frontend_tuner_status[tuner_num].sample_rate),
                                                       timeTagValid = True,
                                                       privateInfo = "MSDD Physical Tuner #"+str(tuner_num) )
                    
                    self.info_msg("Adding stream for port dataSDDS_out, Allocation ID: {0}", alloc_id)
                    self.port_dataSDDS_out.addStream(sdef)

                if protocol is None or 'vita' in protocol.lower():

                    vita_data_payload_format = BULKIO.VITA49DataPacketPayloadFormat(packing_method_processing_efficient = False,
                                                                                    complexity = BULKIO.VITA49_COMPLEX_CARTESIAN, 
                                                                                    data_item_format = BULKIO.VITA49_16T, 
                                                                                    repeating = False,
                                                                                    event_tag_size = 0,
                                                                                    channel_tag_size = 0,
                                                                                    item_packing_field_size = 15,
                                                                                    data_item_size = 15,
                                                                                    repeat_count = 1,
                                                                                    vector_size = 1 )

                    #This is needed for a API change between redhawk 1.8 and 1.10
                    try:
                        vdef = BULKIO.VITA49StreamDefinition(ip_address= self.frontend_tuner_status[tuner_num].output_multicast, 
                                                         vlan = int(self.frontend_tuner_status[tuner_num].output_vlan), 
                                                         port = int(self.frontend_tuner_status[tuner_num].output_port), 
                                                         protocol = BULKIO.VITA49_UDP_TRANSPORT,
                                                         valid_data_format = True, 
                                                         data_format = vita_data_payload_format)
                    except:
                        vdef = BULKIO.VITA49StreamDefinition(ip_address= self.frontend_tuner_status[tuner_num].output_multicast, 
                                                         vlan = int(self.frontend_tuner_status[tuner_num].output_vlan), 
                                                         port = int(self.frontend_tuner_status[tuner_num].output_port), 
                                                         id = alloc_id,
                                                         protocol = BULKIO.VITA49_UDP_TRANSPORT,
                                                         valid_data_format = True, 
                                                         data_format = vita_data_payload_format)

                    self.info_msg("Calling attach on dataVITA49_out, Allocation ID: {0}", alloc_id)
                    self.port_dataVITA49_out.addStream(vdef)
                        
        if fft_port:
            if self.frontend_tuner_status[tuner_num].output_enabled:
                if self.psd_output_configuration.has_key(tuner_num) and self.psd_output_configuration[tuner_num].enabled:
                    num_bins = self.frontend_tuner_status[tuner_num].psd_output_bin_size
                    if num_bins <= 0:
                        num_bins = self.frontend_tuner_status[tuner_num].psd_fft_size
                    binFreq = self.frontend_tuner_status[tuner_num].sample_rate / num_bins;
                    
                    if protocol is None or 'sdds' in protocol.lower():
                        sdef = BULKIO.SDDSStreamDefinition(id = alloc_id,
                                                           dataFormat = BULKIO.SDDS_SI,
                                                           multicastAddress = self.psd_output_configuration[tuner_num].ip_address,
                                                           vlan = int(self.psd_output_configuration[tuner_num].vlan),
                                                           port = int(self.psd_output_configuration[tuner_num].port),
                                                           sampleRate = int(binFreq),
                                                           timeTagValid = True,
                                                           privateInfo = "MSDD FFT Tuner #"+str(tuner_num) )
                        self.info_msg("Adding stream for port dataSDDS_out_PSD, Allocation ID:{0}",alloc_id)
                        self.port_dataSDDS_out_PSD.attach(sdef, alloc_id)


                    if protocol is None or 'vita49' in protocol.lower():
                        vita_data_payload_format = BULKIO.VITA49DataPacketPayloadFormat(packing_method_processing_efficient = False,
                                                                                        complexity = BULKIO.VITA49_COMPLEX_CARTESIAN,
                                                                                        data_item_format = BULKIO.VITA49_16T,
                                                                                        repeating = False,
                                                                                        event_tag_size = 0,
                                                                                        channel_tag_size = 0,
                                                                                        item_packing_field_size = 15,
                                                                                        data_item_size = 15,
                                                                                        repeat_count = 1,
                                                                                        vector_size = 1
                                                                                        )

                        try:
                            vdef = BULKIO.VITA49StreamDefinition(ip_address= self.psd_output_configuration[tuner_num].ip_address,
                                                             vlan = int(self.psd_output_configuration[tuner_num].vlan),
                                                             port = int(self.psd_output_configuration[tuner_num].port),
                                                             protocol = BULKIO.VITA49_UDP_TRANSPORT,
                                                             valid_data_format = True,
                                                             data_format = vita_data_payload_format)
                        except:
                            vdef = BULKIO.VITA49StreamDefinition(ip_address=  self.psd_output_configuration[tuner_num].ip_address,
                                                             vlan = int(self.psd_output_configuration[tuner_num].vlan),
                                                             port = int(self.psd_output_configuration[tuner_num].port),
                                                             id = alloc_id,
                                                             protocol = BULKIO.VITA49_UDP_TRANSPORT,
                                                             valid_data_format = True,
                                                             data_format = vita_data_payload_format)

                        self.info_msg("Adding stream for port dataVITA49_out_PSD, Allocation ID: {0}",alloc_id)
                        self.port_dataVITA49_out_PSD.addStream(alloc_id)

        #update and send SRI
        self.SRIchanged(tuner_num, data_port, fft_port)
        
    
    def sendDetach(self, alloc_id, data_port = True, fft_port = True):
        self.info_msg("Removing streams for Allocation ID: {0}",alloc_id)
 
        if data_port:
            self.port_dataSDDS_out.removeStream(alloc_id)
            self.port_dataVITA49_out.removeStream(alloc_id)
        if fft_port:
            self.port_dataSDDS_out_PSD.removeStream(alloc_id)
            self.port_dataVITA49_out_PSD.removeStream(alloc_id)

    def enableFFT(self, alloc_id ):
        """
        if fft_channels are enabled (see advanced properties) and the allocation id
        is a controlling allocation, then a FFT module will be enable for that tuner's
        output.
        
        FFT channels are dynamically assigned since we don't want to add extra burden 
        to the radio's DSP chip.
        """
        
        tuner_num = self.getTunerMapping(alloc_id)
        
        if tuner_num is None or tuner_num < 0: return

        fft_channel = self.MSDD.get_fft_channel()
        
        if fft_channel is None:
            self.frontend_tuner_status[tuner_num].rx_object.removeFFTChannel()
            return
        
        try:
            self.MSDD.stream_router.unlinkModule(fft_channel.channel_id())

            self.trace_msg("(enableFFT) FFT channel id {0} channel number = {1} ",
                           fft_channel.channel_id(), 
                           fft_channel.channel_number())
            
            protocol=None
            enable=False
            if self.psd_output_configuration.has_key(fft_channel.channel_number()):
                protocol=self.psd_output_configuration[fft_channel.channel_number()].protocol
                enable=self.psd_output_configuration[fft_channel.channel_number()].enabled
                self.info_msg("PSD output configuration tuner {0} enabled = {1} protocol {2} ",
                              fft_channel.channel_number(), 
                              enable,
                              protocol)

            if not enable:
                fft_channel.setEnable(False)
                self.info_msg("PSD output configuration missing or not enabled for FFT channel {0}. ",
                              fft_channel.channel_id())
                return

            fft=fft_channel.fft.object
            fft.fftSize = self.psd_configuration.fft_size
            fft.time_between_fft_ms = self.psd_configuration.time_between_ffts
            fft.outputBins = self.psd_configuration.output_bin_size
            fft.window_type_str = self.psd_configuration.window_type
            fft.peak_mode_str = self.psd_configuration.peak_mode

            ffts_per_second = self.frontend_tuner_status[tuner_num].sample_rate / self.psd_configuration.fft_size
            num_avg = ffts_per_second * self.psd_configuration.time_average
            num_avg = fft.getValidAverage(num_avg)
            fft.num_averages = num_avg

            self.frontend_tuner_status[tuner_num].psd_fft_size = fft.fftSize
            self.frontend_tuner_status[tuner_num].psd_averages = fft.num_averages
            self.frontend_tuner_status[tuner_num].psd_time_between_ffts = fft.time_between_fft_ms
            self.frontend_tuner_status[tuner_num].psd_output_bin_size = fft.outputBins
            self.frontend_tuner_status[tuner_num].psd_window_type = fft.window_type_str
            self.frontend_tuner_status[tuner_num].psd_peak_mode = fft.peak_mode_str

            # link fft to tuner
            fft_channel.link(self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object)
            self.frontend_tuner_status[tuner_num].rx_object.addFFTChannel(fft_channel)
            if fft_channel.output.object:
                self.frontend_tuner_status[tuner_num].fft_output_info = fft_channel.output.object.getInfo()
                self.frontend_tuner_status[tuner_num].fft_output = True

            self.debug_msg("Enabling FFT Channel {0} to Tuner {1} ",
                           fft_channel.channel_id(),
                           tuner_num)
            fft_channel.setEnable(True)
            self.sendAttach(alloc_id, tuner_num,  data_port=False, fft_port=True, protocol=protocol)
        except:
            if logging.NOTSET < self._baseLog.level < logging.INFO:
                traceback.print_exc()
            self.frontend_tuner_status[tuner_num].rx_object.removeFFTChannel()
            fft_channel.unlink()
            fft_channel.setEnable(False)
                
    def disableFFT(self, alloc_id ):

        tuner_num = self.getTunerMapping(alloc_id)
        if tuner_num is None or tuner_num < 0: return

        fft_channel=None
        try:
            fft_channel=self.frontend_tuner_status[tuner_num].rx_object.removeFFTChannel()
            self.frontend_tuner_status[tuner_num].fft_output_info = None
            self.frontend_tuner_status[tuner_num].fft_output = False
            if fft_channel:
                fft_channel.unlink()
            self.debug_msg("Disable FFT Channel {0} for Tuner {1}",
                           fft_channel.channel_id(),
                           tuner_num)
            fft_channel.setEnable(False)
        except:
            pass
        finally:
            self.sendDetach(alloc_id, data_port=False, fft_port=True)
            self.MSDD.save_fft_channel(fft_channel)
            

    def _allocate_frontend_tuner_allocation(self, frontend_tuner_allocation, scanner_props=None ):
        """
        Handle FRONTEND tuner allocations
        """
        self.trace_msg("Received a tuner allocation: {0} ", str(frontend_tuner_allocation))
        _stime_alloc=time.time()

        if self._usageState == CF.Device.BUSY:
            #
            # recheck cpu and network state since the radio could have failed to provide status
            # during an allocation
            #
            self.trace_msg("Acquiring lock for tuner allocation: {0} ", str(frontend_tuner_allocation))
            self.allocation_id_mapping_lock.acquire()
            try:
                self._usageState = self.updateUsageState()
                if self._usageState == CF.Device.BUSY:
                    raise CF.Device.InvalidState(self.format_msg_with_radio("Cannot perform allocation, device is busy"))
            finally:
                self.trace_msg("Releasing lock for tuner allocation: {0} ", str(frontend_tuner_allocation))
                self.allocation_id_mapping_lock.release()

        # Check allocation_id
        if not frontend_tuner_allocation.allocation_id:
            self.error_msg("Allocation request missing Allocation ID, {0}",str(frontend_tuner_allocation))
            raise CF.Device.InvalidCapacity(self.format_msg_with_radio("Missing Allocation ID"),
                                            properties.struct_to_props(frontend_tuner_allocation))

        # Check if allocation ID has already been used
        if  self.getTunerMapping(frontend_tuner_allocation.allocation_id) >= 0:
            self.error_msg("Allocation ID {0} already in use.",
                           frontend_tuner_allocation.allocation_id)
            raise CF.Device.InvalidCapacity(self.format_msg_with_radio( "Allocation ID {0} already in use.",
                                                                        frontend_tuner_allocation.allocation_id),
                                            properties.struct_to_props(frontend_tuner_allocation))

        #check allocationID is valid
        if frontend_tuner_allocation.allocation_id in ("",None):
            self.error_msg("Allocation ID cannot be blank")
            raise CF.Device.InvalidCapacity(self.format_msg_with_radio("Allocation ID cannot be blank, allocation {0}"),
                                            properties.struct_to_props(frontend_tuner_allocation))

        self.debug_msg("Tuner allocation request: {0}",frontend_tuner_allocation)

        if frontend_tuner_allocation.tuner_type == self.FE_TYPE_DDC:
            reject_allocation = True
            for tn in self.dig_tuner_base_nums:
                reject_allocation = reject_allocation and (self.frontend_tuner_status[tn].allocated and self.frontend_tuner_status[tn].tuner_type == self.FE_TYPE_RXDIG and len(self.frontend_tuner_status[tn].tuner_types) == 1)
            if reject_allocation:
                self.debug_msg("DDC allocation failed, reason all tuners are allocated")
                return False

        # Try out the allocation against each tuner. If the allocation passes then enable output and update
        # status structures. If allocation fails because of radio comms issue then failout of this allocation
        # If the allocation failes because of frequency, bandwidth or sample rate constraints then continue to 
        # the next tuner until all tuners are exhausted
        self.trace_msg("Acquiring lock for tuner allocation: {0} ", str(frontend_tuner_allocation))
        self.allocation_id_mapping_lock.acquire()
        emsg=None
        try:
            if len(self.tuner_allocation_ids) != len(self.frontend_tuner_status):
                for idx in range(len(self.frontend_tuner_status)-len(self.tuner_allocation_ids)):
                    self.tuner_allocation_ids.append(tuner_allocation_ids_struct())

            for tuner_num in range(len(self.tuner_allocation_ids)):
                self.debug_msg("--- Attempting allocation on Tuner {0}:"\
                               " Type: {1} Request: {2} ",
                               tuner_num,
                               self.frontend_tuner_status[tuner_num].tuner_type,
                               str(frontend_tuner_allocation))


                # Check allocation agains tuner type
                self.trace_msg( " Checking tuner types {0} "\
                                " default tuner type {1} "\
                                " request type {2} ",
                                self.frontend_tuner_status[tuner_num].tuner_types,
                                self.frontend_tuner_status[tuner_num].tuner_type,
                                frontend_tuner_allocation.tuner_type)
                if self.frontend_tuner_status[tuner_num].tuner_types.count(frontend_tuner_allocation.tuner_type) <= 0:
                    self.debug_msg(' Skipping tuner number {0} tuner type mismatch', tuner_num)
                    continue
                
                # check if allocation requires a specific rf_flow_id
                if len(frontend_tuner_allocation.rf_flow_id) != 0 and  frontend_tuner_allocation.rf_flow_id != self.device_rf_flow:
                    emsg="rf_flow_id mismatch requested {0} device rf_flow_id {1}".format(
                                   frontend_tuner_allocation.rf_flow_id,
                                   self.device_rf_flow)                    
                    self.debug_msg(" Skipping tuner number {0}, "\
                                   "rf_flow_id mismatch requested {1} device rf_flow_id {2}", 
                                   tuner_num,
                                   frontend_tuner_allocation.rf_flow_id,
                                   self.device_rf_flow)
                                   
                    continue

                # check if allocation requires a specific group_id
                if frontend_tuner_allocation.group_id != self.frontend_group_id:
                    emsg="group_id mismatch, requested {0} device group_id {1}".format(
                                   frontend_tuner_allocation.group_id, 
                                   self.frontend_group_id)                    
                    self.debug_msg(" Skipping tuner number {0} "\
                                   "group_id mismatch, requested {1} device group_id {2}",
                                   tuner_num,
                                   frontend_tuner_allocation.group_id, 
                                   self.frontend_group_id)
                    continue

                # Check requested RF frequencies within RFInfo Setting if upstream antenna has provided one
                if self.device_rf_info_pkt:
                    try:
                        validateRequestVsRFInfo(frontend_tuner_allocation,self.device_rf_info_pkt,1)
                    except FRONTEND.BadParameterException , e:
                        emsg="RF Info allocation failure, reason {0}".format(e)
                        self.info_msg("ValidateRequestVsRFInfo Failed: reason {0}",e)
                        raise


                # Calculate IF Offset if there is one
                if_freq = self.convert_rf_to_if(frontend_tuner_allocation.center_frequency)

                ########## LISTENER ALLOCATION - IE - DEVICE CONTROL IS FALSE ##########
                if not frontend_tuner_allocation.device_control:
                    if not self.frontend_tuner_status[tuner_num].allocated:
                        self.debug_msg(' Listener allocation failed because tuner {0} was not allocated',
                                       tuner_num)
                        continue

                    reg_sr = frontend_tuner_allocation.sample_rate
                    reg_sr_tolerance = frontend_tuner_allocation.sample_rate_tolerance
                    if type == self.FE_TYPE_RECEIVER:
                        reg_sr = None
                        reg_sr_tolerance = None
                    valid = self.range_check(frontend_tuner_allocation.center_frequency,
                                             frontend_tuner_allocation.bandwidth,
                                             frontend_tuner_allocation.bandwidth_tolerance,
                                             reg_sr,
                                             reg_sr_tolerance,
                                             self.frontend_tuner_status[tuner_num].center_frequency,
                                             self.frontend_tuner_status[tuner_num].bandwidth,
                                             self.frontend_tuner_status[tuner_num].sample_rate)

                    if not valid:
                        self.debug_msg(" Invalid listener allocation against tuner_num {0}, parameter mismatch on frequency, bandwidth or sample rate",
                                       tuner_num)
                        continue

                    # Make a Listener allocation for this tuner with the requested allocation ID
                    listenerallocation = frontend.fe_types.frontend_listener_allocation()
                    listenerallocation.existing_allocation_id = self.frontend_tuner_status[tuner_num].allocation_id_control
                    listenerallocation.listener_allocation_id = frontend_tuner_allocation.allocation_id
                    retval=False
                    try:
                        self.trace_msg("Releasing lock for tuner allocation: {0} ", str(frontend_tuner_allocation))
                        self.allocation_id_mapping_lock.release()
                        retval =  self._allocate_frontend_listener_allocation(listenerallocation)
                    except:
                        self.exception_msg("Could not allocate listener with allocation {0}", frontend_tuner_allocation)
                    finally:
                        self.trace_msg("Acquiring lock for tuner allocation: {0} ", str(frontend_tuner_allocation))                        
                        self.allocation_id_mapping_lock.acquire()                        

                    return retval


                ########## CONTROLLER ALLOCATION - IE - DEVICE CONTROL IS TRUE ##########
                if self.frontend_tuner_status[tuner_num].allocated:
                    self.debug_msg(" Control allocation failed because given tuner {0} was already allocated",
                                   tuner_num)
                    continue

                ### MAKE SURE PARENT IS ALLCOATED
                parent_tuner = self.MSDD.get_parent_tuner_num(self.frontend_tuner_status[tuner_num].rx_object)
                if parent_tuner != None:
                    self.trace_msg("Checking Parent/Child relationship Tuner {}  Parent {} allocated {} ",tuner_num, parent_tuner, self.frontend_tuner_status[parent_tuner].allocated)
                    if not self.frontend_tuner_status[parent_tuner].allocated:
                        emsg="Missing required parent tuner ({}) allocation".format(parent_tuner)
                        if frontend_tuner_allocation.tuner_type=='DDC':
                            emsg="DDC control allocation failed for tuner {}, missing required parent tuner ({}) allocation".format(tuner_num,parent_tuner)
                        self.debug_msg("Control allocation failed on tuner {} because tuner's parent {} was not allocated",
                                       tuner_num, parent_tuner)
                        continue

                # Check if piggybacked channels are not allocated
                if self.checkSecondaryAllocations(tuner_num):
                    self.debug_msg("Skipping tuner {0}, tuner has secondary allocations active",
                                   tuner_num)
                    continue

                try:
                    cfg_values="cf:%s bw/tol:%s/%s srate/tol:%s/%s"% (frontend_tuner_allocation.center_frequency,
                                                                      frontend_tuner_allocation.bandwidth,
                                                                      frontend_tuner_allocation.bandwidth_tolerance,
                                                                      frontend_tuner_allocation.sample_rate,
                                                                      frontend_tuner_allocation.sample_rate_tolerance)
                    valid_cf = None
                    valid_sr = None
                    valid_bw = None
                    success = True

                    try:
                        valid_cf = self.frontend_tuner_status[tuner_num].rx_object.get_valid_frequency(if_freq)
                        success &= self.frontend_tuner_status[tuner_num].rx_object.setFrequency_Hz(valid_cf)

                        _etime=time.time()-_stime_alloc
                        self.debug_msg("allocate candidate tuner {} duration:{:.7f} (before sr/bw check) "\
                                    ", cf success {} "\
                                    ", if freq {} "\
                                    ", freq {} "\
                                    ", sample rate {}"\
                                    ", bandwidth {}",
                                       tuner_num,
                                       _etime,
                                       success,
                                       if_freq,
                                       valid_cf,
                                       valid_sr,
                                       valid_bw)

                        if frontend_tuner_allocation.sample_rate!=0 and frontend_tuner_allocation.bandwidth!=0:
                            #Specify a SR and BW
                            sr = max(frontend_tuner_allocation.bandwidth,frontend_tuner_allocation.sample_rate) #Ask for enough sr to satisfy the bw request
                            valid_bw = self.frontend_tuner_status[tuner_num].rx_object.get_valid_bandwidth(frontend_tuner_allocation.bandwidth, frontend_tuner_allocation.bandwidth_tolerance)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setBandwidth_Hz(valid_bw)
                            valid_sr = self.frontend_tuner_status[tuner_num].rx_object.get_valid_sample_rate(sr, frontend_tuner_allocation.sample_rate_tolerance)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setSampleRate(valid_sr)
                            self.trace_msg( " SRATE and BW, valid bw {0} valid srate {1} success {2}",
                                            valid_bw,
                                            valid_sr,
                                            success)
                        elif frontend_tuner_allocation.sample_rate!=0 and frontend_tuner_allocation.bandwidth==0:
                            #Specify only SR
                            valid_sr = self.frontend_tuner_status[tuner_num].rx_object.get_valid_sample_rate(frontend_tuner_allocation.sample_rate, frontend_tuner_allocation.sample_rate_tolerance)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setSampleRate(valid_sr)
                            valid_bw =  self.frontend_tuner_status[tuner_num].rx_object.get_bandwidth_for_sample_rate(valid_sr)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setBandwidth_Hz(valid_bw)
                            self.trace_msg( " SRATE only, valid bw {0} valid srate {1} success {2}",
                                            valid_bw,
                                            valid_sr,
                                            success)
                        elif frontend_tuner_allocation.sample_rate==0 and frontend_tuner_allocation.bandwidth!=0:
                            #specify only BW so use it for requested SR with a large tolerance because sample rate is 'don't care'
                            valid_bw = self.frontend_tuner_status[tuner_num].rx_object.get_valid_bandwidth(frontend_tuner_allocation.bandwidth, frontend_tuner_allocation.bandwidth_tolerance)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setBandwidth_Hz(valid_bw)
                            valid_sr = self.frontend_tuner_status[tuner_num].rx_object.get_sample_rate_for_bandwidth(valid_bw)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setSampleRate(valid_sr)
                            self.trace_msg( " BW only, valid bw {0} valid srate {1} success {2}",
                                            valid_bw,
                                            valid_sr,
                                            success)
                        elif frontend_tuner_allocation.sample_rate==0 and frontend_tuner_allocation.bandwidth==0:
                            #Don't care for both sample_rate and # bandwidth so find any valid value and use it
                            valid_bw =  self.frontend_tuner_status[tuner_num].rx_object.get_valid_bandwidth(valid_bw)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setBandwidth_Hz(valid_bw)
                            valid_sr = self.frontend_tuner_status[tuner_num].rx_object.get_sample_rate_for_bandwidth(valid_bw)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setSampleRate(valid_sr)
                            self.trace_msg( " Don't care, valid bw {0} valid srate {1} success {2}",
                                            valid_bw,
                                            valid_sr,
                                            success)
                        else:
                            self.error_msg( "Allocation failed against tuner {0} for unknown reason",
                                            tuner_num)
                            success = False

                        _etime=time.time()-_stime_alloc
                        self.debug_msg("allocate results, tuner {} duration:{:.7f} success {}"\
                                        ", if freq {}"\
                                        ", freq {}"\
                                        ", sample rate {}"\
                                        ", bandwidth {}",
                                       tuner_num,
                                       _etime,
                                       success,
                                       if_freq,
                                       valid_cf,
                                       valid_sr,
                                       valid_bw)

                    except (CommandException, InvalidValue), e:
                        if logging.NOTSET < self._baseLog.level < logging.INFO:
                            traceback.print_exc()
                        success = False
                        msg=self.format_msg_with_radio("Allocation failed, Tuner {0} exception {1} allocation request {2}",
                                            tuner_num, 
                                            e, 
                                            cfg_values)
                        self.debug_msg(msg)
                        raise MsddException(msg)

                    allocated_values=self.format_msg("cf:{0} bw:{1} srate:{2} ",
                                                     valid_cf, 
                                                     valid_bw, 
                                                     valid_sr)
                    if valid_cf == None or valid_bw == None or valid_sr == None or not success:
                        self.debug_msg("Allocation failed, tuner {0} allocation: {1} validated: {2}",
                                       tuner_num, 
                                       cfg_values, 
                                       allocated_values)
                        raise Exception(self.format_msg_with_radio("Tuner {0} Invalid allocation values {1}",
                                                        tuner_num, 
                                                        cfg_values))

                    # check expected network output rate for device.
                    srate = self.frontend_tuner_status[tuner_num].rx_object.getSampleRate()
                    self.debug_msg("Checking network utilization with additional sample rate, tuner: {0} sample rate: {1} ",
                                   tuner_num, 
                                   srate)
                    protocol=None
                    if self.tuner_output_configuration.has_key(tuner_num):
                        protocol = self.tuner_output_configuration[tuner_num].protocol
                        self.debug_msg("Checking additional network utilization for tuner: {0} sample rate: {1} enabled: {2} protocol {3}",
                                       tuner_num, 
                                       srate, 
                                       self.tuner_output_configuration[tuner_num].enabled,
                                       protocol)
                    if self.tuner_output_configuration.has_key(tuner_num) and \
                       self.tuner_output_configuration[tuner_num].enabled:
                        if not self.checkNetworkOutput(additional_rate=srate,
                                                               out_module=self.frontend_tuner_status[tuner_num].rx_object.output_object):
                            raise BusyException(self.format_msg_with_radio("Network output rate would exceed network interface, tuner: {0} adding sample rate {1}",
                                                            tuner_num,
                                                            srate))

                    if self.tuner_output_configuration.has_key(tuner_num) and \
                       not self.tuner_output_configuration[tuner_num].enabled:
                        self.warn_msg("Tuner {0} is allocated but output is disabled",
                                      tuner_num)

                    try:
                        # apply gain settings for the tuner
                        self.set_default_gain_value_for_tuner(tuner_num)

                        self.debug_msg("Set RF freq offset {0}"\
                                       " child modules: {1}",
                                       valid_cf,
                                       ",".join([ x.msdd_channel_id() for x in self.frontend_tuner_status[tuner_num].rx_object.rx_child_objects]))

                        self.frontend_tuner_status[tuner_num].rx_object.updateChildChannelOffsets(valid_cf)

                        self.frontend_tuner_status[tuner_num].tuner_type = frontend_tuner_allocation.tuner_type
                        self.frontend_tuner_status[tuner_num].allocated = True
                        self.frontend_tuner_status[tuner_num].allocation_id_control = frontend_tuner_allocation.allocation_id;
                        self.frontend_tuner_status[tuner_num].allocation_id_csv = self.create_allocation_id_csv(self.frontend_tuner_status[tuner_num].allocation_id_control,
                                                                                                                self.frontend_tuner_status[tuner_num].allocation_id_listeners)
                        self.tuner_allocation_ids[tuner_num].control_allocation_id = frontend_tuner_allocation.allocation_id
                        self.allocation_id_to_tuner_id[frontend_tuner_allocation.allocation_id] =  tuner_num
                        
                        self.deviceEnable( self.frontend_tuner_status[tuner_num], tuner_num)

                        self.frontend_tuner_status[tuner_num].rf_flow_id = self.device_rf_flow
                        if protocol is None or 'sdds' in protocol.lower():
                            self.matchAllocationIdToStreamId(frontend_tuner_allocation.allocation_id, frontend_tuner_allocation.allocation_id,"dataSDDS_out")
                            self.matchAllocationIdToStreamId(frontend_tuner_allocation.allocation_id, frontend_tuner_allocation.allocation_id,"dataSDDS_out_PSD")
                        if protocol is None or 'vita' in protocol.lower():
                            self.matchAllocationIdToStreamId(frontend_tuner_allocation.allocation_id, frontend_tuner_allocation.allocation_id,"dataVITA49_out")
                            self.matchAllocationIdToStreamId(frontend_tuner_allocation.allocation_id, frontend_tuner_allocation.allocation_id,"dataVITA49_out_PSD")

                        # send connection information to downstream consumers
                        try:
                            self.sendAttach(self.frontend_tuner_status[tuner_num].allocation_id_control, tuner_num, protocol=protocol)
                        except:
                            self.warn_msg("Error sending attach message to consumers for allocation {0}", frontend_tuner_allocation)

                        _etime=time.time()-_stime_alloc
                        self.info_msg("Allocation {} duration:{:.7f} SUCCESS,"\
                                      " Tuner: {} "\
                                      " Type: {} "\
                                      " requested {} "\
                                      " allocated {}",
                                      frontend_tuner_allocation.allocation_id,
                                      _etime,
                                      tuner_num,
                                      self.frontend_tuner_status[tuner_num].tuner_type,
                                      cfg_values,
                                      allocated_values)

                        if frontend_tuner_allocation.device_control:
                            self.enableFFT(self.frontend_tuner_status[tuner_num].allocation_id_control)

                    except BusyException, e:
                        raise e
                            
                    except Exception,e:
                        if logging.NOTSET < self._baseLog.level < logging.INFO:
                            traceback.print_exc()
                        raise AllocationFailure("Error completing allocation {0}".format(frontend_tuner_allocation.allocation_id))

                    # update usage state
                    self._usageState = self.updateUsageState()


                    _etime=time.time()-_stime_alloc
                    self.info_msg("Allocation time {:.7f} for allocation {}", _etime,frontend_tuner_allocation.allocation_id)
                    
                    # Successfull allocation return
                    return True

                except (AllocationFailure, BusyException, FRONTEND.BadParameterException), e:
                    emsg = "{}".format(e)
                    self.warn_msg("Allocation failure request {0} reason {1}", frontend_tuner_allocation,e)
                    raise
                                  
                except Exception,e:
                    if logging.NOTSET < self._baseLog.level < logging.INFO:
                        traceback.print_exc()
                    self.frontend_tuner_status[tuner_num].rx_object.setEnable(False)
                    emsg = "{}".format(e)
                    self.debug_msg("--- Allocation Failed, Reason {0} "\
                                   " Tuner: {1} "\
                                   " Type: {2} "\
                                   " Request: {3}",
                                   emsg,
                                   tuner_num,
                                   self.frontend_tuner_status[tuner_num].tuner_type,
                                   frontend_tuner_allocation)

                    continue
        except:
            if logging.NOTSET < self._baseLog.level < logging.INFO:
                traceback.print_exc()
        finally:
            self.trace_msg("Releasing lock for tuner allocation: {0} ", str(frontend_tuner_allocation))                                    
            self.allocation_id_mapping_lock.release()


        # check if an allocation was completed or registered
        tuner_id = self.getTunerMapping(frontend_tuner_allocation.allocation_id)
        if tuner_id >= 0:
            try:
                self.debug_msg("Releasing failed allocation for request: {0} ", frontend_tuner_allocation)
                self.deallocate_frontend_tuner_allocation(frontend_tuner_allocation)
            except:
                pass
            return False            

        if not emsg:
            emsg  = "No tuners available"
        self.warn_msg("Failed allocation, Reason: {0} Allocation request: {1}",
                      emsg,
                      str(frontend_tuner_allocation))

        try:
            self._usageState = self.updateUsageState()
        except:
            pass

        return False


    def enableDigitalOutput(self, tuner_num ):
        try:
            _rx_object = self.frontend_tuner_status[tuner_num].rx_object
            _output_mod = _rx_object.output_object.object
            _output_mod.enable = False
            if not _rx_object.has_streaming_output():
                self.trace_msg("Tuner {0} does not have digital output", tuner_num)
                return
            if not self.tuner_output_configuration.has_key(tuner_num):
                self.warn_msg("Missing output configuration for tuner {0} disabling output", tuner_num)
                return

            try:
                # only set interface if ip_address is lost
                if _output_mod.getIPP() == '0.0.0.0':
                    if self.tuner_output_configuration[tuner_num].protocol=="UDP_VITA49":                
                        _output_mod.configureVita49(False,
                                                    self.tuner_output_configuration[tuner_num].interface,
                                                    self.tuner_output_configuration[tuner_num].ip_address,
                                                    self.tuner_output_configuration[tuner_num].port,
                                                    self.tuner_output_configuration[tuner_num].vlan,
                                                    self.tuner_output_configuration[tuner_num].vlan_enabled,
                                                    self.tuner_output_configuration[tuner_num].endianess,
                                                    self.tuner_output_configuration[tuner_num].timestamp_offset,
                                                    self.tuner_output_configuration[tuner_num].mfp_flush,
                                                    self.VITA49CONTEXTPACKETINTERVAL)
                    else:
                        _output_mod.configureSDDS(False,
                                                  self.tuner_output_configuration[tuner_num].interface,
                                                  self.tuner_output_configuration[tuner_num].ip_address,
                                                  self.tuner_output_configuration[tuner_num].port,
                                                  self.tuner_output_configuration[tuner_num].protocol,
                                                  self.tuner_output_configuration[tuner_num].vlan,
                                                  self.tuner_output_configuration[tuner_num].vlan_enabled,
                                                  self.tuner_output_configuration[tuner_num].endianess,
                                                  self.tuner_output_configuration[tuner_num].timestamp_offset,
                                                  self.tuner_output_configuration[tuner_num].mfp_flush)


                    # prefetch bitrate for each module
                    self.frontend_tuner_status[tuner_num].digital_output_info=_output_mod.getInfo()

                    # relink output module to source if it was lost
                    if self.MSDD.get_corresponding_output_module_object(_rx_object.digital_rx_object) == None:
                        self.MSDD.stream_router.linkModules(
                            _rx_object.digital_rx_object.object.full_reg_name,
                            _output_mod.full_reg_name)
                        self.debug_msg("enableDigitalOutput, tuner {0}"\
                                       " relinking src: {1}"
                                       " dest: {2}",
                                       tuner_num,
                                       _rx_object.digital_rx_object.object.full_reg_name,
                                       _output_mod.full_reg_name)

                success=True
            except:
                self.error_msg("Failure to configure for tuner {0}, output context: "\
                               " protocol {1}"\
                               " iface(radio) {2}"\
                               " ip-addr/port/vlan {3}/{4}/{5}",
                               tuner_num,
                               self.tuner_output_configuration[tuner_num].protocol,
                               self.tuner_output_configuration[tuner_num].interface,
                               self.tuner_output_configuration[tuner_num].ip_address,
                               self.tuner_output_configuration[tuner_num].port,
                               self.tuner_output_configuration[tuner_num].vlan)
                success=False

            self.debug_msg("enableDigitalOutput, tuner {0} output context: "\
                            " protocol {1}"\
                            " iface(radio) {2}"\
                           " ip-addr/port/vlan {3}/{4}/{5}",
                           tuner_num,
                           self.tuner_output_configuration[tuner_num].protocol,
                           self.tuner_output_configuration[tuner_num].interface,
                           self.tuner_output_configuration[tuner_num].ip_address,
                           self.tuner_output_configuration[tuner_num].port,
                           self.tuner_output_configuration[tuner_num].vlan)

            outcfg_enabled=self.tuner_output_configuration[tuner_num].enabled
            self.frontend_tuner_status[tuner_num].digital_output=outcfg_enabled
            if outcfg_enabled:
                # if tuner is just a channelizer then don't enable output module...
                if "CHANNELIZER" in self.frontend_tuner_status[tuner_num].tuner_types:
                    self.info_msg("Disabling digital output for CHANNELIZER only request, tuner {0}",tuner_num)
                    outcfg_enabled=False

            success&= _rx_object.set_output_enable(outcfg_enabled)
            self.debug_msg("enableDigitalOutput, tuner {0}"\
                            " enabled_output: {1}"\
                            " return from set enable {2}",
                           tuner_num,
                           outcfg_enabled,
                           success)
            if not outcfg_enabled:
                self.warn_msg( "Output configuration disables output for tuner {0}",tuner_num)

            if not success:
                msg="Error when updating output configuration, Tuner: {0}"\
                               " with configuration: {1}".format(tuner_num,
                               str(self.tuner_output_configuration[tuner_num]))
                self.error_msg(msg)
                raise AllocationFailure(msg)
                

            self.debug_msg("finished tuner output configuration, tuner {0} output enable state {1}",
                           tuner_num,
                           outcfg_enabled)

        except Exception as e:
            if logging.NOTSET < self._baseLog.level < logging.INFO:
                traceback.print_exc()
            _rx_object.set_output_enable(False)
            self.error_msg("Error when updating output configuration for Tuner: {}, reason {}",tuner_num,e)
            raise e


    '''
    *************************************************************
    Functions supporting tuning allocation
    *************************************************************'''
    def deviceEnable(self, fts, tuner_id):
        '''
        ************************************************************
        modify fts, which corresponds to self.frontend_tuner_status[tuner_id]
        Make sure to set the 'enabled' member of fts to indicate that tuner as enabled
        ************************************************************'''
        self.debug_msg("deviceEnable tuner_id {0}",tuner_id)
        enable=False
        try:
            # enable output port for the tuner
            if self.tuner_output_configuration.has_key(tuner_id):
                self.enableDigitalOutput(tuner_id)

            # enable tuner
            self.debug_msg("deviceEnable tuner {0} enabling digital tuner's digital receiver module", tuner_id)
            self.frontend_tuner_status[tuner_id].rx_object.set_tuner_enable(True)
            enable=True

            self.debug_msg("deviceEnable, tuner {0}"\
                           " msdd digital module enable: {1}"\
                           " msdd output module {2}",
                           tuner_id,
                           self.frontend_tuner_status[tuner_id].rx_object.digital_rx_object.object.enable,
                           self.frontend_tuner_status[tuner_id].rx_object.output_object.object.enable)
        except Exception as e:
            fts.enabled=enable
            self.exception_msg("Exception occurred when enabling tuner {0}",tuner_id)
            try:
                self.frontend_tuner_status[tuner_id].rx_object.setEnable(False)
                if self.frontend_tuner_status[tuner_id].rx_object.hasFFTChannel():
                    self.disableFFT(self.frontend_tuner_status[tuner_id].allocation_id_control)
            except:
                pass
            raise e


        fts.enabled=enable
        self._update_tuner_status([tuner_id])
        return

    def deviceDisable(self,fts, tuner_id):

        restatus_tuners=[tuner_id]
        try:
            # Check if tuner_id has secondary allocations associated with it
            # if so, only turn of output and leave hardware nbddc tuner enabled
            self.debug_msg("deviceDisable,  disable tuner/output for tuner {0}",tuner_id)
            if not self.checkSecondaryAllocations(tuner_id):
                self.frontend_tuner_status[tuner_id].rx_object.setEnable(False)
            else:
                self.debug_msg("Tuner {0} has secondary allocations active, retain nbddc enable state.", tuner_id)
                self.frontend_tuner_status[tuner_id].rx_object.setEnable(False,enabled_secondary=True)

            # Check this tuner is the last tuner allocated for a non-allocated parent.
            # if so, we can disable the parent's hardware tuner
            # Can't check allocated or allocation_id of tuner_num because it is reset in deviceDeleteTuning
            parent_rx_channel = self.frontend_tuner_status[tuner_id].rx_object.rx_parent_object
            if self.rx_channel_tuner_status.has_key(parent_rx_channel):
                parent_idx=self.rx_channel_tuner_status[parent_rx_channel]
                allocated = self.frontend_tuner_status[parent_idx].allocated
                allocs=self.countSecondaryAllocations(parent_idx)
                if allocs == 1 and not allocated :
                    # disable msdd rx_channel for the parent tuner since
                    # no active allocations remain
                    self.debug_msg("Tuner {0}, Parent has no secondary allocations so disabling", tuner_id)
                    parent_rx_channel.setEnable(False)
                    restatus_tuners.append(parent_idx)

        except:
            self.exception_msg("Exception trying to diable tuner {0} ",tuner_id)

        fts.enabled=False
        fts.digital_output=False
        self._update_tuner_status(restatus_tuners)

    def deviceDeleteTuning(self,fts,tuner_id):
        
        control_aid = ""
        try:
            control_aid = self.getControlAllocationId(tuner_id)
            if self.frontend_tuner_status[tuner_id].rx_object.hasFFTChannel():
                self.disableFFT(control_aid)
        except:
            self.error_msg("Error disabling FFT output for Tuner {0}  Allocation ID {1}",
                           tuner_id,
                           control_aid)

        if control_aid and control_aid != "":
            self.sendDetach(control_aid)

        self.frontend_tuner_status[tuner_id].allocation_id_control = ""
        self.frontend_tuner_status[tuner_id].allocated = False
        self.info_msg("Deallocated Allocation ID: {0} Tuner: {1} Type: {2}",
                      control_aid,
                      tuner_id,
                      fts.tuner_type)


    def checkSecondaryAllocations(self, tuner_id ):
        """
        Check if tuner_id has any secondary/child allocations active

        Parameters:
        -----------
        tuner_id : tuner id to check for secondary/child allocations

        Returns:
        --------
        True - child allocations exists
        False - no child allocations are active
        """
        retval = False
        for child in self.frontend_tuner_status[tuner_id].rx_object.rx_child_objects:
            # reverse lookup tuner
            if self.rx_channel_tuner_status.has_key(child):
                child_idx=self.rx_channel_tuner_status[child]
                if self.frontend_tuner_status[child_idx].allocated:
                    return True

    def countSecondaryAllocations(self, tuner_id ):
        """
        Count the number of secondary allocations associated with a tuner

        Parameters:
        -----------
        tuner_id : tuner number to check for secondary/child allocations

        Returns:
        --------
        int : number of active allocations
        """
        count = 0
        for child in self.frontend_tuner_status[tuner_id].rx_object.rx_child_objects:
            if self.rx_channel_tuner_status.has_key(child):
                child_idx=self.rx_channel_tuner_status[child]
                if self.frontend_tuner_status[child_idx].allocated:
                    count += 1
        return count

    def getSecondaryAllocations(self, tuner_id ):
        """
        Return a list secondary allocations for a tuner

        Parameters:
        -----------
        tuner_id : tuner number to check for secondary/child allocations

        Returns:
        --------
        [str] : list of allocation ids
        """
        allocs=[]
        for child in self.frontend_tuner_status[tuner_id].rx_object.rx_child_objects:
            if self.rx_channel_tuner_status.has_key(child):
                child_idx=self.rx_channel_tuner_status[child]
                if self.frontend_tuner_status[child_idx].allocated:
                    allocs.append(self.frontend_tuner_status[child_idx].allocation_id_control)
        return allocs


    def checkLastSecondaryAllocation(self, child_tuner_id, match_allocation=True ):
        """
        For a tuner, check if the tuner is the last secondary/child allocation
        for a parent and the parent is not allocated

        Parameters:
        -----------
        child_tuner_id : tuner id of child

        Returns:
        --------
        True: child tuner is the last allocation for non-allocated parent
        False: otherwise
        """
        aid = self.frontend_tuner_status[child_tuner_id].allocation_id_control
        parent_rx_channel = self.frontend_tuner_status[child_tuner_id].rx_object.rx_parent_object
        if self.rx_channel_tuner_status.has_key(parent_rx_channel):
            parent_idx=self.rx_channel_tuner_status[parent_rx_channel]
            allocated = self.frontend_tuner_status[parent_idx].allocated
            if not allocated:
                allocs=self.getSecondaryAllocations( parent_idx )
                if len(allocs) == 1 and aid in allocs:
                    return True
        return False


    def checkRFInfoCenterFrequency(self,rf_freq):
        if not(self.device_rf_info_pkt):
            return True
        #Check that the Requested RF Frequency is within the analog bandwidth of the RFInfo Packet
        minFreq = self.device_rf_info_pkt.rf_center_freq - self.device_rf_info_pkt.rf_bandwidth/2.0
        maxFreq = self.device_rf_info_pkt.rf_center_freq + self.device_rf_info_pkt.rf_bandwidth/2.0
        self.debug_msg("Checking Freq against RFInfo Packet. Requested RF: {} minFreq: {} maxFreq: {}".rf_freq,minFreq,maxFreq)
        check1 = (rf_freq<maxFreq)
        check2 = (rf_freq>minFreq)
        return (check1 and check2)

    def convert_rf_to_if(self,rf_freq):
        #Convert freq based on RF/IF of Analog Tuner
        
        if not(self.device_rf_info_pkt):
            return rf_freq
        
        if self.device_rf_info_pkt.if_center_freq > 0:
            ifoffset = rf_freq - self.device_rf_info_pkt.rf_center_freq
            if self.device_rf_info_pkt.spectrum_inverted:
                if_freq =self.device_rf_info_pkt.if_center_freq-ifoffset
            else:
                if_freq =self.device_rf_info_pkt.if_center_freq+ifoffset
        else:
            if_freq = rf_freq 

        self.debug_msg("Converted RF Freq of {} to IF Freq {} based on Input RF of {}, IF of {}, and spectral inversion {}",rf_freq,if_freq,self.device_rf_info_pkt.rf_center_freq,self.device_rf_info_pkt.if_center_freq,self.device_rf_info_pkt.spectrum_inverted)
        return if_freq         
    
    def convert_if_to_rf(self,if_freq):
        #Convert freq based on RF/IF of Analog Tuner
        
        if not(self.device_rf_info_pkt):
            return if_freq
        
        if self.device_rf_info_pkt.if_center_freq > 0:
            ifoffset = if_freq - self.device_rf_info_pkt.if_center_freq
            if self.device_rf_info_pkt.spectrum_inverted:
                rf_freq =self.device_rf_info_pkt.rf_center_freq-ifoffset
            else:
                rf_freq =self.device_rf_info_pkt.rf_center_freq+ifoffset
        else:
            rf_freq = if_freq 

        self.debug_msg("Converted IF Freq of {} to RF Freq {} based on Input RF of {}, IF of {}, and spectral inversion {}",if_freq,rf_freq,self.device_rf_info_pkt.rf_center_freq,self.device_rf_info_pkt.if_center_freq,self.device_rf_info_pkt.spectrum_inverted)
        return rf_freq     

    '''
    *************************************************************
    Functions servicing the tuner control port
    *************************************************************'''
    def getTunerType(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid Allocation ID {0}".format(allocation_id))
        return self.frontend_tuner_status[idx].tuner_type

    def getTunerDeviceControl(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid Allocation ID {0}".format(allocation_id))
        if self.getControlAllocationId(idx) == allocation_id:
            return True
        return False

    def getTunerGroupId(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid Allocation ID {0}".format(allocation_id))
        return self.frontend_tuner_status[idx].group_id

    def getTunerRfFlowId(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid Allocation ID {0}".format(allocation_id))
        return self.frontend_tuner_status[idx].rf_flow_id

    def setTunerCenterFrequency(self,allocation_id, freq):
        tuner_num = self.getTunerMapping(allocation_id)
        if tuner_num < 0: raise FRONTEND.FrontendException("Invalid Allocation ID {0}".format(allocation_id))
        if allocation_id != self.getControlAllocationId(tuner_num):
            raise FRONTEND.FrontendException("Allocation ID {0} is not authorized to modify the tuner's frequency.".format(allocation_id))
        if freq<0: raise FRONTEND.BadParameterException()
        
        if_freq = self.convert_rf_to_if(freq)
        valid_cf = None
        changed_child_numbers = []
        try:
            self.allocation_id_mapping_lock.acquire()
            #check this tuner is the only allocation left for a non-allocated parent
            parent_rx_channel = self.frontend_tuner_status[tuner_num].rx_object.rx_parent_object
            if self.checkLastSecondaryAllocation(tuner_num):
                # ok to retune parent and set my offset to this center frequency
                valid_cf=parent_rx_channel.get_valid_frequency(if_freq)
                parent_rx_channel.setFrequency_Hz(valid_cf)
                self.frontend_tuner_status[tuner_num].rx_object.updateRFFrequencyOffset(valid_cf)

            valid_cf = self.frontend_tuner_status[tuner_num].rx_object.get_valid_frequency(if_freq)
            if valid_cf is not None:
                self.debug_msg("setTunerCenterFrequency tuner {0} requested {1} new freq {2}",
                               tuner_num,
                               freq,
                               valid_cf)
                self.frontend_tuner_status[tuner_num].rx_object.setFrequency_Hz(if_freq)
            else:
                raise Exception("Invalid center frequency {0}".format(freq))

            try:
                for child in self.frontend_tuner_status[tuner_num].rx_object.getChildChannels():
                    child_tuner_id=None
                    if self.rx_channel_tuner_status.has_key(child):
                        child_tuner_id=self.rx_channel_tuner_status[child]
                        if self.frontend_tuner_status[child_tuner_id].allocated:
                            self.debug_msg("setTunerCenterFrequency Allocation ID: {0}"\
                                           " MSDD Channel {1}"\
                                           " New Frequency {2}",
                                           allocation_id,
                                           child.msdd_channel_id(),
                                           valid_cf)
                                           
                            if child.centerFrequencyRetune(valid_cf, keepOffset=True):
                                changed_child_numbers.append(child_tuner_id)
                        else:
                            child.updateRFFrequencyOffset(valid_cf)
            except Exception, e:
                if logging.NOTSET < self._baseLog.level < logging.INFO:
                    traceback.print_exc()
                self.warn_msg("Exception setting tuner's frequency to {0} for Allocation ID {1}, reason: {2} ", 
                              freq,
                              allocation_id,
                              e)
                              
        except:
            if logging.NOTSET < self._baseLog.level < logging.INFO:
                traceback.print_exc()
            error_string = "Error setting center frequency to {} for Allocation ID {}".format(freq,tuner_num,allocation_id)
            self.exception_msg(error_string)
            raise FRONTEND.BadParameterException(error_string)
        finally:
            self.allocation_id_mapping_lock.release()
            self.debug_msg("setTunerCenterFrequency update status for {0}", ",".join([ str(x) for x in [tuner_num]+changed_child_numbers]))
            self.update_tuner_status([tuner_num]+changed_child_numbers)

    def getTunerCenterFrequency(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid Allocation ID {0}".format(allocation_id))
        return self.frontend_tuner_status[idx].center_frequency

    def setTunerBandwidth(self,allocation_id, bw):
        tuner_num = self.getTunerMapping(allocation_id)
        if tuner_num < 0: raise FRONTEND.FrontendException("Invalid Allocation ID {0}".format(allocation_id))
        if allocation_id != self.getControlAllocationId(tuner_num):
            raise FRONTEND.FrontendException("Allocation ID {0} is not authorized to modify the tuner's bandwidth.".format(allocation_id))
        if bw<0: raise FRONTEND.BadParameterException()
        
        try:
            self.allocation_id_mapping_lock.acquire()
            self.frontend_tuner_status[tuner_num].rx_object.validate_bandwidth(bw)
            self.debug_msg("Setting bandwidth to {0} on tuner {1} for Allocation ID {2}",
                           bw,
                           tuner_num,
                           allocation_id)
            self.frontend_tuner_status[tuner_num].rx_object.setBandwidth_Hz(bw)
        except Exception, e:
            error_string = "Error setting tuner's bandwidth to {0} for Allocation ID {1}, reason: {2}".format(
                bw,
                allocation_id,
                e)
            self.exception_msg(error_string)
            raise FRONTEND.BadParameterException(error_string)
        finally:
            self.allocation_id_mapping_lock.release()
        self.update_tuner_status([tuner_num])

    def getTunerBandwidth(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid Allocation ID {0}".format(allocation_id))
        return self.frontend_tuner_status[idx].bandwidth

    def setTunerAgcEnable(self,allocation_id, enable):
        raise FRONTEND.NotSupportedException("setTunerAgcEnable not supported")

    def getTunerAgcEnable(self,allocation_id):
        raise FRONTEND.NotSupportedException("getTunerAgcEnable not supported")

    def setTunerGain(self,allocation_id, gain):
        tuner_num = self.getTunerMapping(allocation_id)
        if tuner_num < 0: raise FRONTEND.FrontendException("Invalid Allocation ID {0}".format(allocation_id))
        if allocation_id != self.getControlAllocationId(tuner_num):
            raise FRONTEND.FrontendException("Allocation ID {0} is not authorized to modify the tuner's gain.".format(allocation_id))
    
        valid_gain = None
        try:
            self.allocation_id_mapping_lock.acquire()
            if self.frontend_tuner_status[tuner_num].rx_object.is_analog():
                valid_gain = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.get_valid_gain(gain)
                self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.setGain(valid_gain)
            elif self.frontend_tuner_status[tuner_num].rx_object.is_digital():
                valid_gain = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_gain(gain)
                self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setGain(valid_gain)
            if valid_gain == None:
                raise Exception("")
        except Exception, e:
            error_string = "Error setting gain to {0} for Allocation ID {1}, reason: {2}".format(gain,allocation_id,e)
            self.exception_msg(error_string)
            raise FRONTEND.BadParameterException(error_string)
        finally:
            self.allocation_id_mapping_lock.release()
        self.update_tuner_status([tuner_num])

    def getTunerGain(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid Allocation ID {0}".format(allocation_id))
        return self.frontend_tuner_status[idx].gain

    def setTunerReferenceSource(self,allocation_id, source):
        raise FRONTEND.NotSupportedException("Can not change 10MHz reference")

    def getTunerReferenceSource(self,allocation_id):
        raise FRONTEND.NotSupportedException("getTunerReferenceSource not supported")

    def setTunerEnable(self,allocation_id, enable):
        tuner_num = self.getTunerMapping(allocation_id)
        if tuner_num < 0: raise FRONTEND.FrontendException("Invalid Allocation ID {0}".format(allocation_id))
        if allocation_id != self.getControlAllocationId(tuner_num):
            raise FRONTEND.FrontendException("Allocation ID {0} is not authorized to disable/enable the tuner.".format(allocation_id))
    
        try:
            self.frontend_tuner_status[tuner_num].enabled=enable
            self.frontend_tuner_status[tuner_num].rx_object.setEnable(enable)
        except Exception, e:
            msg="Exception when modifing tuner's enable state for Allocation ID {0}, reason {1} ".format(allocation_id,e)
            self.exception_msg(msg)
        self.update_tuner_status([tuner_num])

    def getTunerEnable(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid Allocation ID {0}".format(allocation_id))
        return self.frontend_tuner_status[idx].enabled


    def setTunerOutputSampleRate(self,allocation_id, sr):
        tuner_num = self.getTunerMapping(allocation_id)
        if tuner_num < 0: raise FRONTEND.FrontendException("Invalid Allocation ID {0}".format(allocation_id))
        if allocation_id != self.getControlAllocationId(tuner_num):
            raise FRONTEND.FrontendException("Allocation ID {0} is not authorized to modify the tuner's sample rate.".format(allocation_id))
        if sr<0: raise FRONTEND.BadParameterException()
        try:
            self.allocation_id_mapping_lock.acquire()
            self.frontend_tuner_status[tuner_num].rx_object.validate_sample_rate(sr)
            self.debug_msg("Setting sample rate to {0} for tuner {1}", sr, tuner_num)
            self.frontend_tuner_status[tuner_num].rx_object.setSampleRate(sr)
        except Exception, e:
            error_string = "Error setting tuner's sample rate to {0} for Allocation ID {1}, reason: {2}".format(sr,allocation_id,e)
            self.exception_msg(error_string)
            raise FRONTEND.BadParameterException(error_string)
        finally:
            self.allocation_id_mapping_lock.release()
        self.update_tuner_status([tuner_num])

    def getTunerOutputSampleRate(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid Allocation ID {0}".format(allocation_id))
        return self.frontend_tuner_status[idx].sample_rate

    '''
    *************************************************************
    Functions servicing the RFInfo port(s)
    - port_name is the port over which the call was received
    *************************************************************'''
    def get_rf_flow_id(self,port_name):
        return str(self.device_rf_flow)

    def set_rf_flow_id(self,port_name, flow_id):
        self.update_rf_flow_id(flow_id)

    def get_rfinfo_pkt(self,port_name):
        return self.device_rf_info_pkt

    def set_rfinfo_pkt(self,port_name, pkt):
        self.device_rf_info_pkt = pkt
        self.update_rf_flow_id(pkt.rf_flow_id)
  
if __name__ == '__main__':
    logging.getLogger().setLevel(logging.INFO)
    logging.debug("Starting Device")
    start_device(MSDD_i)

