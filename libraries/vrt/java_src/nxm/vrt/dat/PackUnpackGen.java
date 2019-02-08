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

package nxm.vrt.dat;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Long.toHexString;

/** <b>Internal Use Only:</b> Generates the applicable pack/unpack code based on the
 *  <tt>PackUnpackGen.code</tt> file. During processing the following replacements
 *  are made within the code:
 *  <pre>
 *    Token               |     Java Replacement     |     C++ Replacement      |
 *    --------------------+--------------------------+--------------------------+
 *    #CPP#               | false                    | true                     |
 *    #JAVA#              | true                     | false                    |
 *    #NATIVE#            | [9]                      | false                    |
 *    #native#            | [9]                      | *delete*                 |
 *    #flatten#           | *delete*                 | __attribute__((flatten)) |
 *    #parallel#          | *delete*                 | _Intel_Pragma("parallel")|
 *    #inline_recursive#  | *delete*                 | [0]                      |
 *    #namespace#         | *delete*                 | [1]                      |
 *    #loop1N#            | *delete*                 | [6,7]                    |
 *    #loopAB#            | *delete*                 | [7]                      |
 *    DataItemFormat_     | [8]                      | DataItemFormat_          |{@code
 *    >>>                 | >>>                      | >>                       |}
 *    const               | *delete*                 | const                    |
 *    final               | final                    | const                    |
 *    public static       | public static            | *delete*                 |
 *    private static      | private static           | static                   |
 *    inline              | *delete*                 | inline                   |
 *    unsigned type       | type                     | utype                    |
 *    byte[] buf          | byte[] buf               | [2]                      |
 *    #param buf          | #param buf               | [3]                      |
 *    type[] name         | type[] name              | type *name               |
 *    null                | null                     | NULL                     |
 *    IllegalArgumentError| new IllegalArgEx...      | VRTException             |
 *    UnsupportedError    | new UnsupportedOpEx...   | VRTException             |
 *    getExponentBits(x)  | x.getExponentBits()      | DataItemFormat_getExp... |
 *    UNUSED_VARIABLE(x)  | *delete*                 | UNUSED_VARIABLE(x)       |
 *    --------------------+--------------------------+--------------------------+
 *    #type#              | External array type   (lower case)                  |
 *    #Type#              | External array type   (upper case)                  |
 *    #typebytes#         | Number of bytes in given type                       |
 *    #typecheck#         | Number of bytes in given integer type [4]           |
 *    #form#              | Data format in buffer (lower case)                  |
 *    #Form#              | Data format in buffer (upper case)                  |
 *    #formbytes#         | Number of bytes in given format [5]                 |
 *    #formcheck#         | Number of bytes in given integer format [4]         |
 *    #formbits#          | Number of bits  in given format                     |
 *    #word#              | Data format for word (Int32 or Int64) (lower case)  |
 *    #Word#              | Data format for word (Int32 or Int64) (upper case)  |
 *    #wordbits#          | Number of bits  in given word (32 or 64)            |
 *    #wordbytes#         | Number of bytes in given word ( 4 or  8)            |
 *    #typemask#          | Bit mast for given type as a 64-bit value           |
 *    #mask#              | Bit mask for given format / container               |
 *    #container#         | Container type unpacked bits                        |
 *    #continerbits#      | Number of bits in container type                    |
 *    (#ct#)              | Container type if double (or float), else delete.   |
 *    --------------------+--------------------------+--------------------------+
 *
 *    Notes:
 *      [0] Replacement is intended to be '_Intel_Pragma("inline recursive")' but
 *          the current Intel compilers seem to take issue with it, so it is
 *          simply deleted for now.
 *      [1] "vrt::PackUnpack::" in .cc file, but omitted in .h file
 *      [2] Replaces with "void *ptr" and adds "char *buf = (char*)ptr" on next line
 *      [3] Replaces javadoc with "ptr" in place of "buf" and adds "pointer" to end
 *      [4] This will be -4 for float and -8 for double.
 *      [5] This will be -8 for bits and -2 for nibbles.
 *      [6] The replacement is '#loop1B#' where B is '8-#formbytes#'.
 *      [7] The replacement is '_Intel_Pragma("loop_count min=A, max=B")'.
 *      [8] Deleted if following a 'case' but changed to 'DataItemFormat.' if
 *          anywhere else.
 *      [9] If JNI code is being generated, the GENERIC functions will be inserted
 *          twice, once with #NATIVE# set to true (i.e. use delegate function)
 *          and #native# set to "" and another time with #NATIVE# set to false
 *          (i.e. use code below) and #native# set to "native_". When not using
 *          JNI #NATIVE# will be set false and #native# will be set to "" to cause
 *          all JNI-related code to be omitted.
 *  </pre>
 */
public final class PackUnpackGen {
  // NOTE:
  //  This file does not extend nxm.vrt.lib.Main since it is built and run
  //  prior to building the rest of the vrt.jar library (this file is used
  //  to generate Java code that the other parts of the library rely on).

  private PackUnpackGen () { } // prevent instantiation

  /** The various data types used. */
  private static enum DataType {
    Double(      "double",  "double",  "D", 8, 64,   -8),
    Float(       "float",   "float",   "F", 4, 32,   -4),
    Long(        "long",    "int64_t", "J", 8, 64,    8),
    Int(         "int",     "int32_t", "I", 4, 32,    4),
    Int24(       "#int24#", "#int24#", "-", 3, 24,    3),
    Short(       "short",   "int16_t", "S", 2, 16,    2),
    Int12(       "#int12#", "#int12#", "-", 0, 12,    0),
    Byte(        "byte",    "int8_t",  "B", 1,  8,    1),
    Nibble(      "#int4#",  "#int4#",  "-", 0,  4,    0),
    Bit(         "#int1#",  "#int1#",  "-", 0,  1,    0),
    Boolean(     "boolean", "bool",    "Z", 0,  1,    0),
    SignedVRT1(  "long",    "int64_t", "J", 0,  0, -100),
    SignedVRT2(  "long",    "int64_t", "J", 0,  0, -100),
    SignedVRT3(  "long",    "int64_t", "J", 0,  0, -100),
    SignedVRT4(  "long",    "int64_t", "J", 0,  0, -100),
    SignedVRT5(  "long",    "int64_t", "J", 0,  0, -100),
    SignedVRT6(  "long",    "int64_t", "J", 0,  0, -100),
    UnsignedVRT1("long",    "int64_t", "J", 0,  0, -100),
    UnsignedVRT2("long",    "int64_t", "J", 0,  0, -100),
    UnsignedVRT3("long",    "int64_t", "J", 0,  0, -100),
    UnsignedVRT4("long",    "int64_t", "J", 0,  0, -100),
    UnsignedVRT5("long",    "int64_t", "J", 0,  0, -100),
    UnsignedVRT6("long",    "int64_t", "J", 0,  0, -100);


