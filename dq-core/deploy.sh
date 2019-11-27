#!/usr/bin/env bash

# $1 - build environment
# $2 - integration project: xsell, npe, gcif, ...
echo "BUILD ENV"$1
case $1 in
    stage)
        REMOTE_HOST=hdpqemu01.internal.unicreditgroup.eu
        REMOTE_USERNAME=tubd2899
        ;;
    test)
        REMOTE_HOST=hdptemu02.internal.unicreditgroup.eu
        REMOTE_USERNAME=tud2q799
        ;;
    dev)
        REMOTE_HOST=server07.cluster01.atscom.it
        REMOTE_USERNAME=alessandro.marino
        ;;
    *)
        echo "Unknown environment! Please, select from: stage, test!"
        exit 1
        ;;
esac

### TEST PARAMS
#REMOTE_ROOT_DIR=/hdp_spool/$REMOTE_USERNAME/dq-${2}-test
REMOTE_ROOT_DIR=/home/$REMOTE_USERNAME
echo 'DELETING PRESENT FILE'
rm -f ./dq-core/target/scala-2.10/*.jar 2> /dev/null
rm -f ./dq-core/target/universal/* 2> /dev/null
echo 'DONE!'

echo 'BUILDING ASSEMBLY...'
sbt -Denv=$1 -Dintegration=$2 "project core" universal:packageBin
echo 'DONE!'

#echo 'GENERATING GIT VERSION...'
#rm -f ./dq-core/target/scala-2.10/git_version.info 2> /dev/null
#printf "commit: " > ./dq-core/target/scala-2.10/git_version.info
#git rev-parse HEAD >> ./dq-core/target/scala-2.10/git_version.info
#printf "descr: " >> ./dq-core/target/scala-2.10/git_version.info
#git describe --long >> ./dq-core/target/scala-2.10/git_version.info
#printf "status: " >> ./dq-core/target/scala-2.10/git_version.info
#git status >> ./dq-core/target/scala-2.10/git_version.info
#echo 'DONE!'

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