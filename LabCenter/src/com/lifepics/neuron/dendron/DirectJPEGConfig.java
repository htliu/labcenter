/*
 * DirectJPEGConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.PageSetup;
import com.lifepics.neuron.struct.*;

import java.awt.print.PageFormat;

/**
 * An object that holds configuration information for the direct-print JPEG format.
 */

public class DirectJPEGConfig extends Structure {

// --- constants ---

   private static final int PIXELS_DEFAULT = 2000; // chosen because 4x6 at 300 DPI generally works
   private static final int PIXELS_MIN     =  100; // arbitrary but very reasonable

// --- fields ---

   public PageSetup pageSetup;
   public boolean rotateCW;
   public boolean completeImmediately;
   public boolean tileEnable;
   public int tilePixels;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      DirectJPEGConfig.class,
      0,new History(new int[] {0,755,1}),
      new AbstractField[] {

         new StructureField("pageSetup","PageSetup",PageSetup.sd,0) {
            public void loadDefault(Object o) {
               tset(o,new PageSetup().loadDefault(4,6,0,PageFormat.PORTRAIT));
            }
         },
         new BooleanField("rotateCW","RotateCW",0,false),
         new BooleanField("completeImmediately","CompleteImmediately",0,false),
         new BooleanField("tileEnable","TileEnable",1,true),
         new IntegerField("tilePixels","TilePixels",1,PIXELS_DEFAULT)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      pageSetup.validate();

      if (tilePixels < PIXELS_MIN) throw new ValidationException(Text.get(this,"e1",new Object[] { Convert.fromInt(PIXELS_MIN) }));
   }

}

