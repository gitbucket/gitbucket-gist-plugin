organization := "io.github.gitbucket"
name := "gitbucket-gist-plugin"
version := "4.16.0"
scalaVersion := "2.12.7"
gitbucketVersion := "4.30.0-SNAPSHOT"

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
javacOptions in compile ++= Seq("-target", "8", "-source", "8")

useJCenter := true
