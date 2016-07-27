/*
 * IntervalField.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;

import javax.swing.*;

/**
 * A field (really, pair of fields) for editing time-interval values.
 */

public class IntervalField extends JPanel {

// --- constants ---

   public static final int MILLIS = 0;
   public static final int SECONDS = 1;
   public static final int MINUTES = 2;
   public static final int HOURS = 3;
   public static final int DAYS = 4;
   public static final int WEEKS = 5;

   private static Object[] unitNames = new Object[] { Text.get(IntervalField.class,"u0"),
                                                      Text.get(IntervalField.class,"u1"),
                                                      Text.get(IntervalField.class,"u2"),
                                                      Text.get(IntervalField.class,"u3"),
                                                      Text.get(IntervalField.class,"u4"),
                                                      Text.get(IntervalField.class,"u5")  };

   private static long[] unitValues = new long[] {         1,
                                                        1000,
                                                       60000,
                                                     3600000,
                                                    86400000,
                                                   604800000  };

// --- fields ---

   private int unitMin;
   private int unitMax;

   private JTextField number;
   private JComboBox unit;

// --- construction ---

   public IntervalField(int unitMin, int unitMax) {
      this(unitMin,unitMax,Text.getInt(IntervalField.class,"w1"));
   }

   public IntervalField(int unitMin, int unitMax, int width) {
      this.unitMin = unitMin;
      this.unitMax = unitMax;

      number = new JTextField(width);
      unit = new JComboBox();
      for (int i=unitMin; i<=unitMax; i++) unit.addItem(unitNames[i]);

      setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
      add(number);
      add(Box.createHorizontalStrut(Text.getInt(this,"d1")));
      add(unit);
   }

// --- get/put ---

   // these don't use the usual JComboBox Field functions,
   // which aren't convenient for variable-content combos

   public long get() throws ValidationException {

      long n = Convert.toLong(Field.get(number));
      long u = unitValues[unitMin + unit.getSelectedIndex()];
      return n * u;
   }

   public void put(long interval) {

      // scan for largest applicable unit
      int i;
      for (i=unitMax; i>unitMin; i--) {
         if (interval % unitValues[i] == 0) break; // found
      }
      // if we reach unitMin, fall through

      // we have to round upward on division (in the unitMin case),
      // otherwise we might convert a valid config into an invalid,
      // by turning a positive number into zero.
      //
      long u = unitValues[i];
      long n = (interval + (u-1)) / u;

      Field.put(number,Convert.fromLong(n));
      unit.setSelectedIndex(i-unitMin);
   }

   public static String format(long interval, int unitMin, int unitMax) {

      // same algorithm as above
      int i;
      for (i=unitMax; i>unitMin; i--) {
         if (interval % unitValues[i] == 0) break;
      }

      long u = unitValues[i];
      long n = (interval + (u-1)) / u;

      String unit = (String) unitNames[i];
      if (n == 1) unit = unit.substring(0,unit.length()-1); // de-pluralize
      return Convert.fromLong(n) + ' ' + unit;
   }

}

