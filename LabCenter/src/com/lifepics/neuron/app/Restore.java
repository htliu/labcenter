/*
 * Restore.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.axon.ProMode;
import com.lifepics.neuron.core.ChainedException;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.ButtonHelper;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.gui.ViewHelper;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.CompoundComparator;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NaturalComparator;
import com.lifepics.neuron.meta.ReverseComparator;
import com.lifepics.neuron.net.GetTransaction;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.net.ServiceUtil;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.table.ListView;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimeZone;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Static code for the configuration restore function, which is the
 * one where we read and use a config file from another instance ID.
 */

public class Restore {

// --- server instance ---

   private static class ServerInstance {
      public int instanceID;
      public int passcode;
      public Date timestamp; // last modified, I think
   }

// --- accessors ---

   private static Accessor instanceIDAccessor = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer( ((ServerInstance) o).instanceID ); }
   };

   private static Accessor timestampAccessorRaw = new Accessor() {
      public Class getFieldClass() { return Date.class; }
      public Object get(Object o) { return ((ServerInstance) o).timestamp; }
   };

   private static Accessor timestampAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromDateExternal( ((ServerInstance) o).timestamp ); }
   };

   private static class NoteAccessor implements Accessor {

      private Integer thisInstanceID;
      private String note;
      public NoteAccessor(Integer thisInstanceID) { this.thisInstanceID = thisInstanceID; note = Text.get(Restore.class,"s2"); }

      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return match(o,thisInstanceID) ? note : ""; }
   };

// --- comparators ---

   private static Comparator instanceIDComparator = new FieldComparator(instanceIDAccessor,  new NaturalComparator());
   private static Comparator timestampComparator  = new FieldComparator(timestampAccessorRaw,new NaturalComparator());

   private static Comparator mainComparator = new CompoundComparator(new ReverseComparator(timestampComparator),instanceIDComparator);

// --- columns ---

   private static GridColumn makeColumn(String suffix, Accessor accessor, Comparator comparator) {
      return new GridColumn(Text.get   (Restore.class,"n" + suffix),
                            Text.getInt(Restore.class,"w" + suffix),accessor,comparator);
   }

// --- dialog ---

   public static class RestoreDialog extends JDialog {
      private Config result;

      private String autoConfigURL;

      public RestoreDialog(Frame owner, LinkedList instances, Integer thisInstanceID, String autoConfigURL) {
         super(owner,Text.get(Restore.class,"s3"),/* modal = */ true);

         this.autoConfigURL = autoConfigURL;

         setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
         addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { doCancel(); } });

         setResizable(false);

      // fields

         GridColumn[] cols = new GridColumn[] {
               makeColumn("1",timestampAccessor,timestampComparator),
               makeColumn("2",new NoteAccessor(thisInstanceID),null)
            };
         // since one is dynamic, just construct them both on the fly

         ListView view = new ListView(instances,null);
         ViewHelper viewHelper = new ViewHelper(view,Text.getInt(Restore.class,"c1"),cols,
            new ViewHelper.DoubleClick() { public void run(Object o) { doSelect(o); } });

         int row = findFirstNonLocalInstance(instances,thisInstanceID);
         if (row != -1) viewHelper.getTable().setRowSelectionInterval(row,row);
         // else we don't want to suggest anything, just leave unselected
         //
         // why don't we just remove the local instances from the list?
         // one, I think it might be confusing to have one missing;
         // two, restore from last local backup might be a nice feature.

         JPanel fields = new JPanel();
         GridBagHelper helper = new GridBagHelper(fields);

         int y = 0;
         int d3 = Text.getInt(Restore.class,"d3");

         helper.add(0,y++,new JLabel(Text.get(Restore.class,"s6")));
         helper.add(0,y++,new JLabel(Text.get(Restore.class,"s7")));
         helper.add(0,y++,new JLabel(Text.get(Restore.class,"s8")));
         helper.add(0,y++,new JLabel(Text.get(Restore.class,"s9")));

         helper.add(0,y++,Box.createVerticalStrut(d3));

         helper.add(0,y++,new JLabel(Text.get(Restore.class,"s10")));
         helper.add(0,y++,new JLabel(Text.get(Restore.class,"s11")));
         helper.add(0,y++,new JLabel(Text.get(Restore.class,"s12")));
         helper.add(0,y++,new JLabel(Text.get(Restore.class,"s13")));
         helper.add(0,y++,new JLabel(Text.get(Restore.class,"s14")));

         helper.add(0,y++,Box.createVerticalStrut(d3));

         helper.add(0,y++,new JLabel(Text.get(Restore.class,"s15")));
         helper.add(0,y++,new JLabel(Text.get(Restore.class,"s16")));

         helper.add(0,y++,Box.createVerticalStrut(d3));

         helper.add(0,y++,viewHelper.getScrollPane(),GridBagHelper.fillVertical);

         helper.add(0,y++,Box.createVerticalStrut(d3));

         helper.add(0,y++,new JLabel(Text.get(Restore.class,"s17")));
         helper.add(0,y++,new JLabel(Text.get(Restore.class,"s18")));

      // buttons

         JButton buttonSelect = new JButton(Text.get(Restore.class,"s4"));
         buttonSelect.addActionListener(viewHelper.getAdapter(new ViewHelper.ButtonPress() { public void run(Object[] o) {
            if (o.length > 0) doSelect(o[0]);
         } }));

         JButton buttonCancel = new JButton(Text.get(Restore.class,"s5"));
         buttonCancel.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            doCancel();
         } });

         JPanel buttons = ButtonHelper.makeButtons(buttonSelect,buttonCancel);

      // finish up

         ButtonHelper.doLayout(this,fields,buttons,buttonSelect);
         pack();
         setLocationRelativeTo(getOwner());
      }

      private void doSelect(Object o) {
         ServerInstance si = (ServerInstance) o;

         Config config;
         try {
            config = getConfig(autoConfigURL,si);
         } catch (Exception e) {
            Pop.error(this,ChainedException.format(Text.get(Restore.class,"e2"),e),Text.get(Restore.class,"s19"));
            return;
         }

         EditDialog dialog = ProMode.isPro(config.proMode) ? (EditDialog) new ConfigDialogPro(this,config)
                                                           : (EditDialog) new ConfigDialog   (this,config);
         if ( ! dialog.run() ) return; // continue looking

         boolean confirmed = Pop.confirm(this,Text.get(Restore.class,"e3"),Text.get(Restore.class,"s20"));
         if ( ! confirmed ) return;

         result = config;
         dispose();
      }

      private void doCancel() {
         result = null;
         dispose();
      }

      public Config run() {
         setVisible(true); // does the modal thing
         return result;
      }
   }

