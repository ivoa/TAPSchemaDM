<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tap="http://ivoa.net/dm/tapschema/v1" xmlns:vft="http://www.ivoa.net/xml/VODML/tapfunctions"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema">
   <xsl:output method="text"/> 
	<xsl:template match="tap:tapschemaModel">
		<xsl:apply-templates select="schema"/>
	</xsl:template>
   <xsl:template match="schema">
create schema <xsl:value-of select="schema_name"/>;
       <xsl:apply-templates select="tables/table"/>
       <xsl:apply-templates select="tables/table/fkeys/foreignKey"/>
   </xsl:template>
   <xsl:template match="table">
     <xsl:text>create table </xsl:text>
     <xsl:value-of select="concat(ancestor::schema/schema_name,'.',table_name)"/>
     <xsl:text> (</xsl:text>
     <xsl:apply-templates select="columns/column"/>
     <!-- the following is a sort of heuristic for primary key - indexed does not necessary mean primary so prefer to identify via foreign keys -->
     <xsl:choose>
         <xsl:when test="parent::tables/table/fkeys/foreignKey[target_table = current()/table_name]">
             <xsl:apply-templates select="(parent::tables/table/fkeys/foreignKey[target_table = current()/table_name])[1]" mode="primarykey"/>
         </xsl:when>
         <xsl:otherwise>
             <xsl:apply-templates select="columns" mode="primarykey"/>
         </xsl:otherwise>
     </xsl:choose>
     <xsl:text>);&#xa;</xsl:text>
   </xsl:template>
   <xsl:template match="foreignKey" mode="primarykey">
     <xsl:value-of select="concat('primary key (', string-join(distinct-values(for $c in columns/fKColumn/target_column return vft:columnNameNormalisation($c)),','),')')"/>
   </xsl:template>
   <xsl:template match="columns" mode="primarykey">
       <xsl:value-of select="concat('primary key (', string-join(distinct-values(for $c in column[indexed='true'] return vft:columnNameNormalisation($c/column_name)),','),')')"/>
   </xsl:template>
   <xsl:template match="column">
     <xsl:value-of select="concat(vft:columnNameNormalisation(column_name),' ', datatype,', ')"/>
   </xsl:template>
   <xsl:template match="foreignKey">
     <xsl:value-of select="concat('alter table if exists ',ancestor::schema/schema_name,'.',ancestor-or-self::table/table_name ,' add constraint ', replace(key_id,'\.','_'), ' foreign key (',string-join(for $c in columns/fKColumn/from_column return vft:columnNameNormalisation($c),','),') references ', ancestor::schema/schema_name,'.',target_table)"/>
       <xsl:text>;&#xa;</xsl:text>
   </xsl:template>  

   <xsl:function name="vft:columnNameNormalisation" as="xsd:string">

        <xsl:param name="col" as="xsd:string"/>
        <xsl:value-of select="tokenize($col,'[\.]')[last()]"/>
    </xsl:function>	

</xsl:stylesheet>