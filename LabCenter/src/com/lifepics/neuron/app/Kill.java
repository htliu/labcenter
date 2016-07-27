/*
 * Kill.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.axon.RollDialogPro;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.thread.StopDialog;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A utility class for closing windows programmatically.
 * It has to be up here in app package because it knows
 * about several specific window classes.
 */

public class Kill {

// --- kill function ---

   public static void kill() {

      Frame[] f = Frame.getFrames();
      for (int i=0; i<f.length; i++) {

         try {
            if (f[i].isDisplayable()) f[i].dispose();
         } catch (Throwable t) {
            // the invisible default parent frame produced by JOptionPane
            // has a dispose override that can throw security exceptions,
            // don't let that be a problem.  I shouldn't be creating that frame,
            // since I don't use a null owner, but we want to be careful here.
         }
      }
   }

// --- can-kill function ---

   /**
    * @return A string reason why not to kill, or null meaning go ahead.
    */
   public static String canKill() {

      Frame[] f = Frame.getFrames();
      for (int i=0; i<f.length; i++) {

         try {
            checkRecursive(f[i]);
         } catch (NoKillException e) {
            return e.getMessage();
         }
      }

      return null;
   }

// --- helper class ---

   private static class NoKillException extends Exception {
      public NoKillException(String message) { super(message); }
   }

// --- recursion ---

   private static void checkRecursive(Window parent) throws NoKillException {

      // I doubt nondisplayable windows can have displayable children,
      // but it's easy enough to scan the whole tree.

      checkSingle(parent);

      Window[] w = parent.getOwnedWindows();
      for (int i=0; i<w.length; i++) {

         checkRecursive(w[i]);
      }
   }

   private static void checkSingle(Window w) throws NoKillException {
      if ( ! w.isDisplayable() ) return;

      Class c = w.getClass();

      if (c == StopDialog.class) throw new NoKillException(Text.get(Kill.class,"c1"));
      // the problem with stop dialogs is, they show that there's an untracked thread
      // doing something, and we have no way to stop it.  also they use timers, which
      // would produce future events.

      if (c == RestartDialog.class) throw new NoKillException(Text.get(Kill.class,"c2"));
      // restart dialog has a timer that will produce future action.  there shouldn't
      // be any of these when we're restarting, since I was careful to prevent having
      // more than one, and to prevent one after a restart is accepted, but you never know ...

      if (c == RollDialogPro.class) throw new NoKillException(Text.get(Kill.class,"c3"));
      // the problem here is the ThumbnailCache, which has tricky interaction between
      // timers and asynchronous image loaders.  maybe we could stop it, but it's not
      // worth the trouble.

      // RollDialog and OrderDialog (but not OrderStubDialog) have similar issues with
      // the asynchronous Thumbnail image loader, but there it's only one image, quick,
      // whereas a ThumbnailCache could be running for quite a while.

      // the User class has a one-shot timer that interacts with the pop-up windows,
      // but that doesn't matter because it will be locked out of doing anything by
      // the special code in User.
   }

}

