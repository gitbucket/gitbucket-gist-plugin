organization := "io.github.gitbucket"
name := "gitbucket-gist-plugin"
version := "4.24.0"
scalaVersion := "2.13.18"
gitbucketVersion := "4.45.0"

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
Compile / javacOptions ++= Seq("-target", "11", "-source", "11")
