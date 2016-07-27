/*
 * ModernStyle.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;

import java.util.HashSet;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

/**
 * The modern style.
 */

public class ModernStyle implements Style.Interface {

// --- branding ---

   // at present, this is the only member variable of the class
   private Brand brand;
   public ModernStyle(Brand brand) { this.brand = brand; }

// --- palette ---

   private static final Color lighterGray = new Color(204,204,204);
   private static final Color darkerGray  = new Color(153,153,153);
   private static final Color darkestGray = new Color(102,102,102);
   private static final Color metalGray   = Color.gray;
   private static final Color cornflower  = new Color(  0,102,255);
   private static final Color happyGreen  = new Color(  0,196,  0);
   private static final Color alertColor  = new Color(255, 51,  0);

// --- font utilities ---

   private static class FontEntry {

      public String name;
      public int style;
      public int size;

      public FontEntry(String name, int style, int size) {
         this.name = name;
         this.style = style;
         this.size = size;
      }
   }

   private static HashSet fontNames = null;

   private static Font matchFont(FontEntry[] entries) {

      // build hash set if necessary
      if (fontNames == null) {
         fontNames = new HashSet();
         String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
         for (int i=0; i<names.length; i++) fontNames.add(names[i]);
      }

      for (int i=0; i<entries.length; i++) {
         FontEntry e = entries[i];
         if (fontNames.contains(e.name)) return new Font(e.name,e.style,e.size);
      }
      return null;
   }

   private static void applyFont(Component c, Font f) {
      if (f != null) c.setFont(f);
      // null means we didn't find a match, go with original font
   }

   private static Font squareFont = matchFont(new FontEntry[] {
      new FontEntry("BankGothic Md BT", Font.PLAIN,16),
      new FontEntry("CopprplGoth Bd BT",Font.PLAIN,16),
      new FontEntry("Courier New",      Font.PLAIN,14)  });

   private static Font smallSquareFont = matchFont(new FontEntry[] {
      new FontEntry("BankGothic Md BT", Font.PLAIN,14),
      new FontEntry("CopprplGoth Bd BT",Font.PLAIN,14),
      new FontEntry("Courier New",      Font.PLAIN,12)  });

   private static Font smallerSquareFont = matchFont(new FontEntry[] {
      new FontEntry("BankGothic Md BT", Font.PLAIN,12),
      new FontEntry("CopprplGoth Bd BT",Font.PLAIN,12),
      new FontEntry("Courier New",      Font.PLAIN,10)  });

   private static Font tinySquareFont = matchFont(new FontEntry[] {
      new FontEntry("BankGothic Md BT", Font.PLAIN,10),
      new FontEntry("CopprplGoth Bd BT",Font.PLAIN,10),
      new FontEntry("Courier New",      Font.PLAIN, 8)  });

   private static Font strongFont = matchFont(new FontEntry[] {
      new FontEntry("Zurich Ex BT",        Font.BOLD,20),
      new FontEntry("Microsoft Sans Serif",Font.BOLD,20)  });

   private static Font mediumStrongFont = matchFont(new FontEntry[] {
      new FontEntry("Zurich Ex BT",        Font.BOLD,16),
      new FontEntry("Microsoft Sans Serif",Font.BOLD,16)  });

   private static Font smallStrongFont = matchFont(new FontEntry[] {
      new FontEntry("Zurich Ex BT",        Font.BOLD,10),
      new FontEntry("Microsoft Sans Serif",Font.BOLD,10)  });

   private static Font counterFont = matchFont(new FontEntry[] {
      new FontEntry("Impact",Font.PLAIN,48),
      new FontEntry("Arial", Font.BOLD, 48)  });

   private static Font buttonFont = matchFont(new FontEntry[] {
      new FontEntry("Arial", Font.BOLD, 11)  });

// --- underlay ---

   private static class Underlay extends JLabel {

      private String underlay;
      public Underlay(String underlay) { this.underlay = underlay; }

