/*
 * Convert.java
 */

package com.lifepics.neuron.core;

import java.io.File;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;

/**
 * A set of utility functions for converting to and from strings,
 * with errors reported via ValidationExceptions.
 */

public class Convert {

   private static void fail(String key, String s) throws ValidationException {
      throw new ValidationException(Text.get(Convert.class,key,new Object[] { s }));
   }

   private static void fail(String key, String s, Throwable t) throws ValidationException {
      throw new ValidationException(Text.get(Convert.class,key,new Object[] { s }),t);
   }

// --- boolean ---

   private static final String BOOL_TRUE = "true";
   private static final String BOOL_FALSE = "false";

   public static boolean toBool(String s) throws ValidationException {
      if (s.equals(BOOL_TRUE)) return true;
      else if (s.equals(BOOL_FALSE)) return false;
      else {
         fail("e3",s);
         return false; // required by compiler
      }
   }

   public static String fromBool(boolean b) {
      return b ? BOOL_TRUE : BOOL_FALSE;
   }

// --- boolean (nullable) ---

   public static Boolean toNullableBool(String s) throws ValidationException {
      return (s == null) ? null : new Boolean(toBool(s));
   }

   public static String fromNullableBool(Boolean b) {
      return (b == null) ? null : fromBool(b.booleanValue());
   }

// --- n-digit integer ---

   /**
    * A special function that checks if a general integer is a valid n-digit integer.
    */
   public static void validateNDigit(int n, int i) throws ValidationException {
      if (i < 0 || i >= (int) Math.pow(10,n)) throw new ValidationException(Text.get(Convert.class,"e9",new Object[] { fromInt(i), fromInt(n) }));
   }

   /**
    * A special function that checks if a general integer is usable with fromIntNDigit.
    * (Arbitrary positive numbers are OK, they'll just be truncated.)
    */
   public static void validateUsable(int n, int i) throws ValidationException {
      if (i < 0) throw new ValidationException(Text.get(Convert.class,"e12",new Object[] { fromInt(i), fromInt(n) }));
   }

   public static int toIntNDigit(int n, String s) throws ValidationException {
      if (s.length() != n) throw new ValidationException(Text.get(Convert.class,"e10",new Object[] { s, fromInt(n) }));

      int i = 0;
      for (int j=0; j<n; j++) {
         char c = s.charAt(j);
         if (c < '0' || c > '9') fail("e11",s);
         i *= 10;
         i += (c - '0');
      }

      return i;
   }

   public static String fromIntNDigit(int n, int i) {

      // i may have more than n digits, in which case it's truncated.
      // i may not be negative; in fact the result will be
      // garbage in that case, because (i % 10) will be negative too.

      char[] c = new char[n];
      for (int j=n-1; j>=0; j--) {
         c[j] = (char) ('0' + (i % 10));
         i /= 10;
      }

      return new String(c);
   }

   public static String fromIntAtLeastNDigit(int n, int i) {

      // as in fromIntNDigit, assume i isn't negative

      String s = Integer.toString(i);

      int len = s.length();
      if (len >= n) return s;

      StringBuffer b = new StringBuffer(n);
      for (int j=len; j<n; j++) b.append('0');
      b.append(s);
      return b.toString();
   }

// --- integer ---

   public static int toInt(String s) throws ValidationException {
      try {
         return Integer.parseInt(s);
      } catch (NumberFormatException e) {
         fail("e1",s,e);
         return 0; // required by compiler
      }
   }

   public static String fromInt(int i) {
      return Integer.toString(i);
   }

// --- integer (nullable) ---

   public static Integer toNullableInt(String s) throws ValidationException {
      return (s == null) ? null : new Integer(toInt(s));
   }

   public static String fromNullableInt(Integer i) {
      return (i == null) ? null : fromInt(i.intValue());
   }

// --- long ---

   public static long toLong(String s) throws ValidationException {
      try {
         return Long.parseLong(s);
      } catch (NumberFormatException e) {
         fail("e2",s,e);
         return 0; // required by compiler
      }
   }

   public static String fromLong(long l) {
      return Long.toString(l);
   }

   public static String fromLongPretty(long l) {
      synchronized (prettyFormat) {
         return prettyFormat.format(l);
      }
   }
   private static DecimalFormat prettyFormat = new DecimalFormat("#,##0");

// --- long (nullable) ---

   public static Long toNullableLong(String s) throws ValidationException {
      return (s == null) ? null : new Long(toLong(s));
   }

   public static String fromNullableLong(Long l) {
      return (l == null) ? null : fromLong(l.longValue());
   }

