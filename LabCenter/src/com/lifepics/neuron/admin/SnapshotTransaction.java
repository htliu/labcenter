/*
 * SnapshotTransaction.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.net.GetTransaction;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.object.XML;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * The transaction for getting a snapshot from the server.
 */

public class SnapshotTransaction extends GetTransaction {

   public Snapshot result;

   private String baseURL;
   private Parameters p;

   public SnapshotTransaction(String baseURL, Parameters p) {
      this.baseURL = baseURL;
      this.p = p;
   }

   public  static final String ADMIN_AUTH = "DWZ5j67WHNhBauIJN6SOJZhPQw5zguuBUIC7WF7yhEB3S5O8";
   private static final String NAME_SNAPSHOT = "Snapshot"; // different from SnapshotFile one

   public String describe() { return Text.get(SnapshotTransaction.class,"s1"); }
   protected String getFixedURL() { return combine(baseURL,"GetSnapshot"); }
   protected void getParameters(Query query) throws IOException {

      addList(query,p.merchants,p.includeDeletedLocations ? "ma" : "m");
      // server can actually mix and match m and ma, but we don't support that in the UI

      addList(query,p.locations,"l");
      addList(query,p.wholesalers,"w");
      addList(query,p.instances,"i");

      query.addPasswordCleartext("auth",ADMIN_AUTH);
   }

   private void addList(Query query, LinkedList list, String key) throws IOException {
      Iterator i = list.iterator();
      while (i.hasNext()) {
         query.add(key,Convert.fromInt(((Integer) i.next()).intValue()));
      }
   }

   protected boolean receive(InputStream inputStream) throws Exception {

      result = (Snapshot) XML.loadStream(inputStream,new Snapshot(),NAME_SNAPSHOT);
      // a rare case when the server knows the details of a standard LC structure

      result.sort();

      return true;
   }

}

