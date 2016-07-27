/*
 * Exif.java
 */

package com.lifepics.neuron.misc;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.XML;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;

import org.w3c.dom.Node;

/**
 * A utility class for reading Exif data ... details follow.
 *
 * Background:  An uncompressed Exif file is a TIFF file with
 * some extra fields.  A compressed Exif file is a JPEG file
 * with an Exif APP1 segment, the contents of which are in
 * TIFF format.  The thumbnail there *may* be uncompressed,
 * according to the spec, but the ones I've seen have always
 * been compressed, which means they're back in JPEG format.
 *
 * As of version 1.1, the jai_imageio library is capable of
 * reading Exif data.  With a TIFF file, you can read image 1
 * to get the thumbnail (just as in version 1.0); with a JPEG
 * file you can apparently just ask for the thumbnail and get
 * it, no matter whether it's stored in JFIF or Exif format.
 * (In version 1.0, only the JFIF thumbnails were supported.)
 *
 * So, the trouble is, when you're using 1.1 to read a TIFF
 * file (or APP1 segment), and the thumbnail is compressed,
 * there's some kind of hard-wired dependency on the native
 * JPEG reader, which we're not using.  So, it conks out;
 * you can't even read the metadata, I think because the reader
 * wants to grab the JPEG fields and present them as TIFF.
 *
 * Here's the actual error you get in that case:
 * java.lang.NullPointerException
 * at com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader.initializeFromMetadata(TIFFImageReader.java:602)
 * at com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader.seekToImage(TIFFImageReader.java:312)
 * at com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader.getImageMetadata(TIFFImageReader.java:828)
 *
 * The 1.0 reader, on the other hand, just isn't equipped
 * to deal with Exif-TIFF data.  If you try to read the
 * non-thumbnail part, the 0th IFD that has the Exif data,
 * it conks out; and if you try to read the 1st IFD, it
 * conks in the same way, either because there's no image
 * data there either or because it can't get over the 0th
 * IFD being empty.
 *
 * Here's the actual error you get in that case:
 * java.lang.NullPointerException
 * at com.sun.media.imageioimpl.plugins.tiff.TIFFImageMetadata.getStandardChromaNode(TIFFImageMetadata.java:233)
 * at javax.imageio.metadata.IIOMetadata.getStandardTree(IIOMetadata.java:664)
 * at com.sun.media.imageioimpl.plugins.tiff.TIFFImageMetadata.getAsTree(TIFFImageMetadata.java:167)
 *
 * Anyway, the point is, there's no nice way to get at
 * the Exif data without also using the native libraries,
 * and we don't want to do that because one of our main
 * goals is to make something that will work on Macs too.
 *
 * So, instead, we use a quick but not too dirty solution:
 * For TIFFs we just read image 1, no problem.
 * For JPEGs, we grab the metadata, find the Exif APP1 segment,
 * run through it with a little TIFF parser, and pull just the
 * information we need to find the JPEG-compressed part.
 * Fortunately, the TIFF format is pretty easy to parse!
 *
 * I'm going to try to build the parser to be flexible, but
 * in practice it's always going to be reading from a byte
 * array that's completely in memory, not from a TIFF file.
 */

public class Exif {

// --- 1. find Exif APP1 ---

   private static final byte[] exifHeader = new byte[] { 'E', 'x', 'i', 'f',  0,   0 };

   private static boolean startsWith(byte[] b, byte[] header) {
      if (b.length < header.length) return false;
      for (int i=0; i<header.length; i++) {
         if (b[i] != header[i]) return false;
      }
      return true;
   }

