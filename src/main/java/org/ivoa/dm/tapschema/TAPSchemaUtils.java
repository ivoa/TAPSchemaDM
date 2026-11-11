package org.ivoa.dm.tapschema;


/*
 * Created on 16/06/2025 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import javax.xml.transform.Source;
import java.util.List;

/**
 * Utilities concerned with model column and table name normalization.
 * Note that this is here to allow package access to the DM.
 */
public class TAPSchemaUtils {

   private TAPSchemaUtils(){};
   /**
    * list of reserved terms.
    */
   private static List<String> reservedADQL = List.of(
         //ADQL specific
         "ABS", "ACOS", "ASIN", "ATAN", "ATAN2", "CEILING", "COS", "DEGREES",
         "EXP", "FLOOR", "LOG", "LOG10", "MOD", "PI", "POWER", "RADIANS",
         "RAND", "ROUND", "SIN", "SQRT", "TAN", "TOP", "TRUNCATE",
         "AREA", "BOX", "CENTROID", "CIRCLE", "CONTAINS", "COORD1", "COORD2",
         "COORDSYS", "DISTANCE", "INTERSECTS", "POINT", "POLYGON", "REGION",
         "COT", "BIGINT", "ILIKE", "IN_UNIT", "OFFSET",
         //SQL - would be better to get from the ADQL parser...
         "ABSOLUTE", "ACTION", "ADD", "ALL", "ALLOCATE", "ALTER", "AND", "ANY",
         "ARE", "AS", "ASC", "ASSERTION", "AT", "AUTHORIZATION", "AVG", "BEGIN",
         "BETWEEN", "BIT", "BIT_LENGTH", "BOTH", "BY", "CASCADE", "CASCADED", "CASE",
         "CAST", "CATALOG", "CHAR", "CHARACTER", "CHAR_LENGTH", "CHARACTER_LENGTH",
         "CHECK", "CLOSE", "COALESCE", "COLLATE", "COLLATION", "COLUMN", "COMMIT",
         "CONNECT", "CONNECTION", "CONSTRAINT", "CONSTRAINTS", "CONTINUE", "CONVERT",
         "CORRESPONDING", "COUNT", "CREATE", "CROSS", "CURRENT", "CURRENT_DATE",
         "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "DATE", "DAY",
         "DEALLOCATE", "DECIMAL", "DECLARE", "DEFAULT", "DEFERRABLE", "DEFERRED", "DELETE",
         "DESC", "DESCRIBE", "DESCRIPTOR", "DIAGNOSTICS", "DISCONNECT", "DISTINCT", "DOMAIN",
         "DOUBLE", "DROP", "ELSE", "END", "END-EXEC", "ESCAPE", "EXCEPT", "EXCEPTION", "EXEC",
         "EXECUTE", "EXISTS", "EXTERNAL", "EXTRACT", "FALSE", "FETCH", "FIRST", "FLOAT", "FOR",
         "FOREIGN", "FOUND", "FROM", "FULL", "GET", "GLOBAL", "GO", "GOTO", "GRANT", "GROUP",
         "HAVING", "HOUR", "IDENTITY", "IMMEDIATE", "IN", "INDICATOR", "INITIALLY", "INNER",
         "INPUT", "INSENSITIVE", "INSERT", "INT", "INTEGER", "INTERSECT", "INTERVAL", "INTO",
         "IS", "ISOLATION", "JOIN", "KEY", "LANGUAGE", "LAST", "LEADING", "LEFT", "LEVEL",
         "LIKE", "LOCAL", "LOWER", "MATCH", "MAX", "MIN", "MINUTE", "MODULE", "MONTH", "NAMES",
         "NATIONAL", "NATURAL", "NCHAR", "NEXT", "NO", "NOT", "NULL", "NULLIF", "NUMERIC",
         "OCTET_LENGTH", "OF", "ON", "ONLY", "OPEN", "OPTION", "OR", "ORDER", "OUTER", "OUTPUT",
         "OVERLAPS", "PAD", "PARTIAL", "POSITION", "PRECISION", "PREPARE", "PRESERVE", "PRIMARY",
         "PRIOR", "PRIVILEGES", "PROCEDURE", "PUBLIC", "READ", "REAL", "REFERENCES", "RELATIVE",
         "RESTRICT", "REVOKE", "RIGHT", "ROLLBACK", "ROWS", "SCHEMA", "SCROLL", "SECOND", "SECTION",
         "SELECT", "SESSION", "SESSION_USER", "SET", "SIZE", "SMALLINT", "SOME", "SPACE", "SQL", "SQLCODE",
         "SQLERROR", "SQLSTATE", "SUBSTRING", "SUM", "SYSTEM_USER", "TABLE", "TEMPORARY", "THEN", "TIME",
         "TIMESTAMP", "TIMEZONE_HOUR", "TIMEZONE_MINUTE", "TO", "TRAILING", "TRANSACTION", "TRANSLATE",
         "TRANSLATION", "TRIM", "TRUE", "UNION", "UNIQUE", "UNKNOWN", "UPDATE", "UPPER", "USAGE", "USER",
         "USING", "VALUE", "VALUES", "VARCHAR", "VARYING", "VIEW", "WHEN", "WHENEVER", "WHERE", "WITH",
         "WORK", "WRITE", "YEAR", "ZONE"

   );

