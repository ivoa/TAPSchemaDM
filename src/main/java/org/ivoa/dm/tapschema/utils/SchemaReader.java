package org.ivoa.dm.tapschema.utils;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.ivoa.dm.tapschema.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * Builds a TAP_SCHEMA model from JDBC metadata. Note that this fills the TAPSchema in a way that is suitable for
 * XML serialization, rather than that desirable for database storage. {@see XMLNormalizer}
 */
public final class SchemaReader implements AutoCloseable {

  private final Connection connection;
  private final boolean ownsConnection;

  private static final Set<String> DEFAULT_SYSTEM_SCHEMAS =
      Set.of(
          "INFORMATION_SCHEMA",
          "PG_CATALOG",
          "SYS",
          "SYSTEM",
          "MYSQL",
          "PERFORMANCE_SCHEMA");

  public SchemaReader(String jdbcUrl) throws SQLException {
    this.connection = DriverManager.getConnection(jdbcUrl);
    this.ownsConnection = true;
  }


  public SchemaReader(DataSource dataSource) throws SQLException {
      this.connection = dataSource.getConnection();
      this.ownsConnection = true;
  }


  public SchemaReader(String jdbcUrl, String user, String password) throws SQLException {
    this.connection = DriverManager.getConnection(jdbcUrl, user, password);
    this.ownsConnection = true;
  }

  public SchemaReader(String jdbcUrl, Properties properties) throws SQLException {
    this.connection = DriverManager.getConnection(jdbcUrl, properties);
    this.ownsConnection = true;
  }

  public SchemaReader(Connection connection) {
    this.connection = Objects.requireNonNull(connection, "connection must not be null");
    this.ownsConnection = false;
  }

  /**
   * Options controlling metadata harvesting.
   * This class gives some control over the schema that are harvested and the types of tables that are included.
   *
    * The options are designed to be fluent, so that they can be easily chained together. For example:
    * <pre>
    *   new SchemaReader(connection).translate(new SchemaReader.Options()
    *     .setIncludeSystemSchemas(true)
    *     .setIncludeSchemas(Set.of("PUBLIC", "ASTRO"))
    *     .setTableTypes("TABLE", "VIEW"));
    * </pre>
   */
  public static final class Options {
    private boolean includeSystemSchemas;
    private String catalog;
    private Set<String> includeSchemas = Set.of();
    private String[] tableTypes = new String[] {"TABLE", "VIEW", "MATERIALIZED VIEW"};

    public boolean isIncludeSystemSchemas() {
      return includeSystemSchemas;
    }

    public Options setIncludeSystemSchemas(boolean includeSystemSchemas) {
      this.includeSystemSchemas = includeSystemSchemas;
      return this;
    }

    public String getCatalog() {
      return catalog;
    }

    public Options setCatalog(String catalog) {
      this.catalog = catalog;
      return this;
    }

    public Set<String> getIncludeSchemas() {
      return includeSchemas;
    }

    public Options setIncludeSchemas(Collection<String> includeSchemas) {
      if (includeSchemas == null || includeSchemas.isEmpty()) {
        this.includeSchemas = Set.of();
      } else {
        Set<String> normalized = new LinkedHashSet<>();
        for (String schema : includeSchemas) {
          if (schema != null && !schema.isBlank()) {
            normalized.add(schema);
          }
        }
        this.includeSchemas = normalized;
      }
      return this;
    }

    public String[] getTableTypes() {
      return tableTypes;
    }

    public Options setTableTypes(String... tableTypes) {
      if (tableTypes == null || tableTypes.length == 0) {
        this.tableTypes = null;
      } else {
        this.tableTypes = tableTypes;
      }
      return this;
    }
  }

  /**
   * Translate the database metadata into a TAP_SCHEMA model. By default, system schemas are excluded and only tables and views are included. See {@link Options} for more control.
   * @return the translated metadata model.
   * @throws SQLException if there is a problem reading the database metadata.
   */
  public TapschemaModel translate() throws SQLException {
    return translate(new Options());
  }

