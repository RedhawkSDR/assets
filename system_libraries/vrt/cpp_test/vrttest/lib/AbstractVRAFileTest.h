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

#ifndef _AbstractVRAFileTest_h
#define _AbstractVRAFileTest_h

#include "AbstractVRAFile.h"
#include "BasicVRAFile.h"
#include "Testable.h"
#include "TestSet.h"
#include <vector>

namespace vrttest {
  /** Tests for the {@link AbstractVRAFile} class. */
  class AbstractVRAFileTest : public VRTObject, public virtual Testable {
    private: string testedClass;
    private: string fname1;
    private: string fname2;

    /** Creates a new instance for the given tested class. */
    public: AbstractVRAFileTest (string testedClass);

    /** Opens a file using an instance of the tested class. */
    protected: virtual AbstractVRAFile* openFile (string fname, FileMode mode, bool isSetSize=false,
                                                  bool isSetCRC=false, bool isStrict=false);

    /** <b>Internal Use Only:</b> Creates a set of test packets to use. */
    public: static vector<BasicVRTPacket*> makeTestPacketVector (); // Used by VRAFileTest and VRLFrameTest

    /** <b>Internal Use Only:</b> Discards a set of test packets to use. */
    public: static void deleteTestPacketVector (vector<BasicVRTPacket*> &v); // Used by VRAFileTest and VRLFrameTest

    // Implement Testable
    public: virtual string getTestedClass ();
    public: virtual vector<TestSet> init ();
    public: virtual void done ();
    public: virtual void call (const string &func);

    // Test functions
    public: virtual void testConstants();
    public: virtual void testAppend();
    public: virtual void testGetThisPacket();
    public: virtual void testGetURI();
    public: virtual void testGetVersion();
    public: virtual void testToString();
  };
} END_NAMESPACE

#endif /* _AbstractVRAFileTest_h */
