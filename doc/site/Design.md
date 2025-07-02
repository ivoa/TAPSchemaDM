TAPSchema Design
================

The TAP schema is described in the [TAP Standard](https://www.ivoa.net/documents/TAP/20190927/REC-TAP-1.1.html#tth_sEc4) in terms of some
outline table definitions with some accompanying text. In trying to make a VO-DML definition of the schema there are some challenges
due to the extra rigour in definition that this introduces.

The design of the [VO-DML definition](../generated/tapschema.vo-dml/) has been driven mainly by obtaining a usable and relatively compact XML serialization, `table`s are children
of `Schema`, `column`s and `ForeignKey`s are children of `table`s, and the columns of `ForeignKey`s are references to columns defined in `table`s. 
Although columns are the only things that are referenced, and consequently the only model elements that actually need identifiers in XML terms, 
the NaturalKey constraint (new to VO-DML 1.1) is used with most of the other elements so that no surrogate keys are created.
The `FKColumns` being a direct composition of the `ForeignKey` does not have an obvious NaturalKey of its own, and so does get a surrogate key created in the `TAP_SCHEMA.key_columns` table. In addition is is "natural" for the VO-DML tooling is to create foreign keys  to the "from" and "target" tables which results in extra `from_column_table` and `target_column_table` columns in the `TAP_SCHEMA.key_columns` table.

In order for the columns to have a truly unique key in the XML serialization the column identifiers are made up from prefixing the column names with the table and schema names delimited with a period. The transformation from the XML serialzation into TAPSchema content has to manipulate these column names into the desired forms.

## Issues with TAPSchema

In creating this model and associated tooling there are a number of things in the design of TAPSchema

* TAPSchema does not explicitly define what primary key for a particular table is - indexed does not mean primary key. It can generally be inferred from the foreign keys on other tables, but this is only true of course if there is a table with which it is joined.
  * This library will store instances with the schema appended to the table names by default - however this can be switched off by defining `TAPSCHEMADM_NO_SCHEMA_IN_TABLE_NAME` as an environment variable. 
* The TAP standard does say that the table_name should be able to be used directly in ADQL, but then it makes the inclusion of schema in the model rather superfluous. It does lead to some other complexities
  * It introduces a security vulnerabilty that malicious content could be put into a tap schema instance, that a client might unwittingly pass on.
  * if an implementation has only one schema then it might choose to leave off the schema from the table name - as the only requirement from the TAP standard is that the table names are unique.  This might lead to inconsistencies in the description of "standard" models like obscore.
    * there is then an issue with case insensitivity and the possible inclusion of quotes in the table names that particular implementations might need to include 
  
    

## Other Implementations of TAP Schema

Both [voltt](https://github.com/gmantele/vollt/blob/master/examples/tap/tap_schema/tap_schema_1.0.sql)
and [OpenCADC](https://github.com/opencadc/tap/blob/main/cadc-tap-server-oracle/src/main/resources/sql/0001_tap_schema11.sql) define the TAP Schema in terms of DDL - although they do make some different choices on the exact keys and indexes.
[GAVO DaCHS](https://gitlab-p4n.aip.de/gavo/dachs/-/blob/main/gavo/resources/inputs/__system__/tap.rd) takes a different approach in defining the TAP schema in terms of its internal "resource definitions". 
