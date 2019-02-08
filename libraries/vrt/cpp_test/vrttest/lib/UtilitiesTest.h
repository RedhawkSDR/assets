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

#ifndef _UtilitiesTest_h
#define _UtilitiesTest_h

#include "Testable.h"
#include "TestSet.h"

namespace vrttest {
  /** Tests for the <tt>Utilities</tt> namespace. */
  class UtilitiesTest : public VRTObject, public virtual Testable {
    // Implement Testable
    public: virtual string getTestedClass ();
    public: virtual vector<TestSet> init ();
    public: virtual void done ();
    public: virtual void call (const string &func);

    // Test functions
    public: virtual void testAppend ();
    public: virtual void testBinarySearch ();
    public: virtual void testCurrentTimeMillis ();
    public: virtual void testFormat ();
    public: virtual void testNormalizeAngle180 ();
    public: virtual void testNormalizeAngle360 ();
    public: virtual void testToBoolean ();
    public: virtual void testToHexString ();
    public: virtual void testToLowerCase ();
    public: virtual void testToStringClassID ();
    public: virtual void testToStringDeviceID ();
    public: virtual void testToStringOUI ();
    public: virtual void testTrimNA ();
  };
} END_NAMESPACE

#endif /* _UtilitiesTest_h */
