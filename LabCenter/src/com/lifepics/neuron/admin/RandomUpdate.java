/*
 * RandomUpdate.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.net.DownloadTransaction;
import com.lifepics.neuron.net.NetUtil;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.object.XML;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A command-line utility for randomly choosing instances to update.
 */

public class RandomUpdate {

   // use this in concert with the manual Closed API call SetDefaultVersion.
   // there are two procedures.
   //
   // if you just want to roll it out slowly, no particular need for it anywhere
   // build new LC,              UpdateToNew, SDV(lift)
   //
   // if you need new installs to have the new version, it has to be the default
   // build new LC, SDV(freeze), UpdateToDef, SDV(lift)

// --- constants ---

   private static final String FILE_LIST = "gru-list.txt"; // gru is the name of the batch file that runs this
   private static final String FILE_DONE = "gru-done.txt";

   private static final String INTEGRATION_NONE = "NONE"; // e.g. all manual

// --- main ---

   public static String spaces(int len) {
      char[] c = new char[len];
      for (int i=0; i<len; i++) c[i] = ' ';
      return new String(c);
   }

   public static void main(String[] args) throws Exception {

      if (args.length < 1) {
         String s = Text.get(RandomUpdate.class,"s2") + ' ';
         System.out.println(s + Text.get(RandomUpdate.class,"s3a"));
         s = spaces(s.length());
         System.out.println(s + Text.get(RandomUpdate.class,"s3b"));
         System.out.println(s + Text.get(RandomUpdate.class,"s3c"));
         System.out.println(s + Text.get(RandomUpdate.class,"s3d"));
         return;
      }

      NetUtil.initNetwork();

      if (args[0].equals("GetList")) {
         getList(Convert.toInt(args[1]),args[2]);
      } else if (args[0].equals("Report")) {
         report(Convert.toInt(args[1]));
      } else if (args[0].equals("UpdateToNew")) {
         update(Convert.toInt(args[1]),Convert.toInt(args[2]),/* toDefault = */ false);
      } else if (args[0].equals("UpdateToDef")) {
         update(Convert.toInt(args[1]),Convert.toInt(args[2]),/* toDefault = */ true);
      } else {
         System.out.println(Text.get(RandomUpdate.class,"s4",new Object[] { args[0] }));
      }
   }

// --- data structures ---

   private static class Record { // subset of Instance
      public int instanceID;
      public Integer passcode;
      public Integer labCenterVersionID;
      public Boolean useDefaultVersion;
      public String reportedIntegrations;
      public boolean done;
   }

   private static class Group {
      public String integration;
      public Vector instances;
      public int countDone;
   }

// --- Record utilities ---

   private static void markDone(Vector list, int instanceID) throws Exception {
      Iterator i = list.iterator();
      while (i.hasNext()) {
         Record r = (Record) i.next();
         if (r.instanceID == instanceID) {
            r.done = true;
            return;
         }
      }
      throw new Exception("Instance " + Convert.fromInt(instanceID) + " not found.");
   }

   private static int countDone(Vector list) {
      int count = 0;

      Iterator i = list.iterator();
      while (i.hasNext()) {
         Record r = (Record) i.next();
         if (r.done) count++;
      }

      return count;
   }

   private static Record getNotDone(Vector list, int index) throws Exception {
      Iterator i = list.iterator();
      while (i.hasNext()) {
         Record r = (Record) i.next();
         if (r.done) continue;
         if (index-- == 0) return r;
      }
      throw new Exception("Instance not found by index value.");
   }

   private static Vector loadList() throws Exception {
      Vector list = new Vector();

      Document doc = XML.readFile(new File(FILE_LIST));
      Node root = XML.getElement(doc,"Instances");

      Iterator i = XML.getElements(root,"Inst");
      while (i.hasNext()) {
         Node node = (Node) i.next();

         Record r = new Record();
         r.instanceID = Convert.toInt(XML.getAttribute(node,"ID"));
         r.passcode = Convert.toNullableInt(XML.getAttributeTry(node,"PC"));
         r.labCenterVersionID = Convert.toNullableInt(XML.getAttributeTry(node,"LV"));
         r.useDefaultVersion = Convert.toNullableBool(XML.getAttributeTry(node,"UD"));
         r.reportedIntegrations = XML.getAttributeTry(node,"RI");
         r.done = false; // done flag is stored in a different file
         list.add(r);
      }

      return list;
   }

   private static void loadDone(Vector list) throws Exception {

      File fileDone = new File(FILE_DONE);
      if ( ! fileDone.exists() ) return;

      FileReader fr = new FileReader(fileDone);
      try {
         BufferedReader br = new BufferedReader(fr);
         while (true) {
            String line = br.readLine();
            if (line == null) break;
            if (line.length() == 0) continue;
            markDone(list,Convert.toInt(line));
         }
      } finally { // not really needed, since we'll exit on exception
         fr.close();
      }
   }

   private static void appendToDone(Vector list) throws Exception {

      File fileDone = new File(FILE_DONE);

      RandomAccessFile raf = new RandomAccessFile(fileDone,"rw"); // creates if not there
      try {
         raf.seek(raf.length());

         Iterator i = list.iterator();
         while (i.hasNext()) {
            Record r = (Record) i.next();
            raf.writeBytes(Convert.fromInt(r.instanceID) + "\r\n");
         }
      } finally {
         raf.close();
      }
   }

   /**
    * Split invalid records out into another list.
    */
   private static Vector splitInvalid(Vector list) {
      Vector invalid = new Vector();

      Iterator i = list.iterator();
      while (i.hasNext()) {
         Record r = (Record) i.next();

         if (    r.passcode == null
              || r.labCenterVersionID == null
              || r.useDefaultVersion == null
              || r.reportedIntegrations == null ) {
            invalid.add(r);
            i.remove();
         }
      }

      return invalid;
   }

