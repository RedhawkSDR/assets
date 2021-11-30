/*
 * This file is protected by Copyright. Please refer to the COPYRIGHT file
 * distributed with this source distribution.
 *
 * This file is part of REDHAWK FmRdsSimulator.
 *
 * REDHAWK FmRdsSimulator is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * REDHAWK FmRdsSimulator is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
#ifndef USRP_UHD_I_IMPL_H
#define USRP_UHD_I_IMPL_H

#include "USRP_UHD_base.h"
#include <uhd/usrp/multi_usrp.hpp>

#include "RDC/RDC.h"
#include "TDC/TDC.h"

/*#include <uhd/types/ranges.hpp>
#include <boost/algorithm/string.hpp> //for split
#include <uhd/usrp/device_props.hpp>
#include <uhd/usrp/mboard_props.hpp>
#include <uhd/usrp/dboard_props.hpp>
#include <uhd/usrp/codec_props.hpp>
#include <uhd/usrp/dsp_props.hpp>
#include <uhd/usrp/subdev_props.hpp>
#include <uhd/usrp/dboard_id.hpp>
#include <uhd/usrp/mboard_eeprom.hpp>
#include <uhd/usrp/dboard_eeprom.hpp>*/

class USRP_UHD_i : public USRP_UHD_base
{
    ENABLE_LOGGING
    public:
        USRP_UHD_i(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl);
        USRP_UHD_i(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl, char *compDev);
        USRP_UHD_i(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl, CF::Properties capacities);
        USRP_UHD_i(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl, CF::Properties capacities, char *compDev);
        ~USRP_UHD_i();

        void constructor();

        int serviceFunction();
        void frontendTunerStatusChanged(const std::vector<frontend_tuner_status_struct_struct>* oldValue, const std::vector<frontend_tuner_status_struct_struct>* newValue);

    protected:
        std::string getTunerType(const std::string& allocation_id);
        bool getTunerDeviceControl(const std::string& allocation_id);
        std::string getTunerGroupId(const std::string& allocation_id);
        std::string getTunerRfFlowId(const std::string& allocation_id);
        double getTunerCenterFrequency(const std::string& allocation_id);
        void setTunerCenterFrequency(const std::string& allocation_id, double freq);
        double getTunerBandwidth(const std::string& allocation_id);
        void setTunerBandwidth(const std::string& allocation_id, double bw);
        bool getTunerAgcEnable(const std::string& allocation_id);
        void setTunerAgcEnable(const std::string& allocation_id, bool enable);
        float getTunerGain(const std::string& allocation_id);
        void setTunerGain(const std::string& allocation_id, float gain);
        long getTunerReferenceSource(const std::string& allocation_id);
        void setTunerReferenceSource(const std::string& allocation_id, long source);
        bool getTunerEnable(const std::string& allocation_id);
        void setTunerEnable(const std::string& allocation_id, bool enable);
        double getTunerOutputSampleRate(const std::string& allocation_id);
        void setTunerOutputSampleRate(const std::string& allocation_id, double sr);
        void configureTuner(const std::string& id, const CF::Properties& tunerSettings);
        CF::Properties* getTunerSettings(const std::string& id);
        frontend::ScanStatus getScanStatus(const std::string& allocation_id);
        void setScanStartTime(const std::string& allocation_id, const BULKIO::PrecisionUTCTime& start_time);
        void setScanStrategy(const std::string& allocation_id, const frontend::ScanStrategy* scan_strategy);
        std::string get_rf_flow_id(const std::string& port_name);
        void set_rf_flow_id(const std::string& port_name, const std::string& id);
        frontend::RFInfoPkt get_rfinfo_pkt(const std::string& port_name);
        void set_rfinfo_pkt(const std::string& port_name, const frontend::RFInfoPkt& pkt);


        std::vector<RDC_ns::RDC_i*> RDCs;
        std::vector<TDC_ns::TDC_i*> TDCs;
        std::map<std::string, CF::Device::Allocations_var> _delegatedAllocations;
        uhd::usrp::multi_usrp::sptr usrp_device_ptr;
        // Try to synchronize the USRP_UHD time to its clock source
        bool _synchronizeClock(const std::string source);

        void deviceReferenceSourceChanged(std::string old_value, std::string new_value);
        void updateDeviceReferenceSource(std::string source);
        CF::Device::Allocations* allocate (const CF::Properties& capacities)
            throw (CF::Device::InvalidState, CF::Device::InvalidCapacity, 
                   CF::Device::InsufficientCapacity, CORBA::SystemException);
        void deallocate (const char* alloc_id) 
            throw (CF::Device::InvalidState, CF::Device::InvalidCapacity, 
                   CORBA::SystemException);
        std::vector<frontend_tuner_status_struct_struct> get_fts();

    private:
        ////////////////////////////////////////
        // Required device specific functions // -- to be implemented by device developer
        ////////////////////////////////////////

        // these are pure virtual, must be implemented here
        void deviceEnable(frontend_tuner_status_struct_struct &fts, size_t tuner_id);
        void deviceDisable(frontend_tuner_status_struct_struct &fts, size_t tuner_id);
        bool deviceSetTuningScan(const frontend::frontend_tuner_allocation_struct &request, const frontend::frontend_scanner_allocation_struct &scan_request, frontend_tuner_status_struct_struct &fts, size_t tuner_id);
        bool deviceSetTuning(const frontend::frontend_tuner_allocation_struct &request, frontend_tuner_status_struct_struct &fts, size_t tuner_id);
        bool deviceDeleteTuning(frontend_tuner_status_struct_struct &fts, size_t tuner_id);

};

#endif // USRP_UHD_I_IMPL_H
