/*
 * UploadConfig.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.net.BandwidthConfig;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.*;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for the upload subsystem.
 */

public class UploadConfig extends Structure {

// --- fields ---

   public String secureURL;
   public String orderUploadURL;
   public String webServicesURL;
   public String uploadSessionURL;
   public String localImageURL;
   public String localPriorityURL;
   public long localPriorityInterval; // millis
   public long idlePollInterval; // millis
   public int successiveFailureLimit;
   public boolean lockdownEnabled;
   public boolean watermarkEnabled;
   public boolean exclusiveEnabled;

   public BandwidthConfig bandwidthConfig;
   public TransformConfig transformConfig;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      UploadConfig.class,
      0,new History(new int[] { 10,721,11,731,12 }),
      new AbstractField[] {

         new StringField("secureURL","SecureURL",3,"https://services.lifepics.com/LCUpload.asp"),
         new StringField("orderUploadURL","OrderUploadURL",4,"https://api.lifepics.com/net/LPWS/LPWebService.asmx"),
         new StringField("webServicesURL","WebServicesURL",8,"https://api.lifepics.com/v3/LPWebService.asmx"),
         new StringField("uploadSessionURL","UploadSessionURL",10,"https://api.lifepics.com/closed/LCService.asmx"),
         new StringField("localImageURL","LocalImageURL",11,"http://api.lifepics.com/closed/UploadLocalImage.aspx"),
         new StringField("localPriorityURL","LocalPriorityURL",12,"https://api.lifepics.com/closed/LCService.asmx/GetPriorityList"),
         new LongField("localPriorityInterval","LocalPriorityInterval-Millis",12,60000),
         new LongField("idlePollInterval","IdlePollInterval-Millis"),
         new IntegerField("successiveFailureLimit","SuccessiveFailureLimit",5,10),
         new BooleanField("lockdownEnabled","LockdownEnabled",7,false),
         new BooleanField("watermarkEnabled","WatermarkEnabled",9,false),
         new BooleanField("exclusiveEnabled","ExclusiveEnabled",9,false),

         new StructureField("bandwidthConfig","BandwidthConfig",BandwidthConfig.sd,6),
         new StructureField("transformConfig","TransformConfig",TransformConfig.sd,2) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               if (version >= 1) {
                  tset(o,new TransformConfig().loadAncient(XML.getElement(node,xmlName)));
               } else {
                  loadDefault(o);
               }
            }
         }
      });

   protected StructureDefinition sd() { return sd; }

// --- copy function ---

   public UploadConfig copy() { return (UploadConfig) CopyUtil.copy(this); }

// --- validation ---

   public void validate() throws ValidationException {

      if (localPriorityInterval < 1) throw new ValidationException(Text.get(this,"e3"));
      // technically it's the local image priority poll interval, but don't say that

      if (idlePollInterval < 1) throw new ValidationException(Text.get(this,"e1"));

      if (successiveFailureLimit < 1) throw new ValidationException(Text.get(this,"e2"));

      bandwidthConfig.validate();
      transformConfig.validate();
   }

}