      protected void paintComponent(Graphics g) {
         Font  saveFont  = g.getFont ();
         Color saveColor = g.getColor();

         if (mediumStrongFont != null) g.setFont(mediumStrongFont);
         g.setColor(darkestGray);

         FontMetrics fm = g.getFontMetrics();
         int ascent = fm.getAscent();
         int descent = fm.getDescent();

         Dimension size = getSize();

         int tx = size.width * 2 / 5; // arbitrary point, even if text clips
         int ty = (size.height + ascent - descent) / 2; // H/2 - (A+D)/2 + A

         g.drawString(underlay,tx,ty);

         g.setColor(saveColor);
         g.setFont (saveFont );
         super.paintComponent(g); // do last so it goes on top
      }
   }

// --- quad utilities ---

   // I don't want to write these, but drawRoundRect, drawArc, and fillArc
   // are not producing consistent results, and that's kind of important.

   // in all cases, the drawing goes in a rectangle with outer corner (x,y)
   // and having width and height equal to the arc radius, in pixels.

   // colors are not preserved, caller is responsible for that.

   // actually, you know what, it's not worth writing an algorithm, just
   // calculate the arcs by hand, since edge is related to radius anyway.

   private static final class Arc {

      public int[] steps;
      public int edge;
      public int radius;

      public Arc(int[] steps) {
         this.steps = steps;

         edge   = steps.length;
         radius = steps.length;
         for (int i=0; i<steps.length; i++) radius += steps[i];
      }
   }

   private static void quadUL(Graphics g, int x, int y, Arc a, Color c1, Color c2, Color c3) {
      g.translate(x,y);

   // exterior

      g.setColor(c1);

      if (c3 != null) { // yes, c3 ... it means don't draw on the interior

         g.fillRect(0,0,a.edge,a.radius);
         g.fillRect(a.edge,0,a.radius-a.edge,a.edge);

      } else {

         int pos = a.radius;
         for (int i=0; i<a.steps.length; i++) {
            pos -= a.steps[i];
            g.drawLine(i,0,i,pos-1);
            g.drawLine(0,i,pos-1,i);
         }
      }

   // arc border

      g.setColor(c2);

      int end = a.radius;
      for (int i=0; i<a.steps.length; i++) {
         int start = end - a.steps[i];
         g.drawLine(i,start,i,end-1);
         g.drawLine(start,i,end-1,i);
         end = start;
      }

   // arc fill

      if (c3 != null) {
         g.setColor(c3);

         int pos = a.radius;
         for (int i=1; i<a.steps.length; i++) {
            pos -= a.steps[i-1];
            g.drawLine(i,pos,i,a.radius-1);
            g.drawLine(pos,i,a.radius-1,i);
         }
      }

      g.translate(-x,-y);
   }

   private static void quadUR(Graphics g, int x, int y, Arc a, Color c1, Color c2, Color c3) {
      g.translate(x,y);
      g.setColor(c1);
      if (c3 != null) {
         g.fillRect(-a.edge,0,a.edge,a.radius);
         g.fillRect(-a.radius,0,a.radius-a.edge,a.edge);
      } else {
         int pos = a.radius;
         for (int i=0; i<a.steps.length; i++) {
            pos -= a.steps[i];
            g.drawLine(-i-1,0,-i-1,pos-1);
            g.drawLine(-1,i,-pos,i);
         }
      }

      g.setColor(c2);
      int end = a.radius;
      for (int i=0; i<a.steps.length; i++) {
         int start = end - a.steps[i];
         g.drawLine(-i-1,start,-i-1,end-1);
         g.drawLine(-end,i,-start-1,i);
         end = start;
      }

      if (c3 != null) {
         g.setColor(c3);
         int pos = a.radius;
         for (int i=1; i<a.steps.length; i++) {
            pos -= a.steps[i-1];
            g.drawLine(-i-1,pos,-i-1,a.radius-1);
            g.drawLine(-a.radius,i,-pos-1,i);
         }
      }

      g.translate(-x,-y);
   }

