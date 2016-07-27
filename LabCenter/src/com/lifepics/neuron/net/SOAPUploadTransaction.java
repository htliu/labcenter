/*
 * SOAPUploadTransaction.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.object.XML;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Another HTTP POST transaction class.  See package notes for comparison.
 * Subclasses implement describe, getFixedURL, and a bunch of functions
 * defined below.  The getParameters function should be left unimplemented.<p>
 *
 * There's a lot of potential for generalizing and factoring, but I won't
 * get into it until necessary.  See the data load classes LoadSnapfish
 * and LoadPhotoChannel for other existing uses of SOAP.  There aren't any
 * others in LabCenter itself.
 */

public abstract class SOAPUploadTransaction extends HTTPTransaction {

// --- more subclass hooks ---

   /**
    * Get the SOAP namespace.
    */
   protected abstract String getNamespace();

   /**
    * Get the SOAP action.
    */
   protected abstract String getAction();

   /**
    * If an action has variants, in most places you use the long name,
    * but to parse the nested response you have to use the short name.
    */
   protected String getActionShort() { return getAction(); }

   /**
    * Get the name  of the XML tag that should be replaced with the file.
    */
   protected abstract String getFileTagName();

   /**
    * Get the value of the XML tag that should be replaced with the file.
    * This isn't a subclass hook, just a nice way to define the constant.
    */
   protected String getFileTagValue() { return "DATA"; }

   /**
    * Get the file to be uploaded.
    */
   protected abstract File getFile();

   /**
    * Get a callback to be used for copy progress reporting.
    */
   protected FileUtil.Callback getCallback() { return null; }

   /**
    * Fill in the SOAP request node.
    */
   protected abstract void sendImpl(Node node) throws Exception;

   /**
    * Handle the SOAP response node.
    */
   protected abstract void recvImpl(Node node) throws Exception;

// --- implementation ---

   /**
    * Send the header to the server, by calling http.Get or http.Post.
    */
   protected HTTPResponse send(HTTPConnection http, String urlFile, PauseCallback callback) throws Exception {

      String namespace = getNamespace();
      String action = getAction();

   // build XML

      Document doc = XML.createDocument();

      Node env = XML.createElement(doc,"soap:Envelope");
      XML.setAttribute(env,"xmlns:soap","http://schemas.xmlsoap.org/soap/envelope/");
      XML.setAttribute(env,"xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
      XML.setAttribute(env,"xmlns:xsd", "http://www.w3.org/2001/XMLSchema");

      Node body = XML.createElement(env,"soap:Body");

      Node node = XML.createElement(body,action);
      XML.setAttribute(node,"xmlns",namespace);

      sendImpl(node);

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      XML.writeStream(buffer,doc);
      String xml = buffer.toString();

   // split around file tag

      // this seems kind of nasty, but actually it's not that bad ...
      // we'll never see the target string in data because it
      // contains angle brackets that would be escaped, and we check
      // for the case where there's accidentally more than one copy.

      // the tag needs to have a value other than an empty string
      // so that it won't turn into the <name/> or <name /> forms
      // in different versions of Java.

      String name  = getFileTagName ();
      String value = getFileTagValue();

      String left  = "<"  + name + ">";
      String right = "</" + name + ">";

      String tag = left + value + right;

      int i1 = xml.indexOf(tag);
      if (i1 == -1) throw new Exception(Text.get(SOAPUploadTransaction.class,"e1"));

      int i2 = xml.indexOf(tag,i1+1);
      if (i2 != -1) throw new Exception(Text.get(SOAPUploadTransaction.class,"e2"));

      int j1 = i1 + left .length();
      int j2 = j1 + value.length();

      byte[] blob1 = xml.substring(0,j1).getBytes();
      byte[] blob2 = xml.substring(j2)  .getBytes();

   // send

      NVPair[] headers = new NVPair[2];
      headers[0] = new NVPair("Content-Type","text/xml; charset=utf-8");
      headers[1] = new NVPair("SOAPAction","\"" + namespace + action + "\""); // yes, quotes

      return sendStream(http,urlFile,callback,getFile(),getCallback(),
                        headers,blob1,blob2,/* base64 = */ true);
   }

   /**
    * Receive data from the server.
    */
   protected boolean receive(InputStream inputStream) throws Exception {

      // this is probably specific to the .NET implementation of SOAP.
      // also the OPEN API doesn't use SOAP faults, just custom error
      // return values that are handled in recvImpl.

      Document doc = XML.readStream(inputStream);

      Node env  = XML.getElement(doc,"soap:Envelope");
      Node body = XML.getElement(env,"soap:Body");

      String action = getAction();

      Node node1 = XML.getElement(body, action + "Response");
      Node node2 = XML.getElement(node1,action + "Result"  );

      String actionShort = getActionShort();
      Node node3 = (actionShort != null) ? XML.getElement(node2,actionShort) : node2;
      // apparently the extra layer is an OPEN thing, not any standard SOAP behavior

      recvImpl(node3);

      return true;
   }

}

