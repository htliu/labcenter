/*
 * ConfigDialog.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.axon.DealerTransaction;
import com.lifepics.neuron.axon.PollConfig;
import com.lifepics.neuron.axon.Roll;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.AgfaConfig;
import com.lifepics.neuron.dendron.AutoCompleteConfig;
import com.lifepics.neuron.dendron.BeaufortConfig;
import com.lifepics.neuron.dendron.BurnConfig;
import com.lifepics.neuron.dendron.DirectJPEGConfig;
import com.lifepics.neuron.dendron.DirectPDFConfig;
import com.lifepics.neuron.dendron.DKS3Config;
import com.lifepics.neuron.dendron.DLSConfig;
import com.lifepics.neuron.dendron.DNPConfig;
import com.lifepics.neuron.dendron.DP2Config;
import com.lifepics.neuron.dendron.Format;
import com.lifepics.neuron.dendron.FujiConfig;
import com.lifepics.neuron.dendron.FujiNewConfig;
import com.lifepics.neuron.dendron.Fuji3Config;
import com.lifepics.neuron.dendron.HotFolderConfig;
import com.lifepics.neuron.dendron.HPConfig;
import com.lifepics.neuron.dendron.KodakConfig;
import com.lifepics.neuron.dendron.KonicaConfig;
import com.lifepics.neuron.dendron.LucidiomConfig;
import com.lifepics.neuron.dendron.ManualConfig;
import com.lifepics.neuron.dendron.MappingConfig;
import com.lifepics.neuron.dendron.NoritsuConfig;
import com.lifepics.neuron.dendron.Order;
import com.lifepics.neuron.dendron.OrderDialog;
import com.lifepics.neuron.dendron.PixelConfig;
import com.lifepics.neuron.dendron.ProductCallback;
import com.lifepics.neuron.dendron.PurusConfig;
import com.lifepics.neuron.dendron.Queue;
import com.lifepics.neuron.dendron.QueueCombo;
import com.lifepics.neuron.dendron.RawJPEGConfig;
import com.lifepics.neuron.dendron.SKUComparator;
import com.lifepics.neuron.dendron.ThreadDefinition;
import com.lifepics.neuron.dendron.XeroxConfig;
import com.lifepics.neuron.dendron.ZBEConfig;
import com.lifepics.neuron.gui.DateField;
import com.lifepics.neuron.gui.DisableTextField;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.IntervalField;
import com.lifepics.neuron.gui.PrintConfig;
import com.lifepics.neuron.gui.ProductBarcode;
import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.misc.ExtensionFileFilter;
import com.lifepics.neuron.misc.TimeZoneUtil;
import com.lifepics.neuron.net.BandwidthConfig;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.SKU;
import com.lifepics.neuron.struct.Structure;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for editing configuration information.
 */

public class ConfigDialog extends EditDialog {

// --- fields ---

   private Config config;
   private HashMap saveFormats;
   private Date saveEffectiveDate;
   private boolean saveProductLabels;
   private boolean showKiosk;

   private JTabbedPane tabbedPane;
   private JPanel tabScan;
   private JPanel tabScanCover;

   private OrderDialog.FormatArrays fa;

   // general
   private JCheckBox showQueueTab;
   private JCheckBox showGrandTotal;
   private JCheckBox holdInvoice;
   private JCheckBox autoComplete;
   private JCheckBox warnNotPrinted;
   private JCheckBox enableTracking;
   private JCheckBox localEnabled;
   private JTextField localDirectory;
   private JCheckBox localDisableInvoice;
   private JCheckBox localDisableLabel;
   private JCheckBox localDisableJpegInv;
   private JComboBox logLevel;
   private JComboBox threadList;

   // notification
   private JCheckBox[] useMessage;
   private JCheckBox[] useTaskBarFlash;
   private JCheckBox[] useStatusScreen;
   private JCheckBox[] useSound;
   private JComboBox[] soundType;
   private JTextField[] soundFile;
   private IntervalField soonInterval;
   private IntervalField renotifyInterval;
   private JRadioButton skuDueAll;
   private JRadioButton skuDueSome;
   private JRadioButton skuDueNone;

   // protected mode
   private JCheckBox minimizeAtStart; // not protected mode field, but goes with them
   private JCheckBox minimizeAtClose;
   private JCheckBox passwordForExit;
   private JCheckBox passwordForSetup;
   private JCheckBox passwordForUnminimize;
   private JTextField unminimizePassword;
   private JCheckBox preventStopStart;

   // merchant
   private JTextField merchant;
   private JPasswordField password;
   private JCheckBox isWholesale;
   private JTextField frameTitleSuffix;

   // queue
   private QueueCombo selectedQueue;
   private JComboBox queueFormat;
   private HashMap mapPanel;
   private ConfigFormat panelFormat;  // these four change together
   private Format saveFormatObject;   // sFO is null on the initial fake panel, and the rest are invalid
   private int[]  saveCompletionModeValues;
   private JComboBox  completionMode; //
   private Queue saveQueue;
   private JCheckBox noAutoPrint;
   private JCheckBox switchToBackup;
   private JCheckBox autoSpawn;
   private JCheckBox autoSpawnSpecial;
   private JTextField autoSpawnPrice;

   // components for format panel swap
   private JPanel swapTab;
   private JPanel swapContainer;
   private GridBagHelper swapHelper;

   // scan
   private JCheckBox downloadEnabled;
   private JCheckBox uploadEnabled;
   private JCheckBox pakonEnabled;

   // download
   private IntervalField listPollInterval;
   private JCheckBox prioritizeEnabled;
   private JCheckBox conversionEnabled;

   // upload
   private ConfigUpload configUpload;

   // purge
   private JCheckBox autoPurgeOrders;
   private JCheckBox autoPurgeJobs;
   private JCheckBox autoPurgePakon;
   private JCheckBox autoPurgeManual;
   private JCheckBox autoPurgeHot;
   private JCheckBox autoPurgeDLS;
   private JCheckBox autoPurgeStale;
   private JCheckBox autoPurgeStaleLocal;
   private IntervalField rollPurgeInterval;
   private IntervalField orderPurgeInterval;
   private IntervalField jobPurgeInterval;
   private IntervalField orderStalePurgeInterval;
   private IntervalField jobStalePurgeInterval;

   // poll
   private JTextField dirHotFuji;
   private JTextField dirHotXML;
   private JTextField dirHotOrder;
   private JTextField dirHotEmail;
   private JTextField dirHotImage;
   private JTextField throttleCount;
   private JTextField defaultEmail;
   private IntervalField imageDelayInterval;

   // DLS
   private JCheckBox scanEnabled;
   private IntervalField scannerPollInterval;
   private JTextField dlsHost;
   private JTextField dlsUserName;
   private JTextField dlsPassword;
   private DateField dlsEffectiveDate;
   private JCheckBox dlsExcludeByID;
   private JCheckBox dlsHoldConfirm;

   // print
   private JTextField copies;
   private JComboBox invoiceColor;
   private JComboBox invoiceType;
   private JCheckBox productBarcodeEnable;
   private JComboBox productBarcodeType;
   private JComboBox timeZone1;
   private JComboBox timeZone2;
   private JComboBox dateFormatInvoice;
   private JComboBox dateFormatLabel;
   private JComboBox accountType;
   private JCheckBox labelPerQueue;
   private JCheckBox showOrderNumber;
   private JCheckBox overrideAddress;
   private JTextField overrideName;
   private JTextField overrideStreet1;
   private JTextField overrideStreet2;
   private JTextField overrideCity;
   private JTextField overrideState;
   private JTextField overrideZIP;
   private JCheckBox markShip;
   private JRadioButton markShipUseShipMth;
   private JRadioButton markShipUseMessage;
   private JTextField markShipMessage;
   private JCheckBox markPay;
   private JComboBox markPayIncludeTax;
   private JTextField markPayAddMessage;
   private JCheckBox markPro;
   private JTextField markProNotMerchantID;
   private JCheckBox markPre;
   private JTextField markPreMessage;
   private JCheckBox showPickupLocation;
   private ConfigSetup setupInvoice;
   private ConfigSetup setupLabel;
   private JCheckBox productEnableForHome;
   private JCheckBox productEnableForAccount;

   // logo
   private JTextField logoFile;
   private JTextField logoWidth;
   private JTextField logoHeight;

   // JPEG invoice
   private JCheckBox jpegEnableForHome;
   private JCheckBox jpegEnableForAccount;
   private JComboBox jpegShowColor;
   private JComboBox jpegSKU;
   private JTextField jpegWidth;
   private JTextField jpegHeight;
   private JComboBox jpegOrientation;
   private JTextField jpegBorderPixels;

   // network
   private ConfigNetwork configNetwork;

   // auto-update
   private JCheckBox enableRestart;
   private JComboBox restartHour1;
   private JComboBox restartHour2;
   private IntervalField restartWaitInterval;

   // kiosk
   private JCheckBox localShareEnabled;
   private JTextField localShareDir;
   private IntervalField localImagePollInterval;
   private JCheckBox rollReceivedEnabled;
   private IntervalField rollReceivedPurgeInterval;

// --- combo boxes ---

   private static Object[] invoiceColorNames = new Object[] { Text.get(ConfigDialog.class,"ic0"),
                                                              Text.get(ConfigDialog.class,"ic1")  };

   private static final int INVOICE_COLOR_BANDW = 0;
   private static final int INVOICE_COLOR_COLOR = 1;

   private static int[] invoiceColorValues = new int[] { INVOICE_COLOR_BANDW,
                                                         INVOICE_COLOR_COLOR  };

   private static Object[] invoiceTypeNames = new Object[] { Text.get(ConfigDialog.class,"it0"),
                                                             Text.get(ConfigDialog.class,"it1")  };

   private static final int INVOICE_TYPE_COMPLETE = 0;
   private static final int INVOICE_TYPE_SUMMARY  = 1;

   private static int[] invoiceTypeValues = new int[] { INVOICE_TYPE_COMPLETE,
                                                        INVOICE_TYPE_SUMMARY   };

   // date format combo is special, see next section

   private static Object[] accountTypeNames = new Object[] { Text.get(ConfigDialog.class,"at0"),
                                                             Text.get(ConfigDialog.class,"at1")  };

   private static final int ACCOUNT_TYPE_STANDARD = 0;
   private static final int ACCOUNT_TYPE_SUMMARY  = 1;

   private static int[] accountTypeValues = new int[] { ACCOUNT_TYPE_STANDARD,
                                                        ACCOUNT_TYPE_SUMMARY  };

   private static Object[] soundTypeNames = new Object[] { Text.get(ConfigDialog.class,"st0"),
                                                           Text.get(ConfigDialog.class,"st1"),
                                                           Text.get(ConfigDialog.class,"st2")  };

   private static int[] soundTypeValues = new int[] { User.SOUND_VOICE,
                                                      User.SOUND_CHIME,
                                                      User.SOUND_FILE   };

   private static Object[] noVoiceNames = new Object[] { Text.get(ConfigDialog.class,"nv0"),
                                                         Text.get(ConfigDialog.class,"nv1")  };

   private static int[] noVoiceValues = new int[] { User.SOUND_CHIME,
                                                    User.SOUND_FILE   };

   private static Object[] hourNames  = new Object[24];
   private static int[]    hourValues = new int   [24];
   static {

      String am = ' ' + Text.get(ConfigDialog.class,"hra");
      String pm = ' ' + Text.get(ConfigDialog.class,"hrp");

      for (int i=0; i<24; i++) {
         int hour = i - 12; // want midnight adjacent to the PM hours
         if (hour < 0) hour += 24;

         String name;
         if      (hour ==  0) name = Text.get(ConfigDialog.class,"hrm");
         else if (hour == 12) name = Text.get(ConfigDialog.class,"hrn");
         else if (hour <  12) name = Convert.fromInt(hour   ) + am;
         else                 name = Convert.fromInt(hour-12) + pm;

         hourNames [i] = name;
         hourValues[i] = hour;
      }
   }

   private static Object[] completionModeNames = new Object[] { Text.get(ConfigDialog.class,"cm0"),
                                                                Text.get(ConfigDialog.class,"cm1"),
                                                                Text.get(ConfigDialog.class,"cm2"),
                                                                Text.get(ConfigDialog.class,"cm3")  };

   private static int[] completionModeValues = new int[] { Format.COMPLETION_MODE_MANUAL,
                                                           Format.COMPLETION_MODE_AUTO,
                                                           Format.COMPLETION_MODE_DETECT,
                                                           Format.COMPLETION_MODE_ACCEPT  };

   public static Object[] includeTaxNames = new Object[] { Text.get(ConfigDialog.class,"ix0"),
                                                           Text.get(ConfigDialog.class,"ix1")  };

   public static final int INCLUDE_TAX_NO  = 0;
   public static final int INCLUDE_TAX_YES = 1;

   public static int[] includeTaxValues = new int[] { INCLUDE_TAX_NO,
                                                      INCLUDE_TAX_YES };

   private static Object[] productBarcodeTypeNames = new Object[] { Text.get(ConfigDialog.class,"pb0"),
                                                                    Text.get(ConfigDialog.class,"pb1"),
                                                                    Text.get(ConfigDialog.class,"pb2")  };

   private static int[] productBarcodeTypeValues = new int[] { ProductBarcode.TYPE_CODE128_STRING,
                                                               ProductBarcode.TYPE_CODE128_DIGITS,
                                                               ProductBarcode.TYPE_EAN13_20_5_5    };

   private static Object[] logLevelObjects = new Object[] { Level.SEVERE,
                                                            Level.WARNING,
                                                            Level.INFO,
                                                            Level.CONFIG,
                                                            Level.FINE,
                                                            Level.FINER,
                                                            Level.FINEST   };

// --- related utility ---

   private static HashSet getUnlockedFormats(Config config) {

      HashSet formats = new HashSet();

      Iterator i = config.queueList.queues.iterator();
      while (i.hasNext()) {
         formats.add(new Integer(((Queue) i.next()).format));
      }

      i = config.formatUnlockList.iterator();
      while (i.hasNext()) {
         formats.add(i.next()); // ok to share, Integer is immutable
      }

      return formats;
   }

// --- date format combo ---

   private static class StandardDateFormat {

      private String value;
      private String text;

      public StandardDateFormat(String name, String value) {
         this.value = value;
         text = Text.get(ConfigDialog.class,"dfx",new Object[] { name, value });
      }

      public String getValue() { return value; }
      public String toString() { return text; }
   }

