/*
 * This file is protected by Copyright. Please refer to the COPYRIGHT file
 * distributed with this source distribution.
 *
 * This file is part of REDHAWK.
 *
 * REDHAWK is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * REDHAWK is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

#include "sinksocket_base.h"

/*******************************************************************************************

    AUTO-GENERATED CODE. DO NOT MODIFY

    The following class functions are for the base class for the component class. To
    customize any of these functions, do not modify them here. Instead, overload them
    on the child class

******************************************************************************************/

sinksocket_base::sinksocket_base(const char *uuid, const char *label) :
    Component(uuid, label),
    ThreadedComponent()
{
    setThreadName(label);

    loadProperties();

    dataOctet_in = new bulkio::InOctetPort("dataOctet_in");
    dataOctet_in->setLogger(this->_baseLog->getChildLogger("dataOctet_in", "ports"));
    addPort("dataOctet_in", "Octet port for input data.", dataOctet_in);
    dataChar_in = new bulkio::InCharPort("dataChar_in");
    dataChar_in->setLogger(this->_baseLog->getChildLogger("dataChar_in", "ports"));
    addPort("dataChar_in", "Char port for input data.", dataChar_in);
    dataShort_in = new bulkio::InShortPort("dataShort_in");
    dataShort_in->setLogger(this->_baseLog->getChildLogger("dataShort_in", "ports"));
    addPort("dataShort_in", "Short port for input data.", dataShort_in);
    dataUshort_in = new bulkio::InUShortPort("dataUshort_in");
    dataUshort_in->setLogger(this->_baseLog->getChildLogger("dataUshort_in", "ports"));
    addPort("dataUshort_in", "Unsigned short port for input data.", dataUshort_in);
    dataLong_in = new bulkio::InLongPort("dataLong_in");
    dataLong_in->setLogger(this->_baseLog->getChildLogger("dataLong_in", "ports"));
    addPort("dataLong_in", "Long port for input data.", dataLong_in);
    dataUlong_in = new bulkio::InULongPort("dataUlong_in");
    dataUlong_in->setLogger(this->_baseLog->getChildLogger("dataUlong_in", "ports"));
    addPort("dataUlong_in", "Unsigned long port for input data.", dataUlong_in);
    dataFloat_in = new bulkio::InFloatPort("dataFloat_in");
    dataFloat_in->setLogger(this->_baseLog->getChildLogger("dataFloat_in", "ports"));
    addPort("dataFloat_in", "Float port for input data.", dataFloat_in);
    dataDouble_in = new bulkio::InDoublePort("dataDouble_in");
    dataDouble_in->setLogger(this->_baseLog->getChildLogger("dataDouble_in", "ports"));
    addPort("dataDouble_in", "Double port for input data.", dataDouble_in);
}

sinksocket_base::~sinksocket_base()
{
    dataOctet_in->_remove_ref();
    dataOctet_in = 0;
    dataChar_in->_remove_ref();
    dataChar_in = 0;
    dataShort_in->_remove_ref();
    dataShort_in = 0;
    dataUshort_in->_remove_ref();
    dataUshort_in = 0;
    dataLong_in->_remove_ref();
    dataLong_in = 0;
    dataUlong_in->_remove_ref();
    dataUlong_in = 0;
    dataFloat_in->_remove_ref();
    dataFloat_in = 0;
    dataDouble_in->_remove_ref();
    dataDouble_in = 0;
}

/*******************************************************************************************
    Framework-level functions
    These functions are generally called by the framework to perform housekeeping.
*******************************************************************************************/
void sinksocket_base::start()
{
    Component::start();
    ThreadedComponent::startThread();
}

void sinksocket_base::stop()
{
    Component::stop();
    if (!ThreadedComponent::stopThread()) {
        throw CF::Resource::StopError(CF::CF_NOTSET, "Processing thread did not die");
    }
}

void sinksocket_base::releaseObject()
{
    // This function clears the component running condition so main shuts down everything
    try {
        stop();
    } catch (CF::Resource::StopError& ex) {
        // TODO - this should probably be logged instead of ignored
    }

    Component::releaseObject();
}

void sinksocket_base::loadProperties()
{
    addProperty(total_bytes,
                0,
                "total_bytes",
                "",
                "readonly",
                "",
                "external",
                "property");

    addProperty(bytes_per_sec,
                0,
                "bytes_per_sec",
                "",
                "readonly",
                "BpsS",
                "external",
                "property");

    addProperty(Connections,
                "Connections",
                "",
                "readwrite",
                "",
                "external",
                "property");

    addProperty(ConnectionStats,
                "ConnectionStats",
                "",
                "readonly",
                "",
                "external",
                "property");

}



