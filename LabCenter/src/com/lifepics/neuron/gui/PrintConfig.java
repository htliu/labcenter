/*
 * PrintConfig.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.w3c.dom.Node;

import java.awt.print.PageFormat;

/**
 * An object that holds configuration information for the print subsystem.
 */

public class PrintConfig extends Structure {

// --- fields ---

   // these apply only to invoice
   public int copies; // not just for auto-print, also default for manual
   public boolean showItems;
   public boolean showColor;
   public boolean productBarcodeEnable; // per-product barcode in summary
   public int productBarcodeType;

   // time zone stuff, for both
   public String timeZone;
   public String dateFormatInvoice;
   public String dateFormatLabel;

   // these apply only to label
   public boolean accountSummary; // should account label be summary form?
   public boolean labelPerQueue;
   //
   // shipping label
   public boolean showOrderNumber;
   public boolean overrideAddress;
   public String overrideName;    // all nullable if override is false, otherwise validated
   public String overrideStreet1;
   public String overrideStreet2; // except this, it can always be null
   public String overrideCity;
   public String overrideState;
   public String overrideZIP;
   public boolean markShip;
   public boolean markShipUseMessage;
   public String  markShipMessage;
   //
   // pickup label (account)
   public boolean markPay;
   public boolean markPayIncludeTax;
   public String  markPayAddMessage; // nullable
   public boolean markPro;
   public Integer markProNotMerchantID; // the merchant ID that is *not* pro
   public boolean markPre;
   public String  markPreMessage;
   public boolean showPickupLocation;

   public Setup setupInvoice;
   public Setup setupLabel;

   // fields for logo on invoice, all nullable
   public File logoFile;
   public Integer logoWidth;
   public Integer logoHeight;

   // fields for JPEG invoice process
   public boolean jpegEnableForHome;
   public boolean jpegEnableForAccount;
   public Boolean jpegDisableForLocal; // null equals false
   public boolean jpegShowColor;
   public SKU jpegSKU; // null if unspecified
   public Double jpegWidth;  // no units, just aspect ratio
   public Double jpegHeight; //
   public int jpegOrientation;
   public int jpegCompression;
   public int jpegBorderPixels;

   // fields for JPEG invoice barcode
   public boolean barcodeEnable;
   public BarcodeConfig barcodeConfig;
   public Integer barcodeWidthPixels;
   public Integer barcodeOverlapPixels;
   public String barcodePrefix;
   public boolean barcodePrefixReplace;
   public boolean barcodeIncludeTax;

   // fields for XML postage generator
   public boolean shipPrintLabel;
   public boolean shipPrintPostage;
   public File postageDir; // nullable
   public boolean postageBatchMode;
   public String postageBatchFile;
   public LinkedList postageMappings;

   // fields for product labels
   public boolean productEnableForHome;
   public boolean productEnableForAccount;

   // other fields
   public LinkedList installHints;

// --- standard date formats ---

   public static final String FORMAT_24_HR    = "yyyy-MM-dd HH:mm:ss";
   public static final String FORMAT_24_HR_TZ = "yyyy-MM-dd HH:mm:ss zzz"; // old invoice format
   public static final String FORMAT_AM_PM    = "MM-dd-yy h:mm:ss a";
   public static final String FORMAT_AM_PM_TZ = "MM-dd-yy h:mm:ss a zzz";