   private static final StandardDateFormat[] dateFormatObjects = new StandardDateFormat[] {

      new StandardDateFormat(Text.get(ConfigDialog.class,"df0"),PrintConfig.FORMAT_24_HR   ),
      new StandardDateFormat(Text.get(ConfigDialog.class,"df1"),PrintConfig.FORMAT_24_HR_TZ),
      new StandardDateFormat(Text.get(ConfigDialog.class,"df2"),PrintConfig.FORMAT_AM_PM   ),
      new StandardDateFormat(Text.get(ConfigDialog.class,"df3"),PrintConfig.FORMAT_AM_PM_TZ),

      new StandardDateFormat(Text.get(ConfigDialog.class,"df4"),PrintConfig.INTL_24_HR   ),
      new StandardDateFormat(Text.get(ConfigDialog.class,"df5"),PrintConfig.INTL_24_HR_TZ),
      new StandardDateFormat(Text.get(ConfigDialog.class,"df6"),PrintConfig.INTL_AM_PM   ),
      new StandardDateFormat(Text.get(ConfigDialog.class,"df7"),PrintConfig.INTL_AM_PM_TZ)
   };

   private static JComboBox constructDateFormatCombo() {
      JComboBox combo = new JComboBox(dateFormatObjects);
      combo.setEditable(true);
      return combo;
   }

   private static void putDateFormatCombo(JComboBox combo, String s) {
      for (int i=0; i<dateFormatObjects.length; i++) {
         if (s.equals(dateFormatObjects[i].getValue())) { combo.setSelectedIndex(i); return; }
      }
      combo.setSelectedItem(s); // set as plain string
   }

   private static String getDateFormatCombo(JComboBox combo) {
      Object o = combo.getSelectedItem();
      if (o instanceof StandardDateFormat) {
         return ((StandardDateFormat) o).getValue();
      } else {
         return (String) o; // must be a plain string
      }
   }

// --- construction ---

   private static final String titleStatic = Text.get(ConfigDialog.class,"s1");

   /**
    * @param config An object that will be modified by the dialog.
    */
   public ConfigDialog(Frame owner, Config config, final SKU forceSKU) {
      this(owner,config,/* disableOK = */ false,forceSKU);
   }
   public ConfigDialog(Frame owner, Config config, boolean disableOK, final SKU forceSKU) {
      super(owner,titleStatic);

      this.config = config;
      prepareSaveFormats();
      saveEffectiveDate = config.scanConfigDLS.effectiveDate;
      saveProductLabels = (config.printConfig.productEnableForHome || config.printConfig.productEnableForAccount);
      showKiosk = true; // (config.localShareEnabled != null);

      construct(constructFields(),/* readonly = */ false,/* resizable = */ false,/* altOK = */ true,disableOK);
      changeTab(tabScan,tabScanCover); // after pack

      if (forceSKU != null) {
         EventQueue.invokeLater(new Runnable() { public void run() { doEditSKU(forceSKU); } });
      }
   }

   public ConfigDialog(Dialog owner, Config config) {
      super(owner,titleStatic);

      this.config = config;
      prepareSaveFormats();
      saveEffectiveDate = config.scanConfigDLS.effectiveDate;
      saveProductLabels = (config.printConfig.productEnableForHome || config.printConfig.productEnableForAccount);
      showKiosk = true; // (config.localShareEnabled != null);

      construct(constructFields(),/* readonly = */ false,/* resizable = */ false,/* altOK = */ true,/* disableOK = */ false);
      changeTab(tabScan,tabScanCover); // after pack
   }

   private void changeTab(JPanel before, JPanel after) {
      int i = tabbedPane.indexOfComponent(before);
      if (i != -1) {
         tabbedPane.setComponentAt(i,after);
         repaint(); // doesn't fire automatically for some reason
      }
   }

// --- memory ---

   private static Integer lastTab;
   private static String lastQueue; // id

   public void dispose() {

      lastTab = new Integer(tabbedPane.getSelectedIndex());
      lastQueue = selectedQueue.get();

      super.dispose();
   }

// --- methods ---

