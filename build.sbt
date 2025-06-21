organization := "tv.cntt"
name         := "sclasner"
version      := "2.0.0-SNAPSHOT"

scalaVersion := "3.7.1"

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

// Skip API doc generation to speedup "publishLocal" while developing.
// Comment out this line when publishing to Sonatype.
Compile / packageDoc / publishArtifact := false
