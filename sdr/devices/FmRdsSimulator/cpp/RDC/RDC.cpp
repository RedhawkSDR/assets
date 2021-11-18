/**************************************************************************

    This is the device code. This file contains the child class where
    custom functionality can be added to the device. Custom
    functionality to the base class can be extended here. Access to
    the ports can also be done from this class

**************************************************************************/

#include "RDC.h"

using namespace RDC_ns;

PREPARE_LOGGING(RDC_i)

RDC_i::RDC_i(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl) :
    RDC_base(devMgr_ior, id, lbl, sftwrPrfl)
{
}

RDC_i::RDC_i(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl, char *compDev) :
    RDC_base(devMgr_ior, id, lbl, sftwrPrfl, compDev)
{
}

RDC_i::RDC_i(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl, CF::Properties capacities) :
    RDC_base(devMgr_ior, id, lbl, sftwrPrfl, capacities)
{
}

RDC_i::RDC_i(char *devMgr_ior, char *id, char *lbl, char *sftwrPrfl, CF::Properties capacities, char *compDev) :
    RDC_base(devMgr_ior, id, lbl, sftwrPrfl, capacities, compDev)
{
}

RDC_i::~RDC_i()
{
}

void RDC_i::constructor()
{
    /***********************************************************************************
     This is the RH constructor. All properties are properly initialized before this function is called 

     For a tuner device, the structure frontend_tuner_status needs to match the number
     of tuners that this device controls and what kind of device it is.
     The options for devices are: ANTENNA, RX, RX_ARRAY, DBOT, ABOT, ARDC, RDC, SRDC, DRDC, TX, TX_ARRAY, TDC
     
     An example of setting up this device as an ABOT would look like this:

     this->addChannels(1, "ABOT");
     
     The incoming request for tuning contains a string describing the requested tuner
     type. The string for the request must match the string in the tuner status.
    ***********************************************************************************/
    this->addChannels(1, "RDC");
	digiSim = NULL;
	cb = NULL;

	/** Register callbacks **/
	addPropertyChangeListener("addAWGN", this, &RDC_i::addAWGNChanged);
	addPropertyChangeListener("noiseSigma", this, &RDC_i::noiseSigmaChanged);

	// 0.5 because of cast truncation.
	unsigned int maxSampleRateInt = (unsigned int) (MAX_SAMPLE_RATE + 0.5);
	int iterator = 2;
	unsigned int tmpSampleRate = maxSampleRateInt;
	availableSampleRates.push_back(tmpSampleRate);

	while (tmpSampleRate >= MIN_SAMPLE_RATE) {
		if (maxSampleRateInt % iterator == 0) {
			tmpSampleRate = maxSampleRateInt / iterator;

			if (tmpSampleRate >= MIN_SAMPLE_RATE) {
				availableSampleRates.push_back(tmpSampleRate);
			}
		}
		++iterator;
	}

	std::sort (availableSampleRates.begin(), availableSampleRates.end());

	// Initialize rfinfo packet
	rfinfo_pkt.if_center_freq =0;
	rfinfo_pkt.rf_flow_id = "";
}

CF::Device::Allocations* RDC_i::allocate (const CF::Properties& capacities) {
    CF::Device::Allocations_var result = new CF::Device::Allocations();
    result = RDC_base::allocate(capacities);
    /*
     * Add data and control ports to response if length is greater than 0
     */
    return result._retn();
}

void RDC_i::addAWGNChanged(const bool* old_value, const bool* new_value) {
	if (digiSim) {
		digiSim->addNoise(*new_value);
	}
}

void RDC_i::noiseSigmaChanged(const float* old_value, const float* new_value) {
	if (digiSim) {
		digiSim->setNoiseSigma(*new_value);
	}
}

