/*
 * SKU.java
 */

package com.lifepics.neuron.struct;

/**
 * A marker interface for OldSKU and NewSKU.
 */

public interface SKU extends PSKU {

   // no requirements beyond Object -- just implement equals, hashCode, and toString
   // actually we have to implement matches for PSKU now too

}