    /** All the available values. */
    public static final DataType[] VALUES = values();

    /** All the available values. */
    public static final DataType[] VRT_VALUES = {
      SignedVRT1,   SignedVRT2,   SignedVRT3,   SignedVRT4,   SignedVRT5,   SignedVRT6,
      UnsignedVRT1, UnsignedVRT2, UnsignedVRT3, UnsignedVRT4, UnsignedVRT5, UnsignedVRT6,
    };

    final String java;      // Java type  name (e.g. int    )
    final String jni;       // JNI  type  name (e.g. jint   )
    final String cpp;       // C++  type  name (e.g. int32_t)
    final String jniCode;   // JNI  type  code (e.g. I      )
    final String javaClass; // Java class name (e.g. Integer)
    final int    bytes;
    final int    bits;
    final int    check;

    /** Constructor.
     *  @param java    The Java type.
     *  @param cpp     The C++ type.
     *  @param jniCode The JNI code.
     *  @param bytes   The number of bytes (0 if a VRT type or not an integral number of bytes).
     *  @param bits    The number of bits (-N if a VRT float type).
     *  @param check   The type-check code (number of bytes for integer types,
     *                 number of bytes * -1 for IEEE-754 float, -100 for VRT float).
     */
    DataType (String java, String cpp, String jniCode, int bytes, int bits, int check) {
      this.java      = java;
      this.jni       = "j"+java;
      this.cpp       = cpp;
      this.jniCode   = jniCode;
      this.javaClass = (java.equals("int"))? "Integer" : toString();
      this.bytes     = bytes;
      this.bits      = bits;
      this.check     = check;
    }

    /** Is this a native type? */
    public boolean isNativeType () {
      return (bytes != 0) && (this != DataType.Int24);
    }

    /** Is this a float/double value? */
    public boolean isFloat () {
      return (this == DataType.Double) || (this == DataType.Float);
    }

    /** Gets the applicable mask value. */
    public long getMask () {
      return 0xFFFFFFFFFFFFFFFFL >>> (64 - bits);
    }

    /** Gets the applicable type check value value. */
    public int getTypeCheck () {
      return check;
    }

    /** Converts Java type to DataType. */
    public static DataType forJava (String type) {
      for (DataType dt : VALUES) {
        if (dt.java.equals(type)) return dt;
      }
      throw new IllegalArgumentException("Unexpected type '"+type+"'");
    }

    /** Converts C++ type to DataType. */
    public static DataType forCPP (String type) {
      for (DataType dt : VALUES) {
        if (dt.cpp.equals(type)) return dt;
      }
      throw new IllegalArgumentException("Unexpected type '"+type+"'");
    }
  }

  private static enum Mode {
    /** Java source file.                     */ JAVA,
    /** JNI source file (Java side).          */ JNI_JAVA,
    /** JNI source file (C++ side).           */ JNI_CPP,
    /** C++ header file (arrays as pointers). */ CPP_HEADER,
    /** C++ source file (arrays as pointers). */ CPP_SOURCE;

    /** Is this for Java code? */
    public boolean isJava () { return (this == JAVA) || (this == JNI_JAVA); }

    /** Is this for JNI? */
    public boolean isJNI () { return (this == JNI_JAVA) || (this == JNI_CPP); }

    /** Is this for C++ code/header? */
    public boolean isCPP () { return !isJava(); }

    /** Is this for a source file (not header)? */
    public boolean isSource () {
      return (this == JAVA) || (this == CPP_SOURCE);
    }

    /** Is this for C++ header? */
    public boolean isHeader () {
      return (this == CPP_HEADER);
    }

    /** Include javadoc/doxygen comments? */
    public boolean isDocumented () { return (this == JAVA) || isHeader(); }

    /** Gets the appropriate code indentation. */
    public String getIndent () {
           if (isJava()  ) return "  ";
      else if (isSource()) return "";
      else                 return "    ";
    }
  };

  /** Gets the container type appropriate for the given type/format. If either
   *  is a long or uses a floating-point type the container type will be long,
   *  for all others it will be int.
   *  @param t        The data type in the user's array.
   *  @param f        The format type in the buffer.
   *  @param wordSize The minimum word size (-1=n/a).
   *  @return The container type.
   */
  private static DataType getContainerType (DataType t, DataType f, int wordSize) {
    if ((wordSize > 32)) return DataType.Long;
    if ((t == DataType.Double) || (f == DataType.Double)) return DataType.Long;
    if ((t == DataType.Long  ) || (f == DataType.Long  )) return DataType.Long;
    if ((t == DataType.Float ) || (f == DataType.Float )) return DataType.Long;
    return DataType.Int;
  }

  /** Gets the mask string. If a container type is specified, the output will be
   *  the intersection of the mask value for the data type and the container type.
   *  @param mode The code output mode.
   *  @param t    The data type (or format) in the user's array.
   *  @param c    The container type (null if n/a).
   *  @param bits The number of bits in the constant value.
   *  @return The mask value.
   */
  private static String getMaskStr (Mode mode, DataType t, DataType c, Integer bits) {
    long mask = (c == null)? t.getMask() : t.getMask() & c.getMask();

    if (bits == null) {
      bits = (c == null)? t.bits : c.bits;
    }

    if (bits <= 32) {
      return "0x"+toHexString(mask).toUpperCase();
    }
    else if (mode.isJava()) {
      return "0x"+toHexString(mask).toUpperCase()+"L";
    }
    else {
      return "__INT64_C(0x"+toHexString(mask).toUpperCase()+")";
    }
  }

  /** Applies the specified variables to a block of code.
   *  @param mode      The code output mode.
   *  @param codeLines The lines of code.
   *  @param t         The data type in the user's array.
   *  @param f         The format type in the buffer.
   */
  private static CharSequence apply (Mode mode, List<String> codeLines, DataType t, DataType f) {
    //return apply(mode, codeLines, t, f, Math.max(t.bits, f.bits));
    return apply(mode, codeLines, t, f, -1, null);
  }

  /** Applies the specified variables to a block of code.
   *  @param mode      The code output mode.
   *  @param codeLines The lines of code.
   *  @param t         The data type in the user's array.
   *  @param f         The format type in the buffer.
   *  @param wordSize  The word size to use.
   */
  private static CharSequence apply (Mode mode, List<String> codeLines,
                                     DataType t, DataType f, int wordSize) {
    return apply(mode, codeLines, t, f, wordSize, null);
  }

