/*
 * User.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.misc.Resource;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.*;
import com.lifepics.neuron.thread.SubsystemController;

import java.applet.Applet;
import java.applet.AudioClip;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A utility class for passing messages from background threads to the user.
 */

public class User {

// --- constants for config record ---

   public static final int CODE_MIN = 0; // must be zero for array

   public static final int CODE_ORDER_ARRIVED    = 0;
   public static final int CODE_SPECIAL_INSTRUCT = 1;
   public static final int CODE_ORDERS_READY     = 2;
   public static final int CODE_ORDER_DUE_SOON   = 3;
   public static final int CODE_ORDER_OVERDUE    = 4;
   public static final int CODE_ORDER_ERROR      = 5;
   public static final int CODE_INVALID_PASSWORD = 6;
   public static final int CODE_MISSING_CHANNEL  = 7;
   public static final int CODE_JOB_ERROR        = 8;
   public static final int CODE_OTHER_PROCESS    = 9;

   public static final int CODE_MAX = 9;
   public static final int CODE_LIMIT = CODE_MAX+1;

   private static final String chimeFile =
         "chime.wav";
   private static final String[] voiceFile = {
         "voice_arrived.wav",
         null,
         null,
         "voice_duesoon.wav",
         "voice_overdue.wav",
         "voice_error.wav",
         "voice_password.wav",
         "voice_sku.wav",
         null, // null is allowed, means validate that sound type isn't voice
         null
      };
   public static boolean hasVoice(int i) { return (voiceFile[i] != null); }

   // don't forget to update the structure definition.

   // the code enumeration is also used in text arrays,
   // namely, in gui/User.s1 and app/ConfigDialog.s93.
   // one other thing you might want to adjust is loadDefault.

   public static final int SKU_DUE_MIN = 0;

   public static final int SKU_DUE_NONE = 0;
   public static final int SKU_DUE_SOME = 1;
   public static final int SKU_DUE_ALL  = 2;

   public static final int SKU_DUE_MAX = 2;

// --- constants for code record ---

   public static final int SOUND_MIN = 0;

   public static final int SOUND_CHIME = 0;
   public static final int SOUND_VOICE = 1;
   public static final int SOUND_FILE  = 2;

   public static final int SOUND_MAX = 2;

// --- enum functions ---

   private static String[] skuDueTable = {
         Text.get(User.class,"sd0"),
         Text.get(User.class,"sd1"),
         Text.get(User.class,"sd2")
      };

   private static int toSkuDue(String s) throws ValidationException {
      for (int i=0; i<skuDueTable.length; i++) {
         if (s.equals(skuDueTable[i])) return i;
      }
      throw new ValidationException(Text.get(User.class,"e4",new Object[] { s }));
   }

   private static String fromSkuDue(int skuDue) {
      return skuDueTable[skuDue];
   }

   private static EnumeratedType skuDueType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toSkuDue(s); }
      public String fromIntForm(int i) { return fromSkuDue(i); }
   };

   private static String[] soundTypeTable = {
         Text.get(User.class,"st0"),
         Text.get(User.class,"st1"),
         Text.get(User.class,"st2")
      };

   private static int toSoundType(String s) throws ValidationException {
      for (int i=0; i<soundTypeTable.length; i++) {
         if (s.equals(soundTypeTable[i])) return i;
      }
      throw new ValidationException(Text.get(User.class,"e2",new Object[] { s }));
   }

   private static String fromSoundType(int soundType) {
      return soundTypeTable[soundType];
   }

   private static EnumeratedType soundTypeType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toSoundType(s); }
      public String fromIntForm(int i) { return fromSoundType(i); }
   };

