/*
 * ItemPanel.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.GridBagHelper;

import java.io.File;
import java.util.LinkedList;

import javax.swing.JPanel;
import java.awt.Component;

/**
 * A superclass that defines the interface for item panels.
 */

public abstract class ItemPanel extends JPanel {

   // order is roughly execution order

   public abstract int addFields(GridBagHelper helper, int y);
   public abstract Component getLastComponent();

   public abstract void setAlbum(String album) throws ValidationException;
   public abstract String getAlbum() throws ValidationException;
   public abstract LinkedList getItems(LinkedList items) throws ValidationException;
   public abstract void stop();
   public abstract File getCurrentDir();

}

