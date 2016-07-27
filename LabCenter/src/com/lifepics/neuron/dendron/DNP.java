/*
 * DNP.java
 */

package com.lifepics.neuron.dendron;

/**
 * A minimal JNI wrapper around most but not all of the DNP API.
 * The internal name of the DNP API is "CSP", hence the DLL is named
 * "cspstat.dll" and the JNI wrapper DLL is named "jnicsp.dll".
 * All this is not to be confused with DNP Print Turbine integration,
 * which is still known in the LC code as Pixel Magic.
 */

public class DNP {

   static { System.loadLibrary("jnicsp"); }
   //
   // if the library is missing, you get an UnsatisfiedLinkError,
   // see COS.java for details.  static constants are OK, though.

// --- constants ---

   public static final int PRINTER_MP3  = 1;
   public static final int PRINTER_MP4  = 2;
   public static final int PRINTER_DS40 = 3;
   public static final int PRINTER_DS80 = 4;
   public static final int PRINTER_RX1  = 5; // DS-RX1

   // there are really two media enumerations,
   // the one we get back from GetMedia
   // that tells what media is loaded in the printer,
   // and the one we pass to SetMediaSize
   // that tells what kind of images we want to send.
   // the first is a subset of the second, marked *.
   // actually the first comes back as different codes
   // in strings, but I'll convert them.
   //
   // actually there are four media enumerations:
   // * what GetMedia can return
   // + ones the user can select in EditSKUDialog
   // - multiples the code may generate
   // x ones I never use under any conditions
   //
   public static final int MEDIA_5x35    =  1; // * L
   public static final int MEDIA_5x7     =  2; // * 2L
   public static final int MEDIA_6x4     =  3; // * PC
   public static final int MEDIA_6x8     =  4; // * A5
   public static final int MEDIA_6x9     =  5; // * A5W
   public static final int MEDIA_6x4x2   =  6; // - PCx2
   //
   public static final int MEDIA_8x10    = 31; // *
   public static final int MEDIA_8x12    = 32; // *
   public static final int MEDIA_8x4     = 33; // +
   public static final int MEDIA_8x5     = 34; // +
   public static final int MEDIA_8x6     = 35; // +
   public static final int MEDIA_8x8     = 36; // +
   public static final int MEDIA_8x4x2   = 37; // -
   public static final int MEDIA_8x5x2   = 38; // -
   public static final int MEDIA_8x6x2   = 39; // -
   public static final int MEDIA_8x5_8x4 = 40; // x
   public static final int MEDIA_8x6_8x4 = 41; // x
   public static final int MEDIA_8x6_8x5 = 42; // x
   public static final int MEDIA_8x8_8x4 = 43; // x
   public static final int MEDIA_8x4x3   = 44; // -

   public static final int CMODE_STANDARD = 0;
   public static final int CMODE_NONSCRAP = 1;
   public static final int CMODE_2INCHCUT = 120;

   public static final int FILTER_OFF = 0;
   public static final int FILTER_ON  = 1;

   public static final int FINISH_GLOSSY = 0;
   public static final int FINISH_MATTE1 = 1;

   public static final int RESOLUTION_300 = 300;
   public static final int RESOLUTION_334 = 334;
   public static final int RESOLUTION_600 = 600;

   public static final int RETRY_OFF = 0;
   public static final int RETRY_ON  = 1;

   // status group bit (only one set)
   //
   public static final int STATUS_NORMAL   = 0x00010000; // NORMAL is called USUALLY in their docs
   public static final int STATUS_SETTING  = 0x00020000;
   public static final int STATUS_HARDWARE = 0x00040000;
   public static final int STATUS_SYSTEM   = 0x00080000;
   public static final int STATUS_FLSHPROG = 0x00100000;

