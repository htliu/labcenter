/*
 * Format.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.ImageUtil;
import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.struct.SKU;
import com.lifepics.neuron.thread.ThreadStopException;
import com.lifepics.neuron.thread.ToldException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

/**
 * Abstract superclass for different job formats.
 */

public abstract class Format {

// --- subclass hooks ---

   public abstract String getShortName();

   public static final int COMPLETION_MODE_MANUAL = 0;
   public static final int COMPLETION_MODE_AUTO   = 1; // when generated
   public static final int COMPLETION_MODE_DETECT = 2; // when printed
   public static final int COMPLETION_MODE_ACCEPT = 3; // when accepted

   public abstract int[] getAllowedCompletionModes();
   public abstract int   getCompletionMode(Object formatConfig);
   public abstract void  setCompletionMode(Object formatConfig, int mode);

// --- format function ---

   /**
    * Format the job by copying files from the order to the right place.
    */
   public abstract void format(Job job, Order order, Object formatConfig) throws Exception;

   /**
    * Same as format, except can return false to report thread stopping.
    * This is rare, since very few formats use interruptible operations.
    */
   public boolean formatStoppable(Job job, Order order, Object formatConfig, FormatCallback fc) throws Exception {
      format(job,order,formatConfig);
      return true;
   }

   public interface FormatCallback {
      HashMap getDueMap();
      File getBurnerFile();
      void saveJobState() throws Exception; // for incremental jobs
   }

// --- format utilities ---

   public String getCapitalizedShortName() {
      String s = getShortName();
      return (s.length() == 0) ? s : s.substring(0,1).toUpperCase() + s.substring(1);
   }

   /**
    * Require that the directory we'll be writing into exists.
    * If not, stop the thread, the configuration needs fixing.
    */
   protected static void require(File dataDir) throws Exception {
      require(dataDir,"h3a");
   }
   protected static void requireFujiRequest(File dataDir) throws Exception {
      require(dataDir,"h3b");
   }
   private static void require(File dataDir, String hintKey) throws Exception {
      if ( ! dataDir.exists() ) {
         Object[] args = new Object[] { Convert.fromFile(dataDir) };
         throw new ThreadStopException(Text.get(Format.class,"e3",args)).setHint(Text.get(Format.class,hintKey,args));
      }
   }

   protected static void autoCreate(File dataDir) throws Exception {

      // note, this isn't part of the Op undo framework,
      // it's just something that we do right away whenever needed.
      // like require, it's a thread-stopper if it fails.

      if ( ! dataDir.exists() && ! dataDir.mkdirs() ) {
         Object[] args = new Object[] { Convert.fromFile(dataDir) };
         throw new ThreadStopException(Text.get(Format.class,"e9",args)).setHint(Text.get(Format.class,"h9",args));
      }
   }

   protected static void requireOrAutoCreate(File dataDir, boolean autoCreate) throws Exception {
      if (autoCreate) {
         autoCreate(dataDir);
      } else {
         require(dataDir);
      }
   }

   /**
    * Report a missing channel.
    */
   protected void missingChannel(SKU sku, String fullID) throws IOException {
      User.tell(User.CODE_MISSING_CHANNEL,Text.get(Format.class,"e8a",new Object[] { sku.toString(), getShortName() }),
                                      sku,Text.get(Format.class,"e8b",new Object[] { fullID }));
      throw (IOException) new IOException(Text.get(Format.class,"e7",new Object[] { sku.toString(), getShortName() }))
         .initCause(new ToldException(new LocalException(LocalStatus.CODE_ERR_ORD_CHANNEL)));
   }

   /**
    * Compute the total quantity for a set of references.
    */
   protected static int totalQuantity(Job job, Order order, LinkedList refs) throws Exception {

      int quantity = 0;

      Iterator i = refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();
         Order.Item item = order.getItem(ref);

         quantity += job.getOutputQuantity(item,ref);
      }

