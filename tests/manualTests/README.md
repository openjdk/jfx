@../../build/run.args# Manual Tests

This directory contains the manual tests and a JavaFX application that can be used to run these tests in any order. 



## Build

To build the manual tests, run the main gradle build:

```console
gradle manualTests
```


## Run

To launch the test runner UI, specify the path to the JavaFX SDK lib/ folder on the command line, example:

```console
cd tests/manualTests
java @../../build/run.args -jar build/libs/manualTests.jar
```

An individual test can be run using the following command:

```console
java @../../build/run.args -cp build/libs/manualTests.jar com.oracle.test.manual.text.EmojiTest
```


