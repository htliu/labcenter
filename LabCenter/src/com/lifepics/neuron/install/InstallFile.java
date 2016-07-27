/*
 * InstallFile.java
 */

package com.lifepics.neuron.install;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.Obfuscate;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * A subobject of {@link InstallConfig} that holds information
 * about a single file.  I split it out because it's going to
 * be fairly widely used and I didn't want to be forced to write
 * "InstallConfig.InstallFile" every time.
 */

public class InstallFile extends Structure {

// --- method enumeration ---

   // this has to come before the structure definition
   // since it's all static

   private static final int METHOD_MIN = 0;

   public static final int METHOD_DOWNLOAD = 0;
   public static final int METHOD_MKDIR = 1;
   public static final int METHOD_RMDIR = 2;
   public static final int METHOD_MOVE = 3;
   public static final int METHOD_COPY = 4;
   public static final int METHOD_MKEMPTY = 5;
   public static final int METHOD_DELETE = 6;
   public static final int METHOD_EXTRACT_ONE = 7;
   public static final int METHOD_EXTRACT_ALL = 8;
   public static final int METHOD_QUERY = 9;
   public static final int METHOD_IMPORT = 10;
   public static final int METHOD_EXPORT = 11;
   public static final int METHOD_INVOKE = 12;
   public static final int METHOD_BRANCH = 13;

   private static final int METHOD_MAX = 13;

   private static String[] methodTable = {
         Text.get(InstallFile.class,"me0"),
         Text.get(InstallFile.class,"me1"),
         Text.get(InstallFile.class,"me2"),
         Text.get(InstallFile.class,"me3"),
         Text.get(InstallFile.class,"me4"),
         Text.get(InstallFile.class,"me5"),
         Text.get(InstallFile.class,"me6"),
         Text.get(InstallFile.class,"me7"),
         Text.get(InstallFile.class,"me8"),
         Text.get(InstallFile.class,"me9"),
         Text.get(InstallFile.class,"me10"),
         Text.get(InstallFile.class,"me11"),
         Text.get(InstallFile.class,"me12"),
         Text.get(InstallFile.class,"me13")
      };

   public static String[] descriptionTable = {
         "de0",
         "de1",
         "de2",
         "de3",
         "de4",
         "de5",
         "de6",
         "de7",
         "de8",
         "de9",
         "de10",
         "de11",
         "de12",
         "de13"
   };
   // clumsy, really ought to have a describe method on InstallFile

   private static int toMethod(String s) throws ValidationException {
      for (int i=0; i<methodTable.length; i++) {
         if (s.equals(methodTable[i])) return i;
      }
      throw new ValidationException(Text.get(InstallFile.class,"e2",new Object[] { s }));
   }

   private static String fromMethod(int method) {
      return methodTable[method];
   }

   private static EnumeratedType methodType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toMethod(s); }
      public String fromIntForm(int i) { return fromMethod(i); }
   };

   private static void validateMethod(int method) throws ValidationException {
      if (method < METHOD_MIN || method > METHOD_MAX) throw new ValidationException(Text.get(InstallFile.class,"e3",new Object[] { Convert.fromInt(method) }));
   }

// --- fields ---

   // which fields are required depends on the method,
   // so nullness can't be checked at the field level.
   //
   // I'm using Boolean null/true instead of boolean false/true
   // so that some fields aren't needed in the hand-crafted XML

   public LinkedList labels; // seems like these should come first
   public int method;
   public String src;
   public String dest;
   public Long verifySize;
   public Boolean verifyXML; // an alternative to verifySize
   public Integer checksum;
   public LinkedList binds;
   public String target;
   public LinkedList paramIn;
   public String paramOut;
   public String type;
   public String typeData;
   public Boolean done;

   public String spare1;
   public String spare2;
   //
   // since the installer rewrites the install config file as it goes,
   // we can't pass data through there to the setup program
   // unless the installer at least understands that there are fields.
   // so, define a couple of spare fields for future expansion.

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      InstallFile.class,
      // no version
      new AbstractField[] {

         new InlineListField("labels","Label"),
         new EnumeratedField("method","Method",methodType),
         new NullableStringField("src","Src"),
         new StringField("dest","Dest"),
         new NullableLongField("verifySize","VerifySize"),
         new NullableBooleanField("verifyXML","VerifyXML"),
         new NullableIntegerField("checksum","Checksum") {
            protected void loadNormal(Node node, Object o) throws ValidationException {
               String temp = XML.getNullableText(node,xmlName);
               Integer i = (temp == null) ? null : new Integer(Obfuscate.toChecksum(temp));
               tset(o,i);
            }
            public void store(Node node, Object o) {
               Integer i = tget(o);
               String temp = (i == null) ? null : Obfuscate.fromChecksum(i.intValue());
               XML.createNullableText(node,xmlName,temp);
            }
         },
         new StructureListField("binds","Bind",Bind.sd,Merge.NO_MERGE),
         new NullableStringField("target","Target"),
         new InlineListField("paramIn","In"),
         new NullableStringField("paramOut","Out"),
         new NullableStringField("type","Type"),
         new NullableStringField("typeData","TypeData"),
         new NullableBooleanField("done","Done"),

         new NullableStringField("spare1","Spare1"),
         new NullableStringField("spare2","Spare2")
      });

   protected StructureDefinition sd() { return sd; }

