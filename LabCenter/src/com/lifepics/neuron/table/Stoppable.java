/*
 * Stoppable.java
 */

package com.lifepics.neuron.table;

/**
 * An interface that threads can implement
 * for special consideration by {@link Table}.
 * This removes the upward dependency
 * to StoppableThread that we used to have.
 */

public interface Stoppable {

   boolean isStopping();

}

