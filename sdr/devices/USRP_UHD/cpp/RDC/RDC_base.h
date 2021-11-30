/*
 * This file is protected by Copyright. Please refer to the COPYRIGHT file
 * distributed with this source distribution.
 *
 * This file is part of REDHAWK USRP_UHD.
 *
 * REDHAWK USRP_UHD is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * REDHAWK USRP_UHD is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
#ifndef RDC_BASE_IMPL_BASE_H
#define RDC_BASE_IMPL_BASE_H

#include <boost/thread.hpp>
#include <frontend/frontend.h>
#include <ossie/ThreadedComponent.h>
#include <ossie/DynamicComponent.h>

#include <frontend/frontend.h>
#include "RDC_port_impl.h"
#include <bulkio/bulkio.h>
#include "RDC_struct_props.h"

#define BOOL_VALUE_HERE 0

namespace RDC_ns {
class RDC_base : public frontend::FrontendTunerDevice<frontend_tuner_status_struct_struct>, public virtual frontend::digital_tuner_delegation, public virtual frontend::rfinfo_delegation, protected ThreadedComponent, public virtual DynamicComponent
{
    friend class CF_DeviceStatus_Out_i;

    public:
        RDC_base(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl);
        RDC_base(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl, char *compDev);
        RDC_base(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl, CF::Properties capacities);
        RDC_base(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl, CF::Properties capacities, char *compDev);
        ~RDC_base();

        void start() throw (CF::Resource::StartError, CORBA::SystemException);

        void stop() throw (CF::Resource::StopError, CORBA::SystemException);

        void releaseObject() throw (CF::LifeCycle::ReleaseError, CORBA::SystemException);

        void loadProperties();
        void removeAllocationIdRouting(const size_t tuner_id);

        virtual CF::Properties* getTunerStatus(const std::string& allocation_id);
        virtual void assignListener(const std::string& listen_alloc_id, const std::string& allocation_id);
        virtual void removeListener(const std::string& listen_alloc_id);
        void frontendTunerStatusChanged(const std::vector<frontend_tuner_status_struct_struct>* oldValue, const std::vector<frontend_tuner_status_struct_struct>* newValue);

    protected:
        // Member variables exposed as properties
        /// Property: rx_autogain_on_tune
        bool rx_autogain_on_tune;
        /// Property: trigger_rx_autogain
        bool trigger_rx_autogain;
        /// Property: rx_autogain_guard_bits
        unsigned short rx_autogain_guard_bits;
        /// Property: device_gain
        float device_gain;
        /// Property: device_mode
        std::string device_mode;
        /// Property: device_characteristics
        device_characteristics_struct device_characteristics;

        // Ports
        /// Port: RFInfo_in
        frontend::InRFInfoPort *RFInfo_in;
        /// Port: DigitalTuner_in
        frontend::InDigitalTunerPort *DigitalTuner_in;
        /// Port: DeviceStatus_out
        CF_DeviceStatus_Out_i *DeviceStatus_out;
        /// Port: dataShort_out
        bulkio::OutShortPort *dataShort_out;
        /// Port: dataSDDS_out
        bulkio::OutSDDSPort *dataSDDS_out;

        std::map<std::string, std::string> listeners;

    private:
        void construct();
};
};
#endif // RDC_BASE_IMPL_BASE_H
