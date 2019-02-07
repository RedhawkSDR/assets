#!/usr/bin/python
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
"""
Provides command & control of an MMS MSDD-X000 series radio 

This module allows to command and control an MSDD radio and can handle 
both wideband and narrrowband firmware loads.    The MSDDradio class 
attempts yo probe the radio for the firmware load name and looks for WB_
or NB_ prefixes.    Based upon that info variable numbers of RF, DDC and 
OUT MSDD modules are instantiated.    These MSDD modules are then accessed
directly to C&C the radio.

"""

__author__ = 'REDHAWK'
__version__= '2.0'


import sys
import socket
import string
import unittest
import random
import thread
import math
import inspect 
import re
import pprint
import time


def create_csv(val_list):
    dv = ""
    for i in range(0,len(val_list)):
        dv += str(val_list[i]) + ','
    return str(dv).rstrip(',')  


class sockCom(object):
    """Class to actually communicate with the radio, NB there should only be one of these per radio"""

    #local debug
    __debug = False
#    __debug = True
    __mutexLock = thread.allocate_lock()

    def __init__(self, address, timeout=0.25):
        self.radioAddress = address
        #generate socket for sending/receiving UDP packets to/from radio
        self.radioSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.radioSocket.settimeout(timeout)
    def update_timeout(self, timeout):
        self.radioSocket.settimeout(timeout)
        
    def flush(self):
        self.__mutexLock.acquire()
        self.radioSocket.sendto("\n", self.radioAddress)
        while True:
            try:
                self.radioSocket.recv(65535)
            except socket.timeout:
                self.__mutexLock.release()
                return
    
    def sendStringCommand(self, command, check_for_output=True, expect_output=True):
        """Sends string directly to radio and waits for response"""
        self.__mutexLock.acquire()
        #DELETE ME
        if self.__debug:
            print "Sending to radio: " + str(command).rstrip('\n')
        self.radioSocket.sendto(command, self.radioAddress)
      
        #The following lines were added to avoid a condition where the radio will reset back to turning Echo On and messing up
        # It has to do with additional clients hitting the radio externally, which re-use connections (0-15) on the radio
        # When the current connection is the last on the list, it gets reallocated to the new connection, and the device gets
        # a new connection when it tries to send data, but the echo is turned back on on the new socket
        try:
            echoMsg = self.radioSocket.recv(65535)
            echoMsg = str(echoMsg).rstrip('\n')
            if self.__debug:
                print "Got echo from radio : " + echoMsg
        except socket.timeout: 
            print "Error receiving echo from radio"

        returnMsg=""
        if not check_for_output:
            self.__mutexLock.release()
            return returnMsg
        
        try:
            returnMsg = self.radioSocket.recv(65535)
            returnMsg = str(returnMsg).rstrip('\n')
            if self.__debug:
                print "Got from radio  : " + returnMsg    
            
            if not expect_output and len(returnMsg) > 0:
                err_str =  "Unexpected output received \"" + str(returnMsg).rstrip('\n') + "\" after command: " + str(command).rstrip('\n')
                if self.__debug:
                    print err_str
                self.__mutexLock.release()
                self.flush()
                raise Exception(err_str)
        except socket.timeout:
            if expect_output:
                if self.__debug:
                    print "Socket timed out... attempting to continue but radio may not be responsive"
                self.__mutexLock.release()
                raise Exception("socket timed out")

        self.__mutexLock.release()
        return returnMsg
    
 
#end class socKCom







class baseModule(object):

    """Base module class for all MSDD modules"""
    MOD_NAME_MAPPING={1:None,2:None}
    
    def __init__(self, com, channel_number=0, mapping_version=2):
        self.sock = com
        self.channel_number = channel_number
        self.mapping_version = mapping_version
        self.update_mapping_version(self.mapping_version)
        self.full_reg_name = str(self.module_name) + ":" + str(self.channel_number)
        self.channel_cmd_str = ""
        if channel_number > 0:
            self.channel_cmd_str= ":" + str(channel_number)
        
    def get_full_reg_name(self):
        return self.full_reg_name
    def get_channel_number(self):
        return self.channel
    def get_module_name(self, mapping_version):
        if not self.MOD_NAME_MAPPING.has_key(int(mapping_version)):
            raise NotImplementedError, "MODULE NAME CORRESPONDING TO VERSION " + str(mapping_version) + "DOES NOT EXIST"
        if self.MOD_NAME_MAPPING[int(mapping_version)] == None:
            raise NotImplementedError, "MODULE NAME CORRESPONDING TO VERSION " + str(mapping_version) + " IS EMPTY!"
        return self.MOD_NAME_MAPPING[int(mapping_version)]    
    
    
    def update_mapping_version(self, mapping_version):
        self.module_name = self.get_module_name(mapping_version)
        self.full_reg_name = str(self.module_name) + ":" + str(self.channel_number)

    def create_range_string(self,min,max,step,sep="::"):
        return str(min) + str(sep) +  str(step) + str(sep) +  str(max)
    def create_csv(self,val_list):
        dv = ""
        for i in range(0,len(val_list)):
            dv += str(val_list[i]) + ','
        return str(dv).rstrip(',')  
    def create_desc_val_csv(self, desc_list, val_list):
        dv = ""
        for i in range(0,len(desc_list)):
            dv += str(desc_list[i]) + ":" + str(val_list[i]) + ','
        return str(dv).rstrip(',')    

    def send_custom_command(self,command_str, check_for_output=True):
        res = self.sock.sendStringCommand(str(command_str),check_for_output=check_for_output)
        return res
    
    def make_command(self,query_bool, command_str, arg1_str = "" ):
        query=""
        if query_bool: query="?"
        command_str = command_str.strip()
        arg1_str = arg1_str.strip();
        return str(self.module_name) + str(self.channel_cmd_str) + " " + str(command_str) + str(query)   + " " + str(arg1_str)+ "\n"
    def send_query_command(self,command_str,arg1_str="", check_for_output=True):
        command=self.make_command(True,command_str,arg1_str)
        res = self.sock.sendStringCommand(command,check_for_output=check_for_output)
        return res
    def send_set_command(self,command_str, arg1_str="", check_for_output=False):
        command=self.make_command(False,command_str,arg1_str)
        res = self.sock.sendStringCommand(command,check_for_output=check_for_output)
        return res
    
    def getter_with_default(self,_get_,default_value=None):
        try:
            ret = _get_()
            return ret
        except:
            if default_value != None:
                return default_value
            raise
    
    def setter_with_validation(self, value, _set_, _get_, tolerance_range=None):
        successful_validation = False
        counter = 0
        
        while successful_validation == False:
            try:
                _set_(value)
                ret = _get_()
            except NotImplementedError:
                return True 
            except Exception:
                self.sock.flush()
                return False 
            except:
                return False
            
            if tolerance_range != None:
                successful_validation = (value >= ret - tolerance_range and value <= ret + tolerance_range)
            else:
                successful_validation = (value == ret)
            
            counter += 1
            if successful_validation or counter > 40:
                return successful_validation
            time.sleep(0.01)    
                
        return successful_validation
        
    
                
        
    def parseResponse(self, resp, items = -1, sep=','):
        """Parses a string response from the radio and returns #items elements"""
        #strip trailing newline, split on spaces and take last element, then split on commas
        resp = str(resp).strip()
        resp_split_ws = resp.split()
        if len(resp_split_ws) < 2:
            raise Exception("UNKNOWN ERROR HAS BEEN DETECTED: " + str(resp))
        if resp_split_ws[len(resp_split_ws)-2] == "ERR":
            raise Exception("ERROR HAS BEEN DETECTED: " + str(resp))
        
        retVal = resp_split_ws[len(resp_split_ws)-1]
        retVal_list = string.rsplit(retVal,sep,items) 
        if items >= 0 and len(retVal_list) != items:
            raise Exception("COULD NOT PARSE OUTPUT \"" + str(resp) + "\"AS DESIRED.")
        return retVal_list

    def parseResponseSpaceOnly(self, resp, items = -1):
        """Parses a string response from the radio and returns #items elements"""
        #strip trailing newline, split on spaces 
        resp = str(resp).strip()
        resp_split_ws = resp.split()
        if len(resp_split_ws) < 2:
            raise Exception("UNKNOWN ERROR HAS BEEN DETECTED: " + str(resp))
        if resp_split_ws[len(resp_split_ws)-2] == "ERR":
            raise Exception("ERROR HAS BEEN DETECTED: " + str(resp))      
        if  len(resp_split_ws) != items:
            raise Exception("COULD NOT PARSE OUTPUT \"" + str(resp) + "\"AS DESIRED.")
        return resp_split_ws
    
    def get_value_valid(self,value,tolerance_per, min_val, max_val, step_val=1,closest_ceil=False):
        if step_val <= 0:
            step_val = 0.25
        valid = False
        next_highest_valid = max(min_val,min(max_val,math.ceil((value-min_val)/step_val) * step_val+min_val))
        val = next_highest_valid
        max_tolerance_val = value + value*(tolerance_per/100.0)
        max_valid_val = math.ceil((max_tolerance_val-min_val)/step_val) * step_val+min_val
        iterate = (val <= max_val and max_valid_val >= min_val)
        while iterate:
            if val > max_val:
                break
            if val > max_valid_val:
                break
            if val >= value and val <= max_val and val >= min_val:
                valid = True
                break            
            val += step_val
        if not valid and closest_ceil:
            return next_highest_valid
        if not valid:
            raise Exception("Valid value not found! Requested Value was %s" %value)
        
        return val
    
    def get_value_valid_list(self,value,value_tolerance_perc, list):
        sorted_list = sorted(list)
        max_tolerance_val = value + value*(value_tolerance_perc/100.0)
        for num in range(0,len(sorted_list)):
            if sorted_list[num] < value:
                continue
            if sorted_list[num] >= value and sorted_list[num] <= max_tolerance_val:
                return sorted_list[num]
            if sorted_list[num] > max_tolerance_val:
                continue
            
        raise Exception("Valid value not found! Requested Value was %s" %value)
        
#end class baseModule

class ConsoleModule(baseModule):
    MOD_NAME_MAPPING={1:"CON",2:"CON"}
    
    def __init__(self, com, channel_number=0, mapping_version=2):
        baseModule.__init__(self,com, channel_number,mapping_version)
        #self.setEcho(False)
        self.setEcho(True)
        
    def make_command(self,query_bool, command_str, arg1_str = "" ):
        query=""
        if query_bool: query="?"
        command_str = command_str.strip()
        arg1_str = arg1_str.strip();
        return str(command_str) + str(query)   + " " + str(arg1_str)+ "\n"
    
    #Generic Radio commands                
    def cmdPing(self):
        try:
            self.getIPP()
        except:
            return False
        return True

    def getIPP(self):
        """Queries the radio for IP and Port """
        resp = self.send_query_command("IPP")
        try:
            response = self.parseResponse(resp, 2,':')
        except:
            response = self.parseResponse(resp, 3,':')[-2:]
        return response   
    def getIPPString(self):
        resp = self.send_query_command("IPP")
        return str(self.parseResponse(resp, 1)[0])
    def getIPP_IP(self):
        return self.getIPP()[0];
    def getIPP_Port(self):
        return self.getIPP()[1];
    
    def getID(self):
        """Queries the radio for ID info, (Model,SN?,Load) """
        resp = self.send_query_command("IDN")
        return self.parseResponse(resp, 3)
    def getIDString(self):
        resp = self.send_query_command("IDN")
        return str(self.parseResponse(resp, 1)[0])
    def getSoftwarePartNumber(self):
        return self.getID()[2];
    
    
    def setEcho(self, value):
        """Controls the echo response of the radio, boolean on/off"""
        arg=str(int(value))
        try:
            self.send_set_command("ECH",arg,True)
        except:
            None
        return value == self.getEcho()
            
    def getEcho(self):
        """Gets the echo response of the radio, boolean on/off"""
        resp = self.send_query_command("ECH")
        return int(self.parseResponse(resp, 1)[0]) == 1
    
    def getCpuLoad(self):
        """Gets CPU load in percent, updated every 500ms """
        resp = self.send_query_command("CPL")
        return float(self.parseResponse(resp, 1)[0])
    
    def getCfg(self, config_type):
        resp = self.send_query_command("CFG",str(config_type))
        return self.parseResponse(resp, 1)
    
    def getModel(self):
        return self.getCfg('MODEL')[0]
    def getSerial(self):
        return self.getCfg('SERIAL')[0]
    def getMinFreq_Hz(self):
        return float(self.getCfg('MIN_FREQ')[0]) *1e6
    def getMaxFreq_Hz(self):
        return float(self.getCfg('MAX_FREQ')[0])*1e6
    def getDspRef_Hz(self):
        return float(self.getCfg('DSP_REF')[0])
    def getAdcClk_Hz(self):
        return float(self.getCfg('ADC_CLK')[0])
    def getRfBoardType(self):
        return self.getCfg('RF_BRD_TYPE')[0]
    def getNumIFPorts(self):
        return int(self.getCfg('IF_PORTS')[0])
    def getHasBitAdc(self):
        return (int(self.getCfg('BIT_SADC')[0]) == 1)
    def getNumEthPorts(self):
        return int(self.getCfg('ETHR_PORTS')[0])
    def getCpuType(self):
        return self.getCfg('CPU_TYPE')[0]
    def getCpuFreq(self):
        return float(self.getCfg('CPU_FREQ')[0])
    def getFpgaType(self):
        return self.getCfg('FPGA_TYPE')[0]
    def getDspBoardType(self):
        return int(self.getCfg('DSP_BRD_TYPE')[0])
    def get1PPSTerm(self):
        return (int(self.getCfg('1PPS_TERM')[0]) == 0)
    def get1PPSVoltage(self):
        return float(self.getCfg('1PPS_VOLTAGE')[0])
    def getNumFpgaWBDDCChannels(self):
        return int(self.getCfg('FPGA_WBDDC_CHANNELS')[0])
    def getNumFpgaNBDDCChannels(self):
        return int(self.getCfg('FPGA_NBDDC_CHANNELS')[0])
    def getFilenameApp(self):
        return self.getCfg('FILE_NAME_APP')[0]
    def getFilenameFpga(self):
        return self.getCfg('FILE_NAME_FPGA')[0]
    def getFilenameBatch(self):
        return self.getCfg('FILE_NAME_BATCH')[0]
    def getFilenameBoot(self):
        return self.getCfg('FILE_NAME_BOOT')[0]
    def getFilenameLoader(self):
        return self.getCfg('FILE_NAME_LOADER')[0]
    def getFilenameConfig(self):
        return self.getCfg('FILE_NAME_CONFIG')[0]
    def getFilenameCal(self):
        return self.getCfg('FILE_NAME_CAL')[0]


    """Accessable properties"""    
    ID = property(getID, doc="Model/Serial/Load Number")
    sw_part_number = property(getSoftwarePartNumber, doc="Software Part Number ")
    echo = property(getEcho,setEcho, doc="Echo Status")
    cpu_load = property(getCpuLoad, doc="CPU Load")
    model = property(getModel,doc="Model name, example MSDD3000-PPS")
    serial = property(getSerial,doc="MSDD serial number")
    minFreq_Hz = property(getMinFreq_Hz,doc="Min tune frequency")
    maxFreq_Hz = property(getMaxFreq_Hz,doc="Max tune frequency")
    dspRef_Hz = property(getDspRef_Hz,doc="DSP reference frequency")
    adcClk_Hz = property(getAdcClk_Hz,doc="ADC clock rate")
    rf_board_type = property(getRfBoardType,doc="RF board type, example MSDR3000")
    num_if_ports = property(getNumIFPorts,doc="Number of IF inputs on DSP board, this will be the same as number of receivers")
    bit_adc_exists = property(getHasBitAdc,doc="0 if the DSP board has no BIT ADC, 1 if there is a BIT ADC")
    num_eth_ports = property(getNumEthPorts,doc="Number of Ethernet ports on DSP board")
    cpu_type = property(getCpuType,doc="DSP CPU type, example  TMS320C6455")
    cpu_rate = property(getCpuFreq,doc="DSP CPU clock rate")
    fpga_type = property(getFpgaType,doc="FPGA type, example EP2C50")
    dsp_type = property(getDspBoardType,doc="DSP board type normally a 0, new boards will be 1")
    pps_termination = property(get1PPSTerm,doc="Default for 1PPS termination if available. 0 = on, 1 = off")
    pps_voltage = property(get1PPSVoltage,doc="Default 1PPS voltage selection if available, 5 or 3.3")
    num_wb_channels = property(getNumFpgaWBDDCChannels,doc="Number of WB channels in the FPGA")
    num_nb_channels = property(getNumFpgaNBDDCChannels,doc="Number of NB channels in the FPGA")
    filename_app = property(getFilenameApp,doc="File name of the application")
    filename_fpga = property(getFilenameFpga,doc="File name of the FPGA")
    filename_batch = property(getFilenameBatch,doc="File name of the Batch")
    filename_boot = property(getFilenameBoot,doc="File name of the Boot program")
    filename_loader = property(getFilenameLoader,doc="File name of the Loader Program")
    filename_config = property(getFilenameConfig,doc="File name of the Config file")
    filename_cal = property(getFilenameCal,doc="File name of the Cal file")
             
