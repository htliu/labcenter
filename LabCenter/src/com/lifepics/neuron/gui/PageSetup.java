/*
 * PageSetup.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * A structure for PrintConfig.Setup and DirectJPEGConfig to share.
 */

public class PageSetup extends Structure {

   public String printer; // null means default printer
   public boolean defaultSize; // if true, width and height ignored
   public double width;
   public double height;
   public double marginT;
   public double marginB;
   public double marginL;
   public double marginR;
   public int orientation;

   public static final StructureDefinition sd = new StructureDefinition(

      PageSetup.class,
      // no version
      new AbstractField[] {

         new NullableStringField("printer","Printer",0,null),
         new BooleanField("defaultSize","DefaultSize",0,true),
         new DoubleField("width","Width",0,0),   // custom loadDefault
         new DoubleField("height","Height",0,0), //
         new DoubleField("marginT","MarginTop",0,0),    // custom loadDefault
         new DoubleField("marginB","MarginBottom",0,0), //
         new DoubleField("marginL","MarginLeft",0,0),   //
         new DoubleField("marginR","MarginRight",0,0),  //
         new EnumeratedField("orientation","Orientation",Orientation.orientationType,0,0) // custom loadDefault
      });

   protected StructureDefinition sd() { return sd; }

   public void validate() throws ValidationException {

      if (width <= 0 || height <= 0) {
         throw new ValidationException(Text.get(PageSetup.class,"e5")); // not "this" because it has a subclass
      }

      if (marginT < 0 || marginB < 0 || marginL < 0 || marginR < 0) {
         throw new ValidationException(Text.get(PageSetup.class,"e6"));
      }

      Orientation.validateOrientation(orientation);
   }

   public Object loadDefault(double width, double height, double margin, int orientation) {
      loadDefault();

      this.width = width;
      this.height = height;
      marginT = margin;
      marginB = margin;
      marginL = margin;
      marginR = margin;
      this.orientation = orientation;

      return this; // convenience
   }
}