   public static final String INTL_24_HR    = "dd-MM-yyyy HH:mm:ss";
   public static final String INTL_24_HR_TZ = "dd-MM-yyyy HH:mm:ss zzz";
   public static final String INTL_AM_PM    = "dd-MM-yyyy h:mm:ss a";
   public static final String INTL_AM_PM_TZ = "dd-MM-yyyy h:mm:ss a zzz";

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      PrintConfig.class,
      0,new History(new int[] {21,712,22,736,23,742,24,763,25}),
      new AbstractField[] {

         new IntegerField("copies","Copies"),
         new BooleanField("showItems","ShowItems",1,true),
         new BooleanField("showColor","ShowColor",2,true),
         new BooleanField("productBarcodeEnable","ProductBarcodeEnable",19,false),
         new EnumeratedField("productBarcodeType","ProductBarcodeType",ProductBarcode.typeType,23,ProductBarcode.TYPE_CODE128_STRING) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               if (version >= 21) {
                  boolean b = Convert.toBool(getElementText(node,"ProductBarcodePrefix"));
                  tset(o,b ? ProductBarcode.TYPE_CODE128_DIGITS
                           : ProductBarcode.TYPE_CODE128_STRING);
               } else {
                  loadDefault(o);
               }
            }
            protected void tstoreSpecial(int t, Node node, Object o, int version) throws ValidationException {
               if (version >= 21) {
                  int type = tget(o);
                  boolean bf = (type == ProductBarcode.TYPE_CODE128_STRING);
                  boolean bt = (type == ProductBarcode.TYPE_CODE128_DIGITS);
                  if (bf || bt) {
                     createElementText(node,"ProductBarcodePrefix",Convert.fromBool(bt));
                     return;
                  }
                  // else fall through and let tstoreDefault throw the exception
               }
               tstoreDefault(t,o);
            }
         },

         new StringField("timeZone","TimeZone",3,"US/Mountain"),
         new StringField("dateFormatInvoice","DateFormatInvoice",11,FORMAT_AM_PM_TZ),
         new StringField("dateFormatLabel","DateFormatLabel",11,FORMAT_AM_PM),

         new BooleanField("accountSummary","AccountSummary",6,false),
         new BooleanField("labelPerQueue","LabelPerQueue",7,false),

         new BooleanField("showOrderNumber","ShowOrderNumber",8,true),
         new BooleanField("overrideAddress","OverrideAddress",8,false),
         new NullableStringField("overrideName","OverrideName",8,null),
         new NullableStringField("overrideStreet1","OverrideStreet1",8,null),
         new NullableStringField("overrideStreet2","OverrideStreet2",8,null),
         new NullableStringField("overrideCity","OverrideCity",8,null),
         new NullableStringField("overrideState","OverrideState",8,null),
         new NullableStringField("overrideZIP","OverrideZIP",8,null),
         new BooleanField("markShip","MarkShip",16,false),
         new BooleanField("markShipUseMessage","MarkShipUseMessage",16,false),
         new StringField("markShipMessage","MarkShipMessage",16,"MAIL ORDER"),

         new BooleanField("markPay","MarkPay",9,false),
         new BooleanField("markPayIncludeTax","MarkPayIncludeTax",10,false),
         new NullableStringField("markPayAddMessage","MarkPayAddMessage",10,null),
         new BooleanField("markPro","MarkPro",9,false),
         new NullableIntegerField("markProNotMerchantID","MarkProNotMerchantID",9,null),
         new BooleanField("markPre","MarkPre",16,false),
         new StringField("markPreMessage","MarkPreMessage",16,"PREPAID"),
         new BooleanField("showPickupLocation","ShowPickupLocation",22,false),

         new StructureField("setupInvoice","SetupInvoice",Setup.sd,4) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               boolean autoPrint = Convert.toBool(XML.getElementText(node,"AutoPrint"));
               tset(o,new Setup().loadDefault(autoPrint,1,PageFormat.PORTRAIT));
            }
         },
         new StructureField("setupLabel","SetupLabel",Setup.sd,4) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               tset(o,new Setup().loadDefault(false,0,PageFormat.LANDSCAPE));
            }
         },

         new NullableFileField("logoFile","LogoFile"),
         new NullableIntegerField("logoWidth","LogoWidth"),
         new NullableIntegerField("logoHeight","LogoHeight"),

         new BooleanField("jpegEnableForHome","JpegEnableForHome",5,false),
         new BooleanField("jpegEnableForAccount","JpegEnableForAccount",5,false),
         new NullableBooleanField("jpegDisableForLocal","JpegDisableForLocal",5,null),
            // this one isn't really from version 5, but it's OK, it's nullable
         new BooleanField("jpegShowColor","JpegShowColor",5,true),
         new NullableSKUField("jpegSKU","JpegSku","JpegSKU",15) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               if (version >= 5) {
                  String s = getElementText(node,oldName); // cf. StringField.loadNormal
                  tset(o,(s.length() == 0) ? null : OldSKU.decode(s)); // before, empty string was unspecified
                  // no need to check for new SKUs, this was all before that
               } else {
                  loadDefault(o);
               }
               // this field has been around since version 5,
               // but in version 15 I fixed it to be nullable.
               // later I turned it into a SKU field.
            }
         },
         new NullableDoubleField("jpegWidth","JpegWidth",5,null),
         new NullableDoubleField("jpegHeight","JpegHeight",5,null),
         new EnumeratedField("jpegOrientation","JpegOrientation",Orientation.orientationType,14,PageFormat.PORTRAIT) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               if (version >= 5) {
                  tset(o,Convert.toInt(getElementText(node,xmlName))); // IntegerField.loadNormal
               } else {
                  loadDefault(o);
               }
               // this field has been around since version 5,
               // but in version 14 I fixed it to be an enum
            }
         },
         new IntegerField("jpegCompression","JpegCompression",5,75),
         new IntegerField("jpegBorderPixels","JpegBorderPixels",13,0),

         new BooleanField("barcodeEnable","BarcodeEnable",12,false),
         new StructureField("barcodeConfig","BarcodeConfig",BarcodeConfig.sd,12),
         new NullableIntegerField("barcodeWidthPixels","BarcodeWidthPixels",12,null),
         new NullableIntegerField("barcodeOverlapPixels","BarcodeOverlapPixels",12,null),
         new StringField("barcodePrefix","BarcodePrefix",12,""),
         new BooleanField("barcodePrefixReplace","BarcodePrefixReplace",20,true),
         new BooleanField("barcodeIncludeTax","BarcodeIncludeTax",12,false),

         new BooleanField("shipPrintLabel","ShipPrintLabel",17,true),
         new BooleanField("shipPrintPostage","ShipPrintPostage",17,false),
         new NullableFileField("postageDir","PostageDir",17,null),
         new BooleanField("postageBatchMode","PostageBatchMode",18,false),
         new StringField("postageBatchFile","PostageBatchFile",18,"postage.xml"),
         new StructureListField("postageMappings","PostageMapping",PostageMapping.sd,Merge.POSITION,18,0),

         new BooleanField("productEnableForHome","ProductEnableForHome",24,false),
         new BooleanField("productEnableForAccount","ProductEnableForAccount",24,false),

         new StructureListField("installHints","InstallHint",InstallHint.sd,Merge.POSITION,25,0)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public static void validateCopies(int copies) throws ValidationException {

      if (copies < 1) throw new ValidationException(Text.get(PrintConfig.class,"e1a"));
   }

   private static void validateDateFormat(String dateFormat) throws ValidationException {
      try {
         new SimpleDateFormat(dateFormat);
      } catch (IllegalArgumentException e) {
         throw new ValidationException(Text.get(PrintConfig.class,"e16"),e);
      }
   }

   public void validate() throws ValidationException {

      if (copies < 1) throw new ValidationException(Text.get(this,"e1b"));
      // different message than validateCopies, otherwise I'd use that

      validateDateFormat(dateFormatInvoice);
      validateDateFormat(dateFormatLabel);

      if (overrideAddress) {
         if (    overrideName == null
              || overrideStreet1 == null // street2 can always be null
              || overrideCity == null
              || overrideState == null
              || overrideZIP == null ) throw new ValidationException(Text.get(this,"e13"));
      }

      if (markShip && markShipUseMessage && markShipMessage.length() == 0) throw new ValidationException(Text.get(this,"e23"));

      if (markPro && markProNotMerchantID == null) throw new ValidationException(Text.get(this,"e14"));

      if (markPre && ! markPay) throw new ValidationException(Text.get(this,"e24"));
      if (markPre && markPreMessage.length() == 0) throw new ValidationException(Text.get(this,"e25"));

      setupInvoice.validate();
      setupLabel  .validate();

   // logo

      if (logoWidth  != null && logoWidth .intValue() < 1) throw new ValidationException(Text.get(PrintConfig.class,"e7"));
      if (logoHeight != null && logoHeight.intValue() < 1) throw new ValidationException(Text.get(PrintConfig.class,"e8"));

      if ((logoWidth == null) != (logoHeight == null)) throw new ValidationException(Text.get(PrintConfig.class,"e9"));
      // if you specify one without the other,
      // the Java HTML viewer will stretch the image, not scale it.

   // JPEG invoice

      if (    jpegWidth  != null && jpegWidth .doubleValue() <= 0
           || jpegHeight != null && jpegHeight.doubleValue() <= 0 ) throw new ValidationException(Text.get(PrintConfig.class,"e10"));

      Orientation.validateOrientation(jpegOrientation);

      if (jpegCompression < 0 || jpegCompression > 100) throw new ValidationException(Text.get(PrintConfig.class,"e11"));
      // TransformConfig has a COMPRESSION_X enumeration,
      // but I don't want to deal with getting access to it from here.

      if (jpegBorderPixels < 0) throw new ValidationException(Text.get(PrintConfig.class,"e22"));

      // no enable unless required fields are there
      if (jpegEnableForHome || jpegEnableForAccount) {

         if (    jpegSKU    == null
              || jpegWidth  == null
              || jpegHeight == null ) throw new ValidationException(Text.get(PrintConfig.class,"e12"));
      }

   // barcode

      validateBarcode();

   // postage

      if ( ! (shipPrintLabel || shipPrintPostage) ) throw new ValidationException(Text.get(PrintConfig.class,"e26"));
      if (shipPrintPostage && (postageDir == null)) throw new ValidationException(Text.get(PrintConfig.class,"e27"));

      validateMappings(postageMappings); // this includes the usual subobject validation loop

   // other

      InstallHint.validate(installHints);
   }

   public void validateBarcode() throws ValidationException {

      barcodeConfig.validate();

      if (barcodeWidthPixels   != null && barcodeWidthPixels  .intValue() < 1) throw new ValidationException(Text.get(this,"e17"));
      if (barcodeOverlapPixels != null && barcodeOverlapPixels.intValue() < 0) throw new ValidationException(Text.get(this,"e18"));

      if ((jpegEnableForHome || jpegEnableForAccount) && barcodeEnable) {

         barcodeConfig.validateEnabledHard();

         if (    barcodeWidthPixels   == null
              || barcodeOverlapPixels == null ) throw new ValidationException(Text.get(this,"e19"));

         // exactly seven digits
         if ( ! (barcodePrefix.length() == 7 && isDigits(barcodePrefix)) ) throw new ValidationException(Text.get(this,"e20"));

      } else {

         // any number of digits
         if ( ! isDigits(barcodePrefix) ) throw new ValidationException(Text.get(this,"e21"));
      }
   }

   private static boolean isDigits(String s) {

      for (int i=0; i<s.length(); i++) {
         char c = s.charAt(i);
         if (c < '0' || c > '9') return false;
      }

      return true;
   }

   public void validateSoft(LinkedList list) {

      if ((jpegEnableForHome || jpegEnableForAccount) && barcodeEnable) {

         // this is only called after hard validation, so we know the fields are non-null.
         // except ... when we're in the barcode dialog, we only call validateBarcode,
         // so the jpegWidth that was read in from the UI field before the dialog started
         // is a total unknown.

         if (jpegWidth != null && jpegWidth.doubleValue() > 0) {

            // assume the JPEG width is in inches.
            // this is the only place we do that,
            // everywhere else it just gives a ratio.
            //
            double widthInches = jpegWidth.doubleValue();

            // note, barcodeWidthPixels is a poor name.
            // when a barcode is being printed,
            // it sets the total JPEG width in pixels.
            //
            int widthPixels = barcodeWidthPixels.intValue();

            double dpi = widthPixels / widthInches; // not a divide by zero, by validation
            barcodeConfig.validateEnabledSoft(dpi,list);
         }
      }
   }

