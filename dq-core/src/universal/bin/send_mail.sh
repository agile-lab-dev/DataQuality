#!/bin/bash

if [[ -z $2 ]]; then
  echo "No arguments found! Please specify text and status (OK/KO/ERROR/SUMMARY)."
  exit 1
elif [[ -n $2 ]]; then
  TEXT=$1
  STATUS=$2
fi

echo "--------------------------------------"
echo -e "$STATUS"
echo -e "$MAIL_TO"
echo -e "$APP_NAME"
echo "--------------------------------------"

MAX_SIZE=9500000
TEXT="Status: $STATUS\nLog file path: $LOG_FILE\n$TEXT"

if [[ ! -z $SUMMARY_DIR ]]; then
  if [[ ! -z $LOG_FILE ]]; then
    filesize=$(stat -c%s "$LOG_FILE")
    echo "Size of $LOG_FILE = $filesize bytes."
    if (( filesize < MAX_SIZE )); then
      cp $LOG_FILE $SUMMARY_DIR
    fi
  fi

  cd $SUMMARY_DIR;
  zip -r out.zip . ;
  cd -;

  filesize=$(stat -c%s "$SUMMARY_DIR/summary.zip")

  if (( filesize < MAX_SIZE )); then
    printf "$TEXT" | mail -s "[$STATUS] $APP_NAME Data Quality report" -a $SUMMARY_DIR/out.zip -r $(whoami).$(hostname -s)@unicredit.eu ${MAIL_TO}
    rm $SUMMARY_DIR/out.zip
    exit 0
  fi
fi

printf "$TEXT" | mail -s "[$STATUS] $APP_NAME Data Quality report" -r $(whoami).$(hostname -s)@unicredit.eu ${MAIL_TO}
exit 0
