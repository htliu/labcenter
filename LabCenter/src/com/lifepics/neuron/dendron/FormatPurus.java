/*
 * FormatPurus.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Op;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Implementation of the Purus format.
 */

public class FormatPurus extends Format {

// --- constants ---

   private static final String META_FILE = "order.xml";

   private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
   private static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_DETECT }; }
   public int   getCompletionMode(Object formatConfig) { return COMPLETION_MODE_DETECT; }
   public void  setCompletionMode(Object formatConfig, int mode) {}

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      PurusConfig config = (PurusConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      require(config.dataDir);
      require(config.statusDir); // we won't be using it right now, but better to check up front

      Convert.validateUsable(config.orderDigits,order.orderID); // a formality

      String prefix = config.prefix;
      String suffix = Convert.fromIntNDigit(config.orderDigits,order.orderID);
      if ( ! config.swap ) { // unswap, just happens to be easier to code it
         prefix = prefix + suffix;
         suffix = "";
      }

      InfixVariation iv = new InfixVariation(prefix,config.reprintDigits,suffix);

      File root = new File(config.dataDir,iv.getRootName());
      root = vary(root,iv);
      String externalID = root.getName();

   // (1) plan the operation ; also check that mappings are defined

      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(root));

      HashSet skus = new HashSet();
      HashSet fset = new HashSet();

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();
         Order.Item item = getOrderItem(order,ref);

         if (skus.add(ref.sku) && ! MappingUtil.existsMapping(config.mappings,ref.sku)) missingChannel(ref.sku,order.getFullID());

         List filenames = item.isMultiImage() ? item.filenames : Collections.singletonList(item.filename);
         if (filenames.size() > 2) throw new Exception(Text.get(this,"e4"));
         Iterator j = filenames.iterator();
         while (j.hasNext()) {
            String filename = (String) j.next();

            if (fset.add(filename)) {

               if ( ! filename.toLowerCase().endsWith(".pdf") ) throw new Exception(Text.get(this,"e2"));
               // not quite worth factoring this yet

               ops.add(new Op.Copy(new File(root,filename),order.getPath(filename)));
            }
         }

         if (item.pageCount == null) throw new Exception(Text.get(this,"e1"));
      }

      if (order.shipMethods.size() > 1) throw new Exception(Text.get(this,"e3"));
      // go ahead and check this now to avoid rollback.
      // it's not a strong requirement - only "produced here" nodes get in the list,
      // and I think the web site still doesn't support multiple ship methods for a
      // single location.
      // could also test Order.isShipToHome for the one method, but let's skip that.

      ops.add(new PurusGenerate(new File(root,META_FILE),job,order,config,externalID));
      // do this last, so hopefully Purus won't pick up the folder early

   // (2) alter files

      Op.transact(ops);

   // (3) alter object

      // Purus takes ownership of the folder
   }

