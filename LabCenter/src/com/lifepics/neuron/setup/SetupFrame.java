/*
 * SetupFrame.java
 */

package com.lifepics.neuron.setup;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.ButtonHelper;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.Graphic;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.install.InstallFile;
import com.lifepics.neuron.misc.AppUtil;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * The main frame window for the setup application.
 * This one is really more like an EditDialog,
 * but dialog windows are required to have owners.
 */

public class SetupFrame extends JFrame {

// --- fields ---

   private AppUtil.ControlInterface control;
   private String mainApp;
   private StoreList storeList;
   private File configFile;

   private JComboBox storeNumber;
   private ButtonGroup labs;
   private JTextField lucidiomAddress;

// --- construction ---

   public SetupFrame(AppUtil.ControlInterface control, File baseDir, File mainDir, LinkedList templates, String mainApp, StoreList storeList, File configFile) {
      super(Text.get(SetupFrame.class,"s1"));

      this.control = control;
      this.mainApp = mainApp;
      this.storeList = storeList;
      this.configFile = configFile;

      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      Graphic.setFrameIcon(this);
      addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { doCancel(); } });

      setResizable(false);

   // fields

      storeNumber = new JComboBox(storeList.stores.toArray());
      storeNumber.setEditable(true);

      labs = new ButtonGroup();

      Iterator i = templates.iterator();
      while (i.hasNext()) {
         InstallFile file = (InstallFile) i.next();
         JRadioButton b = new JRadioButton(file.typeData); // null is fine, it just makes a black button
         b.putClientProperty(fileProperty,file.getDestFile(baseDir,mainDir)); // dest validated not null
         labs.add(b);
      }
      // no default, just leave all unselected

      lucidiomAddress = new JTextField(Text.getInt(this,"d3"));

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      int d4 = Text.getInt(this,"d4");
      int y = 0;

      helper.add(1,y++,Box.createVerticalStrut(d4));

      helper.add(0,y,new JLabel(Text.get(this,"s4") + ' '));
      helper.add(1,y++,storeNumber);

      helper.add(1,y++,Box.createVerticalStrut(d4));

      helper.add(0,y,new JLabel(Text.get(this,"s5") + ' '));

      Enumeration e = labs.getElements();
      while (e.hasMoreElements()) {
         helper.add(1,y++,(JRadioButton) e.nextElement());
      }

      helper.add(1,y++,Box.createVerticalStrut(d4));

      helper.add(0,y,new JLabel(Text.get(this,"s9") + ' '));
      helper.add(1,y++,lucidiomAddress);

      helper.add(1,y++,Box.createVerticalStrut(d4));

   // buttons

      JButton buttonOK = new JButton(Text.get(this,"s2"));
      buttonOK.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doOK(); } });

      JButton buttonCancel = new JButton(Text.get(this,"s3"));
      buttonCancel.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doCancel(); } });

      JPanel buttons = ButtonHelper.makeButtons(buttonOK,buttonCancel);

   // finish up

      ButtonHelper.doLayout(this,fields,buttons,buttonOK);
      pack();
      setLocationRelativeTo(null); // center on screen
   }

// --- constants ---

   private static final Object fileProperty = new Object();
   // use Object rather than String to guarantee no collision

   // names of XML nodes
   private static final String NAME_CONFIG = "Config";
   private static final String NAME_MERCHANT_CONFIG = "MerchantConfig";
   private static final String NAME_MERCHANT = "Merchant";
   private static final String NAME_QUEUE_LIST = "QueueList";
   private static final String NAME_QUEUE = "Queue";
   private static final String NAME_NAME = "Name";
   private static final String NAME_FORMAT_CONFIG = "FormatConfig";
   private static final String NAME_DATA_DIR = "DataDir";
   //
   // it's actually kind of bad that we have these here.
   //
   // for one thing, the same constants already exist in other places,
   // namely, the structure defs for the config and its subobjects.
   //
   // for another, the way I use the constants encodes knowledge about
   // how the data is stored.  if the config changed, this class would
   // not automatically change with it, and could fail.
   //
   // on the other hand, I'm not looking at much of the config,
   // and the parts I'm looking at are pretty stable.  also,
   // if we used a config object, we'd be locked in to that version
   // of the config, and would break if the wrong ConfigCenter were
   // used to update the config data.

   // other strings
   private static final String S_LUCIDIOM = "Lucidiom";
   private static final String S_PATTERN = "10.0.0.0";

