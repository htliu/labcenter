/*
 * PasswordField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.Obfuscate;

import org.w3c.dom.Node;

/**
 * A class that represents a non-null String-valued field of a structure
 * that stores itself in XML using obfuscation.
 */

public class PasswordField extends StringField {

// --- fields ---

   /**
    * A number that's used to vary the obfuscation method slightly,
    * so that you can't just copy and paste into a different field.
    */
   private int classifier;

// --- construction ---

   public PasswordField(String javaName, String xmlName, int classifier) {
      super(javaName,xmlName);
      this.classifier = classifier;
   }

   public PasswordField(String javaName, String xmlName, int classifier, int sinceVersion, String defaultValue) {
      super(javaName,xmlName,sinceVersion,defaultValue);
      this.classifier = classifier;
   }

// --- subclass hooks ---

   protected void loadNormal(Node node, Object o) throws ValidationException {
      tset(o,Obfuscate.recover(getElementText(node,xmlName),classifier));
   }

   public void store(Node node, Object o) {
      createElementText(node,xmlName,Obfuscate.hide(tget(o),classifier));
   }

}

