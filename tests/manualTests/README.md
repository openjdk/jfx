# Manual Tests

This directory contains the manual tests and a JavaFX application that can be used to run these tests in any order. 



## Extend ManualTestWindow

Writing a manual test is easy using the `ManualTestWindow` class, as it provides some basic features found in pretty much
any test:

- instruction text area
- Pass / Fail buttons
- taking a screenshot when the operator clicks on the "Fail" button
- skipping tests on specific platforms

Example:

```java
public class ManualTestExample extends ManualTestWindow {
    public ManualTestExample() {
        super(
            "Manual Test Example",
            """
            Instructions:
            1. verify that a button named "Test" is present
            2. press the button
            3. verify that the button can be pressed
            """,
            400, 250
        );
     }

     public static void main(String[] args) throws Exception {
         launch(args);
     }

     @Override
     protected Node createContent() {
         return new Button("Test");
     }
}
```


## Test Runner and the Test Plan

The manual tests can be run by the automated harness such as [jtreg](https://openjdk.org/jtreg/), or using
a simple test runner `TestRunnerApp` which is wired into the `manualTests.jar`.

The `TestRunnerApp` loads `test-plan.txt` file which contains the list of tests to run, or the user can load
a different test plan.



## Build

To build the manual tests (and the `manualTests.jar`), execute the main gradle build:

```console
gradle manualTests
```


## Run

To launch the `TestRunnerApp` UI:

```console
cd tests/manualTests
java @../../build/run.args -jar build/libs/manualTests.jar
```

An individual test can be run using the following command:

```console
java @../../build/run.args -cp build/libs/manualTests.jar com.oracle.test.manual.text.EmojiTest
```