   /**
    * Try to get Exif data from a JPEG reader.  The result is never null;
    * if there's any problem, an exception is used to report the details.
    */
   private static byte[] getExifData(ImageReader reader, int imageIndex) throws Exception {

   // (a) get metadata object

      // JPEG doesn't have any stream metadata, as far as I've seen
      IIOMetadata meta = reader.getImageMetadata(imageIndex); // can produce IOException
      if (meta == null) throw new Exception(Text.get(Exif.class,"e1"));

   // (b) get in tree form

      Node node;
      try {
         node = meta.getAsTree("javax_imageio_jpeg_image_1.0");
      } catch (IllegalArgumentException e) {
         throw new Exception(Text.get(Exif.class,"e2"));
      }
      // getAsTree doesn't return null, just throws exceptions.
      //
      // the string above is the native metadata format name,
      // but we can't replace it with  getNativeMetadataFormatName
      // because then if the native format changed in the future,
      // we'd get back some weird structure we wouldn't understand.

   // (c) scan for Exif part

      // here we shouldn't throw exceptions on failure, because
      // we're just looking at one segment of many.
      // as far as I know, every node *should* have a MarkerTag entry
      // and every APP1 should have a user object that's a byte array,
      // but if we can find what we want, it doesn't matter what else
      // is out there.

      // on the other hand, I'm willing to count on the fact that
      // all nodes are instances of IIOMetadataNode

      Node sequence = XML.getElement(node,"markerSequence");

      Iterator i = XML.getElements(sequence,"unknown");
      while (i.hasNext()) {
         Node child = (Node) i.next();

         // is it an APP1 segment?  (reverse order to handle null)
         if ( ! "225".equals(XML.getAttributeTry(child,"MarkerTag")) ) continue;

         // is there a byte array?  (should be, but don't count on it)
         Object o = ((IIOMetadataNode) child).getUserObject();
         if ( ! (o instanceof byte[]) ) continue; // catches null too

         // does it start with the Exif header?
         byte[] b = (byte[]) o;
         if ( ! startsWith(b,exifHeader) ) continue;

         return b; // got it!
      }

      throw new Exception(Text.get(Exif.class,"e3")); // not found
   }

// --- 2. TIFF parser ---

   // Exif offsets are 32-bit unsigned integers, so in theory they can
   // exceed the size of Java int ... that's why we use longs here.
   // they're no use to us now, because arrays are limited to int size,
   // but if we ever made a direct file reader, it could matter.

   private interface Source {

      /**
       * Get the current offset in the file (or array).
       */
      long fpos() throws IOException;

      /**
       * Seek to the given offset in the file (or array).
       */
      void seek(long pos) throws IOException;

      /**
       * Read one unsigned byte from the file (or array),
       * and throw an exception if EOF is reached.
       */
      int read() throws IOException;
   }

   private static class ArraySource implements Source {

      private byte[] b;
      private int base;
      private int pos;

      public ArraySource(byte[] b, int base) throws IOException {
         this.b = b;
         if (base < 0 || base > b.length) throw new IOException(Text.get(Exif.class,"e4"));
         this.base = base;
         this.pos  = base;
         // base = b.length isn't useful, but allow it anyway
      }

      public long fpos() { return pos-base; }

      public void seek(long pos) throws IOException {
         pos += base;
         if (pos < base || pos > b.length) throw new IOException(Text.get(Exif.class,"e5"));
         this.pos = (int) pos;
         // cast is OK because b.length fits in an int.
         // you can seek to EOF but not read there,
         // kind of arbitrary but it makes sense to me.
      }

      public int read() throws IOException {
         if (pos == b.length) throw new IOException(Text.get(Exif.class,"e6"));
         return (b[pos++] & 0xFF); // have to strip off the sign extension
      }
   }

   private interface Reader {

      long fpos() throws IOException;
      void seek(long pos) throws IOException;

      int  read8 () throws IOException; //  8-bit unsigned integer
      int  read16() throws IOException; // 16-bit unsigned integer
      long read32() throws IOException; // 32-bit unsigned integer, not quite Java int
   }

   private static class BigEndianReader implements Reader {

      private Source s;
      public BigEndianReader(Source s) { this.s = s; }

      public long fpos() throws IOException { return s.fpos(); }
      public void seek(long pos) throws IOException { s.seek(pos); }

      public int read8() throws IOException { return s.read(); }

      public int read16() throws IOException {
         int b1 = s.read();
         int b2 = s.read();
         return (b1 << 8) + b2;
      }

      public long read32() throws IOException {
         int b1 = s.read();
         int b2 = s.read();
         int b3 = s.read();
         int b4 = s.read();
         return ( ((long) b1) << 24) + (b2 << 16) + (b3 << 8) + b4;
      }
   }

   private static class LittleEndianReader implements Reader {

      private Source s;
      public LittleEndianReader(Source s) { this.s = s; }

      public long fpos() throws IOException { return s.fpos(); }
      public void seek(long pos) throws IOException { s.seek(pos); }

