/*
 * FormatDKS3.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Op;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.SKU;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Implementation of the DKS3 format.
 */

public class FormatDKS3 extends Format {

// --- constants ---

   private static final String PREFIX_GEN = "9";
   private static final String PREFIX_PRT = "1";

   private static final String[] priorityCode = { "D", "C", "B" };
   // the spec isn't clear ... maybe these should be C-A,
   // maybe A should be ultra priority.  doesn't matter, not used.

   private static final String SOURCE_EXT = "12";

   private static final String[] surfaceCode = { "A", "B" };

   private static final char DELIM_1 = '_';
   private static final char DELIM_2 = '+';
   private static final char DELIM_3 = '.';
   private static final char DELIM_4 = '(';
   private static final char DELIM_5 = ')';

   private static final String THUMBS_DIR = "Vignettes";
   private static final String ORDER_FILE = "ORDRE.INI"; // French
   private static final String PHOTO_FILE_PREFIX = "Photo";
   private static final String PHOTO_FILE_SUFFIX = ".xml";

   private static SimpleDateFormat dateFormatDKS3 = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL, COMPLETION_MODE_AUTO }; }
   public int   getCompletionMode(Object formatConfig) { return ((DKS3Config) formatConfig).completeImmediately ? COMPLETION_MODE_AUTO : COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) { ((DKS3Config) formatConfig).completeImmediately = (mode == COMPLETION_MODE_AUTO); }

// --- filename variation ---

   // this is complicated enough that I want to optimize and not use the standard functions

   private static int indexOf(String s, char c, int n) {
      int i = -1;
      while (n-- > 0) {
         i = s.indexOf(c,i+1);
         if (i == -1) break;
      }
      return i;
   }

   private static int getNextVariant(File parent, String orderID) {

      LinkedList variants = new LinkedList();

      File[] files = parent.listFiles(new FileFilter() { public boolean accept(File file) {
         return file.isDirectory();
      } });

      for (int i=0; i<files.length; i++) {
         String s = files[i].getName();

         int i3 = s.lastIndexOf(DELIM_3);
         if (i3 == -1) continue;

         int i2 = s.lastIndexOf(DELIM_2,i3-1);
         if (i2 == -1) continue;

         int i1 = indexOf(s,DELIM_1,9); // the ninth underscore
         if (i1 == -1) continue;

         if (s.substring(i1+1,i2).equalsIgnoreCase(orderID)) variants.add(s.substring(i2+1,i3));
      }

      int n=0;
      while (variants.contains(Convert.fromInt(n))) n++;

      return n;
   }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      DKS3Config config = (DKS3Config) formatConfig;
      // because of validation, the cast is guaranteed to work

      require(config.dataDir);

      if (job.refs.isEmpty()) throw new Exception(Text.get(this,"e1"));
      SKU sku = ((Job.Ref) job.refs.getFirst()).sku;
      DKS3Mapping m = (DKS3Mapping) MappingUtil.getMapping(config.mappings,sku);
      if (m == null) missingChannel(sku,order.getFullID());

      String orderID = config.prefix + Convert.fromInt(order.orderID);

      String generateDate;
      synchronized (dateFormatDKS3) {
         generateDate = dateFormatDKS3.format(new Date());
      }

      String rest =   DELIM_1 + priorityCode[config.priority]
                    + DELIM_1 + generateDate
                    + DELIM_1 + SOURCE_EXT
                    + DELIM_1 + orderID
                    + DELIM_2 + getNextVariant(config.dataDir,orderID)
                    + DELIM_3 + Convert.fromInt(m.width)
                              + surfaceCode[m.surface]
                    + DELIM_4 + Convert.fromInt(totalQuantity(job,order,job.refs))
                    + DELIM_5;

      File root      = new File(config.dataDir,PREFIX_GEN + rest);
      File rootFinal = new File(config.dataDir,PREFIX_PRT + rest);
      LinkedList files = new LinkedList();

   // (1) plan the operation

      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(root));

      files.add(THUMBS_DIR);
      ops.add(new Op.Mkdir(new File(root,THUMBS_DIR)));
      files.add(ORDER_FILE);
      ops.add(new GenerateOrderINI(new File(root,ORDER_FILE),job,orderID,order,config,m));

      int n = 0;

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();
         if ( ! ref.sku.equals(sku) ) throw new Exception(Text.get(this,"e2",new Object[] { sku.toString(), ref.sku.toString() }));
         // check against actual SKU, just in case

         String photoFile = PHOTO_FILE_PREFIX + Convert.fromInt(n++) + PHOTO_FILE_SUFFIX;
         files.add(photoFile);
         ops.add(new GeneratePhotoXML(new File(root,photoFile),ref.filename));
         files.add(ref.filename);
         ops.add(new Op.Copy(new File(root,ref.filename),order.getPath(ref.filename)));
      }

      ops.add(new Op.Move(rootFinal,root));

   // (2) alter files

      Op.transact(ops);

   // (3) alter object

      // job.dir = rootFinal;
      // job.files = files;
      // the DKS3 takes ownership, I'm just keeping the fields available
      // in case we want to use them in the future

      if (config.completeImmediately) job.property = "";
   }