  /** Applies the specified variables to a block of code.
   *  @param mode      The code output mode.
   *  @param codeLines The lines of code.
   *  @param t         The data type in the user's array.
   *  @param f         The format type in the buffer.
   *  @param wordSize  The word size to use.
   *  @param jni       Generate JNI native (true/false) or non-JNI code (null).
   */
  private static CharSequence apply (Mode mode, List<String> codeLines,
                                     DataType t, DataType f, int wordSize,
                                     Boolean jni) {
    DataType     c         = getContainerType(t,f,wordSize);
    DataType     w         = (wordSize <= 32)? DataType.Int : DataType.Long;
    List<String> code      = new ArrayList<String>(codeLines.size());
    String       _NATIVE   = "false";
    String       _native   = "";
    String       _pureJava = "";

    if (jni != null) {
      _NATIVE   = (jni)? "true"    : "false";
      _native   = (jni)? "native_" : "";
      _pureJava = (jni)? ""        : "pureJava_";
    }

    for (String line : codeLines) {
      String ct     = "";             // value for "(#ct#)"
      String word32 = (mode.isJava())? DataType.Int.java  : DataType.Int.cpp;
      //String word64 = (mode.isJava())? DataType.Long.java : DataType.Long.cpp;

      // Setting #ct# is necessary since it makes sure that the low-order bits
      // are consistent when doing conversions. Technically this is a "garbage
      // in garbage out" scenario since the values exceed the limits, but we
      // want to maintain some level of consistency. The casting for 'double' is
      // required all the time, the casting for 'float' is only required for
      // 32-bit C++ (not sure why, but GCC 4.4 and Intel 11.1 give us 0 when
      // casting to an int8_t if we don't) but we do it all the time on C++
      // just to be sure.
      if (t == DataType.Double) ct = (mode.isJava())? "("+c.java+")" : "("+c.cpp+")";
      if (t == DataType.Float ) ct = (mode.isJava())? ""             : "("+c.cpp+")";

      line = line.replace("#type#",                 (mode.isJava())? t.java : t.cpp)
                 .replace("#form#",                 (mode.isJava())? f.java : f.cpp)
                 .replace("#container#",            (mode.isJava())? c.java : c.cpp)
                 .replace("#word#",                 (mode.isJava())? w.java : w.cpp)
                 .replace("#int24#",                word32)
                 .replace("(#ct#)",                 ct)
                 .replace("#UNSAFE#",               "false") //(mode.isJava())? "true" : "false")
                 .replace("#NATIVE#",               _NATIVE)
                 .replace("#native#",               _native)
                 .replace("#pureJava#",             _pureJava)
                 .replace("#Type#",                 t.toString())
                 .replace("#TypeClass#",            t.javaClass)
                 .replace("#JNI_CODE#",             t.jniCode)
                 .replace("#JNI_TYPE#",             t.jni)
                 .replace("#Form#",                 f.toString())
                 .replace("#Word#",                 w.toString())
                 .replace("#typecheck#",            Integer.toString(t.getTypeCheck()))
                 .replace("#formcheck#",            Integer.toString(f.getTypeCheck()))
                 .replace("#typebytes#",            Integer.toString(t.bytes))
                 .replace("#formbytes#",            Integer.toString(f.bytes))
                 .replace("#wordbytes#",            Integer.toString(w.bytes))
                 .replace("#formbits#",             Integer.toString(f.bits))
                 .replace("#continerbits#",         Integer.toString(c.bits))
                 .replace("#wordbits#",             Integer.toString(w.bits))
                 .replace("#typemask#",             getMaskStr(mode, t, null, 64))
                 .replace("#mask#",                 getMaskStr(mode, f, c, null))
                 .replace("#loop1N#",               "#loop1"+(8-f.bytes)+"#");

      if (mode.isJava()) {
        line = line.replaceAll("#loop([0-9])([0-9])#", "");
      }
      else {
        line = line.replaceAll("#loop([0-9])([0-9])#", "_Intel_Pragma(\"loop_count min=$1, max=$2\")");
      }

      if (!line.trim().isEmpty()) {
        code.add(line);
      }
    }

    StringBuilder src = new StringBuilder(65536);
    for (String line : preProcess(code, codeLines)) {
      src.append(line).append('\n');
    }
    return src;
  }

  /** Fix a javadoc line with any Java-specific replacements. */
  private static String fixDocJava (Mode mode, String line) {
    return line;
  }

  /** Fix a doxygen line with any C++-specific replacements. */
  private static String fixDocCPP (Mode mode, String line) {
    if ((mode == Mode.CPP_SOURCE) || (mode == Mode.CPP_HEADER)) {
      // Support passing as buffer via void* pointer
      if (line.contains("@param buf")) {
        line = line.replace("@param buf", "@param ptr") + " pointer";
      }
    }
    return line;
  }

  /** Fix a line with any Java-specific replacements. */
  private static String fixLineJava (Mode mode, boolean first, String origLine, boolean isInline) {
    String line = origLine;
    if (first) {
      if (!line.endsWith(" {")) {
        throw new RuntimeException("Error in '"+origLine+"' expected line to end with ' {'");
      }

      if (mode == Mode.JNI_JAVA) {
        line = line.replace("private static ",        "private static native ")
                   .replaceAll("[ ]+([^ ]+)[ ]+[(]", " native_$1 (");
        line = line.substring(0, line.length()-2) + ";";
      }
    }
    return line.replace("const ",                     "")
               .replace("unsigned ",                  "")
               .replace("inline ",                    "")
               .replace("case DataItemFormat_",       "case ")  // not needed in a Java switch statement
               .replace("DataItemFormat_",            "DataItemFormat.")
               .replace("#flatten# ",                 "")  // not used in Java
               .replace("#parallel#",                 "")  // not used in Java
               .replace("#inline_recursive#",         "")  // not used in Java
               .replace("#namespace#",                "")  // not used in Java
               .replace("#JAVA#",                     "true")
               .replace("#CPP#",                      "false")
               .replace("throw IllegalArgumentError", "throw new IllegalArgumentException")
               .replace("throw UnsupportedError",     "throw new UnsupportedOperationException")
               .replaceAll("getExponentBits[(]([^)]*)[)]", "$1.getExponentBits()")
               .replaceAll("_loop_count[(](.*)[)]",        "")
               .replaceAll("UNUSED_VARIABLE[(](.*)[)];",   "/* UNUSED_VARIABLE($1); */");
  }

