# Mapping TAP Types to IVOA Standards

This is an AI generated mapping of the TAP types to the typical VOSI/TAP metadata and VOTable serialization patterns. It’s not a normative specification, but it should be a useful reference for understanding how the TAP schema types relate to the IVOA standards.


| TAP schema (`TAPType`) | VOSI/TAP metadata (typical) | VOTable serialization (typical) | Example value | Notes |
|---|---|---|---|---|
| `BOOLEAN` | `datatype=BOOLEAN` | `boolean` | `true` | Nullability via column metadata. |
| `SMALLINT` | `datatype=SMALLINT` | `short` | `-123` | Usually 16-bit signed range. |
| `INTEGER` | `datatype=INTEGER` | `int` | `123456` | Usually 32-bit signed range. |
| `BIGINT` | `datatype=BIGINT` | `long` | `9223372036854775807` | Usually 64-bit signed range. |
| `REAL` | `datatype=REAL` | `float` | `3.14` | Single precision. |
| `DOUBLE` | `datatype=DOUBLE` | `double` | `3.141592653589793` | Double precision. |
| `TIMESTAMP` | `datatype=TIMESTAMP` | usually `char`/`unicodeChar` + `xtype` | `2026-03-18T12:34:56Z` | VOTable has no native timestamp primitive; commonly string + convention/`xtype`. |
| `CHAR` | `datatype=CHAR` | `char` or `unicodeChar` (often scalar/fixed) | `A` | Fixed-length character semantics on TAP side. |
| `VARCHAR` | `datatype=VARCHAR` | `char` or `unicodeChar` + `arraysize="*"` (or bounded) | `gaia_dr3` | Variable-length text typically expressed with `arraysize`. |
| `BINARY` | `datatype=BINARY` | `unsignedByte` with fixed `arraysize` | binary bytes | Fixed-length byte array. |
| `VARBINARY` | `datatype=VARBINARY` | `unsignedByte` + variable `arraysize` | binary bytes | Variable-length byte array. |
| `POINT` | `datatype=POINT` | commonly `char` + geometry `xtype`/ADQL text | `POINT('ICRS', 12.3, -45.6)` | Geometry usually represented via conventions, not a dedicated VOTable primitive. |
| `REGION` | `datatype=REGION` | commonly `char` + geometry `xtype`/ADQL text | `CIRCLE('ICRS', 12.3, -45.6, 0.1)` | Same caveat as `POINT`. |
| `CLOB` | `datatype=CLOB` | `char`/`unicodeChar` large `arraysize` | long text | Size limits are implementation-defined. |
| `BLOB` | `datatype=BLOB` | `unsignedByte` large/variable `arraysize` | large binary | Size limits are implementation-defined. |

Key caveats you’ll care about:
- TAP/VOSI type names and VOTable primitive names are related but not identical vocabularies.
- `TIMESTAMP`, `POINT`, and `REGION` are the main “convention-based” mappings in VOTable (often relying on `xtype` and agreed syntax).
- In your model, `column.arraysize` and `column.xtype` in `src/main/vodsl/tapschema.vodsl` are exactly the extra knobs used to make those mappings precise.

