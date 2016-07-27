/*
 * LucidiomMapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds SKU mapping information for the Lucidiom format.
 */

public class LucidiomMapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public String product;
   public String productType; // nullable
   public Integer width;
   public Integer height;
   public String name;

   // the width and height are only needed for PDF products,
   // where we can't get them from the image file (easily)

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      LucidiomMapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new StringField("product","Product"),
         new NullableStringField("productType","ProductType"),
         new NullableIntegerField("width","Width"),
         new NullableIntegerField("height","Height"),
         new StringField("name","Name")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      // product length zero is caught by validateProduct, no need to duplicate

      LucidiomConfig.validateProduct(product);

      if (width  != null && width .intValue() < 0) throw new ValidationException(Text.get(this,"e3"));
      if (height != null && height.intValue() < 0) throw new ValidationException(Text.get(this,"e4"));
         // zero isn't a good dimension, but I can imagine we might want to send it to Lucidiom
      if ((width != null) != (height != null)) throw new ValidationException(Text.get(this,"e5"));

      // no constraints on name part
   }

// --- migration ---

   public void migrate(Channel c) throws ValidationException {

      // the channel is of the form "<product>[(<w>x<h>)][^<type>][ <name>]".
      // peel off the pieces one at a time, starting from the back.
      // the syntax doesn't permit spaces in the product type, although the
      // modernized structure supports it.
      // (the product, width, and height are numbers, so, no spaces there)

      String s = c.channel;

      int i = s.indexOf(' '); // not lastIndexOf, name can include spaces
      if (i != -1) {
         name = s.substring(i+1);
         s    = s.substring(0,i);
      }
      else name = "";

      int j3 = s.indexOf('^'); // not lastIndexOf, nothing prior can interfere
      if (j3 != -1) {
         productType = s.substring(j3+1);
         s           = s.substring(0,j3);
      }
      // else productType stays null

      int j0 = s.indexOf('('); // not lastIndexOf, nothing prior can interfere
      int j1 = s.indexOf('x');
      int j2 = s.indexOf(')');
      if (    j0 != -1 && j1 != -1 && j2 != -1
           && j0 <  j1 && j1 <  j2 && j2 == s.length()-1 ) {
         width  = new Integer(Convert.toInt(s.substring(j0+1,j1)));
         height = new Integer(Convert.toInt(s.substring(j1+1,j2)));
         s      = s.substring(0,j0);
      }
      // else leave width and height null.  if the syntax is bad,
      // the digit validation on the product field will catch it.

      psku = new OldSKU(c.sku);
      product = s;
   }

}