  /** Fix a line with any **generic** C++-specific replacements. */
  private static String fixLineCPP (Mode mode, boolean first, String origLine, boolean isInline) {
    String line = origLine;
    // Replace "#namespace#" with namespace in source, but omit in header; here
    // we use the full "vrt::PackUnpack::" prefix to avoid issues with GCC 3.4.6
    // on RHEL 4 (32-bit).
    String namespace = (mode.isHeader())? "" : "vrt::PackUnpack::";

    if (first) {
      if (line.contains("private static ")) namespace = "";


      line = line.replace("public static ",  ""       )   // default for function in namespace
                 .replace("private static ", "static ")   // "private" function in a file
                 .replace("#namespace#",     namespace);

      // Support passing as buffer via void* pointer
      line = line.replace("byte[] buf", "void *ptr");
      // Support passing of other arrays via pointers
      int i = line.indexOf('(');
      line = line.substring(0,i).replaceAll("([a-z#]+)\\[\\] ", "$1* ")
           + line.substring( i ).replaceAll("([a-z#]+)\\[\\] ", "$1 *");

      // For header file make it end with a ";" rather than "{"
      if (!line.endsWith(" {")) {
        throw new RuntimeException("Error in '"+origLine+"' expected line to end with ' {'");
      }
      if (mode == Mode.CPP_HEADER) {
        line = line.substring(0, line.length()-2) + ";";
      }
    }
    else {
      // Convert "type[] name = new type[len];" to "vector<type> name(len);" or
      // "type *name = new type[len];"
      line = line.replaceAll("([a-z#]+)\\[\\][ ]+([a-zA-Z0-9_]+)[ ]*=[ ]*new[ ]*[a-z#]+\\[([^\\]]+)\\];",
                             "$1 *$2 = new $1[$3];");
    }

    // Fix standard replacements
    line = line.replaceAll("([ <(#])boolean([# >),])", "$1bool$2")
               .replace(">>>",                        ">>") // unsigned shift
               .replace("#JAVA#",                     "false")
               .replace("#CPP#",                      "true")
               .replace("#flatten#",                  "__attribute__((flatten))")
               .replace("#parallel#",                 "_Intel_Pragma(\"parallel\")")
               .replace("null",                       "NULL")
               .replace("final ",                     "const ")
               .replace("#inline_recursive#",         "")
               .replace("throw IllegalArgumentError", "throw VRTException")
               .replace("throw UnsupportedError",     "throw VRTException")
               .replace("getExponentBits(",           "DataItemFormat_getExponentBits(")
               .replaceAll("_loop_count[(](.*)[)]",   "_Intel_Pragma(\"$1\")");

    // Fix types (e.g. "long" to "int64_t")
    for (DataType t : DataType.VALUES) {
      line = line.replaceAll("([ <(#])unsigned "+t.java+"([# >),])", "$1u"+t.cpp+"$2")
                 .replaceAll("([ <(#])"+t.java+"([# >),])", "$1"+t.cpp+"$2");
    }

    // Additional changes
    if (first && (mode == Mode.CPP_SOURCE) && !isInline) {
      // Support passing as buffer via void* pointer
      if (line.contains("const void *ptr")) {
        line += "\n" + mode.getIndent() + "  const char *buf = (const char*)ptr;";
      }
      else if (line.contains("void *ptr")) {
        line += "\n" + mode.getIndent() + "  char *buf = (char*)ptr;";
      }
    }
    else if (first && (mode == Mode.JNI_CPP)) {
      line = genPackUnpackFunction(line);
    }
    return line;
  }

  /** Generates the pack/unpack function for JNI usage. */
  private static String genPackUnpackFunction (String funcDef) {
    StringBuilder code = new StringBuilder(4096);

    FunctionDef func      = new FunctionDef(funcDef);
    String      bufFlag   = (funcDef.contains("unpack"))? "JNI_ABORT" : "0";
    String      otherFlag = (funcDef.contains("unpack"))? "0" : "JNI_ABORT";

    code.append(func.getFuncDefJNI()).append(" {\n");
    code.append("  bool error = false;\n");
    for (int i = 0; i < func.paramArray.size(); i++) {
      if (!func.paramArray.get(i)) continue;
      DataType dt        = func.paramTypes.get(i);
      String   jniType   = (dt == null)? "#JNI_TYPE#" : dt.jni;
      String   paramName = func.paramNames.get(i);


      code.append("  ").append(jniType).append(" *_").append(paramName).append(" = NULL;\n");
    }
    code.append("  \n");
    code.append("  try {\n");
    for (int i = 0; i < func.paramArray.size(); i++) {
      if (!func.paramArray.get(i)) continue;
      DataType dt        = func.paramTypes.get(i);
      String   jniType   = (dt == null)? "#JNI_TYPE#" : dt.jni;
      String   paramName = func.paramNames.get(i);

      code.append("    if (!error && (").append(paramName).append(" != NULL)) {\n");
      code.append("      error = ((_").append(paramName).append(" = (").append(jniType)
                                    .append("*)env->GetPrimitiveArrayCritical(")
                                    .append(paramName).append(", NULL)) == NULL);\n");
      code.append("    }\n");
    }
    code.append("  \n");
    code.append("    if (!error) {\n");
    code.append("      ").append(func.getFuncCallJNI()).append(";\n");
    code.append("    }\n");
    code.append("  }\n");
    code.append("  catch (exception e) {\n");
    code.append("    THROW_JAVA_EXCEPTION(e.what());\n");
    code.append("  }\n");
    code.append("\n");
    for (int i = func.paramArray.size()-1; i >= 0; i--) {
      if (!func.paramArray.get(i)) continue;
      String paramName = func.paramNames.get(i);
      String writeBack = (paramName.equals("buf"))? bufFlag : otherFlag;


      code.append("  if (_").append(paramName).append(" != NULL) ")
                            .append("env->ReleasePrimitiveArrayCritical(")
                            .append(paramName).append(", _").append(paramName)
                            .append(", ").append(writeBack).append(");\n");
    }

    code.append("}");
    return code.toString();
  }

  /** Runs a limited pre-processor on the code. This is modeled after the C/C++
   *  preprocessor but only supports the following:
   *  <pre>
   *    conditional_start := "#IF " test optional_comment | "#ELSEIF " test optional_comment
   *    conditional_else  := "#ELSE"  optional_comment
   *    conditional_end   := "#ENDIF" optional_comment
   *
   *    test              := "(" test ")" | "true" | "false"
   *                                      | test_and | test_or | test_eq
   *                                      | test_le | test_lt | test_ge | test_gt
   *    test_and          := integer "&&" integer
   *    test_or           := integer "||" integer
   *    test_eq           := integer "==" integer
   *    test_le           := integer "&lt;=" integer
   *    test_lt           := integer "&lt;" integer
   *    test_ge           := integer "&gt;=" integer
   *    test_gt           := integer "&gt;" integer
   *
   *    integer           := "true" | "false" | REGEX([-+]?[1-9][0-9]+)
   *
   *    optional_comment  := "" | "//" REGEX(.*)
   *  </pre>
   *  Note for the purposes of integer-based tests, "true" and "false" are equal
   *  to "1" and "0" respectively.
   */
  private static List<String> preProcess (List<String> lines, List<String> origLines) {
    List<String>        code  = new ArrayList<String>(lines.size());
    ArrayDeque<Integer> state = new ArrayDeque<Integer>(4);

    // State Values:
    //   empty list  = Normal (not in any #IF ... #ENDIF)
    //   +1          = In Block   (at this level)
    //   -1          = Skip Block (at this level)
    //   -2          = Done Block (at this level)

    for (int i = 0; i < lines.size(); i++) {
      String line     = lines.get(i);
      String orig     = origLines.get(i);
      String trimLine = line.trim();

      if (trimLine.isEmpty()) {
        // IGNORE
      }
      else if (trimLine.startsWith("#IF")) {
        state.push(evaluateTest(line, orig));
      }
      else if (trimLine.startsWith("#ELSEIF")) {
        int last = state.pop();
        state.push((last == -1)? evaluateTest(line, orig) : -2);
      }
      else if (trimLine.equals("#ELSE")) {
        int last = state.pop();
        state.push((last == -1)? +1 : -1);
      }
      else if (trimLine.startsWith("#ENDIF")) {
        state.pop();
      }
      else {
        boolean ok = true;
        for (Integer s : state) {
          ok = ok && (s == +1);
        }

        if (ok) {
          code.add(line);
        }
      }
    }
    return code;
  }

