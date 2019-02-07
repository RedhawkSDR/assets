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

package nxm.vrttest.net;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import nxm.vrt.lib.AsciiCharSequence;
import nxm.vrt.lib.TimeStamp;
import nxm.vrt.lib.Utilities;
import nxm.vrt.lib.VRTConfig;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.net.NetUtilities;
import nxm.vrt.net.VRTEvent;
import nxm.vrt.net.VRTEventListener;
import nxm.vrttest.net.NetTestHandler.NetTestEcho;
import nxm.vrttest.net.NetTestHandler.NetTestRamp;

import static nxm.vrttest.inc.JsonUtilities.toJSON;

/** A server used when running the network-based tests.
 *
 *  <pre>
 *    Start a Task
 *    --------------------------------------------------------------------------
 *    Requests that a task start. Note that this schedules the task to be
 *    started and returns (i.e. it does not wait for it to start).
 *
 *      URL
 *        /start
 *
 *      INPUT
 *        TYPE      - The type of task, one of:
 *                      "RAMP" (see {@link NetTestRamp})
 *                      "ECHO" (see {@link NetTestEcho})
 *
 *        *other*   - Any other parameters as required for the task (see task-
 *                    specific documentation for details)
 *
 *      OUTPUT
 *        STATUS    - One of:
 *                      "OK"        (request accepted),
 *                      "ERROR"     (request rejected)
 *        MESSAGE   - Free-form message describing status
 *        TASK      - UUID for the task
 *
 *
 *    Query Task Status
 *    --------------------------------------------------------------------------
 *    Queries the status of a task. Note that following the completion of a
 *    task, the "exit status" is only held temporarially, so it is possible
 *    to get a "NOT-FOUND" status for a valid task ID that has been discarded
 *    following a "COMPLETED", "FAILED", or "ABORTED".
 *
 *      URL
 *        /query
 *
 *      INPUT
 *        TASK      - UUID of the task
 *
 *      OUTPUT
 *        STATUS    - One of:
 *                      "OK"        (request ID matched),
 *                      "NOT-FOUND" (request ID not matched),
 *                      "ERROR"     (missing/invalid request ID)
 *        MESSAGE   - Free-form message describing status
 *        TASK      - UUID for the task (same as input)
 *        RESULT    - One of:
 *                      "PENDING"   (not yet started)
 *                      "RUNNING"   (currently running)
 *                      "COMPLETED" (completed normally)
 *                      "FAILED"    (stopped on error)
 *                      "ABORTED"   (stopped on abort, i.e. a "stop request")
 *        ERRORS    - List of errors detected while process was running (null
 *                    if unknown)
 *        WARNINGS  - List of warnings detected while process was running (null
 *                    if unknown)
 *
 *    Stop a Task
 *    --------------------------------------------------------------------------
 *    Requests that a task abort. Note that this schedules the task to be
 *    aborted and returns (i.e. it does not wait for it to stop). Calling this
 *    on a request that has already stopped will have no adverse affects.
 *
 *      URL
 *        /stop
 *
 *      INPUT
 *        REQUEST   - UUID of the request
 *
 *      OUTPUT
 *        STATUS    - One of:
 *                      "OK"        (request ID matched),
 *                      "NOT-FOUND" (request ID not matched),
 *                      "ERROR"     (missing/invalid request ID)
 *        MESSAGE   - Free-form message describing status
 *        TASK      - UUID for the task (same as input)
 *
 *    Shutdown Server
 *    --------------------------------------------------------------------------
 *    Stops the running server. Note that this schedules the server for shutdown
 *    and returns (i.e. it does not wait for it to shutdown).
 *
 *      URL
 *        /shutdown
 *
 *      INPUT
 *        None
 *
 *      OUTPUT
 *        STATUS    - One of:
 *                      "OK"        (request accepted),
 *                      "ERROR"     (request rejected)
 *        MESSAGE   - Free-form message describing status
 *        TASK      - null
 *  </pre>
 */
public final class NetTestServer implements Runnable, HttpHandler, VRTEventListener {
  private static final boolean DEBUG      = true;  // Enable debug output?
  private static final int     BACKLOG    = 16;    // Max server backlog
  private static final String  PATH       = "/";   // Server path
  private static final int     STOP_DELAY = 15;    // Wait time while stopping (sec)

  private final    HttpServer  server;     // Server to use
  private final    HttpContext context;    // Context to use
  private final    int         duration;   // The run-time duration (sec)
  private volatile boolean     done;       // Is server done?

