JAVAFX SAMPLES README

Contents:

What's in the samples zip file?
What do I need to set up my environment?
How do I run the prebuilt samples?
How do I run the sample projects in NetBeans IDE?
What are the other ways I can package the samples?
Sample Descriptions


===============================
What's in the samples zip file?
===============================

The samples zip file contains prebuilt samples that you can run, plus NetBeans
project files for each sample.

Extracting the zip file produces the following directory structure:

--src  (Contains a NetBeans project for each sample)
    --<Sample1>
	--nbproject
	--src
	--build.xml
	--manifest.mf
    --<Sample2>
	...
<sample1>.jar	(Runs the sample as a standalone application)
<sample2>.jar
 ...


========================================
What do I need to set up my environment?
========================================

To run the samples, you need the following environment:

- A supported version** of the Java Development Kit (JDK) or Java Runtime
  Environment (JRE). The JDK includes the JRE.


To open the samples projects in NetBeans IDE, you need the following
environment:

- A supported version** of the JDK.
- A supported version** of NetBeans IDE.

**To find information about the supported versions of operating system and browser
for a particular Java release, see
http://www.oracle.com/technetwork/java/javase/downloads/index.html


==================================
How do I run the prebuilt samples?
==================================

To run as a standalone application, double-click the JAR file.


=================================================
How do I run the sample projects in NetBeans IDE?
=================================================

The following procedure assumes you have already extracted the samples zip
file.

To run the sample projects:

1. In NetBeans IDE, click Open Project in the toolbar, or on the File menu,
   select Open Project.
2. Navigate to the location in which you unzipped the samples, and in the src
   directory, select a project, then click Open.
3. To run the application in NetBeans IDE, in the Project pane, right-click
   the project and choose Run.


==================================================
What are the other ways I can package the samples?
==================================================

You can also package any of the samples as an applet, JNLP, or native bundle 
that includes an installer and a copy of the JRE for execution in an environment
that does not have JavaFX installed. See https://docs.oracle.com/javafx for 
additional information about deploying JavaFX application.


===================
Sample Descriptions
===================

The following samples are included in the zip file.

---------
Ensemble8

A gallery of sample applications that demonstrate a large variety of JavaFX 
features, including animation, charts, and controls. For each sample, you 
can do the following on ALL platforms:
  - View and interact with the running sample.
  - Read its description.
You can do the following for each sample on desktop platforms only:
  - Copy its source code.
  - For several samples, you can adjust the properties of the sample components. 
  - If you are connected to the internet, you can also follow links to the 
    relevant API documentation.

Ensemble8 also runs with JavaFX for ARM.

-------------
MandelbrotSet

A sample application that demonstrates advantages of parallel execution done 
using Java Parallel API. The application renders an image using Mandelbrot set 
algorithm and provides intuitive navigation within the range of input parameters. 
More information is available in index.html file inside the MandelbrotSet folder.

------
Modena

A sample application that demonstrates the look and feel of UI components using 
the Modena theme. It gives you the option to contrast Modena and Caspian themes, 
and explore various aspects of these themes.


------------
3DViewer

3DViewer is a sample application that allows the user to navigate and examine a 
3D scene with a mouse or a trackpad. 3DViewer has importers for a subset of the 
features in OBJ and Maya files. The ability to import animation is also 
provided for Maya files. (Note that in the case of Maya files, construction 
history should be deleted on all the objects when saving as a Maya file.) 
3DViewer also has the ability to export the contents of the scene as Java or 
FXML files.


--------------------------------------------------------------------
Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