   private JComponent constructFields() {
      GridBagHelper helper;

   // constants

      int w6 = Text.getInt(this,"w6"); // width for small numbers

   // format arrays

      HashSet formats = getUnlockedFormats(config);
      OrderDialog.addPublicFormats(formats);
      // compute this dynamically for restore dialog

      fa = OrderDialog.getFormatArrays(formats);

   // fields

      showQueueTab = new JCheckBox(Text.get(this,"s61"));
      showGrandTotal = new JCheckBox();
      holdInvoice = new JCheckBox(Text.get(this,"s65"));
      autoComplete = new JCheckBox(Text.get(this,"s69"));
      warnNotPrinted = new JCheckBox(Text.get(this,"s66"));
      enableTracking = new JCheckBox(Text.get(this,"s262"));
      localEnabled = new JCheckBox();
      localDirectory = ConfigFormat.constructDataDir();
      localDisableInvoice = new JCheckBox();
      localDisableLabel   = new JCheckBox();
      localDisableJpegInv = new JCheckBox();
      logLevel = new JComboBox(logLevelObjects);
      threadList = new JComboBox();
      updateThreadList();

      useMessage = new JCheckBox[User.CODE_LIMIT];
      useTaskBarFlash = new JCheckBox[User.CODE_LIMIT];
      useStatusScreen = new JCheckBox[User.CODE_LIMIT];
      useSound = new JCheckBox[User.CODE_LIMIT];
      soundType = new JComboBox[User.CODE_LIMIT];
      soundFile = new JTextField[User.CODE_LIMIT];
      for (int i=0; i<User.CODE_LIMIT; i++) {
         useMessage[i] = new JCheckBox();
         useTaskBarFlash[i] = new JCheckBox();
         useStatusScreen[i] = new JCheckBox();
         useSound[i] = new JCheckBox();
         soundType[i] = new JComboBox(User.hasVoice(i) ? soundTypeNames : noVoiceNames);
         soundFile[i] = new DisableTextField(Text.getInt(this,"w8"));
      }
      soonInterval = new IntervalField(IntervalField.MINUTES,IntervalField.HOURS);
      renotifyInterval = new IntervalField(IntervalField.MINUTES,IntervalField.HOURS);
      skuDueAll = new JRadioButton(Text.get(this,"s111"));
      skuDueSome = new JRadioButton();
      skuDueNone = new JRadioButton(Text.get(this,"s113"));

      ButtonGroup group = new ButtonGroup();
      group.add(skuDueAll);
      group.add(skuDueSome);
      group.add(skuDueNone);

      minimizeAtStart = new JCheckBox(Text.get(this,"s210"));
      minimizeAtClose = new JCheckBox();
      passwordForExit = new JCheckBox();
      passwordForSetup = new JCheckBox();
      passwordForUnminimize = new JCheckBox();
      unminimizePassword = new JTextField(Text.getInt(this,"w25"));
      preventStopStart = new JCheckBox();

      merchant = new JTextField(Text.getInt(this,"w2"));
      int w3 = Text.getInt(this,"w3");
      password = new JPasswordField(w3);
      isWholesale = new JCheckBox(Text.get(this,"s188"));
      frameTitleSuffix = new JTextField(w3);

      selectedQueue = new QueueCombo(config.queueList,null,null);
      queueFormat = new JComboBox(fa.formatNames);
      queueFormat.setMaximumRowCount(12); // make these not scroll; also allow for future expansion
      //
      selectedQueue.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { queueChanged(); } });
      queueFormat  .addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { queueFormatChanged(); } });
      //
      mapPanel = new HashMap();
      // panelFormat starts out null
      completionMode = new JComboBox(); // values set in putCompletionMode
      noAutoPrint = new JCheckBox(Text.get(this,"s277"));
      switchToBackup = new JCheckBox(Text.get(this,"s278"));
      autoSpawn = new JCheckBox(Text.get(this,"s60"));
      autoSpawnSpecial = new JCheckBox();
      autoSpawnPrice = new JTextField(Text.getInt(this,"w26"));

      downloadEnabled = new JCheckBox(Text.get(this,"s26"));
      uploadEnabled = new JCheckBox(Text.get(this,"s27"));
      pakonEnabled = new JCheckBox(Text.get(this,"s2"));

      listPollInterval = new IntervalField(IntervalField.SECONDS,IntervalField.MINUTES);
      prioritizeEnabled = new JCheckBox(Text.get(this,"s203"));
      conversionEnabled = new JCheckBox(Text.get(this,"s214"));

      configUpload = new ConfigUpload(/* includeCheckboxes = */ true);

      autoPurgeOrders = new JCheckBox();
      autoPurgeJobs = new JCheckBox();
      autoPurgePakon = new JCheckBox(Text.get(this,"s3"));
      autoPurgeManual = new JCheckBox(Text.get(this,"s4"));
      autoPurgeHot = new JCheckBox(Text.get(this,"s67"));
      autoPurgeDLS = new JCheckBox();
      autoPurgeStale = new JCheckBox();
      autoPurgeStaleLocal = new JCheckBox();
      rollPurgeInterval = new IntervalField(IntervalField.HOURS,IntervalField.DAYS);
      orderPurgeInterval = new IntervalField(IntervalField.HOURS,IntervalField.DAYS);
      jobPurgeInterval = new IntervalField(IntervalField.HOURS,IntervalField.DAYS);
      orderStalePurgeInterval = new IntervalField(IntervalField.HOURS,IntervalField.DAYS);
      jobStalePurgeInterval = new IntervalField(IntervalField.HOURS,IntervalField.DAYS);

      dirHotFuji = ConfigFormat.constructDataDir();
      dirHotXML = ConfigFormat.constructDataDir();
      dirHotOrder = ConfigFormat.constructDataDir();
      dirHotEmail = ConfigFormat.constructDataDir();
      dirHotImage = ConfigFormat.constructDataDir();
      throttleCount = new JTextField(Text.getInt(this,"w7"));
      defaultEmail = new JTextField(Text.getInt(this,"w15"));
      imageDelayInterval = new IntervalField(IntervalField.SECONDS,IntervalField.MINUTES);

      scanEnabled = new JCheckBox(Text.get(this,"s122"));
      scannerPollInterval = new IntervalField(IntervalField.SECONDS,IntervalField.HOURS);
      int w9 = Text.getInt(this,"w9");
      dlsHost = new JTextField(w9);
      dlsUserName = new JTextField(w9);
      dlsPassword = new JTextField(w9);
      dlsEffectiveDate = new DateField();
      dlsExcludeByID = new JCheckBox(Text.get(this,"s133"));
      dlsHoldConfirm = new JCheckBox(Text.get(this,"s131"));

      copies = new JTextField(w6);
      invoiceColor = new JComboBox(invoiceColorNames);
      invoiceType = new JComboBox(invoiceTypeNames);
      productBarcodeEnable = new JCheckBox(Text.get(this,"s248"));
      productBarcodeType = new JComboBox(productBarcodeTypeNames);
      timeZone1 = new JComboBox(TimeZoneUtil.getArrayFirst());
      timeZone2 = new JComboBox(); // items are set later
      timeZone1.setEditable(true);
      timeZone2.setEditable(true);
      timeZone1.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { adjustTimeZone2(); } });
      dateFormatInvoice = constructDateFormatCombo();
      dateFormatLabel = constructDateFormatCombo();
      accountType = new JComboBox(accountTypeNames);
      labelPerQueue = new JCheckBox(Text.get(this,"s170"));
      showOrderNumber = new JCheckBox(Text.get(this,"s183"));
      overrideAddress = new JCheckBox(Text.get(this,"s184"));
      int w16 = Text.getInt(this,"w16");
      overrideName = new JTextField(w16);
      overrideStreet1 = new JTextField(w16);
      overrideStreet2 = new JTextField(w16);
      overrideCity = new JTextField(Text.getInt(this,"w17"));
      overrideState = new JTextField(Text.getInt(this,"w18"));
      overrideZIP = new JTextField(Text.getInt(this,"w19"));
      markShip = new JCheckBox(Text.get(this,"s231"));
      markShipUseShipMth = new JRadioButton(Text.get(this,"s232"));
      markShipUseMessage = new JRadioButton(Text.get(this,"s233"));
      markShipMessage = new JTextField(Text.getInt(this,"w23"));
      markPay = new JCheckBox(Text.get(this,"s195"));
      markPayIncludeTax = new JComboBox(includeTaxNames);
      markPayAddMessage = new JTextField(Text.getInt(this,"w21"));
      markPro = new JCheckBox(Text.get(this,"s196"));
      markProNotMerchantID = new JTextField(Text.getInt(this,"w20"));
      markPre = new JCheckBox(Text.get(this,"s234"));
      markPreMessage = new JTextField(Text.getInt(this,"w24"));
      showPickupLocation = new JCheckBox(Text.get(this,"s259"));
      setupInvoice = new ConfigSetup(this,config.printConfig.setupInvoice);
      setupLabel   = new ConfigSetup(this,config.printConfig.setupLabel  );
      productEnableForHome = new JCheckBox(Text.get(this,"s274"));
      productEnableForAccount = new JCheckBox(Text.get(this,"s275"));

      group = new ButtonGroup();
      group.add(markShipUseShipMth);
      group.add(markShipUseMessage);

      logoFile = ConfigFormat.constructDataDir();
      int w13 = Text.getInt(this,"w13");
      logoWidth = new JTextField(w13);
      logoHeight = new JTextField(w13);

      jpegEnableForHome = new JCheckBox(Text.get(this,"s152"));
      jpegEnableForAccount = new JCheckBox(Text.get(this,"s153"));
      jpegShowColor = new JComboBox(invoiceColorNames);
      jpegSKU = new JComboBox(); // standard SKUs added in put function
      jpegSKU.setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
         // this prevents the box from getting too large when there are
         // photo books with a zillion attributes.  it also doesn't let
         // the box get smaller, which is unfortunate but not too awful.
      int w14 = Text.getInt(this,"w14");
      jpegWidth = new JTextField(w14);
      jpegHeight = new JTextField(w14);
      jpegOrientation = new JComboBox(ConfigSetup.orientationNames);
      jpegBorderPixels = new JTextField(Text.getInt(this,"w22"));

      configNetwork = new ConfigNetwork(/* includeStartOverInterval = */ true);

      enableRestart = new JCheckBox();
      restartHour1 = new JComboBox(hourNames);
      restartHour2 = new JComboBox(hourNames);
      restartWaitInterval = new IntervalField(IntervalField.SECONDS,IntervalField.MINUTES);

      if (showKiosk) {
         localShareEnabled = new JCheckBox(Text.get(this,"s267"));
         localShareDir = ConfigFormat.constructDataDir();
         localImagePollInterval = new IntervalField(IntervalField.SECONDS,IntervalField.MINUTES);
         rollReceivedEnabled = new JCheckBox();
         rollReceivedPurgeInterval = new IntervalField(IntervalField.MINUTES,IntervalField.DAYS);
      }

      if ( ! config.queueList.enableBackup ) switchToBackup.setVisible(false);
      // the other approach, build everything but don't show the field

   // merchant

      JPanel panelMerchant = new JPanel();
      panelMerchant.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s5")));

      helper = new GridBagHelper(panelMerchant);

      helper.add(0,0,new JLabel(Text.get(this,"s7a")));
      helper.add(0,1,new JLabel(Text.get(this,"s7b") + ' '),GridBagHelper.alignRight);
      helper.add(0,2,new JLabel(Text.get(this,"s8") + ' '));
      helper.add(0,3,Box.createVerticalStrut(Text.getInt(this,"d27")));
      helper.add(0,4,new JLabel(Text.get(this,"s243") + ' '));

      helper.add    (1,1,merchant);
      helper.addFill(1,2,password);
      helper.addFill(1,4,frameTitleSuffix);// fill because JPasswordField and JTextField with same width aren't same size

   // wholesale

      JPanel panelWholesale = new JPanel();
      panelWholesale.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s189")));

      helper = new GridBagHelper(panelWholesale);

      helper.add    (0,0,  isWholesale);
      helper.add    (1,0,  Box.createHorizontalStrut(Text.getInt(this,"d16")));
      helper.addSpan(2,0,4,new JLabel(Text.get(this,"s191"))); // one col added, see (*)

      JButton buttonRefresh = new JButton(Text.get(this,"s190"));
      buttonRefresh.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doRefresh(); } });

      helper.addSpan(0,1,3,buttonRefresh);
      helper.add    (3,1,  Box.createHorizontalStrut(Text.getInt(this,"d17")));
      helper.add    (4,1,  new JLabel(Text.get(this,"s192")));

   // queue (1)

      JPanel panelQueue1 = new JPanel();
      panelQueue1.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s48")));

      JButton buttonEditSKU = new JButton(Text.get(this,"s49"));
      buttonEditSKU.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doEditSKU(/* forceSKU = */ null); } });

      helper = new GridBagHelper(panelQueue1);

      int y = 0;

      helper.add(0,y,buttonEditSKU);
      y++;

   // queue (2)

      JPanel panelQueue2 = new JPanel();
      // no border

      JButton buttonCreate = new JButton(Text.get(this,"s54"));
      buttonCreate.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { createQueue(); } });

      JButton buttonRename = new JButton(Text.get(this,"s70"));
      buttonRename.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { renameQueue(); } });

      JButton buttonDelete = new JButton(Text.get(this,"s74"));
      buttonDelete.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { deleteQueue(); } });

      helper = new GridBagHelper(panelQueue2);

      y = 0;

      helper.add(0,y,new JLabel(Text.get(this,"s52") + ' '));
      helper.addSpan(1,y,5,selectedQueue);
      y++;

      int d5 = Text.getInt(this,"d5");
      helper.add(0,y,Box.createVerticalStrut(d5));
      y++;

      int d1 = Text.getInt(this,"d1");
      helper.add(1,y,buttonCreate);
      helper.add(2,y,Box.createHorizontalStrut(d1));
      helper.add(3,y,buttonRename);
      helper.add(4,y,Box.createHorizontalStrut(d1));
      helper.add(5,y,buttonDelete);
      y++;

      helper.addSpan(0,y,6,new JLabel(Text.get(this,"s53")));
      y++;
      helper.addSpan(1,y,5,queueFormat);
      y++;

      helper.addSpan(0,y,6,noAutoPrint);
      y++;
      helper.addSpan(0,y,6,switchToBackup);
      y++;

      helper.addSpan(0,y,6,new JLabel(Text.get(this,"s180")));
      y++;
      helper.addSpan(1,y,5,completionMode);
      y++;

   // queue (3)

      panelFormat = new ConfigZBE(this,(ZBEConfig) new ZBEConfig().loadDefault());
      // the panel is disposable, not in map
      // pack using the largest format, then replace later, in put

   // queue (2 & 3)

      JPanel panelQueue23 = new JPanel();
      panelQueue23.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s51")));

      helper = new GridBagHelper(panelQueue23);

      helper.addCenter(0,0,panelQueue2);
      helper.add      (0,1,Box.createVerticalStrut(Text.getInt(this,"d4")));
      helper.addFill  (0,2,panelFormat); // must match queueFormatChanged!

      helper.setColumnWeight(0,1);

      swapContainer = panelQueue23;
      swapHelper = helper;

   // workflow setup

      JPanel queueManagement = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      queueManagement.add(showQueueTab);
      queueManagement.add(new JLabel(Text.get(this,"s212")));
      queueManagement.add(showGrandTotal);
      queueManagement.add(new JLabel(Text.get(this,"s213")));

      JPanel autoSpawnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      autoSpawnPanel.add(autoSpawn);
      autoSpawnPanel.add(new JLabel(Text.get(this,"s222")));
      autoSpawnPanel.add(autoSpawnSpecial);
      autoSpawnPanel.add(new JLabel(Text.get(this,"s223")));

      JPanel autoSpawnMore = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      autoSpawnMore.add(new JLabel(Text.get(this,"s284") + ' '));
      autoSpawnMore.add(autoSpawnPrice);
      autoSpawnMore.add(new JLabel(' ' + Text.get(this,"s285")));

      JButton buttonAutoComplete = new JButton(Text.get(this,"s247"));
      buttonAutoComplete.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doEditAutoComplete(); } });

      JButton buttonEnableTracking = new JButton(Text.get(this,"s263"));
      buttonEnableTracking.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doEditEnableTracking(); } });

      JPanel flowLocal = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      flowLocal.add(new JLabel(Text.get(this,"s216") + ' '));
      flowLocal.add(localDirectory);
      flowLocal.add(Box.createHorizontalStrut(Text.getInt(this,"d22")));
      flowLocal.add(ConfigFormat.makePicker(localDirectory,this,null));

      JPanel flowDisable = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      flowDisable.add(new JLabel(      Text.get(this,"s227") + ' '));
      flowDisable.add(localDisableInvoice);
      flowDisable.add(new JLabel(' ' + Text.get(this,"s228") + ' '));
      flowDisable.add(localDisableLabel  );
      flowDisable.add(new JLabel(' ' + Text.get(this,"s229") + ' '));
      flowDisable.add(localDisableJpegInv);
      flowDisable.add(new JLabel(' ' + Text.get(this,"s230")      ));

   // workflow

      JPanel panelWorkflow = new JPanel();
      panelWorkflow.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s59")));

      helper = new GridBagHelper(panelWorkflow);

      helper.addSpan(0,0,4,minimizeAtStart);

      helper.addSpan(0,1,4,holdInvoice); // ordered by workflow sequence, roughly

      helper.addSpan(0,2,4,queueManagement);

      helper.addSpan(0,3,4,autoSpawnPanel);
      helper.addSpan(1,4,3,autoSpawnMore);

      helper.addSpan(0,5,2,autoComplete);
      helper.add(2,5,Box.createHorizontalStrut(Text.getInt(this,"d28")));
      helper.add(3,5,buttonAutoComplete);

      helper.addSpan(0,6,4,warnNotPrinted);

      helper.addSpan(0,7,2,enableTracking);
      helper.add(3,7,buttonEnableTracking);

      helper.add(0,8,localEnabled);
      helper.addSpan(1,8,3,flowLocal);
      helper.addSpan(1,9,3,flowDisable);

   // enable

      JPanel panelEnable = new JPanel();
      panelEnable.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s15")));

      helper = new GridBagHelper(panelEnable);

      helper.add(0,0,downloadEnabled);
      helper.add(0,1,uploadEnabled);

   // download

      JPanel panelDownload = new JPanel();
      panelDownload.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s16")));

      helper = new GridBagHelper(panelDownload);

      helper.add(0,0,new JLabel(Text.get(this,"s44") + ' '));
      helper.add(1,0,listPollInterval);

      helper.addSpan(0,1,2,prioritizeEnabled);

   // bandwidth

      JPanel panelBandwidth = new JPanel();
      panelBandwidth.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s224")));

      JButton buttonDLBW = new JButton(Text.get(this,"s225"));
      buttonDLBW.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doEditDLBW(); } });

      JButton buttonULBW = new JButton(Text.get(this,"s226"));
      buttonULBW.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doEditULBW(); } });

      helper = new GridBagHelper(panelBandwidth);

      helper.add(0,0,buttonDLBW);
      helper.add(1,0,Box.createHorizontalStrut(Text.getInt(this,"d25")));
      helper.add(2,0,buttonULBW);

   // purge

      JPanel panelPurge = new JPanel();
      panelPurge.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s22")));

      helper = new GridBagHelper(panelPurge);

      y = 0;

      JCheckBox layoutBox = new JCheckBox();
      layoutBox.setEnabled(false);
      int d23 = Text.getInt(this,"d23");

      helper.add(0,y,layoutBox);
      helper.addSpan(1,y,2,new JLabel(Text.get(this,"s23") + ' '));
      helper.add(3,y,rollPurgeInterval);
      y++;

      helper.add(1,y,Box.createHorizontalStrut(Text.getInt(this,"d18")));
      helper.addSpan(2,y,2,autoPurgeManual);
      y++;

      helper.addSpan(2,y,2,autoPurgeHot);
      y++;

      helper.add(0,y,autoPurgeOrders);
      helper.addSpan(1,y,2,new JLabel(Text.get(this,"s24") + ' '));
      helper.add(3,y,orderPurgeInterval);
      helper.add(4,y,Box.createHorizontalStrut(d23));
      helper.add(5,y,new JLabel(Text.get(this,"s217")));
      helper.add(6,y,Box.createHorizontalStrut(d23));
      helper.addSpan(7,y,3,orderStalePurgeInterval);
      y++;

      helper.add(0,y,autoPurgeJobs);
      helper.addSpan(1,y,2,new JLabel(Text.get(this,"s63") + ' '));
      helper.add(3,y,jobPurgeInterval);
      helper.addSpan(7,y,3,jobStalePurgeInterval);
      y++;

      helper.add(3,y,new JLabel(Text.get(this,"s218")));
      helper.add(7,y,new JLabel(Text.get(this,"s219")));
      helper.add(8,y,autoPurgeStale);
      helper.add(9,y,new JLabel(Text.get(this,"s220")));
      y++;

      helper.add(8,y,autoPurgeStaleLocal);
      helper.add(9,y,new JLabel(Text.get(this,"s221")));
      y++;

   // pakon

      JPanel panelPakon = new JPanel();
      panelPakon.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s40")));

      helper = new GridBagHelper(panelPakon);

      helper.add(0,0,pakonEnabled);
      helper.add(0,1,autoPurgePakon);

   // poll sub-panels

      int d26 = Text.getInt(this,"d26");

      JPanel pollSubpanel1 = new JPanel();
      helper = new GridBagHelper(pollSubpanel1);

      helper.add(0,0,Box.createHorizontalStrut(d26));
      helper.add(1,0,new JLabel(Text.get(this,"s99") + ' '));
      helper.add(2,0,throttleCount);
      helper.add(3,0,new JLabel(' ' + Text.get(this,"s100")));
      helper.addSpan(1,1,3,new JLabel(Text.get(this,"s101")));
      helper.addSpan(1,2,3,new JLabel(Text.get(this,"s102")));

      JPanel pollSubpanel2 = new JPanel();
      helper = new GridBagHelper(pollSubpanel2);

      helper.add(0,0,Box.createHorizontalStrut(d26));
      helper.add(1,0,new JLabel(Text.get(this,"s167") + ' '));
      helper.add(2,0,defaultEmail);

      helper.setColumnWeight(2,1);

      JPanel pollSubpanel3 = new JPanel();
      helper = new GridBagHelper(pollSubpanel3);

      helper.add(0,0,Box.createHorizontalStrut(d26));
      helper.add(1,0,new JLabel(Text.get(this,"s241") + ' '));
      helper.add(2,0,imageDelayInterval);
      helper.add(3,0,new JLabel(' ' + Text.get(this,"s242")));

   // poll

      JPanel panelPoll = new JPanel();
      panelPoll.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s68")));

      helper = new GridBagHelper(panelPoll);

      int d13 = Text.getInt(this,"d13");

      y = 0;
      y = ConfigFormat.addDataDir(this,helper,y,Text.get(this,"s165"),dirHotFuji,/* pack = */ true);

      helper.addSpanFill(0,y,4,pollSubpanel2);
      y++;
      helper.add(0,y,Box.createVerticalStrut(d13));
      y++;

      y = ConfigFormat.addDataDir(this,helper,y,Text.get(this,"s166"),dirHotXML, /* pack = */ true);

      helper.addSpan(0,y,4,pollSubpanel1);
      y++;
      helper.add(0,y,Box.createVerticalStrut(d13));
      y++;

      y = ConfigFormat.addDataDir(this,helper,y,Text.get(this,"s194"),dirHotOrder,/* pack = */ true);
      y = ConfigFormat.addDataDir(this,helper,y,Text.get(this,"s215"),dirHotEmail,/* pack = */ true);

      helper.add(0,y,Box.createVerticalStrut(d13));
      y++;

      y = ConfigFormat.addDataDir(this,helper,y,Text.get(this,"s240"),dirHotImage,/* pack = */ true);

      helper.addSpan(0,y,4,pollSubpanel3);
      y++;

   // DLS

      JPanel panelDLS = new JPanel();
      panelDLS.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s121")));

      helper = new GridBagHelper(panelDLS);

      helper.addSpan(0,0,5,scanEnabled);
      helper.add    (0,1,  autoPurgeDLS);
      helper.addSpan(1,1,5,new JLabel(Text.get(this,"s123")));       // +1 (*)
      helper.addSpan(1,2,5,new JLabel(Text.get(this,"s124")));       // +1 (*)

      int d9 = Text.getInt(this,"d9");
      helper.add(0,3,Box.createVerticalStrut(d9));

      helper.addSpan(0,4,2,new JLabel(Text.get(this,"s125") + ' '));
      helper.addSpan(2,4,5,dlsHost);                                 // +2 (*)
      helper.addSpan(0,5,2,new JLabel(Text.get(this,"s126") + ' '));
      helper.addSpan(2,5,5,dlsUserName);                             // +2 (*)
      helper.addSpan(0,6,2,new JLabel(Text.get(this,"s127") + ' '));
      helper.addSpan(2,6,5,dlsPassword);                             // +2 (*)

      helper.add(0,7,Box.createVerticalStrut(d9));

      helper.addSpan(0,8,3,new JLabel(Text.get(this,"s128") + ' '));
      helper.addSpan(3,8,5,dlsEffectiveDate);                        // +3 (*)
      helper.addSpan(0,9,5,new JLabel(Text.get(this,"s129")));

      helper.add(0,10,Box.createVerticalStrut(d9));

      helper.addSpan(0,11,4,new JLabel(Text.get(this,"s130") + ' '));
      helper.add    (4,11,scannerPollInterval);
      helper.addSpan(0,12,5,dlsExcludeByID);
      helper.addSpan(0,13,5,dlsHoldConfirm);

   // other

      JPanel panelOther = new JPanel();
      panelOther.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s117")));

      helper = new GridBagHelper(panelOther);

      helper.add(0,0,new JLabel(Text.get(this,"s94") + ' '));
      helper.add(1,0,timeZone1);
      helper.add(2,0,Box.createHorizontalStrut(Text.getInt(this,"d24")));
      helper.add(3,0,timeZone2);

      helper.add(0,1,Box.createVerticalStrut(Text.getInt(this,"d19")));

      helper.add(0,2,new JLabel(Text.get(this,"s206") + ' '));
      helper.addSpan(1,2,3,dateFormatInvoice);
      helper.add(0,3,new JLabel(Text.get(this,"s207") + ' '));
      helper.addSpan(1,3,3,dateFormatLabel);

   // print

      JPanel panelPrint = new JPanel();
      helper = new GridBagHelper(panelPrint);

      int d3 = Text.getInt(this,"d3");

      helper.addSpan(0,0,4,new JLabel(Text.get(this,"s43") + ' '));
      helper.add    (4,0,  copies);
      helper.add    (0,1,  Box.createVerticalStrut(d3));
      helper.add    (0,2,  new JLabel(Text.get(this,"s58") + ' '));
      helper.add    (1,2,  invoiceColor);
      helper.add    (2,2,  new JLabel(" "));
      helper.addSpan(3,2,2,invoiceType);
      helper.add    (0,3,  Box.createVerticalStrut(d3));

      // (*) note about GridBagHelper
      //
      // the way it assigns widths to columns doesn't always work right.
      // what it does is, it orders the components first by grid width,
      // smallest to largest, and second by order in the component list.
      // then, for each component, it subtracts out the widths that are
      // already in the covered columns, and if the component still
      // doesn't fit, it always enlarges the <i>last covered column</i>.
      // (unless there are weights)
      //
      // here's how it worked back when invoiceType was at 3-4 and label s43 at 0-3.
      //
      //                 item   column widths
      //                 width   0    1    2    3    4
      //                 -----  ---  ---  ---  ---  ---
      // non-span items         2.3  1.7  0.1  0.0  1.0
      // invoiceType     3.1    2.3  1.7  0.1  0.0  3.1
      // label s43       5.3    2.3  1.7  0.1  1.2  3.1
      //
      // so, as a result, column 4 ended up being 1.2 units too wide.
      // I don't know any way to fix this, but you can work around it
      // by using zero-width columns to change the number of columns,
      // and hence the allocation order, or by moving the overlapping
      // spans into subpanels.

   // shipping label

      JPanel sub3 = new JPanel();
      helper = new GridBagHelper(sub3);
      helper.add(0,0,markShip);
      helper.add(1,0,markShipUseShipMth);
      helper.add(1,1,markShipUseMessage);
      helper.add(2,1,markShipMessage);

      JPanel panelLabelShipping = new JPanel();
      panelLabelShipping.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s182")));
      helper = new GridBagHelper(panelLabelShipping);

      helper.addSpan(0,0,7,showOrderNumber);
      helper.addSpan(0,1,7,overrideAddress);

      helper.add(0,2,Box.createHorizontalStrut(Text.getInt(this,"d15")));
      helper.add(1,2,new JLabel(Text.get(this,"s185") + ' '));
      helper.addSpan(2,2,5,overrideName);

      helper.add(1,3,new JLabel(Text.get(this,"s186") + ' '));
      helper.addSpan(2,3,5,overrideStreet1);

      helper.addSpan(2,4,5,overrideStreet2);

      helper.add(2,5,overrideCity);
      helper.add(3,5,new JLabel(Text.get(this,"s187") + ' '));
      helper.add(4,5,overrideState);
      helper.add(5,5,new JLabel(" "));
      helper.add(6,5,overrideZIP);

      helper.addSpan(0,6,7,sub3);

   // account label

      JPanel panelLabelAccount = new JPanel();
      panelLabelAccount.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s181")));
      helper = new GridBagHelper(panelLabelAccount);

      helper.add(0,0,new JLabel(Text.get(this,"s169") + ' '));
      helper.add(1,0,accountType);

      // order in UI (and on label) is different than field order
      //
      JPanel acctFlow1 = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      acctFlow1.add(markPro);
      acctFlow1.add(new JLabel(Text.get(this,"s197") + ' '));
      acctFlow1.add(markProNotMerchantID);
      acctFlow1.add(new JLabel(' ' + Text.get(this,"s198")));
      helper.addSpan(0,1,2,acctFlow1);
      //
      helper.addSpan(0,2,2,showPickupLocation);
      //
      JPanel acctFlow2 = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      acctFlow2.add(markPay);
      acctFlow2.add(new JLabel(Text.get(this,"s199") + ' '));
      acctFlow2.add(markPayIncludeTax);
      acctFlow2.add(new JLabel(' ' + Text.get(this,"s200")));
      helper.addSpan(0,3,2,acctFlow2);
      //
      JPanel acctFlow3 = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      acctFlow3.add(new JLabel(Text.get(this,"s201") + ' '));
      acctFlow3.add(markPayAddMessage);
      acctFlow3.add(new JLabel(' ' + Text.get(this,"s202")));
      helper.add(1,4,acctFlow3,GridBagHelper.alignRight);
      //
      JPanel acctFlow4 = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      acctFlow4.add(markPre);
      acctFlow4.add(new JLabel(Text.get(this,"s235") + ' '));
      acctFlow4.add(markPreMessage);
      acctFlow4.add(new JLabel(' ' + Text.get(this,"s236")));
      helper.addSpan(0,5,2,acctFlow4);

   // setup

      JPanel panelSetupInvoice = new JPanel();
      panelSetupInvoice.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s41")));
      helper = new GridBagHelper(panelSetupInvoice);

      helper.add(0,0,setupInvoice.getPanel(ConfigSetup.PANEL_TOP));
      helper.add(0,1,panelPrint);
      helper.add(0,2,setupInvoice.getPanel(ConfigSetup.PANEL_BOTTOM));
      //
      JPanel codeFlow = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      codeFlow.add(productBarcodeEnable);
      codeFlow.add(new JLabel(Text.get(this,"s257") + ' '));
      codeFlow.add(productBarcodeType);
      codeFlow.add(new JLabel(' ' + Text.get(this,"s258")));
      helper.add(0,3,codeFlow);
      //
      helper.add(0,4,conversionEnabled);
      // officially a DownloadConfig setting, but it goes with the invoice settings
      //
      // actually, both checkbox lines ought to sit in their own frame, since they
      // apply to all kinds of invoice-label generation, not just invoice printing

      JPanel panelModify = setupLabel.getPanel(ConfigSetup.PANEL_TOP);
      helper = GridBagHelper.modify(panelModify);
      helper.add(1,0,new JLabel(Text.get(this,"s272")));
      helper.add(2,0,new JLabel(Text.get(this,"s273")));
      helper.add(2,1,productEnableForHome);
      helper.add(2,2,productEnableForAccount);
      helper.setColumnWeight(2,1);
      // leave the weight in column 1 too

      JPanel panelSetupLabel = new JPanel();
      panelSetupLabel.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s42")));
      helper = new GridBagHelper(panelSetupLabel);

      helper.addFill(0,0,panelModify);
      helper.add(0,1,labelPerQueue);
      helper.add(0,2,setupLabel.getPanel(ConfigSetup.PANEL_BOTTOM));

   // logo

      ExtensionFileFilter logoFilter = new ExtensionFileFilter(Text.get(this,"s143"),new String[] { "gif","jpeg","jpg" });
      // what works in the Java HTML renderer is different than what works in the upload transform (ItemUtil.extensions)

      JPanel panelLogo = new JPanel();
      panelLogo.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s144")));

      helper = new GridBagHelper(panelLogo);

      JPanel logoFlow = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      logoFlow.add(logoFile);
      logoFlow.add(Box.createHorizontalStrut(Text.getInt(this,"d12")));
      logoFlow.add(ConfigFormat.makePicker(logoFile,this,logoFilter));
      helper.add(0,0,new JLabel(Text.get(this,"s145") + ' '));
      helper.add(1,0,logoFlow);

      logoFlow = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      logoFlow.add(logoWidth);
      logoFlow.add(new JLabel(' ' + Text.get(this,"s147") + ' '));
      logoFlow.add(logoHeight);
      logoFlow.add(new JLabel(' ' + Text.get(this,"s148")));
      helper.add(0,1,new JLabel(Text.get(this,"s146") + ' '));
      helper.add(1,1,logoFlow);

   // JPEG invoice

      JPanel panelJpeg = new JPanel();
      panelJpeg.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s150")));

      helper = new GridBagHelper(panelJpeg);

      helper.addSpan(0,0,2,new JLabel(Text.get(this,"s151") + ' '));
      helper.addSpan(2,0,2,jpegEnableForHome);
      helper.addSpan(2,1,2,jpegEnableForAccount);

      helper.add    (0,2,  new JLabel(Text.get(this,"s154") + ' '));
      helper.addSpan(1,2,2,jpegShowColor);
      helper.add    (3,2,  new JLabel(' ' + Text.get(this,"s155")));

      helper.add    (0,3,  new JLabel(Text.get(this,"s156") + ' '));
      helper.addSpan(1,3,3,jpegSKU);

      JPanel jpegFlow = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      jpegFlow.add(jpegWidth);
      jpegFlow.add(new JLabel(' ' + Text.get(this,"s158") + ' '));
      jpegFlow.add(jpegHeight);
      helper.add    (0,4,  new JLabel(Text.get(this,"s157") + ' '));
      helper.addSpan(1,4,3,jpegFlow);

      helper.add    (0,5,  new JLabel(Text.get(this,"s159") + ' '));
      helper.addSpan(1,5,3,jpegOrientation);

      helper.add    (0,6,  new JLabel(Text.get(this,"s209") + ' '));
      helper.addSpan(1,6,3,jpegBorderPixels);

      helper.add    (1,7,  Box.createVerticalStrut(Text.getInt(this,"d20")));

      JButton buttonBarcode = new JButton(Text.get(this,"s208"));
      buttonBarcode.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doBarcode(); } });

      helper.addSpan(1,8,3,buttonBarcode);

   // user config

      ExtensionFileFilter fileFilter = new ExtensionFileFilter(Text.get(this,"s115"),new String[] { "aiff","au","midi","rmf","wav" });

      JPanel panelUser = new JPanel();
      panelUser.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s92")));

      helper = new GridBagHelper(panelUser);

      y = 0;

      helper.addCenter(1,y,new JLabel(Text.get(this,"s103") + ' '));
      helper.addCenter(2,y,new JLabel(Text.get(this,"s104") + ' '));
      helper.addCenter(3,y,new JLabel(Text.get(this,"s105") + ' '));
      helper.addCenter(4,y,new JLabel(Text.get(this,"s106")));
      y++;

      for (int i=0; i<User.CODE_LIMIT; i++) {

         if (i > 0) {
            helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d8")));
            y++;
         }

         JPanel label = new JPanel();
         GridBagHelper helper2 = new GridBagHelper(label);
         int y2 = 0;

         String whole = Text.get(this,"s93_" + i) + '|';
         while (whole.length() > 0) {

            // '|' separates lines
            int index = whole.indexOf('|');
            String part = whole.substring(0,index);
            whole = whole.substring(index+1);

            // '@' is soonInterval
            JComponent c;
            index = part.indexOf('@');
            if (index == -1) {
               c = new JLabel(part);
            } else {
               JPanel p = new JPanel();
               p.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
               p.add(new JLabel(part.substring(0,index)));
               p.add(soonInterval);
               p.add(new JLabel(part.substring(index+1)));
               c = p;
            }

            helper2.add(0,y2++,c);
         }

         helper.add(0,y,label);
         helper.addCenter(1,y,useMessage[i]);
         helper.addCenter(2,y,useTaskBarFlash[i]);
         helper.addCenter(3,y,useStatusScreen[i]);
         helper.addCenter(4,y,useSound[i]);
         helper.add(5,y,soundType[i]);
         helper.add(6,y,soundFile[i]);

         JButton pickerButton = ConfigFormat.makePicker(soundFile[i],this,fileFilter);
         helper.add(7,y,pickerButton);
         soundType[i].addActionListener(new SoundTypeListener(i,pickerButton));

         y++;
      }

      // block for scope control
      {
         JPanel p = new JPanel();
         p.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
         p.add(new JLabel(Text.get(this,"s261") + ' '));
         p.add(renotifyInterval);

         helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d30")));
         y++;
         helper.addSpanCenter(0,y,8,p);
         y++;
      }

   // due time

      JPanel panelDue = new JPanel();
      panelDue.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s107")));

      JButton buttonEditSubset = new JButton(Text.get(this,"s118"));
      buttonEditSubset.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doEditSubset(); } });

      helper = new GridBagHelper(panelDue);

      helper.addSpan(0,0,4,new JLabel(Text.get(this,"s108")));
      helper.addSpan(0,1,4,new JLabel(Text.get(this,"s109")));
      helper.addSpan(0,2,4,new JLabel(Text.get(this,"s110")));
      helper.addSpan(0,3,4,skuDueAll);
      helper.add    (0,4,  skuDueSome);
      helper.add    (1,4,  new JLabel(Text.get(this,"s112") + ' '));
      helper.add    (2,4,  buttonEditSubset);
      helper.add    (3,4,  new JLabel(' ' + Text.get(this,"s119")));
      helper.addSpan(0,5,4,skuDueNone);

   // protected mode

      JPanel panelProtected = new JPanel();
      panelProtected.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s160")));

      helper = new GridBagHelper(panelProtected);

      helper.add(0,0,minimizeAtClose);
      helper.add(0,2,passwordForExit);
      helper.add(0,3,passwordForSetup);
      helper.add(0,4,passwordForUnminimize);
      helper.add(0,5,preventStopStart);

      helper.addSpan(1,0,3,new JLabel(Text.get(this,"s161")));
      helper.addSpan(1,1,3,new JLabel(Text.get(this,"s162")));
      helper.addSpan(1,2,3,new JLabel(Text.get(this,"s163")));
      helper.addSpan(1,3,3,new JLabel(Text.get(this,"s164")));
      helper.add(1,4,new JLabel(Text.get(this,"s249") + ' '));
      helper.add(2,4,unminimizePassword);
      helper.add(3,4,new JLabel(' ' + Text.get(this,"s250")));
      helper.addSpan(1,5,3,new JLabel(Text.get(this,"s168")));

   // auto-update

      JPanel panelAutoUpdate = new JPanel();
      panelAutoUpdate.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s171")));

      helper = new GridBagHelper(panelAutoUpdate);

      helper.addSpan(0,0,2,new JLabel(Text.get(this,"s172")));
      helper.addSpan(0,1,2,new JLabel(Text.get(this,"s173")));
      helper.add(0,2,Box.createVerticalStrut(Text.getInt(this,"d14")));

      helper.add(0,3,enableRestart);
      {
         JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
         line.add(new JLabel(Text.get(this,"s174") + ' '));
         line.add(restartHour1);
         line.add(new JLabel(' ' + Text.get(this,"s175") + ' '));
         line.add(restartHour2);
         line.add(new JLabel(' ' + Text.get(this,"s176")));
         helper.add(1,3,line);
      }

      helper.add(1,4,new JLabel(Text.get(this,"s177")));

      {
         JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
         line.add(new JLabel(Text.get(this,"s178") + ' '));
         line.add(restartWaitInterval);
         line.add(new JLabel(' ' + Text.get(this,"s179")));
         helper.add(1,5,line);
      }

   // kiosk

      JPanel panelKiosk = null;
      if (showKiosk) {

         panelKiosk = new JPanel();
         panelKiosk.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s266")));

         helper = new GridBagHelper(panelKiosk);

         y = 0;

         helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d31")));
         y++;

         helper.addSpanCenter(0,y,7,localShareEnabled);
         y++;

         helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d32")));
         y++;

         helper.add(1,y,new JLabel(Text.get(this,"s268") + ' '));
         helper.addSpan(2,y,3,localShareDir);
         helper.add(5,y,Box.createHorizontalStrut(Text.getInt(this,"d33")));
         helper.add(6,y,ConfigFormat.makePicker(localShareDir,this,null));
         y++;

         helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d34")));
         y++;

         helper.addSpan(1,y,2,new JLabel(Text.get(this,"s269") + ' '));
         helper.addSpan(3,y,4,localImagePollInterval);
         y++;

         helper.add(0,y,rollReceivedEnabled);
         helper.addSpan(1,y,2,new JLabel(Text.get(this,"s270") + ' '));
         helper.add(3,y,rollReceivedPurgeInterval);
         helper.addSpan(4,y,3,new JLabel(' ' + Text.get(this,"s271")));
         y++;
      }

   // printers

      JPanel panelPrinters = new JPanel();
      panelPrinters.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s279")));

      JButton buttonResolve = new JButton(Text.get(this,"s280"));
      buttonResolve.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doResolve(); } });

      helper = new GridBagHelper(panelPrinters);

      helper.addCenter(0,0,buttonResolve);

   // log files

      JPanel panelLog = new JPanel();
      panelLog.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s282")));

      helper = new GridBagHelper(panelLog);

      helper.add(0,0,new JLabel(Text.get(this,"s283") + ' '));
      helper.add(1,0,logLevel);

   // threads

      JPanel panelThreads = new JPanel();
      panelThreads.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s286")));

      JButton buttonThreadAssign = new JButton(Text.get(this,"s287"));
      buttonThreadAssign.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doThreadAssign(); } });

      JButton buttonThreadCreate = new JButton(Text.get(this,"s291"));
      buttonThreadCreate.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doThreadCreate(); } });

      JButton buttonThreadRename = new JButton(Text.get(this,"s292"));
      buttonThreadRename.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doThreadRename(); } });

      JButton buttonThreadDelete = new JButton(Text.get(this,"s293"));
      buttonThreadDelete.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doThreadDelete(); } });

      helper = new GridBagHelper(panelThreads);
      y = 0;

      helper.addSpan(1,y,5,buttonThreadAssign);
      y++;

      helper.add(0,y,Box.createVerticalStrut(d5));
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s290") + ' '));
      helper.addSpan(1,y,5,threadList);
      y++;

      helper.add(0,y,Box.createVerticalStrut(d5));
      y++;

      helper.add(1,y,buttonThreadCreate);
      helper.add(2,y,Box.createHorizontalStrut(d1));
      helper.add(3,y,buttonThreadRename);
      helper.add(4,y,Box.createHorizontalStrut(d1));
      helper.add(5,y,buttonThreadDelete);
      y++;

   // user tab

      JPanel tabUser = new JPanel();
      helper = new GridBagHelper(tabUser);

      helper.addFill(0,0,panelMerchant);
      helper.addFill(0,1,panelProtected);
      helper.addFill(0,2,panelEnable);
      helper.addFill(0,3,panelPurge);
      helper.addFill(0,4,panelOther);
      helper.add    (0,5,new JLabel());

      helper.setRowWeight(5,1);
      helper.setColumnWeight(0,1);

   // queue tab

      JPanel tabQueue = new JPanel();
      helper = new GridBagHelper(tabQueue);

      helper.addFill(0,0,panelQueue1);
      helper.addFill(0,1,panelQueue23);
      helper.add    (0,2,new JLabel());

      helper.setRowWeight(2,1);
      helper.setColumnWeight(0,1);

      swapTab = tabQueue;

   // invoice tab

      JPanel tabInvoice = new JPanel();
      helper = new GridBagHelper(tabInvoice);

      helper.addFill(0,0,panelSetupInvoice);
      helper.addFill(0,1,panelLogo);
      helper.addFill(0,2,panelJpeg);
      helper.add    (0,3,new JLabel());

      helper.setRowWeight(3,1);
      helper.setColumnWeight(0,1);

   // label tab

      JPanel tabLabel = new JPanel();
      helper = new GridBagHelper(tabLabel);

      helper.addFill(0,0,panelSetupLabel);
      helper.addFill(0,1,panelLabelShipping);
      helper.addFill(0,2,panelLabelAccount);
      helper.add    (0,3,new JLabel());

      helper.setRowWeight(3,1);
      helper.setColumnWeight(0,1);

   // notification tab

      JPanel tabNotification = new JPanel();
      helper = new GridBagHelper(tabNotification);

      helper.addFill(0,0,panelUser);
      helper.addFill(0,1,panelDue);
      helper.add    (0,2,new JLabel());

      helper.setRowWeight(2,1);
      helper.setColumnWeight(0,1);

   // scan tab

      tabScan = new JPanel();
      helper = new GridBagHelper(tabScan);

      helper.addFill(0,0,panelPoll);
      helper.addFill(0,1,panelPakon);
      helper.addFill(0,2,panelDLS);
      helper.add    (0,3,new JLabel());

      helper.setRowWeight(3,1);
      helper.setColumnWeight(0,1);

      tabScanCover = new JPanel();
      helper = new GridBagHelper(tabScanCover);

      JButton buttonChange = new JButton(Text.get(this,"s255"));
      buttonChange.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { changeTab(tabScanCover,tabScan); } });

      helper.add(0,0,new JLabel());
      helper.add(0,1,new JLabel(Text.get(this,"s251")));
      helper.add(0,2,new JLabel(Text.get(this,"s252")));
      helper.add(0,3,new JLabel(Text.get(this,"s253")));
      helper.add(0,4,new JLabel(Text.get(this,"s254")));
      helper.add(0,5,Box.createVerticalStrut(Text.getInt(this,"d29")));
      helper.addCenter(0,6,buttonChange);
      helper.add(0,7,new JLabel());

      helper.setRowWeight(0,1);
      helper.setRowWeight(7,3);

   // system tab

      JPanel tabSystem = new JPanel();
      helper = new GridBagHelper(tabSystem);

      helper.addFill(0,0,panelDownload);
      helper.addFill(0,1,configUpload);
      helper.addFill(0,2,panelWorkflow);
      helper.add    (0,3,new JLabel());

      helper.setRowWeight(3,1);
      helper.setColumnWeight(0,1);

   // network tab

      JPanel tabNetwork = new JPanel();
      helper = new GridBagHelper(tabNetwork);

      helper.addFill(0,0,configNetwork);
      helper.addFill(0,1,panelBandwidth);
      helper.addFill(0,2,panelAutoUpdate);
      helper.add    (0,3,new JLabel());

      helper.setRowWeight(3,1);
      helper.setColumnWeight(0,1);

   // other tab

      JPanel tabOther = new JPanel();
      helper = new GridBagHelper(tabOther);

      if (showKiosk)
      helper.addFill(0,0,panelKiosk);
      helper.addFill(0,1,panelPrinters);
      helper.addFill(0,2,panelLog);
      helper.addFill(0,3,panelThreads);
      helper.addFill(0,4,panelWholesale);
      helper.add    (0,5,new JLabel());

      helper.setRowWeight(5,1);
      helper.setColumnWeight(0,1);

   // overall

      tabbedPane = new JTabbedPane();

      tabbedPane.addTab(Text.get(this,"s28"),tabUser);
      tabbedPane.addTab(Text.get(this,"s47"),tabQueue);
      tabbedPane.addTab(Text.get(this,"s95"),tabInvoice);
      tabbedPane.addTab(Text.get(this,"s149"),tabLabel);
      tabbedPane.addTab(Text.get(this,"s91"),tabNotification);
      tabbedPane.addTab(Text.get(this,"s120"),tabScan); // becomes tabScanCover later
      tabbedPane.addTab(Text.get(this,"s30"),tabSystem);
      tabbedPane.addTab(Text.get(this,"s90"),tabNetwork);
      tabbedPane.addTab(Text.get(this,"s265"),tabOther);

      if (lastTab != null) tabbedPane.setSelectedIndex(lastTab.intValue());

      return tabbedPane;
   }

   protected void put() {

      Field.put(showQueueTab,config.showQueueTab);
      Field.put(showGrandTotal,config.showGrandTotal);
      Field.put(holdInvoice,config.holdInvoice);
      Field.put(autoComplete,config.autoComplete);
      Field.put(warnNotPrinted,config.warnNotPrinted);
      Field.put(enableTracking,config.carrierList.enableTracking);
      Field.put(localEnabled,config.localEnabled);
      Field.putNullable(localDirectory,Convert.fromNullableFile(config.localConfig.directory));
      Field.put(localDisableInvoice,Nullable.nbToB(config.printConfig.setupInvoice.disableForLocal));
      Field.put(localDisableLabel,  Nullable.nbToB(config.printConfig.setupLabel  .disableForLocal));
      Field.put(localDisableJpegInv,Nullable.nbToB(config.printConfig.         jpegDisableForLocal));
      logLevel.setSelectedItem(config.logLevel);

      for (int i=0; i<User.CODE_LIMIT; i++) {
         User.CodeRecord r = config.userConfig.record[i];
         Field.put(useMessage[i],r.useMessage);
         Field.put(useTaskBarFlash[i],r.useTaskBarFlash);
         Field.put(useStatusScreen[i],r.useStatusScreen);
         Field.put(useSound[i],r.useSound);
         Field.put(soundType[i],User.hasVoice(i) ? soundTypeValues : noVoiceValues,r.soundType);
         Field.putNullable(soundFile[i],Convert.fromNullableFile(r.soundFile));
      }
      soonInterval.put(config.userConfig.soonInterval);
      renotifyInterval.put(config.userConfig.renotifyInterval);
      if      (config.userConfig.skuDue == User.SKU_DUE_NONE) skuDueNone.setSelected(true);
      else if (config.userConfig.skuDue == User.SKU_DUE_SOME) skuDueSome.setSelected(true);
      else                                                    skuDueAll .setSelected(true);

      Field.put(minimizeAtStart,config.minimizeAtStart);
      Field.put(minimizeAtClose,config.minimizeAtClose);
      Field.put(passwordForExit,config.passwordForExit);
      Field.put(passwordForSetup,config.passwordForSetup);
      Field.put(passwordForUnminimize,config.passwordForUnminimize);
      Field.putNullable(unminimizePassword,config.unminimizePassword);
      Field.put(preventStopStart,config.preventStopStart);

      Field.put(merchant,Convert.fromInt(config.merchantConfig.merchant));
      Field.put(password,config.merchantConfig.password);
      Field.put(isWholesale,config.merchantConfig.isWholesale);
      Field.putNullable(frameTitleSuffix,config.frameTitleSuffix);

      // selectedQueue is just a UI tool, initialized below
      // queueFormat is updated after we've set up the map panels

      Iterator i = config.queueList.queues.iterator();
      while (i.hasNext()) {
         Queue q = (Queue) i.next();

         ConfigFormat panel = putPanel(q.queueID,q.format,q.formatConfig);
         panel.put();
      }

      if (lastQueue != null && selectedQueue.putTry(lastQueue)) ; // queueChanged already fired
      else queueChanged(); // fire queueChange manually, just show the first queue in the combo
      //
      // putTry might not work ... the user could create a queue, cancel out,
      // then re-open the dialog.  but, if it doesn't, we'll just show the
      // first queue.  since the queue IDs are assigned sequentially, there's
      // no way we'll ever come up showing the wrong queue.

      Field.put(autoSpawn,config.autoSpawn);
      Field.put(autoSpawnSpecial,config.autoSpawnSpecial);
      Field.putNullable(autoSpawnPrice,config.autoSpawnPrice);

      Field.put(downloadEnabled,config.downloadEnabled);
      Field.put(uploadEnabled,config.uploadEnabled);
      Field.put(pakonEnabled,config.pakonEnabled);

      listPollInterval.put(config.downloadConfig.listPollInterval);
      Field.put(prioritizeEnabled,config.downloadConfig.prioritizeEnabled);
      Field.put(conversionEnabled,config.downloadConfig.conversionEnabled);

      configUpload.put(config);

      Field.put(autoPurgeOrders,config.purgeConfig.autoPurgeOrders);
      Field.put(autoPurgeJobs,config.purgeConfig.autoPurgeJobs);
      Field.put(autoPurgePakon,config.purgeConfig.autoPurgePakon);
      Field.put(autoPurgeManual,config.purgeConfig.autoPurgeManual);
      Field.put(autoPurgeHot,config.purgeConfig.autoPurgeHot);
      Field.put(autoPurgeDLS,config.purgeConfig.autoPurgeDLS);
      Field.put(autoPurgeStale,config.purgeConfig.autoPurgeStale);
      Field.put(autoPurgeStaleLocal,config.purgeConfig.autoPurgeStaleLocal);
      rollPurgeInterval.put(config.purgeConfig.rollPurgeInterval);
      orderPurgeInterval.put(config.purgeConfig.orderPurgeInterval);
      jobPurgeInterval.put(config.purgeConfig.jobPurgeInterval);
      orderStalePurgeInterval.put(config.purgeConfig.orderStalePurgeInterval);
      jobStalePurgeInterval.put(config.purgeConfig.jobStalePurgeInterval);

      Field.put(dirHotFuji,getTarget(config.pollConfig.targets,Roll.SOURCE_HOT_FUJI));
      Field.put(dirHotXML,getTarget(config.pollConfig.targets,Roll.SOURCE_HOT_XML));
      Field.put(dirHotOrder,getTarget(config.pollConfig.targets,Roll.SOURCE_HOT_ORDER));
      Field.put(dirHotEmail,getTarget(config.pollConfig.targets,Roll.SOURCE_HOT_EMAIL));
      Field.put(dirHotImage,getTarget(config.pollConfig.targets,Roll.SOURCE_HOT_IMAGE));
      Field.put(throttleCount,Convert.fromInt(config.pollConfig.throttleCount));
      Field.putNullable(defaultEmail,config.pollConfig.defaultEmail);
      imageDelayInterval.put(config.pollConfig.imageDelayInterval);

      Field.put(scanEnabled,config.scanEnabled);
      scannerPollInterval.put(config.scannerPollInterval);
      Field.put(dlsHost,config.scanConfigDLS.host);
      Field.put(dlsUserName,config.scanConfigDLS.userName);
      Field.put(dlsPassword,config.scanConfigDLS.password);
      dlsEffectiveDate.put(config.scanConfigDLS.effectiveDate);
      Field.put(dlsExcludeByID,config.scanConfigDLS.excludeByID);
      Field.put(dlsHoldConfirm,config.scanConfigDLS.holdConfirm);

      Field.put(copies,Convert.fromInt(config.printConfig.copies));

      int color = config.printConfig.showColor ? INVOICE_COLOR_COLOR : INVOICE_COLOR_BANDW;
      Field.put(invoiceColor,invoiceColorValues,color);

      int type = config.printConfig.showItems ? INVOICE_TYPE_COMPLETE : INVOICE_TYPE_SUMMARY;
      Field.put(invoiceType,invoiceTypeValues,type);

      Field.put(productBarcodeEnable,config.printConfig.productBarcodeEnable);
      Field.put(productBarcodeType,productBarcodeTypeValues,config.printConfig.productBarcodeType);

      TimeZoneUtil.Split sp = TimeZoneUtil.split(config.printConfig.timeZone);
      timeZone1.setSelectedItem(sp.first);
      // that fires adjustTimeZone2, which sets the correct items
      timeZone2.setSelectedItem(sp.second);

      putDateFormatCombo(dateFormatInvoice,config.printConfig.dateFormatInvoice);
      putDateFormatCombo(dateFormatLabel,config.printConfig.dateFormatLabel);

      type = config.printConfig.accountSummary ? ACCOUNT_TYPE_SUMMARY : ACCOUNT_TYPE_STANDARD;
      Field.put(accountType,accountTypeValues,type);

      Field.put(labelPerQueue,config.printConfig.labelPerQueue);
      Field.put(showOrderNumber,config.printConfig.showOrderNumber);
      Field.put(overrideAddress,config.printConfig.overrideAddress);
      Field.putNullable(overrideName,config.printConfig.overrideName);
      Field.putNullable(overrideStreet1,config.printConfig.overrideStreet1);
      Field.putNullable(overrideStreet2,config.printConfig.overrideStreet2);
      Field.putNullable(overrideCity,config.printConfig.overrideCity);
      Field.putNullable(overrideState,config.printConfig.overrideState);
      Field.putNullable(overrideZIP,config.printConfig.overrideZIP);
      Field.put(markShip,config.printConfig.markShip);
      if (config.printConfig.markShipUseMessage) markShipUseMessage.setSelected(true);
      else                                       markShipUseShipMth.setSelected(true);
      Field.put(markShipMessage,config.printConfig.markShipMessage);
      Field.put(markPay,config.printConfig.markPay);

      int includeTax = config.printConfig.markPayIncludeTax ? INCLUDE_TAX_YES : INCLUDE_TAX_NO;
      Field.put(markPayIncludeTax,includeTaxValues,includeTax);

      Field.putNullable(markPayAddMessage,config.printConfig.markPayAddMessage);
      Field.put(markPro,config.printConfig.markPro);
      Field.putNullable(markProNotMerchantID,Convert.fromNullableInt(config.printConfig.markProNotMerchantID));
      Field.put(markPre,config.printConfig.markPre);
      Field.put(markPreMessage,config.printConfig.markPreMessage);
      Field.put(showPickupLocation,config.printConfig.showPickupLocation);

      setupInvoice.put();
      setupLabel  .put();

      Field.put(productEnableForHome,config.printConfig.productEnableForHome);
      Field.put(productEnableForAccount,config.printConfig.productEnableForAccount);

      Field.putNullable(logoFile,Convert.fromNullableFile(config.printConfig.logoFile));
      Field.putNullable(logoWidth,Convert.fromNullableInt(config.printConfig.logoWidth));
      Field.putNullable(logoHeight,Convert.fromNullableInt(config.printConfig.logoHeight));

      Field.put(jpegEnableForHome,   config.printConfig.jpegEnableForHome   );
      Field.put(jpegEnableForAccount,config.printConfig.jpegEnableForAccount);

      color = config.printConfig.jpegShowColor ? INVOICE_COLOR_COLOR : INVOICE_COLOR_BANDW;
      Field.put(jpegShowColor,invoiceColorValues,color);

      reloadJpegSKUs();
      setSelectedJpegSKU(config.printConfig.jpegSKU);

      Field.putNullable(jpegWidth, Convert.fromNullableDouble(config.printConfig.jpegWidth ));
      Field.putNullable(jpegHeight,Convert.fromNullableDouble(config.printConfig.jpegHeight));
      Field.put(jpegOrientation,ConfigSetup.orientationValues,config.printConfig.jpegOrientation);
      Field.put(jpegBorderPixels,Convert.fromInt(config.printConfig.jpegBorderPixels));

      configNetwork.put(config);

      Field.put(enableRestart,config.autoUpdateConfig.enableRestart);
      Field.put(restartHour1,hourValues,config.autoUpdateConfig.restartHour1);
      Field.put(restartHour2,hourValues,config.autoUpdateConfig.restartHour2);
      restartWaitInterval.put(config.autoUpdateConfig.restartWaitInterval);

      if (showKiosk) {
         Field.put(localShareEnabled,Nullable.nbToB(config.localShareEnabled));
         Field.putNullable(localShareDir,Convert.fromNullableFile(config.localShareDir));
         localImagePollInterval.put(config.localImageConfig.pollInterval);
         boolean rre = (config.purgeConfig.rollReceivedPurgeInterval != null);
         Field.put(rollReceivedEnabled,rre);
         rollReceivedPurgeInterval.put(rre ? config.purgeConfig.rollReceivedPurgeInterval.longValue() : 86400000); // 1 day
      }
   }

   private void getMerchantConfig() throws ValidationException {
      config.merchantConfig.merchant = Convert.toInt(Field.get(merchant));
      config.merchantConfig.password = Field.get(password);
      config.merchantConfig.isWholesale = Field.get(isWholesale);
   }

   private void getJpegSubset() throws ValidationException {
      config.printConfig.jpegEnableForHome    = Field.get(jpegEnableForHome   );
      config.printConfig.jpegEnableForAccount = Field.get(jpegEnableForAccount);
      config.printConfig.jpegWidth = Convert.toNullableDouble(Field.getNullable(jpegWidth));
   }

   protected void getAndValidate() throws ValidationException {

      config.showQueueTab = Field.get(showQueueTab);
      config.showGrandTotal = Field.get(showGrandTotal);
      config.holdInvoice = Field.get(holdInvoice);
      config.autoComplete = Field.get(autoComplete);
      config.warnNotPrinted = Field.get(warnNotPrinted);
      config.carrierList.enableTracking = Field.get(enableTracking);
      config.localEnabled = Field.get(localEnabled);
      config.localConfig.directory = Convert.toNullableFile(Field.getNullable(localDirectory));
      config.printConfig.setupInvoice.disableForLocal = Nullable.bToNb(Field.get(localDisableInvoice));
      config.printConfig.setupLabel  .disableForLocal = Nullable.bToNb(Field.get(localDisableLabel  ));
      config.printConfig.         jpegDisableForLocal = Nullable.bToNb(Field.get(localDisableJpegInv));
      config.logLevel = (Level) logLevel.getSelectedItem();

      for (int i=0; i<User.CODE_LIMIT; i++) {
         User.CodeRecord r = config.userConfig.record[i];
         r.useMessage = Field.get(useMessage[i]);
         r.useTaskBarFlash = Field.get(useTaskBarFlash[i]);
         r.useStatusScreen = Field.get(useStatusScreen[i]);
         r.useSound = Field.get(useSound[i]);
         r.soundType = Field.get(soundType[i],User.hasVoice(i) ? soundTypeValues : noVoiceValues);
         r.soundFile = Convert.toNullableFile(Field.getNullable(soundFile[i]));
      }
      config.userConfig.soonInterval = soonInterval.get();
      config.userConfig.renotifyInterval = (int) renotifyInterval.get();
      if      (skuDueNone.isSelected()) config.userConfig.skuDue = User.SKU_DUE_NONE;
      else if (skuDueSome.isSelected()) config.userConfig.skuDue = User.SKU_DUE_SOME;
      else                              config.userConfig.skuDue = User.SKU_DUE_ALL;

      config.minimizeAtStart = Field.get(minimizeAtStart);
      config.minimizeAtClose = Field.get(minimizeAtClose);
      config.passwordForExit = Field.get(passwordForExit);
      config.passwordForSetup = Field.get(passwordForSetup);
      config.passwordForUnminimize = Field.get(passwordForUnminimize);
      config.unminimizePassword = Field.getNullable(unminimizePassword);
      config.preventStopStart = Field.get(preventStopStart);

      getMerchantConfig();
      config.frameTitleSuffix = Field.getNullable(frameTitleSuffix);

      // selectedQueue is just a UI tool, goes nowhere
      // queueFormat is stored back into the list after each change

      Iterator i = config.queueList.queues.iterator();
      while (i.hasNext()) {
         Queue q = (Queue) i.next();

         ConfigFormat panel = getPanel(q.queueID,q.format,/* create = */ false);
         panel.get();
         q.formatConfig = panel.getFormatConfig();
      }
      getCompletionMode();
      getOtherQueueInfo();

      config.autoSpawn = Field.get(autoSpawn);
      config.autoSpawnSpecial = Field.get(autoSpawnSpecial);
      config.autoSpawnPrice = Field.getNullable(autoSpawnPrice);

      config.downloadEnabled = Field.get(downloadEnabled);
      config.uploadEnabled = Field.get(uploadEnabled);
      config.pakonEnabled = Field.get(pakonEnabled);

      config.downloadConfig.listPollInterval = listPollInterval.get();
      config.downloadConfig.prioritizeEnabled = Field.get(prioritizeEnabled);
      config.downloadConfig.conversionEnabled = Field.get(conversionEnabled);

      configUpload.get(config);

      config.purgeConfig.autoPurgeOrders = Field.get(autoPurgeOrders);
      config.purgeConfig.autoPurgeJobs = Field.get(autoPurgeJobs);
      config.purgeConfig.autoPurgePakon = Field.get(autoPurgePakon);
      config.purgeConfig.autoPurgeManual = Field.get(autoPurgeManual);
      config.purgeConfig.autoPurgeHot = Field.get(autoPurgeHot);
      config.purgeConfig.autoPurgeDLS = Field.get(autoPurgeDLS);
      config.purgeConfig.autoPurgeStale = Field.get(autoPurgeStale);
      config.purgeConfig.autoPurgeStaleLocal = Field.get(autoPurgeStaleLocal);
      config.purgeConfig.rollPurgeInterval = rollPurgeInterval.get();
      config.purgeConfig.orderPurgeInterval = orderPurgeInterval.get();
      config.purgeConfig.jobPurgeInterval = jobPurgeInterval.get();
      config.purgeConfig.orderStalePurgeInterval = orderStalePurgeInterval.get();
      config.purgeConfig.jobStalePurgeInterval = jobStalePurgeInterval.get();

      putTarget(config.pollConfig.targets,Roll.SOURCE_HOT_FUJI,Field.get(dirHotFuji));
      putTarget(config.pollConfig.targets,Roll.SOURCE_HOT_XML,Field.get(dirHotXML));
      putTarget(config.pollConfig.targets,Roll.SOURCE_HOT_ORDER,Field.get(dirHotOrder));
      putTarget(config.pollConfig.targets,Roll.SOURCE_HOT_EMAIL,Field.get(dirHotEmail));
      putTarget(config.pollConfig.targets,Roll.SOURCE_HOT_IMAGE,Field.get(dirHotImage));
      config.pollConfig.throttleCount = Convert.toInt(Field.get(throttleCount));
      config.pollConfig.defaultEmail = Field.getNullable(defaultEmail);
      config.pollConfig.imageDelayInterval = imageDelayInterval.get();

      config.scanEnabled = Field.get(scanEnabled);
      config.scannerPollInterval = scannerPollInterval.get();
      config.scanConfigDLS.host = Field.get(dlsHost);
      config.scanConfigDLS.userName = Field.get(dlsUserName);
      config.scanConfigDLS.password = Field.get(dlsPassword);
      config.scanConfigDLS.effectiveDate = dlsEffectiveDate.get();
      config.scanConfigDLS.excludeByID = Field.get(dlsExcludeByID);
      config.scanConfigDLS.holdConfirm = Field.get(dlsHoldConfirm);

      config.printConfig.copies = Convert.toInt(Field.get(copies));

      int color = Field.get(invoiceColor,invoiceColorValues);
      config.printConfig.showColor = (color == INVOICE_COLOR_COLOR);

      int type = Field.get(invoiceType,invoiceTypeValues);
      config.printConfig.showItems = (type == INVOICE_TYPE_COMPLETE);

      config.printConfig.productBarcodeEnable = Field.get(productBarcodeEnable);
      config.printConfig.productBarcodeType = Field.get(productBarcodeType,productBarcodeTypeValues);

      config.printConfig.timeZone = TimeZoneUtil.join( (String) timeZone1.getSelectedItem(),
                                                       (String) timeZone2.getSelectedItem() );

      config.printConfig.dateFormatInvoice = getDateFormatCombo(dateFormatInvoice);
      config.printConfig.dateFormatLabel = getDateFormatCombo(dateFormatLabel);

      type = Field.get(accountType,accountTypeValues);
      config.printConfig.accountSummary = (type == ACCOUNT_TYPE_SUMMARY);

      config.printConfig.labelPerQueue = Field.get(labelPerQueue);
      config.printConfig.showOrderNumber = Field.get(showOrderNumber);
      config.printConfig.overrideAddress = Field.get(overrideAddress);
      config.printConfig.overrideName = Field.getNullable(overrideName);
      config.printConfig.overrideStreet1 = Field.getNullable(overrideStreet1);
      config.printConfig.overrideStreet2 = Field.getNullable(overrideStreet2);
      config.printConfig.overrideCity = Field.getNullable(overrideCity);
      config.printConfig.overrideState = Field.getNullable(overrideState);
      config.printConfig.overrideZIP = Field.getNullable(overrideZIP);
      config.printConfig.markShip = Field.get(markShip);
      config.printConfig.markShipUseMessage = markShipUseMessage.isSelected();
      config.printConfig.markShipMessage = Field.get(markShipMessage);
      config.printConfig.markPay = Field.get(markPay);

      int includeTax = Field.get(markPayIncludeTax,includeTaxValues);
      config.printConfig.markPayIncludeTax = (includeTax == INCLUDE_TAX_YES);

      config.printConfig.markPayAddMessage = Field.getNullable(markPayAddMessage);
      config.printConfig.markPro = Field.get(markPro);
      config.printConfig.markProNotMerchantID = Convert.toNullableInt(Field.getNullable(markProNotMerchantID));
      config.printConfig.markPre = Field.get(markPre);
      config.printConfig.markPreMessage = Field.get(markPreMessage);
      config.printConfig.showPickupLocation = Field.get(showPickupLocation);

      setupInvoice.get();
      setupLabel  .get();

      config.printConfig.productEnableForHome = Field.get(productEnableForHome);
      config.printConfig.productEnableForAccount = Field.get(productEnableForAccount);

      config.printConfig.logoFile = Convert.toNullableFile(Field.getNullable(logoFile));
      config.printConfig.logoWidth = Convert.toNullableInt(Field.getNullable(logoWidth));
      config.printConfig.logoHeight = Convert.toNullableInt(Field.getNullable(logoHeight));

      getJpegSubset();

      color = Field.get(jpegShowColor,invoiceColorValues);
      config.printConfig.jpegShowColor = (color == INVOICE_COLOR_COLOR);

      config.printConfig.jpegSKU = getSelectedJpegSKU();

      config.printConfig.jpegHeight = Convert.toNullableDouble(Field.getNullable(jpegHeight));
      config.printConfig.jpegOrientation = Field.get(jpegOrientation,ConfigSetup.orientationValues);
      config.printConfig.jpegBorderPixels = Convert.toInt(Field.get(jpegBorderPixels));

      configNetwork.get(config);

      config.autoUpdateConfig.enableRestart = Field.get(enableRestart);
      config.autoUpdateConfig.restartHour1 = Field.get(restartHour1,hourValues);
      config.autoUpdateConfig.restartHour2 = Field.get(restartHour2,hourValues);
      config.autoUpdateConfig.restartWaitInterval = restartWaitInterval.get();

      if (showKiosk) {

         // I want to make the kiosk settings visible to all users now,
         // which means the distinction between null and false is gone,
         // but I still want to try and preserve the different values
         // in the data in case we need to undo the change for some reason.
         // so, basically I want null to stay null as long as possible.
         // this is flaky because the get function can be called more than
         // once, but it still does basically the right thing.

         if (Field.get(localShareEnabled)) {
            config.localShareEnabled = Boolean.TRUE;
         } else {
            config.localShareEnabled = (config.localShareEnabled != null) ? Boolean.FALSE : null;
         }

         config.localShareDir = Convert.toNullableFile(Field.getNullable(localShareDir));
         config.localImageConfig.pollInterval = localImagePollInterval.get();
         boolean rre = Field.get(rollReceivedEnabled);
         config.purgeConfig.rollReceivedPurgeInterval = rre ? new Long(rollReceivedPurgeInterval.get()) : null;
      }

      config.validate();
   }

   private void prepareSaveFormats() {

      // save the original queue formats, because getAndValidate
      // comes before weakValidate, and anyway the config object
      // can get written on several times if there are errors.

      saveFormats = new HashMap();

      Iterator i = config.queueList.queues.iterator();
      while (i.hasNext()) {
         Queue queue = (Queue) i.next();
         saveFormats.put(queue.queueID,new Integer(queue.format));
      }
   }

   protected boolean weakValidate() {

      Iterator i = saveFormats.entrySet().iterator();
      while (i.hasNext()) {
         Map.Entry entry = (Map.Entry) i.next();

         String queueID = (String) entry.getKey();
         int oldFormat = ((Integer) entry.getValue()).intValue();

         Queue queue = config.queueList.findQueueByID(queueID);
         if (queue == null) continue; // get null if deleted

         if (    Queue.getFormatClass(queue.format)
              == Queue.getFormatClass(   oldFormat) ) continue; // no change

         // compare by class rather than by format code,
         // because it's the class that is preserved.
         // i.e., if you change from flat to tree, nothing is lost.
         // but, we need to keep the format code itself,
         // because that's how we look up the short name.

         String s = Text.get(this,"e3",new Object[] { queue.name, getShortName(oldFormat), getShortName(queue.format) });
         boolean confirmed = Pop.confirm(this,s,Text.get(this,"s96"));
         if ( ! confirmed ) return false;
      }

      // check for unused threads
      String message = config.queueList.getThreadValidation();
      if (message != null) {
         boolean confirmed = Pop.confirm(this,message,Text.get(this,"s289"));
         if ( ! confirmed ) return false;
      }

      // try to discourage the user from editing the DLS effective date.
      //
      // there are two cases where we <i>don't</i> warn about it.
      //
      //  * if the date is changing from null to non-null, the scanner
      //    must have been off before, and the user is turning it on.
      //    so, presumably they have some idea what they're doing there.
      //
      //  * if the date is changing from non-null to null, the scanner
      //    is being turned off, so there's nothing to worry about.
      //
      // otherwise, the date is changing between non-null values, and we
      // want to discourage it.  that's true even if the scanner is off ...
      // if the effective date was filled in, it means the scanner was in use,
      // and they might turn it on again, so they shouldn't be fiddling.

      if (      config.scanConfigDLS.effectiveDate != null
           &&   saveEffectiveDate != null
           && ! config.scanConfigDLS.effectiveDate.equals(saveEffectiveDate) ) {

         boolean confirmed = Pop.confirm(this,Text.get(this,"e4"),Text.get(this,"s132"));
         if ( ! confirmed ) return false;
      }

      // what about weak validation of barcode settings?
      // it's an interesting case ... we do weak validation
      // when the dialog closes, but the rules depend on
      // the value of the JPEG width, which is edited outside
      // the dialog.  if we validate again, then when the
      // user edits the barcode settings, we'll ask the exact
      // same question twice in a row.  but, if we don't,
      // we won't warn when a width change breaks the barcode.
      // for now I'll go with the lax approach.

      if (      config.carrierList.enableTracking
           && ! config.carrierList.hasCarrierSubset() ) { // maybe no carriers, maybe none shown

         boolean confirmed = Pop.confirm(this,Text.get(this,"e6"),Text.get(this,"s264"));
         if ( ! confirmed ) return false;
         // annoying to ask this on every save, but tracking numbers really shouldn't be on with no subset.
         // the only reason it's not a hard validation is, maybe one day we'll want the carriers to
         // auto-refresh with the dealers and store hours, and then theoretically if carriers were removed,
         // the subset could become empty and invalidate the config.
      }

      // give hint about barcodes on product labels
      boolean productLabels = (config.printConfig.productEnableForHome || config.printConfig.productEnableForAccount);
      if (      productLabels
           && ! saveProductLabels
           && ! config.printConfig.productBarcodeEnable ) {

         boolean confirmed = Pop.confirm(this,Text.get(this,"e7"),Text.get(this,"s276"));
         if ( ! confirmed ) return false;
      }

      return true;
   }

   private void adjustTimeZone2() {

      timeZone2.setModel(new DefaultComboBoxModel(TimeZoneUtil.getArraySecond((String) timeZone1.getSelectedItem())));
      //
      // this destroys the value in timeZone2, but that's what we want;
      // otherwise it would be way too easy to construct invalid zones,
      // especially in cases where the second array is arrayEmpty.
   }

