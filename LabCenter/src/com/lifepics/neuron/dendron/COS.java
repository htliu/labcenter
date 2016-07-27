/*
 * COS.java
 */

package com.lifepics.neuron.dendron;

import java.nio.ByteBuffer;

/**
 * A minimal JNI wrapper around the parts of the COS API
 * that are used by DLS.  This ties jnicos.dll into Java.
 */

public class COS {

   static { System.loadLibrary("jnicos"); }
   //
   // if the library is missing, you get an UnsatisfiedLinkError
   // from whatever operation caused the class to load, which in
   // this case is the call to COS.init in FormatDLS.
   // if you catch the exception and try to continue, you just get
   // another ULE from the first native call ... again, COS.init.
   // so, you have to catch it there anyway, and you might as well
   // catch the first as the second.
   // note, the library will always be missing on Macintosh, this
   // is not just an academic exercise like handling missing jars
   // would be.

// --- constants ---

   // since these are static final, you can refer to them
   // without any risk of getting an UnsatisfiedLinkError.
   // this works for primitive types and for strings, but
   // not for other kinds of objects.

   // status
   public static final int COS_OK = 0;
   // lots of other values too

   // fileOp
   public static final short COS_CREATE = 0;
   public static final short COS_READ = 1;

   // productClass
   public static final short kStandardPrint = 0;
   public static final short kIndexPrint = 1;
   public static final short kDigitalFile = 2;
   public static final short kCompositePrint = 3;

   // sizeUnit
   public static final short kInch = 0;
   public static final short kMillimeter = 1;

   // mediaSurface
   public static final short kLabDefault = 0;
   public static final short kOther = 1;
   public static final short kMatte = 2;
   public static final short kSemiMatte = 3;
   public static final short kGlossy = 4;
   public static final short kSmoothLuster = 5;
   public static final short kUltraSmoothHiLuster = 6;
   public static final short kFineGrainedLuster = 7;
   public static final short kSilk = 8;
   public static final short kLuster = 9;
   public static final short kDeepMatte = 10;
   public static final short kHighGloss = 11;
   public static final short kLaminate = 12;

   // imageType
   public static final short kPhotoFilePaper = 0;
   public static final short kSeparateDigitalData = 1;
   public static final short kSameDigitalData = 2;
   public static final short kEmbeddedDigitalData = 3;

   // fileFormat
   public static final short kUnknownFileFormat = 0;
   public static final short kFlashPix = 1;
   public static final short kPhotoCD = 2;
   public static final short kEXIFJPEG = 3;
   public static final short kEXIFTIFF = 4;
   public static final short kJPEG = 5;
   public static final short kTIFF = 6;
   public static final short kPICT = 7;
   public static final short kBMPWin = 8;
   public static final short kBMPOS2 = 9;
   public static final short kUPF = 10;
   public static final short kTIFFIT = 11;
   public static final short kGIF = 12;
   public static final short kCIFF = 13;
   public static final short kSPIFF = 14;

   // fileColorSpace
   public static final short kUnknownColorSpace = 0;
   public static final short kNIFRGB = 1;
   public static final short KPhotoYCC = 2;
   public static final short kITU709 = 3;
   public static final short kSWOPCMYK = 4;
   public static final short ksRGB = 5;
   public static final short kUPFColorSpace = 6;
   public static final short kDefinedByInputDevice = 7;
   public static final short kDefinedByOutputDevice = 8;
   public static final short kDefinedByICCProfile = 9;

   // digitalSharpening
   public static final short kNoSharpening = 0;
   public static final short kSharpened = 1;

// --- reference result class ---

   public static class Ref {
      public int value;
   }

   // another possibility would be to create a jillion identical classes,
   // make the methods below be methods on those classes,
   // and have to extract the int from the object <i>every single time</i>.

// --- native functions ---

   public native static void init();

   public native static int Order_Constructor(String filePath, short fileOp, Ref order);
   public native static int Order_WriteOrder       (int order);
   public native static int Order_Destructor       (int order);
   public native static int Order_GetFileInfo      (int order, Ref fileInfo);
   public native static int Order_GetVendorInfo    (int order, Ref vendorInfo);
   public native static int Order_GetConsumerInfo  (int order, Ref consumerInfo);
   public native static int Order_AddNewProduct    (int order, String productName, Ref product);
   public native static int Order_AddNewImageDetail(int order, Ref imageDetail);
   public native static int Order_AddNewImage      (int order, String imageName, Ref image);

   public native static int VendorInfo_SetVendorOrderNumber(int vendorInfo, String vendorOrderNumber);

   public native static int ConsumerInfo_SetConsumerID (int consumerInfo, String consumerID);
   public native static int ConsumerInfo_GetAddressInfo(int consumerInfo, Ref addressInfo);

   public native static int FileInfo_GetFileMod(int fileInfo, short fileModIndex, Ref fileMod);

   public native static int FileMod_SetApplication(int fileMod, String application);

   public native static int AddressInfo_SetDayPhone    (int addressInfo, String dayPhone);
   public native static int AddressInfo_SetNightPhone  (int addressInfo, String nightPhone);
   public native static int AddressInfo_SetFaxNumber   (int addressInfo, String faxNumber);
   public native static int AddressInfo_SetEmailAddress(int addressInfo, String emailAddress);
   public native static int AddressInfo_SetName        (int addressInfo, short nameIndex, String name);

   public native static int Product_SetProductClass      (int product, short productClass);
   public native static int Product_SetProductDescription(int product, String productDescription);
   public native static int Product_GetFinishingInfo     (int product, Ref finishingInfo);
   public native static int Product_GetPrintInfo         (int product, Ref printInfo);
   public native static int Product_AddImageDetailRef    (int product, int imageDetail);

   public native static int FinishingInfo_SetQuantity(int finishingInfo, int quantity);

   public native static int PrintInfo_SetShortDimension  (int printInfo, float shortDimension);
   public native static int PrintInfo_SetLongDimension   (int printInfo, float longDimension);
   public native static int PrintInfo_SetSizeUnit        (int printInfo, short sizeUnit);
   public native static int PrintInfo_SetMediaSurface    (int printInfo, short mediaSurface);
   public native static int PrintInfo_SetBackprintMessage(int printInfo, String backprintMessage);

   public native static int ImageDetail_SetImageType       (int imageDetail, short imageType);
   public native static int ImageDetail_SetRegionOfInterest(int imageDetail, double xPosition, double yPosition, double width, double height);
   public native static int ImageDetail_SetRotation        (int imageDetail, float rotation);
   public native static int ImageDetail_GetDigitalInfo     (int imageDetail, Ref digitalInfo);
   public native static int ImageDetail_AddImageRef        (int imageDetail, int image);

   public native static int DigitalInfo_SetFileFormat         (int digitalInfo, short fileFormat);
   public native static int DigitalInfo_SetFileColorSpace     (int digitalInfo, short fileColorSpace);
   public native static int DigitalInfo_SetDigitalSharpening  (int digitalInfo, short digitalSharpening);
   public native static int DigitalInfo_SetDigitizingSource   (int digitalInfo, String digitizingSource);
   public native static int DigitalInfo_SetDigitizedResolution(int digitalInfo, int digitizedResolution);

   public native static int Image_WriteImage(int image, ByteBuffer imageBuffer);

}

