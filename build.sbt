organization := "io.github.gitbucket"
name := "gitbucket-gist-plugin"
version := "4.23.0"
scalaVersion := "2.13.16"
gitbucketVersion := "4.42.1"

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
Compile / javacOptions ++= Seq("-target", "11", "-source", "11")

useJCenter := true
