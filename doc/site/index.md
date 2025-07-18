TAPSchemaDM
===========

This is the TAP schema defined using VO-DML and created so that a mapping from a arbitrary VO-DML model to its TAP schema can be serialized. The [VO-DML tooling](https://ivoa.github.io/vo-dml/) will produce the TAP schema for a model as part of the output of the `vodmlSchema` command.

The model follows the description of TAP schema in the [TAP Standard](https://www.ivoa.net/documents/TAP/). The TAP Standard does not provide a machine-readable version of the TAP Schema - this DM fills that gap. Some of model element names themselves are different from the names used in the TAP standard (and this is reflected in the XML serialization). However, naturally the RDB serialization will produce exactly the required tables as described in the TAP Standard - although there are some additional columns produced.

## How to use

### Java

The Standard [VO-DML tooling](https://ivoa.github.io/vo-dml/) will produce a TAPSchemaDM instance in the static```.TAPSchema()``` method of the generated model (See [TapSchemaModel](generated/javadoc/org/ivoa/dm/tapschema/TapschemaModel.html) for the Model class of the TAPSchemaDM itself). The code below shows the essential steps
for storing the generated TAPSchema in a database. In this case it is for the TAPSchema for the TAPSchema itself, but it will apply for any model code generated by the VO-DML tooling - which itself does not have a direct dependency declared for this library. However, then to decode and save the TAPSchema instance in a project then a direct dependency to the  [![tapschema](https://img.shields.io/maven-central/v/org.javastro.ivoa.dm/tapschema.svg?label=tapschema)](https://central.sonatype.com/artifact/org.javastro.ivoa.dm/tapschema/) should be declared.

* Unmarshal the XML instance
* Normalise the tap schema instance to produce the extra key columns required by the database serialization
* Serialize this model instance to the database.

```java

TapschemaModel model = new TapschemaModel();
InputStream is = TapschemaModel.TAPSchema();
JAXBContext jc = model.management().contextFactory();
Unmarshaller unmarshaller = jc.createUnmarshaller(); 
JAXBElement<TapschemaModel> el = unmarshaller.unmarshal(new StreamSource(is), TapschemaModel.class);
TapschemaModel model_in = el.getValue();
org.ivoa.dm.tapschema.ColNameKeys.normalize(model_in); // note that this step is necessary before saving to the database to set up the table_name foreign keys
EntityManager em = emf.createEntityManager(); 
em.getTransaction().begin();
model_in.management().persistRefs(em);
em.persist(model_in.getContent(Schema.class).get(0)); 
em.getTransaction().commit();

```
### Direct to DDL

It is possible to work directly with the XML TAPSchema instances (produced by the VO-DML tools [vodmlSchema command](https://ivoa.github.io/vo-dml/Transformers/#schema)) to produce DDL to both create the model tables and fill the TAPSchema tables with the model descriptions.

#### Model Tables

The DDL for the model tables can be created by executing the [tap2posgresql.xsl](https://github.com/ivoa/TAPSchemaDM/blob/main/src/main/resources/tap2posgresql.xsl) against the TAPSchema instance. This repository has a [test](https://github.com/ivoa/TAPSchemaDM/blob/main/src/test/java/org/ivoa/dm/tapschema/TAPSchemaToDDLTest.java) showing this being driven by Java code - in this case the DDL is being created for the TAPSchema itself. 

It is possible to drive the transformation from the commandline (after installing the [Saxon XSLT processor](https://www.saxonica.com/html/documentation12/about/installationjava/installingjava.html))

```shell
java  -jar /opt/packages/SaxonHE12-7J/saxon-he-12.7.jar -xsl:src/main/resources/tap2posgresql.xsl -s:build/generated/sources/vodml/schema/tapschema.vo-dml.tap.xml 
```

It would be equally possible to drive from other languages where Saxon is supported e.g. Python via [saxonche](https://pypi.org/project/saxonche/). 

#### TAPSchema Content
The TAPSchema content for a particular Model can be created by execution the [tap2instanceDDL.xsl](https://github.com/ivoa/TAPSchemaDM/blob/main/src/main/resources/tap2instanceDDL.xsl) XSLT against the TAPSchema instance for a particular model.

## Transforming to VOSI Tables

The design of TAP Schema is very similar to the VOSI Tables definition, but not exactly the same. There is [tap2VOSI.xsl](https://github.com/ivoa/TAPSchemaDM/blob/main/src/main/resources/tap2VOSI.xsl) that can be used to transform a TAPSchemaDM XML instance to a VOSI Tables XML instance.