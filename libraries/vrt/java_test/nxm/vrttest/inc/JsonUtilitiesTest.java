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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nxm.vrttest.inc.JsonUtilities.JsonEntry;
import nxm.vrttest.inc.JsonUtilities.JsonObject;
import nxm.vrttest.inc.JsonUtilities.Mode;

import static nxm.vrttest.inc.JsonUtilities.FILE_EXT;
import static nxm.vrttest.inc.JsonUtilities.MEDIA_TYPE;
import static nxm.vrttest.inc.JsonUtilities.fromJSON;
import static nxm.vrttest.inc.JsonUtilities.isMediaTypeJSON;
import static nxm.vrttest.inc.JsonUtilities.parseNumber;
import static nxm.vrttest.inc.JsonUtilities.toJSON;
import static nxm.vrttest.inc.JsonUtilities.toType;
import static nxm.vrttest.inc.TestRunner.assertAssignableFrom;
import static nxm.vrttest.inc.TestRunner.assertEquals;

/** Tests for the {@link JsonUtilities} class. */
public class JsonUtilitiesTest implements Testable {

  @Override
  public Class<?> getTestedClass () {
    return JsonUtilities.class;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    tests.add(new TestSet("FILE_EXT",              this, "+testIsMediaTypeJSON"));
    tests.add(new TestSet("MEDIA_TYPE",            this, "+testIsMediaTypeJSON"));

    tests.add(new TestSet("fromJSON(..)",          this, "+testToType"));
    tests.add(new TestSet("isMediaTypeJSON(..)",   this,  "testIsMediaTypeJSON"));
    tests.add(new TestSet("parseNumber(..)",       this,  "testParseNumber"));
    tests.add(new TestSet("toJSON(..)",            this,  "testToJSON"));
    tests.add(new TestSet("toType(..)",            this,  "testToType"));

    return tests;
  }

  @Override
  public void done () throws Exception {
    // nothing to do
  }

  @Override
  public String toString () {
    return "Tests for JsonUtilities";
  }

  public void testIsMediaTypeJSON () {
    assertEquals("MEDIA_TYPE", "application/json", MEDIA_TYPE);
    assertEquals("FILE_EXT",   ".json",            FILE_EXT);

    assertEquals("isMediaTypeJSON('application/json')", true,  isMediaTypeJSON("application/json"));
    assertEquals("isMediaTypeJSON('text/json')",        true,  isMediaTypeJSON("text/json"));
    assertEquals("isMediaTypeJSON(MEDIA_TYPE)",         true,  isMediaTypeJSON(MEDIA_TYPE));
    assertEquals("isMediaTypeJSON('json')",             false, isMediaTypeJSON("json"));
    assertEquals("isMediaTypeJSON('application/foo')",  false, isMediaTypeJSON("application/foo"));

    try {
      isMediaTypeJSON(null);
      assertEquals("isMediaTypeJSON(null) causes NullPointerException", true, false);
    }
    catch (NullPointerException e) {
      assertEquals("isMediaTypeJSON(null) causes NullPointerException", true, true);
    }
  }

  /** Tests parseNumber(..) in error and null conditions. */
  private void _testParseNumber (String in) {
    if (in == null) {
      assertEquals("parseNumber("+in+")", null, parseNumber(in));
    }
    else {
      try {
        parseNumber(in);
        assertEquals("parseNumber("+in+") causes NumberFormatException", true, false);
      }
      catch (NumberFormatException e) {
        assertEquals("parseNumber("+in+") causes NumberFormatException", true, true);
      }
    }
  }

