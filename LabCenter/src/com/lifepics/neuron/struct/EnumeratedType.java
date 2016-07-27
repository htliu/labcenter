/*
 * EnumeratedType.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.ValidationException;

/**
 * A simple interface for defining enumerated types.
 */

public interface EnumeratedType {

   int toIntForm(String s) throws ValidationException;
   String fromIntForm(int i);

}

