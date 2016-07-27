/*
 * EditSKUDialog.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.Agfa;
import com.lifepics.neuron.dendron.AgfaConstants;
import com.lifepics.neuron.dendron.AgfaMapping;
import com.lifepics.neuron.dendron.BurnMapping;
import com.lifepics.neuron.dendron.Conversion;
import com.lifepics.neuron.dendron.COS;
import com.lifepics.neuron.dendron.DKS3Mapping;
import com.lifepics.neuron.dendron.DLSMapping;
import com.lifepics.neuron.dendron.DNP;
import com.lifepics.neuron.dendron.DNPMapping;
import com.lifepics.neuron.dendron.DNPMedia;
import com.lifepics.neuron.dendron.DP2Mapping;
import com.lifepics.neuron.dendron.FujiMapping;
import com.lifepics.neuron.dendron.FujiNewMapping;
import com.lifepics.neuron.dendron.Fuji3Mapping;
import com.lifepics.neuron.dendron.HPMapping;
import com.lifepics.neuron.dendron.KodakMapping;
import com.lifepics.neuron.dendron.KonicaMapping;
import com.lifepics.neuron.dendron.LucidiomMapping;
import com.lifepics.neuron.dendron.NewProduct;
import com.lifepics.neuron.dendron.NoritsuMapping;
import com.lifepics.neuron.dendron.Order;
import com.lifepics.neuron.dendron.OrderEnum;
import com.lifepics.neuron.dendron.PatternDialog;
import com.lifepics.neuron.dendron.PatternUtil;
import com.lifepics.neuron.dendron.PixelMapping;
import com.lifepics.neuron.dendron.ProductCallback;
import com.lifepics.neuron.dendron.ProductConfig;
import com.lifepics.neuron.dendron.PurusMapping;
import com.lifepics.neuron.dendron.Queue;
import com.lifepics.neuron.dendron.QueueCombo;
import com.lifepics.neuron.dendron.QueueList;
import com.lifepics.neuron.dendron.QueueMapping;
import com.lifepics.neuron.dendron.RawJPEGMapping;
import com.lifepics.neuron.dendron.SKUComparator;
import com.lifepics.neuron.dendron.XeroxMapping;
import com.lifepics.neuron.dendron.ZBEMapping;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.Grid;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.gui.GridUtil;
import com.lifepics.neuron.gui.ScrollUtil;
import com.lifepics.neuron.gui.ViewHelper;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.BooleanComparator;
import com.lifepics.neuron.meta.EditAccessor;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NaturalComparator;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.meta.NumberStringComparator;
import com.lifepics.neuron.meta.SortUtil;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.struct.Pattern;
import com.lifepics.neuron.struct.PSKU;
import com.lifepics.neuron.struct.SKU;
import com.lifepics.neuron.table.ListView;
import com.lifepics.neuron.table.View;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;

/**
 * A big dialog for editing all SKU-related information.
 */

public class EditSKUDialog extends EditDialog {

// --- summary ---

   // this code is pretty complicated, so here's an explanation.

   // (1) the data in the grid is represented in two ways.
   //
   // first, there are the objects in the view (ListView) ...
   // normally the objects would contain all the grid data,
   // but here the objects are just simple SKUs (OldSKU or NewSKU,
   // or now Pattern as well).
   //
   // second, there are hash maps for all other columns,
   // that map SKU codes to displayable column values.
   // the displayable values are usually just more strings,
   // but for combo box columns are of type LabeledInteger.
   //
   // LabeledInteger objects implement toString so that
   // they show up correctly in rendering and in combo-ing,
   // and they have an extra data-carrying field so that
   // correct behavior doesn't depend on having unique names.

   // (2) built on top of the hash maps are grid accessors,
   // one for each type of displayable value (plus an extra
   // one for the SKU column)
   //
   // the string accessor is pretty simple; there the point
   // is just to be sure that the empty strings returned by
   // the standard cell editor are translated to non-mapped
   // items in the hash map (rather than mapped items that
   // point to empty strings)
   //
   // the LabeledInteger accessor does basically the same thing,
   // but with LabeledInteger objects, and with null represented
   // by a null object (nullObject) instead of an empty string.
   // note that the cell editor returns LabeledInteger objects
   // from editing, since that's what we put into the combo box.
   //
   // then, since we want to be able to sort, and LabeledInteger
   // isn't really suitable, there are two accessor wrappers
   // that convert LabeledIntegers to their integers and strings.

   // (3) the LabeledInteger objects appear in various enumerations.
   // for each such enumeration,
   // we need a combo box to use as a cell editor and a HashMap
   // to use to translate integers into LabeledIntegers.

   // (4) however, hash maps aren't directly seen by the dialog.
   // partly that's because the set of columns is variable,
   // but mainly it's because sometimes two columns (or more,
   // in theory) are tied to a single integration type, and
   // need to be kept together in a nice way.  so, the maps
   // are owned by adapter objects, one per fixed column and
   // one per queue that uses integration.

   // (5) then there's the dialog itself, which is hopefully
   // fairly straightforward now that all the other stuff
   // has been factored out into the sequence of things above.

// --- (1) data objects ---

   private static class LabeledInteger {

      public String integer;
      public String label;
      public LabeledInteger(String integer, String label) { this.integer = integer; this.label = label; }

      // equality works by object identity

      public String toString() { return label; }
   }

// --- (2a) accessors ---

   private static class SKUAccessorRaw implements Accessor {
      public Class getFieldClass() { return PSKU.class; }
      public Object get(Object o) { return o; }
   }

   private static class SKUAccessor implements Accessor {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return o.toString(); }
   }

   private static class StringAccessor implements EditAccessor {

      private Class fieldClass;
      private HashMap map;
      public StringAccessor(HashMap map) { this(String.class,map); }
      public StringAccessor(Class fieldClass, HashMap map) { this.fieldClass = fieldClass; this.map = map; }

      public Class getFieldClass() { return fieldClass; }

      public Object get(Object o) {
         Object value = map.get(o);
         return (value != null) ? value : ""; // value is a string, but no matter
      }

      public void put(Object o, Object value) {
         String s = ((String) value).trim(); // usually Field.get does this
         if (s.length() != 0) {
            map.put(o,s);
         } else {
            map.remove(o);
         }
      }
   }

   private static Object nullObject = new Object() {
      public String toString() { return ""; }
   };

   private static class LabeledIntegerAccessor implements EditAccessor {

      private Class fieldClass;
      private HashMap map;
      public LabeledIntegerAccessor(Class fieldClass, HashMap map) { this.fieldClass = fieldClass; this.map = map; }

      public Class getFieldClass() { return fieldClass; }

      public Object get(Object o) {
         Object value = map.get(o);
         return (value != null) ? value : nullObject;
      }

      public void put(Object o, Object value) {
         if (value != nullObject) {
            map.put(o,value);
         } else {
            map.remove(o);
         }
      }
   }

   private static class IntegerPart implements Accessor {
      private Accessor a;
      public IntegerPart(Accessor a) { this.a = a; }

      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) {
         Object value = a.get(o);
         if (value == nullObject) return null;

         LabeledInteger li = (LabeledInteger) value;
         try {
            return new Integer(Convert.toInt(li.integer));
         } catch (Exception e) {
            return null; // doesn't happen
         }
      }
   }

   private static class LabelPart implements Accessor {
      private Accessor a;
      public LabelPart(Accessor a) { this.a = a; }

      public Class getFieldClass() { return String.class; }
      public Object get(Object o) {
         Object value = a.get(o);
         if (value == nullObject) return ""; // match StringAccessor

         LabeledInteger li = (LabeledInteger) value;
         return li.label;
      }
   }

// --- (2b) flex accessor ---

   // I think you could completely replace StringAccessor and LabeledIntegerAccessor with this,
   // but it's sort of nice to have the guarantee that the data will be all one kind of object,
   // and anyway I don't want to risk breaking anything.
   // the main point is to have enumerated LabelInteger combos that allow entering other values.
   //
   private static class FlexAccessor implements EditAccessor {

      private Class fieldClass;
      private HashMap map;
      public FlexAccessor(Class fieldClass, HashMap map) { this.fieldClass = fieldClass; this.map = map; }

      public Class getFieldClass() { return fieldClass; }

      public Object get(Object o) {
         Object value = map.get(o);
         return (value != null) ? value : ""; // use empty string as null object, since user can enter
      }

      public void put(Object o, Object value) {

         if (value instanceof String) {
            String s = ((String) value).trim(); // usually Field.get does this
            value = (s.length() != 0) ? s : null;
         }

         if (value != null) { // null only comes from string code
            map.put(o,value);
         } else {
            map.remove(o);
         }
      }
   }

   // these are not a perfect inverting pair ... one mm is about 0.04 inches,
   // so when you enter a number to two places, it will round some direction.
   // on the other hand, because of that, millis -> inches -> millis should
   // be stable, which is good because millis is how it's stored in the config.

   private static final double MILLIS_PER_INCH = 25.4;
   private static final String INCHES_SYMBOL   = "\"";

   private static String fromMillis(int millis) {
      return decimalFormat.format(millis / MILLIS_PER_INCH) + INCHES_SYMBOL;
      // special values will be handled by the lookup in flexMillis
   }
   private static DecimalFormat decimalFormat = new DecimalFormat("0.##"); // cf. fromDouble

   private static int toMillis(String s) throws ValidationException {
      if (s.endsWith(INCHES_SYMBOL)) s = s.substring(0,s.length()-1);
      s = s.trim();
      int millis = (int) Math.round(Convert.toDouble(s) * MILLIS_PER_INCH);

      // not quite done, make sure they haven't manually entered one
      // of the special inch values that round in an unexpected way.
      //
      if (millis ==  83) millis =  82;
      if (millis == 121) millis = 120;

      return millis;
   }

   // I'd like to make the next two functions independent of the previous two,
   // but I can't see any way that doesn't involve way too much abstraction.

   private static Object flexMillis(HashMap labeler, int millis) {
      Object value = labeler.get(Convert.fromInt(millis));
      return (value != null) ? value : fromMillis(millis);
   }

   /**
    * @param value A value from a flex column; caller must handle null though.
    */
   private static int unflexMillis(Object value) throws ValidationException {
      return (value instanceof String) ? toMillis((String) value)
                                       : Convert.toInt(((LabeledInteger) value).integer);
   }

   private static class FlexMM implements Accessor { // cf. IntegerPart
      private Accessor a;
      public FlexMM(Accessor a) { this.a = a; }

      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) {
         Object value = a.get(o);
         if (value.equals("")) return null;

         try {
            return new Integer(unflexMillis(value));
         } catch (Exception e) {
            return null; // happens for invalid entry
         }
      }
   }

