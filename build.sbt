
name := "FinalProj6850"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.5.2",
  "org.apache.spark" %% "spark-graphx" % "1.5.2",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"

)

resolvers += Resolver.mavenLocal
    