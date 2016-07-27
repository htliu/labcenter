/*
 * OrderAdapter.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.table.TableAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A kind of {@link TableAdapter} that allows {@linkplain Order orders}
 * and {@linkplain OrderStub order stubs}
 * to be stored in a {@link com.lifepics.neuron.table.Table}.
 */

public class OrderAdapter implements TableAdapter {

   /**
    * Get the primary key for an object.
    */
   public String getKey(Object o) {
      return ((OrderStub) o).getFullID();
   }

   /**
    * Set the primary key for an object.
    */
   public boolean setKey(Object o, String key) {
      throw new UnsupportedOperationException();
      // we could split the key into a sequence and ID,
      // but no need, setKey is only for auto-numbered.
   }

   /**
    * Make a deep copy of an object.
    */
   public Object copy(Object o) {
      return CopyUtil.copy((Copyable) o);
   }

   // the rest of the code makes it possible for the table to contain
   // instances of both OrderStub and Order.
   // this could be generalized to any number of classes, but there's no need.

   private static final String NAME_STUB = "Stub";
   private static final String NAME_ORDER = "Order";

   /**
    * Load an object from a stream.
    */
   public Object load(InputStream inputStream) throws IOException, ValidationException {

      // adaptation of XML.loadStream

      Document doc = XML.readStream(inputStream);

      Node node = XML.getElementTry(doc,NAME_STUB);
      XML.Persist persist;
      if (node != null) {
         persist = new OrderStub();
      } else {
         node = XML.getElement(doc,NAME_ORDER); // do this second, better error message
         persist = new Order();
      }

      persist.load(node); // ignore convenience result
      persist.validate();
      return persist; // convenience
   }

   /**
    * Store an object into a stream.
    */
   public void store(OutputStream outputStream, Object o) throws IOException {
      String name = (o instanceof Order) ? NAME_ORDER : NAME_STUB;
      XML.storeStream(outputStream,((OrderStub) o),name); // cast is also an assertion
   }

}

