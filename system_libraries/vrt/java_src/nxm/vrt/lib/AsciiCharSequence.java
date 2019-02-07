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

package nxm.vrt.lib;

import java.util.Arrays;

/** An immutable {@link CharSequence} that uses ASCII characters in a <tt>byte[]</tt>
 *  array for its source. Any non-ASCII characters are replaced by '?' (ASCII
 *  control characters will not be replaced by '?').
 */
public final class AsciiCharSequence implements CharSequence {
  // This class was substantially revised in Dec 2013 to mirror the changes to
  // java.lang.String made in Java 7 where the offset and length fields were
  // dropped in an effort to improve memory use and performance.

  private static final Byte QUESTION_MARK = '?';

  private static final AsciiCharSequence EMPTY_STRING
      = new AsciiCharSequence(new byte[0], 0, 0, "");

  private final     byte[] chars;  // The ASCII chars
  private transient String string; // The results of calling toString() saved for fast reuse

  /** Fast private constructor. */
  private AsciiCharSequence (byte[] chars, String string) {
    this.chars  = chars;
    this.string = string;
  }

  /** Fast private constructor. */
  private AsciiCharSequence (byte[] chars, int offset, int length, String string) {
    if (offset == 0) {
      this.chars  = (length == chars.length)? chars : Arrays.copyOf(chars, length);
      this.string = string;
    }
    else {
      this.chars  = new byte[length];
      this.string = string;
      System.arraycopy(chars, offset, this.chars, 0, length);
    }
  }

  /** Creates a new instance.
   *  @param chars   The ASCII chars in a <tt>byte[]</tt> array.
   *  @param offset  The offset into the <tt>chars</tt> array.
   *  @param length  The number of chars in the <tt>chars</tt> array starting at <tt>offset</tt>.
   *  @throws ArrayIndexOutOfBoundsException If <tt>offset</tt> or <tt>length</tt> are invalid.
   */
  public AsciiCharSequence (byte[] chars, int offset, int length) {
    this(chars, offset, length, false);
  }

  /** Creates a new instance.
   *  @param chars    The ASCII chars in a <tt>byte[]</tt> array.
   *  @param offset   The offset into the <tt>chars</tt> array.
   *  @param length   The number of chars in the <tt>chars</tt> array starting at <tt>offset</tt>.
   *  @param nullTerm If <tt>true</tt> assume a NUL-terminated string and reduce the length to N
   *                  where N is the index of the first NUL (if any).
   *  @throws ArrayIndexOutOfBoundsException If <tt>offset</tt> or <tt>length</tt> are invalid.
   */
  public AsciiCharSequence (byte[] chars, int offset, int length, boolean nullTerm) {
    super();
    int max = chars.length - offset;

    if (offset <  0 ) throw new ArrayIndexOutOfBoundsException("Invalid offset ("+offset+")");
    if (length <  0 ) throw new ArrayIndexOutOfBoundsException("Invalid length ("+length+")");
    if (length > max) throw new ArrayIndexOutOfBoundsException("Invalid length ("+length+")");

    if (nullTerm) {
      for (int i=0, j=offset; i < length; i++,j++) {
        if (chars[j] == 0) length = i; // trim length at first null
      }
    }

    this.chars = new byte[length];

    for (int i=0,j=offset; i < length; i++,j++) {
      byte c = chars[j];
      this.chars[i] = (c < 0)? QUESTION_MARK : c;
    }
  }

  /** Creates a new instance, but does not copy the values. <b>This should only be used in cases
   *  where the values of <tt>chars[offset]</tt> through <tt>chars[offset+length-1]</tt> can be
   *  guaranteed to be fixed throughout the life of this object. Failure to follow this will result
   *  in undefined behavior.</b>
   *  @param chars   The ASCII chars in a <tt>byte[]</tt> array.
   *  @param offset  The offset into the <tt>chars</tt> array.
   *  @param length  The number of chars in the <tt>chars</tt> array starting
   *                 at <tt>offset</tt>.
   *  @return An applicable {@link AsciiCharSequence}
   *  @throws ArrayIndexOutOfBoundsException If <tt>offset</tt> or <tt>length</tt> are invalid.
   *  @deprecated This function should have been marked "Internal Use Only" and
   *              has subsequently been replaced with a version that takes in
   *              an additional argument.
   */
  @Deprecated
  public static AsciiCharSequence wrap (byte[] chars, int offset, int length) {
    return wrap(chars, offset, length, false);
  }

  /** <b>Internal Use Only:</b> Creates a new instance, but does not copy the
   *  values and does not validate the input.
   *  @param chars   The ASCII chars in a <tt>byte[]</tt> array.
   *  @return An applicable {@link AsciiCharSequence}
   */
  public static AsciiCharSequence wrap (byte[] chars) {
    return new AsciiCharSequence(chars, null);
  }