      public int read8() throws IOException { return s.read(); }

      public int read16() throws IOException {
         int b1 = s.read();
         int b2 = s.read();
         return (b2 << 8) + b1;
      }

      public long read32() throws IOException {
         int b1 = s.read();
         int b2 = s.read();
         int b3 = s.read();
         int b4 = s.read();
         return ( ((long) b4) << 24) + (b3 << 16) + (b2 << 8) + b1;
      }
   }

   private static class Entry {
      public int tag;
      public int type;
      public long count;
      public long off; // the offset to the original location in the IFD
   }

   private static class IFD {
      public Entry[] entry;
      public long offNextIFD;
   }

   private static IFD readIFD(Reader r, long off) throws IOException {
      r.seek(off);
      int n = r.read16(); // note, unsigned 16-bit integer can't be negative

      IFD ifd = new IFD();
      ifd.entry = new Entry[n];

      for (int i=0; i<n; i++) {
         Entry e = new Entry();

         e.tag   = r.read16();
         e.type  = r.read16();
         e.count = r.read32();

         e.off = r.fpos();
         r.read32(); // skip for now; see note in getValue(Entry)

         ifd.entry[i] = e; // no validation, just read and store
      }

      ifd.offNextIFD = r.read32();

      return ifd;
   }

   private static long skipIFD(Reader r, long off) throws IOException {
      r.seek(off);
      int n = r.read16(); // note, unsigned 16-bit integer can't be negative

      r.seek(off+2+n*12);

      return r.read32();
   }

   private static Reader readHeader(Source s) throws IOException {

      int b1 = s.read();
      int b2 = s.read();

      Reader r;
      if        (b1 == 'M' && b2 == b1) { // Motorola
         r = new BigEndianReader(s);
      } else if (b1 == 'I' && b2 == b1) { // Intel
         r = new LittleEndianReader(s);
      } else {
         throw new IOException(Text.get(Exif.class,"e7"));
      }

      if (r.read16() != 42) throw new IOException(Text.get(Exif.class,"e8")); // magic number

      return r;
      // it'd be nice to read and return the first IFD offset,
      // but it's not worth defining a structure to do it.
      // anyway, the caller can just call read32, not too hard.
   }

// --- 3. find JPEG thumbnail ---

   private static final int TYPE_BYTE  = 1;
   private static final int TYPE_SHORT = 3;
   private static final int TYPE_LONG  = 4;

   private static final int TYPE_SBYTE  = 6;
   private static final int TYPE_SSHORT = 8;
   private static final int TYPE_SLONG  = 9;

   /**
    * Get the numerical value of an entry, no matter what type it is.
    */
   private static long getValue(Reader r, Entry e) throws IOException {

      if (e.count != 1) throw new IOException(Text.get(Exif.class,"e9"));

      r.seek(e.off);
      //
      // in general, the value at e.off may be another offset to the real value,
      // but all our types fit into four bytes, so we can just go read.
      // if the value is less than four bytes long, it's left-aligned regardless
      // of the endian-ness, so we can't just do a read32 and deal with it later,
      // we have to actually seek back and re-read.
      //
      // allowing signed values isn't standard, I think, but it's harmless.

      switch (e.type) {

      case TYPE_BYTE:   return r.read8 ();
      case TYPE_SHORT:  return r.read16();
      case TYPE_LONG:   return r.read32();

      case TYPE_SBYTE:   return (byte ) r.read8 (); // truncate and sign-extend
      case TYPE_SSHORT:  return (short) r.read16();
      case TYPE_SLONG:   return (int  ) r.read32();

      default:  throw new IOException(Text.get(Exif.class,"e10"));
      }
   }

   private static class Thumbnail {
      public long off;
      public long len;
   }

   private static final int TAG_COMPRESSION = 259;
   private static final int TAG_JPEG_OFFSET = 513;
   private static final int TAG_JPEG_LENGTH = 514;

   private static final int COMPRESSION_JPEG = 6;

