#!/bin/bash
displayUsageAndExit() {
  echo -e "\nUsage:\n\t$0 -r YYYY-MM-DD -c configpath [-d]\n"
  exit 1
}

HOME_DIR="$( cd ../.. && pwd )"
export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/" && pwd )"
export APP_DIR=${SCRIPT_DIR}

export LOG_DIRECTORY="${HOME_DIR}/logs/"
export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/" && pwd )"

source ${SCRIPT_DIR}/global-parameters.sh
source ${SCRIPT_DIR}/functions.sh

###### CONFIG GENERATION PARAMETERS q
REMOTE_USERNAME=$(whoami)

# todo: Add input dir path
DATA_DIR=

export INPUT_DIR=${DATA_DIR}

# todo: Add output dir path
export OUTPUT_BASE=

######DATA QUALITY PARAMETERS
export HIVE_PATH=user/hive/warehouse/

START_TIME=$(date +"%d-%m-%Y %T")
start_time_seconds=$(date +%s)

export REFDATE=$(date +"%Y-%m-%d")
export PROJECT_NAME="Agile Lab DQ"
export REFERENCE_MONTH=$(date +"%m%Y")

# todo: By default script will run every configuration stored in "current" directory inside "conf"
FOLDER_NAME="current"

arr=$(ls ${SCRIPT_DIR}/../conf/$FOLDER_NAME/*.conf)
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

  bash ${SCRIPT_DIR}/submit.sh -c ${CONFIG_FILE} -d ${REFDATE} 2>&1 >> ${LOG_FILE}
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

if [ ${#FAILED_LIST[@]} > 0 ]; then
  echo "exit code: 1"
  exit 1
else
  echo "exit code: 0"
  exit 0
fi