   private static Vector splitIneligible(Vector list, int target) {
      Vector ineligible = new Vector();

      Iterator i = list.iterator();
      while (i.hasNext()) {
         Record r = (Record) i.next();

         // assume that when useDefaultVersion is set,
         // the version is set to the current default.

         if (r.labCenterVersionID.intValue() >= target) {
            ineligible.add(r);
            i.remove();
         }
      }

      return ineligible;
   }

// --- Group utilities ---

   private static void addIntegration(HashMap groups, String integration, Record r) {
      Group group = (Group) groups.get(integration);
      if (group == null) {
         group = new Group();
         group.integration = integration;
         group.instances = new Vector();
         group.countDone = 0;
         groups.put(integration,group);
      }
      group.instances.add(r);
      if (r.done) group.countDone++;
   }

   private static String[] NO_INTEGRATIONS = new String[] { INTEGRATION_NONE };

   private static String[] getIntegrations(Record r) {
      if (r.reportedIntegrations.length() == 0) {
         return NO_INTEGRATIONS;
      } else {
         return r.reportedIntegrations.split(",",-1);
      }
   }

   private static void addIntegrations(HashMap groups, Record r) {
      String[] integration = getIntegrations(r);
      for (int i=0; i<integration.length; i++) {
         addIntegration(groups,integration[i],r);
      }
   }

   /**
    * Build HashMap from integration name to Group.
    */
   private static HashMap groupByIntegration(Vector list) {
      HashMap groups = new HashMap();

      Iterator i = list.iterator();
      while (i.hasNext()) {
         Record r = (Record) i.next();
         addIntegrations(groups,r);
      }

      return groups;
   }

   private static Vector getMinimalGroups(HashMap groups) {
      int min = Integer.MAX_VALUE;
      Vector result = new Vector();

      Iterator i = groups.values().iterator();
      while (i.hasNext()) {
         Group group = (Group) i.next();
         if (group.countDone == group.instances.size()) continue; // maybe minimal but no use to us
         if (group.countDone < min) {
            result.clear();
            min = group.countDone;
         }
         if (group.countDone == min) {
            result.add(group);
         }
      }

      return result;
   }

   private static void markDone(HashMap groups, Record r) throws Exception {
      r.done = true;
      String[] integration = getIntegrations(r);
      for (int i=0; i<integration.length; i++) {
         ((Group) groups.get(integration[i])).countDone++;
      }
   }

// --- GetList function ---

   private static void getList(int days, String auth) throws Exception {
      new GetActiveInstances(url,days,auth,new File(FILE_LIST)).runInline();
   }

// --- Report function ---

   private static HashMap report(int target) throws Exception {
      System.out.println();

      Vector list = loadList();
      loadDone(list);
      System.out.println(list.size() + " instances");

      Vector invalid = splitInvalid(list);
      System.out.println(invalid.size() + " invalid");
      System.out.println(list.size() + " valid");

      Vector ineligible = splitIneligible(list,target);
      System.out.println(ineligible.size() + " ineligible");
      System.out.println(list.size() + " eligible, " + countDone(list) + " done");

      System.out.println();

      HashMap groups = groupByIntegration(list);
      System.out.println(groups.size() + " integrations");
      show(groups);

      return groups;
   }

   private static void show(HashMap groups) {
      Iterator i = groups.values().iterator();
      while (i.hasNext()) {
         Group group = (Group) i.next();
         System.out.println(group.instances.size() + " " + group.integration + ", " + group.countDone + " done");
      }
   }

// --- Update function ---

   private static void update(int target, int count, boolean toDefault) throws Exception {
      HashMap groups = report(target);
      Random random = new Random();
      Vector listDone = new Vector();

      for (int i=0; i<count; i++) {
         System.out.println();

         Vector gmin = getMinimalGroups(groups);
         int nGroup = gmin.size();
         if (nGroup == 0) { System.out.println("all instances updated"); break; } // shouldn't happen, but handle it

         Group group = (Group) gmin.get(random.nextInt(nGroup));
         int nInstance = group.instances.size()-group.countDone; // not zero because getMinimalGroups tests for that
         Record r = getNotDone(group.instances,random.nextInt(nInstance));
         System.out.println("selected " + group.integration + ", instance " + r.instanceID);

         new UpdateInstanceTransaction(baseURL,r.instanceID,target,/* useDefault = */ toDefault).runInline();
         listDone.add(r);

         markDone(groups,r);
         show(groups);
      }

      appendToDone(listDone);
   }

// --- transaction ---

   private static final String baseURL = "https://api.lifepics.com/closed/LCService.asmx";
   // duplicates AdminConfig value, suboptimal

   private static final String url = "https://api.lifepics.com/closed/LCService.asmx/GetActiveInstances";
   // a non-configured URL, should be rare

   private static class GetActiveInstances extends DownloadTransaction {

      private String url;
      private int days;
      private String auth;
      private File file;

      public GetActiveInstances(String url, int days, String auth, File file) {
         super(/* overwrite = */ true);

         this.url = url;
         this.days = days;
         this.auth = auth;
         this.file = file;
      }

      public String describe() { return Text.get(RandomUpdate.class,"s1"); }
      protected String getFixedURL() { return url; }
      protected void getParameters(Query query) throws IOException {
         query.add("days",Convert.fromInt(days));
         query.addPasswordCleartext("auth",auth);
      }
      protected File getFile() { return file; }
   }

}

