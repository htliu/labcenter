/*
 * Roll.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Reportable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.Rotation;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.*;
import com.lifepics.neuron.thread.Entity;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.logging.Level;

import org.w3c.dom.Node;

/**
 * An object that holds all the information about a roll.
 */

public class Roll extends Structure implements Reportable, Entity {

// --- fields ---

   public int rollID;
   public String bagID; // now nullable, used only for DLS ID
   public int source;

   public String email;

   public File rollDir;
   public int status;
   public int hold;
   public String lastError; // nullable

   public Date receivedDate; // for local images only
   public Date recmodDate;
   public Date failDateFirst; // nullable
   public Date failDateLast;  //

   public LinkedList items; // files, in alphabetical order
   public LinkedList scans; // list of string scan IDs; *list object* is nullable

   public LinkedList extraFiles; // list of string paths for files that are owned but not uploaded
   public String expectedCustomerID; // null unless order upload *and* customer ID was filled in

   public Upload upload; // possibly null
   public Integer transformType; // nullable, and mutually exclusive with transformConfig
   public TransformConfig transformConfig; // nullable

   // optional fields
   public String nameFirst;
   public String nameLast;
   public String street1;
   public String street2;
   public String city;
   public String state;
   public String zipCode;
   public String phone;
   public String country;
   public String album;
   public Boolean notify;
   public Boolean promote; // email promotions
   public String password; // password for new account, not for individual roll
   public String claimNumber;
   public String jobNumber; // a photography session, not a print job

   // optional fields for LC Pro
   public String eventCode;    // this is an arbitrary string identifier
   public PriceList priceList; // the name is only here as a cached copy

   // required field for wholesale operation
   public Dealer dealer;
   // the ID is what matters, but the others can be used in special circumstances

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Roll.class,
      0,2,
      new AbstractField[] {

         new IntegerField("rollID","RollID"),
         new NullableStringField("bagID","BagID"),
         new EnumeratedField("source","Source",RollEnum.sourceType),

         new StringField("email","Email"),

         new FileField("rollDir","RollDir"),
         new EnumeratedField("status","Status",RollEnum.rollStatusType,1,0),
         new EnumeratedField("hold","Hold",RollEnum.rollHoldType,1,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               Roll roll = (Roll) o;
               int oldshc = RollEnum.toOldSHC(XML.getElementText(node,"Status"));
               switch (oldshc) {

               case OLDSHC_PENDING:     roll.status = STATUS_ROLL_PENDING;    roll.hold = HOLD_ROLL_NONE;   break;
               case OLDSHC_SENDING:     roll.status = STATUS_ROLL_SENDING;    roll.hold = HOLD_ROLL_NONE;   break;
               case OLDSHC_COMPLETED:   roll.status = STATUS_ROLL_COMPLETED;  roll.hold = HOLD_ROLL_NONE;   break;
               case OLDSHC_DELETED:     roll.status = STATUS_ROLL_DELETED;    roll.hold = HOLD_ROLL_NONE;   break;

               case OLDSHC_HOLD_EMAIL:  roll.status = STATUS_ROLL_PENDING;    roll.hold = HOLD_ROLL_EMAIL;  break;
               case OLDSHC_HOLD_ERROR:  roll.status = STATUS_ROLL_PENDING;    roll.hold = HOLD_ROLL_ERROR;  break;
               case OLDSHC_HOLD_USER:   roll.status = STATUS_ROLL_PENDING;    roll.hold = HOLD_ROLL_USER;   break;
               case OLDSHC_HOLD_RETRY:  roll.status = STATUS_ROLL_PENDING;    roll.hold = HOLD_ROLL_RETRY;  break;
               }
            }
            // note, order matters .. if you put this on the status field,
            // the hold field's loadDefault would overwrite the hold code.
         },
         new NullableStringField("lastError","LastError"),

         new NullableDateField("receivedDate","ReceivedDate"),
         new DateField("recmodDate","RecmodDate"),
         new NullableDateField("failDateFirst","FailDateFirst"),
         new NullableDateField("failDateLast","FailDateLast"),

