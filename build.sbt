organization := "io.github.gitbucket"
name := "gitbucket-gist-plugin"
version := "4.19.0"
scalaVersion := "2.13.3"
gitbucketVersion := "4.34.0"

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
javacOptions in compile ++= Seq("-target", "8", "-source", "8")

useJCenter := true
