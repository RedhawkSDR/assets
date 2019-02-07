/* ===================== COPYRIGHT NOTICE =====================
 * This file is protected by Copyright. Please refer to the COPYRIGHT file
 * distributed with this source distribution.
 *
 * This file is part of REDHAWK.
 *
 * REDHAWK is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * REDHAWK is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * ============================================================
 */

#ifndef _BasicContextPacketTest_h
#define _BasicContextPacketTest_h

#include "Testable.h"
#include "TestSet.h"

namespace vrttest {
  /** Tests for the {@link UUID} class. */
  class BasicContextPacketTest : public VRTObject, public virtual Testable {
    // Implement Testable
    public: virtual string getTestedClass ();
    public: virtual vector<TestSet> init ();
    public: virtual void done ();
    public: virtual void call (const string &func);

    // Test functions
    public: virtual void testSetPacketType();
    public: virtual void testResetForResend();

    // CIF0
    public: virtual void testGetAdjustedTimestamp();
    public: virtual void testSetAutomaticGainControl();
    public: virtual void testSetBandOffsetIF();
    public: virtual void testSetBandwidth();
    public: virtual void testSetCalibratedTimestamp();
    public: virtual void testSetChangePacket();
    public: virtual void testSetContextAssocLists();
    public: virtual void testSetDataPayloadFormat();
    public: virtual void testSetDataValid();
    public: virtual void testSetDeviceID();
    public: virtual void testSetDiscontinuous();
    public: virtual void testSetEphemerisECEF();
    public: virtual void testSetEphemerisReference();
    public: virtual void testSetGain();
    public: virtual void testSetGeolocation();
    public: virtual void testSetGeoSentences();
    public: virtual void testSetFrequencyIF();
    public: virtual void testSetFrequencyOffsetRF();
    public: virtual void testSetFrequencyRF();
    public: virtual void testSetInvertedSpectrum();
    public: virtual void testSetOverRange();
    public: virtual void testSetOverRangeCount();
    public: virtual void testSetReferenceLevel();
    public: virtual void testSetReferenceLocked();
    public: virtual void testSetReferencePointIdentifier();
    public: virtual void testSetSampleRate();
    public: virtual void testSetSamplePeriod();
    public: virtual void testSetSignalDetected();
//    public: virtual void testSetStateEventBit(); // Tested by all other bit getter/setters
    public: virtual void testSetTimeStampAdjustment();
    public: virtual void testSetTimeStampCalibration();
    public: virtual void testSetTemperature();
    public: virtual void testSetUserDefinedBits();

    public: virtual void testAddCIF1();
    public: virtual void testAddCIF2();
    public: virtual void testAddCIF3();
    public: virtual void testAddCIF7();

