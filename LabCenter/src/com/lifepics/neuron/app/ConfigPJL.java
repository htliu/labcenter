/*
 * ConfigPJL.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.DirectEnum;
import com.lifepics.neuron.dendron.PJLHelper;
import com.lifepics.neuron.gui.Blob;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import java.util.LinkedList;
import java.util.ListIterator;

import java.awt.print.PageFormat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A helper class for editing settings for PJL-based integrations.
 */

public class ConfigPJL {

// --- fields ---

   private Dialog owner;
   private PJLHelper config;

   private JComboBox printer;
   private JTextField tray;
   private JCheckBox trayXerox;
   private JComboBox orientation;
   private JComboBox sides;
   private JComboBox collate;
   private JCheckBox sendPJL;
   private JCheckBox sendJOB;
   private JCheckBox sendEOJ;
   private JCheckBox sendEOL;
   private JCheckBox sendUEL;
   private JList pjl;

// --- combo boxes ---

   private static final int PSEUDO_NULL = -1;

   private static Object[] orientationNames = new Object[] { Text.get(ConfigPJL.class,"orn"),
                                                             Text.get(ConfigPJL.class,"or0"),
                                                             Text.get(ConfigPJL.class,"or1"),
                                                             Text.get(ConfigPJL.class,"or2")  };

   private static int[] orientationValues = new int[] { PSEUDO_NULL,
                                                        PageFormat.PORTRAIT,
                                                        PageFormat.LANDSCAPE,
                                                        PageFormat.REVERSE_LANDSCAPE };

   private static Object[] sidesNames = new Object[] { Text.get(ConfigPJL.class,"sin"),
                                                       Text.get(ConfigPJL.class,"si0"),
                                                       Text.get(ConfigPJL.class,"si1"),
                                                       Text.get(ConfigPJL.class,"si2")  };

   private static int[] sidesValues = new int[] { PSEUDO_NULL,
                                                  DirectEnum.SIDES_SINGLE,
                                                  DirectEnum.SIDES_DUPLEX,
                                                  DirectEnum.SIDES_TUMBLE  };

   private static Object[] collateNames = new Object[] { Text.get(ConfigPJL.class,"con"),
                                                         Text.get(ConfigPJL.class,"co0"),
                                                         Text.get(ConfigPJL.class,"co1")  };

   private static int[] collateValues = new int[] { PSEUDO_NULL,
                                                    DirectEnum.COLLATE_NO,
                                                    DirectEnum.COLLATE_YES  };

// --- construction ---

   public ConfigPJL(Dialog owner, PJLHelper config) {
      this.owner = owner;
      this.config = config;

   // fields

      printer = ConfigSetup.constructPrinterCombo();
      tray = new JTextField(Text.getInt(this,"w1"));
      trayXerox = new JCheckBox(Text.get(this,"s3"));
      orientation = new JComboBox(orientationNames);
      sides = new JComboBox(sidesNames);
      collate = new JComboBox(collateNames);
      sendPJL = new JCheckBox(Text.get(this,"s9"));
      sendJOB = new JCheckBox(Text.get(this,"s10"));
      sendEOJ = new JCheckBox(Text.get(this,"s11"));
      sendEOL = new JCheckBox(Text.get(this,"s12"));
      sendUEL = new JCheckBox(Text.get(this,"s13"));
      pjl = new JList();
      pjl.setVisibleRowCount(Text.getInt(this,"n1"));
      pjl.setPrototypeCellValue(" "); // otherwise empty string displays with no height

      // Q:  why not use something besides JList and allow editing in place?
      // A1: a text area would feel too cramped
      // A2: I just didn't want to deal with setting up a grid, and also a grid
      //     would require distributing a stopEditing call to every queue panel.
      //     also reordering lines in a grid is a nuisance!
   }

   public int doLayoutS(GridBagHelper helper, int y) {

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d1")));
      y++;

      return y;
   }

   public int doLayout1(GridBagHelper helper, int y, boolean showOrientation) {

   // panels

      JPanel panelTray = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      panelTray.add(tray);
      panelTray.add(Box.createHorizontalStrut(Text.getInt(this,"d2")));
      panelTray.add(trayXerox);

      JPanel panelSend = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      panelSend.add(sendPJL);
      panelSend.add(sendJOB);
      panelSend.add(sendEOJ);
      panelSend.add(sendEOL);
      panelSend.add(sendUEL);

   // layout

      helper.add(0,y,new JLabel(Text.get(this,"s1") + ' '));
      helper.add(1,y,printer);
      y++;

      y = doLayoutS(helper,y);

      helper.add(0,y,new JLabel(Text.get(this,"s2") + ' '));
      helper.add(1,y,panelTray);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s4") + ' '));
      helper.add(1,y,sides);
      y++;

      if (showOrientation) {
         helper.add(0,y,new JLabel(Text.get(this,"s7") + ' '));
         helper.add(1,y,orientation);
         y++;
      }

      helper.add(0,y,new JLabel(Text.get(this,"s5") + ' '));
      helper.add(1,y,collate);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s8") + ' '));
      helper.add(1,y,panelSend);
      y++;

      return y;
   }

