organization := "io.github.gitbucket"
name := "gitbucket-gist-plugin"
version := "4.14.0"
scalaVersion := "2.12.4"
gitbucketVersion := "4.23.1"

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
javacOptions in compile ++= Seq("-target", "8", "-source", "8")

useJCenter := true
