#!/bin/bash

. /etc/hadoop/conf/hadoop-env.sh
. /etc/spark/conf/spark-env.sh
. /etc/hbase/conf/hbase-env.sh

######################## END REMOVE THESE LINES ##################################
source $SCRIPT_DIR/global_parameters.sh

cd "${SCRIPT_DIR}/../.."
echo "-------------------------------------------------------"
echo "Data Quality job started on $(date '+%Y-%m-%d %H:%M:%S')"
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
echo "-------------------------------------------------------"
echo " "

spark-submit \
    --class it.agilelab.bigdata.DataQuality.apps.DQMasterBatch \
    --master yarn --deploy-mode client \
    --num-executors $NUM_EXECUTORS \
    --executor-memory $EXECUTOR_MEMORY \
    --executor-cores $EXECUTOR_CORES \
    --driver-memory $DRIVER_MEMORY \
    --files $LOG_CONFIG \
    --conf "spark.ui.showConsoleProgress=true" \
    --conf "spark.default.parallelism=$SPARK_PARALLELISM" \
    --conf "spark.eventLog.enabled=true" \
    --conf "spark.eventLog.dir=$EVENTLOG_DIR" \
    --conf "spark.yarn.executor.memoryOverhead=$EXECUTOR_MEMORY_OVERHEAD" \
    --conf "spark.yarn.driver.memoryOverhead=$DRIVER_MEMORY_OVERHEAD" \
    --conf "spark.speculation=false" \
    --conf "spark.sql.shuffle.partitions=400" \
    --conf "spark.driver.maxResultSize=4g" \
    --conf "spark.hadoop.avro.mapred.ignore.inputs.without.extension=false" \
    $SPARKPARAMS \
    $JAR_NAME \
    $@ 2>&1

RESULT=$?

echo "-------------------------------------------------------"
echo "${LEGAL_ENTITY} ${YEAR}${MONTH} job completed on $(date '+%Y-%m-%d %H:%M:%S')"
echo "-------------------------------------------------------"
exit $RESULT
