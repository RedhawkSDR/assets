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

#ifndef _BasicVRTPacketTest_h
#define _BasicVRTPacketTest_h

#include "Testable.h"
#include "TestSet.h"

namespace vrttest {
  /** Tests for the {@link BasicVRTPacket} class. */
  class BasicVRTPacketTest : public VRTObject, public virtual Testable {
    // Implement Testable
    public: virtual string getTestedClass ();
    public: virtual vector<TestSet> init ();
    public: virtual void done ();
    public: virtual void call (const string &func);

    // Test functions
    public: virtual void testEquals ();
    public: virtual void testGetClassID ();
    public: virtual void testGetField ();
    public: virtual void testGetFieldName ();
    public: virtual void testGetPacket ();
    public: virtual void testGetPacketCount ();
    public: virtual void testGetPacketLength ();
    public: virtual void testGetPacketType ();
    public: virtual void testGetPacketValid ();
    public: virtual void testGetPadBitCount ();
    public: virtual void testGetStreamCode ();
    public: virtual void testGetStreamID ();
    public: virtual void testGetTimeStamp ();
    public: virtual void testIsChangePacket ();
    public: virtual void testIsTimeStampMode ();
    public: virtual void testReadPacket ();
    public: virtual void testResetForResend ();
    public: virtual void testToString ();
    public: virtual void testWritePayload ();
    public: virtual void testIsSpectrumMode ();
  };

  /** Tests for the {@link PayloadFormat} class. */
  class PayloadFormatTest : public VRTObject, public virtual Testable {
    // Implement Testable
    public: virtual string getTestedClass ();
    public: virtual vector<TestSet> init ();
    public: virtual void done ();
    public: virtual void call (const string &func);

    // Test functions
    public: virtual void testEquals ();
    public: virtual void testGetBits ();
    public: virtual void testGetChannelTagSize ();
    public: virtual void testGetDataItemFormat ();
    public: virtual void testGetDataItemSize ();
    public: virtual void testGetDataItemFracSize ();
    public: virtual void testGetDataType ();
    public: virtual void testGetEventTagSize ();
    public: virtual void testGetField ();
    public: virtual void testGetFieldName ();
    public: virtual void testGetItemPackingFieldSize ();
    public: virtual void testGetRealComplexType ();
    public: virtual void testGetRepeatCount ();
    public: virtual void testGetValid ();
    public: virtual void testGetVectorSize ();
    public: virtual void testIsProcessingEfficient ();
    public: virtual void testIsRepeating ();
    public: virtual void testIsSigned ();
    public: virtual void testToString ();
  };
} END_NAMESPACE

#endif /* _BasicVRTPacketTest_h */
