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

        /**
         * @throw CF::Resource::StartError
         * @throw CORBA::SystemException
         */
        void start();

        /**
         * @throw CF::Resource::StopError
         * @throw CORBA::SystemException
         */
        void stop();

        /**
         * @throw CF::LifeCycle::ReleaseError
         * @throw CORBA::SystemException
         */
        void releaseObject();

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
        /// Property: rdc_only_property
        float rdc_only_property;
        /// Property: device_mode
        std::string device_mode;
        /// Property: PathToConfiguration
        std::string PathToConfiguration;
        /// Property: noiseSigma
        float noiseSigma;
        /// Property: addAWGN
        bool addAWGN;
        /// Property: device_characteristics
        device_characteristics_struct device_characteristics;

        // Ports
        /// Port: RFInfo_in
        frontend::InRFInfoPort *RFInfo_in;
        /// Port: DigitalTuner_in
        frontend::InDigitalTunerPort *DigitalTuner_in;
        /// Port: DeviceStatus_out
        CF_DeviceStatus_Out_i *DeviceStatus_out;
        /// Port: dataFloat_out
        bulkio::OutFloatPort *dataFloat_out;

        std::map<std::string, std::string> listeners;

    private:
        void construct();
};
};
#endif // RDC_BASE_IMPL_BASE_H
