# Release Notes for JavaFX 11

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

As of JDK 11 the JavaFX modules are delivered separately from the JDK. These release notes cover the standalone JavaFX 11 release. JavaFX 11 requires either JDK 10 (which must be an OpenJDK build) or JDK 11. JDK 11 is recommended.

## Important Changes

### Running JavaFX applications

Now that the JDK no longer includes JavaFX, it is necessary to explicitly include the JavaFX modules that your application uses. Please refer to the [Getting Started with JavaFX 11](https://openjfx.io/openjfx-docs/) page for instructions.


### Add APIs to customize step repeat timing for Spinner control

The default duration that the mouse has to be pressed on a Spinner control arrow button before the value steps is modified in JavaFX 11. Two new properties, "initialDelay" and "repeatDelay", have been added to configure this behavior. 

  initialDelay: The duration that the mouse has to be pressed on an arrow button before the next value steps. The default is now 300 msec.

  repeatDelay: The duration that the mouse has to be pressed for each successive step after the first value steps. The default is now 60 msec.

See [JDK-8167096](https://bugs.openjdk.java.net/browse/JDK-8167096) for more information.


### Standalone javafx modules no longer have permissions by default

The javafx.* modules are now loaded by the application class loader and no longer have permissions by default. Applications that want to run with a security manager enabled will need to specify a custom policy file, using "-Djava.security.policy", granting all permissions to each of the javafx.* modules.
See [JDK-8210617](https://bugs.openjdk.java.net/browse/JDK-8210617) for more information.


### Switch default GTK version to 3

JavaFX will now use GTK 3 by default on Linux platforms where the gtk3 library is present. Prior to JavaFX 11, the GTK 2 library was the default. This matches the default for AWT in JDK 11. See [JDK-8198654](https://bugs.openjdk.java.net/browse/JDK-8198654) for more information.


## New Features

The following notes describe some of the enhancements in JavaFX 11. See the table at the end of the release notes for a complete list.

### FX Robot API

Public FX Robot API was added to support simulating user interaction such as typing keys on the keyboard and using the mouse as well as capturing graphical information. See [JDK-8090763](https://bugs.openjdk.java.net/browse/JDK-8090763) for more information.

## Removed Features and Options

### Remove support for libavcodec 53 and 55

FX Media support for libavcodec 53 and 55 was removed. These libraries are not present on supported Linux platforms by default, and are no longer needed. See [JDK-8194062](https://bugs.openjdk.java.net/browse/JDK-8194062) for more information.


## Known Issues

### JavaFX crashes with OpenJDK 11 on Ubuntu 18.04 with Wayland

JavaFX crashes on Ubuntu 18.04 Linux machines when the XWayland window server is enabled. This happens whenever the FX window toolkit code uses GTK 3 on Linux, which is the default as of JavaFX 11.

The recommended workaround is to use the Xorg server instead of the Wayland server when running JavaFX applications. Note that Wayland is not supported by JDK 10 or JDK 11.

An alternative workaround is to explicitly force GTK 2 by passing the following system property on the command line:

```
    java -Djdk.gtk.version=2 ...
```

See [JDK-8210411](https://bugs.openjdk.java.net/browse/JDK-8210411) for more information.

### Swing interop requires qualified exports when run with JDK 10

To run FX / Swing interop applications using JavaFX 11 with an OpenJDK 10 release, the following four qualified exports must be added to the `java` command line.

```
--add-exports=java.desktop/java.awt.dnd.peer=javafx.swing
--add-exports=java.desktop/sun.awt=javafx.swing
--add-exports=java.desktop/sun.awt.dnd=javafx.swing
--add-exports=java.desktop/sun.swing=javafx.swing
```

See [JDK-8210615](https://bugs.openjdk.java.net/browse/JDK-8210615) for more information.


### Swing interop fails when run with a security manager with standalone SDK

FX / Swing interop applications will fail when run with a security manager enabled. An application that uses either JFXPanel or SwingNode must run without a security manager enabled.
See [JDK-8202451](https://bugs.openjdk.java.net/browse/JDK-8202451) for more information.


### Swing interop fails when using a minimal jdk image created with jlink

A minimal Java image created using jlink that includes the javafx.swing module from the JavaFX 11 jmods bundle will fail to run FX / Swing interop applications. For example, an image created as follows will not work:

```
    jlink --output myjdk --module-path javafx-jmods-11 \
        --add-modules java.desktop,javafx.swing,javafx.controls
```

The javafx.swing module depends on a new jdk.unsupported.desktop module in JDK 11 that must either be explicitly added or included via the `--bind-services` option.

Workaround: create your image using one of the following two methods:

```
    jlink --output myjdk --module-path javafx-jmods-11 \
        --add-modules java.desktop,javafx.swing,javafx.controls,jdk.unsupported.desktop

    jlink --output myjdk --bind-services --module-path javafx-jmods-11 \
        --add-modules java.desktop,javafx.swing,javafx.controls
```

See [JDK-8210759](https://bugs.openjdk.java.net/browse/JDK-8210759) for more information.


## List of Fixed Bugs

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8203345](https://bugs.openjdk.java.net/browse/JDK-8203345)|Memory leak in VirtualFlow when screen reader is enabled|accessibility
[JDK-8204336](https://bugs.openjdk.java.net/browse/JDK-8204336)|Platform.exit() throws ISE when a nested event loop is active|application-lifecycle
[JDK-8089454](https://bugs.openjdk.java.net/browse/JDK-8089454)|[HTMLEditor] selection removes CENTER alignment|controls
[JDK-8154039](https://bugs.openjdk.java.net/browse/JDK-8154039)|Memory leak when selecting a tab which is not contained in TabPane::getTabs()|controls
[JDK-8157690](https://bugs.openjdk.java.net/browse/JDK-8157690)|[TabPane] Sorting tabs makes tab selection menu empty|controls
[JDK-8165459](https://bugs.openjdk.java.net/browse/JDK-8165459)|HTMLEditor: clipboard toolbar buttons are disabled unexpectedly|controls
[JDK-8185854](https://bugs.openjdk.java.net/browse/JDK-8185854)|NPE on non-editable ComboBox in TabPane with custom Skin|controls
[JDK-8187432](https://bugs.openjdk.java.net/browse/JDK-8187432)|ListView: EditEvent on start has incorrect index|controls
[JDK-8192800](https://bugs.openjdk.java.net/browse/JDK-8192800)|Table auto resize ignores column resize policy|controls
[JDK-8193311](https://bugs.openjdk.java.net/browse/JDK-8193311)|[Spinner] Default button not activated on ENTER|controls
[JDK-8193495](https://bugs.openjdk.java.net/browse/JDK-8193495)|TabPane does not update correctly tab positions in the header area after a quick remove and add operations|controls
[JDK-8194913](https://bugs.openjdk.java.net/browse/JDK-8194913)|Focus traversal is broken if a Pane is added to a ToolBar|controls
[JDK-8196827](https://bugs.openjdk.java.net/browse/JDK-8196827)|test.javafx.scene.control.ComboBoxTest - generates NullPointerException|controls
[JDK-8197846](https://bugs.openjdk.java.net/browse/JDK-8197846)|ComboBox: becomes unclickable after removal and re-adding|controls
[JDK-8197985](https://bugs.openjdk.java.net/browse/JDK-8197985)|Pressing Shift + DOWN in ListView causes Exception to be thrown|controls
[JDK-8200285](https://bugs.openjdk.java.net/browse/JDK-8200285)|TabDragPolicy.REORDER prevents ContextMenu from showing|controls
[JDK-8201285](https://bugs.openjdk.java.net/browse/JDK-8201285)|DateCell text color are not updated correctly when DateCell with disable = true is reused|controls
[JDK-8208610](https://bugs.openjdk.java.net/browse/JDK-8208610)|Incorrect check for calling class in FXMLLoader::getDefaultClassLoader|fxml
[JDK-8129582](https://bugs.openjdk.java.net/browse/JDK-8129582)|Controls slow considerably when displaying RTL-languages text on Linux|graphics
[JDK-8195801](https://bugs.openjdk.java.net/browse/JDK-8195801)|Replace jdk.internal.misc.Unsafe with sun.misc.Unsafe in MarlinFX|graphics
[JDK-8195802](https://bugs.openjdk.java.net/browse/JDK-8195802)|Eliminate use of jdk.internal.misc security utilities in javafx.graphics|graphics
[JDK-8195806](https://bugs.openjdk.java.net/browse/JDK-8195806)|Eliminate dependency on sun.font.lookup in javafx.graphics|graphics
[JDK-8195808](https://bugs.openjdk.java.net/browse/JDK-8195808)|Eliminate dependency on sun.print in javafx.graphics|graphics
[JDK-8196617](https://bugs.openjdk.java.net/browse/JDK-8196617)|FX print tests fail with NPE in some environments|graphics
[JDK-8198354](https://bugs.openjdk.java.net/browse/JDK-8198354)|[macOS] Corrupt Thai characters displayed in word wrapped label |graphics
[JDK-8201231](https://bugs.openjdk.java.net/browse/JDK-8201231)|java.lang.NullPointerException at WindowStage.setPlatformEnabled|graphics
[JDK-8202396](https://bugs.openjdk.java.net/browse/JDK-8202396)|memory leak in ios native imageloader|graphics
[JDK-8202743](https://bugs.openjdk.java.net/browse/JDK-8202743)|Dashed Stroke randomly painted incorrectly, may freeze application|graphics
[JDK-8203378](https://bugs.openjdk.java.net/browse/JDK-8203378)|JDK build fails to compile javafx.graphics module-info.java if FX was built with OpenJDK|graphics
[JDK-8203801](https://bugs.openjdk.java.net/browse/JDK-8203801)|Missing Classpath exception in PrismLoaderGlue.stg file|graphics
[JDK-8207328](https://bugs.openjdk.java.net/browse/JDK-8207328)|API docs for javafx.css.Stylesheet are inaccurate / wrong|graphics
[JDK-8209191](https://bugs.openjdk.java.net/browse/JDK-8209191)|[macOS] Distorted complex text rendering|graphics
[JDK-8088722](https://bugs.openjdk.java.net/browse/JDK-8088722)|GSTPlatform cannot play MP4 files with multiple audio tracks|media
[JDK-8191446](https://bugs.openjdk.java.net/browse/JDK-8191446)|[Linux] Build and deliver the libav media stubs for openjfx build|media
[JDK-8193313](https://bugs.openjdk.java.net/browse/JDK-8193313)|MediaPlayer Leaking Native Memory|media
[JDK-8195803](https://bugs.openjdk.java.net/browse/JDK-8195803)|Eliminate use of sun.nio.ch.DirectBuffer in javafx.media|media
[JDK-8198316](https://bugs.openjdk.java.net/browse/JDK-8198316)|MediaPlayer crashes when playing m3u8 files on macOS High Sierra 10.13.2|media
[JDK-8199008](https://bugs.openjdk.java.net/browse/JDK-8199008)|[macOS, Linux] Instantiating MediaPlayer causes CPU usage to be over 100%|media
[JDK-8199527](https://bugs.openjdk.java.net/browse/JDK-8199527)|Upgrade GStreamer to 1.14|media
[JDK-8202393](https://bugs.openjdk.java.net/browse/JDK-8202393)|App Transport Security blocks http media on macOS with JDK build using new compilers|media
[JDK-8191661](https://bugs.openjdk.java.net/browse/JDK-8191661)|FXCanvas on Win32 HiDPI produces wrong results|other
[JDK-8193910](https://bugs.openjdk.java.net/browse/JDK-8193910)|Version number in cssref.html and introduction_to_fxml.html is wrong|other
[JDK-8195799](https://bugs.openjdk.java.net/browse/JDK-8195799)|Use System logger instead of platform logger in javafx modules|other
[JDK-8195800](https://bugs.openjdk.java.net/browse/JDK-8195800)|Eliminate dependency on sun.reflect.misc in javafx modules|other
[JDK-8195974](https://bugs.openjdk.java.net/browse/JDK-8195974)|Replace use of java.util.logging in javafx with System logger|other
[JDK-8196297](https://bugs.openjdk.java.net/browse/JDK-8196297)|Remove obsolete JFR logger code|other
[JDK-8199357](https://bugs.openjdk.java.net/browse/JDK-8199357)|Remove references to applets and Java Web Start from FX|other
[JDK-8200587](https://bugs.openjdk.java.net/browse/JDK-8200587)|Fix mistakes in FX API docs|other
[JDK-8202036](https://bugs.openjdk.java.net/browse/JDK-8202036)|Update OpenJFX license files to match OpenJDK|other
[JDK-8202357](https://bugs.openjdk.java.net/browse/JDK-8202357)|Extra chars in copyright header in ModuleHelper.java|other
[JDK-8204653](https://bugs.openjdk.java.net/browse/JDK-8204653)|Fix mistakes in FX API docs|other
[JDK-8204956](https://bugs.openjdk.java.net/browse/JDK-8204956)|Cleanup whitespace after fix for JDK-8200285|other
[JDK-8207794](https://bugs.openjdk.java.net/browse/JDK-8207794)|FXCanvas does not update x/y of EmbeddedStageInterface when FXCanvas is reparented|other
[JDK-8208294](https://bugs.openjdk.java.net/browse/JDK-8208294)|install native library fails when jrt protocol is used|other
[JDK-8180151](https://bugs.openjdk.java.net/browse/JDK-8180151)|JavaFX incorrectly renders scenegraph with two 3D boxes with certain dimensions|scenegraph
[JDK-8192056](https://bugs.openjdk.java.net/browse/JDK-8192056)|Memory leak when removing javafx.scene.shape.Sphere-objects from a group or container|scenegraph
[JDK-8205008](https://bugs.openjdk.java.net/browse/JDK-8205008)|GeneralTransform3D transform function with single Vec3d argument wrong results|scenegraph
[JDK-8207377](https://bugs.openjdk.java.net/browse/JDK-8207377)|Document the behavior of Robot::getPixelColor with HiDPI|scenegraph
[JDK-8201291](https://bugs.openjdk.java.net/browse/JDK-8201291)|Clicking a JFXPanel having setFocusable(false) causes its processMouseEvent method to loop forever|swing
[JDK-8088769](https://bugs.openjdk.java.net/browse/JDK-8088769)|Alphachannel for transparent colors is not shown in HtmlEditor|web
[JDK-8088925](https://bugs.openjdk.java.net/browse/JDK-8088925)|Non opaque background cause NumberFormatException|web
[JDK-8089375](https://bugs.openjdk.java.net/browse/JDK-8089375)|When WebWorker file is unaccessible, script should fail silently or post meaningful exception|web
[JDK-8147476](https://bugs.openjdk.java.net/browse/JDK-8147476)|Rendering  issues with MathML  token elements|web
[JDK-8193368](https://bugs.openjdk.java.net/browse/JDK-8193368)|[OS X] Remove redundant files|web
[JDK-8193590](https://bugs.openjdk.java.net/browse/JDK-8193590)|Memory leak when using WebView with Tooltip|web
[JDK-8194265](https://bugs.openjdk.java.net/browse/JDK-8194265)|Webengine (webkit) crash when reading files using FileReader|web
[JDK-8194935](https://bugs.openjdk.java.net/browse/JDK-8194935)|Cherry pick GTK WebKit 2.18.5 changes|web
[JDK-8195804](https://bugs.openjdk.java.net/browse/JDK-8195804)|Remove unused qualified export of sun.net.www from java.base to javafx.web|web
[JDK-8196011](https://bugs.openjdk.java.net/browse/JDK-8196011)|Intermittent crash when using WebView from JFXPanel application|web
[JDK-8196374](https://bugs.openjdk.java.net/browse/JDK-8196374)|windows x86 webview-icu isAlphaNumericString crash |web
[JDK-8196677](https://bugs.openjdk.java.net/browse/JDK-8196677)|Cherry pick GTK WebKit 2.18.6 changes|web
[JDK-8196968](https://bugs.openjdk.java.net/browse/JDK-8196968)|One time crash on exit in JNIEnv_::CallObjectMethod|web
[JDK-8197987](https://bugs.openjdk.java.net/browse/JDK-8197987)|Update libxslt to version 1.1.32|web
[JDK-8199474](https://bugs.openjdk.java.net/browse/JDK-8199474)|Update to 606.1 version of WebKit|web
[JDK-8200418](https://bugs.openjdk.java.net/browse/JDK-8200418)|"webPage.executeCommand(""removeFormat"", null) removes the style of the body element"|web
[JDK-8200629](https://bugs.openjdk.java.net/browse/JDK-8200629)|Update SQLite to version 3.23.0|web
[JDK-8202277](https://bugs.openjdk.java.net/browse/JDK-8202277)|WebView image capture fails with standalone FX due to dependency on javafx.swing|web
[JDK-8203698](https://bugs.openjdk.java.net/browse/JDK-8203698)|JavaFX WebView crashes when visiting certain web sites|web
[JDK-8204856](https://bugs.openjdk.java.net/browse/JDK-8204856)|WebEngine document becomes null after PAGE_REPLACED event|web
[JDK-8206899](https://bugs.openjdk.java.net/browse/JDK-8206899)|DRT crashes randomly when running 'dom/html/level2/html/AppletsCollection.html'|web
[JDK-8206995](https://bugs.openjdk.java.net/browse/JDK-8206995)|Remove unused WebKit files|web
[JDK-8208114](https://bugs.openjdk.java.net/browse/JDK-8208114)|Drag and drop of text contents and URL links functionalities are broken in Webview|web
[JDK-8208622](https://bugs.openjdk.java.net/browse/JDK-8208622)|[WebView] IllegalStateException when invoking print API with html form controls|web
[JDK-8209049](https://bugs.openjdk.java.net/browse/JDK-8209049)|Cherry pick GTK WebKit 2.20.4 changes|web
[JDK-8163795](https://bugs.openjdk.java.net/browse/JDK-8163795)|[Windows] Remove call to StretchBlt in native GetScreenCapture method|window-toolkit
[JDK-8191885](https://bugs.openjdk.java.net/browse/JDK-8191885)|[MacOS] JavaFX main window not resizable coming back from full screen mode in MacOS|window-toolkit
[JDK-8196031](https://bugs.openjdk.java.net/browse/JDK-8196031)|FX Robot mouseMove fails on Windows 10 1709 with HiDPI|window-toolkit
[JDK-8199614](https://bugs.openjdk.java.net/browse/JDK-8199614)|[macos] ImageCursor.getBestSize() throws NullPointerException|window-toolkit
[JDK-8204635](https://bugs.openjdk.java.net/browse/JDK-8204635)|[Linux] getMouseX, getMouseY in gtk GlassRobot.cpp ignore the HiDPI scale|window-toolkit
[JDK-8207372](https://bugs.openjdk.java.net/browse/JDK-8207372)|Robot.mouseWheel not implemented correctly on Linux, Mac|window-toolkit


## List of Enhancements

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8205919](https://bugs.openjdk.java.net/browse/JDK-8205919)|Create artifacts and functionality to upload them to Maven Central|build
[JDK-8167096](https://bugs.openjdk.java.net/browse/JDK-8167096)|Add APIs to customize step repeat timing for Spinner control|controls
[JDK-8177380](https://bugs.openjdk.java.net/browse/JDK-8177380)|Add standard colors in ColorPicker color palette|controls
[JDK-8186187](https://bugs.openjdk.java.net/browse/JDK-8186187)|Modify return type of public API StyleConverter.getEnumConverter()|controls
[JDK-8204621](https://bugs.openjdk.java.net/browse/JDK-8204621)|Upgrade MarlinFX to 0.9.2|graphics
[JDK-8090763](https://bugs.openjdk.java.net/browse/JDK-8090763)|FX Robot API|scenegraph
[JDK-8130379](https://bugs.openjdk.java.net/browse/JDK-8130379)|Enhance the Bounds class with getCenter method|scenegraph
[JDK-8195811](https://bugs.openjdk.java.net/browse/JDK-8195811)|Support FX Swing interop using public API|swing
[JDK-8198654](https://bugs.openjdk.java.net/browse/JDK-8198654)|Switch FX's default GTK version to 3|window-toolkit
