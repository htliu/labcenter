/*
 * ResolveDialog.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.DisableTextField;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.InstallHint;
import com.lifepics.neuron.meta.Selector;
import com.lifepics.neuron.meta.StringSelector;

import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterJob;
import javax.print.PrintService;

/**
 * A dialog for resolving printer names, plus some utility functions.
 */

public class ResolveDialog extends EditDialog {

   // the process is:
   // * caller accumulates records
   // * if there are no records, caller should stop and not show the dialog
   // * caller calls match function
   // * the actual dialog is only used to edit the match fields (and display information)
   // * caller calls apply function

   // naming conventions:
   // * "printer" for printer name value
   // * "combo" for normal printer combo
   // * "match" for matching value
   // * "alt" for alt printer combo used to edit matching values

// --- records ---

   private static final int STATE_NO_MATCH = 0;
   private static final int STATE_MATCH    = 1;
   private static final int STATE_MULTIPLE = 2;
   private static final int STATE_RESOLVED = 3;

   private static String[] stateString = new String[] { Text.get(ResolveDialog.class,"s4"),
                                                        Text.get(ResolveDialog.class,"s5"),
                                                        Text.get(ResolveDialog.class,"s6"),
                                                        Text.get(ResolveDialog.class,"s8")  };

   private static class Record {

      public String printer; // not null
      public LinkedList combos;
      public int state;
      public String match;
      public JComboBox alt;

      public Record(String printer) {
         this.printer = printer;
         combos = new LinkedList();
         // printer and combos are set by accumulate
         // state and match are set by match
         // alt is constructed by the actual dialog
      }
   }

   private static Record findOrCreate(LinkedList records, String printer) {
      Iterator i = records.iterator();
      while (i.hasNext()) {
         Record r = (Record) i.next();
         if (r.printer.equals(printer)) return r;
      }
      Record r = new Record(printer);
      records.add(r);
      return r;
   }

   public static void accumulate(LinkedList records, JComboBox combo) {
      String printer = ConfigSetup.getPrinterCombo(combo);
      if (printer == null) return;
      Record r = findOrCreate(records,printer);
      r.combos.add(combo);
   }

   public static void match(LinkedList records, LinkedList installHints) {
      PrintService[] service = PrinterJob.lookupPrintServices();
      Iterator i = records.iterator();
      while (i.hasNext()) {
         Record r = (Record) i.next();
         match(r,service,installHints);
      }
   }

   private static void match(Record r, PrintService[] service, LinkedList installHints) {

      // compute the state and match fields

   // get selector

      boolean isPattern = r.printer.startsWith(InstallHint.PREFIX);

      Selector selector = null;
      if (isPattern) {
         String pattern = InstallHint.getPattern(installHints,r.printer);
         if (pattern != null) {
            try {
               selector = StringSelector.construct(pattern.toLowerCase()); // case-insensitive
            } catch (Exception e) {
               pattern = null; // impossible by validation, but handle it
            }
         }
         if (pattern == null) {
            r.state = STATE_NO_MATCH;
            r.match = null;
            return;
         }
      } else {
         selector = new StringSelector.Equals(r.printer.toLowerCase()); // case-insensitive
      }

   // scan for matches

      LinkedList matches = new LinkedList();
      for (int i=0; i<service.length; i++) {
         String name = service[i].getName();
         if (selector.select(name.toLowerCase())) matches.add(name);
      }

   // fill in results

      switch (matches.size()) {
      case 0:
         r.state = STATE_NO_MATCH;
         r.match = null;
         break;
      case 1:
         if (isPattern) { // this is the point of the whole exercise
            r.state = STATE_RESOLVED;
            r.match = (String) matches.getFirst(); // only case where r.match is set
         } else {
            r.state = STATE_MATCH;
            r.match = null;
         }
         break;
      default:
         if (isPattern || ! matches.contains(r.printer)) {
            r.state = STATE_MULTIPLE;
            r.match = null;
         } else {
            r.state = STATE_MATCH;
            r.match = null;
            // the way Print.getPrintService works is, if there's an exact match,
            // use that, otherwise use the first case-insensitive match.  so, if
            // we have a case-sensitive match, it's not ambiguous.
         }
      }
   }

   public static void apply(LinkedList records) {
      Iterator i = records.iterator();
      while (i.hasNext()) {
         Record r = (Record) i.next();
         if (r.match != null) apply(r.combos,r.match);
         // else no change
      }
   }

   private static void apply(LinkedList combos, String match) {
      Iterator i = combos.iterator();
      while (i.hasNext()) {
         JComboBox combo = (JComboBox) i.next();
         ConfigSetup.putPrinterCombo(combo,match);
      }
   }

// --- alt printer combo ---

   // cf. ConfigSetup of course

   private static final String noChangeString = Text.get(ResolveDialog.class,"s7");
   private static final Object noChangeObject = new Object() {
      public String toString() { return noChangeString; }
   };

   private static JComboBox constructAltCombo() {
      JComboBox alt = ConfigSetup.constructPrinterCombo();
      alt.removeItemAt(0);
      alt.insertItemAt(noChangeObject,0);
      return alt;
   }

   private static void putAltCombo(JComboBox alt, String s) {
      alt.setSelectedItem( (s == null) ? noChangeObject : s );
   }

   private static String getAltCombo(JComboBox alt) {
      Object selected = alt.getSelectedItem();
      return (selected == noChangeObject) ? null : (String) selected;
   }

// --- fields ---

   private LinkedList records;

// --- construction ---

   public ResolveDialog(Dialog owner, LinkedList records) {
      super(owner,Text.get(ResolveDialog.class,"s1"));
      this.records = records;
      construct(constructFields(),/* readonly = */ false);
   }

// --- methods ---

   private static final Color pastelGreen = new Color(192,255,192);
   private static final Color pastelRed   = new Color(255,192,192);

   private JPanel constructFields() {

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      int y = 0;

      int w1 = Text.getInt(this,"w1");
      int w2 = Text.getInt(this,"w2");
      int w3 = Text.getInt(this,"w3");

      helper.add(1,y,Box.createHorizontalStrut(w2));
      helper.add(3,y,Box.createHorizontalStrut(w2));
      helper.add(5,y,Box.createHorizontalStrut(w2));
      //
      helper.add(2,y,new JLabel(Text.get(this,"s2")));
      helper.add(6,y,new JLabel(Text.get(this,"s3")));
      y++;

      Iterator i = records.iterator();
      while (i.hasNext()) {
         Record r = (Record) i.next();

         JTextField count = new DisableTextField(w3);
         count.setText(Convert.fromInt(r.combos.size()));
         count.setEnabled(false);

         JTextField printer = new DisableTextField(r.printer);
         printer.setEnabled(false);

         JTextField state = new DisableTextField(w1);
         state.setText(stateString[r.state]);
         state.setEnabled(false);
         state.setBackground((r.state == STATE_MATCH || r.state == STATE_RESOLVED) ? pastelGreen : pastelRed);

         r.alt = constructAltCombo();

         helper.addFill(0,y,count);
         helper.addFill(2,y,printer);
         helper.addFill(4,y,state);
         helper.addFill(6,y,r.alt);
         y++;
      }

      return fields;
   }

   protected void put() {
      Iterator i = records.iterator();
      while (i.hasNext()) {
         Record r = (Record) i.next();
         putAltCombo(r.alt,r.match);
      }
   }

   protected void getAndValidate() {
      Iterator i = records.iterator();
      while (i.hasNext()) {
         Record r = (Record) i.next();
         r.match = getAltCombo(r.alt);
      }
   }

}

