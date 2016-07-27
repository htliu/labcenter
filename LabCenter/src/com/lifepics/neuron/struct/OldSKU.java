/*
 * OldSKU.java
 */

package com.lifepics.neuron.struct;

/**
 * An object that represents an old SKU, which is just a string.
 * I'd rather subclass String, but I can't do that because it's
 * a final class.  Like NewSKU, this is used mostly on the
 * dendron side of things; unlike NewSKU, it's small enough that
 * I think it's not worth interning.
 */

public final class OldSKU implements SKU {

   private String product;
   public OldSKU(String product) { this.product = product; }

   public boolean equals(Object o) {
      return (o instanceof OldSKU && product.equals(((OldSKU) o).product));
   }
   public boolean matches(SKU sku) { return equals(sku); }

   public int    hashCode() { return product.hashCode(); }
   public String toString() { return product;            }

   // decode and encode are redundant since you can get the same results
   // with the constructor and toString.  the reason I'm keeping them is,
   // I like the parallel with NewSKU and Pattern, and I want to try and
   // keep slightly different meanings for them.
   //
   // * the constructor is for constructing from raw product information.
   //   here it's just a string, but think about NewSKU ctor.
   // * toString is for displaying formatted SKU information to the user.
   // * decode and encode are for converting between string storage form
   //   and in-memory form.

   public static String encode(OldSKU os) {
      return os.product;
   }

   public static OldSKU decode(String s) {
      return new OldSKU(s);
   }

}