#end class ConsoleModule

class LOGModule(baseModule):
    MOD_NAME_MAPPING={1:"LOG",2:"LOG"}
    
    # Logging Module commands
    def _setIPP(self,ipp):
        self.send_set_command("IPP ",str(ipp))
    def getIPP(self):
        resp = self.send_query_command("IPP")
        return self.parseResponse(resp, 1)[0]
    def setIPP(self,ipp):
        return self.setter_with_validation(str(ipp), self._setIPP, self.getIPP)
        
    
    def _setMask(self, mask):
        self.send_set_command("MSK",str(mask))
    def getMask(self):
        resp = self.send_query_command("MSK")
        return int(self.parseResponse(resp, 1)[0])
    def getValidMasks(self):
        resp = self.send_query_command("MSKL")
        return self.parseResponse(resp)
    def setMask(self, mask):
        return self.setter_with_validation(int(mask), self._setMask, self.getMask)

    def _setLogChannelEnable(self, enable):
        arg="0" 
        if enable: arg="1"
        self.send_set_command("ENB",arg)
    def getLogChannelEnable(self):
        resp = self.send_query_command("ENB")
        return (int(self.parseResponse(resp, -1,':')[0]) == 1)
    def setLogChannelEnable(self, enable):
        return self.setter_with_validation(enable, self._setLogChannelEnable, self.getLogChannelEnable)
    

    """Accessable properties"""
    ipp = property(getIPP, setIPP, doc="Logging module IP and UDP port")
    mask = property(getMask, setMask, doc="Logging Mask value")
    enable = property(setLogChannelEnable, getLogChannelEnable, doc="Enable/Disable log channel")
    
#end class LOGModule



class BoardModule(baseModule):
    MOD_NAME_MAPPING={1:"BRD",2:"BRD"}
    
    def getMeterVal(self):
        resp = self.send_query_command("MTR")
        return self.parseResponse(resp)[0] 
    def getMeterList(self):
        resp = self.send_query_command("MTRL")
        return self.parseResponse(resp)   
    def getBIT(self):
        resp = self.send_query_command("BIT")
        return int(self.parseResponse(resp, 1)[0]) 
    def getBITList(self):
        resp = self.send_query_command("BITL")
        return self.parseResponse(resp)
    
    def _setExternalRef(self, ref):
        self.send_set_command("EXR",str(ref))
    def getExternalRef(self):
        resp = self.send_query_command("EXR\n")
        return int(self.parseResponse(resp, 1)[0]) 
    def getExternalRefList(self):
        resp = self.send_query_command("EXRL")
        return self.parseResponse(resp)
    def setExternalRef(self, ref):
        return self.setter_with_validation(int(ref), self._setExternalRef, self.getExternalRef)
    
    
    """Accessable properties"""    
    BIT = property(getBIT, doc="Built in test results")
    meter = property(getMeterVal, doc="Get Meter Value")
    extRef = property(getExternalRef, setExternalRef, doc="Only on MSDD0660D")
    


class NetModule(baseModule):
    MOD_NAME_MAPPING={1:"NET",2:"NET"}
        
    def _setIpAddr(self, ip_address):
        self.send_set_command("IPP",str(ip_address))
    def getIpAddr(self):
        resp = self.send_query_command("IPP")
        return self.parseResponse(resp,1)[0]
    def setIpAddr(self, ip_address):
        return self.setter_with_validation(str(ip_address), self._setIpAddr, self.getIpAddr)
    
    def getMacAddr(self):
        resp = self.send_query_command("MAC")
        return self.parseResponse(resp,1)[0] 
    def getNwEnable(self):
        resp = self.send_query_command("ENB")
        return (int(self.parseResponse(resp, -1,':')[0]) == 1)   
    def getNwBitRate(self):
        resp = self.send_query_command("BRT")
        return float(self.parseResponse(resp, 1)[0]) 
    
    ip_addr = property(getIpAddr, setIpAddr, doc="")
    mac_addr = property(getMacAddr, doc="")
    network_enabled = property(getNwEnable, doc="")
    network_bit_rate = property(getNwBitRate, doc="")
    


class RcvBaseModule(baseModule):
    """Interface for the MSDD RCV module"""
    MOD_NAME_MAPPING={1:"RCV",2:"RCV"}
    
    
    def _setFrequency_Hz(self, freq):
        freqMHz=freq/1e6
        self.send_set_command("FRQ ",str(freqMHz))
    def getFrequency_Hz(self):
        resp = self.send_query_command("FRQ")
        return float(self.parseResponse(resp, 1)[0])*1e6   
    def getFrequencyList(self):
        resp = self.send_query_command("FRQL")
        return self.parseResponse(resp, 3,':')
    def getFrequencyListStr(self):
        resp = self.send_query_command("FRQL")
        rl = self.parseResponse(resp, 3,':')
        return self.create_range_string(float(rl[0])*1e6, float(rl[1])*1e6, float(rl[2])*1e6)
    def get_valid_frequency(self,center_frequency_hz,center_frequency_tolerance_perc=.01):
        return self.get_value_valid(center_frequency_hz, center_frequency_tolerance_perc, self.min_frequency_hz, self.max_frequency_hz, self.step_frequency_hz)

    def getMinFrequency_Hz(self):
        return float(self.getFrequencyList()[0])*1e6
    def getMaxFrequency_Hz(self):
        return float(self.getFrequencyList()[1])*1e6
    def getStepFrequency_Hz(self):
        return float(self.getFrequencyList()[2])*1e6
    def setFrequency_Hz(self, frequency_hz):
        return self.setter_with_validation(float(frequency_hz), self._setFrequency_Hz, self.getFrequency_Hz,100)
    
    def _setAttenuation(self, attn):
        self.send_set_command("ATN",str(attn))
    def getAttenuation(self):
        resp = self.send_query_command("ATN")
        return float(self.parseResponse(resp, 1)[0])    
    def getAttenuationList(self):
        resp = self.send_query_command("ATNL")
        return self.parseResponse(resp, 3,':')
    def getAttenuationListStr(self):
        resp = self.send_query_command("ATNL")
        rl = self.parseResponse(resp, 3,':')
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def getMinAttenuation(self):
        return float(self.getFrequencyList()[0])
    def getMaxAttenuation(self):
        return float(self.getFrequencyList()[1])
    def getStepAttenuation(self):
        return float(self.getFrequencyList()[2])
    def setAttenuation(self, attn):
        return self.setter_with_validation(float(attn), self._setAttenuation, self.getAttenuation,1)
    
    def _setGain(self, gain):
        self.send_set_command("GAI",str(gain))
    def getGain(self):
        resp = self.send_query_command("GAI")
        return float(self.parseResponse(resp, 1)[0])    
    def getGainList(self):
        resp = self.send_query_command("GAIL")
        return self.parseResponse(resp, 3,':')
    def getGainListStr(self):
        resp = self.send_query_command("GAIL")
        rl = self.parseResponse(resp, 3,':')
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def getMinGain(self):
        return float(self.getFrequencyList()[0])
    def getMaxGain(self):
        return float(self.getFrequencyList()[1])
    def getStepGain(self):
        return float(self.getFrequencyList()[2])
    def setGain(self, gain):
        return self.setter_with_validation(float(gain), self._setGain, self.getGain,1)    
    def get_valid_gain(self,gain):
        gl = self.getGainList()
        return self.get_value_valid(gain, 0, float(gl[0]), float(gl[1]), float(gl[2]),False)
    
    def getBIT(self):
        resp = self.send_query_command("BIT")
        return int(self.parseResponse(resp, 1)[0])        
    def getBITList(self):
        resp = self.send_query_command("BITL")
        return self.parseResponse(resp)
    def getADM(self):
        resp = self.send_query_command("ADM")
        return self.parseResponse(resp)        
    def getADMList(self):
        resp = self.send_query_command("ADML")
        return self.parseResponse(resp) 
    # LEAVING OUT CALIBRATION OVERRIDES FOR NOW!!    
    def getMTR(self):
        resp = self.send_query_command("MTR")
        return self.parseResponse(resp)
    def getMTRList(self):
        resp = self.send_query_command("MTRL")
        return self.parseResponse(resp)
    def getADCSampleCount(self):
        return float(self.getMTR()[0])
    def getADCOverflowCount(self):
        return float(self.getMTR()[1])
    def getADCAbsVal(self):
        return float(self.getMTR()[2])
    def getADCListStr(self):
        return self.create_desc_val_csv(self.getADMList(),self.getADM())
    
    
    def _setBandwidth_Hz(self, bw):
        raise NotImplementedError(str(inspect.stack()[0][3]))
    def getBandwidth_Hz(self):
        raise NotImplementedError(str(inspect.stack()[0][3]))
    def getBandwidthList_Hz(self):
        raise NotImplementedError(str(inspect.stack()[0][3]))
    def getBandwidthListStr(self):
        res_list = self.getBandwidthList_Hz()
        return self.create_csv(res_list)
    def get_valid_bandwidth(self,bandwidth_hz,bandwidth_tolerance_perc):
        bw_list = self.getBandwidthList_Hz()
        return self.get_value_valid_list(bandwidth_hz,bandwidth_tolerance_perc, bw_list)
    def setBandwidth_Hz(self, bw):
        retVal = True
        try:
            retVal =  self.setter_with_validation(float(bw), self._setBandwidth_Hz, self.getBandwidth_Hz,100)
        except NotImplementedError:
            retVal = True
        except:
            retVal = False
        return retVal 
    
    
    
    """Accessable properties"""
    frequency_hz = property(getFrequency_Hz,setFrequency_Hz, doc="Center Frequency in Hz")
    min_frequency_hz = property(getMinFrequency_Hz, doc="Min Center Frequency in Hz")
    max_frequency_hz = property(getMaxFrequency_Hz, doc="Max Center Frequency in Hz")
    step_frequency_hz = property(getStepFrequency_Hz, doc="Step Center Frequency in Hz")
    available_frequency_hz =  property(getFrequencyListStr, doc="Available Frequency in min::step::max")
    
    attenuation = property(getAttenuation,setAttenuation, doc="Attenuation in db")
    min_attenuation = property(getMinAttenuation, doc="Min Attenuation in db")
    max_attenuation = property(getMaxAttenuation, doc="Max Attenuation in db")
    step_attenuation = property(getStepAttenuation, doc="Step Attenuation in db")
    available_attenuation =  property(getAttenuationListStr, doc="Available Attenuation in min::step::max")
    
    bandwidth_hz = property(getBandwidth_Hz,setBandwidth_Hz , doc="Bandwidth in Hz")
    valid_bandwidth_list_hz = property(getBandwidthList_Hz)
    available_bandwidth_hz =  property(getBandwidthListStr, doc="Available Bandwidths")
    
    
    gain = property(getGain,setGain, doc="Gain in db")
    min_gain = property(getMinGain, doc="Min Gain in db")
    max_gain = property(getMaxGain, doc="Max Gain in db")
    step_gain = property(getStepGain, doc="Step Gain in db")
    available_gain =  property(getGainListStr, doc="Available Gain in min::step::max")
    
    bit = property(getBIT, doc="Bit state of the rx")
    adc_meter_vals= property(getMTR, doc="")
    adc_sample_count = property(getADCSampleCount, doc="")
    adc_overflow_count = property(getADCOverflowCount, doc="")
    adc_abs_val = property(getADCAbsVal, doc="")
    adc_meter_str = property(getADCListStr, doc="")


