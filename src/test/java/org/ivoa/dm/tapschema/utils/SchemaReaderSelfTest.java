package org.ivoa.dm.tapschema.utils;


import jakarta.xml.bind.*;
import org.hibernate.Session;
import org.ivoa.dm.tapschema.Schema;
import org.ivoa.dm.tapschema.TapschemaModel;
import org.ivoa.dm.tapschema.XMLNormalizer;
import org.ivoa.vodml.ModelManagement;
import org.ivoa.vodml.validation.AbstractBaseValidation;
import org.ivoa.vodml.validation.XMLValidator;
import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaReaderSelfTest extends AbstractBaseValidation {
     @Test
     public void selfTranslateTest() throws JAXBException, InterruptedException {

        InputStream is = TapschemaModel.TAPSchema();
        assertNotNull(is);
        String ss = new BufferedReader(new InputStreamReader(TapschemaModel.TAPSchema())).lines().collect(Collectors.joining("\n"));
        System.out.println(ss);
        JAXBContext jc = TapschemaModel.contextFactory();
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        JAXBElement<TapschemaModel> el = unmarshaller.unmarshal(new StreamSource(is), TapschemaModel.class);
        TapschemaModel model = el.getValue();
        assertNotNull(model);

        new XMLNormalizer().prepareForDB(model);

        ModelManagement<TapschemaModel> modelManagement = model.management();
        jakarta.persistence.EntityManager em = setupH2Db(TapschemaModel.pu_name(),TapschemaModel.modelDescription.allClassNames());
        em.getTransaction().begin();
        modelManagement.persistRefs(em);
        em.persist(model.getContent(Schema.class).get(0));
        em.getTransaction().commit();

        // now translate the TAP schema from the database.
        //IMPL hibernate specific way of getting connection... generally dirty, see  https://stackoverflow.com/questions/3493495/getting-database-connection-in-pure-jpa-setup
        AtomicBoolean finished = new AtomicBoolean(false);
        Session sess = em.unwrap(Session.class);
        sess.doWork(conn -> {
           //this is happening in different thread
           try {
              SchemaReader reader = new SchemaReader(conn);
              TapschemaModel readTAP = reader.translate(new SchemaReader.Options().setIncludeSchemas(Set.of("TAP_SCHEMA")));
              StringWriter sw = new StringWriter();
              Marshaller m = jc.createMarshaller();
              m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
              m.marshal(readTAP, sw);
              File file = new File("TAPschemaDiscovered.xml");
              FileWriter fw = new FileWriter(file);
              final String xmlOutput = sw.toString();
              fw.write(xmlOutput);
              fw.close();
              XMLValidator xmlValidator = new XMLValidator(readTAP.management());
              XMLValidator.ValidationResult validation = xmlValidator.validate(xmlOutput);
              if(!validation.isOk)
              {
                 validation.printValidationErrors(System.err);
              }
              assertTrue(validation.isOk,"xml validation failed");

              finished.set(true);
           } catch (Exception e) {
              throw new RuntimeException(e);
           }
        });
        while (!finished.get()) {
           Thread.sleep(200); // wait for the above to actually finish before test exits.
        }
     }
}