   private static void quadLL(Graphics g, int x, int y, Arc a, Color c1, Color c2, Color c3) {
      g.translate(x,y);
      g.setColor(c1);
      if (c3 != null) {
         g.fillRect(0,-a.radius,a.edge,a.radius);
         g.fillRect(a.edge,-a.edge,a.radius-a.edge,a.edge);
      } else {
         int pos = a.radius;
         for (int i=0; i<a.steps.length; i++) {
            pos -= a.steps[i];
            g.drawLine(i,-1,i,-pos);
            g.drawLine(0,-i-1,pos-1,-i-1);
         }
      }

      g.setColor(c2);
      int end = a.radius;
      for (int i=0; i<a.steps.length; i++) {
         int start = end - a.steps[i];
         g.drawLine(i,-end,i,-start-1);
         g.drawLine(start,-i-1,end-1,-i-1);
         end = start;
      }

      if (c3 != null) {
         g.setColor(c3);
         int pos = a.radius;
         for (int i=1; i<a.steps.length; i++) {
            pos -= a.steps[i-1];
            g.drawLine(i,-a.radius,i,-pos-1);
            g.drawLine(pos,-i-1,a.radius-1,-i-1);
         }
      }

      g.translate(-x,-y);
   }

   private static void quadLR(Graphics g, int x, int y, Arc a, Color c1, Color c2, Color c3) {
      g.translate(x,y);
      g.setColor(c1);
      if (c3 != null) {
         g.fillRect(-a.edge,-a.radius,a.edge,a.radius);
         g.fillRect(-a.radius,-a.edge,a.radius-a.edge,a.edge);
      } else {
         int pos = a.radius;
         for (int i=0; i<a.steps.length; i++) {
            pos -= a.steps[i];
            g.drawLine(-i-1,-1,-i-1,-pos);
            g.drawLine(-1,-i-1,-pos,-i-1);
         }
      }

      g.setColor(c2);
      int end = a.radius;
      for (int i=0; i<a.steps.length; i++) {
         int start = end - a.steps[i];
         g.drawLine(-i-1,-end,-i-1,-start-1);
         g.drawLine(-end,-i-1,-start-1,-i-1);
         end = start;
      }

      if (c3 != null) {
         g.setColor(c3);
         int pos = a.radius;
         for (int i=1; i<a.steps.length; i++) {
            pos -= a.steps[i-1];
            g.drawLine(-i-1,-a.radius,-i-1,-pos-1);
            g.drawLine(-a.radius,-i-1,-pos-1,-i-1);
         }
      }

      g.translate(-x,-y);
   }

// --- borders ---

   private static final int frameHeight   = 24; // roughly twice smallSquareFont size
   private static final int compactHeight = 20;

   private static final Arc arc3 = new Arc(new int[] { 3, 3, 1 });
   private static final Arc arc4 = new Arc(new int[] { 4, 3, 2, 1 });

   private static class RoundBorder implements Border {

      protected Arc arc;
      protected Color exterior;

      public RoundBorder(Arc arc, Color exterior) {
         this.arc = arc;
         this.exterior = exterior;
      }

      public void setExterior(Color exterior) { this.exterior = exterior; }

      public Insets getBorderInsets(Component c) {
         return new Insets(arc.edge,arc.edge,arc.edge,arc.edge);
      }

      public boolean isBorderOpaque() { return true; }

      public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
         Font  saveFont  = g.getFont ();
         Color saveColor = g.getColor();
         g.translate(x,y);

         paintSimple(c,g,width,height);

         g.translate(-x,-y);
         g.setColor(saveColor);
         g.setFont (saveFont );
      }

      protected Color getULInterior(Component c) { return c.getBackground(); }
      protected Color getURInterior(Component c) { return c.getBackground(); }
      protected Color getUpInterior(Component c) { return c.getBackground(); }
      protected Color getLLInterior(Component c) { return c.getBackground(); }
      protected Color getLfInterior(Component c) { return c.getBackground(); }