class RS422_RcvModule(RcvBaseModule):
    MOD_NAME_MAPPING={1:"RCV",2:"RCV"}
   
    def _setExternalRef(self, ref):
        self.send_set_command("EXR ",str(ref))
    def getExternalRef(self):
        resp = self.send_query_command("EXR")
        return int(self.parseResponse(resp, 1)[0])
    def getExternalRefList(self):
        resp = self.send_query_command("EXRL")
        return self.parseResponse(resp, 3,':')
    def getMinExternalRef(self):
        return int(self.getFrequencyList()[0])
    def getMaxExternalRef(self):
        return int(self.getFrequencyList()[1])
    def getStepExternalRef(self):
        return int(self.getFrequencyList()[2])
    def setExternalRef(self, ref):
        return self.setter_with_validation(int(ref), self._setExternalRef, self.getExternalRef)
 
    def _setBandwidth_Hz(self, bw):
        try:
            rl = self.getBandwidthList_Hz()
            srl = self.getBandwidthList_Hz()
            srl.sort()
            bwPos = max(0,len(rl)-1)
            for x in srl:
                if bw <= x:
                    bwPos = rl.index(x)
                    break;
            self.send_set_command("BWC",str(bwPos))
        except Exception,e:
            print e
    def getBandwidth_Hz(self):
        resp = self.send_query_command("BWC")
        pos = int(self.parseResponse(resp,1)[0])
        rl = self.getBandwidthList_Hz()
        return rl[pos]
    
    def getBandwidthList_Hz(self):
        resp = self.send_query_command("BWCL")
        respList = self.parseResponse(resp)
        hz_List = []
        for i in range(0,len(respList)):
            hz_List.append(float(respList[i])*1e3)
        return hz_List
    def getBandwidthListStr(self):
        res_list = self.getBandwidthList_Hz()
        return self.create_csv(res_list)
    def get_valid_bandwidth(self,bandwidth_hz,bandwidth_tolerance_perc):
        bw_list = self.getBandwidthList_Hz()
        return self.get_value_valid_list(bandwidth_hz,bandwidth_tolerance_perc, bw_list)
    def setBandwidth_Hz(self, bw):
        retVal = True
        try:
            retVal =  self.setter_with_validation(float(bw), self._setBandwidth_Hz, self.getBandwidth_Hz,100)
        except NotImplementedError:
            retVal = True
        except:
            retVal = False
        return retVal 

    def _setLoMode(self, mode):
        self.send_set_command("MSM",str(mode))
    def getLoMode(self):
        resp = self.send_query_command("MSM")
        return int(self.parseResponse(resp, 1)[0])  
    def getLoModeList(self):
        resp = self.send_query_command("MSML")
        return self.parseResponse(resp)
    def setLoMode(self, mode):
        return self.setter_with_validation(int(mode), self._setLoMode, self.getLoMode)


    bandwidth_hz = property(getBandwidth_Hz,setBandwidth_Hz , doc="Bandwidth in Hz")
    valid_bandwidth_list_hz = property(getBandwidthList_Hz)
    lo_mode = property(setLoMode,getLoMode , doc="Lo Mode")

    external_ref = property(getExternalRef,setExternalRef, doc="")
    min_external_ref = property(getMinExternalRef, doc="")
    max_external_ref = property(getMaxExternalRef, doc="")
    step_external_ref = property(getStepExternalRef, doc="")
    

class MSDDX000_RcvModule(RcvBaseModule):
    MOD_NAME_MAPPING={1:"RCV",2:"RCV"}
    MSDD_X000_BANDWIDTH=20000000.0
    MSDD_X000EX_BANDWIDTH=40000000.0
    
    def _setExternalRef(self, ref):
        self.send_set_command("EXR ",str(ref))
    def getExternalRef(self):
        resp = self.send_query_command("EXR")
        return int(self.parseResponse(resp, 1)[0])    
    def getExternalRefList(self):
        resp = self.send_query_command("EXRL")
        return self.parseResponse(resp, 3,':')
    def getMinExternalRef(self):
        return int(self.getFrequencyList()[0])
    def getMaxExternalRef(self):
        return int(self.getFrequencyList()[1])
    def getStepExternalRef(self):
        return int(self.getFrequencyList()[2])
    def setExternalRef(self, ref):
        return self.setter_with_validation(int(ref), self._setExternalRef, self.getExternalRef)    

    def setBandwidth_Hz(self, bw):
        return True
        #return (bw == self.MSDD_X000_BANDWIDTH)
    def getBandwidth_Hz(self):
        return self.MSDD_X000EX_BANDWIDTH
    def getBandwidthList_Hz(self):
        return [self.MSDD_X000_BANDWIDTH,self.MSDD_X000EX_BANDWIDTH ]
    def get_valid_bandwidth(self,bandwidth_hz,bandwidth_tolerance_perc):
        bw_list = self.getBandwidthList_Hz()
        return self.get_value_valid_list(bandwidth_hz,bandwidth_tolerance_perc, bw_list)
        
    bandwidth_hz = property(getBandwidth_Hz,setBandwidth_Hz , doc="Bandwidth in Hz")
    valid_bandwidth_list_hz = property(getBandwidthList_Hz)
    
    external_ref = property(getExternalRef,setExternalRef, doc="")
    min_external_ref = property(getMinExternalRef, doc="")
    max_external_ref = property(getMaxExternalRef, doc="")
    step_external_ref = property(getStepExternalRef, doc="")

#end class RFModule



class TODModule(baseModule):
    MOD_NAME_MAPPING={1:"TOD",2:"TOD"}
    #Timing Module commands                
    def getBIT(self):
        """Get Built In Test results    N.B. 0-4 are errors, all else are OK"""
        resp = self.send_query_command("BIT")
        return int(self.parseResponse(resp, 1)[0]) 
    def getBITList(self):
        """Get Built In Test results    N.B. 0-4 are errors, all else are OK"""
        resp = self.send_query_command("BITL")
        return self.parseResponse(resp)
    def getBITStr(self):
        bit = self.getBIT()
        bitList = self.getBITList()
        if bit < 0 or bit >= len(bitList):
            return "NO ERRORS"
        return str(bitList[bit]) 
    
    
    def setTime(self, seconds):
        """Set Time of Day in seconds, latches on next PPS"""
        self.send_set_command("SET ",str(seconds))
        return True
    
    def getTime(self):
        """Get Time of Day in [secconds,ns]"""
        resp = self.send_query_command("GET")
        return float(self.parseResponse(resp, 1)[0]) 
    
    
    def _setMode(self, mode):
        """set TOD Module mode (0=Sim, 1=1PPS, 2=IRIG-B)"""
        self.send_set_command("MOD",str(mode))
    def getMode(self):
        """Get TOD Module mode (0=Sim, 1=1PPS, 2=IRIG-B)"""
        resp = self.send_query_command("MOD")
        return int(self.parseResponse(resp, 1)[0]) 
    def getModeList(self):
        resp = self.send_query_command("MODL")
        return self.parseResponse(resp)
    def getModeListStr(self):
        res_list = self.getModeList()
        return self.create_csv(res_list)
    def getModeStr(self):
        mode = self.getMode()
        modeList = self.getModeList()
        return str(modeList[mode])
    def getModeNumFromString(self, mode_string):
        modeList = self.getModeList()
        for i in range(0,len(modeList)):
            if modeList[i] == mode_string:
                return i
        return 0
    def _setModeStr(self, mode_string):
        mode_num = self.getModeNumFromString(mode_string)
        self._setMode(mode_num)
    def setMode(self, mode):
        return self.setter_with_validation(int(mode), self._setMode, self.getMode)    
    def setModeStr(self, mode_string):
        return self.setter_with_validation(str(mode_string), self._setModeStr, self.getModeStr)    

    
    def getMeterVal(self):
        resp = self.send_query_command("MTR")
        return self.parseResponse(resp) 
    def getMeterList(self):
        resp = self.send_query_command("MTRL")
        return self.parseResponse(resp)   
    def getMeterListStr(self):
        return self.create_desc_val_csv(self.getMeterList(),self.getMeterVal())
    
    def getPPSVolt(self):
        resp = self.send_query_command("VOL")
        return float(self.parseResponse(resp)[0])
    def _setPPSVolt(self, voltage):
        self.send_set_command("VOL",str(voltage))
    def setPPSVolt(self, voltage):
        return self.setter_with_validation(float(voltage), self._setPPSVolt, self.getPPSVolt)    
         
    def getReferenceAdjust(self):
        resp = self.send_query_command("RFA")
        return float(self.parseResponse(resp)[0])
    def _setReferenceAdjust(self, refAdj_ns_per_sec):
        self.send_set_command("RFA",str(refAdj_ns_per_sec))
    def setReferenceAdjust(self, refAdj_ns_per_sec):
        return self.setter_with_validation(float(refAdj_ns_per_sec), self._setReferenceAdjust, self.getReferenceAdjust)    
    
    def getReferenceTrack(self):
        resp = self.send_query_command("RTK")
        return int(self.parseResponse(resp)[0]) 
    def _setReferenceTrack(self, track_mode):
        self.send_set_command("RTK",str(track_mode))
    def setReferenceTrack(self, track_mode):
        return self.setter_with_validation(int(track_mode), self._setReferenceTrack, self.getReferenceTrack)    
        
    def getSubSecMismatchFault(self):
        resp = self.send_query_command("SML")
        return int(self.parseResponse(resp)[0]) 
    def _setSubSecMismatchFault(self, num_samp_clock_cycles):
        self.send_set_command("SML",str(num_samp_clock_cycles))
    def setSubSecMismatchFault(self, num_samp_clock_cycles):
        return self.setter_with_validation(int(num_samp_clock_cycles), self._setSubSecMismatchFault, self.getSubSecMismatchFault)    
    

    def getToy(self):
        resp = self.send_query_command("TOY")
        return int(self.parseResponse(resp)[0]) 
    def _setToy(self, toy):
        self.send_set_command("TOY",str(toy))
    def setToy(self, toy):
        return self.setter_with_validation(int(toy), self._setToy, self.getToy)    
    

    """Accessable properties"""    
    BIT = property(getBIT, doc="Built in test results")
    BIT_string = property(getBITStr)
    time = property(getTime, setTime, doc="Time of day in secconds")
    mode = property(getMode, setMode, doc="Module Mode (0-sim,1-pps,2-irigb)")
    mode_string = property(getModeStr, doc="Module Mode in string format")
    meter_list_string = property(getMeterListStr)
    available_modes = property(getModeListStr)
    pps_voltage = property(getPPSVolt, setPPSVolt, doc="")
    ref_adjust = property(getReferenceAdjust, setReferenceAdjust, doc="")
    ref_track = property(getReferenceTrack, setReferenceTrack, doc="")
    sub_sec_mismatch_fault = property(getSubSecMismatchFault, setSubSecMismatchFault, doc="")
    toy = property(getToy, setToy, doc="")
        
#end class TODModule



class DdcBaseModule(baseModule):
    MOD_NAME_MAPPING={1:"DDC",2:"DDC"}

    """Interface for the MSDD DDC module"""
    def __init__(self, com, channel_number=0, mapping_version=2):
        baseModule.__init__(self, com, channel_number, mapping_version)
        self.rf_offset_hz = 0
    def updateRfFrequencyOffset(self,rf_offset_hz):
        self.rf_offset_hz = float(rf_offset_hz)
    
    def _setFrequency_Hz(self, freq):
        raise NotImplementedError(str(inspect.stack()[0][3]))
    def getFrequency_Hz(self):
        return 0.0
    def getFrequencyList(self):
        return [0.0,0.0,0.0]
    def getMinFrequency_Hz(self):
        return float(self.getFrequencyList()[0])
    def getMaxFrequency_Hz(self):
        return float(self.getFrequencyList()[1])
    def getStepFrequency_Hz(self):
        return float(self.getFrequencyList()[2])
    def getFrequencyListStr(self):
        rl = self.getFrequencyList()
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def get_valid_frequency(self,center_frequency_hz,center_frequency_tolerance_perc=.01):
        return self.get_value_valid(center_frequency_hz, center_frequency_tolerance_perc, self.min_frequency_hz, self.max_frequency_hz, self.step_frequency_hz)
    def setFrequency_Hz(self, freq):
        retVal = True
        try:
            retVal =  self.setter_with_validation(float(freq), self._setFrequency_Hz, self.getFrequency_Hz,1000)
        except NotImplementedError:
            retVal = True
        except:
            retVal = False
        return retVal 

    def _setBandwidth_Hz(self, bandwidth_hz):
        raise NotImplementedError(str(inspect.stack()[0][3]))
    def getBandwidth_Hz(self):
        return self.getSampleRate()
    def getBandwidthList(self):
        return self.getSampleRateList()
    def getBandwidthListStr(self):
        return self.getSampleRateListStr()
    def get_valid_bandwidth(self,bandwidth_hz,bandwidth_tolerance_perc, ibw_override=None):
        return self.get_valid_sample_rate(bandwidth_hz,bandwidth_tolerance_perc,ibw_override)
    def setBandwidth_Hz(self, bandwidth_hz):
        retVal = True
        try:
            retVal =  self.setter_with_validation(float(bandwidth_hz), self._setBandwidth_Hz, self.getBandwidth_Hz,100)
        except NotImplementedError:
            retVal = True
        except:
            retVal = False
        return retVal     
    
    def _setSampleRate(self, sample_rate):
        isr = self.getInputSampleRate()
        sample_rate = self.get_valid_sample_rate(sample_rate,1)
        dec = 1
        if sample_rate >0:
            dec = isr/sample_rate
        self.setDecimation(dec)
        return
    def getSampleRate(self):
        isr = self.getInputSampleRate()
        dec = self.getDecimation()
        return float(isr / dec)
    def getSampleRateList(self, isr_override=None):
        sr_list = []
        isr = isr_override
        if isr == None:
            isr = self.getInputSampleRate()
        dec_list = self.getDecimationList()
        for dec in dec_list:
            sr_list.append(isr/dec)
        sys.stdout.flush()
        return sr_list
    def getSampleRateListStr(self):
        srl = self.getSampleRateList()
        return self.create_csv(srl)
    def get_valid_sample_rate(self,sample_rate,sample_rate_tolerance_perc, isr_override=None):
        sr_list = self.getSampleRateList(isr_override)
        return self.get_value_valid_list(sample_rate,sample_rate_tolerance_perc, sr_list)
    def setSampleRate(self, sample_rate):
        return self.setter_with_validation(float(sample_rate), self._setSampleRate, self.getSampleRate,100)    

    def _setEnable(self, enable, block_xfer_size=0):
        arg="0" 
        if enable: arg="1"
        if block_xfer_size > 0:
            arg+=":" + str(block_xfer_size)
        self.send_set_command("ENB",str(arg))
    def getEnable(self):
        resp = self.send_query_command("ENB")
        return (int(self.parseResponse(resp, -1,':')[0]) == 1)    
    def getEnableBlockLimits(self):
        resp = self.send_query_command("ENBL")
        return self.parseResponse(resp, 3,':')
    def getMinEnableBlockSize(self):
        return int(self.getFrequencyList()[0])
    def getMaxEnableBlockSize(self):
        return int(self.getFrequencyList()[1])
    def getIntEnableBlockSize(self):
        return int(self.getFrequencyList()[2])
    def setEnable(self, enable):
        return self.setter_with_validation(enable, self._setEnable, self.getEnable)
    
    def _setDecimation(self, dec):
        if self.isDecimationByPosition():
            rl = self.getDecimationList()
            posMap = dict(zip(rl,range(len(rl))))
            decListSorted = sorted(rl)
            decPos = 0
            for supportedDec in decListSorted:
                if dec >= supportedDec:
                    decPos = posMap[supportedDec]
                else:
                    break
            self.send_set_command("DEC",str(decPos))
            return
        self.send_set_command("DEC",str(dec))
        
    def getDecimation(self):
        resp = self.send_query_command("DEC")
        dec = float(self.parseResponse(resp, 1)[0]) 
        if self.isDecimationByPosition():
            rl = self.getDecimationList()
            return float(rl[int(dec)])
        return dec
    
    def isDecimationByPosition(self):
        resp = self.send_query_command("DECL")
        rl = self.parseResponse(resp,-1,':');
        if len(rl) == 3:
            return False
        return True
    
    def getDecimationList(self):
        avail_dec = []
        resp = self.send_query_command("DECL")
        rl = self.parseResponse(resp,-1,':');
        if len(rl) == 3:
            val = float(rl[0])
            step = float(rl[2])
            stop = float(rl[1]) + step
            while val < stop:
                avail_dec.append(val)
                val+=step
            return avail_dec
        else:
            rl = self.parseResponse(resp);
            for dec in rl:
                avail_dec.append(float(dec))
        return avail_dec
    def getDecimationListStr(self):
        res_list = self.getDecimationList()
        return self.create_csv(res_list)
    def setDecimation(self, dec):
        return self.setter_with_validation(float(dec), self._setDecimation, self.getDecimation)
    
    
    def _setAttenuation(self, attn):
        self.send_set_command("ATN",str(attn))
    def getAttenuation(self):
        resp = self.send_query_command("ATN")
        return float(self.parseResponse(resp, 1)[0])    
    def getAttenuationList(self):
        resp = self.send_query_command("ATNL")
        return self.parseResponse(resp, 3,':')
    def getAttenuationListStr(self):
        resp = self.send_query_command("ATNL")
        rl = self.parseResponse(resp, 3,':')
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def getMinAttenuation(self):
        return float(self.getFrequencyList()[0])
    def getMaxAttenuation(self):
        return float(self.getFrequencyList()[1])
    def getStepAttenuation(self):
        return float(self.getFrequencyList()[2])
    def setAttenuation(self, attn):
        return self.setter_with_validation(float(attn), self._setAttenuation, self.getAttenuation,1)
    
    
    def _setGain(self, gain):
        self.send_set_command("GAI",str(gain))
    def getGain(self):
        resp = self.send_query_command("GAI")
        return float(self.parseResponse(resp, 1)[0])    
    def getGainList(self):
        resp = self.send_query_command("GAIL")
        return self.parseResponse(resp, 3,':')
    def getGainListStr(self):
        resp = self.send_query_command("GAIL")
        rl = self.parseResponse(resp, 3,':')
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def get_valid_gain(self,gain):
        gl = self.getGainList()
        return self.get_value_valid(gain, 0, float(gl[0]), float(gl[1]), float(gl[2]),True)
    def getMinGain(self):
        return float(self.getFrequencyList()[0])
    def getMaxGain(self):
        return float(self.getFrequencyList()[1])
    def getStepGain(self):
        return float(self.getFrequencyList()[2])
    def setGain(self, gain):
        return self.setter_with_validation(float(gain), self._setGain, self.getGain,1)
    
    def getBIT(self):
        resp = self.send_query_command("BIT")
        return int(self.parseResponse(resp, 1)[0])        
    def getBITList(self):
        resp = self.send_query_command("BITL")
        return self.parseResponse(resp)
    def getInputSampleRate(self):
        resp = self.send_query_command("ISR")
        return float(self.parseResponse(resp, 1)[0]) 
    
    
    enable = property(getEnable,setEnable, doc="")
    min_enable_block_size = property(getMinEnableBlockSize,doc="")
    max_enable_block_size = property(getMaxEnableBlockSize, doc="")
    internal_enable_block_size = property(getIntEnableBlockSize, doc="")
    
    decimation = property(getDecimation,setDecimation, doc="decimation in db")
    available_decimation =  property(getDecimationListStr, doc="Available Attenuation in min::step::max")
    
    attenuation = property(getAttenuation,setAttenuation, doc="Attenuation in db")
    min_attenuation = property(getMinAttenuation, doc="Min Attenuation in db")
    max_attenuation = property(getMaxAttenuation, doc="Max Attenuation in db")
    step_attenuation = property(getStepAttenuation, doc="Step Attenuation in db")
    available_attenuation =  property(getAttenuationListStr, doc="Available Attenuation in min::step::max")
    
    gain = property(getGain,setGain, doc="Gain in db")
    min_gain = property(getMinGain, doc="Min Gain in db")
    max_gain = property(getMaxGain, doc="Max Gain in db")
    step_gain = property(getStepGain, doc="Step Gain in db")
    available_gain =  property(getGainListStr, doc="Available Gain in min::step::max")
    
    bit = property(getBIT, doc="Bit state of the rx")
    input_sample_rate= property(getInputSampleRate, doc="")
    
    frequency_hz = property(getFrequency_Hz,setFrequency_Hz, doc="Center Frequency in Hz")
    min_frequency_hz = property(getMinFrequency_Hz, doc="Min Center Frequency in Hz")
    max_frequency_hz = property(getMaxFrequency_Hz, doc="Max Center Frequency in Hz")
    step_frequency_hz = property(getStepFrequency_Hz, doc="Step Center Frequency in Hz")
    
    available_frequency_hz =  property(getFrequencyListStr, doc="Available Frequency in min::step::max")
    
    bandwidth_hz = property(getBandwidth_Hz,setBandwidth_Hz , doc="Bandwidth in Hz")
    available_bandwidth_hz =  property(getBandwidthListStr, doc="Available Bandwidths")
    
    sample_rate = property(getSampleRate,setSampleRate , doc="SampleRate in Sps")
    available_sample_rate =  property(getSampleRateListStr, doc="Available Sample Rate")

    
                
