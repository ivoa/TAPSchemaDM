package org.ivoa.dm.tapschema;


/*
 * Created on 16/06/2025 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */


public class ColNameKeys {

   public static void normalize(TapschemaModel model) {

      for (Schema sc: model.getContent(Schema.class)){
         for (Table t: sc.getTables()){
            for (Column c: t.getColumns()){
               String cn = c.getColumn_name();
               if(cn.contains(".")) {
               c.setColumn_name(cn.substring(cn.lastIndexOf('.') + 1));
               c.table_name = cn.substring(0, cn.lastIndexOf('.') );
               }
               else {
                c.table_name=t.table_name;
               }
            }
            for(ForeignKey k: t.getFkeys())
            {
               for(FKColumn c: k.getColumns())
               {
                  updateColumName(c.getFrom_column());
                  updateColumName(c.getTarget_column());
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