      return quantity;
   }

   /**
    * Compute the quantity of the special SKU present for the job/order.
    * This is the maximum of the quantities for all items with that SKU,
    * or -1 if there is no special SKU, or if it's not present.
    */
   public static int specialQuantity(Job job, Order order) throws IOException {

      int quantity = -1;
      if (order.specialSKU == null) return quantity; // no CD product, optimize and exit now

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();

         if ( ! ref.sku.equals(order.specialSKU) ) continue;

         Order.Item item = getOrderItem(order,ref);

         int temp = job.getOutputQuantity(item,ref);
         if (temp > quantity) quantity = temp;
      }

      return quantity;
   }

   protected static Order.Item getOrderItem(Order order, Job.Ref ref) throws IOException {
      try {
         return order.getItem(ref);
      } catch (Exception e) {
         throw new IOException(e.getMessage());
      }
   }

// --- deref utilities ---

   // sometimes we want to think of multi-image products as a bunch of
   // single-image products.  this code helps you make that conversion.
   // there are probably more opportunities for factoring.

   // the name "deref" isn't great, but it's pretty good ... you start
   // with some references, then you follow and dereference them, then
   // you get these.

   // Job.Ref has the same field structure, but here the meanings are different ...
   // the file name is really a file name, and the quantity is the actual
   // output quantity, not an individual quantity override that's usually null.

   // note, a list of Deref objects will not be unique by filename and SKU.

   protected static class Deref {

      public String filename;
      public SKU sku;
      public int outputQuantity;

      public Deref(String filename, SKU sku, int outputQuantity) {
         this.filename = filename;
         this.sku = sku;
         this.outputQuantity = outputQuantity;
      }
   }

   protected static void collate(LinkedList derefs, List filenames, SKU sku, int outputQuantity, boolean reverse) {
      LinkedList list = new LinkedList();

      Iterator j = filenames.iterator();
      while (j.hasNext()) {
         String filename = (String) j.next();
         Deref deref = new Deref(filename,sku,/* outputQuantity = */ 1);
         if (reverse) {
            list.addFirst(deref);
         } else {
            list.add(deref);
         }
      }

      for (int i=0; i<outputQuantity; i++) {
         derefs.addAll(list);
      }
      // adding the same objects multiple times,
      // bizarre, but also saves lots of memory.
   }

   protected static class ReverseIterator implements Iterator {

      private ListIterator li;
      public ReverseIterator(List list) { li = list.listIterator(list.size()); }

      public boolean hasNext() { return li.hasPrevious(); }
      public Object next() { return li.previous(); }
      public void remove() { li.remove(); }
   }
   // sometimes an alternate approach is easier

// --- image file utility ---

   protected static class SizeRecord {
      public int width;
      public int height;
   }

   protected static void readWidthHeight(SizeRecord r, File src) throws Exception {

      FileImageInputStream fiis = null;
      ImageReader reader = null;
      try {

         String suffix = ImageUtil.getSuffix(src);
         reader = ImageUtil.getReader(suffix);

         fiis = new FileImageInputStream(src);
         reader.setInput(fiis);

         r.width  = reader.getWidth (0);
         r.height = reader.getHeight(0);

      } finally {

         if (reader != null) reader.dispose();

         try {
            if (fiis != null) fiis.close();
         } catch (Exception e) {
            // ignore
         }
      }
   }

   /**
    * @return +1 for width > height, -1 for width < height, 0 for equal.
    */
   protected static int getOrientation(double width, double height) throws Exception {
      if (width <= 0 || height <= 0) throw new Exception(Text.get(Format.class,"e10"));
      if (width == height) return 0;
      return (width > height) ? +1 : -1;
   }