// --- format helpers ---

   private static Format getFormatSafe(int format) {
      try {
         return Format.getFormat(format);
      } catch (IOException e) {
         return null; // can't happen
      }
   }

   private static String getShortName(int format) {
      return getFormatSafe(format).getShortName();
   }

   private void putCompletionMode(int format) {

      saveFormatObject = getFormatSafe(format);
      saveCompletionModeValues = saveFormatObject.getAllowedCompletionModes();

      completionMode.removeAllItems();
      for (int i=0; i<saveCompletionModeValues.length; i++) {
         int j = Field.lookup(completionModeValues,saveCompletionModeValues[i]);
         completionMode.addItem(completionModeNames[j]);
      }
      completionMode.setEnabled(saveCompletionModeValues.length > 1);

      int mode = saveFormatObject.getCompletionMode(panelFormat.getFormatConfig());
      Field.put(completionMode,saveCompletionModeValues,mode);
   }

   private void getCompletionMode() {
      if (saveFormatObject == null) return; // ignore initial fake panel

      int mode = Field.get(completionMode,saveCompletionModeValues);
      saveFormatObject.setCompletionMode(panelFormat.getFormatConfig(),mode);
   }

   // these are similar to the completionMode functions, but much simpler,
   // and also they're called from queueChanged, not queueFormatChanged.
   // the way it works is, queueChanged is called during setup and on queue change,
   // and it has a get-put pair.  one more get call from getAndValidate covers it.

   private void putOtherQueueInfo(Queue q) {
      saveQueue = q;
      Field.put(noAutoPrint,   Nullable.nbToB(saveQueue.noAutoPrint));
      Field.put(switchToBackup,Nullable.nbToB(saveQueue.switchToBackup));
   }

   private void getOtherQueueInfo() {
      if (saveQueue == null) return; // ignore first call
      saveQueue.noAutoPrint    = Nullable.bToNb(Field.get(noAutoPrint));
      saveQueue.switchToBackup = Nullable.bToNb(Field.get(switchToBackup));
   }