      protected void paintSimple(Component c, Graphics g, int width, int height) {

         quadUL(g,    0,     0,arc,exterior,darkerGray,getULInterior(c));
         quadUR(g,width,     0,arc,exterior,darkerGray,getURInterior(c));
         quadLL(g,    0,height,arc,exterior,darkerGray,getLLInterior(c));
         quadLR(g,width,height,arc,exterior,darkerGray,c.getBackground());

         g.setColor(darkerGray);
         g.drawLine(arc.radius,       0,width-arc.radius-1,       0);
         g.drawLine(arc.radius,height-1,width-arc.radius-1,height-1);
         g.drawLine(      0,arc.radius,      0,height-arc.radius-1);
         g.drawLine(width-1,arc.radius,width-1,height-arc.radius-1);

         g.setColor(getUpInterior(c));
         g.fillRect(arc.radius,              1,width-2*arc.radius,arc.edge-1);
         g.setColor(c.getBackground());
         g.fillRect(arc.radius,height-arc.edge,width-2*arc.radius,arc.edge-1);
         g.setColor(getLfInterior(c));
         g.fillRect(             1,arc.radius,arc.edge-1,height-2*arc.radius);
         g.setColor(c.getBackground());
         g.fillRect(width-arc.edge,arc.radius,arc.edge-1,height-2*arc.radius);
      }
   }

   private static void paintTitle(Graphics g, String title, int width, int edge) {

      g.setColor(Color.white);
      int ascent = 0, descent = 0, twidth = 0;

      Font[] font = { smallSquareFont, smallerSquareFont, tinySquareFont };
      for (int i=0; i<font.length; i++) {
         g.setFont(font[i]);

         FontMetrics fm = g.getFontMetrics();
         ascent = fm.getAscent();
         descent = fm.getDescent();
         twidth = fm.stringWidth(title);

         if (twidth <= width-2*edge) break; // not a perfect rule, but good enough
      }

      int tx = (width - twidth) / 2;                 // W/2 - TW/2
      int ty = (frameHeight + ascent - descent) / 2; // H/2 - (A+D)/2 + A

      g.drawString(title,tx,ty);
   }

   private static class FrameBorder extends RoundBorder {

      private Color frame;
      private String title;

      public FrameBorder(Arc arc, Color exterior, Color frame, String title) {
         super(arc,exterior);
         this.frame = frame;
         this.title = title;
      }

      public Insets getBorderInsets(Component c) {
         return new Insets(frameHeight,arc.edge,arc.edge,arc.edge);
      }

      protected Color getULInterior(Component c) { return frame; }
      protected Color getURInterior(Component c) { return frame; }
      protected Color getUpInterior(Component c) { return frame; }

      protected void paintSimple(Component c, Graphics g, int width, int height) {
         super.paintSimple(c,g,width,height);

         g.setColor(darkerGray);
         g.drawLine(1,frameHeight-1,width-2,frameHeight-1);

         g.setColor(frame);
         g.fillRect(arc.edge,arc.edge,width-2*arc.edge,arc.radius-arc.edge);
         g.fillRect(1,arc.radius,width-2,frameHeight-arc.radius-1);
         // a tiny bit of repainting over the superclass in the second case

         paintTitle(g,title,width,arc.edge);
      }
   }

   private static class CompactBorder extends RoundBorder {

      private Color frame;
      private String title;

      public CompactBorder(Arc arc, Color exterior, Color frame, String title) {
         super(arc,exterior);
         this.frame = frame;
         this.title = title;
      }

      public Insets getBorderInsets(Component c) {
         return new Insets(arc.edge,arc.edge,arc.edge,arc.edge);
         // we draw to compactHeight on top, but don't report it
      }

      protected Color getULInterior(Component c) { return frame; }

      protected void paintSimple(Component c, Graphics g, int width, int height) {
         super.paintSimple(c,g,width,height);

         // this one doesn't do font scaling, instead it assumes that
         // the text fits, and makes the title area larger or smaller.

         g.setFont(smallSquareFont);

         FontMetrics fm = g.getFontMetrics();
         int ascent = fm.getAscent();
         int descent = fm.getDescent();
         int twidth = fm.stringWidth(title);

      // break to draw the rest

         int d9 = Text.getInt(ModernStyle.class,"d9");
         // besides looking nice, this space guarantees the two quads won't
         // overlap horizontally, so the drawing code won't do weird things.

         width = twidth + 2*arc.edge + 2*d9; // real width no longer matters

         quadLR(g,width,compactHeight,arc,c.getBackground(),darkerGray,frame);
         // here the exterior of the quad is the interior of the component

         g.setColor(darkerGray);
         g.drawLine(width-1,1,width-1,compactHeight-arc.radius-1);
         g.drawLine(1,compactHeight-1,width-arc.radius-1,compactHeight-1);

         g.setColor(frame);
         g.fillRect(1,arc.radius,arc.edge-1,compactHeight-arc.radius-1);
         g.fillRect(arc.edge,arc.edge,arc.radius-arc.edge,compactHeight-arc.edge-1);
         g.fillRect(arc.radius,1,width-2*arc.radius,compactHeight-2);
         g.fillRect(width-arc.radius,1,arc.radius-arc.edge,compactHeight-arc.edge-1);
         g.fillRect(width-arc.edge,1,arc.edge-1,compactHeight-arc.radius-1);

         // a fair amount of overlap, oh well

      // finish the title

         int tx = arc.edge + d9;
         int ty = (compactHeight + ascent - descent) / 2;

         g.setColor(Color.white);
         g.drawString(title,tx,ty);
      }
   }

   private static class RotatedBorder extends RoundBorder {

      private Color frame;
      private String title;

      public RotatedBorder(Arc arc, Color exterior, Color frame, String title) {
         super(arc,exterior);
         this.frame = frame;
         this.title = title;
      }

      public Insets getBorderInsets(Component c) {
         return new Insets(arc.edge,frameHeight,arc.edge,arc.edge);
      }

      protected Color getULInterior(Component c) { return frame; }
      protected Color getLLInterior(Component c) { return frame; }
      protected Color getLfInterior(Component c) { return frame; }

      protected void paintSimple(Component c, Graphics g, int width, int height) {
         super.paintSimple(c,g,width,height);

         // you'd think you could just subclass FrameBorder and transform
         // the whole thing, but the frame comes out with lines missing.

         g.setColor(darkerGray);
         g.drawLine(frameHeight-1,1,frameHeight-1,height-2);

         g.setColor(frame);
         g.fillRect(arc.edge,arc.edge,arc.radius-arc.edge,height-2*arc.edge);
         g.fillRect(arc.radius,1,frameHeight-arc.radius-1,height-2);
         // a tiny bit of repainting over the superclass in the second case

         Graphics2D g2 = (Graphics2D) g;
         AffineTransform saveTransform = g2.getTransform();

         g2.transform(Rotation.corner90(height,width));
         // width and height are swapped because they're *after* the transform
         // use transform instead of rotate to guarantee exact rotation angle

         paintTitle(g,title,height,arc.edge); // note, height not width!

         g2.setTransform(saveTransform);
      }
   }