// --- code record ---

   public static class CodeRecord extends Structure {

      public boolean useMessage;
      public boolean useTaskBarFlash;
      public boolean useStatusScreen;
      public boolean useSound;
      public int soundType;
      public File soundFile; // nullable ; also note it's not made relative, since no need

      public static final StructureDefinition sd = new StructureDefinition(

         CodeRecord.class,
         // no version
         new AbstractField[] {

            new BooleanField("useMessage","UseMessage",0,false),            // custom loadDefault
            new BooleanField("useTaskBarFlash","UseTaskBarFlash",0,false),  //
            new BooleanField("useStatusScreen","UseStatusScreen",0,false),  //
            new BooleanField("useSound","UseSound",0,false),                //
            new EnumeratedField("soundType","SoundType",soundTypeType,0,0), //
            new NullableFileField("soundFile","SoundFile",0,null)
         });

      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException {
         if (soundType < SOUND_MIN || soundType > SOUND_MAX) throw new ValidationException(Text.get(User.class,"e1",new Object[] { Convert.fromInt(soundType) }));
         if (soundType == SOUND_FILE && soundFile == null) throw new ValidationException(Text.get(User.class,"e6"));
      }

      public void validate(int i) throws ValidationException {
         validate();
         if (soundType == SOUND_VOICE && ! hasVoice(i)) throw new ValidationException(Text.get(User.class,"e7"));
      }

      public Object loadDefault(int i) {
         loadDefault();

         if (i == CODE_SPECIAL_INSTRUCT || i == CODE_ORDERS_READY) { // defaults to off

            useMessage = false;
            useTaskBarFlash = false;
            useStatusScreen = false;
            useSound = false;

         } else {

            boolean isError = (i != CODE_ORDER_ARRIVED);

            useMessage = isError;
            useTaskBarFlash = true;
            useStatusScreen = isError;
            useSound = true;

         }
         soundType = hasVoice(i) ? SOUND_VOICE : SOUND_CHIME;

         return this; // convenience
      }
   }

// --- special field type ---

   // this is not quite factorable as a generic field type.
   // it's not StructureArrayField because the array elements
   // have different names, versions, and defaulting.
   // it's almost a StructureField that writes into an array,
   // and that's how I've implemented it, but the defaulting
   // depends on the array index, so it's not generic.
   // also, in this design there's no nice way to initialize
   // the array object, so that's custom too.

   private static class RecordField extends StructureField {

      private int index;

      // non-defaulting constructor not needed

      public RecordField(String javaName, int index, String xmlName, int sinceVersion) {
         super(javaName,xmlName,CodeRecord.sd,sinceVersion);
         this.index = index;
      }

      protected Object tget(Object o) {
         try {
            return ((Object[]) javaField.get(o))[index];
         } catch (IllegalAccessException e) {
            throw new Error(e);
         }
      }

      protected void tset(Object o, Object value) {
         try {
            ((Object[]) javaField.get(o))[index] = value;
         } catch (IllegalAccessException e) {
            throw new Error(e);
         }
      }

      public Object get(Object o) {
         return tget(o);
      }

      public void loadDefault(Object o) {
         tset(o,new CodeRecord().loadDefault(index));
      }

      protected boolean isDefault(Object o) {
         Object model = new CodeRecord().loadDefault(index);
         return sd.equals(tget(o),model);
      }
   }

