ThisBuild / scalaVersion := "3.7.3"

lazy val `casino-bloom` = (project in file("modules/bloom"))

lazy val `casino-benchmarks` = (project in file("modules/benchmarks"))
    .enablePlugins(JmhPlugin)
    .dependsOn(`casino-bloom`)
    .settings(
      libraryDependencies ++= Seq(
        "org.openjdk.jmh" % "jmh-core"                 % "1.37",
        "org.openjdk.jmh" % "jmh-generator-annprocess" % "1.37"
      )
    )