  /** Evaluates a pre-processor style conditional. Only used by preProcess(..).
   *  @return +1 if evaluated successfully, -1 if it failed.
   */
  private static int evaluateTest (String line, String orig) {
    String test = line.replace("#ELSEIF ", "").replace("#IF ", "").trim();
    int i = test.indexOf("//");
    if (i >= 0) {
      test = test.substring(0,i).trim();
    }

    try {
      return (evaluateTest(test))? +1 : -1;
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to evaluate '"+orig+"'", e);
    }
  }

  /** Evaluates a pre-processor style conditional. Only used by preProcess(..). */
  private static boolean evaluateTest (String test) {
    test = test.trim();

    if (test.equals("true" )) return true;
    if (test.equals("false")) return false;

    if (test.startsWith("(")) {
      // Handle parenthesis.
      //   Example:
      //     (1 == 1) && true
      //
      //     evaluateTest(evaluateTest("1 == 1") + " " + " && true")
      //     evaluateTest("true" + " " + " && true")
      int level = 1;
      for (int i = 1; i < test.length(); i++) {
        char c = test.charAt(i);
        if (c == ')') {
          level--;
          if (level == 0) {
            return evaluateTest(evaluateTest(test.substring(1,i)) + " " + test.substring(i+1));
          }
        }
        if (c == '(') {
          level++;
        }
      }
      throw new RuntimeException("Could not evaluate conditional '"+test+"'");
    }

    String fixString = test.replace("&&", " && ").replace("||", " || ")
                           .replace("==", " == ").replace("!=", " != ")
                           .replace("<",  " <"  ).replace("<=",  "<= ")
                           .replace(">",  " >"  ).replace(">=",  ">= ")
                           .replaceAll("<([^=<])", "< $1")
                           .replaceAll(">([^=>])", "> $1");

    String[] tokens = fixString.split("[ ]+");

    if (tokens.length == 0) {
      // ERROR
    }
    else if (tokens.length == 1) {
      return toInt(tokens[0]) != 0;
    }
    else if (tokens.length == 2) {
      // ERROR
    }
    else if (tokens.length == 3) {
      if (tokens[1].equals("==")) return  toInt(tokens[0])     ==  toInt(tokens[2]);
      if (tokens[1].equals("!=")) return  toInt(tokens[0])     !=  toInt(tokens[2]);
      if (tokens[1].equals("<=")) return  toInt(tokens[0])     <=  toInt(tokens[2]);
      if (tokens[1].equals(">=")) return  toInt(tokens[0])     >=  toInt(tokens[2]);
      if (tokens[1].equals("<" )) return  toInt(tokens[0])     <   toInt(tokens[2]);
      if (tokens[1].equals(">" )) return  toInt(tokens[0])     >   toInt(tokens[2]);
      if (tokens[1].equals("&&")) return (toInt(tokens[0])!=0) && (toInt(tokens[2])!=0);
      if (tokens[1].equals("||")) return (toInt(tokens[0])!=0) || (toInt(tokens[2])!=0);
      // ERROR
    }
    else if (tokens[1].equals("&&") || tokens[1].equals("||")) {
      if (tokens[1].equals("&&") && !evaluateTest(tokens[0])) return false;
      if (tokens[1].equals("||") &&  evaluateTest(tokens[0])) return true;

      StringBuilder str = new StringBuilder(80);
      str.append(tokens[2]);
      for (int i = 3; i < tokens.length; i++) {
        str.append(' ').append(tokens[i]);
      }
      return evaluateTest(str.toString());
    }
    throw new RuntimeException("Could not evaluate conditional '"+test+"'");
  }

  /** Converts String to int. Only used by evaluateTest(..). */
  private static int toInt (String str) {
    if (str.equals("false")) return 0;
    if (str.equals("true" )) return 1;
    return Integer.parseInt(str);
  }

  /** Preprocesses the input code. This handles a number of mode-specific filters. */
  private static Map<String,List<String>> preprocessCode (Mode mode, List<String> lines) {
    LinkedHashMap<String,List<String>> map = new LinkedHashMap<String,List<String>>(64);

    String       name    = null;  // The section name
    List<String> code    = null;  // The lines of the code to emit
    int          lineNum = 0;     // Source code line number

    for (String origLine : lines) {
      String  trimLine     = origLine.trim();
      String  line         = mode.getIndent() + origLine;
      String  directive    = (trimLine.startsWith("##"))? trimLine.substring(2).trim() : null;
      lineNum++;

      if (trimLine.isEmpty()) {
        // skip blank lines
      }
      else if (directive != null) {
        if (directive.startsWith("START ")) {
          name = directive.substring(6).trim();
          code = new ArrayList<String>(64);
        }
        else if (directive.equals("END")) {
          if (name == null) throw new RuntimeException("Error on line "+lineNum+": END without START");
          if (code == null) throw new RuntimeException("Error on line "+lineNum+": END without START");

          if (name.equals("COMMENTS") || name.equals("COPYRIGHT")) {
            map.put(name, code);
          }
          else {
            map.put(name, filterCode(mode, code));
          }
          name = null;
          code = null;
        }
      }
      else if (code == null) {
        throw new RuntimeException("Error on line "+lineNum+": Expected START");
      }
      else {
        code.add(line);
      }
    }
    return map;
  }

