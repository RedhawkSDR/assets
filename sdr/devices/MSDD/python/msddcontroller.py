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
import copy
import socket
import select
import string
import unittest
import random
import thread
import math
import inspect 
import re
import pprint
import time
import traceback
import threading
import datetime

class ConnectionFailure(Exception):
    pass

class CommandException(Exception):
    pass

class TransmitFailure(Exception):
    pass

class EchoFailure(Exception):
    pass

class InvalidValue(Exception):
    pass

#
# Helpers
#

def timestamp_msg(msg):
    """
    time stamp a debug message
    """
    ts=datetime.datetime.now()
    return "{}:{}:{}.{} {}".format(ts.hour,ts.minute,ts.second,ts.microsecond,msg)

def create_csv(val_list):
    """
    Create comma separate list of values
    """
    return ','.join([ str(x) for x in val_list])

def expand_bitmask(bit_mask, lsb=True, ltype=int):
    """
    expand a bit mask value into a list of 1 and 0. ltype
    defaults to int but could be str
    """
    blist=[ ltype(x) for x in "{0:b}".format(bit_mask)]
    # put lsb bit in list first
    if lsb:
        blist.reverse()
    return blist

def get_value_valid_list(value,value_tolerance_perc, values, return_max=None):
    """
    Validate that value is in a list of values is within a given tolerance. Search for
    value equal to or closest to value+(value*1.tolerance_percentage). If no value
    is provided then return the min or max value in the list specified by return_max

    Parameters:
    value : value to search for
    value_tolerance_perc : percentage tolerance to use when searching 0..100
    values : list of valid values
    return_max: if no search value give returns min (None/False) or max (True) value

    Returns:
    --------
    If value provided, returns a matching or closest matching value in the list or
    throws InvalidValue exception
    If no value provided returns minimum or maximum in list of values
    """
    sorted_list = sorted(values)
    idx=0
    if return_max: idx=-1
    # just return minimum value or maximum value in the list
    if value == None or value <= 0.0:
        if len(sorted_list) > 0:
            return sorted_list[idx]
        raise InvalidValue("Valid value not found! Requested Value was %s" %value)
    max_tolerance_val=None
    if value_tolerance_perc and value_tolerance_perc > 0.0:
        max_tolerance_val = value + value*(value_tolerance_perc/100.0)
    for num in range(0,len(sorted_list)):
        if sorted_list[num] < value:
            continue
        if sorted_list[num] >= value and ( max_tolerance_val is None or sorted_list[num] <= max_tolerance_val):
            return sorted_list[num]
        if sorted_list[num] > max_tolerance_val:
            continue

    raise InvalidValue("Invalid value, requested value: %s (tolerance:%s%%) not in range: %s " % (value,value_tolerance_perc,','.join([str(x) for x in values])))


def check_value_from_values( value, _get_values_list):
    """
    search for a value in a list an return the position of the value in the list. If values is not
    found returns InvalidValue.

    Parameters:
    ----------
    value : value to search for
    _get_values_list : callable that generates a list or an actual list

    Returns:
    index of value in list or throw InvalidValue

    """
    if callable(_get_values_list):
        vlist=_get_values_list()
    else:
        vlist=_get_values_list
    try:
        idx=vlist.index(value)
    except:
        raise InvalidValue("Item not found in list, item <" + str(value) + "> list " + ",".join([ str(x) for x in vlist]) )
    return True

class Connection(object):
    """Class to actually communicate with the radio, NB there should only be one of these per radio"""

    Success=0
    TimeOut=-1
    Error=-2
    MsgEnd='\n'

    def __init__(self, address, timeout=0.25,enable_trace=False):
        """
        Attributes:
        ----------
        _debug : turn on print statements
        __mutexLock : synchronize access to the device
        radioAddress : (ip, port) tuple
        radioSocket : socket to write and read messages

        Parameters:
        ----------
        address: tuple (ip, port)
        timeout: default time in seconds to wait when receiving a message
        """
	self._debug=False
        self.__mutexLock = threading.Lock()
        self.radioAddress = address
        self.radioSocket=None
        self.trace_on_comm_failure=enable_trace
        self.pause=False
        self.connect(timeout)

    def set_trace_failure(self, onoff):
        self.trace_on_comm_failure=onoff

    def connect(self, timeout):
        """
        Open a UDP socket connection for communicating to the radio. If the socket was
        open then close and reset.

        Parameters:
        ----------
        timeout : timeout in seconds when receiving messages from the radio

        """
        self.timeout=timeout
        if self.radioSocket:
            self.radioSocket.close()
            self.radioSocket = None

        #generate socket for sending/receiving UDP packets to/from radio
        try:
            self.__mutexLock.acquire()
            if self._debug:
                self.log_msg("Opening connection to {}".format(self.radioAddress))
            self.radioSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        except:
            traceback.print_exc()
            self.radioSocket=None
        finally:
            self.__mutexLock.release()


    def _reconnect(self):
        """
        Perform close/open operation on the socket, does not lock access
        """
        if self.radioSocket:
            self.radioSocket.close()
            self.radioSocket = None

        #generate socket for sending/receiving UDP packets to/from radio
        try:
            self.radioSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        except:
            self.radioSocket=None


    def disconnect(self):
        """
        Close the socket connection to the radio.

        """
        try:
            self.__mutexLock.acquire()
            if self.radioSocket:
                self.radioSocket.close()
                self.radioSocket = None
        finally:
            self.radioSocket=None
            self.__mutexLock.release()

    def set_timeout(self, timeout):
        """
        Sets the timeout attribute

        Parameters:
        -----------
        timeout - new timeout value
        """
        self.__mutexLock.acquire()
        self.timeout=timeout
        self.radioSocket.settimeout(timeout)
        self.__mutexLock.release()
        
    def flush(self, retries=None):
        self.__mutexLock.acquire()
        try:
            self._flush(retries)
        finally:
            self.__mutexLock.release()
            return

    def _flush(self, retries=None):
        ret=False
        if retries is None: retries=1
        self.radioSocket.sendto("\n", self.radioAddress)
        while retries != 0:
            self.radioSocket.settimeout(self.timeout/10.0)
            rmsg=self.radioSocket.recv(65535)
            if len(rmsg) > 0 : ret=True
            retries-=1
        return ret

    def log_msg(self,msg):
        print timestamp_msg(msg)

    def sendStringCommand(self, command, check_for_output=True, expect_output=True):
        """
        Sends command to the radio and return response

        Parameters:
        -----------
        command : command string to send to radio
        check_for_output : check for output after command echo is processed
        expect_output : when checking for output that we expected reply message
                        if not, then raise CommandException

        Returns:
        --------
        resp : response string from radio
        """
        self.__mutexLock.acquire()            # synchronize access to the radio

        _timeout=self.timeout                 # configured timeout
        _echo_timeout=_timeout/5.0            # partial wait for echo response
        _debug=self._debug                    # turn on debug messages

        _cmd=command.replace('\n',' ').strip()   # remove newlines for debug messaging
        retries=3                                # retries controls send/echo sequence if timeout occurs
        returnMsg=None
        try:
            #
            # performing retry method for badly behaving radios
            #
            while retries > 0:
                try:  
                    
                    if self._debug:
                        self.log_msg("--SEND-- cmd <{}> radio ({}) timeout({}) echo({}) check({}) expect({}) retries({}) orig debug({}) ".format(_cmd,self.radioAddress,_timeout,_echo_timeout,check_for_output, expect_output,retries,_debug))                        
                    returnMsg = self._sendStringCommand(command, _timeout, _echo_timeout, check_for_output, expect_output)
                    return returnMsg
                except (TransmitFailure, EchoFailure) as e :
                    
                    #
                    # Radio is not responding to commands increase timeout and try again
                    #
                    
                    #
                    # turn on debug messages for diagnostic purposes
                    #
                    self._debug=True

                    #
                    # double the default timeouts for send/recv sequence of a command
                    #
                    _timeout=_timeout*2
                    _echo_timeout=_timeout/2.0
                    retries-=1 

                    _e_msg="Echo"
                    if type(e) == TransmitFailure : _e_msg = "Transmit"
                    
                    if self._debug:
                       self.log_msg("--WARN-- {} Error, Radio not responsive, cmd <{}> radio ({}) timeout ({}) echo({}) check({}) expect({}) retries({}) ".format(_e_msg, _cmd,self.radioAddress,_timeout,_echo_timeout,check_for_output, expect_output,retries))
                    time.sleep(0.09)
                    try:
                        self._flush()
                    except socket.timeout:
                        pass
                    if retries == 0:
                        self.log_msg("--WARN-- {} Error, Radio not responsive, (cmd failure <{}> radio ({}) check({}) expect({}) ex({}))".format(_e_msg,_cmd,self.radioAddress,check_for_output, expect_output,type(e)))
                        raise ConnectionFailure("Command IO ({}) failure, cmd <{}> radio ({})".format(_e_msg,_cmd,self.radioAddress))
            
        except socket.timeout as e :
            if expect_output:
                if self._debug:
                    self.log_msg("Socket timed out, processing cmd <{}> radio ({}) may not be responsive. (check:{} expect:{})".format(_cmd,self.radioAddress,check_for_output,expect_output))
                raise ConnectionFailure(self._except_msg)
        except socket.error as e :
            self.log_msg("Socket error, cmd {} radio({}) may not be responsive, reconnect".format(_cmd,self.radioAddress))
            self._reconnect()
            raise e
        finally:
            # reset initial values for timeout and debug
            self._debug=_debug
            self.__mutexLock.release()
            
        return returnMsg


    
    def _sendStringCommand(self, command, timeout, echo_timeout, check_for_output=True, expect_output=True):
        """Sends command to the radio and process response

        Parameters:
        -----------
        command : command string to send to radio
        check_for_output : check for output after command echo is processed
        expect_output : when checking for output that we expected reply message
                        if not, then raise CommandException

        Returns:
        --------
        resp : response string from radio
        """
        self._except_msg=""
        _cmd=command.replace('\n',' ').strip()
        _timeout=timeout

        if self._debug:
            self.log_msg("--DEBUG-- Sending to radio:{0} command <{1}> timeout:{2} check:{3} expect:{4}".format(self.radioAddress,_cmd,timeout,check_for_output,expect_output))

        # set exception message
        self._except_msg="Socket timed out receiving echo for command <{}> radio ({})".format( _cmd,self.radioAddress)
        self.radioSocket.settimeout(_timeout)
        try:
            self.radioSocket.sendto(command, self.radioAddress)
        except socket.timeout:
            raise TransmitFailure

        # process echo of command, should happen right away
        _expect_output=True
        try:
            _stime=time.time()
            _iready,_oready,_eready = select.select([self.radioSocket],[],[],echo_timeout)
            _etime=time.time()
            if self._debug:
                self.log_msg("--DEBUG--  ECHO select wait {:.7f} timeout:{} msg:{})".format(_etime-_stime,echo_timeout,_cmd))
            if _iready == None or len(_iready) == 0:
                raise socket.timeout

            _stime=time.time()
            echoMsg = self.radioSocket.recv(65535)
            _etime=time.time()
            if self._debug:            
                self.log_msg("--DEBUG--  ECHO recv time {:.7f}  msg:{})".format(_etime-_stime,_cmd))
        except socket.timeout:
                raise EchoFailure

        # setup for exceptions when processing response messages
        _expect_output = expect_output
        self._except_msg = "Socket timed out({}) when reading results for command <{}> radio ({})".format(timeout,_cmd,self.radioAddress)

        if self._debug:
            resp = re.sub(r'[^\x00-\x7F]+',' ', str(echoMsg))
            resp = resp.replace('\n',' ')
            self.log_msg("--DEBUG-- Echo from radio <{0}> (check for output={1})".format(resp,check_for_output))

        # early return, do not look for error conditions or response messages
        if not check_for_output:
            return ""

        # if we don't expect anything then no need to wait the entire time..
        _timeout=timeout
        if not expect_output: 
            _timeout=echo_timeout/4.0
        
        _stime=time.time()
        _iready,_oready,_eready = select.select([self.radioSocket],[],[],_timeout)
        _etime=time.time()
        if self._debug:
            self.log_msg("--DEBUG-- RESP select wait {:.7f} timeout:{} msg:{})".format(_etime-_stime,
                                                                                       _timeout,
                                                                                       _cmd))
        if _iready == None or len(_iready) == 0:
                raise socket.timeout

        _stime=time.time()
        returnMsg = self.radioSocket.recv(65535)
        _etime=time.time()
        if self._debug:        
            self.log_msg("--DEBUG-- RESP recv time {:.7f} msg:{})".format(_etime-_stime,_cmd))
                
        self._except_msg=""
        # clean up return message
        returnMsg = re.sub(r'[^\x00-\x7F]+',' ', str(returnMsg))
        returnMsg = returnMsg.replace('\n',' ').rstrip()
        if self._debug:
            self.log_msg("--DEBUG-- Response from command : {} (check for output:{}) radio {})".format(returnMsg,check_for_output,self.radioAddress))

        # check for unexpected output
        if not expect_output and len(returnMsg) > 0:
            err_str =  "Unexpected output received \"{}\" from command:{} radio {}".format(returnMsg,_cmd,self.radioAddress)
            if self._debug:
                print err_str
            # send newlines to reset command processing
            self._flush()
            raise CommandException(err_str)

        return returnMsg