#end class DDCModule

class WBDDCModule(DdcBaseModule):
    MOD_NAME_MAPPING={1:"DDC",2:"WBDDC"}
    MSDD_X000_BANDWIDTH=20000000.0
    MSDD_X000EX_BANDWIDTH=40000000.0
    
    def setBandwidth_Hz(self, bw):
        return True
        #return (bw == self.MSDD_X000_BANDWIDTH)
    def getBandwidth_Hz(self):
        if (self.sample_rate <= 25000000):
            return self.MSDD_X000_BANDWIDTH
        elif self.sample_rate == 50000000:
            return self.MSDD_X000EX_BANDWIDTH
    def getBandwidthList_Hz(self):
        rv = []
        for rate in DdcBaseModule.getSampleRateList(self):
            if rate <= 25000000.0:
                rv.append(self.MSDD_X000_BANDWIDTH)
            elif rate == 50000000.0:
                rv.append(self.MSDD_X000EX_BANDWIDTH)
            else:
                print "ERROR: Trying to match BW for unknown rate" , rate
        return rv
    def getBandwidthListStr(self):
        return self.create_csv(self.getBandwidthList_Hz)
    def get_valid_bandwidth(self,bandwidth_hz,bandwidth_tolerance_perc, ibw_override=None):
        bw_list = self.getBandwidthList_Hz()
        return self.get_value_valid_list(bandwidth_hz,bandwidth_tolerance_perc, bw_list)
    
    
    bandwidth_hz = property(getBandwidth_Hz,setBandwidth_Hz , doc="Bandwidth in Hz")
    valid_bandwidth_list_hz = property(getBandwidthList_Hz)

class NBDDCModule(DdcBaseModule):
    MOD_NAME_MAPPING={1:"DDC",2:"NBDDC"}

        
    def updateRfFrequencyOffset(self,rf_offset_hz):
        self.rf_offset_hz = float(rf_offset_hz)
    def _setFrequency_Hz(self, freq):
        freq_offset = freq - self.rf_offset_hz
        self.send_set_command("FRQ ",str(freq_offset))
    def getFrequency_Hz(self):
        resp = self.send_query_command("FRQ")
        return float(self.parseResponse(resp, 1)[0])+self.rf_offset_hz
    def getFrequencyList(self):
        resp = self.send_query_command("FRQL")
        rl = self.parseResponse(resp, 3,':')
        return [float(rl[0])+self.rf_offset_hz,float(rl[1])+self.rf_offset_hz,float(rl[2])]
    def getMinFrequency_Hz(self):
        return float(self.getFrequencyList()[0])
    def getMaxFrequency_Hz(self):
        return float(self.getFrequencyList()[1])
    def getStepFrequency_Hz(self):
        return float(self.getFrequencyList()[2])
    def getFrequencyListStr(self):
        rl = self.getFrequencyList()
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def get_valid_frequency(self,center_frequency_hz,center_frequency_tolerance_perc=.01):
        return self.get_value_valid(center_frequency_hz, center_frequency_tolerance_perc, self.min_frequency_hz, self.max_frequency_hz, self.step_frequency_hz)
    def setFrequency_Hz(self, frequency_hz):
        return self.setter_with_validation(float(frequency_hz), self._setFrequency_Hz, self.getFrequency_Hz,1000)
   
   
    def setBandwidth_Hz(self, bw):
        return True
    def getBandwidth_Hz(self):
        resp = self.send_query_command("BWT")
        return float(self.parseResponse(resp, 1)[0])*1e3    
    def getBandwidthList_Hz(self):
        return [self.getBandwidth_Hz()]
    def getBandwidthListStr(self):
        return str(self.getBandwidthList_Hz())
    def get_valid_bandwidth(self,bandwidth_hz,bandwidth_tolerance_perc, ibw_override=None):
        bw_list = self.getBandwidthList_Hz()
        return self.get_value_valid_list(bandwidth_hz,bandwidth_tolerance_perc, bw_list)
    
    
    frequency_hz = property(getFrequency_Hz,setFrequency_Hz, doc="Center Frequency in Hz")
    min_frequency_hz = property(getMinFrequency_Hz, doc="Min Center Frequency in Hz")
    max_frequency_hz = property(getMaxFrequency_Hz, doc="Max Center Frequency in Hz")
    step_frequency_hz = property(getStepFrequency_Hz, doc="Step Center Frequency in Hz")
    available_frequency_hz =  property(getFrequencyListStr, doc="Available Frequency in min::step::max")
    bandwidth_hz = property(getBandwidth_Hz, doc="Bandwidth in Hz")
    
    
class SoftDdcBaseModule(DdcBaseModule):
    MOD_NAME_MAPPING={1:"SDC",2:"SWDDCDEC2"}
    
    """Interface for the MSDD DDC module"""
    def updateRfFrequencyOffset(self,rf_offset_hz):
        self.rf_offset_hz = float(rf_offset_hz)
    def _setFrequency_Hz(self, freq):
        freq_offset = freq - self.rf_offset_hz
        self.send_set_command("FRQ ",str(freq_offset))
    def getFrequency_Hz(self):
        resp = self.send_query_command("FRQ")
        return float(self.parseResponse(resp, 1)[0])+self.rf_offset_hz
    def getFrequencyList(self):
        resp = self.send_query_command("FRQL")
        rl = self.parseResponse(resp, 3,':')
        return [float(rl[0])+self.rf_offset_hz,float(rl[1])+self.rf_offset_hz,float(rl[2])]
    def getMinFrequency_Hz(self):
        return float(self.getFrequencyList()[0])
    def getMaxFrequency_Hz(self):
        return float(self.getFrequencyList()[1])
    def getStepFrequency_Hz(self):
        return float(self.getFrequencyList()[2])
    def getFrequencyListStr(self):
        rl = self.getFrequencyList()
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def get_valid_frequency(self,center_frequency_hz,center_frequency_tolerance_perc=.01):
        return self.get_value_valid(center_frequency_hz, center_frequency_tolerance_perc, self.min_frequency_hz, self.max_frequency_hz, self.step_frequency_hz)
    def setFrequency_Hz(self, frequency_hz):
        return self.setter_with_validation(float(frequency_hz), self._setFrequency_Hz, self.getFrequency_Hz,1000)
    def _setBandwidth_Hz(self, bandwidth_hz):
        return self.setSampleRate(bandwidth_hz)

    def setAttenuation(self, attn):
        return (int(attn) == 0)
    def getAttenuation(self):
        return 0
    def getAttenuationList(self):
        return [0]
    def getAttenuationListStr(self):
        return "0"
    def getMinAttenuation(self):
        return 0
    def getMaxAttenuation(self):
        return 0
    def getStepAttenuation(self):
        return 0
    
    def setGain(self, gain):
        return (int(gain) == 0)
    def getGain(self):
        return 0
    def getGainList(self):
        return [0,0,0]
    def getGainListStr(self):
        return "0::0::0"
    def get_valid_gain(self,gain):
        gl = self.getGainList()
        return self.get_value_valid(gain, 0, float(gl[0]), float(gl[1]), float(gl[2]),True)
    def getMinGain(self):
        return 0
    def getMaxGain(self):
        return 0
    def getStepGain(self):
        return 0
    
    def getBIT(self):
        return 0     
    def getBITList(self):
        return []
    

    frequency_hz = property(getFrequency_Hz,setFrequency_Hz, doc="Center Frequency in Hz")
    min_frequency_hz = property(getMinFrequency_Hz, doc="Min Center Frequency in Hz")
    max_frequency_hz = property(getMaxFrequency_Hz, doc="Max Center Frequency in Hz")
    step_frequency_hz = property(getStepFrequency_Hz, doc="Step Center Frequency in Hz")
    available_frequency_hz =  property(getFrequencyListStr, doc="Available Frequency in min::step::max")
    
    gain = property(getGain,setGain, doc="Gain in db")
    min_gain = property(getMinGain, doc="Min Gain in db")
    max_gain = property(getMaxGain, doc="Max Gain in db")
    step_gain = property(getStepGain, doc="Step Gain in db")
    available_gain =  property(getGainListStr, doc="Available Gain in min::step::max")
    
class StreamRouterModule(baseModule):
    MOD_NAME_MAPPING={1:"SRT",2:"SRT"}
    

    def getModuleList(self):
        resp = self.send_query_command("MODL")
        return self.parseResponse(resp)    
    def getModuleListStr(self):
        res_list = self.getModuleList()
        return self.create_csv(res_list)
    
    def getModulesFlowListByDestination(self,full_reg_name):
        resp = self.send_query_command("SRCL",str(full_reg_name))
        return self.parseResponse(resp)
    
    def getModulesFlowListBySource(self,full_reg_name):
        resp = self.send_query_command("DSTL",str(full_reg_name))
        return self.parseResponse(resp)    
    
    def linkModules(self,source_reg_name,destination_reg_name):
        link_str = str(source_reg_name) + " " + str(destination_reg_name)
        self.send_set_command("LNK",link_str)
        dest_lst = self.getModulesFlowListBySource(source_reg_name)
        for dest in dest_lst:
            if dest == source_reg_name:
                return True
        return False
        
    def unlinkModule(self,reg_name):
        self.send_set_command("UNL",str(reg_name))
        src_lst = self.getModulesFlowListByDestination(reg_name)
        return (len(src_lst) == 0)
        
    def getOutputChannelForModule(self,full_reg_name):
        modList = self.getModulesFlowListBySource(full_reg_name)
        for module in modList:
            if module == "OUT":
                return 0
            else:
                m = re.search('OUT:(\d+)',module)
                if m:
                    return int(m.group(1))
        return None
    
    def getFFTChannelForModule(self,full_reg_name):
        modList = self.getModulesFlowListBySource(full_reg_name)
        for module in modList:
            if module == "FFT":
                return 0
            else:
                m = re.search('FFT:(\d+)',module)
                if m:
                    return int(m.group(1))
        return None    
    
    def getChannelList(self,module_name):
        ret_val = []
        try:
            min_max_lst = []
            resp = self.send_query_command("RCL",str(module_name))
            resp_list = self.parseResponse(resp)
            for i in range(0,len(resp_list)):
                ch_split = str(resp_list[i]).rsplit(':')
                if len(ch_split) <= 1:
                    min_max_lst.append(0)
                else:
                    min_max_lst.append(int(ch_split[len(ch_split)-1]))
            for i in range(min_max_lst[0],min_max_lst[1]+1):
                ret_val.append(i)
        except:
            ret_val.append(0)
        return ret_val
    def getInstallName(self,reg_module):
        resp = self.send_query_command("INA",str(reg_module))
        return str(self.parseResponse(resp, -1,':')[0])
    
    def getRegistrationName(self,installed_module):
        resp = self.send_query_command("RNA",str(installed_module))
        return str(self.parseResponse(resp, 1)[0])
    
    def registerModule(self,reg_name, install_name, starting_channel, num_channels):
        reg_str = str(reg_name) + " " + str(install_name) + " " + str(starting_channel) + " " + str(num_channels)
        self.send_set_command("REG",reg_str)
            
    
    module_list = property(getModuleList,doc="")
    module_list_csv = property(getModuleListStr,doc="")
    
    
    

