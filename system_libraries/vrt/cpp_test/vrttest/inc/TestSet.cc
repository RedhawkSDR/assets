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

#include "TestSet.h"
#include "Testable.h"
#include "TestRunner.h"
#include "Utilities.h"
#include <sstream>

using namespace std;
using namespace vrt;
using namespace vrttest;

/** Compares two test sets for the purposes of sorting. */
static bool comp (const TestSet& i, const TestSet& j) {
  string iname = i.descr;
  string jname = j.descr;
  for (uint32_t a = 0; (a < iname.length()) && (a < jname.length()); a++)
    if (iname[a] != jname[a])
      return iname[a] < jname[a];
  return iname.length() < jname.length();
}

/** Formats a stack trace. */
static void formatStackTrace (string &s, const VRTException &t) {
  ostringstream str;
  str << "   " << t.toString() << endl;

  vector<string> trace = t.getStackTrace();
  for (size_t i = 0; i < trace.size(); i++) {
    str << "        at " << trace[i] << endl;
  }
  s += str.str();
}

TestSet::TestSet (const TestSet &ts) :
  VRTObject(ts), // <-- Used to avoid warnings under GCC with -Wextra turned on
  descr(ts.descr),
  obj(ts.obj),
  tests(ts.tests),
  funcName(ts.funcName),
  testCount(ts.testCount),
  skipCount(ts.skipCount),
  errorCaught(ts.errorCaught),
  running(ts.running)
{
  // done
}

TestSet::TestSet () :
  descr(""),
  obj(NULL),
  funcName(""),
  testCount(__INT64_C(0)),
  skipCount(__INT64_C(0)),
  //errorCaught(NULL),
  running(NULL)
{
  // done
}

TestSet::TestSet (const string &descr, Testable *obj, const string &func) :
  descr(descr),
  obj(obj),
  funcName(func),
  testCount(__INT64_C(0)),
  skipCount(__INT64_C(0)),
  //errorCaught(NULL),
  running(NULL)
{
  // done
}

TestSet::TestSet (Testable *obj) :
  descr("???"),
  obj(obj),
  funcName(""),
  testCount(__INT64_C(0)),
  skipCount(__INT64_C(0)),
  //errorCaught(NULL),
  running(NULL)
{
  descr = string("Tests for ") + obj->getTestedClass();
  tests = obj->init();
}

TestSet::TestSet (const string &descr, Testable *obj, const vector<TestSet> &tests) :
  descr(descr),
  obj(obj),
  tests(tests),
  funcName(""),
  testCount(__INT64_C(0)),
  skipCount(__INT64_C(0)),
  running(NULL)
{
  // done
}

void TestSet::call (const string &func) const {
  if (running != NULL) running->call(func);
  else                 obj->call(func);
}

