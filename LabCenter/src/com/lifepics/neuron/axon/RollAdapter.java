/*
 * RollAdapter.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.table.TableAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A kind of {@link TableAdapter} that allows {@linkplain Roll rolls}
 * to be stored in a {@link com.lifepics.neuron.table.Table}.
 */

public class RollAdapter implements TableAdapter {

   /**
    * Get the primary key for an object.
    */
   public String getKey(Object o) {
      return Convert.fromInt(((Roll) o).rollID);
   }

   /**
    * Set the primary key for an object.
    */
   public boolean setKey(Object o, String key) {
      try {
         ((Roll) o).rollID = Convert.toInt(key);
         return true;
      } catch (ValidationException e) {
         return false;
      }
   }

   /**
    * Make a deep copy of an object.
    */
   public Object copy(Object o) {
      return CopyUtil.copy((Copyable) o);
   }

   private static final String NAME_ROLL = "Roll";

   /**
    * Load an object from a stream.
    */
   public Object load(InputStream inputStream) throws IOException, ValidationException {
      return XML.loadStream(inputStream,new Roll(),NAME_ROLL);
   }

   /**
    * Store an object into a stream.
    */
   public void store(OutputStream outputStream, Object o) throws IOException {
      XML.storeStream(outputStream,((Roll) o),NAME_ROLL);
   }

}

