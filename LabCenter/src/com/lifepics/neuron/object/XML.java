/*
 * XML.java
 */

package com.lifepics.neuron.object;

import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * A utility class to hide the details of the standard XML calls.
 */

public class XML {

// --- read helpers ---

   /**
    * Find a child element node with the given name.
    * If there are more than one, the first is used.
    *
    * @return The node, or null if there is none.
    */
   public static Node getElementTry(Node node, String name) {

      NodeList children = node.getChildNodes();
      int len = children.getLength();
      for (int i=0; i<len; i++) {
         Node child = children.item(i);

         if (    child.getNodeType() == Node.ELEMENT_NODE
              && child.getNodeName().equals(name)         ) return child;
      }

      return null;
   }

   /**
    * Find a child element node with the given name.
    * If there are more than one, the first is used.
    */
   public static Node getElement(Node node, String name) throws ValidationException {
      Node result = getElementTry(node,name);
      if (result == null) {
         throw new ValidationException(Text.get(XML.class,"e1a",new Object[] { name }));
      }
      return result;
   }

   /**
    * Find a child element node with the given name and predicate.
    * If there are more than one, the first is used.
    *
    * @param right The right-hand side of the predicate.  Null is a valid value.
    * @return The node, or null if there is none.
    */
   public static Node getElementWithPredicateTry(Node node, String name, String left, String right) throws ValidationException {

      NodeList children = node.getChildNodes();
      int len = children.getLength();
      for (int i=0; i<len; i++) {
         Node child = children.item(i);

         if (    child.getNodeType() == Node.ELEMENT_NODE
              && child.getNodeName().equals(name)
              && Nullable.equals(getNullableText(child,left),right) ) return child;
      }
      // why allow the predicate to be null?  for the right side it isn't so important,
      // but for the left side I think it could be handy, for example if you want to
      // search for a special tag that's only present on one of several XML structures.

      return null;
   }

   /**
    * Find a child element node with the given name and predicate.
    * If there are more than one, the first is used.
    *
    * @param right The right-hand side of the predicate.  Null is a valid value.
    */
   public static Node getElementWithPredicate(Node node, String name, String left, String right) throws ValidationException {
      Node result = getElementWithPredicateTry(node,name,left,right);
      if (result == null) {
         throw new ValidationException(Text.get(XML.class,"e1b",new Object[] { name, left, right }));
      }
      return result;
   }

   /**
    * A minimal implementation of XPath-like behavior.
    */
   public static Node getElementPath(Node node, String path) throws ValidationException {
      String[] s = path.split("/",-1); // -1 to stop weird default behavior
      for (int i=0; i<s.length; i++) {

         // the path elements can now have the form chapter[title="Introduction"],
         // another XPath thing.  don't be super-strict about the parsing, it's not worth the trouble.

         int i1 = s[i].indexOf('[');
         int i2 = s[i].lastIndexOf(']');
         if (i1 == -1 && i2 == -1) { // no predicate

            node = getElement(node,s[i]);

         } else { // predicate

            boolean ok = false;

            if (i1 != -1 && i2 != -1 && i1 < i2 && i2 == s[i].length()-1) {

               String name = s[i].substring(0,i1);
               String rest = s[i].substring(i1+1,i2);

               int i3 = rest.indexOf('=');
               if (i3 != -1) {

                  String left  = rest.substring(0,i3);
                  String right = rest.substring(i3+1);
                  if (right.charAt(0) == '"' && right.charAt(right.length()-1) == '"') {
                     right = right.substring(1,right.length()-1);

                     node = getElementWithPredicate(node,name,left,right);
                     ok = true;
                  }
               }
            }

            if ( ! ok ) throw new ValidationException(Text.get(XML.class,"e17",new Object[] { s[i] }));
         }
      }
      return node;
   }

   /**
    * Find all child element nodes with the given name.
    * Actually they aren't all found at once,
    * they are returned in the form of an iterator.
    */
   public static Iterator getElements(Node node, String name) {
      return new NodeIterator(node,name);
   }

   private static class NodeIterator implements Iterator {

      private NodeList children;
      private int len;
      private int i;
      private String name;

      public NodeIterator(Node node, String name) {
         children = node.getChildNodes();
         len = children.getLength();
         i = 0;
         this.name = name;
      }

      public boolean hasNext() {
         for ( ; i<len; i++) {
            Node child = children.item(i);
            if (    child.getNodeType() == Node.ELEMENT_NODE
                 && child.getNodeName().equals(name)         ) return true;
         }

         return false;
      }

      public Object next() {
         return children.item(i++);
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }
   }

