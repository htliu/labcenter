/*
 * UpdateInstanceTransaction.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.net.GetTransaction;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.object.XML;

import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Document;

/**
 * The transaction for updating an instance on the server.
 * The only editable field right now is the LC version ID.
 */

public class UpdateInstanceTransaction extends GetTransaction {

   private String baseURL;
   private int instanceID;
   private int versionID;
   private boolean useDefault;

   public UpdateInstanceTransaction(String baseURL, int instanceID, int versionID, boolean useDefault) {
      this.baseURL = baseURL;
      this.instanceID = instanceID;
      this.versionID = versionID;
      this.useDefault = useDefault;
   }

   public String describe() { return Text.get(UpdateInstanceTransaction.class,"s1"); }
   protected String getFixedURL() { return combine(baseURL,"UpdateInstance"); }
   protected void getParameters(Query query) throws IOException {
      query.add("instanceID",Convert.fromInt(instanceID));
      query.add("versionID",Convert.fromInt(versionID));
      query.add("useDefault",Convert.fromBool(useDefault)); // C# convention same as LC convention
      query.addPasswordCleartext("auth",SnapshotTransaction.ADMIN_AUTH);
   }

   protected boolean receive(InputStream inputStream) throws Exception {

      Document doc = XML.readStream(inputStream);
      String s = XML.getElementText(doc,"string");
      if ( ! s.equals("ok") ) throw new Exception(Text.get(this,"e1",new Object[] { s }));

      return true;
   }

}