  /** Tests parseNumber(..) in error and null conditions. */
  private void _testParseNumber (String in, int exp) {
    Number num = parseNumber(in);
         if (num instanceof Byte   ) assertEquals("parseNumber("+in+")",       exp, num.intValue());
    else if (num instanceof Short  ) assertEquals("parseNumber("+in+")",       exp, num.intValue());
    else if (num instanceof Integer) assertEquals("parseNumber("+in+")",       exp, num.intValue());
    else if (num instanceof Long   ) assertEquals("parseNumber("+in+")", (long)exp, num.longValue());
    else if (num instanceof Float  ) assertEquals("parseNumber("+in+")",       exp, num);
    else                             assertEquals("parseNumber("+in+")",       exp, num);
    _testParseNumber(Long.toString(exp),   (long  )exp);
    _testParseNumber(Float.toString(exp),  (float )exp);
    _testParseNumber(Double.toString(exp), (double)exp);
  }
  /** Tests parseNumber(..) in error and null conditions. */
  private void _testParseNumber (String in, long exp) {
    Number num = parseNumber(in);
         if (num instanceof Byte   ) assertEquals("parseNumber("+in+")", exp, num.longValue());
    else if (num instanceof Short  ) assertEquals("parseNumber("+in+")", exp, num.longValue());
    else if (num instanceof Integer) assertEquals("parseNumber("+in+")", exp, num.longValue());
    else if (num instanceof Long   ) assertEquals("parseNumber("+in+")", exp, num.longValue());
    else if (num instanceof Float  ) assertEquals("parseNumber("+in+")", exp, num);
    else                             assertEquals("parseNumber("+in+")", exp, num);
  }
  /** Tests parseNumber(..) in error and null conditions. */
  private void _testParseNumber (String in, float exp) {
    Number num = parseNumber(in);
         if (num instanceof Byte   ) assertEquals("parseNumber("+in+")", exp, num.floatValue());
    else if (num instanceof Short  ) assertEquals("parseNumber("+in+")", exp, num.floatValue());
    else if (num instanceof Integer) assertEquals("parseNumber("+in+")", exp, num.floatValue());
    else if (num instanceof Long   ) assertEquals("parseNumber("+in+")", exp, num.floatValue());
    else if (num instanceof Float  ) assertEquals("parseNumber("+in+")", exp, num.floatValue());
    else                             assertEquals("parseNumber("+in+")", exp, num.floatValue());
  }
  /** Tests parseNumber(..) in error and null conditions. */
  private void _testParseNumber (String in, double exp) {
    Number num = parseNumber(in);
         if (num instanceof Byte   ) assertEquals("parseNumber("+in+")", exp, num.doubleValue());
    else if (num instanceof Short  ) assertEquals("parseNumber("+in+")", exp, num.doubleValue());
    else if (num instanceof Integer) assertEquals("parseNumber("+in+")", exp, num.doubleValue());
    else if (num instanceof Long   ) assertEquals("parseNumber("+in+")", exp, num.doubleValue());
    else if (num instanceof Float  ) assertEquals("parseNumber("+in+")", exp, num.doubleValue());
    else                             assertEquals("parseNumber("+in+")", exp, num.doubleValue());
  }

