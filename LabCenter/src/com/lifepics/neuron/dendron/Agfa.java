/*
 * Agfa.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;

import com.agfa.pfdf.ConnectionKit;

/**
 * Try a slightly different strategy than with the COS integration.
 * There, the COS class has the native functions,
 * and FormatDLS calls them directly and generates exceptions as needed.
 * Here, the ConnectionKit class (in the Agfa jar) has the native functions,
 * this class has exception-generating wrappers that also handle the
 * somewhat nasty output variables, and FormatAgfa will call this class.<p>
 *
 * Also the way the enumerations are handled is different.
 * There, they're with the native functions, but here they're in the middle ...
 * which makes sense, because it's the top-level caller that uses constants.
 */

public class Agfa {

   // *** WARNING!  This class requires an external jar file!
   // *** Callers must catch and handle NoClassDefFoundError!

// --- constants ---

   // standard subdirectories
   public static final String SUBDIR_DATA_PATH      = "data";
   public static final String SUBDIR_DELETE_PATH    = "delete";
   public static final String SUBDIR_PICTURE_PATH   = "pict";
   public static final String SUBDIR_PROT_PATH      = "prot";
   public static final String SUBDIR_RECV_PATH      = "recv";
   public static final String SUBDIR_SEND_PATH      = "send";
   public static final String SUBDIR_SERIALIZE_PATH = "serialize";
   public static final String SUBDIR_VERSION_PATH   = "version";

   // service
   public static final String SERVICE_DEFAULT = "5001"; // could be a string service name

   // the rest of these are incomplete enumerations, sometimes very incomplete

   // device ID (also machine number)
   public static final int DEVICE_ID_CONNECTION_KIT = 200;

   // device type (also machine type)
   public static final int DEVICE_TYPE_CONNECTION_KIT = 122;

   // film size
   public static final int FILM_SIZE_MIXED = 0;

   // film category
   public static final int FILM_CATEGORY_DIGITAL_IMAGE = 3;

   // photo finishing mode
   public static final int PHOTO_FINISHING_MODE_FIRST_RUN = 0;

   // scheme
   public static final String SCHEME_PDM = "PDM://";

   // input media
   public static final int INPUT_MEDIA_NETWORK = 4;

   // work type
   public static final int WORK_TYPE_SERVICE_PRINT = 1;

   // paper surface
   // same as the mediaSurface enumeration in COS, except no negative values allowed.

   // attribute ID
   public static final int ATTY_PrintLength = 40;
   public static final int ATTY_BackprintText = 91;
   public static final int ATTY_RotationInstructions = 200;
   public static final int ATTY_OrderIDClient = 341;
   public static final int ATTY_SourceDeviceType = 346;
   public static final int ATTY_TargetDeviceID = 347;
   public static final int AGFA_ATTY_ImageFillFlag = 11007;
   public static final int AGFA_ATTY_WhiteBorderWidth = 11032;

   // image fill
   public static final int IMAGE_FILL_MIN = 0;
   public static final int IMAGE_FILL_FIT_IMAGE = 0;
   public static final int IMAGE_FILL_FILL_PRINT = 1;
   public static final int IMAGE_FILL_FIX_LONGER = 2;
   public static final int IMAGE_FILL_FIX_SHORTER = 3;
   public static final int IMAGE_FILL_MAX = 3;

// --- utilities ---

   // the native functions are non-static, so we need an object,
   // but the output variables are static, so it doesn't make any sense
   // to have more than one object.  and, here's the one object.

   private static ConnectionKit kit = new ConnectionKit();

   private static void check(String key, int errorCode) throws AgfaException {
      if (errorCode != 0) throw new AgfaException(Text.get(Agfa.class,key),errorCode);
   }

// --- wrapper functions ---

   // note that the names are lowercase ... the point is to distinguish them
   // from the original ConnectionKit functions, which were uppercase.
   // also it looks better this way, fits the normal Java naming conventions.