         new StructureListField("items","Item",Item.sd,Merge.NO_MERGE),
         new NullableStringListField("scans","Scans","Scan"),

         new InlineListField("extraFiles","ExtraFile"),
         new NullableStringField("expectedCustomerID","ExpectedCustomerID",2,null),

         new NullableStructureField("upload","Upload",Upload.sd),
         new NullableEnumeratedField("transformType","TransformType",RollEnum.transformTypeType),
         new NullableStructureField("transformConfig","TransformConfig",TransformConfig.sd),

         new NullableStringField("nameFirst","FirstName"),
         new NullableStringField("nameLast","LastName"),
         new NullableStringField("street1","Street1"),
         new NullableStringField("street2","Street2"),
         new NullableStringField("city","City"),
         new NullableStringField("state","State"),
         new NullableStringField("zipCode","ZipCode"),
         new NullableStringField("phone","Phone"),
         new NullableStringField("country","Country"),
         new NullableStringField("album","Album"),
         new NullableBooleanField("notify","Notify"),
         new NullableBooleanField("promote","Promote"),
         new NullableStringField("password","Password"),
         new NullableStringField("claimNumber","ClaimNumber"),
         new NullableStringField("jobNumber","JobNumber"),

         new NullableStringField("eventCode","EventCode"),
         new NullableStructureField("priceList","PriceList",PriceList.sd),

         new NullableStructureField("dealer","Dealer",Dealer.sd)
      });

   protected StructureDefinition sd() { return sd; }

// --- implementation of Reportable ---

   public String getReportType() {
      return Log.ROLL;
   }

   public String getReportID() {
      return Convert.fromInt(rollID);
   }

   public String getReportMerchant() {
      return null;
   }

   public String getLocalOrderSeq() {
      return null;
   }

   public int getLocalOrderID() {
      return 0; // not used because no seq
   }

   public int getLocalStatusCode() {
      return 0; // not used because no seq
   }

// --- implementation of Entity ---

   public String getID() {
      return Convert.fromInt(rollID);
   }

   public boolean testStatus(int statusFrom, int statusActive) {
      return (    (    status == statusFrom
                    || status == statusActive )
               && hold == HOLD_ROLL_NONE );
   }

   public void setStatus(int status) {
      this.status = status;
      recmodDate = new Date();
   }

   public void setStatusError(int statusFrom, String lastError, boolean told,
                              boolean pauseRetry, long pauseRetryLimit) {
      Date now = new Date();

      if (pauseRetry) {

         // now we can check the condition on pauseRetryLimit; see DiagnoseHandler.
         // it is: if we've failed before, and are past the limit, no more retries.

         if (    failDateFirst != null
              && now.getTime() >= failDateFirst.getTime() + pauseRetryLimit ) {

            pauseRetry = false;
         }
      }

      status = statusFrom;
      hold = pauseRetry ? HOLD_ROLL_RETRY : HOLD_ROLL_ERROR;
      this.lastError = lastError;
      recmodDate = now;
      if (pauseRetry) {
         if (failDateFirst == null) failDateFirst = now;
         failDateLast = now;
      }
   }

   // not part of Entity, but goes with setStatusError
   public void endRetry(Object o, String key) {

      if (failDateFirst != null) {
         // report as if severe, to fit with the original errors
         Log.log(this,Level.INFO,o,key,new Object[] { Convert.fromInt(rollID) },null,Level.SEVERE);
      }

      failDateFirst = null;
      failDateLast = null;
      //
      // it would make sense to put this part inside the if-test,
      // but it used to be unconditional, let's keep it that way.
      // also, what if somehow (hand editing) failDateFirst gets
      // set to null even though failDateLast isn't?  no problem
      // if we don't send a success notification, but the fields
      // should still get cleared.
   }

   public boolean isRetrying() {
      return (      hold == HOLD_ROLL_RETRY
               && ! failDateLast.equals(failDateFirst) );
   }

