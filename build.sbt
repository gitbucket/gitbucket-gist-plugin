organization := "io.github.gitbucket"
name := "gitbucket-gist-plugin"
version := "4.20.0"
scalaVersion := "2.13.5"
gitbucketVersion := "4.36.0"

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
Compile / javacOptions ++= Seq("-target", "8", "-source", "8")

useJCenter := true
