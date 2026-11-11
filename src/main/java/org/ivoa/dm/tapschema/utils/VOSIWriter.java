package org.ivoa.dm.tapschema.utils;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import net.sf.saxon.s9api.*;
import org.ivoa.dm.tapschema.TAPSchemaUtils;
import org.ivoa.dm.tapschema.TapschemaModel;
import org.slf4j.Logger;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
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
   private final TapschemaModel model;
   private final Document doc;
   private DocumentBuilderFactory dbf;


   /**
    * constructs a VOSIWriter.
    * @param model the TAP schema model to use for the conversion.
    */
   public VOSIWriter(TapschemaModel model) {

      try {
         this.model = TAPSchemaUtils.copy(model); // make a copy to avoid modifying the original model
         TAPSchemaUtils.prepareForSerialization(this.model);
         stylesheet = compiler.compile(new StreamSource(TapschemaModel.class.getResourceAsStream("/tap2VOSI.xsl")));
         dbf = DocumentBuilderFactory.newInstance();
         doc = dbf.newDocumentBuilder().newDocument();
      } catch (SaxonApiException e) {
         log.error("Failed to compile XSLT stylesheet for TAP to VOSI conversion", e);
         throw new RuntimeException(e); // should not happen
      } catch (ParserConfigurationException e) {
         log.error("Failed to create DocumentBuilder", e);
         throw new RuntimeException(e);
      } catch (JAXBException e) {
         log.error("Failed to copy TAP schema model", e);
         throw new RuntimeException("Failed to copy TAP schema model",e);
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
    * @param dataType the choice of type to describe datatypes.
    * @return The XML representation of the VOSI tableset.
    * @throws SaxonApiException if there is a problem with the XSLT transformation.
    * @throws JAXBException if there is a problem with the JAXB context.
    */

   public String convertToVOSI( VOSIDataType dataType) throws SaxonApiException, JAXBException {
      Xslt30Transformer transformer = stylesheet.load30();
      JAXBContext jc = model.management().contextFactory();
      Marshaller m = jc.createMarshaller();
      m.marshal(model, doc);
      StringWriter writer = new StringWriter();
      Serializer out = processor.newSerializer(writer);
      out.setOutputProperty(Serializer.Property.METHOD, "xml");

      transformer.setStylesheetParameters(Collections.singletonMap(
            new QName("typeDescriptionMode"), new XdmAtomicValue(dataType.name())));
      transformer.transform(new DOMSource(doc), out);
      return writer.toString();
   }


}