// --- INI generation ---

   private static DecimalFormat decimalFormatDKS3 = new DecimalFormat("0.000000"); // always six places
   private static final int BACKPRINT_LENGTH = 40;

   private static class GenerateOrderINI extends Op.Generate {

      private Job job;
      private String orderID;
      private Order order;
      private DKS3Config config;
      private DKS3Mapping m;

      public GenerateOrderINI(File dest, Job job, String orderID, Order order, DKS3Config config, DKS3Mapping m) {
         super(dest);
         this.job = job;
         this.orderID = orderID;
         this.order = order;
         this.config = config;
         this.m = m;
      }

      public void subdo(Writer writer) throws IOException {

         writer.write("[INFOS]" + line);
         writer.write("NUMERO=" + orderID + line);
         writer.write("INDICE=0" + line);
         writer.write("VERSION=3" + line);
         writer.write("PERSISTANCE=1" + line);

         writer.write("[CONFIG_DKS]" + line);
         writer.write("FORMAT_APS=0" + line);
         writer.write("LARG_PAPIER=" + Convert.fromInt(m.width) + line);
         writer.write("TYPE_PAPIER=" + Convert.fromInt(m.surface) + line); // numeric here, alphabetic in folder name
         writer.write("AVANCE=7" + line);
         writer.write("AVANCE_PARAM=" + Convert.fromInt(m.advance) + line);
         writer.write("ENTREE=" + SOURCE_EXT + line);
         writer.write("SORTIE=1" + line);
         writer.write("PRIO_DKS=1" + line);
         writer.write("PRIO_ORDRE_EXTERNE=" + Convert.fromInt(config.priority) + line); // numeric here, alphabetic in folder name
         writer.write("PRIO_INTERNET=1" + line);
         writer.write("PRIO_KIAS=1" + line);
         writer.write("PRIO_CONSOLE=1" + line);
         writer.write("CADRAGE=" + Convert.fromInt(m.crop) + line);
         writer.write("ETAT_IMP_DOS=" + zeroOne(Backprint.hasAnything(config.backprints)) + line);
         writer.write("RESOL_SCAN=0" + line);
         writer.write("RESOL_SCAN_PLAT=0" + line);
         writer.write("TYPE_SCANNER=255" + line);
         writer.write("TYPE_DOCUMENT_SCANNER=0" + line);
         writer.write("TYPE_DOCUMENT_SCANNER_PLAT=5" + line);
         writer.write("MEDIA_DOCUMENT_SCANNER=0" + line);
         writer.write("MEDIA_DOCUMENT_SCANNER_PLAT=1" + line);
         writer.write("FORMAT_DOCUMENT_SCANNER=5" + line);
         writer.write("FORMAT_DOCUMENT_SCANNER_PLAT=5" + line);
         writer.write("AUTO_DUST=0" + line);
         writer.write("USED_FILM=0" + line);
         writer.write("A_ARCHIVER=" + zeroOne(config.autoArchive) + line);
         writer.write("TEXTE_AU_DOS=" + line);
         writer.write("APP_GRAVURE=1" + line);
         writer.write("PERSISTENCE=0" + line);
         writer.write("SHARPNESS=10" + line);
         writer.write("SMOOTH=10" + line);
         writer.write("THRESHOLD=5" + line);

         writer.write("[CORRECT]" + line);
         writer.write("RED=0" + line);
         writer.write("GREEN=0" + line);
         writer.write("BLUE=0" + line);
         writer.write("CONTRAST=0" + line);
         writer.write("DENSITY=0" + line);
         writer.write("FLASH=0" + line);
         writer.write("SATURATION=0" + line);

         SizeRecord r = new SizeRecord();

         int n = 0;

         Iterator i = job.refs.iterator();
         while (i.hasNext()) {
            Job.Ref ref = (Job.Ref) i.next();
            Order.Item item = getOrderItem(order,ref);

            String ns   = Convert.fromInt(n  );
            String np1s = Convert.fromInt(n+1);
            n++;

            String aspectRatio;
            try {
               readWidthHeight(r,order.getPath(ref.filename));
               if (r.width <= 0 || r.height <= 0) throw new Exception(Text.get(FormatDKS3.class,"e3"));
               double d = r.width / (double) r.height;
               synchronized (decimalFormatDKS3) {
                  aspectRatio = decimalFormatDKS3.format(d);
               }
            } catch (Exception e) {
               throw new IOException(e.getMessage());
            }

            writer.write("[PHOTO" + ns + "]" + line);
            writer.write("NOM=" + PHOTO_FILE_PREFIX + ns + PHOTO_FILE_SUFFIX + line);
            writer.write("QUANTITE=" + Convert.fromInt(job.getOutputQuantity(item,ref)) + line);
            writer.write("DEMIVUE=0" + line);
            writer.write("NUMVUE=" + np1s + line);
            writer.write("COEFG=1.000000" + line);
            writer.write("NUMORDRE=" + line);
            writer.write("TYPEIMPDOS=1" + line);
            writer.write("TYPE_PHOTO=0" + line);
            if (m.border != 0) writer.write("MB=" + Convert.fromInt(m.border) + line);
            writer.write("TYPE_SCANNER=255" + line);
            writer.write("APS=0" + line);
            writer.write("ASPECT_RATIO=" + aspectRatio + line);

            int bn = 1;

            Iterator bi = config.backprints.iterator();
            while (bi.hasNext()) {
               Backprint b = (Backprint) bi.next();

               String bs = b.generate(order,item,/* sequence = */ null); // validated no sequence
               if (bs == null) bs = ""; // always generate all lines
               if (bs.length() > BACKPRINT_LENGTH) bs = bs.substring(0,BACKPRINT_LENGTH);

               writer.write("LIGIMP" + Convert.fromInt(bn++) + "=" + bs + line);
            }
         }
      }
   }

   private static String zeroOne(boolean b) { return b ? "1" : "0"; }

