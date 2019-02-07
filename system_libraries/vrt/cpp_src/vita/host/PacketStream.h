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

#ifndef _PacketStream_h
#define _PacketStream_h
#define CUTMODE_DATE 0x8000

#include "VRTObject.h"
#include "Utilities.h"
#include "BasicContextPacket.h"
#include "BasicVRTState.h"
#include "PSUtilities.h"
#include "VRTWriter.h"
#include "StandardDataPacket.h"
#include <string>

#include <headers.h>
#include <xmtime.h>
#include "vrtio.h"

typedef struct field_mapping {
  int32_t vrt_field;
  char    blue_keyword[50];
} field_mapping_t;

namespace private_PacketStream{
  static const vector<char> EMPTY(4096);
}

using namespace private_PacketStream;
using namespace std;
using namespace vrt;

typedef struct {
  const char * midasKeyword;
  const char * vrtField;
  bool addIfMissing;
} midasKeywordMapping;

static string nullValue = "NULL";

/** <b>Internal use only:</b> Used internal to {@link sourcevrt} and {@link sinkvrt}. */
class PacketStream : public VRTObject {
private: map<string,string> vrtToBlueKeywordMapping;
private: map<string,string> blueToVrtKeywordMapping;

  private: HEADER             hcb;
  public:  double             transferLengthElem;  // Transfer length in elements
  private: CPHEADER           pipe;
  public:  int32_t            transferLengthBytes; // Transfer length in bytes
  public:  int32_t            defTransferLength;   // Default transfer length in bytes
  public:  int32_t            streamID;
  public:  bool               packetMode;
  public:  bool               force1000;
  public:  TimeStamp          lastPacketTimeStamp;
  public:  TimeStamp          nextPacketTimeStamp;
  public:  int32_t            counter;
  public:  bool               newContext;
  private: bool               initialized;
  private: double             archivePeriod;
  private: int32_t            archiveCutNo;
  private: TimeStamp          lastCut;
  private: TimeStamp          firstCut;
  private: TimeStamp          nextCut;
  private: string             outFileName;
  private: BasicContextPacket context;
  private: BasicVRTState      currentState;
  private: vector<char>       dataBuffer; //(BasicVRTPacket::MAX_PACKET_LENGTH)
  private: XMValue            keyBuffer;
  private: bool               exactTopOfSecond;
  private: bool               openOnNextDataPacket;
  private: bool               firstDataPacketReceived;
  private: string             keywords;
  private: int32_t            numFiles;
  private: bool               debug;
  private: map<string,string> fieldMapping;
  private: bool               datastreamInitialized;
  private: bool               writeActive;
  private: int32_t            archiveMissingSamples;
  private: int32_t            totalMissingSamples;

  private: TimeStamp lastTs;

  public: static map<string,string> getDefaultFieldMap();

  /** Default destructor. */
  public: ~PacketStream ();

  /** Default constructor. */
  public: PacketStream ();

  /** Creates a new BLUE-to-VRT stream and opens the input pipe.
   *  @param in         The input pipe (not open yet).
   *  @param streamID   The VRT stream ID.
   *  @param force1000  Force treatment as a Type 1000 file?
   *  @param packetMode Use packet mode?
   *  @param xfer       Default transfer length in bytes.
   *  @param debug      Enables extra debug output
   */
  public: PacketStream (const string &in, int32_t streamID, bool force1000, bool packetMode, int32_t xfer, bool debug = false, map<string,string> fieldMapping=getDefaultFieldMap());

  /** Creates a new VRT-to-BLUE stream and opens the output pipe.
   *  @param out        The output pipe (not open yet).
   *  @param streamID   The VRT stream ID.
   *  @param force1000  Force treatment as a Type 1000 file?
   *  @param packetMode Use packet mode?
   *  @param archivePeriod length of archive files in seconds, integers will cause packets to be split at top of second
   *  @param ctx        The context packet defining the input VRT stream.
   *  @param keywords   Name of result that contains keywords in table format
   *  @param debug      Enables extra debug output
   */
  public: PacketStream (const string &out, int32_t streamID, bool force1000, bool packetMode, double archivePeriod, const BasicContextPacket &ctx, string keywords = "", int32_t numFiles = -1, bool debug = false, bool exactTopOfSecond = false, map<string,string> fieldMapping=getDefaultFieldMap());

  /** Creates a new VRT-to-BLUE stream but does NOT initialize the internal details required for
   *  writing to the output pipe, the output pipe must be initialized by calling <tt>initialize(ctx)</tt>
   *  @param ctx        The context packet defining the input VRT stream.
   *  @param out        The output pipe (not open yet).
   *  @param streamID   The VRT stream ID.
   *  @param force1000  Force treatment as a Type 1000 file?
   *  @param packetMode Use packet mode?
   *  @param archivePeriod length of archive files in seconds, integers will cause packets to be split at top of second
   *  @param keywords   Name of result that contains keywords in table format
   *  @param debug      Enables extra debug output
   */
  public: PacketStream (const string &out, int32_t streamID, bool force1000, bool packetMode, double archivePeriod, string keywords = "", int32_t numFiles = -1, bool debug = false, bool exactTopOfSecond = false, map<string,string> fieldMapping=getDefaultFieldMap());

