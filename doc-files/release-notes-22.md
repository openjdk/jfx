# Release Notes for JavaFX 22

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

These release notes cover the standalone JavaFX 22 release. JavaFX 22 requires JDK 17 or later.

## Important Changes

### Animation May Be Started Asynchronously

The Animation methods play, start, stop, and pause may now be called on any thread. If these methods are called on a thread other than the JavaFX Application Thread, they will delegate to that thread to ensure proper thread safety. Since the execution is asynchronous in that case, the status might not be updated right away.

See [JDK-8324658](https://bugs.openjdk.org/browse/JDK-8324658) for more information.

## List of Enhancements

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8309558](https://bugs.openjdk.org/browse/JDK-8309558)|Create implementation of NSAccessibilityCheckBox protocol|accessibility
[JDK-8309629](https://bugs.openjdk.org/browse/JDK-8309629)|Create implementation of NSAccessibilityRadioButton protocol|accessibility
[JDK-8324658](https://bugs.openjdk.org/browse/JDK-8324658)|Allow animation play/start/stop/pause methods to be called on any thread|animation
[JDK-8318204](https://bugs.openjdk.org/browse/JDK-8318204)|Use new EventTarget methods in ListenerHelper|controls
[JDK-8301302](https://bugs.openjdk.org/browse/JDK-8301302)|Platform preferences API|graphics
[JDK-8314147](https://bugs.openjdk.org/browse/JDK-8314147)|Updated the PhongMaterial documentation|graphics
[JDK-8320359](https://bugs.openjdk.org/browse/JDK-8320359)|ImageView: add styleable fitWidth, fitHeight, preserveRatio, smooth properties|graphics
[JDK-8321573](https://bugs.openjdk.org/browse/JDK-8321573)|Improve Platform.Preferences documentation|graphics


## List of Fixed Bugs

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8284544](https://bugs.openjdk.org/browse/JDK-8284544)|[Win] Name-Property of Spinner cannot be changed|accessibility
[JDK-8314597](https://bugs.openjdk.org/browse/JDK-8314597)|Deprecate for removal protected access methods in converters|base
[JDK-8317370](https://bugs.openjdk.org/browse/JDK-8317370)|JavaFX runtime version is wrong at runtime|base
[JDK-8187314](https://bugs.openjdk.org/browse/JDK-8187314)|All Cells: must show backing data always|controls
[JDK-8205067](https://bugs.openjdk.org/browse/JDK-8205067)|Resizing window with TextField hides text value|controls
[JDK-8248914](https://bugs.openjdk.org/browse/JDK-8248914)|Javafx TextField positions the cursor incorrectly after pressing DEL key|controls
[JDK-8282290](https://bugs.openjdk.org/browse/JDK-8282290)|TextField Cursor Position one off|controls
[JDK-8283675](https://bugs.openjdk.org/browse/JDK-8283675)|Line not removed from LineChart when series cleared|controls
[JDK-8285700](https://bugs.openjdk.org/browse/JDK-8285700)|[TreeTableView] graphic property of TreeItem is still visible after collapsing tree|controls
[JDK-8303478](https://bugs.openjdk.org/browse/JDK-8303478)|DatePicker throws uncatchable exception on tab out from garbled text|controls
[JDK-8311185](https://bugs.openjdk.org/browse/JDK-8311185)|VirtualFlow jump when cellcount changes|controls
[JDK-8311983](https://bugs.openjdk.org/browse/JDK-8311983)|ListView sometimes throws an IndexOutOfBoundsException|controls
[JDK-8313628](https://bugs.openjdk.org/browse/JDK-8313628)|Column drag header, overlay and line are not correctly aligned|controls
[JDK-8313651](https://bugs.openjdk.org/browse/JDK-8313651)|Add 'final' keyword to public property methods in controls|controls
[JDK-8320444](https://bugs.openjdk.org/browse/JDK-8320444)|Column drag header is positioned wrong for nested columns|controls
[JDK-8321722](https://bugs.openjdk.org/browse/JDK-8321722)|Tab header flickering when dragging slowly other tabs and reordering uncompleted|controls
[JDK-8321970](https://bugs.openjdk.org/browse/JDK-8321970)|New table columns don't appear when using fixed cell size unless refreshing tableView|controls
[JDK-8323543](https://bugs.openjdk.org/browse/JDK-8323543)|NPE when table items are set to null|controls
[JDK-8260342](https://bugs.openjdk.org/browse/JDK-8260342)|FXMLLoader fails to load a sub layout using fx:include with the resources attribute|fxml
[JDK-8189282](https://bugs.openjdk.org/browse/JDK-8189282)|JavaFX: Invalid position of candidate pop-up of InputMethod in Hi-DPI on Windows|graphics
[JDK-8254126](https://bugs.openjdk.org/browse/JDK-8254126)|the position of Chinese Input Method candidates window is wrong|graphics
[JDK-8269921](https://bugs.openjdk.org/browse/JDK-8269921)|TextFlow: listeners on bounds can throw NPE while computing text bounds|graphics
[JDK-8283401](https://bugs.openjdk.org/browse/JDK-8283401)|ArrayIndexOutOfBoundsException when disconnecting screen(s)|graphics
[JDK-8301893](https://bugs.openjdk.org/browse/JDK-8301893)|IME window position is off on secondary screen|graphics
[JDK-8306083](https://bugs.openjdk.org/browse/JDK-8306083)|Text.hitTest is incorrect when more than one Text node in TextFlow|graphics
[JDK-8307536](https://bugs.openjdk.org/browse/JDK-8307536)|Exception from NativeLibLoader when running concurrent applications with empty cache|graphics
[JDK-8310885](https://bugs.openjdk.org/browse/JDK-8310885)|Width/height of window is not set after calling sizeToScene|graphics
[JDK-8311216](https://bugs.openjdk.org/browse/JDK-8311216)|DataURI can lose information in some charset environments|graphics
[JDK-8313648](https://bugs.openjdk.org/browse/JDK-8313648)|JavaFX application continues to show a black screen after graphic card driver crash|graphics
[JDK-8313856](https://bugs.openjdk.org/browse/JDK-8313856)|Replace VLA with malloc in pango|graphics
[JDK-8314141](https://bugs.openjdk.org/browse/JDK-8314141)|Missing default for switch in CreateBitmap|graphics
[JDK-8316419](https://bugs.openjdk.org/browse/JDK-8316419)|[macos] Setting X/Y makes Stage maximization not work before show|graphics
[JDK-8316423](https://bugs.openjdk.org/browse/JDK-8316423)|[linux] Secondary Stage does not respect Scene's dimensions when shown|graphics
[JDK-8316518](https://bugs.openjdk.org/browse/JDK-8316518)|javafx.print.Paper getWidth / getHeight rounds values, causing errors.|graphics
[JDK-8316781](https://bugs.openjdk.org/browse/JDK-8316781)|Legal, Monarch paper sizes are incorrect in javafx.print.Paper|graphics
[JDK-8319079](https://bugs.openjdk.org/browse/JDK-8319079)|Missing range checks in decora|graphics
[JDK-8322795](https://bugs.openjdk.org/browse/JDK-8322795)|CSS performance regression up to 10x|graphics
[JDK-8323077](https://bugs.openjdk.org/browse/JDK-8323077)|C type error (incompatible function pointer) in X11GLContext.c|graphics
[JDK-8323078](https://bugs.openjdk.org/browse/JDK-8323078)|Incorrect length argument to g_utf8_strlen in pango.c|graphics
[JDK-8324879](https://bugs.openjdk.org/browse/JDK-8324879)|Platform-specific preferences keys are incorrect for Windows toolkit|graphics
[JDK-8325550](https://bugs.openjdk.org/browse/JDK-8325550)|Grammatical error in AnchorPane.setLeftAnchor (and other setters) javadoc|graphics
[JDK-8313900](https://bugs.openjdk.org/browse/JDK-8313900)|Possible NULL pointer access in NativeAudioSpectrum and NativeVideoBuffer|media
[JDK-8317508](https://bugs.openjdk.org/browse/JDK-8317508)|Provide media support for libavcodec version 60|media
[JDK-8318386](https://bugs.openjdk.org/browse/JDK-8318386)|Update Glib to 2.78.1|media
[JDK-8318387](https://bugs.openjdk.org/browse/JDK-8318387)|Update GStreamer to 1.22.6|media
[JDK-8185831](https://bugs.openjdk.org/browse/JDK-8185831)|Pseudo selectors do not appear to work in Node.lookupAll()|scenegraph
[JDK-8199216](https://bugs.openjdk.org/browse/JDK-8199216)|Quadratic layout time with nested nodes and pseudo-class in style sheet|scenegraph
[JDK-8313956](https://bugs.openjdk.org/browse/JDK-8313956)|focusWithin on parents of a newly-added focused node is not updated|scenegraph
[JDK-8318059](https://bugs.openjdk.org/browse/JDK-8318059)|Typo is javafx.scene.Node.usesMirroring comment|scenegraph
[JDK-8318624](https://bugs.openjdk.org/browse/JDK-8318624)|API docs specify incorrect default value for nodeOrientation property|scenegraph
[JDK-8222209](https://bugs.openjdk.org/browse/JDK-8222209)|JavaFX is rendered blurry on systems with monitors in different configuration|swing
[JDK-8262518](https://bugs.openjdk.org/browse/JDK-8262518)|SwingNode.setContent does not close previous content, resulting in memory leak|swing
[JDK-8274932](https://bugs.openjdk.org/browse/JDK-8274932)|Render scales in EmbeddedWindow are not properly updated|swing
[JDK-8317836](https://bugs.openjdk.org/browse/JDK-8317836)|FX nodes embedded in JFXPanel need to track component orientation|swing
[JDK-8310681](https://bugs.openjdk.org/browse/JDK-8310681)|Update WebKit to 616.1|web
[JDK-8311097](https://bugs.openjdk.org/browse/JDK-8311097)|Synchron XMLHttpRequest not receiving data|web
[JDK-8313177](https://bugs.openjdk.org/browse/JDK-8313177)|Web Workers timeout with Webkit 616.1|web
[JDK-8313181](https://bugs.openjdk.org/browse/JDK-8313181)|Enabling media controls on webkit 616.1 does not load button images on video Element|web
[JDK-8313711](https://bugs.openjdk.org/browse/JDK-8313711)|Cherry-pick WebKit 616.1 stabilization fixes|web
[JDK-8314212](https://bugs.openjdk.org/browse/JDK-8314212)|Crash when loading cnn.com in WebView|web
[JDK-8318388](https://bugs.openjdk.org/browse/JDK-8318388)|Update libxslt to 1.1.39|web
[JDK-8320267](https://bugs.openjdk.org/browse/JDK-8320267)|WebView crashes on macOS 11 with WebKit 616.1|web
[JDK-8087368](https://bugs.openjdk.org/browse/JDK-8087368)|java runtime environment error when trying to execute showAndWait() function|window-toolkit
[JDK-8087700](https://bugs.openjdk.org/browse/JDK-8087700)|[KeyCombination, Mac] KeyCharacterCombinations behave erratically|window-toolkit
[JDK-8221261](https://bugs.openjdk.org/browse/JDK-8221261)|Deadlock on macOS in JFXPanel app when handling IME calls|window-toolkit
[JDK-8251240](https://bugs.openjdk.org/browse/JDK-8251240)|Menus inaccessible on Linux with i3 wm|window-toolkit
[JDK-8255835](https://bugs.openjdk.org/browse/JDK-8255835)|[macOS] Undecorated stage cannot be maximized|window-toolkit
[JDK-8274967](https://bugs.openjdk.org/browse/JDK-8274967)|KeyCharacterCombinations for punctuation and symbols fail on non-US keyboards|window-toolkit
[JDK-8284445](https://bugs.openjdk.org/browse/JDK-8284445)|macOS 12 prints a warning when a function key shortcut is assigned to a menu|window-toolkit
[JDK-8301219](https://bugs.openjdk.org/browse/JDK-8301219)|JavaFX crash when closing with the escape key|window-toolkit
[JDK-8305675](https://bugs.openjdk.org/browse/JDK-8305675)|[macos] Stage set to iconified before being shown is displayed on screen|window-toolkit
[JDK-8314149](https://bugs.openjdk.org/browse/JDK-8314149)|Clipboard does inexact string comparison on mime type|window-toolkit
[JDK-8315074](https://bugs.openjdk.org/browse/JDK-8315074)|Possible null pointer access in native glass|window-toolkit
[JDK-8315657](https://bugs.openjdk.org/browse/JDK-8315657)|Application window not activated in macOS 14 Sonoma|window-toolkit
[JDK-8315958](https://bugs.openjdk.org/browse/JDK-8315958)|Missing range checks in GlassPasteboard|window-toolkit
[JDK-8318841](https://bugs.openjdk.org/browse/JDK-8318841)|macOS: Memory leak with MenuItem when Menu.useSystemMenuBar(true) is used|window-toolkit
[JDK-8319066](https://bugs.openjdk.org/browse/JDK-8319066)|Application window not always activated in macOS 14 Sonoma|window-toolkit
[JDK-8319341](https://bugs.openjdk.org/browse/JDK-8319341)|[Linux] Remove operation to show or hide children because it is unnecessary|window-toolkit
[JDK-8319669](https://bugs.openjdk.org/browse/JDK-8319669)|[macos14] Running any JavaFX app prints Secure coding warning|window-toolkit
[JDK-8322215](https://bugs.openjdk.org/browse/JDK-8322215)|[win] OS events that close the stage can cause Glass to reference freed memory|window-toolkit


## List of Security fixes

Issue key|Summary|Subcomponent
---------|-------|------------
JDK-8313048 (not public)|Better Glyph handling|graphics
JDK-8313105 (not public)|Improved media framing|media
JDK-8313056 (not public)|General enhancements of Glass|window-toolkit
