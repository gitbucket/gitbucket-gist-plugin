organization := "io.github.gitbucket"
name := "gitbucket-gist-plugin"
version := "4.15.0"
scalaVersion := "2.12.6"
gitbucketVersion := "4.26.0-SNAPSHOT"

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
javacOptions in compile ++= Seq("-target", "8", "-source", "8")

useJCenter := true
