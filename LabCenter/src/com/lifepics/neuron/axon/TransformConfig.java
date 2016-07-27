/*
 * TransformConfig.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.*;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for image transforms.
 */

public class TransformConfig extends Structure {

// --- fields ---

   public boolean enableLimit;
   public int dpi;
   public double xInches;  // xLimit = dpi * xInches
   public double yInches;
   public boolean alwaysCompress;
   public int compression; // integer 0-100 scales to float 0-1

// --- constants ---

   public static final int COMPRESSION_MIN        =   0;
   public static final int COMPRESSION_MIN_USEFUL =   5;
   public static final int COMPRESSION_DEFAULT    =  75;
   public static final int COMPRESSION_MAX_USEFUL =  95;
   public static final int COMPRESSION_MAX        = 100;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      TransformConfig.class,
      0,3,
      new AbstractField[] {

         new BooleanField("enableLimit","EnableLimit",1,true),
         new IntegerField("dpi","DPI",3,303), // really, 303
         new DoubleField("xInches","XInches",1,8),
         new DoubleField("yInches","YInches",1,10),
         new BooleanField("alwaysCompress","AlwaysCompress",1,true),
         new IntegerField("compression","Compression",3,90)
      });

   protected StructureDefinition sd() { return sd; }

// --- utilities ---

   public int getXLimit() { return getLimit(xInches); }
   public int getYLimit() { return getLimit(yInches); }

   private int getLimit(double inches) {
      int limit = (int) Math.round(dpi*inches);
      if (limit == 0) limit = 1; // might round down, so guarantee nonzero
      return limit;
      // you could use Math.ceil, but then that'd make getInches a pain,
      // because it would have to handle the case where the division
      // produces a result that's infinitesimally larger than it should be.
   }

   // inverse, with getLimit(getInches(x)) == x in all cases,
   // so that when I convert users from pixels to inches,
   // the resulting behavior will always be exactly the same.
   //
   // no longer used, just saved so the comments make sense
   //
   private double getInches(int limit) {
      return limit / (double) dpi;
   }

   // in theory there could be some trouble with preserving the limit
   // after the inches goes through a file cycle, but in practice,
   // Convert allows four-place precision, which is plenty for 300 DPI
   // (which is the only one that matters for the conversion step)

// --- copy function ---

   public TransformConfig copy() { return (TransformConfig) CopyUtil.copy(this); }

// --- validation ---

   public void validate() throws ValidationException {

      if (dpi < 1) throw new ValidationException(Text.get(this,"e1"));
      if ( ! (xInches > 0 && yInches > 0) ) throw new ValidationException(Text.get(this,"e2"));
      if (compression < COMPRESSION_MIN || compression > COMPRESSION_MAX) throw new ValidationException(Text.get(this,"e3",new Object[] { Convert.fromInt(COMPRESSION_MIN), Convert.fromInt(COMPRESSION_MAX) }));
   }

// --- persistence ---

   // note, AbstractRollDialog constructs too

   public Object loadAncient(Node node) throws ValidationException {
      loadDefault();

      compression = Convert.toInt(XML.getElementText(node,"Compression"));
      // just like version 0 except there's no version number attached

      return this;
   }

   public TransformConfig derive(int transformType) { // this is like a loadDefault

      // DPI and compression are not derived, they come from the server

      TransformConfig tc = copy();

      switch (transformType) {
      case Roll.TRANSFORM_TYPE_REGULAR:
         tc.enableLimit = false;
         tc.xInches = 11; // these are absolutely not used when enableLimit is false,
         tc.yInches = 14; // but I want to fill in some value so that it's all reset.
         tc.alwaysCompress = false;
         break;
      case Roll.TRANSFORM_TYPE_FAST:
         tc.enableLimit = true;
         tc.xInches = 11;
         tc.yInches = 14;
         tc.alwaysCompress = true;
         break;
      case Roll.TRANSFORM_TYPE_FASTEST:
         tc.enableLimit = true;
         tc.xInches = 5;
         tc.yInches = 7;
         tc.alwaysCompress = true;
         break;
      default: throw new IllegalArgumentException();
      }

      return tc;
   }

}

