/*
 * Copyable.java
 */

package com.lifepics.neuron.object;

/**
 * An interface for objects that are publicly copyable.
 * The function is named clone rather than copy
 * because it exactly matches the existing clone function.<p>
 *
 * A copyable object will typically also use the function
 * {@link CopyUtil#copy(Copyable) CopyUtil.copy} to implement
 * a type-safe copy function that doesn't throw exceptions.
 */

public interface Copyable extends Cloneable {

   Object clone() throws CloneNotSupportedException;

}