class baseModule(object):
    """Base module class for all MSDD modules

    Attributes:
    ----------
    MOD_NAME_MAPPING : naming convention for modules for either Gen1 or Gen2 application code

    connection : connection object to use when sending messages to radio
    channel_number : channel number assigned by radio to a module
    mapping_version : if gen1 or gen2 mapping is used ( index into MOD_MAP_NAME
    update_mapping_version : sets the module name and full_reg_name
    full_reg_name : full module registration name e.g. RCV:1

    """
    MOD_NAME_MAPPING={1:None,2:None}
    
    def __init__(self, connection, channel_number=0, mapping_version=2):
        """

        Parameters:
        ----------
        connection : connection to use for communicating with MSDD radio module
        channel_number : channel number associated with the module instance
        mapping_version : naming convention used to
        """
        self.connection = connection
        self.channel_number = channel_number
        self.mapping_version = mapping_version
        self.update_mapping_version(self.mapping_version)
        self.full_reg_name = str(self.module_name) + ":" + str(self.channel_number)
        self._debug=False

    def log_msg(self,msg):
        print timestamp_msg(msg)
        
    def get_full_reg_name(self):
        """
        Return module instance as identified by the radio, usually module:channel_number
        """
        return self.full_reg_name

    def channel_id(self):
        return self.full_reg_name

    def get_channel_number(self):
        return self.channel_number

    def get_module_name(self, mapping_version):
        if not self.MOD_NAME_MAPPING.has_key(int(mapping_version)):
            raise NotImplementedError, "MODULE NAME CORRESPONDING TO VERSION " + str(mapping_version) + "DOES NOT EXIST"
        if self.MOD_NAME_MAPPING[int(mapping_version)] == None:
            raise NotImplementedError, "MODULE NAME CORRESPONDING TO VERSION " + str(mapping_version) + " IS EMPTY!"
        return self.MOD_NAME_MAPPING[int(mapping_version)]    

    def update_mapping_version(self, mapping_version):
        self.module_name = self.get_module_name(mapping_version)
        self.full_reg_name = str(self.module_name) + ":" + str(self.channel_number)

    def send_custom_command(self,command_str, check_for_output=True):
        res = self.connection.sendStringCommand(str(command_str),check_for_output=check_for_output)
        return res

    def make_command(self,command, args = "", query=None ):
        query_str=""
        if query: query_str="?"
        command_str = command.strip()
        args_str=""
        if len(args.strip()) > 0:
            args_str = " {}".format(args.strip());
        return "{}:{} {}{}{}\n".format(self.module_name,self.channel_number,command_str,query_str,args_str)

    def send_query_command(self,command_str,arg1_str="", check_for_output=True):
        command=self.make_command(command_str,arg1_str,True)
        res = self.connection.sendStringCommand(command,
                                          check_for_output=check_for_output)
        if self._debug:
            self.log_msg( "send_query_command (completed) command {} response {}".format(command.replace('\n',' '),
                                                                                         res))

        if self.connection.pause:
            time.sleep(.008)
        return res

    def send_set_command(self,command_str, arg1_str="", check_for_output=True, expect_output=False):
        command=self.make_command(command_str,arg1_str)
        res=None
        res = self.connection.sendStringCommand(command,
                                                check_for_output=check_for_output,
                                                expect_output=expect_output)

        if self._debug:
            self.log_msg("send_set_command (completed) command {} response {} ".format(command.replace('\n',' '),
                                                                                       res))

        if self.connection.pause:
            time.sleep(.008)

        return res

    def parseResponse(self, resp, items = -1, sep=','):
        """Parses a string response from the radio and returns #items elements

        Parameters:
        ----------
        resp : response message to parse
        items : number of items to expect after parsing last token in
                response message ( -1 == don't care)
        sep : split text of last token on separator character
        """
        #strip trailing newline, split on spaces and take last element, then split on commas
        resp = re.sub(r'[^\x00-\x7F]+',' ', str(resp))
        resp = str(resp).strip()
        resp = resp.rstrip()
        resp_split_ws = resp.split()

        # check for error condition
        if re.search(r'.* ERR .*',resp):
            err_str = "Error condition returned, message " + resp
            if self._debug:
                print err_str
            raise CommandException(err_str)

        # check for minimum number of arguments
        if len(resp_split_ws) < 2:
            raise CommandException("Invalid response detected: " + str(resp))

        retVal_list=[]
        retVal = resp_split_ws[-1]
        retVal_list = retVal.rsplit(sep,items)
        if self._debug:
            print "parseResponse  response ", resp, " retval(split ws) ",  retVal, " return list ", retVal_list
        if items >= 0 and len(retVal_list) != items:
            raise CommandException("Could not parse reponse message for item check " + str(items) + " response \"" + str(resp) + "\" AS DESIRED.")
        return retVal_list

    def parseResponseSpaceOnly(self, resp, items = -1):
        """Parses a string response from the radio and returns #items elements"""
        #strip trailing newline, split on spaces 
        resp = re.sub(r'[^\x00-\x7F]+',' ', str(resp))
        resp = str(resp).strip()
        resp = resp.rstrip()
        resp_split_ws = resp.split()
        if self._debug:
            print "parseResponseSpaceOnly  response ", resp, " return list ", resp_split_ws
        if len(resp_split_ws) < 2:
            raise Exception("UNKNOWN ERROR HAS BEEN DETECTED: " + str(resp))
        if resp_split_ws[len(resp_split_ws)-2] == "ERR":
            raise Exception("ERROR HAS BEEN DETECTED: " + str(resp))      
        if  len(resp_split_ws) != items:
            raise Exception("COULD NOT PARSE OUTPUT \"" + str(resp) + "\"AS DESIRED.")
        return resp_split_ws

    def _setEnable(self, enable):
        arg="0"
        if enable: arg="1"
        self.send_set_command("ENB",str(arg))

    def create_range_string(self,min,max,step,sep="::"):
        return str(min) + str(sep) +  str(step) + str(sep) +  str(max)

    def create_csv(self,val_list):
        return create_csv(val_list)

    def create_desc_val_csv(self, desc_list, val_list):
        if len(desc_list) == 0: return ""
        mlen=min(len(desc_list), len(val_list))
        dlist=desc_list[:mlen]
        vlist=val_list[:mlen]
        return ','.join( str(x[0])+":"+str(x[1]) for x in zip(dlist,vlist))

    def decode_bit_mask(self, bit_mask, bit_names):
        blist = expand_bitmask(bit_mask)
        return ','.join( [ bit_names[x[0]] for x in enumerate(blist) if x[1] == 1 ])

    def process_bit_mask(self, _get_mask, _get_mask_names ):
        bmask=_get_mask
        if callable(_get_mask):
            bmask=_get_mask()
        bnames=_get_mask_names()
        return self.decode_bit_mask(bmask,bnames)

    def in_range(self, value, min_max_step ):
        try:
            step=1
            _min_max_step=min_max_step
            if callable(min_max_step): _min_max_step = min_max_step()
            _min = int(_min_max_step.split(':')[0])
            _max = int(_min_max_step.split(':')[1])
            step = int(_min_max_step.split(':')[2])
        except:
            raise Exception("Range specification error, range " +str(min_max_step))

        if step == 0 : step=1
        if self._debug:
            print "in range   ", value, " min ", _min, " max ",  _max, " modulo ", value%step
        if not (value >= _min and value<= _max and (value%step == 0)) :
            raise InvalidValue("Value not in range with limits, value " +str(value) + " range " +str(min_max_step))

    
    def get_value_valid(self,value,tolerance_per, min_val, max_val, step_val=1,closest_ceil=False):
        if step_val <= 0:
            step_val = 0.25
        valid = False
        try:
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
        except TypeError:
            raise InvalidValue("Missing min/max values, Requested Value was %s" %value)
        if not valid:
            raise InvalidValue("Valid value not found! Requested Value was %s (max/min) %s/%s" % (value,max_val,min_val))
        
        return val

    def get_indexed_value(self, _get_index, _get_values_list, rtype=None):
        if callable(_get_index) :
            i = _get_index()
        else:
            i = _get_index
        vlist=_get_values_list()
        if i < len(vlist) :
            ret=vlist[i]
            if rtype: return rtype(ret)
            return ret
        else:
            return None

    def get_indexed_values(self, _get_indexes, _get_value_list, rtype=None):
        ilist = _get_indexes()
        vlist=_get_value_list()
        res=[ vlist[idx] for idx in ilist if idx < len(vlist) ]
        if rtype:
            return [ rtype(x)  for x in res ]
        else:
            return res

    def getter_with_default(self,_get_,default_value=None):
        try:
            ret = _get_()
            return ret
        except:
            if default_value != None:
                return default_value
            raise

    def get_value_valid_list(self,value,value_tolerance_perc, values, return_max=None):
        return get_value_valid_list(value, value_tolerance_perc, values, return_max)

    def setter_with_validation(self, value,
                               _set_,
                               _get_,
                               tolerance_range=None,
                               retries=None,
                               retry_interval=0.08):
        successful_validation = False
        counter = 0
        ret=None
        while successful_validation == False:
            try:
                if self._debug:
                    self.log_msg("setter_with_validation calling set >>>> {} ".format(value))
                _set_(value)
                ret = _get_()
                if self._debug:
                    self.log_msg("setter_with_validation calling get {} <<<<< ".format(ret))
            except NotImplementedError:
                return True
            except InvalidValue, e:
                raise e
            except CommandException as e:
                raise e
            except Exception:
                return False
            
            if tolerance_range != None:
                successful_validation = (value >= ret - tolerance_range and value <= ret + tolerance_range)
            else:
                successful_validation = (value == ret)

            counter += 1
            if successful_validation  or retries==None or counter > retries:
                if self._debug:
                    self.log_msg("setter_with_validation return {} value {} get value {}".format(
                        successful_validation, value, ret))
                return successful_validation
            time.sleep(retry_interval)

        if self._debug:
            self.log_msg("setter_with_validation valid {} value {} get value {}".format(
                        successful_validation, value, ret))            
        return successful_validation

    def _setEnable(self, enable):
        arg="0"
        if enable: arg="1"
        self.send_set_command("ENB",arg)

    def getEnable(self):
        resp = self.send_query_command("ENB")
        return (int(self.parseResponse(resp, -1,':')[0]) == 1)

    def setEnable(self, enable):
        return self.setter_with_validation(enable, self._setEnable, self.getEnable)

    enable = property(getEnable, setEnable, doc="Enable/Disable module")

#end class baseModule


class baseIPPModule(baseModule):
    def __init__(self, connection, channel_number=0, mapping_version=2, multi_interface=False):
        super(baseIPPModule,self).__init__(connection, channel_number,mapping_version)
        self._interface=None
        self._multi_interface=multi_interface
        try:
            connection=self.getIPP()
            if self._multi_interface:    # provide default interface to 0 it we have more than one
                self._interface=0
            if len(connection) == 3:     # check current IPP state to see if we are already assigned interface
                self._interface=int(connection[0])
                
        except:
            pass

    def _formatIPPargs(self,ip_address, port, interface):
        ipp_args = str(ip_address).strip() + ":" + str(port).strip()
        inf_arg=""
        if interface is None:
            # if the interface was determined from response or it supports interface addressing
            if self._interface is not None or self._multi_interface:
                _inf=self._interface
                # use interface from original query or default 0
                if _inf is None: _inf=0
                inf_arg="{}:".format(_inf)
        else:
            # used supplied interface to format ipp arguments
            inf_arg="{}:".format(interface)
            # radio does not require interface
            if self._interface is None:
                inf_arg=""

        ipp_args = "{}{}".format(inf_arg,ipp_args)
        return ipp_args

    def setAddressPort(self,ip_address, port, interface=None):
	ipp_args=self._formatIPPargs(ip_address,port,interface)
        self.setIPP(ipp_args)
        # validate return..and check for interface
        self.validateIPP(ipp_args)
        return True

    def validateIPP(self,ipp_args):
	ipp_resp=self.getIPP()
        ipp_set = ipp_args.split(":")
        if self._debug:
            print "baseIPPModule, validateIPP ipp_args: ", ipp_args, " result: ", ipp_resp

        ridx_end= -len(ipp_resp)-1
        ridx=-1;
        # start from the end of the response and validate return
        # possible return values from radio: ip:port, inf:ip:port
        for ridx, rvalue in zip(range(ridx,ridx_end,ridx),ipp_resp[::ridx]):
            if  abs(ridx) <= len(ipp_set) and rvalue != ipp_set[ridx]:
                raise InvalidValue("Set IPP to " + ipp_args + " failed" )
            # if interface was returned save off
            if ridx==-3: 
                if self._debug:
                    print "baseIPPModule, IPP interface returned ",int(ipp_resp[ridx])
                self._interface=int(ipp_resp[ridx])


    def setIPP(self,ipp_args):
        if self._debug:
            print "baseIPPModule, setIPP args: ", ipp_args
        self.send_set_command("IPP", ipp_args)

    def getIPP(self):
        """ Gets the  IP and UDP port setting for the module"""
        resp = self.send_query_command("IPP")
	response=[]
        try:
            response = self.parseResponse(resp, 3,':')
        except:
            response = self.parseResponse(resp, 2,':')
        return response
    
    def getIPPString(self):
        """ Gets the present IP and UDP port setting """
        resp = self.getIPP()
        return ':'.join( resp )
    
    def ___getIPP_Interface(self):
        resp=self.getIPP()
        try:
            ret=int(resp[-3])
        except:
            ret=0
        return ret

    def getIPP_Interface(self):
        ret=0
        if self._interface:
            ret=self._interface
        return ret

    def getIPP_IP(self):
        return self.getIPP()[-2]

    def getIPP_Port(self):
        return self.getIPP()[-1]

    connection_address = property(getIPPString)
    ip_address = property(getIPP_IP)
    ip_port = property(getIPP_Port)
    interface = property(getIPP_Interface)


class ConsoleModule(baseIPPModule):
    MOD_NAME_MAPPING={1:"CON",2:"CON"}
    """
    ConsoleModule supports the command set from the console module of the MSDD receivver

    """
    def __init__(self, connection, channel_number=0, mapping_version=2, multi_interface=False):
        """
        Create an instance of the ConsoleModule class

        Parameters:
        -----------
        connection : connection to use for send/recv of commands
        channel_number : channel id of the module
        mapping_version : what version of firmware mapping is used
        """
        super(ConsoleModule,self).__init__(connection, channel_number, mapping_version, multi_interface)

    def cmdPing(self):
        try:
            self.getIPP()
        except:
            return False
        return True

    def getIPPString(self):
        return super(ConsoleModule,self).getIPPString()

    def setAddressPort(self,ip_address, port, interface=None):
        raise CommandException("Cannot set ip address for console")

    def setIPP(self,ipp_args):
        raise CommandException("Cannot set ip address for console")        
    
    def getID(self):
        """Queries the radio for ID info, (Model,SN?,Load) """
        resp = self.send_query_command("IDN")
        return self.parseResponse(resp)

    def getIDString(self):
        resp = self.send_query_command("IDN")
        return ' '.join(self.parseResponse(resp))

    def getSoftwarePartNumber(self):
        return self.getID()[2];
    
    def setEcho(self, value):
        """Controls the echo response of the radio, boolean on/off"""
        arg=str(int(value))
        try:
            self.send_set_command("ECH",arg)
        except:
            traceback.print_exc()
            pass
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

    def getRFBoardType(self):
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
    address = property(getIPPString, doc="Address of console management port")
    sw_part_number = property(getSoftwarePartNumber, doc="Software Part Number ")
    echo = property(getEcho,setEcho, doc="Echo Status")
    cpu_load = property(getCpuLoad, doc="CPU Load")
    model = property(getModel,doc="Model name, example MSDD3000-PPS")
    serial = property(getSerial,doc="MSDD serial number")
    minFreq_Hz = property(getMinFreq_Hz,doc="Min tune frequency")
    maxFreq_Hz = property(getMaxFreq_Hz,doc="Max tune frequency")
    dspRef_Hz = property(getDspRef_Hz,doc="DSP reference frequency")
    adcClk_Hz = property(getAdcClk_Hz,doc="ADC clock rate")
    rf_board_type = property(getRFBoardType,doc="RF board type, example MSDR3000")
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

class LOGModule(baseIPPModule):
    MOD_NAME_MAPPING={1:"LOG",2:"LOG"}

    def __init__(self, connection, channel_number=0, mapping_version=2, multi_interface=False):
        super(LOGModule,self).__init__(connection, channel_number, mapping_version, multi_interface)

    # Logging Module commands
    def getIPPString(self):
        return super(LOGModule,self).getIPPString()

    def _setMask(self, mask):
        self.send_set_command("MSK",str(mask))

    def getMask(self):
        resp = self.send_query_command("MSK")
        return int(self.parseResponse(resp, 1)[0])

    def getMaskReadable(self):
        return self.process_bit_mask(self.getMask, self.getMaskList)

    def getMaskList(self):
        resp = self.send_query_command("MSKL")
        return self.parseResponse(resp)

    def setMask(self, mask):
        return self.setter_with_validation(int(mask), self._setMask, self.getMask)

    def _setEnable(self, enable):
        arg="0" 
        if enable: arg="1"
        self.send_set_command("ENB",arg)

    def getEnable(self):
        resp = self.send_query_command("ENB")
        return (int(self.parseResponse(resp, -1,':')[0]) == 1)

    def setEnable(self, enable):
        return self.setter_with_validation(enable, self._setEnable, self.getEnable)

    """Accessable properties"""
    ipp = property(getIPPString, doc="Logging module IP and UDP port")
    mask = property(getMask, setMask, doc="Logging Mask value")
    mask_readable = property(getMaskReadable)
    mask_list = property(getMaskList)
    enable = property(getEnable, setEnable, doc="Enable/Disable log channel")
    
#end class LOGModule



class BoardModule(baseModule):
    MOD_NAME_MAPPING={1:"BRD",2:"BRD"}
    
    def getMeter(self):
        resp = self.send_query_command("MTR")
        return self.parseResponse(resp)

    def getMeterList(self):
        resp = self.send_query_command("MTRL")
        return self.parseResponse(resp)

    def getMeterStr(self):
        return self.create_desc_val_csv(self.getMeterList(), self.getMeter())

    def getBIT(self):
        resp = self.send_query_command("BIT")
        return int(self.parseResponse(resp, 1)[0]) 

    def getBITStr(self,bit=None):
        if bit is None:
            return self.process_bit_mask( self.getBIT, self.getBITList )
        else:
            return self.process_bit_mask( bit, self.getBITList )

    def getBITList(self):
        resp = self.send_query_command("BITL")
        return self.parseResponse(resp)
    
    def _setExternalRef(self, ref):
        self.send_set_command("EXR",str(ref))

    def getExternalRef(self):
        resp = self.send_query_command("EXR")
        return int(self.parseResponse(resp, 1)[0]) 

    def getExternalRefList(self):
        resp = self.send_query_command("EXRL")
        return self.parseResponse(resp,3,':')

    def getExternalRefListStr(self):
        rl=self.getExternalRefList()
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))

    def setExternalRef(self, ref):
        return self.setter_with_validation(int(ref), self._setExternalRef, self.getExternalRef)

    def reset(self):
        self.send_set_command("RESET")
    
    
    """Accessable properties"""    
    bit = property(getBIT, doc="Bit error state for board module")
    bit_readable = property(getBITStr, doc="Bit error states (readable)")
    bit_list = property(getBITList, doc="Built in test results")
    meter = property(getMeter, doc="Get Meter Value")
    meter_readable = property(getMeterStr, doc="Get meter values as readable list")
    meter_list = property(getMeterList, doc="Get Meter Value")
    extRef = property(getExternalRef, setExternalRef, doc="External reference ")
    extRef_limits = property(getExternalRefListStr, doc="External reference limits")
    


class NetModule(baseModule):
    MOD_NAME_MAPPING={1:"NET",2:"NET"}
        
    def __init__(self, connection, channel_number=1, mapping_version=2):
        baseModule.__init__(self, connection, channel_number, mapping_version)
        self._bit_rate=None

    def _setIpAddress(self, ip_address):
        # save off enable state of network module
        _enb=self.getEnable()
        self.setEnable(False)
        self.send_set_command("IPP",str(ip_address))
        self.setEnable(_enb)

    def getIpAddress(self):
        resp = self.send_query_command("IPP")
        _ip=self.parseResponse(resp,1)[0]
        # if IPP command run on Network module returns leading zeros in ip address tuple
        _ip_addr=[]
        for x in _ip.split('.'):
            _ip_addr.append(x.lstrip('0'))
        return ".".join(_ip_addr)

    def setIpAddress(self, ip_address):
        return self.setter_with_validation(str(ip_address), self._setIpAddress, self.getIpAddress)
    
    def getMacAddr(self):
        resp = self.send_query_command("MAC")
        return self.parseResponse(resp,1)[0] 

    def getEnable(self):
        resp = self.send_query_command("ENB")
        return (int(self.parseResponse(resp, -1,':')[0]) == 1)   

    def getBitRate(self):
        if not self._bit_rate:
            resp = self.send_query_command("BRT")
            self._bit_rate = float(self.parseResponse(resp, 1)[0]) 
        return self._bit_rate

    # property access to set/get methods
    ip_address = property(getIpAddress, setIpAddress, doc="")
    mac_addr = property(getMacAddr, doc="")
    bit_rate = property(getBitRate, doc="")
    