   // status detail bit (only one set)
   //
   public static final int STATUS_NORMAL_IDLE       = STATUS_NORMAL | 0x0001;
   public static final int STATUS_NORMAL_PRINTING   = STATUS_NORMAL | 0x0002;
   public static final int STATUS_NORMAL_STANDSTILL = STATUS_NORMAL | 0x0004;
   public static final int STATUS_NORMAL_PAPER_END  = STATUS_NORMAL | 0x0008;
   public static final int STATUS_NORMAL_RIBBON_END = STATUS_NORMAL | 0x0010;
   public static final int STATUS_NORMAL_COOLING    = STATUS_NORMAL | 0x0020;
   public static final int STATUS_NORMAL_MOTCOOLING = STATUS_NORMAL | 0x0040;
   public static final int STATUS_NORMAL_SHOOTING   = STATUS_NORMAL | 0x0080;
   public static final int STATUS_NORMAL_BACKPRINT  = STATUS_NORMAL | 0x0100;
   //
   // that's all the normal ones, leaving out the rest for now

   public static final int STATUS_SETTING_COVER_OPEN   = STATUS_SETTING | 0x0001;
   public static final int STATUS_SETTING_PAPER_JAM    = STATUS_SETTING | 0x0002;
   public static final int STATUS_SETTING_RIBBON_ERR   = STATUS_SETTING | 0x0004;
   public static final int STATUS_SETTING_PAPER_ERR    = STATUS_SETTING | 0x0008;
   public static final int STATUS_SETTING_DATA_ERR     = STATUS_SETTING | 0x0010;
   public static final int STATUS_SETTING_SCRAPBOX_ERR = STATUS_SETTING | 0x0020;

// --- native functions ---

   // there are two kinds of functions that return int
   //
   // unmarked       actual integer results, negative if error
   // marked /*b*/   boolean results, negative if error

   // function order is header file order, which is pretty random

   // I confirmed by experiment that synchronized native static
   // really works.  the lock is on the class object (DNP.class).

   public synchronized native static String    GetFirmwVersion(int port);
   public synchronized native static String    GetSensorInfo(int port);
   public synchronized native static int       GetStatus(int port);
   public synchronized native static String    GetMedia(int port);
   public synchronized native static int       GetPQTY(int port);
   public synchronized native static int       GetCounterL(int port);
   public synchronized native static int       GetCounterMatte(int port);
   public synchronized native static int       GetCounterA(int port);
   public synchronized native static int       GetCounterB(int port);
   public synchronized native static int       GetCounterP(int port);
   public synchronized native static int       GetCounterM(int port);
   public synchronized native static int /*b*/ SetClearCounterA(int port);
   public synchronized native static int /*b*/ SetClearCounterB(int port);
   public synchronized native static int /*b*/ SetClearCounterM(int port);
   public synchronized native static int /*b*/ SetCounterP(int port, int counter);
   public synchronized native static int       GetResolutionH(int port);
   public synchronized native static int       GetResolutionV(int port);
   public synchronized native static int       GetFreeBuffer(int port);
   public synchronized native static String    GetMediaLotNo(int port);
   public synchronized native static int       GetMediaCounter(int port);
   public synchronized native static int       GetMediaColorOffset(int port);
   public synchronized native static int /*b*/ SetCutterMode(int port, int cmode);
   public synchronized native static int /*b*/ SetOvercoatFinish(int port, int finish);
   public synchronized native static int /*b*/ SetRetryControl(int port, int retry);
   public synchronized native static int       GetPrinterPortNum(byte[] data);
   public synchronized native static int /*b*/ SetPQTY(int port, int quantity);
   public synchronized native static int /*b*/ SetMediaSize(int port, int media);
   public synchronized native static int /*b*/ SendImageData(int port, byte[] data, int x, int y, int w, int h);
   public synchronized native static int /*b*/ PrintImageData(int port);
   public synchronized native static int /*b*/ StartPageLayout(int port);
   public synchronized native static int /*b*/ EndPageLayout(int port);
   public synchronized native static int /*b*/ SetResolution(int port, int resolution);
   public synchronized native static String    GetSerialNo(int port);
   public synchronized native static int /*b*/ StopPrint(int port);
   public synchronized native static String    GetRfidMediaClass(int port);
   public synchronized native static String    GetRfidReserveData(int port, int page);
   public synchronized native static int       GetInitialMediaCount(int port);
   public synchronized native static int /*b*/ SetUSBTimeout(int port, int time);
   public synchronized native static int       SetPrinterFilter(int filter);

}

