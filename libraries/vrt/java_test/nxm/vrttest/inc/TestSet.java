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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/** Describes a test set. */
public final class TestSet implements Comparable<TestSet> {
  /** Show all errors. */
  public static final int SHOW_ERRORS  = 0x0001;
  /** Show stack trace for all errors (n/a unless {@link #SHOW_ERRORS} is set). */
  public static final int SHOW_STACK   = 0x0002;
  /** Show overall test summary. */
  public static final int SHOW_SUMMARY = 0x0004;
  /** Show all errors. */
  public static final int DEF_FLAGS    = SHOW_ERRORS|SHOW_STACK|SHOW_SUMMARY;

  /** Test count type. */
  private enum CountType {
    ClassesTested,   ClassesFailed,
    FunctionsTested, FunctionsFailed, FunctionsSkipped, FunctionsMissing,
    AllTested,       AllFailed,       AllSkipped
  };

  private final String        descr;         // Description of the test
  private final String        tested;        // Tested function (just a guess)
  private final Testable      obj;           // The Testable object
  private final List<TestSet> tests;         // Sub-tests to run
  private final String        funcName;      // Function name (null if tests!=null)
  private final Method        funcCall;      // Function call (null if tests!=null)
  private       long          testCount;     // Test count    (n/a  if tests!=null)
  private       long          skipCount;     // Skip count    (n/a  if tests!=null)
  private       Throwable     errorCaught;   // Error caught  (n/a  if tests!=null)
  private       TestSet       running;       // Current test  (null if tests==null)

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
   *  @throws Exception If there is any error in setting up the test set.
   */
  public TestSet (String descr, Testable obj, String func) throws Exception {
    Method _funcCall = null;
    if ((obj != null) && !isSkipFunc(func)) {
      _funcCall = obj.getClass().getMethod(func);
    }
    this.descr    = descr;
    this.tested   = guessTestedFunc(descr, obj, func);
    this.obj      = obj;
    this.tests    = null;
    this.funcName = func;
    this.funcCall = _funcCall;
  }

  /** <b>Internal Use Only:</b> Creates a new test set that will execute the
   *  functions in the given {@link Testable} object.
   *  @param obj The test case object.
   */
  TestSet (Testable obj) throws Exception {
    this((obj.getTestedClass() == null)? obj.toString() : "Tests for "+obj.getTestedClass().getName(),
         obj,
         obj.init());
    addMissingTests();
    Collections.sort(tests);
  }

  /** <b>Internal Use Only:</b> Creates a new test set that will execute the
   *  functions in the given {@link Testable} object and/or run the given set
   *  of tests.
   */
  TestSet (String descr, Testable obj, List<TestSet> tests) throws Exception {
    this.descr    = descr;
    this.tested   = null;
    this.obj      = obj;
    this.tests    = tests;
    this.funcName = null;
    this.funcCall = null;
  }

  @Override
  public String toString () {
    return descr;
  }

  @Override
  public int compareTo (TestSet that) {
    return toString().compareTo(that.toString());
  }

  /** Is this a skip function? Supported prefix values:
   *  <pre>
   *    '+'   - Tested in other test function
   *    '-'   - Skip tests (e.g. known bug)
   *    '!'   - Missing test function (internal use only)
   *  </pre>
   */
  private static boolean isSkipFunc (String func) {
    return func.startsWith("+") || func.startsWith("-") || func.startsWith("!");
  }