void RDC_i::initDigitizer() {
	RH_TRACE(this->_baseLog, "Entering Method");

	if (cb) {
		delete(cb);
		cb = NULL;
	}

	if (digiSim) {
		delete(digiSim);
		digiSim = NULL;
	}

	cb = new MyCallBackClass(dataFloat_out, &frontend_tuner_status,this->identifier() );


	digiSim = RfSimulators::RfSimulatorFactory::createFmRdsSimulator();
	digiSim->setCenterFrequencyRange(MIN_FREQ_RANGE, MAX_FREQ_RANGE);
	digiSim->setGainRange(MIN_GAIN_RANGE, MAX_GAIN_RANGE);
	digiSim->addNoise(addAWGN);
	digiSim->setNoiseSigma(noiseSigma);

	switch (this->log_level())
	{
	case 5:
		digiSim->init(PathToConfiguration, cb, RfSimulators::TRACE);
		break;
	case 4:
		digiSim->init(PathToConfiguration, cb, RfSimulators::DEBUG);
		break;
	case 3:
		digiSim->init(PathToConfiguration, cb, RfSimulators::INFO);
		break;
	case 2:
		digiSim->init(PathToConfiguration, cb, RfSimulators::WARN);
		break;
	case 1:
		digiSim->init(PathToConfiguration, cb, RfSimulators::ERROR);
		break;
	default:
		digiSim->init(PathToConfiguration, cb, RfSimulators::WARN);
		break;
	}

    // Initialize status vector
    // this device only has a single tuner

    frontend_tuner_status[0].allocation_id_csv = "";
    frontend_tuner_status[0].tuner_type = "RDC";
    frontend_tuner_status[0].center_frequency = digiSim->getCenterFrequency();
    frontend_tuner_status[0].sample_rate = digiSim->getSampleRate();
    // set bandwidth to the max sample rate.
    frontend_tuner_status[0].bandwidth = MAX_SAMPLE_RATE;
    frontend_tuner_status[0].rf_flow_id = "";
    frontend_tuner_status[0].gain = digiSim->getGain();
    frontend_tuner_status[0].group_id = "";
    frontend_tuner_status[0].enabled = false;
    frontend_tuner_status[0].stream_id.clear();

}

