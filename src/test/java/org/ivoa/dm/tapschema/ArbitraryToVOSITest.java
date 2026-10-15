package org.ivoa.dm.tapschema;


/*
 * Created on 26/06/2025 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import net.sf.saxon.s9api.*;
import org.ivoa.dm.tapschema.utils.VOSIWriter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArbitraryToVOSITest {

    static VOSIWriter vosiwriter;


    @BeforeAll
    public static void init() {
        vosiwriter = new VOSIWriter();

    }

    @Test
    public void writeVOSIAsTAP () throws SaxonApiException {

        InputStream resource = TapschemaModel.class.getResourceAsStream("/test.xml");
        String vosi = vosiwriter.convertToVOSI(new StreamSource(resource), VOSIWriter.VOSIDataType.TAP);
        System.out.println(vosi);
        org.javastro.ivoa.schema.XMLValidator validator =  new org.javastro.ivoa.schema.XMLValidator();
        boolean res = validator.validate(new StreamSource(new java.io.StringReader(vosi)));
        if(!res) {
            validator.printErrors(System.err);
        }
        assertTrue(res);

    }

    @Test
    public void writeVOSIAsVOTABLE () throws SaxonApiException, FileNotFoundException {

        InputStream resource = TapschemaModel.class.getResourceAsStream("/test.xml");
        String vosi = vosiwriter.convertToVOSI(new StreamSource(resource), VOSIWriter.VOSIDataType.VOTABLE);
        System.out.println(vosi);
        org.javastro.ivoa.schema.XMLValidator validator =  new org.javastro.ivoa.schema.XMLValidator();
        boolean res = validator.validate(new StreamSource(new java.io.StringReader(vosi)));
        if(!res) {
            validator.printErrors(System.err);
        }
        assertTrue(res);


    }

}