   /**
    * Find a child text node and return its contents.
    * If there are more than one, the first is used.<p>
    *
    * If the element was of the form &lt;x&gt;&lt;/x&gt; or &lt;x/&gt;,
    * there is no text node; in that case the empty string is returned.<p>
    *
    * Due to the structure of XML trees, this is not a common operation.
    * Most of the time you should use {@link #getElementText(Node,String) getElementText} instead.
    */
   public static String getText(Node node) throws ValidationException {
      Node child = getTextImpl(node);
      return (child == null) ? "" : child.getNodeValue();
   }

   /**
    * Find a child text node and return it.  The node must be present:
    * where getText returns an empty string, this throws an exception.
    */
   public static Node getTextNode(Node node) throws ValidationException {
      Node child = getTextImpl(node);
      if (child == null) throw new ValidationException(Text.get(XML.class,"e16"));
      return child;
   }

   private static Node getTextImpl(Node node) throws ValidationException {

      NodeList children = node.getChildNodes();
      int len = children.getLength();

      if (len == 0) return null;

      for (int i=0; i<len; i++) {
         Node child = children.item(i);

         if (child.getNodeType() == Node.TEXT_NODE) return child;
      }

      throw new ValidationException(Text.get(XML.class,"e2"));
   }

   /**
    * Find a child element node with the given name,
    * and return the contents of the text node underneath it.
    */
   public static String getElementText(Node node, String name) throws ValidationException {
      return getText(getElement(node,name));
   }

   /**
    * Same as getElementText except that the result is null if the node isn't there.
    */
   public static String getNullableText(Node node, String name) throws ValidationException {
      Node child = getElementTry(node,name);
      return (child == null) ? null : getText(child);
   }

   /**
    * Get the value of the given attribute.
    *
    * @return The value, or null if there is none.
    */
   public static String getAttributeTry(Node node, String name) throws ValidationException {

      NamedNodeMap attributes = node.getAttributes();
      if (attributes != null) {
         Node attribute = attributes.getNamedItem(name);
         if (attribute != null) {
            return attribute.getNodeValue();
         }
      }

      return null;
   }

   /**
    * Get the value of the given attribute.
    */
   public static String getAttribute(Node node, String name) throws ValidationException {
      String result = getAttributeTry(node,name);
      if (result == null) {
         throw new ValidationException(Text.get(XML.class,"e3",new Object[] { name }));
      }
      return result;
   }

// --- write helpers ---

   private static Document getDocument(Node node) {
      Document doc = node.getOwnerDocument();
      return (doc != null) ? doc : (Document) node; // only the document itself returns null
   }

   /**
    * Create an element node.
    * This is just a simple wrapper, for uniformity.
    */
   public static Node createElement(Node parent, String name) {
      Document doc = getDocument(parent);

      Node node = doc.createElement(name);
      parent.appendChild(node);

      return node;
   }

   /**
    * Create an element node with a text node underneath it.
    */
   public static void createElementText(Node parent, String name, String text) {
      Document doc = getDocument(parent);

      Node node = doc.createElement(name);
      parent.appendChild(node);

      node.appendChild(doc.createTextNode(text));
   }

   /**
    * Same as createElementText except that the node isn't created if the string is null.
    */
   public static void createNullableText(Node parent, String name, String text) {
      if (text != null) createElementText(parent,name,text);
   }

   /**
    * Same as createElementText except that it inserts the node at the front of the list.
    */
   public static void createInitialText(Node parent, String name, String text) {
      Document doc = getDocument(parent);

      Node node = doc.createElement(name);
      parent.insertBefore(node,parent.getFirstChild()); // no-child case works

      node.appendChild(doc.createTextNode(text));
   }

   /**
    * Same as createElementText except that it inserts the node before another one.
    */
   public static void insertElementText(Node parent, Node child, String name, String text) {
      Document doc = getDocument(parent);

      Node node = doc.createElement(name);
      parent.insertBefore(node,child);

      node.appendChild(doc.createTextNode(text));
   }

   /**
    * Same as createElementText except that it drops the node on top of another one.
    */
   public static void replaceElementText(Node parent, Node child, String name, String text) {
      Document doc = getDocument(parent);

      Node node = doc.createElement(name);
      parent.replaceChild(node,child);

      node.appendChild(doc.createTextNode(text));
   }

   /**
    * Set the value of the given attribute.
    */
   public static void setAttribute(Node node, String name, String value) {
      Document doc = getDocument(node);

      Attr attribute = doc.createAttribute(name); // Attr extends Node
      attribute.setValue(value);

      NamedNodeMap attributes = node.getAttributes();
      attributes.setNamedItem(attribute);
   }

// --- list helpers ---

