package org.ivoa.dm.tapschema.utils;

import jakarta.xml.bind.JAXBException;
import org.ivoa.dm.tapschema.TapschemaModel;
import org.ivoa.vodml.validation.XMLValidator;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;

public class ProposalDMPostgresTest {
   @Test
   public void getSchema() throws SQLException {
      String url = System.getenv().getOrDefault("PG_URL", "jdbc:postgresql://localhost:61411/quarkus");
      String user = System.getenv().getOrDefault("PG_USER", "quarkus");
      String password = System.getenv().getOrDefault("PG_PASSWORD", "quarkus");

      try (Connection connection = DriverManager.getConnection(url, user, password)) {
         assertNotNull(connection);
         assertNotNull(connection.getSchema());
         SchemaReader sr = new SchemaReader(connection);
         TapschemaModel model = sr.translate();
         sr.writeSchemas(model, new FileOutputStream("postgres_tapschema.xml"));
         XMLValidator validator = new XMLValidator(model.management());
         XMLValidator.ValidationResult result = validator.validate(new StreamSource(new FileInputStream("postgres_tapschema.xml")));
         if(!result.isOk)         {
            result.printValidationErrors(System.out);
         }
         assertTrue(result.isOk,"tapschema created from proposaldm invalid");
      } catch (IOException | JAXBException e) {
         throw new RuntimeException(e);
      }
   }
}