// --- constants ---

   public static final String TYPE_SETUP_APP = "SETUP";
   public static final String TYPE_SETUP_STORE_LIST = "STORE LIST";
   public static final String TYPE_SETUP_TEMPLATE = "TEMPLATE";
   //
   public static final String TYPE_MAIN_APP = "MAIN";
   //
   // the type field is used to mark special file types
   // for various reasons.  it'll be null in most cases.
   // we don't validate it because future setup apps
   // may have special file types of their own that are
   // not known to the locked-in installer app.
   //
   // also, this mechanism is not perfectly general.
   // we might want to have some special files come down
   // in jar files and get extracted, and right now we
   // don't have any way to signal that.  but, no matter,
   // this is sufficient for now.
   //
   // we could put the special-file information in some
   // other fields somewhere, but then the file names
   // would be duplicated in the install file, and would
   // surely get mis-updated some day since the file is
   // going to be maintained by hand.  avoiding that was
   // the whole point of this design.

// --- helpers ---

   // how are InstallFile objects used in general, outside the
   // big bunch of method-dependent code in InstallThread?
   //
   // 1. files can have a type code, and can be selected based on that
   // 2. for selected files, we may call getDestFile and getAppName
   // 3. the setup GUI looks at the type data of the selected files
   // 4. TransferTracker uses getShortName, getVerifySize, and getDone
   // 5. the installItems loop reads and writes the done field
   // 6. the same loop also uses getShortName to log install progress
   //
   // most of this is pretty harmless, but because of how getShortName
   // and getDestFile are used, every InstallFile really does need to
   // have dest filled in.  I was thinking about making it nullable for
   // queries, but you still need something for the tracker to show.

   private static final String  ABS_PREFIX = "@";
   private static final String BASE_PREFIX = ":";

   /**
    * Convert a relative path string into an an absolute path
    * based on mainDir most of the time but on baseDir if the
    * path string starts with a colon.
    * Normally you'll call getSrcFile or getDestFile, this is
    * just public for the special case METHOD_EXTRACT_ONE.
    *
    * Now absolute path is an option too; that's helpful sometimes.
    */
   public static File getFile(String s, File baseDir, File mainDir) {
      File dir;
      if (s.startsWith(ABS_PREFIX)) {
         return new File(s.substring(ABS_PREFIX.length()));
      } else if (s.startsWith(BASE_PREFIX)) {
         dir = baseDir;
         s = s.substring(BASE_PREFIX.length());
      } else {
         dir = mainDir;
      }
      return new File(dir,s);
   }

   public File getSrcFile (File baseDir, File mainDir) { return getFile(src, baseDir,mainDir); }
   public File getDestFile(File baseDir, File mainDir) { return getFile(dest,baseDir,mainDir); }

   /**
    * Get a nice displayable filename without knowing the directories.
    */
   public String getShortName() {
      String s = (method == METHOD_EXTRACT_ALL) ? src : dest;
      if (s.startsWith(BASE_PREFIX)) {
         s = s.substring(BASE_PREFIX.length());
      }
      return new File(s).getName(); // keep it nice and short

      // the reason we use src instead of dest for METHOD_EXTRACT_ALL
      // is not just that there are really multiple destination files,
      // it's also that the destination might well be the root of the
      // main directory, so that getName would return an empty string.
   }

   /**
    * Get the name of an application file, which by convention goes in the base directory.
    */
   public String getAppName() throws Exception {
      // cf. getShortName
      // app files are identified by type, so by validation this isn't METHOD_EXTRACT_ALL
      String s = dest;
      if (s.startsWith(BASE_PREFIX)) {
         s = s.substring(BASE_PREFIX.length());
         File f = new File(s);
         if (f.getParentFile() == null) { // no subdirectory
            return f.getName();
         }
      }
      throw new Exception(Text.get(this,"e9"));
   }

   public long getVerifySize() {
      return (verifySize != null) ? verifySize.longValue() : 0;
   }

   public boolean getVerifyXML() {
      return (verifyXML != null) ? verifyXML.booleanValue() : false;
   }

   public Integer getChecksum() {
      return checksum;
   }

   public boolean getDone() {
      return (done != null) ? done.booleanValue() : false;
   }

