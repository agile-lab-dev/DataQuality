#!/usr/bin/env bash
# TODO: Add credentials
REMOTE_HOST=
REMOTE_USERNAME=
BUILD_PREDICATE=

REMOTE_ROOT_DIR=

echo 'DELETING PRESENT FILE...'
rm -f ./target/scala-2.10/*.jar 2> /dev/null
rm -f ./target/universal/* 2> /dev/null

echo 'DONE!'

echo 'BUILDING ASSEMBLY...'

#sbt assembly
sbt universal:packageBin

if [[ $? -ne 0 ]] ; then
    exit 1
fi

echo 'DONE!'

echo 'GENERATING GIT VERSION...'

rm -f ./target/scala-2.10/git_version.info 2> /dev/null
printf "commit: " > ./target/scala-2.10/git_version.info
git rev-parse HEAD >> ./target/scala-2.10/git_version.info
printf "descr: " >> ./target/scala-2.10/git_version.info
git describe --long >> ./target/scala-2.10/git_version.info
printf "status: " >> ./target/scala-2.10/git_version.info
git status >> ./target/scala-2.10/git_version.info

echo 'UPLOADING FILES...'

scp  target/universal/*.zip $REMOTE_USERNAME@$REMOTE_HOST:$REMOTE_ROOT_DIR

echo "############################################# "
echo "#   _____   ____  _   _ ______   _   _   _  # "
echo "#  |  __ \ / __ \| \ | |  ____| | | | | | | # "
echo "#  | |  | | |  | |  \| | |__    | | | | | | # "
echo "#  | |  | | |  | | . ' |  __|   | | | | | | # "
echo "#  | |__| | |__| | |\  | |____  |_| |_| |_| # "
echo "#  |_____/ \____/|_| \_|______| (_) (_) (_) # "
echo "#                                           # "
echo "############################################# "

echo SHA256: `sha256sum target/scala-2.10/*.jar`