  public void testParseNumber () {
    _testParseNumber(null);
    _testParseNumber("ten");
    _testParseNumber("3.14.2015");
    _testParseNumber("3f14");
    _testParseNumber("3e1.14");
    _testParseNumber("0",                                   0                       );
    _testParseNumber("+0",                                 +0                       );
    _testParseNumber("-0",                                 -0                       );
    _testParseNumber("0.0",                                 0.0                     );
    _testParseNumber("+0.0",                                Double.valueOf("+0.0")  );
    _testParseNumber("-0.0",                                Double.valueOf("-0.0")  );
    _testParseNumber("42",                                  42                      );
    _testParseNumber("+42",                                +42                      );
    _testParseNumber("-42",                                -42                      );
    _testParseNumber("42.0",                                42.0                    );
    _testParseNumber("+42.0",                              +42.0                    );
    _testParseNumber("-42.0",                              -42.0                    );
    _testParseNumber("3.14",                                3.14                    );
    _testParseNumber("+3.14",                              +3.14                    );
    _testParseNumber("-3.14",                              -3.14                    );
    _testParseNumber(Integer.toString(Integer.MAX_VALUE),   Integer.MAX_VALUE       );
    _testParseNumber(Integer.toString(Integer.MIN_VALUE),   Integer.MIN_VALUE       );
    _testParseNumber(Long.toString(   Long.MAX_VALUE   ),   Long.MAX_VALUE          );
    _testParseNumber(Long.toString(   Long.MIN_VALUE   ),   Long.MIN_VALUE          );
    _testParseNumber(Float.toString( +Float.MAX_VALUE  ),  +Float.MAX_VALUE         );
    _testParseNumber(Float.toString( -Float.MAX_VALUE  ),  -Float.MAX_VALUE         );
    _testParseNumber(Float.toString(  Float.MIN_NORMAL ),   Float.MIN_NORMAL        );
    _testParseNumber(Double.toString(+Double.MAX_VALUE ),  +Double.MAX_VALUE        );
    _testParseNumber(Double.toString(-Double.MAX_VALUE ),  -Double.MAX_VALUE        );
    _testParseNumber(Double.toString( Double.MIN_NORMAL),   Double.MIN_NORMAL       );
    _testParseNumber("NaN",                                 Double.NaN              );
    _testParseNumber("Infinity",                            Double.POSITIVE_INFINITY);
    _testParseNumber("+Infinity",                           Double.POSITIVE_INFINITY);
    _testParseNumber("-Infinity",                           Double.NEGATIVE_INFINITY);

    int  j = 0x00000001;
    long k = 0x0000000000000001L;
    for (int i = -512; i < 512; i++      ) _testParseNumber(Integer.toString(i), i);
    for (int i = -512; i < 512; i++      ) _testParseNumber(Long.toString(i),    i);
    for (int i =    0; i <  32; i++,j<<=1) _testParseNumber(Integer.toString(j), j);
    for (int i =    0; i <  32; i++,j>>=1) _testParseNumber(Integer.toString(j), j);
    for (int i =    0; i <  64; i++,k<<=1) _testParseNumber(Long.toString(k),    k);
    for (int i =    0; i <  64; i++,k>>=1) _testParseNumber(Long.toString(k),    k);
  }

  private <T> void _testToType (Number num) {
    _testToType(Byte.class,    num, num.byteValue());
    _testToType(Short.class,   num, num.shortValue());
    _testToType(Integer.class, num, num.intValue());
    _testToType(Long.class,    num, num.longValue());
    _testToType(Float.class,   num, num.floatValue());
    _testToType(Double.class,  num, num.doubleValue());
    //_testToType(Byte.TYPE,     num, num.byteValue());
    //_testToType(Short.TYPE,    num, num.shortValue());
    //_testToType(Integer.TYPE,  num, num.intValue());
    //_testToType(Long.TYPE,     num, num.longValue());
    //_testToType(Float.TYPE,    num, num.floatValue());
    //_testToType(Double.TYPE,   num, num.doubleValue());
  }

  private <T> void _testToType (Class<T> type, Object in, Object out) {
    _testToType(type, type, in, out);
  }

  private <T> void _testToType (Type type, Class<T> clazz, Object in, Object out) {
    Object act = toType(type,in);
    if (act != null) {
      assertAssignableFrom("toType("+type+", "+in+")", clazz, act.getClass());
    }
    if ((act != null) && act.getClass().isArray() && out.getClass().isArray()) {
      assertEquals(        "toType("+type+", "+in+")", Array.getLength(out), Array.getLength(act));
      for (int i = 0; i < Array.getLength(act); i++) {
        assertEquals(      "toType("+type+", "+in+")", Array.get(out,i), Array.get(act,i));
      }
    }
    else {
      assertEquals(        "toType("+type+", "+in+")", out,  act);
    }
  }

