/*
 * ProductCallback.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.struct.SKU;

/**
 * A callback interface for iterating over old and new products.
 */

public interface ProductCallback {

   void f(SKU sku, String description, Long dueInterval);

}

