/*
 * Queue.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for a single print queue.
 */

public class Queue extends Structure {

// --- fields ---

   public String queueID;
   public String name;
   public int format;
   public Boolean noAutoPrint; // noAutoSpawn is technically better but I don't like it
   public Boolean switchToBackup;
   public String threadID; // nullable

   /**
    * Format-specific information for the queue.
    * The object type depends on the format.
    * The object must be a subclass of Structure.
    */
   public Object formatConfig;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Queue.class,
      0,new History(new int[] {0,754,1,757,2,769,3}),
      new AbstractField[] {

         new StringField("queueID","QueueID",0,null), // custom loadDefault
         new StringField("name","Name",0,null),       //
         new EnumeratedField("format","Format",OrderEnum.formatType,0,Order.FORMAT_FLAT),
         new NullableBooleanField("noAutoPrint","NoAutoPrint",1,null),
         new NullableBooleanField("switchToBackup","SwitchToBackup",2,null),
         new NullableStringField("threadID","ThreadID",3,null),
         new CompositeField("formatConfig","FormatConfig",0) {
            private Structure tget(Object o) {
               try {
                  return (Structure) javaField.get(o);
               } catch (IllegalAccessException e) {
                  throw new Error(e);
               }
            }
            private void tset(Object o, Object value) {
               try {
                  javaField.set(o,value);
               } catch (IllegalAccessException e) {
                  throw new Error(e);
               }
            }
            public boolean isDynamic() { return true; }
            public Collection getChildren(Object o) {
               return null;
            }
            public void init(Object o) {
               // no action needed -- init doesn't operate recursively,
               // it's called as each individual object is constructed.
            }
            public void makeRelativeTo(Object o, File base) {
               tget(o).makeRelativeTo(base);
            }
            public void copy(Object oDest, Object oSrc) {
               tset(oDest,CopyUtil.copy(tget(oSrc)));
            }
            public boolean equals(Object o1, Object o2) {
               return tget(o1).equals(tget(o2));
               // this is OK even if the objects have different classes ...
               // Structure.equals checks the classes first, and anyway
               // we won't even get here in that case since the format
               // field will be different and terminate the equals test.
            }
            public void merge(Object oDest, Object oBase, Object oSrc) {
               Structure valDest = tget(oDest);
               Structure valBase = tget(oBase);
               Structure valSrc  = tget(oSrc);
               if (    valDest.getClass().equals(valBase.getClass())
                    && valSrc .getClass().equals(valBase.getClass()) ) {
                  valDest.merge(valBase,valSrc);
               } else {
                  if ( ! valSrc.equals(valBase) ) copy(oDest,oSrc);
               }
               // this is a variant of the NullableStructureField null-pattern.
               // here the objects can't be null, but they can be of different
               // classes, and that prevents merge in much the same way.
            }
            public void loadDefault(Object o) {
               // we could construct a new object here, and then carefully
               // call putPanel in ConfigDialog.createQueue,
               // or we could just leave it null and let getPanel create it.
            }
            protected void loadNormal(Node node, Object o) throws ValidationException {
               int format = ((Queue) o).format;
               Order.validateFormat(format); // will do again later, but need now too
               Structure val = (Structure) getFormatObject(format);
               tset(o,val.load(XML.getElement(node,xmlName)));
               // it's just a matter of style whether we use tset(o,___)
               // or ((Queue) o).formatConfig = ___.  I chose
               // to be consistent with the other loadNormal functions.
            }
            public void store(Node node, Object o) {
               tget(o).store(XML.createElement(node,xmlName));
            }
            protected void tstoreNormal(int t, Node node, Object o) throws ValidationException {
               tget(o).tstore(t,XML.createElement(node,xmlName));
            }
            // isDefault is the mirror of loadDefault, so it's complicated;
            // let's leave it with the default behavior of returning false
         }
      });

   protected StructureDefinition sd() { return sd; }

   // note, the order of format and formatConfig matters, because
   // in load, we have to read the format so we know what type of
   // formatConfig to construct.

   // formatConfig can't be a StructureField because its type can vary.
   // a good way of thinking about what's going on with it:
   // normally Structure is just an entry point to StructureDefinition
   // and the various AbstractField subclasses, which manipulate the
   // objects without being part of them.  here, though, the recursion
   // passes through Structure as well, and so is not external to the
   // objects.

   // the formatConfig field class shares some things with StructureField,
   // but it shouldn't be a subclass, because it needs to get custom code
   // any time a new function is added to AbstractField.