// --- (3) enumerations ---

   private interface Enumeration {

      JComboBox getCombo();
      HashMap getLabeler();
   }

   private static class StandardEnumeration implements Enumeration {

      private JComboBox combo;
      private HashMap labeler;

      protected StandardEnumeration() {
         combo = new JComboBox();
         labeler = new HashMap();
      }

      protected void addNull() { combo.addItem(nullObject); }
      protected void addNull_Flex() { combo.addItem(""); }

      protected void addItemIntKey(int integer, String key) {
         addItem(Convert.fromInt(integer),Text.get(EditSKUDialog.class,key));
      }
      protected void addItemInt(int integer, String label) {
         addItem(Convert.fromInt(integer),label);
      }
      protected void addItem(String integer, String label) {
         LabeledInteger li = new LabeledInteger(integer,label);
         combo.addItem(li);
         labeler.put(integer,li);
      }

      public JComboBox getCombo() { return combo; }
      public HashMap getLabeler() { return labeler; }
   }

   private static class QueueEnumeration extends StandardEnumeration {

      public QueueEnumeration(QueueList queueList) {
         addNull();

         Iterator i = queueList.queues.iterator();
         while (i.hasNext()) {
            Queue q = (Queue) i.next();
            addItem(q.queueID,q.name);
         }
      }
   }

   private static Object[] productSetKodak = new Object[] { "", "4x6", "4x8-greeting-card", "5x7", "8x10", "8x12" };
   //
   // this is a unusual ... externally displayed strings,
   // but their values are rigidly defined by the spec.
   // I guess the right thing to do is display them here, not put them in the text file.

   private static Object[] surfaceSetDP2 = new Object[] { "", "1", "2" };

   private static Object[] productTypeCD = new Object[] { "", "CD", "DVD" };
   // like above, these aren't just decoration, they're special values used to communicate with the burner

   private static Object[] marginValues = new Object[] { RawJPEGMapping.MARGIN_STANDARD,
                                                         RawJPEGMapping.MARGIN_OVERSIZE,
                                                         RawJPEGMapping.MARGIN_CLIPINSIDE,
                                                         RawJPEGMapping.MARGIN_BL_AUTOFIT,
                                                         RawJPEGMapping.MARGIN_BL_CROP };

   private static int[] surfaceValues = new int[] { COS.kLabDefault,
                                                    -1,
                                                    -2,
                                                    -3,
                                                    -4,
                                                    COS.kMatte,
                                                    COS.kSemiMatte,
                                                    COS.kGlossy,
                                                    COS.kSilk,
                                                    COS.kLuster,
                                                    COS.kSmoothLuster,
                                                    COS.kUltraSmoothHiLuster,
                                                    COS.kFineGrainedLuster,
                                                    COS.kDeepMatte,
                                                    COS.kHighGloss,
                                                    COS.kLaminate,
                                                    COS.kOther };

   private static int[] surfaceValuesKodak = new int[] { COS.kLabDefault,
                                                         -1,
                                                         -2,
                                                         -3,
                                                         -4,
                                                         COS.kMatte,
                                                         COS.kGlossy,
                                                         COS.kLuster };

   private static class StandardSurfaceEnumeration extends StandardEnumeration {

      protected void addSurface(int surface) {
         addItemInt(surface,OrderEnum.fromSurfaceExternal(surface));
      }
      protected void addSurfaceArray(int[] surfaceArray, boolean skip) {
         for (int i=0; i<surfaceArray.length; i++) {
            int surface = surfaceArray[i];
            if (skip && surface < 0) continue;
            addSurface(surface);
         }
      }
   }

   private static class SurfaceEnumeration extends StandardSurfaceEnumeration {

      public SurfaceEnumeration(boolean skip) {
         addNull();
         addSurfaceArray(surfaceValues,skip);
      }
   }

   private static class SurfaceEnumerationKodak extends StandardSurfaceEnumeration {

      public SurfaceEnumerationKodak() {
         addNull();
         addSurfaceArray(surfaceValuesKodak,false);
      }
   }

   private static class PrintWidthEnumeration extends StandardEnumeration {

      public PrintWidthEnumeration() {
         addNull_Flex();

         for (int i=0; i<AgfaConstants.PRINT_WIDTH_VALUES.length; i++) {
            addItemInt(AgfaConstants.PRINT_WIDTH_VALUES[i],
                       AgfaConstants.PRINT_WIDTH_NAMES[i] + INCHES_SYMBOL);
         }
      }
   }

   private static class RotationEnumeration extends StandardEnumeration {

      public RotationEnumeration() {
         addNull();

         for (int i=0; i<AgfaConstants.ROTATION_VALUES.length; i++) {
            addItemInt(AgfaConstants.ROTATION_VALUES[i],
                       Text.get(EditSKUDialog.class,"s9",new Object[] { AgfaConstants.ROTATION_NAMES[i] }));
         }
      }
   }

   private static class ImageFillEnumeration extends StandardEnumeration {

      public ImageFillEnumeration() {
         addNull();
         addItemIntKey(Agfa.IMAGE_FILL_FIT_IMAGE,  "if0");
         addItemIntKey(Agfa.IMAGE_FILL_FILL_PRINT, "if1");
         addItemIntKey(Agfa.IMAGE_FILL_FIX_LONGER, "if2");
         addItemIntKey(Agfa.IMAGE_FILL_FIX_SHORTER,"if3");
      }
   }

   private static class BorderWidthEnumeration extends StandardEnumeration {

      public BorderWidthEnumeration() {
         addNull_Flex();
         addItemInt( 6,"0.25" + INCHES_SYMBOL);
         addItemInt(13,"0.5"  + INCHES_SYMBOL);
      }
   }

   private static class ServiceEnumeration extends StandardEnumeration {

      public ServiceEnumeration() {
         addNull();
         addItemIntKey(10,"se0");
         addItemIntKey(20,"se1");
         addItemIntKey(30,"se2");
         addItemIntKey(40,"se3");
      }
   }

   private static class FinishTypeEnumeration extends StandardEnumeration {

      public FinishTypeEnumeration() {
         addNull();
         addItemIntKey(10,"ft0");
         addItemIntKey(20,"ft1");
         addItemIntKey(30,"ft2");
      }
      // this is really just a surface enumeration for Pixel Magic
   }

   private static class DKS3SurfaceEnumeration extends StandardEnumeration {

      public DKS3SurfaceEnumeration() {
         addNull();
         addItemIntKey(DKS3Mapping.SURFACE_GLOSSY,"ds0");
         addItemIntKey(DKS3Mapping.SURFACE_OTHER, "ds1");
      }
   }

   private static class DKS3WidthEnumeration extends StandardEnumeration {

      public DKS3WidthEnumeration() {
         addNull();
         addItemIntKey(DKS3Mapping.WIDTH__89_3_5,"dw0");
         addItemIntKey(DKS3Mapping.WIDTH_102_4,  "dw1");
         addItemIntKey(DKS3Mapping.WIDTH_114_4_5,"dw2");
         addItemIntKey(DKS3Mapping.WIDTH_127_5,  "dw3");
         addItemIntKey(DKS3Mapping.WIDTH_152_6,  "dw4");
         addItemIntKey(DKS3Mapping.WIDTH_178_7,  "dw5");
         addItemIntKey(DKS3Mapping.WIDTH_203_8,  "dw6");
         addItemIntKey(DKS3Mapping.WIDTH_210_8_5,"dw7");
         addItemIntKey(DKS3Mapping.WIDTH_240_9_5,"dw8");
         addItemIntKey(DKS3Mapping.WIDTH_250_10, "dw9");
         addItemIntKey(DKS3Mapping.WIDTH_305_12, "dw10");
      }
   }

   private static class DKS3AdvanceEnumeration extends StandardEnumeration {

      public DKS3AdvanceEnumeration() {
         addNull_Flex();
         addItemIntKey(102,"da1");
         addItemIntKey(114,"da2");
         addItemIntKey(127,"da3");
         addItemIntKey(152,"da4");
         addItemIntKey(178,"da5");
         addItemIntKey(203,"da6");
         addItemIntKey(210,"da7");
         addItemIntKey(240,"da8");
         addItemIntKey(250,"da9");
         addItemIntKey(305,"da10");
         addItemIntKey(457,"da11");
      }
   }
   // change the width enumeration by removing below min and adding max

   private static class DKS3CropEnumeration extends StandardEnumeration {

      public DKS3CropEnumeration() {
         addNull();
         addItemIntKey(DKS3Mapping.CROP_FULL_PAPER,  "dc0");
         addItemIntKey(DKS3Mapping.CROP_FULL_IMAGE,  "dc1");
         addItemIntKey(DKS3Mapping.CROP_AUTO_ADVANCE,"dc2");
      }
   }

   private static class DKS3BorderEnumeration extends StandardEnumeration {

      public DKS3BorderEnumeration() {
         addNull();
         addItemIntKey( 5,"db1");
         addItemIntKey( 6,"db2");
         addItemIntKey( 7,"db3");
         addItemIntKey( 8,"db4");
         addItemIntKey( 9,"db5");
         addItemIntKey(10,"db6");
         addItemIntKey( 0,"db0");
      }
   }

   private static class BooleanEnumeration extends StandardEnumeration {

      public BooleanEnumeration() {
         addNull();
         addItemIntKey(0,"bo0");
         addItemIntKey(1,"bo1");
      }
   }

   private static class DNPMediaEnumeration extends StandardEnumeration {

      public DNPMediaEnumeration() {
         addNull();
         for (int i=0; i<DNPMedia.table.length; i++) {
            DNPMedia m = DNPMedia.table[i];
            addItemInt(m.media,m.describe());
         }
      }
   }

   private static class DNPFinishEnumeration extends StandardEnumeration {

      public DNPFinishEnumeration() {
         addNull();
         addItemIntKey(DNP.FINISH_GLOSSY,"df0");
         addItemIntKey(DNP.FINISH_MATTE1,"df1");
      }
   }

// --- (4a) utilities ---

   // comparator codes
   private static final int CC_STRING  = 0;
   private static final int CC_LABEL   = 1;
   private static final int CC_INTEGER = 2;
   private static final int CC_FLEXMM  = 3;
   private static final int CC_SKU     = 4;
   private static final int CC_BOOLEAN = 5;
   private static final int CC_MARGIN  = 6;

   private static GridColumn makeColumn(String suffix, Accessor accessor, int cc, String queueID, String queueName, boolean editable) {

      Comparator comparator = null;
      switch (cc) {
      case CC_STRING:   comparator = new FieldComparator(                accessor, noCaseComparator );  break;
      case CC_LABEL:    comparator = new FieldComparator(new LabelPart  (accessor),noCaseComparator );  break;
      case CC_INTEGER:  comparator = new FieldComparator(new IntegerPart(accessor),naturalComparator);  break;
      case CC_FLEXMM:   comparator = new FieldComparator(new FlexMM     (accessor),naturalComparator);  break;
      case CC_SKU:      comparator = new FieldComparator(new SKUAccessorRaw(),SKUComparator.displayOrder);  break;
      case CC_BOOLEAN:  comparator = new FieldComparator(                accessor, booleanComparator);  break;
      case CC_MARGIN:   comparator = new FieldComparator(                accessor, marginComparator );  break;
      }
      // the CC_SKU case rudely doesn't use the passed-in accessor,
      // but it's not worth adding an argument everywhere to fix it.

      String queueSuffix = (queueName != null) ? " (" + queueName + ")" : "";

      GridColumn col = new GridColumn(Text.get   (EditSKUDialog.class,"n" + suffix) + queueSuffix,
                                      Text.getInt(EditSKUDialog.class,"w" + suffix),accessor,comparator);
      if (editable) col.editable = true;
      col.property = queueID; // names are unique too, but I don't like to rely on that
      return col;
   }

   /**
    * Given a map from PSKU to whatever, check if any of the keys matches the SKU.
    * This is the equivalent of MappingUtil.getMapping.  Usually here we want
    * to call the equivalent of getExactMapping, which is map.get(sku), but there
    * are a few times when we want the pattern-matching form.
    */
   private static boolean matchesAnyKey(HashMap map, SKU sku) {

      Iterator i = map.keySet().iterator();
      while (i.hasNext()) {
         PSKU psku = (PSKU) i.next();
         if (psku.matches(sku)) return true;
      }
      return false;
   }

   private static void transferMaps(SKU skuNew, SKU skuOld, HashMap[] maps) {

      // if the new SKU has somehow already been configured,
      // we want to leave it alone and not overwrite.
      // also, we want to check all the columns for a queue together,
      // so that we don't mix and match entries.

      // creating HashMap arrays every time is inefficient,
      // but this isn't a common operation; it can be inefficient.

      for (int i=0; i<maps.length; i++) {
         if (matchesAnyKey(maps[i],skuNew)) return;
      }

      transferMapsSimple(skuNew,skuOld,maps);
   }

   private static void transferMapsSimple(PSKU skuNew, PSKU skuOld, HashMap[] maps) {

      for (int i=0; i<maps.length; i++) {
         Object o = maps[i].get(skuOld);
         if (o != null) maps[i].put(skuNew,o);
         // else leave it empty
         // (we just checked, it was empty)
      }
   }

   private static void cleanMaps(Collection covered, HashMap[] maps) {

      // double iteration is bad form, but not worth making two functions

      Iterator j = covered.iterator();
      while (j.hasNext()) {
         PSKU psku = (PSKU) j.next();

         for (int i=0; i<maps.length; i++) {
            maps[i].remove(psku); // ignore result
         }
      }
   }

   private void findDiffs(PSKU pskuMain, Collection pskuOther, HashSet diffs, HashMap[] maps) {

      // double iteration is bad form, but not worth making two functions
      //
      // hard to know the best plan here.  this way we keep checking a PSKU even after we've found
      // a diff for it, but we only call get(pskuMain) once per map instead of once per comparison.
      // I guess I'm hoping diffs are rare.

      for (int i=0; i<maps.length; i++) {
         Object oMain = maps[i].get(pskuMain);

         Iterator j = pskuOther.iterator();
         while (j.hasNext()) {
            PSKU psku = (PSKU) j.next();

            Object oOther = maps[i].get(psku);

            // we only want to find differences where other has info that main doesn't.
            // so, oOther null is fine.
            if (oOther != null && ! oOther.equals(oMain)) diffs.add(psku);
            //
            // all the objects in the hash maps can be compared with equals.  the only
            // difficulty is that if you type a standard milli value into a flex field,
            // it will be a string rather than a labeledInteger, and won't match.
         }
      }
   }

   private static String toZeroOneString(boolean b) { return Convert.fromInt(b ? 1 : 0); }
   private static boolean fromZeroOneString(String s) throws ValidationException { return (Convert.toInt(s) != 0); }

