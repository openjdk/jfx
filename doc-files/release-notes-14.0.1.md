# Release Notes for JavaFX 14.0.1

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

These notes document the JavaFX 14.0.1 update release. As such, they complement
the [JavaFX 14 Release Notes](https://github.com/openjdk/jfx/blob/jfx14/doc-files/release-notes-14.md).

## List of Fixed Bugs

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8233942](https://bugs.openjdk.java.net/browse/JDK-8233942) | Update to 609.1 version of WebKit | web
[JDK-8237003](https://bugs.openjdk.java.net/browse/JDK-8237003) | Remove hardcoded WebAnimationsCSSIntegrationEnabled flag in DumpRenderTree | web
[JDK-8238526](https://bugs.openjdk.java.net/browse/JDK-8238526) | Cherry pick GTK WebKit 2.26.3 changes | web
[JDK-8239454](https://bugs.openjdk.java.net/browse/JDK-8239454) | LLIntData : invalid opcode returned for 16 and 32 bit wide instructions | web
[JDK-8239109](https://bugs.openjdk.java.net/browse/JDK-8239109) | Update SQLite to version 3.31.1 | web
[JDK-8240211](https://bugs.openjdk.java.net/browse/JDK-8240211) | Stack overflow on Windows 32-bit can lead to crash | web
[JDK-8240832](https://bugs.openjdk.java.net/browse/JDK-8240832) | Remove unused applecoreaudio.md third-party legal file | media

## List of Security fixes

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8236798] (not public) | Enhance FX scripting support (see NOTE) | web

NOTE:

JavaScript programs that are run in the context of a web page loaded by WebEngine can communicate with Java objects passed from the application to the JavaScript program. JavaScript programs that reference java.lang.Class objects are now limited to the following methods:

```
getCanonicalName
getEnumConstants
getFields
getMethods
getName
getPackageName
getSimpleName
getSuperclass
getTypeName
getTypeParameters
isAssignableFrom
isArray
isEnum
isInstance
isInterface
isLocalClass
isMemberClass
isPrimitive
isSynthetic
toGenericString
toString
```

No methods can be called on the following classes:
```
java.lang.ClassLoader
java.lang.Module
java.lang.Runtime
java.lang.System

java.lang.invoke.*
java.lang.module.*
java.lang.reflect.*
java.security.*
sun.misc.*
```
