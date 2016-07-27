/*
 * BarcodeConfig.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.util.LinkedList;

/**
 * An object that holds configuration information for barcode printing.
 * The numbers are really all just used as coordinates for drawing,
 * but if the coordinates don't eventually correspond to print pixels,
 * you'll get bar width variation from the scaling.
 */

public class BarcodeConfig extends Structure {

// --- fields ---

   public Integer modulePixels;
   public Integer heightPixels;
   public Integer textPixels;
   public Integer marginPixelH;
   public Integer marginPixelV;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      BarcodeConfig.class,
      0,0,
      new AbstractField[] {

         new NullableIntegerField("modulePixels","ModulePixels",0,null),
         new NullableIntegerField("heightPixels","HeightPixels",0,null),
         new NullableIntegerField("textPixels",  "TextPixels",  0,null),
         new NullableIntegerField("marginPixelH","MarginPixelH",0,null),
         new NullableIntegerField("marginPixelV","MarginPixelV",0,null)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (modulePixels != null && modulePixels.intValue() < 1) throw new ValidationException(Text.get(this,"e2"));
      if (heightPixels != null && heightPixels.intValue() < 1) throw new ValidationException(Text.get(this,"e3"));
      if (textPixels   != null && textPixels  .intValue() < 1) throw new ValidationException(Text.get(this,"e4"));
      if (marginPixelH != null && marginPixelH.intValue() < 0) throw new ValidationException(Text.get(this,"e5"));
      if (marginPixelV != null && marginPixelV.intValue() < 0) throw new ValidationException(Text.get(this,"e6"));
   }

   /**
    * Some validations only apply when barcode printing is enabled.
    */
   public void validateEnabledHard() throws ValidationException {

      if (    modulePixels == null
           || heightPixels == null
           || textPixels   == null
           || marginPixelH == null
           || marginPixelV == null ) throw new ValidationException(Text.get(this,"e1"));
   }

   /**
    * Some validations only apply when barcode printing is enabled.
    */
   public void validateEnabledSoft(double dpi, LinkedList list) {

      // this is only called after hard validation, so we know the fields are non-null

   // bar dimensions

      // dimension values from the GS1 UPC web site:
      //
      //           width   height
      // target    1.469   1.020
      // minimum   1.175   0.816   0.8 * target
      // maximum   2.938   2.040   2.0 * target

      final double WIDTH_MIN = 1.175;
      final double WIDTH_MAX = 2.938;
      final double HEIGHT_MIN = 0.816;
      final double HEIGHT_MAX = 2.040;

      double width = modulePixels.intValue() * 95 / dpi; // UPC is always 95 modules wide
      if (width < WIDTH_MIN || width > WIDTH_MAX) list.add(Text.get(this,"e7",new Object[] { Convert.fromDouble(width), Convert.fromDouble(WIDTH_MIN), Convert.fromDouble(WIDTH_MAX) }));

      double height = heightPixels.intValue() / dpi;
      if (height < HEIGHT_MIN || height > HEIGHT_MAX) list.add(Text.get(this,"e8",new Object[] { Convert.fromDouble(height), Convert.fromDouble(HEIGHT_MIN), Convert.fromDouble(HEIGHT_MAX) }));

   // margin rule

      if (marginPixelH.intValue() < 9 * modulePixels.intValue()) list.add(Text.get(this,"e9"));
      // this rule is from Wikipedia, but it sounds plausible
   }

}

