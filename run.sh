#!/bin/bash
displayUsageAndExit() {
  echo -e "\nUsage:\n\t$0 -r YYYY-MM-DD -c configpath [-d]\n"
  exit 1
}

HOME_DIR=


LOG_DIR=${HOME_DIR}/logs
LOG_FILE="${LOG_DIR}/DQ_$(date +%Y-%m-%d-%H-%M)_sh".log
EVENTLOG_DIR=${LOG_DIR}/sparklogs
mkdir -p $LOG_DIR
mkdir -p $EVENTLOG_DIR
echo "Logging to file ${LOG_FILE}"




HADOOP_DIR=/etc/hadoop/conf
JAR_NAME=DataQuality-framework-assembly-1.0.jar

export INPUT_FILE_NAME=
export INPUT_DIR=
export OUTPUT_DIR=

##test input directory with sample file
export SAMPLE_INPUT_DIR=

######SPARK PARAMETERS
NUM_EXECUTORS=2
EXECUTOR_MEMORY=10g
EXECUTOR_CORES=4
DRIVER_MEMORY=5g
EXECUTOR_MEMORY_OVERHEAD=5000
DRIVER_MEMORY_OVERHEAD=5000
SPARK_PARALLELISM=50

######DATA QUALITY PARAMETERS
CONFIG_FILE=${HOME_DIR}/$2     ##"Conf.conf"
##SAMPLEREFDATE=$1             ##"2017-01-21"
REFDATE=$1



echo "Submitting DATA QUALITY Spark job: ${CONFIG_FILE}  -  ${INPUT_FILE_NAME}${REFDATE}"
source $HOME_DIR/submit.sh >> ${LOG_FILE} 2>&1
