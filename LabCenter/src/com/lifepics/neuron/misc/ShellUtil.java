/*
 * ShellUtil.java
 */

package com.lifepics.neuron.misc;

/**
 * Some JNI related to folders and shortcuts (Windows-specific).
 */

public class ShellUtil {

// --- availability ---

   private static boolean available = false;

   static {
      try {
         System.loadLibrary("jnishell");
         available = true;
      } catch (UnsatisfiedLinkError e) {
         // available remains false
      } catch (NoClassDefFoundError e) {
         // available remains false
      }
      // on a second attempt you get NoClassDefFoundError,
      // but there's no second attempt here, so no matter.
      // on the other hand, no harm in catching it.
   }

   public static boolean isAvailable() { return available; }

// --- folders ---

   // there are lots more, just search for CSIDL_STARTUP
   public static final int CSIDL_STARTUP = 7;
   public static final int CSIDL_STARTMENU = 11;
   public static final int CSIDL_DESKTOPDIRECTORY = 16;
   public static final int CSIDL_COMMON_STARTUP = 24; // "common" means "all users"
   public static final int CSIDL_COMMON_STARTMENU = 22;
   public static final int CSIDL_COMMON_DESKTOPDIRECTORY = 25;

   /**
    * Get the path to the folder indicated by the given code.
    *
    * @return The path, or null if there was some difficulty.
    *         Note, the string does not end with a delimiter.
    */
   public native static String getFolderPath(int folder);

// --- shortcuts ---

   /**
    * Create a shortcut at the location given in createPath.
    * The other strings are nullable, with null meaning don't
    * set the corresponding property ... however a shortcut
    * without a targetPath isn't likely to turn out very well.
    *
    * @return A HRESULT code ... nonnegative means success.
    */
   public native static int createShortcut(String createPath,
                                           String targetPath,
                                           String arguments,
                                           String description,
                                           String workingDirectory,
                                           String iconPath,
                                           int    iconIndex);

}

