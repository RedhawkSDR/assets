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

#ifndef _Threadable_h
#define _Threadable_h

#include "VRTObject.h"
#include <pthread.h>
#include <iostream>

namespace vrt {
  using namespace std;

  /** Status of this task. */
  enum Status {
    /** Not yet started.    */ Status_PENDING,
    /** Currently running.  */ Status_RUNNING,
    /** Completed normally. */ Status_COMPLETED,
    /** Stopped on error.   */ Status_FAILED,
    /** Stopped on abort.   */ Status_ABORTED
  };

  /** Utility class that handles common issues regarding threadable objects. */
  class Threadable : public VRTObject {
    private: pthread_t     thread;   // The thread object
    private: bool          started;  // Has the thread been started?
    private: bool          attached; // Is the thread started but not not detached/joined?
    private: bool          term;     // Terminate now?
    private: bool          done;     // Done?
    private: VRTException *error;    // Error caught

    /** Basic no-argument constructor. */
    public: Threadable ();

    /** Basic copy constructor. */
    public: Threadable (const Threadable &t);

    /** Basic copy constructor. */
    public: ~Threadable ();

    /** Starts the object running in a new thread.
     *  @throws VRTException If already running.
     */
    public: virtual void start ();

    /** Restarts the object in a new thread. If an object supports restarts it should define the
     *  following:
     *  <pre>
     *    public: void restart () { super_restart(); }
     *  </pre>
     */
    protected: inline void super_restart () {
      stop();
      started = false;
      start();
    }

    /** Runs the processing in the current thread. In general, {@link #start} should be used
     *  to start processing.
     */
    public: void run ();

     /** Stops the object running. This is identical to <tt>stop(true)</tt>. */
     public: inline void close () { stop(true); }

    /** Tells the object to stop running.
     *  @param wait Wait for the thread to complete (true)?
     */
    public: virtual void stop (bool wait=true);

    /** Joins the object's thread. */
    public: void join ();

    /** Sleeps until a given system time (see {@link Utilities#currentTimeMillis()})
     *  or until {@link #stopNow()} is true.
     *  @param ms     The system time to wait for *or* the duration to wait for.
     *  @param flags 0=Duration, 1=EndTime
     */
    private: void _sleep (int64_t ms, int32_t flags) const;

    /** Sleeps until a given system time (see {@link Utilities#currentTimeMillis()})
     *  or until {@link #stopNow()} is true.
     *  @param sysTime The system time to wait for (in ms).
     */
    protected: void sleepUntil (int64_t sysTime) const;

    /** Sleeps for a given duration or until {@link #stopNow()} is true.
     *  @param millis The sleep duration in ms.
     */
    protected: void sleep (int64_t millis) const;

    /** Gets the current status. Note that during a restart the results of calling
     *  this function will be unpredictable.
     */
    public: Status getStatus () const;

    /** Get the error that caused the thread to abort. <br>
     *  <br>
     *  <b>The caller is responsible for deleting the returned object if not null.</b>
     *  @return The error that caused {@link #run()} to abort. This will return
     *          null if not done (see {@link #isDone()}) and/or if it completed
     *          "normally".
     */
    public: VRTException* getAbortError () const;

    /** Is this thread done? */
    public: inline bool isDone () const { return started && done; }

    /** Is this thread started? */
    public: inline bool isStarted () const { return started; }

    /** Should this thread stop now? */
    protected: inline bool stopNow () const { return term || !started; }

    /** Runs the thread. */
    protected: virtual void runThread () = 0;

    /** Closes any connections, This will always be called at the end of a call to {@link #run()}. */
    protected: virtual void shutdown ();
  };
} END_NAMESPACE
#endif /* _Threadable_h */