bool TestSet::runTest (string &s, int32_t flags) {
  if (funcName != "") {
    char skip = ' ';
    try {
      if (isSkipFunc(funcName)) {
        skip = funcName[0];
      }
      else {
        obj->call(funcName);
      }
    }
    catch (VRTException t) {
      errorCaught = t;
    }

    if (skip == ' ') {
      s += Utilities::format("  %-48s (attempted %8" PRId64 ", failed %2" PRId64 ", skipped %2" PRId64 ") %s\n",
                             descr.c_str(),
                             testCount,
                             ((isNull(errorCaught))? __INT64_C(0) : __INT64_C(1)),
                             skipCount,
                             ((isNull(errorCaught))? "PASS"       : "FAIL"));
    }
    else if (skip == '+') {
      s += Utilities::format("  %-48s (attempted        N, failed  0, skipped  0) PASS\n", descr.c_str());
    }
    else if (skip == '-') {
      s += Utilities::format("  %-48s (attempted        -, failed  -, skipped  -) SKIP\n", descr.c_str());
    }
    else if (skip == '!') {
      s += Utilities::format("  %-48s (attempted        -, failed  -, skipped  -) MISS\n", descr.c_str());
    }
    else {
      throw VRTException("Unsupported skip='%c'", skip);
    }

    if ((!isNull(errorCaught)) && ((flags & SHOW_ERRORS) != 0)) {
      if ((flags & SHOW_STACK) != 0) {
        formatStackTrace(s, errorCaught);
      }
      else {
        s += Utilities::format("    %s\n", errorCaught.getMessage().c_str());
      }
    }
    return isNull(errorCaught);
  }
  else {
    int subFlags = flags & ~SHOW_SUMMARY;
    s += Utilities::format("%-50s\n", descr.c_str());

    sort(tests.begin(),tests.end(),comp);

    for (size_t i = 0; i < tests.size(); i++) {
      running = &tests[i];
      tests[i].runTest(s, subFlags);
    }
    if ((tests.size() > 0) && (obj != NULL)) {
      obj->done();
    }
    running = NULL;

    if ((flags & SHOW_SUMMARY) != 0) {
      int64_t ct = getTestCount(CountType_ClassesTested);
      int64_t cf = getTestCount(CountType_ClassesFailed);
      int64_t ft = getTestCount(CountType_FunctionsTested);
      int64_t ff = getTestCount(CountType_FunctionsFailed);
      int64_t fs = getTestCount(CountType_FunctionsSkipped);
      int64_t fm = getTestCount(CountType_FunctionsMissing);
      int64_t tt = getTestCount(CountType_AllTested);
      int64_t tf = getTestCount(CountType_AllFailed);
      int64_t ts = getTestCount(CountType_AllSkipped);

      s += Utilities::format("\n");
      s += Utilities::format("\n");
      s += Utilities::format("Summary:\n");
      s += Utilities::format("             |           Tested      Failed     Skipped     Missing\n");
      s += Utilities::format("  -----------+-----------------------------------------------------\n");
      s += Utilities::format("  Classes    | %16" PRId64 "  %10" PRId64 "  %10" PRId64 "         n/a\n", ct, cf, __INT64_C(0));
      s += Utilities::format("  Functions  | %16" PRId64 "  %10" PRId64 "  %10" PRId64 "  %10" PRId64 "\n",       ft, ff, fs, fm);
      s += Utilities::format("  Test Cases | %16" PRId64 "  %10" PRId64 "  %10" PRId64 "         n/a\n", tt, tf, ts);
      s += Utilities::format("  \n");
      s += Utilities::format("Overall Status:\n");
      s += Utilities::format("  **%s**\n", ((tf == 0)? "PASS" : "FAIL"));
      return (tf == 0);
    }
    else {
      int64_t tf = getTestCount(CountType_AllFailed);
      return (tf == 0);
    }
  }
}

/** Counts the number of test cases of the given type. */
int64_t TestSet::getTestCount (CountType type) {
  if (!isNull(funcName)) {
    int32_t skip = (isSkipFunc(funcName) && (funcName[0] == '-'))? 1 : 0;
    int32_t miss = (isSkipFunc(funcName) && (funcName[0] == '!'))? 1 : 0;

    switch (type) {
      case CountType_FunctionsTested:  return 1-skip-miss;
      case CountType_FunctionsFailed:  return (!isNull(errorCaught))? 1 : 0;
      case CountType_FunctionsSkipped: return skip;
      case CountType_FunctionsMissing: return miss;
      case CountType_AllTested:        return testCount;
      case CountType_AllFailed:        return (!isNull(errorCaught))? 1 : 0;
      case CountType_AllSkipped:       return skipCount;
      default:                         break; // handled below
    }
  }
  else if (obj != NULL) {
    switch (type) {
      case CountType_ClassesTested:  return 1;
      case CountType_ClassesFailed:  return (getTestCount(CountType_FunctionsFailed) > 0)? 1 : 0;
      default:                       break; // handled below
    }
  }

  // Total sub-tests
  int64_t total = 0;
  for (size_t i = 0; i < tests.size(); i++) {
    total += tests[i].getTestCount(type);
  }
  return total;
}
