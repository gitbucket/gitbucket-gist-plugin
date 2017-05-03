# gitbucket-gist-plugin [![Build Status](https://travis-ci.org/gitbucket/gitbucket-gist-plugin.svg?branch=master)](https://travis-ci.org/gitbucket/gitbucket-gist-plugin)

This is an example of GitBucket plug-in. This plug-in provides code snippet repository like Gist.

Plugin version | GitBucket version
:--------------|:--------------------
4.8.x          | 4.11.x, 4.12.x
4.7.x          | 4.11.x
4.6.x          | 4.10.x
4.5.x          | 4.9.x
4.4.x          | 4.8.x
4.2.x, 4.3.x   | 4.2.x, 4.3.x, 4.4.x, 4.5.x, 4.6.x, 4.7.x
4.0.x          | 4.0.x, 4.1.x
3.13.x         | 3.13.x
3.12.x         | 3.12.x
3.11.x         | 3.11.x
3.10.x         | 3.10.x
3.7.x          | 3.7.x, 3.8.x, 3.9.x
3.6.x          | 3.6.x


## Installation

Download jar file from [the release page](https://github.com/gitbucket/gitbucket-gist-plugin/releases) and put into `GITBUCKET_HOME/plugins`.

**Note:** If you had used this plugin with GitBucket 3.x, it does not work after upgrading to GitBucket 4.x. Solution is below:

1. `UPDATE VERSIONS SET VERSION='2.0.0' WHERE MODULE_ID='gist';`
2. restart gitbucket
3. can open snippets page
4. `SELECT VERSION FROM VERSIONS WHERE MODULE_ID='gist'` -> `4.2.0`

See [Connect to H2 database](https://github.com/gitbucket/gitbucket/wiki/Connect-to-H2-database) to know how to execute SQL on the GitBucket database.

## Build from source

1. Hit `./sbt.sh package` in the root directory of this repository.
2. Copy `target/scala-2.12/gitbucket-gist-plugin_2.12-x.x.x.jar` into `GITBUCKET_HOME/plugins`.
3. Restart GitBucket.
