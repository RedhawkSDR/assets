#!/usr/bin/env python3
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

import logging
import copy
import math
import sys
import inspect 
import traceback
from time import sleep
from datetime import datetime, time, timedelta
from pprint import pprint as pp

from omniORB import CORBA, any
from ossie.device import start_device
from frontend.fe_types import *
from ossie import properties

from .MSDD import * 

class CompositeMSDD_i(MSDD_i):
    """
    CompositeMSDD is a MSDD_i receiver instance used in parent/child deployment (same process space). 
    The MSDD_Controller parent will instantiate one of these devices for each receiver path (RCV:x) through the radio.  
    This parent/child deployment pattern will work for both single and dual channel MSDD receiver types.  
    The MSDD_Controller parent is responsible establishing the connection to the MSDD radio and managing the radio's assets 
    that are not directly associcated with a RCV:x path (i.e. tod, brd, net modules).  The CompositeMSDD_i will respond to 
    tuner allocation requests, gain, psd configuration settings. The MSDD_Controller is responsible for configuring
    the CompositeMSDD at startup with the set of properties defined in the node's DCD xml file.  The properties
    as filtered by the receiver_identifier string associated with each signal path (i.e. RCV:0 or RCV:1).
    
    When MSDD_Controller performs an abreviated lifecycle deployment process for the devices it manages. After the 
    CompositeMSDD_i is launched in its own thread, the MSDD_Controller will call the Resource::initialize method
    that will call this class' constructor method. Since MSDD_Controller and CompositeMSDD reside in the same
    process space, there is no 'configure' step.  Any property settings managed by the MSDD_Controller during 
    startup (via DCD file) are directly assigned to the CompositeMSDD instance after the initialize method 
    using the CompositeMSDD's assigned receiver_identifier.

    MSDD_Controller::constructor -> 
             launch CompositeMSDD in thread
             filter tuner_output, block_output, psd_output configs using receiver id (i.e RCV:0)
             assign output configs to newly launched ComponsiteMSDD

    Frontend allocation and tuner control methods handle via MSDD.py 
    MSDD::_allocate_frontend_tuner_allocation - handle tuner allocation requests
    MSDD::deviceEnable  - enable tuner output during allocation
    MSDD::deviceDisable  - disable tuner output durin deallocation
    MSDD::deviceDeleteTuning - finish deallocation process
    """


    def setParentInstance(self, parent):
        """
        Set the managing parent assigned to this object. This is part of the deployment process and is called
        after the device has been launched.
        """
        self._parentInstance = parent
       
    def setMDDRadio(self, msddRadio ):
        """
        Set MSDDRadio object that this object can use to interface withe MSDD radio. The MSDD_Controller establishes and
        maintains the connection to the MSDD radio using the MSDDRadio object.  
        """
        self.MSDD=msddRadio

    def setSignalFlow(self, flow_identifier ):
        """
        Set receiver_identifier to use when filtering rx_channel objects on the MSDD radio. 

        This method will be called after initialization and during the property configuration phase of the deployment.
        This information is required for the CompositeMSDD to filter out the MSDDRadio's rx_channels
        that have receiver identifier that matches the value of the flow_identifier parameter.  Managing
        all the channels with the same receiver identifier will allow for allocations to be peformed
        based on rf_flow_id.
        """
        self.receiver_identifier=flow_identifier

    def constructor(self):
        """
        Override MSDD's constructor to just initialize variables. Construction is a two step process and
        we need our properties initialized first before we can proceed.  the postContructor method is called
        by MSDD_Controller after launch and property initialization.
        """
        self.MSDD = None                     # provided by MSDD_Controller
        self.msdd_console = None             # resolved when self.MSDD is assigned
        self.device_rf_flow = ""             # flow id assigned to this device
        self.device_rf_info_pkt=None         # rf_info_packet assigned to the device
        self.dig_tuner_base_nums= []         # stores all channels that have digitial output   
        self.tuner_output_configuration={}   # tuner output configuration derived from tuner_output property
        self.psd_output_configuration={}     # psd output configuration derived from block_psd_output property
        self.msdd_status=MSDD_base.msdd_status_struct()   # MSDD radio state/status 
        self.frontend_tuner_status = []      # frontend tuner status structure for allocations
        self.tuner_allocation_ids = []       # allocation ids for all tuner allocation requests
        self.rx_channel_tuner_status={}      # reverse lookup of MSDD rx_channel to frontend_tuner_status object
        self.receiver_identifier=None        # assigned receiver identifier RCV:1, etc..

    def postConstructor(self):
        """
        Performing remaining startup sequence of MSDD_i::constructor that was not initially performed
        in our constructor method. This method requires self.receiver_identifier is configured so the
        correct rx_channel objects can be filtered an assigned to our frontend tuner_status structure.
        """

        # used assigned MSDDRadio instance and update msdd_status properties 
        self.connect_to_msdd()

        # set our assigned receiver identifer, we filter channels based on the source receiver id
        self.msdd_status.receiver_identifier=self.receiver_identifier

        # initilize frontend_tuner_status objects for our rx_channels
        self.update_tuner_status()
        
        self.debug_msg("Create output configuration for assigned tuners and fft channels ")
        self.update_output_configuration()

        self.debug_msg("Update tuner status with output configuration")
        self.update_tuner_status()

        self.debug_msg("Disable all tuner hardware output ")
        for tuner_num in range(0,len(self.frontend_tuner_status)):
            try:
                self.frontend_tuner_status[tuner_num].rx_object.setEnable(False)
            except:
                self.warn_msg("Disabling tuner {0} failed. ",tuner_num)

        # property changes will be handled by base classes

    def disconnect_from_msdd(self):
        """
        The MSDD_Controller manages the MSDDRadio object that we are given.  The actual disconnect will occur
        by the MSDD_controller.
        """
        if self.MSDD != None:

            self.info_msg('Performing tuner disconnect')

            # disable all tuner status and purge and listeners
            for t in range(0,len(self.frontend_tuner_status)):
                try:
                    if self.frontend_tuner_status[t].enabled:
                        self.frontend_tuner_status[t].enabled = False
                        self.purgeListeners(t)
                except IndexError:
                    self.warn_msg('Disconnect failed, missing tuner {0}',t)
                    break

    def connect_to_msdd(self):
        if self.MSDD == None:
            self.msdd_console=None
            try:
                # grab parent's MSDD reference
                self.MSDD = self._parentInstance.MSDD
                if self.MSDD:
                    self._baseLog.info("Connected to MSDD {0}".format(self.MSDD.radioAddress))
                    self.msdd.ip_address = self.MSDD.radioAddress[0]
                    self.msdd.port = self.MSDD.radioAddress[1]
                    self.msdd_console = self.MSDD.console
                    self.update_msdd_status()
            except:
                if logging.NOTSET < self._baseLog.level < logging.INFO:
                    traceback.print_exc()
                self.exception_msg("Unable to connect to radio")
                return False

        self.info_msg('Completed connection to radio')
        return True    

    def process(self):
        # parent handles timing issues
        return FINISH

if __name__ == '__main__':
    logging.info("Starting CompositeMSDD_i Device")
    start_device(CompositeMSDD_i)

