#!/bin/bash

sendmail () {
    # todo: Add alert mail recipients
    MAILING_LIST=

    log_level=$1

    gzip -f $LOG_FILE

    if [ ${log_level} -gt 0 ]; then
        RESULT="ERROR"
        TEXT="
        DQ run encountered a problem: ${2}

        Log file:
        ${LOG_FILE}.gz (attached)
        "
    else
        RESULT="SUCCESS"
        TEXT="
        DQ run completed: ${2}

        Log file:
        ${LOG_FILE}.gz (attached)
        "
    fi

    echo "$TEXT"

    # todo: Modify mailing account
    echo "$TEXT" | mail -s "DATAQUALITY ${RESULT} REPORT" -a ${LOG_FILE}".gz" -r $(whoami).$(hostname -s)@mail.com ${MAILING_LIST}
}