   /**
    * A version of fromNullableLong that never returns a null string,
    * for use in error messages instead of XML files.
    */
   public static String describeNullableLong(Long l) {
      return (l == null) ? Text.get(Convert.class,"s6") : fromLong(l.longValue());
   }

// --- cents ---

   /**
    * Convert a string containing a money amount to an integer number of cents.
    * This only accepts things of the form N.NN, NN.NN, etc.
    * The point is, sometimes you don't want to deal with floating-point math.
    */
   public static int toCents(String s0) throws ValidationException {

      String s = s0.replaceAll(",",""); // too flexible, but not worth fixing.
      // the server format's the same as fromLongPretty, so we could use that,
      // except I don't want to deal with DecimalFormat's parsing generality.

      int len = s.length();
      if (len < 4 || s.charAt(len-3) != '.') fail("e16",s0);

      return   toIntNDigit(len-3,s.substring(0,len-3)) * 100
             + toIntNDigit(2,    s.substring(len-2  ));
      // use toIntNDigit instead of toInt so that minus signs aren't allowed
   }

   /**
    * toCents only accepts positive numbers, but this will produce negatives
    * if that's what you give it ... RuntimeException seemed like a bad idea.
    * Maybe eventually we'll change toCents to handle that.
    */
   public static String fromCents(int i) {

      String sign = "";
      if (i < 0) { i = -i; sign = "-"; }

      return sign + fromLongPretty(i/100) + "." + fromIntNDigit(2,i%100);
   }

// --- double ---

   // the conversion functions here are not strict inverses.
   // for one thing, the parse side accepts any precision.
   // for another, DecimalFormat is locale-sensitive, while
   // Double.parseDouble is not.  this is currently handled
   // by setting the locale at startup, because there's no
   // way to specify a locale for a DecimalFormat (?!).

   // accept any precision, but if you enter too many digits,
   // they'll be lost next time you go through an edit cycle.
   //
   public static double toDouble(String s) throws ValidationException {
      try {
         return Double.parseDouble(s);
      } catch (NumberFormatException e) {
         fail("e14",s,e);
         return 0; // required by compiler
      }
   }

   // if you generate arbitrary precision output, sometimes
   // you get weird behavior, like "0.007" -> "0.0070".
   // I haven't been able to find one that produces a string
   // ending in "0000001" or "9999999", but you never know.
   // so, use a DecimalFormat to make sure things stay tidy.
   //
   public static String fromDouble(double d) {
      synchronized (decimalFormat) {
         return decimalFormat.format(d);
      }
   }
   private static DecimalFormat decimalFormat = new DecimalFormat("0.####"); // allow exact sixteenths

// --- double (nullable) ---

   public static Double toNullableDouble(String s) throws ValidationException {
      return (s == null) ? null : new Double(toDouble(s));
   }

   public static String fromNullableDouble(Double d) {
      return (d == null) ? null : fromDouble(d.doubleValue());
   }

// --- date (internal) ---

   private static SimpleDateFormat dateFormatInternal = new SimpleDateFormat(Text.get(Convert.class,"s1"));
   static { dateFormatInternal.setLenient(false); }

   public static Date toDateInternal(String s) throws ValidationException {
      if (s.equals("")) return null;
      try {
         synchronized (dateFormatInternal) {
            return dateFormatInternal.parse(s);
         }
      } catch (ParseException e) {
         fail("e5",s,e);
         return null; // required by compiler
      }
   }

   public static String fromDateInternal(Date d) {
      if (d == null) return "";
      synchronized (dateFormatInternal) {
         return dateFormatInternal.format(d);
      }
   }

// --- date (internal, nullable) ---

   // this is a little weird ... the functions above
   // map the null date to the empty string.
   // on the from-side that case is covered up,
   // but on the to-side, the empty string
   // gets accepted as a null, and maps back to null.

   public static Date toNullableDateInternal(String s) throws ValidationException {
      return (s == null) ? null : toDateInternal(s);
   }

   public static String fromNullableDateInternal(Date d) {
      return (d == null) ? null : fromDateInternal(d);
   }

// --- date (external) ---

   private static SimpleDateFormat dateFormatExternal = new SimpleDateFormat(Text.get(Convert.class,"s2"));
   static { dateFormatExternal.setLenient(false); }

   public static Date toDateExternal(String s) throws ValidationException {
      if (s.equals("")) return null;
      try {
         synchronized (dateFormatExternal) {
            return dateFormatExternal.parse(s);
         }
      } catch (ParseException e) {
         fail("e6",s,e);
         return null; // required by compiler
      }
   }

