#!/usr/bin/env python3
#
#
# AUTO-GENERATED
#
# Source: MSDD_Controller.spd.xml
from   omniORB import any as _any
from datetime import datetime, time, timedelta
import time as _time
import logging
import traceback
import pprint
import inspect
import os

from MSDD_Controller_base import *
from ossie.device import start_device
from ossie.cf import CF

# MSDD helper files/packages
import MSDD as _MSDD
from MSDD.msddcontroller import InvalidValue
from MSDD.msddcontroller import CommandException
from MSDD.msddcontroller import MSDDRadio
from MSDD.msddcontroller import create_csv
from MSDD.time_helpers import TimeOfDay
from dynamiccomponent import DynamicComponent

class MSDD_Controller_i(MSDD_Controller_base,DynamicComponent):
    """
    MSDD_Controller is the AggregateDevice parent for each receiver channel of the target MSDD radio. This class implements
    methods that will be employed by FEI 3.0 to define the parent/child relationship between the MSDD_Controller(parent) and its
    CompositeMSDD(child) class.  The proper FEI 3.0 design would have one MSDD_Controller and N WBDDC modules and their M NBDDC 
    tuners.  The choice was made to reuse the existing rh.MSDD device to manage the 1 WBDDC and M NBDDC tuners by filtering on the
    RCV module assigned to each WBDDC module, to reduce software maintenance, and missing FEI 3.0 API methods.

    The MSDD_Controller is responsible for managing all the singleton relationships between the REDHAWK proxies and the MSDD radio.
    The MSDD_Controller will establish the connection to the radio, initial radio configuration and tuner discovery.

    The MSDD_Controller manages the life-cycle of child CompositeMSDD devices by assigning a specific RCV/WBDDC module pairs to filter when
    selecting NBDDC devices. The properties to configure the child device are provided through the MSDD_Controller's properties that
    contain an added receiver_identifier property. After the child device is created and launched in separate thread, the CompositeMSDD's
    properties are directly changed using MSDD_Controller properties filter by receiver_identifier.

    """
    def __init__(self, devmgr, uuid, label, softwareProfile, compositeDevice, execparams):
        # save off any exec params that we are given to pass to children
        self._execparams=execparams
        MSDD_Controller_base.__init__(self, devmgr, uuid, label, softwareProfile, compositeDevice, execparams)

        # this class performs maintains the relationshipt between the parent and child devices
        DynamicComponent.__init__(self)

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


    def _makeExecParams(self):
        eparams={}
        # well known command line args to child devices command line
        params=["LOGGING_CONFIG_URI", "DEBUG_LEVEL" ]
        for x in params:
            if x in self._execparams:
                eparams[x]= self._execparams[x]
        return eparams
            

    def constructor(self):
        """
        Connect to MSDD instance, update status properties, and perform TOD initialization sequence.
        
        """
        self.MSDD = None
        self.msdd_console = None
        self.msdd_status=MSDD_Controller_base.MsddStatus()
        self.interval_update_msdd_status = 60
        self.update_msdd_status_mark = time.time()
        self.output_protocol='sdds'                     # set sdds as output stream protocol

        # dup required exec params to pass child MSDD 
        eparams=self._makeExecParams()

        # thread is auto started.. wait until we fully initialized to start time variance checks
        self._enableTimeChecks=False

        # determine time source method and output protocol
        self.output_protocol=self.determine_output_protocol()
        self.info_msg("Protocol for output streams will be {0}", self.output_protocol.upper())

        # 
        # connect to MSDDRadio
        # 
        if self.connect_to_msdd(): 

            # set clock reference
            try:
                self.process_clock_ref()
            except:
                raise CF.LifeCycle.InitializeError(self.format_msg_with_radio("Failure to process clock reference value {0} ", 
                                                                              self.msdd.clock_ref))

            # initialize time of day module
            try:
                # initialize time of day module with TimeOfDay class
                self._time_of_day=TimeOfDay(self.MSDD, 
                                            self,          
                                            self.enable_time_checks, 
                                            self.host_delta_cb, 
                                            self.ntp_status_cb)
                self._time_of_day.initialize(self.time_of_day, self.output_protocol)
            except:
                traceback.print_exc()
                raise CF.LifeCycle.InitializeError(self.format_msg_with_radio("Failure to process time of day configuration"))

            # update msdd status properties
            self.update_msdd_status()

            # create child devices for each RCV device
            for rx_chan in self.MSDD.rx_channels:
                if rx_chan.is_analog() or rx_chan.is_analog_and_digital():
                    # For each receiver add a MSDD radio device
                    eparams['PROFILE_NAME']='MSDD.spd.xml'
                    msdd_rcvr = self.addChild("CompositeMSDD", "MSDD", exec_params=eparams)
                    msdd_label = msdd_rcvr._get_label()
                    self.debug_msg("Added MSDD {0}",msdd_label)
                    msdd_rcvr.setSignalFlow(rx_chan.msdd_channel_id())

                    # REDHAWK properties do not allow for structs of structs/sequences.
                    # We add receiver_identifier property that matches msdd receiver id value (e.g. RCV:1) to filter against
                    # for output configuration properties
                    tuner_output_config, block_output_config, block_psd_output = self.get_output_configuration(rx_chan.msdd_channel_id())
                    
                    # assign output configuration and required configuration props to child
                    msdd_rcvr.advanced = self.advanced
                    msdd_rcvr.psd_configuration = self.psd_configuration
                    msdd_rcvr.gain_configuration = self.gain_configuration
                    msdd_rcvr.msdd.clock_ref = self.msdd.clock_ref
                    msdd_rcvr.time_of_day = self.time_of_day
                    msdd_rcvr.tuner_output = tuner_output_config
                    msdd_rcvr.block_tuner_output = block_output_config
                    msdd_rcvr.block_psd_output = block_psd_output
                    self.debug_msg("MSDD {0}, tuner output configuration {0} ", msdd_label, pprint.pformat( [ str(x) for x in tuner_output_config]), no_id=True)
                    self.debug_msg("MSDD {0}, block output configuration {1} ",msdd_label, pprint.pformat( [ str(x) for x in block_output_config] ), no_id=True )
                    self.debug_msg("MSDD {0}, block psd configuration {1} ", msdd_label, pprint.pformat( [ str(x) for x in block_psd_output ] ), no_id=True)
                    try:
                        # finish device setup
                        msdd_rcvr.postConstructor()
                    except:
                        traceback.print_exc()                           

        else:
            # connection failure raise initialization error
            raise CF.LifeCycle.InitializeError( self.format_msg_with_radio("Failure to establish connection."))
                                     
        return

    def updateUsageState(self):
        """
        Determine if device is busy. If all child devices are busy then we are busy.
        """
        state = CF.Device.IDLE
        not_busy=len(self._dynamicComponents)
        for obj in self._dynamicComponents:
            if obj._usageState == CF.Device.ACTIVE:
                state = obj._usageState
            if obj._usageState == CF.Device.BUSY:
                not_busy-=1

        if not_busy == 0: 
            state=CF.DeviceBusy

        return state

    def get_output_configuration(self, receiver_id ):

        # filter out tuner output configuration information for child receiver
        cfgs=list([x for x in self.tuner_output if (x.receiver_identifier == receiver_id)])

        tuner_output_config=[]
        if len(cfgs) > 0:
            for x in cfgs:
                c=_MSDD.MSDD_base.tuner_output_definition_struct()
                for m in c.getMembers():
                    try:
                        setattr(c, m[0], getattr(x,m[0]))
                    except:
                        traceback.print_exc()
                tuner_output_config.append(c)

        cfgs=list([x for x in self.block_tuner_output if (x.receiver_identifier == receiver_id)])
                                     
        # filter out block tuner output configuration information for child receiver
        block_output_config=[]
        if len(cfgs) > 0:
            for x in cfgs:
                c=_MSDD.MSDD_base.block_tuner_output_definition_struct()
                for m in c.getMembers():
                    try:
                        setattr(c, m[0], getattr(x,m[0]))
                    except:
                        traceback.print_exc()
                block_output_config.append(c)

        cfgs=list([x for x in self.block_psd_output if (x.receiver_identifier == receiver_id)])

        # filter out block psd  output configuration information for child receiver
        block_psd_config=[]
        if len(cfgs) > 0:
            for x in cfgs:
                c=_MSDD.MSDD_base.block_psd_output_definition_struct()
                for m in c.getMembers():
                    try:
                        setattr(c, m[0], getattr(x,m[0]))
                    except:
                        traceback.print_exc()
                block_psd_config.append(c)


        return tuner_output_config, block_output_config, block_psd_config
                                     

    def disconnect_from_msdd(self):
        if self.MSDD != None:
            self.msdd_status.connected=False
        self.MSDD=None

    def connect_to_msdd(self):
        self.msdd_console=None
        try:
            self.info_msg("Establishing connection.")

            self.MSDD = MSDDRadio(self.msdd.ip_address,
                                  self.msdd.port,
                                  self.msdd.timeout,
                                  enable_inline_swddc=self.advanced.enable_inline_swddc,
                                  enable_fft_channels=self.advanced.enable_fft_channels)


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
        except:
            if logging.NOTSET < self._log.level < logging.INFO:
                traceback.print_exc()
            self.exception_msg("Unable to connect to MSDD.")
            self.disconnect_from_msdd()
            return False

        return True    


    def identifyModel(self):
        return "{0} ({1})".format(self.device_address,self.device_model)

    def msddReceiverInfo(self):
        return "{0}:{1}/Controller".format(self.msdd.ip_address, self.msdd.port )

    def process_clock_ref(self):
        for board_num in range(0,len(self.MSDD.board_modules)):
            self.MSDD.board_modules[board_num].object.setExternalRef(self.msdd.clock_ref)

    def enable_time_checks(self, time_was_set=False):
        self._enableTimeChecks=time_was_set

    def host_delta_cb(self, new_delta):
        self.debug_msg('host_delta_cb delta {0}',new_delta)
        self.msdd_status.tod_host_delta = new_delta

    def ntp_status_cb(self, is_running):
        self.msdd_status.tod_ntp_running = is_running

    def determine_output_protocol(self, default_proto='sdds'):
        """
        Determine time configuration method based on output protocol.
        """
        proto=default_proto
        for cfg in self.block_tuner_output:
            if 'vita' in str(cfg.protocol).lower():
                proto='vita49'
        for cfg in self.tuner_output:
            if 'vita' in str(cfg.protocol).lower():
                proto='vita49'
        return proto
        
    def update_msdd_status(self):
        if self.MSDD == None:
            self.msdd_status=MSDD_Controller_base.MsddStatus()
            return True
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
                self.msdd_status.tod_bit_state = tod.getBITStr(self.check_tod_bit(force=True))
                self.msdd_status.tod_toy = str(tod.toy)

            self.trace_msg("Completed update msdd_status")
        except:
            if logging.NOTSET < self._log.level < logging.INFO:
                traceback.print_exc()
            self.error_msg("Unable to update msdd_status property")
            return False
    
        return True
        
    def process(self):
        if self._enableTimeChecks and self.MSDD:
            try:
                if self._time_of_day:
                    self._time_of_day.monitor_time_variances()
            except Exception as e:
                self.warn_msg('Exception occurred when performing time variance monitoring:\n{0}\n',
                              e)
                
        if time.time() > self.update_msdd_status_mark+self.interval_update_msdd_status:
            self.update_msdd_status()
            self.update_msdd_status_mark = time.time()

        self.updateUsageState()
        
        return NOOP   
  
if __name__ == '__main__':
    logging.getLogger().setLevel(logging.INFO)
    logging.debug("Starting Device")
    start_device(MSDD_Controller_i)

