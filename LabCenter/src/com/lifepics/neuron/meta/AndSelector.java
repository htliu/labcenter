/*
 * AndSelector.java
 */

package com.lifepics.neuron.meta;

/**
 * A selector that combines the logic of other selectors using AND.
 */

public class AndSelector implements Selector {

// --- fields ---

   private Selector selector1;
   private Selector selector2;

// --- construction ---

   public AndSelector(Selector selector1, Selector selector2) {
      this.selector1 = selector1;
      this.selector2 = selector2;
   }

// --- implementation of Selector ---

   public boolean select(Object o) {
      return selector1.select(o) && selector2.select(o);
   }

}

