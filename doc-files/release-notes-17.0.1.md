# Release Notes for JavaFX 17.0.1

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

As of JDK 11 the JavaFX modules are delivered separately from the JDK. These release notes cover the standalone JavaFX 17.0.1 release. As such, they complement the [JavaFX 17 Release Notes](https://github.com/openjdk/jfx/blob/jfx17/doc-files/release-notes-17.md).

JavaFX 17.0.1 requires JDK 11 or later.

## List of Fixed Bugs

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8273138](https://bugs.openjdk.java.net/browse/JDK-8273138)|BidirectionalBinding fails to observe changes of invalid properties|base
[JDK-8273754](https://bugs.openjdk.java.net/browse/JDK-8273754)|Re-introduce Automatic-Module-Name in empty jars|build
[JDK-8273324](https://bugs.openjdk.java.net/browse/JDK-8273324)|IllegalArgumentException: fromIndex(0) > toIndex(-1) after clear and select TableCell|controls
[JDK-8269374](https://bugs.openjdk.java.net/browse/JDK-8269374)|Menu inoperable after setting stage to second monitor|graphics
[JDK-8268718](https://bugs.openjdk.java.net/browse/JDK-8268718)|[macos] Video stops, but audio continues to play when stopTime is reached|media
[JDK-8268849](https://bugs.openjdk.java.net/browse/JDK-8268849)|Update to 612.1 version of WebKit|web
[JDK-8270479](https://bugs.openjdk.java.net/browse/JDK-8270479)|WebKit 612.1 build fails with Visual Studio 2017|web
[JDK-8272329](https://bugs.openjdk.java.net/browse/JDK-8272329)|Cherry pick GTK WebKit 2.32.3 changes|web
[JDK-8274107](https://bugs.openjdk.java.net/browse/JDK-8274107)|Cherry pick GTK WebKit 2.32.4 changes|web