   /**
    * @param r A reader fresh from the readHeader call (i.e., at offset 4).
    */
   private static Thumbnail getThumbnail(Reader r) throws IOException {

      long off = r.read32();
      if (off == 0) throw new IOException(Text.get(Exif.class,"e11"));

      off = skipIFD(r,off);
      if (off == 0) throw new IOException(Text.get(Exif.class,"e12"));

      Thumbnail t = new Thumbnail();

      boolean foundCmp = false;
      boolean foundOff = false;
      boolean foundLen = false;

      IFD ifd = readIFD(r,off);
      for (int i=0; i<ifd.entry.length; i++) {
         Entry e = ifd.entry[i];

         switch (e.tag) {

         case TAG_COMPRESSION:
            if (getValue(r,e) != COMPRESSION_JPEG) throw new IOException(Text.get(Exif.class,"e13"));
            foundCmp = true;
            break;

         case TAG_JPEG_OFFSET:
            t.off = getValue(r,e);
            foundOff = true;
            break;

         case TAG_JPEG_LENGTH:
            t.len = getValue(r,e);
            foundLen = true;
            break;

         // the rest we don't care about
         }
      }

      if ( ! (foundCmp && foundOff && foundLen) ) throw new IOException(Text.get(Exif.class,"e14"));
      // if there are more than one of any of these,
      // we'll just use the last value .. not worth catching

      return t; // let caller validate offset and length
   }

// --- 4. put it all together ---

   public static ImageInputStream getExifThumbnail(ImageReader reader, int imageIndex) throws Exception {

      byte[] b = getExifData(reader,imageIndex);
      Source s = new ArraySource(b,/* base = */ exifHeader.length);
      Reader r = readHeader(s);
      Thumbnail t = getThumbnail(r);
      t.off += exifHeader.length; // awkward, but just do it here

      if (t.len < 0) throw new IOException(Text.get(Exif.class,"e15"));
      if (t.off < 0 || t.off+t.len > b.length) throw new IOException(Text.get(Exif.class,"e16"));

      return new ByteArrayImageInputStream(b,(int) t.off,(int) t.len);
      // as earlier, the int casts are OK because the offset and length
      // are both smaller than b.length.
      // note that t.off+t.len can't overflow, both are 32 bits maximum
   }

   // in the end, the only thing that's unsatisfying about this code
   // is that it doesn't distinguish "error condition" from
   // "non-error condition that means there's no Exif thumbnail".
   // but, they amount to the same thing for GUI display, so we
   // can go with this for now.

// --- 5. image input stream ---

   // why do we even need this?  why not just return InputStream?
   // the trouble is, there's no ImageInputStream that can operate
   // on a byte array without caching it to memory or disk ...
   // and that's stupid, because we already have it all in memory.

   // it wouldn't be too hard to go back and replace Stream/Reader
   // with ByteArrayImageInputStream, but I like the simplicity of
   // the existing code, and it works, so why mess with it?

   // cloned from FileImageInputStream, should be a good model.
   // there's obviously a fair amount of ArraySource here, too.

   private static class ByteArrayImageInputStream extends ImageInputStreamImpl {

      private byte[] b;
      private int base;
      private int pos;
      private int end;

      public ByteArrayImageInputStream(byte[] b, int base, int len) {
         this.b = b;
         // caller already validated everything, don't worry about it
         this.base = base;
         this.pos  = base;
         this.end  = base+len;
      }

      public int read() throws IOException {
         checkClosed();
         bitOffset = 0;
         if (pos == end) {
            return -1;
         } else {
            streamPos++;
            return (b[pos++] & 0xFF);
         }
      }

      public int read(byte[] b, int off, int len) throws IOException {
         checkClosed();
         bitOffset = 0;
         // assume valid arguments (0 <= off <= off+len <= b.length)
         int avail = end-pos;
         if (len > avail) len = avail;
         if (len == 0) {
            return -1;
         } else {
            streamPos += len;
            System.arraycopy(this.b,this.pos,b,off,len);
            pos += len;
            return len;
         }
      }

      public long length() {
         try {
            checkClosed();
            return end-base;
         } catch (IOException e) {
            return -1L;
         }
      }

      public void seek(long pos) throws IOException {
         checkClosed();
         if (pos < flushedPos) {
            throw new IndexOutOfBoundsException("pos < flushedPos!"); // cloned constant
         }
         bitOffset = 0;
         pos += base;
         if (pos < base || pos > end) throw new IOException(Text.get(Exif.class,"e17"));
         this.pos = (int) pos;
         streamPos = this.pos-base;
      }
   }

}

