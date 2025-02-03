TAPSchemaDM
===========

This is the TAP schema defined using VO-DML and created with the VO-DML tools.

The database serialization of this model matches the description of the [TAP schema in the TAP Standard](https://www.ivoa.net/documents/TAP/20190927/REC-TAP-1.1.html#tth_sEc4).

The [vodml tooling](https://github.com/ivoa/vo-dml) will produce TAP schema instances (serialized as XML) automatically for each model. This schema can be read with the `TAPSchema()` method for the model class. The schema instance then then be instantiated using this model - the [TAPSchemaSeltTest.selReadTest()](https://github.com/ivoa/TAPSchemaDM/blob/3ba4f3bbff0a4c8edc7f0a94b365447f2e94f9bc/src/test/java/org/ivoa/tap/schema/TAPSchemaSelfTest.java#L40) method shows this being done on the TAP schema for the TAP schema itself.

