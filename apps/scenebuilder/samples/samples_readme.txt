JAVAFX SCENE BUILDER SAMPLES README

Contents

What's in the samples zip file?
What do I need to set up my environment?
How do I build and run the samples?
How do I build and run the sample projects in NetBeans IDE?
How do I edit the sample projects in JavaFX Scene Builder?
Sample Descriptions


===============================
What's in the samples zip file?
===============================

The samples zip file contain samples that you can build and run, 
either from the command line or in NetBeans IDE.
All of the samples contain one or more FXML files. FXML is the underlying format of JavaFX Scene Builder.

Extracting the samples zip file produces the following directory structure:

--<Sample1>
    --nbproject
    --src
    --build.xml
    --manifest.mf 
--<Sample2>
    ...
 
    
========================================
What do I need to set up my environment?
========================================

To build and run the samples from the command line, you need the following environment:

- Ant 1.8 or higher.
- A supported version** of the JavaFX SDK that matches the samples zip 
  file version. The JavaFX SDK includes the JavaFX Runtime.
- A supported version** of the Java Development Kit (JDK).
  The JDK includes the JRE.


To build and run the samples in NetBeans IDE, you need the following 
environment:

- A supported version** of the JavaFX SDK that matches the samples zip file version.
- A supported version** of the JDK.
- A supported version** of NetBeans IDE.


To open the samples projects in JavaFX Scene Builder, you need the following 
environment:

- A version of JavaFX Scene Builder that matches the samples zip file version.
- A supported version** of the JRE.


**To find the supported versions of operating system, Java platform, JavaFX Platform
and NetBeans IDE for a particular JavaFX Scene Builder release, see the release 
documentation page at
https://docs.oracle.com/javafx/release-documentation.html 

To get the latest release of JavaFX Scene Builder, go to 
http://www.oracle.com/technetwork/java/javafx/downloads/index.html


=========================================================
How do I build and run the samples from the command line?
=========================================================

Use the following command to build and run the application:

ant -Dplatforms.Default_JavaFX_Platform.home=<JAVA_HOME>
    -f <SAMPLE_NAME>/build.xml
    <TARGET>

The main values for <TARGET> are clean, jar, run.
Replace <TARGET> with -projecthelp to get a list of available targets.

The following example shows how to build and run the HelloWorld sample on Windows:
ant -Dplatforms.JDK_1.8.home="C:\Program Files\Java\jdk1.8.0" -f HelloI18N\build.xml run

The following example shows how to build and run HelloI18N on Mac (where Java 8 from Oracle is installed at its default location):
ant -Dplatforms.JDK_1.8.home=/usr -f HelloI18N/build.xml run


===========================================================
How do I build and run the sample projects in NetBeans IDE?
===========================================================

To build and run a sample project in NetBeans IDE:

1. Click Open Project in the toolbar, or on the File menu 
   choose Open Project.
   
2. Navigate to the location in which you unzipped the samples, select 
   one of the sample directories, then click Open.
   
3. To run the application in NetBeans IDE, in the Project pane, right-click 
   the project and choose Run.

You can change the mode in which the application runs in the IDE by selecting the Run 
category in Project Properties and choosing Standalone, as Web Start, or in the Browser as the Run setting. 


==========================================================
How do I edit the sample projects in JavaFX Scene Builder?
==========================================================

1. In JavaFX Scene Builder, on the File menu choose Open.

2. Navigate to the location in which you unzipped the samples, then in the src
   directory look for one or more file names with the extension .fxml.
   
3. Select an fxml file, then click Open.


===================
Sample Descriptions
===================

The following samples are included in the zip file.

---------
HelloI18N

This sample behaves as HelloWorld one.
In addition it is internationalized and localized in English and French.
From within Scene Builder you can preview the application in English, in French or view the raw key value used for the button's text.


-----
Login

A slightly more complex sample that includes some assets, such as Button, TextField,
and CheckBox, that have some logic associated with them.
It demonstrates a simple login system and user session, where users can log in
and edit their profile information.
The default login/password credentials are demo/demo.


-----------------
IssueTrackingLite

This sample contains the starting point for the IssueTrackingLite sample application, which is 
created using the Getting Started with JavaFX Scene Builder document. It is a complete NetBeans 
project and includes a completed UI layout that the tutorial will instruct you to rename so you 
can replace it with your own. You need to follow the tutorial, which will lead you through creating 
the UI layout from the start and connecting it to the controller class. The renamed UI layout provided with
this sample can be used for your reference as you create your own IssueTrackingLite.fxml layout file.


-----------------
IssueTrackingBiDi

The IssueTrackingBiDi sample demonstrates the use of the bi-directional rendering feature.
The sample is a localized version of the IssueTrackingLite sample and uses two left-to-right languages, English and French,
and two right-to-left languages, Arabic and Hebrew. The whole application's flow follows the flow of the language used.
An example of how to use the DatePicker control is also included in the sample.


------------
UnlockCustom

A sample where after an initial click to unveil a key pad you need to enter a valid PIN, 1234, to remove the lock.
This application uses GridPane component as well as an animation.
UnlockCustom uses a key pad defined as a custom type.
The key pad is defined as a custom type named Keypad.
From within Unlock.fxml we refer to the key pad by its Java class name, Keypad, thanks a statement of the form <Keypad .../>.


--------------
HelloSwingNode

A sample that demonstrates a Swing asset in action, a JButton, within the JavaFX application.
Two buttons are live in this application, a JavaFX Button and a Swing JButton.
The user can click on either the JavaFX one or the Swing one to respectively disable or enable the other button.


----------
AirportApp

A sample application with an aeronautical theme. The user can select an airport from the ListView
which is then located on a map (utilising a ScrollPane to animate to the correct location).
There is a detailed video showing how this app is constructed, including applying CSS and connecting code.
You can find the video on the Java YouTube channel at: http://youtu.be/ij0HwRAlCmo


-------------------------------------------------------------------- 
Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.

