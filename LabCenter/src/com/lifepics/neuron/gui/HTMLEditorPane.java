/*
 * HTMLEditorPane.java
 */

package com.lifepics.neuron.gui;

import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

/**
 * A minor adaptation of JEditorPane.
 */

public class HTMLEditorPane extends JEditorPane implements HyperlinkListener {

   public HTMLEditorPane(URL url) {

      setEditorKitForContentType("text/html",new SynchronousKit());
      setEditable(false);
      // addHyperlinkListener(this); // must be read-only for this to work
      setPageSafe(url);
   }

   public void setPageSafe(URL url) {
      try {
         setPage(url);
      } catch (Exception e) {
         // ignore
      }
   }

   private static class SynchronousKit extends HTMLEditorKit {

      public Document createDefaultDocument() {
         HTMLDocument doc = (HTMLDocument) super.createDefaultDocument();
         doc.setAsynchronousLoadPriority(-1); // means load synchronously
         return doc;
      }
   }

   public void hyperlinkUpdate(HyperlinkEvent e) {

      // adapted from the API documentation for JEditorPane;
      // the documentation for processHTML... was also good.

      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
         if (    e instanceof HTMLFrameHyperlinkEvent
              && ! ((HTMLFrameHyperlinkEvent) e).getTarget().equals("_top") ) {
            ((HTMLDocument) getDocument()).processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) e);
         } else {
            setPageSafe(e.getURL());
         }
      }
   }

}