// --- variation interface and implementations ---

   // in theory, access to these objects ought to be synchronized,
   // but (a) I'm pretty sure they only run in the format thread,
   // and (b) they have no state, so it doesn't matter if they're sync'd.

   protected interface Variation {
      String vary(String root, int n) throws Exception;
   }

   private static class StandardVariation implements Variation {
      public String vary(String root, int n) {
         return root + "_r" + Convert.fromInt(n);
      }
   }
   private static Variation standardVariation = new StandardVariation();

   protected static class SuffixVariation implements Variation {
      public String vary(String root, int n) {

         String suffix = "_r" + Convert.fromInt(n);

         int i = root.lastIndexOf('.');
         return (i == -1) ? root + suffix
                          : root.substring(0,i) + suffix + root.substring(i);
      }
   }

   protected static class PrefixVariation implements Variation {

      private int base;
      public PrefixVariation(int base) { this.base = base; }

      public int atoi(char c) {

         if (c >= '0' && c <= '9') return (c - '0');
         else throw new RuntimeException(Text.get(Format.class,"e4"));

         // the format function can throw an arbitrary exception,
         // so this runtime exception will be caught and handled.
      }

      public char itoa(int i) {
         return (char) (i + '0');
      }

      public String vary(String root, int n) {

         char[] c = root.toCharArray();
         char[] d = Convert.fromInt(n).toCharArray();

         if (base+d.length > c.length) throw new RuntimeException(Text.get(Format.class,"e5"));

         // add reversed d to the front of c, mod 10
         for (int i=0; i<d.length; i++) {
            c[base+i] = itoa( (atoi(c[base+i]) + atoi(d[(d.length-1)-i])) % 10 );
         }
         // note about Konica behavior:
         // if you have a full set of 10^NDIGIT_ORDER_ID orders and reprints,
         // this will error out when you try to add to the dot in SUFFIX_ORD.

         return new String(c);
      }
   }

   protected static class InfixVariation implements Variation {

      // a slightly different model here so that we can generate
      // the root name and the variations with the same function
      // and not have to parse out the root.

      private String prefix;
      private int digits;
      private String suffix;

      public InfixVariation(String prefix, int digits, String suffix) {
         this.prefix = prefix;
         this.digits = digits;
         this.suffix = suffix;
      }

      private String getName(int n) throws Exception {
         Convert.validateNDigit(digits,n);
         return prefix + Convert.fromIntNDigit(digits,n) + suffix;
      }

      public String getRootName() throws Exception {
         return getName(0);
      }

      public String vary(String root, int n) throws Exception {
         return getName(n);
         // root is known and ignored
      }
   }

// --- activity interface and implementations ---

   protected interface Activity {
      boolean isActive(File dir);
   }

   private static class SubstringFilter implements FileFilter {

      private int len;
      private int beginIndex;
      private int endIndex;
      private String pattern;

      public SubstringFilter(String name, int beginCut, int endCut) {
         len = name.length();
         beginIndex = beginCut;
         endIndex = len - endCut;
         pattern = name.substring(beginIndex,endIndex);
      }

      public boolean accept(File file) {
         if ( ! file.isDirectory() ) return false;

         String name = file.getName();
         return (    name.length() == len
                  && name.substring(beginIndex,endIndex).equalsIgnoreCase(pattern) );

         // case sensitivity is a problem here only in theory.
         // in practice, we generate all the names ourselves,
         // so they'll all have the same case (barring weird config changes).
         // the other activities use File.exists, not String.equals,
         // so they're fine.
      }
   }

   protected static class SubstringActivity implements Activity {

      private int beginCut;
      private int endCut;

      public SubstringActivity(int beginCut, int endCut) {
         this.beginCut = beginCut;
         this.endCut = endCut;
      }

      public boolean isActive(File dir) {
         File[] file = dir.getParentFile().listFiles(new SubstringFilter(dir.getName(),beginCut,endCut));
         return (file != null && file.length > 0);
      }
   }

   protected static class StandardActivity implements Activity {
      public boolean isActive(File dir) { return dir.exists(); }
   }
   private static Activity standardActivity = new StandardActivity();

   protected static class SuffixedActivity implements Activity {
      private String suffix;
      public SuffixedActivity(String suffix) { this.suffix = suffix; }
      public boolean isActive(File dir) { return new File(dir.getParentFile(),dir.getName() + suffix).exists(); }
   }

   protected static class RelocatedActivity implements Activity {
      private File parent;
      public RelocatedActivity(File parent) { this.parent = parent; }
      public boolean isActive(File dir) { return new File(parent,dir.getName()).exists(); }
   }

   protected static class OrActivity implements Activity {
      private Activity a1;
      private Activity a2;
      private Activity a3;
      public OrActivity(Activity a1, Activity a2, Activity a3) { this.a1 = a1; this.a2 = a2; this.a3 = a3; }
      public OrActivity(Activity a1, Activity a2) { this.a1 = a1; this.a2 = a2; }
      public boolean isActive(File dir) { return a1.isActive(dir) || a2.isActive(dir) || (a3 != null && a3.isActive(dir)); }
   }

   protected static class HashActivity implements Activity {
      private HashSet set;
      public HashActivity() { set = new HashSet(); }
      public boolean isActive(File dir) { return set.contains(dir.getName().toLowerCase()); }
      public void add(File dir) { set.add(dir.getName().toLowerCase()); }
      // this is a weird one, for the case when you're adding a bunch of files
      // to a directory all at once, and need to handle collisions with things
      // that aren't even there yet.
      // the activity framework doesn't have any way to feed the final decision
      // back to the activity, so for now you have to add it manually afterward
   }

