package org.ivoa.dm.tapschema;


/*
 * Created on 26/06/2025 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import net.sf.saxon.s9api.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TAPSchemaToVOSITest {

    static Processor processor;
    static XsltCompiler compiler;
    @BeforeAll
    public static void init() {

        processor = new Processor(false);
        compiler = processor.newXsltCompiler();

    }

    @Test
    public void writeVOSI () throws SaxonApiException, FileNotFoundException {

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

    }

}
