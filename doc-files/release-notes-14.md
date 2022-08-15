# Release Notes for JavaFX 14

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

As of JDK 11 the JavaFX modules are delivered separately from the JDK. These release notes cover the standalone JavaFX 14 release. JavaFX 14 requires JDK 11 or later.

## List of Fixed Bugs

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8237975](https://bugs.openjdk.org/browse/JDK-8237975) | Non-embedded Animations do not play backwards after being paused | animation
[JDK-8236753](https://bugs.openjdk.org/browse/JDK-8236753) | Animations do not play backwards after being stopped | animation
[JDK-8232524](https://bugs.openjdk.org/browse/JDK-8232524) | SynchronizedObservableMap cannot be be protected for copying/iterating | base
[JDK-8220396](https://bugs.openjdk.org/browse/JDK-8220396) | Bindings class gives a lot of unneeded 'select-binding' log messages | base
[JDK-8229472](https://bugs.openjdk.org/browse/JDK-8229472) | Deprecate for removal JavaBeanXxxPropertyBuilders constructors | base
[JDK-8207774](https://bugs.openjdk.org/browse/JDK-8207774) | TextField: must not forward ENTER if actionHandler consumed the actionEvent | controls
[JDK-8207759](https://bugs.openjdk.org/browse/JDK-8207759) | VK_ENTER not consumed by TextField when default Button exists | controls
[JDK-8179097](https://bugs.openjdk.org/browse/JDK-8179097) | NPE in MenuButtonSkinBase class | controls
[JDK-8185937](https://bugs.openjdk.org/browse/JDK-8185937) | Spinner with Double/Integer value factory ignores up/down arrow keys | controls
[JDK-8233040](https://bugs.openjdk.org/browse/JDK-8233040) | ComboBoxPopupControl: remove eventFilter for F4 | controls
[JDK-8232811](https://bugs.openjdk.org/browse/JDK-8232811) | Dialog's preferred size no longer accommodates multi-line strings | controls
[JDK-8221334](https://bugs.openjdk.org/browse/JDK-8221334) | TableViewSkin: must initialize flow's cellCount in constructor | controls
[JDK-8220722](https://bugs.openjdk.org/browse/JDK-8220722) | ProgressBarSkin: adds strong listener to control's width property | controls
[JDK-8237372](https://bugs.openjdk.org/browse/JDK-8237372) | NullPointerException in TabPaneSkin.stopDrag | controls
[JDK-8193445](https://bugs.openjdk.org/browse/JDK-8193445) | JavaFX CSS is applied redundantly leading to significant performance degradation | controls
[JDK-8196587](https://bugs.openjdk.org/browse/JDK-8196587) | Remove use of deprecated finalize method from JPEGImageLoader | graphics
[JDK-8166194](https://bugs.openjdk.org/browse/JDK-8166194) | JavaFX: poor printing quality for Region nodes | graphics
[JDK-8189092](https://bugs.openjdk.org/browse/JDK-8189092) | ArrayIndexOutOfBoundsException on Linux in getCachedGlyph | graphics
[JDK-8236448](https://bugs.openjdk.org/browse/JDK-8236448) | Remove unused and repair broken Android/Dalvik code | graphics
[JDK-8236484](https://bugs.openjdk.org/browse/JDK-8236484) | Compile error in monocle dispman | graphics
[JDK-8232687](https://bugs.openjdk.org/browse/JDK-8232687) | No static JNI loader for libprism-sw | graphics
[JDK-8232943](https://bugs.openjdk.org/browse/JDK-8232943) | Gesture support is not initialized on iOS | graphics
[JDK-8232929](https://bugs.openjdk.org/browse/JDK-8232929) | Duplicate symbols when building static libraries | graphics
[JDK-8232210](https://bugs.openjdk.org/browse/JDK-8232210) | Update Mesa 3-D Headers to version 19.2.1 | graphics
[JDK-8235151](https://bugs.openjdk.org/browse/JDK-8235151) | Nonexistent notifyQuit method referred from iOS GlassHelper.m | graphics
[JDK-8235150](https://bugs.openjdk.org/browse/JDK-8235150) | IosApplication does not pass the required object in _leaveNestedEventLoopImpl | graphics
[JDK-8235627](https://bugs.openjdk.org/browse/JDK-8235627) | Blank stages when running JavaFX app in a macOS virtual machine | graphics
[JDK-8234916](https://bugs.openjdk.org/browse/JDK-8234916) | [macos 10.15] Garbled text running with native-image | graphics
[JDK-8223296](https://bugs.openjdk.org/browse/JDK-8223296) | NullPointerException in GlassScene.java at line 325 | graphics
[JDK-8236808](https://bugs.openjdk.org/browse/JDK-8236808) | javafx_iio can not be used in static environment | graphics
[JDK-8088198](https://bugs.openjdk.org/browse/JDK-8088198) | Exception thrown from snapshot if dimensions are larger than max texture size | graphics
[JDK-8232589](https://bugs.openjdk.org/browse/JDK-8232589) | Remove CoreAudio Utility Classes | media
[JDK-8230610](https://bugs.openjdk.org/browse/JDK-8230610) | Upgrade GStreamer to version 1.16.1 | media
[JDK-8230609](https://bugs.openjdk.org/browse/JDK-8230609) | Upgrade glib to version 2.62.2 | media
[JDK-8233338](https://bugs.openjdk.org/browse/JDK-8233338) | FX javadoc headings are out of sequence | other
[JDK-8232824](https://bugs.openjdk.org/browse/JDK-8232824) | Removing TabPane with strong referenced content causes memory leak from weak one | scenegraph
[JDK-8200224](https://bugs.openjdk.org/browse/JDK-8200224) | First mouse press each time JFXPanel gains focus is triggered twice | swing
[JDK-8218640](https://bugs.openjdk.org/browse/JDK-8218640) | Update ICU4C to version 64.2 | web
[JDK-8233747](https://bugs.openjdk.org/browse/JDK-8233747) | JVM crash in com.sun.webkit.dom.DocumentImpl.createAttribute | web
[JDK-8230492](https://bugs.openjdk.org/browse/JDK-8230492) | font-family not set in HTMLEditor if font name has a number in it | web
[JDK-8236912](https://bugs.openjdk.org/browse/JDK-8236912) | NullPointerException when clicking in WebView with Button 4 or Button 5 | web
[JDK-8231188](https://bugs.openjdk.org/browse/JDK-8231188) | Update SQLite to version 3.30.1 | web
[JDK-8234056](https://bugs.openjdk.org/browse/JDK-8234056) | Upgrade to libxslt 1.1.34 | web
[JDK-8231513](https://bugs.openjdk.org/browse/JDK-8231513) | JavaFX cause Keystroke Receiving prompt on MacOS 10.15 (Catalina) | window-toolkit
[JDK-8234474](https://bugs.openjdk.org/browse/JDK-8234474) | [macos 10.15] Crash in file dialog in sandbox mode | window-toolkit
[JDK-8228766](https://bugs.openjdk.org/browse/JDK-8228766) | Platform.startup() deadlock on mac when called from class initializer | window-toolkit
[JDK-8227366](https://bugs.openjdk.org/browse/JDK-8227366) | Wrong stage gets focused after modal stage creation | window-toolkit

## List of Enhancements

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8207957](https://bugs.openjdk.org/browse/JDK-8207957) | TableSkinUtils should not contain actual code implementation | controls
[JDK-8130738](https://bugs.openjdk.org/browse/JDK-8130738) | Add tabSize property to Text and TextFlow | graphics
[JDK-8226850](https://bugs.openjdk.org/browse/JDK-8226850) | Use an EnumSet for DirtyBits instead of an ordinal-based mask | graphics
[JDK-8092352](https://bugs.openjdk.org/browse/JDK-8092352) | Skip event dispatch if there are no handlers/filters | scenegraph
[JDK-8211308](https://bugs.openjdk.org/browse/JDK-8211308) | Support HTTP/2 in WebView | web
[JDK-8087980](https://bugs.openjdk.org/browse/JDK-8087980) | Add property to disable Monocle cursor | window-toolkit
[JDK-8225571](https://bugs.openjdk.org/browse/JDK-8225571) | Port Linux glass drag source (DND) to use gtk instead of gdk | window-toolkit

## List of Security fixes

Issue key|Summary|Subcomponent
---------|-------|------------
JDK-8227473 (not public) | Improve gstreamer media support | media
JDK-8227402 (not public) | Improve XSLT processing | web
JDK-8232121 (not public) | Better numbering system | web
JDK-8232128 (not public) | Better formatting for numbers | web
JDK-8232214 (not public) | Improved internal validations | web
