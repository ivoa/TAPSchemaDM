package org.ivoa.tap.schema;


/*
 * Created on 31/01/2025 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.ivoa.vodml.validation.XMLValidator;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** tests whether the TAP schema can read its own TAP schema description */
public class TAPSchemaSelfTest {
    @Test
    public void selfValidationTest() {
        TapschemaModel model = new TapschemaModel();
        InputStream is = TapschemaModel.TAPSchema();
        assertNotNull(is);
        XMLValidator validator = new XMLValidator(model.management());
        XMLValidator.ValidationResult result = validator.validate(new StreamSource(is));
        assertNotNull(result);
        if(!result.isOk)
        {
            result.printValidationErrors(System.out);
        }
        assertTrue(result.isOk);

    }
    @Test
    public void selfReadTest() throws JAXBException {
        TapschemaModel model = new TapschemaModel();
        InputStream is = TapschemaModel.TAPSchema();
        assertNotNull(is);
        JAXBContext jc = model.management().contextFactory();
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        JAXBElement<TapschemaModel> el = unmarshaller.unmarshal(new StreamSource(is), TapschemaModel.class);
        TapschemaModel model_in = el.getValue();
        assertNotNull(model_in);
        List<Schema> schema = model_in.getContent(Schema.class);
        assertEquals(1, schema.size());
        Schema schema_in = schema.get(0);
        assertEquals(5,schema_in.getTables().size());
   //TODO add more tests of self consistency
    }
}