  public void testToType () throws Exception {
    // ==== BASIC TYPES ============================================================================
    List<String> inList   = Arrays.asList("A","B","C","D");
    List<String> outList1 = new LinkedList<String>(inList);
    List<String> outList2 = new ArrayList<String>(inList);

    Map<String,Object> inMap  = new HashMap<String,Object>(4);
    Map<String,Object> outMap = new LinkedHashMap<String,Object>(4);
    inMap.put("one",   1);    outMap.put("one",   1);
    inMap.put("two",   2);    outMap.put("two",   2);
    inMap.put("three", 3);    outMap.put("three", 3);
    inMap.put("four",  4);    outMap.put("four",  4);

    _testToType(Object.class,        null,   null);
    _testToType(Integer.class,       42,     42  );
    _testToType(String.class,        "Foo",  "Foo");
    _testToType(outList1.getClass(), inList, outList1);
    _testToType(outList2.getClass(), inList, outList2);
    _testToType(outMap.getClass(),   inMap,  outMap);

    // ==== JSON OBJECT ============================================================================
    // tested in testToJSON()

    // ==== STRINGS ================================================================================
    BigDecimal dec  = new BigDecimal("123.456");
    URL        url  = new URL("http://example.com");
    UUID       uuid = UUID.randomUUID();
    Instant    now  = Instant.now();

    _testToType(Character.class,     "A",                      'A');
    _testToType(Mode.class,          "Auto",                   Mode.Auto);
    _testToType(String.class,        new StringBuilder("Foo"), "Foo");
    _testToType(BigDecimal.class,    dec.toString(),           dec);
    _testToType(URL.class,           url.toString(),           url);
    _testToType(UUID.class,          uuid.toString(),          uuid);
    _testToType(Instant.class,       now.toString(),           now);

    // ==== NUMBERS ================================================================================
    int  j = 0x00000001;
    long k = 0x0000000000000001L;
    for (int i = -512; i < 512; i++      ) _testToType(i);
    for (int i = -512; i < 512; i++      ) _testToType(i);
    for (int i =    0; i <  32; i++,j<<=1) _testToType(j);
    for (int i =    0; i <  32; i++,j>>=1) _testToType(j);
    for (int i =    0; i <  64; i++,k<<=1) _testToType(k);
    for (int i =    0; i <  64; i++,k>>=1) _testToType(k);
    _testToType(3.14);
    _testToType(Double.POSITIVE_INFINITY);
    _testToType(Double.NEGATIVE_INFINITY);
    _testToType(Double.NaN);

    // ==== GENERIC TYPES ==========================================================================
    Method sampleMethod1     = JsonUtilitiesTest.class.getDeclaredMethod("_sampleMethod1", Object.class, Object[].class, Map.class );
    Method sampleMethod2     = JsonUtilitiesTest.class.getDeclaredMethod("_sampleMethod2", Number.class, Number[].class, List.class);
    Type   T1                = sampleMethod1.getGenericReturnType();         // <T>                => T
    Type   T_extends_Number1 = sampleMethod2.getGenericReturnType();         // <T extends Number> => T
    Type   T2                = sampleMethod1.getGenericParameterTypes()[0];  // <T>                => T
    Type   T_extends_Number2 = sampleMethod2.getGenericParameterTypes()[0];  // <T extends Number> => T
    Type   TA                = sampleMethod1.getGenericParameterTypes()[1];  // <T> => T[]
    Type   T_extends_NumberA = sampleMethod2.getGenericParameterTypes()[1];  // <T extends Number> => T[]
    Type   Map_String_X      = sampleMethod1.getGenericParameterTypes()[2];  // Map<String,?>
    Type   List_X_extends_CS = sampleMethod2.getGenericParameterTypes()[2];  // List<? extends CharSequence>

    List<Object>       objList = new LinkedList<Object>();
    List<Number>       numList = new LinkedList<Number>();
    List<CharSequence> csList  = new ArrayList<CharSequence>();
    objList.add(1.0);    numList.add(1.0);    csList.add("1.0");
    objList.add(2);      numList.add(2);      csList.add("2");
    objList.add("3");    numList.add(3);      csList.add("3");

    Map<Object,Object> mapIn   = new HashMap<Object,Object>();
    Map<Object,Object> mapOut  = new HashMap<Object,Object>();
    mapIn.put("one", 1.0);     mapOut.put("one", 1.0);
    mapIn.put(2,     2.0);     mapOut.put("2",   2.0);
    mapIn.put(3.0,   3.0);     mapOut.put("3.0", 3.0);

    _testToType(T1,                Object.class,   123,     123);
    _testToType(T_extends_Number1, Number.class,   123,     123);
    _testToType(T2,                Object.class,   123,     123);
    _testToType(T_extends_Number2, Number.class,   123,     123);
    _testToType(TA,                Object[].class, objList, objList.toArray(new Object[3]));
    _testToType(T_extends_NumberA, Number[].class, objList, numList.toArray(new Number[3]));
    _testToType(Map_String_X,      Map.class,      mapIn,   mapOut);
    _testToType(List_X_extends_CS, List.class,     objList, csList);
  }

