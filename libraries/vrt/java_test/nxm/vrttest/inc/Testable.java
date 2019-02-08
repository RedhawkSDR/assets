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

package nxm.vrttest.inc;

import java.util.List;

/** Interface defining a test case. The test case must have a no-argument constructor. */
public interface Testable {
  /** This will identify the class being tested. Typical usage:
   *  <pre>
   *    public Class&lt;?&gt; getTestedClass () {
   *      return MyClass.class;
   *    }
   *  </pre>
   *  @return The class being tested.
   */
  public Class<?> getTestedClass ();

  /** This will initialize the test class and will return test sets for all of
   *  of the enclosed test functions. Typical usage:
   *  <pre>
   *    public List&lt;TestSet&gt; init () throws Exception {
   *      List&lt;TestSet&gt; tests = new ArrayList&lt;TestSet&gt;();
   *
   *      tests.add(new TestSet("MyClass.foo(int)",    this, "testFoo_int"));
   *      tests.add(new TestSet("MyClass.foo(String)", this, "testFoo_String"));
   *
   *      return tests;
   *    }
   *  </pre>
   *  @return The list of the test functions to call within this test case.
   *  @throws Exception If there is an exception during initialization.
   */
  public List<TestSet> init () throws Exception;

  /** This will finalize the class subsequent to the tests being run. Typical
   *  usage:
   *  <pre>
   *    public void done () throws Exception {
   *      // nothing to do
   *    }
   *  </pre>
   *  @throws Exception If there is an exception during finalization.
   */
  public void done () throws Exception;
}