  /**
   * Translate the database metadata into a TAP_SCHEMA model. By default, system schemas are excluded and only tables and views are included. See {@link Options} for more control.
   * @param options translation options.
   * @return the translated metadata model.
   * @throws SQLException if there is a problem reading the database metadata.
   */
  public TapschemaModel translate(Options options) throws SQLException {
    Options effective = options == null ? new Options() : options;

    DatabaseMetaData metadata = connection.getMetaData();
    TapschemaModel model = new TapschemaModel();

    Map<String, Table> tableByKey = new LinkedHashMap<>();
    Map<String, Column> columnByKey = new LinkedHashMap<>();

    List<String> schemas = discoverSchemas(metadata, effective);
    int schemaIndex = 0;
    for (String schemaName : schemas) {
      Schema schema = new Schema();
      schema.setSchema_name(schemaName);
      schema.setSchema_index(schemaIndex++);
      model.addContent(schema);

      harvestTablesAndColumns(metadata, effective, schemaName, schema, tableByKey, columnByKey);
    }

    harvestForeignKeys(metadata, effective, schemas, tableByKey, columnByKey);
    model.processReferences();
    return model;
  }

  @Override
  public void close() throws SQLException {
    if (ownsConnection) {
      connection.close();
    }
  }

  private static List<String> discoverSchemas(DatabaseMetaData metadata, Options options)
      throws SQLException {
    List<String> schemaNames = new ArrayList<>();
    try (ResultSet rs = metadata.getSchemas()) {
      while (rs.next()) {
        String schemaName = rs.getString("TABLE_SCHEM");
        if (!shouldIncludeSchema(schemaName, options)) {
          continue;
        }
        schemaNames.add(schemaName);
      }
    }

    if (schemaNames.isEmpty()) {
      String fallback = metadata.getUserName();
      if (shouldIncludeSchema(fallback, options)) {
        schemaNames.add(fallback);
      }
    }

    return schemaNames;
  }

  private static boolean shouldIncludeSchema(String schemaName, Options options) {
    if (schemaName == null || schemaName.isBlank()) {
      return false;
    }

     if (!options.isIncludeSystemSchemas() && DEFAULT_SYSTEM_SCHEMAS.contains(schemaName.toUpperCase())) {
      return false;
    }

    return options.getIncludeSchemas().isEmpty() || options.getIncludeSchemas().contains(schemaName);
  }

  private static void harvestTablesAndColumns(
      DatabaseMetaData metadata,
      Options options,
      String schemaName,
      Schema schema,
      Map<String, Table> tableByKey,
      Map<String, Column> columnByKey)
      throws SQLException {

    int tableIndex = 0;
    try (ResultSet tableRows =
        metadata.getTables(options.getCatalog(), schemaName, "%", options.getTableTypes())) {
      while (tableRows.next()) {
        String tableName = tableRows.getString("TABLE_NAME");
        String tableType = tableRows.getString("TABLE_TYPE");
        String remarks = tableRows.getString("REMARKS");

        Table table = new Table();
        final String key = tableKey(schemaName, tableName);
        table.setTable_name(key);
        table.setTable_type(mapTableType(tableType));
        table.setDescription(emptyToNull(remarks));
        table.setTable_index(tableIndex++);

        schema.addToTables(table);

        tableByKey.put(key, table);

        harvestColumns(metadata, options, schemaName, tableName, table, columnByKey);
      }
    }
  }

  private static void harvestColumns(
      DatabaseMetaData metadata,
      Options options,
      String schemaName,
      String tableName,
      Table table,
      Map<String, Column> columnByKey)
      throws SQLException {

    Set<String> indexedColumns = readIndexedColumns(metadata, options.getCatalog(), schemaName, tableName);
    Set<String> primaryKeyColumns = readPrimaryKeyColumns(metadata, options.getCatalog(), schemaName, tableName);

    try (ResultSet columnRows =
        metadata.getColumns(options.getCatalog(), schemaName, tableName, "%")) {
      while (columnRows.next()) {
        String columnName = columnRows.getString("COLUMN_NAME");
        int dataType = columnRows.getInt("DATA_TYPE");
        String typeName = columnRows.getString("TYPE_NAME");
        int columnSize = columnRows.getInt("COLUMN_SIZE");
        int ordinal = columnRows.getInt("ORDINAL_POSITION");

        Column column = new Column();
        final String key = columnKey(schemaName, tableName, columnName);
        column.setColumn_name(key);
        column.setDatatype(mapTapType(dataType, typeName));
        column.setArraysize(computeArraySize(dataType, columnSize));
        column.setDescription(emptyToNull(columnRows.getString("REMARKS")));
        column.setColumn_index(ordinal > 0 ? ordinal : null);
        column.setNullable(readNullable(columnRows));

        boolean isIndexed = indexedColumns.contains(columnName);
        boolean isPrimaryKey = primaryKeyColumns.contains(columnName);
        column.setIndexed(isIndexed || isPrimaryKey);
        column.setPrincipal(isPrimaryKey);
        column.setIsNaturalKey(isPrimaryKey);
        column.setStd(Boolean.FALSE);

        table.addToColumns(column);

        columnByKey.put(key, column);
      }
    }
  }