  /** Guesses the name of the function being tested. */
  private static String guessTestedFunc (String descr, Testable obj, String func) {
    // GUESS 1: (check description with entry (e.g. "setFoo(..)" -> "setFoo"))
    String guess1;

    if (descr == null) {
      guess1 = "";
    }
    else {
      int idx = descr.indexOf('(');
      guess1 = (idx <= 0)? descr : descr.substring(0,idx).trim();
    }

    // GUESS 1: (strip +/- then check for JUnit-style name (e.g. "testGetFoo" -> "getFoo"))
    String guess2 = func;
    if ((guess2.length() > 1) && isSkipFunc(guess2)) {
      guess2 = guess2.substring(1);
    }
    if ((guess2.length() >= 5) && guess2.startsWith("test")) {
      guess2 = guess2.substring(4,5).toLowerCase() + guess2.substring(5);
    }

    // GUESS 2: (check for function name with type specifier (e.g. "setFoo_int" -> "setFoo"))
    String guess3 = guess2;
    int    index  = guess3.indexOf('_');
    if (index > 0) {
      guess3 = func.substring(0,index);
    }

    if ((obj != null) && (obj.getTestedClass() != null)) {
      for (Method m : obj.getTestedClass().getMethods()) {
        if (m.getName().equalsIgnoreCase(guess1)) return m.getName();
      }
      for (Method m : obj.getTestedClass().getMethods()) {
        if (m.getName().equalsIgnoreCase(guess2)) return m.getName();
      }
      for (Method m : obj.getTestedClass().getMethods()) {
        if (m.getName().equalsIgnoreCase(guess3)) return m.getName();
      }
    }
    if (guess1.equalsIgnoreCase(guess2)) return guess1; // likely guess
    if (guess1.equalsIgnoreCase(guess3)) return guess1; // likely guess
    if (guess2.equalsIgnoreCase(guess3)) return guess2; // likely guess

    return "<unknown>";
  }

  /** Tries to identify any missing test cases. */
  private void addMissingTests () throws Exception {
    if (obj.getTestedClass() == null) return;

    for (Method m : obj.getTestedClass().getDeclaredMethods()) {
      if (!Modifier.isPublic(m.getModifiers())) continue;
      if (Modifier.isAbstract(m.getModifiers())) continue;
      String  name = m.getName();
      boolean ok   = false;

      for (TestSet ts : tests) {
        if (name.equals(ts.tested)) {
          ok = true;
          break;
        }
      }

      if (!ok) {
        tests.add(new TestSet(m.getName()+"(..)", null, "!test"+m.getName()));
      }
    }
  }

  /** Counts the current test. */
  public void countTest () {
    if (running != null) running.countTest();
    else                 testCount++;
  }

  /** Counts a skipped test. */
  public void countSkip () {
    if (running != null) running.countSkip();
    else                 skipCount++;
  }

  /** <b>Internal Use Only:</b> Calls the given function. */
  void call (String func) throws Exception {
    if (running != null) {
      running.call(func);
    }
    else {
      Method m = obj.getClass().getMethod(func);
      m.invoke(obj);
    }
  }

  /** Runs the test case, throwing exception if test fails.
   *  @param flags The flags for running the test case.
   *  @return The output of running the test case.
   *  @throws Exception If there is a major failure in running the test case (i.e. not a normal
   *                    failed test).
   */
  public CharSequence runTest (int flags) throws Exception {
    Formatter f = new Formatter();
    runTest(f, flags);
    return f.toString();
  }

