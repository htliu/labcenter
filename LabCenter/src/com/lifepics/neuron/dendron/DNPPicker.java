/*
 * DNPPicker.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;

import java.util.LinkedList;

/**
 * A helper class for picking DNP printers.
 * Could go in FormatDNP, too.
 */

public class DNPPicker {

   public static class Printer {

      public int printerType;
      public String printerTypeName; // filled in by caller
      public int printerID;
      public String serialNumber;
      public String mediaLoaded;
   }

   public static LinkedList getPrinters() throws Exception {

      FormatDNP.initialize(); // handles UnsatisfiedLinkException and so forth

      byte[] data = new byte[64]; // so, limited to 32 printers on one machine
      int count = FormatDNP.checkGetPrinterPortNum(data);

      LinkedList printers = new LinkedList();

      for (int i=0; i<count; i++) {
         Printer p = new Printer();

         p.printerType = data[2*i  ];
         p.printerID   = data[2*i+1];

         try {
            p.serialNumber = FormatDNP.checkGetSerialNo(i);
         } catch (Exception e) {
            p.serialNumber = unknown;
         }

         try {
            p.mediaLoaded = DNPMedia.findMedia(FormatDNP.checkGetMedia(i)).describe();
         } catch (Exception e) {
            p.mediaLoaded = unknown;
         }

         printers.add(p);
      }

      return printers;
   }

   private static final String unknown = Text.get(DNPPicker.class,"s1");

}

