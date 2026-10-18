organization := "io.github.gitbucket"
name := "gitbucket-gist-plugin"
version := "4.26.0"
scalaVersion := "3.10.0"
gitbucketVersion := "4.48.0"

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
Compile / javacOptions ++= Seq("-target", "11", "-source", "11")
