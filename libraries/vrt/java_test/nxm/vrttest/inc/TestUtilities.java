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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/** Utility methods for use during testing. */
public final class TestUtilities {
  private TestUtilities () { } // prevent instantiation

  /** Special float values. There is a 1:1 correspondence between
   *  <tt>SPECIAL_FLOAT_BITS[n]</tt> and <tt>SPECIAL_FLOAT_VALS[n]</tt> with
   *  the only difference being the use of a bit pattern vs the corresponding
   *  floating-point value.
   */
  public static final int[] SPECIAL_FLOAT_BITS = {
    0xFF800000,      // -Inf
    0xFF7FFFFF,      // Largest  Negative Normalized
    0xC0000000,      // -2.0
    0xBF800000,      // -1.0
    0xBF000000,      // -0.5
    0x80800000,      // Smallest Negative Normalized
    0x807FFFFF,      // Largest  Negative Denorm
    0x80000001,      // Smallest Negative Denorm
    0x80000000,      // -0.0
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

  /** Special float values. */
  public static final float[] SPECIAL_FLOAT_VALS = new float[SPECIAL_FLOAT_BITS.length];
  static {
    for (int i = 0; i < SPECIAL_FLOAT_BITS.length; i++) {
      SPECIAL_FLOAT_VALS[i] = Float.intBitsToFloat(SPECIAL_FLOAT_BITS[i]);
    }
  }

  /** Special double values. There is a 1:1 correspondence between
   *  <tt>SPECIAL_FLOAT_BITS[n]</tt> and <tt>SPECIAL_FLOAT_VALS[n]</tt> with
   *  the only difference being the use of a bit pattern vs the corresponding
   *  floating-point value.
   */
  public static final long[] SPECIAL_DOUBLE_BITS = {
    0xFFF0000000000000L,      // -Inf
    0xFFEFFFFFFFFFFFFFL,      // Largest  Negative Normalized
    0xC000000000000000L,      // -2.0
    0xBFF0000000000000L,      // -1.0
    0xBFE0000000000000L,      // -0.5
    0x8010000000000000L,      // Smallest Negative Normalized
    0x80FFFFFFFFFFFFFFL,      // Largest  Negative Denorm
    0x8000000000000001L,      // Smallest Negative Denorm
    0x8000000000000000L,      // -0.0
    0x0000000000000000L,      // +0.0
    0x0000000000000001L,      // Smallest Positive Denorm
    0x00FFFFFFFFFFFFFFL,      // Largest  Positive Denorm
    0x0010000000000000L,      // Smallest Positive Normalized
    0x3FE0000000000000L,      // +0.5
    0x3FF0000000000000L,      // +1.0
    0x4000000000000000L,      // +2.0
    0x7FEFFFFFFFFFFFFFL,      // Largest  Positive Normalized
    0x7FF0000000000000L,      // +Inf

    0x3FB999999999999AL,      // +0.1
    0x4005BF0A8B145769L,      // e
    0x3FF6A09E667F3BCDL,      // SQRT(2.0)
    0x3FFBB67AE8584CAAL,      // SQRT(3.0)
    0x40094C583ADA5B53L,      // SQRT(10.0)

    0x7FF0000000000001L,      // NaN ("Smallest" Value)
    0x7FF8000000000000L,      // NaN (Java Normalized Value)
    0x7FFFFFFFFFFFFFFFL,      // NaN ("Largest" Value)
  };

  /** Special double values. */
  public static final double[] SPECIAL_DOUBLE_VALS = new double[SPECIAL_DOUBLE_BITS.length];
  static {
    for (int i = 0; i < SPECIAL_FLOAT_BITS.length; i++) {
      SPECIAL_DOUBLE_VALS[i] = Double.longBitsToDouble(SPECIAL_DOUBLE_BITS[i]);
    }
  }

  /** The package list to use with <tt>getClassForName(..)</tt>. */
  private static final String[] PACKAGE_LIST = { "", "java.lang.", "nxm.vrt.lib." };


  /** Creates a new instance of the tested class using the default constructor.
   *  @param <T>   The object type.
   *  @param clazz The object class.
   *  @return The new instance.
   */
  public static <T> T newInstance (Class<T> clazz) {
    try {
      return clazz.newInstance();
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to instantiate "+clazz, e);
    }
  }

  /** Creates a new instance of the tested class using a specific constructor.
   *  @param <T>       The object type.
   *  @param clazz     The object class.
   *  @param signature The constructor signature.
   *  @param vals      The values to pass to the constructor.
   *  @return The new instance.
   */
  public static <T> T newInstance (Class<T> clazz, String signature, Object ... vals) {
    try {
      return getConstructor(clazz, signature).newInstance(vals);
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to instantiate "+clazz, e);
    }
  }

  /** Gets a class constructor based on the given signature. */
  @SuppressWarnings("unchecked")
  private static <C> Constructor<C> getConstructor (Class<C> clazz, String signature) throws NoSuchMethodException, ClassNotFoundException {
    List<Class<?>> types = new ArrayList<Class<?>>(8);

    int openParen = signature.indexOf('(');
    if ((openParen >= 0) && signature.endsWith(")")) {
      if (clazz == null) {
        clazz = (Class<C>)getClassForName(signature.substring(0, openParen));
      }
      signature = signature.substring(openParen+1, signature.length()-1);
    }
    signature = signature.trim();

    if (!signature.isEmpty()) {
      String[] typeNames = signature.split("\\s*,\\s*");

      for (String typeName : typeNames) {
        types.add(getClassForName(typeName));
      }
    }

    Class<?>[] _types = types.toArray(new Class<?>[types.size()]);
    return clazz.getConstructor(_types);
  }

  /** Gets a class object for the named class with checks for primitive types
   *  and arrays.
   */
  private static Class<?> getClassForName (String name) throws NoSuchMethodException, ClassNotFoundException {
    if (name.equals("byte"   )) return Byte.TYPE;
    if (name.equals("short"  )) return Short.TYPE;
    if (name.equals("int"    )) return Integer.TYPE;
    if (name.equals("long"   )) return Long.TYPE;
    if (name.equals("float"  )) return Float.TYPE;
    if (name.equals("double" )) return Double.TYPE;
    if (name.equals("char"   )) return Character.TYPE;
    if (name.equals("boolean")) return Boolean.TYPE;

    // ==== TRY NON-ARRAY ======================================================
    if (!name.endsWith("[]")) {
      for (String pkg : PACKAGE_LIST) {
        try {
          return Class.forName(pkg + name);
        }
        catch (ClassNotFoundException e) {
          // ignore
        }
      }
      throw new ClassNotFoundException(name);
    }

    // ==== TRY ARRAY ==========================================================
    StringBuilder prefix = new StringBuilder(32);
    while (name.endsWith("[]")) {
      prefix.append('[');
      name = name.substring(0, name.length()-2);
    }

    if (name.equals("byte"   )) return Class.forName(prefix+"B");
    if (name.equals("short"  )) return Class.forName(prefix+"S");
    if (name.equals("int"    )) return Class.forName(prefix+"I");
    if (name.equals("long"   )) return Class.forName(prefix+"L");
    if (name.equals("float"  )) return Class.forName(prefix+"F");
    if (name.equals("double" )) return Class.forName(prefix+"D");
    if (name.equals("char"   )) return Class.forName(prefix+"C");
    if (name.equals("boolean")) return Class.forName(prefix+"Z");

    String _name = name.replace('.','/');
    for (String pkg : PACKAGE_LIST) {
      String _pkg = pkg.replace('.','/');
      try {
        return Class.forName(prefix + _pkg + _name);
      }
      catch (ClassNotFoundException e) {
        // ignore
      }
    }
    throw new ClassNotFoundException(prefix + _name);
  }
}
