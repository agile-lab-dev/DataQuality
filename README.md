# Data Quality for BigData
---

Table of contents
-------------

- [Overview](#overview)
- [Background](#background)
- [Scenario](#scenario)
- [Compnents](#components)
- [Using DQ](#using-DQ)

---



Overview
-------------

DQ is a framework to build parallel and distributed quality checks on big data environments.
It can be used to calculate metrics and perform checks to assure quality on structured or unstructured data.
It relies entirely on Spark.

It has been developed by a collaboration between [Agile Lab](http://www.agilelab.it) and [Unicredit D&A department](https://github.com/UniCreditDnA).

Compared to typical data quality products, this framework performs quality checks at raw level.
It doesn't leverage any kind of SQL abstraction like Hive or Impala because they perform type checks at runtime hiding bad formatted data.
Hadoop is mainly unstructured data ( files ), so we think that quality checks must be performed at row level without typed abstractions.


With DQ you are allowed to:
- load eterogenous data from different sources (hdfs, DB etc.) and various formats (Avro, Parquet, CSV etc.)
- select, define and perform metrics on DataFrame
- compose and perform checks
- evaluate quality and consistency on data, determined by constraints
- save results on Hdfs, datastore etc.

---


Background
-------------

Perform metrics and checks on huge data is a hard task. With DQ we want to build a flexible framework that allow to specify a formalism to make different metrics to calculate in parallel and one-pass application, and perform and store checks on data.
We use Typesafe Configuration to set workflows and Spark to ingest and perform Data Quality applications based on it.

---


Scenario
-------------


The project is divided into 6 core modules:

- **Config-files**: typesafe configuration files that contains the logic of your data Quality applications: here you can specify sources to load, metrics to process on data, checks to evaluate and targets to save results.
- **DQ-configs**: is the entry point of each applicaton, and provides a configuration parser and loader to run a data quality workflow from a specified configuration file.
- **DQ-metrics**: metrics are formula that can be applied to a table or a column. For each workflow you can define a set of metrics to process in one-pass run.
- **DQ-checks**: checks represent a list of calculated metrics. These objects are run in order to evaluate the quality of data.
- **DQ-sources**: it provides loader for structured data and table from various database and filesystems. To save results of your application you can use **DQ-targets**
- **DQ-apps**: consist of the main of your application, where Spark process the defined and parsed configuration workflow.


**Distributed deployment**, is committed on Hadoop YARN (for Spark).
Please note that the distributed deployment is still under development.

**Run on cluster**, is possible via scheduled period and range on dates.

---



Architecture
-------------

![Architecture](diagrams/DQ2.png)

---



Components
-------------

### Metrics

- On Tables:
    - columns
    - records

- On Columns:
    - empty values
    - null values
    - unique values
    - Min,max,sum,avg for numeric columns
    - Min, max, avg lenght for string columns
    - well formatted ( es. Date with formatter )
    - string domain consistency

- Composed metrics:
    - derived from tables and columns


### Checks

Combinings Metrics you can achieve quality checks.

Quality check categories:<br />

**File Snapshot**<br />
Evaluated on single-file data:
- records bounds ( Min - Max )
- Sum or Avg numeric values bounds ( Min - Max )
- % empty values x column
- % null values x column
- % duplicated values x column ( to check a primary key, % duplicated == 0)
- Unique values vs a specified domain
- Schema match
- String lenghts requirements

**File Trend** (under development) <br />
Comparison between metrics on a specific file today with same metrics in the past
- record today - # record yestarday
- record today – avg(# record) last month

**Cross Files** (under development) <br />
It will be possible to specify dependencies of each file, in order to check metrics on previous transformation steps
- record file A - # record file B. File A is a dependency of file B
- unique values on primary key of file A and # unique values on primary eky of file B 

**Custom SQL** ( applicable only on JDBC targets)<br />
It will be possible to configure specific queries to a target JDBC endpoint ( Oracle, Impala, Hive )
Queries must return a single row with two column:
- Query name
- Test OK/Test KO

**Cut-Off Time** (under development)<br />
This check will monitor last change dates and times, compared with the configured ones.

---




Using DQ
------------

DQ is written in Scala, and the build is managed with SBT.

Before starting:
- Install JDK
- Install SBT
- Install Git


The steps to getting DQ up and running for development are pretty simple:

- Clone this repository:

    `git clone https://github.com/agile-lab-dev/DataQuality.git`

- Start DQ. You can either run DQ in local or cluster mode:

    - local: default setting
    
    - cluster: set isLocal = false calling makeSparkContext() in `DQ/utils/DQMainClass`

- Run DQ. You can either run DQ via scheduled or provided mode (shell):

    - `run.sh`, takes parameters from command line:
        **-n**, Spark job name
        **-c**, Path to configuration file
        **-r**, Indicates the date at which the DataQuality checks will be performed
        **-d**, Specifies whether the application is operating under debugging conditions
        **-h**, Path to hadoop configuration
    
    - `scheduled_run.sh`: set isLocal = false calling makeSparkContext() in `DQ/utils/DQMainClass`
 
---



Define Data Quality workflow by configuration
------------
In order to build your application you need to define a config file where to list sources, metrics, checks and targets.
Data Quality config example:
      
Sources: [

    {
      id = "FIXEDFILE"
      type = "HDFS"
      path = ${INPUT_DIR}${INPUT_FILE_NAME}"{{YYYYMMDD}}",
      fileType = "fixed",
      fieldLengths = ["name:16", "id:32", "cap:5"]
    }
  ]


  Metrics: [
  
    {
      id: "2"
      name: "STRING_IN_DOMAIN"
      type: "COLUMN"
      description: "determine number of values contained in this domain (string)"
      config: {
        file: "FIXEDFILE",
        column: "name",
        params:[{domainSet: "UNKNOWN"}]
      }
    }
  ]

  Checks: [
  
    {
      type: "snapshot"
      subtype: "EQUAL_TO"
      name: "FIXED_CHECK"
      description: "check for specific value assence in column (with threshold)"
      config: {
        metrics: ["2"]
        params: [{threshold: "0"}]
      }
    },
    {
      type: "snapshot"
      subtype: "GREATER_THAN"
      name: "FIXED_CHECK"
      description: "check for rows_number greather than threshold (row count comparison)"
      config: {
        metrics: ["1"]
        params: [{threshold: "1000"}]
      }
    }
  ]

  Targets: [
  
    {
      type: "COLUMNAR-METRICS"
      fileFormat: "csv"
      ruleName: "AT_LEAST_ONE_ERROR",
      config: {
        path: ${OUTPUT_DIR}
        delimiter: "|"
        savemode: "append"
      }
    },
    {
      type: "FILE-METRICS",
      fileFormat: "csv"
      ruleName: "AT_LEAST_ONE_ERROR",
      config: {
        path: ${OUTPUT_DIR}
        name: "FIXED-FILE-METRICS"
        delimiter: "|"
        savemode: "append"
      }
    },
    {
      type: "CHECKS"
      fileFormat: "csv"
      ruleName: "AT_LEAST_ONE_ERROR",
      config: {
        path: ${OUTPUT_DIR}
        delimiter: "|"
        savemode: "overwrite"
      }
    }
  ]

You can find configuration examples (workflows) under **resources**.  

