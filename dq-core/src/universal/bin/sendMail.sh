#!/bin/bash

if [ -z $1 ]
then
    echo "No arguments found!"
    exit 1
elif [ -n $1 ]
then
TARGET_NAME=$1
MAIL_TO=$2
NUMBER_FAILED_CHEKCS=$3
FAILED_CHECKS=$4
FILE_PATH=$5
fi

TEXT="for ${TARGET_NAME}  ${NUMBER_FAILED_CHEKCS} checks have failed $2"
echo $TEXT
    ATTACHED_FILE="/tmp/DQ-${TARGET_NAME}.csv"
    rm -f $ATTACHED_FILE
    hdfs dfs -get $FILE_PATH  ${ATTACHED_FILE}
    gzip -f $ATTACHED_FILE
    TEXT="for ${TARGET_NAME}  ${NUMBER_FAILED_CHEKCS} checks have failed : [${FAILED_CHECKS}] filepath: ${FILE_PATH} "

    echo "$TEXT"
#    todo: Define proper credentials
    echo "$TEXT" | mail -s "DATAQUALITY ${RESULT} REPORT CHECKS ${TARGET_NAME}"  -a "${ATTACHED_FILE}.gz" -r $(whoami).$(hostname -s)@host.com ${MAIL_TO}
    rm -f $ATTACHED_FILE
    rm -f "${ATTACHED_FILE}.gz"

exit 0
