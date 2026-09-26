import org.scalajs.linker.interface.ModuleSplitStyle

ThisBuild / scalaVersion := "3.9.0"
ThisBuild / organization := "io.github.scala-tessella"

lazy val atlas = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name                            := "krotenheerdt-atlas",
    scalacOptions ++= Seq("-deprecation", "-feature", "-Werror"),
    scalafmtOnCompile               := true,
    // an ES module application, split per package for Vite's development server
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("atlas")))
    },
    libraryDependencies ++= Seq(
      "com.raquo"     %%% "laminar"     % "17.2.1",
      "org.scala-js"  %%% "scalajs-dom" % "2.8.1",
      "org.scalameta" %%% "munit"       % "1.3.6" % Test
    ),
    // the data bundle the application reads, pinned in data.version: its version becomes a constant, so every data
    // URL carries it and can be cached for good
    Compile / sourceGenerators += Def.task {
      val version = IO.read(baseDirectory.value / "data.version").trim
      require(version.matches("[0-9a-zA-Z._-]+"), s"data.version: unexpected '$version'")
      val out     = (Compile / sourceManaged).value / "atlas" / "DataVersion.scala"
      IO.write(
        out,
        s"""package atlas
           |
           |/** The data bundle this build reads (data.version). */
           |object DataVersion:
           |  val value: String = "$version"
           |""".stripMargin
      )
      Seq(out)
    }.taskValue
  )
