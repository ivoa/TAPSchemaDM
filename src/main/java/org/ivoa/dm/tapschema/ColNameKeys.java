package org.ivoa.dm.tapschema;


/*
 * Created on 16/06/2025 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

/**
 * Utilities concerned with model column name normalization.
 */
public class ColNameKeys {

   /**
    * Normalize the model keys.
    * Make sure that the correct keys are generated for a model that as been read from an XML instance.
    * This need to be done to make the model ready for saving to a database.
    * @param model the model instance to be normalized.
    */
   public static void normalize(TapschemaModel model) {

      for (Schema sc: model.getContent(Schema.class)){

         for (Table t: sc.getTables()){
            t.schema_name = sc.schema_name;
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

   private static void updateColumName(Column fc) {
      String cn = fc.getColumn_name();
      fc.setColumn_name(cn.substring(cn.lastIndexOf('.') + 1));
   }
}
