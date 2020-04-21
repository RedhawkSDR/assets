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
# REDHAWK rh.MSDD is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
# details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.
#
#
#

from ossie.cf import ExtendedCF
from omniORB import CORBA
import struct #@UnresolvedImport
from bulkio.bulkioInterfaces import BULKIO, BULKIO__POA #@UnusedImport
# End of update_project insertions

from ossie.device import start_device
import logging
import copy

from MSDD_base import * 
from frontend.fe_types import *
from frontend.tuner_device import validateRequestVsRFInfo
# additional imports
import copy, math, sys
from pprint import pprint as pp
from omniORB import CORBA, any
from ossie import properties
import inspect 
# MSDD helper files/packages
from msddcontroller import InvalidValue
from msddcontroller import CommandException
from msddcontroller import MSDDRadio
from msddcontroller import create_csv
from datetime import datetime, time, timedelta
from time import sleep
import traceback

querycounter = 0 

def get_seconds_in_gps_time():
    previous_time = datetime(1980,1,6,0,0,11)
    seconds = get_utc_time_pair(previous_time)
    return seconds[0] + seconds[1]

def get_seconds_from_start_of_year():
    previous_time = datetime(int(datetime.now().year),1,1)
    seconds = get_utc_time_pair(previous_time)
    return seconds[0] + seconds[1]

def get_seconds_in_utc():
    previous_time = datetime(1970,1,1)
    seconds = get_utc_time_pair(previous_time)
    return seconds[0] + seconds[1]

