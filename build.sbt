organization := "io.github.gitbucket"
name := "gitbucket-gist-plugin"
version := "4.22.0"
scalaVersion := "2.13.10"
gitbucketVersion := "4.38.3"

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
Compile / javacOptions ++= Seq("-target", "8", "-source", "8")

useJCenter := true
