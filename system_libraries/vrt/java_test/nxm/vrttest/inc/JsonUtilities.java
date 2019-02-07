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

package nxm.vrttest.inc;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;

import static java.lang.Character.isISOControl;
import static java.lang.Character.isWhitespace;

/** <b>Internal Use Only:</b> Utilities for converting to/from JSON. <br>
 *  <br>
 *  <h2>Type Conversions</h2>
 *  This supports the following conversions:
 *  <pre>
 *    | Java Type (toJSON)   | JSON Type (toJSON/fromJSON) | Java Type (fromJSON) | Notes |
 *    +----------------------+-----------------------------+----------------------+-------+
 *    | null                 | "null"                      | null                 |       |
 *    +----------------------+-----------------------------+----------------------+-------+
 *    | JsonObject           | object                      | JsonObject           |  [0]  |
 *    +----------------------+-----------------------------+----------------------+-------+
 *    | Boolean              | "true" or "false"           | Boolean              |       |
 *    +----------------------+-----------------------------+----------------------+-------+
 *    | Number               | number [1]                  | Number [1]           |  [1]  |
 *    +----------------------+-----------------------------+----------------------+-------+
 *    | Map[?,?]             | object                      | Map[String,Object]   |  [2]  |
 *    +----------------------+-----------------------------+----------------------+-------+
 *    | Iterable[?]          | array                       | List[Object]         |  [3]  |
 *    | array                |                             |                      |       |
 *    +----------------------+-----------------------------+----------------------+-------+
 *    | CharSequence         | string                      | String               |  [4]  |
 *    | Character            |                             |                      |       |
 *    | Object               |                             |                      |       |
 *    +----------------------+-----------------------------+----------------------+-------+
 *    [0] See {@link JsonObject} for more details.
 *    [1] See section on "Numeric Conversions" for more details.
 *    [2] When converting to JSON, the keys in a Map will be converted to strings.
 *    [3] Object or primitive type.
 *    [4] If none of the above match, conversion to string done using .toString().
 *  </pre>
 *  <h2>Numeric Conversions</h2>
 *  When converting from JSON to Java for a Number, the conversion will be to a {@link Double}
 *  if any of the following are true:
 *  <ul>
 *    <li>The value contains a decimal point ('.') and/or an exponent ('e' or 'E'),</li>
 *    <li>The value is "Infinity", "-Infinity", or "NaN", or</li>
 *    <li>The value is outside the range of {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE}
 *        (inclusive).</li>
 *  </ul>
 *  Otherwise the conversion will be to either an {@link Integer} or {@link Long} consistent with
 *  [RFC4627]. <i>[Note: Old versions of this class converted all numbers to {@link Double}, which
 *  is consistent with [JS] (and [RFC4627]) but has the unfortunate side-effect (in Java) of
 *  converting "0" to "0.0" and dropping some of the integer precision available to a 64-bit
 *  {@link Long}.]</i> <br>
 *  <br>
 *  Officially JSON does not support the non-numbers (NaN and infinities) from IEEE 754 as
 *  explicitly stated in [ECMA-404], however JavaScript and many other tools (e.g. Python) support
 *  them by default. This class does likewise when converting to JSON when {@link #ALLOW_IEEE754}
 *  is set to true (the default). When reading JSON values, the non-numbers are always supported
 *  (i.e. same as {@link #ALLOW_IEEE754} being set to true) since their meaning is clear and
 *  otherwise they would cause an exception (this is consistent with [RFC4627]).
 *  <pre>
 *    | IEEE 754 Special    | Java      | JSON (ALLOW_IEEE754=true) | JSON (ALLOW_IEEE754=false)   |
 *    +---------------------+-----------+---------------------------+------------------------------+
 *    | Positive Infinity   | +Infinity |  Infinity                 |  1.7976931348623157e+308 [1] |
 *    | Negative Infinity   | -Infinity | -Infinity                 | -1.7976931348623157e+308     |
 *    | Not A Number        | NaN       | NaN                       | null                         |
 *    +---------------------+-----------+---------------------------+------------------------------+
 *    | Negative Zero       | -0.0      | -0.0                      | -0.0                         |
 *    +---------------------+-----------+---------------------------+------------------------------+
 *    [1] This value matches {@link Double#MAX_VALUE}.
 *  </pre>
 *  As shown above, negative zero is maintained if specified as a floating-point value (i.e. "-0.0")
 *  when converting in either direction; however it is not maintained when converting from JSON to
 *  Java if specified in integer form (i.e. "-0") ([ECMA-404] is ambiguous at to the meaning of
 *  negative zero). <br>
 *  <br>
 *  Officially numbers in JSON omit leading zeros and all positive numbers omit the leading plus
 *  sign ('+') (see [ECMA-404]). This class follows that rule when converting to JSON, but allows
 *  leading zeros and a leading plus sign when reading JSON values, since their meaning is clear
 *  and otherwise they would cause an exception. <br>
 *  <br>
 *  <h2>JSON Formatting</h2>
 *  When generating JSON content, whitespace is optional (per [RFC4627]). This class provides
 *  options for how whitespace should be used to ballance between (human-)readability, conversion
 *  speed, and transmission size. The available options are listed {@link Mode here} and with the
 *  current option set in {@link #MODE}. <br>
 *  <br>
 *  <h2>References</h2>
 *  <pre>
 *    [ECMA-262] Ecma International. "ECMAScript Language Specification." Standard ECMA-262.
 *               Edition 5.1 (June 2011).
 *               <a href="http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-262.pdf"><!--
 *               -->http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-262.pdf</a>
 *
 *    [ECMA-404] Ecma International. "The JSON Data Interchange Format." Standard ECMA-404.
 *               Edition 1 (October 2013).
 *               <a href="http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-404.pdf"><!--
 *               -->http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-404.pdf</a>
 *
 *    [JS]       <i>Reference to [ECMA-262], but excluding Section 15.12 and Annex A through Annex F.</i>
 *
 *    [RFC4627]  Crockford, Douglas. "The application/json Media Type for JavaScript Object Notation
 *               (JSON)." Network Working Group. RFC 4627 (July 2006).
 *               <a href="http://www.ietf.org/rfc/rfc4627.txt">http://www.ietf.org/rfc/rfc4627.txt</a>
 *  </pre>
 */
public final class JsonUtilities {
  private JsonUtilities () { } // prevent instantiation

  //////////////////////////////////////////////////////////////////////////////
  // JSON Object Types
  //////////////////////////////////////////////////////////////////////////////
  private static final Map<Class<?>,JsonObjectInfo<?>> jsonObjects  = new WeakHashMap<>(64);

  private static final JsonEntry AUTO_ENTRY = new JsonEntry() {
    @Override public Class<? extends Annotation> annotationType () { return JsonEntry.class; }
    @Override public String                      name           () { return "";              }
    @Override public boolean                     required       () { return false;           }
    @Override public boolean                     ignoreSet      () { return false;           }
    @Override public Class<?>                    type           () { return Type.class;      }
    @Override public Class<?>                    valtype        () { return Object.class;    }
    @Override public Class<?>                    keytype        () { return String.class;    }
  };

  /** Gets a JsonObject.
   *  @param class The requested class type.
   */
  @SuppressWarnings("unchecked")
  private static synchronized <T> JsonObjectInfo<T> getJsonObject (Class<T> clazz) {
    if (!jsonObjects.containsKey(clazz)) {
      JsonObject jo = clazz.getAnnotation(JsonObject.class);
      if (jo == null) return null;

      // Init the info and add new object to map
      JsonObjectInfo<T> ji = new JsonObjectInfo<>(clazz, jo);
      jsonObjects.put(clazz, ji);
    }
    return (JsonObjectInfo<T>)jsonObjects.get(clazz);
  }

