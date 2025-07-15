package org.ivoa.dm.tapschema;


/*
 * Created on 16/06/2025 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import java.util.List;

/**
 * Utilities concerned with model column and table name normalization.
 */
public class ColNameKeys {


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
    * If true the schema name will be appended to the table name wherever it occurs. It is true by default.
    * It can be made false by setting TAPSCHEMADM_NO_SCHEMA_IN_TABLE_NAME in the environment.
    */
   static boolean include_schema = true;

   static {
      include_schema = !System.getenv().containsKey("TAPSCHEMADM_NO_SCHEMA_IN_TABLE_NAME");
   }
   /**
    * Normalize the model keys.
    * Make sure that the correct keys are generated for a model that as been read from an XML instance.
    * This needs to be done to make the model ready for saving to a database.
    * @param model the model instance to be normalized.
    */
   public static void normalize(TapschemaModel model) {

      for (Schema sc: model.getContent(Schema.class)){

         for (Table t: sc.getTables()){
            t.schema_name = sc.schema_name;
            if (include_schema)t.table_name = sc.schema_name+"."+t.table_name;
            for (Column c: t.getColumns()){
               c.table_name=t.table_name;
               c.schema_name=sc.schema_name;
               String cn = c.getColumn_name();
               if(cn.contains(".")) {
               c.setColumn_name(quoteColum(cn.substring(cn.lastIndexOf('.') + 1)));
               }
            }

         }
      }
   }

   /**
    * Quote a column name if necessary.
    * @param columName the column name.
    * @return the column name quoted if necessary.
    */
   private static String quoteColum(String columName) {
      if (reservedADQL.contains(columName.toUpperCase())) {
         return "\""+columName+"\"";
      }
      else  {
         return columName;
      }
   }

}
