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

#ifndef _TestUtilities_h
#define _TestUtilities_h

#include "VRTObject.h"
#include <vector>

namespace vrttest {
  /** Utility methods for use during testing. */
  class TestUtilities {
    /** The number of special float values. */
    public: static const size_t SPECIAL_FLOAT_COUNT;

    /** Special float values. There is a 1:1 correspondence between
     *  <tt>SPECIAL_FLOAT_BITS[n]</tt> and <tt>SPECIAL_FLOAT_VALS[n]</tt> with
     *  the only difference being the use of a bit pattern vs the corresponding
     *  floating-point value.
     */
    public: static const int32_t SPECIAL_FLOAT_BITS[26];

    /** Special float values. */
    public: static const float *SPECIAL_FLOAT_VALS;

    /** The number of special double values. */
    public: static const size_t SPECIAL_DOUBLE_COUNT;

    /** Special double values. There is a 1:1 correspondence between
     *  <tt>SPECIAL_FLOAT_BITS[n]</tt> and <tt>SPECIAL_FLOAT_VALS[n]</tt> with
     *  the only difference being the use of a bit pattern vs the corresponding
     *  floating-point value.
     */
    public: static const int64_t SPECIAL_DOUBLE_BITS[26];

    /** Special double values. */
    public: static const double *SPECIAL_DOUBLE_VALS;
  };
}

#endif /* _TestUtilities_h */
