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

#ifndef _BasicDataPacketTest_h
#define _BasicDataPacketTest_h

#include "Testable.h"
#include "TestSet.h"
#include "BasicVRTPacket.h"

namespace vrttest {
  /** Tests for the {@link BasicDataPacket} class. */
  class BasicDataPacketTest : public VRTObject, public virtual Testable {
    private: vector<char>          input;       // input test values (0x00 through 0xFF)
    private: vector<char>          outputSwap2; // same as input with bytes swapped by 2
    private: vector<char>          outputSwap4; // same as input with bytes swapped by 4
    private: vector<char>          outputSwap8; // same as input with bytes swapped by 8
    private: vector<PayloadFormat> formats;     // test formats

    // Implement Testable
    public: virtual string getTestedClass ();
    public: virtual vector<TestSet> init ();
    public: virtual void done ();
    public: virtual void call (const string &func);

    // Test functions
    public: virtual void testGetData();
    public: virtual void testGetDataByte();
    public: virtual void testGetField();
    public: virtual void testGetFieldName();
    public: virtual void testGetLostSamples();
    public: virtual void testGetNextTimeStamp();
    public: virtual void testSetAssocPacketCount();
    public: virtual void testSetAutomaticGainControl();
    public: virtual void testSetBit11();
    public: virtual void testSetBit10();
    public: virtual void testSetBit9();
    public: virtual void testSetBit8();
    public: virtual void testSetCalibratedTimeStamp();
    public: virtual void testSetData();
    public: virtual void testSetDataByte();
    public: virtual void testSetDataLength();
    public: virtual void testSetDataValid();
    public: virtual void testSetDiscontinuous();
    public: virtual void testSetInvertedSpectrum();
    public: virtual void testSetOverRange();
    public: virtual void testSetPacketType();
    public: virtual void testSetPayloadFormat();
    public: virtual void testSetReferenceLocked();
    public: virtual void testSetScalarDataLength();
    public: virtual void testSetSignalDetected();
  };
} END_NAMESPACE

#endif /* _BasicDataPacketTest_h */