    // CIF1
    public: virtual void testSetBufferStatus();
    public: virtual void testSetBuildVersion();
    public: virtual void testSetV49SpecVersion();
    public: virtual void testSetHealthStatus();
    public: virtual void testSetDiscreteIO64();
    public: virtual void testSetDiscreteIO32();
    public: virtual void testSetDiscreteIO32CallOrder();
    public: virtual void testSetIndexList();
    public: virtual void testSetSectorScanStep();
    public: virtual void testSetCIFsArray();
    public: virtual void testSetSpectrumField();
    public: virtual void testSetAuxBandwidth();
    public: virtual void testSetAuxGain();
//    public: virtual void testSetAuxGain1(); // tested by testSetAuxGain
//    public: virtual void testSetAuxGain2(); // tested by testSetAuxGain
    public: virtual void testSetAuxFrequency();
//    public: virtual void testSetSNR(); // tested by testSetSNRNoiseFigure
//    public: virtual void testSetNoiseFigure(); // tested by testSetSNRNoiseFigure
    public: virtual void testSetSNRNoiseFigure();
//    public: virtual void testSetSecondOrderInputInterceptPoint(); // tested by testSetInputInterceptPoint
//    public: virtual void testSetThirdOrderInputInterceptPoint(); // tested by testSetInputInterceptPoint
    public: virtual void testSetInputInterceptPoint();
    public: virtual void testSetOneDecibelCompressionPoint();
    public: virtual void testSetThreshold();
//    public: virtual void testSetEbN0(); // tested by testSetEbN0BitErrorRate
//    public: virtual void testSetBitErrorRate(); // tested by testSetEbN0BitErrorRate
    public: virtual void testSetEbN0BitErrorRate();
    public: virtual void testSetRange();
    public: virtual void testSetBeamwidth();
    public: virtual void testSet3DPointingVectorStructured();
//    public: virtual void testSet3DPointingVectorElevation(); // tested by testSet3DPointingVector
//    public: virtual void testSet3DPointingVectorAzimuth(); // tested by testSet3DPointingVector
    public: virtual void testSet3DPointingVector();
//    public: virtual void testSetPolarizationTiltAngle(); // tested by testSetPolarizationAngle
//    public: virtual void testSetPolarizationEllipticityAngle(); // tested by testSetPolarizationAngle
    public: virtual void testSetPolarizationAngle();
    public: virtual void testSetPhaseOffset();

    // CIF2
    public: virtual void testSetSpatialReferenceType();
    public: virtual void testSetSpatialScanType();
    public: virtual void testSetFunctionPriorityID();
    public: virtual void testSetEventID();
    public: virtual void testSetModeID();
    public: virtual void testSetFunctionID();
    public: virtual void testSetModulationType();
    public: virtual void testSetModulationClass();
    public: virtual void testSetEmsDeviceClass();
    public: virtual void testSetOperator();
    public: virtual void testSetCountryCode();

    public: virtual void testSetRFFootprintRange();
    public: virtual void testSetRFFootprint();
    public: virtual void testSetCommunicationPriorityID();
    public: virtual void testSetEmsDeviceInstance();
    public: virtual void testSetEmsDeviceType();
    public: virtual void testSetPlatformDisplay();
    public: virtual void testSetPlatformInstance();
    public: virtual void testSetPlatformClass();
    public: virtual void testSetTrackID();
    public: virtual void testSetInformationSource();
    public: virtual void testSetControllerUUIDField();
    public: virtual void testSetControllerIDField();
    public: virtual void testSetControlleeUUIDField();
    public: virtual void testSetControlleeIDField();
    public: virtual void testSetCitedMessageID();
    public: virtual void testSetBindField();

    public: virtual void testSetChildrenSID();
    public: virtual void testSetParentsSID();
    public: virtual void testSetSiblingsSID();
    public: virtual void testSetCitedSID();

    // CIF3
    public: virtual void testSetNetworkID();
    public: virtual void testSetTroposphericState();
    public: virtual void testSetSeaAndSwellState();
    public: virtual void testSetBarometricPressure();
    public: virtual void testSetHumidity();
    public: virtual void testSetSeaGroundTemperature();
    public: virtual void testSetAirTemperature();
    public: virtual void testSetShelfLife();
    public: virtual void testSetAge();
    public: virtual void testSetJitter();
    public: virtual void testSetDwell();
    public: virtual void testSetDuration();
    public: virtual void testSetPeriod();
    public: virtual void testSetPulseWidth();
    public: virtual void testSetOffsetTime();
    public: virtual void testSetFallTime();
    public: virtual void testSetRiseTime();
    public: virtual void testSetTimestampSkew();
    public: virtual void testSetTimestampDetails();

    // CIF7
    public: virtual void testSetCIF7Belief();
    public: virtual void testSetCIF7Probability();
    public: virtual void testSetReferencePointIdentifierCIF7Median();
    public: virtual void testSetReferencePointIdentifierCIF7Multiple();

  };
} END_NAMESPACE

#endif /* _BasicContextPacketTest_h */
