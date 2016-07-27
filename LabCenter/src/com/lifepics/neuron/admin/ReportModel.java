/*
 * ReportModel.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.struct.FieldNode;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * An implementation of {@link TreeModel} on {@link ReportNode}.
 */

public class ReportModel implements TreeModel {

   private ReportNode root;
   public ReportModel(ReportNode root) { this.root = root; }

   public Object getRoot() { return root; }

   // the one function with some content

   public boolean isLeaf(Object node) {
      return (((ReportNode) node).node.getFieldClass() != FieldNode.class);
   }

   // simple list pass-through functions

   public Object getChild(Object parent, int index) {
      return ((ReportNode) parent).children.get(index);
   }
   public int getChildCount(Object parent) {
      return ((ReportNode) parent).children.size();
   }
   public int getIndexOfChild(Object parent, Object child) {
      return ((ReportNode) parent).children.indexOf(child);
   }

   // static model, none of these functions need to do anything

   public void addTreeModelListener(TreeModelListener l) {}
   public void removeTreeModelListener(TreeModelListener l) {}
   public void valueForPathChanged(TreePath path, Object newValue) {}

}