// --- setup class ---

   public static class Setup extends PageSetup {

      public boolean enableForHome;
      public boolean enableForAccount;
      public Boolean disableForLocal; // null equals false

      public static final StructureDefinition sd = new StructureDefinition(

         Setup.class,
         PageSetup.sd,
         new AbstractField[] {

            new BooleanField("enableForHome","EnableForHome",0,false),       // custom loadDefault
            new BooleanField("enableForAccount","EnableForAccount",0,false), //
            new NullableBooleanField("disableForLocal","DisableForLocal",0,null),
         });

      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException {
         super.validate();

         // no other validations at the moment
      }

      public Object loadDefault(boolean enable, double margin, int orientation) {
         loadDefault(8.5,11,margin,orientation);

         enableForHome = enable;
         enableForAccount = enable;

         return this; // convenience
      }
   }

// --- postage mapping class ---

   public static class PostageMapping extends Structure {

      public String shipMethod; // for first entry, null
      public LinkedList values; //                  names

      public static final StructureDefinition sd = new StructureDefinition(

         PostageMapping.class,
         // no version
         new AbstractField[] {

            new NullableStringField("shipMethod","ShipMethod",0,null),
            new InlineListField("values","Value",0)
         });

      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException {
      }
   }

   private static void validateNames(LinkedList names) throws ValidationException {

      Pattern pattern = Pattern.compile("[a-z]+"); // one or more lowercase letters
      // the point is to check that the names are suitable for use as XSL variables

      HashSet set = new HashSet();

      Iterator i = names.iterator();
      while (i.hasNext()) {
         String name = (String) i.next();

         if ( ! pattern.matcher(name).matches() ) throw new ValidationException(Text.get(PrintConfig.class,"e32",new Object[] { name }));
         if ( ! set.add(name) ) throw new ValidationException(Text.get(PrintConfig.class,"e33",new Object[] { name }));
      }
   }

   private static void validateMappings(LinkedList mappings) throws ValidationException {

      // empty list of mappings is an allowed state

      boolean first = true;
      HashSet set = new HashSet();
      int countGoal = 0;

      Iterator i = mappings.iterator();
      while (i.hasNext()) {
         PostageMapping m = (PostageMapping) i.next();

         m.validate(); // a formality

         int countThis = m.values.size();
         if (first) {

            if (m.shipMethod != null) throw new ValidationException(Text.get(PrintConfig.class,"e28"));
            countGoal = countThis;
            validateNames(m.values);

            first = false;
         } else {

            if (m.shipMethod == null) throw new ValidationException(Text.get(PrintConfig.class,"e29"));
            // this isn't really necessary, I just like it.
            // Invoice.enumerateLabelInfo can produce null as a value,
            // but it means the label is for pickup, not shipping,
            // so we should never need to match it.  also, I like not
            // having any other mapping entry look like the first.

            if ( ! set.add(m.shipMethod) ) throw new ValidationException(Text.get(PrintConfig.class,"e30",new Object[] { m.shipMethod }));

            if (countThis != countGoal) throw new ValidationException(Text.get(PrintConfig.class,"e31"));
         }
      }
   }

   /**
    * @param shipMethod The shipping method, not null.
    * @return The postage mapping, null if none found.
    */
   public static PostageMapping findMapping(LinkedList mappings, String shipMethod) {

      Iterator i = mappings.iterator();
      while (i.hasNext()) {
         PostageMapping m = (PostageMapping) i.next();

         if (shipMethod.equals(m.shipMethod)) return m; // normally I'd say
         //  m.shipMethod.equals(shipMethod) but the first shipMethod value is null
      }

      return null;
   }

}

