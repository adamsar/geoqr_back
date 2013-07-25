import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "GeoQR"
  val appVersion      = "1.0-SNAPSHOT"
  
  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "joda-time" % "joda-time" % "1.6.1",
    "junit" % "junit" % "4.8.1",
    "org.mongodb" %% "casbah" % "2.6.2"
  );  

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  );

}
