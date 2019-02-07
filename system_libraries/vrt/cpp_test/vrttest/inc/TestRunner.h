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

#ifndef _TestRunner_h
#define _TestRunner_h

#include "NetUtilities.h"
#include "TestSet.h"
#include "Testable.h"
#include "Utilities.h"
#include "VRTObject.h"
#include "Record.h"
#include "Value.h"
#include <sstream>
#include <vector>
#include <map>

using namespace std;
using namespace vrt;

#define TestFailedException(msg,exp,act)      VRTException(formatError(msg,exp,act))
#define TestFailedExceptionT(msg,exp,act,tol) VRTException(formatError(msg,exp,act,tol))
#define PLUS_MINUS "+/-"

namespace vrttest {
  /** Main class for running tests. Normal usage:
   *  <pre>
   *    vector&lt;Testable*&gt; testCases;
   *    testCases.push_back(new MyTest1());
   *    testCases.push_back(new MyTest2());
   *    testCases.push_back(new MyTest3());
   *
   *    string str = runTests(testCases);
   *    cout &lt;&lt; str &lt;&lt; endl;
   *  </pre>
   *  Previously this defined the following template:
   *  <pre>
   *    // Assert that a given test will have the specified value.
   *    // @param msg  String describing the test case.
   *    // @param exp  The expected value.
   *    // @param act  The actual value.
   *    template <typename T>
   *    inline void _assertEquals (const string &msg, T exp, T act) { ... }
   *  </pre>
   *  However this caused issues on some systems where a specialized version for
   *  <tt>VRTObject*</tt> pointers was unsupported in this form. Accordingly the
   *  various types are all defined herein as inline functions.
   */
  namespace TestRunner {
    extern TestSet *thisTest; // the current test set
    extern string   serverURL; // the server being used

    /** <b>Internal Use Only:</b> Formats an error message.
     *  @param msg  String describing the test case.
     *  @param exp  The expected value.
     *  @param act  The actual value.
     */
    template <typename T>
    inline string formatError (const string &msg, T exp, T act) {
      string _msg = (msg == "")? "???" : msg;
      ostringstream str;
      str << "Testing " << _msg << " expected '" << exp << "' but got '" << act << "'";
      return str.str();
    }

    // force 8-bit values to print in numeric form
    template <>
    inline string formatError (const string &msg, char exp, char act) {
      string _msg = (msg == "")? "???" : msg;
      return Utilities::format("Testing %s expected '%d' but got '%d'", _msg.c_str(), exp, act);
    }

    // force 8-bit values to print in numeric form
    template <>
    inline string formatError (const string &msg, signed char exp, signed char act) {
      string _msg = (msg == "")? "???" : msg;
      return Utilities::format("Testing %s expected '%d' but got '%d'", _msg.c_str(), exp, act);
    }

    // force 8-bit values to print in numeric form
    template <>
    inline string formatError (const string &msg, unsigned char exp, unsigned char act) {
      string _msg = (msg == "")? "???" : msg;
      // The cast to uint32_t is required when using the Intel compiler
      return Utilities::format("Testing %s expected '%ud' but got '%ud'", _msg.c_str(), (uint32_t)exp, (uint32_t)act);
    }

    // special VRTObject handling
    template <>
    inline string formatError (const string &msg, const VRTObject &exp, const VRTObject &act) {
      string _msg = (msg == "")? "???" : msg;
      string _exp = exp.toString();
      string _act = act.toString();
      return string("Testing ") + _msg + " expected '" + _exp + "' but got '" + _act + "'";
    }

    // special VRTObject handling
    template <>
    inline string formatError (const string &msg, const VRTObject *exp, const VRTObject *act) {
      string _msg = (msg == "")? "???" : msg;
      string _exp = (((void*)exp) == NULL)? string("NULL") : exp->toString();
      string _act = (((void*)act) == NULL)? string("NULL") : act->toString();
      return string("Testing ") + _msg + " expected '" + _exp + "' but got '" + _act + "'";
    }

    // special Record handling
    template <>
    inline string formatError (const string &msg, const Record &exp, const Record &act) {
      string _msg = (msg == "")? "???" : msg;
      string _exp = exp.toString();
      string _act = act.toString();
      return string("Testing ") + _msg + " expected '" + _exp + "' but got '" + _act + "'";
    }

    // special Record handling
    template <>
    inline string formatError (const string &msg, const Record *exp, const Record *act) {
      string _msg = (msg == "")? "???" : msg;
      string _exp = (((void*)exp) == NULL)? string("NULL") : exp->toString();
      string _act = (((void*)act) == NULL)? string("NULL") : act->toString();
      return string("Testing ") + _msg + " expected '" + _exp + "' but got '" + _act + "'";
    }

