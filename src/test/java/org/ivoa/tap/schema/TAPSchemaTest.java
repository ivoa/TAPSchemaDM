package org.ivoa.tap.schema;


/*
 * Created on 14/01/2025 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.ivoa.vodml.testing.AutoDBRoundTripTest;

import java.util.ArrayList;
import java.util.List;

public class TAPSchemaTest extends AutoDBRoundTripTest<TapschemaModel,String,Schema> {

    TapschemaModel tapschemaModel;
    Schema theschema;

    final static String desc = "description".repeat(372);


    @Override
    public Schema entityForDb() {
        return theschema;
    }

    @Override
    public void testEntity(Schema schema) {

    }

   @Override
   protected String setDbDumpFile() {
      return "tapschema_dump.sql";
   }


   @Override
    public TapschemaModel createModel() {
        tapschemaModel = new TapschemaModel();
        Table table1 = Table.createTable(t -> {
           t.table_name ="table1";
           t.description = desc;
           t.table_type = TableType.TABLE;
           t.columns = List.of(
                 Column.createColumn(c -> {
                    c.column_name ="col1";
                    c.datatype=TAPType.VARCHAR;
                    c.indexed = true;
                    c.principal = false;
                    c.std = false;
                    c.utype = "a.b.c";
                 }),
                 Column.createColumn(c -> {
                    c.column_name ="col2";
                    c.datatype=TAPType.INTEGER;
                    c.indexed = false;
                    c.principal = false;
                    c.std = false;
                    c.utype = "a.b.c";
                 })
           );
           t.fkeys = new ArrayList<>();

        });
        List<Table> tables = List.of( table1
              ,
              Table.createTable(t -> {
                  t.table_name ="table2";
                  t.table_type = TableType.TABLE;
                  t.columns = List.of(
                        Column.createColumn(c -> {
                           c.column_name ="cola";
                           c.datatype=TAPType.VARCHAR;
                           c.indexed = true;
                           c.principal = false;
                           c.std = false;
                           c.utype = "c.b.a";
                           c.description = desc;
                        }),
                        Column.createColumn(c -> {
                           c.column_name ="colb";
                           c.datatype=TAPType.INTEGER;
                           c.indexed = false;
                           c.principal = false;
                           c.std = false;
                           c.utype = "c.b.b  ";
                        })
                   );
                  t.fkeys = List.of(
                        new ForeignKey("key1",table1,"fkey","fkutype",
                              List.of(new FKColumn(table1.columns.get(0),t.columns.get(1))))
                  );
              })
        );
        theschema = new Schema("mySchema", "utype", desc, 1, tables);
        tapschemaModel.addContent(theschema);
        return tapschemaModel;
    }

    @Override
    public void testModel(TapschemaModel tapschemaModel) {

    }
}
