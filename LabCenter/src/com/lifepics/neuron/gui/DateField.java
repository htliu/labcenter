/*
 * DateField.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.*;

/**
 * A set of fields for editing a date value (without a time part).
 */

public class DateField extends JPanel {

// --- constants ---

   private static Object[] monthNames, dayNames;
   private static int[] monthValues, dayValues;

   static {
      final int MONTHS = 12;
      final int DAYS = 31;

      Calendar calendar = Calendar.getInstance();
      SimpleDateFormat monthFormat = new SimpleDateFormat("MMM");

      monthNames = new Object[MONTHS+1];
      monthValues = new int[MONTHS+1];
      monthNames[0] = "";
      monthValues[0] = -1;
      for (int i=1; i<=MONTHS; i++) {
         // strange but true, months count from zero
         calendar.set(Calendar.MONTH,i-1);
         monthNames[i] = monthFormat.format(calendar.getTime());
         monthValues[i] = i-1;
      }

      dayNames = new Object[DAYS+1];
      dayValues = new int[DAYS+1];
      dayNames[0] = "";
      dayValues[0] = -1;
      for (int i=1; i<=DAYS; i++) {
         dayNames[i] = Convert.fromInt(i);
         dayValues[i] = i;
      }
   }

// --- fields ---

   private JComboBox month;
   private JComboBox day;
   private JTextField year;

// --- construction ---

   public DateField() {

      month = new JComboBox(monthNames);
      day = new JComboBox(dayNames);
      year = new JTextField(Text.getInt(this,"w1"));

      int d1 = Text.getInt(this,"d1");

      setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
      add(month);
      add(Box.createHorizontalStrut(d1));
      add(day);
      add(Box.createHorizontalStrut(d1));
      add(year);
   }

// --- get/put ---

   public Date get() throws ValidationException {

      int m = Field.get(month,monthValues);
      int d = Field.get(day,dayValues);
      String yearString = Field.getNullable(year);

      if (yearString == null) {
         if (m == -1 && d == -1) return null;
         // else fall through and fail
      } else {
         if (m != -1 && d != -1) {
            int y = Convert.toInt(yearString);
            if (y < 1000) throw new ValidationException(Text.get(this,"e2"));
               // the point of this is mainly to prevent negative numbers
            Calendar calendar = Calendar.getInstance();
            calendar.set(y,m,d,0,0,0);
            calendar.set(Calendar.MILLISECOND,0); // otherwise the result varies
            return calendar.getTime();
         }
         // else fall through and fail
      }

      throw new ValidationException(Text.get(this,"e1"));
   }

   public void put(Date date) {

      int m;
      int d;
      String yearString;

      if (date == null) {
         m = -1;
         d = -1;
         yearString = null;
      } else {
         Calendar calendar = Calendar.getInstance();
         calendar.setTime(date);
         m = calendar.get(Calendar.MONTH);
         d = calendar.get(Calendar.DAY_OF_MONTH);
         yearString = Convert.fromInt(calendar.get(Calendar.YEAR));
      }

      Field.put(month,monthValues,m);
      Field.put(day,dayValues,d);
      Field.putNullable(year,yearString);
   }

}

