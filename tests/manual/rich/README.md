# Rich Text Area Demo

This application demonstrates capabilities of the RichTextArea control.

RichTextArea control: [RichTextArea.java](https://github.com/andy-goryachev-oracle/jfx/blob/ag.rich.text.area/modules/javafx.controls/src/main/java/javafx/scene/control/rich/RichTextArea.java)

Demonstration Application:
[RichTextAreaDemoApp.java](https://github.com/andy-goryachev-oracle/jfx/blob/ag.rich.text.area/tests/manual/rich/src/com/oracle/tools/demo/rich/RichTextAreaDemoApp.java)


## Prerequisites

JavaFX SDK is required to build the demo.  You can use a JavaFX SDK that you build or you can download the JavaFX SDK.
The latest SDK can be found here:

https://jdk.java.net/javafx21/


## Build

The tool uses `ant` to build a non-modular JAR.  You'll need to specify the path to JavaFX SDK 21+
(using absolute path, the script apparently does not understand ~ symbols):
```
ant -Djavafx.home=<JAVAFX>
```


## Run

The demo requires JDK 17+ and JavaFX 21+.

To launch, specify the path to the JavaFX SDK lib/ folder on the command line, example:

```
java -p <JAVAFX>/javafx-sdk-21/lib/ --add-modules ALL-MODULE-PATH -jar RichTextArea.jar
```


## User Preferences

Applications stores the user preferences (window position, currently selected page, etc.) in `~/.RichTextAreaDemo` directory.