// --- sound file helper ---

   private class SoundTypeListener implements ActionListener {

      private int i;
      private JButton pickerButton;
      public SoundTypeListener(int i, JButton pickerButton) { this.i = i; this.pickerButton = pickerButton; }

      public void actionPerformed(ActionEvent e) {
         int type = Field.get(soundType[i],User.hasVoice(i) ? soundTypeValues : noVoiceValues);
         boolean enable = (type == User.SOUND_FILE);
         soundFile[i].setEnabled(enable);
         pickerButton.setEnabled(enable);
      }
   }

// --- PollConfig helpers ---

   // this approach limits you to one target for each source type.
   // if that ever matters, probably a grid is the way to go.

   private static PollConfig.Target findTarget(LinkedList targets, int source) {
      Iterator i = targets.iterator();
      while (i.hasNext()) {
         PollConfig.Target target = (PollConfig.Target) i.next();
         if (target.source == source) return target;
      }
      return null;
   }

   private static String getTarget(LinkedList targets, int source) {
      PollConfig.Target target = findTarget(targets,source);
      return (target == null) ? "" : Convert.fromFile(target.directory);
   }

   private static void putTarget(LinkedList targets, int source, String s) throws ValidationException {
      PollConfig.Target target = findTarget(targets,source);

      if (s.length() != 0) {

         if (target == null) {
            target = new PollConfig.Target();
            target.source = source;
            targets.add(target);
         }
         target.directory = Convert.toFile(s);

      } else {

         if (target != null) {
            targets.remove(target);
         }
      }
   }

