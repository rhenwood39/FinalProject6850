
name := "FinalProj6850"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.5.2",
  "org.apache.spark" %% "spark-graphx" % "2.1.0",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.twitter4j" % "twitter4j-core" % "4.0.1"
  //"net.liftweb" %% "lift-json" % "2.5.1",
  //"com.fasterxml.jackson.module" % "jackson-module-scala" % "2.8.6"

)

resolvers += Resolver.mavenLocal
    