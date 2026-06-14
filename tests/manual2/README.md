# Manual Tests

This directory contains the manual tests and a JavaFX application that can be used to run these tests in any order. 



## Build

The tool uses `ant` to build a non-modular JAR.  You'll need to specify the path to JavaFX SDK
using absolute path:
```
ant -Djavafx.home=<JAVAFX>
```


## Run

To launch, specify the path to the JavaFX SDK lib/ folder on the command line, example:

```
java -p "../../build/sdk/lib" --enable-native-access=javafx.graphics --add-modules ALL-MODULE-PATH -jar dist/ManualTests.jar
```


## TODO

The ant build should be wired into the main `build.gradle`.
