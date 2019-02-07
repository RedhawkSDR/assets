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

#include "PackUnpackTest.h"
#include "VRTObject.h"
#include "PackUnpack.h"
#include "VRTConfig.h"
#include "VRTMath.h"
#include "TestRunner.h"
#include "TestUtilities.h"

using namespace std;
using namespace vrt;
using namespace vrttest;
using namespace PackUnpack;
using namespace TestRunner;

#define SPECIAL_FLOAT_COUNT  TestUtilities::SPECIAL_FLOAT_COUNT
#define SPECIAL_FLOAT_BITS   TestUtilities::SPECIAL_FLOAT_BITS
#define SPECIAL_FLOAT_VALS   TestUtilities::SPECIAL_FLOAT_VALS
#define SPECIAL_DOUBLE_COUNT TestUtilities::SPECIAL_DOUBLE_COUNT
#define SPECIAL_DOUBLE_BITS  TestUtilities::SPECIAL_DOUBLE_BITS
#define SPECIAL_DOUBLE_VALS  TestUtilities::SPECIAL_DOUBLE_VALS

#define QUICK_TEST           VRTConfig::getTestQuick()

/** Determines number of exponent bits in VRT format. */
static inline int32_t getExponentBits (DataItemFormat form) { // TODO: This belongs elsewhere
  switch (form) {
    case DataItemFormat_SignedVRT1:   return 1;
    case DataItemFormat_SignedVRT2:   return 2;
    case DataItemFormat_SignedVRT3:   return 3;
    case DataItemFormat_SignedVRT4:   return 4;
    case DataItemFormat_SignedVRT5:   return 5;
    case DataItemFormat_SignedVRT6:   return 6;
    case DataItemFormat_UnsignedVRT1: return 1;
    case DataItemFormat_UnsignedVRT2: return 2;
    case DataItemFormat_UnsignedVRT3: return 3;
    case DataItemFormat_UnsignedVRT4: return 4;
    case DataItemFormat_UnsignedVRT5: return 5;
    case DataItemFormat_UnsignedVRT6: return 6;
    default: return -1;
  }
}

string PackUnpackTest::getTestedClass () {
  return "vrt::PackUnpack";
}

vector<TestSet> PackUnpackTest::init () {
  vector<TestSet> tests;
  tests.push_back(TestSet("packAsByte(..)",     this, "testPack"));
  tests.push_back(TestSet("packAsDouble(..)",   this, "+testPack"));
  tests.push_back(TestSet("packAsFloat(..)",    this, "+testPack"));
  tests.push_back(TestSet("packAsInt(..)",      this, "+testPack"));
  tests.push_back(TestSet("packAsLong(..)",     this, "+testPack"));
  tests.push_back(TestSet("packAsShort(..)",    this, "+testPack"));

  tests.push_back(TestSet("unpackAsByte(..)",   this, "testUnpack"));
  tests.push_back(TestSet("unpackAsDouble(..)", this, "+testUnpack"));
  tests.push_back(TestSet("unpackAsFloat(..)",  this, "+testUnpack"));
  tests.push_back(TestSet("unpackAsInt(..)",    this, "+testUnpack"));
  tests.push_back(TestSet("unpackAsLong(..)",   this, "+testUnpack"));
  tests.push_back(TestSet("unpackAsShort(..)",  this, "+testUnpack"));

  return tests;
}

void PackUnpackTest::done () {
  // nothing to do
}

void PackUnpackTest::call (const string &func) {
  if (func == "testPack"  ) testPack();
  if (func == "testUnpack") testUnpack();
}


/** Gets bits from an array.
 *  @param buf       The buffer to read from.
 *  @param bitOffset The bit offset into the buffer to start at.
 *  @param numBits   The number of bits to read in.
 *  @param sign      Should the output value be sign-extended?
 */
static int64_t getBits (void *ptr, int32_t bitOffset, int32_t numBits, bool sign) {
  int64_t bits = VRTMath::unpackBits64(ptr, bitOffset, numBits);
  if (sign) { // sign-extend
    int32_t shift = 64 - numBits;
    bits = ((bits << shift) >> shift);
  }
  return bits;
}

/** Creates an int array with a range of values. */
static vector<int32_t> range (int32_t start, int32_t end) {
  vector<int32_t> vals;

  for (int32_t i = 0; i < (end-start); i++) {
    vals.push_back(start + i);
  }
  return vals;
}

