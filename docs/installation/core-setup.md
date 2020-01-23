## Data Quality core module setup and usage

DQ main application is written in Scala, and the build is managed with SBT.

> **Before starting:** Install JDK, Scala, sbt and Git.

First of all, clone this repository:
```
git clone https://github.com/agile-lab-dev/DataQuality.git
```

Then you have 2 options:
- Run DQ on local
- Create an archive with setup to run in your distributed environment

#### Local run

Simply run `DQMasterBatch` class using your IDE or Java tools with the following arguments

- __-a__: Path to application configuration file.
> **Example:** ./Agile.DataQuality/dq-core/src/main/resources/conf/dev.conf

- __-c__: Path to run configuration file.
> **Example:** ./Agile.DataQuality/docs/examples/conf/full-prostprocess-example.conf

- __-d__: Run date.
> **Example:** 2019-01-01

- __-l__: _Optional._ Flag to run in local mode.

- __-r__: _Optional._ Flag to repartition sources after reading.

#### Configuration file

The configuration file contains variables about storage or log features

All parameters are inside **data_quality** object

 - application_name, the application name (default **Data_Quality**)
 - s3_bucket, bucket url (default "")
 - hive_warehouse_path, hive absolute path (default "")
 - hbase_host, hdase absolute path (default "")
 - tmp_files_management
   - local_fs_path, absolute path
   - hdfs_path, absolute path
 - metric_error_management
   - dump_directory_path, absolute path
   - dump_size, max number of collected errors for 1 metric for 1 partition (default **1000**)
   - empty_file, to write a file also when result set is empty (default **false**)
   - file_config
     - format, file format (default **csv**)
     - delimiter
     - quote
     - escape
     - quote_mode
 - virtual_sources_management
   - dump_directory_path, absolute path
   - file_format
   - delimiter
 - storage
   - type, "NONE" or "DB"
   - config
     - subtype, "POSTGRES", "SQLITE" or "ORACLE"
     - host
     - user
     - password
     - schema
 - mailing
   - mode, valid values are "", ""internal" or "external". Note: internal SMTP thru bash script (check universal/bin/sendMail.sh for extra configuration)
   - mail_script_path, absolute path
   - config
     - address, sender address
     - hostname, SMTP host
     - username, SMTP user
     - password, SMTP password
     - smtpPort, SMTP port
     - sslOnConnect, SSL flag to enable secure connection
   - notifications, true or false (default **false**)

A complete example is into **dq-core/src/main/resources/conf** folder

#### Distributed environment

##### Deployment
Primarily you'll need to deploy your application to the cluster. You can assemble the jar on your own using sbt
 or you can use some of our predefined utilities.
 
To use our `deploy.sh` script follow the following steps:
- Setup REMOTE_HOST and REMOTE_USERNAME in the `deploy.sh`.
- Create an `application.conf` for your environment.
- Create a directory with the internal directories `bin` and `conf`. In the corresponding directories put your
 run scripts and configuration files.
 > **Tip:** You can use `run-default.sh` as a base for your run script.
- Link `application.conf` file and directory with run scripts and confs to the correnspontig parameter values
in the `build.sbt`.
- Run `deploy.sh` with your parameters.

##### Submitting
In distributed environment Data Quality application is being treated as a standard Spark Job, submitted
 by `submit.sh` script.
 
You can submit your job manually to leverage it on a run script. This is completely up to you.