class RcvBaseModule(baseModule):
    """Interface for the MSDD RCV module"""
    MOD_NAME_MAPPING={1:"RCV",2:"RCV"}
    
    def __init__(self, connection, channel_number=0, mapping_version=2):
        baseModule.__init__(self, connection, channel_number, mapping_version)
        self._frequency_list=None
        self._attenuation_list=None
        self._gain_list=None
    
    def _setFrequency_Hz(self, freq_hz):
        # frequency command take mhz as units
        freqMHz=freq_hz/1e6
        self.send_set_command("FRQ ",str(freqMHz))

    def getFrequency_Hz(self):
        resp = self.send_query_command("FRQ")
        return float(self.parseResponse(resp, 1)[0])*1e6   

    def getFrequencyList(self):
        if self._frequency_list is None:
            resp = self.send_query_command("FRQL")
            self._frequency_list=self.parseResponse(resp, 3,':')
        return self._frequency_list

    def getFrequencyListStr(self):
        rl = self.getFrequencyList()
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
        return self.setter_with_validation(float(frequency_hz), self._setFrequency_Hz, self.getFrequency_Hz, 100)
    
    def _setAttenuation(self, attn):
        self.send_set_command("ATN",str(attn))
    def getAttenuation(self):
        resp = self.send_query_command("ATN")
        return float(self.parseResponse(resp, 1)[0])    
    def getAttenuationList(self):
        if self._attenuation_list is None:
            resp = self.send_query_command("ATNL")
            self._attenuation_list=self.parseResponse(resp, 3,':')
        return self._attenuation_list
    def getAttenuationListStr(self):
        rl = self.getAttenuationList()
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def getMinAttenuation(self):
        return float(self.getAttenuationList()[0])
    def getMaxAttenuation(self):
        return float(self.getAttenuationList()[1])
    def getStepAttenuation(self):
        return float(self.getAttenuationList()[2])
    def setAttenuation(self, attn):
        return self.setter_with_validation(float(attn), self._setAttenuation, self.getAttenuation)
    
    def _setGain(self, gain):
        self.send_set_command("GAI",str(gain))
    def getGain(self):
        resp = self.send_query_command("GAI")
        return float(self.parseResponse(resp, 1)[0])    
    def getGainList(self):
        if self._gain_list is None:
            resp = self.send_query_command("GAIL")
            self._gain_list= self.parseResponse(resp, 3,':')
        return self._gain_list
    def getGainListStr(self):
        rl = self.getGainList()
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def getMinGain(self):
        return float(self.getGainList()[0])
    def getMaxGain(self):
        return float(self.getGainList()[1])
    def getStepGain(self):
        return float(self.getGainList()[2])
    def setGain(self, gain):
        return self.setter_with_validation(float(gain), self._setGain, self.getGain)
    def get_valid_gain(self,gain):
        gl = self.getGainList()
        return self.get_value_valid(gain, 0, float(gl[0]), float(gl[1]), float(gl[2]),False)
    
    def getBIT(self):
        resp = self.send_query_command("BIT")
        return int(self.parseResponse(resp, 1)[0])        
    def getBITList(self):
        resp = self.send_query_command("BITL")
        return self.parseResponse(resp)
    def getBITStr(self,bit=None):
        if bit is None:
            bit=self.getBIT
        return self.process_bit_mask( bit, self.getBITList )
    def getADM(self):
        resp = self.send_query_command("ADM")
        return self.parseResponse(resp)        
    def getADMList(self):
        resp = self.send_query_command("ADML")
        return self.parseResponse(resp) 
    def getADCSampleCount(self):
        return float(self.getADM()[0])
    def getADCOverflowCount(self):
        return float(self.getADM()[1])
    def getADCAbsMax(self):
        return float(self.getADM()[2])
    def getADCListStr(self):
        return self.create_desc_val_csv(self.getADMList(),self.getADM())

    # LEAVING OUT CALIBRATION OVERRIDES FOR NOW!!    
    def getMTR(self):
        resp = self.send_query_command("MTR")
        return self.parseResponse(resp)
    def getMTRList(self):
        resp = self.send_query_command("MTRL")
        return self.parseResponse(resp)
    def getMTRStr(self):
        return self.process_bit_mask( self.getMTR, self.getMTRList )
    
    def _setBandwidth_Hz(self, bw):
        raise NotImplementedError(str(inspect.stack()[0][3]))
    def getBandwidth_Hz(self):
        raise NotImplementedError(str(inspect.stack()[0][3]))
    def getBandwidthList_Hz(self):
        raise NotImplementedError(str(inspect.stack()[0][3]))
    def getBandwidthListStr(self):
        res_list = self.getBandwidthList_Hz()
        return self.create_csv(res_list)
    def get_valid_bandwidth(self,bandwidth_hz=None,bandwidth_tolerance_perc=None):
        bw_list = self.getBandwidthList_Hz()
        return self.get_value_valid_list(bandwidth_hz,bandwidth_tolerance_perc, bw_list)
    def setBandwidth_Hz(self, bw):
        retVal =  self.setter_with_validation(float(bw), self._setBandwidth_Hz, self.getBandwidth_Hz)

    def _setExternalRef(self, ref):
        self.send_set_command("EXR ",str(ref))
    def getExternalRef(self):
        resp = self.send_query_command("EXR")
        return int(self.parseResponse(resp, 1)[0])
    def getExternalRefList(self):
        resp = self.send_query_command("EXRL")
        return self.parseResponse(resp, 3,':')
    def getMinExternalRef(self):
        return int(self.getExternalRefList()[0])
    def getMaxExternalRef(self):
        return int(self.getExternalRefList()[1])
    def getStepExternalRef(self):
        return int(self.getExternalRefList()[2])
    def setExternalRef(self, ref):
        return self.setter_with_validation(int(ref), self._setExternalRef, self.getExternalRef)
    
    """Accessable properties"""
    frequency_hz = property(getFrequency_Hz,setFrequency_Hz, doc="Center Frequency in Hz")
    min_frequency_hz = property(getMinFrequency_Hz, doc="Min Center Frequency in Hz")
    max_frequency_hz = property(getMaxFrequency_Hz, doc="Max Center Frequency in Hz")
    step_frequency_hz = property(getStepFrequency_Hz, doc="Step Center Frequency in Hz")
    available_frequency_hz =  property(getFrequencyListStr, doc="Available Frequency in min::step::max")
    frequency_hz_limits =  property(getFrequencyListStr, doc="Available Frequency in min::step::max")
    
    attenuation = property(getAttenuation,setAttenuation, doc="Attenuation in db")
    min_attenuation = property(getMinAttenuation, doc="Min Attenuation in db")
    max_attenuation = property(getMaxAttenuation, doc="Max Attenuation in db")
    step_attenuation = property(getStepAttenuation, doc="Step Attenuation in db")
    available_attenuation =  property(getAttenuationListStr, doc="Available Attenuation in min::step::max")
    
    bandwidth_hz = property(getBandwidth_Hz,setBandwidth_Hz , doc="Bandwidth in Hz")
    bandwidth_hz_limits = property(getBandwidthList_Hz)
    valid_bandwidth_list_hz = property(getBandwidthList_Hz)
    available_bandwidth_hz =  property(getBandwidthListStr, doc="Available Bandwidths")
    
    gain = property(getGain,setGain, doc="Gain in db")
    gain_limits = property(getGainList, doc="Gain limits in db")
    min_gain = property(getMinGain, doc="Min Gain in db")
    max_gain = property(getMaxGain, doc="Max Gain in db")
    step_gain = property(getStepGain, doc="Step Gain in db")
    available_gain =  property(getGainListStr, doc="Available Gain in min::step::max")
    
    bit = property(getBIT, doc="Bit state of the rx")
    bit_readable = property(getBITStr, doc="Bit state of the rx")
    bit_list = property(getBITList, doc="Bit state of the rx")
    adc_meter= property(getADM, doc="")
    adc_meter_list= property(getADMList, doc="")
    adc_meter_readable = property(getADCListStr, doc="")
    adc_sample_count = property(getADCSampleCount, doc="")
    adc_overflow_count = property(getADCOverflowCount, doc="")
    adc_abs_max = property(getADCAbsMax, doc="")

    external_ref = property(getExternalRef,setExternalRef, doc="")
    external_ref_limits = property(getExternalRefList, doc="")
    min_external_ref = property(getMinExternalRef, doc="")
    max_external_ref = property(getMaxExternalRef, doc="")
    step_external_ref = property(getStepExternalRef, doc="")

class RS422_RcvModule(RcvBaseModule):
    MOD_NAME_MAPPING={1:"RCV",2:"RCV"}
   
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
            if self._debug:
                traceback.print_exc()

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
    def get_valid_bandwidth(self,bandwidth_hz=None,bandwidth_tolerance_perc=None):
        bw_list = self.getBandwidthList_Hz()
        return self.get_value_valid_list(bandwidth_hz,bandwidth_tolerance_perc, bw_list)
    def setBandwidth_Hz(self, bw):
        return self.setter_with_validation(float(bw), self._setBandwidth_Hz, self.getBandwidth_Hz)
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
    bandwidth_hz_limits = property(getBandwidthList_Hz)
    valid_bandwidth_list_hz = property(getBandwidthList_Hz)
    available_bandwidth_hz =  property(getBandwidthListStr, doc="Available Bandwidths")

    lo_mode = property(getLoMode,setLoMode , doc="Lo Mode")
    lo_mode_limits = property(getLoModeList, doc="Lo Mode")
    

class MSDDX000_RcvModule(RcvBaseModule):
    MOD_NAME_MAPPING={1:"RCV",2:"RCV"}
    MSDD_X000_BANDWIDTH=20000000.0
    MSDD_X000EX_BANDWIDTH=40000000.0
    
    def validBandwidth(self,bandwidth_hz):
        bw_list=self.getBandwidthList_Hz()
        try:
            idx=bw_list.index(bandwidth_hz)
        except:
            raise InvalidValue("Invalid bandwidth value " + str(bandwidth_hz))
        return True
    def setBandwidth_Hz(self, bw_hz):
        return self.validBandwidth(bw_hz)
    def getBandwidth_Hz(self):
        return self.MSDD_X000EX_BANDWIDTH
    def getBandwidthList_Hz(self):
        return [self.MSDD_X000_BANDWIDTH,self.MSDD_X000EX_BANDWIDTH ]
    def get_valid_bandwidth(self,bandwidth_hz=None,bandwidth_tolerance_perc=None):
        bw_list = self.getBandwidthList_Hz()
        return self.get_value_valid_list(bandwidth_hz,bandwidth_tolerance_perc, bw_list)
    def getBandwidthListStr(self):
        res_list = self.getBandwidthList_Hz()
        return self.create_csv(res_list)
        
    bandwidth_hz = property(getBandwidth_Hz,setBandwidth_Hz , doc="Bandwidth in Hz")
    bandwidth_hz_limits = property(getBandwidthList_Hz)
    valid_bandwidth_list_hz = property(getBandwidthList_Hz)
    available_bandwidth_hz =  property(getBandwidthListStr, doc="Available Bandwidths")

#end class RFModule


class TODModule(baseModule):
    MOD_NAME_MAPPING={1:"TOD",2:"TOD"}
    #Timing Module commands                
    def getBIT(self):
        """Get Built In Test results    N.B. 0-4 are errors, all else are OK"""
        resp = self.send_query_command("BIT")
        # RESOLVE,
        #return int(self.parseResponse(resp,1)[0])
        return int(self.parseResponse(resp)[0])

    def getBITList(self):
        """Get Built In Test results    N.B. 0-4 are errors, all else are OK"""
        resp = self.send_query_command("BITL")
        return self.parseResponse(resp)

    def getBITStr(self,bit=None):
        if bit is None:
            bit=self.getBIT
        return self.process_bit_mask( bit, self.getBITList )
    
    def setTime(self, seconds):
        """Set Time of Day in seconds, latches on next PPS"""
        seconds=int(seconds)
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

    def getModeStr(self):
        return self.get_indexed_value(self.getMode, self.getModeList, rtype=str)

    def getModeList(self):
        resp = self.send_query_command("MODL")
        return self.parseResponse(resp)

    def getModeListStr(self):
        res_list = self.getModeList()
        return self.create_csv(res_list)

    def getModeIndex(self, mode):
        ret=0
        try:
            modeList = self.getModeList()
            if modeList:
                ret=modeList.index(mode)
        except:
            pass
        return ret

    def setMode(self, mode):
        if type(mode) == str:
            mode = self.getModeIndex(mode)
        return self.setter_with_validation(int(mode), self._setMode, self.getMode)    

    def getMeter(self):
        resp = self.send_query_command("MTR")
        return self.parseResponse(resp) 

    def getMeterList(self):
        resp = self.send_query_command("MTRL")
        return self.parseResponse(resp)   

    def getMeterListStr(self):
        return self.create_desc_val_csv(self.getMeterList(),self.getMeter())
    
    def getPPSVolt(self):
        resp = self.send_query_command("VOL")
        return float(self.parseResponse(resp)[0])

    def _setPPSVolt(self, voltage):
        self.send_set_command("VOL",str(voltage))

    def setPPSVolt(self, voltage):
        return self.setter_with_validation(float(voltage), self._setPPSVolt, self.getPPSVolt)    

    def getPPSVoltageLimits(self):
        resp = self.send_query_command("VOLL")
        return self.parseResponse(resp)

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

    def getToyList(self):
        resp = self.send_query_command("TOYL")
        return self.parseResponse(resp)

    """Accessable properties"""    
    bit = property(getBIT, doc="Bit error state of module")
    bit_readable = property(getBITStr, "Bit error state of module (readable)")
    bit_list = property(getBITList, "List of possible bit error states")
    time = property(getTime, setTime, doc="Time of day in secconds")
    mode = property(getMode, setMode, doc="Module Mode (0-sim,1-pps,2-irigb)")
    mode_readable = property(getModeStr, doc="Module Mode in string format")
    mode_list = property(getModeList)
    available_modes = property(getModeListStr)
    meter = property(getMeter)
    meter_list = property(getMeterList)
    meter_readable = property(getMeterListStr)
    pps_voltage = property(getPPSVolt, setPPSVolt, doc="")
    pps_voltage_limits = property(getPPSVoltageLimits,doc="")
    ref_adjust = property(getReferenceAdjust, setReferenceAdjust, doc="")
    ref_track = property(getReferenceTrack, setReferenceTrack, doc="")
    sub_sec_mismatch_fault = property(getSubSecMismatchFault, setSubSecMismatchFault, doc="")
    toy = property(getToy, setToy, doc="")
    toy_list = property(getToyList, doc="")
        
#end class TODModule