// --- order upload constants ---

   public static final String UPLOAD_FILE = "upload.xml"; // for order upload

   // important tags here, less important ones in hot folder code
   //
   public static final String UPLOAD_TAG_ORDER = "Order";
   //
   public static final String UPLOAD_TAG_ORDER_INFO = "OrderInfo";
   public static final String UPLOAD_TAG_LOCATION_ID = "LocationID";
   public static final String UPLOAD_TAG_CUSTOMER_ID = "CustomerID";
   //
   public static final String UPLOAD_TAG_ORDER_ITEM = "OrderItem";
   public static final String UPLOAD_TAG_IMAGES = "Images";
   public static final String UPLOAD_TAG_FILENAME = "Filename";
   public static final String UPLOAD_TAG_IMAGE = "Image";

// --- another constant ---

   public static final String ANONYMOUS_ACCOUNT = "anonymous@lifepics.com";

// --- roll source enumeration ---

   private static final int SOURCE_MIN = 0;

   public static final int SOURCE_MANUAL = 0;
   public static final int SOURCE_PAKON = 1;
   public static final int SOURCE_HOT_SIMPLE = 2;
   public static final int SOURCE_HOT_XML = 3;
   public static final int SOURCE_DLS = 4;
   public static final int SOURCE_HOT_FUJI = 5;
   public static final int SOURCE_HOT_ORDER = 6;
   public static final int SOURCE_HOT_EMAIL = 7;
   public static final int SOURCE_HOT_IMAGE = 8;
   public static final int SOURCE_LOCAL = 9;

   private static final int SOURCE_MAX = 9;

   // originally this was just a tag to show where the upload came from,
   // but over time we've added some logic based on it.
   //
   // * there are different purge enables for different sources
   // * email defaulting is restricted to Fuji and Image poller
   // * recent addition of a bunch of SOURCE_LOCAL stuff
   // * recent replacement of isOrderUpload with SOURCE_HOT_ORDER stuff
   //
   // you're probably better off searching the code for details, but
   // here's a quick summary.
   //
   // * local and order uploads allow blank email and are not editable
   // * order upload purges the order file; local upload doesn't rmdir
   // * local upload must have one image with image ID, and that ID is
   //   not cleared on rebuild
   // * various differences in UploadThread, see the code there

// --- old status-hold code enumeration ---

   private static final int OLDSHC_MIN = 0;

   public static final int OLDSHC_PENDING = 0;
   public static final int OLDSHC_SENDING = 1;

   public static final int OLDSHC_HOLD_EMAIL = 2;
   public static final int OLDSHC_HOLD_ERROR = 3;
   public static final int OLDSHC_HOLD_USER = 4;
   public static final int OLDSHC_HOLD_RETRY = 5;

   public static final int OLDSHC_COMPLETED = 6;
   public static final int OLDSHC_DELETED = 7;

   private static final int OLDSHC_MAX = 7;

// --- roll status enumeration ---

   private static final int STATUS_ROLL_MIN = 0;

   public static final int STATUS_ROLL_SCANNED = 0;
   public static final int STATUS_ROLL_COPYING = 1;
   public static final int STATUS_ROLL_PENDING = 2;
   public static final int STATUS_ROLL_SENDING = 3;
   public static final int STATUS_ROLL_COMPLETED = 4;
   public static final int STATUS_ROLL_DELETED = 5;

   private static final int STATUS_ROLL_MAX = 5;

// --- roll hold enumeration ---

   private static final int HOLD_ROLL_MIN = 0;

   public static final int HOLD_ROLL_NONE = 0;
   public static final int HOLD_ROLL_EMAIL = 1;
   public static final int HOLD_ROLL_ERROR = 2;
   public static final int HOLD_ROLL_USER = 3;
   public static final int HOLD_ROLL_RETRY = 4;
   public static final int HOLD_ROLL_CONFIRM = 5;
   public static final int HOLD_ROLL_DEALER = 6;

   private static final int HOLD_ROLL_MAX = 6;

