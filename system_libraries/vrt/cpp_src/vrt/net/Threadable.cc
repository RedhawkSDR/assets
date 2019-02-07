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

#include "Threadable.h"
#include "Utilities.h"
#include <string>
#include <ostream>
#include <sys/types.h>
#include <sys/wait.h>

using namespace std;
using namespace vrt;

static void *_run (void *ptr) {
  Threadable *t = (Threadable*)ptr;
  t->run();
  return NULL;
}


Threadable::Threadable () :
  started(false),
  attached(false),
  term(false),
  done(false),
  error(NULL)
{
  // done
}

Threadable::Threadable (const Threadable &t) :
  VRTObject(t), // <-- Used to avoid warnings under GCC with -Wextra turned on
  started(t.started),
  attached(t.attached),
  term(t.term),
  done(t.done),
  error(NULL)
{
  if (t.error != NULL) {
    error = new VRTException(*t.error);
  }
}

Threadable::~Threadable () {
  safe_delete(error);
  if (attached) {
    pthread_detach(thread);
    attached = false;
  }
}

void Threadable::start () {
  if (started) throw VRTException("Already started");

  term     = false;
  done     = false;
  started  = true;

  int errnum = pthread_create(&thread, NULL, _run, (void*)(Threadable*)this);
  if (errnum != 0) {
    throw VRTException(errnum);
  }
  attached = true;
}

void Threadable::run () {
  // RUN THREAD
  try {
    runThread();
  }
  catch (VRTException e) {
    cout << "ERROR: Exception in thread: " << e.toString() << endl;
  }
  catch (exception e) {
    cout << "ERROR: Exception in thread: " << e.what() << endl;
  }
  catch (...) {
    cout << "ERROR: Exception in thread: <unknown exception>" << endl;
  }

  // SHUTDOWN
  try {
    shutdown();
  }
  catch (VRTException e) {
    cout << "ERROR: Exception in thread shutdown: " << e.toString() << endl;
  }
  catch (exception e) {
    cout << "ERROR: Exception in thread shutdown: " << e.what() << endl;
  }
  catch (...) {
    cout << "ERROR: Exception in thread shutdown: <unknown exception>" << endl;
  }

  // DONE
  done = true;
}

void Threadable::stop (bool wait) {
  term = true;
  if (wait && started) {
    if (pthread_equal(thread, pthread_self())) {
      throw VRTException("Can not call stop(true) from within the processing thread.");
    }
    join();
  }
}

void Threadable::join () {
  if (!started) throw VRTException("Cannot join thread that has not been started");
  if (done    ) return;

  void *rval;
  int errnum = pthread_join(thread,&rval);

  if (errnum != 0) {
    throw VRTException(errnum);
  }
  attached = false;
}

#define TIME_ADJ    10 // fudge factor for internal delays (ms)
#define MAX_SLEEP  100 // max sleep time (ms)

void Threadable::_sleep (int64_t ms, int32_t flags) const {
  int64_t now = Utilities::currentTimeMillis();
  if (flags == 0) {
    ms = now + ms;
  }
  int64_t endTime = ms - TIME_ADJ;

  while (!stopNow()) {
    int64_t delay = endTime - now;

    if (delay <= 0) {
      return; // done
    }
    if (delay <= MAX_SLEEP) {
      Utilities::sleep(delay); // sleep remainder then exit
      return;
    }
    // only sleep max duration
    Utilities::sleep(MAX_SLEEP);
    now = Utilities::currentTimeMillis();
  }
}

void Threadable::sleepUntil (int64_t sysTime) const {
  _sleep(sysTime, 1);
}

void Threadable::sleep (int64_t millis) const {
  if (millis <= MAX_SLEEP) {
    // quick version for a short sleep which is fairly common
    Utilities::sleep(millis);
    return;
  }
  _sleep(millis, 0);
}

Status Threadable::getStatus () const {
  if (!isStarted() ) return Status_PENDING;
  if (!isDone()    ) return Status_RUNNING;
  if (error != NULL) return Status_FAILED;
  if (term         ) return Status_ABORTED;
                     return Status_COMPLETED;
}

VRTException* Threadable::getAbortError () const {
  return (error == NULL)? NULL : new VRTException(*error);
}

void Threadable::shutdown () {
  // done
}