class DDCBaseModule(baseModule):
    MOD_NAME_MAPPING={1:"DDC",2:"DDC"}

    """Interface for the MSDD DDC module"""
    def __init__(self, connection, channel_number=0, mapping_version=2):
        baseModule.__init__(self, connection, channel_number, mapping_version)
        self.rf_offset_hz = 0
        self._input_sample_rate=None
        self._bw_hz_list=None
        self._decimation_by_position=None
        self._decimation_list=None
        self._frequency_list=None
        self._sample_rate_list=None
        self._gain_list=None
        self._attenuation_list=None

    def updateRFFrequencyOffset(self,rf_offset_hz):
        self.rf_offset_hz = float(rf_offset_hz)
    def _setFrequency_Hz(self, freq):
        freq_offset = freq - self.rf_offset_hz
        self.send_set_command("FRQ ",str(freq_offset))
    def getFrequency_Hz(self):
        resp = self.send_query_command("FRQ")
        return float(self.parseResponse(resp, 1)[0])+self.rf_offset_hz
    def getFrequencyList(self):
        if self._frequency_list is None:
            resp = self.send_query_command("FRQL")
            self._frequency_list=self.parseResponse(resp, 3,':')
        return [float(self._frequency_list[0])+self.rf_offset_hz,float(self._frequency_list[1])+self.rf_offset_hz,float(self._frequency_list[2])]
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
        return self.setter_with_validation(float(freq), self._setFrequency_Hz, self.getFrequency_Hz,1000)

    def _setBandwidth_Hz(self, bandwidth_hz):
        bw=bandwidth_hz
        self.validBandwidth(bw)
        bw_list = self.getBandwidthList_Hz()
        # find matching decimation for bandwidth setting
        idx=bw_list.index(bw)
        dlist=self.getDecimationList()
        if self._debug:
            print " DDCBase _setBandwidth ", bw, bw_list, " index ", idx, " decimation ", dlist[idx]
        self.setDecimation(dlist[idx])

    # seed bandwidth list using decimation values and then getting bandwidth from device
    def _getBandwidthList_Hz(self):            
        ret=[]
        isr=self.getInputSampleRate()
        decs=self.getDecimationList()
        for x in decs:
            try:
                ret.append( float(isr/x))
            except:
                pass
        if self._debug:
            print "DDCBase, _getBandwidthList_Hz ", self.channel_id(), " list ", ret
        return ret

    def getBandwidth_Hz(self):
        return self.getSampleRate()

    def getBandwidthList_Hz(self):
        if self._bw_hz_list is None:
            self._bw_hz_list=self._getBandwidthList_Hz()
        return self._bw_hz_list[:]

    def getBandwidthListStr(self):
        return self.create_csv(self.getBandwidthList_Hz())

    def get_valid_bandwidth(self,bandwidth_hz=None,bandwidth_tolerance_perc=None, ibw_override=None):
        bw_list=self.getBandwidthList_Hz()
        return self.get_value_valid_list(bandwidth_hz,bandwidth_tolerance_perc,bw_list)

    def validBandwidth(self,bandwidth_hz):
        bw_list=self.getBandwidthList_Hz()
        try:
            if self._debug:
                print "DDCBase, validBandwidth ", bandwidth_hz, " list ", bw_list
            idx=bw_list.index(bandwidth_hz)
        except:
            raise InvalidValue("Invalid bandwidth value " + str(bandwidth_hz))
        return True

    def get_sample_rate_for_bandwidth(self,bandwidth):
        sr_list =self.getSampleRateList()
        bw_list=self.getBandwidthList_Hz()
        srate=0.0
        try:
           bid=bw_list.index(bandwidth)
           return sr_list[bid]
        except:
            return self.get_valid_sample_rate(bandwidth, None, sr_list)

    def get_bandwidth_for_sample_rate(self,sample_rate):
        sr_list =self.getSampleRateList()
        bw_list=self.getBandwidthList_Hz()
        bw=0.0
        try:
           sid=sr_list.index(sample_rate)
           bw=bw_list[sid]
           return bw
        except:
            return self.get_valid_bandwidth(None, None, bw_list)

    def setBandwidth_Hz(self, bandwidth_hz):
        return self.setter_with_validation(float(bandwidth_hz), self._setBandwidth_Hz, self.getBandwidth_Hz)
    
    def _setSampleRate(self, sample_rate):
        self.validSampleRate(sample_rate)
        isr = self.getInputSampleRate()
        dec = 1
        if sample_rate >0:
            dec = isr/sample_rate
        self.setDecimation(dec)

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
        return sr_list

    def getSampleRateListStr(self):
        srl = self.getSampleRateList()
        return self.create_csv(srl)

    def validSampleRate(self,srate):
        sr_list=self.getSampleRateList()
        try:
            idx=sr_list.index(srate)
        except:
            raise InvalidValue("Invalid sample rate value " + str(srate))
        return True

    def get_valid_sample_rate(self,sample_rate,sample_rate_tolerance_perc, isr_override=None):
        sr_list = self.getSampleRateList(isr_override)
        return self.get_value_valid_list(sample_rate,sample_rate_tolerance_perc, sr_list)

    def setSampleRate(self, sample_rate):
        return self.setter_with_validation(float(sample_rate), self._setSampleRate, self.getSampleRate)

    def getInputSampleRate(self):
        resp = self.send_query_command("ISR")
        return float(self.parseResponse(resp, 1)[0])
    
    def _setDecimation(self, dec):
        _dec = dec
        bpos=False
        if self.isDecimationByPosition():
            bpos=True
            rl = self.getDecimationList()
            posMap = dict(zip(rl,range(len(rl))))
            decListSorted = sorted(rl)
            decPos = 0
            for supportedDec in decListSorted:
                if dec >= supportedDec:
                    decPos = posMap[supportedDec]
                else:
                    break
            _dec = decPos
        if self._debug:
            print "_setDecimation ", dec, " is by_position ", bpos
        self.send_set_command("DEC",str(_dec))


    def _getDecimation(self):
        """
        perform DEC command on against module
        """
        resp = self.send_query_command("DEC")
        return float(self.parseResponse(resp, 1)[0])
        
    def getDecimation(self):
        dec = self._getDecimation()
        if self.isDecimationByPosition():
            rl = self.getDecimationList()
            if self._debug:
                print "getDecimation list of dec ", rl
            return float(rl[int(dec)])
        return dec


    def _isDecimationByPosition(self, decl_resp):
        dlist=self.parseResponse(decl_resp,sep=':');
        if self._debug:
            print "_isDecimationByPosition ", len(dlist) != 3
        return len(dlist) != 3  # min:max:step
    
    def isDecimationByPosition(self):
        if self._decimation_by_position is None:
            decl_resp=self._getDecimationList()
            self._decimation_by_position = self._isDecimationByPosition(decl_resp)
        return self._decimation_by_position

    def _getDecimationList(self):
        return self.send_query_command("DECL")

    def _processDecimationList(self, resp):
        avail_dec = []
        rl = self.parseResponse(resp,sep=':');
        if len(rl) == 3:
            val = float(rl[0])
            step = float(rl[2])
            stop = float(rl[1]) + step
            if step == 0:
                avail_dec.append(val)
            else:
                while val < stop:
                    avail_dec.append(val)
                    val+=step
            return avail_dec
        else:
            rl = self.parseResponse(resp);
            for dec in rl:
                avail_dec.append(float(dec))
        return avail_dec

    def getDecimationList(self):
        if self._decimation_list is None:
            resp = self._getDecimationList()
            self._decimation_by_position = self._isDecimationByPosition(resp)
            self._decimation_list = self._processDecimationList(resp)
        return self._decimation_list

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
        if self._attenuation_list is None:
            resp = self.send_query_command("ATNL")
            self._attenuation_list = self.parseResponse(resp, 3,':')
        return self._attenuation_list
    def getAttenuationListStr(self):
        rl = self.getAttenuationList()
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def getMinAttenuation(self):
        return float(self.getAttenuationList()[0])
    def getMaxAttenuation(self):
        return float(self.getAttenuationList()[1])
    def getStepAttenuation(self):
        return float(self.getAttenuationList()[2])
    def setAttenuation(self, attn):
        return self.setter_with_validation(float(attn), self._setAttenuation, self.getAttenuation)
    
    
    def _setGain(self, gain):
        self.send_set_command("GAI",str(gain),check_for_output=True, expect_output=False)
    def getGain(self):
        resp = self.send_query_command("GAI")
        return float(self.parseResponse(resp, 1)[0])    
    def getGainList(self):
        if self._gain_list is None:
            resp = self.send_query_command("GAIL")
            self._gain_list= self.parseResponse(resp, 3,':')
        return self._gain_list
    def getGainListStr(self):
        rl = self.getGainList()
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def get_valid_gain(self,gain):
        gl = self.getGainList()
        return self.get_value_valid(gain, 0, float(gl[0]), float(gl[1]), float(gl[2]),True)
    def getMinGain(self):
        return float(self.getGainList()[0])
    def getMaxGain(self):
        return float(self.getGainList()[1])
    def getStepGain(self):
        return float(self.getGainList()[2])
    def setGain(self, gain):
        return self.setter_with_validation(float(gain), self._setGain, self.getGain)
    
    def getBIT(self):
        resp = self.send_query_command("BIT")
        return int(self.parseResponse(resp, 1)[0])        

    def getBITStr(self,bit=None):
        if bit is None:
            bit=self.getBIT
        return self.process_bit_mask( bit, self.getBITList )

    def getBITList(self):
        resp = self.send_query_command("BITL")
        return self.parseResponse(resp)

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
        return int(self.getEnableBlockLimits()[0])
    def getMaxEnableBlockSize(self):
        return int(self.getEnableBlockLimits()[1])
    def getIntEnableBlockSize(self):
        return int(self.getEnableBlockLimits()[2])
    def setEnable(self, enable):
        return self.setter_with_validation(enable, self._setEnable, self.getEnable)
    
    enable = property(getEnable,setEnable, doc="")
    min_enable_block_size = property(getMinEnableBlockSize,doc="")
    max_enable_block_size = property(getMaxEnableBlockSize, doc="")
    internal_enable_block_size = property(getIntEnableBlockSize, doc="")
    
    decimation = property(getDecimation,setDecimation, doc="decimation in db")
    decimation_limits = property(getDecimationList, doc="decimation limits in db")
    available_decimation =  property(getDecimationListStr, doc="Available Attenuation in min::step::max")
    
    attenuation = property(getAttenuation,setAttenuation, doc="Attenuation in db")
    attenuation_limits = property(getAttenuationList, doc="Attenuation limits db")
    min_attenuation = property(getMinAttenuation, doc="Min Attenuation in db")
    max_attenuation = property(getMaxAttenuation, doc="Max Attenuation in db")
    step_attenuation = property(getStepAttenuation, doc="Step Attenuation in db")
    available_attenuation =  property(getAttenuationListStr, doc="Available Attenuation in min::step::max")
    
    gain = property(getGain,setGain, doc="Gain in db")
    gain_limits = property(getGainList, doc="Gain limits db")
    min_gain = property(getMinGain, doc="Min Gain in db")
    max_gain = property(getMaxGain, doc="Max Gain in db")
    step_gain = property(getStepGain, doc="Step Gain in db")
    available_gain =  property(getGainListStr, doc="Available Gain in min::step::max")
    
    bit = property(getBIT, doc="Bit state of the rx")
    bit_readable = property(getBITStr, doc="Bit state of the rx (readable)")
    bit_list = property(getBITList, doc="List of bit error states")

    input_sample_rate= property(getInputSampleRate, doc="")
    
    frequency_hz = property(getFrequency_Hz,setFrequency_Hz, doc="Center Frequency in Hz")
    frequency_hz_limits = property(getFrequencyList, doc="Center Frequency limits in Hz")
    min_frequency_hz = property(getMinFrequency_Hz, doc="Min Center Frequency in Hz")
    max_frequency_hz = property(getMaxFrequency_Hz, doc="Max Center Frequency in Hz")
    step_frequency_hz = property(getStepFrequency_Hz, doc="Step Center Frequency in Hz")
    available_frequency_hz =  property(getFrequencyListStr, doc="Available Frequency in min::step::max")
    bandwidth_hz = property(getBandwidth_Hz,setBandwidth_Hz , doc="Bandwidth in Hz")
    bandwidth_hz_limits = property(getBandwidthList_Hz, doc="Bandwidth limits in Hz")
    valid_bandwidth_list_hz = property(getBandwidthList_Hz)
    available_bandwidth_hz =  property(getBandwidthListStr, doc="Available Bandwidths")
    
    sample_rate = property(getSampleRate,setSampleRate , doc="SampleRate in Sps")
    sample_rate_limits = property(getSampleRateList, doc="SampleRate limits in Sps")
    available_sample_rate =  property(getSampleRateListStr, doc="Available Sample Rate")
                
#end class DDCModule

class WBDDCModule(DDCBaseModule):
    MOD_NAME_MAPPING={1:"DDC",2:"WBDDC"}
    MSDD_X000_BANDWIDTH=20000000.0
    MSDD_X000EX_BANDWIDTH=40000000.0
    
    def __init__(self, connection, channel_number=0, mapping_version=2):
        super(WBDDCModule,self).__init__(connection, channel_number, mapping_version)

    def getInputSampleRate(self):
        if self._input_sample_rate is None:
            self._input_sample_rate=super(WBDDCModule,self).getInputSampleRate()
        return self._input_sample_rate

    def setSampleRate(self, sample_rate):
        if self._debug:
            print "WBDDC DDC setSampleRate ", sample_rate
        return self.setter_with_validation(float(sample_rate),
                                           self._setSampleRate,
                                           self.getSampleRate)

    def getSampleRate(self):
        return super(WBDDCModule,self).getSampleRate()

    def getSampleRateList(self, isr_override=None):
        if self._sample_rate_list is None:
            self._sample_rate_list=super(WBDDCModule,self).getSampleRateList(isr_override)
        return self._sample_rate_list

    def getSampleRateListStr(self):
        srl = self.getSampleRateList()
        return self.create_csv(srl)

    def setBandwidth_Hz(self, bandwidth_hz):
        ret=self.setter_with_validation(float(bandwidth_hz), self._setBandwidth_Hz, self.getBandwidth_Hz)
        if self._debug:
            print "WBDDC setBandwidth_hz ", bandwidth_hz, " ret ", ret
        return ret

    def getBandwidth_Hz(self):
        dec=self.getDecimation()
        dec_list=self.getDecimationList()
        idx=dec_list.index(dec)
        bw_list=self.getBandwidthList_Hz()
        if idx < len(bw_list):
            return bw_list[idx]
        else:
            # prior method
            if (self.sample_rate <= 25000000):
                return self.MSDD_X000_BANDWIDTH
            elif self.sample_rate == 50000000:
                return self.MSDD_X000EX_BANDWIDTH

    def _get_ratio(self):
        min_sr=min(self.getSampleRateList())
        min_bw=self.MSDD_X000_BANDWIDTH
        if min_sr > self.MSDD_X000EX_BANDWIDTH : min_bw=self.MSDD_X000EX_BANDWIDTH
        return min_bw/min_sr

    def getBandwidthList_Hz(self):
        rv = []
        bw_ratio=self._get_ratio()
        for srate in self.getSampleRateList():
            bw = srate*bw_ratio
            if self._debug:
                   print "wbddc, bw:", bw, " srate", srate, " ratio ", bw_ratio
            rv.append(bw)
        return rv

    def getBandwidthListStr(self):
        return self.create_csv(self.getBandwidthList_Hz())

    input_sample_rate= property(getInputSampleRate, doc="")

    sample_rate = property(getSampleRate,setSampleRate , doc="SampleRate in Sps")
    sample_rate_limits = property(getSampleRateList, doc="SampleRate limits in Sps")
    available_sample_rate =  property(getSampleRateListStr, doc="Available Sample Rate")

    bandwidth_hz = property(getBandwidth_Hz,setBandwidth_Hz , doc="Bandwidth in Hz")
    bandwidth_hz_limits = property(getBandwidthList_Hz, doc="Bandwidth limits in Hz")
    valid_bandwidth_list_hz = property(getBandwidthList_Hz)
    available_bandwidth_hz =  property(getBandwidthListStr, doc="Available Bandwidths")

class NBDDCModule(DDCBaseModule):
    MOD_NAME_MAPPING={1:"DDC",2:"NBDDC"}

    def __init__(self, connection, channel_number=0, mapping_version=2):
        super(NBDDCModule,self).__init__(connection, channel_number, mapping_version)

    def _getBandwidthList_Hz(self):
        """
        Get the list of valid bandwidths for the channel. This requires actually setting
        the decimation rate to let the radio update its bandwidth setting.
        """
        ret=[]
        for x in self.getDecimationList():
            try:
                # nbddc have not query on bandwidth so we need to set the decimation and get
                # the corresponding value back
                self.setDecimation(x)
                ret.append( self.getBandwidth_Hz() )
            except:
                pass
        if self._debug:
            print " nbddc _getBandwidthList ", self.channel_id(), " list " ,ret
        return ret
   
    def setBandwidth_Hz(self, bandwidth_hz):
        return self.setter_with_validation(float(bandwidth_hz), self._setBandwidth_Hz, self.getBandwidth_Hz)

    def getBandwidth_Hz(self):
        resp = self.send_query_command("BWT")
        return float(self.parseResponse(resp, 1)[0])*1e3    

    def getBandwidthList_Hz(self):
        if self._bw_hz_list is None:
            self._bw_hz_list=self._getBandwidthList_Hz()
        return self._bw_hz_list[:]

    def getBandwidthListStr(self):
        return self.create_csv(self.getBandwidthList_Hz())

    bandwidth_hz = property(getBandwidth_Hz,setBandwidth_Hz , doc="Bandwidth in Hz")
    bandwidth_hz_limits = property(getBandwidthList_Hz, doc="Bandwidth limits in Hz")
    valid_bandwidth_list_hz = property(getBandwidthList_Hz)
    available_bandwidth_hz =  property(getBandwidthListStr, doc="Available Bandwidths")

    