  /** Filters the code, only used by preprocessCode(..). */
  private static List<String> filterCode (Mode mode, List<String> lines) {
    ArrayList<String> all = new ArrayList<String>(1024);
    int i = 0;

    while (i < lines.size()) {
      ArrayList<String> comments    = new ArrayList<String>(32);
      ArrayList<String> annotations = new ArrayList<String>(4);
      ArrayList<String> code        = new ArrayList<String>(128);

      // ==== FILTER THE COMMENTS ====================================================================
      if (!lines.get(i).trim().startsWith("/**")) {
        throw new RuntimeException("Expected comment, but found '"+lines.get(0)+"'");
      }
      for (; i < lines.size(); i++) {
        String origLine = lines.get(i);

        if (mode.isJava()) comments.add(fixDocJava(mode, origLine));
        else               comments.add(fixDocCPP( mode, origLine));

        if (origLine.contains("*/")) {
          i++;
          break;
        }
      }

      // ==== FILTER THE ANNOTATIONS =================================================================
      for (; i < lines.size(); i++) {
        String origLine = lines.get(i);
        String trimLine = origLine.trim();

        if (!trimLine.startsWith("@")) break;
        annotations.add(origLine);
      }

      // ==== FILTER THE CODE ========================================================================
      boolean isInline  = false; // Is this an inline function?
      boolean isPrivate = false; // Is this a private function?
      int     firstLine = 1;     // 0=MethodBody, 1=FirstLine (normal), 2=FirstLine (preprocessor)

      for (; i < lines.size(); i++) {
        String origLine = lines.get(i);
        String trimLine = origLine.trim();

        if (trimLine.startsWith("/**")) break;

        if (trimLine.startsWith("# ")) {
          trimLine = "#" + trimLine.substring(1).trim(); // strip extra speces off after frist '#'
        }

        boolean preprocessor = trimLine.startsWith("#if")
                            || trimLine.startsWith("#else")
                            || trimLine.startsWith("#endif")
                            || trimLine.startsWith("#pragma")
                            || trimLine.startsWith("#define");
        boolean isPreproc    = trimLine.startsWith("#IF")
                            || trimLine.startsWith("#ELSE")
                            || trimLine.startsWith("#ENDIF");

        if (firstLine > 0) {
          if (trimLine.startsWith("#IF"   )) firstLine = 2;
          if (trimLine.startsWith("#ENDIF")) firstLine = 0;
        }

        if (firstLine > 0) {
          isInline  = trimLine.contains("inline ");
          isPrivate = trimLine.contains("private ");
        }
        else if (mode.isHeader()) {
          continue;
        }

        if (preprocessor) {
          code.add(trimLine);
        }
        else if (isPreproc) {
          if (mode.isJava()) code.add(fixLineJava(mode, false, trimLine, false));
          else               code.add(fixLineCPP( mode, false, trimLine, false));
        }
        else {
          if (mode.isJava()) code.add(fixLineJava(mode, (firstLine > 0), origLine, isInline));
          else               code.add(fixLineCPP( mode, (firstLine > 0), origLine, isInline));
          if (firstLine == 1) firstLine = 0;
        }
      }

      // ==== SELECT WHAT IS NEEDED ==================================================================
      switch (mode) {
        case JAVA:         all.addAll(comments); all.addAll(annotations); all.addAll(code); break;
        case JNI_JAVA:                           all.addAll(annotations); all.addAll(code); break;
        case JNI_CPP:                                                     all.addAll(code); break;
        case CPP_HEADER:
          if (isPrivate) { /* ignore */                                                     break; }
          else           { all.addAll(comments);                          all.addAll(code); break; }
        case CPP_SOURCE:
          if (isPrivate) { all.addAll(comments);                          all.addAll(code); break; }
          if (!isInline) {                                                all.addAll(code); break; }
          else           { /* ignore */                                                     break; }
        default: throw new AssertionError("Unknown mode "+mode);
      }
    }
    return all;
  }

  /** Prints out the code for the various pack/unpack functions.
   *  @param out    The stream to print to.
   *  @param mode   The mode being used.
   *  @param code   The template code as read in.
   *  @param doJNI  Include JNI-compatible version of GENERIC Java functions?
   */
  private static void printFunctions (PrintStream out, Mode mode, Map<String,List<String>> code,
                                      boolean doJNI) throws Exception {
    for (DataType t : DataType.VALUES) {
      if (!t.isNativeType()) continue;
      out.print(apply(mode, code.get("FPOINT" ), t, DataType.Double));
      out.print(apply(mode, code.get("FPOINT" ), t, DataType.Float));
      out.print(apply(mode, code.get("INTEGER"), t, DataType.Long));
      out.print(apply(mode, code.get("INTEGER"), t, DataType.Int));
      out.print(apply(mode, code.get("INTEGER"), t, DataType.Int24));
      out.print(apply(mode, code.get("INTEGER"), t, DataType.Short));
      out.print(apply(mode, code.get("INT12S" ), t, DataType.Int12));
      out.print(apply(mode, code.get("INTEGER"), t, DataType.Byte));
      out.print(apply(mode, code.get("NIBBLES"), t, DataType.Nibble));
      out.print(apply(mode, code.get("BITS"   ), t, DataType.Bit));
      out.print(apply(mode, code.get("WORD"   ), t, DataType.Int,          32));
      out.print(apply(mode, code.get("WORD"   ), t, DataType.Int,          64));
      out.print(apply(mode, code.get("WORD"   ), t, DataType.Float,        64));
      out.print(apply(mode, code.get("WORD"   ), t, DataType.SignedVRT1,   32));
      out.print(apply(mode, code.get("WORD"   ), t, DataType.SignedVRT1,   64));
      out.print(apply(mode, code.get("BUFFER" ), t, DataType.Int,          32));
      out.print(apply(mode, code.get("BUFFER" ), t, DataType.Int,          64));
      out.print(apply(mode, code.get("BUFFER" ), t, DataType.Float,        64));
      out.print(apply(mode, code.get("BUFFER" ), t, DataType.SignedVRT1,   32));
      out.print(apply(mode, code.get("BUFFER" ), t, DataType.SignedVRT1,   64));

      if (mode.isCPP()) {
        out.print("#if NOT_USING_JNI\n");
        out.print(apply(mode, code.get("GENERIC"), t, DataType.Byte, -1, null));
        out.print("#endif /* NOT_USING_JNI */\n");
      }
      else if (doJNI) {
        out.print(apply(mode, code.get("GENERIC"), t, DataType.Byte, -1, true));
        out.print(apply(mode, code.get("GENERIC"), t, DataType.Byte, -1, false));
      }
      else {
        out.print(apply(mode, code.get("GENERIC"), t, DataType.Byte, -1, null));
      }
    }
  }

  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public static void main (String[] args) throws Exception {
    // Note that we are using:
    //    new PrintStream(new BufferedOutputStream(new FileOutputStream(...)));
    // rather than:
    //    new PrintStream(...);
    // as the later version can be extremely slow on some systems.

    InputStream    is = PackUnpackGen.class.getClassLoader().getResourceAsStream("nxm/vrt/dat/PackUnpackGen.code");
    BufferedReader in = new BufferedReader(new InputStreamReader(is), 65536);
    List<String>   inputLines = new ArrayList<String>(512);
    String line;
    while ((line = in.readLine()) != null) {
      inputLines.add(line);
    }
    in.close();

    boolean doJava = (args.length == 2) && args[0].equals("--java");
    boolean doJNI  = (args.length == 3) && args[0].equals("--jni");
    boolean doCPP  = (args.length == 3) && args[0].equals("--cpp");

    if (!(doJava || doJNI || doCPP)) {
      System.err.println("ERROR: expected --java, --jni, or --cpp");
    }

    if (doJava || doJNI) {
      Mode                     mode = Mode.JAVA;
      Map<String,List<String>> code = preprocessCode(mode,          inputLines);
      Map<String,List<String>> jni  = preprocessCode(Mode.JNI_JAVA, inputLines);
      PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(args[1]), 65536));

