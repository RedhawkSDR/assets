#ifndef RDC_I_IMPL_H
#define RDC_I_IMPL_H

#include "RDC_base.h"

#include "RfSimulators/RfSimulatorFactory.h"
#include "RfSimulators/RfSimulator.h"
#include "RfSimulators/Exceptions.h"
#include "MyCallBackClass.h"

#define DEFAULT_STREAM_ID "MyStreamID"
#define MAX_SAMPLE_RATE 2280000.0
#define MIN_SAMPLE_RATE (MAX_SAMPLE_RATE / 1000.0)

// From the FM Bandwidth
#define MIN_FREQ_RANGE 88000000
#define MAX_FREQ_RANGE 108000000

// Totally arbitrary gain limitations.
#define MIN_GAIN_RANGE -100
#define MAX_GAIN_RANGE 100

namespace RDC_ns {
class RDC_i : public RDC_base
{
    ENABLE_LOGGING
    public:
        RDC_i(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl);
        RDC_i(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl, char *compDev);
        RDC_i(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl, CF::Properties capacities);
        RDC_i(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl, CF::Properties capacities, char *compDev);
        ~RDC_i();

        void constructor();

        CF::Device::Allocations* allocate (const CF::Properties& capacities);

        int serviceFunction();

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
        void configureTuner(const std::string& allocation_id, const CF::Properties& tunerSettings);
        CF::Properties* getTunerSettings(const std::string& allocation_id);
        std::string get_rf_flow_id(const std::string& port_name);
        void set_rf_flow_id(const std::string& port_name, const std::string& id);
        frontend::RFInfoPkt get_rfinfo_pkt(const std::string& port_name);
        void set_rfinfo_pkt(const std::string& port_name, const frontend::RFInfoPkt& pkt);

        void initDigitizer();

    private:
        ////////////////////////////////////////
        // Required device specific functions // -- to be implemented by device developer
        ////////////////////////////////////////

        RfSimulators::RfSimulator* digiSim;
        MyCallBackClass *cb;
        void addAWGNChanged(const bool* old_value, const bool* new_value);
        void noiseSigmaChanged(const float* old_value, const float* new_value);

        // these are pure virtual, must be implemented here
        void deviceEnable(frontend_tuner_status_struct_struct &fts, size_t tuner_id);
        void deviceDisable(frontend_tuner_status_struct_struct &fts, size_t tuner_id);
        bool deviceSetTuning(const frontend::frontend_tuner_allocation_struct &request, frontend_tuner_status_struct_struct &fts, size_t tuner_id);
        bool deviceDeleteTuning(frontend_tuner_status_struct_struct &fts, size_t tuner_id);
        std::vector<unsigned int> availableSampleRates;
        // keep track of RFInfoPkt from RFInfo_in port
        frontend::RFInfoPkt rfinfo_pkt;

};
};

#endif // RDC_I_IMPL_H
