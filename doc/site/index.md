TAPSchemaDM
===========

This is the TAP schema defined using VO-DML and created so that a mapping from a arbitrary VO-DML model to its TAP schema can be serialized. The [VO-DML tooling](https://ivoa.github.io/vo-dml/) will produce the TAP schema for a model as part of the output of the `vodmlSchema` command.

The model follows the description of TAP schema in the [TAP Standard](https://www.ivoa.net/documents/TAP/). The TAP Standard does not provide a machine-readable version of the TAP Schema - this DM fills that gap. Some of model element names themselves are different from the names used in the TAP standard (and this is reflected in the XML serialization). However, naturally the RDB serialization will produce exactly the same tables as described in the TAP Standard. 
