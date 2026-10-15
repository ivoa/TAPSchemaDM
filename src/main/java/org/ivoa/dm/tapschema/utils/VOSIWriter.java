package org.ivoa.dm.tapschema.utils;

import net.sf.saxon.s9api.*;
import org.ivoa.dm.tapschema.TapschemaModel;
import org.slf4j.Logger;

import javax.xml.transform.stream.StreamSource;
import java.io.StringWriter;
import java.util.Collections;

/**
 * A utility class for converting TAP schema XML definitions to VOSI XML representations.
 *  It uses XSLT transformations to perform the conversion, allowing for different classes of data descriptors (VOTABLE or TAP).
 *
 * @author Paul Harrison (paul.harrison@manchester.ac.uk) */
public class VOSIWriter {
   private static final Logger log = org.slf4j.LoggerFactory.getLogger(VOSIWriter.class);
   private final Processor processor = new Processor(false);
   private final XsltCompiler compiler = processor.newXsltCompiler();
   private final XsltExecutable stylesheet;

   /**
    * constructs a VOSIWriter.
    */
   public VOSIWriter() {
      try {
         stylesheet = compiler.compile(new StreamSource(TapschemaModel.class.getResourceAsStream("/tap2VOSI.xsl")));
      } catch (SaxonApiException e) {
         log.error("Failed to compile XSLT stylesheet for TAP to VOSI conversion", e);
         throw new RuntimeException(e); // should not happen
      }
   }

   /**
    * Enum to represent the forms that the DataType can be described as.
    */
   public enum VOSIDataType {
      /** use VOTABLE type */
      VOTABLE,
      /** use TAP types */
      TAP
   }

   /**
    * convert the TAP schema to VOSI.
    * @param tapSchemaSource the tap schema source.
    * @param dataType the choice of type to describe datatypes.
    * @return The XML representation of the VOSI tableset.
    * @throws SaxonApiException if there is a problem with the XSLT transformation.
    */

   public String convertToVOSI(StreamSource tapSchemaSource, VOSIDataType dataType) throws SaxonApiException {
      Xslt30Transformer transformer = stylesheet.load30();
      StringWriter writer = new StringWriter();
      Serializer out = processor.newSerializer(writer);
      out.setOutputProperty(Serializer.Property.METHOD, "xml");

      transformer.setStylesheetParameters(Collections.singletonMap(
            new QName("typeDescriptionMode"), new XdmAtomicValue(dataType.name())));
      transformer.transform(tapSchemaSource, out);
      return writer.toString();
   }


}
