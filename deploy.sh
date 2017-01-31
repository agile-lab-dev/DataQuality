#!/usr/bin/env bash
REMOTE_HOST=
REMOTE_USERNAME=
REMOTE_ROOT_DIR=


echo 'DELETING PRESENT FILE'
rm -f ./target/scala-2.10/*.jar 2> /dev/null

echo 'DONE!'
echo ''

echo 'BUILDING ASSEMBLY'
echo ''

sbt assembly

if [[ $? -ne 0 ]] ; then
    exit 1
fi

echo 'DONE!'
echo ''

echo 'GENERATING GIT VERSION'
echo ''

rm -f ./target/scala-2.10/git_version.info 2> /dev/null
printf "commit: " > ./target/scala-2.10/git_version.info
git rev-parse HEAD >> ./target/scala-2.10/git_version.info
printf "descr: " >> ./target/scala-2.10/git_version.info
git describe --long >> ./target/scala-2.10/git_version.info
printf "status: " >> ./target/scala-2.10/git_version.info
git status >> ./target/scala-2.10/git_version.info

echo 'UPLOADING FILES'
echo ''

scp target/scala-2.10/*.jar $REMOTE_USERNAME@$REMOTE_HOST:$REMOTE_ROOT_DIR
scp src/main/resources/log4j.properties $REMOTE_USERNAME@$REMOTE_HOST:$REMOTE_ROOT_DIR
scp src/main/resources/Conf.conf $REMOTE_USERNAME@$REMOTE_HOST:$REMOTE_ROOT_DIR
scp submit.sh $REMOTE_USERNAME@$REMOTE_HOST:$REMOTE_ROOT_DIR
scp run.sh $REMOTE_USERNAME@$REMOTE_HOST:$REMOTE_ROOT_DIR

echo 'DONE!'
echo ''

echo SHA256: `sha256sum target/scala-2.10/*.jar`

