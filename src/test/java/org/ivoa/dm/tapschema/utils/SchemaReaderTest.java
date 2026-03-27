package org.ivoa.dm.tapschema.utils;

import org.ivoa.dm.tapschema.Column;
import org.ivoa.dm.tapschema.ForeignKey;
import org.ivoa.dm.tapschema.Schema;
import org.ivoa.dm.tapschema.TAPType;
import org.ivoa.dm.tapschema.Table;
import org.ivoa.dm.tapschema.TapschemaModel;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaReaderTest {

  @Test
  void harvestsTablesColumnsIndexesAndForeignKeys() throws Exception {
    String url = "jdbc:h2:mem:schemaReader;DB_CLOSE_DELAY=-1";

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE SCHEMA IF NOT EXISTS ASTRO");
      statement.execute("SET SCHEMA ASTRO");
      statement.execute(
          "CREATE TABLE CATALOG (ID INTEGER PRIMARY KEY, NAME VARCHAR(64) NOT NULL, RA DOUBLE, DEC DOUBLE)");
      statement.execute(
          "CREATE TABLE OBS (ID BIGINT PRIMARY KEY, CATALOG_ID INTEGER NOT NULL, BAND VARCHAR(16), CONSTRAINT FK_OBS_CATALOG FOREIGN KEY (CATALOG_ID) REFERENCES CATALOG(ID))");
      statement.execute("CREATE INDEX IDX_OBS_BAND ON OBS(BAND)");
    }

    TapschemaModel model;
    try (SchemaReader reader = new SchemaReader(url, "sa", "")) {
      model = reader.translate();
    }
    assertNotNull(model);

    List<Schema> schemas = model.getContent(Schema.class);
    Schema schema =
        schemas.stream()
            .filter(s -> "ASTRO".equalsIgnoreCase(s.getSchema_name()))
            .findFirst()
            .orElseThrow();

    Map<String, Table> tablesByShortName =
        schema.getTables().stream()
            .collect(
                Collectors.toMap(
                    t -> {
                      String name = t.getTable_name();
                      int splitAt = name.lastIndexOf('.');
                      return splitAt >= 0 ? name.substring(splitAt + 1).toUpperCase() : name.toUpperCase();
                    },
                    t -> t));

    assertEquals(2, tablesByShortName.size());
    Table catalog = tablesByShortName.get("CATALOG");
    Table obs = tablesByShortName.get("OBS");
    assertNotNull(catalog);
    assertNotNull(obs);

    Map<String, Column> obsColumns =
        obs.getColumns().stream()
            .collect(Collectors.toMap(c -> c.getColumn_name().toUpperCase(), c -> c));

    assertEquals(TAPType.BIGINT, obsColumns.get("ID").getDatatype());
    assertEquals(TAPType.INTEGER, obsColumns.get("CATALOG_ID").getDatatype());
    assertTrue(obsColumns.get("BAND").getIndexed());
    assertFalse(obsColumns.get("CATALOG_ID").getNullable());

    assertEquals(1, obs.getFkeys().size());
    ForeignKey key = obs.getFkeys().get(0);
    assertNotNull(key.getKey_id());
    assertTrue(
        key.getTarget_table().getTable_name().toUpperCase().endsWith(".CATALOG")
            || "CATALOG".equalsIgnoreCase(key.getTarget_table().getTable_name()));
    assertEquals(1, key.getColumns().size());
    assertEquals(
        "CATALOG_ID", key.getColumns().get(0).getFrom_column().getColumn_name().toUpperCase());
    assertEquals("ID", key.getColumns().get(0).getTarget_column().getColumn_name().toUpperCase());
  }
}