   public static String fromDateExternal(Date d) {
      if (d == null) return "";
      synchronized (dateFormatExternal) {
         return dateFormatExternal.format(d);
      }
   }

// --- date (start) ---

   private static SimpleDateFormat dateFormatStartEnd = new SimpleDateFormat(Text.get(Convert.class,"s3"));
   static { dateFormatStartEnd.setLenient(false); }

   public static Date toDateStart(String s) throws ValidationException {
      if (s.equals("")) return null;
      try {
         synchronized (dateFormatStartEnd) {
            return dateFormatStartEnd.parse(s);
         }
      } catch (ParseException e) {
         fail("e7",s,e);
         return null; // required by compiler
      }
   }

   public static String fromDateStart(Date d) {
      if (d == null) return "";
      synchronized (dateFormatStartEnd) {
         return dateFormatStartEnd.format(d);
      }
   }

// --- date (end) ---

   private static Date add(Date date, int days) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(date);
      calendar.add(Calendar.DATE,days);
      return calendar.getTime();
   }

   public static Date toDateEnd(String s) throws ValidationException {
      if (s.equals("")) return null;
      try {
         synchronized (dateFormatStartEnd) {
            return add(dateFormatStartEnd.parse(s),1);
         }
      } catch (ParseException e) {
         fail("e8",s,e);
         return null; // required by compiler
      }
   }

   public static String fromDateEnd(Date d) {
      if (d == null) return "";
      synchronized (dateFormatStartEnd) {
         return dateFormatStartEnd.format(add(d,-1));
      }
   }

// --- time zone parse ---

   // a more recent complication: the Java time zone parser has bugs
   // in some versions, and/or when a Java version update happens.
   // in particular, it fails to recognize "-0400" as a valid time zone.
   // to fix this, just parse it ourselves, it's not a tricky format.
   //
   // I think I read something about it online one time, but I couldn't
   // find it again ... maybe it's too ancient now.  some bug with time
   // zones or locales or something?  the trouble is always with stores
   // that are in Newfoundland or somewhere like that.

   // to use this, remove the "Z" from the date format, set the zone
   // of the formatter to GMT, and replace f.parse(s) with parseZone(f,gap,s)

   private static int digit(String s, int i) throws ParseException {
      char c = s.charAt(i);
      if (c < '0' || c > '9') throw new ParseException(Text.get(Convert.class,"e17"),i);
      return (c - '0');
   }

   public static Date parseZone(SimpleDateFormat dateFormat, String gap, String s) throws ParseException {

      int i2 = s.length() - 5;
      int i1 = i2 - gap.length();
      if (i1 < 0) throw new ParseException(Text.get(Convert.class,"e18"),0);

      if ( ! s.substring(i1,i2).equals(gap) ) throw new ParseException(Text.get(Convert.class,"e19"),i1);

      int sign;
      char c = s.charAt(i2);
      switch (c) {
      case '+':  sign = +1;  break;
      case '-':  sign = -1;  break;
      default:   throw new ParseException(Text.get(Convert.class,"e20"),i2);
      }

      int h = digit(s,i2+1)*10 + digit(s,i2+2);
      if (h < 0 || h > 23) throw new ParseException(Text.get(Convert.class,"e21"),i2+1);

      int m = digit(s,i2+3)*10 + digit(s,i2+4);
      if (m < 0 || m > 59) throw new ParseException(Text.get(Convert.class,"e22"),i2+3);

      int offset = sign * (h*60 + m) * 60000; // int is large enough by a factor of 10

      Date d = dateFormat.parse(s.substring(0,i1));

      d.setTime(d.getTime() - offset); // no DST, thankfully
      return d;
   }

