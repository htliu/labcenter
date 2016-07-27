/*
 * PSKU.java
 */

package com.lifepics.neuron.struct;

/**
 * A marker interface for OldSKU, NewSKU, and Pattern.
 * The name is short for "pattern SKU" or something.
 * Every SKU is also a PSKU that matches just one SKU.
 */

public interface PSKU {

   // implement equals, hashCode, toString, and matches

   /**
    * Test whether the argument matches the PSKU object.
    */
   boolean matches(SKU sku);

}

