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

package nxm.vrt.net;

import java.io.Closeable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import nxm.vrt.lib.Utilities;

import static nxm.vrt.lib.Utilities.newNamedThreadFactory;

/** Utility class that handles common issues regarding threadable objects. */
public abstract class Threadable implements Closeable, Runnable {
  /** All of the thread factories in use. */
  private static final Map<Class<?>,ThreadFactory> threadFactories
    = new HashMap<Class<?>,ThreadFactory>(8);

  private final    ThreadFactory            threadFactory;
  private volatile Thread                   thread  = null;
  private volatile UncaughtExceptionHandler handler = null;
  private volatile boolean                  term    = false; // formerly called 'stop' changed to match C++
  private volatile Throwable                error   = null;

  /** Status of this task. */
  public static enum Status {
    /** Not yet started.    */ PENDING,
    /** Currently running.  */ RUNNING,
    /** Completed normally. */ COMPLETED,
    /** Stopped on error.   */ FAILED,
    /** Stopped on abort.   */ ABORTED,
  };

  /** Creates a new instance. */
  public Threadable () {
    this.threadFactory = getThreadFactory(getClass());
  }

  /** Gets the thread factory applicable to the given class type. */
  private static synchronized ThreadFactory getThreadFactory (Class<?> clazz) {
    ThreadFactory f = threadFactories.get(clazz);
    if (f == null) {
      String name = clazz.getSimpleName();
      if (name.isEmpty()) name = "Threadable";
      f = newNamedThreadFactory(name);
      threadFactories.put(clazz, f);
    }
    return f;
  }

  /** Starts the object running in a new thread.
   *  @throws RuntimeException If already running.
   */
  public void start () {
    if (thread != null) throw new RuntimeException("Already started");
    term   = false;
    thread = threadFactory.newThread(this);
    if (handler != null) {
      thread.setUncaughtExceptionHandler(handler);
    }
    thread.start();
  }

  /** Restarts the object in a new thread. If an object supports restarts it should define the
   *  following:
   *  <pre>
   *    public void restart () { super.restart(); }
   *  </pre>
   */
  protected void restart () {
    stop();
    thread = null;
    error  = null;
    start();
  }

  @Override
  public final void run () {
    Throwable err = null;
    try {
      runThread();
    }
    catch (Throwable t) {
      err = t;
      if (t instanceof RuntimeException) throw (RuntimeException)t;
      throw new RuntimeException("Exception in thread", t);
    }
    finally {
      shutdown();
      error = err; // <-- only set AFTER shutdown
    }
  }

  /** Stops the object running. This is identical to <tt>stop(true)</tt>. */
  @Override
  public final void close () { stop(true); }

  /** Stops the object running. This is identical to <tt>stop(true)</tt>. */
  public final void stop () { stop(true); }

  /** Tells the object to stop running. <br>
   *  <br>
   *  <i>Previous versions of this class allowed {@link #close()} and {@link #stop()} to
   *  be overridden. This unintentionally created a situation where users were calling
   *  those functions in cases where they should have called <tt>stop(false)</tt> and
   *  were making their code more deadlock-prone in some situations where the stop was
   *  instigated within the active thread. The solution was to add the {@link #stop(boolean)}
   *  function and require all code go through there.</i>
   *  @param wait Wait for the thread to complete (true)?
   */
  public void stop (boolean wait) {
    term = true;
    if (wait && (thread != null)) {
      if (thread == Thread.currentThread()) {
        throw new RuntimeException("Can not call stop(true) from within the processing thread.");
      }

      try {
        thread.interrupt();
        thread.join();
      }
      catch (InterruptedException e) {
        throw new RuntimeException("Thread stopped on interrupt", e);
      }
    }
  }

  /** Joins the object's thread.
   *  @throws InterruptedException If interrupted while waiting for the join.
   */
  public void join () throws InterruptedException {
    if (thread == null) return;
    if (thread == Thread.currentThread()) {
      throw new RuntimeException("Can not call join() from within the processing thread.");
    }
    thread.join();
  }

  private static final long TIME_ADJ  =  10; // fudge factor for internal delays (ms)
  private static final long MAX_SLEEP = 100; // max sleep time (ms)

  /** Sleeps until a given system time (see {@link System#currentTimeMillis()})
   *  or until {@link #stopNow()} is true.
   *  @param ms     The system time to wait for *or* the duration to wait for.
   *  @param flags 0=Duration, 1=EndTime
   */
  private void _sleep (long ms, int flags) {
    // Rather than calling Utilities.sleep(..) this directly calls Thread.sleep(..)
    // so we can catch the InterruptedException and fully break out.
    long now = System.currentTimeMillis();
    if (flags == 0) {
      ms = now + ms;
    }
    long endTime = ms - TIME_ADJ;

    try {
      while (!stopNow()) {
        long delay = endTime - now;

        if (delay <= 0) {
          return; // Done
        }
        if (delay <= MAX_SLEEP) {
          Thread.sleep(delay); // sleep remainder then exit
          return;
        }
        // only sleep max duration
        Thread.sleep(MAX_SLEEP);
        now = System.currentTimeMillis();
      }
    }
    catch (InterruptedException ex) {
      // ignore
    }
  }

  /** Sleeps until a given system time (see {@link System#currentTimeMillis()})
   *  or until {@link #stopNow()} is true.
   *  @param sysTime The system time to wait for.
   */
  protected final void sleepUntil (long sysTime) {
    _sleep(sysTime, 1);
  }

  /** Sleeps for a given duration or until {@link #stopNow()} is true.
   *  @param millis The sleep duration in ms.
   */
  protected final void sleep (long millis) {
    if (millis <= MAX_SLEEP) {
      // quick version for a short sleep which is fairly common
      Utilities.sleep(millis);
      return;
    }
    _sleep(millis, 0);
  }

  /** Sets the {@link UncaughtExceptionHandler} to use. This will simply call the
   *  {@link VRTEventListener#errorOccurred} method when an exception is caught.
   *  @param h The exception handler to use.
   */
  public final void setUncaughtExceptionHandler (final VRTEventListener h) {
    setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        h.errorOccurred(new VRTEvent(t), "Exception in thread "+t+": "+e, e);
      }
    });
  }

  /** Sets the {@link UncaughtExceptionHandler} to use.
   *  @param h The exception handler to use.
   */
  public final void setUncaughtExceptionHandler (UncaughtExceptionHandler h) {
    handler = h;
  }

  /** Gets the current status. Note that during a restart the results of calling
   *  this function will be unpredictable.
   *  @return The current status.
   */
  public final Status getStatus () {
    if (!isStarted() ) return Status.PENDING;
    if (!isDone()    ) return Status.RUNNING;
    if (error != null) return Status.FAILED;
    if (term         ) return Status.ABORTED;
                       return Status.COMPLETED;
  }

  /** Get the error that caused the thread to abort.
   *  @return The error that caused {@link #run()} to abort. This will return
   *          null if not done (see {@link #isDone()}) and/or if it completed
   *          "normally".
   */
  public final Throwable getAbortError () { return (isDone())? error : null; }

  /** Is this thread done? */
  public final boolean isDone () { return (thread == null) || (!thread.isAlive()); }

  /** Is this thread started? */
  public final boolean isStarted () { return (thread != null); }

  /** Should this thread stop now? */
  protected final boolean stopNow () { return term || (thread == null) || thread.isInterrupted(); }

  /** Runs the thread. */
  protected abstract void runThread () throws Exception;

  /** Closes any connections, This will always be called at the end of a call to {@link #run()}. */
  protected void shutdown () { }
}