def get_utc_time_pair(previous_time = datetime(1970,1,1)):
    utcdiff = datetime.utcnow() - previous_time
    whole_sec = (utcdiff.seconds + utcdiff.days * 24 * 3600)
    fract_sec = utcdiff.microseconds/10e6
    return [whole_sec,fract_sec]

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
    def __init__(self, tuner_channel_number, enabled, ip_address, port, protocol,vlan_enabled, vlan_tci, timestamp_offset,endianess,mfp_flush):
        self.tuner_channel_number = tuner_channel_number
        self.enabled = enabled
        self.ip_address = ip_address
        self.port = port
        self.protocol = protocol
        self.timestamp_offset = timestamp_offset
        self.vlan_tci = vlan_tci
        self.vlan_enabled = vlan_enabled
        self.endianess = endianess
        self.mfp_flush = mfp_flush

    def __str__(self):
        return "ch={0} (enb:{1}) ip:port={2}:{3} vlan{4} proto={5} endian={6} mfp={7}".format(self.tuner_channel_number, self.enabled, self.ip_address,
                                        self.port, self.vlan_tci, self.protocol, self.endianess, self.mfp_flush)


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
       
    def getter_with_default(self,_get_,default_value=None):
        try:
            ret = _get_()
            return ret
        except:
            if default_value != None:
                return default_value
            raise
    
    def extend_tuner_status_struct(self, struct_mod):
        s = struct_mod
        s.tuner_types=[]
        s.rx_object = None
        s.allocation_id_control = ""
        s.allocation_id_listeners = []
        
        s.internal_allocation = False
        s.internal_allocation_children = []
        s.internal_allocation_parent = None
        return s
    
    def create_allocation_id_csv(self, control_str, listener_list):
        aid_csv = control_str
        for i in range(0,len(listener_list)):
            aid_csv+=("," + str(listener_list[i]))
        return aid_csv    
    
    def constructor(self):
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
        
        self.addPropertyChangeListener("msdd_configuration",self.msdd_configuration_changed)
        self.addPropertyChangeListener("msdd_gain_configuration",self.msdd_gain_configuration_changed)
        self.addPropertyChangeListener("clock_ref",self.clock_ref_changed)
        self.addPropertyChangeListener("advanced",self.advanced_changed)
        self.addPropertyChangeListener("msdd_output_configuration",self.msdd_output_configuration_changed)
        self.addPropertyChangeListener("msdd_block_output_configuration",self.msdd_block_output_configuration_changed)
        self.addPropertyChangeListener("msdd_psd_configuration",self.msdd_psd_configuration_changed)
        self.addPropertyChangeListener("msdd_time_of_day_configuration",self.msdd_time_of_day_configuration_changed)
        self.addPropertyChangeListener("msdd_advanced_debugging_tools",self.msdd_advanced_debugging_tools_changed)
        

        self.connect_to_msdd()
        self.clock_ref_changed("clock_ref",self.clock_ref, self.clock_ref)
        self.advanced_changed("advanced",self.advanced,self.advanced)

        self._log.debug("Process PSD configuration changes ")
        self.msdd_psd_configuration_changed("msdd_psd_configuration",self.msdd_psd_configuration,self.msdd_psd_configuration)
        self._log.debug("Process Time Of Day  configuration changes ")
        self.msdd_time_of_day_configuration_changed("msdd_time_of_day_configuration",self.msdd_time_of_day_configuration,self.msdd_time_of_day_configuration)
        
        self._log.debug("Create output configuration for tuners and fft channels ")
        self.update_output_configuration()

        self._log.debug("Update tuner status")
        self.update_tuner_status()

        self._log.debug("Disable all tuner hardware and output")
        for tuner_num in range(0,len(self.frontend_tuner_status)):
            try:
                self.frontend_tuner_status[tuner_num].rx_object.setEnable(False)
            except:
                self._log.warn("Unable to disable tuner number: " + str(tuner_num))

        # The device creates the tuner status structure dynamically based on the device it is controlling so we are not using the setNumChannels
        #self.setNumChannels(1, "RX_DIGITIZER");
        # dump output configuration to make sure we don't have recursive loop
        if self._log.level < logging.INFO:
            if self.MSDD:
                for mod in self.MSDD.out_modules:
                    src_mods = self.MSDD.stream_router.getModulesFlowListByDestination(mod.object.full_reg_name)
                    dest_mods = self.MSDD.stream_router.getModulesFlowListBySource(mod.object.full_reg_name)
                    self._log.debug("output module: " + str(mod.object.full_reg_name) + " sources : " + str(src_mods))
                    self._log.debug("output module: " + str(mod.object.full_reg_name) + " dest : " + str(dest_mods))

    def getPort(self, name):
        if name == "AnalogTuner_in":
            self._log.trace("getPort() could not find port %s", name)
            raise CF.PortSupplier.UnknownPort()
        return MSDD_base.getPort(self, name)


    def updateUsageState(self):
        """
        This is called automatically after allocateCapacity or deallocateCapacity are called.
        Your implementation should determine the current state of the device:
           self._usageState = CF.Device.IDLE   # not in use
           self._usageState = CF.Device.ACTIVE # in use, with capacity remaining for allocation
           self._usageState = CF.Device.BUSY   # in use, with no capacity remaining for allocation
        """
        state =  FrontendTunerDevice.updateUsageState(self)
        if state != CF.Device.BUSY:
            try:
                # check cpu usage of radio and network utilization
                if not self.checkReceiverCpuLoad(self.advanced.max_cpu_load):
                    state=CF.Device.BUSY

                # check all output modules are within rate
                if not self.checkReceiverNetworkOutput():
                    state=CF.Device.BUSY
            except:
                pass

        return state


    def identifyDevice(self):
        return self.device_address + " (" + self.device_model + ")"

    def checkReceiverCpuLoad(self, max_cpu_load):
        """
        Get the current cpu load on radio and check if limit is within specified load maximum
        """
        load=0.0
        if self.msdd_console:
            try:
                load=self.msdd_console.cpu_load
            except:
                self._log.warn("Unable to get CPU load from MSDD device, msdd: " + self.identifyDevice())
        self._log.trace('checkReceiverCpuLoad, checking load, actual load: ' + str(load) + ' max ' + str(max_cpu_load))
        if load > max_cpu_load:
            self._log.warn('MSDD CPU exceeds maximum load (' + str(max_cpu_load) + " for msdd: " + self.identifyDevice())
            return False

        return True

    def checkReceiverNetworkOutput(self, additional_rate=None, out_module=None ):
        """
        check all enabled output modules if they are within the recommend rate for the radio's network interface.
        if the out_module parameter is specified then include that module's expected bit rate

        additional_rate = include this specific sample rate for the out_module parameter
        out_module = include the output module's bit rate if the module is disable
        """
        # hold calculated totals for all the network interfaces on the radio
        netstat=[]
        if additional_rate is None:
            additional_rate=0.0

        if self.MSDD:
            try:
                # seed netstat  list for each interface defined
                for  n in self.MSDD.network_modules:
                    try:
                        idx=self.MSDD.network_modules.index(n)
                        netstat.append( { 'interface' : idx,
                                          'total' : 0.0,
                                          'bit_rate' : n.object.bit_rate*1e6
                                      })
                    except:
                        if logging.NOTSET < self._log.level < logging.INFO:
                            traceback.print_exc()

                self._log.trace('checkReceiverNetworkOutput msdd receiver interfaces '  + str(netstat))

                #
                # resolve, this list can be large we might want to just look at
                # output modules for each rx_channel and fft_channels instead all possible output modules
                #
                for mod in self.MSDD.out_modules:
                    pkt_bit_rate=0
                    m=mod.object    # actual MSDD output module
                    if m.enable or mod == out_module:
                        # get interface number, msdd reports net:x x:1-n, IPP command take 0-(n-1)
                        inf=m.interface-1
                        if inf < 0: inf=0

                        srate=m.input_sample_rate # get input rate into module
                        if m == out_module:
                            # add in param if passed, used during allocations to check
                            srate=additional_rate

                        pkt_data_len=m.samples_per_frame  # pkt length in samples
                        data_width =m.data_width          # sample length in bytes
                        proto_name=m.protocol_readable    # protocol
                        proto_extra_bits=0                # default for raw
                        pkt_rate =  (srate*data_width)/pkt_data_len
                        self._log.debug("checkReceiverNetworkOutput, interface %s out module %s pkt_data_len %s data_width %s pkt_rate %s netstat %s " % (inf, m.channel_id(), pkt_data_len,
                                                                                                                                                          data_width,
                                                                                                                                                          pkt_bit_rate, netstat[inf]))
                        if "SDDS" in proto_name:
                            pkt_header=56 # SDDS header length (56 bytes)
                        if "VITA49" in proto_name:
                            pkt_header = 28  # IF data packet header in bits (7 words (32bits) )
                            # resolve, need to add in context packets, initial testing shows no context packets..
                        pkt_bit_rate = pkt_rate*pkt_data_len*(8*data_width) + pkt_rate*pkt_header*8
                        self._log.debug("checkReceiverNetworkOutput, interface %s out module %s pkt_rate %s netstat %s " % (inf, m.channel_id(), pkt_bit_rate, netstat[inf]))
                        netstat[inf]['total'] += pkt_bit_rate

                # check all netstats
                for n in netstat:
                    max_allowable_bit_rate=n['bit_rate'] * (self.advanced.max_nic_percentage/100.0)
                    self._log.debug("checkReceiverNetworkOutput " + str(n['interface']) + " total rate " + str(n['total']) + \
                                    " (bps) max allowable rate " + str(max_allowable_bit_rate) + " (bps) " + \
                                    "  exceeds: " + str(n['total'] > max_allowable_bit_rate))
                    if n['total'] > max_allowable_bit_rate:
                        self._log.error("Network rate exceeded for " + str(n['interface']) + " total rate " + str(n['total']) + " (bps)  max allowable rate " + str(max_allowable_bit_rate)+ " (bps)")
                        return False
            except:
                if logging.NOTSET < self._log.level < logging.INFO:
                    traceback.print_exc()
                self._log.error("Unable to determine network utilization for msdd: " + self.identifyDevice())

        return True

    def disconnect_from_msdd(self):
        if self.MSDD != None:
            #Need to tear down old devices here if needed
            for t in range(0,len(self.frontend_tuner_status)):
                try:
                    if self.frontend_tuner_status[t].enabled:
                        self.frontend_tuner_status[t].enabled = False
                        self.purgeListeners(t)
                except IndexError:
                    self._log.warn('disconnect_from_msdd - frontend_tuner_status[%d] accessed, but does not exist'%t)
                    break
            self.frontend_tuner_status=[]
            self.msdd_status=MSDD_base.msdd_status_struct()
        self.MSDD=None
        self.update_msdd_status()
        self.update_tuner_status()
        
    
    def connect_to_msdd(self, update_tuners=True):
        if self.MSDD != None:
            valid_ip = False
            for nw_module in self.MSDD.network_modules:
                if  self.msdd_configuration.msdd_ip_address == nw_module.object.ip_address: 
                    valid_ip = True
                    break
            if not valid_ip:
                self._log.info("MSDD connection changed, ip " + str(self.msdd_configuration.msdd_ip_address) + " port " + str(self.msdd_configuration.msdd_port))
                self.disconnect_from_msdd()
        if self.MSDD == None:
            self.msdd_console=None
            try:
                self._log.info("Connecting to MSDD ip "  + str(self.msdd_configuration.msdd_ip_address) + " port " + str(self.msdd_configuration.msdd_port))
                self.MSDD = MSDDRadio(self.msdd_configuration.msdd_ip_address,
                                      self.msdd_configuration.msdd_port,
                                      self.advanced.udp_timeout,
                                      enable_inline_swddc=self.advanced.enable_inline_swddc,
                                      enable_secondary_tuners=self.advanced.enable_secondary_tuners,
                                      enable_fft_channels=self.advanced.enable_fft_channels)

                
                rate_failure = None
                for net_mods in self.MSDD.network_modules:
                    connected_rate = net_mods.object.bit_rate
                    if connected_rate < self.advanced.minimum_connected_nic_rate:
                        rate_failure = connected_rate

                if rate_failure:
                    self._log.warn("MSDD connection rate of " + str(rate_failure) + " Mbps which does not meet minimum rate check, " + str(self.advanced.minimum_connected_nic_rate) + " Mbps")
                    raise Exception("MSDD connection rate check failure")

                self.msdd_console = self.MSDD.console
                self.update_msdd_status()
                if update_tuners:
                    self.update_tuner_status()
            except:
                if logging.NOTSET < self._log.level < logging.INFO:
                    traceback.print_exc()
                self._log.exception("Unable to connect to MSDD, IP:PORT =  " + str(self.msdd_configuration.msdd_ip_address) + ":" + str(self.msdd_configuration.msdd_port))
                self.disconnect_from_msdd()
                return False

        return True    

    def update_rf_flow_id(self, rf_flow_id):
	self.device_rf_flow = rf_flow_id
	for tuner_num in range(0,len(self.frontend_tuner_status)):
	    self.frontend_tuner_status[tuner_num].rf_flow_id = self.device_rf_flow

    def update_msdd_status(self):
        if self.MSDD == None:
            self.msdd_status=MSDD_base.msdd_status_struct() #Reset to defaults
            return True
        try:
            self.device_model=str(self.msdd_console.model)
            self.device_address = self.msdd_console.address
            self.msdd_status.connected = True
            self.msdd_status.ip_address = str(self.msdd_console.ip_address)
            self.msdd_status.port = str(self.msdd_console.ip_port)
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
                self.msdd_status.tod_bit_state = tod.bit_readable
                self.msdd_status.tod_toy = str(tod.toy)
        except:
            if logging.NOTSET < self._log.level < logging.INFO:
                traceback.print_exc()
            print self._log.level
            self._log.error("ERROR WHEN UPDATING MSDD_STATUS STRUCTURE")
            return False
    
        return True

    def getTunerTypeList(self,type_mode):
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

        #Make sure status structure is initialized
        if len(self.frontend_tuner_status) <= 0:
            self.dig_tuner_base_nums = []
            for tuner_num in range(0,len(self.MSDD.rx_channels)):
                #Determine frontend tuner type
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
                    tuner_types = self.getTunerTypeList(self.advanced.sw_ddc_mode)
                elif msdd_type_string == MSDDRadio.MSDDRXTYPE_SPC:
                    continue
                else:
                    self._log.error("UNKOWN TUNER TYPE " + str(msdd_type_string) + " REPORTED BY THE MSDD")

                # skip empty tuner types
                if len(tuner_types) == 0 : continue

                tuner_type = ""
                if len(tuner_types) > 0:
                        tuner_type = tuner_types[0]
                #create structure            
                tuner_struct = MSDD_base.frontend_tuner_status_struct_struct(tuner_type=tuner_type,
                                                              available_tuner_type = create_csv(tuner_types),
                                                              msdd_registration_name_csv= self.MSDD.rx_channels[tuner_num].get_registration_name_csv(),
                                                              msdd_installation_name_csv= self.MSDD.rx_channels[tuner_num].get_install_name_csv(),
                                                              tuner_number=tuner_num,
                                                              msdd_channel_type = msdd_type_string)
                tuner_struct = self.extend_tuner_status_struct(tuner_struct)                          
                tuner_struct.tuner_types = tuner_types
                tuner_struct.rx_object = self.MSDD.rx_channels[tuner_num]
                self._log.debug("Adding tuner: " + str(tuner_num) \
                                + " type: " + str(tuner_type)+":"+",".join(tuner_types)  \
                                + " msdd channel flow: " + tuner_struct.rx_object.get_digital_signal_path())
                self.rx_channel_tuner_status[tuner_struct.rx_object]=tuner_num
                self.frontend_tuner_status.append(tuner_struct)
                

        #Update
        if len(tuner_range) <= 0:
            tuner_range = range(0,len(self.frontend_tuner_status))
            self._log.trace("update_tuner status, refresh tuners 0.."+str(len(self.frontend_tuner_status)))
        for tuner_num in tuner_range:            
            try:
                data_port = True
                fft_port = False
                #for just analog tuners
                status = self.frontend_tuner_status[tuner_num].rx_object.getStatus()
                if self.frontend_tuner_status[tuner_num].rx_object.is_analog() and not self.frontend_tuner_status[tuner_num].rx_object.is_digital():
                    self._log.trace("update_tuner_status update ANALOG tuner settings for tuner " + str(tuner_num))
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
                    self._log.trace("update_tuner_status update DIGITAL tuner settings for tuner " + str(tuner_num))
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
                    self._log.trace("update_tuner_status update ANALOG/DIGITAL tuner settings for tuner " + str(tuner_num))
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
                    self._log.trace("update_tuner_status update STREAMING context for tuner " + str(tuner_num))
                    if status:
                        self.frontend_tuner_status[tuner_num].output_format = "CI"
                        self.frontend_tuner_status[tuner_num].output_multicast = status.output_multicast
                        self.frontend_tuner_status[tuner_num].output_port = status.output_port
                        self.frontend_tuner_status[tuner_num].output_enabled = status.output_enabled
                        self.frontend_tuner_status[tuner_num].output_protocol = status.output_protocol
                        self.frontend_tuner_status[tuner_num].output_vlan_enabled = status.output_vlan_enabled
                        self.frontend_tuner_status[tuner_num].output_vlan_tci = status.output_vlan_tci
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
                self._log.trace("update_tuner_status, completed tuner status update for tuner " + str(tuner_num))
            except Exception, e:
                self._log.exception((e))
                self._log.error("ERROR WHEN UPDATING TUNER STATUS FOR TUNER: "+ str(tuner_num))
                
            
    def update_output_configuration(self):
        self._log.debug("update_output_configuration " )
        if self.MSDD == None:
            return False

        self.tuner_output_configuration={}
        
        self._log.debug("Setting up output configuration from msdd_block_configuration")
        # output block configuration
        for block_oc_num in range(0,len(self.msdd_block_output_configuration)):
            min_num = self.msdd_block_output_configuration[block_oc_num].tuner_number_start
            max_num = self.msdd_block_output_configuration[block_oc_num].tuner_number_stop
            if max_num < min_num:
                max_num = min_num
            
            ip_address=self.msdd_block_output_configuration[block_oc_num].ip_address
            port=self.msdd_block_output_configuration[block_oc_num].port
            vlan_enabled=(self.msdd_block_output_configuration[block_oc_num].vlan > 0 and self.msdd_block_output_configuration[block_oc_num].vlan_enable)
            vlan=max(0,self.msdd_block_output_configuration[block_oc_num].vlan)
            
            for oc_num in range(min_num,max_num+1):
                self.tuner_output_configuration[oc_num] = output_config(tuner_channel_number=oc_num,
                                                                        enabled=self.msdd_block_output_configuration[block_oc_num].enabled,
                                                                        ip_address= str(ip_address).strip(),
                                                                        port=port,
                                                                        vlan_enabled=vlan_enabled,
                                                                        vlan_tci= vlan,
                                                                        protocol=self.msdd_block_output_configuration[block_oc_num].protocol,
                                                                        endianess = self.msdd_block_output_configuration[block_oc_num].endianess,
                                                                        timestamp_offset=self.msdd_block_output_configuration[block_oc_num].timestamp_offset,
                                                                        mfp_flush=self.msdd_block_output_configuration[block_oc_num].mfp_flush,
                )
                if self.msdd_block_output_configuration[block_oc_num].increment_ip_address:
                    try:
                        ip_address = increment_ip(ip_address)
                    except:
                        self._log.error("Can not further increment ip address: " + str(ip_address))
                        continue
                if self.msdd_block_output_configuration[block_oc_num].increment_port:
                    port +=1
                if self.msdd_block_output_configuration[block_oc_num].increment_vlan and vlan >= 0:
                    vlan +=1

        self._log.debug("Setting up output configuration from msdd_output_configuration")
        # msdd_output_configuration
        for oc_num in range(0,len(self.msdd_output_configuration)):
                tuner_num = self.msdd_output_configuration[oc_num].tuner_number
                vlan_enabled=(self.msdd_output_configuration[oc_num].vlan > 0 and self.msdd_output_configuration[oc_num].vlan_enable)
                vlan=max(0,self.msdd_output_configuration[oc_num].vlan)
                self.tuner_output_configuration[tuner_num] = output_config(tuner_channel_number=tuner_num,
                                                                           enabled=self.msdd_output_configuration[oc_num].enabled,
                                                                           ip_address=str(self.msdd_output_configuration[oc_num].ip_address).strip(),
                                                                           port=self.msdd_output_configuration[oc_num].port,
                                                                           vlan_enabled=vlan_enabled,
                                                                           vlan_tci=vlan,
                                                                           protocol=self.msdd_output_configuration[oc_num].protocol,
                                                                           endianess = self.msdd_output_configuration[oc_num].endianess,
                                                                           timestamp_offset=self.msdd_output_configuration[oc_num].timestamp_offset,
                                                                           mfp_flush=self.msdd_output_configuration[oc_num].mfp_flush)
    

        self._log.debug("Configure output modules from configuration cache ")
        for tuner_num in range(0,len(self.frontend_tuner_status)):
            try:
                _rx_object = self.frontend_tuner_status[tuner_num].rx_object
                _rx_object.set_output_enable(False)
                if not _rx_object.has_streaming_output():
                    continue
                _output_mod = _rx_object.output_object.object

                if not self.tuner_output_configuration.has_key(tuner_num):
                    continue
                success = _output_mod.setAddressPort(self.tuner_output_configuration[tuner_num].ip_address,str(self.tuner_output_configuration[tuner_num].port))
                self._log.trace("update_output_configuration tuner " + str(tuner_num) +  " ip address " + str(success))
                proto = _output_mod.getOutputProtocolNumberFromString(self.tuner_output_configuration[tuner_num].protocol)
                self._log.trace("update_output_configuration tuner " + str(tuner_num) +  " protocol " + str(success))
                success &= _output_mod.setOutputProtocol(self.tuner_output_configuration[tuner_num].protocol)
                if self.tuner_output_configuration[tuner_num].protocol=="UDP_VITA49":
                    success &= _output_mod.setCDR(self.VITA49CONTEXTPACKETINTERVAL)
                    success &= _output_mod.setCCR(1)
                success &= _output_mod.setTimestampOff(self.tuner_output_configuration[tuner_num].timestamp_offset)
                self._log.trace("update_output_configuration tuner " + str(tuner_num) +  " timestamp offset " + str(success))
                success &= _output_mod.setEnableVlanTagging(self.tuner_output_configuration[tuner_num].vlan_enabled)
                self._log.trace("update_output_configuration tuner " + str(tuner_num) +  " vlan enable " + str(success))
                success &= _output_mod.setOutputEndianess(self.tuner_output_configuration[tuner_num].endianess)
                self._log.trace("update_output_configuration tuner " + str(tuner_num) +  " endianess " + str(success))
                success &= _output_mod.setVlanTci(max(0,self.tuner_output_configuration[tuner_num].vlan_tci))
                self._log.trace("update_output_configuration tuner " + str(tuner_num) +  " vlan tci " + str(success))
                enable = self.tuner_output_configuration[tuner_num].enabled
                _output_mod.setMFP(self.tuner_output_configuration[tuner_num].mfp_flush)
                self._log.debug("output configuration: tuner " + str(tuner_num) +
                                " enabled " + str(enable) +
                                " protocol " + self.tuner_output_configuration[tuner_num].protocol +
                                " ip-addr/port/vlan " + self.tuner_output_configuration[tuner_num].ip_address + "/" +
                                str(self.tuner_output_configuration[tuner_num].port) + "/" +
                                str(self.tuner_output_configuration[tuner_num].vlan_tci ) )
                if not success:
                    self._log.error("ERROR WHEN UPDATING OUTPUT CONFIGURATION FOR TUNER: "+ str(tuner_num) + " WITH VALUES: " + str(self.tuner_output_configuration[tuner_num]))
            except:
                self._log.error("ERROR WHEN UPDATING OUTPUT CONFIGURATION FOR TUNER: "+ str(tuner_num))
        
        #
        # Process PSD output configuration and assign to FFT Channels
        #

        self.psd_output_configuration={}

        # psd only supports block configuration
        for block_oc_num in range(0,len(self.msdd_psd_output_configuration)):
            min_num = self.msdd_psd_output_configuration[block_oc_num].fft_channel_start
            max_num = self.msdd_psd_output_configuration[block_oc_num].fft_channel_stop
            if max_num < min_num:
                max_num = min_num

            ip_address=self.msdd_psd_output_configuration[block_oc_num].ip_address
            port=self.msdd_psd_output_configuration[block_oc_num].port
            vlan_enabled=(self.msdd_psd_output_configuration[block_oc_num].vlan > 0 and self.msdd_psd_output_configuration[block_oc_num].vlan_enable)
            vlan=max(0,self.msdd_psd_output_configuration[block_oc_num].vlan)

            for oc_num in range(min_num,max_num+1):
                self.psd_output_configuration[oc_num] = output_config(tuner_channel_number=oc_num,
                                                enabled=self.msdd_psd_output_configuration[block_oc_num].enabled,
                                                ip_address= str(ip_address).strip(),
                                                port=port,
                                                vlan_enabled=vlan_enabled,
                                                vlan_tci= vlan,
                                                protocol=self.msdd_psd_output_configuration[block_oc_num].protocol,
                                                endianess = self.msdd_psd_output_configuration[block_oc_num].endianess,
                                                timestamp_offset=self.msdd_psd_output_configuration[block_oc_num].timestamp_offset,
                                                mfp_flush=self.msdd_psd_output_configuration[block_oc_num].mfp_flush,
                                                )
                if self.msdd_psd_output_configuration[block_oc_num].increment_ip_address:
                    try:
                        ip_address = increment_ip(ip_address)
                    except:
                        self._log.error("Can not further increment ip address: " + str(ip_address))
                        continue
                if self.msdd_psd_output_configuration[block_oc_num].increment_port:
                    port +=1
                if self.msdd_psd_output_configuration[block_oc_num].increment_vlan and vlan >= 0:
                    vlan +=1

        self._log.debug("Configure FFT channels output context and disable FFT output ")
        for fidx in range(0,len(self.MSDD.fft_channels)):
            fft_channel=self.MSDD.fft_channels[fidx]
            try:
                fft_chan_num=fft_channel.channel_number()
                fft_channel.setEnable(False)
                if not self.psd_output_configuration.has_key(fft_chan_num): continue

                output_cfg=self.psd_output_configuration[fft_chan_num]

                success = fft_channel.output.object.setAddressPort(output_cfg.ip_address,str(output_cfg.port))
                proto = fft_channel.output.object.getOutputProtocolNumberFromString(output_cfg.protocol)
                success &= fft_channel.output.object.setOutputProtocol(proto)
                if output_cfg.protocol=="UDP_VITA49":
                    success &= fft_channel.output.object.setCDR(self.VITA49CONTEXTPACKETINTERVAL)
                    success &= fft_channel.output.object.setCCR(1)
                success &= fft_channel.output.object.setTimestampOff(output_cfg.timestamp_offset)
                success &= fft_channel.output.object.setEnableVlanTagging(output_cfg.vlan_enabled)
                success &= fft_channel.output.object.setOutputEndianess(output_cfg.endianess)
                success &= fft_channel.output.object.setVlanTci(max(0,output_cfg.vlan_tci))
                fft_channel.output.object.setMFP(output_cfg.mfp_flush)

                self._log.trace("update_output_configuration fft channel " + str(fft_chan_num) +
                                " protocol " + self.psd_output_configuration[fft_chan_num].protocol +
                                " ip-addr/port/vlan " + self.psd_output_configuration[fft_chan_num].ip_address + "/" +
                                str(self.psd_output_configuration[fft_chan_num].port) )

                if not success:
                    self._log.error("Error during configuring output for FFT Channel " + fft_channel.channel_id())


            except:
                if logging.NOTSET < self._log.level < logging.INFO:
                    traceback.print_exc()
                self._log.error("Error disabling output for  FFT Channels: "+ fft_channel.channel_id())

    def set_default_msdd_gain_value_for_tuner(self,tuner_num):
        try:
            msdd_type_string = self.frontend_tuner_status[tuner_num].rx_object.get_msdd_type_string()
            if msdd_type_string == MSDDRadio.MSDDRXTYPE_ANALOG_RX:
                valid_gain = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.get_valid_gain(self.msdd_gain_configuration.rcvr_gain)
                self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.setGain(valid_gain)
            elif msdd_type_string == MSDDRadio.MSDDRXTYPE_DIGITAL_RX:
                try:
                    valid_gain = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.get_valid_gain(self.msdd_gain_configuration.rcvr_gain)
                    self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.setGain(valid_gain)
                except:
                    None
                valid_gain = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_gain(self.msdd_gain_configuration.wb_ddc_gain)
                self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setGain(valid_gain)
            elif msdd_type_string == MSDDRadio.MSDDRXTYPE_HW_DDC:
                valid_gain = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_gain(self.msdd_gain_configuration.hw_ddc_gain)
                self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setGain(valid_gain)
            elif msdd_type_string == MSDDRadio.MSDDRXTYPE_SW_DDC:
                valid_gain = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_gain(self.msdd_gain_configuration.sw_ddc_gain)
                self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setGain(valid_gain)
        except:
            self._log.warn('Could not set gain value for tuner ' + str(tuner_num))        


    def msdd_gain_configuration_changed(self, propid,oldval, newval):
        self.msdd_gain_configuration = newval

    def clock_ref_changed(self, propid,oldval, newval):
        self.clock_ref = newval
        if self.MSDD == None:
            return
        self.allocation_id_mapping_lock.acquire()
        try:

            for tuner_num in range(0,len(self.frontend_tuner_status)):
                if self.frontend_tuner_status[tuner_num].rx_object.is_analog():
                    self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.setExternalRef(self.clock_ref)
            for board_num in range(0,len(self.MSDD.board_modules)):
                self.MSDD.board_modules[board_num].object.setExternalRef(self.clock_ref)
        except:
            self._log('Exception occurred when trying to configure external reference clock')
        finally:
            self.allocation_id_mapping_lock.release()
        self.update_msdd_status()


    def advanced_changed(self, propid,oldval, newval):
        try:
            self.allocation_id_mapping_lock.acquire()

            self.advanced = newval
            if self.MSDD != None:
                self.MSDD.connection.set_timeout(self.advanced.udp_timeout)

            for tuner_num in range(0,len(self.frontend_tuner_status)):

                tuner_types=[]
                if self.MSDD.rx_channels[tuner_num].is_analog_only(): #Receiver
                    tuner_types = self.getTunerTypeList(self.advanced.rcvr_mode)
                elif self.frontend_tuner_status[tuner_num].rx_object.is_digital_only() and self.frontend_tuner_status[tuner_num].rx_object.is_hardware_based(): # hw_ddc
                    tuner_types = self.getTunerTypeList(self.advanced.hw_ddc_mode)
                elif self.frontend_tuner_status[tuner_num].rx_object.is_digital_only(): #sw_ddc
                    tuner_types = self.getTunerTypeList(self.advanced.sw_ddc_mode)
                elif self.frontend_tuner_status[tuner_num].rx_object.is_analog_and_digital():  #wb_ddc
                    tuner_types = self.getTunerTypeList(self.advanced.wb_ddc_mode)
                else:
                    raise Exception("UNKOWN TUNER TYPE")
                self.frontend_tuner_status[tuner_num].tuner_types = tuner_types
        finally:
            self.allocation_id_mapping_lock.release()

    def msdd_output_configuration_changed(self, propid, oldval, newval):
        self.msdd_output_configuration = newval

    def msdd_block_output_configuration_changed(self, propid, oldval, newval):
        self.msdd_block_output_configuration = newval
        
    def msdd_psd_configuration_changed(self,propid, oldval, newval):
        self.msdd_psd_configuration = newval
        
    def msdd_time_of_day_configuration_changed(self,propid, oldval, newval):
        self.msdd_time_of_day_configuration = newval
        if self.MSDD == None:
            return False
        
        if len(self.MSDD.tod_modules) > 0:
            #mode_num = self.MSDD.tod_modules[0].object.getModeNumFromString(self.msdd_time_of_day_configuration.mode)
            self.MSDD.tod_modules[0].object.mode = self.msdd_time_of_day_configuration.mode
            self.msdd_time_of_day_configuration.mode = str(self.MSDD.tod_modules[0].object.mode_readable)
            self.MSDD.tod_modules[0].object.ref_adjust = int(self.msdd_time_of_day_configuration.reference_adjust)
            self.MSDD.tod_modules[0].object.reference_track = int(self.msdd_time_of_day_configuration.reference_track)
            if str(self.msdd_time_of_day_configuration.mode) == "IRIGB":
                self.MSDD.tod_modules[0].object.toy = int(self.msdd_time_of_day_configuration.toy)
            else:
                self.MSDD.tod_modules[0].object.toy = 1
            if self.MSDD.tod_modules[0].object.mode <= 1:
                vita49_time = False
                for tuner_num in range(0,len(self.frontend_tuner_status)):
                    try:
                        if not self.frontend_tuner_status[tuner_num].rx_object.has_streaming_output():
                            continue
                        if not self.frontend_tuner_status[tuner_num].rx_object.output_object.object.getEnable():
                            continue
                        
                        if str(self.frontend_tuner_status[tuner_num].rx_object.output_object.object.getOutputProtocolString()).find("VITA49") >= 0:
                            vita49_time = True
                        break
                    except:
                        None
                        
                '''
                The time is set on the next pps so the time we want is now +1 second to be accurate.
                '''        
                if vita49_time:
                    self._log.debug("Setting timestamp to GPS time")
                    self.MSDD.tod_modules[0].object.time = get_seconds_in_gps_time() + 1 
                else:
                    self._log.debug("Setting timestamp to SDDS time")
                    self.MSDD.tod_modules[0].object.time = get_seconds_from_start_of_year() + 1
        
        self.update_msdd_status()
        
        
    def msdd_configuration_changed(self, propid,oldval, newval):
        self._log.debug("MSDD Configuration changed. New value: " + str(newval))
        self.msdd_configuration = newval
        if not self.connect_to_msdd(): 
            sleep(0.25)
            self.connect_to_msdd()
        
    def msdd_advanced_debugging_tools_changed(self, propid,oldval, newval):
        self.msdd_advanced_debugging_tools = newval
        self.msdd_advanced_debugging_tools.command = str(self.msdd_advanced_debugging_tools.command).rstrip('\n')
       
        if len(self.msdd_advanced_debugging_tools.command) <= 0:
            self.msdd_advanced_debugging_tools.response = "[ COMMAND FIELD IS EMPTY ]"
            return
        if not self.advanced.enable_msdd_advanced_debugging_tools:
            self.msdd_advanced_debugging_tools.response = "[ CAN NOT USE ADVANCED TOOLS UNTIL ENABLED IN ADVANCED PROPERTIES ]"
            return
        if self.MSDD == None:
            self.msdd_advanced_debugging_tools.response = "[ CAN NOT USE ADVANCED TOOLS UNTIL MSDD IS CONNECTED ]"
            return
        try:
            self.msdd_advanced_debugging_tools.response = self.msdd_console.send_custom_command(self.msdd_advanced_debugging_tools.command + '\n')
            self.msdd_advanced_debugging_tools.response = self.msdd_advanced_debugging_tools.response.rstrip('\n')
            if len(self.msdd_advanced_debugging_tools.response) <= 0:
                self.msdd_advanced_debugging_tools.response = "[ RESPONSE EMPTY ]"
            return
        except:
            self.msdd_advanced_debugging_tools.response = "[ NO RESPONSE RECEIVED FROM RADIO. THIS IS VALID FOR SETTER COMMANDS ]"
            return     

    def process(self):
        return NOOP   

    def range_check(self, req_cf, req_bw, req_bw_tol, req_sr, req_sr_tol , cur_cf, cur_bw, cur_sr):
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
        if not self.frontend_tuner_status[tunerNum].allocated or not self.frontend_tuner_status[tunerNum].enabled:
            return
        
        if data_port:
            #tuner is active, so generate timestamp and header info
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
            
            #use system time for timestamp...
            ts = get_utc_time_pair()
            T = BULKIO.PrecisionUTCTime(tcmode=0,
                                        tcstatus=0,
                                        toff=0,
                                        twsec=ts[0],
                                        tfsec=ts[1])
            for alloc_id in self.getTunerAllocations(tunerNum):
                H.streamID = alloc_id
                self.port_dataSDDS_out.pushSRI(H, T)
                self.port_dataVITA49_out.pushSRI(H, T)
                self._log.debug("Pushing DATA SRI on alloc_id: " + str(alloc_id) + " with contents: " + str(H))
        
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
                self._log.debug("Pushing FFT SRI on alloc_id: " + str(alloc_id) + " with contents: " + str(H))            

    def sendAttach(self, alloc_id, tuner_num, data_port = True, fft_port = False ):
        self._log.debug("Sending Attach() on ID:"+str(alloc_id))
    
        if data_port:
            if self.frontend_tuner_status[tuner_num].output_enabled:
                sdef = BULKIO.SDDSStreamDefinition(id = alloc_id,
                                                   dataFormat = BULKIO.SDDS_CI,
                                                   multicastAddress = self.frontend_tuner_status[tuner_num].output_multicast,
                                                   vlan = int(self.frontend_tuner_status[tuner_num].output_vlan),
                                                   port = int(self.frontend_tuner_status[tuner_num].output_port),
                                                   sampleRate = int(self.frontend_tuner_status[tuner_num].sample_rate),
                                                   timeTagValid = True,
                                                   privateInfo = "MSDD Physical Tuner #"+str(tuner_num) )
               
                self._log.debug("calling Port Attach() on ID:"+str(alloc_id))
                self.port_dataSDDS_out.attach(sdef, alloc_id)

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
                self.port_dataVITA49_out.attach(vdef, alloc_id)
                        
        if fft_port:
            if self.frontend_tuner_status[tuner_num].output_enabled:
                if self.psd_output_configuration.has_key(tuner_num) and self.psd_output_configuration[tuner_num].enabled:
                    num_bins = self.frontend_tuner_status[tuner_num].psd_output_bin_size
                    if num_bins <= 0:
                        num_bins = self.frontend_tuner_status[tuner_num].psd_fft_size
                    binFreq = self.frontend_tuner_status[tuner_num].sample_rate / num_bins;
                    sdef = BULKIO.SDDSStreamDefinition(id = alloc_id,
                                                       dataFormat = BULKIO.SDDS_SI,
                                                       multicastAddress = self.psd_output_configuration[tuner_num].ip_address,
                                                       vlan = int(self.psd_output_configuration[tuner_num].vlan_tci),
                                                       port = int(self.psd_output_configuration[tuner_num].port),
                                                       sampleRate = int(binFreq),
                                                       timeTagValid = True,
                                                       privateInfo = "MSDD FFT Tuner #"+str(tuner_num) )
                    self.port_dataSDDS_out_PSD.attach(sdef, alloc_id)


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
                                                         vlan = int(self.psd_output_configuration[tuner_num].vlan_tci),
                                                         port = int(self.psd_output_configuration[tuner_num].port),
                                                         protocol = BULKIO.VITA49_UDP_TRANSPORT,
                                                         valid_data_format = True,
                                                         data_format = vita_data_payload_format)
                    except:
                        vdef = BULKIO.VITA49StreamDefinition(ip_address=  self.psd_output_configuration[tuner_num].ip_address,
                                                         vlan = int(self.psd_output_configuration[tuner_num].vlan_tci),
                                                         port = int(self.psd_output_configuration[tuner_num].port),
                                                         id = alloc_id,
                                                         protocol = BULKIO.VITA49_UDP_TRANSPORT,
                                                         valid_data_format = True,
                                                         data_format = vita_data_payload_format)


                    self.port_dataVITA49_out_PSD.attach(vdef, alloc_id)

        #update and send SRI too
        self.SRIchanged(tuner_num, data_port, fft_port)
        
    
    def sendDetach(self, alloc_id, data_port = True, fft_port = True):
        self._log.debug("Sending Detach() on ID:"+str(alloc_id))
 
        if data_port:
            self.port_dataSDDS_out.detach(connectionId=alloc_id)
            self.port_dataVITA49_out.detach(connectionId=alloc_id)
        if fft_port:
            self.port_dataSDDS_out_PSD.detach(connectionId=alloc_id)
            self.port_dataVITA49_out_PSD.detach(connectionId=alloc_id)

    def enableFFT(self, alloc_id ):
        
        tuner_num = self.getTunerMapping(alloc_id)
        
        if tuner_num is None or tuner_num < 0: return

        fft_channel = self.MSDD.get_fft_channel()
        
        if fft_channel is None:
            self.frontend_tuner_status[tuner_num].rx_object.removeFFTChannel()
            return
        
        try:
            self.MSDD.stream_router.unlinkModule(fft_channel.channel_id())

            enable=False
            if self.psd_output_configuration.has_key(fft_channel.channel_number()):
                enable=self.psd_output_configuration[fft_channel.channel_number()].enabled

            if not enable:
                fft_channel.setEnable(False)
                self._log.info("PSD output configuration missing or not enabled for FFT channel " + str(fft_channel.channel_id()))
                return

            fft=fft_channel.fft.object
            fft.fftSize = self.msdd_psd_configuration.fft_size
            fft.time_between_fft_ms = self.msdd_psd_configuration.time_between_ffts
            fft.outputBins = self.msdd_psd_configuration.output_bin_size
            fft.window_type_str = self.msdd_psd_configuration.window_type
            fft.peak_mode_str = self.msdd_psd_configuration.peak_mode

            ffts_per_second = self.frontend_tuner_status[tuner_num].sample_rate / self.msdd_psd_configuration.fft_size
            num_avg = ffts_per_second * self.msdd_psd_configuration.time_average
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

            self._log.debug("Enabling FFT Channel to Tuner " + str(tuner_num) + " FFT " + fft_channel.channel_id())
            fft_channel.setEnable(True)
            self.sendAttach(alloc_id, tuner_num,  data_port=False, fft_port=True)
        except:
            if logging.NOTSET < self._log.level < logging.INFO:
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
            if fft_channel:
                fft_channel.unlink()
            self._log.debug("Disable FFT Channel for Tuner " + str(tuner_num) + " FFT " + fft_channel.channel_id())
            fft.setEnable(False)
        except:
            pass
        finally:
            self.sendDetach(alloc_id, tuner_num,  data_port=False, fft_port=True)
            self.MSDD.save_fft_channel(fft_channel)

    def _allocate_frontend_tuner_allocation(self, frontend_tuner_allocation, scanner_props=None ):
        self._log.trace( "Received a tuner allocation, contents:"+str(frontend_tuner_allocation))

        if self._usageState == CF.Device.BUSY:
            raise CF.Device.InvalidState("Cannot perform allocation, device is busy")

        # Check allocation_id
        if not frontend_tuner_allocation.allocation_id:
            self._log.info("allocate_frontend_tuner_allocation: MISSING ALLOCATION_ID")
            raise CF.Device.InvalidCapacity("MISSING ALLOCATION_ID", properties.struct_to_props(frontend_tuner_allocation))

        # Check if allocation ID has already been used
        if  self.getTunerMapping(frontend_tuner_allocation.allocation_id) >= 0:
            self._log.info("allocate_frontend_tuner_allocation: ALLOCATION_ID "+frontend_tuner_allocation.allocation_id+" ALREADY IN USE")
            raise CF.Device.InvalidCapacity("ALLOCATION_ID "+frontend_tuner_allocation.allocation_id+" ALREADY IN USE", properties.struct_to_props(frontend_tuner_allocation))

        #check allocationID is valid
        if frontend_tuner_allocation.allocation_id in ("",None):
            self._log.warn("Allocation ID cannot be blank")
            raise CF.Device.InvalidCapacity("Allocation ID cannot be blank",properties.struct_to_props(value))

        self._log.debug("Tuner allocation, request: " + str(frontend_tuner_allocation))

        if frontend_tuner_allocation.tuner_type == self.FE_TYPE_DDC:
            reject_allocation = True
            for tn in self.dig_tuner_base_nums:
                reject_allocation = reject_allocation and (self.frontend_tuner_status[tn].allocated and self.frontend_tuner_status[tn].tuner_type == self.FE_TYPE_RXDIG and len(self.frontend_tuner_status[tn].tuner_types) == 1)
            if reject_allocation:
                self._log.debug("Allocation Failed. Asked for a DDC and all tuners were RX_DIGITIZERS and already allocated")
                return False

        self.allocation_id_mapping_lock.acquire()
        emsg=None
        try:
            if len(self.tuner_allocation_ids) != len(self.frontend_tuner_status):
                for idx in range(len(self.frontend_tuner_status)-len(self.tuner_allocation_ids)):
                    self.tuner_allocation_ids.append(tuner_allocation_ids_struct())

            for tuner_num in range(len(self.tuner_allocation_ids)):
                self._log.debug("--- Attempting allocation on Tuner: " + str(tuner_num) \
                                + " Type:" + str(self.frontend_tuner_status[tuner_num].tuner_type)+ \
                                " with request: " + str(frontend_tuner_allocation))

                ########## BASIC CHECKS FOR TUNER TYPE, RF FLOW ID AND GROUP ID ##########
                self._log.trace( " tuner types " + str(self.frontend_tuner_status[tuner_num].tuner_types) \
                                 + " default tuner type " + str(self.frontend_tuner_status[tuner_num].tuner_type) \
                                 + " request type " + str( frontend_tuner_allocation.tuner_type))
                if self.frontend_tuner_status[tuner_num].tuner_types.count(frontend_tuner_allocation.tuner_type) <= 0:
                    self._log.debug(' Skipping tuner number ' + str(tuner_num) + ' tuner type mismatch')
                    continue
                if len(frontend_tuner_allocation.rf_flow_id) != 0 and  frontend_tuner_allocation.rf_flow_id != self.device_rf_flow:
                    self._log.debug(' Skipping tuner number ' + str(tuner_num) + ' rf_flow_id required but id mismatch')
                    continue
                if frontend_tuner_allocation.group_id != self.frontend_group_id:
                    self._log.debug(' Skipping tuner number ' + str(tuner_num) + ' group_id required but id mismatch')
                    continue

                # Check that RF will fit within RFInfo Setting
                if self.device_rf_info_pkt:
                    try:
                        validateRequestVsRFInfo(frontend_tuner_allocation,self.device_rf_info_pkt,1)
                    except FRONTEND.BadParameterException , e:
                        self._log.info("ValidateRequestVsRFInfo Failed: %s" %(str(e)))
                        continue


                # Calculate IF Offset if there is one
                if_freq = self.convert_rf_to_if(frontend_tuner_allocation.center_frequency)

                ########## LISTENER ALLOCATION - IE - DEVICE CONTROL IS FALSE ##########
                if not frontend_tuner_allocation.device_control:
                    if not self.frontend_tuner_status[tuner_num].allocated:
                        self._log.debug(' Listener allocation failed because tuner was not already allocated')
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
                        self._log.debug(" Listener Allocation failed, parameters don't match")
                        continue

                    # Make a Listener allocation for this tuner with the requested allocation ID
                    listenerallocation = frontend.fe_types.frontend_listener_allocation()
                    listenerallocation.existing_allocation_id = self.frontend_tuner_status[tuner_num].allocation_id_control
                    listenerallocation.listener_allocation_id = frontend_tuner_allocation.allocation_id
                    retval=False
                    try:
                        retval =  self._allocate_frontend_listener_allocation(listenerallocation)
                    except:
                        self._log.exception("Could not allocate listener" + str(frontend_tuner_allocation))

                    return retval


                ########## CONTROLLER ALLOCATION - IE - DEVICE CONTROL IS TRUE ##########
                if self.frontend_tuner_status[tuner_num].allocated:
                    self._log.debug(" Control allocation failed because given tuner was already allocated")
                    continue

                ### MAKE SURE PARENT IS ALLCOATED
                parent_tuner = self.MSDD.get_parent_tuner_num(self.frontend_tuner_status[tuner_num].rx_object)
                if parent_tuner != None:
                    if not self.frontend_tuner_status[parent_tuner].allocated:
                        emsg="Missing required allocation dependency on parent tuner"
                        if frontend_tuner_allocation.tuner_type=='DDC':
                            emsg="Missing allocation for CHANNELIZER"
                        self._log.debug("Control allocation failed on tuner " + str(tuner_num) + " because tuner's parent was not already allocated")
                        continue

                # Check if piggybacked channels are not allocated
                if self.checkSecondaryAllocations(tuner_num):
                    self._log.debug("Tuner " + str(tuner_num) + " has secondary allocations active, skipping this tuner.")
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

                        self._log.trace("allocate candidate tuner (before sr/bw check) " + str(tuner_num) +
                                    ", cf success " + str(success) +
                                    ", if freq " + str(if_freq) +
                                    ", freq " + str(valid_cf) +
                                    ", sample rate " + str(valid_sr) +
                                    ", bandwidth " + str(valid_bw) )

                        if frontend_tuner_allocation.sample_rate!=0 and frontend_tuner_allocation.bandwidth!=0:
                            #Specify a SR and BW
                            sr = max(frontend_tuner_allocation.bandwidth,frontend_tuner_allocation.sample_rate) #Ask for enough sr to satisfy the bw request
                            valid_bw = self.frontend_tuner_status[tuner_num].rx_object.get_valid_bandwidth(frontend_tuner_allocation.bandwidth, frontend_tuner_allocation.bandwidth_tolerance)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setBandwidth_Hz(valid_bw)
                            valid_sr = self.frontend_tuner_status[tuner_num].rx_object.get_valid_sample_rate(sr, frontend_tuner_allocation.sample_rate_tolerance)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setSampleRate(valid_sr)
                            self._log.trace( " SRATE and BW,  valid bw " + str(valid_bw) + " valid srate "+ str( valid_sr) + " success " + str(success))
                        elif frontend_tuner_allocation.sample_rate!=0 and frontend_tuner_allocation.bandwidth==0:
                            #Specify only SR
                            valid_sr = self.frontend_tuner_status[tuner_num].rx_object.get_valid_sample_rate(frontend_tuner_allocation.sample_rate, frontend_tuner_allocation.sample_rate_tolerance)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setSampleRate(valid_sr)
                            valid_bw =  self.frontend_tuner_status[tuner_num].rx_object.get_bandwidth_for_sample_rate(valid_sr)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setBandwidth_Hz(valid_bw)
                            self._log.trace( " SRATE only, valid bw " + str(valid_bw) + " valid srate "+ str( valid_sr) + " success " + str(success))
                        elif frontend_tuner_allocation.sample_rate==0 and frontend_tuner_allocation.bandwidth!=0:
                            #specify only BW so use it for requested SR with a large tolerance because sample rate is 'don't care'
                            valid_bw = self.frontend_tuner_status[tuner_num].rx_object.get_valid_bandwidth(frontend_tuner_allocation.bandwidth, frontend_tuner_allocation.bandwidth_tolerance)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setBandwidth_Hz(valid_bw)
                            valid_sr = self.frontend_tuner_status[tuner_num].rx_object.get_sample_rate_for_bandwidth(valid_bw)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setSampleRate(valid_sr)
                            self._log.trace( " BW only, valid bw " + str(valid_bw) + " valid srate "+ str( valid_sr) + " success " + str(success))
                        elif frontend_tuner_allocation.sample_rate==0 and frontend_tuner_allocation.bandwidth==0:
                            #Don't care for both sample_rate and # bandwidth so find any valid value and use it
                            valid_bw =  self.frontend_tuner_status[tuner_num].rx_object.get_valid_bandwidth(valid_bw)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setBandwidth_Hz(valid_bw)
                            valid_sr = self.frontend_tuner_status[tuner_num].rx_object.get_sample_rate_for_bandwidth(valid_bw)
                            success &= self.frontend_tuner_status[tuner_num].rx_object.setSampleRate(valid_sr)
                            self._log.trace( " Don't care, valid bw " + str(valid_bw) + " valid srate "+ str( valid_sr) + " success " + str(success))
                        else:
                            self._log.error("Allocation Failed, unknown reason, Shouldn't get here")
                            success = False

                        self._log.debug("allocate results, tuner " + str(tuner_num) + " success " + str(success)+
                                        ", if freq " + str(if_freq) +
                                        ", freq " + str(valid_cf) +
                                        ", sample rate " + str(valid_sr) +
                                        ", bandwidth " + str(valid_bw))

                    except (CommandException, InvalidValue), e:
                        if logging.NOTSET < self._log.level < logging.INFO:
                            traceback.print_exc()
                        success = False
                        msg="Allocation failed, Tuner %s exception <%s> allocation request %s" % (tuner_num, e, cfg_values)
                        self._log.debug(msg)
                        raise Exception(msg)

                    allocated_values="cf:%s bw:%s srate:%s " % (valid_cf, valid_bw, valid_sr)
                    if valid_cf == None or valid_bw == None or valid_sr == None or not success:
                        self._log.debug("Allocation failed, tuner %s allocation: %s validated: %s" % (tuner_num, cfg_values, allocated_values))
                        raise Exception("Tuner %s, Invalid allocation values, %s"% (tuner_num, cfg_values))

                    # check expected network output rate for device.
                    srate = self.frontend_tuner_status[tuner_num].rx_object.getSampleRate()
                    self._log.debug("Checking additional network utilization tuner: %s sample rate: %s " % (tuner_num, srate))
                    if self.tuner_output_configuration.has_key(tuner_num) and \
                       self.tuner_output_configuration[tuner_num].enabled:
                        if not self.checkReceiverNetworkOutput(additional_rate=srate,
                                                               out_module=self.frontend_tuner_status[tuner_num].rx_object.output_object):
                            raise Exception("Output rate would exceed network interface, tuner: " + str(tuner_num) + " adding sample rate " + str(srate) )

                    if self.tuner_output_configuration.has_key(tuner_num) and \
                       not self.tuner_output_configuration[tuner_num].enabled:
                        self._log.warn("Tuner is allocated but output is disabled, Tuner: {0}".format(tuner_num))

                    # check cpu load on device we might be too busy now..
                    self.frontend_tuner_status[tuner_num].rx_object.set_tuner_enable(True)
                    if not self.checkReceiverCpuLoad(self.advanced.max_cpu_load):
                        self.frontend_tuner_status[tuner_num].rx_object.set_tuner_enable(False)
                        raise Exception("Cannot perform allocation, device will become too busy")

                    # apply gain settings for the tuner
                    self.set_default_msdd_gain_value_for_tuner(tuner_num)

                    self._log.debug("Set RF freq offset " + str(valid_cf) + ", child modules "+",".join([ x.msdd_channel_id() for x in self.frontend_tuner_status[tuner_num].rx_object.rx_child_objects]))
                    self.frontend_tuner_status[tuner_num].rx_object.updateChildChannelOffsets(valid_cf)

                    self.frontend_tuner_status[tuner_num].tuner_type = frontend_tuner_allocation.tuner_type
                    self.frontend_tuner_status[tuner_num].allocated = True
                    self.frontend_tuner_status[tuner_num].allocation_id_control = frontend_tuner_allocation.allocation_id;
                    self.frontend_tuner_status[tuner_num].allocation_id_csv = self.create_allocation_id_csv(self.frontend_tuner_status[tuner_num].allocation_id_control,
                                                                                                            self.frontend_tuner_status[tuner_num].allocation_id_listeners)
                    self.deviceEnable( self.frontend_tuner_status[tuner_num], tuner_num)
                    self.frontend_tuner_status[tuner_num].rf_flow_id = self.device_rf_flow
                    self.matchAllocationIdToStreamId(frontend_tuner_allocation.allocation_id, frontend_tuner_allocation.allocation_id,"dataSDDS_out")
                    self.matchAllocationIdToStreamId(frontend_tuner_allocation.allocation_id, frontend_tuner_allocation.allocation_id,"dataSDDS_out_PSD")
                    self.matchAllocationIdToStreamId(frontend_tuner_allocation.allocation_id, frontend_tuner_allocation.allocation_id,"dataVITA49_out")
                    self.matchAllocationIdToStreamId(frontend_tuner_allocation.allocation_id, frontend_tuner_allocation.allocation_id,"dataVITA49_out_PSD")
                    self.tuner_allocation_ids[tuner_num].control_allocation_id = frontend_tuner_allocation.allocation_id
                    self.allocation_id_to_tuner_id[frontend_tuner_allocation.allocation_id] =  tuner_num

                    # send connection information to downstream consumers
                    self.sendAttach(self.frontend_tuner_status[tuner_num].allocation_id_control, tuner_num)

                    self._log.info("Allocation SUCCESS, Tuner: " + str(tuner_num)  \
                                   + " Type: " + str(self.frontend_tuner_status[tuner_num].tuner_type) \
                                   + " requested " + cfg_values + " allocated " + allocated_values)

                    if frontend_tuner_allocation.device_control:
                        self.enableFFT(self.frontend_tuner_status[tuner_num].allocation_id_control)

                    # update usage state
                    self._usageState = self.updateUsageState()

                    # Successfull return
                    return True

                except Exception,e:
                    if logging.NOTSET < self._log.level < logging.INFO:
                        traceback.print_exc()
                    self.frontend_tuner_status[tuner_num].rx_object.setEnable(False)
                    emsg = "Failed to allocate: " + str(e)
                    self._log.debug("--- Failed allocation, Reason " + emsg \
                                    + " Tuner: " + str(tuner_num) \
                                    + " Type:" + str(self.frontend_tuner_status[tuner_num].tuner_type) \
                                    + " Request: " + str(frontend_tuner_allocation))
                    continue
        except:
            traceback.print_exc()
            if logging.NOTSET < self._log.level < logging.INFO:
                traceback.print_exc()
        finally:
            self.allocation_id_mapping_lock.release()

        self._log.debug("Allocation Failed. Tried all available tuners and none of them have satisfied the request")
        if not emsg:
            emsg  = "No tuners available"
        self._log.warn("Failed allocation, Reason: " + emsg + ", Allocation request: " + str(frontend_tuner_allocation))

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
                self._log.trace("Tuner " + str(tuner_num) + " does not have digital output")
                return
            if not self.tuner_output_configuration.has_key(tuner_num):
                self._log.warn("Missing output configuration for tuner " + str(tuner_num) + " disabling output")
                return
            
            # RESOLVE - since output configuration is read only... maybe we should not do this..
            # apply output settings to tuner's output module
            success = _output_mod.setAddressPort(self.tuner_output_configuration[tuner_num].ip_address,str(self.tuner_output_configuration[tuner_num].port))
            self._log.trace("enable digital output, tuner " + str(tuner_num) +  " ip address " + str(success))
            proto = _output_mod.getOutputProtocolNumberFromString(self.tuner_output_configuration[tuner_num].protocol)
            success &= _output_mod.setOutputProtocol(self.tuner_output_configuration[tuner_num].protocol)
            self._log.trace("enable digital output tuner " + str(tuner_num) +  " protocol " + str(success))
            if self.tuner_output_configuration[tuner_num].protocol=="UDP_VITA49":
                success &= _output_mod.setCDR(self.VITA49CONTEXTPACKETINTERVAL)
                self._log.trace("enable digital output tuner " + str(tuner_num) +  " vita49 context packet internval " + str(success))
                success &= _output_mod.setCCR(1)
                self._log.trace("enable digital output tuner " + str(tuner_num) +  " vita49 context packet " + str(success))
            success &= _output_mod.setTimestampOff(self.tuner_output_configuration[tuner_num].timestamp_offset)
            self._log.trace("enable digital output tuner " + str(tuner_num) +  " time stamp " + str(success))
            success &= _output_mod.setEnableVlanTagging(self.tuner_output_configuration[tuner_num].vlan_enabled)
            self._log.trace("enable digital output tuner " + str(tuner_num) +  " vlan tag " + str(success))
            success &= _output_mod.setOutputEndianess(self.tuner_output_configuration[tuner_num].endianess)
            self._log.trace("enable digital output tuner " + str(tuner_num) +  " endianess " + str(success))
            success &= _output_mod.setVlanTci(max(0,self.tuner_output_configuration[tuner_num].vlan_tci))
            self._log.trace("enable digital output tuner " + str(tuner_num) +  " vlan tci " + str(success))
            _output_mod.setMFP(self.tuner_output_configuration[tuner_num].mfp_flush)

            self._log.debug("enableDigitalOutput, tuner " + str(tuner_num) + " output context: "
                            " protocol " + self.tuner_output_configuration[tuner_num].protocol +
                            " ip-addr/port/vlan " + self.tuner_output_configuration[tuner_num].ip_address + "/" +
                            str(self.tuner_output_configuration[tuner_num].port)+ "/" +
                            str(self.tuner_output_configuration[tuner_num].vlan_tci) )

            outcfg_enabled=self.tuner_output_configuration[tuner_num].enabled
            if outcfg_enabled:
                # relink output module to source if it was lost
                if self.MSDD.get_corresponding_output_module_object(_rx_object.digital_rx_object) == None:
                    self.MSDD.stream_router.linkModules(
                        _rx_object.digital_rx_object.object.full_reg_name,
                        _output_mod.full_reg_name)
                    self._log.debug("enableDigitalOutput, tuner " + str(tuner_num) +
                                    " relinking src: " + _rx_object.digital_rx_object.object.full_reg_name +
                                    " dest: " + _output_mod.full_reg_name)

                # if tuner is just a channelizer then don't enable output module...
                if "CHANNELIZER" == self.frontend_tuner_status[tuner_num]:
                    self._log.info("enableDigitalOutput, Disabling output for CHANNELIZER tuner" + str(tuner_num))
                    outcfg_enabled=False

            success&= _rx_object.set_output_enable(outcfg_enabled)
            self._log.debug("enableDigitalOutput, tuner " + str(tuner_num) +
                            " enabled_output: " + str(outcfg_enabled) + " return from set enable " + str(success))
            if not outcfg_enabled:
                self._log.warn( "Output configuration disables output for tuner " + str(tuner_num))

            if not success:
                self._log.error("Error when updating output configuration, Tuner: "+ str(tuner_num) + " with configuration: " + str(self.tuner_output_configuration[tuner_num]))

            self._log.debug("finished tuner output configuration, tuner "+ str(tuner_num) + " output enable state: " +str(outcfg_enabled))

        except:
            if logging.NOTSET < self._log.level < logging.INFO:
                traceback.print_exc()
            _rx_object.set_output_enable(False)
            self._log.error("Error when updating output configuration for Tuner: "+ str(tuner_num))


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
        self._log.debug("deviceEnable tuner_id " +str(tuner_id))
        enable=False
        try:
            # enable output port for the tuner
            if self.tuner_output_configuration.has_key(tuner_id):
                self.enableDigitalOutput(tuner_id)

            # enable tuner
            self._log.debug("deviceEnable tuner " + str(tuner_id) + " enabling digital tuner's digital receiver module")
            self.frontend_tuner_status[tuner_id].rx_object.set_tuner_enable(True)
            enable=True

            self._log.debug("deviceEnable,  tuner " + str(tuner_id) + " msdd digital module enable: " +
                            str(self.frontend_tuner_status[tuner_id].rx_object.digital_rx_object.object.enable) +
                            " msdd output module " + str(self.frontend_tuner_status[tuner_id].rx_object.output_object.object.enable))
        except:
            self._log.exception("Exception enabling tuner " + str(tuner_id))
            self.frontend_tuner_status[tuner_id].rx_object.setEnable(False)
            if self.frontend_tuner_status[tuner_id].rx_object.hasFFTChannel():
                self.disableFFT(self.frontend_tuner_status[tuner_id].allocation_id_control)

        fts.enabled=enable
        self._update_tuner_status([tuner_id])
        return

    def deviceDisable(self,fts, tuner_id):
        '''
        ************************************************************
        modify fts, which corresponds to self.frontend_tuner_status[tuner_id]
        Make sure to reset the 'enabled' member of fts to indicate that tuner as disabled
        ************************************************************'''
        restatus_tuners=[tuner_id]
        try:
            # Check if tuner_id has secondary allocations associated with it
            # if so, only turn of output and leave hardware nbddc tuner enabled
            self._log.debug("deviceDisable,  disable tuner/output for tuner " + str(tuner_id) )
            if not self.checkSecondaryAllocations(tuner_id):
                self.frontend_tuner_status[tuner_id].rx_object.setEnable(False)
            else:
                self._log.debug("Tuner " + str(tuner_id) + " has secondary allocations active, do not disable nbddc ")
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
                    self._log.debug("Tuner " + str(tuner_id) + " Parent has no secondary allocations so disabling ")
                    parent_rx_channel.setEnable(False)
                    restatus_tuners.append(parent_idx)

        except:
            self._log.exception("deviceDisable, disable tuner exception for tuner " + str(tuner_id))

        fts.enabled=False
        self._update_tuner_status(restatus_tuners)

    def deviceDeleteTuning(self,fts,tuner_id):

        try:
            control_aid = self.getControlAllocationId(tuner_id)
            if self.frontend_tuner_status[tuner_id].rx_object.hasFFTChannel():
                self.disableFFT(control_aid)
        except:
            self._log.error("Error disabling FFT output for tuner " + str(tuner_id) + " allocation " + (control_aid))

        self.frontend_tuner_status[tuner_id].internal_allocation_parent = None
        self.frontend_tuner_status[tuner_id].internal_allocation_children = []
        self.frontend_tuner_status[tuner_id].internal_allocation = False
        self.frontend_tuner_status[tuner_id].allocation_id_control = ""
        self.frontend_tuner_status[tuner_id].allocated = False
        self._log.info("Deallocated allocation id: " + control_aid + " Tuner: " + str(tuner_id) + " Type: " + str(fts.tuner_type) )


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
        self._log.debug("Checking Freq against RFInfo Packet. Requested RF: %s minFreq: %s maxFreq: %s" %(rf_freq,minFreq,maxFreq)) 
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

        self._log.debug("Converted RF Freq of %s to IF Freq %s based on Input RF of %s, IF of %s, and spectral inversion %s" %(rf_freq,if_freq,self.device_rf_info_pkt.rf_center_freq,self.device_rf_info_pkt.if_center_freq,self.device_rf_info_pkt.spectrum_inverted))
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

        self._log.debug("Converted IF Freq of %s to RF Freq %s based on Input RF of %s, IF of %s, and spectral inversion %s" %(if_freq,rf_freq,self.device_rf_info_pkt.rf_center_freq,self.device_rf_info_pkt.if_center_freq,self.device_rf_info_pkt.spectrum_inverted))
        return rf_freq     

    '''
    *************************************************************
    Functions servicing the tuner control port
    *************************************************************'''
    def getTunerType(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid allocation id")
        return self.frontend_tuner_status[idx].tuner_type

    def getTunerDeviceControl(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid allocation id")
        if self.getControlAllocationId(idx) == allocation_id:
            return True
        return False

    def getTunerGroupId(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid allocation id")
        return self.frontend_tuner_status[idx].group_id

    def getTunerRfFlowId(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid allocation id")
        return self.frontend_tuner_status[idx].rf_flow_id

    def setTunerCenterFrequency(self,allocation_id, freq):
        tuner_num = self.getTunerMapping(allocation_id)
        if tuner_num < 0: raise FRONTEND.FrontendException("Invalid allocation id")
        if allocation_id != self.getControlAllocationId(tuner_num):
            raise FRONTEND.FrontendException(("ID "+str(allocation_id)+" does not have authorization to modify the tuner"))
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
                self._log.debug("setTunerCenterFrequency tuner " + str(tuner_num) + " requested " + str(freq) + " new freq " + str(valid_cf) )
                self.frontend_tuner_status[tuner_num].rx_object.setFrequency_Hz(if_freq)
            else:
                raise Exception("Invalid center frequency")

            try:
                self.frontend_tuner_status[tuner_num].rx_object._debug=True
                for child in self.frontend_tuner_status[tuner_num].rx_object.getChildChannels():
                    child_tuner_id=None
                    if self.rx_channel_tuner_status.has_key(child):
                        child_tuner_id=self.rx_channel_tuner_status[child]
                        if self.frontend_tuner_status[child_tuner_id].allocated:
                            self._log.debug("setTunerCenterFrequency update center frequency msdd channel " + child.msdd_channel_id() + " frequency " + str(valid_cf))
                            if child.centerFrequencyRetune(valid_cf, keepOffset=True):
                                changed_child_numbers.append(child_tuner_id)
                        else:
                            child.updateRFFrequencyOffset(valid_cf)
                self.frontend_tuner_status[tuner_num].rx_object._debug=False
            except Exception, e:
                if logging.NOTSET < self._log.level < logging.INFO:
                    traceback.print_exc()
                self._log.warn("PROBLEM: %s"%e)
        except:
            if logging.NOTSET < self._log.level < logging.INFO:
                traceback.print_exc()
            error_string = "Error setting center frequency to " + str(freq) + " for tuner " + str(tuner_num)
            self._log.exception(error_string)
            raise FRONTEND.BadParameterException(error_string)
        finally:
            self.allocation_id_mapping_lock.release()
            self._log.debug("setTunerCenterFrequency update status for " + ",".join([ str(x) for x in [tuner_num]+changed_child_numbers]))
            self.update_tuner_status([tuner_num]+changed_child_numbers)

    def getTunerCenterFrequency(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid allocation id")
        return self.frontend_tuner_status[idx].center_frequency

    def setTunerBandwidth(self,allocation_id, bw):
        tuner_num = self.getTunerMapping(allocation_id)
        if tuner_num < 0: raise FRONTEND.FrontendException("Invalid allocation id")
        if allocation_id != self.getControlAllocationId(tuner_num):
            raise FRONTEND.FrontendException(("ID "+str(allocation_id)+" does not have authorization to modify the tuner"))
        if bw<0: raise FRONTEND.BadParameterException()
        
        try:
            self.allocation_id_mapping_lock.acquire()
            self.frontend_tuner_status[tuner_num].rx_object.validate_bandwidth(bw)
            self._log.debug("setting bandwidth to " + str(bw) + " for tuner " + str(tuner_num))
            self.frontend_tuner_status[tuner_num].rx_object.setBandwidth_Hz(bw)
        except Exception, e:
            error_string = "Error setting tuner bandwidth to " + str(bw) + " for tuner " + str(tuner_num) + ", reason: " + str(e)
            self._log.exception(error_string)
            raise FRONTEND.BadParameterException(error_string)
        finally:
            self.allocation_id_mapping_lock.release()
        self.update_tuner_status([tuner_num])

    def getTunerBandwidth(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid allocation id")
        return self.frontend_tuner_status[idx].bandwidth

    def setTunerAgcEnable(self,allocation_id, enable):
        raise FRONTEND.NotSupportedException("setTunerAgcEnable not supported")

    def getTunerAgcEnable(self,allocation_id):
        raise FRONTEND.NotSupportedException("getTunerAgcEnable not supported")

    def setTunerGain(self,allocation_id, gain):
        tuner_num = self.getTunerMapping(allocation_id)
        if tuner_num < 0: raise FRONTEND.FrontendException("Invalid allocation id")
        if allocation_id != self.getControlAllocationId(tuner_num):
            raise FRONTEND.FrontendException(("ID "+str(allocation_id)+" does not have authorization to modify the tuner"))
    
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
        except:
            error_string = "Error setting gain to " + str(gain) + " for tuner " + str(tuner_num)
            self._log.exception(error_string)
            raise FRONTEND.BadParameterException(error_string)
        finally:
            self.allocation_id_mapping_lock.release()
        self.update_tuner_status([tuner_num])

    def getTunerGain(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid allocation id")
        return self.frontend_tuner_status[idx].gain

    def setTunerReferenceSource(self,allocation_id, source):
        raise FRONTEND.NotSupportedException("Can not change 10MHz reference")

    def getTunerReferenceSource(self,allocation_id):
        raise FRONTEND.NotSupportedException("getTunerReferenceSource not supported")

    def setTunerEnable(self,allocation_id, enable):
        tuner_num = self.getTunerMapping(allocation_id)
        if tuner_num < 0: raise FRONTEND.FrontendException("Invalid allocation id")
        if allocation_id != self.getControlAllocationId(tuner_num):
            raise FRONTEND.FrontendException(("ID "+str(allocation_id)+" does not have authorization to modify the tuner"))
    
        try:
            self.frontend_tuner_status[tuner_num].enabled=enable
            self.frontend_tuner_status[tuner_num].rx_object.setEnable(enable)
        except:
            self._log.exception("Exception on Tuner Enable")
        self.update_tuner_status([tuner_num])

    def getTunerEnable(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid allocation id")
        return self.frontend_tuner_status[idx].enabled


    def setTunerOutputSampleRate(self,allocation_id, sr):
        tuner_num = self.getTunerMapping(allocation_id)
        if tuner_num < 0: raise FRONTEND.FrontendException("Invalid allocation id")
        if allocation_id != self.getControlAllocationId(tuner_num):
            raise FRONTEND.FrontendException(("ID "+str(allocation_id)+" does not have authorization to modify the tuner"))
        if sr<0: raise FRONTEND.BadParameterException()
        try:
            self.allocation_id_mapping_lock.acquire()
            self.frontend_tuner_status[tuner_num].rx_object.validate_sample_rate(sr)
            self._log.debug("setting sample rate to " + str(sr) + " for tuner " + str(tuner_num))
            self.frontend_tuner_status[tuner_num].rx_object.setSampleRate(sr)
        except Exception, e:
            error_string = "Error setting tuner sample rate to " + str(sr) + " for tuner " + str(tuner_num) + ", reason: " + str(e)
            self._log.exception(str(e))
            raise FRONTEND.BadParameterException(error_string)
        finally:
            self.allocation_id_mapping_lock.release()
        self.update_tuner_status([tuner_num])

    def getTunerOutputSampleRate(self,allocation_id):
        idx = self.getTunerMapping(allocation_id)
        if idx < 0: raise FRONTEND.FrontendException("Invalid allocation id")
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

