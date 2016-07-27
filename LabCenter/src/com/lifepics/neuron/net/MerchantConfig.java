/*
 * MerchantConfig.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds merchant configuration information.
 */

public class MerchantConfig extends Structure {

// --- fields ---

   public int merchant;
   public String password;
   public boolean isWholesale;
   public String wholesaleMerchantName; // the flag that turns on WSC_PSEUDO

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      MerchantConfig.class,
      0,1,
      new AbstractField[] {

         new IntegerField("merchant","Merchant"),
         new PasswordField("password","Password",/* classifier = */ 0),
         new BooleanField("isWholesale","IsWholesale",1,false),
         new NullableStringField("wholesaleMerchantName","WholesaleMerchantName",0,null)
      });

   protected StructureDefinition sd() { return sd; }

// --- copy function ---

   public MerchantConfig copy() { return (MerchantConfig) CopyUtil.copy(this); }

// --- validation ---

   public void validate() throws ValidationException {
   }

}