  private <T>                T _sampleMethod1 (T a, T[] b, Map<String,?>                c) { return null; }
  private <T extends Number> T _sampleMethod2 (T a, T[] b, List<? extends CharSequence> c) { return null; }

  private void _testFromToJSON (Object obj, String json) {
    _testFromToJSON(obj, json, obj);
  }

  private void _testFromToJSON (Object objIn, String json, Object objOut) {
    CharSequence jsonAuto     = toJSON(objIn, Mode.Auto);
    CharSequence jsonFastAuto = toJSON(objIn, Mode.FastAuto);
    CharSequence jsonCompact  = toJSON(objIn, Mode.Compact);
    CharSequence jsonFormal   = toJSON(objIn, Mode.Formal);
    Object       exp          = fromJSON(jsonAuto);

    assertEquals("toJSON("+objIn+",Compact)",  json,   jsonCompact.toString());
    assertEquals("toJSON("+objIn+",Auto)",     exp,    fromJSON(jsonAuto));
    assertEquals("toJSON("+objIn+",FastAuto)", exp,    fromJSON(jsonFastAuto));
    assertEquals("toJSON("+objIn+",Formal)",   exp,    fromJSON(jsonFormal));
    assertEquals("fromJSON("+jsonCompact+")",  objOut, fromJSON(jsonCompact));
  }

  private <T> void _testFromToJsonObj (Object objIn, String json, T objOut) {
    Class<?>     type         = objOut.getClass();
    CharSequence jsonAuto     = toJSON(objIn, Mode.Auto);
    CharSequence jsonFastAuto = toJSON(objIn, Mode.FastAuto);
    CharSequence jsonCompact  = toJSON(objIn, Mode.Compact);
    CharSequence jsonFormal   = toJSON(objIn, Mode.Formal);
    Object       exp          = fromJSON(type, jsonAuto);

    assertEquals("toJSON("+objIn+",Compact)",  json,   jsonCompact.toString());
    assertEquals("toJSON("+objIn+",Auto)",     exp,    fromJSON(type, jsonAuto));
    assertEquals("toJSON("+objIn+",FastAuto)", exp,    fromJSON(type, jsonFastAuto));
    assertEquals("toJSON("+objIn+",Formal)",   exp,    fromJSON(type, jsonFormal));
    assertEquals("fromJSON("+jsonCompact+")",  objOut, fromJSON(type, jsonCompact));
  }

