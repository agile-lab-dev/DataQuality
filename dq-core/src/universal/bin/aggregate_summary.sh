#!/bin/bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/" && pwd )"
export TMP_DIR=${SCRIPT_DIR}"../tmp"

nowmonth=$(date +%Y-%m)
START_TIME="$( date -d "$nowmonth-15 last month" '+%Y-%m')"

echo $START_TIME
export APP_NAME=${START_TIME}

export SUMMARY_DIR=${SCRIPT_DIR}"/../tmp/${START_TIME}-summary"
rm -r $SUMMARY_DIR
mkdir $SUMMARY_DIR
SUMMARY_REGEX=${SCRIPT_DIR}"/../tmp/"${START_TIME}"-*/*/summary.csv"

cat $SUMMARY_REGEX > $SUMMARY_DIR"/summary.csv"

export MAIL_TO="username@mail.com"
bash ${SCRIPT_DIR}"/send_mail.sh" "Content:\n$( cat $SUMMARY_DIR"/summary.csv" )" "SUMMARY"

echo "SUCCESS"
