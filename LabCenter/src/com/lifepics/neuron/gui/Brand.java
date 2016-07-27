/*
 * Brand.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;

import javax.swing.ImageIcon;

/**
 * The branding component of the modern style, factored out for clarity.
 */

public abstract class Brand {

// --- interface ---

   public abstract String getName();
   public abstract ImageIcon getLogo();
   public abstract ImageIcon getMenuLogo();
   public abstract boolean frameMenuLogo();

// --- LifePics ---

   public static final Brand brandLifePics = new Brand() {
      public String getName() { return Text.get(Brand.class,"s1"); }
      public ImageIcon getLogo()     { return Graphic.getIcon("LifePics.gif"); }
      public ImageIcon getMenuLogo() { return Graphic.getIcon("LifePics.gif",Text.getInt(Brand.class,"d1")); }
      public boolean frameMenuLogo() { return true; }
   };

// --- QDPC ---

   public static final Brand brandQDPC = new Brand() {
      public String getName() { return Text.get(Brand.class,"s2"); }
      public ImageIcon getLogo()     { return Graphic.getIcon("qdpc2.gif"); }
      public ImageIcon getMenuLogo() { return Graphic.getIcon("qdpc1.gif"); }
      public boolean frameMenuLogo() { return false; }
   };

}

