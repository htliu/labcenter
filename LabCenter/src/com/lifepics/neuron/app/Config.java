/*
 * Config.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.axon.Dealer;
import com.lifepics.neuron.axon.LocalImageConfig;
import com.lifepics.neuron.axon.PollConfig;
import com.lifepics.neuron.axon.PriceList;
import com.lifepics.neuron.axon.ProMode;
import com.lifepics.neuron.axon.Roll;
import com.lifepics.neuron.axon.ScanConfigDLS;
import com.lifepics.neuron.axon.UploadConfig;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.AutoCompleteConfig;
import com.lifepics.neuron.dendron.CarrierList;
import com.lifepics.neuron.dendron.CoverInfo;
import com.lifepics.neuron.dendron.DownloadConfig;
import com.lifepics.neuron.dendron.LocalConfig;
import com.lifepics.neuron.dendron.OrderEnum;
import com.lifepics.neuron.dendron.ProductConfig;
import com.lifepics.neuron.dendron.QueueList;
import com.lifepics.neuron.gui.PrintConfig;
import com.lifepics.neuron.gui.ProductBarcodeConfig;
import com.lifepics.neuron.gui.Style;
import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.misc.PurgeConfig;
import com.lifepics.neuron.misc.StoreHours;
import com.lifepics.neuron.net.DiagnoseConfig;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.object.Relative;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for the whole application.
 */

public class Config extends Structure {

// --- note ---

   // this applies not just to fields in Config itself, but to all
   // subfields recursively.
   // this is just the best place I can think of to put the note.

   // normally, when you add a config field, you want to make sure that
   // it's distributed to the right places at startup, and that
   // it's redistributed to the right places when the config changes.
   //
   // some fields, like helpURL, are always retrieved in the UI thread
   // from the current config object.  that counts as redistribution.
   //
   // but, in a few cases, it's really too much trouble to redistribute,
   // and then you need to add the field to AutoConfig.equalsUndistributed,
   // so that we'll know to restart LabCenter if it changes in auto-config.
   // such fields cannot be edited in the UI, of course
   //  -- unless later we add some restart-forcing code there too.
   //
   // the other thing you need to be aware of is, when we auto-config
   // at startup time, most objects aren't constructed, so we can't
   // use the standard redistribution code.  instead, there's some custom
   // code in Main.initAutoUpdate.  but, you only need to add lines there
   // if the field is used prior to auto-update,
   // *and* if it's not one that will cause a restart.

// --- fields ---

   // fields that are not real
   public int versionLoaded;
   public File manualImageDir; // real one is in Memory class

   // general
   public LinkedList installDirs;
   public int proMode;
   public String proEmail;
   public String style;
   public int lockPort;
   public String frameTitleSuffix; // nullable
   public boolean showQueueTab;
   public boolean showGrandTotal;
   public boolean holdInvoice;
   public boolean enableItemPrice;
   public boolean autoComplete;
   public AutoCompleteConfig autoCompleteConfig;
   public String storeHoursURL;
   public LinkedList storeHours;
   public boolean warnNotPrinted;
   public User.Config userConfig;
   public String helpURL;
   public String manualURL;
   public LinkedList helpItems;
   public String adminURL;
   public String statusURL;
   public String newsURL;
   public String restoreURL;
   public int statusNewsInterval;
   public int backupCount;
   public boolean minimizeAtStart;
   public boolean minimizeAtClose;
   public boolean passwordForExit;
   public boolean passwordForSetup;
   public boolean passwordForUnminimize;
   public String unminimizePassword;
   public boolean preventStopStart;
   public LinkedList formatUnlockList; // list of Integer

   // log files
   // logDir is in a fixed place below mainDir
   public int logCount;
   public int logSize; // bytes
   public Level logLevel;
   public Level reportLevel;
   public int reportQueueSize;
   public String reportURL;
   public int kioskLogCount;
   public int kioskLogSize; // bytes

   // data files
   public File orderDataDir;
   public File rollDataDir;
   public QueueList queueList;
   public ProductConfig productConfig;
   public LinkedList coverInfos;
   public boolean autoSpawn; // may show as "auto print" in UI
   public boolean autoSpawnSpecial;
   public String autoSpawnPrice; // nullable
   public long scanInterval; // millis
   public PurgeConfig purgeConfig;

   // formatting
   public long dlsBusyPollInterval; // millis
   public int dlsBusyRetries;
   public String skuRefreshURL; // oldProductURL
   public String newProductURL;
   public String conversionURL;

   // common
   public MerchantConfig merchantConfig;
   public DiagnoseConfig diagnoseConfig;
   public int defaultTimeoutInterval;
   public boolean errorWindowEnabled;
   public long    errorWindowInterval; // millis
   public int     errorWindowPercent;

