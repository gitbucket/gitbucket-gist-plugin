organization := "io.github.gitbucket"
name := "gitbucket-gist-plugin"
version := "4.18.0"
scalaVersion := "2.12.8"
gitbucketVersion := "4.31.2"

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
javacOptions in compile ++= Seq("-target", "8", "-source", "8")

useJCenter := true
