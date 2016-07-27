/*
 * GroupUtil.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.FieldAccessor;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NaturalComparator;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.struct.SKU;

import java.util.Comparator;

/**
 * A utility class containing accessors, comparators, and things.
 */

public class GroupUtil {

// --- accessors ---

   public static Accessor skuRaw = new Accessor() {
      public Class getFieldClass() { return SKU.class; }
      public Object get(Object o) { return ((Group) o).sku; }
   };

   public static Accessor sku = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Group) o).sku.toString(); }
   };

   public static Accessor statusString = new Accessor() { // essentially, status
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Group) o).statusString; }
   };

   public static Accessor statusOrder = new Accessor() {  // essentially, statusRaw
      public Class getFieldClass() { return Double.class; }
      public Object get(Object o) { return new Double(((Group) o).statusOrder); }
   };

   public static Accessor totalItemsRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Group) o).totalItems); }
   };

   public static Accessor totalItems = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromInt(((Group) o).totalItems); }
   };

   public static Accessor totalQuantityRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Group) o).totalQuantity); }
   };

   public static Accessor totalQuantity = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromInt(((Group) o).totalQuantity); }
   };

// --- queue column ---

   private static class QueueAccessor implements Accessor {

      private QueueList queueList;
      public QueueAccessor(QueueList queueList) { this.queueList = queueList; }

      public void reinit(QueueList queueList) { this.queueList = queueList; }

      public Class getFieldClass() { return String.class; }

      public Object get(Object o) {
         SKU sku = ((Group) o).sku;
         Queue q = queueList.findQueueBySKU(sku);
         return (q != null) ? q.name : ""; // null seems to work, but why push it?
      }
   }

   public static Comparator orderQueue(QueueList queueList) {
      Accessor accessor = new QueueAccessor(queueList);
      return sc(accessor);
   }

   public static GridColumn colQueue(QueueList queueList) {
      Accessor accessor = new QueueAccessor(queueList);
      return col(5,accessor,sc(accessor));
   }

   public static void reinit(GridColumn col, QueueList queueList) {
      ((QueueAccessor) col.accessor).reinit(queueList);
      // accessor is shared into comparator
      // caller has to force requery, we can't do that
   }

// --- columns ---

   private static GridColumn col(int n, Accessor accessor, Comparator comparator) {
      String suffix = Convert.fromInt(n);
      String name = Text.get(GroupUtil.class,"n" + suffix);
      int width;
      try {
         width = Convert.toInt(Text.get(GroupUtil.class,"w" + suffix));
      } catch (ValidationException e) {
         width = 1;
         // nothing we can do in a static context
      }
      return new GridColumn(name,width,accessor,comparator);
   }

   private static Comparator nc(Accessor accessor) {
      return new FieldComparator(accessor,new NaturalComparator());
   }

   private static Comparator sc(Accessor accessor) {
      return new FieldComparator(accessor,new NoCaseComparator());
   }

   public static Comparator orderSKU = new FieldComparator(skuRaw,SKUComparator.displayOrder);

   public static GridColumn colSKU           = col(1,sku,orderSKU);
   public static GridColumn colStatus        = col(2,statusString,nc(statusOrder));
   public static GridColumn colTotalItems    = col(3,totalItems,nc(totalItemsRaw));
   public static GridColumn colTotalQuantity = col(4,totalQuantity,nc(totalQuantityRaw));
   //                                              5 is used by colQueue, above

// --- order columns ---

   public static Accessor order = new Accessor() {
      public Class getFieldClass() { return Order.class; } // (*)
      public Object get(Object o) { return ((Group) o).order; }
   };

   // (*) getFieldClass is only called when the accessor
   // is used in a GridColumn, so this is never called,
   // but then there's no harm in returning the real class.
   // the same philosophy applies to all raw SKU columns.

   public static GridColumn colOrder(GridColumn col) {
      return colOrder(col,col.name);
   }
   public static GridColumn colOrder(GridColumn col, String name) {
      Comparator c = (col.comparator == null) ? null : new FieldComparator(order,col.comparator);
         // don't define a comparator if the underlying field doesn't have one
      return new GridColumn(name,col.width,new FieldAccessor(order,col.accessor),c);
   }

   // normally when we construct a column, the comparator is FC(accessor,c).
   // that's not literally true here, but the effect is the same.
   // you can prove it using the identity FC(a1,FC(a2,c)) = FC(FA(a1,a2),c).
   //
   //             col.comparator    is       FC(col.accessor,c),
   //
   // so FC(order,col.comparator) = FC(order,FC(col.accessor,c))
   //                             = FC(FA(order,col.accessor),c) = FC(accessor,c)

}