class SWDDCModule(DDCBaseModule):
    MOD_NAME_MAPPING={1:"SDC",2:"SWDDCDEC2"}
    
    """Interface for the MSDD Software DDC module"""
    def __init__(self, connection, channel_number=0, mapping_version=2):
        super(SWDDCModule,self).__init__(connection, channel_number, mapping_version)
        self._parent=None

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
        return ""
    def get_valid_gain(self,gain):
        gl = self.getGainList()
        return self.get_value_valid(gain, 0, float(gl[0]), float(gl[1]), float(gl[2]),True)
    def getMinGain(self):
        return 0
    def getMaxGain(self):
        return 0
    def getStepGain(self):
        return 0
    
    def getBIT(self,bit=None):
        return 0     
    def getBITStr(self):
        return ""
    def getBITList(self):
        return []

    def setParent(self, parent):
        self._parent=parent

    # if parent is known then we are basing the bandwidth list on the parent's current bandwidth
    def _getBandwidthList_Hz(self):
        ret=[]
        parent_bw=None
        if self._parent:
            parent_bw=self._parent.getBandwidth_Hz()

        sr=self.getInputSampleRate()
        decs=self.getDecimationList()

        for x in decs:
            try:
                bw=float(sr/x)
                if parent_bw:
                    bw=min(bw,parent_bw)
                ret.append(bw)
            except:
                pass
        return ret

    def getBandwidth_Hz(self):
        if self._parent:
            # take the smaller of parent's current bw and our sample rate
            bw=self._parent.getBandwidth_Hz()
            sr=self.getSampleRate()
            return min(bw,sr)
        else:
            return self.getSampleRate()

    def getBandwidthList_Hz(self):
        # always rebuild list since upstream could reset sample rate on us
        self._bw_hz_list=self._getBandwidthList_Hz()
        return self._bw_hz_list[:]

    def getBandwidthListStr(self):
        return self.create_csv(self.getBandwidthList_Hz())

    def setBandwidth_Hz(self, bandwidth_hz):
        if self._debug:
            print "SWDDC setBandwidth_Hz "
        return self.setter_with_validation(float(bandwidth_hz), self._setBandwidth_Hz, self.getBandwidth_Hz)

    # reassign for specialized behavior
    bandwidth_hz = property(getBandwidth_Hz,setBandwidth_Hz , doc="Bandwidth in Hz")
    bandwidth_hz_limits = property(getBandwidthList_Hz, doc="Bandwidth limits in Hz")
    valid_bandwidth_list_hz = property(getBandwidthList_Hz)
    available_bandwidth_hz =  property(getBandwidthListStr, doc="Available Bandwidths")

    #
    # reassign properties for unsupported behavior
    #
    bit = property(getBIT, doc="Bit state of the rx")
    bit_readable = property(getBITStr, doc="Bit state of the rx (readable)")
    bit_list = property(getBITList, doc="List of bit error states")

    gain = property(getGain,setGain, doc="Gain in db")
    min_gain = property(getMinGain, doc="Min Gain in db")
    max_gain = property(getMaxGain, doc="Max Gain in db")
    step_gain = property(getStepGain, doc="Step Gain in db")
    available_gain =  property(getGainListStr, doc="Available Gain in min::step::max")

    attenuation = property(getAttenuation,setAttenuation, doc="Attenuation in db")
    attenuation_limits = property(getAttenuationList, doc="Attenuation limits db")
    min_attenuation = property(getMinAttenuation, doc="Min Attenuation in db")
    max_attenuation = property(getMaxAttenuation, doc="Max Attenuation in db")
    step_attenuation = property(getStepAttenuation, doc="Step Attenuation in db")
    available_attenuation =  property(getAttenuationListStr, doc="Available Attenuation in min::step::max")

    
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
        if self._debug:
            print "getModulesFlowListBySource ", full_reg_name, " result: ", resp
        return self.parseResponse(resp)    
    
    def linkModules(self,source_reg_name,destination_reg_name):
        link_str = str(source_reg_name) + " " + str(destination_reg_name)
        if self._debug:
            print "Link Modules ", link_str
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

    def getChannelLimits(self,module_name):
        ret_val = []
        try:
            min_max_lst = []
            resp = self.send_query_command("RCL",str(module_name))
            channel_limits = self.parseResponse(resp)
            for limit_id in channel_limits:
                channel_numnber=0
                try:
                    channel_number = str(limit_id).rsplit(':')
                    min_max_lst.append(int(channel_number[-1]))
                except:
                    min_max_lst.append(channel_number)
            for i in range(min_max_lst[0],min_max_lst[1]+1):
                ret_val.append(i)
        except:
           ret_val=[]
        return ret_val

    def getInstallName(self,reg_module):
        resp = self.send_query_command("INA",str(reg_module))
        return str(self.parseResponse(resp, sep=':')[0])
    
    def getRegistrationName(self,installed_module):
        resp = self.send_query_command("RNA",str(installed_module))
        return str(self.parseResponse(resp, 1)[0])
    
    def registerModule(self,reg_name, install_name, starting_channel, num_channels):
        reg_str = str(reg_name) + " " + str(install_name) + " " + str(starting_channel) + " " + str(num_channels)
        self.send_set_command("REG",reg_str)
            
    modules = property(getModuleList,doc="")
    modules_readable = property(getModuleListStr,doc="")
    
    

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
        return int(self.getEnableSetsLimits()[0])
    def getMaxEnableSetsSize(self):
        return int(self.getEnableSetsLimits()[1])
    def getIntEnableSetsSize(self):
        return int(self.getEnableSetsLimits()[2])
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
        return int(self.getEnableSetsLimits()[0])
    def getMaxEnableSetsSize(self):
        return int(self.getEnableSetsLimits()[1])
    def getIntEnableSetsSize(self):
        return int(self.getEnableSetsLimits()[2])
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

        self.setTippingModeNum(mode, loss)
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
    
                
class OUTModule(baseIPPModule):
    class Info:
        def __init__(self):
            self.ip_address=""
            self.port=0
            self.vlan_enabled=False
            self.vlan =0
            self.flow=""
            self.timestamp_offset=0
            self.endianess=0
            self.mpf_flush=0
            self.samples_per_frame=512
            self.packet_data_len=1024
            self.data_width=16
            self.protocol=""
            self.channel_number=None
            self.input_sample_rate=0
            self.interface=0

    MOD_NAME_MAPPING={1:"OUT",2:"OUT"}
    
    PROTOCOL_UDP_SDDS=0
    PROTOCOL_UDP_SDDSX=1
    PROTOCOL_UDP_VITA49=2
    PROTOCOL_UDP_RAW=3
    PROTOCOL_UDP_SDDSA=4

    def __init__(self, connection, channel_number=0, mapping_version=2, multi_interface=False):
        super(OUTModule,self).__init__(connection, channel_number, mapping_version, multi_interface)
        self._enable_block_limits=None
        self._packet_rate_limits=None
        self._packet_length_limits=None
        self._protocol_list=None
        self._endianess_list=None
        self._gain_limits=None
        self._protocol_name=''
        self._info=OUTModule.Info()


    def _configureModule(self, enable, ipp, pol,
                        vlan, vlan_enable,
                        endianess, time_offset, flush_opt,
                        proto_opts="" ):

        # concatenate commands for quicker processing
        _enable_arg="0"
        if enable: _enable_arg="1"
        _ipp="IPP {};".format(ipp)
        _pol="POL {};".format(pol)
        _vlan_enable=0
        if vlan_enable: _vlan_enable=1
        _vlan="VLANEN {}; VLANTCI {};".format(_vlan_enable,vlan)

        if type(endianess) == str:
            endianess=self.getOutputEndianessIndex(endianess)
            
        cmd="ENB {}; {} {} {} {} END {}; TSOFS {}; MFP {};".format(
            _enable_arg, _ipp, _pol, _vlan, proto_opts, endianess, time_offset, flush_opt)
        self.send_set_command(cmd)

        
        
    def configureVita49(self, enable, interface, ip_address, port,
                         vlan, vlan_enable,
                         endianess, time_offset, flush_opt, context_interval):
        ipp=self._formatIPPargs(ip_address,port,interface)
        proto_opts="CDR {}; CCR 1;".format(context_interval)
        self._protocol_name='UDP_VITA49'
        pol=self.getOutputProtocolNumberFromString("UDP_VITA49")        
        self._configureModule(enable, ipp, pol,
                            vlan, vlan_enable,
                            endianess, time_offset, flush_opt,
                            proto_opts)
        self.validateIPP(ipp)
        
        self._info.ip_address=ip_address
        self._info.port=port
        self._info.protocol=self._protocol_name
        self._info.vlan=vlan
        self._info.vlan_enabled=vlan_enable
        self._info.endianess=endianess
        self._info.channel_number = self.channel_id()
        self._info.timestamp_offsest=time_offset
        self._info.samples_per_frame=self.samples_per_frame
        self._info.data_width=self.data_width
        self._info.input_sample_rate=self.input_sample_rate
        self._info.interface=self.getIPP_Interface()

    def configureSDDS(self, enable, interface, ip_address, port, 
                       protocol, vlan, vlan_enable,
                       endianess, time_offset, flush_opt):

        ipp=self._formatIPPargs(ip_address,port,interface)
        if type(protocol)==str:
            self._protocol_name=protocol
            protocol=self.getOutputProtocolNumberFromString(protocol)
        self._configureModule(enable, ipp, protocol,
                            vlan, vlan_enable,
                            endianess, time_offset, flush_opt)
        self.validateIPP(ipp)

        self._info.ip_address=ip_address
        self._info.port=port
        self._info.protocol=self._protocol_name
        self._info.vlan=vlan
        self._info.vlan_enabled=vlan_enable
        self._info.endianess=endianess
        self._info.channel_number = self.channel_id()
        self._info.timestamp_offsest=time_offset
        self._info.samples_per_frame=self.samples_per_frame
        self._info.data_width=self.data_width
        self._info.input_sample_rate=self.input_sample_rate
        self._info.interface=self.getIPP_Interface()

    def getInfo(self):
        return self._info

    def _setEnable(self, enable, block_xfer_size=None, sync_channel=None):
        enable_arg="0"
        if enable: enable_arg="1"
        if block_xfer_size is None or block_xfer_size < 0:
            block_xfer_size=0
        enable_arg+=":" + str(block_xfer_size)
        if sync_channel is not None and sync_channel >= 0:
            enable_arg+=":" + str(sync_channel)
        if self._debug:
            print " OUTModule, setEnable ENB :", str(enable_arg)
        self.send_set_command("ENB",str(enable_arg))

    def getEnable(self):
	try:
            resp = self.send_query_command("ENB")
            if self._debug:
                print "OUTModule,  getEnable resp ", resp
            return (int(self.parseResponse(resp, -1,':')[0]) == 1)
        except Exception, e:
            print e
            raise e

    def setEnable(self, enable):
        return self.setter_with_validation(enable, self._setEnable, self.getEnable)

    def getEnableBlockLimits(self):
        if self._enable_block_limits is None:
            resp = self.send_query_command("ENBL")
            self._enable_block_limits=self.parseResponse(resp, 3,':')
        return self._enable_block_limits

    def getMinEnableBlockSize(self):
        return int(self.getEnableBlockLimits()[0])

    def getMaxEnableBlockSize(self):
        return int(self.getEnableBlockLimits()[1])

    def getStepEnableBlockSize(self):
        return int(self.getEnableBlockLimits()[2])
    
    def getBIT(self):
        resp = self.send_query_command("BIT")
        return int(self.parseResponse(resp, 1)[0])

    def getBITStr(self,bit=None):
        if bit is None:
            return self.process_bit_mask( self.getBIT, self.getBITList )
        else:
            return self.process_bit_mask( bit, self.getBITList )

    def getBITList(self):
        resp = self.send_query_command("BITL")
        return self.parseResponse(resp)
    
    def _setUdpPacketRate(self, rat):
        self.send_set_command("RAT",str(rat))
    def getUdpPacketRate(self):
        resp = self.send_query_command("RAT")
        return float(self.parseResponse(resp, 1)[0])
    def getUdpPacketRateLimits(self):
        if self._packet_rate_limits is None:
            resp = self.send_query_command("RATL")
            self._packet_rate_limits=self.parseResponse(resp, 3,':')
        return self._packet_rate_limits
    def getMinUdpPacketRate(self):
        return float(self.getUdpPacketRateLimits()[0])
    def getMaxUdpPacketRate(self):
        return float(self.getUdpPacketRateLimits()[1])
    def getStepUdpPacketRate(self):
        return float(self.getUdpPacketRateLimits()[2])
    def setUdpPacketRate(self, rat):
        return self.setter_with_validation(float(rat), self._setUdpPacketRate, self.getUdpPacketRate)
    
    def _setOutputDataWidth(self, dwt):
        self.send_set_command("DWT",str(dwt))
    def _getOutputDataWidth(self):
        resp = self.send_query_command("DWT")
        return int(self.parseResponse(resp, 1)[0])    
    def getOutputDataWidth(self):
        return self.get_indexed_value(self._getOutputDataWidth, self.getOutputDataWidthList, rtype=int)

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
    def getOutputSamplesPerFrameLimits(self):
        if self._packet_length_limits is None:
            resp = self.send_query_command("LENL")
            self._packet_length_limits=self.parseResponse(resp, 3,':')
        return self._packet_length_limits
    def getMinOutputSamplesPerFrame(self):
        return int(self.getOutputSamplesPerFrameLimits()[0])
    def getMaxOutputSamplesPerFrame(self):
        return int(self.getOutputSamplesPerFrameLimits()[1])
    def getStepOutputSamplesPerFrame(self):
        return int(self.getOutputSamplesPerFrameLimits()[2])
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

    def getOutputProtocolStr(self):
        return self.get_indexed_value( self.getOutputProtocol, self.getOutputProtocolList, rtype=str)
    
    def getOutputProtocolList(self):
        if self._protocol_list is None:
            resp = self.send_query_command("POLL")
            self._protocol_list=self.parseResponse(resp)
        return self._protocol_list
    
    def getOutputProtocolNumberFromString(self,protocol):
        try:
            proto_list = self.getOutputProtocolList()
            return proto_list.index(protocol)
        except:
            raise InvalidValue("Invalid output protocol specified <" + str(protocol) + ">")
    
    def setOutputProtocol(self, proto):
        if type(proto)==str:
            proto=self.getOutputProtocolNumberFromString(proto)
        return self.setter_with_validation(int(proto), self._setOutputProtocol, self.getOutputProtocol)
    def _setOutputEndianess(self, end):
        self.send_set_command("END",str(end))

    def getOutputEndianess(self):
        resp = self.send_query_command("END")
        return int(self.parseResponse(resp, 1)[0])    

    def getOutputEndianessList(self):
        if self._endianess_list is None:
            resp = self.send_query_command("ENDL")
            self._endianess_list=self.parseResponse(resp)
        return self._endianess_list
    def getOutputEndianessStr(self):
        return self.get_indexed_value( self.getOutputEndianess, self.getOutputEndianessList, rtype=str)

    def getOutputEndianessIndex(self,endian):
        try:
            vlist = self.getOutputEndianessList()
            return vlist.index(endian)
        except:
            raise InvalidValue("Invalid endianess specified <" + str(endian) + ">")

    def setOutputEndianess(self, end):
        if type(end) == str:
            end=self.getOutputEndianessIndex(end)
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
        self._setAdditionalPktAlloc(int(add))
    
    def getInputSampleRate(self):
        resp = self.send_query_command("ISR")
        return float(self.parseResponse(resp, 1)[0]) 
    
    def _setGain(self, gain):
        self.send_set_command("GAI",str(gain))
    def getGain(self):
        resp = self.send_query_command("GAI")
        return float(self.parseResponse(resp, 1)[0])    
    def getGainLimits(self):
        if self._gain_limits is None:
            resp = self.send_query_command("GAIL")
            self._gain_limits=self.parseResponse(resp, 3,':')
        return self._gain_limits
    def getGainLimitsStr(self):
        rl = self.getGainLimits()
        return self.create_range_string(float(rl[0]), float(rl[1]), float(rl[2]))
    def getMinGain(self):
        return float(self.getGainLimits()[0])
    def getMaxGain(self):
        return float(self.getGainLimits()[1])
    def getStepGain(self):
        return float(self.getGainLimits()[2])
    def setGain(self, attn):
        return self.setter_with_validation(float(attn), self._setGain, self.getGain)
    
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
    
    
    def _setMFP(self, mfp):
        self.send_set_command("MFP",str(mfp))
    def getMFP(self):
        resp = self.send_query_command("MFP")
        return int(self.parseResponse(resp, 1)[0])
    def getMFPLimits(self):
        resp = self.send_query_command("MFPL")
        return self.parseResponse(resp)
    def setMFP(self, mfp):
        self.in_range( int(mfp), self.getMFPLimits()[0])
        return self.setter_with_validation(int(mfp), self._setMFP, self.getMFP)   
    
    def getCDR(self):
        resp = self.send_query_command("CDR")
        return float(self.parseResponse(resp, 1)[0])
    def getCDRLimits(self):
        resp = self.send_query_command("CDRL")
        return self.parseResponse(resp)
    def _setCDR(self,cdr):
        self.send_set_command("CDR",str(cdr))
    def setCDR(self,cdr):
        return self.setter_with_validation(float(cdr), self._setCDR, self.getCDR)        

    def getCCR(self):
        resp = self.send_query_command("CCR")
        return int(self.parseResponseSpaceOnly(resp, 4)[2]) 
    def getCCRLimits(self):
        resp = self.send_query_command("CCRL")
        return self.parseResponse(resp)
    def _setCCR(self,ccr):
        self.send_set_command("CCR",str(ccr))
    def setCCR(self,ccr):
        self.in_range( int(ccr), self.getCCRLimits()[0])
        return self.setter_with_validation(int(ccr), self._setCCR, self.getCCR) 
    
    enable = property(getEnable,setEnable, doc="")
    enable_block_limits= property(getEnableBlockLimits, doc="")
    bit = property(getBIT)
    bit_readable = property(getBITStr)
    bit_list = property(getBITList)
    packet_rate = property(getUdpPacketRate,setUdpPacketRate, doc="")
    packet_rate_limits = property(getUdpPacketRateLimits, doc="")
    data_width = property(getOutputDataWidth,setOutputDataWidth, doc="")
    samples_per_frame = property(getOutputSamplesPerFrame,setOutputSamplesPerFrame, doc="")
    samples_per_frame_limits = property(getOutputSamplesPerFrameLimits, doc="")
    stream_id = property(getOutputStreamID,setOutputStreamID, doc="")
    protocol = property(getOutputProtocol,setOutputProtocol, doc="")
    protocol_readable = property(getOutputProtocolStr,doc="")
    protocol_list = property(getOutputProtocolList,"")
    endianess = property(getOutputEndianess,setOutputEndianess, doc="")
    endianess_readable = property(getOutputEndianessStr, doc="")
    endianess_list = property(getOutputEndianessList, doc="")
    add_pkt_allocations = property(getAdditionalPktAlloc,setAdditionalPktAlloc, doc="")
    input_sample_rate = property(getInputSampleRate, doc="")
    gain_when_reducing_bits = property(getGain,setGain, doc="")
    vlan_tagging_enable = property(getEnableVlanTagging,setEnableVlanTagging, doc="")
    vlan_tci = property(getVlanTci,setVlanTci, doc="")
    vlan_timestamp_ref = property(getTimestampRef,setTimestampRef, doc="")
    timestamp_offset = property(getTimestampOff,setTimestampOff, doc="")
    mfp = property(getMFP,setMFP, doc="")
    mfp_limits = property(getMFPLimits,doc="")
    cdr = property(getCDR,setCDR, doc="")
    cdr_limits = property(getCDRLimits, doc="")
    ccr = property(getCCR,setCCR, doc="")
    ccr_limits = property(getCCRLimits, doc="")
    gain = property(getGain,setGain, doc="Gain in db")
    gain_limits = property(getGainLimits, doc="Gain limits db")
    min_gain = property(getMinGain, doc="Min Gain in db")
    max_gain = property(getMaxGain, doc="Max Gain in db")
    step_gain = property(getStepGain, doc="Step Gain in db")
    available_gain =  property(getGainLimitsStr, doc="Available Gain in min::step::max")

    
