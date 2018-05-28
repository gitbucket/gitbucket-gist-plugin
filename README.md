# gitbucket-gist-plugin [![Build Status](https://travis-ci.org/gitbucket/gitbucket-gist-plugin.svg?branch=master)](https://travis-ci.org/gitbucket/gitbucket-gist-plugin)

This is a GitBucket plug-in which provides code snippet repository like Gist.

Plugin version | GitBucket version
:--------------|:--------------------
4.15.x         | 4.25.x -
4.13.x, 4.14.x | 4.23.x -
4.12.x         | 4.21.x -
4.11.x         | 4.19.x -
4.10.x         | 4.15.x - 4.18.x
4.9.x          | 4.14.x
4.8.x          | 4.11.x - 4.13.x
4.7.x          | 4.11.x
4.6.x          | 4.10.x
4.5.x          | 4.9.x
4.4.x          | 4.8.x
4.2.x, 4.3.x   | 4.2.x - 4.7.x
4.0.x          | 4.0.x, 4.1.x
3.13.x         | 3.13.x
3.12.x         | 3.12.x
3.11.x         | 3.11.x
3.10.x         | 3.10.x
3.7.x          | 3.7.x - 3.9.x
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

Run `sbt assembly` and copy generated `/target/scala-2.12/gitbucket-gist-plugin-assembply-x.x.x.jar` to `~/.gitbucket/plugins/` (If the directory does not exist, create it by hand before copying the jar), or just run `sbt install`.