    /** <b>Internal Use Only:</b> Formats a numeric error message.
     *  @param msg  String describing the test case.
     *  @param exp  The expected value.
     *  @param act  The actual value.
     *  @param tol  The absolute tolerance permitted (inclusive).
     */
    template <typename N>
    inline string formatError (const string &msg, N exp, N act, N tol) {
      string _msg = (msg == "")? "???" : msg;
      ostringstream str;
      str << "Testing " << _msg << " expected " << exp << " " << PLUS_MINUS << tol << " but got " << act;
      return str.str();
    }

    // force 8-bit values to print in numeric form
    inline string formatError (const string &msg, char exp, char act, char tol) {
      string _msg = (msg == "")? "???" : msg;
      return Utilities::format("Testing %s expected '%d' " PLUS_MINUS "%d but got '%d'", _msg.c_str(), exp, tol, act);
    }

    // force 8-bit values to print in numeric form
    inline string formatError (const string &msg, signed char exp, signed char act, signed char tol) {
      string _msg = (msg == "")? "???" : msg;
      return Utilities::format("Testing %s expected '%d' " PLUS_MINUS "%d but got '%d'", _msg.c_str(), exp, tol, act);
    }

    // force 8-bit values to print in numeric form
    inline string formatError (const string &msg, unsigned char exp, unsigned char act, unsigned char tol) {
      string _msg = (msg == "")? "???" : msg;
      // The cast to uint32_t is required when using the Intel compiler
      return Utilities::format("Testing %s expected '%ud' " PLUS_MINUS "%ud but got '%ud'", _msg.c_str(),
                                                             (uint32_t)exp, (uint32_t)tol, (uint32_t)act);
    }


    /** Are the two values equal? */
    template <typename T>
    inline bool _isEqual (T exp, T act) {
      // Need to disable Intel compiler warning 1572 (floating-point equality and
      // inequality comparisons are unreliable) since it will otherwise flag the
      // following check even though it is a correct.
      _Intel_Pragma("warning push")
      _Intel_Pragma("warning disable 1572")
      return (exp == act)
          || (isNull(exp) && isNull(act)); // <-- (need this for NaN=NaN with float/double)
      _Intel_Pragma("warning pop")
    }

    /** Are the two values equal (or within tolerance)? */
    template <typename N>
    bool _isEqual (N exp, N act, N tol) {
      if (_isEqual(exp, act)) return true;
      if (isNull(exp) || isNull(act)) return false;

      N delta = act - exp;
      return !((delta > tol) || (delta < -tol));
    }

    /** Runs all of the given test cases and prints results to console.
     *  @param testCases The test cases to run.
     *  @param verbose   Verbose output on/off.
     *  @return True if the tests PASS, false if they FAIL.
     */
    bool runTests (vector<Testable*> &testCases, bool verbose);

    /** Sends a request to the network test server and returns the response. */
    Value* sendServerRequest (const string &path, map<string,string> &req)
      __attribute__((warn_unused_result)); // user must free return value

    /** Gets the default socket options to use; these aren't general options, but
     *  rather are modified to best suit the test cases (which have slightly
     *  different requirements than normal uses).
     *  @param transport Specific transport to use for any transport-specific
     *                   optimizations (can be null).
     *  @return The set of options to use (never null).
     */
    map<string,string> getTestSocketOptions (TransportProtocol transport);

#define ASSERT_EQUALS(type) \
inline void assertEquals (const string &msg, type exp, type act) { \
  thisTest->countTest(); \
  if (!_isEqual(exp, act)) { \
    throw TestFailedException(msg, exp, act); \
  } \
}

    ASSERT_EQUALS(int8_t)
    ASSERT_EQUALS(int16_t)
    ASSERT_EQUALS(int32_t)
    ASSERT_EQUALS(int64_t)
    ASSERT_EQUALS(uint8_t)
    ASSERT_EQUALS(uint16_t)
    ASSERT_EQUALS(uint32_t)
    ASSERT_EQUALS(uint64_t)
    ASSERT_EQUALS(float)
    ASSERT_EQUALS(double)
    ASSERT_EQUALS(const string &)
    ASSERT_EQUALS(const wstring &)

#if (defined(__APPLE__) && defined(__MACH__))
    // Required under OS X (other BSD?) but causes errors on other platforms since it overlaps
    // with 32-/64-bit definitions
    ASSERT_EQUALS(size_t)
#endif