// --- methods ---

   private void queueChanged() {
      Queue q = selectedQueue.getQueueObject(); // never null
      Field.put(queueFormat,fa.formatValues,q.format);
      // now queueFormatChanged will fire, even if the actual value didn't change

      getOtherQueueInfo(); // into previous queue object
      putOtherQueueInfo(q);
   }

   private void queueFormatChanged() {
      Queue q = selectedQueue.getQueueObject(); // never null
      q.format = Field.get(queueFormat,fa.formatValues);

      // that was pointless if we got here from queueChanged,
      // but otherwise we have to store the value right away;
      // that's how we remember q.format for different queues with a single combo.

      swapContainer.remove(panelFormat);

      getCompletionMode();
      panelFormat = getPanel(q.queueID,q.format,/* create = */ true);
      putCompletionMode(q.format);

      swapHelper.addFill(0,2,panelFormat);
      swapTab.revalidate(); // maybe could use this dialog here too
      swapTab.repaint(); // else the GUI doesn't update immediately
   }

   private void createQueue() {
      Queue q;

      while (true) {
         String name = Pop.inputString(this,Text.get(this,"s56"),Text.get(this,"s55"));
         if (name == null) return; // canceled

         try {
            q = config.queueList.createQueue(name.trim());
            break;
         } catch (ValidationException e) {
            Pop.error(this,e,Text.get(this,"s57"));
         }
      }

      selectedQueue.reinit(config.queueList);
      selectedQueue.put(q.queueID);
   }

   private void renameQueue() {

      Queue q = selectedQueue.getQueueObject(); // never null
      String nameOld = q.name;

      while (true) {
         String name = Pop.inputString(this,Text.get(this,"s72",new Object[] { nameOld }),Text.get(this,"s71"));
         if (name == null) return; // canceled

         try {
            config.queueList.renameQueue(q,name.trim());
            break;
         } catch (ValidationException e) {
            Pop.error(this,e,Text.get(this,"s73"));
         }
      }

      selectedQueue.reinit(config.queueList);
      // selectedQueue already showing correct ID
   }

   private void deleteQueue() {

      // queue list is validated to have entries, have to maintain that or we get weird behavior
      // (nothing to show in panel, exception if you try to rename / delete, who knows what else)
      if (config.queueList.queues.size() == 1) {
         Pop.error(this,Text.get(this,"e5"),Text.get(this,"s260"));
         return;
      }

      Queue q = selectedQueue.getQueueObject(); // never null
      String nameOld = q.name;

      boolean confirmed = Pop.confirm(this,Text.get(this,"s76",new Object[] { nameOld }),Text.get(this,"s75"));
      if ( ! confirmed ) return;

      try {
         config.queueList.deleteQueue(q);
      } catch (ValidationException e) {
         Pop.error(this,e,Text.get(this,"s77"));
         return;
      }

      selectedQueue.reinit(config.queueList);
      // this was showing the deleted queue, and will change to the first item
   }