  //////////////////////////////////////////////////////////////////////////////
  // Formatting Options
  //////////////////////////////////////////////////////////////////////////////
  /** The modes for the output JSON content that control how whitespace is used
   *  allowing choices between readability and speed.
   */
  public static enum Mode {
    /** Auto-select a format mode.*/
    Auto,
    /** Auto-select a format mode based on conversion speed. */
    FastAuto,
    /** Format JSON in a very compact format (optimizes transmission, but makes it
     *  hard for humans to read).
     */
    Compact,
    /** Format JSON in a formal, easy-to-read, manner. */
    Formal;
  };

  /** The modes for the output JSON content. This defaults to Auto, but can be overridden by
   *  setting the "JsonUtilities.MODE" system property prior to loading this class.
   */
  public static final Mode MODE
    = Mode.valueOf(System.getProperty("JsonUtilities.MODE", "Auto"));

  /** Allow IEEE 754 values in JSON strings. This defaults to true, but can be overridden by
   *  setting the "JsonUtilities.ALLOW_IEEE754" system property prior to loading this class.
   */
  public static final boolean ALLOW_IEEE754
    = Boolean.valueOf(System.getProperty("JsonUtilities.ALLOW_IEEE754", "true"));

  //////////////////////////////////////////////////////////////////////////////
  // Media Types / File Extensions
  //////////////////////////////////////////////////////////////////////////////
  /** The official media type for JSON content from [RFC4627]. */
  public static final String MEDIA_TYPE = "application/json";

  /** The official file extension for JSON content from [RFC4627]. */
  public static final String FILE_EXT   = ".json";

  /** Does the given media type match JSON? This will return true for both the official media
   *  type from [RFC4627] ({@value #MEDIA_TYPE}) and the unofficial, but commonly-seen, media
   *  type "text/json".
   *  @param mediaType The media type.
   *  @return true if the media type matches JSON content, false otherwise.
   *  @throws NullPointerException if the media type is null.
   */
  public static boolean isMediaTypeJSON (String mediaType) {
    return mediaType.equals(MEDIA_TYPE)
        || mediaType.equals("text/json");
  }

  //////////////////////////////////////////////////////////////////////////////
  // CONVERT JAVA TO JSON
  //////////////////////////////////////////////////////////////////////////////
  /** Converts a Java object to a JSON object using the default mode ({@link #MODE}).<br>
   *  <br>
   *  <i>Implementation Note: This never returns null since the JSON representation that equates
   *  to a null input value is the string "null".</i>
   *  @param obj The object to convert (can be null).
   *  @return The JSON string. <b>This will never be null.</b>
   */
  public static CharSequence toJSON (Object obj) {
    return toJSON(obj, MODE);
  }

  /** Converts a Java object to a JSON object. <br>
   *  <br>
   *  <i>Implementation Note: This never returns null since the JSON representation that equates
   *  to a null input value is the string "null".</i>
   *  @param obj  The object to convert (can be null).
   *  @param mode The mode to use (can not be null).
   *  @return The JSON string. <b>This will never be null.</b>
   */
  public static CharSequence toJSON (Object obj, Mode mode) {
    StringBuilder str = new StringBuilder(128);
    return toJSON(str, "", obj, false, mode);
  }

  /** Converts a Java CharSequence to a JSON object.
   *  @param str The StringBuilder to append to.
   *  @param obj The object to convert (not null).
   *  @return The StringBuilder (to allow chaining).
   */
  private static StringBuilder toJSON (StringBuilder str, CharSequence obj) {
    str.append('"');
    for (int i = 0; i < obj.length(); i++) {
      char c = obj.charAt(i);
      switch (c) {
        case '\"': str.append("\\\""); break;
        case '\\': str.append("\\\\"); break;
        case '/':  str.append("/");    break; // don't escape (optional in [ECMA-404] not used in [JS])
        case '\b': str.append("\\b" ); break;
        case '\f': str.append("\\f" ); break;
        case '\n': str.append("\\n" ); break;
        case '\r': str.append("\\r" ); break;
        case '\t': str.append("\\t" ); break;
        default:
          if (!isISOControl(c)) {
            str.append(c);
          }
          else {
            str.append("\\u");
            str.append(hexChar(i >> 12));
            str.append(hexChar(i >>  8));
            str.append(hexChar(i >>  4));
            str.append(hexChar(i      ));
          }
      }
    }
    str.append('"');
    return str;
  }

  /** Converts a Java object to a JSON object with indentation as listed.
   *  @param str     The StringBuilder to append to.
   *  @param indent  The indent level (as a string).
   *  @param obj     The object to convert.
   *  @param fast    Should we use fast mode?
   *  @param inBlock Are we currently in an indented block?
   *  @param compact Should we use a compact form?
   *  @return str
   */
  @SuppressWarnings( {"fallthrough", "rawtypes", "unchecked"})
  private static StringBuilder toJSON (StringBuilder str, String indent, Object obj,
                                       boolean inBlock, Mode mode) {
    JsonObjectInfo jo = (obj == null)? null : getJsonObject(obj.getClass());
    if (jo != null) {
      obj = jo.toJSON(obj);
    }
    if (obj instanceof Number) {
      if (!ALLOW_IEEE754) {
        double d = ((Number)obj).doubleValue();
        if (Double.isNaN(d)     ) return str.append("null");
        if (Double.isInfinite(d)) return str.append(Double.MAX_VALUE * Math.signum(d));
      }
      return str.append(obj);
    }
    if (obj instanceof Boolean     ) return str.append(obj);
    if (obj instanceof Character   ) return toJSON(str, obj.toString());
    if (obj instanceof CharSequence) return toJSON(str, obj.toString());
    if (obj == null                ) return str.append("null");

    String space;
    String newline;
    String indent0; // indent+0
    String indent2; // indent+2
    String indent4; // indent+4

    switch (mode) {
      case Auto:
        StringBuilder s = new StringBuilder(80);
        s = toJSON(s, indent, obj, inBlock, Mode.Compact);
        if (s.length() < 80) return str.append(s);
        // FALLTHROUGH
      case FastAuto:
        space   = " ";
        newline = "\n";
        indent0 = indent;
        indent2 = indent + " ";
        indent4 = indent + "  ";
        break;
      case Formal:
        space   = " ";
        newline = "\n";
        indent0 = indent;
        indent2 = indent + "  ";
        indent4 = indent + "    ";
        break;
      case Compact:
        space   = "";
        newline = "";
        indent0 = "";
        indent2 = "";
        indent4 = "";
        break;
      default:
        throw new AssertionError("Unknown mode "+mode);
    }

    if (obj instanceof Map) {
      Map<?,?> map = (Map<?,?>)obj;
      if (map.isEmpty()) return str.append('{').append(space).append('}');

      if (inBlock) str.append(indent);
      str.append('{').append(newline);

      int count = 0;
      for (Map.Entry<?,?> e : map.entrySet()) {
        if (count > 0) str.append(',').append(newline);
        str.append(indent2);
        toJSON(str, e.getKey().toString());
        str.append(space).append(':').append(space);
        toJSON(str, indent4, e.getValue(), false, mode);
        count++;
      }
      return str.append(newline).append(indent0).append('}');
    }

    if (obj.getClass().isArray()) {
      int len = Array.getLength(obj);
      if (len == 0) return str.append('[').append(space).append(']');

      if (inBlock) str.append(indent);
      str.append('[').append(newline).append(indent2);

      for (int i = 0; i < len; i++) {
        if (i > 0) str.append(',').append(newline);
        toJSON(str, indent4, Array.get(obj,i), true, mode);
      }
      return str.append(newline).append(indent).append(']');
    }

    if (obj instanceof Iterable) {
      Iterable<?> it  = (Iterable<?>)obj;

      if (inBlock) str.append(indent);
      str.append('[').append(newline);

      int count = 0;
      for (Object o : it) {
        if (count > 0) str.append(',').append(newline);
        toJSON(str, indent4, o, true, mode);
        count++;
      }
      return str.append(newline).append(indent0).append(']');
    }

    // default handling same as CharSequence/String
    return toJSON(str, obj.toString());
  }

  //////////////////////////////////////////////////////////////////////////////
  // CONVERT JSON TO JAVA
  //////////////////////////////////////////////////////////////////////////////