// --- progress bar ---

   private static class ProgressBar extends JPanel implements TransferProgress {

      private Color exterior;
      private double fraction;

      public ProgressBar(Color exterior) {

         this.exterior = exterior;
         fraction = 0;

         // caller will set background via adjustPanel
         setOpaque(true);
         setLayout(new FlowLayout(FlowLayout.CENTER,0,0));
         add(Box.createHorizontalStrut(frameHeight));
      }

      public void setExterior(Color exterior) {
         this.exterior = exterior;
         // apparently change in panel background already causes repaint
      }

      private void setFraction(double fraction) {
         this.fraction = fraction;
         repaint();
      }

      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         Font  saveFont  = g.getFont ();
         Color saveColor = g.getColor();

         Dimension size = getSize();

      // the bar

         // lots of overpainting here, but I just want to make it work

         int barHeight = (int) ((size.height-2) * fraction);
         // perfectionism, don't count the two border pixels

         g.setColor(happyGreen);
         g.fillRect(1,size.height-1-barHeight,size.width-2,barHeight);

      // the frame

         quadUL(g,          0,          0,arc3,exterior,darkerGray,null);
         quadUR(g,frameHeight,          0,arc3,exterior,darkerGray,null);
         quadLL(g,          0,size.height,arc3,exterior,darkerGray,null);
         quadLR(g,frameHeight,size.height,arc3,exterior,darkerGray,null);

         g.setColor(darkerGray);
         g.drawLine(arc3.radius,            0,frameHeight-arc3.radius-1,            0);
         g.drawLine(arc3.radius,size.height-1,frameHeight-arc3.radius-1,size.height-1);
         g.drawLine(            0,arc3.radius,            0,size.height-arc3.radius-1);
         g.drawLine(frameHeight-1,arc3.radius,frameHeight-1,size.height-arc3.radius-1);

      // the text

         Graphics2D g2 = (Graphics2D) g;
         AffineTransform saveTransform = g2.getTransform();

         g2.transform(Rotation.pure90()); // really some corner rotation, but the code works like this
         g.setColor(Color.black);
         g.setFont(smallSquareFont);

         String title = Text.get(ModernStyle.class,"s1");

         FontMetrics fm = g.getFontMetrics();
         int ascent = fm.getAscent();
         int descent = fm.getDescent();
         int twidth = fm.stringWidth(title);

         int tx = - (twidth + Text.getInt(ModernStyle.class,"d12") );
         int ty = (frameHeight + ascent - descent) / 2; // the usual

         g.drawString(title,tx,ty);

         g2.setTransform(saveTransform);

      // done

         g.setColor(saveColor);
         g.setFont (saveFont );
      }

   // --- implementation of TransferProgress ---

      public void setProgress(double fraction, int percent) {
         setFraction(fraction);
      }

      public void clearProgress() {
         setFraction(0);
      }
   }

