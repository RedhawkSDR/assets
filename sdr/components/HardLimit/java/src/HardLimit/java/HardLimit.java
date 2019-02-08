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
package HardLimit.java;

import java.util.Properties;

import BULKIO.StreamSRI;
import bulkio.InFloatPort;

/**
 * This is the component code. This file contains the derived class where custom
 * functionality can be added to the component. You may add methods and code to
 * this class to handle property changes, respond to incoming data, and perform
 * general component housekeeping
 *
 * Source: HardLimit.spd.xml
 */
public class HardLimit extends HardLimit_base {
    /**
     * This is the component constructor. In this method, you may add additional
     * functionality to properties, such as listening for changes and handling
     * allocation, and set up internal state for your component.
     *
     * A component may listen for external changes to properties (i.e., by a
     * call to configure) using the PropertyListener interface. Listeners are
     * registered by calling addPropertyListener() on the property instance
     * with an object that implements the PropertyListener interface for that
     * data type (e.g., "PropertyListener<Float>" for a float property). More
     * than one listener can be connected to a property.
     *
     *   Example:
     *       // This example makes use of the following properties:
     *       //  - A float value called scaleValue
     *       // The file must import "org.ossie.properties.PropertyListener"
     *
     *       this.scaleValue.addPropertyListener(new PropertyListener<Float>() {
     *           public void valueChanged(Float oldValue, Float newValue) {
     *               logger.debug("Changed scaleValue " + oldValue + " to " + newValue);
     *           }
     *       });
     *
     * The recommended practice is for the implementation of valueChanged() to
     * contain only glue code to dispatch the call to a private method on the
     * component class.
     */
    public HardLimit() {
        super();
    }

    public boolean hasSri(String streamID)
	{
        StreamSRI[] sriArray = port_dataFloat_out.activeSRIs();
        for (StreamSRI sri : sriArray) {
          if (streamID.equals(sri.streamID)) {
        	  return true; // found match
          }
        }
        return false; // not found	
	}
    
    protected int serviceFunction() {
    	InFloatPort.Packet data = this.port_dataFloat_in.getPacket(-1);

        if (data !=null) {
        	if (data.inputQueueFlushed)
        		logger.warn("input queue flushed - data has been thrown on the floor.");
        	if (data.sriChanged() || (!this.hasSri(data.getStreamID()))) {
                this.port_dataFloat_out.pushSRI(data.getSRI());
            }	
        	
        	for (int i =0; i<data.getData().length; i++) {
        		if (data.getData()[i] > this.upper_limit.getValue()) {
        			data.getData()[i] = this.upper_limit.getValue();
        		}
        		else if (data.getData()[i] < this.lower_limit.getValue()) {
        			data.getData()[i] = this.lower_limit.getValue();
        		}
        	}
        	this.port_dataFloat_out.pushPacket(data.getData(), data.getTime(), data.getEndOfStream(), data.getStreamID());
        return NORMAL;
        }
        else
        	return NOOP;
    }

    /**
     * Set additional options for ORB startup. For example:
     *
     *   orbProps.put("com.sun.CORBA.giop.ORBFragmentSize", Integer.toString(fragSize));
     *
     * @param orbProps
     */
    public static void configureOrb(final Properties orbProps) {
    }
}
