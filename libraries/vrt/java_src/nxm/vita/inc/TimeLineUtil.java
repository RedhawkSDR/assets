package nxm.vita.inc;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import nxm.sys.inc.InternalUseOnly;
import nxm.sys.lib.Keywords;
import nxm.sys.lib.MidasException;
import nxm.sys.lib.Time;
import nxm.sys.lib.TimeLine;

/** Utility methods that belong in the {@link TimeLine} class. */
public final class TimeLineUtil {
  private TimeLineUtil () { }

  /** Convert this TimeLine to Map that is suitable to store as Keywords in BLUE files with high
   *  precision time entries following the BLUE TC (Time Code) keywords as outlined in the JICD 4.2
   *  ICD. This follows the Platinum BLUE Data File Exchange Format Standard rev 4.
   *  @param dbpe     Bytes per element (to convert file offset in bytes to offset in elements).
   *  @throws IllegalStateException when this timeline does not have any entries
   *  @return Map of keywords to store the high precision timeline in the extended header of a BLUE file
   *  @since NeXtMidas 3.7.2
   *  @see #fromBlueKeywords
   */
  // When inserted into TimeLine.java the 'tl' parameter becomes 'this' and everything from START
  // to END can be deleted.
  @InternalUseOnly
  public static Map<String,Object> toBlueKeywords (TimeLine tl, double dbpe) throws IllegalStateException { // package-private
    // === START ===
    int      current;
    double[] offsets;
    Time[]   times;

    try {
      Field _current = TimeLine.class.getDeclaredField("current");   _current.setAccessible(true);
      Field _offsets = TimeLine.class.getDeclaredField("offsets");   _offsets.setAccessible(true);
      Field _times   = TimeLine.class.getDeclaredField("times");     _times.setAccessible(true);

      current = (Integer )_current.get(tl);
      offsets = (double[])_offsets.get(tl);
      times   = (Time[]  )_times.get(tl);
    }
    catch (ReflectiveOperationException|SecurityException|IllegalArgumentException e) {
      throw new MidasException("Unable to access fields in TimeLine", e);
    }
    // === END ===

    if (current < 0) throw new IllegalStateException("TimeLine is empty");

    Map<String,Object> map = new LinkedHashMap<>((current+1)*3);
    for (int i=0,j=1; i <= current; i++,j++) {
      map.put("TCSAMPLE_"+j, offsets[i]/dbpe);
      map.put("TC_WHOLE_"+j, times[i].getWSec());
      map.put("TC_FRAC_"+j,  times[i].getFSec());
    }
    return map;
  }

  /** Try to create high precision time-line from a {@link Keywords} object that follows the
   *  standard BLUE TC (Time Code) keywords as outlined in the JICD 4.2 ICD.
   *  @param kw       The Keywords from DataFile with Platinum time-line keywords.
   *  @param dfDelta  The DataFile's time delta (XDELTA/YDELTA) per element.
   *  @param dbpe     Bytes per element (to convert file offset in bytes to offset in elements).
   *  @return The new TimeLine instance from BLUE TC keywords if successful,
   *          otherwise null if unable to extract the time-line.
   *  @see #toBlueKeywords
   *  @since NeXtMidas 3.7.2
   */
  @InternalUseOnly
  public static TimeLine fromBlueKeywords (Keywords kw, double dfDelta, double dbpe) {
    double   tlt = 250e-12; // 1/4 nanosecond (matches SDDS)
    TimeLine tl  = new TimeLine(-1, dfDelta / dbpe, tlt);
    boolean  any = false;

    Double offset;
    for (int i = 1; (offset = kw.getD("TCSAMPLE_"+i, null)) != null; i++) {
      Double wsec = kw.getD("TC_WHOLE_"+i, null);
      Double fsec = kw.getD("TC_FRAC_"+i, null);

      if (wsec == null) {
        throw new MidasException("Invalid BLUE TC keywords, missing TC_WHOLE_"+i);
      }
      if (fsec == null) {
        throw new MidasException("Invalid BLUE TC keywords, missing TC_FRAC_"+i);
      }

      any = true;
      tl.setTimeAt(offset*dbpe, new Time(wsec, fsec));
    }
    //tl.format = Format.BLUE_HIGH_PRECISION;
    return (any)? tl : null;
  }

}