   // local share
   public Boolean localShareEnabled; // null means locked, not even visible
   public File localShareDir;

   // download
   public boolean downloadEnabled;
   public DownloadConfig downloadConfig;
   public String carrierURL;
   public CarrierList carrierList;

   // local scanner
   public boolean localEnabled;
   public LocalConfig localConfig;

   // upload
   public boolean uploadEnabled;
   public UploadConfig uploadConfig;
   public boolean claimEnabled;
   public String claimEmail;
   public String priceListURL;
   public LinkedList priceLists;
   public String dealerURL;
   public LinkedList dealers;

   // Pakon scanner
   public boolean pakonEnabled;
   public int pakonPort;

   // poll scanner
   public boolean pollEnabled;
   public PollConfig pollConfig;
   public LocalImageConfig localImageConfig;

   // other scanners
   public boolean scanEnabled;
   public long scannerPollInterval; // millis
   public ScanConfigDLS scanConfigDLS;

   // auto-update
   public AutoUpdateConfig autoUpdateConfig;
   public Boolean customInvoiceXsl;

   // auto-print
   public PrintConfig printConfig;
   public ProductBarcodeConfig productBarcodeConfig;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Config.class,
      4,new History(new int[] {47,720,48,721,49,723,50,724,51,751,52,761,53,763,54}),
         // sync with special cases in Main.java
      new AbstractField[] {

         new InlineListField("installDirs","InstallDir"), // pretend this has been a real field all along
         new EnumeratedField("proMode","ProMode",ProMode.proModeType,47,ProMode.NORMAL) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               if (version >= 25) { // when it was boolean
                  tset(o,Convert.toBool(getElementText(node,xmlName)) ? ProMode.PRO_OLD : ProMode.NORMAL);
               } else {
                  loadDefault(o);
               }
            }
            public void load(Node node, Object o, int version) throws ValidationException {
               Config config = (Config) o;

               // here's where we initialize the two fields that aren't really there.
               // this has nothing to do with proMode, it just has to go somewhere.
               // (also it runs first, just like it used to, not that that matters.)

               config.versionLoaded = version;

               if (version < 16) {
                  config.manualImageDir = Convert.toFile(XML.getElementText(node,"ManualImageDir"));
               }
               // else no longer used

               super.load(node,o,version);
            }
            public void makeRelativeTo(Object o, File base) {
               Config config = (Config) o;
               if (config.manualImageDir != null) config.manualImageDir = Relative.makeRelativeTo(base,config.manualImageDir);
            }
         },
         new StringField("proEmail","ProEmail",25,""),
         new StringField("style","Style",21,Style.getDefaultStyle()),
         new IntegerField("lockPort","LockPort"),
         new NullableStringField("frameTitleSuffix","FrameTitleSuffix",39,null),
         new BooleanField("showQueueTab","ShowQueueTab",6,false),
         new BooleanField("showGrandTotal","ShowGrandTotal",37,false),
         new BooleanField("holdInvoice","HoldInvoice",7,false),
         new BooleanField("enableItemPrice","EnableItemPrice",50,false),
         new BooleanField("autoComplete","AutoComplete",10,false),
         new StructureField("autoCompleteConfig","AutoCompleteConfig",AutoCompleteConfig.sd,45) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               loadDefault(o); // usual behavior

               // now maybe pull in autoCompleteDelay, which used to be a top-level field by itself.
               // it also used to be Long, not long -- that's why there are two layers of "if" here.
               if (version >= 40) {
                  String s = XML.getNullableText(node,"AutoCompleteDelay-Millis");
                  if (s != null) {
                     ((Config) o).autoCompleteConfig.autoCompleteDelay = Convert.toLong(s);
                  }
               }
            }
         },
         new StringField("storeHoursURL","StoreHoursURL",45,"https://api.lifepics.com/closed/LCService.asmx/GetHoursInfo"),
         new StructureNestField("storeHours","StoreHours","Entry",StoreHours.sd,Merge.IDENTITY,45,0).with(StoreHours.idAccessor,StoreHours.idComparator),
         new BooleanField("warnNotPrinted","WarnNotPrinted",8,false),
         new StructureField("userConfig","UserConfig",User.Config.sd,11),
         new StringField("helpURL","HelpURL",15,"http://www.lifepics.com/marketing/help.htm"),
         new StringField("manualURL","ManualURL",30,"http://help.lifepics.com/LabCenter_TOC.htm"),
         new StructureListField("helpItems","HelpItem",HelpItem.sd,Merge.POSITION,49,0),
         new StringField("adminURL","AdminURL",47,"https://services.lifepics.com/LabCenter/GoToAdmin.asp"),
         new StringField("statusURL","StatusURL",25,"https://services.lifepics.com/net/LabcenterStatus.aspx"),
         new StringField("newsURL","NewsURL",23,"http://www.lifepics.com/marketing/LabCenter/news.htm"),
         new StringField("restoreURL","RestoreURL",42,"https://services.lifepics.com/net/LabCenter/Service.asmx/GetBackupInstances"),
         new IntegerField("statusNewsInterval","StatusNewsInterval-Millis",23,3600000), // 1 hour
         new IntegerField("backupCount","BackupCount",16,10),
         new BooleanField("minimizeAtStart","MinimizeAtStart",35,false),
         new BooleanField("minimizeAtClose","MinimizeAtClose",27,false),
         new BooleanField("passwordForExit","PasswordForExit",27,false),
         new BooleanField("passwordForSetup","PasswordForSetup",27,false),
         new BooleanField("passwordForUnminimize","PasswordForUnminimize",46,false),
         new NullableStringField("unminimizePassword","UnminimizePassword",46,null),
         new BooleanField("preventStopStart","PreventStopStart",28,false) {
            public void loadDefault(Object o) {
               tset(o,((Config) o).passwordForSetup); // which is already loaded
            }
         },
         new IntegerListField("formatUnlockList","FormatUnlock",OrderEnum.formatType),

         new IntegerField("logCount","LogCount"),
         new IntegerField("logSize","LogSize-Bytes"),
         new LevelField("logLevel","LogLevel"),
         new LevelField("reportLevel","ReportLevel",15,Level.SEVERE),
         new IntegerField("reportQueueSize","ReportQueueSize",13,100),
         new StringField("reportURL","ReportURL",13,"https://services.lifepics.com/LabCenter/Report.asp"),
         new IntegerField("kioskLogCount","KioskLogCount",53,10),
         new IntegerField("kioskLogSize","KioskLogSize",53,100000),

         new FileField("orderDataDir","OrderDataDir"),
         new FileField("rollDataDir","RollDataDir"),
         new StructureField("queueList","QueueList",QueueList.sd),
         new StructureField("productConfig","ProductConfig",ProductConfig.sd,38) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               tset(o,new ProductConfig().loadDefault(XML.getElement(node,"QueueList")));
               // this is not a mistake; the product config migrates from the queue list
            }
         },
         new StructureListField("coverInfos","CoverInfo",CoverInfo.sd,Merge.IDENTITY).with(CoverInfo.bookSKUAccessor,CoverInfo.bookSKUComparator),
         new BooleanField("autoSpawn","AutoSpawn"),
         new BooleanField("autoSpawnSpecial","AutoSpawnSpecial",44,true), // true is bad default, but that's the old behavior
         new NullableStringField("autoSpawnPrice","AutoSpawnPrice",54,null),
         new LongField("scanInterval","ScanInterval-Millis",5,60000),
         new StructureField("purgeConfig","PurgeConfig",PurgeConfig.sd),

         new LongField("dlsBusyPollInterval","DLSBusyPollInterval-Millis",18,60000), // 1 minute
         new IntegerField("dlsBusyRetries","DLSBusyRetries",18,5),
         new StringField("skuRefreshURL","SKURefreshURL",18,"https://services.lifepics.com/LabCenter/GetSKUList.asp"),
         new StringField("newProductURL","NewProductURL",39,"https://services.lifepics.com/net/GetSKUList.aspx"),
         new StringField("conversionURL","ConversionURL",41,"https://services.lifepics.com/net/GetSkuMapping.aspx"),
            // added in version 39, re-defaulted in version 41

         new StructureField("merchantConfig","MerchantConfig",MerchantConfig.sd),
         new StructureField("diagnoseConfig","DiagnoseConfig",DiagnoseConfig.sd),
         new IntegerField("defaultTimeoutInterval","DefaultTimeoutInterval-Millis"),
         new BooleanField("errorWindowEnabled","ErrorWindowEnabled",17,true),
         new LongField("errorWindowInterval","ErrorWindowInterval-Millis",17,3600000), // 1 hour
         new IntegerField("errorWindowPercent","ErrorWindowPercent",17,90),

         new NullableBooleanField("localShareEnabled","LocalShareEnabled",49,null),
         new NullableFileField("localShareDir","LocalShareDir",48,null),

         new BooleanField("downloadEnabled","DownloadEnabled"),
         new StructureField("downloadConfig","DownloadConfig",DownloadConfig.sd),
         new StringField("carrierURL","CarrierURL",51,"https://api.lifepics.com/closed/LCService.asmx/GetCarriers"),
         new StructureField("carrierList","CarrierList",CarrierList.sd,51),

         new BooleanField("localEnabled","LocalEnabled",43,false),
         new StructureField("localConfig","LocalConfig",LocalConfig.sd,43),

         new BooleanField("uploadEnabled","UploadEnabled"),
         new StructureField("uploadConfig","UploadConfig",UploadConfig.sd),
         new BooleanField("claimEnabled","ClaimEnabled",45,false),
         new StringField("claimEmail","ClaimEmail",45,Roll.ANONYMOUS_ACCOUNT),
         new StringField("priceListURL","PriceListURL",26,"https://services.lifepics.com/LabCenter/PriceList.aspx"),
         new StructureNestField("priceLists","PriceLists","PriceList",PriceList.sd,Merge.IDENTITY,26,0).with(PriceList.idAccessor,PriceList.nameComparator),
         new StringField("dealerURL","DealerURL",31,"https://services.lifepics.com/LabCenter/GetDealerList.asp"),
         new StructureNestField("dealers","Dealers","Dealer",Dealer.sd,Merge.IDENTITY,31,0).with(Dealer.idAccessor,Dealer.nameComparator),

         new BooleanField("pakonEnabled","PakonEnabled"),
         new IntegerField("pakonPort","PakonPort"),

         new BooleanField("pollEnabled","PollEnabled",9,true),
         new StructureField("pollConfig","PollConfig",PollConfig.sd,9),
         new StructureField("localImageConfig","LocalImageConfig",LocalImageConfig.sd,48),

         new BooleanField("scanEnabled","ScanEnabled",22,false),
         new LongField("scannerPollInterval","ScannerPollInterval-Millis",22,3600000), // 1 hour
         new StructureField("scanConfigDLS","ScanConfigDLS",ScanConfigDLS.sd,22),

         new StructureField("autoUpdateConfig","AutoUpdateConfig",AutoUpdateConfig.sd),
         new NullableBooleanField("customInvoiceXsl","CustomInvoiceXsl",36,null),

         new StructureField("printConfig","PrintConfig",PrintConfig.sd),
         new StructureField("productBarcodeConfig","ProductBarcodeConfig",ProductBarcodeConfig.sd,52)
      });

   protected StructureDefinition sd() { return sd; }