  /** The map of all active tasks. */
  final Map<UUID,NetTestHandler> activeTasks
          = Collections.synchronizedMap(new LinkedHashMap<UUID,NetTestHandler>());

  /** The map of all recent tasks (memory permitting). */
  final Map<UUID,NetTestHandler> recentTasks
          = Collections.synchronizedMap(new WeakHashMap<UUID,NetTestHandler>());

  @SuppressWarnings("LeakingThisInConstructor")
  public NetTestServer () throws IOException {
    String host = NetUtilities.getHost(VRTConfig.TEST_SERVER);
    int    port = NetUtilities.getPort(VRTConfig.TEST_SERVER);

    InetSocketAddress address = new InetSocketAddress(host, port);

    this.server   = HttpServer.create(address, BACKLOG);
    this.context  = server.createContext(PATH, this);
    this.duration = VRTConfig.TEST_SERVER_TIMEOUT;
  }

  /** Runs the service. */
  @Override
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public void run () {
    if (DEBUG) System.out.println("DEBUG: Starting Server on "+server.getAddress()+"...");
    server.start();
    if (DEBUG) System.out.println("DEBUG: Started");


    if (DEBUG) System.out.println("DEBUG: Running for "+duration+" seconds");
    long endTime = System.currentTimeMillis() + (duration * 1000); // end time in ms

    while (!done && (System.currentTimeMillis() < endTime)) {
      Utilities.sleep(1000);
    }
    done = true;

    if (DEBUG) System.out.println("DEBUG: Stopping Server...");
    for (NetTestHandler h : recentTasks.values()) {
      h.stop(false);  // <-- Tell all tasks to stop
    }
    for (NetTestHandler h : recentTasks.values()) {
      h.stop(true);   // <-- Wait for all tasks to stop
    }
    server.stop(STOP_DELAY);
    if (DEBUG) System.out.println("DEBUG: Done");
  }

  /** Checks the path (and method) of a request, returning true if successful and
   *  false if path doesn't match. If path matches but method is wrong and exception
   *  will be thrown.
   */
  private boolean checkPath (URI uri, String method, String path, String ... methods) {
    if (!uri.getPath().equals(path)) return false;

    for (String m : methods) {
      if (method.equals(m)) return true;
    }

    throw new HttpException(405, "Method Not Allowed");
  }


  @Override
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public void handle (HttpExchange ex) throws IOException {
    // ==== READ REQUEST =======================================================
    String      method     = ex.getRequestMethod();
    URI         uri        = ex.getRequestURI();
    String      protocol   = ex.getProtocol();
    Headers     inHeaders  = ex.getRequestHeaders();
    InputStream inBody     = ex.getRequestBody();

    if (DEBUG) {
      System.out.println("");
      System.out.println("DEBUG: Received Request:");
      System.out.println("DEBUG:   time     = "+TimeStamp.getSystemTime());
      System.out.println("DEBUG:   method   = "+method);
      System.out.println("DEBUG:   URI      = "+uri);
      System.out.println("DEBUG:   protocol = "+protocol);
      System.out.println("DEBUG:   headers  = "+inHeaders.entrySet());
    }

    // ==== HANDLE REQUEST =====================================================
    Headers            outHeaders = ex.getResponseHeaders();
    Map<String,Object> output     = new LinkedHashMap<String,Object>();
    int                outCode;     // HTTP response code

    try {
      Map<String,String> query = parseQuery(uri.getQuery());

      // ---- TBD --------------------------------------------------------------
      if (checkPath(uri, method, "/start", "GET")) {
        outCode = handleStart(query, output);
      }
      else if (checkPath(uri, method, "/stop", "GET")) {
        outCode = handleStop(query, output);
      }
      else if (checkPath(uri, method, "/query", "GET")) {
        outCode = handleQuery(query, output);
      }
      else if (checkPath(uri, method, "/shutdown", "GET")) {
        outCode = handleShutdown(query, output);
      }
      else {
        outCode = 404;
        output.put("STATUS",  "ERROR");
        output.put("MESSAGE", "Not Found");
      }
    }
    catch (HttpException e) {
      outCode  = e.outCode;
      output.clear();
      output.put("STATUS",  "ERROR");
      output.put("MESSAGE", e.getMessage());
    }
    catch (Exception e) {
      if (DEBUG) {
        System.out.println("");
        System.out.println("DEBUG: Internal Server Error Detected:");
        System.out.println("DEBUG: "+e);
        e.printStackTrace(System.out);
      }
      outCode  = 500;
      output.clear();
      output.put("STATUS",  "ERROR");
      output.put("MESSAGE", "Internal Server Error");
    }

    // ==== WRITE OUTPUT =======================================================
    AsciiCharSequence json = AsciiCharSequence.fromCharSequence(toJSON(output));
    outHeaders.add("Content-Type", "application/json");

    if (DEBUG) {
      String out = json.toString().replace("\n", "\n                   ");

      System.out.println("DEBUG: Sending Response:");
      System.out.println("DEBUG:   time    = "+TimeStamp.getSystemTime());
      System.out.println("DEBUG:   code    = "+outCode);
      System.out.println("DEBUG:   length  = "+json.length());
      System.out.println("DEBUG:   headers = "+outHeaders.entrySet());
      System.out.println("DEBUG:   output  = "+out);
      System.out.println("");
    }
    ex.sendResponseHeaders(outCode, json.length());

    OutputStream outBody = ex.getResponseBody();
    outBody.write(json.getBytes());
    outBody.close();
  }

