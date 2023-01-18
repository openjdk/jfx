# Release Notes for JavaFX 19.0.2

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

These notes document the JavaFX 19.0.2 update release. As such, they complement the [JavaFX 19](https://github.com/openjdk/jfx/blob/jfx19/doc-files/release-notes-19.md) release notes.

## Important Changes

### FXML JavaScript Engine Disabled by Default

The “JavaScript script engine” for FXML is now disabled by default. Any `.fxml` file that has a "javascript" Processing Instruction (PI) will no longer load by default, and an exception will be thrown.

If the JDK has a JavaScript script engine, it can be enabled by setting the system property:

```
-Djavafx.allowjs=true
```

## List of Security Fixes

Issue key|Summary|Subcomponent
---------|-------|------------
JDK-8294779 (not public) | Improve FX pages | fxml
JDK-8289336 (not public) | Better platform image support | graphics
JDK-8289343 (not public) | Better GL support | graphics
JDK-8299628 (not public) | BMP top-down images fail to load after JDK-8289336 | graphics
JDK-8292097 (not public) | Better video decoding | media
JDK-8292105 (not public) | Improve Robot functionality | window-toolkit
JDK-8292112 (not public) | Better DragView handling | window-toolkit