// --- helpers ---

   private static boolean match(Object o, Integer thisInstanceID) {
      return (    thisInstanceID != null
               && ((ServerInstance) o).instanceID == thisInstanceID.intValue() );
   }

   private static int findFirstNonLocalInstance(LinkedList instances, Integer thisInstanceID) {

      // the algorithm isn't just (firstLocal ? 1 : 0) because we don't validate
      // against duplicate IDs, and also because the list might not be that long

      int j = 0;

      Iterator i = instances.iterator();
      while (i.hasNext()) {
         if ( ! match(i.next(),thisInstanceID) ) return j;
         j++;
      }

      return -1;
   }

   private static Config getConfig(String autoConfigURL, ServerInstance si) throws Exception {

      // if the chosen instance is local, we have the data already,
      // since it's in server.xml -- but don't try and handle that.

      AutoConfig.GetDataTransaction t = new AutoConfig.GetDataTransaction(autoConfigURL,si.instanceID,si.passcode);
      t.runInline();

      // clear server fields to make a usable config object
      // (don't bother validating that they were filled in)
      //
      t.result.autoUpdateConfig.instanceID     = null;
      t.result.autoUpdateConfig.passcode       = null;
      t.result.autoUpdateConfig.revisionNumber = null;

      return t.result;
   }

// --- transaction ---

   public static class GetInstances extends GetTransaction {

      private String restoreURL;
      private MerchantConfig merchantConfig;
      private LinkedList result;

      public GetInstances(String restoreURL, MerchantConfig merchantConfig, LinkedList result) {
         this.restoreURL = restoreURL;
         this.merchantConfig = merchantConfig;
         this.result = result;
      }

      public String describe() { return Text.get(Restore.class,"s1"); }
      protected String getFixedURL() { return restoreURL; }
      protected void getParameters(Query query) throws IOException {
         if (merchantConfig.isWholesale) {
            throw new IOException(Text.get(Restore.class,"e1")); // not supported
         } else {
            query.add("mlrfnbr",Convert.fromInt(merchantConfig.merchant));
         }
         query.addPasswordObfuscate("encpassword",merchantConfig.password);
      }

      protected boolean receive(InputStream inputStream) throws Exception {

         result.clear(); // in case we're retrying

         SimpleDateFormat format = new SimpleDateFormat(Text.get(Restore.class,"f1"));
         format.setLenient(false);
         format.setTimeZone(TimeZone.getTimeZone("US/Mountain"));
         // because the server sends the time as mountain time

         Document doc = XML.readStream(inputStream);
         Node res = ServiceUtil.parseResult(doc,"BackupInstances");
         Node list = XML.getElement(res,"Instances");

         Iterator i = XML.getElements(list,"Instance");
         while (i.hasNext()) {
            Node node = (Node) i.next();

            ServerInstance si = new ServerInstance();

            si.instanceID = Convert.toInt(XML.getElementText(node,"ID"));
            si.passcode = Convert.toInt(XML.getElementText(node,"Passcode"));
            si.timestamp = format.parse(XML.getElementText(node,"TS"));

            result.add(si);
         }

         // normally we'd validate a little, here or in the caller,
         // but it's not worth it here.  if we have duplicate IDs,
         // we'll just get two entries on the screen, no harm done.

         Collections.sort(result,mainComparator);
         // server probably returns in some random order

         return true;
      }
   }

}

