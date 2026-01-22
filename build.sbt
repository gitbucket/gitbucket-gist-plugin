organization := "io.github.gitbucket"
name := "gitbucket-gist-plugin"
version := "4.24.0"
scalaVersion := "3.8.1"
gitbucketVersion := "4.45.0"

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
Compile / javacOptions ++= Seq("-target", "11", "-source", "11")