// --- implementation ---

   public Light getLight(Object owner, int style) {
      int width, height;

      switch (style) {

      case Style.LIGHT_SUBSYSTEM_LARGE:
      case Style.LIGHT_VIEW_HUGE:
         width = height = Text.getInt(this,"d1");
         break;

      case Style.LIGHT_SUBSYSTEM_THIN:
         width = Text.getInt(this,"d2");
         height = Text.getInt(this,"d1");
         break;

      case Style.LIGHT_SUBSYSTEM_SMALL:
         width = height = Text.getInt(this,"d3");
         break;

      case Style.LIGHT_SUBSYSTEM_VIEW:
         width = Text.getInt(this,"d2");
         height = Text.getInt(this,"d5");
         break;

      case Style.LIGHT_VIEW_LARGE:
         width = height = Text.getInt(this,"d4");
         break;
      case Style.LIGHT_VIEW_SMALL:
         width = height = Text.getInt(this,"d5");
         break;

      default:
         throw new IllegalArgumentException();
      }

      // account for borders
      width += 2;
      height += 2;

      IconLight light = new IconLight(owner,new Dimension(width,height));
      light.setBackground(lighterGray);
      return light;
   }

   public JPanel getLightLayout(JComponent[] lights) {
      JPanel panel = new JPanel();
      GridBagHelper helper = new GridBagHelper(panel);

      int d6 = Text.getInt(this,"d6");

      int x = 0;

      for (int i=0; i<lights.length; i++) {
         if (i > 0) helper.add(x++,0,Box.createHorizontalStrut(d6));
         helper.add(x++,0,lights[i]);
      }

      return panel;
   }

   public JLabel getUnderlay(String underlay) {
      return new Underlay(underlay.toUpperCase());
   }

   public JPanel getProgressPanel() {
      return new ProgressBar(lighterGray);
   }

   public String getBrandName() {
      return brand.getName();
   }

   public ImageIcon getLogo() {
      return brand.getLogo();
   }

   public void adjustMenuBar(JMenuBar menuBar) {
      menuBar.setBackground(metalGray);

      menuBar.add(Box.createHorizontalGlue());
      JLabel logo = new JLabel(brand.getMenuLogo());
      if (brand.frameMenuLogo()) {
         logo.setBackground(Color.white);
         logo.setOpaque(true);
         logo.setBorder(new RoundBorder(arc4,metalGray));
      }
      menuBar.add(logo);

      int count = menuBar.getMenuCount();
      for (int i=0; i<count; i++) {
         JMenu menu = menuBar.getMenu(i);
         if (menu != null) {
            applyFont(menu,squareFont);

            // menu.setForeground(Color.white);
            // the Windows 7 menu bar ignores the setBackground call
            // and shows a light-colored gradient thing, leaving the
            // foreground text unreadable.

            menu.setBackground(metalGray);
            adjustMenu(menu);
         }
      }
   }

   public void adjustMenu(JMenu menu) {
      int count = menu.getItemCount();
      for (int i=0; i<count; i++) {
         JMenuItem item = menu.getItem(i);
         if (item != null) {
            applyFont(item,squareFont);

            // item.setForeground(Color.white);
            // see above

            item.setBackground(metalGray);
         }
      }
   }

   public void adjustMenu_LiveHelp(JMenu menu) {
   }

   public void adjustTabbedPane(JTabbedPane tabbedPane) {
      applyFont(tabbedPane,smallSquareFont);

      // tabbedPane.setForeground(Color.white);
      // the 1.5 JTabbedPane ignores the setBackground
      // call, leaving the foreground text unreadable.

      tabbedPane.setBackground(metalGray);
      tabbedPane.setOpaque(true);
   }

   public JPanel adjustTab(JPanel tab) {
      tab.setBackground(lighterGray);
      int d8 = Text.getInt(this,"d8");
      Border border = new RoundBorder(arc4,metalGray);
      if (tab.getBorder() != null) border = BorderFactory.createCompoundBorder(border,tab.getBorder());
      tab.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(d8,d8,d8,d8,metalGray),
                                                       border));
      return tab;
   }

   public void refreshTabbedPane(JTabbedPane tabbedPane) {
      int count    = tabbedPane.getTabCount();
      int selected = tabbedPane.getSelectedIndex();
      for (int i=0; i<count; i++) {
         Color color = (i == selected) ? cornflower : metalGray;
         tabbedPane.setBackgroundAt(i,color);
      }
   }

   public boolean showSummaryGrid() {
      return false;
   }

   public boolean newStatusLayout() {
      return true;
   }

   public boolean lowerTransferID() {
      return true;
   }

   public boolean tweakViewPanel() {
      return true;
   }

   public boolean allowAdjacentFields() {
      return false;
   }

   public int getPanelGap() {
      return Text.getInt(this,"d10");
   }

   public int getDetailGap() {
      return Text.getInt(this,"d11");
   }

   public JLabel adjustLine1(JLabel label) {
      applyFont(label,strongFont);
      label.setText(label.getText().toUpperCase());
      label.setForeground(darkestGray);
      return label;
   }

   public JLabel adjustLine2(JLabel label) {
      applyFont(label,smallStrongFont);
      label.setText(label.getText().toUpperCase());
      label.setForeground(darkestGray);
      return label;
   }

   public JLabel adjustCounter(JLabel label) {
      applyFont(label,counterFont);
      return label;
   }

   public JLabel adjustPlain(JLabel label) {
      applyFont(label,buttonFont);
      return label;
   }

   public JLabel adjustControl(JLabel label) {
      applyFont(label,smallSquareFont);
      return label;
   }

   public JLabel adjustHeader(JLabel label) {
      applyFont(label,mediumStrongFont);
      label.setText(label.getText().toUpperCase());
      label.setForeground(darkestGray);
      return label;
   }

   public JButton adjustButton(JButton button) {
      applyFont(button,buttonFont);
      button.setText(button.getText().toUpperCase());
      button.setBackground(lighterGray);
      return button;
   }

   public JButton adjustButton_Details(JButton button) {
      adjustButton(button);
      button.setIcon(Graphic.getIcon("arrow.gif"));
      button.setHorizontalTextPosition(JButton.LEFT);
      return button;
   }

   public void adjustDisabledField(JTextField field) {
      field.setDisabledTextColor(Color.black);
      field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.black),
                                                         BorderFactory.createEmptyBorder(1,1,1,1)));
   }

   public Border createNormalBorder(String title) {
      return new FrameBorder(arc3,lighterGray,Color.black,title);
   }

   public Border createMajorBorder(String title) {
      return createNormalBorder(title.toUpperCase());
   }

   public Border createCompactBorder(String title) {
      return new CompactBorder(arc3,lighterGray,Color.black,title);
   }

   public Border createSidebarBorder(String title) {
      return new RotatedBorder(arc3,lighterGray,cornflower,title.toUpperCase());
   }

   public Border createTotalsBorder() {
      int d14 = Text.getInt(this,"d14");
      int d15 = Text.getInt(this,"d15");
      return BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,Color.black),
                                                BorderFactory.createEmptyBorder(d15,d14,d15,d14));
   }

   public Border createButtonBorder() {
      int d16 = Text.getInt(this,"d16");
      return BorderFactory.createEmptyBorder(0,d16,d16,d16);
   }

   public JPanel adjustPanel(JPanel panel) {
      Style.colorize(panel,Color.white,/* button = */ false);
      return panel;
   }

   public JPanel adjustPanel_Group(JPanel panel) {
      adjustPanel(panel);
      int d13 = Text.getInt(this,"d13");
      panel.setBorder(BorderFactory.createCompoundBorder(new RoundBorder(arc3,lighterGray),
                                                         BorderFactory.createEmptyBorder(d13,d13,d13,d13)));
      return panel;
   }

   public void adjustScroll_Grid(JScrollPane scroll) {
      scroll.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.black));
   }

   public void adjustScroll_Other(JScrollPane scroll) {
      scroll.setBorder(null);
   }

   public void colorize(JFrame frame, boolean alert) {
      Color color = alert ? alertColor : lighterGray;
      colorizeShallow(findTabbedPane(frame.getContentPane()),color);
   }

