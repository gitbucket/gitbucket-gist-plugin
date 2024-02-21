organization := "io.github.gitbucket"
name := "gitbucket-gist-plugin"
version := "4.23.0"
scalaVersion := "2.13.13"
gitbucketVersion := "4.40.0"

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
Compile / javacOptions ++= Seq("-target", "11", "-source", "11")

useJCenter := true
