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
