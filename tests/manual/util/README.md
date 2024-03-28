# Test Utilities

## ManualTestWindow

This facility provides a framework for manual tests to display test instructions, test pane, and Pass/Fail buttons.

A simple test would look like this:

```java
public class SampleManualTest {
     public static void main(String[] args) throws Exception {
         ManualTestWindow.builder().
             title("Sample Manual Test").
             instructions(
                 """
                 Provide
                 multi-line instructions here.
                 """
             ).
             ui(() -> createTestUI()).
             buildAndRun();
     }

     private static Node createTestUI() {
         return new Label("Test UI");
     }
}
```

Resulting application window:

![screenshot](doc/ManualTestWindow.png)

