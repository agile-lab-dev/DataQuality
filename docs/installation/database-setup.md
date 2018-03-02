# DQ: Database setup guide

## Requirements

In the current setup we are using:
- PostgreSQL 9.3

UI module can work with any database with provision of corresponding JDBC driver.
Core module supports usage of Postgres, Oracle and SQLite databases.

* Note: Schemas are written in PostgreSQL dialect. You'll need to translate it in order to run on a different RDBMS.
* Note: Unfortunately, PostgreSQL 9.3 isn't supporting upsert feature, so there are a set of triggers to workaround this problem. This workaround is decreasing DB IO performance, so in the future updates, we'll port DQ on more modern database

## DQ-UI module
### Defining a schema

UI module is using database to store and process content of one configuration of DQ application. You can download configuration and reset the database in order to have multiple configuration files.

Schema made of 3 sql files stored [here](/dq-ui/conf/evolutions/default)
- [1.sql](/dq-ui/conf/evolutions/default/1.sql): Defines main tables of UI module
- [2.sql](/dq-ui/conf/evolutions/default/2.sql): Defines meta information for metrics
- [3.sql](/dq-ui/conf/evolutions/default/3.sql): Defines meta information for checks

All parts are mandatory. Just run them one by one.

### Configuring 'application.conf'

In the ['applicaton.conf'](/dq-ui/conf/application.conf) of UI module you need to update those strings:
```hocon
db.default.driver=org.postgresql.Driver
db.default.url="jdbc:postgresql://localhost:5432/dataquality"
db.default.username=postgres
db.default.password=""
```

## DQ-Core module
### Defining a schema

Core module needs database to store results of previous runs and perform trend checks.

Schema stored [here](/dq-core/schema.sql).

### Configuring 'application.conf'

Configure core application setup in [application.conf](/dq-core/src/main/resources/application.conf)
- to setup core database :
```hocon
  //  Result storage configuration
  //  Supported types: "DB"
  //  "DB" subtypes: "SQLITE","ORACLE","POSTGRES"
  storage:{
    type: "DB"
    config: {
      host: "localhost:5432/dataquality?user=postgres"
      subtype: "POSTGRES"
    }
  }
```
- setup a hive connection:
```hocon
  hiveDir: "" //${HIVE_PATH}
```
- metric error collection:
```hocon
  errorDumpSize: 1000
  errorFolderPath: "./Agile.DataQuality/side-code/dump"
```
- virtual source temporary folder:
```hocon
  vsDumpConfig: {
    fileFormat: "csv"
    path: "./Agile.DataQuality/side-code/dump/virtual"
    delimiter: ","
  }
```

