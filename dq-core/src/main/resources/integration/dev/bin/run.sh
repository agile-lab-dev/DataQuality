#!/bin/bash

# Utility functions
displayUsageAndExit() {
  echo -e "\nUsage:\n\t$0 -r YYYY-MM-DD -c configpath [-d]\n"
  exit 1
}
# -----

export PROJECT_NAME="DEV"
REMOTE_USERNAME=$(whoami)
kinit -kt ~/${REMOTE_USERNAME}.keytab ${REMOTE_USERNAME}

HOME_DIR="$( cd ../.. && pwd )"
export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/" && pwd )"
export APP_DIR=${SCRIPT_DIR}
export LOG_DIRECTORY="${HOME_DIR}/logs/"

source ${SCRIPT_DIR}/global-parameters.sh
source ${SCRIPT_DIR}/functions.sh

FAILED_LIST_FILE=$SCRIPT_DIR"/failed.txt"

###### CONFIG GENERATION PARAMETERS q
REMOTE_USERNAME=$(whoami)

#export INPUT_DIR=${DATA_DIR}

export OUTPUT_BASE=/user/$REMOTE_USERNAME/${PROJECT_NAME}-DQ/OUTPUT

######DATA QUALITY PARAMETERS
#export SQLITE_PATH=${SCRIPT_DIR}/../local-db/"dataquality.db"
export ERROR_DUMP_SIZE=200000

export HIVE_PATH="user/hive/warehouse/"

START_TIME=$(date +"%d-%m-%Y %T")
start_time_seconds=$(date +%s)

export REFDATE=$(date +"%Y-%m-%d")

arr=$(find ${SCRIPT_DIR}/../conf/testrun -name *.conf)
FAILED_LIST=()

echo "Loading confs from $FOLDER_NAME folder..."
# iterate through array using a counter
counter=0
succeeded=0
for i in $arr ; do
  counter=$((counter+1))
  CONFIG_FILE_NAME=$(echo $i | rev | cut -d"/" -f1 | rev | cut -d"." -f1)
  CONFIG_FILE=$i

  echo "---Starting $CONFIG_FILE_NAME---"

  export OUTPUT_DIR=$OUTPUT_BASE"/"$CONFIG_FILE_NAME
  export TIMESTAMP_CREATION=$(date +"%Y%m%d%H%M%S")

  echo "Current timestamp is "$TIMESTAMP_CREATION
  echo "Submitting DATA QUALITY Spark job with config: ${CONFIG_FILE_NAME} - ${REFDATE}"

  bash ${SCRIPT_DIR}/submit.sh -a ${APP_CONFIG} -c ${CONFIG_FILE} -d ${REFDATE} 2>&1 >> ${LOG_FILE}
  if [ $? -ne 0 ]; then
    echo "Job finished. Status: FAILED"
    FAILED_LIST+=($CONFIG_FILE_NAME)
  else
    echo "Job finished. Status: SUCCESS"
    succeeded=$((succeeded+1))
  fi
done

echo ---All configs finished!---

end_time_seconds=$(date +%s)
END_TIME=$(date +"%d-%m-%Y %T")
runtime=$(python -c "print '%u:%02u' % ((${end_time_seconds} - ${start_time_seconds})/60, (${end_time_seconds} - ${start_time_seconds})%60)")

echo "Run finished in $runtime minutes"
echo "Total amount of configs: $counter. Succeeded: $succeeded, Failed: ${#FAILED_LIST[@]}"

printf '%s\n' "${FAILED_LIST[@]}"

if [ $succeeded -eq 0 ]
 then
  echo "exit code: 1"
  exit 1
elif [ ${#FAILED_LIST[@]} -ne 0 ]
 then
  echo "exit code: 2"

  rm $FAILED_LIST_FILE
  touch $FAILED_LIST_FILE

  printf "%s\n" "${FAILED_LIST[@]}" > $FAILED_LIST_FILE

  exit 2
else
  echo "exit code: 0"
  exit 0
fi