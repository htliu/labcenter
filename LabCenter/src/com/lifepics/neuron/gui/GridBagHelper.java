/*
 * GridBagHelper.java
 */

package com.lifepics.neuron.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A helper class for creating grid bag layouts.
 */

public class GridBagHelper {

// --- fields ---

   private Container owner;
   private GridBagLayout layout;
   private GridBagConstraints constraints;

// --- construction ---

   public GridBagHelper(Container owner) {
      this.owner = owner;

      layout = new GridBagLayout();
      owner.setLayout(layout);

      constraints = new GridBagConstraints();
   }

   private GridBagHelper() {
   }
   public static GridBagHelper modify(Container owner) {
      GridBagHelper helper = new GridBagHelper();
      helper.owner = owner;
      helper.layout = (GridBagLayout) owner.getLayout();
      helper.constraints = new GridBagConstraints();
      return helper;
   }

// --- component placement ---

   // there are two choices you have to make when placing a component:
   //
   //  * should it span multiple columns?
   //
   //  * should it align left (normal), align center,
   //    or expand to fill all available space?

   public void add(int x, int y, Component c) {
      add(x,y,c,false,false,1);
   }

   public void addCenter(int x, int y, Component c) {
      add(x,y,c,true,false,1);
   }

   public void addFill(int x, int y, Component c) {
      add(x,y,c,false,true,1);
   }

   public void addSpan(int x, int y, int span, Component c) {
      add(x,y,c,false,false,span);
   }

   public void addSpanCenter(int x, int y, int span, Component c) {
      add(x,y,c,true,false,span);
   }

   public void addSpanFill(int x, int y, int span, Component c) {
      add(x,y,c,false,true,span);
   }

   private void add(int x, int y, Component c, boolean center, boolean fill, int span) {

      constraints.anchor = center ? GridBagConstraints.CENTER : GridBagConstraints.WEST;
      constraints.fill = fill ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
      constraints.gridwidth = span;

      // any field on constraints that we set, we set every time,
      // so there is no difficulty in reusing the constraints object

      add(x,y,c,constraints);
   }

   /**
    * An add method that allows you to pass in an arbitrary GridBagConstraints object,
    * which is useful when you want to do something not covered by the methods above.
    */
   public void add(int x, int y, Component c, GridBagConstraints constraints) {

      constraints.gridx = x;
      constraints.gridy = y;

      layout.setConstraints(c,constraints);
      owner.add(c);
   }

// --- some less common constraints ---

   // since my add function will write onto the x and y coordinates of these,
   // it would be bad if there were multiple threads, but it's all in the UI.

   public static final GridBagConstraints fillBoth = new GridBagConstraints();
   public static final GridBagConstraints fillVertical = new GridBagConstraints();
   public static final GridBagConstraints alignTop = new GridBagConstraints();
   public static final GridBagConstraints alignTopLeft = new GridBagConstraints();
   public static final GridBagConstraints alignRight = new GridBagConstraints();

   static {
      fillBoth.fill = GridBagConstraints.BOTH;

      fillVertical.anchor = GridBagConstraints.CENTER;
      fillVertical.fill = GridBagConstraints.VERTICAL;

      alignTop.anchor = GridBagConstraints.NORTH;

      alignTopLeft.anchor = GridBagConstraints.NORTHWEST;

      alignRight.anchor = GridBagConstraints.EAST;
   }

// --- row and column weights ---

   /**
    * Add a zero-size component to the layout.
    * This is useful when you want to assign a weight to a row or column
    * that lies beyond the current extent of the grid.
    */
   public void addBlank(int x, int y) {
      add(x,y,new JLabel());
   }

   private double[] setWeight(double[] weights, int i, double weight) {
      int needLength = i+1;
      if (weights == null || weights.length < needLength) {
         double[] temp = new double[needLength];
         if (weights != null) System.arraycopy(weights,0,temp,0,weights.length);
         weights = temp;
      }
      weights[i] = weight;
      return weights;
   }

   public void setRowWeight(int y, double weight) {
      layout.rowWeights = setWeight(layout.rowWeights,y,weight);
   }

   public void setColumnWeight(int x, double weight) {
      layout.columnWeights = setWeight(layout.columnWeights,x,weight);
   }

}

