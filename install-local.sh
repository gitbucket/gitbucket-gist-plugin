#!/bin/sh
./sbt.sh package
cp target/scala-2.11/gitbucket-gist-plugin_2.11-3.13.0.jar ~/.gitbucket/plugins/