  /** <b>Internal Use Only:</b> Creates a new instance, but does not copy the
   *  values unless required. <b>This should only be used in cases where the
   *  values of <tt>chars[offset]</tt> through <tt>chars[offset+length-1]</tt>
   *  can be guaranteed to be fixed throughout the life of this object. Failure
   *  to follow this will result in undefined behavior.</b>
   *  @param chars   The ASCII chars in a <tt>byte[]</tt> array.
   *  @param offset  The offset into the <tt>chars</tt> array.
   *  @param length  The number of chars in the <tt>chars</tt> array starting
   *                 at <tt>offset</tt>.
   *  @param nullTerm If <tt>true</tt> assume a NUL-terminated string and reduce
   *                  the length accordingly.
   *  @return An applicable {@link AsciiCharSequence}
   *  @throws ArrayIndexOutOfBoundsException If <tt>offset</tt> or <tt>length</tt> are invalid.
   */
  public static AsciiCharSequence wrap (byte[] chars, int offset, int length, boolean nullTerm) {
    if ((offset != 0) || (length != chars.length)) {
      // Can't use fast method
      return new AsciiCharSequence(chars, offset, length, nullTerm);
    }
    else {
      int max = chars.length - offset;

      if (offset <  0 ) throw new ArrayIndexOutOfBoundsException("Invalid offset ("+offset+")");
      if (length <  0 ) throw new ArrayIndexOutOfBoundsException("Invalid length ("+length+")");
      if (length > max) throw new ArrayIndexOutOfBoundsException("Invalid length ("+length+")");

      boolean ok = true;
      for (int i=0,j=offset; ok && (i < length); i++,j++) {
        if (nullTerm && (chars[j] == 0)) {
          length = i;
          break;
        }
        ok = chars[j] >= 0;
      }

      if (length == 0) return EMPTY_STRING;

      return (ok)? new AsciiCharSequence(chars, offset, length, null)
                 : new AsciiCharSequence(chars, offset, length, false);
    }
  }

  /** Converts a string to an ASCII string. This is identical to <tt>fromCharSequence(seq,(byte)'?')</tt>.
   *  @param seq The char sequence to convert.
   *  @return An applicable {@link AsciiCharSequence}
   */
  public static AsciiCharSequence fromCharSequence (CharSequence seq) {
    return fromCharSequence(seq, QUESTION_MARK);
  }

  /** Converts a string to an ASCII string.
   *  @param seq         The char sequence to convert.
   *  @param replacement The replacement character to use if a non-ASCII character is encountered
   *                     (typically '?').
   *  @return An applicable {@link AsciiCharSequence} or null if null was passed in.
   *  @throws IllegalArgumentException If <tt>replacement</tt> is null and a non-ASCII character is
   *          encountered.
   */
  public static AsciiCharSequence fromCharSequence (CharSequence seq, Byte replacement) {
    if (seq instanceof AsciiCharSequence) return (AsciiCharSequence)seq;
    if (seq == null) return null;

    boolean okString = (seq instanceof String);
    byte[] chars = new byte[seq.length()];
    for (int i = 0; i < chars.length; i++) {
      int c = seq.charAt(i);
      if (c <= 0x7F) {
        chars[i] = (byte)c;
      }
      else if (replacement != null) {
        chars[i] = replacement;
        okString = false;
      }
      else {
        throw new IllegalArgumentException("Non-ASCII string given '"+seq+"'");
      }
    }
    String str = (okString)? (String)seq : null;
    return new AsciiCharSequence(chars, 0, chars.length, str);
  }

  @Override public int  length   ()          { return chars.length; }
  @Override public char charAt   (int index) { return (char)chars[index]; }
  @Override public int  hashCode ()          { return toString().hashCode(); }

  /** <b>Internal Use Only:</b> Encodes this ASCII string into a sequence of
   *  bytes, but skips an array copy where possible. <b>This should only be used
   *  in cases where the values in the returned array can be guaranteed not to
   *  be modified. Failure to follow this will result in undefined behavior.</b>
   *  @return The bytes for the ASCII string.
   */
  public byte[] unwrap () {
    return chars;
  }

  /** Encodes this ASCII string into a sequence of bytes.
   *  @return The bytes for the ASCII string.
   */
  public byte[] getBytes () {
    return Arrays.copyOf(chars, chars.length);
  }

  @Override
  public CharSequence subSequence (int start, int end) {
    if ((start == 0) && (end == chars.length)) return this;
    if ((start < 0) || (end < start) || (end > chars.length)) {
      throw new IllegalArgumentException("Invalid start/end values for subsequence, "
                                        +"given "+start+" and "+end+" for a string "
                                        +"of length "+chars.length);
    }
    if (end == start) return EMPTY_STRING;
    String str = (string == null)? null : string.substring(start, end);
    return new AsciiCharSequence(chars, start, end - start, str);
  }

  @Override
  public String toString () {
    if (string == null) {
      string = new String(chars);
    }
    return string;
  }

  @Override
  public boolean equals (Object obj) {
    return (obj instanceof AsciiCharSequence)
        && Arrays.equals(chars, ((AsciiCharSequence)obj).chars);
  }

  /** Compares this {@link AsciiCharSequence} to an arbitrary {@link CharSequence}. This method
   *  will return <tt>true</tt> if all the character values represented by this object match the
   *  one passed in. <i>This method was modeled after {@link String#contentEquals(CharSequence)}.</i>
   *  @param  cs The sequence to compare this object.
   *  @return <tt>true</tt> if this object represents the same sequence of char values as the one
   *          passed in, otherwise <tt>false</tt> is returned.
   */
  public boolean contentEquals (CharSequence cs) {
    if (cs instanceof AsciiCharSequence) {
      return Arrays.equals(chars, ((AsciiCharSequence)cs).chars);
    }

    if (cs ==  null ) return false;
    if (cs ==  this ) return true;
    if (cs == string) return true;   // <-- must come after "if (cs == null)" test

    if (chars.length != cs.length()) return false;
    for (int i = 0; i < chars.length; i++) {
      if (chars[i] != cs.charAt(i)) return false;
    }
    return true;
  }
}