// --- config record ---

   public static class Config extends Structure {

      public CodeRecord[] record;
      public long soonInterval; // millis
      public int skuDue;
      public LinkedList skus;
      public int renotifyInterval; // millis ; has to be int because it's for a GUI timer

      public static final StructureDefinition sd = new StructureDefinition(

         Config.class,
         0,new History(new int[] {6,721,7}),
         new AbstractField[] {

            new RecordField("record",CODE_ORDER_ARRIVED,"OrderArrived",2),
            new RecordField("record",CODE_SPECIAL_INSTRUCT,"SpecialInstructions",6),
            new RecordField("record",CODE_ORDERS_READY,"OrdersReady",7),
            new RecordField("record",CODE_ORDER_DUE_SOON,"OrderDueSoon",2),
            new RecordField("record",CODE_ORDER_OVERDUE,"OrderOverdue",2),
            new RecordField("record",CODE_ORDER_ERROR,"OrderError",2),
            new RecordField("record",CODE_INVALID_PASSWORD,"InvalidPassword",2),
            new RecordField("record",CODE_MISSING_CHANNEL,"MissingChannel",2),
            new RecordField("record",CODE_JOB_ERROR,"JobError",5),
            new RecordField("record",CODE_OTHER_PROCESS,"OtherProcess",3),
            new LongField("soonInterval","SoonInterval-Millis",2,600000), // 10 minutes
            new EnumeratedField("skuDue","SkuDueType",skuDueType,2,SKU_DUE_ALL),
            new InlineListField("skus","SkuDue",4) {

               // this is the one place where the XML representation breaks down.
               // I want a single list containing mixed old and new SKUs,
               // and I don't want to use a different tag, because then I'd need
               // to rework down to the getElements level to read it.
               // the best solution I can think of is, store it like a mapping,
               // or like a one-field attributed structure.
               // this field has been here since version 2, and was changed in 4.

               private static final String tagOld = "SKU";
               private static final String tagNew = "A";

               protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
                  if (version >= 2) {
                     loadOldSKU(node,o); // normal inline list of old SKUs
                  } else {
                     loadDefault(o);
                  }
               }
               protected void loadOldSKU(Node node, Object o) throws ValidationException {
                  LinkedList list = tget(o);

                  Iterator i = XML.getElements(node,xmlName);
                  while (i.hasNext()) {
                     Node child = (Node) i.next();
                     list.add(OldSKU.decode(XML.getText(child)));
                  }
               }
               protected void loadNormal(Node node, Object o) throws ValidationException {
                  LinkedList list = tget(o);

                  Iterator i = XML.getElements(node,xmlName);
                  while (i.hasNext()) {
                     Node child = (Node) i.next();

                     String s = XML.getAttributeTry(child,tagNew);
                     if (s != null) {
                        list.add(NewSKU.decode(s));
                     } else {
                        s = XML.getAttribute(child,tagOld);
                        list.add(OldSKU.decode(s));
                     }
                  }
               }
               public void store(Node node, Object o) {

                  Iterator i = tget(o).iterator();
                  while (i.hasNext()) {
                     Object val = i.next();

                     Node child = XML.createElement(node,xmlName);
                     if (val instanceof NewSKU) {
                        XML.setAttribute(child,tagNew,NewSKU.encode((NewSKU) val));
                     } else {
                        XML.setAttribute(child,tagOld,OldSKU.encode((OldSKU) val));
                     }
                  }
               }
            },
            new IntegerField("renotifyInterval","RenotifyInterval-Millis",3,1800000) // 30 minutes
         })
      {
         // special code that we need for the record array

         public void init(Object o) {
            ((Config) o).record = new CodeRecord[CODE_LIMIT];
            super.init(o);
         }

         public void copy(Object oDest, Object oSrc) {
            ((Config) oDest).record = (CodeRecord[]) ((Config) oSrc).record.clone();
            // there's no field corresponding to the whole array,
            // so we have to go in by hand to prevent the array from being shared
            super.copy(oDest,oSrc);
         }
      };

      protected StructureDefinition sd() { return sd; }

      // normally, you'd have *some* fields that existed in version 0.
      // this is a special case ... on upgrade to version 2,
      // we want to forget the old settings and use the defaults.

      public void validate() throws ValidationException {
         for (int i=0; i<CODE_LIMIT; i++) {
            record[i].validate(i);
         }
         if (soonInterval < 1) throw new ValidationException(Text.get(User.class,"e5"));
         if (skuDue < SKU_DUE_MIN || skuDue > SKU_DUE_MAX) throw new ValidationException(Text.get(User.class,"e3",new Object[] { Convert.fromInt(skuDue) }));
         if (renotifyInterval < 1) throw new ValidationException(Text.get(User.class,"e8"));
      }
   }

// --- static config ---

   private static Config config;
   public static void setConfig(Config config) { User.config = config; }

   private static EditSKUInterface editSKUInterface;
   public static void setEditSKUInterface(EditSKUInterface editSKUInterface) { User.editSKUInterface = editSKUInterface; }

   private static JFrame frame;
   public static void setFrame(JFrame frame) { User.frame = frame; trigger(); }
      // this should only be called after the frame is visible

   private static boolean wasIconified = false;
   public static void deiconifySuccessful() { wasIconified = false; trigger(); }

   private static boolean terminated = false;
   public static void terminate() { terminated = true; }
      // call this at app exit to prevent new windows from popping up.  I'll use a two-pronged strategy:
      // block the major entry points, and also block right before we do any of the notification things.

// --- audio clip cache ---

   private static HashMap clipMap = new HashMap();

   private static AudioClip getClip(URL url) {
      AudioClip clip = (AudioClip) clipMap.get(url);
      if (clip == null) {
         clip = Applet.newAudioClip(url);
         clipMap.put(url,clip);
      }
      return clip;
   }

