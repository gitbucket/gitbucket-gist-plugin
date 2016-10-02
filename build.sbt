val Organization = "io.github.gitbucket"
val ProjectName = "gitbucket-gist-plugin"
val ProjectVersion = "4.3.0"

lazy val root = (project in file(".")).enablePlugins(SbtTwirl)

organization := Organization
name := ProjectName
version := ProjectVersion
scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "io.github.gitbucket" %% "gitbucket"          % "4.5.0" % "provided",
  "com.typesafe.play"   %% "twirl-compiler"     % "1.0.4" % "provided",
  "javax.servlet"        % "javax.servlet-api"  % "3.1.0" % "provided"
)

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
javacOptions in compile ++= Seq("-target", "7", "-source", "7")