   /**
    * Normalize the model keys.
    * Make sure that the correct keys are generated for a model that as been translated from an XML instance.
    * This needs to be done to make the model ready for saving to a database.
    * @param model the model instance to be normalized.
    */
   public static void normalizeFromSerialization(TapschemaModel model) {

      for (Schema sc: model.getContent(Schema.class)){

         for (Table t: sc.getTables()){
            t.schema_name = sc.schema_name;
            t.table_name = t.table_name.substring(t.table_name.lastIndexOf('.') + 1);
            for (Column c: t.getColumns()){
               c.table_name=t.table_name;
               c.schema_name=sc.schema_name;
               String cn = c.getColumn_name();
               if(cn.contains(".")) {
               c.setColumn_name(cn.substring(cn.lastIndexOf('.') + 1));
               }
            }
         }
      }
   }

   /**
    * Used to prepare a schema for use in the TAP service.
    * @param schema
    * @return a copy of the original schema with the table names denormalised to have the schema included as required by the TAP standard.
    *
    */
   public static Schema adjustforTAPStd(Schema schema) {
      TapschemaModel model = new TapschemaModel();
      model.createContext();
      Schema newschema = new Schema(schema);
      newschema.schema_name = schema.schema_name;
      newschema.updateClonedReferences();
      for (Table t: newschema.getTables()){
         String tn = String.join(".",schema.schema_name,t.getTable_name());
         t.schema_name = schema.schema_name;
         t.table_name=tn;
         for (Column c: t.getColumns()){
            c.schema_name = schema.schema_name;
            c.table_name=tn;
            c.column_name=quoteColumn(c.column_name);
         }
      }
      return newschema;
   }

   /**
    * prepare a model that has been read from a database for XML serialization. Note that
    * the serialization is not performed.
    * @param model the model to be serialized
    */
   public static void prepareForSerialization(TapschemaModel model) {
      for (Schema sc: model.getContent(Schema.class)){
         for (Table t: sc.getTables()){
            String tn = String.join(".",sc.schema_name,t.getTable_name());
            t.table_name=tn;
            for (Column c: t.getColumns()){
               c.column_name=String.join(".",tn,c.column_name);
            }
         }
      }
   }

   /**
    * Quote a column name if necessary.
    * @param columName the column name.
    * @return the column name quoted if necessary.
    */
   private static String quoteColumn(String columName) {
      if (reservedADQL.contains(columName.toUpperCase())) {
         return "\""+columName+"\"";
      }
      else  {
         return columName;
      }
   }

   /**
    * Read a tapschema model from an XML source. This is used to read the TAP schema from the standard XML representation.
    * @param xml The xml source.
    * @return the TAPSchemaModel.
    * @throws JAXBException
    */
   public static TapschemaModel readModelFromXML(Source xml) throws JAXBException {
      JAXBContext jaxbContext = TapschemaModel.contextFactory();
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      JAXBElement<TapschemaModel> el = unmarshaller.unmarshal(xml, TapschemaModel.class);
      TapschemaModel model = el.getValue();
      normalizeFromSerialization(model);
      return model;
   }

   /**
    * Copy a TAPSchema model.
    * @param model The model to copy.
    * @return The copied model.
    * @throws JAXBException
    */
   public static TapschemaModel copy(TapschemaModel model) throws JAXBException {
      TapschemaModel retval = new TapschemaModel();
      retval.createContext();
      for (Schema sc: model.getSchema()){
         Schema newschema = new Schema(sc);
         newschema.schema_name = sc.schema_name;
         newschema.updateClonedReferences();
         retval.addContent(newschema);
      }

      return retval;
   }

}