// --- filename variation for reprints (variation plus activity) ---

   protected File vary(File root) throws Exception {
      return vary(root,standardVariation,standardActivity);
   }

   protected File vary(File root, Variation v) throws Exception {
      return vary(root,v,standardActivity);
   }

   protected File vary(File root, Activity a) throws Exception {
      return vary(root,standardVariation,a);
   }

   protected File vary(File root, Variation v, Activity a) throws Exception {

      File file = root; // initial attempt is <i>not</i> result of vary(root,0)
      int n = 1;

      while (a.isActive(file)) { file = new File(root.getParentFile(),v.vary(root.getName(),n++)); }

      return file;
   }

// --- special characters ---

   private static Pattern patternWindows = Pattern.compile("[\\\\/:*?\"<>|]");

   /**
    * Remove characters from a string so it can be part of a Windows filename.
    */
   public static String expunge(String s) {
      return patternWindows.matcher(s).replaceAll("_");
   }

// --- length limit ---

   // this is sort of like variation, but not enough that I want to unify them yet.
   // also both of them are sort of like the unique-name code in OrderParser.
   // in variation, the state is kept in the file system, but here it's in the object.

   // for Fuji, we need to remember the transformations we've done so we can
   // write the request file ... but that test is based on the original name,
   // not the transformed name, so we need both a HashMap and a HashSet.

   private static Pattern patternExclude = Pattern.compile("[^a-zA-Z0-9 '\"\\x2d_:;]");
   //
   // the guy who gave us the Fuji PIC 2.6 specification said to exclude the following
   //    ?/,`~!@#$%^&*()+
   // and also to have only one period.  this is stricter than Windows, but maybe it's
   // what Fuji wants.  anyway, consulting the ASCII characters, we obviously want to
   // allow alphanumerics, and spaces; then removing the excluded ones leaves these open.
   //    '"-_:;<=>[\]{|}
   // I arbitrarily decided that '"-_:; should be allowed, the rest not.

   protected static final int NO_LIMIT = 10000; // ugly, but at least it's factored here

   protected static class LengthLimit {

      private boolean enabled;
      private int limit;
      private boolean exclude;
      private HashSet set;
      private HashMap map;

      public LengthLimit(boolean enabled, int limit) {
         this(enabled,limit,/* exclude = */ false);
      }

      public LengthLimit(boolean enabled, int limit, boolean exclude) {
         this.enabled = enabled;
         this.limit = limit;
         this.exclude = exclude;
         this.set = new HashSet();
         this.map = new HashMap();
      }

      public String transform(String nameIn) {
         return transform(nameIn,nameIn);
      }
      public String transform(Object key, String nameIn) {

         if ( ! enabled ) return nameIn;

         String nameOut = (String) map.get(key);
         if (nameOut != null) return nameOut;

         int i = nameIn.lastIndexOf('.');
         String prefixIn = (i == -1) ? nameIn : nameIn.substring(0,i);
         String suffixIn = (i == -1) ? ""     : nameIn.substring(  i);

         if (exclude) {
            prefixIn = patternExclude.matcher(prefixIn).replaceAll("");
            suffixIn = patternExclude.matcher(suffixIn).replaceAll("");
            if (suffixIn.length() > 0) suffixIn = '.' + suffixIn;
            // the period in the suffix will be removed, if it's there,
            // but don't add it back in unless something else survived.
         }

         // there are two conditions here:
         // (1) the name length must not exceed the length limit.
         // (2) the name must be unique within this scope.
         //     for example, the first file might pass the limit,
         //     but happen to match the transformed second file.

         // also note the limit doesn't include the suffix part.

         String prefix = prefixIn; // first attempt is untransformed
         String name = prefix + suffixIn;

         int n = 0;

         while (prefix.length() > limit || set.contains(name.toLowerCase())) { // (*)

            String s = Convert.fromInt(n++);

            int len = s.length();
            if (len >= limit) throw new RuntimeException(Text.get(Format.class,"e6"));
            // I never limit to less than length 8 (MS-DOS length),
            // so you'd need to have at least 10^7 files for this to happen.

            int keep = limit-1-len;
            if (keep > prefixIn.length()) keep = prefixIn.length();

            prefix = prefixIn.substring(0,keep) + '_' + s;
            name = prefix + suffixIn;

            // using '_' isn't ideal, because it makes it harder to tell
            // whether a modification came from here or from OrderParser,
            // but I need to use a special but not-too-special character,
            // and this is the winner there.
         }

         set.add(name.toLowerCase()); // (*)
         map.put(key,name);

         // (*) we can't say "set.containsIgnoreCase", but we can get the
         // same effect by converting to lower case before adding or testing.

         return name;
      }
   }