// --- metafile generation ---

   private static class PurusGenerate extends Op.GenerateXML {

      private Job job;
      private Order order;
      private PurusConfig config;
      private String externalID;

      public PurusGenerate(File dest, Job job, Order order, PurusConfig config, String externalID) {
         super(dest);
         this.job = job;
         this.order = order;
         this.config = config;
         this.externalID = externalID;
      }

      public void subdo(Document doc) throws IOException {

         Node env = XML.createElement(doc,"Envelope");
         XML.createElementText(env,"InterfaceVersion","1.01.00");

         Date useDate = (order.invoiceDate != null) ? order.invoiceDate : new Date();
         // date and time are required, but invoiceDate can be null for old / local

         Node head = XML.createElement(env,"Head");
         XML.createElementText(head,"ExternalEnvelopeId",externalID);
         XML.createElementText(head,"Mandator",config.mandator);
         XML.createElementText(head,"Dealer",config.dealer);
         XML.createElementText(head,"DealerNumber",config.dealer); // yes, duplicate value
         XML.createElementText(head,"Payment","0");
         XML.createElementText(head,"Currency","USD");
         XML.createElementText(head,"OrderLocalDate",dateFormat.format(useDate));
         XML.createElementText(head,"OrderLocalTime",timeFormat.format(useDate));
         XML.createElementText(head,"CustomerLanguage","EN"); // "en-us" would be better but XSD says length 2

         Node bill = XML.createElement(head,"BillingAddress");
         generateAddress(bill,order.company,order.street1,order.street2,order.city,order.state,order.zipCode,order.country);

         Node ship = XML.createElement(head,"DeliveryAddress");
         generateAddress(ship,order.shipCompany,order.shipStreet1,order.shipStreet2,order.shipCity,order.shipState,order.shipZipCode,order.shipCountry);

         String shipMethod = "";
         if (order.shipMethods.size() > 0) { // then exactly one, by validation
            shipMethod = ((Order.ShipMethod) order.shipMethods.getFirst()).name;
         }
         XML.createElementText(ship,"ShipMethod",shipMethod);
         // assuming Order.isShipToHome is true, we could use Order.getFirstShipMethod,
         // but better not to, since it's a legacy function.

         Node list = XML.createElement(env,"Positions"); // maybe as in "imposition"?
         XML.createElementText(list,"NumberOfProducts",Convert.fromInt(job.refs.size()));
         XML.createElementText(list,"ShippingFee","0");
         XML.createElementText(list,"PromotionCode","");

         Iterator i = job.refs.iterator();
         while (i.hasNext()) {
            Job.Ref ref = (Job.Ref) i.next();
            Order.Item item = getOrderItem(order,ref);

            PurusMapping m = (PurusMapping) MappingUtil.getMapping(config.mappings,ref.sku);

            Node prod = XML.createElement(list,"Product");
            XML.createElementText(prod,"ProductId",m.productID);
            XML.createElementText(prod,"ExternalOrderId",Convert.fromInt(order.orderID)); // OK if not unique
            XML.createElementText(prod,"ProductName",m.productName);
            XML.createElementText(prod,"Impression",Convert.fromInt(job.getOutputQuantity(item,ref)));

            List filenames = item.isMultiImage() ? item.filenames : Collections.singletonList(item.filename);
            XML.createElementText(prod,"Filename",(String) filenames.get(0));
            if (filenames.size() > 1) XML.createElementText(prod,"CoverFilename",(String) filenames.get(1));

            XML.createElementText(prod,"NormalPrice","0");
            XML.createElementText(prod,"Discount","0");
            XML.createElementText(prod,"DeliveryPartNr","0");
            XML.createElementText(prod,"ProductType","PDF");
            XML.createElementText(prod,"WorkType","ALBUM");

            int pageCount = item.pageCount.intValue();
            XML.createElementText(prod,"ProductPages",Convert.fromInt(pageCount));

            XML.createElementText(prod,"skucode",denull(m.mercuryMain));

            Node comps = XML.createElement(prod,"OrderComponents");

            generateComponent(comps,"Pages",denull(m.surfaceMain),""); // same surface
            generateComponent(comps,"Cover",denull(m.surfaceMain),""); // same surface

            if (m.pageLimit != null && pageCount > m.pageLimit.intValue()) {
               Node comp = generateComponent(comps,"Additional Pages",denull(m.surfaceAdditional),denull(m.mercuryAdditional));

               // OK to hard-code the fact that pages have two sides.
               // not sure if server can produce odd page count, but
               // if it can, rounding up is correct.
               int quantity = (pageCount - m.pageLimit.intValue() + 1) / 2;
               XML.createElementText(comp,"Quantity",Convert.fromInt(quantity));
            }
         }

         XML.addIndentation(env);
      }

      private Node generateComponent(Node comps, String name, String surface, String mercury) {
         Node comp = XML.createElement(comps,"OrderComponent");
         XML.createElementText(comp,"OrderComponentName",name);
         XML.createElementText(comp,"ComponentSurfaceName",surface);
         XML.createElementText(comp,"skucode",mercury);
         return comp;
      }

      private void generateAddress(Node node, String company, String street1, String street2, String city, String state, String zipCode, String country) {

         // the XSD says these have max length 50,
         // but let's let Purus take care of that

			XML.createElementText(node,"Company",denull(company));
			XML.createElementText(node,"Address",order.email);
			XML.createElementText(node,"FirstName",order.nameFirst);
			XML.createElementText(node,"LastName",order.nameLast);
			XML.createElementText(node,"Street",join(street1,street2));
			XML.createElementText(node,"PLC",denull(zipCode));
			XML.createElementText(node,"City",denull(city));
			XML.createElementText(node,"State",denull(state));
			XML.createElementText(node,"Country",denull(country));
			XML.createElementText(node,"Phone",order.phone);
			XML.createElementText(node,"Fax","");
			XML.createElementText(node,"Mobile","");
			XML.createElementText(node,"Internet",""); // homepage
      }
   }

   private static String denull(String s) {
      return (s == null) ? "" : s;
   }

   private static String join(String s1, String s2) {
      if (s1 == null || s1.length() == 0) return denull(s2);
      if (s2 == null || s2.length() == 0) return denull(s1);
      return s1 + " " + s2;
   }

}

