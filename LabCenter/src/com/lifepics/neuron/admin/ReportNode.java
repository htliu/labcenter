/*
 * ReportNode.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.EditAccessor;
import com.lifepics.neuron.meta.OuterAccessor;
import com.lifepics.neuron.meta.OuterEditAccessor;
import com.lifepics.neuron.struct.FieldNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A wrapper that goes around {@link FieldNode} to make it usable in a JTree
 * and provide various other functions.
 */

public class ReportNode {

// --- fields ---

   public FieldNode node;

   public ReportNode parent;
   public ArrayList children;

   public HashMap report;

   public Accessor accessorBase; // from Instance object, using OuterAccessor
   public Accessor accessorEdit; // for atomic fields this is an EditAccessor

// --- construction ---

   public ReportNode(FieldNode node, ReportNode parent) {

      this.node = node;

      this.parent = parent;
      children = new ArrayList();

      report = new HashMap();

      if (parent == null) {
         accessorBase = new RootAccessorBase();
         accessorEdit = new RootAccessorEdit();
      } else {
         accessorBase = new OuterAccessor(parent.accessorBase,node);
         if (node instanceof EditAccessor) {
            accessorEdit = new CopyOnWrite(parent.accessorEdit,(EditAccessor) node);
         } else {
            accessorEdit = new OuterAccessor(parent.accessorEdit,node);
         }
      }
      // why make all the accessors up front?  I'm not sure it's the best design,
      // actually, but it does let you share the parent accessors across columns.
   }

// --- accessors ---

   private static class RootAccessorBase implements Accessor {
      public Class getFieldClass() { return FieldNode.class; }
      public Object get(Object o) {
         Instance instance = (Instance) o;
         return instance.configBase;
      }
   }

   private static class RootAccessorEdit implements Accessor {
      public Class getFieldClass() { return FieldNode.class; }
      public Object get(Object o) {
         Instance instance = (Instance) o;
         return (instance.configEdit != null) ? instance.configEdit : instance.configBase;
      }
   }

   private static class CopyOnWrite extends OuterEditAccessor {
      public CopyOnWrite(Accessor accessor1, EditAccessor accessor2) { super(accessor1,accessor2); }
      public void put(Object o, Object value) {
         Instance instance = (Instance) o;
         if (instance.configEdit == null) {

            // before copying, check (on configBase) that the outer join will succeed
            // this is a nuisance, but it's important not to send a config update for
            // no reason.
            if (accessor1.get(o) == null) return;

            instance.configEdit = instance.configBase.copy();
         }
         super.put(o,value);
      }
   }

   public String toString() { return node.getName(); } // necessary for going in ReportModel

// --- tree construction ---

   // it would be nice if we could combine this with reporting, but it doesn't work that way
   // -- any dynamic nodes created after the first pass would end up with incomplete tallies.

   /**
    * @param isNew True if the current ReportNode was just created and hence needs to have its
    *              children added even if it's not dynamic.
    */
   public void build(Object o, boolean isNew) {
      if (node.getFieldClass() != FieldNode.class) { // atomic field, no children
         // done
      } else if (o == null) { // can't iterate further
         // done
      } else { // can and should iterate further

         Iterator i = children.iterator(); // existing children (ReportNode)
         while (i.hasNext()) {
            ReportNode child = (ReportNode) i.next();
            child.build(child.node.get(o),/* isNew = */ false);
         }

         if (isNew || node.isDynamic()) {

            Collection newChildren = node.getChildren(o);
            if (newChildren != null) { // null means empty

               if ( ! isNew ) exclude(children,newChildren);
               // note 1: no need to exclude when the node is dynamic *and* new
               // note 2: newChildren is only modifiable when isDynamic is true

               i = newChildren.iterator(); // new children (FieldNode)
               while (i.hasNext()) {
                  FieldNode cnode = (FieldNode) i.next();
                  ReportNode child = new ReportNode(cnode,this);
                  children.add(child);
                  child.build(child.node.get(o),/* isNew = */ true);
                  // child.node is the same thing as cnode,
                  // but I want to keep the same structure as in the first loop
               }
            }
         }
      }
   }

   private static void exclude(Collection children, Collection newChildren) {
      Iterator i = newChildren.iterator();
      while (i.hasNext()) {
         FieldNode cnode = (FieldNode) i.next();
         if (contains(children,cnode)) i.remove();
      }
   }

   private static boolean contains(Collection children, FieldNode cnode) {
      Iterator i = children.iterator();
      while (i.hasNext()) {
         ReportNode child = (ReportNode) i.next();
         if (child.node.equals(cnode)) return true;
      }
      return false;
   }

// --- reporting ---

   public static class Record {
      public Object value;
      public int count;
      public Record(Object value) { this.value = value; count = 0; }
   }

   private void tally(Object value) {
      Record r = (Record) report.get(value);
      if (r == null) {
         r = new Record(value);
         report.put(value,r);
      }
      r.count++;
   }

   public void report(Object o) {
      if (node.getFieldClass() != FieldNode.class) { // atomic field, no children
         tally(o);
      } else if (o == null) { // can't iterate further
         tally(Boolean.FALSE);
      } else { // can and should iterate further
         tally(Boolean.TRUE);

         Iterator i = children.iterator();
         while (i.hasNext()) {
            ReportNode child = (ReportNode) i.next();
            child.report(child.node.get(o));
         }
      }
   }

}

