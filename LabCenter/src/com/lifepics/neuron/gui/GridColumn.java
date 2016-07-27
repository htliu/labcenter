/*
 * GridColumn.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.meta.Accessor;

import java.util.Comparator;

/**
 * A class that represents a column of a {@linkplain Grid grid}.
 */

public class GridColumn {

// --- fields ---

   /**
    * The column name, which will appear in the table header.
    */
   public String name;

   /**
    * The column width, in characters.
    */
   public int width;

   /**
    * An accessor for the field that appears in this column.
    */
   public Accessor accessor;

   /**
    * A comparator that acts on instances of the <i>object</i>.
    * (If you want to act on instances of the field, use a {@link com.lifepics.neuron.meta.FieldComparator}.)
    * A value of null means that you can't sort on the column.
    */
   public Comparator comparator;

   /**
    * Whether the column is editable.
    */
   public boolean editable;

   /**
    * A user-definable property.
    */
   public Object property;

// --- construction ---

   public GridColumn(String name, int width, Accessor accessor, Comparator comparator) {
      this.name = name;
      this.width = width;
      this.accessor = accessor;
      this.comparator = comparator;
      editable = false;
      // property starts out null
   }

}