  public void testToJSON () throws Exception {
    // ==== BASIC TYPES AND LISTS ==================================================================
    Integer[]     array   = { 1,2,3,4 };
    List<Integer> list    = Arrays.asList(array);
    String        listStr = "[1,2,3,4]";

    _testFromToJSON(null,                     "null");
    _testFromToJSON(true,                     "true");
    _testFromToJSON(false,                    "false");
    _testFromToJSON(0,                        "0" );
    _testFromToJSON(Double.NaN,               "NaN");
    _testFromToJSON(Double.POSITIVE_INFINITY, "Infinity");
    _testFromToJSON(Double.NEGATIVE_INFINITY, "-Infinity");
    _testFromToJSON('X',                      "\"X\"",      "X");  // Character => String
    _testFromToJSON(array,                    listStr,      list); // array => List
    _testFromToJSON(list,                     listStr,      list);

    // ==== NUMBERS ================================================================================
    int  j = 0x00000001;
    long k = 0x0000000000000001L;
    for (int i = -512; i < 512; i++      ) _testFromToJSON(i, Integer.toString(i));
    for (int i = -512; i < 512; i++      ) _testFromToJSON(i, Long.toString(i));
    for (int i =    0; i <  32; i++,j<<=1) _testFromToJSON(j, Integer.toString(j));
    for (int i =    0; i <  32; i++,j>>=1) _testFromToJSON(j, Integer.toString(j));
    for (int i =    0; i <  64; i++,k<<=1) _testFromToJSON(k, Long.toString(k));
    for (int i =    0; i <  64; i++,k>>=1) _testFromToJSON(k, Long.toString(k));

    // ==== MAPS ===================================================================================
    Map<String,Integer> map1 = new LinkedHashMap<String,Integer>();
    Map<String,Object>  map2 = new LinkedHashMap<String,Object>();
    StringBuilder       str1 = new StringBuilder();
    StringBuilder       str2 = new StringBuilder();
    for (int i = 0; i < 10; i++) {
      map1.put(Integer.toString(i), i);
      str1.append(((i == 0)? '{' : ','));
      str1.append('\"').append(i).append("\":").append(i);
    }
    str1.append('}');

    str2.append("{\"Numbers\":").append(str1);
    str2.append(",\"String\":\"Hello World!\"}");
    map2.put("Numbers", map1);
    map2.put("String",  "Hello World!");

    _testFromToJSON(map1, str1.toString());
    _testFromToJSON(map2, str2.toString());

    // ==== JSON OBJECT ============================================================================
    TestClassAuto           auto      = new TestClassAuto();
    TestClassAltName        altName   = new TestClassAltName();
    TestClassFooBarBaz      fooBarBaz = new TestClassFooBarBaz();
    TestClassFactory        factory   = new TestClassFactory("ABC123");
    TestClassTypes          types     = new TestClassTypes();
    Map<StringBuilder,Byte> myMap     = new LinkedHashMap<StringBuilder,Byte>();
    auto.set("Hello World!");
    altName.setFooBar("Four score and...");
    fooBarBaz.setFoo((byte)1);
    fooBarBaz.setBar(2);
    fooBarBaz.baz = 3.14;
    factory.myValidator();
    types.setChars(new StringBuilder("ABC"));
    types.setList(Arrays.asList(new StringBuilder("ONE"), new StringBuilder("TWO")));
    types.setMap(myMap);
    myMap.put(new StringBuilder("ONE"), (byte)1);
    myMap.put(new StringBuilder("TWO"), (byte)2);

    _testFromToJsonObj(auto,      "{\"value\":\"Hello World!\"}",       auto);
    _testFromToJsonObj(altName,   "{\"foo_bar\":\"Four score and...\"}",altName);
    _testFromToJsonObj(fooBarBaz, "{\"bar\":2,\"baz\":3.14,\"foo\":1}", fooBarBaz);
    _testFromToJsonObj(factory,   "{\"length\":6,\"text\":\"ABC123\"}", factory);
    _testFromToJsonObj(types,     "{\"chars\":\"ABC\",\"list\":[\"ONE\",\"TWO\"],"+
                                   "\"map\":{\"ONE\":1,\"TWO\":2}}",    types);

    try {
      fromJSON(TestClassAuto.class, "{\"oops\":123}");
      assertEquals("fromJSON(..) Exception if strict and requires are *not* violated", false, false);
    }
    catch (Exception e) {
      assertEquals("fromJSON(..) Exception if strict and requires are *not* violated", false, true);
    }

    try {
      fromJSON(TestClassFooBarBaz.class, "{}");
      assertEquals("fromJSON(..) Exception if required parameter is missing", true, false);
    }
    catch (Exception e) {
      assertEquals("fromJSON(..) Exception if required parameter is missing", true, true);
    }

    try {
      fromJSON(TestClassFooBarBaz.class, "{\"bar\":2,\"baz\":3.14,\"foo\":1,\"oops\":123}");
      assertEquals("fromJSON(..) Exception if strict is violated", true, false);
    }
    catch (Exception e) {
      assertEquals("fromJSON(..) Exception if strict is violated", true, true);
    }

    try {
      fromJSON(TestClassFooBarBaz.class, "{}");
      assertEquals("fromJSON(..) Exception if required parameter is missing", true, false);
    }
    catch (Exception e) {
      assertEquals("fromJSON(..) Exception if required parameter is missing", true, true);
    }

    if (true) {
      ClassLoader    cl    = JsonUtilitiesTest.class.getClassLoader();
      InputStream    input = cl.getResourceAsStream("nxm/vrttest/inc/JsonUtilitiesTest.json");
      BufferedReader in    = new BufferedReader(new InputStreamReader(input));
      StringBuilder  json  = new StringBuilder();
      String         line;

      while ((line = in.readLine()) != null) {
        json.append(line).append('\n');
      }
      in.close();
      TestClassGitHub obj = fromJSON(TestClassGitHub.class, json);
      assertAssignableFrom("fromJSON(..) for JsonUtilitiesTest.json", TestClassGitHub.class, obj.getClass());
      assertAssignableFrom("fromJSON(..) for JsonUtilitiesTest.json", GitHubItem.class,      obj.items.get(0).getClass());
    }
  }

