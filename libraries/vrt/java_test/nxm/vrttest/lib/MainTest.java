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

package nxm.vrttest.lib;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import nxm.vrt.lib.VRTConfig;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;

import static nxm.vrttest.inc.TestRunner.assertEquals;

/** Tests for the {@link Main} class. This assumes that 'pkt_src/AppendixA.xml'
 *  and has not been modified recently. Modifications to 'pkt_src/AppendixA.xml'
 *  may require the test cases to be updated.
 */
public class MainTest<T extends nxm.vrt.lib.Main> implements Testable {

  private final Class<T> testedClass;

  /** Creates a new instance using the given implementation class. */
  public MainTest (Class<T> testedClass) {
    this.testedClass = testedClass;
  }

  @Override
  public Class<?> getTestedClass () {
    return testedClass;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    tests.add(new TestSet("main(..)",  this, "testMain"));

    return tests;
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }

  @Override
  public String toString () {
    return "Tests for Main";
  }

  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public void testMain () throws Exception {
    PrintStream _out = System.out;
    PrintStream _err = System.err;

    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
      ByteArrayOutputStream err = new ByteArrayOutputStream(4096);

      System.setOut(new PrintStream(out));
      System.setErr(new PrintStream(err));

      nxm.vrt.lib.Main.main();
      assertEquals("Main with no arguments prints usage to System.out", true,  (out.size() > 0));
      assertEquals("Main with no arguments prints usage to System.err", false, (err.size() > 0));


      String version = "VRT Java Library "+VRTConfig.LIBRARY_VERSION;
      String usage   = out.toString();
      String error   = "";

      out.reset();
      err.reset();
      nxm.vrt.lib.Main.main("-H");
      assertEquals("Main with -H prints usage to System.out", usage, out.toString());
      assertEquals("Main with -H prints usage to System.err", error, err.toString());

      out.reset();
      err.reset();
      nxm.vrt.lib.Main.main("--help");
      assertEquals("Main with --help prints usage to System.out", usage, out.toString());
      assertEquals("Main with --help prints usage to System.err", error, err.toString());

      out.reset();
      err.reset();
      nxm.vrt.lib.Main.main("-V");
      assertEquals("Main with -V prints version to System.out", version, out.toString().trim());
      assertEquals("Main with -V prints version to System.err", error,   err.toString().trim());

      out.reset();
      err.reset();
      nxm.vrt.lib.Main.main("--version");
      assertEquals("Main with --version prints version to System.out", version, out.toString().trim());
      assertEquals("Main with --version prints version to System.err", error,   err.toString().trim());

      out.reset();
      err.reset();
      nxm.vrt.lib.Main.main("--dumpcshscript");
      String firstLine = "#!/bin/csh" + System.getProperty("line.separator", "\n");
      assertEquals("Main with --dumpcshscript prints script to System.out", true,  out.toString().startsWith(firstLine));
      assertEquals("Main with --dumpcshscript prints script to System.err", error, err.toString());
    }
    finally {
      System.setOut(_out);
      System.setErr(_err);
    }
  }
}
