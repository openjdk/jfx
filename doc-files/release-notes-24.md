# Release Notes for JavaFX 24

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

These release notes cover the standalone JavaFX 24 release. JavaFX 24 requires JDK 22 or later. JDK 24 is recommended.

## Important Changes

### JavaFX 24 Requires JDK 22 or Later

JavaFX 24 is compiled with `--release 22` and thus requires JDK 22 or later in order to run. If you attempt to run with an older JDK, the Java launcher will exit with an error message indicating that the `javafx.base` module cannot be read.

See [JDK-8340003](https://bugs.openjdk.org/browse/JDK-8340003) for more information.

### JavaFX Applications Must Use `--enable-native-access`

Running a JavaFX application on JDK 24 will produce a warning from each of the three JavaFX modules that rely on native access, due to the changes specified in [JEP 472](https://openjdk.org/jeps/472). Each warning will include the following message:

```
WARNING: Restricted methods will be blocked in a future release unless native access is enabled
```

In order to suppress the warning now, and to be able to run your application at all in a subsequent version of the JDK, you need to explicitly enable native access for all modules that need it. This is done by passing `--enable-native-access=<list-of-modules>` to `java` on the command line, listing the modules that you grant native access. This list of modules includes `javafx.graphics` and, optionally, `javafx.media` and `javafx.web`, if your application uses those modules.

For example:

```
java --enable-native-access=javafx.graphics,javafx.media,javafx.web
```

See [JDK-8347744](https://bugs.openjdk.org/browse/JDK-8347744) for more information.

### The `jdk.jsobject` Module is Now Included with JavaFX

The `jdk.jsobject` module, which is used by JavaFX WebView applications, is now included with JavaFX, replacing the JDK module of the same name. The `jdk.jsobject` module is deprecated as of JDK 24, and will be removed in a future release of the JDK.

To facilitate the transition, `jdk.jsobject` is now an upgradable module in the JDK. This means that the version of `jdk.jsobject` delivered with JavaFX can be used in place of the one in the JDK to avoid the compiler warning. This can be done as follows:

#### Applications using the SDK

When running with the JavaFX SDK, use the `--upgrade-module-path` argument. For example:

```
javac --upgrade-module-path=/path/to/javafx-sdk-24/lib
java --upgrade-module-path=/path/to/javafx-sdk-24/lib
```

NOTE: The above will fail if you run your application with JDK 23 or earlier. JDK 24 is recommended when running JavaFX 24, but if you choose to run JavaFX 24 with an earlier JDK, use the `--module-path` option instead.

#### Applications using `jlink` to create a custom Java runtime image:

When creating your custom Java runtime image, put the JavaFX jmods on the module path ahead of the JDK jmoods. For example:

```
jlink --output jdk-with-javafx \
    --module-path /path/to/javafx-jmods-24:/path/to/jdk-24/jmods \
    --add-modules ALL-MODULE-PATH
```

NOTE: The above will fail if you create a custom image using JDK 23 or earlier. JDK 24 is recommended with JavaFX 24, but if you choose to run JavaFX 24 with an earlier JDK, put the JDK jmods ahead of the JavaFX jmods on the module path (that is, reverse the order of `javafx-jmods-24` and `jdk-24/jmods`).

See [JDK-8337280](https://bugs.openjdk.org/browse/JDK-8337280) for more information.

### Pluggable Image Loading via javax.imageio

JavaFX 24 supports the Java Image I/O API, allowing applications to use third-party image loaders in addition to the built-in image loaders. This includes the ability to use variable-density image loaders for formats like SVG. When an image is loaded using a variable-density image loader, JavaFX rasterizes the image with the screen's DPI scaling.

Applications that want to use this feature can use existing open-source Image I/O extension libraries, or register a custom Image I/O service provider instance with the IIORegistry class. Refer to the Java Image I/O documentation for more information.

See [JDK-8306707](https://bugs.openjdk.org/browse/JDK-8306707) for more information.

### ScrollPane Consumes Navigation Keys Only When It Has Direct Focus

TODO: Copy Description from release-note Sub-task, JDK-8343066

See [JDK-8340852](https://bugs.openjdk.org/browse/JDK-8340852) for more information.

## Removed Features and Options

### JavaFX No Longer Supports Running With a Security Manager

The Java Security Manager has been permanently disabled in JDK 24 via [JEP 486](https://openjdk.org/jeps/486).

Likewise, as of JavaFX 24, it is no longer possible to run JavaFX applications with a security manager enabled. This is true even if you run your application on an older JDK that still supports the security manager.

The following exception will be thrown when the JavaFX runtime is initialized if the Security Manager is enabled:

```
UnsupportedOperationException: JavaFX does not support running with the Security Manager
```

See [JDK-8341090](https://bugs.openjdk.org/browse/JDK-8341090) for more information.

## Known Issues

### JavaFX Warning Printed for Use of Terminally Deprecated Methods in sun.misc.Unsafe

Running a JavaFX application on JDK 24 will produce a warning the first time any UI Control or complex shape is rendered:

```
WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
...
WARNING: sun.misc.Unsafe::allocateMemory will be removed in a future release
```

To disable this warning, pass `--sun-misc-unsafe-memory-access=allow` to `java` on the command line. For example:

```
java --sun-misc-unsafe-memory-access=allow
```

This will be fixed in a subsequent version of JavaFX, after which time this flag will no longer be needed.

See [JDK-8345121](https://bugs.openjdk.org/browse/JDK-8345121) for more information.

## List of New Features

Issue Key | Summary | Subcomponent
--------- | ------- | ------------
[JDK-8301121](https://bugs.openjdk.org/browse/JDK-8301121) | RichTextArea Control (Incubator) | controls
[JDK-8343646](https://bugs.openjdk.org/browse/JDK-8343646) | Public InputMap (Incubator) | controls
[JDK-8091673](https://bugs.openjdk.org/browse/JDK-8091673) | Public focus traversal API for use in custom controls | controls
[JDK-8306707](https://bugs.openjdk.org/browse/JDK-8306707) | Support pluggable image loading via javax.imageio | graphics
[JDK-8329098](https://bugs.openjdk.org/browse/JDK-8329098) | Support "@1x" image naming convention as fallback | graphics
[JDK-8332895](https://bugs.openjdk.org/browse/JDK-8332895) | Support interpolation for backgrounds and borders | graphics
[JDK-8341514](https://bugs.openjdk.org/browse/JDK-8341514) | Add reducedMotion and reducedTransparency preferences | graphics
[JDK-8343336](https://bugs.openjdk.org/browse/JDK-8343336) | Add persistentScrollBars preference | graphics
[JDK-8343398](https://bugs.openjdk.org/browse/JDK-8343398) | Add reducedData preference | graphics
[JDK-8345188](https://bugs.openjdk.org/browse/JDK-8345188) | Support tree-structural pseudo-classes | scenegraph

## List of Other Enhancements

Issue Key | Summary | Subcomponent
--------- | ------- | ------------
[JDK-8336031](https://bugs.openjdk.org/browse/JDK-8336031) | Create implementation of NSAccessibilityStaticText protocol | accessibility
[JDK-8226911](https://bugs.openjdk.org/browse/JDK-8226911) | Interpolatable's contract should be reexamined | animation
[JDK-8344443](https://bugs.openjdk.org/browse/JDK-8344443) | Deprecate FXPermission for removal | base
[JDK-8334874](https://bugs.openjdk.org/browse/JDK-8334874) | Horizontal scroll events from touch pads should scroll the TabPane tabs | controls
[JDK-8338016](https://bugs.openjdk.org/browse/JDK-8338016) | SplitMenuButton constructors should match MenuButton | controls
[JDK-8323706](https://bugs.openjdk.org/browse/JDK-8323706) | Remove SimpleSelector and CompoundSelector classes | graphics
[JDK-8339603](https://bugs.openjdk.org/browse/JDK-8339603) | Seal the class hierarchy of Node, Camera, LightBase, Shape, Shape3D | graphics
[JDK-8341372](https://bugs.openjdk.org/browse/JDK-8341372) | BackgroundPosition, BorderImage, BorderStroke, CornerRadii should be final | graphics
[JDK-8346227](https://bugs.openjdk.org/browse/JDK-8346227) | Seal Paint and Material | graphics
[JDK-8337280](https://bugs.openjdk.org/browse/JDK-8337280) | Include jdk.jsobject module with JavaFX | other
[JDK-8341090](https://bugs.openjdk.org/browse/JDK-8341090) | Remove support for security manager from JavaFX | other
[JDK-8305418](https://bugs.openjdk.org/browse/JDK-8305418) | [Linux] Replace obsolete XIM as Input Method Editor | window-toolkit

## List of Fixed Bugs

Issue Key | Summary | Subcomponent
--------- | ------- | ------------
[JDK-8336331](https://bugs.openjdk.org/browse/JDK-8336331) | Doc: Clarification in AccessibleAttribute, AccessibleRole | accessibility
[JDK-8342459](https://bugs.openjdk.org/browse/JDK-8342459) | Remove calls to doPrivileged in javafx.base | base
[JDK-8342994](https://bugs.openjdk.org/browse/JDK-8342994) | Remove security manager calls in com.sun.javafx.reflect | base
[JDK-8340829](https://bugs.openjdk.org/browse/JDK-8340829) | Generated API docs should clearly identify EA builds | build
[JDK-8345136](https://bugs.openjdk.org/browse/JDK-8345136) | Update JDK_DOCS property to point to JDK 23 docs | build
[JDK-8218745](https://bugs.openjdk.org/browse/JDK-8218745) | TableView: visual glitch at borders on horizontal scrolling | controls
[JDK-8296387](https://bugs.openjdk.org/browse/JDK-8296387) | [Tooltip, CSS] -fx-show-delay is only applied to the first tooltip that is shown before it is displayed | controls
[JDK-8320232](https://bugs.openjdk.org/browse/JDK-8320232) | Cells duplicated when table collapsed and expanded | controls
[JDK-8334900](https://bugs.openjdk.org/browse/JDK-8334900) | IOOBE when adding data to a Series of a BarChart that already contains data | controls
[JDK-8334901](https://bugs.openjdk.org/browse/JDK-8334901) | Style class "negative" is not always added for new BarChart data with negative value | controls
[JDK-8336592](https://bugs.openjdk.org/browse/JDK-8336592) | Wrong type in documentation for TreeTableView | controls
[JDK-8340852](https://bugs.openjdk.org/browse/JDK-8340852) | ScrollPane should not consume navigation keys when it doesn't have direct focus | controls
[JDK-8341440](https://bugs.openjdk.org/browse/JDK-8341440) | ScrollPane: no immediate effect changing fitWidth/fitHeight | controls
[JDK-8341687](https://bugs.openjdk.org/browse/JDK-8341687) | Memory leak in TableView after interacting with TableMenuButton | controls
[JDK-8342233](https://bugs.openjdk.org/browse/JDK-8342233) | Regression: TextInputControl selection is backwards in RTL mode | controls
[JDK-8344067](https://bugs.openjdk.org/browse/JDK-8344067) | TableCell indices may not match the TableRow index | controls
[JDK-8347305](https://bugs.openjdk.org/browse/JDK-8347305) | RichTextArea Follow-up | controls
[JDK-8347715](https://bugs.openjdk.org/browse/JDK-8347715) | RichTextArea Follow-up: Minor Bugs | controls
[JDK-8348736](https://bugs.openjdk.org/browse/JDK-8348736) | RichTextArea clamp and getText | controls
[JDK-8323787](https://bugs.openjdk.org/browse/JDK-8323787) | Mac System MenuBar throws IOB exception | graphics
[JDK-8333374](https://bugs.openjdk.org/browse/JDK-8333374) | Cannot invoke "com.sun.prism.RTTexture.contentsUseful()" because "this.txt" is null | graphics
[JDK-8336097](https://bugs.openjdk.org/browse/JDK-8336097) | UserAgent Styles using lookups are promoted to Author level if look-up is defined in Author stylesheet | graphics
[JDK-8336389](https://bugs.openjdk.org/browse/JDK-8336389) | Infinite loop occurs while resolving lookups | graphics
[JDK-8339068](https://bugs.openjdk.org/browse/JDK-8339068) | [Linux] NPE: Cannot read field "firstFont" because "<local4>" is null | graphics
[JDK-8340405](https://bugs.openjdk.org/browse/JDK-8340405) | JavaFX shutdown hook can hang preventing app from exiting | graphics
[JDK-8341010](https://bugs.openjdk.org/browse/JDK-8341010) | TriangleMesh.vertexFormat Property default value is wrong | graphics
[JDK-8341418](https://bugs.openjdk.org/browse/JDK-8341418) | Prism/es2 DrawableInfo is never freed (leak) | graphics
[JDK-8342453](https://bugs.openjdk.org/browse/JDK-8342453) | Remove calls to doPrivileged in javafx.graphics/com.sun.javafx.tk | graphics
[JDK-8342454](https://bugs.openjdk.org/browse/JDK-8342454) | Remove calls to doPrivileged in javafx.graphics/com.sun.glass | graphics
[JDK-8342456](https://bugs.openjdk.org/browse/JDK-8342456) | Remove calls to doPrivileged in javafx.graphics/other | graphics
[JDK-8342703](https://bugs.openjdk.org/browse/JDK-8342703) | CSS transition is not started when initial value was not specified | graphics
[JDK-8183521](https://bugs.openjdk.org/browse/JDK-8183521) | Unable to type characters with tilde with swiss german keyboard layout | localization
[JDK-8336277](https://bugs.openjdk.org/browse/JDK-8336277) | Colors are incorrect when playing H.265/HEVC on Windows 11 | media
[JDK-8336938](https://bugs.openjdk.org/browse/JDK-8336938) | Update libFFI to 3.4.6 | media
[JDK-8336939](https://bugs.openjdk.org/browse/JDK-8336939) | Update Glib to 2.80.4 | media
[JDK-8336940](https://bugs.openjdk.org/browse/JDK-8336940) | Update GStreamer to 1.24.6 | media
[JDK-8338701](https://bugs.openjdk.org/browse/JDK-8338701) | Provide media support for libavcodec version 61 | media
[JDK-8346228](https://bugs.openjdk.org/browse/JDK-8346228) | Update GStreamer to 1.24.10 | media
[JDK-8346229](https://bugs.openjdk.org/browse/JDK-8346229) | Update Glib to 2.82.4 | media
[JDK-8342457](https://bugs.openjdk.org/browse/JDK-8342457) | Remove calls to doPrivileged in swing | other
[JDK-8342911](https://bugs.openjdk.org/browse/JDK-8342911) | Remove calls to doPrivileged in controls | other
[JDK-8342912](https://bugs.openjdk.org/browse/JDK-8342912) | Remove calls to doPrivileged in fxml | other
[JDK-8342913](https://bugs.openjdk.org/browse/JDK-8342913) | Remove calls to doPrivileged in media | other
[JDK-8342914](https://bugs.openjdk.org/browse/JDK-8342914) | Remove calls to doPrivileged in swt | other
[JDK-8342992](https://bugs.openjdk.org/browse/JDK-8342992) | Security manager check should not use deprecated methods | other
[JDK-8342993](https://bugs.openjdk.org/browse/JDK-8342993) | Remove uses of AccessController and AccessControlContext from JavaFX | other
[JDK-8342997](https://bugs.openjdk.org/browse/JDK-8342997) | Remove use of System::getSecurityManager and SecurityManager from JavaFX | other
[JDK-8342998](https://bugs.openjdk.org/browse/JDK-8342998) | Remove all uses of AccessControlException | other
[JDK-8344367](https://bugs.openjdk.org/browse/JDK-8344367) | Fix mistakes in FX API docs | other
[JDK-8288893](https://bugs.openjdk.org/browse/JDK-8288893) | Popup and its subclasses cannot input text from InputMethod | scenegraph
[JDK-8335470](https://bugs.openjdk.org/browse/JDK-8335470) | [XWayland] JavaFX tests that use AWT Robot fail on Wayland | swing
[JDK-8340005](https://bugs.openjdk.org/browse/JDK-8340005) | Eliminate native access calls from javafx.swing | swing
[JDK-8340849](https://bugs.openjdk.org/browse/JDK-8340849) | [macos] Crash when creating a child window of a JavaFX window after Platform::exit | swing
[JDK-8328994](https://bugs.openjdk.org/browse/JDK-8328994) | Update WebKit to 619.1 | web
[JDK-8334124](https://bugs.openjdk.org/browse/JDK-8334124) | Rendering issues with CSS "text-shadow" in WebView | web
[JDK-8336941](https://bugs.openjdk.org/browse/JDK-8336941) | Update libxslt to 1.1.42 | web
[JDK-8337481](https://bugs.openjdk.org/browse/JDK-8337481) | File API: file.name contains path instead of name | web
[JDK-8338307](https://bugs.openjdk.org/browse/JDK-8338307) | Additional WebKit 619.1 fixes from WebKitGTK 2.44.3 | web
[JDK-8340208](https://bugs.openjdk.org/browse/JDK-8340208) | Additional WebKit 619.1 fixes from WebKitGTK 2.44.4 | web
[JDK-8342460](https://bugs.openjdk.org/browse/JDK-8342460) | Remove calls to doPrivileged in javafx.web | web
[JDK-8342461](https://bugs.openjdk.org/browse/JDK-8342461) | Remove calls to doPrivileged in javafx.web/{android,ios} | web
[JDK-8087863](https://bugs.openjdk.org/browse/JDK-8087863) | Mac: "Select All" within ListView/TreeView is handled differently depending on the useSystemMenuBar value | window-toolkit
[JDK-8273743](https://bugs.openjdk.org/browse/JDK-8273743) | KeyCharacterCombination for "+" does not work on US QWERTY keyboard layout | window-toolkit
[JDK-8319779](https://bugs.openjdk.org/browse/JDK-8319779) | SystemMenu: memory leak due to listener never being removed | window-toolkit
[JDK-8325445](https://bugs.openjdk.org/browse/JDK-8325445) | [macOS] Colors are not displayed in sRGB color space | window-toolkit
[JDK-8332222](https://bugs.openjdk.org/browse/JDK-8332222) | Linux Debian: Maximized stage shrinks when opening another stage | window-toolkit
[JDK-8333919](https://bugs.openjdk.org/browse/JDK-8333919) | [macOS] dragViewOffsetX/dragViewOffsetY are ignored for the dragView image | window-toolkit
[JDK-8335469](https://bugs.openjdk.org/browse/JDK-8335469) | [XWayland] crash when an AWT ScreenCast session overlaps with an FX ScreenCast session | window-toolkit
[JDK-8339178](https://bugs.openjdk.org/browse/JDK-8339178) | [macos] Swing InterOp Platform.exit() crash | window-toolkit
[JDK-8339183](https://bugs.openjdk.org/browse/JDK-8339183) | [macos] Premature exit in Swing interop when last JFrame is disposed | window-toolkit
[JDK-8340982](https://bugs.openjdk.org/browse/JDK-8340982) | [win] Dead key followed by Space generates two characters instead of one | window-toolkit
[JDK-8344372](https://bugs.openjdk.org/browse/JDK-8344372) | Setting width for TRANSPARENT Stage -> gtk_window_resize: assertion 'height > 0' | window-toolkit
[JDK-8348744](https://bugs.openjdk.org/browse/JDK-8348744) | Application window not always activated on macOS 15 | window-toolkit

## List of Security fixes

Issue Key | Summary | Subcomponent
--------- | ------- | ------------
JDK-8335714 (not public) | Enhance playing MP3s | media
JDK-8335715 (not public) | Improve Direct Show support | media
