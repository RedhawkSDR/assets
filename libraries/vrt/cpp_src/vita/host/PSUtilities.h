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

#ifndef _PSUtilities_h
#define _PSUtilities_h

#include <vector>
#include <sstream>
#include <primitive.h>
#include <cstdlib>
#include <map>

using namespace std;
using namespace vrt;


const string  MODES         = "NOCHANGE,START,STOP";
const int32_t MODE_NOCHANGE = 1; // mode value: no change
const int32_t MODE_START    = 2; // mode value: start
const int32_t MODE_STOP     = 3; // mode value: stop

/** Fake streamID for use with the ALLSTREAMS output. This setting is intentionally equal to
 *  the "null" value following other conventions in the VRT libraries where lack of a streamID
 *  specifier is indicated with a "null" value. Within this file we simply use ALLSTREAMS as an
 *  alias to make the meaning obvious.
 */
const int32_t ALLSTREAMS = INT32_NULL;
const int32_t DATASTREAM = INT32_NULL+1;


/** Gets the value of a specified X-Midas result and converts it to the applicable type.
 *  @param label The label of the result.
 *  @param def   The default value to return if the result is not found.
 *  @return The value (converted to the applicable type if necessary) or def if not found.
 */
inline string getResult (const string &label, const string &def) {
  string aaval;
  double ddval;
  char   str[32];
  char   type = m_rfind(label, aaval, ddval);

  switch (type) {
    case ' ': return def;   // not found
    case 'A': return aaval;
    case 'D': snprintf(str, 32, "%f",          ddval); return str;
    case 'F': snprintf(str, 32, "%f",          ddval); return str;
    case 'L': snprintf(str, 32, "%d", (int32_t)ddval); return str;
    case 'I': snprintf(str, 32, "%d", (int32_t)ddval); return str;
    case 'B': snprintf(str, 32, "%d", (int32_t)ddval); return str;
    default:  snprintf(str, 32, "%f",          ddval); return str; // unknown, keep as double
  }
}

/** Gets the value of a specified X-Midas result and converts it to the applicable type.
 *  @param label The label of the result.
 *  @param def   The default value to return if the result is not found.
 *  @return The value (converted to the applicable type if necessary) or def if not found.
 */
inline double getResult (const string &label, double def) {
  string aaval;
  double ddval;
  char   type = m_rfind(label, aaval, ddval);

  if (type == ' ') return def; // not found
  if (type == 'A') {
    if (m_a2d(aaval, ddval) <= 0) {
      throw VRTException("Expected numeric value for result '%s' but found '%s'", label.c_str(), aaval.c_str());
    }
  }
  return ddval;
}

inline float   getResult (const string &label, float   def) { return (float  )getResult(label, (double)def); }
inline int32_t getResult (const string &label, int32_t def) { return (int32_t)getResult(label, (double)def); }
inline int16_t getResult (const string &label, int16_t def) { return (int16_t)getResult(label, (double)def); }
inline int8_t  getResult (const string &label, int8_t  def) { return (int8_t )getResult(label, (double)def); }


#if XM_SYS_VERSION_AT_LEAST(4,11,0)
// m_get_command_switches is now a standard X-Midas function

#elif XM_SYS_VERSION_AT_LEAST(4,9,0)
#  include <midas/internal/shell.hh>
inline void m_get_command_switches (XMValue::KVList &switches) {
  string cmdSwitchStr;
  m_get_command_switches(cmdSwitchStr);

  midas::internal::parse_switches_all(cmdSwitchStr.begin(), cmdSwitchStr.end(), switches);
}

#else
#  warning "Could not determine if the X-Midas m_get_command_switches(XMValue::KVList &switches) \
            or midas::internal::parse_switches_all(..) functions are available, disabling mapping \
            of Python switches to X-Midas TAG=VAL arguments."
inline void m_get_command_switches (XMValue::KVList &switches) {
  // Do nothing
  switches.clear();
}
#endif


/** Safely calls a function that returns an errno and throws an exception if the call fails. */
static inline void safeCall (char *msg, int error) {
  if (error != 0) {
    char errorMsg[128];
    strerror_r(error, errorMsg, 128);

    if (msg == NULL) {
      throw VRTException("ERROR: %s", errorMsg);
    }
    else {
      throw VRTException("ERROR: Can not %s: %s", msg, errorMsg);
    }
  }
}

#endif /* _PSUtilities_h */
