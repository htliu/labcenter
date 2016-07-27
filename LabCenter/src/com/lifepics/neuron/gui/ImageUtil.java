/*
 * ImageUtil.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;

import java.io.File;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

/**
 * A utility class for image I/O operations.
 */

public class ImageUtil {

   public static String getSuffix(File file) throws Exception {
      return getSuffix(file.getName());
   }
   public static String getSuffix(String name) throws Exception {

      int index = name.lastIndexOf('.');
      if (index == -1) throw new Exception(Text.get(ImageUtil.class,"e1",new Object[] { name }));

      return name.substring(index+1).toLowerCase();
   }

   public static ImageReader getReader(String suffix) throws Exception {

      Iterator i = ImageIO.getImageReadersBySuffix(suffix);
      if ( ! i.hasNext() ) throw new Exception(Text.get(ImageUtil.class,"e2",new Object[] { suffix }));

      return (ImageReader) i.next(); // take first reader
   }

}