  /** Indicates if c is a JSON operator. */
  private static boolean isOperator (char c) {
    return (c == ':') || (c == '[') || (c == '{')
        || (c == ',') || (c == ']') || (c == '}');
  }

  /** Indicates if s is a JSON operator. */
  private static boolean isOperator (String s) {
    return (s.length() == 1) && isOperator(s.charAt(0));
  }

  /** Tokenizes a JSON input string. */
  private static LinkedList<String> tokenize (String json) {
    LinkedList<String> tokens = new LinkedList<>();

    int length = json.length();
    int start  = -1;
    for (int i = 0; i < length; i++) {
      char c = json.charAt(i);

      if (isWhitespace(c)) {
        if (start >= 0) {
          // End of token
          tokens.add(json.substring(start,i));
        }
        start = -1;
      }
      else if (isOperator(c)) {
        if (start >= 0) {
          // End of token
          tokens.add(json.substring(start,i));
        }
        // Plus operator token
        tokens.add(json.substring(i,i+1));
        start = -1;
      }
      else if (start >= 0) {
        // Still in current token
      }
      else if (c == '\"') {
        // New string token
        start = i;
        while ((start >= 0) && (i < length)) {
          i++;
          c = json.charAt(i);

          if (c == '\"') {
            // End of string
            tokens.add(json.substring(start,i+1));
            start = -1;
          }
          else if (c == '\\') {
            // Escape sequence
            if (i == length-1) {
              throw new IllegalArgumentException("Invalid JSON object, expected escape after '\\'");
            }
            i++;
          }
        }
        if (start >= 0) throw new IllegalArgumentException("Invalid JSON object, expected '\"'");
      }
      else {
        // New non-string token
        start = i;
      }
    }
    if (start >= 0) {
      tokens.add(json.substring(start,length));
    }
    return tokens;
  }

  /** Converts JSON string to Java string. */
  @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch"})
  private static String _fromStr (String token) {
    if (!token.startsWith("\"") || !token.endsWith("\"")) {
      throw new IllegalArgumentException("Invalid JSON string, missing quote near '"+token+"'.");
    }
    if (token.indexOf('\\') < 0) {
      return token.substring(1, token.length()-1); // no control characters (this is the 90% case)
    }

    StringBuilder str = new StringBuilder(token.length());
    try {
      for (int i = 1; i < token.length()-1; i++) {
        char c = token.charAt(i);
        if (c != '\\') {
          str.append(c); // normal character
          continue;
        }

        char c1 = token.charAt(++i);
        switch (c1) {
          case '"':  str.append('\"'    ); break;
          case '\\': str.append('\\'    ); break;
          case '/':  str.append('/'     ); break;
          case 'b':  str.append('\b'    ); break;
          case 'f':  str.append('\f'    ); break;
          case 'n':  str.append('\n'    ); break;
          case 'r':  str.append('\r'    ); break;
          case 't':  str.append('\t'    ); break;
          case 'u':
            int u = hexBits(token.charAt(++i) << 12)
                  | hexBits(token.charAt(++i) <<  8)
                  | hexBits(token.charAt(++i) <<  4)
                  | hexBits(token.charAt(++i)      );
            str.append((char)u);
            break;
          case '\'': str.append('\''    ); break; // Not in [ECMA-404] but in [JS]
          case 'v':  str.append('\u000B'); break; // Not in [ECMA-404] but in [JS]
          case '0':  str.append('\0'    ); break; // Not in [ECMA-404] but in [JS]
          case 'x':                               // Not in [ECMA-404] but in [JS]
            int x = hexBits(token.charAt(++i) << 4)
                  | hexBits(token.charAt(++i)     );
            str.append((char)x);
            break;
          default:
            throw new IllegalArgumentException("Invalid JSON object, invalid escape sequence in '"+token+"'");
        }
      }
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON object, invalid escape sequence in '"+token+"'");
    }
    return str.toString();
  }

  /** Converts a single-entry JSON object to a Java object. */
  private static Object _fromJSON (String token) {
    if (token.equals("null"  )) return null;
    if (token.equals("true"  )) return Boolean.TRUE;
    if (token.equals("false" )) return Boolean.FALSE;
    if (token.startsWith("\"")) return _fromStr(token);

    int len = token.length();
    if (!isOperator(token) && (len >= 1)) {
      try {
        return parseNumber(token);
      }
      catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid JSON number, found '"+token+"'.");
      }
    }