   public int doLayout2(GridBagHelper helper, int y) {

   // panels

      JScrollPane scroll = new JScrollPane(pjl,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                               JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      scroll.setBorder(BorderFactory.createLoweredBevelBorder());

      JButton buttonEdit   = new JButton(Text.get(this,"s14"));
      JButton buttonEditHP = new JButton(Text.get(this,"s15"));

      buttonEdit  .addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doEdit();   } });
      buttonEditHP.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doEditHP(); } });

      JPanel panelPJL = new JPanel();
      GridBagHelper h = new GridBagHelper(panelPJL);

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridheight = 4;
      h.add(0,0,scroll,constraints);

      h.add(1,0,Box.createHorizontalStrut(Text.getInt(this,"d3")));
      h.addFill(2,0,buttonEdit);
      h.add(2,1,Box.createVerticalStrut(Text.getInt(this,"d4")));
      h.addFill(2,2,buttonEditHP);

      h.setColumnWeight(0,1);

   // layout

      helper.addSpan(0,y,2,new JLabel(Text.get(this,"s6")));
      y++;
      helper.addSpanFill(0,y,2,panelPJL);
      y++;

      return y;
   }

// --- helpers ---

   private static int pne(Integer i) { return (i == null) ? PSEUDO_NULL : i.intValue();   }
   private static Integer gne(int i) { return (i == PSEUDO_NULL) ? null : new Integer(i); }
   // short for "put nullable enum" and "get nullable enum"

// --- semi-implementation of ConfigFormat ---

   public void put() {

      ConfigSetup.putPrinterCombo(printer,config.printer);
      Field.putNullable(tray,config.tray);
      Field.put(trayXerox,config.trayXerox);
      Field.put(orientation,orientationValues,pne(config.orientation));
      Field.put(sides,sidesValues,pne(config.sides));
      Field.put(collate,collateValues,pne(config.collate));
      Field.put(sendPJL,config.sendPJL);
      Field.put(sendJOB,config.sendJOB);
      Field.put(sendEOJ,config.sendEOJ);
      Field.put(sendEOL,config.sendEOL);
      Field.put(sendUEL,config.sendUEL);
      Field.put(pjl,config.pjl);
   }

   public void get() throws ValidationException {

      config.printer = ConfigSetup.getPrinterCombo(printer);
      config.tray = Field.getNullable(tray);
      config.trayXerox = Field.get(trayXerox);
      config.orientation = gne(Field.get(orientation,orientationValues));
      config.sides = gne(Field.get(sides,sidesValues));
      config.collate = gne(Field.get(collate,collateValues));
      config.sendPJL = Field.get(sendPJL);
      config.sendJOB = Field.get(sendJOB);
      config.sendEOJ = Field.get(sendEOJ);
      config.sendEOL = Field.get(sendEOL);
      config.sendUEL = Field.get(sendUEL);
      config.pjl = Field.get(pjl);
   }

   public JComboBox accessPrinterCombo() { return printer; }

// --- dialog helpers ---

   private void doEdit() {
      DialogEdit dialog = new DialogEdit(owner,Field.get(pjl));
      if (dialog.run()) {
         Field.put(pjl,dialog.getResult());
      }
   }

   private void doEditHP() {
      DialogEditHP dialog = new DialogEditHP(owner,Field.get(pjl));
      if (dialog.run()) {
         Field.put(pjl,dialog.getResult());
      }
   }

