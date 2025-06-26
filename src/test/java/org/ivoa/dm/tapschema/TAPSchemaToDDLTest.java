package org.ivoa.dm.tapschema;


/*
 * Created on 26/06/2025 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import net.sf.saxon.s9api.*;
import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.File;

public class TAPSchemaToDDLTest {

    @Test
    public void writeDDL () throws SaxonApiException {
        Processor processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();
        XsltExecutable stylesheet = compiler.compile(new StreamSource(TapschemaModel.class.getResourceAsStream("/tap2posgresql.xsl")));
        Serializer out = processor.newSerializer(new File("tapschema_ddl.sql"));
        out.setOutputProperty(Serializer.Property.METHOD, "text");
        out.setOutputProperty(Serializer.Property.INDENT, "yes");
        Xslt30Transformer transformer = stylesheet.load30();
        transformer.transform(new StreamSource(TapschemaModel.TAPSchema()), out);
    }
}