    // Non-standard forms...
    inline void assertEquals (const string &msg, const VRTObject *exp, const VRTObject *act) {
      thisTest->countTest();
      if (!VRTObject::equal(exp, act)) {
        throw TestFailedException(msg, exp, act);
      }
    }
    inline void assertEquals (const string &msg, const VRTObject &exp, const VRTObject &act) {
      thisTest->countTest();
      if (!VRTObject::equal(exp, act)) {
        throw TestFailedException(msg, exp, act);
      }
    }
    inline void assertEquals (const string &msg, const Record *exp, const Record *act) {
      thisTest->countTest();
      if (!Record::equal(exp, act)) {
        throw TestFailedException(msg, exp, act);
      }
    }
    inline void assertEquals (const string &msg, const Record &exp, const Record &act) {
      thisTest->countTest();
      if (!Record::equal(exp, act)) {
        throw TestFailedException(msg, exp, act);
      }
    }
    inline void assertEquals (const string &msg, const void *exp, const void *act) {
      thisTest->countTest();
      if (exp != act) {
        throw TestFailedException(msg, exp, act);
      }
    }

    // These have types that differ...
    inline void assertEquals (const string &msg, int64_t exp, uint64_t act) {
      assertEquals(msg, exp, (int64_t)act);
    }
    inline void assertEquals (const string &msg, uint64_t exp, int64_t act) {
      assertEquals(msg, (int64_t)exp, act);
    }
    inline void assertEquals (const string &msg, int32_t exp, uint32_t act) {
      assertEquals(msg, exp, (int32_t)act);
    }
    inline void assertEquals (const string &msg, uint32_t exp, int32_t act) {
      assertEquals(msg, (int32_t)exp, act);
    }

    /** Assert that a given test will have the specified value +/- a tolerance.
     *  This test will first try an <tt>exp.equals(act)</tt>, failing that it
     *  will do a numeric check. This behavior ensures that the following:
     *  <pre>
     *    - If both are NaN the test will PASS, if only one is it will FAIL.
     *    - If one is -0.0 and the other is +0.0 the test will PASS.
     *  </pre>
     *  Currently the tolerance checks only support instances of {@link Double},
     *  {@link Float}, {@link Long}, {@link Integer}, {@link Short}, {@link Byte}.
     *  For all other instances of {@link Number} this assumes a strict equality
     *  check (i.e. <tt>tol=0</tt>).
     *  @param msg  String describing the test case.
     *  @param exp  The expected value.
     *  @param act  The actual value.
     *  @param tol  The absolute tolerance permitted (inclusive). Passing in null
     *              is the same as passing in zero.
     *  @param args Format string arguments.
     */
    template <typename N>
    void assertEquals (const string &msg, N exp, N act, N tol) {
      thisTest->countTest();
      if (!_isEqual(exp, act, tol)) {
        throw TestFailedExceptionT(msg, exp, act, tol);
      }
    }

    /** Assert that a given test will have the specified value. This does a
     *  pointer check rather than a value check.
     *  @param msg  String describing the test case.
     *  @param exp  The expected value.
     *  @param act  The actual value.
     */
    void assertSame (const string &msg, const VRTObject *exp, const VRTObject *act);

    /** Assert that a given test will have the specified value. This does a
     *  check on two buffers. Note: <tt>expIdx</tt> and <tt>actIdx</tt> do
     *  not need to match (permitting one to be offset), however <tt>expLen</tt>
     *  and <tt>actLen</tt> must match.
     *  @param msg    String describing the test case.
     *  @param expBuf The expected buffer value.
     *  @param expIdx The expected buffer start index.
     *  @param expLen The expected buffer length.
     *  @param actBuf The actual buffer value.
     *  @param actIdx The actual buffer start index.
     *  @param actLen The actual buffer length.
     */
    void assertBufEquals (const string &msg, const void *expBuf, size_t expIdx, size_t expLen,
                                             const void *actBuf, size_t actIdx, size_t actLen);

    /** Assert that a given test will have the specified value. This is identical
     *  to <tt>assertBufEquals(msg,expBuf,0,expLen,actBuf,0,actLen)</tt>.
     *  @param msg    String describing the test case.
     *  @param expBuf The expected buffer pointer.
     *  @param expLen The expected buffer length.
     *  @param actBuf The actual buffer pointer.
     *  @param actLen The actual buffer length.
     */
    inline void assertBufEquals (const string &msg, const void *expBuf, size_t expLen,
                                                    const void *actBuf, size_t actLen) {
      assertBufEquals(msg,expBuf,0,expLen,actBuf,0,actLen);
    }

