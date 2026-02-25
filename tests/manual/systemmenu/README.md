# Compiling and Running

To compile and run the tests, run the following command (You do need to run the gradle build first):

```
java @../../../build/run.args MacOSSystemMenuMultiWindowTest.java
```

The tests are used to verify that the system menu bar works as expected.
For this purpose the window itself shows the expected content of the system menu bar inside a text area.
This content should be compared with the actual system menu that is attached to the top of the screen.
If the content of the text area and the system menu bar match, the test can be considered successful.