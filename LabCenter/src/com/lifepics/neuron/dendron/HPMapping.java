/*
 * HPMapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import org.w3c.dom.Node;

/**
 * An object that holds SKU mapping information for the HP format.
 */

public class HPMapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public String productID; // really an int, but we don't require that
   public String productName;
   public boolean crop;
   public boolean enhance;
   public String layoutWidth;
   public String layoutHeight;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      HPMapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new StringField("productID","ProductID") {
            protected void loadNormal(Node node, Object o) throws ValidationException {
               String temp = getNullableText(node,xmlName);
               String s = (temp == null) ? "???" : temp;
               tset(o,s);
            }
            // the point of this override is to make it easy to migrate from the old
            // mapping structure that didn't have an ID field.  if there were more
            // than one install doing this, it would probably be worth doing the lookup
            // in the product line file instead of filling in question marks.
         },
         new StringField("productName","ProductName"),
         new BooleanField("crop","Crop"),
         new BooleanField("enhance","Enhance"),
         new NullableStringField("layoutWidth","LayoutWidth"),
         new NullableStringField("layoutHeight","LayoutHeight")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
      if ((layoutWidth != null) != (layoutHeight != null)) throw new ValidationException(Text.get(this,"e1"));
   }

// --- migration ---

   public void migrate(Channel c) throws ValidationException {}
      // n/a, this mapping never existed as a channel

}

