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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.Utilities;
import nxm.vrt.lib.VRTConfig;
import nxm.vrt.net.NetUtilities;
import nxm.vrt.net.NetUtilities.TransportProtocol;


/** Main class for running tests. Normal usage:
 *  <pre>
 *    CharSequence str = TestMain.runTests(new MyTest1(),
 *                                         new MyTest2(),
 *                                         new MyTest3());
 *    System.out.println(str);
 *  </pre>
 */
public class TestRunner {
  //private static final String PLUS_MINUS = "\u00B1";
  private static final String PLUS_MINUS = "+/-";

  private static TestSet thisTest  = null; // the current test set
  private static String  serverURL = null; // the server being used
  private TestRunner () { } // prevent instantiation

  /** Runs all of the given test cases and prints results to console.
   *  @param testCases The test cases to run.
   *  @param verbose   Verbose output on/off.
   *  @return True if the tests PASS, false if they FAIL.
   */
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public static boolean runTests (List<Testable> testCases, boolean verbose) {
    try {
      List<TestSet> tests = new ArrayList<TestSet>(80);
      for (Testable tc : testCases) {
        tests.add(new TestSet(tc));
      }
      thisTest  = new TestSet("", null, tests);
      serverURL = "http://"+VRTConfig.TEST_SERVER;

      Formatter    formatter = new Formatter();
      int          flags     = TestSet.DEF_FLAGS;
      TimeStamp    start     = TimeStamp.getSystemTime();
      boolean      pass      = thisTest.runTest(formatter, flags);
      TimeStamp    end       = TimeStamp.getSystemTime();
      TimeStamp    delta     = end.toGPS().addTime(-start.getGPSSeconds(), -start.getPicoSeconds());
      double       duration  = delta.getGPSSeconds() + (delta.getPicoSeconds() * 1e-12);

      if (verbose) {
        System.out.println(formatter);
        System.out.format("Duration:%n");
        System.out.format("  %.3f sec%n", duration);
        System.out.format("%n");
      }
      return pass;
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to run tests", e);
    }
  }

  /** Gets the default socket options to use; these aren't general options, but
   *  rather are modified to best suit the test cases (which have slightly
   *  different requirements than normal uses).
   *  @param transport Specific transport to use for any transport-specific
   *                   optimizations (can be null).
   *  @return The set of options to use (never null).
   */
  public static Map<String,Object> getTestSocketOptions (TransportProtocol transport) {
    boolean            optimize = true;    // optimize for VITA-49 traffic
    int                bufSize  = 1048576; // 1 MiB
    Map<String,Object> options  = NetUtilities.getDefaultSocketOptions(optimize, transport);

    options.put("RCVBUF_EAGER", true); // always use, even if not default
    options.put("SO_SNDBUF",    bufSize);
    options.put("SO_RCVBUF",    bufSize);

    return options;
  }

  /** Sends a request to the network test server and returns the response. This assumes the response
   *  will be a JSON object.
   *  @param path The URL path on the network server (e.g. "/service")
   *  @param req  The request parameters.
   *  @return The results of the request.
   */
  public static Map<?,?> sendServerRequest (String path, Map<String,?> req) {
    StringBuilder url   = new StringBuilder(80);
    boolean       first = true;

    url.append(serverURL);
    url.append(path);
    for (String key : req.keySet()) {
      Object val = req.get(key);
      if (val != null) {

        if (first) url.append("?");
        else       url.append("&");
        url.append(key).append('=').append(val);
        first = false;
      }
    }

    try {
      URLConnection conn = new URL(url.toString()).openConnection();
      InputStream   in   = conn.getInputStream();
      byte[]        buf  = new byte[conn.getContentLength() + 1024];
      int           len  = 0;

      int numRead = in.read(buf, len, buf.length);
      while (numRead >= 0) {
        len += numRead;
        if (len == buf.length) {
          buf = Arrays.copyOf(buf, buf.length+1024);
        }
        numRead = in.read(buf, len, buf.length-len);
      }

      return (Map<?,?>)JsonUtilities.fromJSON(new String(buf));
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to send request to server using "+url, e);
    }
  }

