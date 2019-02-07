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

#ifndef _VRTMathTest_h
#define _VRTMathTest_h

#include "Testable.h"
#include "TestSet.h"

namespace vrttest {
  /** Tests for the {@link VRTMath} class. */
  class VRTMathTest : public VRTObject, public virtual Testable {
    // Implement Testable
    public: virtual string getTestedClass ();
    public: virtual vector<TestSet> init ();
    public: virtual void done ();
    public: virtual void call (const string &func);

    // Test functions
    public: virtual void testDoubleToRawLongBits ();
    public: virtual void testFloatToRawIntBits ();
    public: virtual void testFromDouble64 ();
    public: virtual void testFromDouble32 ();
    public: virtual void testFromDouble16 ();
    public: virtual void testFromHalf ();
    public: virtual void testFromHalfInternal ();
    public: virtual void testPackAscii ();
    public: virtual void testPackBits64 ();
    public: virtual void testPackBoolNull ();
    public: virtual void testPackBoolean ();
    public: virtual void testPackByte ();
    public: virtual void testPackBytes ();
    public: virtual void testPackDouble ();
    public: virtual void testPackFloat ();
    public: virtual void testPackInt ();
    public: virtual void testPackInt24 ();
    public: virtual void testPackLong ();
    public: virtual void testPackShort ();
    public: virtual void testToHalf ();
    public: virtual void testToHalfInternal ();
    public: virtual void testUnpackBits64 ();
    public: virtual void testUnpackBoolNull ();
    public: virtual void testUnpackBoolean ();
    public: virtual void testUnpackByte ();
    public: virtual void testUnpackBytes ();
    public: virtual void testUnpackDouble ();
    public: virtual void testUnpackFloat ();
    public: virtual void testUnpackInetAddr ();
    public: virtual void testUnpackInt ();
    public: virtual void testUnpackInt24 ();
    public: virtual void testUnpackLong ();
    public: virtual void testUnpackShort ();
    public: virtual void testUnpackTimeStamp ();
    public: virtual void testUnpackUUID ();
    public: virtual void testVRTFloat ();
  };
} END_NAMESPACE

#endif /* _VRTMathTest_h */