// --- transform type enumeration ---

   public static final int TRANSFORM_TYPE_MIN = 0;

   public static final int TRANSFORM_TYPE_REGULAR = 0;
   public static final int TRANSFORM_TYPE_FAST    = 1;
   public static final int TRANSFORM_TYPE_FASTEST = 2;

   public static final int TRANSFORM_TYPE_MAX = 2;

// --- file status enumeration ---

   private static final int STATUS_FILE_MIN = 0;

   public static final int STATUS_FILE_PENDING = 0;
   public static final int STATUS_FILE_SENDING = 1;
   public static final int STATUS_FILE_SENT = 2;

   private static final int STATUS_FILE_MAX = 2;

// --- helper functions ---

   public boolean isPurgeableStatus() {
      return (    status == STATUS_ROLL_COMPLETED
               || status == STATUS_ROLL_DELETED   );
   }

   public Item findItemByFilename(String filename) {
      Iterator i = items.iterator();
      while (i.hasNext()) {
         Item item = (Item) i.next();
         if (item.filename.equals(filename)) return item;
      }
      return null;
   }

   public boolean hasSubalbums() {
      Iterator i = items.iterator();
      while (i.hasNext()) {
         Item item = (Item) i.next();
         if (item.subalbum != null) return true;
      }
      return false;
   }

