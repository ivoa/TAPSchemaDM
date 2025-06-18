package org.ivoa.dm.tapschema;


/*
 * Created on 31/01/2025 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import org.hibernate.Session;
import org.ivoa.vodml.ModelManagement;
import org.ivoa.vodml.validation.XMLValidator;
import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        assertEquals("TAP_SCHEMA",schema_in.schema_name);
        assertEquals(5,schema_in.getTables().size());
   //TODO add more tests of self consistency
    }
    
    @Test
    public void selfDBTest() throws JAXBException {
         TapschemaModel model = new TapschemaModel();
        InputStream is = TapschemaModel.TAPSchema();
        assertNotNull(is);
        JAXBContext jc = model.management().contextFactory();
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        JAXBElement<TapschemaModel> el = unmarshaller.unmarshal(new StreamSource(is), TapschemaModel.class);
        TapschemaModel model_in = el.getValue();
        assertNotNull(model_in);
        ColNameKeys.normalize(model_in);
        ModelManagement<TapschemaModel> modelManagement = model_in.management();
         Map<String, String> props = new HashMap<>();
         props.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:"+modelManagement.pu_name()+";DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS TAP_SCHEMA");//IMPL differenrt DB for each PU to stop interactions
        props.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        props.put("jakarta.persistence.schema-generation.scripts.create-target", "test.sql");
        props.put("jakarta.persistence.schema-generation.scripts.drop-target", "test-drop.sql");
        props.put("hibernate.hbm2ddl.schema-generation.script.append", "false");
        props.put("jakarta.persistence.create-database-schemas", "true");

        props.put("jakarta.persistence.schema-generation.create-source", "metadata");
        props.put("jakarta.persistence.schema-generation.database.action", "drop-and-create");
        props.put("jakarta.persistence.schema-generation.scripts.action", "drop-and-create");
        props.put("jakarta.persistence.jdbc.user", "");        
        jakarta.persistence.EntityManagerFactory emf = jakarta.persistence.Persistence.createEntityManagerFactory(modelManagement.pu_name(), props);

        jakarta.persistence.EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        modelManagement.persistRefs(em);
        em.persist(model_in.getContent(Schema.class).get(0));
        em.getTransaction().commit();
        Session sess = em.unwrap(Session.class);
            sess.doWork(conn -> {
                PreparedStatement ps = conn.prepareStatement("SCRIPT TO ?"); // this is H2db specific
                ps.setString(1, "tapschema_dump.sql");
                ps.execute();
            });
    }
}