   public static LinkedList getInlineList(Node node, String name) throws ValidationException {
      LinkedList list = new LinkedList();
      Iterator i = XML.getElements(node,name);
      while (i.hasNext()) {
         list.add(XML.getText((Node) i.next()));
      }
      return list;
   }

   public static void createInlineList(Node node, String name, LinkedList list) {
      Iterator i = list.iterator();
      while (i.hasNext()) {
         XML.createElementText(node,name,(String) i.next());
      }
   }

   public static LinkedList getStringList(Node node, String name1, String name2) throws ValidationException {
      return getInlineList(XML.getElement(node,name1),name2);
   }

   public static void createStringList(Node node, String name1, String name2, LinkedList list) {
      createInlineList(XML.createElement(node,name1),name2,list);
   }

// --- formatting ---

   private static Vector indent = new Vector();
   static { indent.add("\n"); }

   /**
    * Create a text node containing a line break and some indentation.
    *
    * @param depth The number of levels of indentation.  Zero is allowed.
    */
   private static Node getIndentation(Document doc, int depth) {

      synchronized (indent) {

         // the vector class itself is thread-safe,
         // but we are going to be making a whole series of calls,
         // and we don't want them to be interleaved with others

         // make sure indent contains enough entries
         while ( ! (depth < indent.size()) ) {
            indent.add( ((String) indent.lastElement()) + "   " );
         }
      }

      return doc.createTextNode((String) indent.get(depth));
   }

   private static void addIndentation(Document doc, Node node, int depth) {

      // this isn't guaranteed to work for all XML,
      // just for the simple kind we actually produce,
      // which is a tree of elements, some with text

      NodeList children = node.getChildNodes();
      int len = children.getLength();

      if (len == 0) return;
      if (len == 1 && isTextNode(children.item(0).getNodeType())) return;

      // do children now, before we add more nodes
      for (int i=0; i<len; i++) {
         addIndentation(doc,children.item(i),depth+1);
      }

      // work backward so that the indices work out nicely
      node.appendChild(getIndentation(doc,depth));
      for (int i=len-1; i>=0; i--) {
         node.insertBefore(getIndentation(doc,depth+1),children.item(i));
      }
   }

   /**
    * Add text nodes containing line breaks and indentation to another node
    * so that it produces output that is easy to read in a text editor.
    * This should be applied to the single child of the document, not to the document itself.
    */
   public static void addIndentation(Node node) {
      Document doc = getDocument(node);
      addIndentation(doc,node,0);
   }

   private static boolean isWhitespace(String s) {
      char[] a = s.toCharArray();
      for (int i=0; i<a.length; i++) {
         char c = a[i];
         if ( ! (c == ' ' || c == '\t' || c == '\r' || c == '\n') ) return false;
      }
      return true;
   }

   private static boolean isTextNode(int type) {
      return (type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE);
   }

   private static boolean isWhitespace(Node node) {
      return (    node.getNodeType() == Node.TEXT_NODE
               && isWhitespace(node.getNodeValue())    );
   }

   public static void removeWhitespace(Node node) {

      // again, this isn't guaranteed to work for all XML.
      // it removes whitespace, not just indentation,
      // which is why I've given it a different name,
      // but among other things it should act as an inverse to addIndentation.

      NodeList children = node.getChildNodes();
      int len = children.getLength();

      if (len == 0) return;
      if (len == 1 && isTextNode(children.item(0).getNodeType())) return;

      // don't even examine element-text pairs,
      // but check any other text node,
      // and if it's all whitespace, remove it.

      // work backward so that the indices work out nicely
      for (int i=len-1; i>=0; i--) {
         Node child = children.item(i);
         if (isWhitespace(child)) {
            node.removeChild(child);
         } else {
            removeWhitespace(child);
         }
      }
   }

// --- top-level helpers ---

   // although DocumentBuilder and Transformer objects can be reused,
   // it is not desirable to cache them, because they are not thread-safe.

   /**
    * Read an XML document from a stream.
    */
   public static Document readStream(InputStream stream) throws IOException {
      try {

         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         DocumentBuilder builder = factory.newDocumentBuilder();

         return builder.parse(stream);

      } catch (ParserConfigurationException e) {
         throw (IOException) new IOException(Text.get(XML.class,"e4")).initCause(e);
      } catch (SAXException e) {
         throw (IOException) new IOException(Text.get(XML.class,"e5")).initCause(e);
      }
   }