// --- format class ---

   // these assume the format is valid, they don't validate it.
   // (there's still a check, but it doesn't give a nice error.)

   // for the first exception, I debated about using IllegalArgumentException
   // or UnsupportedOperationException ... the flaw could be in either place.
   // caller could pass invalid value, or programmer could forget to implement.

   public static Class getFormatClass(int format) {
      switch (format) {

      case Order.FORMAT_FLAT:     return ManualConfig.class;
      case Order.FORMAT_TREE:     return ManualConfig.class;
      case Order.FORMAT_NORITSU:  return NoritsuConfig.class;
      case Order.FORMAT_KONICA:   return KonicaConfig.class;
      case Order.FORMAT_FUJI:     return FujiConfig.class;
      case Order.FORMAT_FUJI_NEW: return FujiNewConfig.class;
      case Order.FORMAT_DLS:      return DLSConfig.class;
      case Order.FORMAT_KODAK:    return KodakConfig.class;
      case Order.FORMAT_AGFA:     return AgfaConfig.class;
      case Order.FORMAT_LUCIDIOM: return LucidiomConfig.class;
      case Order.FORMAT_PIXEL:    return PixelConfig.class;
      case Order.FORMAT_DP2:      return DP2Config.class;
      case Order.FORMAT_BEAUFORT: return BeaufortConfig.class;
      case Order.FORMAT_DKS3:     return DKS3Config.class;
      case Order.FORMAT_DIRECT_PDF: return DirectPDFConfig.class;
      case Order.FORMAT_ZBE:      return ZBEConfig.class;
      case Order.FORMAT_FUJI3:    return Fuji3Config.class;
      case Order.FORMAT_HP:       return HPConfig.class;
      case Order.FORMAT_XEROX:    return XeroxConfig.class;
      case Order.FORMAT_DIRECT_JPEG: return DirectJPEGConfig.class;
      case Order.FORMAT_BURN:     return BurnConfig.class;
      case Order.FORMAT_HOT_FOLDER: return HotFolderConfig.class;
      case Order.FORMAT_DNP:      return DNPConfig.class;
      case Order.FORMAT_PURUS:    return PurusConfig.class;
      case Order.FORMAT_RAW_JPEG: return RawJPEGConfig.class;
      default:
         throw new Error(Text.get(Queue.class,"e1",new Object[] { Convert.fromInt(format) }));
      }
   }

   public static Object getFormatObject(int format) {
      Class c = getFormatClass(format); // don't catch the runtime exception this throws
      try {
         return c.newInstance();
      } catch (Exception e) {
         throw new Error(Text.get(Queue.class,"e2",new Object[] { Convert.fromInt(format) }));
      }
   }

// --- sort functions ---

   public static Accessor idAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Queue) o).queueID; }
   };

   public static Accessor nameAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Queue) o).name; }
   };

   public static Comparator nameComparator = new FieldComparator(nameAccessor,new NoCaseComparator());

// --- validation ---

   public static void validateName(String name) throws ValidationException {
      if (name.length() == 0) throw new ValidationException(Text.get(Queue.class,"e4"));
   }

   public void validate() throws ValidationException {

      if (queueID.length() == 0) throw new ValidationException(Text.get(this,"e3"));
      validateName(name);

      // no sense validating threadID here, just handle it at QueueList level

      Order.validateFormat(format);

      if ( ! getFormatClass(format).isInstance(formatConfig) ) throw new ValidationException(Text.get(this,"e5",new Object[] { formatConfig.getClass().getName(), OrderEnum.fromFormat(format) }));
      // freshly-loaded objects always have the right class, so this is really a check on the UI
      //
      // also it handles a merge problem:  if the queue type changes locally,
      // and at the same time the queue data changes on the server,
      // then the merge will overwrite the local data but not the local type.

      ((XML.Persist) formatConfig).validate();
   }

// --- persistence ---

   public Object loadDefault(String queueID, String name) {
      loadDefault();

      this.queueID = queueID;
      this.name = name;

      return this; // convenience
   }

}

