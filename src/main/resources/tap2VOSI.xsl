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

    <xsl:param name="typeDescriptionMode" as="xsd:string" select="'VOTABLE'"/> <!-- can be TAP or VOTABLE -->
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
        <xsl:apply-templates select="(table_name,description,utype,columns,fkeys)"/>
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
            <xsl:apply-templates select="(column_name,description,unit,ucd,utype,datatype,indexed,principal,nullable)"/>
        </column>
    </xsl:template>
    <xsl:template match="(indexed|std)">
        <xsl:if test="text()='1'">
        <flag><xsl:value-of select="name()"/></flag>
        </xsl:if>
    </xsl:template>
    <xsl:template match="principal">
        <xsl:if test="text()='1'">
        <flag>primary</flag>
        </xsl:if>
    </xsl:template>
    <xsl:template match="nullable">
        <xsl:if test="text()='true'">
        <flag>nullable</flag>
        </xsl:if>
    </xsl:template>
    <xsl:template match="datatype">
        <xsl:element name="dataType" >
            <!-- note here how extendedType is used vs xtype in votable -->
            <xsl:choose>
                <xsl:when test="$typeDescriptionMode = 'TAP'">
                    <xsl:attribute name="type" namespace="http://www.w3.org/2001/XMLSchema-instance">vs:TAPType</xsl:attribute>

                    <xsl:if test="@arraysize">
                        <xsl:attribute name="arraysize"><xsl:value-of select="@arraysize"/></xsl:attribute>
                    </xsl:if>
                    <xsl:choose>
                        <!-- translate back into types that are acceptable to VOSI types
                        TODO would be nice if VOSI types were represented properly in the registry -->
                        <xsl:when test="text() = ('CIRCLE','POLYGON','LINE','BOX','ELLIPSE')"><xsl:text>REGION</xsl:text></xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="."/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="type" namespace="http://www.w3.org/2001/XMLSchema-instance">vs:VOTableType</xsl:attribute>
                    <xsl:if test="@arraysize">
                        <xsl:attribute name="arraysize"><xsl:value-of select="@arraysize"/></xsl:attribute>
                    </xsl:if>
                    <xsl:choose>
                        <xsl:when test="text() = 'VARCHAR'">char</xsl:when>
                        <xsl:when test="text() = 'INTEGER'">int</xsl:when>
                        <xsl:when test="text() = 'SMALLINT'">short</xsl:when>
                        <xsl:when test="text() = 'BIGINT'">long</xsl:when>
                        <xsl:when test="text() = 'REAL'">float</xsl:when>
                        <xsl:when test="text() = 'DOUBLE'">double</xsl:when>
                        <xsl:when test="text() = 'BOOLEAN'">boolean</xsl:when>
                        <xsl:when test="text() = 'TIMESTAMP'">
                            <xsl:attribute name="arraysize">*</xsl:attribute>
                            <xsl:attribute name="extendedType">timestamp</xsl:attribute>
                            <xsl:text>char</xsl:text>
                        </xsl:when>
                        <xsl:when test="text() = 'CHAR'">char</xsl:when>
                        <xsl:when test="text() = 'VARCHAR'">char</xsl:when>
                        <xsl:when test="text() = 'CLOB'">char</xsl:when> <!-- todo check this -->
                        <xsl:when test="text() = 'POINT'">
                            <xsl:attribute name="arraysize">2</xsl:attribute>
                            <xsl:attribute name="extendedType">point</xsl:attribute>
                            <xsl:text>double</xsl:text>
                        </xsl:when>
                        <xsl:when test="text() = 'CIRCLE'">
                            <xsl:attribute name="arraysize">3</xsl:attribute>
                            <xsl:attribute name="extendedType">circle</xsl:attribute>
                            <xsl:text>double</xsl:text>
                        </xsl:when>
                        <xsl:when test="text() = 'ELLIPSE'">
                            <xsl:attribute name="arraysize">5</xsl:attribute>
                            <xsl:attribute name="extendedType">ellipse</xsl:attribute>
                            <xsl:text>double</xsl:text>
                        </xsl:when>
                        <xsl:when test="text() = ('POLYGON','REGION')"> <!-- TODO this might not be the most appropriate mapping for REGION - need to see if region will appear in the wild -->
                            <xsl:attribute name="arraysize">*</xsl:attribute>
                            <xsl:attribute name="extendedType">polygon</xsl:attribute>
                            <xsl:text>double</xsl:text>
                        </xsl:when>
                        <xsl:when test="text() = 'LINE'">
                            <xsl:attribute name="arraysize">2</xsl:attribute>
                            <xsl:attribute name="extendedType">line</xsl:attribute>
                            <xsl:text>double</xsl:text>
                        </xsl:when>

                        <xsl:otherwise>
                            <xsl:value-of select="lower-case(text())"/>  <!-- IMPL this catches "unknown" things will probably produce invalid XML against a schema, but acts to flag new things not captured   -->
                            <xsl:message>Unknown data type: <xsl:value-of select="text()"/></xsl:message>
                        </xsl:otherwise>

                    </xsl:choose>

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
        <xsl:value-of select="vft:columnNameQuoting(tokenize($col,'[\.]')[last()])"/>
    </xsl:function>



    <xsl:variable name="reservedWords" as="xsd:string*">
        <xsl:sequence select="(
         'ABS', 'ACOS', 'ASIN', 'ATAN', 'ATAN2', 'CEILING', 'COS', 'DEGREES',
         'EXP', 'FLOOR', 'LOG', 'LOG10', 'MOD', 'PI', 'POWER', 'RADIANS',
         'RAND', 'ROUND', 'SIN', 'SQRT', 'TAN', 'TOP', 'TRUNCATE',
         'AREA', 'BOX', 'CENTROID', 'CIRCLE', 'CONTAINS', 'COORD1', 'COORD2',
         'COORDSYS', 'DISTANCE', 'INTERSECTS', 'POINT', 'POLYGON', 'REGION',
         'COT', 'BIGINT', 'ILIKE', 'IN_UNIT', 'OFFSET',
         'ABSOLUTE', 'ACTION', 'ADD', 'ALL', 'ALLOCATE', 'ALTER', 'AND', 'ANY',
         'ARE', 'AS', 'ASC', 'ASSERTION', 'AT', 'AUTHORIZATION', 'AVG', 'BEGIN',
         'BETWEEN', 'BIT', 'BIT_LENGTH', 'BOTH', 'BY', 'CASCADE', 'CASCADED', 'CASE',
         'CAST', 'CATALOG', 'CHAR', 'CHARACTER', 'CHAR_LENGTH', 'CHARACTER_LENGTH',
         'CHECK', 'CLOSE', 'COALESCE', 'COLLATE', 'COLLATION', 'COLUMN', 'COMMIT',
         'CONNECT', 'CONNECTION', 'CONSTRAINT', 'CONSTRAINTS', 'CONTINUE', 'CONVERT',
         'CORRESPONDING', 'COUNT', 'CREATE', 'CROSS', 'CURRENT', 'CURRENT_DATE',
         'CURRENT_TIME', 'CURRENT_TIMESTAMP', 'CURRENT_USER', 'CURSOR', 'DATE', 'DAY',
         'DEALLOCATE', 'DECIMAL', 'DECLARE', 'DEFAULT', 'DEFERRABLE', 'DEFERRED', 'DELETE',
         'DESC', 'DESCRIBE', 'DESCRIPTOR', 'DIAGNOSTICS', 'DISCONNECT', 'DISTINCT', 'DOMAIN',
         'DOUBLE', 'DROP', 'ELSE', 'END', 'END-EXEC', 'ESCAPE', 'EXCEPT', 'EXCEPTION', 'EXEC',
         'EXECUTE', 'EXISTS', 'EXTERNAL', 'EXTRACT', 'FALSE', 'FETCH', 'FIRST', 'FLOAT', 'FOR',
         'FOREIGN', 'FOUND', 'FROM', 'FULL', 'GET', 'GLOBAL', 'GO', 'GOTO', 'GRANT', 'GROUP',
         'HAVING', 'HOUR', 'IDENTITY', 'IMMEDIATE', 'IN', 'INDICATOR', 'INITIALLY', 'INNER',
         'INPUT', 'INSENSITIVE', 'INSERT', 'INT', 'INTEGER', 'INTERSECT', 'INTERVAL', 'INTO',
         'IS', 'ISOLATION', 'JOIN', 'KEY', 'LANGUAGE', 'LAST', 'LEADING', 'LEFT', 'LEVEL',
         'LIKE', 'LOCAL', 'LOWER', 'MATCH', 'MAX', 'MIN', 'MINUTE', 'MODULE', 'MONTH', 'NAMES',
         'NATIONAL', 'NATURAL', 'NCHAR', 'NEXT', 'NO', 'NOT', 'NULL', 'NULLIF', 'NUMERIC',
         'OCTET_LENGTH', 'OF', 'ON', 'ONLY', 'OPEN', 'OPTION', 'OR', 'ORDER', 'OUTER', 'OUTPUT',
         'OVERLAPS', 'PAD', 'PARTIAL', 'POSITION', 'PRECISION', 'PREPARE', 'PRESERVE', 'PRIMARY',
         'PRIOR', 'PRIVILEGES', 'PROCEDURE', 'PUBLIC', 'READ', 'REAL', 'REFERENCES', 'RELATIVE',
         'RESTRICT', 'REVOKE', 'RIGHT', 'ROLLBACK', 'ROWS', 'SCHEMA', 'SCROLL', 'SECOND', 'SECTION',
         'SELECT', 'SESSION', 'SESSION_USER', 'SET', 'SIZE', 'SMALLINT', 'SOME', 'SPACE', 'SQL', 'SQLCODE',
         'SQLERROR', 'SQLSTATE', 'SUBSTRING', 'SUM', 'SYSTEM_USER', 'TABLE', 'TEMPORARY', 'THEN', 'TIME',
         'TIMESTAMP', 'TIMEZONE_HOUR', 'TIMEZONE_MINUTE', 'TO', 'TRAILING', 'TRANSACTION', 'TRANSLATE',
         'TRANSLATION', 'TRIM', 'TRUE', 'UNION', 'UNIQUE', 'UNKNOWN', 'UPDATE', 'UPPER', 'USAGE', 'USER',
         'USING', 'VALUE', 'VALUES', 'VARCHAR', 'VARYING', 'VIEW', 'WHEN', 'WHENEVER', 'WHERE', 'WITH',
         'WORK', 'WRITE', 'YEAR', 'ZONE')"/>


    </xsl:variable>

    <xsl:function name="vft:columnNameQuoting" as="xsd:string">
        <xsl:param name="col" as="xsd:string"/>
        <xsl:choose>
            <xsl:when test="upper-case($col) = $reservedWords">
                <xsl:value-of select="concat('&quot;',$col,'&quot;')"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$col"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

</xsl:stylesheet>