class FFTModule(baseModule):
    MOD_NAME_MAPPING={1:"FFT",2:"FFT"}
    def _setEnable(self, enable, num_sets=0):
        arg="0" 
        if enable: 
            arg="1:" + str(num_sets)
        self.send_set_command("ENB",str(arg))
    def getEnable(self):
        resp = self.send_query_command("ENB")
        return (int(self.parseResponse(resp, -1,':')[0]) == 1)    
    def getEnableSetsLimits(self):
        resp = self.send_query_command("ENBL")
        return self.parseResponse(resp, 3,':')
    def getMinEnableSetsSize(self):
        return int(self.getFrequencyList()[0])
    def getMaxEnableSetsSize(self):
        return int(self.getFrequencyList()[1])
    def getIntEnableSetsSize(self):
        return int(self.getFrequencyList()[2])
    def setEnable(self, enable):
        return self.setter_with_validation(enable, self._setEnable, self.getEnable)
    
    
    def _setAverage(self, num_avg):
        self.send_set_command("AVG",str(num_avg))
    def getAverage(self):
        resp = self.send_query_command("AVG")
        return int(self.parseResponse(resp, 1)[0])    
    def getAverageList(self):
        resp = self.send_query_command("AVGL")
        return self.parseResponse(resp, 3,':')
    def getAverageListStr(self):
        resp = self.send_query_command("AVGL")
        rl = self.parseResponse(resp, 3,':')
        return self.create_range_string(int(rl[0]), int(rl[1]), int(rl[2]))
    def getMinAverage(self):
        return int(self.getAverageList()[0])
    def getMaxAverage(self):
        return int(self.getAverageList()[1])
    def getStepAverage(self):
        return int(self.getAverageList()[2])
    def getValidAverage(self,num_avg):
        valid_avg_list = self.getAverageList()
        return self.get_value_valid(num_avg,0,int(valid_avg_list[0]), int(valid_avg_list[1]), int(valid_avg_list[2]),True)
    def setAverage(self, num_avg):
        return self.setter_with_validation(int(num_avg), self._setAverage, self.getAverage)
    
    
    def _setFFTRate(self, rate):
        self.send_set_command("RAT",str(rate))
    def getFFTRate(self):
        resp = self.send_query_command("RAT")
        return float(self.parseResponse(resp, 1)[0])    
    def getFFTRateList(self):
        resp = self.send_query_command("RATL")
        return self.parseResponse(resp, 3,':')
    def getFFTRateListStr(self):
        resp = self.send_query_command("RATL")
        rl = self.parseResponse(resp, 3,':')
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def getMinFFTRate(self):
        return float(self.getFFTRateList()[0])
    def getMaxFFTRate(self):
        return float(self.getFFTRateList()[1])
    def getStepFFTRate(self):
        return float(self.getFFTRateList()[2])
    def setFFTRate(self, rate):
        return self.setter_with_validation(float(rate), self._setFFTRate, self.getFFTRate)
    
    
    def _setFFTSize(self, fftSize=4096):
        rl = self.getFFTSizeList()
        fftPos = max(0,len(rl)-1)
        for x in reversed(range(0,len(rl))):
            if fftSize <= rl[x]:
                fftPos = x
                break;
        self.send_set_command("PNT ",str(fftPos))
    def getFFTSize(self):
        resp = self.send_query_command("PNT")
        pos = int(self.parseResponse(resp, 1)[0])
        rl = self.getFFTSizeList()
        return int(rl[pos])
    def getFFTSizeList(self):
        resp = self.send_query_command("PNTL")
        return self.parseResponse(resp)
    def getFFTSizeListStr(self):
        rl = self.getFFTSizeList()
        return self.create_desc_val_csv(range(0,len(rl)),rl)
    def setFFTSize(self, fftSize=4096):
        return self.setter_with_validation(int(fftSize), self._setFFTSize, self.getFFTSize)    

    def _setBinSize(self, bin_size):
        self.send_set_command("BIN",str(bin_size))
    def getBinSize(self):
        resp = self.send_query_command("BIN")
        return int(self.parseResponse(resp, 1)[0])    
    def getBinSizeList(self):
        resp = self.send_query_command("BINL")
        return self.parseResponse(resp, 3,':')
    def getBinSizeListStr(self):
        resp = self.send_query_command("BINL")
        rl = self.parseResponse(resp, 3,':')
        return self.create_range_string(int(rl[0]), int(rl[1]), int(rl[2]))
    def getMinBinSize(self):
        return int(self.getBinSizeList()[0])
    def getMaxBinSize(self):
        return int(self.getBinSizeList()[1])
    def getStepBinSize(self):
        return int(self.getBinSizeList()[2])    
    def setBinSize(self, bin_size):
        return self.setter_with_validation(int(bin_size), self._setBinSize, self.getBinSize)   
    
    
    def _setWindowType(self, window_type_number):
        self.send_set_command("WND",str(window_type_number))
    def getWindowType(self):
        resp = self.send_query_command("WND")
        return int(self.parseResponse(resp, 1)[0])
    def getWindowTypeString(self):
        pos = self.getWindowType()
        rl = self.getWindowTypeList()
        return str(rl[pos])
    def getWindowTypeList(self):
        resp = self.send_query_command("WNDL")
        return self.parseResponse(resp)
    def getWindowTypeListStr(self):
        return create_csv(self.getWindowTypeList())
    def getWindowTypeNumberFromString(self,window_type_string):
        rl = self.getWindowTypeList()
        for i in range(0,len(rl)):
            if str(window_type_string) == str(rl[i]):
                return i
        return 0
    def _setWindowTypeString(self, window_type_string):
        pos = self.getWindowTypeNumberFromString(window_type_string)
        return self._setWindowType(pos)
    def setWindowType(self, window_type_number):
        return self.setter_with_validation(int(window_type_number), self._setWindowType, self.getWindowType)   
    def setWindowTypeString(self, window_type_string):
        return self.setter_with_validation(str(window_type_string), self._setWindowTypeString, self.getWindowTypeString)   
    
   

    def _setPeakMode(self, peak_mode_number):
        self.send_set_command("PMD",str(peak_mode_number))
    def _setPeakModeString(self, peak_mode_string):
        pos = self.getPeakModeNumberFromString(peak_mode_string)
        return self.setPeakMode(pos)
    def getPeakMode(self):
        resp = self.send_query_command("PMD")
        return int(self.parseResponse(resp, 1)[0])
    def getPeakModeString(self):
        pos = self.getPeakMode()
        rl = self.getPeakModeList()
        return str(rl[pos])
    def getPeakModeList(self):
        resp = self.send_query_command("PMDL")
        return self.parseResponse(resp)
    def getPeakModeListStr(self):
        return create_csv(self.getPeakModeList())
    def getPeakModeNumberFromString(self,peak_mode_string):
        rl = self.getPeakModeList()
        for i in range(0,len(rl)):
            if str(peak_mode_string) == str(rl[i]):
                return i
        return 0    
    def setPeakMode(self, peak_mode_number):
        return self.setter_with_validation(int(peak_mode_number), self._setPeakMode, self.getPeakMode)   
    def setPeakModeString(self, peak_mode_string):
        return self.setter_with_validation(str(peak_mode_string), self._setPeakModeString, self.getPeakModeString)   
    
    
    def _setPeakDecayRate(self, rate):
        self.send_set_command("PDR",str(rate))
    def getPeakDecayRate(self):
        resp = self.send_query_command("PDR")
        return float(self.parseResponse(resp, 1)[0])    
    def getPeakDecayRateList(self):
        resp = self.send_query_command("PDRL")
        return self.parseResponse(resp, 3,':')
    def getPeakDecayRateListStr(self):
        resp = self.send_query_command("PDRL")
        rl = self.parseResponse(resp, 3,':')
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def getMinPeakDecayRate(self):
        return float(self.getPeakDecayRateList()[0])
    def getMaxPeakDecayRate(self):
        return float(self.getPeakDecayRateList()[1])
    def getStepPeakDecayRate(self):
        return float(self.getPeakDecayRateList()[2])
    def setPeakDecayRate(self, rate):
        return self.setter_with_validation(float(rate), self._setPeakDecayRate, self.getPeakDecayRate)   

    def getInputSampleRate(self):
        resp = self.send_query_command("ISR")
        return float(self.parseResponse(resp, 1)[0]) 
    
    
    def _setTippingThresholds(self, up_threshold=10, down_threshold=2):
        self.send_set_command("TTHR",str(up_threshold) + " " + str(down_threshold))
    def getTippingThresholdsList(self):
        resp = self.send_query_command("TTHR")
        return self.parseResponse(resp, 2,' ')
    def getUpTippingThresholds(self):
        return float(self.getTippingThresholdsList()[0])
    def getDownTippingThresholds(self):
        return float(self.getTippingThresholdsList()[1])
    def setTippingThresholds(self, up_threshold=10, down_threshold=2):
        self._setTippingThresholds(up_threshold,down_threshold)
        return str(self.getTippingThresholdsList()) == str([up_threshold,down_threshold])
    
    def setTippingMode(self, tipping_enabled=True, spectrum_output=True):
        mode = 0
        if not tipping_enabled and spectrum_output:
            mode = 0
        elif tipping_enabled and spectrum_output:
            mode = 1
        elif tipping_enabled and not spectrum_output:
            mode = 2
        self.setTippingModeNum(mode)
        return mode == self.getTippingMode()
    def setTippingModeNum(self, mode):
        self.send_set_command("TIP",str(mode))
    def getTippingMode(self):
        resp = self.send_query_command("TIP")
        return int(self.parseResponse(resp)[0])
    
    def setTippingRefByTrace(self):
        self.send_set_command("TP2R")
    def setTippingRefByConstant(self, threshold_value=-100):
        self.send_set_command("TSET",str(threshold_value))
    
    enable = property(getEnable,setEnable, doc="")
    num_averages = property(getAverage,setAverage, doc="")
    time_between_fft_ms =  property(getFFTRate,setFFTRate, doc="")
    fftSize = property(getFFTSize,setFFTSize, doc="")
    outputBins =  property(getBinSize,setBinSize, doc="")
    window_type_str = property(getWindowTypeString,setWindowTypeString, doc="")
    peak_mode_pos =  property(getPeakMode,setPeakMode, doc="")
    peak_mode_str =  property(getPeakModeString,setPeakModeString, doc="")
    peak_decay_rate = property(getPeakDecayRate,setPeakDecayRate, doc="")
    input_sample_rate= property(getInputSampleRate, doc="")

