/*
 * Browser.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;

import java.lang.reflect.Method;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A utility class for launching an external browser.
 * The code is adapted from http://www.centerkey.com/java/browser/,
 * to remove guessing on Linux and add an error handling dialog.
 */

public class Browser {

   public static void launch(Window owner, String url, String description) {
      boolean failed = false;
      try {
         launchBrowser(url);
      } catch (Exception e) {
         failed = true;
      }
      launchDialog(owner,url,description,failed);
   }

   private static void launchBrowser(String url) throws Exception {
      String os = System.getProperty("os.name");

      if (os.startsWith("Windows")) {
         Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);

      } else if (os.startsWith("Mac OS")) {
         Class fileManager = Class.forName("com.apple.eio.FileManager");
         Method openURL = fileManager.getDeclaredMethod("openURL",new Class[] { String.class });
         openURL.invoke(null,new Object[] { url });

      } else {
         throw new Exception(); // switch to dialog
      }
   }

   private static JDialog constructDialog(Window owner, String title, boolean modal) {
      if (owner instanceof Frame ) return new JDialog((Frame ) owner,title,modal);
      if (owner instanceof Dialog) return new JDialog((Dialog) owner,title,modal);
      throw new IllegalArgumentException();
   }

   private static void launchDialog(Window owner, String url, String description, boolean failed) {

   // dialog

      final JDialog dialog = constructDialog(owner,Text.get(Browser.class,"s1"),/* modal = */ true);

      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.setResizable(false);

   // constants

      int d1 = Text.getInt(Browser.class,"d1");
      int d3 = Text.getInt(Browser.class,"d3");
      int d4 = Text.getInt(Browser.class,"d4");

   // controls

      JTextField field = new JTextField(Text.getInt(Browser.class,"w1"));
      Field.put(field,url);
      // if you call setEnabled(false), there's no copy and paste

      JButton button = new JButton(Text.get(Browser.class,"s2"));
      button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { dialog.dispose(); } });
      dialog.getRootPane().setDefaultButton(button);

   // panel

      JPanel panel = new JPanel();
      panel.setBorder(BorderFactory.createEmptyBorder(d1,d1,d1,d1));

      GridBagHelper helper = new GridBagHelper(panel);

      String key = (failed ? "s4" : "s3");
      int y = 0;

      helper.add(0,y++,new JLabel(Text.get(Browser.class,key + "a",new Object[] { description })));
      helper.add(0,y++,new JLabel(Text.get(Browser.class,key + "b")));
      helper.add(0,y++,Box.createVerticalStrut(d3));
      helper.addCenter(0,y++,field);
      helper.add(0,y++,Box.createVerticalStrut(d4));
      helper.addCenter(0,y++,button);

   // go

      dialog.getContentPane().add(panel);
      dialog.pack();
      dialog.setLocationRelativeTo(owner);
      dialog.setVisible(true);
   }

}

