<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tap="http://ivoa.net/dm/tapschema/v1" xmlns:vft="http://www.ivoa.net/xml/VODML/tapfunctions"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema">
    <!--TODO this needs to be made more robust to column ordering and missing values -->
   <xsl:output method="text"/>
    <xsl:variable name="sq"><xsl:text>'</xsl:text></xsl:variable>
    <xsl:variable name="dq"><xsl:text>"</xsl:text></xsl:variable>
    <xsl:variable name='nl'><xsl:text>
</xsl:text></xsl:variable>

    <xsl:template match="tap:tapschemaModel">
        insert into TAP_SCHEMA.schemas (schema_name, description)
        values <xsl:apply-templates select="schema"/>;

        insert into TAP_SCHEMA.tables (table_name, table_type, utype, description, schema_name)
        values <xsl:apply-templates select="schema/tables/table"/>;

        <!--TODO there are more columns to be put into the columns table -->
        insert into TAP_SCHEMA.columns (column_name,datatype,description,utype,indexed,principal,std)
        values <xsl:apply-templates select="schema/tables/table/columns/column"/>;

        insert into TAP_SCHEMA.keys (key_id,target_table,description,utype,from_table)
        values <xsl:apply-templates select="schema/tables/table/fkeys/foreignKey"/>;

        insert into TAP_SCHEMA.key_columns (key_id, from_column, from_column_table, target_column, target_column_table)
        values <xsl:apply-templates select="schema/tables/table/fkeys/foreignKey/columns/fKColumn"/>;
	</xsl:template>
   <xsl:template match="schema">
       ( <xsl:value-of select="vft:concat( * except tables) "/>)<xsl:if test="not(position()=last())">,</xsl:if>

   </xsl:template>
   <xsl:template match="table">
       ( <xsl:value-of select="concat(vft:concat( * except (columns,fkeys)),',',$sq,current()/ancestor::schema/schema_name,$sq)"/> )<xsl:if test="not(position()=last())">,</xsl:if>
   </xsl:template>
   <xsl:template match="foreignKey">
       ( <xsl:value-of select="concat(vft:concat( * except (columns)),',',$sq,current()/ancestor::table/table_name,$sq)"/> )<xsl:if test="not(position()=last())">,</xsl:if>
   </xsl:template>
   <xsl:template match="column">
       ( <xsl:value-of select="concat($sq,vft:columnNameNormalisation(column_name),$sq,',',vft:concat( * except column_name ))"/> )<xsl:if test="not(position()=last())">,</xsl:if>
   </xsl:template>
   <xsl:template match="fKColumn">
       <!--IMPL - note that this produces the extra columns that this TAPSchemaDM naturally does -->
       ( <xsl:value-of select="string-join( for $v in (current()/ancestor::foreignKey/key_id,vft:columnNameNormalisation(from_column),
       //table[./columns/column/column_name = current()/from_column]/table_name,
       vft:columnNameNormalisation(target_column),
       current()/ancestor::foreignKey/target_table) return concat($sq,$v,$sq),',') "/> )<xsl:if test="not(position()=last())">,</xsl:if>
   </xsl:template>

   <xsl:function name="vft:columnNameNormalisation" as="xsd:string">

        <xsl:param name="col" as="xsd:string"/>
        <xsl:value-of select="tokenize($col,'[\.]')[last()]"/>
    </xsl:function>

    <xsl:function name="vft:concat" as="xsd:string">
        <xsl:param name="els" as="element()*"/>
        <xsl:sequence select="string-join(for $v in ($els) return concat($sq,$v,$sq),',')"/>
    </xsl:function>

</xsl:stylesheet>