// --- check / limit ---

   // this code looks like it should be merged with validateAlbum / getValidAlbum,
   // below, but we can't do that ... there are already bad album names out there
   // in the field, so if we impose new requirements, LC won't start.
   // I'm surprised I didn't get into trouble with that with the slashes, actually.
   //
   // anyway, what we'll do instead is, have the same kinds of function pairs,
   // but the check function will be called during weak validation,
   // and the limit function will be called at upload time, not creation time.
   // (getValidAlbum is called at creation time for non-manual roll sources.)

   // these satisfy the identity s = getPrefix(s) + getSuffix(s)

   public static String getPrefix(String s) {
      int i = s.lastIndexOf('.');
      return (i == -1) ? s  : s.substring(0,i);
   }

   public static String getSuffix(String s) {
      int i = s.lastIndexOf('.');
      return (i == -1) ? "" : s.substring(i); // including dot
   }

   private static String limit(String s, int len) {
      return (s.length() <= len) ? s : s.substring(0,len);
   }

   private static final int LENGTH_ALBUM_MAX  =  50;
   private static final int LENGTH_FILE_MAX   = 100;
   private static final int LENGTH_SUFFIX_MAX =  20; // arbitrary but very weak requirement

   private static final String illegalChars = "[\\\\/:*?\"<>|']+"; // also in e12
   //
   // this list contains all the characters that Windows doesn't allow in file names,
   // because the server is a Windows box and uses the name LC sends as the actual name
   // on disk, plus apostrophe, because the server also uses the name in database calls
   // without enquoting it.

   private static final String illegalAlbum = "[<>']+"; // also in e15 and e18
   //
   // the subset of illegal characters that don't work in album names.
   // I thought I'd tested them all and they worked, but not any more.
   //
   // status of characters as of 1/18/2013:
   // * slash and backslash are excluded by the album-subalbum logic
   // * standard method, server sends back invalid XML for < and '
   // * web services method, ' works but server silently drops < and anything after
   // * excluding > just for consistency, it works with both methods
   // * all other ASCII works

   public static String getIllegalAlbumInfo() {
      return Text.get(Roll.class,"e18",new Object[] { Convert.fromInt(LENGTH_ALBUM_MAX) });
   }

   public static String limitAlbum(String album) {

      album = album.replaceAll(illegalAlbum," ").trim();
      album = limit(album,LENGTH_ALBUM_MAX).trim();
      // need to re-trim to prevent trailing spaces from appearing

      return album;
   }

   public static String limitFilename(String prefixPart, String suffixPart) {
      String prefix = getPrefix(prefixPart);
      String suffix = getSuffix(suffixPart);

      prefix = prefix.replaceAll(illegalChars," ").trim();
      suffix = suffix.replaceAll(illegalChars," ").trim();
      // suffix trim isn't so good because of dot, but it won't matter,
      // especially since the suffix is from the real file name, which
      // the user can't edit.
      // note we replace a string of illegal chars with a single space.

      // have to guarantee prefix length limit will be positive
      suffix = limit(suffix,LENGTH_SUFFIX_MAX).trim();
      prefix = limit(prefix,LENGTH_FILE_MAX-suffix.length()).trim();

      return prefix + suffix;
   }
   // the server doesn't mind receiving several files with the same name.
   // this is very fortunate, because we don't know the final file names
   // until we do the upload (because of the transformation step).

   private static boolean checkFilename(String prefixPart, String suffixPart) {
      String prefix = getPrefix(prefixPart);
      String suffix = getSuffix(suffixPart);

      prefix = prefix.replaceAll(illegalChars," ").trim();
      suffix = suffix.replaceAll(illegalChars," ").trim();

      int plen = prefix.length();
      int slen = suffix.length();

      if (slen < 4) slen = 4;
      // we don't know whether the transform filename will be used,
      // so validate to fit either way.
      // the number is 4 rather than 3 because of the filename dot.
      // here we're assuming that the transform ends in ".jpg"!

      return (           slen <= LENGTH_SUFFIX_MAX
               && plen + slen <= LENGTH_FILE_MAX   );
   }

   public LinkedList checkNames() {
      LinkedList errorList = new LinkedList();

      // easiest to integrate this all into a single function,
      // because of the list and because the file name errors
      // are grouped together.

      if (album != null) {

         if ( ! album.replaceAll(illegalAlbum," ").equals(album) ) errorList.add(Text.get(this,"e15"));

         int extra = (album.length() - LENGTH_ALBUM_MAX);
         if (extra > 0) errorList.add(Text.get(this,"e10",new Object[] { new Integer(extra), Convert.fromInt(extra), Convert.fromInt(LENGTH_ALBUM_MAX) }));
      }

      boolean tooLong = false;
      boolean illegal = false;

      Iterator i = items.iterator();
      while (i.hasNext()) {
         Item item = (Item) i.next();

         String original = item.getOriginalFilename();
         if ( ! original.replaceAll(illegalChars," ").equals(original) ) illegal = true;
         //
         // the suffix of the original filename isn't really used,
         // but to give consistent behavior, report illegal chars there too.
         // the real suffix comes from item.filename or the transform file,
         // but the user can't edit either of those, so there, don't report.

         if ( ! checkFilename(original,item.filename) ) tooLong = true;
         //
         // if we've reported illegal chars and the user is fine with that,
         // we don't want to also report that the string is too long
         // if in fact it's going to get shorter when the chars are removed.
         // so, do the regular transform and look for length limiting.
      }

      if (tooLong) errorList.add(Text.get(this,"e11",new Object[] { Convert.fromInt(LENGTH_FILE_MAX) }));
      if (illegal) errorList.add(Text.get(this,"e12"));

      return errorList;
   }

