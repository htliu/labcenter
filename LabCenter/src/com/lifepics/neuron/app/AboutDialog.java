/*
 * AboutDialog.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.Blob;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.Style;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * An about dialog.
 */

public class AboutDialog extends JDialog {

   public AboutDialog(Frame owner) {
      super(owner,Text.get(AboutDialog.class,"s1",new Object[] { Style.style.getBrandName() }),/* modal = */ true);

      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      setResizable(false);

   // brand

      Object[] args = new Object[] { Style.style.getBrandName() };

   // logo

      JLabel imageLabel = new JLabel(Style.style.getLogo());

   // app info

      int d1 = Text.getInt(this,"d1");
      int d2 = Text.getInt(this,"d2");

      JPanel panel = new JPanel();
      panel.setBorder(BorderFactory.createEtchedBorder());

      GridBagHelper helper = new GridBagHelper(panel);

      helper.add(1,0,Box.createVerticalStrut(d1));
      helper.addCenter(1,1,new JLabel(Text.get(this,"s2",args)));
      helper.addCenter(1,2,new JLabel(Text.get(this,"s3a",new Object[] { getVersion() })));
      helper.add(1,3,Box.createVerticalStrut(d2));
      helper.addCenter(1,4,new JLabel(Text.get(this,"s4")));
      helper.add(1,5,Box.createVerticalStrut(d1));

      helper.addBlank(2,0);

      helper.setColumnWeight(0,1);
      helper.setColumnWeight(2,1);

   // blobs

      int w1 = Text.getInt(this,"w1");
      int d6 = Text.getInt(this,"d6");

      JComponent description = Blob.makeBlob(Blob.getBlob(this,"b1",args),null,d6,w1);
      JComponent disclaimer  = Blob.makeBlob(Blob.getBlob(this,"b2"     ),null,d6,w1);

   // OK button

      JButton button = new JButton(Text.get(this,"s5"));
      button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { dispose(); } });

   // overall

      int d3 = Text.getInt(this,"d3");
      int d4 = Text.getInt(this,"d4");
      int d5 = Text.getInt(this,"d5");

      ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(d3,d3,d3,d3));

      helper = new GridBagHelper(getContentPane());

      helper.addCenter(0,0,imageLabel);
      helper.add(0,1,Box.createVerticalStrut(d3));
      helper.addFill(0,2,panel);
      helper.add(0,3,Box.createVerticalStrut(d4));
      helper.addFill(0,4,description);
      helper.add(0,5,Box.createVerticalStrut(d5));
      helper.addFill(0,6,disclaimer);
      helper.add(0,7,Box.createVerticalStrut(d3));
      helper.addCenter(0,8,button);

   // finish up

      // for some reason, the text areas don't figure out the right size
      // the first time around, you have to call pack again for it to work

      pack();
      pack();
      setLocationRelativeTo(getOwner());
   }

// --- methods ---

   public static String getVersion() {
      return Text.get(AboutDialog.class,"s3b");
   }

   /**
    * Run the dialog.
    */
   public void run() {
      setVisible(true); // does the modal thing
   }

}