   /**
    * Read an XML document from a file.
    */
   public static Document readFile(File file) throws IOException {

      InputStream stream = new FileInputStream(file);
      try {

         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         DocumentBuilder builder = factory.newDocumentBuilder();

         return builder.parse(stream);

      } catch (ParserConfigurationException e) {
         throw (IOException) new IOException(Text.get(XML.class,"e6")).initCause(e);
      } catch (SAXException e) {
         throw (IOException) new IOException(Text.get(XML.class,"e7")).initCause(e);

      } finally {
         stream.close();
         // same idea as in writeFile, but here I was able to reproduce the
         // bug myself.  in Java 1.6, if you try to parse an empty file,
         // the parser leaves some kind of handle open, so you can't delete
         // the file for an unspecified amount of time.
      }
   }

   /**
    * Create a new XML document (prior to writing).
    */
   public static Document createDocument() throws IOException {
      try {

         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         DocumentBuilder builder = factory.newDocumentBuilder();

         return builder.newDocument();

      } catch (ParserConfigurationException e) {
         throw (IOException) new IOException(Text.get(XML.class,"e8")).initCause(e);
      }
   }

   /**
    * Write an XML document to a stream.
    */
   public static void writeStream(OutputStream stream, Document doc) throws IOException {
      writeStream(stream,doc,/* omit = */ false);
   }

   /**
    * Write an XML document to a stream, with options.
    */
   public static void writeStream(OutputStream stream, Document doc, boolean omit) throws IOException {
      try {

         TransformerFactory factory = TransformerFactory.newInstance();
         Transformer transformer = factory.newTransformer();

         if (omit) transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
         // same effect as this XSL
         // <xsl:output method="xml" omit-xml-declaration="yes" indent="no"/>

         DOMSource source = new DOMSource(doc);
         StreamResult result = new StreamResult(stream);

         transformer.transform(source,result);

      } catch (TransformerConfigurationException e) {
         throw (IOException) new IOException(Text.get(XML.class,"e9")).initCause(e);
      } catch (TransformerException e) {
         throw (IOException) new IOException(Text.get(XML.class,"e10")).initCause(e);
      }
   }

   /**
    * Write an XML document to a file.
    */
   public static void writeFile(File file, Document doc) throws IOException {
      writeFile(file,doc,null,null,null);
   }

   /**
    * Write an XML document to a file, possibly transformed.
    */
   public static void writeFile(File file, Document doc, File transform, Map parameters, String doctype) throws IOException {

      OutputStream stream = new FileOutputStream(file);
      try {

         TransformerFactory factory = TransformerFactory.newInstance();
         Transformer transformer = (transform != null) ? factory.newTransformer(new StreamSource(transform))
                                                       : factory.newTransformer();
         loadParameters(transformer,parameters);

         if (doctype != null) {
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,doctype);
         }

         DOMSource source = new DOMSource(doc);
         StreamResult result = new StreamResult(stream);

         transformer.transform(source,result);

      } catch (TransformerConfigurationException e) {
         throw (IOException) new IOException(Text.get(XML.class,"e11")).initCause(e);
      } catch (TransformerException e) {
         throw (IOException) new IOException(Text.get(XML.class,"e12")).initCause(e);

      } finally {
         stream.close(); // I was using plain old "new StreamResult(file)"
         // for a long time, but now I'm changing it, because I saw
         // a case where rollback failed on the XML file, and I bet it was
         // because the file was held open for too long.
      }
   }

   /**
    * @param transform Either an InputStream or a Transformer.
    * @return The transformer object, that can then be passed back in as a transform.
    */
   public static Object transform(Document dest, Document src, Object transform, Map parameters) throws IOException {
      try {

         Transformer transformer;
         if (transform instanceof InputStream) {

            TransformerFactory factory = TransformerFactory.newInstance();
            transformer = factory.newTransformer(new StreamSource((InputStream) transform));

            loadParameters(transformer,parameters);

         } else {
            transformer = (Transformer) transform;
         }

         DOMSource source = new DOMSource(src);
         DOMResult result = new DOMResult(dest);

         transformer.transform(source,result);
         return transformer;

      } catch (TransformerConfigurationException e) {
         throw (IOException) new IOException(Text.get(XML.class,"e14")).initCause(e);
      } catch (TransformerException e) {
         throw (IOException) new IOException(Text.get(XML.class,"e15")).initCause(e);
      }
   }

   private static void loadParameters(Transformer transformer, Map parameters) {
      if (parameters != null) {
         Iterator i = parameters.entrySet().iterator();
         while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            transformer.setParameter((String) entry.getKey(),entry.getValue());
         }
      }
   }