  /** Root test class. */
  private static class TestClass {
    @Override
    public boolean equals (Object obj) {
      return (obj != null) && (obj.getClass() == getClass())
                           && toString().equals(obj.toString());
    }
    @Override
    public int hashCode () {
      return toString().hashCode();
    }
  }

  @JsonObject(auto=true)
  public static class TestClassAuto extends TestClass {
    private String value = "Not Set!";
    @Override             public String toString ()           { return value; }
    /* auto @JsonEntry */ public String get      ()           { return value; }
    /* auto @JsonEntry */ public void   set      (String val) { value = val;  }
  }

  @JsonObject(auto=true)
  public static class TestClassAltName extends TestClass {
    private String value = "Not Set!";
    @Override                  public String toString  ()           { return value; }
    @JsonEntry(name="foo_bar") public String getFooBar ()           { return value; }
    @JsonEntry(name="foo_bar") public void   setFooBar (String val) { value = val;  }
  }

  @JsonObject(strict=true)
  public static class TestClassFooBarBaz extends TestClass {
               private Byte   foo; // private and not a JsonEntry
               public  int    bar; // public but not a JsonEntry
    @JsonEntry public  double baz; // public and is a JsonEntry

    @Override                 public String toString ()         { return foo+"/"+bar+"/"+baz; }
    @JsonEntry(required=true) public Byte   getFoo   ()         { return foo; }
    @JsonEntry(required=true) public int    getBar   ()         { return bar; }
    @JsonEntry(required=true) public void   setFoo   (Byte val) { foo = val;  }
    /* auto @JsonEntry */     public void   setBar   (int  val) { bar = val;  }
  }

  @JsonObject(strict=true)
  public static class TestClassTypes extends TestClass {
    private CharSequence                               chars;
    private List<? extends Appendable>                 list;
    private Map<? extends Appendable,? extends Number> map;

    @Override              public String                     toString () { return chars+"/"+list+"/"+map; }
    /* auto @JsonEntry */  public CharSequence               getChars () { return chars; }
    /* auto @JsonEntry */  public List<? extends Appendable> getList  () { return list; }
    /* auto @JsonEntry */  public Map<? extends Appendable,
                                      ? extends Number>      getMap   () { return map; }

    @JsonEntry(type=StringBuilder.class)
    public void setChars (CharSequence val) {
      chars = val;
    }
    @JsonEntry(type=ArrayList.class,valtype=StringBuilder.class)
    public void setList (List<? extends Appendable> val) {
      list = val;
    }
    @JsonEntry(type=LinkedHashMap.class,keytype=StringBuilder.class,valtype=Byte.class)
    public void setMap (Map<? extends Appendable,? extends Number> val) {
      map = val;
    }
  }

