/*
 * TransferPanel.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.TransferListener;

import java.text.DecimalFormat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel that displays full information from a {@link TransferListener} stream.
 */

public class TransferPanel extends JPanel implements TransferListener {

// --- fields ---

   private TransferProgress transferProgress;

   private JTextField fieldGroupID;
   private JTextField fieldCount;
   private JTextField fieldCountGoal;
   private JTextField fieldTotalSize;
   private JTextField fieldTotalSizeGoal;
   private JTextField fieldPercent;
   private JTextField fieldKbps;
   private JTextField fieldTime;
   private JTextField fieldEstimated;
   private JTextField fieldFilename;
   private JTextField fieldSize;
   private JTextField fieldSizeGoal;

// --- construction ---

   public TransferPanel(String title, String groupLabel, TransferProgress transferProgress) {
      this(title,groupLabel,transferProgress,/* hideTotalSize = */ false);
   }
   public TransferPanel(String title, String groupLabel, TransferProgress transferProgress, boolean hideTotalSize) {
      this.transferProgress = transferProgress;

      setBorder(Style.style.createCompactBorder(title));

      int w1 = Text.getInt(this,"w1");
      int w2 = Text.getInt(this,"w2");
      int w3 = Text.getInt(this,"w3");
      int w4 = Text.getInt(this,"w4");

      int d1 = Text.getInt(this,"d1");
      int d2 = Text.getInt(this,"d2");
      int d3 = Text.getInt(this,"d3");

      boolean allowAdjacentFields = Style.style.allowAdjacentFields();

      fieldGroupID       = makeField(w1,false);
      fieldCount         = makeField(w2,true);
      fieldCountGoal     = makeField(w2,true);
      fieldTotalSize     = makeField(w3,true);
      fieldTotalSizeGoal = makeField(w3,true);
      fieldPercent       = makeField(w4,true);
      fieldKbps          = makeField(w4,true);
      fieldTime          = makeField(w4,true);
      fieldEstimated     = makeField(w4,true);
      fieldFilename      = makeField(0,false); // no column count, expand to fill
      fieldSize          = makeField(w3,true);
      fieldSizeGoal      = makeField(w3,true);

      GridBagHelper helper = new GridBagHelper(this);

      String divider = ' ' + Text.get(this,"s1") + ' ';

      if ( ! allowAdjacentFields ) helper.add(0,0,Box.createVerticalStrut(d1));

      if (groupLabel != null) {

         int y = Style.style.lowerTransferID() ? 3 : 1;

         helper.add(1,y,Style.style.adjustPlain(new JLabel(groupLabel + ' ')));
         helper.add(2,y,fieldGroupID);
      }
      // else just leave them out of the layout

      helper.add(3,1,Style.style.adjustPlain(new JLabel(' ' + Text.get(this,"s2") + ' ')));
      helper.add(4,1,fieldCount);
      helper.add(5,1,Style.style.adjustPlain(new JLabel(divider)));
      helper.add(6,1,fieldCountGoal);
      helper.add(8,1,new JLabel());

      int y = hideTotalSize ? 3 : 1;
      helper.add(9,y,Style.style.adjustPlain(new JLabel(' ' + Text.get(this,"s3") + ' ')));

      if ( ! hideTotalSize ) {
         helper.add(10,1,fieldTotalSize);
         helper.add(11,1,Style.style.adjustPlain(new JLabel(divider)));
         helper.add(12,1,fieldTotalSizeGoal);
      }

      helper.add(13,1,Style.style.adjustPlain(new JLabel(' ' + Text.get(this,"s4") + ' ')));
      helper.add(14,1,fieldPercent);
      helper.add(15,1,Style.style.adjustPlain(new JLabel(' ' + Text.get(this,"s7") + ' ')));
      helper.add(16,1,fieldTime);

      helper.add(13,3,Style.style.adjustPlain(new JLabel(' ' + Text.get(this,"s6") + ' ')));
      helper.add(14,3,fieldKbps);
      helper.add(15,3,Style.style.adjustPlain(new JLabel(' ' + Text.get(this,"s8") + ' ')));
      helper.add(16,3,fieldEstimated);

      if ( ! allowAdjacentFields ) helper.add(0,2,Box.createVerticalStrut(d2));

      helper.add(3,3,Style.style.adjustPlain(new JLabel(' ' + Text.get(this,"s5") + ' ')));
      helper.addSpanFill(4,3,4,fieldFilename);
      helper.add(10,3,fieldSize);
      helper.add(11,3,Style.style.adjustPlain(new JLabel(divider)));
      helper.add(12,3,fieldSizeGoal);

      helper.add(0,4,Box.createVerticalStrut(d1));

      if ( ! allowAdjacentFields ) {
         helper.add( 0,1,Box.createHorizontalStrut(d3));
         helper.add(17,1,Box.createHorizontalStrut(d3));
      }

      // ok, this is ugly, but it handles the change from glue to non-glue
      if (allowAdjacentFields) helper.setColumnWeight(2,1);
      else helper.addSpan(1,2,2,Box.createHorizontalStrut(Text.getInt(this,"d4")));

      helper.setColumnWeight(7,3);
      helper.setColumnWeight(8,1);
      helper.setColumnWeight(12,1);
      helper.setColumnWeight(14,1);
   }

// --- UI helpers ---