// --- date (absolute) ---

   // most times in the system come from the local computer clock.
   // I trust the user to have the hours and minutes set correctly,
   // but not the time zone, so I store and display most times
   // as purely local times, without any time zone information.
   // so, if the user changes the time zone and restarts, no matter.
   // note that the Java interpreter does not detect changes to the
   // default (system) time zone while the app is running.

   // however, for the invoice time (and maybe other times later),
   // that approach doesn't work.  the user has control over what
   // time zone should be used for display, so we have to store
   // the absolute time, and always use a time zone when displaying.
   // these should not automatically adjust if the user changes the
   // system time zone and restarts!

   private static SimpleDateFormat dateFormatAbsoluteInternalN = new SimpleDateFormat(Text.get(Convert.class,"s4n"));
   static { dateFormatAbsoluteInternalN.setLenient(false);
            dateFormatAbsoluteInternalN.setTimeZone(TimeZone.getTimeZone("GMT")); }

   private static SimpleDateFormat dateFormatAbsoluteInternalZ = new SimpleDateFormat(Text.get(Convert.class,"s4z"));
   static { dateFormatAbsoluteInternalZ.setLenient(false); }

   public static Date toDateAbsoluteInternal(String s) throws ValidationException {
      if (s.equals("")) return null;
      try {
         synchronized (dateFormatAbsoluteInternalN) {
            return parseZone(dateFormatAbsoluteInternalN," ",s);
         }
      } catch (ParseException e) {
         fail("e15",s,e);
         return null; // required by compiler
      }
   }

   public static String fromDateAbsoluteInternal(Date d) {
      if (d == null) return "";
      synchronized (dateFormatAbsoluteInternalZ) {
         return dateFormatAbsoluteInternalZ.format(d);
      }
   }

   private static SimpleDateFormat dateFormatAbsoluteExternal = new SimpleDateFormat(Text.get(Convert.class,"s5"));
   static { dateFormatAbsoluteExternal.setLenient(false); }

   public static String fromDateAbsoluteExternal(Date d, TimeZone timeZone) {
      if (d == null) return "";
      synchronized (dateFormatAbsoluteExternal) {
         dateFormatAbsoluteExternal.setTimeZone(timeZone);
         return dateFormatAbsoluteExternal.format(d);
      }
   }

// --- time ---

   // this is military HH:mm time.  reference info: cents conversion,
   // time zone parsing, IntervalField, TransferPanel.formatInterval.
   //
   // need a validation function since not every int value is valid

   public static int MINUTES_PER_DAY = 1440;

   public static void validateTime(int t) throws ValidationException {
      if (t < 0 || t >= MINUTES_PER_DAY) fail("e23",fromInt(t));
   }

   public static int toTime(String s) throws ValidationException {

      if (s.length() != 5 || s.charAt(2) != ':') fail("e24",s);

      int h = toIntNDigit(2,s.substring(0,2));
      int m = toIntNDigit(2,s.substring(3,5));

      if (h >= 24 || m >= 60) fail("e25",s);
      //
      // calling validateTime on the combined result is insufficient,
      // would allow e.g. 00:99 for 01:39

      return h*60 + m;
   }

   public static String fromTime(int t) {
      return fromIntNDigit(2,t/60) + ":" + fromIntNDigit(2,t%60);
   }

   /**
    * Compute the nonnegative delta t2-t1 taking wraparound into account.
    * Careful with equal input values, that produces 0.
    */
   public static int deltaTime(int t2,int t1) {
      int d = t2-t1;
      if (d < 0) d += MINUTES_PER_DAY;
      return d;
   }

// --- day of week ---

   // the DAY_OF_WEEK values in Calendar are one-based, and
   // range from SUNDAY = 1 to SATURDAY = 7.
   // the values here are offset by 1, to SUNDAY = 0, since
   // that makes the TOW calculations easier.

   // no validation method needed because we never edit these,
   // we just read and write them

   private static String[] dowTable = {
         Text.get(Convert.class,"dow0"),
         Text.get(Convert.class,"dow1"),
         Text.get(Convert.class,"dow2"),
         Text.get(Convert.class,"dow3"),
         Text.get(Convert.class,"dow4"),
         Text.get(Convert.class,"dow5"),
         Text.get(Convert.class,"dow6")
      };

   private static int find(String[] table, String s) {
      for (int i=0; i<table.length; i++) {
         if (s.equals(table[i])) return i;
      }
      return -1;
   }

   public static int toDOW(String s) throws ValidationException {
      int i = find(dowTable,s);
      if (i == -1) fail("e26",s);
      return i;
   }

   public static String fromDOW(int dow) {
      return dowTable[dow];
   }

// --- time of week ---

   // a TOW is a DOW plus a time within that day.  internally we
   // represent one as a single integer, up to 7*MINUTES_PER_DAY
   // (10080 not 10800)

   // no validation method needed because we never edit these,
   // we just read and write them

   public static int MINUTES_PER_WEEK = 10080;

   public static int toTOW(String s) throws ValidationException {

      if (s.length() != 9 || s.charAt(3) != ' ') fail("e27",s);

      int d = toDOW (s.substring(0,3));
      int t = toTime(s.substring(4,9));

      return d*MINUTES_PER_DAY + t;
   }

   public static String fromTOW(int tow) {
      return fromDOW(tow/MINUTES_PER_DAY) + ' ' + fromTime(tow%MINUTES_PER_DAY);
   }

   /**
    * Compute the nonnegative delta tow2-tow1 taking wraparound into account.
    * Careful with equal input values, that produces 0.
    */
   public static int deltaTOW(int tow2,int tow1) {
      int d = tow2-tow1;
      if (d < 0) d += MINUTES_PER_WEEK;
      return d;
   }

   /**
    * Add a delta (positive or negative) to a TOW.
    */
   public static int addTOW(int tow, int delta) {
      tow = (tow+delta) % MINUTES_PER_WEEK;
      if (tow < 0) tow += MINUTES_PER_WEEK;
      return tow;
   }

