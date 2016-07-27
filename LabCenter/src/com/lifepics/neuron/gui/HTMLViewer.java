/*
 * HTMLViewer.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.thread.StopDialog;

import java.net.URL;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A minimal dialog for viewing HTML data.
 */

public class HTMLViewer extends JDialog {

// --- fields ---

   private int mode;

   private JEditorPane editorPane;
   private JTextField fieldCopies;

// --- construction ---

   /**
    * A pseudo-constructor to deal with the annoying fact that
    * the superclass constructor distinguishes between Frame and Dialog.
    */
   public static HTMLViewer create(Window owner, String title, URL url, int mode) {
      return (owner instanceof Frame) ? new HTMLViewer((Frame ) owner,title,url,mode)
                                      : new HTMLViewer((Dialog) owner,title,url,mode);
   }

   /**
    * @param url The URL that should be displayed initially.
    *            The function File.toURL is often useful here.
    */
   public HTMLViewer(Frame owner, String title, URL url, int mode) {
      super(owner,title,/* modal = */ true);
      construct(url);
      this.mode = mode;
   }

   public HTMLViewer(Dialog owner, String title, URL url, int mode) {
      super(owner,title,/* modal = */ true);
      construct(url);
      this.mode = mode;
   }

   private void construct(URL url) {

      setDefaultCloseOperation(DISPOSE_ON_CLOSE);

   // build scroll pane

      editorPane = new HTMLEditorPane(url);

      // if you put an editor pane directly into a scroll pane, as is natural,
      // there's some communication between them regarding the viewport size ...
      // the pane doesn't adjust its layout to fit the width, like a browser,
      // but it does respond slightly, just enough to mess up the print output.
      //
      // I tried overriding getScrollableTracksViewportWidth and ___Height,
      // but that didn't stop the communication (although it should have) ...
      // and if you look at the original implementations, you can see that
      // there's covert communication, being aware of the parent object class.
      // so, you'd have to read all the code to really stop the communication.
      //
      // so, instead, insert a JPanel layer between them, that disrupts it.
      //
      JPanel extraPanel = new JPanel();
      extraPanel.setLayout(new BorderLayout()); // default layout has gaps
      extraPanel.add(editorPane);

      JScrollPane scroll = new JScrollPane(extraPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                      JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

   // build buttons

      fieldCopies = new JTextField(Text.getInt(this,"w1"));
      fieldCopies.setMaximumSize(fieldCopies.getPreferredSize()); // otherwise this expands before glue
      Field.put(fieldCopies,Convert.fromInt(Print.getConfig().copies));

      JPanel buttons = new ButtonHelper()
         .add(new JLabel(Text.get(this,"s1") + ' '))
         .add(fieldCopies)
         .addStrut()
         .addButton(Text.get(this,"s2"),new ActionListener() { public void actionPerformed(ActionEvent e) { doPrint(); } })
         .addGlue()
         .addButton(Text.get(this,"s3"),new ActionListener() { public void actionPerformed(ActionEvent e) { dispose(); } })
         .end();

   // finish up

      ButtonHelper.doLayout(this,scroll,buttons,null,/* fieldsBorder = */ false);
      ScrollUtil.setSizeToPreferred(scroll,this);
      setLocationRelativeTo(getOwner());
   }

// --- methods ---

   /**
    * Run the dialog.
    */
   public void run() {
      setVisible(true); // does the modal thing
   }

   private void doPrint() {
      try {
         int copies = Convert.toInt(Field.get(fieldCopies));
         PrintConfig.validateCopies(copies);

         // Print.print(editorPane,copies,null,getTitle(),mode);
         // that's the idea, but as usual we add a dialog
         //
         PrintThread printThread = new PrintThread(copies);
         printThread.start();
         StopDialog.joinSafe(printThread,this);
         printThread.rethrow();

      } catch (Exception e) {
         Pop.error(getOwner(),e,Text.get(this,"s4"));
      }
   }

// --- print thread ---

   private class PrintThread extends Thread {

      private int copies;
      private Exception exception;

      public PrintThread(int copies) {
         super(Text.get(HTMLViewer.class,"s5"));
         this.copies = copies;
         // exception starts out null
      }

      public void run() {
         try {
            Print.print(editorPane,copies,null,getTitle(),mode);
            // we never use a label footer here, since we have no idea
            // whether these are reprints or additional labels or what
         } catch (Exception e) {
            exception = e;
         }
      }

      /**
       * Transfer exceptions back to main thread.
       */
      public void rethrow() throws Exception {
         if (exception != null) throw exception;
      }
   }

}

