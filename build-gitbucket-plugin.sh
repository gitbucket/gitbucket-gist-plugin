#!/bin/sh
rm -rf gitbucket
git clone https://github.com/gitbucket/gitbucket.git
cd gitbucket

GITBUCKET_VERSION=`cat build.sbt | grep "GitBucketVersion =" | sed 's/^.*"\(.*\)".*$/\1/'`
echo "GITBUCKET_VERSION: $GITBUCKET_VERSION"
sbt publishLocal

cd ..
sbt -Dgitbucket.version=$GITBUCKET_VERSION test
