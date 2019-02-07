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

#ifndef _BasicCommandPacketTest_h
#define _BasicCommandPacketTest_h

#include "Testable.h"
#include "TestSet.h"

namespace vrttest {
  /** Tests for the {@link UUID} class. */
  class BasicCommandPacketTest : public VRTObject, public virtual Testable {
    // Implement Testable
    public: virtual string getTestedClass ();
    public: virtual vector<TestSet> init ();
    public: virtual void done ();
    public: virtual void call (const string &func);

    // Test functions
    public: virtual void testSetPacketType();
    public: virtual void testResetForResend();
// These methods are protected and cannot be tested in this way
//    public: virtual void testSetControlleeEnable();
//    public: virtual void testSetControlleeFormat();
//    public: virtual void testSetControllerEnable();
//    public: virtual void testSetControllerFormat();
    public: virtual void testSetPartialChangePermitted();
    public: virtual void testSetWarningsPermitted();
    public: virtual void testSetErrorsPermitted();
    public: virtual void testSetAction(); // setAction1Flag, setAction0Flag, setActionNoAction, setActionDryRun, setActionTakeAction
    public: virtual void testSetNotAckOnly();
    public: virtual void testSetValidationAcknowledge();
    public: virtual void testSetRequestValidationAcknowledge();
    public: virtual void testSetExecutionAcknowledge();
    public: virtual void testSetRequestExecutionAcknowledge();
    public: virtual void testSetQueryAcknowledge();
    public: virtual void testSetRequestQueryAcknowledge();
    public: virtual void testSetWarningsGenerated();
    public: virtual void testSetRequestWarningsGenerated();
    public: virtual void testSetErrorsGenerated();
    public: virtual void testSetRequestErrorsGenerated();
    public: virtual void testSetTimestampControlMode(); // setTimestampControl2Bit, setTimestampControl1Bit, setTimestampControl0Bit, setTimestampControlMode
    public: virtual void testSetPartialAction();
    public: virtual void testSetActionExecuted();

    public: virtual void testSetMessageID();
    public: virtual void testSetControlleeID(); // setControlleeIDNumber, setControlleeUUID
    public: virtual void testSetControllerID(); // setControllerIDNumber, setControllerUUID
    public: virtual void testSetCancelMode();
  };
} END_NAMESPACE

#endif /* _BasicCommandPacketTest_h */