   private JTextField makeField(int columns, boolean alignRight) {
      JTextField field = new JTextField(columns);

      field.setEnabled(false);
      if (alignRight) field.setHorizontalAlignment(JTextField.RIGHT);

      Style.style.adjustDisabledField(field);

      return field;
   }

// --- implementation of TransferListener ---

   private static final int kB = 1024;

   private static void put(JTextField f, String s) {
      if ( ! Field.get(f).equals(s) ) Field.put(f,s); // reduce flicker
   }

// --- group fields ---

   public void setGroupID(String groupID) {
      put(fieldGroupID,groupID);
   }

   public void setCount(int count) {
      put(fieldCount,Convert.fromInt(count));
   }

   public void setCountGoal(int countGoal) {
      put(fieldCountGoal,Convert.fromInt(countGoal));
   }

   public void setTotalSize(long totalSize) {
      put(fieldTotalSize,Convert.fromLongPretty(totalSize/kB));
   }

   public void setTotalSizeGoal(long totalSizeGoal) {
      put(fieldTotalSizeGoal,Convert.fromLongPretty(totalSizeGoal/kB));
   }

   public void setProgress(double fraction, int percent) {
      put(fieldPercent,Convert.fromInt(percent) + '%');
      transferProgress.setProgress(fraction,percent);
   }

   public void clearGroup() {
      put(fieldGroupID,"");
      put(fieldCount,"");
      put(fieldCountGoal,"");
      put(fieldTotalSize,"");
      put(fieldTotalSizeGoal,"");
      put(fieldPercent,"");
      transferProgress.clearProgress();
   }

// --- time fields ---

   public void setTime(long timeMillis) {
      put(fieldTime,formatInterval(timeMillis));
   }

   public void setEstimated(Long estimatedMillis) {
      if (estimatedMillis != null) {
         put(fieldEstimated,formatInterval(estimatedMillis.longValue()));
      } else {
         put(fieldEstimated,""); // could be different if we wanted
      }
   }

   public void setKbps(Double kbps) {
      if (kbps != null) {
         put(fieldKbps,kbpsFormat.format(kbps.doubleValue()));
      } else {
         put(fieldKbps,""); // could be different if we wanted
      }
   }

   public void clearTime() {
      put(fieldTime,"");
      put(fieldEstimated,"");
      put(fieldKbps,"");
   }

   private static String formatInterval(long interval) {

      // for intervals, not dates/times.  it's similar to what's in IntervalField,
      // except that the display units are fixed and there are no string suffixes.

      long seconds = (interval + 500) / 1000; // normal rounding here, it's for display only

      long minutes = seconds / 60;
      seconds      = seconds % 60;

      long hours   = minutes / 60;
      minutes      = minutes % 60;

      String s1 = Convert.fromLong(hours);
      String s2 = Convert.fromLong(minutes);
      String s3 = Convert.fromLong(seconds);

      // 0:00:00 if there are hours, 0:00 if not

      String key;
      if (hours != 0) {
         key = "s9";
         if (s2.length() < 2) s2 = "0" + s2;
      } else {
         key = "s10";
      }

      if (s3.length() < 2) s3 = "0" + s3;

      return Text.get(TransferPanel.class,key,new Object[] { s1, s2, s3 });
   }

   private static DecimalFormat kbpsFormat = new DecimalFormat("#,##0.0");
      // same as fromLongPretty except with a decimal place

// --- file fields ---

   public void setFilename(String filename) {
      put(fieldFilename,filename);
   }

   public void setSize(long size) {
      put(fieldSize,Convert.fromLongPretty(size/kB));
   }

   public void setSizeGoal(long sizeGoal) {
      put(fieldSizeGoal,Convert.fromLongPretty(sizeGoal/kB));
   }

   public void clearFile() {
      put(fieldFilename,"");
      put(fieldSize,"");
      put(fieldSizeGoal,"");
   }

}