  /** Parses an HTTP query string into a Map of key=val pairs. */
  private static Map<String,String> parseQuery (String query) {
    Map<String,String> map = new LinkedHashMap<String,String>();
    if ((query != null) && !query.isEmpty()) {
      for (String token : query.split("&")) {
        int i = token.indexOf('=');
        int j = token.lastIndexOf('=');

        if ((i < 0) || (i != j)) {
          throw new HttpException(400, "Bad Request: Invalid query string");
        }

        map.put(fromHTTP(token.substring(0,i).toUpperCase()),
                fromHTTP(token.substring(i+1)));
      }
    }
    return map;
  }

  /** Convert a URL-encoded string with HTTP escapes to a Java string. */
  private static String fromHTTP (String str) {
    str = str.replace('+', ' ');

    int i = str.indexOf('%');
    if (i < 0) return str;

    try {
      while (i >= 0) {
        if (i >= str.length()-2) {
          throw new HttpException(400, "Bad Request: Invalid HTTP escape in query string");
        }
        int num = Integer.parseInt(str.substring(i+1,i+3), 16);

        str = str.substring(0,i) + ((char)num) + str.substring(i+3);
        i   = str.indexOf('%',i+1);
      }
    }
    catch (NumberFormatException e) {
      throw new HttpException(400, "Bad Request: Invalid HTTP escape in query string");
    }
    return str;
  }


  //////////////////////////////////////////////////////////////////////////////
  // REQUEST HANDLERS
  //////////////////////////////////////////////////////////////////////////////
  /** Handles a shutdown request.
   *  @param query  The HTTP query submitted.
   *  @param output The output content.
   *  @return The HTTP status code (usually 200=OK or 202=ACCEPTED).
   */
  private int handleShutdown (Map<String,String> query, Map<String,Object> output) {
    done = true; // <-- do shutdown
    output.put("STATUS",  "OK");
    output.put("MESSAGE", "Shutting down");
    output.put("TASK",    null);
    return 202;
  }

  /** Handles a start request.
   *  @param query  The HTTP query submitted.
   *  @param output The output content.
   *  @return The HTTP status code (usually 200=OK or 202=ACCEPTED).
   */
  private int handleStart (Map<String,String> query, Map<String,Object> output) {
    NetTestHandler handler = null;
    String         type    = query.get("TYPE");
    if ((type == null) || type.isEmpty()) {
      output.put("STATUS",  "ERROR");
      output.put("MESSAGE", "Missing TYPE parameter with start request");
      output.put("TASK",    null);
      return 200;
    }

    try {
      String t = type.toUpperCase();
      if (t.equals("RAMP")) handler = new NetTestRamp(this, query, output);
      if (t.equals("ECHO")) handler = new NetTestEcho(this, query, output);

    }
    catch (Exception e) {
      output.put("STATUS",  "ERROR");
      output.put("MESSAGE", e.getMessage());
      output.put("TASK",    null);
      return 200;
    }


    if (handler == null) {
      output.put("STATUS",  "ERROR");
      output.put("MESSAGE", "Inavlid TYPE parameter with start request, expected "
                           +"task type (e.g. RAMP,etc.) but given '"+type+"'");
      output.put("TASK",    null);
      return 200;
    }

    handler.start();
    output.put("STATUS",  "OK");
    output.put("MESSAGE", "Start request accepted");
    output.put("TASK",    handler.getTaskID());
    return 202;
  }