class SpectralScanModule(baseModule):
    MOD_NAME_MAPPING={1:"SPC",2:"SPC"}
    def _setEnable(self, enable, num_sets=0):
        arg="0" 
        if enable: 
            arg="1:" + str(num_sets)
        self.send_set_command("ENB",str(arg))
    def getEnable(self):
        resp = self.send_query_command("ENB")
        return (int(self.parseResponse(resp, -1,':')[0]) == 1)    
    def getEnableSetsLimits(self):
        resp = self.send_query_command("ENBL")
        return self.parseResponse(resp, 3,':')
    def getMinEnableSetsSize(self):
        return int(self.getFrequencyList()[0])
    def getMaxEnableSetsSize(self):
        return int(self.getFrequencyList()[1])
    def getIntEnableSetsSize(self):
        return int(self.getFrequencyList()[2])
    def setEnable(self, enable):
        return self.setter_with_validation(enable, self._setEnable, self.getEnable)
    
    
    def _setAverage(self, num_avg):
        self.send_set_command("AVG",str(num_avg))
    def getAverage(self):
        resp = self.send_query_command("AVG")
        return int(self.parseResponse(resp, 1)[0])    
    def getAverageList(self):
        resp = self.send_query_command("AVGL")
        return self.parseResponse(resp, 3,':')
    def getAverageListStr(self):
        resp = self.send_query_command("AVGL")
        rl = self.parseResponse(resp, 3,':')
        return self.create_range_string(int(rl[0]), int(rl[1]), int(rl[2]))
    def getMinAverage(self):
        return int(self.getAverageList()[0])
    def getMaxAverage(self):
        return int(self.getAverageList()[1])
    def getStepAverage(self):
        return int(self.getAverageList()[2])
    def getValidAverage(self,num_avg):
        valid_avg_list = self.getAverageList()
        return self.get_value_valid(num_avg,0,int(valid_avg_list[0]), int(valid_avg_list[1]), int(valid_avg_list[2]),True)
    def setAverage(self, num_avg):
        return self.setter_with_validation(int(num_avg), self._setAverage, self.getAverage)
    
    
    def _setFFTRate(self, rate):
        self.send_set_command("RAT",str(rate))
    def getFFTRate(self):
        resp = self.send_query_command("RAT")
        return float(self.parseResponse(resp, 1)[0])    
    def getFFTRateList(self):
        resp = self.send_query_command("RATL")
        return self.parseResponse(resp, 3,':')
    def getFFTRateListStr(self):
        resp = self.send_query_command("RATL")
        rl = self.parseResponse(resp, 3,':')
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def getMinFFTRate(self):
        return float(self.getFFTRateList()[0])
    def getMaxFFTRate(self):
        return float(self.getFFTRateList()[1])
    def getStepFFTRate(self):
        return float(self.getFFTRateList()[2])
    def setFFTRate(self, rate):
        return self.setter_with_validation(float(rate), self._setFFTRate, self.getFFTRate)
    
    
    def _setFFTSize(self, fftSize=4096):
        rl = self.getFFTSizeList()
        fftPos = max(0,len(rl)-1)
        for x in range(0,len(rl)):
            if fftSize <= int(rl[x]):
                fftPos = x
                break;
        self.send_set_command("PNT ",str(fftPos))
    def getFFTSize(self):
        resp = self.send_query_command("PNT")
        pos = int(self.parseResponse(resp, 1)[0])
        rl = self.getFFTSizeList()
        return int(rl[pos])
    def getFFTSizeList(self):
        resp = self.send_query_command("PNTL")
        return self.parseResponse(resp)
    def getFFTSizeListStr(self):
        rl = self.getFFTSizeList()
        return self.create_desc_val_csv(range(0,len(rl)),rl)
    def setFFTSize(self, fftSize=4096):
        return self.setter_with_validation(int(fftSize), self._setFFTSize, self.getFFTSize)    

    def _setBinSize(self, bin_size):
        self.send_set_command("BIN",str(bin_size))
    def getBinSize(self):
        resp = self.send_query_command("BIN")
        return int(self.parseResponse(resp, 1)[0])    
    def getBinSizeList(self):
        resp = self.send_query_command("BINL")
        return self.parseResponse(resp, 3,':')
    def getBinSizeListStr(self):
        resp = self.send_query_command("BINL")
        rl = self.parseResponse(resp, 3,':')
        return self.create_range_string(int(rl[0]), int(rl[1]), int(rl[2]))
    def getMinBinSize(self):
        return int(self.getBinSizeList()[0])
    def getMaxBinSize(self):
        return int(self.getBinSizeList()[1])
    def getStepBinSize(self):
        return int(self.getBinSizeList()[2])    
    def get_valid_binsize(self, bin):
        return bin <= self.getMaxBinSize() and bin >= self.getMinBinSize() and bin % self.getStepBinSize() == 0
    def setBinSize(self, bin_size):
        return self.setter_with_validation(int(bin_size), self._setBinSize, self.getBinSize)   
    
    
    def _setWindowType(self, window_type_number):
        self.send_set_command("WND",str(window_type_number))
    def getWindowType(self):
        resp = self.send_query_command("WND")
        return int(self.parseResponse(resp, 1)[0])
    def getWindowTypeString(self):
        pos = self.getWindowType()-1
        rl = self.getWindowTypeList()
        if (pos > -1 and pos < len(rl)):
            rv = str(rl[pos])
        else:
            rv = ""
        return rv
    def getWindowTypeList(self):
        resp = self.send_query_command("WNDL")
        return self.parseResponse(resp)
    def getWindowTypeListStr(self):
        return create_csv(self.getWindowTypeList())
    def getWindowTypeNumberFromString(self,window_type_string):
        rl = self.getWindowTypeList()
        for i in range(0,len(rl)):
            if str(window_type_string) == str(rl[i]):
                return i
        return 0
    def _setWindowTypeString(self, window_type_string):
        pos = self.getWindowTypeNumberFromString(window_type_string)
        return self._setWindowType(pos)
    def setWindowType(self, window_type_number):
        return self.setter_with_validation(int(window_type_number), self._setWindowType, self.getWindowType)   
    def setWindowTypeString(self, window_type_string):
        return self.setter_with_validation(str(window_type_string), self._setWindowTypeString, self.getWindowTypeString)   
    
   

    def _setPeakMode(self, peak_mode_number):
        self.send_set_command("PMD",str(peak_mode_number))
    def _setPeakModeString(self, peak_mode_string):
        pos = self.getPeakModeNumberFromString(peak_mode_string)
        return self.setPeakMode(pos)
    def getPeakMode(self):
        resp = self.send_query_command("PMD")
        return int(self.parseResponse(resp, 1)[0])
    def getPeakModeString(self):
        pos = self.getPeakMode()
        rl = self.getPeakModeList()
        return str(rl[pos])
    def getPeakModeList(self):
        resp = self.send_query_command("PMDL")
        return self.parseResponse(resp)
    def getPeakModeListStr(self):
        return create_csv(self.getPeakModeList())
    def getPeakModeNumberFromString(self,peak_mode_string):
        rl = self.getPeakModeList()
        for i in range(0,len(rl)):
            if str(peak_mode_string) == str(rl[i]):
                return i
        return 0    
    def setPeakMode(self, peak_mode_number):
        return self.setter_with_validation(int(peak_mode_number), self._setPeakMode, self.getPeakMode)   
    def setPeakModeString(self, peak_mode_string):
        return self.setter_with_validation(str(peak_mode_string), self._setPeakModeString, self.getPeakModeString)   
    
    
    def _setPeakDecayRate(self, rate):
        self.send_set_command("PDR",str(rate))
    def getPeakDecayRate(self):
        resp = self.send_query_command("PDR")
        return float(self.parseResponse(resp, 1)[0])    
    def getPeakDecayRateList(self):
        resp = self.send_query_command("PDRL")
        return self.parseResponse(resp, 3,':')
    def getPeakDecayRateListStr(self):
        resp = self.send_query_command("PDRL")
        rl = self.parseResponse(resp, 3,':')
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def getMinPeakDecayRate(self):
        return float(self.getPeakDecayRateList()[0])
    def getMaxPeakDecayRate(self):
        return float(self.getPeakDecayRateList()[1])
    def getStepPeakDecayRate(self):
        return float(self.getPeakDecayRateList()[2])
    def setPeakDecayRate(self, rate):
        return self.setter_with_validation(float(rate), self._setPeakDecayRate, self.getPeakDecayRate)   

    def getInputSampleRate(self):
        resp = self.send_query_command("ISR")
        return float(self.parseResponse(resp, 1)[0]) 
    
    
    def _setFrequencies(self, min_freq=30000000.0, max_freq=3000000000.0):
        #Set frequencies
        minmhz = min_freq/1e6
        maxmhz = max_freq/1e6
        self.send_set_command("FRQ", "%s:%s" % (str(minmhz), str(maxmhz)))
   
    def _getFrequencies(self):
        resp = self.send_query_command("FRQ")
        return self.parseResponse(resp, 2, ":")        
    def getMinFrequency(self):
        return float(self._getFrequencies()[0])*1e6
    def getMaxFrequency(self):
        return float(self._getFrequencies()[1])*1e6
    def getStepsFrequency(self):
        return float(self._getFrequencies()[2])*1e6
    def getFrequencies(self):
        return [self.getMinFrequency(), self.getMaxFrequency()]
    def setFrequencies(self, minfrq, maxfrq):
        self._setFrequencies(minfrq, maxfrq)
        return self.getFrequencies() == [minfrq, maxfrq]
        

    #Not supporting IF BW Modification

    
    def _setTippingThresholds(self, up_threshold=10, down_threshold=2):
        self.send_set_command("TTHR",str(up_threshold) + " " + str(down_threshold))
    def getTippingThresholdsList(self):
        resp = self.send_query_command("TTHR")
        return self.parseResponse(resp, 2,' ')
    def getUpTippingThresholds(self):
        return float(self.getTippingThresholdsList()[0])
    def getDownTippingThresholds(self):
        return float(self.getTippingThresholdsList()[1])
    def setTippingThresholds(self, up_threshold=10, down_threshold=2):
        self._setTippingThresholds(up_threshold,down_threshold)
        return str(self.getTippingThresholdsList()) == str([up_threshold,down_threshold])
    
    def setTippingMode(self, tipping_enabled=True, spectrum_output=True, loss_report=False):
        mode = 0
        if not tipping_enabled and spectrum_output:
            mode = 0
        elif tipping_enabled and spectrum_output:
            mode = 1
        elif tipping_enabled and not spectrum_output:
            mode = 2

        loss = 0
        if loss_report: 
            loss = 1

        self.setTippingModeNums(mode, loss)
        return str([mode, loss]) == str(self.getTippingMode()[0:1])

    def setTippingModeNum(self, mode, loss):
        self.send_set_command("TIP","%s,%s" % (str(mode), str(loss)))

    def getTippingMode(self):
        resp = self.send_query_command("TIP")
        return self.parseResponse(resp, 3, ",")

    
    enable = property(getEnable,setEnable, doc="")
    num_averages = property(getAverage,setAverage, doc="")
    time_between_fft_ms =  property(getFFTRate,setFFTRate, doc="")
    fftSize = property(getFFTSize,setFFTSize, doc="")
    outputBins =  property(getBinSize,setBinSize, doc="")
    window_type_str = property(getWindowTypeString,setWindowTypeString, doc="")
    peak_mode_pos =  property(getPeakMode,setPeakMode, doc="")
    peak_mode_str =  property(getPeakModeString,setPeakModeString, doc="")
    peak_decay_rate = property(getPeakDecayRate,setPeakDecayRate, doc="")
     
   
class GpsNavModule(baseModule):
    MOD_NAME_MAPPING={1:"GPS",2:"GPS"}
    def __init__(self, com, channel_number=0, mapping_version=2):
        raise NotImplementedError, "NAV/GPS Module is not implemented!"
    
class TFNModule(baseModule):
    MOD_NAME_MAPPING={1:"TFN",2:"TFN"}
    def __init__(self, com, channel_number=0, mapping_version=2):
        raise NotImplementedError, "TFN Module is not implemented!"
    
class IQCircularBufferModule(baseModule):
    MOD_NAME_MAPPING={1:None,2:"IQB"}
    def __init__(self, com, channel_number=0, mapping_version=2):
        raise NotImplementedError, "IQCircularBuffer Module is not implemented!"
    
                
class OUTModule(baseModule):
    MOD_NAME_MAPPING={1:"OUT",2:"OUT"}
    
    PROTOCOL_UDP_SDDS=0
    PROTOCOL_UDP_SDDSX=1
    PROTOCOL_UDP_VITA49=2
    PROTOCOL_UDP_RAW=3
    PROTOCOL_UDP_SDDSA=4
    
    def setIPP_string(self,ipp_str):
        self.send_set_command("IPP ",str(ipp_str))
        return (ipp_str == self.getIPPString() or ("0:" + ipp_str == self.getIPPString()))
    def setIP(self,ip_address, port):
        ip_arg = str(ip_address).strip() + ":" + str(port).strip()
        return self.setIPP_string(ip_arg)
    
    def getIPP(self):
        """ Gets the present IP and UDP port setting for logging module"""
        resp = self.send_query_command("IPP")
        try:
            response = self.parseResponse(resp, 2,':')
        except:
            response = self.parseResponse(resp, 3,':')[-2:]
        return response
    
    def getIPPString(self):
        """ Gets the present IP and UDP port setting for logging module"""
        resp = self.send_query_command("IPP")
        return str(self.parseResponse(resp, 1)[0])
    def getIPP_IP(self):
        return self.getIPP()[0];
    def getIPP_Port(self):
        return self.getIPP()[1];
    
    

    def _setEnable(self, enable, block_xfer_size=0, sync_channel=-1):
        arg="0" 
        if enable: arg="1"
        arg+=":" + str(block_xfer_size)
        if sync_channel >= 0:
            arg+=":" + str(sync_channel)
        self.send_set_command("ENB",str(arg))
    def getEnable(self):
        resp = self.send_query_command("ENB")
        return (int(self.parseResponse(resp, -1,':')[0]) == 1)    
    def getEnableBlockLimits(self):
        resp = self.send_query_command("ENBL")
        return self.parseResponse(resp, 3,':')
    def getMinEnableBlockSize(self):
        return int(self.getFrequencyList()[0])
    def getMaxEnableBlockSize(self):
        return int(self.getFrequencyList()[1])
    def getStepEnableBlockSize(self):
        return int(self.getFrequencyList()[2])
    def setEnable(self, enable):
        return self.setter_with_validation(enable, self._setEnable, self.getEnable)
    
    def getBIT(self):
        resp = self.send_query_command("BIT")
        return int(self.parseResponse(resp, 1)[0])        
    def getBITList(self):
        resp = self.send_query_command("BITL")
        return self.parseResponse(resp)
    
    def _setUdpPacketRate(self, rat):
        self.send_set_command("RAT",str(rat))
    def getUdpPacketRate(self):
        resp = self.send_query_command("RAT")
        return self.parseResponse(resp, 1)[0]    
    def getUdpPacketRateList(self):
        resp = self.send_query_command("RATL")
        return self.parseResponse(resp, 3,':')
    def getMinUdpPacketRate(self):
        return int(self.getFrequencyList()[0])
    def getMaxUdpPacketRate(self):
        return int(self.getFrequencyList()[1])
    def getStepUdpPacketRate(self):
        return int(self.getFrequencyList()[2])
    def setUdpPacketRate(self, rat):
        return self.setter_with_validation(int(rat), self._setUdpPacketRate, self.getUdpPacketRate)
    
    
    def _setOutputDataWidth(self, dwt):
        self.send_set_command("DWT",str(dwt))
    def getOutputDataWidth(self):
        resp = self.send_query_command("DWT")
        return int(self.parseResponse(resp, 1)[0])    
    def getOutputDataWidthList(self):
        resp = self.send_query_command("DWTL")
        return self.parseResponse(resp)
    def setOutputDataWidth(self, dwt):
        return self.setter_with_validation(int(dwt), self._setOutputDataWidth, self.getOutputDataWidth)
    
    def _setOutputSamplesPerFrame(self, olen):
        self.send_set_command("LEN",str(olen))
    def getOutputSamplesPerFrame(self):
        resp = self.send_query_command("LEN")
        return int(self.parseResponse(resp, 1)[0])    
    def getOutputSamplesPerFrameList(self):
        resp = self.send_query_command("LENL")
        return self.parseResponse(resp, 3,':')
    def getMinOutputSamplesPerFrame(self):
        return int(self.getFrequencyList()[0])
    def getMaxOutputSamplesPerFrame(self):
        return int(self.getFrequencyList()[1])
    def getStepOutputSamplesPerFrame(self):
        return int(self.getFrequencyList()[2])
    def setOutputSamplesPerFrame(self, olen):
        return self.setter_with_validation(int(olen), self._setOutputSamplesPerFrame, self.getOutputSamplesPerFrame)
    
    
    def _setOutputStreamID(self, osid):
        self.send_set_command("SID",str(osid))
    def getOutputStreamID(self):
        resp = self.send_query_command("SID")
        return self.parseResponse(resp, 1)[0]
    def setOutputStreamID(self, osid):
        return self.setter_with_validation(str(osid), self._setOutputStreamID, self.getOutputStreamID)
    
    
    def _setOutputProtocol(self, proto):
        self.send_set_command("POL",str(proto))
    def getOutputProtocol(self):
        resp = self.send_query_command("POL")
        return int(self.parseResponse(resp, 1)[0])    
    def getOutputProtocolString(self):
        proto = self.getOutputProtocol()
        proto_list = self.getOutputProtocolList()
        return proto_list[proto]
    def getOutputProtocolList(self):
        resp = self.send_query_command("POLL")
        return self.parseResponse(resp)
    def getOutputProtocolNumberFromString(self,protocol_string):
        resp = self.send_query_command("POLL")
        pro_lst = self.parseResponse(resp)
        for i in range(0,len(pro_lst)):
            if str(protocol_string) == str(pro_lst[i]):
                return i
        return 0
    def setOutputProtocol(self, proto):
        return self.setter_with_validation(int(proto), self._setOutputProtocol, self.getOutputProtocol)   
    def setOutputProtocolString(self, proto_string):
        proto_num = self.getOutputProtocolNumberFromString(proto_string)
        return self.setOutputProtocol(proto_num)   
    
    
    
    def _setOutputEndianess(self, end):
        self.send_set_command("END",str(end))
    def getOutputEndianess(self):
        resp = self.send_query_command("END")
        return int(self.parseResponse(resp, 1)[0])    
    def getOutputEndianessList(self):
        resp = self.send_query_command("ENDL")
        return self.parseResponse(resp)
    def setOutputEndianess(self, end):
        return self.setter_with_validation(int(end), self._setOutputEndianess, self.getOutputEndianess)   
    
    def _setAdditionalPktAlloc(self, add):
        self.send_set_command("PKT",str(add))
    def getAdditionalPktAlloc(self):
        resp = self.send_query_command("PKT")
        return int(self.parseResponse(resp, 1)[0])    
    def getAdditionalPktAllocMax(self):
        resp = self.send_query_command("PKTL")
        return int(self.parseResponse(resp, 1)[0]) 
    def setAdditionalPktAlloc(self, add):
        return self.setter_with_validation(int(add), self._setAdditionalPktAlloc, self.getAdditionalPktAlloc)   
    
    
    def getInputSampleRate(self):
        resp = self.send_query_command("ISR")
        return float(self.parseResponse(resp, 1)[0]) 
    
    def _setGain(self, attn):
        self.send_set_command("GAI",str(attn))
    def getGain(self):
        resp = self.send_query_command("GAI")
        return float(self.parseResponse(resp, 1)[0])    
    def getGainList(self):
        resp = self.send_query_command("GAIL")
        return self.parseResponse(resp, 3,':')
    def getMinGain(self):
        return float(self.getFrequencyList()[0])
    def getMaxGain(self):
        return float(self.getFrequencyList()[1])
    def getStepGain(self):
        return float(self.getFrequencyList()[2])
    def setGain(self, attn):
        return self.setter_with_validation(float(attn), self._setGain, self.getGain,1)   
    
    def _setEnableVlanTagging(self, enable):
        arg="0" 
        if enable: arg="1"
        self.send_set_command("VLANEN",arg)
    def getEnableVlanTagging(self):
        resp = self.send_query_command("VLANEN")
        return (int(self.parseResponse(resp, 1)[0]) == 1)
    def setEnableVlanTagging(self, enable):
        return self.setter_with_validation(enable, self._setEnableVlanTagging, self.getEnableVlanTagging)   
    
    def _setVlanTci(self, tci):
        self.send_set_command("VLANTCI",str(tci))
    def getVlanTci(self):
        resp = self.send_query_command("VLANTCI")
        return int(self.parseResponse(resp, 1)[0])
    def setVlanTci(self, tci):
        return self.setter_with_validation(int(tci), self._setVlanTci, self.getVlanTci)   
    
    def _setTimestampRef(self, ref):
        self.send_set_command("TSREF",str(ref))
    def getTimestampRef(self):
        resp = self.send_query_command("TSREF")
        return int(self.parseResponse(resp, 1)[0])
    def setTimestampRef(self, ref):
        return self.setter_with_validation(int(ref), self._setTimestampRef, self.getTimestampRef)   
    
    def getTimestampOff(self):
        resp = self.send_query_command("TSOFS")
        return int(self.parseResponse(resp, 1)[0])
    def _setTimestampOff(self, offset):
        self.send_set_command("TSOFS",str(offset))
    def setTimestampOff(self, offset):
        return self.setter_with_validation(int(offset), self._setTimestampOff, self.getTimestampOff)   
    
    
    def getMFP(self):
        resp = self.send_query_command("MFP")
        return int(self.parseResponse(resp, 1)[0])
    def _setMFP(self, mfp):
        self.send_set_command("MFP",str(mfp))
    def setMFP(self, mfp):
        return self.setter_with_validation(int(mfp), self._setMFP, self.getMFP)   
    
    def getCDR(self):
        resp = self.send_query_command("CDR")
        return float(self.parseResponse(resp, 1)[0])
    def _setCDR(self,cdr):
        self.send_set_command("CDR",str(cdr))
    def setCDR(self,cdr):
        return self.setter_with_validation(float(cdr), self._setCDR, self.getCDR)        

    def getCCR(self):
        resp = self.send_query_command("CCR")
        return int(self.parseResponseSpaceOnly(resp, 4)[2]) 
    def _setCCR(self,ccr):
        self.send_set_command("CCR",str(ccr))
    def setCCR(self,ccr):
        return self.setter_with_validation(int(ccr), self._setCCR, self.getCCR) 
    
    ip_port_string = property(getIPPString)
    ip_addr = property(getIPP_IP)
    ip_port = property(getIPP_Port)
    enabled = property(getEnable,setEnable, doc="")
    bit = property(getBIT)
    pkt_rate = property(getUdpPacketRate,setUdpPacketRate, doc="")
    data_width = property(getOutputDataWidth,setOutputDataWidth, doc="")
    samples_per_frame = property(getOutputSamplesPerFrame,setOutputSamplesPerFrame, doc="")
    stream_id = property(getOutputStreamID,setOutputStreamID, doc="")
    protocol = property(getOutputProtocol,setOutputProtocol, doc="")
    endianess = property(getOutputEndianess,setOutputEndianess, doc="")
    add_pkt_allocations = property(getAdditionalPktAlloc,setAdditionalPktAlloc, doc="")
    input_sample_rate = property(getInputSampleRate, doc="")
    gain_when_reducing_bits = property(getGain,setGain, doc="")
    vlan_tagging_enabled = property(getEnableVlanTagging,setEnableVlanTagging, doc="")
    vlan_tci = property(getVlanTci,setVlanTci, doc="")
    vlan_timestamp_ref = property(getTimestampRef,setTimestampRef, doc="")
    timestamp_offset_ns = property(getTimestampOff,setTimestampOff, doc="")
    mfp = property(getMFP,setMFP, doc="")
    cdr = property(getCDR,setCDR, doc="")
    ccr = property(getCCR,setCCR, doc="")
    
