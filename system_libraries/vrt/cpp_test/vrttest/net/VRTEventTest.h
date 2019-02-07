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

#ifndef _VRTEventTest_h
#define _VRTEventTest_h

#include "NetUtilities.h"
#include "Testable.h"
#include "TestRunner.h"
#include "TestSet.h"
#include "Utilities.h"

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace TestRunner;

namespace vrttest {
  /** Tests for the {@link VRTEvent} class. */
  class VRTEventTest : public VRTObject, public virtual Testable {
    // Implement Testable
    public: virtual string getTestedClass ();
    public: virtual vector<TestSet> init ();
    public: virtual void done ();
    public: virtual void call (const string &func);

    // Test functions
    public: virtual void testEquals ();
    public: virtual void testGetPacket ();
  };
} END_NAMESPACE
#endif /* _VRTEventTest_h */
