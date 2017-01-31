#!/bin/bash

################################## REMOVE THESE LINES ##########################
. /etc/hadoop/conf/hadoop-env.sh

export SPARK_HOME=~/spark-1.6.1-bin-hadoop-spark
export DEFAULT_HADOOP_HOME=/opt/cloudera/parcels/CDH-5.3.6-1.cdh5.3.6.p0.11/lib/hadoop

### Path of Spark assembly jar in HDFS
# export SPARK_JAR_HDFS_PATH=${SPARK_JAR_HDFS_PATH:-/user/spark/share/lib/spark-assembly.jar}

### Let's run everything with JVM runtime, instead of Scala
export JAVA_HOME=/usr/java/jdk1.7.0_67-cloudera

export SPARK_LAUNCH_WITH_SCALA=0
export SPARK_LIBRARY_PATH=${SPARK_HOME}/lib
export SCALA_LIBRARY_PATH=${SPARK_HOME}/lib

export HADOOP_HOME=${HADOOP_HOME:-$DEFAULT_HADOOP_HOME}

if [ -n "$HADOOP_HOME" ]; then
  export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:${HADOOP_HOME}/lib/native
fi

export HADOOP_CONF_DIR=${HADOOP_CONF_DIR:-/etc/hadoop/conf}

######################## END REMOVE THESE LINES ##################################


cd $HOME_DIR
echo "-------------------------------------------------------"
echo "DQ-${REFDATE}-${CONFIG_FILE} job started on $(date '+%Y-%m-%d %H:%M:%S')"
echo "-------------------------------------------------------"
echo " "
echo "***** SPARK PARAMETERS "
echo "  NUM_EXECUTORS="${NUM_EXECUTORS}
echo "  EXECUTOR_MEMORY="${EXECUTOR_MEMORY}
echo "  EXECUTOR_CORES="${EXECUTOR_CORES}
echo "  DRIVER_MEMORY="${DRIVER_MEMORY}
echo "  EXECUTOR_MEMORY_OVERHEAD="${EXECUTOR_MEMORY_OVERHEAD}
echo "  DRIVER_MEMORY_OVERHEAD="${DRIVER_MEMORY_OVERHEAD}
echo "  SPARK_PARALLELISM="${SPARK_PARALLELISM}
echo "  JAR_NAME="${JAR_NAME}
echo " "
echo "***** DATA QUALITY PARAMETERS "
echo "  CONFIG_FILE="${CONFIG_FILE}
echo "  REF_DATE = "${REFDATE}
echo "  SPARK_NUMPARTITIONS="${SPARK_NUMPARTITIONS}
echo "  HOME_DIR="${HOME_DIR}
echo "  INPUT_DIR="${INPUT_DIR}
echo "  OUTPUT_DIR="${OUTPUT_DIR}
echo "-------------------------------------------------------"
echo " "

$SPARK_HOME/bin/spark-submit \
    --class it.agilelab.bigdata.DataQuality.apps.DQMasterBatch \
    --master yarn --deploy-mode client \
    --num-executors $NUM_EXECUTORS \
    --executor-memory $EXECUTOR_MEMORY \
    --executor-cores $EXECUTOR_CORES \
    --driver-memory $DRIVER_MEMORY \
    --files "log4j.properties" \
    --conf "spark.ui.showConsoleProgress=false" \
    --conf "spark.default.parallelism=$SPARK_PARALLELISM" \
    --conf "spark.eventLog.enabled=true" \
    --conf "spark.eventLog.dir=$EVENTLOG_DIR" \
    --conf "spark.yarn.executor.memoryOverhead=$EXECUTOR_MEMORY_OVERHEAD" \
    --conf "spark.yarn.driver.memoryOverhead=$DRIVER_MEMORY_OVERHEAD" \
    --conf "spark.speculation=false" \
    --conf "spark.sql.shuffle.partitions=400" \
    $SPARKPARAMS \
    $JAR_NAME \
    -n "DQ-${REFDATE}-${CONFIG_FILE}" \
    -c ${CONFIG_FILE} \
    -r ${REFDATE} \
RESULT=$?
echo " "
echo "-------------------------------------------------------"
echo "${LEGAL_ENTITY} ${YEAR}${MONTH} job completed on $(date '+%Y-%m-%d %H:%M:%S')"
echo "-------------------------------------------------------"
exit $RESULT