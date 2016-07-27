/*
 * Resource.java
 */

package com.lifepics.neuron.misc;

import java.io.InputStream;
import java.net.URL;
import java.util.MissingResourceException;

/**
 * A utility class for accessing resources (files) in the class path.
 */

public class Resource {

   private static String getPath(Class c, String name) {
      return c.getPackage().getName().replace('.','/') + "/" + name;
   }

   public static URL getResource(Object o, String name) {
      Class c = (o instanceof Class) ? (Class) o : o.getClass();

      URL resource = c.getClassLoader().getResource(getPath(c,name));
      if (resource == null) throw new MissingResourceException("",c.getName(),name); // programmer error

      return resource;
   }

   public static InputStream getResourceAsStream(Object o, String name) {
      Class c = (o instanceof Class) ? (Class) o : o.getClass();

      InputStream resource = c.getClassLoader().getResourceAsStream(getPath(c,name));
      if (resource == null) throw new MissingResourceException("",c.getName(),name); // programmer error

      return resource;
   }

}