// --- validation ---

   public static void validateSource(int source) throws ValidationException {

      if (source < SOURCE_MIN || source > SOURCE_MAX) {
         throw new ValidationException(Text.get(Roll.class,"e2",new Object[] { Convert.fromInt(source) }));
      }
   }

   public static void validateAlbum(String album) throws ValidationException {
      validateAlbum(album,/* claimNumber = */ false);
   }
   public static void validateAlbum(String album, boolean claimNumber) throws ValidationException {
      if (    album.indexOf('/' ) != -1
           || album.indexOf('\\') != -1 ) throw new ValidationException(Text.get(Roll.class,claimNumber ? "e9b" : "e9a"));
   }

   // we can't really validate that the subalbum doesn't contain slashes,
   // because of course it can .... that's how we store nested subalbums.
   // but, at least we can put this here with validateAlbum,
   // and call it in the right place in the UI subalbum editing sequence.
   //
   public static void validateSubalbum(String subalbum) throws ValidationException {
      if (    subalbum.indexOf('/' ) != -1
           || subalbum.indexOf('\\') != -1 ) throw new ValidationException(Text.get(Roll.class,"e13"));
   }

   public static String getValidAlbum(String album) {

      if (album == null) return null; // convenience

      album = album.replace('/', '-');
      album = album.replace('\\','-');
      return album.trim();

      // this is for when we get a roll from an automatic source
      // and don't want to error out with an invalid character.
      // the most likely cause is, the album is a date that uses '/'.

      // oh, also, we want to trim, because trailing spaces were
      // causing trouble in the XML uploads.  this is not a very
      // systematic solution ... we should really trim any field
      // that arrives through any interface.  or should we error?
   }

   public void validate() throws ValidationException {

      validateSource(source);

      if (status < STATUS_ROLL_MIN || status > STATUS_ROLL_MAX) {
         throw new ValidationException(Text.get(this,"e3",new Object[] { Convert.fromInt(status) }));
      }

      if (hold < HOLD_ROLL_MIN || hold > HOLD_ROLL_MAX) {
         throw new ValidationException(Text.get(this,"e4",new Object[] { Convert.fromInt(hold) }));
      }

      // we don't want to be validating email addresses
      // during ordinary database loading,
      // so in this context the email address is just an arbitrary string.
      // email validation is tied into the whole process,
      // particularly to HOLD_ROLL_EMAIL.

      ListIterator li = items.listIterator();
      while (li.hasNext()) {
         ((Item) li.next()).validate();
      }

      if (upload != null) upload.validate();

      if (transformType != null) {

         int type = transformType.intValue();
         if (type < TRANSFORM_TYPE_MIN || type > TRANSFORM_TYPE_MAX) {
            throw new ValidationException(Text.get(this,"e16",new Object[] { Convert.fromInt(type) }));
         }

         if (transformConfig != null) {
            throw new ValidationException(Text.get(this,"e17"));
         }
      }
      if (transformConfig != null) transformConfig.validate();

      if (album != null) validateAlbum(album);

      if (priceList != null) priceList.validate();

      if (dealer != null) dealer.validate();
      // the dealer is like the email address .... only more so, because
      // the validation depends on isWholesale, which changes at runtime.

      // special conditions for local uploads
      // local uploads have a very restricted set of fields, see LocalImageThread,
      // but I want to check these conditions up front so that I can define
      // getLocalImageID without having to check for imaginary cases all the time
      if (source == SOURCE_LOCAL) {
         if (items.size() != 1) throw new ValidationException(Text.get(this,"e19"));
         Item item = (Item) items.getFirst();
         if (item.imageID == null) throw new ValidationException(Text.get(this,"e20"));
      }
   }

   public String getLocalImageID() {
      return (source == SOURCE_LOCAL) ? ((Item) items.getFirst()).imageID : null;
      // by validation, the code in the first case always works, and is not null
   }

   public void assertWholesale(boolean isWholesale) throws IOException {
      // this is a last-ditch test, so the messages are a bit formal and unhelpful
      boolean isWholesaleRoll = (dealer != null);
      if (isWholesale != isWholesaleRoll) throw new IOException(Text.get(this,isWholesale ? "e14a" : "e14b"));
   }

