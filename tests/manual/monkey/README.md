# Monkey Tester

This is a testing tool developed to support ad-hoc testing of JavaFX controls.

![screenshot](doc/screenshot.png)


## Prerequisites

JavaFX SDK is required to build the tool.  The latest SDK can be found here:

https://jdk.java.net/javafx21/


## Build

The tool uses `ant` to build a non-modular JAR.  You'll need to specify the path to JavaFX SDK 20+:
```
ant -Djavafx.home=<dir>
```


## Run

The tool requires JDK 17+ and JavaFX 20+.

To launch, specify the path to the javaFX SDK lib/ folder on the command line, example:

```
java -p javafx-sdk-20/lib/ --add-modules ALL-MODULE-PATH -jar MonkeyTester.jar
```


## Contact

andy.goryachev@oracle.com