// --- (4b) adapters ---

   private interface Adapter {

      void makeColumns(LinkedList cols, String queueID, String queueName);

      void putMappings(LinkedList mappings, HashSet skus);
      LinkedList getMappings(LinkedList skus) throws ValidationException;

      HashMap[] getHashMaps();
   }

   private static class DescriptionAdapter { // doesn't actually implement Adapter, but same idea

      private HashMap mapDescription;
      public DescriptionAdapter() {
         mapDescription = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("2",new StringAccessor(mapDescription),CC_STRING,queueID,queueName,/* editable = */ false));
      }

      public void putProductConfig(ProductConfig productConfig, final HashSet skus) {
         mapDescription.clear(); // for refresh
         productConfig.iterate(new ProductCallback() { public void f(SKU sku, String description, Long dueInterval) {
            skus.add(sku);
            mapDescription.put(sku,description);
         } });
      }
   }

   private static class QueueAdapter implements Adapter {

      private HashMap mapQueue;
      private HashMap mapBackupQueue;
      private HashMap labeler;
      private boolean enableBackup;
      private String singleQueue;
      public QueueAdapter(HashMap labeler, boolean enableBackup) {
         this.labeler = labeler;
         this.enableBackup = enableBackup;
         mapQueue = new HashMap();
         mapBackupQueue = new HashMap();

         // precompute single-queue info
         singleQueue = (labeler.size() == 1) ? (String) labeler.keySet().iterator().next() : null;
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("3", new LabeledIntegerAccessor(QueueEnumeration.class,mapQueue      ),CC_LABEL,queueID,queueName,/* editable = */ true));
         if (enableBackup)
         cols.add(makeColumn("57",new LabeledIntegerAccessor(QueueEnumeration.class,mapBackupQueue),CC_LABEL,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            QueueMapping m = (QueueMapping) i.next();

            skus.add(m.psku);
            mapQueue.put(m.psku,labeler.get(m.queueID));
            if (m.backupQueueID != null) mapBackupQueue.put(m.psku,labeler.get(m.backupQueueID));
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();

            Object value1 = mapQueue.get(psku);
            Object value2 = mapBackupQueue.get(psku);

            if (value1 != null) {
               QueueMapping m = new QueueMapping();

               m.psku = psku;
               m.queueID = ((LabeledInteger) value1).integer;
               m.backupQueueID = (value2 == null) ? null : ((LabeledInteger) value2).integer;

               mappings.add(m);
            }
            // else no entry, even if value2 is non-null, because we have no way to store it.
            // the usual solution is to throw ValidationException, but here the column
            // might be hidden, so I won't bother.  QueueList.deleteQueue has the same issue.
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapQueue, mapBackupQueue }; }

      public String getCurrentQueueID(PSKU psku) {
         // this is used to color cells in the UI, so although it resembles
         // findQueueIDBySKU, we want an exact match, not a getMapping call
         Object value = mapQueue.get(psku);
         return (value != null) ? ((LabeledInteger) value).integer : null;
      }

      public String getCurrentBackupQueueID(PSKU psku) {
         Object value = mapBackupQueue.get(psku);
         return (value != null) ? ((LabeledInteger) value).integer : null;
      }

      public String getSingleQueue() { return singleQueue; }

      public void setIfNotSet(SKU sku, String queueID) {
         if ( ! matchesAnyKey(mapQueue,sku) ) mapQueue.put(sku,labeler.get(queueID));
      }
   }

   private static class KonicaAdapter implements Adapter {

      private HashMap mapChannel;
      public KonicaAdapter() {
         mapChannel = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("4",new StringAccessor(mapChannel),CC_STRING,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            KonicaMapping m = (KonicaMapping) i.next();

            skus.add(m.psku);
            mapChannel.put(m.psku,m.channel);
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();
            Object value = mapChannel.get(psku);
            if (value != null) {
               KonicaMapping m = new KonicaMapping();

               m.psku = psku;
               m.channel = (String) value;

               mappings.add(m);
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapChannel }; }
   }

   private static class NoritsuAdapter implements Adapter {

      private HashMap mapChannel;
      public NoritsuAdapter() {
         mapChannel = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("30",new StringAccessor(mapChannel),CC_STRING,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            NoritsuMapping m = (NoritsuMapping) i.next();

            skus.add(m.psku);
            mapChannel.put(m.psku,m.channel);
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();
            Object value = mapChannel.get(psku);
            if (value != null) {
               NoritsuMapping m = new NoritsuMapping();

               m.psku = psku;
               m.channel = NoritsuMapping.normalize((String) value);

               mappings.add(m);
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapChannel }; }
   }

   private static class FujiAdapter implements Adapter {

      private HashMap mapProduct;
      private HashMap mapPrintCode;
      public FujiAdapter() {
         mapProduct   = new HashMap();
         mapPrintCode = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("5",new StringAccessor(mapProduct  ),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("6",new StringAccessor(mapPrintCode),CC_STRING,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            FujiMapping m = (FujiMapping) i.next();

            skus.add(m.psku);
            mapProduct  .put(m.psku,m.product  );
            mapPrintCode.put(m.psku,m.printCode);
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();
            Object value1 = mapProduct.get(psku);
            Object value2 = mapPrintCode.get(psku);
            if (value1 == null && value2 == null) {
               // no entry
            } else if (value1 != null && value2 != null) {
               FujiMapping m = new FujiMapping();

               m.psku = psku;
               m.product   = (String) value1;
               m.printCode = (String) value2;

               mappings.add(m);
            } else {
               throw new ValidationException(Text.get(EditSKUDialog.class,"e1",new Object[] { psku.toString() }));
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapProduct, mapPrintCode }; }
   }

   private static class FujiNewAdapter implements Adapter {

      private HashMap mapPrintCode;
      public FujiNewAdapter() {
         mapPrintCode = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("9",new StringAccessor(mapPrintCode),CC_STRING,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            FujiNewMapping m = (FujiNewMapping) i.next();

            skus.add(m.psku);
            mapPrintCode.put(m.psku,m.printCode);
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();
            Object value = mapPrintCode.get(psku);
            if (value != null) {
               FujiNewMapping m = new FujiNewMapping();

               m.psku = psku;
               m.printCode = (String) value;

               mappings.add(m);
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapPrintCode }; }
   }

   private static class DLSAdapter implements Adapter {

      private HashMap mapProduct;
      private HashMap mapSurface;
      private HashMap labeler;
      public DLSAdapter(HashMap labeler) {
         this.labeler = labeler;
         mapProduct = new HashMap();
         mapSurface = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("7",new StringAccessor(mapProduct),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("8",new LabeledIntegerAccessor(SurfaceEnumeration.class,mapSurface),CC_LABEL,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            DLSMapping m = (DLSMapping) i.next();

            skus.add(m.psku);
            mapProduct.put(m.psku,m.product);
            mapSurface.put(m.psku,labeler.get(Convert.fromInt(m.surface)));
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();
            Object value1 = mapProduct.get(psku);
            Object value2 = mapSurface.get(psku);
            if (value1 == null && value2 == null) {
               // no entry
            } else if (value1 != null && value2 != null) {
               DLSMapping m = new DLSMapping();

               m.psku = psku;
               m.product = (String) value1;
               m.surface = Convert.toInt(((LabeledInteger) value2).integer);

               mappings.add(m);
            } else {
               throw new ValidationException(Text.get(EditSKUDialog.class,"e2",new Object[] { psku.toString() }));
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapProduct, mapSurface }; }
   }

   private static class KodakAdapter implements Adapter {

      private HashMap mapProduct;
      private HashMap mapSurface;
      private HashMap mapBorder;
      private HashMap labelerSurface;
      private HashMap labelerBorder;
      public KodakAdapter(HashMap labelerSurface, HashMap labelerBorder) {
         this.labelerSurface = labelerSurface;
         this.labelerBorder = labelerBorder;
         mapProduct = new HashMap();
         mapSurface = new HashMap();
         mapBorder = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         // as with Agfa, using the adapter class as a marker
         cols.add(makeColumn("14",new StringAccessor(KodakAdapter.class,mapProduct),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("15",new LabeledIntegerAccessor(SurfaceEnumerationKodak.class,mapSurface),CC_LABEL,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("50",new LabeledIntegerAccessor(BooleanEnumeration.class,mapBorder),CC_INTEGER,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            KodakMapping m = (KodakMapping) i.next();

            skus.add(m.psku);
            mapProduct.put(m.psku,m.product);
            if (m.surface != null) mapSurface.put(m.psku,labelerSurface.get(Convert.fromInt(m.surface.intValue())));
            if (m.border  != null) mapBorder .put(m.psku,labelerBorder .get(toZeroOneString(m.border.booleanValue())));
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();
            Object value1 = mapProduct.get(psku);
            Object value2 = mapSurface.get(psku);
            Object value3 = mapBorder .get(psku);
            if (value1 == null && value2 == null && value3 == null) {
               // no entry
            } else if (value1 != null) { // surface and border can be null
               KodakMapping m = new KodakMapping();

               m.psku = psku;
               m.product = (String) value1;
               m.surface = (value2 == null) ? null : new Integer(Convert.toInt(((LabeledInteger) value2).integer));
               m.border  = (value3 == null) ? null : new Boolean(fromZeroOneString(((LabeledInteger) value3).integer));

               mappings.add(m);
            } else {
               throw new ValidationException(Text.get(EditSKUDialog.class,"e5",new Object[] { psku.toString() }));
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapProduct, mapSurface, mapBorder }; }
   }

   private static class AgfaAdapter implements Adapter {

      private HashMap mapSurface;
      private HashMap mapPrintWidth;
      private HashMap mapPrintLength;
      private HashMap mapRotation;
      private HashMap mapImageFill;
      private HashMap mapBorderWidth;
      private HashMap labelerSurface;
      private HashMap labelerPrintWidth;
      private HashMap labelerRotation;
      private HashMap labelerImageFill;
      private HashMap labelerBorderWidth;
      public AgfaAdapter(HashMap labelerSurface, HashMap labelerPrintWidth, HashMap labelerRotation, HashMap labelerImageFill, HashMap labelerBorderWidth) {
         this.labelerSurface = labelerSurface;
         this.labelerPrintWidth = labelerPrintWidth;
         this.labelerRotation = labelerRotation;
         this.labelerImageFill = labelerImageFill;
         this.labelerBorderWidth = labelerBorderWidth;
         mapSurface = new HashMap();
         mapPrintWidth = new HashMap();
         mapPrintLength = new HashMap();
         mapRotation = new HashMap();
         mapImageFill = new HashMap();
         mapBorderWidth = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         // note, using AgfaAdapter as marker class for Agfa surface enumeration,
         // since SurfaceEnumeration.class already goes to DLS enumeration.
         cols.add(makeColumn("10",new LabeledIntegerAccessor(AgfaAdapter.class,          mapSurface    ),CC_LABEL,  queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("11",new FlexAccessor(PrintWidthEnumeration.class,mapPrintWidth ),CC_FLEXMM,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("12",new FlexAccessor(PrintWidthEnumeration.class,mapPrintLength),CC_FLEXMM,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("13",new LabeledIntegerAccessor(RotationEnumeration.class,  mapRotation   ),CC_INTEGER,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("16",new LabeledIntegerAccessor(ImageFillEnumeration.class, mapImageFill  ),CC_INTEGER,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("17",new FlexAccessor(BorderWidthEnumeration.class,mapBorderWidth),CC_FLEXMM,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            AgfaMapping m = (AgfaMapping) i.next();

            skus.add(m.psku);
            mapSurface   .put(m.psku,labelerSurface.get(Convert.fromInt(m.surface)));
            mapPrintWidth.put(m.psku,flexMillis(labelerPrintWidth,m.printWidth));
            if (m.printLength != null) mapPrintLength.put(m.psku,flexMillis(labelerPrintWidth,m.printLength.intValue()));
            if (m.rotation    != null) mapRotation   .put(m.psku,labelerRotation .get(Convert.fromInt(m.rotation .intValue())));
            if (m.imageFill   != null) mapImageFill  .put(m.psku,labelerImageFill.get(Convert.fromInt(m.imageFill.intValue())));
            if (m.borderWidth != null) mapBorderWidth.put(m.psku,flexMillis(labelerBorderWidth,m.borderWidth.intValue()));
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();

            Object value1 = mapSurface    .get(psku);
            Object value2 = mapPrintWidth .get(psku);
            Object value3 = mapPrintLength.get(psku);
            Object value4 = mapRotation   .get(psku);
            Object value5 = mapImageFill  .get(psku);
            Object value6 = mapBorderWidth.get(psku);

            if (value1 == null && value2 == null && value3 == null && value4 == null && value5 == null && value6 == null) {
               // no entry
            } else if (value1 != null && value2 != null) {
               AgfaMapping m = new AgfaMapping();

               m.psku = psku;
               m.surface     = Convert.toInt(((LabeledInteger) value1).integer);
               m.printWidth  = unflexMillis(value2);
               m.printLength = (value3 == null) ? null : new Integer(unflexMillis(value3));
               m.rotation    = (value4 == null) ? null : new Integer(Convert.toInt(((LabeledInteger) value4).integer));
               m.imageFill   = (value5 == null) ? null : new Integer(Convert.toInt(((LabeledInteger) value5).integer));
               m.borderWidth = (value6 == null) ? null : new Integer(unflexMillis(value6));

               mappings.add(m);
            } else {
               throw new ValidationException(Text.get(EditSKUDialog.class,"e4",new Object[] { psku.toString() }));
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapSurface, mapPrintWidth, mapPrintLength, mapRotation, mapImageFill, mapBorderWidth }; }
   }

   private static class PixelAdapter implements Adapter {

      private HashMap mapProduct;
      private HashMap mapService;
      private HashMap mapFinishType;
      private HashMap labelerService;
      private HashMap labelerFinishType;
      public PixelAdapter(HashMap labelerService, HashMap labelerFinishType) {
         this.labelerService = labelerService;
         this.labelerFinishType = labelerFinishType;
         mapProduct = new HashMap();
         mapService = new HashMap();
         mapFinishType = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("18",new StringAccessor(mapProduct),                                       CC_STRING, queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("19",new LabeledIntegerAccessor(ServiceEnumeration.class,mapService),      CC_INTEGER,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("20",new LabeledIntegerAccessor(FinishTypeEnumeration.class,mapFinishType),CC_INTEGER,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            PixelMapping m = (PixelMapping) i.next();

            skus.add(m.psku);
            mapProduct   .put(m.psku,m.product);
            mapService   .put(m.psku,labelerService   .get(Convert.fromInt(m.service   )));
            mapFinishType.put(m.psku,labelerFinishType.get(Convert.fromInt(m.finishType)));
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();

            Object value1 = mapProduct   .get(psku);
            Object value2 = mapService   .get(psku);
            Object value3 = mapFinishType.get(psku);

            if (value1 == null && value2 == null && value3 == null) {
               // no entry
            } else if (value1 != null && value2 != null && value3 != null) {
               PixelMapping m = new PixelMapping();

               m.psku = psku;
               m.product    = (String) value1;
               m.service    = Convert.toInt(((LabeledInteger) value2).integer);
               m.finishType = Convert.toInt(((LabeledInteger) value3).integer);

               mappings.add(m);
            } else {
               throw new ValidationException(Text.get(EditSKUDialog.class,"e6",new Object[] { psku.toString() }));
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapProduct, mapService, mapFinishType }; }
   }

   private static class DP2Adapter implements Adapter {

      private HashMap mapProduct;
      private HashMap mapSurface;
      private HashMap mapPaperWidth;
      private HashMap mapAutoCrop;
      public DP2Adapter() {
         mapProduct = new HashMap();
         mapSurface = new HashMap();
         mapPaperWidth = new HashMap();
         mapAutoCrop = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("21",new StringAccessor(                 mapProduct),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("22",new StringAccessor(DP2Adapter.class,mapSurface),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("23",new StringAccessor(              mapPaperWidth),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("24",new StringAccessor(                mapAutoCrop),CC_STRING,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            DP2Mapping m = (DP2Mapping) i.next();

            skus.add(m.psku);
            mapProduct.put(m.psku,m.product);
            if (m.surface    != null) mapSurface   .put(m.psku,Convert.fromInt(m.surface.intValue()));
            if (m.paperWidth != null) mapPaperWidth.put(m.psku,m.paperWidth);
            if (m.autoCrop   != null) mapAutoCrop  .put(m.psku,m.autoCrop  );
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();

            Object value1 = mapProduct.get(psku);
            Object value2 = mapSurface.get(psku);
            Object value3 = mapPaperWidth.get(psku);
            Object value4 = mapAutoCrop.get(psku);

            if (value1 == null && value2 == null && value3 == null && value4 == null) {
               // no entry
            } else if (value1 != null) {
               DP2Mapping m = new DP2Mapping();

               m.psku = psku;
               m.product = (String) value1;
               m.surface = (value2 == null) ? null : new Integer(Convert.toInt((String) value2));
               //
               // we could use Convert.toNullableInt here, but it wouldn't be parallel
               // to the code in putMappings.  the problem is, a null in value2
               // represents a non-entry, not an entry with a value of null, so the meaning
               // of Convert.toNullableInt is slightly wrong.
               // similarly for the ones below ... we could just do a direct assignment.
               //
               m.paperWidth = (value3 == null) ? null : (String) value3;
               m.autoCrop   = (value4 == null) ? null : (String) value4;

               mappings.add(m);
            } else {
               throw new ValidationException(Text.get(EditSKUDialog.class,"e7",new Object[] { psku.toString() }));
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapProduct, mapSurface, mapPaperWidth, mapAutoCrop }; }
   }

   private static class DKS3Adapter implements Adapter {

      private HashMap mapSurface;
      private HashMap mapWidth;
      private HashMap mapAdvance;
      private HashMap mapCrop;
      private HashMap mapBorder;
      private HashMap labelerSurface;
      private HashMap labelerWidth;
      private HashMap labelerAdvance;
      private HashMap labelerCrop;
      private HashMap labelerBorder;
      public DKS3Adapter(HashMap labelerSurface, HashMap labelerWidth, HashMap labelerAdvance, HashMap labelerCrop, HashMap labelerBorder) {
         this.labelerSurface = labelerSurface;
         this.labelerWidth = labelerWidth;
         this.labelerAdvance = labelerAdvance;
         this.labelerCrop = labelerCrop;
         this.labelerBorder = labelerBorder;
         mapSurface = new HashMap();
         mapWidth = new HashMap();
         mapAdvance = new HashMap();
         mapCrop = new HashMap();
         mapBorder = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("25",new LabeledIntegerAccessor(DKS3SurfaceEnumeration.class,mapSurface),CC_LABEL,  queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("26",new LabeledIntegerAccessor(DKS3WidthEnumeration.class,  mapWidth  ),CC_INTEGER,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("27",new FlexAccessor          (DKS3AdvanceEnumeration.class,mapAdvance),CC_FLEXMM, queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("28",new LabeledIntegerAccessor(DKS3CropEnumeration.class,   mapCrop   ),CC_LABEL,  queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("29",new LabeledIntegerAccessor(DKS3BorderEnumeration.class, mapBorder ),CC_INTEGER,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            DKS3Mapping m = (DKS3Mapping) i.next();

            skus.add(m.psku);
            mapSurface.put(m.psku,labelerSurface.get(Convert.fromInt(m.surface)));
            mapWidth  .put(m.psku,labelerWidth  .get(Convert.fromInt(m.width)));
            mapAdvance.put(m.psku,flexMillis(labelerAdvance,m.advance));
            mapCrop   .put(m.psku,labelerCrop   .get(Convert.fromInt(m.crop)));
            mapBorder .put(m.psku,labelerBorder .get(Convert.fromInt(m.border)));
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();

            Object value1 = mapSurface.get(psku);
            Object value2 = mapWidth  .get(psku);
            Object value3 = mapAdvance.get(psku);
            Object value4 = mapCrop   .get(psku);
            Object value5 = mapBorder .get(psku);

            if (value1 == null && value2 == null && value3 == null && value4 == null && value5 == null) {
               // no entry
            } else if (value1 != null && value2 != null && value3 != null && value4 != null && value5 != null) {
               DKS3Mapping m = new DKS3Mapping();

               m.psku = psku;
               m.surface = Convert.toInt(((LabeledInteger) value1).integer);
               m.width   = Convert.toInt(((LabeledInteger) value2).integer);
               m.advance = unflexMillis(value3);
               m.crop    = Convert.toInt(((LabeledInteger) value4).integer);
               m.border  = Convert.toInt(((LabeledInteger) value5).integer);

               mappings.add(m);
            } else {
               throw new ValidationException(Text.get(EditSKUDialog.class,"e8",new Object[] { psku.toString() }));
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapSurface, mapWidth, mapAdvance, mapCrop, mapBorder }; }
   }

   private static class LucidiomAdapter implements Adapter {

      private HashMap mapProduct;
      private HashMap mapProductType;
      private HashMap mapWidth;
      private HashMap mapHeight;
      private HashMap mapName;
      public LucidiomAdapter() {
         mapProduct = new HashMap();
         mapProductType = new HashMap();
         mapWidth = new HashMap();
         mapHeight = new HashMap();
         mapName = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("31",new StringAccessor(mapProduct    ),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("32",new StringAccessor(mapProductType),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("33",new StringAccessor(mapWidth      ),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("34",new StringAccessor(mapHeight     ),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("35",new StringAccessor(mapName       ),CC_STRING,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            LucidiomMapping m = (LucidiomMapping) i.next();

            skus.add(m.psku);
            mapProduct.put(m.psku,m.product);
            if (m.productType != null) mapProductType.put(m.psku,m.productType);
            if (m.width  != null) mapWidth .put(m.psku,Convert.fromInt(m.width .intValue()));
            if (m.height != null) mapHeight.put(m.psku,Convert.fromInt(m.height.intValue()));
            if (m.name.length() > 0) mapName.put(m.psku,m.name);
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();

            Object value1 = mapProduct.get(psku);
            Object value2 = mapProductType.get(psku);
            Object value3 = mapWidth.get(psku);
            Object value4 = mapHeight.get(psku);
            Object value5 = mapName.get(psku);

            if (value1 == null && value2 == null && value3 == null && value4 == null && value5 == null) {
               // no entry
            } else if (value1 != null) {
               LucidiomMapping m = new LucidiomMapping();

               m.psku = psku;
               m.product = (String) value1;
               m.productType = (value2 == null) ? null : (String) value2;
               m.width  = (value3 == null) ? null : new Integer(Convert.toInt((String) value3));
               m.height = (value4 == null) ? null : new Integer(Convert.toInt((String) value4));
               m.name = (value5 == null) ? "" : (String) value5;

               mappings.add(m);
            } else {
               throw new ValidationException(Text.get(EditSKUDialog.class,"e9",new Object[] { psku.toString() }));
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapProduct, mapProductType, mapWidth, mapHeight, mapName }; }
   }

   private static class ZBEAdapter implements Adapter {

      private HashMap mapWidth;
      private HashMap mapHeight;
      private HashMap mapColorProfile;
      private HashMap mapProductPath;
      private HashMap labeler;
      public ZBEAdapter(HashMap labeler) {
         this.labeler = labeler;
         mapWidth  = new HashMap();
         mapHeight = new HashMap();
         mapColorProfile = new HashMap();
         mapProductPath  = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("36",new StringAccessor(mapWidth ),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("37",new StringAccessor(mapHeight),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("38",new LabeledIntegerAccessor(BooleanEnumeration.class,mapColorProfile),CC_INTEGER,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("39",new StringAccessor(mapProductPath),CC_STRING,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            ZBEMapping m = (ZBEMapping) i.next();

            skus.add(m.psku);
            mapWidth .put(m.psku,Convert.fromDouble(m.width ));
            mapHeight.put(m.psku,Convert.fromDouble(m.height));
            if (m.colorProfile != null) mapColorProfile.put(m.psku,labeler.get(toZeroOneString(m.colorProfile.booleanValue())));
            if (m.productPath  != null) mapProductPath .put(m.psku,m.productPath);
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();

            Object value1 = mapWidth .get(psku);
            Object value2 = mapHeight.get(psku);
            Object value3 = mapColorProfile.get(psku);
            Object value4 = mapProductPath .get(psku);

            if (value1 == null && value2 == null && value3 == null && value4 == null) {
               // no entry
            } else if (value1 != null && value2 != null) {
               ZBEMapping m = new ZBEMapping();

               m.psku = psku;
               m.width  = Convert.toDouble((String) value1);
               m.height = Convert.toDouble((String) value2);
               m.colorProfile = (value3 == null) ? null : new Boolean(fromZeroOneString(((LabeledInteger) value3).integer));
               m.productPath  = (value4 == null) ? null : (String) value4;

               mappings.add(m);
            } else {
               throw new ValidationException(Text.get(EditSKUDialog.class,"e10",new Object[] { psku.toString() }));
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapWidth, mapHeight, mapColorProfile, mapProductPath }; }
   }

   private static class Fuji3Adapter implements Adapter {

      private HashMap mapPrintCode;
      private HashMap mapSurface;
      public Fuji3Adapter() {
         mapPrintCode = new HashMap();
         mapSurface   = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("40",new StringAccessor(mapPrintCode),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("41",new StringAccessor(mapSurface  ),CC_STRING,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            Fuji3Mapping m = (Fuji3Mapping) i.next();

            skus.add(m.psku);
            mapPrintCode.put(m.psku,m.printCode);
            if (m.surface != null) mapSurface.put(m.psku,m.surface);
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();

            Object value1 = mapPrintCode.get(psku);
            Object value2 = mapSurface  .get(psku);

            if (value1 == null && value2 == null) {
               // no entry
            } else if (value1 != null) {
               Fuji3Mapping m = new Fuji3Mapping();

               m.psku = psku;
               m.printCode = (String) value1;
               m.surface = (value2 == null) ? null : (String) value2;

               mappings.add(m);
            } else {
               throw new ValidationException(Text.get(EditSKUDialog.class,"e11",new Object[] { psku.toString() }));
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapPrintCode, mapSurface }; }
   }

   private static class HPAdapter implements Adapter {

      private HashMap mapProductID;
      private HashMap mapProductName;
      private HashMap mapCrop;
      private HashMap mapEnhance;
      private HashMap mapLayoutWidth;
      private HashMap mapLayoutHeight;
      private HashMap labeler;
      public HPAdapter(HashMap labeler) {
         this.labeler = labeler;
         mapProductID = new HashMap();
         mapProductName = new HashMap();
         mapCrop = new HashMap();
         mapEnhance = new HashMap();
         mapLayoutWidth = new HashMap();
         mapLayoutHeight = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("45",new StringAccessor(mapProductID  ),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("42",new StringAccessor(mapProductName),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("43",new LabeledIntegerAccessor(BooleanEnumeration.class,mapCrop   ),CC_INTEGER,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("44",new LabeledIntegerAccessor(BooleanEnumeration.class,mapEnhance),CC_INTEGER,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("46",new StringAccessor(mapLayoutWidth ),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("47",new StringAccessor(mapLayoutHeight),CC_STRING,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            HPMapping m = (HPMapping) i.next();

            skus.add(m.psku);
            mapProductID  .put(m.psku,m.productID  );
            mapProductName.put(m.psku,m.productName);
            mapCrop   .put(m.psku,labeler.get(toZeroOneString(m.crop   )));
            mapEnhance.put(m.psku,labeler.get(toZeroOneString(m.enhance)));
            if (m.layoutWidth  != null) mapLayoutWidth .put(m.psku,m.layoutWidth );
            if (m.layoutHeight != null) mapLayoutHeight.put(m.psku,m.layoutHeight);
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();

            Object value1 = mapProductID  .get(psku);
            Object value2 = mapProductName.get(psku);
            Object value3 = mapCrop   .get(psku);
            Object value4 = mapEnhance.get(psku);
            Object value5 = mapLayoutWidth .get(psku);
            Object value6 = mapLayoutHeight.get(psku);

            if (value1 == null && value2 == null && value3 == null && value4 == null && value5 == null && value6 == null) {
               // no entry
            } else if (value1 != null && value2 != null && value3 != null && value4 != null) {
               HPMapping m = new HPMapping();

               m.psku = psku;
               m.productID   = (String) value1;
               m.productName = (String) value2;
               m.crop    = fromZeroOneString(((LabeledInteger) value3).integer);
               m.enhance = fromZeroOneString(((LabeledInteger) value4).integer);
               m.layoutWidth  = (value5 == null) ? null : (String) value5;
               m.layoutHeight = (value6 == null) ? null : (String) value6;

               mappings.add(m);
            } else {
               throw new ValidationException(Text.get(EditSKUDialog.class,"e12",new Object[] { psku.toString() }));
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapProductID, mapProductName, mapCrop, mapEnhance, mapLayoutWidth, mapLayoutHeight }; }
   }

   private static class XeroxAdapter implements Adapter {

      private HashMap mapProductID;
      private HashMap mapDescription;
      public XeroxAdapter() {
         mapProductID = new HashMap();
         mapDescription = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("48",new StringAccessor(mapProductID  ),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("49",new StringAccessor(mapDescription),CC_STRING,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            XeroxMapping m = (XeroxMapping) i.next();

            skus.add(m.psku);
            mapProductID  .put(m.psku,m.productID  );
            mapDescription.put(m.psku,m.description);
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();

            Object value1 = mapProductID  .get(psku);
            Object value2 = mapDescription.get(psku);

            if (value1 == null && value2 == null) {
               // no entry
            } else if (value1 != null && value2 != null) {
               XeroxMapping m = new XeroxMapping();

               m.psku = psku;
               m.productID   = (String) value1;
               m.description = (String) value2;

               mappings.add(m);
            } else {
               throw new ValidationException(Text.get(EditSKUDialog.class,"e13",new Object[] { psku.toString() }));
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapProductID, mapDescription }; }
   }

   private static class BurnAdapter implements Adapter {

      private HashMap mapProductType;
      public BurnAdapter() {
         mapProductType = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("52",new StringAccessor(BurnAdapter.class,mapProductType),CC_STRING,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            BurnMapping m = (BurnMapping) i.next();

            skus.add(m.psku);
            mapProductType.put(m.psku,m.productType);
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();
            Object value = mapProductType.get(psku);
            if (value != null) {
               BurnMapping m = new BurnMapping();

               m.psku = psku;
               m.productType = (String) value;

               mappings.add(m);
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapProductType }; }
   }

   private static class DNPAdapter implements Adapter {

      private HashMap mapMedia;
      private HashMap mapFinish;
      private HashMap mapMargin;
      private HashMap mapTiles;
      private HashMap labelerMedia;
      private HashMap labelerFinish;
      public DNPAdapter(HashMap labelerMedia, HashMap labelerFinish) {
         this.labelerMedia = labelerMedia;
         this.labelerFinish = labelerFinish;
         mapMedia = new HashMap();
         mapFinish = new HashMap();
         mapMargin = new HashMap();
         mapTiles = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("53",new LabeledIntegerAccessor(DNPMediaEnumeration .class,mapMedia ),CC_LABEL,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("54",new LabeledIntegerAccessor(DNPFinishEnumeration.class,mapFinish),CC_LABEL,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("55",new StringAccessor(mapMargin),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("56",new StringAccessor(mapTiles ),CC_STRING,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            DNPMapping m = (DNPMapping) i.next();

            skus.add(m.psku);
            mapMedia .put(m.psku,labelerMedia .get(Convert.fromInt(m.media)));
            mapFinish.put(m.psku,labelerFinish.get(Convert.fromInt(m.finish)));
            if (m.margin != null) mapMargin.put(m.psku,m.margin);
            if (m.tiles  != null) mapTiles .put(m.psku,m.tiles );
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();

            Object value1 = mapMedia .get(psku);
            Object value2 = mapFinish.get(psku);
            Object value3 = mapMargin.get(psku);
            Object value4 = mapTiles .get(psku);

            if (value1 == null && value2 == null && value3 == null && value4 == null) {
               // no entry
            } else if (value1 != null && value2 != null) {
               DNPMapping m = new DNPMapping();

               m.psku = psku;
               m.media  = Convert.toInt(((LabeledInteger) value1).integer);
               m.finish = Convert.toInt(((LabeledInteger) value2).integer);
               m.margin = (value3 == null) ? null : (String) value3;
               m.tiles  = (value4 == null) ? null : (String) value4;

               mappings.add(m);
            } else {
               throw new ValidationException(Text.get(EditSKUDialog.class,"e17",new Object[] { psku.toString() }));
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapMedia, mapFinish, mapMargin, mapTiles }; }
   }

   private static class PurusAdapter implements Adapter {

      private HashMap mapProductID;
      private HashMap mapProductName;
      private HashMap mapMercuryMain;
      private HashMap mapSurfaceMain;
      private HashMap mapPageLimit;
      private HashMap mapMercuryAdditional;
      private HashMap mapSurfaceAdditional;
      public PurusAdapter() {
         mapProductID = new HashMap();
         mapProductName = new HashMap();
         mapMercuryMain = new HashMap();
         mapSurfaceMain = new HashMap();
         mapPageLimit = new HashMap();
         mapMercuryAdditional = new HashMap();
         mapSurfaceAdditional = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("58",new StringAccessor(mapProductID  ),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("59",new StringAccessor(mapProductName),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("60",new StringAccessor(mapMercuryMain),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("61",new StringAccessor(mapSurfaceMain),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("62",new StringAccessor(mapPageLimit),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("63",new StringAccessor(mapMercuryAdditional),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("64",new StringAccessor(mapSurfaceAdditional),CC_STRING,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            PurusMapping m = (PurusMapping) i.next();

            skus.add(m.psku);
            mapProductID  .put(m.psku,m.productID  );
            mapProductName.put(m.psku,m.productName);
            if (m.mercuryMain != null) mapMercuryMain.put(m.psku,m.mercuryMain);
            if (m.surfaceMain != null) mapSurfaceMain.put(m.psku,m.surfaceMain);
            if (m.pageLimit != null) mapPageLimit.put(m.psku,Convert.fromInt(m.pageLimit.intValue()));
            if (m.mercuryAdditional != null) mapMercuryAdditional.put(m.psku,m.mercuryAdditional);
            if (m.surfaceAdditional != null) mapSurfaceAdditional.put(m.psku,m.surfaceAdditional);
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();

            Object value1 = mapProductID  .get(psku);
            Object value2 = mapProductName.get(psku);
            Object value3 = mapMercuryMain.get(psku);
            Object value4 = mapSurfaceMain.get(psku);
            Object value5 = mapPageLimit.get(psku);
            Object value6 = mapMercuryAdditional.get(psku);
            Object value7 = mapSurfaceAdditional.get(psku);

            if (value1 == null && value2 == null && value3 == null && value4 == null && value5 == null && value6 == null && value7 == null) {
               // no entry
            } else if (value1 != null && value2 != null) {
               PurusMapping m = new PurusMapping();

               m.psku = psku;
               m.productID   = (String) value1;
               m.productName = (String) value2;
               m.mercuryMain = (value3 == null) ? null : (String) value3;
               m.surfaceMain = (value4 == null) ? null : (String) value4;
               m.pageLimit = (value5 == null) ? null : new Integer(Convert.toInt((String) value5));
               m.mercuryAdditional = (value6 == null) ? null : (String) value6;
               m.surfaceAdditional = (value7 == null) ? null : (String) value7;

               mappings.add(m);
            } else {
               throw new ValidationException(Text.get(EditSKUDialog.class,"e18",new Object[] { psku.toString() }));
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapProductID, mapProductName, mapMercuryMain, mapSurfaceMain, mapPageLimit, mapMercuryAdditional, mapSurfaceAdditional }; }
   }

   private static Comparator marginComparator = new Comparator() { // goes with RawJPEGAdapter
      public int compare(Object o1, Object o2) {

         // the values being compared are either String or Margin, never null.
         // the desired order is strings first since they're "Standard", then
         // the margin objects.

         if (o1 instanceof String) {
            if (o2 instanceof String) { // String . String
               return NumberStringComparator.compareIgnoreCase((String) o1,(String) o2);
            } else {                    // String < Margin
               return -1;
            }
         } else {
            if (o2 instanceof String) { // Margin > String
               return 1;
            } else {                    // Margin . Margin
               return ((RawJPEGMapping.Margin) o1).index - ((RawJPEGMapping.Margin) o2).index;
            }
         }
      }
   };

   private static class RawJPEGAdapter implements Adapter {

      private HashMap mapWidth;
      private HashMap mapHeight;
      private HashMap mapMargin;
      public RawJPEGAdapter() {
         mapWidth  = new HashMap();
         mapHeight = new HashMap();
         mapMargin = new HashMap();
      }

      public void makeColumns(LinkedList cols, String queueID, String queueName) {
         cols.add(makeColumn("65",new StringAccessor(mapWidth ),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("66",new StringAccessor(mapHeight),CC_STRING,queueID,queueName,/* editable = */ true));
         cols.add(makeColumn("67",new FlexAccessor(RawJPEGAdapter.class,mapMargin),CC_MARGIN,queueID,queueName,/* editable = */ true));
      }

      public void putMappings(LinkedList mappings, HashSet skus) {
         Iterator i = mappings.iterator();
         while (i.hasNext()) {
            RawJPEGMapping m = (RawJPEGMapping) i.next();

            skus.add(m.psku);
            mapWidth .put(m.psku,Convert.fromDouble(m.width ));
            mapHeight.put(m.psku,Convert.fromDouble(m.height));

            if (m.margin != null) {
               try {
                  mapMargin.put(m.psku,RawJPEGMapping.combo(m.margin));
               } catch (ValidationException e) {
                  // can't happen
               }
            }
         }
      }

      public LinkedList getMappings(LinkedList skus) throws ValidationException {
         LinkedList mappings = new LinkedList();

         Iterator i = skus.iterator();
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();

            Object value1 = mapWidth .get(psku);
            Object value2 = mapHeight.get(psku);
            Object value3 = mapMargin.get(psku);

            if (value1 == null && value2 == null && value3 == null) {
               // no entry
            } else if (value1 != null && value2 != null) {
               RawJPEGMapping m = new RawJPEGMapping();

               m.psku = psku;
               m.width  = Convert.toDouble((String) value1);
               m.height = Convert.toDouble((String) value2);

               if (value3 == null) {
                  m.margin = null;
               } else if (value3 instanceof String) {
                  m.margin = (String) value3;
                  // sloppy but harmless, allow entry of actual enum values.
                  // if STANDARD is entered it will be caught in validation.
               } else {
                  m.margin = ((RawJPEGMapping.Margin) value3).enumText;
               }

               mappings.add(m);
            } else {
               throw new ValidationException(Text.get(EditSKUDialog.class,"e19",new Object[] { psku.toString() }));
            }
         }

         return mappings;
      }

      public HashMap[] getHashMaps() { return new HashMap[] { mapWidth, mapHeight, mapMargin }; }
   }

// --- (5) the dialog ---

// --- fields ---

   // initialized in constructor

   private EditSKUData data;
   private String skuRefreshURL;
   private String newProductURL;
   private String conversionURL;
   private MerchantConfig merchantConfig;

   private Enumeration queueEnumeration;
   private Enumeration surfaceEnumeration;
   private Enumeration surfaceEnumerationKodak;
   private Enumeration surfaceEnumerationAgfa;
   private Enumeration printWidthEnumeration;
   private Enumeration rotationEnumeration;
   private Enumeration imageFillEnumeration;
   private Enumeration borderWidthEnumeration;
   private Enumeration serviceEnumeration;
   private Enumeration finishTypeEnumeration;
   private Enumeration dks3SurfaceEnumeration;
   private Enumeration dks3WidthEnumeration;
   private Enumeration dks3AdvanceEnumeration;
   private Enumeration dks3CropEnumeration;
   private Enumeration dks3BorderEnumeration;
   private Enumeration booleanEnumeration;
   private Enumeration dnpMediaEnumeration;
   private Enumeration dnpFinishEnumeration;

   // initialized in constructFields

   private DescriptionAdapter descriptionAdapter;
   private QueueAdapter queueAdapter; // declare as QueueAdapter to access special functions
   // other adapters in data object

   private ArrayList rows; // (*) contains SKUs
   private ListView view;
   private ViewHelper viewHelper;
   private GridUtil.Sorter sorter;
   private JTable tableFixed;

   private JLabel defaultQueueLabel;
   private QueueCombo defaultQueue;
   private JCheckBox showAllCodes;

   // initialized in prepare/put

   private LinkedList skusAll;
   private LinkedList skusActive;
   // (*) rows is filled in here

// --- construction ---

   public EditSKUDialog(Dialog owner, EditSKUData data, QueueList queueList, SKU forceSKU, String skuRefreshURL, String newProductURL, String conversionURL, MerchantConfig merchantConfig) {
      super(owner,Text.get(EditSKUDialog.class,"s1"));

      this.data = data;
      this.skuRefreshURL = skuRefreshURL;
      this.newProductURL = newProductURL;
      this.conversionURL = conversionURL;
      this.merchantConfig = merchantConfig;

      queueEnumeration = new QueueEnumeration(queueList);
      surfaceEnumeration = new SurfaceEnumeration(false);
      surfaceEnumerationKodak = new SurfaceEnumerationKodak();
      surfaceEnumerationAgfa = new SurfaceEnumeration(true);
      printWidthEnumeration = new PrintWidthEnumeration();
      rotationEnumeration = new RotationEnumeration();
      imageFillEnumeration = new ImageFillEnumeration();
      borderWidthEnumeration = new BorderWidthEnumeration();
      serviceEnumeration = new ServiceEnumeration();
      finishTypeEnumeration = new FinishTypeEnumeration();
      dks3SurfaceEnumeration = new DKS3SurfaceEnumeration();
      dks3WidthEnumeration = new DKS3WidthEnumeration();
      dks3AdvanceEnumeration = new DKS3AdvanceEnumeration();
      dks3CropEnumeration = new DKS3CropEnumeration();
      dks3BorderEnumeration = new DKS3BorderEnumeration();
      booleanEnumeration = new BooleanEnumeration();
      dnpMediaEnumeration = new DNPMediaEnumeration();
      dnpFinishEnumeration = new DNPFinishEnumeration();

      construct(constructFields(queueList,forceSKU),/* readonly = */ false,/* resizable = */ true);
   }

// --- methods ---

   private Adapter getAdapter(int format) { // not static because of enumerations
      switch (format) {
      case Order.FORMAT_NORITSU:  return new NoritsuAdapter();
      case Order.FORMAT_KONICA:   return new KonicaAdapter();
      case Order.FORMAT_FUJI:     return new FujiAdapter();
      case Order.FORMAT_FUJI_NEW: return new FujiNewAdapter();
      case Order.FORMAT_DLS:      return new DLSAdapter(surfaceEnumeration.getLabeler());
      case Order.FORMAT_KODAK:    return new KodakAdapter(surfaceEnumerationKodak.getLabeler(),
                                                          booleanEnumeration.getLabeler());
      case Order.FORMAT_AGFA:     return new AgfaAdapter(surfaceEnumerationAgfa.getLabeler(),
                                                         printWidthEnumeration.getLabeler(),
                                                         rotationEnumeration.getLabeler(),
                                                         imageFillEnumeration.getLabeler(),
                                                         borderWidthEnumeration.getLabeler());
      case Order.FORMAT_LUCIDIOM: return new LucidiomAdapter();
      case Order.FORMAT_PIXEL:    return new PixelAdapter(serviceEnumeration.getLabeler(),
                                                          finishTypeEnumeration.getLabeler());
      case Order.FORMAT_DP2:      return new DP2Adapter();
      case Order.FORMAT_DKS3:     return new DKS3Adapter(dks3SurfaceEnumeration.getLabeler(),
                                                         dks3WidthEnumeration.getLabeler(),
                                                         dks3AdvanceEnumeration.getLabeler(),
                                                         dks3CropEnumeration.getLabeler(),
                                                         dks3BorderEnumeration.getLabeler());
      case Order.FORMAT_ZBE:      return new ZBEAdapter(booleanEnumeration.getLabeler());
      case Order.FORMAT_FUJI3:    return new Fuji3Adapter();
      case Order.FORMAT_HP:       return new HPAdapter(booleanEnumeration.getLabeler());
      case Order.FORMAT_XEROX:    return new XeroxAdapter();
      case Order.FORMAT_BURN:     return new BurnAdapter();
      case Order.FORMAT_DNP:      return new DNPAdapter(dnpMediaEnumeration.getLabeler(),dnpFinishEnumeration.getLabeler());
      case Order.FORMAT_PURUS:    return new PurusAdapter();
      case Order.FORMAT_RAW_JPEG: return new RawJPEGAdapter();
      default:                    throw new IllegalArgumentException();
      }
   }

   private JPanel constructFields(QueueList queueList, SKU forceSKU) {

   // columns

      LinkedList colsFixed = new LinkedList();
      LinkedList cols = new LinkedList();

      GridColumn skuColumn = makeColumn("1",new SKUAccessor(),CC_SKU,null,null,/* editable = */ false);
      colsFixed.add(skuColumn);

      descriptionAdapter = new DescriptionAdapter();
      queueAdapter = new QueueAdapter(queueEnumeration.getLabeler(),queueList.enableBackup);
      //
      descriptionAdapter.makeColumns(cols,null,null);
      cols.add(makeColumn("51",new RuleAccessor(),CC_BOOLEAN,null,null,/* editable = */ true));
      int index1 = cols.size();
      queueAdapter.makeColumns(cols,null,null);
      int index2 = cols.size();

      Iterator i = data.mappingQueues.iterator();
      while (i.hasNext()) {
         EditSKUData.MappingQueue mq = (EditSKUData.MappingQueue) i.next();

         mq.adapter = getAdapter(mq.format);
         ((Adapter) mq.adapter).makeColumns(cols,mq.queueID,mq.name);
      }

      GridColumn[] colsFixedArray = (GridColumn[]) colsFixed.toArray(new GridColumn[colsFixed.size()]);
      GridColumn[] colsArray = (GridColumn[]) cols.toArray(new GridColumn[cols.size()]);

   // column auto-resize

      boolean sa = prepare(forceSKU); // this is really part of put, but I've split it out
      // and pulled it forward through EditDialog.construct to here.

      int max = 0;

      i = skusAll.iterator();
      while (i.hasNext()) {
         PSKU psku = (PSKU) i.next();
         int len = psku.toString().length();
         if (len > max) max = len;
      }

      // scale back a bit since most characters aren't as wide as uppercase M
      max = max * 3 / 4;

      // prevent it from taking over whole screen when there are many attribs
      double limit = ScrollUtil.getWidth(0.4); // 40% vs. the 90% from ScrollUtil.setSizeToLarge
      limit /= GridUtil.getScale(); // width is in pixels, convert to columns
      if (max > limit) max = (int) Math.round(limit); // could be no-op under certain conditions

      if (max > skuColumn.width) skuColumn.width = max;

   // the grid (without row data yet)

      rows = new ArrayList();

      int c1 = Text.getInt(this,"c1"); // default row count
      view = new ListView(rows,SKUComparator.displayOrder);
      MagicGrid magicGrid = new MagicGrid(view,colsArray,index1,index2);
      viewHelper = new ViewHelper(view,c1,colsArray,null,magicGrid);
      magicGrid.table = viewHelper.getTable(); // awkward construction

      // now undo some of the defaults
      viewHelper.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // not just for user resize!
      GridUtil.setShowGrid(viewHelper.getTable()); // different look for editable data
      viewHelper.getScrollPane().setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      viewHelper.fixHeaderBackground();

      // EditDialog finds the minimum window size by packing to the preferred size.
      // but, the preferred size computed above assumes there's no horizontal scrolling,
      // and here is often way too large -- which of course is why I'm adding scrolling.
      // so, we need to turn it down artifically, just being sure to leave enough
      // room for the horizontal scroll bar.  d3 is an arbitrary number that does that.
      //
      Dimension d = viewHelper.getTable().getPreferredScrollableViewportSize();
      viewHelper.getTable().setPreferredScrollableViewportSize(new Dimension(Text.getInt(this,"d3"),d.height));

      sorter = viewHelper.makeSortable(view);

   // the fixed area

      // this is similar to what ViewHelper does.  we could probably even
      // use another ViewHelper and ignore the scroll pane, but I don't want
      // to risk interfering with the weird JTable-JScrollPane communication

      // (*3) the two marked items let us resize the fixed columns.
      // I'm not totally clear on the details, but here's what I know.
      //
      // first, if auto-resize is on, the total width of the table
      // is held constant, so you have to turn that off.
      // once you've done that, resizing a column changes the table's
      // preferred size, just as you'd expect.
      //
      // in spite of that, revalidating the scroll pane doesn't make
      // the fixed area change size.  why not?  it turns out it's
      // because the table lives in a JViewport using ViewportLayout,
      // and the layout sees that the table is Scrollable and uses
      // the preferred scrollable viewport size instead just of the
      // preferred size.  and, the scrollable viewport size is just
      // a constant that we get to set, not wired up to anything.
      //
      // how to fix it?  well, we could hide the table inside a panel
      // to break the communication (cf. HTMLViewer), and indeed
      // that seems to work, but we do still have vertical scrolling
      // going on, and since I don't understand the communication
      // it seems better not to mess with it.  also there was some issue
      // with panel borders.
      //
      // so, alternate solution, I do the JTable function override below
      // to make the scrollable viewport width track the preferred width.
      //
      // after that, I thought I'd need to set up a trigger to make
      // changes to the column widths revalidate the scroll pane,
      // but no, it's all in Java already, everything magically works.

      Grid gridFixed = new Grid(view,colsFixedArray);

      tableFixed = new JTable(gridFixed) {
         public Dimension getPreferredScrollableViewportSize() { // (*3)
            return new Dimension(getPreferredSize().width,preferredViewportSize.height);
         }
      };
      GridUtil.setPreferredSize(tableFixed,gridFixed,c1);

      tableFixed.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // (*3)
      tableFixed.setColumnSelectionAllowed(false);
      tableFixed.getTableHeader().setReorderingAllowed(false);
      GridUtil.setShowGrid(tableFixed);
      // we can't set the background color yet, see below

      GridUtil.addToExistingSorter(sorter,tableFixed,gridFixed);

      // now do some things that aren't so similar

      viewHelper.getScrollPane().setRowHeaderView(tableFixed);
      viewHelper.getScrollPane().setCorner(JScrollPane.UPPER_LEFT_CORNER,tableFixed.getTableHeader());

      tableFixed.setSelectionModel(viewHelper.getTable().getSelectionModel()); // share

      // set the background color -- this can't happen until the row header view is set
      viewHelper.getScrollPane().getRowHeader().setBackground(tableFixed.getBackground());

   // custom editors

      viewHelper.getTable().setDefaultRenderer(Object.class,new QueueRenderer(view,queueAdapter,colsArray,queueList.enableBackup));
      // can't use ViewHelper.colorize, it determines color by row only

      viewHelper.getTable().setDefaultEditor(QueueEnumeration.class,new DefaultCellEditor(queueEnumeration.getCombo()));
      //
      viewHelper.getTable().setDefaultEditor(SurfaceEnumeration.class,new DefaultCellEditor(surfaceEnumeration.getCombo()));
      viewHelper.getTable().setDefaultEditor(SurfaceEnumerationKodak.class,new DefaultCellEditor(surfaceEnumerationKodak.getCombo()));
      viewHelper.getTable().setDefaultEditor(AgfaAdapter.class,new DefaultCellEditor(surfaceEnumerationAgfa.getCombo()));
      //
      JComboBox combo = printWidthEnumeration.getCombo();
      combo.setEditable(true);
      viewHelper.getTable().setDefaultEditor(PrintWidthEnumeration.class,new DefaultCellEditor(combo));
      //
      viewHelper.getTable().setDefaultEditor(RotationEnumeration.class,new DefaultCellEditor(rotationEnumeration.getCombo()));
      viewHelper.getTable().setDefaultEditor(ImageFillEnumeration.class,new DefaultCellEditor(imageFillEnumeration.getCombo()));
      //
      combo = borderWidthEnumeration.getCombo();
      combo.setEditable(true);
      viewHelper.getTable().setDefaultEditor(BorderWidthEnumeration.class,new DefaultCellEditor(combo));
      //
      viewHelper.getTable().setDefaultEditor(ServiceEnumeration.class,new DefaultCellEditor(serviceEnumeration.getCombo()));
      viewHelper.getTable().setDefaultEditor(FinishTypeEnumeration.class,new DefaultCellEditor(finishTypeEnumeration.getCombo()));
      viewHelper.getTable().setDefaultEditor(DKS3SurfaceEnumeration.class,new DefaultCellEditor(dks3SurfaceEnumeration.getCombo()));
      viewHelper.getTable().setDefaultEditor(DKS3WidthEnumeration.class,new DefaultCellEditor(dks3WidthEnumeration.getCombo()));
      //
      combo = dks3AdvanceEnumeration.getCombo();
      combo.setEditable(true);
      viewHelper.getTable().setDefaultEditor(DKS3AdvanceEnumeration.class,new DefaultCellEditor(combo));
      //
      viewHelper.getTable().setDefaultEditor(DKS3CropEnumeration.class,new DefaultCellEditor(dks3CropEnumeration.getCombo()));
      viewHelper.getTable().setDefaultEditor(DKS3BorderEnumeration.class,new DefaultCellEditor(dks3BorderEnumeration.getCombo()));
      viewHelper.getTable().setDefaultEditor(BooleanEnumeration.class,new DefaultCellEditor(booleanEnumeration.getCombo()));
      viewHelper.getTable().setDefaultEditor(DNPMediaEnumeration.class,new DefaultCellEditor(dnpMediaEnumeration.getCombo()));
      viewHelper.getTable().setDefaultEditor(DNPFinishEnumeration.class,new DefaultCellEditor(dnpFinishEnumeration.getCombo()));
      //
      // the class values are supposed to tell the type of data in the column,
      // but here they're just codes to let the table select the right editor.

      combo = new JComboBox(productSetKodak);
      combo.setEditable(true);
      viewHelper.getTable().setDefaultEditor(KodakAdapter.class,new DefaultCellEditor(combo));
      // special case, a string-valued combo box

      combo = new JComboBox(surfaceSetDP2);
      combo.setEditable(true);
      viewHelper.getTable().setDefaultEditor(DP2Adapter.class,new DefaultCellEditor(combo));
      // and another one

      combo = new JComboBox(productTypeCD);
      combo.setEditable(true);
      viewHelper.getTable().setDefaultEditor(BurnAdapter.class,new DefaultCellEditor(combo));
      // and another one

      combo = new JComboBox(marginValues);
      combo.setEditable(true);
      viewHelper.getTable().setDefaultEditor(RawJPEGAdapter.class,new DefaultCellEditor(combo));
      // the strangest one yet, a string-valued combo box with non-LabeledInteger enum values

   // normal construction

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      JButton buttonRefresh = new JButton(Text.get(this,"s2"));
      buttonRefresh.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { refresh(); } });

      JButton buttonAddRule = new JButton(Text.get(this,"s10"));
      buttonAddRule.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { addRule(/* psku = */ null,/* auto = */ false); } });

      JButton buttonRemoveRule = new JButton(Text.get(this,"s12"));
      buttonRemoveRule.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { removeRule(/* psku = */ null); } });

      defaultQueueLabel = new JLabel(Text.get(this,"s3") + ' ');
      defaultQueue = new QueueCombo(queueList,Text.get(this,"s4"),null);

      showAllCodes = new JCheckBox(Text.get(this,"s5"));
      Field.put(showAllCodes,sa);
      showAllCodes.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { showAll(); } });

      int d1 = Text.getInt(this,"d1");
      int d4 = Text.getInt(this,"d4");

      helper.add(0,0,buttonRefresh);
      helper.add(1,0,Box.createHorizontalStrut(d1));
      helper.add(2,0,buttonAddRule);
      helper.add(3,0,Box.createHorizontalStrut(d4));
      helper.add(4,0,buttonRemoveRule);
      if (data.defaultQueue != null) helper.add(5,0,Box.createHorizontalStrut(d4));
      helper.add(6,0,defaultQueueLabel);
      helper.add(7,0,defaultQueue);
      helper.add(8,0,Box.createHorizontalStrut(d1));
      helper.add(9,0,showAllCodes);

      helper.add(0,1,Box.createVerticalStrut(Text.getInt(this,"d2")));

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridwidth = 10;
      helper.add(0,2,viewHelper.getScrollPane(),constraints);

      helper.setRowWeight(2,1);
      helper.setColumnWeight(1,1);
      helper.setColumnWeight(8,1);

      if (data.defaultQueue == null) hideDefaultQueue(); // see (**) below

      return fields;
   }

   protected void setCustomSize() {
      ScrollUtil.setSizeToLarge(this);
   }

   private void hideDefaultQueue() {
      defaultQueueLabel.setVisible(false);
      defaultQueue     .setVisible(false);
   }

   private boolean prepare(SKU forceSKU) {
      HashSet setAll = new HashSet();

      descriptionAdapter.putProductConfig(data.productConfig,setAll);
      HashSet setActive = (HashSet) setAll.clone();

      queueAdapter.putMappings(data.mappings,setAll);
      // descriptionAdapter would usually go here

      Iterator i = data.mappingQueues.iterator();
      while (i.hasNext()) {
         EditSKUData.MappingQueue mq = (EditSKUData.MappingQueue) i.next();

         ((Adapter) mq.adapter).putMappings(mq.mappings,setAll);
      }

      LinkedList patterns = PatternUtil.getPatterns(setAll);
      setActive.addAll(PatternUtil.getActive(patterns,data.productConfig));
      // make active patterns also show up in the active set

      LinkedList covered = PatternUtil.getCovered(patterns,setAll); // setAll contains setActive
      clean(covered);
      setAll   .removeAll(covered);
      setActive.removeAll(covered);

      boolean sa = false;
      if (forceSKU != null) { // before we sort, add forceSKU if necessary

         Pattern pattern = PatternUtil.getMatch(patterns,forceSKU);
         PSKU useSKU = (pattern != null) ? (PSKU) pattern : forceSKU;

         if ( ! setAll.contains(useSKU) ) setAll.add(useSKU);

         if ( ! setActive.contains(useSKU) ) sa = true;

         gotoLater(useSKU);
      }

      skusAll    = PatternUtil.sortedList(setAll   );
      skusActive = PatternUtil.sortedList(setActive);

      return sa;
   }

   protected void put() {
      defaultQueue.put(data.defaultQueue);
      showAll();
   }

   protected void getAndValidate() throws ValidationException {
      stopEditing();

      data.mappings = queueAdapter.getMappings(skusAll);
      data.defaultQueue = defaultQueue.get();
      // products are updated immediately on refresh

      Iterator i = data.mappingQueues.iterator();
      while (i.hasNext()) {
         EditSKUData.MappingQueue mq = (EditSKUData.MappingQueue) i.next();

         mq.mappings = ((Adapter) mq.adapter).getMappings(skusAll);
      }

      data.validate();
   }

   private void transfer(SKU skuNew, SKU skuOld) {

      transferMaps(skuNew,skuOld,queueAdapter.getHashMaps());

      Iterator i = data.mappingQueues.iterator();
      while (i.hasNext()) {
         EditSKUData.MappingQueue mq = (EditSKUData.MappingQueue) i.next();

         transferMaps(skuNew,skuOld,((Adapter) mq.adapter).getHashMaps());
      }

      // transfer to other places that aren't visible in EditSKUDialog

      if (skuOld.equals(data.jpegSKU)) data.jpegSKU = skuNew;
      // reverse order because skuOld is definitely not null

      if (data.dueSkus.contains(skuOld) && ! data.dueSkus.contains(skuNew)) {
         SortUtil.addInSortedOrder(data.dueSkus,skuNew,SKUComparator.displayOrder);
      }
      // the mappings are sorted during getAndValidate, but these we have to fix now.
      // the "already contains skuNew" case is kind of academic, but it could happen.
   }

   private void transferSimple(PSKU pskuNew, PSKU pskuOld) {

      transferMapsSimple(pskuNew,pskuOld,queueAdapter.getHashMaps());

      Iterator i = data.mappingQueues.iterator();
      while (i.hasNext()) {
         EditSKUData.MappingQueue mq = (EditSKUData.MappingQueue) i.next();

         transferMapsSimple(pskuNew,pskuOld,((Adapter) mq.adapter).getHashMaps());
      }
   }

   private void clean(Collection covered) {

      cleanMaps(covered,queueAdapter.getHashMaps());

      Iterator i = data.mappingQueues.iterator();
      while (i.hasNext()) {
         EditSKUData.MappingQueue mq = (EditSKUData.MappingQueue) i.next();

         cleanMaps(covered,((Adapter) mq.adapter).getHashMaps());
      }
   }

   private HashSet findDiffs(PSKU pskuMain, Collection pskuOther) {
      HashSet diffs = new HashSet();

      findDiffs(pskuMain,pskuOther,diffs,queueAdapter.getHashMaps());

      Iterator i = data.mappingQueues.iterator();
      while (i.hasNext()) {
         EditSKUData.MappingQueue mq = (EditSKUData.MappingQueue) i.next();

         findDiffs(pskuMain,pskuOther,diffs,((Adapter) mq.adapter).getHashMaps());
      }

      return diffs;
   }

// --- queue renderer ---

   private static final Color darkGreen = new Color(0,128,0);
   private static final Color darkCyan  = new Color(0,128,128);
   private static final Color disabled  = new JLabel().getBackground();

   private static class QueueRenderer extends DefaultTableCellRenderer {

      private View view;
      private QueueAdapter queueAdapter;
      private GridColumn[] colsArray;
      private boolean enableBackup;

      public QueueRenderer(View view, QueueAdapter queueAdapter, GridColumn[] colsArray, boolean enableBackup) {
         this.view = view;
         this.queueAdapter = queueAdapter;
         this.colsArray = colsArray;
         this.enableBackup = enableBackup;
      }

      public boolean isMatch(String rowQueueID, String colQueueID) {
         return rowQueueID != null && rowQueueID.equals(colQueueID);
      }

      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

         Color color1 = null;
         Color color2 = null;

      // figure out color

         String colQueueID = (String) colsArray[column].property;
         if (colQueueID == null) {
            color1 = Color.white; // non-queue column
         } else {
            PSKU psku = (PSKU) view.get(row);
            if (isMatch(queueAdapter.getCurrentQueueID(psku),colQueueID)) { // match
               if (isSelected) color2 =   darkGreen;
               else            color1 = Color.green;
            } else if (enableBackup && isMatch(queueAdapter.getCurrentBackupQueueID(psku),colQueueID)) { // match backup
               if (isSelected) color2 =   darkCyan;
               else            color1 = Color.cyan;
            } else {
               color1 = disabled; // non-match
            }
            // the enableBackup check isn't just an optimization!
            // without that, hidden backupQueueID settings would
            // still change the cell colors.
         }

      // apply color

         if (color1 != null) setBackground(color1);
         Component c = super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
         if (color2 != null) setBackground(color2);
         // apply color2 after superclass call so that it overrides selection color

         return c;
      }
   }

   /**
    * The point is to cause a row repaint when the queue changes.
    */
   private static class MagicGrid extends Grid {

      private int index1;
      private int index2;
      public JTable table;

      public MagicGrid(View view, GridColumn[] columns, int index1, int index2) {
         super(view,columns);
         this.index1 = index1;
         this.index2 = index2;
      }

      public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
         super.setValueAt(aValue,rowIndex,columnIndex);

         if (index1 <= columnIndex && columnIndex < index2) {
            table.valueChanged(new ListSelectionEvent(this,rowIndex,rowIndex,false));
         }
      }
   }

// --- utilities ---

   private void stopEditing() {
      viewHelper.stopEditing();
      // only one little thing, but there could be more some day,
      // and this is called in several places

      ViewHelper.stopEditing(tableFixed);
      // the one flaw I know of in this dual-table scheme is,
      // there can be two cell editors open at the same time
      // (not any more, since tableFixed isn't editable now)
   }

   private static Comparator naturalComparator = new NaturalComparator();
   private static Comparator noCaseComparator = new NoCaseComparator();
   private static Comparator booleanComparator = new BooleanComparator();

// --- UI commands ---

   private void showAll() {
      stopEditing(); // in case edited row is going to vanish

      PSKU psku = null;
      int offset = 0;
      int row = tableFixed.getSelectedRow();
      if (row != -1) {
         psku = (PSKU) rows.get(row);
         offset = getOffset(row);
      }

      showAllImpl();

      if (row != -1) {
         gotoLater(psku,offset);
      }
   }

   private void showAllImpl() {

      rows.clear();
      rows.addAll( Field.get(showAllCodes) ? skusAll : skusActive );
      view.userChange(SKUComparator.displayOrder);
      sorter.reset();
      // now that we use the ListView interface to modify the rows list (in addRule)
      // we need to keep the view comparator in sync with the data in the rows list.
   }

   /**
    * Get the vertical offset of the given row within the visible area.
    */
   private int getOffset(int row) {

      Rectangle r = tableFixed.getCellRect(row,0,/* includeSpacing = */ true);
      // as long as we're consistent, it doesn't matter
      // if we include spacing or not, but I bet this way is slightly faster.

      JViewport viewport = viewHelper.getScrollPane().getViewport();
      Point p = viewport.getViewPosition();

      return r.y - p.y; // can have any value, since row may not be visible
   }

   private void gotoLater(final PSKU psku) {
      EventQueue.invokeLater(new Runnable() { public void run() { gotoNow(psku); } });
   }
   private void gotoNow(PSKU psku) {
      int offsetRow = Text.getInt(this,"c2"); // show a nice amount of context
      gotoNow(psku,offsetRow * tableFixed.getRowHeight());
   }

   /**
    * Select the given SKU and try to scroll the view to put it in the given position.
    * You typically have to use the "later" form because the viewport doesn't sync up
    * with recent events until it revalidates.
    */
   private void gotoLater(final PSKU psku, final int offset) {
      EventQueue.invokeLater(new Runnable() { public void run() { gotoNow(psku,offset); } });
   }
   private void gotoNow(PSKU psku, int offset) {

      int row = rows.indexOf(psku);
      if (row == -1) return;

      Rectangle r = tableFixed.getCellRect(row,0,/* includeSpacing = */ true);
      // as long as we're consistent, it doesn't matter
      // if we include spacing or not, but I bet this way is slightly faster.
      // actually, this way is the right one for the limit calculation below.

      JViewport viewport = viewHelper.getScrollPane().getViewport();
      Point p = viewport.getViewPosition();

      // force offset into visible range
      int limit = viewport.getExtentSize().height - r.height;
      if (offset < 0) offset = 0;
      if (offset > limit) offset = limit;

      int target = r.y - offset;
      if (target < 0) target = 0;
      if (target != p.y) viewport.setViewPosition(new Point(p.x,target));

      // the viewport usually handles negative target correctly,
      // but if the current y is zero it seems to fail.
      // I've never seen it fail in the other direction, so don't
      // worry about that yet.

      tableFixed.clearSelection();
      tableFixed.addRowSelectionInterval(row,row);
   }

// --- rule stuff ---

   // general theory of rules:
   //
   // in 6.0.0 we allowed rules to coexist with overlapping non-rules, but now in 6.1.0
   // we want to prevent that.  however, it's a really tough problem, the worst case
   // being how to handle a simultaneous rule and covered-SKU creation that gets merged.
   // also, as long as the PSKUs are all kept in order, the coexistence isn't a problem,
   // it's just a matter of tidiness.  so, the idea is, let's just have the UI clean up
   // overlaps and not produce new ones, and let the rest of the system do whatever.
   //
   // so, the places where SKU lists change can be divided into two groups.  first, the
   // parts of the system that keep doing whatever.
   //
   // * push with ConfigCenter (though I might add a cleanup option there at some point)
   // * auto-config
   // * auto-refresh
   //
   // second, the parts here in ESD that make sure there aren't any overlaps.  there's no
   // universal solution, we just have to hand-tune each part.
   //
   // * ESD startup (not a change, but we don't want to display existing overlaps)
   // * ESD refresh
   // * add rule
   // * remove rule

   private class RuleAccessor implements EditAccessor { // not static since it will call out
      public Class getFieldClass() { return Boolean.class; }
      public Object get(Object o) { return new Boolean(o instanceof Pattern); }
      public void put(final Object o, Object value) {
         EventQueue.invokeLater(new Runnable() { public void run() {
            if (o instanceof Pattern) removeRule((PSKU) o);
            else addRule((PSKU) o,/* auto = */ true);
            // could use value to decide whether to add or remove, but o seems more reliable;
            // also pass o along in case there's some confusion between clicked and selected
         } });
         // calling stopEditing inside put function causes infinite loop, that's why I added
         // the invokeLater
      }
   };

   private static class SelectionInfo {
      public PSKU psku;
      public int offset;
   }

   private SelectionInfo getSelectionInfo(PSKU psku, boolean add) throws ValidationException {

      SelectionInfo si = new SelectionInfo();
      int row;
      if (psku == null) {

         // doesn't matter which table we use, selection is the same
         // only look at the first selected row if there are several
         row = tableFixed.getSelectedRow();
         if (row == -1) throw new ValidationException(Text.get(this,add ? "e14a" : "e14b"));

         si.psku = (PSKU) rows.get(row);

      } else {

         // we knew this earlier, unfortunate that we have to look it up
         row = rows.indexOf(psku);
         if (row == -1) throw new ValidationException(Text.get(this,"e15"));

         si.psku = psku;
      }

      si.offset = getOffset(row);
      return si;
   }

   /**
    * @param auto True if when possible, we should automatically create
    *             a general rule without bringing up the pattern dialog.
    */
   private void addRule(PSKU psku_in, boolean auto) {
      stopEditing();
      try {

         SelectionInfo si = getSelectionInfo(psku_in,/* add = */ true);

         Pattern pattern = PatternDialog.pick(this,data.productConfig,si.psku,auto);
         if (pattern == null) return;

         boolean add = ! skusAll.contains(pattern);
         if (add) {

            // order doesn't matter a lot here, except that we have to be sure to copy
            // the data from the old SKU before we clean it

            LinkedList covered = PatternUtil.getCovered(Collections.singletonList(pattern),skusAll);

            HashSet diffs = findDiffs(si.psku,covered);
            if (diffs.size() > 0) {
               if ( ! PatternDialog.confirmAddInconsistent(this,PatternUtil.sortedList(diffs)) ) return;
            }
            // this warning lies outside PatternDialog because if there are inconsistencies,
            // you probably want to look at the SKUs, not go back and pick a different rule.
            // of course PatternDialog also doesn't have the info needed to compute it.
            //
            // the reason the diff report is just SKUs is that a typical store will have
            // a small number of queues but possibly a large number of SKUs.  so, if you
            // want to go look at the diffs, knowing the SKU is a good start.  more info
            // might be better, but it's hard to compute and display.

            SortUtil.addInSortedOrder(skusAll,pattern,SKUComparator.displayOrder);
            if (PatternUtil.isActive(pattern.getProduct(),data.productConfig)) {
               SortUtil.addInSortedOrder(skusActive,pattern,SKUComparator.displayOrder);
            }

            transferSimple(pattern,si.psku);
            // one minor flaw here is that si.psku may not be one of the covered SKUs.
            // but, it's important to be able to select the row you want to copy, and
            // I don't see what else to do about it.
            // also, the inconsistency warning will take care of a lot of it.

            clean(covered);
            skusAll   .removeAll(covered);
            skusActive.removeAll(covered);

            showAllImpl(); // refresh the view
         }

         gotoLater(pattern,si.offset);

      } catch (ValidationException e) {
         Pop.error(this,e,Text.get(this,"s11"));
      }
   }

   private void removeRule(PSKU psku_in) {
      stopEditing();
      try {

         SelectionInfo si = getSelectionInfo(psku_in,/* add = */ false);

         if ( ! (si.psku instanceof Pattern) ) throw new ValidationException(Text.get(this,"e16"));
         String product = ((Pattern) si.psku).getProduct();

         LinkedList records = PatternUtil.getProductRecords(product,data.productConfig);
         if (records.size() == 0) { // inactive
            if ( ! PatternDialog.confirmRemoveInactive(this,product) ) return;
         }

         SKU targetSKU = null;
         if (records.size() > 0) { // else nothing to do, avoid the effort

            final LinkedList patterns = PatternUtil.getPatterns(skusAll);
            patterns.remove(si.psku);
            // could restrict to patterns for the specific product, if we wanted

            final LinkedList uncovered = new LinkedList();
            NewProduct.iterate(records,new ProductCallback() { public void f(SKU sku, String description, Long dueInterval) {
               if ( ! PatternUtil.matchesAny(patterns,sku) ) uncovered.add(sku);
            } });
            uncovered.removeAll(skusActive); // slightly faster than using skusAll, and we know these are active

            if (uncovered.size() == 0) { // double-covered, row will be removed
               if ( ! PatternDialog.confirmRemoveCovered(this,si.psku) ) return;
            }

            if (uncovered.size() > 0) { // again, avoid the work

               skusAll   .addAll(uncovered);
               skusActive.addAll(uncovered); // again, we know these are active

               skusAll    = PatternUtil.sortedList(skusAll   );
               skusActive = PatternUtil.sortedList(skusActive);

               // now find the topmost of the uncovered rows so we can jump to it
               //
               int targetRow = skusActive.size(); // nice large number
               Iterator i = uncovered.iterator();
               while (i.hasNext()) {
                  SKU iSKU = (SKU) i.next();
                  int iRow = skusActive.indexOf(iSKU); // again, slightly faster
                  if (iRow < targetRow) {
                     targetSKU = iSKU;
                     targetRow = iRow;
                  }

                  // as long as we're iterating, do the transfer too
                  transferSimple(iSKU,si.psku);
               }
            }
         }

         clean(Collections.singletonList(si.psku));
         skusAll   .remove(si.psku);
         skusActive.remove(si.psku); // might not be there, not a problem

         showAllImpl(); // refresh the view
         if (targetSKU != null) gotoLater(targetSKU,si.offset); // else it's like normal "show all", basically random

      } catch (ValidationException e) {
         Pop.error(this,e,Text.get(this,"s13"));
      }
   }

// --- refresh ---

   private class UIRefreshInterface implements Refresh.RefreshInterface {

      private LinkedList covered; // extra state to avoid passing stuff around

      private StringBuffer b;
      public UIRefreshInterface() { b = new StringBuffer(); }

      public void line(String s) {
         b.append(s);
         b.append('\n');
      }

      public void blank() {
         b.append('\n');
      }

      public void result(String key) {

         // remove trailing newline if present
         int len = b.length();
         if (len > 0) b.setLength(len-1);

         Pop.info(EditSKUDialog.this,b.toString(),Text.get(EditSKUDialog.class,key));
      }

      public MerchantConfig getMerchantConfig() { return merchantConfig; }
      public String getOldProductURL() { return skuRefreshURL; }
      public String getNewProductURL() { return newProductURL; }
      public String getConversionURL() { return conversionURL; }

      public ProductConfig getProductConfig() { return data.productConfig; }
      public void setProductConfig(ProductConfig pc) { data.productConfig = pc; }

      public Collection recomputeSKUs(ProductConfig pc) {
         HashSet setAll = new HashSet();

         descriptionAdapter.putProductConfig(pc,setAll);
         HashSet setActive = (HashSet) setAll.clone();

         Conversion.addAll(pc.conversions,setAll);
         // these don't count as active, but we need them in the list
         // in case we receive a conversion to a not-yet-defined product.
         // without this, we'd do the transfer and put entries into
         // the adapter hash maps, but we wouldn't pull them out when we
         // close because the SKUs wouldn't be listed.

         // take union to get skusAll
         setAll.addAll(skusAll);
         //
         // note, this is perhaps not the ideal behavior.
         // skusAll is originally the union of active product SKUs
         // with SKUs that exist in data .. but now we have no way
         // to remove dataless, deactivated product SKUs.
         // on the other hand, there's no way to tell whether they're
         // dataless without scanning the current data tables,
         // and this way we just never remove any rows, which is OK.

         LinkedList patterns = PatternUtil.getPatterns(setAll);
         setActive.addAll(PatternUtil.getActive(patterns,pc));
         // make active patterns also show up in the active set

         covered = PatternUtil.getCovered(patterns,setAll); // setAll contains setActive
         // clean(covered); // (*4)
         setAll   .removeAll(covered);
         setActive.removeAll(covered);
         // can't make the clean call yet because transfer and setIfNotSet can add more SKUs.
         // they do both use matchesAnyKey, but that only checks for overlap within the same
         // hash map, not across all maps.
         // note, we don't want to block the transfer and setIfNotSet calls up front because
         // they have desirable effects on things other than hash maps, like on the JPEG SKU.

         skusAll    = PatternUtil.sortedList(setAll   ); // replaces old skusAll
         skusActive = PatternUtil.sortedList(setActive); // replaces old skusActive

         return skusAll; // no copy, trust it won't get modified
      }

      public void transfer(SKU skuNew, SKU skuOld) { EditSKUDialog.this.transfer(skuNew,skuOld); }

      public String getSingleQueue() { return queueAdapter.getSingleQueue(); }
      public void setIfNotSet(SKU sku, String queueID) { queueAdapter.setIfNotSet(sku,queueID); }

      public String getDefaultQueue() { return defaultQueue.get(); }
      public void clearDefaultQueue() {
         defaultQueue.put(null); // which will get saved later
         hideDefaultQueue();  // hide it in the user interface
      }

      public void clean() { EditSKUDialog.this.clean(covered); } // (*4)
      public void showAll() { EditSKUDialog.this.showAll(); }
   }

   private void refresh() {
      stopEditing();
      Refresh.refresh(new UIRefreshInterface());
   }

}

