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

#ifndef _TestSet_h
#define _TestSet_h

#include "VRTObject.h"
#include <vector>
#include <algorithm>

using namespace std;
using namespace vrt;

namespace vrttest {
  class Testable;

  /** Test count type. */
  enum CountType {
    CountType_ClassesTested,   CountType_ClassesFailed,
    CountType_FunctionsTested, CountType_FunctionsFailed, CountType_FunctionsSkipped, CountType_FunctionsMissing,
    CountType_AllTested,       CountType_AllFailed,       CountType_AllSkipped
  };

  /** Describes a test set. */
  class TestSet : public VRTObject {
    /** Show all errors. */
    public: static const int32_t SHOW_ERRORS  = 0x0001;
    /** Show stack trace for all errors (n/a unless {@link #SHOW_ERRORS} is set). */
    public: static const int32_t SHOW_STACK   = 0x0002;
    /** Show overall test summary. */
    public: static const int32_t SHOW_SUMMARY = 0x0004;
    /** Show all errors. */
    public: static const int32_t DEF_FLAGS    = SHOW_ERRORS|SHOW_STACK|SHOW_SUMMARY;


    //TODO: write accessor for descr, (used in TestRunner.h compDescr(..) function)
    public : string          descr;         // Description of the test
    private: Testable       *obj;           // The Testable object
    private: vector<TestSet> tests;         // Sub-tests to run
    private: string          funcName;      // Function name (""   if tests!=null)
    private: int64_t         testCount;     // Test count    (n/a  if tests!=null)
    private: int64_t         skipCount;     // Skip count    (n/a  if tests!=null)
    private: VRTException    errorCaught;   // Error caught  (n/a  if tests!=null)
    private: TestSet        *running;       // Current test  (null if tests==null)

    /** Empty constructor. */
    public: TestSet ();

    /** Copy constructor. */
    public: TestSet (const TestSet &ts);

    /** Creates a new test set that will execute the given function. If used with
     *  test functions that follow JUnit conventions this will be called as:
     *  <pre>
     *    new TestSet("MyClass.foo", this, "testFoo");
     *  </pre>
     *  However, if tests for various functions are split up based on signature,
     *  this may be used as:
     *  <pre>
     *    new TestSet("MyClass.foo(int)",    this, "testFoo_int");
     *    new TestSet("MyClass.foo(String)", this, "testFoo_String");
     *  </pre>
     *  <b>Skipping a test:</b><br>
     *  <br>
     *  If tested elsewhere, the test can be skipped by putting the name of the
     *  function it is tested in prefixed by a "+". This is commonly used with
     *  set/get functions, for example:
     *  <pre>
     *    new TestSet("MyClass.setFoo(int)",  this, "testSetFoo");
     *    new TestSet("MyClass.getFoo()",     this, "+testSetFoo");
     *  </pre>
     *  If a test is to be skipped for some reason (known bug, platform-specific,
     *  etc.), the test can be skipped by putting the name of the function it is
     *  tested in prefixed by a "-" or simply by listing the name as "-". For
     *  example:
     *  <pre>
     *    new TestSet("MyClass.dump()",   this, "-");        // skip debug function
     *    new TestSet("MyClass.bar()",    this, "+testBar"); // skip known bug
     *  </pre>
     *  @param descr A description of the test.
     *  @param obj   The test case object.
     *  @param func  The name of the function to call.
     */
    public: TestSet (const string &descr, Testable *obj, const string &func);

    /** <b>Internal Use Only:</b> Creates a new test set that will execute the
     *  functions in the given {@link Testable} object.
     *  @param obj The test case object.
     */
    public: TestSet (Testable *obj);

    /** <b>Internal Use Only:</b> Creates a new test set that will execute the
     *  functions in the given {@link Testable} object and/or run the given set
     *  of tests.
     */
    public: TestSet (const string &descr, Testable *obj, const vector<TestSet> &tests);

    /** Converts to string form. */
    public: inline string toString () const {
      return descr;
    }

    /** Is this a null value. */
    public: inline bool isNullValue () const {
      return (descr        == ""  )
          && (obj          == NULL)
          && (tests.size() == 0   )
          && (funcName     == ""  )
          && (testCount    == __INT64_C(0))
          && (skipCount    == __INT64_C(0))
          && (running      == NULL);
    }

    /** Is this a skip function? Supported prefix values:
     *  <pre>
     *    '+'   - Tested in other test function
     *    '-'   - Skip tests (e.g. known bug)
     *    '!'   - Missing test function (internal use only)
     *  </pre>
     */
    private: static inline bool isSkipFunc (const string &func) {
      return (func.length() > 0) && ((func[0] == '+') || (func[0] == '-') || (func[0] == '!'));
    }

    /** Counts the current test. */
    public: inline void countTest () {
      if (running != NULL) running->countTest();
      else                 testCount++;
    }

    /** Counts a skipped test. */
    public: inline void countSkip () {
      if (running != NULL) running->countSkip();
      else                 skipCount++;
    }

    /** <b>Internal Use Only:</b> Calls the given function. */
    public: void call (const string &func) const;

    /** Runs the test case, throwing exception if test fails. */
    public: bool runTest (string &s, int32_t flags);

    /** Counts the number of test cases of the given type. */
    private: int64_t getTestCount (CountType type);
  };
}

#endif /* _TestSet_h */
