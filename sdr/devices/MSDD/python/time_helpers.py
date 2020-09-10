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
import os
import sys
import math
import copy
import inspect 
import logging
import subprocess
import traceback
import time as _time
import threading
from datetime import datetime, timedelta
from pprint import pprint as pp

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
    fract_sec = utcdiff.microseconds / 1e6
    return [whole_sec,fract_sec]

class TimeOfDay(object):
    def __init__(self, 
                 tod_module,
                 logger,
                 time_set_cb=None,
                 host_delta_cb=None,
                 ntp_status_cb=None):

        self._baseLog=logger
        self.tod_module=tod_module
        self.time_set_cb=time_set_cb
        self.host_delta_cb=host_delta_cb
        self.ntp_status_cb=ntp_status_cb
        self.mode='SIM'
        self.output_protocol='sdds',
        self.get_time=get_seconds_from_start_of_year
        self.set_time=self.set_time_sim

    def monitor_time_variances(self):
        tod_bit=self.get_tod_bit()
        if tod_bit :
            msg='`tod bit? = {0}`: {1}'.format(tod_bit, self.tod_module.getBITStr(tod_bit))
            self._baseLog.warn_msg(msg)

        self.update_tod_host_delta()
        self.update_ntp_status()

    def initialize(self, time_of_day, output_protocol, enable_thread=False):
        if enable_thread:
            t=threading.Thread(target=self._initialize, args=(time_of_day, output_protocol,1.0))
            if t: 
                t.start()
        else:
            self._initialize(time_of_day,output_protocol)

    def _initialize(self, time_of_day, output_protocol, delay=None):
        if delay:
            _time.sleep(delay)

        # defer time monitoring till setup finishes
        self._baseLog.info_msg("Initialize Time Of Day (TOD) Module.")

        self.time_of_day_changed(time_of_day, output_protocol, set_time=False)

        # return False if we defer initialization, or IRIG is source
        time_set=False
        try:
            if self.tod_module.mode_readable != 'IRIGB':
                time_set=self.set_time_on_msdd(retries=1)
                if not time_set:
                    # defer time monitoring till setup finishes
                    t=threading.Thread(target=self._retry_time_setup)
                    if t: t.start()
        except:
            pass
        if self.time_set_cb:
            self.time_set_cb(time_set)

    def assign_get_time_method(self, output_protocol=None):
        proto=self.output_protocol
        if output_protocol:
            proto=output_protocol

        if proto == 'sdds':
            self._baseLog.debug_msg("Setting timestamp to SDDS time")
            self.get_time = get_seconds_from_start_of_year
        else:
            self._baseLog.debug_msg("Setting timestamp to GPS time")
            self.get_time = get_seconds_in_gps_time        

    def assign_set_time_method(self, mode=None):
        _mode=self.mode
        if mode:
            _mode=mode
        if _mode.upper() in [ 'SIM', 'ONEPPS']:
            # default to Simulated option
            if _mode.upper() == 'ONEPPS':  
                self._baseLog.debug_msg("Setting set time method to ONEPPS")
                self.set_time=self.set_time_onepps
            else:
                self._baseLog.debug_msg("Setting set time method to SIM")
                self.set_time=self.set_time_sim
    
    def time_of_day_changed(self, time_of_day, output_protocol, set_time=True):
        if not self.tod_module:
            return

        mode_str=""
        try:
            self.tod_module.mode = str(time_of_day.mode)
            msg='Setting time of day mode to {0}'.format(time_of_day.mode)
            self._baseLog.info_msg(msg)
            mode_str = self.tod_module.mode_readable
            self.mode=mode_str

            self.tod_module.ref_adjust = int(time_of_day.reference_adjust)
            self.tod_module.reference_track = int(time_of_day.reference_track)
            if str(time_of_day.mode) == "IRIGB":
                self.tod_module.toy = int(time_of_day.toy)
            else:
                self.tod_module.toy = 1
        except:
            msg='Error configuring time module, mode: {0} toy {1}'.format(time_of_day.mode,
                                                                          time_of_day.toy)
            self._baseLog.error_msg(msg)

        self.output_protocol=output_protocol
        # configure time source and set methods to configure time of day module
        self.assign_get_time_method(output_protocol)
        self.assign_set_time_method(mode_str)

        # set time for sim and onepps modes
        if mode_str in ['SIM', 'ONEPPS'] and set_time:
            self.set_time_on_msdd()

    def _retry_time_setup(self):
        self._baseLog.info_msg("Retry thread to setup MSDD Time of Day ")
        self.set_time_on_msdd()
        # kick off update thread to monitor time of day 
        if self.time_set_cb:
            self._baseLog.trace_msg('Enabling time checks from service function')
            self.time_set_cb(True)

    def update_tod_host_delta(self):
        if not self.tod_module:
            return

        if self.tod_module.mode_readable == 'IRIGB':
            return
        host_time = self.get_time()
        tod_time = self.tod_module.time
        host_delta=host_time - tod_time
        msg='tod_host_delta, host {0} tod {1} delta {2}'.format(host_time, tod_time, host_delta)
        self._baseLog.trace_msg(msg)
        if self.host_delta_cb:
            msg='calling host_delta_cb, delta {0}'.format(host_delta)
            self._baseLog.trace_msg(msg)
            self.host_delta_cb( host_delta )
        else:
            if abs(host_time - tod_time) > 0.5:
                self._baseLog.warn_msg('host time and TOD module time differ by >0.5s')

    ##
    ## NTP Status methods
    ##
    def get_ntp_status(self):
        exception = None
        stdout = ''
        try:
            dnull=open('/dev/null')
            proc = subprocess.Popen(['service', 'ntpd', 'status'], stdout=subprocess.PIPE,stderr=dnull)
            stdout = proc.communicate()[0]
        except Exception as e:
            exception = e
        return exception, stdout

    def parse_ntp_status(self, stdout):
        for line in stdout.split('\n'):
            # el6
            if 'ntpd' in line and 'is running' in line:
                return True
            # el7
            if 'Active' in line and 'active (running)' in line:
                return True
        return False

    def update_ntp_status(self):
        if not self.tod_module:
            return
        if self.tod_module.mode_readable == 'IRIGB':
            return
        ntp_running=False
        exception, stdout = self.get_ntp_status()
        if exception:
            msg='`service ntpd status`:  {0}'.format(exception)
            self._baseLog.warn_msg(msg)
            ntp_running = False
        else:
            ntp_running = self.parse_ntp_status(stdout)
            msg='ntp_running:  {0}'.format(ntp_running)
            self._baseLog.trace_msg(msg)
        if self.ntp_status_cb:
            msg='`calling ntp status`:  {0}'.format(ntp_running)
            self._baseLog.trace_msg(msg)
            self.ntp_status_cb(ntp_running)


    ##
    ## TOD Module Bit Error Check
    ##
    def get_tod_bit(self):
        """Return value of `tod bit?`.

        `tod.bit` is not reset when `tod mod <n>` is set.
        It is reset when it is read.
        Usually, it is repopulated at the next whole second. However, if tod.mode == ONEPPS and the
        1PPS cable is unplugged, it is not repopulated until somewhere between 20 to 30s later.
        """
        try:
            if not self.tod_module:
                return

            tod_bit=self.tod_module.bit  # clear existing value
            start = int(_time.time())
            while int(_time.time()) == start:  # wait for next second rollover
                _time.sleep(0.01)
            tod_bit = self.tod_module.bit
            msg='tod.mode:  {0},  tod.bit:  {1}'.format(self.tod_module.mode_readable, tod_bit)
            self._baseLog.trace_msg(msg)
            return tod_bit
        except:
            pass
        return 0


    def check_tod_bit(self):
        tod_bit = self.get_tod_bit()
        if tod_bit :
            msg='`tod bit? = {0}`: {1}'.format(tod_bit, self.tod_module.getBITStr(tod_bit))
            self._baseLog.warn_msg(msg)
        return tod_bit

    ##
    ## Set time on MSDD support methods
    ##

    def set_time_onepps(self):
        """Set tod.time when tod.mode == ONEPPS."""
        if not self.tod_module:
            return

        time_trigger=False
        # wait until tod on radio is less than .5 to allow for 
        # delivery of 150ms before pulse
        while not time_trigger:
            tod_time = self.tod_module.time
            allowable=tod_time-int(tod_time)
            if allowable < .46:
                break
            allowable=1.0-allowable
            msg='set_time_onepps, wait for next time check {0}'.format(allowable)
            self._baseLog.debug_msg(msg)
            if allowable > 0.0:
                _time.sleep(allowable)

        wtime=self.get_time()
        time_offset=1
        msg='set_time_onepps, get_time {0} tod time {1} offset {2} allowable {3} delta {4}'.format(wtime, 
                                                                                                    tod_time,
                                                                                                    time_offset,
                                                                                                    allowable,
                                                                                                    wtime-self.tod_module.time)
        self._baseLog.debug_msg(msg)

        # 2020-03 email from MMS:
        #     "TOD SET <time in seconds>  must be delivered at least 150ms
        #      prior to the next rising edge of the 1PPS at the receiver input."
        # At this point, we should always be > 0.5s before 1PPS = tod_rollover.
        # The truncated time is set at the next whole second, so add 1 second.
        bit=self.tod_module.bit
        wtime = self.get_time()
        msg='set_time_onepps , setting MSDD time: get_time {0} get_time+offset {1} '.format(wtime, int(wtime) + time_offset)
        self._baseLog.debug_msg(msg)
        self.tod_module.time=int(wtime) + time_offset

        # 2020-03 email from MMS:  "Must wait 2 seconds after transfer for valid time."
        _time.sleep(2.5)
        now=self.get_time()
        msg = 'set_time_onepps, checking time: now {0} tod {1} delta {2}'.format(now,
                                                                                 self.tod_module.time,
                                                                                 now-self.tod_module.time)
        self._baseLog.debug_msg(msg)


    def set_time_sim(self):
        """Set tod.time for the case tod.mode is SIM."""
        if not self.tod_module:
            return

        time_trigger=False
        # wait until tod on radio is less than .5 to allow for 
        # delivery of 150ms before pulse
        while not time_trigger:
            tod_time = self.tod_module.time
            allowable=tod_time-int(tod_time)
            if allowable < .46:
                break
            allowable=1.0-allowable
            if allowable > 0.0:
                _time.sleep(allowable)

        wtime=self.get_time()
        host_time=_time.time()
        host_delta=host_time-int(host_time)
        time_offset=1
        # if host tick occurs before tod tick then add another second and we are more than
        # halfway into the second
        if  host_delta > allowable and host_delta > .5:
            time_offset+=1

        msg='set_time_sim, host {0} tod time {1} offset {2} allowable {3} delta {4}'.format(host_time, 
                                                                                            tod_time,
                                                                                            time_offset,
                                                                                            allowable,
                                                                                            wtime-self.tod_module.time)
        self._baseLog.debug_msg(msg)


        # 2020-03 email from MMS:
        #     "TOD SET <time in seconds>  must be delivered at least 150ms
        #      prior to the next rising edge of the 1PPS at the receiver input."
        # At this point, we should always be > 0.5s before 1PPS = tod_rollover.
        # The truncated time is set at the next whole second, so add 1 second.
        bit=self.tod_module.bit
        wtime = self.get_time()
        msg='set_time_sim , setting MSDD time:  get_time {0} get_time+offset {1} tod time {2}'.format(wtime, int(wtime) + time_offset, self.tod_module.time)
        self._baseLog.debug_msg(msg)
        self.tod_module.time=int(wtime) + time_offset

        # 2020-03 email from MMS:  "Must wait minimum 2 seconds after transfer for valid time."
        _time.sleep(2.5)
        now=self.get_time()
        msg='set_time_sim, checking time: now {0} tod {1} delta {2}'.format(now,
                                                                            self.tod_module.time,
                                                                            now-self.tod_module.time)
        self._baseLog.debug_msg(msg)


    def set_time_on_msdd(self,retries=5):
        """Set tod.time on MSDD."""
        try:
            while retries and self.tod_module:
                msg='Retry setting time on MSDD, retries {0}'.format(retries)
                self._baseLog.info_msg(msg)
                # set time on radio based on source, sim, onepps, irig
                self.set_time()
                htime=int(self.get_time())
                mtime=int(self.tod_module.time)
                # check if times match..
                if htime == mtime :
                    msg="Time of Day SYNCED host {0} MSDD {1}".format(htime, mtime)
                    self._baseLog.info_msg(msg)
                    return True

                msg="set_time_on_msdd, Retry time NOT SYNCED host {0} msdd {1} ".format(htime, mtime)
                self._baseLog.debug_msg(msg)
                retries -=1
        except:
            traceback.print_exc()
        self._baseLog.warn_msg("Failed to synchronize Time of Day on MSDD and host's source value")
        return False

