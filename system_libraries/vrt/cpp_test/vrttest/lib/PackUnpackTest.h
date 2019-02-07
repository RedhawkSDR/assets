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

#ifndef _PackUnpackTest_h
#define _PackUnpackTest_h

#include "Testable.h"
#include "TestSet.h"

namespace vrttest {
  /** Tests for {@link PackUnpack}. This has a HUGE number of iterations since simply
   *  due to the high multiplicity inherent in the payload formats:
   *  <pre>
   *    dSize    =  1 to 64
   *    eSize    =  0 to  7 (or null)
   *    cSize    =  0 to 15 (or null)
   *    fSize    =  N to 64
   *    format   =  Int/UInt/Float32/Double64
   *    packing  =  Link/Processing
   *    offset   =  N
   *    arrayFmt =  byte/short/int/long/float/double
   *  </pre>
   *  In the end there are around 48 million permutations that need to be tested
   *  (170 million if you include the VRT floating point formats). This tries to
   *  do at least some testing on all of them. The integer formats are the most
   *  tested as are the direct byte/short/int/long/float/double payloads. <br>
   *  <br>
   *  When the full suite of tests are included (VRT_QUICK_TEST=false) this test
   *  set takes a long time to run at around ~20 billion tests. By default only a
   *  small subset is run (VRT_QUICK_TEST=true) with around 0.3 billion tests.
   */
  class PackUnpackTest : public VRTObject, public virtual Testable {
    // Implement Testable
    public: virtual string getTestedClass ();
    public: virtual vector<TestSet> init ();
    public: virtual void done ();
    public: virtual void call (const string &func);

    // Test functions
    public: virtual void testPack ();
    public: virtual void testUnpack ();
  };
} END_NAMESPACE

#endif /* _PackUnpackTest_h */
