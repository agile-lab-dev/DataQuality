#!/usr/bin/env bash

# $1 - build environment
# $2 - integration project: ...
# todo: add your credentials
case $1 in
    dev)
        REMOTE_HOST=
        REMOTE_USERNAME=
        ;;
    test)
        REMOTE_HOST=
        REMOTE_USERNAME=
        ;;
    *)
        echo "Unknown environment! Please, select from: stage, test!"
        exit 1
        ;;
esac

### TEST PARAMS
REMOTE_ROOT_DIR=/hdp_spool/$REMOTE_USERNAME/dataquality-${2}

echo 'DELETING PRESENT FILE'
rm -f ./dq-core/target/scala-2.10/*.jar 2> /dev/null
rm -f ./dq-core/target/universal/* 2> /dev/null
echo 'DONE!'

echo 'BUILDING ASSEMBLY...'
sbt -Denv=$1 -Dintegration=$2 "project core" universal:packageBin
echo 'DONE!'

echo 'UPLOADING FILES...'
scp  dq-core/target/universal/*.zip $REMOTE_USERNAME@$REMOTE_HOST:$REMOTE_ROOT_DIR

echo "############################################# "
echo "#   _____   ____  _   _ ______   _   _   _  # "
echo "#  |  __ \ / __ \| \ | |  ____| | | | | | | # "
echo "#  | |  | | |  | |  \| | |__    | | | | | | # "
echo "#  | |  | | |  | | . ' |  __|   | | | | | | # "
echo "#  | |__| | |__| | |\  | |____  |_| |_| |_| # "
echo "#  |_____/ \____/|_| \_|______| (_) (_) (_) # "
echo "#                                           # "
echo "############################################# "