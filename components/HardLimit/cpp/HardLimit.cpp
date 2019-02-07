/*
 * This file is protected by Copyright. Please refer to the COPYRIGHT file distributed with this 
 * source distribution.
 * 
 * This file is part of REDHAWK Basic Components HardLimit.
 * 
 * REDHAWK Basic Components HardLimit is free software: you can redistribute it and/or modify it under the terms of 
 * the GNU Lesser General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * REDHAWK Basic Components HardLimit is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
 * PURPOSE.  See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this 
 * program.  If not, see http://www.gnu.org/licenses/.
 */

/**************************************************************************

    This is the component code. This file contains the child class where
    custom functionality can be added to the component. Custom
    functionality to the base class can be extended here. Access to
    the ports can also be done from this class

**************************************************************************/

#include "HardLimit.h"

PREPARE_LOGGING(HardLimit_i)

HardLimit_i::HardLimit_i(const char *uuid, const char *label) :
    HardLimit_base(uuid, label)
{
}

HardLimit_i::~HardLimit_i()
{
}

int HardLimit_i::serviceFunction()
{
    LOG_DEBUG(HardLimit_i, "serviceFunction() example log message");
    
    // Get data from input port.  -1 means non-blocking, return immediately if no data
    bulkio::InFloatPort::dataTransfer *tmp = dataFloat_in->getPacket(-1);

    // if no data is available then return NOOP which will sleep briefly and then call process() again
    if (not tmp) { // No data is available
    	return NOOP;
    }
	if (tmp->inputQueueFlushed)
		LOG_WARN(HardLimit_i, "input queue flushed - data has been thrown on the floor.");

    // Implement the hard limit logic
    for (unsigned int i=0; i<tmp->dataBuffer.size(); i++) {
        if (tmp->dataBuffer[i] > upper_limit)
        	tmp->dataBuffer[i] = upper_limit;

        else if (tmp->dataBuffer[i] < lower_limit)
        	tmp->dataBuffer[i] = lower_limit;
    }

     //Output sri equals the input sri for this component.  If anything is updated, then push that update out.
    if (tmp->sriChanged || (dataFloat_out->getCurrentSRI().count(tmp->streamID)==0)) {
    	dataFloat_out->pushSRI(tmp->SRI);
    }

     // Push the modified data out along with the T, EOS, and streamID we received in the input
     dataFloat_out->pushPacket(tmp->dataBuffer,tmp->T,tmp->EOS,tmp->streamID);

    // Since we did work then return NORMAL and thus call process() immediately
     delete tmp;
     return NORMAL;
}