    throw new IllegalArgumentException("Invalid JSON object, expected null, Boolean, "
                                     + "String, or Number, but found '"+token+"'.");
  }

  /** Converts a JSON object to a Java object.
   *  @param json The JSON object to convert.
   *  @return The Java object.
   */
  public static Object fromJSON (CharSequence json) {
    String str = json.toString().trim();
    return fromJSON(null, tokenize(str));
  }

  /** Converts a JSON object to a Java object.
   *  @param <T>  The expected type.
   *  @param type The expected type. This should be a basic type that maps directly to/from JSON
   *              (e.g. {@link Integer} or {@link Map}) or an object type that has {@link JsonObject}
   *              and {@link JsonEntry} annotations. See {@link #toType(Class,Object)} for details on
   *              the conversions used.
   *  @param json The JSON object to convert.
   *  @return The Java object.
   */
  @SuppressWarnings("unchecked")
  public static <T> T fromJSON (Class<T> type, CharSequence json) {
    return toType(type, fromJSON(json));
  }

  /** Converts a JSON object to a Java object.
   *  @param first  The token (can be null).
   *  @param tokens The remaining tokens.
   *  @return The Java object.
   */
  private static Object fromJSON (String first, LinkedList<String> tokens) {
    // Get first entry (if not already present)
    if (first == null) first = tokens.poll();

    if (first == null) {
      throw new IllegalArgumentException("Premature end of JSON object/array");
    }
    else if (first.equals("{")) {
      LinkedHashMap<String,Object> map = new LinkedHashMap<>(32);

      while (true) {
        first = tokens.poll();

        // Preliminary checks
             if (first == null    ) throw new IllegalArgumentException("Invalid JSON object, expected '}'");
        else if (first.equals("}")) break; // done
        else if (map.isEmpty()    ) { } // no comma expected
        else if (first.equals(",")) first = tokens.poll(); // found comma, now check next entry
        else                        throw new IllegalArgumentException("Invalid JSON object, expected '}' or ','");

        // Secondary checks
        String sep = tokens.poll();
        String val = tokens.poll();
        if (first == null    ) throw new IllegalArgumentException("Invalid JSON object, expected key");
        if (isOperator(first)) throw new IllegalArgumentException("Invalid JSON object, expected key before '"+first+"'");
        if (sep   == null    ) throw new IllegalArgumentException("Invalid JSON object, expected ':' after "+first);
        if (!sep.equals(":") ) throw new IllegalArgumentException("Invalid JSON object, expected ':' after "+first);
        if (val   == null    ) throw new IllegalArgumentException("Invalid JSON object, expected value for "+first);

        map.put(_fromStr(first), fromJSON(val,tokens)); // key must be a string
      }
      return map;
    }
    else if (first.equals("[")) {
      ArrayList<Object> list = new ArrayList<>(32);

      while (true) {
        first = tokens.poll();

        // Preliminary checks
             if (first == null    ) throw new IllegalArgumentException("Invalid JSON array, expected ']'");
        else if (first.equals("]")) break; // done
        else if (list.isEmpty()   ) { } // no comma expected
        else if (first.equals(",")) first = tokens.poll(); // found comma, now check next entry
        else                        throw new IllegalArgumentException("Invalid JSON array, expected '}' or ','");

        // Secondary checks
        if (first == null) throw new IllegalArgumentException("Invalid JSON array, expected value");

        list.add(fromJSON(first, tokens));
      }
      return list;
    }
    else {
      // single entry
      return _fromJSON(first);
    }
  }

  /** <b>Internal Use Only:</b> Parses a number.
   *  @param num The number in string form.
   *  @return The parsed number (or null if the input is null).
   *  @throws NumberFormatException If parsing fails.
   */
  public static Number parseNumber (String num) throws NumberFormatException {
    try {
      // Note that all NaN and infinite values will fail the integer conversions below and get
      // handled in the exception handling block. Since they should be exceedingly rare (and are
      // non-standard JSON anyhow) the slowness caused by this shouldn't be an issue.
      if (num               == null) return null;  int len = num.length();
      if (len               ==    1) return Integer.valueOf(num); // single-digit number (common)
      if (num.indexOf('.')  >=    0) return Double.valueOf(num);  // decimal place
      if (num.indexOf('e')  >     0) return Double.valueOf(num);  // exponent
      if (num.indexOf('E')  >     0) return Double.valueOf(num);  // exponent
      if (len               <=    9) return Integer.valueOf(num); // well inside int range
                                     return Long.valueOf(num);    // may still be outside of long range
    }
    catch (NumberFormatException e) {
      return Double.valueOf(num); // try parsing as double to handle out-of-range numbers
    }
  }

  /** Hex bits to char. */
  private static char hexChar (int i) {
    i = i & 0xF;
    int off = ((i >> 3) & ((i >> 2) | (i >> 1)) & 0x1) * 7; // offset for 'A' through 'F'
    return (char)('0' + i + off);
  }

  /** Hex char to bits. */
  private static int hexBits (int c) {
    switch (c) {
      case '0': return 0x0;  case '1': return 0x1;
      case '2': return 0x2;  case '3': return 0x3;
      case '4': return 0x4;  case '5': return 0x5;
      case '6': return 0x6;  case '7': return 0x7;
      case '8': return 0x8;  case '9': return 0x9;
      case 'A': return 0xA;  case 'a': return 0xA;
      case 'B': return 0xB;  case 'b': return 0xB;
      case 'C': return 0xC;  case 'c': return 0xC;
      case 'D': return 0xD;  case 'd': return 0xD;
      case 'E': return 0xE;  case 'e': return 0xE;
      case 'F': return 0xF;  case 'f': return 0xF;
      default: throw new IllegalArgumentException("Invalid hex char");
    }
  }

  /** <b>Internal Use Only:</b> Tries converting a value that was parsed from JSON to a specific
   *  type. The conversions attempted are as follows:
   *  <pre>
   *    +--------------+-------------------------+-------------------------------------------------+
   *    | CURRENT TYPE |      REQUIRED TYPE      |                 CONVERSION                      |
   *    +--------------+-------------------------+-------------------------------------------------+
   *    | null         | T                       | return null;                                    |
   *    +--------------+-------------------------+-------------------------------------------------+
   *    | T            | T   (non-parameterized) | return val;                                     |
   *    +--------------+-------------------------+-------------------------------------------------+
   *    | T            | T is JsonObject         | obj = new T();    // or T.factory(..)           |
   *    |              |                         | ...               // set entries in obj         |
   *    |              |                         | obj.validate();   // as required                |
   *    |              |                         | return obj;                                     |
   *    +--------------+-------------------------+-------------------------------------------------+
   *    | Map          | T extends Map[K,V]      | map = new T();                                  |
   *    |              |                         | for (Map.Entry e : val) {                       |
   *    |              |                         |   map.put(toType(K, e.getKey()),                |
   *    |              |                         |           toType(V, e.getValue()));             |
   *    |              |                         | }                                               |
   *    |              |                         | return map;                                     |
   *    +--------------+-------------------------+-------------------------------------------------+
   *    | Collection   | T extends Collection[V] | col = new T();                                  |
   *    |              |                         | for (Object obj : val) {                        |
   *    |              |                         |   col.add(toType(V, obj));                      |
   *    |              |                         | }                                               |
   *    |              |                         | return map;                                     |
   *    +--------------+-------------------------+-------------------------------------------------+
   *    | Collection   | V[] (array type)        | arr = new V[n];                                 |
   *    |              |                         | for (Object o : val) {                          |
   *    |              |                         |   arr[i++] = toType(V, o);                      |
   *    |              |                         | }                                               |
   *    |              |                         | return map;                                     |
   *    +--------------+-------------------------+-------------------------------------------------+
   *    | CharSequence | Character               | return val.charAt(0);    // if 1-char in length |
   *    |   or         +-------------------------+-------------------------------------------------+
   *    | String       | T extends Enum          | return T.valueOf(val);                          |
   *    |   or         +-------------------------+-------------------------------------------------+
   *    | Character    | String                  | return val.toString();                          |
   *    |              +-------------------------+-------------------------------------------------+
   *    |              | T has fromString(String)| return T.fromString(val.toString());            |
   *    |              +-------------------------+-------------------------------------------------+
   *    |              | T has valueOf(String)   | return T.valueOf(val.toString());               |
   *    |              +-------------------------+-------------------------------------------------+
   *    |              | T has valueOf(CharSeq)  | return T.valueOf(val);                          |
   *    |              +-------------------------+-------------------------------------------------+
   *    |              | T has parse(String)     | return T.parse(val);                            |
   *    |              +-------------------------+-------------------------------------------------+
   *    |              | T has parse(CharSeq)    | return T.parse(val);                            |
   *    |              +-------------------------+-------------------------------------------------+
   *    |              | T has new T(String)     | return new T(val.toString());                   |
   *    |              +-------------------------+-------------------------------------------------+
   *    |              | T has new T(CharSeq)    | return new T(val);                              |
   *    +--------------+-------------------------+-------------------------------------------------+
   *    | Number       | Byte     (or byte  )    | return val.byteValue();                         |
   *    |              +-------------------------+-------------------------------------------------+
   *    |              | Short    (or short )    | return val.shortValue();                        |
   *    |              +-------------------------+-------------------------------------------------+
   *    |              | Integer  (or int   )    | return val.intValue();                          |
   *    |              +-------------------------+-------------------------------------------------+
   *    |              | Long     (or long  )    | return val.longValue();                         |
   *    |              +-------------------------+-------------------------------------------------+
   *    |              | Float    (or float )    | return val.floatValue();                        |
   *    |              +-------------------------+-------------------------------------------------+
   *    |              | Double   (or double)    | return val.doubleValue();                       |
   *    |              +-------------------------+-------------------------------------------------+
   *    |              | T extends Enum          | return T.values()[val];                         |
   *    +--------------+-------------------------+-------------------------------------------------+
   *  </pre>
   *  The large number of {@link CharSequence} handling methods is intended to make many of the
   *  automatic <tt>.toString()</tt> conversions in {@link #toJSON(Object)} reflexive when used in
   *  a {@link JsonObject}. For example:
   *  <pre>
   *    java.math.BigDecimal in toJSON(..):    String     str  = dec.toString();
   *    java.math.BigDecimal in fromJSON(..):  BigDecimal dec  = new BigDecimal(str);
   *
   *    java.net.URL         in toJSON(..):    String     str  = url.toString();
   *    java.net.URL         in fromJSON(..):  URL        url  = new URL(str);
   *
   *    java.time.Instant    in toJSON(..):    String     str  = time.toString();
   *    java.time.Instant    in fromJSON(..):  Instant    time = Instant.parse(str);
   *
   *    java.util.UUID       in toJSON(..):    String     str  = uuid.toString();
   *    java.util.UUID       in fromJSON(..):  UUID       uuid = UUID.fromString(str);
   *  </pre>
   *  @param type The required type.
   *  @param val  The current value.
   *  @return The value converted to the required type.
   *  @throws NullPointerException if type is null
   *  @throws ClassCastException   if unable to convert to the required type.
   */
  @SuppressWarnings("unchecked")
  public static Object toType (Type type, Object val) {
    if (type instanceof Class) {
      // Use the 'Class' version to handle primitive types, etc.
      return toType((Class)type, val);
    }
    else {
      return _toType(type, val);
    }
  }

  /** <b>Internal Use Only:</b> Tries converting a value that was parsed from JSON to a specific
   *  type. The conversions attempted are as shown with {@link #toType(Type,Object)}.
   *  @param <T>  The required type.
   *  @param type The required type.
   *  @param val  The current value.
   *  @return The value converted to the required type.
   *  @throws NullPointerException if type is null
   *  @throws ClassCastException   if unable to convert to the required type.
   */
  @SuppressWarnings("unchecked")
  public static <T> T toType (Class<T> type, Object val) {
    if (type == null) {
      throw new NullPointerException("Unable to convert class Object to class null");
    }
    if (type.isPrimitive()) {
      if (type == Boolean.TYPE  ) return (T)_toType(Boolean.class,   val);
      if (type == Character.TYPE) return (T)_toType(Character.class, val);
      if (type == Byte.TYPE     ) return (T)_toType(Byte.class,      val);
      if (type == Short.TYPE    ) return (T)_toType(Short.class,     val);
      if (type == Integer.TYPE  ) return (T)_toType(Integer.class,   val);
      if (type == Long.TYPE     ) return (T)_toType(Long.class,      val);
      if (type == Float.TYPE    ) return (T)_toType(Float.class,     val);
      if (type == Double.TYPE   ) return (T)_toType(Double.class,    val);
    }
    return type.cast(_toType(type, val));
  }

  /** <b>Internal Use Only:</b> Tries converting a value that was parsed from JSON to a specific
   *  type. The conversions attempted are as shown with {@link #toType(Type,Object)}.
   *  @param origType The required type (not a primitive type).
   *  @param val  The current value.
   *  @return The value converted to the required type.
   *  @throws NullPointerException if type is null
   *  @throws ClassCastException   if unable to convert to the required type.
   */
  @SuppressWarnings( {"unchecked", "rawtypes", "BroadCatchBlock", "TooBroadCatch", "UseSpecificCatch"})
  private static Object _toType (final Type origType, final Object origVal) {
    if (origType == null) throw new NullPointerException("Unable to convert class Object to class null");
    if (origVal  == null) return null;

    try {
      Object val  = origVal; // current object value
      Type   type = refineType(origType);

      // ==== INTERNAL TYPE MODS ===================================================================
      if (val instanceof Character) {
        // fromJSON(..) never actually produces a Character it produces a String, so everything
        // below assumes String. This is just here in the off-chance a Character is passed in.
        val = val.toString();
      }
      if (val instanceof CharSequence) {
        // romJSON(..) never actually produces a CharSequence (in its current version) it produces
        // a String, so everything below assumes String. This is just here in the off-chance a
        // CharSequence is passed in.
        val = val.toString();
      }

      // ==== INITIAL CHECKS =======================================================================
      Class             clazz = null;
      ParameterizedType pt    = null;

      if (type instanceof Class) {
        clazz = (Class)type;
        if (clazz.isInstance(val)) return clazz.cast(val);

        JsonObjectInfo<?> jo = getJsonObject(clazz);
        if (jo != null) {
          return jo.fromJSON((Map<String,Object>)val);
        }
      }
      else if (type instanceof ParameterizedType) {
        pt    = (ParameterizedType)type;
        clazz = (Class)pt.getRawType();
      }
      else if (type instanceof GenericArrayType) {
        clazz = typeToClass(type);
      }
      else {
        // Other Type instances are TBD
        throw new TypeCastException(origType, origVal, "unsupported Type "+type.getClass());
      }

      // ==== MAP CONVERSION =======================================================================
      if (Map.class.isAssignableFrom(clazz)) {
        Map map = (Map)newInstance(clazz);
        if (pt == null) {  // basic Map<?,?>
          map.putAll((Map)val);
        }
        else {             // parameterized Map<K,V>
          Type K = pt.getActualTypeArguments()[0];
          Type V = pt.getActualTypeArguments()[1];
          for (Map.Entry<?,?> e : ((Map<?,?>)val).entrySet()) {
            map.put(toType(K, e.getKey()),
                    toType(V, e.getValue()));
          }
        }
        return map;
      }

      // ==== COLLECTION CONVERSION ================================================================
      if (Collection.class.isAssignableFrom(clazz) || Iterable.class.isAssignableFrom(clazz)) {
        Collection col = (Collection)newInstance(clazz);
        if (pt == null) {  // basic Collection<?>
          col.addAll((Collection)val);
        }
        else {             // parameterized Collection<V>
          Type V = pt.getActualTypeArguments()[0];
          for (Object o : (Iterable<?>)val) {
            col.add(toType(V, o));
          }
        }
        return col;
      }

      // ==== ARRAY CONVERSION =====================================================================
      if (clazz.isArray() || (type instanceof GenericArrayType)) {
        Collection col = (Collection)val;
        int        len = col.size();
        Object     arr = Array.newInstance(clazz.getComponentType(), len);
        int        i   = 0;
        Type       V   = clazz.getComponentType();

        if (type instanceof GenericArrayType) {
          V = ((GenericArrayType)type).getGenericComponentType();
        }
        for (Object v : col) {
          Array.set(arr, i++, toType(V, v));
        }
        return arr;
      }

      // ==== NO CONVERSION REQUIRED ===============================================================
      // This must come after all of the checks for generic collections and arrays since the
      // isInstance(..) check can't differentiate a List<String> from a List<Object>
      if (clazz.isInstance(val)) {
        return val;
      }

      // ==== CHAR SEQUENCE CONVERSION =============================================================
      if (clazz.isAssignableFrom(String.class)) {
        return val.toString(); // Handles String and CharSequence
      }
      if (val instanceof String) {
        String str = val.toString(); // .toString() is faster than cast to String
        if ((clazz == Character.class) && (str.length() == 1)) {
          return str.charAt(0);
        }
        if (!clazz.isInterface()) {
          Object obj;
          obj = tryStaticMethod(clazz, "fromString", String.class,       str);  if (obj != null) return obj;
          obj = tryStaticMethod(clazz, "valueOf",    String.class,       str);  if (obj != null) return obj;
          obj = tryStaticMethod(clazz, "valueOf",    CharSequence.class, str);  if (obj != null) return obj;
          obj = tryStaticMethod(clazz, "parse",      String.class,       str);  if (obj != null) return obj;
          obj = tryStaticMethod(clazz, "parse",      CharSequence.class, str);  if (obj != null) return obj;
          obj = tryStaticMethod(clazz, "<init>",     String.class,       str);  if (obj != null) return obj;
          obj = tryStaticMethod(clazz, "<init>",     CharSequence.class, str);  if (obj != null) return obj;
        }
        if (clazz == Number.class) {
          return parseNumber(val.toString());
        }
      }

      // ==== NUMBER CONVERSION ====================================================================
      if (val instanceof Number) {
        Number num = (Number)val;
        if (type == Number.class ) return num;
        if (type == Byte.class   ) return num.byteValue();
        if (type == Short.class  ) return num.shortValue();
        if (type == Integer.class) return num.intValue();
        if (type == Long.class   ) return num.longValue();
        if (type == Float.class  ) return num.floatValue();
        if (type == Double.class ) return num.doubleValue();
      }
    }
    catch (TypeCastException e) {
      throw e;
    }
    catch (InvocationTargetException e) {
      throw new TypeCastException(origType, origVal, e.getCause());
    }
    catch (Exception e) {
      throw new TypeCastException(origType, origVal, e);
    }
    throw new TypeCastException(origType, origVal);
  }

  /** Converts a {@link WildcardType} or {@link TypeVariable} to the underlying {@link Class} type.
   *  Note that this intentionally omits {@link ParameterizedType} and {@link GenericArrayType},
   *  just returning them as-is.
   *  @param  The input type.
   *  @return The "refined" type
   */
  private static Type refineType (Type type) {
    while ((type instanceof WildcardType) || (type instanceof TypeVariable)) {
      Type[] bounds = (type instanceof WildcardType)? ((WildcardType)type).getUpperBounds()
                                                    : ((TypeVariable)type).getBounds();
      if ((bounds == null) || (bounds.length == 0)) {
        type = Object.class; // no upper bounds (i.e. <T> => use Object)
      }
      else {
        type = bounds[0];    // has upper bounds (i.e. <T extends U> => use U)
      }
    }
    return type;
  }

  /** Converts a {@link Type} to the underlying {@link Class} type. Note that this intentionally
   *  omits {@link GenericArrayType}, just returning it as-is.
   *  @param  The input type.
   *  @return The "refined" type
   */
  private static Class<?> typeToClass (Type type) throws ClassNotFoundException {
    type = refineType(type);
    if (type instanceof Class) {
      return (Class)type;
    }
    else if (type instanceof ParameterizedType) {
      return typeToClass(((ParameterizedType)type).getRawType());
    }
    else if (type instanceof GenericArrayType) {
      Class<?> c = typeToClass(((GenericArrayType)type).getGenericComponentType());
      return Array.newInstance(c,0).getClass();
    }
    else {
      // Other Type instances are TBD
      throw new UnsupportedOperationException("Could not determine Class matching "+type);
    }
  }

  /** Creates a new instance of the specified class type, with special handling for Java Collections.
   *  If a Java Collection is specified via the interface, a concrete instantiation of it is returned.
   *  @param <T>   The desired type.
   *  @param clazz The desired type.
   *  @return The new object.
   *  @throws InstantiationException
   *  @throws IllegalAccessException
   */
  @SuppressWarnings( {"rawtypes", "CollectionWithoutInitialCapacity"})
  private static <T> T newInstance (Class<T> clazz) throws InstantiationException, IllegalAccessException {
    if (clazz.isInterface()) {
      // Need specific types for Java collections
      if (clazz.isAssignableFrom(ArrayList.class    )) return clazz.cast(new ArrayList());
      if (clazz.isAssignableFrom(LinkedList.class   )) return clazz.cast(new LinkedList());
      if (clazz.isAssignableFrom(LinkedHashMap.class)) return clazz.cast(new LinkedHashMap());
      if (clazz.isAssignableFrom(TreeMap.class      )) return clazz.cast(new TreeMap());
      if (clazz.isAssignableFrom(TreeSet.class      )) return clazz.cast(new TreeSet());
    }
    return clazz.newInstance();
  }

  /** Tries creating an object via a static method (or constructor).
   *  @param <T>    Type being created.
   *  @param <X>    Parameter type.
   *  @param clazz  Type being created.
   *  @param method Method name ("<init>" for constructor).
   *  @param type   Parameter type.
   *  @param value  The parameter value.
   *  @return The new instance or null if no matching method.
   *  @throws Exception If an exception is thrown while creating the instance.
   */
  private static <T,X> T tryStaticMethod (Class<T> clazz, String method, Class<X> type, X value) throws Exception {
    try {
      if (method.equals("<init>")) {
        Constructor<T> c = clazz.getConstructor(type);
        return c.newInstance(value);
      }
      else {
        Method m = clazz.getMethod(method, type);
        if (Modifier.isStatic(m.getModifiers())) {
          return clazz.cast(m.invoke(null, value));
        }
      }
    }
    catch (NoSuchMethodException e) {
      // ignore
    }
    return null;
  }

  /** Marks a class that has a special JSON export/import handling. The class must have a
   *  no-argument constructor or a factory method that can be used to create a new instance.
   */
  @Documented
  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public static @interface JsonObject {
    /** Provides the name of the object type.
     *  @return The name of the object type. If set to the empty string, the fully-qualified Java
     *          class name is used.
     */
    String name () default "";

    /** Provides the factory method to use. This is useful when the annotated type is an interface
     *  and/or the factory needs to determine the appropriate sub-class to use. The factory method
     *  must have the following signature:
     *  <pre>
     *    public static void myFactoryName (Map&lt;String,Object&gt; json);
     *  </pre>
     *  @return The name of the factory method or the empty string if none exists.
     */
    String factory () default "";

    /** Provides the validation method to use. This is useful when additional validation is required
     *  following object initialization. The validation method must have the following signature:
     *  <pre>
     *    public void myValidateMethod ();
     *  </pre>
     *  @return The name of the validation method or the empty string if none exists.
     */
    String validator () default "";

    /** Should a strict conversion be done on de-serialization? When doing a strict conversion, any
     *  extra fields will trigger an error. When doing a non-strict conversion, any extra fields
     *  are silently ignored.
     *  @return true if strict, false otherwise.
     */
    boolean strict () default false;

    /** Automatically handle all "set"/"get"/"is" methods as if they had {@link JsonEntry} annotations?
     *  When set to true *all* methods starting with "set", "get", or "is" (other than "getClass()")
     *  are assumed to have {@link JsonEntry} annotations. This is intended for use with simple
     *  container classes.
     *  @return true if strict, false otherwise.
     */
    boolean auto () default false;
  }

  /** Marks a get/set method in a {@link JsonObject} for special JSON export/import handling.
   *  Note that it is expected that there is a 1:1 mapping between get/set (or is/set) methods, so
   *  only one needs to be annotated. If both are annotated, the annotations must match.
   */
  @Documented
  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD,ElementType.FIELD})
  public static @interface JsonEntry {
    /** Provides the name of the entry.
     *  @return The entry name. If set to the empty string, the name is taken from the method name.
     */
    String name () default "";

    /** Is the entry required? If required it is an error not to specify the value.
     *  @return true if required, false otherwise.
     */
    boolean required () default false;

    /** Should any attempt to set this value be silently ignored? This is typically used on
     *  instance-specific fields that provide some limited utility in the JSON content but are
     *  not set directly on the Java side.
     *  @return true if calls to set the value should be silently ignored, false otherwise.
     */
    boolean ignoreSet () default false;

    /** What type should be used when calling the set method? This is used to specify a concrete
     *  type when the input is an interface or abstract class. Note that there is already special
     *  handling for most of the Java Collections types, {@link Number} and {@link CharSequence} so
     *  so they don't need to be specified.<br>
     *  <br>
     *  If defining a parameterized List or Map, set {@link #valtype()} and {@link #keytype} to
     *  specify the value (and key) types. <br>
     *  <br>
     *  If anything more complex, a special method will need to be declared that takes in the
     *  appropriate concrete types.
     *  <pre>
     *    #JsonEntry
     *    public void setFoo (Number val);                     // OK  (Number is OK)
     *
     *    #JsonEntry
     *    public void setFoo (List[CharSequence] val);         // OK  (Collections and CharSequence are OK)
     *
     *    #JsonEntry
     *    public void setFoo (MyInterface val);                // BAD (Can't instantiate MyInterface)
     *
     *    #JsonEntry
     *    public void setFoo (List[MyInterface] val);          // BAD (Can't instantiate MyInterface)
     *
     *    #JsonEntry
     *    public void setFoo (Map[String,MyInterface] val);    // BAD (Can't instantiate MyInterface)
     *
     *    #JsonEntry(type=MyInterfaceImpl.class)
     *    public void setFoo (MyInterface val);                // OK  (Will set using a MyInterfaceImpl)
     *
     *    #JsonEntry(type=ArrayList.class,valtype=MyInterfaceImpl.class)
     *    public void setFoo (List[MyInterface] val);          // OK  (Will set using a MyInterfaceImpl)
     *
     *    #JsonEntry(type=HashMap.class,keytype=String.class,valtype=MyInterfaceImpl.class)
     *    public void setFoo (Map[String,MyInterface] val);    // OK  (Will set using a MyInterfaceImpl)
     *
     *    #JsonEntry
     *    public void setFoo (List[List[MyInterface]] val);    // BAD (to complex, need to define new method)
     *  </pre>
     *  @return The type to use or Type.class to take the type from the set method's (or field's)
     *          parameter type.
     */
    Class<?> type () default Type.class;

    /** What type should be used when calling the set method? This specifies the value type for the
     *  value in a {@link Collection} or {@link Map} when {@link #type()} is set.
     *  @return The value type to use (ignored if {@link #type()} is set to the default value {@link Type}.
     */
    Class<?> valtype () default Object.class;

    /** What type should be used when calling the set method? This specifies the key type for the
     *  value in {@link Map}.
     *  @return The key type to use (ignored if {@link #type()} is set to the default value {@link Type}.
     */
    Class<?> keytype () default String.class;
  }

  /** A container for information about a {@link JsonObject}. */
  static class JsonObjectInfo<T> {
    final Class<T>                        clazz;       // The class reference
    final JsonObject                      object;      // The original annotation
    final Method                          factory;     // factory method (null=n/a)
    final Method                          validation;  // validation method (null=n/a)
    final SortedMap<String,JsonEntryInfo> entries;     // use SortedMap for consistent ordering

    /** Creates a new instance.
     *  @param clazz  The class type.
     *  @param object The object type.
     */
    @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch", "UseSpecificCatch"})
    JsonObjectInfo (Class<T> clazz, JsonObject object) {
      this.clazz   = clazz;
      this.object  = object;
      this.entries = new TreeMap<>();

      // ==== FIND FACTORY / VALIDATOR =============================================================
      try {
        this.factory = (object.factory().isEmpty())? null : clazz.getMethod(object.factory(), Map.class);
      }
      catch (Exception e) {
        throw new RuntimeException("Could not find factory method "+object.factory()+"(Map) in "+clazz,e);
      }
      try {
        this.validation = (object.validator().isEmpty())? null : clazz.getMethod(object.validator());
      }
      catch (Exception e) {
        throw new RuntimeException("Could not find validation method "+object.validator()+"() in "+clazz,e);
      }
      if ((factory != null) && !clazz.isAssignableFrom(factory.getReturnType())) {
        throw new RuntimeException("Conflicting return type on factory method "+object.factory()+"(Map) "
                                  +"expected return type of "+clazz+" but found "+factory.getReturnType());
      }

      // ==== LOAD ANNOTATED FIELDS ================================================================
      for (Field f : clazz.getFields()) {
        JsonEntry je = f.getAnnotation(JsonEntry.class);
        if (je == null) continue;
        String name = je.name();
        if (name.isEmpty()) name = f.getName();
        JsonEntryInfo jx = entries.get(name);

        if (jx != null) {
          throw new IllegalArgumentException("Duplicate JsonEntry in "+clazz+" for "+name+" found "+jx+" and "+f);
        }
        jx = new JsonEntryInfo(je);
        jx.field = f;
        entries.put(name, jx);
      }

      // ==== LOAD "OTHER METHODS" MAP =============================================================
      Map<String,Method> otherMethods = new LinkedHashMap<>(32);

      for (Method m : clazz.getMethods()) {
        JsonEntry je = m.getAnnotation(JsonEntry.class);
        String    n  = m.getName();
        Method    mx = otherMethods.get(n);

        if (je != null) {                                              // ANNOTATION FOUND
          otherMethods.put(n, null);                                   // force use of annotation
        }
        else if (!otherMethods.containsKey(n)) {                       // NEW ENTRY
          otherMethods.put(n, m);
        }
        else if (mx == null) {                                         // ALREADY ANNOTATED (OR ERROR)
                                                                       // force use of annotation
        }
        else if (n.startsWith("get") || n.startsWith("is")) {          // GET METHOD
          if (m.getParameterCount() == 0) {                            // prefer zero parameters (only one)
            otherMethods.put(n, m);
          }
        }
        else if (n.startsWith("set")) {                                // SET METHOD
          if (m.getParameterCount() == 1) {                            // prefer one parameter
            if (mx.getParameterCount() == 1) {
              otherMethods.put(n, null);                               // => error (duplicates)
            }
            else {
              otherMethods.put(n, m);                                  // => better option
            }
          }
        }
        else {                                                         // OTHER
                                                                       // ignore non-bean methods
        }
      }

      // ==== LOAD ANNOTATED METHODS ===============================================================
      for (Method m : clazz.getMethods()) {
        JsonEntry je       = m.getAnnotation(JsonEntry.class);
        String    n        = m.getName();
        Class<?>  type     = m.getReturnType();
        String    defName;         // default name
        String    setName  = null; // name for set method to look up
        String    getName1 = null; // name for get method to look up ('get' version)
        String    getName2 = null; // name for get method to look up ('is'  version)
        Method    set      = null; // set method
        Method    get      = null; // get method
        String    name;

        // ---- Handle auto=true -------------------------------------------------------------------
        if (je == null) {
          if (object.auto() && !Modifier.isStatic(m.getModifiers()) && n.startsWith("set")) {
            // trigget auto entries off the "set" method as this will also pull in "get" (or "is")
            je = AUTO_ENTRY;
          }
          else {
            continue; // skip this method
          }
        }

        // ---- Determine the default name and the paired method(s) --------------------------------
             if (n.startsWith("get")) { defName = n.substring(3); get = m; setName  = "set"+n.substring(3); }
        else if (n.startsWith("is" )) { defName = n.substring(2); get = m; setName  = "set"+n.substring(2); }
        else if (n.startsWith("set")) { defName = n.substring(3); set = m; getName1 = "get"+n.substring(3);
                                                                           getName2 =  "is"+n.substring(3); }
        else if ( type == Void.TYPE ) { defName = n;              set = m; getName1 = n; } // e.g. foo(int)
        else                          { defName = n;              get = m; setName  = n; } // e.g. foo()

        // ---- Determine the JSON name ------------------------------------------------------------
        if (!je.name().isEmpty()) {
          name = je.name();
        }
        else if (defName.isEmpty()) {
          name = "value"; // no-name get() and set(..) => "value" in JSON
        }
        else {
          String _name = (Character.toLowerCase(defName.charAt(0)) + defName.substring(1)); // getFoo => "foo"
          // We anticipate a lot of duplicates and hold them indefinitely, so use intern() to save
          // memory. Note that everything else stored is already a single instances.
          name = _name.intern();
        }

        // ---- Lookup the existing entry ----------------------------------------------------------
        JsonEntryInfo jx = entries.get(name);
        if (jx == null) {
          jx = new JsonEntryInfo(je);
          entries.put(name, jx);
        }
        else if (!jx.entry.equals(je)) {
          throw new IllegalArgumentException("Conflicting JsonEntry in "+clazz+" for "+name+" found "+m+" and "+jx
                                            +" with conflicting information");
        }

        // ---- Map get and set methods and check for duplicates -----------------------------------
        if (get != null) {
          if (jx.getMethod != null) {
            throw new IllegalArgumentException("Duplicate JsonEntry in "+clazz+" for "+name+" found "+get+" and "+jx.getMethod);
          }
          jx.getMethod = get;
        }
        if (set != null) {
          if (jx.setMethod != null) {
            throw new IllegalArgumentException("Duplicate JsonEntry in "+clazz+" for "+name+" found "+set+" and "+jx.setMethod);
          }
          jx.setMethod = set;
        }
        if ((jx.setMethod == null) && (setName != null) && !jx.entry.ignoreSet()) {
          jx.setMethod = otherMethods.get(setName);
        }
        if ((jx.getMethod == null) && (getName1 != null)) {
          jx.getMethod = otherMethods.get(getName1);

          if (otherMethods.get(getName2) != null) {
            if (jx.getMethod == null) {
              jx.getMethod = otherMethods.get(getName2);
            }
            else {
              throw new IllegalArgumentException("Duplicate JsonEntry in "+clazz+" for "+name+" found "+otherMethods.get(getName2)+" and "+jx.getMethod);
            }
          }
        }
      }

      // ==== VALIDATE ENTRIES =====================================================================
      for (Map.Entry<String,JsonEntryInfo> e : entries.entrySet()) {
        String        name = e.getKey();
        JsonEntryInfo jx   = e.getValue();

        if (jx.field != null) {
          if ((jx.getMethod != null) || (e.getValue().setMethod != null)) {
            throw new IllegalArgumentException("Duplicate JsonEntry in "+clazz+" for "+name+" found "+jx);
          }
        }
        else if (jx.getMethod == null) {
          throw new IllegalArgumentException("Invalid JsonEntry in "+clazz+" for "+name+" expected "
                                            +"GET method taking 0 parameters but found none");
        }
        else if (jx.getMethod.getParameterCount() != 0) {
          throw new IllegalArgumentException("Invalid JsonEntry in "+clazz+" for "+name+" expected "
                                            +"GET method taking 0 parameters but found "+jx.getMethod);
        }
        else if (jx.entry.ignoreSet()) {
          if (jx.setMethod != null) {
            throw new IllegalArgumentException("Invalid JsonEntry in "+clazz+" for "+name+" expected "
                                              +"no SET method but found "+jx.setMethod);
          }
        }
        else {
          if (jx.setMethod == null) {
            throw new IllegalArgumentException("Invalid JsonEntry in "+clazz+" for "+name+" expected "
                                              +"SET method taking 1 parameter but found none");
          }
          else if (jx.setMethod.getParameterCount() != 1) {
            throw new IllegalArgumentException("Invalid JsonEntry in "+clazz+" for "+name+" expected "
                                              +"SET method taking 1 parameter but found "+jx.setMethod);
          }
        }
      }
    }

    /** Converts to a JSON object.
     *  @param obj The Java object type.
     *  @return The JSON object.
     */
    @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch", "UseSpecificCatch"})
    public Map<String,Object> toJSON (T obj) {
      Map<String,Object> json = new LinkedHashMap<>(entries.size());
      for (Map.Entry<String,JsonEntryInfo> e : entries.entrySet()) {
        try {
          json.put(e.getKey(), e.getValue().get(obj));
        }
        catch (Exception ex) {
          throw new RuntimeException("Can not get "+e.getKey()+" in "+obj.getClass()+": "+ex, ex);
        }
      }
      return json;
    }

    /** Converts to a JSON object.
     *  @param obj The Java object type.
     *  @return The JSON object.
     */
    @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch", "UseSpecificCatch"})
    public T fromJSON (Map<String,Object> json) {
      // ==== OBJECT INIT ==========================================================================
      T obj;
      try {
        obj = (factory == null)? clazz.newInstance() : clazz.cast(factory.invoke(null, json));
      }
      catch (InvocationTargetException ex) {
        throw new RuntimeException("Can not initialize "+clazz, ex.getCause());
      }
      catch (Exception ex) {
        throw new RuntimeException("Can not initialize "+clazz, ex);
      }

      // ==== STRICT CHECKS ========================================================================
      if (object.strict()) {
        for (String name : json.keySet()) {
          if (!entries.containsKey(name)) {
            throw new RuntimeException("Can not initialize "+clazz+" due to illegal entry '"+name+"'");
          }
        }
      }

      // ==== SET VALUES ===========================================================================
      for (Map.Entry<String,JsonEntryInfo> en : entries.entrySet()) {
        String        name = en.getKey();
        JsonEntryInfo e    = en.getValue();
        Object        val  = json.get(name);

        if (!json.containsKey(name)) {
          if (e.entry.required()) {
            throw new RuntimeException("Can not initialize "+clazz+" due to missing required entry '"+name+"'");
          }
        }
        else {
          try {
            e.set(obj, val);
          }
          catch (Exception ex) {
            throw new RuntimeException("Can not set '"+name+"' in "+obj.getClass(), ex);
          }
        }
      }

      // ==== VALIDATE =============================================================================
      try {
        if (validation != null) {
          validation.invoke(obj);
        }
      }
      catch (InvocationTargetException ex) {
        throw new RuntimeException("Validation failed for "+clazz, ex.getCause());
      }
      catch (Exception ex) {
        throw new RuntimeException("Validation failed for "+clazz, ex);
      }
      return obj;
    }
  }

  /** A container for information about a {@link JsonEntry}. */
  static class JsonEntryInfo {
    final JsonEntry entry;
          Method    setMethod;
          Method    getMethod;
          Field     field;

    /** Creates a new instance.
     *  @param entry The entry
     */
    JsonEntryInfo (JsonEntry entry) {
      this.entry = entry;
    }

    @Override
    public String toString () {
      StringBuilder str = new StringBuilder(120);
      if (setMethod != null) {                                          str.append(setMethod); }
      if (getMethod != null) { if (str.length() > 0) str.append(" / "); str.append(getMethod); }
      if (field     != null) { if (str.length() > 0) str.append(" / "); str.append(field    ); }
      return str.toString();
    }

    /** Gets the return type.
     *  @return The object type.
     */
    public Type getType () {
      Class<?> clazz = entry.type();
      if (clazz != Type.class) {
        if (Collection.class.isAssignableFrom(clazz)) {
          return new ParamType(clazz, entry.valtype());
        }
        else if (Map.class.isAssignableFrom(clazz)) {
          return new ParamType(clazz, entry.keytype(), entry.valtype());
        }
        else {
          return clazz;
        }
      }
      else if (field != null) {
        return field.getGenericType();
      }
      else {
        return setMethod.getGenericParameterTypes()[0];
      }
    }

    /** Executes a get on the given object.
     *  @param obj The given object.
     *  @return The value.
     *  @throws IllegalAccessException
     *  @throws InvocationTargetException
     */
    public Object get (Object obj) throws IllegalAccessException, InvocationTargetException {
      return (field != null)? field.get(obj) : getMethod.invoke(obj);
    }

    /** Executes a set on the given object.
     *  @param obj The given object.
     *  @param val The value.
     *  @throws IllegalArgumentException
     *  @throws IllegalAccessException
     *  @throws InvocationTargetException
     */
    public void set (Object obj, Object val) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
      if (entry.ignoreSet()) return; // ignore any set attempts
      val = toType(getType(), val);
      if (field != null) field.set(obj, val);
      else               setMethod.invoke(obj, val);
    }
  }

  /** A variant on {@link ClassCastException} that allows us to differentiate exceptions thrown from
   *  inside {@link JsonUtilities#_toType(Type,Object)} vs from normal {@link ClassCastException}
   *  throws.
   */
  static final class TypeCastException extends ClassCastException {
    private static final long serialVersionUID = -2769692240467821376L;

    /** Creates a new instance.
     *  @param type The type to convert to.
     *  @param val  The value passed in.
     */
    TypeCastException (Type type, Object val) {
      this(type, val, (String)null);
    }

    /** Creates a new instance.
     *  @param type The type to convert to.
     *  @param val  The value passed in.
     *  @param msg  A message describing the conversion error.
     */
    TypeCastException (Type type, Object val, String msg) {
      super("Unable to convert "+((val  == null)? null : val.getClass())+" to "
                                +((type == null)? null : type)
                                +((msg  == null)? ""   : ": "+msg));
    }

    /** Creates a new instance.
     *  @param type  The type to convert to.
     *  @param val   The value passed in.
     *  @param cause The cause of the exception.
     */
    TypeCastException (Type type, Object val, Throwable cause) {
      this(type, val, (cause==null)? null : cause.getMessage());
      if (cause instanceof InvocationTargetException) {
        initCause(cause.getCause());
      }
      else if (cause instanceof ClassCastException) {
        // ignore ClassCastException
      }
      else if (cause != null) {
        initCause(cause);
      }
    }
  }

  /** A very basic implementation of {@link ParameterizedType}. */
  static final class ParamType implements ParameterizedType {
    private final Type   rawType;
    private final Type[] actTypes;

    ParamType (Type rawType, Type... actTypes) {
      this.rawType  = rawType;
      this.actTypes = actTypes;
    }

    @Override
    public String toString () {
      StringBuilder str = new StringBuilder(80);
      str.append(rawType);
      str.append('<');
      for (int i = 0; i < actTypes.length; i++) {
        if (i > 0) str.append(',');
        str.append(actTypes[i]);
      }
      str.append('>');
      return str.toString();
    }

    @Override public Type   getOwnerType           () { return null;  }
    @Override public Type   getRawType             () { return rawType; }
    @Override public Type[] getActualTypeArguments () { return actTypes; }
  }
}