/***********************************************************************************************

    Basic functionality:

        The service function is called by the serviceThread object (of type ProcessThread).
        This call happens immediately after the previous call if the return value for
        the previous call was NORMAL.
        If the return value for the previous call was NOOP, then the serviceThread waits
        an amount of time defined in the serviceThread's constructor.
        
    SRI:
        To create a StreamSRI object, use the following code:
                std::string stream_id = "testStream";
                BULKIO::StreamSRI sri = bulkio::sri::create(stream_id);

        To create a StreamSRI object based on tuner status structure index 'idx' and collector center frequency of 100:
                std::string stream_id = "my_stream_id";
                BULKIO::StreamSRI sri = this->create(stream_id, this->frontend_tuner_status[idx], 100);

    Time:
        To create a PrecisionUTCTime object, use the following code:
                BULKIO::PrecisionUTCTime tstamp = bulkio::time::utils::now();

        
    Ports:

        Data is passed to the serviceFunction through by reading from input streams
        (BulkIO only). The input stream class is a port-specific class, so each port
        implementing the BulkIO interface will have its own type-specific input stream.
        UDP multicast (dataSDDS and dataVITA49) ports do not support streams.

        The input stream from which to read can be requested with the getCurrentStream()
        method. The optional argument to getCurrentStream() is a floating point number that
        specifies the time to wait in seconds. A zero value is non-blocking. A negative value
        is blocking.  Constants have been defined for these values, bulkio::Const::BLOCKING and
        bulkio::Const::NON_BLOCKING.

        More advanced uses of input streams are possible; refer to the REDHAWK documentation
        for more details.

        Input streams return data blocks that automatically manage the memory for the data
        and include the SRI that was in effect at the time the data was received. It is not
        necessary to delete the block; it will be cleaned up when it goes out of scope.

        To send data using a BulkIO interface, create an output stream and write the
        data to it. When done with the output stream, the close() method sends and end-of-
        stream flag and cleans up.

        NOTE: If you have a BULKIO dataSDDS or dataVITA49  port, you must manually call 
              "port->updateStats()" to update the port statistics when appropriate.

        Example:
            // This example assumes that the device has two ports:
            //  An input (provides) port of type bulkio::InShortPort called dataShort_in
            //  An output (uses) port of type bulkio::OutFloatPort called dataFloat_out
            // The mapping between the port and the class is found
            // in the device base class header file

            bulkio::InShortStream inputStream = dataShort_in->getCurrentStream();
            if (!inputStream) { // No streams are available
                return NOOP;
            }

            // Get the output stream, creating it if it doesn't exist yet
            bulkio::OutFloatStream outputStream = dataFloat_out->getStream(inputStream.streamID());
            if (!outputStream) {
                outputStream = dataFloat_out->createStream(inputStream.sri());
            }

            bulkio::ShortDataBlock block = inputStream.read();
            if (!block) { // No data available
                // Propagate end-of-stream
                if (inputStream.eos()) {
                   outputStream.close();
                }
                return NOOP;
            }

            if (block.sriChanged()) {
                // Update output SRI
                outputStream.sri(block.sri());
            }

            // Get read-only access to the input data
            redhawk::shared_buffer<short> inputData = block.buffer();

            // Acquire a new buffer to hold the output data
            redhawk::buffer<float> outputData(inputData.size());

            // Transform input data into output data
            for (size_t index = 0; index < inputData.size(); ++index) {
                outputData[index] = (float) inputData[index];
            }

            // Write to the output stream; outputData must not be modified after
            // this method call
            outputStream.write(outputData, block.getStartTime());

            return NORMAL;

        If working with complex data (i.e., the "mode" on the SRI is set to
        true), the data block's complex() method will return true. Data blocks
        provide a cxbuffer() method that returns a complex interpretation of the
        buffer without making a copy:

            if (block.complex()) {
                redhawk::shared_buffer<std::complex<short> > inData = block.cxbuffer();
                redhawk::buffer<std::complex<float> > outData(inData.size());
                for (size_t index = 0; index < inData.size(); ++index) {
                    outData[index] = inData[index];
                }
                outputStream.write(outData, block.getStartTime());
            }

        Interactions with non-BULKIO ports are left up to the device developer's discretion
        
    Messages:
    
        To receive a message, you need (1) an input port of type MessageEvent, (2) a message prototype described
        as a structure property of kind message, (3) a callback to service the message, and (4) to register the callback
        with the input port.
        
        Assuming a property of type message is declared called "my_msg", an input port called "msg_input" is declared of
        type MessageEvent, create the following code:
        
        void RDC_i::my_message_callback(const std::string& id, const my_msg_struct &msg){
        }
        
        Register the message callback onto the input port with the following form:
        this->msg_input->registerMessage("my_msg", this, &RDC_i::my_message_callback);
        
        To send a message, you need to (1) create a message structure, (2) a message prototype described
        as a structure property of kind message, and (3) send the message over the port.
        
        Assuming a property of type message is declared called "my_msg", an output port called "msg_output" is declared of
        type MessageEvent, create the following code:
        
        ::my_msg_struct msg_out;
        this->msg_output->sendMessage(msg_out);

    Accessing the Device Manager and Domain Manager:
    
        Both the Device Manager hosting this Device and the Domain Manager hosting
        the Device Manager are available to the Device.
        
        To access the Domain Manager:
            CF::DomainManager_ptr dommgr = this->getDomainManager()->getRef();
        To access the Device Manager:
            CF::DeviceManager_ptr devmgr = this->getDeviceManager()->getRef();
    
    Properties:
        
        Properties are accessed directly as member variables. For example, if the
        property name is "baudRate", it may be accessed within member functions as
        "baudRate". Unnamed properties are given the property id as its name.
        Property types are mapped to the nearest C++ type, (e.g. "string" becomes
        "std::string"). All generated properties are declared in the base class
        (RDC_base).
    
        Simple sequence properties are mapped to "std::vector" of the simple type.
        Struct properties, if used, are mapped to C++ structs defined in the
        generated file "struct_props.h". Field names are taken from the name in
        the properties file; if no name is given, a generated name of the form
        "field_n" is used, where "n" is the ordinal number of the field.
        
        Example:
            // This example makes use of the following Properties:
            //  - A float value called scaleValue
            //  - A boolean called scaleInput
              
            if (scaleInput) {
                dataOut[i] = dataIn[i] * scaleValue;
            } else {
                dataOut[i] = dataIn[i];
            }
            
        Callback methods can be associated with a property so that the methods are
        called each time the property value changes.  This is done by calling 
        addPropertyListener(<property>, this, &RDC_i::<callback method>)
        in the constructor.

        The callback method receives two arguments, the old and new values, and
        should return nothing (void). The arguments can be passed by value,
        receiving a copy (preferred for primitive types), or by const reference
        (preferred for strings, structs and vectors).

        Example:
            // This example makes use of the following Properties:
            //  - A float value called scaleValue
            //  - A struct property called status
            
        //Add to RDC.cpp
        RDC_i::RDC_i(const char *uuid, const char *label) :
            RDC_base(uuid, label)
        {
            addPropertyListener(scaleValue, this, &RDC_i::scaleChanged);
            addPropertyListener(status, this, &RDC_i::statusChanged);
        }

        void RDC_i::scaleChanged(float oldValue, float newValue)
        {
            RH_DEBUG(this->_baseLog, "scaleValue changed from" << oldValue << " to " << newValue);
        }
            
        void RDC_i::statusChanged(const status_struct& oldValue, const status_struct& newValue)
        {
            RH_DEBUG(this->_baseLog, "status changed");
        }
            
        //Add to RDC.h
        void scaleChanged(float oldValue, float newValue);
        void statusChanged(const status_struct& oldValue, const status_struct& newValue);

    Logging:

        The member _baseLog is a logger whose base name is the component (or device) instance name.
        New logs should be created based on this logger name.

        To create a new logger,
            rh_logger::LoggerPtr my_logger = this->_baseLog->getChildLogger("foo");

        Assuming component instance name abc_1, my_logger will then be created with the 
        name "abc_1.user.foo".

    Allocation:
    
        Allocation callbacks are available to customize the Device's response to 
        allocation requests. For example, if the Device contains the allocation 
        property "my_alloc" of type string, the allocation and deallocation
        callbacks follow the pattern (with arbitrary function names
        my_alloc_fn and my_dealloc_fn):
        
        bool RDC_i::my_alloc_fn(const std::string &value)
        {
            // perform logic
            return true; // successful allocation
        }
        void RDC_i::my_dealloc_fn(const std::string &value)
        {
            // perform logic
        }
        
        The allocation and deallocation functions are then registered with the Device
        base class with the setAllocationImpl call. Note that the variable for the property is used rather
        than its id:
        
        this->setAllocationImpl(my_alloc, this, &RDC_i::my_alloc_fn, &RDC_i::my_dealloc_fn);
        
        

************************************************************************************************/
int RDC_i::serviceFunction()
{
    RH_TRACE(this->_baseLog, "serviceFunction() example log message");
    
    return NOOP;
}

