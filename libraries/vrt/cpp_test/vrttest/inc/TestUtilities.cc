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

#include "TestUtilities.h"

using namespace vrt;
using namespace vrttest;

const int32_t TestUtilities::SPECIAL_FLOAT_BITS[26] = {
  (int32_t)__INT64_C(0xFF800000),      // -Inf
  (int32_t)__INT64_C(0xFF7FFFFF),      // Largest  Negative Normalized
  (int32_t)__INT64_C(0xC0000000),      // -2.0
  (int32_t)__INT64_C(0xBF800000),      // -1.0
  (int32_t)__INT64_C(0xBF000000),      // -0.5
  (int32_t)__INT64_C(0x80800000),      // Smallest Negative Normalized
  (int32_t)__INT64_C(0x807FFFFF),      // Largest  Negative Denorm
  (int32_t)__INT64_C(0x80000001),      // Smallest Negative Denorm
  (int32_t)__INT64_C(0x80000000),      // -0.0
  0x00000000,      // +0.0
  0x00000001,      // Smallest Positive Denorm
  0x007FFFFF,      // Largest  Positive Denorm
  0x00800000,      // Smallest Positive Normalized
  0x3F000000,      // +0.5
  0x3F800000,      // +1.0
  0x40000000,      // +2.0
  0x7F7FFFFF,      // Largest  Positive Normalized
  0x7F800000,      // +Inf

  0x3DCCCCCD,      // +0.1
  0x402DF854,      // e
  0x3FB504F3,      // SQRT(2.0)
  0x3FDDB3D7,      // SQRT(3.0)
  0x404A62C2,      // SQRT(10.0)

  0x7F800001,      // NaN ("Smallest" Value)
  0x7FC00000,      // NaN (Java Normalized Value)
  0x7FFFFFFF,      // NaN ("Largest" Value)
};

const int64_t TestUtilities::SPECIAL_DOUBLE_BITS[26] = {
  (int64_t)__INT64_C(0xFFF0000000000000U),      // -Inf
  (int64_t)__INT64_C(0xFFEFFFFFFFFFFFFFU),      // Largest  Negative Normalized
  (int64_t)__INT64_C(0xC000000000000000U),      // -2.0
  (int64_t)__INT64_C(0xBFF0000000000000U),      // -1.0
  (int64_t)__INT64_C(0xBFE0000000000000U),      // -0.5
  (int64_t)__INT64_C(0x8010000000000000U),      // Smallest Negative Normalized
  (int64_t)__INT64_C(0x80FFFFFFFFFFFFFFU),      // Largest  Negative Denorm
  (int64_t)__INT64_C(0x8000000000000001U),      // Smallest Negative Denorm
  (int64_t)__INT64_C(0x8000000000000000U),      // -0.0
  __INT64_C(0x0000000000000000),      // +0.0
  __INT64_C(0x0000000000000001),      // Smallest Positive Denorm
  __INT64_C(0x00FFFFFFFFFFFFFF),      // Largest  Positive Denorm
  __INT64_C(0x0010000000000000),      // Smallest Positive Normalized
  __INT64_C(0x3FE0000000000000),      // +0.5
  __INT64_C(0x3FF0000000000000),      // +1.0
  __INT64_C(0x4000000000000000),      // +2.0
  __INT64_C(0x7FEFFFFFFFFFFFFF),      // Largest  Positive Normalized
  __INT64_C(0x7FF0000000000000),      // +Inf

  __INT64_C(0x3FB999999999999A),      // +0.1
  __INT64_C(0x4005BF0A8B145769),      // e
  __INT64_C(0x3FF6A09E667F3BCD),      // SQRT(2.0)
  __INT64_C(0x3FFBB67AE8584CAA),      // SQRT(3.0)
  __INT64_C(0x40094C583ADA5B53),      // SQRT(10.0)

  __INT64_C(0x7FF0000000000001),      // NaN ("Smallest" Value)
  __INT64_C(0x7FF8000000000000),      // NaN (Java Normalized Value)
  __INT64_C(0x7FFFFFFFFFFFFFFF),      // NaN ("Largest" Value)
};

const size_t  TestUtilities::SPECIAL_FLOAT_COUNT  = 26;
const size_t  TestUtilities::SPECIAL_DOUBLE_COUNT = 26;
const float  *TestUtilities::SPECIAL_FLOAT_VALS   = (float*)(void*)TestUtilities::SPECIAL_FLOAT_BITS;
const double *TestUtilities::SPECIAL_DOUBLE_VALS = (double*)(void*)TestUtilities::SPECIAL_DOUBLE_BITS;