  private static Set<String> readIndexedColumns(
      DatabaseMetaData metadata, String catalog, String schemaName, String tableName)
      throws SQLException {
    Set<String> indexed = new HashSet<>();
    try (ResultSet indexRows = metadata.getIndexInfo(catalog, schemaName, tableName, false, false)) {
      while (indexRows.next()) {
        String columnName = indexRows.getString("COLUMN_NAME");
        if (columnName != null && !columnName.isBlank()) {
          indexed.add(columnName);
        }
      }
    }
    return indexed;
  }

  private static Set<String> readPrimaryKeyColumns(
      DatabaseMetaData metadata, String catalog, String schemaName, String tableName)
      throws SQLException {
    Set<String> primary = new HashSet<>();
    try (ResultSet pkRows = metadata.getPrimaryKeys(catalog, schemaName, tableName)) {
      while (pkRows.next()) {
        String columnName = pkRows.getString("COLUMN_NAME");
        if (columnName != null && !columnName.isBlank()) {
          primary.add(columnName);
        }
      }
    }
    return primary;
  }

  private static void harvestForeignKeys(
      DatabaseMetaData metadata,
      Options options,
      List<String> schemas,
      Map<String, Table> tableByKey,
      Map<String, Column> columnByKey)
      throws SQLException {

    for (String sourceSchema : schemas) {
      try (ResultSet tableRows =
          metadata.getTables(options.getCatalog(), sourceSchema, "%", options.getTableTypes())) {
        while (tableRows.next()) {
          String sourceTableName = tableRows.getString("TABLE_NAME");
          Table sourceTable = tableByKey.get(tableKey(sourceSchema, sourceTableName));
          if (sourceTable == null) {
            continue;
          }

          Map<String, ForeignKey> grouped = new LinkedHashMap<>();
          try (ResultSet fkRows =
              metadata.getImportedKeys(options.getCatalog(), sourceSchema, sourceTableName)) {
            while (fkRows.next()) {
              String fkSchema = firstNonBlank(fkRows.getString("FKTABLE_SCHEM"), sourceSchema);
              String fkTable = fkRows.getString("FKTABLE_NAME");
              String fkColumn = fkRows.getString("FKCOLUMN_NAME");

              String pkSchema = firstNonBlank(fkRows.getString("PKTABLE_SCHEM"), sourceSchema);
              String pkTable = fkRows.getString("PKTABLE_NAME");
              String pkColumn = fkRows.getString("PKCOLUMN_NAME");

              String fkName = fkRows.getString("FK_NAME");
              short keySeq = fkRows.getShort("KEY_SEQ");

              String groupKey =
                  keyIdentity(fkSchema, fkTable, pkSchema, pkTable, fkName, keySeq);
              ForeignKey foreignKey = grouped.get(groupKey);
              if (foreignKey == null) {
                foreignKey = new ForeignKey();
                foreignKey.setKey_id(keyId(fkSchema, fkTable, pkSchema, pkTable, fkName));
                foreignKey.setDescription(emptyToNull(fkName));
                foreignKey.setTarget_table(tableByKey.get(tableKey(pkSchema, pkTable)));
                grouped.put(groupKey, foreignKey);
              }

              Column fromColumn = columnByKey.get(columnKey(fkSchema, fkTable, fkColumn));
              Column targetColumn = columnByKey.get(columnKey(pkSchema, pkTable, pkColumn));
              if (fromColumn != null && targetColumn != null) {
                foreignKey.addToColumns(new FKColumn(fromColumn, targetColumn));
              }
            }
          }

          for (ForeignKey foreignKey : grouped.values()) {
            if (foreignKey.getTarget_table() != null && !foreignKey.getColumns().isEmpty()) {
              sourceTable.addToFkeys(foreignKey);
            }
          }
        }
      }
    }
  }