// --- tell sequence ---

   public static void tell(int code, String message) {
      tell(code,message,null,null,null);
   }
   public static void tell(int code, String message, SKU sku, String messageExtra) {
      tell(code,message,sku,null,messageExtra);
   }
   public static void tell(int code, String message, SubsystemController subsystem) {
      tell(code,message,null,subsystem,null);
   }
   private static void tell(int code, String message, SKU sku, SubsystemController subsystem, String messageExtra) {
      if (terminated) return;
      EventQueue.invokeLater(new Message(code,message,sku,subsystem,messageExtra));
   }

   private static class Message implements Runnable {

      public int code;
      public String message;
      public SKU sku;
      public SubsystemController subsystem;
      public String messageExtra; // nullable

      public Message(int code, String message, SKU sku, SubsystemController subsystem, String messageExtra) {
         this.code = code;
         this.message = message;
         this.sku = sku;
         this.subsystem = subsystem;
         this.messageExtra = messageExtra;
      }

      public boolean equals(Object o) {
         if ( ! (o instanceof Message) ) return false;
         Message m = (Message) o;
         return (    code == m.code
                  && message.equals(m.message)
                  && Nullable.equalsObject(sku,m.sku)
                  && subsystem == m.subsystem // n.b. works for null too
                  && Nullable.equals(messageExtra,m.messageExtra) );
      }

      public void run() {
         queueInbox.add(this);
         trigger();
      }

      public Object text;      // useMessage (or useStatusScreen)
      public boolean toFront;  // useTaskBarFlash
      public boolean colorize; // useStatusScreen
      public URL sound;        // useSound

      public void prepare() {

         // look this up all at once so that we don't grab some settings
         // at one time and some at another

         CodeRecord r = config.record[code];

         if (r.useMessage) {
            // kind of arbitrary, but intentional: if useStatusScreen
            // is true but useMessage is false, the message that pops
            // won't have the MiniPanel or the "you can turn this off"

            String s1 = message;

            String s2 = Text.get(User.class,"s2");
            if (messageExtra != null) s2 = messageExtra + "\n\n" + s2;

            Object special = null;
            if (sku != null) { // subsystem is null in this case
               special = makeSKUButton(sku);
            } else if (subsystem != null) {
               special = new MiniPanel(subsystem);
            }

            if (special != null) {

               int d1 = Text.getInt(User.class,"d1");
               Component c1 = Box.createVerticalStrut(d1);
               Component c2 = Box.createVerticalStrut(d1);

               text = new Object[] { s1, c1, special, c2, s2 };
               // JOptionPane actually accepts many kinds of objects
               // in the message argument, including arrays of things

            } else {
               text = s1 + "\n\n" + s2;
            }
         }

         if (r.useTaskBarFlash) {
            toFront = true;
         }

         if (r.useStatusScreen) {
            colorize = true;
            if (text == null) text = Text.get(User.class,"s3");
            // have to pop a dialog so that we don't just
            // turn the color on and off before a repaint happens.
            // actually we also have to pop a dialog because that
            // is where the color gets turned off!
         }

         if (r.useSound) {
            switch (r.soundType) {
            case SOUND_CHIME:
               sound = Resource.getResource(User.class,chimeFile);
               break;
            case SOUND_VOICE:
               sound = Resource.getResource(User.class,voiceFile[code]);
               break;
            case SOUND_FILE:
               try {
                  sound = r.soundFile.toURL(); // not nullable in this case
               } catch (Exception e) {
                  // leave sound null, no sound
               }
               break;
            }
         }
      }
   }

   private static LinkedList queueInbox = new LinkedList();
   private static LinkedList queueSound = new LinkedList();
   private static LinkedList queueMessage = new LinkedList();
   private static boolean activeMessage = false;

   // avoid playing sounds one on top of another.  the longest sound at present
   // is 5s ... I'm using the 10s delay so LC hopefully won't sound too frantic.
   private static int SOUND_DELAY = 10000;
   private static Timer soundTimer = null; // also serves as flag for whether sound in progress
   private static ActionListener soundComplete = new ActionListener() { public void actionPerformed(ActionEvent e) { soundTimer = null; trigger(); } };

   // the trigger function looks at what's in the queues and does whatever
   // it can at the moment.  there are four places / times when it's called:
   //
   // * when the frame is first set, since we can't do anything before that
   // * after the frame gets unminimized
   // * when a new message is posted
   // * when the sound timer has run out

   private static void trigger() {
      if (frame == null || terminated) return; // wait for the setFrame trigger

      if (isIconified(frame)) wasIconified = true;
      //
      // in the message loop below, the idea is to say "if iconified, don't show messages".
      //
      // problem 1: if you call toFront, isIconified starts returning true, even though
      // there hasn't been time for anything to happen in the GUI yet.
      //
      // problem 2: if trigger gets calls more than once in rapid succession, as it did
      // in my testing, the first trigger can call toFront, then the second one can pop
      // a message too early.  we have to wait for the deiconifySuccessful call from the
      // main frame before we can do anything.

   // some things always happen immediately

      while ( ! queueInbox.isEmpty() ) {
         Message m = (Message) queueInbox.removeFirst();
         m.prepare();
         if (m.text != null && ! queueMessage.contains(m)) queueMessage.add(m); // don't pile up duplicates
         if (m.toFront && ! terminated) toFront(frame,/* alert = */ true);
         if (m.colorize && ! terminated) Style.style.colorize(frame,/* alert = */ true);
         if (m.sound != null) queueSound.add(m);
      }

   // see if we can start playing a sound

      if (soundTimer == null && ! queueSound.isEmpty()) {

         Message m = (Message) queueSound.removeFirst();
         if ( ! terminated ) getClip(m.sound).play();

         soundTimer = new Timer(SOUND_DELAY,soundComplete);
         soundTimer.setRepeats(false);
         soundTimer.start();
         // maybe the timer will do nothing except clear itself, but that's fine
      }

   // see if we can pop any messages

      // everything here happens in the UI thread,
      // so we don't need thread synchronization,
      // but we do need an active flag, because the thread
      // can reenter this function inside the modal loop.

      if (activeMessage || wasIconified) return;
      activeMessage = true;

      while ( ! queueMessage.isEmpty() ) {

         Message m = (Message) queueMessage.getFirst();
         if ( ! terminated ) Pop.warningVariant(frame,m.text,Text.get(User.class,"s1_" + m.code));
         queueMessage.removeFirst();
         // remove at end, otherwise one duplicate could pile up.

         if (m.colorize && ! anyColorize(queueMessage) && ! terminated) Style.style.colorize(frame,/* alert = */ false);
      }

      activeMessage = false;
   }

   private static boolean anyColorize(LinkedList queue) {
      Iterator i = queue.iterator();
      while (i.hasNext()) {
         Message m = (Message) i.next();
         if (m.colorize) return true;
      }
      return false;
   }

   private static JPanel makeSKUButton(final SKU sku) {

      JButton button = new JButton(Text.get(User.class,"s4"));
      button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { editSKUInterface.editSKU(sku); } });

      // if it's just a button, it gets stretched to fit horizontally
      JPanel panel = new JPanel();
      GridBagHelper helper = new GridBagHelper(panel);

      helper.add(0,0,new JLabel());
      helper.add(1,0,button);
      helper.add(2,0,new JLabel());
      helper.add(3,0,Box.createHorizontalStrut(Text.getInt(User.class,"d2"))); // center in entire dialog

      helper.setColumnWeight(0,1);
      helper.setColumnWeight(2,1);

      return panel;
   }

// --- frame state helpers ---

   public static boolean isIconified(JFrame frame) {
      return ((frame.getExtendedState() & Frame.ICONIFIED) != 0);
   }

   public static void iconify(JFrame frame) {
      int state = frame.getExtendedState();
      if ((state & Frame.ICONIFIED) == 0) {
         state  ^= Frame.ICONIFIED;
         frame.setExtendedState(state);
      }
   }

   public static boolean deiconify(JFrame frame) {
      int state = frame.getExtendedState();
      boolean iconified = ((state & Frame.ICONIFIED) != 0);
      if (iconified) {
         state  ^= Frame.ICONIFIED;
         frame.setExtendedState(state);
      }
      frame.repaint(); // else some edge bits don't repaint, not sure why
      return iconified;
   }

   public interface FrameAlert {
      void setAlert();
   }

   public static void toFront(JFrame frame, boolean alert) {
      if (deiconify(frame)) {

         // we're deiconifying, so the frame event handler will fire.
         // if we're alerting the user that LC requires attention,
         // tell the frame so it can pop the correct password dialog.
         // the handler will clear the flag when it's done.
         //
         if (alert && frame instanceof FrameAlert) ((FrameAlert) frame).setAlert();
      }
      frame.toFront();
   }

}