  /** Runs the test case, throwing exception if test fails. */
  boolean runTest (Formatter f, int flags) throws Exception {
    if (funcName != null) {
      char skip = ' ';
      try {
        if (isSkipFunc(funcName)) {
          skip = funcName.charAt(0);
        }
        else {
          funcCall.invoke(obj);
        }
      }
      catch (InvocationTargetException t) {
        errorCaught = t.getCause();
      }
      catch (Throwable t) {
        errorCaught = t;
      }

      if (skip == ' ') {
        f.format("  %-48s (attempted %8d, failed %2d, skipped %2d) %s%n",
                 descr,
                 testCount,
                 ((errorCaught == null)?      0 :      1),
                 skipCount,
                 ((errorCaught == null)? "PASS" : "FAIL"));
      }
      else if (skip == '+') {
        f.format("  %-48s (attempted        N, failed  0, skipped  0) PASS%n", descr);
      }
      else if (skip == '-') {
        f.format("  %-48s (attempted        -, failed  -, skipped  -) SKIP%n", descr);
      }
      else if (skip == '!') {
        f.format("  %-48s (attempted        -, failed  -, skipped  -) MISS%n", descr);
      }
      else { // skip
        throw new AssertionError("Unsupported skip='"+skip+"'");
      }

      if ((errorCaught != null) && ((flags & SHOW_ERRORS) != 0)) {
        if ((flags & SHOW_STACK) != 0) {
          formatStackTrace(f, errorCaught);
        }
        else {
          f.format("    %s%n", errorCaught.getMessage());
        }
      }
      return (errorCaught == null);
    }
    else {
      int subFlags = flags & ~SHOW_SUMMARY;
      f.format("%-50s%n", descr);
      for (TestSet ts : tests) {
        running = ts;
        ts.runTest(f, subFlags);
      }
      running = null;
      if ((tests != null) && (obj != null)) {
        obj.done();
      }

      if ((flags & SHOW_SUMMARY) != 0) {
        long ct = getTestCount(CountType.ClassesTested);
        long cf = getTestCount(CountType.ClassesFailed);
        long ft = getTestCount(CountType.FunctionsTested);
        long ff = getTestCount(CountType.FunctionsFailed);
        long fs = getTestCount(CountType.FunctionsSkipped);
        long fm = getTestCount(CountType.FunctionsMissing);
        long tt = getTestCount(CountType.AllTested);
        long tf = getTestCount(CountType.AllFailed);
        long ts = getTestCount(CountType.AllSkipped);

        f.format("%n");
        f.format("%n");
        f.format("Summary:%n");
        f.format("             |           Tested      Failed     Skipped     Missing%n");
        f.format("  -----------+-----------------------------------------------------%n");
        f.format("  Classes    | %,16d  %,10d  %,10d  %10s%n",  ct, cf, 0, "n/a");
        f.format("  Functions  | %,16d  %,10d  %,10d  %,10d%n", ft, ff, fs, fm);
        f.format("  Test Cases | %,16d  %,10d  %,10d  %10s%n",  tt, tf, ts, "n/a");
        f.format("  %n");
        f.format("Overall Status:%n");
        f.format("  **%s**%n", ((tf == 0)? "PASS" : "FAIL"));
        return (tf == 0);
      }
      else {
        long tf = getTestCount(CountType.AllFailed);
        return (tf == 0);
      }
    }
  }

  /** Counts the number of test cases of the given type. */
  private long getTestCount (CountType type) {
    if (funcName != null) {
      int skip = (isSkipFunc(funcName) && (funcName.charAt(0) == '-'))? 1 : 0;
      int miss = (isSkipFunc(funcName) && (funcName.charAt(0) == '!'))? 1 : 0;

      switch (type) {
        case FunctionsTested:  return 1-skip-miss;
        case FunctionsFailed:  return (errorCaught != null)? 1 : 0;
        case FunctionsSkipped: return skip;
        case FunctionsMissing: return miss;
        case AllTested:        return testCount;
        case AllFailed:        return (errorCaught != null)? 1 : 0;
        case AllSkipped:       return skipCount;
        default:               break; // handled below
      }
    }
    else if (obj != null) {
      switch (type) {
        case ClassesTested:  return 1;
        case ClassesFailed:  return (getTestCount(CountType.FunctionsFailed) > 0)? 1 : 0;
        default:             break; // handled below
      }
    }

    // Total sub-tests
    long total = 0;
    for (TestSet ts : tests) {
      total += ts.getTestCount(type);
    }
    return total;
  }

  /** Formats a stack trace. */
  private static void formatStackTrace (Formatter f, Throwable t) {
    f.format("    %s%n", t);
    for (StackTraceElement st : t.getStackTrace()) {
      f.format("        at %s%n", st);
    }

    while ((t = t.getCause()) != null) {
      f.format("      Caused by: %s%n", t);
      for (StackTraceElement st : t.getStackTrace()) {
        f.format("        at %s%n", st);
      }
    }
  }
}
