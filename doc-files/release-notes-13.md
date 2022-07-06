# Release Notes for JavaFX 13

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

As of JDK 11 the JavaFX modules are delivered separately from the JDK. These release notes cover the standalone JavaFX 13 release. JavaFX 13 requires JDK 11 or later.

## List of Fixed Bugs

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8221702](https://bugs.openjdk.org/browse/JDK-8221702)|Use HTTPS to download all build dependencies|build
[JDK-8220012](https://bugs.openjdk.org/browse/JDK-8220012)|Accordion control holds reference to child pane after it is removed|controls
[JDK-8209938](https://bugs.openjdk.org/browse/JDK-8209938)|Default and Cancel button cause memory leak|controls
[JDK-8222222](https://bugs.openjdk.org/browse/JDK-8222222)|Gradients defined in CSS always use "reflect" even when "repeat" is specified|controls
[JDK-8089986](https://bugs.openjdk.org/browse/JDK-8089986)|Menu beeps when mnemonics is used|controls
[JDK-8222073](https://bugs.openjdk.org/browse/JDK-8222073)|Revert unintentional change to Dialog.java|controls
[JDK-8222457](https://bugs.openjdk.org/browse/JDK-8222457)|TabPane doesn't respect order of TabPane.getTabs() list|controls
[JDK-8222214](https://bugs.openjdk.org/browse/JDK-8222214)|TableView rows disappears when inside a pane and KEY_UP is pressed|controls
[JDK-8197536](https://bugs.openjdk.org/browse/JDK-8197536)|TableView, ListView: unexpected scrolling behaviour on up/down keys|controls
[JDK-8201539](https://bugs.openjdk.org/browse/JDK-8201539)|Crash in DirectWrite CreateBitmap code when running TestFX test suite|graphics
[JDK-8222211](https://bugs.openjdk.org/browse/JDK-8222211)|Creating animated gif image from non FX App thread causes exception|graphics
[JDK-8210973](https://bugs.openjdk.org/browse/JDK-8210973)|Focus goes to wrong Window when dismissing an Alert|graphics
[JDK-8217492](https://bugs.openjdk.org/browse/JDK-8217492)|JavaFX - memory leak after the event WindowEvent.DESTROY|graphics
[JDK-8221987](https://bugs.openjdk.org/browse/JDK-8221987)|NPE in javafx.graphics/javafx.stage.Window$TKBoundsConfigurator.apply|graphics
[JDK-8226789](https://bugs.openjdk.org/browse/JDK-8226789)|Path rendered incorrectly when it goes outside the clipping region|graphics
[JDK-8219008](https://bugs.openjdk.org/browse/JDK-8219008)|Update OpenGL Headers to version 4.6|graphics
[JDK-8229890](https://bugs.openjdk.org/browse/JDK-8229890)|WritableImage update fails for empty region|graphics
[JDK-8208076](https://bugs.openjdk.org/browse/JDK-8208076)|display INVISIBLE_GLYPH_ID as square box on Windows|graphics
[JDK-8208173](https://bugs.openjdk.org/browse/JDK-8208173)|isComplexCharCode() returns false for U+11FF|graphics
[JDK-8222217](https://bugs.openjdk.org/browse/JDK-8222217)|FX build fails on 32-bit Windows after fix for JDK-8133841|media
[JDK-8133841](https://bugs.openjdk.org/browse/JDK-8133841)|Full HD video can not be played on standard 1080p screen in portrait mode|media
[JDK-8209180](https://bugs.openjdk.org/browse/JDK-8209180)|Media fails to load source from custom image, with jrt: URL|media
[JDK-8215894](https://bugs.openjdk.org/browse/JDK-8215894)|Provide media support for libav version 58|media
[JDK-8222780](https://bugs.openjdk.org/browse/JDK-8222780)|Visual Studio does not open media vs_projects files|media
[JDK-8213510](https://bugs.openjdk.org/browse/JDK-8213510)|[Windows] MediaPlayer does not play some mp3 with artwork stream in mjpeg|media
[JDK-8211900](https://bugs.openjdk.org/browse/JDK-8211900)|javafx.media classes directly reference platform classes that are excluded|media
[JDK-8218174](https://bugs.openjdk.org/browse/JDK-8218174)|Add missing license file for Mesa header files|other
[JDK-8222746](https://bugs.openjdk.org/browse/JDK-8222746)|Cleanup third-party legal files|other
[JDK-8221377](https://bugs.openjdk.org/browse/JDK-8221377)|Fix mistakes in FX API docs|other
[JDK-8223377](https://bugs.openjdk.org/browse/JDK-8223377)|JavaFX can crash due to loading the wrong native libraries if system libraries are installed|other
[JDK-8222212](https://bugs.openjdk.org/browse/JDK-8222212)|Memory Leak with SwingNode using Drag and Drop function|swing
[JDK-8224636](https://bugs.openjdk.org/browse/JDK-8224636)|"CSS ""pointer-events"" property ""stroke"" is not respected for SVG renderings"|web
[JDK-8219539](https://bugs.openjdk.org/browse/JDK-8219539)|Cherry pick GTK WebKit 2.22.6 changes|web
[JDK-8220147](https://bugs.openjdk.org/browse/JDK-8220147)|Cherry pick GTK WebKit 2.22.7 changes|web
[JDK-8227079](https://bugs.openjdk.org/browse/JDK-8227079)|Cherry pick GTK WebKit 2.24.3 changes|web
[JDK-8215775](https://bugs.openjdk.org/browse/JDK-8215775)|Scrollbars from web pages appear to be absolute, overlapping everything|web
[JDK-8225203](https://bugs.openjdk.org/browse/JDK-8225203)|Update SQLite to version 3.28.0|web
[JDK-8219362](https://bugs.openjdk.org/browse/JDK-8219362)|Update to 608.1 version of WebKit|web
[JDK-8217942](https://bugs.openjdk.org/browse/JDK-8217942)|Upgrade to libxslt 1.1.33|web
[JDK-8222912](https://bugs.openjdk.org/browse/JDK-8222912)|Websocket client doesn't work in WebView|web
[JDK-8221941](https://bugs.openjdk.org/browse/JDK-8221941)|Wrong package declaration for WCTextRunImpl.java in web module|web
[JDK-8219734](https://bugs.openjdk.org/browse/JDK-8219734)|[WebView] Get rid of macOS SDK private API usage|web
[JDK-8219917](https://bugs.openjdk.org/browse/JDK-8219917)|[WebView] Sub-resource integrity check fails on Windows and Linux|web
[JDK-8227431](https://bugs.openjdk.org/browse/JDK-8227431)|[Windows] Fix assertion failure on X86 32-bit when enabling CLOOP based JavaScript interpreter|web
[JDK-8230361](https://bugs.openjdk.org/browse/JDK-8230361)|[web] Cookies are not enabled in WebKit v608.1 |web
[JDK-8229328](https://bugs.openjdk.org/browse/JDK-8229328)|[windows] PlatformFileHandle type should be JGObject rather than void 
[JDK-8222788](https://bugs.openjdk.org/browse/JDK-8222788)|javafx.web build fails on XCode 10.2|web
[JDK-8226951](https://bugs.openjdk.org/browse/JDK-8226951)|Backout commit for JDK-8226537 to fix the attribution|window-toolkit
[JDK-8211302](https://bugs.openjdk.org/browse/JDK-8211302)|DragAndDrop no longer works with GTK3|window-toolkit
[JDK-8226537](https://bugs.openjdk.org/browse/JDK-8226537)|Multi-level Stage::initOwner can crash gnome-shell or X.org server|window-toolkit
[JDK-8226274](https://bugs.openjdk.org/browse/JDK-8226274)|NPE in WinWindow.notifyMoving when Stage with no Scene is shown on 2nd monitor|window-toolkit
[JDK-8088717](https://bugs.openjdk.org/browse/JDK-8088717)|Win: UNDECORATED windows are not minimized with the taskbar button|window-toolkit
[JDK-8220272](https://bugs.openjdk.org/browse/JDK-8220272)|Window order is not correct when Modality.WINDOW_MODAL|window-toolkit
[JDK-8212060](https://bugs.openjdk.org/browse/JDK-8212060)|[GTK3] Stage sometimes shown at top-left before moving to correct position|window-toolkit


## List of Enhancements

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8226454](https://bugs.openjdk.org/browse/JDK-8226454)|Point2D and Point3D should implement Interpolatable|animation
[JDK-8221269](https://bugs.openjdk.org/browse/JDK-8221269)|Extract embedded actions from JSL grammar file to Visitor class|build
[JDK-8223760](https://bugs.openjdk.org/browse/JDK-8223760)|support static build for macosx|build
[JDK-8222258](https://bugs.openjdk.org/browse/JDK-8222258)|Add exclusion scope for LightBase|graphics
[JDK-8167148](https://bugs.openjdk.org/browse/JDK-8167148)|Add native rendering support  by supporting WritableImages backed by NIO ByteBuffers|graphics
[JDK-8217605](https://bugs.openjdk.org/browse/JDK-8217605)|Add support for e-paper displays|graphics
[JDK-8226912](https://bugs.openjdk.org/browse/JDK-8226912)|Color, Point2D and Point3D's fields should be made final|graphics
[JDK-8217470](https://bugs.openjdk.org/browse/JDK-8217470)|Upgrade Direct3D9 shader model from 2.0 to 3.0 for 3D operations|graphics
