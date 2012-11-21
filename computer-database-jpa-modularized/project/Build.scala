import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "computer-database-jpa-modularized"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    javaCore,
    javaJdbc,
    javaJpa,
    "org.hibernate" % "hibernate-entitymanager" % "3.6.9.Final"
  )

  val model = play.Project(
    appName + "-model", appVersion, appDependencies, path = file("modules/model")
  ).settings(
    ebeanEnabled := false
  )

  val site = play.Project(
    appName + "-site", appVersion, appDependencies, path = file("modules/site")
  ).dependsOn(
    model
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
  ).dependsOn(
    model, site
  ).aggregate( 
    model, site
  )

}