// --- file ---

   // the string "" is expressly forbidden.  it converts to an empty relative path,
   // which is not very useful, and allows path entries to be left blank in the UI.

   // these don't handle null.

   public static File toFile(String s) throws ValidationException {
      if (s.length() == 0) throw new ValidationException(Text.get(Convert.class,"e13"));
      return new File(s);
   }

   public static String fromFile(File f) {
      return f.getAbsolutePath();
   }

// --- file (nullable) ---

   public static File toNullableFile(String s) throws ValidationException {
      return (s == null) ? null : toFile(s);
   }

   public static String fromNullableFile(File f) {
      return (f == null) ? null : f.getAbsolutePath();
   }

// --- level ---

   public static Level toLevel(String s) throws ValidationException {
      try {
         return Level.parse(s);
      } catch (IllegalArgumentException e) {
         fail("e4",s,e);
         return null; // required by compiler
      }
   }

   public static String fromLevel(Level l) {
      return l.toString();
   }

// --- int array ---

   // array length n should be at least two, otherwise why are you using this?

   private static String S_COMMA = ",";
   private static char   C_COMMA = ',';

   public static int[] toIntArray(String s, int n) throws ValidationException {
      String[] sa = s.split(S_COMMA,-1); // -1 to stop weird default behavior
      if (sa.length != n) throw new ValidationException(Text.get(Convert.class,"e28",new Object[] { s, fromInt(n) }));
      int[] a = new int[n];
      for (int i=0; i<a.length; i++) {
         a[i] = toInt(sa[i]);
      }
      return a;
   }

   public static String fromIntArray(int[] a) {
      StringBuffer b = new StringBuffer();
      for (int i=0; i<a.length; i++) {
         if (i != 0) b.append(C_COMMA);
         b.append(fromInt(a[i]));
      }
      return b.toString();
   }

// --- double array ---

   public static double[] toDoubleArray(String s, int n) throws ValidationException {
      String[] sa = s.split(S_COMMA,-1); // -1 to stop weird default behavior
      if (sa.length != n) throw new ValidationException(Text.get(Convert.class,"e29",new Object[] { s, fromInt(n) }));
      double[] a = new double[n];
      for (int i=0; i<a.length; i++) {
         a[i] = toDouble(sa[i]);
      }
      return a;
   }

   public static String fromDoubleArray(double[] a) {
      StringBuffer b = new StringBuffer();
      for (int i=0; i<a.length; i++) {
         if (i != 0) b.append(C_COMMA);
         b.append(fromDouble(a[i]));
      }
      return b.toString();
   }

// --- flex array ---

   // array length n should be at least two, otherwise why are you using this?

   public static int[] makeUniform(int a0, int n) {
      int[] a = new int[n];
      for (int i=0; i<a.length; i++) {
         a[i] = a0;
      }
      return a;
   }

   public static boolean isUniform(int[] a) {
      for (int i=1; i<a.length; i++) {
         if (a[i] != a[0]) return false;
      }
      return true;
   }

   public static int[] toFlexArray(String s, int n) throws ValidationException {
      if (s.indexOf(C_COMMA) == -1) {
         return makeUniform(toInt(s),n);
      } else {
         return toIntArray(s,n);
      }
   }

   public static String fromFlexArray(int[] a) {
      if (isUniform(a)) {
         return fromInt(a[0]);
      } else {
         return fromIntArray(a);
      }
   }

// --- flex array (double) ---

   public static double[] makeUniformDouble(double a0, int n) {
      double[] a = new double[n];
      for (int i=0; i<a.length; i++) {
         a[i] = a0;
      }
      return a;
   }

   public static boolean isUniformDouble(double[] a) {
      for (int i=1; i<a.length; i++) {
         if (a[i] != a[0]) return false;
      }
      return true;
   }

   public static double[] toFlexArrayDouble(String s, int n) throws ValidationException {
      if (s.indexOf(C_COMMA) == -1) {
         return makeUniformDouble(toDouble(s),n);
      } else {
         return toDoubleArray(s,n);
      }
   }

   public static String fromFlexArrayDouble(double[] a) {
      if (isUniformDouble(a)) {
         return fromDouble(a[0]);
      } else {
         return fromDoubleArray(a);
      }
   }

}

