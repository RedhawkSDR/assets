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
#
# AUTO-GENERATED
#
# Source: MSDD.spd.xml
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
from msddcontroller import MSDDRadio
from msddcontroller import create_csv
from datetime import datetime, time, timedelta
from time import sleep

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


class MSDD_i(MSDD_base):
    FE_TYPE_RECEIVER="RX"
    FE_TYPE_RXDIG="RX_DIGITIZER"
    FE_TYPE_RXDIGCHAN="RX_DIGITIZER_CHANNELIZER"
    FE_TYPE_DDC="DDC"
    FE_TYPE_PSD="PSD"
    FE_TYPE_SPC="RX_DIGITIZER_SPECTRALSCANNER"
    FE_DDC_BW=int(1)
    FE_RXDIGCHAN_BW=int(2)
    FE_RXDIG_BW=int(4)
    FE_RX_BW=int(8)
    FE_PSD_BW=int(16)
    FE_SPC_BW=int(32)
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
        self.disconnect_from_msdd()
        self.pending_fft_registrations = []
        self.pending_spc_registrations = []
        self.device_rf_flow = ""
        self.device_rf_info_pkt=None
        self.dig_tuner_base_nums= []
        
        self.addPropertyChangeListener("msdd_configuration",self.msdd_configuration_changed)
        self.addPropertyChangeListener("msdd_gain_configuration",self.msdd_gain_configuration_changed)
        self.addPropertyChangeListener("clock_ref",self.clock_ref_changed)
        self.addPropertyChangeListener("advanced",self.advanced_changed)
        self.addPropertyChangeListener("msdd_output_configuration",self.msdd_output_configuration_changed)
        self.addPropertyChangeListener("msdd_block_output_configuration",self.msdd_block_output_configuration_changed)
        self.addPropertyChangeListener("msdd_psd_configuration",self.msdd_psd_configuration_changed)
        self.addPropertyChangeListener("msdd_spc_configuration",self.msdd_spc_configuration_changed)
        self.addPropertyChangeListener("msdd_time_of_day_configuration",self.msdd_time_of_day_configuration_changed)
        self.addPropertyChangeListener("msdd_advanced_debugging_tools",self.msdd_advanced_debugging_tools_changed)
        
        self.msdd_configuration_changed("msdd_configuration",self.msdd_configuration,self.msdd_configuration)
        self.msdd_gain_configuration_changed("msdd_gain_configuration",self.msdd_gain_configuration, self.msdd_gain_configuration)
        self.clock_ref_changed("clock_ref",self.clock_ref, self.clock_ref)
        self.advanced_changed("advanced",self.advanced,self.advanced)
        self.msdd_output_configuration_changed("msdd_output_configuration",self.msdd_output_configuration,self.msdd_output_configuration)
        self.msdd_block_output_configuration_changed("msdd_block_output_configuration",self.msdd_block_output_configuration,self.msdd_block_output_configuration)
        self.msdd_psd_configuration_changed("msdd_psd_configuration",self.msdd_psd_configuration,self.msdd_psd_configuration)
        self.msdd_spc_configuration_changed("msdd_spc_configuration",self.msdd_spc_configuration,self.msdd_spc_configuration)
        self.msdd_time_of_day_configuration_changed("msdd_time_of_day_configuration",self.msdd_time_of_day_configuration,self.msdd_time_of_day_configuration)
        self.msdd_advanced_debugging_tools_changed("msdd_advanced_debugging_tools",self.msdd_advanced_debugging_tools,self.msdd_advanced_debugging_tools )        
        
        self.tuner_allocation_ids = []
        
        # The device creates the tuner status structure dynamically based on the device it is controlling so we are not using the setNumChannels
        #self.setNumChannels(1, "RX_DIGITIZER");
        

    def getPort(self, name):
        if name == "AnalogTuner_in":
            self._log.trace("getPort() could not find port %s", name)
            raise CF.PortSupplier.UnknownPort()
        return MSDD_base.getPort(self, name)


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
        self.update_output_configuration()
        
        
    
    def connect_to_msdd(self):
        if self.MSDD != None:
            valid_ip = False
            for nw_module in self.MSDD.network_modules:
                if  self.msdd_configuration.msdd_ip_address == nw_module.object.ip_addr: 
                    valid_ip = True
                    break
            if not valid_ip:
                self.disconnect_from_msdd()
        if self.MSDD == None:
            try:
                self.MSDD = MSDDRadio(self.msdd_configuration.msdd_ip_address, self.msdd_configuration.msdd_port, self.advanced.udp_timeout)
            
                for net_mods in self.MSDD.network_modules:
                    connected_rate = net_mods.object.getNwBitRate()
                    if connected_rate < self.advanced.minimum_connected_nic_rate:
                        self._log.error("MSDD CONNECTED AT THE RATE OF " + str(connected_rate) + " Mbps WHICH DOES NOT MEET THE MINIMUM RATE OF" + str(self.advanced.minimum_connected_nic_rate) + " Mbps")
                        raise Exception("")
                    
                self.update_msdd_status()
                self.update_tuner_status()
                self.update_output_configuration()
                self.clock_ref_changed("clock_ref",self.clock_ref, self.clock_ref)
                self.msdd_gain_configuration_changed("msdd_gain_configuration",self.msdd_gain_configuration, self.msdd_gain_configuration)
            except:
                self._log.exception("UNABLE TO CONNECT TO THE MSDD WITH IP:PORT =  " + str(self.msdd_configuration.msdd_ip_address) + ":" + str(self.msdd_configuration.msdd_port))
                self.disconnect_from_msdd()
                return False
            
        return True

    def update_msdd_status(self):
        if self.MSDD == None:
            self.msdd_status=MSDD_base.msdd_status_struct() #Reset to defaults
            return True
        try:
            self.msdd_status.connected = True
            self.msdd_status.ip_address = str(self.msdd_configuration.msdd_ip_address)
            self.msdd_status.port = str(self.msdd_configuration.msdd_port)
            self.msdd_status.model = str(self.MSDD.console_module.model)
            self.msdd_status.serial = str(self.MSDD.console_module.serial)
            self.msdd_status.software_part_number = str(self.MSDD.console_module.sw_part_number)
            self.msdd_status.rf_board_type = str(self.MSDD.console_module.rf_board_type)
            self.msdd_status.fpga_type = str(self.MSDD.console_module.fpga_type)
            self.msdd_status.dsp_type = str(self.MSDD.console_module.dsp_type)
            self.msdd_status.minimum_frequency_hz = str(self.MSDD.console_module.minFreq_Hz)
            self.msdd_status.maximum_frequency_hz = str(self.MSDD.console_module.maxFreq_Hz)
            self.msdd_status.dsp_reference_frequency_hz = str(self.MSDD.console_module.dspRef_Hz)
            self.msdd_status.adc_clock_frequency_hz = str(self.MSDD.console_module.adcClk_Hz)
            self.msdd_status.num_if_ports = str(self.MSDD.console_module.num_if_ports)
            self.msdd_status.num_eth_ports = str(self.MSDD.console_module.num_eth_ports)
            self.msdd_status.cpu_type = str(self.MSDD.console_module.cpu_type)
            self.msdd_status.cpu_rate = str(self.MSDD.console_module.cpu_rate)
            self.msdd_status.cpu_load = str(self.MSDD.console_module.cpu_load)
            self.msdd_status.pps_termination = str(self.MSDD.console_module.pps_termination)
            self.msdd_status.pps_voltage = str(self.MSDD.console_module.pps_voltage)
            self.msdd_status.number_wb_ddc_channels = str(self.MSDD.console_module.num_wb_channels)
            self.msdd_status.number_nb_ddc_channels = str(self.MSDD.console_module.num_nb_channels)
            self.msdd_status.filename_app = str(self.MSDD.console_module.filename_app)
            self.msdd_status.filename_fpga = str(self.MSDD.console_module.filename_fpga)
            self.msdd_status.filename_batch = str(self.MSDD.console_module.filename_batch)
            self.msdd_status.filename_boot = str(self.MSDD.console_module.filename_boot)
            self.msdd_status.filename_loader = str(self.MSDD.console_module.filename_loader)
            self.msdd_status.filename_config = str(self.MSDD.console_module.filename_config)
            self.msdd_status.filename_cal = str(self.MSDD.console_module.filename_cal)
            if len(self.MSDD.tod_modules) > 0:
                self.msdd_status.tod_module = str(self.MSDD.tod_modules[0].object.mode_string)
                self.msdd_status.tod_available_module = str(self.MSDD.tod_modules[0].object.available_modes)
                self.msdd_status.tod_meter_list = str(self.MSDD.tod_modules[0].object.meter_list_string)
                self.msdd_status.tod_tod_reference_adjust = str(self.MSDD.tod_modules[0].object.ref_adjust)
                self.msdd_status.tod_track_mode_state = str(self.MSDD.tod_modules[0].object.ref_track)
                self.msdd_status.tod_bit_state = str(self.MSDD.tod_modules[0].object.BIT_string)
                self.msdd_status.tod_toy = str(self.getter_with_default(self.MSDD.tod_modules[0].object.getToy,""))
        except:
            self._log.error("ERROR WHEN UPDATING MSDD_STATUS STRUCTURE")
            return False
    
        return True
    def getTunerTypeList(self,type_mode):
            tuner_lst = []
            if (type_mode & self.FE_RX_BW) == self.FE_RX_BW:
                tuner_lst.append(self.FE_TYPE_RECEIVER)
            if (type_mode & self.FE_PSD_BW) == self.FE_PSD_BW:
                tuner_lst.append(self.FE_TYPE_PSD)
            if (type_mode & self.FE_SPC_BW) == self.FE_SPC_BW:
                tuner_lst.append(self.FE_TYPE_SPC)
            if (type_mode & self.FE_DDC_BW) == self.FE_DDC_BW:
                tuner_lst.append(self.FE_TYPE_DDC)
            if (type_mode & self.FE_RXDIG_BW) == self.FE_RXDIG_BW:
                tuner_lst.append(self.FE_TYPE_RXDIG)
            if (type_mode & self.FE_RXDIGCHAN_BW) == self.FE_RXDIGCHAN_BW:
                tuner_lst.append(self.FE_TYPE_RXDIGCHAN)
            return tuner_lst

        
    
    def update_tuner_status(self, tuner_range=[]):
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
                    tuner_types = self.getTunerTypeList(self.advanced.psd_mode)
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
                    tuner_types = self.getTunerTypeList(self.advanced.spc_mode)
                else:
                    self._log.error("UNKOWN TUNER TYPE REPORTED BY THE MSDD")
                
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
                self.frontend_tuner_status.append(tuner_struct)
                

        #Update
        if len(tuner_range) <= 0:
            tuner_range = range(0,len(self.frontend_tuner_status))
        for tuner_num in tuner_range:            
            try:
                data_port = True
                fft_port = False
                spc_port = False         
                #for just analog tuners  
                if self.frontend_tuner_status[tuner_num].rx_object.is_analog() and not self.frontend_tuner_status[tuner_num].rx_object.is_digital():
                    self.frontend_tuner_status[tuner_num].center_frequency = self.convert_if_to_rf(self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.frequency_hz)
                    self.frontend_tuner_status[tuner_num].available_frequency = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.available_frequency_hz
                    self.frontend_tuner_status[tuner_num].gain = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.gain;
                    self.frontend_tuner_status[tuner_num].available_gain = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.available_gain
                    self.frontend_tuner_status[tuner_num].attenuation = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.attenuation;
                    self.frontend_tuner_status[tuner_num].available_attenuation = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.available_attenuation
                    self.frontend_tuner_status[tuner_num].adc_meter_values = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.adc_meter_str;
                    self.frontend_tuner_status[tuner_num].bandwidth = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.bandwidth_hz
                    self.frontend_tuner_status[tuner_num].available_bandwidth = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.available_bandwidth_hz
                    self.frontend_tuner_status[tuner_num].rcvr_gain = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.gain      
                #for just digital tuners             
                elif self.frontend_tuner_status[tuner_num].rx_object.is_digital() and not self.frontend_tuner_status[tuner_num].rx_object.is_analog():
                    self.frontend_tuner_status[tuner_num].enabled = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.enable;
                    self.frontend_tuner_status[tuner_num].decimation = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.decimation
                    self.frontend_tuner_status[tuner_num].available_decimation = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.available_decimation
                    self.frontend_tuner_status[tuner_num].attenuation = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.attenuation
                    self.frontend_tuner_status[tuner_num].available_attenuation = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.available_attenuation
                    self.frontend_tuner_status[tuner_num].gain = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.gain;
                    self.frontend_tuner_status[tuner_num].available_gain = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.available_gain
                    self.frontend_tuner_status[tuner_num].input_sample_rate = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.input_sample_rate;
                    self.frontend_tuner_status[tuner_num].sample_rate = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.sample_rate
                    self.frontend_tuner_status[tuner_num].available_sample_rate = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.available_sample_rate
                    self.frontend_tuner_status[tuner_num].bandwidth = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.bandwidth_hz
                    self.frontend_tuner_status[tuner_num].available_bandwidth = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.available_bandwidth_hz
                    self.frontend_tuner_status[tuner_num].center_frequency = self.convert_if_to_rf(self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.frequency_hz)
                    self.frontend_tuner_status[tuner_num].available_frequency = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.available_frequency_hz       
                    self.frontend_tuner_status[tuner_num].ddc_gain = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.gain
                    self.frontend_tuner_status[tuner_num].complex = True
                #if the tuner is both analog and digital (usually rx_digitizer mode)
                elif self.frontend_tuner_status[tuner_num].rx_object.is_digital() and  self.frontend_tuner_status[tuner_num].rx_object.is_analog():

                    self.frontend_tuner_status[tuner_num].enabled = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.enable;
                    self.frontend_tuner_status[tuner_num].decimation = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.decimation
                    self.frontend_tuner_status[tuner_num].available_decimation = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.available_decimation
                    self.frontend_tuner_status[tuner_num].attenuation = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.attenuation
                    self.frontend_tuner_status[tuner_num].available_attenuation = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.available_attenuation
                    self.frontend_tuner_status[tuner_num].gain = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.gain;
                    self.frontend_tuner_status[tuner_num].available_gain = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.available_gain
                    self.frontend_tuner_status[tuner_num].input_sample_rate = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.input_sample_rate;
                    self.frontend_tuner_status[tuner_num].sample_rate = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.sample_rate
                    self.frontend_tuner_status[tuner_num].available_sample_rate = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.available_sample_rate
                    self.frontend_tuner_status[tuner_num].bandwidth = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.bandwidth_hz
                    self.frontend_tuner_status[tuner_num].available_bandwidth = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.available_bandwidth_hz
                    self.frontend_tuner_status[tuner_num].center_frequency = self.convert_if_to_rf(self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.frequency_hz)
                    self.frontend_tuner_status[tuner_num].available_frequency = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.available_frequency_hz       
                    self.frontend_tuner_status[tuner_num].ddc_gain = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.gain
                    self.frontend_tuner_status[tuner_num].complex = True
                #if the tuner has streaming output
                if self.frontend_tuner_status[tuner_num].rx_object.has_streaming_output():
                    self.frontend_tuner_status[tuner_num].output_format = "CI"
                    self.frontend_tuner_status[tuner_num].output_multicast = str(self.frontend_tuner_status[tuner_num].rx_object.output_object.object.ip_addr)
                    self.frontend_tuner_status[tuner_num].output_port = str(self.frontend_tuner_status[tuner_num].rx_object.output_object.object.ip_port)
                    self.frontend_tuner_status[tuner_num].output_enabled = self.frontend_tuner_status[tuner_num].rx_object.output_object.object.enabled
                    self.frontend_tuner_status[tuner_num].output_protocol = str(self.frontend_tuner_status[tuner_num].rx_object.output_object.object.protocol)
                    self.frontend_tuner_status[tuner_num].output_vlan_enabled = int(self.frontend_tuner_status[tuner_num].rx_object.output_object.object.vlan_tagging_enabled) == 1
                    self.frontend_tuner_status[tuner_num].output_vlan_tci = str(self.frontend_tuner_status[tuner_num].rx_object.output_object.object.vlan_tci)
                    self.frontend_tuner_status[tuner_num].output_vlan = int(self.frontend_tuner_status[tuner_num].output_vlan_tci) & 0xFFF
                    self.frontend_tuner_status[tuner_num].output_flow = self.MSDD.get_stream_flow(self.frontend_tuner_status[tuner_num].rx_object.output_object)
                    self.frontend_tuner_status[tuner_num].output_timestamp_offset = str(self.frontend_tuner_status[tuner_num].rx_object.output_object.object.timestamp_offset_ns)
                    self.frontend_tuner_status[tuner_num].output_channel = str(self.frontend_tuner_status[tuner_num].rx_object.output_object.object.channel_number)
                    self.frontend_tuner_status[tuner_num].output_endianess = self.frontend_tuner_status[tuner_num].rx_object.output_object.object.endianess
                    self.frontend_tuner_status[tuner_num].output_mfp_flush = self.getter_with_default(self.frontend_tuner_status[tuner_num].rx_object.output_object.object.getMFP,-1)
                if self.frontend_tuner_status[tuner_num].rx_object.has_fft_object():  
                    data_port = False
                    fft_port = True
                    spc_port = False
                    self.frontend_tuner_status[tuner_num].enabled = self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.enable
                    self.frontend_tuner_status[tuner_num].psd_fft_size = self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.fftSize
                    self.frontend_tuner_status[tuner_num].psd_averages = self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.num_averages
                    self.frontend_tuner_status[tuner_num].psd_time_between_ffts = self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.time_between_fft_ms
                    self.frontend_tuner_status[tuner_num].psd_output_bin_size = self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.outputBins
                    self.frontend_tuner_status[tuner_num].psd_window_type = self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.window_type_str
                    self.frontend_tuner_status[tuner_num].psd_peak_mode = self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.peak_mode_str
                if self.frontend_tuner_status[tuner_num].rx_object.is_spectral_scan():  
                    data_port = False
                    fft_port = False
                    spc_port = True
                    self.frontend_tuner_status[tuner_num].enabled = self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.enable
                    self.frontend_tuner_status[tuner_num].spc_fft_size = self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.fftSize
                    self.frontend_tuner_status[tuner_num].spc_averages = self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.num_averages
                    self.frontend_tuner_status[tuner_num].spc_time_between_ffts = self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.time_between_fft_ms
                    self.frontend_tuner_status[tuner_num].spc_output_bin_size = self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.outputBins
                    self.frontend_tuner_status[tuner_num].spc_window_type = self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.window_type_str
                    self.frontend_tuner_status[tuner_num].spc_peak_mode = self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.peak_mode_str
                    self.frontend_tuner_status[tuner_num].spc_start_frequency = float(self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.getMinFrequency())
                    self.frontend_tuner_status[tuner_num].spc_stop_frequency = float(self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.getMaxFrequency())
                self.SRIchanged(tuner_num, data_port, fft_port, spc_port)

            except Exception, e:
                self._log.exception((e))
                self._log.error("ERROR WHEN UPDATING TUNER STATUS FOR TUNER: "+ str(tuner_num))
                
            
    def update_output_configuration(self, updateTunerStatus=True):
        self._log.debug("update_output_configuration " )
        if self.MSDD == None:
            return False
    
        oc_hash={}
        
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
                oc_hash[oc_num] = output_config(tuner_channel_number=oc_num,
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
                    
        # msdd_output_configuration
        for oc_num in range(0,len(self.msdd_output_configuration)):
            tuner_num = self.msdd_output_configuration[oc_num].tuner_number
            vlan_enabled=(self.msdd_output_configuration[oc_num].vlan > 0 and self.msdd_output_configuration[oc_num].vlan_enable)
            vlan=max(0,self.msdd_output_configuration[oc_num].vlan)
            oc_hash[tuner_num] = output_config(tuner_channel_number=tuner_num,
                                            enabled=self.msdd_output_configuration[oc_num].enabled,
                                            ip_address=str(self.msdd_output_configuration[oc_num].ip_address).strip(),
                                            port=self.msdd_output_configuration[oc_num].port,
                                            vlan_enabled=vlan_enabled,
                                            vlan_tci=vlan,
                                            protocol=self.msdd_output_configuration[oc_num].protocol,
                                            endianess = self.msdd_output_configuration[oc_num].endianess,
                                            timestamp_offset=self.msdd_output_configuration[oc_num].timestamp_offset,
                                             mfp_flush=self.msdd_output_configuration[oc_num].mfp_flush)
    
        
        for tuner_num in range(0,len(self.frontend_tuner_status)):
            try:
                if not self.frontend_tuner_status[tuner_num].rx_object.has_streaming_output():
                    continue
                if not oc_hash.has_key(tuner_num):
                    self.frontend_tuner_status[tuner_num].rx_object.output_object.object.setEnable(False)
                    continue
                success= self.frontend_tuner_status[tuner_num].rx_object.output_object.object.setEnable(oc_hash[tuner_num].enabled)
                success &= self.frontend_tuner_status[tuner_num].rx_object.output_object.object.setIP(oc_hash[tuner_num].ip_address,str(oc_hash[tuner_num].port))
                proto = self.frontend_tuner_status[tuner_num].rx_object.output_object.object.getOutputProtocolNumberFromString(oc_hash[tuner_num].protocol)
                success &= self.frontend_tuner_status[tuner_num].rx_object.output_object.object.setOutputProtocol(proto)
                success &= self.frontend_tuner_status[tuner_num].rx_object.output_object.object.setCDR(self.VITA49CONTEXTPACKETINTERVAL)
                success &= self.frontend_tuner_status[tuner_num].rx_object.output_object.object.setCCR(1)
                success &= self.frontend_tuner_status[tuner_num].rx_object.output_object.object.setTimestampOff(oc_hash[tuner_num].timestamp_offset)
                success &= self.frontend_tuner_status[tuner_num].rx_object.output_object.object.setEnableVlanTagging(oc_hash[tuner_num].vlan_enabled)
                success &= self.frontend_tuner_status[tuner_num].rx_object.output_object.object.setOutputEndianess(oc_hash[tuner_num].endianess)
                success &= self.frontend_tuner_status[tuner_num].rx_object.output_object.object.setVlanTci(max(0,oc_hash[tuner_num].vlan_tci))
                self.frontend_tuner_status[tuner_num].rx_object.output_object.object.setMFP(oc_hash[tuner_num].mfp_flush)
                if not success:
                    self._log.error("ERROR WHEN UPDATING OUTPUT CONFIGURATION FOR TUNER: "+ str(tuner_num) + " WITH VALUES: " + str(oc_hash[tuner_num]))
            except:
                self._log.error("ERROR WHEN UPDATING OUTPUT CONFIGURATION FOR TUNER: "+ str(tuner_num))
        
        if updateTunerStatus:
            self.update_tuner_status()
        
        
        self.msdd_time_of_day_configuration_changed("msdd_time_of_day_configuration",self.msdd_time_of_day_configuration,  self.msdd_time_of_day_configuration)
        return True


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
        if self.MSDD == None:
            return
        
        for tuner_num in range(0,len(self.frontend_tuner_status)):
            self.set_default_msdd_gain_value_for_tuner(tuner_num)
        self.update_tuner_status()
    
    
    
    
    def clock_ref_changed(self, propid,oldval, newval):
        self.clock_ref = newval  
        if self.MSDD == None:
            return
        for tuner_num in range(0,len(self.frontend_tuner_status)):
            if self.frontend_tuner_status[tuner_num].rx_object.is_analog():
                self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.setExternalRef(self.clock_ref)
        for board_num in range(0,len(self.MSDD.board_modules)):
            self.MSDD.board_modules[board_num].object.setExternalRef(self.clock_ref)
        
        self.update_msdd_status()
          
                
    def advanced_changed(self, propid,oldval, newval):
        self.advanced = newval   
        if self.MSDD != None:
            self.MSDD.com.update_timeout(self.advanced.udp_timeout)
        
        for tuner_num in range(0,len(self.frontend_tuner_status)):
       
            tuner_types=[]
            if self.MSDD.rx_channels[tuner_num].is_fft():
                tuner_types = self.getTunerTypeList(self.advanced.psd_mode)
            elif self.MSDD.rx_channels[tuner_num].is_analog_only(): #Receiver
                tuner_types = self.getTunerTypeList(self.advanced.rcvr_mode)
            elif self.frontend_tuner_status[tuner_num].rx_object.is_digital_only() and self.frontend_tuner_status[tuner_num].rx_object.is_hardware_based(): # hw_ddc
                tuner_types = self.getTunerTypeList(self.advanced.hw_ddc_mode)
            elif self.frontend_tuner_status[tuner_num].rx_object.is_digital_only(): #sw_ddc
                tuner_types = self.getTunerTypeList(self.advanced.sw_ddc_mode)
            elif self.frontend_tuner_status[tuner_num].rx_object.is_analog_and_digital():  #wb_ddc
                tuner_types = self.getTunerTypeList(self.advanced.wb_ddc_mode)
            elif self.MSDD.rx_channels[tuner_num].is_spectral_scan():
                tuner_types = self.getTunerTypeList(self.advanced.spc_mode)
            else:
                raise Exception("UNKOWN TUNER TYPE") 
            self.frontend_tuner_status[tuner_num].tuner_types = tuner_types

    
    def msdd_output_configuration_changed(self, propid, oldval, newval):
        self.msdd_output_configuration = newval
        self.update_output_configuration()
        
    def msdd_block_output_configuration_changed(self, propid, oldval, newval):
        self.msdd_block_output_configuration = newval
        self.update_output_configuration()
        
    def msdd_psd_configuration_changed(self,propid, oldval, newval):
        self.msdd_psd_configuration = newval
        for tuner_num in range(0,len(self.frontend_tuner_status)):
            self.update_fft_configuration(tuner_num)

    def msdd_spc_configuration_changed(self,propid, oldval, newval):
        self.msdd_spc_configuration = newval
        for tuner_num in range(0,len(self.frontend_tuner_status)):
            self.update_spc_configuration(tuner_num)

        
    def msdd_time_of_day_configuration_changed(self,propid, oldval, newval):
        self.msdd_time_of_day_configuration = newval
        if self.MSDD == None:
            return False
        
        if len(self.MSDD.tod_modules) > 0:
            mode_num = self.MSDD.tod_modules[0].object.getModeNumFromString(self.msdd_time_of_day_configuration.mode)
            self.MSDD.tod_modules[0].object.mode = mode_num
            self.msdd_time_of_day_configuration.mode = str(self.MSDD.tod_modules[0].object.mode_string)
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
            self.msdd_advanced_debugging_tools.response = self.MSDD.console_module.send_custom_command(self.msdd_advanced_debugging_tools.command + '\n')
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
    
    def SRIchanged(self, tunerNum, data_port = True, fft_port = False, spc_port = False):
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

        if spc_port:
            num_bins = self.frontend_tuner_status[tunerNum].spc_output_bin_size
            if num_bins <= 0:
                num_bins = self.frontend_tuner_status[tunerNum].spc_fft_size      
            binFreq = self.frontend_tuner_status[tunerNum].spc_stop_frequency - self.frontend_tuner_status[tunerNum].spc_start_frequency / num_bins;
            
            #tuner is active, so generate timestamp and header info
            H = BULKIO.StreamSRI(hversion=1, #header version
                                 xstart=self.frontend_tuner_status[tunerNum].spc_start_frequency, #no offset provided
                                 xdelta=binFreq,
                                 xunits=BULKIO.UNITS_FREQUENCY, #for time data
                                 subsize=long(num_bins), #1-d data
                                 ystart=0.0, #n/a
                                 ydelta=self.frontend_tuner_status[tunerNum].spc_time_between_ffts * 1.0e-3, #n/a
                                 yunits=BULKIO.UNITS_TIME, #n/a
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
                self.port_dataSDDS_out_SPC.pushSRI(H, T)
                self._log.debug("Pushing SPC SRI on alloc_id: " + str(alloc_id) + " with contents: " + str(H))
          

    def sendAttach(self, alloc_id, tuner_num, data_port = True, fft_port = False, spc_port = False ):
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
                num_bins = self.frontend_tuner_status[tuner_num].psd_output_bin_size
                if num_bins <= 0:
                    num_bins = self.frontend_tuner_status[tuner_num].psd_fft_size      
                binFreq = self.frontend_tuner_status[tuner_num].sample_rate / num_bins;
                sdef = BULKIO.SDDSStreamDefinition(id = alloc_id,
                                                   dataFormat = BULKIO.SDDS_SI,
                                                   multicastAddress = self.frontend_tuner_status[tuner_num].output_multicast,
                                                   vlan = int(self.frontend_tuner_status[tuner_num].output_vlan),
                                                   port = int(self.frontend_tuner_status[tuner_num].output_port),
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
                    vdef = BULKIO.VITA49StreamDefinition(ipAddress= self.frontend_tuner_status[tuner_num].output_multicast, 
                                                     vlan = int(self.frontend_tuner_status[tuner_num].output_vlan), 
                                                     port = int(self.frontend_tuner_status[tuner_num].output_port), 
                                                     protocol = BULKIO.VITA49_UDP_TRANSPORT,
                                                     valid_data_format = True, 
                                                     data_format = vita_data_payload_format)
                except:
                    vdef = BULKIO.VITA49StreamDefinition(ipAddress= self.frontend_tuner_status[tuner_num].output_multicast, 
                                                     vlan = int(self.frontend_tuner_status[tuner_num].output_vlan), 
                                                     port = int(self.frontend_tuner_status[tuner_num].output_port), 
                                                     id = alloc_id,
                                                     protocol = BULKIO.VITA49_UDP_TRANSPORT,
                                                     valid_data_format = True, 
                                                     data_format = vita_data_payload_format)
                
                
                self.port_dataVITA49_out_PSD.attach(vdef, alloc_id)
        
        if spc_port:
            if self.frontend_tuner_status[tuner_num].output_enabled:
                num_bins = self.frontend_tuner_status[tuner_num].spc_output_bin_size
                if num_bins <= 0:
                    num_bins = self.frontend_tuner_status[tuner_num].spc_fft_size      
                binFreq = (self.frontend_tuner_status[tuner_num].spc_stop_frequency - self.frontend_tuner_status[tuner_num].spc_start_frequency) / num_bins;
                sdef = BULKIO.SDDSStreamDefinition(id = alloc_id,
                                                   dataFormat = BULKIO.SDDS_SI,
                                                   multicastAddress = self.frontend_tuner_status[tuner_num].output_multicast,
                                                   vlan = int(self.frontend_tuner_status[tuner_num].output_vlan_tci),
                                                   port = int(self.frontend_tuner_status[tuner_num].output_port),
                                                   sampleRate = int(binFreq),
                                                   timeTagValid = True,
                                                   privateInfo = "MSDD SPC Tuner #"+str(tuner_num) )
                self.port_dataSDDS_out_SPC.attach(sdef, alloc_id)
     
    
        #update and send SRI too
        self.SRIchanged(tuner_num, data_port, fft_port, spc_port)
        
    
    def sendDetach(self, alloc_id, data_port = True, fft_port = True, spc_port = True):
        self._log.debug("Sending Detach() on ID:"+str(alloc_id))
 
        if data_port:
            self.port_dataSDDS_out.detach(connectionId=alloc_id)
            self.port_dataVITA49_out.detach(connectionId=alloc_id)
        if fft_port:
            self.port_dataSDDS_out_PSD.detach(connectionId=alloc_id)
            self.port_dataVITA49_out_PSD.detach(connectionId=alloc_id)
        if spc_port:
            self.port_dataSDDS_out_SPC.detach(connectionId=alloc_id)
          


    def register_fft_connection(self, alloc_id):
        parent_tuner_num = self.findTunerByAllocationID(alloc_id=alloc_id, include_control=True, include_listeners=True, fft_mode=False)
        if parent_tuner_num != None:
            for tuner_num in range(0,len(self.frontend_tuner_status)):
                if self.frontend_tuner_status[tuner_num].allocated or self.frontend_tuner_status[tuner_num].msdd_channel_type != MSDDRadio.MSDDRXTYPE_FFT:
                    continue 
                self.frontend_tuner_status[tuner_num].input_sample_rate =  self.frontend_tuner_status[parent_tuner_num].input_sample_rate
                self.frontend_tuner_status[tuner_num].sample_rate = self.frontend_tuner_status[parent_tuner_num].sample_rate
                self.frontend_tuner_status[tuner_num].bandwidth = self.frontend_tuner_status[parent_tuner_num].bandwidth
                self.frontend_tuner_status[tuner_num].center_frequency = self.frontend_tuner_status[parent_tuner_num].center_frequency      
                self.frontend_tuner_status[tuner_num].allocated = True
                self.frontend_tuner_status[tuner_num].allocation_id_control = self.frontend_tuner_status[parent_tuner_num].allocation_id_control
                self.frontend_tuner_status[tuner_num].allocation_id_csv = self.frontend_tuner_status[parent_tuner_num].allocation_id_csv
            
                self.update_fft_configuration(tuner_num)
                
                source_reg_name = self.frontend_tuner_status[parent_tuner_num].rx_object.digital_rx_object.object.full_reg_name
                dest_reg_name = self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.full_reg_name
                self.MSDD.stream_router.linkModules(source_reg_name,dest_reg_name)
                
                self.update_tuner_status([tuner_num])
                self.sendAttach(self.frontend_tuner_status[tuner_num].allocation_id_control, tuner_num, False, True, False)
                if self.pending_fft_registrations.count(alloc_id) > 0:
                    self.pending_fft_registrations.remove(alloc_id)
                return True
    
        if self.pending_fft_registrations.count(alloc_id) <= 0:
            self.pending_fft_registrations.append(alloc_id)
        
        return False
    
    def update_fft_configuration(self,tuner_num):
        if tuner_num == None:
            return False
        if not self.frontend_tuner_status[tuner_num].allocated or self.frontend_tuner_status[tuner_num].msdd_channel_type != MSDDRadio.MSDDRXTYPE_FFT:
            return False 
        
        self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.fftSize = self.msdd_psd_configuration.fft_size
        self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.time_between_fft_ms = self.msdd_psd_configuration.time_between_ffts
        self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.outputBins = self.msdd_psd_configuration.output_bin_size
        self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.window_type_str = self.msdd_psd_configuration.window_type
        self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.peak_mode_str = self.msdd_psd_configuration.peak_mode 
        
        ffts_per_second = self.frontend_tuner_status[tuner_num].sample_rate / self.msdd_psd_configuration.fft_size
        num_avg = ffts_per_second * self.msdd_psd_configuration.time_average
        num_avg = self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.getValidAverage(num_avg)
        self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.num_averages = num_avg
        self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.enable = True
        
        
    def deregister_fft_connection(self, alloc_id):
        if self.pending_fft_registrations.count(alloc_id) > 0:
            self.pending_fft_registrations.remove(alloc_id)
        tuner_num = self.findTunerByAllocationID(alloc_id=alloc_id, include_control=True, include_listeners=True, fft_mode=True)
        if tuner_num == None:
            return False
        
        self.frontend_tuner_status[tuner_num].rx_object.fft_object.object.enable = False
        self.purgeListeners(tuner_num)
        
        self.frontend_tuner_status[tuner_num].input_sample_rate =  0
        self.frontend_tuner_status[tuner_num].sample_rate = 0
        self.frontend_tuner_status[tuner_num].bandwidth = 0
        self.frontend_tuner_status[tuner_num].center_frequency = 0
        self.frontend_tuner_status[tuner_num].allocated = False
        self.frontend_tuner_status[tuner_num].allocation_id_control = ""
        self.frontend_tuner_status[tuner_num].allocation_id_csv = ""
        
        self.update_tuner_status([tuner_num])    
        self.sendDetach(alloc_id, False, True, False)
        return True
    
    def register_spc_connection(self, alloc_id, parent_tuner):
        self._log.trace("Register spc connection called")
        parent_tuner_num = parent_tuner
        if parent_tuner_num != None:
            for tuner_num in range(0,len(self.frontend_tuner_status)):
                if not alloc_id in self.frontend_tuner_status[tuner_num].allocation_id_csv:
                    continue 
                self.frontend_tuner_status[tuner_num].input_sample_rate =  self.frontend_tuner_status[parent_tuner_num].input_sample_rate
    
                self.frontend_tuner_status[parent_tuner_num].allocated = self.frontend_tuner_status[tuner_num].allocated
                #self.frontend_tuner_status[parent_tuner_num].allocation_id_control = self.frontend_tuner_status[tuner_num].allocation_id_control
                #self.frontend_tuner_status[parent_tuner_num].allocation_id_csv = self.frontend_tuner_status[tuner_num].allocation_id_csv
                
                self.update_spc_configuration(tuner_num, parent_tuner_num)
                
                self.update_tuner_status([tuner_num])
                self.sendAttach(self.frontend_tuner_status[tuner_num].allocation_id_control, tuner_num, False, False, True)
                if self.pending_spc_registrations.count(alloc_id) > 0:
                    self.pending_spc_registrations.remove(alloc_id)
                return True
    
        if self.pending_spc_registrations.count(alloc_id) <= 0:
            self.pending_spc_registrations.append(alloc_id)
        
        return False
    
    def update_spc_configuration(self,tuner_num, parent_num=None):
        if tuner_num == None:
            return False
        if not self.frontend_tuner_status[tuner_num].allocated or self.frontend_tuner_status[tuner_num].msdd_channel_type != MSDDRadio.MSDDRXTYPE_SPC:
            return False 
        
        if parent_num == None:
            parent_num = self.MSDD.get_parent_tuner_num(self.frontend_tuner_status[tuner_num].rx_object)
            if parent_num == None:
                return False
        
        self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.fftSize = self.msdd_spc_configuration.fft_size
        self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.time_between_fft_ms = self.msdd_spc_configuration.time_between_ffts
        self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.window_type_str = self.msdd_spc_configuration.window_type
        self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.peak_mode_str = self.msdd_spc_configuration.peak_mode 
        
        self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.outputBins = self.msdd_spc_configuration.output_bin_size
        self.frontend_tuner_status[tuner_num].sample_rate = self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.outputBins
        self.frontend_tuner_status[tuner_num].spc_output_bin_size = self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.outputBins
        
        self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.setFrequencies(self.msdd_spc_configuration.start_frequency, self.msdd_spc_configuration.stop_frequency)
        self.frontend_tuner_status[tuner_num].bandwidth = self.msdd_spc_configuration.stop_frequency - self.msdd_spc_configuration.start_frequency
        self.frontend_tuner_status[tuner_num].center_frequency =  (self.msdd_spc_configuration.stop_frequency + self.msdd_spc_configuration.start_frequency) / 2.0     
        self.frontend_tuner_status[tuner_num].sample_rate = self.frontend_tuner_status[tuner_num].bandwidth
        
        num_avg = self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.getValidAverage(self.msdd_spc_configuration.num_fft_averages)
        self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.num_averages = num_avg
        self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.enable = True
        if self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_wbddc_object:
            self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_wbddc_object.object.enable = True
        
    def deregister_spc_connection(self, alloc_id):
        if self.pending_spc_registrations.count(alloc_id) > 0:
            self.pending_spc_registrations.remove(alloc_id)
        tuner_num = self.findTunerByAllocationID(alloc_id=alloc_id, include_control=True, include_listeners=True, spc_mode=True)
        if tuner_num == None:
            return False
        
        self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.enable = False
        self.purgeListeners(tuner_num)
        
        self.frontend_tuner_status[tuner_num].input_sample_rate =  0
        self.frontend_tuner_status[tuner_num].sample_rate = 0
        self.frontend_tuner_status[tuner_num].bandwidth = 0
        self.frontend_tuner_status[tuner_num].center_frequency = 0
        self.frontend_tuner_status[tuner_num].allocated = False
        self.frontend_tuner_status[tuner_num].allocation_id_control = ""
        self.frontend_tuner_status[tuner_num].allocation_id_csv = ""

        if self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_wbddc_object:
            self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_wbddc_object.object.enable = False

        
        self.update_tuner_status([tuner_num])    
        self.sendDetach(alloc_id, False, False, True)
        return True

    
    def _allocate_frontend_tuner_allocation(self, value, from_child_tuner_num = None, retry_tuner_num = None):
        
        self._log.trace( "Received a tuner allocation, contents:"+str(value))
        internal_allocation_request = (from_child_tuner_num != None)
        
        # These are used later in the allocation. If the allocation fails, then we may be able to remap the child tuner (ie -software ddc) 
        #   in the MSDD with another parent.  
        available_sw_tuners = []
        available_hw_tuners = []
        
        self._log.debug("!!!! ALLOCATION REQUEST (INTERNAL=" + str(internal_allocation_request) + ") WITH CONFIG: " + str(value))

        if value.tuner_type == self.FE_TYPE_DDC:
            reject_allocation = True
            for tn in self.dig_tuner_base_nums:
                reject_allocation = reject_allocation and (self.frontend_tuner_status[tn].allocated and self.frontend_tuner_status[tn].tuner_type == self.FE_TYPE_RXDIG)
            if reject_allocation:
                self._log.debug("Allocation Failed. Asked for a DDC and all tuners were RX_DIGITIZERS and already allocated")
                return False
        
        #check allocationID is valid
        if value.allocation_id in ("",None):
            self._log.warn("Allocation ID cannot be blank")
            raise CF.Device.InvalidCapacity("Allocation ID cannot be blank",properties.struct_to_props(value))
        
        #Check if allocationID is already used
        for tuner in self.frontend_tuner_status:
            alloc_ids = tuner.allocation_id_csv.split(',')
            for alloc_id in alloc_ids:
                if alloc_id==value.allocation_id:
                    self._log.warn("Allocation ID already in use")
                    raise CF.Device.InvalidCapacity("Allocation ID already used ",properties.struct_to_props(value))

        if len(self.tuner_allocation_ids) != len(self.frontend_tuner_status):
            for idx in range(len(self.frontend_tuner_status)-len(self.tuner_allocation_ids)):
                self.tuner_allocation_ids.append(tuner_allocation_ids_struct())
                
        if retry_tuner_num == None:
            tuner_range = range(0,len(self.frontend_tuner_status))
        else:
            tuner_range = [retry_tuner_num]
        for tuner_num in tuner_range:
            self._log.debug("--- ATTEMPTING ALLOCATION  (INTERNAL=" + str(internal_allocation_request) + ") ON TUNER: " + str(tuner_num) + " WITH CONFIG: " + str(value))
            # Used to map out available hardware and software tuners
            if self.frontend_tuner_status[tuner_num].rx_object.is_hardware_based():
                available_hw_tuners.append(tuner_num)
            else:
                if not(self.frontend_tuner_status[tuner_num].allocated):
                    available_sw_tuners.append(tuner_num)
            
            ########## BASIC CHECKS FOR TUNER TYPE, RF FLOW ID AND GROUP ID ##########
            if self.frontend_tuner_status[tuner_num].tuner_types.count(value.tuner_type) <= 0:
                self._log.debug(' Allocation Failed, No tuner of the correct type available')
                continue 
            if len(value.rf_flow_id) != 0 and  value.rf_flow_id != self.device_rf_flow:
                self._log.debug(' Allocation Failed, rf_flow_ID does not match')
                continue
            if value.group_id != self.frontend_group_id:
                self._log.debug(' Allocation Failed, group ID does not match')
                continue
            
            # Non-internal allocations can not use internal allocated parents
            if not internal_allocation_request and ( self.frontend_tuner_status[tuner_num].allocated and self.frontend_tuner_status[tuner_num].internal_allocation ) :
                self._log.debug(' Allocation Failed, Non-internal allocations can not use internal allocated parents')
                continue
            
            # Check that RF will fit within RFInfo Setting
            if self.device_rf_info_pkt:
                try:
                    validateRequestVsRFInfo(value,self.device_rf_info_pkt,1)
                except FRONTEND.BadParameterException , e:
                    self._log.info("ValidateRequestVsRFInfo Failed: %s" %(str(e)))
                    continue
            
            
            # Calculate IF Offset if there is one
            if_freq = self.convert_rf_to_if(value.center_frequency) 
            
            ########## LISTENER ALLOCATION - IE - DEVICE CONTROL IS FALSE ##########
            if not value.device_control:
                if not self.frontend_tuner_status[tuner_num].allocated:
                    self._log.debug(' Listener Allocation failed because tuner was not already allocated')
                    continue
                
                reg_sr = value.sample_rate
                reg_sr_tolerance = value.sample_rate_tolerance
                if type == self.FE_TYPE_RECEIVER:
                    reg_sr = None
                    reg_sr_tolerance = None
                valid = self.range_check(value.center_frequency, value.bandwidth, value.bandwidth_tolerance, reg_sr, reg_sr_tolerance,
                                         self.frontend_tuner_status[tuner_num].center_frequency, self.frontend_tuner_status[tuner_num].bandwidth, self.frontend_tuner_status[tuner_num].sample_rate)
                             
                if not valid:
                    self._log.debug(" Listener Allocation failed, parameters don't match")
                    continue
                
                # Make a Listener allocation for this tuner with the requested allocation ID
                listenerallocation = frontend.fe_types.frontend_listener_allocation()
                listenerallocation.existing_allocation_id = self.frontend_tuner_status[tuner_num].allocation_id_control
                listenerallocation.listener_allocation_id = value.allocation_id
                try:
                    retval =  self._allocate_frontend_listener_allocation(listenerallocation)
                except:
                    self._log.exception("Could not allocation Listener")
                    return False
                if retval: 
                    if internal_allocation_request:
                        # If this listener is allocated for a child tuner make the necessary links and updates. 
                        self.frontend_tuner_status[tuner_num].internal_allocation_children.append(from_child_tuner_num)
                        self.frontend_tuner_status[from_child_tuner_num].internal_allocation_parent = tuner_num
                        try:
                            if not self.MSDD.add_child_tuner(self.frontend_tuner_status[tuner_num].rx_object,self.frontend_tuner_status[from_child_tuner_num].rx_object):
                                self._log.error("Child Add Failed")
                                raise
                        except:
                            self._log.exception("Error on add_child")
                            return False
                        try:
                            self.frontend_tuner_status[from_child_tuner_num].rx_object.digital_rx_object.object.updateRfFrequencyOffset(self.convert_rf_to_if(self.frontend_tuner_status[tuner_num].center_frequency))
                        except:
                            self._log.exception("Error on updateRfFreq " + str(self.frontend_tuner_status[tuner_num].center_frequency))
                            return False                    
                    return True
                else:
                    self._log.debug(" Allocating Listener Failed")
                    return False
                
                
            ########## CONTROLLER ALLOCATION - IE - DEVICE CONTROL IS TRUE ##########
            if self.frontend_tuner_status[tuner_num].allocated:
                self._log.debug(" Control Allocating failed because given tuner was already allocated")
                continue
            

                
            ### MAKE SURE PARENT IS ALLCOATED
            parent_tuner = self.MSDD.get_parent_tuner_num(self.frontend_tuner_status[tuner_num].rx_object)
            if parent_tuner != None and value.tuner_type != self.FE_TYPE_SPC:
                if not self.frontend_tuner_status[parent_tuner].allocated:
                    self._log.debug(" Control Allocating failed because given tuner's parent was not already allocated")
                    continue

            try:    
                valid_cf = None
                valid_sr = None
                valid_bw = None
                success = True
                
                if self.frontend_tuner_status[tuner_num].rx_object.is_digital_only(): #Only set center frequency if it does NOT have a corresponding analog receiver
                    try:
                        valid_cf = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_frequency(if_freq)
                    except:
                        self._log.exception("--- Exception on get_valid_frequency ")
                        success = False
                    success &= self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setFrequency_Hz(valid_cf)
                        
                if self.frontend_tuner_status[tuner_num].rx_object.is_digital():
                    if value.sample_rate!=0 and value.bandwidth!=0: #Specify a SR and BW
                        sr = max(value.bandwidth,value.sample_rate) #Ask for enough sr to satisfy the bw request
                        valid_sr = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_sample_rate(sr, value.sample_rate_tolerance)
                        success &= self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setSampleRate(valid_sr)
                        valid_bw = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_bandwidth(value.bandwidth, value.bandwidth_tolerance)
                        success &= self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setBandwidth_Hz(valid_bw)
                        success &= self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setEnable(True) 
                                          
                    elif value.sample_rate!=0 and value.bandwidth==0: #Specify only SR
                        valid_sr = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_sample_rate(value.sample_rate, value.sample_rate_tolerance)
                        valid_bw = valid_sr
                        success &= self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setSampleRate(valid_sr)
                        success &= self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setBandwidth_Hz(valid_bw)
                        success &= self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setEnable(True)
                       
                    elif value.sample_rate==0 and value.bandwidth!=0: #specify only BW so use it for requested SR with a large tolerance because sample rate is 'don't care'
                        valid_sr = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_sample_rate(value.bandwidth, 100)
                        success &= self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setSampleRate(valid_sr)
                        valid_bw = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_bandwidth(value.bandwidth, value.bandwidth_tolerance)
                        success &= self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setBandwidth_Hz(valid_bw)
                        success &= self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setEnable(True)
                    elif value.sample_rate==0 and value.bandwidth==0: #Don't care for both sample_rate and bandwidth so find any valid value and use it
                        valid_sr = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_sample_rate(1, 1e15)
                        success &= self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setSampleRate(valid_sr)
                        valid_bw = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_bandwidth(1, 1e15)
                        success &= self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setBandwidth_Hz(valid_bw)
                        success &= self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setEnable(True)
                    else:
                        self._log.error("Allocation Failed, unknown reason, Shouldn't get here")
                        success = False
                
                if self.frontend_tuner_status[tuner_num].rx_object.is_analog():
                    valid_sr = 0.0
                    valid_cf = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.get_valid_frequency(if_freq)
                    if value.bandwidth != 0:
                        valid_bw = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.get_valid_bandwidth(value.bandwidth, value.bandwidth_tolerance)
                    else: 
                        valid_bw = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.get_valid_bandwidth(1, 1e15)
                    success &= self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.setFrequency_Hz(valid_cf)
                    success &= self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.setBandwidth_Hz(valid_bw)
                
                if self.frontend_tuner_status[tuner_num].rx_object.is_spectral_scan() and value.tuner_type == self.FE_TYPE_SPC:
                    #Because it uses the RCV module, use it as part of the validation
                    valid_sr = 0.0
                    minFreq = (if_freq - value.bandwidth / 2.0) 
                    maxFreq = (if_freq + value.bandwidth / 2.0) 
                    valid_cf_min = self.frontend_tuner_status[tuner_num].rx_object.rx_parent_object.analog_rx_object.object.get_valid_frequency(minFreq)
                    valid_cf_max = self.frontend_tuner_status[tuner_num].rx_object.rx_parent_object.analog_rx_object.object.get_valid_frequency(maxFreq)
                    valid_cf = valid_cf_min and valid_cf_max and maxFreq > minFreq
                    self.msdd_spc_configuration.start_frequency = minFreq
                    self.msdd_spc_configuration.stop_frequency = maxFreq
                    valid_bw = value.bandwidth > 0
                    success &= self.frontend_tuner_status[tuner_num].rx_object.spectral_scan_object.object.setFrequencies(minFreq, maxFreq)
                self.set_default_msdd_gain_value_for_tuner(tuner_num)
                
                if valid_cf == None or valid_bw == None or valid_sr == None or not success:
                    self._log.debug("EITHER CENTER FREQUENCY, BANDWIDTH, OR SAMPLE RATE IS INVALID")
                    self._log.debug("Valid_cf: %s, Valid_bw: %s, Valid_sr: %s, Success: %s" %(valid_cf,valid_bw,valid_sr,success))
                    raise Exception("EITHER CENTER FREQUENCY, BANDWIDTH, OR SAMPLE RATE IS INVALID")
        
                if internal_allocation_request:
                    self.frontend_tuner_status[tuner_num].internal_allocation = True
                    self.frontend_tuner_status[tuner_num].internal_allocation_children.append(from_child_tuner_num)
                    self.frontend_tuner_status[from_child_tuner_num].internal_allocation_parent = tuner_num
                    self.MSDD.add_child_tuner(self.frontend_tuner_status[tuner_num].rx_object,self.frontend_tuner_status[from_child_tuner_num].rx_object)

                for children in self.frontend_tuner_status[tuner_num].rx_object.rx_child_objects:
                    if children.digital_rx_object != None:
                        children.digital_rx_object.object.updateRfFrequencyOffset(valid_cf)
                        
                self.frontend_tuner_status[tuner_num].tuner_type = value.tuner_type
                self.frontend_tuner_status[tuner_num].allocated = True
                self.frontend_tuner_status[tuner_num].allocation_id_control = value.allocation_id;
                self.frontend_tuner_status[tuner_num].allocation_id_csv = self.create_allocation_id_csv(self.frontend_tuner_status[tuner_num].allocation_id_control, self.frontend_tuner_status[tuner_num].allocation_id_listeners)
                self.update_tuner_status([tuner_num])
                self.frontend_tuner_status[tuner_num].rf_flow_id = self.device_rf_flow
                self.matchAllocationIdToStreamId(value.allocation_id, value.allocation_id,"dataSDDS_out")
                self.matchAllocationIdToStreamId(value.allocation_id, value.allocation_id,"dataSDDS_out_PSD")
                self.matchAllocationIdToStreamId(value.allocation_id, value.allocation_id,"dataSDDS_out_SPC")
                self.matchAllocationIdToStreamId(value.allocation_id, value.allocation_id,"dataVITA49_out")
                self.matchAllocationIdToStreamId(value.allocation_id, value.allocation_id,"dataVITA49_out_PSD")
                self.tuner_allocation_ids[tuner_num].control_allocation_id = value.allocation_id
                self.allocation_id_to_tuner_id[value.allocation_id] =  tuner_num
                
                if value.tuner_type != self.FE_TYPE_SPC:
                    self.sendAttach(self.frontend_tuner_status[tuner_num].allocation_id_control, tuner_num)

                self._log.info("--- SUCCESSFUL ALLOCATION REQUREST  (INTERNAL=" + str(internal_allocation_request) + ") ON TUNER: " + str(tuner_num) + " WITH CONFIG: " + str(value))

                if len(self.msdd_block_output_configuration)==0 and len(self.msdd_output_configuration)==0:
                    self._log.info("Tuner Allocated but the output is not currently setup. No data will be output until the output is configured")

                if self.pending_fft_registrations.count(self.frontend_tuner_status[tuner_num].allocation_id_control) > 0:
                    self.register_fft_connection(self.frontend_tuner_status[tuner_num].allocation_id_control)
                    
                if value.tuner_type == self.FE_TYPE_SPC:
                    parent_tuner = self.MSDD.get_parent_tuner_num(self.frontend_tuner_status[tuner_num].rx_object)
                    self.register_spc_connection(self.frontend_tuner_status[tuner_num].allocation_id_control, parent_tuner)
                   
                return True
                
            except Exception,e:
                    self._log.exception("Failed to allocate: " + str(e))
                    self._log.info("--- FAILED ALLOCATION REQUREST  (INTERNAL=" + str(internal_allocation_request) + ") ON TUNER: " + str(tuner_num) + " WITH CONFIG: " + str(value))
                    continue
        #If got here, than all allocations failed. We can check to see if we can remap a software ddc to a hardware ddc. Currently we 
        # do NOT allow chaining of multiple software ddc's together
        allow_remap_check = len(available_sw_tuners) > 0 and len(available_hw_tuners) > 0 and self.advanced.allow_internal_allocations
        if allow_remap_check and not(internal_allocation_request) and value.tuner_type=='DDC':
            self._log.debug("Looking to do make an internal allocation") 
            sw_tuner_num = available_sw_tuners[0]
            parent_allocation = copy.deepcopy(value)
            parent_allocation.tuner_type = self.FE_TYPE_DDC
            parent_allocation.allocation_id = "internal_alloc_" + str(uuid.uuid4())  
            dec_list = self.frontend_tuner_status[sw_tuner_num].rx_object.digital_rx_object.object.getDecimationList()
            parent_allocation.sample_rate *= dec_list[0]
            parent_allocation.sample_rate_tolerance = ((value.sample_rate *(1+value.sample_rate_tolerance/100)*dec_list[-1])/parent_allocation.sample_rate -1)*100
            parent_allocation.bandwidth *= dec_list[0]
            parent_allocation.bandwidth_tolerance = ((value.bandwidth *(1+value.bandwidth_tolerance/100)*dec_list[-1])/parent_allocation.bandwidth -1)*100
            parent_allocation.device_control = False
            parent_allocated = False
            try:
                self._log.trace("--- ATTEMPTING PARENT SOFTWARE REMAP ALLOCATION REQUREST AS LISTENER (INTERNAL=" + str(internal_allocation_request) + ") ON TUNER: " + str(sw_tuner_num) + " WITH CONFIG: " + str(parent_allocation))
                parent_allocated = self._allocate_frontend_tuner_allocation(parent_allocation,sw_tuner_num,None)
                if not parent_allocated:
                    parent_allocation.device_control = True
                    self._log.trace("--- 2ATTEMPTING PARENT SOFTWARE REMAP ALLOCATION REQUREST AS CONTROL (INTERNAL=" + str(internal_allocation_request) + ") ON TUNER: " + str(sw_tuner_num) + " WITH CONFIG: " + str(parent_allocation))
                    parent_allocated = self._allocate_frontend_tuner_allocation(parent_allocation,sw_tuner_num,None)
                if not parent_allocated:
                    raise Exception("PARENT COULD NOT BE ALLOCATED")
                self._log.trace("--- ATTEMPTING SW TUNER SOFTWARE REMAP ALLOCATION REQUREST  (INTERNAL=" + str(internal_allocation_request) + ") ON TUNER: " + str(sw_tuner_num) + " WITH CONFIG: " + str(parent_allocation))

                tuner_allocated = self._allocate_frontend_tuner_allocation(value,None,sw_tuner_num)
                if not tuner_allocated:
                    raise Exception("SW TUNER COULD NOT BE ALLOCATED")
                return True
            except:
                if parent_allocated:
                    self.deallocate_frontend_tuner_allocation(parent_allocation)
                self._log.exception("Allocation Failed. Exception thrown while trying to do an internal allocation") 
                return False
        self._log.debug("Allocation Failed. Tried All Available Tuners and none of them have satisfied the request")
        return False                
            

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
        try:
            if self.frontend_tuner_status[tuner_id].rx_object.is_digital():
                self.frontend_tuner_status[tuner_id].rx_object.digital_rx_object.object.setEnable(True)
        except:
            self._log.exception("Exception on Tuner Enable")
        self.update_tuner_status([tuner_id])
        return

    def deviceDisable(self,fts, tuner_id):
        '''
        ************************************************************
        modify fts, which corresponds to self.frontend_tuner_status[tuner_id]
        Make sure to reset the 'enabled' member of fts to indicate that tuner as disabled
        ************************************************************'''
        try:
            if self.frontend_tuner_status[tuner_id].rx_object.is_digital():
                self.frontend_tuner_status[tuner_id].rx_object.digital_rx_object.object.setEnable(False)
        except:
            self._log.exception("Exception on Tuner Enable")

        self.update_tuner_status([tuner_id])

    def deviceDeleteTuning(self,fts,tuner_id):
        parent_tuner = self.frontend_tuner_status[tuner_id].internal_allocation_parent
        if parent_tuner != None:
            self.frontend_tuner_status[parent_tuner].internal_allocation_children.remove(tuner_id)
            if(len(self.frontend_tuner_status[parent_tuner].internal_allocation_children) <= 0):
                parent_allocation =frontend.fe_types.frontend_tuner_allocation()
                parent_allocation.allocation_id = self.frontend_tuner_status[parent_tuner].allocation_id_control
                self.deallocate_frontend_tuner_allocation(parent_allocation)
        self.frontend_tuner_status[tuner_id].internal_allocation_parent = None
        self.frontend_tuner_status[tuner_id].internal_allocation_children = []
        self.frontend_tuner_status[tuner_id].internal_allocation = False
        self.frontend_tuner_status[tuner_id].allocated = False

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
            if self.frontend_tuner_status[tuner_num].rx_object.is_digital_only():
                org_cf_offset = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.rf_offset_hz
                parent_tnr = self.MSDD.get_parent_tuner_num(self.frontend_tuner_status[tuner_num].rx_object)
                if parent_tnr is not None:
                    #if the parent DDC is no longer tuned in a valid range then retune it
                    if not self.frontend_tuner_status[parent_tnr].enabled or \
                        len(self.MSDD.get_child_tuner_num_list(self.frontend_tuner_status[tuner_num].rx_object)) <2: 
                        self.setTunerCenterFrequency(self.frontend_tuner_status[parent_tnr].allocation_id_control, freq)
                    parent_cf = self.convert_rf_to_if(self.frontend_tuner_status[parent_tnr].center_frequency)
                    self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.updateRfFrequencyOffset(parent_cf)
                    valid_cf = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_frequency(if_freq)
                    if valid_cf is not None:
                        self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setFrequency_Hz(valid_cf)
                        self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.enable = True
                    else:
                        self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.updateRfFrequencyOffset(org_cf_offset)
            if self.frontend_tuner_status[tuner_num].rx_object.is_analog():
                valid_cf = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.get_valid_frequency(if_freq)
                self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.setFrequency_Hz(if_freq)
            if self.frontend_tuner_status[tuner_num].rx_object.is_spectral_scan():
                startFreq = freq - self.frontend_tuner_status[tuner_num].bandwidth / 2.0
                stopFreq = freq + self.frontend_tuner_status[tuner_num].bandwidth / 2.0
                spcconfig = properties.props_to_dict(self.query(properties.props_from_dict({"msdd_spc_configuration": {}})))
                spcconfig["msdd_spc_configuration"]["msdd_spc_configuration::start_frequency"] = startFreq
                spcconfig["msdd_spc_configuration"]["msdd_spc_configuration::stop_frequency"] = stopFreq
                self.configure(properties.props_from_dict(spcconfig))
                valid_cf = True
            if valid_cf == None:
                raise Exception("")
            try:
                
                for child_num in self.MSDD.get_child_tuner_num_list(self.frontend_tuner_status[tuner_num].rx_object):
                    child = self.frontend_tuner_status[child_num]
                    if child.rx_object.digital_rx_object != None and self.frontend_tuner_status[child_num].allocated:
                        org_cf_offset = child.rx_object.digital_rx_object.object.rf_offset_hz
                        child.rx_object.digital_rx_object.object.updateRfFrequencyOffset(valid_cf)
                        self._log.debug("Updating offset with: %s"%valid_cf)
                        try:
                            current_cf = child.rx_object.digital_rx_object.object.get_valid_frequency(child.center_frequency)
                            self._log.debug("Current valid CF %s"%current_cf)
                            child.rx_object.digital_rx_object.object.setFrequency_Hz(current_cf)
                            if child.allocated:
                                child.rx_object.digital_rx_object.object.enable = True
                        except:
                            child.rx_object.digital_rx_object.object.enable = False
                            child.rx_object.digital_rx_object.object.updateRfFrequencyOffset(org_cf_offset)
                        changed_child_numbers.append(child_num)
            except Exception, e:
                self._log.warn("PROBLEM: %s"%e)
        except:
            error_string = "Error setting center frequency to " + str(freq) + " for tuner " + str(tuner_num)
            self._log.exception(error_string)
            raise FRONTEND.BadParameterException(error_string)
        finally:
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
        
        valid_bw = None
        try:
            if self.frontend_tuner_status[tuner_num].rx_object.is_digital():
                try:
                    valid_bw = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_bandwidth(bw,100)
                    self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setBandwidth_Hz(valid_bw)
                except NotImplementedError:
                    None
                valid_bw = 0
        
            if self.frontend_tuner_status[tuner_num].rx_object.is_analog():
                valid_bw = self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.get_valid_bandwidth(bw,100)
                self.frontend_tuner_status[tuner_num].rx_object.analog_rx_object.object.setBandwidth_Hz(valid_bw)
            if self.frontend_tuner_status[tuner_num].rx_object.is_spectral_scan():
                startFreq = self.frontend_tuner_status[tuner_num].center_frequency - bw /2.0
                stopFreq = self.frontend_tuner_status[tuner_num].center_frequency + bw /2.0
                spcconfig = properties.props_to_dict(self.query(properties.props_from_dict({"msdd_spc_configuration": {}})))
                spcconfig["msdd_spc_configuration"]["msdd_spc_configuration::start_frequency"] = startFreq
                spcconfig["msdd_spc_configuration"]["msdd_spc_configuration::stop_frequency"] = stopFreq
                self.configure(properties.props_from_dict(spcconfig))
                valid_bw = True
            if valid_bw == None:
                raise Exception("")
        except Exception, e:
            self._log.warning(str(e))
            error_string = "Error setting bandwidth to " + str(bw) + " for tuner " + str(tuner_num)
            self._log.exception(error_string)
            raise FRONTEND.BadParameterException(error_string)
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
            if self.frontend_tuner_status[tuner_num].rx_object.is_digital():
                self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setEnable(enable)
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
        valid_sr = None
        try:
            if self.frontend_tuner_status[tuner_num].rx_object.is_digital():
                valid_sr = self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.get_valid_sample_rate(sr, 0)
                self.frontend_tuner_status[tuner_num].rx_object.digital_rx_object.object.setSampleRate(valid_sr)
            if self.frontend_tuner_status[tuner_num].rx_object.is_spectral_scan():
                startFreq = self.frontend_tuner_status[tuner_num].center_frequency - sr /2.0
                stopFreq = self.frontend_tuner_status[tuner_num].center_frequency + sr /2.0
                spcconfig = properties.props_to_dict(self.query(properties.props_from_dict({"msdd_spc_configuration": {}})))
                spcconfig["msdd_spc_configuration"]["msdd_spc_configuration::start_frequency"] = startFreq
                spcconfig["msdd_spc_configuration"]["msdd_spc_configuration::stop_frequency"] = stopFreq
                self.configure(properties.props_from_dict(spcconfig))
                valid_sr = True
            else:
                valid_sr = 0
            if valid_sr == None:
                raise Exception("")
        except Exception, e:
            error_string = "Error setting sr to " + str(sr) + " for tuner " + str(tuner_num)
            self._log.error(error_string)
            self._log.exception(str(e))
            raise FRONTEND.BadParameterException(error_string)
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
        self.device_rf_flow = pkt.rf_flow_id
        #self.update_rf_flow_id(pkt.rf_flow_id)
  
if __name__ == '__main__':
    logging.getLogger().setLevel(logging.INFO)
    logging.debug("Starting Device")
    start_device(MSDD_i)

