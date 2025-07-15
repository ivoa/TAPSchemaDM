<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:tap="http://ivoa.net/dm/tapschema/v1"
                xmlns:vft="http://www.ivoa.net/xml/VODML/tapfunctions"
                xmlns:vosi="http://www.ivoa.net/xml/VOSITables/v1.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:vs="http://www.ivoa.net/xml/VODataService/v1.1"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema"  exclude-result-prefixes="vft xsd tap">
    <!--TODO this needs to be made more robust to column ordering and missing values -->
   <xsl:output method="xml" indent="yes"/>


    <xsl:template match="tap:tapschemaModel">
        <vosi:tableset>
            <xsl:apply-templates select="schema"/>
        </vosi:tableset>
	</xsl:template>
   <xsl:template match="schema">
      <schema>
          <xsl:apply-templates select="*"/>
      </schema>
   </xsl:template>

   <xsl:template match="tables">
       <xsl:apply-templates select="*"/>
   </xsl:template>
    <xsl:template match="table">
        <xsl:element name="table">
            <xsl:attribute name="type" select="table_type"/>
        <xsl:apply-templates select="(table_name,desciption,utype,columns,fkeys)"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="table_name|schema_name">
        <name><xsl:value-of select="."/></name>
    </xsl:template>
    <xsl:template match="table/columns">
       <xsl:apply-templates select="*"/>
   </xsl:template>
    <xsl:template match="table/columns/column">
        <column>
            <xsl:apply-templates select="(column_name,description,unit,ucd,utype,datatype,indexed,principal)"/>
        </column>
    </xsl:template>
    <xsl:template match="indexed">
        <flag><xsl:value-of select="name()"/></flag>
    </xsl:template>
    <xsl:template match="principal">
        <flag>primary</flag>
    </xsl:template>
    <xsl:template match="datatype">
        <xsl:element name="dataType" >
            <xsl:attribute name="type" namespace="http://www.w3.org/2001/XMLSchema-instance">vs:VOTableType</xsl:attribute>
            <xsl:choose>
                <xsl:when test="text() = 'VARCHAR'">char</xsl:when>
                <xsl:when test="text() = 'INTEGER'">int</xsl:when>
                <xsl:when test="text() = 'BIGINT'">long</xsl:when><!--FIXME this list is not comprehensive -->

                <xsl:otherwise>
                    <xsl:value-of select="lower-case(text())"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>
    <xsl:template match="column_name">
        <name><xsl:value-of select="vft:columnNameNormalisation(text())"/></name>
    </xsl:template>
    <xsl:template match="fkeys">
        <xsl:apply-templates select="*"/>
    </xsl:template>
    <xsl:template match="foreignKey">
        <foreignKey>
            <xsl:apply-templates select="(target_table,columns/fKColumn,description,utype)"/>
        </foreignKey>
    </xsl:template>
    <xsl:template match="fKColumn">
        <fkColumn>
            <xsl:apply-templates select="*"/>
        </fkColumn>
    </xsl:template>
    <xsl:template match="target_table">
        <targetTable><xsl:value-of select="."/></targetTable>
    </xsl:template>
    <xsl:template match="from_column">
        <fromColumn><xsl:value-of select="vft:columnNameNormalisation(text())"/></fromColumn>
    </xsl:template>
    <xsl:template match="target_column">
        <targetColumn><xsl:value-of select="vft:columnNameNormalisation(text())"/></targetColumn>
    </xsl:template>
    <xsl:template match="*">
        <xsl:element name="{name()}">
            <xsl:apply-templates select="@*|text()"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="@*|text()">
        <xsl:copy>
        </xsl:copy>
    </xsl:template>

   <xsl:function name="vft:columnNameNormalisation" as="xsd:string">

        <xsl:param name="col" as="xsd:string"/>
        <xsl:value-of select="tokenize($col,'[\.]')[last()]"/>
    </xsl:function>



</xsl:stylesheet>