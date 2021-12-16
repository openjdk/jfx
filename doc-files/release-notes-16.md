# Release Notes for JavaFX 16

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

As of JDK 11 the JavaFX modules are delivered separately from the JDK. These release notes cover the standalone JavaFX 16 release. JavaFX 16 requires JDK 11 or later.

## Important Changes

### JavaFX runtime logs a warning if javafx.* modules are loaded from the classpath

The JavaFX classes must be loaded from a set of named `javafx.*` modules on the _module path_. Loading the JavaFX classes from the classpath is not supported.
The JavaFX runtime logs a warning at startup if the JavaFX classes are not loaded from the expected named module.
See [JDK-8256362](https://bugs.openjdk.java.net/browse/JDK-8256362) for more information.

## Removed Features and Options

### The obsolete Pisces rasterizer has been removed from JavaFX

The obsolete Pisces rasterizer has been removed from JavaFX.
The Marlin rasterizer has been the default since JDK 10, but it was possible to select either the native Pisces rasterizer or the Java-based Pisces rasterizer by setting the `prism.rasterizerorder` system property to `nativepisces` or `javapisces`, respectively.
Those options will now be silently ignored, and the default Marlin rasterizer will always be used.
See [JDK-8196079](https://bugs.openjdk.java.net/browse/JDK-8196079) for more information.

## List of Fixed Bugs

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8256362](https://bugs.openjdk.java.net/browse/JDK-8256362)|JavaFX must warn when the javafx.* modules are loaded from the classpath|application-lifecycle
[JDK-8251352](https://bugs.openjdk.java.net/browse/JDK-8251352)|Many javafx.base classes have implicit no-arg constructors|base
[JDK-8251946](https://bugs.openjdk.java.net/browse/JDK-8251946)|ObservableList.setAll does not conform to specification|base
[JDK-8177945](https://bugs.openjdk.java.net/browse/JDK-8177945)|Single cell selection flickers when adding data to TableView|controls
[JDK-8178297](https://bugs.openjdk.java.net/browse/JDK-8178297)|TableView scrolls slightly when adding new elements|controls
[JDK-8209788](https://bugs.openjdk.java.net/browse/JDK-8209788)|Left/Right/Ctrl+A keys not working in editor of ComboBox if popup showing|controls
[JDK-8242621](https://bugs.openjdk.java.net/browse/JDK-8242621)|TabPane: Memory leak when switching skin|controls
[JDK-8245053](https://bugs.openjdk.java.net/browse/JDK-8245053)|Keyboard doesn't show when TextInputControl has focus|controls
[JDK-8246202](https://bugs.openjdk.java.net/browse/JDK-8246202)|ChoiceBoxSkin: misbehavior on switching skin, part 2|controls
[JDK-8246745](https://bugs.openjdk.java.net/browse/JDK-8246745)|ListCell/Skin: misbehavior on switching skin|controls
[JDK-8247576](https://bugs.openjdk.java.net/browse/JDK-8247576)|Labeled/SkinBase: misbehavior on switching skin|controls
[JDK-8251941](https://bugs.openjdk.java.net/browse/JDK-8251941)|ListCell: visual artifact when items contain null values|controls
[JDK-8252236](https://bugs.openjdk.java.net/browse/JDK-8252236)|TabPane: must keep header of selected tab visible|controls
[JDK-8252811](https://bugs.openjdk.java.net/browse/JDK-8252811)|The list of cells in a VirtualFlow is cleared every time the number of items changes|controls
[JDK-8253597](https://bugs.openjdk.java.net/browse/JDK-8253597)|TreeTableView: must select leaf row on click into indentation region|controls
[JDK-8253634](https://bugs.openjdk.java.net/browse/JDK-8253634)|TreeCell/Skin: misbehavior on switching skin|controls
[JDK-8254964](https://bugs.openjdk.java.net/browse/JDK-8254964)|Fix default values in Spinner class|controls
[JDK-8256821](https://bugs.openjdk.java.net/browse/JDK-8256821)|TreeViewSkin/Behavior: misbehavior on switching skin|controls
[JDK-8199592](https://bugs.openjdk.java.net/browse/JDK-8199592)|Control labels truncated at certain DPI scaling levels|graphics
[JDK-8211294](https://bugs.openjdk.java.net/browse/JDK-8211294)|ScrollPane content is blurry with 125% scaling|graphics
[JDK-8248908](https://bugs.openjdk.java.net/browse/JDK-8248908)|Printer.createPageLayout() returns 0.75" margins instead of hardware margins|graphics
[JDK-8252446](https://bugs.openjdk.java.net/browse/JDK-8252446)|Screen.getScreens() is empty sometimes|graphics
[JDK-8254605](https://bugs.openjdk.java.net/browse/JDK-8254605)|repaint on Android broken|graphics
[JDK-8255415](https://bugs.openjdk.java.net/browse/JDK-8255415)|Nested calls to snap methods in Region give different results|graphics
[JDK-8256012](https://bugs.openjdk.java.net/browse/JDK-8256012)|Fix build of Monocle for Linux|graphics
[JDK-8257719](https://bugs.openjdk.java.net/browse/JDK-8257719)|JFXPanel scene fails to render correctly on HiDPI after fix for JDK-8199592|graphics
[JDK-8258592](https://bugs.openjdk.java.net/browse/JDK-8258592)|Control labels in Dialogs are truncated at certain DPI scaling levels|graphics
[JDK-8248365](https://bugs.openjdk.java.net/browse/JDK-8248365)|Debug build crashes on Windows when playing media file|media
[JDK-8252060](https://bugs.openjdk.java.net/browse/JDK-8252060)|gstreamer fails to build with gcc 10|media
[JDK-8252107](https://bugs.openjdk.java.net/browse/JDK-8252107)|Media pipeline initialization can crash if audio or video bin state change fails|media
[JDK-8252389](https://bugs.openjdk.java.net/browse/JDK-8252389)|Fix mistakes in FX API docs|other
[JDK-8251353](https://bugs.openjdk.java.net/browse/JDK-8251353)|Many javafx scenegraph classes have implicit no-arg constructors|scenegraph
[JDK-8252387](https://bugs.openjdk.java.net/browse/JDK-8252387)|Deprecate for removal css Selector and ShapeConverter constructors|scenegraph
[JDK-8252547](https://bugs.openjdk.java.net/browse/JDK-8252547)|Correct transformations docs in Node|scenegraph
[JDK-8231372](https://bugs.openjdk.java.net/browse/JDK-8231372)|JFXPanel fails to render if setScene called on Swing thread|swing
[JDK-8181775](https://bugs.openjdk.java.net/browse/JDK-8181775)|JavaFX WebView does not calculate border-radius properly|web
[JDK-8202990](https://bugs.openjdk.java.net/browse/JDK-8202990)|javafx webview css filter property with display scaling|web
[JDK-8240969](https://bugs.openjdk.java.net/browse/JDK-8240969)|WebView does not allow to load style sheet in modularized applications|web
[JDK-8242361](https://bugs.openjdk.java.net/browse/JDK-8242361)|JavaFX Web View crashes with Segmentation Fault, when HTML contains Data-URIs|web
[JDK-8245284](https://bugs.openjdk.java.net/browse/JDK-8245284)|Update to 610.1 version of WebKit|web
[JDK-8249839](https://bugs.openjdk.java.net/browse/JDK-8249839)|Cherry pick GTK WebKit 2.28.3 changes|web
[JDK-8252062](https://bugs.openjdk.java.net/browse/JDK-8252062)|WebKit build fails with recent VS 2019 compiler|web
[JDK-8252381](https://bugs.openjdk.java.net/browse/JDK-8252381)|Cherry pick GTK WebKit 2.28.4 changes|web
[JDK-8253696](https://bugs.openjdk.java.net/browse/JDK-8253696)|WebEngine refuses to load local "file:///" CSS stylesheets when using JDK 15|web
[JDK-8254049](https://bugs.openjdk.java.net/browse/JDK-8254049)|Update WebView to public suffix list 2020-04-24|web
[JDK-8257897](https://bugs.openjdk.java.net/browse/JDK-8257897)|Fix webkit build for XCode 12|web
[JDK-8201568](https://bugs.openjdk.java.net/browse/JDK-8201568)|zForce touchscreen input device fails when closed and immediately reopened|window-toolkit
[JDK-8233678](https://bugs.openjdk.java.net/browse/JDK-8233678)|[macos 10.15] System menu bar does not work initially on macOS Catalina|window-toolkit
[JDK-8237491](https://bugs.openjdk.java.net/browse/JDK-8237491)|[Linux] Undecorated stage cannot be maximized|window-toolkit
[JDK-8241840](https://bugs.openjdk.java.net/browse/JDK-8241840)|Memoryleak: Closed focused Stages are not collected with Monocle.|window-toolkit
[JDK-8251241](https://bugs.openjdk.java.net/browse/JDK-8251241)|macOS: iconify property doesn't change after minimize when resizable is false|window-toolkit
[JDK-8251555](https://bugs.openjdk.java.net/browse/JDK-8251555)|Remove unused focusedWindow field in glass Window to avoid leak|window-toolkit
[JDK-8255723](https://bugs.openjdk.java.net/browse/JDK-8255723)|Gtk glass backend should run with Gtk+ 3.8 (minimum)|window-toolkit

## List of Enhancement

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8252546](https://bugs.openjdk.java.net/browse/JDK-8252546)|Move ObservableValue's equality check and lazy evaluation descriptions to @implSpec|base
[JDK-8196079](https://bugs.openjdk.java.net/browse/JDK-8196079)|Remove obsolete Pisces rasterizer|graphics
[JDK-8217472](https://bugs.openjdk.java.net/browse/JDK-8217472)|Add attenuation for PointLight|graphics
[JDK-8254569](https://bugs.openjdk.java.net/browse/JDK-8254569)|Remove hard dependency on Dispman in Monocle fb rendering|graphics
[JDK-8242861](https://bugs.openjdk.java.net/browse/JDK-8242861)|Update ImagePattern to apply SVG pattern transforms|web