// --- helpers ---

   private static JRadioButton getSelectedButton(ButtonGroup g) {
      Enumeration e = g.getElements();
      while (e.hasMoreElements()) {
         JRadioButton b = (JRadioButton) e.nextElement();
         if (b.isSelected()) return b;
      }
      return null;
   }

   /**
    * There are too many uncertainties involved with java.net.InetAddress,
    * for example it will let through numbers without any dots at all.
    * So, let's just validate for the simple format that we expect to see.
    */
   private static boolean isValidAddress(String address) {

      String[] s = address.split("\\.",-1); // -1 to stop weird default behavior
      if (s.length != 4) return false;

      int n;
      for (int i=0; i<s.length; i++) {
         try {
            n = Convert.toInt(s[i]);
         } catch (ValidationException e) {
            return false;
         }
         if (n < 0 || n > 255) return false;
      }

      return true;
   }

   private static Node findQueueByName(Node queueList, String name) throws ValidationException {
      Iterator i = XML.getElements(queueList,NAME_QUEUE);
      while (i.hasNext()) {
         Node queue = (Node) i.next();
         if (XML.getElementText(queue,NAME_NAME).equals(name)) return queue;
      }
      throw new ValidationException(Text.get(SetupFrame.class,"e6",new Object[] { name }));
   }

   /**
    * Like String.replaceFirst except without regular expression worries.
    */
   private static String replaceFirst(String s, String oldStr, String newStr) throws Exception {
      int i = s.indexOf(oldStr);
      if (i == -1) throw new Exception(Text.get(SetupFrame.class,"e5",new Object[] { oldStr }));

      return s.substring(0,i) + newStr + s.substring(i+oldStr.length(),s.length());
   }

// --- commands ---

   private void doOK() {

   // read parameters from UI

      int locationID;
      File templateFile = null;
      String lucidiomAddr;

      try {

         // object is either a predefined store object or a typed-in string store number
         Object o = storeNumber.getSelectedItem();
         Store store;
         if (o instanceof Store) {
            store = (Store) o;
         } else {
            String storeNumber = ((String) o).trim();
            store = storeList.findStoreByNumber(storeNumber);
            if (store == null) throw new ValidationException(Text.get(this,"e1",new Object[] { storeNumber }));
         }
         locationID = store.locationID;

         JRadioButton b = getSelectedButton(labs);
         if (b == null) throw new ValidationException(Text.get(this,"e2"));
         templateFile = ((File) b.getClientProperty(fileProperty));

         lucidiomAddr = Field.get(lucidiomAddress);
         if ( ! isValidAddress(lucidiomAddr) ) throw new ValidationException(Text.get(this,"e3"));
         // LucidiomConfig.dataDir doesn't have this much validation, but this is what we want

      } catch (ValidationException e) {
         Pop.error(this,e,Text.get(this,"s10"));
         return;
      }

   // create config file

      Node t;
      try {

         Document doc = XML.readFile(templateFile);
         Node node = XML.getElement(doc,NAME_CONFIG);

      // poke locationID

         Node merchantConfig = XML.getElement(node,NAME_MERCHANT_CONFIG);
         Node merchant = XML.getElement(merchantConfig,NAME_MERCHANT);

         t = XML.getTextNode(merchant);
         t.setNodeValue(Convert.fromInt(locationID));

      // poke lucidiomAddr

         Node queueList = XML.getElement(node,NAME_QUEUE_LIST);
         Node queue = findQueueByName(queueList,S_LUCIDIOM);
         Node formatConfig = XML.getElement(queue,NAME_FORMAT_CONFIG);
         Node dataDir = XML.getElement(formatConfig,NAME_DATA_DIR);

         t = XML.getTextNode(dataDir);
         t.setNodeValue(replaceFirst(t.getNodeValue(),S_PATTERN,lucidiomAddr));

      // done

         XML.writeFile(configFile,doc);
         //
         // this overwrites the file if it exists, which is fine.
         // think of it as like a partially downloaded file,
         // we happily overwrite those when we have to try again.

         control.restart(mainApp);

      } catch (Exception e) {
         // log as well as notify, so that it's in the log file for future reference
         Log.log(Level.SEVERE,this,"e4",e);
         Pop.error(this,e,Text.get(this,"s11"));
      }
   }

   private void doCancel() {

      boolean confirmed = Pop.confirm(this,Text.get(this,"e7"),Text.get(this,"s12"));
      if ( ! confirmed ) return;

      control.exit();
   }

}