  /** Is this an object type representing a primitive number? */
  private static boolean isPrimitiveNumber (Number n) {
    return (n instanceof Double) || (n instanceof Float) || (n instanceof Long)
        || (n instanceof Integer) || (n instanceof Short) || (n instanceof Byte);
  }

  /** Are the two values equal? */
  private static <T> boolean isEqual (T exp, T act) {
    if ((exp instanceof Number) && (act instanceof Number)) {
      return isEqual((Number)exp,(Number)act,null);
    }

    return !((exp != act) && ((exp == null) || (act == null) || !exp.equals(act)));
  }

  /** Are the two values equal (or within tolerance)? */
  private static <N extends Number> boolean isEqual (N exp, N act, N tol) {
    if ((exp == act)                  ) return true;
    if ((exp == null) || (act == null)) return false;
    if ((exp.equals(act)             )) return true;
    if ((!isPrimitiveNumber(exp)     )) return false; // use .equals(..) for non-prim for now
    if ((!isPrimitiveNumber(act)     )) return false; // use .equals(..) for non-prim for now

    if ((exp instanceof Double) || (act instanceof Double)) {
      double _exp   = exp.doubleValue();
      double _act   = act.doubleValue();

      if (Double.isNaN(_exp) || Double.isInfinite(_act) ||
          Double.isNaN(_act) || Double.isInfinite(_exp)) {
        return false;
      }
      double _tol   = (tol == null)? 0d : tol.doubleValue();
      double _delta = _act - _exp;
      return !((_delta > _tol) || (_delta < -_tol));
    }
    else if ((exp instanceof Float) || (act instanceof Float)) {
      float _exp   = exp.floatValue();
      float _act   = act.floatValue();

      if (Float.isNaN(_exp) || Float.isInfinite(_act) ||
          Float.isNaN(_act) || Float.isInfinite(_exp)) {
        return false;
      }
      float _tol   = (tol == null)? 0f : tol.floatValue();
      float _delta = _act - _exp;
      return !((_delta > _tol) || (_delta < -_tol));
    }
    else if ((exp instanceof Long) || (act instanceof Long)) {
      long _exp   = exp.longValue();
      long _act   = act.longValue();
      long _tol   = (tol == null)? 0L : tol.longValue();
      long _delta = _act - _exp;
      return !((_delta > _tol) || (_delta < -_tol));
    }
    else {
      int _exp   = exp.intValue();
      int _act   = act.intValue();
      int _tol   = (tol == null)? 0 : tol.intValue();
      int _delta = _act - _exp;
      return !((_delta > _tol) || (_delta < -_tol));
    }
  }