// --- nicer top-level helpers ---

   /**
    * An interface for objects that know how to persist in XML format.
    */
   public interface Persist {

      /**
       * Check that the object's fields are valid.
       * This is called after the object and all subobjects are loaded,
       * and should call validate recursively on the subobjects.
       */
      void validate() throws ValidationException;

      /**
       * Load the object and its subobjects from a node.
       *
       * @return The object itself, as a convenience.
       */
      Object load(Node node) throws ValidationException;

      /**
       * Store the object and its subobjects into a node.
       */
      void store(Node node);

      /**
       * Store the object and its subobjects into a node,
       * but in the form that was used at the given time.
       */
      void tstore(int t, Node node) throws ValidationException;
   }

   /**
    * Load a persistent object from an XML document.
    */
   public static Object loadDoc(Document doc, Persist persist, String name) throws ValidationException {
      Node node = getElement(doc,name);
      persist.load(node); // ignore convenience result
      persist.validate();
      return persist; // convenience
   }

   /**
    * Load a persistent object from a stream in XML format.
    */
   public static Object loadStream(InputStream stream, Persist persist, String name) throws IOException, ValidationException {
      return loadDoc(readStream(stream),persist,name);
   }

   /**
    * Load a persistent object from a file in XML format.
    */
   public static Object loadFile(File file, Persist persist, String name) throws IOException, ValidationException {
      return loadDoc(readFile(file),persist,name);
   }

   /**
    * Store a persistent object to an XML document.
    */
   public static Document storeDoc(Persist persist, String name) throws IOException {
      Document doc = createDocument();
      Node node = createElement(doc,name);
      persist.store(node);
      addIndentation(node);
      return doc;
   }

   /**
    * Store a persistent object to a stream in XML format.
    */
   public static void storeStream(OutputStream stream, Persist persist, String name) throws IOException {
      writeStream(stream,storeDoc(persist,name));
   }

   /**
    * Store a persistent object to a file in XML format.
    */
   public static void storeFile(File file, Persist persist, String name) throws IOException {
      writeFile(file,storeDoc(persist,name));
   }

   public static Document tstoreDoc(int t, Persist persist, String name) throws IOException {
      Document doc = createDocument();
      Node node = createElement(doc,name);
      try {
         persist.tstore(t,node);
      } catch (ValidationException e) {
         throw (IOException) new IOException(Text.get(XML.class,"e18")).initCause(e);
      }
      addIndentation(node);
      return doc;
   }

   public static void tstoreStream(OutputStream stream, int t, Persist persist, String name) throws IOException {
      writeStream(stream,tstoreDoc(t,persist,name));
   }

   public static void tstoreFile(File file, int t, Persist persist, String name) throws IOException {
      writeFile(file,tstoreDoc(t,persist,name));
   }

// --- conversion functions ---

   // in normal operation you never have to call these,
   // the XML framework handles the conversion.
   // these are only for fixing errors in other places.

   // this is also not a complete implementation of XML encoding,
   // I'm just dealing with some common troublesome cases.

   // these are rarely used, efficiency is less of a concern
   // than having clean error-free code.

   private static String[] encoded = { "&amp;","&lt;","&gt;","&apos;","&quot;" };
   private static String[] decoded = { "&",    "<",   ">",   "'",     "\""     };

   public static String replaceAll(String s, String rold, String rnew) {
      int i = 0;
      while (true) {
         i = s.indexOf(rold,i);
         if (i == -1) break;
         s = s.substring(0,i) + rnew + s.substring(i+rold.length());
         i += rnew.length();
      }
      return s;
      // why not use String.replaceAll?  because I don't want to deal with
      // figuring out and testing the regexes for the special characters.
   }

   public static String replaceAll(String s, String[] rold, String[] rnew) {
      for (int i=0; i<rold.length; i++) {
         s = replaceAll(s,rold[i],rnew[i]);
      }
      return s;
   }

   public static String manualEncode(String s) { return replaceAll(s,decoded,encoded); }
   public static String manualDecode(String s) { return replaceAll(s,encoded,decoded); }

// --- diagnostic functions ---

   /**
    * Report node attributes as a series of text lines.
    */
   public static String reportAttributes(Node node) {
      StringBuffer result = new StringBuffer();

      NamedNodeMap attributes = node.getAttributes();
      if (attributes != null) {

         for (int i=0; i<attributes.getLength(); i++) {
            Node attribute = attributes.item(i);

            result.append(attribute.getNodeName());
            result.append("=\"");
            result.append(attribute.getNodeValue());
            result.append("\"\n");
         }
      }

      return result.toString();
   }

}