// --- shallow colorization ---

   private static JTabbedPane findTabbedPane(Container c) {

      // there should be only one tabbed pane ... search and find it.
      // in practice, the search goes through one JPanel and then ends.

      if (c instanceof JTabbedPane) return (JTabbedPane) c;

      if (Style.isTraversablePanel(c)) {
         Component[] sub = c.getComponents();
         for (int i=0; i<sub.length; i++) {
            JTabbedPane temp = findTabbedPane((Container) sub[i]);
            if (temp != null) return temp;
         }
      }

      return null; // shouldn't happen
   }

   private static void colorizeShallow(JTabbedPane tabbedPane, Color color) {
      int count = tabbedPane.getTabCount();
      for (int i=0; i<count; i++) {
         Component c = tabbedPane.getComponentAt(i);

         c.setBackground(color);

         if (Style.isTraversablePanel(c)) { // true
            Component[] sub = ((Container) c).getComponents();
            for (int j=0; j<sub.length; j++) {
               colorizeShallow(sub[j],color);
            }
         }
      }
   }

   private static void colorizeShallow(Component c, Color color) {
      if (c instanceof ProgressBar) {
         ((ProgressBar) c).setExterior(color);

      } else if (c instanceof JComponent) { // true
         Border border = ((JComponent) c).getBorder();
         while (border instanceof CompoundBorder) {
            border = ((CompoundBorder) border).getOutsideBorder();
         }
         if (border instanceof RoundBorder) { // true
            ((RoundBorder) border).setExterior(color);
         }
         // note null is not an instance of anything
      }
   }

}

