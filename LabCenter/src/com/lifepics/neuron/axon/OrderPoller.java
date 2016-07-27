/*
 * OrderPoller.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.Relative;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A custom poller that handles order uploads.
 */

public class OrderPoller extends FixedPrefixPoller {

   private static final String SUFFIX_XML = ".xml";

   public boolean accept(File file) {
      return super.accept(file) && endsWithIgnoreCase(file.getName(),SUFFIX_XML);
   }

// --- utilities ---

   private static Boolean toBoolean(String s) throws ValidationException {
      if      (s == null        ) return null; // must come first
      else if (s.equals("True" )) return Boolean.TRUE;
      else if (s.equals("False")) return Boolean.FALSE;
      else throw new ValidationException(Text.get(OrderPoller.class,"e1",new Object[] { s }));
   }
   // this is a one-way conversion, no need to define constants

   private static int toMethod(String s) throws ValidationException {
      if      (s == null       ) return RollManager.METHOD_COPY; // default, and must come first
      else if (s.equals("COPY")) return RollManager.METHOD_COPY;
      else if (s.equals("MOVE")) return RollManager.METHOD_MOVE;
      else if (s.equals("COPY_AND_DELETE")) return RollManager.METHOD_COPY_AND_DELETE;
      else throw new ValidationException(Text.get(OrderPoller.class,"e3",new Object[] { s }));
   }

   private static String getAndRemoveNullable(Node node, String name) throws ValidationException {

      // the model here is getNullableText, which just gets the first instance
      // and ignores the rest.  here, though, we can't ignore them, since then
      // we'd leave them sitting in the final file.  so, remove them all.

      Node child = XML.getElementTry(node,name);
      String result = (child == null) ? null : XML.getText(child);

      while (child != null) {
         node.removeChild(child);
         child = XML.getElementTry(node,name);
      }

      return result;
   }

// --- main function ---

   protected int processRenamed(File original, File file, PollCallback callback) throws Exception {

      Document doc = XML.readFile(file);
      Node order = XML.getElement(doc,Roll.UPLOAD_TAG_ORDER);

      XML.removeWhitespace(order); // might as well do now
      // if we don't remove and re-add, the FileBase and FileMode removal
      // can leave blank lines with spaces; also I like to guarantee that
      // the indentation is tidy, since LC owns the upload order XML file.

   // create roll

      Roll roll = new Roll();

      roll.source = Roll.SOURCE_HOT_ORDER;

   // section - order info

      Node orderInfo = XML.getElement(order,Roll.UPLOAD_TAG_ORDER_INFO);

      String locationID = XML.getElementText(orderInfo,Roll.UPLOAD_TAG_LOCATION_ID);
      roll.dealer = callback.getDealerForLocation(locationID); // usually just null

      roll.expectedCustomerID = XML.getNullableText(orderInfo,Roll.UPLOAD_TAG_CUSTOMER_ID);
      roll.notify = Boolean.FALSE;
      // there *is* a field "EmailReceipt" that tells whether to notify,
      // but the SubmitOrder service will read it and respond appropriately;
      // if the regular upload process notified, it would be a duplicate.

      boolean account = toBoolean(XML.getElementText(orderInfo,"CreateAccount")).booleanValue(); // always there
      if (account) {
         roll.album = Roll.getValidAlbum(XML.getNullableText(orderInfo,"Album"));
         roll.promote = toBoolean(XML.getNullableText(orderInfo,"EmailPromotions"));
         roll.password = XML.getNullableText(orderInfo,"AccountPassword");
      }
      // else just leave null.  we could set the album on the guest account,
      // but there's no reason to.

      // why do we set expectedCustomerID and notify even if the account flag is false?
      // if there's no account, there'll be no email address, so setting the notify
      // flag is academic, but I like the solidity of it.  as for the customer ID, well,
      // of course it shouldn't be present if there's no email address, but if it is,
      // we want to save the value and error out, rather than uploading to a guest account
      // and then placing the order against some other account.

   // section - billing

      Node billTo = XML.getElement(order,"BillTo");

      if (account) {
         roll.email = XML.getNullableText(billTo,"Email");
         roll.nameFirst = XML.getElementText(billTo,"FirstName"); // always there
         roll.nameLast = XML.getElementText(billTo,"LastName");   // always there
         roll.street1 = XML.getNullableText(billTo,"Address1");
         roll.street2 = XML.getNullableText(billTo,"Address2");
         roll.city = XML.getNullableText(billTo,"City");
         roll.state = XML.getNullableText(billTo,"State");
         roll.zipCode = XML.getNullableText(billTo,"PostalCode");
         roll.phone = XML.getNullableText(billTo,"Phone");
         roll.country = XML.getNullableText(billTo,"Country");
      }
      // if we pass an email address and so on to the regular uploader,
      // it will create an account if it doesn't already exist, period.
      // so, we have to check the CreateAccount flag ourselves and not
      // pass an email address if it's not set.
      // the other fields wouldn't do any harm, but it's cleaner to leave them out.

      if (roll.email == null) roll.email = ""; // nullable in XML file but not in LC

   // files

      // see note in XMLPoller about order of files and subalbums.
      // the big difference here is that the same file can appear
      // on more than one order item.
      // also we need to pre-disambiguate the items so we can put
      // the names in the XML document.

      File base = file.getParentFile();

      String sBase = getAndRemoveNullable(order,"FileBase");
      if (sBase != null) {
         File f = new File(sBase);
         f = Relative.makeRelativeTo(base,f);
         base = f;
      }

      String sMode = getAndRemoveNullable(order,"FileMode");
      int method = toMethod(sMode); // null gets defaulted

      HashMap map = new HashMap();
      HashSet set = new HashSet();

      Iterator h = XML.getElements(order,Roll.UPLOAD_TAG_ORDER_ITEM);
      while (h.hasNext()) {
         Node orderItem = (Node) h.next();

         Node images = XML.getElement(orderItem,Roll.UPLOAD_TAG_IMAGES);

         Iterator i = XML.getElements(images,"File");
         while (i.hasNext()) {
            Node node = (Node) i.next();
            String s = XML.getText(node);

            // the hash map takes the string contents of the File node
            // to the disambiguated filename of that image in the roll.
            // it would be nicer if the key was the unique identity of the file,
            // independent of the path, but (a) File.equals doesn't detect that,
            // (b) I don't want to bother with getCanonicalPath, and
            // (c) there's no reason to expect different paths to the same file.

            // it's helpful to compare this code to the file code in XMLPoller.
            // one thing to note is, if an image appears more than once,
            // the file attributes on the later instances are totally ignored.

            String name = (String) map.get(s);
            if (name == null) {

               File f = new File(s);
               f = Relative.makeRelativeTo(base,f);

               if ( ! f.exists() ) throw new Exception(Text.get(this,"e2",new Object[] { s }));

               Roll.PreItem item = ItemUtil.makeItem(f);
               XMLPoller.parseFileAttributes(node,item);
               roll.items.add(item);

               ItemUtil.disambiguateItem(set,item);
               // doesn't matter that we've already added it to the roll, that's not used

               name = item.filename;
               map.put(s,name);
            }

            XML.replaceElementText(images,node,Roll.UPLOAD_TAG_FILENAME,name);
            // we can replace nodes without breaking the iteration
         }
      }

      roll.extraFiles.add(new File(original.getParentFile(),getFinalName(original)).getPath());
      // in FujiPoller we knew the relative location and used a relative path,
      // but here we're going to be copying the files as usual, so the relative path is tricky

   // done

      XML.addIndentation(order);

      return callback.submitWithItems(roll,method,doc); // this logs on success
   }

}

