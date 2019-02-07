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

#ifndef _Testable_h
#define _Testable_h

#include "TestSet.h"
#include <vector>

namespace vrttest {
  /** Interface defining a test case. The test case must have a no-argument
   *  constructor.
   */
  class Testable {
    /** Default constructor. */
    public: Testable () { }

    /** Default destructor. */
    public: virtual ~Testable () { }

    /** This will identify the class being tested. Typical usage:
     *  <pre>
     *    string getTestedClass () {
     *      return "vrt::MyClass";
     *    }
     *  </pre>
     *  @return The name of the class being tested.
     */
    public: virtual string getTestedClass () = 0;

    /** This will initialize the test class and will return test sets for all of
     *  of the enclosed test functions. Typical usage:
     *  <pre>
     *    vector&lt;TestSet&gt; init () {
     *      vector&lt;TestSet&gt; tests;
     *
     *      tests.push_back(TestSet("MyClass.foo(int)",    this, &testFoo_int));
     *      tests.push_back(TestSet("MyClass.foo(String)", this, &testFoo_String));
     *
     *      return tests;
     *    }
     *  </pre>
     *  @return The list of the test functions to call within this test case.
     */
    public: virtual vector<TestSet> init () = 0;

    /** This will finalize the class subsequent to the tests being run. Typical
     *  usage:
     *  <pre>
     *    void done () {
     *      // nothing to do
     *    }
     *  </pre>
     */
    public: virtual void done () = 0;

    /** This will call the named function. Typical usage:
     *  <pre>
     *    void call (const string &func) {
     *      if (func == "testFoo_int"   ) testFoo_int();
     *      if (func == "testFoo_String") testFoo_String();
     *    }
     *  </pre>
     */
    public: virtual void call (const string &func) = 0;
  };
} END_NAMESPACE
#endif /* _Testable_h */