  /** Creates a new VRT-to-BLUE archival stream but does NOT initialize the internal details required for
   *  writing to the output pipe, the output pipe must be initialized by calling <tt>initialize(ctx)</tt>
   *  An archival directory and filename pattern must be provided. <br>
   *  <br>
   *  Archival pattern is identified with the following place-holders:
   *  <pre>
   *   ${TIME} - Time in "hhmmss" format.
   *   ${DATE] - Date in "yyyymmdd" format.
   *   ${FREQ} - Tuner frequency (rounded to the nearest hz).
   *   ${CUTNO} - Cut number.
   *  </pre>
   *  For example:
   *  <pre>
   *   "tuner1_${DATE}_${TIME}.prm" would result in a filename of "tuner1_20130305_165110.prm if a file was cut at
   *   16:51:10 on March 5, 2013.
   *
   *   Time, Date, and tuner Frequency are all taken from the VRT headers on the given stream.
   *   Cut number is simply a sequence number starting at 0 for the given PacketStream.
   *  </pre>
   *  @param out            The output pipe (not open yet).
   *  @param streamID       The VRT stream ID.
   *  @param force1000      Force treatment as a Type 1000 file?
   *  @param archiveDir     Directory for file archival
   *  @param archivePattern Pattern for filename creation
   *  @param cutMode        Cut mode for archival files
   *  @param cutPeriod      Period for cutting files in seconds.  Files will be cut to the nearest second, respecting
   *                        VRT packet borders.
   */
  private: void initKeywords();

  private: string generateFileName(const TimeStamp& timeStamp, string fileName) const;

  /** Opens a new archive file. */
  public: void openArchiveFile(const TimeStamp& timeStamp);

  /** Closes a new archive file. */
  public: void closeArchiveFile();

  /** Opens a VRT-to-BLUE archive. */
  private: void openVrtToBlueArchive (const string &archiveDir, const string &archivePattern);

  /** Opens a VRT-to-BLUE stream. */
  private: void openVrtToBlue (const string &out, const TimeStamp &ts);

  /** Opens a BLUE-to-VRT stream. */
  private: void openBlueToVrt (const string &in);

  private: void addDynamicKeywords();

  private: void addStaticKeyword(XMValue xmv, string key="");

  /** Closes any associated files. */
  public: void close ();

  /** Printable description of the object for debugging purposes. */
  public: string toString () const;

  /** Indicates if the stream is null. */
  public: bool isNullValue () const;

  /** Indicates if the stream is open. */
  public: inline bool isInitialized () const { return initialized; }

  /** Indicates if the stream is in archive mode. */
  public: inline bool isArchiveMode () const { return archivePeriod < 0; }

  /** Initialize the output details for a VRT-to-BLUE stream. */
  public: inline void initialize (const BasicContextPacket& ctx) {
    BasicContextPacket context(ctx);
    currentState.updateState(context);

    nextPacketTimeStamp = ctx.getTimeStamp();
    lastCut             = ctx.getTimeStamp();
    initialized   = true;
    if (archivePeriod > 0) {
//      openOnNextDataPacket = true;
    }
    else {
      openVrtToBlue(outFileName, nextPacketTimeStamp);
    }
  }

  public: inline bool updateState (const BasicContextPacket& ctx) {
    bool cut = currentState.updateState(ctx);

    if (cut && archivePeriod > 0) {
      closeArchiveFile();
      return false;
    }
    else if (cut) {
      close();

      m_warning("Closing pipe due to incompatible change to stream " + ctx.getStreamID()
          + " for " + pipe.file_name);
      return true;
    }

    return cut;
  }

  public: bool updateState (const TimeStamp& ts) {
    currentState.updateState(ts);
    return false;
  }

  public: bool updateState (const BasicDataPacket& data) {
    bool cut = currentState.updateState(data);

    if (cut && archivePeriod > 0) {
      closeArchiveFile();
    }
    else if (cut) {
      close();
      m_warning("Closing pipe due to incompatible change to stream "+ data.getStreamID()
          + " for " + pipe.file_name);
      return true;
    }

    return false;
  }

  public: bool updateState (const XMValue &v) { keyBuffer.update(v); return false; }

  /** Converts stream ID to stream name. */
  public: static inline string getStreamName (int32_t streamID) {
    if (streamID == ALLSTREAMS) return "ALLSTREAMS";
    char str[32];
    snprintf(str, 32, "STREAM%d", streamID);
    return str;
  }
  public: int64_t getMissingSamples(TimeStamp& ts);

  public: bool writeToPipe(PayloadFormat pf, TimeStamp ts, char* data, int32_t nelem, bool missing=false);

  public: bool writeToPipe(const BasicDataPacket& dp, bool fillMode);

  /**  Writes data from the packet to the pipe. */
  public: bool writeToPipe (const BasicVRTPacket& p, bool packetMode, bool fillMode);

  public: bool readFromPipe(bool packetMode, int32_t contextFrequency, VRTWriter* packetWriter);

  /** Resets the output details for a VRT-to-BLUE stream. The call to this
   *  method will reset <tt>isInitialized()</tt> to false and require a new
   *  call to <tt>initialize(..)</tt>.
   */
  public: inline void reset () {
    nextPacketTimeStamp = TimeStamp();
    initialized   = false;
//    reformat      = '-';
  }

  public: int64_t isTimeToCut(const TimeStamp& tc) const;

  public: inline BasicVRTState getState() const {
    return currentState;
  }

  public: inline void setStreamID(const int32_t& streamID) {
    this->streamID = streamID;
  }

};

#endif /* _PacketStream_h */