  /** Handles a stop request.
   *  @param query  The HTTP query submitted.
   *  @param output The output content.
   *  @return The HTTP status code (usually 200=OK or 202=ACCEPTED).
   */
  private int handleStop (Map<String,String> query, Map<String,Object> output) {
    NetTestHandler handler = getRequestHandler(query, output);
    if (handler == null) {
      return 200;
    }
    else if (handler.isDone()) {
      output.put("STATUS",  "OK");
      output.put("MESSAGE", "Stop request not applicable, already stopped");
      return 200;
    }
    else {
      handler.stop(false);
      output.put("STATUS",  "OK");
      output.put("MESSAGE", "Stop request accepted");
      return 202;
    }
  }

  /** Handles a stop request.
   *  @param query  The HTTP query submitted.
   *  @param output The output content.
   *  @return The HTTP status code (usually 200=OK or 202=ACCEPTED).
   */
  private int handleQuery (Map<String,String> query, Map<String,Object> output) {
    NetTestHandler handler = getRequestHandler(query, output);
    if (handler == null) {
      output.put("RESULT",   output.get("STATUS")); // "ERROR" or "NOT-FOUND"
      output.put("ERRORS",   null);
      output.put("WARNINGS", null);
    }
    else {
      output.put("MESSAGE",  "Successfully obtained task status");
      output.put("RESULT",   handler.getStatus());
      output.put("ERRORS",   handler.getErrors());
      output.put("WARNINGS", handler.getWarnings());
    }
    return 200;
  }

  /** Gets the handler used for a given request.
   *  @param query  The HTTP query submitted.
   *  @param output The output content.
   *  @return The handler or null if not found. Note that the output will
   *          will be updated as applicable.
   */
  private NetTestHandler getRequestHandler (Map<String,String> query, Map<String,Object> output) {
    if (!query.containsKey("TASK")) {
      output.put("STATUS",  "ERROR");
      output.put("MESSAGE", "No REQUEST identifier provided");
      output.put("TASK",    null);
      return null;
    }

    UUID uuid;
    try {
      uuid = UUID.fromString(query.get("TASK"));
    }
    catch (Exception e) {
      output.put("STATUS",  "ERROR");
      output.put("MESSAGE", "Invalid REQUEST identifier provided");
      output.put("TASK",    query.get("TASK"));
      return null;
    }

    NetTestHandler handler = recentTasks.get(uuid);
    if (handler == null) {
      output.put("STATUS",  "NOT-FOUND");
      output.put("MESSAGE", "Unknown REQUEST identifier provided");
      output.put("TASK",    uuid);
      return null;
    }
    else {
      output.put("STATUS",  "OK");
      output.put("MESSAGE", "");
      output.put("TASK",    uuid);
      return handler;
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  // Implement VRTEventListener
  //////////////////////////////////////////////////////////////////////////////
  @Override public void receivedPacket (VRTEvent e, VRTPacket p) { } // ignore
  @Override public void sentPacket (VRTEvent e, VRTPacket p) { } // ignore

  @Override
  @SuppressWarnings({"UseOfSystemOutOrSystemErr","CallToThreadDumpStack"})
  public void errorOccurred (VRTEvent e, String msg, Throwable t) {
    System.err.println("ERROR: "+msg+" (source="+e.getSource()+")");
    if (DEBUG && (t != null)) {
      t.printStackTrace();
    }
  }

  @Override
  @SuppressWarnings({"UseOfSystemOutOrSystemErr","CallToThreadDumpStack"})
  public void warningOccurred (VRTEvent e, String msg, Throwable t) {
    System.err.println("WARNING: "+msg+" (source="+e.getSource()+")");
    if (DEBUG && (t != null)) {
      t.printStackTrace();
    }
  }


  //////////////////////////////////////////////////////////////////////////////
  // NESTED CLASSES
  //////////////////////////////////////////////////////////////////////////////

  /** An exception with HTTP output information. */
  private static final class HttpException extends RuntimeException {
    private static final long serialVersionUID = 0xa982d7536f9be4f0L;

    final int outCode; // HTTP output code

    /** Creates a new instance. */
    public HttpException (int outCode, String outMessage) {
      super(outMessage);
      this.outCode = outCode;
    }

    /** Creates a new instance. */
    public HttpException (int outCode, String outMessage, Throwable cause) {
      super(outMessage, cause);
      this.outCode = outCode;
    }
  }
}
