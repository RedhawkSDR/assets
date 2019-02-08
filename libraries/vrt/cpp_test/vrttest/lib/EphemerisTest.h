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

#ifndef _EphemerisTest_h
#define _EphemerisTest_h

#include "Testable.h"
#include "TestSet.h"

namespace vrttest {
  /** Tests for the {@link UUID} class. */
  class EphemerisTest : public VRTObject, public virtual Testable {
    // Implement Testable
    public: virtual string getTestedClass ();
    public: virtual vector<TestSet> init ();
    public: virtual void done ();
    public: virtual void call (const string &func);

    // Test functions
    public: virtual void testSetAttitudeAlpha();
    public: virtual void testSetAttitudeBeta();
    public: virtual void testSetAttitudePhi();
    public: virtual void testSetPositionX();
    public: virtual void testSetPositionY();
    public: virtual void testSetPositionZ();
    public: virtual void testSetTimeStamp();
    public: virtual void testSetVelocityX();
    public: virtual void testSetVelocityY();
    public: virtual void testSetVelocityZ();
  };
} END_NAMESPACE
#endif /* _BasicContextPacketTest_h */
