#!/bin/sh
./sbt.sh package
cp target/scala-2.11/gitbucket-gist-plugin_2.11-2.0.0-SNAPSHOT.jar ~/.gitbucket/plugins/