void PackUnpackTest::testPack () {
  vector<int32_t> eSizeOptions = range(-1,8);
  vector<int32_t> cSizeOptions = range(-1,8);
  vector<int32_t> dSizeOptions = range(1,65);

  if (QUICK_TEST) {
    eSizeOptions.resize(0);      cSizeOptions.resize(0);      dSizeOptions.resize(0);
    eSizeOptions.push_back(-1);  cSizeOptions.push_back(-1);  dSizeOptions.push_back( 1);
    eSizeOptions.push_back( 0);  cSizeOptions.push_back( 0);  dSizeOptions.push_back( 4);
    eSizeOptions.push_back( 4);  cSizeOptions.push_back( 4);  dSizeOptions.push_back( 7);
    eSizeOptions.push_back( 7);  cSizeOptions.push_back( 7);  dSizeOptions.push_back( 8);
                                 cSizeOptions.push_back(15);  dSizeOptions.push_back( 9);
                                                              dSizeOptions.push_back(12);
                                                              dSizeOptions.push_back(15);
                                                              dSizeOptions.push_back(16);
                                                              dSizeOptions.push_back(17);
                                                              dSizeOptions.push_back(24);
                                                              dSizeOptions.push_back(31);
                                                              dSizeOptions.push_back(32);
                                                              dSizeOptions.push_back(33);
                                                              dSizeOptions.push_back(48);
                                                              dSizeOptions.push_back(63);
                                                              dSizeOptions.push_back(64);
  }

  // ==== INTEGER INPUT VALUES ===============================================
  vector<int8_t > inByte(256);
  vector<int16_t> inShort(256*2);
  vector<float  > inFloat(256*3);
  vector<int32_t> inInt(256*4);
  vector<double > inDouble(256*5);
  vector<int64_t> inLong(256*8);
  vector<int32_t> inEvent(inLong.size());
  vector<int32_t> inChannel(inLong.size());

  for (size_t j = 0; j < inLong.size();) {
    for (int64_t i = 0x00; i <= 0xFF; i++,j++) {
      int64_t bits = ((i ^ 0xFF) << 56)
                   | ((i ^ 0xFA) << 48)
                   | ((i ^ 0xF5) << 40)
                   | ((i ^ 0xAF) << 32)
                   | ((i ^ 0xAA) << 24)
                   | ((i ^ 0xA5) << 16)
                   | ((i ^ 0x55) <<  8)
                   | ((i       )      );
      if (j < inByte.size()   ) inByte[j]    = (int8_t )bits;
      if (j < inShort.size()  ) inShort[j]   = (int16_t)bits;
      if (j < inInt.size()    ) inInt[j]     = (int32_t)bits;
      if (j < inLong.size()   ) inLong[j]    = (int64_t)bits;
      if (j < inFloat.size()  ) inFloat[j]   = (float  )(bits & 0x00FFFFFF);
      if (j < inDouble.size() ) inDouble[j]  = (double )(bits & __INT64_C(0xFFFFFFFFFF));
      if (j < inEvent.size()  ) inEvent[j]   = (int32_t)(bits & 0x0000007F); //  7-bit max
      if (j < inChannel.size()) inChannel[j] = (int32_t)(bits & 0x00007FFF); // 15-bit max
    }
  }

  // ==== FLOAT/DOUBLE EXP VALUES ============================================
  vector<int32_t> fexp(SPECIAL_FLOAT_COUNT);
  vector<int64_t> dexp(SPECIAL_DOUBLE_COUNT);

  for (size_t i = 0; i < fexp.size(); i++) {
    fexp[i] = SPECIAL_FLOAT_BITS[i];
    dexp[i] = SPECIAL_DOUBLE_BITS[i];

    // Normalize the NaN values
    if ((fexp[i] >= 0x7F800001                   ) && (fexp[i] <= 0x7FFFFFFF                   )) fexp[i] = 0x7FC00000;
    if ((dexp[i] >= __INT64_C(0x7FF0000000000001)) && (dexp[i] <= __INT64_C(0x7FFFFFFFFFFFFFFF))) dexp[i] = __INT64_C(0x7FF8000000000000);
  }

  // ==== VRT INPUT VALUES ===================================================
  vector<double> inVRT;

  double testVal = -2.0;
  for (size_t i = 0; i < SPECIAL_DOUBLE_COUNT; i++) {
    inVRT.push_back(SPECIAL_DOUBLE_VALS[i]);
  }
  for (size_t i = SPECIAL_DOUBLE_COUNT; i < inVRT.size(); i++) {
    inVRT.push_back(testVal);
    testVal += 1.0 / 256.0;
  }

  // ==== TEST CASES =========================================================
  for (int32_t offset = 0; offset <= 512; offset+=512) {
    for (size_t eSizeIdx = 0; eSizeIdx < eSizeOptions.size(); eSizeIdx++) {
      int32_t          eSize    = eSizeOptions[eSizeIdx];
      int32_t         *evt      = (eSize < 0)? NULL : &inEvent[0];
      vector<int32_t>  expEvent(inEvent.size());

      if (eSize > 0) {
        int32_t mask = ~(-1 << eSize);
        for (size_t i = 0; i < expEvent.size(); i++) {
          expEvent[i] = inEvent[i] & mask;
        }
      }

      for (size_t cSizeIdx = 0; cSizeIdx < cSizeOptions.size(); cSizeIdx++) {
        int32_t          cSize      = cSizeOptions[cSizeIdx];
        int32_t         *chan       = (cSize < 0)? NULL : &inChannel[0];
        vector<int32_t>  expChannel(inChannel.size());

        if (cSize > 0) {
          int32_t mask = ~(-1 << cSize);
          for (size_t i = 0; i < expChannel.size(); i++) {
            expChannel[i] = inChannel[i] & mask;
          }
        }

        // ---- INTEGER TESTS ------------------------------------------------
        for (size_t dSizeIdx = 0; dSizeIdx < dSizeOptions.size(); dSizeIdx++) {
          int32_t dSize        = dSizeOptions[dSizeIdx];
          int32_t minFieldSize = max(0,eSize) + max(0,cSize) + dSize;

          for (int32_t fSize = minFieldSize; fSize <= 64; fSize++) {
            if (QUICK_TEST && (fSize != dSize) && (fSize != minFieldSize)
                           && (Utilities::binarySearch(dSizeOptions, fSize) < 0)) {
              continue;
            }

            for (int32_t sign = 0; sign <= 1; sign++) {
              for (int32_t proc = 0; proc <= 1; proc++) {
                DataItemFormat  format = (sign == 0)? DataItemFormat_UnsignedInt : DataItemFormat_SignedInt;
                RealComplexType real   = RealComplexType_Real;
                PayloadFormat   pf((proc!=0), real, format, false, max(0,eSize), max(0,cSize), fSize, dSize, 1, 1);
                string          pfStr  = pf.toString();
                int32_t         maxLen = (int32_t)(inLong.size() * 64); // maximum possible buffer length

                vector<char> outByte(offset+maxLen);
                vector<char> outShort(offset+maxLen);
                vector<char> outInt(offset+maxLen);
                vector<char> outLong(offset+maxLen);
                vector<char> outFloat(offset+maxLen);
                vector<char> outDouble(offset+maxLen);

                packAsByte(  pf, &outByte[0],   offset, &inByte[0],   chan, evt, (int32_t)inByte.size()  );
                packAsShort( pf, &outShort[0],  offset, &inShort[0],  chan, evt, (int32_t)inShort.size() );
                packAsInt(   pf, &outInt[0],    offset, &inInt[0],    chan, evt, (int32_t)inInt.size()   );
                packAsLong(  pf, &outLong[0],   offset, &inLong[0],   chan, evt, (int32_t)inLong.size()  );
                packAsFloat( pf, &outFloat[0],  offset, &inFloat[0],  chan, evt, (int32_t)inFloat.size() );
                packAsDouble(pf, &outDouble[0], offset, &inDouble[0], chan, evt, (int32_t)inDouble.size());

                vector<int64_t> expByte(inByte.size());
                vector<int64_t> expShort(inShort.size());
                vector<int64_t> expInt(inInt.size());
                vector<int64_t> expLong(inLong.size());
                vector<int64_t> expFloat(inFloat.size());
                vector<int64_t> expDouble(inDouble.size());

                int32_t shift = 64 - dSize;
                int64_t mask  = (dSize == 64)? __INT64_C(-1) : ~((__INT64_C(-1)) << dSize);
                for (size_t i = 0; i < expLong.size(); i++) {
                  if (sign == 0) {
                    if (i < expByte.size()  ) expByte[i]   = ((inLong[i] & mask & __INT64_C(  0x000000FF)));
                    if (i < expShort.size() ) expShort[i]  = ((inLong[i] & mask & __INT64_C(  0x0000FFFF)));
                    if (i < expFloat.size() ) expFloat[i]  = ((inLong[i] & mask & __INT64_C(  0x00FFFFFF)));
                    if (i < expInt.size()   ) expInt[i]    = ((inLong[i] & mask & __INT64_C(  0xFFFFFFFF)));
                    if (i < expDouble.size()) expDouble[i] = ((inLong[i] & mask & __INT64_C(0xFFFFFFFFFF)));
                    if (i < expLong.size()  ) expLong[i]   = ((inLong[i] & mask                          ));
                  }
                  else {
                    if (i < expByte.size()  ) expByte[i]   = (((int64_t)inByte[i]  ) << shift) >> shift;
                    if (i < expShort.size() ) expShort[i]  = (((int64_t)inShort[i] ) << shift) >> shift;
                    if (i < expInt.size()   ) expInt[i]    = (((int64_t)inInt[i]   ) << shift) >> shift;
                    if (i < expLong.size()  ) expLong[i]   = (((int64_t)inLong[i]  ) << shift) >> shift;
                    if (i < expFloat.size() ) expFloat[i]  = (((inLong[i] & mask & __INT64_C(  0x00FFFFFF))) << shift) >> shift;
                    if (i < expDouble.size()) expDouble[i] = (((inLong[i] & mask & __INT64_C(0xFFFFFFFFFF))) << shift) >> shift;
                  }
                }

                vector<int64_t> actByte(inByte.size());
                vector<int64_t> actShort(inShort.size());
                vector<int64_t> actInt(inInt.size());
                vector<int64_t> actLong(inLong.size());
                vector<int64_t> actFloat(inFloat.size());
                vector<int64_t> actDouble(inDouble.size());

                vector<int32_t> evtByte(inByte.size());
                vector<int32_t> evtShort(inShort.size());
                vector<int32_t> evtInt(inInt.size());
                vector<int32_t> evtLong(inLong.size());
                vector<int32_t> evtFloat(inFloat.size());
                vector<int32_t> evtDouble(inDouble.size());

                vector<int32_t> chanByte(inByte.size());
                vector<int32_t> chanShort(inShort.size());
                vector<int32_t> chanInt(inInt.size());
                vector<int32_t> chanLong(inLong.size());
                vector<int32_t> chanFloat(inFloat.size());
                vector<int32_t> chanDouble(inDouble.size());

                unpackAsLong(pf, &outByte[0],   offset, &actByte[0],   &chanByte[0],   &evtByte[0],   (int32_t)actByte.size()  );
                unpackAsLong(pf, &outShort[0],  offset, &actShort[0],  &chanShort[0],  &evtShort[0],  (int32_t)actShort.size() );
                unpackAsLong(pf, &outInt[0],    offset, &actInt[0],    &chanInt[0],    &evtInt[0],    (int32_t)actInt.size()   );
                unpackAsLong(pf, &outLong[0],   offset, &actLong[0],   &chanLong[0],   &evtLong[0],   (int32_t)actLong.size()  );
                unpackAsLong(pf, &outFloat[0],  offset, &actFloat[0],  &chanFloat[0],  &evtFloat[0],  (int32_t)actFloat.size() );
                unpackAsLong(pf, &outDouble[0], offset, &actDouble[0], &chanDouble[0], &evtDouble[0], (int32_t)actDouble.size());

                assertArrayEquals("packAsByte(..) values for "+pfStr,   expByte,   0, expByte.size(),   actByte,   0, actByte.size()  );
                assertArrayEquals("packAsShort(..) values for "+pfStr,  expShort,  0, expShort.size(),  actShort,  0, actShort.size() );
                assertArrayEquals("packAsInt(..) values for "+pfStr,    expInt,    0, expInt.size(),    actInt,    0, actInt.size()   );
                assertArrayEquals("packAsLong(..) values for "+pfStr,   expLong,   0, expLong.size(),   actLong,   0, actLong.size()  );
                assertArrayEquals("packAsFloat(..) values for "+pfStr,  expFloat,  0, expFloat.size(),  actFloat,  0, actFloat.size() );
                assertArrayEquals("packAsDouble(..) values for "+pfStr, expDouble, 0, expDouble.size(), actDouble, 0, actDouble.size());

                assertArrayEquals("packAsByte(..) channel tags for "+pfStr,   expChannel, 0, expByte.size(),   chanByte,   0, chanByte.size()  );
                assertArrayEquals("packAsShort(..) channel tags for "+pfStr,  expChannel, 0, expShort.size(),  chanShort,  0, chanShort.size() );
                assertArrayEquals("packAsInt(..) channel tags for "+pfStr,    expChannel, 0, expInt.size(),    chanInt,    0, chanInt.size()   );
                assertArrayEquals("packAsLong(..) channel tags for "+pfStr,   expChannel, 0, expLong.size(),   chanLong,   0, chanLong.size()  );
                assertArrayEquals("packAsFloat(..) channel tags for "+pfStr,  expChannel, 0, expFloat.size(),  chanFloat,  0, chanFloat.size() );
                assertArrayEquals("packAsDouble(..) channel tags for "+pfStr, expChannel, 0, expDouble.size(), chanDouble, 0, chanDouble.size());

                assertArrayEquals("packAsByte(..) event tags for "+pfStr,   expEvent, 0, expByte.size(),   evtByte,   0, evtByte.size()  );
                assertArrayEquals("packAsShort(..) event tags for "+pfStr,  expEvent, 0, expShort.size(),  evtShort,  0, evtShort.size() );
                assertArrayEquals("packAsInt(..) event tags for "+pfStr,    expEvent, 0, expInt.size(),    evtInt,    0, evtInt.size()   );
                assertArrayEquals("packAsLong(..) event tags for "+pfStr,   expEvent, 0, expLong.size(),   evtLong,   0, evtLong.size()  );
                assertArrayEquals("packAsFloat(..) event tags for "+pfStr,  expEvent, 0, expFloat.size(),  evtFloat,  0, evtFloat.size() );
                assertArrayEquals("packAsDouble(..) event tags for "+pfStr, expEvent, 0, expDouble.size(), evtDouble, 0, evtDouble.size());
              }
            }
          }
        }

        // ---- IEEE-754 FLOAT TESTS -----------------------------------------
        int32_t fminFieldSize = max(0,eSize) + max(0,cSize) + 32;
        for (int32_t fSize = fminFieldSize; fSize <= 64; fSize++) {
          if (QUICK_TEST && (fSize != 32) && (fSize != fminFieldSize)
                         && (Utilities::binarySearch(dSizeOptions, fSize) < 0)) {
            continue;
          }

          for (int32_t proc = 0; proc <= 1; proc++) {
            DataItemFormat  format = DataItemFormat_Float;
            RealComplexType real   = RealComplexType_Real;
            PayloadFormat   pf(    (proc!=0), real, format,                   false, max(0,eSize), max(0,cSize), fSize, 32, 1, 1);
            PayloadFormat   pfBits((proc!=0), real, DataItemFormat_SignedInt, false, max(0,eSize), max(0,cSize), fSize, 32, 1, 1);
            string          pfStr  = pf.toString();

            vector<char>    out(offset + SPECIAL_FLOAT_COUNT*8);
            vector<int32_t> act(SPECIAL_FLOAT_COUNT);
            vector<int32_t> actEvt(SPECIAL_FLOAT_COUNT);
            vector<int32_t> actChan(SPECIAL_FLOAT_COUNT);

            packAsFloat(pf,     &out[0], offset, SPECIAL_FLOAT_VALS, &chan[0],    &evt[0],    (int32_t)fexp.size());
            unpackAsInt(pfBits, &out[0], offset, &act[0],            &actChan[0], &actEvt[0], (int32_t)fexp.size());

            for (size_t i = 0; i < fexp.size(); i++) {
              // Normalize the NaN values
              if ((act[i] >= 0x7F800001) && (act[i] <= 0x7FFFFFFF)) act[i] = 0x7FC00000;
            }

            assertArrayEquals("packAsFloat(..) values for "+pfStr,       fexp,       0, fexp.size(), act,     0, fexp.size());
            assertArrayEquals("packAsFloat(..) event tags for "+pfStr,   expEvent,   0, fexp.size(), actEvt,  0, fexp.size());
            assertArrayEquals("packAsFloat(..) channel tags for "+pfStr, expChannel, 0, fexp.size(), actChan, 0, fexp.size());
          }
        }

        // ---- IEEE-754 DOUBLE TESTS ----------------------------------------
        int32_t dminFieldSize = max(0,eSize) + max(0,cSize) + 64;
        for (int32_t fSize = dminFieldSize; fSize <= 64; fSize++) {
          for (int32_t proc = 0; proc <= 1; proc++) {
            DataItemFormat  format = DataItemFormat_Double;
            RealComplexType real   = RealComplexType_Real;
            PayloadFormat   pf(    (proc!=0), real, format,                   false, max(0,eSize), max(0,cSize), fSize, 64, 1, 1);
            PayloadFormat   pfBits((proc!=0), real, DataItemFormat_SignedInt, false, max(0,eSize), max(0,cSize), fSize, 64, 1, 1);
            string          pfStr  = pf.toString();

            vector<char>    out(offset + SPECIAL_DOUBLE_COUNT*8);
            vector<int64_t> act(SPECIAL_DOUBLE_COUNT);
            vector<int32_t> actEvt(SPECIAL_DOUBLE_COUNT);
            vector<int32_t> actChan(SPECIAL_DOUBLE_COUNT);

            packAsDouble(pf,     &out[0], offset, SPECIAL_DOUBLE_VALS, &chan[0],    &evt[0],    (int32_t)dexp.size());
            unpackAsLong(pfBits, &out[0], offset, &act[0],             &actChan[0], &actEvt[0], (int32_t)dexp.size());

            for (size_t i = 0; i < dexp.size(); i++) {
              // Normalize the NaN values
              if ((act[i] >= __INT64_C(0x7FF0000000000001)) && (act[i] <= __INT64_C(0x7FFFFFFFFFFFFFFF))) act[i] = __INT64_C(0x7FF8000000000000);
            }

            assertArrayEquals("packAsDouble(..) values for "+pfStr,       dexp,       0, dexp.size(), act,     0, dexp.size());
            assertArrayEquals("packAsDouble(..) event tags for "+pfStr,   expEvent,   0, dexp.size(), actEvt,  0, dexp.size());
            assertArrayEquals("packAsDouble(..) channel tags for "+pfStr, expChannel, 0, dexp.size(), actChan, 0, dexp.size());
          }
        }

        // ---- VRT FLOAT TESTS ----------------------------------------------
        for (int32_t fmt = 0; fmt <= 31; fmt++) {
          DataItemFormat format = (DataItemFormat)fmt;
          int32_t        eBits  = getExponentBits(format);
          if (eBits < 0) continue; // Not VRT float type

          for (size_t dSizeIdx = 0; dSizeIdx < dSizeOptions.size(); dSizeIdx++) {
            int32_t dSize = dSizeOptions[dSizeIdx];
            if (dSize < eBits+1) continue; // Not valid data size

            int32_t minFieldSize = max(0,eSize) + max(0,cSize) + dSize;
            for (int32_t fSize = minFieldSize; fSize <= 64; fSize++) {
              if (QUICK_TEST && (fSize != dSize) && (fSize != minFieldSize)
                             && (Utilities::binarySearch(dSizeOptions, fSize) < 0)) {
                continue;
              }
              for (int32_t proc = 0; proc <= 1; proc++) {
                RealComplexType real  = RealComplexType_Real;
                PayloadFormat   pf((proc!=0), real, format, false, max(0,eSize), max(0,cSize), fSize, dSize, 1, 1);
                string          pfStr = pf.toString();

                vector<char>    out(offset + inVRT.size()*8);
                vector<int64_t> expBits(inVRT.size());
                vector<double > expVals(inVRT.size());
                vector<double > actVals(inVRT.size());
                vector<int32_t> actEvt(inVRT.size());
                vector<int32_t> actChan(inVRT.size());

                for (size_t i = 0; i < expBits.size(); i++) {
                  expBits[i] = VRTMath::toVRTFloat(format, dSize, inVRT[i]);
                  expVals[i] = VRTMath::fromVRTFloat(format, dSize, expBits[i]);
                }

                packAsDouble(  pf, &out[0], offset, &inVRT[0],   &chan[0],    &evt[0],    (int32_t)inVRT.size());
                unpackAsDouble(pf, &out[0], offset, &actVals[0], &actChan[0], &actEvt[0], (int32_t)inVRT.size());

                assertArrayEquals("packAsDouble(..) values for "+pfStr,       expVals,    0, expVals.size(),   actVals, 0, expVals.size());
                assertArrayEquals("packAsDouble(..) channel tags for "+pfStr, expChannel, 0, expVals.size(),   actChan, 0, expVals.size());
                assertArrayEquals("packAsDouble(..) event tags for "+pfStr,   expEvent,   0, expVals.size(),   actEvt,  0, expVals.size());
              }
            }
          }
        }
      }
    }
  }
}

