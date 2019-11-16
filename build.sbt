organization := "tv.cntt"
name         := "sclasner"
version      := "1.8.0-SNAPSHOT"

crossScalaVersions := Seq("2.13.1", "2.12.10")
scalaVersion       := "2.13.1"

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

// Skip API doc generation to speedup "publishLocal" while developing.
// Comment out this line when publishing to Sonatype.
publishArtifact in (Compile, packageDoc) := false
