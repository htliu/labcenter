/*
 * NotSelector.java
 */

package com.lifepics.neuron.meta;

/**
 * A selector that applies a NOT prefix to the logic of another selector.
 */

public class NotSelector implements Selector {

// --- fields ---

   private Selector selector;

// --- construction ---

   public NotSelector(Selector selector) {
      this.selector = selector;
   }

// --- implementation of Selector ---

   public boolean select(Object o) {
      return ! selector.select(o);
   }

}