#end class OUTModule


class object_container(object):
    def __init__(self):
        self.used_objects = []
        self.free_objects = []
    def put_object(self, obj):
        if self.used_objects.count(obj) > 0:
            self.used_objects.remove(obj)
        self.free_objects.append(obj)

    def is_used(self, obj):
        if obj == None:
            return False
        try:
            idx=self.used_objects.index(obj)
            return True
        except:
            return False
        
    def mark_as_used(self, obj):
        if obj == None:
            return
        if self.free_objects.count(obj) <= 0:
            return 
        self.free_objects.remove(obj)
        self.used_objects.append(obj)

    def get_used_objects(self):
        return self.used_objects[:]
          
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
    MSDDRXTYPE_SPC="SPC"                #SPC

    class registered_module(object):
        """
        Registered module name for each msdd channel module (commands RNA(register modules), RCL(registered channel limits)
        """
        def __init__(self, registration_name, installation_name, channel_number):
            self.channel_number = channel_number
            self.installation_name = installation_name
            self.registration_name = registration_name

        def channel_id(self):
            return self.registration_name + ':' + str(self.channel_number)

    class object_module(object):
        """
        Defines the relationship of actual xxxModule class and the registered name
        """
        def __init__(self, registered, object_module = None):
            self.channel_number = registered.channel_number
            self.installation_name = registered.installation_name
            self.registration_name = registered.registration_name
            self.object = object_module

        def channel_id(self):
            return self.registration_name + ':' + str(self.channel_number)

    class rx_channel(object):
        """
        Exposed receiver channels that are created from the following receiver paths:
         rcv -> wbddc -> out
         rcv -> wbddc -> nbddc -> out
         rcv -> wbddc -> nbddc -> swddc (inline) -> out  (finer bw tuning)
        """
        def __init__(self, msdd_radio, reg_mod, rx_channel_num,coherent=False, msdd_rx_type="UNKNOWN"):
            self._msdd = msdd_radio
            self.channel_number = reg_mod.channel_number
            self.installation_name = reg_mod.installation_name
            self.registration_name = reg_mod.registration_name
            self.rx_channel_num = rx_channel_num
            self.coherent = coherent
            self.msdd_rx_type = msdd_rx_type
            
            self.analog_rx_object = None
            self.digital_rx_object = None
            self.output_object = None
            self.swddc_object=None
            self.spectral_scan_object = None
            self.spectral_scan_wbddc_object = None
            
            self.rx_parent_object = None
            self.rx_child_objects=[]
            self._debug=False
            self._srates={}
            self._bw_rates={}
            self.fft_channel=None
            
            # cache for status info for quicker response and less radio IO
            self._analog_status=None
            self._analog_digital_status=None
            self._digital_status=None
            self._output_status=None
            self._fft_status=None

        def msdd_channel_id(self):
            return self.registration_name + ":" + str(self.channel_number)
        def get_msdd_type_string(self):
            return self.msdd_rx_type
        def update_output_object(self, output_object):
            self.output_object = output_object
        def has_streaming_output(self):
            return self.output_object != None
        def has_fft_object(self):
            return self.fft_channel != None
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
            if self.fft_channel != None:
                reg_name_list.append(self.fft_channel.object.get_full_reg_name())
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
            if self.fft_channel != None:
                install_name_list.append(self.fft_channel.installation_name)
            if self.spectral_scan_object != None:
                install_name_list.append(self.spectral_scan_object.installation_name)
            if self.output_object != None:
                install_name_list.append(self.output_object.installation_name)
            return create_csv(install_name_list)

        def addFFTChannel(self, fft ):
            self.fft_channel=fft
            self.fft_channel=fft.fft

        def removeFFTChannel(self):
            fft=self.fft_channel
            self.fft_channel=None
            return fft

        def hasFFTChannel(self):
            return self.fft_channel != None

        # status return object
        class _status(object):
            pass

        def _get_analog_status(self):
            if not self._analog_status:
                status=status=self._status()
                status.center_frequency = self.getFrequeny_Hz()
                status.available_frequency = self.analog_rx_object.object.available_frequency_hz
                status.gain = self.analog_rx_object.object.gain
                status.available_gain = self.analog_rx_object.object.available_gain
                status.attenuation = self.analog_rx_object.object.attenuation
                status.available_attenuation = self.analog_rx_object.object.available_attenuation
                status.adc_meter_values = self.analog_rx_object.object.adc_meter_readable
                status.bandwidth = self.getBandwidth_Hz()
                status.available_bandwidth = self.analog_rx_object.object.available_bandwidth_hz
                status.rcvr_gain = self.digital_rx_object.object.gain
                self._analog_status=copy.copy(status)
            else:
                self._analog_status=self.getFrequeny_Hz()

            return self._analog_status

        def _get_analog_digital_status(self):
            if not self._analog_digital_status:
                status=self._status()
                status.enabled = self.getEnable()
                status.decimation = self.digital_rx_object.object.decimation
                status.available_decimation = self.digital_rx_object.object.available_decimation
                status.attenuation = self.analog_rx_object.object.attenuation
                status.available_attenuation = self.analog_rx_object.object.available_attenuation
                status.gain = self.analog_rx_object.object.gain
                status.available_gain = self.analog_rx_object.object.available_gain
                status.input_sample_rate = self.digital_rx_object.object.input_sample_rate
                status.sample_rate = self.getSampleRate()
                status.available_sample_rate = create_csv(self.getSampleRateLimits())
                status.bandwidth = self.getBandwidth_Hz()
                status.available_bandwidth = create_csv(self.getBandwidthLimits())
                status.center_frequency = self.getFrequency_Hz()
                status.available_frequency = self.analog_rx_object.object.available_frequency_hz
                status.ddc_gain = self.digital_rx_object.object.gain
                self._analog_digital_status=copy.copy(status)
            else:
                self._analog_digital_status.enabled=self.getEnable()
                self._analog_digital_status.center_frequency = self.getFrequency_Hz()
                self._analog_digital_status.sample_rate = self.getSampleRate()
                self._analog_digital_status.bandwidth = self.getBandwidth_Hz()

            return self._analog_digital_status

        def _get_digital_status(self):
            if not self._digital_status:
                status=self._status()
                status.enabled = self.getEnable()
                status.decimation = self.digital_rx_object.object.decimation
                status.available_decimation = self.digital_rx_object.object.available_decimation
                status.attenuation = self.digital_rx_object.object.attenuation
                status.available_attenuation = self.digital_rx_object.object.available_attenuation
                status.gain = self.digital_rx_object.object.gain
                status.available_gain = self.digital_rx_object.object.available_gain
                status.input_sample_rate = self.digital_rx_object.object.input_sample_rate
                status.sample_rate = self.getSampleRate()
                status.available_sample_rate = create_csv(self.getSampleRateLimits())
                status.bandwidth = self.getBandwidth_Hz()
                status.available_bandwidth = create_csv(self.getBandwidthLimits())
                status.center_frequency = self.getFrequency_Hz()
                status.available_frequency = self.digital_rx_object.object.available_frequency_hz
                status.ddc_gain = self.digital_rx_object.object.gain
                self._digital_status=copy.copy(status)
            else:
                self._digital_status.enabled=self.getEnable()
                self._digital_status.center_frequency = self.getFrequency_Hz()
                self._digital_status.sample_rate = self.getSampleRate()
                self._digital_status.bandwidth = self.getBandwidth_Hz()

            return self._digital_status

            return status

        def getStatus(self):
            if self.is_analog() and not self.is_digital():
                return self._get_analog_status()
            if self.is_analog() and self.is_digital():
                return self._get_analog_digital_status()
            if not self.is_analog() and self.is_digital():
                return self._get_digital_status()
            return None

        def getOutputStatus(self):
            if self.output_object:
                try:
                    # get caches settings from output port since these change
                    # during startup.
                    info=self.output_object.object.getInfo()
                    if not self._output_status:
                        status=self._status()
                        self._output_status=copy.copy(status)
                        self._output_status.output_flow = self._msdd.get_stream_flow(self.output_object)


                    self._output_status.output_multicast = info.ip_address
                    self._output_status.output_port = int(info.port)
                    self._output_status.output_enabled = self.output_object.object.enable
                    self._output_status.output_protocol = info.protocol
                    self._output_status.output_vlan_enabled = info.vlan_enabled
                    self._output_status.output_vlan = info.vlan
                    self._output_status.output_timestamp_offset = info.timestamp_offset
                    self._output_status.output_channel = str(info.channel_number)
                    self._output_status.output_endianess = info.endianess
                    self._output_status.output_mfp_flush = str(self.output_object.object.mfp)
                except:
                    traceback.print_exc()
                    
            return self._output_status

        def getFFTStatus(self):
            status=None
            if self.output_object:
                if not self._fft_status:
                    status=self._status()
                    status.psd_fft_size = self.fft_channel.object.fftSize
                    status.psd_averages = self.fft_channel.object.num_averages
                    status.psd_time_between_ffts = self.fft_channel.object.time_between_fft_ms
                    status.psd_output_bin_size = self.fft_channel.object.outputBins
                    status.psd_window_type = self.fft_channel.object.window_type_str
                    status.psd_peak_mode = self.fft_channel.object.peak_mode_str
                    self._fft_status=copy.copy(status)
                    
            return self._fft_status

            return status

        def get_digital_signal_path(self):
            """
            For information only, return src tuner --> out module for this channel
            """
            ret=""
            if self.digital_rx_object:
                ret+=self.digital_rx_object.object.full_reg_name
            ret+=" --> "
            if self.output_object:
                ret+=self.output_object.object.full_reg_name
            return ret

        def getEnable(self):
            """
            Get enable status for tuner modules assigned to channel
            """
            ret=True
            if self.analog_rx_object:
                ret&=self.analog_rx_object.object.enable
            if self.digital_rx_object:
                ret&=self.digital_rx_object.object.enable
            if self.swddc_object:
                ret&=self.swddc_object.object.enable
            return ret

        def setEnable(self, enable=True, enabled_secondary=False):
            """
            Enable or disable tuner/output
            """
            ret=self.set_tuner_enable(enable, enabled_secondary )
            if ret:
                ret&=self.set_output_enable(enable)
            return ret

        def set_tuner_enable(self, enable=True, enabled_secondary=False):
            """
            Enable/Disable all tuners assigned to this channel
            """
            ret=True
            if self.digital_rx_object and not enabled_secondary:
                ret&=self.digital_rx_object.object.setEnable(enable)
            if self.swddc_object:
                ret&=self.swddc_object.object.setEnable(enable)
            return ret

        def set_output_enable(self, enable=True):
            """
            Enable/Disable output modules assigned to the source digital tuner for this channel
            """
            ret=True
            if self.has_streaming_output():
                ret=self.output_object.object.setEnable(enable)
            return ret

        def getChildChannels(self):
            return self.rx_child_objects[:]

        def updateChildChannelOffsets(self, new_cf):
            if self.rx_child_objects:
                for ch in self.rx_child_objects:
                    ch.updateRFFrequencyOffset(new_cf)

        def centerFrequencyRetune(self, new_cf, keepOffset=False):
            retval=True
            # try to keep the channels frequency the same for the new_cf value
            # if not and keepOffset is True, then we just move relative to the new cf
            #
            old_offset=ch.getRFFrequencyOffset()
            old_cf = ch.getFrequency_Hz()
            if self._debug:
                print "centerFrequencyRetune, channel ", ch.msdd_channel_id(), " new center frequency ", new_cf, " old freq ", old_cf, " old offset", old_cf-old_offset
            ch.updateRFFrequencyOffset(new_cf)
            try:
                ch.setFrequency_Hz(old_cf)
                if self._debug:
                    print "centerFrequencyRetune, channel ", ch.msdd_channelc_id(), " able to keep same frequency ", old_cf
                retval=True
            except:
                if self._debug:
                    traceback.print_exc()
                # unable to keep the same frequency so keep same offset
                retval=False
                if keepOffset:
                    freq_diff=old_cf - old_offset
                    ch_new_freq = new_cf + freq_diff
                    try:
                        ch.setFrequency_Hz(ch_new_freq)
                        if self_debug:
                            print "centerFrequencyRetune, channel ", ch.msdd_channel_id(), " keep offset same new freq ", ch_new_freq
                        retval=True
                    except:
                        retval=False
                        if self._debug:
                            traceback.print_exc()

            return retval

        def updateRFFrequencyOffset(self, new_cf):
            if self.digital_rx_object:
                self.digital_rx_object.object.updateRFFrequencyOffset(new_cf)

        def getRFFrequencyOffset(self):
            if self.digital_rx_object:
                return self.digital_rx_object.object.rf_offset_hz

        def get_valid_frequency(self, if_freq_hz):
            if self.analog_rx_object:
                return self.analog_rx_object.object.get_valid_frequency(if_freq_hz)
            if self.digital_rx_object:
                return self.digital_rx_object.object.get_valid_frequency(if_freq_hz)

        def getFrequency_Hz(self):
            if self.analog_rx_object:
                return self.analog_rx_object.object.getFrequency_Hz()
            if self.digital_rx_object:
                return self.digital_rx_object.object.getFrequency_Hz()

        def setFrequency_Hz(self, freq, updateOffsets=False):
            if self.analog_rx_object:
                return self.analog_rx_object.object.setFrequency_Hz(freq)
            if self.digital_rx_object:
                if updateOffsets:
                    self.updateChildChannelOffsets(freq)
                return self.digital_rx_object.object.setFrequency_Hz(freq)
        
        def getSampleRate(self):
            if self.swddc_object is None:
                return self.digital_rx_object.object.getSampleRate()
            else:
                return self.swddc_object.object.getSampleRate()

        def setSampleRate(self, srate):
            if self.is_analog_only(): return True

            success=False
            if srate is None: return success

            if self.digital_rx_object is None: return success

            # get sample rate cache..
            self._get_sample_rates()

            #  grab sample rate for the input rate
            if srate in self._srates:
                if self.digital_rx_object and self._srates[srate][1]:
                    success=self.digital_rx_object.object.setSampleRate(self._srates[srate][1])
                if self.swddc_object and self._srates[srate][2]:
                    success&=self.swddc_object.object.setSampleRate(self._srates[srate][2])

            return success

        def getSampleRateLimits(self):
            srates=self._get_sample_rates().keys()
            srates.sort()
            return srates

        def _get_sample_rates(self):
            if len(self._srates) == 0:
                srates={}
                try:
                    if self.digital_rx_object:
                        hw_ddc_srates = self.digital_rx_object.object.getSampleRateList()
                        hw_ddc_bw_rates = self.digital_rx_object.object.getBandwidthList_Hz()
                        # fill out bw list if shorter than srates
                        if len(hw_ddc_bw_rates) < len(hw_ddc_srates):
                            hw_ddc_bw_rates = hw_ddc_bw_rates + [ hw_ddc_bw_rates[-1] ]*(len(hw_ddc_srates)-len(hw_ddc_bw_rates))
                        for n, hw_ddc_srate in zip(range(len(hw_ddc_srates)), hw_ddc_srates):
                            self.digital_rx_object.object.setSampleRate(hw_ddc_srate)
                            if self.swddc_object:
                                sw_ddc_srates = self.swddc_object.object.getSampleRateList()
                                sw_ddc_bw_rates = self.swddc_object.object.getBandwidthList_Hz()
                                for i, sw_ddc_srate in zip(range(len(sw_ddc_srates)), sw_ddc_srates):
                                    bw=min(sw_ddc_bw_rates[i],hw_ddc_bw_rates[n])
                                    srates[sw_ddc_srate] = ( None, hw_ddc_srate, sw_ddc_srate, bw )
                            else:
                                if n >= len(hw_ddc_bw_rates): n=-1
                                srates[ hw_ddc_srate ] = ( None, hw_ddc_srate, None, hw_ddc_bw_rates[n])
                except:
                    traceback.print_exc()
                    srates={}

                self._srates=srates
            return self._srates


        def validate_sample_rate(self, srate):
            srates=self.getSampleRateLimits()
            srates.sort()
            return check_value_from_values(srate, srates)

        def get_valid_sample_rate(self, srate=None, srate_tolerance=None, return_max=None ):
            """
            For a given sample rate (srate), determine if the channel can support the rate. Return
            an actual sample rate supported between srate to (srate + (srate*(srate_tolerance/100))) or None
            if srate == None or  <= 0.0 return lowest possible sample rate
            """
            valid_sr=None
            srates=self._get_sample_rates().keys()
            srates.sort()
            if self._debug:
                print "get_valid_sample_rate, digital rx check srate ", srate, " srate_tol ", srate_tolerance, " sample rates ", srates
            valid_sr=get_value_valid_list( srate, srate_tolerance, srates, return_max )
            if self._debug:
                print "get_valid_sample_rate, digital rx check srate ", srate, " valid sample rate", valid_sr
            return valid_sr


        def getBandwidth_Hz(self):
            if self.swddc_object is None:
                return self.digital_rx_object.object.getBandwidth_Hz()
            else:
                return self.swddc_object.object.getBandwidth_Hz()

        def setBandwidth_Hz(self, bw_hz ):
            if bw_hz is None: return False
            self._get_bandwidths()
            success=False
            if bw_hz in self._bw_rates:
                success=True
                if self.analog_rx_object and self._bw_rates[bw_hz][0]:
                    success&=self.analog_rx_object.object.setBandwidth_Hz(self._bw_rates[bw_hz][0])
                if self.digital_rx_object and self._bw_rates[bw_hz][1]:
                    success&=self.digital_rx_object.object.setBandwidth_Hz(self._bw_rates[bw_hz][1])
                if self.swddc_object and self._bw_rates[bw_hz][2]:
                    success&=self.swddc_object.object.setBandwidth_Hz(self._bw_rates[bw_hz][2])
            return success

        def getBandwidthLimits(self):
            bw_rates=self._get_bandwidths().keys()
            bw_rates.sort()
            return bw_rates

        def _get_bandwidths(self):
            if len(self._bw_rates) == 0:
                bw_rates={}
                try:
                    analog_bws=[None]
                    if self.analog_rx_object:
                        analog_bws = self.analog_rx_object.object.getBandwidthList_Hz()
                    for analog_bw in analog_bws:
                        if analog_bw:
                            self.analog_rx_object.object.setBandwidth_Hz(analog_bw)
                        if self.digital_rx_object:
                            # get sample rate and bandwidth list from hw ddc module
                            hw_ddc_srates = self.digital_rx_object.object.getSampleRateList()
                            hw_ddc_bw_rates = self.digital_rx_object.object.getBandwidthList_Hz()
                            # fill out bw list if shorter than srates
                            if len(hw_ddc_bw_rates) < len(hw_ddc_srates):
                                hw_ddc_bw_rates = hw_ddc_bw_rates + [ hw_ddc_bw_rates[-1] ]*(len(hw_ddc_srates)-len(hw_ddc_bw_rates))
                            for n, hw_ddc_bw in zip(range(len(hw_ddc_bw_rates)), hw_ddc_bw_rates):
                                self.digital_rx_object.object.setSampleRate(hw_ddc_srates[n])
                                if self.swddc_object:
                                    sw_ddc_rates = self.swddc_object.object.getSampleRateList()
                                    sw_ddc_bw_rates = self.swddc_object.object.getBandwidthList_Hz()
                                    for i, sw_ddc_bw in zip(range(len(sw_ddc_bw_rates)), sw_ddc_bw_rates):
                                        # choose lowest possible value for srate/bw pair
                                        srate=min( hw_ddc_srates[n], sw_ddc_rates[i])
                                        bw=min(hw_ddc_bw, sw_ddc_bw)
                                        bw_rates[bw] = ( analog_bw, hw_ddc_bw, sw_ddc_bw, srate)
                                else:
                                    if n>= len(hw_ddc_srates) : n = -1
                                    bw_rates[ hw_ddc_bw ] = ( analog_bw, hw_ddc_bw, None, hw_ddc_srates[n])
                        else:
                            if analog_bw:
                                bw_rates[analog_bw] = ( analog_bw, None, None, None)
                except:
                    bw_rates={}

                self._bw_rates=bw_rates
            return self._bw_rates

        def validate_bandwidth(self, bw_hz):
            bw_list = self.getBandwidthLimits()
            return check_value_from_values(bw_hz, bw_list)

        def get_valid_bandwidth(self, bw_hz=None, bw_tolerance=None, return_max=None):
            """
            For a given bandwidth bw_hz, determine if the channel can support the rate. Return
            an actual bandwidth supported between bw_hz to (bw_hz + (bw_hz*bw_tolerance/100))) or None
            if bw_hz == None or  <= 0.0 return lowest possible bw_hz
            """
            valid_bw=None
            bw_rates=self._get_bandwidths().keys()
            bw_rates.sort()
            if self._debug:
                print "get_valid_bandwidth, digital rx check bw ", bw_hz, " bw_tol ", bw_tolerance, " bandwidths ", bw_rates
            valid_bw=get_value_valid_list( bw_hz, bw_tolerance, bw_rates, return_max )
            if self._debug:
                print "get_valid_bandwidth, digital rx check bw ", bw_hz, " valid bandwidth", valid_bw
            return valid_bw

        def get_bandwidth_for_sample_rate(self, srate ):
            """
            For a specified sample rate find the matching bandwidth for the channel

            Parameters:
            -----------
            srate : sample rate in hz to use for the look up
            """
            if not len(self._srates): self._get_sample_rates()
            if srate in self._srates:
                # the sample rate cache contains the bandwidth setting for a sample rate
                return self._srates[srate][3]
            return None

        def get_sample_rate_for_bandwidth(self, bw ):
            """
            For a specified bandwidth find the matching sample rate for the channel

            Parameters:
            -----------
            bw : bandwidth in hz to use for the look up
            """
            if not len(self._bw_rates): x=self._get_bandwidths()
            if bw in self._bw_rates:
                # the bandwidth cache contains the sample rate for a specifc bandwidth
                return self._bw_rates[bw][3]
            return None

    class secondary_channel_module(rx_channel):
        """
        Exposed receiver channels that are created from SWDDC modules that require a 
        primary rx_channel.
         rcv -> wbddc -> nbddc ->  out (primary)
                              `->swddc (secondary) -> out
        """
        def __init__(self, msdd_radio, digital_mod, primary_mod, output_mod, rx_channel_num ):
            super(MSDDRadio.secondary_channel_module,self).__init__( msdd_radio,
                                                                     digital_mod,
                                                                     rx_channel_num,
                                                                     False,
                                                                     MSDDRadio.MSDDRXTYPE_SW_DDC )
            self.digital_rx_object=digital_mod
            self.primary_channel=primary_mod
            self.digital_rx_object.object.setParent(self.primary_channel.object)
            self.output_object=output_mod

        def get_digital_signal_path(self):
            """
            For information only, return src tuner --> out module for this channel
            """
            ret=""
            pb=None
            if self.primary_channel:
                pb=self.primary_channel.channel_id()
            if self.digital_rx_object:
                ret+=self.digital_rx_object.object.full_reg_name
            if pb:
                ret+=" (piggyback:"+pb+") --> "
            else:
                ret+=" --> "
            if self.output_object:
                ret+=self.output_object.object.full_reg_name
            return ret


        def setEnable(self, enable=True):
            """
            Enable or disable tuner/output
            """
            ret=self.set_tuner_enable(enable)
            if ret:
                ret=self.set_output_enable(enable)

        def set_tuner_enable(self, enable=True):
            """
            Enable/Disable all tuners assigned to this channel
            """
            ret=True
            if self.digital_rx_object:
                ret&=self.digital_rx_object.object.setEnable(enable)
            return ret

        def set_output_enable(self, enable=True):
            """
            Enable/Disable output modules assigned to the source digital tuner for this channel
            """
            ret=True
            if self.has_streaming_output():
                ret=self.output_object.object.setEnable(enable)
            return ret

        def _get_bandwidths(self):
            """
            Get the current set of bandwidths supported by this channel. Since
            we are secondary channel, then we are tied the parent's current bandwidth
            """
            bw_list=[]
            if self.digital_rx_object:
                bw_list=self.digital_rx_object.object.getBandwidthList_Hz()[:]
            return bw_list

        def valid_bandwidth(self, bw_hz):
            bw_list=self._get_bandwidths()
            return check_value_from_values(bw_hz, bw_list)

        def get_valid_bandwidth(self, bw_hz=None, bw_tolerance=None, return_max=None):
            """
            # grab primary's sample rate.. apply swddc decimations
            """
            valid_bw=None
            bw_list=self._get_bandwidths()
            if self._debug:
                print "get_valid_bandwidth,  check bw ", bw_hz, " bw_tol ", bw_tolerance, " bw_list ", bw_list
            valid_bw = get_value_valid_list( bw_hz, bw_tolerance, bw_list, return_max )
            return valid_bw

        def setBandwidth_Hz(self, bw ):
            if self.digital_rx_object:
                return self.digital_rx_object.object.setBandwidth_Hz(bw)
            return False

        def getBandwidthLimits(self):
            bw_list=self._get_bandwidths()
            bw_list.sort()
            return bw_list

        def _get_sample_rates(self):
            """
            Get the current set of sample rates supported by this channel. Since
            we are secondary channel, then we are tied the parent's current sample rate
            """
            srate_list=[]
            if self.digital_rx_object:
                srate_list=self.digital_rx_object.object.getSampleRateList()[:]
            return srate_list

        def valid_sample_rate(self, srate):
            srate_list=self._get_sample_rates()[:]
            return check_value_from_values(srate, srate_list)

        def get_valid_sample_rate(self, srate=None, srate_tolerance=None, return_max=None ):
            """
            # grab primary's sample rate.. apply my decimations
            """
            valid_sr=None
            if self.digital_rx_object and self.primary_channel:
                parent_srate = self.primary_channel.object.getSampleRate()
                dec_list  = self.digital_rx_object.object.getDecimationList()
                if self._debug:
                    print "get_valid_sample_rate, swddc decimation list ", dec_list
                srate_list = [ parent_srate/x for x in dec_list ]
                valid_sr = get_value_valid_list( srate, srate_tolerance, srate_list, return_max )

            return valid_sr

        def getSampleRateLimits(self):
            srate_list=self._get_sample_rates()
            srate_list.sort()
            return srate_list

        def getSampleRate(self):
            if self.digital_rx_object:
                return self.digital_rx_object.object.getSampleRate()
            return None

        def setSampleRate(self, srate ):
            if self.digital_rx_object:
                return self.digital_rx_object.object.setSampleRate(srate)
            return False


    class fft_channel(object):
        def __init__(self, fft_mod, output_mod,router, link_output=False ):
            self._debug=False
            self.fft=fft_mod
            self.output=output_mod
            self.stream_router=router
            self.upstream=None
            if link_output:
                self.stream_router.linkModules(self.fft.channel_id(), self.output.channel_id())

        def channel_number(self):
            return self.fft.channel_number

        def channel_id(self):
            if self.fft:
                return self.fft.channel_id()
            return None

        def setEnable(self, enable):
            if self.fft:
                self.fft.object.enable=enable
            if self.output:
                self.output.object.enable=enable

        def set_fft_enable(self, enable):
            if self.fft:
                self.fft.object.enable=enable

        def set_output_enable(self, enable):
            if self.output:
                self.output.object.enable=enable

        def link(self, upstream_mod ):
            if self.stream_router:
                if upstream_mod:
                    self.stream_router.linkModules(upstream_mod.channel_id(), self.fft.channel_id())
                    self.upstream=upstream_mod

        def unlink(self):
            mod=self.upstream
            if mod:
                if self.stream_router:
                    self.stream_router.unlinkModule(self.fft.channel_id())
                    self.upstream=None
            return mod


    """MSDDRadio: Manages a single MSDD-X000 single channel or MSDD-0600 dual channel radio
       Note that this class is just a container for the individual modules
       The module instances should be accessed directly for command and control


    """
    def __init__(self,
                 address,
                 port,
                 udp_timeout=0.5,
                 enable_fft_channels=False,
                 connection_debug=False,
                 radio_debug=False
    ):

        start_time_total = time.time()

        #Set up sockets
        self.radioAddress = (address, int(port))
        # for initial setup we want IO requests to process fast so choose smaller value 
        _timeout=min(udp_timeout,0.5)
        self.connection = Connection(self.radioAddress,_timeout)
        self.connection._debug=connection_debug
        self.enable_fft_channels=enable_fft_channels
        
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
	self._debug=radio_debug
        
        #setup the basic modules    
        self.console = ConsoleModule(self.connection)       #CON
        self.console.echo=True
        self.stream_router = StreamRouterModule(self.connection)   #SRT
        
        # Determine mapping version
        mapping_version = 2
        batch_filename = self.console.getFilenameBatch()
        m = re.search('_Gen(\d+)_Map',batch_filename)
        if m:
            mapping_version = int(m.group(1))

        start_time=time.time()
        # create map of module type to instances, Key: module type, Value: list of all corresponding module instances
        mod_count=0
        self.registered_modules={}
        if self._debug:
            print "MSDD Application Modules ", self.stream_router.modules
        for module in self.stream_router.modules:
            try:
                reg_name = self.stream_router.getRegistrationName(module)
                if self.registered_modules.has_key(module):
                    continue
                ch_list = self.stream_router.getChannelLimits(reg_name)
                if self._debug:
                    print "Module ", reg_name, " channels ", ch_list
                for channel in ch_list:
                    mod_count+=1
                    reg_full = str(reg_name) + ':' + str(channel)
                    inst_name = self.stream_router.getInstallName(reg_full)
                    self.registered_modules.setdefault(module,[]).append(self.registered_module(reg_name,inst_name,channel))
            except:
               pass

        print "(duration:{:.4f} MSDD Registered module count {} {}".format((time.time()-start_time), mod_count, self.radioAddress)
        self.msdd_id = self.console.ID
        if self._debug:
            print "Completed building registered module lists for ", self.msdd_id
                
        start_time=time.time()
        self.fft_object_container = object_container()
        self.spectral_scan_object_container = object_container()
        self.output_object_container = object_container()
        self.swddc_object_container = object_container()
        
        self.fft_channels_container = object_container()
        self.fft_channels = []
        self.rx_channels = []
        self.active_output_modules=[]
        self.fullRegName_to_RxChanNum = {}
        self.msdd_channel_to_rx_channel = {}
        self.module_dict = {}

        #
        # check number of network interfaces for multi interface devices
        # addresses
        for reg_mod in self.registered_modules['NETWORK']:
            obj= NetModule(self.connection,reg_mod.channel_number,mapping_version)
            obj_mod = self.object_module(reg_mod,obj)
            self.network_modules.append(obj_mod)

        for inst_name,reg_mod_list in self.registered_modules.items():
            for reg_mod in reg_mod_list:
                try:
                    obj_mod = None
                    if self._debug:
                        print "MSDD Module type: ", str(reg_mod.installation_name),   \
                        " channel ID: " +  str(reg_mod.registration_name) + ':' +str(reg_mod.channel_number)
                    if inst_name == "CON":
                        obj= ConsoleModule(self.connection,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod,obj)
                        self.console_modules.append(obj_mod)
                        break #Do not need more than one console module
                    elif inst_name == "SRT":
                        obj= StreamRouterModule(self.connection,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod,obj)
                        self.stream_modules.append(obj_mod)
                    elif inst_name == "BRD":
                        obj= BoardModule(self.connection,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod,obj)
                        self.board_modules.append(obj_mod)
                    elif inst_name == "NETWORK":
                        continue
                    elif inst_name == "SWDDCDEC2":
                        obj= SWDDCModule(self.connection,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod,obj)
                        self.software_ddc_modules.append(obj_mod)
                        self.swddc_object_container.put_object(obj_mod)
                    elif inst_name == "IQB":
                        obj= IQCircularBufferModule(self.connection,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod,obj)
                        self.iq_circular_buffer_modules.append(obj_mod)
                    elif inst_name == "OUT":
                        obj= OUTModule(self.connection,reg_mod.channel_number,mapping_version,self.isMultiInterface())
                        obj_mod = self.object_module(reg_mod,obj)
                        self.out_modules.append(obj_mod)
                        self.output_object_container.put_object(obj_mod)
                    elif inst_name == "TOD":
                        obj= TODModule(self.connection,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod,obj)
                        self.tod_modules.append(obj_mod)
                    elif inst_name == "SPC":
                        obj= SpectralScanModule(self.connection,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod,obj)
                        self.spectral_scan_modules.append(obj_mod)
                        self.spectral_scan_object_container.put_object(obj_mod)
                    elif inst_name == "FFT":
                        obj= FFTModule(self.connection,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod,obj)
                        self.fft_modules.append(obj_mod)
                        self.fft_object_container.put_object(obj_mod)
                    elif inst_name == "LOG":
                        obj= LOGModule(self.connection,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod,obj)
                        self.log_modules.append(obj_mod)
                    elif inst_name == "WBDDC":
                        obj= WBDDCModule(self.connection,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod,obj)
                        self.wb_ddc_modules.append(obj_mod)
                    elif inst_name == "NBDDC":
                        obj= NBDDCModule(self.connection,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod,obj)
                        self.nb_ddc_modules.append(obj_mod)
                    elif re.match('MSDR\d{4}', inst_name):
                        obj= MSDDX000_RcvModule(self.connection,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod,obj)
                        self.msdrx000_modules.append(obj_mod)
                    elif inst_name == "MSDR_RS422":
                        obj= RS422_RcvModule(self.connection,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod,obj)
                        self.msdr_rs422_modules.append(obj_mod)
                    elif inst_name == "TFN":
                        obj= TFNModule(self.connection,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod,obj)
                        self.tfn_modules.append(obj_mod)
                    elif inst_name == "GPS":
                        obj= GpsNavModule(self.connection,reg_mod.channel_number,mapping_version)
                        obj_mod = self.object_module(reg_mod,obj)
                        self.gps_modules.append(obj_mod)
                    
                    if obj_mod != None:
                        full_reg_name = obj_mod.object.get_full_reg_name()
                        self.module_dict[full_reg_name] = obj_mod
                except NotImplementedError:
                    pass

        print "(duration:{:.4f} MSDD Created individual modules for radio {}".format((time.time()-start_time),self.radioAddress)

        start_time=time.time()
        for mod in self.spectral_scan_modules:
            mod.object._setEnable(False)
            self.stream_router.unlinkModule(mod.channel_id())            
        print "(duration:{:.4f} MSDD Unlinked SPC modules for radio {}".format((time.time()-start_time),self.radioAddress)


        #
        # Create RX Channels 
        #
        
        #
        # generate channels for RCV modules and find its associated WBDDC module
        #
        start_time=time.time()
        for mod in self.msdrx000_modules + self.msdr_rs422_modules:
            rx_mod = self.rx_channel(self,mod,len(self.rx_channels),True,self.MSDDRXTYPE_ANALOG_RX)
            rx_mod.analog_rx_object = mod
            rx_mod.output_object=None
            current_position=len(self.rx_channels)
            if self._debug:
                print " Analog RCV {}  for rx_channel {} ".format(mod.object.channel_id(),current_position)
            if current_position < len(self.wb_ddc_modules):
                rx_mod.msdd_rx_type = self.MSDDRXTYPE_DIGITAL_RX
                rx_mod.digital_rx_object = self.wb_ddc_modules[current_position]
                rx_mod.output_object = self.get_corresponding_output_module_object(rx_mod.digital_rx_object)
            if self._debug:
                oreg_name =""
                if rx_mod.output_object:
                    oreg_name = rx_mod.output_object.object.full_reg_name
                print " rx_channel {} RCV->WBDDC->OUT {} -> {} -> {}".format(current_position, mod.object.channel_id(), rx_mod.digital_rx_object.object.channel_id(),  oreg_name)
            self.output_object_container.mark_as_used(rx_mod.output_object)
            self.save_active_output_modules(rx_mod.output_object)
            self.rx_channels.append(rx_mod)
        print "(duration:{:.4f} MSDD Finished RCV-WBDDC channels, added {} RX channels for radio {}".format((time.time()-start_time), len(self.rx_channels), self.radioAddress)

        #
        # generate channels for NBDDC modules and check for swddc module and output modules
        #
        start_time=time.time()
        for mod in self.nb_ddc_modules:
            rx_mod = self.rx_channel(self,mod,len(self.rx_channels), True, self.MSDDRXTYPE_HW_DDC)
            rx_mod.digital_rx_object = mod
            rx_mod.swddc_object = self.get_inline_swddc_module_object(mod)
            if rx_mod.swddc_object:
                rx_mod.output_object = self.get_corresponding_output_module_object(rx_mod.swddc_object)
            else:
                rx_mod.output_object = self.get_corresponding_output_module_object(mod)
            if self._debug:
                oreg_name =""
                swddc_name="<empty>"
                if rx_mod.output_object:
                    oreg_name = rx_mod.output_object.object.full_reg_name
                if rx_mod.swddc_object:
                    swddc_name = rx_mod.swddc_object.object.full_reg_name
                print "rx_channel {} NBDDC-> (SWDDC) -> OUT {} -> {} -> {}".format( len(self.rx_channels), mod.object.channel_id(), swddc_name, oreg_name)
            self.output_object_container.mark_as_used(rx_mod.output_object)
            self.save_active_output_modules(rx_mod.output_object)
            self.swddc_object_container.mark_as_used(rx_mod.swddc_object)
            self.rx_channels.append(rx_mod)
        print "(duration:{:.4f} MSDD Finished NBDDC channels, added {}  RX channels for radio {}".format((time.time()-start_time), len(self.nb_ddc_modules), self.radioAddress)

        #
        # Enable FFT (Spectral output) for RX Channels
        #
        if enable_fft_channels:
            if self._debug:
                print "MSDD Creating FFT channels ", len(self.fft_modules)
            start_time=time.time()
            # create fft channels
            for fft_mod in self.fft_modules:
                fft_mod.object._setEnable(False)
                self.stream_router.unlinkModule(mod.channel_id())            
                link_output=False
                output = self.get_corresponding_output_module_object(fft_mod)
                if output is None:
                    output=self.output_object_container.get_object()
                    link_output=True

                if output is None: break

                fft=self.fft_object_container.get_object()
                if fft is None:
                    if output:
                        self.output_object_container.put_object(output)
                        break

                if self._debug:
                    print "FFT channel ", fft.channel_id(),  " -> ", output.channel_id()
                fft_ch=self.fft_channel( fft, output, self.stream_router, link_output )
                self.fft_channels.append(fft_ch)
                self.fft_channels_container.put_object(fft_ch)
            print "(duration:{:.4f} Created {} FFT Channels for radio {}".format((time.time()-start_time), len(self.fft_channels), self.radioAddress)

        start_time=time.time()

        #
        # Create reverse lookup tables based on msdd channel id values to rx_channel numbers
        #
        for rx_pos, rx_channel in zip(range(len(self.rx_channels)), self.rx_channels):
            if self._debug:
                print "update Channel ", rx_channel.msdd_channel_id()
            self.update_rx_channel_mapping( rx_channel, rx_pos)
        print "(duration:{:.4f} MSDD Build reverse lookup, msdd mod->rx_channel {} {}".format((time.time()-start_time), len(self.msdd_channel_to_rx_channel), self.radioAddress)

        start_time=time.time()
        # CREATE PARENT CHILD MAPPING FOR TUNERS
        for rx_pos, rx_channel in zip(range(len(self.rx_channels)), self.rx_channels):
            rx_channel.rx_child_objects=[]
            for mod in [ rx_channel.analog_rx_object, rx_channel.digital_rx_object ]:
                if mod == None: continue
                # get list of msdd modules that are linked to mod, an msdd module
                child_lst = self.stream_router.getModulesFlowListBySource(mod.object.full_reg_name)
                if self._debug:
                    print "Source Module: ", mod.object.channel_id(), " child list: ", child_lst
                for child in child_lst:
                    # skip over ourself.
                    if child == mod.object.channel_id(): continue
                    try:
                        # skip over module that are linked to this channel
                        rx_child=self.msdd_channel_to_rx_channel[child]
                        if rx_child == rx_channel:
                            continue
                        if self._debug:
                            print "Adding Child ", rx_child.msdd_channel_id() , " to ", rx_channel.msdd_channel_id()
                        rx_channel.rx_child_objects.append(rx_child)
                        rx_child.rx_parent_object = rx_channel
                        if self._debug:
                            print "Assigning Parent ", rx_channel.msdd_channel_id(), "to ", rx_child.msdd_channel_id()
                    except:
                        # skip over modules that are not part of our lookup index
                        pass

        print "(duration:{:.4f} MSDD Find tuner parent/child relationships for radio {}".format((time.time()-start_time),self.radioAddress)
        print time.ctime(), "Finished (duration:{:.4f} setting up MSDD {} connection {} RX CHANNELS {} FFT CHANNELS {}".format((time.time()-start_time_total), 
                                                                                                                               self.msdd_id , 
                                                                                                                               self.radioAddress, 
                                                                                                                               len(self.rx_channels), 
                                                                                                                               len(self.fft_channels))

        # we are all done now set the timeout to what the caller requested
        self.connection.set_timeout(udp_timeout)


    def isMultiInterface(self):
        return len(self.network_modules) > 1


    #
    # Perform unlinking of SWDDCS to removed load from DSP chip
    #
    def unlink_swddc_modules(self):
        time.sleep(.5)
        start_time=time.time()
        for mod in self.software_ddc_modules:
            if self._debug:
                print "Found a sw_ddc module unlink ", mod.object.full_reg_name, " channel id ", mod.channel_id()
            self.stream_router.unlinkModule(mod.channel_id())

            # remove swddc from its output module
            output = self.get_corresponding_output_module_object(mod)
            if output:
                if self._debug:
                    print "found output for swddc ", mod.channel_id(), " output ", output.channel_id()
                self.stream_router.unlinkModule(output.channel_id())
        print "(duration:{:.4f} MSDD Cleaning up SWDDC Modules (post) {}".format((time.time()-start_time),self.radioAddress)


    #
    # Save list of output modules that will be used to determine network utilization
    #
    def save_active_output_modules(self,output_module):
        if output_module == None: return
        # only save active instances
        for x in self.active_output_modules:
            if x == output_module : return
        self.active_output_modules.append(output_module)

    def update_rx_channel_mapping(self, rx_chan_mod, position):
        if rx_chan_mod == None:
            return
        if rx_chan_mod.analog_rx_object != None:
            self.fullRegName_to_RxChanNum[rx_chan_mod.analog_rx_object.object.full_reg_name] = position
            self.msdd_channel_to_rx_channel[rx_chan_mod.analog_rx_object.object.full_reg_name] = rx_chan_mod
        if rx_chan_mod.digital_rx_object != None:
            self.fullRegName_to_RxChanNum[rx_chan_mod.digital_rx_object.object.full_reg_name] = position
            self.msdd_channel_to_rx_channel[rx_chan_mod.digital_rx_object.object.full_reg_name] = rx_chan_mod
        if rx_chan_mod.swddc_object != None:
            self.fullRegName_to_RxChanNum[rx_chan_mod.swddc_object.object.full_reg_name] = position
            self.msdd_channel_to_rx_channel[rx_chan_mod.swddc_object.object.full_reg_name] = rx_chan_mod
        if rx_chan_mod.output_object != None:
            self.fullRegName_to_RxChanNum[rx_chan_mod.output_object.object.full_reg_name] = position
            self.msdd_channel_to_rx_channel[rx_chan_mod.output_object.object.full_reg_name] = rx_chan_mod
        if rx_chan_mod.fft_channel != None:
            self.fullRegName_to_RxChanNum[rx_chan_mod.fft_channel.fft.object.full_reg_name] = position
            self.msdd_channel_to_rx_channel[rx_chan_mod.fft_channel.fft.object.full_reg_name] = rx_chan_mod
        if rx_chan_mod.spectral_scan_object != None:
            self.fullRegName_to_RxChanNum[rx_chan_mod.spectral_scan_object.object.full_reg_name] = position
            self.msdd_channel_to_rx_channel[rx_chan_mod.spectral_scan_object.object.full_reg_name] = rx_chan_mod

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

    def get_root_parent(self, rx_chan_mod):
        last_parent=rx_chan_mod
        parent = rx_chan_mod.rx_parent_object
        while parent != None:
            parent = last_parent.rx_parent_object
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

    def get_rx_channel_from_msdd_channel(self, full_reg_name):
        if self.msdd_channel_to_rx_channel.has_key(full_reg_name):
            return self.fullRegName_to_RxChanNum[full_reg_name]
        return None

    
    def get_corresponding_output_module_object(self,object_module):
        if object_module == None:
            return None
        try:
            full_reg_name = object_module.object.get_full_reg_name()
            output_num = self.get_output_channel_for_module(full_reg_name)
            if output_num == None:
                return None
            for out_pos in range(0,len(self.out_modules)):
                if output_num == self.out_modules[out_pos].channel_number:
                    return self.out_modules[out_pos]
        except:
            return None
        return None

    def get_inline_swddc_module_object(self,object_module):
        if object_module == None:
            return None
        try:
            full_reg_name = object_module.object.get_full_reg_name()
            ch_num = self.get_swddc_channel_for_module(full_reg_name)
            if ch_num == None:
                return None
            for idx in range(0,len(self.software_ddc_modules)):
                if ch_num == self.software_ddc_modules[idx].channel_number:
                    return self.software_ddc_modules[idx]
        except:
            return None
        return None

    
    def get_corresponding_fft_module_object(self,object_module):
        if object_module == None:
            return None
        try:
            full_reg_name = object_module.object.get_full_reg_name()
            output_num = self.get_fft_channel_for_module(full_reg_name)
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
            if len(flow_list):
                flow_list=flow_list[1:]
            return create_csv(flow_list)
        except:
            return ""
        return ""

    def get_timeofday_module(self, idx=0 ):
        ret=None
        try:
            ret=self.tod_modules[idx].object
        except:
            pass
        return ret

    def get_active_output_modules(self):
        return self.active_output_modules[:]

    def get_output_channel_for_module(self,full_reg_name):
        modList = self.stream_router.getModulesFlowListBySource(full_reg_name)
        # trim off first item in list is requested item
        if len(modList):
            modList = modList[1:]
        if self._debug:
            print "get_output_channel_for_module ", full_reg_name, " modlist ", modList
            
        for module in modList:
            m = re.search('OUT(:(\d+)|)',module)
            if m:
                ch=0
                if m.group(2):
                    ch=int(m.group(2))
                return ch
        # try to look for swddc or sdc
        for module in modList:
            m = re.search('^SDC|SWDDC',module)
            if m:
                ch=self.get_output_channel_for_module(module)
                if ch != None:
                    return ch
        return None

    def get_swddc_channel_for_module(self,full_reg_name):
        try:
            modList = self.stream_router.getModulesFlowListBySource(full_reg_name)

            # only return a swddc if there are no output modules listed
            outmods=[ omod for omod in modList if omod.startswith('OUT') ]
            if len(outmods) > 0 : 
                return None

            for module in modList:
                if"SWDDCDEC2" == module or "SDC" == module:
                    return 0
                else:
                    m = re.search('SWDDCDEC2:(\d+)',module)
                    if m:
                        return int(m.group(1))
                    m = re.search('SDC:(\d+)',module)
                    if m:
                        return int(m.group(1))
            return None
        except:
            traceback.print_exc()
        return None

    def get_fft_channel_for_module(self,full_reg_name):
        modList = self.stream_router.getModulesFlowListBySource(full_reg_name)
        for module in modList:
            if module == "FFT":
                return 0
            else:
                m = re.search('FFT:(\d+)',module)
                if m:
                    return int(m.group(1))
        return None

    def save_fft_channel(self, fft ):
        self.fft_channels_container.put_object(fft)

    def get_fft_channel(self):
        return self.fft_channels_container.get_object()


if __name__ == '__main__':
    address = ''#raw_input('IP Address of Radio (127.0.0.1): ')
    if (address == ''):
        address = '127.0.0.1'
    port = ''#raw_input('Port of Radio (23): ')
    if (port == ''):
        port = '23'
    radio = MSDDRadio(address, port)
