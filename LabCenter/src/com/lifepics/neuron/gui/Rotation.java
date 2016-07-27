/*
 * Rotation.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;

import java.awt.geom.AffineTransform;

/**
 * A utility class to hold the various rotation transforms.
 */

public class Rotation {

// --- rotation type operations ---

   // in theory, any integer could be a rotation, and we could have
   // a function "normalize" that does "&3" to produce normal form.
   //
   // in practice, all rotations are normalized all the time.

   public static void validate(int r) throws ValidationException {
      if (r < 0 || r > 3) {
         throw new ValidationException(Text.get(Rotation.class,"e1",new Object[] { Convert.fromInt(r) }));
      }
   }

   public static boolean isEven(int r) { return ((r & 1) == 0); }
   public static boolean isOdd (int r) { return ((r & 1) == 1); }

   public static int add(int r1, int r2) {
      return (r1 + r2) & 3;
      // "&3" is better than "%4" because it returns the
      // normal form even if the arguments aren't normal
   }

   public static int rotateCW(int r) {
      return add(r,3); // 3 is the normal form of -1
   }

   public static int rotateCCW(int r) {
      return add(r,1);
   }

   public static int rotate(int r, boolean clockwise) {
      return add(r,clockwise ? 3 : 1);
   }

// --- the identity transformation ---

   // it would be nice to store this and the pure rotations as constants,
   // but AffineTransform objects are mutable.

   public static AffineTransform identity() {
      return new AffineTransform(1,0,0,1,0,0);
   }

// --- pure rotations ---

   public static AffineTransform pure90() {
      return new AffineTransform(0,-1,1,0,0,0);
   }

   public static AffineTransform pure180() {
      return new AffineTransform(-1,0,0,-1,0,0);
   }

   public static AffineTransform pure270() {
      return new AffineTransform(0,1,-1,0,0,0);
   }

   public static AffineTransform pure(int rotation) {
      switch (rotation) {
      case 0:  return identity();
      case 1:  return pure90();
      case 2:  return pure180();
      case 3:  return pure270();
      default: throw new IllegalArgumentException();
      }
   }

// --- rotations holding center fixed ---

   // the arguments are doubles mainly as a convenient way
   // to avoid having to cast to double for the arithmetic.
   // in practice they will be integers.

   // if the width and height are opposite parity,
   // the 90 and 270 rotations will use half coordinates,
   // which is maybe not ideal.  use with caution!

   public static AffineTransform center90(double w, double h) {
      return new AffineTransform(0,-1,1,0,(w-h)/2,(w+h)/2);
   }

   public static AffineTransform center180(double w, double h) {
      return new AffineTransform(-1,0,0,-1,w,h);
   }

   public static AffineTransform center270(double w, double h) {
      return new AffineTransform(0,1,-1,0,(w+h)/2,(h-w)/2);
   }

   public static AffineTransform center(int rotation, double w, double h) {
      switch (rotation) {
      case 0:  return identity();
      case 1:  return center90(w,h);
      case 2:  return center180(w,h);
      case 3:  return center270(w,h);
      default: throw new IllegalArgumentException();
      }
   }

// --- rotations holding UL corner fixed ---

   public static AffineTransform corner90(double w, double h) {
      return new AffineTransform(0,-1,1,0,0,w);
   }

   public static AffineTransform corner180(double w, double h) {
      return new AffineTransform(-1,0,0,-1,w,h);
   }

   public static AffineTransform corner270(double w, double h) {
      return new AffineTransform(0,1,-1,0,h,0);
   }

   public static AffineTransform corner(int rotation, double w, double h) {
      switch (rotation) {
      case 0:  return identity();
      case 1:  return corner90(w,h);
      case 2:  return corner180(w,h);
      case 3:  return corner270(w,h);
      default: throw new IllegalArgumentException();
      }
   }

}

