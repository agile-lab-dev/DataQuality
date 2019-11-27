# Sources

Sources are the first and, probably, the most important part of the DataQuality pipeline.
They define files and data sources, that will be processed in future.

Plus, current evolution of DataQuality is allowing you to preprocess data from sources with simple
SQL queries, which will be evaluated with Spark SQL API.

---

There are 2 families of Sources:
- **Source:** simple data source like files and external databases
- **Virtual sources:** product of transformation/combination of simple Sources

## Database connections
> *Tip:* Before you'll be able to pull query results from external databases - you need to define database connector

Currently DataQuality framework supports following databases:
- SQLite
- Oracle
- Postgres

Fill the following form and put it inside "Databases" part of the config:
```hocon
Databases: [
    {
        id: "ORACLE_TEST"
        type: "DB"
        subtype: "ORACLE"
        config: {
            host: "your.db.com",
            port: 1521,
            service: "servicename",
            user: "username",
            password: "password"
        }
    }, 
    ...
]
```

## Simple sources

### Metric error collection
In order to collect statusable metric errors, you must define "keyFields" of a Source/Virtual Source. DataQuality will
 automatically collect all metric fails with a configuration specified in application form.
 
 ```hocon
keyFields: ["height","weight","name"]
```

### SQL supporting data sources
##### DB table

You are able to load tables from defined databases as sources. DataQuality will pull the entire table, but you can
 modify it later with Virtual Sources.

```hocon
{
    id = "sample_table"
    type = "TABLE"
    database = "ORACLE_TEST"
    table = "pinsa"
    // Optional fields
    username = "rocks"
    password = "pocks"
}
```
##### Hive table
Will load Hive table as source with selected query. You need to setup hive connection in application conf before.
```hocon
{
    id = "HIVE_TEST"
    type = "HIVE"
    date = "2017-05-19"
    query = "select * from sources;"
}
```

### HDFS files
##### CSV file
Classic comma separated values. You also can provide a schema or parse it from the header
```hocon
{
    id = "CONT"
    type = "HDFS"
    path = "/path/resources/sample-data/contract.csv",
    fileType = "csv",
    delimiter = "|", // optional
    quote = "'", // optional
    escape = "\\", // optional
    header = false,
    // Optional fields
    schema = [{name: "a", type: "string"}, {name: "b", type: "string"},{name: "c", type: "string"}]
    date = "2017-05-19" 
}
```
##### Fixed format file
File without separation, all values takes constant amount of space.

You'll need to define column names and size in a form of "NAME:SIZE".
> **Tip:** All entered columns will be of type String.
```hocon
{
    id = "FIXEDFILE"
    type = "HDFS"
    path = ${INPUT_DIR}${INPUT_FILE_NAME},
    fileType = "fixed",
    fieldLengths = ["name:16", "id:32", "cap:5"]
}
```
##### AVRO file
Support custom schema from .avsc file
```hocon
{
    id = "AVRO_TEST"
    type = "HDFS"
    path = "/path/resources/sample-data/sample-avro/output/yourfile.avro"
    fileType = "avro"
    schema = "/path/test.avsc"
}
```
##### PARQUET file
Similar as avro but without custom schema support
	
## Virtual sources

Virtual sources are allowing you to define new sources by applying some transformation 
on pre-existing source (or virtual source) data with Spark SQL, before applying metrics on them.


Currently, we are supporting several types of VSs: 
- FILTER-SQL: Allows you to create a new source by applying a transformation on a source (or a virtual source),
by simply specifying a sql-query to apply on. 
- JOIN-SQL: Joining two sources/vs with specified query
- JOIN: Join two sources/vs on a list of  specified columns.

> **Tip:** all virtual sources will be save in the folder specified in application.conf

### FILTER-SQL
You need to specify in the `parentSources` field the id of the Source (or virtualSource), and the query to
apply on, in the field `sql`. 

> **Tip:** Keep in mind, that headers are case sensitive and columns in the sources are String by default.
 Cast columns to avoid problems with type.

Example:
```hocon
{
    id = "TEST_AUTHORS"
    type = "FILTER-SQL"
    parentSources=["TEST_SOURCE"],
    sql="select distinct names as AUTHOR from TEST_SOURCE where height is null and snapshot_date='20150431'"
}
```
### JOIN-SQL
Use this Virtual Source in order to join two Sources with a specific column

Example:
```hocon
{
    id = "PERIMETER"
    type = "JOIN-SQL"
    parentSources=["PART1","PART2"],
    sql="select * from PART1 full outer join PART2 on NAME=AUTHOR"
    // optional fields
    keyFields = ["NAME","AGE"]
}
```
### JOIN
DataQuality also supports automatic join on a specified columns. You have to specify also the join type here 
that could be one of: `inner`, `outer`, `left_outer`, `right_outer`, `leftsemi`.

 
 
Example:
```hocon
{
  id = "PERIMETER"
  type = "JOIN-SQL"
  parentSources=["PART1","PART2"]
  joiningColumns = ["NAME","AGE"]
  joinType = "inner"
}
```

Virtual sources are a very powerful tool that allows you to define metrics on new sources 
obtained by joining or filtering existing ones. The parent sources of a virtual source
can contains id of both source or virtual source, so each virtual source implicitly defines a `Directed Acyclic Graph` of dependencies
of sources. Be aware that this lineage must always end with simple sources in order to be valid one, and should
not define cyclic dependencies.