// --- validation ---

   public void validate() throws ValidationException {

      // mainly we're checking nullness of fields is appropriate to method.
      // note we don't call Convert.fromMethod until after we've validated.

      // my best summary of the rules:
      //
      // * some things have src, some don't, it's intuitive
      // * all things have dest
      // * downloads may have verification fields
      // * most things may have a type plus data
      //
      // also note that src isn't always a file name.  for downloads it's the URL,
      // while for METHOD_EXTRACT_ONE it's the jar file name plus file name.

      validateMethod(method);

      boolean isCreate = (method == METHOD_MKDIR || method == METHOD_MKEMPTY); // create from nothing
      boolean isDelete = (method == METHOD_RMDIR || method == METHOD_DELETE);
      boolean isSingle = (method == METHOD_IMPORT || method == METHOD_EXPORT || method == METHOD_INVOKE);
      boolean isBound  = (method == METHOD_IMPORT || method == METHOD_EXPORT || method == METHOD_QUERY );
      boolean isPseudo = (method == METHOD_EXTRACT_ALL || method == METHOD_QUERY || method == METHOD_BRANCH);

      // src must be absent from create/delete/single, must be present on all others
      if (isCreate || isDelete || isSingle) {
         if (src != null) throw new ValidationException(Text.get(this,"e4",new Object[] { fromMethod(method) }));
      } else {
         if (src == null) throw new ValidationException(Text.get(this,"e5",new Object[] { fromMethod(method) }));
      }

      // dest is declared not null at the struct level

      // verify fields are only allowed for download.
      // they are not required and are not exclusive
      // (but in practice we don't combine them).
      if (method != METHOD_DOWNLOAD && (verifySize != null || verifyXML != null || checksum != null)) {
         throw new ValidationException(Text.get(this,"e6",new Object[] { fromMethod(method) }));
      }

      // verifySize must be positive
      if (verifySize != null && verifySize.longValue() <= 0) {
         throw new ValidationException(Text.get(this,"e1",new Object[] { Convert.fromLong(verifySize.longValue()) }));
      }

      // bind fields are only allowed for certain methods
      if ( ! isBound && binds.size() > 0 ) {
         throw new ValidationException(Text.get(this,"e10",new Object[] { fromMethod(method) }));
      }

      Iterator i = binds.iterator();
      while (i.hasNext()) {
         ((Bind) i.next()).validate();
      }
      // duplicate bindings are allowed, since they might be useful for export

      // argument fields are only allowed for invoke
      if (method != METHOD_INVOKE && (target != null || paramIn.size() > 0 || paramOut != null)) {
         throw new ValidationException(Text.get(this,"e11",new Object[] { fromMethod(method) }));
      }

      // target is required for invoke
      if (method == METHOD_INVOKE && target == null) {
         throw new ValidationException(Text.get(this,"e12",new Object[] { fromMethod(method) }));
      }

      // type is not allowed on delete or pseudo dest (since the file won't be there)
      if ((isDelete || isPseudo) && type != null) {
         throw new ValidationException(Text.get(this,"e7",new Object[] { fromMethod(method) }));
      }

      // type data is not allowed if there's no type
      if (typeData != null && type == null) {
         throw new ValidationException(Text.get(this,"e8"));
      }

      // done goes from null to true for all methods
   }

// --- bind class ---

   public static class Bind extends Structure {

      public String name;
      public String path;

      public static final StructureDefinition sd = new StructureDefinition(

         Bind.class,
         // no version
         new AbstractField[] {

            new StringField("name","Name"),
            new StringField("path","Path")
         });

      static { sd.setAttributed(); }
      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException {
      }
   }

}