#end class OUTModule




class object_container(object):
    def __init__(self):
        self.used_objects = []
        self.free_objects = []
    def put_object(self, obj):
        if self.used_objects.count(obj) > 0:
            self.used_objects.remove(obj)
        self.free_objects.append(obj)
        
    def mark_as_used(self, obj):
        if obj == None:
            return
        if self.free_objects.count(obj) <= 0:
            return 
        self.free_objects.remove(obj)
        self.used_objects.append(obj)
          
    def get_object(self, obj=None):
        #ensure that free array is not empty
        if len(self.free_objects) <= 0:
            return None
        
        # if obj is defined, then remove that specific object or return None. If not defined, then pull any from the list
        if obj != None:
            if self.free_objects.count(obj) <= 0:
                return None
            self.free_objects.remove(obj)  
        else:
            obj = self.free_objects.pop(0)
            
        # add to used list  
        self.used_objects.append(obj)
        return obj
        
            


class MSDDRadio:
    MSDDRXTYPE_UNKNOWN="UNKNOWN"        #UNKNOWN
    MSDDRXTYPE_ANALOG_RX="ANALOG_RX"    #RCVR
    MSDDRXTYPE_DIGITAL_RX="DIGITAL_RX"  #RCVR+WB_DDC
    MSDDRXTYPE_HW_DDC="HW_DDC"          #NB_DDC
    MSDDRXTYPE_SW_DDC="SW_DDC"          #SW_DDC
    MSDDRXTYPE_FFT="FFT"                #FFT
    MSDDRXTYPE_SPC="SPC"                #FFT

    class registered_module(object):
        def __init__(self, registration_name, installation_name, channel_number):
            self.channel_number = channel_number
            self.installation_name = installation_name
            self.registration_name = registration_name
    class object_module(object):
        def __init__(self, registration_name, installation_name, channel_number, object_module = None):
            self.channel_number = channel_number
            self.installation_name = installation_name
            self.registration_name = registration_name
            self.object = object_module
    class msdd_channel_module(registered_module):
        def __init__(self, registration_name, installation_name, channel_number,rx_channel_num,coherent=False, msdd_rx_type="UNKNOWN"):
            self.channel_number = channel_number
            self.installation_name = installation_name
            self.registration_name = registration_name
            self.rx_channel_num = rx_channel_num
            self.coherent = coherent
            self.msdd_rx_type = msdd_rx_type
            
            self.analog_rx_object = None
            self.digital_rx_object = None
            self.output_object = None
            self.fft_object = None
            self.spectral_scan_object = None
            self.spectral_scan_wbddc_object = None
            
            self.rx_parent_object = None
            self.rx_child_objects=[]

        def get_msdd_type_string(self):
            return self.msdd_rx_type
        def update_output_object(self, output_object):
            self.output_object = output_object
        def has_streaming_output(self):
            return self.output_object != None
        def has_fft_object(self):
            return self.fft_object != None
        def has_spectral_scan_object(self):
            return self.spectral_scan_object != None
        
        
        
        
        def is_analog_only (self):
            return self.analog_rx_object != None and self.digital_rx_object == None
        def is_analog (self):
            return self.analog_rx_object != None
        def is_digital_only (self):
            return self.analog_rx_object == None and self.digital_rx_object != None
        def is_digital (self):
            return self.digital_rx_object != None
        def is_analog_and_digital (self):
            return self.analog_rx_object != None and self.digital_rx_object != None
        def is_hardware_based(self):
            return self.msdd_rx_type != MSDDRadio.MSDDRXTYPE_SW_DDC
        def is_fft(self):
            return self.msdd_rx_type == MSDDRadio.MSDDRXTYPE_FFT
        def is_spectral_scan(self):
            return self.msdd_rx_type == MSDDRadio.MSDDRXTYPE_SPC
        
        def get_registration_name_csv(self):
            reg_name_list = []
            if self.analog_rx_object != None:
                reg_name_list.append(self.analog_rx_object.object.get_full_reg_name())
            if self.digital_rx_object != None:
                reg_name_list.append(self.digital_rx_object.object.get_full_reg_name())
            if self.fft_object != None:
                reg_name_list.append(self.fft_object.object.get_full_reg_name())
            if self.spectral_scan_object != None:
                reg_name_list.append(self.spectral_scan_object.object.get_full_reg_name())
            if self.output_object != None:
                reg_name_list.append(self.output_object.object.get_full_reg_name())
            return create_csv(reg_name_list)
        def get_install_name_csv(self):
            install_name_list = []
            if self.analog_rx_object != None:
                install_name_list.append(self.analog_rx_object.installation_name)
            if self.digital_rx_object != None:
                install_name_list.append(self.digital_rx_object.installation_name)
            if self.fft_object != None:
                install_name_list.append(self.fft_object.installation_name)
            if self.spectral_scan_object != None:
                install_name_list.append(self.spectral_scan_object.installation_name)
            if self.output_object != None:
                install_name_list.append(self.output_object.installation_name)
            return create_csv(install_name_list)
        
        
   
    
    """Class to manage a single MSDD-X000 radio instance"""
    """Note that this class is just a container for the individual modules"""
    """The module instances should be accessed directly for command and control"""
    def __init__(self, address, port, udp_timeout=0.25, validation_polling_time=0.25, validation_number_retries = 0):

        #Set up sockets
        self.radioAddress = (address, int(port))
        self.com = sockCom(self.radioAddress,udp_timeout)
        
        self.validation_polling_time = validation_polling_time
        self.validation_number_retries = validation_number_retries
        
        #Available Module lists (sorted by installation name)
        self.console_modules = []                           #CON
        self.stream_modules = []                            #SRT
        self.board_modules = []                             #BRD
        self.network_modules = []                           #NETWORK
        self.software_ddc_modules = []                      #SWDDCDEC2
        self.iq_circular_buffer_modules = []                #IQB
        self.out_modules = []                               #OUT
        self.tod_modules = []                               #TOD
        self.spectral_scan_modules = []                     #SPC
        self.fft_modules = []                               #FFT
        self.log_modules = []                               #LOG
        self.nb_ddc_modules = []                            #WBDDC
        self.wb_ddc_modules = []                            #NBDDC
        self.msdrx000_modules = []                          #MSDR3000
        self.msdr_rs422_modules = []                        #MSDR_RS422
        self.tfn_modules = []                               #TFN
        self.gps_modules = []                               #GPS
        
        
        #setup the basic modules    
        self.console_module = ConsoleModule(self.com)       #CON
        self.stream_router = StreamRouterModule(self.com)   #SRT
        
        # Determine mapping version
        mapping_version = 2
        batch_filename = self.console_module.getFilenameBatch()
        m = re.search('_Gen(\d+)_Map',batch_filename)
        if m:
            mapping_version = int(m.group(1))
        
        
        # Get a hash of all installed modules. Key: Install Name, Value: list of all corresponding modules
        self.registration_hash={}
        for module in self.stream_router.module_list:
            try:
                reg_name = self.stream_router.getRegistrationName(module)
                if self.registration_hash.has_key(module):
                    continue
                ch_list = self.stream_router.getChannelList(reg_name)               
                self.registration_hash[module] = []
                for channel in ch_list:
                    reg_full = str(reg_name) + ':' + str(channel)
                    inst_name = self.stream_router.getInstallName(reg_full)
                    self.registration_hash[module].append(self.registered_module(reg_name,inst_name,channel))
            except:
                None
                
        self.fft_object_container = object_container()
        self.spectral_scan_object_container = object_container()
        self.output_object_container = object_container()
        
        self.rx_channels = []
        self.fullRegName_to_RxChanNum = {}
        self.module_dict = {}
        for inst_name,reg_mod_list in self.registration_hash.items():
            for reg_mod in reg_mod_list:
                try:
                    obj_mod = None
                    #print str(reg_mod.registration_name) + '[' + str(reg_mod.channel_number) + '] = ' + str(reg_mod.installation_name)
                    if inst_name == "CON":
                        obj= ConsoleModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.console_modules.append(obj_mod)
                        break #Do not need more than one console module
                    elif inst_name == "SRT":
                        obj= StreamRouterModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.stream_modules.append(obj_mod)
                    elif inst_name == "BRD":
                        obj= BoardModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.board_modules.append(obj_mod)
                    elif inst_name == "NETWORK":
                        obj= NetModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.network_modules.append(obj_mod)
                    elif inst_name == "SWDDCDEC2":
                        obj= SoftDdcBaseModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.software_ddc_modules.append(obj_mod)
                    elif inst_name == "IQB":
                        obj= IQCircularBufferModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.iq_circular_buffer_modules.append(obj_mod)
                    elif inst_name == "OUT":
                        obj= OUTModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.out_modules.append(obj_mod)
                        self.output_object_container.put_object(obj_mod)
                    elif inst_name == "TOD":
                        obj= TODModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.tod_modules.append(obj_mod)
                    elif inst_name == "SPC":
                        obj= SpectralScanModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.spectral_scan_modules.append(obj_mod)
                        self.spectral_scan_object_container.put_object(obj_mod)
                    elif inst_name == "FFT":
                        obj= FFTModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.fft_modules.append(obj_mod)
                        self.fft_object_container.put_object(obj_mod)
                    elif inst_name == "LOG":
                        obj= LOGModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.log_modules.append(obj_mod)
                    elif inst_name == "WBDDC":
                        obj= WBDDCModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.wb_ddc_modules.append(obj_mod)
                    elif inst_name == "NBDDC":
                        obj= NBDDCModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.nb_ddc_modules.append(obj_mod)
                    elif inst_name == "MSDR3000":
                        obj= MSDDX000_RcvModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.msdrx000_modules.append(obj_mod)
                    elif inst_name == "MSDR_RS422":
                        obj= RS422_RcvModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.msdr_rs422_modules.append(obj_mod)
                    elif inst_name == "TFN":
                        obj= TFNModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.tfn_modules.append(obj_mod)
                    elif inst_name == "GPS":
                        obj= GpsNavModule(self.com,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod.registration_name,reg_mod.installation_name,reg_mod.channel_number,obj)
                        self.gps_modules.append(obj_mod)
                    
                    if obj_mod != None:
                        full_reg_name = obj_mod.object.get_full_reg_name()
                        self.module_dict[full_reg_name] = obj_mod
                except NotImplementedError:
                    None       
        
                
        # CREATE RX_CHANNEL ARRAY
        for modules in [self.msdrx000_modules, self.msdr_rs422_modules]:
            for mod in modules:
                rx_mod = self.msdd_channel_module(mod.registration_name, mod.installation_name, mod.channel_number,len(self.rx_channels),True,self.MSDDRXTYPE_ANALOG_RX)
                rx_mod.analog_rx_object = mod
                current_position=len(self.rx_channels)
                if current_position < len(self.wb_ddc_modules):
                    rx_mod.msdd_rx_type = self.MSDDRXTYPE_DIGITAL_RX
                    rx_mod.digital_rx_object = self.wb_ddc_modules[current_position]
                    rx_mod.output_object = self.get_corresponding_output_module_object(rx_mod.digital_rx_object)
                    if rx_mod.output_object == None:
                        output_obj = self.output_object_container.get_object()
                        if output_obj == None:
                            print "ERROR: CAN NOT GET FREE OUTPUT OBJECT"
                            continue
                        self.stream_router.linkModules(rx_mod.digital_rx_object.object.full_reg_name, output_obj.object.full_reg_name)
                        self.output_object_container.mark_as_used(output_obj)
                        rx_mod.output_object = self.get_corresponding_output_module_object(rx_mod.digital_rx_object)
                self.update_rx_channel_mapping(rx_mod, len(self.rx_channels))
                self.rx_channels.append(rx_mod)
        
        for modules in [self.nb_ddc_modules]:
            for mod in modules:
                rx_mod = self.msdd_channel_module(mod.registration_name, mod.installation_name, mod.channel_number,len(self.rx_channels), True, self.MSDDRXTYPE_HW_DDC)
                rx_mod.digital_rx_object = mod
                output_obj = self.get_corresponding_output_module_object(mod)
                if output_obj == None:
                    output_obj_unused = self.output_object_container.get_object()
                    if output_obj_unused == None:
                        print "ERROR: CAN NOT GET FREE OUTPUT OBJECT"
                        continue
                    self.stream_router.linkModules(mod.object.full_reg_name, output_obj_unused.object.full_reg_name)
                    self.output_object_container.mark_as_used(output_obj_unused)
                else:
                    self.output_object_container.mark_as_used(output_obj)
                rx_mod.output_object = self.get_corresponding_output_module_object(mod)
                self.update_rx_channel_mapping(rx_mod, len(self.rx_channels))
                self.rx_channels.append(rx_mod)
                
        for modules in [self.software_ddc_modules]:
            for mod in modules:
                rx_mod = self.msdd_channel_module(mod.registration_name, mod.installation_name, mod.channel_number,len(self.rx_channels), False, self.MSDDRXTYPE_SW_DDC)
                rx_mod.digital_rx_object = mod
                output_obj = self.get_corresponding_output_module_object(mod)
                if output_obj == None:
                    output_obj_unused = self.output_object_container.get_object()
                    if output_obj_unused == None:
                        print "ERROR: CAN NOT GET FREE OUTPUT OBJECT"
                        continue
                    self.stream_router.linkModules(mod.object.full_reg_name, output_obj_unused.object.full_reg_name)
                    self.output_object_container.mark_as_used(output_obj_unused)
                else:
                    self.output_object_container.mark_as_used(output_obj)
                rx_mod.output_object = self.get_corresponding_output_module_object(mod)
                self.update_rx_channel_mapping(rx_mod, len(self.rx_channels))
                self.rx_channels.append(rx_mod)
                
        for modules in [self.fft_modules]:
            for mod in modules:
                self.stream_router.unlinkModule(mod.object.full_reg_name)
                output_obj = self.get_corresponding_output_module_object(mod)
                if output_obj == None:
                    output_obj_unused = self.output_object_container.get_object()
                    if output_obj_unused == None:
                        print "ERROR: CAN NOT GET FREE OUTPUT OBJECT"
                        continue
                    self.stream_router.linkModules(mod.object.full_reg_name, output_obj_unused.object.full_reg_name)
                    self.output_object_container.mark_as_used(output_obj_unused)
                else:
                    self.output_object_container.mark_as_used(output_obj)
                rx_mod.output_object = self.get_corresponding_output_module_object(mod)
                
                rx_mod = self.msdd_channel_module(mod.registration_name, mod.installation_name, mod.channel_number,len(self.rx_channels), False, self.MSDDRXTYPE_FFT)
                rx_mod.fft_object = mod
                self.update_rx_channel_mapping(rx_mod, len(self.rx_channels))
                self.rx_channels.append(rx_mod)

        for modules in [self.spectral_scan_modules]:
            for mod in modules:
                modList = self.stream_router.getModulesFlowListBySource(mod.object.full_reg_name)
                 
                self.stream_router.unlinkModule(mod.object.full_reg_name)
                output_obj = self.get_corresponding_output_module_object(mod)
                 
                #Need a RCV, WBDDC, OUT and SPC for this
                #Check if we already have an output connected
                if output_obj == None:
                    output_obj = self.output_object_container.get_object()
                    if output_obj == None:
                        print "ERROR: CAN NOT GET FREE OUTPUT OBJECT"
                        continue
                    self.stream_router.linkModules(mod.object.full_reg_name, output_obj.object.full_reg_name)
 
                #Shortcut, make the SPC number == RCV/WBDDC number                 
                #Check to see if we already have a mapping for RCV and WBDDC
                rcv_str = MSDDX000_RcvModule.MOD_NAME_MAPPING[mapping_version]
                wbddc_str = WBDDCModule.MOD_NAME_MAPPING[mapping_version]
                 
                def findModuleNumber(mods, modname):
                    for mod in mods:
                        m = re.search('%s:(\d+)' % modname,mod)
                        if m:
                            return int(m.group(1))
                    return None
                 
                rcv_num = findModuleNumber(modList, rcv_str)
                wbddc_num = findModuleNumber(modList, wbddc_str)
                spc_num = findModuleNumber(modList, "SPC")
                 
                if rcv_num == None:
                    rcv_num = spc_num + 1
                if wbddc_num == None:
                    wbddc_num = spc_num
                 
                self.stream_router.linkModules("%s:%d" % (rcv_str, rcv_num), "%s:%d %s:%d %s" % (wbddc_str, wbddc_num, "SPC", spc_num, output_obj.object.full_reg_name))
                 
                self.output_object_container.mark_as_used(output_obj)
                 
                rx_mod = self.msdd_channel_module(mod.registration_name, mod.installation_name, mod.channel_number,len(self.rx_channels), False, self.MSDDRXTYPE_SPC)
                rx_mod.msdd_rx_type = self.MSDDRXTYPE_SPC
                
                wbddc_obj = None
                for wbddc in self.wb_ddc_modules:
                    if wbddc_str == wbddc.registration_name and int(wbddc_num) == int(wbddc.channel_number):
                        wbddc_obj = wbddc 
                        break
                
                rx_mod.spectral_scan_wbddc_object = wbddc_obj 
                rx_mod.spectral_scan_object = mod
                rx_mod.output_object = output_obj
                self.update_rx_channel_mapping(rx_mod, len(self.rx_channels))
                self.rx_channels.append(rx_mod)
                      
        # CREATE PARENT CHILD MAPPING FOR TUNERS
        for rx_pos in range(0,len(self.rx_channels)):
            self.rx_channels[rx_pos].rx_child_objects=[]
            #Find children
            if self.rx_channels[rx_pos].is_digital():
                self.rx_channels[rx_pos].digital_rx_object.object.setEnable(False)
            if self.rx_channels[rx_pos].is_fft():
                self.rx_channels[rx_pos].fft_object.object.setEnable(False)
            if self.rx_channels[rx_pos].is_spectral_scan():
                self.rx_channels[rx_pos].spectral_scan_object.object.setEnable(False)
            for mod in [ self.rx_channels[rx_pos].analog_rx_object, self.rx_channels[rx_pos].digital_rx_object ]:
                if mod == None: 
                    continue
                child_lst = self.stream_router.getModulesFlowListBySource(mod.object.full_reg_name)
                for child in child_lst:
                    pos = self.get_rx_channel_position_from_registration_name(child)
                    if pos == None or pos >= len(self.rx_channels) or rx_pos == pos:
                        continue
                    self.rx_channels[rx_pos].rx_child_objects.append(self.rx_channels[pos])
                    self.rx_channels[pos].rx_parent_object = self.rx_channels[rx_pos]
 