// --- panel map ---

   private static class Key {

      private String queueID;
      private int format;

      public Key(String queueID, int format) {

      // handle sharing

         // formats that share panels must have the same class in Queue.getFormatClass,
         // but not vice versa ... it's possible to have multiple copies of one class.

         if (format == Order.FORMAT_TREE) format = Order.FORMAT_FLAT; // these use same panel

      // construct

         this.queueID = queueID;
         this.format = format;
      }

      public int hashCode() { return queueID.hashCode() + format; }
      // Integer.hashCode returns the wrapped integer

      public boolean equals(Object o) {
         if ( ! (o instanceof Key) ) return false;
         Key k = (Key) o;

         return (    queueID.equals(k.queueID)
                  && format == k.format        );
      }
   }

   private ConfigFormat newPanel(Object formatConfig) {
      if      (formatConfig instanceof ManualConfig ) return new ConfigManual (this,false,(ManualConfig ) formatConfig);
      else if (formatConfig instanceof NoritsuConfig) return new ConfigNoritsu(this,false,(NoritsuConfig) formatConfig);
      else if (formatConfig instanceof KonicaConfig ) return new ConfigKonica (this,false,(KonicaConfig ) formatConfig);
      else if (formatConfig instanceof FujiConfig   ) return new ConfigFuji   (this,false,(FujiConfig   ) formatConfig);
      else if (formatConfig instanceof FujiNewConfig) return new ConfigFujiNew(this,false,(FujiNewConfig) formatConfig);
      else if (formatConfig instanceof DLSConfig    ) return new ConfigDLS    (this,false,(DLSConfig    ) formatConfig);
      else if (formatConfig instanceof KodakConfig  ) return new ConfigKodak  (this,false,(KodakConfig  ) formatConfig);
      else if (formatConfig instanceof AgfaConfig   ) return new ConfigAgfa   (this,false,(AgfaConfig   ) formatConfig);
      else if (formatConfig instanceof LucidiomConfig) return new ConfigLucidiom(this,(LucidiomConfig) formatConfig);
      else if (formatConfig instanceof PixelConfig   ) return new ConfigPixel   (this,(PixelConfig)    formatConfig);
      else if (formatConfig instanceof DP2Config     ) return new ConfigDP2     (this,(DP2Config)      formatConfig);
      else if (formatConfig instanceof BeaufortConfig) return new ConfigBeaufort(this,(BeaufortConfig) formatConfig);
      else if (formatConfig instanceof DKS3Config    ) return new ConfigDKS3    (this,(DKS3Config)     formatConfig);
      else if (formatConfig instanceof DirectPDFConfig) return new ConfigDirectPDF(this,(DirectPDFConfig) formatConfig);
      else if (formatConfig instanceof ZBEConfig     ) return new ConfigZBE     (this,(ZBEConfig)      formatConfig);
      else if (formatConfig instanceof Fuji3Config   ) return new ConfigFuji3   (this,(Fuji3Config)    formatConfig);
      else if (formatConfig instanceof HPConfig      ) return new ConfigHP      (this,(HPConfig)       formatConfig);
      else if (formatConfig instanceof XeroxConfig   ) return new ConfigXerox   (this,(XeroxConfig)    formatConfig);
      else if (formatConfig instanceof DirectJPEGConfig) return new ConfigDirectJPEG(this,(DirectJPEGConfig) formatConfig);
      else if (formatConfig instanceof BurnConfig    ) return new ConfigBurn    (this,(BurnConfig)     formatConfig);
      else if (formatConfig instanceof HotFolderConfig) return new ConfigHotFolder(this,(HotFolderConfig) formatConfig);
      else if (formatConfig instanceof DNPConfig     ) return new ConfigDNP     (this,(DNPConfig)      formatConfig);
      else if (formatConfig instanceof PurusConfig   ) return new ConfigPurus   (this,(PurusConfig)    formatConfig);
      else if (formatConfig instanceof RawJPEGConfig ) return new ConfigRawJPEG (this,(RawJPEGConfig)  formatConfig);
      else throw new Error(Text.get(this,"e2",new Object[] { formatConfig.getClass().getName() }));
   }

   private ConfigFormat putPanel(String queueID, int format, Object formatConfig) {

      Key key = new Key(queueID,format);
      ConfigFormat panel = newPanel(formatConfig);

      mapPanel.put(key,panel);
      return panel;

      // only call this once per queue, otherwise entries may overwrite
   }

   private ConfigFormat getPanel(String queueID, int format, boolean create) {

      Key key = new Key(queueID,format);
      ConfigFormat panel = (ConfigFormat) mapPanel.get(key);

      if (panel != null) return panel;

      if ( ! create ) return null;
      // this never happens, must be a relic from a time when I didn't create
      // all the panels up front.  it's called three places with create false,
      // and in all of them we'd get a NPE if null was ever actually returned.

      panel = newPanel(((Structure) Queue.getFormatObject(format)).loadDefault());
      panel.put(); // else defaults never get loaded

      mapPanel.put(key,panel);
      return panel;
   }

