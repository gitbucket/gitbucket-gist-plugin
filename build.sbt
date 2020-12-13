organization := "io.github.gitbucket"
name := "gitbucket-gist-plugin"
version := "4.20.0"
scalaVersion := "2.13.3"
gitbucketVersion := "4.35.0"

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
javacOptions in compile ++= Seq("-target", "8", "-source", "8")

useJCenter := true
