/*
 * RestartDialog.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.ButtonHelper;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.IntervalField;
import com.lifepics.neuron.gui.User;

import java.util.logging.Level;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog that gives the user the option to prevent a restart.
 */

public class RestartDialog extends JDialog {

// --- constants ---

   private static final int RESULT_NONE    = 0; // internal use only
   private static final int RESULT_TIMEOUT = 1;
   private static final int RESULT_OK      = 2;
   private static final int RESULT_CANCEL  = 3;

// --- fields ---

   private int waitInterval;
   private int result;

// --- construction ---

   /**
    * @param isNewVersion A flag telling whether we're restarting because of a new version
    *                     or just because of some config change.  (If both, call it true.)
    *                     The only effect of the flag is to change the wording of the dialog.
    */
   private RestartDialog(Frame owner, int waitInterval, boolean isNewVersion) {
      super(owner,Text.get(RestartDialog.class,isNewVersion ? "s1" : "s6"),/* modal = */ true);

      this.waitInterval = waitInterval;
      result = RESULT_NONE;

      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { finish(RESULT_CANCEL); } });

      setResizable(false);

   // text

      String s = IntervalField.format(waitInterval,IntervalField.SECONDS,IntervalField.MINUTES);

      JPanel text = new JPanel();
      GridBagHelper helper = new GridBagHelper(text);

      int y = 0;

      if (isNewVersion) {
         helper.add(0,y++,new JLabel(Text.get(this,"s2")));
      } else {
         helper.add(0,y++,new JLabel(Text.get(this,"s7")));
         helper.add(0,y++,new JLabel(Text.get(this,"s8")));
      }
      helper.add(0,y++,new JLabel(Text.get(this,"s3",new Object[] { s })));

   // buttons

      JButton buttonOK = new JButton(Text.get(this,"s4"));
      buttonOK.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { finish(RESULT_OK); } });

      JButton buttonCancel = new JButton(Text.get(this,"s5"));
      buttonCancel.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { finish(RESULT_CANCEL); } });

      JPanel buttons = ButtonHelper.makeButtons(buttonOK,buttonCancel);

   // finish up

      ButtonHelper.doLayout(this,text,buttons,buttonOK);
      pack();
      setLocationRelativeTo(getOwner());
   }

// --- methods ---

   private void finish(int result) {
      if (this.result == RESULT_NONE) {
         this.result = result;
         dispose();
      }
      // else we're seeing a delayed timer event, ignore
   }

   private int run() {

      Timer timer = new Timer(waitInterval,new ActionListener() { public void actionPerformed(ActionEvent e) { finish(RESULT_TIMEOUT); } });
      timer.setRepeats(false);
      timer.start();

      setVisible(true); // does the modal thing

      timer.stop(); // may do nothing, but worth a try

      return result;
   }

// --- static interface ---

   // this is pretty ugly, but there's no nice way to let a subsystem have access to the frame.
   // see also the User class.

   private static JFrame frame;
   private static Global.Control control;

   public static void setFrame(JFrame frame, Global.Control control) { RestartDialog.frame = frame; RestartDialog.control = control; }
      // this should only be called after the frame is visible

   private static int run(int waitInterval, boolean isNewVersion) {

      if (frame == null) return RESULT_CANCEL;
      //
      // the app must have started right before the auto-update period,
      // and either received the new version quickly or had it on file.
      // anyway, tough luck, wait until next time.

      if (User.isIconified(frame)) return RESULT_OK;
      //
      // a lesson from User.tell, don't pop dialogs while minimized.
      // there we had to wait for the frame to unminimize, but here
      // we can just go ahead and restart.

      return new RestartDialog(frame,waitInterval,isNewVersion).run();
   }

// --- restart sequence ---

   // we could worry about overlapping with User pop-up windows,
   // but it's not worth it, just allow a double pop-up.
   // however, we definitely don't want two RestartDialog at once.
   // in practice we'll probably never try to open a second one,
   // but in theory there are various ways it could happen ... the hours
   // set to 3AM - 2AM, on DST change day, or the network settings
   // allowing a download to stall until just before the next update.

   // also, once we're committed to restarting, don't allow any more
   // restart dialogs, it would just be confusing.

   // note, if we can't restart, it's not the end of the world,
   // it just means we won't get the new version and/or the new
   // values of those few config settings that don't propagate.

   // activeVersion is non-null if we're in the middle of something

   private static Object lock = new Object();
   private static String activeVersion;
   private static int activeWaitInterval; // not valid unless activeVersion set
   private static boolean activeIsNewVersion; // ditto

   public static void activate(String version, int waitInterval, boolean isNewVersion) {

      synchronized (lock) {
         if (activeVersion != null) return;
         activeVersion = version;
         activeWaitInterval = waitInterval;
         activeIsNewVersion = isNewVersion;
      }
      EventQueue.invokeLater(new Runnable() { public void run() { more(); } });
   }

   private static void deactivate() {

      synchronized (lock) {
         activeVersion = null;
      }
      // no need to check, we only call this when active
   }

   private static void more() {
      String reason;

      int result = run(activeWaitInterval,activeIsNewVersion);
      if (result == RESULT_CANCEL) {

         Log.log(Level.INFO,RestartDialog.class,"i1");
         deactivate();

      } else if ((reason = Kill.canKill()) != null) {

         Log.log(Level.INFO,RestartDialog.class,"i2",new Object[] { reason });
         deactivate();

         // it seems silly to ask the user to restart, and then not do it
         // because some window is present, but I think maybe it's possible
         // there could be a stop dialog in the background, and it's gone
         // by now.  anyway, once we get here, we're in control of the UI thread,
         // so nothing is going anywhere after this.

         // if the user OK'd the restart, explain why nothing happened
         if (result == RESULT_OK) {
            Pop.info(frame,Text.get(RestartDialog.class,"e1"),Text.get(RestartDialog.class,"s9"));
            // note, we know frame isn't null because we didn't get RESULT_CANCEL
         }

      } else {

         Log.log(Level.INFO,RestartDialog.class,"i3");
         // leave active to prevent theoretical possibility of more dialogs

         control.restart(activeVersion);
         // we know control isn't null because the dialog reports cancellation
         // if the frame (and hence the control) hasn't been set.
      }
   }

}

