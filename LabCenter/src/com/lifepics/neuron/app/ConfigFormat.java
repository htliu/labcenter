/*
 * ConfigFormat.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.misc.ExtensionFileFilter;

import java.io.File;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel superclass for editing format settings.
 */

public abstract class ConfigFormat extends JPanel {

   private Dialog owner;
   protected ConfigFormat(Dialog owner) { this.owner = owner; }

   protected Dialog getOwner() { return owner; }

// --- interface ---

   public abstract Object getFormatConfig();

   public abstract void put();
   public abstract void get() throws ValidationException;

   public JComboBox accessPrinterCombo() { return null; }

// --- data dir helpers ---

   public static JTextField constructDataDir() {
      return new JTextField(Text.getInt(ConfigFormat.class,"w4"));
   }

   public int addDataDir(GridBagHelper helper, int y, String label, JTextField dataDir) {
      return addDataDir(owner,helper,y,label,dataDir,/* pack = */ false);
   }
   public static int addDataDir(Dialog owner, GridBagHelper helper, int y, String label, JTextField dataDir) {
      return addDataDir(owner,helper,y,label,dataDir,/* pack = */ false);
   }
   public static int addDataDir(Dialog owner, GridBagHelper helper, int y, String label, JTextField dataDir, boolean pack) {

      // there are three formats now: no label, label above, label packed on same line.
      // I remove the fourth potential format by declaring that if label is null, pack
      // will be ignored.

      if (label != null) {
         if (pack) {
            helper.add(0,y,new JLabel(label + ' '));
         } else {
            helper.addSpan(0,y,4,new JLabel(label));
            y++;
         }
      }

      if (label == null || ! pack) addSpacer(helper,y);
      // this performs two functions:  adding a spacer, yes,
      // and also making sure there's something in column 0.

      helper.add(1,y,dataDir);
      helper.add(2,y,Box.createHorizontalStrut(Text.getInt(ConfigFormat.class,"d1")));
      helper.add(3,y,makePicker(dataDir,owner,null));
      y++;

      return y;
   }

   /**
    * A function to create a standard spacer.  We used to accomplish this layout task
    * by calling setColumnWeight(0,1), but now the notification settings are too wide.
    */
   public static void addSpacer(GridBagHelper helper, int y) {
      helper.add(0,y,Box.createHorizontalStrut(Text.getInt(ConfigFormat.class,"d2")));
   }

   public static JButton makePicker(JTextField fileField, Dialog owner, FileFilter fileFilter) {
      JButton button = new JButton(Text.get(ConfigFormat.class,"s12"));
      button.addActionListener(new Picker(fileField,owner,fileFilter));
      return button;
   }

   private static class Picker implements ActionListener {

      private JTextField fileField;
      private Dialog owner;
      private FileFilter fileFilter;
      public Picker(JTextField fileField, Dialog owner, FileFilter fileFilter) { this.fileField = fileField; this.owner = owner; this.fileFilter = fileFilter; }

      public void actionPerformed(ActionEvent e) {
         JFileChooser chooser = new JFileChooser();

         String key;
         if (fileFilter == null) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            key = "s25a";
         } else {
            ExtensionFileFilter.clearFilters(chooser);
            chooser.addChoosableFileFilter(fileFilter);
            key = "s25b";
         }

         try {
            String s = Field.get(fileField);
            if (s.length() == 0) s = Convert.fromFile(new File(""));
            chooser.setSelectedFile(Convert.toFile(s));

            // this is kind of roundabout.  the problem is, Convert.toFile throws an exception
            // when the string length is zero, i.e., when the field hasn't been filled in yet.
            // so, a natural thing to do would be to use a file object as a default on failure,
            // but the problem there is, the chooser ignores setSelectedFile(new File("")),
            // and goes to C:\Windows\Desktop, which is nasty.  the fromFile-toFile conversion
            // produces a different result, the current process directory, which is not the same
            // as the main directory that paths are made relative to at startup, but it's close.

         } catch (ValidationException e2) {
            // ignore and use nasty chooser default
         }

         int result = chooser.showDialog(owner,Text.get(ConfigFormat.class,key));

         if (result == JFileChooser.APPROVE_OPTION) {
            Field.put(fileField,Convert.fromFile(chooser.getSelectedFile()));
         }
      }
   }

// --- length limit helpers ---

   public static JCheckBox constructLimitEnable() {
      return new JCheckBox(Text.get(ConfigFormat.class,"s26") + ' ');
   }

   public static JTextField constructLimitLength() {
      return new JTextField(Text.getInt(ConfigFormat.class,"w5"));
   }

   public int addLengthLimit(GridBagHelper a_helper, int y, JCheckBox limitEnable, JTextField limitLength) {

      JPanel panel = new JPanel();
      GridBagHelper helper = new GridBagHelper(panel);

      helper.add(0,0,limitEnable);
      helper.add(1,0,limitLength);
      helper.add(2,0,new JLabel(' ' + Text.get(ConfigFormat.class,"s27")));

      a_helper.addSpan(1,y,3,panel);
      y++;

      return y;
   }

// --- combo box helpers ---

   protected static class LabeledNumber { // similar to LabeledInteger in EditSKUDialog

      public int number;
      public String label;

      public LabeledNumber(int number, String name) {
         this.number = number;
         label = Text.get(ConfigFormat.class,"s28",new Object[] { Convert.fromInt(number), name });
      }

      // equality works by object identity

      public String toString() { return label; }
   }

   // here's how the conversion works
   //
   //    Integer        Object
   //    ------------   ------------
   //    null           ""
   //        in set     LabeledNumber
   //    not in set     string

   protected static Object fromNumber(Object[] set, Integer number) {

      if (number == null) return "";

      int value = number.intValue();

      for (int i=0; i<set.length; i++) {
         Object o = set[i];
         if (    o instanceof LabeledNumber
              && ((LabeledNumber) o).number == value ) return o;
      }

      return Convert.fromInt(value);
   }

   protected static Integer toNumber(Object o) throws ValidationException {

      if (o instanceof LabeledNumber) return new Integer( ((LabeledNumber) o).number );

      String s = (String) o;
      return (s.equals("")) ? null : new Integer(Convert.toInt(s));
   }

// --- more combo box helpers ---

   protected static final int NULLABLE_BOOLEAN_DEFAULT = 0;
   protected static final int NULLABLE_BOOLEAN_NO      = 1;
   protected static final int NULLABLE_BOOLEAN_YES     = 2;

   protected static int nbToInt(Boolean b) {
      return (b == null) ? NULLABLE_BOOLEAN_DEFAULT : (b.booleanValue() ? NULLABLE_BOOLEAN_YES : NULLABLE_BOOLEAN_NO);
   }
   protected static Boolean intToNb(int i) {
      return (i == NULLABLE_BOOLEAN_DEFAULT) ? null : new Boolean(i == NULLABLE_BOOLEAN_YES);
   }

   protected static Object[] nullableBooleanNames = new Object[] { Text.get(ConfigFormat.class,"nb0"),
                                                                   Text.get(ConfigFormat.class,"nb1"),
                                                                   Text.get(ConfigFormat.class,"nb2")  };
   protected static int[] nullableBooleanValues = new int[] { NULLABLE_BOOLEAN_DEFAULT,
                                                              NULLABLE_BOOLEAN_NO,
                                                              NULLABLE_BOOLEAN_YES };

}