   public static void setPathOptions(String DataPath, String DeletePath, String PicturePath, String ProtPath, String RecvPath, String SendPath, String SerializePath, String VersionPath) throws AgfaException {
      check("f1",kit.dllSetPathOptions(DataPath,DeletePath,PicturePath,ProtPath,RecvPath,SendPath,SerializePath,VersionPath));
   }
   public static void setConnectionOptions(String Host, String Service, int nBatchNumberPoolsize, int nImageNumberPoolsize) throws AgfaException {
      check("f2",kit.dllSetConnectionOptions(Host,Service,nBatchNumberPoolsize,nImageNumberPoolsize));
   }
   public static void setClientOptions(int nMachineNumber, int nMachineType, String SoftwareVersion, int nOperatorNumber, String OperatorName, String DealerNumber, String DealerName, boolean bTrashMode) throws AgfaException {
      check("f3",kit.dllSetClientOptions(nMachineNumber,nMachineType,SoftwareVersion,nOperatorNumber,OperatorName,DealerNumber,DealerName,bTrashMode));
   }
   public static void startCK() throws AgfaException {
      check("f4",kit.dllStartCK());
   }
   public static void stopCK() throws AgfaException {
      check("f5",kit.dllStopCK());
   }
   public static boolean checkConnection(String Host, String Service, int nTimeout) {
      return (kit.dllCheckConnection(Host,Service,nTimeout) == 0);
      // the native function returns 0 for success, -1 for at least one failure case.
      // the C++ example code treats it as an error code ... so, I guess I will, too.
   }

   public static int createBatch(int nFilmSize, int nFilmCategory, int nPhotoFinishingMode) throws AgfaException {
      check("f6",kit.dllCreateBatch(nFilmSize,nFilmCategory,nPhotoFinishingMode));
      return kit.m_outHandle;
   }
   public static void destroyBatch(int hBatch) throws AgfaException {
      check("f7",kit.dllDestroyBatch(hBatch));
   }
   public static void saveBatch(int hBatch) throws AgfaException {
      check("f8",kit.dllSaveBatch(hBatch));
   }
   public static void sendBatch(int hBatch) throws AgfaException {
      check("f9",kit.dllSendBatch(hBatch));
   }

   public static int addImage(int hBatch, String ImageFile, String ThumbnailFile, String ShareName, String Scheme, int nInputMedia, boolean bDeleteSource) throws AgfaException {
      check("f10",kit.dllAddImage(hBatch,ImageFile,ThumbnailFile,ShareName,Scheme,nInputMedia,bDeleteSource));
      return kit.m_outHandle;
   }
   public static void sendImage(int hImage) throws AgfaException {
      check("f11",kit.dllSendImage(hImage));
   }

   public static int addImageJob(int hImage, int nWorkType, int nPaperSurface, int nPrintWidth, int nQuantityRequested) throws AgfaException {
      check("f12",kit.dllAddImageJob(hImage,nWorkType,nPaperSurface,nPrintWidth,nQuantityRequested));
      return kit.m_outHandle;
   }

   public static int addBatchEvent(int hBatch, int nEventType, String MessageCode, String MessageText) throws AgfaException {
      check("f13",kit.dllAddBatchEvent(hBatch,nEventType,MessageCode,MessageText));
      return kit.m_outHandle;
   }

   public static void setBatchAttyInt(int hBatch, int nAttyId, int attyValue) throws AgfaException {
      check("f14",kit.dllSetBatchAttyInt(hBatch,nAttyId,attyValue));
   }
   public static void setBatchAttyStr(int hBatch, int nAttyId, String attyValue) throws AgfaException {
      check("f15",kit.dllSetBatchAttyStr(hBatch,nAttyId,attyValue));
   }

   public static void setOrderAttyInt(int hBatch, int nAttyId, int attyValue) throws AgfaException {
      check("f16",kit.dllSetOrderAttyInt(hBatch,nAttyId,attyValue));
   }
   public static void setOrderAttyStr(int hBatch, int nAttyId, String attyValue) throws AgfaException {
      check("f17",kit.dllSetOrderAttyStr(hBatch,nAttyId,attyValue));
   }

   public static void setJobAttyInt(int hJob, int nAttyId, int attyValue) throws AgfaException {
      check("f18",kit.dllSetJobAttyInt(hJob,nAttyId,attyValue));
   }
   public static void setJobAttyStr(int hJob, int nAttyId, String attyValue) throws AgfaException {
      check("f19",kit.dllSetJobAttyStr(hJob,nAttyId,attyValue));
   }

}