    /** Assert that a given test will have the specified value. This does a
     *  check on two buffers. Note: <tt>expIdx</tt> and <tt>actIdx</tt> do
     *  not need to match (permitting one to be offset), however <tt>expLen</tt>
     *  and <tt>actLen</tt> must match.
     *  @param msg    String describing the test case.
     *  @param expBuf The expected buffer value.
     *  @param expIdx The expected buffer start index.
     *  @param expLen The expected buffer length.
     *  @param actBuf The actual buffer value.
     *  @param actIdx The actual buffer start index.
     *  @param actLen The actual buffer length.
     */
    inline void assertBufEquals (const string &msg, const vector<char> &expBuf, size_t expIdx, size_t expLen,
                                                    const vector<char> &actBuf, size_t actIdx, size_t actLen) {
      assertBufEquals(msg, &expBuf[0], expIdx, expLen,
                           &actBuf[0], actIdx, actLen);
    }

    /** Assert that a given test will have the specified value. This does a
     *  check on two array. Note: <tt>expIdx</tt> and <tt>actIdx</tt> do
     *  not need to match (permitting one to be offset), however <tt>expLen</tt>
     *  and <tt>actLen</tt> must match.
     *  @param msg      String describing the test case.
     *  @param expArray The expected array value.
     *  @param expLen   The expected array length.
     *  @param actArray The actual array value.
     *  @param actLen   The actual array length.
     */
    template <typename T>
    inline void assertArrayEquals (const string &msg, const T* expArray, size_t expLen,
                                                      const T* actArray, size_t actLen) {
      thisTest->countTest();
      if (expLen != actLen) {
        throw TestFailedException(msg+string(" (length)"), expLen, actLen);
      }
      else if (expArray == actArray) {
        // PASS (same location in memory)
      }
      else if ((expArray == NULL) && (actArray == NULL)) {
        // PASS
      }
      else if (expArray == NULL) {
        throw TestFailedException(msg, "null", "{ ... }");
      }
      else if (actArray == NULL) {
        throw TestFailedException(msg, "{ ... }", "null");
      }
      else {
        for (size_t i = 0; i < expLen; i++) {
          if (!_isEqual(expArray[i], actArray[i])) {
            string message = Utilities::format("%s i=%d", msg.c_str(), (int32_t)i);
            throw TestFailedException(message, expArray[i], actArray[i]);
          }
        }
      }
    }

    /** Assert that a given test will have the specified value. This does a
     *  check on two array. Note: <tt>expIdx</tt> and <tt>actIdx</tt> do
     *  not need to match (permitting one to be offset), however <tt>expLen</tt>
     *  and <tt>actLen</tt> must match.
     *  @param msg      String describing the test case.
     *  @param expArray The expected array value.
     *  @param expIdx   The expected array start index.
     *  @param expLen   The expected array length.
     *  @param actArray The actual array value.
     *  @param actIdx   The actual array start index.
     *  @param actLen   The actual array length.
     */
    template <typename T>
    inline void assertArrayEquals (const string &msg, const vector<T> &expArray, size_t expIdx, size_t expLen,
                                                      const vector<T> &actArray, size_t actIdx, size_t actLen) {
      assertArrayEquals(msg, &expArray[expIdx], expLen, &actArray[actIdx], actLen);
    }

    /** Assert that a given test will have the specified value. This does a
     *  check on two array. Note: <tt>expIdx</tt> and <tt>actIdx</tt> do
     *  not need to match (permitting one to be offset), however <tt>expLen</tt>
     *  and <tt>actLen</tt> must match.
     *  @param msg      String describing the test case.
     *  @param expArray The expected array value.
     *  @param expIdx   The expected array start index.
     *  @param expLen   The expected array length.
     *  @param actArray The actual array value.
     *  @param actIdx   The actual array start index.
     *  @param actLen   The actual array length.
     */
    template <typename T>
    inline void assertArrayEquals (const string &msg, const vector<T> &expArray, const vector<T> &actArray) {
      assertArrayEquals(msg, expArray, 0, expArray.size(), actArray, 0, actArray.size());
    }

    /** Assert that a given call will result in an exception being thrown.
     *  @param msg    String describing the test case.
     *  @param func   Name of the function to call via <tt>call(func)</tt>.
     *  @param expMsg The expected error message.
     */
    void assertException (const string &msg, const string &func);

    /** Assert that a given call will result in an exception being thrown with
     *  the specified message.
     *  @param msg    String describing the test case.
     *  @param func   Name of the function to call via <tt>call(func)</tt>.
     *  @param expMsg The expected error message.
     */
    void assertException (const string &msg, const string &func, const string &expMsg);
  } END_NAMESPACE
} END_NAMESPACE
#endif /* _TestRunner_h */
