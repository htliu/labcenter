/*
 * ScrollUtil.java
 */

package com.lifepics.neuron.gui;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

/**
 * A little class for setting the preferred size of a JScrollPane.
 */

public class ScrollUtil {

   // don't call these until after you've set the minimum size!

   public static void setSizeToMedium(Window window) { setSize(window,0.8); }
   public static void setSizeToLarge (Window window) { setSize(window,0.9); }
   //
   // I think the theory behind the 0.8 was just that I found it more pleasing

   private static void setSize(Window window, double fraction) {

      Dimension limit = Toolkit.getDefaultToolkit().getScreenSize();

      limit.width  *= fraction;
      limit.height *= fraction;

      Dimension size = window.getSize();

      if (size.width  < limit.width ) size.width  = limit.width;
      if (size.height < limit.height) size.height = limit.height;

      window.setSize(size);
   }

   public static void limitSizeToMedium(Window window) { limitSize(window,0.8); }
   public static void limitSizeToLarge (Window window) { limitSize(window,0.9); }

   private static void limitSize(Window window, double fraction) {

      Dimension limit = Toolkit.getDefaultToolkit().getScreenSize();

      limit.width  *= fraction;
      limit.height *= fraction;

      Dimension size = window.getSize();

      if (size.width  > limit.width ) size.width  = limit.width;
      if (size.height > limit.height) size.height = limit.height;

      window.setSize(size);
   }

   public static double getWidth(double fraction) {
      Dimension limit = Toolkit.getDefaultToolkit().getScreenSize();
      return limit.width * fraction;
   }

   // this used to be called setPreferredSize, and indeed it does set the preferred size
   // of the scroll pane .. but the real goal is to set the size of the *window* so that
   // it displays the viewed object at its preferred size, if possible.

   public static void setSizeToPreferred(JScrollPane scroll, Window window) {

   // gather information

      Dimension limit = Toolkit.getDefaultToolkit().getScreenSize();

      limit.width  *= 0.9;
      limit.height *= 0.9;
      //
      // don't use the whole screen ... besides looking bad,
      // it makes part of the window hide behind the taskbar.

   // size to show all ...

      // this assumes that there are no corners, headers, or viewport border.
      // there may be scroll bars, but that will be determined by what we do.

      Dimension size = scroll.getViewport().getView().getPreferredSize();

      if (scroll.getBorder() != null) { // this is true
         Insets insets = scroll.getBorder().getBorderInsets(scroll);

         size.width  += insets.left + insets.right;
         size.height += insets.top  + insets.bottom;
      }

      // so, now we've got the size of the scroll pane itself,
      // and we're going to make it so that plus the widgets
      // fits within the limit size.  that's an iterative process,
      // though, and we can reduce the number of iterations
      // by applying the limit to the size of the scroll pane,
      // not yet including widgets.

      Adjuster a = new Adjuster();
      a.adjust(size,size.width-limit.width,size.height-limit.height);

   // ... but limit to fit on screen, including widgets

      // the whole excess may not be visible the first time around,
      // because pack sometimes decides to limit the window
      // to be roughly the screen size.  that's why we have to iterate.

      while (true) {

      // set the size, see if everything's OK

         scroll.setPreferredSize(size);
         window.pack();

         Dimension windowSize = window.getSize();

         int excessWidth  = windowSize.width  - limit.width;
         int excessHeight = windowSize.height - limit.height;

         if (    (excessWidth  <= 0 || size.width  == 0)
              && (excessHeight <= 0 || size.height == 0) ) break;

      // adjust the size

         a.adjust(size,excessWidth,excessHeight);
      }

      // caller will also call setLocationRelativeTo, so this is a good place
      // to mention a surprising thing about how that function behaves.
      // if the default placement would make the window extend off the bottom
      // of the screen, not only is it scooted up as far as possible,
      // it's also scooted to the left or right side of the target window.
   }

   private static class Adjuster {

      private int verticalWidth;
      private int horizontalHeight;
      private boolean hasVertical;
      private boolean hasHorizontal;

      public Adjuster() {
         verticalWidth    = new JScrollBar(JScrollBar.VERTICAL  ).getPreferredSize().width;
         horizontalHeight = new JScrollBar(JScrollBar.HORIZONTAL).getPreferredSize().height;
         hasVertical   = false;
         hasHorizontal = false;
      }

      public void adjust(Dimension size, int excessWidth, int excessHeight) {

         // basically, the plan is, the first time the width or height is adjusted,
         // add the height or width of the scroll bar to the other quantity.
         // no attempt is made to correct for new excess that's created by this;
         // if there is any, it will get taken care of on the next go-round.

         if (excessWidth > 0) {
            size.width -= excessWidth;
            if ( ! hasHorizontal ) {
               size.height += horizontalHeight;
               hasHorizontal = true;
            }
         }

         if (excessHeight > 0) {
            size.height -= excessHeight;
            if ( ! hasVertical ) {
               size.width += verticalWidth;
               hasVertical = true;
            }
         }

         // handle absurd case where widgets don't fit on screen
         if (size.width  < 0) size.width  = 0;
         if (size.height < 0) size.height = 0;
      }
   }

}

