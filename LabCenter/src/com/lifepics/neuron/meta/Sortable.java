/*
 * Sortable.java
 */

package com.lifepics.neuron.meta;

import java.util.Comparator;

/**
 * An interface for sorting a thing without knowing what it is.
 */

public interface Sortable {

   void sort(Comparator comparator);

}

