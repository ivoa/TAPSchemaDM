package org.ivoa.dm.tapschema;


/*
 * Created on 26/06/2025 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.xml.bind.JAXBException;
import net.sf.saxon.s9api.*;
import org.ivoa.dm.tapschema.utils.VOSIWriter;
import org.javastro.ivoa.entities.IvoaJAXBUtils;
import org.javastro.ivoa.entities.vosi.tables.Tableset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TAPSchemaToVOSITest {

    static Processor processor;
    static XsltCompiler compiler;
    @BeforeAll
    public static void init() {

        processor = new Processor(false);
        compiler = processor.newXsltCompiler();

    }

    @Test
    public void writeVOSI () throws SaxonApiException, IOException, JAXBException, SAXException {

        XsltExecutable stylesheet = compiler.compile(new StreamSource(TapschemaModel.class.getResourceAsStream("/tap2VOSI.xsl")));
        Serializer out = processor.newSerializer(new File("tables_VOSI.xml"));
        out.setOutputProperty(Serializer.Property.METHOD, "xml");
        Xslt30Transformer transformer = stylesheet.load30();
        transformer.transform(new StreamSource(TapschemaModel.TAPSchema()), out);
        org.javastro.ivoa.schema.XMLValidator validator =  new org.javastro.ivoa.schema.XMLValidator();
        boolean res = validator.validate(new File("tables_VOSI.xml"));
        if(!res) {
            validator.printErrors(System.err);
        }
        assertTrue(res);
        // read back and test


    }

    @Test
    public void testVosiWriter() throws JAXBException, SaxonApiException, IOException, SAXException, ParserConfigurationException {

        TapschemaModel model = TAPSchemaUtils.readModelFromXML(new StreamSource(TapschemaModel.TAPSchema()));
        VOSIWriter vosiWriter = new VOSIWriter(model);
        String vosi = vosiWriter.convertToVOSI(VOSIWriter.VOSIDataType.VOTABLE);

        //make sure that the original model has not been altered
        model.getSchema().forEach(s -> {
            assertEquals("TAP_SCHEMA",s.schema_name);
            assertEquals(5,s.getTables().size());
            s.getTables().forEach(t -> {
                assertNotNull(t.table_name);
                assertFalse(t.getTable_name().contains("."),t.getTable_name());
                t.getColumns().forEach(c -> {
                    assertNotNull(c.column_name);
                    assertFalse(c.getColumn_name().contains("."), c.getColumn_name());
                });
            });
        });

        Tableset ts = IvoaJAXBUtils.unmarshall(new java.io.StringReader(vosi), Tableset.class, false);

        assertNotNull(ts);
        assertEquals(1, ts.getSchemas().size());
        var s = ts.getSchemas().get(0);
        assertEquals("TAP_SCHEMA", s.getName());
        assertEquals(5, s.getTables().size());
        s.getTables().stream()               .filter(t -> t.getName().equals("TAP_SCHEMA.columns"))
              .findFirst()
              .ifPresentOrElse(t -> {
                  assertEquals(17, t.getColumns().size());
                  var cols = t.getColumns();
                  assertTrue(cols.stream().anyMatch(c -> c.getName().equals("\"size\"")),"size column name should be quoted");
              }, () -> {
                  fail("Expected columns table not found");
              });
    }

}
