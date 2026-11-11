package org.ivoa.dm.tapschema;


import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.TypedQuery;

/*
 * Created on 31/01/2025 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import org.hibernate.Session;
import org.ivoa.vodml.ModelManagement;
import org.ivoa.vodml.validation.AbstractBaseValidation;
import org.ivoa.vodml.validation.XMLValidator;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.stream.Collectors;



import static org.junit.jupiter.api.Assertions.*;

/** tests whether the TAP schema can translate its own TAP schema description */
public class TAPSchemaSelfTest extends AbstractBaseValidation {

    @Override
    protected String setSerializationDumpPrefix()
    {
        return "tapschema_reserialization";
    }

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
    public void selfReadTest() throws JAXBException, ParserConfigurationException, TransformerException, JsonProcessingException {
        TapschemaModel model = new TapschemaModel();
        InputStream is = TapschemaModel.TAPSchema();
        assertNotNull(is);
        String ss = new BufferedReader(new InputStreamReader(TapschemaModel.TAPSchema())).lines().collect(Collectors.joining("\n"));
        System.out.println(ss);
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
        TAPSchemaUtils.normalizeFromSerialization(model_in); // this should work ok = tested below
        TAPSchemaUtils.prepareForSerialization(model_in);
         RoundTripResult<TapschemaModel> xres = roundtripXML(model_in);
         assertTrue(xres.isValid);
       RoundTripResult<TapschemaModel> jres = roundTripJSON(model_in);
       //TODO JSON schema for TAPSchema is not correct - VODML
    }

    @Test
    public void testReadUtil() throws JAXBException {
       TapschemaModel model = TAPSchemaUtils.readModelFromXML(new StreamSource(TapschemaModel.TAPSchema()));
       model.getSchema().forEach(s -> {
           assertEquals("TAP_SCHEMA",s.schema_name);
           assertEquals(5,s.getTables().size());
           s.getTables().forEach(t -> {
               assertNotNull(t.table_name);
              assertFalse(t.getTable_name().contains("."));
              t.getColumns().forEach(c -> {
                  assertNotNull(c.column_name);
                  assertFalse(c.getColumn_name().contains("."));
              });
           });
       });
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

        Schema schemaToSave = TAPSchemaUtils.adjustforTAPStd(model_in.getContent(Schema.class).get(0));
        assertNotNull(schemaToSave);

        ModelManagement<TapschemaModel> modelManagement = model_in.management();

        jakarta.persistence.EntityManager em = setupH2Db(TapschemaModel.pu_name(),TapschemaModel.modelDescription.allClassNames());
        em.getTransaction().begin();
        modelManagement.persistRefs(em);
        em.persist(schemaToSave);
        em.getTransaction().commit();
        Session sess = em.unwrap(Session.class);
            sess.doWork(conn -> {
                PreparedStatement ps = conn.prepareStatement("SCRIPT TO ?"); // this is H2db specific
                ps.setString(1, "tapschema_dump.sql");
                ps.execute();
            });

       // translate back in
           TypedQuery<Schema> qin = em.createQuery("select s from schemas s where s.schema_name='TAP_SCHEMA'",Schema.class) ;
           Schema sin = qin.getSingleResult();
           assertNotNull(sin);
           assertEquals(5, sin.getTables().size());

           TypedQuery<ForeignKey> qkeys = em.createQuery("select k from keys k ",ForeignKey.class) ;
           for (ForeignKey key : qkeys.getResultList())
           {
               assertNotNull(key.key_id);
           }




    }
}