/*************************************************************
Functions supporting tuning allocation
*************************************************************/
void RDC_i::deviceEnable(frontend_tuner_status_struct_struct &fts, size_t tuner_id){
    /************************************************************
    modify fts, which corresponds to this->frontend_tuner_status[tuner_id]
    Make sure to set the 'enabled' member of fts to indicate that tuner as enabled
    ************************************************************/
	if (not digiSim) {
		RH_ERROR(this->_baseLog, "Tried to enable the device before it was initialized!");
		return;
	}

	digiSim->start();

	fts.center_frequency = digiSim->getCenterFrequency();
	fts.sample_rate = digiSim->getSampleRate();
	fts.bandwidth = MAX_SAMPLE_RATE;
	fts.stream_id = DEFAULT_STREAM_ID;
	fts.enabled = true;

    return;
}
void RDC_i::deviceDisable(frontend_tuner_status_struct_struct &fts, size_t tuner_id){
    /************************************************************
    modify fts, which corresponds to this->frontend_tuner_status[tuner_id]
    Make sure to reset the 'enabled' member of fts to indicate that tuner as disabled
    ************************************************************/
	if (digiSim) {
		digiSim->stop();
	}

	fts.enabled = false;
    return;
}
bool RDC_i::deviceSetTuning(const frontend::frontend_tuner_allocation_struct &request, frontend_tuner_status_struct_struct &fts, size_t tuner_id){
    /************************************************************
    modify fts, which corresponds to this->frontend_tuner_status[tuner_id]
      At a minimum, bandwidth, center frequency, and sample_rate have to be set
      If the device is tuned to exactly what the request was, the code should be:
        fts.bandwidth = request.bandwidth;
        fts.center_frequency = request.center_frequency;
        fts.sample_rate = request.sample_rate;

    return true if the tuning succeeded, and false if it failed
    ************************************************************/
	bool sriChanged = false;

	/************************************************************
    modify fts, which corresponds to this->frontend_tuner_status[tuner_id]
    return true if the tuning succeeded, and false if it failed
    ************************************************************/
	RH_INFO(this->_baseLog, "Received a request: ");
	RH_INFO(this->_baseLog, "Allocation ID: " << request.allocation_id);
	RH_INFO(this->_baseLog, "Bandwidth: " << request.bandwidth);
	RH_INFO(this->_baseLog, "Bandwidth Tolerance: " << request.bandwidth_tolerance);
	RH_INFO(this->_baseLog, "Center Frequency: " << request.center_frequency);
	RH_INFO(this->_baseLog, "Device Control: " << request.device_control);
	RH_INFO(this->_baseLog, "Group ID: " << request.group_id);
	RH_INFO(this->_baseLog, "RF Flow ID: " << request.rf_flow_id);
	RH_INFO(this->_baseLog, "Sample Rate: " << request.sample_rate);
	RH_INFO(this->_baseLog, "Sample Rate Tolerance: " << request.sample_rate_tolerance);
	RH_INFO(this->_baseLog, "Tuner Type: " << request.tuner_type);

	if (not digiSim) {
		RH_WARN(this->_baseLog, "deviceSetTuning called when Simulator has not been created.  This is not expected");
		return false;
	}

	if (request.tuner_type != "RX_DIGITIZER") {
		RH_WARN(this->_baseLog, "Tuner type does not equal RX_DIGITIZER.  Request denied.");
		return false;
	}

    // check request against RTL specs and analog input
    const bool complex = true; // RTL operates using complex data
    // Note: since samples are complex, assume BW == SR (rather than BW == SR/2 for Real samples)
    if (rfinfo_pkt.if_center_freq !=0)	{
		try {
			validateRequestVsRFInfo(request,rfinfo_pkt, complex);
		} catch(FRONTEND::BadParameterException& e){
			RH_INFO(this->_baseLog," Failed to Validate against rfInfo_Pkt. deviceSetTuning|BadParameterException - " << e.msg);
			throw;
		}
	}
    double if_offset = 0.0;
    // calculate if_offset according to rx rfinfo packet
    if(frontend::floatingPointCompare(rfinfo_pkt.if_center_freq,0) > 0){
        if_offset = rfinfo_pkt.rf_center_freq-rfinfo_pkt.if_center_freq;
		RH_DEBUG(this->_baseLog, "Set IF offset to : " << if_offset);

    }

	unsigned int sampleRateToSet = 0;

	// User cares about sample rate
	if (request.sample_rate != 0.0) {

		// For FEI tolerance, it is not a +/- it's give me this or better.
		float minAcceptableSampleRate = request.sample_rate;
		float maxAcceptableSampleRate = (1 + request.sample_rate_tolerance/100.0) * request.sample_rate;


		RH_INFO(this->_baseLog, "Based on request, minimum acceptable sample rate: " << minAcceptableSampleRate);
		RH_INFO(this->_baseLog, "Based on request, maximum acceptable sample rate: " << maxAcceptableSampleRate);

		// if the request isn't in the sample rate range return false
		if (minAcceptableSampleRate >= MAX_SAMPLE_RATE || maxAcceptableSampleRate <= MIN_SAMPLE_RATE) {
			RH_WARN(this->_baseLog, "Requested sample rate is outside of range");
			return false;
		}

		std::vector<unsigned int>::iterator closestIterator;
		closestIterator = std::lower_bound(availableSampleRates.begin(), availableSampleRates.end(), (unsigned int) (minAcceptableSampleRate+0.5));

		if (closestIterator == availableSampleRates.end()) {
			RH_ERROR(this->_baseLog, "Did not find sample rate in available list...this should not happen");
		}

		unsigned int closestSampleRate = *closestIterator;

		if (closestSampleRate > maxAcceptableSampleRate) {
			RH_WARN(this->_baseLog, "Cannot deliver sample rate within the requested tolerance");
			return false;
		}

		sampleRateToSet = closestSampleRate;
	}

	// User cares about bandwidth...they shouldn't though.  It's not setable.
	if (request.bandwidth != 0.0) {
		float minAcceptableBandwidth = request.bandwidth;
		float maxAcceptableBandwidth = (1 + request.bandwidth_tolerance/100.0) * request.bandwidth;

		RH_INFO(this->_baseLog, "Based on request, minimum acceptable bandwidth: " << minAcceptableBandwidth);
		RH_INFO(this->_baseLog, "Based on request, maximum acceptable bandwidth: " << maxAcceptableBandwidth);


		// if MAX_SAMPLE_RATE < minAcceptable or MAX_SAMPLE_RATE > maxAcceptable we return an error.
		if (frontend::floatingPointCompare(MAX_SAMPLE_RATE, minAcceptableBandwidth) < 0
				|| frontend::floatingPointCompare(MAX_SAMPLE_RATE, maxAcceptableBandwidth) > 0) {
			RH_WARN(this->_baseLog, "Bandwidth cannot be accommodated.  Set bandwidth to: " << MAX_SAMPLE_RATE);
			return false;
		}

	}

	try {
		digiSim->setCenterFrequency((float) request.center_frequency-if_offset);
		fts.center_frequency = digiSim->getCenterFrequency()+if_offset;
		sriChanged = true; // The COL and CHAN RF likely changed.
	} catch (RfSimulators::OutOfRangeException &ex) {
		RH_WARN(this->_baseLog, "Tried to tune to " << request.center_frequency-if_offset << " but request was rejected by simulator.");
		return false;
	}

	if (request.sample_rate > 0 && sampleRateToSet > 0) {
		RH_INFO(this->_baseLog, "Setting the sample rate to: " << sampleRateToSet);
		digiSim->setSampleRate(sampleRateToSet);
		fts.sample_rate = digiSim->getSampleRate();
		sriChanged = true;
	}

	fts.bandwidth = MAX_SAMPLE_RATE;

	if (sriChanged) {
		cb->pushUpdatedSRI();
	}

    return true;
}
bool RDC_i::deviceDeleteTuning(frontend_tuner_status_struct_struct &fts, size_t tuner_id) {
    /************************************************************
    modify fts, which corresponds to this->frontend_tuner_status[tuner_id]
    return true if the tune deletion succeeded, and false if it failed
    ************************************************************/
	if (digiSim) {
		digiSim->stop();
	}
	cb->pushEOS();

    fts.center_frequency = 0.0;
    fts.sample_rate = 0.0;
    fts.bandwidth = 0.0;
    fts.stream_id.clear();
    return true;
}
/*************************************************************
Functions servicing the tuner control port
*************************************************************/
std::string RDC_i::getTunerType(const std::string& allocation_id) {
    return frontend_tuner_status[0].tuner_type;
}