// --- XML generation ---

   private static final String ID_SRC = "IMAGE_USER";
   private static final String ID_DST = "IMAGE_USER_PROCESS";

   private static class GeneratePhotoXML extends Op.GenerateXML {

      private String filename;

      public GeneratePhotoXML(File dest, String filename) {
         super(dest);
         this.filename = filename;
      }

      public void subdo(Document doc) throws IOException {

         // it's excessive to do all this in order to fill in a file name,
         // but at least this guarantees the correct escaping on the name

         Node node = XML.createElement(doc,"Doc");

         Node data = XML.createElement(node,"Data");
         Node img  = XML.createElement(data,"Img");
         XML.setAttribute(img,"Pathname",filename);
         XML.setAttribute(img,"Id",ID_SRC);

         Node process = XML.createElement(node,"Process");
         XML.setAttribute(process,"Idsrc",ID_SRC);
         XML.setAttribute(process,"Iddst",ID_DST);

         Node crop = XML.createElement(process,"CROP");
         XML.setAttribute(crop,"X","0.000000%");
         XML.setAttribute(crop,"Y","0.000000%");
         XML.setAttribute(crop,"W","100.000000%");
         XML.setAttribute(crop,"H","100.000000%");

         Node rotate = XML.createElement(process,"ROTATE");
         XML.setAttribute(rotate,"ANGLE","0");

         Node adjust = XML.createElement(process,"ADJUST_COLORS");
         XML.setAttribute(adjust,"R","0");
         XML.setAttribute(adjust,"G","0");
         XML.setAttribute(adjust,"B","0");
         XML.setAttribute(adjust,"CONTRAST","0");
         XML.setAttribute(adjust,"SATURATION","0");
         XML.setAttribute(adjust,"DENSITY","0");
         XML.setAttribute(adjust,"FLASH","0");

         Node effect = XML.createElement(process,"EFFECT");
         XML.setAttribute(effect,"SHARPNESS","10");
         XML.setAttribute(effect,"SMOOTH","10");
         XML.setAttribute(effect,"THRESHOLD","5");

         Node canvas = XML.createElement(node,"Canvas");
         Node layer = XML.createElement(canvas,"Layer");
         Node image = XML.createElement(layer,"Image");
         Node imageFile = XML.createElement(image,"ImageFile");
         XML.setAttribute(imageFile,"Id",ID_DST);

         XML.addIndentation(node);
      }
   }

// --- completion ---

   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {
      return (property != null);
      // not null means job should complete immediately
   }

}