// --- SKU edit screen ---

   private EditSKUData copyIn() {

      EditSKUData data = new EditSKUData();

      data.productConfig = config.productConfig.copy();

      data.mappings = CopyUtil.copyList(config.queueList.mappings);
      data.defaultQueue = config.queueList.defaultQueue;

      // read current queue information from panels, not from queue list

      data.mappingQueues = new LinkedList();

      Iterator i = config.queueList.queues.iterator();
      while (i.hasNext()) {
         Queue q = (Queue) i.next();
         ConfigFormat panel = getPanel(q.queueID,q.format,/* create = */ false);

         Object o = panel.getFormatConfig();
         if (o instanceof MappingConfig) {

            EditSKUData.MappingQueue mq = new EditSKUData.MappingQueue();

            mq.format = q.format;
            mq.queueID = q.queueID;
            mq.name = q.name;
            mq.mappingConfig = (MappingConfig) o;
            // adapter is for dialog to use

            mq.mappings = mq.mappingConfig.getMappings(); // n.b., not copied

            data.mappingQueues.add(mq);
         }
      }

      data.jpegSKU = getSelectedJpegSKU();
      data.dueSkus = new LinkedList(config.userConfig.skus);

      return data;
   }

   private void copyOut(EditSKUData data) {

      config.productConfig = data.productConfig;

      config.queueList.mappings = data.mappings;
      config.queueList.defaultQueue = data.defaultQueue;

      Iterator i = data.mappingQueues.iterator();
      while (i.hasNext()) {
         EditSKUData.MappingQueue mq = (EditSKUData.MappingQueue) i.next();

         mq.mappingConfig.putMappings(mq.mappings);
      }

      // we needed to do this even before we did SKU transfers on it,
      // because the other entries in the combo box could change too.
      // so, it's important that this happen after productConfig is set!
      reloadJpegSKUs();
      setSelectedJpegSKU(data.jpegSKU);
      //
      config.userConfig.skus = data.dueSkus;
   }

   private void doEditSKU(SKU forceSKU) {

      try {
         try {
            getMerchantConfig();
         } catch (ValidationException e) {
            // ignore and proceed with whatever values were there before
         }
         // note, it's OK to write on the config object, it's just like
         // what happens when you hit OK and then find an invalid field.

         EditSKUData data = copyIn();
         if (new EditSKUDialog(this,data,config.queueList,forceSKU,config.skuRefreshURL,config.newProductURL,config.conversionURL,config.merchantConfig).run()) {
            copyOut(data);
         }
      } catch (Throwable t) {
         Pop.diagnostics(this,Text.get(this,"s205"),t);
      }
   }

// --- JPEG SKU combo ---

   private static Object nullObject = new Object() { public String toString() { return ""; } };

   private SKU getSelectedJpegSKU() {

      Object o = jpegSKU.getSelectedItem();
      return (o == nullObject) ? null : (SKU) o;
   }

   private void setSelectedJpegSKU(SKU sku) {

      Object o = (sku == null) ? nullObject : sku;
      jpegSKU.setSelectedItem(o);

      if ( ! o.equals(jpegSKU.getSelectedItem()) ) { // nonstandard SKU, add to combo
         //
         // why equals and not just ==?  well, since the combo isn't editable,
         // it should in theory always return the identical object,
         // even for strings, but I don't want to rely on that.
         // why reversed order?  because o is known to be non-null

         jpegSKU.insertItemAt(o,1);
         jpegSKU.setSelectedIndex(1);
         // put nonstandard items at position 1, out of sort order
      }
      // note that setSelectedJpegSKU is only called immediately after
      // reloadJpegSKU, so there's no way for the combo to fill up
      // with gunk ... there can be at most one nonstandard SKU in the list.
   }

   private void reloadJpegSKUs() {
      jpegSKU.removeAllItems();

      final LinkedList list = new LinkedList();
      config.productConfig.iterate(new ProductCallback() { public void f(SKU sku, String description, Long dueInterval) {
         list.add(sku);
      } });
      Collections.sort(list,SKUComparator.displayOrder);

      jpegSKU.addItem(nullObject);
      Iterator i = list.iterator();
      while (i.hasNext()) {
         jpegSKU.addItem(i.next());
      }
   }

// --- other commands ---

   private void doEditSubset() {
      LinkedList skus = new LinkedList(config.userConfig.skus);
      if (new EditSubsetDialog(this,config.productConfig,skus).run()) {
         config.userConfig.skus = skus;
      }
   }

   private void doRefresh() {

      try {
         getMerchantConfig();
      } catch (ValidationException e) {
      }
      // like doEditSKU, above

      config.dealers = DealerTransaction.refresh(this,config.dealerURL,config.merchantConfig,config.dealers);
      // the refresh function handles the UI and everything
   }

   private void doBarcode() {

      // get enables and width number, if possible, since they're used
      // in the weak validation of barcode settings.
      // note that we save getting the width (which can fail) for last.
      try {
         getJpegSubset();
      } catch (ValidationException e) {
      }
      // like doEditSKU, above

      PrintConfig pc = (PrintConfig) CopyUtil.copy(config.printConfig);
      if (new BarcodeDialog(this,pc).run()) {

         // the changes were accepted, but now we don't have any good way
         // to get them back into the original PrintConfig.
         // the problem is, ConfigSetup and maybe other things hold references
         // to subobjects of the original, so we can't replace them.
         // we can't just say config.printConfig = pc; we can't even hack in
         // and use StructureDefinition.copy, because that copies subobjects.
         // so, since I'm short on time, just be stupid and copy the specific
         // fields that the dialog modifies.
         //
         BarcodeDialog.recopy(config.printConfig,pc);
      }
   }

   private void doEditDLBW() {
      BandwidthConfig bc = config.downloadConfig.bandwidthConfig.copy();
      if (new BandwidthDialog(this,bc,"d",false).run()) {
         config.downloadConfig.bandwidthConfig = bc;
      }
   }

   private void doEditULBW() {
      BandwidthConfig bc = config.uploadConfig.bandwidthConfig.copy();
      if (new BandwidthDialog(this,bc,"u",showKiosk).run()) {
         config.uploadConfig.bandwidthConfig = bc;
      }
   }

   private void doEditAutoComplete() {

      try {
         getMerchantConfig();
      } catch (ValidationException e) {
      }
      // like doEditSKU, above

      AutoCompleteConfig acc = config.autoCompleteConfig.copy();
      LinkedList storeHours = CopyUtil.copyList(config.storeHours);
      if (new AutoCompleteDialog(this,acc,storeHours,config.storeHoursURL,config.merchantConfig).run()) {
         config.autoCompleteConfig = acc;
         config.storeHours = storeHours;
      }
   }

   private void doEditEnableTracking() {
      CarrierDialog d = new CarrierDialog(this,config.carrierList,config.carrierURL);
      if (d.run()) {
         config.carrierList = d.result(); // here the dialog handles the object copy
      }
   }

   private void doResolve() {

      LinkedList records = new LinkedList();

      ResolveDialog.accumulate(records,setupInvoice.accessPrinterCombo());
      ResolveDialog.accumulate(records,setupLabel  .accessPrinterCombo());

      Iterator i = config.queueList.queues.iterator();
      while (i.hasNext()) {
         Queue q = (Queue) i.next();

         ConfigFormat panel = getPanel(q.queueID,q.format,/* create = */ false);
         JComboBox combo = panel.accessPrinterCombo();
         if (combo != null) ResolveDialog.accumulate(records,combo);
      }
      // same iteration over queues as in other places, so order of occurrence
      // is the alphabetical order of the queues, and we don't get combos from
      // panels that are not for the current format of the queue.
      // the second part is debatable, but the alphabetical order is important.

      if (records.isEmpty()) {
         Pop.info(this,Text.get(this,"e8"),Text.get(this,"s281"));
         return;
      }

      // could sort records here, but order of occurrence will be
      // more stable than alphabetical order

      ResolveDialog.match(records,config.printConfig.installHints);
      // OK to get hints directly from config, not editable in UI

      if (new ResolveDialog(this,records).run()) {
         ResolveDialog.apply(records);
      }
   }

   private void doThreadAssign() {

      if (config.queueList.threads.isEmpty()) {
         Pop.info(this,Text.get(this,"e9"),Text.get(this,"s288"));
         return;
      }

      new ThreadDialog(this,config.queueList).run();
      // on success, writes directly on real config
   }

   private void updateThreadList() {

      // this is an unusual combo because it's just for show.
      // note that items appear in the order they were added.
      threadList.removeAllItems();
      threadList.addItem(ThreadDefinition.DEFAULT_THREAD_NAME);

      Iterator i = config.queueList.threads.iterator();
      while (i.hasNext()) {
         threadList.addItem((ThreadDefinition) i.next());
      }
   }

   private ThreadDefinition getSelectedThread() {
      Object o = threadList.getSelectedItem();
      return (o instanceof ThreadDefinition) ? (ThreadDefinition) o : null;
   }

   private void doThreadCreate() {

      while (true) {
         String threadName = Pop.inputString(this,Text.get(this,"s295"),Text.get(this,"s294"));
         if (threadName == null) return; // canceled

         try {
            config.queueList.createThread(threadName.trim());
            break;
         } catch (ValidationException e) {
            Pop.error(this,e,Text.get(this,"s296"));
         }
      }

      updateThreadList();
   }

   private void doThreadRename() {

      ThreadDefinition t = getSelectedThread();
      if (t == null) {
         Pop.error(this,Text.get(this,"e10"),Text.get(this,"s297"));
         return;
      }
      String threadNameOld = t.threadName;

      while (true) {
         String threadName = Pop.inputString(this,Text.get(this,"s299",new Object[] { threadNameOld }),Text.get(this,"s298"));
         if (threadName == null) return; // canceled

         try {
            config.queueList.renameThread(t,threadName.trim());
            break;
         } catch (ValidationException e) {
            Pop.error(this,e,Text.get(this,"s300"));
         }
      }

      updateThreadList();
   }

   private void doThreadDelete() {

      ThreadDefinition t = getSelectedThread();
      if (t == null) {
         Pop.error(this,Text.get(this,"e11"),Text.get(this,"s301"));
         return;
      }
      String threadNameOld = t.threadName;

      boolean confirmed = Pop.confirm(this,Text.get(this,"s303",new Object[] { threadNameOld }),Text.get(this,"s302"));
      if ( ! confirmed ) return;

      try {
         config.queueList.deleteThread(t);
      } catch (ValidationException e) {
         Pop.error(this,e,Text.get(this,"s304"));
         return;
      }

      updateThreadList();
   }

}