// --- edit ---

   private static class DialogEdit extends EditDialog {

   // --- fields ---

      // list used for both input and output
      private LinkedList list;
      public LinkedList getResult() { return list; }

      private JTextArea text;

   // --- construction ---

      public DialogEdit(Dialog owner, LinkedList list) {
         super(owner,Text.get(ConfigPJL.class,"s16"));
         this.list = list;
         construct(constructFields(),/* readonly = */ false);
      }

   // --- methods ---

      private JPanel constructFields() {

         text = Blob.makeEditBlob(Text.getInt(ConfigPJL.class,"h2"),Text.getInt(ConfigPJL.class,"w2"));

         JScrollPane scroll = new JScrollPane(text,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                   JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
         scroll.setBorder(BorderFactory.createLoweredBevelBorder());

         // similar to end of Blob.makeBlob
         // usually the gaps aren't an issue because we use GridBagLayout instead
         JPanel fields = new JPanel();
         fields.setLayout(new FlowLayout(FlowLayout.LEFT,0,0)); // remove default hgap and vgap
         fields.add(scroll);

         return fields;
      }

      protected void put() {
         Field.put(text,list);
      }

      protected void getAndValidate() throws ValidationException {
         list = Field.get(text);
      }
   }

// --- edit HP ---

   private static class HPOption {

      public String key;
      public String var;
      public String[] values;

      public HPOption(String key, String var, String[] values) {
         this.key = key;
         this.var = var;
         this.values = values;
      }
   }

   private static class Pair {

      public String var;
      public String val;

      public Pair(String var, String val) {
         this.var = var;
         this.val = val;
      }
   }

   private static HPOption[] options = new HPOption[] {
         new HPOption("o1","CUTTER",new String[] { "","ON","OFF" }),
         null,
         new HPOption("o2","PRINTQUALITY",new String[] { "","DRAFT","NORMAL","HIGH" }),
         new HPOption("o3","MAXDETAIL",new String[] { "","ON","OFF" }),
         new HPOption("o4","EXTRAPASSES",new String[] { "","ON","OFF" }),
         null,
         new HPOption("o5","MEDIASOURCE",new String[] { "","MANUALFEED","ROLL1" }),
         new HPOption("o6","RESOLUTION",new String[] { "","300","600","1200" }),
         new HPOption("o7","GLOSSENHANCER",new String[] { "","OFF","INKEDAREA","FULLPAGE" })
      };

   private static class DialogEditHP extends EditDialog {

   // --- fields ---

      // here the list is modified in place.  why is this OK?
      // (1) the caller always gives us a new list object
      // (2) getAndValidate never fails and so is only called once.
      private LinkedList list;
      public LinkedList getResult() { return list; }

      private JComboBox[] combo;
      private Integer[] index;

   // --- construction ---

      public DialogEditHP(Dialog owner, LinkedList list) {
         super(owner,Text.get(ConfigPJL.class,"s17"));
         this.list = list;
         construct(constructFields(),/* readonly = */ false);
      }

   // --- methods ---

      private JPanel constructFields() {

         combo = new JComboBox[options.length];
         index = new Integer[options.length];
         for (int i=0; i<options.length; i++) {
            if (options[i] != null) {
               combo[i] = new JComboBox(options[i].values);
               combo[i].setEditable(true);
            }
         }

         int d5 = Text.getInt(ConfigPJL.class,"d5");

         JPanel fields = new JPanel();
         GridBagHelper helper = new GridBagHelper(fields);

         for (int i=0; i<options.length; i++) {
            if (options[i] != null) {
               helper.add(0,i,new JLabel(Text.get(ConfigPJL.class,options[i].key) + ' '));
               helper.add(1,i,combo[i]);
            } else {
               helper.add(0,i,Box.createVerticalStrut(d5));
            }
         }

         return fields;
      }

      protected void put() {
         ListIterator li = list.listIterator(); // not just Iterator because we need the index number
         while (li.hasNext()) {
            Pair p = parse((String) li.next());
            if (p != null) {
               int k = findOption(p.var);
               if (k != -1) {
                  combo[k].setSelectedItem(p.val);
                  index[k] = new Integer(li.previousIndex());
               }
            }
         }
      }

      protected void getAndValidate() throws ValidationException {

         // make two passes to keep the index values from changing
         Object deleted = new Object();

         for (int i=0; i<options.length; i++) {
            if (options[i] != null) {
               String s = ((String) combo[i].getSelectedItem()).trim();
               if (s.length() != 0) {
                  s = "SET " + options[i].var + "=" + s;
                  if (index[i] != null) { // update
                     list.set(index[i].intValue(),s);
                  } else {                // create
                     list.add(s);
                  }
               } else {
                  if (index[i] != null) { // delete
                     list.set(index[i].intValue(),deleted);
                  }
                  // else it stays null
               }
            }
         }

         ListIterator li = list.listIterator();
         while (li.hasNext()) {
            if (li.next() == deleted) li.remove();
         }
      }
   }

   private static Pair parse(String s) { // this relies on arg having been trimmed
      String u = s.toUpperCase();
      if ( ! u.startsWith("SET ") ) return null; // at least one space is required

      int eq = uniqueIndexOf(s,'=');
      if (eq == -1) return null;

      int i0 = skipForward(s,4);
      int i1 = skipBackward(s,eq);
      if (i1 < i0) return null;

      int i2 = skipForward(s,eq+1);
      int i3 = s.length(); // no skip because arg was trimmed

      String var = u.substring(i0,i1);
      String val = s.substring(i2,i3); // don't force uppercase

      // to be proper PJL, var and val shouldn't contain spaces,
      // but if var contains spaces it won't match, and if val
      // contains spaces (or is empty), I still want to edit it.

      return new Pair(var,val);
   }

   private static int uniqueIndexOf(String s, char c) {
      int i = s.indexOf(c);
      if (i == -1) return -1;
      int j = s.indexOf(c,i+1);
      if (j != -1) return -1;
      return i;
   }

   private static int skipForward(String s, int i) {
      int n = s.length();
      while (i < n && s.charAt(i) == ' ') i++;
      return i;
   }

   private static int skipBackward(String s, int i) {
      while (i > 0 && s.charAt(i-1) == ' ') i--;
      return i;
   }

   private static int findOption(String var) {
      for (int i=0; i<options.length; i++) {
         if (options[i] != null) {
            if (options[i].var.equals(var)) return i;
         }
      }
      return -1;
   }

}

