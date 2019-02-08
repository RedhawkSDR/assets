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

#ifndef _JsonUtilities_h
#define _JsonUtilities_h

#include "Value.h"
#include <vector>

using namespace std;
using namespace vrt;

namespace vrttest {
  /** <b>Internal Use Only:</b> Utilities for converting to/from JSON. <br>
   *  <br>
   *  This supports the following conversions:
   *  <pre>
   *    Java Type        | JSON Type
   *    -----------------+------------------------
   *    null             | "null"
   *    Boolean          | "true" or "false"
   *    Number           | Number
   *    CharSequence     | String
   *    Map              | Object
   *    Iterable         | Array
   *    -----------------+------------------------
   *    array            | Array
   *    Character        | String
   *    Object           | String
   *    -----------------+------------------------
   *    Note that entries on the lower half of the table are only
   *    applicable when converting to JSON, they will not be used
   *    when converting from JSON.
   *  </pre>
   */
  namespace JsonUtilities {
    /** Converts a Java object to a JSON object .
     *  @param obj The object to convert.
     *  @return The JSON string.
     */
    string toJSON (Value *obj);

    /** Converts a Java object to a JSON object .
     *  @param obj The object to convert.
     *  @return The JSON string.
     */
    inline string toJSON (Value &obj) {
      return toJSON(&obj);
    }

    /** Converts a JSON object to a Value.
     *  @param json The JSON object to convert.
     *  @return The Java object.
     */
    Value* fromJSON (const string &json);
  } END_NAMESPACE
} END_NAMESPACE
#endif /* _JsonUtilities_h */