  @JsonObject(factory="myFactoryMethod",validator="myValidator")
  public static class TestClassFactory extends TestClass {
              String  text;
              int     length;
    transient boolean valid;  // transient is unnecessary here, but is good example of usage

    public TestClassFactory (String str) { // not callable via no-argument constructor
      this.text   = str;
      this.length = str.length();
      this.valid  = false; // only set to true by myValidator()
    }

    public static TestClassFactory myFactoryMethod (Map<String,Object> map) {
      return new TestClassFactory("FACTORY");
    }

    public void myValidator () {
      if (length != text.length()) {
        throw new RuntimeException("Bad length value");
      }
      valid = true;
    }

    @Override  public String toString  ()           { return text+"/"+length+"/"+valid; }
    @JsonEntry public String getText   ()           { return text;   }
    @JsonEntry public int    getLength ()           { return length; }
    @JsonEntry public void   setText   (String val) { text   = val;  }
    @JsonEntry public void   setLength (int val)    { length = val;  }

    // not a @JsonEntry
    public void setText (String txt, int len) { text = txt; length = len; }
  }

  /** This represents the values from a GitHub API query made on 21 July 2015:
   *    https://api.github.com/search/repositories?q=Redhawk (see "JsonUtilitiesTest.json").
   */
  @JsonObject
  public static class TestClassGitHub {
    @JsonEntry public int              total_count;
    @JsonEntry public boolean          incomplete_results;
    @JsonEntry public List<GitHubItem> items;

    @Override
    public String toString () {
      return total_count+","+incomplete_results+","+items;
    }
  }
  @JsonObject
  public static abstract class AbstractGitHubItem {
    @JsonEntry public int         id;
    @JsonEntry public String      name;
    @JsonEntry public String      full_name;
    @JsonEntry public URL         url;
    @JsonEntry public URL         html_url;

    @Override
    public String toString () {
      return id+","+name+","+full_name+","+url+","+html_url;
    }
  }
  @JsonObject
  public static class GitHubItem extends AbstractGitHubItem {
    // This list is incomplete
    @JsonEntry public GitHubOwner owner;
    @JsonEntry(name="private") public boolean     isPrivate;
    @JsonEntry public String      description;
    @JsonEntry public boolean     fork;
    @JsonEntry public Instant     created_at;
    @JsonEntry public Instant     updated_at;
    @JsonEntry public Instant     pushed_at;
    @JsonEntry public String      homepage;
    @JsonEntry public int         size;
    @JsonEntry public int         stargazers_count;
    @JsonEntry public int         watchers_count;
    @JsonEntry public String      language;
    @JsonEntry public boolean     has_issues;
    @JsonEntry public boolean     has_downloads;
    @JsonEntry public boolean     has_wiki;
    @JsonEntry public boolean     has_pages;
    @JsonEntry public int         forks_count;
    @JsonEntry public URL         mirror_url;
    @JsonEntry public int         open_issues_count;
    @JsonEntry public int         forks;
    @JsonEntry public int         open_issues;
    @JsonEntry public int         watchers;
    @JsonEntry public String      default_branch;
    @JsonEntry public float       score;

    @Override
    public String toString () {
      return super.toString()+","+owner+","+isPrivate+","+description+","+fork+","+created_at+","
           + updated_at+","+pushed_at+","+homepage+","+size+","+stargazers_count+","+watchers_count
           + ","+language+","+has_issues+","+has_downloads+","+has_wiki+","+has_pages+","+forks_count
           + ","+mirror_url+","+open_issues_count+","+forks+","+open_issues+","+watchers+","
           + default_branch+","+score;
    }
  }
  @JsonObject
  public static class GitHubOwner extends AbstractGitHubItem {
    // This list is incomplete
    @JsonEntry public URL         avatar_url;
    @JsonEntry public GitHubType  type;
    @JsonEntry public boolean     site_admin;

    @Override
    public String toString () {
      return super.toString()+","+avatar_url+","+type+","+site_admin;
    }
  }
  public static enum GitHubType { User, Organization };
}