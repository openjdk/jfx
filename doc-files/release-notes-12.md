# Release Notes for JavaFX 12

## Introduction

The following notes describe important changes and information about this release.

As of JDK 11 the JavaFX modules are delivered separately from the JDK. These release notes cover the standalone JavaFX 12 release. JavaFX 12 requires JDK 11.

## List of Fixed Bugs

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8152395](https://bugs.openjdk.java.net/browse/JDK-8152395)|[ToolBar] Overflow button of ToolBar doesn't appear when the size of the items increases|controls
[JDK-8212102](https://bugs.openjdk.java.net/browse/JDK-8212102)|[TextField] IOOBE on paste/replace text with control characters|controls
[JDK-8217270](https://bugs.openjdk.java.net/browse/JDK-8217270)|Broken link to cssref.html in javafx.controls package docs|controls
[JDK-8167068](https://bugs.openjdk.java.net/browse/JDK-8167068)|GLS language errors|graphics
[JDK-8188810](https://bugs.openjdk.java.net/browse/JDK-8188810)|Fonts are blurry on Ubuntu 16.04 and Debian 9|graphics
[JDK-8203884](https://bugs.openjdk.java.net/browse/JDK-8203884)|Update libjpeg to version 9c|graphics
[JDK-8207839](https://bugs.openjdk.java.net/browse/JDK-8207839)|[win] Negative glyph_id causes ArrayIndexOutOfBoundsException|graphics
[JDK-8209764](https://bugs.openjdk.java.net/browse/JDK-8209764)|JavaFX/Monocle - Partial Screen Capture Broken|graphics
[JDK-8209791](https://bugs.openjdk.java.net/browse/JDK-8209791)|OpenJFX build fails in PrismPrint.c due to missing JNICALL|graphics
[JDK-8209968](https://bugs.openjdk.java.net/browse/JDK-8209968)|Image size sometimes off by 1 when scaling down images with preserveRatio true|graphics
[JDK-8209969](https://bugs.openjdk.java.net/browse/JDK-8209969)|Monocle setBounds issue (width/height mixed)|graphics
[JDK-8210219](https://bugs.openjdk.java.net/browse/JDK-8210219)|GlassClipboard.cpp fails to compile with newer versions of VS2017|graphics
[JDK-8210386](https://bugs.openjdk.java.net/browse/JDK-8210386)|Clipping problems with complex affine transforms: negative scaling factors or small scaling factors|graphics
[JDK-8212115](https://bugs.openjdk.java.net/browse/JDK-8212115)|Typo in javadoc for javafx.stage.Window|graphics
[JDK-8214035](https://bugs.openjdk.java.net/browse/JDK-8214035)|Unable to render cmyk jpeg image|graphics
[JDK-8214397](https://bugs.openjdk.java.net/browse/JDK-8214397)|Provide fallback to tmpdir if user home is not writable for native libs|graphics
[JDK-8214185](https://bugs.openjdk.java.net/browse/JDK-8214185)|Upgrade GStreamer to the latest (1.14.4) version|media
[JDK-8183399](https://bugs.openjdk.java.net/browse/JDK-8183399)|[macOSX] Scroll events finish with invalid delta values|other
[JDK-8189926](https://bugs.openjdk.java.net/browse/JDK-8189926)|[Mac] Pulse timer should pause when idle|other
[JDK-8211014](https://bugs.openjdk.java.net/browse/JDK-8211014)|Fix mistakes in FX API docs|other
[JDK-8205092](https://bugs.openjdk.java.net/browse/JDK-8205092)|NullPointerException in PickResultChooser.processOffer when using viewOrder|scenegraph
[JDK-8207837](https://bugs.openjdk.java.net/browse/JDK-8207837)|Indeterminate ProgressBar does not animate if content is added after scene is set on window|scenegraph
[JDK-8216377](https://bugs.openjdk.java.net/browse/JDK-8216377)|JavaFX: memoryleak for initial nodes of Window|scenegraph
[JDK-8210092](https://bugs.openjdk.java.net/browse/JDK-8210092)|Remove old javafx.swing implementation|swing
[JDK-8207159](https://bugs.openjdk.java.net/browse/JDK-8207159)|Update ICU to version 62.1|web
[JDK-8209457](https://bugs.openjdk.java.net/browse/JDK-8209457)|[WebView] Canvas.toDataURL with image/jpeg MIME type fails|web
[JDK-8210218](https://bugs.openjdk.java.net/browse/JDK-8210218)|WebKit build fails with newer versions of VS 2017|web
[JDK-8211399](https://bugs.openjdk.java.net/browse/JDK-8211399)|libxslt fails to build with glibc 2.26|web
[JDK-8211454](https://bugs.openjdk.java.net/browse/JDK-8211454)|Update SQLite to version 3.26.0|web
[JDK-8213541](https://bugs.openjdk.java.net/browse/JDK-8213541)|WebView does not handle HTTP response without ContentType|web
[JDK-8213806](https://bugs.openjdk.java.net/browse/JDK-8213806)|WebView - JVM crashes for given HTML|web
[JDK-8214119](https://bugs.openjdk.java.net/browse/JDK-8214119)|Update to 607.1 version of WebKit|web
[JDK-8214452](https://bugs.openjdk.java.net/browse/JDK-8214452)|Update libxml2 to version 2.9.9|web
[JDK-8215702](https://bugs.openjdk.java.net/browse/JDK-8215702)|SVG gradients are not rendered|web
[JDK-8215799](https://bugs.openjdk.java.net/browse/JDK-8215799)|Complex text is not rendered by webkit on Windows|web
[JDK-8216470](https://bugs.openjdk.java.net/browse/JDK-8216470)|Some methods of System.Logger are unimplemented in PlatformLogger|web
[JDK-8218611](https://bugs.openjdk.java.net/browse/JDK-8218611)|[DRT] fast/xslt tests fails with Unsupported encoding windows-1251|web
[JDK-8210411](https://bugs.openjdk.java.net/browse/JDK-8210411)|JavaFX crashes on Ubuntu 18.04 with Wayland|window-toolkit
[JDK-8211280](https://bugs.openjdk.java.net/browse/JDK-8211280)|JavaFX build fails on Linux with gcc8|window-toolkit
[JDK-8211304](https://bugs.openjdk.java.net/browse/JDK-8211304)|[macOS] Crash on focus loss from dialog on macOS 10.14 Mojave|window-toolkit
[JDK-8218424](https://bugs.openjdk.java.net/browse/JDK-8218424)|[macOSX] mousewheel scrolling slow|window-toolkit


## List of Enhancements

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8207942](https://bugs.openjdk.java.net/browse/JDK-8207942)|Add new protected VirtualFlow methods for subclassing|controls
[JDK-8210361](https://bugs.openjdk.java.net/browse/JDK-8210361)|Add images to docs for public API classes of controls and missing examples|controls
[JDK-8204060](https://bugs.openjdk.java.net/browse/JDK-8204060)|[Canvas] Add API in GraphicsContext to control image smoothing|graphics
[JDK-8214069](https://bugs.openjdk.java.net/browse/JDK-8214069)|Use xdg-open to get default web browser on Linux systems|graphics
[JDK-8088418](https://bugs.openjdk.java.net/browse/JDK-8088418)|Reintroduce JFR Pulse Logger|other
[JDK-8090930](https://bugs.openjdk.java.net/browse/JDK-8090930)|Support mouse forward/back buttons|scenegraph
[JDK-8211249](https://bugs.openjdk.java.net/browse/JDK-8211249)|Refactor javafx.swing implementation to get rid of unneeded abstraction layer|swing
[JDK-8148129](https://bugs.openjdk.java.net/browse/JDK-8148129)|Implement Accelerated composition for WebView|web
[JDK-8207772](https://bugs.openjdk.java.net/browse/JDK-8207772)|File API and FileReader should be supported in WebView|web