bool RDC_i::getTunerDeviceControl(const std::string& allocation_id) {
    return true;
}

std::string RDC_i::getTunerGroupId(const std::string& allocation_id) {
    return frontend_tuner_status[0].group_id;
}

std::string RDC_i::getTunerRfFlowId(const std::string& allocation_id) {
    return frontend_tuner_status[0].rf_flow_id;
}

void RDC_i::setTunerCenterFrequency(const std::string& allocation_id, double freq) {
    if (freq<0) throw FRONTEND::BadParameterException("Center frequency cannot be less than 0");
    // set hardware to new value. Raise an exception if it's not possible
    this->frontend_tuner_status[0].center_frequency = freq;
}

double RDC_i::getTunerCenterFrequency(const std::string& allocation_id) {
    return frontend_tuner_status[0].center_frequency;
}

void RDC_i::setTunerBandwidth(const std::string& allocation_id, double bw) {
    if (bw<0) throw FRONTEND::BadParameterException("Bandwidth cannot be less than 0");
    // set hardware to new value. Raise an exception if it's not possible
    this->frontend_tuner_status[0].bandwidth = bw;
}

double RDC_i::getTunerBandwidth(const std::string& allocation_id) {
    return frontend_tuner_status[0].bandwidth;
}

void RDC_i::setTunerAgcEnable(const std::string& allocation_id, bool enable)
{
    throw FRONTEND::NotSupportedException("setTunerAgcEnable not supported");
}

