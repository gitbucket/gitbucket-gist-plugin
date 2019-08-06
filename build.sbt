organization := "io.github.gitbucket"
name := "gitbucket-gist-plugin"
version := "4.18.0-SNAPSHOT"
scalaVersion := "2.13.0"
gitbucketVersion := "4.32.0"

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
javacOptions in compile ++= Seq("-target", "8", "-source", "8")

useJCenter := true