// --- chunk iterator ---

   /**
    * An iterator that takes a quantity and a chunk size and returns
    * a sequence of integers (Integer) that add up to the quantity
    * and are divided into suitable chunks.  Actually there are two sizes,
    * so that (in the real example) we can send 999 as a single chunk
    * but then divide 1000 into 500+500 rather than an inconvenient 999+1.
    */
   protected static class ChunkIterator implements Iterator {

      private int quantity;
      private int chunkStd;
      private int chunkMax;

      public ChunkIterator(int quantity, int chunkStd, int chunkMax, boolean hasSequence) {

         if (hasSequence) {
            chunkStd = 1;
            chunkMax = 1;
         }
         // this is just a convenience, all callers do this same thing

         this.quantity = quantity;
         this.chunkStd = chunkStd;
         this.chunkMax = chunkMax;
      }

      public boolean hasNext() {
         return (quantity > 0);
      }

      public Object next() {
         int result = (quantity <= chunkMax) ? quantity : chunkStd;
         quantity -= result;
         return new Integer(result);
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }
   }

   /**
    * A function that computes the number of chunks that the iterator would return,
    * without actually constructing an object and iterating through all the chunks.
    */
   public static int getChunkCount(int quantity, int chunkStd, int chunkMax, boolean hasSequence) {

      if (hasSequence) return quantity;
      // setting chunkStd and chunkMax to 1 would give the same result, but why bother?

      //  1 - chunkMax : 1 chunk  -- wider range, no nice way to unify with other cases
      // next chunkStd : 2 chunks -- first one here is ... chunkMax+1
      // next chunkStd : 3 chunks
      // so ...
      return (quantity <= chunkMax) ? 1 : 2 + (quantity - (chunkMax+1)) / chunkStd;
   }