bool RDC_i::getTunerAgcEnable(const std::string& allocation_id)
{
    throw FRONTEND::NotSupportedException("getTunerAgcEnable not supported");
}

void RDC_i::setTunerGain(const std::string& allocation_id, float gain)
{
    throw FRONTEND::NotSupportedException("setTunerGain not supported");
}

float RDC_i::getTunerGain(const std::string& allocation_id)
{
    throw FRONTEND::NotSupportedException("getTunerGain not supported");
}

void RDC_i::setTunerReferenceSource(const std::string& allocation_id, long source)
{
    throw FRONTEND::NotSupportedException("setTunerReferenceSource not supported");
}

long RDC_i::getTunerReferenceSource(const std::string& allocation_id)
{
    throw FRONTEND::NotSupportedException("getTunerReferenceSource not supported");
}

void RDC_i::setTunerEnable(const std::string& allocation_id, bool enable) {
    // set hardware to new value. Raise an exception if it's not possible
    this->frontend_tuner_status[0].enabled = enable;
}

bool RDC_i::getTunerEnable(const std::string& allocation_id) {
    return frontend_tuner_status[0].enabled;
}

void RDC_i::setTunerOutputSampleRate(const std::string& allocation_id, double sr) {
    if (sr<0) throw FRONTEND::BadParameterException("Sample rate cannot be less than 0");
    // set hardware to new value. Raise an exception if it's not possible
    this->frontend_tuner_status[0].sample_rate = sr;
}

double RDC_i::getTunerOutputSampleRate(const std::string& allocation_id){
    return frontend_tuner_status[0].sample_rate;
}

void RDC_i::configureTuner(const std::string& allocation_id, const CF::Properties& tunerSettings){
    // set the appropriate tuner settings
}

CF::Properties* RDC_i::getTunerSettings(const std::string& allocation_id){
    // return the tuner settings
    redhawk::PropertyMap* tuner_settings = new redhawk::PropertyMap();
    return tuner_settings;
}

/*************************************************************
Functions servicing the RFInfo port(s)
- port_name is the port over which the call was received
*************************************************************/
std::string RDC_i::get_rf_flow_id(const std::string& port_name)
{
    return std::string("none");
}

void RDC_i::set_rf_flow_id(const std::string& port_name, const std::string& id)
{
}

frontend::RFInfoPkt RDC_i::get_rfinfo_pkt(const std::string& port_name)
{
    frontend::RFInfoPkt pkt;
    return pkt;
}

void RDC_i::set_rfinfo_pkt(const std::string& port_name, const frontend::RFInfoPkt &pkt)
{
}