// --- item class ---

   public static class Item extends Structure {

      public String filename;
      public int status;
      public String originalFilename; // nullable (different than download side!)
      public String subalbum; // nullable
      public int rotation; // rotation, 0-3 for 0-270 degrees of CCW rotation
                           // the value 0 is represented by a null in storage
      public String imageID; // null until image uploaded (unless local image)

      public Item() {}
      public Item(PreItem item) {

         filename = item.filename;
         status = item.status;
         originalFilename = item.originalFilename;
         subalbum = item.subalbum;
         rotation = item.rotation;
         imageID = item.imageID; // for completeness

         // it'd be nice to use the structure for this, but unfortunately
         // the structure only knows how to copy if there's cloning first
      }

      public static final StructureDefinition sd = new StructureDefinition(

         Item.class,
         // no version
         new AbstractField[] {

            new StringField("filename","Filename"),
            new EnumeratedField("status","Status",RollEnum.fileStatusType),
            new NullableStringField("originalFilename","OriginalFilename"),
            new NullableStringField("subalbum","Subalbum"),
            new IntegerField("rotation","Rotation") {
               protected void loadNormal(Node node, Object o) throws ValidationException {
                  String temp = XML.getNullableText(node,xmlName);
                  int i = (temp == null) ? 0 : Convert.toInt(temp);
                  tset(o,i);
               }
               public void store(Node node, Object o) {
                  int i = tget(o);
                  String temp = (i == 0) ? null : Convert.fromInt(i);
                  XML.createNullableText(node,xmlName,temp);
               }
            },
            new NullableStringField("imageID","ImageID")
         });

      protected StructureDefinition sd() { return sd; }

      public String getOriginalFilename() { return (originalFilename != null) ? originalFilename : filename; }
      public void setOriginalFilename(String s) { originalFilename = (s == null || s.equals(filename)) ? null : s; }
         // although I handle it, setting the name to null is usually not the best approach

      public void validate() throws ValidationException {

         if (status < STATUS_FILE_MIN || status > STATUS_FILE_MAX) {
            throw new ValidationException(Text.get(Roll.class,"e1",new Object[] { Convert.fromInt(status) }));
         }

         Rotation.validate(rotation);
      }
   }

   /**
    * Class used for keeping track of sources when adding new files.
    */
   public static class PreItem extends Item {

      public File file;
   }

// --- upload class ---

   // there are some validations on these fields, but it's complicated,
   // better to let UploadThread handle it.  all other uses of the
   // upload object should only depend on whether it's null or not null.

   public static class Upload extends Structure {

      public Boolean lockdown;
      public Boolean watermark;
      public Boolean exclusive;
      public int uploadID;
      public String uploadURL;
      public String errorURL;
      public String completeURL;
      public String PID;
      public String merchantID;       // WS = for web services upload only
      public String customerID;       // null for old in-progress uploads
      public String albumID;          // WS
      public LinkedList subalbumInfo; // WS
      public Boolean eventCodeDone;   // WS
      public Boolean uploadEmailDone; // WS
      public String orderID;          // null until order upload finishes
      public String extraSessionID;   // WS

      public static final StructureDefinition sd = new StructureDefinition(

         Upload.class,
         // no version
         new AbstractField[] {

            new NullableBooleanField("lockdown","Lockdown"),
            new NullableBooleanField("watermark","Watermark"),
            new NullableBooleanField("exclusive","Exclusive"),
            new IntegerField("uploadID","UploadID"),
            new NullableStringField("uploadURL","UploadURL"),
            new NullableStringField("errorURL","ErrorURL"),
            new NullableStringField("completeURL","CompleteURL"),
            new StringField("PID","PID"),
            new NullableStringField("merchantID","MerchantID"),
            new NullableStringField("customerID","CustomerID"),
            new NullableStringField("albumID","AlbumID"),
            new StructureListField("subalbumInfo","SubalbumInfo",SubalbumInfo.sd,Merge.NO_MERGE),
            new NullableBooleanField("eventCodeDone","EventCodeDone"),
            new NullableBooleanField("uploadEmailDone","UploadEmailDone"),
            new NullableStringField("orderID","OrderID"),
            new NullableStringField("extraSessionID","ExtraSessionID")
         });

      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException {

         Iterator i = subalbumInfo.iterator();
         while (i.hasNext()) {
            ((SubalbumInfo) i.next()).validate();
         }
      }
   }

// --- subalbum info class ---

   public static class SubalbumInfo extends Structure {

      public String name;
      public String id;

      public static final StructureDefinition sd = new StructureDefinition(

         SubalbumInfo.class,
         // no version
         new AbstractField[] {

            new StringField("name","Name"),
            new StringField("id","ID")
         });

      static { sd.setAttributed(); }
      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException {
      }
   }

}

