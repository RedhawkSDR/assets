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

#include "TestRunner.h"
#include "JsonUtilities.h"
#include "NetUtilities.h"
#include "TimeStamp.h"
#include "Utilities.h"

using namespace std;
using namespace vrt;
using namespace vrttest;

TestSet *TestRunner::thisTest  = NULL;
string   TestRunner::serverURL = "";

bool TestRunner::runTests (vector<Testable*> &testCases, bool verbose) {
  try {
    vector<TestSet> tests;

    for (size_t i = 0; i < testCases.size(); i++) {
      tests.push_back(TestSet(testCases[i]));
    }
    thisTest  = new TestSet("", NULL, tests);
    serverURL = string("http://")+VRTConfig::getTestServer();

    string    out       = "";
    int32_t   flags     = TestSet::DEF_FLAGS;
    TimeStamp start     = TimeStamp::getSystemTime();
    bool      pass      = thisTest->runTest(out, flags);
    TimeStamp end       = TimeStamp::getSystemTime();
    TimeStamp delta     = end.toGPS().addTime(-start.getGPSSeconds(), -start.getPicoSeconds());
    double    duration  = delta.getGPSSeconds() + (delta.getPicoSeconds() * 1e-12);

    if (verbose) {
      cout << out << endl;
      printf("Duration:\n");
      printf("  %.3f sec\n", duration);
      printf("\n");
    }
    delete thisTest;
    thisTest = NULL;
    return pass;
  }
  catch (VRTException e) {
    throw VRTException("Unable to run tests: %s", e.what());
  }
  catch (exception e) {
    throw VRTException("Unable to run tests: %s", e.what());
  }
}

map<string,string> TestRunner::getTestSocketOptions (TransportProtocol transport) {
  bool               optimize = true;
  string             bufSize  = "1048576"; // 1 MiB
  map<string,string> options  = NetUtilities::getDefaultSocketOptions(optimize, transport);

  options["RCVBUF_EAGER"] = "true"; // always use, even if not default
  options["SO_SNDBUF"   ] = bufSize;
  options["SO_RCVBUF"   ] = bufSize;

  return options;
}

Value* TestRunner::sendServerRequest (const string &path, map<string,string> &req) {
  string url   = "";
  bool   first = true;

  url += serverURL;
  url += path;
  for (map<string,string>::iterator it = req.begin(); it != req.end(); ++it) {
    url += (first)? "?" : "&";
    url += it->first + "=" + it->second;
    first = false;
  }

  try {
    map<string,string> header;
    string             data;

    NetUtilities::doHttpGet(url, header, data);
    return JsonUtilities::fromJSON(data);
  }
  catch (VRTException e) {
    throw VRTException("Unable to send request to server using %s: %s", url.c_str(), e.what());
  }
}

void TestRunner::assertSame (const string &msg, const VRTObject *exp, const VRTObject *act) {
  thisTest->countTest();
  void *expPtr = (void*)exp;
  void *actPtr = (void*)act;
  if (expPtr == actPtr) {
    // PASS
  }
  else if (expPtr == NULL) {
    throw TestFailedException(msg, string("null"), act->toString());
  }
  else if (actPtr == NULL) {
    throw TestFailedException(msg, exp->toString(), string("null"));
  }
  else {
    throw TestFailedException(msg, exp->toString(), act->toString());
  }
}

void TestRunner::assertBufEquals (const string &msg, const void *expBuf, size_t expIdx, size_t expLen,
                                                     const void *actBuf, size_t actIdx, size_t actLen) {
  const char *_expBuf = (const char*)expBuf;
  const char *_actBuf = (const char*)actBuf;

  thisTest->countTest();
  if ((expBuf == NULL) && (actBuf == NULL)) {
    // PASS
  }
  else if (expBuf == NULL) {
    throw TestFailedException(msg, "null", "{ ... }");
  }
  else if (actBuf == NULL) {
    throw TestFailedException(msg, "{ ... }", "null");
  }
  else if (expLen != actLen) {
    throw TestFailedException(msg+" (length)", expLen, actLen);
  }
  else if (&_expBuf[expIdx] == &_actBuf[actIdx]) {
    // PASS (same location in memory)
  }
  else {
    for (size_t i = 0; i < expLen; i++) {
      if (_expBuf[i+expIdx] != _actBuf[i+actIdx]) {
        int32_t e = 0xFF & ((int32_t)_expBuf[i+expIdx]);
        int32_t a = 0xFF & ((int32_t)_actBuf[i+actIdx]);
        char txt[32]; snprintf(txt, 32, " (i=%d)", (int32_t)i);
        throw TestFailedException(msg+txt, "0x"+Utilities::toHexString(e,1),
                                           "0x"+Utilities::toHexString(a,1));
      }
    }
  }
}

void TestRunner::assertException (const string &msg, const string &func) {
  thisTest->countTest();
  try {
    thisTest->call(func);
  }
  catch (VRTException e) {
    UNUSED_VARIABLE(e);
    return; // PASS
  }
  catch (exception e) {
    UNUSED_VARIABLE(e);
    return; // PASS
  }
  throw TestFailedException(msg, "<exception>", "<no exception>");
}

void TestRunner::assertException (const string &msg, const string &func, const string &expMsg) {
  thisTest->countTest();
  try {
    thisTest->call(func);
  }
  catch (VRTException e) {
    if (expMsg != e.toString()) {
      throw TestFailedException(msg, expMsg, e.toString());
    }
    else {
      return; // PASS
    }
  }
  catch (exception e) {
    string actMsg(e.what());
    if (expMsg != actMsg) {
      throw TestFailedException(msg, expMsg, actMsg);
    }
    else {
      return; // PASS
    }
  }
  throw TestFailedException(msg, "<exception>", "<no exception>");
}
