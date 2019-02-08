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

#ifndef _TimeStampTest_h
#define _TimeStampTest_h

#include "Testable.h"
#include "TestSet.h"

namespace vrttest {
  /** Tests for the {@link TimeStamp} class. */
  class TimeStampTest : public VRTObject, public virtual Testable {
    // Implement Testable
    public: virtual string getTestedClass ();
    public: virtual vector<TestSet> init ();
    public: virtual void done ();
    public: virtual void call (const string &func);

    // Test functions
    public: virtual void testTimeStamp ();
    public: virtual void testAddPicoSeconds ();
    public: virtual void testAddSeconds ();
    public: virtual void testAddTime ();
    public: virtual void testCompareTo ();
    public: virtual void testEquals ();
    public: virtual void testForTime ();
    public: virtual void testForTimeIRIG ();
    public: virtual void testForTimeMidas ();
    public: virtual void testForTimePOSIX ();
    public: virtual void testForTimePTP ();
    public: virtual void testGetEpoch ();
    public: virtual void testGetField ();
    public: virtual void testGetFieldName ();
    public: virtual void testGetFractionalSeconds ();
    public: virtual void testGetGPSSeconds ();
    public: virtual void testGetMidasSeconds ();
    public: virtual void testGetNORADSeconds ();
    public: virtual void testGetPOSIXSeconds ();
    public: virtual void testGetPicoSeconds ();
    public: virtual void testGetSystemTime ();
    public: virtual void testGetUTCSeconds ();
    public: virtual void testParseTime ();
    public: virtual void testSetField ();
    public: virtual void testToStringUTC ();
    public: virtual void testToStringGPS ();
    public: virtual void testToUTC ();
    public: virtual void testToGPS ();
  };
} END_NAMESPACE

#endif /* _TimeStampTest_h */
