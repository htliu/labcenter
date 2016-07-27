/*
 * PJLHelper.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.Orientation;
import com.lifepics.neuron.gui.Print;
import com.lifepics.neuron.struct.Structure;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.SequenceInputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import java.awt.print.PageFormat;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;

/**
 * An unusual case.  For raw JPEG and raw PDF integrations,
 * I want to put the PJL generation code in one place,
 * but the fields need to have some different default values.
 * So, set up a superclass, but don't use the Structure code
 * for superclass inheritance.
 */

public abstract class PJLHelper extends Structure {

// --- fields ---

   public String printer; // null means default printer
   public String tray;    // null means default tray
   public boolean trayXerox;
   public Integer orientation; // PageFormat, not OrientationRequested
   public Integer sides;
   public Integer collate;
   public boolean sendPJL;
   public boolean sendJOB;
   public boolean sendEOJ;
   public boolean sendEOL; // final EOL
   public boolean sendUEL; // final UEL
   public LinkedList pjl; // list of other PJL strings

// --- validation ---

   public void validate() throws ValidationException {

      Orientation.validateOrientation(orientation);
      DirectEnum.validateSides(sides);
      DirectEnum.validateCollate(collate);
   }

// --- methods ---

   public void addStandardCommand(LinkedList commands, int quantity, Integer orientation) {

      if (tray != null) {
         commands.add("SET " + (trayXerox ? "XMEDIASOURCE" : "MEDIASOURCE") + "=" + tray);
      }

      // I think I don't want an orientation field for the new integration,
      // but I'm not sure, so keep the field for now, but hide the combo
      // in the UI so it stays null, and allow passing in an override here.
      if (orientation != null) {

         String s = null;

         switch (orientation.intValue()) {
         case PageFormat.PORTRAIT:           s = "PORTRAIT";    break;
         case PageFormat.LANDSCAPE:          s = "LANDSCAPE";   break;
         case PageFormat.REVERSE_LANDSCAPE:  s = "RLANDSCAPE";  break;
         }
         // some printers may allow RPORTRAIT, but PageFormat doesn't

         if (s != null) commands.add("SET ORIENTATION=" + s);
      }

      if (sides != null) {

         boolean duplex = false;
         String binding = null;

         switch (sides.intValue()) {
         case DirectEnum.SIDES_DUPLEX:  duplex = true;  binding = "LONGEDGE";   break;
         case DirectEnum.SIDES_TUMBLE:  duplex = true;  binding = "SHORTEDGE";  break;
         }
         // else SIDES_SINGLE, leave as above

         commands.add("SET DUPLEX=" + (duplex ? "ON" : "OFF"));
         if (binding != null) commands.add("SET BINDING=" + binding);
      }

      // in the Java API it makes sense to leave out the collate attribute,
      // but here we need a place to put the quantity,
      // so here "default" means "the LC default" not "the printer default".
      // and, the LC default is, no.
      boolean bcoll = (collate != null && collate.intValue() == DirectEnum.COLLATE_YES);
      //
      commands.add("SET " + (bcoll ? "QTY" : "COPIES") + "=" + Convert.fromInt(quantity));

      Iterator i = pjl.iterator();
      while (i.hasNext()) {
         String s = (String) i.next();
         if (s.length() > 0) commands.add(s);
      }
      // there are two reasons for the "length > 0" test.  one, it's possible to put
      // blank lines in between non-blank ones, and I don't want to send "@PJL "
      // or even "@PJL" in that case.  it ought to be harmless, I just don't like it.
      // two, before I made a text area for editing, there used to be a text field
      // for each existing line, and ConfigDirectPDF used to default to having three
      // empty lines so that there would be something to edit.  any old PDF queues
      // that haven't been edited since then will still have those three empty lines.
   }

   public void addLanguageCommand(LinkedList commands, String language) {

      commands.add("ENTER LANGUAGE=" + language);
   }

   public void sendToPrinter(LinkedList commands, File file, String orderID) throws Exception {
      LinkedList streams = new LinkedList();
      createStreams(streams,commands,file,orderID);
      sendToPrinter(streams);
   }

   public void createStreams(LinkedList streams, LinkedList commands, File file, String orderID) throws Exception {

      String jobName = Text.get(PJLHelper.class,"s1",new Object[] { orderID });

      streams.add(new ByteArrayInputStream(buildPrefix(commands,jobName)));
      streams.add(new FileInputStream(file));
      streams.add(new ByteArrayInputStream(buildSuffix()));
   }

   public void sendToPrinter(LinkedList streams) throws Exception {

      // not much of a member function, only uses printer,
      // but maybe we'll want to add more arguments later.

      PrintService service = (printer == null) ? PrintServiceLookup.lookupDefaultPrintService()
                                               : Print.getPrintService(printer);

      DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
      if ( ! service.isDocFlavorSupported(flavor) ) throw new Exception(Text.get(PJLHelper.class,"e2"));

      PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
      // in autosense mode, printers don't accept any attributes,
      // so we'll just leave this empty and send PJL commands instead

      SequenceInputStream stream = new SequenceInputStream(Collections.enumeration(streams));

      Doc doc = new SimpleDoc(stream,flavor,null);
      DocPrintJob printJob = service.createPrintJob();
      printJob.print(doc,pras);
      // need to do more here to guarantee that stream gets closed eventually
   }

// --- helpers ---

   private static final String EOL = "\r\n";         // CR is allowed and ignored, let's include it
   private static final String UEL = "\u001B%-12345X"; // "Universal Exit Language" escape sequence

   private byte[] buildPrefix(LinkedList commands, String jobName) {

      StringBuffer buf = new StringBuffer();
      buf.append(UEL);

      if (sendPJL) {
         buf.append("@PJL");
         buf.append(EOL);
      }

      if (sendJOB) {
         buf.append("@PJL JOB NAME=\"" + jobName + "\""); // rely on caller not to include quote in job name
         buf.append(EOL);
      }

      Iterator i = commands.iterator();
      while (i.hasNext()) {
         buf.append("@PJL ");
         buf.append((String) i.next());
         buf.append(EOL);
      }

      return buf.toString().getBytes();
   }

   private byte[] buildSuffix() {

      StringBuffer buf = new StringBuffer();
      buf.append(UEL);

      if (sendEOJ) {
         buf.append("@PJL EOJ");
         if (sendEOL) buf.append(EOL);
         if (sendUEL) buf.append(UEL);
      }

      return buf.toString().getBytes();
   }

}

