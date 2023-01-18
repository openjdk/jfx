# Release Notes for JavaFX 19.0.2

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

These notes document the JavaFX 19.0.2 update release. As such, they complement the [JavaFX 19](https://github.com/openjdk/jfx/blob/jfx19/doc-files/release-notes-19.md) release notes.

### FXML JavaScript Engine Disabled by Default

The “JavaScript script engine” for FXML is now disabled by default. Any `.fxml` file that has a "javascript" Processing Instruction (PI) will no longer load by default, and an exception will be thrown.

If the JDK has a JavaScript script engine, it can be enabled by setting the system property:

```
-Djavafx.allowjs=true
```

## List of Fixed Bugs

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8280841](https://bugs.openjdk.java.net/browse/JDK-8280841)|Update SQLite to 3.37.2|web
[JDK-8282134](https://bugs.openjdk.java.net/browse/JDK-8282134)|Certain regex can cause a JS trap in WebView|web
[JDK-8283328](https://bugs.openjdk.java.net/browse/JDK-8283328)|Update libxml2 to 2.9.13|web
[JDK-8286256](https://bugs.openjdk.java.net/browse/JDK-8286256)|Update libxml2 to 2.9.14|web
[JDK-8286257](https://bugs.openjdk.java.net/browse/JDK-8286257)|Update libxslt to 1.1.35|web
