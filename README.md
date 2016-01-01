# gitbucket-gist-plugin

This is an example of GitBucket plug-in. This plug-in provides code snippet repository like Gist.

Plugin version | GitBucket version
:--------------|:--------------------
3.11.x         | 3.11.x
3.10.x         | 3.10.x
3.7.x          | 3.7.x, 3.8.x, 3.9.x
3.6.x          | 3.6.x


## Installation

Download jar file from [the release page](https://github.com/gitbucket/gitbucket-gist-plugin/releases) and put into `GITBUCKET_HOME/plugins`.

## Build from source

1. Hit `./sbt.sh package` in the root directory of this repository.
2. Copy `target/scala-2.11/gitbucket-gist-plugin_2.11-x.x.x.jar` into `GITBUCKET_HOME/plugins`.
3. Restart GitBucket.
