name := "cockroach-test"

version := "0.1"

scalaVersion := "2.12.4"

enablePlugins(GatlingPlugin)

javaOptions in Gatling := overrideDefaultJavaOptions("-Xms1024m", "-Xmx2048m")

libraryDependencies ++= Seq(
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.3.0",
  "io.gatling" % "gatling-test-framework" % "2.3.0",
  "org.jdbi" % "jdbi3-bom" % "3.0.0-rc1",
  "org.jdbi" % "jdbi3-core" % "3.0.0-rc1",
  "org.postgresql" % "postgresql" % "42.1.4",
  "com.zaxxer" % "HikariCP" % "2.7.3"
)
