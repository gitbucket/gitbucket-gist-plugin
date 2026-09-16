organization := "io.github.gitbucket"
name := "gitbucket-gist-plugin"
version := "4.25.0"
scalaVersion := "3.9.0"
gitbucketVersion := "4.47.0"

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
Compile / javacOptions ++= Seq("-target", "11", "-source", "11")