#         print "Number of FFT Modules: " + str(len(self.fft_modules))
#         print "Number of FFT Modules Used: " + str(len(self.fft_object_container.used_objects))
#         print "Number of FFT Modules Free: " + str(len(self.fft_object_container.free_objects))
#  
#         print "Number of OUT Modules: " + str(len(self.out_modules))
#         print "Number of OUT Modules Used: " + str(len(self.output_object_container.used_objects))
#         print "Number of OUT Modules Free: " + str(len(self.output_object_container.free_objects))
#  
#         print "Number of SPC Modules: " + str(len(self.spectral_scan_modules))
#         print "Number of SPC Modules Used: " + str(len(self.spectral_scan_object_container.used_objects))
#         print "Number of SPC Modules Free: " + str(len(self.spectral_scan_object_container.free_objects))
#  
#         for outPos in range(0,64):
#             fn = "OUT:" + str(outPos)
#             flow = ""
#             try:
#                 flow = self.stream_router.getModulesFlowListByDestination(fn)
#             except:
#                 None
#             print fn + " -- " + str(flow)
#              
#         print "MAPPING: "
#         for rx_pos in range(0,len(self.rx_channels)):
#             print "TUNER: " + str(rx_pos)
#             if self.rx_channels[rx_pos].rx_parent_object == None:
#                 print "\tParent: NONE"
#             else:
#                 print "\tParent: " + str(self.rx_channels[rx_pos].rx_parent_object.rx_channel_num)
#              
#             chld_lst = []
#             for child in self.rx_channels[rx_pos].rx_child_objects:
#                 chld_lst.append(child.rx_channel_num)
#             print "\tChildren: " + str(chld_lst)
#         pprint.pprint(self.fullRegName_to_RxChanNum)
    
    def update_rx_channel_mapping(self, rx_chan_mod, position):
        if rx_chan_mod == None:
            return
        if rx_chan_mod.analog_rx_object != None:
            self.fullRegName_to_RxChanNum[rx_chan_mod.analog_rx_object.object.full_reg_name] = position
        if rx_chan_mod.digital_rx_object != None:
            self.fullRegName_to_RxChanNum[rx_chan_mod.digital_rx_object.object.full_reg_name] = position
        if rx_chan_mod.output_object != None:
            self.fullRegName_to_RxChanNum[rx_chan_mod.output_object.object.full_reg_name] = position
        if rx_chan_mod.fft_object != None:
            self.fullRegName_to_RxChanNum[rx_chan_mod.fft_object.object.full_reg_name] = position
        if rx_chan_mod.spectral_scan_object != None:
            self.fullRegName_to_RxChanNum[rx_chan_mod.spectral_scan_object.object.full_reg_name] = position
    def add_child_tuner(self, parent, child):
        if child.digital_rx_object == None or parent.digital_rx_object == None:
            return False
        if child.rx_parent_object != None:
            child.rx_parent_object.rx_child_objects.remove(child)
        child.rx_parent_object = parent
        parent.rx_child_objects.append(child)
        
        child_reg_name = child.digital_rx_object.object.full_reg_name
        parent_reg_name = parent.digital_rx_object.object.full_reg_name

        self.stream_router.linkModules(parent_reg_name,child_reg_name)
        return True
    
    def get_root_parent_tuner_num(self, rx_chan_mod):
        parent = self.get_parent_tuner_num(rx_chan_mod)
        last_parent = parent
        while parent != None:
            parent = self.get_parent_tuner_num(last_parent)
            if parent == None:
                return last_parent
            last_parent = parent
        return last_parent
    
    def get_parent_tuner_num(self, rx_chan_mod):
        if rx_chan_mod == None or rx_chan_mod.rx_parent_object == None:
            return None
        return rx_chan_mod.rx_parent_object.rx_channel_num
    
    def get_child_tuner_num_list(self, rx_chan_mod):
        if rx_chan_mod == None or len(rx_chan_mod.rx_child_objects) <= 0:
            return []
        chld_lst = []
        for child in rx_chan_mod.rx_child_objects:
            chld_lst.append(child.rx_channel_num)
        return chld_lst
        
    def get_rx_channel_position_from_registration_name(self, full_reg_name):
        if self.fullRegName_to_RxChanNum.has_key(full_reg_name):
            return self.fullRegName_to_RxChanNum[full_reg_name]
        return None
    
    def get_corresponding_output_module_object(self,object_module):
        if object_module == None:
            return None
        try:
            full_reg_name = object_module.object.get_full_reg_name()
            output_num = self.stream_router.getOutputChannelForModule(full_reg_name)
            if output_num == None:
                return None
            for out_pos in range(0,len(self.out_modules)):
                if output_num == self.out_modules[out_pos].channel_number:
                    return self.out_modules[out_pos]
        except:
            return None
        return None
    
    def get_corresponding_fft_module_object(self,object_module):
        if object_module == None:
            return None
        try:
            full_reg_name = object_module.object.get_full_reg_name()
            output_num = self.stream_router.getFFTChannelForModule(full_reg_name)
            if output_num == None:
                return None
            for out_pos in range(0,len(self.fft_modules)):
                if output_num == self.fft_modules[out_pos].channel_number:
                    return self.fft_modules[out_pos]
        except:
            return None
        return None    
    
    def get_stream_flow(self,object_module):
        if object_module == None:
            return ""
        try:
            full_reg_name = object_module.object.get_full_reg_name()
            flow_list = self.stream_router.getModulesFlowListByDestination(full_reg_name)
            return create_csv(flow_list)
        except:
            return ""
        return ""



if __name__ == '__main__':
    address = ''#raw_input('IP Address of Radio (127.0.0.1): ')
    if (address == ''):
        address = '127.0.0.1'
    port = ''#raw_input('Port of Radio (23): ')
    if (port == ''):
        port = '23'
    radio = MSDDRadio(address, port)