      out.print("//\n");
      out.print("//  THIS FILE IS AUTO-GENERATED, DO NOT MODIFY\n");
      out.print("//\n");
      out.print("\n");
      for (String c : code.get("COPYRIGHT")) {
        c = c.substring(mode.getIndent().length());
        out.print(c+"\n");
      }
      out.print("\n");
      out.print("package nxm.vrt.lib;\n");
      out.print("\n");
      if (false) {
        out.print("import java.lang.reflect.Field;\n");
        out.print("\n");
      }
      out.print("import javax.annotation.Generated;\n");
      out.print("import nxm.vrt.lib.VRTPacket.DataItemFormat;\n");
      out.print("import nxm.vrt.lib.VRTPacket.PayloadFormat;\n");
      out.print("\n");
      out.print("import static java.lang.Double.doubleToRawLongBits;\n");
      out.print("import static java.lang.Double.longBitsToDouble;\n");
      out.print("import static java.lang.Float.floatToRawIntBits;\n");
      out.print("import static java.lang.Float.intBitsToFloat;\n");
      out.print("import static nxm.vrt.lib.VRTMath.*;\n");
      out.print("\n");
      for (String c : code.get("COMMENTS")) {
        c = c.substring(mode.getIndent().length());
        out.print(c+"\n");
      }
      out.print("@Generated(\"PackUnpackGen\")\n");
      out.print("public final class PackUnpack {\n");
      out.print("  private PackUnpack () { } // prevents instantiation\n");
      out.print("\n");
      if (false) {
        out.print("  //\n");
        out.print("  //  CONSTANTS TO SUPPORT UNSAFE ACCESS\n");
        out.print("  //\n");
        out.print("  private static final sun.misc.Unsafe unsafe;\n");
        out.print("  private static final long            byteArrayOffset;\n");
        out.print("  private static final long            shortArrayOffset;\n");
        out.print("  private static final long            intArrayOffset;\n");
        out.print("  private static final long            longArrayOffset;\n");
        out.print("  \n");
        out.print("  static {\n");
        out.print("    Class<sun.misc.Unsafe> clazz   = sun.misc.Unsafe.class;\n");
        out.print("    Object                 _unsafe = null;\n");
        out.print("    try {\n");
        out.print("      Field field = clazz.getDeclaredField(\"theUnsafe\");\n");
        out.print("      \n");
        out.print("      field.setAccessible(true); \n");
        out.print("      _unsafe = field.get(null);\n");
        out.print("    }\n");
        out.print("    catch (Exception e) {\n");
        out.print("      // IGNORE\n");
        out.print("      e.printStackTrace();\n");
        out.print("    }\n");
        out.print("    \n");
        out.print("    if (_unsafe == null) {\n");
        out.print("      unsafe           = null;\n");
        out.print("      byteArrayOffset  = 0;\n");
        out.print("      shortArrayOffset = 0;\n");
        out.print("      intArrayOffset   = 0;\n");
        out.print("      longArrayOffset  = 0;\n");
        out.print("    }\n");
        out.print("    else {\n");
        out.print("      unsafe           = clazz.cast(_unsafe);\n");
        out.print("      byteArrayOffset  = unsafe.arrayBaseOffset( byte[].class);\n");
        out.print("      shortArrayOffset = unsafe.arrayBaseOffset(short[].class);\n");
        out.print("      intArrayOffset   = unsafe.arrayBaseOffset(  int[].class);\n");
        out.print("      longArrayOffset  = unsafe.arrayBaseOffset( long[].class);\n");
        out.print("    }\n");
        out.print("  }\n");
        out.print("  \n");
      }
      if (doJNI) {
        out.print("  //\n");
        out.print("  //  CONSTANTS TO SUPPORT JNI\n");
        out.print("  //\n");
        out.print("  private static final boolean noJNI;\n");
        out.print("  static {\n");
        out.print("    boolean _noJNI = true;\n");
        out.print("    try {\n");
        out.print("      System.loadLibrary(\"PackUnpack\");\n");
        out.print("      _noJNI = false;\n");
        out.print("    }\n");
        out.print("    catch (Throwable t) {\n");
        out.print("      System.err.println(\"Unable to load JNI library for fast buffer conversions, \"\n");
        out.print("                        +\"using default Java implementation (\"+t+\")\");\n");
        out.print("    }\n");
        out.print("    noJNI = _noJNI;\n");
out.print("    System.out.println(\"~~~~~~~~~~~~~~~ noJNI = \"+noJNI);\n");
        out.print("  }\n");
        out.print("\n");
        out.print("  //\n");
        out.print("  //  NATIVE JNI IMPLEMENTATION\n");
        out.print("  //\n");
        printFunctions(out, Mode.JNI_JAVA, jni, false);
        out.print("\n");
        out.print("  //\n");
        out.print("  //  NORMAL JAVA IMPLEMENTATION\n");
        out.print("  //\n");
      }
      printFunctions(out, mode, code, doJNI);
      out.print("}\n");
      out.close();
    }
    if (doJNI) {
      Mode                     mode = Mode.JNI_CPP;
      Map<String,List<String>> code = preprocessCode(mode, inputLines);
      PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(args[2]), 65536));

      out.print("//\n");
      out.print("//  THIS FILE IS AUTO-GENERATED, DO NOT MODIFY\n");
      out.print("//\n");
      out.print("\n");
      for (String c : code.get("COPYRIGHT")) {
        c = c.substring(mode.getIndent().length());
        out.print(c+"\n");
      }
      out.print("\n");
      out.print("#include \"nxm_vrt_lib_PackUnpack.h\"\n");
      out.print("#include \"PackUnpack.cc\" // Include C++ code file since functions are static!\n");
      out.print("\n");
      out.print("using namespace std;\n");
      out.print("using namespace vrt;\n");
      out.print("\n");
      out.print("/** Tells the Java VM to throw a RuntimeException with the given\n");
      out.print(" *  message once the function returns.\n");
      out.print(" */\n");
      out.print("#define THROW_JAVA_EXCEPTION(msg) {\\\n");
      out.print("  if (false) { /* set true when debugging JNI code */\\\n");
      out.print("    cerr << \"ERROR: Exception in JNI code: \" << msg << endl;\\\n");
      out.print("  }\\\n");
      out.print("  jclass _RuntimeException = env->FindClass(\"java/lang/RuntimeException\");\\\n");
      out.print("  if (_RuntimeException == NULL) return; /* Java VM Exception */\\\n");
      out.print("  jint status = env->ThrowNew(_RuntimeException, msg);\\\n");
      out.print("  if (status != 0) {\\\n");
      out.print("    env->FatalError(\"Unable to throw Java exception from JNI code\");\\\n");
      out.print("  }\\\n");
      out.print("}\n");
      out.print("\n");
      out.print("\n");
      printFunctions(out, mode, code, false);
      out.print("\n");
      out.close();
    }
    if (doCPP) {
      Mode                     mode = Mode.CPP_HEADER;
      Map<String,List<String>> code = preprocessCode(mode, inputLines);
      PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(args[1]), 65536));

      out.print("//\n");
      out.print("//  THIS FILE IS AUTO-GENERATED, DO NOT MODIFY\n");
      out.print("//\n");
      out.print("\n");
      for (String c : code.get("COPYRIGHT")) {
        c = c.substring(mode.getIndent().length());
        out.print(c+"\n");
      }
      out.print("\n");
      out.print("#ifndef _PackUnpack_h\n");
      out.print("#define _PackUnpack_h\n");
      out.print("\n");
      out.print("#include \"VRTMath.h\"\n");
      out.print("#include \"VRTObject.h\"\n");
      out.print("#include \"BasicVRTPacket.h\"\n");
      out.print("#include <vector>\n");
      out.print("\n");
      out.print("using namespace std;\n");
      out.print("using namespace vrt;\n");
      out.print("\n");
      out.print("namespace vrt {\n");
      for (String c : code.get("COMMENTS")) {
        c = c.substring(mode.getIndent().length());
        out.print("  "+c+"\n");
      }
      out.print("  namespace PackUnpack {\n");
      printFunctions(out, mode, code, false);
      out.print("  } END_NAMESPACE\n");
      out.print("} END_NAMESPACE\n");
      out.print("#endif /* _PackUnpack_h */\n");
      out.print("\n");
      out.close();
    }
    if (doCPP) {
      Mode                     mode = Mode.CPP_SOURCE;
      Map<String,List<String>> code = preprocessCode(mode, inputLines);
      PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(args[2]), 65536));

      out.print("//\n");
      out.print("//  THIS FILE IS AUTO-GENERATED, DO NOT MODIFY\n");
      out.print("//\n");
      out.print("\n");
      for (String c : code.get("COPYRIGHT")) {
        c = c.substring(mode.getIndent().length());
        out.print(c+"\n");
      }
      out.print("\n");
      out.print("#include \"PackUnpack.h\"\n");
      out.print("\n");
      out.print("using namespace std;\n");
      out.print("using namespace vrt;\n");
      out.print("using namespace vrt::VRTMath;\n");
      out.print("\n");
      printFunctions(out, mode, code, false);
      out.print("\n");
      out.close();
    }
  }


  /** A parsed function definition. */
  private static class FunctionDef {
    final String         type;
    final String         name;
    final List<Boolean>  paramArray = new ArrayList<Boolean>(16);
    final List<DataType> paramTypes = new ArrayList<DataType>(16);
    final List<String>   paramNames = new ArrayList<String>(16);

    /** Creates a new instance. */
    FunctionDef (String orig) {
      String _type   = null;
      String _name   = null;
      String funcDef = orig.replace('{',' ').trim();

      int i;
      while (((i = funcDef.indexOf(' ')) >= 0) && (_name == null)) {
        String token = funcDef.substring(0,i);

        if (token.equals("static")) {
          // ignore
        }
        else if (token.startsWith("__attribute__")) {
          // ignore
        }
        else if (_type == null) {
          _type = token;
        }
        else {
          _name = token;
        }
        funcDef = funcDef.substring(i).trim();
      }
      if ((_name == null) || !funcDef.startsWith("(") || !funcDef.endsWith(")")) {
        throw new IllegalArgumentException("Illegal function definition '"+orig+"'");
      }
      this.type = _type;
      this.name = _name;

      funcDef = funcDef.substring(1,funcDef.length()-1).trim();
      if (funcDef.isEmpty()) {
        // DONE
      }
      else {
        for (String arg : funcDef.split(",")) {
          arg = arg.trim();
          if (arg.startsWith("const ")) {
            arg = arg.substring(6).trim();
          }
          int     j = arg.indexOf(' ');
          boolean a = false;
          String  t = arg.substring(0,j).trim();
          String  n = arg.substring(j+1).trim();

          if (n.startsWith("*")) {
            a = true;
            n = n.substring(1).trim();
          }
          else if (t.endsWith("*")) {
            a = true;
            n = n.substring(0, n.length()-1).trim();
          }

          if (n.equals("ptr")) n = "buf";

          DataType dt;
               if (t.equals("void"  )) dt = DataType.Byte;  // void* in C++ matches byte[] in Java
          else if (t.equals("#type#")) dt = null;           // this gets filled in later
          else                         dt = DataType.forCPP(t);

          paramArray.add(a);
          paramTypes.add(dt);
          paramNames.add(n);
        }
      }
    }

    /** Creates a function call to this function. */
    public CharSequence getFuncCallJNI () {
      StringBuilder str = new StringBuilder(256);
      str.append(name).append('(');

      for (int i = 0; i < paramNames.size(); i++) {
        if (i > 0) str.append(", ");
        if (paramArray.get(i)) {
          str.append('_'); // use C++ version of array
        }
        str.append(paramNames.get(i));
      }
      str.append(')');
      return str;
    }

    /** Creates a JNI function definition for this function. */
    public CharSequence getFuncDefJNI () {
      StringBuilder str = new StringBuilder(256);
      str.append("JNIEXPORT ").append(type).append(" JNICALL ");
      str.append("Java_nxm_vrt_lib_PackUnpack_native_1").append(name.replace("_", "_1"));
      str.append("__");

      for (int i = 0; i < paramTypes.size(); i++) {
        if (paramArray.get(i)) {
          str.append("_3"); // JNI array indicator
        }
        DataType dt = paramTypes.get(i);
        if (dt == null) {
          str.append("#JNI_CODE#");
        }
        else {
          str.append(dt.jniCode);
        }
      }
      str.append("(JNIEnv *env, jclass clazz");

      for (int i = 0; i < paramNames.size(); i++) {
        str.append(", ");

        DataType dt = paramTypes.get(i);
        if (dt == null) {
          str.append("#JNI_TYPE#");
        }
        else {
          str.append(dt.jni);
        }
        if (paramArray.get(i)) {
          str.append("Array"); // JNI array indicator
        }

        str.append(' ');
        str.append(paramNames.get(i));
      }
      str.append(')');
      return str;
    }
  }
}
