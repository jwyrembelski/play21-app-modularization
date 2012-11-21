play21-app-modularization
=========================

An investigation into breaking a Play 2.1 app into modules.  Basic modules to include: 

* common model
* tags
* layouts
* web app

Given Play's very slow rebuild time with larger (all-in-one) projects, this investigation should provide the average user a template for setting up their project to avoid the problem entirely.

With the all-in-one sample project "computer-database-jpa" from the Play 2.1RC1 distribution, I begin.

Steps to Reproduce
==================

## 0) Ensure we're in the same directory

	export PROJECT_HOME=/path/to/anywhere
	cd $PROJECT_HOME

## 1) Setup project
Create a Java project and remove all the sample bits.

	play new computer-database-jpa-modularized
	cd computer-database-jpa-modularized
	rm -rf README conf/routes app public test

Prune the unnecessary bits from the application.conf file. After this step mine looks like this

	application.secret="JMj]sjv6k<_w=rI2=HpD]@X9eGZG4Z89;1Yfn8ik_IEuvSlipdpjKduMgv5p;vtE"
	application.langs="en"
	logger.root=ERROR
	logger.play=INFO
	logger.application=DEBUG

## 2) Create common model module
Create the modules directory and the common model module.

	mkdir modules
	cd modules
	play new model
	cd model
	rm -rf README conf/routes app/* public test/*
	rsync -va $PLAY_HOME/samples/java/computer-database-jpa/app/models app/
	rsync -va $PLAY_HOME/samples/java/computer-database-jpa/test/ModelTest.java test/
	rsync -va $PLAY_HOME/samples/java/computer-database-jpa/conf/META-INF conf/
	rsync -va $PLAY_HOME/samples/java/computer-database-jpa/conf/evolutions conf/

Prepare the application.conf file. After this step mine looks like this:

	# Common Model Module
	
	application.secret="`41erREcA=1T9E<VZaYb6xOdRTt`HS1F?VOp=eY8t;xqYaLm<WvMf?Wp9;5oy6yV"
	application.langs="en"
	
	db.default.driver=org.h2.Driver
	db.default.url="jdbc:h2:mem:play"
	db.default.jndiName=DefaultDS
	jpa.default=defaultPersistenceUnit
	
	logger.root=ERROR
	logger.play=INFO
	logger.application=DEBUG

Prepare the Build.scala file.  Ensure that the appDependencies and project config match those of the sample project. 

Verify that the module builds and the tests run.

	play compile
	play test

## 3) Install the module within the project
Following the documentation at

* [http://www.playframework.org/documentation/2.0.4/SBTSubProjects](http://www.playframework.org/documentation/2.0.4/SBTSubProjects)
* [http://www.scala-sbt.org/release/docs/Getting-Started/Multi-Project](http://www.scala-sbt.org/release/docs/Getting-Started/Multi-Project)

I removed the Build.scala from the model sub-project and installed the important bits in the project. 

	cd $PROJECT_HOME/computer-database-jpa-modularized
	rm modules/model/project/Build.scala

The **$PROJECT_HOME/computer-database-jpa-modularized/project/Build.scala** now looks like this:

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
	
	  val	model = play.Project(
	    appName + "-model", appVersion, appDependencies, path = file("modules/model")
	  )
	
	  val main = play.Project(appName, appVersion, appDependencies).settings(
	     ebeanEnabled := false     
	  ).dependsOn(
	    model
	  ).aggregate( 
	    model 
	  )
	
	}

thoroughly cleaning out all possible temporary files as follows

	find . -type d -name target |xargs rm -rf 
	find . -type d -name logs |xargs rm -rf 
	find . -type d -name public |xargs rm -rf 

and executing the test cases again

	play compile test

fails with

	[info] ModelTest
	[error] Test ModelTest.findById failed: java.lang.RuntimeException: No JPA EntityManagerFactory configured for name [default]

Which I take to mean that I must also bring some portion or all of what is found in **module/model/conf/** up to the project level. My first guess is that it is the application.conf in that it isn't "standard jar material".

Moving

	db.default.driver=org.h2.Driver
	db.default.url="jdbc:h2:mem:play"
	db.default.jndiName=DefaultDS
	jpa.default=defaultPersistenceUnit

up does the trick. The tests now pass. It seems logical that nothing in the sub-project's application.conf files will be used by the project, so I delete the file entirely.

## 4) Install the site module
Setup the new module

	cd $PROJECT_HOME/computer-database-jpa-modularized/modules/
	play new site
	rm -rf README conf/routes conf/application.conf app/* test/* public/* project

Install the necessary bits from the sample

	rsync -va $PLAY_HOME/samples/java/computer-database-jpa/app/views app/
	rsync -va $PLAY_HOME/samples/java/computer-database-jpa/app/controllers app/
	rsync -va $PLAY_HOME/samples/java/computer-database-jpa/test/FunctionalTest.java test/
	rsync -va $PLAY_HOME/samples/java/computer-database-jpa/test/IntegrationTest.java test/

Edit **project/Build.scala** by adding a definition for the site module

	  val site = play.Project(
	    appName + "-site", appVersion, appDependencies, path = 	file("modules/site")
	  ).dependsOn(
	    model
	  )

and adding the site module to the project dependencies 

	  val main = play.Project(appName, appVersion, 	appDependencies).settings(
	  ).dependsOn(
	    model, site
	  ).aggregate( 
	    model, site
	  )

That was easy!