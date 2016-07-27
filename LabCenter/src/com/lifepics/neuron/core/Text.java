/*
 * Text.java
 */

package com.lifepics.neuron.core;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A utility class for obtaining localized text.<p>
 *
 * When you want to obtain a piece of localized text,
 * you must specify which class the text is associated with.
 * This can be done either by naming the class object
 * or by providing an instance of the class,
 * which you can often do conveniently using "this".
 * The text is then retrieved from the file Text.properties
 * in the package associated with that class.
 * The properties file must be stored in the classpath.
 */

public class Text {

   /**
    * Get a fixed piece of localized text.
    *
    * @param o An object that specifies which class the text is associated with.
    *          The object can be the class object or an instance of the desired class.
    * @param key The key used to look up the text in the properties file.
    *            The final part of the class name is applied as a prefix.
    */
   public static String get(Object o, String key) {
      Class c = (o instanceof Class) ? (Class) o : o.getClass();

      String name = c.getName();
      int i = name.lastIndexOf('.');

      String pckage = (i == -1) ? ""   : name.substring(0,i+1); // includes dot
      String prefix = (i == -1) ? name : name.substring(  i+1);

      // we could also get the package name via c.getPackage().getName(),
      // but we already need to do most of the work to get the prefix

      ResourceBundle bundle = ResourceBundle.getBundle(pckage + "Text",Locale.getDefault(),c.getClassLoader()); // (*)
      return bundle.getString(prefix + '.' + key);

      // getBundle and getString can throw MissingResourceException,
      // but we're not required to catch it ... and we shouldn't, either,
      // because it only happens as a result of programmer error

      // (*) use c.getClassLoader() instead of the default Text.class.getClassLoader()
      // so that invoke targets in the installer can have their own message bundles.
      // ResourceBundle may cache the bundle data, but that's not a problem in any way:
      //
      // Q: won't ResourceBundle hold a reference to the class loader and prevent GC?
      // A: no, the bundle data is just a Properties object, a collection of strings.
      // Q: does the cached data create interference across class loaders?
      // A: no, otherwise auto-updated LabCenters would use text from the old version.
      //    I tested it with nested invoke class loaders too, just to be sure.
      // Q: but, if the cache tracks the class loader, doesn't that hold a reference
      //    to the class loader?
      // A: yeah, I guess, but if you look at the Sun code it seems like they've got
      //    some kind of weak reference thing going on where it can clean up anyways.
      //
      // also, note that class loaders search top down, not bottom up.  if you build
      // an invoke jar with class that overlaps the installer, you get the installer
      // class, not the invoke class, no matter who tries to load it.  same goes for
      // the resource bundles.
      //
      // so, basically everything is fine as long as the contents of the invoke jar
      // don't overlap the installer.  different invoke jars can overlap each other
      // with no trouble -- I'm only putting them in different packages as a matter
      // of style, so that they don't carry each others' text.
   }

   /**
    * Get a fixed integer (for UI layout).
    */
   public static int getInt(Object o, String key) {
      try {
         return Convert.toInt(get(o,key));
      } catch (ValidationException e) {
         return 0;
         // programmer error, will be apparent in layout
      }
   }

   /**
    * Get localized text with arguments substituted in.
    *
    * @param o An object that specifies which class the text is associated with.
    *          The object can be the class object or an instance of the desired class.
    * @param key The key used to look up the text in the properties file.
    *            The final part of the class name is applied as a prefix.
    * @param args The arguments to be substituted.
    *             The syntax "new Object[] { ... }" is particularly useful.
    */
   public static String get(Object o, String key, Object[] args) {
      return MessageFormat.format(get(o,key),args);
   }

}