  private static String keyIdentity(
      String fkSchema,
      String fkTable,
      String pkSchema,
      String pkTable,
      String fkName,
      short keySeq) {
    String stableName = fkName == null ? "anon-" + keySeq : fkName;
    return fkSchema
        + "|"
        + fkTable
        + "|"
        + pkSchema
        + "|"
        + pkTable
        + "|"
        + stableName;
  }

  private static String keyId(
      String fkSchema, String fkTable, String pkSchema, String pkTable, String fkName) {
    String base =
        firstNonBlank(fkName, fkSchema + "_" + fkTable + "_FK");
    return base + "__" + pkSchema + "_" + pkTable;
  }

  private static TableType mapTableType(String tableType) {
    if (tableType == null) {
      return TableType.TABLE;
    }
     if (tableType.toUpperCase().contains("VIEW")) {
      return TableType.VIEW;
    }
    return TableType.TABLE;
  }

  private static TAPType mapTapType(int sqlType, String typeName) {
    return switch (sqlType) {
      case Types.BOOLEAN, Types.BIT -> TAPType.BOOLEAN;
      case Types.TINYINT, Types.SMALLINT -> TAPType.SMALLINT;
      case Types.INTEGER -> TAPType.INTEGER;
      case Types.BIGINT -> TAPType.BIGINT;
      case Types.REAL -> TAPType.REAL;
      case Types.FLOAT, Types.DOUBLE, Types.NUMERIC, Types.DECIMAL -> TAPType.DOUBLE;
      case Types.DATE,
          Types.TIME,
          Types.TIMESTAMP,
          Types.TIMESTAMP_WITH_TIMEZONE,
          Types.TIME_WITH_TIMEZONE -> TAPType.TIMESTAMP;
      case Types.CHAR, Types.NCHAR -> TAPType.CHAR;
      case Types.VARCHAR, Types.NVARCHAR, Types.SQLXML -> TAPType.VARCHAR;
      case Types.LONGVARCHAR, Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB -> TAPType.CLOB;
      case Types.BINARY -> TAPType.BINARY;
      case Types.VARBINARY -> TAPType.VARBINARY;
      case Types.LONGVARBINARY, Types.BLOB -> TAPType.BLOB;
      default -> mapNonStandard(typeName);
    };
  }

  private static TAPType mapNonStandard(String typeName) {
     if (typeName.contains("POINT")) {
      return TAPType.POINT;
    }
    if (typeName.contains("REGION")) {
      return TAPType.REGION;
    }
    if (typeName.contains("BLOB") || typeName.contains("BYTEA")) {
      return TAPType.BLOB;
    }
    if (typeName.contains("BINARY")) {
      return TAPType.VARBINARY;
    }
    if (typeName.contains("CLOB") || typeName.contains("TEXT") || typeName.contains("JSON")) {
      return TAPType.CLOB;
    }
    if (typeName.contains("CHAR") || typeName.contains("STRING")) {
      return TAPType.VARCHAR;
    }
    return TAPType.VARCHAR;
  }

  private static String computeArraySize(int sqlType, int columnSize) {
    if (columnSize <= 0) {
      return null;
    }
    return switch (sqlType) {
      case Types.CHAR,
          Types.VARCHAR,
          Types.NCHAR,
          Types.NVARCHAR,
          Types.BINARY,
          Types.VARBINARY -> String.valueOf(columnSize);
      default -> null;
    };
  }

  private static Boolean readNullable(ResultSet columnRows) throws SQLException {
    int nullable = columnRows.getInt("NULLABLE");
    if (nullable == DatabaseMetaData.columnNoNulls) {
      return Boolean.FALSE;
    }
    if (nullable == DatabaseMetaData.columnNullable) {
      return Boolean.TRUE;
    }
    return null;
  }

  private static String tableKey(String schema, String table) {
    return schema + "." + table;
  }

  private static String columnKey(String schema, String table, String column) {
    return tableKey(schema, table) + "." + column;
  }

 

  private static String emptyToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }

  private static String firstNonBlank(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return value;
  }

  public void writeSchemas(TapschemaModel model, OutputStream out) throws IOException, JAXBException {
    JAXBContext jc = TapschemaModel.contextFactory();
    Marshaller m = jc.createMarshaller();
    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    m.marshal(model, out);
  }
}