// --- completion ---

   // the default implementations provide valid non-completion.
   // but, if format doesn't have directory (e.g., Fuji),
   // it doesn't matter, none of these functions will be called
   // (except for Konica and Noritsu legacy code, on order dir).

   /**
    * Given the name of a print directory, put it into complete form.
    * If the form isn't unique, getComplete should return a standard form
    * for makeComplete to use,  and isComplete should recognize all forms.
    *
    * @return The final directory name, or null if there is no completion.
    */
   protected File getComplete(File dir, String property) { return null; }

   /**
    * Look at a print directory to see if it's been marked complete.
    *
    * @return The final directory name, if complete, or null if not.
    */
   public File isComplete(File dir, String property) {

      if (dir.exists()) return null; // still there, not complete

      File complete = getComplete(dir,property);
      if (complete == null) return null; // no completion, not complete

      if (complete.exists()) return complete;

      return null; // missing, call it not complete
   }

   /**
    * Same as isComplete, but the implementation has the option of
    * throwing an error, if the folder is renamed to indicate such.
    */
   public File isCompleteOrError(File dir, String property) throws Exception {
      return isComplete(dir,property);
   }

   /**
    * A special hook for formats that complete without a directory.
    */
   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {
      return false;
   }

   /**
    * Mark a print directory complete, if it hasn't already been marked.
    *
    * @return The final directory name, never null.
    */
   public File makeComplete(File dir, String property) {

      File complete = isComplete(dir,property);
      if (complete != null) return complete; // already complete

      complete = getComplete(dir,property);
      if (complete == null) return dir; // no completion, call it done

      if (dir.renameTo(complete)) return complete; // completed

      // this is always called in a context where we just want
      // to do as much as possible, and not throw any exceptions.
      // so, if it didn't work, log it.

      Log.log(Level.WARNING,Format.class,"e2",new Object[] { Convert.fromFile(dir) });

      return complete; // pretend it worked
   }

   /**
    * Relic hook to make an <i>order</i> complete.
    */
   public void makeComplete(Order order) {}

// --- special interface ---

   public interface Special {

      Object getQueueScanData(int type);
      void   setQueueScanData(int type, Object o);

      Object getQueueConfig() throws Exception;

      void queueError(Throwable t);
   }

   // the scan data is stored by queue ID, so you only need to
   // pass in a type if you have one queue with multiple types.

// --- entry point ---

   public static Format getFormat(int format) throws IOException {

      switch (format) {
      case Order.FORMAT_FLAT:     return new FormatFlat();
      case Order.FORMAT_TREE:     return new FormatTree();
      case Order.FORMAT_NORITSU:  return new FormatNoritsu();
      case Order.FORMAT_KONICA:   return new FormatKonica();
      case Order.FORMAT_FUJI:     return new FormatFuji();
      case Order.FORMAT_FUJI_NEW: return new FormatFujiNew();
      case Order.FORMAT_DLS:      return new FormatDLSStub();
      case Order.FORMAT_KODAK:    return new FormatKodak();
      case Order.FORMAT_AGFA:     return new FormatAgfa();
      case Order.FORMAT_LUCIDIOM: return new FormatLucidiom();
      case Order.FORMAT_PIXEL:    return new FormatPixel();
      case Order.FORMAT_DP2:      return new FormatDP2();
      case Order.FORMAT_BEAUFORT: return new FormatBeaufort();
      case Order.FORMAT_DKS3:     return new FormatDKS3();
      case Order.FORMAT_DIRECT_PDF: return new FormatDirectPDF();
      case Order.FORMAT_ZBE:      return new FormatZBE();
      case Order.FORMAT_FUJI3:    return new FormatFuji3();
      case Order.FORMAT_HP:       return new FormatHP();
      case Order.FORMAT_XEROX:    return new FormatXerox();
      case Order.FORMAT_DIRECT_JPEG: return new FormatDirectJPEG();
      case Order.FORMAT_BURN:     return new FormatBurn();
      case Order.FORMAT_HOT_FOLDER: return new FormatHotFolder();
      case Order.FORMAT_DNP:      return new FormatDNP();
      case Order.FORMAT_PURUS:    return new FormatPurus();
      case Order.FORMAT_RAW_JPEG: return new FormatRawJPEG();
      default:
         throw new IOException(Text.get(Format.class,"e1"));
      }
   }

}