// --- copy function ---

   public Config copy() { return (Config) CopyUtil.copy(this); }

// --- validation ---

   public void validate() throws ValidationException {

      ProMode.validateProMode(proMode);

      autoCompleteConfig.validate();
      StoreHours.validate(storeHours);

      // cross-validation on things that use the store hours (which are meant to be shared)
      autoCompleteConfig.validateHours(storeHours);

      userConfig.validate();

      if (statusNewsInterval < 1) throw new ValidationException(Text.get(this,"e17"));
      if (backupCount < 0) throw new ValidationException(Text.get(this,"e7"));

      if (passwordForUnminimize && unminimizePassword == null) throw new ValidationException(Text.get(this,"e18"));

      // could check uniqueness in formatUnlockList, but it doesn't really matter

      if (logCount < 1) throw new ValidationException(Text.get(this,"e2"));
      if (logSize < 0) throw new ValidationException(Text.get(this,"e3"));
      if (reportQueueSize < 1) throw new ValidationException(Text.get(this,"e6"));
      if (kioskLogCount < 1) throw new ValidationException(Text.get(this,"e20"));
      if (kioskLogSize < 0) throw new ValidationException(Text.get(this,"e21"));

      queueList.validate();
      productConfig.validate();
      CoverInfo.validate(coverInfos);

      if (autoSpawnPrice != null) Convert.toCents(autoSpawnPrice); // error message is good enough

      if (scanInterval < 1) throw new ValidationException(Text.get(this,"e5"));

      purgeConfig.validate();

      if (dlsBusyPollInterval < 1) throw new ValidationException(Text.get(this,"e10"));
      if (dlsBusyRetries < 0) throw new ValidationException(Text.get(this,"e11"));

      merchantConfig.validate();
      diagnoseConfig.validate();

      if (defaultTimeoutInterval < 1) throw new ValidationException(Text.get(this,"e4"));

      if (errorWindowInterval < 1) throw new ValidationException(Text.get(this,"e8"));
      if (errorWindowPercent < 1 || errorWindowPercent > 100) throw new ValidationException(Text.get(this,"e9"));

      if (Nullable.nbToB(localShareEnabled) && localShareDir == null) throw new ValidationException(Text.get(this,"e19"));

      downloadConfig.validate();
      carrierList.validate();
      localConfig.validate();
      if (localEnabled && localConfig.directory == null) throw new ValidationException(Text.get(this,"e15"));
      uploadConfig.validate();
      PriceList.validate(priceLists);
      Dealer.validate(dealers);
      pollConfig.validate();
      localImageConfig.validate();

      if (scannerPollInterval < 1) throw new ValidationException(Text.get(this,"e12"));
      scanConfigDLS.validate();
      if (scanEnabled && scanConfigDLS.effectiveDate == null) throw new ValidationException(Text.get(this,"e13"));

      autoUpdateConfig.validate();
      printConfig.validate();
      productBarcodeConfig.validate();
   }

}

