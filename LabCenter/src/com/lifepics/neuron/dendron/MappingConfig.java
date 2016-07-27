/*
 * MappingConfig.java
 */

package com.lifepics.neuron.dendron;

import java.util.LinkedList;

/**
 * An interface that defines common behavior for config objects with mappings.
 */

public interface MappingConfig {

   LinkedList getMappings();
   void putMappings(LinkedList mappings);

   boolean mapsSpecialSKU();

}