void PackUnpackTest::testUnpack () {
  // ==== TEST CASES =========================================================
  for (int offset = 0; offset <= 512; offset+=512) {
    // ---- INPUT VALUES -----------------------------------------------------
    vector<char> buf(offset);
    for (int32_t i = 0x00; i <= 0xFF; i++) {
      buf.push_back((char)i);
    }

    vector<char> fbuf(offset+SPECIAL_FLOAT_COUNT*4);
    vector<char> dbuf(offset+SPECIAL_FLOAT_COUNT*8);

    for (size_t i = 0; i < SPECIAL_FLOAT_COUNT; i++) {
      VRTMath::packFloat( fbuf, (int32_t)(offset+i*4), SPECIAL_FLOAT_VALS[i]);
      VRTMath::packDouble(dbuf, (int32_t)(offset+i*8), SPECIAL_DOUBLE_VALS[i]);
    }

    // ---- INTEGER TESTS ----------------------------------------------------
    for (int32_t dSize = 1; dSize <= 64; dSize++) {
      for (int32_t fSize = dSize; fSize <= 64; fSize++) {
        for (int32_t sign = 0; sign <= 1; sign++) {
          for (int32_t proc = 0; proc <= 1; proc++) {
            DataItemFormat  format = (sign == 0)? DataItemFormat_UnsignedInt : DataItemFormat_SignedInt;
            RealComplexType real   = RealComplexType_Real;
            PayloadFormat   pf((proc!=0), real, format, false, 0, 0, fSize, dSize, 1, 1);
            string          pfStr  = pf.toString();
            int32_t         count;

                 if (proc  ==  0) count = 256*8 / fSize;
            else if (fSize <= 32) count = 256*8 / 32 * (32 / fSize);
            else                  count = 256*8 / 64 * (64 / fSize);

            vector<int8_t > expByte(count);
            vector<int16_t> expShort(count);
            vector<float  > expFloat(count);
            vector<int32_t> expInt(count);
            vector<double > expDouble(count);
            vector<int64_t> expLong(count);

            for (int32_t i=0,bitOffset=0; i < count; i++,bitOffset+=fSize) {
              if (proc != 0) {
                int32_t avail = (fSize <= 32)? 32 - (bitOffset%32)
                                             : 64 - (bitOffset%64);
                if (avail < fSize) {
                  bitOffset += avail;
                }
              }
              int64_t bits = getBits(&buf[0], offset*8+bitOffset, dSize, (sign==1));

              expByte[i]   = (int8_t )bits;
              expShort[i]  = (int16_t)bits;
              expInt[i]    = (int32_t)bits;
              expLong[i]   = (int64_t)bits;
              expFloat[i]  = (sign == 0)? (float  )((uint64_t)bits) : (float  )bits;
              expDouble[i] = (sign == 0)? (double )((uint64_t)bits) : (double )bits;
            }

            vector<int8_t > actByte(count);
            vector<int16_t> actShort(count);
            vector<float  > actFloat(count);
            vector<int32_t> actInt(count);
            vector<double > actDouble(count);
            vector<int64_t> actLong(count);

            unpackAsByte(  pf, &buf[0], offset, &actByte[0],   NULL, NULL, count);
            unpackAsShort( pf, &buf[0], offset, &actShort[0],  NULL, NULL, count);
            unpackAsInt(   pf, &buf[0], offset, &actInt[0],    NULL, NULL, count);
            unpackAsLong(  pf, &buf[0], offset, &actLong[0],   NULL, NULL, count);
            unpackAsFloat( pf, &buf[0], offset, &actFloat[0],  NULL, NULL, count);
            unpackAsDouble(pf, &buf[0], offset, &actDouble[0], NULL, NULL, count);

            assertArrayEquals(string("unpackAsByte(..) for ")+pfStr,   expByte,   0, count, actByte,   0, count);
            assertArrayEquals(string("unpackAsShort(..) for ")+pfStr,  expShort,  0, count, actShort,  0, count);
            assertArrayEquals(string("unpackAsInt(..) for ")+pfStr,    expInt,    0, count, actInt,    0, count);
            assertArrayEquals(string("unpackAsLong(..) for ")+pfStr,   expLong,   0, count, actLong,   0, count);
            assertArrayEquals(string("unpackAsFloat(..) for ")+pfStr,  expFloat,  0, count, actFloat,  0, count);
            assertArrayEquals(string("unpackAsDouble(..) for ")+pfStr, expDouble, 0, count, actDouble, 0, count);
          }
        }
      }
    }

    // ---- UNSIGNED TESTS ---------------------------------------------------
    // Special test cases added for VRT-40 to make sure conversions of unsigned
    // 32/64-bit numbers work properly.
    if (true) {
      char bbuf[16] = { (char)0x80, (char)0x81, (char)0x82, (char)0x83,
                        (char)0x84, (char)0x85, (char)0x86, (char)0x87,
                        (char)0x88, (char)0x89, (char)0x8A, (char)0x8B,
                        (char)0x8C, (char)0x8D, (char)0x8E, (char)0x8F };

      vector<int64_t> expLong32(3); /* only 3 because last is undefined */
      vector<float  > expFloat32(4);
      vector<double > expDouble32(4);
      vector<float  > expFloat64(2);
      vector<double > expDouble64(2);

      expLong32[0]   = __INT64_C(2155971203);
      expLong32[1]   = __INT64_C(2223343239);
      expLong32[2]   = __INT64_C(2290715275);
      expFloat32[0]  = (float)2155971203.0;
      expFloat32[1]  = (float)2223343239.0;
      expFloat32[2]  = (float)2290715275.0;
      expFloat32[3]  = (float)2358087311.0;
      expDouble32[0] = 2155971203.0;
      expDouble32[1] = 2223343239.0;
      expDouble32[2] = 2290715275.0;
      expDouble32[3] = 2358087311.0;
      expFloat64[0]  = (float)9259825810226120327.0;
      expFloat64[1]  = (float)9838547192930733711.0;
      expDouble64[0] = 9259825810226120327.0;
      expDouble64[1] = 9838547192930733711.0;

      vector<int64_t> actLong32(3);
      vector<float  > actFloat32(4);
      vector<double > actDouble32(4);
      vector<float  > actFloat64(2);
      vector<double > actDouble64(2);

      unpackAsLong(  PayloadFormat_UINT32, bbuf, 0, &actLong32[0],   NULL, NULL, 3);
      unpackAsFloat( PayloadFormat_UINT32, bbuf, 0, &actFloat32[0],  NULL, NULL, 4);
      unpackAsDouble(PayloadFormat_UINT32, bbuf, 0, &actDouble32[0], NULL, NULL, 4);
      unpackAsFloat( PayloadFormat_UINT64, bbuf, 0, &actFloat64[0],  NULL, NULL, 2);
      unpackAsDouble(PayloadFormat_UINT64, bbuf, 0, &actDouble64[0], NULL, NULL, 2);

      assertArrayEquals("unpackAsLong(..) for UINT32",   expLong32,   0, 3, actLong32,   0, 3);
      assertArrayEquals("unpackAsFloat(..) for UINT32",  expFloat32,  0, 4, actFloat32,  0, 4);
      assertArrayEquals("unpackAsDouble(..) for UINT32", expDouble32, 0, 4, actDouble32, 0, 4);
      assertArrayEquals("unpackAsFloat(..) for UINT64",  expFloat64,  0, 2, actFloat64,  0, 2);
      assertArrayEquals("unpackAsDouble(..) for UINT64", expDouble64, 0, 2, actDouble64, 0, 2);
    }

    // ---- FLOAT TESTS (1 of 2) ---------------------------------------------
    // Test float with special values in FLOAT32 format
    if (true) {
      PayloadFormat pf    = PayloadFormat_FLOAT32;
      string        pfStr = pf.toString();
      vector<float> act(SPECIAL_FLOAT_COUNT);

      unpackAsFloat(pf, &fbuf[0], offset, &act[0], NULL, NULL, (int32_t)SPECIAL_FLOAT_COUNT);
      assertArrayEquals(string("unpackAsFloat(..) for ")+pfStr, &SPECIAL_FLOAT_VALS[0], SPECIAL_FLOAT_COUNT, &act[0], act.size());
    }

    // ---- DOUBLE TESTS -----------------------------------------------------
    // Test double with special values in DOUBLE64 format
    if (true) {
      PayloadFormat  pf    = PayloadFormat_DOUBLE64;
      string         pfStr = pf.toString();
      vector<double> act(SPECIAL_FLOAT_COUNT);

      unpackAsDouble(pf, &dbuf[0], offset, &act[0], NULL, NULL, (int32_t)SPECIAL_FLOAT_COUNT);
      assertArrayEquals(string("unpackAsDouble(..) for ")+pfStr, &SPECIAL_DOUBLE_VALS[0], SPECIAL_DOUBLE_COUNT, &act[0], act.size());
    }

    // ---- FLOAT TESTS (2 of 2) ---------------------------------------------
    // Test float with various values and differing field size, presume that unpackAsInt(..)
    // works and base the expected float value on that.
    for (int32_t fSize = 32; fSize <= 64; fSize++) {
      for (int32_t proc = 0; proc <= 1; proc++) {
        RealComplexType real    = RealComplexType_Real;
        PayloadFormat   pfFloat((proc!=0), real, DataItemFormat_Float,     false, 0, 0, fSize, 32, 1, 1);
        PayloadFormat   pfInt(  (proc!=0), real, DataItemFormat_SignedInt, false, 0, 0, fSize, 32, 1, 1);
        string          pfStr   = pfFloat.toString();
        int32_t         count;

             if (proc  ==  0) count = 256*8 / fSize;
        else if (fSize <= 32) count = 256*8 / 32 * (32 / fSize);
        else                  count = 256*8 / 64 * (64 / fSize);

        vector<float> expFloat(count);
        vector<float> actFloat(count);
        vector<int  > actInt(count);

        unpackAsFloat(pfFloat, &buf[0], 0, &actFloat[0], NULL, NULL, count);
        unpackAsInt(  pfInt,   &buf[0], 0, &actInt[0],   NULL, NULL, count);

        for (size_t i = 0; i < expFloat.size(); i++) {
          expFloat[i] = VRTMath::intBitsToFloat(actInt[i]);
        }

        assertArrayEquals(string("unpackAsFloat(..) for ")+pfStr, expFloat, 0, count, actFloat, 0, count);
      }
    }

    // ---- VRT FLOAT TESTS --------------------------------------------------
    for (int32_t fmt = 0; fmt <= 31; fmt++) {
      DataItemFormat format = (DataItemFormat)fmt;
      int32_t        eBits  = getExponentBits(format);
      if (eBits < 0) continue; // Not VRT float type

      for (int32_t dSize = 1+eBits; dSize <= 64; dSize++) {
        for (int32_t fSize = dSize; fSize <= dSize; fSize++) {
          for (int32_t proc = 0; proc <= 1; proc++) {
            RealComplexType real   = RealComplexType_Real;
            PayloadFormat   pf((proc!=0), real, format, false, 0, 0, fSize, dSize, 1, 1);
            string          pfStr  = pf.toString();
            int32_t         count;

                 if (proc  ==  0) count = 256*8 / fSize;
            else if (fSize <= 32) count = 256*8 / 32 * (32 / fSize);
            else                  count = 256*8 / 64 * (64 / fSize);

            vector<double> exp(count);
            vector<double> act(count);

            for (int i=0,bitOffset=0; i < count; i++,bitOffset+=fSize) {
              if (proc != 0) {
                int32_t avail = (fSize <= 32)? 32 - (bitOffset%32)
                                             : 64 - (bitOffset%64);
                if (avail < fSize) {
                  bitOffset += avail;
                }
              }
              int64_t bits = getBits(&buf[0], offset*8+bitOffset, dSize, false);

              exp[i] = VRTMath::fromVRTFloat(format, dSize, bits);
            }

            unpackAsDouble(pf, &buf[0], offset, &act[0], NULL, NULL, count);
            assertArrayEquals(string("unpackAsDouble(..) for ")+pfStr, exp, 0, count, act, 0, count);
          }
        }
      }
    }
  }
}