  /** Assert that a given test will have the specified value. If both <tt>exp</tt>
   *  and <tt>act</tt> are numbers, this will call
   *  <tt>assertEquals(msg,(Number)exp,(Number)act,(Number)null)</tt>. <br>
   *  <br>
   *  <i>Implementation Note: At one time there was a separate
   *  <tt>&lt;N extends Number&gt; assertEquals(CharSequence,N,N)</tt> function,
   *  however it caused issues under Java 1.6.0 (seen under 64-bit Mac OS X
   *  10.6.8) due to Java Bug 7085024.</i>
   *  @param <T>  The object type.
   *  @param msg  String describing the test case.
   *  @param exp  The expected value.
   *  @param act  The actual value.
   */
  public static <T> void assertEquals (CharSequence msg, T exp, T act) {
    thisTest.countTest();
    if (!isEqual(exp, act)) {
      throw new TestFailedException(msg, exp, act);
    }
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
   *  @param <N>  The object type.
   *  @param msg  String describing the test case.
   *  @param exp  The expected value.
   *  @param act  The actual value.
   *  @param tol  The absolute tolerance permitted (inclusive). Passing in null
   *              is the same as passing in zero.
   */
  public static <N extends Number> void assertEquals (CharSequence msg, N exp, N act, N tol) {
    thisTest.countTest();
    if (!isEqual(exp, act, tol)) {
      throw new TestFailedException(msg, exp, act, tol);
    }
  }

  /** Assert that a given test will have the specified value. This does a
   *  pointer check rather than a value check.
   *  @param <T>  The object type.
   *  @param msg  String describing the test case.
   *  @param exp  The expected value.
   *  @param act  The actual value.
   */
  public static <T> void assertSame (CharSequence msg, T exp, T act) {
    thisTest.countTest();
    if (exp != act) {
      throw new TestFailedException(msg, exp, act);
    }
  }

  /** Assert that a given test will have type that is equal to, or a subclass of
   *  the expected type.
   *  @param msg  String describing the test case.
   *  @param exp  The expected type.
   *  @param act  The actual type.
   */
  public static void assertAssignableFrom (CharSequence msg, Class<?> exp, Class<?> act) {
    thisTest.countTest();
    if ((act == null) || !exp.isAssignableFrom(act)) {
      throw new TestFailedException(msg, exp, act);
    }
  }

  /** Assert that a given test will have type that is equal to, or a subclass of
   *  the expected type.
   *  @param msg  String describing the test case.
   *  @param exp  The expected type.
   *  @param act  The actual object.
   */
  public static void assertInstanceOf (CharSequence msg, Class<?> exp, Object act) {
    thisTest.countTest();
    if ((act == null) || !exp.isInstance(act)) {
      throw new TestFailedException(msg, exp, act);
    }
  }

  /** Assert that a given test will have the specified value. This does a
   *  check on two buffers.
   *  @param msg    String describing the test case.
   *  @param expBuf The expected buffer value.
   *  @param actBuf The actual buffer value.
   */
  public static void assertBufEquals (CharSequence msg, byte[] expBuf, byte[] actBuf) {
    assertBufEquals(msg, expBuf, 0, expBuf.length, actBuf, 0, actBuf.length);
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
  public static void assertBufEquals (CharSequence msg, byte[] expBuf, int expIdx, int expLen,
                                                        byte[] actBuf, int actIdx, int actLen) {
    thisTest.countTest();
    if (Arrays.equals(expBuf, actBuf)) {
      // PASS (use the fast array check first -- it is usually an intrinsic in the Java VM)
    }
    else if ((expBuf == null) && (actBuf == null)) {
      // PASS
    }
    else if (expBuf == null) {
      throw new TestFailedException(msg, "null", "{ ... }");
    }
    else if (actBuf == null) {
      throw new TestFailedException(msg, "{ ... }", "null");
    }
    else if (expLen != actLen) {
      throw new TestFailedException(msg+" (length)", expLen, actLen);
    }
    else if ((expBuf == actBuf) && (expIdx == actIdx)) {
      // PASS (same location in memory)
    }
    else {
      for (int i = 0; i < expLen; i++) {
        if (expBuf[i+expIdx] != actBuf[i+actIdx]) {
          int e = 0xFF & expBuf[i+expIdx];
          int a = 0xFF & actBuf[i+actIdx];
          throw new TestFailedException(msg+" (i="+i+")", "0x"+Utilities.toHexString(e,1),
                                                          "0x"+Utilities.toHexString(a,1));
        }
      }
    }
  }

  /** Assert that a given test will have the specified value. This does a
   *  check on two array.
   *  @param msg      String describing the test case.
   *  @param expArray The expected array value.
   *  @param actArray The actual array value.
   */
  public static void assertArrayEquals (CharSequence msg, Object expArray, Object actArray) {
    assertArrayEquals(msg, expArray, 0, ((expArray == null)? 0 : Array.getLength(expArray)),
                           actArray, 0, ((actArray == null)? 0 : Array.getLength(actArray)));
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
  public static void assertArrayEquals (CharSequence msg, Object expArray, int expIdx, int expLen,
                                                          Object actArray, int actIdx, int actLen) {
    thisTest.countTest();

    if (expLen != actLen) {
      throw new TestFailedException(msg+" (length)", expLen, actLen);
    }
    else if ((expArray == actArray) && (expIdx == actIdx)) {
      // PASS (same location in memory)
    }
    else if ((expArray instanceof byte[]) && (actArray instanceof byte[])) {
      byte[] _expArray = (byte[])expArray;
      byte[] _actArray = (byte[])actArray;

      for (int i = 0; i < expLen; i++,expIdx++,actIdx++) {
        if (_expArray[expIdx] != _actArray[actIdx]) {
          throw new TestFailedException(""+msg+" i="+i, _expArray[expIdx], _actArray[actIdx]);
        }
      }
    }
    else if ((expArray instanceof short[]) && (actArray instanceof short[])) {
      short[] _expArray = (short[])expArray;
      short[] _actArray = (short[])actArray;

      for (int i = 0; i < expLen; i++,expIdx++,actIdx++) {
        if (_expArray[expIdx] != _actArray[actIdx]) {
          throw new TestFailedException(""+msg+" i="+i, _expArray[expIdx], _actArray[actIdx]);
        }
      }
    }
    else if ((expArray instanceof int[]) && (actArray instanceof int[])) {
      int[] _expArray = (int[])expArray;
      int[] _actArray = (int[])actArray;

      for (int i = 0; i < expLen; i++,expIdx++,actIdx++) {
        if (_expArray[expIdx] != _actArray[actIdx]) {
          throw new TestFailedException(""+msg+" i="+i, _expArray[expIdx], _actArray[actIdx]);
        }
      }
    }
    else if ((expArray instanceof long[]) && (actArray instanceof long[])) {
      long[] _expArray = (long[])expArray;
      long[] _actArray = (long[])actArray;

      for (int i = 0; i < expLen; i++,expIdx++,actIdx++) {
        if (_expArray[expIdx] != _actArray[actIdx]) {
          throw new TestFailedException(""+msg+" i="+i, _expArray[expIdx], _actArray[actIdx]);
        }
      }
    }
    else if ((expArray instanceof float[]) && (actArray instanceof float[])) {
      float[] _expArray = (float[])expArray;
      float[] _actArray = (float[])actArray;

      for (int i = 0; i < expLen; i++,expIdx++,actIdx++) {
        if (_expArray[expIdx] != _actArray[actIdx]) {
          if (Float.isNaN(_expArray[expIdx]) && Float.isNaN(_actArray[i])) {
            // OK
          }
          else {
            throw new TestFailedException(""+msg+" i="+i, _expArray[expIdx], _actArray[i]);
          }
        }
      }
    }
    else if ((expArray instanceof double[]) && (actArray instanceof double[])) {
      double[] _expArray = (double[])expArray;
      double[] _actArray = (double[])actArray;

      for (int i = 0; i < expLen; i++,expIdx++,actIdx++) {
        if (_expArray[expIdx] != _actArray[actIdx]) {
          if (Double.isNaN(_expArray[expIdx]) && Double.isNaN(_actArray[actIdx])) {
            // OK
          }
          else {
            throw new TestFailedException(""+msg+" i="+i, _expArray[expIdx], _actArray[actIdx]);
          }
        }
      }
    }
    else if ((expArray == null) && (actArray == null)) {
      // PASS
    }
    else if (expArray == null) {
      throw new TestFailedException(msg, "null", "{ ... }");
    }
    else if (actArray == null) {
      throw new TestFailedException(msg, "{ ... }", "null");
    }
    else {
      // DEFAULT TEST
      for (int i = 0; i < expLen; i++) {
        Object exp = Array.get(expArray, i+expIdx);
        Object act = Array.get(actArray, i+actIdx);

        if (!isEqual(exp, act)) {
          throw new TestFailedException(""+msg+" i="+i, exp, act);
        }
      }
    }
  }

  /** Quick version with faster array check. */
  public static void assertArrayEquals (CharSequence msg, byte[] expArray, int expIdx, int expLen,
                                                          byte[] actArray, int actIdx, int actLen) {
    if ((expIdx == 0) && (actIdx == 0) && Arrays.equals(expArray, actArray)) {
      thisTest.countTest();
    }
    else {
      assertArrayEquals(msg, (Object)expArray, expIdx, expLen, (Object)actArray, actIdx, actLen);
    }
  }

  /** Quick version with faster array check. */
  public static void assertArrayEquals (CharSequence msg, short[] expArray, int expIdx, int expLen,
                                                          short[] actArray, int actIdx, int actLen) {
    if ((expIdx == 0) && (actIdx == 0) && Arrays.equals(expArray, actArray)) {
      thisTest.countTest();
    }
    else {
      assertArrayEquals(msg, (Object)expArray, expIdx, expLen, (Object)actArray, actIdx, actLen);
    }
  }

  /** Quick version with faster array check. */
  public static void assertArrayEquals (CharSequence msg, int[] expArray, int expIdx, int expLen,
                                                          int[] actArray, int actIdx, int actLen) {
    if ((expIdx == 0) && (actIdx == 0) && Arrays.equals(expArray, actArray)) {
      thisTest.countTest();
    }
    else {
      assertArrayEquals(msg, (Object)expArray, expIdx, expLen, (Object)actArray, actIdx, actLen);
    }
  }

  /** Quick version with faster array check. */
  public static void assertArrayEquals (CharSequence msg, long[] expArray, int expIdx, int expLen,
                                                          long[] actArray, int actIdx, int actLen) {
    if ((expIdx == 0) && (actIdx == 0) && Arrays.equals(expArray, actArray)) {
      thisTest.countTest();
    }
    else {
      assertArrayEquals(msg, (Object)expArray, expIdx, expLen, (Object)actArray, actIdx, actLen);
    }
  }

  /** Assert that a given call will result in an exception being thrown.
   *  @param msg    String describing the test case.
   *  @param func   Name of the function to call via reflection.
   */
  public static void assertException (CharSequence msg, String func) {
    thisTest.countTest();
    try {
      thisTest.call(func);
    }
    catch (Throwable t) {
      return; // PASS
    }
    throw new TestFailedException(msg, "<exception>", "<no exception>");
  }

  /** Assert that a given call will result in an exception being thrown with
   *  the specified message.
   *  @param msg    String describing the test case.
   *  @param func   Name of the function to call via reflection.
   *  @param expMsg The expected error message.
   */
  public static void assertException (CharSequence msg, String func, String expMsg) {
    thisTest.countTest();
    try {
      thisTest.call(func);
    }
    catch (InvocationTargetException t) {
      String actMsg = String.valueOf(t.getMessage());            // <-- Example: NPE has message=null
      String altMsg = String.valueOf(t.getCause().getMessage()); // <-- Example: NPE has message=null

      if (expMsg.equals(actMsg) || expMsg.equals(altMsg)) {
        return; // PASS
      }
      else {
        throw new TestFailedException(msg, expMsg, t.getMessage());
      }
    }
    catch (Throwable t) {
      String actMsg = String.valueOf(t.getMessage());            // <-- Example: NPE has message=null
      if (expMsg.equals(actMsg)) {
        return; // PASS
      }
      else {
        throw new TestFailedException(msg, expMsg, t.getMessage());
      }
    }
    throw new TestFailedException(msg, "<exception>", "<no exception>");
  }

  /** Assert that a given call will result in an exception being thrown with
   *  the specified message.
   *  @param msg     String describing the test case.
   *  @param func    Name of the function to call via reflection.
   *  @param expType The expected error type.
   */
  public static void assertException (CharSequence msg, String func, Class<? extends Throwable> expType) {
    thisTest.countTest();
    try {
      thisTest.call(func);
    }
    catch (InvocationTargetException t) {
      if (expType.isAssignableFrom(t.getClass()) || expType.isAssignableFrom(t.getCause().getClass())) {
        return; // PASS
      }
      else {
        throw new TestFailedException(msg, expType.getName(), t.getCause().getClass().getName());
      }
    }
    catch (Throwable t) {
      if (expType.isAssignableFrom(t.getClass())) {
        return; // PASS
      }
      else {
        throw new TestFailedException(msg, expType.getName(), t.getClass().getName());
      }
    }
    throw new TestFailedException(msg, "<exception>", "<no exception>");
  }

  /** Converts a value to a string with special handling for null, strings, and
   *  empty values.
   */
  private static String valToString (Object val) {
    if (val == null) return "<null>";
    if (val instanceof CharSequence) return "\"" + val + "\"";

    String str = val.toString();
    return (str.isEmpty())? "<empty>" : str;
  }

  /** Formats an error message.
   *  @param msg  String describing the test case.
   *  @param exp  The expected value.
   *  @param act  The actual value.
   */
  private static <T> String formatError (CharSequence msg, T exp, T act) {
    Formatter f      = new Formatter();
    String    expStr = valToString(exp);
    String    actStr = valToString(act);

    if ((msg == null) || (msg.length() == 0)) {
      msg = "???";
    }

    f.format("Testing %s expected %s but got %s", msg, expStr, actStr);
    return f.toString();
  }

  /** Formats a numeric error message.
   *  @param msg  String describing the test case.
   *  @param exp  The expected value.
   *  @param act  The actual value.
   *  @param tol  The absolute tolerance permitted (inclusive).
   */
  private static <N extends Number> String formatError (CharSequence msg, N exp, N act, N tol) {
    Formatter f      = new Formatter();
    String    expStr = valToString(exp);
    String    actStr = valToString(act);

    if ((msg == null) || (msg.length() == 0)) {
      msg = "???";
    }

    if (tol == null) f.format("Testing %s expected %s but got %s",      msg, expStr,                  actStr);
    else             f.format("Testing %s expected %s %s%s but got %s", msg, expStr, PLUS_MINUS, tol, actStr);
    return f.toString();
  }


  //////////////////////////////////////////////////////////////////////////////
  // NESTED CLASSES
  //////////////////////////////////////////////////////////////////////////////

  /** Exception thrown when a test case fails. */
  public static class TestFailedException extends RuntimeException {
    private static final long serialVersionUID = 0x805f31e55ee7460aL;

    /** Creates a new instance.
     *  @param msg  String describing the test case.
     *  @param exp  The expected value.
     *  @param act  The actual value.
     */
    <T> TestFailedException (CharSequence msg, T exp, T act) {
      super(formatError(msg,exp,act));
      fixStackTrace();
    }

    /** Creates a new instance.
     *  @param msg  String describing the test case.
     *  @param exp  The expected value.
     *  @param act  The actual value.
     *  @param tol  The absolute tolerance permitted (inclusive).
     */
    <N extends Number> TestFailedException (CharSequence msg, N exp, N act, N tol) {
      super(formatError(msg,exp,act,tol));
      fixStackTrace();
    }

    @Override
    public String toString () {
      return "TestFailedException: " + getMessage();
    }

    /** Strip off errors within TestMain so the top shows the line in the test
     *  case rather than the internal assertion function.
     */
    private void fixStackTrace () {
      try {
        StackTraceElement[] stack  = getStackTrace();
        int                 start  = 0;
        String              ignore = TestRunner.class.getName();

        while (start < stack.length) {
          if (!ignore.equals(stack[start].getClassName())) break;
          start++;
        }
        stack = Arrays.copyOfRange(stack, start, stack.length);
        setStackTrace(stack);
      }
      catch (Throwable t) {
        // ignore
      }
    }
  }
}
